"""Mars block and item textures: dusty soils (with boot prints), dune sands, basalt, layered sediments, ices, ores.

Realistic subject, vanilla execution: 4-6 tones per block, calm clumpy noise like vanilla sand and stone,
light from the top-left. Mars colours are muted butterscotch/rust, never saturated orange.
"""
import random

from sbtex.core import Canvas, ramp, clustered, fbm, parse_mask, mask_from, dome_shade, edge_pixels, pick
from modules.moon import quant, white, blend, blur_dir, voronoi, footprint, bricks, overlay, RAW_CHUNK


# --------------------------------------------------------------------------- palettes (dark -> light)

# Martian dust/regolith: muted rust-butterscotch (bead, then five powder tones).
REGOLITH = ramp('#5e3220', '#86492f', '#8f5134', '#985939', '#a1623f', '#ab6c46')
# Hematite spherules ("blueberries"): blue-grey iron oxide beads.
BERRY = ramp('#2f2c33', '#4a4752', '#6c6a76')
# Perchlorate/sulfate salt crust: pale cream.
SALT = ramp('#b49a7c', '#cbb698', '#ddcdb2')
# Phyllosilicate clay: pale grey-tan with a faint green cast.
CLAY = ramp('#6f6355', '#8a7d6c', '#938674', '#9c8f7c', '#a59884', '#afa28d')
# CO2 frost over dust: pinkish white.
FROST = ramp('#8f6c5c', '#b9a49c', '#c9b9b3', '#d6cbc7', '#e2dad7', '#eee9e7')
# Dark basaltic dune sand.
SAND = ramp('#2f2724', '#433833', '#4a3e38', '#51443d', '#584a42', '#605048')
# Olivine sand: olive green.
OLIVINE = ramp('#35352a', '#4b4c37', '#53543c', '#5b5c41', '#636447', '#6c6c4d')
# Martian basalt: dark warm grey (a touch of rust in the lightest tone).
BASALT = ramp('#1f1d1d', '#292626', '#332f2e', '#3d3836', '#48423f', '#554d49', '#635952')
# Layered sandstone: rust-tan laminae.
SANDSTONE = ramp('#5c3424', '#6d3f2b', '#7e4b33', '#8e573b', '#9d6444', '#ab724e', '#b8815a')
# Mudstone: grey-purple lake sediment.
MUDSTONE = ramp('#3a2e2e', '#463838', '#524242', '#5e4d4b', '#6a5855', '#78645f')
# Gypsum: white satin-spar fibres.
GYPSUM = ramp('#9d968a', '#b9b2a6', '#cdc7bc', '#ddd8cf', '#ebe7e0', '#f6f4ef')
# Water ice: dust-stained blue-white.
ICE = ramp('#7c8590', '#97a1ac', '#aab3bd', '#bbc4cd', '#cbd3db', '#dbe2e8')
# Dusty ice: ice full of reddish dust layers.
DUSTY_ICE = ramp('#6e5a52', '#82706a', '#958581', '#a69995', '#b6aba8', '#c6bdbb')
# Dry ice: clean CO2 ice, white with a cold blue shade.
DRY_ICE = ramp('#a9b5c2', '#bec9d4', '#cfd8e1', '#dee5ec', '#ebf0f4', '#f7f9fb')
# Hematite: steel grey with a red-brown streak.
HEMATITE = ramp('#1e1a1c', '#2f282b', '#45393b', '#5c4b4c', '#7a6667', '#9d8a8a', '#c2b3b2', '#e2d8d6')


# --------------------------------------------------------------------------- soils

SOIL_SHARES = [5, 20, 36, 26, 10, 3]


def dust(r, seed):
    """Fine dust, like vanilla red sand but calmer: soft clumps, a few darker grains."""
    f = blend((white(seed), 0.42), (clustered(16, 16, seed + 1, passes=1), 0.4), (fbm(16, 16, seed + 2, [(8, 1.0)]), 0.18))
    cv, _ = quant(f, r, SOIL_SHARES)
    rng = random.Random(seed + 9)
    for _ in range(3):
        x, y = rng.randrange(16), rng.randrange(16)
        cv[x, y] = r[0]
        cv[x + 1, y + 1] = r[4]
    return cv


