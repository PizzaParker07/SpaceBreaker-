"""Venus textures: scorched basalt plains, folded tessera rock, sulfur crust, metallic highland frost, and the light
composites used to build in the clouds. Same rules as every block: few tones, calm noise, light from the top-left."""
import random

from sbtex.core import Canvas, ramp, clustered, fbm, parse_mask, mask_from, dome_shade, edge_pixels, pick
from modules.moon import quant, white, blend, blur_dir, bricks

# Venus basalt: dark grey-brown, baked; a faint warm cast from the orange daylight is NOT painted in (light does that).
BASALT = ramp('#24201e', '#2f2a27', '#3a3431', '#443e3a', '#4a4440', '#57504b', '#635b55')
# Weathered surface gravel: browner, finer.
SOIL = ramp('#2a2420', '#3a312b', '#41372f', '#483d35', '#4f433a', '#574a40')
# Tessera: strongly folded rock, banded grey and rust.
TESSERA = ramp('#2c2624', '#3b3330', '#4b413c', '#5a4e47', '#6a5c53', '#7a6b60')
# Sulfur crust: muted yellow, never neon.
SULFUR = ramp('#5e5222', '#7d6d2c', '#958437', '#a99743', '#bba951', '#cbba63')
# Metal frost (lead and bismuth sulfides): bright, slightly cool grey with sparkle.
FROST = ramp('#5f5b52', '#7a7568', '#938e80', '#a8a293', '#bdb7a8', '#cdc8ba', '#e0dbcf')
# Light composite panel: warm white with a fine carbon weave.
COMPOSITE = ramp('#8e8f8c', '#a9aaa6', '#bdbeb9', '#cdceca', '#dcddd9', '#e9eae6')
WEAVE = ramp('#3a3c40', '#55585d')
# Aerogel: pale blue haze (translucent).
AEROGEL = ramp('#9fb6c8', '#b7cad8', '#cad9e4', '#dae6ee')
# Envelope fabric: off-white coated cloth.
FABRIC = ramp('#a9a59a', '#bdb9ae', '#cfcbc1', '#ddd9cf', '#e9e6dd')
# Sulfur item.
SULFUR_ITEM = ramp('#4a3f14', '#6f5f1e', '#958129', '#b9a136', '#d4bd4a', '#e7d26c', '#f3e59a')


def venus_basalt():
    """Lava plains rock: flat slabs with fine shrinkage cracks, slightly vesicular."""
    r = BASALT
    f = blend((clustered(16, 16, 5001, passes=1), 0.5), (white(5002), 0.35), (fbm(16, 16, 5003, [(8, 1.0)]), 0.15))
    cv, _ = quant(f, [r[1], r[2], r[3], r[4], r[5]], [8, 26, 38, 22, 6])
    for line in ([(0, 5), (1, 5), (2, 6), (3, 6), (4, 6), (5, 7)], [(9, 11), (10, 11), (11, 12), (12, 12), (13, 12), (14, 13), (15, 13)],
                 [(8, 1), (8, 2), (9, 3)]):
        for x, y in line:
            cv[x, y] = r[0]
            cv[x, y + 1] = r[4]
    return cv


def venus_soil(v=0):
    """Weathered basalt gravel: small angular grains."""
    f = blend((white(5101 + v), 0.55), (clustered(16, 16, 5102 + v), 0.45))
    cv, _ = quant(f, SOIL, [5, 20, 34, 26, 11, 4])
    rng = random.Random(5103 + v)
    for _ in range(4):
        x, y = rng.randrange(16), rng.randrange(16)
        cv[x, y] = SOIL[5]
        cv[x + 1, y + 1] = SOIL[0]
    return cv


# one irregular fold profile across the tile (seamless: starts and ends at 0)
FOLD = [0, 0, 1, 1, 2, 2, 2, 1, 0, -1, -1, -2, -2, -1, -1, 0]


