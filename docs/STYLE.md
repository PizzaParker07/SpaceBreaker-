# Spacebreaker — Art Style Guide (vanilla-close)

The mod must look like it belongs in vanilla Minecraft. A player should not be able to tell where
vanilla ends and Spacebreaker begins. Realistic subject matter, vanilla execution.

All textures are drawn **by code** (`tools/textures`), pixel by pixel. No generated images.
Compare every new texture side by side with the closest vanilla texture before committing
(`tools/textures/preview.py` reads vanilla textures from the local Minecraft jar for comparison only —
never copy vanilla pixels into the repository).

## 1. Palette
- **Few colours.** Blocks: 4–6 colours. Items: 6–9 colours. Ores: host rock + 2–3 ore tones.
  Run `python3 tools/textures/lint.py` — it warns above these limits.
- **Muted.** Match vanilla saturation; no neon. Bright accents only for things that glow.
- **Ramps.** Every material has one ramp in `sbtex/palettes.py`; textures only use ramp colours.
- **Light from the top-left.** Highlights top/left, shade bottom/right.

## 2. Blocks
- Low contrast noise like vanilla stone/tuff/basalt; no busy per-pixel static.
- Geological directionality where it makes sense (basalt: vertical streaks; sediment: horizontal bands).
- Avoid visible tiling: check a 3×3 tile preview.
- **Ores:** small irregular flecks and clusters (1–4 px), 2–3 tones, one light pixel per cluster,
  **no dark outlines or drop shadows**, coverage 15–25 %. Earth ores are overlays on the vanilla host
  texture (stone / deepslate) so they match resource packs.

## 3. Items
- Vanilla silhouettes and proportions (ingots: the diagonal bar; raw materials: lumpy chunk; armour icons:
  thick outline). The outline is the darkest tone of the item's own material (black only where vanilla
  would use it, e.g. armour icons).
- One clear read at 16×16: shape first, detail second.
- 3D in hand only where it adds something (tanks, tools, devices); the inventory icon is always a clean 2D sprite.

## 4. Models
- Chunky, whole-pixel geometry (1/16 steps; 1/32 only for thin trims). No sub-pixel greebles.
- Think vanilla blocks with parts: lantern, grindstone, lectern, bell, blast furnace. Mechanical detail is
  painted into the texture, not modelled.
- Machines: a clear front face (facing), status shown by texture (screens, lights) with animated/emissive
  layers.
- Armour: keeps the vanilla armour volume (head +1 px, body +1 px …) with a few extra parts
  (backpack, visor, lamps). It must read as armour in vanilla's world.

## 5. Light and shaders
- Glowing parts use emissive render types or full-bright quads (and alpha < 255 in the `_s` map).
- Every block texture gets LabPBR `_n` / `_s` maps (via `save(..., spec=...)`).
- Full-screen effects are drawn in the GUI layer (on top of the shader pack's final frame).

## 6. Review
- Every content pack is photographed in the automated showcase (`./gradlew runShowcase -Pscenario=<name>`),
  with and without shaders, and reviewed before it is merged.
