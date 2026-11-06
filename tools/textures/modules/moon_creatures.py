"""Moon creature textures (64x64 entity sheets). UV layouts mirror the Java models in client/moon:
a box added with texOffs(u, v) and size (w, h, d) uses the vanilla cube layout:

    top    (u+d,     v)      w x d        (visible top of the mob)
    bottom (u+d+w,   v)      w x d
    sides  (u,       v+d)    d x h  right (-x)
           (u+d,     v+d)    w x h  front (-z)
           (u+d+w,   v+d)    d x h  left (+x)
           (u+2d+w,  v+d)    w x h  back (+z)
"""
import random

from sbtex.core import Canvas, ramp, clustered
from sbtex import palettes as P

TICK = ramp('#2f2c28', '#433f39', '#57524b', '#6d675f', '#837d74', '#9a948a', '#b2aca1')
ELDER = ramp('#1f1d1b', '#2e2b27', '#3e3a35', '#504b44', '#655f57', '#7c766c', '#958f84')
DUST = ramp('#3a3833', '#4a4741', '#5b5850', '#6d6a61', '#807c72', '#948f84', '#aaa598')
EYE = ramp('#5a3a10', '#b07a22', '#f2c45a')
LENS = ramp('#07080a', '#15181d', '#2a2f37')
RED = ramp('#4a0b0b', '#9a1c16', '#e8452e', '#ff9a7a')


def faces(u, v, w, h, d):
    return {
        'top': (u + d, v, w, d), 'bottom': (u + d + w, v, w, d),
        'right': (u, v + d, d, h), 'front': (u + d, v + d, w, h),
        'left': (u + d + w, v + d, d, h), 'back': (u + 2 * d + w, v + d, w, h),
    }


def paint(cv, u, v, w, h, d, fn):
    """fn(face, x, y, fw, fh) -> colour or None, x/y local to the face."""
    for name, (fx, fy, fw, fh) in faces(u, v, w, h, d).items():
        for y in range(fh):
            for x in range(fw):
                c = fn(name, x, y, fw, fh)
                if c is not None:
                    cv.put(fx + x, fy + y, c)


def noise_pick(r, base, n, x, y, spread=1):
    """Base tone with a little clumpy variation."""
    t = base + (1 if n[y % 64][x % 64] > 0.72 else (-1 if n[y % 64][x % 64] < 0.25 else 0)) * spread
    return r[max(0, min(len(r) - 1, t))]


# --------------------------------------------------------------------------- regolith tick

def tick_sheet(r, crystals=False, seed=5):
    cv = Canvas(64, 64)
    n = clustered(64, 64, seed, passes=1)
    rng = random.Random(seed)

    top = [
        "1223322.",
        "23344332",
        "23465432",
        "234440432",
        "23344432",
        "2304443 2",
        "23446432",
        "23344332",
        "22333332",
        "1222222.",
    ]

    def body(face, x, y, fw, fh):
        if face == 'top':
            # leathery oval carapace: pale dusty centre, darker rim, a few pits and dust specks
            ch = top[y][x] if x < len(top[y]) else '2'
            t = 1 if ch in '. ' else int(ch)
            return r[t] if t in (0, 6) else noise_pick(r, t, n, x + 10, y)
        if face == 'bottom':
            return noise_pick(r, 1, n, x + 20, y + 30)
        # sides: top edge catches light, lower part in shadow
        t = 3 if y == 0 else (2 if y == 1 else 1)
        if face == 'back' and y == 1 and x % 2 == 1:
            t = 0
        return noise_pick(r, t, n, x + 3, y + 40)

    paint(cv, 0, 0, 8, 3, 10, body)

    def shield(face, x, y, fw, fh):
        if face == 'top':
            t = 5 if (x == 0 or y == 0) else (3 if (x == fw - 1 or y == fh - 1) else 4)
            return r[t]
        return r[3] if face != 'bottom' else r[2]

    paint(cv, 0, 13, 6, 1, 5, shield)

    def head(face, x, y, fw, fh):
        if face == 'top':
            return r[4] if y == 0 else r[3]
        if face == 'front':
            return r[1] if y == 1 else r[2]
        return r[2] if y == 0 else r[1]

    paint(cv, 22, 13, 4, 2, 3, head)
    paint(cv, 36, 13, 1, 1, 2, lambda f, x, y, fw, fh: r[4] if f == 'front' else r[2])

    def leg(face, x, y, fw, fh):
        if face in ('top', 'front', 'back', 'bottom'):
            # jointed: dark segments with pale joint rings, tip darkest
            if x in (2, 5):
                return r[4]
            if face == 'top':
                return r[1] if x < 1 else r[3]
            return r[2]
        return r[1]

    paint(cv, 0, 20, 8, 1, 1, leg)

    if crystals:
        # mineral crust on an old elder: KREEP crystals grown into the carapace
        k = P.KREEP
        for x, y in [(12, 3), (15, 6), (11, 8), (16, 2), (13, 10)]:
            cv[x, y] = k[3]
            cv[x + 1, y] = k[1]
        cv[12, 3] = k[5]
    return cv


