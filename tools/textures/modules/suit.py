"""EVA suit Mk1: armour model sheets, overlay sheet and inventory icons.

The box UV layouts mirror client/suit/EvaSuitModel.java (texOffs + box size) — keep them in sync.

Minecraft box UV layout, box at (u, v) with size (w, h, d):
  top    (u+d,       v,     w, d)   last row = front
  bottom (u+d+w,     v,     w, d)
  west   (u,         v+d,   d, h)
  north  (u+d,       v+d,   w, h)   front side, row 0 = top
  east   (u+d+w,     v+d,   d, h)
  south  (u+2d+w,    v+d,   w, h)   back side

Material look: white composite/fabric shell, grey joints and gloves, blue anodised bearing rings,
gold foil accents, smoked visor glass. Light comes from the top-left like every other texture.
"""
import random

from sbtex.core import Canvas, TRANSPARENT, parse_mask
from sbtex import palettes as P

W, S, A, G, R, V, L = P.SUIT_WHITE, P.STEEL, P.ANODIZED_BLUE, P.GOLD, P.RUBBER, P.VISOR_GLASS, P.LAMP
C = P.OXYGEN_CYAN
CU = P.BRASS

# ----------------------------------------------------------------------------------- layouts (u, v, w, h, d)
HELMET = {
    'shell_x': (0, 0, 10, 8, 8),
    'shell_z': (0, 16, 8, 8, 10),
    'crown': (0, 34, 8, 1, 8),
    'dome': (0, 43, 6, 1, 6),
    'ring': (0, 50, 10, 1, 10),
    'liner': (40, 16, 6, 6, 6),
    'lamp_r': (36, 0, 1, 2, 4),
    'lamp_l': (48, 0, 1, 2, 4),
    'valve': (36, 6, 2, 2, 1),
}
TORSO = {
    'torso': (0, 0, 8, 12, 4),
    'box': (24, 0, 6, 3, 2),
    'plss': (0, 32, 8, 10, 2),
    'bottle_r': (24, 32, 2, 8, 2),
    'bottle_l': (32, 32, 2, 8, 2),
    'cap': (40, 32, 6, 1, 2),
    'antenna': (56, 32, 1, 3, 1),
    'sleeve': (0, 16, 4, 8, 4),
    'glove': (16, 16, 4, 3, 4),
    'elbow': (32, 16, 4, 1, 4),
    'wrist': (48, 16, 4, 1, 4),
    'shoulder': (32, 21, 4, 1, 4),
    'mmu_r': (0, 48, 2, 10, 3),
    'mmu_l': (10, 48, 2, 10, 3),
    'mmu_bar': (20, 48, 12, 1, 3),
}
LEGS = {
    'hips': (0, 0, 8, 4, 4),
    'waist': (24, 0, 8, 1, 4),
    'leg': (0, 8, 4, 10, 4),
    'knee': (16, 8, 4, 1, 4),
}
BOOTS = {
    'upper': (0, 0, 6, 5, 6),
    'sole': (24, 0, 6, 1, 7),
    'toe': (0, 11, 6, 2, 1),
    'ankle': (24, 8, 6, 1, 6),
}
OVERLAY = {
    'glass': (0, 0, 10, 5, 1),
    'gold': (0, 8, 10, 5, 1),
    'gold_up': (0, 16, 8, 1, 1),
}


def faces(u, v, w, h, d):
    return {
        'top': (u + d, v, w, d),
        'bottom': (u + d + w, v, w, d),
        'west': (u, v + d, d, h),
        'north': (u + d, v + d, w, h),
        'east': (u + d + w, v + d, d, h),
        'south': (u + 2 * d + w, v + d, w, h),
    }


def paint(cv, rect, fn):
    """fn(x, y, w, h) -> colour or None (keep)."""
    x0, y0, w, h = rect
    for y in range(h):
        for x in range(w):
            c = fn(x, y, w, h)
            if c is not None:
                cv[x0 + x, y0 + y] = c


def solid(c):
    return lambda *a: c


