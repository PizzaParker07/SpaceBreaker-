"""Asteroid belt textures: Ceres (dark carbonaceous regolith, ammoniated clay, bright carbonate salt crust, hydrated rock,
buried ice, magnetite ore) and Vesta (howardite regolith, eucrite basalt and bricks, diogenite, chromite ore), their
items, the Rock Mite, and the two small worlds as discs for skies and maps.

Vanilla execution: 4-6 tones per block from one ramp, calm clumpy noise, light from the top-left.
"""
import math
import random

from sbtex.core import Canvas, clustered, fbm, parse_mask, mask_from, dome_shade, edge_pixels, pick
from sbtex import palettes as P
from modules.moon import quant, white, blend, blur_dir, voronoi, footprint, bricks, overlay, RAW_CHUNK
from modules.mars import dust, prints
from modules.moon_creatures import paint, noise_pick


# --------------------------------------------------------------------------- Ceres

def ceres_regolith(v=0):
    """Fine dark dust like vanilla sand, but nearly charcoal: Ceres reflects under a tenth of the sunlight."""
    return dust(P.CERES_SOIL, 8001 + v * 37)


def ammoniated_clay(v=0):
    """Smooth, slightly greasy clay in soft blotches, a few fine shrinkage cracks (dark line, lit lower lip)."""
    r = P.AMMONIA_CLAY
    f = blend((clustered(16, 16, 8101 + v, passes=2), 0.62), (white(8102 + v), 0.38))
    cv, _ = quant(f, r, [3, 17, 35, 30, 12, 3])
    cracks = [[(2, 3), (3, 3), (4, 4), (5, 4)], [(10, 8), (11, 9), (11, 10), (12, 11)], [(4, 12), (5, 12), (6, 13)]]
    if v:
        cracks = [[((x + 6) % 15, (y + 4) % 15) for x, y in c] for c in cracks]
    for line in cracks:
        for x, y in line:
            cv[x, y] = r[0]
            cv[x, y + 1] = r[5]
    return cv


def carbonate_crust(v=0):
    """Dried brine: a bright salt crust in soft blotches like calcite, broken by a few thin grey cracks whose lower lip
    is pushed up and catches the light, like the edge of a salt-pan polygon."""
    r = P.CARBONATE
    cell, edge, pts = voronoi(8201 + v * 13, 4)
    f = blend((clustered(16, 16, 8202 + v, passes=2), 0.6), (white(8203 + v), 0.4))
    cv, _ = quant(f, [r[2], r[3], r[4], r[5]], [10, 34, 40, 16])
    for y in range(16):
        for x in range(16):
            if edge[y][x] < 0.32:
                cv[x, y] = r[1]
                rx, ry = x, (y + 1) % 16
                if edge[ry][rx] >= 0.32:
                    cv[rx, ry] = r[5]
    rng = random.Random(8204 + v)
    for _ in range(2):
        x, y = rng.randrange(16), rng.randrange(16)
        if cv[x, y] != r[1]:
            cv[x, y] = r[0]
    return cv


def cerean_rock():
    """Dark hydrated rock, like tuff with a brown cast: soft larger blotches over fine grain, a few pale clasts
    (lit top-left, shade below-right) and dark pits."""
    r = P.CEREAN_ROCK
    f = blend((clustered(16, 16, 8301, passes=2), 0.5), (fbm(16, 16, 8303, [(8, 1.0), (4, 0.5)]), 0.25), (white(8302), 0.25))
    cv, _ = quant(f, [r[1], r[2], r[3], r[4], r[5]], [10, 24, 34, 22, 10])
    for x, y in [(3, 2), (12, 5), (7, 10), (1, 13)]:
        cv[x, y] = r[5]
        cv[x + 1, y] = r[4]
        cv[x + 1, y + 1] = r[1]
    for x, y in [(9, 1), (14, 11), (5, 6)]:
        cv[x, y] = r[0]
    return cv


def cerean_ice():
    """Dirty ground ice: broad soft diagonal bands like packed ice, grey-blue with carbon dust specks and bright facets."""
    r = P.CEREAN_ICE
    f = blend((blur_dir(clustered(16, 16, 8401, passes=1), 2, 1, 1), 0.6), (fbm(16, 16, 8404, [(8, 1.0)]), 0.2), (white(8402), 0.2))
    cv, _ = quant(f, [r[1], r[2], r[3], r[4], r[5]], [8, 26, 36, 22, 8])
    rng = random.Random(8403)
    for _ in range(4):
        cv[rng.randrange(16), rng.randrange(16)] = r[0]
    for x, y in [(4, 3), (5, 4), (12, 9), (13, 10)]:
        cv[x, y] = r[5]
    return cv


