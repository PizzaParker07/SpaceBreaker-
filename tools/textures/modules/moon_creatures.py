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

def tick_sheet(r, seed=5):
    """Young tick: a dusty grey dome (pale crown, darker rim, pits), dark head, orange-dark mouth, pale leg joints."""
    cv = Canvas(64, 64)
    n = clustered(64, 64, seed, passes=1)

    def shell(base, ox, oy):
        def fn(face, x, y, fw, fh):
            if face == 'bottom':
                return noise_pick(r, 1, n, x + ox, y + oy)
            if face == 'top':
                rim = x in (0, fw - 1) or y in (0, fh - 1)
                pit = (x * 5 + y * 3 + ox) % 9 == 0
                return r[1] if pit else noise_pick(r, base + (0 if rim else 1), n, x + ox, y + oy)
            return noise_pick(r, base - (1 if y == fh - 1 else 0), n, x + ox, y + oy)
        return fn

    paint(cv, 0, 0, 8, 2, 9, shell(2, 0, 0))
    paint(cv, 0, 11, 6, 2, 7, shell(3, 7, 13))
    paint(cv, 26, 11, 4, 1, 5, shell(5, 3, 21))

    def head(face, x, y, fw, fh):
        if face == 'top':
            return r[3] if y else r[4]
        if face == 'front':
            return r[1]
        return r[2] if y == 0 else r[1]

    paint(cv, 34, 0, 3, 2, 3, head)
    paint(cv, 46, 0, 1, 1, 1, lambda f, x, y, fw, fh: MOUTH[2] if f == 'front' else MOUTH[1])
    paint(cv, 50, 0, 1, 1, 2, lambda f, x, y, fw, fh: r[4] if f == 'front' else (r[3] if f == 'top' else r[2]))

    def thigh(face, x, y, fw, fh):
        if face == 'top':
            return r[5] if x == 0 else r[3]
        return r[2] if face != 'bottom' else r[1]

    paint(cv, 0, 20, 3, 1, 1, thigh)

    def shin(face, x, y, fw, fh):
        if face == 'top':
            return r[5]
        if face == 'bottom':
            return r[0]
        return r[4] if y == 0 else (r[2] if y < 3 else r[1])

    paint(cv, 8, 20, 1, 4, 1, shin)
    paint(cv, 12, 20, 2, 4, 1, shin)
    return cv


def tick_eyes():
    cv = Canvas(64, 64)
    # head front face starts at (37, 3), 3 x 2: two glinting eyes at the upper corners
    cv[37, 3] = EYE[2]
    cv[39, 3] = EYE[2]
    return cv


MOUTH = ramp('#3a1606', '#6e2c0c', '#a14714')


