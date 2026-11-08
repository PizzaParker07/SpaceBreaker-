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


# --------------------------------------------------------------------------- Ariel

def ariel_regolith(v=0):
    return dust(P.ARIEL_SOIL, 15601 + v * 19)


def ariel_ice():
    return _rock(P.ARIEL_ICE, 15701)


def graben_floor_ice():
    """Smooth floor of the rift valleys, flooded by icy lava: calm, faint flow lines along the valley."""
    r = P.GRABEN_FLOOR
    f = blend((blur_dir(clustered(16, 16, 15801, passes=2), 4, 1, 0), 0.7), (white(15802), 0.3))
    cv, _ = quant(f, [r[1], r[2], r[3], r[4]], [10, 40, 38, 12])
    return cv


# Frost moth: icy blue-white wings with frost veins, a scalloped trailing edge, an eye spot on each wing and a pale rim;
# a soft grey fuzzy body and feathery antennae.
MOTH_ICE = [hexc(h) for h in ('#4f6070', '#6f8496', '#93a8bb', '#b7cad8', '#d5e3ed', '#ecf4f9', '#fafdff')]
MOTH_FUR = [hexc(h) for h in ('#5c5e63', '#787a7f', '#95979b', '#b2b3b6', '#cdcdcf', '#e3e3e4')]
MOTH_SPOT = [hexc(h) for h in ('#22304a', '#3d5a7c', '#9fd8f0')]


def _moth_wing(cv, u, v, w, d, n, spot, seed):
    """A flat wing plane, drawn on its top face (u+d, v) only: the bottom face stays transparent, so the two faces of the
    zero-thickness plane never fight (the mob renders without culling, so the top face shows from below too). x = 0 is
    the body, y = 0 the leading edge."""
    shape = {}
    for y in range(d):
        for x in range(w):
            # the trailing edge sweeps forward toward the tip; the tip and the root's back corner are rounded
            limit = (d - 1) - (x / max(1, w - 1)) ** 2.2 * (d * 0.45)
            if y > limit:
                continue
            if x == w - 1 and (y == 0 or y >= limit - 1):
                continue
            shape[x, y] = True
    sx, sy, sr = spot
    for (x, y) in shape:
        edge = any((x + dx, y + dy) not in shape for dx, dy in ((1, 0), (-1, 0), (0, 1), (0, -1)) if x + dx >= 0)
        trailing = (x, y + 1) not in shape
        if trailing and (x + seed) % 3 == 0 and 0 < x < w - 1:
            continue  # scalloped trailing edge
        c = MOTH_ICE[noise_pick(list(range(7)), 4, n, x + u, y + v)]
        # frost veins: lines fanning out from the root
        ang = math.atan2(y - d * 0.35, x + 0.5)
        if x > 0 and min(abs(ang - a) for a in (-0.6, -0.15, 0.3, 0.75)) < 0.09:
            c = MOTH_ICE[2]
        dist = math.hypot(x - sx, y - sy)
        if sr and dist < sr:
            c = MOTH_SPOT[2] if dist < sr * 0.4 else (MOTH_SPOT[0] if dist < sr * 0.75 else MOTH_SPOT[1])
        if y == 0 or (x == w - 1 or (x + 1, y) not in shape) and not trailing:
            c = MOTH_ICE[6]
        elif trailing:
            c = MOTH_ICE[3]
        if x == 0:
            c = MOTH_ICE[1]
        cv[u + d + x, v + y] = c


def frost_moth():
    """64x32 sheet for FrostMothModel (box layout in moon_creatures.faces)."""
    cv = Canvas(64, 32)
    n = clustered(64, 64, 16001, passes=1)

    def fur(face, x, y, fw, fh):
        return MOTH_FUR[noise_pick(list(range(6)), 3 if face != 'bottom' else 2, n, x * 3, y * 5, spread=2)]

    def head(face, x, y, fw, fh):
        if face == 'front' and y == 0:
            return hexc('#141820')
        return fur(face, x, y, fw, fh)

    def abdomen(face, x, y, fw, fh):
        if face in ('left', 'right', 'top') and (x if face == 'top' else x) % 2 == 1 and face != 'top':
            return MOTH_FUR[1]
        return fur(face, x, y, fw, fh)

    def ruff(face, x, y, fw, fh):
        return MOTH_FUR[5] if (x + y) % 2 == 0 else MOTH_FUR[4]

    paint(cv, 0, 0, 2, 2, 2, head)
    paint(cv, 8, 0, 3, 3, 3, fur)
    paint(cv, 20, 0, 2, 2, 5, abdomen)
    paint(cv, 34, 0, 4, 4, 1, ruff)

    def feather(face, x, y, fw, fh):
        if face != 'front':
            return None  # one face only, as for the wings
        if x == fw // 2:
            return MOTH_FUR[3]
        return MOTH_FUR[5] if (y + x) % 2 == 0 else None

    paint(cv, 44, 0, 3, 4, 0, feather)
    _moth_wing(cv, 0, 8, 10, 7, n, (5.5, 2.6, 1.9), 0)
    _moth_wing(cv, 0, 16, 7, 5, n, (3.2, 1.8, 1.3), 1)
    return cv


