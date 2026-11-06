# Spacebreaker — Quality assurance

Owner: QA & playtesting area (`dev/qa`). QA writes tests, tools and reports; the owning area fixes the bug.

## How it works
- `src/testmod/.../testmod/qa/ProgressionModel.java` builds a **whole-game progression model** from the live registries of a
  GameTest server: every recipe type (crafting, smelting, blasting, smoking, stonecutting, smithing and all eleven machine
  types, fluids included), item/fluid tags, every loot table (blocks, creatures, dungeon chests; tech disks with their
  `random_tech_disk` pools, field samples and flight records), world generation of every dimension (level stem, biomes,
  placed/configured features, terrain profiles, NeoForge biome modifiers), dungeon definitions, the tech tree, rocket parts
  and the transfer graph. Rules the game implements in code are modelled by hand: machines extending `MachineBlockEntity`
  lock until researched, power = combustion generator where the air is breathable or solar panels, the research lab needs
  power, a sample kit gives the samples of the world it is used in, the first orbit gives the orbital flight records,
  pumpjack over crude oil, atmosphere processor gas per body, fluids are not carried between worlds.
- Flights: `RocketSearch` builds every assembler-legal design (up to 3 stages, 1 engine + 1–3 tanks + radial + separator
  per stage, heat shield / capsule / parachute) from the parts the player may use and flies the real `MissionPlanner`
  (staging, heat-shield rating, TWR ≥ 1.05 on the pad) on the cheapest route. A world counts as reached when the rocket can
  come back to Earth, either on what it carried or after refuelling with propellant made there.
- The model is a fixed point; every fact remembers its step and what it came from, so the report prints a valid order.
- **What-if run:** technologies whose prerequisites are met but that have no possible source get an assumed disk on Earth,
  round after round, to show the dead ends hiding behind the first one.
- `tools/qa/lang_audit.py`: key parity, placeholders, empty strings, English left in Turkish, missing Turkish letters,
  guide chapters in both languages.

Run: `./gradlew runGameTestServer` (writes `run-gametest/qa/progression.md` and `facts.tsv`), then
`python3 tools/qa/update_qa_doc.py`. Default realism (BALANCED).

### Regression guards (GameTests in `testmod/qa`)
- `noNewTechnologyDeadEnds` — fails when a technology that is not in `KNOWN_DEAD_ENDS` cannot be learned. Remove entries as
  their bugs are fixed (the log prints which ones became reachable).
- `moonAndMarsStayReachableOnceResearchIsFixed` — in the what-if model, first launch, Earth orbit, Moon and Mars round
  trips must stay possible (catches recipe / fuel / rocket / transfer regressions).
- `progressionReportIsWritten` — the model sees recipes, worlds and a buildable research lab.

## Findings — round 1 (2026-11-06)

| ID | Severity | Owner | Finding |
|---|---|---|---|
| QA-1 | **Blocker** | research (+ industry) | No liquid oxygen before space → the first rocket can never be fuelled. |
| QA-2 | High | research | `suit_mk3` has no samples and no disk anywhere → `icy_moons`, `cryobot_drilling`, `plume_sampling` can never be learned. |
| QA-3 | High | inner planets + rocketry | Mercury surface and orbit: no round trip with any buildable rocket and no fuel there → all Mercury content unreachable. |
| QA-4 | Medium | suit | Moon night (−173 °C) is colder than the Mk1 EVA rating (−160 °C); the first better suit needs Mars samples. |
| QA-5 | Medium | suit (blueprint) | `hologram_projector` has no recipe or loot: survival players cannot use "Show hologram". |
| QA-6 | Info | research | Several technologies have no source at all yet (future content): `fusion_power`, `fusion_drive`, `nuclear_thermal_propulsion`, `orbital_shipyard`. |
| QA-7 | Info | research | `unlocks` lists are enforced only for machines, rocket parts and suit tiers; crafting is never gated (e.g. `sample_kit` is in `field_geology`, which is learned from Earth samples that need the kit — a cycle if crafting gating is ever added). |

### QA-1 No liquid oxygen before space (blocker)
- The first rocket needs RP-1 **and LOX** (`Propellant.KEROLOX`, fuel umbilical takes `c:rp1` + `c:liquid_oxygen`).
- LOX only comes from `liquefaction/oxygen` in the **cryogenic liquefier** (locked by `cryogenics`, tier 2, no samples).
- Oxygen only comes from the **electrolyser** (locked by `electrolysis`, tier 1, no samples) or the **atmosphere processor**
  (locked by `sabatier_process`, needs Mars samples).
- `electrolysis` and `cryogenics` disks only drop in dungeons on Ceres, Deimos, Mars, Moon…; the Earth observatory
  (`dungeons/earth/observatory_archive`) rolls propulsion/astronomy tier 1 and the fixed list fuel_refining /
  launch_infrastructure / electronics / metal_pressing.
- Result: 60 of 80 technologies are unreachable in a fresh world. The guide (`progression.md`, `cryogenics.md`) tells the
  player to research electrolysis and cryogenics on Earth, which is impossible.
- Reproduce: new survival world, get every Earth disk, build lab + generator: the electrolysis and cryogenics disks never
  appear; `Research needed` stays on the liquefier.
- Suggested fix (research): add `electrolysis` and `cryogenics` to the observatory archive's fixed disk list, or give both
  `samples: [spacebreaker:earth]`. The what-if run shows that with that alone the first launch, Earth orbit, the Moon and
  Mars round trips all work (see the generated section).

### QA-2 suit_mk3 cannot be learned
- `suit_mk3` (tier 3, requires `radiation_shielding`) has no samples and no loot table rolls it (pools filtered by tier/category
  never include it). `icy_moons` requires it, so `cryobot_drilling` and `plume_sampling` are dead ends even late game.
- Fix: add a sample kind (e.g. `ganymede`) or a disk in a Jovian dungeon.

### QA-3 Mercury unreachable
- BALANCED: round trip to Mercury surface needs 12 864 m/s gameplay Δv, orbit 10 360 m/s; best buildable design (3 stages,
  methalox) has 9 794 m/s vacuum Δv. No methalox can be made on Mercury (no CO₂/CH₄), and hydrogen engines unlock no part.
- Effect: `graphite`, `graphite_ore`, `mercury_regolith`, `hollow_deposit`, `mercurian_crust`, `low_reflectance_rock` and
  every Mercury sample are unobtainable; `solar_shielding` (only Mercury samples) needs another source.
- Fix options: lower Mercury transfer cost (gravity assist via Venus), a hydrogen/methane source on Mercury, or a
  hydrolox engine part for `hydrogen_engines`.
- Update: dev/inner adds in-situ methalox on Mercury (polar ice → water → H₂/O₂, graphite + O₂ → CO₂, Sabatier). Tested
  merged with dev/qa: the chain is correct, but even a one-way landing fails — Earth orbit 3158 → Venus orbit 1238 →
  Mercury orbit 3327 → surface 1252 = 8975 m/s gameplay Δv, more than the best design delivers after ascent losses
  (9794 m/s vacuum). Still open on rocketry's side (hydrolox upper stage) or a cheaper route.

### QA-4 Moon night vs Mk1 suit
- Moon body data: night −173 °C, day 120 °C. `SuitTier.MK1` covers −160…125 °C. Thermal regulator modules
  (`suit_upgrades`) and heated suits both need `suit_mk2` (Mars samples), so on the first destination the player slowly
  cools down at night. Also Vesta (−190 °C) and Titan (−180 °C) need the heated suit (reachable later via Mars).
- Fix: Mk1 minimum −180 °C, or make the thermal regulator module a Moon-tier upgrade.

### QA-5 Hologram projector has no recipe
- `BlueprintModule.HOLOGRAM_PROJECTOR` is registered but no recipe or loot gives it; the guide's "Show hologram" button
  needs it. Fix: add a crafting recipe.

## Localization (round 1)
- Key parity en_us/tr_tr is complete (1305 each), placeholders and colour codes match, no broken characters.
- Turkish reads naturally overall. Small suggestions (research keeps docs/GLOSSARY.md):
  - `message.spacebreaker.launch.twr`: "…oranı %s, 1'in altında" — the real limit is 1.05: "…oranı %s, en az 1,05 olmalı".
  - `message.spacebreaker.orbit.arrived`: "%s içindesin" reads oddly for "Dünya yörüngesi": "Buradasın: %s. Nereye
    gideceğini seçmek için yıldız haritasını aç."
  - `gui.spacebreaker.assembler.twr` / `stage_line`: "İ/A" is not a common abbreviation; "İtki/ağırlık" or "İ/Ağ".

<!-- BEGIN GENERATED: do not edit below, run tools/qa/update_qa_doc.py -->

# Generated progression report

<!-- generated by the GameTest qa/ProgressionAuditTests (realism BALANCED) -->
### Progression model summary

- recipes analysed: 1674, special recipes skipped: 15
- worlds: 23, technologies: 80, model steps: 10
- machines that lock themselves until researched: arc_furnace, atmosphere_processor, chemical_reactor, cryogenic_liquefier, electrolyzer, fabricator, ice_melter, press, pumpjack, refinery, regolith_heater, sabatier_reactor

### Goals

### ❌ First rocket launch (reaches Earth orbit, return not required)

Not reachable in a fresh survival world.

### ❌ Earth orbit and back

Not reachable in a fresh survival world.

### ❌ Moon landing and return

Not reachable in a fresh survival world.

### ❌ Mars landing and return

Not reachable in a fresh survival world.

### Technologies in the order a fresh world can learn them

- step 3: `aluminium_smelting` — analysed spacebreaker:earth
- step 3: `basic_rocketry` — tech disk
- step 3: `field_geology` — analysed spacebreaker:earth
- step 3: `launch_infrastructure` — tech disk
- step 3: `metal_pressing` — tech disk
- step 3: `metallurgy` — analysed spacebreaker:earth
- step 3: `observational_astronomy` — tech disk
- step 3: `orbital_mechanics` — tech disk
- step 3: `pressure_suits` — analysed spacebreaker:earth
- step 3: `solar_power` — analysed spacebreaker:earth
- step 3: `solid_rocket_motors` — tech disk
- step 4: `electronics` — tech disk
- step 4: `fuel_refining` — tech disk
- step 4: `guidance_systems` — tech disk
- step 4: `liquid_fuel_engines` — tech disk
- step 4: `precision_manufacturing` — analysed spacebreaker:earth
- step 4: `radio_communication` — tech disk
- step 4: `staging` — tech disk
- step 4: `vacuum_engines` — tech disk
- step 5: `landing_systems` — tech disk

### Technologies that cannot be learned

