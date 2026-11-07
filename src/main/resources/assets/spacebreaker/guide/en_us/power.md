---
title: Power
icon: spacebreaker:combustion_generator
items: [spacebreaker:combustion_generator, spacebreaker:solar_panel, spacebreaker:battery_bank, spacebreaker:copper_cable, spacebreaker:hv_cable, spacebreaker:rtg, spacebreaker:rtg_fuel_pellet, spacebreaker:power_monitor]
---
Every machine runs on **FE** (energy). Production and use are measured in "FE/t": per game tick (20 ticks per second).

{items spacebreaker:combustion_generator spacebreaker:solar_panel spacebreaker:battery_bank spacebreaker:copper_cable spacebreaker:hv_cable}

| Block | What it does |
|---|---|
| {item spacebreaker:combustion_generator} | Burns fuel for **40 FE/t**. Only works in breathable air. |
| {item spacebreaker:solar_panel} | Follows the sun; **20 FE/t** at a clear noon. Less in rain, none at night. |
| {item spacebreaker:battery_bank} | Stores **1,000,000 FE**, gives 2,048 FE/t out of its front. Placed against the underside of a block (a ceiling), it gives downwards. |
| {item spacebreaker:copper_cable} | Carries power; for small loads. |
| {item spacebreaker:hv_cable} | Carries a lot of power. |

{recipe spacebreaker:combustion_generator}

> [!WARNING]
> On the Moon and other airless worlds the combustion generator **does not work**: there is no oxygen to burn. Use solar panels and batteries there.

> [!TIP]
> The solar panel gives power out of every side except the top. Placing it right next to the lab is enough.

{page}
## Power monitor
{items spacebreaker:power_monitor}
Place a {item spacebreaker:power_monitor} against a battery bank (or any block that stores energy): its screen shows the charge, the net flow and the time until full or empty. Its comparator output follows the charge, so redstone can start a generator when the charge runs low.

## Night and power {#night}
For a steady 10 FE/t load (a panel gives at most 20 FE/t at noon, a battery bank holds 1,000,000 FE):

| World | Night | Sunlight | Panels | Battery banks |
|---|---|---|---|---|
| Mercury | 115 min | 667% (capped at 3×) | 0.4 | 1.4 |
| Moon | 47 min | 100% | 1.1 | 0.6 |
| Mars | 9 min | 43% | 2.5 | 0.1 |
| Ceres | 5 min | 13% | 8 | 0.06 |
| Europa | 16 min | 3.7% | 29 | 0.2 |
| Ganymede | 23 min | 3.7% | 29 | 0.3 |
| Callisto | 35 min | 3.7% | 29 | 0.4 |
| Titan | 35 min | 1.1% | 100 | 0.4 |
| Pluto | 22 min | 0.06% | ~1000 | 0.3 |

>! From Europa outwards solar panels are not practical: the Sun is too far. Use an {item spacebreaker:rtg} there ({tech spacebreaker:radioisotope_power}); one RTG carries four 10 FE/t loads and runs on {item spacebreaker:rtg_fuel_pellet}.

{recipe? spacebreaker:rtg}
