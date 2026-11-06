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
| QA-1 | ~~Blocker~~ **fixed** (research d6f395c) | research (+ industry) | No liquid oxygen before space → the first rocket can never be fuelled. |
| QA-2 | ~~High~~ **fixed** (research d6f395c) | research | `suit_mk3` has no samples and no disk anywhere → `icy_moons`, `cryobot_drilling`, `plume_sampling` can never be learned. |
| QA-3 | High — fixed on dev branches (inner ISRU + rocketry hydrolox 7cc36b7), open on 1.21.1 until merged | inner planets + rocketry | Mercury surface and orbit: no round trip with any buildable rocket and no fuel there → all Mercury content unreachable. |
| QA-4 | Medium | suit | Moon night (−173 °C) is colder than the Mk1 EVA rating (−160 °C); the first better suit needs Mars samples. |
| QA-5 | Medium | suit (blueprint) | `hologram_projector` has no recipe or loot: survival players cannot use "Show hologram". |
| QA-6 | Info | research | Several technologies have no source at all yet (future content): `fusion_power`, `fusion_drive`, `nuclear_thermal_propulsion`, `orbital_shipyard`. |
| QA-7 | Info — guide wording fixed (research b623d9e) | research | `unlocks` lists are enforced only for machines, rocket parts and suit tiers; crafting is never gated (e.g. `sample_kit` is in `field_geology`, which is learned from Earth samples that need the kit — a cycle if crafting gating is ever added). |

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

### Status after round 1 fixes (1.21.1 @ c4426e2 + merge)
- Every one of the 80 technologies is learnable in survival; `KNOWN_DEAD_ENDS` is empty again, so any new dead end fails
  `noNewTechnologyDeadEnds`.
- First launch, Earth orbit, Moon and Mars round trips work; Uranus/Neptune/Pluto worlds (Triton, Pluto, Charon) are
  covered and reachable.
- With dev/research + dev/inner + dev/rocketry merged together (tested locally, 205/205 GameTests): Mercury surface is
  reached with a 2-stage hydrolox rocket (9408 m/s vacuum) that refuels on Mercury from local ice/graphite.
- Still open: QA-4 (Moon night vs Mk1), QA-5 (hologram projector recipe). Info: Mercury −180…430 °C is not covered by any
  single suit (thermal −170…500, heated −250…80) — play in the terminator zone or say so in the guide.

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

- recipes analysed: 1692, special recipes skipped: 15
- worlds: 26, technologies: 83, model steps: 22
- machines that lock themselves until researched: arc_furnace, atmosphere_processor, chemical_reactor, cryogenic_liquefier, electrolyzer, fabricator, ice_melter, press, pumpjack, refinery, regolith_heater, sabatier_reactor

### Goals

### ✅ First rocket launch (reaches Earth orbit, return not required)

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
1. step 5: **electrolysis** — analysed spacebreaker:earth
1. step 5: **landing_systems** — tech disk
1. step 6: **cryogenics** — analysed spacebreaker:earth

Machines built on the way: arc_furnace, combustion_generator, cryogenic_liquefier, electrolyzer, fabricator, launch_pad, press, pumpjack, refinery, research_lab, rocket_assembler

### ✅ Earth orbit and back

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
1. step 5: **electrolysis** — analysed spacebreaker:earth
1. step 5: **landing_systems** — tech disk
1. step 6: **cryogenics** — analysed spacebreaker:earth

Machines built on the way: arc_furnace, combustion_generator, cryogenic_liquefier, electrolyzer, fabricator, launch_pad, press, pumpjack, refinery, research_lab, rocket_assembler

Flights:

- step 10: spacebreaker:earth_orbit — round trip to spacebreaker:earth/orbit with 1-stage [kerosene_engine, fuel_tank_large, heat_shield, crew_capsule], 3274 m/s vacuum Δv

### ✅ Moon landing and return

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
1. step 5: **electrolysis** — analysed spacebreaker:earth
1. step 5: **landing_systems** — tech disk
1. step 6: **cryogenics** — analysed spacebreaker:earth

