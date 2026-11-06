---
title: Air for the Base
icon: spacebreaker:oxygen_distributor
items: [spacebreaker:oxygen_distributor, spacebreaker:airlock_door]
research: spacebreaker:oxygen_distribution
---
For a base you can walk around in without a helmet you need a sealed room, a distributor and an airlock.

{structure spacebreaker:pressurised_habitat}

{items spacebreaker:oxygen_distributor spacebreaker:airlock_door}

1. Build a room closed on every side (up to **4,096 blocks** of volume).
2. Place an {item spacebreaker:oxygen_distributor} inside. Fit a full tank or **pipe oxygen** into it.
3. The distributor scans the room. If it leaks, it tells you **where**.
4. Use an {item spacebreaker:airlock_door} as the door: airtight while closed, opened by hand or redstone.

{recipe spacebreaker:oxygen_distributor}
{recipe spacebreaker:airlock_door}

## How much oxygen?
- **Pressurising:** first it fills the room, **4 breath ticks** per block. The status message shows the percentage.
- **Upkeep:** 1 breath tick every tick, plus 1 for every **40 blocks**.
- **Leaks:** the air escapes and the room has to be pressurised again.

Example: a 100-block room first needs 400 breath ticks (200 mB of oxygen gas), then uses 3.5 breath ticks per tick.

> [!TIP]
> The distributor checks the room again when a block is broken or placed. Glass works for windows; an ordinary door lets the air out.
