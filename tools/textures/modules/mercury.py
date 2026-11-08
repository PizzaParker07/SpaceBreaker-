"""Mercury textures: dark graphite-rich regolith (boot prints), grey crust, low-reflectance graphite rock and ore,
bright hollow deposits, and the Terminator Crawler."""
import random

from sbtex.core import Canvas, ramp, clustered, fbm, parse_mask, mask_from, dome_shade, edge_pixels, pick
from modules.moon import quant, white, blend, blur_dir, footprint, overlay, RAW_CHUNK
from modules.mars import dust, prints
from modules.moon_creatures import paint, noise_pick

# Regolith: dark, cool grey with a faint blue cast (low-reflectance material).
REGOLITH = ramp('#55514d', '#736e69', '#7b7671', '#837e79', '#8c8681', '#958f8a')
# Crust: mid grey intercrater plains rock.
CRUST = ramp('#58534c', '#6c665e', '#7c756c', '#8c847a', '#9c948a', '#aaa297')
# Low-reflectance rock: near black, graphite flakes.
LRM = ramp('#3c3a38', '#4b4846', '#55524f', '#5e5b58', '#6a6764')
GRAPHITE = ramp('#2a2c31', '#4a4e57', '#7a808b', '#a9afb9')
# Hollow deposits: bright, slightly blue-white sulfide crust.
HOLLOW = ramp('#7e848e', '#959ba5', '#a5abb4', '#b8bcc4', '#c6cad1', '#d3d6dc')
CRAWLER = ramp('#2a2522', '#3b332e', '#4f443c', '#65564b', '#7c6a5c', '#93806f')
GLOW = ramp('#6a2c08', '#c8641a', '#ffb04a', '#ffe0a0')


def mercury_regolith(v=0):
    return dust(REGOLITH, 7001 + v * 29)


def hollow_deposit(v=0):
    """Bright, flaky sublimation crust of the hollows: pale blue-white with soft shadowed pits."""
    f = blend((clustered(16, 16, 7101 + v, passes=2), 0.6), (white(7102 + v), 0.4))
    cv, _ = quant(f, HOLLOW[1:], [8, 26, 38, 20, 8])
    rng = random.Random(7103 + v)
    for _ in range(3):
        x, y = rng.randrange(16), rng.randrange(16)
        cv[x, y] = HOLLOW[0]
        cv[x + 1, y + 1] = HOLLOW[5]
    return cv


ICE = ramp('#2c2f36', '#7e94a8', '#9db2c4', '#b6c8d6', '#cdd9e3', '#e6eef4')


def mercury_ice():
    """Polar crater ice: grey-blue packed ice with dark graphite flecks and a few yellowish sulfur specks."""
    f = blend((blur_dir(white(7701), 3, 1, 1), 0.6), (clustered(16, 16, 7702, passes=1), 0.4))
    cv, _ = quant(f, ICE[2:5], [24, 48, 28])
    rng = random.Random(7703)
    for _ in range(7):
        cv[rng.randrange(16), rng.randrange(16)] = ICE[0]
    for x, y in [(4, 11), (12, 3)]:
        cv[x, y] = HOLLOW[1]
    for x, y in [(3, 4), (4, 5), (10, 9), (11, 10)]:
        cv[x, y] = ICE[5]
    return cv


def mercurian_crust():
    """Old cratered crust: fine-grained grey rock with shock-darkened streaks."""
    r = CRUST
    f = blend((clustered(16, 16, 7201, passes=1), 0.5), (white(7202), 0.35), (fbm(16, 16, 7203, [(8, 1.0)]), 0.15))
    cv, _ = quant(f, [r[1], r[2], r[3], r[4], r[5]], [8, 24, 38, 22, 8])
    for x, y in [(3, 3), (4, 4), (11, 9), (12, 10), (7, 13)]:
        cv[x, y] = r[0]
    return cv


def low_reflectance_rock():
    """Dark graphite-rich rock: near-black with tiny lustrous flakes."""
    f = blend((white(7301), 0.55), (clustered(16, 16, 7302), 0.45))
    cv, _ = quant(f, LRM, [10, 26, 34, 22, 8])
    for x, y in [(2, 5), (9, 2), (13, 11), (6, 12)]:
        cv[x, y] = GRAPHITE[2]
    return cv


