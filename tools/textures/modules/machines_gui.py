"""Machine screen textures: the standard 176x166 panel with the player inventory, plus a widget sheet
(slot frames, progress arrow, flame, energy bar, tank gauge, slot hints) used by every machine screen."""
from PIL import Image

from sbtex.core import Canvas

BG = (198, 198, 198, 255)
HI = (255, 255, 255, 255)
SH = (85, 85, 85, 255)
OUT = (0, 0, 0, 255)
SLOT_DARK = (55, 55, 55, 255)
SLOT_FILL = (139, 139, 139, 255)


def rect(cv, x0, y0, w, h, c):
    for y in range(y0, y0 + h):
        for x in range(x0, x0 + w):
            cv.put(x, y, c)


def bevel_in(cv, x, y, w, h, fill=SLOT_FILL):
    """Recessed frame (slots, gauges): dark top-left, white bottom-right."""
    rect(cv, x, y, w, h, fill)
    for i in range(w):
        cv.put(x + i, y, SLOT_DARK)
        cv.put(x + i, y + h - 1, HI)
    for j in range(h):
        cv.put(x, y + j, SLOT_DARK)
        cv.put(x + w - 1, y + j, HI)
    cv.put(x + w - 1, y, SLOT_FILL)
    cv.put(x, y + h - 1, SLOT_FILL)


def panel(w=176, h=166) -> Canvas:
    cv = Canvas(256, 256)
    rect(cv, 0, 0, w, h, BG)
    # rounded outer outline with a light inner bevel
    for x in range(2, w - 2):
        cv.put(x, 0, OUT)
        cv.put(x, h - 1, OUT)
        cv.put(x, 1, HI)
        cv.put(x, 2, HI)
        cv.put(x, h - 2, SH)
        cv.put(x, h - 3, SH)
    for y in range(2, h - 2):
        cv.put(0, y, OUT)
        cv.put(w - 1, y, OUT)
        cv.put(1, y, HI)
        cv.put(2, y, HI)
        cv.put(w - 2, y, SH)
        cv.put(w - 3, y, SH)
    for x, y in ((0, 0), (1, 0), (0, 1), (w - 1, 0), (w - 2, 0), (w - 1, 1), (0, h - 1), (1, h - 1), (0, h - 2),
                 (w - 1, h - 1), (w - 2, h - 1), (w - 1, h - 2)):
        cv.put(x, y, (0, 0, 0, 0))
    for x, y in ((1, 1), (w - 2, h - 2)):
        cv.put(x, y, OUT)
    cv.put(w - 2, 1, OUT)
    cv.put(1, h - 2, OUT)
    cv.put(2, 2, HI)
    cv.put(w - 3, h - 3, SH)
    # player inventory
    for row in range(3):
        for col in range(9):
            bevel_in(cv, 7 + col * 18, 83 + row * 18, 18, 18)
    for col in range(9):
        bevel_in(cv, 7 + col * 18, 141, 18, 18)
    return cv


def widgets(cv: Canvas):
    ox = 176
    bevel_in(cv, ox, 0, 18, 18)                         # slot frame
    # output slot: same frame with a subtle darker fill
    bevel_in(cv, ox, 18, 26, 26)
    # progress arrow 24x17: empty (grey) and full (white) at (176,44) and (200,44)
    arrow = ["..........##..........",
             "..........###.........",
             "..........####........",
             "#################.....",
             "##################....",
             "###################...",
             "####################..",
             "###################...",
             "##################....",
             "#################.....",
             "..........####........",
             "..........###.........",
             "..........##.........."]
    for j, row in enumerate(arrow):
        for i, ch in enumerate(row):
            if ch == '#':
                cv.put(ox + 1 + i, 46 + j, (139, 139, 139, 255))
                cv.put(ox + 25 + i, 46 + j, (255, 255, 255, 255))
    # flame 14x14 empty / lit at (176,62) / (190,62)
    flame = ["......#.......",
             ".....##.......",
             ".....###......",
             "....####...#..",
             "...#####..##..",
             "...######.###.",
             "..###########.",
             "..###########.",
             ".############.",
             ".#############",
             ".#############",
             "..###########.",
             "...#########..",
             ".....#####...."]
    lit = [(255, 240, 160, 255), (255, 200, 60, 255), (240, 130, 30, 255), (200, 70, 20, 255)]
    for j, row in enumerate(flame):
        for i, ch in enumerate(row):
            if ch == '#':
                cv.put(ox + i, 62 + j, (139, 139, 139, 255))
                band = lit[min(3, max(0, (j - 2) // 3))] if j > 8 or i in range(5, 10) else lit[min(3, j // 4)]
                cv.put(ox + 14 + i, 62 + j, band)
    # energy bar frame 14x54 at (176,76); fill 12x52 at (190,77)
    bevel_in(cv, ox, 76, 14, 54, (40, 30, 30, 255))
    for y in range(52):
        for x in range(12):
            t = y / 51
            r = int(250 - 60 * t)
            g = int(70 + 40 * (1 - t))
            c = (r, g, 40, 255)
            if x in (0, 11):
                c = (max(0, r - 50), max(0, g - 30), 30, 255)
            if y % 4 == 0:
                c = (max(0, r - 30), max(0, g - 20), 30, 255)
            cv.put(ox + 14 + x, 77 + y, c)
    # tank frame 18x54 at (202,76); gauge marks overlay 16x52 at (220,77)
    bevel_in(cv, ox + 26, 76, 18, 54, (55, 55, 55, 255))
    for y in range(52):
        mark = 6 if y % 13 == 0 else (3 if y % 13 == 6 or y % 13 == 7 and False else 0)
        if y % 13 == 0 and y > 0:
            mark = 6
        elif y % 13 == 6:
            mark = 3
        else:
            mark = 0
        for x in range(mark):
            cv.put(ox + 44 + 15 - x, 77 + y, (230, 230, 230, 200))
    # glass highlight stripe
    for y in range(52):
        cv.put(ox + 44 + 2, 77 + y, (255, 255, 255, 60))
    # hints: press die ghost at (232,0), fuel ghost at (232,16)
    ghost = (120, 120, 120, 255)
    for y in range(4, 12):
        for x in range(3, 13):
            if y in (4, 11) or x in (3, 12) or (6 <= x <= 9 and 7 <= y <= 8):
                cv.put(232 + x, y, ghost)
    for j, row in enumerate(flame):
        for i, ch in enumerate(row):
            if ch == '#' and (j + i) % 2 == 0:
                cv.put(233 + i, 17 + j, ghost)


def export(save):
    cv = panel()
    widgets(cv)
    save(cv, 'gui', 'machine')
