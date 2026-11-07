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
| `galaxy.NearbyStars` | real-star catalogue: the Sun and 43 star systems within 25 ly, Fomalhaut just outside (positions from J2000 RA/Dec and distance, companions such as Alpha Centauri B, Sirius B, Procyon B) plus TRAPPIST-1, with their **confirmed** planets (Proxima b and d, Barnard's Star b–e, Gliese 876, HD 219134, Teegarden's Star, Wolf 1061, GJ 1061, YZ Ceti, Luyten's Star, Lacaille 9352, Ross 128, Lalande 21185, Gliese 581, Gliese 667 C, Epsilon Eridani b, Epsilon Indi Ab, TRAPPIST-1 b–h). Inside 25 ly only real stars exist; planets the generator adds around them have `Details.discovered() == false` ("not yet discovered") |
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
`mean_anomaly_deg` (J2000, default 0), and secular rates `node_rate_deg_per_year`,
`periapsis_rate_deg_per_year` (default 0, a fixed orbit), and the spin axis `rotation.pole_ra_deg`,
`rotation.pole_dec_deg` (ICRF J2000, right-hand rule; default none).

**Moon orbits in the planet's equator.** If a planet has a pole, its moons' `inclination_deg`,
`longitude_ascending_node_deg` and `argument_periapsis_deg` are measured in its equator, with Ω
counted from the equator's ascending node on the ecliptic (`OrbitalElements.referencePole`,
`Frames.equatorialMoonOrbits`). Poles come from IAU WGCCRE 2015: Mars 317.681° / 52.887°, Jupiter
268.057° / 64.495°, Saturn 40.589° / 83.537°, Uranus 77.311° / 15.175° (the antipode of the IAU
north pole, because Uranus spins retrograde), Neptune 299.36° / 43.46°, Pluto 132.993° / −6.163°.
Earth has none, so the Moon stays referred to the ecliptic. Before this change every moon orbited
in the ecliptic, and Uranus's moons were eclipsed every few days in every year.
`MoonFramesTest` checks:
- Each pole reproduces its planet's axial tilt within 3°.
- Saturn's modelled equinox falls in 2009.6 and Uranus's in 2007.9.
- Uranus's moons are eclipsed around the 2007 equinox and not at the 2028 solstice.
- Titan is eclipsed around 2009 but not in 2017.
- Phobos misses Mars's shadow near the solstice of January 2000 and is eclipsed every orbit half a
  season later.

**Secular precession.** With rates, Ω(t) = Ω₀ + Ω̇t and ω(t) = ω₀ + ω̇t, and the mean anomaly
advances at n − Ω̇ − ω̇, so the mean longitude keeps the sidereal rate set by `period_days`. Only the
Moon uses it: Ω̇ = −19.341°/y (the nodes regress once in 18.61 years) and ω̇ = +60.032°/y (the line of
apsides, Ω + ω, advances once in 8.85 years), with Meeus's J2000 mean elements (Ω₀ 125.0445°,
ω₀ 318.3087°, M₀ 134.9634°, sidereal month 27.321662 d). Without it the eclipse seasons drift by about
19° a year away from J2000. `LunarEclipseTest` finds seven real total lunar eclipses (2000-01-21,
2010-12-21, 2015-09-28, 2022-05-16, 2022-11-08, 2025-03-14, 2025-09-07). In each the Moon passes
0.23–0.37° from the shadow axis, inside the umbra, within 3 hours of the observed time. The
remaining error is the Moon's periodic perturbations (evection, variation), which are not modelled.
The full moon of 2022-08-12, between those seasons, passes 5.1° away and is not eclipsed.

Landable bodies use the dimension `spacebreaker:<name>` (Earth is the overworld): Mercury, Venus, Mars,
Phobos, Deimos, Ceres, Vesta, Io, Europa, Ganymede, Callisto, Titan, Enceladus, Miranda, Triton, Pluto,
Charon, the Moon. Jupiter, Saturn, Uranus and Neptune are not landable.

Galilean moons: the J2000 phases (`mean_anomaly_deg`, measured from Jupiter's equatorial node on the
ecliptic at 337.8°) and the periods come from Meeus's mean longitudes and mean motions (*Astronomical
Algorithms*, ch. 44), so the moons stand where the real ones did. `GalileanMoonsTest` checks the Laplace
resonance λ_Io − 3λ_Europa + 2λ_Ganymede = 180° (±0.5°) from 2000 to 2100.

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
| Bennu | 0.00 | 0.00 | 0 | 0.00 | 0.00 | 0.00 |
| Ryugu | 0.00 | 0.00 | 0 | 0.00 | 0.00 | 0.00 |
| Itokawa | 0.00 | 0.00 | 0 | 0.00 | 0.00 | 0.00 |
| Eros | 0.01 | 0.01 | 0 | 0.01 | 0.01 | 0.00 |
| Mars | 3.73 | 5.03 | 182 | 4.01 | 0.65 ¹ | 1.43 |
| Phobos | 0.01 | 0.01 | 1 | 0.01 | 0.01 | 0.00 |
| Deimos | 0.00 | 0.01 | 0 | 0.00 | 0.00 | 0.00 |
| Encke | 0.00 | 0.00 | 0 | 0.00 | 0.00 | 0.00 |
| Vesta | 0.25 | 0.36 | 13 | 0.22 | 0.22 | 0.10 |
| Ceres | 0.28 | 0.52 | 23 | 0.34 | 0.34 | 0.15 |
| Tempel 1 | 0.00 | 0.00 | 0 | 0.00 | 0.00 | 0.00 |
| Churyumov Gerasimenko | 0.00 | 0.00 | 0 | 0.00 | 0.00 | 0.00 |
| Jupiter | 25.92 | 60.20 | 1637 | — | — | 17.43 |
| Io | 1.80 | 2.56 | 91 | 2.08 | 2.08 | 0.73 |
| Europa | 1.32 | 2.03 | 78 | 1.67 | 1.67 | 0.58 |
| Ganymede | 1.43 | 2.74 | 132 | 2.28 | 2.28 | 0.78 |
| Callisto | 1.24 | 2.44 | 121 | 2.04 | 2.04 | 0.70 |
| Saturn | 11.19 | 36.09 | 1630 | — | — | 10.43 |
| Enceladus | 0.11 | 0.24 | 13 | 0.19 | 0.19 | 0.07 |
| Titan | 1.35 | 2.64 | 261 | 2.50 | 0.04 ¹ | 0.74 |
| Halley | 0.00 | 0.00 | 0 | 0.00 | 0.00 | 0.00 |
| Uranus | 9.01 | 21.38 | 773 | — | — | 6.17 |
| Miranda | 0.08 | 0.19 | 12 | 0.15 | 0.15 | 0.06 |
| Ariel | 0.25 | 0.54 | 29 | 0.44 | 0.44 | 0.15 |
| Umbriel | 0.25 | 0.54 | 29 | 0.45 | 0.45 | 0.16 |
| Titania | 0.36 | 0.76 | 39 | 0.63 | 0.63 | 0.22 |
| Oberon | 0.35 | 0.73 | 38 | 0.61 | 0.61 | 0.21 |
| Neptune | 11.27 | 23.56 | 697 | — | — | 6.81 |
| Proteus | 0.07 | 0.17 | 11 | 0.13 | 0.13 | 0.05 |
| Triton | 0.78 | 1.45 | 68 | 1.21 | 1.21 | 0.42 |
| Nereid | 0.07 | 0.16 | 9 | 0.11 | 0.11 | 0.04 |
| Pluto | 0.62 | 1.21 | 59 | 1.01 | 1.01 | 0.35 |
| Charon | 0.29 | 0.59 | 30 | 0.49 | 0.49 | 0.17 |
| Nix | 0.01 | 0.02 | 1 | 0.01 | 0.01 | 0.01 |
| Hydra | 0.01 | 0.02 | 1 | 0.01 | 0.01 | 0.01 |
| Hale Bopp | 0.00 | 0.02 | 2 | — | — | 0.00 |

