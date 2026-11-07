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

## Round 7 (2026-11-07, 1.21.1 @ 26840ce)

- Re-run after the suit maintenance station, Mars dust-storm warning and landing/ascent-time merges: 80/80
  technologies, no unobtainable item, every built world reached and returnable, 52/52 satellite/probe missions, no
  free-energy or duplication loop (728 resources, 4028 edges). 331/331 GameTests. No open QA finding.

## Round 8: long-session endurance (2026-11-07, 1.21.1 @ 0052d5c)

New GameTest `qa/SoakTests.longSessionStaysFlat` (own batch `qa_soak`, writes `run-gametest/qa/soak.md`): 3000 ticks with
24 rovers, 12 Moon rockets (fuelled; half idle on the ground, half in orbit state), 24 satellites, 16 combustion
generators on coal, 16 crushers fed with ore and 200 FE/t, 16 solar panels, all in force-loaded chunks (without a player or
a forced chunk a level stops ticking entities and block entities after 300 empty ticks — the first attempt measured that).

| Measure | Result |
|---|---|
| Server tick time per 100-tick window | mean 3.5 ms, first 3.5 ms, last 2.8 ms (first window 12.8 ms is the setup); budget 50 ms |
| Heap after GC | 228 MB → 230 MB (+1 MB over 3000 ticks) |
| Mod entities | 60 → 60, all 60 alive, each ticked ~2990 times |
| Work done | crushers 25 ore each (≈120 ticks per recipe), generators burn only while their buffer has room |

No leak, no runaway growth, no duplicated or lost entity. Guards in the test: entities must really tick, no mod entity lost
or multiplied, last-window tick time ≤ max(3 × first, first + 20 ms), heap growth < 512 MB. Not covered: rockets in
flight between dimensions, rovers being driven, and client-side frame time.

## Round 9 (2026-11-07, 1.21.1 @ 58d2412)

- Re-run after the 8 small worlds, sintering and micrometeorite merges: 80/80 technologies, no unobtainable item, every
  built world (31) reached and returnable, 64/64 satellite/probe missions, no free-energy or duplication loop
  (732 resources, 4048 edges; the new `sintering` recipe type is now mapped to the sintering furnace). 343/343 GameTests.
- **Bennu is early-game:** a 1-stage kerosene rocket (5083 m/s vacuum) flies the round trip (4891 m/s gameplay), at
  step 10 — the same step as the first Moon landing, no technology beyond the first Moon rocket.
- **Micrometeorites cannot kill a healthy player:** `MeteorShowers` caps the damage at `MAX_PER_SHOWER` = 6 HP (3 hearts)
  per player and shower, 1 HP per hit (0.5 in a sealed suit), and the damage type has `scaling: never`, so difficulty does
  not raise it. Note (info): a player already below 6 HP can still be finished by a shower; the warning 60 s ahead
  (`WARNING` = 1200 ticks) gives time to take cover.
- Soak test re-run: mean tick 3.7 ms, heap −2 MB, 60/60 entities.

## Round 10 (2026-11-07, 1.21.1 @ 0f76e4fb)

- Re-run after asteroid mining, comets and the low-gravity launch fix: 80/80 technologies, no unobtainable item, every
  built world reached and returnable, 67/67 satellite/probe missions, no free-energy or duplication loop. 349/349.
- **Asteroid fuel from local rubble** (checked by recipe and tag, since the model lets items travel between worlds):
  Bennu and Ryugu rubble is `c:regoliths/carbonaceous` → regolith heater (`carbonaceous_water`) gives water + CO₂ →
  electrolysis H₂ + O₂ → liquefier LOX + LH₂: **hydrolox can be made on site**, methalox too via Sabatier. Eros and Itokawa
  rubble is `c:regoliths/stony` → oxygen (LOX) plus iron/nickel only: the fuel half has to be brought (by design, info).
- **Comets** (Encke, Halley, Churyumov–Gerasimenko; `BodyType.COMET`, no dimension) appear only as orbit locations: crewed
  round trips to their orbits work (Encke 7246 m/s with kerosene, Halley 8381 and 67P 8572 with hydrolox), observation
  satellites reach all three, nothing in the model or the mission table breaks.

## Round 11 (2026-11-07, 1.21.1 @ 335abb16)

- Re-run after the comet worlds: 80/80 technologies, no unobtainable item, every built world reached and returnable,
  70/70 satellite/probe missions, no free-energy or duplication loop. Five full runs in a row green (354, 354, 354, 355,
  355); no flaky test seen, including outer's tholin worm test (no failure in five runs).
- **Comet landings and return**: Encke at step 10 (3-stage kerosene, 7246 m/s), Halley and 67P at step 16 (2-stage
  hydrolox, 8383 / 8573 m/s). Surface climate: Halley −230…−150 °C needs the heated suit; Encke Mk1; 67P any suit.
- **Fuel from comet ice**: `comet_ice`, `encke_ice`, `halley_ice` are mined there and are in `c:ices/water` → ice melter
  → water → electrolysis → LOX + LH₂: hydrolox can be made on every comet.
- **Very low gravity** (Halley ≈ 7.6·10⁻⁴ m/s², Bennu ≈ 8·10⁻⁶): the mission model's TWR and ascent legs behave (huge TWR,
  tiny ascent Δv); nothing divides by zero or blocks a launch. Rocketry's TinyMoonTests cover the in-game ascent.
- **grand_tour is completable**: new GameTest `grandTourIsCompletable` reads the advancement and checks that every
  requirement group (inner, asteroid belt, Jovian, Saturnian, Uranian, Neptunian, Plutonian, near-Earth asteroids, comets)
  has at least one world the model reaches and returns from.
- Soak: mean tick 3.4 ms, heap +3 MB, 60/60 entities.

- Soak test hardened (flake reported by moon: "mod entities 59 → 60" once): the baseline is now what the test spawned,
  counted per entity kind, and only those kinds are compared at the end (other mod creatures may spawn naturally near the
  area; a freshly added entity can be missing from the area query for a tick). The failure message names the kind that
  changed. Two full runs green.

## Launch re-check with rocketry 0453578b (2026-11-07)

`qa_launch` on 1.21.1 + dev/rocketry 0453578b (`docs/screenshots/qa/qa_launch_4_fixed.jpg`):
- QA-23 **fixed**: Earth stays in view through the ascent and after arrival in orbit.
- QA-24 **fixed**: the star-map and dismount hints sit on separate lines above the action bar.
- QA-21: the stop before the switch is 4 ticks (825–828), inside the fade, so not visible.
- QA-22: a deliberate fade: picture brightness 29 → 18 (tick 800) → black from ~815 to 840 → back to full at 860;
  about 1.25 s of full black, 2.5 s in all. The flight HUD is now a slim top-left line.
- The rider stays in the seat through the switch (no unseated ticks). Transit screen 8 ticks (829–836).
- With rocketry 9c0fc018 (`qa_launch_5_no_blackout.jpg`): **no black frame any more**. Brightness 29 → 21 at the
  lowest (a 25 % dip from tick ~800), the last frame held through the switch (ticks ~820–842, ~1.1 s), then a 0.3 s
  cross-fade into the orbit view (back to 29 at tick 854). QA-22 closed.

## Re-check of the 1.0.0 feedback fixes (2026-11-07, branch tips merged locally)

dev/qa + dev/suit 722c4777 + dev/compat a80fda8d + dev/rocketry 224d6856 + dev/industry 89620b70: runData clean,
424/424 green.

- Rover R1/R2 **fixed** (industry 44a83bab, 2aab9735): the panel sits behind both heads; getting out puts the rider on
  the ground 1.5 blocks to the side. `RoverAuditTests` now uses the new panel position.
- QA-25 **fixed** (suit 722c4777): with the small default HUD, Jade and WTHIT show their full tooltip, Spacebreaker's
  dust line included (`docs/screenshots/qa/qa_hud_recheck.jpg`). QA-26 still open: the Jade config key is missing
  (added locally for the shots).
- Realistic setting (rocketry, 4 % tanks): 2/4 goals (orbit and back), 31/87 techs; balanced and easy unchanged.
- Launch (`qa_launch_3_recheck.jpg`, `qa_launch.tsv`): the stop before the switch is now 6 ticks (825–830, was 10);
  the transit screen 7 ticks (831–837, was 10). From about tick 810 to 840 the screen is fully black (no HUD, no
  rocket, no stars) — if this is a deliberate fade it reads as a blackout of ~1.5 s. The Earth disc is no longer drawn
  below during the ascent at all (it was in the 1.0.0 run). On arrival the two action-bar lines still overlap
  (QA-24, tick 860).

## Helmet HUD and block info overlays (2026-11-07, 1.0.0 feedback)

Client scenario `qa_hud_overlay` (`-PwithJade=true` / `-PwithWthit=true`): full EVA suit on the Moon, looking at a
dusty solar panel. Shots: `docs/screenshots/qa/qa_hud_jade.jpg`, `qa_hud_wthit.jpg`.

- QA-25 (suit, high): the helmet visor's dark top band is drawn **over** the Jade and WTHIT tooltips. Their first
  lines (block name, and the Spacebreaker dust line) are hidden; WTHIT's "Energy" line is cut in half. The HUD panels
  themselves (bottom left) do not collide. Fix idea: draw the visor frame before other mods' overlays (a lower GUI
  layer) or leave the top ~10 % of the screen clear.
- QA-26 (compat, medium): Jade asserts `Missing config translation: config.jade.plugin_spacebreaker.info_lines`. In a
  development client (assertions on) this breaks the title screen and the game never reaches a world; in a release
  the Jade settings show the raw key. Needs the key in en_us/tr_tr. (For these shots it was added locally, not
  committed.)

## Launch to orbit, frame by frame (2026-11-07, 1.0.0 feedback)

