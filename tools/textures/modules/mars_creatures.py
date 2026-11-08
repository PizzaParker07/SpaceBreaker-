"""Mars creature textures (entity sheets). UV layouts mirror the Java models in client/mars; the cube layout helpers
come from moon_creatures (vanilla box UV: top, bottom, then right/front/left/back below them)."""
import random

from sbtex.core import Canvas, ramp, clustered
from modules.moon_creatures import paint, noise_pick

BEETLE = ramp('#2a1410', '#4a2216', '#6b311c', '#8a4224', '#a5552e', '#bf6c3d', '#d48a55')
CHITIN = ramp('#141112', '#221d1d', '#302929', '#403737', '#524848')
GRAZER = ramp('#3a302b', '#4b3f38', '#5e5047', '#716157', '#857368', '#9a877a', '#ae9b8d')
PLATE = ramp('#4a2c20', '#5c3828', '#6e4532', '#80533c', '#916246', '#a27352')
LURKER = ramp('#4a2a1d', '#5c3424', '#6d3f2b', '#7e4b33', '#8e573b', '#9d6444', '#ab724e')
LURKER_BUFF = ramp('#86644a', '#a07e5c', '#b99670')
LURKER_BELLY = ramp('#2e2220', '#3d2e2a', '#4c3a35')
EYE = ramp('#6a3c0c', '#c47a1c', '#f5c040', '#fff0b0')
WORM = ramp('#5e6772', '#7a8591', '#98a2ad', '#b4bdc6', '#ced5db', '#e6eaee')
ICE = ramp('#9cc4dc', '#bcdcec', '#d8eef8', '#f2fbff')
ICE_CORE = ramp('#2c3846', '#3c4a5c', '#506276', '#66788c')
GULLET = ramp('#5a2a0e', '#7a3a12', '#9a4e1a')
ROCKS = ramp('#3d2a22', '#5a3e30', '#7a5a46', '#2e2624')
DUST = ramp('#6b4630', '#8a5c3e', '#a8744e', '#c08c62', '#d4a57c')


