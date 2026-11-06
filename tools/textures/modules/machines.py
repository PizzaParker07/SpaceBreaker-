"""Textures for 3D block/item models: solar panel, oxygen tank (in-hand model)."""
import random

from sbtex.core import Canvas, clustered, pick
from sbtex import palettes as P


# ----------------------------------------------------------------------------- solar panel

def solar_cells() -> Canvas:
    """Top of the tracking panel: aluminium frame, 3x3 monocrystalline cells on a white backsheet.
    Cells are pseudo-square (clipped corners) with a faint busbar and a glass sheen."""
    C, B, A = P.CELL, P.BACKSHEET, P.ALUMINIUM
    cv = Canvas()
    for y in range(16):
        for x in range(16):
            cv[x, y] = B[0]
    starts = (1, 6, 11)
    for cy in starts:
        for cx in starts:
            for y in range(4):
                for x in range(4):
                    corner = (x in (0, 3)) and (y in (0, 3))
                    if corner:
                        cv[cx + x, cy + y] = B[1]
                        continue
                    v = 2 if (x + y) % 3 else 1
                    if x in (1, 2) and y in (1, 2):
                        v = 3  # busbar crossing
                    cv[cx + x, cy + y] = C[v]
    # glass sheen: a soft diagonal band
    for y in range(16):
        for x in range(16):
            d = x + y
            c = cv[x, y]
            if 9 <= d <= 11 and c[:3] in [tuple(C[i]) for i in range(8)]:
                idx = [tuple(C[i]) for i in range(8)].index(c[:3])
                cv[x, y] = C[min(7, idx + (2 if d == 10 else 1))]
    # frame
    for i in range(16):
        cv[i, 0] = A[6]
        cv[0, i] = A[6]
        cv[i, 15] = A[3]
        cv[15, i] = A[3]
    cv[0, 15] = A[4]
    cv[15, 0] = A[4]
    return cv


def solar_back() -> Canvas:
    """Underside: white backsheet, frame, junction box and two cables."""
    B, A, R = P.BACKSHEET, P.ALUMINIUM, P.RUBBER
    cv = Canvas()
    n = clustered(16, 16, 5, passes=1)
    for y in range(16):
        for x in range(16):
            cv[x, y] = pick(B[1:5], 0.3 + n[y][x] * 0.5)
    for i in range(16):
        cv[i, 0] = A[4]
        cv[0, i] = A[4]
        cv[i, 15] = A[2]
        cv[15, i] = A[2]
    # junction box
    for y in range(2, 5):
        for x in range(6, 10):
            cv[x, y] = R[3] if y > 2 else R[4]
    cv[9, 4] = R[1]
    # cables
    for y in range(5, 9):
        cv[6, y] = R[2]
        cv[9, y] = R[2]
    cv[6, 9] = R[1]
    cv[9, 9] = R[1]
    return cv


def solar_frame() -> Canvas:
    """Edges of the panel and the mounting rails: anodised aluminium extrusion."""
    A = P.ALUMINIUM
    cv = Canvas()
    for y in range(16):
        for x in range(16):
            row = y % 4
            cv[x, y] = A[[6, 5, 4, 3][row]]
    for x in range(0, 16, 8):
        for y in range(16):
            if y % 4 == 1:
                cv[x + 3, y] = A[2]  # rivet
    return cv


def steel_mount(seed: int = 21) -> Canvas:
    """Galvanised steel for the base plate, post and drive housing."""
    S = P.STEEL
    cv = Canvas()
    n = clustered(16, 16, seed, passes=2)
    g = clustered(16, 16, seed + 1, passes=0)
    for y in range(16):
        for x in range(16):
            cv[x, y] = pick(S[3:7], 0.15 + n[y][x] * 0.55 + g[y][x] * 0.3)
    return cv