# --------------------------------------------------------------------------- Titania

def titania_regolith(v=0):
    """Coarse-grained: most pixels grouped into 2x2 grains, so it reads rougher than the other moons' fine dust."""
    cv = dust(P.TITANIA_SOIL, 16101 + v * 19)
    w = white(16151 + v * 19)
    for y in range(0, 16, 2):
        for x in range(0, 16, 2):
            if w[y][x] < 0.7:
                c = cv[x, y]
                for dx, dy in ((1, 0), (0, 1), (1, 1)):
                    cv[x + dx, y + dy] = c
    return cv


def titania_ice():
    return _rock(P.TITANIA_ICE, 16201)


def canyon_wall_ice():
    """Pale canyon-wall ice: layered in thin horizontal bands, like the faces of the great canyons."""
    r = P.CANYON_WALL
    cv = _rock(r, 16301)
    for y in (3, 4, 10):
        for x in range(16):
            if (x * 7 + y) % 5:
                cv[x, y] = r[1] if y != 4 else r[4]
    return cv


def scree_roller():
    """64x32 sheet for ScreeRollerModel: banded grey-brown plates with pale edges, soft darker body, pale snout."""
    cv = Canvas(64, 32)
    r = P.ROLLER
    n = clustered(64, 64, 16401, passes=1)

    def plate(face, x, y, fw, fh):
        if face == 'bottom':
            return r[1]
        if face == 'top':
            return r[5] if y in (0, fh - 1) else noise_pick(r, 3, n, x, y)
        return r[4] if y == 0 else noise_pick(r, 2, n, x + 20, y)

    paint(cv, 0, 0, 8, 5, 4, plate)
    paint(cv, 0, 9, 9, 6, 3, plate)
    paint(cv, 0, 18, 8, 5, 4, plate)

    def ball(face, x, y, fw, fh):
        return r[5] if (x + y) % 5 == 0 else noise_pick(r, 3, n, x + 30, y + 10)

    paint(cv, 28, 0, 7, 7, 7, ball)

    def head(face, x, y, fw, fh):
        if face == 'front' and y == 0 and x in (0, 2):
            return hexc('#141210')
        return r[4] if face == 'front' else noise_pick(r, 3, n, x + 50, y)

    paint(cv, 28, 14, 3, 3, 4, head)
    paint(cv, 42, 14, 2, 3, 2, lambda f, x, y, fw, fh: r[1])
    return cv


# --------------------------------------------------------------------------- Umbriel

def umbriel_regolith(v=0):
    return dust(P.UMBRIEL_SOIL, 17101 + v * 19)


def umbriel_ice():
    return _rock(P.UMBRIEL_ICE, 17201)


def sooty_ice():
    """Sooty ice: almost black, with a few faint grey ice grains showing through the carbon film."""
    r = P.SOOT
    cv = _rock(r, 17301)
    w = white(17302)
    for y in range(16):
        for x in range(16):
            if w[x][y] > 0.94:
                cv[x, y] = r[5]
    return cv


# Gloam hopper: mauve-grey hide, slate rock plates, big pale eyes that glow in Umbriel's dark (eyes layer).
GLOAM = [hexc(h) for h in ('#211c26', '#2e2735', '#3c3445', '#4b4256', '#5b5168', '#6d627b', '#82778f', '#a094aa')]
GLOAM_PLATE = [hexc(h) for h in ('#3f3c44', '#55525b', '#6c6973', '#86838c')]
GLOAM_EYE = [hexc(h) for h in ('#163a40', '#4fb3c0', '#bdf3f8')]