¹ with aerobraking and parachutes; a fully powered landing costs about the same as the ascent.

#### From low Earth orbit

| Target | Departure | Capture | Capture (aerobraking) | Total | Transfer | Window every | Phase angle |
|---|---:|---:|---:|---:|---:|---:|---:|
| Mercury | 5.55 | 7.54 | — | 13.09 | 105 d | 116 d | 108° |
| Venus | 3.50 | 3.32 | 0.03 | 6.82 | 146 d | 584 d | -54° |
| Moon | 3.13 | 0.82 | — | 3.95 | 5 d | 1.5 h | 115° |
| Bennu | 3.26 | 0.85 | — | 4.10 | 200 d | 6.1 y | 15° |
| Ryugu | 3.30 | 1.21 | — | 4.50 | 209 d | 4.4 y | 21° |
| Itokawa | 3.41 | 1.87 | — | 5.28 | 229 d | 2.9 y | 32° |
| Eros | 3.54 | 2.41 | — | 5.95 | 249 d | 845 d | 41° |
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
| Ariel | 7.98 | 3.24 | — | 11.21 | 16.0 y | 370 d | 111° |
| Umbriel | 7.98 | 3.08 | — | 11.06 | 16.0 y | 370 d | 111° |
| Titania | 7.98 | 2.86 | — | 10.84 | 16.0 y | 370 d | 111° |
| Oberon | 7.98 | 2.87 | — | 10.84 | 16.0 y | 370 d | 111° |
| Neptune | 8.25 | 7.16 | 0.08 | 15.40 | 30.6 y | 367 d | 113° |
| Proteus | 8.25 | 3.78 | — | 12.03 | 30.6 y | 367 d | 113° |
| Triton | 8.25 | 2.34 | — | 10.59 | 30.6 y | 367 d | 113° |
| Nereid | 8.25 | 3.13 | — | 11.38 | 30.6 y | 367 d | 113° |
| Pluto | 8.36 | 3.04 | — | 11.40 | 45.5 y | 367 d | 114° |
| Charon | 8.36 | 3.13 | — | 11.49 | 45.5 y | 367 d | 114° |
| Nix | 8.36 | 3.55 | — | 11.91 | 45.5 y | 367 d | 114° |
| Hydra | 8.36 | 3.56 | — | 11.93 | 45.5 y | 367 d | 114° |

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
| Uranus → Ariel | 4.86 | 2.48 | 7.34 | 13.0 h |
| Uranus → Umbriel | 5.20 | 2.37 | 7.57 | 20.2 h |
| Uranus → Titania | 5.56 | 2.01 | 7.57 | 40.3 h |
| Uranus → Oberon | 5.71 | 1.83 | 7.55 | 3 d |
| Neptune → Proteus | 4.65 | 2.97 | 7.62 | 6.4 h |
| Neptune → Triton | 6.02 | 2.12 | 8.14 | 27.7 h |
| Neptune → Nereid | 6.75 | 0.91 | 7.66 | 64 d |
| Pluto → Charon | 0.31 | 0.19 | 0.50 | 31.5 h |
| Pluto → Nix | 0.33 | 0.09 | 0.42 | 5 d |
| Pluto → Hydra | 0.33 | 0.08 | 0.42 | 7 d |
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

### Near-Earth asteroids (Bennu, Ryugu, Itokawa, Eros)
All four cross or approach Earth's orbit (q < 1.3 AU) and are the cheapest bodies to land on after the Moon:
surface gravity is micro-g (Bennu ≈ 80 µm/s², Eros ≈ 6 mm/s²), so landing and take-off cost almost nothing and
the Δv is dominated by the heliocentric transfer. Escape speeds are tens of centimetres per second (Bennu ≈ 0.2 m/s, Eros ≈ 10 m/s):
a jump or a careless step leaves the body.

| Body | Size | Shape / spin | Surface | Albedo (geom.) / colours | Game |
|---|---|---|---|---|---|
| Bennu | 490 m | spinning top, equatorial ridge; 4.30 h | B-type carbonaceous rubble pile; boulders up to 50 m, hydrated clays, carbonate veins; ejects pebbles | 0.044; `#1E1D1C` `#2E2C2A` `#3F3C39`, carbonate veins `#D8D3C8` | pitch-black boulder field, no fine dust, occasional floating pebbles |
| Ryugu | 900 m | spinning top; 7.63 h, retrograde | C-type rubble pile, very porous, boulder-strewn, few smooth ponds | 0.045; `#232120` `#353230` `#46423E` | like Bennu but rounder, dark gravel plains between boulders |
| Itokawa | 535 × 294 × 209 m | contact binary "sea otter"; 12.1 h | S-type (LL chondrite); rough boulder highlands and smooth gravel seas (Muses Sea) | 0.53 (V band); `#6E6A62` `#8E887D` `#ADA699` | two lobes joined by a gravel neck; grey boulders vs. smooth gravel ponds |
| Eros | 34 × 11 × 11 km | elongated saddle; 5.27 h | S-type, ≈ 100 m regolith; Psyche crater (5.3 km), Himeros saddle, flat-floored dust ponds, grooves | 0.25; `#6F6355` `#8F8070` `#B0A08C`, fresh crater walls `#C8BBA6` | the largest NEA: real craters, long linear grooves, dust ponds, boulders from Shoemaker Regio |

Bond albedos in the JSONs: Bennu 0.017, Ryugu 0.018, Eros 0.07, Itokawa 0.20. Orbits use J2000 elements
(Bennu a = 1.126 AU, e = 0.204; Ryugu 1.190 AU, 0.190; Itokawa 1.324 AU, 0.280; Eros 1.458 AU, 0.223) with
the period from Kepler's third law, and `solar_irradiance` = 1361 / a² (the orbit mean the other bodies use).
`AsteroidTerrain` can take the colour ramps above; a dimension for these bodies is an outer/universe decision.

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

### Ariel (Uranus, 1158 km)
| Quantity | Value |
|---|---|
| Relief | chasmata: flat-floored graben valleys 15–50 km wide and up to several km deep, criss-crossing the surface, some floors filled with smooth, ridged material (likely cryovolcanic) |
| Craters | the youngest-looking of Uranus' big moons: fewer large craters than Umbriel or Oberon, many small fresh craters with bright rays |
| Surface | water ice with CO₂ ice on the trailing hemisphere and dark carbon-rich material; ≈ −189 °C at noon, −213 °C at night |
| Albedo / colours | 0.53 (geometric), Bond 0.23; ice plains `#8E8A84` `#B3AEA6` `#CFCAC2`, graben floors (smooth) `#9A958E`, fresh crater rays `#E4E2DE`, CO₂ frost `#DADDE0` |
| Game | brighter than Miranda and smoother: grey-white cratered plains cut by long, deep, flat-bottomed canyons; a few bright rayed craters; gravity 0.25 m/s² |

