# Zeus

Client-side Minecraft mod that helps **resource packs** use **Fusion** (and similar CTM backends) in place of **Athena CTM** — for example on [Chipped](https://modrinth.com/mod/chipped) mossy blocks.

Zeus does **not** author models or packs. It suppresses Athena when a pack already declares the configured backend loader, and (optionally with Fusion present) makes Fusion’s item biome tints follow the player’s biome.

## Features

### 1. Pack-driven Athena suppress

Chipped ships connected textures through [Athena](https://modrinth.com/mod/athena-ctm). Athena can still claim models even when a pack supplies Fusion (`"loader": "fusion:model"`), which blocks Fusion-only features (overlays, `tinting: biome_grass`, etc.).

Zeus:

1. Reads `config/zeus.toml` for namespaces + replacement backend
2. On Athena `getData`, checks the effective model / blockstate from the resource manager
3. If that JSON uses the backend’s loader (e.g. `fusion:model`) under a remount namespace, returns `null` so Athena stands down

### 2. Live Fusion item biome tint

Fusion texture `tinting` (`biome_grass` / `biome_foliage` / `biome_water`) works for **placed blocks** using world position. For **items** (hand, inventory, GUI), Fusion calls its tint helper with a null position and applies a **fixed default** grass/foliage color.

When Fusion is present, Zeus mixins that helper so a null world/pos uses the **client player’s biome** instead. Pack overlays with `"tinting": "biome_grass"` then track live biome tint in hand/hotbar the same way placed blocks do.

- Soft dependency: mixin applies only if Fusion is on the classpath
- Does **not** go through Visual Overhaul — VO only registers vanilla `ItemColors`; Fusion’s item tint path never uses those

### 3. Debug commands

Client commands (Fabric / NeoForge):

| Command | Purpose |
|---------|---------|
| `/zeus loader` | Probe the **looked-at block**: effective model loaders, Athena `getData`, baked model walk, Zeus suppress decision |
| `/zeus loader item` | Probe the **held item** (main hand, else offhand): item/composite model JSON, baked Fusion tree, Fusion tintIndex / sprite tinting diagnostics |
| `/zeus help` | Short command list |

Probe lines are also written to the log as `[probe] …`.

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

For biome-tinted overlays on blocks **and** items:

```json
{
  "fusion": {
    "type": "base",
    "render_type": "cutout",
    "tinting": "biome_grass"
  }
}
```

Item models that only `"parent"` a block do **not** pick up Fusion block `append_models` overlays. Prefer Fusion **composite** item models (base + overlay) rather than item `append_models` (third-person hand often breaks with append).

Blockstates that only point at Fusion models (without `athena:*`) are enough — Zeus follows `variants` / `multipart` model refs. Packs that only append Fusion overlays via model modifiers **without** replacing the Athena model leave Athena in control.

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
- Live Fusion item biome tint: implemented (Fusion soft-depend mixin)
- Debug probes (`/zeus loader`, `/zeus loader item`): implemented
- Extra backends: register via `CtmBackendRegistry` with `modelLoaderId()`

## License

Zeus code is **MIT** (Copyright 2026 minerguy341). See [LICENSE](LICENSE).

Terrarium retains All Rights Reserved on Chipped / related **non-code assets** (textures, models, etc.) under the [Terrarium License](https://github.com/terrarium-earth/Chipped/blob/1.21.x/LICENSE). Zeus does not relicense those assets.
