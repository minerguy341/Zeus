# Zeus

Client-side Minecraft mod that lets **resource packs** replace **Athena CTM** (e.g. Chipped) with another CTM backend such as **Fusion**. Zeus does not generate models or packs — it only stops Athena from winning the bake when a pack already specifies the configured loader.

## Why

Chipped ships connected textures through [Athena](https://modrinth.com/mod/athena-ctm). Athena can still claim models even when a resource pack supplies Fusion (`"loader": "fusion:model"`), which blocks Fusion-only features (overlays, biome tint CTM, etc.). Zeus:

1. Reads `config/zeus.toml` for namespaces + replacement backend
2. On Athena `getData`, checks the effective model / blockstate from the resource manager
3. If that JSON uses the backend's loader (e.g. `fusion:model`) under a remount namespace, returns `null` so Athena stands down

## Config (`config/zeus.toml`)

```toml
remountNamespaces = "chipped"
ctmBackend = "fusion"
stripAthenaWhenBackendMissing = true
```

| Option | Meaning |
|--------|---------|
| `remountNamespaces` | Namespaces where Athena may be suppressed |
| `ctmBackend` | Backend id → expected model loader (`fusion` → `fusion:model`) |
| `stripAthenaWhenBackendMissing` | If the backend mod is absent, still suppress Athena when a pack uses its loader |

## Resource pack requirements

Packs must set the backend loader on the **model** (or blockstate), for example:

```json
{
  "loader": "fusion:model",
  "type": "connecting",
  "parent": "minecraft:block/cube_all",
  "textures": { "all": "…" },
  "connections": [{ "type": "is_same_block" }]
}
```

Blockstates that only point at such models (without `athena:*`) are enough — Zeus follows `variants` / `multipart` model refs. Packs that only append Fusion overlays via model modifiers **without** replacing the Athena model leave Athena in control.

## Dev

- Stonecutter nodes: `1.21.1-fabric`, `1.21.1-neoforge`
- JDK 21: `JAVA_HOME="C:\Program Files\Java\jdk-21.0.11"`
- Drop Fabric jars into **`mods/`** (project root). Loom remaps them for the Mojmap client.
  - Needed: Athena, Fusion, Chipped, Resourceful Lib
  - Also put **bytecodecs** and **yabn** in `mods/` — Resourceful Lib JiJ bundles them, but Loom `modLocalRuntime` flat jars are not unpacked, so those libraries must be present separately.
  - Do **not** put intermediary jars in `run/mods` — that folder is not remapped and will crash

```bash
./gradlew :1.21.1-fabric:runClient
```

## Status

- Pack-driven Athena suppress for Fusion: implemented
- Extra backends: register via `CtmBackendRegistry` with `modelLoaderId()`
