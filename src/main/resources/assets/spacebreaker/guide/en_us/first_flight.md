---
title: Your First Flight
icon: spacebreaker:rocket
research: spacebreaker:basic_rocketry
---
From the pad to orbit and back home, step by step.

## 1. Get ready
- Wear the four-piece [EVA suit](eva_suit) with two full tanks. The capsule is pressurised: seated, you breathe even without a suit, but the moment you step out you are back in vacuum.
- Take spare tanks and an {item spacebreaker:emergency_beacon}.
- Build your [launch pad](launch_pad) and check it at the console.

## 2. What you need
- {tech spacebreaker:basic_rocketry}: capsule, nose cone, assembler. Its disk is in the observatory.
- {tech spacebreaker:launch_infrastructure}: pad and tower.
- {tech spacebreaker:liquid_fuel_engines} and {tech spacebreaker:staging}: engine, tanks, decoupler.
- {tech spacebreaker:vacuum_engines}: the upper-stage engine. Analyse the **Suborbital Flight** data from the observatory archive in the lab.

## 3. Design the rocket
In the [assembler](rocket_assembler), build a crewed rocket with at least two stages. Orbit needs about **9,400 m/s** of Δv.

>! Your first rocket has **no heat shield or parachute**: {tech spacebreaker:landing_systems} unlocks them, and it needs **Orbital Flight** data. You get that data the first time you reach orbit. Come home with the {item spacebreaker:emergency_beacon}.

## 4. Place it and fuel it
1. {key key.use} the {item spacebreaker:rocket} on the middle trench grate.
2. Pipe a tank of RP-1 and a tank of liquid oxygen into the {item spacebreaker:fuel_umbilical}. It fills the rocket.
3. Watch the fuel level on the console.

{page}

## 5. Board and launch {#launch}
1. Board the rocket with {key key.use}. The camera moves around the rocket: turn it with the mouse, zoom with the wheel, switch to the cockpit with {key key.togglePerspective}.
2. Check the console's list: pad, fuel, Δv and thrust-to-weight should be green.
3. Press **Launch**, or press {key key.spacebreaker.launch} while aboard. The countdown starts; the same key aborts it.

> [!WARNING]
> If you run out of fuel the capsule separates and comes down on its parachute. Without a heat shield, a fall from high up can still kill you.

## 6. In orbit
There is no loading screen: after a short transition, a message tells you when you arrive, and two **Orbital Flight** data drop into your bag. Open the **star map** with {key key.spacebreaker.star_map} click a world and pick **Go to orbit** or **Land**. The map tells you plainly whether your fuel is enough. Lunar orbit needs about **4,000 m/s** more. → [Flight and Navigation](navigation)

## 7. Coming home
- **First flight:** use the {item spacebreaker:emergency_beacon}. A capsule with a heat shield and parachute comes and takes you home.
- **Later flights:** put a **heat shield** under your capsule and a **parachute** on top. Pick Earth on the star map and **Land**; the shield takes the heat and the parachute opens.

> Back home, analyse the Orbital Flight data in the lab: {tech spacebreaker:landing_systems} unlocks the heat shield, parachute and landing legs. It also needs {tech spacebreaker:precision_manufacturing}: the fabricator makes the ablative shield plates the heat shield is built from. You need the legs to land on the Moon.
{recipe spacebreaker:rocketry/heat_shield}
