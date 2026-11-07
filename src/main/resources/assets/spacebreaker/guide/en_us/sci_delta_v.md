---
title: Delta-v and the Rocket Equation
icon: minecraft:firework_rocket
research: spacebreaker:basic_rocketry
---
In space, distance is not measured in kilometres but in **Δv**: how much you can change your speed in total (m/s). Every manoeuvre has a Δv price; your rocket's Δv has to cover them.

## The rocket equation {#equation}
{color blue}**Δv = Isp · g₀ · ln(full mass / empty mass)**{/color}

- **Isp** (specific impulse): how efficient the engine is. Kerosene ≈ 300 s, hydrogen ≈ 450 s.
- **ln**: the logarithm. That is the catch: doubling the fuel does **not** double the Δv.

> [!WARNING]
> Reaching orbit from Earth takes 9.4 km/s. A single kerosene stage would have to be 96 % fuel. That is close to impossible.

## Why stages? {#stages}
Dropping empty tanks and engines makes the rest of the rocket **lighter**, so the same fuel gives more Δv. That is why real rockets have 2–3 stages.

Example: a 504-tonne rocket, 10 tonnes of payload, kerosene engines.

| | Δv |
|---|---|
| One stage | ≈ 7.9 km/s: not enough for orbit |
| Same mass, two stages | ≈ 10.8 km/s: reaches orbit |

## Thrust-to-weight ratio {#twr}
On the pad, thrust must beat weight (ratio > 1) or you will not lift off. The higher the ratio, the less Δv gravity takes: about 1.5 km/s lost on Earth at 1.3, only ≈ 0.2 km/s on the Moon at 2.

## Numbers in the game {#realism}
The rocket assembler shows each stage's Δv and thrust-to-weight ratio. The server's realism setting scales trip costs:

| Setting | Cost | Earth → orbit |
|---|---|---|
| Realistic | real value | ≈ 9.0–9.4 km/s |
| Balanced | 35 % | ≈ 3.2 km/s |
| Easy | 25 % | ≈ 2.3 km/s |

>! On **Realistic** you can reach orbit; the Moon and Mars need a heavier rocket than today's assembly limit. For farther worlds pick **Balanced** or **Easy**.

→ [Rocket Equation](rocket_equation) · [Transfer Orbits](sci_transfers)
