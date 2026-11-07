# Spacebreaker — Renderer compatibility (Sodium, Iris, shader packs)

Owner's rule: **the mod must work with Sodium and with Iris shader packs.** Both are optional — the mod never
needs them at compile time or at run time.

| Setup | Status |
|---|---|
| Vanilla renderer | reference look |
| Sodium 0.8 (no shaders) | works: terrain, block entity renderers (solar panel tracker, tanks, consoles), translucent glass/visor, emissive layers look the same as vanilla |
| Sodium + Iris 1.8 + Complementary Reimagined r5.9.3 | works: black space sky, stars, Sun, Earth with phases from the Moon, planet below in orbit, Mars sky; terrain lit by the pack (sun shadows follow the world's own sun) |
| Sodium + Iris + BSL v10.1.8 | works (same fixes, checked shot by shot) |
| Sodium + Iris + MakeUp Ultra Fast 9.6 | works (same fixes; the ground is a little cooler/lavender than vanilla) |

Per world with Complementary (vanilla reference shots live in each area's screenshot folder):

| World | Kind | Shader result |
|---|---|---|
| Moon | airless | black sky, stars, Earth with phases, grey regolith, sun shadows |
| Earth orbit / Moon orbit | orbit | planet below, Sun, stars; station plates neutral grey |
| Mercury | airless | huge Sun disc, grey crust at noon and at sunrise (`iris_mercury_*`) |
| Mars | thin | own butterscotch sky, blue sunset; dust storm haze (`iris_mars_11_dust_storm` vs `vanilla_mars_11_dust_storm`), a little brighter than vanilla |
| Venus | thick | dark ground, dense orange haze, whiteout in the clouds, cloud sea above (`venus_vanilla_bug_fixed.jpg`: vanilla / before / after) |
| Titan | thick | orange haze and dim light from the generic haze pass, no Titan code (`iris_titan_*` vs `vanilla_titan_01_dunes`); ground a little warmer than vanilla, the small methane drizzle particles are hard to see under the pack |
| Pluto, Charon | airless | black sky, Charon/Pluto in the sky, Pluto's blue haze layers (drawn by its sky, so the sky pass carries them) (`iris_pluto_*`, `vanilla_pluto_05_blue_haze`); ground a little cooler than vanilla |
| Triton | airless | Neptune with its terminator, nitrogen geysers (`iris_triton_*`) |
| Enceladus | airless | black sky with Saturn and its rings, crescent phase (`iris_enceladus_*`): same as vanilla |
| Europa, Io (and Ganymede, Callisto) | airless | black sky, stars, the huge Jupiter with its terminator, Io's plumes (`iris_europa_01_jupiter`, `iris_io_02_sulfur_plume`); as dark as vanilla where Jupiter's night side faces the ground |
| Phobos, Deimos | airless | classified and handled like the Moon; no separate shader screenshots yet |

Screen effects and HUDs (all in the GUI layer, drawn over the pack's final frame) were checked under Complementary:
rocket camera and cockpit window (`iris_rocketry_24_rocket_camera`, `iris_rocketry_25_cockpit`), the transit animation
that replaces the loading screen during dimension changes (`iris_rocketry_26/27/28_transit_*`; the frames differ from
vanilla only in the animation phase, the shader run is slower), orbit HUD (`iris_rocketry_12_orbit_hud`), suit HUD on the Moon (`iris_suit_moon_03_hud_vacuum`), launch
(`iris_rocketry_07_ignition`), Mars dust tint. The reentry plasma overlay (`iris_13_reentry_plasma`, compat shot 13 with
a short view distance so the frame is ready during the heating phase) is drawn over the pack's frame as well.

Sodium only: the compat scenario shots match vanilla; chunk meshing of freshly generated Moon and Mars terrain at
12 chunks finished within the same minimum shot wait as vanilla (no stalls or timeouts). No deeper profiling done.

Screenshots: `docs/screenshots/compat/` (`vanilla_*`, `sodium_*`, `iris_*` = Complementary, `bsl_*`, `makeup_*`;
`iris_bug_*` are the state before the fix, `before_after_complementary.jpg` puts both side by side). All taken with
Sodium 0.8.13 + Iris 1.8.14-beta.1 on Mesa llvmpipe (software GL 4.5).

## 1. The problem

Iris chooses a shader pack's program set from the dimension id (`Iris.getCurrentDimension()`): first the pack's
`dimension.properties`, then `effects = minecraft:the_end / the_nether`, otherwise the id itself — which packs map to
the Overworld programs (Complementary: `dimension.world0=*`). The Overworld programs then

1. paint their own atmosphere over every pixel without geometry (`gbuffers_skybasic` overwrites colours,
   `deferred1` adds volumetric clouds and aurora) — the sky our `DimensionSpecialEffects` draws never reaches the
   screen: brown/grey sky with clouds on the Moon, grey void in orbit with no planet below;
2. read vanilla's weather (space worlds inherit the Overworld's rain in vanilla) — wet, rainy Moon;
3. light the ground with their warm, atmosphere-filtered sun plus an ambient fill derived from the `skyColor`
   uniform — black biome sky = no fill, grey regolith looks like sand.

**Why not the End program?** Mapping airless worlds to `world1` was examined in Complementary's source: the End
`deferred1` overwrites every sky pixel with the purple End sky and End stars, `gbuffers_skytextured` outputs a flat
colour, and terrain gets a dim purple light with no day/night. The Moon needs hard white sunlight and shadows that
follow the Sun, which only the Overworld programs give. So we keep the Overworld programs and correct their inputs.

## 2. How it works (package `compat/`)

| Piece | What it does |
|---|---|
| `SpaceWorlds` | classifies every dimension from its celestial body JSON: `ORBIT`, `AIRLESS` (vacuum), `THIN_ATMOSPHERE` (< 20 kPa, Mars), `THICK_ATMOSPHERE` (Venus, Titan), `VANILLA` (Overworld, Nether, End, unknown). New worlds are covered by adding `dimension` / `orbit_dimension` to their body JSON — a GameTest fails if a `spacebreaker:` dimension has no body. |
| `client/ShaderSkyPass` | **sky replacement.** Iris renders into Minecraft's own depth buffer, which still holds the scene depth after the pack's final pass. NeoForge fires `RenderLevelStageEvent.Stage.AFTER_LEVEL` in `GameRenderer` right after `LevelRenderer.renderLevel` returns — after Iris's composite/final passes, before the depth clear for the hand. There our normal sky (`level.effects().renderSky`, unchanged vanilla code) is drawn into an off-screen target and copied onto the frame with `glDepthRange(1,1)` + `GL_LEQUAL`, i.e. only where the depth is still the cleared far value: the sky. Terrain, entities and the hand keep the pack's shading. Works with any pack, no pack-specific code. |
| `client/HazePass` | **thick and dusty air.** Packs ignore vanilla fog and the light map, so Venus was a bright beige desert. At `AFTER_SOLID_BLOCKS` the terrain fog the world set up this frame is captured (start, end, shape, colour — after every mod's `ViewportEvent` fog handlers); the world's own `adjustLightmapColors` gives its full-daylight colour. After the pack's final pass (and after the sky pass) the depth buffer is copied (`glCopyTexSubImage2D`, avoids a feedback loop and converts formats) and the core shader `spacebreaker:compat_haze` applies `frame * tint * (1 - f) + fog * f` to scene pixels only (two blended draws). Thick atmospheres get haze + tint; thin ones (Mars) get haze only while their fog is closer than 80 % of the render distance (dust storms). Generic: Titan or any new world uses its own fog and light map. |
| dawn white balance (`HazePass.dawnBalance`) | packs colour sunlight by the sun's height as if it shone through Earth's air, so the Moon and Mercury turned orange/pink at sunrise and sunset. Packs take the height from vanilla's sun angle; from the same angle the pass pulls red and green back toward blue on scene pixels of airless worlds (up to −42 % / −18 % at the horizon, nothing above ~40° or at night). A multiply can only darken, which suits a grazing sun. Checked with Complementary on the Moon and Mercury. |
| `mixin/iris/PackCloudsMixin` | `DimensionSpecialEffects.getCloudHeight` → 25000 in space worlds while a pack is active. Iris passes it as `cloudHeight`; pack clouds move beyond their draw distance (no clouds in front of distant terrain, no cloud shadows). Our sky effects draw clouds themselves, vanilla never reads it there. |
| `mixin/iris/PackAmbientMixin` | `ClientLevel.getSkyColor` → a cool fill in airless worlds while a pack is active (Iris passes it as `skyColor`). Packs use it differently: Complementary adds `pow(skyColor, 0.75)` to a warm, atmosphere-filtered sun and needs (0.20, 0.30, 0.72) to bring grey regolith back to neutral; MakeUp mixes 60 % of it into an already neutral ambient, so other packs get a moderate (0.20, 0.27, 0.55); BSL ignores it. The pack family is read from Iris's current pack name (internal method, by reflection; unknown → moderate value). Mars keeps its biome sky colour (butterscotch fill is right there). |
| `mixin/ClientWeatherMixin` | client-side `getRainLevel` / `getThunderLevel` = 0 in space worlds (always, with or without Iris). Server-side weather is untouched. |
| `CompatMixinPlugin` | the `iris.*` mixins are only applied when Iris (or Oculus) is installed: without it the game runs the exact vanilla code paths. |
| `client/ShaderState` | Iris is reached only through its public API `net.irisshaders.iris.api.v0.IrisApi` (`isShaderPackInUse`) by reflection. |

Full-screen effects and HUDs stay in the GUI layer (drawn on top of the pack's final frame) — unchanged.

### Settings — `config/spacebreaker-compat-client.toml`
```
[shaders]
spaceSky = "AUTO"        # AUTO | ALWAYS | OFF
hidePackClouds = true
atmosphereHaze = true    # haze/tint/dawn pass (two full-screen draws); off on slow GPUs
```
`AUTO` replaces the pack's sky in space worlds; with Distant Horizons it also tests DH's depth texture, with Voxy
(no depth API) it stays off. `ALWAYS` does it anyway; `OFF` leaves the pack alone.

### Drawing "right after the sky"
Code that draws sky decorations at `RenderLevelStageEvent.Stage.AFTER_SKY` (planet dots, the ascent darkening) works
with shader packs too: on vanilla-sky worlds (Earth) the pack keeps them; in our space worlds `ShaderSkyPass` replays
the AFTER_SKY stage into its off-screen sky, so they come back on top of the pack's frame
(`iris_planet_dots_mars_earth`, `iris_planet_dots_earth_jupiter`). `ShaderSkyPass.replayingAfterSky()` tells a
listener which of the two calls it is in (draw nothing that is not sky, e.g. no depth writes).

## 2b. Holograms and previews: `compat/client/GhostBlocks`

For structure projectors, placement previews and other "ghost" blocks: `GhostBlocks.render(poseStack, buffers, state,
r, g, b, alpha)` at `RenderLevelStageEvent.Stage.AFTER_TRANSLUCENT_BLOCKS` (pose stack translated to the camera,
`Minecraft.getInstance().renderBuffers().bufferSource()`), then `GhostBlocks.flush(buffers)` once per frame. It draws
the block's baked model through `Sheets.translucentCullBlockSheet()` with scaled alpha and full-bright light, so Iris
batches it as a translucent entity and packs shade it with their translucent program (correct alpha and sorting).
Avoid immediate-mode position/colour drawing for world-space holograms: packs may discard or recolour it.
The Hologram Projector (`client/blueprint`) uses the same technique (translucent block sheet, tinted, alpha) plus
`RenderType.lines()` outlines; checked with Complementary: `iris_blueprint_01_launch_site`, `iris_blueprint_04_habitat`.
`vanilla_14_ghost_blocks` / `iris_14_ghost_blocks`: a 4×2×2 ghost (steel, tanks, glass) between the prop rows; with
Complementary the tint is weaker (the pack re-lights translucent entities) but the ghost stays clearly visible.

## 3. Known limitations

- **Ground tint differs slightly per pack:** the ambient fill above is a compromise per pack family; packs not
  tested here may look a little warm (sandy) or cool (lavender) on the Moon by day.
- **Haze strength:** the pack's own (brighter) lighting sits under our haze, so Mars storms read a little lighter
  than vanilla at mid distance; Venus matches closely. Translucent blocks and particles in front of terrain are hazed
  by the depth of what is behind them.
- **Low sun on airless worlds:** the dawn white balance removes most of the packs' orange; the result is a little
  cool and darker than vanilla right at the horizon. Packs that tint dawn much more or less than Complementary are
  only partly corrected.
- **Sky seen through glass or water** is the pack's (translucent geometry writes depth before the sky pass). Looking
  out of a glass window into space shows the pack's sky colour behind the glass.
- **Pack bloom/lens flare** around the pack's own sun can bleed a little onto terrain silhouettes; the Sun disc
  itself is ours.
- **Distant Horizons / Voxy:** their far terrain lives in its own depth buffer, not in Minecraft's.
  - Distant Horizons: `LodDepth` reads DH's depth texture through DH's public API (`DhApi.Delayed.renderProxy
    .getDhDepthTextureId()`, the same texture Iris attaches to its DH framebuffers) and its far value; the sky
    composite (core shader `compat_sky_composite`) keeps pixels where DH drew terrain, and the haze pass treats them
    as beyond the render distance (fully hazed on Venus/Titan). With that, `AUTO` stays on with DH.
  - Tested with DH 3.3.3 + Iris + Complementary before the DH depth support: `AUTO` then switched our passes off and
    the Moon showed the pack's blue Overworld sky (`dh_auto_moon_before_lod_support`); `ALWAYS` gave the right sky
    (`dh_always_moon`).
  - Verified with the DH depth support (DH 3.3.3 + Iris + Complementary, `AUTO`, fresh world,
    `-Pscenario=lod -PwithCompat=true`): on the Moon the LOD terrain beyond the 6-chunk view stays visible under the
    black sky and stars (`dh_lod_moon_verified`); on Venus it is covered by the thick haze as intended
    (`dh_lod_venus_verified`); no errors from our passes in the log. The curved horizon and the dithered fade
    between real and LOD chunks on the Moon are DH's own settings. If DH's texture cannot be read, `AUTO` falls
    back to off as before.
  - Voxy: no depth API; `AUTO` keeps the passes off, `ALWAYS` paints over its terrain.
- **Sun direction vs. shadows:** packs cast shadows from vanilla's sun path (plus their own `sunPathRotation`); our
  drawn Sun also tilts with the observer's latitude. Near the poles the two differ by up to the latitude.
- Shader packs that use a different dimension mapping (custom `dimension.properties` entries for `spacebreaker:*`)
  get the program set they ask for; the sky replacement still applies.
- Not part of this layer, noticed while testing: server-side, `level.isRaining()` on the Moon follows the
  Overworld's weather (vanilla's derived level data). `SolarPanelBlockEntity` uses it — solar output drops on the
  Moon when it rains on Earth (industry area).

- **Blue "water" particles are faded by Complementary:** its `gbuffers_textured` recognises blue particles
  (`b > 1.15·(r+g)`, as vanilla `FALLING_WATER` / `RAIN`), pales them and drops ~60 % of them by dither. Titan's drizzle
  now uses its own methane drop particle and is visible under the pack (`iris_titan_03_methane_rain`); it reads darker
  than in vanilla because Titan's dim light and haze apply to the drops too. Other areas: avoid blue-dominant vanilla
  water particles for effects that must stay visible with shaders.

- **Light pools / added light (helmet lamps):** re-drawing lit blocks full-bright through a translucent type works,
  but packs light full-bright vertices as torch light, so the pool turns orange (`iris_eva_lamp_pool_current`).
  Drawn additively with `RenderType.eyes(TextureAtlas.LOCATION_BLOCKS)` (packs: emissive "eyes" program) it stays
  white under Complementary and looks the same in vanilla (`iris_eva_lamp_pool_eyes`): recommended for light effects.
  The safety tether (`RenderType.leash()`) and the MMU gas puffs (particles) are visible under the pack
  (`iris_eva_tether`, `iris_eva_mmu`).
- **Rovers, satellites, station (checked with Complementary):** rover headlights and light cones show on the Moon and
  on Mars at night, wheel tracks stay visible, the HUD is unchanged (`rover_moon_vanilla_vs_iris`,
  `rover_mars_vanilla_vs_iris`). Satellites in orbit, the station and the capsule render as without shaders
  (`rocketry_satellites_station_vanilla_vs_iris`). The rover's blob shadow is replaced by the pack's real shadow.
  Station window: its texture is semi-transparent but the model is `cutout`, so vanilla draws an opaque blue pane while
  the pack shows it dark (`rocketry_station_window`); it is `translucent` now (see the next point).
  Habitat module, Heat Siphon at night and the solar array: `habitat_siphon_solar_vanilla_vs_iris.jpg`.
- **Windows, water and other translucent surfaces in front of the sky:** they write depth, so the sky pass alone
  left the pack's (pale, Overworld) sky behind them. Iris keeps a copy of the depth from just before its translucent
  pass (the pack's `depthtex1`: terrain and entities, no glass or water), read by reflection in
  `ShaderState.opaqueDepthTexture()`; where only translucent geometry covers the sky, our sky is blended in and the
  pack's glass colour is kept at 35 % (`compat_sky_glass`). (A copy of our own at `AFTER_BLOCK_ENTITIES` was tried
  first: Iris draws entities after that point, so rockets in front of the sky turned dark.) After the translucent
  pass the opaque frame of a window cannot be told from its pane, so a block whose texture mixes an opaque frame with
  a see-through pane puts them in two layers (NeoForge `neoforge:composite` model: frame `cutout`, pane
  `translucent`), as the station window does. `habitat_window_steps.jpg`: vanilla, pack before, after, and with the
  split model; `station_jolt_coldtrap_aerostat_vanilla_vs_iris.jpg`: habitat inside, docked capsule, docking jolt,
  polar cold-trap crater, Venus aerostat.
- **Day and night in orbit:** orbits have their own day and night (the planet's shadow, `OrbitDaylight`), shown
  through our sky and the world's light map, which packs ignore: the pack lit the station from vanilla's sun angle,
  so it stayed in full sun on the night side. While a pack is active in an orbit world, Iris's sun angle and
  worldTime follow the orbit instead (`compat/client/PackTime`, mixins `iris.PackSunMixin` and
  `iris.PackWorldTimeMixin`): orbital noon in sunlight, midnight in the shadow, dusk across the penumbra
  (`orbit_day_night_vanilla_vs_iris.jpg`). Without a pack nothing changes.
- **Very dark and very bright surfaces (Uranus' moons):** checked with Complementary on Umbriel (dark plains, the
  bright Wunda floor) and Oberon (dark-floored crater, mountain, chasma): the pack's exposure keeps both ends: Wunda
  stays white without blowing out, the dark crater floor stays clearly darker than its rim, nothing turns black.
  As on the Moon, the pack's ground is brighter and a little warmer than vanilla (`umbriel_habitat_night_vanilla_vs_iris`,
  `oberon_vanilla_vs_iris`). The habitat module at night (lamp inside) looks as without shaders, with the night
  sky in the windows (last row of the Umbriel sheet).
- **Lunar shuttle on a landing beacon pad, Venus descent probes, regolith excavator:** same as without shaders
  (`shuttle_probe_excavator_vanilla_vs_iris.jpg`); the probes under their parachutes take Venus' daylight tint and
  haze like the ground around them. Showcase note: `server.tell(new TickTask(t, ...))` does not wait for tick `t`
  (it only defers under load); delayed showcase actions use `RocketryScenario.later`. With a pack the client is
  much slower than the server, so moving things are released continuously rather than timed to the frame.
- **Mars dust storm:** the storm closes vanilla's terrain fog in to 22 blocks; the haze pass captures exactly that
  (start 0, end 22 at full strength) and applies it over the pack's frame. The pack adds next to no fog of its own
  there, so the two do not stack. With a pack the storm looks a little thinner than vanilla because the pack lights
  the ground brighter under the same fog amount and colour. Dust devils render and cast pack shadows.
  `mars_storm_haze_steps.jpg`: vanilla, pack with `atmosphereHaze = false`, pack with the haze, dust devil.
- **Mars lander and the launch console:** the lander on Mars looks as without shaders; the console is a screen,
  drawn after the pack's frame, so its rows (including the cargo route row of uncrewed cargo rockets) cannot be
  affected by a pack (`mars_lander_console_vanilla_vs_iris.jpg`).
- **Micrometeorite streaks and tiny worlds:** the streaks are drawn right after the sky (AFTER_SKY), which the sky
  pass replays, so they show with a pack; at very low frame rates there are fewer of them (one spawn batch per
  frame, reported to the planet engine area). Dark Bennu and bright Hydra keep their contrast under the pack's
  exposure: Bennu's rubble stays dark but readable, Hydra's ice is bright without blowing out
  (`meteor_bennu_hydra_vanilla_vs_iris.jpg`).
- **Comets:** in space worlds the comet tails are part of our sky. On the Overworld the pack keeps its own sky
  and clouds: Halley in 2061 is drawn, its dust tail partly hidden behind the pack's blocky night clouds (they are in
  front of it, as clouds are). 67P's surface and jets look as without shaders (`comets_vanilla_vs_iris.jpg`).
- **Base blocks (pressurised glass, porthole, LED panels, sliding door, warning light):** the glass is see-through and
  tinted under the pack, the LED panels light the corridor at night as in vanilla, door and warning light render
  (`base_blocks_vanilla_vs_iris.jpg`). The rotating warning-light beam was not caught in a still frame.
- **Glows without depth (rocket plumes, beams) in front of the sky:** emissive translucent render types
  (`entityTranslucentEmissive`, `eyes`) write no depth, so the sky pass, which copies our sky wherever the depth is
  still far, painted over them (the NTR plume vanished under the pack). `compat/client/LateEffects.drawOrDefer(pose,
  buffers, (p, b) -> ...)` draws such an effect at once without a pack, and with the sky pass active queues it and
  draws it after our sky and haze, tested against the scene depth (`ntr_plume_late_effects.jpg`: vanilla, pack before,
  pack with the deferral). Effects that do write depth (glass, water, ice) get our sky blended in behind them; that
  blend now only covers translucent blocks (depth after AFTER_TRANSLUCENT_BLOCKS), not translucent entities.
- **Mars clouds, canyon fog, seasonal frost:** clouds are part of our sky; the canyon morning fog is the world's fog,
  applied by the haze pass; frost blocks render as in vanilla (`mars_clouds_fog_frost_vanilla_vs_iris.jpg`). The
  ground looks browner than vanilla's grey basalt under the pack's warm sunlight.
- **Docking jolt:** a camera roll/pitch from `ViewportEvent.ComputeCameraAngles`; Iris builds its matrices from the
  same camera, so the shake is the same with a pack (at most about 2°, too small to tell apart in a still frame).
- **Inside a closed module the pack's ambient fill is blue:** the airless sky colour handed to the pack
  (see above) is what lights a room that only gets sky light through windows; a lamp brings it back to neutral.
- **Pack border fog on airless worlds:** terrain at the edge of the render distance fades to the pack's fog colour,
  derived from the sky colour we hand it (pale blue), not to black.
- **Silhouette halo at night on thin-air worlds:** where the pack's own sky is much brighter than ours (Mars at night),
  its anti-aliasing leaves a one-pixel pack-coloured rim along terrain edges.

## 3b. Performance

Measured with the `perf` scenario (`-Pscenario=perf`, Mesa llvmpipe software GL at 1920×1080 — absolute frame rates
are far below a real GPU; the relative numbers matter) and GL timer queries (`-Dspacebreaker.compat.profile=true`).

| | Moon | Mars | Titan | Europa |
|---|---|---|---|---|
| Server: 81 fresh chunks (ms/chunk) | 13.6 | 22.1 | 10.4 | 11.4 |
| Client view built (vanilla / Sodium / Iris) | ≤ 1.1 s / ≤ 1.1 s / ≤ 1.1 s | same | 2.1 / 1.1 / 1.1 s | ≤ 1.1 s all |
| Vanilla fps (llvmpipe) | 19.5 | 18.5 | 24.6 | 23.4 |
| Sodium fps (llvmpipe) | 15.9 | 15.7 | 18.9 | 23.2 |
| Iris + Complementary fps (llvmpipe) | 1.5–1.8 | 1.6–1.7 | 1.7–1.9 | 1.9–2.0 |
| Sky pass GPU ms/frame (Iris) | ~7 | ~14.6 | ~7.3 | ~7.7–12 |
| Haze pass GPU ms/frame (Iris) | – | – (no storm) | ~17–19 | – |

- With Iris the frame takes ~550 ms on llvmpipe; our passes are 1–2 % (sky) and ~3 % (haze) of it. On a GPU both are
  a full-screen draw or two: well under a millisecond.
- Sodium is slower than vanilla on llvmpipe (it is tuned for GPUs); not a mod issue. Chunk generation is the same with
  and without Sodium/Iris (server side), Mars being the most expensive terrain.
- `GhostBlocks`: 16 ghost blocks ≈ 0.15–0.23 ms/frame GPU (vanilla, llvmpipe).
- Optimisations: the duplicate sky inside the pack's world pass is skipped (`SkipPackSkyMixin`); the haze pass only
  runs where it changes something and can be switched off (`atmosphereHaze`). Further steps if needed on real
  hardware: half-resolution sky target, merging the two haze draws.

## 4. How to test

Downloads for testing only (never committed) — see `docs/DEV_WORKFLOW.md` → "Shader compatibility test downloads".
Other packs used for the cross-check (Modrinth, free):
```
curl -sL -o run-showcase/shaderpacks/BSL_v10.1.8.zip https://cdn.modrinth.com/data/Q1vvjJYV/versions/Y68yiql9/BSL_v10.1.8.zip
curl -sL -o run-showcase/shaderpacks/MakeUp-UltraFast-9.6.zip https://cdn.modrinth.com/data/izsIPI7a/versions/t4YRxQug/MakeUp-UltraFast-9.6.zip
```
The `compat` showcase scenario shoots the same 12 views in every setup: Moon by day/low sun/night with Earth,
a prop stage (solar panels, tanks, filler, console, glass, lanterns), a suited player (visor), Earth orbit
(down and up), Moon orbit, Mars day/sunset/night.
```
# vanilla
LIBGL_ALWAYS_SOFTWARE=1 xvfb-run -a -s "-screen 0 1920x1080x24" ./gradlew runShowcase -Pscenario=compat
# Sodium only: run-mods/ holds only sodium.jar, or iris.properties has enableShaders=false
... ./gradlew runShowcase -Pscenario=compat -PwithCompat=true -PshotPrefix=sodium
# Sodium + Iris + pack named in run-showcase/config/iris.properties
... ./gradlew runShowcase -Pscenario=compat -PwithCompat=true -PshotPrefix=iris
# a subset of shots
... -Pshots='0[1357]_.*'
```
`./gradlew runGameTestServer` runs `testmod/compat/CompatGameTests` (classification of every body/dimension, no
Overworld weather in space kinds, server weather untouched, Iris-only mixins gated, config defaults).

What to look at: sky pure black with stars in airless worlds; Earth/planet discs present and correctly occluded by
terrain and blocks (sharp silhouettes, no halo); no clouds or rain; regolith grey, not sand; Mars sky butterscotch
by day, blue-ish around the setting Sun; hand, held items and GUI effects on top.

## 5. Recipe viewers: JEI and EMI

Optional, like the renderer integrations: JEI loads `compat/jei/SpacebreakerJeiPlugin` through its `@JeiPlugin`
annotation and EMI loads `compat/emi/SpacebreakerEmiPlugin` through `@EmiEntrypoint`; without them nothing in those
packages is ever loaded. Only their APIs are on the compile classpath (`compileOnly`, versions in
`gradle.properties`: JEI 19.70.0.469, EMI 1.1.24+1.21.1); they are never bundled.

What is shown (data in `compat/recipes/RecipeViewerData`, shared by both plugins and the GameTests):

| Category | Machine (icon and catalyst) |
|---|---|
| Crushing, Arc Smelting, Pressing, Fabricating | crusher, arc furnace, press, fabricator |
| Refining, Electrolysis, Cryogenic Liquefaction | refinery, electrolyzer, cryogenic liquefier |
| Sabatier Reaction, Regolith Heating, Ice Melting, Chemical Reaction | Sabatier reactor, regolith heater, ice melter, chemical reactor |
| Sintering, Uranium Enrichment | sintering furnace, gas centrifuge (`jei_emi_enrichment_item_duct.jpg`) |
| Suit Modules (smithing) | smithing table: template + suit piece + module = fitted piece, every fitting combination |
| Smelting (vanilla) | the arc furnace is added as a catalyst (it smelts anything a furnace can) |
| Information pages | Rocket Assembler (rockets are assembled from parts, not crafted), Atmosphere Processor, Item Duct (sneak-use an end: pull / filter) |

Each processing recipe shows its item inputs, fluid inputs and outputs as tanks with amounts, the press die or other
tool (not consumed), bonus outputs with their chance, energy (FE/t and total), time, and in red
"Research needed: X" / "Gereken araştırma: X" when a technology's `unlocks` lists the recipe id or its result item.
New recipe kinds only need a line in `RecipeViewerData.CATEGORIES` and a title in `CompatModule` (a GameTest fails
when a `ProcessingRecipe.Kind` in `MachinesModule` has no category).

Vehicles and spacecraft made at a crafting table (rover, RTG, space probe, communication and observation
satellites, 3-crew capsule) are plain `minecraft:crafting_shaped` recipes: both viewers list them in their own
Crafting category with no help from us (`jei_crafted_vehicles.jpg`, `emi_crafted_vehicles.jpg`; showcase shots
`*_recipe_<item>`). Also checked: cargo module, landing beacon, suit maintenance station, regolith excavator,
aerostat platform kit, Venus descent probe and solar array (`jei_emi_night_additions.jpg`, JEI top, EMI bottom);
their recipe tags are named, so EMI reports no untranslated tags. The Sintering category (industry) shows with the sintering furnace as icon and catalyst
(`jei_emi_sintering.jpg`). Asteroid mining (`jei_emi_asteroid_mining.jpg`, uses of Bennu and Eros regolith): carbonaceous rubble
in Regolith Heating (water, CO2, coal dust), stony rubble in Crushing (iron, nickel dust); the `c:regoliths/*` tags
are named. JEI opens a uses view on its own Block Tags tab; our categories are the other tabs. Comet ice melts in Ice Melting (it is in `c:ices/water`);
comet dust (and Encke and Halley dust) now has its own Regolith Heating recipe (water, CO2, tholin) and is
also a carbonaceous regolith (`emi_comet_dust_recipes.jpg`). The Mars lander, like every rocket, is assembled,
not crafted: the Rocket Assembler information page covers it.

Screenshots: `jei_*` and `emi_*`. Test: `./gradlew runShowcase -Pscenario=jei -PwithJei=true` or
`-Pscenario=emi -PwithEmi=true` (JEI/EMI come from maven.blamejared.com / maven.terraformersmc.com).

Tag names: viewers show `tag.item.<ns>.<path>` on tag slots. Every item tag of ours used in a recipe has one now
(metals in `EarthModule`, the rest as fallbacks in `CompatModule.TAG_NAMES`, added only when the owning area has not
named it); the GameTest `everyRecipeTagOfOursHasAName` fails for a new unnamed one. REI is not covered yet.

## 6. Block info overlays: Jade and WTHIT

Optional, compile-only (Modrinth Maven, `jade_version` / `wthit_version` in `gradle.properties`, never bundled).
Jade finds `compat/jade/SpacebreakerJadePlugin` through `@WailaPlugin`; WTHIT finds `compat/wthit/SpacebreakerWthitPlugin`
through `waila_plugins.json`. Both show the lines of `compat/info/BlockInfoLines`, client side only (the values are
already synced to the client by the block entities):

- Solar panel: "Dust: 42%, output: 66%" / "Toz: %42, verim: %66" (dust on the cells, `SolarPanelBlockEntity`).

New lines go into `BlockInfoLines.lines` and show in both overlays. GameTest: `dustySolarPanelShowsDustAndOutput`.
Showcase: `-Pscenario=info -PwithJade=true` or `-PwithWthit=true` (a dusty panel under the crosshair).
**Not yet run in the game here:** starting the client with the Jade or WTHIT jar was not permitted in this test
environment; the plugins compile against both APIs and the line itself is covered by the GameTest.

## 7. Release checklist (1.0)

- **Dedicated server:** the GameTest server runs on the dedicated-server side with every module (all tests pass).
  Client classes are only referenced from client packages, from compat plugins that only the client-side mods load,
  from mixins in the client list, and from `playToClient` handler lambdas (never run on a server).
- **Optional mods:** Iris, Sodium, JEI, EMI, Jade and WTHIT are `optional` dependencies with minimum versions in
  `neoforge.mods.toml` (an older one is refused with a message, not a crash); Distant Horizons and Voxy are only read by
  reflection and need no entry. Without any of them the mod runs unchanged.
- **Showcase pass with a pack** (`-PwithCompat=true`, Iris + Sodium + Complementary in `run-mods/`; add DH for `lod`):
  `compat`, `moon`, `mars`, `mars_dust`, `mars_clouds`, `mars_frost`, `venus`, `mercury`, `mercury_sunrise`,
  `inner_orbit`, `phobos`, `rocketry -Pshots='(32|34|35|37|47|49|50|52)_.*'`, `rover_moon`, `rover_mars`,
  `sky_planets`, `coma_sky`, `jovian`, `io`, `ganymede`, `saturnian`, `suit_eva_titan`, `enceladus`, `miranda`,
  `umbriel`, `oberon`, `triton`, `pluto`, `churyumov_gerasimenko`, `halley`, `bennu`, `hydra`, `lod`.
  Look for: our black sky with stars and planets (not the pack's blue), no pack clouds, haze on Venus/Titan/Mars
  storms, plumes and glows in front of space, windows showing space, orbit night darkening.