### Umbriel (Uranus, 1169 km)
| Quantity | Value |
|---|---|
| Relief | low and uniform; old, heavily cratered terrain; Wunda crater (131 km) has a bright ring on its floor |
| Craters | saturated, the oldest surface of Uranus' big moons; few fresh rayed craters |
| Surface | water ice darkened by carbon-rich material, CO₂ ice on the trailing side; the darkest of the five big moons |
| Albedo / colours | 0.26 (geometric), Bond 0.10; dark ramp `#3F3D3B` `#56534F` `#6C6863`, Wunda ring `#C9C5BE` |
| Game | a dark, quiet crater field under a huge Uranus; one bright ring as a landmark |

### Titania (Uranus, 1578 km — the largest)
| Quantity | Value |
|---|---|
| Relief | great fault canyons (Messina Chasmata, 1500 km long, up to 2–5 km deep), scarps |
| Craters | moderately cratered; fewer large craters than Oberon or Umbriel (resurfaced); Gertrude crater 326 km |
| Surface | water ice and dark material, CO₂ frost on the trailing hemisphere |
| Albedo / colours | 0.35 (geometric), Bond 0.17; ramp `#5C5853` `#7D7871` `#9C968E`, canyon walls `#B5B0A8`, CO₂ frost `#D4D7DA` |
| Game | grey uplands crossed by enormous straight canyons; gravity 0.36 m/s² |

### Oberon (Uranus, 1523 km — the outermost)
| Quantity | Value |
|---|---|
| Relief | an 11 km high mountain on the limb, chasmata, many large craters with central peaks |
| Craters | heavily cratered and old; several crater floors partly flooded with dark material |
| Surface | water ice, dark carbon-rich material on crater floors, reddish tint |
| Albedo / colours | 0.31 (geometric), Bond 0.14; ramp `#57524C` `#77706A` `#958D85`, dark crater floors `#3A3633`, reddish dust `#8A6E62` |
| Game | rugged, cratered and dim; dark pools on crater floors, one giant mountain |

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

### Proteus and Nereid (Neptune)
| Body | Facts | Look and game |
|---|---|---|
| Proteus (420 km) | one of the largest irregular moons: a lumpy, box-shaped body just under the size where gravity would pull it round; Pharos crater ≈ 255 km; orbits in 27 h, tidally locked | very dark (geometric albedo 0.10), grey-brown `#3A3633` `#4F4A45` `#625C56`; heavily cratered, a giant crater filling one face |
| Nereid (340 km) | probably a captured body on the most eccentric moon orbit known (e = 0.75: 1.4 to 9.7 million km from Neptune, a 360-day year); not locked, spins in ≈ 11.6 h | neutral grey `#6A6762` `#8A8680` `#A8A49D` (geometric 0.16), water ice; Neptune is a big disc at closest approach and a bright star at the far end |

### Nix and Hydra (Pluto)
| Body | Facts | Look and game |
|---|---|---|
| Nix (≈ 50 × 33 × 31 km) | small, elongated; orbits the Pluto–Charon pair in 24.9 days; spins chaotically (≈ 44 h); masses are estimates | bright water ice (geometric 0.56) with a reddish patch: `#B9B4AC` `#D4D0C9` `#E8E5E0`, patch `#B98E7A` |
| Hydra (≈ 51 × 36 × 31 km) | orbits Pluto–Charon in 38.2 days; spins fast (≈ 10.3 h); very bright, almost pure water ice | `#D0CEC9` `#E6E4E0` `#F3F2F0` (geometric 0.83) |

Both orbit the Pluto–Charon barycentre: their periods only match Kepler's law with Charon's mass included
(the data test accounts for it). Gravity is under 0.01 m/s² and escape speed ≈ 20 m/s: a jump will not
leave them, but any small thruster burn will.

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

## Physics QA

`PhysicsQaTest` checks the shipped data and the numbers the game uses against independent references
and writes `build/astro/physics_qa.md` (snapshot below).

| Check | Reference | Tolerance |
|---|---|---|
| Mass, mean radius | NASA planetary / satellite fact sheets | 2 % / 1 % |
| Orbital period | NASA fact sheets (sidereal) | 1 % |
| Solar day | NASA fact sheets | 2 % |
| Mean temperature | NASA fact sheets (global mean) vs our (noon + pre-dawn) / 2 | 15 K with thick air, 60 K airless |
| Surface pressure | NASA fact sheets | 10 % |
| Δv legs | published Δv budgets (trans-lunar 3.13, LOI 0.85, TMI 3.6, MOI 2.1, Earth ascent 9.4, Mars ascent 4.1 km/s …) | per leg, in the report |
| Transfer times, synodic periods | Hohmann textbook values | 2 % / 1 % |

Gameplay Δv (rocketry `Transfers.cost`) = library Δv × realism factor (REALISTIC 1, BALANCED 0.35,
EASY 0.25) − fin/RCS bonuses; the report lists the scaled columns. Mars landings now include a powered
descent after the parachutes (0.65 km/s from low orbit, real landers 0.6–1 km/s).

#### Report snapshot

##### Bodies vs NASA fact sheets (all within tolerance)

