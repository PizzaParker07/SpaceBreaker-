# Spacebreaker — Astrophysics library (`core/astro`)

Pure-Java physics for every other area: orbits, transfers, rocket maths, stars, planets and the
procedural galaxy. No Minecraft classes except `BodyAdapter`, the bridge to the `celestial_body`
data-pack registry. **SI units everywhere** (m, kg, s, m/s, rad, K) unless a name says otherwise;
stellar values use solar units (M☉, L☉, R☉). Everything is covered by JUnit tests (`./gradlew test`)
against known values, and GameTests check that the data loads.

## Quick start

```java
// the Solar System as pure-Java bodies (from the celestial_body registry)
BodySystem sys = BodyAdapter.system(level.registryAccess()).orElseThrow();
AstroBody earth = sys.require("spacebreaker:earth");

double vEsc   = earth.escapeVelocity();                       // 11 186 m/s
double ascent = TransferPlanner.ascent(earth, 1.3);           // ~9.2 km/s to low orbit, losses included
TransferPlanner.Plan toMars = TransferPlanner.plan(sys, "spacebreaker:earth", "spacebreaker:mars", true);
toMars.deltaV("departure");   // 3.6 km/s trans-Mars injection from low Earth orbit
toMars.deltaV("capture");     // with aerobraking: only a small periapsis raise
toMars.transferTime();        // ~259 days (s)
double t = BodyAdapter.simTime(level);                       // simulated s since J2000 (server time scale)
double wait = TransferPlanner.timeToWindow(sys, "spacebreaker:earth", "spacebreaker:mars", t);

// rockets
List<RocketMath.Stage> stack = List.of(new RocketMath.Stage(dry1, prop1, thrust1, isp1),
                                       new RocketMath.Stage(dry2, prop2, thrust2, isp2));
RocketMath.StageResult[] r = RocketMath.stack(stack, payloadKg, earth.surfaceGravity());
double dv = RocketMath.totalDeltaV(stack, payloadKg);
```

## API overview

| Class | What it does |
|---|---|
| `AstroConstants` | G, AU, light year, parsec, day/year, g₀, solar and Earth constants, σ, k_B, amu |
| `Vec3`, `StateVector` | double-precision vectors; position + velocity |
| `OrbitalElements` | a, e, i, Ω, ω, M₀ around a central μ; period, mean motion, apsides, `stateAt(t)` |
| `Kepler` | Kepler's equation (elliptic and hyperbolic), anomaly conversions, state vectors ⇄ elements, vis-viva, circular/escape speed, period ⇄ semi-major axis, sphere of influence, Hill radius |
| `Transfers` | Hohmann, bi-elliptic, patched-conic departure, capture (circular, elliptic, aerobraking), escape, synodic period, Hohmann phase angle, time to a phase angle |
| `RocketMath` | Tsiolkovsky Δv, mass ratio, propellant for a Δv, mass flow, burn time, TWR, combined Isp, Isp vs ambient pressure, multi-stage stack results (Δv, masses, burn time, TWR per stage) |
| `TransferPlanner` | mission-level numbers between bodies: surface ⇄ low orbit (gravity and drag losses, surface rotation, parachutes), low orbit → escape, low orbit → low orbit for siblings, planet ⇄ moon and nested cases (Earth → Phobos), launch windows from real positions |
| `AstroBody`, `BodySystem` | pure body record (SI) with μ, g, v_esc, density, mean molar mass, scale height, default low orbit; a body tree with ancestry, positions and velocities over time |
| `GameClock` | game ticks ⇄ simulated seconds since J2000 using the server's `orbitalTimeScale` (real seconds per simulated hour) |
| `StellarModel` | main-sequence mass → luminosity, radius, temperature, lifetime, spectral class, black-body colour; catalogue stars |
| `PlanetModel` | habitable zone, flux, equilibrium/day-side/greenhouse temperature, gravity, escape velocity, thermal speed and Jeans retention, exosphere temperature, tidal-locking time, composition classes, mass–radius relations |
| `DeltaVMap` | Markdown Δv tables for a system (the map below) |
| `BodyAdapter` | `CelestialBody` ⇄ `AstroBody`, whole registry as a `BodySystem`, generated `StarSystem` → `CelestialBody` records |
| `galaxy.Galaxy` | seed-based Milky Way: lazily generated 10 pc sectors, star lookup by address, `starsWithin`, `system(star)` (cached) |
| `galaxy.GalaxyModel` | stellar density: thin + thick disk with four spiral arms, bar, halo; 0.1 stars/pc³ at the Sun |
| `galaxy.StarSystemGenerator` | Kroupa IMF, stellar remnants, planets (snow line, spacing, giants), moons inside a third of the Hill sphere, atmospheres, tidal locking |
| `galaxy.NearbyStars` | real stars as fixed entries: Sun, Alpha Centauri A/B, Proxima Centauri (+ b), Barnard's Star (+ b), Sirius A/B, Epsilon Eridani (+ b), Tau Ceti, TRAPPIST-1 (+ b–h); unknown planets are filled in by the generator |
| `galaxy.Rng` | SplitMix64: identical numbers on every machine for the same world seed |

### Conventions
- **Time** `t` is simulated seconds since the J2000 epoch (1 Jan 2000, 12:00 TT). At t = 0 the planets
  are where they really were. Moons other than the Moon use orbit angles of 0 (their phase is not real).