def box(cv, spec, **painters):
    """Paint the faces of one box; painters: face name -> fn, 'sides' applies to the four sides, 'all' to all."""
    f = faces(*spec)
    for name in ('top', 'bottom', 'west', 'north', 'east', 'south'):
        fn = painters.get(name)
        if fn is None and name in ('west', 'north', 'east', 'south'):
            fn = painters.get('sides')
        if fn is None:
            fn = painters.get('all')
        if fn is not None:
            paint(cv, f[name], fn)


# ----------------------------------------------------------------------------------- material painters

def fabric(seed, base=6):
    """Off-white coated fabric: soft lighting from the top, a few faint creases. 3-4 tones."""
    rng = random.Random(seed)

    def fn(x, y, w, h):
        t = y / max(1, h - 1)
        b = base + 1 if t < 0.12 else (base if t < 0.75 else base - 1)
        if x == 0 and b < 7:
            b += 1  # left edge catches the light
        if x == w - 1 and w > 2:
            b -= 1
        if rng.random() < 0.08:
            b -= 1
        return W[max(2, min(7, b))]
    return fn


def ring_side(x, y, w, h):
    """Blue anodised bearing ring, outer face: lit upper edge, darker lower edge."""
    if y == h - 1 and h > 1:
        return A[3]
    return A[5] if x > 0 else A[6]


def joint_side(x, y, w, h):
    """Fabric joint bellows (shoulders, elbows, knees): a soft grey band, not a hard ring."""
    return W[4] if x > 0 else W[5]


def joint_flat(x, y, w, h):
    return W[5]


def ring_flat(x, y, w, h):
    edge = x in (0, w - 1) or y in (0, h - 1)
    return A[6] if edge else A[4]


def steel_panel(x, y, w, h):
    if y == 0 or x == 0:
        return S[6]
    if y == h - 1 or x == w - 1:
        return S[3]
    return S[5]


# ----------------------------------------------------------------------------------- helmet

def eva_helmet() -> Canvas:
    cv = Canvas(64, 64)
    def hard(x, y, w, h):
        """Hard composite shell: brighter and smoother than fabric."""
        t = y / max(1, h - 1)
        b = 7 if t < 0.25 else (6 if t < 0.8 else 5)
        if x == 0 and b < 7:
            b += 1
        return W[b]

    # side slab (x = +-5): visible as the helmet's cheeks
    def side(x, y, w, h):
        if (x, y) in ((3, 4), (4, 4)):
            return S[6] if x == 3 else S[4]      # visor hinge boss
        if (x, y) in ((3, 5), (4, 5)):
            return S[4] if x == 3 else S[2]
        if y == 7:
            return W[4]
        if x in (1, w - 2) and 1 <= y <= 6:
            return W[5]                           # seam of the visor assembly
        return hard(x, y, w, h)

    def side_front(x, y, w, h):
        """North/south faces of the side slab: only the two outer columns show (bevel corners)."""
        if x not in (0, w - 1):
            return TRANSPARENT
        if 1 <= y <= 5:
            return TRANSPARENT                    # behind the wrap-around visor glass
        return hard(x, y, w, h)

    def side_back(x, y, w, h):
        if x not in (0, w - 1):
            return TRANSPARENT
        return hard(x, y, w, h)

    box(cv, HELMET['shell_x'], west=side, east=side, north=side_front, south=side_back,
        top=solid(W[7]), bottom=solid(TRANSPARENT))

    # front/back slab (z = +-5): the face plate and the back of the shell
    def front(x, y, w, h):
        if 1 <= y <= 5:
            return TRANSPARENT                    # visor opening (glass is in the overlay)
        if y == 0:
            return W[7] if x < w - 1 else W[6]    # brow
        if y == 6:
            return W[6] if 0 < x < w - 1 else W[5]
        # chin: two small vent slots either side of a comm grille
        if x in (1, 6):
            return W[3]
        if x in (3, 4):
            return S[4]
        return W[5]

    def back(x, y, w, h):
        if y == 7:
            return W[4]
        return hard(x, y, w, h)

    f = faces(*HELMET['shell_z'])
    paint(cv, f['north'], front)
    paint(cv, f['south'], back)
    # the side strips: column at the front end is under the glass in the visor rows
    for name in ('west', 'east'):
        x0, y0, w, h = f[name]
        for y in range(h):
            for x in range(w):
                if x not in (0, w - 1):
                    c = TRANSPARENT
                elif 1 <= y <= 5:
                    # the front end of the strip sits under the wrap-around glass, the back end stays shell
                    c = hard(x, y, w, h) if _is_back_column(name, x, w) else TRANSPARENT
                else:
                    c = hard(x, y, w, h)
                cv[x0 + x, y0 + y] = c
    paint(cv, f['top'], lambda x, y, w, h: W[7])
    paint(cv, f['bottom'], solid(TRANSPARENT))

    # crown and dome: brightest part, small specular highlight at the top-left
    box(cv, HELMET['crown'], top=lambda x, y, w, h: W[7], sides=lambda x, y, w, h: W[7] if x < w - 1 else W[6],
        bottom=solid(TRANSPARENT))
    box(cv, HELMET['dome'], top=lambda x, y, w, h: W[7], sides=solid(W[7]), bottom=solid(TRANSPARENT))

    # neck ring
    box(cv, HELMET['ring'], sides=ring_side, top=ring_flat, bottom=solid(A[1]))

    # padded liner (seen through the visor on armour stands)
    box(cv, HELMET['liner'], all=lambda x, y, w, h: R[2] if (x + 2 * y) % 4 else R[1])

    # side lamps: white housing, steel bezel; the lens itself glows in the overlay
    for name in ('lamp_r', 'lamp_l'):
        box(cv, HELMET[name], top=solid(W[7]), bottom=solid(W[4]),
            sides=lambda x, y, w, h: W[6] if y == 0 else W[5], north=solid(L[2]), south=solid(S[4]))
    # purge valve on the back
    box(cv, HELMET['valve'], north=solid(S[4]), south=lambda x, y, w, h: S[6] if (x, y) == (0, 0) else S[5],
        top=solid(S[6]), bottom=solid(S[3]), west=solid(S[5]), east=solid(S[3]))
    return cv