def _gloam_sheet(eyes_only):
    cv = Canvas(64, 32)
    r = GLOAM
    n = clustered(64, 64, 17401, passes=1)

    def hide(base):
        return lambda face, x, y, fw, fh: None if eyes_only else (
            r[base - 2] if face == 'bottom' else noise_pick(r, base, n, x * 2 + fw, y * 3 + fh))

    def body(face, x, y, fw, fh):
        if eyes_only:
            return None
        if face == 'bottom':
            return r[6]
        if face in ('left', 'right') and y >= fh - 2:
            return r[5]  # paler flanks toward the belly
        return noise_pick(r, 3, n, x, y)

    def plate(face, x, y, fw, fh):
        if eyes_only:
            return None
        if face == 'top':
            return GLOAM_PLATE[3] if y == 0 else GLOAM_PLATE[2 if (x + y) % 3 else 1]
        return GLOAM_PLATE[0] if face == 'bottom' else GLOAM_PLATE[1]

    def head(face, x, y, fw, fh):
        if face == 'front' and y == 1:
            # two big eyes, pale with a dark rim toward the middle
            if x in (0, 3):
                return GLOAM_EYE[2]
            return None if eyes_only else r[1]  # the dark bridge between the eyes
        if eyes_only:
            return None
        if face == 'front' and y == 2 and x in (0, 3):
            return GLOAM_EYE[1]
        if face == 'front' and y == 2:
            return None if eyes_only else r[2]
        return noise_pick(r, 4, n, x + 30, y)

    def ear(face, x, y, fw, fh):
        if eyes_only:
            return None
        if face in ('front', 'back') and 0 < x < fw - 1 and y > 0:
            return r[6]  # the pale inner ear
        return r[3]

    def snout(face, x, y, fw, fh):
        if eyes_only:
            return None
        return r[1] if face == 'front' else r[4]

    paint(cv, 0, 0, 5, 5, 6, body)
    paint(cv, 22, 0, 4, 1, 3, plate)
    paint(cv, 36, 0, 4, 4, 4, head)
    paint(cv, 52, 0, 3, 3, 1, ear)
    paint(cv, 52, 4, 2, 1, 1, snout)
    paint(cv, 0, 12, 2, 4, 4, hide(3))
    paint(cv, 0, 20, 2, 1, 4, lambda f, x, y, fw, fh: None if eyes_only else (r[1] if f == 'bottom' else r[5]))
    paint(cv, 12, 12, 1, 3, 1, hide(4))
    paint(cv, 16, 12, 1, 1, 8, lambda f, x, y, fw, fh: None if eyes_only else (r[2] if (x + y) % 4 == 0 else r[3]))
    paint(cv, 36, 12, 2, 2, 2, lambda f, x, y, fw, fh: None if eyes_only else r[7 if (x + y) % 2 else 6])
    return cv


def gloam_hopper():
    """64x32 sheet for GloamHopperModel (box layout in moon_creatures.faces)."""
    return _gloam_sheet(False)


def gloam_hopper_eyes():
    """The glowing eyes alone, for the renderer's eyes layer."""
    return _gloam_sheet(True)


# --------------------------------------------------------------------------- Oberon

def oberon_regolith(v=0):
    """Calmer than plain dust: the darkest and brightest lone grains are pulled one step toward the middle."""
    r = P.OBERON_SOIL
    cv = dust(r, 18101 + v * 19)
    for y in range(16):
        for x in range(16):
            if cv[x, y] == r[0]:
                cv[x, y] = r[1]
            elif cv[x, y] == r[5]:
                cv[x, y] = r[4]
    return cv


def oberon_ice():
    return _rock(P.OBERON_ICE, 18201)


def dark_crater_fill():
    """Dark crater fill: a near-black, finely grained deposit with a few low ripples."""
    return dust(P.DARK_FILL, 18301)


# Rim burrower (Oberon): reddish-grey hide like the moon's dusky soil, darker bands on the spine ridge, pale bone shovel
# and claws.
BURROW_HIDE = [hexc(h) for h in ('#2a2420', '#3a322d', '#4a403a', '#5a4f48', '#6c5f56', '#7f7066', '#938377')]
BURROW_BONE = [hexc(h) for h in ('#8f826f', '#b3a58f', '#d0c3ad', '#e6dbc7')]