- **Frames:** planets use the J2000 ecliptic; moons use their parent's equator. `BodySystem.position`
  adds the chain of parent-relative positions.
- **Δv figures** treat orbits as circular and coplanar (the usual Δv-map simplification): no plane
  changes, no gravity assists. Low orbits: 10 scale heights + 2 % of the radius above the surface for
  bodies with air, 5 % of the radius for airless ones.
- **Gravity loss** ≈ 0.25 · v_circ / TWR, **drag loss** ≈ 130 m/s · √(p/p⊕) · √(H/H⊕). Good to ~10 %
  for Earth, the Moon and Mars; meant for gameplay, not mission design.
- Gas and ice giants are not landable; their "surface" is the 1-bar level.

### For other areas
- **Rocketry:** `RocketMath` for the assembly-table numbers (Δv per stage, TWR, burn time);
  `TransferPlanner` for "can this rocket get there"; `timeToWindow` for the launch-window display.
- **Planet engine:** `PlanetModel` for generated worlds; `Galaxy` + `BodyAdapter.toCelestialBodies`
  give data-format bodies for runtime dimensions (pass a function that returns the dimension you create).
- **Sky rendering / star map:** `BodySystem.position` and `GameClock`; `CelestialBody.rings`;
  `StellarModel.blackbodyColor` for star colours; `NearbyStars.Entry.nameEn/nameTr` for labels.
- **Suit & survival:** body data already has temperature, pressure, radiation; `PlanetModel` gives the
  same for generated planets via `StarSystem.Details`.

## Data (`data/spacebreaker/spacebreaker/celestial_body/*.json`)

Real NASA/JPL values (planetary fact sheets; J2000 mean elements for the planets). New optional
fields, all with defaults: `albedo` (Bond, default 0.3), `rings` (`inner_radius_km`,
`outer_radius_km`), and orbit angles `longitude_ascending_node_deg`, `argument_periapsis_deg`,
`mean_anomaly_deg` (J2000, default 0).

Landable bodies use the dimension `spacebreaker:<name>` (Earth is the overworld): Mercury, Venus, Mars,
Phobos, Deimos, Ceres, Vesta, Io, Europa, Ganymede, Callisto, Titan, Enceladus, Miranda, Triton, Pluto,
Charon, the Moon. Jupiter, Saturn, Uranus and Neptune are not landable.

Surface radiation doses are real where measured (Moon 1.37 mSv/day, Mars 0.67, Jovian moons from
NASA estimates: Io ≈ 36 Sv/day, Europa ≈ 5.4 Sv/day, Ganymede ≈ 80 mSv/day, Callisto ≈ 0.1 mSv/day)
and estimates elsewhere. Ceres and Vesta mean anomalies are approximate.

## Δv map of the Solar System

All values in km/s, between low orbits unless stated. Generated by `DeltaVMap` (the test
`DeltaVMapTest` writes `build/astro/delta_v_map.md` and fails if this section is out of date).

<!-- delta-v-map:begin -->
#### Surface and orbit

| Body | g (m/s²) | v_esc (km/s) | Low orbit (km) | Surface → orbit | Orbit → surface | Orbit → escape |
|---|---:|---:|---:|---:|---:|---:|
| Mercury | 3.70 | 4.25 | 122 | 3.58 | 3.58 | 1.21 |
| Venus | 8.87 | 10.36 | 280 | 10.40 | 0.08 ¹ | 2.97 |
| Earth | 9.82 | 11.19 | 212 | 9.02 | 0.06 ¹ | 3.22 |
| Moon | 1.62 | 2.38 | 87 | 2.00 | 2.00 | 0.68 |
| Mars | 3.73 | 5.03 | 182 | 4.01 | 0.11 ¹ | 1.43 |
| Phobos | 0.01 | 0.01 | 1 | 0.01 | 0.01 | 0.00 |
| Deimos | 0.00 | 0.01 | 0 | 0.00 | 0.00 | 0.00 |
| Vesta | 0.25 | 0.36 | 13 | 0.22 | 0.22 | 0.10 |
| Ceres | 0.28 | 0.52 | 23 | 0.34 | 0.34 | 0.15 |
| Jupiter | 25.92 | 60.20 | 1637 | — | — | 17.43 |
| Io | 1.80 | 2.56 | 91 | 2.08 | 2.08 | 0.73 |
| Europa | 1.32 | 2.03 | 78 | 1.67 | 1.67 | 0.58 |
| Ganymede | 1.43 | 2.74 | 132 | 2.28 | 2.28 | 0.78 |
| Callisto | 1.24 | 2.44 | 121 | 2.04 | 2.04 | 0.70 |
| Saturn | 11.19 | 36.09 | 1630 | — | — | 10.43 |
| Enceladus | 0.11 | 0.24 | 13 | 0.19 | 0.19 | 0.07 |
| Titan | 1.35 | 2.64 | 261 | 2.50 | 0.04 ¹ | 0.74 |
| Uranus | 9.01 | 21.38 | 773 | — | — | 6.17 |
| Miranda | 0.08 | 0.19 | 12 | 0.15 | 0.15 | 0.06 |
| Neptune | 11.27 | 23.56 | 697 | — | — | 6.81 |
| Triton | 0.78 | 1.45 | 68 | 1.21 | 1.21 | 0.42 |
| Pluto | 0.62 | 1.21 | 59 | 1.01 | 1.01 | 0.35 |
| Charon | 0.29 | 0.59 | 30 | 0.49 | 0.49 | 0.17 |