def beetle():
    """Rust Beetle: two domed rust-red wing cases with pitted rust spots, a long metallic sheen down each crown and a
    dark seam between them, a darker thorax shield, black chitin body, head, mandibles and legs."""
    cv = Canvas(64, 64)
    n = clustered(64, 64, 41, passes=1)
    rng = random.Random(42)
    pits = {(rng.randrange(0, 40), rng.randrange(0, 40)) for _ in range(60)}

    def pitted(t, x, y, ox, oy):
        if (x + ox, y + oy) in pits:
            return BEETLE[1]
        return noise_pick(BEETLE, t, n, x + ox, y + oy)

    def lower(face, x, y, fw, fh):
        # the broad lower shell of one case: a lit outer flank, the dark seam on its inner (left-hand UV) side
        if face == 'top':
            return BEETLE[1] if x == 0 else pitted(4 if x < fw - 1 else 3, x, y, 3, 1)
        if face == 'bottom':
            return CHITIN[1]
        if face in ('right', 'left'):
            return pitted(3 if y == 0 else 2, x, y, 11, 7)
        return BEETLE[2] if y == 0 else BEETLE[1]

    def crown(face, x, y, fw, fh):
        if face == 'top':
            if x == 1 and 0 < y < fh - 2 and n[y * 3 % 64][7] > 0.35:
                return BEETLE[5]  # a broken metallic sheen down the crown
            if y == fh - 1:
                return BEETLE[3]  # the crown rounds off toward the tail
            return pitted(4, x, y, 21, 3)
        if face == 'bottom':
            return BEETLE[2]
        return BEETLE[4] if face in ('right', 'left') else BEETLE[3]

    paint(cv, 0, 14, 4, 2, 10, lower)
    paint(cv, 0, 28, 3, 1, 9, crown)
    paint(cv, 0, 40, 2, 1, 7, crown)

    def body(face, x, y, fw, fh):
        if face == 'bottom':
            return CHITIN[1] if (y % 3) else CHITIN[2]
        return noise_pick(CHITIN, 2, n, x + 11, y + 20)

    paint(cv, 0, 0, 6, 3, 9, body)

    def thorax(face, x, y, fw, fh):
        if face == 'top':
            if y == fh - 1:
                return BEETLE[1]  # the rim where it meets the wing cases
            return BEETLE[4] if (x in (1, fw - 2) and y == 1) else pitted(3, x, y, 30, 20)
        if face == 'bottom':
            return CHITIN[1]
        return BEETLE[2] if y == 0 else BEETLE[1]

    paint(cv, 30, 0, 6, 2, 4, thorax)
    paint(cv, 46, 20, 4, 1, 3, lambda f, x, y, fw, fh: pitted(4 if f == 'top' else 3, x, y, 33, 25))

    def head(face, x, y, fw, fh):
        if face == 'front':
            if y == 0 and x in (0, fw - 1):
                return CHITIN[4]  # eyes catch the light
            return CHITIN[1] if y == fh - 1 else CHITIN[2]
        if face == 'top':
            return CHITIN[3] if y == 0 else CHITIN[2]
        return CHITIN[1]

    paint(cv, 30, 8, 4, 2, 3, head)
    paint(cv, 44, 8, 1, 1, 2, lambda f, x, y, fw, fh: CHITIN[3] if f == 'top' else CHITIN[1])
    paint(cv, 44, 12, 1, 1, 2, lambda f, x, y, fw, fh: CHITIN[4] if f == 'front' else (CHITIN[3] if f == 'top' else CHITIN[2]))
    paint(cv, 50, 8, 1, 1, 3, lambda f, x, y, fw, fh: CHITIN[3] if f in ('top', 'front') else CHITIN[1])
    paint(cv, 28, 28, 4, 1, 1, lambda f, x, y, fw, fh: CHITIN[3] if f == 'top' else CHITIN[2])
    paint(cv, 28, 31, 1, 6, 1, lambda f, x, y, fw, fh: CHITIN[0] if y == fh - 1 else (CHITIN[3] if f == 'front' and y < 2 else CHITIN[2]))
    return cv


HIDE = ramp('#4a3a2e', '#5e4a3a', '#735b47', '#896d55', '#9d7f63', '#b09272')
ROCK = ramp('#2f2a28', '#3f3834', '#514842', '#635850', '#76695f', '#897b6f')
RUST = ramp('#5a2a18', '#7a3a20', '#9a4e2a')
MOUTH = ramp('#3a1a0c', '#6a3010')


