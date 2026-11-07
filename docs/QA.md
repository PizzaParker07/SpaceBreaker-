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

## Findings — round 2 (2026-11-07, 1.21.1 @ 92d675c)

Base state: all 80 technologies learnable, no unobtainable item, every built world reachable and returnable (Mercury surface
with refuelling there, Mercury orbit round trip with hydrolox). New paths checked: rover, uncrewed satellite/probe
missions, station kit + docking port, hydrolox to Mercury.

| ID | Severity | Owner | Finding |
|---|---|---|---|
| QA-8 | Medium — **fixed** (rocketry 8a41e2b2) | rocketry | The star map said "not enough fuel" when the real problem was a missing heat shield. (The first report also claimed satellites/probes could not carry a heat shield: that was a QA model error, see below.) |
| QA-9 | Info — **fixed** (industry b125472: tanks keep their contents; rocketry da199207: station fuel depot) | rocketry + industry | A station can only be refuelled with fuel carried up in canisters/buckets: tanks drop empty and nothing can be made in orbit. |
| QA-10 | Info — **fixed** (research 9f07601: Moon / orbital flight samples) | research | `space_probes` is learned from Mars samples (or a disk), so probes come after the first crewed Mars trip and cannot scout Mars first. |

### QA-8 Uncrewed missions blocked by the heat shield rule
- The star map flies uncrewed rockets on `StarMapData.flightPath` (Earth orbit → straight to the target). Arriving at a world
  with air needs heat-shield rating 2 (`Transfers.reentryRating`: direct entry / aerocapture from another body).
- A satellite or probe sits in the capsule slot. Correction (rocketry): `shield_without_capsule` only fires when that slot
  is empty, so a satellite **can** fly on a heat shield; the first QA run wrongly assumed it could not, and the satellite
  part of this finding was a model error. Re-run with the corrected model on 1.21.1 @ 92d675c: satellites and probes on a
  heat shield already reach every world, so only the misleading "not enough fuel" message was a real bug.
- `StarMapData.trip` uses `MissionPlanner.fly(...).possible()` as "enough fuel"; `fly` fails with `heat_shield`, so the map
  shows **"Not enough fuel"** (wrong reason) and blocks Go even when the rocket has thousands of m/s to spare.
- Model result (hydrolox, BALANCED): probes land on the Moon, Callisto, Europa, Ganymede, Io, Miranda, Triton and every
  airless world; satellites reach every airless orbit. Blocked: probe → Mars, Venus, Titan; satellite → Mars, Venus,
  Titan, Jupiter, Saturn, Uranus, Neptune orbits.
- Fix options: allow a heat shield under a satellite/probe (aeroshell), give the probe part its own heat-shield rating
  (`heat_shield` field in `space_probe.json`), or route uncrewed rockets through the target orbit with a propulsive capture.
  Separately, `StarMapData.trip` should report `heat_shield` instead of "not enough fuel".
- Fixed in dev/rocketry 8a41e2b2: `space_probe` has its own aeroshell (heat_shield 2), the star map reports a separate
  `NEEDS_HEAT_SHIELD` reason. Tested merged with 1.21.1: all 40 satellite/probe missions fly.
- Guard: `moonAndMarsStayReachableOnceResearchIsFixed` now also requires a Moon satellite and a Moon probe to fly.

### QA-9 Station refuelling
- The docking port only passes fluids from pipes into the rocket. `TankBlock` drops itself without its contents and orbit has
  no resources, so every millibucket must come up in cryogenic canisters (4 buckets each) or buckets and be poured into a tank
  at the station. It works, but filling a hydrolox stage takes dozens of canisters. Idea: tanks keep their contents
  when broken (block entity data on the item), or a "propellant depot" payload.

### Rover
- `rover` is craftable once `rovers` is known (step 8 of the model, after the first Moon samples). It charges from its roof
  panel or from powered blocks next to it; the RTG needs `rtg_fuel_pellet` (chemical reactor, `acid_resistant_coatings`),
  reachable at step 11 through a Moon disk or later from Venus samples. No lock. Range/charge rates are not modelled.

### Status after round 2 fixes (1.21.1 @ 6e9af27)
- Re-run of the model: 80/80 technologies, no unobtainable item, all built worlds reached and returnable, all 40
  satellite/probe missions fly. QA-8, QA-9, QA-10 closed.
- QA-11 (Moon, low): `MoonLifeGameTests.outpostHasAirlockLootAndGuards` is flaky — failed 1 of 3 full runs on the same commit
  with "drones guard the outpost, found 1" (`MoonLifeGameTests.java:155`). The drones are counted right after the
  dispenser spawns them; they can drift out of the box (or one is lost) before the check. Suggest counting in
  `succeedWhen` over a few ticks, or counting spawned drones instead of drones still inside the bounding box.

- Model rule (inner planets, by design): Mercury's local industry (polar ice in −240 °C cold traps) needs the heated suit
  (`ProgressionModel.WORK_SUIT`). With it, the full heated suit is ready at step 13; on 1.21.1 @ 6e9af27 Mercury surface is
  a plain hydrolox round trip (step 15, 2-stage, 9408 m/s vacuum), so the ice-mine refuel is optional, not a lock.

## Client playthrough (`-Pscenario=qa_playthrough`)
`LIBGL_ALWAYS_SOFTWARE=1 xvfb-run -a -s "-screen 0 1920x1080x24" ./gradlew runShowcase -Pscenario=qa_playthrough`
(`testmod/showcase/QaPlaythroughScenario.java`): survival bench (research lab with a disk waiting for prerequisites,
combustion generator, press, fabricator, electrolyser, cryogenic liquefier, each GUI opened), then the rocketry scenes in
flight order (pad, assembler, launch console, ignition, liftoff, ascent, Earth orbit, orbit HUD, star map, Moon descent,
Moon landing), the EVA suit HUD on the Moon with two tanks, and a Turkish pass (rocket camera HUD, suit HUD).
Compressed shots: `docs/screenshots/qa/` (22 images, vanilla renderer, 2026-11-07, 1.21.1 @ 6e9af27).

Reviewed: no missing textures (purple/black), no untranslated keys, no invisible entities, Turkish HUD and key hints correct.
- QA-12 (research, low): Research Lab status line is cut off — "Learn the prerequisit…" (`gui.spacebreaker.lab.status.9`,
  `qa02_research_lab.jpg`). Shorter text ("Missing prerequisites" / "Ön koşul eksik") or a tooltip with the full line.
