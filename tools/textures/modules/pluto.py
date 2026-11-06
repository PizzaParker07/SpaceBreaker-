"""Pluto and Charon: frosts and ices, the tholin regolith, ores, the ammonia crystal, the two creatures and the sky discs.
All drawn by code, vanilla-close: calm noise, few tones, one lit and one shaded edge."""
import math
import random

from sbtex.core import Canvas, clustered, fbm, mask_from, parse_mask, dome_shade, edge_pixels, pick, hexc
from sbtex import palettes as P
from modules.moon import quant, white, blend, blur_dir, voronoi, footprint, overlay, RAW_CHUNK
from modules.mars import dust, prints
from modules.moon_creatures import paint, noise_pick
from modules.jupiter import frost, ice_block
from modules.belt import body_disc


# --------------------------------------------------------------------------- Pluto

def plutonian_frost(v=0):
    return frost(P.PLUTO_FROST, 13101 + v * 19)


def tholin_regolith(v=0):
    """Dark red-brown dust of the equatorial lowlands: tholins rained out of the haze over millions of years."""
    return dust(P.PLUTO_THOLIN, 13201 + v * 23)


def nitrogen_ice():
    """Glassy nitrogen ice of the great plain: very calm, faint polygon cracks, a soft blue sheen."""
    r = P.NITROGEN_ICE
    cv = ice_block(r, 13301, cracks=[[(2, 9), (3, 9), (4, 10)], [(11, 2), (12, 3)]])
    return cv


def methane_frost():
    return frost(P.METHANE_FROST, 13401)


def plutonian_ice():
    """Water-ice bedrock: grey-beige, granular like stone, a few darker inclusions."""
    r = P.PLUTO_ICE
    f = blend((clustered(16, 16, 13501, passes=1), 0.55), (white(13502), 0.3), (fbm(16, 16, 13503, [(8, 1.0)]), 0.15))
    cv, _ = quant(f, [r[0], r[1], r[2], r[3], r[4], r[5]], [3, 10, 30, 34, 16, 7])
    return cv


THO = [
    (2, 3, [(0, 0, 2), (1, 0, 3), (0, 1, 1), (1, 1, 0)]),
    (10, 2, [(0, 0, 3), (1, 0, 2), (1, 1, 1), (2, 1, 0)]),
    (5, 9, [(0, 0, 2), (1, 0, 3), (2, 0, 2), (1, 1, 1), (2, 1, 0)]),
    (12, 11, [(0, 0, 3), (0, 1, 1), (1, 1, 0)]),
]


def plutonian_tholin_overlay():
    r = P.PLUTO_THOLIN
    return overlay(THO, [r[1], r[3], r[4], r[5]])


# --------------------------------------------------------------------------- Charon

def charon_regolith(v=0):
    return dust(P.CHARON_SOIL, 13601 + v * 29)


def charon_ice():
    r = P.CHARON_ICE
    f = blend((clustered(16, 16, 13701, passes=1), 0.55), (white(13702), 0.3), (fbm(16, 16, 13703, [(8, 1.0)]), 0.15))
    cv, _ = quant(f, [r[0], r[1], r[2], r[3], r[4], r[5]], [3, 12, 32, 32, 15, 6])
    return cv


def red_polar_frost(v=0):
    return frost(P.RED_FROST, 13801 + v * 31)


AMM = [
    (3, 2, [(0, 0, 4), (0, 1, 3), (1, 1, 2), (0, 2, 1)]),
    (11, 4, [(0, 0, 4), (1, 0, 3), (0, 1, 2), (1, 1, 1)]),
    (6, 9, [(1, 0, 4), (0, 1, 3), (1, 1, 3), (2, 1, 2), (1, 2, 1)]),
    (12, 12, [(0, 0, 4), (0, 1, 2)]),
]


def ammonia_overlay():
    r = P.AMMONIA
    return overlay(AMM, [r[0], r[1], r[2], r[3], r[4]])


def ammonia_crystal():
    """A small cluster of pale cyan crystals: three prisms, lit on the left, shaded on the right."""
    r = P.AMMONIA
    cv = Canvas()
    prisms = [(5, 3, 11, 3), (9, 5, 12, 2), (3, 7, 12, 2)]
    for x0, y0, y1, w in prisms:
        for y in range(y0, y1 + 1):
            for x in range(x0, x0 + w):
                t = 4 if x == x0 else (1 if x == x0 + w - 1 else 3)
                if y == y0:
                    t = 4
                cv[x, y] = r[t]
        cv[x0, y1 + 1] = r[0]
    for x in range(3, 13):
        if cv.img.getpixel((x, 13))[3] == 0:
            cv[x, 13] = r[0]
    return cv


# --------------------------------------------------------------------------- creatures

def nitrogen_slider():
    """64x32 sheet for NitrogenSliderModel: pale beige back with darker red-brown blotches, a light belly, dark eyes."""
    cv = Canvas(64, 32)
    r = P.SLIDER
    n = clustered(64, 64, 13901, passes=1)

    def body(face, x, y, fw, fh):
        if face == 'bottom':
            return r[5]
        if face == 'top':
            return r[1] if n[y][x] > 0.78 else noise_pick(r, 4, n, x, y)
        if y >= fh - 1:
            return r[5]
        return r[2] if n[y + 20][x] > 0.8 else noise_pick(r, 3, n, x + 20, y + 30)

    paint(cv, 0, 0, 8, 5, 13, body)

    def head(face, x, y, fw, fh):
        if face == 'front' and fw == 6 and y == 1 and x in (1, 4):
            return hexc('#14100d')
        if face == 'bottom':
            return r[5]
        return noise_pick(r, 4 if face == 'top' else 3, n, x + 40, y)

    paint(cv, 0, 18, 6, 5, 5, head)
    paint(cv, 22, 18, 3, 2, 2, lambda f, x, y, fw, fh: r[2] if f == 'front' and y == 0 else r[3])
    paint(cv, 34, 0, 5, 1, 3, lambda f, x, y, fw, fh: r[2] if f == 'top' else r[1])
    paint(cv, 34, 6, 5, 3, 4, body)
    paint(cv, 34, 14, 8, 1, 3, lambda f, x, y, fw, fh: r[2] if f == 'top' else r[1])
    return cv