¹ with aerobraking and parachutes; a fully powered landing costs about the same as the ascent.

#### From low Earth orbit

| Target | Departure | Capture | Capture (aerobraking) | Total | Transfer | Window every | Phase angle |
|---|---:|---:|---:|---:|---:|---:|---:|
| Mercury | 5.55 | 7.54 | — | 13.09 | 105 d | 116 d | 108° |
| Venus | 3.50 | 3.32 | 0.03 | 6.82 | 146 d | 584 d | -54° |
| Moon | 3.13 | 0.82 | — | 3.95 | 5 d | 1.5 h | 115° |
| Mars | 3.61 | 2.10 | 0.02 | 5.72 | 259 d | 780 d | 44° |
| Phobos | 3.61 | 1.87 | — | 5.48 | 259 d | 780 d | 44° |
| Deimos | 3.61 | 1.91 | — | 5.52 | 259 d | 780 d | 44° |
| Vesta | 4.53 | 4.20 | — | 8.73 | 398 d | 504 d | 72° |
| Ceres | 4.91 | 4.53 | — | 9.44 | 472 d | 467 d | 79° |
| Jupiter | 6.30 | 17.70 | 0.21 | 24.00 | 997 d | 399 d | 97° |
| Io | 6.30 | 6.44 | — | 12.75 | 997 d | 399 d | 97° |
| Europa | 6.30 | 5.39 | — | 11.70 | 997 d | 399 d | 97° |
| Ganymede | 6.30 | 4.23 | — | 10.54 | 997 d | 399 d | 97° |
| Callisto | 6.30 | 3.58 | — | 9.89 | 997 d | 399 d | 97° |
| Saturn | 7.28 | 10.84 | 0.12 | 18.12 | 6.0 y | 378 d | 106° |
| Enceladus | 7.28 | 5.88 | — | 13.16 | 6.0 y | 378 d | 106° |
| Titan | 7.28 | 2.95 | 0.01 | 10.23 | 6.0 y | 378 d | 106° |
| Uranus | 7.98 | 6.68 | 0.07 | 14.65 | 16.0 y | 370 d | 111° |
| Miranda | 7.98 | 3.73 | — | 11.71 | 16.0 y | 370 d | 111° |
| Neptune | 8.25 | 7.16 | 0.08 | 15.40 | 30.6 y | 367 d | 113° |
| Triton | 8.25 | 2.34 | — | 10.59 | 30.6 y | 367 d | 113° |
| Pluto | 8.36 | 3.04 | — | 11.40 | 45.5 y | 367 d | 114° |
| Charon | 8.36 | 3.13 | — | 11.49 | 45.5 y | 367 d | 114° |

#### Moons from their planet's low orbit

| Planet → moon | Departure | Capture | Total | Transfer |
|---|---:|---:|---:|---:|
| Earth → Moon | 3.13 | 0.82 | 3.95 | 5 d |
| Mars → Phobos | 0.70 | 0.54 | 1.25 | 2.2 h |
| Mars → Deimos | 1.10 | 0.65 | 1.75 | 6.6 h |
| Jupiter → Io | 12.94 | 6.61 | 19.56 | 9.5 h |
| Jupiter → Europa | 14.49 | 6.56 | 21.05 | 17.5 h |
| Jupiter → Ganymede | 15.54 | 5.63 | 21.16 | 33.5 h |
| Jupiter → Callisto | 16.33 | 4.76 | 21.09 | 3 d |
| Saturn → Enceladus | 6.65 | 4.46 | 11.11 | 8.1 h |
| Saturn → Titan | 9.59 | 2.84 | 12.42 | 3 d |
| Uranus → Miranda | 4.32 | 2.69 | 7.00 | 7.9 h |
| Neptune → Triton | 6.02 | 2.12 | 8.14 | 27.7 h |
| Pluto → Charon | 0.31 | 0.19 | 0.50 | 31.5 h |
<!-- delta-v-map:end -->

## Orbit service (server side)

`OrbitService.get(server)` gives one cached `Ephemeris` per server, clocked by the overworld game time
and `orbitalTimeScale`, so every dimension sees the same sky.

```java
OrbitService orbits = OrbitService.get(server);
Ephemeris.Window w = orbits.window("spacebreaker:earth", "spacebreaker:mars");
w.open();                       // within ±2° of the Hohmann phase angle (planet ↔ moon: always true)
long ticks = orbits.ticksUntil(w);   // countdown for the launch console (0 when open)
w.plan().total();               // Δv of the transfer (m/s), w.plan().transferTime() (simulated s)
Vec3 mars = orbits.ephemeris().position("spacebreaker:mars");       // m from the Sun, now
double au = orbits.ephemeris().starDistanceAu("spacebreaker:mars"); // for sunlight / solar panels
```

Positions are recomputed only when the clock moves; a window is kept until it has passed and then the
next one is searched from the real (eccentric) orbits. `Ephemeris` itself is pure Java and works with any
`BodySystem` (generated star systems too) and any clock.

## Appendix: terrain profiles for the inner planets

Reference numbers for the planet engine (Mars, Venus and Mercury surfaces). Real values first, then a
suggestion for the game. Elevations are relative to the mean radius (Mars: the MOLA datum).
Crater densities are cumulative counts N(≥ D) per km²; real size–frequency distributions follow roughly
N(≥ D) ∝ D⁻² between 1 and 100 km (steeper, ∝ D⁻³, for small craters on young surfaces).

