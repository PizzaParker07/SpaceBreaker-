---
title: Rocket Parts
icon: spacebreaker:kerosene_engine
items: [spacebreaker:nose_cone, spacebreaker:crew_capsule, spacebreaker:heat_shield, spacebreaker:parachute, spacebreaker:fuel_tank_small, spacebreaker:fuel_tank_medium, spacebreaker:fuel_tank_large, spacebreaker:kerosene_engine, spacebreaker:vacuum_engine, spacebreaker:decoupler, spacebreaker:interstage, spacebreaker:fins, spacebreaker:grid_fins, spacebreaker:landing_legs, spacebreaker:rcs_thrusters]
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
Each stage has **one engine** at the bottom and **fuel tanks** above it. Tanks hold RP-1 and liquid oxygen.
- The **sea-level engine** works well in air: for the first stage.
- The **vacuum engine** is far more efficient in space: for the upper stages.

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
