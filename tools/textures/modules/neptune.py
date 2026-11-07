"""Triton: frosts, the pink polar frost, plume dust, bedrock ice, the nitrogen geyser, clathrate ore and item, the plume
beetle, and Neptune's disc for the sky. All drawn by code, vanilla-close."""
import math
import random

from sbtex.core import Canvas, clustered, fbm, hexc
from sbtex import palettes as P
from modules.moon import quant, white, blend, footprint, overlay
from modules.mars import dust, prints
from modules.moon_creatures import paint, noise_pick
from modules.jupiter import frost, ice_block, _reduce


def triton_frost(v=0):
    return frost(P.TRITON_FROST, 14301 + v * 19)


def pink_nitrogen_frost(v=0):
    return frost(P.PINK_FROST, 14401 + v * 23)


def plume_dust(v=0):
    return dust(P.PLUME_DUST, 14501 + v * 29)


def triton_ice():
    r = P.TRITON_ICE
    f = blend((clustered(16, 16, 14601, passes=1), 0.55), (white(14602), 0.3), (fbm(16, 16, 14603, [(8, 1.0)]), 0.15))
    cv, _ = quant(f, [r[0], r[1], r[2], r[3], r[4], r[5]], [3, 12, 32, 32, 15, 6])
    return cv


def geyser_top():
    """A vent in pink frost: dark throat, a ring of dark dust around it."""
    r = P.PINK_FROST
    d = P.PLUME_DUST
    cv = _reduce(frost(r, 14701), [r[2], r[3], r[4]])
    for y in range(16):
        for x in range(16):
            k = math.hypot(x - 7.5, y - 7.5)
            if k < 2.4:
                cv[x, y] = d[0] if k < 1.5 else d[2]
            elif k < 4.4 and (x * 3 + y) % 4:
                cv[x, y] = d[3]
    return cv


def geyser_side():
    r = P.TRITON_ICE
    cv = _reduce(triton_ice(), [r[1], r[2], r[3], r[4]])
    for x in range(16):
        cv[x, 0] = P.PLUME_DUST[2]
        if x % 3 == 1:
            cv[x, 1] = P.PLUME_DUST[3]
    return cv


CLA = [
    (2, 2, [(0, 0, 3), (1, 0, 4), (0, 1, 2), (1, 1, 1)]),
    (10, 3, [(0, 0, 4), (1, 0, 3), (1, 1, 1)]),
    (5, 9, [(0, 0, 3), (1, 0, 4), (2, 0, 3), (1, 1, 2), (2, 1, 0)]),
    (12, 11, [(0, 0, 4), (0, 1, 2), (1, 1, 0)]),
]


def clathrate_overlay():
    return overlay(CLA, [P.CLATHRATE[0], P.CLATHRATE[1], P.CLATHRATE[2], P.CLATHRATE[3], P.CLATHRATE[3]])


def methane_clathrate():
    """A chunk of milky clathrate ice: pale cages around yellowish methane, one bright facet."""
    r = P.CLATHRATE
    cv = Canvas()
    for y in range(3, 13):
        for x in range(3, 13):
            if abs(x - 7.5) + abs(y - 7.5) > 6.5:
                continue
            t = 2 + (1 if (x + y) % 3 == 0 else 0) + (1 if x < 7 and y < 7 else 0) - (1 if x > 9 or y > 9 else 0)
            cv[x, y] = r[max(0, min(4, t))]
    for x, y in [(5, 5), (6, 5), (5, 6)]:
        cv[x, y] = r[4]
    return cv


def plume_beetle():
    """64x32 sheet for PlumeBeetleModel: near-black body, wing cases with a faint pink sheen of frost, pale horns."""
    cv = Canvas(64, 32)
    r = P.BEETLE
    pink = P.PINK_FROST
    n = clustered(64, 64, 14801, passes=1)
    paint(cv, 0, 0, 8, 3, 10, lambda f, x, y, fw, fh: r[1] if f == 'bottom' else noise_pick(r, 2, n, x, y))

    def head(face, x, y, fw, fh):
        if face == 'front' and y == 1 and x in (1, 3):
            return pink[3]
        return noise_pick(r, 3, n, x + 30, y)

    paint(cv, 0, 13, 5, 3, 3, head)
    paint(cv, 16, 13, 1, 2, 1, lambda f, x, y, fw, fh: pink[4])

    def case(face, x, y, fw, fh):
        if face == 'top':
            if n[y + 10][x + 40] > 0.82:
                return pink[1]
            return r[5] if x == 0 else noise_pick(r, 4, n, x + 20, y)
        return noise_pick(r, 3, n, x + 50, y + 5)

    paint(cv, 28, 0, 4, 3, 9, case)
    paint(cv, 0, 20, 4, 1, 1, lambda f, x, y, fw, fh: r[3] if f == 'top' else r[2])
    return cv