def tick_eyes():
    cv = Canvas(64, 64)
    # head front face starts at (25, 16), 4 x 2: two glinting eyes at the outer corners
    cv[25, 16] = EYE[2]
    cv[28, 16] = EYE[2]
    return cv


# --------------------------------------------------------------------------- dust swimmer

def dust_swimmer():
    """Mottled dust-grey back (camouflage) with darker spots, a pale belly, eyes on short stalks."""
    r = DUST
    cv = Canvas(64, 64)
    n = clustered(64, 64, 17, passes=1)
    spots = {(1, 2), (5, 3), (2, 6), (6, 7), (3, 10), (1, 12), (6, 11), (4, 5)}

    def body(face, x, y, fw, fh):
        if face == 'top':
            t = 4
            if x in (0, fw - 1):
                t = 3
            if (x, y) in spots or (x - 1, y) in spots:
                t = 1  # dark spots
            if x in (3, 4) and y % 4 == 1:
                t = 5  # pale spine scales
            return noise_pick(r, t, n, x, y)
        if face == 'bottom':
            return r[6] if (x + y) % 4 else r[5]
        t = 3 if y == 0 else (2 if y == 1 else 5)
        return noise_pick(r, t, n, x + 30, y + 40)

    paint(cv, 0, 0, 8, 3, 14, body)

    def head(face, x, y, fw, fh):
        if face == 'top':
            return r[4] if y > 0 else r[5]
        if face == 'front':
            return r[1] if y == 1 and 1 <= x <= 4 else r[3]  # wide mouth slit for sifting dust
        return r[3] if face != 'bottom' else r[6]

    paint(cv, 0, 17, 6, 2, 4, head)
    paint(cv, 20, 17, 1, 2, 1, lambda f, x, y, fw, fh: (P.ILMENITE[1] if y == 0 else r[3]) if f != 'top' else P.ILMENITE[5])

    def fin(face, x, y, fw, fh):
        if face == 'top':
            if y == fh - 1 or x == fw - 1:
                return r[5]  # pale trailing edge
            return r[2] if x % 2 == 0 else r[4]  # rays
        if face == 'bottom':
            return r[6]
        return r[3]

    paint(cv, 24, 17, 7, 1, 6, fin)

    def dorsal(face, x, y, fw, fh):
        if face in ('right', 'left'):
            if y == 0:
                return r[5]
            return r[2] if x % 2 == 0 else r[3]
        return r[4]

    paint(cv, 44, 0, 1, 5, 3, dorsal)
    paint(cv, 52, 0, 1, 3, 3, dorsal)

    def tail(face, x, y, fw, fh):
        if face == 'top':
            return r[4] if y % 2 else r[2]
        if face == 'bottom':
            return r[6]
        return r[2]

    paint(cv, 0, 25, 3, 2, 7, tail)
    paint(cv, 20, 25, 1, 4, 3, lambda f, x, y, fw, fh: r[5] if y == 0 else (r[3] if x % 2 else r[2]))
    return cv


