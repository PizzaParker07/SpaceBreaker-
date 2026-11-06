"""Miranda: regolith, bedrock ice, the bright and dark corona ices, the carbon deposit, the cliff clinger, and Uranus'
disc and rings for the sky. All drawn by code, vanilla-close."""
import math

from sbtex.core import Canvas, clustered, fbm, hexc
from sbtex import palettes as P
from modules.moon import quant, white, blend, blur_dir, footprint, overlay
from modules.mars import dust, prints
from modules.moon_creatures import paint, noise_pick


def mirandan_regolith(v=0):
    return dust(P.MIRANDA_SOIL, 15101 + v * 19)


def _rock(r, seed):
    f = blend((clustered(16, 16, seed, passes=1), 0.55), (white(seed + 1), 0.3), (fbm(16, 16, seed + 2, [(8, 1.0)]), 0.15))
    cv, _ = quant(f, [r[0], r[1], r[2], r[3], r[4], r[5]], [3, 12, 32, 32, 15, 6])
    return cv


def mirandan_ice():
    return _rock(P.MIRANDA_ICE, 15201)


def bright_ridge_ice():
    """Bright ridge ice: clean, with faint streaks along the ridge."""
    r = P.BRIGHT_RIDGE
    f = blend((blur_dir(clustered(16, 16, 15301, passes=1), 3, 1, 0), 0.6), (white(15302), 0.4))
    cv, _ = quant(f, [r[1], r[2], r[3], r[4], r[5]], [8, 24, 36, 24, 8])
    return cv


def dark_band_ice():
    """Dark band ice: carbon-stained, streaked the same way."""
    r = P.DARK_BAND
    f = blend((blur_dir(clustered(16, 16, 15401, passes=1), 3, 1, 0), 0.6), (white(15402), 0.4))
    cv, _ = quant(f, [r[1], r[2], r[3], r[4], r[5]], [8, 24, 36, 24, 8])
    return cv


CAR = [
    (2, 3, [(0, 0, 1), (1, 0, 2), (0, 1, 0), (1, 1, 0)]),
    (10, 2, [(0, 0, 2), (1, 0, 1), (1, 1, 0)]),
    (5, 9, [(0, 0, 1), (1, 0, 3), (2, 0, 1), (1, 1, 0), (2, 1, 0)]),
    (12, 11, [(0, 0, 3), (0, 1, 1), (1, 1, 0)]),
]


def carbon_overlay():
    return overlay(CAR, [hexc('#0e0e0f'), hexc('#1a1a1c'), hexc('#26262a'), hexc('#3a3a40')])


def cliff_clinger():
    """64x32 sheet for CliffClingerModel: dark grey central disc with a pale ring, five arms banded pale at the tips."""
    cv = Canvas(64, 32)
    r = P.CLINGER
    n = clustered(64, 64, 15501, passes=1)

    def disc(face, x, y, fw, fh):
        if face == 'top':
            d = math.hypot(x - (fw - 1) / 2, y - (fh - 1) / 2)
            if 1.3 < d < 2.3:
                return r[5]
            return noise_pick(r, 2, n, x, y)
        return r[1] if face == 'bottom' else noise_pick(r, 2, n, x + 20, y)

    paint(cv, 0, 0, 6, 2, 6, disc)

    def arm(face, x, y, fw, fh):
        if face == 'top':
            return r[4] if y < 2 else (r[3] if y % 2 else noise_pick(r, 2, n, x + 40, y))
        return r[1]

    paint(cv, 0, 8, 2, 1, 6, arm)
    return cv


def uranus_disc(size=256):
    """Uranus: an almost featureless pale cyan disc, a slightly brighter polar cap and a few very faint bands."""
    from PIL import Image
    base = hexc('#a7d8de')
    band = hexc('#9ccfd6')
    cap = hexc('#b9e2e6')
    img = Image.new('RGBA', (size, size), (0, 0, 0, 0))
    px = img.load()
    c = (size - 1) / 2
    for y in range(size):
        for x in range(size):
            u = (x - c) / c
            v = (y - c) / c
            if math.hypot(u, v) > 1:
                continue
            col = base
            if v < -0.62:
                col = cap
            elif int((v + 1) * 7) % 3 == 0:
                col = band
            px[x, y] = (col[0], col[1], col[2], 255)
    return img


def uranus_rings(width=256):
    """Uranus' rings: a few thin, dark, narrow rings with gaps between (mostly empty)."""
    from PIL import Image
    img = Image.new('RGBA', (width, 4), (0, 0, 0, 0))
    px = img.load()
    rings = [0.12, 0.2, 0.27, 0.33, 0.44, 0.52, 0.9]
    for x in range(width):
        t = (x + 0.5) / width
        a = 0
        for ring in rings:
            if abs(t - ring) < (0.012 if ring == 0.9 else 0.006):
                a = 150 if ring == 0.9 else 90
        for y in range(4):
            px[x, y] = (70, 72, 74, a)
    return img


def export(save):
    dust_spec = (18, 8, 64, None)
    base = mirandan_regolith(0)
    save(base, 'block', 'mirandan_regolith', dust_spec)
    save(mirandan_regolith(1), 'block', 'mirandan_regolith_1', dust_spec)
    for side in ('left', 'right'):
        save(footprint(base, prints(P.MIRANDA_SOIL), side), 'block', f'mirandan_regolith_footprint_{side}', (22, 8, 64, None))
    save(mirandan_ice(), 'block', 'mirandan_ice', (40, 12, 20, None))
    save(bright_ridge_ice(), 'block', 'bright_ridge_ice', (60, 14, 10, None))
    save(dark_band_ice(), 'block', 'dark_band_ice', (40, 12, 20, None))
    save(carbon_overlay(), 'block', 'mirandan_carbon_ore_overlay', (60, 20, 0, None), wrap=False)
    save(cliff_clinger(), 'entity', 'uranus/cliff_clinger')
    import os
    out = os.path.join(os.path.dirname(__file__), '..', '..', '..', 'src', 'main', 'resources', 'assets', 'spacebreaker', 'textures',
                       'environment')
    os.makedirs(out, exist_ok=True)
    uranus_disc().save(os.path.join(out, 'uranus_disc.png'))
    uranus_rings().save(os.path.join(out, 'uranus_rings.png'))