def grazer():
    """Stonegrazer: a sandy hide under shingled grey rock plates flecked with rust, a wedge head with small amber eyes
    and a wide dark scraper jaw with a pale cutting edge (the mouth inside dark orange)."""
    cv = Canvas(128, 64)
    n = clustered(128, 64, 51, passes=1)

    def body(face, x, y, fw, fh):
        if face == 'top':
            return noise_pick(ROCK, 2, n, x, y)
        if face == 'bottom':
            return noise_pick(HIDE, 1, n, x + 40, y)
        t = 4 if y < 2 else (3 if y < fh - 2 else 2)
        if x % 6 == 0 and 1 < y < fh - 1:
            t -= 1  # skin folds
        return noise_pick(HIDE, t, n, x + 7, y + 22)

    paint(cv, 0, 0, 18, 9, 10, body)
    paint(cv, 56, 0, 14, 7, 12, body)

    def plates(face, x, y, fw, fh):
        if face == 'top':
            if y == 0:
                return ROCK[5]  # lit leading edge
            if (x * 7 + y * 3) % 11 == 0:
                return RUST[2]
            return noise_pick(ROCK, 3, n, x + 3, y + 5)
        if face == 'bottom':
            return ROCK[0]
        if y == 0:
            return ROCK[4]
        return RUST[1] if (x + y) % 5 == 0 else ROCK[2]

    paint(cv, 0, 20, 20, 2, 5, plates)

    def head(face, x, y, fw, fh):
        if face == 'front':
            if y == 1 and x in (1, fw - 2):
                return EYE[2]
            if y == 1 and x in (2, fw - 3):
                return EYE[1]
            return noise_pick(HIDE, 3 if y < 2 else 2, n, x + 90, y)
        if face == 'top':
            return ROCK[3] if y % 2 == 0 else ROCK[2]
        return noise_pick(HIDE, 2, n, x + 60, y + 40)

    paint(cv, 0, 28, 10, 6, 6, head)

    def jaw(face, x, y, fw, fh):
        if face == 'top':
            return MOUTH[1] if y > 0 else ROCK[5]  # inside the mouth, the cutting edge at the front
        if face == 'front':
            return ROCK[5] if y == 0 else ROCK[1]
        return ROCK[1] if y else ROCK[2]

    paint(cv, 32, 28, 12, 2, 5, jaw)
    paint(cv, 66, 28, 4, 3, 5, lambda f, x, y, fw, fh: ROCK[3] if f == 'top' else noise_pick(HIDE, 2, n, x + 110, y + 50))
    paint(cv, 104, 28, 6, 5, 4, lambda f, x, y, fw, fh: noise_pick(HIDE, 2 if f != 'top' else 3, n, x + 70, y + 30))  # neck

    def leg(face, x, y, fw, fh):
        if y >= fh - 1:
            return ROCK[1]  # horny foot rim
        return noise_pick(HIDE, 3 if face in ('front', 'left') else 2, n, x + 100, y + 10)

    paint(cv, 84, 28, 5, 7, 5, leg)
    return cv


