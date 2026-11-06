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