| Body | mass | radius | period | day | mean T | pressure |
|---|---|---|---|---|---|---|
| mercury | 3.301e+23 kg | 2439.7 km | 87.97 d | 4222.60 h | 125 °C (ref 167) | 0.000 kPa |
| venus | 4.868e+24 kg | 6051.8 km | 224.7 d | 2802.00 h | 464 °C (ref 464) | 9200 kPa |
| earth | 5.972e+24 kg | 6371.0 km | 365.3 d | 24.00 h | 15 °C (ref 15) | 101.3 kPa |
| moon | 7.342e+22 kg | 1737.4 km | 27.32 d | 708.70 h | -27 °C (ref -20) | 0.000 kPa |
| mars | 6.417e+23 kg | 3389.5 km | 687.0 d | 24.66 h | -50 °C (ref -65) | 0.6360 kPa |
| phobos | 1.066e+16 kg | 11.1 km | 0.3189 d | locked | -58 °C (ref —) | 0.000 kPa |
| deimos | 1.476e+15 kg | 6.3 km | 1.262 d | locked | -75 °C (ref —) | 0.000 kPa |
| ceres | 9.384e+20 kg | 469.7 km | 1682 d | 9.07 h | -91 °C (ref -105) | 0.000 kPa |
| vesta | 2.590e+20 kg | 262.7 km | 1326 d | 5.34 h | -105 °C (ref -100) | 0.000 kPa |
| jupiter | 1.898e+27 kg | 69911.0 km | 4333 d | 9.93 h | -108 °C (ref -110) | 1 bar ref. |
| io | 8.932e+22 kg | 1821.6 km | 1.769 d | 42.46 h | -163 °C (ref -163) | 0.000 kPa |
| europa | 4.800e+22 kg | 1560.8 km | 3.551 d | 85.23 h | -170 °C (ref -171) | 0.000 kPa |
| ganymede | 1.482e+23 kg | 2634.1 km | 7.155 d | 171.71 h | -161 °C (ref -163) | 0.000 kPa |
| callisto | 1.076e+23 kg | 2410.3 km | 16.69 d | 400.54 h | -151 °C (ref -139) | 0.000 kPa |
| saturn | 5.683e+26 kg | 58232.0 km | 1.076e+04 d | 10.66 h | -139 °C (ref -140) | 1 bar ref. |
| titan | 1.345e+23 kg | 2574.7 km | 15.95 d | 382.68 h | -180 °C (ref -180) | 146.7 kPa |
| enceladus | 1.080e+20 kg | 252.1 km | 1.370 d | 32.89 h | -211 °C (ref -201) | 0.000 kPa |
| uranus | 8.681e+25 kg | 25362.0 km | 3.069e+04 d | 17.24 h | -197 °C (ref -195) | 1 bar ref. |
| miranda | 6.590e+19 kg | 235.8 km | 1.413 d | 33.92 h | -200 °C (ref -187) | 0.000 kPa |
| neptune | 1.024e+26 kg | 24622.0 km | 6.019e+04 d | 16.11 h | -201 °C (ref -200) | 1 bar ref. |
| triton | 2.139e+22 kg | 1353.4 km | 5.877 d | 141.04 h | -236 °C (ref -235) | 0.001400 kPa |
| pluto | 1.303e+22 kg | 1188.3 km | 9.056e+04 d | 153.29 h | -228 °C (ref -229) | 0.001000 kPa |
| charon | 1.586e+21 kg | 606.0 km | 6.387 d | 153.29 h | -225 °C (ref -220) | 0.000 kPa |

##### Transfer times and launch windows

| Target | transfer | ref | window every | ref |
|---|---|---|---|---|
| mercury | 105 d | 106 d | 116 d | 116 d |
| venus | 146 d | 146 d | 584 d | 584 d |
| mars | 259 d | 259 d | 780 d | 780 d |
| ceres | 472 d | 472 d | 467 d | 467 d |
| jupiter | 997 d | 997 d | 399 d | 399 d |
| saturn | 6.0 y | 6.0 y | 378 d | 378 d |
| uranus | 16.0 y | 16.0 y | 370 d | 370 d |
| neptune | 30.6 y | 30.6 y | 367 d | 368 d |
| pluto | 45.5 y | 45.5 y | 367 d | 367 d |
| moon | 5 d | ≈ 5 d (Hohmann) | always | — |

At the default orbitalTimeScale (1 real s per simulated hour) a Mars transfer coasts 1.7 real hours and a Mars window comes every 5.2 real hours.

##### Δv vs published budgets (real values; gameplay = real × realism factor − fin/RCS bonuses)

| Leg | library (km/s) | reference | BALANCED ×0.35 | EASY ×0.25 | |
|---|---|---|---|---|---|
| Earth surface → LEO | 9.02 | 9.40 ± 0.60 | 3.16 | 2.26 | ok |
| LEO → Earth surface (deorbit) | 0.06 | 0.10 ± 0.08 | 0.02 | 0.02 | ok |
| LEO → escape | 3.22 | 3.22 ± 0.06 | 1.13 | 0.81 | ok |
| Trans-lunar injection | 3.13 | 3.13 ± 0.10 | 1.10 | 0.78 | ok |
| Lunar orbit insertion | 0.82 | 0.85 ± 0.10 | 0.29 | 0.21 | ok |
| Low lunar orbit → Moon surface | 2.00 | 1.87 ± 0.20 | 0.70 | 0.50 | ok |
| Moon surface → low lunar orbit | 2.00 | 1.87 ± 0.20 | 0.70 | 0.50 | ok |
| Trans-Mars injection | 3.61 | 3.60 ± 0.12 | 1.26 | 0.90 | ok |
| Mars orbit insertion (propulsive) | 2.10 | 2.10 ± 0.20 | 0.74 | 0.53 | ok |
| Mars orbit insertion (aerobraking) | 0.02 | 0.10 ± 0.10 | 0.01 | 0.00 | ok |
| Low Mars orbit → surface (aero + powered) | 0.65 | 0.75 ± 0.30 | 0.23 | 0.16 | ok |
| Mars surface → low Mars orbit | 4.01 | 4.10 ± 0.30 | 1.40 | 1.00 | ok |
| Trans-Venus injection | 3.50 | 3.50 ± 0.15 | 1.23 | 0.88 | ok |
| Venus orbit insertion (propulsive) | 3.32 | 3.30 ± 0.30 | 1.16 | 0.83 | ok |
| Trans-Mercury injection | 5.55 | 5.50 ± 0.30 | 1.94 | 1.39 | ok |
| Mercury orbit insertion | 7.54 | 7.50 ± 0.60 | 2.64 | 1.88 | ok |
| Trans-Jupiter injection | 6.30 | 6.30 ± 0.20 | 2.21 | 1.58 | ok |
| Low Jupiter orbit insertion | 17.70 | 17.20 ± 1.00 | 6.19 | 4.42 | ok |
| Phobos surface → orbit | 0.01 | 0.01 ± 0.01 | 0.00 | 0.00 | ok |


## Body look for model rendering (`BodyLook`)

`BodyLook.of(sys, body, pole, axialTiltDeg, observer, t)` or `BodyAdapter.look(access | sys, id, ...)` returns what
a renderer needs to draw a body as a model:

| Field | Meaning |
|---|---|
| `spinAxis` | right-hand spin axis (ecliptic unit vector) |
| `rotationAngle` | prime meridian angle about the axis, from the axis's ascending node on the ecliptic |
| `siderealRotationSeconds` | one turn against the stars, from the data's solar day and the orbital period (Earth 23.93 h, Venus 243 d backwards) |
| `retrograde` | spins backwards (axis more than 90° from the orbit normal) |
| `axialTiltDeg` | angle between the spin axis and the orbit normal |
| `sunDirection`, `viewDirection` | from the body to the Sun and to the observer |
| `phaseAngleRad`, `illuminatedFraction` | Sun–body–observer angle and the lit fraction (1 + cos α)/2 |

Helpers:
- `toBodyFixed` / `toEcliptic` convert between frames; the body-fixed frame has x on the prime meridian and z
  on the spin axis.
- `sunInBodyFrame` and `viewInBodyFrame` give the light direction and the visible face.

The spin axis comes from the data's pole. Without one, it comes from `KNOWN_POLES` (IAU 2015 values for
Mercury, Venus, Earth and the Moon; they are not in the data, so the Moon's orbit stays on the ecliptic).
Otherwise it is the orbit normal tilted by `axial_tilt_deg` about the line of nodes. A tidally locked body
turns its prime meridian to its parent. `BodyLookTest` checks:
- every planet's tilt and spin direction;
- the sidereal periods of Mercury, Venus, Earth and Mars;
- the Moon's full and new phases on real 2022 dates, and its near side facing Earth;
- Jupiter's phase angle staying under 12.5°;
- the Sun crossing Uranus's equator in 2007 and standing over a pole in 2028.

