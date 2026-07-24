package dev.zeus.remount.fusion;

import dev.zeus.remount.CtmBackend;

/**
 * Fusion Connected Textures — packs use {@code "loader": "fusion:model"}.
 */
public final class FusionBackend implements CtmBackend {
    @Override
    public String id() {
        return "fusion";
    }

    @Override
    public boolean isAvailable() {
        /*? if fabric {*/
        return net.fabricmc.loader.api.FabricLoader.getInstance().isModLoaded("fusion");
        /*?} else {*/
        /*return net.neoforged.fml.ModList.get().isLoaded("fusion");*/
        /*?}*/
    }

    @Override
    public String modelLoaderId() {
        return "fusion:model";
    }
}