def tessera():
    """Folded, sheared rock: wavy bands that bend across the block."""
    r = TESSERA
    g = blend((white(5201), 0.5), (clustered(16, 16, 5202), 0.5))
    beds = [4, 3, 2, 3, 4, 5, 4, 2, 1, 2, 3, 4, 3, 3, 2, 3]
    cv = Canvas()
    for y in range(16):
        for x in range(16):
            # chevron folds: bands shift up and down along x
            shift = FOLD[x]
            t = beds[(y + shift) % 16] + (1 if g[y][x] > 0.8 else -1 if g[y][x] < 0.18 else 0)
            cv[x, y] = r[max(0, min(5, t))]
    return cv


def sulfur_crust():
    """Sulfur precipitate crusted over dark rock: irregular ochre scabs in a dark matrix, each lit on its upper edge
    and shadowed under its lower one, so it reads as a lumpy crust rather than a soft, even sponge."""
    r = SULFUR
    f = blend((clustered(16, 16, 5301, passes=3), 0.7), (white(5302), 0.3))
    cv, idx = quant(f, [BASALT[1], BASALT[3], r[0], r[1], r[2], r[4]], [24, 18, 18, 20, 14, 6])
    out = cv.copy()
    for x in range(16):
        for y in range(16):
            above = idx[(y - 1) % 16][x]
            below = idx[(y + 1) % 16][x]
            if idx[y][x] >= 3 and above <= 1:
                out[x, y] = r[4]  # lit top edge of a scab
            elif idx[y][x] >= 2 and below <= 1:
                out[x, y] = BASALT[1]  # its shadowed underside
    return out


def metal_frost():
    """Radar-bright highland frost of lead and bismuth sulfides: grey crystals with bright facets."""
    r = FROST
    f = blend((white(5401), 0.55), (clustered(16, 16, 5402), 0.45))
    f = blend((f, 0.6), (clustered(16, 16, 5403, passes=2), 0.4))
    cv, _ = quant(f, [r[2], r[3], r[4], r[5]], [14, 38, 34, 14])
    for x, y in [(2, 3), (10, 2), (6, 9), (13, 12), (3, 13)]:
        cv[x, y] = r[6]
        cv[x + 1, y + 1] = r[2]
    return cv


def composite_panel():
    """Carbon-fibre reinforced light panel: pale face with a fine twill weave and a bevelled edge."""
    r = COMPOSITE
    f = blend((clustered(16, 16, 5501, passes=2), 0.6), (white(5502), 0.4))
    cv, _ = quant(f, [r[2], r[3], r[4]], [25, 50, 25])
    for y in range(2, 14):
        for x in range(2, 14):
            if (x + y * 2) % 6 == 0:
                cv[x, y] = r[1]
    for i in range(16):
        cv[i, 0] = r[5]
        cv[0, i] = r[5]
        cv[i, 15] = r[0]
        cv[15, i] = r[0]
    for i in range(1, 15):
        cv[i, 14] = r[1]
        cv[14, i] = r[1]
    return cv


def aerogel():
    """Translucent aerogel: soft blue haze with brighter clumps (alpha set in the PNG)."""
    r = AEROGEL
    f = blend((clustered(16, 16, 5601, passes=2), 0.7), (white(5602), 0.3))
    cv, _ = quant(f, r, [20, 35, 30, 15])
    for y in range(16):
        for x in range(16):
            c = cv[x, y]
            edge = x in (0, 15) or y in (0, 15)
            cv[x, y] = (c[0], c[1], c[2], 170)
    return cv


def envelope_fabric():
    """Coated envelope cloth: woven like wool, with sewn seams every 8 px."""
    r = FABRIC
    f = blend((white(5701), 0.5), (clustered(16, 16, 5702), 0.5))
    cv, _ = quant(f, [r[1], r[2], r[3], r[4]], [15, 35, 35, 15])
    for i in range(16):
        cv[i, 7] = r[0]
        cv[i, 8] = r[4]
        cv[7, i] = r[0] if i % 2 == 0 else r[1]
    return cv