- `acid_resistant_coatings` (tier 2): needs pressurised_modules; no source reached (samples: venus; disk in loot: dungeons/mars/station_storage, chests/moon/lava_tube_probe, chests/moon/tech_data, chests/moon/outpost_lab …)
- `advanced_electronics` (tier 2): no source reached (samples: meteorite; disk in loot: dungeons/deimos/ice_cache, dungeons/mars/station_storage, dungeons/mercury/ice_mine_office, chests/moon/lava_tube_probe …)
- `aerial_mobility` (tier 4): needs rovers; no source reached (samples: titan; disk in loot: dungeons/titan/hangar_workshop)
- `airlocks` (tier 1): needs oxygen_storage; needs pressurised_modules; no source reached (samples: none; disk in loot: dungeons/deimos/ice_cache, dungeons/mars/riverbed_probe, chests/moon/lava_tube_probe, chests/moon/tech_data …)
- `asteroid_mining` (tier 3): needs rovers; needs ion_propulsion; no source reached (samples: meteorite, ceres, vesta; disk in loot: dungeons/ceres/outpost_office)
- `astrobiology` (tier 2): needs martian_geology; needs spectroscopy; no source reached (samples: mars, venus; disk in loot: dungeons/mars/riverbed_probe, dungeons/mars/station_command, dungeons/mars/station_lab, dungeons/mercury/ice_mine_office …)
- `brine_chemistry` (tier 3): needs water_recycling; no source reached (samples: ceres; disk in loot: dungeons/ceres/outpost_office, dungeons/ceres/outpost_core_store, dungeons/europa/probe_samples)
- `cloud_platforms` (tier 2): needs space_stations; no source reached (samples: venus; disk in loot: dungeons/mars/station_storage, chests/moon/lava_tube_probe, chests/moon/tech_data, chests/moon/outpost_lab …)
- `cryobot_drilling` (tier 4): needs icy_moons; no source reached (samples: europa, ganymede; disk in loot: dungeons/europa/probe_instruments, dungeons/europa/probe_samples)
- `cryogenics` (tier 2): needs electrolysis; no source reached (samples: none; disk in loot: dungeons/ceres/outpost_stores, dungeons/deimos/ice_cache, dungeons/mars/station_storage, dungeons/mars/station_isru …)
- `deep_space_habitats` (tier 4): needs pressurised_modules; needs radiation_shielding; no source reached (samples: callisto; disk in loot: dungeons/callisto/lander_cargo, dungeons/callisto/lander_cabin)
- `deep_space_network` (tier 3): needs space_probes; needs nuclear_fission; no source reached (samples: none; disk in loot: dungeons/callisto/lander_cabin, dungeons/enceladus/lander_cabin, dungeons/europa/probe_instruments, dungeons/ganymede/lander_cabin …)
- `dust_proof_seals` (tier 2): no source reached (samples: mars; disk in loot: dungeons/deimos/ice_cache, dungeons/mars/riverbed_probe, dungeons/mars/station_command, chests/moon/lava_tube_probe …)
- `electrolysis` (tier 1): no source reached (samples: none; disk in loot: dungeons/ceres/outpost_stores, dungeons/deimos/ice_cache, dungeons/mars/station_storage, dungeons/mars/station_isru …)
- `eva_mobility` (tier 2): needs thermal_regulation; no source reached (samples: none; disk in loot: dungeons/deimos/ice_cache, dungeons/mars/riverbed_probe, chests/moon/lava_tube_probe, chests/moon/tech_data …)
- `fusion_drive` (tier 3): needs nuclear_thermal_propulsion; needs fusion_power; no source reached (samples: none; disk in loot: nowhere)
- `fusion_power` (tier 3): needs nuclear_fission; no source reached (samples: none; disk in loot: nowhere)
- `gravity_assists` (tier 3): needs interplanetary_transfers; needs space_probes; no source reached (samples: callisto; disk in loot: dungeons/phobos/probe_vault)
- `heat_resistant_electronics` (tier 2): needs advanced_electronics; no source reached (samples: venus, mercury; disk in loot: dungeons/deimos/ice_cache, dungeons/mars/station_storage, dungeons/mercury/ice_mine_office, chests/moon/lava_tube_probe …)
- `heated_suits` (tier 2): needs suit_mk2; needs cryogenics; no source reached (samples: mars; disk in loot: dungeons/deimos/ice_cache, dungeons/mars/riverbed_probe, chests/moon/lava_tube_probe, chests/moon/tech_data …)
- `heavy_launch` (tier 2): no source reached (samples: none; disk in loot: dungeons/mars/station_storage, chests/moon/lava_tube_probe, chests/moon/tech_data, chests/moon/outpost_lab …)
- `hydrocarbon_processing` (tier 4): needs methane_engines; no source reached (samples: titan; disk in loot: dungeons/titan/hangar_lab, dungeons/titan/hangar_workshop)
- `hydrogen_engines` (tier 2): needs cryogenics; no source reached (samples: none; disk in loot: dungeons/mars/station_command, chests/moon/lava_tube_probe, chests/moon/tech_data, chests/moon/outpost_lab …)
- `icy_moons` (tier 3): needs astrobiology; needs suit_mk3; no source reached (samples: europa, enceladus, ganymede, callisto; disk in loot: dungeons/europa/probe_instruments)
- `interplanetary_transfers` (tier 2): no source reached (samples: moon; disk in loot: chests/moon/lava_tube_probe, chests/moon/tech_data, chests/moon/outpost_lab, chests/moon/outpost_command …)
- `ion_propulsion` (tier 2): needs hydrogen_engines; needs advanced_electronics; no source reached (samples: none; disk in loot: dungeons/mars/station_command, chests/moon/lava_tube_probe, chests/moon/tech_data, chests/moon/outpost_lab …)
- `isru` (tier 2): needs electrolysis; needs lunar_surveying; no source reached (samples: moon; disk in loot: dungeons/deimos/ice_cache, dungeons/mars/station_command, dungeons/mars/station_storage, dungeons/mars/station_isru …)
- `lunar_surveying` (tier 1): no source reached (samples: moon; disk in loot: dungeons/mars/riverbed_probe, dungeons/mars/station_command, dungeons/mercury/ice_mine_office, chests/moon/lava_tube_probe …)
- `magnetospheric_physics` (tier 4): needs radiation_shielding; no source reached (samples: ganymede; disk in loot: dungeons/ganymede/lander_cargo, dungeons/ganymede/lander_cabin)
- `martian_geology` (tier 2): needs interplanetary_transfers; no source reached (samples: mars; disk in loot: dungeons/mars/riverbed_probe, dungeons/mars/station_command, dungeons/mercury/ice_mine_office, chests/moon/lava_tube_probe …)
- `methane_engines` (tier 2): needs sabatier_process; no source reached (samples: mars; disk in loot: dungeons/mars/station_command, dungeons/mars/station_isru, chests/moon/lava_tube_probe, chests/moon/tech_data …)
- `microgravity_mining` (tier 4): needs asteroid_mining; no source reached (samples: ceres, vesta; disk in loot: dungeons/ceres/outpost_office, dungeons/ceres/outpost_core_store, dungeons/vesta/lander_cargo, dungeons/vesta/lander_cabin)
- `nuclear_fission` (tier 3): needs advanced_electronics; needs cryogenics; no source reached (samples: none; disk in loot: dungeons/io/observatory_vault)
- `nuclear_thermal_propulsion` (tier 3): needs hydrogen_engines; needs nuclear_fission; no source reached (samples: none; disk in loot: nowhere)
- `orbital_shipyard` (tier 3): needs space_stations; needs heavy_launch; needs fusion_power; no source reached (samples: none; disk in loot: nowhere)
- `oxygen_distribution` (tier 1): needs airlocks; needs electrolysis; no source reached (samples: moon; disk in loot: dungeons/deimos/ice_cache, dungeons/mars/riverbed_probe, chests/moon/lava_tube_probe, chests/moon/tech_data …)
- `oxygen_storage` (tier 1): no source reached (samples: none; disk in loot: dungeons/deimos/ice_cache, dungeons/mars/riverbed_probe, chests/moon/lava_tube_probe, chests/moon/tech_data …)
- `phobos_survey` (tier 2): needs martian_geology; no source reached (samples: phobos; disk in loot: dungeons/mars/riverbed_probe, dungeons/mars/station_command, dungeons/mercury/ice_mine_office, chests/moon/lava_tube_probe …)
- `plume_sampling` (tier 5): needs cryobot_drilling; needs astrobiology; no source reached (samples: enceladus; disk in loot: dungeons/enceladus/lander_cabin, dungeons/enceladus/lander_cargo, dungeons/titan/hangar_lab)
- `polar_ice_mining` (tier 2): needs lunar_surveying; no source reached (samples: mercury, mars; disk in loot: dungeons/deimos/ice_cache, dungeons/mars/riverbed_probe, dungeons/mars/station_command, dungeons/mercury/ice_mine_office …)
- `pressurised_modules` (tier 1): no source reached (samples: none; disk in loot: dungeons/mars/station_storage, chests/moon/lava_tube_probe, chests/moon/tech_data, chests/moon/outpost_lab …)
- `radiation_hardened_electronics` (tier 4): needs radiation_shielding; needs advanced_electronics; no source reached (samples: europa, io; disk in loot: dungeons/europa/probe_instruments, dungeons/io/observatory_logs)
- `radiation_hardened_suits` (tier 3): needs radiation_shielding; no source reached (samples: europa, io; disk in loot: nowhere)
- `radiation_shielding` (tier 3): needs suit_mk2; needs nuclear_fission; no source reached (samples: io, europa, ganymede; disk in loot: dungeons/callisto/lander_cabin, dungeons/ceres/outpost_office, dungeons/enceladus/lander_cabin, dungeons/europa/probe_instruments …)
- `regolith_sintering` (tier 1): needs pressurised_modules; no source reached (samples: moon; disk in loot: dungeons/mars/station_storage, chests/moon/lava_tube_probe, chests/moon/tech_data, chests/moon/outpost_lab …)
- `rovers` (tier 2): needs lunar_surveying; no source reached (samples: none; disk in loot: dungeons/mars/riverbed_probe, dungeons/mars/station_command, dungeons/mercury/ice_mine_office, chests/moon/lava_tube_probe …)
- `sabatier_process` (tier 2): needs electrolysis; no source reached (samples: mars; disk in loot: dungeons/deimos/ice_cache, dungeons/mars/station_lab, dungeons/mars/station_storage, dungeons/mars/station_isru …)
- `satellites` (tier 2): no source reached (samples: orbital_flight; disk in loot: chests/moon/lava_tube_probe, chests/moon/tech_data, chests/moon/outpost_lab, chests/moon/outpost_command …)
- `solar_shielding` (tier 2): needs thermal_regulation; no source reached (samples: mercury; disk in loot: dungeons/deimos/ice_cache, dungeons/mars/riverbed_probe, chests/moon/lava_tube_probe, chests/moon/tech_data …)
- `space_probes` (tier 2): needs satellites; needs interplanetary_transfers; no source reached (samples: none; disk in loot: dungeons/ceres/outpost_office, dungeons/europa/probe_instruments, dungeons/mars/riverbed_probe, chests/moon/lava_tube_probe …)
- `space_stations` (tier 2): needs pressurised_modules; no source reached (samples: orbital_flight; disk in loot: dungeons/mars/station_storage, chests/moon/lava_tube_probe, chests/moon/tech_data, chests/moon/outpost_lab …)
- `spectroscopy` (tier 2): needs advanced_electronics; no source reached (samples: meteorite; disk in loot: dungeons/io/observatory_logs, chests/moon/lava_tube_probe, chests/moon/tech_data, chests/moon/outpost_lab …)
- `suit_mk2` (tier 2): needs eva_mobility; needs advanced_electronics; no source reached (samples: mars; disk in loot: dungeons/deimos/ice_cache, dungeons/mars/riverbed_probe, chests/moon/lava_tube_probe, chests/moon/tech_data …)
- `suit_mk3` (tier 3): needs radiation_shielding; no source reached (samples: none; disk in loot: nowhere)
- `suit_upgrades` (tier 2): needs suit_mk2; no source reached (samples: mars; disk in loot: dungeons/deimos/ice_cache, dungeons/mars/riverbed_probe, chests/moon/lava_tube_probe, chests/moon/tech_data …)
- `thermal_regulation` (tier 1): no source reached (samples: none; disk in loot: dungeons/deimos/ice_cache, dungeons/io/observatory_logs, dungeons/mars/riverbed_probe, dungeons/mercury/ice_mine_office …)
- `thermal_suits` (tier 2): needs suit_mk2; needs heat_resistant_electronics; no source reached (samples: venus, mercury; disk in loot: dungeons/deimos/ice_cache, dungeons/mars/riverbed_probe, chests/moon/lava_tube_probe, chests/moon/tech_data …)
- `titan_expedition` (tier 3): needs astrobiology; needs cryogenics; no source reached (samples: titan; disk in loot: nowhere)
- `volcanic_heat_shielding` (tier 4): needs thermal_regulation; needs suit_mk2; no source reached (samples: io; disk in loot: dungeons/io/observatory_vault, dungeons/io/observatory_logs)
- `water_recycling` (tier 2): needs oxygen_distribution; no source reached (samples: none; disk in loot: dungeons/ceres/outpost_office, dungeons/deimos/ice_cache, dungeons/mars/riverbed_probe, dungeons/mars/station_lab …)

