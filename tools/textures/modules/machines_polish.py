"""Second pass on the plainest machines (crusher, battery bank, regolith heater, sintering furnace, regolith
excavator, ice melter): parts textures for their new silhouettes and the animated strips they show while running.
Runs after machines_industry / machines_isru (module order), so it only adds new textures."""
import math
import os
import random

from sbtex.core import Canvas
from sbtex import palettes as P
from modules.machines_industry import S, GLOW, HAZ_Y, casing, rect, recess, hazard, glow_overlay

BRICK = [(120, 52, 38), (150, 70, 50)]
ROCK = P.REGOLITH
ICE = [(150, 190, 215), (200, 222, 240), (232, 242, 250)]
WATER = [(38, 78, 140), (52, 98, 165), (70, 122, 186)]


def strip(frames):
    return Canvas(16, 16 * frames)


def frame(cv, f, fn):
    """Draws one 16x16 frame: fn(sub) on a fresh canvas pasted at row f."""
    sub = Canvas()
    fn(sub)
    cv.paste(sub, 0, f * 16)


def mcmeta(name, frametime, interpolate=False):
    from generate import TEX
    with open(os.path.join(TEX, 'block', name + '.png.mcmeta'), 'w') as fh:
        fh.write('{\n  "animation": {"frametime": %d%s}\n}\n' % (frametime, ', "interpolate": true' if interpolate else ''))


def wheel(cv, cx, cy, r, spokes, angle, rim, spoke, hub, buckets=None):
    """A spoked wheel turned by `angle` radians, drawn on whole pixels; the gaps between the spokes stay transparent
    so the housing shows through. `buckets` (dark, lip) adds digging buckets on the rim at every spoke."""
    for y in range(cv.h):
        for x in range(cv.w):
            dx, dy = x + 0.5 - cx, y + 0.5 - cy
            d = math.hypot(dx, dy)
            if d > r:
                continue
            a = (math.atan2(dy, dx) - angle) % (2 * math.pi)
            sector = a / (2 * math.pi) * spokes
            off = abs(sector - round(sector))
            if buckets and d > r - 2.2 and off < 0.28:
                cv[x, y] = buckets[1] if d > r - 0.9 else buckets[0]
            elif d > r - 1.3:
                cv[x, y] = rim
            elif d < 2.0:
                cv[x, y] = hub
            elif d * math.sin(off * 2 * math.pi / spokes) < 0.75:
                cv[x, y] = spoke


# ----------------------------------------------------------------------------- crusher

def crusher_jaws(cv, phase):
    """Front of the crusher: the jaw opening with teeth; `phase` 0..3 opens and closes the jaws by a pixel."""
    base = casing(12, seam=False)
    cv.paste(base)
    recess(cv, 3, 3, 12, 9, S[0])
    gap = (0, 1, 1, 0)[phase]
    for x in range(3, 13):
        t = (x + phase) % 2
        cv[x, 3 + t + gap] = S[3]
        cv[x, 3 + gap] = S[3]
        cv[x, 9 - t - gap] = S[3]
        cv[x, 9 - gap] = S[3]
    hazard(cv, 12, 13)


def crusher_front_on():
    cv = strip(4)
    for f in range(4):
        frame(cv, f, lambda c, f=f: crusher_jaws(c, f))
    return cv


def crusher_flywheel(frames=1):
    cv = strip(frames)
    for f in range(frames):
        frame(cv, f, lambda c, f=f: wheel(c, 8, 8, 5.4, 4, f * math.pi / 8, S[5], S[3], S[6]))
    return cv


def crusher_hopper():
    """Looking down the feed hopper: sloped steel walls into a dark throat with broken rock."""
    cv = Canvas()
    rnd = random.Random(5)
    for y in range(16):
        for x in range(16):
            d = max(abs(x - 7.5), abs(y - 7.5))
            cv[x, y] = S[5] if d > 6.5 else (S[3] if d > 4.5 else S[0])
    for y in range(4, 12):
        for x in range(4, 12):
            r = rnd.random()
            cv[x, y] = S[0] if r < 0.25 else (ROCK[1] if r < 0.6 else (ROCK[3] if r < 0.9 else ROCK[5]))
    return cv


# ----------------------------------------------------------------------------- battery bank

def battery_cell():
    """Side of a cell stack: vertical cooling ribs between two clamping bands."""
    cv = Canvas()
    for y in range(16):
        for x in range(16):
            cv[x, y] = S[4] if x % 3 == 0 else (S[3] if x % 3 == 1 else S[2])
    for x in range(16):
        cv[x, 2] = S[5]
        cv[x, 13] = S[5]
        cv[x, 3] = S[1]
        cv[x, 14] = S[1]
    return cv