def sulfur_item():
    """A lumpy yellow sulfur crystal cluster."""
    rows = [
        "................",
        "................",
        "........X.......",
        ".......XXX......",
        "....X..XXX.X....",
        "...XXX.XXXXXX...",
        "...XXXXXXXXXX...",
        "..XXXXXXXXXXXX..",
        "..XXXXXXXXXXXX..",
        ".XXXXXXXXXXXXX..",
        ".XXXXXXXXXXXXXX.",
        "..XXXXXXXXXXXX..",
        "...XXXXXXXXXX...",
        ".....XXXXXX.....",
        "................",
        "................",
    ]
    m = mask_from(parse_mask(rows))
    sh = dome_shade(m, roundness=2.5)
    r = SULFUR_ITEM
    rng = random.Random(5801)
    cv = Canvas()
    for y in range(16):
        for x in range(16):
            if m[y][x]:
                cv[x, y] = pick(r[2:6], 0.1 + 0.85 * sh[y][x] + (rng.random() - 0.5) * 0.2)
    for x, y in [(8, 3), (4, 5), (11, 5), (6, 8)]:
        cv[x, y] = r[6]
    for x, y, side in edge_pixels(m):
        cv[x, y] = r[0] if side == 'br' else r[1]
    return cv


def acid_coating_item():
    """A small sealed tin of fluoropolymer: white body, yellow hazard band, grey lid."""
    rows = [
        "................",
        "................",
        "................",
        ".....LLLLLL.....",
        "....LllllllL....",
        "....WWWWWWWW....",
        "....WwwwwwwW....",
        "....YyYyYyYY....",
        "....yYyYyYyY....",
        "....WwwwwwwW....",
        "....WwwwwwwW....",
        "....WwwwwwwW....",
        "....DDDDDDDD....",
        "................",
        "................",
        "................",
    ]
    tone = {'L': COMPOSITE[1], 'l': COMPOSITE[3], 'W': COMPOSITE[2], 'w': COMPOSITE[4], 'Y': SULFUR[3], 'y': WEAVE[0], 'D': COMPOSITE[0]}
    cv = Canvas()
    for y, row in enumerate(rows):
        for x, ch in enumerate(row):
            if ch in tone:
                cv[x, y] = tone[ch]
    for y in range(5, 12):
        cv[4, y] = COMPOSITE[1]
        cv[11, y] = COMPOSITE[0]
    return cv


def aerostat_kit():
    """A packed aerostat: a strapped bundle of cream envelope fabric on a grey composite base, with a yellow tag."""
    rows = [
        "................",
        "................",
        "....FFFFFFF.....",
        "...FfffSfffF....",
        "..FffffSffffF...",
        "..FfffYSYfffF...",
        "..SSSSSSSSSSS...",
        "..FfffYSYfffF...",
        "..FffffSffffF...",
        "...FfffSfffF....",
        "..PPPPPPPPPPP...",
        "..PpppppppppP...",
        "..DDDDDDDDDDD...",
        "................",
        "................",
        "................",
    ]
    tone = {'F': COMPOSITE[1], 'f': COMPOSITE[4], 'S': WEAVE[0], 'Y': SULFUR[4], 'P': COMPOSITE[2], 'p': COMPOSITE[3], 'D': COMPOSITE[0]}
    cv = Canvas()
    for y, row in enumerate(rows):
        for x, ch in enumerate(row):
            if ch in tone:
                cv[x, y] = tone[ch]
    return cv


