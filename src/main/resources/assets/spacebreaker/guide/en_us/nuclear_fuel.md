---
title: Nuclear Fuel
icon: spacebreaker:nuclear_fuel_element
items: [spacebreaker:uraninite_ore, spacebreaker:raw_uraninite, spacebreaker:uranium_dust, spacebreaker:yellowcake, spacebreaker:gas_centrifuge, spacebreaker:enriched_uranium, spacebreaker:depleted_uranium, spacebreaker:nuclear_fuel_element, spacebreaker:rtg_fuel_pellet]
research: spacebreaker:nuclear_fission
---
The road from uraninite ore to the fuel of a nuclear thermal rocket.

## The chain
1. Grind {item spacebreaker:raw_uraninite} into **uranium dust** in the crusher.
2. In the chemical reactor, dust + **125 mB of sulfuric acid** → {item spacebreaker:yellowcake}.
3. In the {item spacebreaker:gas_centrifuge}, 4 yellowcake → **1** {item spacebreaker:enriched_uranium} + **3** {item spacebreaker:depleted_uranium}.
4. In the electric arc furnace, 2 enriched uranium + 2 coal dust + 2 tungsten rods → {item spacebreaker:nuclear_fuel_element}.
{recipe spacebreaker:chemical_reaction/yellowcake}
{recipe spacebreaker:centrifuging/enriched_uranium}

{page}
## The centrifuge cascade
One centrifuge alone is slow. Real plants run thousands one after another. Here **every working centrifuge** placed beside one on the same level adds **half a stage of speed**; up to four neighbours, so at most **3×**. Line them up in a row or a plus shape.

> Every enrichment leaves a big pile of {item spacebreaker:depleted_uranium}: the natural by-product of enrichment.

{recipe spacebreaker:arc_smelting/nuclear_fuel_element}

## RTG pellet
The {item spacebreaker:rtg_fuel_pellet} is now made from **4 yellowcake in the sintering furnace**. → [Power](power)
{recipe spacebreaker:sintering/rtg_fuel_pellet}

>! The pellet itself does give off radiation: carrying it gives you a dose. Put it in the RTG and keep your distance.

> **Fresh uranium fuel is not dangerous:** uranium has low activity, and you can pick it up and carry it. The real radiation comes when the fuel element **burns in a nuclear thermal rocket engine**. → [Rocket Parts](rocket_parts)
