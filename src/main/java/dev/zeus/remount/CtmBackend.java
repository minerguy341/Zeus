package dev.zeus.remount;

/**
 * Pluggable CTM target. Config selects one by {@link #id()}; resource packs supply models
 * using {@link #modelLoaderId()}.
 */
public interface CtmBackend {
    /** Config / registry id, e.g. {@code fusion}. */
    String id();

    /** Whether this backend's mod is loaded on the client. */
    boolean isAvailable();

    /**
     * Model loader id that resource packs use for this backend, e.g. {@code fusion:model}.
     * Zeus suppresses Athena when the effective model (or blockstate) uses this loader.
     */
    String modelLoaderId();
}