- QA-13 (research, info): advancement "Welcome Home" (`safe_return`, a challenge under "One Small Step") is also granted by
  criterion `b` = any change from `earth_orbit` to the Overworld, e.g. coming down from the very first orbital flight or with
  the emergency beacon, before any Moon landing. If it should mean "back from the Moon", drop criterion `b` or require the
  milestone only.
- Suit HUD: two filled tanks show 10:00 of oxygen on the Moon. Not yet compared with real flight + EVA times (next round).

- QA-12, QA-13 **fixed** (research bac61e9b): status line "Missing prerequisites" / "Ön koşul eksik" with a full-text tooltip;
  "Welcome Home" now needs the Moon + a soft capsule landing on Earth. The ordering leftover is fixed too (rocketry: the
  rocket remembers it has been to another body, SAFE_RETURN only for such flights).
- QA-14 (inner, low): `aerostatFloatsWithLootAndGuards` failed once ("acid ray guardians, found 0") in a research run and
  passed on retry — same pattern as QA-11 (entities counted right after spawning). Reported by research to inner.

## Round 3 (2026-11-07, 1.21.1 @ 9435c3d)

- Progression model re-run: 80/80 technologies, no unobtainable item, every built world reached and returnable, 40/40
  uncrewed missions. With the Mercury cold-trap rule (local industry needs the heated suit) the heated suit is ready at
  step 13 and Mercury is a plain hydrolox round trip at step 15: no lock. Two full runs 287/287 (QA-11/QA-14 did not recur).
- **Infinite-resource audit** (`qa/LoopAuditTests`, writes `run-gametest/qa/loops.md`): every recipe (crafting, cooking,
  stonecutting, smithing, all machines incl. fluids) as a graph — 717 resources, 3973 input→output edges, 11 052 closed
  cycles of 2–3 recipes checked. **No free-energy recipe** (no machine output burns in the combustion generator for more
  FE than the recipe and its inputs cost) and **no multiplication cycle** (items and fluids checked separately so units
  match). Guard: the test fails on any new loop. Not covered: code-driven producers (atmosphere processor, pumpjack — finite
  per oil block, RTG decay, solar) and loops longer than three recipes.
- **Oxygen vs travel time**: small tank 5 min, large tank 15 min, two tank slots (three with the extra-oxygen upgrade);
  two small tanks show 10:00 on the HUD. A flight is countdown 10 s + ascent + burn 3 s + transfer 6 s + descent per leg, so
  a whole Moon mission is a few minutes: tanks are ample at every tier.
- QA-15 (suit + rocketry, info): the crew capsule is **not** an oxygen source (`Environment.canBreathe` has only sealed rooms
  as sources), so a player without a sealed suit suffocates while sitting in the capsule in orbit or in transit (seen in the
  playthrough: "OKSİJEN BİTTİ" in the capsule in Earth orbit). If the capsule should be pressurised, register it as an
  `Environment.OxygenSource` for its passengers; otherwise say so in the guide and the launch checklist ("crew in sealed suits").
  **Fixed** on dev/rocketry (the capsule is a pressurised cabin, registered as an oxygen source for seated passengers;
  leaving it still needs a suit or a sealed room). To verify on 1.21.1 with `qa_playthrough` once merged.

## Round 4 (2026-11-07, 1.21.1 @ 8dc7d6f)

- Model re-run: 80/80 technologies, no unobtainable item, all built worlds reached and returnable (Ariel: hydrolox round
  trip at step 15), 42/42 satellite/probe missions, no free-energy or duplication loop (720 resources, 3986 edges). 301/301.
  Only gas-giant orbits (Jupiter 11 631 m/s) stay out of round-trip reach — nothing to land on there; information only.
- New paths: Moon ice-drilling station (cold-trap dungeon: Mk1 frosts but survives short visits, heated suit for long ones)
  and the power monitor add no lock. QA-15 (pressurised capsule) merged.
- QA-16 (**medium**, industry + rocketry): **solar panels ignore night in orbit.** Orbit dimension types have
  `fixed_time: 6000`, so `SolarPanelBlockEntity` (`sunAngle(level)`) always sees noon; `OrbitDaylight.sunlit` is only used by
  the client sky (`OrbitSkyEffects`) and no `Environment.addSunlightModifier` is registered for orbits (only Mars dust
  storms and Venus clouds). Result: the sky goes dark for a third of each 10-minute orbit but panels keep full output, so a
  station never needs batteries and the "energy balance" the night was meant to create does not exist. Fix: register
  `Environment.addSunlightModifier((level, pos) -> Bodies.isOrbit(level) ? OrbitDaylight.sunlit(level, pos) : 1)` (rocketry)
  — the panel already multiplies by `solarFactor(level, pos)`. Then a station drawing 40 FE/t (fuel depot pumping) through
  ~4000 ticks of shadow needs ~160 000 FE of batteries; worth a line in the station guide.
  **Fixed** by industry 7a3da21 (panel, station array and rover roof panel multiply by `OrbitDaylight.sunlit` on the server;
  GameTest `stationArrayCarriesTheDepotThroughShadow`). Rocketry asked not to add the sunlight modifier as well (it would
  apply the shadow twice). To confirm on 1.21.1 after the 05:00 merge.
  **Merge warning:** messages crossed and rocketry also registered `Environment.addSunlightModifier(OrbitDaylight::sunlit)`
  (`RocketryModule.java:230`). With both branches merged the shadow is applied twice (sunlit²: same in full sun/shadow,
  too low in penumbra). One of the two must go — suggested: keep the general modifier, drop the direct multiplication in
  `SolarPanelBlockEntity.java:117` (and in `RoverEntity.java:375` if the rover also uses `solarFactor`). Integrator informed.
  Update 04:25: both sides then removed their change (industry e38b4ff5, rocketry reverted the modifier), so **no** server
  code applies the orbital shadow — QA-16 is open again. Integrator decision: rocketry restores the modifier, industry
  stays as e38b4ff5.

- Model rule updated (inner planets, Moon change 8a61c8e: Mercury cold traps about −213 °C): Mercury's local industry now
  needs the heated suit **or** Mk1 with thermal regulator modules. Mercury stays a plain hydrolox round trip (step 15).
