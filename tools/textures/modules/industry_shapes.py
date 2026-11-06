"""Item icon shapes for the Industry area (ingots, nuggets, raw chunks, plates, dusts, wires, rods,
storage blocks). Every shape is a hand-drawn shade template (digits = ramp index, 0 darkest) that is
coloured with a material ramp, so all metals share vanilla-like silhouettes and light from the top-left."""
import random

from sbtex.core import Canvas, parse_mask, mask_from, dome_shade, edge_pixels, pick, clustered


def from_template(rows, r, extra=None) -> Canvas:
    """rows: 16 strings; '0'-'7' = ramp index, '.' = empty, other chars looked up in extra (char -> colour)."""
    cv = Canvas()
    for y, row in enumerate(rows):
        for x, ch in enumerate(row):
            if ch == '.':
                continue
            if ch.isdigit():
                cv[x, y] = r[int(ch)]
            elif extra and ch in extra:
                cv[x, y] = extra[ch]
    return cv


# Ingot: a trapezoidal bar lying diagonally, top face lit, long front face in shade.
INGOT = [
    "................",
    "................",
    "................",
    "...........111..",
    "........1115661.",
    ".....1115566551.",
    "..1115566555541.",
    ".156655555554331",
    "1565555555443321",
    "1455555544332210",
    "1344444333222100",
    ".1333332221100..",
    "..1222211000....",
    "...10000........",
    "................",
    "................",
]

def _outline(cv: Canvas, r):
    """1 px outline outside the shape: darkest tone below/right, second tone above/left."""
    filled = {(x, y) for y in range(16) for x in range(16) if cv.alpha(x, y)}
    for y in range(16):
        for x in range(16):
            if (x, y) in filled:
                continue
            if (x - 1, y) in filled or (x, y - 1) in filled:
                cv.put(x, y, r[0])
            elif (x + 1, y) in filled or (x, y + 1) in filled:
                cv.put(x, y, r[1])


def bar(r, x0=1, y0=8, length=10, depth=3, height=3, glint=True) -> Canvas:
    """Diagonal bar seen from the top-left: flat lit top, shaded long front face, end face.
    Long edges follow clean 2:1 pixel lines like vanilla ingots."""
    cv = Canvas()
    x1 = x0 + length            # right end of the back edge
    xf0, xf1 = x0 + depth, x1 + depth

    def back(x):                # top boundary of the top face
        return y0 - (x - x0) // 2 if x <= x1 else y0 - length // 2 + (x - x1)

    def front(x):               # front (lower) boundary of the top face
        return y0 + depth - (x - xf0) // 2 if x >= xf0 else y0 + (x - x0)

    for x in range(x0, xf1 + 1):
        top, bot = back(x), front(x)
        for y in range(top, bot + 1):
            edge_front = y == bot and x >= xf0
            cv.put(x, y, r[6] if edge_front else r[5])
        for k in range(1, height + 1):
            y = bot + k
            if x >= xf0:
                cv.put(x, y, r[2] if k == height else r[3])
            else:
                cv.put(x, y, r[4] if k < height else r[3])
    # the far top edge catches a little light too
    for x in range(x0, x1 + 1):
        cv.put(x, back(x), r[6] if x % 2 == 0 else r[5])
    if glint:
        for x, y in ((xf0 + 1, front(xf0 + 1)), (xf0 + 2, front(xf0 + 2)), (x0 + 1, back(x0 + 1) + 1)):
            cv.put(x, y, r[7])
    _outline(cv, r)
    return cv


def ingot(r) -> Canvas:
    return bar(r)




def nugget(r) -> Canvas:
    return bar(r, x0=5, y0=8, length=4, depth=2, height=2)


def plate(r) -> Canvas:
    """A square sheet lying flat: big lit top with brushed streaks, 1 px thick edge."""
    cv = bar(r, x0=1, y0=9, length=10, depth=5, height=1, glint=False)
    for y in range(16):          # one soft diagonal sheen across the face
        for x in range(16):
            if cv[x, y][:3] == tuple(r[5]) and 13 <= x + 2 * y <= 14 + 1:
                cv[x, y] = r[6]
    return cv


def rod(r) -> Canvas:
    """A sturdy bar from bottom-left to top-right (2 px wide, lit upper edge)."""
    cv = Canvas()
    for i in range(10):
        x, y = 3 + i, 12 - i
        cv.put(x, y, r[6] if i % 3 == 1 else r[5])
        cv.put(x + 1, y, r[4])
        cv.put(x, y + 1, r[3])
        cv.put(x + 1, y + 1, r[3])
        cv.put(x + 1, y + 2, r[2])
    cv.put(13, 2, r[7])
    _outline(cv, r)
    return cv


