---
title: Flight and Navigation
icon: spacebreaker:emergency_beacon
items: [spacebreaker:emergency_beacon, spacebreaker:landing_beacon]
research: spacebreaker:orbital_mechanics
---
## The camera aboard
When you board, the camera switches to a **third-person** view that circles the rocket.
- Mouse: orbit around the rocket.
- Mouse wheel: zoom in / out.
- {key key.togglePerspective}: the cockpit window.

There are no loading screens on the way to orbit, between worlds or when landing; a transition animation plays instead.

## The star map
With the rocket **in orbit**, {key key.spacebreaker.star_map} opens the map. Launch from a pad first.
1. It is one map of the whole Solar System: every world sits where it really is on its orbit, as a small disc in its own colours. Comets and near-Earth asteroids are small dots on their orbits. A small marker shows where your rocket is.
2. **Scroll** to zoom, **drag** to move, **right-click** to see everything again. Click a world: the view glides to it, close enough to see its moons.
3. The short panel shows the world's name and kind, **Orbit** or **Land**, and its dangers as coloured words: **No air**, **Unbreathable air**, **Crushing air**, **Hot**, **Cold**, **Radiation**.
4. For the trip it shows the **Δv** and the **Time** in large figures, one fuel bar (fuel needed against fuel on board), a line on getting home (**Home: enough fuel**, **Home: make fuel there** or **Home: not enough fuel**), the launch window and only the most important warning.
5. The route is a thin dashed arc on the map. Press **Go**. If it is greyed out, the panel says why.

A small **station** mark beside a world means there is a space station in its orbit. → [Space Station](space_station)

While flying, watch the **Fuel left (Δv)** line on the display.

> **Δv** is how much your rocket can change its speed; in short: your fuel.

## Landing on the Moon
- Lunar orbit to the surface takes about **1,870 m/s**. The way back takes as much.
- You cannot land without **landing legs**.
- A hard landing damages the rocket; RCS thrusters soften it.
- On long trips cryogenic fuel boils off; the trip card shows how much. → [Boil-off](rocket_parts#boil_off)
- Put a {item spacebreaker:landing_beacon} at your base: rockets descending from orbit land on the nearest one ("Landing beacon in range: coming down on the base"). On worlds with air the wind can drift the touchdown up to six blocks. The star map shows surface bases and their beacon count beside stations.
- **Lunar shuttle:** a single reusable stage (vacuum engine, medium tank, landing legs, capsule, RCS thrusters). It flies between a station in Moon orbit and a surface base on one tank, docks at a port and refuels from the depot. → [Space Station](space_station)
- **Mars lander:** methalox vacuum engine, large tank, landing legs, heat shield, capsule, parachute and RCS thrusters. It flies from Mars orbit to the surface and, refuelled with methane and oxygen made on Mars, back to orbit. → [In-Situ Resources](isru)

## Reentry
The heat shield's rating matters: one survives a return from **low orbit**, a better one a direct return **from the Moon**. Its tooltip tells you which.

{items spacebreaker:emergency_beacon}

**Emergency return beacon:** away from Earth, it calls a capsule that brings you home. It does nothing on Earth.