def lurker():
    """Canyon Lurker: a stepped carapace banded like the canyon walls (rust beds, pale buff ledges, dark seams), mottled
    rock on the treads, knobby crest stones; only the amber eyes give it away."""
    cv = Canvas(128, 64)
    n = clustered(64, 64, 61, passes=1)
    m = clustered(64, 64, 62, passes=2)

    def tread(face, x, y, fw, fh, ox):
        """Top of a tier: mottled rock, a pale worn ledge on the rim, a few dark pits."""
        if (x == 0 or y == 0 or x == fw - 1 or y == fh - 1) and n[(y + ox) % 64][(x * 2 + ox) % 64] > 0.45:
            return noise_pick(LURKER_BUFF, 0, n, x + ox, y)
        if m[(y + ox) % 64][(x * 3) % 64] > 0.83:
            return LURKER[1]
        return noise_pick(LURKER, 4 if m[y % 64][(x + ox) % 64] > 0.5 else 3, n, x + ox, y + 7)

    def riser(beds):
        """Side of a tier: one bed per row, the beds wavering a little so they read as rock, not planks."""
        def fn(face, x, y, fw, fh):
            wobble = 1 if n[(x * 2) % 64][(y * 5 + 3) % 64] > 0.7 else 0
            r, t = beds[min(len(beds) - 1, y + wobble if y + wobble < fh else y)]
            if r is LURKER and m[(x * 2) % 64][(y * 9) % 64] < 0.18:
                return LURKER[1]  # a dark seam where the bed has weathered back
            return noise_pick(r, t, n, x + 11 * y, y + 20)
        return fn

    belly = lambda f, x, y, fw, fh: noise_pick(LURKER_BELLY, 1, n, x, y + 40)
    base_side = riser([(LURKER, 4), (LURKER_BUFF, 0), (LURKER, 2)])
    mid_side = riser([(LURKER, 4), (LURKER_BUFF, 0)])
    crest_side = riser([(LURKER, 5), (LURKER_BUFF, 1)])

    def tier(side, ox):
        def fn(face, x, y, fw, fh):
            if face == 'top':
                return tread(face, x, y, fw, fh, ox)
            if face == 'bottom':
                return belly(face, x, y, fw, fh)
            return side(face, x, y, fw, fh)
        return fn

    paint(cv, 0, 0, 14, 3, 16, tier(base_side, 0))
    paint(cv, 0, 20, 11, 2, 12, tier(mid_side, 17))
    paint(cv, 0, 35, 7, 2, 8, tier(crest_side, 33))
    paint(cv, 32, 35, 2, 2, 2, lambda f, x, y, fw, fh: noise_pick(LURKER, 5 if f == 'top' else 3, n, x + 50, y + 3))

    def head(face, x, y, fw, fh):
        if face == 'front':
            if y == 0 and x in (1, fw - 2):
                return EYE[2]
            if y == fh - 1:
                return LURKER_BELLY[0]  # the dark mouth slit between the mandibles
            return noise_pick(LURKER, 2, n, x + 50, y)
        if face == 'top':
            return LURKER_BUFF[1] if y == fh - 1 else noise_pick(LURKER, 4, n, x + 40, y + 50)
        return noise_pick(LURKER, 2, n, x + 40, y + 50)

    paint(cv, 64, 0, 6, 3, 4, head)
    paint(cv, 64, 8, 1, 1, 3, lambda f, x, y, fw, fh: LURKER_BELLY[0] if f == 'front' else (LURKER[2] if f in ('top', 'back') else LURKER[1]))

    def segment(face, x, y, fw, fh):
        if face in ('front', 'back'):
            return LURKER[1]
        if face in ('left', 'right') and x == 0:
            return LURKER_BELLY[2]  # the joint ring at the elbow end
        return noise_pick(LURKER, 4 if face == 'top' else 2, n, x + 7 * y, y + 44)

    paint(cv, 64, 13, 2, 2, 6, segment)
    paint(cv, 64, 22, 2, 2, 5, segment)
    paint(cv, 84, 0, 3, 3, 3, lambda f, x, y, fw, fh: noise_pick(LURKER, 5 if f == 'top' else 3, n, x + 30, y + 58))

    def finger(face, x, y, fw, fh):
        if face == 'front' or (face in ('left', 'right', 'top', 'bottom') and x == 0):
            return LURKER_BELLY[0]  # dark horn tip
        return LURKER_BUFF[1] if face == 'top' else LURKER[3]

    paint(cv, 84, 7, 1, 1, 3, finger)
    paint(cv, 96, 0, 5, 1, 1, lambda f, x, y, fw, fh: LURKER[4] if f == 'top' else LURKER[2])
    paint(cv, 96, 3, 1, 7, 1, lambda f, x, y, fw, fh: LURKER_BELLY[0] if y == fh - 1 else (LURKER_BUFF[0] if y == 0 else LURKER[2 if f in ('front', 'left') else 1]))
    return cv


def lurker_eyes():
    cv = Canvas(128, 64)
    # eyes on the head's front face: head texOffs (64, 0), size 6x3x4 -> front at (68, 4)
    cv[69, 4] = EYE[3]
    cv[72, 4] = EYE[3]
    return cv