Keep one `BodySystem` per frame and use `BodyAdapter.look(sys, id, body, observer, t)`, because building the
system each call is wasteful.

## Sky ephemeris (`SkyEphemeris`)

What you see in the sky from any body, for the sky renderer (`client/sky`) and telescopes.

```java
BodySystem sys = BodyAdapter.system(level.registryAccess()).orElseThrow();   // cache it per registry
double t = BodyAdapter.simTime(level);              // game time → simulated s since J2000 (orbitalTimeScale)
List<SkyEphemeris.SkyObject> sky = SkyEphemeris.visible(sys, "spacebreaker:mars", t, 6.0);  // brightest first
for (SkyEphemeris.SkyObject o : sky) {
    o.direction();          // unit vector, system frame (ecliptic J2000), from the observer's centre
    o.apparentMagnitude();  // Sun from Earth −26.7, full Moon −12.7, Venus −4.4, Jupiter −2.9, naked eye ≤ 6
    o.angularDiameter();    // rad — draw a disc when it is larger than a pixel (Jupiter from Io: 19°)
    o.illuminated();        // lit fraction (phases), o.phaseAngle(), o.elongation() from the star
    o.colorRgb();           // Earth blue-white, Mars rust, Venus cream, ice giants cyan, stars black-body
}
SkyEphemeris.Horizontal h = SkyEphemeris.toHorizontal(o.direction(), axialTiltRad, rotationAngleRad, latitudeRad);
```

`toHorizontal` uses the observer body's axial tilt (about the x axis), how far it has turned
(local sidereal angle: 0 when the meridian faces +x) and the latitude (z axis in the world). Magnitudes:
m = V(1, α) + 5 log₁₀(r·Δ/AU²). Mercury, Venus, Earth, Mars and Jupiter use their measured phase curves
(Mallama & Hilton 2018), so Venus stays near −4 even as a thin crescent and −3.3 at inferior conjunction;
other bodies use H from diameter and geometric albedo (optional body field `geometric_albedo`, NASA values;
Bond albedo when missing) with a Lambert phase law. Saturn's rings are not included. `SkyEphemerisTest` checks the Sun, the full Moon, Venus over a whole synodic cycle (superior conjunction
−3.9, greatest elongation 24 Mar 2020: 46°, −4.4, greatest brilliancy −4.7, a 12° crescent brighter than −3.5), Jupiter (3 Nov 2023: −2.9, 49.5″), Mars (13 Oct 2020: −2.6, 22″) and Saturn
at opposition, Earth from Mars (≈ −2.5) and the Galilean moons from Io.

## Asteroids and small bodies (`core/astro/smallbody`)

```java
Asteroid a = AsteroidGenerator.generate(worldSeed, Region.MAIN_BELT, index);   // same seed + index = same rock
List<Asteroid> belt = AsteroidGenerator.population(worldSeed, Region.MAIN_BELT, 1000);
a.type();            // SpectralType C / S / M / V / D
a.diameterKm(); a.shape(); a.rubblePile(); a.density(); a.mass(); a.rotationHours();
a.surfaceGravity(); a.escapeVelocity(); a.canJumpOff();   // a 1 km rock: v_esc ≈ 0.5 m/s
a.composition(); a.metalVeins(); a.pgmPpm();               // mining value
AsteroidTerrain t = a.terrain();   // hints + TerrainParams + rock/regolith, boulders, metal veins, ice
CelestialBody c = BodyAdapter.toCelestialBody(a.toAstroBody(), a.details(), Optional.empty());
```

| Model | Rule |
|---|---|
| Regions | `MAIN_BELT` 2.1–3.3 AU, `TROJANS` at Jupiter's L4/L5 (±60° in mean longitude, ±12° libration), `KUIPER_BELT` 39–48 AU |
| Sizes | cumulative power law N(> D) ∝ D^−2.5 between the region's limits (main belt 50 m – 100 km; the real large bodies are data files) |
| Types | inner belt S-rich (55 % S, 25 % C), outer belt C-rich (75 % C), 7 % M everywhere, V (Vesta family) in the inner belt, D beyond Jupiter |
| Orbits | no main-belt body inside a Kirkwood gap (3:1 at 2.50, 5:2 at 2.82, 7:3 at 2.96, 2:1 at 3.28 AU); Rayleigh eccentricities and inclinations |
| Structure | above 200 m most bodies are rubble piles (porosity 30–50 %, spin no faster than the 2.2 h barrier); small monoliths can spin in minutes; triaxial shapes, small bodies more elongated |

| Type | Grain density | Albedo | Main materials | Metal veins | PGM |
|---|---|---|---|---|---|
| C carbonaceous | 2400 kg/m³ | 0.03–0.09 | clays, water, organics, magnetite | 0.1 | ≈ 0.8 ppm |
| S stony | 3400 | 0.15–0.30 | olivine, pyroxene, iron–nickel 15 % | 0.3 | ≈ 1.5 ppm |
| M metallic | 7000 | 0.10–0.20 | iron–nickel 88 %, sulfides | 0.9 | ≈ 25 ppm |
| V basaltic | 3300 | 0.30–0.45 | pyroxene, plagioclase | 0.05 | ≈ 0.2 ppm |
| D primitive icy | 1900 | 0.03–0.10 | water ice, silicates, organics | 0.05 | ≈ 0.5 ppm |

Bulk density = grain density × (1 − porosity): rubble-pile C ≈ 1.45 g/cm³ (Bennu and Ryugu 1.19),
S ≈ 2.0 (Itokawa 1.9, Eros 2.7). `AsteroidGeneratorTest` checks all of the above.

### Comets (`Comet`, `CometActivity`)
`Comet.CATALOGUE` holds five real comets (four periodic, plus the long-period Hale–Bopp) with J2000 elements and a perihelion time
(M₀ at J2000 follows from it). `CometActivity.at(comet, t)` gives the outgassing at simulated time t:

| Quantity | Model |
|---|---|
| Water production | Q = Z₁ · A_active · g(r), Z₁ = 4·10²¹ m⁻² s⁻¹ (sunlight at 1 AU over the latent heat of water, rotating-nucleus average) |
| g(r) | Marsden water-ice law α (r/r₀)^−2.15 (1 + (r/r₀)^5.093)^−4.6142, r₀ = 2.808 AU, g(1 AU) = 1: ≈ r^−2.15 inside 1 AU, collapses beyond 3 AU |
| Dust | dust-to-gas mass ratio per comet |
| Coma | Haser profile n(ρ) = Q/(4πvρ²)·e^(−ρ/L), edge at 10⁸ m⁻³; v = 0.85 km/s · r^−½, L = v · 8.3·10⁴ s · r² |
| Tail | empirical 2·10⁶ km · √(Q/10²⁸) / r, along the Sun → comet direction |
| Level | DORMANT < 10²⁴ s⁻¹ ≤ WEAK < 10²⁷ ≤ ACTIVE < 10²⁹ ≤ GREAT (bilingual names) |

Active fractions are calibrated to observed water production:

