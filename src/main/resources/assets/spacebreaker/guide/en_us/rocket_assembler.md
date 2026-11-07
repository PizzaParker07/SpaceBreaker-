---
title: Rocket Assembler
icon: spacebreaker:rocket_assembler
items: [spacebreaker:rocket_assembler, spacebreaker:rocket]
research: spacebreaker:basic_rocketry
---
{recipe spacebreaker:rocketry/rocket_assembler}

Put the parts into the assembler's slots and it works out the rocket for you.

1. **Fill the stages from the bottom up.** Stage 1 is at the bottom: engine and tanks.
2. Put a **separator** between stages.
3. Put the **payload** on top: a capsule (heat shield under it, parachute on top) or a nose cone.
4. Add side parts in the side slots.
5. Check each stage's **Δv** and **thrust-to-weight** on screen, then press **Assemble**.

You get a {item spacebreaker:rocket}. Its tooltip shows the height, mass, number of stages and total Δv.

> A stage with a nuclear thermal engine is marked **☢ Nuclear**. On the first stage the mark turns red: it is too weak to lift off and would irradiate the pad, so put it on an upper stage. → [Nuclear thermal engine](rocket_parts#ntr)

## How much Δv?
| Trip | Δv (m/s) |
|---|---|
| Earth surface → low orbit | 9,400 |
| Low orbit → lunar orbit | 4,000 |
| Lunar orbit → lunar surface | 1,870 |
| Lunar surface → lunar orbit | 1,870 |
| Lunar orbit → Earth (direct reentry) | 850 |

> [!WARNING]
> If thrust-to-weight is **below 1** the rocket cannot lift off. 1.3–1.5 is good for the first stage.

> [!TIP]
> When Δv falls short, more fuel is not always the answer: tanks are heavy too. Another stage, or a vacuum engine on the upper stage, usually works better. → [The Rocket Equation](rocket_equation)