Machines built on the way: arc_furnace, combustion_generator, cryogenic_liquefier, electrolyzer, fabricator, launch_pad, press, pumpjack, refinery, research_lab, rocket_assembler

Flights:

- step 10: spacebreaker:moon — round trip to spacebreaker:moon/surface with 2-stage [kerosene_engine, 3× fuel_tank_large / decoupler / vacuum_engine, fuel_tank_large, heat_shield, crew_capsule], 6625 m/s vacuum Δv

### ✅ Mars landing and return

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
1. step 5: **electrolysis** — analysed spacebreaker:earth
1. step 5: **landing_systems** — tech disk
1. step 6: **cryogenics** — analysed spacebreaker:earth

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
- step 3: `pressurised_modules` — analysed spacebreaker:earth
- step 3: `solar_power` — analysed spacebreaker:earth
- step 3: `solid_rocket_motors` — tech disk
- step 3: `thermal_regulation` — analysed spacebreaker:earth
- step 4: `electronics` — tech disk
- step 4: `fuel_refining` — tech disk
- step 4: `guidance_systems` — tech disk
- step 4: `liquid_fuel_engines` — tech disk
- step 4: `oxygen_storage` — analysed spacebreaker:earth
- step 4: `precision_manufacturing` — analysed spacebreaker:earth
- step 4: `radio_communication` — tech disk
- step 4: `staging` — tech disk
- step 4: `vacuum_engines` — tech disk
- step 5: `airlocks` — analysed spacebreaker:earth
- step 5: `electrolysis` — analysed spacebreaker:earth
- step 5: `landing_systems` — tech disk
- step 6: `cryogenics` — analysed spacebreaker:earth
- step 11: `acid_resistant_coatings` — tech disk
- step 11: `advanced_electronics` — tech disk
- step 11: `dust_proof_seals` — tech disk
- step 11: `eva_mobility` — tech disk
- step 11: `heat_resistant_electronics` — tech disk
- step 11: `heavy_launch` — tech disk
- step 11: `hydrogen_engines` — tech disk
- step 11: `interplanetary_transfers` — tech disk
- step 11: `ion_propulsion` — tech disk
- step 11: `lunar_surveying` — tech disk
- step 11: `martian_geology` — tech disk
- step 11: `nuclear_fission` — analysed spacebreaker:moon
- step 11: `nuclear_thermal_propulsion` — analysed spacebreaker:mars
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
- step 12: `asteroid_mining` — analysed spacebreaker:meteorite
- step 12: `astrobiology` — tech disk
- step 12: `cloud_platforms` — tech disk
- step 12: `deep_space_network` — tech disk
- step 12: `fusion_power` — analysed spacebreaker:moon
- step 12: `gravity_assists` — tech disk
- step 12: `heated_suits` — tech disk
- step 12: `isru` — tech disk
- step 12: `methane_engines` — tech disk
- step 12: `microgravity_mining` — tech disk
- step 12: `orbital_shipyard` — analysed spacebreaker:orbital_flight
- step 12: `radiation_shielding` — tech disk
- step 16: `aerial_mobility` — tech disk
- step 16: `brine_chemistry` — tech disk
- step 16: `deep_space_habitats` — tech disk
- step 16: `fusion_drive` — analysed spacebreaker:ganymede
- step 16: `hydrocarbon_processing` — tech disk
- step 16: `kuiper_belt_navigation` — tech disk
- step 16: `magnetospheric_physics` — tech disk
- step 16: `radiation_hardened_electronics` — tech disk
- step 16: `radiation_hardened_suits` — analysed spacebreaker:europa
- step 16: `suit_mk3` — analysed spacebreaker:ganymede
- step 16: `titan_expedition` — analysed spacebreaker:titan
- step 17: `icy_moons` — tech disk
- step 17: `nitrogen_glaciology` — tech disk
- step 18: `cryobot_drilling` — tech disk
- step 18: `plume_sampling` — tech disk
- step 19: `cryovolcanism` — tech disk
- step 21: `volcanic_heat_shielding` — tech disk

