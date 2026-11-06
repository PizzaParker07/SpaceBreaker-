"""Phobos and Deimos: dark carbonaceous regolith (with boot prints), chondrite rock, and Mars as seen in their sky."""
import math
import random

from sbtex.core import Canvas, ramp, clustered, fbm
from modules.moon import quant, white, blend, footprint
from modules.mars import dust, prints

# Phobos: very dark, slightly warm grey-brown (albedo 0.07).
PHOBOS_SOIL = ramp('#1f1c1a', '#2e2a27', '#332f2b', '#38332f', '#3d3833', '#433d38')
# Deimos: smoother, a shade lighter and greyer.
DEIMOS_SOIL = ramp('#2a2826', '#3b3835', '#413e3a', '#47433f', '#4d4945', '#544f4a')
# Carbonaceous chondrite: black matrix, pale chondrules, white refractory inclusions.
CHONDRITE = ramp('#151313', '#1f1c1b', '#292524', '#332e2c', '#4d4641', '#7e766c', '#b8b0a2')


def phobos_regolith(v=0):
    return dust(PHOBOS_SOIL, 4101 + v * 31)


def deimos_regolith(v=0):
    f = blend((clustered(16, 16, 4201 + v, passes=2), 0.55), (white(4202 + v), 0.45))
    cv, _ = quant(f, DEIMOS_SOIL, [4, 18, 36, 28, 11, 3])
    return cv


def chondrite():
    """Dark fine matrix packed with round chondrules (lit top-left rim) and a few white CAI flecks."""
    r = CHONDRITE
    f = blend((white(4301), 0.6), (clustered(16, 16, 4302), 0.4))
    cv, _ = quant(f, [r[1], r[2], r[3]], [30, 45, 25])
    for x, y in [(2, 2), (9, 4), (13, 10), (4, 11), (10, 13)]:
        cv[x, y] = r[5]
        cv[x + 1, y] = r[4]
        cv[x, y + 1] = r[4]
        cv[x + 1, y + 1] = r[3]
    for x, y in [(7, 1), (14, 6), (1, 7), (6, 8)]:
        cv[x, y] = r[6]
    return cv


# --------------------------------------------------------------------------- Mars in the sky

MARS_DISC = ramp('#4a2416', '#6b331d', '#8a4426', '#a65631', '#bd6b3e', '#cf8652')
MARS_DARK = ramp('#3a2a22', '#4f3a2e')
CAP = ramp('#d8d4cf', '#f1efec')


def mars_disc(size=64):
    """Mars as a full disc: rusty plains, dark albedo markings, the rift line, white polar caps. Shading is applied
    in game per vertex (phase), so the texture is evenly lit."""
    cv = Canvas(size, size)
    n = fbm(size, size, 4401, [(16, 1.0), (8, 0.5), (4, 0.25)])
    rng = random.Random(4402)
    c = (size - 1) / 2
    for y in range(size):
        for x in range(size):
            dx = (x - c) / c
            dy = (y - c) / c
            d = math.hypot(dx, dy)
            if d > 1:
                continue
            lat = -dy
            v = n[y][x]
            if abs(lat) > 0.86 - 0.05 * v:
                col = CAP[1] if abs(lat) > 0.92 else CAP[0]
            elif v < 0.24 and -0.45 < lat < 0.3:
                col = MARS_DARK[0] if v < 0.16 else MARS_DARK[1]
            else:
                col = MARS_DISC[2 + min(3, int(v * 4))]
            # Valles Marineris: a dark scar just south of the equator
            if abs(lat + 0.1 + 0.03 * math.sin(dx * 6)) < 0.025 and -0.55 < dx < 0.35:
                col = MARS_DISC[0]
            # limb darkening of the dusty atmosphere
            if d > 0.93:
                col = MARS_DISC[1] if col not in CAP else CAP[0]
            cv[x, y] = col
    return cv


def export(save):
    dust_spec = (18, 8, 64, None)
    for name, fn, r in (('phobos_regolith', phobos_regolith, PHOBOS_SOIL), ('deimos_regolith', deimos_regolith, DEIMOS_SOIL)):
        base = fn(0)
        save(base, 'block', name, dust_spec)
        save(fn(1), 'block', name + '_1', dust_spec)
        for side in ('left', 'right'):
            save(footprint(base, prints(r), side), 'block', f'{name}_footprint_{side}', (22, 8, 64, None))
    save(chondrite(), 'block', 'carbonaceous_chondrite', (30, 10, 30, None))
    save(mars_disc(), 'environment', 'mars_disc')
