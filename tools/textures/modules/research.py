"""Research area: tech disk, field sample, experiment record, sample kit, field guide, Research Lab block,
lab / research-tree / guide GUI art, advancement background and guide illustrations.

Item icons are hand-placed pixel grids (one character per pixel, mapped onto material ramps).
GUI art follows the vanilla container look: #C6C6C6 panels, white/#555555 bevels, #8B8B8B slots."""
import math
import random

from PIL import Image

from sbtex.core import Canvas, ramp, clustered, pick, rgba
from sbtex import palettes as P

# ----------------------------------------------------------------------------- ramps (research only)
PLASTIC = ramp('#17181c', '#262830', '#353843', '#454956', '#5a5f6e')
PAPER = ramp('#7d745c', '#a39a80', '#d2cab2', '#e9e4d6', '#f8f3e2')
INK = ramp('#2c2a33', '#3e4a6b')
CORK = ramp('#5e3d22', '#7a5232', '#a8794c')
GLASS = ramp('#4c6573', '#7f9cab', '#a9c2cd', '#e3f0f4')
LEATHER = ramp('#3b2414', '#5a3820', '#7a4f2d', '#97663c', '#b07d4d')
WOOD = ramp('#5a3d22', '#8a6036')
COVER = ramp('#0f1830', '#17264a', '#203566', '#2b4682', '#3a5ca1')
GREY = ramp('#7a7a7a', '#a8a8a8', '#d4d4d4', '#ffffff')  # tint layers (multiplied by the item colour)

# vanilla GUI greys
G_BLACK = (0, 0, 0, 255)
G_DARK = (55, 55, 55, 255)
G_SHADOW = (85, 85, 85, 255)
G_SLOT = (139, 139, 139, 255)
G_PANEL = (198, 198, 198, 255)
G_WHITE = (255, 255, 255, 255)


def grid(rows, legend, w=16, h=16) -> Canvas:
    assert len(rows) == h, (len(rows), rows)
    cv = Canvas(w, h)
    for y, row in enumerate(rows):
        assert len(row) == w, (y, row, len(row))
        for x, ch in enumerate(row):
            if ch in legend and legend[ch] is not None:
                cv[x, y] = legend[ch]
    return cv


# ============================================================================= items

TECH_DISK_ROWS = [
    "................",
    ".ooooaaaaaaooo..",
    ".ohhhecwwcahmmo.",
    ".ohmmecwwcammmo.",
    ".ohmmecwwcammmo.",
    ".ohmmecwwcammmo.",
    ".ohmmaaaaaammmo.",
    ".ohmmmmmmmmmmdo.",
    ".ohLLLLLLLLLLdo.",
    ".ohlllllllllldo.",
    ".ohppppppppppdo.",
    ".ohptttttttppdo.",
    ".ohpttttpppppdo.",
    ".oddddddddddddo.",
    ".oooooooooooooo.",
    "................",
]


def tech_disk() -> Canvas:
    A = P.ALUMINIUM
    return grid(TECH_DISK_ROWS, {
        'o': PLASTIC[0], 'd': PLASTIC[1], 'm': PLASTIC[2], 'h': PLASTIC[3], 'w': PLASTIC[0],
        'a': A[2], 'c': A[5], 'e': A[6],
        'p': PAPER[3], 't': PAPER[1], 'L': PAPER[3], 'l': PAPER[3],
    })


def tech_disk_label() -> Canvas:
    """Tinted layer: the coloured band of the label (branch colour)."""
    return grid(TECH_DISK_ROWS, {'L': GREY[3], 'l': GREY[1]})


SAMPLE_ROWS = [
    "................",
    "......kKKk......",
    "......kKkk......",
    ".....gGGGGg.....",
    "......gW.g......",
    ".....gW...g.....",
    "....gW.....g....",
    "...gW.......g...",
    "...gCCCCCCCCCg..",
    "...gCCCCCCCCCg..",
    "...gCCCCCCCCCg..",
    "...gCCCCCCCCCg..",
    "...gCCCCCCCCCg..",
    "...gCCCCCCCCCg..",
    "....ggggggggg...",
    "................",
]


def field_sample() -> Canvas:
    return grid(SAMPLE_ROWS, {'k': CORK[0], 'K': CORK[2], 'g': GLASS[0], 'G': GLASS[2], 'W': GLASS[3], 'C': GLASS[1]})


def field_sample_contents() -> Canvas:
    """Tinted layer: soil and pebbles in the jar (greyscale, multiplied by the sample colour)."""
    cv = Canvas()
    rng = random.Random(7)
    n = clustered(16, 16, 7, passes=1)
    for y in range(8, 14):
        for x in range(4, 13):
            v = 0.25 + n[y][x] * 0.5
            if y == 8:
                v += 0.25  # lit surface
            cv[x, y] = pick(GREY[:3], v)
    for x, y in [(5, 10), (9, 9), (7, 12), (11, 11), (4, 12)]:   # pebbles: light top-left, dark under
        cv[x, y] = GREY[3]
        if y + 1 <= 13:
            cv[x, y + 1] = GREY[0]
    # glass highlight streak stays visible over the contents
    for y in range(8, 13):
        cv[5, y] = cv[5, y] if (y % 3) else GREY[3]
    return cv