def worm():
    """Polar Worm: clear ice rings (translucent shells with bright frosted rims) round a dark slate core, a blunt
    frost-white head whose round maw is ringed with ice teeth around a dark orange gullet."""
    cv = Canvas(128, 64)
    n = clustered(64, 64, 71, passes=1)

    def clear(c, a):
        return (c[0], c[1], c[2], a)

    def head(face, x, y, fw, fh):
        if face == 'front':
            cx, cy = (fw - 1) / 2, (fh - 1) / 2
            d = ((x - cx) ** 2 + (y - cy) ** 2) ** 0.5
            if d < 1.2:
                return GULLET[1] if d < 0.8 else GULLET[0]
            if d < 2.0:
                return ICE_CORE[1]  # the dark lip the teeth stand on
            return WORM[4] if y == 0 else (WORM[2] if y == fh - 1 else WORM[3])
        if face == 'top':
            return noise_pick(WORM, 5 if y < fh - 1 else 4, n, x, y)
        if face == 'bottom':
            return WORM[1]
        return noise_pick(WORM, 4 if y < 2 else 3, n, x + 9, y)

    paint(cv, 0, 0, 6, 5, 5, head)
    paint(cv, 24, 0, 1, 1, 2, lambda f, x, y, fw, fh: ICE[3] if f == 'front' else (ICE[2] if f == 'top' else ICE[1]))
    paint(cv, 70, 0, 1, 2, 2, lambda f, x, y, fw, fh: clear(ICE[3] if f == 'top' or y == 0 else ICE[2], 225))
    paint(cv, 70, 12, 1, 1, 3, lambda f, x, y, fw, fh: clear(ICE[3] if f in ('back', 'top') else ICE[2], 235))

    def shell(face, x, y, fw, fh):
        frost = n[(y * 5 + x) % 64][(x * 3 + 17) % 64]
        if face in ('front', 'back'):
            # the ring's end faces: a bright frosted rim, clear in the middle
            if x == 0 or y == 0 or x == fw - 1 or y == fh - 1:
                return clear(ICE[3], 215)
            return clear(ICE[1], 60)
        # the long faces stay clear so the core shows; only a speck of frost here and there
        if frost > 0.8:
            return clear(ICE[3], 150)
        return clear(ICE[2] if face == 'top' else ICE[1], 70)

    for u, v, w, h in ((0, 12, 6, 5), (20, 12, 5, 4), (40, 12, 4, 3), (56, 12, 3, 2)):
        paint(cv, u, v, w, h, 3, shell)

    def core(face, x, y, fw, fh):
        return noise_pick(ICE_CORE, 1 if face == 'top' else 0, n, x + 40, y + 13)

    for u, v, w, h in ((0, 24, 4, 3), (20, 24, 3, 2), (40, 24, 2, 1), (56, 24, 1, 1)):
        paint(cv, u, v, w, h, 4, core)
    return cv


def dust_devil():
    """Dust Devil: translucent dust streaked on a slant by the spin. Rows 0-24 feed the funnel rings and fade from
    dense (the narrow foot samples the upper rows) to thin (the wide top samples the lower ones); rows 32-47 are the
    faint ground skirt, rows 48-63 the dense core; a few opaque pebbles sit at (100, 40)."""
    cv = Canvas(128, 64)
    rng = random.Random(81)
    n = clustered(64, 64, 82, passes=1)

    def streak_px(x, y, density):
        k = (x + y * 3 + int(n[y % 64][x % 64] * 7)) % 11
        if k in (0, 1):
            c, a = DUST[3], 215
        elif k in (2, 3, 4):
            c, a = DUST[2], 160
        elif k == 6:
            c, a = DUST[1], 120
        else:
            c, a = DUST[0], 0 if n[y % 64][x % 64] < 0.5 else 60
        if rng.random() < 0.03:
            c, a = DUST[4], 235
        return (c[0], c[1], c[2], int(a * density))

    for y in range(64):
        for x in range(128):
            if y < 32:
                density = max(0.55, 1.05 - max(0, y - 3) * 0.028)
            elif y < 48:
                density = 0.45
            else:
                density = 1.15
            px = streak_px(x, y, density)
            cv[x, y] = (px[0], px[1], px[2], min(255, px[3]))
    for x in range(100, 112):
        for y in range(40, 42):
            c = ROCKS[(x * 7 + y * 3) % len(ROCKS)]
            cv[x, y] = (c[0], c[1], c[2], 255)
    return cv


def export(save):
    save(beetle(), 'entity', 'mars/rust_beetle')
    save(grazer(), 'entity', 'mars/stonegrazer')
    save(lurker(), 'entity', 'mars/canyon_lurker')
    save(lurker_eyes(), 'entity', 'mars/canyon_lurker_eyes')
    save(worm(), 'entity', 'mars/polar_worm')
    save(dust_devil(), 'entity', 'mars/dust_devil')
