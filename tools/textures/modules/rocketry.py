"""Rocketry textures: rocket part model sheets (orthographic views, see tools/models/modules/rocketry.py)
and their 16x16 inventory icons.

Sheet coordinates: in a side view, column c (0..31) is the horizontal position across the 32 px body
(c = 16 is just right of the centre) and model height y maps to row 31 - y.
"""
import math
import os
import random
import sys

from sbtex.core import Canvas, ramp, clustered, pick, TRANSPARENT
from sbtex import palettes as P

_MODELS = os.path.abspath(os.path.join(os.path.dirname(__file__), '..', '..', 'models'))
if _MODELS not in sys.path:
    sys.path.insert(0, _MODELS)

# --------------------------------------------------------------------------- materials (rocketry only)
WHITE = ramp('#5f636a', '#7f838a', '#a0a4aa', '#bdc0c5', '#d2d4d8', '#e1e2e5', '#ececee', '#f6f6f7')
CARBON = ramp('#121315', '#1a1b1e', '#232528', '#2d2f33', '#383b3f', '#45484d', '#55585e')
ABLATOR = ramp('#17100c', '#251912', '#36251a', '#463122', '#583f2b', '#6d4f37', '#836246')
COPPER = ramp('#1e130f', '#311f18', '#472d20', '#5f3d29', '#784e33', '#93633f', '#ad7b51')
NIOBIUM = ramp('#2b292d', '#3e3b41', '#545057', '#6b666e', '#837d86', '#9d979f', '#b8b2b9')
HAZARD = ramp('#4a3a06', '#7a610c', '#a88714', '#cfa823', '#e8c443')
RED = ramp('#3d0f0d', '#5e1613', '#82201a', '#a42c22', '#c03b2c', '#d65a48')
STEEL, ALU, GLASS, RUBBER, GOLD = P.STEEL, P.ALUMINIUM, P.VISOR_GLASS, P.RUBBER, P.GOLD
INNER = ramp('#0b0909', '#140f0d', '#1d1512', '#2a1d17')

SIDE, TOP, BOTTOM, AUX = (0, 0), (32, 0), (32, 32), (0, 32)


def new_sheet():
    return Canvas(64, 64)


def col_shade(c, w=32):
    """Cylinder shading for column c of a body w px wide: 1 = highlight (left of centre), 0 = shadow edge."""
    t = (c + 0.5 - w / 2) / (w / 2)          # -1 .. 1
    v = 1.0 - 0.75 * (t + 0.3) ** 2
    return max(0.0, min(1.0, v))


def radius_at(c, r):
    """True if column c of the 32-wide view is within a body of radius r."""
    return 16 - r <= c < 16 + r


def put_side(cv, region, c, y, color):
    ox, oy = region
    if 0 <= c < 32 and 0 <= y < 32:
        cv[ox + c, oy + 31 - y] = color


def top_view(cv, region, fn):
    """fn(dx, dz, dist) -> colour for a top/bottom view; dx/dz from the centre in px."""
    ox, oy = region
    for row in range(32):
        for c in range(32):
            dx, dz = c + 0.5 - 16, row + 0.5 - 16
            col = fn(dx, dz, math.hypot(dx, dz))
            if col is not None:
                cv[ox + c, oy + row] = col


def paint_body(cv, region, y0, y1, r, ramp_, lo, hi, seed, grain=0.12, rows=None):
    """Shaded cylindrical body between heights y0..y1 using ramp indices lo..hi."""
    n = clustered(32, 32, seed, passes=1)
    for y in range(y0, y1):
        for c in range(32):
            if not radius_at(c, r + 1):
                continue
            v = col_shade(c - (16 - r), 2 * r) if r < 16 else col_shade(c)
            v += (n[y % 32][c] - 0.5) * grain
            idx = lo + (hi - lo) * max(0.0, min(1.0, v))
            put_side(cv, region, c, y, ramp_[int(round(idx))])


def ring(cv, region, y, r, ramp_, lo, hi):
    """A seam/band row in a darker or lighter tone, still cylinder-shaded."""
    for c in range(32):
        if radius_at(c, r + 1):
            v = col_shade(c - (16 - r), 2 * r) if r < 16 else col_shade(c)
            put_side(cv, region, c, y, ramp_[int(round(lo + (hi - lo) * v))])


def bulkhead(cv, region, dark=False, port=True, seed=5):
    """Tank dome seen from above/below: aluminium with weld rings and a centre port."""
    rng = random.Random(seed)

    def fn(dx, dz, d):
        if d > 16.2:
            return None
        if d > 14.5:
            return WHITE[3] if not dark else WHITE[2]
        if 9.5 < d < 10.5:
            return ALU[2]
        if port and d < 2.5:
            return STEEL[2] if d < 1.5 else STEEL[5]
        base = 4 if (dx + dz) < 0 else 3
        if dark:
            base -= 1
        if rng.random() < 0.1:
            base -= 1
        return ALU[base]
    top_view(cv, region, fn)


# --------------------------------------------------------------------------- tanks