# magnetite: chunky grains with a blue-steel metallic sheen (the host rock is dark, so the ore reads by its lustre)
MAG = [
    (1, 1, [(1, 0, 1), (2, 0, 2), (0, 1, 1), (1, 1, 3), (2, 1, 2), (3, 1, 0), (1, 2, 0), (2, 2, 1)]),
    (10, 2, [(0, 0, 2), (1, 0, 1), (0, 1, 3), (1, 1, 1), (2, 1, 0), (1, 2, 0)]),
    (12, 8, [(1, 0, 2), (0, 1, 1), (1, 1, 3), (2, 1, 1), (0, 2, 0), (1, 2, 1), (2, 2, 0)]),
    (4, 7, [(1, 0, 1), (2, 0, 2), (0, 1, 1), (1, 1, 3), (2, 1, 2), (1, 2, 0), (2, 2, 0), (3, 2, 1)]),
    (8, 12, [(0, 0, 2), (1, 0, 3), (0, 1, 1), (1, 1, 1), (2, 1, 0)]),
    (1, 12, [(0, 0, 2), (1, 0, 1), (0, 1, 0), (1, 1, 1)]),
]


def magnetite_overlay():
    r = P.MAGNETITE
    return overlay(MAG, [r[2], r[3], r[3], r[4]])


def raw_lump(r, seed, glints):
    """A raw-ore lump (vanilla raw iron silhouette) shaded as a dome, outline from the material's own dark tones."""
    m = mask_from(parse_mask(RAW_CHUNK))
    sh = dome_shade(m, roundness=3.0)
    rng = random.Random(seed)
    cv = Canvas()
    for y in range(16):
        for x in range(16):
            if m[y][x]:
                v = 0.05 + 0.8 * sh[y][x] + (rng.random() - 0.5) * 0.14
                cv[x, y] = pick(r[2:-1], v)
    for x, y in glints:
        cv[x, y] = r[-1]
    for x, y, side in edge_pixels(m):
        cv[x, y] = r[0] if side == 'br' else r[1]
    return cv


def raw_magnetite():
    r = ['#0c0c0e', '#17181b', '#24252a', '#33353b', '#44474e', '#5a5d65', '#80848d', '#b4b8bf']
    from sbtex.core import hexc
    return raw_lump([hexc(c) for c in r], 8501, [(5, 4), (9, 7), (6, 10)])


# --------------------------------------------------------------------------- Vesta

def vestan_regolith(v=0):
    """Grey-buff howardite dust: brighter than Ceres, with a few dark glassy grains."""
    return dust(P.VESTA_SOIL, 8601 + v * 41)


def eucrite():
    """Fine grey basalt with pale plagioclase laths: short light streaks in a few directions over a calm matrix."""
    r = P.EUCRITE
    f = blend((clustered(16, 16, 8701, passes=1), 0.5), (white(8702), 0.5))
    cv, _ = quant(f, [r[1], r[2], r[3], r[4]], [12, 34, 38, 16])
    laths = [((2, 2), (1, 0), 3), ((9, 1), (1, 1), 2), ((13, 5), (0, 1), 3), ((5, 7), (1, -1), 2), ((10, 10), (1, 0), 3),
             ((1, 11), (1, 1), 2), ((6, 13), (1, 0), 2), ((13, 13), (0, 1), 2)]
    for (x, y), (dx, dy), n in laths:
        for i in range(n):
            cv[(x + dx * i) % 16, (y + dy * i) % 16] = r[5]
        cv[(x + dx * n) % 16, (y + dy * n + 1) % 16] = r[0]
    return cv


def eucrite_bricks():
    r = P.EUCRITE
    return bricks([r[0], r[1], r[2], r[3], r[4], r[5]], 8801, face=[r[2], r[3], r[4]], mortar=r[0])


def diogenite():
    """Coarse orthopyroxene: blocky greenish-brown crystals, each a flat tone with a shaded lower-right edge."""
    r = P.DIOGENITE
    cell, edge, pts = voronoi(8901, 18)
    rng = random.Random(8902)
    base = {i: rng.choice([2, 3, 3, 3, 4, 4, 5]) for i in range(len(pts))}
    grain = white(8903)
    cv = Canvas()
    for y in range(16):
        for x in range(16):
            c = cell[y][x]
            t = base[c] + (1 if grain[y][x] > 0.86 else (-1 if grain[y][x] < 0.14 else 0))
            nb = [cell[y][(x + 1) % 16], cell[(y + 1) % 16][x]]
            if any(n != c and base[n] > base[c] for n in nb):
                t -= 1
            cv[x, y] = r[max(1, min(5, t))]
    for x, y in [(4, 3), (11, 8), (2, 12)]:
        cv[x, y] = r[0]
    return cv


