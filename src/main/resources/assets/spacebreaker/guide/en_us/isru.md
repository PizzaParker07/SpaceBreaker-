---
title: In-Situ Resources (ISRU)
icon: spacebreaker:sabatier_reactor
items: [spacebreaker:atmosphere_processor, spacebreaker:sabatier_reactor, spacebreaker:regolith_heater, spacebreaker:ice_melter, spacebreaker:chemical_reactor]
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
Melts rock and splits it with electricity: **oxygen from rock**.
{recipe spacebreaker:regolith_processing/regolith}

{page}
## Sabatier reactor
Turns carbon dioxide and hydrogen into **methane and water**. The water goes back to the electrolyser; you can liquefy and store the methane with the [Cryogenics](cryogenics) liquefier.

>! Your rocket engines burn **RP-1 and liquid oxygen** for now. Bring RP-1 from Earth for the way home; liquid oxygen you can make on the spot.
{recipe spacebreaker:sabatier/methane}

> [!TIP]
> **Oxygen and methane on Mars:** ice melter → water → electrolyser → oxygen + hydrogen; oxygen → cryogenic liquefier → liquid oxygen. Hydrogen and CO₂ from the atmosphere processor go into the Sabatier reactor. The water it gives goes back to the electrolyser.

## Chemical reactor
Combines one solid and two fluids; for example sulfur, water and oxygen make **sulfuric acid**.
{recipe spacebreaker:chemical_reaction/sulfuric_acid}