def wire(r, flange=None) -> Canvas:
    """A spool of wire: steel flanges top and bottom, tight windings shaded as a cylinder."""
    from sbtex import palettes as P
    f = flange or P.CARBON_STEEL
    cv = Canvas()
    cyl = [2, 3, 4, 5, 5, 5, 4, 4, 3, 2]       # cylinder shading across x = 3..12
    for y in range(4, 12):
        for i, x in enumerate(range(3, 13)):
            t = cyl[i] + (1 if y % 2 == 0 else -1) * (1 if 2 <= i <= 6 else 0)
            cv.put(x, y, r[max(2, min(6, t + 1))])
    for y, shade in ((2, (5, 5, 4)), (3, (4, 3, 3)), (12, (4, 4, 3)), (13, (3, 3, 3))):
        for x in range(2, 14):
            k = 0 if x < 6 else (1 if x < 11 else 2)
            cv.put(x, y, f[shade[k]])
    cv.put(13, 8, r[5])
    cv.put(14, 9, r[4])
    cv.put(14, 10, r[3])
    _outline(cv, [f[1], f[1]])
    return cv


DUST = [
    "................",
    "................",
    "................",
    "................",
    "................",
    "........1.......",
    ".......1661.....",
    "......165541....",
    ".....16554431...",
    "....1655544331..",
    "...165545443321.",
    "..16554544433210",
    ".1554444333322100",
    "..0011222211100.",
    "................",
    "................",
]


def dust(r, seed=1) -> Canvas:
    rows = [row[:16] for row in DUST]
    cv = from_template(rows, r)
    rng = random.Random(seed)
    for y in range(6, 13):
        for x in range(16):
            if cv.alpha(x, y) and rng.random() < 0.18:
                c = cv[x, y][:3]
                idx = [tuple(t) for t in r].index(c) if c in [tuple(t) for t in r] else 4
                if 2 <= idx <= 6:
                    cv[x, y] = r[idx + 1] if rng.random() < 0.5 else r[idx - 1]
    return cv


def _ellipse(cx, cy, rx, ry):
    return [[((x + .5 - cx) / rx) ** 2 + ((y + .5 - cy) / ry) ** 2 <= 1 for x in range(16)] for y in range(16)]


RAW_LUMPS = {
    'a': [(6.5, 6.5, 5.0, 4.2), (10.5, 9.5, 4.2, 4.0), (5.5, 11.0, 3.6, 2.8)],
    'b': [(7.5, 7.0, 6.0, 4.6), (4.5, 11.0, 3.6, 3.0), (11.5, 11.0, 3.4, 3.0)],
    'c': [(9.0, 6.0, 5.0, 4.0), (6.0, 9.5, 5.0, 4.0), (11.0, 11.5, 3.0, 2.5)],
}


def raw_chunk(r, variant='a', speck=None, specks=(), seed=3) -> Canvas:
    """Raw ore: two or three overlapping lumps, each lit from the top-left, darkest-tone outline.
    speck: colour ramp for mineral inclusions placed at `specks` (x, y) positions."""
    cv = Canvas()
    union = [[False] * 16 for _ in range(16)]
    rng = random.Random(seed)
    for cx, cy, rx, ry in RAW_LUMPS[variant]:
        m = _ellipse(cx, cy, rx, ry)
        sh = dome_shade(m, roundness=2.2)
        for y in range(16):
            for x in range(16):
                if m[y][x]:
                    union[y][x] = True
                    v = sh[y][x] + (rng.random() - 0.5) * 0.12
                    cv[x, y] = pick(r[2:8], v)
    for x, y, side in edge_pixels(union):
        if side == 'br':
            cv[x, y] = r[1]
    _outline(cv, r)
    if speck:
        for x, y in specks:
            if cv.alpha(x, y) and union[y][x]:
                cv[x, y] = speck[1]
                if cv.alpha(x - 1, y - 1) and union[y - 1][x - 1]:
                    cv[x - 1, y - 1] = speck[2]
    return cv


