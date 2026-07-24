package dev.zeus.mixin;

import com.google.gson.JsonObject;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import dev.zeus.remount.AthenaSuppress;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import org.spongepowered.asm.mixin.Mixin;

/**
 * When a resource pack already specifies the configured CTM backend's model loader,
 * prevent Athena from claiming that model via {@code getData}.
 * <p>
 * Athena re-asserts in {@code AthenaModelLoadingPlugin}: {@code modifyModelBeforeBake}
 * replaces the pack's unbaked model whenever {@code getData} returns non-null.
 */
@Mixin(targets = "earth.terrarium.athena.impl.loading.AthenaResourceLoader", remap = false)
public class AthenaResourceLoaderMixin {
    @WrapMethod(method = "reload", remap = false)
    private static void zeus$clearSuppressCacheOnReload(ResourceManager manager, Operation<Void> original) {
        AthenaSuppress.clearCache();
        original.call(manager);
    }

    @WrapMethod(
            method = "getData(Lnet/minecraft/resources/ResourceLocation;Lnet/minecraft/resources/ResourceLocation;)Lcom/google/gson/JsonObject;",
            remap = false
    )
    private static JsonObject zeus$skipWhenPackProvidesBackendLoader(
            ResourceLocation modelType,
            ResourceLocation modelId,
            Operation<JsonObject> original
    ) {
        // Athena calls getData for every model x every loader. Only pay for pack I/O
        // when Athena would actually claim the model.
        JsonObject data = original.call(modelType, modelId);
        if (data == null) {
            return null;
        }
        if (AthenaSuppress.shouldSuppress(modelId)) {
            return null;
        }
        return data;
    }
}