**Vertical scale for the game.** Real relief (Mars: 30 km) does not fit a build height of 384 blocks.
Suggested mapping: keep horizontal distances 1 block = 1 m near the player, and compress heights with
`blocks = 64 + 9 · h_km` for |h| ≤ 8 km and a soft cap (`tanh`) above, so Olympus Mons is a rare,
huge mountain at ~ 250 and Hellas floors sit near −10. Crater shapes should keep their real
depth-to-diameter ratios at the block scale (they are small enough to fit).

**Crater shape (all three).** Simple bowl craters: depth ≈ 0.2 D, rim height ≈ 0.04 D, ejecta blanket
to ~ 1 D beyond the rim. Above the simple-to-complex transition diameter D_t (≈ inversely proportional to
gravity) craters get flat floors, terraced walls and central peaks, with depth ≈ 0.1 D^0.3 km.

### Mars
| Quantity | Value |
|---|---|
| Elevation range | −8.2 km (Hellas basin) to +21.9 km (Olympus Mons) |
| Hypsometry | bimodal: northern lowlands ≈ −4 ± 1 km (≈ 1/3 of the surface, smooth), southern highlands ≈ +1.5 ± 1.5 km (≈ 2/3, heavily cratered); volcanic rises above +5 km cover ≈ 3 % (Tharsis, Elysium) |
| Elevation σ | ≈ 3 km globally |
| Slopes | plains 0.5–2°, highlands 2–5°, shield volcano flanks 2–5°, canyon walls 20–35° |
| Craters | ≈ 385 000 craters ≥ 1 km on the planet (N ≈ 2.7·10⁻³ /km² on average); highlands ≈ 6·10⁻³, lowlands ≈ 1·10⁻³; D_t ≈ 7 km |
| Big features | Valles Marineris (4000 km long, up to 7 km deep, 200 km wide), Tharsis shields (300–600 km wide), outflow channels, polar layered deposits and caps (water ice + seasonal CO₂ frost), dune fields, dust devils |
| Surface | basalt (olivine, pyroxene) under fine iron-oxide dust; hematite spherules, sulfates (gypsum), clays (smectite) in old terrain, perchlorate salts in the soil, ground ice poleward of ≈ 45° |
| Colours (sRGB) | dust `#C1683C`, bright dust `#D9A066`, dark basalt sand `#5A4A42`, clay `#8C6A55`, ice cap `#E8E4DA`; day sky `#C9A27E`, sunset glow near the Sun `#6C8FB3` |

Game suggestions: two terrain archetypes split by a wavy dichotomy line at latitude 0–30° N
(z axis = latitude), crater density 3× higher in the south, ice-rich blocks near the poles, rare
canyon and shield-volcano features. Matching blocks from CONTENT.md: Mars regolith, sand, basalt,
olivine sand, clay, hematite spherule soil, perchlorate soil, dry ice, water ice.

### Venus
| Quantity | Value |
|---|---|
| Elevation range | −2.9 km (Diana Chasma) to +11 km (Maxwell Montes) |
| Hypsometry | unimodal: ≈ 60 % within ±0.5 km of the mean, lowland plains below −1 km ≈ 20 %, highlands above +2 km ≈ 8 % |
| Elevation σ | ≈ 1 km |
| Slopes | plains < 1–2°, tesserae 5–20°, mountain belts up to 30° |
| Craters | only ≈ 940 on the whole planet (N ≈ 2·10⁻⁶ /km²), randomly spread, none below ≈ 2 km (the thick air breaks small impactors; small ones appear as crater clusters and dark "splotches"); D_t ≈ 3 km; surface age 300–700 Myr |
| Big features | volcanic plains (≈ 80 %), lava channels up to 7000 km, shield volcanoes, coronae (ring-shaped uplifts 100–1000 km), pancake domes (25 km wide, 750 m high), tesserae (crumpled highlands ≈ 8 %), wind streaks |
| Surface | basalt (tholeiitic and alkaline), no water or ice; a bright metallic frost (lead/bismuth sulfides, tellurium) above ≈ +2.5 km |
| Colours (sRGB) | basalt seen in Venusian light `#8A6A3A`, true rock colour `#4A4440`, mountain-top frost `#BDB7A8`; sky and haze `#D8A657`, very dim and diffuse, no visible Sun disc |

Game suggestions: mostly flat, smooth plains with long lava channels and domes; few craters
(1 per several km² of play area at most); everything at 464 °C and 92 bar (cloud platforms are the
habitable layer, see DESIGN). Matching blocks: Venus basalt.

### Mercury
| Quantity | Value |
|---|---|
| Elevation range | −5.4 km to +4.5 km (≈ 10 km total) |
| Hypsometry | unimodal, σ ≈ 1 km |
| Slopes | like the Moon: smooth plains < 2°, cratered terrain 5–15°, crater walls 25–35° |
| Craters | heavily cratered terrain and intercrater plains close to lunar highlands (N(≥ 1 km) ≈ 2·10⁻² /km²); smooth plains (≈ 27 % of the surface) ≈ 5·10⁻³ /km²; D_t ≈ 10 km; Caloris basin 1550 km |
| Big features | lobate scarps (cliffs 1–3 km high, hundreds of km long), smooth volcanic plains, bright rayed craters, hollows (bright, shallow pits a few km across), permanently shadowed polar craters holding water ice |
| Surface | low-iron magnesium silicates, high sulfur (up to 4 wt %), dark low-reflectance material coloured by graphite, sodium and potassium in the exosphere |
| Colours (sRGB) | regolith `#8C8681`, low-reflectance material `#5E5B58`, smooth plains `#9C948A`, hollows `#B8BCC4`, polar ice `#D6DCE0`; black sky, Sun 2.5× wider than from Earth |