| Comet | a (AU) | e | q (AU) | P (y) | R (km) | ρ (kg/m³) | v_esc (m/s) | Q at q (s⁻¹) | switches on | water lost / orbit | erosion / orbit |
|---|---|---|---|---|---|---|---|---|---|---|---|
| 67P/Churyumov–Gerasimenko | 3.463 | 0.641 | 1.243 | 6.44 | 1.65 | 530 | 0.9 | 1.6·10²⁸ (Rosetta 1–3·10²⁸) | 3.7 AU | 7.7·10⁹ kg (Rosetta ≈ 6·10⁹) | 1.1 m |
| 1P/Halley (retrograde) | 17.83 | 0.967 | 0.586 | 75.3 | 4.4 | 617 | 2.6 | 9.4·10²⁹ at q (Giotto ≈ 7·10²⁹ at 0.89 AU) | 4.1 AU | 1.7·10¹¹ kg | 2.3 m |
| 2P/Encke | 2.215 | 0.848 | 0.336 | 3.30 | 2.4 | 604 | 1.4 | 9.3·10²⁷ | 3.1 AU | 9.2·10⁸ kg | 0.1 m |
| C/1995 O1 (Hale–Bopp) | 185.8 | 0.995 | 0.914 | 2533 | 30 | 504 | 15.9 | 1.0·10³¹ (observed ≈ 10³¹) | 4.7 AU ¹ | 2.8·10¹² kg | 3.0 m |
| 9P/Tempel 1 | 3.122 | 0.518 | 1.506 | 5.52 | 2.83 | 474 | 1.5 | 7.8·10²⁷ (Deep Impact ≈ 10²⁸) | 3.7 AU | 4.5·10⁹ kg | 0.2 m |

¹ the real Hale–Bopp was already active at 7 AU, driven by CO ice; the model only sublimates water.

For the game, a comet nucleus is like the near-Earth asteroids (escape speed about 1 m/s) but its
surface changes along the orbit. Beyond about 3.5 AU it is dormant: dark, cold and dusty. Near the
Sun it has a coma tens of thousands of km wide and a tail of 10⁶–10⁷ km pointing away from the Sun,
and its surface vents gas and dust. `Activity` can drive sky rendering, and `level()` can switch
surface hazards on and off. The three comets are also `celestial_body` data (`churyumov_gerasimenko`,
`halley`, `encke`; type `comet`, the same ids as `Comet.id()`). A comet gets a `dimension` only when its
world exists: 67P, Encke, Halley and Tempel 1 have one (`spacebreaker:<id>`); Hale–Bopp is sky and map only. The transfer table of the Δv map skips them, because
a coplanar Hohmann estimate means nothing at Halley's 162° inclination. `CometActivityTest` checks orbits, perihelion timing, production
against Rosetta, Giotto and Encke values, mass loss per orbit, the coma and the tail.

**Brightness.** `CometActivity.apparentMagnitude(comet, cometPos, observerPos)` is the brighter of two
magnitudes:
- the coma, m_H + 5 log Δ, where m_H comes from the water production through Jorda et al. (2008),
  log₁₀ Q = 30.675 − 0.2453 m_H;
- the bare nucleus, H + 5 log(rΔ), with H = 5 log₁₀(1329 km / (D √p)) and no phase effects.

`nakedEye(m)` means m ≤ 6. Against real apparitions from Earth (`CometMagnitudeTest`):

| Apparition | Model | Observed |
|---|---|---|
| Hale–Bopp at perihelion, 1997 | −0.7 | about −0.8 |
| 67P at perihelion, 2015 | 11.3 | 11–12 |
| Halley, March 1986 | 4.5 | 2.5–4 |
| Halley in 2000, 25 AU | 28 | bare nucleus |

### Comet rendezvous (`Lambert`, `CometRendezvous`)
`TransferPlanner.plan` treats every target as a coplanar circular orbit at its semi-major axis. That is
meaningless for comets. Hale–Bopp would take 451 years, and Halley's retrograde motion is ignored.
`CometRendezvous.estimate(sys, comet, point, after)` instead solves Lambert arcs in 3-D. It starts
from the real Earth, from low Earth orbit, and flies to the comet near its next perihelion or aphelion
after `after` (arrival within ±60 days). It searches the flight time and both transfer directions for
the cheapest total. The cost is the departure burn plus a full velocity match on arrival, so
inclination and retrograde motion are paid in full. There are no gravity assists. `best(...)` returns
the cheaper end. Values from the first passages after J2000 (`CometRendezvousTest`):

| Comet | Arrive near | Arrival (AU) | Departure from LEO | Velocity match | Total | Flight |
|---|---|---:|---:|---:|---:|---:|
| 67P/Churyumov–Gerasimenko | perihelion | 1.28 | 7.8 km/s | 11.3 km/s | 19.1 km/s | 0.55 y |
| 67P/Churyumov–Gerasimenko | aphelion | 5.67 | 7.2 km/s | 0.7 km/s | 7.9 km/s | 2.94 y |
| 1P/Halley | perihelion | 1.32 | 4.0 km/s | 52.5 km/s | 56.5 km/s | 1.08 y |
| 1P/Halley | aphelion | 35.08 | 17.0 km/s | 2.2 km/s | 19.2 km/s | 45.74 y |
| 2P/Encke | perihelion | 0.88 | 19.5 km/s | 5.9 km/s | 25.4 km/s | 1.04 y |
| 2P/Encke | aphelion | 4.07 | 6.5 km/s | 3.6 km/s | 10.1 km/s | 2.37 y |
| C/1995 O1 (Hale–Bopp) | perihelion | 0.92 | 32.2 km/s | 13.5 km/s | 45.7 km/s | 0.36 y |
| C/1995 O1 (Hale–Bopp) | aphelion | 370.69 | 31.4 km/s | 0.2 km/s | 31.5 km/s | 1322.48 y |
| 9P/Tempel 1 | perihelion | 1.51 | 4.6 km/s | 8.7 km/s | 13.2 km/s | 0.65 y |
| 9P/Tempel 1 | aphelion | 4.73 | 6.3 km/s | 2.8 km/s | 9.0 km/s | 2.98 y |

Reading: near perihelion a comet moves fast, so the velocity match dominates; near aphelion it is
slow, but the trip is long. 67P and Tempel 1 are reachable with chemical rockets (8–9 km/s near
aphelion, about 3 years). Halley costs about 56 km/s near perihelion (retrograde) or 19 km/s and
46 years near aphelion, and Hale–Bopp about 31 km/s in either case. Rosetta reached 67P with four
gravity assists in 10 years, so assists (`GravityAssist`) would lower these figures.

For the map, `CometRendezvous.Table.compute(sys, after, passages)` precomputes every catalogue comet's
next `passages` perihelion and aphelion options, plus the cheapest anywhere-on-orbit option within 5, 10, 20
and 40 years (`TABLE_FLIGHT_YEARS`). Two passages take about 4 s; build the table once,
off the main thread, and keep it, since it is immutable. Queries take a budget (total Δv in m/s,
flight time in years):
- `of(id)`: every option, in arrival order.
- `upcoming(id, now)`: the options whose departure is still ahead.
- `cheapest(id, now, maxDv, maxYears)` and `soonest(...)`: the best option within the budget.
- `reachable(...)`: whether any option fits. False means "observation only" for that budget.

