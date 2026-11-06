"""Item icons (16x16, GUI). Silhouettes are either 2:1 'isometric' polygons (crisp pixel lines)
or hand-drawn character grids; shading always comes from the material ramp."""
import random

from .core import Canvas, poly_mask, union, pick, dome_shade, parse_mask, mask_from, edge_pixels
from . import palettes as P


def _outline_around(cv: Canvas, mask, col_br, col_tl=None):
    """Draw a 1px outline outside the mask (bottom/right darker)."""
    h, w = len(mask), len(mask[0])
    col_tl = col_tl or col_br

    def m(x, y):
        return 0 <= x < w and 0 <= y < h and mask[y][x]

    for y in range(h):
        for x in range(w):
            if m(x, y):
                continue
            if m(x - 1, y) or m(x, y - 1) or m(x - 1, y - 1):
                cv.put(x, y, col_br)
            elif m(x + 1, y) or m(x, y + 1) or m(x + 1, y + 1):
                cv.put(x, y, col_tl)


def _faces_bar(p0, u, v, h):
    """Box seen from the top-left: returns (top, front(long, facing down-right), end(facing down-left))."""
    x0, y0 = p0
    p1 = (x0 + u[0], y0 + u[1])
    p2 = (p1[0] + v[0], p1[1] + v[1])
    p3 = (x0 + v[0], y0 + v[1])
    top = [p0, p1, p2, p3]
    front = [p3, p2, (p2[0], p2[1] + h), (p3[0], p3[1] + h)]
    end = [p0, p3, (p3[0], p3[1] + h), (x0, y0 + h)]
    return top, front, end


def ingot(r, glint=True) -> Canvas:
    top, front, end = _faces_bar((1, 8), (8, -4), (4, 2), 3)
    mt, mf, me = (poly_mask(16, 16, top), poly_mask(16, 16, front), poly_mask(16, 16, end))
    cv = Canvas()
    all_m = union(mt, mf, me)
    for y in range(16):
        for x in range(16):
            if mt[y][x]:
                far_edge = y - 1 >= 0 and not mt[y - 1][x]
                cv[x, y] = r[6] if far_edge else r[5]
            elif me[y][x]:
                cv[x, y] = r[4]
            elif mf[y][x]:
                bottom = y + 1 >= 16 or not mf[y + 1][x]
                cv[x, y] = r[2] if bottom else r[3]
    if glint:
        for x, y in [(4, 7), (5, 7), (5, 6), (10, 5)]:
            if mt[y][x]:
                cv[x, y] = r[7]
    _outline_around(cv, all_m, r[0], r[1])
    return cv


def plate(r, seed=3) -> Canvas:
    top, front, end = _faces_bar((1, 8), (8, -4), (6, 3), 1)
    mt, mf, me = (poly_mask(16, 16, top), poly_mask(16, 16, front), poly_mask(16, 16, end))
    all_m = union(mt, mf, me)
    cv = Canvas()
    for y in range(16):
        for x in range(16):
            if mt[y][x]:
                # brushed finish: faint streaks along one diagonal
                streak = (x - 2 * y) % 5 == 0
                cv[x, y] = r[6] if streak else r[5]
            elif me[y][x]:
                cv[x, y] = r[3]
            elif mf[y][x]:
                cv[x, y] = r[2]
    for y in range(16):
        for x in range(16):
            if mt[y][x] and (y - 1 < 0 or not mt[y - 1][x]):
                cv[x, y] = r[7]          # far edges catch the light
    _outline_around(cv, all_m, r[0], r[1])
    return cv


RAW_CHUNK = parse_mask([
    "................",
    "................",
    "......XXX.......",
    "....XXXXXX......",
    "...XXXXXXXX.XX..",
    "..XXXXXXXXXXXXX.",
    "..XXXXXXXXXXXXX.",
    ".XXXXXXXXXXXXXX.",
    ".XXXXXXXXXXXXXX.",
    ".XXXXXXXXXXXXX..",
    "..XXXXXXXXXXXX..",
    "..XXXXXXXXXXX...",
    "...XXXXXXXXX....",
    "....XXX..XX.....",
    "................",
    "................",
])