Game suggestions: Moon-like crater fields with long scarps and brighter hollows; ice only in crater
floors near the poles that never see the Sun; huge day/night swing (430 °C / −180 °C) with a 176-day
solar day, so the terminator matters. Matching blocks: Mercury graphitic regolith.

## Procedural star systems (`core/astro/galaxy`)

Everything is deterministic for a world seed (`new Galaxy(level.getSeed())`), pure Java and unit-tested.

```java
Galaxy galaxy = new Galaxy(server.overworld().getSeed());
List<StarSeed> near = galaxy.starsWithin(GalaxyModel.SUN, 10);       // pc; real stars included
StarSystem sys = galaxy.system(near.get(0)).orElseThrow();           // empty only for the Sun (use the registry)
sys.nameEn(); sys.designation();          // "Velorin", "SB 1F3K-9QX" (names are not translated)
sys.primary();                            // StellarModel.Star: mass, L, R, T, colour, class, lifetime
sys.binary();                             // Optional companion star (data only for now)
sys.planets();                            // AstroBody list; moons via sys.bodies().children(id)
sys.details(id);                          // irradiance, radiation, axial tilt, gravity, rings
sys.terrainHints(id);                     // TerrainHints for the planet engine
Map<ResourceLocation, CelestialBody> records = BodyAdapter.toCelestialBodies(sys, id -> Optional.empty());
```

| Step | Model |
|---|---|
| Galaxy | thin + thick disk, four log-spiral arms (pitch 12.5°), 27° bar, halo; 10 pc sectors made on first use and cached (LRU); star address = sector + index, id = 64-bit hash of seed and address |
| Names | catalogue designation `SB XXXX-XXX` from the id plus a syllable-based proper name (never a real star, planet or common word); planets add `b, c, …`, moons `I, II, …` |
| Stars | Kroupa IMF 0.08–150 M☉, age 0.1–10 Gyr; stars older than their lifetime become white dwarfs, neutron stars or black holes; mass → luminosity, radius, temperature, spectral class, black-body RGB |
| Binaries | companion with probability 27 % (< 0.5 M☉), 45 % (< 1.5 M☉), 60 % (heavier); mass ratio 0.1–1, separation log-normal around 50 AU; planets only inside the S-type stability limit 0.3 · a · (1 − e) |
| Planets | Poisson count (more for heavier stars), geometric spacing 1.4–2.3×, every periapsis ≥ 1.15 × the previous apoapsis (orbits never cross); snow line 2.7 AU · √L: rocky / iron / ocean worlds inside, giants (35 % · M per slot) and icy worlds outside; mass–radius laws per composition |
| Moons and rings | giants: Poisson(2.5) moons, terrestrial planets above 0.3 M⊕: 30 % chance of one; all moons inside a third of the Hill sphere and outside the Roche limit; 40 % of giants have rings inside their Roche limit for ice |
| Climate | equilibrium temperature with albedo by composition, grey greenhouse from pressure and CO₂/CH₄, atmosphere only if Jeans retention (exosphere temperature) keeps N₂; tidal locking from the Gladman formula; day/night swing from pressure; surface radiation from stellar flux and air column |

### Terrain hints (`TerrainHints`)

`TerrainHints.of(bodySystem, id)` for Solar System bodies, `StarSystem.terrainHints(id)` for generated
ones. All values come from physics, so the same rules give the real Solar System its look (unit-tested:
Moon and Mercury grey regolith, Mars rust, Venus dark basalt, Io sulfur with lava, Europa pale ice,
Callisto dirty ice, Titan organic haze with methane lakes, Earth oceans).

| Field | Meaning and rule |
|---|---|
| `colorFamily`, `baseColor`, `accentColor` | `GREY_REGOLITH`, `RUST`, `DARK_BASALT`, `PALE_ICE`, `DIRTY_ICE`, `SULFUR`, `ORGANIC_HAZE`, `OCEAN`, `GAS_BANDS`, with sRGB colours (±8 % per-body tint) |
| `craterDensity` | craters ≥ 1 km per km²: 10⁻² for an old airless surface, reduced by air, volcanism and liquids; ≤ 2·10⁻⁶ under very thick air |
| `liquid`, `liquidCoverage` | `WATER`, `METHANE`, `NITROGEN` or `LAVA` when the surface temperature lies between the melting point and the boiling point at the surface pressure (Clausius–Clapeyron), lava above 1000 K or with Io-class tidal heating |
| `glaciation` | 1 for icy worlds below 0 °C, polar caps on cold worlds with air, polar cold traps on airless ones |
| `volcanism` | max of radiogenic heat (√mass · e^(−age/4 Gyr)) and tidal heating (Ė = 21/2 · k₂/Q · G M² R⁵ n e² / a⁶, Io = 1) |
| `roughness`, `dunes` | relief from craters, volcanism and low gravity; dunes when there is wind (p > 0.3 kPa) and dry land |
| `volatileIce` | fraction under frozen nitrogen/methane glaciers: a nitrogen atmosphere over a surface below 63 K (Pluto, Triton) |
| `plumes` | geysers: icy worlds with strong tides (Enceladus, Europa) or nitrogen ice in enough sunlight (Triton) |