**Anywhere along the orbit.** `cheapestWithin(sys, comet, after, maxYears)` (also `Point.ANYWHERE`, and the table's
anywhere options) drops the perihelion/aphelion pin. It searches arrivals along the next
orbit (at most 80 years ahead) with departure after `after` and flight time at most `maxYears`
(capped at 40), so a longer allowance never costs more. The cheapest from J2000:

| Comet | ≤ 5 y | ≤ 10 y | ≤ 20 y |
|---|---:|---:|---:|
| 67P/Churyumov–Gerasimenko | 7.9 km/s, 2.8 y, 5.6 AU | 7.9 km/s, 2.8 y, 5.6 AU | 7.9 km/s, 2.8 y, 5.6 AU |
| 1P/Halley | 20.5 km/s, 4.5 y, 14.0 AU | 19.5 km/s, 8.3 y, 19.8 AU | 19.1 km/s, 14.4 y, 27.0 AU |
| 2P/Encke | 9.7 km/s, 1.6 y, 4.0 AU | 9.7 km/s, 1.6 y, 4.0 AU | 9.7 km/s, 1.6 y, 4.0 AU |
| C/1995 O1 (Hale–Bopp) | 49.0 km/s, 4.8 y, 21.1 AU | 43.4 km/s, 9.8 y, 29.5 AU | 38.3 km/s, 18.3 y, 41.2 AU |
| 9P/Tempel 1 | 8.1 km/s, 2.4 y, 4.7 AU | 8.1 km/s, 2.4 y, 4.7 AU | 8.1 km/s, 2.4 y, 4.7 AU |

Far out, a comet is slow enough that even Halley's retrograde motion costs about 19.5 km/s within 10 years.
That is beyond chemical stages but within a nuclear thermal stack (Isp 880 s). Hale–Bopp stays near
40 km/s. `Table.access(id, now, chemicalDv, nuclearDv, maxYears)` turns this into the map label
`Access.CHEMICAL` / `NUCLEAR` ("Needs a nuclear engine" / "Nükleer motor gerekir") / `OBSERVATION_ONLY`
("Yalnız gözlem"). With 15 km/s chemical, 30 km/s nuclear and 10 years, 67P, Encke and Tempel 1 are
chemical, Halley is nuclear and Hale–Bopp is observation only. The actual limits come from rocketry's
designs (QA's progression model). Long flights still need rocketry's boil-off model.

**Going home.** `returnTrip(sys, comet, after, maxYears, maxEntrySpeed)` returns the cheapest way from the
comet onto a Lambert arc that ends in Earth's atmosphere. It is a burn at the comet plus, if the arrival is
faster than `maxEntrySpeed`, a braking burn just above the atmosphere (`Return.brakeDv`). There is no orbit
capture at Earth. Departure is at or after `after` (the rendezvous arrival plus any stay). The search is a
grid followed by a multi-start pattern refinement and takes about 1 s per call. Below, each row takes the
cheapest rendezvous within 10 years, then the cheapest return, with entry at most 15 km/s (Stardust
re-entered at 12.9 km/s):

| Comet | Rendezvous (≤ 10 y) | Return ≤ 10 y | Return ≤ 40 y | Round trip (≤ 10 y + ≤ 40 y) |
|---|---:|---:|---:|---:|
| 67P/Churyumov–Gerasimenko | 7.9 km/s, 2.8 y | 0.8 km/s (0.8 + 0.0 brake), 3.2 y from 5.7 AU | 0.8 km/s (0.8 + 0.0 brake), 3.2 y from 5.7 AU | 8.6 km/s |
| 1P/Halley | 19.5 km/s, 8.3 y | 10.7 km/s (3.6 + 7.2 brake), 9.0 y from 20.6 AU | 10.7 km/s (3.6 + 7.2 brake), 9.0 y from 20.6 AU | 30.2 km/s |
| 2P/Encke | 9.7 km/s, 1.6 y | 3.4 km/s (3.4 + 0.0 brake), 2.0 y from 4.1 AU | 3.4 km/s (3.4 + 0.0 brake), 2.0 y from 4.1 AU | 13.2 km/s |
| C/1995 O1 (Hale–Bopp) | 43.4 km/s, 9.8 y | 48.7 km/s (18.4 + 30.2 brake), 10.0 y from 30.5 AU | 36.1 km/s (5.9 + 30.2 brake), 40.0 y from 29.6 AU | 79.4 km/s |
| 9P/Tempel 1 | 8.1 km/s, 2.4 y | 1.6 km/s (1.6 + 0.0 brake), 2.2 y from 4.7 AU | 1.6 km/s (1.6 + 0.0 brake), 2.2 y from 4.7 AU | 9.7 km/s |

Leaving a comet from far out is cheap, because it is slow there: 67P, Tempel 1 and Encke return for 1–3 km/s.
For a retrograde comet the hard part is the arrival. Riding Halley home costs almost nothing but enters at
≈ 68 km/s, so the best return is a 3.6 km/s burn far out plus a 7.2 km/s brake: about 30 km/s for the
whole round trip. Hale–Bopp is hopeless at about 80 km/s.


## Gravity assists (`GravityAssist`, `TransferPlanner.planWithAssists`)

`TransferPlanner.planWithAssists(sys, from, to, aerobrake)` returns the cheaper of the direct Hohmann
plan and a gravity-assist tour; `plan(...)` itself is unchanged. Tours exist for the inner Solar System
(both ends inside 1.6 AU, a planet orbiting between them): a flyby of that planet lowers the perihelion
to the target's orbit (a deep-space burn pumps its v∞ first if needed), then resonant flybys of the
target with small burns cut the arrival speed to ≈ 30 % (at least 1.5 km/s) before capture. Rule of
thumb: a deep-space burn changes v∞ by 2.5× its Δv (v∞ leveraging). Legs: `departure`, `flyby_pumping`,
`target_flybys`, `capture`; `synodicPeriod`/`phaseAngle` are those of the first leg.

| Route | Direct | With assists | Time |
|---|---|---|---|
| LEO → low Mercury orbit | 13.09 km/s | 8.31 km/s (Venus, 4 Mercury flybys) | 105 d → 3.8 y |
| Low Mercury orbit → LEO (aerocapture) | 7.57 km/s | 4.85 km/s | 3.8 y |
| Low Mars orbit → low Mercury orbit | 16.90 km/s | 8.75 km/s (Earth, 6 flybys) | 5.9 y |

MESSENGER needed ≈ 7 km/s from launch to its (elliptical) Mercury orbit, so the numbers are of the right
size. `GravityAssistTest` checks them.

### Surface legs with timing (`TransferPlanner.ascentLeg` / `descentLeg`)

For shuttles between a low orbit and the surface (e.g. the Moon lander): `ascentLeg(body, twr)` and
`descentLeg(body, useAtmosphere, twr)` return Δv (the same as `ascent`/`descent`), powered `burnSeconds`
and unpowered `coastSeconds`. The burn assumes the average thrust-to-weight is 1.3× the lift-off value;
the descent coasts half an orbit from the deorbit burn down (Moon ≈ 56 min) and adds 10 min of entry and
parachutes where there is air. Moon at TWR 2: ascent ≈ 1.9 km/s in ≈ 7.5 min, descent ≈ 56 min coast +
≈ 7.5 min powered — close to Apollo's lunar module. Times are simulated seconds (`GameClock` maps them to
game ticks).

