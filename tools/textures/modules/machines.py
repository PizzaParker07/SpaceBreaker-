"""Textures for 3D block models: solar panel."""
import random

from sbtex.core import Canvas, clustered, pick
from sbtex import palettes as P


# ----------------------------------------------------------------------------- solar panel

def solar_cells() -> Canvas:
    """Top of the tracking panel: aluminium frame, 3x3 monocrystalline cells on a light backsheet.
    Cells are pseudo-square (clipped corners) with a busbar and one soft glass sheen (6 colours)."""
    C, B, A = P.CELL, P.BACKSHEET, P.ALUMINIUM
    cv = Canvas()
    for y in range(16):
        for x in range(16):
            cv[x, y] = B[1]
    starts = (1, 6, 11)
    for cy in starts:
        for cx in starts:
            for y in range(4):
                for x in range(4):
                    if (x in (0, 3)) and (y in (0, 3)):
                        continue
                    sheen = 9 <= (cx + x) + (cy + y) <= 10
                    cv[cx + x, cy + y] = C[4] if sheen else (C[3] if x in (1, 2) and y in (1, 2) else C[2])
    for i in range(16):
        cv[i, 0] = A[5]
        cv[0, i] = A[5]
        cv[i, 15] = A[3]
        cv[15, i] = A[3]
    return cv


def solar_back() -> Canvas:
    """Underside: backsheet, frame, junction box and two cables (6 colours)."""
    B, A, R = P.BACKSHEET, P.ALUMINIUM, P.RUBBER
    cv = Canvas()
    n = clustered(16, 16, 5, passes=1)
    for y in range(16):
        for x in range(16):
            cv[x, y] = B[2] if n[y][x] > 0.3 else B[1]
    for i in range(16):
        cv[i, 0] = A[4]
        cv[0, i] = A[4]
        cv[i, 15] = A[2]
        cv[15, i] = A[2]
    for y in range(2, 5):
        for x in range(6, 10):
            cv[x, y] = R[3]
    for y in range(5, 10):
        cv[6, y] = R[1]
        cv[9, y] = R[1]
    for x in range(6, 10):
        cv[x, 4] = R[1]
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


def export(save):
    cell_rules = lambda c: (235, 10, 0, 255) if (c[2] > c[0] + 20) else ((150, 232, 0, 255) if c[0] > 150 and c[2] > 150 and abs(c[0] - c[2]) < 30 and c[0] < 240 else None)
    save(solar_cells(), 'block', 'solar_panel_cells', (90, 20, 0, cell_rules), wrap=False)
    save(solar_back(), 'block', 'solar_panel_back', (60, 20, 0, None), wrap=False)
    save(solar_frame(), 'block', 'solar_panel_frame', (170, 232, 0, None))
    save(steel_mount(), 'block', 'solar_panel_steel', (120, 230, 0, None))
    save(solar_base_top(), 'block', 'solar_panel_base_top', (120, 230, 0, None), wrap=False)
    save(solar_housing(), 'block', 'solar_panel_housing', (120, 230, 0, None), wrap=False)