def neptune_disc(size=256):
    """Neptune: deep blue, a few soft lighter and darker bands, a dark oval storm and bright white cloud streaks."""
    from PIL import Image
    rng = random.Random(14901)
    deep = hexc('#2f4fa8')
    blue = hexc('#3a5fbf')
    light = hexc('#4f74cf')
    dark = hexc('#253f8c')
    polar = hexc('#2a458f')
    white_c = hexc('#dfe8f5')
    bands = [(-1.0, -0.7, polar), (-0.7, -0.45, blue), (-0.45, -0.25, light), (-0.25, 0.1, blue), (0.1, 0.3, deep), (0.3, 0.55, light),
             (0.55, 0.75, dark), (0.75, 1.0, polar)]
    streaks = [(rng.uniform(-0.6, 0.6), rng.uniform(-0.6, 0.6), rng.uniform(0.12, 0.3)) for _ in range(6)]
    img = Image.new('RGBA', (size, size), (0, 0, 0, 0))
    px = img.load()
    c = (size - 1) / 2
    for y in range(size):
        for x in range(size):
            u = (x - c) / c
            v = (y - c) / c
            if math.hypot(u, v) > 1:
                continue
            vv = v + 0.01 * math.sin(u * 9 + v * 5)
            col = blue
            for top, bot, colour in bands:
                if top <= vv < bot:
                    col = colour
            # the dark storm with a white companion cloud
            if ((u + 0.25) / 0.16) ** 2 + ((v - 0.22) / 0.07) ** 2 < 1:
                col = dark if ((u + 0.25) / 0.16) ** 2 + ((v - 0.22) / 0.07) ** 2 > 0.3 else hexc('#1d3272')
            if ((u + 0.24) / 0.09) ** 2 + ((v - 0.31) / 0.025) ** 2 < 1:
                col = white_c
            for su, sv, sl in streaks:
                if abs(v - sv) < 0.012 and abs(u - su) < sl:
                    col = white_c
            px[x, y] = (col[0], col[1], col[2], 255)
    return img


# --------------------------------------------------------------------------- Proteus

def proteus_regolith(v=0):
    return dust(P.PROTEUS_SOIL, 14801 + v * 29)


def proteus_crust():
    r = P.PROTEUS_CRUST
    f = blend((clustered(16, 16, 14901, passes=1), 0.55), (white(14901 + 1), 0.3), (fbm(16, 16, 14901 + 2, [(8, 1.0)]), 0.15))
    cv, _ = quant(f, [r[0], r[1], r[2], r[3], r[4], r[5]], [3, 12, 32, 32, 15, 6])
    return cv


# --------------------------------------------------------------------------- Nereid

def nereid_frost(v=0):
    return dust(P.NEREID_FROST, 15001 + v * 29)


def nereid_ice():
    r = P.NEREID_ICE
    f = blend((clustered(16, 16, 15101, passes=1), 0.55), (white(15102), 0.3), (fbm(16, 16, 15103, [(8, 1.0)]), 0.15))
    cv, _ = quant(f, [r[0], r[1], r[2], r[3], r[4], r[5]], [3, 12, 32, 32, 15, 6])
    return cv


def export(save):
    dust_spec = (18, 8, 64, None)
    for name, fn, r in (('triton_frost', triton_frost, P.TRITON_FROST), ('pink_nitrogen_frost', pink_nitrogen_frost, P.PINK_FROST),
                        ('plume_dust', plume_dust, P.PLUME_DUST)):
        base = fn(0)
        save(base, 'block', name, dust_spec)
        save(fn(1), 'block', name + '_1', dust_spec)
        for side in ('left', 'right'):
            save(footprint(base, prints(r), side), 'block', f'{name}_footprint_{side}', (22, 8, 64, None))
    save(triton_ice(), 'block', 'triton_ice', (40, 12, 20, None))
    save(geyser_top(), 'block', 'nitrogen_geyser_top', (60, 12, 20, None))
    save(geyser_side(), 'block', 'nitrogen_geyser_side', (40, 12, 20, None))
    save(clathrate_overlay(), 'block', 'methane_clathrate_ore_overlay', (100, 20, 0, None), wrap=False)
    save(methane_clathrate(), 'item', 'methane_clathrate')
    save(plume_beetle(), 'entity', 'neptune/plume_beetle')
    for name, fn, r in (('proteus_regolith', proteus_regolith, P.PROTEUS_SOIL),):
        base = fn(0)
        save(base, 'block', name, dust_spec)
        save(fn(1), 'block', name + '_1', dust_spec)
        for side in ('left', 'right'):
            save(footprint(base, prints(r), side), 'block', f'{name}_footprint_{side}', (22, 8, 64, None))
    save(proteus_crust(), 'block', 'proteus_crust', (40, 12, 20, None))
    for name, fn, r in (('nereid_frost', nereid_frost, P.NEREID_FROST),):
        base = fn(0)
        save(base, 'block', name, dust_spec)
        save(fn(1), 'block', name + '_1', dust_spec)
        for side in ('left', 'right'):
            save(footprint(base, prints(r), side), 'block', f'{name}_footprint_{side}', (22, 8, 64, None))
    save(nereid_ice(), 'block', 'nereid_ice', (40, 12, 20, None))
    import os
    out = os.path.join(os.path.dirname(__file__), '..', '..', '..', 'src', 'main', 'resources', 'assets', 'spacebreaker', 'textures',
                       'environment')
    os.makedirs(out, exist_ok=True)
    neptune_disc().save(os.path.join(out, 'neptune_disc.png'))