def _is_back_column(face, x, w):
    """West faces run back -> front (column 0 = back), east faces front -> back."""
    return x == 0 if face == 'west' else x == w - 1


# ----------------------------------------------------------------------------------- torso

def eva_torso() -> Canvas:
    cv = Canvas(64, 64)

    # upper torso (hard upper torso, 8x12x4 texels stretched over the vanilla armour volume)
    chest_fabric = fabric(21, 6)

    def chest(x, y, w, h):
        if y == 0:
            return W[7]
        if y in (h - 2,):
            return W[4]                          # hem fold above the waist bearing
        if y == h - 1:
            return W[3]
        if x in (0, w - 1) and y > 1:
            return W[5]
        if (x, y) in ((1, 2), (2, 2)):
            return A[5]                          # mission patch, right shoulder
        if (x, y) in ((1, 3), (2, 3)):
            return A[3]
        if y == 8 and x in (1, 6):
            return S[5]                          # cooling water / vent connectors
        return chest_fabric(x, y, w, h)

    def back(x, y, w, h):
        if y >= h - 2:
            return W[4] if y == h - 2 else W[3]
        return W[5] if y > 9 else W[6]

    def flank(x, y, w, h):
        if y == 0:
            return W[7]
        if y >= h - 2:
            return W[4] if y == h - 2 else W[3]
        return chest_fabric(x, y, w, h)

    box(cv, TORSO['torso'], north=chest, south=back, west=flank, east=flank,
        top=lambda x, y, w, h: W[7] if 0 < y < h - 1 else W[6], bottom=solid(W[3]))

    # chest control box: grey panel, display window, knobs and a row of switches
    def panel(x, y, w, h):
        if y == 0:
            return S[6]
        if y == 1:
            if 1 <= x <= 4:
                return S[1]                      # display window (glows from the overlay)
            return S[7] if x == 0 else S[3]      # knobs
        return S[6] if x % 2 == 0 else S[3]      # switches
    box(cv, TORSO['box'], north=panel, top=solid(S[6]), bottom=solid(S[2]),
        west=solid(S[5]), east=solid(S[3]), south=solid(S[3]))

    # life support backpack
    def pack_back(x, y, w, h):
        if y == h - 1:
            return S[3] if x % 2 == 0 else S[2]   # vent slots along the bottom
        if 3 <= x <= 4 and 1 <= y <= 8:
            return S[4] if y % 2 else S[5]        # radiator grille between the bottles
        if y == 0:
            return W[7]
        if x == 0:
            return W[6]
        return W[5] if x == w - 1 else W[6]

    def pack_side(x, y, w, h):
        if y in (2, 3) and x == 0:
            return A[5] if y == 2 else A[3]       # umbilical connector
        if y == 0:
            return W[7]
        if y == h - 1:
            return W[4]
        return W[6] if x == 0 else W[5]

    box(cv, TORSO['plss'], south=pack_back, west=pack_side, east=pack_side, north=solid(W[4]),
        top=lambda x, y, w, h: W[7], bottom=lambda x, y, w, h: S[3] if x % 2 else S[2])

    # oxygen bottles: rounded aluminium with a cyan band, shaded as cylinders
    def bottle(x, y, w, h):
        if y == 0:
            return S[5] if x == 0 else S[3]       # shoulder of the bottle
        if y == 2:
            return C[5] if x == 0 else C[3]       # oxygen band
        if y == h - 1:
            return S[4] if x == 0 else S[3]
        return S[7] if x == 0 else S[5]
    for name in ('bottle_r', 'bottle_l'):
        box(cv, TORSO[name], sides=bottle, top=lambda x, y, w, h: CU[5] if (x, y) == (0, 0) else S[4], bottom=solid(S[3]))

    # vent cap and antenna
    box(cv, TORSO['cap'], top=lambda x, y, w, h: S[2] if x % 2 else S[5], sides=solid(S[5]),
        north=solid(S[4]), bottom=solid(S[3]))
    box(cv, TORSO['antenna'], top=solid(S[7]), sides=lambda x, y, w, h: S[6] if y == 0 else S[4], bottom=solid(S[3]))

    # sleeves: fabric with a fold line, gloves grey with dark palm grip
    sleeve_fabric = fabric(23, 6)

    def sleeve(x, y, w, h):
        return sleeve_fabric(x, y, w, h)
    box(cv, TORSO['sleeve'], sides=sleeve, top=solid(W[7]), bottom=solid(W[4]))

    def glove(x, y, w, h):
        if y == h - 1:
            return R[3]
        return S[6] if (y == 0 or x == 0) else S[5]
    box(cv, TORSO['glove'], sides=glove, top=solid(S[6]), bottom=solid(R[3]))

    box(cv, TORSO['wrist'], sides=ring_side, top=ring_flat, bottom=solid(A[2]))
    for name in ('elbow', 'shoulder'):
        box(cv, TORSO[name], sides=joint_side, top=joint_flat, bottom=solid(W[3]))

    # Maneuvering Unit: white frame towers with steel thruster blocks at the corners
    def tower(x, y, w, h):
        if y in (0, h - 1):
            return S[2] if x % 2 == 0 else S[4]   # thruster nozzles
        if y == 4:
            return G[5]                           # gold foil band
        return W[6] if x == 0 else W[5]
    for name in ('mmu_r', 'mmu_l'):
        box(cv, TORSO[name], sides=tower, top=solid(S[4]), bottom=solid(S[3]))
    box(cv, TORSO['mmu_bar'], sides=lambda x, y, w, h: S[5], top=solid(S[6]), bottom=solid(S[3]))
    return cv