def solar_base_top() -> Canvas:
    """Base plate seen from above: steel with four anchor bolts and a hazard edge."""
    S = P.STEEL
    cv = steel_mount(31)
    for i in range(16):
        cv[i, 0] = S[6]
        cv[0, i] = S[6]
        cv[i, 15] = S[1]
        cv[15, i] = S[1]
    for bx, by in [(2, 2), (12, 2), (2, 12), (12, 12)]:
        cv[bx, by] = S[7]
        cv[bx + 1, by] = S[5]
        cv[bx, by + 1] = S[4]
        cv[bx + 1, by + 1] = S[1]
    return cv


def solar_housing() -> Canvas:
    """Drive housing: steel with cooling slots."""
    S = P.STEEL
    cv = steel_mount(41)
    for y in (4, 7, 10):
        for x in range(3, 13):
            cv[x, y] = S[1]
            cv[x, y + 1] = S[6]
    for i in range(16):
        cv[i, 0] = S[6]
        cv[i, 15] = S[1]
    return cv


# ----------------------------------------------------------------------------- oxygen tank (in-hand 3D)

def oxygen_tank_model() -> Canvas:
    """UV sheet for the in-hand oxygen tank model (16x16).
    Regions:
      (0,0)-(4,11)   body side: white paint, cyan O2 band, steel foot ring
      (4,0)-(8,4)    end caps (white)
      (8,0)-(12,4)   shoulder / collar (steel)
      (12,0)-(16,2)  brass valve
      (12,2)-(16,4)  hand wheel (dark brass)
      (8,4)-(10,6)   gauge face
      (10,4)-(12,6)  gauge body (steel)
    """
    W, S, B, C = P.SUIT_WHITE, P.STEEL, P.BRASS, P.OXYGEN_CYAN
    cv = Canvas()
    body = [W[6], W[6], C[5], C[3], W[6], W[6], W[6], W[5], W[6], S[5], S[3]]
    for y, col in enumerate(body):
        for x in range(4):
            c = col
            if x == 0 and col in (W[6],):
                c = W[7]
            cv[x, y] = c
    # small white mark on the band (the 'O2' label reads as a light tick)
    cv[1, 2] = W[7]
    cv[2, 3] = C[6]
    for y in range(4):
        for x in range(4, 8):
            cv[x, y] = W[6] if (x + y) % 3 else W[5]
    for y in range(4):
        for x in range(8, 12):
            cv[x, y] = S[6] if y < 2 else S[4]
    R = P.RUBBER
    for x in range(12, 16):
        cv[x, 0] = B[5]
        cv[x, 1] = B[3]
        cv[x, 2] = R[4]      # black hand wheel
        cv[x, 3] = R[2]
    cv[8, 4] = W[7]
    cv[9, 4] = W[7]
    cv[8, 5] = W[6]
    cv[9, 5] = (180, 40, 30)
    for y in range(4, 6):
        for x in range(10, 12):
            cv[x, y] = S[5]
    return cv


def export(save):
    cell_rules = lambda c: (235, 10, 0, 255) if (c[2] > c[0] + 20) else ((150, 232, 0, 255) if c[0] > 150 and c[2] > 150 and abs(c[0] - c[2]) < 30 and c[0] < 240 else None)
    save(solar_cells(), 'block', 'solar_panel_cells', (90, 20, 0, cell_rules), wrap=False)
    save(solar_back(), 'block', 'solar_panel_back', (60, 20, 0, None), wrap=False)
    save(solar_frame(), 'block', 'solar_panel_frame', (170, 232, 0, None))
    save(steel_mount(), 'block', 'solar_panel_steel', (120, 230, 0, None))
    save(solar_base_top(), 'block', 'solar_panel_base_top', (120, 230, 0, None), wrap=False)
    save(solar_housing(), 'block', 'solar_panel_housing', (120, 230, 0, None), wrap=False)
    save(oxygen_tank_model(), 'item', 'oxygen_tank_model')
