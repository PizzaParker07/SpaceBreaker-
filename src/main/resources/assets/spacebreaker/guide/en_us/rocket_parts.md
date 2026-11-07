---
title: Rocket Parts
icon: spacebreaker:kerosene_engine
items: [spacebreaker:nose_cone, spacebreaker:crew_capsule, spacebreaker:heat_shield, spacebreaker:parachute, spacebreaker:fuel_tank_small, spacebreaker:fuel_tank_medium, spacebreaker:fuel_tank_large, spacebreaker:kerosene_engine, spacebreaker:vacuum_engine, spacebreaker:methalox_engine, spacebreaker:methalox_vacuum_engine, spacebreaker:hydrolox_engine, spacebreaker:hydrolox_vacuum_engine, spacebreaker:decoupler, spacebreaker:interstage, spacebreaker:fins, spacebreaker:grid_fins, spacebreaker:landing_legs, spacebreaker:rcs_thrusters]
research: spacebreaker:basic_rocketry
---
A rocket is a stack of **stages** with the **payload** on top.

## Payload
{items spacebreaker:crew_capsule spacebreaker:heat_shield spacebreaker:parachute spacebreaker:nose_cone}
- {item spacebreaker:crew_capsule}: carries the crew.
- {item spacebreaker:heat_shield}: goes **under** the capsule; protects you during reentry.
- {item spacebreaker:parachute}: goes **on top** of the capsule, for a soft landing.
- {item spacebreaker:nose_cone}: the top of an uncrewed payload.

## Stages
{items spacebreaker:kerosene_engine spacebreaker:vacuum_engine spacebreaker:fuel_tank_small spacebreaker:fuel_tank_medium spacebreaker:fuel_tank_large}
Each stage has **one engine** at the bottom and **fuel tanks** above it. The tanks carry whatever that stage's engine burns.
- The **sea-level engine** works well in air: for the first stage.
- The **vacuum engine** is far more efficient in space: for the upper stages.

## Engines and fuels
{items spacebreaker:methalox_engine spacebreaker:methalox_vacuum_engine spacebreaker:hydrolox_engine spacebreaker:hydrolox_vacuum_engine}
**Methalox engines** are unlocked by {tech spacebreaker:methane_engines}. They burn **liquid methane and liquid oxygen**; you can make that fuel on Mars. Two Isp numbers: sea level / vacuum.

| Engine | Thrust | Isp | Fuel |
|---|---|---|---|
| Kerosene (sea level) | 560 kN | 282 / 311 s | RP-1 + LOX (1 : 2.56) |
| Kerosene (vacuum) | 190 kN | 345 s | RP-1 + LOX (1 : 2.56) |
| Methalox (sea level) | 600 kN | 330 / 350 s | methane + LOX (1 : 3.6) |
| Methalox (vacuum) | 220 kN | 380 s | methane + LOX (1 : 3.6) |
| Hydrolox (sea level) | 650 kN | 366 / 452 s | hydrogen + LOX (1 : 6) |
| Hydrolox (vacuum) | 110 kN | 465 s | hydrogen + LOX (1 : 6) |

**Hydrolox engines** are unlocked by {tech spacebreaker:hydrogen_engines}. Liquid hydrogen is the most efficient fuel but very **bulky** (a fourteenth of water's density): the same mass needs far bigger tanks.

The assembler shows each stage's fuel type and Δv.

## Separators
{items spacebreaker:decoupler spacebreaker:interstage}
Put a **decoupler** or an **interstage** between stages. A stage that runs dry drops away and the rocket gets lighter.

## Side parts
{items spacebreaker:fins spacebreaker:grid_fins spacebreaker:landing_legs spacebreaker:rcs_thrusters}
- **Fins**: less steering loss on ascent.
- **Grid fins**: steer the first stage.
- **Landing legs**: **required** to land on the Moon.
- **RCS thrusters**: fine control for landings.

{recipe spacebreaker:rocketry/kerosene_engine}