GRA = [
    (2, 2, [(0, 0, 2), (1, 0, 1), (1, 1, 0)]),
    (10, 1, [(0, 0, 3), (1, 0, 2), (0, 1, 1), (1, 1, 0)]),
    (5, 6, [(0, 0, 2), (1, 0, 3), (2, 0, 1), (1, 1, 0)]),
    (12, 7, [(0, 0, 3), (0, 1, 1), (1, 1, 0)]),
    (1, 11, [(0, 0, 2), (1, 0, 2), (1, 1, 0), (2, 1, 1)]),
    (8, 12, [(0, 0, 3), (1, 0, 1), (0, 1, 0)]),
    (13, 13, [(0, 0, 2), (0, 1, 1)]),
]


def graphite_overlay():
    """Flat metallic graphite flakes: grey blades with one bright sheen pixel."""
    return overlay(GRA, [GRAPHITE[0], GRAPHITE[1], GRAPHITE[2], GRAPHITE[3]])


def graphite_item():
    """A flaky lump of graphite: dark steel grey, layered, with a soft metallic sheen."""
    m = mask_from(parse_mask(RAW_CHUNK))
    sh = dome_shade(m, roundness=3.0)
    rng = random.Random(7401)
    cv = Canvas()
    r = ramp('#141518', '#24262b', '#353840', '#4a4e57', '#626873', '#7f8692', '#a9afb9', '#d0d5dc')
    for y in range(16):
        for x in range(16):
            if m[y][x]:
                v = 0.05 + 0.8 * sh[y][x] + (rng.random() - 0.5) * 0.12
                cv[x, y] = pick(r[2:6], v)
    for y in (5, 8, 11):
        for x in range(3, 13):
            if m[y][x] and cv[x, y] != r[0]:
                cv[x, y] = r[2]
    for x, y in [(5, 4), (6, 4), (9, 7), (10, 7)]:
        cv[x, y] = r[6]
    cv[5, 4] = r[7]
    for x, y, side in edge_pixels(m):
        cv[x, y] = r[0] if side == 'br' else r[1]
    return cv


def crawler():
    """Terminator Crawler: a long, low arthropod armoured in dark heat-shield plates; glowing ember eyes."""
    cv = Canvas(64, 64)
    n = clustered(64, 64, 7501, passes=1)

    def body(face, x, y, fw, fh):
        if face == 'top':
            seg = y % 4
            t = 4 if seg == 0 else (3 if seg < 3 else 1)
            if x in (0, fw - 1):
                t -= 1
            return noise_pick(CRAWLER, max(0, t), n, x, y)
        if face == 'bottom':
            return CRAWLER[1]
        return noise_pick(CRAWLER, 2 if y == 0 else 1, n, x + 20, y + 30)

    paint(cv, 0, 0, 8, 3, 18, body)

    def head(face, x, y, fw, fh):
        if face == 'front':
            if y == 1 and x in (1, fw - 2):
                return GLOW[2]
            return CRAWLER[2] if y == 0 else CRAWLER[1]
        return noise_pick(CRAWLER, 3 if face == 'top' else 2, n, x + 40, y)

    paint(cv, 0, 22, 6, 3, 4, head)
    paint(cv, 22, 22, 6, 1, 1, lambda f, x, y, fw, fh: CRAWLER[3] if f == 'top' else CRAWLER[1])
    return cv


def crawler_eyes():
    cv = Canvas(64, 64)
    cv[5, 27] = GLOW[3]
    cv[8, 27] = GLOW[3]
    return cv


HOLLOW_CRUST = ramp('#dfeaf4', '#f0f7fc')
SNAIL_BODY = ramp('#3e3a37', '#524c47', '#665f58', '#7a726a', '#8e857c')