def experiment_data() -> Canvas:
    rows = []
    ink = {11: 5, 10: 6, 9: 6, 8: 7, 7: 7, 6: 8, 5: 9, 4: 10}  # row -> ink column (a rising flight curve)
    for y in range(16):
        if y in (0, 15):
            rows.append('.' * 16)
            continue
        if y in (1, 14):
            rows.append('...oooooooooo...')
            continue
        hole = 'h' if y % 2 == 0 else 'p'
        mid = []
        for x in range(5, 11):
            if y == 2:
                mid.append('H')
            elif y == 3:
                mid.append('L')
            elif ink.get(y) == x:
                mid.append('i')
            elif y == 8:
                mid.append('s')
            elif y == 12 and x < 10:
                mid.append('t')
            else:
                mid.append('p')
        edge = 's' if y == 8 else hole
        rows.append('...o' + edge + ''.join(mid) + edge + 'o...')
    return grid(rows, {'o': PAPER[0], 'p': PAPER[4], 's': PAPER[2], 'h': PAPER[1], 'i': INK[1], 't': PAPER[1],
                       'H': PAPER[4], 'L': PAPER[4]})


def experiment_data_band() -> Canvas:
    cv = Canvas()
    for x in range(5, 11):
        cv[x, 2] = GREY[3]
        cv[x, 3] = GREY[1]
    return cv


SAMPLE_KIT_ROWS = [
    "................",
    ".........MMMM...",
    ".........MMMMMMm",
    ".........mmmm...",
    "..........wW....",
    ".........wW.....",
    "..ooooowWooo....",
    ".oFFFFwWFFFFo...",
    ".oFFFFbbFFFFo...",
    ".offffbbffffo...",
    ".oFPPPPPPPPPo...",
    ".oFPPPPPPPPfo...",
    ".oFPPPPPPPPfo...",
    ".oPPPPPPPPffo...",
    "..oooooooooo....",
    "................",
]


def sample_kit() -> Canvas:
    S = P.STEEL
    return grid(SAMPLE_KIT_ROWS, {'o': LEATHER[0], 'f': LEATHER[1], 'P': LEATHER[2], 'F': LEATHER[3],
                                  'w': WOOD[0], 'W': WOOD[1], 'm': S[3], 'M': S[6], 'b': P.BRASS[5]})


def guide_book() -> Canvas:
    G = P.GOLD
    cv = Canvas()
    o, s, c, C, H = COVER[0], COVER[1], COVER[2], COVER[3], COVER[4]
    for x in range(2, 13):
        cv[x, 1] = o
        cv[x, 14] = o
    for y in range(2, 14):
        cv[1, y] = o
        cv[2, y] = s
        cv[12, y] = o
        cv[13, y] = PAPER[4] if y % 2 else PAPER[2]
        cv[14, y] = o if y > 2 else (0, 0, 0, 0)
        for x in range(3, 12):
            cv[x, y] = H if (y == 2 or x == 3) else (s if (y == 13 or x == 11) else c)
    cv[13, 14] = o
    cv[13, 1] = (0, 0, 0, 0)
    cv[14, 14] = (0, 0, 0, 0)
    # spine bands
    for y in (3, 12):
        cv[2, y] = G[4]
    # emblem: a ringed planet
    for x, y, t in [(7, 4, 6), (6, 5, 5), (7, 5, 6), (8, 5, 4),
                    (4, 6, 3), (5, 6, 5), (6, 6, 6), (7, 6, 5), (8, 6, 5), (9, 6, 4), (10, 6, 3),
                    (6, 7, 4), (7, 7, 4), (8, 7, 3), (7, 8, 3)]:
        cv[x, y] = G[max(4, t)]
    # title rule
    for x in range(5, 10):
        cv[x, 10] = G[4]
    return cv


# ============================================================================= Research Lab block

def _panel(seed, lo=3, hi=6) -> Canvas:
    S = P.STEEL
    n = clustered(16, 16, seed, passes=2)
    cv = Canvas()
    for y in range(16):
        for x in range(16):
            cv[x, y] = pick(S[lo:hi + 1], 0.2 + n[y][x] * 0.6)
    return cv


def _bevel(cv, x0, y0, x1, y1, light, dark):
    for x in range(x0, x1 + 1):
        cv[x, y0] = light
        cv[x, y1] = dark
    for y in range(y0, y1 + 1):
        cv[x0, y] = light
        cv[x1, y] = dark


def lab_side() -> Canvas:
    """Cabinet side (the model uses rows 7..15): pale lab-grey enamel, recessed panel, vent louvres."""
    A = P.ALUMINIUM
    cv = Canvas()
    n = clustered(16, 16, 61, passes=2)
    for y in range(16):
        for x in range(16):
            cv[x, y] = pick(A[3:6], 0.35 + n[y][x] * 0.4)
    for x in range(16):
        cv[x, 7] = A[6]
        cv[x, 15] = A[1]
    for y in range(7, 16):
        cv[0, y] = A[6]
        cv[15, y] = A[1]
    _bevel(cv, 2, 9, 13, 13, A[2], A[6])        # recessed panel
    for y in (10, 12):                           # louvres
        for x in range(4, 12):
            cv[x, y] = A[1]
            cv[x, y + 1] = A[5]
    return cv