# --------------------------------------------------------------------------- survey drone

def survey_drone():
    al = P.ALUMINIUM
    au = P.GOLD
    cell = P.CELL
    back = P.BACKSHEET
    cv = Canvas(64, 64)
    rng = random.Random(23)
    foil = clustered(64, 64, 29, passes=0)

    def gold(x, y):
        # crinkled multi-layer insulation: gold with random light and dark creases
        v = foil[y % 64][x % 64]
        return au[5] if v > 0.8 else (au[2] if v < 0.2 else au[4] if v > 0.5 else au[3])

    def core(face, x, y, fw, fh):
        edge = x == 0 or y == 0 or x == fw - 1 or y == fh - 1
        if face == 'top':
            if edge:
                return al[3]
            if (x, y) in ((1, 1), (6, 1), (1, 6), (6, 6)):
                return al[2]  # bolts
            return al[6] if (x + y) < 6 else al[5]
        if face == 'bottom':
            return al[1] if edge else al[2]
        if face == 'back':
            if edge:
                return al[3]
            return al[2] if y in (2, 4) and 1 < x < 6 else al[4]  # radiator slots
        if edge:
            return au[2]
        return gold(x + (0 if face == 'front' else 20), y)

    paint(cv, 0, 0, 8, 8, 8, core)

    def lens(face, x, y, fw, fh):
        if face == 'front':
            if (x, y) in ((1, 1), (2, 1), (1, 2), (2, 2)):
                return RED[1]
            if (x, y) == (1, 1):
                return RED[2]
            return LENS[1] if 0 < x < 3 and 0 < y < 3 else LENS[2]
        return al[2]

    paint(cv, 32, 0, 4, 4, 1, lens)
    cv[34, 2] = LENS[0]
    cv[33, 2] = RED[2]
    paint(cv, 44, 0, 1, 5, 1, lambda f, x, y, fw, fh: al[5] if f != 'bottom' else al[3])
    paint(cv, 48, 0, 2, 1, 2, lambda f, x, y, fw, fh: RED[2] if f == 'top' else RED[1])

    def wing(face, x, y, fw, fh):
        if face == 'top':
            if x == 0 or y == 0 or x == fw - 1 or y == fh - 1:
                return al[3]
            if x % 3 == 0 or y == 3:
                return cell[1]  # cell gaps
            return cell[5] if (x + y) % 5 == 0 else cell[3]
        if face == 'bottom':
            return back[2] if (x + y) % 3 else back[1]
        return al[3]

    paint(cv, 0, 16, 9, 1, 6, wing)

    def nozzle(face, x, y, fw, fh):
        if face == 'bottom':
            return LENS[0]
        return P.STEEL[3] if y == 0 else P.STEEL[2]

    paint(cv, 12, 24, 2, 2, 2, nozzle)
    return cv


def survey_drone_lights():
    cv = Canvas(64, 64)
    # lens iris (front face of the lens box at (33, 1))
    cv[34, 2] = RED[3]
    cv[34, 3] = RED[2]
    cv[35, 2] = RED[2]
    cv[35, 3] = RED[2]
    # antenna beacon (top of the tip at (50, 0))
    cv[50, 0] = RED[3]
    cv[51, 1] = RED[2]
    # status LEDs on the back panel (back face of core at (24, 8))
    cv[25, 9] = (120, 255, 160, 255)
    cv[27, 9] = (255, 200, 80, 255)
    return cv


