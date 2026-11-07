---
title: The Rocket Equation
icon: minecraft:firework_rocket
research: spacebreaker:basic_rocketry
---
{color blue}**Δv = Isp · g₀ · ln(m₀ / m₁)**{/color}

- **Δv**: how much the rocket can change its speed (m/s).
- **Isp**: specific impulse, how efficiently the exhaust leaves (seconds).
- **g₀**: 9.81 m/s².
- **m₀ / m₁**: full mass divided by empty mass.

## What does it mean?
Because of the logarithm, adding fuel helps **less and less**. That is why rockets drop empty stages: no dead weight.

## Example
Isp 300 s, 100 t full, 25 t empty: Δv = 300 · 9.81 · ln(4) ≈ **4,080 m/s**. Not enough for orbit; you need a second stage.

> [!TIP]
> To lift off, **thrust must beat weight**: thrust-to-weight above 1, ideally 1.3–1.5.

→ More: [Delta-v and the Rocket Equation](sci_delta_v)
