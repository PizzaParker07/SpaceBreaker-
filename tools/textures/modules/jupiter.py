"""Jupiter's moons: Europa (frost, shell ice, stained lineae ice, refrozen brine, salt ore, geysers), Io (sulfur sand,
SO2 frost, red sulfur, basalt, sulfur ore, plume vents), Ganymede (regolith, grooved ice, dark crust), Callisto
(dark regolith, bright frost, rock), the glow serpent and sulfur crab, and Jupiter's disc for the moons' skies.

Vanilla execution: 4-6 tones per block from one ramp, calm noise, light from the top-left.
"""
import math
import random

from sbtex.core import Canvas, clustered, fbm, parse_mask, mask_from, dome_shade, edge_pixels, pick, hexc
from sbtex import palettes as P
from modules.moon import quant, white, blend, blur_dir, voronoi, footprint, bricks, overlay
from modules.mars import dust, prints
from modules.moon_creatures import paint, noise_pick


# --------------------------------------------------------------------------- shared

def frost(r, seed):
    """Fine frost like vanilla snow: very calm, a few sparkling grains (lit pixel beside a shaded one)."""
    f = blend((clustered(16, 16, seed, passes=2), 0.55), (white(seed + 1), 0.45))
    cv, _ = quant(f, r, [3, 15, 34, 30, 14, 4])
    rng = random.Random(seed + 2)
    for _ in range(3):
        x, y = rng.randrange(16), rng.randrange(16)
        cv[x, y] = r[5]
        cv[(x + 1) % 16, (y + 1) % 16] = r[1]
    return cv


def ice_block(r, seed, cracks=()):
    """Opaque shell ice like packed ice: soft diagonal bands, a few hairline cracks (dark line with a lit edge)."""
    f = blend((blur_dir(clustered(16, 16, seed, passes=1), 2, 1, 1), 0.6), (fbm(16, 16, seed + 1, [(8, 1.0)]), 0.25), (white(seed + 2), 0.15))
    cv, _ = quant(f, [r[1], r[2], r[3], r[4], r[5]], [8, 24, 36, 24, 8])
    for line in cracks:
        for x, y in line:
            cv[x % 16, y % 16] = r[1]
            cv[x % 16, (y + 1) % 16] = r[5]
    return cv


# --------------------------------------------------------------------------- Europa

def europan_frost(v=0):
    return frost(P.EUROPA_FROST, 9301 + v * 17)


def europan_ice():
    return ice_block(P.EUROPA_ICE, 9401, cracks=[[(3, 9), (4, 10), (5, 10)]])


def europan_ice_bricks():
    r = P.EUROPA_ICE
    return bricks([r[0], r[1], r[2], r[3], r[4], r[5]], 9402, face=[r[2], r[3], r[4]], mortar=r[1])


def stained_ice():
    """Ice soaked with salts and sulfur from below: red-brown, in streaks along the grain."""
    r = P.STAINED_ICE
    f = blend((blur_dir(clustered(16, 16, 9501, passes=1), 2, 1, 0), 0.6), (white(9502), 0.4))
    cv, _ = quant(f, r, [4, 16, 32, 28, 14, 6])
    return cv


def refrozen_brine():
    """Chaos-terrain matrix: slushy orange-brown ice with white salt crystals frozen out of it."""
    r = P.BRINE_ICE
    f = blend((clustered(16, 16, 9601, passes=2), 0.6), (white(9602), 0.4))
    cv, _ = quant(f, r[:5], [8, 24, 36, 24, 8])
    for x, y in [(2, 2), (3, 2), (11, 5), (6, 9), (7, 9), (13, 12), (4, 13)]:
        cv[x, y] = r[5]
    return cv


SALT = [
    (2, 2, [(0, 0, 3), (1, 0, 2), (0, 1, 2), (1, 1, 1)]),
    (10, 1, [(0, 0, 3), (1, 0, 1), (0, 1, 1), (1, 1, 0)]),
    (6, 6, [(0, 0, 2), (1, 0, 3), (2, 0, 1), (0, 1, 1), (1, 1, 2), (2, 1, 0)]),
    (13, 6, [(0, 0, 3), (0, 1, 1), (1, 1, 0)]),
    (2, 10, [(0, 0, 3), (1, 0, 2), (1, 1, 0)]),
    (9, 12, [(0, 0, 3), (1, 0, 2), (0, 1, 1), (1, 1, 1), (2, 1, 0)]),
]