### Worlds

| World | Reached | How | Survival when first reached |
|---|---|---|---|
| minecraft:overworld | yes | step 0: start | home |
| minecraft:the_nether | yes | step 0: nether portal (vanilla) | — |
| minecraft:the_end | yes | step 0: end portal (vanilla) | — |
| spacebreaker:callisto | **no** | — | — |
| spacebreaker:ceres | **no** | — | — |
| spacebreaker:deimos | **no** | — | — |
| spacebreaker:earth_orbit | **no** | — | — |
| spacebreaker:enceladus | **no** | — | — |
| spacebreaker:europa | **no** | — | — |
| spacebreaker:ganymede | **no** | — | — |
| spacebreaker:io | **no** | — | — |
| spacebreaker:mars | **no** | — | — |
| spacebreaker:mars_orbit | **no** | — | — |
| spacebreaker:mercury | **no** | — | — |
| spacebreaker:mercury_orbit | **no** | — | — |
| spacebreaker:moon | **no** | — | — |
| spacebreaker:moon_orbit | **no** | — | — |
| spacebreaker:orbit | **no** | — | — |
| spacebreaker:phobos | **no** | — | — |
| spacebreaker:titan | **no** | — | — |
| spacebreaker:venus | **no** | — | — |
| spacebreaker:venus_orbit | **no** | — | — |
| spacebreaker:vesta | **no** | — | — |

### Items that cannot be obtained