def raw_bauxite() -> Canvas:
    r = P.BAUXITE
    m = mask_from(RAW_CHUNK)
    sh = dome_shade(m, roundness=3.5)
    cv = Canvas()
    rng = random.Random(7)
    for y in range(16):
        for x in range(16):
            if m[y][x]:
                v = 0.1 + 0.85 * sh[y][x] + (rng.random() - 0.5) * 0.18
                cv[x, y] = pick(r[1:6], v)
    # pisoids on the surface: small lit dots with a shadow pixel
    for x, y in [(4, 6), (8, 4), (11, 6), (6, 9), (10, 10), (3, 9), (13, 8), (8, 12)]:
        if m[y][x]:
            cv[x, y] = r[6]
            if m[y + 1][x + 1]:
                cv[x + 1, y + 1] = r[2]
    # crisp outline
    for x, y, side in edge_pixels(m):
        cv[x, y] = r[0] if side == 'br' else r[1]
    return cv


OXYGEN_TANK_ICON = [
    "................",
    "......bbbb......",
    "......BkkB......",
    ".......SS.......",
    "......sSSs......",
    ".......mm.......",
    "......oWWo......",
    ".....oWHWwgo....",
    "....oWHWWwwgo...",
    "....oCCcCcccO...",
    "....oCcccccco...",
    "....oWHWWwwgo...",
    "....oWHWWwwgo...",
    "....oWHWWwwgo...",
    "....omsssmmmo...",
    ".....ooooooo....",
]


def oxygen_tank_icon() -> Canvas:
    W, S, B, C = P.SUIT_WHITE, P.STEEL, P.BRASS, P.OXYGEN_CYAN
    pal = {
        'o': W[0], 'O': C[0],
        'W': W[6], 'H': W[7], 'w': W[4], 'g': W[2],
        'C': C[5], 'c': C[3],
        's': S[5], 'S': S[7], 'm': S[3],
        'b': B[4], 'B': B[2], 'k': B[1],
        'n': S[2], 'G': W[7],
    }
    cv = Canvas()
    for y, row in enumerate(OXYGEN_TANK_ICON):
        for x, ch in enumerate(row):
            if ch in pal:
                cv[x, y] = pal[ch]
    return cv


EVA_HELMET_ICON = [
    "................",
    ".....oooooo.....",
    "...ooWWWWWwwo...",
    "..oWWWWWWWwwgo..",
    ".oWWWmmmmmmwggo.",
    ".oWWmVvvvVVmwgo.",
    "oWWmVrRvvVVVmgLo",
    "oWWmVRrvVVVVmgLo",
    "oWWmVrvVVVVVmggo",
    "oWwmVvVVVVVVmggo",
    ".owwmVVVVVVmggo.",
    ".ogwwmmmmmmwggo.",
    "..obbBBBBbbbbo..",
    "..obbbbbbbbbdo..",
    "...oooooooooo...",
    "................",
]


def eva_helmet_icon() -> Canvas:
    W, V, S, A, L = P.SUIT_WHITE, P.VISOR_GLASS, P.STEEL, P.ANODIZED_BLUE, P.LAMP
    pal = {
        'o': W[0], 'W': W[7], 'w': W[5], 'g': W[3],
        'm': S[5], 'V': V[1], 'v': V[3], 'r': V[5], 'R': V[7],
        'b': A[3], 'B': A[5], 'd': A[1], 'L': L[3],
    }
    cv = Canvas()
    for y, row in enumerate(EVA_HELMET_ICON):
        for x, ch in enumerate(row):
            if ch in pal:
                cv[x, y] = pal[ch]
    return cv
