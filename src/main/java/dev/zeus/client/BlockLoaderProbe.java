package dev.zeus.client;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import dev.zeus.ZeusConfig;
import dev.zeus.remount.AthenaSuppress;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;

import net.minecraft.client.resources.model.BakedModel;
import java.lang.reflect.Field;
import java.util.Collection;
import java.util.HashSet;
import java.io.BufferedReader;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * Resolves which model loader(s) the looked-at block's effective resources declare.
 */
public final class BlockLoaderProbe {
    private BlockLoaderProbe() {
    }

    public static void reportLookedAtBlock() {
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        if (player == null || mc.level == null) {
            return;
        }
        HitResult hit = mc.hitResult;
        if (hit == null || hit.getType() != HitResult.Type.BLOCK) {
            tell(player, "Look at a block first.");
            return;
        }

        BlockHitResult blockHit = (BlockHitResult) hit;
        BlockPos pos = blockHit.getBlockPos();
        BlockState state = mc.level.getBlockState(pos);
        ResourceLocation blockId = BuiltInRegistries.BLOCK.getKey(state.getBlock());
        ResourceManager resources = mc.getResourceManager();

        tell(player, "---- Zeus loader probe ----");
        tell(player, "Block: " + blockId + " @ " + pos.toShortString());
        tell(player, "State: " + state);
        tell(player, "Namespace remountable: " + ZeusConfig.remounts(blockId.getNamespace())
                + " (backend=" + ZeusConfig.ctmBackend + ")");

        ResourceLocation blockstateLoc = ResourceLocation.fromNamespaceAndPath(
                blockId.getNamespace(),
                "blockstates/" + blockId.getPath() + ".json"
        );
        Optional<Resource> blockstateRes = resources.getResource(blockstateLoc);
        if (blockstateRes.isEmpty()) {
            tell(player, "Blockstate JSON: MISSING (" + blockstateLoc + ")");
            return;
        }
        tell(player, "Blockstate: " + blockstateLoc + " [" + packId(blockstateRes.get()) + "]");

        Optional<JsonObject> blockstateJson = readJson(resources, blockstateLoc);
        if (blockstateJson.isEmpty()) {
            tell(player, "Blockstate JSON: unreadable");
            return;
        }

        JsonObject bsJson = blockstateJson.get();
        String bsLoader = readLoader(bsJson);
        if (bsLoader != null) {
            tell(player, "Blockstate loader key: " + bsLoader + " => " + classify(bsLoader, null));
        } else {
            tell(player, "Blockstate loader key: (none)");
        }

        List<ModelRef> models = collectModels(bsJson);
        if (models.isEmpty()) {
            tell(player, "No model refs in blockstate variants/multipart.");
        }

        Set<String> loaders = new LinkedHashSet<>();
        for (ModelRef ref : models) {
            inspectModel(player, resources, ref, loaders, 0);
        }

        String summary = loaders.isEmpty() ? "vanilla (no loader key)" : String.join(", ", loaders);
        tell(player, "Effective loader(s): " + summary);

        boolean wouldSuppress = AthenaSuppress.shouldSuppress(
                ResourceLocation.fromNamespaceAndPath(blockId.getNamespace(), "block/" + blockId.getPath())
        );
        // Also try plain block id path used by some Athena keys
        boolean wouldSuppressPlain = AthenaSuppress.shouldSuppress(blockId);
        tell(player, "Zeus would suppress Athena for block/" + blockId.getPath() + ": " + wouldSuppress
                + " | for " + blockId + ": " + wouldSuppressPlain);

        probeAthena(player, blockId);
        probeBakedModel(player, state);
        tell(player, "---- end probe ----");
    }