def descent_probe():
    """A squat heat-hardened lander: a grey tungsten sphere on a ring, a dark heat shield, a cream parachute pack on top."""
    rows = [
        "................",
        "......FFFF......",
        ".....FffffF.....",
        "......SSSS......",
        ".....TTTTTT.....",
        "....TtttttTT....",
        "...TttWWtttTT...",
        "...TttWWtttTT...",
        "...TtttttttTT...",
        "....TtttttTT....",
        "..RRRRRRRRRRRR..",
        "...DDDDDDDDDD...",
        ".....DDDDDD.....",
        "................",
        "................",
        "................",
    ]
    tone = {'F': COMPOSITE[2], 'f': COMPOSITE[4], 'S': WEAVE[0], 'T': WEAVE[0], 't': COMPOSITE[1], 'W': SULFUR[4], 'R': COMPOSITE[3], 'D': BASALT[1]}
    cv = Canvas()
    for y, row in enumerate(rows):
        for x, ch in enumerate(row):
            if ch in tone:
                cv[x, y] = tone[ch]
    return cv


def lift_gas_sac():
    """A Cloud Jelly's gas bladder: a pale, translucent-looking lilac balloon with a tied neck and a soft highlight."""
    m = mask_from(parse_mask([
        "................",
        "......####......",
        "....########....",
        "...##########...",
        "..############..",
        "..############..",
        "..############..",
        "...##########...",
        "....########....",
        ".....######.....",
        "......####......",
        ".......##.......",
        "......####......",
        ".......##.......",
        "................",
        "................",
    ]))
    sh = dome_shade(m, roundness=2.4)
    r = ramp('#5e4a68', '#7d6488', '#9d84a8', '#bba3c4', '#d6c4dc', '#efe6f2')
    cv = Canvas()
    for y in range(16):
        for x in range(16):
            if m[y][x]:
                cv[x, y] = pick(r[1:5], 0.1 + 0.85 * sh[y][x]) if y < 11 else (r[0] if y == 12 else r[1])
    for x, y in [(5, 3), (6, 3), (5, 4)]:
        cv[x, y] = r[5]
    for x, y, side in edge_pixels(m):
        if y < 11:
            cv[x, y] = r[0] if side == 'br' else r[1]
    return cv


def export(save):
    rock = (40, 12, 20, None)
    save(venus_basalt(), 'block', 'venus_basalt', rock)
    save(venus_soil(0), 'block', 'venus_soil', (18, 10, 40, None))
    save(tessera(), 'block', 'tessera_rock', rock)
    save(sulfur_crust(), 'block', 'sulfur_crust', (30, 12, 20, None))
    shiny = lambda c: (210, 230, 0, 255) if c[3] and sum(c[:3]) > 450 else None
    save(metal_frost(), 'block', 'metal_frost', (120, 200, 0, shiny))
    save(composite_panel(), 'block', 'composite_panel', (110, 20, 0, None))
    save(aerogel(), 'block', 'aerogel', (220, 20, 0, None))
    save(envelope_fabric(), 'block', 'envelope_fabric', (10, 10, 0, None))
    save(sulfur_item(), 'item', 'sulfur')
    save(acid_coating_item(), 'item', 'acid_coating')
    save(lift_gas_sac(), 'item', 'lift_gas_sac')
    save(aerostat_kit(), 'item', 'aerostat_kit')
    save(descent_probe(), 'item', 'descent_probe')
    export_more(save)


# --------------------------------------------------------------------------- creatures and sky

from modules.moon_creatures import paint, noise_pick  # noqa: E402

ACID = ramp('#3e3a1c', '#5a5426', '#777031', '#948b3e', '#b0a64e', '#c9c068')
GLOW = ramp('#c8d038', '#e4ec5a', '#f6fa9a')
BELLY = ramp('#9e987a', '#b8b294', '#d6d1b4')
SAC = ramp('#b8a98e', '#cfc3a8', '#ddd2b8', '#e9e0ca', '#f3ecdb')
VEIN = ramp('#9c7f98', '#7f6580', '#634e66')