def martian_regolith(v=0):
    return dust(REGOLITH, 1501 + v * 37)


def hematite_soil(v=0):
    """Regolith studded with BB-sized hematite concretions: dark grey bead, a lit pixel top-left, shadow below."""
    cv = dust([REGOLITH[1], REGOLITH[1], REGOLITH[2], REGOLITH[3], REGOLITH[4], REGOLITH[5]], 1601 + v * 41)
    rng = random.Random(1602 + v)
    spots = [(2, 2), (9, 1), (13, 5), (5, 7), (11, 10), (1, 12), (7, 13), (14, 14)]
    rng.shuffle(spots)
    for i, (x, y) in enumerate(spots[:6]):
        x = (x + rng.randrange(3)) % 16
        y = (y + rng.randrange(3)) % 16
        if i % 3 == 0:  # a bigger spherule: 2 px with a highlight
            cv[x, y] = BERRY[2]
            cv[x + 1, y] = BERRY[1]
            cv[x + 1, y + 1] = REGOLITH[0]
        else:
            cv[x, y] = BERRY[1]
            cv[x + 1, y + 1] = REGOLITH[0]
    return cv


def perchlorate_soil(v=0):
    """Rust soil with a patchy pale salt crust (the crust follows a soft noise field)."""
    base = dust(REGOLITH, 1701 + v * 43)
    crust = fbm(16, 16, 1702 + v, [(4, 1.0), (2, 0.6)])
    g = white(1703 + v)
    for y in range(16):
        for x in range(16):
            c = 0.6 * crust[y][x] + 0.4 * g[y][x]
            if c > 0.74:
                base[x, y] = SALT[2] if g[y][x] > 0.75 else SALT[1]
            elif c > 0.68:
                base[x, y] = SALT[0]
    return base


def martian_clay(v=0):
    """Smooth pale clay with a few fine desiccation cracks (dark line, lit lower lip)."""
    f = blend((clustered(16, 16, 1801 + v, passes=2), 0.6), (white(1802 + v), 0.4))
    cv, _ = quant(f, CLAY, [4, 16, 34, 30, 12, 4])
    cracks = [[(3, 4), (4, 4), (5, 5), (6, 5), (7, 5)], [(11, 9), (11, 10), (12, 11), (12, 12)], [(1, 13), (2, 13), (3, 12)]]
    if v:
        cracks = [[(x + 5, y + 3) for x, y in c] for c in cracks]
    for line in cracks:
        for x, y in line:
            cv[x, y] = CLAY[0]
            cv[x, y + 1] = CLAY[5]
    return cv


def co2_frost(v=0):
    """Seasonal CO2 frost on dust: white crystals, pink dust showing through in places."""
    f = blend((white(1901 + v), 0.5), (clustered(16, 16, 1902 + v, passes=1), 0.5))
    cv, _ = quant(f, FROST, [6, 12, 20, 30, 22, 10])
    rng = random.Random(1903 + v)
    for _ in range(6):
        cv[rng.randrange(16), rng.randrange(16)] = FROST[5]
    return cv


def prints(r):
    """Footprint tones (pit shadow, tread dark, tread light, lit wall, rim lit, rim shade) from a 6-tone ramp."""
    return r[0], r[1], r[2], r[4], r[5], r[2]


# --------------------------------------------------------------------------- sands

def sand(r, seed):
    """Wind-sorted sand like vanilla sand: fine even grain with a soft ripple direction."""
    f = blend((white(seed), 0.5), (blur_dir(white(seed + 1), 1, 1, 0), 0.3), (clustered(16, 16, seed + 2), 0.2))
    cv, _ = quant(f, r, [3, 18, 34, 28, 13, 4])
    return cv


def martian_sand():
    return sand(SAND, 2001)


def olivine_sand():
    return sand(OLIVINE, 2101)


# --------------------------------------------------------------------------- rocks

