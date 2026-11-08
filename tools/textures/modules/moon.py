"""Moon block and item textures: soils (with boot prints), rocks, building stone, ice, ores.

Every texture uses one ramp from sbtex/palettes.py and 4-6 tones (blocks) like vanilla.
Tone shares are set explicitly (percentile mapping), so contrast stays as calm as vanilla
stone/sand whatever the noise does.
"""
import math
import random

from sbtex.core import Canvas, clustered, fbm, normalize, parse_mask, mask_from, dome_shade, edge_pixels, pick, ramp
from sbtex import palettes as P


# --------------------------------------------------------------------------- helpers

def quant(field, tones, shares):
    """Map a 16x16 field onto tones by percentile: shares[i] = fraction of pixels with tone i."""
    h, w = len(field), len(field[0])
    cells = sorted(((field[y][x], x, y) for y in range(h) for x in range(w)))
    total = sum(shares)
    bounds = []
    acc = 0
    for s in shares:
        acc += s
        bounds.append(acc / total * len(cells))
    out = [[0] * w for _ in range(h)]
    i = 0
    for n, (_, x, y) in enumerate(cells):
        while n >= bounds[i] and i < len(bounds) - 1:
            i += 1
        out[y][x] = i
    cv = Canvas(w, h)
    for y in range(h):
        for x in range(w):
            cv[x, y] = tones[out[y][x]]
    return cv, out


def white(seed, w=16, h=16):
    rng = random.Random(seed)
    return [[rng.random() for _ in range(w)] for _ in range(h)]


def blend(*pairs):
    """Weighted sum of fields: blend((f1, w1), (f2, w2), ...)."""
    h, w = len(pairs[0][0]), len(pairs[0][0][0])
    return [[sum(f[y][x] * k for f, k in pairs) for x in range(w)] for y in range(h)]


def blur_dir(f, passes, dx, dy):
    """Directional blur on the torus (streaks along (dx, dy))."""
    h, w = len(f), len(f[0])
    for _ in range(passes):
        g = [[0.0] * w for _ in range(h)]
        for y in range(h):
            for x in range(w):
                g[y][x] = (f[y][x] * 2 + f[(y + dy) % h][(x + dx) % w] + f[(y - dy) % h][(x - dx) % w]) / 4
        f = g
    return f


def voronoi(seed, n, w=16, h=16):
    """Seamless Voronoi cells: returns (cell id grid, edge-distance grid, points)."""
    rng = random.Random(seed)
    pts = [(rng.random() * w, rng.random() * h) for _ in range(n)]
    cell = [[0] * w for _ in range(h)]
    edge = [[0.0] * w for _ in range(h)]
    for y in range(h):
        for x in range(w):
            ds = []
            for i, (px, py) in enumerate(pts):
                dx = min(abs(x + 0.5 - px), w - abs(x + 0.5 - px))
                dy = min(abs(y + 0.5 - py), h - abs(y + 0.5 - py))
                ds.append((dx * dx + dy * dy, i))
            ds.sort()
            cell[y][x] = ds[0][1]
            edge[y][x] = math.sqrt(ds[1][0]) - math.sqrt(ds[0][0])
    return cell, edge, pts


# --------------------------------------------------------------------------- soils

SOIL_SHARES = [5, 20, 36, 26, 10, 3]


def soil(r, seed, specks=True):
    """Powdery soil like vanilla sand: fine grain in soft clumps, a few dark glass beads."""
    f = blend((white(seed), 0.4), (clustered(16, 16, seed + 1, passes=1), 0.42), (fbm(16, 16, seed + 2, [(8, 1.0)]), 0.18))
    cv, idx = quant(f, r, SOIL_SHARES)
    if specks:
        rng = random.Random(seed + 9)
        # a couple of 2-pixel dark glass beads / micro-pits: dark pixel with a lit pixel below-right
        for _ in range(2):
            x, y = rng.randrange(16), rng.randrange(16)
            cv[x, y] = r[0]
            cv[x + 1, y + 1] = r[4]
    return cv


def regolith_ramp():
    return list(P.REGOLITH_SOIL)


def mare_ramp():
    return list(P.MARE_SOIL)


def lunar_regolith(variant=0):
    return soil(regolith_ramp(), 101 + variant * 37)


def mare_regolith(variant=0):
    return soil(mare_ramp(), 211 + variant * 41)