def metal_block(r, style='plain', seed=5) -> Canvas:
    """Storage block: smooth metal face with a bevelled border and a style-specific panel pattern."""
    cv = Canvas()
    for y in range(16):
        for x in range(16):
            cv[x, y] = r[5] if (x + y) % 9 not in (0, 1) else r[4]
    for i in range(16):
        cv[i, 0] = r[6]
        cv[0, i] = r[6]
        cv[i, 15] = r[2]
        cv[15, i] = r[2]
        cv[1, i] = r[5] if 0 < i < 15 else cv[1, i]
        cv[i, 14] = r[3] if 0 < i < 15 else cv[i, 14]
        cv[14, i] = r[3] if 0 < i < 15 else cv[14, i]
    if style == 'lines':        # rolled sheets, horizontal seams
        for x in range(1, 15):
            cv[x, 5] = r[3]
            cv[x, 6] = r[6]
            cv[x, 10] = r[3]
            cv[x, 11] = r[6]
    elif style == 'rivets':     # riveted panel
        for bx, by in ((3, 3), (11, 3), (3, 11), (11, 11)):
            cv[bx, by] = r[7]
            cv[bx + 1, by] = r[5]
            cv[bx, by + 1] = r[4]
            cv[bx + 1, by + 1] = r[2]
    elif style == 'tiles':      # four tiles
        for i in range(1, 15):
            cv[7, i] = r[3]
            cv[8, i] = r[6]
            cv[i, 7] = r[3]
            cv[i, 8] = r[6]
    elif style == 'brushed':
        for y in range(2, 14):
            for x in range(2, 14):
                if (x * 3 + y * 7) % 11 == 0:
                    cv[x, y] = r[6]
    return cv


def raw_block(r, speck=None, seed=9) -> Canvas:
    """Block of raw ore: packed rounded lumps (tiling Voronoi cells), each lit from the top-left."""
    rng = random.Random(seed)
    pts = [(rng.uniform(0, 16), rng.uniform(0, 16)) for _ in range(9)]

    def near(x, y):
        best = []
        for i, (px, py) in enumerate(pts):
            for ox in (-16, 0, 16):
                for oy in (-16, 0, 16):
                    dx, dy = x + .5 - px - ox, y + .5 - py - oy
                    best.append((dx * dx + dy * dy, i, dx, dy))
        best.sort()
        return best[0], best[1]

    cv = Canvas()
    for y in range(16):
        for x in range(16):
            (d1, i1, dx, dy), (d2, _, _, _) = near(x, y)
            gap = d2 ** .5 - d1 ** .5
            if gap < 0.9:
                cv[x, y] = r[1] if (dx + dy) > 0 else r[2]
                continue
            lit = -(dx + dy) / 6.0
            v = 0.5 + lit * 0.5 + rng.uniform(-0.08, 0.08)
            cv[x, y] = pick(r[2:7], v)
    if speck:
        for _ in range(6):
            x, y = rng.randrange(16), rng.randrange(16)
            if cv[x, y][:3] not in (tuple(r[1]), tuple(r[2])):
                cv[x, y] = speck[2]
                cv[x + 1, y + 1] = speck[1]
    return cv


# --------------------------------------------------------------------------- ore overlays

# Fleck shapes: list of (dx, dy, tone) with tone 0 = dark, 1 = mid, 2 = light (exactly one light pixel).
FLECKS = {
    'round': [[(0, 0, 2), (1, 0, 1), (0, 1, 1), (1, 1, 0)],
              [(1, 0, 1), (0, 1, 2), (1, 1, 1), (2, 1, 1), (1, 2, 0)],
              [(0, 0, 2), (1, 0, 1), (1, 1, 0)]],
    'needle': [[(0, 0, 2), (1, 1, 1), (2, 2, 1), (3, 3, 0)],
               [(0, 2, 1), (1, 1, 2), (2, 0, 1), (3, -1, 0)],
               [(0, 0, 2), (1, 1, 1), (2, 2, 0)]],
    'blocky': [[(0, 0, 2), (1, 0, 1), (2, 0, 1), (0, 1, 1), (1, 1, 1), (2, 1, 0)],
               [(0, 0, 2), (1, 0, 1), (0, 1, 1), (1, 1, 0), (2, 1, 0)],
               [(1, 0, 1), (0, 1, 2), (1, 1, 1), (1, 2, 0)]],
    'lath': [[(0, 0, 2), (1, 0, 1), (2, 0, 1), (3, 0, 1), (1, 1, 0), (2, 1, 0), (3, 1, 0)],
             [(0, 0, 2), (1, 0, 1), (2, 0, 1), (2, 1, 0), (3, 1, 0)],
             [(0, 0, 2), (0, 1, 1), (1, 1, 1), (1, 2, 0)]],
    'tabular': [[(0, 0, 1), (1, 0, 2), (0, 1, 1), (1, 1, 1), (0, 2, 0), (1, 2, 0)],
                [(0, 0, 2), (1, 0, 1), (0, 1, 1), (1, 1, 0)],
                [(0, 0, 1), (1, 0, 1), (2, 0, 2), (1, 1, 0), (2, 1, 0)]],
}