def martian_basalt():
    """Fine-grained dark lava with frozen gas bubbles (dark pit, lit lower-right inner wall)."""
    r = BASALT
    f = blend((blur_dir(white(2201), 1, 1, 1), 0.45), (clustered(16, 16, 2202, passes=1), 0.4), (white(2203), 0.15))
    cv, _ = quant(f, [r[1], r[2], r[3], r[4], r[5]], [8, 26, 38, 22, 6])
    for x, y in [(3, 2), (11, 4), (6, 9), (14, 12), (1, 14), (9, 13)]:
        cv[x, y] = r[0]
        cv[x + 1, y + 1] = r[5]
    return cv


def polished_martian_basalt():
    r = BASALT
    f = blend((clustered(16, 16, 2301, passes=2), 0.6), (white(2302), 0.4))
    cv, _ = quant(f, [r[2], r[3], r[4], r[5]], [14, 40, 36, 10])
    for i in range(16):
        cv[i, 0] = r[5]
        cv[0, i] = r[5]
        cv[i, 15] = r[1]
        cv[15, i] = r[1]
    for i in range(1, 15):
        cv[i, 1] = r[4] if cv[i, 1] != r[5] else r[5]
        cv[1, i] = r[4] if cv[1, i] != r[5] else r[5]
        cv[i, 14] = r[2]
        cv[14, i] = r[2]
    cv[0, 15] = r[3]
    cv[15, 0] = r[3]
    return cv


def martian_basalt_bricks():
    r = BASALT
    return bricks([r[0], r[1], r[2], r[3], r[4], r[5]], 2401, rows=((0, 0), (4, 4), (8, 0), (12, 4)),
                  face=[r[2], r[3], r[4]], mortar=r[0])


def laminae(r, seed, beds, grain=0.3, wave=1):
    """Horizontal beds: each row gets a bed tone; bed boundaries wander by a pixel along x (seamless), and a clumpy
    grain noise breaks every bed up like vanilla sandstone so it never reads as planks."""
    g = blend((white(seed + 1), 0.55), (clustered(16, 16, seed + 2), 0.45))
    rng = random.Random(seed)
    # seamless 1-px wave of the bedding: a few smooth steps around the tile
    shift = [0] * 16
    if wave:
        phase = rng.randrange(16)
        for x in range(16):
            t = (x + phase) % 16
            shift[x] = 1 if 4 <= t < 9 else (-1 if 12 <= t < 15 and wave > 1 else 0)
    cv = Canvas()
    for y in range(16):
        for x in range(16):
            base = beds[(y + shift[x]) % 16]
            v = g[y][x]
            t = base + (1 if v > 1 - grain / 2 else -1 if v < grain / 2 else 0)
            cv[x, y] = r[max(0, min(len(r) - 1, t))]
    return cv


def martian_sandstone_side():
    """Rust sandstone in thin beds; each bed lit on its top edge, darker parting below (like vanilla sandstone)."""
    r = SANDSTONE
    beds = [4, 4, 3, 3, 3, 2, 4, 4, 3, 3, 2, 2, 3, 4, 3, 2]
    return laminae(r, 2501, beds, grain=0.55, wave=2)


def martian_sandstone_top():
    r = SANDSTONE
    f = blend((white(2601), 0.55), (clustered(16, 16, 2602), 0.45))
    cv, _ = quant(f, [r[2], r[3], r[4], r[5]], [10, 36, 40, 14])
    return cv


def martian_sandstone_bottom():
    r = SANDSTONE
    f = blend((white(2701), 0.55), (clustered(16, 16, 2702), 0.45))
    cv, _ = quant(f, [r[1], r[2], r[3], r[4]], [12, 38, 38, 12])
    return cv


def martian_mudstone():
    """Finely laminated lake mudstone with thin pale calcium-sulfate veins."""
    r = MUDSTONE
    beds = [3, 3, 2, 3, 3, 2, 2, 3, 2, 3, 4, 3, 3, 3, 2, 2]
    cv = laminae(r, 2801, beds, grain=0.5, wave=1)
    for x, y in [(2, 5), (3, 5), (4, 6), (5, 6), (11, 11), (12, 11), (13, 12)]:
        cv[x, y] = r[5]
    return cv