def battery_terminal():
    cv = Canvas()
    for y in range(16):
        for x in range(16):
            cv[x, y] = (170, 40, 30) if x < 8 else (40, 40, 44)
    return cv


def battery_flow():
    """A green chase light running along the bottom strip while power flows in or out."""
    cv = strip(4)
    for f in range(4):
        def draw(c, f=f):
            for x in range(2, 14):
                if (x - f) % 4 == 0:
                    c[x, 14] = (150, 240, 160)
                elif (x - f) % 4 == 1:
                    c[x, 14] = (90, 220, 110)
        frame(cv, f, draw)
    return cv


# ----------------------------------------------------------------------------- regolith heater

def heater_glow_on():
    """Crucible slit flicker: four frames, blended."""
    cv = strip(4)
    for f in range(4):
        rnd = random.Random(30 + f)

        def draw(c, rnd=rnd):
            for x in range(5, 11):
                c[x, 8] = GLOW[3] if rnd.random() < 0.5 else GLOW[2]
                c[x, 9] = GLOW[1] if rnd.random() < 0.7 else GLOW[2]
        frame(cv, f, draw)
    return cv


def regolith_hopper():
    """Feed hopper seen from above: steel lip around a heap of grey regolith."""
    cv = Canvas()
    rnd = random.Random(9)
    for y in range(16):
        for x in range(16):
            d = max(abs(x - 7.5), abs(y - 7.5))
            if d > 6.5:
                cv[x, y] = S[5]
            elif d > 5.5:
                cv[x, y] = S[2]
            else:
                cv[x, y] = ROCK[3] if rnd.random() < 0.55 else (ROCK[5] if rnd.random() < 0.5 else ROCK[1])
    return cv


def flue_top():
    cv = Canvas()
    for y in range(16):
        for x in range(16):
            d = max(abs(x - 7.5), abs(y - 7.5))
            cv[x, y] = S[4] if d > 6.5 else S[0]
    return cv


def ember_on():
    """Hot mouth of a flue or chimney, seen from above: a dim ember that breathes."""
    cv = strip(4)
    for f in range(4):
        def draw(c, f=f):
            for y in range(16):
                for x in range(16):
                    d = max(abs(x - 7.5), abs(y - 7.5))
                    if d <= 5.5:
                        c[x, y] = GLOW[0] if d > 3.5 - f % 2 else GLOW[1 + (f == 2)]
        frame(cv, f, draw)
    return cv


# ----------------------------------------------------------------------------- sintering furnace