def elder_sheet(seed=9):
    """Elder tick: dark breccia armour plates studded with pale clasts, KREEP crystals, pale knee rings, dark orange mouth."""
    r = ELDER
    k = P.KREEP
    cv = Canvas(64, 64)
    n = clustered(64, 64, seed, passes=1)
    rng = random.Random(seed)

    def clasty(face, x, y, fw, fh, base, ox, oy):
        # breccia: dark matrix with angular pale clasts; plate tops lighter at the front edge
        if face == 'bottom':
            return noise_pick(r, 1, n, x + ox, y + oy)
        h = (x * 7 + y * 13 + ox * 3 + oy) % 11
        if h == 0:
            return r[6]
        if h == 5:
            return r[5]
        t = base + (1 if face == 'top' else 0) - (1 if face != 'top' and y == fh - 1 else 0)
        return noise_pick(r, max(1, t), n, x + ox, y + oy)

    paint(cv, 0, 0, 10, 4, 11, lambda f, x, y, fw, fh: clasty(f, x, y, fw, fh, 2, 0, 0))
    paint(cv, 0, 15, 11, 3, 4, lambda f, x, y, fw, fh: clasty(f, x, y, fw, fh, 3, 5, 17)
          if not (f == 'top' and y == 0) else r[5])
    paint(cv, 30, 15, 9, 2, 4, lambda f, x, y, fw, fh: clasty(f, x, y, fw, fh, 3, 11, 3)
          if not (f == 'top' and y == 0) else r[5])
    paint(cv, 0, 22, 9, 2, 4, lambda f, x, y, fw, fh: clasty(f, x, y, fw, fh, 2, 19, 7))

    def head(face, x, y, fw, fh):
        if face == 'top':
            return r[4] if y == 0 else clasty(face, x, y, fw, fh, 3, 2, 9)
        if face == 'front':
            return r[1] if y == fh - 1 else r[2]
        return r[2] if y == 0 else r[1]

    paint(cv, 26, 22, 6, 3, 3, head)
    # the mouth between the pincers: dark orange inside, darker rim
    paint(cv, 44, 22, 3, 1, 1, lambda f, x, y, fw, fh: MOUTH[2] if f == 'front' and x == 1 else MOUTH[1] if f == 'front' else MOUTH[0])

    def pincer(face, x, y, fw, fh):
        if face == 'top':
            return r[5] if x == 0 or y == 0 else r[4]
        if face == 'bottom':
            return r[1]
        return r[3] if y == 0 else r[2]

    paint(cv, 44, 0, 2, 2, 4, pincer)
    paint(cv, 56, 0, 1, 1, 2, lambda f, x, y, fw, fh: r[6] if f == 'front' else r[4])

    def crystal(face, x, y, fw, fh):
        if face in ('top', 'bottom'):
            return k[5] if face == 'top' else k[1]
        return k[4] if x == 0 else (k[2] if x == fw - 1 else k[3])

    paint(cv, 44, 8, 2, 4, 2, crystal)
    paint(cv, 52, 8, 1, 3, 1, crystal)

    def thigh(face, x, y, fw, fh):
        if face in ('right', 'left'):
            return r[2]
        if face == 'top':
            return r[5] if x in (0, 6) else r[3]
        return r[1] if face == 'bottom' else (r[4] if x in (0, 6) else r[2])

    paint(cv, 0, 28, 7, 2, 2, thigh)

    def shin(face, x, y, fw, fh):
        if face == 'top':
            return r[5]
        if face == 'bottom':
            return r[0]
        # pale ring at the knee, darker toward the clawed foot
        if y == 0:
            return r[5]
        return r[3] if y < 3 else r[2]

    paint(cv, 18, 28, 2, 5, 2, shin)
    paint(cv, 26, 28, 1, 6, 1, lambda f, x, y, fw, fh: r[0] if f == 'bottom' or y == fh - 1 else (r[4] if f == 'top' else r[1]))
    return cv


def elder_eyes():
    cv = Canvas(64, 64)
    # head front face starts at (29, 25), 6 x 3: four small amber eyes in a row above the mouth
    for x in (29, 30, 33, 34):
        cv[x, 25] = EYE[2] if x in (30, 33) else EYE[1]
    return cv


# --------------------------------------------------------------------------- dust swimmer

def dust_swimmer():
    """Dust-grey back with dark spots and a dark spine, pale fin edges and belly, a pale sifting slit, eyes on stalks."""
    r = DUST
    cv = Canvas(64, 64)
    n = clustered(64, 64, 17, passes=1)
    spots = {(1, 2), (6, 3), (1, 6), (6, 7), (1, 10), (6, 11), (2, 13), (5, 1)}

    def body(face, x, y, fw, fh):
        if face == 'top':
            t = 3
            if x in (0, fw - 1):
                t = 6  # pale flank line
            if (x, y) in spots:
                t = 0  # dark spots
            if x in (3, 4):
                t = 1  # dark spine stripe under the ridge
            return noise_pick(r, t, n, x, y)
        if face == 'bottom':
            return r[6] if (x + y) % 4 else r[5]
        t = 5 if y == 0 else (2 if y == 1 else 6)
        return noise_pick(r, t, n, x + 30, y + 40)

    paint(cv, 0, 0, 8, 3, 14, body)

    def head(face, x, y, fw, fh):
        if face == 'top':
            return r[6] if y == 0 else noise_pick(r, 3, n, x + 50, y)
        if face == 'front':
            return r[6] if y == 1 and 1 <= x <= 5 else r[3]  # wide pale sifting slit
        return r[3] if face != 'bottom' else r[6]

    paint(cv, 0, 17, 7, 2, 4, head)
    paint(cv, 22, 17, 1, 3, 1, lambda f, x, y, fw, fh: (P.ILMENITE[1] if y == 0 else r[2]) if f != 'top' else P.ILMENITE[5])

    def fin(face, x, y, fw, fh):
        if face == 'top':
            if y == 0 or y == fh - 1 or x == fw - 1:
                return r[6]  # pale edge all round
            return r[1] if x % 2 == 0 else r[3]  # rays
        if face == 'bottom':
            return r[6]
        return r[5]

    paint(cv, 0, 34, 7, 1, 8, fin)
    paint(cv, 30, 34, 3, 1, 5, fin)

    def plate(face, x, y, fw, fh):
        # dark ridge plates with a pale cutting edge on top and on the leading edge
        if face == 'top' or (face in ('right', 'left') and y == 0):
            return r[6]
        if face in ('right', 'left'):
            return r[1] if x > 0 else r[3]
        return r[1]

    paint(cv, 44, 0, 1, 4, 3, plate)
    paint(cv, 52, 0, 1, 3, 3, plate)
    paint(cv, 44, 8, 1, 2, 3, plate)

    def tail(face, x, y, fw, fh):
        if face == 'top':
            return r[4] if y % 2 else r[2]
        if face == 'bottom':
            return r[6]
        return r[2]

    paint(cv, 0, 25, 3, 2, 7, tail)
    paint(cv, 20, 25, 1, 5, 4, lambda f, x, y, fw, fh: r[6] if y == 0 or x == fw - 1 else (r[2] if x % 2 else r[1]))
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
    save(elder_sheet(), 'entity/moon', 'elder_regolith_tick')
    save(elder_eyes(), 'entity/moon', 'elder_regolith_tick_eyes')
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