# ----------------------------------------------------------------------------------- legs

def eva_legs() -> Canvas:
    cv = Canvas(64, 32)
    hips = fabric(31, 6)
    box(cv, LEGS['hips'], sides=hips, top=solid(W[6]), bottom=solid(W[4]))
    box(cv, LEGS['waist'], sides=ring_side, top=ring_flat, bottom=solid(A[2]))
    leg_fabric = fabric(33, 6)

    def leg(x, y, w, h):
        if y == h - 1:
            return W[4]
        return leg_fabric(x, y, w, h)

    def leg_front(x, y, w, h):
        if y == 6 and 1 <= x <= 2:
            return W[5]                           # knee patch
        return leg(x, y, w, h)
    box(cv, LEGS['leg'], sides=leg, north=leg_front, top=solid(W[6]), bottom=solid(W[4]))
    box(cv, LEGS['knee'], sides=joint_side, top=joint_flat, bottom=solid(W[3]))
    return cv


# ----------------------------------------------------------------------------------- boots

def eva_boots(magnetic=False) -> Canvas:
    cv = Canvas(64, 32)

    def upper(x, y, w, h):
        if magnetic and y == h - 1:
            return CU[4] if x % 2 == 0 else CU[2]   # copper electromagnet coil band
        if y == h - 1:
            return W[4]
        if y == 0:
            return W[7]
        if x == 0:
            return W[6]
        return W[5] if x == w - 1 else W[6]

    def upper_front(x, y, w, h):
        if 1 <= y <= 2 and 1 <= x <= 4:
            return S[5] if (x + y) % 2 else S[4]    # lacing / strap
        return upper(x, y, w, h)

    box(cv, BOOTS['upper'], sides=upper, north=upper_front, top=solid(W[6]), bottom=solid(R[2]))
    sole = (S[3], S[2], S[5]) if magnetic else (R[3], R[2], R[4])

    def sole_side(x, y, w, h):
        return sole[0] if x % 3 else sole[1]
    box(cv, BOOTS['sole'], sides=sole_side, top=solid(sole[2]), bottom=lambda x, y, w, h: sole[1] if (x + y) % 2 else sole[0])
    box(cv, BOOTS['toe'], sides=lambda x, y, w, h: R[4] if y == 0 else R[3], north=lambda x, y, w, h: R[5] if y == 0 else R[3],
        top=solid(R[5]), bottom=solid(R[2]))
    box(cv, BOOTS['ankle'], sides=ring_side, top=ring_flat, bottom=solid(A[2]))
    return cv