- QA-17 (rocketry, low): `RocketryGameTests.dockingApproach` is flaky on 1.21.1 @ 8dc7d6f — failed 2 of 5 full runs at the
  300-tick timeout with "approach finished" (`RocketryGameTests.java:927`): the rocket was still `docking()` when the test
  ended. The other runs pass. Possibly the approach speed depends on the tick the entity first ticks (spawned in a chunk
  that starts ticking late); a longer timeout or waiting for the first entity tick before `approachPort` would tell.
  **Fixed** on dev/rocketry (forced chunk, approach starts after 10 settled ticks, 600-tick timeout; 5 green runs in a row).

## Round 5 (2026-11-07, 1.21.1 @ 0488108)

- Model re-run: 80/80 technologies, no unobtainable item, all built worlds reached and returnable — new Umbriel, Titania
  and Oberon are hydrolox round trips at step 15; 48/48 satellite/probe missions; no free-energy or duplication loop
  (726 resources, 4011 edges). 317/317 GameTests.
- QA-16 **closed**: the orbital shadow is applied once on the server — only `RocketryModule.java:231`
  (`Environment.addSunlightModifier(OrbitDaylight::sunlit)`); no direct multiplication left in panels or the rover
  (`git grep`); industry's `sunlightModifiersApplyOnce` test guards it. QA-17 merged.

## Round 6 (2026-11-07, 1.21.1 @ 0624890)

- Model re-run after the industry automation, shuttle, Venus probe, lava tube shelter and geyser station merges:
  80/80 technologies, no unobtainable item, every built world reached and returnable, 52/52 satellite/probe missions,
  no free-energy or duplication loop (727 resources, 4023 edges). 329/329 GameTests.
- Regolith heater change (industry 97190e9b: 250 mB O₂ + 20 mB He-3, H₂ only from ice): hydrolox still comes from Earth
  electrolysis (H₂ step 7, liquid H₂ step 9); He-3 at step 13. No route lost.
- Regolith excavator (step 9) digs real blocks (5×5 patch to a fixed depth) — finite, not a free source.
- Landing beacon craftable at step 1. Proteus, Nereid, Nix and Hydra have locations but no world yet (listed as
  "world not built yet").

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

- recipes analysed: 1763, special recipes skipped: 15
- worlds: 31, technologies: 87, model steps: 23
- machines that lock themselves until researched: arc_furnace, atmosphere_processor, chemical_reactor, cryogenic_liquefier, electrolyzer, fabricator, ice_melter, press, pumpjack, refinery, regolith_heater, rtg, sabatier_reactor

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
1. step 5: **electrolysis** — tech disk
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
1. step 5: **electrolysis** — tech disk
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
1. step 5: **electrolysis** — tech disk
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
1. step 5: **electrolysis** — tech disk
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
- step 5: `electrolysis` — tech disk
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
- step 11: `radioisotope_power` — tech disk
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
- step 17: `aerial_mobility` — tech disk
- step 17: `brine_chemistry` — tech disk
- step 17: `deep_space_habitats` — tech disk
- step 17: `fusion_drive` — analysed spacebreaker:ganymede
- step 17: `hydrocarbon_processing` — tech disk
- step 17: `kuiper_belt_navigation` — tech disk
- step 17: `magnetospheric_physics` — tech disk
- step 17: `radiation_hardened_electronics` — tech disk
- step 17: `radiation_hardened_suits` — analysed spacebreaker:europa
- step 17: `suit_mk3` — analysed spacebreaker:ganymede
- step 17: `titan_expedition` — analysed spacebreaker:titan
- step 17: `volcanic_heat_shielding` — analysed spacebreaker:io
- step 18: `icy_moons` — tech disk
- step 18: `nitrogen_glaciology` — tech disk
- step 18: `vertical_exploration` — tech disk
- step 19: `cryobot_drilling` — tech disk
- step 19: `ice_shell_tectonics` — tech disk
- step 19: `plume_sampling` — tech disk
- step 19: `tectonic_mapping` — tech disk
- step 20: `cryovolcanism` — tech disk

### Technologies that cannot be learned

None.

### Worlds

