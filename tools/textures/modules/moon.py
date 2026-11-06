"""Moon block textures: mare basalt, lunar regolith (+ boot prints)."""
import random

from sbtex.core import Canvas, fbm, clustered, pick, parse_mask, mask_from
from sbtex.palettes import MARE_BASALT, REGOLITH


def mare_basalt(seed: int = 11) -> Canvas:
    r = MARE_BASALT
    rng = random.Random(seed)
    big = clustered(16, 16, seed, passes=3)
    clump = clustered(16, 16, seed + 1, passes=1)
    fine = clustered(16, 16, seed + 2, passes=0)
    cv = Canvas()
    for y in range(16):
        for x in range(16):
            v = big[y][x] * 0.45 + clump[y][x] * 0.35 + fine[y][x] * 0.2
            cv[x, y] = pick(r[1:6], 0.05 + v * 0.95)
    # vesicles (gas bubbles): dark pit with a faint lit lower rim
    for x, y in [(3, 4), (11, 2), (8, 11), (14, 13)]:
        cv[x, y] = r[0]
        cv[x + 1, y] = r[1]
        cv[x, y + 1] = r[4]
    # plagioclase laths: a few pale specks
    for x, y, l in [(6, 1, 2), (1, 9, 1), (12, 7, 2), (5, 14, 1)]:
        for i in range(l):
            cv[x + i, y] = r[5]
        cv[x + l, y + 1] = r[2]
    return cv


def _regolith_base(seed: int) -> Canvas:
    r = REGOLITH
    rng = random.Random(seed)
    soft = fbm(16, 16, seed, [(8, 0.5), (4, 0.5)], grain=0.0)
    grain = clustered(16, 16, seed + 7, passes=0)
    cv = Canvas()
    for y in range(16):
        for x in range(16):
            v = 0.55 * soft[y][x] + 0.45 * grain[y][x]
            cv[x, y] = pick(r[2:6], v)
    # dark glass beads / tiny shadows
    for _ in range(6):
        cv[rng.randrange(16), rng.randrange(16)] = r[1]
    # pebbles: lit top-left, shadow bottom-right
    for _ in range(2):
        x, y = rng.randrange(16), rng.randrange(16)
        big = rng.random() < 0.45
        cv[x, y] = r[6]
        cv[x + 1, y + 1] = r[0]
        cv[x + 1, y] = r[4] if not big else r[6]
        if big:
            cv[x, y + 1] = r[5]
            cv[x + 2, y + 1] = r[1]
            cv[x + 1, y + 2] = r[0]
            cv[x, y + 2] = r[2]
        else:
            cv[x + 2, y] = r[2]
    return cv


def lunar_regolith(variant: int = 0) -> Canvas:
    return _regolith_base(101 + variant * 37)


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


def lunar_regolith_footprint(side: str) -> Canvas:
    """Boot print pointing north (toe at top). side = 'left' | 'right'."""
    r = REGOLITH
    cv = lunar_regolith(0)
    m = mask_from(BOOT)
    ox = 3 if side == 'left' else 7
    oy = 3
    h, w = len(m), len(m[0])

    def inside(x, y):
        return 0 <= x < w and 0 <= y < h and m[y][x]

    # raised dust rim just outside the print
    for y in range(-1, h + 1):
        for x in range(-1, w + 1):
            if inside(x, y):
                continue
            near_tl = inside(x + 1, y) or inside(x, y + 1) or inside(x + 1, y + 1)
            near_br = inside(x - 1, y) or inside(x, y - 1) or inside(x - 1, y - 1)
            if near_tl and not near_br:
                cv.put(ox + x, oy + y, r[5])  # rim facing the light
            elif near_br and not near_tl:
                cv.put(ox + x, oy + y, r[3])
    for y in range(h):
        for x in range(w):
            if not m[y][x]:
                continue
            wall_tl = not inside(x - 1, y) or not inside(x, y - 1)   # pit wall facing away from light
            wall_br = not inside(x + 1, y) or not inside(x, y + 1)   # pit wall catching light
            rib = (y % 2 == 0)
            if wall_tl:
                c = r[1]
            elif wall_br:
                c = r[4]
            else:
                c = r[3] if rib else r[2]
            cv.put(ox + x, oy + y, c)
    return cv


def export(save):
    save(mare_basalt(), 'block', 'mare_basalt', (40, 12, 20, None))
    save(lunar_regolith(0), 'block', 'lunar_regolith', (18, 10, 64, None))
    save(lunar_regolith(1), 'block', 'lunar_regolith_1', (18, 10, 64, None))
    for side in ('left', 'right'):
        save(lunar_regolith_footprint(side), 'block', f'lunar_regolith_footprint_{side}', (22, 10, 64, None))
    from sbtex.palettes import ILMENITE  # noqa: F401  (ore overlay lives with the other ores)