def salt_overlay():
    """Cubic salt crystals: white faces with one grey shaded side, no outline."""
    r = P.OCEAN_SALT
    return overlay(SALT, [r[1], r[2], r[4], r[5]])


def ocean_salt():
    """A small heap of salt crystals: three stacked cubes, light top faces, grey right faces."""
    r = P.OCEAN_SALT
    cv = Canvas()
    cubes = [(3, 8, 5), (8, 9, 5), (5, 4, 5)]
    for cx, cy, s in cubes:
        for y in range(s):
            for x in range(s):
                if y == 0 or x == 0:
                    c = r[5]
                elif x == s - 1 or y == s - 1:
                    c = r[1]
                else:
                    c = r[3] if (x + y) % 3 else r[4]
                cv[cx + x, cy + y] = c
    for cx, cy, s in cubes:
        cv[cx + 1, cy + 1] = r[5]
    return cv


def _reduce(cv, tones):
    """Snap every pixel to the nearest of a few tones (keeps feature textures within the style's colour budget)."""
    for y in range(cv.h if hasattr(cv, 'h') else 16):
        for x in range(16):
            c = cv[x, y]
            best = min(tones, key=lambda t: sum((c[i] - t[i]) ** 2 for i in range(3)))
            cv[x, y] = best
    return cv


def geyser_top():
    """A vent in the ice: dark throat, a stained lip, a rim of fresh frost, blue ice around."""
    r = P.EUROPA_ICE
    fr = P.EUROPA_FROST
    cv = _reduce(ice_block(r, 9701), [r[2], r[3], r[4]])
    for y in range(16):
        for x in range(16):
            d = math.hypot(x - 7.5, y - 7.5)
            if d < 2.6:
                cv[x, y] = r[0] if d < 1.6 else P.STAINED_ICE[2]
            elif d < 4.6:
                cv[x, y] = fr[5] if (x + y) % 3 else r[4]
    return cv


def geyser_side():
    r = P.EUROPA_ICE
    cv = _reduce(ice_block(r, 9702), [r[2], r[3], r[4], r[1]])
    for x in range(16):
        cv[x, 0] = P.EUROPA_FROST[5]
        if x % 3 == 0:
            cv[x, 1] = P.EUROPA_FROST[5]
    return cv


# --------------------------------------------------------------------------- Io

def sulfur_sand(v=0):
    return dust(P.SULFUR_SAND, 9801 + v * 19)


def so2_frost(v=0):
    return frost(P.SO2_FROST, 9901 + v * 23)


def red_sulfur(v=0):
    return dust(P.RED_SULFUR, 10001 + v * 29)


def ionian_basalt():
    """Fresh silicate lava: near black with a faint green cast, ropy streaks and gas vesicles."""
    r = P.IONIAN_BASALT
    f = blend((blur_dir(white(10101), 2, 1, 0), 0.45), (clustered(16, 16, 10102, passes=1), 0.4), (white(10103), 0.15))
    cv, _ = quant(f, [r[1], r[2], r[3], r[4], r[5]], [8, 26, 38, 22, 6])
    for x, y in [(3, 2), (11, 4), (6, 9), (14, 12), (1, 14)]:
        cv[x, y] = r[0]
        cv[x + 1, y + 1] = r[5]
    return cv


SUL = [
    (1, 1, [(0, 0, 2), (1, 0, 3), (0, 1, 1), (1, 1, 2), (2, 1, 0)]),
    (9, 2, [(0, 0, 3), (1, 0, 2), (0, 1, 1), (1, 1, 0)]),
    (13, 7, [(0, 0, 2), (0, 1, 3), (1, 1, 1), (0, 2, 0)]),
    (5, 7, [(0, 0, 3), (1, 0, 2), (2, 0, 1), (1, 1, 1), (2, 1, 0)]),
    (2, 12, [(0, 0, 2), (1, 0, 3), (1, 1, 1), (2, 1, 0)]),
    (10, 12, [(0, 0, 3), (1, 0, 1), (0, 1, 1), (1, 1, 0)]),
]


def sulfur_overlay():
    r = P.IO_SULFUR
    return overlay(SUL, [r[1], r[2], r[3], r[4]])


