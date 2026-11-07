"""Planetary rover entity sheet (128x128), its headlight glow layer and the soft light-cone texture.
UVs mirror client/vehicles/RoverModel.java (vanilla cube layout, see moon_creatures.faces)."""
import math

from sbtex.core import Canvas, ramp, clustered
from sbtex import palettes as P
from modules.moon_creatures import faces

AL = P.ALUMINIUM
ST = P.STEEL
GOLD = P.GOLD
CELL = P.CELL
W = P.TUNGSTEN
FABRIC = ramp('#14202c', '#1d3042', '#284258', '#35556f', '#456a86', '#5a819c')
WHITE = ramp('#7e848c', '#a3a9b0', '#c6cbd1', '#e2e5e8', '#f4f5f6')
LENS = ramp('#2a2d32', '#5e646c', '#a8aeb5', '#dfe3e8')
LAMP = [(255, 240, 200), (255, 250, 228), (255, 255, 248)]
YELLOW = (222, 182, 48)


def paint(cv, u, v, w, h, d, fn):
    for name, (fx, fy, fw, fh) in faces(u, v, w, h, d).items():
        for y in range(fh):
            for x in range(fw):
                c = fn(name, x, y, fw, fh)
                if c is not None:
                    cv.put(fx + x, fy + y, c)


N = clustered(128, 128, 4711, passes=1)


def shade(r, face, base):
    return r[max(0, min(len(r) - 1, base + {'top': 1, 'bottom': -2, 'front': 0, 'back': -1, 'left': 0, 'right': 0}[face]))]


def chassis(face, x, y, fw, fh):
    if face == 'top':
        # riveted floor panels with a darker frame edge
        if x in (0, fw - 1) or y in (0, fh - 1):
            return AL[2]
        if y % 9 == 0:
            return AL[3]
        if (x % 7 == 3 and y % 9 == 4):
            return AL[6]
        return AL[5] if N[y][x] > 0.75 else AL[4]
    if face == 'bottom':
        return ST[2]
    if y == 0:
        return AL[5]
    if y == fh - 1:
        return AL[1]
    # side frame tube with lightening holes
    return AL[1] if (x % 6 == 3 and y == 1) else AL[3]


def seat(face, x, y, fw, fh):
    if face in ('top', 'front', 'back') and 0 < x < fw - 1:
        return FABRIC[3] if (x + y) % 3 else FABRIC[2]
    return AL[3] if face != 'bottom' else AL[1]


def seat_back(face, x, y, fw, fh):
    if face in ('front', 'back') and 0 < x < fw - 1 and 0 < y < fh - 1:
        return FABRIC[4] if (x * 2 + y) % 5 else FABRIC[2]
    return AL[3]


def console(face, x, y, fw, fh):
    if face == 'back':                      # faces the driver
        if y == 0 or y == fh - 1:
            return ST[2]
        if 1 <= y <= 2 and x in (2, 3):
            return (88, 200, 90)            # status lamps
        if 1 <= y <= 2 and x in (5, 6):
            return (230, 170, 50)
        if 1 <= y <= 3 and 8 <= x <= 11:
            return ST[0] if y == 2 and x == 9 else WHITE[2]  # attitude dial
        return ST[3]
    return shade(ST, face, 3)


def lamp_bar(face, x, y, fw, fh):
    if face == 'front' and y == 1 and x in (2, 3, 4, fw - 5, fw - 4, fw - 3):
        return LENS[3] if x in (3, fw - 4) else LENS[2]
    return shade(ST, face, 2)


def lamp_glow(face, x, y, fw, fh):
    if face == 'front' and y == 1 and x in (2, 3, 4, fw - 5, fw - 4, fw - 3):
        return LAMP[2] if x in (3, fw - 4) else LAMP[0]
    return None


def wheel(face, x, y, fw, fh):
    if face in ('left', 'right'):
        # wire-mesh wheel seen from the side: tread ring, mesh, hub
        cx, cy = (fw - 1) / 2, (fh - 1) / 2
        dd = math.hypot(x - cx, y - cy)
        if dd < 1.3:
            return AL[6]
        if dd < 2.2:
            return ST[2]
        if (x + y) % 2 == 0:
            return ST[4]
        return ST[3]
    # tread: titanium chevrons over the zinc mesh
    if (y + abs(x - (fw - 1) / 2) * 1.0) % 3 < 1:
        return AL[5]
    return ST[2] if (x + y) % 2 else ST[3]


def fender(face, x, y, fw, fh):
    if face == 'top':
        return WHITE[3] if 0 < x < fw - 1 else WHITE[1]
    if face == 'bottom':
        return WHITE[0]
    return WHITE[2]


def mast(face, x, y, fw, fh):
    return shade(AL, face, 3)


def dish(face, x, y, fw, fh):
    if face in ('front', 'back'):
        cx, cy = (fw - 1) / 2, (fh - 1) / 2
        dd = math.hypot(x - cx, y - cy)
        if dd > 3.9:
            return None if face == 'front' else WHITE[0]
        if dd < 0.8:
            return ST[2]
        return WHITE[4] if dd < 2.4 else WHITE[3]
    return WHITE[1]