# Cluster anchors shared by all ores (hand-placed so flecks never line up and tile cleanly).
ANCHORS = [(1, 1, 0), (9, 1, 1), (13, 4, 2), (4, 5, 1), (10, 7, 0), (1, 9, 2), (6, 10, 0), (12, 12, 1), (3, 13, 2), (8, 14, 1)]


def ore_overlay(r, style, tones=(2, 4, 6), skip=(), extra=()) -> Canvas:
    """Ore flecks for an overlay on stone/deepslate: small irregular clusters in 3 ramp tones."""
    cv = Canvas()
    for i, (ax, ay, v) in enumerate(list(ANCHORS) + list(extra)):
        if i in skip:
            continue
        shape = FLECKS[style][(v + i) % len(FLECKS[style])]
        for dx, dy, t in shape:
            cv[(ax + dx) % 16, (ay + dy) % 16] = r[tones[t]]
    return cv


# --------------------------------------------------------------------------- intermediates

def ellipse_ring(cx, cy, rx, ry, inner):
    out = []
    for y in range(16):
        for x in range(16):
            d = ((x + .5 - cx) / rx) ** 2 + ((y + .5 - cy) / ry) ** 2
            if d <= 1 and d > inner:
                out.append((x, y, d))
    return out


def wafer(r) -> Canvas:
    """A polished wafer: a flat disc seen at an angle, mirror sheen band, flat (notch) edge."""
    cv = Canvas()
    for y in range(16):
        for x in range(16):
            d = ((x + .5 - 8) / 7) ** 2 + ((y + .5 - 7.5) / 5) ** 2
            if d <= 1 and y < 12:
                band = 10 <= x + y <= 12 or 17 <= x + y <= 18
                cv[x, y] = r[6] if band else (r[4] if x + y < 14 else r[3])
    for x in range(16):                       # wafer edge (thickness) at the front
        for y in range(15, 0, -1):
            if cv.alpha(x, y - 1) and not cv.alpha(x, y):
                cv[x, y] = r[2]
                break
    for x in range(5, 11):                    # the flat
        cv.put(x, 2, r[5])
    _outline(cv, r)
    return cv


def board(r_board, trace, chip, pads, advanced=False) -> Canvas:
    """Circuit board: plate-shaped PCB with traces, a chip and pads."""
    cv = bar(r_board, x0=1, y0=9, length=10, depth=5, height=1, glint=False)
    top = [(x, y) for y in range(16) for x in range(16) if cv[x, y][:3] in (tuple(r_board[5]), tuple(r_board[6]))]
    tops = set(top)
    for x, y in top:
        cv[x, y] = r_board[4]
    for x, y in top:                           # traces along the 2:1 diagonals
        if (x + 2 * y) % 6 == 0 and (x, y) in tops:
            cv[x, y] = trace
    chip_px = [(6, 8), (7, 8), (8, 7), (9, 7), (6, 9), (7, 9), (8, 8), (9, 8), (8, 9), (7, 7)]
    if advanced:
        chip_px += [(10, 7), (10, 6), (9, 6), (5, 9), (6, 10), (7, 10)]
    for x, y in chip_px:
        if (x, y) in tops:
            cv[x, y] = chip[1]
    for x, y in ((7, 7), (8, 7)):
        if (x, y) in tops:
            cv[x, y] = chip[3]
    for x, y in pads:
        if (x, y) in tops:
            cv[x, y] = trace
    return cv


def motor(body, copper, shaft) -> Canvas:
    """Electric motor: a short steel cylinder lying on its side, copper windings at the open end, shaft."""
    cv = Canvas()
    cyl = [1, 2, 3, 4, 5, 5, 4, 3]  # vertical shading of the drum (y = 4..11)
    for y in range(4, 12):
        for x in range(3, 11):
            cv[x, y] = body[cyl[y - 4] + 1]
        cv[3, y] = body[cyl[y - 4]]
    for y in (5, 8):                           # cooling fins
        for x in range(4, 10):
            cv[x, y] = body[2]
    for y in range(5, 11):                     # end face with windings
        for x in range(11, 13):
            cv[x, y] = copper[5 if (y + x) % 2 else 3]
    for y in range(6, 10):
        cv[13, y] = copper[2]
    cv.put(14, 7, shaft[6])
    cv.put(15, 7, shaft[5])
    cv.put(14, 8, shaft[3])
    cv.put(15, 8, shaft[2])
    for x in range(4, 10):                     # feet
        cv[x, 12] = body[1]
    _outline(cv, body)
    return cv


