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

def _lerp(a, b, t):
    return tuple(int(round(a[i] + (b[i] - a[i]) * t)) for i in range(3))


def mars_disc(size=256):
    """Mars as a full, evenly lit disc for the sky of its moons, drawn like the vanilla Moon: crisp albedo regions in a
    few flat tones rather than smooth gradients. Shown: the bright Tharsis rise with its three shield volcanoes in a
    row and Olympus Mons apart to the north-west (each with a caldera), Noctis Labyrinthus at the western end of Valles
    Marineris, the main east-west canyon with parallel chasmata, the wide central troughs and the chaos at its eastern
    mouth, the dark albedo regions, bright Hellas and the polar caps. Phase shading is applied in game."""
    import math
    from PIL import Image
    rng = random.Random(4402)
    grids = []
    for cell in (32, 12, 5):
        g = [[rng.random() for _ in range(size // cell + 2)] for _ in range(size // cell + 2)]
        grids.append((cell, g))

    def noise(x, y):
        v = 0
        w = 0
        for k, (cell, g) in enumerate(grids):
            fx, fy = x / cell, y / cell
            ix, iy = int(fx), int(fy)
            tx, ty = fx - ix, fy - iy
            a = g[iy][ix] * (1 - tx) + g[iy][ix + 1] * tx
            b = g[iy + 1][ix] * (1 - tx) + g[iy + 1][ix + 1] * tx
            amp = 0.55 ** k
            v += (a * (1 - ty) + b * ty) * amp
            w += amp
        return v / w

    # six flat albedo tones, dark to bright, plus canyon floor, cap and caldera shades
    tones = [(96, 58, 40), (124, 74, 50), (152, 90, 58), (178, 104, 64), (199, 124, 78), (218, 150, 100)]
    canyon = (70, 40, 29)
    wall = (110, 64, 44)
    caps = [(206, 198, 186), (236, 232, 222)]
    # dark albedo regions (u, v, ru, rv, depth): Acidalia, Mare Erythraeum, Margaritifer, Syrtis-ward, Sirenum
    darks = [(0.42, -0.42, 0.22, 0.13, 0.55), (0.25, 0.42, 0.42, 0.13, 0.5), (0.52, 0.18, 0.12, 0.2, 0.45),
             (0.86, 0.02, 0.14, 0.3, 0.55), (-0.62, 0.48, 0.3, 0.1, 0.35)]
    shields = [(-0.50, 0.19, 0.06), (-0.44, 0.04, 0.065), (-0.37, -0.11, 0.06)]
    olympus = (-0.72, -0.22, 0.11)

    def seg_dist(u, v, x0, y0, x1, y1):
        dx, dy = x1 - x0, y1 - y0
        t = max(0.0, min(1.0, ((u - x0) * dx + (v - y0) * dy) / (dx * dx + dy * dy)))
        return math.hypot(u - x0 - t * dx, v - y0 - t * dy)

    # Valles Marineris: (x0, y0, x1, y1, half width) segments, mostly east-west, slightly descending to the east
    chasma = [(-0.20, 0.140, 0.02, 0.150, 0.016),   # Ius / Tithonium
              (-0.18, 0.105, -0.02, 0.110, 0.012),  # parallel northern chasma
              (-0.04, 0.115, 0.12, 0.125, 0.030),   # Melas / Candor: the wide central troughs
              (-0.02, 0.085, 0.10, 0.090, 0.016),   # Ophir / Candor north
              (0.10, 0.130, 0.34, 0.165, 0.020),    # Coprates
              (0.10, 0.180, 0.28, 0.205, 0.008),    # a thinner southern side canyon
              (0.32, 0.165, 0.44, 0.150, 0.026)]    # Eos / Capri opening eastward
    # Noctis Labyrinthus: a short criss-cross net at the western end
    noctis = []
    nr = random.Random(77)
    for _ in range(9):
        cx, cy = -0.27 + nr.uniform(-0.06, 0.06), 0.13 + nr.uniform(-0.05, 0.05)
        ang = nr.choice((0.0, 1.05, 2.1)) + nr.uniform(-0.2, 0.2)
        ln = nr.uniform(0.02, 0.035)
        noctis.append((cx - ln * math.cos(ang), cy - ln * math.sin(ang), cx + ln * math.cos(ang), cy + ln * math.sin(ang), 0.008))

    img = Image.new('RGBA', (size, size), (0, 0, 0, 0))
    px = img.load()
    c = (size - 1) / 2
    jit = random.Random(4403)
    for y in range(size):
        for x in range(size):
            u = (x - c) / c
            v = (y - c) / c
            d = math.hypot(u, v)
            if d > 1:
                continue
            n = noise(x, y)
            # albedo, 0 dark .. 1 bright
            al = 0.62 + (n - 0.5) * 0.5
            if v > 0.05:  # southern highlands: darker, more mottled
                al -= min(0.18, (v - 0.05) * 0.35) * (0.7 + 0.6 * n)
            for du, dv, ru, rv, k in darks:
                dd = math.hypot((u - du) / ru, (v - dv) / rv) + (n - 0.5) * 1.3 + 0.35 * math.sin(u * 23 + v * 17)
                if dd < 1:
                    al -= k * min(1.0, (1 - dd) * 2.5)
            # Tharsis rise and Hellas: bright and dusty
            th = math.hypot((u + 0.48) / 0.32, (v - 0.02) / 0.4) + (n - 0.5) * 0.4
            if th < 1:
                al += 0.16 * min(1.0, (1 - th) * 3)
            hd = math.hypot((u - 0.55) / 0.16, (v - 0.62) / 0.1) + (n - 0.5) * 0.4
            if hd < 1:
                al += 0.3 * min(1.0, (1 - hd) * 3)
            al += (jit.random() - 0.5) * 0.05
            col = tones[max(0, min(5, int(al * 6)))]
            # volcanoes: a dome lit from the upper left, a hard shadow side, a dark caldera
            for vu, vv, r in shields + [olympus]:
                lu, lv = (u - vu) / r, (v - vv) / r
                vd = math.hypot(lu, lv)
                if vd < 1:
                    if vd < (0.3 if r > 0.1 else 0.22):
                        col = tones[1] if vd > (0.2 if r > 0.1 else 0.12) else tones[0]
                    elif lu + lv < -0.25:
                        col = tones[5]
                    elif lu + lv > 0.45:
                        col = tones[2]
                    else:
                        col = tones[4]
                    if r > 0.1 and vd > 0.88:  # Olympus: its basal escarpment
                        col = tones[1] if lu + lv > 0 else tones[5]
            # Valles Marineris and Noctis: dark floor with a brown wall band, edges roughened
            rough = (n - 0.5) * 0.012
            best = 9.0
            for x0, y0, x1, y1, w in chasma + noctis:
                best = min(best, seg_dist(u, v, x0, y0, x1, y1) / max(0.004, w * 1.35 + rough))
            if best < 1:
                col = canyon if best < 0.55 else wall
            # chaos terrain at the eastern mouth: blotchy dark knobs
            ch = math.hypot((u - 0.47) / 0.07, (v - 0.13) / 0.07)
            if ch < 1 and n > 0.52:
                col = wall if n < 0.6 else canyon
            # polar caps with ragged edges
            lat = -v
            edge = 0.84 + (n - 0.5) * 0.16 + 0.012 * math.sin(u * 31 + 1.3)
            if lat > edge:
                col = caps[1] if lat > edge + 0.04 else caps[0]
            if -lat > edge + 0.05:
                col = caps[1] if -lat > edge + 0.09 else caps[0]
            px[x, y] = (col[0], col[1], col[2], 255)
    return img


def export(save):
    dust_spec = (18, 8, 64, None)
    for name, fn, r in (('phobos_regolith', phobos_regolith, PHOBOS_SOIL), ('deimos_regolith', deimos_regolith, DEIMOS_SOIL)):
        base = fn(0)
        save(base, 'block', name, dust_spec)
        save(fn(1), 'block', name + '_1', dust_spec)
        for side in ('left', 'right'):
            save(footprint(base, prints(r), side), 'block', f'{name}_footprint_{side}', (22, 8, 64, None))
    save(chondrite(), 'block', 'carbonaceous_chondrite', (30, 10, 30, None))
    import os
    out = os.path.join(os.path.dirname(__file__), '..', '..', '..', 'src', 'main', 'resources', 'assets', 'spacebreaker', 'textures', 'environment')
    mars_disc().save(os.path.join(out, 'mars_disc.png'))
    halo = os.path.join(out, 'mars_halo.png')
    if os.path.exists(halo):
        os.remove(halo)