def vent_top():
    """A plume vent: a glowing orange throat in black basalt, ringed by yellow sulfur crust."""
    b = P.IONIAN_BASALT
    cv = _reduce(ionian_basalt(), [b[1], b[2], b[4]])
    glow = [hexc('#c4501a'), hexc('#ffc760')]
    for y in range(16):
        for x in range(16):
            d = math.hypot(x - 7.5, y - 7.5)
            if d < 1.6:
                cv[x, y] = glow[1]
            elif d < 2.8:
                cv[x, y] = glow[0]
            elif d < 4.6 and (x * 3 + y) % 5:
                cv[x, y] = P.SULFUR_SAND[4]
    return cv


def vent_side():
    b = P.IONIAN_BASALT
    cv = _reduce(ionian_basalt(), [b[1], b[2], b[4]])
    for x in range(16):
        cv[x, 0] = P.SULFUR_SAND[4]
        cv[x, 1] = P.SULFUR_SAND[2] if x % 2 else P.SULFUR_SAND[4]
    return cv


# --------------------------------------------------------------------------- Ganymede and Callisto

def ganymede_regolith(v=0):
    return dust(P.GANYMEDE_SOIL, 10201 + v * 31)


def grooved_ice():
    """Bright young ice with fine parallel furrows (the grooves seen up close): horizontal grain."""
    r = P.GROOVED_ICE
    f = blend((blur_dir(white(10301), 3, 1, 0), 0.6), (clustered(16, 16, 10302), 0.4))
    cv, _ = quant(f, [r[1], r[2], r[3], r[4], r[5]], [8, 24, 36, 24, 8])
    for y in (4, 11):
        for x in range(16):
            if (x + y) % 5:
                cv[x, y] = r[1]
                cv[x, y + 1] = r[5] if x % 3 else r[4]
    return cv


def dark_ice_crust():
    """Ancient dark crust: dusty ice and rock mixed, brown-grey, clumpy like tuff."""
    r = P.DARK_ICE_CRUST
    f = blend((clustered(16, 16, 10401, passes=2), 0.55), (white(10402), 0.3), (fbm(16, 16, 10403, [(8, 1.0)]), 0.15))
    cv, _ = quant(f, [r[1], r[2], r[3], r[4], r[5]], [10, 26, 34, 22, 8])
    for x, y in [(4, 3), (12, 8), (7, 13)]:
        cv[x, y] = r[0]
    return cv


def callisto_regolith(v=0):
    return dust(P.CALLISTO_SOIL, 10501 + v * 37)


def bright_frost(v=0):
    return frost(P.BRIGHT_FROST, 10601 + v * 41)


def callistan_rock():
    """Dark ice-rich rock: grey, fine, with pale frost-filled pores."""
    r = P.CALLISTAN_ROCK
    f = blend((clustered(16, 16, 10701, passes=1), 0.5), (white(10702), 0.35), (fbm(16, 16, 10703, [(8, 1.0)]), 0.15))
    cv, _ = quant(f, [r[1], r[2], r[3], r[4], r[5]], [8, 26, 38, 22, 6])
    for x, y in [(3, 3), (10, 6), (6, 11), (13, 13)]:
        cv[x, y] = r[5]
        cv[x + 1, y + 1] = r[1]
    return cv


# --------------------------------------------------------------------------- creatures

def glow_serpent():
    """64x32 sheet for GlowSerpentModel: deep blue-black skin, a paler belly line, spot rows (lit in the glow layer)."""
    cv = Canvas(64, 32)
    r = P.SERPENT
    n = clustered(64, 64, 10801, passes=1)

    def skin(base):
        def fn(face, x, y, fw, fh):
            if face == 'bottom':
                return r[3]
            if face == 'top':
                return noise_pick(r, base + 1, n, x, y)
            return noise_pick(r, base if y < fh - 1 else base + 1, n, x + 13, y + 7)
        return fn

    paint(cv, 0, 0, 4, 4, 5, skin(2))
    for u, v, w, h in ((18, 0, 4, 4), (36, 0, 3, 3), (18, 10, 3, 3), (36, 10, 2, 2)):
        paint(cv, u, v, w, h, 5, skin(1))
    # barbels and fin: thin membranes
    for y in range(9, 14):
        for x in range(0, 6):
            cv[x, y] = r[3]
    for y in range(16, 28):
        for x in range(0, 12):
            if (x + y) % 3 == 0:
                cv[x, y] = r[4]
            else:
                cv[x, y] = r[2]
    return cv