Large retrograde moons count as captured: the tides that circularised their orbits resurfaced them
(Triton: young, cryovolcanic).

### Terrain parameters (`TerrainParams`)

`TerrainParams.of(body, hints)` turns the hints into the numbers of a worldgen `TerrainProfile`
(block units): base height, relief amplitude/scale/detail/roughness, lowland threshold and depth, crater
density multiplier (Moon = 1, Venus ≈ 0.35), crater depth/rim ratios, complex-crater radius (∝ g^−0.3),
fresh-crater share, polar and ice latitudes (caps cover the glaciated fraction: 1 − sin φ = f), sea level
and liquid, and switches for dunes, volcanoes, valleys and ice caps. `referenceProfile()` names a
hand-made profile whose blocks fit the colour family (`moon`, `mars`, `venus`) until generic
colour-variant blocks exist. Unit tests keep the real bodies close to their hand-made profiles.

## Interstellar travel (`Interstellar`)

```java
List<Interstellar.Destination> near = Interstellar.nearby(galaxy, galaxy.sun(), 15); // ly, nearest first
near.get(0).nameEn();          // "Proxima Centauri", 4.24 ly; summary(): kind, class, colour, mass, L
Interstellar.Trip t = Interstellar.trip(near.get(0).distanceLy(), 0.1);  // cruise at 0.1 c
t.deltaV(); t.travelYears(); t.shipYears();   // 2·c·atanh β, d/β, with time dilation
```

A placeholder for the deep space drive: gate it behind the research tech `Interstellar.REQUIRED_TECH`
(`spacebreaker:deep_space_drive`); for gameplay, `scaledTravelSeconds(ly, secondsPerLy)` gives a short
scaled trip time.

## Appendix: terrain profiles for the asteroids and outer moons

Same format as the inner-planet appendix. Colours are sRGB suggestions for block ramps (dark → light);
albedos are geometric (Bond in the body JSON). `TerrainParams` values come from `OuterTerrainTest`,
which keeps their ranking real: Callisto ≥ Ganymede > Enceladus > Europa in crater density, Io none.

### Ceres (dwarf planet, 940 km)
| Quantity | Value |
|---|---|
| Relief | ≈ 15 km total (−7.3 to +7.4 km); Ahuna Mons, a 4 km high, 17 km wide cryovolcanic dome |
| Craters | heavily cratered but few basins > 300 km (they relaxed); polygonal craters; D_t ≈ 10 km; Occator (92 km) with bright faculae |
| Surface | ammoniated clays (phyllosilicates), magnesium and sodium carbonates, salts; water ice a few metres down and in shadowed polar craters |
| Albedo / colours | 0.09; dark regolith ramp `#3E3C3A` `#5C5A57` `#77746F`, faculae `#E8E6E0`, salt crust `#CFCAC0` |
| Game | dark grey cratered plains, rare bright salt patches in crater floors, a single tall dome; ice only under the surface and at the poles |

### Vesta (asteroid, 525 km)
| Quantity | Value |
|---|---|
| Relief | extreme for its size: ≈ 41 km total; Rheasilvia basin 505 km wide with a ≈ 22 km central peak |
| Craters | saturated; D_t ≈ 30 km; equatorial troughs (Divalia Fossa 465 km long, 22 km wide, 5 km deep) |
| Surface | basaltic howardite–eucrite–diogenite rock (pyroxene-rich), dark carbonaceous patches from impacts |
| Albedo / colours | 0.42; ramp `#6E665E` `#9A9086` `#C8BFB2`, dark material `#4A4440`, bright fresh ejecta `#DDD5C8` |
| Game | steep, rugged and bright; one giant basin with a central mountain; parallel trough grooves |

### Io
| Quantity | Value |
|---|---|
| Relief | non-volcanic mountains up to 17.5 km (Boösaule Montes), plateaus, calderas (paterae) up to 200 km |
| Craters | no impact craters at all (resurfaced ≈ 1 cm per year) |
| Volcanism | > 400 active volcanoes, lava lakes (Loki Patera, 200 km), plumes up to 400 km high; silicate lava at 1300–1600 K |
| Surface | sulfur allotropes and SO₂ frost over silicate crust |
| Albedo / colours | 0.63; sulfur ramp `#8C6A2E` `#D9C25A` `#EDE39A`, red short-chain sulfur `#B5552B`, SO₂ frost `#E8E2C8`, fresh black lava `#2A2622`, glowing lava `#FF8A2A` |
| Game | colourful flat plains, scattered calderas with lava lakes, isolated tall mountains; no craters; extreme radiation (data: 36 Sv/day) |

### Europa
| Quantity | Value |
|---|---|
| Relief | very flat: mostly < 200 m, ridges 100–300 m high, rare features up to ≈ 1 km |
| Craters | very few (≈ 40 larger than 10 km), surface 40–90 Myr old |
| Features | double ridges and bands (lineae) crossing everywhere, chaos terrain (ice rafts in refrozen matrix), possible plumes |
| Surface | water ice with hydrated salts and sulfuric acid; reddish-brown material along lineae and chaos |
| Albedo / colours | 0.67; ice ramp `#C9CDD0` `#E6EBEF` `#F4F6F8`, lineae `#A0603C` `#7A4A32`, chaos matrix `#B59A82` |
| Game | smooth white plains criss-crossed by brown ridge lines, patches of jumbled ice blocks; ocean under ≈ 15–25 km of ice |