def rim_burrower():
    """64x32 sheet for RimBurrowerModel (box layout in moon_creatures.faces)."""
    cv = Canvas(64, 32)
    r = BURROW_HIDE
    b = BURROW_BONE
    n = clustered(64, 64, 18401, passes=1)

    def body(face, x, y, fw, fh):
        if face == 'bottom':
            return r[1]
        if face == 'top':
            return r[3] if x % 3 else r[4]
        if y == fh - 1:
            return r[2]
        return noise_pick(r, 3, n, x, y)

    def hump(face, x, y, fw, fh):
        if face == 'top':
            return r[4] if (y % 2) else r[5]
        return noise_pick(r, 4, n, x + 30, y)

    def head(face, x, y, fw, fh):
        if face == 'top' and y == 0 and x in (0, fw - 1):
            return hexc('#141010')  # small eyes on the top corners
        return noise_pick(r, 4, n, x + 40, y + 10)

    paint(cv, 0, 0, 7, 4, 8, body)
    paint(cv, 30, 0, 5, 2, 5, hump)
    paint(cv, 50, 0, 2, 1, 1, lambda f, x, y, fw, fh: r[1] if f != 'top' else r[2])
    paint(cv, 0, 12, 6, 2, 4, head)
    paint(cv, 20, 12, 6, 1, 2, lambda f, x, y, fw, fh: b[3] if f == 'top' else (b[1] if f == 'bottom' else b[2]))
    paint(cv, 36, 12, 2, 3, 2, lambda f, x, y, fw, fh: noise_pick(r, 3, n, x + 5, y + 50))
    paint(cv, 44, 12, 3, 1, 3, lambda f, x, y, fw, fh: b[2] if f == 'top' else b[1])
    paint(cv, 56, 0, 1, 1, 2, lambda f, x, y, fw, fh: b[3] if f == 'front' else b[2])
    paint(cv, 20, 16, 2, 2, 2, lambda f, x, y, fw, fh: r[2])
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
    base = ariel_regolith(0)
    save(base, 'block', 'ariel_regolith', dust_spec)
    save(ariel_regolith(1), 'block', 'ariel_regolith_1', dust_spec)
    for side in ('left', 'right'):
        save(footprint(base, prints(P.ARIEL_SOIL), side), 'block', f'ariel_regolith_footprint_{side}', (22, 8, 64, None))
    save(ariel_ice(), 'block', 'ariel_ice', (40, 12, 20, None))
    save(graben_floor_ice(), 'block', 'graben_floor_ice', (100, 16, 0, None))
    save(frost_moth(), 'entity', 'uranus/frost_moth')
    base = titania_regolith(0)
    save(base, 'block', 'titania_regolith', dust_spec)
    save(titania_regolith(1), 'block', 'titania_regolith_1', dust_spec)
    for side in ('left', 'right'):
        save(footprint(base, prints(P.TITANIA_SOIL), side), 'block', f'titania_regolith_footprint_{side}', (22, 8, 64, None))
    save(titania_ice(), 'block', 'titania_ice', (40, 12, 20, None))
    save(canyon_wall_ice(), 'block', 'canyon_wall_ice', (40, 12, 20, None))
    save(scree_roller(), 'entity', 'uranus/scree_roller')
    base = umbriel_regolith(0)
    save(base, 'block', 'umbriel_regolith', dust_spec)
    save(umbriel_regolith(1), 'block', 'umbriel_regolith_1', dust_spec)
    for side in ('left', 'right'):
        save(footprint(base, prints(P.UMBRIEL_SOIL), side), 'block', f'umbriel_regolith_footprint_{side}', (22, 8, 64, None))
    save(umbriel_ice(), 'block', 'umbriel_ice', (40, 12, 20, None))
    save(sooty_ice(), 'block', 'sooty_ice', (30, 10, 30, None))
    save(gloam_hopper(), 'entity', 'uranus/gloam_hopper')
    save(gloam_hopper_eyes(), 'entity', 'uranus/gloam_hopper_eyes')
    base = oberon_regolith(0)
    save(base, 'block', 'oberon_regolith', dust_spec)
    save(oberon_regolith(1), 'block', 'oberon_regolith_1', dust_spec)
    for side in ('left', 'right'):
        save(footprint(base, prints(P.OBERON_SOIL), side), 'block', f'oberon_regolith_footprint_{side}', (22, 8, 64, None))
    save(oberon_ice(), 'block', 'oberon_ice', (40, 12, 20, None))
    save(dark_crater_fill(), 'block', 'dark_crater_fill', dust_spec)
    save(rim_burrower(), 'entity', 'uranus/rim_burrower')
    import os
    out = os.path.join(os.path.dirname(__file__), '..', '..', '..', 'src', 'main', 'resources', 'assets', 'spacebreaker', 'textures',
                       'environment')
    os.makedirs(out, exist_ok=True)
    uranus_disc().save(os.path.join(out, 'uranus_disc.png'))
    uranus_rings().save(os.path.join(out, 'uranus_rings.png'))
