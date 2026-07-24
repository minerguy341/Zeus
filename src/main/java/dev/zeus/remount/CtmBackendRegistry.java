package dev.zeus.remount;

import dev.zeus.Zeus;
import dev.zeus.ZeusConfig;
import dev.zeus.remount.fusion.FusionBackend;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Registry of CTM backends. Expand here when adding Continuity / others.
 */
public final class CtmBackendRegistry {
    private static final Map<String, CtmBackend> BACKENDS = new LinkedHashMap<>();

    static {
        register(new FusionBackend());
    }

    private CtmBackendRegistry() {
    }

    public static void register(CtmBackend backend) {
        BACKENDS.put(backend.id().toLowerCase(), backend);
        Zeus.LOGGER.debug("Registered CTM backend {}", backend.id());
    }

    public static Optional<CtmBackend> active() {
        return Optional.ofNullable(BACKENDS.get(ZeusConfig.ctmBackend.toLowerCase()));
    }

    public static Collection<CtmBackend> all() {
        return BACKENDS.values();
    }
}