    private static void inspectModel(
            LocalPlayer player,
            ResourceManager resources,
            ModelRef ref,
            Set<String> loaders,
            int depth
    ) {
        if (depth > 4) {
            return;
        }
        ResourceLocation modelLoc = ResourceLocation.fromNamespaceAndPath(
                ref.id.getNamespace(),
                "models/" + ref.id.getPath() + ".json"
        );
        Optional<Resource> res = resources.getResource(modelLoc);
        if (res.isEmpty()) {
            tell(player, indent(depth) + "model " + ref.id + " MISSING");
            loaders.add("missing");
            return;
        }
        Optional<JsonObject> jsonOpt = readJson(resources, modelLoc);
        if (jsonOpt.isEmpty()) {
            tell(player, indent(depth) + "model " + ref.id + " unreadable [" + packId(res.get()) + "]");
            return;
        }
        JsonObject json = jsonOpt.get();
        String loader = readLoader(json);
        String type = json.has("type") && json.get("type").isJsonPrimitive()
                ? json.get("type").getAsString()
                : null;
        String label = classify(loader, type);
        loaders.add(label);
        StringBuilder line = new StringBuilder();
        line.append(indent(depth)).append("model ").append(ref.id)
                .append(" [").append(packId(res.get())).append("] => ").append(label);
        if (loader != null) {
            line.append(" (loader=").append(loader);
            if (type != null) {
                line.append(", type=").append(type);
            }
            line.append(")");
        } else if (type != null) {
            line.append(" (type=").append(type).append(")");
        }
        tell(player, line.toString());

        // Composite children
        if ("composite".equalsIgnoreCase(type) && json.has("models") && json.get("models").isJsonArray()) {
            for (JsonElement el : json.getAsJsonArray("models")) {
                String childId = null;
                if (el.isJsonPrimitive()) {
                    childId = el.getAsString();
                } else if (el.isJsonObject() && el.getAsJsonObject().has("model")) {
                    JsonElement m = el.getAsJsonObject().get("model");
                    if (m.isJsonPrimitive()) {
                        childId = m.getAsString();
                    }
                }
                if (childId != null) {
                    inspectModel(player, resources, new ModelRef(ResourceLocation.parse(childId)), loaders, depth + 1);
                }
            }
        }

        // Parent chain (vanilla)
        if (json.has("parent") && json.get("parent").isJsonPrimitive()) {
            String parent = json.get("parent").getAsString();
            if (!parent.startsWith("#")) {
                ResourceLocation parentId = ResourceLocation.parse(parent.contains(":") ? parent : "minecraft:" + parent);
                // Only recurse if parent json might declare a loader (rare)
                Optional<JsonObject> parentJson = readJson(
                        resources,
                        ResourceLocation.fromNamespaceAndPath(parentId.getNamespace(), "models/" + parentId.getPath() + ".json")
                );
                if (parentJson.isPresent()) {
                    String parentLoader = readLoader(parentJson.get());
                    if (parentLoader != null) {
                        inspectModel(player, resources, new ModelRef(parentId), loaders, depth + 1);
                    }
                }
            }
        }
    }

    private static void probeAthena(LocalPlayer player, ResourceLocation blockId) {
        try {
            Class<?> clazz = Class.forName("earth.terrarium.athena.impl.loading.AthenaResourceLoader");
            Method getData = clazz.getMethod("getData", ResourceLocation.class, ResourceLocation.class);
            ResourceLocation[] modelIds = new ResourceLocation[] {
                    ResourceLocation.fromNamespaceAndPath(blockId.getNamespace(), "block/" + blockId.getPath()),
                    blockId
            };
            // Common Athena loader types
            String[] types = new String[] {
                    "athena:ctm",
                    "athena:giant",
                    "athena:pillar",
                    "athena:connected_pillar",
                    "athena:pane",
                    "athena:carpet",
                    "athena:limited_pillar"
            };
            boolean any = false;
            for (String type : types) {
                ResourceLocation typeId = ResourceLocation.parse(type);
                for (ResourceLocation modelId : modelIds) {
                    Object data = getData.invoke(null, typeId, modelId);
                    if (data != null) {
                        any = true;
                        tell(player, "Athena.getData(" + type + ", " + modelId + ") = PRESENT");
                    }
                }
            }
            if (!any) {
                tell(player, "Athena.getData(...): none for common types/ids (suppressed or no Athena CTM)");
            }
        } catch (ClassNotFoundException e) {
            tell(player, "Athena: not on classpath");
        } catch (Exception e) {
            tell(player, "Athena probe failed: " + e);
        }
    }