def volcanic_glass_soil(variant=0):
    r = P.VOLCANIC_GLASS
    tones = [r[1], r[2], r[3], r[4], r[5]]
    f = blend((white(331 + variant), 0.6), (clustered(16, 16, 332 + variant, passes=1), 0.4))
    cv, _ = quant(f, tones, [12, 28, 34, 20, 6])
    # black glass beads: single dark pixels scattered (~8 %)
    rng = random.Random(333 + variant)
    for _ in range(20):
        cv[rng.randrange(16), rng.randrange(16)] = r[0]
    return cv


def icy_regolith(variant=0):
    rr = regolith_ramp()
    fr = P.FROST
    f = blend((white(441 + variant), 0.55), (clustered(16, 16, 442 + variant, passes=1), 0.45))
    cv, _ = quant(f, [rr[1], rr[2], rr[3], rr[4]], [16, 38, 32, 14])
    # frost crystals: little 1-3 px glints, pale cyan-white with a bright centre
    rng = random.Random(443 + variant)
    for _ in range(7):
        x, y = rng.randrange(16), rng.randrange(16)
        cv[x, y] = fr[0]
        if rng.random() < 0.5:
            cv[x + 1, y] = fr[0]
    for _ in range(4):
        cv[rng.randrange(16), rng.randrange(16)] = fr[1]
    return cv


BOOT = parse_mask([
    ".XXXX.",
    "XXXXXX",
    "XXXXXX",
    "XXXXXX",
    "XXXXXX",
    "XXXXXX",
    ".XXXX.",
    ".XXXX.",
    ".XXXX.",
    ".XXXX.",
    ".XXXX.",
])


def footprint(base, tones, side):
    """Boot print pointing north (toe at top) pressed into a soil texture.
    tones = (pit shadow, tread dark, tread light, lit wall, rim lit, rim shade)."""
    shadow, tread_d, tread_l, lit_wall, rim_lit, rim_shade = tones
    cv = base.copy()
    m = mask_from(BOOT)
    ox = 3 if side == 'left' else 7
    oy = 3
    h, w = len(m), len(m[0])

    def inside(x, y):
        return 0 <= x < w and 0 <= y < h and m[y][x]

    for y in range(-1, h + 1):
        for x in range(-1, w + 1):
            if inside(x, y):
                continue
            near_tl = inside(x + 1, y) or inside(x, y + 1) or inside(x + 1, y + 1)
            near_br = inside(x - 1, y) or inside(x, y - 1) or inside(x - 1, y - 1)
            if near_tl and not near_br:
                cv.put(ox + x, oy + y, rim_lit)
            elif near_br and not near_tl:
                cv.put(ox + x, oy + y, rim_shade)
    for y in range(h):
        for x in range(w):
            if not m[y][x]:
                continue
            wall_tl = not inside(x - 1, y) or not inside(x, y - 1)
            wall_br = not inside(x + 1, y) or not inside(x, y + 1)
            if wall_tl:
                c = shadow
            elif wall_br:
                c = lit_wall
            else:
                c = tread_l if y % 2 == 0 else tread_d
            cv.put(ox + x, oy + y, c)
    return cv


def soil_prints(r6):
    """Footprint tones from a 6-tone soil ramp."""
    return r6[0], r6[1], r6[2], r6[4], r6[5], r6[2]