# ----------------------------------------------------------------------------------- overlay (glass, gold, glow)

def eva_suit_overlay() -> Canvas:
    cv = Canvas(64, 32)

    def glass(x, y, w, h):
        # smoked glass, a soft diagonal sky reflection at the top-left, darker towards the chin
        if (x + y) in (1, 2) and y <= 2:
            return (*V[7], 200) if (x + y) == 1 else (*V[6], 160)
        if (x, y) in ((w - 2, 3), (w - 3, 4)):
            return (*V[5], 150)                    # second, fainter reflection lower right
        if y == 0:
            return (*V[1], 170)
        return (*V[2 if y < 3 else 3], 135)

    def glass_edge(x, y, w, h):
        return (*V[2], 90)

    box(cv, OVERLAY['glass'], north=glass, west=glass_edge, east=glass_edge, top=glass_edge, bottom=glass_edge,
        south=solid(TRANSPARENT))

    # gold sun visor: bright sky reflection on top, a crisp horizon, warm ground below
    rows = [G[6], G[7], G[5], G[4], G[3]]

    def gold(x, y, w, h):
        c = rows[min(y, len(rows) - 1)]
        if (x, y) in ((1, 0), (2, 0), (1, 1)):
            return G[7]
        if x == w - 1:
            return G[max(0, G.index(c) - 1)]
        return c
    box(cv, OVERLAY['gold'], north=gold, west=lambda x, y, w, h: rows[y], east=lambda x, y, w, h: G[max(0, G.index(rows[y]) - 1)],
        top=solid(G[6]), bottom=solid(G[2]), south=solid(G[2]))
    box(cv, OVERLAY['gold_up'], north=lambda x, y, w, h: G[6] if x < w - 2 else G[4], top=solid(G[5]),
        bottom=solid(G[3]), west=solid(G[5]), east=solid(G[4]), south=solid(G[3]))

    # glowing parts: lamp lenses, chest display, status light
    cv[24, 0] = L[5]
    cv[24, 1] = L[3]
    cv[26, 0] = L[5]
    cv[26, 1] = L[3]
    for x, c in enumerate((C[6], C[4], (98, 210, 120), C[4])):
        cv[24 + x, 4] = c
    cv[25, 6] = (90, 220, 110)
    cv[26, 6] = (90, 220, 110)
    return cv