def stun_bolt():
    cv = Canvas(16, 16)
    rows = [
        "................",
        "................",
        "................",
        ".......a........",
        "......aba.......",
        ".....abcba......",
        "....abcdcba.....",
        "...abcdddcba....",
        "....abcdcba.....",
        ".....abcba......",
        "......aba.......",
        ".......a........",
        "................",
        "................",
        "................",
        "................",
    ]
    cols = {'a': (40, 120, 160, 140), 'b': (70, 190, 230, 210), 'c': (150, 235, 255, 255), 'd': (235, 255, 255, 255)}
    for y, row in enumerate(rows):
        for x, ch in enumerate(row):
            if ch in cols:
                cv[x, y] = cols[ch]
    return cv


def export(save):
    save(tick_sheet(TICK), 'entity/moon', 'regolith_tick')
    save(tick_sheet(ELDER, crystals=True, seed=9), 'entity/moon', 'elder_regolith_tick')
    save(tick_eyes(), 'entity/moon', 'regolith_tick_eyes')
    save(dust_swimmer(), 'entity/moon', 'dust_swimmer')
    save(survey_drone(), 'entity/moon', 'survey_drone')
    save(survey_drone_lights(), 'entity/moon', 'survey_drone_lights')
    save(stun_bolt(), 'entity/moon', 'stun_bolt')


# --------------------------------------------------------------------------- glimmer beetle

BEETLE = ramp('#11171a', '#1b2427', '#263538', '#35494b', '#4a6563', '#678783')
GLIMMER = ramp('#1f5a50', '#3a9c86', '#6fd8bf', '#c2f5e7')


def glimmer_beetle():
    r = BEETLE
    g = GLIMMER
    cv = Canvas(64, 64)

    def abdomen(face, x, y, fw, fh):
        if face == 'top':
            if x in (2, 3):
                return r[1] if y < fh - 2 else g[0]  # elytra seam, glow showing at the tail
            if y >= fh - 2:
                return g[1] if (x + y) % 2 else g[0]
            t = 4 if (x in (1, 4) and y < 3) else (3 if x in (1, 4) else 2)
            if y == 0:
                t += 1
            return r[min(5, t)]
        if face == 'back':
            return g[2] if y == 1 else g[1]
        if face == 'bottom':
            return g[0] if y >= fh - 3 else r[0]
        return r[1] if y > 0 else r[3]

    paint(cv, 0, 0, 6, 3, 7, abdomen)
    paint(cv, 0, 10, 4, 2, 3, lambda f, x, y, fw, fh: (r[4] if y == 0 else r[3]) if f == 'top' else r[1])
    paint(cv, 14, 10, 2, 2, 2, lambda f, x, y, fw, fh: (g[2] if y == 0 else r[1]) if f == 'front' else r[2])
    paint(cv, 22, 10, 1, 2, 1, lambda f, x, y, fw, fh: r[3] if y == 0 else r[2])
    paint(cv, 0, 16, 4, 1, 1, lambda f, x, y, fw, fh: r[3] if x == 1 else r[1])
    return cv


def glimmer_beetle_glow():
    """Only the luminous parts: rear of the abdomen, tail seam, eye spots."""
    g = GLIMMER
    cv = Canvas(64, 64)
    # abdomen top rear rows (top face at (7, 0), 6 x 7)
    for x in range(6):
        for y in (5, 6):
            cv[7 + x, y] = g[2] if (x + y) % 2 else g[1]
    cv[9, 4] = g[1]
    cv[10, 4] = g[1]
    # back face at (20, 7), 6 x 3
    for x in range(6):
        for y in range(3):
            cv[20 + x, 7 + y] = g[3] if y == 1 else g[2]
    # bottom face rear (13, 0) rows 4..6
    for x in range(6):
        for y in range(4, 7):
            cv[13 + x, y] = g[1]
    # eye spots on the head front (16, 12)
    cv[16, 12] = g[3]
    cv[17, 12] = g[3]
    return cv


_export_creatures = export


def export(save):  # noqa: F811 - extend the module export
    _export_creatures(save)
    save(glimmer_beetle(), 'entity/moon', 'glimmer_beetle')
    save(glimmer_beetle_glow(), 'entity/moon', 'glimmer_beetle_glow')