def frost_tortoise():
    """64x32 sheet for FrostTortoiseModel: dark grey scuted shell with dark red frost on the upper tiers, grey skin."""
    cv = Canvas(64, 32)
    r = P.TORTOISE
    red = P.RED_FROST
    n = clustered(64, 64, 14001, passes=1)

    def plate(top_red):
        def f(face, x, y, fw, fh):
            if face == 'bottom':
                return r[1]
            if face == 'top':
                # scutes: a dark seam every 4 pixels
                if x % 4 == 0 or y % 4 == 0:
                    return r[1]
                if top_red and n[y][x + 10] > 0.35:
                    return noise_pick(red, 4, n, x, y)
                return noise_pick(r, 4, n, x, y + 7)
            return r[2] if y == fh - 1 else noise_pick(r, 3, n, x + 30, y)
        return f

    paint(cv, 0, 0, 14, 3, 14, plate(False))
    paint(cv, 0, 17, 10, 3, 10, plate(True))
    paint(cv, 40, 17, 6, 2, 6, plate(True))

    def head(face, x, y, fw, fh):
        if face == 'front' and y == 1 and x in (0, 3):
            return hexc('#0e0c0a')
        return noise_pick(r, 4 if face == 'top' else 3, n, x + 50, y + 3)

    paint(cv, 42, 0, 4, 4, 5, head)
    paint(cv, 42, 9, 3, 3, 3, lambda f, x, y, fw, fh: r[2] if f == 'bottom' else noise_pick(r, 3, n, x + 52, y + 9))
    return cv


# --------------------------------------------------------------------------- sky discs

def pluto_disc():
    """Pluto as seen from Charon's near side: beige, the bright heart in the middle-right, dark red belt below it."""
    tones = [hexc(h) for h in ('#5b4636', '#7a6450', '#988069', '#b19a82', '#c7b39b')]
    heart = hexc('#ece6dc')
    red = hexc('#5a2a1c')
    spots = [(0.18, 0.05, 0.3, heart), (0.38, -0.08, 0.24, heart), (0.3, 0.28, 0.2, heart)]
    for i in range(7):
        a = -0.9 + i * 0.22
        spots.append((a, 0.42 + 0.06 * math.sin(i * 1.7), 0.17, red))
    return body_disc(128, tones, 14101, spots=spots)


def charon_disc():
    """Charon: neutral grey, a dark red polar cap at the top, a darker band of chasms across the middle."""
    tones = [hexc(h) for h in ('#3e3d3b', '#52504e', '#66645f', '#79766f', '#8b8780')]
    red = hexc('#4a2116')
    spots = [(0.0, -1.05, 0.42, red), (-0.25, -0.75, 0.12, red), (0.3, -0.72, 0.1, red)]
    return body_disc(128, tones, 14201, spots=spots)


# --------------------------------------------------------------------------- export

def export(save):
    dust_spec = (18, 8, 64, None)
    for name, fn, r in (('plutonian_frost', plutonian_frost, P.PLUTO_FROST), ('tholin_regolith', tholin_regolith, P.PLUTO_THOLIN),
                        ('charon_regolith', charon_regolith, P.CHARON_SOIL), ('red_polar_frost', red_polar_frost, P.RED_FROST)):
        base = fn(0)
        save(base, 'block', name, dust_spec)
        save(fn(1), 'block', name + '_1', dust_spec)
        for side in ('left', 'right'):
            save(footprint(base, prints(r), side), 'block', f'{name}_footprint_{side}', (22, 8, 64, None))
    save(nitrogen_ice(), 'block', 'nitrogen_ice', (200, 20, 0, None))
    save(methane_frost(), 'block', 'methane_frost', (18, 8, 64, None))
    save(plutonian_ice(), 'block', 'plutonian_ice', (40, 12, 20, None))
    save(plutonian_tholin_overlay(), 'block', 'plutonian_tholin_ore_overlay', (90, 20, 0, None), wrap=False)
    save(charon_ice(), 'block', 'charon_ice', (40, 12, 20, None))
    save(ammonia_overlay(), 'block', 'ammonia_hydrate_ore_overlay', (120, 20, 0, None), wrap=False)
    save(ammonia_crystal(), 'item', 'ammonia_crystal')
    save(nitrogen_slider(), 'entity', 'pluto/nitrogen_slider')
    save(frost_tortoise(), 'entity', 'pluto/frost_tortoise')
    import os
    out = os.path.join(os.path.dirname(__file__), '..', '..', '..', 'src', 'main', 'resources', 'assets', 'spacebreaker', 'textures',
                       'environment')
    os.makedirs(out, exist_ok=True)
    pluto_disc().save(os.path.join(out, 'pluto_disc.png'))
    charon_disc().save(os.path.join(out, 'charon_disc.png'))