| World | Reached | How | Survival when first reached |
|---|---|---|---|
| minecraft:overworld | yes | step 0: start | 8…22 °C, 101.3 kPa breathable, radiation 0.01; suits: eva; power there yes |
| minecraft:the_nether | yes | step 0: nether portal (vanilla) | — |
| minecraft:the_end | yes | step 0: end portal (vanilla) | — |
| spacebreaker:ariel | yes | step 16: round trip to spacebreaker:ariel/surface with 2-stage [hydrolox_engine, 3× fuel_tank_large / decoupler / hydrolox_vacuum_engine, fuel_tank_large, heat_shield, crew_capsule], 9408 m/s vacuum Δv | -213…-189 °C, 0.0 kPa, radiation 1.50; suits: heated; oxygen tank yes, filler yes, O₂ at home yes, O₂ made there yes; power there yes |
| spacebreaker:callisto | yes | step 16: round trip to spacebreaker:callisto/surface with 2-stage [hydrolox_engine, 3× fuel_tank_large / decoupler / hydrolox_vacuum_engine, 2× fuel_tank_large, heat_shield, crew_capsule], 9996 m/s vacuum Δv | -193…-108 °C, 0.0 kPa, radiation 0.10; suits: heated; oxygen tank yes, filler yes, O₂ at home yes, O₂ made there yes; power there yes |
| spacebreaker:ceres | yes | step 16: round trip to spacebreaker:ceres/surface with 2-stage [hydrolox_engine, 2× fuel_tank_large / decoupler / hydrolox_vacuum_engine, fuel_tank_large, heat_shield, crew_capsule], 8502 m/s vacuum Δv | -143…-38 °C, 0.0 kPa, radiation 1.40; suits: eva/thermal/heated; oxygen tank yes, filler yes, O₂ at home yes, O₂ made there yes; power there yes |
| spacebreaker:charon | yes | step 16: round trip to spacebreaker:charon/surface with 2-stage [hydrolox_engine, 2× fuel_tank_large / decoupler / hydrolox_vacuum_engine, fuel_tank_large, heat_shield, crew_capsule], 8502 m/s vacuum Δv | -230…-220 °C, 0.0 kPa, radiation 1.40; suits: heated; oxygen tank yes, filler yes, O₂ at home yes, O₂ made there yes; power there yes |
| spacebreaker:deimos | yes | step 10: round trip to spacebreaker:deimos/surface with 2-stage [kerosene_engine, 2× fuel_tank_large / decoupler / vacuum_engine, fuel_tank_large, heat_shield, crew_capsule], 6003 m/s vacuum Δv | -110…-40 °C, 0.0 kPa, radiation 1.20; suits: eva; oxygen tank yes, filler yes, O₂ at home yes, O₂ made there NO; power there yes |
| spacebreaker:earth_orbit | yes | step 10: round trip to spacebreaker:earth/orbit with 1-stage [kerosene_engine, fuel_tank_large, heat_shield, crew_capsule], 3274 m/s vacuum Δv | in orbit (vacuum, inside the capsule); oxygen tank yes, filler yes, O₂ at home yes, O₂ made there NO; power there yes |
| spacebreaker:enceladus | yes | step 16: round trip to spacebreaker:enceladus/surface with 2-stage [hydrolox_engine, 3× fuel_tank_large / decoupler / hydrolox_vacuum_engine, 3× fuel_tank_large, heat_shield, crew_capsule], 10292 m/s vacuum Δv | -223…-198 °C, 0.0 kPa, radiation 2.00; suits: heated; oxygen tank yes, filler yes, O₂ at home yes, O₂ made there yes; power there yes |
| spacebreaker:europa | yes | step 16: round trip to spacebreaker:europa/surface with 3-stage [hydrolox_engine, 2× fuel_tank_large / decoupler / hydrolox_vacuum_engine, 2× fuel_tank_large / decoupler / hydrolox_vacuum_engine, fuel_tank_large, heat_shield, crew_capsule], 10479 m/s vacuum Δv | -190…-150 °C, 0.0 kPa, radiation 5400.00; suits: eva/thermal/heated; oxygen tank yes, filler yes, O₂ at home yes, O₂ made there yes; power there yes |
| spacebreaker:ganymede | yes | step 16: round trip to spacebreaker:ganymede/surface with 2-stage [hydrolox_engine, 3× fuel_tank_large / decoupler / hydrolox_vacuum_engine, 3× fuel_tank_large, heat_shield, crew_capsule], 10292 m/s vacuum Δv | -200…-121 °C, 0.0 kPa, radiation 80.00; suits: heated; oxygen tank yes, filler yes, O₂ at home yes, O₂ made there yes; power there yes |
| spacebreaker:io | yes | step 21: flight with refuelling at spacebreaker:io/surface with 2-stage [hydrolox_engine, 3× fuel_tank_large / decoupler / hydrolox_vacuum_engine, fuel_tank_large, heat_shield, crew_capsule], 9408 m/s vacuum Δv | -183…-143 °C, 0.0 kPa, radiation 36000.00; suits: eva/thermal/heated/radiation; oxygen tank yes, filler yes, O₂ at home yes, O₂ made there yes; power there yes |
| spacebreaker:mars | yes | step 10: round trip to spacebreaker:mars/surface with 2-stage [kerosene_engine, 3× fuel_tank_large / decoupler / vacuum_engine, 2× fuel_tank_large, heat_shield, crew_capsule], 7130 m/s vacuum Δv | -90…-10 °C, 0.6 kPa, radiation 0.67; suits: eva; oxygen tank yes, filler yes, O₂ at home yes, O₂ made there NO; power there yes |
| spacebreaker:mars_orbit | yes | step 10: round trip to spacebreaker:mars/orbit with 2-stage [kerosene_engine, fuel_tank_large / decoupler / vacuum_engine, fuel_tank_large, heat_shield, crew_capsule], 5123 m/s vacuum Δv | in orbit (vacuum, inside the capsule); oxygen tank yes, filler yes, O₂ at home yes, O₂ made there NO; power there yes |
| spacebreaker:mercury | yes | step 16: round trip to spacebreaker:mercury/surface with 2-stage [hydrolox_engine, 3× fuel_tank_large / decoupler / hydrolox_vacuum_engine, fuel_tank_large, heat_shield, crew_capsule], 9408 m/s vacuum Δv | -180…430 °C, 0.0 kPa, radiation 2.00; suits: thermal; oxygen tank yes, filler yes, O₂ at home yes, O₂ made there yes; power there yes |
| spacebreaker:mercury_orbit | yes | step 10: round trip to spacebreaker:mercury/orbit with 2-stage [kerosene_engine, 3× fuel_tank_large / decoupler / vacuum_engine, 2× fuel_tank_large, heat_shield, crew_capsule], 7130 m/s vacuum Δv | in orbit (vacuum, inside the capsule); oxygen tank yes, filler yes, O₂ at home yes, O₂ made there NO; power there yes |
| spacebreaker:miranda | yes | step 16: round trip to spacebreaker:miranda/surface with 2-stage [hydrolox_engine, 3× fuel_tank_large / decoupler / hydrolox_vacuum_engine, fuel_tank_large, heat_shield, crew_capsule], 9408 m/s vacuum Δv | -213…-187 °C, 0.0 kPa, radiation 1.50; suits: heated; oxygen tank yes, filler yes, O₂ at home yes, O₂ made there yes; power there yes |
| spacebreaker:moon | yes | step 10: round trip to spacebreaker:moon/surface with 2-stage [kerosene_engine, 3× fuel_tank_large / decoupler / vacuum_engine, fuel_tank_large, heat_shield, crew_capsule], 6625 m/s vacuum Δv | -173…120 °C, 0.0 kPa, radiation 1.37; suits: eva; oxygen tank yes, filler yes, O₂ at home yes, O₂ made there NO; power there yes |
| spacebreaker:moon_orbit | yes | step 10: round trip to spacebreaker:moon/orbit with 1-stage [kerosene_engine, 3× fuel_tank_large, heat_shield, crew_capsule], 5083 m/s vacuum Δv | in orbit (vacuum, inside the capsule); oxygen tank yes, filler yes, O₂ at home yes, O₂ made there NO; power there yes |
| spacebreaker:oberon | yes | step 16: round trip to spacebreaker:oberon/surface with 2-stage [hydrolox_engine, 2× fuel_tank_large / decoupler / hydrolox_vacuum_engine, fuel_tank_large, heat_shield, crew_capsule], 8502 m/s vacuum Δv | -214…-198 °C, 0.0 kPa, radiation 1.50; suits: heated; oxygen tank yes, filler yes, O₂ at home yes, O₂ made there yes; power there yes |
| spacebreaker:orbit | yes | step 10: round trip to spacebreaker:phobos/orbit with 2-stage [kerosene_engine, 2× fuel_tank_large / decoupler / vacuum_engine, fuel_tank_large, heat_shield, crew_capsule], 6003 m/s vacuum Δv | in orbit (vacuum, inside the capsule); oxygen tank yes, filler yes, O₂ at home yes, O₂ made there yes; power there yes |
| spacebreaker:phobos | yes | step 10: round trip to spacebreaker:phobos/surface with 2-stage [kerosene_engine, 2× fuel_tank_large / decoupler / vacuum_engine, fuel_tank_large, heat_shield, crew_capsule], 6003 m/s vacuum Δv | -112…-4 °C, 0.0 kPa, radiation 1.00; suits: eva; oxygen tank yes, filler yes, O₂ at home yes, O₂ made there NO; power there yes |
| spacebreaker:pluto | yes | step 16: round trip to spacebreaker:pluto/surface with 2-stage [hydrolox_engine, 3× fuel_tank_large / decoupler / hydrolox_vacuum_engine, fuel_tank_large, heat_shield, crew_capsule], 9408 m/s vacuum Δv | -233…-223 °C, 0.0 kPa, radiation 1.40; suits: heated; oxygen tank yes, filler yes, O₂ at home yes, O₂ made there yes; power there yes |
| spacebreaker:titan | yes | step 16: round trip to spacebreaker:titan/surface with 2-stage [hydrolox_engine, 2× fuel_tank_large / decoupler / hydrolox_vacuum_engine, fuel_tank_large, heat_shield, crew_capsule], 8502 m/s vacuum Δv | -180…-179 °C, 146.7 kPa, radiation 0.01; suits: eva/thermal/heated; oxygen tank yes, filler yes, O₂ at home yes, O₂ made there yes; power there yes |
| spacebreaker:titania | yes | step 16: round trip to spacebreaker:titania/surface with 2-stage [hydrolox_engine, 2× fuel_tank_large / decoupler / hydrolox_vacuum_engine, fuel_tank_large, heat_shield, crew_capsule], 8502 m/s vacuum Δv | -214…-203 °C, 0.0 kPa, radiation 1.50; suits: heated; oxygen tank yes, filler yes, O₂ at home yes, O₂ made there yes; power there yes |
| spacebreaker:triton | yes | step 16: round trip to spacebreaker:triton/surface with 2-stage [hydrolox_engine, 3× fuel_tank_large / decoupler / hydrolox_vacuum_engine, fuel_tank_large, heat_shield, crew_capsule], 9408 m/s vacuum Δv | -236…-235 °C, 0.0 kPa, radiation 1.50; suits: heated; oxygen tank yes, filler yes, O₂ at home yes, O₂ made there yes; power there yes |
| spacebreaker:umbriel | yes | step 16: round trip to spacebreaker:umbriel/surface with 2-stage [hydrolox_engine, 2× fuel_tank_large / decoupler / hydrolox_vacuum_engine, fuel_tank_large, heat_shield, crew_capsule], 8502 m/s vacuum Δv | -214…-198 °C, 0.0 kPa, radiation 1.50; suits: heated; oxygen tank yes, filler yes, O₂ at home yes, O₂ made there yes; power there yes |
| spacebreaker:venus | yes | step 16: round trip to spacebreaker:venus/surface with 3-stage [hydrolox_engine, fuel_tank_large / decoupler / hydrolox_vacuum_engine, 2× fuel_tank_large / decoupler / hydrolox_vacuum_engine, fuel_tank_large, heat_shield, crew_capsule], 9692 m/s vacuum Δv | 464…464 °C, 9200.0 kPa, radiation 0.00; suits: thermal; oxygen tank yes, filler yes, O₂ at home yes, O₂ made there yes; power there yes |
| spacebreaker:venus_orbit | yes | step 10: round trip to spacebreaker:venus/orbit with 2-stage [kerosene_engine, 2× fuel_tank_large / decoupler / vacuum_engine, fuel_tank_large, heat_shield, crew_capsule], 6003 m/s vacuum Δv | in orbit (vacuum, inside the capsule); oxygen tank yes, filler yes, O₂ at home yes, O₂ made there NO; power there yes |
| spacebreaker:vesta | yes | step 10: round trip to spacebreaker:vesta/surface with 3-stage [kerosene_engine, fuel_tank_large, fuel_tank_medium / decoupler / vacuum_engine, 3× fuel_tank_large / decoupler / vacuum_engine, fuel_tank_large, heat_shield, crew_capsule], 7848 m/s vacuum Δv | -190…-20 °C, 0.0 kPa, radiation 1.40; suits: eva; oxygen tank yes, filler yes, O₂ at home yes, O₂ made there NO; power there yes |