# ----------------------------------------------------------------------------------- inventory icons
# Vanilla armour icon style: two-tone outline (dark grey top-left, near black bottom-right).

ICON_HELMET = [
    "................",
    "................",
    ".....oooooo.....",
    "....oHHWWWWk....",
    "...oHWWWWWWwk...",
    "..oHWWWWWWWwwk..",
    "..oWWvvvvvvvwk..",
    ".oLWvrRvvvvvwLk.",
    ".oLWvRvvvvvvwLk.",
    "..oWvvvvvvvvwk..",
    "..oWwvvvvvvwgk..",
    "..oBBBBBBBBbbk..",
    "...kkkkkkkkkk...",
    "................",
    "................",
    "................",
]

ICON_TORSO = [
    "................",
    "..ooooo..ooooo..",
    ".oHWWWo..oWWwgk.",
    ".oWWWWWookWWwgk.",
    ".oWWWWWWWWWWwgk.",
    ".oBBWsSSSSsWwBk.",
    ".kWWWsdsdssWwgk.",
    ".kWkWWWWWWWWwkgk",
    "..kkWWWWWWWwwkk.",
    "...kWWWWWWWwgk..",
    "...kWWYWWYWwgk..",
    "...kWWWWWWWwgk..",
    "...kBBBBBBBBbk..",
    "....kkkkkkkkk...",
    "................",
    "................",
]

ICON_LEGS = [
    "................",
    "................",
    "...oooooooook...",
    "...oBBBBBBBbk...",
    "...oHWWWWWwgk...",
    "...oWWWkkWwgk...",
    "...oWWk..kWgk...",
    "...oBBk..kBbk...",
    "...oWwk..kWgk...",
    "...oWwk..kWgk...",
    "...oWwk..kWgk...",
    "...oWgk..kWgk...",
    "...kggk..kggk...",
    "...kkkk..kkkk...",
    "................",
    "................",
]

ICON_BOOTS = [
    "................",
    "................",
    "................",
    "....ooo..ooo....",
    "...oBBbk.oBBbk..",
    "...oHWgk.oHWgk..",
    "...oWWgk.oWWgk..",
    "...oWwgk.oWwgk..",
    "...oWwgk.oWwgk..",
    "..oWWwgk.oWwwgk.",
    ".oWWwwgk.oWWwwgk",
    ".ossssskksssssk.",
    ".kkkkkk..kkkkkk.",
    "................",
    "................",
    "................",
]


ICON_LARGE_TANK = [
    "......bbbb......",
    "......BkkB......",
    ".......SS.......",
    ".....oSSSSo.....",
    "....oHWWWwgo....",
    "...oHWWWWwwgo...",
    "...oCCcCcccco...",
    "...oCcccccccO...",
    "...oWHWWWwwgo...",
    "...oWHWWWwwgo...",
    "...oWHWWWwwgo...",
    "...oCCcCcccco...",
    "...oCcccccccO...",
    "...oWHWWWwwgo...",
    "...omsssssmmo...",
    "....oooooooo....",
]

ICON_MMU = [
    "................",
    "..mm........mm..",
    ".mSSo.oooo.oSSm.",
    ".oWWooHWWwooWwo.",
    ".oWWoHWWWWwoWwo.",
    ".oYYoWsddsWoYYo.",
    ".oWWoWWWWWwoWwo.",
    ".oWWoWdsdsWoWwo.",
    ".oWWoWWWWWwoWwo.",
    ".oWWoowwwwooWwo.",
    ".oWWWWWWWWWWWwo.",
    ".owgggggggggggo.",
    ".mSSo......oSSm.",
    "..mm........mm..",
    "................",
    "................",
]


