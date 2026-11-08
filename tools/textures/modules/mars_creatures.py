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
LURKER_BELLY = ramp('#2e2220', '#3d2e2a', '#4c3a35')
EYE = ramp('#6a3c0c', '#c47a1c', '#f5c040', '#fff0b0')
WORM = ramp('#5e6772', '#7a8591', '#98a2ad', '#b4bdc6', '#ced5db', '#e6eaee')
MAW = ramp('#2a1c20', '#45303a', '#6a4a56')
DUST = ramp('#6b4630', '#8a5c3e', '#a8744e', '#c08c62', '#d4a57c')


def beetle():
    """Rust Beetle: domed rust-red wing cases with darker pits and a pale sheen, black chitin head and legs."""
    cv = Canvas(64, 64)
    n = clustered(64, 64, 41, passes=1)
    rng = random.Random(42)
    spots = {(rng.randrange(1, 7), rng.randrange(1, 9)) for _ in range(6)}

    def shell(face, x, y, fw, fh):
        if face == 'top':
            if x in (3,):  # the seam between the wing cases
                return BEETLE[1]
            if (x, y) in spots:
                return BEETLE[2]
            t = 5 if x in (1, 5) and 1 <= y <= 6 else 4  # long highlight on each dome
            if y == 0 or y == fh - 1 or x == 0 or x == fw - 1:
                t = 3
            return noise_pick(BEETLE, t, n, x, y)
        if face == 'bottom':
            return CHITIN[1]
        return noise_pick(BEETLE, 3 if y == 0 else 2, n, x + 30, y)

    paint(cv, 0, 14, 7, 2, 9, shell)

    def body(face, x, y, fw, fh):
        if face == 'bottom':
            return CHITIN[1] if (y % 3) else CHITIN[2]
        if face == 'top':
            return BEETLE[2]
        return noise_pick(BEETLE, 2 if y < 2 else 1, n, x + 11, y + 20)

    paint(cv, 0, 0, 8, 4, 10, body)

    def head(face, x, y, fw, fh):
        if face == 'front':
            if y == 0 and x in (0, fw - 1):
                return CHITIN[4]  # eyes catch the light
            return CHITIN[1] if y == fh - 1 else CHITIN[2]
        if face == 'top':
            return CHITIN[3] if y == 0 else CHITIN[2]
        return CHITIN[1]

    paint(cv, 36, 0, 4, 3, 3, head)
    paint(cv, 36, 8, 1, 1, 2, lambda f, x, y, fw, fh: CHITIN[3] if f in ('top', 'front') else CHITIN[1])

    def leg(face, x, y, fw, fh):
        if face == 'top':
            return CHITIN[3] if x in (1, 3) else CHITIN[2]
        return CHITIN[1] if x in (1, 3) else CHITIN[2]

    paint(cv, 0, 28, 1, 1, 5, leg)
    paint(cv, 14, 28, 5, 1, 1, leg)
    paint(cv, 14, 32, 1, 3, 1, lambda f, x, y, fw, fh: CHITIN[0] if y == fh - 1 else (CHITIN[3] if f == 'front' else CHITIN[2]))
    paint(cv, 44, 8, 1, 1, 4, lambda f, x, y, fw, fh: CHITIN[4] if f == 'front' else (CHITIN[3] if f == 'top' and x == 0 else CHITIN[2]))
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

    paint(cv, 0, 0, 16, 8, 22, body)

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

    paint(cv, 0, 32, 17, 2, 5, plates)

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

    paint(cv, 80, 0, 10, 6, 6, head)

    def jaw(face, x, y, fw, fh):
        if face == 'top':
            return MOUTH[1] if y > 0 else ROCK[5]  # inside the mouth, the cutting edge at the front
        if face == 'front':
            return ROCK[5] if y == 0 else ROCK[1]
        return ROCK[1] if y else ROCK[2]

    paint(cv, 80, 14, 12, 2, 5, jaw)
    paint(cv, 80, 24, 4, 3, 5, lambda f, x, y, fw, fh: ROCK[3] if f == 'top' else noise_pick(HIDE, 2, n, x + 110, y + 50))

    def leg(face, x, y, fw, fh):
        if y >= fh - 1:
            return ROCK[1]  # horny foot rim
        return noise_pick(HIDE, 3 if face in ('front', 'left') else 2, n, x + 100, y + 10)

    paint(cv, 100, 24, 5, 6, 5, leg)
    return cv