### Items that cannot be obtained

None.

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
| spacebreaker:mercury/orbit | round_trip | 6657 | 2-stage [kerosene_engine, 3× fuel_tank_large / decoupler / vacuum_engine, 2× fuel_tank_large, heat_shield, crew_capsule], 7130 m/s vacuum Δv |
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
| spacebreaker:mercury/surface | round_trip | 9161 | 2-stage [hydrolox_engine, 3× fuel_tank_large / decoupler / hydrolox_vacuum_engine, fuel_tank_large, heat_shield, crew_capsule], 9408 m/s vacuum Δv |
| spacebreaker:venus/surface | round_trip | 9187 | 3-stage [hydrolox_engine, fuel_tank_large / decoupler / hydrolox_vacuum_engine, 2× fuel_tank_large / decoupler / hydrolox_vacuum_engine, fuel_tank_large, heat_shield, crew_capsule], 9692 m/s vacuum Δv |
| spacebreaker:ceres/surface | round_trip | 7980 | 2-stage [hydrolox_engine, 2× fuel_tank_large / decoupler / hydrolox_vacuum_engine, fuel_tank_large, heat_shield, crew_capsule], 8502 m/s vacuum Δv |
| spacebreaker:ceres/orbit | round_trip | 7739 | 2-stage [hydrolox_engine, 2× fuel_tank_large / decoupler / hydrolox_vacuum_engine, fuel_tank_large, heat_shield, crew_capsule], 8502 m/s vacuum Δv |
| spacebreaker:io/orbit | round_trip | 9875 | 2-stage [hydrolox_engine, 3× fuel_tank_large / decoupler / hydrolox_vacuum_engine, 3× fuel_tank_large, heat_shield, crew_capsule], 10292 m/s vacuum Δv |
| spacebreaker:europa/surface | round_trip | 10310 | 3-stage [hydrolox_engine, 2× fuel_tank_large / decoupler / hydrolox_vacuum_engine, 2× fuel_tank_large / decoupler / hydrolox_vacuum_engine, fuel_tank_large, heat_shield, crew_capsule], 10479 m/s vacuum Δv |
| spacebreaker:europa/orbit | round_trip | 9138 | 2-stage [hydrolox_engine, 3× fuel_tank_large / decoupler / hydrolox_vacuum_engine, fuel_tank_large, heat_shield, crew_capsule], 9408 m/s vacuum Δv |
| spacebreaker:ganymede/surface | round_trip | 9924 | 2-stage [hydrolox_engine, 3× fuel_tank_large / decoupler / hydrolox_vacuum_engine, 3× fuel_tank_large, heat_shield, crew_capsule], 10292 m/s vacuum Δv |
| spacebreaker:ganymede/orbit | round_trip | 8327 | 2-stage [hydrolox_engine, 2× fuel_tank_large / decoupler / hydrolox_vacuum_engine, fuel_tank_large, heat_shield, crew_capsule], 8502 m/s vacuum Δv |
| spacebreaker:callisto/surface | round_trip | 9304 | 2-stage [hydrolox_engine, 3× fuel_tank_large / decoupler / hydrolox_vacuum_engine, 2× fuel_tank_large, heat_shield, crew_capsule], 9996 m/s vacuum Δv |
| spacebreaker:callisto/orbit | round_trip | 7872 | 2-stage [hydrolox_engine, 2× fuel_tank_large / decoupler / hydrolox_vacuum_engine, fuel_tank_large, heat_shield, crew_capsule], 8502 m/s vacuum Δv |
| spacebreaker:saturn/orbit | round_trip | 9545 | 2-stage [hydrolox_engine, 3× fuel_tank_large / decoupler / hydrolox_vacuum_engine, 2× fuel_tank_large, heat_shield, crew_capsule], 9996 m/s vacuum Δv |
| spacebreaker:enceladus/surface | round_trip | 9735 | 2-stage [hydrolox_engine, 3× fuel_tank_large / decoupler / hydrolox_vacuum_engine, 3× fuel_tank_large, heat_shield, crew_capsule], 10292 m/s vacuum Δv |
| spacebreaker:enceladus/orbit | round_trip | 9604 | 2-stage [hydrolox_engine, 3× fuel_tank_large / decoupler / hydrolox_vacuum_engine, 2× fuel_tank_large, heat_shield, crew_capsule], 9996 m/s vacuum Δv |
| spacebreaker:titan/surface | round_trip | 7615 | 2-stage [hydrolox_engine, 2× fuel_tank_large / decoupler / hydrolox_vacuum_engine, fuel_tank_large, heat_shield, crew_capsule], 8502 m/s vacuum Δv |
| spacebreaker:uranus/orbit | round_trip | 8313 | 2-stage [hydrolox_engine, 2× fuel_tank_large / decoupler / hydrolox_vacuum_engine, fuel_tank_large, heat_shield, crew_capsule], 8502 m/s vacuum Δv |
| spacebreaker:miranda/surface | round_trip | 8665 | 2-stage [hydrolox_engine, 3× fuel_tank_large / decoupler / hydrolox_vacuum_engine, fuel_tank_large, heat_shield, crew_capsule], 9408 m/s vacuum Δv |
| spacebreaker:miranda/orbit | round_trip | 8560 | 2-stage [hydrolox_engine, 3× fuel_tank_large / decoupler / hydrolox_vacuum_engine, fuel_tank_large, heat_shield, crew_capsule], 9408 m/s vacuum Δv |
| spacebreaker:ariel/surface | round_trip | 8478 | 2-stage [hydrolox_engine, 3× fuel_tank_large / decoupler / hydrolox_vacuum_engine, fuel_tank_large, heat_shield, crew_capsule], 9408 m/s vacuum Δv |
| spacebreaker:ariel/orbit | round_trip | 8173 | 2-stage [hydrolox_engine, 2× fuel_tank_large / decoupler / hydrolox_vacuum_engine, fuel_tank_large, heat_shield, crew_capsule], 8502 m/s vacuum Δv |
| spacebreaker:umbriel/surface | round_trip | 8317 | 2-stage [hydrolox_engine, 2× fuel_tank_large / decoupler / hydrolox_vacuum_engine, fuel_tank_large, heat_shield, crew_capsule], 8502 m/s vacuum Δv |
| spacebreaker:umbriel/orbit | round_trip | 8005 | 2-stage [hydrolox_engine, 2× fuel_tank_large / decoupler / hydrolox_vacuum_engine, fuel_tank_large, heat_shield, crew_capsule], 8502 m/s vacuum Δv |
| spacebreaker:titania/surface | round_trip | 8214 | 2-stage [hydrolox_engine, 2× fuel_tank_large / decoupler / hydrolox_vacuum_engine, fuel_tank_large, heat_shield, crew_capsule], 8502 m/s vacuum Δv |
| spacebreaker:titania/orbit | round_trip | 7772 | 2-stage [hydrolox_engine, 2× fuel_tank_large / decoupler / hydrolox_vacuum_engine, fuel_tank_large, heat_shield, crew_capsule], 8502 m/s vacuum Δv |
| spacebreaker:oberon/surface | round_trip | 8148 | 2-stage [hydrolox_engine, 2× fuel_tank_large / decoupler / hydrolox_vacuum_engine, fuel_tank_large, heat_shield, crew_capsule], 8502 m/s vacuum Δv |
| spacebreaker:oberon/orbit | round_trip | 7718 | 2-stage [hydrolox_engine, 2× fuel_tank_large / decoupler / hydrolox_vacuum_engine, fuel_tank_large, heat_shield, crew_capsule], 8502 m/s vacuum Δv |
| spacebreaker:neptune/orbit | round_trip | 8578 | 2-stage [hydrolox_engine, 3× fuel_tank_large / decoupler / hydrolox_vacuum_engine, fuel_tank_large, heat_shield, crew_capsule], 9408 m/s vacuum Δv |
| spacebreaker:proteus/orbit | round_trip | 8692 | 2-stage [hydrolox_engine, 3× fuel_tank_large / decoupler / hydrolox_vacuum_engine, fuel_tank_large, heat_shield, crew_capsule], 9408 m/s vacuum Δv |
| spacebreaker:triton/surface | round_trip | 8525 | 2-stage [hydrolox_engine, 3× fuel_tank_large / decoupler / hydrolox_vacuum_engine, fuel_tank_large, heat_shield, crew_capsule], 9408 m/s vacuum Δv |
| spacebreaker:triton/orbit | round_trip | 7681 | 2-stage [hydrolox_engine, 2× fuel_tank_large / decoupler / hydrolox_vacuum_engine, fuel_tank_large, heat_shield, crew_capsule], 8502 m/s vacuum Δv |
| spacebreaker:nereid/orbit | round_trip | 7832 | 2-stage [hydrolox_engine, 2× fuel_tank_large / decoupler / hydrolox_vacuum_engine, fuel_tank_large, heat_shield, crew_capsule], 8502 m/s vacuum Δv |
| spacebreaker:pluto/surface | round_trip | 8608 | 2-stage [hydrolox_engine, 3× fuel_tank_large / decoupler / hydrolox_vacuum_engine, fuel_tank_large, heat_shield, crew_capsule], 9408 m/s vacuum Δv |
| spacebreaker:pluto/orbit | round_trip | 7904 | 2-stage [hydrolox_engine, 2× fuel_tank_large / decoupler / hydrolox_vacuum_engine, fuel_tank_large, heat_shield, crew_capsule], 8502 m/s vacuum Δv |
| spacebreaker:charon/surface | round_trip | 8275 | 2-stage [hydrolox_engine, 2× fuel_tank_large / decoupler / hydrolox_vacuum_engine, fuel_tank_large, heat_shield, crew_capsule], 8502 m/s vacuum Δv |
| spacebreaker:charon/orbit | round_trip | 7932 | 2-stage [hydrolox_engine, 2× fuel_tank_large / decoupler / hydrolox_vacuum_engine, fuel_tank_large, heat_shield, crew_capsule], 8502 m/s vacuum Δv |
| spacebreaker:nix/orbit | round_trip | 8146 | 2-stage [hydrolox_engine, 2× fuel_tank_large / decoupler / hydrolox_vacuum_engine, fuel_tank_large, heat_shield, crew_capsule], 8502 m/s vacuum Δv |
| spacebreaker:hydra/orbit | round_trip | 8154 | 2-stage [hydrolox_engine, 2× fuel_tank_large / decoupler / hydrolox_vacuum_engine, fuel_tank_large, heat_shield, crew_capsule], 8502 m/s vacuum Δv |
| spacebreaker:io/surface | refuel_there | 8348 | 2-stage [hydrolox_engine, 3× fuel_tank_large / decoupler / hydrolox_vacuum_engine, fuel_tank_large, heat_shield, crew_capsule], 9408 m/s vacuum Δv |

