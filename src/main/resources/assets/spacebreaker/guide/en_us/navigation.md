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
1. The Sun is on the left; the **You are here** pin shows where you are. Click a planet to zoom into its system. Go back with **◀ Solar System** or a right-click.
2. The panel on the right shows the chosen world's picture, a short description and its hazards: **No air**, **Unbreathable air**, **Crushing pressure**, **Hot**, **Cold**, **Radiation**.
3. Pick **Go to orbit** or **Land**. (A world that is not built yet says **Coming soon**; a body without ground says **No ground to land on**.)
4. The panel tells you:
   - **Fuel needed X · on board Y** → **Enough ✔** or **X short ✘**
   - **Fuel to come home:** enough ✔ / not enough ✘
   - **Trip time:** X days
   - **Launch window:** now is fine, or the best one is a little later (going now costs more fuel)
5. The route is drawn on the map in numbered steps. Press the big **Go** button. If it is greyed out, it tells you why.

A small **station icon** beside a planet means there is a space station in its orbit; the side panel shows who built it and how many docking ports it has. → [Space Station](space_station)

While flying, watch the **Fuel left (Δv)** line on the display.

> **Δv** is how much your rocket can change its speed; in short: your fuel.

## Landing on the Moon
- Lunar orbit to the surface takes about **1,870 m/s**. The way back takes as much.
- You cannot land without **landing legs**.
- A hard landing damages the rocket; RCS thrusters soften it.
- Put a {item spacebreaker:landing_beacon} at your base: rockets descending from orbit land on the nearest one ("Landing beacon in range: coming down on the base"). On worlds with air the wind can drift the touchdown up to six blocks. The star map shows surface bases and their beacon count beside stations.
- **Lunar shuttle:** a single reusable stage (vacuum engine, medium tank, landing legs, capsule, RCS thrusters). It flies between a station in Moon orbit and a surface base on one tank, docks at a port and refuels from the depot. → [Space Station](space_station)
- **Mars lander:** methalox vacuum engine, large tank, landing legs, heat shield, capsule, parachute and RCS thrusters. It flies from Mars orbit to the surface and, refuelled with methane and oxygen made on Mars, back to orbit. → [In-Situ Resources](isru)

## Reentry
The heat shield's rating matters: one survives a return from **low orbit**, a better one a direct return **from the Moon**. Its tooltip tells you which.

{items spacebreaker:emergency_beacon}

**Emergency return beacon:** away from Earth, it calls a capsule that brings you home. It does nothing on Earth.
