package dev.zeus.remount;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import dev.zeus.Zeus;
import dev.zeus.ZeusConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;

import java.io.BufferedReader;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Decides whether Athena should stand down for a model id because a resource pack
 * already specifies the configured CTM backend's loader.
 */
public final class AthenaSuppress {
    private static final ConcurrentHashMap<ResourceLocation, Boolean> CACHE = new ConcurrentHashMap<>();

    private AthenaSuppress() {
    }

    public static void clearCache() {
        CACHE.clear();
    }

    public static boolean shouldSuppress(ResourceLocation modelId) {
        if (modelId == null) {
            return false;
        }
        if (!ZeusConfig.remounts(modelId.getNamespace())) {
            return false;
        }

        Boolean cached = CACHE.get(modelId);
        if (cached != null) {
            return cached;
        }

        boolean suppress = computeSuppress(modelId);
        CACHE.put(modelId, suppress);
        return suppress;
    }

    private static boolean computeSuppress(ResourceLocation modelId) {
        Optional<CtmBackend> backend = CtmBackendRegistry.active();
        if (backend.isEmpty()) {
            return false;
        }
        CtmBackend ctm = backend.get();
        if (!ctm.isAvailable() && !ZeusConfig.stripAthenaWhenBackendMissing) {
            return false;
        }

        String expectedLoader = ctm.modelLoaderId();
        try {
            Minecraft minecraft = Minecraft.getInstance();
            if (minecraft == null) {
                return false;
            }
            ResourceManager resources = minecraft.getResourceManager();
            if (resources == null) {
                return false;
            }
            return usesLoader(resources, modelId, expectedLoader);
        } catch (Exception e) {
            Zeus.LOGGER.debug("Athena suppress check failed for {}: {}", modelId, e.toString());
            return false;
        }
    }

    private static boolean usesLoader(ResourceManager resources, ResourceLocation modelId, String expectedLoader) {
        if (modelJsonUsesLoader(resources, modelId, expectedLoader)) {
            return true;
        }

        ResourceLocation blockstateLoc = blockstateLocation(modelId);
        Optional<JsonObject> blockstate = readJson(resources, blockstateLoc);
        if (blockstate.isEmpty()) {
            return false;
        }
        JsonObject json = blockstate.get();
        if (loaderMatches(json, expectedLoader)) {
            return true;
        }
        return blockstateModelsUseLoader(resources, json, expectedLoader);
    }

    private static boolean modelJsonUsesLoader(ResourceManager resources, ResourceLocation modelId, String expectedLoader) {
        ResourceLocation modelLoc = ResourceLocation.fromNamespaceAndPath(
                modelId.getNamespace(),
                "models/" + modelId.getPath() + ".json"
        );
        return readJson(resources, modelLoc).map(json -> loaderMatches(json, expectedLoader)).orElse(false);
    }

    private static ResourceLocation blockstateLocation(ResourceLocation modelId) {
        String path = modelId.getPath();
        if (path.startsWith("block/")) {
            path = path.substring("block/".length());
        }
        return ResourceLocation.fromNamespaceAndPath(modelId.getNamespace(), "blockstates/" + path + ".json");
    }

    private static boolean blockstateModelsUseLoader(ResourceManager resources, JsonObject blockstate, String expectedLoader) {
        if (blockstate.has("variants") && blockstate.get("variants").isJsonObject()) {
            for (Map.Entry<String, JsonElement> entry : blockstate.getAsJsonObject("variants").entrySet()) {
                if (variantEntryUsesLoader(resources, entry.getValue(), expectedLoader)) {
                    return true;
                }
            }
        }
        if (blockstate.has("multipart") && blockstate.get("multipart").isJsonArray()) {
            for (JsonElement part : blockstate.getAsJsonArray("multipart")) {
                if (!part.isJsonObject()) {
                    continue;
                }
                JsonObject obj = part.getAsJsonObject();
                if (obj.has("apply") && variantEntryUsesLoader(resources, obj.get("apply"), expectedLoader)) {
                    return true;
                }
            }
        }
        return false;
    }

    private static boolean variantEntryUsesLoader(ResourceManager resources, JsonElement element, String expectedLoader) {
        if (element.isJsonArray()) {
            for (JsonElement child : element.getAsJsonArray()) {
                if (variantEntryUsesLoader(resources, child, expectedLoader)) {
                    return true;
                }
            }
            return false;
        }
        if (!element.isJsonObject()) {
            return false;
        }
        JsonObject variant = element.getAsJsonObject();
        if (loaderMatches(variant, expectedLoader)) {
            return true;
        }
        if (!variant.has("model") || !variant.get("model").isJsonPrimitive()) {
            return false;
        }
        ResourceLocation modelId = ResourceLocation.parse(variant.get("model").getAsString());
        return modelJsonUsesLoader(resources, modelId, expectedLoader);
    }

    private static boolean loaderMatches(JsonObject json, String expectedLoader) {
        String loader = readLoader(json);
        return loader != null && loader.equalsIgnoreCase(expectedLoader);
    }

    private static String readLoader(JsonObject json) {
        if (json.has("loader") && json.get("loader").isJsonPrimitive()) {
            return json.get("loader").getAsString();
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
        } catch (Exception e) {
            Zeus.LOGGER.debug("Failed reading {}: {}", location, e.toString());
        }
        return Optional.empty();
    }
}