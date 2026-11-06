"""Inner planets seen from orbit: equirectangular maps (512 x 256, longitude 0..360 east, north up) for the orbit
skies of Venus, Mercury and Mars, plus small pixel discs for seeing them from afar. Drawn like the vanilla Moon:
a few flat tones with crisp regions, no smooth gradients."""
import math
import os
import random

W, H = 512, 256


def _hex(c):
    return tuple(int(c[i:i + 2], 16) for i in (1, 3, 5))


def _value_noise(seed, cells):
    rng = random.Random(seed)
    grids = []
    for cx, cy in cells:
        grids.append((cx, cy, [[rng.random() for _ in range(cx + 1)] for _ in range(cy + 2)]))

    def noise(u, v):
        """u wraps (0..1), v clamps (0..1)."""
        total = 0
        weight = 0
        for k, (cx, cy, g) in enumerate(grids):
            fx = (u % 1.0) * cx
            fy = max(0.0, min(0.9999, v)) * cy
            ix, iy = int(fx), int(fy)
            tx, ty = fx - ix, fy - iy
            x1 = (ix + 1) % cx
            a = g[iy][ix] * (1 - tx) + g[iy][x1] * tx
            b = g[iy + 1][ix] * (1 - tx) + g[iy + 1][x1] * tx
            amp = 0.55 ** k
            total += (a * (1 - ty) + b * ty) * amp
            weight += amp
        return total / weight
    return noise


def _ang(lon1, lat1, lon2, lat2):
    """Great-circle distance in degrees."""
    p1, p2 = math.radians(lat1), math.radians(lat2)
    dl = math.radians(lon1 - lon2)
    c = math.sin(p1) * math.sin(p2) + math.cos(p1) * math.cos(p2) * math.cos(dl)
    return math.degrees(math.acos(max(-1.0, min(1.0, c))))


def _image(fn):
    from PIL import Image
    img = Image.new('RGBA', (W, H))
    px = img.load()
    for y in range(H):
        lat = 90 - (y + 0.5) * 180 / H
        for x in range(W):
            lon = (x + 0.5) * 360 / W
            px[x, y] = fn(lon, lat, x, y) + (255,)
    return img


# --------------------------------------------------------------------------- Venus

VENUS = [_hex(c) for c in ('#b89c62', '#cbb27a', '#d9c48f', '#e4d3a6', '#eee3bf')]


def venus_map():
    """The cloud deck: pale butterscotch with darker chevron (sideways Y) bands opening from the equator, brighter
    polar collars. The tops race round the planet in four days, so the bands are long and stretched."""
    n = _value_noise(9101, [(8, 4), (24, 10), (64, 24)])

    def fn(lon, lat, x, y):
        u = lon / 360
        # chevron: band latitude grows with distance from the leading meridian of each of two Ys
        phase = (u * 2) % 1.0
        chevron = abs(lat) - 55 * phase
        bands = math.cos(math.radians(chevron * 6 + 40 * n(u, y / H)))
        val = 0.55 + 0.22 * bands + (n(u * 2, y / H) - 0.5) * 0.5
        val += 0.25 * max(0.0, min(1.0, (abs(lat) - 50) / 12))  # bright polar collars
        val -= 0.08 * max(0.0, 1 - abs(lat) / 12)
        return VENUS[max(0, min(4, int(val * 5)))]
    return _image(fn)


def venus_disc(size=32):
    """Venus as a tiny, brilliant, almost featureless disc."""
    return _disc(size, lambda u, v, n: VENUS[4] if n > 0.42 else VENUS[3], 9111)


# --------------------------------------------------------------------------- Mercury

MERCURY = [_hex(c) for c in ('#3f3c39', '#57534f', '#6e6964', '#86807a', '#9d9791', '#b8b3ad')]


def mercury_map():
    """Grey, crowded with craters: dark-floored basins, bright rims, a few fresh craters with white ray systems, the
    smooth lighter plains of Caloris, and dark low-reflectance patches."""
    n = _value_noise(9201, [(10, 5), (32, 14)])
    rng = random.Random(9202)
    craters = []
    for _ in range(420):
        r = 0.6 + 7.5 * rng.random() ** 2.4
        craters.append((rng.uniform(0, 360), math.degrees(math.asin(rng.uniform(-1, 1))), r))
    fresh = [(rng.uniform(0, 360), rng.uniform(-60, 60), rng.uniform(1.2, 2.2)) for _ in range(5)]
    caloris = (162.0, 30.0, 26.0)

    def fn(lon, lat, x, y):
        u = lon / 360
        val = 0.55 + (n(u, y / H) - 0.5) * 0.7
        # low-reflectance material in broad patches
        if n(u + 0.37, y / H) < 0.36:
            val -= 0.18
        d = _ang(lon, lat, caloris[0], caloris[1])
        if d < caloris[2]:
            val = 0.62 + (n(u * 3, y / H) - 0.5) * 0.2  # smooth interior plains
            if d > caloris[2] - 3.5:
                val = 0.82 if (lon - caloris[0]) < 0 else 0.3  # mountain ring, lit on one side
        for clon, clat, r in craters:
            if abs(clat - lat) > r or min(abs(clon - lon), 360 - abs(clon - lon)) * math.cos(math.radians(lat)) > r:
                continue
            dd = _ang(lon, lat, clon, clat)
            if dd < r:
                t = dd / r
                if t > 0.78:
                    val = 0.85 if (lon - clon) * 1 + (clat - lat) * 0.5 < 0 else 0.25  # rim: lit west wall
                else:
                    val = min(val, 0.38 + 0.1 * t)
        for clon, clat, r in fresh:
            dd = _ang(lon, lat, clon, clat)
            if dd < r:
                val = 0.98
            elif dd < r * 9:
                # rays: thin bright spokes fading outward
                ang = math.atan2(lat - clat, (lon - clon) * math.cos(math.radians(lat)))
                spoke = math.cos(ang * 7 + clon) > 0.86
                if spoke and rng.random() < 1.2 - dd / (r * 9):
                    val = max(val, 0.8)
        return MERCURY[max(0, min(5, int(val * 6)))]
    return _image(fn)


