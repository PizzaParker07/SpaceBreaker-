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
Put on all four pieces and you hear a hiss and see **"Suit sealed · life support online"**. The note shows at most once every 30 seconds. Taking a piece off is only announced where there is no air: **"Suit unsealed: no air out here!"**

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
## Helmet {#hud}
The helmet display stays small. Beside the hotbar, on the side away from your main hand, is one oxygen line: a tank icon and a bar. The time left (mm:ss) appears beside it below 30 %, or while a screen such as your inventory is open. The bar is blue, orange below **30 %**, and red and blinking below **15 %**.
- **Hazard icons** appear after the oxygen time only while something is wrong: suit (open or below 20 % in an airless place), temperature (beyond your suit's rating, or your body outside 35–38.5 °C; red outside 32–41 °C), radiation (over 50 mSv/day, or 250 mSv in total; red from 1,000), pressure (suit under strain) and maneuvering gas (below 10 %).
- **The way home** (arrow, target and distance) only shows when your oxygen will not last the walk back to the target you picked with the environment sensor. → [Environment sensor](tools)
- **One warning** blinks above the hotbar, such as **TURN BACK: O2 RUNNING LOW FOR THE WAY HOME** in yellow, or **NOT ENOUGH O2 TO GET BACK**, **SUIT NOT SEALED** and **BEYOND SUIT RATING** in red.
- Radio captions appear at the top left; the sensor card moves down below them while they are shown. The top centre of the screen is kept clear.

If the Sun is blinding, lower the **gold sun visor**: {key key.sneak} + {key key.use} with the helmet in hand.

> Want every gauge? Turn on **detailedHud** in the client config: body temperature, radiation, pressure, gravity and outside temperature, the MAG / TETHER / COLD TRAP / µG labels, maneuvering gas, and the way home all the time. **visorRim** adds a thin visor rim round the screen that frosts over in the cold (off by default; **visorRimOpacity**). **opacity** fades the whole display and **enabled** turns it off.

{page}
## Helmet lamps {#lamps}
The helmet lamps switch on by themselves in the dark and light where you look. {key key.spacebreaker.helmet_lamp} switches between a **wide** beam (about 20 blocks) for working nearby and a **long** beam (about 40 blocks) for lava tubes and shadowed craters. Only the light changes: no extra power or oxygen. Everyone sees the same beam.

## Sound {#sound}
With the **realistic sound** client setting (atmosphereSound, **off** by default) the world sounds a little quieter and softer where the air is thin: on Mars at least 80 % of the volume, in vacuum (the Moon, orbit, asteroids, comets) about half, and the high notes fade first. Your own steps and tools, helmet sounds (radio, mission control, warning beeps), breathable air and pressurised rooms stay normal. With the setting off everything sounds as usual.

## When you die
The tanks stay inside the body piece, which drops like any armour: pick it up and the tanks come back with it. Breath, body temperature and radiation dose reset.

## Suit maintenance station
{items spacebreaker:suit_station}
Stand on or next to a {item spacebreaker:suit_station} in your suit: it tops up your tanks and Maneuvering Unit and slowly mends wear on your suit pieces. A piece placed in its slot is mended too, and it charges the {item spacebreaker:electric_drill}. → [Tools](tools) It uses the planet's breathable air, or piped oxygen where there is none, and runs on FE.
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