def acid_ray():
    """Acid Ray (128x64): an olive back mottled with dark rosettes, a pale spine ridge, a darker eye hump with two
    acid-yellow eyes, wing panels darkening outward to glowing acid-yellow margins, a pale belly."""
    cv = Canvas(128, 64)
    n = clustered(64, 64, 6001, passes=1)
    m = clustered(64, 64, 6002, passes=2)

    def back(t, x, y, ox, oy):
        if m[(y + oy) % 64][(x + ox) % 64] > 0.74:
            return ACID[0]  # a dark rosette
        return noise_pick(ACID, t, n, x + ox, y + oy)

    def body(face, x, y, fw, fh):
        if face == 'top':
            return back(2 if 1 < x < fw - 2 else 1, x, y, 0, 0)
        if face == 'bottom':
            return noise_pick(BELLY, 1, n, x + 20, y)
        return ACID[2] if y == 0 else BELLY[0]

    paint(cv, 0, 0, 6, 2, 12, body)
    paint(cv, 40, 0, 4, 1, 2, body)
    paint(cv, 40, 4, 4, 1, 2, body)

    def hump(face, x, y, fw, fh):
        if face == 'front' or (face == 'top' and y == 0):
            return GLOW[2] if x in (0, fw - 1) else ACID[0]  # the eyes at the hump's front corners
        return back(2, x, y, 9, 33)

    paint(cv, 56, 0, 5, 1, 3, hump)
    paint(cv, 56, 6, 2, 1, 8, lambda f, x, y, fw, fh: ACID[4] if f == 'top' and x == 0 else (ACID[3] if f == 'top' else ACID[2]))

    def panel(level):
        def fn(face, x, y, fw, fh):
            if face == 'top':
                outer = x >= fw - 1 or y in (0, fh - 1)
                if outer and level > 0 or (level == 0 and y in (0, fh - 1)):
                    return GLOW[1] if (x + y) % 2 == 0 else GLOW[0]
                return back(2 if level == 0 else 1, x, y, 30 + level * 9, 10)
            if face == 'bottom':
                return noise_pick(BELLY, 2 if level == 0 else 1, n, x + 40, y + 30)
            return GLOW[0]
        return fn

    paint(cv, 0, 16, 4, 1, 12, panel(0))
    paint(cv, 0, 30, 4, 1, 8, panel(1))
    paint(cv, 0, 40, 3, 1, 4, panel(2))
    paint(cv, 40, 8, 1, 1, 2, lambda f, x, y, fw, fh: GLOW[0] if f == 'front' else (ACID[3] if f == 'top' else ACID[1]))
    paint(cv, 80, 0, 1, 1, 7, lambda f, x, y, fw, fh: ACID[2] if f == 'top' else ACID[1])
    paint(cv, 80, 8, 1, 1, 6, lambda f, x, y, fw, fh: ACID[3] if f == 'top' and x % 2 == 0 else ACID[1])
    paint(cv, 80, 16, 2, 1, 2, lambda f, x, y, fw, fh: GLOW[1] if f == 'top' else GLOW[0])
    return cv


CORE = ramp('#c8742c', '#e89a40', '#f8c868')


