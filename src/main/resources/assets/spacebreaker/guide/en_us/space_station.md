---
title: Space Station
icon: spacebreaker:docking_port
items: [spacebreaker:station_kit, spacebreaker:habitat_kit, spacebreaker:station_hull, spacebreaker:station_window, spacebreaker:docking_port, spacebreaker:station_bunk, spacebreaker:fuel_depot, spacebreaker:solar_array, spacebreaker:crew_capsule_large]
research: spacebreaker:space_stations
---
A home, workshop and fuel depot in orbit. You no longer have to fuel your rocket from the ground every time.

{items spacebreaker:station_kit spacebreaker:station_hull spacebreaker:station_window spacebreaker:docking_port}

## Building it
1. Take a {item spacebreaker:station_kit} to orbit and use it in space: a **5×5** hull floor appears.
2. Build walls and a roof with {item spacebreaker:station_hull} and {item spacebreaker:station_window}.
3. Put an {item spacebreaker:oxygen_distributor} inside: the closed room is pressurised and you can take off your helmet. → [Air for the Base](base_air)
4. Fit a {item spacebreaker:docking_port} on the outside.

## Habitat module kit
{items spacebreaker:habitat_kit}
If you would rather not build the walls by hand, use a {item spacebreaker:habitat_kit} in orbit. A sealed **7×5×7** room unfolds in front of you:
- hull walls with a band of windows and an airlock door facing you,
- a {item spacebreaker:docking_port} on the far wall,
- an {item spacebreaker:oxygen_distributor} in the middle and a {item spacebreaker:station_bunk} in a corner,
- a row of solar panels on the roof.

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
The {item spacebreaker:solar_array} carries three panels on a one-block footprint and makes up to **60 FE/t**. The sun never sets in orbit, so the array runs at full power around the clock.

## Docking
- A crewed rocket arriving in an orbit with a station starts **36 blocks** out from the nearest docking port. It closes in quickly, then creeps the last stretch on thruster puffs and locks on with a clunk: **"Docked"**. Nobody can step off on the way in.
- The crew of a rocket in orbit within **12 blocks** of the port can step off into the station and back on.
- Fuel and liquid oxygen piped into the port flow into the docked rocket: the station becomes a **fuel depot**.

## The large capsule
{items spacebreaker:crew_capsule_large}
The {item spacebreaker:crew_capsule_large} carries **three people**; it is unlocked by {tech spacebreaker:heavy_launch}.