### Technologies that cannot be learned

None.

### Worlds

| World | Reached | How | Survival when first reached |
|---|---|---|---|
| minecraft:overworld | yes | step 0: start | 8…22 °C, 101.3 kPa breathable, radiation 0.01; suits: eva; power there yes |
| minecraft:the_nether | yes | step 0: nether portal (vanilla) | — |
| minecraft:the_end | yes | step 0: end portal (vanilla) | — |
| spacebreaker:callisto | yes | step 15: flight with refuelling at spacebreaker:callisto/surface with 2-stage [methalox_engine, 3× fuel_tank_large / decoupler / methalox_vacuum_engine, 2× fuel_tank_large, heat_shield, crew_capsule], 7880 m/s vacuum Δv | -193…-108 °C, 0.0 kPa, radiation 0.10; suits: heated; oxygen tank yes, filler yes, O₂ at home yes, O₂ made there yes; power there yes |
| spacebreaker:ceres | yes | step 15: round trip to spacebreaker:ceres/surface with 3-stage [methalox_engine, fuel_tank_large / decoupler / methalox_vacuum_engine, 3× fuel_tank_large / decoupler / methalox_vacuum_engine, fuel_tank_large, heat_shield, crew_capsule], 8253 m/s vacuum Δv | -143…-38 °C, 0.0 kPa, radiation 1.40; suits: eva/thermal/heated; oxygen tank yes, filler yes, O₂ at home yes, O₂ made there yes; power there yes |
| spacebreaker:charon | yes | step 15: round trip to spacebreaker:charon/surface with 3-stage [methalox_engine, 2× fuel_tank_large / decoupler / methalox_vacuum_engine, 3× fuel_tank_large / decoupler / methalox_vacuum_engine, fuel_tank_large, heat_shield, crew_capsule], 8761 m/s vacuum Δv | -230…-220 °C, 0.0 kPa, radiation 1.40; suits: heated; oxygen tank yes, filler yes, O₂ at home yes, O₂ made there yes; power there yes |
| spacebreaker:deimos | yes | step 10: round trip to spacebreaker:deimos/surface with 2-stage [kerosene_engine, 2× fuel_tank_large / decoupler / vacuum_engine, fuel_tank_large, heat_shield, crew_capsule], 6003 m/s vacuum Δv | -110…-40 °C, 0.0 kPa, radiation 1.20; suits: eva; oxygen tank yes, filler yes, O₂ at home yes, O₂ made there NO; power there yes |
| spacebreaker:earth_orbit | yes | step 10: round trip to spacebreaker:earth/orbit with 1-stage [kerosene_engine, fuel_tank_large, heat_shield, crew_capsule], 3274 m/s vacuum Δv | in orbit (vacuum, inside the capsule); oxygen tank yes, filler yes, O₂ at home yes, O₂ made there NO; power there yes |
| spacebreaker:enceladus | yes | step 15: flight with refuelling at spacebreaker:enceladus/surface with 2-stage [methalox_engine, 3× fuel_tank_large / decoupler / methalox_vacuum_engine, 2× fuel_tank_large, heat_shield, crew_capsule], 7880 m/s vacuum Δv | -223…-198 °C, 0.0 kPa, radiation 2.00; suits: heated; oxygen tank yes, filler yes, O₂ at home yes, O₂ made there yes; power there yes |
| spacebreaker:europa | yes | step 15: flight with refuelling at spacebreaker:europa/surface with 2-stage [methalox_engine, 3× fuel_tank_large / decoupler / methalox_vacuum_engine, 3× fuel_tank_large, heat_shield, crew_capsule], 8164 m/s vacuum Δv | -190…-150 °C, 0.0 kPa, radiation 5400.00; suits: heated; oxygen tank yes, filler yes, O₂ at home yes, O₂ made there yes; power there yes |
| spacebreaker:ganymede | yes | step 15: flight with refuelling at spacebreaker:ganymede/surface with 2-stage [methalox_engine, 3× fuel_tank_large / decoupler / methalox_vacuum_engine, 2× fuel_tank_large, heat_shield, crew_capsule], 7880 m/s vacuum Δv | -200…-121 °C, 0.0 kPa, radiation 80.00; suits: heated; oxygen tank yes, filler yes, O₂ at home yes, O₂ made there yes; power there yes |
| spacebreaker:io | yes | step 20: flight with refuelling at spacebreaker:io/surface with 3-stage [methalox_engine, 2× fuel_tank_large / decoupler / methalox_vacuum_engine, 3× fuel_tank_large / decoupler / methalox_vacuum_engine, fuel_tank_large, heat_shield, crew_capsule], 8761 m/s vacuum Δv | -183…-143 °C, 0.0 kPa, radiation 36000.00; suits: heated/radiation; oxygen tank yes, filler yes, O₂ at home yes, O₂ made there yes; power there yes |
| spacebreaker:mars | yes | step 10: round trip to spacebreaker:mars/surface with 2-stage [kerosene_engine, 3× fuel_tank_large / decoupler / vacuum_engine, 2× fuel_tank_large, heat_shield, crew_capsule], 7130 m/s vacuum Δv | -90…-10 °C, 0.6 kPa, radiation 0.67; suits: eva; oxygen tank yes, filler yes, O₂ at home yes, O₂ made there NO; power there yes |
| spacebreaker:mars_orbit | yes | step 10: round trip to spacebreaker:mars/orbit with 2-stage [kerosene_engine, fuel_tank_large / decoupler / vacuum_engine, fuel_tank_large, heat_shield, crew_capsule], 5123 m/s vacuum Δv | in orbit (vacuum, inside the capsule); oxygen tank yes, filler yes, O₂ at home yes, O₂ made there NO; power there yes |
| spacebreaker:mercury | **no** | — | — |
| spacebreaker:mercury_orbit | one way | one way only: one-way flight to spacebreaker:mercury/orbit with 2-stage [methalox_engine, 3× fuel_tank_large / decoupler / methalox_vacuum_engine, 2× fuel_tank_large, heat_shield, crew_capsule], 7880 m/s vacuum Δv | — |
| spacebreaker:moon | yes | step 10: round trip to spacebreaker:moon/surface with 2-stage [kerosene_engine, 3× fuel_tank_large / decoupler / vacuum_engine, fuel_tank_large, heat_shield, crew_capsule], 6625 m/s vacuum Δv | -173…120 °C, 0.0 kPa, radiation 1.37; NO SUIT covers the temperature range yet; oxygen tank yes, filler yes, O₂ at home yes, O₂ made there NO; power there yes |
| spacebreaker:moon_orbit | yes | step 10: round trip to spacebreaker:moon/orbit with 1-stage [kerosene_engine, 3× fuel_tank_large, heat_shield, crew_capsule], 5083 m/s vacuum Δv | in orbit (vacuum, inside the capsule); oxygen tank yes, filler yes, O₂ at home yes, O₂ made there NO; power there yes |
| spacebreaker:orbit | yes | step 10: round trip to spacebreaker:phobos/orbit with 2-stage [kerosene_engine, 2× fuel_tank_large / decoupler / vacuum_engine, fuel_tank_large, heat_shield, crew_capsule], 6003 m/s vacuum Δv | in orbit (vacuum, inside the capsule); oxygen tank yes, filler yes, O₂ at home yes, O₂ made there yes; power there yes |
| spacebreaker:phobos | yes | step 10: round trip to spacebreaker:phobos/surface with 2-stage [kerosene_engine, 2× fuel_tank_large / decoupler / vacuum_engine, fuel_tank_large, heat_shield, crew_capsule], 6003 m/s vacuum Δv | -112…-4 °C, 0.0 kPa, radiation 1.00; suits: eva; oxygen tank yes, filler yes, O₂ at home yes, O₂ made there NO; power there yes |
| spacebreaker:pluto | yes | step 15: round trip to spacebreaker:pluto/surface with 3-stage [methalox_engine, 2× fuel_tank_large, fins / decoupler / methalox_vacuum_engine, 3× fuel_tank_large / decoupler / methalox_vacuum_engine, fuel_tank_large, heat_shield, crew_capsule], 8758 m/s vacuum Δv | -233…-223 °C, 0.0 kPa, radiation 1.40; suits: heated; oxygen tank yes, filler yes, O₂ at home yes, O₂ made there yes; power there yes |
| spacebreaker:titan | yes | step 15: round trip to spacebreaker:titan/surface with 2-stage [methalox_engine, 3× fuel_tank_large / decoupler / methalox_vacuum_engine, 3× fuel_tank_large, heat_shield, crew_capsule], 8164 m/s vacuum Δv | -180…-179 °C, 146.7 kPa, radiation 0.01; suits: heated; oxygen tank yes, filler yes, O₂ at home yes, O₂ made there yes; power there yes |
| spacebreaker:triton | yes | step 15: round trip to spacebreaker:triton/surface with 3-stage [methalox_engine, 2× fuel_tank_large / decoupler / methalox_vacuum_engine, 3× fuel_tank_large / decoupler / methalox_vacuum_engine, fuel_tank_large, heat_shield, crew_capsule], 8761 m/s vacuum Δv | -236…-235 °C, 0.0 kPa, radiation 1.50; suits: heated; oxygen tank yes, filler yes, O₂ at home yes, O₂ made there yes; power there yes |
| spacebreaker:venus | yes | step 15: flight with refuelling at spacebreaker:venus/surface with 1-stage [methalox_engine, 2× fuel_tank_large, heat_shield, crew_capsule], 5022 m/s vacuum Δv | 464…464 °C, 9200.0 kPa, radiation 0.00; suits: thermal; oxygen tank yes, filler yes, O₂ at home yes, O₂ made there yes; power there yes |
| spacebreaker:venus_orbit | yes | step 10: round trip to spacebreaker:venus/orbit with 2-stage [kerosene_engine, 2× fuel_tank_large / decoupler / vacuum_engine, fuel_tank_large, heat_shield, crew_capsule], 6003 m/s vacuum Δv | in orbit (vacuum, inside the capsule); oxygen tank yes, filler yes, O₂ at home yes, O₂ made there NO; power there yes |
| spacebreaker:vesta | yes | step 10: round trip to spacebreaker:vesta/surface with 3-stage [kerosene_engine, fuel_tank_large, fuel_tank_medium / decoupler / vacuum_engine, 3× fuel_tank_large / decoupler / vacuum_engine, fuel_tank_large, heat_shield, crew_capsule], 7848 m/s vacuum Δv | -190…-20 °C, 0.0 kPa, radiation 1.40; NO SUIT covers the temperature range yet; oxygen tank yes, filler yes, O₂ at home yes, O₂ made there NO; power there yes |

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
| spacebreaker:earth/orbit | round_trip | 3180 | 1-stage [kerosene_engine, fuel_tank_large, heat_shield, crew_capsule], 3274 m/s vacuum Δv |
| spacebreaker:moon/surface | round_trip | 6226 | 2-stage [kerosene_engine, 3× fuel_tank_large / decoupler / vacuum_engine, fuel_tank_large, heat_shield, crew_capsule], 6625 m/s vacuum Δv |
| spacebreaker:moon/orbit | round_trip | 4830 | 1-stage [kerosene_engine, 3× fuel_tank_large, heat_shield, crew_capsule], 5083 m/s vacuum Δv |
| spacebreaker:mars/surface | round_trip | 6562 | 2-stage [kerosene_engine, 3× fuel_tank_large / decoupler / vacuum_engine, 2× fuel_tank_large, heat_shield, crew_capsule], 7130 m/s vacuum Δv |
| spacebreaker:mars/orbit | round_trip | 5164 | 2-stage [kerosene_engine, fuel_tank_large / decoupler / vacuum_engine, fuel_tank_large, heat_shield, crew_capsule], 5123 m/s vacuum Δv |
| spacebreaker:phobos/surface | round_trip | 5524 | 2-stage [kerosene_engine, 2× fuel_tank_large / decoupler / vacuum_engine, fuel_tank_large, heat_shield, crew_capsule], 6003 m/s vacuum Δv |
| spacebreaker:phobos/orbit | round_trip | 5520 | 2-stage [kerosene_engine, 2× fuel_tank_large / decoupler / vacuum_engine, fuel_tank_large, heat_shield, crew_capsule], 6003 m/s vacuum Δv |
| spacebreaker:deimos/surface | round_trip | 5713 | 2-stage [kerosene_engine, 2× fuel_tank_large / decoupler / vacuum_engine, fuel_tank_large, heat_shield, crew_capsule], 6003 m/s vacuum Δv |
| spacebreaker:deimos/orbit | round_trip | 5710 | 2-stage [kerosene_engine, 2× fuel_tank_large / decoupler / vacuum_engine, fuel_tank_large, heat_shield, crew_capsule], 6003 m/s vacuum Δv |
| spacebreaker:vesta/surface | round_trip | 7456 | 3-stage [kerosene_engine, fuel_tank_large, fuel_tank_medium / decoupler / vacuum_engine, 3× fuel_tank_large / decoupler / vacuum_engine, fuel_tank_large, heat_shield, crew_capsule], 7848 m/s vacuum Δv |
| spacebreaker:vesta/orbit | round_trip | 7302 | 3-stage [kerosene_engine, fuel_tank_large / decoupler / vacuum_engine, 3× fuel_tank_large / decoupler / vacuum_engine, fuel_tank_large, heat_shield, crew_capsule], 7515 m/s vacuum Δv |
| spacebreaker:titan/orbit | round_trip | 6743 | 2-stage [kerosene_engine, 3× fuel_tank_large / decoupler / vacuum_engine, 2× fuel_tank_large, heat_shield, crew_capsule], 7130 m/s vacuum Δv |
| spacebreaker:venus/surface | refuel_there | 4384 | 1-stage [methalox_engine, 2× fuel_tank_large, heat_shield, crew_capsule], 5022 m/s vacuum Δv |
| spacebreaker:ceres/surface | round_trip | 7980 | 3-stage [methalox_engine, fuel_tank_large / decoupler / methalox_vacuum_engine, 3× fuel_tank_large / decoupler / methalox_vacuum_engine, fuel_tank_large, heat_shield, crew_capsule], 8253 m/s vacuum Δv |
| spacebreaker:ceres/orbit | round_trip | 7739 | 2-stage [methalox_engine, 3× fuel_tank_large / decoupler / methalox_vacuum_engine, 2× fuel_tank_large, heat_shield, crew_capsule], 7880 m/s vacuum Δv |
| spacebreaker:europa/surface | refuel_there | 7837 | 2-stage [methalox_engine, 3× fuel_tank_large / decoupler / methalox_vacuum_engine, 3× fuel_tank_large, heat_shield, crew_capsule], 8164 m/s vacuum Δv |
| spacebreaker:callisto/surface | refuel_there | 7334 | 2-stage [methalox_engine, 3× fuel_tank_large / decoupler / methalox_vacuum_engine, 2× fuel_tank_large, heat_shield, crew_capsule], 7880 m/s vacuum Δv |
| spacebreaker:callisto/orbit | round_trip | 7872 | 2-stage [methalox_engine, 3× fuel_tank_large / decoupler / methalox_vacuum_engine, 3× fuel_tank_large, heat_shield, crew_capsule], 8164 m/s vacuum Δv |
| spacebreaker:ganymede/surface | refuel_there | 7644 | 2-stage [methalox_engine, 3× fuel_tank_large / decoupler / methalox_vacuum_engine, 2× fuel_tank_large, heat_shield, crew_capsule], 7880 m/s vacuum Δv |
| spacebreaker:ganymede/orbit | round_trip | 8327 | 3-stage [methalox_engine, 2× fuel_tank_large / decoupler / methalox_vacuum_engine, 3× fuel_tank_large / decoupler / methalox_vacuum_engine, fuel_tank_large, heat_shield, crew_capsule], 8761 m/s vacuum Δv |
| spacebreaker:enceladus/surface | refuel_there | 7611 | 2-stage [methalox_engine, 3× fuel_tank_large / decoupler / methalox_vacuum_engine, 2× fuel_tank_large, heat_shield, crew_capsule], 7880 m/s vacuum Δv |
| spacebreaker:titan/surface | round_trip | 7615 | 2-stage [methalox_engine, 3× fuel_tank_large / decoupler / methalox_vacuum_engine, 3× fuel_tank_large, heat_shield, crew_capsule], 8164 m/s vacuum Δv |
| spacebreaker:uranus/orbit | round_trip | 8313 | 3-stage [methalox_engine, 2× fuel_tank_large / decoupler / methalox_vacuum_engine, 3× fuel_tank_large / decoupler / methalox_vacuum_engine, fuel_tank_large, heat_shield, crew_capsule], 8761 m/s vacuum Δv |
| spacebreaker:miranda/orbit | round_trip | 8560 | 3-stage [methalox_engine, 2× fuel_tank_large / decoupler / methalox_vacuum_engine, 3× fuel_tank_large / decoupler / methalox_vacuum_engine, fuel_tank_large, heat_shield, crew_capsule], 8761 m/s vacuum Δv |
| spacebreaker:neptune/orbit | round_trip | 8578 | 3-stage [methalox_engine, 2× fuel_tank_large, fins / decoupler / methalox_vacuum_engine, 3× fuel_tank_large / decoupler / methalox_vacuum_engine, fuel_tank_large, heat_shield, crew_capsule], 8758 m/s vacuum Δv |
| spacebreaker:triton/surface | round_trip | 8525 | 3-stage [methalox_engine, 2× fuel_tank_large / decoupler / methalox_vacuum_engine, 3× fuel_tank_large / decoupler / methalox_vacuum_engine, fuel_tank_large, heat_shield, crew_capsule], 8761 m/s vacuum Δv |
| spacebreaker:triton/orbit | round_trip | 7681 | 2-stage [methalox_engine, 3× fuel_tank_large / decoupler / methalox_vacuum_engine, 2× fuel_tank_large, heat_shield, crew_capsule], 7880 m/s vacuum Δv |
| spacebreaker:pluto/surface | round_trip | 8608 | 3-stage [methalox_engine, 2× fuel_tank_large, fins / decoupler / methalox_vacuum_engine, 3× fuel_tank_large / decoupler / methalox_vacuum_engine, fuel_tank_large, heat_shield, crew_capsule], 8758 m/s vacuum Δv |
| spacebreaker:pluto/orbit | round_trip | 7904 | 2-stage [methalox_engine, 3× fuel_tank_large / decoupler / methalox_vacuum_engine, 3× fuel_tank_large, heat_shield, crew_capsule], 8164 m/s vacuum Δv |
| spacebreaker:charon/surface | round_trip | 8275 | 3-stage [methalox_engine, 2× fuel_tank_large / decoupler / methalox_vacuum_engine, 3× fuel_tank_large / decoupler / methalox_vacuum_engine, fuel_tank_large, heat_shield, crew_capsule], 8761 m/s vacuum Δv |
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
| spacebreaker:miranda/surface | world not built yet | — | — |

# Localization audit (`tools/qa/lang_audit.py`)

1377 English and 1377 Turkish keys.

## Errors (0)

None.

## Warnings (1)

- `propellant.spacebreaker.kerolox` is the English text in tr_tr: "RP-1/LOX"