# --------------------------------------------------------------------------- heat siphon

FROST = ramp('#1c2a36', '#2c4352', '#43626f', '#6a8c96', '#9dbcc2', '#d3e8ea')
CORE = ramp('#1d4f7a', '#3d86c4', '#7cc2f0', '#d2efff')


def heat_siphon():
    """Frost crystal: dark blue-grey inside, pale bright facet edges (reads as ice), a dark mouth, pale tendrils."""
    r = FROST
    c = CORE
    cv = Canvas(64, 64)

    def crystal(face, x, y, fw, fh):
        edge = x in (0, fw - 1) or y in (0, fh - 1)
        if face == 'bottom':
            return c[0] if not edge else r[2]
        if edge:
            return r[5] if face == 'top' or y == 0 else r[4]
        # inner facets: diagonal light streaks through dark ice
        if (x + y) % 4 == 0:
            return r[4]
        if (x - y) % 5 == 0:
            return r[3]
        return r[1] if (x * 3 + y) % 7 else r[2]

    paint(cv, 0, 0, 6, 5, 6, crystal)
    paint(cv, 0, 11, 6, 4, 6, crystal)
    paint(cv, 24, 0, 4, 2, 4, lambda f, x, y, fw, fh: c[1] if f == 'bottom' else (r[2] if y else r[3]))

    def head(face, x, y, fw, fh):
        if face == 'front':
            if y == 1 and x == 1:
                return MOUTH[1]  # the feeding mouth, dark orange inside
            if y == 1:
                return r[0]
            return r[3]
        return r[4] if face == 'top' else r[2]

    paint(cv, 40, 0, 3, 2, 2, head)

    def shard(face, x, y, fw, fh):
        if face in ('top', 'bottom'):
            return r[5]
        t = 5 if x == 0 else 4
        return r[t] if y < 2 else (c[1] if y == fh - 1 else r[t - 1])

    paint(cv, 48, 0, 2, 7, 2, shard)
    paint(cv, 56, 0, 2, 5, 2, shard)
    paint(cv, 0, 21, 1, 4, 1, lambda f, x, y, fw, fh: r[5] if y == 3 else (r[4] if y > 1 else r[3]))
    return cv


def heat_siphon_glow():
    """Glowing parts: the core seen through the belly and the facets, the shard tips, the eyes. Scaled by the renderer."""
    c = CORE
    cv = Canvas(64, 64)
    # belly (bottom of the core at (12, 0), 6 x 6) and the hanging core bulb under it
    for x in range(1, 5):
        for y in range(1, 5):
            cv[12 + x, y] = c[2] if (x + y) % 2 else c[1]
    for x in range(4):
        for y in range(4):
            cv[32 + x, y] = c[2]
    # a cold light deep in each side facet
    for u, v, w in ((0, 6, 6), (0, 17, 6)):
        for side in range(4):
            cv[u + side * 6 + 2, v + 2] = c[1]
            cv[u + side * 6 + 3, v + 2] = c[2]
    # shard tips: side faces start at v + d
    for u, h in ((48, 7), (56, 5)):
        for x in range(8):
            cv.put(u + x, 2 + h - 1, c[2])
            cv.put(u + x, 2 + h - 2, c[1])
        cv[u + 2, 0] = c[3]
        cv[u + 3, 0] = c[3]
    # eyes on the head front (42, 2), 3 x 2
    cv[42, 2] = c[3]
    cv[44, 2] = c[3]
    return cv


_export_moon_creatures = export


def export(save):  # noqa: F811 - extend the module export
    _export_moon_creatures(save)
    save(heat_siphon(), 'entity/moon', 'heat_siphon')
    save(heat_siphon_glow(), 'entity/moon', 'heat_siphon_glow')