def fuel_tank(h, style, seed):
    cv = new_sheet()
    paint_body(cv, SIDE, 0, h, 16, WHITE, 3, 7, seed)
    # weld rings at both ends and the cable raceway
    ring(cv, SIDE, 0, 16, ALU, 1, 3)
    ring(cv, SIDE, h - 1, 16, ALU, 3, 5)
    if h >= 24:
        ring(cv, SIDE, h // 2, 16, WHITE, 2, 4)  # common bulkhead between RP-1 and LOX
    for y in range(1, h - 1):
        put_side(cv, SIDE, 22, y, WHITE[3])
    if style == 'band':
        for y in range(h // 2 + 2, h // 2 + 5):
            ring(cv, SIDE, y, 16, CARBON, 2, 5)
    elif style == 'collar':
        # dark forward collar and a thin band low on the tank
        for y in range(h - 7, h - 1):
            ring(cv, SIDE, y, 16, CARBON, 2, 5)
        ring(cv, SIDE, 4, 16, CARBON, 2, 5)
    else:
        # LOX fill/vent port near the top
        for y in (h - 5, h - 4):
            for c in (6, 7):
                put_side(cv, SIDE, c, y, STEEL[5] if y == h - 4 else STEEL[3])
    bulkhead(cv, TOP, seed=seed)
    bulkhead(cv, BOTTOM, dark=True, seed=seed + 1)
    return cv


def fuel_tank_insulated(h, seed):
    """A tank wrapped in multi-layer insulation: crinkled gold foil blankets, silver seams and tie-down bands."""
    cv = new_sheet()
    rng = random.Random(seed)
    paint_body(cv, SIDE, 0, h, 16, GOLD, 2, 6, seed, grain=0.35)       # crinkled foil catches the light unevenly
    for y in range(h):
        for c in range(32):
            if rng.random() < 0.08:
                put_side(cv, SIDE, c, y, GOLD[6] if rng.random() < 0.5 else GOLD[1])
    for c in range(3, 32, 8):                                           # blanket seams, silver tape
        for y in range(h):
            put_side(cv, SIDE, c, y, ALU[4] if y % 2 else ALU[3])
    for y in (1, h // 2, h - 2):                                        # tie-down bands
        ring(cv, SIDE, y, 16, ALU, 2, 4)
    ring(cv, SIDE, 0, 16, ALU, 1, 3)
    ring(cv, SIDE, h - 1, 16, ALU, 3, 5)
    bulkhead(cv, TOP, seed=seed)
    bulkhead(cv, BOTTOM, dark=True, seed=seed + 1)
    return cv


# --------------------------------------------------------------------------- engines

def thrust_structure(cv, y0, y1, seed):
    paint_body(cv, SIDE, y0, y1, 16, CARBON, 1, 5, seed)
    ring(cv, SIDE, y1 - 1, 16, STEEL, 3, 6)
    for c in range(2, 30, 5):
        put_side(cv, SIDE, c, y0 + 1, STEEL[5])     # bolts on the skirt


def bell(cv, y0, y1, r_of_y, ramp_, lo, hi, seed, lip=None):
    n = clustered(32, 32, seed, passes=1)
    for y in range(y0, y1):
        r = r_of_y(y)
        for c in range(32):
            if not radius_at(c, r):
                continue
            v = col_shade(c - (16 - r), 2 * r) + (n[y][c] - 0.5) * 0.1
            put_side(cv, SIDE, c, y, ramp_[int(round(lo + (hi - lo) * max(0, min(1, v))))])
    if lip is not None:
        r = r_of_y(y0)
        for c in range(32):
            if radius_at(c, r):
                put_side(cv, SIDE, c, y0, lip[int(round(1 + 3 * col_shade(c - (16 - r), 2 * r)))])


def nozzle_exit(cv, region, r_exit, r_throat):
    """Looking up into the bell: dark soot inside, a bright lip, the injector glow far inside."""
    def fn(dx, dz, d):
        if d > r_exit + 0.2:
            return None
        if d > r_exit - 1:
            return COPPER[4]
        if d < r_throat * 0.6:
            return INNER[3]
        return INNER[0] if d > r_exit * 0.6 else INNER[1]
    top_view(cv, region, fn)


def engine_top(cv, region, seed):
    rng = random.Random(seed)

    def fn(dx, dz, d):
        if d > 16.2:
            return None
        if d > 14.5:
            return CARBON[4]
        if d < 4:
            return STEEL[4] if d < 3 else STEEL[2]
        if abs(dx) < 1 or abs(dz) < 1:
            return CARBON[1]                      # thrust frame beams
        return CARBON[2 + (rng.random() < 0.2)]
    top_view(cv, region, fn)


def turbopump(cv, ys, seed):
    """Aux view: turbopump housing with a pipe and the gas generator exhaust (only some columns are used)."""
    n = clustered(32, 32, seed, passes=1)
    for y in range(32):
        for c in range(32):
            v = 0.5 + (n[y][c] - 0.5) * 0.4
            put_side(cv, AUX, c, y, STEEL[int(round(2 + 3 * v))])
    for y in ys:
        for c in range(32):
            put_side(cv, AUX, c, y, STEEL[6])     # flange rings


def kerosene_engine():
    cv = new_sheet()
    thrust_structure(cv, 16, 20, 31)
    bell(cv, 13, 16, lambda y: 7, STEEL, 2, 6, 32)           # thrust cone + gimbal
    bell(cv, 12, 13, lambda y: 4, STEEL, 1, 4, 33)
    radii = {10: 5, 11: 5, 7: 7, 8: 7, 9: 7, 3: 8, 4: 8, 5: 8, 6: 8, 0: 9, 1: 9, 2: 9}
    bell(cv, 0, 12, lambda y: radii[y], COPPER, 1, 6, 34, lip=COPPER)
    for y in range(1, 12):
        put_side(cv, SIDE, 16 - radii[y], y, COPPER[1])     # dark coolant tubes at the silhouette
    for y in (4, 8):
        ring(cv, SIDE, y, radii[y], COPPER, 3, 6)            # stiffener bands
    turbopump(cv, (9, 14), 35)
    engine_top(cv, TOP, 36)
    nozzle_exit(cv, BOTTOM, 9, 4)
    # underside of the skirt around the engine (bottom view also shows the skirt ring)
    ox, oy = BOTTOM
    for row in range(32):
        for c in range(32):
            d = math.hypot(c + 0.5 - 16, row + 0.5 - 16)
            if 9.5 < d <= 16.2:
                cv[ox + c, oy + row] = CARBON[1] if d < 15 else CARBON[3]
    return cv


def vacuum_engine():
    cv = new_sheet()
    thrust_structure(cv, 24, 28, 41)
    bell(cv, 21, 24, lambda y: 7, STEEL, 2, 6, 42)
    bell(cv, 19, 21, lambda y: 4, STEEL, 1, 4, 43)
    radii = {}
    for y0, y1, r in [(16, 19, 6), (12, 16, 8), (8, 12, 10), (4, 8, 12), (0, 4, 13)]:
        for y in range(y0, y1):
            radii[y] = r
    bell(cv, 12, 19, lambda y: radii[y], COPPER, 1, 5, 44)                  # regeneratively cooled part
    bell(cv, 0, 12, lambda y: radii[y], NIOBIUM, 2, 6, 45, lip=NIOBIUM)      # radiatively cooled extension
    ring(cv, SIDE, 12, radii[12], STEEL, 2, 5)                               # extension flange
    for y in (5, 9):
        ring(cv, SIDE, y, radii[y], NIOBIUM, 1, 4)
    turbopump(cv, (21,), 46)
    engine_top(cv, TOP, 47)
    nozzle_exit(cv, BOTTOM, 13, 4)
    ox, oy = BOTTOM
    for row in range(32):
        for c in range(32):
            d = math.hypot(c + 0.5 - 16, row + 0.5 - 16)
            if 13.5 < d <= 16.2:
                cv[ox + c, oy + row] = CARBON[3]
    return cv


def methalox_engine():
    cv = new_sheet()
    thrust_structure(cv, 16, 20, 131)
    bell(cv, 14, 16, lambda y: 6, STEEL, 3, 6, 132)           # powerhead
    bell(cv, 12, 14, lambda y: 4, STEEL, 1, 4, 133)
    radii = {}
    for y0, y1, r in [(9, 12, 6), (5, 9, 7), (2, 5, 8), (0, 2, 9)]:
        for y in range(y0, y1):
            radii[y] = r
    bell(cv, 0, 12, lambda y: radii[y], STEEL, 2, 6, 134, lip=STEEL)  # steel, not copper: cleaner, hotter fuel
    for y in (3, 7, 10):
        ring(cv, SIDE, y, radii[y], STEEL, 1, 3)                       # coolant manifolds
    turbopump(cv, (11, 15), 135)
    engine_top(cv, TOP, 136)
    nozzle_exit(cv, BOTTOM, 9, 4)
    ox, oy = BOTTOM
    for row in range(32):
        for c in range(32):
            d = math.hypot(c + 0.5 - 16, row + 0.5 - 16)
            if 9.5 < d <= 16.2:
                cv[ox + c, oy + row] = CARBON[1] if d < 15 else CARBON[3]
    return cv


def methalox_vacuum_engine():
    cv = new_sheet()
    thrust_structure(cv, 26, 30, 141)
    bell(cv, 23, 26, lambda y: 6, STEEL, 3, 6, 142)
    bell(cv, 21, 23, lambda y: 4, STEEL, 1, 4, 143)
    radii = {}
    for y0, y1, r in [(18, 21, 7), (14, 18, 9), (10, 14, 11), (5, 10, 13), (0, 5, 14)]:
        for y in range(y0, y1):
            radii[y] = r
    bell(cv, 14, 21, lambda y: radii[y], STEEL, 2, 6, 144)                 # cooled steel throat section
    bell(cv, 0, 14, lambda y: radii[y], CARBON, 2, 5, 145, lip=CARBON)      # dark radiatively cooled extension
    ring(cv, SIDE, 14, radii[14], STEEL, 3, 6)                              # extension flange
    for y in (4, 9):
        ring(cv, SIDE, y, radii[y], CARBON, 1, 3)
    turbopump(cv, (23,), 146)
    engine_top(cv, TOP, 147)
    nozzle_exit(cv, BOTTOM, 14, 4)
    ox, oy = BOTTOM
    for row in range(32):
        for c in range(32):
            d = math.hypot(c + 0.5 - 16, row + 0.5 - 16)
            if 14.5 < d <= 16.2:
                cv[ox + c, oy + row] = CARBON[3]
    return cv


def hydrolox_engine():
    cv = new_sheet()
    thrust_structure(cv, 18, 22, 151)
    ring(cv, SIDE, 20, 16, WHITE, 4, 6)                        # white insulation band: it is very cold up here
    bell(cv, 15, 18, lambda y: 6, STEEL, 3, 6, 152)
    bell(cv, 13, 15, lambda y: 4, STEEL, 1, 4, 153)
    radii = {}
    for y0, y1, r in [(10, 13, 6), (6, 10, 8), (3, 6, 9), (0, 3, 10)]:
        for y in range(y0, y1):
            radii[y] = r
    bell(cv, 0, 13, lambda y: radii[y], COPPER, 2, 5, 154, lip=COPPER)       # hundreds of brazed coolant tubes
    for y in range(1, 13):
        for c in range(0, 32, 3):
            put_side(cv, SIDE, c, y, COPPER[1])                             # tube seams
    ring(cv, SIDE, 6, radii[6], STEEL, 2, 5)
    turbopump(cv, (13, 17), 155)
    engine_top(cv, TOP, 156)
    nozzle_exit(cv, BOTTOM, 10, 4)
    ox, oy = BOTTOM
    for row in range(32):
        for c in range(32):
            d = math.hypot(c + 0.5 - 16, row + 0.5 - 16)
            if 10.5 < d <= 16.2:
                cv[ox + c, oy + row] = CARBON[1] if d < 15 else CARBON[3]
    return cv


def hydrolox_vacuum_engine():
    cv = new_sheet()
    thrust_structure(cv, 26, 30, 161)
    bell(cv, 23, 26, lambda y: 5, STEEL, 3, 6, 162)
    bell(cv, 21, 23, lambda y: 3, STEEL, 1, 4, 163)
    radii = {}
    for y0, y1, r in [(17, 21, 5), (12, 17, 7), (6, 12, 9), (0, 6, 12)]:
        for y in range(y0, y1):
            radii[y] = r
    bell(cv, 12, 21, lambda y: radii[y], COPPER, 2, 5, 164)
    bell(cv, 0, 12, lambda y: radii[y], GOLD, 1, 4, 165, lip=GOLD)       # thin extension in gold-coloured niobium alloy
    ring(cv, SIDE, 12, radii[12], STEEL, 3, 6)
    turbopump(cv, (24,), 166)
    engine_top(cv, TOP, 167)
    nozzle_exit(cv, BOTTOM, 12, 3)
    ox, oy = BOTTOM
    for row in range(32):
        for c in range(32):
            d = math.hypot(c + 0.5 - 16, row + 0.5 - 16)
            if 12.5 < d <= 16.2:
                cv[ox + c, oy + row] = CARBON[3]
    return cv


NTR_BELL = [(y, y + 1, r) for y, r in enumerate([11, 10, 9, 8, 8, 7, 6, 6, 5, 5, 4, 4])]


def nuclear_thermal_engine():
    cv = new_sheet()
    radius = {y: r for y0, y1, r in NTR_BELL for y in range(y0, y1)}
    thrust_structure(cv, 28, 32, 171)
    bell(cv, 24, 28, lambda y: 13, CARBON, 1, 4, 172)                    # thick tungsten shadow shield
    ring(cv, SIDE, 27, 13, STEEL, 3, 6)                                  # bright rim on its top edge
    for c in range(1, 32, 4):
        put_side(cv, SIDE, c, 25, STEEL[4])                              # bolts round the shield
    bell(cv, 14, 24, lambda y: 8, STEEL, 2, 6, 173)                      # reactor pressure vessel
    for y in (15, 23):
        ring(cv, SIDE, y, 8, STEEL, 1, 3)
    for y in (18, 19):                                                   # one plain warning stripe
        ring(cv, SIDE, y, 8, HAZARD, 2, 4)
    bell(cv, 12, 14, lambda y: 4, STEEL, 1, 4, 174)                      # throat
    bell(cv, 0, 12, lambda y: radius[y], COPPER, 3, 5, 175, lip=COPPER)  # regeneratively cooled bell
    for y in range(1, 12):
        r = radius[y]
        for c in range(16 - r, 16 + r):
            if (c - (16 - r)) % 4 == 0:
                put_side(cv, SIDE, c, y, COPPER[3])                      # faint coolant tube seams
    # aux view: the coolant pipes, polished steel with flanges
    for y in range(32):
        for c in range(32):
            put_side(cv, AUX, c, y, STEEL[5] if c % 4 == 1 else STEEL[3])
    for y in (13, 23):
        for c in range(32):
            put_side(cv, AUX, c, y, STEEL[1])
    # top view: hidden under the mount, except the bell's ledges, so it is painted as the copper bell seen from above
    def top(dx, dz, d):
        if d > 16.2:
            return None
        if d > 12:
            return CARBON[3]
        return COPPER[4]                       # same tone as the bell's side, so the ledges blend into one surface
    top_view(cv, TOP, top)
    nozzle_exit(cv, BOTTOM, 11, 4)
    return cv


PANEL = ramp('#0b1430', '#13214a', '#1c3166', '#2a4682', '#4a6aa8', '#8aa6d8')


def solar_panel(cv, region):
    """A whole region of blue solar cells with a fine silver grid."""
    ox, oy = region
    for v in range(32):
        for u in range(32):
            grid = u % 4 == 0 or v % 4 == 0
            shade = 2 + ((u // 4 + v // 4) % 2)
            cv[ox + u, oy + v] = STEEL[4] if grid else PANEL[shade if (u + v) % 7 else shade + 1]


def foil(cv, region, c0, c1, y0, y1, seed):
    """Crinkled gold insulation foil on the side view between columns c0..c1 and heights y0..y1."""
    n = clustered(32, 32, seed, passes=1)
    for y in range(y0, y1):
        for c in range(c0, c1):
            v = n[y % 32][c % 32]
            put_side(cv, region, c, y, GOLD[int(round(1 + 3 * v))])


def comm_satellite():
    cv = new_sheet()
    for y in range(0, 3):
        for c in range(1, 31):
            put_side(cv, SIDE, c, y, STEEL[2 + (c % 3 == 0)])                 # adapter ring
    foil(cv, SIDE, 0, 32, 3, 17, 171)
    for c in range(10, 22):
        put_side(cv, SIDE, c, 9, STEEL[5])                                    # equipment panel
        put_side(cv, SIDE, c, 10, STEEL[3])
    for y in range(17, 20):
        for c in range(0, 32):
            put_side(cv, SIDE, c, y, WHITE[6] if y == 19 else WHITE[4])        # dish rim
    for y in range(20, 24):
        for c in range(14, 18):
            put_side(cv, SIDE, c, y, STEEL[5] if y < 23 else STEEL[3])
    solar_panel(cv, AUX)
    top_view(cv, TOP, lambda dx, dz, d: None if d > 16 else (STEEL[3] if d < 1.5 else WHITE[6 - int(d / 4) % 3] if d < 9.5 else GOLD[3]))
    top_view(cv, BOTTOM, lambda dx, dz, d: None if d > 16 else (CARBON[2] if d < 6 else STEEL[3] if d < 7.5 else GOLD[2]))
    return cv


def cargo_module():
    """White cargo can: dark adapter ring, two orange cargo doors with hinges, a black-and-yellow stencil band."""
    cv = new_sheet()
    for y in range(0, 22):
        for c in range(32):
            col = STEEL[2 + (c % 3 == 0)] if y < 2 else WHITE[5] if (c + y) % 7 else WHITE[4]
            if y >= 20:
                col = STEEL[4]
            put_side(cv, SIDE, c, y, col)
    for c in range(32):
        put_side(cv, SIDE, c, 3, HAZARD[3] if c % 4 < 2 else CARBON[2])     # stencil band
    for door in (2, 18):
        for y in range(6, 17):
            for c in range(door, door + 12):
                edge = y in (6, 16) or c in (door, door + 11)
                put_side(cv, SIDE, c, y, CARBON[2] if edge else ORANGE[4] if (y + c) % 5 else ORANGE[3])
        for y in (8, 14):
            put_side(cv, SIDE, door + 1, y, STEEL[5])                        # hinges
        put_side(cv, SIDE, door + 9, 11, STEEL[5])                           # latch
    top_view(cv, TOP, lambda dx, dz, d: None if d > 16 else (CARBON[3] if d < 4 else STEEL[3] if d < 8.5 else WHITE[5]))
    top_view(cv, BOTTOM, lambda dx, dz, d: None if d > 16 else (CARBON[2] if d < 6 else STEEL[3] if d < 10.5 else WHITE[4]))
    return cv


def station_core_part():
    """The folded station core: white hull, grey frame bands, four yellow-edged mouth hatches, a radiator stripe."""
    cv = new_sheet()
    for y in range(0, 31):
        for c in range(32):
            col = STEEL[2 + (c % 3 == 0)] if y < 2 else WHITE[5] if (c + y) % 9 else WHITE[4]
            if y >= 28:
                col = STEEL[4]
            elif y in (7, 21):
                col = STEEL[3]
            put_side(cv, SIDE, c, y, col)
    for hx in (1, 9, 17, 25):
        for y in range(10, 18):
            for c in range(hx, hx + 6):
                edge = y in (10, 17) or c in (hx, hx + 5)
                put_side(cv, SIDE, c, y, (HAZARD[3] if (y + c) % 2 else CARBON[2]) if edge else WHITE[6])
        put_side(cv, SIDE, hx + 2, 13, STEEL[5])
        put_side(cv, SIDE, hx + 3, 14, STEEL[5])
    for c in range(32):
        put_side(cv, SIDE, c, 24, GOLD[3] if c % 2 else GOLD[2])               # radiator foil band
    top_view(cv, TOP, lambda dx, dz, d: None if d > 16 else (CARBON[3] if d < 2 else STEEL[4] if d < 4 else STEEL[3] if d < 8.5 else WHITE[5]))
    top_view(cv, BOTTOM, lambda dx, dz, d: None if d > 16 else (CARBON[2] if d < 6 else STEEL[3] if d < 10.5 else WHITE[4]))
    return cv


def docking_module_part():
    """The folded docking module: a white tunnel can with a window band and the docking ring on its nose."""
    cv = new_sheet()
    for y in range(0, 23):
        for c in range(32):
            col = STEEL[2 + (c % 3 == 0)] if y < 2 else WHITE[5] if (c + y) % 7 else WHITE[4]
            if y >= 20:
                col = STEEL[4]
            put_side(cv, SIDE, c, y, col)
    for c in range(32):
        put_side(cv, SIDE, c, 3, HAZARD[3] if c % 4 < 2 else CARBON[2])
        if c % 8 in (2, 3, 4, 5):
            for y in (11, 12):
                put_side(cv, SIDE, c, y, GLASS[3] if y == 11 else GLASS[2])  # window band
    top_view(cv, TOP, lambda dx, dz, d: None if d > 16 else (CARBON[3] if d < 2 else STEEL[5] if d < 4 else STEEL[3] if d < 7.5 else WHITE[5]))
    top_view(cv, BOTTOM, lambda dx, dz, d: None if d > 16 else (CARBON[2] if d < 6 else STEEL[3] if d < 10.5 else WHITE[4]))
    return cv


def observation_satellite():
    cv = new_sheet()
    for y in range(0, 10):
        for c in range(3, 29):
            put_side(cv, SIDE, c, y, WHITE[5] if y > 1 else CARBON[3])          # telescope tube, dark baffle at the mouth
    for c in range(1, 31):
        put_side(cv, SIDE, c, 8, STEEL[3])
        put_side(cv, SIDE, c, 9, STEEL[4])
    foil(cv, SIDE, 0, 32, 10, 21, 172)
    for c in range(12, 20):
        for y in range(14, 17):
            put_side(cv, SIDE, c, y, CARBON[4] if y != 15 else STEEL[5])       # star tracker
    for y in range(21, 24):
        for c in range(13, 19):
            put_side(cv, SIDE, c, y, STEEL[4])
    solar_panel(cv, AUX)
    top_view(cv, TOP, lambda dx, dz, d: None if d > 16 else (WHITE[6] if d < 3 else GOLD[3]))
    top_view(cv, BOTTOM, lambda dx, dz, d: None if d > 16 else (CARBON[0] if d < 3.5 else CARBON[3] if d < 5 else STEEL[3] if d < 7.5 else GOLD[2]))
    return cv


def space_probe():
    cv = new_sheet()
    for y in range(0, 7):
        for c in range(0, 32):
            put_side(cv, SIDE, c, y, STEEL[3] if y else STEEL[1])               # legs and pads
    foil(cv, SIDE, 0, 32, 5, 12, 173)
    for c in range(4, 28):
        put_side(cv, SIDE, c, 12, STEEL[5])
    for y in range(13, 16):
        for c in range(0, 32):
            put_side(cv, SIDE, c, y, WHITE[5] if y == 14 else WHITE[3])
    for y in range(16, 21):
        for c in range(0, 32):
            put_side(cv, SIDE, c, y, CARBON[3] if y >= 19 else STEEL[4])       # mast and camera head
    for c in range(20, 23):
        put_side(cv, SIDE, c, 19, (60, 140, 200, 255))                          # camera lens
    top_view(cv, TOP, lambda dx, dz, d: None if d > 16 else (WHITE[6] if d < 5 else STEEL[4] if d < 7 else GOLD[3]))
    top_view(cv, BOTTOM, lambda dx, dz, d: None if d > 16 else (CARBON[2] if d < 4 else GOLD[2]))
    return cv


LARGE_CAPSULE = [(0, 6, 16), (6, 12, 15), (12, 17, 14), (17, 22, 12), (22, 26, 10), (26, 29, 8), (29, 32, 6)]


def large_capsule_radius(y):
    for y0, y1, r in LARGE_CAPSULE:
        if y0 <= y < y1:
            return r
    return 0


def crew_capsule_large():
    cv = new_sheet()
    for region in (SIDE, AUX):
        n = clustered(32, 32, 181, passes=1)
        for y in range(32):
            r = large_capsule_radius(y)
            for c in range(32):
                if not radius_at(c, r):
                    continue
                v = col_shade(c - (16 - r), 2 * r) + (n[y][c] - 0.5) * 0.1
                put_side(cv, region, c, y, WHITE[int(round(2 + 5 * max(0, min(1, v))))])   # white thermal tiles
        for y0, _, r in LARGE_CAPSULE[1:]:
            ring(cv, region, y0, r, WHITE, 1, 3)                                         # panel seams
        ring(cv, region, 0, 16, CARBON, 1, 3)                                            # heat shield interface
        ring(cv, region, 31, 6, ALU, 3, 6)                                               # docking ring
        for c in range(4, 28):
            put_side(cv, region, c, 2, RED[3] if c % 4 else WHITE[1])                     # trunk stripe
    # three windows on the front, one per seat
    for wx in (7, 14, 21):
        for y in range(15, 19):
            for c in range(wx, wx + 4):
                frame = y in (15, 18) or c in (wx, wx + 3)
                put_side(cv, AUX, c, y, CARBON[1] if frame else (GLASS[5] if c == wx + 1 and y == 17 else GLASS[2]))
    for y in range(5, 13):
        for c in range(12, 20):
            if y in (5, 12) or c in (12, 19):
                put_side(cv, AUX, c, y, WHITE[1])                                         # side hatch outline
    put_side(cv, AUX, 18, 8, STEEL[2])
    def top(dx, dz, d):
        if d > 16.2:
            return None
        if d < 3:
            return CARBON[2]
        if d < 6.2:
            return ALU[5] if d < 5 else ALU[3]
        return WHITE[5] if (dx + dz) < 0 else WHITE[4]
    top_view(cv, TOP, top)
    top_view(cv, BOTTOM, lambda dx, dz, d: None if d > 16.2 else CARBON[2])
    return cv


# --------------------------------------------------------------------------- separators

def decoupler():
    cv = new_sheet()
    paint_body(cv, SIDE, 0, 3, 16, STEEL, 2, 6, 51)
    ring(cv, SIDE, 0, 16, STEEL, 1, 3)
    ring(cv, SIDE, 2, 16, STEEL, 4, 7)
    for c in range(1, 32, 4):
        put_side(cv, SIDE, c, 1, HAZARD[3] if (c // 4) % 2 == 0 else CARBON[2])   # separation line marks
    def fn(dx, dz, d):
        if d > 16.2:
            return None
        return STEEL[5] if d > 14.5 else (STEEL[3] if d > 13 else ALU[3])
    top_view(cv, TOP, fn)
    top_view(cv, BOTTOM, lambda dx, dz, d: None if d > 16.2 else (STEEL[3] if d > 14.5 else ALU[2]))
    return cv


def interstage():
    cv = new_sheet()
    paint_body(cv, SIDE, 0, 28, 16, CARBON, 1, 5, 61, grain=0.18)
    ring(cv, SIDE, 0, 16, CARBON, 0, 2)
    ring(cv, SIDE, 27, 16, STEEL, 3, 5)
    ring(cv, SIDE, 26, 16, CARBON, 3, 6)
    # vent slots and the separation joint
    for y in (18, 19):
        for c in range(3, 30, 6):
            put_side(cv, SIDE, c, y, CARBON[0])
            put_side(cv, SIDE, c + 1, y, CARBON[0])
    ring(cv, SIDE, 4, 16, STEEL, 2, 4)
    # looking in from the top/bottom: the dark inside of the shell
    def fn(dx, dz, d):
        if d > 16.2:
            return None
        return CARBON[4] if d > 14.5 else INNER[1]
    top_view(cv, TOP, fn)
    top_view(cv, BOTTOM, fn)
    return cv


# --------------------------------------------------------------------------- crew section

def heat_shield():
    cv = new_sheet()
    paint_body(cv, SIDE, 0, 3, 16, ABLATOR, 1, 5, 71, grain=0.2)
    ring(cv, SIDE, 2, 16, ABLATOR, 3, 6)
    rng = random.Random(72)

    def under(dx, dz, d):
        if d > 16.2:
            return None
        # honeycomb ablator: hex-ish cells shown as a staggered grid with darker cell walls
        cx = int((dx + 32) // 3)
        cz = int((dz + 32 + (1.5 if cx % 2 else 0)) // 3)
        wall = (int(dx + 32) % 3 == 0) or (int(dz + 32 + (1.5 if cx % 2 else 0)) % 3 == 0)
        char = 1 if d < 10 else 0
        if wall:
            return ABLATOR[1 + char]
        return ABLATOR[3 + char + (rng.random() < 0.15)]
    top_view(cv, BOTTOM, under)
    top_view(cv, TOP, lambda dx, dz, d: None if d > 16.2 else (ABLATOR[4] if d > 14.5 else STEEL[3]))
    return cv


def capsule_profile_radius(y):
    for y0, y1, r in [(0, 4, 16), (4, 8, 15), (8, 12, 14), (12, 16, 12), (16, 20, 11), (20, 23, 9), (23, 26, 7)]:
        if y0 <= y < y1:
            return r
    return 0


def crew_capsule():
    cv = new_sheet()
    for region in (SIDE, AUX):
        n = clustered(32, 32, 81, passes=1)
        for y in range(26):
            r = capsule_profile_radius(y)
            for c in range(32):
                if not radius_at(c, r):
                    continue
                v = col_shade(c - (16 - r), 2 * r) + (n[y][c] - 0.5) * 0.12
                # corrugated back-shell shingles: lighter top edge on every second row
                v += 0.18 if y % 2 == 1 else -0.05
                put_side(cv, region, c, y, CARBON[int(round(1 + 5 * max(0, min(1, v))))])
        for y in (3, 7, 11, 15, 19, 22):
            ring(cv, region, y, capsule_profile_radius(y), CARBON, 4, 6)   # step edges catch the light
        ring(cv, region, 0, 16, ALU, 2, 4)           # heat shield interface ring
        ring(cv, region, 25, 7, ALU, 3, 6)           # forward ring
    # front view: crew hatch with a window and a handle
    for y in range(6, 18):
        for c in range(10, 22):
            edge = y in (6, 17) or c in (10, 21)
            if edge:
                put_side(cv, AUX, c, y, CARBON[0] if (y == 6 or c == 21) else CARBON[5])
    for y in range(12, 16):
        for c in range(13, 19):
            frame = y in (12, 15) or c in (13, 18)
            if frame:
                put_side(cv, AUX, c, y, ALU[5] if (y == 15 or c == 13) else ALU[3])
            else:
                g = GLASS[5] if (c - 14 + 15 - y) == 1 else GLASS[2]
                put_side(cv, AUX, c, y, g)
    for c in (19, 20):
        put_side(cv, AUX, c, 9, STEEL[2])               # hatch handle
    put_side(cv, AUX, 11, 8, RED[3])                     # rescue marking
    put_side(cv, AUX, 11, 9, RED[3])
    # top: docking ring / parachute port
    def top(dx, dz, d):
        if d > 16.2:
            return None
        if d < 3:
            return ALU[2]
        if d < 7.2:
            return ALU[5] if d < 6 else ALU[3]
        return CARBON[5] if (dx + dz) < 0 else CARBON[4]
    top_view(cv, TOP, top)
    top_view(cv, BOTTOM, lambda dx, dz, d: None if d > 16.2 else CARBON[2])
    return cv


def parachute():
    cv = new_sheet()
    paint_body(cv, SIDE, 0, 5, 6, WHITE, 3, 7, 91)
    ring(cv, SIDE, 4, 6, RED, 2, 4)
    ring(cv, SIDE, 0, 6, ALU, 2, 4)
    paint_body(cv, SIDE, 5, 6, 4, ALU, 3, 6, 92)
    def top(dx, dz, d):
        if d > 6.2:
            return None
        if d < 1.5:
            return STEEL[3]
        if d < 4.2:
            return ALU[5]
        return WHITE[6] if dx + dz < 0 else WHITE[5]
    top_view(cv, TOP, top)
    top_view(cv, BOTTOM, lambda dx, dz, d: None if d > 6.2 else ALU[2])
    return cv


def nose_cone():
    cv = new_sheet()
    prof = [(0, 4, 16), (4, 8, 15), (8, 11, 13), (11, 14, 11), (14, 17, 9), (17, 20, 6), (20, 22, 4), (22, 24, 2)]

    def rad(y):
        for y0, y1, r in prof:
            if y0 <= y < y1:
                return r
        return 0
    n = clustered(32, 32, 101, passes=1)
    for y in range(24):
        r = rad(y)
        for c in range(32):
            if radius_at(c, r):
                v = col_shade(c - (16 - r), 2 * r) + (n[y][c] - 0.5) * 0.1
                put_side(cv, SIDE, c, y, WHITE[int(round(3 + 4 * max(0, min(1, v))))])
    for y in range(20, 24):
        ring(cv, SIDE, y, rad(y), CARBON, 2, 5)           # dark tip
    ring(cv, SIDE, 0, 16, ALU, 2, 4)
    for y in range(1, 20):
        put_side(cv, SIDE, 16, y, WHITE[3])               # fairing separation line
    def top(dx, dz, d):
        if d > 16.2:
            return None
        if d < 4.2:
            return CARBON[4] if dx + dz < 0 else CARBON[3]
        return WHITE[6] if dx + dz < 0 else WHITE[5]
    top_view(cv, TOP, top)
    top_view(cv, BOTTOM, lambda dx, dz, d: None if d > 16.2 else ALU[2])
    return cv


# --------------------------------------------------------------------------- radial parts

def fill(cv, x0, y0, w, h, fn):
    for y in range(h):
        for x in range(w):
            c = fn(x, y)
            if c is not None:
                cv[x0 + x, y0 + y] = c


def radial_front(cv, fn):
    """Front (north/south) view: fn(c, y) with c = column 0..31 and y = model height -16..31."""
    fill(cv, 0, 0, 32, 48, lambda x, r: fn(x, 31 - r))


def fins():
    cv = new_sheet()
    n = clustered(48, 48, 111, passes=1)

    def prof(o, y):
        # o: 0 at the root, 7 at the tip
        v = 0.75 - o * 0.04 + (n[y % 48][o % 48] - 0.5) * 0.15
        if y in (-4, -3) or o == 7:
            return ALU[2]
        return ALU[int(round(2 + 4 * max(0, min(1, v))))]
    fill(cv, 32, 0, 16, 48, lambda x, r: prof((x - 8) if x >= 8 else (7 - x), 31 - r))
    radial_front(cv, lambda c, y: ALU[5] if c == 15 else ALU[3])
    fill(cv, 0, 48, 32, 16, lambda x, r: ALU[6])
    fill(cv, 32, 48, 32, 16, lambda x, r: ALU[2])
    return cv


def grid_fins():
    cv = new_sheet()
    radial_front(cv, lambda c, y: STEEL[5] if y == -2 else STEEL[3])
    fill(cv, 32, 0, 16, 48, lambda x, r: STEEL[4] if (31 - r) > -3 else STEEL[2])

    def lattice(x, r):
        # top/bottom: a waffle grid with see-through cells (columns 3..12 of the front, rows z -16..-10)
        z = r - 16
        c = x - 8
        border = x in (11, 20) or z in (-16, -11)
        grid = (x % 3 == 2) or ((z + 16) % 3 == 2)
        if border:
            return STEEL[6] if r < 8 else STEEL[3]
        return (STEEL[5] if r < 8 else STEEL[2]) if grid else TRANSPARENT
    fill(cv, 0, 48, 32, 16, lattice)
    fill(cv, 32, 48, 32, 16, lambda x, r: lattice(x, r))
    return cv


def landing_legs():
    cv = new_sheet()
    n = clustered(48, 48, 121, passes=1)

    def front(c, y):
        if y <= -14:
            return STEEL[5] if y == -14 else STEEL[3]            # foot pad
        if y >= 16:
            return STEEL[4]                                      # hinge
        v = col_shade(c - 14, 4) + (n[(y + 16) % 48][c] - 0.5) * 0.15
        if y in (2, 3):
            return GOLD[3]                                       # shock absorber collar
        return CARBON[int(round(1 + 4 * max(0, min(1, v))))]
    radial_front(cv, front)
    fill(cv, 32, 0, 16, 48, lambda x, r: front(15, 31 - r))
    fill(cv, 0, 48, 32, 16, lambda x, r: STEEL[5])
    fill(cv, 32, 48, 32, 16, lambda x, r: STEEL[2])
    return cv


def rcs_thrusters():
    cv = new_sheet()

    def front(c, y):
        if y in (-8, -2) or c in (12, 19) and -6 <= y <= -4:
            return CARBON[1]                                      # nozzle mouths
        return WHITE[5] if y >= -5 else WHITE[3]
    radial_front(cv, front)
    fill(cv, 32, 0, 16, 48, lambda x, r: WHITE[4] if (31 - r) >= -5 else WHITE[2])
    fill(cv, 0, 48, 32, 16, lambda x, r: WHITE[6])
    fill(cv, 32, 48, 32, 16, lambda x, r: CARBON[2])
    return cv


# --------------------------------------------------------------------------- 16x16 icons

def grid_icon(rows, pal):
    cv = Canvas()
    for y, row in enumerate(rows):
        for x, ch in enumerate(row):
            if ch in pal:
                cv[x, y] = pal[ch]
    return cv


def cylinder_icon(top_y, bottom_y, x0=3, x1=12, body=WHITE, outline=None, bands=(), dark_band=()):
    """A short upright cylinder seen slightly from above: elliptic top, shaded body (3 tones), seams."""
    cv = Canvas()
    outline = outline or body[0]
    w = x1 - x0 + 1
    for y in range(top_y + 1, bottom_y):
        for x in range(x0, x1 + 1):
            t = (x - x0) / max(1, w - 1)
            idx = 7 if 0.12 < t < 0.45 else (6 if t < 0.75 else 5)
            c = body[idx]
            if y in bands:
                c = body[3]
            if y in dark_band:
                c = CARBON[3] if idx >= 6 else CARBON[2]
            cv[x, y] = c
    # top ellipse (2 px tall), seen from above
    for x in range(x0 + 1, x1):
        cv[x, top_y] = body[6] if x < (x0 + x1) // 2 + 2 else body[5]
        cv[x, top_y + 1] = body[4]
    for y in range(top_y + 1, bottom_y):
        cv[x0 - 1, y] = outline
        cv[x1 + 1, y] = outline
    for x in range(x0 + 1, x1):
        cv[x, top_y - 1] = outline
        cv[x, bottom_y] = outline
    cv[x0, top_y] = outline
    cv[x1, top_y] = outline
    cv[x0, bottom_y - 1] = outline
    cv[x1, bottom_y - 1] = outline
    cv[x0, bottom_y] = TRANSPARENT
    cv[x1, bottom_y] = TRANSPARENT
    return cv


def mli_icon():
    """A folded multi-layer insulation blanket: gold foil on top, silver layers showing at the edge."""
    cv = Canvas()
    rng = random.Random(41)
    for y in range(3, 13):
        for x in range(2 + (y - 3) // 3, 14 - (12 - y) // 4):
            if y >= 11:
                cv[x, y] = ALU[4] if (x + y) % 2 else ALU[2]          # the stacked silver layers at the folded edge
            else:
                cv[x, y] = GOLD[5] if rng.random() < 0.25 else GOLD[3] if (x * 3 + y) % 5 else GOLD[2]
    return cv


def icons():
    out = {}
    out['fuel_tank_small'] = cylinder_icon(4, 12, bands=(5,), outline=WHITE[0])
    out['fuel_tank_medium'] = cylinder_icon(2, 14, bands=(3, 8), dark_band=(10, 11), outline=WHITE[0])
    out['fuel_tank_large'] = cylinder_icon(1, 15, bands=(8, 13), dark_band=(3, 4, 5), outline=WHITE[0])
    gold = [GOLD[0], GOLD[1], GOLD[2], ALU[4], GOLD[3], GOLD[4], GOLD[5], GOLD[6]]     # body[3] is the silver seam band
    out['fuel_tank_small_insulated'] = cylinder_icon(4, 12, body=gold, bands=(5, 10), outline=GOLD[0])
    out['fuel_tank_medium_insulated'] = cylinder_icon(2, 14, body=gold, bands=(3, 8, 12), outline=GOLD[0])
    out['fuel_tank_large_insulated'] = cylinder_icon(1, 15, body=gold, bands=(2, 8, 14), outline=GOLD[0])
    out['multi_layer_insulation'] = mli_icon()
    w, s, c, k = WHITE, STEEL, COPPER, CARBON
    out['kerosene_engine'] = grid_icon([
        "................",
        "..oooooooooooo..",
        ".oKKKKKKKKKKkko.",
        ".okkkkkkkkkkkdo.",
        "..ooooSSSoooo...",
        "......SsS.pP....",
        "......sss.pP....",
        ".......o..pp....",
        "......cCco......",
        ".....cCCcco.....",
        ".....CCcccdo....",
        "....cCCcccdo....",
        "....CCccccddo...",
        "...cCCcccccdo...",
        "...DDDDDDDDDD...",
        "................",
    ], {'o': k[0], 'K': k[5], 'k': k[3], 'd': k[1], 'S': s[6], 's': s[4], 'p': s[4], 'P': s[6],
        'C': c[6], 'c': c[4], 'D': c[1]})
    out['vacuum_engine'] = grid_icon([
        "..oooooooooooo..",
        ".oKKKKKKKKKKkko.",
        "..ooooSSSoooo...",
        "......SsS.P.....",
        "......cCc.p.....",
        ".....cCCco......",
        ".....cCcco......",
        "....nNNnnno.....",
        "....NNnnnno.....",
        "...nNNnnnnno....",
        "...NNnnnnnno....",
        "..nNNnnnnnnno...",
        "..NNnnnnnnnno...",
        ".nNNnnnnnnnnno..",
        ".DDDDDDDDDDDDD..",
        "................",
    ], {'o': k[0], 'K': k[5], 'k': k[3], 'S': s[6], 's': s[4], 'p': s[4], 'P': s[6],
        'C': c[6], 'c': c[4], 'N': NIOBIUM[6], 'n': NIOBIUM[4], 'D': k[0]})
    out['methalox_engine'] = grid_icon([
        "................",
        "..oooooooooooo..",
        ".oKKKKKKKKKKkko.",
        ".okkkkkkkkkkkdo.",
        "..ooooSSSoooo...",
        ".Pp...SsS...pP..",
        ".Pp...sss...pP..",
        ".pp...WWw...pp..",
        "......WWwo......",
        ".....WWwwwo.....",
        ".....WWwwwdo....",
        "....WWwwwwdo....",
        "....WWwwwwwdo...",
        "...WWwwwwwwdo...",
        "...DDDDDDDDDD...",
        "................",
    ], {'o': k[0], 'K': k[5], 'k': k[3], 'd': s[1], 'S': s[6], 's': s[4], 'p': s[4], 'P': s[6],
        'W': s[6], 'w': s[4], 'D': k[1]})
    out['methalox_vacuum_engine'] = grid_icon([
        "..oooooooooooo..",
        ".oKKKKKKKKKKkko.",
        "..ooooSSSoooo...",
        ".Pp...SsS...pP..",
        ".pp...WWw...pp..",
        ".....WWwwo......",
        ".....WWwwo......",
        "....WWwwwwo.....",
        "....nNNnnno.....",
        "...nNNnnnnno....",
        "...NNnnnnnno....",
        "..nNNnnnnnnno...",
        "..NNnnnnnnnnno..",
        ".nNNnnnnnnnnnno.",
        ".DDDDDDDDDDDDDD.",
        "................",
    ], {'o': k[0], 'K': k[5], 'k': k[3], 'S': s[6], 's': s[4], 'p': s[4], 'P': s[6],
        'W': s[6], 'w': s[4], 'N': k[5], 'n': k[3], 'D': k[0]})
    out['hydrolox_engine'] = grid_icon([
        "................",
        "..oooooooooooo..",
        ".oKKKKKKKKKKkko.",
        ".oWWWWWWWWWWWwo.",
        "..ooooSSSoooo...",
        ".Pp...SsS...pP..",
        ".pp...sss...pp..",
        "......cCco......",
        ".....cCccco.....",
        ".....CcCcCdo....",
        "....cCcCcCdo....",
        "....CcCcCcCdo...",
        "...cCcCcCcCdo...",
        "...CcCcCcCcCdo..",
        "...DDDDDDDDDDD..",
        "................",
    ], {'o': k[0], 'K': k[5], 'k': k[3], 'W': w[5], 'w': w[3], 'd': k[3], 'S': s[6], 's': s[4], 'p': s[4], 'P': s[6],
        'C': c[6], 'c': c[4], 'D': k[0]})
    out['hydrolox_vacuum_engine'] = grid_icon([
        "..oooooooooooo..",
        ".oKKKKKKKKKKkko.",
        "..ooooSSSoooo...",
        "......SsSP......",
        "......cCcp......",
        ".....cCcco......",
        ".....cCcco......",
        ".....gGggo......",
        "....gGGgggo.....",
        "....GGggggo.....",
        "...gGGgggggo....",
        "...GGgggggggo...",
        "..gGGgggggggo...",
        "..GGgggggggggo..",
        ".DDDDDDDDDDDDD..",
        "................",
    ], {'o': k[0], 'K': k[5], 'k': k[3], 'S': s[6], 's': s[4], 'p': s[4], 'P': s[6],
        'C': c[6], 'c': c[4], 'G': GOLD[5], 'g': GOLD[3], 'D': k[0]})
    out['station_core_module'] = grid_icon([
        "................",
        "......oooo......",
        ".....oKkkKo.....",
        "...oooooooooo...",
        "..oWWWWWWWWWWo..",
        "..owwwwwwwwwwo..",
        "..oWYYWWWWYYWo..",
        "..oWYkYWWYkYWo..",
        "..oWYYWWWWYYWo..",
        "..oWWWWWWWWWWo..",
        "..oGgGgGgGgGgo..",
        "..oWWWWWWWWWWo..",
        "..owwwwwwwwwwo..",
        "...oooooooooo...",
        ".....oKKKKo.....",
        "................",
    ], {'o': CARBON[0], 'K': STEEL[5], 'k': STEEL[3], 'W': WHITE[6], 'w': WHITE[4], 'Y': HAZARD[3], 'G': GOLD[4], 'g': GOLD[2]})
    out['docking_module'] = grid_icon([
        "................",
        "................",
        "......oooo......",
        ".....oKkkKo.....",
        "....oooooooo....",
        "...oWWWWWWWWo...",
        "...oYkYkYkYko...",
        "...oWWWWWWWWo...",
        "...oWBBWWBBWo...",
        "...oWWWWWWWWo...",
        "...owwwwwwwwo...",
        "....oooooooo....",
        ".....oKKKKo.....",
        "................",
        "................",
        "................",
    ], {'o': CARBON[0], 'K': STEEL[5], 'k': STEEL[3], 'W': WHITE[6], 'w': WHITE[4], 'Y': HAZARD[3], 'B': GLASS[3]})
    g_, s_, k_, w_ = GOLD, STEEL, CARBON, WHITE
    out['cargo_module'] = grid_icon([
        "................",
        "......oooo......",
        ".....oKKKKo.....",
        "...oooooooooo...",
        "..oWWWWWWWWWWo..",
        "..oYkYkYkYkYko..",
        "..oWOOOWWOOOWo..",
        "..oWOooWWOooWo..",
        "..oWOOOWWOOOWo..",
        "..oWOOOWWOOOWo..",
        "..oWWWWWWWWWWo..",
        "..oooooooooooo..",
        ".....oSSSSo.....",
        "....oSSSSSSo....",
        "................",
        "................",
    ], {'o': k_[0], 'K': k_[4], 'k': k_[2], 'W': w_[6], 'Y': HAZARD[3], 'O': ORANGE[4], 'S': s_[4]})
    out['comm_satellite'] = grid_icon([
        "................",
        ".......o........",
        "......oWo.......",
        "....oWWWWWo.....",
        "...oWwwwwwWo....",
        ".....ooooo......",
        "bb...oGGGo...bb.",
        "bBb..oGgGo..bBb.",
        "bBbssoGGgossbBb.",
        "bBb..oGgGo..bBb.",
        "bb...oGGGo...bb.",
        ".....ooooo......",
        "......sss.......",
        ".....sssss......",
        "................",
        "................",
    ], {'o': k_[0], 'W': w_[6], 'w': w_[4], 'G': g_[4], 'g': g_[2], 's': s_[4], 'b': PANEL[1], 'B': PANEL[3]})
    out['observation_satellite'] = grid_icon([
        "................",
        ".......s........",
        "......sss.......",
        ".....ooooo......",
        "bb...oGGGo...bb.",
        "bBb..oGgGo..bBb.",
        "bBbssoGGgossbBb.",
        "bBb..oGgGo..bBb.",
        "bb...oGGGo...bb.",
        ".....ooooo......",
        "......oWWo......",
        "......oWwo......",
        "......oWwo......",
        "......okko......",
        ".......oo.......",
        "................",
    ], {'o': k_[0], 'W': w_[6], 'w': w_[4], 'k': k_[2], 'G': g_[4], 'g': g_[2], 's': s_[4], 'b': PANEL[1], 'B': PANEL[3]})
    out['space_probe'] = grid_icon([
        "................",
        "..........kk....",
        "..........ks....",
        "......WWW.s.....",
        ".....WwwwWs.....",
        "......ooooo.....",
        "....oGGGGGGo....",
        "...oGGgGGgGGo...",
        "...oGgGGgGGgo...",
        "....oooooooo....",
        "...s.s....s.s...",
        "..s..s....s..s..",
        ".s...s....s...s.",
        "ss..ss....ss..ss",
        "................",
        "................",
    ], {'o': k_[0], 'W': w_[6], 'w': w_[4], 'k': k_[2], 'G': g_[4], 'g': g_[2], 's': s_[4]})
    out['crew_capsule_large'] = grid_icon([
        "......oooo......",
        ".....oAAAAo.....",
        ".....oWWWWo.....",
        "....oWWWWWWo....",
        "....oWWWWWWo....",
        "...oWWWWWWWWo...",
        "...oWgWWgWWgo...",
        "..oWWgWWgWWgWo..",
        "..oWWWWWWWWWWo..",
        "..oWWWWWWWWWWo..",
        ".oWWWWWWWWWWWwo.",
        ".oWWWWWWWWWWWwo.",
        ".oRRRRRRRRRRRRo.",
        ".okkkkkkkkkkkko.",
        "..oooooooooooo..",
        "................",
    ], {'o': CARBON[0], 'A': ALU[4], 'W': WHITE[6], 'w': WHITE[3], 'g': GLASS[3], 'R': RED[3], 'k': CARBON[2]})
    out['decoupler'] = grid_icon([
        "................", "................", "................", "................",
        "................",
        "..oooooooooooo..",
        ".oSSSSSSSSSsso.",
        ".oyKyKyKyKyKyKo.",
        ".osssssssssddo..",
        "..oooooooooooo..",
        "................", "................", "................", "................",
        "................", "................",
    ], {'o': s[0], 'S': s[6], 's': s[4], 'd': s[2], 'y': HAZARD[3], 'K': k[2]})
    out['interstage'] = cylinder_icon(2, 14, body=CARBON + [CARBON[-1]], outline=CARBON[0], bands=(6,))
    out['heat_shield'] = grid_icon([
        "................", "................", "................", "................",
        "................", "................",
        "...oooooooooo...",
        ".ooAAAAAAAAaaoo.",
        "oAAaaaaaaaaaaddo",
        "oaaaaaaaaaaadddo",
        ".oddddddddddddo.",
        "..oooooooooooo..",
        "................", "................", "................", "................",
    ], {'o': ABLATOR[0], 'A': ABLATOR[5], 'a': ABLATOR[3], 'd': ABLATOR[2]})
    out['crew_capsule'] = grid_icon([
        "................",
        "......oooo......",
        ".....oAAAao.....",
        ".....oWWWwo.....",
        "....oWWWWwwo....",
        "....oWWWWwgo....",
        "...oWWgggWwgo...",
        "...oWWgVvgwgo...",
        "..oWWWgvvgwwgo..",
        "..oWWWWggWwwgo..",
        ".oWWWWWWWWwwggo.",
        ".oWWWWWWWwwwggo.",
        "oWWWWWWWWwwwgggo",
        "oKKKKKKKKKKKKkko",
        ".oooooooooooooo.",
        "................",
    ], {'o': k[0], 'W': k[5], 'w': k[3], 'g': k[2], 'A': ALU[6], 'a': ALU[4], 'V': GLASS[6], 'v': GLASS[2],
        'K': ALU[4], 'k': ALU[2]})
    out['parachute'] = grid_icon([
        "................", "................", "................",
        ".......oo.......",
        "......oAao......",
        ".....oooooo.....",
        "....oWWWWwwo....",
        "....oWWWWwgo....",
        "....oRRRRrro....",
        "....oWWWWwgo....",
        "....oWWWWwgo....",
        "....oaaaaaao....",
        ".....oooooo.....",
        "................", "................", "................",
    ], {'o': w[0], 'W': w[7], 'w': w[5], 'g': w[3], 'R': RED[4], 'r': RED[2], 'A': ALU[5], 'a': ALU[3]})
    out['nose_cone'] = grid_icon([
        ".......oo.......",
        "......oKko......",
        "......oKko......",
        ".....oWWwgo.....",
        ".....oWWwgo.....",
        "....oWWWwwgo....",
        "....oWWWwwgo....",
        "....oWWWwwgo....",
        "...oWWWWwwggo...",
        "...oWWWWwwggo...",
        "...oWWWWwwggo...",
        "..oWWWWWwwwggo..",
        "..oWWWWWwwwggo..",
        "..oaaaaaaaaaao..",
        "...oooooooooo...",
        "................",
    ], {'o': w[0], 'W': w[7], 'w': w[5], 'g': w[3], 'K': k[4], 'k': k[2], 'a': ALU[3]})
    out['fins'] = grid_icon([
        "................",
        "..........oo....",
        ".........oAo....",
        "........oAAo....",
        ".......oAAao....",
        "......oAAaao....",
        ".....oAAaaao....",
        "....oAAaaaao....",
        "...oAAaaaaao....",
        "..oAAaaaaaao....",
        ".oAAaaaaaaao....",
        ".oaaaaaaaaao....",
        "..ooooooooddo...",
        "..........ddo...",
        "...........oo...",
        "................",
    ], {'o': ALU[0], 'A': ALU[6], 'a': ALU[4], 'd': ALU[2]})
    out['grid_fins'] = grid_icon([
        "................", "................",
        "..oooooooooooo..",
        "..oSSSSSSSSSso..",
        "..oS.s.s.s.sso..",
        "..oSssssssssso..",
        "..oS.s.s.s.sso..",
        "..oSssssssssso..",
        "..oS.s.s.s.sso..",
        "..oSssssssssso..",
        "..oS.s.s.s.sso..",
        "..osssssssssdo..",
        "..oooooHHooooo..",
        "......oHho......",
        ".......oo.......",
        "................",
    ], {'o': s[0], 'S': s[6], 's': s[4], 'd': s[2], 'H': s[5], 'h': s[3]})
    out['landing_legs'] = grid_icon([
        "................",
        ".........oo.....",
        "........oSso....",
        "........oKko....",
        ".......oKko.....",
        ".......oKko.....",
        "......oGgo......",
        "......oKko......",
        ".....oKko.......",
        ".....oKko.......",
        "....oKko........",
        "....oKko........",
        "..ooSSsoo.......",
        ".oSSSSssso......",
        "..ooooooo.......",
        "................",
    ], {'o': k[0], 'K': k[5], 'k': k[3], 'S': s[6], 's': s[4], 'G': GOLD[5], 'g': GOLD[3]})
    out['rcs_thrusters'] = grid_icon([
        "................", "................", "................",
        ".......oo.......",
        "......oNno......",
        "....ooooooo.....",
        "...oWWWWWwgo....",
        ".ooWWWWWWwggoo..",
        "oNnWWWWWWwggNno.",
        ".ooWWWWWwwggoo..",
        "...oWwwwwwggo...",
        "....ooooooooo...",
        "......oNno......",
        ".......oo.......",
        "................", "................",
    ], {'o': w[0], 'W': w[7], 'w': w[5], 'g': w[3], 'N': k[4], 'n': k[1]})
    out['rocket'] = grid_icon([
        ".......oo.......",
        "......oWwo......",
        "......oKko......",
        ".....oWWwgo.....",
        ".....oWVwgo.....",
        ".....oWWwgo.....",
        ".....oKKkko.....",
        ".....oWWwgo.....",
        ".....oWWwgo.....",
        "....ooWWwgoo....",
        "...oAoWWwgoAo...",
        "..oAAoKKkkoAAo..",
        "..oAaoWWwgoaAo..",
        "..ooooSssSoooo..",
        "......cCcc......",
        ".....oFFffo.....",
    ], {'o': w[0], 'W': w[7], 'w': w[5], 'g': w[3], 'K': k[4], 'k': k[2], 'V': k[2], 'A': w[5], 'a': w[3],
        'S': w[3], 's': k[2], 'C': COPPER[5], 'c': k[2], 'F': (232, 170, 70), 'f': (204, 106, 44)})
    out['nuclear_thermal_engine'] = grid_icon([
        "..oooooooooooo..",
        ".oKKKKKKKKKKkko.",
        "..oNNNNNNNNNno..",
        "....oWWWWwwo....",
        "....oWWWWwwo....",
        "....oYkYkYko....",
        "....oWWWWwwo....",
        "......oSso......",
        "......nNno......",
        ".....nNNnno.....",
        ".....NNnnno.....",
        "....nNNnnnno....",
        "...nNNnnnnnno...",
        "...NNnnnnnnnno..",
        "......cCcc......",
        "................",
    ], {'o': k[0], 'K': k[4], 'k': k[2], 'N': NIOBIUM[5], 'n': NIOBIUM[3], 'W': w[6], 'w': w[4], 'Y': HAZARD[3],
        'S': NIOBIUM[5], 's': NIOBIUM[3], 'c': k[2], 'C': (150, 200, 255)})
    return out


SHEETS = {
    'fuel_tank_small': lambda: fuel_tank(16, 'plain', 11),
    'fuel_tank_medium': lambda: fuel_tank(24, 'band', 12),
    'fuel_tank_large': lambda: fuel_tank(32, 'collar', 13),
    'fuel_tank_small_insulated': lambda: fuel_tank_insulated(16, 21),
    'fuel_tank_medium_insulated': lambda: fuel_tank_insulated(24, 22),
    'fuel_tank_large_insulated': lambda: fuel_tank_insulated(32, 23),
    'kerosene_engine': kerosene_engine,
    'vacuum_engine': vacuum_engine,
    'methalox_engine': methalox_engine,
    'methalox_vacuum_engine': methalox_vacuum_engine,
    'hydrolox_engine': hydrolox_engine,
    'hydrolox_vacuum_engine': hydrolox_vacuum_engine,
    'nuclear_thermal_engine': nuclear_thermal_engine,
    'comm_satellite': comm_satellite,
    'observation_satellite': observation_satellite,
    'space_probe': space_probe,
    'cargo_module': cargo_module,
    'station_core_module': station_core_part,
    'docking_module': docking_module_part,
    'decoupler': decoupler,
    'interstage': interstage,
    'heat_shield': heat_shield,
    'crew_capsule': crew_capsule,
    'crew_capsule_large': crew_capsule_large,
    'parachute': parachute,
    'nose_cone': nose_cone,
    'fins': fins,
    'grid_fins': grid_fins,
    'landing_legs': landing_legs,
    'rcs_thrusters': rcs_thrusters,
}


def export(save):
    metal = lambda c: None
    for name, fn in SHEETS.items():
        save(fn(), 'block/rocket', name, (110, 30, 0, None), wrap=False)
    for name, cv in icons().items():
        save(cv, 'item', name)


# =========================================================================== launch facility blocks (16x16)

CONCRETE = ramp('#5a5853', '#6b6963', '#7b7972', '#8a8881', '#99978f', '#a8a69e')
ORANGE = ramp('#4f1c0c', '#7a2c12', '#a03d18', '#c0521f', '#d7692a', '#e8873e')
SCORCH = ramp('#1c1b1a', '#2a2826', '#3a3734')
SCREEN = ramp('#06140a', '#0b2412', '#12381b', '#1e5a2b', '#3a8f48', '#7ce07a')
BLUEPRINT = ramp('#0f2445', '#16335e', '#1f4479', '#2d5a94', '#7fa6d6', '#cfe0f4')


def block_noise(seed, r, lo, hi, passes=2, grain=0.25):
    cv = Canvas()
    n = clustered(16, 16, seed, passes=passes)
    g = clustered(16, 16, seed + 7, passes=0)
    for y in range(16):
        for x in range(16):
            v = n[y][x] * (1 - grain) + g[y][x] * grain
            cv[x, y] = r[int(round(lo + (hi - lo) * v))]
    return cv


def launch_pad_top():
    """Heat-resistant concrete slab with an embedded steel plate cross and bolt heads."""
    cv = block_noise(201, CONCRETE, 1, 4)
    for i in range(16):
        cv[i, 0] = CONCRETE[5]
        cv[0, i] = CONCRETE[5]
        cv[i, 15] = CONCRETE[0]
        cv[15, i] = CONCRETE[0]
    for x, y in ((3, 3), (12, 3), (3, 12), (12, 12)):
        cv[x, y] = STEEL[6]
        cv[x + 1, y + 1] = STEEL[2]
        cv[x + 1, y] = STEEL[4]
        cv[x, y + 1] = STEEL[4]
    # faint soot
    rng = random.Random(202)
    for _ in range(10):
        x, y = rng.randrange(1, 15), rng.randrange(1, 15)
        cv[x, y] = CONCRETE[0]
    return cv


def launch_pad_side():
    cv = block_noise(203, CONCRETE, 1, 4)
    for x in range(16):
        cv[x, 0] = STEEL[5]
        cv[x, 1] = STEEL[3]
        cv[x, 15] = CONCRETE[0]
    for x in (2, 7, 12):
        cv[x, 0] = STEEL[7]
    return cv


def flame_trench_top():
    """Steel grate over the trench: dark slots with the deflector glimpsed below."""
    cv = Canvas()
    for y in range(16):
        for x in range(16):
            bar = (x % 4 == 0) or (y in (0, 15))
            if bar:
                cv[x, y] = STEEL[5] if x % 4 == 0 and y % 5 else STEEL[4]
            else:
                cv[x, y] = SCORCH[0] if (x + y) % 5 else SCORCH[1]
    for x in range(0, 16, 4):
        for y in (5, 10):
            cv[x, y] = STEEL[7]
    for y in range(16):
        cv[15, y] = STEEL[2]
    return cv


def flame_trench_side():
    cv = block_noise(205, STEEL, 2, 4)
    for x in range(16):
        cv[x, 0] = STEEL[6]
        cv[x, 15] = STEEL[1]
        for y in (5, 10):
            cv[x, y] = STEEL[1]
            cv[x, y + 1] = STEEL[5]
    return cv


def service_tower():
    """Painted steel lattice: frame, X bracing and open bays (cut out)."""
    cv = Canvas()
    O = ORANGE
    for y in range(16):
        for x in range(16):
            frame = x in (0, 1, 14, 15) or y in (0, 1, 14, 15)
            diag = abs(x - y) <= 0 or abs((15 - x) - y) <= 0
            if frame:
                cv[x, y] = O[4] if (x in (0, 1) or y in (0, 1)) else O[2]
                if x in (0, 15) or y in (0, 15):
                    cv[x, y] = O[3] if (x == 0 or y == 0) else O[1]
            elif diag:
                cv[x, y] = O[3] if x < 8 else O[2]
            else:
                cv[x, y] = TRANSPARENT
    for x, y in ((1, 1), (14, 1), (1, 14), (14, 14), (7, 7), (8, 8), (7, 8), (8, 7)):
        cv[x, y] = STEEL[6]
    return cv


def service_tower_top():
    cv = block_noise(207, ORANGE, 2, 4, grain=0.15)
    for i in range(16):
        cv[i, 0] = ORANGE[5]
        cv[0, i] = ORANGE[5]
        cv[i, 15] = ORANGE[1]
        cv[15, i] = ORANGE[1]
    return cv


def tower_steel():
    return block_noise(209, STEEL, 2, 5, grain=0.2)


def crew_arm_walkway():
    cv = Canvas()
    for y in range(16):
        for x in range(16):
            grid = x % 3 == 0 or y % 3 == 0
            cv[x, y] = STEEL[5] if grid else STEEL[1]
    return cv


def crew_arm_rail():
    cv = Canvas()
    for y in range(16):
        for x in range(16):
            cv[x, y] = HAZARD[3] if (x + y) % 8 < 4 else CARBON[2]
    return cv


def fuel_umbilical_front():
    cv = block_noise(211, STEEL, 2, 4)
    # two quick-disconnect couplings: RP-1 (amber ring) and LOX (cyan ring)
    for cx, ring_ in ((5, P.BRASS), (10, P.OXYGEN_CYAN)):
        for y in range(6, 10):
            for x in range(cx - 2, cx + 2):
                edge = y in (6, 9) or x in (cx - 2, cx + 1)
                cv[x, y] = ring_[4] if edge else RUBBER[1]
    for x in range(16):
        cv[x, 0] = STEEL[6]
        cv[x, 15] = STEEL[1]
    return cv


def fuel_umbilical_hose():
    cv = Canvas()
    for y in range(16):
        for x in range(16):
            v = 2 + (1 if y % 4 == 0 else 0) + (1 if x < 6 else 0)
            cv[x, y] = RUBBER[min(5, v)]
    return cv


def console_body():
    cv = block_noise(213, ALU, 2, 4, grain=0.15)
    for i in range(16):
        cv[i, 0] = ALU[5]
        cv[0, i] = ALU[5]
        cv[i, 15] = ALU[1]
        cv[15, i] = ALU[1]
    for y in (11, 12, 13):
        for x in range(3, 13, 2):
            cv[x, y] = ALU[1]          # vent slots
    return cv


def console_top():
    cv = console_body()
    keycap = cv[8, 9]  # body steel, so the keys cost no extra colour
    # keyboard and two big buttons on the desk
    for y in range(3, 8):
        for x in range(2, 14):
            key = y in (4, 6) and x % 2 == 1 and 2 < x < 13
            cv[x, y] = keycap if key else CARBON[4]
    cv[4, 11], cv[5, 11], cv[4, 12], cv[5, 12] = RED[4], RED[4], RED[4], RED[4]
    cv[10, 11], cv[11, 11], cv[10, 12], cv[11, 12] = SCREEN[4], SCREEN[4], SCREEN[4], SCREEN[4]
    return cv


def console_screen():
    """Rows 0..7: monitor housing face; rows 8..13: the glowing display (green text lines and a trajectory)."""
    cv = Canvas()
    for y in range(16):
        for x in range(16):
            cv[x, y] = CARBON[2] if y < 8 else SCREEN[0]
    for x in range(1, 15):
        cv[x, 1] = CARBON[4]
    rng = random.Random(215)
    for y in (9, 11, 13):
        n = rng.randrange(4, 9)
        for x in range(2, 2 + n):
            cv[x, y] = SCREEN[3 if x % 3 else 4]
    # an ascent curve on the right half
    for i, x in enumerate(range(9, 15)):
        y = 13 - (i * i) // 6
        cv[x, y] = SCREEN[5]
    return cv


def console_panel():
    cv = console_body()
    for x, col in ((4, SCREEN[5]), (7, HAZARD[4]), (10, RED[5])):
        cv[x, 4] = col                 # status lamps
        cv[x, 5] = CARBON[1]
    return cv


def assembler_top():
    """Blueprint of a rocket on a steel workbench, with a ruler and a wrench."""
    cv = Canvas()
    for y in range(16):
        for x in range(16):
            cv[x, y] = STEEL[4]
    for y in range(1, 15):
        for x in range(1, 15):
            grid = x % 4 == 1 or y % 4 == 1
            cv[x, y] = BLUEPRINT[2] if grid else BLUEPRINT[1]
    # rocket outline in white
    pts = [(7, 3), (8, 3), (6, 4), (9, 4)] + [(6, y) for y in range(5, 12)] + [(9, y) for y in range(5, 12)] + \
          [(5, 10), (10, 10), (5, 11), (10, 11), (4, 12), (11, 12), (6, 12), (9, 12)]
    for x, y in pts:
        cv[x, y] = BLUEPRINT[5]
    for i in range(16):
        cv[i, 0] = STEEL[6]
        cv[0, i] = STEEL[6]
        cv[i, 15] = STEEL[2]
        cv[15, i] = STEEL[2]
    return cv


def assembler_side():
    cv = block_noise(217, STEEL, 2, 4)
    for x in range(16):
        cv[x, 0] = STEEL[6]
        cv[x, 1] = STEEL[5]
        cv[x, 15] = STEEL[1]
    for y in range(2, 15):
        cv[0, y] = STEEL[5]
        cv[15, y] = STEEL[1]
    return cv


def assembler_front():
    cv = assembler_side()
    # two drawers with handles
    for top in (3, 9):
        for y in range(top, top + 5):
            for x in range(2, 14):
                edge = y in (top, top + 4) or x in (2, 13)
                cv[x, y] = (STEEL[6] if (y == top or x == 2) else STEEL[1]) if edge else STEEL[3]
        for x in range(6, 10):
            cv[x, top + 2] = ALU[6]
    return cv


# =========================================================================== items

def propellant_cartridge():
    return grid_icon([
        "................",
        "......oooo......",
        ".....oSSsso.....",
        ".....oWWwwo.....",
        "....oWWWwwgo....",
        "....oAAAaaao....",
        "....oWWWwwgo....",
        "....oWWWwwgo....",
        "....oCCCccco....",
        "....oWWWwwgo....",
        "....oWWWwwgo....",
        "....oWWWwwgo....",
        "....oSSSsss.....",
        ".....oooooo.....",
        "................",
        "................",
    ], {'o': WHITE[0], 'W': WHITE[7], 'w': WHITE[5], 'g': WHITE[3], 'S': STEEL[6], 's': WHITE[3],
        'A': P.BRASS[5], 'a': P.BRASS[3], 'C': P.OXYGEN_CYAN[5], 'c': P.OXYGEN_CYAN[3]})


def emergency_beacon():
    return grid_icon([
        ".........o......",
        ".........o......",
        "........oSo.....",
        "......ooRRoo....",
        ".....oRrrrRro...",
        ".....oORRRRoo...",
        "....oOOOOOOooo..",
        "....oOOooOOOdo..",
        "....oOOooOOOdo..",
        "....oOOOOOOOdo..",
        "....oOOkkkOOdo..",
        "....oOOkkkOOdo..",
        "....oOOOOOOddo..",
        "....oddddddddo..",
        ".....oooooooo...",
        "................",
    ], {'o': ORANGE[0], 'O': ORANGE[4], 'd': ORANGE[2], 'R': RED[5], 'r': (255, 190, 150), 'S': STEEL[6],
        'k': CARBON[2]})


def parachute_canopy():
    """Top view of the canopy (32x32): alternating orange and white gores, a vent hole in the middle."""
    cv = Canvas(32, 32)
    for y in range(32):
        for x in range(32):
            dx, dy = x + 0.5 - 16, y + 0.5 - 16
            d = math.hypot(dx, dy)
            if d > 16.2:
                continue
            a = (math.atan2(dy, dx) + math.pi) / (2 * math.pi)
            gore = int(a * 12) % 2
            base = ORANGE[4] if gore else WHITE[6]
            if d < 2:
                base = CARBON[1]
            elif int(d) % 5 == 4:
                base = ORANGE[2] if gore else WHITE[3]
            cv[x, y] = base
    # rim colour row for the side faces (rows 0-3 of the 'side' uv)
    return cv


# =========================================================================== environment (sky) textures

# rough continents: (lon, lat) polygons in degrees, drawn by hand from the general shape of the map
CONTINENTS = [
    # North America
    [(-168, 66), (-140, 70), (-95, 72), (-78, 62), (-60, 55), (-66, 45), (-80, 32), (-82, 25), (-97, 26), (-105, 22),
     (-115, 30), (-124, 40), (-125, 49), (-135, 58), (-160, 59)],
    # Central America
    [(-105, 22), (-97, 18), (-88, 15), (-80, 8), (-77, 9), (-84, 15), (-90, 21), (-97, 26)],
    # South America
    [(-80, 9), (-60, 11), (-50, 0), (-35, -6), (-40, -22), (-58, -38), (-68, -55), (-75, -50), (-72, -30), (-81, -6)],
    # Greenland
    [(-55, 60), (-43, 60), (-20, 70), (-22, 82), (-60, 82), (-72, 77)],
    # Eurasia
    [(-10, 36), (-9, 44), (0, 50), (5, 58), (12, 64), (25, 71), (60, 70), (100, 77), (140, 72), (180, 68), (170, 60),
     (160, 52), (142, 46), (130, 35), (122, 30), (121, 22), (108, 18), (105, 9), (100, 14), (98, 8), (92, 22), (80, 15),
     (73, 20), (67, 25), (57, 25), (50, 30), (35, 36), (27, 40), (20, 38), (12, 44), (3, 42)],
    # Arabia
    [(35, 30), (43, 13), (52, 16), (59, 23), (50, 30)],
    # Africa
    [(-17, 15), (-16, 28), (-6, 36), (10, 37), (32, 31), (43, 12), (51, 11), (40, -15), (35, -25), (20, -35), (12, -18),
     (9, 4), (-8, 4)],
    # Madagascar
    [(44, -13), (50, -15), (47, -25), (43, -22)],
    # India (part of Eurasia polygon already; extra bulge)
    [(68, 23), (73, 20), (77, 8), (80, 13), (88, 22)],
    # Southeast Asia islands
    [(95, 5), (105, -6), (120, -9), (140, -8), (150, -6), (130, 0), (118, 5), (108, 2)],
    # Japan
    [(130, 31), (141, 36), (142, 45), (138, 40)],
    # Australia
    [(114, -22), (122, -18), (131, -12), (142, -11), (146, -19), (153, -26), (150, -37), (140, -38), (130, -32),
     (116, -35)],
    # New Zealand
    [(167, -46), (174, -41), (178, -38), (172, -35), (169, -43)],
    # British Isles
    [(-10, 52), (-5, 58), (0, 53), (-5, 50)],
    # Scandinavia is in Eurasia; Antarctica
    [(-180, -70), (-120, -73), (-60, -64), (0, -70), (60, -67), (120, -66), (180, -70), (180, -90), (-180, -90)],
]

OCEAN = ramp('#0a1b3c', '#0f2750', '#163563', '#1f4878', '#2f6290')
LAND = ramp('#2c4a1e', '#3b5f25', '#4f7430', '#6b7d3a', '#8a7a4a', '#a8915e', '#c4ab78')
ICE = ramp('#b8c4cf', '#d5dde5', '#eef2f6')


def _in_poly(x, y, pts):
    c = False
    j = len(pts) - 1
    for i in range(len(pts)):
        xi, yi = pts[i]
        xj, yj = pts[j]
        if ((yi > y) != (yj > y)) and (x < (xj - xi) * (y - yi) / (yj - yi + 1e-9) + xi):
            c = not c
        j = i
    return c


def smooth_noise(w, h, seed, cell, passes=1):
    """Seamless-ish smooth noise: a coarse clumpy grid upscaled bicubically (no blocky steps)."""
    from PIL import Image as _Img
    gw, gh = max(2, w // cell), max(2, h // cell)
    g = clustered(gw, gh, seed, passes=passes)
    im = _Img.new('F', (gw, gh))
    im.putdata([v for row in g for v in row])
    big = im.resize((w, h), _Img.BICUBIC)
    px = big.load()
    return [[min(1.0, max(0.0, px[x, y])) for x in range(w)] for y in range(h)]


def lon_of(x, w):
    return (x + 0.5) / w * 360 - 180


def earth_map(w=2048, h=1024):
    """Equirectangular Earth: continents rasterised from the outlines above, coasts roughened by noise,
    biome colours by latitude (forest, steppe, desert), ice caps, shallow seas along the coasts."""
    from PIL import Image as _Img, ImageDraw, ImageFilter
    big = _Img.new('L', (w, h), 0)
    d = ImageDraw.Draw(big)
    for poly in CONTINENTS:
        d.polygon([((lon + 180) / 360 * w, (90 - lat) / 180 * h) for lon, lat in poly], fill=255)
    n = smooth_noise(w, h, 301, 32, passes=1)
    fine = smooth_noise(w, h, 302, 6, passes=1)
    mask = big.load()
    shelf = big.filter(ImageFilter.MaxFilter(13)).load()
    cv = Canvas(w, h)
    for y in range(h):
        lat = 90 - (y + 0.5) / h * 180
        a = abs(lat)
        for x in range(w):
            nv = n[y][x]
            fv = fine[y][x]
            # roughen the coastline by sampling the mask a little off to the side
            sx = int(x + (nv - 0.5) * 18 + (fv - 0.5) * 6) % w
            sy = min(h - 1, max(0, int(y + (fv - 0.5) * 12)))
            land = mask[sx, sy] > 127
            if a > 82 - nv * 4 or (land and (lat < -62 or (lat > 60 and -60 < lon_of(x, w) < -18))):
                cv[x, y] = ICE[1 if fv > 0.5 else 2]
                continue
            if land:
                a = abs(lat) + (fv - 0.5) * 8 + (nv - 0.5) * 10   # wobble the climate belts
                if a > 60:
                    cv[x, y] = LAND[3] if fv > 0.45 else LAND[4]       # tundra
                    continue
                desert = 14 < a < 34 and nv > 0.33
                if desert:
                    v = 4 + (1 if fv > 0.5 else 0) + (1 if fv > 0.85 else 0)
                else:
                    v = (0 if a < 14 else 1) + (1 if nv > 0.55 else 0) + (1 if a > 45 and fv > 0.6 else 0)
                cv[x, y] = LAND[min(6, v)]
            else:
                coast = shelf[sx, sy] > 127
                cv[x, y] = OCEAN[3 if coast else (1 if nv < 0.45 else 2)]
    return cv


def earth_clouds(w=2048, h=1024):
    cv = Canvas(w, h)
    big = smooth_noise(w, h, 311, 20, passes=1)
    small = smooth_noise(w, h, 312, 6, passes=1)
    for y in range(h):
        lat = 90 - (y + 0.5) / h * 180
        band = 0.035 * math.cos(math.radians(lat) * 3)  # slightly cloudier storm belts
        for x in range(w):
            # bilinear-ish: smooth large swirls plus fine edges
            v = big[y][x] * 0.7 + small[y][x] * 0.3 + band
            if v > 0.73:
                a = 225 if v > 0.82 else 150
                cv[x, y] = (*WHITE[7], a)
            elif v > 0.695:
                cv[x, y] = (*WHITE[5], 90)
    return cv


def earth_limb():
    cv = Canvas(16, 16)
    for y in range(16):
        for x in range(16):
            cv[x, y] = (120, 185, 255, 255)
    return cv


MOON_ROCK = ramp('#3d3d3f', '#555456', '#6e6c6c', '#8a8886', '#a5a29e', '#bfbcb6')
MARIA = [((-20, 20), 22, 14), ((15, 25), 14, 10), ((30, 5), 12, 8), ((-35, -15), 14, 10), ((55, 15), 8, 6),
         ((-55, 10), 18, 26), ((0, -5), 9, 6), ((25, -15), 10, 7), ((40, -45), 6, 4)]


def moon_map(w=512, h=256):
    cv = Canvas(w, h)
    n = clustered(w, h, 321, passes=2)
    g = clustered(w, h, 322, passes=0)
    rng = random.Random(323)
    craters = [(rng.uniform(-180, 180), rng.uniform(-80, 80), rng.uniform(1.5, 7)) for _ in range(90)]
    for y in range(h):
        lat = 90 - (y + 0.5) / h * 180
        for x in range(w):
            lon = (x + 0.5) / w * 360 - 180
            v = 3 + (1 if n[y][x] > 0.55 else 0) - (1 if g[y][x] < 0.15 else 0)
            for (cl, ct), rl, rt in MARIA:
                if ((lon - cl) / rl) ** 2 + ((lat - ct) / rt) ** 2 < 1 + (n[y][x] - 0.5) * 0.6:
                    v = 1 + (1 if n[y][x] > 0.6 else 0)
            for cl, ct, r in craters:
                d = math.hypot((lon - cl) * math.cos(math.radians(lat)), lat - ct)
                if d < r:
                    v = 2 if d < r * 0.7 else 5
            cv[x, y] = MOON_ROCK[max(0, min(5, v))]
    return cv


def disc(tex_fn, w, h, size=64, limb=None):
    """A lit disc (sphere seen from afar, lit from the left) sampled from an equirectangular map."""
    src = tex_fn(w, h)
    cv = Canvas(size, size)
    r = size / 2 - 1
    for y in range(size):
        for x in range(size):
            dx, dy = (x + 0.5 - size / 2) / r, (y + 0.5 - size / 2) / r
            d2 = dx * dx + dy * dy
            if d2 > 1:
                if limb and d2 < 1.08:
                    cv[x, y] = limb
                continue
            dz = math.sqrt(1 - d2)
            lon = math.atan2(dx, dz)
            lat = math.asin(-dy)
            u = int((lon / (2 * math.pi) + 0.5) * w) % w
            v = int((0.5 - lat / math.pi) * h) % h
            c = src[u, v]
            light = max(0.12, min(1.0, (-0.55 * dx - 0.3 * dy + 0.75 * dz) * 1.25))
            cv[x, y] = (int(c[0] * light), int(c[1] * light), int(c[2] * light), 255)
    return cv


def rocket_plume():
    """Exhaust plume strip (16x64): white-hot core near the nozzle fading to orange, then transparent."""
    cv = Canvas(16, 64)
    for y in range(64):
        t = y / 63
        for x in range(16):
            c = abs(x + 0.5 - 8) / 8
            core = max(0.0, 1 - c * (1.4 + t))
            if core <= 0:
                continue
            fade = (1 - t) ** 1.3
            a = int(255 * min(1, core * 1.4) * fade)
            if a < 8:
                continue
            if t < 0.2 and c < 0.35:
                col = (255, 247, 220)
            elif t < 0.5:
                col = (255, 196, 92) if c < 0.5 else (240, 140, 50)
            else:
                col = (230, 120, 40) if c < 0.5 else (190, 80, 30)
            # shock diamonds
            if t < 0.6 and int(y) % 12 in (4, 5) and c < 0.3:
                col = (255, 255, 240)
            cv[x, y] = (*col, a)
    return cv


def rocket_plume_nuclear():
    """Nuclear thermal exhaust (16x64): hot hydrogen, almost invisible, a faint blue-violet core that fades fast."""
    cv = Canvas(16, 64)
    for y in range(64):
        t = y / 63
        for x in range(16):
            c = abs(x + 0.5 - 8) / 8
            core = max(0.0, 1 - c * (1.8 + t))
            if core <= 0:
                continue
            a = int(200 * min(1, core * 1.2) * (1 - t) ** 1.8)
            if a < 6:
                continue
            col = (225, 230, 255) if t < 0.15 and c < 0.3 else (170, 175, 255) if c < 0.5 else (150, 130, 240)
            cv[x, y] = (*col, a)
    return cv


def rocket_steam(i):
    """A billow of launch steam (16x16, tinted in game): a dense white-grey core with lumpy, soft translucent edges;
    three variants so neighbouring puffs never repeat."""
    rng = random.Random(7101 + i)
    cv = Canvas(16, 16)
    lumps = [(rng.uniform(4.5, 11.5), rng.uniform(4.5, 11.5), rng.uniform(3.0, 4.6)) for _ in range(5)]
    for y in range(16):
        for x in range(16):
            d = min(((x + 0.5 - cx) ** 2 + (y + 0.5 - cy) ** 2) ** 0.5 / r for cx, cy, r in lumps)
            d += rng.uniform(-0.08, 0.08)
            if d < 0.55:
                cv[x, y] = (246, 246, 244, 235)
            elif d < 0.8:
                cv[x, y] = (226, 226, 224, 180)
            elif d < 1.0:
                cv[x, y] = (206, 206, 204, 110)
            elif d < 1.2:
                cv[x, y] = (190, 190, 188, 45)
    return cv


def rocket_smoke_column():
    """Exhaust smoke column (16x64, tiles vertically): a soft-edged band of white-grey billows, denser in the middle."""
    rng = random.Random(7201)
    cv = Canvas(16, 64)
    bumps = [(rng.uniform(0, 64), rng.uniform(2.5, 4.5)) for _ in range(14)]
    for y in range(64):
        # the edge wanders: billows bulge out along the column (wraps at the tile seam)
        bulge = max(math.exp(-(min(abs(y - by), 64 - abs(y - by)) / w) ** 2) for by, w in bumps)
        half = 5.2 + 2.3 * bulge
        for x in range(16):
            c = abs(x + 0.5 - 8) / half
            if c >= 1:
                continue
            shade = 236 if c < 0.45 else 218 if c < 0.75 else 200
            a = int(230 * (1 - c ** 2.2))
            if a < 10:
                continue
            cv[x, y] = (shade, shade, shade - 2, a)
    return cv


def rocket_ground_glow():
    """Glow of the exhaust on the pad (16x16, additive): a hot yellow-white centre fading to orange at the rim."""
    cv = Canvas(16, 16)
    for y in range(16):
        for x in range(16):
            d = ((x + 0.5 - 8) ** 2 + (y + 0.5 - 8) ** 2) ** 0.5 / 8
            if d >= 1:
                continue
            a = int(255 * (1 - d) ** 1.6)
            if a < 8:
                continue
            col = (255, 240, 200) if d < 0.3 else (255, 190, 100) if d < 0.6 else (240, 130, 50)
            cv[x, y] = (*col, a)
    return cv


# =========================================================================== GUI backgrounds

GUI_BG, GUI_HI, GUI_LO, GUI_EDGE = (198, 198, 198), (255, 255, 255), (85, 85, 85), (0, 0, 0)
SLOT_IN, SLOT_DARK, SLOT_LIGHT = (139, 139, 139), (55, 55, 55), (255, 255, 255)


def gui_panel(cv, x0, y0, w, h):
    """Vanilla container panel: black rounded outline, white/dark bevel, light grey fill."""
    for y in range(y0, y0 + h):
        for x in range(x0, x0 + w):
            cv[x, y] = GUI_BG
    for x in range(x0 + 2, x0 + w - 2):
        cv[x, y0] = GUI_EDGE
        cv[x, y0 + h - 1] = GUI_EDGE
        cv[x, y0 + 1] = GUI_HI
        cv[x, y0 + 2] = GUI_HI
        cv[x, y0 + h - 2] = GUI_LO
        cv[x, y0 + h - 3] = GUI_LO
    for y in range(y0 + 2, y0 + h - 2):
        cv[x0, y] = GUI_EDGE
        cv[x0 + w - 1, y] = GUI_EDGE
        cv[x0 + 1, y] = GUI_HI
        cv[x0 + 2, y] = GUI_HI
        cv[x0 + w - 2, y] = GUI_LO
        cv[x0 + w - 3, y] = GUI_LO
    for (cx, cy) in ((x0 + 1, y0 + 1), (x0 + w - 2, y0 + h - 2)):
        cv[cx, cy] = GUI_EDGE
    cv[x0 + w - 2, y0 + 1] = GUI_EDGE
    cv[x0 + 1, y0 + h - 2] = GUI_EDGE
    cv[x0 + w - 3, y0 + 2] = GUI_BG
    cv[x0 + 2, y0 + h - 3] = GUI_BG
    for (cx, cy) in ((x0 + 3, y0 + 3), (x0 + w - 4, y0 + h - 4)):
        cv[cx, cy] = GUI_HI if cx == x0 + 3 else GUI_LO


def gui_slot(cv, x, y, size=18):
    for j in range(size):
        for i in range(size):
            cv[x + i, y + j] = SLOT_IN
    for i in range(size - 1):
        cv[x + i, y] = SLOT_DARK
        cv[x, y + i] = SLOT_DARK
        cv[x + size - 1 - i, y + size - 1] = SLOT_LIGHT
        cv[x + size - 1, y + size - 1 - i] = SLOT_LIGHT
    cv[x + size - 1, y] = SLOT_IN
    cv[x, y + size - 1] = SLOT_IN


def gui_inset(cv, x, y, w, h, fill):
    for j in range(h):
        for i in range(w):
            cv[x + i, y + j] = fill
    for i in range(w):
        cv[x + i, y] = SLOT_DARK
        cv[x + i, y + h - 1] = SLOT_LIGHT
    for j in range(h):
        cv[x, y + j] = SLOT_DARK
        cv[x + w - 1, y + j] = SLOT_LIGHT


def ghost(cv, x, y, icon, alpha=70):
    """A faint silhouette of the part a slot expects (like vanilla's empty armour slots)."""
    for j in range(16):
        for i in range(16):
            c = icon[i, j]
            if c[3] > 0:
                cv[x + 1 + i, y + 1 + j] = (110, 110, 110) if alpha else c


def gui_assembler():
    """512x256 sheet; the screen uses the top-left 300x222. Must match RocketAssemblerMenu/Screen."""
    cv = Canvas(512, 256)
    gui_panel(cv, 0, 0, 300, 222)
    ic = icons()
    slot_x, payload_y, stage_y0, gap = 26, 24, 50, 22
    # payload row: heat shield, capsule, parachute/nose
    for k, name in enumerate(('heat_shield', 'crew_capsule', 'parachute')):
        gui_slot(cv, slot_x + 18 + k * 18 - 1, payload_y - 1)
        ghost(cv, slot_x + 18 + k * 18 - 1, payload_y - 1, ic[name])
    expect = ('kerosene_engine', 'fuel_tank_small', 'fuel_tank_small', 'fuel_tank_small', 'fins', 'decoupler')
    for s in range(3):
        y = stage_y0 + (2 - s) * gap
        for k in range(6):
            gui_slot(cv, slot_x + k * 18 - 1, y - 1)
            ghost(cv, slot_x + k * 18 - 1, y - 1, ic[expect[k] if not (s == 2 and k == 5) else 'decoupler'])
    # thin "stack" rail left of the rows
    for y in range(payload_y, stage_y0 + 2 * gap + 16):
        cv[20, y] = (150, 150, 150)
    # output slot (big)
    ox, oy = 152, 112
    gui_slot(cv, ox - 5, oy - 5, 26)
    # arrow from the button to the output
    for i in range(6):
        for j in range(-i // 2, i // 2 + 1):
            pass
    # preview window: blueprint grid
    px, py, pw, ph = 182, 18, 110, 104
    gui_inset(cv, px - 1, py - 1, pw + 2, ph + 2, BLUEPRINT[0])
    for y in range(py, py + ph):
        for x in range(px, px + pw):
            if (x - px) % 10 == 0 or (y - py) % 10 == 0:
                cv[x, y] = BLUEPRINT[1]
    # stats window
    sx, sy, sw, sh = 182, 126, 110, 88
    gui_inset(cv, sx - 1, sy - 1, sw + 2, sh + 2, (36, 40, 46))
    # player inventory
    inv_y = 140
    for r in range(3):
        for c in range(9):
            gui_slot(cv, 7 + c * 18, inv_y - 1 + r * 18)
    for c in range(9):
        gui_slot(cv, 7 + c * 18, inv_y + 57)
    return cv


def gui_console():
    cv = Canvas(256, 256)
    gui_panel(cv, 0, 0, 236, 176)
    # the monitor
    x, y, w, h = 8, 18, 220, 124
    gui_inset(cv, x - 1, y - 1, w + 2, h + 2, SCREEN[0])
    for j in range(y, y + h):
        if (j - y) % 2 == 1:
            for i in range(x, x + w):
                cv[i, j] = (5, 17, 9)       # scanlines
    # divider between checklist and clock
    for j in range(y + 4, y + h - 4):
        cv[x + 150, j] = SCREEN[2]
    return cv


def _merge_cost(pq, counts):
    a, b = pq
    d = sum((u - v) ** 2 for u, v in zip(a, b))
    # accents (saturated colours such as lights and port rings) are kept unless their own shades meet
    accent = max(a[:3]) - min(a[:3]) > 70 or max(b[:3]) - min(b[:3]) > 70
    return d * (40 if accent else min(counts[a], counts[b]) ** 0.5)


def limit_colours(cv, n=6):
    """Merge the two closest colours (the rarer into the commoner) until at most n remain (STYLE.md colour
    budget), weighted by how rare the rarer one is, so stray shades collapse first and small accents such as lights keep their colour."""
    counts = {}
    for y in range(cv.h):
        for x in range(cv.w):
            c = cv[x, y]
            if c[3] > 0:
                counts[c] = counts.get(c, 0) + 1
    remap = {}
    while len(counts) > n:
        cs = list(counts)
        a, b = min(((p, q) for i, p in enumerate(cs) for q in cs[i + 1:]),
                   key=lambda pq: _merge_cost(pq, counts))
        rare, near = (a, b) if counts[a] <= counts[b] else (b, a)
        counts[near] += counts.pop(rare)
        remap[rare] = near
        for k, v in list(remap.items()):
            if v == rare:
                remap[k] = near
    for y in range(cv.h):
        for x in range(cv.w):
            c = cv[x, y]
            if c in remap:
                cv[x, y] = remap[c]
    return cv


HULL = ramp('#8c9096', '#b4b8bd', '#cfd2d6', '#e1e3e6', '#eef0f2', '#f8f9fa')


def station_hull():
    """White insulated hull panel with a seam frame and four rivets."""
    cv = block_noise(301, HULL, 3, 5, passes=1, grain=0.2)
    for i in range(16):
        cv[i, 0] = HULL[5]
        cv[0, i] = HULL[5]
        cv[i, 15] = HULL[1]
        cv[15, i] = HULL[1]
    for x, y in ((2, 2), (13, 2), (2, 13), (13, 13)):
        cv[x, y] = HULL[0]
    for x in range(5, 11):
        cv[x, 8] = HULL[2]                                  # panel seam across the middle
    return cv


def station_window():
    """Thick window frame around a glass pane (transparent pixels let the view through)."""
    cv = Canvas()
    for y in range(16):
        for x in range(16):
            frame = x < 2 or y < 2 or x > 13 or y > 13
            if frame:
                cv[x, y] = HULL[4] if (x < 1 or y < 1) else HULL[1] if (x > 14 or y > 14) else HULL[3]
            else:
                glint = (x - y) in (4, 5) and x < 10
                cv[x, y] = (230, 240, 255, 150) if glint else (120, 160, 200, 70)
    return cv


def window_part(cv, frame):
    """The opaque pixels of a window (frame) or the see-through ones (pane); everything else fully clear."""
    out = Canvas()
    for y in range(16):
        for x in range(16):
            c = cv[x, y]
            if c is not None and len(c) == 4 and ((c[3] == 255) == frame) and c[3] > 0:
                out[x, y] = c
            elif c is not None and len(c) == 3 and frame:
                out[x, y] = c
    return out


def docking_port_top():
    """Looking at the port: grey docking ring, dark hatch and yellow alignment marks."""
    cv = Canvas()
    for y in range(16):
        for x in range(16):
            d = math.hypot(x - 7.5, y - 7.5)
            if d > 7.6:
                c = HULL[3]
            elif d > 6.1:
                c = STEEL[5]
            elif d > 4.6:
                c = STEEL[3]
            else:
                c = CARBON[3] if d > 1.5 else CARBON[4]
            cv[x, y] = c
    for x, y in ((7, 0), (8, 0), (0, 7), (0, 8), (15, 7), (15, 8), (7, 15), (8, 15)):
        cv[x, y] = HAZARD[3]
    return cv


def docking_port_side():
    cv = station_hull()
    for x in range(16):
        cv[x, 1] = HAZARD[3] if x % 4 < 2 else CARBON[2]       # warning band below the ring
    return cv


def station_hatch(center):
    """A mouth hatch panel: white plate with a hazard-striped rim on the outer edges; the middle one has the round
    hatch with its handwheel."""
    cv = station_hull()
    for y in range(16):
        for x in range(16):
            if x in (0, 15) or y in (0, 15):
                cv[x, y] = HAZARD[3] if (x + y) // 2 % 2 else CARBON[2]
    if center:
        for y in range(16):
            for x in range(16):
                d = math.hypot(x - 7.5, y - 7.5)
                if 4.6 < d <= 6.1:
                    cv[x, y] = STEEL[4]
                elif d <= 4.6:
                    cv[x, y] = STEEL[2] if d > 3.2 else HULL[4]
        for x, y in ((7, 5), (8, 5), (5, 7), (5, 8), (10, 7), (10, 8), (7, 10), (8, 10), (7, 7), (8, 8), (7, 8), (8, 7)):
            cv[x, y] = CARBON[3]
    return cv


def station_core_front():
    """The core console: hull frame around a green status screen showing the station's modules."""
    cv = station_hull()
    for y in range(3, 11):
        for x in range(2, 14):
            cv[x, y] = SCREEN[1] if (x in (2, 13) or y in (3, 10)) else SCREEN[0]
    for x, y in ((5, 6), (6, 6), (7, 6), (8, 6), (9, 6), (7, 5), (7, 7), (4, 6), (10, 6)):
        cv[x, y] = SCREEN[5]
    for x in (4, 7, 10):
        cv[x, 8] = SCREEN[3]
    for x in range(3, 13, 3):
        cv[x, 13] = HAZARD[3]
        cv[x + 1, 13] = CARBON[2]
    return cv


def station_core_top():
    cv = station_hull()
    for x in range(3, 13):
        cv[x, 7] = CARBON[2]
        cv[x, 8] = CARBON[3]
    return cv


def station_kit_icon():
    cv = Canvas()
    rows = [
        "................",
        "..oooooooooooo..",
        ".oWWWWWWWWWWWWo.",
        ".oWwwwwwwwwwwWo.",
        ".oWwYYYYYYYYwWo.",
        ".oWwYkkkkkkYwWo.",
        ".oWwYkWWWWkYwWo.",
        ".oWwYkWWWWkYwWo.",
        ".oWwYkkkkkkYwWo.",
        ".oWwYYYYYYYYwWo.",
        ".oWwwwwwwwwwwWo.",
        ".oWWWWWWWWWWWWo.",
        "..oooooooooooo..",
        "....ss....ss....",
        "...ssss..ssss...",
        "................",
    ]
    pal = {'o': CARBON[0], 'W': HULL[5], 'w': HULL[2], 'Y': HAZARD[3], 'k': CARBON[3], 's': STEEL[3]}
    for y, row in enumerate(rows):
        for x, ch in enumerate(row):
            if ch in pal:
                cv[x, y] = pal[ch]
    return cv


BAG = ramp('#7a3210', '#a8461a', '#cc5e22', '#e07a34', '#f0a050', '#f8c890')


def station_bunk_top():
    """Orange sleeping bag seen from above: pillow at the head (north), two grey straps holding it down."""
    cv = Canvas()
    for y in range(16):
        for x in range(16):
            edge = x in (0, 15) or y in (0, 15)
            if edge:
                c = STEEL[2]
            elif y < 5:
                c = HULL[4] if 2 <= x <= 13 and 1 <= y <= 3 else BAG[1]       # pillow
            else:
                c = BAG[3] if (x + y) % 5 else BAG[2]
                if x in (1, 14):
                    c = BAG[1]
            cv[x, y] = c
    for y in (7, 12):
        for x in range(1, 15):
            cv[x, y] = STEEL[4] if x % 6 else STEEL[2]                         # straps with buckles
    for y in range(6, 15):
        cv[8, y] = BAG[5] if y % 2 else cv[8, y]                               # zip
    return cv


def station_bunk_side():
    cv = Canvas()
    for y in range(16):
        for x in range(16):
            if y < 9:
                cv[x, y] = (0, 0, 0, 0)
            elif y >= 14:
                cv[x, y] = STEEL[2] if y == 15 else STEEL[4]                   # frame
            else:
                cv[x, y] = BAG[3] if y > 10 else BAG[4]
    for y in range(9, 14):
        cv[4, y] = STEEL[4]
        cv[11, y] = STEEL[4]
    return cv


def fuel_depot_side():
    """Hull panel with four sight gauges, one per liquid: RP-1, oxygen, methane, hydrogen."""
    cv = station_hull()
    gauges = ((2, (196, 150, 70)), (5, (120, 190, 240)), (9, (150, 200, 120)), (12, (220, 230, 250)))
    for x0, colour in gauges:
        for y in range(3, 13):
            for x in (x0, x0 + 1):
                cv[x, y] = colour if y >= 6 else CARBON[3]
        cv[x0, 2] = STEEL[2]
        cv[x0 + 1, 2] = STEEL[2]
        cv[x0, 13] = STEEL[2]
        cv[x0 + 1, 13] = STEEL[2]
    for x in range(16):
        cv[x, 15] = HAZARD[3] if x % 4 < 2 else CARBON[2]
    return cv


def fuel_depot_top():
    """Filler caps on a hull plate."""
    cv = station_hull()
    for cx, cy in ((4, 4), (11, 4), (4, 11), (11, 11)):
        for y in range(16):
            for x in range(16):
                d = math.hypot(x - cx, y - cy)
                if d < 2.6:
                    cv[x, y] = STEEL[4] if d > 1.4 else STEEL[2]
    return cv


def landing_beacon_top():
    """Landing pad marker seen from above: dark plate, yellow ring and a white H, red lights at the corners."""
    cv = Canvas()
    for y in range(16):
        for x in range(16):
            d = math.hypot(x - 7.5, y - 7.5)
            c = CARBON[2] if (x + y) % 3 else CARBON[3]
            if 5.6 < d < 7.0:
                c = HAZARD[3]
            cv[x, y] = c
    for y in range(4, 12):
        cv[5, y] = HULL[5]
        cv[10, y] = HULL[5]
    for x in range(6, 10):
        cv[x, 7] = HULL[5]
        cv[x, 8] = HULL[4]
    for x, y in ((0, 0), (15, 0), (0, 15), (15, 15)):
        cv[x, y] = (230, 60, 50)
    return cv


def landing_beacon_side():
    cv = station_hull()
    for x in range(16):
        cv[x, 0] = HAZARD[3] if x % 4 < 2 else CARBON[2]
        cv[x, 1] = HAZARD[2] if x % 4 < 2 else CARBON[1]
    cv[7, 4] = (230, 60, 50)
    cv[8, 4] = (230, 60, 50)
    return cv


AMBER = ramp('#5a2a04', '#8a4508', '#c06a0e', '#e8901a', '#ffb830', '#ffd870', '#fff2c0')


def warning_light_base():
    """Dark steel housing with four screws."""
    cv = Canvas()
    for y in range(16):
        for x in range(16):
            cv[x, y] = STEEL[2] if (x + y) % 5 else STEEL[1]
    for x in range(16):
        cv[x, 0] = STEEL[4]
        cv[0, x] = STEEL[4]
        cv[x, 15] = STEEL[0]
        cv[15, x] = STEEL[0]
    for x, y in ((3, 3), (12, 3), (3, 12), (12, 12)):
        cv[x, y] = STEEL[5]
    return cv


def warning_light_lens(phase=None):
    """Ribbed amber dome. With a phase (0..7) a bright beam sweeps round it, frame by frame."""
    cv = Canvas()
    for y in range(16):
        for x in range(16):
            rib = x % 3 == 0
            if phase is None:
                c = AMBER[1] if rib else AMBER[2]
            else:
                # the mirror's beam: brightest at its centre column, wrapping round the dome
                d = min((x - phase * 2) % 16, (phase * 2 - x) % 16)
                c = AMBER[6] if d == 0 else AMBER[5] if d <= 2 else AMBER[4]
                if rib and d > 2:
                    c = AMBER[3]
            cv[x, y] = c
    return cv


ALPANEL = ramp('#7e848c', '#9aa0a8', '#b2b8bf', '#c6cbd1', '#d8dce0', '#e8ebee')
AL_BRUSH = ramp('#7f8994', '#9ea7b1', '#b4bcc5', '#bec6ce', '#d2d8de')
TITAN = ramp('#3e444c', '#4f565f', '#5f6772', '#717a86', '#86909c')


def aluminum_panel():
    """Brushed aluminium panel: soft horizontal brush streaks, a seam frame and corner rivets, low contrast."""
    cv = Canvas()
    for y in range(16):
        for x in range(16):
            cv[x, y] = AL_BRUSH[2]
        # one long thin streak per row, lighter on darker rows and darker on lighter rows
        x0 = (2, 9, 5, 12, 1, 7, 14, 4, 10, 3, 13, 6, 0, 11, 8, 15)[y]
        length = (6, 4, 8, 5, 7, 3, 6, 9, 4, 7, 5, 8, 3, 6, 9, 5)[y]
        for k in range(length):
            x = (x0 + k) % 16
            cv[x, y] = AL_BRUSH[3]
    # panel seam: light top-left edge, darker bottom-right edge
    for i in range(16):
        cv[i, 0] = AL_BRUSH[4]
        cv[0, i] = AL_BRUSH[4]
        cv[i, 15] = AL_BRUSH[1]
        cv[15, i] = AL_BRUSH[1]
    for x, y in ((2, 2), (13, 2), (2, 13), (13, 13)):
        cv[x, y] = AL_BRUSH[0]
        cv[x - 1, y - 1] = AL_BRUSH[4]
    return cv


def titanium_plate():
    """Heavy dark titanium deck plate with raised diamond tread and hex bolts."""
    cv = Canvas()
    for y in range(16):
        for x in range(16):
            tread = (x + y) % 4 == 0 and (x - y) % 8 == 0
            cv[x, y] = TITAN[4] if tread else TITAN[2] if (x * 5 + y * 3) % 7 else TITAN[1]
    for i in range(16):
        cv[i, 0] = TITAN[3]
        cv[0, i] = TITAN[3]
        cv[i, 15] = TITAN[0]
        cv[15, i] = TITAN[0]
    for x, y in ((3, 3), (12, 3), (3, 12), (12, 12)):
        cv[x, y] = TITAN[4]
        cv[x + 1, y] = TITAN[3]
    return cv


def pressure_glass():
    """Thick laminated glass in a narrow steel frame (frame and pane split for the composite model)."""
    cv = Canvas()
    for y in range(16):
        for x in range(16):
            if x == 0 or y == 0 or x == 15 or y == 15:
                cv[x, y] = ALPANEL[4] if (x == 0 or y == 0) else ALPANEL[1]
            else:
                glint = (x - y) in (3, 4) and 4 < x < 11
                layer = x in (1, 14) or y in (1, 14)
                cv[x, y] = (235, 245, 255, 160) if glint else (150, 190, 215, 110) if layer else (130, 175, 205, 80)
    return cv


def porthole():
    """A round window set in a riveted hull plate."""
    cv = Canvas()
    for y in range(16):
        for x in range(16):
            d = math.hypot(x - 7.5, y - 7.5)
            if d < 4.6:
                glint = (x - y) in (0, 1) and x < 8
                cv[x, y] = (235, 245, 255, 160) if glint else (130, 175, 205, 90)
            elif d < 6.2:
                cv[x, y] = STEEL[4] if d < 5.4 else STEEL[2]
            else:
                cv[x, y] = HULL[3] if (x + y) % 6 else HULL[2]
    for a in range(8):
        ang = a * math.pi / 4
        cv[int(round(7.5 + 5.4 * math.cos(ang))), int(round(7.5 + 5.4 * math.sin(ang)))] = STEEL[2]   # bolts round the ring
    return cv


def grate():
    """Open steel grating: bars with square holes you can see through."""
    cv = Canvas()
    for y in range(16):
        for x in range(16):
            bar = x % 4 == 0 or y % 4 == 0 or x == 15 or y == 15
            if bar:
                cv[x, y] = STEEL[4] if (x % 4 == 0 and y % 4 == 0) else STEEL[3] if y % 4 == 0 else STEEL[2]
    return cv


def sliding_door(upper):
    """One half of a two-leaf sliding door: aluminium leaves meeting in the middle, a hazard band at the foot,
    a small window in the upper half."""
    cv = Canvas()
    for y in range(16):
        for x in range(16):
            c = ALPANEL[3] if (x * 3 + y * 7) % 9 else ALPANEL[2]
            if x in (7, 8):
                c = STEEL[1] if x == 7 else STEEL[3]          # where the leaves meet
            if x in (0, 15):
                c = STEEL[2]
            cv[x, y] = c
    if upper:
        for y in range(3, 9):
            for x in range(3, 13):
                if x in (7, 8):
                    continue
                edge = y in (3, 8) or x in (3, 12)
                cv[x, y] = STEEL[2] if edge else (120, 165, 195) if (x - y) % 5 else (190, 220, 240)
        for x in range(1, 15):
            cv[x, 0] = STEEL[1]
    else:
        for y in range(11, 15):
            for x in range(1, 15):
                if x not in (7, 8):
                    cv[x, y] = HAZARD[3] if ((x + y) // 2) % 2 else CARBON[2]   # yellow-black chevrons at the foot
        for x in range(1, 15):
            cv[x, 15] = STEEL[1]
    return cv


def door_frame():
    cv = Canvas()
    for y in range(16):
        for x in range(16):
            cv[x, y] = STEEL[3] if (x + y) % 4 else STEEL[2]
    return cv


def hazard_stripe():
    """Diagonal yellow and black stripes."""
    cv = Canvas()
    for y in range(16):
        for x in range(16):
            band = ((x + y) // 4) % 2 == 0
            cv[x, y] = (HAZARD[3] if (x * 3 + y) % 7 else HAZARD[2]) if band else (CARBON[1] if (x + y * 3) % 5 else CARBON[2])
    return cv


def led_panel():
    """A flush LED light panel: bright diffuser in a thin frame."""
    cv = Canvas()
    for y in range(16):
        for x in range(16):
            edge = x in (0, 15) or y in (0, 15)
            inner = x in (1, 14) or y in (1, 14)
            cv[x, y] = ALPANEL[1] if edge else ALPANEL[4] if inner else (250, 252, 255) if (x + y) % 6 else (236, 242, 250)
    return cv


def floodlight_face():
    """The lamp face: a bright lens behind a grille."""
    cv = Canvas()
    for y in range(16):
        for x in range(16):
            d = math.hypot(x - 7.5, y - 7.5)
            if d > 7.2:
                c = STEEL[1]
            elif y % 3 == 0:
                c = STEEL[3]
            else:
                c = (255, 252, 230) if d < 4 else (240, 232, 190)
            cv[x, y] = c
    return cv


def floodlight_side():
    cv = Canvas()
    for y in range(16):
        for x in range(16):
            cv[x, y] = STEEL[2] if y % 2 else STEEL[1]      # cooling fins
    return cv


def cable_duct():
    """A grey wall cover; through its slots, coloured cable runs."""
    cv = Canvas()
    colours = ((200, 60, 50), (60, 120, 200), (230, 190, 60), (60, 160, 90))
    for y in range(16):
        for x in range(16):
            cv[x, y] = ALPANEL[2] if (x + y) % 5 else ALPANEL[1]
    for i, y0 in enumerate((3, 7, 11)):
        for x in range(2, 14):
            cv[x, y0] = CARBON[1]
            cv[x, y0 + 1] = colours[(x // 3 + i) % 4] if x % 3 else CARBON[2]
    for x, y in ((1, 1), (14, 1), (1, 14), (14, 14)):
        cv[x, y] = ALPANEL[4]
    return cv


def pipe_duct():
    """A cover with two pipes showing through: a cold blue one and a warm red one, with flanges."""
    cv = Canvas()
    for y in range(16):
        for x in range(16):
            cv[x, y] = ALPANEL[2] if (x + y) % 5 else ALPANEL[1]
    for x0, col in ((3, ((90, 140, 200), (60, 100, 160))), (10, ((200, 90, 70), (150, 60, 50)))):
        for y in range(16):
            for x in range(x0, x0 + 3):
                cv[x, y] = col[0] if x == x0 + 1 else col[1]
            if y in (4, 12):
                for x in range(x0 - 1, x0 + 4):
                    cv[x, y] = STEEL[3]
    return cv


def steel_ladder():
    cv = Canvas()
    for y in range(16):
        for x in (2, 3, 12, 13):
            cv[x, y] = STEEL[3] if x in (2, 12) else STEEL[1]
        if y % 4 == 1:
            for x in range(4, 12):
                cv[x, y] = STEEL[4]
                cv[x, y + 1] = STEEL[1]
    return cv


def railing():
    """Yellow safety railing: two rails and posts."""
    cv = Canvas()
    for y in range(16):
        for x in range(16):
            if y in (1, 2, 8, 9) or x in (7, 8):
                cv[x, y] = HAZARD[3] if y in (1, 8) or x == 7 else HAZARD[1]
    return cv


def habitat_kit_icon():
    cv = Canvas()
    rows = [
        "......ssss......",
        "......SSSS......",
        "..oooooooooooo..",
        ".oWWWWWWWWWWWWo.",
        ".oWbbWWWWWWbbWo.",
        ".oWbbWWWWWWbbWo.",
        ".oWWWWkkkkWWWWo.",
        ".oWWWWkYYkWWWWo.",
        ".oWwwwkYYkwwwWo.",
        ".oWwwwkkkkwwwWo.",
        ".oWWWWWWWWWWWWo.",
        "..oooooooooooo..",
        "....ss....ss....",
        "...ssss..ssss...",
        "................",
        "................",
    ]
    pal = {'o': CARBON[0], 'W': HULL[5], 'w': HULL[2], 'Y': HAZARD[3], 'k': CARBON[3], 's': STEEL[3],
           'S': PANEL[3], 'b': PANEL[4]}
    for y, row in enumerate(rows):
        for x, ch in enumerate(row):
            if ch in pal:
                cv[x, y] = pal[ch]
    return cv


def export_facility(save):
    blk = (100, 30, 0, None)
    save(limit_colours(launch_pad_top()), 'block', 'launch_pad_top', blk)
    save(limit_colours(station_hull()), 'block', 'station_hull', (180, 30, 0, None))
    save(station_window(), 'block', 'station_window', (200, 20, 0, None))
    # split for the composite model: opaque frame (cutout layer) and tinted pane (translucent layer)
    save(window_part(station_window(), True), 'block', 'station_window_frame', (200, 20, 0, None))
    save(window_part(station_window(), False), 'block', 'station_window_pane', (200, 20, 0, None))
    save(limit_colours(docking_port_top()), 'block', 'docking_port_top', (180, 30, 0, None))
    save(limit_colours(docking_port_side()), 'block', 'docking_port_side', (180, 30, 0, None))
    save(station_kit_icon(), 'item', 'station_kit')
    save(limit_colours(station_hatch(False)), 'block', 'station_hatch', (180, 30, 0, None))
    save(limit_colours(station_hatch(True)), 'block', 'station_hatch_center', (180, 30, 0, None))
    save(limit_colours(station_core_front()), 'block', 'station_core_front', (180, 30, 0, None))
    save(limit_colours(station_core_top()), 'block', 'station_core_top', (180, 30, 0, None))
    save(habitat_kit_icon(), 'item', 'habitat_kit')
    save(limit_colours(warning_light_base()), 'block', 'warning_light_base', (140, 230, 0, None))
    save(limit_colours(aluminum_panel()), 'block', 'aluminum_panel', (170, 230, 0, None))
    save(limit_colours(titanium_plate()), 'block', 'titanium_deck', (150, 230, 0, None))
    for name, fn in (('pressure_glass', pressure_glass), ('porthole', porthole)):
        save(fn(), 'block', name, (200, 20, 0, None))
        save(window_part(fn(), True), 'block', name + '_frame', (200, 20, 0, None))
        save(window_part(fn(), False), 'block', name + '_pane', (200, 20, 0, None))
    save(grate(), 'block', 'grate', (140, 230, 0, None))
    save(limit_colours(sliding_door(False)), 'block', 'sliding_door_lower', (170, 230, 0, None))
    save(limit_colours(sliding_door(True)), 'block', 'sliding_door_upper', (170, 230, 0, None))
    save(limit_colours(door_frame()), 'block', 'sliding_door_frame', (140, 230, 0, None))
    save(limit_colours(hazard_stripe()), 'block', 'hazard_stripe', (120, 230, 0, None))
    save(limit_colours(led_panel()), 'block', 'led_panel', (220, 20, 0, None))
    save(limit_colours(floodlight_face()), 'block', 'floodlight_face', (220, 20, 0, None))
    save(limit_colours(floodlight_side()), 'block', 'floodlight_side', (140, 230, 0, None))
    save(limit_colours(cable_duct()), 'block', 'cable_duct', (150, 230, 0, None))
    save(limit_colours(pipe_duct()), 'block', 'pipe_duct', (150, 230, 0, None))
    save(steel_ladder(), 'block', 'steel_ladder')
    save(railing(), 'block', 'railing')
    icon = Canvas()
    lower, upper = sliding_door(False), sliding_door(True)
    for y in range(16):
        for x in range(4, 12):
            src = upper if y < 8 else lower
            icon[x, y] = src[(x - 4) * 2, (y % 8) * 2]
    save(icon, 'item', 'sliding_door')
    save(limit_colours(warning_light_lens()), 'block', 'warning_light_lens', (200, 20, 0, None))
    # lit lens: 8 frames of a beam sweeping round the dome
    frames = Canvas(16, 128)
    for f in range(8):
        frame = warning_light_lens(f)
        for y in range(16):
            for x in range(16):
                frames[x, f * 16 + y] = frame[x, y]
    path = save(frames, 'block', 'warning_light_lens_on')
    with open(path + '.mcmeta', 'w') as fh:
        fh.write('{\n  "animation": {\n    "frametime": 2\n  }\n}\n')
    save(limit_colours(landing_beacon_top()), 'block', 'landing_beacon_top', (180, 30, 0, None))
    save(limit_colours(landing_beacon_side()), 'block', 'landing_beacon_side', (180, 30, 0, None))
    save(limit_colours(station_bunk_top()), 'block', 'station_bunk_top', (180, 30, 0, None))
    save(station_bunk_side(), 'block', 'station_bunk_side', (180, 30, 0, None))
    save(limit_colours(fuel_depot_side()), 'block', 'fuel_depot_side', (180, 30, 0, None))
    save(limit_colours(fuel_depot_top()), 'block', 'fuel_depot_top', (180, 30, 0, None))
    save(limit_colours(launch_pad_side()), 'block', 'launch_pad_side', blk)
    save(flame_trench_top(), 'block', 'flame_trench_top', (140, 230, 0, None))
    save(flame_trench_side(), 'block', 'flame_trench_side', (140, 230, 0, None))
    save(service_tower(), 'block', 'service_tower', (90, 30, 0, None), wrap=False)
    save(service_tower_top(), 'block', 'service_tower_top', (90, 30, 0, None))
    save(tower_steel(), 'block', 'tower_steel', (140, 230, 0, None))
    save(crew_arm_walkway(), 'block', 'crew_arm_walkway', (140, 230, 0, None))
    save(crew_arm_rail(), 'block', 'crew_arm_rail', (90, 30, 0, None))
    save(limit_colours(fuel_umbilical_front()), 'block', 'fuel_umbilical_front', (140, 230, 0, None), wrap=False)
    save(fuel_umbilical_hose(), 'block', 'fuel_umbilical_hose', (60, 20, 0, None))
    screen_rule = lambda c: (200, 20, 0, 200) if c[1] > c[0] + 20 and c[1] > 40 else None
    save(console_body(), 'block', 'console_body', (150, 232, 0, None), wrap=False)
    save(limit_colours(console_top()), 'block', 'console_top', (150, 232, 0, screen_rule), wrap=False)
    save(console_screen(), 'block', 'console_screen', (200, 20, 0, screen_rule), wrap=False)
    save(limit_colours(console_panel()), 'block', 'console_panel', (150, 232, 0, None), wrap=False)
    save(limit_colours(assembler_top()), 'block', 'rocket_assembler_top', (80, 20, 0, None), wrap=False)
    save(assembler_side(), 'block', 'rocket_assembler_side', (140, 230, 0, None), wrap=False)
    save(limit_colours(assembler_front()), 'block', 'rocket_assembler_front', (140, 230, 0, None), wrap=False)
    save(parachute_canopy(), 'block/rocket', 'parachute_canopy')
    save(propellant_cartridge(), 'item', 'propellant_cartridge')
    save(emergency_beacon(), 'item', 'emergency_beacon')
    save(earth_map(), 'environment', 'orbit_earth')
    save(earth_clouds(), 'environment', 'orbit_earth_clouds')
    save(earth_limb(), 'environment', 'orbit_earth_limb')
    save(moon_map(), 'environment', 'orbit_moon')
    save(disc(earth_map, 2048, 1024, limb=(110, 170, 240, 120)), 'environment', 'orbit_earth_disc')
    save(disc(moon_map, 512, 256), 'environment', 'orbit_moon_disc')
    save(rocket_plume(), 'entity', 'rocket_plume')
    save(rocket_plume_nuclear(), 'entity', 'rocket_plume_nuclear')
    save(rocket_smoke_column(), 'entity', 'rocket_smoke_column')
    save(rocket_ground_glow(), 'entity', 'rocket_ground_glow')
    for i in range(3):
        save(rocket_steam(i), 'particle', f'rocket_steam_{i}')
    save(gui_assembler(), 'gui', 'rocket_assembler')
    save(gui_console(), 'gui', 'launch_console')


_export_parts = export


def export(save):  # noqa: F811
    _export_parts(save)
    export_facility(save)


# =========================================================================== other worlds seen from orbit

ROCK_RAMP = ramp('#2e2b29', '#3f3b38', '#524d49', '#66605b', '#7a736d', '#8f8780')
GAS_RAMP = ramp('#5b4632', '#7a6047', '#987a5c', '#b39674', '#cbb08e', '#ddc8a8', '#ece0c6')
ICE_RAMP = ramp('#2c4f6b', '#3b6584', '#4d7c9c', '#6293b2', '#7aa9c4', '#97bfd4', '#b8d6e4')


def cratered(w, h, r, seed, maria=(), craters=70, caps=None):
    """Generic rocky world map: mottled ground, darker plains, bright-rimmed craters, optional polar caps."""
    cv = Canvas(w, h)
    n = smooth_noise(w, h, seed, 16)
    f = smooth_noise(w, h, seed + 1, 3)
    rng = random.Random(seed + 2)
    cr = [(rng.uniform(-180, 180), rng.uniform(-75, 75), rng.uniform(1.5, 8)) for _ in range(craters)]
    top = len(r) - 1
    for y in range(h):
        lat = 90 - (y + 0.5) / h * 180
        for x in range(w):
            lon = (x + 0.5) / w * 360 - 180
            v = 2 + n[y][x] * (top - 2) + (f[y][x] - 0.5) * 1.2
            for (cl, ct), rl, rt in maria:
                if ((lon - cl) / rl) ** 2 + ((lat - ct) / rt) ** 2 < 1 + (n[y][x] - 0.5) * 0.8:
                    v -= 1.6
            for cl, ct, rad in cr:
                dd = math.hypot((lon - cl) * math.cos(math.radians(lat)), lat - ct)
                if dd < rad:
                    v = 1.2 if dd < rad * 0.72 else top - 0.4
            c = r[int(max(0, min(top, round(v))))]
            if caps and abs(lat) > caps - n[y][x] * 6:
                c = ICE[1 if f[y][x] > 0.5 else 2]
            cv[x, y] = c
    return cv


def generic_rock_map(w=512, h=256):
    return cratered(w, h, ROCK_RAMP, 431, craters=90)


def banded_map(r, seed):
    def fn(w=512, h=256):
        cv = Canvas(w, h)
        n = smooth_noise(w, h, seed, 24)
        f = smooth_noise(w, h, seed + 1, 4)
        for y in range(h):
            lat = 90 - (y + 0.5) / h * 180
            for x in range(w):
                v = 3 + math.sin(math.radians(lat * 7 + n[y][x] * 40)) * 2 + (f[y][x] - 0.5)
                cv[x, y] = r[int(max(0, min(len(r) - 1, round(v))))]
        return cv
    return fn


def limb_tex(rgb):
    cv = Canvas(16, 16)
    for y in range(16):
        for x in range(16):
            cv[x, y] = (*rgb, 255)
    return cv


def export_worlds(save):
    worlds = {
        # Mars, Venus and Mercury maps (orbit_<name>.png, <name>_disc.png) belong to the inner planets area
        'generic_rock': generic_rock_map, 'generic_gas': banded_map(GAS_RAMP, 441), 'generic_ice': banded_map(ICE_RAMP, 451),
    }
    for name, fn in worlds.items():
        save(fn(), 'environment', 'orbit_' + name)
        save(disc(fn, 512, 256), 'environment', 'orbit_' + name + '_disc')
    save(limb_tex((230, 150, 110)), 'environment', 'orbit_mars_limb')
    save(limb_tex((250, 225, 160)), 'environment', 'orbit_venus_limb')
    save(limb_tex((170, 200, 230)), 'environment', 'orbit_generic_limb')


_export_with_facility = export


def export(save):  # noqa: F811
    _export_with_facility(save)
    export_worlds(save)
