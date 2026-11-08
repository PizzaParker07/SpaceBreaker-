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
    """Dark steel-grey lump peppered with small metallic grey grains (two-tone flecks), so it reads apart from the
    reddish hematite and the purple-black ilmenite in an inventory."""
    r = ['#0c0c0e', '#17181b', '#24262b', '#33363c', '#44484f', '#5a5f67', '#80868f', '#b4bac2']
    from sbtex.core import hexc
    cv = raw_lump([hexc(c) for c in r], 8501, [(5, 4), (9, 7), (6, 10)])
    m = mask_from(parse_mask(RAW_CHUNK))
    fleck = hexc('#9ba3ad')
    shine = hexc('#d2d8df')
    # metallic grains: a lit pixel with a mid-grey tail, kept off the outline
    for x, y in [(3, 6), (7, 5), (11, 5), (4, 9), (8, 9), (12, 8), (10, 11), (6, 12), (13, 10)]:
        inside = all(m[yy][xx] for xx, yy in ((x, y), (x + 1, y), (x, y + 1), (x - 1, y), (x, y - 1)) if 0 <= xx < 16 and 0 <= yy < 16)
        if inside:
            cv[x, y] = shine
            cv[x + 1, y] = fleck
    return cv


# --------------------------------------------------------------------------- Vesta

def vestan_regolith(v=0):
    """Grey-buff howardite dust: brighter than Ceres, with a few dark glassy grains."""
    return dust(P.VESTA_SOIL, 8601 + v * 41)


def diogenitic_regolith(v=0):
    """Ochre-tan dust from the giant south basin: warmer and a little coarser than the grey howardite plains."""
    return dust(P.DIOGENITIC_SOIL, 8651 + v * 43)


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
    clay = P.AMMONIA_CLAY
    salt = P.MITE_SALT
    n = clustered(64, 64, 9101, passes=1)

    def plate(tones, ox, oy):
        # an armour plate: flat top with a pale carbonate crust along its edges, darker overhanging sides
        def f(face, x, y, fw, fh):
            if face == 'top':
                edge = y in (0, fh - 1) or x in (0, fw - 1)
                if edge and n[y + oy][x + ox] > 0.68:
                    return salt[1] if n[y + oy + 7][x + ox] > 0.6 else salt[0]
                return noise_pick(tones, 3 if not edge else 2, n, x + ox, y + oy)
            if face == 'bottom':
                return r[0]
            if y == 0 and n[y + oy + 3][x + ox] > 0.75:
                return salt[0]
            return noise_pick(tones, 2 if y == 0 else 1, n, x + ox + 20, y + oy + 30)
        return f

    paint(cv, 0, 0, 8, 2, 4, plate(clay, 0, 0))
    paint(cv, 0, 6, 9, 3, 4, plate(r, 11, 5))
    paint(cv, 0, 13, 8, 2, 4, plate(clay, 23, 9))
    paint(cv, 0, 19, 6, 1, 3, plate(r, 31, 17))

    def head(face, x, y, fw, fh):
        if face == 'front':
            # two dark sensory pits and a pale mouth ridge, no eyes
            if y == 1 and x in (1, fw - 2):
                return r[0]
            if y == fh - 1:
                return salt[0] if x in (2, 3) else r[1]
            return r[3] if y == 0 else r[2]
        if face == 'top':
            return noise_pick(r, 4 if y < fh - 1 else 3, n, x + 40, y)
        return noise_pick(r, 2, n, x + 40, y + 9)

    paint(cv, 28, 0, 6, 3, 4, head)
    paint(cv, 28, 8, 1, 1, 2, lambda f, x, y, fw, fh: salt[1] if f == 'front' else clay[3])
    paint(cv, 36, 8, 1, 1, 4, lambda f, x, y, fw, fh: (r[5] if f == 'front' else r[3]) if f != 'bottom' else r[1])

    def ridge(face, x, y, fw, fh):
        if face == 'top':
            return salt[1] if (x + y) % 3 else salt[2]
        return salt[0] if face in ('left', 'right') else r[4]

    paint(cv, 48, 0, 2, 1, 4, ridge)
    paint(cv, 28, 14, 4, 1, 1, lambda f, x, y, fw, fh: (clay[3] if x < 2 else clay[2]) if f == 'top' else clay[1])
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

# --------------------------------------------------------------------------- near-Earth asteroids

def bennu_regolith(v=0):
    return dust(P.BENNU_SOIL, 19101 + v * 29)


def bennu_boulder():
    r = P.BENNU_ROCK
    f = blend((clustered(16, 16, 19201, passes=1), 0.55), (white(19201 + 1), 0.3), (fbm(16, 16, 19201 + 2, [(8, 1.0)]), 0.15))
    cv, _ = quant(f, [r[0], r[1], r[2], r[3], r[4], r[5]], [3, 12, 32, 32, 15, 6])
    return cv


