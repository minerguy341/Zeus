package dev.zeus.mixin;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.BiomeColors;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Coerce;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Fusion item rendering calls {@code QuadTintingHelper.getColor} with a null world/pos,
 * which forces the default grass/foliage color. When that happens, substitute the
 * client player's biome colors so held/inventory Fusion overlays track the live biome.
 * <p>
 * Visual Overhaul cannot do this: it only registers vanilla {@code ItemColors}, and
 * Fusion's magic tintIndex path never consults those providers.
 */
@Mixin(targets = "com.supermartijn642.fusion.texture.QuadTintingHelper", remap = false)
public class FusionQuadTintingHelperMixin {
    @Inject(
            method = "getColor(Lcom/supermartijn642/fusion/api/texture/types/base/BaseTextureData$QuadTinting;Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/world/level/BlockAndTintGetter;Lnet/minecraft/core/BlockPos;)I",
            at = @At("HEAD"),
            cancellable = true,
            remap = false
    )
    private static void zeus$liveBiomeWhenNoWorldPos(
            @Coerce Object tinting,
            BlockState state,
            BlockAndTintGetter level,
            BlockPos pos,
            CallbackInfoReturnable<Integer> cir
    ) {
        if (level != null && pos != null) {
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        if (player == null || mc.level == null || !(tinting instanceof Enum<?> tintEnum)) {
            return;
        }

        BlockAndTintGetter world = mc.level;
        BlockPos at = player.blockPosition();
        int color;
        switch (tintEnum.name()) {
            case "BIOME_GRASS" -> color = BiomeColors.getAverageGrassColor(world, at);
            case "BIOME_FOLIAGE" -> color = BiomeColors.getAverageFoliageColor(world, at);
            case "BIOME_WATER" -> color = BiomeColors.getAverageWaterColor(world, at);
            default -> {
                return;
            }
        }
        cir.setReturnValue(color | 0xFF000000);
    }
}