SERPENT_SPOTS = [
    # head: two eyes
    (5, 6), (8, 6),
    # spot rows along the sides of each segment (left and right faces) and the top
    (19, 7), (21, 7), (27, 7), (29, 7), (24, 5), (25, 2),
    (37, 6), (39, 6), (44, 6), (46, 6), (41, 4),
    (19, 16), (21, 16), (27, 16), (29, 16), (24, 13),
    (37, 15), (39, 15), (43, 15), (45, 15),
]


def glow_serpent_glow():
    cv = Canvas(64, 32)
    g = P.SERPENT_GLOW
    for i, (x, y) in enumerate(SERPENT_SPOTS):
        cv[x, y] = g[2] if i < 2 else g[1]
    return cv


def sulfur_crab():
    """64x32 sheet for SulfurCrabModel: glazed yellow-orange shell with darker plates, pale eye stalks, dark claw tips."""
    cv = Canvas(64, 32)
    r = P.CRAB
    n = clustered(64, 64, 10901, passes=1)

    def body(face, x, y, fw, fh):
        if face == 'top':
            rim = x in (0, fw - 1) or y in (0, fh - 1)
            t = 3 if rim else (5 if n[y][x] > 0.6 else 4)
            if (x + y * 2) % 7 == 0 and not rim:
                t = 6
            return r[t]
        if face == 'bottom':
            return r[1]
        return noise_pick(r, 3 if y == 0 else 2, n, x + 20, y + 30)

    paint(cv, 0, 0, 12, 4, 10, body)
    paint(cv, 0, 14, 8, 1, 6, lambda f, x, y, fw, fh: r[5] if f == 'top' else r[3])
    paint(cv, 28, 14, 1, 2, 1, lambda f, x, y, fw, fh: hexc('#1a120b') if (f == 'top') else r[6])

    def claw(face, x, y, fw, fh):
        if face == 'front':
            return r[1] if y == fh // 2 else r[2]
        if face == 'top':
            return r[5] if y > 1 else r[4]
        return noise_pick(r, 3, n, x + 44, y)

    paint(cv, 44, 0, 4, 3, 5, claw)
    paint(cv, 32, 14, 6, 1, 1, lambda f, x, y, fw, fh: (r[4] if x < 4 else r[2]) if f == 'top' else r[2])
    return cv


# --------------------------------------------------------------------------- Jupiter's disc

def jupiter_disc(size=256):
    """Jupiter as a full disc in flat tones, like the vanilla Moon: cream zones and tan-brown belts with wavy edges,
    blue-grey festoons along the equator, dark belt bars, white ovals, the Great Red Spot in the south, grey-brown poles.
    Phase and limb shading are applied in game."""
    from PIL import Image
    rng = random.Random(11001)
    zone = hexc('#e2d8c3')
    zone2 = hexc('#d6c7a6')
    tan = hexc('#c2a57e')
    belt = hexc('#a07f5c')
    dark = hexc('#7d6046')
    festoon = hexc('#7d8790')
    polar = hexc('#988e7f')
    polar2 = hexc('#827869')
    grs = hexc('#c47a52')
    grs2 = hexc('#b0603f')
    oval = hexc('#efe8da')
    # bands from north (-1) to south (+1): (v_top, v_bottom, colour)
    bands = [(-1.0, -0.78, polar2), (-0.78, -0.62, polar), (-0.62, -0.52, tan), (-0.52, -0.42, zone2), (-0.42, -0.34, belt),
             (-0.34, -0.24, zone), (-0.24, -0.09, belt), (-0.09, 0.08, zone), (0.08, 0.26, belt), (0.26, 0.4, zone),
             (0.4, 0.5, tan), (0.5, 0.62, zone2), (0.62, 0.78, polar), (0.78, 1.0, polar2)]
    img = Image.new('RGBA', (size, size), (0, 0, 0, 0))
    px = img.load()
    c = (size - 1) / 2
    for y in range(size):
        for x in range(size):
            u = (x - c) / c
            v = (y - c) / c
            if math.hypot(u, v) > 1:
                continue
            # band edges wave with longitude
            vv = v + 0.012 * math.sin(u * 17 + v * 9) + 0.008 * math.sin(u * 41 - v * 5)
            col = zone
            for top, bot, colour in bands:
                if top <= vv < bot:
                    col = colour
                    break
            # dark bars inside the equatorial belts
            if (-0.2 < vv < -0.13 or 0.14 < vv < 0.2) and math.sin(u * 13 + (1 if vv > 0 else 2)) > 0.55:
                col = dark
            # blue-grey festoons hanging from the north equatorial belt into the equatorial zone
            if -0.09 <= vv < 0.0:
                for k in range(6):
                    fu = -0.85 + k * 0.33
                    if abs(u - fu - (vv + 0.09) * 0.8) < 0.035 * (1 - (vv + 0.09) / 0.09):
                        col = festoon
            # the Great Red Spot and its pale hollow
            gd = math.hypot((u - 0.28) / 0.19, (v - 0.33) / 0.085)
            if gd < 1.25:
                col = zone if gd > 1.0 else (grs2 if gd > 0.65 else grs)
            # white ovals in the south temperate belt
            for ou in (-0.45, -0.15, 0.62):
                if math.hypot((u - ou) / 0.05, (v - 0.45) / 0.03) < 1:
                    col = oval
            px[x, y] = (col[0], col[1], col[2], 255)
    return img