### Missions the best buildable rocket cannot fly

| Destination | Δv needed for a round trip (gameplay) | Best vacuum Δv of any buildable design | One way: legs (gameplay m/s) |
|---|---|---|---|
| spacebreaker:jupiter/orbit | 11631 | 12222 | spacebreaker:earth/orbit 3158 → spacebreaker:jupiter/orbit 2279 = 5437 |
| spacebreaker:proteus/surface | world not built yet | — | — |
| spacebreaker:nereid/surface | world not built yet | — | — |
| spacebreaker:nix/surface | world not built yet | — | — |
| spacebreaker:hydra/surface | world not built yet | — | — |

### Uncrewed missions (satellites and probes, one way)

- observation_satellite → spacebreaker:ariel/orbit: flown: 1-stage [hydrolox_engine, 2× fuel_tank_large, observation_satellite], 7849 m/s vacuum Δv
- observation_satellite → spacebreaker:callisto/orbit: flown: 1-stage [hydrolox_engine, 2× fuel_tank_large, observation_satellite], 7849 m/s vacuum Δv
- observation_satellite → spacebreaker:ceres/orbit: flown: 1-stage [hydrolox_engine, 2× fuel_tank_large, observation_satellite], 7849 m/s vacuum Δv
- observation_satellite → spacebreaker:charon/orbit: flown: 1-stage [hydrolox_engine, 2× fuel_tank_large, observation_satellite], 7849 m/s vacuum Δv
- observation_satellite → spacebreaker:deimos/orbit: flown: 1-stage [hydrolox_vacuum_engine, fuel_tank_large, observation_satellite], 7211 m/s vacuum Δv
- observation_satellite → spacebreaker:enceladus/orbit: flown: 1-stage [hydrolox_engine, 2× fuel_tank_large, observation_satellite], 7849 m/s vacuum Δv
- observation_satellite → spacebreaker:europa/orbit: flown: 1-stage [hydrolox_engine, 2× fuel_tank_large, observation_satellite], 7849 m/s vacuum Δv
- observation_satellite → spacebreaker:ganymede/orbit: flown: 1-stage [hydrolox_engine, 2× fuel_tank_large, observation_satellite], 7849 m/s vacuum Δv
- observation_satellite → spacebreaker:hydra/orbit: flown: 1-stage [hydrolox_engine, 2× fuel_tank_large, observation_satellite], 7849 m/s vacuum Δv
- observation_satellite → spacebreaker:io/orbit: flown: 1-stage [hydrolox_engine, 2× fuel_tank_large, observation_satellite], 7849 m/s vacuum Δv
- observation_satellite → spacebreaker:jupiter/orbit: flown: 1-stage [hydrolox_engine, fuel_tank_large, heat_shield, observation_satellite], 5901 m/s vacuum Δv
- observation_satellite → spacebreaker:mars/orbit: flown: 1-stage [hydrolox_vacuum_engine, fuel_tank_large, heat_shield, observation_satellite], 6717 m/s vacuum Δv
- observation_satellite → spacebreaker:mercury/orbit: flown: 1-stage [hydrolox_engine, fuel_tank_large, observation_satellite], 6276 m/s vacuum Δv
- observation_satellite → spacebreaker:miranda/orbit: flown: 1-stage [hydrolox_engine, 2× fuel_tank_large, observation_satellite], 7849 m/s vacuum Δv
- observation_satellite → spacebreaker:moon/orbit: flown: 1-stage [hydrolox_vacuum_engine, fuel_tank_large, observation_satellite], 7211 m/s vacuum Δv
- observation_satellite → spacebreaker:neptune/orbit: flown: 1-stage [hydrolox_engine, 2× fuel_tank_large, heat_shield, observation_satellite], 7522 m/s vacuum Δv
- observation_satellite → spacebreaker:nereid/orbit: flown: 1-stage [hydrolox_engine, 2× fuel_tank_large, observation_satellite], 7849 m/s vacuum Δv
- observation_satellite → spacebreaker:nix/orbit: flown: 1-stage [hydrolox_engine, 2× fuel_tank_large, observation_satellite], 7849 m/s vacuum Δv
- observation_satellite → spacebreaker:oberon/orbit: flown: 1-stage [hydrolox_engine, 2× fuel_tank_large, observation_satellite], 7849 m/s vacuum Δv
- observation_satellite → spacebreaker:phobos/orbit: flown: 1-stage [hydrolox_vacuum_engine, fuel_tank_large, observation_satellite], 7211 m/s vacuum Δv
- observation_satellite → spacebreaker:pluto/orbit: flown: 1-stage [hydrolox_engine, 2× fuel_tank_large, observation_satellite], 7849 m/s vacuum Δv
- observation_satellite → spacebreaker:proteus/orbit: flown: 1-stage [hydrolox_engine, 2× fuel_tank_large, observation_satellite], 7849 m/s vacuum Δv
- observation_satellite → spacebreaker:saturn/orbit: flown: 1-stage [hydrolox_engine, fuel_tank_large, heat_shield, observation_satellite], 5901 m/s vacuum Δv
- observation_satellite → spacebreaker:titan/orbit: flown: 1-stage [hydrolox_engine, fuel_tank_large, heat_shield, observation_satellite], 5901 m/s vacuum Δv
- observation_satellite → spacebreaker:titania/orbit: flown: 1-stage [hydrolox_engine, 2× fuel_tank_large, observation_satellite], 7849 m/s vacuum Δv
- observation_satellite → spacebreaker:triton/orbit: flown: 1-stage [hydrolox_engine, 2× fuel_tank_large, observation_satellite], 7849 m/s vacuum Δv
- observation_satellite → spacebreaker:umbriel/orbit: flown: 1-stage [hydrolox_engine, 2× fuel_tank_large, observation_satellite], 7849 m/s vacuum Δv
- observation_satellite → spacebreaker:uranus/orbit: flown: 1-stage [hydrolox_engine, 2× fuel_tank_large, heat_shield, observation_satellite], 7522 m/s vacuum Δv
- observation_satellite → spacebreaker:venus/orbit: flown: 1-stage [hydrolox_vacuum_engine, fuel_tank_large, heat_shield, observation_satellite], 6717 m/s vacuum Δv
- observation_satellite → spacebreaker:vesta/orbit: flown: 1-stage [hydrolox_engine, fuel_tank_large, observation_satellite], 6276 m/s vacuum Δv
- space_probe → spacebreaker:ariel/surface: flown: 1-stage [hydrolox_engine, 2× fuel_tank_large, space_probe], 8214 m/s vacuum Δv
- space_probe → spacebreaker:callisto/surface: flown: 1-stage [hydrolox_engine, 2× fuel_tank_large, space_probe], 8214 m/s vacuum Δv
- space_probe → spacebreaker:ceres/surface: flown: 1-stage [hydrolox_engine, fuel_tank_large, space_probe], 6712 m/s vacuum Δv
- space_probe → spacebreaker:charon/surface: flown: 1-stage [hydrolox_engine, 2× fuel_tank_large, space_probe], 8214 m/s vacuum Δv
- space_probe → spacebreaker:deimos/surface: flown: 1-stage [hydrolox_vacuum_engine, fuel_tank_large, space_probe], 7803 m/s vacuum Δv
- space_probe → spacebreaker:enceladus/surface: flown: 1-stage [hydrolox_engine, 2× fuel_tank_large, space_probe], 8214 m/s vacuum Δv
- space_probe → spacebreaker:europa/surface: flown: 1-stage [hydrolox_engine, 2× fuel_tank_large, space_probe], 8214 m/s vacuum Δv
- space_probe → spacebreaker:ganymede/surface: flown: 1-stage [hydrolox_engine, 2× fuel_tank_large, space_probe], 8214 m/s vacuum Δv
- space_probe → spacebreaker:io/surface: flown: 1-stage [hydrolox_engine, 3× fuel_tank_large, space_probe], 8931 m/s vacuum Δv
- space_probe → spacebreaker:mars/surface: flown: 1-stage [hydrolox_vacuum_engine, fuel_tank_large, space_probe], 7803 m/s vacuum Δv
- space_probe → spacebreaker:mercury/surface: flown: 1-stage [hydrolox_engine, 2× fuel_tank_large, space_probe], 8214 m/s vacuum Δv
- space_probe → spacebreaker:miranda/surface: flown: 1-stage [hydrolox_engine, 2× fuel_tank_large, space_probe], 8214 m/s vacuum Δv
- space_probe → spacebreaker:moon/surface: flown: 1-stage [hydrolox_vacuum_engine, fuel_tank_large, space_probe], 7803 m/s vacuum Δv
- space_probe → spacebreaker:oberon/surface: flown: 1-stage [hydrolox_engine, 2× fuel_tank_large, space_probe], 8214 m/s vacuum Δv
- space_probe → spacebreaker:phobos/surface: flown: 1-stage [hydrolox_vacuum_engine, fuel_tank_large, space_probe], 7803 m/s vacuum Δv
- space_probe → spacebreaker:pluto/surface: flown: 1-stage [hydrolox_engine, 2× fuel_tank_large, space_probe], 8214 m/s vacuum Δv
- space_probe → spacebreaker:titan/surface: flown: 1-stage [hydrolox_vacuum_engine, fuel_tank_large, space_probe], 7803 m/s vacuum Δv
- space_probe → spacebreaker:titania/surface: flown: 1-stage [hydrolox_engine, 2× fuel_tank_large, space_probe], 8214 m/s vacuum Δv
- space_probe → spacebreaker:triton/surface: flown: 1-stage [hydrolox_engine, 2× fuel_tank_large, space_probe], 8214 m/s vacuum Δv
- space_probe → spacebreaker:umbriel/surface: flown: 1-stage [hydrolox_engine, 2× fuel_tank_large, space_probe], 8214 m/s vacuum Δv
- space_probe → spacebreaker:venus/surface: flown: 1-stage [hydrolox_vacuum_engine, fuel_tank_large, space_probe], 7803 m/s vacuum Δv
- space_probe → spacebreaker:vesta/surface: flown: 1-stage [hydrolox_vacuum_engine, fuel_tank_large, space_probe], 7803 m/s vacuum Δv

# Localization audit (`tools/qa/lang_audit.py`)

1664 English and 1664 Turkish keys.

## Errors (0)

None.

## Warnings (2)

- `propellant.spacebreaker.kerolox` is the English text in tr_tr: "RP-1/LOX"
- `sign.spacebreaker.aerostat_valve.0` missing Turkish letters: kapali→kapalı in "GONDOL KAPALI"
