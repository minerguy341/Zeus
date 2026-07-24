package dev.zeus.mixin;

import org.objectweb.asm.tree.ClassNode;
import org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;

import java.util.List;
import java.util.Set;

/**
 * Applies Athena mixins only when Athena is on the classpath.
 * <p>
 * Must not {@link Class#forName} the target: that defines the class before Mixin can
 * transform it, so the suppress mixin never applies and Athena keeps claiming models.
 */
public final class ZeusMixinPlugin implements IMixinConfigPlugin {
    private static final String ATHENA_MIXIN = "dev.zeus.mixin.AthenaResourceLoaderMixin";
    private static final String ATHENA_LOADER_RESOURCE =
            "earth/terrarium/athena/impl/loading/AthenaResourceLoader.class";
    private static final String FUSION_TINT_MIXIN = "dev.zeus.mixin.FusionQuadTintingHelperMixin";
    private static final String FUSION_TINT_RESOURCE =
            "com/supermartijn642/fusion/texture/QuadTintingHelper.class";

    @Override
    public void onLoad(String mixinPackage) {
    }

    @Override
    public String getRefMapperConfig() {
        return null;
    }

    @Override
    public boolean shouldApplyMixin(String targetClassName, String mixinClassName) {
        if (ATHENA_MIXIN.equals(mixinClassName)) {
            return resourceExists(ATHENA_LOADER_RESOURCE);
        }
        if (FUSION_TINT_MIXIN.equals(mixinClassName)) {
            return resourceExists(FUSION_TINT_RESOURCE);
        }
        return true;
    }

    /**
     * Presence check that does not define the class (unlike {@code Class.forName}).
     */
    private static boolean resourceExists(String path) {
        ClassLoader loader = ZeusMixinPlugin.class.getClassLoader();
        if (loader != null && loader.getResource(path) != null) {
            return true;
        }
        ClassLoader context = Thread.currentThread().getContextClassLoader();
        return context != null && context.getResource(path) != null;
    }

    @Override
    public void acceptTargets(Set<String> myTargets, Set<String> otherTargets) {
    }

    @Override
    public List<String> getMixins() {
        return null;
    }

    @Override
    public void preApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {
    }

    @Override
    public void postApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {
    }
}
