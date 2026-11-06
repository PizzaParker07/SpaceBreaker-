"""Mercury textures: dark graphite-rich regolith (boot prints), grey crust, low-reflectance graphite rock and ore,
bright hollow deposits, and the Terminator Crawler."""
import random

from sbtex.core import Canvas, ramp, clustered, fbm, parse_mask, mask_from, dome_shade, edge_pixels, pick
from modules.moon import quant, white, blend, footprint, overlay, RAW_CHUNK
from modules.mars import dust, prints
from modules.moon_creatures import paint, noise_pick

# Regolith: dark, cool grey with a faint blue cast (low-reflectance material).
REGOLITH = ramp('#1c1d20', '#2b2d31', '#303236', '#35373b', '#3b3d42', '#424449')
# Crust: mid grey intercrater plains rock.
CRUST = ramp('#2f2f31', '#3b3b3e', '#47474a', '#535357', '#606064', '#6e6e72')
# Low-reflectance rock: near black, graphite flakes.
LRM = ramp('#141518', '#1c1d21', '#24262a', '#2d2f34', '#383a40')
GRAPHITE = ramp('#2a2c31', '#4a4e57', '#7a808b', '#a9afb9')
# Hollow deposits: bright, slightly blue-white sulfide crust.
HOLLOW = ramp('#55606e', '#6c7888', '#808d9e', '#94a1b2', '#a8b4c3', '#bcc6d2')
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
    save(low_reflectance_rock(), 'block', 'low_reflectance_rock', rock)
    gr = lambda c: (160, 210, 0, 255) if c[3] else None
    save(graphite_overlay(), 'block', 'graphite_ore_overlay', (140, 200, 0, gr), wrap=False)
    save(graphite_item(), 'item', 'graphite')
    save(crawler(), 'entity', 'mercury/terminator_crawler')
    save(crawler_eyes(), 'entity', 'mercury/terminator_crawler_eyes')