### Ganymede
| Quantity | Value |
|---|---|
| Relief | modest; grooved terrain with parallel ridges and troughs a few hundred metres high, spaced 3–10 km |
| Craters | dark terrain (≈ 1/3, old) heavily cratered; bright terrain (≈ 2/3) younger with fewer craters; palimpsests (flat crater ghosts) |
| Surface | water ice and silicates; thin polar frost caps above ≈ 40° latitude |
| Albedo / colours | 0.43; dark terrain `#4E463E` `#6E6458`, bright grooved terrain `#9C9488` `#BDB6AA`, polar frost `#DCDDDC` |
| Game | two terrain archetypes (dark cratered / bright grooved) split by noise; frosted poles |

### Callisto
| Quantity | Value |
|---|---|
| Relief | low; ring structures of multi-ring basins (Valhalla, 3800 km rings), knobs and ice spires a few hundred metres high |
| Craters | the most heavily cratered body known (saturated); few small craters (eroded by sublimation) |
| Surface | ice mixed with dark dust that covers most of it; bright ice on crater rims and slopes |
| Albedo / colours | 0.22; dust `#3F3A33` `#5F574C` `#7A7166`, exposed ice `#B8B3A8` `#D6D3CC` |
| Game | dark, smooth-ish crater fields, bright crater rims, very old and quiet; low radiation (safest Jovian moon) |

### Titan
| Quantity | Value |
|---|---|
| Relief | gentle: mostly within ±1 km, mountains up to 3.3 km (Mithrim Montes), labyrinth terrain |
| Craters | very few (≈ 90 known), eroded and buried |
| Features | equatorial longitudinal dunes of organic sand (≈ 100 m high, 1–2 km apart, hundreds of km long); river channels; north polar seas (Kraken Mare ≈ 400 000 km², Ligeia Mare ≈ 126 000 km²) and southern lakes; lakes and seas cover ≈ 1.5–2 % of the surface |
| Surface | water-ice bedrock under organic sediments (tholins); "pebbles" of ice |
| Albedo / colours | 0.22; under the haze the ground looks `#5A3E24` `#8A5A2E` `#A8743C`, dune sand `#3A2A1E` `#4E3A2A`, ice pebbles `#9C8A70`, lake liquid `#1E1A14` |
| Haze (body JSON) | `colour` `#b07a3e`, `high_colour` `#d9a35a`, `visibility_blocks` 160 — thinner near the surface than Venus but the sky is a uniform orange |
| Game | orange-lit dunes at the equator, rolling plains and channels, methane lakes towards the poles; 1.47 bar, −179 °C, no suit pressure needed but full thermal protection |