def snail():
    """Hollow Snail (64x64): a grey, finely wrinkled foot and neck; a pale blue-white shell banded like the hollow crust and caked with bright crust patches,
    a dark spiral line coiling in on each side to a pale knob."""
    import math
    cv = Canvas(64, 64)
    n = clustered(64, 32, 7601, passes=1)

    def body(face, x, y, fw, fh, ox=0):
        if face == 'bottom':
            return SNAIL_BODY[0]
        if face == 'top':
            return noise_pick(SNAIL_BODY, 3 if x in (1, fw - 2) else 2, n, x + ox, y)
        return noise_pick(SNAIL_BODY, 2 if y == 0 else 1, n, x + 20 + ox, y + 4)

    paint(cv, 0, 0, 5, 2, 13, body)
    paint(cv, 36, 0, 3, 1, 3, body)
    paint(cv, 48, 0, 3, 4, 3, lambda f, x, y, fw, fh: body(f, x, y, fw, fh, 9) if (y + x) % 3 else SNAIL_BODY[1])

    def spiral(x, y, fw, fh):
        cx, cy = (fw - 1) / 2, (fh - 1) / 2
        r = math.hypot(x - cx, y - cy)
        a = (math.atan2(y - cy, x - cx) / (2 * math.pi)) % 1.0
        # a line whose radius grows by 1.7 px per turn
        return abs(((r / 1.7) - a) % 1.0 - 0.5) > 0.36

    crust = clustered(64, 32, 7602, passes=2)

    def shell(face, x, y, fw, fh):
        if face == 'bottom':
            return SNAIL_BODY[1]
        if face != 'front' and crust[(y * 3 + (7 if face == 'top' else 0)) % 32][(x * 5 + y) % 64] > 0.68:
            return HOLLOW_CRUST[1 if (x + y) % 3 else 0]  # bright patches of hollow crust caught on the shell
        if face in ('left', 'right'):
            if spiral(x, y, fw, fh):
                return GRAPHITE[2]
            turn = int(math.hypot(x - (fw - 1) / 2, y - (fh - 1) / 2) / 1.7)
            return HOLLOW[5] if turn % 2 == 0 else HOLLOW[3]
        if face == 'top':
            return HOLLOW[5] if x in (1, 2, 3) else HOLLOW[4]
        # the front and back of the coil: growth bands across the whorl
        return HOLLOW[4] if y % 2 else HOLLOW[2 if (x + y) % 5 else 1]

    paint(cv, 0, 16, 5, 6, 8, shell)
    paint(cv, 28, 16, 5, 8, 6, shell)
    paint(cv, 50, 24, 1, 2, 2, lambda f, x, y, fw, fh: HOLLOW[5] if f in ('right', 'left', 'top') else HOLLOW[3])

    def head(face, x, y, fw, fh):
        if face == 'front' and y == fh - 1:
            return SNAIL_BODY[0]  # rasping mouth
        return noise_pick(SNAIL_BODY, 3 if face == 'top' else 2, n, x + 40, y + 10)

    paint(cv, 48, 8, 3, 2, 3, head)
    paint(cv, 36, 6, 1, 4, 1, lambda f, x, y, fw, fh: GRAPHITE[0] if y == 0 else SNAIL_BODY[2])
    paint(cv, 36, 12, 1, 1, 2, lambda f, x, y, fw, fh: SNAIL_BODY[3] if f == 'front' else SNAIL_BODY[2])
    return cv


def export(save):
    dust_spec = (18, 8, 64, None)
    for name, fn, r in (('mercury_regolith', mercury_regolith, REGOLITH), ('hollow_deposit', hollow_deposit, HOLLOW)):
        base = fn(0)
        save(base, 'block', name, dust_spec)
        save(fn(1), 'block', name + '_1', dust_spec)
        for side in ('left', 'right'):
            save(footprint(base, prints(r), side), 'block', f'{name}_footprint_{side}', (22, 8, 64, None))
    rock = (40, 12, 20, None)
    save(mercurian_crust(), 'block', 'mercurian_crust', rock)
    save(mercury_ice(), 'block', 'mercury_ice', (200, 22, 0, lambda c: (200, 22, 0, 255) if c[3] else None))
    save(low_reflectance_rock(), 'block', 'low_reflectance_rock', rock)
    gr = lambda c: (160, 210, 0, 255) if c[3] else None
    save(graphite_overlay(), 'block', 'graphite_ore_overlay', (140, 200, 0, gr), wrap=False)
    save(graphite_item(), 'item', 'graphite')
    save(crawler(), 'entity', 'mercury/terminator_crawler')
    save(crawler_eyes(), 'entity', 'mercury/terminator_crawler_eyes')
    save(snail(), 'entity', 'mercury/hollow_snail')
