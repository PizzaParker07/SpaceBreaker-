---
title: Rover
icon: spacebreaker:rover
items: [spacebreaker:rover, spacebreaker:sprint_rover, spacebreaker:pressurized_rover, spacebreaker:amphibious_rover]
research: spacebreaker:rovers
---
An electric vehicle for reaching distant craters, ice fields and ruins from your base.

{recipe spacebreaker:rover}

| | |
|---|---|
| Seats | 2 |
| Battery | 400,000 FE |
| Use | 50 FE per block (≈ 8,000 blocks) |
| Climbs | 1 block |
| Cargo | 27 slots |

## Driving
- Get in: {key key.use}. Get out: {key key.sneak}.
- **Cargo:** click the rover while holding {key key.sneak}.
- Grip depends on gravity: in low gravity you slide easily, so turn slowly.
- Grip also depends on the ground. On **ice**, and on Pluto's nitrogen ice most of all, the rover speeds up and brakes weakly, steers poorly and keeps sliding the way it was going. Brake early.

## Charging
- Next to a {item spacebreaker:battery_bank} or a cable it charges at **200 FE/t**.
- {key key.use} the rover with a {item spacebreaker:solar_panel} or {item spacebreaker:rtg} in hand: it is mounted on the rear deck behind the seats and charges on the move.

## Other rovers {#models}
Both are built from a finished rover, so the upgrades and charging work the same.

| | Rover | Sprint | Pressurized |
|---|---|---|---|
| Top speed | 9 m/s | 13 m/s | 7 m/s |
| Battery | 400,000 FE | 300,000 FE | 1,200,000 FE |
| Range | ≈ 8,000 blocks | ≈ 8,500 blocks | ≈ 15,000 blocks |
| Cargo | 27 slots | 9 slots | 27 slots |

{items spacebreaker:sprint_rover}
The {item spacebreaker:sprint_rover} is light and quiet on an aluminium-lithium frame, but its tyres grip less: it skids sooner on loose ground.
{recipe spacebreaker:sprint_rover}

{items spacebreaker:pressurized_rover}
The {item spacebreaker:pressurized_rover} has a closed cabin: aboard you need no helmet, and its walls stop **half the radiation**. To fill its oxygen tank, {key key.use} it holding a container of oxygen gas or LOX (such as a {item spacebreaker:cryogenic_canister}), or park it beside a tank or gas pipe that holds oxygen: it tops itself up. The tank holds air for about **1.8 hours** for one rider, and life support draws **4 FE/t** from the battery while anyone is aboard. With an empty tank or a flat battery the cabin loses pressure ("Cabin unpressurized"), so keep your suit on.
{recipe spacebreaker:pressurized_rover}

{items spacebreaker:amphibious_rover}
The {item spacebreaker:amphibious_rover} puts the pressurized cabin on a sealed hull with side floats and a stern propeller. It is a little slower on land (6 m/s), but it drives straight into water or liquid methane, as on Titan's lakes, and sails on at **5 m/s**, using twice the energy per block afloat. Afloat there are no brakes: reverse the propeller to slow down. Nose into a bank to climb back out onto the shore.
{recipe spacebreaker:amphibious_rover}
