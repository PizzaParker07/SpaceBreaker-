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


def mars_disc(size=512):
    """Mars as a full, evenly lit disc for the sky of its moons, at high resolution: rusty plains and darker
    southern highlands, Syrtis Major, Acidalia and Mare Erythraeum, the Tharsis volcanoes with Olympus Mons, the scar
    of Valles Marineris, bright Hellas, and the white polar caps. Phase shading is applied in game."""
    import math
    from PIL import Image
    rng = random.Random(4402)
    # value noise for natural mottling, sampled bilinearly at several scales
    grids = []
    for cell in (64, 24, 9):
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
            amp = 1 / (k + 1)
            v += (a * (1 - ty) + b * ty) * amp
            w += amp
        return v / w

    bright = (217, 160, 102)   # bright dust
    dust = (193, 104, 60)      # dust
    dark = (110, 72, 52)       # dark albedo features
    cap = (232, 228, 218)
    # (u, v, radius, strength) of dark regions in disc coordinates (-1..1, v down)
    darks = [(0.42, 0.05, 0.22, 1.0), (0.55, -0.32, 0.18, 0.8), (-0.05, 0.22, 0.35, 0.7), (0.15, 0.32, 0.25, 0.6), (-0.55, 0.15, 0.2, 0.5)]
    volcanoes = [(-0.62, -0.12, 0.085), (-0.4, -0.05, 0.035), (-0.36, 0.06, 0.035), (-0.32, 0.17, 0.035)]
    img = Image.new('RGBA', (size, size), (0, 0, 0, 0))
    px = img.load()
    c = (size - 1) / 2
    for y in range(size):
        for x in range(size):
            u = (x - c) / c
            v = (y - c) / c
            d = math.hypot(u, v)
            if d > 1:
                continue
            n = noise(x, y)
            col = _lerp(dust, bright, max(0.0, min(1.0, (n - 0.35) * 2.2)))
            # southern highlands a shade darker and more cratered-looking
            if v > 0:
                col = _lerp(col, _lerp(dust, dark, 0.35), min(0.6, v * 0.9) * (0.6 + 0.4 * n))
            for du, dv, r, k in darks:
                dd = math.hypot(u - du, v - dv) / r
                if dd < 1.3:
                    t = max(0.0, 1 - dd) ** 0.8 * k * (0.55 + 0.6 * n)
                    col = _lerp(col, dark, min(1.0, t))
            # Hellas: a bright, smooth basin in the south
            hd = math.hypot(u - 0.3, v - 0.55) / 0.13
            if hd < 1:
                col = _lerp(col, bright, (1 - hd) * 0.8)
            # Tharsis volcanoes: lit summits with a dark caldera dot, Olympus Mons biggest with its cliff ring
            for vu, vv, r in volcanoes:
                vd = math.hypot(u - vu, v - vv) / r
                if vd < 1:
                    # a lit dome: brighter toward the upper-left flank, a soft shadow on the lower-right
                    lu = (u - vu) / r
                    lv = (v - vv) / r
                    shade = -(lu + lv) * 0.5 * (1 - vd)
                    col = _lerp(col, (214, 150, 104) if shade > 0 else (120, 70, 46), min(1.0, abs(shade) * 2.2 + (1 - vd) * 0.25))
                    if vd < 0.12 and r > 0.05:
                        col = _lerp(col, dark, 0.6)
            # Valles Marineris: a long dark scar east of Tharsis
            vy = 0.1 + 0.035 * math.sin(u * 7)
            w = (0.006 + 0.012 * (1 - abs(u - 0.04) / 0.34)) * (0.7 + 0.6 * n)
            if -0.3 < u < 0.38 and abs(v - vy) < w:
                col = _lerp(col, (88, 50, 34), 0.55 + 0.25 * (1 - abs(v - vy) / w))
            # polar caps, ragged edges
            lat = -v
            edge = 0.82 + 0.05 * (n - 0.5)
            if lat > edge:
                col = _lerp(col, cap, min(1.0, (lat - edge) * 14))
            if -lat > edge + 0.04:
                col = _lerp(col, cap, min(1.0, (-lat - edge - 0.04) * 12))
            # soft, dusty limb: a little brighter and redder near the edge
            if d > 0.9:
                col = _lerp(col, (230, 160, 120), (d - 0.9) * 2.5)
            a = 255 if d < 0.995 else int(255 * (1 - (d - 0.995) / 0.005))
            px[x, y] = (col[0], col[1], col[2], max(0, a))
    return img


def mars_halo(size=128):
    """Thin, pale atmosphere ring drawn around the disc (additive)."""
    import math
    from PIL import Image
    img = Image.new('RGBA', (size, size), (0, 0, 0, 0))
    px = img.load()
    c = (size - 1) / 2
    for y in range(size):
        for x in range(size):
            d = math.hypot(x - c, y - c) / c
            # disc edge sits at 1/1.08 of the halo radius
            t = (d - 1 / 1.08) / (1 - 1 / 1.08)
            if -0.6 < t < 1:
                a = math.exp(-((t - 0.05) / 0.35) ** 2) if t > -0.2 else 0
                px[x, y] = (int(150 + 80 * a), int(160 + 60 * a), int(190 + 40 * a), int(200 * a))
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
    mars_halo().save(os.path.join(out, 'mars_halo.png'))