def ryugu_regolith(v=0):
    return dust(P.RYUGU_SOIL, 19301 + v * 29)


def ryugu_rock():
    r = P.RYUGU_ROCK
    f = blend((clustered(16, 16, 19401, passes=1), 0.55), (white(19401 + 1), 0.3), (fbm(16, 16, 19401 + 2, [(8, 1.0)]), 0.15))
    cv, _ = quant(f, [r[0], r[1], r[2], r[3], r[4], r[5]], [3, 12, 32, 32, 15, 6])
    return cv


def eros_regolith(v=0):
    return dust(P.EROS_SOIL, 19501 + v * 29)


def eros_chondrite():
    r = P.EROS_ROCK
    f = blend((clustered(16, 16, 19601, passes=1), 0.55), (white(19601 + 1), 0.3), (fbm(16, 16, 19601 + 2, [(8, 1.0)]), 0.15))
    cv, _ = quant(f, [r[0], r[1], r[2], r[3], r[4], r[5]], [3, 12, 32, 32, 15, 6])
    return cv


def itokawa_gravel(v=0):
    return dust(P.ITOKAWA_SOIL, 19701 + v * 29)


def itokawa_rock():
    r = P.ITOKAWA_ROCK
    f = blend((clustered(16, 16, 19801, passes=1), 0.55), (white(19801 + 1), 0.3), (fbm(16, 16, 19801 + 2, [(8, 1.0)]), 0.15))
    cv, _ = quant(f, [r[0], r[1], r[2], r[3], r[4], r[5]], [3, 12, 32, 32, 15, 6])
    return cv



# --------------------------------------------------------------------------- comets

def comet_dust(v=0):
    return dust(P.COMET_DUST, 20101 + v * 29)


def comet_ice():
    """Dirty comet ice: dark, with pale specks of clean ice showing through."""
    r = P.COMET_ICE
    f = blend((clustered(16, 16, 20201, passes=1), 0.55), (white(20202), 0.3), (fbm(16, 16, 20203, [(8, 1.0)]), 0.15))
    cv, _ = quant(f, [r[0], r[1], r[2], r[3], r[4], r[5]], [14, 30, 30, 14, 8, 4])
    return cv


def comet_jet():
    """A jet vent: dark dirty ice round a pale, frosty mouth of fresh ice."""
    r = P.COMET_ICE
    cv = comet_ice()
    for y in range(16):
        for x in range(16):
            d = ((x - 7.5) ** 2 + (y - 7.5) ** 2) ** 0.5
            if d < 2.2:
                cv[x, y] = r[5]
            elif d < 3.6:
                cv[x, y] = r[3] if (x + y) % 2 else r[4]
    return cv


def encke_dust(v=0):
    return dust(P.ENCKE_DUST, 20301 + v * 29)


def encke_ice():
    r = P.ENCKE_ICE
    f = blend((clustered(16, 16, 20401, passes=1), 0.55), (white(20401 + 1), 0.3), (fbm(16, 16, 20401 + 2, [(8, 1.0)]), 0.15))
    cv, _ = quant(f, [r[0], r[1], r[2], r[3], r[4], r[5]], [14, 30, 30, 14, 8, 4])
    return cv


def halley_dust(v=0):
    return dust(P.HALLEY_DUST, 20501 + v * 29)


def halley_ice():
    r = P.HALLEY_ICE
    f = blend((clustered(16, 16, 20601, passes=1), 0.55), (white(20601 + 1), 0.3), (fbm(16, 16, 20601 + 2, [(8, 1.0)]), 0.15))
    cv, _ = quant(f, [r[0], r[1], r[2], r[3], r[4], r[5]], [14, 30, 30, 14, 8, 4])
    return cv


def tempel_dust(v=0):
    return dust(P.TEMPEL_DUST, 20701 + v * 29)


def tempel_ice():
    r = P.TEMPEL_ICE
    f = blend((clustered(16, 16, 20801, passes=1), 0.55), (white(20801 + 1), 0.3), (fbm(16, 16, 20801 + 2, [(8, 1.0)]), 0.15))
    cv, _ = quant(f, [r[0], r[1], r[2], r[3], r[4], r[5]], [12, 28, 30, 16, 9, 5])
    return cv




def gold_foil_cladding():
    """Crinkled gold thermal blanket over a hull panel: soft diagonal creases, bright glints, a darker seam every 8 px."""
    r = P.GOLD_FOIL
    creases = blur_dir(clustered(16, 16, 21101, passes=1), 1, 1, 1)
    f = blend((creases, 0.55), (fbm(16, 16, 21102, [(4, 1.0), (2, 0.5)]), 0.3), (white(21103), 0.15))
    cv, _ = quant(f, [r[0], r[1], r[2], r[3], r[4], r[5]], [6, 22, 34, 22, 12, 4])
    for i in range(16):
        cv[i, 7] = r[1] if cv[i, 7] in (r[4], r[5]) else r[0] if i % 5 == 0 else r[1]
        cv[7, i] = r[1] if i % 3 else r[0]
    return cv