def gypsum_block():
    """Satin-spar gypsum: vertical fibres, soft sheen bands."""
    r = GYPSUM
    f = blend((blur_dir(white(2901), 3, 0, 1), 0.7), (white(2902), 0.3))
    cv, _ = quant(f, [r[1], r[2], r[3], r[4], r[5]], [6, 24, 36, 24, 10])
    return cv


# --------------------------------------------------------------------------- ices

def martian_ice():
    """Clear water ice stained by dust: packed-ice style diagonal grain, a few dust specks."""
    r = ICE
    f = blend((blur_dir(white(3001), 3, 1, 1), 0.65), (clustered(16, 16, 3002), 0.35))
    cv, _ = quant(f, [r[1], r[2], r[3], r[4], r[5]], [8, 26, 36, 22, 8])
    rng = random.Random(3003)
    for _ in range(5):
        cv[rng.randrange(16), rng.randrange(16)] = r[0]
    for x, y in [(3, 4), (4, 5), (11, 10), (12, 11)]:
        cv[x, y] = r[5]
    return cv


def dusty_ice():
    """Polar layered deposit: ice in bands of varying dust content."""
    beds = [4, 4, 3, 2, 3, 4, 5, 4, 4, 3, 2, 1, 2, 4, 4, 3]
    return laminae(DUSTY_ICE, 3101, beds, grain=0.45, wave=2)


def dry_ice():
    """Frozen CO2: dense white slab with soft frosty clumps and cold-blue shading."""
    r = DRY_ICE
    f = blend((clustered(16, 16, 3201, passes=2), 0.55), (white(3202), 0.45))
    cv, _ = quant(f, [r[0], r[1], r[2], r[3], r[4], r[5]], [5, 15, 30, 28, 16, 6])
    return cv


# --------------------------------------------------------------------------- ore overlays

# hematite: steel-grey blades and specular glints, small clusters (STYLE.md: 15-25 % coverage, no outlines)
HEM = [
    (2, 1, [(0, 0, 3), (1, 0, 1), (1, 1, 0), (2, 1, 1)]),
    (10, 2, [(0, 0, 2), (0, 1, 1), (1, 1, 0)]),
    (13, 6, [(0, 0, 3), (1, 0, 2), (0, 1, 1), (1, 1, 0), (0, 2, 0)]),
    (5, 6, [(0, 0, 2), (1, 0, 3), (0, 1, 1), (1, 1, 1), (2, 1, 0)]),
    (9, 9, [(0, 0, 3), (1, 0, 1), (0, 1, 0)]),
    (1, 10, [(0, 0, 2), (1, 0, 1), (1, 1, 0), (2, 1, 0)]),
    (6, 12, [(0, 0, 3), (1, 0, 2), (0, 1, 1), (1, 1, 0)]),
    (12, 13, [(0, 0, 2), (0, 1, 1), (1, 1, 0)]),
]


def hematite_overlay():
    r = HEMATITE
    return overlay(HEM, [r[2], r[3], r[5], r[7]])


# gypsum: thin white veins crossing the beds
GYP = [
    (1, 3, [(0, 0, 1), (1, 0, 2), (2, 1, 3), (3, 1, 2), (4, 1, 1), (5, 2, 1), (2, 2, 0)]),
    (10, 1, [(0, 0, 2), (1, 0, 3), (1, 1, 1), (2, 2, 2), (3, 2, 1)]),
    (6, 9, [(0, 0, 1), (1, 0, 2), (2, 0, 3), (3, 1, 2), (4, 1, 1), (2, 1, 0)]),
    (13, 8, [(0, 0, 3), (0, 1, 1), (-1, 2, 2)]),
    (1, 13, [(0, 0, 2), (1, 0, 3), (2, 0, 1)]),
]


def gypsum_overlay():
    r = GYPSUM
    return overlay(GYP, [r[1], r[2], r[4], r[5]])