ICON_SMALL_TANK = [
    "................",
    "......bbbb......",
    "......mbbm......",
    ".......ss.......",
    "......osso......",
    ".....oHWWwo.....",
    "....oHWWWwwo....",
    "....oCCcCcco....",
    "....oCccccco....",
    "....oWHWWwwo....",
    "....oWHWWwwo....",
    "....oWHWWwwo....",
    "....oWHWWwwo....",
    "....omsssmmo....",
    ".....oooooo.....",
    "................",
]


def icon_small_tank() -> Canvas:
    pal = {'o': W[0], 'W': W[6], 'H': W[7], 'w': W[4], 'C': C[5], 'c': C[3], 's': S[5], 'm': S[3], 'b': CU[4]}
    return _icon(ICON_SMALL_TANK, pal)


def icon_large_tank() -> Canvas:
    pal = {'o': W[0], 'O': W[0], 'W': W[6], 'H': W[7], 'w': W[4], 'g': W[4], 'C': C[5], 'c': C[3],
           's': S[5], 'S': S[5], 'm': S[3], 'b': CU[4], 'B': S[3], 'k': S[3]}
    return _icon(ICON_LARGE_TANK, pal)


def icon_mmu() -> Canvas:
    pal = {'o': S[1], 'W': W[6], 'H': W[7], 'w': W[4], 'g': W[2], 'Y': G[5], 's': S[5], 'S': S[5], 'd': S[3], 'm': S[2]}
    return _icon(ICON_MMU, pal)


def _icon(rows, pal) -> Canvas:
    cv = Canvas()
    for y, row in enumerate(rows):
        for x, ch in enumerate(row):
            if ch in pal:
                cv[x, y] = pal[ch]
    return cv


OUTLINE = {'o': (45, 45, 45), 'k': (25, 25, 25)}


def icon_helmet() -> Canvas:
    return _icon(ICON_HELMET, OUTLINE | {'H': W[7], 'W': W[6], 'w': W[4], 'g': W[4], 'v': V[1], 'r': V[6], 'R': V[6],
                                         'B': A[5], 'b': A[5], 'L': L[3]})


def icon_torso() -> Canvas:
    return _icon(ICON_TORSO, OUTLINE | {'H': W[7], 'W': W[6], 'w': W[4], 'g': W[4], 'B': A[5], 'b': A[5],
                                        's': S[5], 'S': S[5], 'd': S[2], 'Y': G[5]})


def icon_legs() -> Canvas:
    return _icon(ICON_LEGS, OUTLINE | {'H': W[7], 'W': W[6], 'w': W[4], 'g': W[2], 'B': A[5], 'b': A[3]})


def icon_boots(magnetic=False) -> Canvas:
    pal = OUTLINE | {'H': W[7], 'W': W[6], 'w': W[4], 'g': W[2], 'B': A[5], 'b': A[3], 's': R[4]}
    if magnetic:
        pal |= {'s': CU[4], 'g': S[4], 'w': S[6], 'W': S[7], 'H': W[6]}
    return _icon(ICON_BOOTS, pal)


def export(save):
    save(icon_small_tank(), 'item', 'oxygen_tank')
    save(icon_helmet(), 'item', 'eva_helmet')
    save(icon_torso(), 'item', 'eva_chestplate')
    save(icon_legs(), 'item', 'eva_leggings')
    save(icon_boots(), 'item', 'eva_boots')
    save(icon_boots(True), 'item', 'magnetic_boots')
    save(icon_large_tank(), 'item', 'large_oxygen_tank')
    save(icon_mmu(), 'item', 'maneuvering_unit')
    save(eva_helmet(), 'models/armor', 'eva_helmet')
    save(eva_torso(), 'models/armor', 'eva_torso')
    save(eva_legs(), 'models/armor', 'eva_legs')
    save(eva_boots(), 'models/armor', 'eva_boots')
    save(eva_boots(True), 'models/armor', 'magnetic_boots')
    save(eva_suit_overlay(), 'models/armor', 'eva_suit_overlay')
