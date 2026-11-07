---
title: EVA Suit
icon: spacebreaker:eva_chestplate
items: [spacebreaker:eva_helmet, spacebreaker:eva_chestplate, spacebreaker:eva_leggings, spacebreaker:eva_boots, spacebreaker:heated_helmet, spacebreaker:heated_chestplate, spacebreaker:heated_leggings, spacebreaker:heated_boots, spacebreaker:thermal_helmet, spacebreaker:thermal_chestplate, spacebreaker:thermal_leggings, spacebreaker:thermal_boots, spacebreaker:radiation_helmet, spacebreaker:radiation_chestplate, spacebreaker:radiation_leggings, spacebreaker:radiation_boots, spacebreaker:suit_station]
research: spacebreaker:pressure_suits
---
In space, your suit is your personal spacecraft. You make the first one before leaving Earth, from ordinary materials.

{items spacebreaker:eva_helmet spacebreaker:eva_chestplate spacebreaker:eva_leggings spacebreaker:eva_boots}

## Recipes
- **Helmet:** 2 aluminium plates, 1 gold ingot, 2 white wool, 1 glass.
- **Body:** 4 white wool, 2 lapis, 1 redstone dust, 1 aluminium plate.
- **Leggings:** 2 aluminium plates, 1 lapis, 4 white wool.
- **Boots:** 2 white wool, 2 black wool.
{recipe spacebreaker:eva_helmet}
{recipe spacebreaker:eva_chestplate}
{recipe spacebreaker:eva_leggings}
{recipe spacebreaker:eva_boots}

{page}
## Sealing
Put on all four pieces and you hear a hiss and see **"Suit sealed · life support running"**. Take one off and it says **"Suit open"**; in an airless place the warning is red.

>! With even one piece missing you cannot breathe in a vacuum.

## Backpack and tanks
The tanks sit in the backpack inside the {item spacebreaker:eva_chestplate}: **two tanks**, three with the Extra Oxygen module. → [Oxygen](oxygen)
- {key key.use} with a tank in hand while wearing the body: fits it.
- In the inventory, right-click the tank onto the body: fits it. Right-click with an empty cursor: takes it out.

## Warnings
- Oxygen below **25%**: a bell.
- Below **10%**: a beep every 3 seconds.
- Out of oxygen, or the suit open in an airless place: a fast alarm. The warning on the display blinks.

{page}
## Helmet
The helmet display shows oxygen, suit state, body and outside temperature, radiation and gravity. If the Sun is blinding, lower the **gold sun visor**: {key key.sneak} + {key key.use} with the helmet in hand.

## When you die
The tanks stay inside the body piece, which drops like any armour: pick it up and the tanks come back with it. Breath, body temperature and radiation dose reset.

## Suit maintenance station
{items spacebreaker:suit_station}
Stand on or next to a {item spacebreaker:suit_station} in your suit: it tops up your tanks and Maneuvering Unit and slowly mends wear on your suit pieces. A piece placed in its slot is mended too. It uses the planet's breathable air, or piped oxygen where there is none, and runs on FE.
{recipe spacebreaker:suit_station}

## Suit tiers
| Suit | Temperature | Radiation shielding |
|---|---|---|
| EVA (Mk1) | −190 / 130 °C | 30% |
| Heated | −250 / 80 °C | 40% |
| Thermal | −190 / 500 °C | 40% |
| Radiation | −190 / 130 °C | 96% |

- **Heated suit** ({tech spacebreaker:heated_suits}): cold worlds, Martian nights and the outer moons.
- **Thermal suit** ({tech spacebreaker:thermal_suits}): Mercury's day and Venus.
- **Radiation suit** ({tech spacebreaker:radiation_hardened_suits}): Jupiter's radiation belts.

> Wear a suit whose technology you do not know and you do not get its full protection; the tooltip tells you what is missing.