# --------------------------------------------------------------------------- items

def raw_hematite():
    """Lumpy steel-grey chunk with a rusty streak on the lower faces and a few mirror-bright blades."""
    m = mask_from(parse_mask(RAW_CHUNK))
    sh = dome_shade(m, roundness=3.0)
    rng = random.Random(3301)
    r = HEMATITE
    cv = Canvas()
    for y in range(16):
        for x in range(16):
            if m[y][x]:
                v = 0.05 + 0.8 * sh[y][x] + (rng.random() - 0.5) * 0.15
                cv[x, y] = pick([r[2], r[3], r[4], r[5]], v)
    for x, y in [(4, 10), (5, 11), (9, 11), (10, 10), (11, 11)]:
        cv[x, y] = SANDSTONE[2]
    for x, y, l in [(4, 3, 3), (9, 5, 2), (6, 7, 3)]:
        for i in range(l):
            cv[x + i, y] = r[6]
    cv[4, 3] = r[7]
    for x, y, side in edge_pixels(m):
        cv[x, y] = r[0] if side == 'br' else r[1]
    return cv


def gypsum_crystal():
    """A clear selenite blade: long prism leaning right, lit left face, shaded right face."""
    r = GYPSUM
    rows = [
        "................",
        "..........LH....",
        ".........LLHD...",
        "........LLMHD...",
        ".......LLMMD....",
        "......LLMMD.....",
        ".....LLMMD......",
        "....LLMMD.......",
        "...LLMMD..lh....",
        "...LMMD..llhd...",
        "..LMMD..llmd....",
        "..LMD..llmd.....",
        "..MD..llmd......",
        "......lmd.......",
        "................",
        "................",
    ]
    tone = {'H': r[5], 'L': r[4], 'M': r[3], 'D': r[1], 'h': r[4], 'l': r[3], 'm': r[2], 'd': r[0]}
    cv = Canvas()
    for y, row in enumerate(rows):
        for x, ch in enumerate(row):
            if ch in tone:
                cv[x, y] = tone[ch]
    return cv


# --------------------------------------------------------------------------- export

def export(save):
    rock = (40, 12, 20, None)
    dust_spec = (18, 10, 64, None)
    soils = {
        'martian_regolith': (martian_regolith, REGOLITH),
        'hematite_soil': (hematite_soil, REGOLITH),
        'perchlorate_soil': (perchlorate_soil, REGOLITH),
        'martian_clay': (martian_clay, CLAY),
        'co2_frost': (co2_frost, FROST),
    }
    for name, (fn, r) in soils.items():
        base = fn(0)
        save(base, 'block', name, dust_spec)
        save(fn(1), 'block', name + '_1', dust_spec)
        for side in ('left', 'right'):
            save(footprint(base, prints(r), side), 'block', f'{name}_footprint_{side}', (22, 10, 64, None))
    save(martian_sand(), 'block', 'martian_sand', dust_spec)
    save(olivine_sand(), 'block', 'olivine_sand', (30, 14, 50, None))
    save(martian_basalt(), 'block', 'martian_basalt', rock)
    save(polished_martian_basalt(), 'block', 'polished_martian_basalt', (90, 14, 0, None))
    save(martian_basalt_bricks(), 'block', 'martian_basalt_bricks', rock)
    save(martian_sandstone_side(), 'block', 'martian_sandstone', rock)
    save(martian_sandstone_top(), 'block', 'martian_sandstone_top', rock)
    save(martian_sandstone_bottom(), 'block', 'martian_sandstone_bottom', rock)
    save(martian_mudstone(), 'block', 'martian_mudstone', rock)
    save(gypsum_block(), 'block', 'gypsum_block', (120, 20, 0, None))
    ice_spec = lambda c: (200, 22, 0, 255) if c[3] else None
    save(martian_ice(), 'block', 'martian_ice', (200, 22, 0, ice_spec))
    save(dusty_ice(), 'block', 'dusty_ice', (140, 20, 0, None))
    save(dry_ice(), 'block', 'dry_ice', (170, 22, 0, None))
    hem = lambda c: (200, 230, 0, 255) if c[3] and sum(c[:3]) > 300 else ((120, 200, 0, 255) if c[3] else None)
    save(hematite_overlay(), 'block', 'hematite_ore_overlay', (150, 200, 0, hem), wrap=False)
    gyp = lambda c: (150, 30, 0, 255) if c[3] else None
    save(gypsum_overlay(), 'block', 'gypsum_ore_overlay', (120, 30, 0, gyp), wrap=False)
    save(raw_hematite(), 'item', 'raw_hematite')
    save(gypsum_crystal(), 'item', 'gypsum_crystal')
    export_sky(save)