def cargo(face, x, y, fw, fh):
    # gold foil thermal blanket with taped seams
    if face == 'top':
        if y in (0, fh - 1) or x in (0, fw - 1):
            return GOLD[3]
        return GOLD[5] if N[y + 60][x + 40] > 0.6 else GOLD[4]
    if face == 'bottom':
        return GOLD[2]
    if y == 0:
        return GOLD[5]
    if x % 7 == 0:
        return GOLD[2]
    return GOLD[4] if N[y + 70][x + 10] > 0.45 else GOLD[3]


def solar(face, x, y, fw, fh):
    if face == 'top':
        if x in (0, fw - 1) or y in (0, fh - 1):
            return AL[4]
        if x % 4 == 0 or y % 4 == 0:
            return CELL[2]
        return CELL[5] if (x + y) % 7 == 0 else CELL[4]
    if face == 'bottom':
        return AL[2]
    return AL[4]


def rtg(face, x, y, fw, fh):
    if face == 'top':
        return W[4] if 0 < x < fw - 1 and 0 < y < fh - 1 else AL[3]
    if face == 'bottom':
        return W[2]
    if face == 'back' and 1 <= x <= 3 and 2 <= y <= 4:
        return YELLOW if not (x == 2 and y == 3) else W[2]
    return (AL[5], AL[3], AL[1])[x % 3] if 0 < y < fh - 1 else W[3]


def hub(face, x, y, fw, fh):
    # hub motor cap: dark housing with a bright bolt circle
    if face in ('left', 'right'):
        return AL[6] if (x, y) in ((1, 1), (2, 2), (1, 2), (2, 1)) else ST[1]
    return ST[2]


def strut(face, x, y, fw, fh):
    return AL[5] if y % 2 == 0 else AL[3]


def bumper(face, x, y, fw, fh):
    if face == 'front':
        return YELLOW if (x // 2) % 2 == 0 else ST[1]
    return shade(ST, face, 2)


def sheet(glow=False):
    cv = Canvas(128, 128)
    if glow:
        paint(cv, 32, 49, 18, 3, 2, lamp_glow)
        # console lamps stay lit too
        def lamps(face, x, y, fw, fh):
            if face == 'back' and 1 <= y <= 2 and x in (2, 3):
                return (120, 240, 120)
            return None
        paint(cv, 32, 40, 14, 5, 3, lamps)
        return cv
    paint(cv, 0, 0, 22, 3, 36, chassis)
    paint(cv, 0, 40, 8, 2, 7, seat)
    paint(cv, 0, 50, 8, 8, 1, seat_back)
    paint(cv, 32, 40, 14, 5, 3, console)
    paint(cv, 32, 49, 18, 3, 2, lamp_bar)
    paint(cv, 0, 60, 4, 10, 10, wheel)
    paint(cv, 30, 60, 6, 1, 14, fender)
    paint(cv, 72, 40, 1, 14, 1, mast)
    paint(cv, 78, 40, 8, 8, 1, dish)
    paint(cv, 72, 56, 14, 5, 5, cargo)
    paint(cv, 0, 82, 24, 1, 16, solar)
    paint(cv, 82, 82, 5, 8, 5, rtg)
    paint(cv, 0, 100, 20, 2, 12, chassis)       # rear deck
    paint(cv, 66, 100, 1, 8, 1, mast)           # solar panel posts
    paint(cv, 72, 100, 1, 4, 4, hub)
    paint(cv, 84, 100, 1, 4, 1, strut)
    paint(cv, 90, 100, 1, 2, 7, seat)           # armrests
    paint(cv, 0, 114, 20, 2, 2, bumper)
    return cv


def beam():
    """Soft cone: bright and narrow at the lamp (v = 0), fading out along v and towards the edges."""
    cv = Canvas(16, 16)
    for y in range(16):
        for x in range(16):
            edge = 1 - abs(x - 7.5) / 8
            a = int(255 * (1 - y / 16) ** 1.5 * edge ** 0.8)
            cv[x, y] = (255, 255, 255, max(0, min(255, a)))
    return cv


def export(save):
    save(sheet(), 'entity', 'rover')
    save(sheet(glow=True), 'entity', 'rover_lights')
    save(beam(), 'entity', 'rover_beam')


def icon():
    """Side view: aluminium deck on two mesh wheels, a seat, the antenna dish and the gold cargo box."""
    cv = Canvas()
    for x in range(1, 15):
        cv[x, 9] = AL[5]
        cv[x, 10] = AL[2]
    for cx in (4, 11):
        for y in range(9, 16):
            for x in range(cx - 3, cx + 4):
                d = math.hypot(x - cx, y - 12.5)
                if d < 3.2:
                    cv[x, y] = ST[1] if d > 2.3 else (AL[5] if d < 0.9 else ST[3])
    for y in (6, 7, 8):
        cv[6, y] = FABRIC[3]
    for x in (6, 7, 8):
        cv[x, 8] = FABRIC[3]
    for y in (6, 7, 8):
        cv[12, y] = GOLD[4]
        cv[13, y] = GOLD[3]
    for y in range(2, 9):
        cv[14, y] = AL[3]
    for y in (1, 2, 3):
        cv[13, y] = AL[5]
    cv[1, 8] = LENS[3]
    return cv


_export_rover = export


def export(save):  # noqa: F811
    _export_rover(save)
    save(icon(), 'item', 'rover')