def mercury_disc(size=32):
    return _disc(size, lambda u, v, n: MERCURY[4] if n > 0.55 else (MERCURY[3] if n > 0.35 else MERCURY[2]), 9211)


# --------------------------------------------------------------------------- Mars

MARS = [_hex(c) for c in ('#603a28', '#7c4a32', '#985a3a', '#b26840', '#c77c4e', '#da9664')]
MARS_CAP = [_hex('#cec6ba'), _hex('#ece8de')]
MARS_CANYON = _hex('#46281d')


def mars_map():
    """Real geography: Tharsis with Olympus Mons and the three shields, Valles Marineris with Noctis Labyrinthus,
    the dark albedo features (Syrtis Major, Acidalia, Mare Erythraeum, Sinus Meridiani, Utopia, Cimmerium), bright
    Hellas and Argyre, and both polar caps."""
    n = _value_noise(9301, [(10, 5), (30, 12), (80, 30)])
    darks = [(70, 8, 16, 0.55), (330, 45, 20, 0.5), (320, -25, 22, 0.45), (0, -4, 12, 0.5), (115, 45, 18, 0.35),
             (150, -25, 22, 0.4), (200, -25, 16, 0.3), (95, -20, 12, 0.35)]
    brights = [(70, -42, 13, 0.4), (318, -50, 9, 0.3), (250, 5, 28, 0.15), (180, 15, 30, 0.1)]
    volcanoes = [(226, 18, 8.5), (256, 12, 4.5), (247, 0, 4.5), (239, -9, 4.5)]

    def fn(lon, lat, x, y):
        u = lon / 360
        val = 0.6 + (n(u, y / H) - 0.5) * 0.45
        val -= 0.08 * max(0.0, min(1.0, (-lat - 0) / 20))
        for dl, dla, r, k in darks:
            d = _ang(lon, lat, dl, dla) / r + (n(u + 0.2, y / H) - 0.5) * 1.6 + 0.3 * math.sin(math.radians(lon * 9 + lat * 7))
            if d < 1:
                val -= k * min(1.0, (1 - d) * 3)
        for bl, bla, r, k in brights:
            d = _ang(lon, lat, bl, bla) / r + (n(u + 0.5, y / H) - 0.5) * 0.6
            if d < 1:
                val += k * min(1.0, (1 - d) * 3)
        col = MARS[max(0, min(5, int(val * 6)))]
        for vl, vla, r in volcanoes:
            d = _ang(lon, lat, vl, vla)
            if d < r:
                t = d / r
                col = MARS[1] if t < 0.25 else (MARS[5] if (lon - vl) + (vla - lat) < 0 else MARS[3])
        # Valles Marineris: main trough plus a parallel branch, Noctis maze at the west end, chaos to the east
        if 265 < lon < 318:
            centre = -8 - 0.06 * (lon - 265)
            w = 0.8 + 1.1 * math.exp(-((lon - 290) / 9) ** 2) + (n(u * 4, y / H) - 0.5) * 0.6
            if abs(lat - centre) < w:
                col = MARS_CANYON
            if 275 < lon < 300 and abs(lat - (centre + 3.5)) < 0.6:
                col = MARS_CANYON
        if 252 < lon < 266 and -12 < lat < -2 and (int(lon * 1.7) % 3 == 0 or int(lat * 1.9) % 3 == 0) and n(u * 6, y / H) > 0.45:
            col = MARS_CANYON
        if 318 <= lon < 330 and -15 < lat < -2 and n(u * 8, y / H) > 0.55:
            col = MARS[0]
        edge = 76 + (n(u * 3, y / H) - 0.5) * 8
        if lat > edge:
            col = MARS_CAP[1] if lat > edge + 4 else MARS_CAP[0]
        if lat < -edge - 4:
            col = MARS_CAP[1] if lat < -edge - 8 else MARS_CAP[0]
        return col
    return _image(fn)


def _disc(size, colour, seed):
    from PIL import Image
    n = _value_noise(seed, [(4, 4), (8, 8)])
    img = Image.new('RGBA', (size, size), (0, 0, 0, 0))
    px = img.load()
    c = (size - 1) / 2
    for y in range(size):
        for x in range(size):
            u, v = (x - c) / c, (y - c) / c
            if u * u + v * v <= 1:
                px[x, y] = colour(u, v, n(x / size, y / size)) + (255,)
    return img


def export(save):
    out = os.path.join(os.path.dirname(__file__), '..', '..', '..', 'src', 'main', 'resources', 'assets', 'spacebreaker',
                       'textures', 'environment')
    venus_map().save(os.path.join(out, 'orbit_venus.png'))
    mercury_map().save(os.path.join(out, 'orbit_mercury.png'))
    mars_map().save(os.path.join(out, 'orbit_mars.png'))
    venus_disc().save(os.path.join(out, 'venus_disc.png'))
    mercury_disc().save(os.path.join(out, 'mercury_disc.png'))