New client scenario `qa_launch` (`./gradlew runShowcase -Pscenario=qa_launch`): the player sits in the pad rocket,
the countdown runs, and every client tick is logged (screen, dimension, height) to `run-showcase/qa_launch.tsv`, with
a frame every 20 ticks and every 2 ticks around a change. Contact sheets: `docs/screenshots/qa/qa_launch_1_ascent_transit.jpg`,
`qa_launch_2_arrival.jpg`. Findings (rocketry):

- QA-21: for 10 ticks before the switch (ticks 823–832) the rocket stops dead at 1422.6 m: the picture freezes.
- QA-22: the flight HUD disappears about 2 s before the switch (from tick ~800) and the rocket is barely visible; at
  tick 780 it is drawn see-through over the Earth disc.
- The switch itself is a 10-tick (0.5 s) `TransitScreen` ("Coasting to Earth orbit") over a matching Earth view, no
  vanilla loading screen; the player is out of the seat for 2 ticks in it.
- QA-23: about 1.5 s after arrival the Earth disc below vanishes (tick 880 on: stars only) — a second hard cut.
- QA-24: two action-bar lines overlap on arrival ("Press M for the star map" over the dismount hint).

## 1.0.0 check and 1.0.1 notes (2026-11-07, 1.21.1 @ 182a4ef6)

- 1.0.0 merged: runData clean, 420/420 GameTests green.
- **QA-20 fixed** (rocketry e9968379, tank dry mass ~5 %, stronger kerosene engine, balanced Δv scale 0.42): realism
  matrix — BALANCED 4/4 goals, 87/87 techs, grand_tour complete, 40 round-trip worlds, Earth orbit / Moon / Mars at
  step 10 (unchanged); EASY the same; **REALISTIC reaches Earth orbit** (goal 1/4; best rocket 9523 m/s for 9022 m/s).
  It cannot yet fly orbit and back with a capsule, so research stops at 26/87 there; 1.0 accepts orbit as the realistic
  target. New regression test `ProgressionAuditTests.realisticSettingReachesOrbit`.
- Showcase: `ShowcaseDriver.fov(int)` clamps a field of view below 30 (which the game ignores silently) and logs it.
- Open for 1.0.1: names of `dimension.spacebreaker.earth_orbit`, `.moon_orbit`, `.orbit` (rocketry); comet asserts to
  follow rocketry 19c62d87 (Halley ≤ step 21, only by refuelling there).

## 1.0 test matrix (2026-11-07, 1.21.1 @ 7dca25ba + dev/qa 452e75bc)

| # | check | how | result |
|---|---|---|---|
| a | progression, every realism setting | `DensityWhatIfTests.realismMatrix` | BALANCED and EASY: 4/4 goals, 87/87 techs, grand_tour complete, 40 round-trip worlds, Moon/Mars at step 10. **REALISTIC: stuck before orbit** (QA-20: needs 9022 m/s, best rocket 8079 m/s; 0/4 goals, 26/87 techs) |
| b | missing translations | `tools/qa/asset_audit.py` + `tools/qa/lang_audit.py` | 0 lang errors (EN/TR parity, placeholders). Missing names: `entity.spacebreaker.satellite`, `dimension.spacebreaker.earth_orbit`, `.moon_orbit`, `.orbit` (EN and TR) |
| c | missing textures/models | `tools/qa/asset_audit.py` (registry dump from `RegistryDumpTests`) | none: 321 blocks, 515 items, 1273 models, 64 sounds with files and subtitles, 386 code keys |
| d | every item obtainable | progression model | only `co2_frost_layer` (seasonal block that forms on its own, info); no orphan items |
| e | dedicated server, every dimension | `tools/qa/server_smoke.sh` (runServer + RCON) | started in 8.7 s; a chunk generated and force-loaded in all 40 dimensions; no exception; clean stop |
| f | long session | `qa/SoakTests` | mean tick 3.43 ms, heap +2 MB, 60/60 entities |
| g | flaky tests | full suite 3 times | 417/417, 417/417, 417/417: no flaky test |

Also new for 1.0: `ProgressionAuditTests.cometBalanceHolds` (Halley with NTR by step 17, no direct chemical round
trip to Halley, 67P and Tempel 1 chemical, Hale–Bopp sky only) and `OxygenTourTests` (Moon excursions: heeding TURN
BACK always leaves air for the walk home; 2 small tanks give 6–7 min of work at 50–150 blocks).

## Boil-off and insulated tanks (2026-11-07, rocketry 84643490 / 2e79ad08, checked on a local merge)

- Boil-off at the balanced scale 0.2 (84643490): every round-trip step unchanged; only the early one-way kerosene landings
  on Enceladus and Pluto move from step 10 to 16 (their round trips were already at 16). The Hale–Bopp orbit round trip
  no longer works without insulation (long coast); the observation satellite still reaches it.
- Insulated tanks (2e79ad08), offered to the top stage of every design: on the balanced setting no step changes; the
  designs get smaller (Titan 10924 → 8244 m/s, Pluto 3 → 2 stages) and the Hale–Bopp orbit round trip comes back.
- **QA-20 (rocketry, high): the realistic setting stalls at the first goal.** Earth surface → orbit needs 9022 m/s there,
  but the best rocket buildable at that point gives 8079 m/s vacuum Δv (kerosene, up to 3 stages of up to 3 large tanks
  each, the search's limits). With no orbit there is no Moon, no samples, no further research: 0/4 goals, 26/87
  technologies, insulated or not. The search already covers the assembler's whole layout (3 stages, each one engine,
  up to 3 tanks and a radial part; `AssemblerSlots`), only a nose cone on top is not tried. So on the realistic setting
  a new world cannot reach orbit: the realistic ascent cost or the early engines/tanks need a look.
- The rocket search now keeps candidates without their layout (rebuilt on demand): 2.6 million designs with insulated
  tops ran out of heap otherwise.

## Propellant density what-if (2026-11-07, 1.21.1 @ 66b77afa)

Rocketry asked whether scaling the propellant a tank holds by the fuel's density is safe for progression. New
`qa/DensityWhatIfTests` (runs only with `-Dspacebreaker.qa.densityWhatIf=true`) re-runs the whole model per option:

| option | goals | techs | grand_tour | round-trip worlds | Io | others (Mercury … Charon) | upper stage chosen |
|---|---|---|---|---|---|---|---|
| A: no factor (today) | 4/4 | 87/87 | ✅ | 40 | 16 | 16 (Vesta 10) | hydrolox + **NTR** everywhere outside Mars |
| B: realistic (methalox 0.81, hydrolox 0.35, NTR 0.07), today's tanks | 4/4 | 87/87 | ✅ | 40 | 21 | 16 | methalox/kerosene, often refuelling at the target; **NTR never used** |
| C: square root (0.90 / 0.59 / 0.26) | 4/4 | 87/87 | ✅ | 40 | 21 | 16 | methalox + hydrolox vacuum stages; **NTR never used** |
| D: NTR only 0.35 | 4/4 | 87/87 | ✅ | 40 | 21 | 16 | hydrolox; **NTR never used** |

No option breaks a goal, a technology or grand_tour: methalox, kerosene and local refuelling cover every world, and only
Io moves from step 16 to 21 (it then needs fuel made there). The real difference is the NTR's role: with any factor on
liquid hydrogen it is never the best upper stage, so it would only make sense together with lighter, larger LH₂ tanks
(option B with new tanks), which the model can check once they exist.

## Round 14 (2026-11-07, 1.21.1 @ 66b77afa)

- Merged 66b77afa (nuclear thermal engine, Tempel 1 world, uranium enrichment, RTG power in the model): runData clean,
  402/402 GameTests green, 4 goals ✅, 80/80 technologies, no dead end, grand_tour completable. Only "unobtainable" entry:
  `co2_frost_layer` (seasonal block, info).
- **Progression model**: `centrifuging` mapped to the gas centrifuge (it was silently missing: enriched uranium looked
  unobtainable without any test failing); `progressionReportIsWritten` now fails on any recipe type the model cannot map.
  Power: solar panels count only where sunlight ≥ 0.1 of Earth's (up to Ceres); elsewhere an RTG with a fuel pellet
  (outer moons, Pluto system, Halley, 67P at step 12, Io 17). The rocket search drops bottom stages and designs that
  cannot lift off from Earth (exact pruning; fixed the out-of-memory with the 7th engine).
- **Uranium chain**: uraninite 1, uranium dust 4, tungsten rod 5, sintering furnace 7, chemical reactor and gas
  centrifuge 8, sulfuric acid and yellowcake 12 (gated by `acid_resistant_coatings`, a disk from the Mars station),
  enriched uranium and nuclear fuel element 13, own RTG pellets 13 (first pellet from Moon loot at 11).
- **Nuclear thermal engine**: technology at step 11, engine at **step 14** (needs the fuel element). From step 16 every
  outer round trip uses it as the upper stage (2-stage hydrolox + NTR, 8335–12577 m/s). The search never puts it in a
  bottom stage (launch TWR too low), as intended.
- **Tempel 1** is now a world: one-way landing at step 10 (kerosene, 6625 m/s), round trip at step 16 (hydrolox + NTR,
  8335 m/s), probe samples at 16, local ice and dust mined at 17. **Hale–Bopp** is only in the sky now: no location, no
  mission in the model.
- **QA-18 / QA-19 fixed** (rocketry cfbd97bd): two-door-wide airlock stays sealed, a second click cancels the cycle.
  `AirlockAuditTests` now asserts both.