    private static String classify(String loader, String type) {
        if (loader == null || loader.isEmpty()) {
            return "vanilla";
        }
        String l = loader.toLowerCase();
        if (l.startsWith("athena:") || "athena".equals(l)) {
            return "athena";
        }
        if (l.startsWith("fusion:") || "fusion".equals(l)) {
            return type != null ? "fusion/" + type : "fusion";
        }
        if (l.contains(":")) {
            return l.split(":", 2)[0] + " (" + l + ")";
        }
        return loader;
    }

    private static List<ModelRef> collectModels(JsonObject blockstate) {
        List<ModelRef> out = new ArrayList<>();
        if (blockstate.has("variants") && blockstate.get("variants").isJsonObject()) {
            for (Map.Entry<String, JsonElement> e : blockstate.getAsJsonObject("variants").entrySet()) {
                collectFromVariant(e.getValue(), out);
            }
        }
        if (blockstate.has("multipart") && blockstate.get("multipart").isJsonArray()) {
            for (JsonElement part : blockstate.getAsJsonArray("multipart")) {
                if (part.isJsonObject() && part.getAsJsonObject().has("apply")) {
                    collectFromVariant(part.getAsJsonObject().get("apply"), out);
                }
            }
        }
        // Athena-style blockstate may only have athena:loader
        if (out.isEmpty()) {
            String athenaLoader = null;
            if (blockstate.has("athena:loader") && blockstate.get("athena:loader").isJsonPrimitive()) {
                athenaLoader = blockstate.get("athena:loader").getAsString();
            } else if (blockstate.has("loader") && blockstate.get("loader").isJsonPrimitive()) {
                String l = blockstate.get("loader").getAsString();
                if (l.startsWith("athena:")) {
                    athenaLoader = l;
                }
            }
            if (athenaLoader != null) {
                // No discrete model list — mark via synthetic note later
            }
        }
        return out;
    }

    private static void collectFromVariant(JsonElement element, List<ModelRef> out) {
        if (element.isJsonArray()) {
            for (JsonElement child : element.getAsJsonArray()) {
                collectFromVariant(child, out);
            }
            return;
        }
        if (!element.isJsonObject()) {
            return;
        }
        JsonObject obj = element.getAsJsonObject();
        if (obj.has("model") && obj.get("model").isJsonPrimitive()) {
            out.add(new ModelRef(ResourceLocation.parse(obj.get("model").getAsString())));
        }
    }

    private static String readLoader(JsonObject json) {
        if (json.has("loader") && json.get("loader").isJsonPrimitive()) {
            return json.get("loader").getAsString();
        }
        if (json.has("athena:loader") && json.get("athena:loader").isJsonPrimitive()) {
            return json.get("athena:loader").getAsString();
        }
        return null;
    }

    private static Optional<JsonObject> readJson(ResourceManager resources, ResourceLocation location) {
        try {
            Optional<Resource> resource = resources.getResource(location);
            if (resource.isEmpty()) {
                return Optional.empty();
            }
            try (BufferedReader reader = resource.get().openAsReader()) {
                JsonElement element = JsonParser.parseReader(reader);
                if (element.isJsonObject()) {
                    return Optional.of(element.getAsJsonObject());
                }
            }
        } catch (Exception ignored) {
        }
        return Optional.empty();
    }

    private static String packId(Resource resource) {
        try {
            return resource.sourcePackId();
        } catch (Throwable t) {
            return "?";
        }
    }

    private static String indent(int depth) {
        return "  ".repeat(Math.max(0, depth));
    }

    private static void tell(LocalPlayer player, String msg) {
        player.displayClientMessage(Component.literal(msg), false);
        ZeusDebugCommands.log(msg);
    }


