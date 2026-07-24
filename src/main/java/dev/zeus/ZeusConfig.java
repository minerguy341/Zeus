package dev.zeus;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * Flat TOML config at {@code config/zeus.toml}. Rewritten after every load so docs stay current.
 */
public final class ZeusConfig {
    /** Comma-separated asset namespaces where Athena may be suppressed for pack-driven loaders. */
    public static String remountNamespaces = "chipped";
    /** Active CTM backend id. Default: fusion. Resource packs must use that backend's model loader. */
    public static String ctmBackend = "fusion";
    /**
     * When the configured backend mod is missing, still suppress Athena if a pack uses that
     * backend's loader. When false, Athena is never suppressed without the backend mod.
     */
    public static boolean stripAthenaWhenBackendMissing = true;

    private static long loadedModified = -1;
    private static Set<String> namespaceCache = Set.of("chipped");

    private ZeusConfig() {
    }

    public static Set<String> namespaces() {
        return namespaceCache;
    }

    public static boolean remounts(String namespace) {
        return namespaceCache.contains(namespace);
    }

    public static boolean fusionBackend() {
        return "fusion".equalsIgnoreCase(ctmBackend);
    }

    private static Path file() {
        /*? if fabric {*/
        return net.fabricmc.loader.api.FabricLoader.getInstance().getConfigDir().resolve("zeus.toml");
        /*?} else {*/
        /*return net.neoforged.fml.loading.FMLPaths.CONFIGDIR.get().resolve("zeus.toml");*/
        /*?}*/
    }

    public static void load() {
        Path path = file();
        try {
            if (Files.exists(path)) {
                apply(parseFlatToml(Files.readString(path)));
            }
            refreshNamespaceCache();
            write(path);
            loadedModified = Files.getLastModifiedTime(path).toMillis();
            Zeus.LOGGER.info(
                    "Config loaded: remountNamespaces={}, ctmBackend={}, stripAthenaWhenBackendMissing={}",
                    remountNamespaces, ctmBackend, stripAthenaWhenBackendMissing
            );
        } catch (Exception e) {
            Zeus.LOGGER.warn("Could not read config {}; using defaults", path, e);
            refreshNamespaceCache();
        }
    }

    public static void maybeReload() {
        try {
            Path path = file();
            if (Files.exists(path) && Files.getLastModifiedTime(path).toMillis() != loadedModified) {
                load();
            }
        } catch (IOException ignored) {
        }
    }

    private static void refreshNamespaceCache() {
        LinkedHashSet<String> set = new LinkedHashSet<>();
        for (String part : remountNamespaces.split(",")) {
            String ns = part.trim().toLowerCase(Locale.ROOT);
            if (!ns.isEmpty()) {
                set.add(ns);
            }
        }
        if (set.isEmpty()) {
            set.add("chipped");
        }
        namespaceCache = Set.copyOf(set);
    }

    private static void apply(Map<String, String> values) {
        if (values.containsKey("remountNamespaces")) {
            remountNamespaces = values.get("remountNamespaces");
        }
        if (values.containsKey("ctmBackend")) {
            ctmBackend = values.get("ctmBackend");
        }
        stripAthenaWhenBackendMissing = parseBool(
                values.get("stripAthenaWhenBackendMissing"),
                stripAthenaWhenBackendMissing
        );
    }

    private static Map<String, String> parseFlatToml(String content) {
        Map<String, String> values = new HashMap<>();
        for (String rawLine : content.split("\\R")) {
            String line = rawLine.trim();
            if (line.isEmpty() || line.startsWith("#") || line.startsWith("[")) {
                continue;
            }
            int eq = line.indexOf('=');
            if (eq <= 0) {
                continue;
            }
            String key = line.substring(0, eq).trim();
            String value = line.substring(eq + 1).trim();
            if (value.startsWith("\"")) {
                int close = value.indexOf('"', 1);
                if (close > 0) {
                    value = value.substring(1, close);
                }
            } else {
                int hash = value.indexOf('#');
                if (hash >= 0) {
                    value = value.substring(0, hash).trim();
                }
            }
            values.put(key, value);
        }
        return values;
    }

    private static void write(Path path) throws IOException {
        StringBuilder out = new StringBuilder();
        out.append("# Zeus — let resource packs replace Athena CTM with another backend.\n");
        out.append("# Zeus does not generate models; packs supply them. Restart / F3+T after edits.\n\n");

        out.append("# Asset namespaces where Athena may be suppressed, comma-separated.\n");
        out.append("# Default: \"chipped\". Athena is only skipped when a pack uses the backend loader.\n");
        out.append("remountNamespaces = \"").append(remountNamespaces).append("\"\n\n");

        out.append("# Replacement CTM backend. Default: \"fusion\" (loader fusion:model).\n");
        out.append("# Accepted: fusion (more backends later).\n");
        out.append("ctmBackend = \"").append(ctmBackend).append("\"\n\n");

        out.append("# If the backend mod is missing, still suppress Athena when a pack uses its loader.\n");
        out.append("# Default: true. Accepted: true, false.\n");
        out.append("stripAthenaWhenBackendMissing = ").append(stripAthenaWhenBackendMissing).append("\n");

        Files.createDirectories(path.getParent());
        Files.writeString(path, out.toString());
    }

    private static boolean parseBool(String value, boolean fallback) {
        return value == null ? fallback : Boolean.parseBoolean(value);
    }
}