**Methane cycle (Titan).** Surface temperature 94 K, pressure 146.7 kPa; methane is 4.9 % of the air
near the surface with ≈ 45 % relative humidity (Huygens). Clouds form at 20–40 km; rain is rare but
heavy (seasonal storms, one year on Titan = 29.5 Earth years), drops 1–10 mm fall slowly (≈ 1.6 m/s).
Liquids: methane (melts 90.7 K, boils 111.7 K at 1 atm, ≈ 118 K at Titan's surface pressure), ethane
(melts 90.4 K) and dissolved nitrogen; Ligeia Mare is roughly 70 % methane, 15 % nitrogen, 15 % ethane.
Evaporation from the seas is slow; most rain falls near the poles in summer. Game suggestion: methane
rain weather that fills low basins near the poles, liquid that does not freeze at surface temperature,
and no water anywhere except as rock (ice).

### Enceladus
| Quantity | Value |
|---|---|
| Relief | ridged plains; south polar "tiger stripes": 4 parallel fractures ≈ 130 km long, 2 km wide, 500 m deep with 100 m flanking ridges |
| Craters | north cratered (softened craters), south polar region almost crater-free (< 1 Myr) |
| Activity | plumes of water vapour and ice grains from the tiger stripes (≈ 200 kg/s) feeding Saturn's E ring; global ocean |
| Surface | nearly pure, fresh water ice — the most reflective surface in the Solar System |
| Albedo / colours | 1.38 (geometric), Bond 0.81; ice ramp `#D9DEE2` `#EEF1F4` `#FAFBFC`, tiger-stripe blue `#8AA8C0`, plume deposits `#F6F8FA` |
| Game | blinding white plains, cratered north, four long blue-tinted fissures near the south pole venting ice-crystal plumes |

## Appendix: terrain profiles for Miranda, Triton, Pluto and Charon

`TransNeptunianTerrainTest` keeps these real: Triton young with geysers and a nitrogen cap, Pluto with
nitrogen glaciers but no geysers, Charon and Miranda old and rugged.

### Miranda (Uranus, 472 km)
| Quantity | Value |
|---|---|
| Relief | the most extreme for its size: Verona Rupes, a fault scarp 5–10 km high (the tallest cliff known); canyons up to 20 km deep |
| Features | three coronae (Arden, Elsinore, Inverness): huge oval or trapezoid patches of parallel ridges and grooves, 200–300 km across, cut into old cratered terrain |
| Craters | old rolling terrain heavily cratered; coronae younger, fewer craters |
| Surface | water ice with silicates and carbon compounds |
| Albedo / colours | 0.32; old terrain `#5E5A55` `#86817A`, corona bands alternating `#6A655F` / `#ABA59C`, bright scarp faces `#C8C3BA` |
| Game | patchwork: cratered grey uplands interrupted by striped corona provinces and colossal cliffs; very low gravity (0.08 m/s²) |

### Triton (Neptune, 2707 km)
| Quantity | Value |
|---|---|
| Relief | low, mostly < 1 km; cryovolcanic plains, walled plains, "cantaloupe terrain" (dimpled ridges, cells 30–40 km) |
| Craters | very few (surface 10–100 Myr old) |
| Activity | nitrogen geysers: dark plumes rising ≈ 8 km, blown sideways into dark streaks 100+ km long on the southern cap; driven by sunlight warming the nitrogen ice |
| Atmosphere | ≈ 1.4 Pa nitrogen with traces of methane, thin haze; 38 K |
| Surface | frozen nitrogen (55 % of the cap), water ice, CO₂ and methane ice |
| Albedo / colours | 0.72; south polar cap `#E2C9C0` `#F0DDD6` (pinkish), cantaloupe terrain `#B8B0A2` `#9C9486`, geyser streaks `#4A4440`, bluish fresh frost `#D2DCE4` |
| Game | smooth pale plains, a pinkish polar cap with dark wind streaks and active geyser vents, dimpled terrain elsewhere; orbits Neptune backwards |

### Pluto (dwarf planet, 2377 km)
| Quantity | Value |
|---|---|
| Relief | water-ice mountains up to 6 km (Tenzing Montes, Hillary Montes) around Sputnik Planitia; possible cryovolcanoes (Wright Mons, 4 km high, 150 km wide) |
| Features | Sputnik Planitia: a 1000 km nitrogen-ice glacier basin with no craters, split into convection cells 20–30 km across, ≈ 3–4 km below the surroundings; glaciers flowing into it; bladed methane terrain (Tartarus Dorsa, blades up to 500 m) |
| Craters | moderately cratered elsewhere; Cthulhu Macula old and heavily cratered |
| Atmosphere | ≈ 1 Pa nitrogen with methane and CO; layered blue haze up to 200 km (tholin particles) |
| Surface | N₂, CH₄ and CO ices; water-ice bedrock; red-brown tholins |
| Albedo / colours | 0.52 (geometric), Bond 0.72; nitrogen plains `#E8E2D8` `#F4EFE6`, Cthulhu tholins `#5A2E1E` `#7A3E26`, mid terrain `#B08A6A` `#C9A88A`, water-ice mountains `#D6D2CC` |
| Haze (body JSON) | `colour` `#5f7fa6`, `high_colour` `#8fb0d6`, `visibility_blocks` 512 — a thin blue haze on a mostly dark sky |
| Game | a giant bright crater-free ice plain with polygonal cells and floating water-ice blocks, ringed by tall ice mountains; dark red cratered lands elsewhere |

### Charon (Pluto, 1212 km)
| Quantity | Value |
|---|---|
| Relief | belt of chasms across the equator (Serenity Chasma, Mordor area canyons up to 7–9 km deep); southern Vulcan Planitia smoother and lower |
| Craters | northern terrain heavily cratered; Vulcan Planitia fewer |
| Features | Mordor Macula: a dark red north polar cap made of tholins from Pluto's escaping methane |
| Surface | water ice with ammonia hydrates; no atmosphere |
| Albedo / colours | 0.38 (geometric); grey ice `#8C8A86` `#ABA8A3`, Mordor red `#6E3A28` `#8A4A30`, fresh crater ice `#D0CEC9` |
| Game | grey, rugged and old; deep canyons across the middle, a rusty red north pole; Pluto hangs fixed in the sky (mutual tidal lock) |

## Star map data (`galaxy.GalaxyMap`)

```java
// local view: stars around a viewpoint, brightest first
List<GalaxyMap.Marker> sky = GalaxyMap.local(galaxy, galaxy.sun(), 25, 200);  // radius ly, max markers
GalaxyMap.Marker m = sky.get(0);   // Sirius, m ≈ −1.5
m.offsetLy();  m.colorRgb();  m.apparentMagnitude();  m.nameEn();  m.nameTr();  m.designation();  m.summary();

// galaxy view: a top-down picture (same for every seed), log-scaled brightness and colour per cell
GalaxyMap.Overview o = GalaxyMap.overview(128);
o.brightness()[x][y];  o.color()[x][y];  o.sun();   // Sun cell, x points towards the galactic centre
```

Apparent magnitudes use the luminosity as if it were visual light (m = 4.83 − 2.5 log L + 5 log d − 5),
which is right for Sun-like stars and too bright for red dwarfs (Proxima shows as ≈ 7 instead of 11) —
fine for a map. Overview colours blend an old yellow population (`OLD_STARS`) with young blue stars
along the spiral arms (`YOUNG_STARS`).

Planets of the real nearby stars use the same `TerrainHints`/`TerrainParams` rules;
`NearbyPlanetsTerrainTest` checks them for several seeds (liquids only within their temperature range,
molten worlds look dark, no ice on warm worlds, TRAPPIST-1 sunlight falling outwards).