def export(save):
    for name, fn, r in (('encke_dust', encke_dust, P.ENCKE_DUST),):
        base = fn(0)
        save(base, 'block', name, (18, 8, 64, None))
        save(fn(1), 'block', name + '_1', (18, 8, 64, None))
        for side in ('left', 'right'):
            save(footprint(base, prints(r), side), 'block', f'{name}_footprint_{side}', (22, 8, 64, None))
    save(encke_ice(), 'block', 'encke_ice', (60, 14, 10, None))
    for name, fn, r in (('halley_dust', halley_dust, P.HALLEY_DUST),):
        base = fn(0)
        save(base, 'block', name, (18, 8, 64, None))
        save(fn(1), 'block', name + '_1', (18, 8, 64, None))
        for side in ('left', 'right'):
            save(footprint(base, prints(r), side), 'block', f'{name}_footprint_{side}', (22, 8, 64, None))
    save(halley_ice(), 'block', 'halley_ice', (60, 14, 10, None))
    save(gold_foil_cladding(), 'block', 'gold_foil_cladding', (70, 30, 4, None))
    for name, fn, r, ice in (('tempel_dust', tempel_dust, P.TEMPEL_DUST, tempel_ice),):
        base = fn(0)
        save(base, 'block', name, (18, 8, 64, None))
        save(fn(1), 'block', name + '_1', (18, 8, 64, None))
        for side in ('left', 'right'):
            save(footprint(base, prints(r), side), 'block', f'{name}_footprint_{side}', (22, 8, 64, None))
        save(ice(), 'block', name.replace('_dust', '_ice'), (60, 14, 10, None))
    for name, fn, r in (('comet_dust', comet_dust, P.COMET_DUST),):
        base = fn(0)
        save(base, 'block', name, (18, 8, 64, None))
        save(fn(1), 'block', name + '_1', (18, 8, 64, None))
        for side in ('left', 'right'):
            save(footprint(base, prints(r), side), 'block', f'{name}_footprint_{side}', (22, 8, 64, None))
    save(comet_ice(), 'block', 'comet_ice', (60, 14, 10, None))
    save(comet_jet(), 'block', 'comet_jet', (30, 10, 30, None))
    for name, fn, r in (('bennu_regolith', bennu_regolith, P.BENNU_SOIL),):
        base = fn(0)
        save(base, 'block', name, (18, 8, 64, None))
        save(fn(1), 'block', name + '_1', (18, 8, 64, None))
        for side in ('left', 'right'):
            save(footprint(base, prints(r), side), 'block', f'{name}_footprint_{side}', (22, 8, 64, None))
    save(bennu_boulder(), 'block', 'bennu_boulder', (40, 12, 20, None))
    for name, fn, r in (('ryugu_regolith', ryugu_regolith, P.RYUGU_SOIL),):
        base = fn(0)
        save(base, 'block', name, (18, 8, 64, None))
        save(fn(1), 'block', name + '_1', (18, 8, 64, None))
        for side in ('left', 'right'):
            save(footprint(base, prints(r), side), 'block', f'{name}_footprint_{side}', (22, 8, 64, None))
    save(ryugu_rock(), 'block', 'ryugu_rock', (40, 12, 20, None))
    for name, fn, r in (('eros_regolith', eros_regolith, P.EROS_SOIL),):
        base = fn(0)
        save(base, 'block', name, (18, 8, 64, None))
        save(fn(1), 'block', name + '_1', (18, 8, 64, None))
        for side in ('left', 'right'):
            save(footprint(base, prints(r), side), 'block', f'{name}_footprint_{side}', (22, 8, 64, None))
    save(eros_chondrite(), 'block', 'eros_chondrite', (40, 12, 20, None))
    for name, fn, r in (('itokawa_gravel', itokawa_gravel, P.ITOKAWA_SOIL),):
        base = fn(0)
        save(base, 'block', name, (18, 8, 64, None))
        save(fn(1), 'block', name + '_1', (18, 8, 64, None))
        for side in ('left', 'right'):
            save(footprint(base, prints(r), side), 'block', f'{name}_footprint_{side}', (22, 8, 64, None))
    save(itokawa_rock(), 'block', 'itokawa_rock', (40, 12, 20, None))
    dust_spec = (18, 8, 64, None)
    for name, fn, r in (('ceres_regolith', ceres_regolith, P.CERES_SOIL), ('ammoniated_clay', ammoniated_clay, P.AMMONIA_CLAY),
                        ('carbonate_crust', carbonate_crust, P.CARBONATE), ('vestan_regolith', vestan_regolith, P.VESTA_SOIL),
                        ('diogenitic_regolith', diogenitic_regolith, P.DIOGENITIC_SOIL)):
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