# chromite: small black-brown grains in chains (they settle in layers in the cumulate rock)
CHR = [
    (1, 2, [(0, 0, 2), (1, 0, 1), (2, 0, 1), (0, 1, 1), (1, 1, 0), (2, 1, 0)]),
    (9, 1, [(0, 0, 3), (1, 0, 1), (0, 1, 1), (1, 1, 0)]),
    (4, 6, [(0, 0, 2), (1, 0, 3), (2, 0, 1), (0, 1, 1), (1, 1, 0), (2, 1, 1), (3, 1, 0)]),
    (12, 6, [(0, 0, 2), (1, 0, 1), (0, 1, 1), (1, 1, 0), (0, 2, 0)]),
    (1, 11, [(0, 0, 3), (1, 0, 1), (0, 1, 1), (1, 1, 0)]),
    (8, 11, [(0, 0, 2), (1, 0, 2), (2, 0, 1), (1, 1, 1), (2, 1, 0), (3, 1, 0)]),
    (13, 13, [(0, 0, 3), (1, 0, 1), (0, 1, 0)]),
]


def chromite_overlay():
    r = P.CHROMITE
    return overlay(CHR, [r[0], r[1], r[3], r[4]])


def chromite_item():
    r = ['#0f0c0b', '#1b1614', '#28211e', '#372e29', '#4a3f38', '#5e5149', '#7d726b', '#a39890']
    from sbtex.core import hexc
    return raw_lump([hexc(c) for c in r], 9001, [(5, 4), (6, 4), (10, 8)])


# --------------------------------------------------------------------------- Rock Mite (64x32, RockMiteModel)

def rock_mite():
    cv = Canvas(64, 32)
    r = P.ROCK_MITE
    salt = P.MITE_SALT
    n = clustered(64, 64, 9101, passes=1)

    def body(face, x, y, fw, fh):
        if face == 'top':
            # segmented carapace: a darker groove every third row, pale salt grains on the rear half
            t = 3 if (y % 3) else 2
            if x in (0, fw - 1):
                t -= 1
            if y > fh * 0.55 and n[y][x + 5] > 0.82:
                return salt[1]
            return noise_pick(r, t, n, x, y)
        if face == 'bottom':
            return r[1]
        # sides: carapace edge on top, darker underside
        return noise_pick(r, 3 if y == 0 else (2 if y < 2 else 1), n, x + 20, y + 30)

    paint(cv, 0, 0, 8, 4, 10, body)

    def hump(face, x, y, fw, fh):
        if face == 'top':
            if n[y + 20][x] > 0.7:
                return salt[0]
            return noise_pick(r, 4, n, x + 11, y + 3)
        return noise_pick(r, 3, n, x + 9, y + 40)

    paint(cv, 0, 14, 6, 2, 5, hump)

    def head(face, x, y, fw, fh):
        if face == 'front':
            # two dark sensory pits instead of eyes
            if y == 1 and x in (1, fw - 2):
                return r[0]
            return r[3] if y == 0 else r[2]
        return noise_pick(r, 3 if face == 'top' else 2, n, x + 40, y)

    paint(cv, 36, 0, 5, 3, 3, head)
    paint(cv, 36, 6, 1, 1, 3, lambda f, x, y, fw, fh: r[4] if f in ('top', 'front') else r[2])

    def crystal(face, x, y, fw, fh):
        if face == 'top':
            return salt[3]
        if face in ('front', 'right'):
            return salt[2] if y < fh - 1 else salt[1]
        return salt[1] if y < fh - 1 else salt[0]

    paint(cv, 44, 6, 2, 3, 2, crystal)
    paint(cv, 52, 0, 1, 2, 1, crystal)
    paint(cv, 22, 14, 5, 1, 1, lambda f, x, y, fw, fh: (r[3] if x < 3 else r[2]) if f == 'top' else r[1])
    return cv


# --------------------------------------------------------------------------- discs (sky, maps)

