---
title: Research Lab
icon: spacebreaker:research_lab
items: [spacebreaker:research_lab]
---
A bench that analyses samples, disks and experiment records and teaches you technologies.

{recipe spacebreaker:research_lab}

## How to use it
1. Place the lab and give it power: a {item spacebreaker:combustion_generator} is enough at first.
2. Open it with {key key.use}. The **researcher** is whoever opened the lab last.
3. Put a disk, sample or experiment record in the left slot.
4. When the arrow fills up you learn the technology. A notification pops up; several technologies learned at once share one.

The energy bar is on the left. Hover the arrow for what the lab is reading and how long is left. A small red **!** above the arrow means something needs you (no power, nothing new, the result slot is full...): hover it for the reason.

| Technology phase | Time | Power |
|---|---|---|
| Phase 1 | 30 s | 10 FE/t |
| Phase 2 | 60 s | 24 FE/t |
| Phase 3 | 90 s | 60 FE/t |

The lab stores 20,000 FE. If it runs out of power the analysis pauses; progress is kept.

> [!NOTE]
> A disk is **handed back** after reading, so you can lend it to friends. Samples are used up. If a sample has nothing new for you, the lab says so and keeps it.

> [!WARNING]
> To read a disk you need to know the technology's **prerequisites**. Keep advanced disks for later.

## Researching as a team
The two-people button at the top right turns on **team sharing**. While it is on, everyone in your scoreboard team learns what the lab finds, even players who are offline (they learn it when they join). Create a team with `/team add <name>`, join it with `/team join <name>`.

The tree button next to it opens the [Research Tree](research_tree).