def lurker():
    """Canyon Lurker: its carapace is layered like the canyon walls; only the amber eyes give it away."""
    cv = Canvas(64, 64)
    n = clustered(64, 64, 61, passes=1)
    beds = [5, 4, 4, 3, 3, 2, 4, 4, 3, 2, 2, 3, 4, 3]

    def body(face, x, y, fw, fh):
        if face == 'top':
            return noise_pick(LURKER, beds[y % len(beds)] + (1 if y % 7 == 0 else 0), n, x, y)
        if face == 'bottom':
            return LURKER_BELLY[1]
        return noise_pick(LURKER, beds[(y * 2 + x // 5) % len(beds)] - 1, n, x + 20, y + 30)

    paint(cv, 0, 0, 12, 5, 14, body)

    def head(face, x, y, fw, fh):
        if face == 'front':
            if y == 1 and x in (1, fw - 2):
                return EYE[2]
            if y == fh - 1:
                return LURKER_BELLY[0]
            return noise_pick(LURKER, 3, n, x + 50, y)
        return noise_pick(LURKER, 4 if face == 'top' else 2, n, x + 40, y + 50)

    paint(cv, 0, 20, 6, 4, 4, head)
    paint(cv, 24, 20, 2, 2, 8, lambda f, x, y, fw, fh: noise_pick(LURKER, 4 if f == 'top' else 2, n, x, y + 44))

    def claw(face, x, y, fw, fh):
        if face == 'front':
            return LURKER[1] if x == 1 else LURKER[5]
        return noise_pick(LURKER, 5 if face == 'top' else 3, n, x + 30, y + 58)

    paint(cv, 44, 20, 3, 3, 4, claw)
    paint(cv, 0, 30, 6, 1, 1, lambda f, x, y, fw, fh: LURKER[3] if f == "top" else LURKER[1])
    paint(cv, 0, 34, 1, 3, 1, lambda f, x, y, fw, fh: LURKER_BELLY[0] if y == fh - 1 else (LURKER[3] if f == 'front' else LURKER[2]))
    return cv


def lurker_eyes():
    cv = Canvas(64, 64)
    # eyes on the head's front face: head texOffs (0, 20), size 6x4x4 -> front at (4, 24)
    cv[5, 25] = EYE[3]
    cv[8, 25] = EYE[3]
    return cv


def worm():
    """Polar Worm: pale, frost-bristled ringed body; a round maw ringed with ice-white teeth."""
    cv = Canvas(64, 64)
    n = clustered(64, 64, 71, passes=1)

    def head(face, x, y, fw, fh):
        if face == 'front':
            cx, cy = (fw - 1) / 2, (fh - 1) / 2
            d = max(abs(x - cx), abs(y - cy))
            if d < 1:
                return MAW[0]
            if d < 1.6:
                return MAW[2] if (x + y) % 2 == 0 else MAW[1]
            return WORM[4] if y == 0 else (WORM[2] if y == fh - 1 else WORM[3])
        return noise_pick(WORM, 3 if face == 'top' else 2, n, x, y)

    paint(cv, 0, 0, 6, 6, 6, head)

    def seg(face, x, y, fw, fh):
        if face in ('top', 'bottom'):
            return noise_pick(WORM, 3 if face == 'top' else 1, n, x + 20, y)
        if y == 0:
            return WORM[4]  # ring edge lit
        if y == fh - 1:
            return WORM[1]
        if (x + y) % 4 == 0:
            return WORM[5]  # frost bristle
        return noise_pick(WORM, 3, n, x + 30, y + 20)

    paint(cv, 0, 14, 5, 5, 5, seg)
    paint(cv, 24, 14, 4, 4, 4, seg)
    paint(cv, 44, 14, 3, 3, 3, seg)
    return cv


def dust_devil():
    """Dust Devil: translucent swirling dust, streaks slanted by the spin, denser at the core."""
    cv = Canvas(64, 64)
    rng = random.Random(81)
    n = clustered(64, 64, 82, passes=1)
    for y in range(64):
        for x in range(64):
            streak = (x + y * 2 + int(n[y][x] * 6)) % 9
            if streak in (0, 1):
                c, a = DUST[3], 210
            elif streak in (2, 3):
                c, a = DUST[2], 150
            elif streak == 5:
                c, a = DUST[1], 110
            else:
                c, a = DUST[0], 0 if n[y][x] < 0.45 else 70
            if rng.random() < 0.04:
                c, a = DUST[4], 230
            cv[x, y] = (c[0], c[1], c[2], a)
    return cv


def export(save):
    save(beetle(), 'entity', 'mars/rust_beetle')
    save(grazer(), 'entity', 'mars/stonegrazer')
    save(lurker(), 'entity', 'mars/canyon_lurker')
    save(lurker_eyes(), 'entity', 'mars/canyon_lurker_eyes')
    save(worm(), 'entity', 'mars/polar_worm')
    save(dust_devil(), 'entity', 'mars/dust_devil')