- `liquid_oxygen_bucket`: no recipe, loot, drop or world source
- `liquid_hydrogen_bucket`: no recipe, loot, drop or world source
- `liquid_methane_bucket`: no recipe, loot, drop or world source
- `raw_ilmenite`: no recipe, loot, drop or world source
- `kreep_fragment`: no recipe, loot, drop or world source
- `lunar_regolith`: spacebreaker:dry_lunar_regolith: spacebreaker:icy_regolith
- `mare_basalt`: no recipe, loot, drop or world source
- `ilmenite_ore`: no recipe, loot, drop or world source
- `kreep_ore`: no recipe, loot, drop or world source
- `mare_regolith`: no recipe, loot, drop or world source
- `volcanic_glass_soil`: no recipe, loot, drop or world source
- `icy_regolith`: no recipe, loot, drop or world source
- `lunar_ice`: no recipe, loot, drop or world source
- `anorthosite`: no recipe, loot, drop or world source
- `anorthosite_stairs`: spacebreaker:anorthosite_stairs: spacebreaker:anorthosite, spacebreaker:anorthosite, spacebreaker:anorthosite, spacebreaker:anorthosite, spacebreaker:anorthosite, spacebreaker:anorthosite | spacebreaker:anorthosite_stairs_from_anorthosite_stonecutting: spacebreaker:anorthosite
- `anorthosite_slab`: spacebreaker:anorthosite_slab_from_anorthosite_stonecutting: spacebreaker:anorthosite | spacebreaker:anorthosite_slab: spacebreaker:anorthosite, spacebreaker:anorthosite, spacebreaker:anorthosite
- `anorthosite_wall`: spacebreaker:anorthosite_wall_from_anorthosite_stonecutting: spacebreaker:anorthosite | spacebreaker:anorthosite_wall: spacebreaker:anorthosite, spacebreaker:anorthosite, spacebreaker:anorthosite, spacebreaker:anorthosite, spacebreaker:anorthosite, spacebreaker:anorthosite
- `polished_anorthosite`: spacebreaker:polished_anorthosite_from_anorthosite_stonecutting: spacebreaker:anorthosite | spacebreaker:polished_anorthosite: spacebreaker:anorthosite, spacebreaker:anorthosite, spacebreaker:anorthosite, spacebreaker:anorthosite
- `polished_anorthosite_stairs`: spacebreaker:polished_anorthosite_stairs: spacebreaker:polished_anorthosite, spacebreaker:polished_anorthosite, spacebreaker:polished_anorthosite, spacebreaker:polished_anorthosite, spacebreaker:polished_anorthosite, spacebreaker:polished_anorthosite | spacebreaker:polished_anorthosite_stairs_from_polished_anorthosite_stonecutting: spacebreaker:polished_anorthosite | spacebreaker:polished_anorthosite_stairs_from_anorthosite_stonecutting: spacebreaker:anorthosite
- `polished_anorthosite_slab`: spacebreaker:polished_anorthosite_slab_from_anorthosite_stonecutting: spacebreaker:anorthosite | spacebreaker:polished_anorthosite_slab_from_polished_anorthosite_stonecutting: spacebreaker:polished_anorthosite | spacebreaker:polished_anorthosite_slab: spacebreaker:polished_anorthosite, spacebreaker:polished_anorthosite, spacebreaker:polished_anorthosite
- `anorthosite_bricks`: spacebreaker:anorthosite_bricks: spacebreaker:polished_anorthosite, spacebreaker:polished_anorthosite, spacebreaker:polished_anorthosite, spacebreaker:polished_anorthosite | spacebreaker:anorthosite_bricks_from_polished_anorthosite_stonecutting: spacebreaker:polished_anorthosite | spacebreaker:anorthosite_bricks_from_anorthosite_stonecutting: spacebreaker:anorthosite
- `anorthosite_brick_stairs`: spacebreaker:anorthosite_brick_stairs: spacebreaker:anorthosite_bricks, spacebreaker:anorthosite_bricks, spacebreaker:anorthosite_bricks, spacebreaker:anorthosite_bricks, spacebreaker:anorthosite_bricks, spacebreaker:anorthosite_bricks | spacebreaker:anorthosite_brick_stairs_from_anorthosite_bricks_stonecutting: spacebreaker:anorthosite_bricks | spacebreaker:anorthosite_brick_stairs_from_anorthosite_stonecutting: spacebreaker:anorthosite
- `anorthosite_brick_slab`: spacebreaker:anorthosite_brick_slab_from_anorthosite_bricks_stonecutting: spacebreaker:anorthosite_bricks | spacebreaker:anorthosite_brick_slab: spacebreaker:anorthosite_bricks, spacebreaker:anorthosite_bricks, spacebreaker:anorthosite_bricks | spacebreaker:anorthosite_brick_slab_from_anorthosite_stonecutting: spacebreaker:anorthosite
- `anorthosite_brick_wall`: spacebreaker:anorthosite_brick_wall_from_anorthosite_stonecutting: spacebreaker:anorthosite | spacebreaker:anorthosite_brick_wall_from_anorthosite_bricks_stonecutting: spacebreaker:anorthosite_bricks | spacebreaker:anorthosite_brick_wall: spacebreaker:anorthosite_bricks, spacebreaker:anorthosite_bricks, spacebreaker:anorthosite_bricks, spacebreaker:anorthosite_bricks, spacebreaker:anorthosite_bricks, spacebreaker:anorthosite_bricks
- `impact_breccia`: no recipe, loot, drop or world source
- `sintered_regolith`: spacebreaker:sintered_regolith_from_mare_regolith: spacebreaker:mare_regolith | spacebreaker:sintered_regolith_from_lunar_regolith: spacebreaker:lunar_regolith
- `sintered_regolith_bricks`: spacebreaker:sintered_regolith_bricks_from_sintered_regolith_stonecutting: spacebreaker:sintered_regolith | spacebreaker:sintered_regolith_bricks: spacebreaker:sintered_regolith, spacebreaker:sintered_regolith, spacebreaker:sintered_regolith, spacebreaker:sintered_regolith
- `sintered_regolith_brick_stairs`: spacebreaker:sintered_regolith_brick_stairs_from_sintered_regolith_bricks_stonecutting: spacebreaker:sintered_regolith_bricks | spacebreaker:sintered_regolith_brick_stairs: spacebreaker:sintered_regolith_bricks, spacebreaker:sintered_regolith_bricks, spacebreaker:sintered_regolith_bricks, spacebreaker:sintered_regolith_bricks, spacebreaker:sintered_regolith_bricks, spacebreaker:sintered_regolith_bricks
- `sintered_regolith_brick_slab`: spacebreaker:sintered_regolith_brick_slab_from_sintered_regolith_bricks_stonecutting: spacebreaker:sintered_regolith_bricks | spacebreaker:sintered_regolith_brick_slab: spacebreaker:sintered_regolith_bricks, spacebreaker:sintered_regolith_bricks, spacebreaker:sintered_regolith_bricks
- `sintered_regolith_brick_wall`: spacebreaker:sintered_regolith_brick_wall_from_sintered_regolith_bricks_stonecutting: spacebreaker:sintered_regolith_bricks | spacebreaker:sintered_regolith_brick_wall: spacebreaker:sintered_regolith_bricks, spacebreaker:sintered_regolith_bricks, spacebreaker:sintered_regolith_bricks, spacebreaker:sintered_regolith_bricks, spacebreaker:sintered_regolith_bricks, spacebreaker:sintered_regolith_bricks
- `meteoric_iron`: no recipe, loot, drop or world source
- `hologram_projector`: no recipe, loot, drop or world source
- `rocket`: no recipe, loot, drop or world source
- `martian_regolith`: no recipe, loot, drop or world source
- `hematite_soil`: no recipe, loot, drop or world source
- `perchlorate_soil`: no recipe, loot, drop or world source
- `martian_clay`: no recipe, loot, drop or world source
- `co2_frost`: no recipe, loot, drop or world source
- `martian_sand`: spacebreaker:crushing/dusty_ice: spacebreaker:dusty_ice
- `olivine_sand`: no recipe, loot, drop or world source
- `martian_basalt`: no recipe, loot, drop or world source
- `martian_basalt_stairs`: spacebreaker:martian_basalt_stairs: spacebreaker:martian_basalt, spacebreaker:martian_basalt, spacebreaker:martian_basalt, spacebreaker:martian_basalt, spacebreaker:martian_basalt, spacebreaker:martian_basalt | spacebreaker:martian_basalt_stairs_from_martian_basalt_stonecutting: spacebreaker:martian_basalt
- `martian_basalt_slab`: spacebreaker:martian_basalt_slab_from_martian_basalt_stonecutting: spacebreaker:martian_basalt | spacebreaker:martian_basalt_slab: spacebreaker:martian_basalt, spacebreaker:martian_basalt, spacebreaker:martian_basalt
- `martian_basalt_wall`: spacebreaker:martian_basalt_wall: spacebreaker:martian_basalt, spacebreaker:martian_basalt, spacebreaker:martian_basalt, spacebreaker:martian_basalt, spacebreaker:martian_basalt, spacebreaker:martian_basalt | spacebreaker:martian_basalt_wall_from_martian_basalt_stonecutting: spacebreaker:martian_basalt
- `polished_martian_basalt`: spacebreaker:polished_martian_basalt: spacebreaker:martian_basalt, spacebreaker:martian_basalt, spacebreaker:martian_basalt, spacebreaker:martian_basalt | spacebreaker:polished_martian_basalt_from_martian_basalt_stonecutting: spacebreaker:martian_basalt
- `polished_martian_basalt_stairs`: spacebreaker:polished_martian_basalt_stairs_from_polished_martian_basalt_stonecutting: spacebreaker:polished_martian_basalt | spacebreaker:polished_martian_basalt_stairs: spacebreaker:polished_martian_basalt, spacebreaker:polished_martian_basalt, spacebreaker:polished_martian_basalt, spacebreaker:polished_martian_basalt, spacebreaker:polished_martian_basalt, spacebreaker:polished_martian_basalt
- `polished_martian_basalt_slab`: spacebreaker:polished_martian_basalt_slab: spacebreaker:polished_martian_basalt, spacebreaker:polished_martian_basalt, spacebreaker:polished_martian_basalt | spacebreaker:polished_martian_basalt_slab_from_polished_martian_basalt_stonecutting: spacebreaker:polished_martian_basalt
- `martian_basalt_bricks`: spacebreaker:martian_basalt_bricks: spacebreaker:polished_martian_basalt, spacebreaker:polished_martian_basalt, spacebreaker:polished_martian_basalt, spacebreaker:polished_martian_basalt | spacebreaker:martian_basalt_bricks_from_polished_martian_basalt_stonecutting: spacebreaker:polished_martian_basalt | spacebreaker:martian_basalt_bricks_from_martian_basalt_stonecutting: spacebreaker:martian_basalt
- `martian_basalt_brick_stairs`: spacebreaker:martian_basalt_brick_stairs: spacebreaker:martian_basalt_bricks, spacebreaker:martian_basalt_bricks, spacebreaker:martian_basalt_bricks, spacebreaker:martian_basalt_bricks, spacebreaker:martian_basalt_bricks, spacebreaker:martian_basalt_bricks | spacebreaker:martian_basalt_brick_stairs_from_martian_basalt_bricks_stonecutting: spacebreaker:martian_basalt_bricks
- `martian_basalt_brick_slab`: spacebreaker:martian_basalt_brick_slab_from_martian_basalt_bricks_stonecutting: spacebreaker:martian_basalt_bricks | spacebreaker:martian_basalt_brick_slab: spacebreaker:martian_basalt_bricks, spacebreaker:martian_basalt_bricks, spacebreaker:martian_basalt_bricks
- `martian_basalt_brick_wall`: spacebreaker:martian_basalt_brick_wall: spacebreaker:martian_basalt_bricks, spacebreaker:martian_basalt_bricks, spacebreaker:martian_basalt_bricks, spacebreaker:martian_basalt_bricks, spacebreaker:martian_basalt_bricks, spacebreaker:martian_basalt_bricks | spacebreaker:martian_basalt_brick_wall_from_martian_basalt_bricks_stonecutting: spacebreaker:martian_basalt_bricks
- `martian_sandstone`: spacebreaker:martian_sandstone: spacebreaker:martian_sand, spacebreaker:martian_sand, spacebreaker:martian_sand, spacebreaker:martian_sand
- `martian_sandstone_stairs`: spacebreaker:martian_sandstone_stairs: spacebreaker:martian_sandstone, spacebreaker:martian_sandstone, spacebreaker:martian_sandstone, spacebreaker:martian_sandstone, spacebreaker:martian_sandstone, spacebreaker:martian_sandstone | spacebreaker:martian_sandstone_stairs_from_martian_sandstone_stonecutting: spacebreaker:martian_sandstone
- `martian_sandstone_slab`: spacebreaker:martian_sandstone_slab_from_martian_sandstone_stonecutting: spacebreaker:martian_sandstone | spacebreaker:martian_sandstone_slab: spacebreaker:martian_sandstone, spacebreaker:martian_sandstone, spacebreaker:martian_sandstone
- `martian_sandstone_wall`: spacebreaker:martian_sandstone_wall_from_martian_sandstone_stonecutting: spacebreaker:martian_sandstone | spacebreaker:martian_sandstone_wall: spacebreaker:martian_sandstone, spacebreaker:martian_sandstone, spacebreaker:martian_sandstone, spacebreaker:martian_sandstone, spacebreaker:martian_sandstone, spacebreaker:martian_sandstone
- `martian_mudstone`: no recipe, loot, drop or world source
- `gypsum_block`: spacebreaker:gypsum_block: spacebreaker:gypsum_crystal, spacebreaker:gypsum_crystal, spacebreaker:gypsum_crystal, spacebreaker:gypsum_crystal
- `martian_ice`: no recipe, loot, drop or world source
- `dusty_ice`: no recipe, loot, drop or world source
- `dry_ice`: no recipe, loot, drop or world source
- `hematite_ore`: no recipe, loot, drop or world source
- `gypsum_ore`: no recipe, loot, drop or world source
- `phobos_regolith`: no recipe, loot, drop or world source
- `deimos_regolith`: no recipe, loot, drop or world source
- `carbonaceous_chondrite`: no recipe, loot, drop or world source
- `raw_hematite`: no recipe, loot, drop or world source
- `gypsum_crystal`: no recipe, loot, drop or world source
- `perchlorate_salt`: spacebreaker:crushing/perchlorate_soil: spacebreaker:perchlorate_soil
- `venus_basalt`: no recipe, loot, drop or world source
- `venus_soil`: no recipe, loot, drop or world source
- `tessera_rock`: no recipe, loot, drop or world source
- `sulfur_crust`: no recipe, loot, drop or world source
- `metal_frost`: no recipe, loot, drop or world source
- `sulfur`: spacebreaker:crushing/sulfur_crust: spacebreaker:sulfur_crust | spacebreaker:sulfur_from_smelting_ionian_sulfur_ore: spacebreaker:ionian_sulfur_ore | spacebreaker:crushing/hollow_deposit: spacebreaker:hollow_deposit
- `acid_coating`: spacebreaker:acid_coating: spacebreaker:sulfur
- `mercury_regolith`: no recipe, loot, drop or world source
- `hollow_deposit`: no recipe, loot, drop or world source
- `mercurian_crust`: no recipe, loot, drop or world source
- `low_reflectance_rock`: no recipe, loot, drop or world source
- `graphite_ore`: no recipe, loot, drop or world source
- `graphite`: no recipe, loot, drop or world source
- `ceres_regolith`: no recipe, loot, drop or world source
- `ammoniated_clay`: no recipe, loot, drop or world source
- `carbonate_crust`: no recipe, loot, drop or world source
- `cerean_rock`: no recipe, loot, drop or world source
- `cerean_rock_stairs`: spacebreaker:cerean_rock_stairs: spacebreaker:cerean_rock, spacebreaker:cerean_rock, spacebreaker:cerean_rock, spacebreaker:cerean_rock, spacebreaker:cerean_rock, spacebreaker:cerean_rock | spacebreaker:cerean_rock_stairs_from_cerean_rock_stonecutting: spacebreaker:cerean_rock
- `cerean_rock_slab`: spacebreaker:cerean_rock_slab: spacebreaker:cerean_rock, spacebreaker:cerean_rock, spacebreaker:cerean_rock | spacebreaker:cerean_rock_slab_from_cerean_rock_stonecutting: spacebreaker:cerean_rock
- `cerean_rock_wall`: spacebreaker:cerean_rock_wall_from_cerean_rock_stonecutting: spacebreaker:cerean_rock | spacebreaker:cerean_rock_wall: spacebreaker:cerean_rock, spacebreaker:cerean_rock, spacebreaker:cerean_rock, spacebreaker:cerean_rock, spacebreaker:cerean_rock, spacebreaker:cerean_rock
- `cerean_ice`: no recipe, loot, drop or world source
- `magnetite_ore`: no recipe, loot, drop or world source
- `raw_magnetite`: no recipe, loot, drop or world source
- `vestan_regolith`: no recipe, loot, drop or world source
- `diogenitic_regolith`: no recipe, loot, drop or world source
- `eucrite`: no recipe, loot, drop or world source
- `eucrite_stairs`: spacebreaker:eucrite_stairs: spacebreaker:eucrite, spacebreaker:eucrite, spacebreaker:eucrite, spacebreaker:eucrite, spacebreaker:eucrite, spacebreaker:eucrite | spacebreaker:eucrite_stairs_from_eucrite_stonecutting: spacebreaker:eucrite
- `eucrite_slab`: spacebreaker:eucrite_slab_from_eucrite_stonecutting: spacebreaker:eucrite | spacebreaker:eucrite_slab: spacebreaker:eucrite, spacebreaker:eucrite, spacebreaker:eucrite
- `eucrite_wall`: spacebreaker:eucrite_wall: spacebreaker:eucrite, spacebreaker:eucrite, spacebreaker:eucrite, spacebreaker:eucrite, spacebreaker:eucrite, spacebreaker:eucrite | spacebreaker:eucrite_wall_from_eucrite_stonecutting: spacebreaker:eucrite
- `eucrite_bricks`: spacebreaker:eucrite_bricks_from_eucrite_stonecutting: spacebreaker:eucrite | spacebreaker:eucrite_bricks: spacebreaker:eucrite, spacebreaker:eucrite, spacebreaker:eucrite, spacebreaker:eucrite
- `eucrite_brick_stairs`: spacebreaker:eucrite_brick_stairs_from_eucrite_bricks_stonecutting: spacebreaker:eucrite_bricks | spacebreaker:eucrite_brick_stairs: spacebreaker:eucrite_bricks, spacebreaker:eucrite_bricks, spacebreaker:eucrite_bricks, spacebreaker:eucrite_bricks, spacebreaker:eucrite_bricks, spacebreaker:eucrite_bricks | spacebreaker:eucrite_brick_stairs_from_eucrite_stonecutting: spacebreaker:eucrite
- `eucrite_brick_slab`: spacebreaker:eucrite_brick_slab_from_eucrite_bricks_stonecutting: spacebreaker:eucrite_bricks | spacebreaker:eucrite_brick_slab: spacebreaker:eucrite_bricks, spacebreaker:eucrite_bricks, spacebreaker:eucrite_bricks | spacebreaker:eucrite_brick_slab_from_eucrite_stonecutting: spacebreaker:eucrite
- `eucrite_brick_wall`: spacebreaker:eucrite_brick_wall: spacebreaker:eucrite_bricks, spacebreaker:eucrite_bricks, spacebreaker:eucrite_bricks, spacebreaker:eucrite_bricks, spacebreaker:eucrite_bricks, spacebreaker:eucrite_bricks | spacebreaker:eucrite_brick_wall_from_eucrite_stonecutting: spacebreaker:eucrite | spacebreaker:eucrite_brick_wall_from_eucrite_bricks_stonecutting: spacebreaker:eucrite_bricks
- `diogenite`: no recipe, loot, drop or world source
- `chromite_ore`: no recipe, loot, drop or world source
- `chromite`: no recipe, loot, drop or world source
- `europan_frost`: no recipe, loot, drop or world source
- `europan_ice`: no recipe, loot, drop or world source
- `europan_ice_bricks`: spacebreaker:europan_ice_bricks_from_europan_ice_stonecutting: spacebreaker:europan_ice | spacebreaker:europan_ice_bricks: spacebreaker:europan_ice, spacebreaker:europan_ice, spacebreaker:europan_ice, spacebreaker:europan_ice
- `europan_ice_brick_stairs`: spacebreaker:europan_ice_brick_stairs: spacebreaker:europan_ice_bricks, spacebreaker:europan_ice_bricks, spacebreaker:europan_ice_bricks, spacebreaker:europan_ice_bricks, spacebreaker:europan_ice_bricks, spacebreaker:europan_ice_bricks | spacebreaker:europan_ice_brick_stairs_from_europan_ice_bricks_stonecutting: spacebreaker:europan_ice_bricks
- `europan_ice_brick_slab`: spacebreaker:europan_ice_brick_slab: spacebreaker:europan_ice_bricks, spacebreaker:europan_ice_bricks, spacebreaker:europan_ice_bricks | spacebreaker:europan_ice_brick_slab_from_europan_ice_bricks_stonecutting: spacebreaker:europan_ice_bricks
- `europan_ice_brick_wall`: spacebreaker:europan_ice_brick_wall: spacebreaker:europan_ice_bricks, spacebreaker:europan_ice_bricks, spacebreaker:europan_ice_bricks, spacebreaker:europan_ice_bricks, spacebreaker:europan_ice_bricks, spacebreaker:europan_ice_bricks | spacebreaker:europan_ice_brick_wall_from_europan_ice_bricks_stonecutting: spacebreaker:europan_ice_bricks
- `stained_ice`: no recipe, loot, drop or world source
- `refrozen_brine`: no recipe, loot, drop or world source
- `salt_ore`: no recipe, loot, drop or world source
- `ocean_salt`: spacebreaker:ocean_salt_from_smelting_salt_ore: spacebreaker:salt_ore
- `ice_geyser`: no recipe, loot, drop or world source
- `sulfur_sand`: no recipe, loot, drop or world source
- `so2_frost`: no recipe, loot, drop or world source
- `red_sulfur`: no recipe, loot, drop or world source
- `ionian_basalt`: no recipe, loot, drop or world source
- `ionian_basalt_stairs`: spacebreaker:ionian_basalt_stairs: spacebreaker:ionian_basalt, spacebreaker:ionian_basalt, spacebreaker:ionian_basalt, spacebreaker:ionian_basalt, spacebreaker:ionian_basalt, spacebreaker:ionian_basalt | spacebreaker:ionian_basalt_stairs_from_ionian_basalt_stonecutting: spacebreaker:ionian_basalt
- `ionian_basalt_slab`: spacebreaker:ionian_basalt_slab: spacebreaker:ionian_basalt, spacebreaker:ionian_basalt, spacebreaker:ionian_basalt | spacebreaker:ionian_basalt_slab_from_ionian_basalt_stonecutting: spacebreaker:ionian_basalt
- `ionian_basalt_wall`: spacebreaker:ionian_basalt_wall: spacebreaker:ionian_basalt, spacebreaker:ionian_basalt, spacebreaker:ionian_basalt, spacebreaker:ionian_basalt, spacebreaker:ionian_basalt, spacebreaker:ionian_basalt | spacebreaker:ionian_basalt_wall_from_ionian_basalt_stonecutting: spacebreaker:ionian_basalt
- `ionian_sulfur_ore`: no recipe, loot, drop or world source
- `sulfur_vent`: no recipe, loot, drop or world source
- `ganymede_regolith`: no recipe, loot, drop or world source
- `grooved_ice`: no recipe, loot, drop or world source
- `dark_ice_crust`: no recipe, loot, drop or world source
- `callisto_regolith`: no recipe, loot, drop or world source
- `bright_frost`: no recipe, loot, drop or world source
- `callistan_rock`: no recipe, loot, drop or world source
- `callistan_rock_stairs`: spacebreaker:callistan_rock_stairs_from_callistan_rock_stonecutting: spacebreaker:callistan_rock | spacebreaker:callistan_rock_stairs: spacebreaker:callistan_rock, spacebreaker:callistan_rock, spacebreaker:callistan_rock, spacebreaker:callistan_rock, spacebreaker:callistan_rock, spacebreaker:callistan_rock
- `callistan_rock_slab`: spacebreaker:callistan_rock_slab: spacebreaker:callistan_rock, spacebreaker:callistan_rock, spacebreaker:callistan_rock | spacebreaker:callistan_rock_slab_from_callistan_rock_stonecutting: spacebreaker:callistan_rock
- `callistan_rock_wall`: spacebreaker:callistan_rock_wall: spacebreaker:callistan_rock, spacebreaker:callistan_rock, spacebreaker:callistan_rock, spacebreaker:callistan_rock, spacebreaker:callistan_rock, spacebreaker:callistan_rock | spacebreaker:callistan_rock_wall_from_callistan_rock_stonecutting: spacebreaker:callistan_rock
- `tholin_sand`: no recipe, loot, drop or world source
- `tholin_mud`: no recipe, loot, drop or world source
- `titan_ice`: no recipe, loot, drop or world source
- `titan_ice_stairs`: spacebreaker:titan_ice_stairs: spacebreaker:titan_ice, spacebreaker:titan_ice, spacebreaker:titan_ice, spacebreaker:titan_ice, spacebreaker:titan_ice, spacebreaker:titan_ice | spacebreaker:titan_ice_stairs_from_titan_ice_stonecutting: spacebreaker:titan_ice
- `titan_ice_slab`: spacebreaker:titan_ice_slab_from_titan_ice_stonecutting: spacebreaker:titan_ice | spacebreaker:titan_ice_slab: spacebreaker:titan_ice, spacebreaker:titan_ice, spacebreaker:titan_ice
- `titan_ice_wall`: spacebreaker:titan_ice_wall_from_titan_ice_stonecutting: spacebreaker:titan_ice | spacebreaker:titan_ice_wall: spacebreaker:titan_ice, spacebreaker:titan_ice, spacebreaker:titan_ice, spacebreaker:titan_ice, spacebreaker:titan_ice, spacebreaker:titan_ice
- `ice_cobbles`: no recipe, loot, drop or world source
- `tholin_ore`: no recipe, loot, drop or world source
- `tholin`: spacebreaker:tholin_from_smelting_tholin_ore: spacebreaker:tholin_ore
- `enceladus_snow`: no recipe, loot, drop or world source
- `enceladus_ice`: no recipe, loot, drop or world source
- `sulcus_ice`: no recipe, loot, drop or world source

