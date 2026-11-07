---
title: Space Station
icon: spacebreaker:docking_port
items: [spacebreaker:station_kit, spacebreaker:habitat_kit, spacebreaker:station_hull, spacebreaker:station_window, spacebreaker:docking_port, spacebreaker:station_bunk, spacebreaker:fuel_depot, spacebreaker:solar_array, spacebreaker:crew_capsule_large, spacebreaker:cargo_module, spacebreaker:station_core_module, spacebreaker:docking_module, spacebreaker:habitation_module, spacebreaker:airlock_module, spacebreaker:solar_wings, spacebreaker:station_hatch]
research: spacebreaker:space_stations
---
A home, workshop and fuel depot in orbit. You no longer have to fuel your rocket from the ground every time.

{items spacebreaker:station_kit spacebreaker:station_hull spacebreaker:station_window spacebreaker:docking_port}

## Building it
1. Take a {item spacebreaker:station_kit} to orbit and use it in space: a **5×5** hull floor appears.
2. Build walls and a roof with {item spacebreaker:station_hull} and {item spacebreaker:station_window}.
3. Put an {item spacebreaker:oxygen_distributor} inside: the closed room is pressurised and you can take off your helmet. → [Air for the Base](base_air)
4. Fit a {item spacebreaker:docking_port} on the outside.

## Station modules {#modules}
{items spacebreaker:station_core_module spacebreaker:docking_module}
Ready-made modules join through standard **mouths**: a 3 × 3 opening at walking height, closed by nine {item spacebreaker:station_hatch} blocks (the middle one is marked) while nothing is attached.
- **{item spacebreaker:station_core_module}** (7 × 7 × 7): the heart of a station, with the {item spacebreaker:station_core} console, an oxygen distributor, roof lights, a docking port on the roof and four side mouths, each with a window above it.
- **{item spacebreaker:docking_module}** (5 × 5 × 7): a short tunnel with a docking port at its far end.

**By hand:** use a module on a mouth's hatch. It is built against the wall and both walls open into a short passage, so the two modules share one room of air. A core used in open space in orbit starts a new station ("A station core can only be unfolded in orbit" anywhere else). While you hold a module and look at a mouth, an outline shows where it goes: green when there is room, red when something is in the way.

**By rocket:** modules fit the capsule slot. A module rocket docks at the station in its orbit and leaves the module waiting in the core console; open the console to take it. With no station within 512 blocks, a core module unfolds a new station ahead of the rocket, which docks on its roof.
{recipe spacebreaker:rocketry/station_core_module}
{recipe spacebreaker:rocketry/docking_module}

### Living, airlock and power {#stage2}
{items spacebreaker:habitation_module spacebreaker:airlock_module spacebreaker:solar_wings}
- **{item spacebreaker:habitation_module}** (7 × 5 × 9): living quarters with three bunks in sleeping pods, two barrel lockers, two window bands, roof lights and a sealed mouth at the far end so the station can grow on.
- **{item spacebreaker:airlock_module}** (5 × 5 × 5): an airlock door into the station, an airlock door to space and a small chamber between them. Close the inner door before you open the outer one, and the station keeps its air while you step out.
- **{item spacebreaker:solar_wings}**: built outside the hull, so the mouth stays sealed; its middle hatch becomes a battery bank feeding the station. A boom of grate and copper cable runs out from a rotary joint and carries **four solar arrays**, two on each side, with an antenna at the tip.

They are attached and delivered like the core and the docking module.
{recipe spacebreaker:rocketry/habitation_module}
{recipe spacebreaker:rocketry/airlock_module}
{recipe spacebreaker:rocketry/solar_wings}

## Habitat module kit
{items spacebreaker:habitat_kit}
If you would rather not build the walls by hand, use a {item spacebreaker:habitat_kit} in orbit. A sealed **7×5×7** room unfolds in front of you:
- hull walls with a band of windows and an airlock door facing you,
- a {item spacebreaker:docking_port} on the far wall,
- an {item spacebreaker:oxygen_distributor} in the middle and a {item spacebreaker:station_bunk} in a corner,
- a solar array on the roof, a {item spacebreaker:battery_bank} set into the ceiling under it and a cable end hanging from the ceiling into the room: plug the room's machines in there.

> Slot an oxygen tank into the distributor or pipe oxygen in, and the room fills with air. The kit does not unfold if anything is in the way.

{page}
## Bunk
{items spacebreaker:station_bunk}
Beds do not work off Earth. Use a {item spacebreaker:station_bunk} and the top of it becomes your **respawn point**: on a station, on the Moon or on any world.

## Fuel depot
{items spacebreaker:fuel_depot}
The {item spacebreaker:fuel_depot} stores fuel and liquid oxygen and pumps them into rockets docked at any docking port within **8 blocks**. Pumping costs **40 FE/t**; feed it from solar panels through conduits.

> Carry fuel to the station in a tank: tanks keep their contents when broken. → [Cryogenics](cryogenics)

## Solar array
{items spacebreaker:solar_array}
The {item spacebreaker:solar_array} carries three panels on a one-block footprint and makes up to **60 FE/t**. Orbit has nights too: each orbit lasts about **12000 ticks** and close to a third of it is spent in the planet's shadow. In shadow, panels and arrays make **nothing**: add a {item spacebreaker:battery_bank} for the night. → [Night and power](power#night)

## Docking
- A crewed rocket arriving in an orbit with a station starts **36 blocks** out from the nearest docking port. It closes in quickly, then creeps the last stretch on thruster puffs and locks on with a clunk: **"Docked"**. Nobody can step off on the way in.
- The crew of a rocket in orbit within **12 blocks** of the port can step off into the station and back on.
- Fuel and liquid oxygen piped into the port flow into the docked rocket: the station becomes a **fuel depot**.

## Cargo rocket
{items spacebreaker:cargo_module}
You do not have to carry supplies yourself. In the rocket assembler, fit a {item spacebreaker:cargo_module} in place of the capsule: it is a **27-slot** hold and the rocket flies uncrewed.
- **Orbit target:** it docks at the station. **Surface target:** it lands on the nearest {item spacebreaker:landing_beacon}.
- On arrival it unloads the hold into the **chests and shulker boxes** within **4 blocks** of the port or beacon, nearest first. It never puts anything into machines.
- **Barrels are the outbox:** whatever you put in them is loaded for the next trip.
{recipe spacebreaker:rocketry/cargo_module}

### Automatic route
A cargo rocket launched to a target other than Earth's surface keeps shuttling between where it took off and its target (a Moon base ↔ a station in Moon orbit, for example). At each end it unloads into the chests → loads from the barrels → waits **200 ticks** (the fuel depot refuels it) → flies to the other end. With an empty hold or not enough fuel it waits, and tells you once.

> The launch console's last line shows the route: the next stop, how full the hold is and whether the rocket is loading, waiting for cargo or fuel, or under way.

> To switch the route on or off, {key key.use} the rocket with an empty hand while holding {key key.sneak}.

> What does not fit stays in the hold, and chat tells you how many items are waiting. Put enough chests next to the port or beacon.

## The large capsule
{items spacebreaker:crew_capsule_large}
The {item spacebreaker:crew_capsule_large} carries **three people**; it is unlocked by {tech spacebreaker:heavy_launch}.