# --------------------------------------------------------------------------- export

def export(save):
    dust_spec = (18, 8, 64, None)
    frost_spec = (90, 14, 30, None)
    for name, fn, r, spec in (('europan_frost', europan_frost, P.EUROPA_FROST, frost_spec), ('sulfur_sand', sulfur_sand, P.SULFUR_SAND, dust_spec),
                              ('so2_frost', so2_frost, P.SO2_FROST, frost_spec), ('red_sulfur', red_sulfur, P.RED_SULFUR, dust_spec),
                              ('ganymede_regolith', ganymede_regolith, P.GANYMEDE_SOIL, dust_spec),
                              ('callisto_regolith', callisto_regolith, P.CALLISTO_SOIL, dust_spec),
                              ('bright_frost', bright_frost, P.BRIGHT_FROST, frost_spec)):
        base = fn(0)
        save(base, 'block', name, spec)
        save(fn(1), 'block', name + '_1', spec)
        for side in ('left', 'right'):
            save(footprint(base, prints(r), side), 'block', f'{name}_footprint_{side}', (22, 8, 64, None))
    ice = (200, 20, 0, None)
    rock = (40, 12, 20, None)
    save(europan_ice(), 'block', 'europan_ice', ice)
    save(europan_ice_bricks(), 'block', 'europan_ice_bricks', ice)
    save(stained_ice(), 'block', 'stained_ice', ice)
    save(refrozen_brine(), 'block', 'refrozen_brine', (150, 18, 10, None))
    save(salt_overlay(), 'block', 'salt_ore_overlay', (180, 30, 0, None), wrap=False)
    save(geyser_top(), 'block', 'ice_geyser_top', ice)
    save(geyser_side(), 'block', 'ice_geyser_side', ice)
    save(ionian_basalt(), 'block', 'ionian_basalt', rock)
    save(sulfur_overlay(), 'block', 'ionian_sulfur_ore_overlay', (120, 30, 0, None), wrap=False)
    glow = lambda c: (60, 30, 0, 200) if c[3] and c[0] > 190 and c[2] < 140 else None
    save(vent_top(), 'block', 'sulfur_vent_top', (40, 12, 20, glow))
    save(vent_side(), 'block', 'sulfur_vent_side', rock)
    save(grooved_ice(), 'block', 'grooved_ice', ice)
    save(dark_ice_crust(), 'block', 'dark_ice_crust', rock)
    save(callistan_rock(), 'block', 'callistan_rock', rock)
    save(ocean_salt(), 'item', 'ocean_salt')
    save(glow_serpent(), 'entity', 'jupiter/glow_serpent')
    save(glow_serpent_glow(), 'entity', 'jupiter/glow_serpent_glow')
    save(sulfur_crab(), 'entity', 'jupiter/sulfur_crab')
    import os
    out = os.path.join(os.path.dirname(__file__), '..', '..', '..', 'src', 'main', 'resources', 'assets', 'spacebreaker', 'textures', 'environment')
    jupiter_disc().save(os.path.join(out, 'jupiter_disc.png'))