def o_ring(r) -> Canvas:
    cv = Canvas()
    for x, y, d in ellipse_ring(8, 8, 6.5, 4.5, 0.38):
        top = y < 8
        cv[x, y] = r[5] if (top and x < 9) else (r[4] if top else (r[3] if d > 0.7 else r[2]))
    for x, y in ((5, 5), (6, 4)):
        cv[x, y] = r[7]
    _outline(cv, r)
    return cv


def die(r, kind) -> Canvas:
    """Press die: a thick steel block with the die cavity on top."""
    cv = bar(r, x0=1, y0=8, length=10, depth=5, height=4, glint=False)
    if kind == 'plate':
        cav = [(x, y) for y in range(16) for x in range(16) if 6 <= x <= 10 and 5 <= y <= 9 and 13 <= x + 2 * y - 3 <= 19]
    elif kind == 'wire':
        cav = [(7, 7), (8, 7), (7, 8), (8, 6)]
    else:
        cav = [(4 + i, 9 - i // 2) for i in range(8)]
    for x, y in cav:
        if cv.alpha(x, y):
            cv[x, y] = r[1]
            if cv.alpha(x, y + 1) and (x, y + 1) not in cav:
                cv[x, y + 1] = r[6]
    return cv


def canister(white, foil, steel) -> Canvas:
    """Insulated canister: white cylinder with a gold-foil band, valve on top."""
    cv = Canvas()
    shade = [3, 5, 6, 6, 5, 5, 4, 3, 2]
    for y in range(4, 15):
        for i, x in enumerate(range(4, 13)):
            ramp_ = foil if 8 <= y <= 10 else white
            cv[x, y] = ramp_[shade[i] if 8 <= y <= 10 else min(7, shade[i] + 1)]
    for x in range(5, 12):
        cv[x, 3] = white[5]
    for x in range(7, 10):
        cv[x, 2] = steel[5]
        cv[x, 1] = steel[6]
    cv[10, 1] = steel[3]
    cv[11, 1] = steel[4]
    _outline(cv, white)
    return cv


def foam_block(r, seed=4) -> Canvas:
    """A slab of sprayed foam insulation with pores."""
    cv = bar(r, x0=1, y0=8, length=10, depth=5, height=3, glint=False)
    rng = random.Random(seed)
    for y in range(16):
        for x in range(16):
            c = cv[x, y]
            if c[3] and c[:3] == tuple(r[5]) and rng.random() < 0.22:
                cv[x, y] = r[4] if rng.random() < 0.6 else r[6]
    return cv


def crumpled(r, seed=6) -> Canvas:
    """Thin crumpled metal foil."""
    cv = bar(r, x0=1, y0=9, length=10, depth=5, height=1, glint=False)
    rng = random.Random(seed)
    for y in range(16):
        for x in range(16):
            c = cv[x, y]
            if c[3] and c[:3] in (tuple(r[5]), tuple(r[6])):
                v = (x * 5 + y * 3 + rng.randrange(3)) % 7
                cv[x, y] = r[7] if v == 0 else (r[4] if v in (3, 4) else r[5])
    return cv


def honeycomb(r) -> Canvas:
    """Ablative heat-shield tile: phenolic honeycomb matrix."""
    cv = bar(r, x0=1, y0=8, length=10, depth=5, height=2, glint=False)
    for y in range(16):
        for x in range(16):
            c = cv[x, y]
            if c[3] and c[:3] in (tuple(r[5]), tuple(r[6])):
                cv[x, y] = r[6] if (x % 3 == 0 or (y + (x // 3) % 2) % 2 == 0) else r[4]
    return cv


def solar_cell_icon(cell, frame) -> Canvas:
    cv = bar(cell, x0=1, y0=9, length=10, depth=5, height=1, glint=False)
    for y in range(16):
        for x in range(16):
            c = cv[x, y]
            if c[3] and c[:3] in (tuple(cell[5]), tuple(cell[6])):
                cv[x, y] = cell[3]
                if (x + 2 * y) % 5 == 0:
                    cv[x, y] = frame[5]
                elif (x - 2 * y) % 8 == 0:
                    cv[x, y] = frame[4]
    return cv
