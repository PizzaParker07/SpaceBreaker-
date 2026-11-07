---
title: Satellites and Probes
icon: spacebreaker:space_probe
items: [spacebreaker:space_probe, spacebreaker:observation_satellite, spacebreaker:comm_satellite]
research: spacebreaker:satellites
---
You do not have to go to a world yourself to study it. An uncrewed rocket carries a satellite or probe instead of a capsule.

{items spacebreaker:observation_satellite spacebreaker:comm_satellite spacebreaker:space_probe}

| Payload | What it does |
|---|---|
| {item spacebreaker:observation_satellite} | When released, sends you **Orbital Survey** data. |
| {item spacebreaker:comm_satellite} | Opens the **mission control** signal in that planet's system. |
| {item spacebreaker:space_probe} | Lands on the target and sends you **2 samples** of that world and **Probe Landing** data. |

> Venus's surface has its own probe: the [Venus Descent Probe](venus_clouds), released from the clouds.

## Setting up a mission
1. In the [assembler](rocket_assembler), put the satellite or probe in the capsule slot.
2. Press **Target...** on the console.
3. The star map opens in **Mission Planning**: pick the target and press **Set as mission target**.
4. **Launch**. Once in orbit the rocket flies to its target by itself. The console shows **Uncrewed mission** and **One-way trip (uncrewed)**.

## Signal
Without a **communications relay** around a world you get **No signal**: mission control cannot be heard there. A relay satellite fixes that. A world an observation satellite has seen shows **Mapped from orbit ✔** on the star map.

> Probes are the safest way to collect a world's samples before you go. Orbital Survey and Probe Landing data unlock their own technologies too: {tech spacebreaker:spectroscopy}, {tech spacebreaker:astrobiology} and more.