    private static void probeBakedModel(LocalPlayer player, BlockState state) {
        try {
            BakedModel model = Minecraft.getInstance().getBlockRenderer().getBlockModel(state);
            tell(player, "BakedModel (from ModelBakery / BlockModelShaper):");
            Set<String> tags = new LinkedHashSet<>();
            Set<Object> visited = new HashSet<>();
            walkBaked(player, model, 0, visited, tags);
            tell(player, "Baked verdict: " + bakedVerdict(tags));
        } catch (Exception e) {
            tell(player, "BakedModel probe failed: " + e);
        }
    }

    private static String bakedVerdict(Set<String> tags) {
        boolean athena = tags.contains("athena");
        boolean fusion = tags.contains("fusion");
        boolean vanilla = tags.contains("vanilla");
        if (athena && fusion) {
            return "BOTH Athena + Fusion (likely double mesh / Z-fight)";
        }
        if (athena) {
            return "Athena only";
        }
        if (fusion) {
            return "Fusion only";
        }
        if (vanilla) {
            return "vanilla-like only";
        }
        return tags.isEmpty() ? "unknown" : String.join(", ", tags);
    }

    private static void walkBaked(
            LocalPlayer player,
            Object model,
            int depth,
            Set<Object> visited,
            Set<String> tags
    ) {
        if (model == null || depth > 8 || !visited.add(model)) {
            return;
        }
        Class<?> cls = model.getClass();
        String name = cls.getName();
        String tag = tagBakedClass(name);
        tags.add(tag);
        tell(player, indent(depth) + tag + " :: " + name);

        // Unwrap known wrapper fields + any BakedModel-typed fields
        for (Field field : allFields(cls)) {
            try {
                field.setAccessible(true);
                Object value = field.get(model);
                if (value == null) {
                    continue;
                }
                if (value instanceof BakedModel) {
                    walkBaked(player, value, depth + 1, visited, tags);
                } else if (value instanceof BakedModel[] arr) {
                    for (BakedModel child : arr) {
                        walkBaked(player, child, depth + 1, visited, tags);
                    }
                } else if (value instanceof Collection<?> col) {
                    for (Object child : col) {
                        if (child instanceof BakedModel || isLikelyModel(child)) {
                            walkBaked(player, child, depth + 1, visited, tags);
                        }
                    }
                } else if (value instanceof Map<?, ?> map) {
                    for (Object child : map.values()) {
                        if (child instanceof BakedModel || isLikelyModel(child)) {
                            walkBaked(player, child, depth + 1, visited, tags);
                        } else if (child instanceof Collection<?> nested) {
                            for (Object n : nested) {
                                if (n instanceof BakedModel || isLikelyModel(n)) {
                                    walkBaked(player, n, depth + 1, visited, tags);
                                }
                            }
                        }
                    }
                } else if (isLikelyModel(value) && depth < 6) {
                    // e.g. ConditionalModel entries holding a baked model field
                    walkBaked(player, value, depth + 1, visited, tags);
                }
            } catch (Throwable ignored) {
            }
        }
    }

    private static boolean isLikelyModel(Object value) {
        if (value == null) {
            return false;
        }
        String n = value.getClass().getName();
        return n.contains("BakedModel") || n.contains("ConditionalModel") || n.contains("ModelEntry");
    }

    private static String tagBakedClass(String name) {
        String lower = name.toLowerCase();
        if (lower.contains("athena")) {
            return "athena";
        }
        if (lower.contains("supermartijn642.fusion") || lower.contains(".fusion.")) {
            return "fusion";
        }
        if (name.startsWith("net.minecraft.")) {
            return "vanilla";
        }
        return "other";
    }

    private static List<Field> allFields(Class<?> cls) {
        List<Field> fields = new ArrayList<>();
        for (Class<?> c = cls; c != null && c != Object.class; c = c.getSuperclass()) {
            for (Field f : c.getDeclaredFields()) {
                fields.add(f);
            }
        }
        return fields;
    }

    private record ModelRef(ResourceLocation id) {
    }
}