# --------------------------------------------------------------------------- sky and particles

PHOBOS = ramp('#2a2420', '#3b332d', '#4d433b', '#5f544a', '#73675b', '#8a7d6f')


def moon_disc(size, rows, seed, craters):
    """A lumpy little moon: dome-shaded silhouette from an ASCII mask, a few craters (dark pit, lit far rim)."""
    m = mask_from(parse_mask(rows))
    sh = dome_shade(m, roundness=2.2)
    rng = random.Random(seed)
    cv = Canvas(size, size)
    for y in range(size):
        for x in range(size):
            if m[y][x]:
                cv[x, y] = pick(PHOBOS[1:], 0.1 + 0.85 * sh[y][x] + (rng.random() - 0.5) * 0.12)
    for x, y, big in craters:
        cv[x, y] = PHOBOS[0]
        if big:
            cv[x + 1, y] = PHOBOS[1]
            cv[x, y + 1] = PHOBOS[1]
            cv[x + 1, y + 1] = PHOBOS[4]
        else:
            cv[x + 1, y + 1] = PHOBOS[4]
    return cv


def phobos():
    rows = [
        "................",
        "................",
        "................",
        ".....XXXXXX.....",
        "...XXXXXXXXXX...",
        "..XXXXXXXXXXXX..",
        "..XXXXXXXXXXXXX.",
        ".XXXXXXXXXXXXXX.",
        ".XXXXXXXXXXXXXX.",
        ".XXXXXXXXXXXXXX.",
        "..XXXXXXXXXXXX..",
        "...XXXXXXXXXX...",
        "....XXXXXXXX....",
        "................",
        "................",
        "................",
    ]
    return moon_disc(16, rows, 3401, [(4, 6, True), (10, 8, False), (7, 10, False), (12, 5, False)])


def deimos():
    rows = [
        "................",
        "................",
        "................",
        "................",
        "......XXXX......",
        ".....XXXXXX.....",
        "....XXXXXXXX....",
        "....XXXXXXXX....",
        "....XXXXXXXX....",
        ".....XXXXXX.....",
        "......XXXX......",
        "................",
        "................",
        "................",
        "................",
        "................",
    ]
    cv = moon_disc(16, rows, 3402, [(7, 6, False)])
    # Deimos is smoother and paler: lift every tone one step
    for y in range(16):
        for x in range(16):
            c = cv[x, y]
            if c[3] and c[:3] in PHOBOS[:-1]:
                cv[x, y] = PHOBOS[PHOBOS.index(c[:3]) + 1]
    return cv


def dust_mote(i):
    """Tiny pale grain (tinted in game), soft one-pixel edge."""
    shapes = [
        ["....", ".XX.", ".Xx.", "...."],
        ["....", ".X..", ".xX.", "...."],
        ["....", "..X.", ".Xx.", ".x.."],
    ]
    cv = Canvas(8, 8)
    for y, row in enumerate(shapes[i]):
        for x, ch in enumerate(row):
            if ch == 'X':
                cv[x + 2, y + 2] = (230, 214, 196, 255)
            elif ch == 'x':
                cv[x + 2, y + 2] = (200, 180, 160, 170)
    return cv


def export_sky(save):
    save(phobos(), 'environment', 'phobos')
    save(deimos(), 'environment', 'deimos')
    for i in range(3):
        save(dust_mote(i), 'particle', f'dust_mote_{i}')