def sinter_top():
    """Kiln lid: refractory brick crown with a steel band and a lifting lug."""
    cv = casing(82, rivets=True, seam=False)
    for y in range(3, 13):
        for x in range(3, 13):
            cv[x, y] = S[2] if (y % 3 == 0 or (x + (2 if (y // 3) % 2 else 0)) % 4 == 0) else BRICK[(x + y) % 2 == 0]
    rect(cv, 6, 7, 9, 8, S[5])
    return cv


def sinter_glow_on():
    cv = strip(4)
    for f in range(4):
        rnd = random.Random(80 + f)

        def draw(c, rnd=rnd):
            for y in range(4, 12):
                for x in range(4, 12):
                    if y < 6 and x in (4, 11):
                        continue
                    v = rnd.random() + (y - 4) * 0.06
                    c[x, y] = GLOW[3] if v > 1.05 else (GLOW[2] if v > 0.7 else (GLOW[1] if v > 0.35 else GLOW[0]))
        frame(cv, f, draw)
    return cv


# ----------------------------------------------------------------------------- regolith excavator

def excavator_wheel(frames=1):
    cv = strip(frames)
    for f in range(frames):
        frame(cv, f, lambda c, f=f: wheel(c, 8, 8, 6.0, 8, f * (math.pi / 16), S[4], S[3], S[5], buckets=(S[1], S[6])))
    return cv


def excavator_body():
    cv = casing(72, seam=False)
    rect(cv, 3, 4, 12, 9, S[2])
    rect(cv, 4, 5, 11, 8, S[1])
    for x in range(1, 15):
        cv[x, 13] = HAZ_Y if (x // 2) % 2 == 0 else S[1]
    return cv


def chute():
    """Conveyor chute on top: a belt with cleats."""
    cv = Canvas()
    for y in range(16):
        for x in range(16):
            cv[x, y] = S[4] if x in (0, 15) else (S[3] if y % 4 == 0 else S[1])
    return cv


def chute_on():
    cv = strip(4)
    for f in range(4):
        def draw(c, f=f):
            for y in range(16):
                for x in range(16):
                    c[x, y] = S[4] if x in (0, 15) else (S[3] if (y + f) % 4 == 0 else (ROCK[3] if (x * 7 + y + f) % 9 == 0 else S[1]))
        frame(cv, f, draw)
    return cv


# ----------------------------------------------------------------------------- ice melter

def melter_tank_on():
    """Meltwater tank while heating: ice floes bob and bubbles rise from the coil."""
    cv = strip(4)
    for f in range(4):
        def draw(c, f=f):
            for y in range(16):
                for x in range(16):
                    col = WATER[1] if 2 < x < 13 else WATER[0]
                    if x in (0, 15):
                        col = S[3]
                    elif y < 3:
                        col = ICE[0]
                    c[x, y] = col
            for x, y in ((4, 3), (5, 3), (9, 3), (10, 3), (11, 3)):
                c[x + (f // 2), y + (f % 2)] = ICE[1]
            for k in range(6):
                bx = 2 + (k * 5) % 11
                by = 15 - ((f * 3 + k * 5) % 11)
                c[bx, by] = WATER[2]
        frame(cv, f, draw)
    return cv


def melter_top():
    """Ice hopper on the lid: chunks of dirty ice in a steel frame."""
    cv = Canvas()
    rnd = random.Random(14)
    for y in range(16):
        for x in range(16):
            d = max(abs(x - 7.5), abs(y - 7.5))
            if d > 6.5:
                cv[x, y] = S[5]
            elif d > 5.5:
                cv[x, y] = S[2]
            else:
                r = rnd.random()
                cv[x, y] = ICE[1] if r < 0.45 else (ICE[0] if r < 0.85 else ICE[2])
    return cv


def coil_glow_on():
    cv = strip(4)
    for f in range(4):
        frame(cv, f, lambda c, f=f: [c.__setitem__((x, y), GLOW[(x + y + f) % 2 + (1 if f == 2 else 0)])
                                     for y in (13, 14) for x in range(2, 14) if (x + y) % 2 == 0])
    return cv


def export(save):
    metal = (150, 230, 0, None)
    save(crusher_front_on(), 'block', 'crusher_front_on')
    mcmeta('crusher_front_on', 2)
    save(crusher_flywheel(), 'block', 'crusher_flywheel', metal)
    save(crusher_flywheel(4), 'block', 'crusher_flywheel_on')
    mcmeta('crusher_flywheel_on', 2)
    save(crusher_hopper(), 'block', 'crusher_hopper', metal)

    save(battery_cell(), 'block', 'battery_bank_cell', metal)
    save(battery_terminal(), 'block', 'battery_bank_terminal', metal)
    save(battery_flow(), 'block', 'battery_bank_flow')
    mcmeta('battery_bank_flow', 3)

    save(heater_glow_on(), 'block', 'regolith_heater_glow_on')
    mcmeta('regolith_heater_glow_on', 4, True)
    save(regolith_hopper(), 'block', 'regolith_heater_hopper', metal)
    save(flue_top(), 'block', 'machine_flue_top', metal)
    save(ember_on(), 'block', 'machine_flue_ember')
    mcmeta('machine_flue_ember', 6, True)

    save(sinter_top(), 'block', 'sintering_furnace_top', metal)
    save(sinter_glow_on(), 'block', 'sintering_furnace_glow_on')
    mcmeta('sintering_furnace_glow_on', 4, True)

    save(excavator_wheel(), 'block', 'regolith_excavator_wheel', metal)
    save(excavator_wheel(4), 'block', 'regolith_excavator_wheel_on')
    mcmeta('regolith_excavator_wheel_on', 2)
    save(excavator_body(), 'block', 'regolith_excavator_body', metal)
    save(chute(), 'block', 'regolith_excavator_chute', metal)
    save(chute_on(), 'block', 'regolith_excavator_chute_on')
    mcmeta('regolith_excavator_chute_on', 3)

    save(melter_tank_on(), 'block', 'ice_melter_tank_on')
    mcmeta('ice_melter_tank_on', 4)
    save(melter_top(), 'block', 'ice_melter_top', metal)
    save(coil_glow_on(), 'block', 'ice_melter_glow_on')
    mcmeta('ice_melter_glow_on', 5, True)