- Eclipses after astro 785cc2f4 (moon orbits in the planet's equator): 14 of 20 moon levels have no eclipse within a
  year; one empty search 4–5 ms, now repeated only every half year (moon 412af00c).

## Round 13 (2026-11-07, 1.21.1 @ b5a48b70)

- Merged b5a48b70: runData clean, 384/384 GameTests green (380 + 4 new QA tests), 80/80 technologies, no dead end, no
  unobtainable item, no loop, grand_tour still completable.
- **New comets Hale–Bopp and Tempel 1** appear as orbit targets. The model flies both: Tempel 1 orbit round trip
  8255 m/s (2-stage hydrolox, step 16), **Hale–Bopp orbit round trip 7555 m/s** (3-stage kerosene) — cheaper than Tempel 1,
  Ceres or 67P. The mission cost is a coplanar estimate; Hale–Bopp's real orbit (i = 89°, e = 0.995, a ≈ 186 AU) would
  cost far more. Noted for the pending design decision; satellites reach both orbits. grand_tour's comet group was
  already satisfied by Encke, so it does not change.
- **Seasonal frost and eclipses vs. tick time** (the soak test runs in the overworld, so they are timed directly by the
  new `qa/WorldEffectCostTests`): one eclipse search costs 0.05–0.3 ms on moons with an eclipse within a year and up
  to ≈ 4 ms on the 5 moons with none (Triton, Nereid, Charon, Nix, Hydra), where it repeats every 100 ticks
  (≈ 0.04 ms/tick on average, one 4 ms step every 5 s while players are there). The coverage itself is cached per tick,
  so solar panels do not pay for it. Mars frost: 1.5 µs per column, 4 columns per player per tick → 0.006 ms per
  player per tick. Soak (overworld): mean 3.41 ms, heap flat, 60/60 entities. Verdict: no tick-time problem.
  Side note for astro: the Uranian moons get a 100 % eclipse every few days at any date; with Uranus' 98° tilt they
  should only happen around its equinoxes (next ≈ 2049). Realism question, not a performance one.
- **Airlock / pressurised room** (new `qa/AirlockAuditTests`, real room flood fill on every tick): with one sliding door
  in each wall the base stays sealed on every tick of a full cycle, by hand and by redstone ✅.
  - **QA-18 (rocketry): a two-door-wide airlock leaks.** Each door only pairs with the door in its own line, so with
    both inner leaves open, asking the left outer leaf to open shuts only the left inner leaf; the right inner leaf stays
    open and the base is open to vacuum (measured: inner false/true, outer true/false → LEAKING). Fix idea: treat
    adjacent sliding doors in the same wall as one door (partners of any leaf = partners of the whole doorway), or
    refuse to open while any door within reach on the other side is open.
  - QA-19 (rocketry, minor): clicking the outer door again during its 2 s cycle is lost: it still opens at the end
    (the base stays sealed, but the player cannot cancel). Same for a redstone signal that goes off during the cycle.

## Round 12 (2026-11-07, 1.21.1 @ c26a79b7)

- Re-run after dusty panels, dust whirls, the sensor, titanium tools and the electric drill: 364/364 GameTests green,
  80/80 technologies, no dead end, no unobtainable item, no free-energy or duplication loop.
- **Mars panel dust does not lock early energy.** Code: full cover keeps 60 % of the output (`MAX_DUST_LOSS = 0.4`, so
  the loss is 40 %, not 60 %); clean → fully covered in about three sols (`DustLoad.FULL_TICKS = 72000`), six times
  faster in a full storm. Cleaning is free: sneak + right-click with an empty hand wipes a panel, a brush wipes one for
  1 durability, a passing dust whirl wipes everything within 2 blocks. Numbers for one 20 FE/t panel at Mars' ~43 %
  sunlight: ≈ 8.6 FE/t at noon clean, ≈ 5.2 FE/t fully dusty, ≈ 1.3 FE/t in a full storm (the storm itself, −85 %, is the
  real dip, not the dust). The atmosphere processor (40 FE/t, the first Mars machine, step 12) needs roughly 9–10
  clean panels over a day, about 16 if never wiped; solar panels are already buildable at step 10 and the model still
  reaches Mars power, CO₂, O₂ and water at steps 10–13. Verdict: a chore with a free fix, not a lock.
- **Titanium tools and the electric drill do not skip progression.** Titanium (step 4, arc furnace + metallurgy): 1100
  uses, speed 7, iron mining level: between iron and diamond, slower than diamond (8). Electric drill (step 4): speed 14
  on everything a pickaxe or shovel mines, 100 FE per block (1000 blocks per 100 kFE charge), unbreakable, but the same
  iron mining level (`INCORRECT_FOR_IRON_TOOL`), so obsidian and ancient debris still need diamond/netherite. Faster
  than an unenchanted diamond pickaxe (8) or netherite (9), slower than Efficiency V diamond (34); it can itself take
  Efficiency (enchantability 16, ≈ 40 with Efficiency V), still below instant-mining stone. Info only: no gate in the
  tech tree depends on mining speed.
- **Dust whirls in the soak test**: 3000 ticks, mean tick 3.61 ms, heap −1 MB, 60/60 spawned entities
  (rocket 12, rover 24, satellite 24); whirls despawn after 60–120 s (`life`) and caused no entity or tick growth.

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

- recipes analysed: 1825, special recipes skipped: 15
- worlds: 43, technologies: 87, model steps: 23
- machines that lock themselves until researched: arc_furnace, atmosphere_processor, chemical_reactor, cryogenic_liquefier, electrolyzer, fabricator, fission_reactor, gas_centrifuge, ice_melter, press, pumpjack, refinery, regolith_excavator, regolith_heater, rtg, sabatier_reactor, sintering_furnace

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

- step 10: spacebreaker:earth_orbit — round trip to spacebreaker:earth/orbit with 1-stage [kerosene_engine, 2× fuel_tank_large, heat_shield, crew_capsule], 4976 m/s vacuum Δv

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

- step 10: spacebreaker:moon — round trip to spacebreaker:moon/surface with 2-stage [kerosene_engine, 3× fuel_tank_large / decoupler / vacuum_engine, 2× fuel_tank_large, heat_shield, crew_capsule], 7819 m/s vacuum Δv

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

- step 10: spacebreaker:mars — round trip to spacebreaker:mars/surface with 3-stage [kerosene_engine, fuel_tank_large / decoupler / vacuum_engine, 3× fuel_tank_large / decoupler / vacuum_engine, 2× fuel_tank_large, heat_shield, crew_capsule], 8571 m/s vacuum Δv

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
- step 17: `volcanic_heat_shielding` — tech disk
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
| spacebreaker:ariel | yes | step 16: round trip to spacebreaker:ariel/surface with 2-stage [hydrolox_engine, fuel_tank_large / decoupler / nuclear_thermal_engine, 3× fuel_tank_large_insulated, heat_shield, crew_capsule], 13276 m/s vacuum Δv | -213…-189 °C, 0.0 kPa, radiation 1.50; suits: heated; oxygen tank yes, filler yes, O₂ at home yes, O₂ made there yes; power there yes |
| spacebreaker:bennu | yes | step 10: round trip to spacebreaker:bennu/surface with 2-stage [kerosene_engine, fuel_tank_large / decoupler / vacuum_engine, 2× fuel_tank_large, heat_shield, crew_capsule], 6522 m/s vacuum Δv | -70…60 °C, 0.0 kPa, radiation 1.40; suits: eva; oxygen tank yes, filler yes, O₂ at home yes, O₂ made there NO; power there yes |
| spacebreaker:callisto | yes | step 16: round trip to spacebreaker:callisto/surface with 2-stage [hydrolox_engine, fuel_tank_large / decoupler / nuclear_thermal_engine, 3× fuel_tank_large, heat_shield, crew_capsule], 13615 m/s vacuum Δv | -193…-108 °C, 0.0 kPa, radiation 0.10; suits: heated; oxygen tank yes, filler yes, O₂ at home yes, O₂ made there yes; power there yes |
| spacebreaker:ceres | yes | step 16: round trip to spacebreaker:ceres/surface with 2-stage [hydrolox_engine, fuel_tank_large / decoupler / nuclear_thermal_engine, 2× fuel_tank_large, heat_shield, crew_capsule], 11624 m/s vacuum Δv | -143…-38 °C, 0.0 kPa, radiation 1.40; suits: eva/thermal/heated; oxygen tank yes, filler yes, O₂ at home yes, O₂ made there yes; power there yes |
| spacebreaker:charon | yes | step 16: round trip to spacebreaker:charon/surface with 2-stage [hydrolox_engine, 2× fuel_tank_large / decoupler / nuclear_thermal_engine, 3× fuel_tank_large_insulated, heat_shield, crew_capsule], 14055 m/s vacuum Δv | -230…-220 °C, 0.0 kPa, radiation 1.40; suits: heated; oxygen tank yes, filler yes, O₂ at home yes, O₂ made there yes; power there yes |
| spacebreaker:churyumov_gerasimenko | yes | step 10: round trip to spacebreaker:churyumov_gerasimenko/surface with 2-stage [kerosene_engine, 3× fuel_tank_large / decoupler / vacuum_engine, 2× fuel_tank_large, heat_shield, crew_capsule], 7819 m/s vacuum Δv | -180…-70 °C, 0.0 kPa, radiation 1.40; suits: eva; oxygen tank yes, filler yes, O₂ at home yes, O₂ made there NO; power there NO |
| spacebreaker:deimos | yes | step 10: round trip to spacebreaker:deimos/surface with 2-stage [kerosene_engine, 2× fuel_tank_large / decoupler / vacuum_engine, 2× fuel_tank_large, heat_shield, crew_capsule], 7248 m/s vacuum Δv | -110…-40 °C, 0.0 kPa, radiation 1.20; suits: eva; oxygen tank yes, filler yes, O₂ at home yes, O₂ made there NO; power there yes |
| spacebreaker:earth_orbit | yes | step 10: round trip to spacebreaker:earth/orbit with 1-stage [kerosene_engine, 2× fuel_tank_large, heat_shield, crew_capsule], 4976 m/s vacuum Δv | in orbit (vacuum, inside the capsule); oxygen tank yes, filler yes, O₂ at home yes, O₂ made there NO; power there yes |
| spacebreaker:enceladus | yes | step 16: round trip to spacebreaker:enceladus/surface with 2-stage [hydrolox_engine, fuel_tank_large / decoupler / nuclear_thermal_engine, 3× fuel_tank_large_insulated, heat_shield, crew_capsule], 13276 m/s vacuum Δv | -223…-198 °C, 0.0 kPa, radiation 2.00; suits: heated; oxygen tank yes, filler yes, O₂ at home yes, O₂ made there NO; power there yes |
| spacebreaker:encke | yes | step 16: round trip to spacebreaker:encke/surface with 2-stage [hydrolox_engine, fuel_tank_large / decoupler / nuclear_thermal_engine, 2× fuel_tank_large, heat_shield, crew_capsule], 11624 m/s vacuum Δv | -150…-20 °C, 0.0 kPa, radiation 1.40; suits: eva/thermal/heated; oxygen tank yes, filler yes, O₂ at home yes, O₂ made there yes; power there yes |
| spacebreaker:eros | yes | step 10: round trip to spacebreaker:eros/surface with 2-stage [kerosene_engine, 2× fuel_tank_large / decoupler / vacuum_engine, 2× fuel_tank_large, heat_shield, crew_capsule], 7248 m/s vacuum Δv | -120…20 °C, 0.0 kPa, radiation 1.40; suits: eva; oxygen tank yes, filler yes, O₂ at home yes, O₂ made there NO; power there yes |
| spacebreaker:europa | yes | step 16: round trip to spacebreaker:europa/surface with 2-stage [hydrolox_engine, fuel_tank_large / decoupler / nuclear_thermal_engine, 3× fuel_tank_large, heat_shield, crew_capsule], 13615 m/s vacuum Δv | -190…-150 °C, 0.0 kPa, radiation 5400.00; suits: eva/thermal/heated; oxygen tank yes, filler yes, O₂ at home yes, O₂ made there NO; power there yes |
| spacebreaker:ganymede | yes | step 16: round trip to spacebreaker:ganymede/surface with 2-stage [hydrolox_engine, fuel_tank_large / decoupler / nuclear_thermal_engine, 3× fuel_tank_large, heat_shield, crew_capsule], 13615 m/s vacuum Δv | -200…-121 °C, 0.0 kPa, radiation 80.00; suits: heated; oxygen tank yes, filler yes, O₂ at home yes, O₂ made there yes; power there yes |
| spacebreaker:halley | yes | step 21: flight with refuelling at spacebreaker:halley/surface with 2-stage [hydrolox_engine, fuel_tank_large / decoupler / nuclear_thermal_engine, 3× fuel_tank_large, heat_shield, crew_capsule], 13615 m/s vacuum Δv | -230…-150 °C, 0.0 kPa, radiation 1.40; suits: heated; oxygen tank yes, filler yes, O₂ at home yes, O₂ made there yes; power there yes |
| spacebreaker:hydra | yes | step 16: round trip to spacebreaker:hydra/surface with 2-stage [hydrolox_engine, fuel_tank_large / decoupler / nuclear_thermal_engine, 3× fuel_tank_large_insulated, heat_shield, crew_capsule], 13276 m/s vacuum Δv | -235…-230 °C, 0.0 kPa, radiation 1.40; suits: heated; oxygen tank yes, filler yes, O₂ at home yes, O₂ made there yes; power there yes |
| spacebreaker:io | yes | step 16: round trip to spacebreaker:io/surface with 2-stage [hydrolox_engine, 3× fuel_tank_large / decoupler / nuclear_thermal_engine, 3× fuel_tank_large, heat_shield, crew_capsule], 15056 m/s vacuum Δv | -183…-143 °C, 0.0 kPa, radiation 36000.00; suits: eva/thermal/heated; oxygen tank yes, filler yes, O₂ at home yes, O₂ made there NO; power there yes |
| spacebreaker:itokawa | yes | step 10: round trip to spacebreaker:itokawa/surface with 2-stage [kerosene_engine, 2× fuel_tank_large / decoupler / vacuum_engine, 2× fuel_tank_large, heat_shield, crew_capsule], 7248 m/s vacuum Δv | -100…30 °C, 0.0 kPa, radiation 1.40; suits: eva; oxygen tank yes, filler yes, O₂ at home yes, O₂ made there NO; power there yes |
| spacebreaker:mars | yes | step 10: round trip to spacebreaker:mars/surface with 3-stage [kerosene_engine, fuel_tank_large / decoupler / vacuum_engine, 3× fuel_tank_large / decoupler / vacuum_engine, 2× fuel_tank_large, heat_shield, crew_capsule], 8571 m/s vacuum Δv | -90…-10 °C, 0.6 kPa, radiation 0.67; suits: eva; oxygen tank yes, filler yes, O₂ at home yes, O₂ made there NO; power there yes |
| spacebreaker:mars_orbit | yes | step 10: round trip to spacebreaker:mars/orbit with 2-stage [kerosene_engine, fuel_tank_large / decoupler / vacuum_engine, 2× fuel_tank_large, heat_shield, crew_capsule], 6522 m/s vacuum Δv | in orbit (vacuum, inside the capsule); oxygen tank yes, filler yes, O₂ at home yes, O₂ made there NO; power there yes |
| spacebreaker:mercury | yes | step 16: round trip to spacebreaker:mercury/surface with 2-stage [hydrolox_engine, fuel_tank_large / decoupler / nuclear_thermal_engine, 2× fuel_tank_large_insulated, heat_shield, crew_capsule], 11402 m/s vacuum Δv | -180…430 °C, 0.0 kPa, radiation 2.00; suits: thermal; oxygen tank yes, filler yes, O₂ at home yes, O₂ made there yes; power there yes |
| spacebreaker:mercury_orbit | yes | step 10: round trip to spacebreaker:mercury/orbit with 2-stage [kerosene_engine, 3× fuel_tank_large / decoupler / vacuum_engine, 3× fuel_tank_large, heat_shield, crew_capsule], 8257 m/s vacuum Δv | in orbit (vacuum, inside the capsule); oxygen tank yes, filler yes, O₂ at home yes, O₂ made there NO; power there yes |
| spacebreaker:miranda | yes | step 16: round trip to spacebreaker:miranda/surface with 2-stage [hydrolox_engine, fuel_tank_large / decoupler / nuclear_thermal_engine, 2× fuel_tank_large_insulated, heat_shield, crew_capsule], 11402 m/s vacuum Δv | -213…-187 °C, 0.0 kPa, radiation 1.50; suits: heated; oxygen tank yes, filler yes, O₂ at home yes, O₂ made there yes; power there yes |
| spacebreaker:moon | yes | step 10: round trip to spacebreaker:moon/surface with 2-stage [kerosene_engine, 3× fuel_tank_large / decoupler / vacuum_engine, 2× fuel_tank_large, heat_shield, crew_capsule], 7819 m/s vacuum Δv | -173…120 °C, 0.0 kPa, radiation 1.37; suits: eva; oxygen tank yes, filler yes, O₂ at home yes, O₂ made there NO; power there yes |
| spacebreaker:moon_orbit | yes | step 10: round trip to spacebreaker:moon/orbit with 2-stage [kerosene_engine, fuel_tank_large / decoupler / vacuum_engine, 2× fuel_tank_large, heat_shield, crew_capsule], 6522 m/s vacuum Δv | in orbit (vacuum, inside the capsule); oxygen tank yes, filler yes, O₂ at home yes, O₂ made there NO; power there yes |
| spacebreaker:nereid | yes | step 16: round trip to spacebreaker:nereid/surface with 2-stage [hydrolox_engine, fuel_tank_large / decoupler / nuclear_thermal_engine, 3× fuel_tank_large_insulated, heat_shield, crew_capsule], 13276 m/s vacuum Δv | -226…-218 °C, 0.0 kPa, radiation 1.40; suits: heated; oxygen tank yes, filler yes, O₂ at home yes, O₂ made there yes; power there yes |
| spacebreaker:nix | yes | step 16: round trip to spacebreaker:nix/surface with 2-stage [hydrolox_engine, fuel_tank_large / decoupler / nuclear_thermal_engine, 3× fuel_tank_large_insulated, heat_shield, crew_capsule], 13276 m/s vacuum Δv | -235…-230 °C, 0.0 kPa, radiation 1.40; suits: heated; oxygen tank yes, filler yes, O₂ at home yes, O₂ made there yes; power there yes |
| spacebreaker:oberon | yes | step 16: round trip to spacebreaker:oberon/surface with 2-stage [hydrolox_engine, fuel_tank_large / decoupler / nuclear_thermal_engine, 2× fuel_tank_large_insulated, heat_shield, crew_capsule], 11402 m/s vacuum Δv | -214…-198 °C, 0.0 kPa, radiation 1.50; suits: heated; oxygen tank yes, filler yes, O₂ at home yes, O₂ made there yes; power there yes |
| spacebreaker:orbit | yes | step 10: round trip to spacebreaker:bennu/orbit with 2-stage [kerosene_engine, fuel_tank_large / decoupler / vacuum_engine, 2× fuel_tank_large, heat_shield, crew_capsule], 6522 m/s vacuum Δv | in orbit (vacuum, inside the capsule); oxygen tank yes, filler yes, O₂ at home yes, O₂ made there yes; power there yes |
| spacebreaker:phobos | yes | step 10: round trip to spacebreaker:phobos/surface with 2-stage [kerosene_engine, 2× fuel_tank_large / decoupler / vacuum_engine, 2× fuel_tank_large, heat_shield, crew_capsule], 7248 m/s vacuum Δv | -112…-4 °C, 0.0 kPa, radiation 1.00; suits: eva; oxygen tank yes, filler yes, O₂ at home yes, O₂ made there NO; power there yes |
| spacebreaker:pluto | yes | step 16: round trip to spacebreaker:pluto/surface with 2-stage [hydrolox_engine, 3× fuel_tank_large / decoupler / nuclear_thermal_engine, 3× fuel_tank_large_insulated, heat_shield, crew_capsule], 14706 m/s vacuum Δv | -233…-223 °C, 0.0 kPa, radiation 1.40; suits: heated; oxygen tank yes, filler yes, O₂ at home yes, O₂ made there yes; power there yes |
| spacebreaker:proteus | yes | step 16: round trip to spacebreaker:proteus/surface with 2-stage [hydrolox_engine, fuel_tank_large / decoupler / nuclear_thermal_engine, 3× fuel_tank_large_insulated, heat_shield, crew_capsule], 13276 m/s vacuum Δv | -226…-218 °C, 0.0 kPa, radiation 1.50; suits: heated; oxygen tank yes, filler yes, O₂ at home yes, O₂ made there yes; power there yes |
| spacebreaker:ryugu | yes | step 10: round trip to spacebreaker:ryugu/surface with 2-stage [kerosene_engine, fuel_tank_large / decoupler / vacuum_engine, 2× fuel_tank_large, heat_shield, crew_capsule], 6522 m/s vacuum Δv | -70…50 °C, 0.0 kPa, radiation 1.40; suits: eva; oxygen tank yes, filler yes, O₂ at home yes, O₂ made there NO; power there yes |
| spacebreaker:tempel_1 | yes | step 10: round trip to spacebreaker:tempel_1/surface with 2-stage [kerosene_engine, 3× fuel_tank_large / decoupler / vacuum_engine, 3× fuel_tank_large, heat_shield, crew_capsule], 8257 m/s vacuum Δv | -170…-60 °C, 0.0 kPa, radiation 1.40; suits: eva; oxygen tank yes, filler yes, O₂ at home yes, O₂ made there NO; power there yes |
| spacebreaker:titan | yes | step 16: round trip to spacebreaker:titan/surface with 2-stage [hydrolox_engine, fuel_tank_large / decoupler / nuclear_thermal_engine, 2× fuel_tank_large, heat_shield, crew_capsule], 11624 m/s vacuum Δv | -180…-179 °C, 146.7 kPa, radiation 0.01; suits: eva/thermal/heated; oxygen tank yes, filler yes, O₂ at home yes, O₂ made there yes; power there yes |
| spacebreaker:titania | yes | step 16: round trip to spacebreaker:titania/surface with 2-stage [hydrolox_engine, fuel_tank_large / decoupler / nuclear_thermal_engine, 2× fuel_tank_large_insulated, heat_shield, crew_capsule], 11402 m/s vacuum Δv | -214…-203 °C, 0.0 kPa, radiation 1.50; suits: heated; oxygen tank yes, filler yes, O₂ at home yes, O₂ made there yes; power there yes |
| spacebreaker:triton | yes | step 16: round trip to spacebreaker:triton/surface with 2-stage [hydrolox_engine, fuel_tank_large / decoupler / nuclear_thermal_engine, 3× fuel_tank_large_insulated, heat_shield, crew_capsule], 13276 m/s vacuum Δv | -236…-235 °C, 0.0 kPa, radiation 1.50; suits: heated; oxygen tank yes, filler yes, O₂ at home yes, O₂ made there yes; power there yes |
| spacebreaker:umbriel | yes | step 16: round trip to spacebreaker:umbriel/surface with 2-stage [hydrolox_engine, fuel_tank_large / decoupler / nuclear_thermal_engine, 3× fuel_tank_large_insulated, heat_shield, crew_capsule], 13276 m/s vacuum Δv | -214…-198 °C, 0.0 kPa, radiation 1.50; suits: heated; oxygen tank yes, filler yes, O₂ at home yes, O₂ made there yes; power there yes |
| spacebreaker:venus | yes | step 16: round trip to spacebreaker:venus/surface with 2-stage [hydrolox_engine, fuel_tank_large / decoupler / nuclear_thermal_engine, 3× fuel_tank_large, heat_shield, crew_capsule], 13615 m/s vacuum Δv | 464…464 °C, 9200.0 kPa, radiation 0.00; suits: thermal; oxygen tank yes, filler yes, O₂ at home yes, O₂ made there yes; power there yes |
| spacebreaker:venus_orbit | yes | step 10: round trip to spacebreaker:venus/orbit with 2-stage [kerosene_engine, 2× fuel_tank_large / decoupler / vacuum_engine, 2× fuel_tank_large, heat_shield, crew_capsule], 7248 m/s vacuum Δv | in orbit (vacuum, inside the capsule); oxygen tank yes, filler yes, O₂ at home yes, O₂ made there NO; power there yes |
| spacebreaker:vesta | yes | step 10: round trip to spacebreaker:vesta/surface with 3-stage [kerosene_engine, 3× fuel_tank_large / decoupler / vacuum_engine, 3× fuel_tank_large / decoupler / vacuum_engine, 3× fuel_tank_large, heat_shield, crew_capsule], 9566 m/s vacuum Δv | -190…-20 °C, 0.0 kPa, radiation 1.40; suits: eva; oxygen tank yes, filler yes, O₂ at home yes, O₂ made there NO; power there yes |

### Items that cannot be obtained

- `co2_frost_layer`: no recipe, loot, drop or world source

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
| spacebreaker:mercury/orbit | round_trip | 8179 | 2-stage [kerosene_engine, 3× fuel_tank_large / decoupler / vacuum_engine, 3× fuel_tank_large, heat_shield, crew_capsule], 8257 m/s vacuum Δv |
| spacebreaker:venus/orbit | round_trip | 6829 | 2-stage [kerosene_engine, 2× fuel_tank_large / decoupler / vacuum_engine, 2× fuel_tank_large, heat_shield, crew_capsule], 7248 m/s vacuum Δv |
| spacebreaker:earth/surface | round_trip | 0 | 1-stage [vacuum_engine, fuel_tank_large, crew_capsule], 4190 m/s vacuum Δv |
| spacebreaker:earth/orbit | round_trip | 3907 | 1-stage [kerosene_engine, 2× fuel_tank_large, heat_shield, crew_capsule], 4976 m/s vacuum Δv |
| spacebreaker:moon/surface | round_trip | 7650 | 2-stage [kerosene_engine, 3× fuel_tank_large / decoupler / vacuum_engine, 2× fuel_tank_large, heat_shield, crew_capsule], 7819 m/s vacuum Δv |
| spacebreaker:moon/orbit | round_trip | 5934 | 2-stage [kerosene_engine, fuel_tank_large / decoupler / vacuum_engine, 2× fuel_tank_large, heat_shield, crew_capsule], 6522 m/s vacuum Δv |
| spacebreaker:bennu/surface | round_trip | 6009 | 2-stage [kerosene_engine, fuel_tank_large / decoupler / vacuum_engine, 2× fuel_tank_large, heat_shield, crew_capsule], 6522 m/s vacuum Δv |
| spacebreaker:bennu/orbit | round_trip | 6009 | 2-stage [kerosene_engine, fuel_tank_large / decoupler / vacuum_engine, 2× fuel_tank_large, heat_shield, crew_capsule], 6522 m/s vacuum Δv |
| spacebreaker:ryugu/surface | round_trip | 6336 | 2-stage [kerosene_engine, fuel_tank_large / decoupler / vacuum_engine, 2× fuel_tank_large, heat_shield, crew_capsule], 6522 m/s vacuum Δv |
| spacebreaker:ryugu/orbit | round_trip | 6336 | 2-stage [kerosene_engine, fuel_tank_large / decoupler / vacuum_engine, 2× fuel_tank_large, heat_shield, crew_capsule], 6522 m/s vacuum Δv |
| spacebreaker:itokawa/surface | round_trip | 6954 | 2-stage [kerosene_engine, 2× fuel_tank_large / decoupler / vacuum_engine, 2× fuel_tank_large, heat_shield, crew_capsule], 7248 m/s vacuum Δv |
| spacebreaker:itokawa/orbit | round_trip | 6954 | 2-stage [kerosene_engine, 2× fuel_tank_large / decoupler / vacuum_engine, 2× fuel_tank_large, heat_shield, crew_capsule], 7248 m/s vacuum Δv |
| spacebreaker:eros/surface | round_trip | 7214 | 2-stage [kerosene_engine, 2× fuel_tank_large / decoupler / vacuum_engine, 2× fuel_tank_large, heat_shield, crew_capsule], 7248 m/s vacuum Δv |
| spacebreaker:eros/orbit | round_trip | 7209 | 2-stage [kerosene_engine, 2× fuel_tank_large / decoupler / vacuum_engine, 2× fuel_tank_large, heat_shield, crew_capsule], 7248 m/s vacuum Δv |
| spacebreaker:mars/surface | round_trip | 8062 | 3-stage [kerosene_engine, fuel_tank_large / decoupler / vacuum_engine, 3× fuel_tank_large / decoupler / vacuum_engine, 2× fuel_tank_large, heat_shield, crew_capsule], 8571 m/s vacuum Δv |
| spacebreaker:mars/orbit | round_trip | 6344 | 2-stage [kerosene_engine, fuel_tank_large / decoupler / vacuum_engine, 2× fuel_tank_large, heat_shield, crew_capsule], 6522 m/s vacuum Δv |
| spacebreaker:phobos/surface | round_trip | 6787 | 2-stage [kerosene_engine, 2× fuel_tank_large / decoupler / vacuum_engine, 2× fuel_tank_large, heat_shield, crew_capsule], 7248 m/s vacuum Δv |
| spacebreaker:phobos/orbit | round_trip | 6781 | 2-stage [kerosene_engine, 2× fuel_tank_large / decoupler / vacuum_engine, 2× fuel_tank_large, heat_shield, crew_capsule], 7248 m/s vacuum Δv |
| spacebreaker:deimos/surface | round_trip | 7018 | 2-stage [kerosene_engine, 2× fuel_tank_large / decoupler / vacuum_engine, 2× fuel_tank_large, heat_shield, crew_capsule], 7248 m/s vacuum Δv |
| spacebreaker:deimos/orbit | round_trip | 7015 | 2-stage [kerosene_engine, 2× fuel_tank_large / decoupler / vacuum_engine, 2× fuel_tank_large, heat_shield, crew_capsule], 7248 m/s vacuum Δv |
| spacebreaker:vesta/surface | round_trip | 9160 | 3-stage [kerosene_engine, 3× fuel_tank_large / decoupler / vacuum_engine, 3× fuel_tank_large / decoupler / vacuum_engine, 3× fuel_tank_large, heat_shield, crew_capsule], 9566 m/s vacuum Δv |
| spacebreaker:vesta/orbit | round_trip | 8971 | 3-stage [kerosene_engine, 3× fuel_tank_large / decoupler / vacuum_engine, 3× fuel_tank_large / decoupler / vacuum_engine, 2× fuel_tank_large, heat_shield, crew_capsule], 9342 m/s vacuum Δv |
| spacebreaker:tempel_1/surface | round_trip | 8099 | 2-stage [kerosene_engine, 3× fuel_tank_large / decoupler / vacuum_engine, 3× fuel_tank_large, heat_shield, crew_capsule], 8257 m/s vacuum Δv |
| spacebreaker:tempel_1/orbit | round_trip | 8098 | 2-stage [kerosene_engine, 3× fuel_tank_large / decoupler / vacuum_engine, 3× fuel_tank_large, heat_shield, crew_capsule], 8257 m/s vacuum Δv |
| spacebreaker:churyumov_gerasimenko/surface | round_trip | 7606 | 2-stage [kerosene_engine, 3× fuel_tank_large / decoupler / vacuum_engine, 2× fuel_tank_large, heat_shield, crew_capsule], 7819 m/s vacuum Δv |
| spacebreaker:churyumov_gerasimenko/orbit | round_trip | 7606 | 2-stage [kerosene_engine, 3× fuel_tank_large / decoupler / vacuum_engine, 2× fuel_tank_large, heat_shield, crew_capsule], 7819 m/s vacuum Δv |
| spacebreaker:titan/orbit | round_trip | 8284 | 3-stage [kerosene_engine, fuel_tank_large / decoupler / vacuum_engine, 3× fuel_tank_large / decoupler / vacuum_engine, 2× fuel_tank_large, heat_shield, crew_capsule], 8571 m/s vacuum Δv |
| spacebreaker:mercury/surface | round_trip | 11255 | 2-stage [hydrolox_engine, fuel_tank_large / decoupler / nuclear_thermal_engine, 2× fuel_tank_large_insulated, heat_shield, crew_capsule], 11402 m/s vacuum Δv |
| spacebreaker:venus/surface | round_trip | 11287 | 2-stage [hydrolox_engine, fuel_tank_large / decoupler / nuclear_thermal_engine, 3× fuel_tank_large, heat_shield, crew_capsule], 13615 m/s vacuum Δv |
| spacebreaker:encke/surface | round_trip | 9550 | 2-stage [hydrolox_engine, fuel_tank_large / decoupler / nuclear_thermal_engine, 2× fuel_tank_large, heat_shield, crew_capsule], 11624 m/s vacuum Δv |
| spacebreaker:encke/orbit | round_trip | 9549 | 2-stage [hydrolox_engine, fuel_tank_large / decoupler / nuclear_thermal_engine, 2× fuel_tank_large, heat_shield, crew_capsule], 11624 m/s vacuum Δv |
| spacebreaker:ceres/surface | round_trip | 9804 | 2-stage [hydrolox_engine, fuel_tank_large / decoupler / nuclear_thermal_engine, 2× fuel_tank_large, heat_shield, crew_capsule], 11624 m/s vacuum Δv |
| spacebreaker:ceres/orbit | round_trip | 9508 | 2-stage [hydrolox_engine, fuel_tank_large / decoupler / nuclear_thermal_engine, 2× fuel_tank_large, heat_shield, crew_capsule], 11624 m/s vacuum Δv |
| spacebreaker:jupiter/orbit | round_trip | 14290 | 3-stage [hydrolox_engine, fuel_tank_large / decoupler / nuclear_thermal_engine, 2× fuel_tank_large / decoupler / nuclear_thermal_engine, 2× fuel_tank_large_insulated, heat_shield, crew_capsule], 14847 m/s vacuum Δv |
| spacebreaker:io/surface | round_trip | 13921 | 2-stage [hydrolox_engine, 3× fuel_tank_large / decoupler / nuclear_thermal_engine, 3× fuel_tank_large, heat_shield, crew_capsule], 15056 m/s vacuum Δv |
| spacebreaker:io/orbit | round_trip | 12133 | 2-stage [hydrolox_engine, fuel_tank_large / decoupler / nuclear_thermal_engine, 3× fuel_tank_large, heat_shield, crew_capsule], 13615 m/s vacuum Δv |
| spacebreaker:europa/surface | round_trip | 12666 | 2-stage [hydrolox_engine, fuel_tank_large / decoupler / nuclear_thermal_engine, 3× fuel_tank_large, heat_shield, crew_capsule], 13615 m/s vacuum Δv |
| spacebreaker:europa/orbit | round_trip | 11226 | 2-stage [hydrolox_engine, fuel_tank_large / decoupler / nuclear_thermal_engine, 2× fuel_tank_large, heat_shield, crew_capsule], 11624 m/s vacuum Δv |
| spacebreaker:ganymede/surface | round_trip | 12192 | 2-stage [hydrolox_engine, fuel_tank_large / decoupler / nuclear_thermal_engine, 3× fuel_tank_large, heat_shield, crew_capsule], 13615 m/s vacuum Δv |
| spacebreaker:ganymede/orbit | round_trip | 10230 | 2-stage [hydrolox_engine, fuel_tank_large / decoupler / nuclear_thermal_engine, 2× fuel_tank_large, heat_shield, crew_capsule], 11624 m/s vacuum Δv |
| spacebreaker:callisto/surface | round_trip | 11430 | 2-stage [hydrolox_engine, fuel_tank_large / decoupler / nuclear_thermal_engine, 3× fuel_tank_large, heat_shield, crew_capsule], 13615 m/s vacuum Δv |
| spacebreaker:callisto/orbit | round_trip | 9672 | 2-stage [hydrolox_engine, fuel_tank_large / decoupler / nuclear_thermal_engine, 2× fuel_tank_large, heat_shield, crew_capsule], 11624 m/s vacuum Δv |
| spacebreaker:saturn/orbit | round_trip | 11726 | 2-stage [hydrolox_engine, fuel_tank_large / decoupler / nuclear_thermal_engine, 3× fuel_tank_large, heat_shield, crew_capsule], 13615 m/s vacuum Δv |
| spacebreaker:enceladus/surface | round_trip | 11960 | 2-stage [hydrolox_engine, fuel_tank_large / decoupler / nuclear_thermal_engine, 3× fuel_tank_large_insulated, heat_shield, crew_capsule], 13276 m/s vacuum Δv |
| spacebreaker:enceladus/orbit | round_trip | 11799 | 2-stage [hydrolox_engine, fuel_tank_large / decoupler / nuclear_thermal_engine, 3× fuel_tank_large_insulated, heat_shield, crew_capsule], 13276 m/s vacuum Δv |
| spacebreaker:titan/surface | round_trip | 9356 | 2-stage [hydrolox_engine, fuel_tank_large / decoupler / nuclear_thermal_engine, 2× fuel_tank_large, heat_shield, crew_capsule], 11624 m/s vacuum Δv |
| spacebreaker:uranus/orbit | round_trip | 10213 | 2-stage [hydrolox_engine, fuel_tank_large / decoupler / nuclear_thermal_engine, 2× fuel_tank_large_insulated, heat_shield, crew_capsule], 11402 m/s vacuum Δv |
| spacebreaker:miranda/surface | round_trip | 10646 | 2-stage [hydrolox_engine, fuel_tank_large / decoupler / nuclear_thermal_engine, 2× fuel_tank_large_insulated, heat_shield, crew_capsule], 11402 m/s vacuum Δv |
| spacebreaker:miranda/orbit | round_trip | 10516 | 2-stage [hydrolox_engine, fuel_tank_large / decoupler / nuclear_thermal_engine, 2× fuel_tank_large_insulated, heat_shield, crew_capsule], 11402 m/s vacuum Δv |
| spacebreaker:ariel/surface | round_trip | 10416 | 2-stage [hydrolox_engine, fuel_tank_large / decoupler / nuclear_thermal_engine, 3× fuel_tank_large_insulated, heat_shield, crew_capsule], 13276 m/s vacuum Δv |
| spacebreaker:ariel/orbit | round_trip | 10041 | 2-stage [hydrolox_engine, fuel_tank_large / decoupler / nuclear_thermal_engine, 2× fuel_tank_large_insulated, heat_shield, crew_capsule], 11402 m/s vacuum Δv |
| spacebreaker:umbriel/surface | round_trip | 10218 | 2-stage [hydrolox_engine, fuel_tank_large / decoupler / nuclear_thermal_engine, 3× fuel_tank_large_insulated, heat_shield, crew_capsule], 13276 m/s vacuum Δv |
| spacebreaker:umbriel/orbit | round_trip | 9835 | 2-stage [hydrolox_engine, fuel_tank_large / decoupler / nuclear_thermal_engine, 2× fuel_tank_large_insulated, heat_shield, crew_capsule], 11402 m/s vacuum Δv |
| spacebreaker:titania/surface | round_trip | 10092 | 2-stage [hydrolox_engine, fuel_tank_large / decoupler / nuclear_thermal_engine, 2× fuel_tank_large_insulated, heat_shield, crew_capsule], 11402 m/s vacuum Δv |
| spacebreaker:titania/orbit | round_trip | 9548 | 2-stage [hydrolox_engine, fuel_tank_large / decoupler / nuclear_thermal_engine, 2× fuel_tank_large_insulated, heat_shield, crew_capsule], 11402 m/s vacuum Δv |
| spacebreaker:oberon/surface | round_trip | 10010 | 2-stage [hydrolox_engine, fuel_tank_large / decoupler / nuclear_thermal_engine, 2× fuel_tank_large_insulated, heat_shield, crew_capsule], 11402 m/s vacuum Δv |
| spacebreaker:oberon/orbit | round_trip | 9482 | 2-stage [hydrolox_engine, fuel_tank_large / decoupler / nuclear_thermal_engine, 2× fuel_tank_large_insulated, heat_shield, crew_capsule], 11402 m/s vacuum Δv |
| spacebreaker:neptune/orbit | round_trip | 10538 | 2-stage [hydrolox_engine, fuel_tank_large / decoupler / nuclear_thermal_engine, 3× fuel_tank_large_insulated, heat_shield, crew_capsule], 13276 m/s vacuum Δv |
| spacebreaker:proteus/surface | round_trip | 10788 | 2-stage [hydrolox_engine, fuel_tank_large / decoupler / nuclear_thermal_engine, 3× fuel_tank_large_insulated, heat_shield, crew_capsule], 13276 m/s vacuum Δv |
| spacebreaker:proteus/orbit | round_trip | 10679 | 2-stage [hydrolox_engine, fuel_tank_large / decoupler / nuclear_thermal_engine, 3× fuel_tank_large_insulated, heat_shield, crew_capsule], 13276 m/s vacuum Δv |
| spacebreaker:triton/surface | round_trip | 10474 | 2-stage [hydrolox_engine, fuel_tank_large / decoupler / nuclear_thermal_engine, 3× fuel_tank_large_insulated, heat_shield, crew_capsule], 13276 m/s vacuum Δv |
| spacebreaker:triton/orbit | round_trip | 9437 | 2-stage [hydrolox_engine, fuel_tank_large / decoupler / nuclear_thermal_engine, 2× fuel_tank_large_insulated, heat_shield, crew_capsule], 11402 m/s vacuum Δv |
| spacebreaker:nereid/surface | round_trip | 9713 | 2-stage [hydrolox_engine, fuel_tank_large / decoupler / nuclear_thermal_engine, 3× fuel_tank_large_insulated, heat_shield, crew_capsule], 13276 m/s vacuum Δv |
| spacebreaker:nereid/orbit | round_trip | 9622 | 2-stage [hydrolox_engine, fuel_tank_large / decoupler / nuclear_thermal_engine, 2× fuel_tank_large_insulated, heat_shield, crew_capsule], 11402 m/s vacuum Δv |
| spacebreaker:pluto/surface | round_trip | 10576 | 2-stage [hydrolox_engine, 3× fuel_tank_large / decoupler / nuclear_thermal_engine, 3× fuel_tank_large_insulated, heat_shield, crew_capsule], 14706 m/s vacuum Δv |
| spacebreaker:pluto/orbit | round_trip | 9711 | 2-stage [hydrolox_engine, fuel_tank_large / decoupler / nuclear_thermal_engine, 3× fuel_tank_large_insulated, heat_shield, crew_capsule], 13276 m/s vacuum Δv |
| spacebreaker:charon/surface | round_trip | 10167 | 2-stage [hydrolox_engine, 2× fuel_tank_large / decoupler / nuclear_thermal_engine, 3× fuel_tank_large_insulated, heat_shield, crew_capsule], 14055 m/s vacuum Δv |
| spacebreaker:charon/orbit | round_trip | 9745 | 2-stage [hydrolox_engine, fuel_tank_large / decoupler / nuclear_thermal_engine, 3× fuel_tank_large_insulated, heat_shield, crew_capsule], 13276 m/s vacuum Δv |
| spacebreaker:nix/surface | round_trip | 10021 | 2-stage [hydrolox_engine, fuel_tank_large / decoupler / nuclear_thermal_engine, 3× fuel_tank_large_insulated, heat_shield, crew_capsule], 13276 m/s vacuum Δv |
| spacebreaker:nix/orbit | round_trip | 10008 | 2-stage [hydrolox_engine, fuel_tank_large / decoupler / nuclear_thermal_engine, 3× fuel_tank_large_insulated, heat_shield, crew_capsule], 13276 m/s vacuum Δv |
| spacebreaker:hydra/surface | round_trip | 10028 | 2-stage [hydrolox_engine, fuel_tank_large / decoupler / nuclear_thermal_engine, 3× fuel_tank_large_insulated, heat_shield, crew_capsule], 13276 m/s vacuum Δv |
| spacebreaker:hydra/orbit | round_trip | 10017 | 2-stage [hydrolox_engine, fuel_tank_large / decoupler / nuclear_thermal_engine, 3× fuel_tank_large_insulated, heat_shield, crew_capsule], 13276 m/s vacuum Δv |
| spacebreaker:halley/surface | refuel_there | 12093 | 2-stage [hydrolox_engine, fuel_tank_large / decoupler / nuclear_thermal_engine, 3× fuel_tank_large, heat_shield, crew_capsule], 13615 m/s vacuum Δv |

### Missions the best buildable rocket cannot fly

| Destination | Δv needed for a round trip (gameplay) | Best vacuum Δv of any buildable design | One way: legs (gameplay m/s) |
|---|---|---|---|
| spacebreaker:halley/orbit | 16714 | 18320 | spacebreaker:earth/orbit 3880 → spacebreaker:halley/orbit 8213 = 12093 |
| spacebreaker:hale_bopp/orbit | no route | — | — |

### Uncrewed missions (satellites and probes, one way)

- observation_satellite → spacebreaker:ariel/orbit: flown: 1-stage [hydrolox_engine, 2× fuel_tank_large, observation_satellite], 8893 m/s vacuum Δv
- observation_satellite → spacebreaker:bennu/orbit: flown: 1-stage [nuclear_thermal_engine, fuel_tank_large, observation_satellite], 8428 m/s vacuum Δv
- observation_satellite → spacebreaker:callisto/orbit: flown: 1-stage [hydrolox_engine, 2× fuel_tank_large, observation_satellite], 8893 m/s vacuum Δv
- observation_satellite → spacebreaker:ceres/orbit: flown: 1-stage [hydrolox_engine, 2× fuel_tank_large, observation_satellite], 8893 m/s vacuum Δv
- observation_satellite → spacebreaker:charon/orbit: flown: 1-stage [hydrolox_engine, 2× fuel_tank_large, observation_satellite], 8893 m/s vacuum Δv
- observation_satellite → spacebreaker:churyumov_gerasimenko/orbit: flown: 1-stage [hydrolox_engine, 2× fuel_tank_large, observation_satellite], 8893 m/s vacuum Δv
- observation_satellite → spacebreaker:deimos/orbit: flown: 1-stage [nuclear_thermal_engine, fuel_tank_large, observation_satellite], 8428 m/s vacuum Δv
- observation_satellite → spacebreaker:enceladus/orbit: flown: 1-stage [hydrolox_engine, 3× fuel_tank_large, observation_satellite], 10004 m/s vacuum Δv
- observation_satellite → spacebreaker:encke/orbit: flown: 1-stage [hydrolox_engine, 2× fuel_tank_large, observation_satellite], 8893 m/s vacuum Δv
- observation_satellite → spacebreaker:eros/orbit: flown: 1-stage [nuclear_thermal_engine, fuel_tank_large, observation_satellite], 8428 m/s vacuum Δv
- observation_satellite → spacebreaker:europa/orbit: flown: 1-stage [hydrolox_engine, 3× fuel_tank_large, observation_satellite], 10004 m/s vacuum Δv
- observation_satellite → spacebreaker:ganymede/orbit: flown: 1-stage [hydrolox_engine, 2× fuel_tank_large, observation_satellite], 8893 m/s vacuum Δv
- observation_satellite → spacebreaker:hale_bopp/orbit: NOT POSSIBLE: no_route (leg 1); path spacebreaker:earth/orbit (shield 0) → spacebreaker:hale_bopp/orbit (shield 0)
- observation_satellite → spacebreaker:halley/orbit: flown: 2-stage [hydrolox_engine, fuel_tank_large / decoupler / nuclear_thermal_engine, 2× fuel_tank_large, observation_satellite], 13541 m/s vacuum Δv
- observation_satellite → spacebreaker:hydra/orbit: flown: 1-stage [hydrolox_engine, 3× fuel_tank_large, observation_satellite], 10004 m/s vacuum Δv
- observation_satellite → spacebreaker:io/orbit: flown: 1-stage [hydrolox_engine, 3× fuel_tank_large, observation_satellite], 10004 m/s vacuum Δv
- observation_satellite → spacebreaker:itokawa/orbit: flown: 1-stage [nuclear_thermal_engine, fuel_tank_large, observation_satellite], 8428 m/s vacuum Δv
- observation_satellite → spacebreaker:jupiter/orbit: flown: 1-stage [hydrolox_engine, 2× fuel_tank_large, heat_shield, observation_satellite], 8449 m/s vacuum Δv
- observation_satellite → spacebreaker:mars/orbit: flown: 1-stage [nuclear_thermal_engine, fuel_tank_large, heat_shield, observation_satellite], 8100 m/s vacuum Δv
- observation_satellite → spacebreaker:mercury/orbit: flown: 1-stage [hydrolox_engine, 2× fuel_tank_large, observation_satellite], 8893 m/s vacuum Δv
- observation_satellite → spacebreaker:miranda/orbit: flown: 1-stage [hydrolox_engine, 3× fuel_tank_large, observation_satellite], 10004 m/s vacuum Δv
- observation_satellite → spacebreaker:moon/orbit: flown: 1-stage [nuclear_thermal_engine, fuel_tank_large, observation_satellite], 8428 m/s vacuum Δv
- observation_satellite → spacebreaker:neptune/orbit: flown: 1-stage [hydrolox_engine, 2× fuel_tank_large, heat_shield, observation_satellite], 8449 m/s vacuum Δv
- observation_satellite → spacebreaker:nereid/orbit: flown: 1-stage [hydrolox_engine, 2× fuel_tank_large, observation_satellite], 8893 m/s vacuum Δv
- observation_satellite → spacebreaker:nix/orbit: flown: 1-stage [hydrolox_engine, 3× fuel_tank_large, observation_satellite], 10004 m/s vacuum Δv
- observation_satellite → spacebreaker:oberon/orbit: flown: 1-stage [hydrolox_engine, 2× fuel_tank_large, observation_satellite], 8893 m/s vacuum Δv
- observation_satellite → spacebreaker:phobos/orbit: flown: 1-stage [nuclear_thermal_engine, fuel_tank_large, observation_satellite], 8428 m/s vacuum Δv
- observation_satellite → spacebreaker:pluto/orbit: flown: 1-stage [hydrolox_engine, 2× fuel_tank_large, observation_satellite], 8893 m/s vacuum Δv
- observation_satellite → spacebreaker:proteus/orbit: flown: 1-stage [hydrolox_engine, 3× fuel_tank_large, observation_satellite], 10004 m/s vacuum Δv
- observation_satellite → spacebreaker:ryugu/orbit: flown: 1-stage [nuclear_thermal_engine, fuel_tank_large, observation_satellite], 8428 m/s vacuum Δv
- observation_satellite → spacebreaker:saturn/orbit: flown: 1-stage [hydrolox_engine, 2× fuel_tank_large, heat_shield, observation_satellite], 8449 m/s vacuum Δv
- observation_satellite → spacebreaker:tempel_1/orbit: flown: 1-stage [hydrolox_engine, 2× fuel_tank_large, observation_satellite], 8893 m/s vacuum Δv
- observation_satellite → spacebreaker:titan/orbit: flown: 1-stage [hydrolox_engine, 2× fuel_tank_large, heat_shield, observation_satellite], 8449 m/s vacuum Δv
- observation_satellite → spacebreaker:titania/orbit: flown: 1-stage [hydrolox_engine, 2× fuel_tank_large, observation_satellite], 8893 m/s vacuum Δv
- observation_satellite → spacebreaker:triton/orbit: flown: 1-stage [hydrolox_engine, 2× fuel_tank_large, observation_satellite], 8893 m/s vacuum Δv
- observation_satellite → spacebreaker:umbriel/orbit: flown: 1-stage [hydrolox_engine, 2× fuel_tank_large, observation_satellite], 8893 m/s vacuum Δv
- observation_satellite → spacebreaker:uranus/orbit: flown: 1-stage [hydrolox_engine, 2× fuel_tank_large, heat_shield, observation_satellite], 8449 m/s vacuum Δv
- observation_satellite → spacebreaker:venus/orbit: flown: 1-stage [nuclear_thermal_engine, fuel_tank_large, heat_shield, observation_satellite], 8100 m/s vacuum Δv
- observation_satellite → spacebreaker:vesta/orbit: flown: 1-stage [hydrolox_engine, 2× fuel_tank_large, observation_satellite], 8893 m/s vacuum Δv
- space_probe → spacebreaker:ariel/surface: flown: 1-stage [hydrolox_engine, 2× fuel_tank_large, space_probe], 9404 m/s vacuum Δv
- space_probe → spacebreaker:bennu/surface: flown: 1-stage [nuclear_thermal_engine, fuel_tank_large, space_probe], 8786 m/s vacuum Δv
- space_probe → spacebreaker:callisto/surface: flown: 1-stage [hydrolox_engine, 2× fuel_tank_large, space_probe], 9404 m/s vacuum Δv
- space_probe → spacebreaker:ceres/surface: flown: 1-stage [hydrolox_engine, 2× fuel_tank_large, space_probe], 9404 m/s vacuum Δv
- space_probe → spacebreaker:charon/surface: flown: 2-stage [hydrolox_engine, fuel_tank_large / decoupler / hydrolox_vacuum_engine, fuel_tank_large_insulated, space_probe], 10912 m/s vacuum Δv
- space_probe → spacebreaker:churyumov_gerasimenko/surface: flown: 1-stage [hydrolox_vacuum_engine, fuel_tank_large, space_probe], 8779 m/s vacuum Δv
- space_probe → spacebreaker:deimos/surface: flown: 1-stage [nuclear_thermal_engine, fuel_tank_large, space_probe], 8786 m/s vacuum Δv
- space_probe → spacebreaker:enceladus/surface: flown: 1-stage [hydrolox_engine, 3× fuel_tank_large, space_probe], 10467 m/s vacuum Δv
- space_probe → spacebreaker:encke/surface: flown: 1-stage [hydrolox_engine, 2× fuel_tank_large, space_probe], 9404 m/s vacuum Δv
- space_probe → spacebreaker:eros/surface: flown: 1-stage [nuclear_thermal_engine, fuel_tank_large, space_probe], 8786 m/s vacuum Δv
- space_probe → spacebreaker:europa/surface: flown: 1-stage [hydrolox_engine, 3× fuel_tank_large, space_probe], 10467 m/s vacuum Δv
- space_probe → spacebreaker:ganymede/surface: flown: 1-stage [hydrolox_engine, 3× fuel_tank_large, space_probe], 10467 m/s vacuum Δv
- space_probe → spacebreaker:halley/surface: flown: 2-stage [hydrolox_engine, fuel_tank_large / decoupler / nuclear_thermal_engine, 2× fuel_tank_large, space_probe], 13962 m/s vacuum Δv
- space_probe → spacebreaker:hydra/surface: flown: 1-stage [hydrolox_engine, 2× fuel_tank_large, space_probe], 9404 m/s vacuum Δv
- space_probe → spacebreaker:io/surface: flown: 2-stage [hydrolox_engine, fuel_tank_large / decoupler / hydrolox_vacuum_engine, fuel_tank_large, space_probe], 11268 m/s vacuum Δv
- space_probe → spacebreaker:itokawa/surface: flown: 1-stage [nuclear_thermal_engine, fuel_tank_large, space_probe], 8786 m/s vacuum Δv
- space_probe → spacebreaker:mars/surface: flown: 1-stage [nuclear_thermal_engine, fuel_tank_large, space_probe], 8786 m/s vacuum Δv
- space_probe → spacebreaker:mercury/surface: flown: 1-stage [hydrolox_engine, 3× fuel_tank_large, space_probe], 10467 m/s vacuum Δv
- space_probe → spacebreaker:miranda/surface: flown: 1-stage [hydrolox_engine, 2× fuel_tank_large, space_probe], 9404 m/s vacuum Δv
- space_probe → spacebreaker:moon/surface: flown: 1-stage [nuclear_thermal_engine, fuel_tank_large, space_probe], 8786 m/s vacuum Δv
- space_probe → spacebreaker:nereid/surface: flown: 1-stage [hydrolox_engine, 2× fuel_tank_large, space_probe], 9404 m/s vacuum Δv
- space_probe → spacebreaker:nix/surface: flown: 1-stage [hydrolox_engine, 2× fuel_tank_large, space_probe], 9404 m/s vacuum Δv
- space_probe → spacebreaker:oberon/surface: flown: 1-stage [hydrolox_engine, 2× fuel_tank_large, space_probe], 9404 m/s vacuum Δv
- space_probe → spacebreaker:phobos/surface: flown: 1-stage [nuclear_thermal_engine, fuel_tank_large, space_probe], 8786 m/s vacuum Δv
- space_probe → spacebreaker:pluto/surface: flown: 2-stage [hydrolox_engine, fuel_tank_large / decoupler / hydrolox_vacuum_engine, fuel_tank_large_insulated, space_probe], 10912 m/s vacuum Δv
- space_probe → spacebreaker:proteus/surface: flown: 1-stage [hydrolox_engine, 3× fuel_tank_large, space_probe], 10467 m/s vacuum Δv
- space_probe → spacebreaker:ryugu/surface: flown: 1-stage [nuclear_thermal_engine, fuel_tank_large, space_probe], 8786 m/s vacuum Δv
- space_probe → spacebreaker:tempel_1/surface: flown: 1-stage [hydrolox_engine, fuel_tank_large, space_probe], 7396 m/s vacuum Δv
- space_probe → spacebreaker:titan/surface: flown: 1-stage [nuclear_thermal_engine, fuel_tank_large, space_probe], 8786 m/s vacuum Δv
- space_probe → spacebreaker:titania/surface: flown: 1-stage [hydrolox_engine, 2× fuel_tank_large, space_probe], 9404 m/s vacuum Δv
- space_probe → spacebreaker:triton/surface: flown: 2-stage [hydrolox_engine, fuel_tank_large / decoupler / hydrolox_vacuum_engine, fuel_tank_large_insulated, space_probe], 10912 m/s vacuum Δv
- space_probe → spacebreaker:umbriel/surface: flown: 1-stage [hydrolox_engine, 2× fuel_tank_large, space_probe], 9404 m/s vacuum Δv
- space_probe → spacebreaker:venus/surface: flown: 1-stage [nuclear_thermal_engine, fuel_tank_large, space_probe], 8786 m/s vacuum Δv
- space_probe → spacebreaker:vesta/surface: flown: 1-stage [hydrolox_engine, 2× fuel_tank_large, space_probe], 9404 m/s vacuum Δv

# Localization audit (`tools/qa/lang_audit.py`)

2013 English and 2013 Turkish keys.

## Errors (0)

None.

## Warnings (6)

- `body.spacebreaker.encke` is the English text in tr_tr: "2P/Encke"
- `body.spacebreaker.hale_bopp` is the English text in tr_tr: "C/1995 O1 (Hale–Bopp)"
- `body.spacebreaker.halley` is the English text in tr_tr: "1P/Halley"
- `body.spacebreaker.tempel_1` is the English text in tr_tr: "9P/Tempel 1"
- `propellant.spacebreaker.kerolox` is the English text in tr_tr: "RP-1/LOX"
- `sign.spacebreaker.aerostat_valve.0` missing Turkish letters: kapali→kapalı in "GONDOL KAPALI"