### Special recipes (not analysed)

- `minecraft:armor_dye (minecraft:crafting_special_armordye)`
- `minecraft:banner_duplicate (minecraft:crafting_special_bannerduplicate)`
- `minecraft:book_cloning (minecraft:crafting_special_bookcloning)`
- `minecraft:decorated_pot (minecraft:crafting_decorated_pot)`
- `minecraft:firework_rocket (minecraft:crafting_special_firework_rocket)`
- `minecraft:firework_star (minecraft:crafting_special_firework_star)`
- `minecraft:firework_star_fade (minecraft:crafting_special_firework_star_fade)`
- `minecraft:map_cloning (minecraft:crafting_special_mapcloning)`
- `minecraft:repair_item (minecraft:crafting_special_repairitem)`
- `minecraft:shield_decoration (minecraft:crafting_special_shielddecoration)`
- `minecraft:shulker_box_coloring (minecraft:crafting_special_shulkerboxcoloring)`
- `minecraft:suspicious_stew (minecraft:crafting_special_suspiciousstew)`
- `minecraft:tipped_arrow (minecraft:crafting_special_tippedarrow)`
- `spacebreaker:acid_coating_armor (spacebreaker:acid_coating)`
- `spacebreaker:suit_upgrade (spacebreaker:suit_upgrade)`

### Missions flown by the model

| Destination | Mode | Δv needed (gameplay) | Smallest rocket |
|---|---|---|---|

### Missions the best buildable rocket cannot fly

| Destination | Δv needed for a round trip (gameplay) | Best vacuum Δv of any buildable design | One way: legs (gameplay m/s) |
|---|---|---|---|
| spacebreaker:mercury/surface | 12864 | 0 | spacebreaker:earth/orbit 3158 → spacebreaker:venus/orbit 1238 → spacebreaker:mercury/orbit 3327 → spacebreaker:mercury/surface 1252 = 8975 |
| spacebreaker:mercury/orbit | 10360 | 0 | spacebreaker:earth/orbit 3158 → spacebreaker:venus/orbit 1238 → spacebreaker:mercury/orbit 3327 = 7723 |
| spacebreaker:venus/surface | 9187 | 0 | spacebreaker:earth/orbit 3158 → spacebreaker:venus/surface 1226 = 4384 |
| spacebreaker:venus/orbit | 5559 | 0 | spacebreaker:earth/orbit 3158 → spacebreaker:venus/orbit 1238 = 4396 |
| spacebreaker:earth/orbit | 3180 | 0 | spacebreaker:earth/orbit 3158 = 3158 |
| spacebreaker:moon/surface | 6226 | 0 | spacebreaker:earth/orbit 3158 → spacebreaker:moon/orbit 1384 → spacebreaker:moon/surface 698 = 5240 |
| spacebreaker:moon/orbit | 4830 | 0 | spacebreaker:earth/orbit 3158 → spacebreaker:moon/orbit 1384 = 4542 |
| spacebreaker:mars/surface | 6562 | 0 | spacebreaker:earth/orbit 3158 → spacebreaker:mars/surface 1264 = 4422 |
| spacebreaker:mars/orbit | 5164 | 0 | spacebreaker:earth/orbit 3158 → spacebreaker:mars/orbit 1269 = 4427 |
| spacebreaker:phobos/surface | 5524 | 0 | spacebreaker:earth/orbit 3158 → spacebreaker:mars/orbit 1269 → spacebreaker:phobos/orbit 436 → spacebreaker:phobos/surface 2 = 4866 |
| spacebreaker:phobos/orbit | 5520 | 0 | spacebreaker:earth/orbit 3158 → spacebreaker:mars/orbit 1269 → spacebreaker:phobos/orbit 436 = 4864 |
| spacebreaker:deimos/surface | 5713 | 0 | spacebreaker:earth/orbit 3158 → spacebreaker:mars/orbit 1269 → spacebreaker:deimos/orbit 613 → spacebreaker:deimos/surface 2 = 5042 |
| spacebreaker:deimos/orbit | 5710 | 0 | spacebreaker:earth/orbit 3158 → spacebreaker:mars/orbit 1269 → spacebreaker:deimos/orbit 613 = 5041 |
| spacebreaker:vesta/surface | 7456 | 0 | spacebreaker:earth/orbit 3158 → spacebreaker:mars/orbit 1269 → spacebreaker:vesta/orbit 1406 → spacebreaker:vesta/surface 77 = 5911 |
| spacebreaker:vesta/orbit | 7302 | 0 | spacebreaker:earth/orbit 3158 → spacebreaker:mars/orbit 1269 → spacebreaker:vesta/orbit 1406 = 5834 |
| spacebreaker:ceres/surface | 7980 | 0 | spacebreaker:earth/orbit 3158 → spacebreaker:mars/orbit 1269 → spacebreaker:ceres/orbit 1727 → spacebreaker:ceres/surface 121 = 6274 |
| spacebreaker:ceres/orbit | 7739 | 0 | spacebreaker:earth/orbit 3158 → spacebreaker:mars/orbit 1269 → spacebreaker:ceres/orbit 1727 = 6154 |
| spacebreaker:jupiter/orbit | 11631 | 0 | spacebreaker:earth/orbit 3158 → spacebreaker:jupiter/orbit 2279 = 5437 |
| spacebreaker:io/surface | 11331 | 0 | spacebreaker:earth/orbit 3158 → spacebreaker:io/orbit 4462 → spacebreaker:io/surface 728 = 8348 |
| spacebreaker:io/orbit | 9875 | 0 | spacebreaker:earth/orbit 3158 → spacebreaker:io/orbit 4462 = 7620 |
| spacebreaker:europa/surface | 10310 | 0 | spacebreaker:earth/orbit 3158 → spacebreaker:europa/orbit 4093 → spacebreaker:europa/surface 586 = 7837 |
| spacebreaker:europa/orbit | 9138 | 0 | spacebreaker:earth/orbit 3158 → spacebreaker:europa/orbit 4093 = 7251 |
| spacebreaker:callisto/surface | 9304 | 0 | spacebreaker:earth/orbit 3158 → spacebreaker:callisto/orbit 3461 → spacebreaker:callisto/surface 716 = 7334 |
| spacebreaker:callisto/orbit | 7872 | 0 | spacebreaker:earth/orbit 3158 → spacebreaker:callisto/orbit 3461 = 6618 |
| spacebreaker:ganymede/surface | 9924 | 0 | spacebreaker:earth/orbit 3158 → spacebreaker:ganymede/orbit 3688 → spacebreaker:ganymede/surface 798 = 7644 |
| spacebreaker:ganymede/orbit | 8327 | 0 | spacebreaker:earth/orbit 3158 → spacebreaker:ganymede/orbit 3688 = 6846 |
| spacebreaker:saturn/orbit | 9545 | 0 | spacebreaker:earth/orbit 3158 → spacebreaker:saturn/orbit 2593 = 5751 |
| spacebreaker:enceladus/surface | 9735 | 0 | spacebreaker:earth/orbit 3158 → spacebreaker:titan/orbit 2552 → spacebreaker:enceladus/orbit 1835 → spacebreaker:enceladus/surface 66 = 7611 |
| spacebreaker:enceladus/orbit | 9604 | 0 | spacebreaker:earth/orbit 3158 → spacebreaker:titan/orbit 2552 → spacebreaker:enceladus/orbit 1835 = 7545 |
| spacebreaker:titan/surface | 7615 | 0 | spacebreaker:earth/orbit 3158 → spacebreaker:titan/surface 2549 = 5707 |
| spacebreaker:titan/orbit | 6743 | 0 | spacebreaker:earth/orbit 3158 → spacebreaker:titan/orbit 2552 = 5710 |
| spacebreaker:uranus/orbit | 8313 | 0 | spacebreaker:earth/orbit 3158 → spacebreaker:uranus/orbit 2818 = 5976 |
| spacebreaker:miranda/surface | 8665 | 0 | spacebreaker:earth/orbit 3158 → spacebreaker:miranda/orbit 4097 → spacebreaker:miranda/surface 53 = 7308 |
| spacebreaker:miranda/orbit | 8560 | 0 | spacebreaker:earth/orbit 3158 → spacebreaker:miranda/orbit 4097 = 7255 |
| spacebreaker:neptune/orbit | 8578 | 0 | spacebreaker:earth/orbit 3158 → spacebreaker:neptune/orbit 2915 = 6073 |
| spacebreaker:triton/surface | 8525 | 0 | spacebreaker:earth/orbit 3158 → spacebreaker:triton/orbit 3705 → spacebreaker:triton/surface 422 = 7285 |
| spacebreaker:triton/orbit | 7681 | 0 | spacebreaker:earth/orbit 3158 → spacebreaker:triton/orbit 3705 = 6863 |
| spacebreaker:pluto/surface | 8608 | 0 | spacebreaker:earth/orbit 3158 → spacebreaker:titan/orbit 2552 → spacebreaker:pluto/orbit 1132 → spacebreaker:pluto/surface 352 = 7194 |
| spacebreaker:pluto/orbit | 7904 | 0 | spacebreaker:earth/orbit 3158 → spacebreaker:titan/orbit 2552 → spacebreaker:pluto/orbit 1132 = 6842 |
| spacebreaker:charon/surface | 8275 | 0 | spacebreaker:earth/orbit 3158 → spacebreaker:titan/orbit 2552 → spacebreaker:charon/orbit 1127 → spacebreaker:charon/surface 172 = 7009 |
| spacebreaker:charon/orbit | 7932 | 0 | spacebreaker:earth/orbit 3158 → spacebreaker:titan/orbit 2552 → spacebreaker:charon/orbit 1127 = 6837 |