def wheel_track(base):
    """Rover wheel rut running north-south, pressed into a soil texture: a 6 px groove with chevron tread
    ridges. Tones come from the soil's own colours (sorted by brightness) plus one deeper pit shadow,
    lit from the top-left like the boot prints."""
    cols = sorted({base[x, y][:3] for y in range(16) for x in range(16)}, key=lambda c: 0.3 * c[0] + 0.59 * c[1] + 0.11 * c[2])
    dark, mid, light = cols[0], cols[min(1, len(cols) - 1)], cols[-1]
    shadow = tuple(int(v * 0.72) for v in dark)
    cv = base.copy()
    x0, x1 = 5, 10
    for y in range(16):
        cv[x0 - 1, y] = light          # rim catching the light
        cv[x1 + 1, y] = mid            # rim in shade
        cv[x0, y] = shadow             # left wall in shadow
        cv[x1, y] = light              # right wall lit
        for x in range(x0 + 1, x1):
            ridge = (y + abs(2 * x - (x0 + x1)) // 2) % 4 == 0
            cv[x, y] = mid if ridge else dark
    return cv


def finish(save):
    """After every module: a wheel-track texture for each soil that has boot prints (all planets)."""
    import glob
    import os
    from PIL import Image
    tex = os.path.join(os.path.dirname(os.path.abspath(__file__)), '..', '..', '..', 'src', 'main', 'resources',
                       'assets', 'spacebreaker', 'textures', 'block')
    n = 0
    for path in sorted(glob.glob(os.path.join(tex, '*_footprint_left.png'))):
        name = os.path.basename(path)[:-len('_footprint_left.png')]
        src = os.path.join(tex, name + '.png')
        if not os.path.exists(src):
            continue
        cv = Canvas()
        cv.img = Image.open(src).convert('RGBA')
        cv.p = cv.img.load()
        save(wheel_track(cv), 'block', f'{name}_footprint_track', (22, 10, 64, None))
        n += 1
    return n


# --------------------------------------------------------------------------- rocks

def mare_basalt():
    r = P.MARE_BASALT
    tones = [r[1], r[2], r[3], r[4], r[5]]
    f = blend((blur_dir(white(11), 2, 1, 0), 0.5), (clustered(16, 16, 12, passes=1), 0.35), (white(13), 0.15))
    cv, idx = quant(f, tones, [8, 26, 38, 22, 6])
    # vesicles: frozen gas bubbles. Dark pit, the lower-right inner wall catches the light.
    for x, y in [(2, 3), (10, 1), (13, 9), (6, 12), (8, 6)]:
        cv[x, y] = r[0]
        cv[x + 1, y] = r[1]
        cv[x, y + 1] = r[4]
    return cv


def anorthosite():
    """Coarse plagioclase crystals: blocky cells, each a flat tone with a lit top-left and shaded bottom-right edge,
    plus rare dark pyroxene grains."""
    r = P.ANORTHOSITE
    cell, edge, pts = voronoi(501, 15)
    rng = random.Random(502)
    base = {i: rng.choice([2, 3, 3, 4, 4, 5]) for i in range(len(pts))}
    cv = Canvas()
    for y in range(16):
        for x in range(16):
            t = base[cell[y][x]]
            c = cell[y][x]
            # facet shading: pixel on the cell border toward bottom-right of its neighbour gets shadow
            # shadow only where a crystal meets a brighter neighbour below/right: soft, blocky facets
            nb = [cell[y][(x + 1) % 16], cell[(y + 1) % 16][x]]
            if any(n != c and base[n] >= base[c] for n in nb):
                t -= 1
            cv[x, y] = r[max(1, min(5, t))]
    rng2 = random.Random(503)
    for x, y in [(4, 2), (12, 6), (2, 11), (9, 13)]:
        cv[x, y] = r[1]
        if rng2.random() < 0.5:
            cv[x + 1, y] = r[1]
    return cv


def polished_anorthosite():
    """Like polished andesite: one smooth slab with a bevelled rim."""
    r = P.ANORTHOSITE
    f = blend((clustered(16, 16, 611, passes=2), 0.6), (white(612), 0.4))
    cv, _ = quant(f, [r[2], r[3], r[4], r[5]], [14, 40, 36, 10])
    for i in range(16):
        cv[i, 0] = r[5]
        cv[0, i] = r[5]
        cv[i, 15] = r[1]
        cv[15, i] = r[1]
    cv[0, 15] = r[3]
    cv[15, 0] = r[3]
    for i in range(1, 15):
        cv[i, 1] = r[4] if cv[i, 1] != r[5] else r[5]
        cv[1, i] = r[4] if cv[1, i] != r[5] else r[5]
        cv[i, 14] = r[2]
        cv[14, i] = r[2]
    return cv


def bricks(r, seed, rows=((0, 0), (4, 4), (8, 0), (12, 4)), brick_w=8, mortar=None, face=None, shares=(20, 45, 35)):
    """Running-bond bricks, vanilla stone-brick layout: 4 rows of 4 px, joints every brick_w px.
    face = tones for the brick face (dark->light), mortar = joint colour."""
    face = face or [r[2], r[3], r[4]]
    mortar = mortar or r[0]
    f = blend((clustered(16, 16, seed, passes=1), 0.5), (white(seed + 1), 0.5))
    cv, _ = quant(f, face, list(shares))
    for ry, off in rows:
        for x in range(16):
            cv[x, (ry + 3) % 16] = mortar  # bed joint under each course
        for j in range(0, 16, brick_w):
            jx = (j + off) % 16
            for y in range(ry, ry + 3):
                cv[jx, y % 16] = mortar
        # top edge of each brick lit, right end shaded
        for x in range(16):
            if cv[x, ry] != mortar:
                cv[x, ry] = r[5]
        for j in range(0, 16, brick_w):
            jx = (j + off - 1) % 16
            for y in range(ry, ry + 3):
                if cv[jx, y % 16] != mortar:
                    cv[jx, y % 16] = r[1]
            sx = (j + off + 1) % 16
            for y in range(ry + 1, ry + 3):
                if cv[sx, y % 16] not in (mortar, r[5]):
                    cv[sx, y % 16] = r[4]
    return cv


def anorthosite_bricks():
    r = P.ANORTHOSITE
    return bricks([r[0], r[1], r[2], r[3], r[4], r[5]], 701, face=[r[2], r[3], r[4]], mortar=r[0])


def impact_breccia():
    """Shocked melt matrix full of angular clasts: pale anorthosite chips and dark basalt chips."""
    r = P.BRECCIA
    f = blend((white(801), 0.55), (clustered(16, 16, 802, passes=1), 0.45))
    cv, _ = quant(f, [r[1], r[2], r[3]], [25, 45, 30])
    clasts = [  # (x, y, shape rows, light?)
        (1, 1, ["XXX", "XXXX", ".XX"], True),
        (9, 0, ["XX", "XXX"], False),
        (12, 4, ["XXX", "XX"], True),
        (5, 6, ["XX", "XXX", "XXX", "X.."], True),
        (0, 10, ["XX", "X"], False),
        (10, 10, ["XXXX", "XXX", ".X"], True),
        (3, 13, ["XXX", ".XX"], False),
        (14, 13, ["XX", "XX"], True),
    ]
    for ox, oy, rows, light in clasts:
        m = [[c == 'X' for c in row] for row in rows]
        hgt = len(m)
        for y, row in enumerate(m):
            for x, on in enumerate(row):
                if not on:
                    continue
                top = y == 0 or x >= len(m[y - 1]) or not m[y - 1][x]
                lft = x == 0 or not row[x - 1]
                bot = y == hgt - 1 or x >= len(m[y + 1]) or not m[y + 1][x]
                rgt = x == len(row) - 1 or not row[x + 1]
                if light:
                    c = r[6] if (top or lft) else r[4]
                else:
                    c = r[1] if (top or lft) else r[0]
                cv[ox + x, oy + y] = c
    return cv


def lunar_ice():
    """Dusty polar ice: packed-ice style diagonal grain with dark dust inclusions."""
    r = P.LUNAR_ICE
    f = blend((blur_dir(white(901), 3, 1, 1), 0.65), (clustered(16, 16, 902, passes=1), 0.35))
    cv, _ = quant(f, [r[1], r[2], r[3], r[4], r[5]], [8, 26, 36, 22, 8])
    rng = random.Random(903)
    for _ in range(5):
        x, y = rng.randrange(16), rng.randrange(16)
        cv[x, y] = r[0]
    # a couple of bright fracture glints along the grain
    for x, y in [(3, 4), (4, 5), (11, 10), (12, 11)]:
        cv[x, y] = r[5]
    return cv


def sintered_regolith():
    """Regolith melted into a hard ceramic: smooth, with tiny pores and a slight glassy sheen."""
    r = P.SINTERED
    f = blend((clustered(16, 16, 1001, passes=2), 0.55), (white(1002), 0.45))
    cv, _ = quant(f, [r[2], r[3], r[4], r[5]], [14, 38, 36, 12])
    rng = random.Random(1003)
    for _ in range(7):
        x, y = rng.randrange(16), rng.randrange(16)
        cv[x, y] = r[1]
        cv[x + 1, y + 1] = r[5]
    return cv


def sintered_regolith_bricks():
    r = P.SINTERED
    return bricks([r[0], r[1], r[2], r[3], r[4], r[6]], 1101, rows=((0, 0), (4, 6), (8, 2), (12, 8)),
                  face=[r[2], r[3], r[4]], mortar=r[0])


def meteoric_iron():
    """Iron-nickel meteorite: dark fusion crust dimpled with thumb-print hollows (regmaglypts).
    Each hollow is a shallow bowl: shaded on its top-left inner slope, lit on the bottom-right one,
    with bright metal only where the crust chipped off the ridges."""
    r = P.METEORIC_IRON
    cell, edge, pts = voronoi(1201, 7)
    radius = {}
    for y in range(16):
        for x in range(16):
            c = cell[y][x]
            px, py = pts[c]
            dx = (x + 0.5 - px + 8) % 16 - 8
            dy = (y + 0.5 - py + 8) % 16 - 8
            radius[c] = max(radius.get(c, 1.0), (dx * dx + dy * dy) ** 0.5)
    cv = Canvas()
    for y in range(16):
        for x in range(16):
            c = cell[y][x]
            px, py = pts[c]
            dx = (x + 0.5 - px + 8) % 16 - 8
            dy = (y + 0.5 - py + 8) % 16 - 8
            d = (dx * dx + dy * dy) ** 0.5
            rad = radius[c]
            # bowl: brightness follows the slope direction, strongest near the rim
            slope = ((dx + dy) / (d + 1e-6)) * min(1.0, d / rad) if d > 0.3 else 0.0
            t = 2 + slope * 1.1
            if edge[y][x] < 0.8:
                t = 3.4  # rounded ridge between hollows
            cv[x, y] = r[max(1, min(4, int(round(t))))]
    for x, y in [(3, 5), (11, 2), (13, 12), (6, 13), (8, 8)]:
        if cv[x, y] in (r[3], r[4]):
            cv[x, y] = r[5]
    cv[11, 2] = r[6]
    return cv


# --------------------------------------------------------------------------- ore overlays

def overlay(clusters, tones):
    """clusters: list of (x, y, pixel list [(dx, dy, tone index)])."""
    cv = Canvas()
    for x, y, px in clusters:
        for dx, dy, t in px:
            cv[(x + dx) % 16, (y + dy) % 16] = tones[t]
    return cv


# small irregular flecks, one light pixel per cluster, no outlines (STYLE.md)
ILM = [
    (2, 2, [(0, 0, 3), (1, 0, 1), (0, 1, 1), (1, 1, 0)]),
    (9, 1, [(0, 0, 2), (1, 0, 1), (1, 1, 0)]),
    (13, 4, [(0, 0, 3), (0, 1, 1), (-1, 1, 0), (0, 2, 0)]),
    (5, 6, [(0, 0, 2), (1, 0, 3), (0, 1, 1), (1, 1, 1), (2, 1, 0), (1, 2, 0)]),
    (10, 8, [(0, 0, 3), (1, 0, 1), (0, 1, 0)]),
    (1, 10, [(0, 0, 2), (1, 0, 1), (1, 1, 0)]),
    (6, 12, [(0, 0, 3), (1, 0, 2), (0, 1, 1), (1, 1, 0), (2, 1, 0)]),
    (13, 12, [(0, 0, 3), (0, 1, 1), (1, 1, 0)]),
]


def ilmenite_overlay():
    r = P.ILMENITE
    return overlay(ILM, [r[3], r[4], r[5], r[6]])


KRP = [
    (3, 1, [(0, 0, 3), (0, 1, 2), (1, 1, 1)]),
    (11, 2, [(0, 0, 2), (1, 0, 3), (1, 1, 1), (0, 1, 1)]),
    (7, 5, [(0, 0, 3), (0, 1, 1)]),
    (1, 7, [(0, 0, 2), (1, 0, 3), (0, 1, 1), (1, 1, 0), (2, 0, 1)]),
    (12, 8, [(0, 0, 3), (0, 1, 2), (-1, 1, 1), (0, 2, 0)]),
    (6, 10, [(0, 0, 3), (1, 0, 1)]),
    (3, 13, [(0, 0, 2), (1, 0, 3), (1, 1, 1), (2, 1, 0)]),
    (10, 13, [(0, 0, 3), (0, 1, 1), (1, 1, 0)]),
]


def kreep_overlay():
    r = P.KREEP
    return overlay(KRP, [r[0], r[1], r[2], r[4]])


# --------------------------------------------------------------------------- items

RAW_CHUNK = [
    "................",
    "....XXX.........",
    "...XXXXXX..XX...",
    "..XXXXXXXXXXXX..",
    "..XXXXXXXXXXXXX.",
    ".XXXXXXXXXXXXXX.",
    ".XXXXXXXXXXXXXX.",
    "XXXXXXXXXXXXXXX.",
    "XXXXXXXXXXXXXXXX",
    ".XXXXXXXXXXXXXXX",
    ".XXXXXXXXXXXXXX.",
    "..XXXXXXXXXXXXX.",
    "...XXXX..XXXXX..",
    "....XX....XXX...",
    "................",
    "................",
]


def raw_ilmenite():
    """Lumpy glossy-black chunk; its flat facets throw a violet sheen (tells it apart from haematite and magnetite)."""
    r = P.ILMENITE
    violet = ramp('#2c1f45', '#4d3480', '#7a58c0', '#b497ec', '#e6dcff')
    m = mask_from(parse_mask(RAW_CHUNK))
    sh = dome_shade(m, roundness=3.0)
    rng = random.Random(1301)
    cv = Canvas()
    for y in range(16):
        for x in range(16):
            if m[y][x]:
                v = 0.05 + 0.8 * sh[y][x] + (rng.random() - 0.5) * 0.15
                cv[x, y] = pick([r[1], r[2], r[3], r[4]], v)
    # flat crystal faces catching the light (tabular ilmenite): violet sheen, a bright glint, a dark lower edge
    for x, y, l in [(4, 3, 3), (10, 4, 2), (3, 7, 2), (8, 8, 3), (11, 10, 2)]:
        for i in range(l):
            cv[x + i, y] = violet[2] if i else violet[3]
            cv[x + i, y + 1] = violet[1] if i < l - 1 else violet[0]
    cv[4, 3] = violet[4]
    cv[8, 8] = violet[4]
    for x, y, side in edge_pixels(m):
        cv[x, y] = r[0] if side == 'br' else violet[0]
    return cv


def kreep_fragment():
    """A breccia shard studded with pale yellow-green phosphate crystals."""
    k = P.KREEP
    b = P.BRECCIA
    rows = [
        "................",
        "................",
        "................",
        ".........kK.....",
        "......kK.kkK....",
        ".....bkkbbkb....",
        "....bbbbbbbbb...",
        "...bbbbbkKbbbb..",
        "...bbkKbkkbbbb..",
        "..bbbkkbbbbbbb..",
        "..bbbbbbbbbbb...",
        "...bbbbbbbbbb...",
        "....bbbbbbbb....",
        "......bbbb......",
        "................",
        "................",
    ]
    m = mask_from(parse_mask(rows))
    sh = dome_shade(m, roundness=3.0)
    cv = Canvas()
    for y, row in enumerate(rows):
        for x, ch in enumerate(row):
            if ch == '.':
                continue
            if ch == 'K':
                cv[x, y] = k[5]
            elif ch == 'k':
                cv[x, y] = k[3] if sh[y][x] > 0.5 else k[2]
            else:
                cv[x, y] = pick([b[2], b[3], b[4], b[5]], 0.1 + 0.85 * sh[y][x])
    for x, y, side in edge_pixels(m):
        ch = rows[y][x]
        if ch in 'kK':
            cv[x, y] = k[1] if side == 'br' else k[4]
        else:
            cv[x, y] = b[0] if side == 'br' else b[1]
    return cv


# --------------------------------------------------------------------------- export

def export(save):
    rock = (40, 12, 20, None)
    dust = (18, 10, 64, None)
    soils = {
        'lunar_regolith': (lunar_regolith, regolith_ramp()),
        'mare_regolith': (mare_regolith, mare_ramp()),
        'volcanic_glass_soil': (volcanic_glass_soil, [P.VOLCANIC_GLASS[i] for i in (1, 2, 3, 4, 5, 5)]),
        'icy_regolith': (icy_regolith, [regolith_ramp()[i] for i in (1, 1, 2, 3, 4, 4)]),
    }
    for name, (fn, ramp6) in soils.items():
        base = fn(0)
        save(base, 'block', name, dust)
        save(fn(1), 'block', name + '_1', dust)
        for side in ('left', 'right'):
            save(footprint(base, soil_prints(ramp6), side), 'block', f'{name}_footprint_{side}', (22, 10, 64, None))
    save(mare_basalt(), 'block', 'mare_basalt', rock)
    save(anorthosite(), 'block', 'anorthosite', rock)
    save(polished_anorthosite(), 'block', 'polished_anorthosite', (90, 14, 0, None))
    save(anorthosite_bricks(), 'block', 'anorthosite_bricks', rock)
    save(impact_breccia(), 'block', 'impact_breccia', rock)
    ice_spec = lambda c: (200, 22, 0, 255) if c[3] else None
    save(lunar_ice(), 'block', 'lunar_ice', (200, 22, 0, ice_spec))
    save(sintered_regolith(), 'block', 'sintered_regolith', (70, 14, 10, None))
    save(sintered_regolith_bricks(), 'block', 'sintered_regolith_bricks', (60, 14, 10, None))
    metal = lambda c: (170, 230, 0, 255) if c[3] and sum(c[:3]) > 330 else None
    save(meteoric_iron(), 'block', 'meteoric_iron', (60, 20, 0, metal))
    ilm_rules = lambda c: (220, 230, 0, 255) if c[3] else None
    save(ilmenite_overlay(), 'block', 'ilmenite_ore_overlay', (180, 230, 0, ilm_rules), wrap=False)
    kreep_rules = lambda c: (170, 40, 0, 255) if c[3] else None
    save(kreep_overlay(), 'block', 'kreep_ore_overlay', (150, 40, 0, kreep_rules), wrap=False)
    save(raw_ilmenite(), 'item', 'raw_ilmenite')
    save(kreep_fragment(), 'item', 'kreep_fragment')


# --------------------------------------------------------------------------- glimmer lamp and shell

GLIMMER = [(31, 90, 80), (58, 156, 134), (111, 216, 191), (194, 245, 231)]


def glimmer_lamp():
    """Sintered frame around a glass pane, behind it a stack of glowing beetle shells."""
    s = P.SINTERED
    g = GLIMMER
    cv = Canvas()
    for y in range(16):
        for x in range(16):
            edge = min(x, y, 15 - x, 15 - y)
            if edge == 0:
                c = s[3] if (x == 0 or y == 0) else s[1]
            elif edge == 1:
                c = s[2] if (x == 1 or y == 1) else s[3]
            else:
                # glowing shells: oval segments with darker seams
                seam = (x + 2 * (y // 4)) % 6 == 0 or y % 4 == 1
                c = g[1] if seam else (g[3] if (x + y) % 5 == 0 else g[2])
            cv[x, y] = c
    return cv


SHELL = [
    "................",
    "................",
    "......aaaa......",
    ".....abbbba.....",
    "....abcccbba....",
    "....abcddcba....",
    "...abccddccba...",
    "...abcdeedcba...",
    "...abcdeedcba...",
    "...abccddccba...",
    "....abcddcba....",
    "....abccccba....",
    ".....abbbba.....",
    "......aaaa......",
    "................",
    "................",
]


def glimmer_shell():
    """A shed glimmer beetle shell: dark teal plate with a luminous core."""
    cols = {'a': (17, 23, 26), 'b': (38, 53, 56), 'c': (74, 101, 99), 'd': (58, 156, 134), 'e': (194, 245, 231)}
    cv = Canvas()
    for y, row in enumerate(SHELL):
        for x, ch in enumerate(row):
            if ch in cols:
                cv[x, y] = cols[ch]
    # light from the top-left
    cv[6, 4] = (103, 135, 131)
    cv[5, 5] = (103, 135, 131)
    return cv


_export_moon = export


def export(save):  # noqa: F811 - extend the module export
    _export_moon(save)
    glow = lambda c: (120, 20, 0, 200) if c[3] and c[1] > 140 else None
    save(glimmer_lamp(), 'block', 'glimmer_lamp', (120, 20, 0, glow))
    save(glimmer_shell(), 'item', 'glimmer_shell')


def ice_core_sample():
    """A drilled ice core lying diagonally: pale blue-white layered ice with darker dust bands, metal end caps."""
    ice = [(140, 180, 206), (186, 218, 234), (226, 242, 250)]
    dust = [(84, 96, 106), (110, 122, 130), (132, 144, 152)]
    cap = [(52, 56, 62), (104, 110, 118), (158, 164, 170)]
    cv = Canvas()
    for i in range(12):
        x, y = 2 + i, 13 - i
        r = cap if i in (0, 11) else dust if i in (4, 7) else ice
        cv[x, y - 1] = r[2]   # upper-left edge catches the light
        cv[x, y] = r[1]
        cv[x + 1, y] = r[0]   # lower-right edge in shade
    return cv


_export_moon_ice = export


def export(save):  # noqa: F811 - extend the module export
    _export_moon_ice(save)
    save(ice_core_sample(), 'item', 'ice_core_sample')