def lab_front() -> Canvas:
    """Cabinet front (rows 7..15): a disk-slot drawer with a status light over a cupboard door."""
    A, S = P.ALUMINIUM, P.STEEL
    cv = lab_side()
    for y in range(8, 15):
        for x in range(1, 15):
            cv[x, y] = A[4]
    _bevel(cv, 2, 8, 13, 10, A[6], A[2])   # drawer with the disk slot
    for x in range(4, 10):
        cv[x, 9] = PLASTIC[0]
    cv[11, 9] = (0x3a, 0x8f, 0x4a, 255)    # status LED (single tone)
    _bevel(cv, 2, 11, 13, 14, A[6], A[2])  # cupboard door
    for x in range(6, 10):                 # handle
        cv[x, 12] = A[6]
        cv[x, 13] = A[2]
    for x in range(1, 15):                 # kick plate
        cv[x, 15] = A[1]
    return cv


def lab_top() -> Canvas:
    """Worktop: dark epoxy resin with an aluminium edge."""
    A = P.ALUMINIUM
    n = clustered(16, 16, 63, passes=2)
    cv = Canvas()
    for y in range(16):
        for x in range(16):
            cv[x, y] = pick(PLASTIC[2:5], 0.3 + n[y][x] * 0.5)
    _bevel(cv, 0, 0, 15, 15, A[6], A[3])
    return cv


def lab_unit() -> Canvas:
    """Analyser housing (sides/top/back): white enamel, a panel seam and a vent grille."""
    A = P.ALUMINIUM
    n = clustered(16, 16, 64, passes=2)
    cv = Canvas()
    for y in range(16):
        for x in range(16):
            cv[x, y] = pick(A[4:7], 0.4 + n[y][x] * 0.4)
    _bevel(cv, 0, 0, 15, 15, A[7], A[3])
    for y in range(3, 8, 2):
        for x in range(4, 12):
            cv[x, y] = A[3]
    return cv


def lab_screen(on=False) -> Canvas:
    """Analyser front, used at u 2..14, v 2..9 of this sheet: bezel + display (u 4..9, v 3..7), two knobs and a
    power light. The 'on' version shows a glowing spectrum; the model lights the display up when working.
    Flat enamel (no noise) so the face stays within the 6-colour block limit."""
    A = P.ALUMINIUM
    glow = ramp('#14382a', '#2f8a58', '#5fd08a', '#b8f5c9')
    cv = Canvas()
    for y in range(16):
        for x in range(16):
            cv[x, y] = A[5]
    _bevel(cv, 0, 0, 15, 15, A[7], A[3])
    for x in range(2, 14):
        cv[x, 2] = A[7]
        cv[x, 8] = A[3]
    _bevel(cv, 3, 2, 10, 8, A[3], A[7])
    for y in range(3, 8):
        for x in range(4, 10):
            cv[x, y] = glow[0] if on else PLASTIC[0]
    if on:
        heights = [2, 4, 3, 5, 2, 3]
        for i, hgt in enumerate(heights):
            for k in range(hgt):
                cv[4 + i, 7 - k] = glow[1] if k < hgt - 1 else glow[3]
        cv[12, 3] = glow[3]                  # power light: green while working
    else:
        cv[12, 3] = (0xb8, 0x3a, 0x2c, 255)  # power light: red standby
    for y in (5, 7):                         # knobs
        cv[11, y] = A[3]
        cv[12, y] = A[3]
    return cv