## What if the source-less technologies got a disk on Earth

Assumed disks: airlocks, cryogenics, electrolysis, eva_mobility, fusion_drive, fusion_power, heavy_launch, nuclear_thermal_propulsion, orbital_shipyard, oxygen_storage, pressurised_modules, suit_mk3, thermal_regulation

<!-- generated by the GameTest qa/ProgressionAuditTests (realism BALANCED) -->
### Progression model summary

- recipes analysed: 1674, special recipes skipped: 15
- worlds: 23, technologies: 80, model steps: 22
- machines that lock themselves until researched: arc_furnace, atmosphere_processor, chemical_reactor, cryogenic_liquefier, electrolyzer, fabricator, ice_melter, press, pumpjack, refinery, regolith_heater, sabatier_reactor

### Goals

#### ✅ First rocket launch (reaches Earth orbit, return not required)

Research in this order:

1. step 3: **metal_pressing** — tech disk
1. step 3: **aluminium_smelting** — analysed spacebreaker:earth
1. step 3: **solar_power** — analysed spacebreaker:earth
1. step 3: **metallurgy** — analysed spacebreaker:earth
1. step 3: **basic_rocketry** — tech disk
1. step 4: **electronics** — tech disk
1. step 4: **fuel_refining** — tech disk
1. step 4: **precision_manufacturing** — analysed spacebreaker:earth
1. step 4: **vacuum_engines** — tech disk
1. step 4: **staging** — tech disk
1. step 4: **liquid_fuel_engines** — tech disk
1. step 4: **guidance_systems** — tech disk
1. step 5: **electrolysis** — tech disk
1. step 5: **landing_systems** — tech disk
1. step 6: **cryogenics** — tech disk

Machines built on the way: arc_furnace, combustion_generator, cryogenic_liquefier, electrolyzer, fabricator, launch_pad, press, pumpjack, refinery, research_lab, rocket_assembler

#### ✅ Earth orbit and back

Research in this order:

1. step 3: **metal_pressing** — tech disk
1. step 3: **aluminium_smelting** — analysed spacebreaker:earth
1. step 3: **solar_power** — analysed spacebreaker:earth
1. step 3: **metallurgy** — analysed spacebreaker:earth
1. step 3: **basic_rocketry** — tech disk
1. step 4: **electronics** — tech disk
1. step 4: **fuel_refining** — tech disk
1. step 4: **precision_manufacturing** — analysed spacebreaker:earth
1. step 4: **vacuum_engines** — tech disk
1. step 4: **staging** — tech disk
1. step 4: **liquid_fuel_engines** — tech disk
1. step 4: **guidance_systems** — tech disk
1. step 5: **electrolysis** — tech disk
1. step 5: **landing_systems** — tech disk
1. step 6: **cryogenics** — tech disk

Machines built on the way: arc_furnace, combustion_generator, cryogenic_liquefier, electrolyzer, fabricator, launch_pad, press, pumpjack, refinery, research_lab, rocket_assembler

Flights:

- step 10: spacebreaker:earth_orbit — round trip to spacebreaker:earth/orbit with 1-stage [vacuum_engine, fuel_tank_large, fuel_tank_medium, heat_shield, crew_capsule], 4705 m/s vacuum Δv

#### ✅ Moon landing and return

Research in this order:

1. step 3: **metal_pressing** — tech disk
1. step 3: **aluminium_smelting** — analysed spacebreaker:earth
1. step 3: **solar_power** — analysed spacebreaker:earth
1. step 3: **metallurgy** — analysed spacebreaker:earth
1. step 3: **basic_rocketry** — tech disk
1. step 4: **electronics** — tech disk
1. step 4: **fuel_refining** — tech disk
1. step 4: **precision_manufacturing** — analysed spacebreaker:earth
1. step 4: **vacuum_engines** — tech disk
1. step 4: **staging** — tech disk
1. step 4: **liquid_fuel_engines** — tech disk
1. step 4: **guidance_systems** — tech disk
1. step 5: **electrolysis** — tech disk
1. step 5: **landing_systems** — tech disk
1. step 6: **cryogenics** — tech disk

Machines built on the way: arc_furnace, combustion_generator, cryogenic_liquefier, electrolyzer, fabricator, launch_pad, press, pumpjack, refinery, research_lab, rocket_assembler

Flights:

- step 10: spacebreaker:moon — round trip to spacebreaker:moon/surface with 2-stage [kerosene_engine, 3× fuel_tank_large / decoupler / vacuum_engine, fuel_tank_large, heat_shield, crew_capsule], 6625 m/s vacuum Δv

#### ✅ Mars landing and return

Research in this order:

1. step 3: **metal_pressing** — tech disk
1. step 3: **aluminium_smelting** — analysed spacebreaker:earth
1. step 3: **solar_power** — analysed spacebreaker:earth
1. step 3: **metallurgy** — analysed spacebreaker:earth
1. step 3: **basic_rocketry** — tech disk
1. step 4: **electronics** — tech disk
1. step 4: **fuel_refining** — tech disk
1. step 4: **precision_manufacturing** — analysed spacebreaker:earth
1. step 4: **vacuum_engines** — tech disk
1. step 4: **staging** — tech disk
1. step 4: **liquid_fuel_engines** — tech disk
1. step 4: **guidance_systems** — tech disk
1. step 5: **electrolysis** — tech disk
1. step 5: **landing_systems** — tech disk
1. step 6: **cryogenics** — tech disk

Machines built on the way: arc_furnace, combustion_generator, cryogenic_liquefier, electrolyzer, fabricator, launch_pad, press, pumpjack, refinery, research_lab, rocket_assembler

Flights:

- step 10: spacebreaker:mars — round trip to spacebreaker:mars/surface with 2-stage [kerosene_engine, 3× fuel_tank_large / decoupler / vacuum_engine, 2× fuel_tank_large, heat_shield, crew_capsule], 7130 m/s vacuum Δv

### Technologies in the order a fresh world can learn them

- step 3: `aluminium_smelting` — analysed spacebreaker:earth
- step 3: `basic_rocketry` — tech disk
- step 3: `field_geology` — analysed spacebreaker:earth
- step 3: `launch_infrastructure` — tech disk
- step 3: `metal_pressing` — tech disk
- step 3: `metallurgy` — analysed spacebreaker:earth
- step 3: `observational_astronomy` — tech disk
- step 3: `orbital_mechanics` — tech disk
- step 3: `pressure_suits` — analysed spacebreaker:earth
- step 3: `pressurised_modules` — tech disk
- step 3: `solar_power` — analysed spacebreaker:earth
- step 3: `solid_rocket_motors` — tech disk
- step 3: `thermal_regulation` — tech disk
- step 4: `electronics` — tech disk
- step 4: `fuel_refining` — tech disk
- step 4: `guidance_systems` — tech disk
- step 4: `liquid_fuel_engines` — tech disk
- step 4: `oxygen_storage` — tech disk
- step 4: `precision_manufacturing` — analysed spacebreaker:earth
- step 4: `radio_communication` — tech disk
- step 4: `staging` — tech disk
- step 4: `vacuum_engines` — tech disk
- step 5: `airlocks` — tech disk
- step 5: `electrolysis` — tech disk
- step 5: `eva_mobility` — tech disk
- step 5: `heavy_launch` — tech disk
- step 5: `landing_systems` — tech disk
- step 6: `cryogenics` — tech disk
- step 11: `acid_resistant_coatings` — tech disk
- step 11: `advanced_electronics` — tech disk
- step 11: `dust_proof_seals` — tech disk
- step 11: `heat_resistant_electronics` — tech disk
- step 11: `hydrogen_engines` — tech disk
- step 11: `interplanetary_transfers` — tech disk
- step 11: `ion_propulsion` — tech disk
- step 11: `lunar_surveying` — tech disk
- step 11: `martian_geology` — tech disk
- step 11: `oxygen_distribution` — tech disk
- step 11: `phobos_survey` — tech disk
- step 11: `polar_ice_mining` — tech disk
- step 11: `regolith_sintering` — tech disk
- step 11: `rovers` — tech disk
- step 11: `sabatier_process` — tech disk
- step 11: `satellites` — tech disk
- step 11: `solar_shielding` — tech disk
- step 11: `space_probes` — tech disk
- step 11: `space_stations` — tech disk
- step 11: `spectroscopy` — tech disk
- step 11: `suit_mk2` — tech disk
- step 11: `suit_upgrades` — tech disk
- step 11: `thermal_suits` — tech disk
- step 11: `water_recycling` — tech disk
- step 12: `aerial_mobility` — tech disk
- step 12: `asteroid_mining` — analysed spacebreaker:meteorite
- step 12: `astrobiology` — tech disk
- step 12: `cloud_platforms` — tech disk
- step 12: `gravity_assists` — tech disk
- step 12: `heated_suits` — tech disk
- step 12: `isru` — tech disk
- step 12: `methane_engines` — tech disk
- step 12: `microgravity_mining` — tech disk
- step 12: `titan_expedition` — analysed spacebreaker:titan
- step 13: `hydrocarbon_processing` — tech disk
- step 16: `brine_chemistry` — tech disk
- step 19: `nuclear_fission` — tech disk
- step 19: `nuclear_thermal_propulsion` — tech disk
- step 19: `radiation_shielding` — tech disk
- step 19: `suit_mk3` — tech disk
- step 19: `volcanic_heat_shielding` — tech disk
- step 20: `deep_space_habitats` — tech disk
- step 20: `deep_space_network` — tech disk
- step 20: `fusion_power` — tech disk
- step 20: `icy_moons` — tech disk
- step 20: `magnetospheric_physics` — tech disk
- step 20: `orbital_shipyard` — tech disk
- step 20: `radiation_hardened_electronics` — tech disk
- step 20: `radiation_hardened_suits` — analysed spacebreaker:europa
- step 21: `cryobot_drilling` — tech disk
- step 21: `fusion_drive` — tech disk
- step 21: `plume_sampling` — tech disk