def body_disc(size, tones, seed, spots=(), basin=None, lumpy=0.0):
    """A small world as a full disc in a few flat tones (like the vanilla Moon): noise albedo, crater pits with a lit
    far rim, optional bright spots and a big basin. Phase shading is applied in game."""
    from PIL import Image
    rng = random.Random(seed)
    img = Image.new('RGBA', (size, size), (0, 0, 0, 0))
    px = img.load()
    c = (size - 1) / 2
    craters = [(rng.uniform(-0.8, 0.8), rng.uniform(-0.8, 0.8), rng.uniform(0.04, 0.13)) for _ in range(26)]
    grid = [[rng.random() for _ in range(10)] for _ in range(10)]

    def noise(u, v):
        fx, fy = (u + 1) * 4, (v + 1) * 4
        ix, iy = int(fx), int(fy)
        tx, ty = fx - ix, fy - iy
        a = grid[iy][ix] * (1 - tx) + grid[iy][ix + 1] * tx
        b = grid[iy + 1][ix] * (1 - tx) + grid[iy + 1][ix + 1] * tx
        return a * (1 - ty) + b * ty

    for y in range(size):
        for x in range(size):
            u = (x - c) / c
            v = (y - c) / c
            ang = math.atan2(v, u)
            edge = 1 - lumpy * (0.5 + 0.5 * math.sin(ang * 3 + seed)) * 0.12
            if math.hypot(u, v) > edge:
                continue
            al = 0.45 + (noise(u, v) - 0.5) * 0.6
            t = max(0, min(len(tones) - 1, int(al * len(tones))))
            col = tones[t]
            for cu, cv_, cr in craters:
                d = math.hypot(u - cu, v - cv_) / cr
                if d < 1:
                    # pit shaded on the near (upper-left) wall, lit on the far wall
                    lit = (u - cu) + (v - cv_)
                    col = tones[0] if lit < 0 else tones[min(len(tones) - 1, t + 1)]
                    if d > 0.8:
                        col = tones[min(len(tones) - 1, t + 1)] if lit > 0 else tones[max(0, t - 1)]
            if basin:
                bu, bv, br = basin
                d = math.hypot(u - bu, v - bv) / br
                if d < 1:
                    col = tones[1] if d > 0.82 else (tones[2] if d > 0.25 else tones[3])
            for su, sv, sr, scol in spots:
                if math.hypot(u - su, v - sv) < sr:
                    col = scol
            px[x, y] = (col[0], col[1], col[2], 255)
    return img


def ceres_disc():
    from sbtex.core import hexc
    tones = [hexc(h) for h in ('#2c2a28', '#3a3734', '#46423e', '#524d48', '#5e5853')]
    bright = hexc('#e2dfd8')
    return body_disc(128, tones, 9201, spots=[(0.18, -0.22, 0.035, bright), (0.24, -0.2, 0.018, bright), (-0.4, 0.3, 0.015, bright)])


def vesta_disc():
    from sbtex.core import hexc
    tones = [hexc(h) for h in ('#4d4944', '#605b55', '#736d66', '#857e76', '#968f86')]
    return body_disc(128, tones, 9202, basin=(0.0, 0.78, 0.55), lumpy=1.0)


# --------------------------------------------------------------------------- export

def export(save):
    dust_spec = (18, 8, 64, None)
    for name, fn, r in (('ceres_regolith', ceres_regolith, P.CERES_SOIL), ('ammoniated_clay', ammoniated_clay, P.AMMONIA_CLAY),
                        ('carbonate_crust', carbonate_crust, P.CARBONATE), ('vestan_regolith', vestan_regolith, P.VESTA_SOIL)):
        base = fn(0)
        spec = (60, 12, 40, None) if name == 'carbonate_crust' else dust_spec
        save(base, 'block', name, spec)
        save(fn(1), 'block', name + '_1', spec)
        for side in ('left', 'right'):
            save(footprint(base, prints(r), side), 'block', f'{name}_footprint_{side}', (22, 8, 64, None))
    rock = (40, 12, 20, None)
    save(cerean_rock(), 'block', 'cerean_rock', rock)
    save(cerean_ice(), 'block', 'cerean_ice', (200, 20, 0, None))
    save(eucrite(), 'block', 'eucrite', rock)
    save(eucrite_bricks(), 'block', 'eucrite_bricks', rock)
    save(diogenite(), 'block', 'diogenite', (60, 14, 10, None))
    metal = lambda c: (170, 220, 0, 255) if c[3] else None
    save(magnetite_overlay(), 'block', 'magnetite_ore_overlay', (150, 210, 0, metal), wrap=False)
    save(chromite_overlay(), 'block', 'chromite_ore_overlay', (130, 190, 0, metal), wrap=False)
    save(raw_magnetite(), 'item', 'raw_magnetite')
    save(chromite_item(), 'item', 'chromite')
    save(rock_mite(), 'entity', 'belt/rock_mite')
    import os
    out = os.path.join(os.path.dirname(__file__), '..', '..', '..', 'src', 'main', 'resources', 'assets', 'spacebreaker', 'textures',
                       'environment')
    os.makedirs(out, exist_ok=True)
    ceres_disc().save(os.path.join(out, 'ceres_disc.png'))
    vesta_disc().save(os.path.join(out, 'vesta_disc.png'))
