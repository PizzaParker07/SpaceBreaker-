---
title: In-Situ Resources (ISRU)
icon: spacebreaker:sabatier_reactor
items: [spacebreaker:atmosphere_processor, spacebreaker:sabatier_reactor, spacebreaker:regolith_heater, spacebreaker:ice_melter, spacebreaker:chemical_reactor, spacebreaker:regolith_excavator, spacebreaker:sintering_furnace, spacebreaker:sintered_regolith, spacebreaker:sintered_regolith_bricks]
research: spacebreaker:isru
---
Every kilogram launched from Earth is expensive. So make what you can **where you land**. This is called in-situ resource utilisation (ISRU).

## The chain
| Input → output | Machine |
|---|---|
| Ice → water | {item spacebreaker:ice_melter} |
| Dry ice → CO₂ | {item spacebreaker:ice_melter} |
| Water → O₂ + H₂ | {item spacebreaker:electrolyzer} |
| Local air → CO₂ (Mars) | {item spacebreaker:atmosphere_processor} |
| CO₂ + H₂ → methane + water | {item spacebreaker:sabatier_reactor} |
| Regolith → O₂ | {item spacebreaker:regolith_heater} |

{page}
## Ice melter
Turns ice into clean water and dry ice into carbon dioxide.
{recipe spacebreaker:melting/ice}

## Atmosphere processor
Separates the most useful gas from the local air: **carbon dioxide** on Mars. It needs **open air** above it; it does not work in a closed room.

## Regolith heater
Melts rock and splits it with electricity: **oxygen from rock**. One regolith gives **250 mB of oxygen**. Moon and Mercury regolith, soaked by the solar wind, also gives **20 mB of helium-3**. Hydrogen comes from ice.
{recipe spacebreaker:regolith_processing/regolith}
{recipe spacebreaker:regolith_processing/solar_wind_volatiles}

## Regolith excavator
{items spacebreaker:regolith_excavator}
The {item spacebreaker:regolith_excavator} digs the **5×5** patch below it layer by layer, down to **16 blocks**, and hands what it digs to the machines or chests beside it. It uses **30 FE/t**.
{recipe spacebreaker:regolith_excavator}

> **Fully automatic LOX on a Moon base:** excavator → regolith heater → cryogenic liquefier → cryogenic tank. Pipe and cable them together and liquid oxygen piles up even while you are away.

{page}
## Sabatier reactor
Turns carbon dioxide and hydrogen into **methane and water**. The water goes back to the electrolyser. Turn the methane into **liquid methane** with the [Cryogenics](cryogenics) liquefier: with liquid oxygen it fuels the **methalox engines**. Fill the rocket through the fuel umbilical, a pipe, or by right-clicking with a bucket or cryogenic canister. → [Rocket Parts](rocket_parts)
{recipe spacebreaker:sabatier/methane}

> [!TIP]
> **Oxygen and methane on Mars:** ice melter → water → electrolyser → oxygen + hydrogen; oxygen → cryogenic liquefier → liquid oxygen. Hydrogen and CO₂ from the atmosphere processor go into the Sabatier reactor. The water it gives goes back to the electrolyser.

## Chemical reactor
Combines one solid and two fluids; for example sulfur, water and oxygen make **sulfuric acid**.
{recipe spacebreaker:chemical_reaction/sulfuric_acid}

{page}
## Sintering furnace
{items spacebreaker:sintering_furnace spacebreaker:sintered_regolith spacebreaker:sintered_regolith_bricks}
The {item spacebreaker:sintering_furnace} heats any planet's regolith with electricity and fuses it into {item spacebreaker:sintered_regolith}; no air needed. A stonecutter turns it into bricks, slabs, stairs and walls.
{recipe spacebreaker:sintering/regolith}

> Build your base from bricks made on site: no need to ship blocks from Earth. **Full blocks are airtight**; stairs, slabs, walls and other partial blocks leak air. → [Air for the Base](base_air)

> Put the excavator next to the furnace: the regolith it digs goes straight in and building blocks pile up on their own.