### Technologies that cannot be learned

None.

### Worlds

| World | Reached | How | Survival when first reached |
|---|---|---|---|
| minecraft:overworld | yes | step 0: start | 8…22 °C, 101.3 kPa breathable, radiation 0.01; suits: eva; power there yes |
| minecraft:the_nether | yes | step 0: nether portal (vanilla) | — |
| minecraft:the_end | yes | step 0: end portal (vanilla) | — |
| spacebreaker:callisto | yes | step 15: flight with refuelling at spacebreaker:callisto/surface with 2-stage [methalox_engine, 3× fuel_tank_large / decoupler / methalox_vacuum_engine, 2× fuel_tank_large, heat_shield, crew_capsule], 7880 m/s vacuum Δv | -193…-108 °C, 0.0 kPa, radiation 0.10; suits: heated; oxygen tank yes, filler yes, O₂ at home yes, O₂ made there yes; power there yes |
| spacebreaker:ceres | yes | step 15: round trip to spacebreaker:ceres/surface with 2-stage [methalox_engine, 3× fuel_tank_large / decoupler / methalox_vacuum_engine, 3× fuel_tank_large, heat_shield, crew_capsule], 8164 m/s vacuum Δv | -143…-38 °C, 0.0 kPa, radiation 1.40; suits: eva/thermal/heated; oxygen tank yes, filler yes, O₂ at home yes, O₂ made there yes; power there yes |
| spacebreaker:deimos | yes | step 10: round trip to spacebreaker:deimos/surface with 2-stage [kerosene_engine, 2× fuel_tank_large / decoupler / vacuum_engine, fuel_tank_large, heat_shield, crew_capsule], 6003 m/s vacuum Δv | -110…-40 °C, 0.0 kPa, radiation 1.20; suits: eva; oxygen tank yes, filler yes, O₂ at home yes, O₂ made there NO; power there yes |
| spacebreaker:earth_orbit | yes | step 10: round trip to spacebreaker:earth/orbit with 1-stage [vacuum_engine, fuel_tank_large, fuel_tank_medium, heat_shield, crew_capsule], 4705 m/s vacuum Δv | in orbit (vacuum, inside the capsule); oxygen tank yes, filler yes, O₂ at home yes, O₂ made there NO; power there yes |
| spacebreaker:enceladus | yes | step 15: flight with refuelling at spacebreaker:enceladus/surface with 2-stage [methalox_engine, 3× fuel_tank_large / decoupler / methalox_vacuum_engine, 2× fuel_tank_large, heat_shield, crew_capsule], 7880 m/s vacuum Δv | -223…-198 °C, 0.0 kPa, radiation 2.00; suits: heated; oxygen tank yes, filler yes, O₂ at home yes, O₂ made there yes; power there yes |
| spacebreaker:europa | yes | step 15: flight with refuelling at spacebreaker:europa/surface with 2-stage [methalox_engine, 3× fuel_tank_large / decoupler / methalox_vacuum_engine, 3× fuel_tank_large, heat_shield, crew_capsule], 8164 m/s vacuum Δv | -190…-150 °C, 0.0 kPa, radiation 5400.00; suits: heated; oxygen tank yes, filler yes, O₂ at home yes, O₂ made there yes; power there yes |
| spacebreaker:ganymede | yes | step 15: flight with refuelling at spacebreaker:ganymede/surface with 2-stage [methalox_engine, 3× fuel_tank_large / decoupler / methalox_vacuum_engine, 2× fuel_tank_large, heat_shield, crew_capsule], 7880 m/s vacuum Δv | -200…-121 °C, 0.0 kPa, radiation 80.00; suits: heated; oxygen tank yes, filler yes, O₂ at home yes, O₂ made there yes; power there yes |
| spacebreaker:io | yes | step 18: flight with refuelling at spacebreaker:io/surface with 3-stage [methalox_engine, 2× fuel_tank_large / decoupler / methalox_vacuum_engine, 3× fuel_tank_large / decoupler / methalox_vacuum_engine, fuel_tank_large, heat_shield, crew_capsule], 8761 m/s vacuum Δv | -183…-143 °C, 0.0 kPa, radiation 36000.00; suits: heated; oxygen tank yes, filler yes, O₂ at home yes, O₂ made there yes; power there yes |
| spacebreaker:mars | yes | step 10: round trip to spacebreaker:mars/surface with 2-stage [kerosene_engine, 3× fuel_tank_large / decoupler / vacuum_engine, 2× fuel_tank_large, heat_shield, crew_capsule], 7130 m/s vacuum Δv | -90…-10 °C, 0.6 kPa, radiation 0.67; suits: eva; oxygen tank yes, filler yes, O₂ at home yes, O₂ made there NO; power there yes |
| spacebreaker:mars_orbit | yes | step 10: round trip to spacebreaker:mars/orbit with 2-stage [kerosene_engine, 2× fuel_tank_large / decoupler / vacuum_engine, fuel_tank_large, heat_shield, crew_capsule], 6003 m/s vacuum Δv | in orbit (vacuum, inside the capsule); oxygen tank yes, filler yes, O₂ at home yes, O₂ made there NO; power there yes |
| spacebreaker:mercury | **no** | — | — |
| spacebreaker:mercury_orbit | one way | one way only: one-way flight to spacebreaker:mercury/orbit with 3-stage [kerosene_engine, fuel_tank_large, fuel_tank_medium / decoupler / vacuum_engine, 3× fuel_tank_large / decoupler / vacuum_engine, fuel_tank_large, heat_shield, crew_capsule], 7848 m/s vacuum Δv | — |
| spacebreaker:moon | yes | step 10: round trip to spacebreaker:moon/surface with 2-stage [kerosene_engine, 3× fuel_tank_large / decoupler / vacuum_engine, fuel_tank_large, heat_shield, crew_capsule], 6625 m/s vacuum Δv | -173…120 °C, 0.0 kPa, radiation 1.37; NO SUIT covers the temperature range yet; oxygen tank yes, filler yes, O₂ at home yes, O₂ made there NO; power there yes |
| spacebreaker:moon_orbit | yes | step 10: round trip to spacebreaker:moon/orbit with 1-stage [kerosene_engine, 3× fuel_tank_large, heat_shield, crew_capsule], 5083 m/s vacuum Δv | in orbit (vacuum, inside the capsule); oxygen tank yes, filler yes, O₂ at home yes, O₂ made there NO; power there yes |
| spacebreaker:orbit | yes | step 10: round trip to spacebreaker:phobos/orbit with 2-stage [kerosene_engine, 2× fuel_tank_large / decoupler / vacuum_engine, fuel_tank_large, heat_shield, crew_capsule], 6003 m/s vacuum Δv | in orbit (vacuum, inside the capsule); oxygen tank yes, filler yes, O₂ at home yes, O₂ made there yes; power there yes |
| spacebreaker:phobos | yes | step 10: round trip to spacebreaker:phobos/surface with 2-stage [kerosene_engine, 2× fuel_tank_large / decoupler / vacuum_engine, fuel_tank_large, heat_shield, crew_capsule], 6003 m/s vacuum Δv | -112…-4 °C, 0.0 kPa, radiation 1.00; suits: eva; oxygen tank yes, filler yes, O₂ at home yes, O₂ made there NO; power there yes |
| spacebreaker:titan | yes | step 10: round trip to spacebreaker:titan/surface with 3-stage [kerosene_engine, fuel_tank_large, fuel_tank_medium, fins / decoupler / vacuum_engine, 3× fuel_tank_large / decoupler / vacuum_engine, fuel_tank_large, heat_shield, crew_capsule], 7845 m/s vacuum Δv | -180…-179 °C, 146.7 kPa, radiation 0.01; NO SUIT covers the temperature range yet; oxygen tank yes, filler yes, O₂ at home yes, O₂ made there NO; power there yes |
| spacebreaker:venus | yes | step 15: flight with refuelling at spacebreaker:venus/surface with 1-stage [methalox_engine, 2× fuel_tank_large, heat_shield, crew_capsule], 5022 m/s vacuum Δv | 464…464 °C, 9200.0 kPa, radiation 0.00; suits: thermal; oxygen tank yes, filler yes, O₂ at home yes, O₂ made there yes; power there yes |
| spacebreaker:venus_orbit | yes | step 10: round trip to spacebreaker:venus/orbit with 2-stage [kerosene_engine, 2× fuel_tank_large / decoupler / vacuum_engine, fuel_tank_large, heat_shield, crew_capsule], 6003 m/s vacuum Δv | in orbit (vacuum, inside the capsule); oxygen tank yes, filler yes, O₂ at home yes, O₂ made there NO; power there yes |
| spacebreaker:vesta | yes | step 10: round trip to spacebreaker:vesta/surface with 3-stage [kerosene_engine, fuel_tank_large / decoupler / vacuum_engine, 3× fuel_tank_large / decoupler / vacuum_engine, fuel_tank_large, heat_shield, crew_capsule], 7515 m/s vacuum Δv | -190…-20 °C, 0.0 kPa, radiation 1.40; NO SUIT covers the temperature range yet; oxygen tank yes, filler yes, O₂ at home yes, O₂ made there NO; power there yes |

### Items that cannot be obtained

- `hologram_projector`: no recipe, loot, drop or world source
- `mercury_regolith`: no recipe, loot, drop or world source
- `hollow_deposit`: no recipe, loot, drop or world source
- `mercurian_crust`: no recipe, loot, drop or world source
- `low_reflectance_rock`: no recipe, loot, drop or world source
- `graphite_ore`: no recipe, loot, drop or world source
- `graphite`: no recipe, loot, drop or world source

### Special recipes (not analysed)

- `minecraft:armor_dye (minecraft:crafting_special_armordye)`
- `minecraft:banner_duplicate (minecraft:crafting_special_bannerduplicate)`
- `minecraft:book_cloning (minecraft:crafting_special_bookcloning)`
- `minecraft:decorated_pot (minecraft:crafting_decorated_pot)`
- `minecraft:firework_rocket (minecraft:crafting_special_firework_rocket)`
- `minecraft:firework_star (minecraft:crafting_special_firework_star)`
- `minecraft:firework_star_fade (minecraft:crafting_special_firework_star_fade)`
- `minecraft:map_cloning (minecraft:crafting_special_mapcloning)`
- `minecraft:repair_item (minecraft:crafting_special_repairitem)`
- `minecraft:shield_decoration (minecraft:crafting_special_shielddecoration)`
- `minecraft:shulker_box_coloring (minecraft:crafting_special_shulkerboxcoloring)`
- `minecraft:suspicious_stew (minecraft:crafting_special_suspiciousstew)`
- `minecraft:tipped_arrow (minecraft:crafting_special_tippedarrow)`
- `spacebreaker:acid_coating_armor (spacebreaker:acid_coating)`
- `spacebreaker:suit_upgrade (spacebreaker:suit_upgrade)`

