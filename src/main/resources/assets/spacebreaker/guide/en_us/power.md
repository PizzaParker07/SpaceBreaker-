---
title: Power
icon: spacebreaker:combustion_generator
items: [spacebreaker:combustion_generator, spacebreaker:solar_panel, spacebreaker:battery_bank, spacebreaker:copper_cable, spacebreaker:hv_cable]
---
Every machine runs on **FE** (energy). Production and use are measured in "FE/t": per game tick (20 ticks per second).

{items spacebreaker:combustion_generator spacebreaker:solar_panel spacebreaker:battery_bank spacebreaker:copper_cable spacebreaker:hv_cable}

| Block | What it does |
|---|---|
| {item spacebreaker:combustion_generator} | Burns fuel for **40 FE/t**. Only works in breathable air. |
| {item spacebreaker:solar_panel} | Follows the sun; **20 FE/t** at a clear noon. Less in rain, none at night. |
| {item spacebreaker:battery_bank} | Stores **1,000,000 FE**, gives 2,048 FE/t out of its front. |
| {item spacebreaker:copper_cable} | Carries power; for small loads. |
| {item spacebreaker:hv_cable} | Carries a lot of power. |

{recipe spacebreaker:combustion_generator}

> [!WARNING]
> On the Moon and other airless worlds the combustion generator **does not work**: there is no oxygen to burn. Use solar panels and batteries there.

> [!TIP]
> The solar panel gives power out of every side except the top. Placing it right next to the lab is enough.