def cloud_jelly():
    """Cloud Jelly (128x64): a pale translucent bell with lilac radial veins in tiers, a darker frilled skirt with a
    scalloped edge, a warm glowing core seen through it, lilac tentacles banded paler toward their tips."""
    cv = Canvas(128, 64)
    n = clustered(128, 64, 6101, passes=2)

    def bell(alpha):
        def f(face, x, y, fw, fh):
            v = n[(y * 3) % 64][(x * 3) % 128]
            if face == 'top':
                c = VEIN[0] if (x == fw // 2 or y == fh // 2 or x == y or x == fw - 1 - y) else (SAC[3] if v > 0.5 else SAC[2])
                return (c[0], c[1], c[2], alpha + 20)
            if face == 'bottom':
                return (SAC[1][0], SAC[1][1], SAC[1][2], alpha - 30)
            if x % 3 == 1:
                return (VEIN[0][0], VEIN[0][1], VEIN[0][2], alpha + 30)
            c = SAC[4] if y == 0 else (SAC[2] if v > 0.4 else SAC[1])
            return (c[0], c[1], c[2], alpha)
        return f

    paint(cv, 0, 0, 16, 4, 16, bell(150))
    paint(cv, 64, 0, 13, 4, 13, bell(160))
    paint(cv, 0, 20, 9, 3, 9, bell(170))
    paint(cv, 36, 20, 5, 2, 5, bell(190))

    def skirt(face, x, y, fw, fh):
        if face in ('top', 'bottom'):
            edge = x in (0, fw - 1) or y in (0, fh - 1)
            if edge and (x + y) % 2 == 0:
                return None  # scalloped edge
            c = VEIN[1] if edge else (VEIN[0] if (x + y) % 4 == 0 else SAC[1])
            return (c[0], c[1], c[2], 210)
        return (VEIN[1][0], VEIN[1][1], VEIN[1][2], 220) if x % 2 == 0 else None

    paint(cv, 0, 36, 18, 1, 18, skirt)
    paint(cv, 56, 20, 5, 5, 5, lambda f, x, y, fw, fh: CORE[2] if 1 <= x <= 3 and 1 <= y <= 3 else CORE[1])

    def tentacle(face, x, y, fw, fh):
        c = VEIN[0] if y % 3 else VEIN[2]
        return (c[0], c[1], c[2], 230)

    paint(cv, 76, 20, 1, 10, 1, tentacle)
    paint(cv, 80, 20, 1, 10, 1, lambda f, x, y, fw, fh: (SAC[2][0], SAC[2][1], SAC[2][2], 200) if y > fh - 3 else (VEIN[0][0], VEIN[0][1], VEIN[0][2], 220))
    paint(cv, 84, 20, 2, 8, 2, lambda f, x, y, fw, fh: (VEIN[1][0], VEIN[1][1], VEIN[1][2], 230) if (x + y) % 2 else (SAC[2][0], SAC[2][1], SAC[2][2], 220))
    return cv


def cloud_sea():
    """Tiling cloud-top texture (64x64): rounded puffs in cream and pale yellow, sunlit on top and shadowed in the
    hollows, drawn out along the east-west super-rotation winds; the gaps between puffs are clear so a second, higher
    layer drawn with it reads as heaped cloud tops, and each puff carries a darker rim where it meets a gap."""
    f = fbm(64, 64, 6201, [(32, 1.0), (16, 0.7), (8, 0.45), (4, 0.25), (2, 0.1)])
    g = fbm(64, 64, 6202, [(64, 1.0), (16, 0.5)])
    cv = Canvas(64, 64)
    r = ramp('#a8966a', '#bfae84', '#d2c49c', '#e3d8b8', '#f1eada', '#fcf9ee')

    def v_at(x, y):
        return 0.6 * f[(y * 2) % 64][x % 64] + 0.4 * g[y % 64][(x * 2) % 64]

    for y in range(64):
        for x in range(64):
            v = v_at(x, y)
            # light from the north-east: brighter where the puff rises toward it, darker on the far side
            slope = v_at(x + 1, y - 1) - v
            t = 0.55 + (v - 0.5) * 1.1 - slope * 4.0
            if v < 0.47:
                t -= 0.25  # the shadowed rim around each puff
            c = pick(r, max(0.0, min(0.999, t)))
            a = 255 if v > 0.42 else int(255 * max(0.0, (v - 0.32) / 0.10))
            cv[x, y] = (c[0], c[1], c[2], a)
    return cv


def export_more(save):
    save(acid_ray(), 'entity', 'venus/acid_ray')
    save(cloud_jelly(), 'entity', 'venus/cloud_jelly')
    save(cloud_sea(), 'environment', 'venus_cloud_sea')