def lab_microscope() -> Canvas:
    """Small parts sheet for the microscope (black enamel + chrome)."""
    S = P.STEEL
    cv = Canvas()
    for y in range(16):
        for x in range(16):
            cv[x, y] = PLASTIC[1] if (x // 4 + y // 4) % 2 else PLASTIC[2]
    for x in range(0, 16):
        cv[x, 0] = S[7]
        cv[x, 15] = PLASTIC[0]
    for y in range(8, 12):
        for x in range(8, 12):
            cv[x, y] = S[6] if (x + y) % 2 else S[5]   # chrome tube region (uv 8..12)
    return cv


# ============================================================================= GUI

def _gui_panel(img, x0, y0, w, h):
    """Vanilla container panel with rounded black outline and white/grey bevel."""
    px = img.load()
    x1, y1 = x0 + w - 1, y0 + h - 1
    for y in range(y0, y1 + 1):
        for x in range(x0, x1 + 1):
            px[x, y] = G_PANEL
    for x in range(x0 + 2, x1 - 1):
        px[x, y0] = G_BLACK
        px[x, y1] = G_BLACK
        px[x, y0 + 1] = G_WHITE
        px[x, y0 + 2] = G_WHITE
        px[x, y1 - 1] = G_SHADOW
        px[x, y1 - 2] = G_SHADOW
    for y in range(y0 + 2, y1 - 1):
        px[x0, y] = G_BLACK
        px[x1, y] = G_BLACK
        px[x0 + 1, y] = G_WHITE
        px[x0 + 2, y] = G_WHITE
        px[x1 - 1, y] = G_SHADOW
        px[x1 - 2, y] = G_SHADOW
    for (x, y, c) in [(x0, y0, None), (x0 + 1, y0, None), (x0, y0 + 1, None), (x0 + 1, y0 + 1, G_BLACK),
                      (x1, y0, None), (x1 - 1, y0, G_BLACK), (x1, y0 + 1, G_BLACK), (x1 - 1, y0 + 1, G_PANEL),
                      (x0, y1, None), (x0 + 1, y1, G_BLACK), (x0, y1 - 1, G_BLACK), (x0 + 1, y1 - 1, G_PANEL),
                      (x1, y1, None), (x1 - 1, y1, None), (x1, y1 - 1, None), (x1 - 1, y1 - 1, G_BLACK)]:
        px[x, y] = c if c else (0, 0, 0, 0)
    px[x0 + 2, y0 + 2] = G_WHITE
    px[x1 - 2, y0 + 1] = G_PANEL
    px[x0 + 1, y1 - 2] = G_PANEL
    px[x1 - 2, y1 - 2] = G_SHADOW


def _slot(img, x, y, w=18, h=18):
    px = img.load()
    for yy in range(y, y + h):
        for xx in range(x, x + w):
            px[xx, yy] = G_SLOT
    for xx in range(x, x + w - 1):
        px[xx, y] = G_DARK
    for yy in range(y, y + h - 1):
        px[x, yy] = G_DARK
    for xx in range(x + 1, x + w):
        px[xx, y + h - 1] = G_WHITE
    for yy in range(y + 1, y + h):
        px[x + w - 1, yy] = G_WHITE


ARROW_ROWS = [
    "...............x........",
    "...............xx.......",
    "...............xxx......",
    "...............xxxx.....",
    "...............xxxxx....",
    "xxxxxxxxxxxxxxxxxxxxxx..",
    "xxxxxxxxxxxxxxxxxxxxxxx.",
    "xxxxxxxxxxxxxxxxxxxxxxxx",
    "xxxxxxxxxxxxxxxxxxxxxxx.",
    "xxxxxxxxxxxxxxxxxxxxxx..",
    "...............xxxxx....",
    "...............xxxx.....",
    "...............xxx......",
    "...............xx.......",
    "...............x........",
    "........................",
]


def _arrow(img, ox, oy, col):
    px = img.load()
    for y, row in enumerate(ARROW_ROWS):
        for x, ch in enumerate(row):
            if ch == 'x':
                px[ox + x, oy + y] = col


def lab_container() -> Image.Image:
    img = Image.new('RGBA', (256, 256), (0, 0, 0, 0))
    _gui_panel(img, 0, 0, 176, 166)
    _slot(img, 55, 34)                     # input
    _slot(img, 111, 30, 26, 26)            # output (big, like the furnace result)
    _arrow(img, 80, 35, G_SLOT)
    # small status display above the arrow
    px = img.load()
    for y in range(17, 31):
        for x in range(78, 106):
            edge_dark = x == 78 or y == 17
            edge_light = x == 105 or y == 30
            px[x, y] = G_DARK if edge_dark else G_WHITE if edge_light else (0x1d, 0x22, 0x24, 255)
    # energy gauge
    _slot(img, 152, 17, 16, 52)
    for y in range(18, 68):
        for x in range(153, 167):
            px[x, y] = (0x2b, 0x2b, 0x2b, 255)
    for y in range(22, 68, 5):              # scale ticks
        px[153, y] = G_SHADOW
        px[154, y] = G_SHADOW
    # player inventory
    for row in range(3):
        for col in range(9):
            _slot(img, 7 + col * 18, 83 + row * 18)
    for col in range(9):
        _slot(img, 7 + col * 18, 141)
    return img


def lab_progress() -> Canvas:
    cv = Canvas(24, 16)
    for y, row in enumerate(ARROW_ROWS):
        for x, ch in enumerate(row):
            if ch == 'x':
                cv[x, y] = G_WHITE
    return cv


def lab_energy() -> Canvas:
    """Energy fill: warm red cells with a lighter left edge (FE is traditionally red)."""
    r = ramp('#5c1610', '#8f2418', '#c23a22', '#e0603a', '#f29a6a')
    cv = Canvas(14, 50)
    for y in range(50):
        for x in range(14):
            v = r[2]
            if x <= 1:
                v = r[3]
            elif x >= 12:
                v = r[1]
            if y % 5 == 4:
                v = r[0]
            cv[x, y] = v
        if y % 5 == 0:
            cv[2, y] = r[4]
    return cv


def lab_screen_on_sprite() -> Canvas:
    glow = ramp('#14382a', '#2f8a58', '#5fd08a', '#b8f5c9')
    cv = Canvas(26, 12)
    for y in range(12):
        for x in range(26):
            cv[x, y] = (0x0c, 0x1d, 0x16, 255)
    for x in range(26):
        y = int(round(6 + 3.5 * math.sin(x / 26 * math.tau * 1.5)))
        cv[x, y] = glow[3]
        if 0 < y < 11:
            cv[x, y + 1] = glow[1]
    for x in range(0, 26, 5):
        for y in range(1, 11, 2):
            if cv[x, y] == (0x0c, 0x1d, 0x16, 255):
                cv[x, y] = glow[0]
    return cv


def icon_tree() -> Canvas:
    """Three linked nodes (the research tree)."""
    rows = [
        "................",
        "................",
        "..dddd..........",
        "..dLLd..........",
        "..dLLd----dddd..",
        "..dddd.|..dLLd..",
        ".......|..dLLd..",
        ".......|..dddd..",
        ".......|........",
        ".......|..dddd..",
        ".......|--dLLd..",
        "..........dLLd..",
        "..........dddd..",
        "................",
        "................",
        "................",
    ]
    return grid(rows, {'d': (0x37, 0x37, 0x37, 255), 'L': (0xe0, 0xc0, 0x50, 255), '-': (0x37, 0x37, 0x37, 255),
                       '|': (0x37, 0x37, 0x37, 255)})


def icon_team(on: bool) -> Canvas:
    rows = [
        "................",
        "................",
        ".....hhh........",
        "....hhhhh.......",
        "....hhhhh..ggg..",
        ".....hhh..ggggg.",
        "...........ggg..",
        "...bbbbbbb......",
        "..bbbbbbbbb.ccc.",
        "..bbbbbbbbbccccc",
        "..bbbbbbbbbccccc",
        "..bbbbbbbbbccccc",
        "................",
        "................",
        "................",
        "................",
    ]
    if on:
        legend = {'h': (0xd9, 0xb0, 0x8c, 255), 'b': (0x3a, 0x6e, 0xb0, 255),
                  'g': (0xd9, 0xb0, 0x8c, 255), 'c': (0x3f, 0x9a, 0x52, 255)}
    else:
        legend = {'h': (0x8b, 0x8b, 0x8b, 255), 'b': (0x55, 0x55, 0x55, 255),
                  'g': (0x8b, 0x8b, 0x8b, 255), 'c': (0x55, 0x55, 0x55, 255)}
    return grid(rows, legend)


def research_window() -> Canvas:
    """Nine-slice window frame (64×64; border 9, title bar 18) with a transparent centre."""
    W = H = 64
    cv = Canvas(W, H)
    img = Image.new('RGBA', (W, H), (0, 0, 0, 0))
    _gui_panel(img, 0, 0, W, H)
    px = img.load()
    # inner viewport cut-out with a dark 1px rim (like the advancement window)
    for y in range(18, H - 9):
        for x in range(9, W - 9):
            px[x, y] = (0, 0, 0, 0)
    for x in range(8, W - 8):
        px[x, 17] = G_SHADOW
        px[x, H - 9] = G_WHITE
    for y in range(17, H - 8):
        px[8, y] = G_SHADOW
        px[W - 9, y] = G_WHITE
    cv.img = img
    cv.p = img.load()
    return cv


def node_frame(kind: str) -> Canvas:
    """26×26 technology frame: known = brass, available = light steel, locked = dark slate."""
    if kind == 'known':
        face, light, dark = (0xd6, 0xa8, 0x3a, 255), (0xf5, 0xd3, 0x6e, 255), (0x7a, 0x55, 0x16, 255)
    elif kind == 'available':
        face, light, dark = (0xc6, 0xc6, 0xc6, 255), (0xff, 0xff, 0xff, 255), (0x55, 0x55, 0x55, 255)
    else:
        face, light, dark = (0x5a, 0x5f, 0x68, 255), (0x7c, 0x82, 0x8c, 255), (0x30, 0x33, 0x39, 255)
    cv = Canvas(26, 26)
    for y in range(1, 25):
        for x in range(1, 25):
            corner = (x in (1, 24)) and (y in (1, 24))
            if corner:
                continue
            cv[x, y] = face
    for i in range(2, 24):
        cv[i, 0] = G_BLACK
        cv[i, 25] = G_BLACK
        cv[0, i] = G_BLACK
        cv[25, i] = G_BLACK
        cv[i, 1] = light
        cv[1, i] = light
        cv[i, 24] = dark
        cv[24, i] = dark
    for x, y in [(1, 1), (24, 1), (1, 24), (24, 24)]:
        cv[x, y] = G_BLACK
    # recessed icon well
    well = (0x8b, 0x8b, 0x8b, 255) if kind != 'locked' else (0x3e, 0x42, 0x49, 255)
    for y in range(4, 21):
        for x in range(4, 22):
            cv[x, y] = well
    for x in range(4, 22):
        cv[x, 3] = dark
        cv[x, 21] = light
    for y in range(3, 22):
        cv[3, y] = dark
        cv[22, y] = light
    return cv


def blueprint() -> Canvas:
    base, line, dot = (0x1b, 0x2a, 0x45, 255), (0x24, 0x38, 0x5c, 255), (0x2c, 0x44, 0x6c, 255)
    cv = Canvas()
    for y in range(16):
        for x in range(16):
            cv[x, y] = base
    for i in range(16):
        cv[i, 0] = line
        cv[0, i] = line
    cv[8, 8] = dot
    cv[0, 0] = dot
    return cv


def space_background() -> Canvas:
    base = (0x0b, 0x10, 0x20, 255)
    cv = Canvas()
    rng = random.Random(1969)
    n = clustered(16, 16, 12, passes=2)
    for y in range(16):
        for x in range(16):
            v = n[y][x]
            cv[x, y] = (0x0b + int(v * 6), 0x10 + int(v * 6), 0x20 + int(v * 10), 255)
    for x, y, c in [(3, 2, (0xd8, 0xdc, 0xe6, 255)), (11, 5, (0x9a, 0xa4, 0xbc, 255)), (6, 11, (0xf0, 0xe6, 0xc8, 255)),
                    (14, 13, (0x8a, 0x92, 0xa8, 255)), (1, 8, (0x6c, 0x76, 0x92, 255))]:
        cv[x, y] = c
    return cv


# ----------------------------------------------------------------------------- guide book GUI

def guide_book_gui() -> Image.Image:
    """Two-page book, 300×188 inside a 512×256 sheet: deep blue leather, gold corners, cream pages."""
    W, H = 300, 188
    img = Image.new('RGBA', (512, 256), (0, 0, 0, 0))
    px = img.load()
    rng = random.Random(5)
    lea = [c + (255,) for c in COVER]
    # cover
    for y in range(H):
        for x in range(W):
            edge = x in (0, W - 1) or y in (0, H - 1)
            corner = (x < 2 or x > W - 3) and (y < 2 or y > H - 3)
            if corner and (x in (0, W - 1) and y in (0, H - 1)):
                continue
            if edge:
                px[x, y] = lea[0]
            else:
                v = 2 + (1 if rng.random() < 0.15 else 0) - (1 if rng.random() < 0.1 else 0)
                px[x, y] = lea[max(1, min(3, v))]
    for x in range(2, W - 2):             # top highlight, bottom shade
        px[x, 1] = lea[4]
        px[x, H - 2] = lea[1]
    for y in range(2, H - 2):
        px[1, y] = lea[4]
        px[W - 2, y] = lea[1]
    # stitching
    for x in range(5, W - 5, 3):
        px[x, 4] = lea[4]
        px[x, H - 5] = lea[4]
    for y in range(6, H - 6, 3):
        px[4, y] = lea[4]
        px[W - 5, y] = lea[4]
    # pages
    paper = [c + (255,) for c in PAPER]
    for side in (0, 1):
        x0 = 10 if side == 0 else 151
        x1 = 149 if side == 0 else 290
        for y in range(8, H - 9):
            for x in range(x0, x1):
                g = (x1 - 1 - x) if side == 0 else (x - x0)  # distance from the gutter
                c = paper[4]
                if g < 2:
                    c = paper[2]
                elif g < 5:
                    c = paper[3]
                if rng.random() < 0.02:
                    c = paper[3]
                px[x, y] = c
        # page stack edges (bottom and outer side)
        for x in range(x0, x1):
            px[x, H - 9] = paper[2]
            px[x, H - 8] = paper[1]
        for y in range(8, H - 8):
            ox = x0 - 1 if side == 0 else x1
            px[ox, y] = paper[1]
    # gutter
    for y in range(6, H - 6):
        px[149, y] = lea[0]
        px[150, y] = lea[1]
    # gold corner protectors
    gold = [c + (255,) for c in P.GOLD]
    for (cx, cy, sx, sy) in [(0, 0, 1, 1), (W - 1, 0, -1, 1), (0, H - 1, 1, -1), (W - 1, H - 1, -1, -1)]:
        for i in range(9):
            for j in range(9 - i):
                x, y = cx + sx * i, cy + sy * j
                if i + j in (0,):
                    continue
                if i + j < 9:
                    t = 6 if i + j < 3 else (5 if (i + j) % 3 else 4)
                    if i + j == 8:
                        t = 2
                    px[x, y] = gold[t]
    return img


def guide_slot() -> Canvas:
    paper = PAPER
    cv = Canvas(18, 18)
    for y in range(18):
        for x in range(18):
            cv[x, y] = paper[2]
    for i in range(17):
        cv[i, 0] = paper[0]
        cv[0, i] = paper[0]
    for i in range(1, 18):
        cv[i, 17] = paper[4]
        cv[17, i] = paper[4]
    return cv


def guide_recipe() -> Canvas:
    cv = Canvas(100, 56)
    slot = guide_slot()
    for gy in range(3):
        for gx in range(3):
            cv.paste(slot, 1 + gx * 18, 1 + gy * 18)
    # arrow in ink (a smaller version of the container arrow)
    ink = PAPER[1]
    for y in range(9):
        half = 4 - abs(y - 4)
        for x in range(57, 66):
            if 3 <= y <= 5:
                cv[x, 23 + y] = ink
        for x in range(66, 66 + half + 1):
            cv[x, 23 + y] = ink
    # output slot (big)
    big = Canvas(24, 24)
    for y in range(24):
        for x in range(24):
            big[x, y] = PAPER[2]
    for i in range(23):
        big[i, 0] = PAPER[0]
        big[0, i] = PAPER[0]
    for i in range(1, 24):
        big[i, 23] = PAPER[4]
        big[23, i] = PAPER[4]
    cv.paste(big, 75, 16)
    return cv


def guide_lock() -> Canvas:
    rows = [
        "...oooo...",
        "..o....o..",
        "..o....o..",
        "..o....o..",
        "oooooooooo",
        "oGGGGGGGGo",
        "oGgggggGGo",
        "oGgg..ggGo",
        "oGggg.ggGo",
        "oGggggggGo",
        "oGGGGGGGGo",
        "oooooooooo",
    ]
    return grid(rows, {'o': PAPER[0], 'G': P.BRASS[4], 'g': P.BRASS[3]}, 10, 12)


def guide_back() -> Canvas:
    rows = [
        "....o...........",
        "...oo...........",
        "..ooooooooooooo.",
        ".oooooooooooooo.",
        "oooooooooooooo..",
        ".oooooooooooooo.",
        "..ooooooooooooo.",
        "...oo...........",
        "....o...........",
        "................",
    ]
    return grid(rows, {'o': PAPER[1]}, 16, 10)


# ----------------------------------------------------------------------------- guide illustrations

def _img(w, h, bg=(0, 0, 0, 0)):
    return Image.new('RGBA', (w, h), bg)


def illus_orbit() -> Image.Image:
    """Earth at the lower left, a dotted orbit, a capsule with its velocity arrow, the Moon far away."""
    W, H = 112, 64
    img = _img(W, H, (0x13, 0x1a, 0x30, 255))
    px = img.load()
    rng = random.Random(3)
    for _ in range(40):
        x, y = rng.randrange(W), rng.randrange(H)
        px[x, y] = (0x8a, 0x92, 0xa8, 255) if rng.random() < 0.7 else (0xe0, 0xe4, 0xee, 255)
    ocean = ramp('#16365e', '#1f4a7c', '#2d64a0', '#4a86c0')
    land = ramp('#2f5a2a', '#477a36', '#6a9a46')
    ex, ey, er = 18, 74, 40
    n = clustered(64, 64, 9, passes=2)
    for y in range(H):
        for x in range(W):
            d = math.hypot(x - ex, y - ey)
            if d < er:
                light = max(0.0, min(1.0, 0.5 + ((ex - x) * -0.4 + (ey - y) * 0.6) / er))
                v = n[y % 64][x % 64]
                if v > 0.6:
                    px[x, y] = land[min(2, int(light * 3))] + (255,)
                else:
                    px[x, y] = ocean[min(3, int(light * 4))] + (255,)
            elif d < er + 2:
                px[x, y] = (0x6f, 0xa8, 0xdc, 255)  # atmosphere rim
    # orbit ellipse (dotted)
    for i in range(0, 360, 4):
        a = math.radians(i)
        x = ex + 62 * math.cos(a)
        y = ey - 4 + 52 * math.sin(a) * 0.95
        if 0 <= x < W and 0 <= y < H and math.hypot(x - ex, y - ey) > er + 2:
            px[int(x), int(y)] = (0xc8, 0xd0, 0xe0, 255)
    # capsule at ~ -60 degrees with velocity arrow tangent
    a = math.radians(-58)
    cx, cy = ex + 62 * math.cos(a), ey - 4 + 52 * math.sin(a) * 0.95
    cx, cy = int(cx), int(cy)
    for dx, dy, c in [(0, 0, (0xe4, 0xe8, 0xed, 255)), (1, 0, (0xe4, 0xe8, 0xed, 255)), (0, 1, (0xb0, 0xb8, 0xc2, 255)),
                      (1, 1, (0x8f, 0x98, 0xa4, 255)), (2, 0, (0xcd, 0xd3, 0xda, 255)), (-1, 1, (0xd8, 0x67, 0x3a, 255))]:
        px[cx + dx, cy + dy] = c
    yellow = (0xf2, 0xc9, 0x4c, 255)
    tx, ty = 0.86, 0.5   # tangent direction (prograde)
    for k in range(4, 22):
        px[int(cx + tx * k), int(cy + ty * k * 0.6)] = yellow
    ax, ay = int(cx + tx * 22), int(cy + ty * 22 * 0.6)
    for dx, dy in [(-1, -2), (-2, -1), (-1, 1), (-2, 0), (-1, 0), (-3, 1), (-2, 2)]:
        px[ax + dx, ay + dy] = yellow
    # gravity arrow towards Earth
    red = (0xd8, 0x67, 0x3a, 255)
    for k in range(4, 14):
        px[int(cx - k * 0.45), int(cy + k * 0.9)] = red
    # Moon
    mx, my = 96, 14
    for y in range(my - 6, my + 7):
        for x in range(mx - 6, mx + 7):
            d = math.hypot(x - mx, y - my)
            if d < 6:
                v = 0.6 + ((mx - x) * 0.5 + (my - y) * 0.5) / 12
                g = int(0x7a + v * 0x40)
                px[x, y] = (g, g, g - 4, 255)
    px[mx - 2, my] = (0x6a, 0x6a, 0x66, 255)
    px[mx + 1, my + 2] = (0x6a, 0x6a, 0x66, 255)
    return img


def illus_lab_flow(disk: Canvas, sample: Canvas, front: Canvas, screen: Canvas) -> Image.Image:
    """Disk / sample → Research Lab → new technology node."""
    W, H = 112, 48
    img = _img(W, H)
    up = lambda cv, k=2: cv.img.resize((cv.w * k, cv.h * k), Image.NEAREST)
    img.alpha_composite(up(disk), (0, 0))
    img.alpha_composite(up(sample), (0, 16))
    # lab: front face with the screen unit on top (flat sketch)
    lab = Image.new('RGBA', (32, 40), (0, 0, 0, 0))
    lab.alpha_composite(front.img.resize((32, 20), Image.NEAREST), (0, 20))
    lab.alpha_composite(screen.img.resize((24, 20), Image.NEAREST), (4, 2))
    img.alpha_composite(lab, (40, 6))
    ink = PAPER[1] + (255,)
    px = img.load()
    for x in range(33, 39):
        px[x, 24] = ink
    for d in (1, 2):
        px[38 - d, 24 - d] = ink
        px[38 - d, 24 + d] = ink
    for x in range(75, 83):
        px[x, 24] = ink
    for d in (1, 2):
        px[82 - d, 24 - d] = ink
        px[82 - d, 24 + d] = ink
    node = node_frame('known')
    img.alpha_composite(node.img, (85, 11))
    star = P.GOLD
    for x, y, t in [(97, 18, 6), (96, 19, 5), (97, 19, 6), (98, 19, 5), (95, 20, 4), (96, 20, 5), (97, 20, 7), (98, 20, 5), (99, 20, 4),
                    (96, 21, 5), (97, 21, 6), (98, 21, 5), (97, 22, 5), (96, 23, 4), (98, 23, 4)]:
        px[x, y] = star[t] + (255,)
    return img


def illus_observatory() -> Image.Image:
    """The ruined observatory and test gantry against a dusk sky (vanilla-like banded sky)."""
    W, H = 112, 64
    img = _img(W, H)
    px = img.load()
    sky = ramp('#1d2340', '#2c3358', '#47467a', '#7a5a86', '#c27a6a', '#e6a265')
    for y in range(H):
        band = min(5, y * 6 // 46)
        for x in range(W):
            px[x, y] = sky[band] + (255,)
    stone = ramp('#2a2a30', '#3a3a42', '#4a4a54')
    copper = ramp('#2c5a4e', '#3f7a68', '#5f9a84')
    ground = ramp('#1e2a1a', '#2a3a22')
    for y in range(50, H):
        for x in range(W):
            px[x, y] = ground[(x // 3 + y) % 2] + (255,)
    # drum
    for y in range(34, 50):
        for x in range(14, 46):
            ruin = x < 22 and y < 34 + (22 - x) * 1.6
            if not ruin:
                px[x, y] = stone[1 if (x + y // 2) % 5 else 0] + (255,)
    # dome (east half intact, slit)
    cx, cy, r = 30, 34, 15
    for y in range(cy - r, cy):
        for x in range(cx - r, cx + r + 1):
            d = math.hypot(x - cx, (y - cy) * 1.1)
            if d < r and not (x < cx - 3 and d > r - 4 and (x * 7 + y * 3) % 5 < 3) and not (abs(x - cx - 2) < 1):
                if x < cx - 6 and y < cy - 6:
                    continue
                px[x, y] = copper[2 if x > cx else 1] + (255,)
    # telescope poking out of the slit
    for k in range(10):
        px[cx + 2 + k // 2, cy - 8 - k] = (0x9a, 0x7a, 0x4a, 255)
    # gantry
    for y in range(18, 50):
        for x in (78, 84):
            px[x, y] = stone[0] + (255,)
        if y % 4 == 0:
            for x in range(78, 85):
                px[x, y] = stone[0] + (255,)
        if y % 4 in (1, 2):
            px[78 + (y % 4) * 2, y] = stone[0] + (255,)
    for x in range(62, 78):            # arm
        px[x, 26] = stone[0] + (255,)
    for y in range(27, 33):
        px[63, y] = stone[0] + (255,)
    # rocket wreck
    for x in range(56, 74):
        px[x, 48] = (0xd8, 0xd8, 0xd4, 255)
        px[x, 49] = (0xa8, 0xa8, 0xa4, 255)
    px[62, 48] = (0xb0, 0x3a, 0x30, 255)
    px[62, 49] = (0x8a, 0x2c, 0x24, 255)
    # first stars
    for x, y in [(8, 6), (40, 3), (70, 9), (100, 5), (92, 15), (20, 14)]:
        px[x, y] = (0xe8, 0xe8, 0xf0, 255)
    return img


# ============================================================================= export

def export(save):
    disk, label = tech_disk(), tech_disk_label()
    save(disk, 'item', 'tech_disk')
    save(label, 'item', 'tech_disk_label')
    sample = field_sample()
    save(sample, 'item', 'field_sample')
    save(field_sample_contents(), 'item', 'field_sample_contents')
    save(experiment_data(), 'item', 'experiment_data')
    save(experiment_data_band(), 'item', 'experiment_data_band')
    save(sample_kit(), 'item', 'sample_kit')
    save(guide_book(), 'item', 'guide_book')

    steel_rules = None
    front, screen = lab_front(), lab_screen(False)
    save(lab_side(), 'block', 'research_lab_side', (150, 230, 0, steel_rules), wrap=False)
    save(front, 'block', 'research_lab_front', (150, 230, 0, steel_rules), wrap=False)
    save(lab_top(), 'block', 'research_lab_top', (200, 40, 0, steel_rules), wrap=False)
    save(lab_unit(), 'block', 'research_lab_unit', (170, 40, 0, steel_rules), wrap=False)
    save(screen, 'block', 'research_lab_screen', (170, 40, 0, steel_rules), wrap=False)
    glow_rules = lambda c: (220, 40, 0, 200) if c[3] and c[1] > c[0] + 40 and c[1] > 100 else None
    save(lab_screen(True), 'block', 'research_lab_screen_on', (170, 40, 0, glow_rules), wrap=False)
    save(lab_microscope(), 'block', 'research_lab_microscope', (190, 230, 0, steel_rules), wrap=False)

    # GUI (not 16×16: saved as plain images)
    def raw(img, rel):
        cv = Canvas(img.width, img.height)
        cv.img = img
        cv.p = img.load()
        return save(cv, 'gui', rel)

    raw(lab_container(), 'container/research_lab')
    save(lab_progress(), 'gui', 'sprites/container/research_lab/progress')
    save(lab_energy(), 'gui', 'sprites/container/research_lab/energy')
    save(lab_screen_on_sprite(), 'gui', 'sprites/container/research_lab/screen_on')
    save(icon_tree(), 'gui', 'sprites/container/research_lab/icon_tree')
    save(icon_team(True), 'gui', 'sprites/container/research_lab/icon_team_on')
    save(icon_team(False), 'gui', 'sprites/container/research_lab/icon_team_off')
    save(research_window(), 'gui', 'sprites/research/window')
    for k in ('known', 'available', 'locked'):
        save(node_frame(k), 'gui', f'sprites/research/node_{k}')
    save(blueprint(), 'gui', 'research/blueprint')
    save(space_background(), 'gui', 'advancements/backgrounds/space')
    raw(guide_book_gui(), 'guide/book')
    save(guide_slot(), 'gui', 'sprites/guide/slot')
    save(guide_recipe(), 'gui', 'sprites/guide/recipe')
    save(guide_lock(), 'gui', 'sprites/guide/lock')
    save(guide_back(), 'gui', 'sprites/guide/back')
    raw(illus_orbit(), 'guide/orbit')
    raw(illus_lab_flow(disk, sample, front, screen), 'guide/lab_flow')
    raw(illus_observatory(), 'guide/observatory')