### Missions flown by the model

| Destination | Mode | Δv needed (gameplay) | Smallest rocket |
|---|---|---|---|
| spacebreaker:venus/orbit | round_trip | 5559 | 2-stage [kerosene_engine, 2× fuel_tank_large / decoupler / vacuum_engine, fuel_tank_large, heat_shield, crew_capsule], 6003 m/s vacuum Δv |
| spacebreaker:earth/surface | round_trip | 0 | 1-stage [vacuum_engine, fuel_tank_large, crew_capsule], 3887 m/s vacuum Δv |
| spacebreaker:earth/orbit | round_trip | 3180 | 1-stage [vacuum_engine, fuel_tank_large, fuel_tank_medium, heat_shield, crew_capsule], 4705 m/s vacuum Δv |
| spacebreaker:moon/surface | round_trip | 6226 | 2-stage [kerosene_engine, 3× fuel_tank_large / decoupler / vacuum_engine, fuel_tank_large, heat_shield, crew_capsule], 6625 m/s vacuum Δv |
| spacebreaker:moon/orbit | round_trip | 4830 | 1-stage [kerosene_engine, 3× fuel_tank_large, heat_shield, crew_capsule], 5083 m/s vacuum Δv |
| spacebreaker:mars/surface | round_trip | 6562 | 2-stage [kerosene_engine, 3× fuel_tank_large / decoupler / vacuum_engine, 2× fuel_tank_large, heat_shield, crew_capsule], 7130 m/s vacuum Δv |
| spacebreaker:mars/orbit | round_trip | 5164 | 2-stage [kerosene_engine, 2× fuel_tank_large / decoupler / vacuum_engine, fuel_tank_large, heat_shield, crew_capsule], 6003 m/s vacuum Δv |
| spacebreaker:phobos/surface | round_trip | 5524 | 2-stage [kerosene_engine, 2× fuel_tank_large / decoupler / vacuum_engine, fuel_tank_large, heat_shield, crew_capsule], 6003 m/s vacuum Δv |
| spacebreaker:phobos/orbit | round_trip | 5520 | 2-stage [kerosene_engine, 2× fuel_tank_large / decoupler / vacuum_engine, fuel_tank_large, heat_shield, crew_capsule], 6003 m/s vacuum Δv |
| spacebreaker:deimos/surface | round_trip | 5713 | 2-stage [kerosene_engine, 2× fuel_tank_large / decoupler / vacuum_engine, fuel_tank_large, heat_shield, crew_capsule], 6003 m/s vacuum Δv |
| spacebreaker:deimos/orbit | round_trip | 5710 | 2-stage [kerosene_engine, 2× fuel_tank_large / decoupler / vacuum_engine, fuel_tank_large, heat_shield, crew_capsule], 6003 m/s vacuum Δv |
| spacebreaker:vesta/surface | round_trip | 7456 | 3-stage [kerosene_engine, fuel_tank_large / decoupler / vacuum_engine, 3× fuel_tank_large / decoupler / vacuum_engine, fuel_tank_large, heat_shield, crew_capsule], 7515 m/s vacuum Δv |
| spacebreaker:vesta/orbit | round_trip | 7302 | 3-stage [kerosene_engine, fuel_tank_large / decoupler / vacuum_engine, 3× fuel_tank_large / decoupler / vacuum_engine, fuel_tank_large, heat_shield, crew_capsule], 7515 m/s vacuum Δv |
| spacebreaker:ceres/orbit | round_trip | 7739 | 3-stage [kerosene_engine, fuel_tank_large, fuel_tank_medium / decoupler / vacuum_engine, 3× fuel_tank_large / decoupler / vacuum_engine, fuel_tank_large, heat_shield, crew_capsule], 7848 m/s vacuum Δv |
| spacebreaker:titan/surface | round_trip | 7615 | 3-stage [kerosene_engine, fuel_tank_large, fuel_tank_medium, fins / decoupler / vacuum_engine, 3× fuel_tank_large / decoupler / vacuum_engine, fuel_tank_large, heat_shield, crew_capsule], 7845 m/s vacuum Δv |
| spacebreaker:titan/orbit | round_trip | 6743 | 2-stage [kerosene_engine, 3× fuel_tank_large / decoupler / vacuum_engine, 2× fuel_tank_large, heat_shield, crew_capsule], 7130 m/s vacuum Δv |
| spacebreaker:triton/orbit | round_trip | 7681 | 3-stage [kerosene_engine, fuel_tank_large, fuel_tank_medium / decoupler / vacuum_engine, 3× fuel_tank_large / decoupler / vacuum_engine, fuel_tank_large, heat_shield, crew_capsule], 7848 m/s vacuum Δv |
| spacebreaker:venus/surface | refuel_there | 4384 | 1-stage [methalox_engine, 2× fuel_tank_large, heat_shield, crew_capsule], 5022 m/s vacuum Δv |
| spacebreaker:ceres/surface | round_trip | 7980 | 2-stage [methalox_engine, 3× fuel_tank_large / decoupler / methalox_vacuum_engine, 3× fuel_tank_large, heat_shield, crew_capsule], 8164 m/s vacuum Δv |
| spacebreaker:europa/surface | refuel_there | 7837 | 2-stage [methalox_engine, 3× fuel_tank_large / decoupler / methalox_vacuum_engine, 3× fuel_tank_large, heat_shield, crew_capsule], 8164 m/s vacuum Δv |
| spacebreaker:callisto/surface | refuel_there | 7334 | 2-stage [methalox_engine, 3× fuel_tank_large / decoupler / methalox_vacuum_engine, 2× fuel_tank_large, heat_shield, crew_capsule], 7880 m/s vacuum Δv |
| spacebreaker:callisto/orbit | round_trip | 7872 | 2-stage [methalox_engine, 3× fuel_tank_large / decoupler / methalox_vacuum_engine, 3× fuel_tank_large, heat_shield, crew_capsule], 8164 m/s vacuum Δv |
| spacebreaker:ganymede/surface | refuel_there | 7644 | 2-stage [methalox_engine, 3× fuel_tank_large / decoupler / methalox_vacuum_engine, 2× fuel_tank_large, heat_shield, crew_capsule], 7880 m/s vacuum Δv |
| spacebreaker:ganymede/orbit | round_trip | 8327 | 3-stage [methalox_engine, 2× fuel_tank_large / decoupler / methalox_vacuum_engine, 3× fuel_tank_large / decoupler / methalox_vacuum_engine, fuel_tank_large, heat_shield, crew_capsule], 8761 m/s vacuum Δv |
| spacebreaker:enceladus/surface | refuel_there | 7611 | 2-stage [methalox_engine, 3× fuel_tank_large / decoupler / methalox_vacuum_engine, 2× fuel_tank_large, heat_shield, crew_capsule], 7880 m/s vacuum Δv |
| spacebreaker:uranus/orbit | round_trip | 8313 | 3-stage [methalox_engine, 2× fuel_tank_large / decoupler / methalox_vacuum_engine, 3× fuel_tank_large / decoupler / methalox_vacuum_engine, fuel_tank_large, heat_shield, crew_capsule], 8761 m/s vacuum Δv |
| spacebreaker:miranda/orbit | round_trip | 8560 | 3-stage [methalox_engine, 2× fuel_tank_large / decoupler / methalox_vacuum_engine, 3× fuel_tank_large / decoupler / methalox_vacuum_engine, fuel_tank_large, heat_shield, crew_capsule], 8761 m/s vacuum Δv |
| spacebreaker:neptune/orbit | round_trip | 8578 | 3-stage [methalox_engine, 2× fuel_tank_large / decoupler / methalox_vacuum_engine, 3× fuel_tank_large / decoupler / methalox_vacuum_engine, fuel_tank_large, heat_shield, crew_capsule], 8761 m/s vacuum Δv |
| spacebreaker:pluto/orbit | round_trip | 7904 | 2-stage [methalox_engine, 3× fuel_tank_large / decoupler / methalox_vacuum_engine, 3× fuel_tank_large, heat_shield, crew_capsule], 8164 m/s vacuum Δv |
| spacebreaker:charon/orbit | round_trip | 7932 | 2-stage [methalox_engine, 3× fuel_tank_large / decoupler / methalox_vacuum_engine, 3× fuel_tank_large, heat_shield, crew_capsule], 8164 m/s vacuum Δv |
| spacebreaker:io/surface | refuel_there | 8348 | 3-stage [methalox_engine, 2× fuel_tank_large / decoupler / methalox_vacuum_engine, 3× fuel_tank_large / decoupler / methalox_vacuum_engine, fuel_tank_large, heat_shield, crew_capsule], 8761 m/s vacuum Δv |

### Missions the best buildable rocket cannot fly

| Destination | Δv needed for a round trip (gameplay) | Best vacuum Δv of any buildable design | One way: legs (gameplay m/s) |
|---|---|---|---|
| spacebreaker:mercury/surface | 12864 | 9794 | spacebreaker:earth/orbit 3158 → spacebreaker:venus/orbit 1238 → spacebreaker:mercury/orbit 3327 → spacebreaker:mercury/surface 1252 = 8975 |
| spacebreaker:mercury/orbit | 10360 | 9794 | spacebreaker:earth/orbit 3158 → spacebreaker:venus/orbit 1238 → spacebreaker:mercury/orbit 3327 = 7723 |
| spacebreaker:jupiter/orbit | 11631 | 9794 | spacebreaker:earth/orbit 3158 → spacebreaker:jupiter/orbit 2279 = 5437 |
| spacebreaker:io/orbit | 9875 | 9794 | spacebreaker:earth/orbit 3158 → spacebreaker:io/orbit 4462 = 7620 |
| spacebreaker:europa/orbit | 9138 | 9794 | spacebreaker:earth/orbit 3158 → spacebreaker:europa/orbit 4093 = 7251 |
| spacebreaker:saturn/orbit | 9545 | 9794 | spacebreaker:earth/orbit 3158 → spacebreaker:saturn/orbit 2593 = 5751 |
| spacebreaker:enceladus/orbit | 9604 | 9794 | spacebreaker:earth/orbit 3158 → spacebreaker:titan/orbit 2552 → spacebreaker:enceladus/orbit 1835 = 7545 |
| spacebreaker:miranda/surface | 8665 | 9794 | spacebreaker:earth/orbit 3158 → spacebreaker:miranda/orbit 4097 → spacebreaker:miranda/surface 53 = 7308 |
| spacebreaker:triton/surface | 8525 | 9794 | spacebreaker:earth/orbit 3158 → spacebreaker:triton/orbit 3705 → spacebreaker:triton/surface 422 = 7285 |
| spacebreaker:pluto/surface | 8608 | 9794 | spacebreaker:earth/orbit 3158 → spacebreaker:titan/orbit 2552 → spacebreaker:pluto/orbit 1132 → spacebreaker:pluto/surface 352 = 7194 |
| spacebreaker:charon/surface | 8275 | 9794 | spacebreaker:earth/orbit 3158 → spacebreaker:titan/orbit 2552 → spacebreaker:charon/orbit 1127 → spacebreaker:charon/surface 172 = 7009 |

# Localization audit (`tools/qa/lang_audit.py`)

1305 English and 1305 Turkish keys.

## Errors (0)

None.

## Warnings (1)

- `propellant.spacebreaker.kerolox` is the English text in tr_tr: "RP-1/LOX"