### Real neighbourhood and the galaxy picture

Inside `NearbyStars.CATALOGUE_RADIUS_LY` (25 ly) the galaxy contains only catalogued real stars; generated
stars begin outside it. Each `StarSystem.Details` says whether a body is `discovered` (a confirmed, real
planet) or a generator fill-in for a world nobody has found yet — show the latter as "not yet discovered".
`GalaxyMap.overview` now draws two major and two minor spiral arms (Scutum–Centaurus and Perseus brightest)
weighted by the light of young stars, around a bright bar and bulge.

### System cards for the star map (`galaxy.SystemCard`)

`SystemCard.of(galaxy, starSeed)` is everything the "nearby stars" view needs about one system: names,
designation, distance, star summary, `binary`, `knownPlanets` (confirmed — show this count),
`undiscoveredPlanets` (generator fill-ins), the habitable zone and a `PlanetCard` per planet (innermost
first: kind, discovered, mass and radius in Earth units, a, period, equilibrium temperature, habitable-zone
flag, landable, moons, rings, colour family). `BodyAdapter.toCelestialBodies(system, dims)` turns any system
into `CelestialBody` records without registering them; a GameTest converts every catalogued system and
round-trips it through the data-pack codec.

## Eclipses in orbit (`Eclipses`)

For solar arrays in orbit dimensions and satellites:

```java
Eclipses.Sunlight s = Eclipses.station(sys, "spacebreaker:earth", 400_000, BodyAdapter.simTime(level));
s.sunlit();           // 0 in the umbra .. 1 in full sun (smooth across the penumbra)
s.irradiance();       // W/m² on a panel facing the star, already × sunlit (Earth ≈ 1361, Mars 490–720)
s.eclipseFraction();  // share of every orbit in shadow (400 km: 0.39 ≈ 36 of 92 minutes)
s.orbitPeriod(); s.phase(); s.inShadow();
Eclipses.eclipseFraction(planetRadius, orbitRadius, beta);   // orbit average for any β angle
Eclipses.sunlitFraction(pos, starPos, planetRadius, starRadius);   // any point, with penumbra
```

The station orbit lies in the system plane (β ≈ 0: the worst case, longest nights) unless an inclination is
given, and starts on the day side at J2000. Checked: ISS-like orbit 36 min of darkness, geostationary
≈ 70 min at equinox and none when β > 8.7°, low lunar orbit ≈ 47 of 118 minutes. For an energy average
over time, multiply peak output by `1 − eclipseFraction()`.

## Polar light and cold traps (`PolarIllumination`)

```java
PolarIllumination p = BodyAdapter.polarIllumination(level.registryAccess(), "spacebreaker:moon").orElseThrow();
p.psrLatitudeDeg();    // permanently shadowed crater floors from here to the pole (Moon ≈ 80.5°, 90 = none)
p.psrAreaFraction();   // share of the surface in permanent shadow (Moon ≈ 0.08 %)
p.coldTrapTempK();     // floor temperature (Moon ≈ 40 K, Mercury ≈ 60 K)
p.iceStable();         // water ice survives there (airless and below 110 K)
p.maxPoleSunElevationDeg(); p.effectiveObliquityDeg(); p.polarDayNight();
PolarIllumination.permanentlyShadowed(latitude, axialTilt, depthRatio, sunRadiusDeg);   // one crater
```

Model: at a pole the star never climbs higher than the (folded) axial tilt; a crater floor stays dark when
its rim angle atan(2·depth/diameter) — 11.3° for a typical degraded polar crater (depth/diameter 0.1) —
is higher than the star's top elevation plus its radius. Floors are warmed only by scattered light
(10⁻⁴ of the starlight) and internal heat (0.02 W/m²). Results: Moon, Mercury and Ceres keep ice in polar
cold traps (as LRO, MESSENGER and Dawn found); Mars, Earth and Pluto (large tilts) and bodies with air have
none. For the planet engine: use `psrLatitudeDeg()` as the latitude above which deep crater floors hold ice.

## Free-flight core (`core/astro/flight`)

These are the pure-maths parts of free spaceflight. Gameplay (the craft entity, the scenes, transitions) is
built on them.

- `SceneScale(centralRadius, radiusBlocks, compression)` maps real metres to blocks around a scene's central
  body. Inside the body the scale is linear; outside it is logarithmic, b = Rb·(1 + k·ln(d/R)). Directions
  are kept and the mapping is exactly invertible. `EARTH_MOON` draws Earth at 128 blocks. Low orbit is about
  15 blocks up and the Moon about 2,200 blocks out. `blocksPerMetre(d)` gives the local scale for speeds;
  `displayRadius(r, d, min)` keeps far bodies visible.
- `Gravity.acceleration(sys, center, bodies, rel, t)` is real-scale gravity in the central body's
  non-rotating frame. Third-body pulls are taken as perturbations, so the frame's own acceleration cancels.
  `Gravity.pull` gives a single point mass.
- `FlightState(position, velocity, mass)` steps with velocity Verlet (symplectic, so orbits do not drift),
  with constant thrust and mass flow; `advance` adds substeps for long ticks. `assist(v, reference, maxDv)`
  is the optional flight assist that damps the relative velocity.
- `FlightReadout` holds what a cockpit display shows as plain, server-safe data: target id, relative speed (+ when
  receding), distance or altitude, Δv left and full, assist, predicted path and next manoeuvre (NaN when none).
  The HUD (client/hud, suit) turns it into text.
- `OrbitPredictor.predict(sys, body, state, t, horizon, step, maxSegments)` predicts patched conics for the
  HUD. Each segment ends in an impact, an exit from the current sphere of influence or an entry into a
  moon's, and the next conic continues in the new body's frame. `soi(sys, body)` gives a sphere of influence.

- `ManeuverNode(time, prograde, normal, radial)` is a planned burn in the local orbital frame: `deltaV(state)`,
  `apply`, `burnSeconds(mass, thrust, isp)` and `startTime` (the burn centred on the node).
- `EntryGate` gives a body's layers: `zone(body, altitude)` returns SPACE, ORBIT_SHELL (below twice the
  low-orbit altitude), ENTRY (below the top of the air, or half the low-orbit altitude on airless worlds) or
  SURFACE. `descendsThroughEntry(body, state)` is the hand-over test to the surface world.
- `SurfacePoint(lat, lon, altitude)` converts to and from a position over a turning body through `BodyLook`.
  `mapXZ(blocksPerDegree)` maps it onto a surface world (x east, z south).

`FlightCoreTest` checks:
- scale round trips and the Earth–Moon distances in blocks;
- Earth-frame gravity within 1e-5 of μ/r² in low orbit, with the Moon dominating near the Moon;
- 100 low-Earth orbits at 10 s steps staying within 2 km;
- the Δv of a burn matching the rocket equation to 1e-6;
- a 3.13 km/s translunar injection entering the Moon's sphere of influence (66,000 km) within 1.5–6 days.
