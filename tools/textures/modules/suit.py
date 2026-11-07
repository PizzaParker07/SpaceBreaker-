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
    'valve': (36, 6, 2, 1, 1),
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
    cv = Canvas(128, 64)
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
    box(cv, HELMET['valve'], north=solid(S[5]), south=lambda x, y, w, h: S[7] if x == 0 else S[6],
        top=solid(S[7]), bottom=solid(S[4]), west=solid(S[6]), east=solid(S[5]))
    return cv


def _is_back_column(face, x, w):
    """West faces run back -> front (column 0 = back), east faces front -> back."""
    return x == 0 if face == 'west' else x == w - 1


# ----------------------------------------------------------------------------------- torso

def eva_torso() -> Canvas:
    cv = Canvas(128, 64)

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
    cv = Canvas(128, 32)
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
    cv = Canvas(128, 32)

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
        # smoked glass reflecting the scene like a curved visor: a bright diagonal sky glint at the top-left,
        # a pale horizon band across the lower third, a small second glint lower right, darker at the chin
        if (x + y) in (1, 2, 3) and y <= 2 and x <= 3:
            return (*V[7], 215) if (x + y) == 2 else (*V[6], 170)
        if (x, y) in ((w - 2, 2), (w - 3, 3)):
            return (*V[6], 160)
        if y == 3 and 0 < x < w - 1:
            return (*V[4], 150)
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


def oxygen_tank_model() -> Canvas:
    """UV sheet of the in-hand 3D oxygen tanks (both sizes), 9 colours.
    Regions:
      (0,0)-(6,11)   body side: white paint lit from the left, cyan O2 band, steel foot ring
      (6,0)-(12,6)   end caps (white)
      (12,0)-(16,4)  collar (steel)
      (12,4)-(16,6)  brass valve
      (12,6)-(16,8)  hand wheel (rubber)
      (6,6)-(8,8)    gauge face, (8,6)-(10,8) gauge body
    """
    cv = Canvas()
    rows = ['W', 'W', 'C', 'c', 'W', 'W', 'W', 'W', 'W', 'S', 's']
    shade = [W[7], W[7], W[6], W[6], W[6], S[6]]
    for y, kind in enumerate(rows):
        for x in range(6):
            if kind == 'W':
                c = shade[x]
            elif kind == 'C':
                c = C[5] if x < 4 else C[3]
            elif kind == 'c':
                c = C[3]
            elif kind == 'S':
                c = S[6]
            else:
                c = S[4]
            cv[x, y] = c
    for y in range(6):
        for x in range(6, 12):
            edge = x in (6, 11) or y in (0, 5)
            cv[x, y] = W[6] if edge else W[7]
    for y in range(4):
        for x in range(12, 16):
            cv[x, y] = S[6] if y < 2 else S[4]
    for x in range(12, 16):
        cv[x, 4] = CU[4]
        cv[x, 5] = CU[2]
        cv[x, 6] = R[2]
        cv[x, 7] = R[2]
    cv[6, 6] = W[7]
    cv[7, 6] = W[7]
    cv[6, 7] = W[7]
    cv[7, 7] = R[2]                           # needle
    for y in range(6, 8):
        for x in range(8, 10):
            cv[x, y] = S[4]
    return cv


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


# ===================================================================================== suit tiers
# Every tier reuses the Mk1 painters with its own material ramp (module globals W and A are swapped while a
# sheet is drawn), then gets its tier-only parts painted on top. Accent colours (bearing rings, patches) are
# drawn with a sentinel ramp and split into a grey "_accent" sheet that the game tints with the dye colour.
from sbtex.core import ramp as _ramp

WHITE = P.SUIT_WHITE
BLUE = P.ANODIZED_BLUE
SILVER = _ramp('#3f4247', '#53575d', '#6a6e75', '#82878e', '#9a9fa6', '#b0b4ba', '#c4c8cd', '#d8dbdf')
ORANGE = _ramp('#4a1f0a', '#6e2f0f', '#924015', '#b5521c', '#d06626', '#e47c36', '#f1974f', '#f8b46f')
LEAD = _ramp('#1e2124', '#2b2f33', '#3a3e44', '#4a4f56', '#5c626a', '#70767e', '#868c94', '#9ea4ab')
HAZARD = _ramp('#4a3606', '#6e520a', '#93700f', '#b58e17', '#d2aa22', '#e6c235', '#f2d65a', '#fae896')
DARK_STEEL = _ramp('#121418', '#1b1e23', '#262a30', '#31363d', '#3d434b', '#4b525b', '#5c636d', '#6f7781')
GOLD_ANOD = _ramp('#3d2a08', '#5e420d', '#7f5a12', '#a07318', '#c08d22', '#d8a636', '#e8c055', '#f4d888')
SENT = [(255, 0, 32 * i) for i in range(8)]
GREY = [(g, g, g) for g in (60, 80, 105, 135, 170, 215, 245, 255)]

TIER_RAMPS = {  # primary, accent (default dye colour ramp)
    'eva': (WHITE, BLUE),
    'thermal': (SILVER, GOLD_ANOD),
    'heated': (ORANGE, DARK_STEEL),
    'radiation': (LEAD, HAZARD),
}


def _with_ramps(primary, accent, fn, *args):
    global W, A
    old = W, A
    W, A = primary, accent
    try:
        return fn(*args)
    finally:
        W, A = old


def _split_accent(cv, accent_ramp):
    """Sentinel pixels -> grey accent sheet; in the base sheet they get the tier's default accent colour."""
    acc = Canvas(cv.w, cv.h)
    lookup = {c: i for i, c in enumerate(SENT)}
    for y in range(cv.h):
        for x in range(cv.w):
            c = cv.p[x, y]
            if c[3] and c[:3] in lookup:
                k = lookup[c[:3]]
                acc[x, y] = GREY[k]
                cv[x, y] = accent_ramp[k]
    return cv, acc


def _flat(r, tl=7, mid=6, br=4):
    """Plate face lit from the top-left: light top row and left column, dark bottom/right edge."""
    def fn(x, y, w, h):
        if (y == 0 or x == 0) and not (y == h - 1 and h > 1):
            return r[tl]
        if (y == h - 1 and h > 1) or (x == w - 1 and w > 1):
            return r[br]
        return r[mid]
    return fn


def _ribbed(r, period=2):
    """Radiator / louvre face: alternating fins."""
    def fn(x, y, w, h):
        if y == 0 or x == 0:
            return r[7]
        if x == w - 1 or y == h - 1:
            return r[3]
        return r[6] if y % period == 0 else r[4]
    return fn


def _quilted(r):
    """Padded insulation: soft horizontal baffles (like a down jacket), lit from the top-left."""
    def fn(x, y, w, h):
        if y % 3 == 2:
            return r[4]                      # stitched seam between baffles
        if y % 3 == 0:
            return r[6] if x > 0 else r[7]   # puffed top of a baffle catches the light
        return r[5]
    return fn


def helmet_extras(cv, tier):
    if tier == 'thermal':
        box(cv, (64, 28, 10, 1, 2), top=_flat(SILVER), north=_flat(G, 7, 6, 4), sides=_flat(SILVER, 6, 5, 3), bottom=solid(G[3]))
    elif tier == 'heated':
        box(cv, (64, 0, 12, 2, 12), sides=_quilted(ORANGE), top=_quilted(ORANGE), bottom=solid(ORANGE[2]))
        box(cv, (64, 14, 4, 1, 4), top=lambda x, y, w, h: S[6] if (x + y) % 2 else S[4], sides=solid(S[4]), bottom=solid(S[2]))
    elif tier == 'radiation':
        plate = _flat(HAZARD, 6, 5, 3)
        box(cv, (64, 20, 10, 2, 1), all=plate, bottom=solid(HAZARD[2]))
        box(cv, (64, 24, 10, 2, 1), all=plate, top=solid(HAZARD[3]))
        for u in (88, 104):
            box(cv, (u, 20, 1, 4, 6), all=_flat(LEAD, 6, 5, 3))
        box(cv, (64, 30, 2, 2, 8), sides=_ribbed(HAZARD), top=_flat(HAZARD), bottom=solid(HAZARD[2]))


def torso_extras(cv, tier):
    if tier == 'thermal':
        for u in (64, 76):
            box(cv, (u, 0, 5, 8, 1), north=_ribbed(G), south=_ribbed(G), sides=solid(G[3]), top=solid(G[6]), bottom=solid(G[2]))
    elif tier == 'heated':
        def pack(x, y, w, h):
            if y == 1 and 1 <= x <= 4:
                return (255, 150, 60) if x % 2 else (220, 110, 40)   # heater status bars
            return _flat(DARK_STEEL, 6, 5, 3)(x, y, w, h)
        box(cv, (64, 24, 6, 3, 1), north=pack, sides=solid(DARK_STEEL[4]), top=solid(DARK_STEEL[6]), bottom=solid(DARK_STEEL[2]))
        box(cv, (64, 16, 4, 3, 4), sides=_quilted(ORANGE), top=_quilted(ORANGE), bottom=solid(ORANGE[3]))
    elif tier == 'radiation':
        def abdomen(x, y, w, h):
            if y in (1, 2) and x in (1, w - 2):
                return HAZARD[2]                                  # bolts
            return _flat(HAZARD, 6, 5, 3)(x, y, w, h)
        box(cv, (64, 40, 8, 4, 1), north=abdomen, sides=solid(HAZARD[3]), top=solid(HAZARD[6]), bottom=solid(HAZARD[2]))

        def shield(x, y, w, h):
            if x in (0, w - 1) or y in (0, h - 1):
                return LEAD[6] if (x == 0 or y == 0) else LEAD[2]
            if (x + y) % 4 == 0 and 2 <= y <= h - 3:
                return HAZARD[5]                                  # hazard chevrons
            return LEAD[4]
        box(cv, (88, 32, 10, 12, 1), south=shield, north=solid(LEAD[3]), sides=solid(LEAD[3]), top=solid(LEAD[5]), bottom=solid(LEAD[2]))
        box(cv, (64, 32, 6, 2, 6), sides=_flat(HAZARD, 6, 5, 3), top=_flat(HAZARD), bottom=solid(HAZARD[2]))


def legs_extras(cv, tier):
    if tier == 'thermal':
        box(cv, (64, 0, 4, 2, 1), all=_flat(G, 7, 5, 3))
    elif tier == 'heated':
        box(cv, (64, 4, 4, 5, 4), sides=_quilted(ORANGE), top=solid(ORANGE[5]), bottom=solid(ORANGE[3]))
    elif tier == 'radiation':
        box(cv, (64, 16, 4, 5, 1), north=_flat(HAZARD, 6, 5, 3), sides=solid(HAZARD[3]), top=solid(HAZARD[6]), bottom=solid(HAZARD[2]))


def boots_extras(cv, tier):
    if tier == 'heated':
        box(cv, (64, 0, 6, 2, 6), sides=_quilted(ORANGE), top=solid(ORANGE[6]), bottom=solid(ORANGE[3]))
    elif tier == 'radiation':
        box(cv, (64, 8, 6, 4, 1), north=_flat(HAZARD, 6, 5, 3), sides=solid(HAZARD[3]), top=solid(HAZARD[6]), bottom=solid(HAZARD[2]))


def tier_sheets(tier):
    """{texture name: (base, accent)} for the four armour sheets of a tier."""
    primary, accent = TIER_RAMPS[tier]
    out = {}
    for piece, fn, extras in (('helmet', eva_helmet, helmet_extras), ('torso', eva_torso, torso_extras),
                              ('legs', eva_legs, legs_extras), ('boots', eva_boots, boots_extras)):
        cv = _with_ramps(primary, SENT, fn)
        extras(cv, tier)
        out[f'{tier}_{piece}'] = _split_accent(cv, accent)
    if tier == 'eva':
        cv = _with_ramps(primary, SENT, eva_boots, True)
        out['magnetic_boots'] = _split_accent(cv, accent)
    return out


# ------------------------------------------------------------------- tier icons (base + grey accent layer)

ICON_HELMET_RAD = [r.replace('v', 'P') if i in (6, 9, 10) else r for i, r in enumerate(ICON_HELMET)]


def tier_icons(tier):
    primary, accent = TIER_RAMPS[tier]
    pal = OUTLINE | {'H': primary[7], 'W': primary[6], 'w': primary[4], 'g': primary[4], 'B': SENT[5], 'b': SENT[5],
                     'v': V[1], 'r': V[6], 'R': V[6], 'L': L[3], 's': S[5], 'S': S[5], 'd': S[2], 'Y': G[5]}
    if tier == 'thermal':
        pal |= {'v': G[5], 'r': G[7], 'R': G[7], 'Y': G[6], 'd': G[3], 's': G[5], 'S': G[5]}
    elif tier == 'heated':
        pal |= {'s': DARK_STEEL[5], 'S': DARK_STEEL[5], 'd': (240, 130, 50), 'Y': DARK_STEEL[6]}
    elif tier == 'radiation':
        pal |= {'P': HAZARD[5], 's': HAZARD[5], 'S': HAZARD[5], 'd': HAZARD[2], 'Y': HAZARD[6]}
    boots = pal | {'s': R[4] if tier != 'radiation' else HAZARD[4]}
    icons = {
        f'{tier}_helmet': _icon(ICON_HELMET_RAD if tier == 'radiation' else ICON_HELMET, pal),
        f'{tier}_chestplate': _icon(ICON_TORSO, pal),
        f'{tier}_leggings': _icon(ICON_LEGS, pal),
        f'{tier}_boots': _icon(ICON_BOOTS, boots),
    }
    if tier == 'eva':
        mag = boots | {'s': CU[4], 'g': S[4], 'w': S[6], 'W': S[7], 'H': primary[6]}
        icons['magnetic_boots'] = _icon(ICON_BOOTS, mag)
    return {k: _split_accent(v, accent) for k, v in icons.items()}


# ------------------------------------------------------------------- upgrade items

ICON_TEMPLATE = [
    "................",
    "...oooooooooo...",
    "..oHWWWWWWWWwo..",
    "..oWaaaaaaaawo..",
    "..oWa......awo..",
    "..oWa.oooo.awo..",
    "..oWa.oHWo.awo..",
    "..oWa.oWwo.awo..",
    "..oWa.oooo.awo..",
    "..oWa......awo..",
    "..oWaaaaaaaawo..",
    "..oWWWWWWWWWwo..",
    "..owwwwwwwwwwo..",
    "...oooooooooo...",
    "................",
    "................",
]

ICON_MODULE = [
    "................",
    "................",
    "...oooooooooo...",
    "..oHWWWWWWWWwo..",
    "..oWccccccccwo..",
    "..oWcXXXXXXcwo..",
    "..oWcXYYYYXcwo..",
    "..oWcXYZZYXcwo..",
    "..oWcXYZZYXcwo..",
    "..oWcXYYYYXcwo..",
    "..oWcXXXXXXcwo..",
    "..oWccccccccwo..",
    "..owwwwwwwwwwo..",
    "...oPoPoPoPoo...",
    "................",
    "................",
]


def icon_template():
    # vanilla smithing templates are dark slabs with an engraved motif; ours carries a helmet outline
    return _icon(ICON_TEMPLATE, {'o': (30, 32, 36), 'H': S[6], 'W': S[4], 'w': S[2], 'a': A[5]})


def icon_module(core):
    # a circuit module: steel body, dark board, coloured core telling the function, gold pins
    pal = {'o': (30, 32, 36), 'H': S[7], 'W': S[5], 'w': S[3], 'c': S[2], 'X': (40, 70, 50), 'P': G[5]}
    pal |= core
    return _icon(ICON_MODULE, pal)


MODULE_CORES = {
    'thermal_regulator_module': {'Y': (230, 140, 40), 'Z': (255, 200, 90)},
    'radiation_shield_module': {'Y': HAZARD[4], 'Z': (40, 40, 40)},
    'extra_oxygen_module': {'Y': C[4], 'Z': C[6]},
    'jetpack_booster_module': {'Y': (200, 80, 60), 'Z': (250, 220, 120)},
}


def export_tiers(save):
    for tier in TIER_RAMPS:
        for name, (base, acc) in tier_sheets(tier).items():
            save(base, 'models/armor', name)
            save(acc, 'models/armor', name + '_accent')
        for name, (base, acc) in tier_icons(tier).items():
            save(base, 'item', name)
            save(acc, 'item', name + '_accent')
    save(icon_template(), 'item', 'suit_upgrade_template')
    for name, core in MODULE_CORES.items():
        save(icon_module(core), 'item', name)


# ===================================================================================== HUD icons
# 9x9 icons for the helmet HUD, drawn like vanilla's heart/armour/bubble icons: dark outline, flat colour,
# one highlight. Sheet: hud_icons.png, 9 px per icon in a row.
HUD_ICONS = {
    'oxygen': [
        "...KKK...",
        "...KbK...",
        "..KKKKK..",
        ".KHWWWsK.",
        ".KWCCCsK.",
        ".KWCCCsK.",
        ".KHWWWsK.",
        ".KHWWWsK.",
        "..KKKKK..",
    ],
    'suit': [
        ".KKKKKKK.",
        "KHWWWWWsK",
        "KWVVVVVsK",
        "KWVvVVVsK",
        "KWVVVVVsK",
        "KHWWWWWsK",
        ".KBBBBBK.",
        "..KKKKK..",
        ".........",
    ],
    'temperature': [
        "...KKK...",
        "...KWK...",
        "...KWK...",
        "...KrK...",
        "...KrK...",
        "..KrrrK..",
        ".KrRRrrK.",
        ".KrrrrrK.",
        "..KKKKK..",
    ],
    'radiation': [
        ".........",
        ".KKK.KKK.",
        "KYYYKYYYK",
        "KYYKKKYYK",
        ".KKKYKKK.",
        "...KKK...",
        "..KYYYK..",
        "..KYYYK..",
        "...KKK...",
    ],
    'pressure': [
        "..KKKKK..",
        ".KHWWWsK.",
        "KHWWWKWsK",
        "KWWWKWWsK",
        "KWWKKWWsK",
        "KWWWWWWsK",
        ".KsWWWsK.",
        "..KKKKK..",
        ".........",
    ],
    'mmu': [
        "..KKKKK..",
        ".KHWWWsK.",
        ".KWSSSsK.",
        ".KWWWWsK.",
        ".KKKKKKK.",
        "..KS.SK..",
        "..Kc.cK..",
        "...c.c...",
        "...c.c...",
    ],
}
HUD_ORDER = ('oxygen', 'suit', 'temperature', 'radiation', 'pressure', 'mmu')


def hud_icons() -> Canvas:
    pal = {'K': (24, 24, 28), 'H': W[7], 'W': W[6], 's': W[3], 'C': C[5], 'b': CU[4], 'V': V[2], 'v': V[6],
           'B': A[5], 'r': (200, 50, 40), 'R': (255, 140, 120), 'Y': (240, 200, 40), 'S': S[5], 'c': (210, 230, 240)}
    cv = Canvas(9 * len(HUD_ORDER), 9)
    for i, name in enumerate(HUD_ORDER):
        for y, row in enumerate(HUD_ICONS[name]):
            for x, ch in enumerate(row):
                if ch in pal:
                    cv[i * 9 + x, y] = pal[ch]
    return cv


# Safety tether: a coil of safety-orange line, its tail running up to a steel carabiner
ICON_TETHER = [
    "...........oo...",
    "..........oSSo..",
    ".........oSoosS.",
    ".........oSo.os.",
    ".........oSo.os.",
    "....oooo.oSo.os.",
    "..ooHHHHooSo.os.",
    ".oHHhhhhhooSsso.",
    "oHhooooooddooo..",
    "oHo.oHHo.oHdo...",
    "oho.oHdo.ohdo...",
    "ohho.oo.ohhdo...",
    ".ohhhoooohddo...",
    "..oddhhhhddo....",
    "...oooooooo.....",
    "................",
]


def icon_tether() -> Canvas:
    pal = {'o': (45, 30, 18), 'H': ORANGE[7], 'h': ORANGE[5], 'd': ORANGE[3], 'S': S[7], 's': S[4]}
    return _icon(ICON_TETHER, pal)

# ----------------------------------------------------------------------------- helmet lamp light
LAMP_LIGHT = (255, 238, 200)


def lamp_beam() -> Canvas:
    """Short visible beam in dusty or misty air: brightest at the lamp (v = 0), fading out in steps."""
    cv = Canvas(16, 32)
    for y in range(32):
        a = int(170 * (1 - y / 32) ** 1.3) // 8 * 8
        for x in range(16):
            edge = min(x, 15 - x)
            k = 1.0 if edge >= 4 else (0.6 if edge >= 2 else 0.3)
            if a > 0:
                cv[x, y] = (*LAMP_LIGHT, int(a * k))
    return cv


# ----------------------------------------------------------------------------- visor frost (HUD)
def visor_frost() -> Canvas:
    """Rime creeping in from the visor rim, drawn over the whole screen when the suit is beyond its cold rating.
    160 x 90 so it scales up to the screen in big, vanilla-looking pixels; ice crystals grow in from the edges
    (more at the corners), a few feathery spikes reach further in. 4 colours, 3 alpha steps."""
    rnd = random.Random(907)
    w, h = 160, 90
    cv = Canvas(w, h)
    ice = [(236, 246, 252), (212, 232, 246), (186, 214, 236), (160, 196, 226)]
    # coarse value noise for a ragged edge
    grid = [[rnd.random() for _ in range(w // 4 + 2)] for _ in range(h // 4 + 2)]

    def noise(x, y):
        gx, gy = x / 4, y / 4
        x0, y0 = int(gx), int(gy)
        fx, fy = gx - x0, gy - y0
        a = grid[y0][x0] * (1 - fx) + grid[y0][x0 + 1] * fx
        b = grid[y0 + 1][x0] * (1 - fx) + grid[y0 + 1][x0 + 1] * fx
        return a * (1 - fy) + b * fy

    for y in range(h):
        for x in range(w):
            ex = min(x, w - 1 - x) / (w / 2)
            ey = min(y, h - 1 - y) / (h / 2)
            d = min(ex * 1.3, ey) - (1 - ex) ** 3 * (1 - ey) ** 3 * 0.5  # corners frost deeper
            v = noise(x, y) * 0.09 + rnd.random() * 0.02
            depth = 0.04 + v - d
            if depth > 0:
                step = 2 if depth > 0.12 else (1 if depth > 0.05 else 0)
                alpha = (110, 170, 215)[step]
                cv[x, y] = (*ice[min(3, int(rnd.random() * 2) + (2 - step))], alpha)
    # feathery spikes growing inwards from the rim
    for _ in range(46):
        side = rnd.choice('tblr')
        if side in 'tb':
            x, y = rnd.randrange(w), (2 if side == 't' else h - 3)
            dx, dy = rnd.choice((-1, 0, 1)), (1 if side == 't' else -1)
        else:
            x, y = (2 if side == 'l' else w - 3), rnd.randrange(h)
            dx, dy = (1 if side == 'l' else -1), rnd.choice((-1, 0, 1))
        for i in range(rnd.randrange(5, 14)):
            if cv.inside(x, y):
                cv[x, y] = (*ice[0], 200)
                if i % 3 == 1:
                    for bx, by in ((x + dy, y + dx), (x - dy, y - dx)):
                        if cv.inside(bx, by):
                            cv[bx, by] = (*ice[1], 150)
            x += dx if rnd.random() < 0.8 else 0
            y += dy if rnd.random() < 0.8 else 0
    return cv


def visor_frame() -> Canvas:
    """Inside of the helmet seen in first person: a rounded visor opening (curved corners, a little more brow at the
    top), the dark rim fading in over a few pixels in steps, and faint reflections of the visor glass along the
    upper left curve and the lower right corner. 320 x 180, stretched over the screen; drawn on the GUI layer."""
    w, h = 320, 180
    cv = Canvas(w, h)
    left, right, top, bottom, r = 9, w - 10, 13, h - 8, 34
    rim = (16, 19, 24)

    def outside(x, y):
        """Distance outside the rounded opening (negative inside)."""
        cx = min(max(x, left + r), right - r)
        cy = min(max(y, top + r), bottom - r)
        if left + r <= x <= right - r or top + r <= y <= bottom - r:
            return max(left - x, x - right, top - y, y - bottom)
        return ((x - cx) ** 2 + (y - cy) ** 2) ** 0.5 - r

    for y in range(h):
        for x in range(w):
            d = outside(x + 0.5, y + 0.5)
            if d >= 0:
                step = min(3, int(d / 2.5))
                cv[x, y] = (*rim, (90, 150, 205, 235)[step])
            elif d > -1.5:
                # the visor's glass edge, all the way round, so the frame reads whole even against a dark sky
                corner = x < left + r and y < top + r
                cv[x, y] = (196, 214, 228, 70) if corner else (120, 132, 146, 54)
    # two short glints on the glass: an arc in the upper left corner, a dash in the lower right
    for i in range(26):
        a = 3.14159 * (1.05 + 0.4 * i / 25)
        x = int(left + r + (r - 6) * __import__('math').cos(a))
        y = int(top + r + (r - 6) * __import__('math').sin(a))
        cv[x, y] = (226, 238, 246, 70)
    for i in range(14):
        cv[right - 30 + i, bottom - 6 - i // 4] = (226, 238, 246, 44)
    return cv


# Environment Sensor: a handheld instrument with a cyan screen, a keypad and a stub antenna
ICON_SENSOR = [
    "..........ss....",
    "..........ms....",
    "....oooooooso...",
    "...oHWWWWWWwo...",
    "...oWkkkkkkwo...",
    "...oWkCcCckwo...",
    "...oWkcCcCkwo...",
    "...oWkCCcckwo...",
    "...oWkkkkkkwo...",
    "...oWWWWWWWwo...",
    "...oWbWrWbWwo...",
    "...oWWWWWWWwo...",
    "...oWbWbWbWwo...",
    "...owwwwwwwwo...",
    "....oooooooo....",
    "................",
]


def icon_sensor() -> Canvas:
    pal = {'o': S[1], 'W': S[5], 'H': S[6], 'w': S[3], 'k': (24, 26, 30), 'C': C[5], 'c': C[3], 'b': S[2],
           'r': (200, 60, 50), 's': S[5], 'm': S[3]}
    return _icon(ICON_SENSOR, pal)


# ----------------------------------------------------------------------------- titanium tools
# Titanium heads (cool grey-violet ramp) on dark composite handles with a cyan grip band.
TOOL_ICONS = {
    'titanium_pickaxe': [
        "................",
        ".....ooooo......",
        "...ooHHTTToo....",
        "..oHToooooTTo...",
        ".oHto...oKotTo..",
        ".oTo...oKko.oTo.",
        ".oto..oKko...oo.",
        ".oo..oKko.......",
        "....oKko........",
        "...oCco.........",
        "..oKko..........",
        ".oKko...........",
        "oKko............",
        "oko.............",
        ".o..............",
        "................",
    ],
    'titanium_axe': [
        "................",
        "...ooooo........",
        "..oHHTTTo...oo..",
        ".oHTTTTTTo.oKo..",
        ".oTTTTTTTtoKko..",
        ".otTTTTTtoKko...",
        "..ottttooKko....",
        "...oooooKko.....",
        "......oKko......",
        ".....oCco.......",
        "....oKko........",
        "...oKko.........",
        "..oKko..........",
        ".oKko...........",
        ".oo.............",
        "................",
    ],
    'titanium_shovel': [
        "................",
        "...........ooo..",
        "..........oHTto.",
        ".........oHTTto.",
        "........oHTTtto.",
        ".........otttoo.",
        "........oKkoo...",
        ".......oKko.....",
        "......oKko......",
        ".....oCco.......",
        "....oKko........",
        "...oKko.........",
        "..oKko..........",
        ".oKko...........",
        ".oo.............",
        "................",
    ],
    'titanium_hoe': [
        "................",
        ".....oooooo.....",
        "....oHTTTTto....",
        ".....ooooKtoo...",
        "........oKko....",
        ".......oKko.....",
        "......oKko......",
        ".....oKko.......",
        "....oCco........",
        "...oKko.........",
        "..oKko..........",
        ".oKko...........",
        "oKko............",
        "oko.............",
        ".o..............",
        "................",
    ],
    'titanium_sword': [
        "............ooo.",
        "...........oHTo.",
        "..........oHTto.",
        ".........oHTto..",
        "........oHTto...",
        ".......oHTto....",
        "......oHTto.....",
        ".oo..oHTto......",
        ".oTooHTto.......",
        "..oTTtto........",
        "...oTto.........",
        "..oCcoTo........",
        ".oKko.oo........",
        "oKko............",
        ".oo.............",
        "................",
    ],
}


def tool_icon(name) -> Canvas:
    T = P.TITANIUM
    pal = {'o': T[0], 'H': T[7], 'T': T[5], 't': T[3], 'K': S[2], 'k': S[1], 'C': C[5], 'c': C[3]}
    return _icon(TOOL_ICONS[name], pal)


# Electric Drill: white body with a battery pack, cyan charge window, a steel chuck and a titanium bit
ICON_DRILL = [
    "................",
    "................",
    "....oooooooo....",
    "...oHWWWWWWwoooo",
    "...oWWWWWWWwossb",
    "...oWCcWWWWwoSSt",
    "...oWWWWWWwwoooo",
    "....oowwwoooo...",
    "......oKko......",
    "......oKko......",
    ".....oKKko......",
    ".....oBBbo......",
    ".....oBCbo......",
    ".....oBBbo......",
    "......oooo......",
    "................",
]


def icon_drill() -> Canvas:
    T = P.TITANIUM
    pal = {'o': S[1], 'W': W[6], 'H': W[7], 'w': W[4], 'C': C[5], 'c': C[5], 'K': S[2], 'k': S[1],
           'B': S[3], 'b': S[2], 's': S[5], 'S': S[3], 't': T[6]}
    return _icon(ICON_DRILL, pal)


# Wide-bore drill head: a broad titanium cutter crown with three teeth around a redstone-driven hub
ICON_DRILL_HEAD = [
    "................",
    "..oo...oo...oo..",
    "..oTo.oTTo.oTo..",
    "..oTToTHTToTTo..",
    "...oTTHHHTTto...",
    "...oTHHttTTto...",
    "....oTRRrTto....",
    "....oTRrrTto....",
    ".....otTTto.....",
    ".....oSSSso.....",
    "......oSso......",
    "......oSso......",
    "......oSso......",
    ".......oo.......",
    "................",
    "................",
]


def icon_drill_head() -> Canvas:
    T = P.TITANIUM
    pal = {'o': T[0], 'H': T[7], 'T': T[5], 't': T[3], 'R': (220, 40, 30), 'r': (150, 20, 15), 'S': S[5], 's': S[3]}
    return _icon(ICON_DRILL_HEAD, pal)


def export(save):
    save(hud_icons(), 'gui', 'hud_icons')
    save(visor_frost(), 'gui', 'visor_frost')
    save(visor_frame(), 'gui', 'visor_frame')
    export_blocks(save)
    save(icon_small_tank(), 'item', 'oxygen_tank')
    save(oxygen_tank_model(), 'item', 'oxygen_tank_model')
    save(icon_large_tank(), 'item', 'large_oxygen_tank')
    save(icon_mmu(), 'item', 'maneuvering_unit')
    save(icon_tether(), 'item', 'safety_tether')
    save(icon_sensor(), 'item', 'environment_sensor')
    save(icon_drill(), 'item', 'electric_drill')
    save(icon_drill_head(), 'item', 'wide_bore_drill_head')
    for name in TOOL_ICONS:
        save(tool_icon(name), 'item', name)
    export_tiers(save)
    save(eva_suit_overlay(), 'models/armor', 'eva_suit_overlay')
    save(lamp_beam(), 'misc', 'helmet_lamp_beam')


# ===================================================================================== life support blocks
# 16x16 block textures, 4-6 colours each, light from the top-left. Machines share a white painted casing
# (same paint as the suit), a galvanised steel plinth and blue anodised fittings.

LAMP_ON = (110, 230, 120)


def _canvas_fill(fn) -> Canvas:
    cv = Canvas()
    for y in range(16):
        for x in range(16):
            c = fn(x, y)
            if c is not None:
                cv[x, y] = c
    return cv


def casing() -> Canvas:
    """White painted panel: two panels with a seam, edge highlights, four rivets."""
    rng = random.Random(51)

    def fn(x, y):
        if y == 0 or x == 0:
            return W[7]
        if y == 15 or x == 15:
            return W[3]
        if y == 8:
            return W[3]                      # panel seam
        if y == 9:
            return W[6]
        if (x, y) in ((2, 2), (13, 2), (2, 13), (13, 13), (2, 6), (13, 6)):
            return W[3]                      # rivets
        return W[6]
    return _canvas_fill(fn)


def plinth() -> Canvas:
    """Galvanised steel base: horizontal brushed grain, darker lower edge."""
    rng = random.Random(52)

    def fn(x, y):
        if y == 0:
            return S[6]
        if y >= 14:
            return S[2]
        return S[4] if (rng.random() < 0.2 or (y % 4 == 2 and rng.random() < 0.5)) else S[5]
    return _canvas_fill(fn)


def steel_top() -> Canvas:
    """Top plate of a machine: steel with a bevelled edge and corner bolts."""
    rng = random.Random(53)

    def fn(x, y):
        if x == 0 or y == 0:
            return S[6]
        if x == 15 or y == 15:
            return S[2]
        if (x, y) in ((2, 2), (13, 2), (2, 13), (13, 13)):
            return S[6]
        if (x, y) in ((3, 3), (14, 3), (3, 14), (14, 14)):
            return S[2]
        return S[4] if rng.random() < 0.18 else S[5]
    return _canvas_fill(fn)


def ring_tex() -> Canvas:
    """Blue anodised cradle ring (used on small elements)."""
    def fn(x, y):
        if y == 0 or x == 0:
            return A[6]
        if y == 15 or x == 15:
            return A[3]
        return A[5] if (x + y) % 5 else A[4]
    return _canvas_fill(fn)


def filler_front(on=False) -> Canvas:
    """Casing with a cyan O2 band across the top, a centred pressure gauge and a small status lamp."""
    cv = casing()
    for x in range(1, 15):
        cv[x, 3] = C[4]                      # oxygen colour band
    dial = parse_mask([
        ".bbbb.",
        "bwwwwb",
        "bwwwnb",
        "bwwnwb",
        "bwwwwb",
        ".bbbb.",
    ])
    for y, row in enumerate(dial):
        for x, ch in enumerate(row):
            if ch == 'b':
                cv[5 + x, 6 + y] = S[3]
            elif ch == 'w':
                cv[5 + x, 6 + y] = W[7]
            elif ch == 'n':
                cv[5 + x, 6 + y] = S[3]
    cv[12, 11] = LAMP_ON if on else S[3]   # status lamp
    return cv


def filler_top() -> Canvas:
    """Top plate with the tank cradle and its quick-connect port."""
    cv = steel_top()
    for y in range(5, 11):
        for x in range(5, 11):
            edge = x in (5, 10) or y in (5, 10)
            cv[x, y] = A[5] if edge else S[2]
    cv[7, 7] = S[6]
    cv[8, 7] = S[4]
    cv[7, 8] = S[4]
    cv[8, 8] = S[2]
    return cv


def station_front(on=False) -> Canvas:
    """Suit maintenance station front: an amber service band, a small helmet sign and a status lamp."""
    cv = casing()
    for x in range(1, 15):
        cv[x, 2] = G[5]                      # amber service band
    sign = parse_mask([
        "..www...",
        ".wwwww..",
        "wwvvvww.",
        "wwvvvww.",
        ".wwwww..",
        "..bbb...",
    ])
    for y, row in enumerate(sign):
        for x, ch in enumerate(row):
            if ch == 'w':
                cv[4 + x, 5 + y] = W[7]
            elif ch == 'v':
                cv[4 + x, 5 + y] = V[2]
            elif ch == 'b':
                cv[4 + x, 5 + y] = V[2]
    cv[12, 12] = LAMP_ON if on else S[3]   # status lamp
    return cv


def station_top() -> Canvas:
    """Standing grate of the maintenance station: steel bars over the service ducts."""
    cv = steel_top()
    for y in range(2, 14):
        for x in range(2, 14):
            cv[x, y] = S[5] if (x - 2) % 3 != 2 else S[2]
    for x in range(2, 14):
        cv[x, 2] = S[3]
        cv[x, 13] = S[3]
    return cv


def distributor_front(on=False) -> Canvas:
    """Casing with an air outlet louvre and a status lamp."""
    cv = casing()
    for y in range(2, 8):
        for x in range(3, 13):
            cv[x, y] = S[2] if y % 2 == 0 else S[5]
    for x in range(6, 10):
        cv[x, 11] = S[2]
        cv[x, 13] = S[2]
    cv[6, 12] = S[2]
    cv[9, 12] = S[2]
    cv[7, 12] = LAMP_ON if on else S[2]
    cv[8, 12] = LAMP_ON if on else S[2]
    return cv


def distributor_top() -> Canvas:
    """Fan grille: round dark opening, four blades, steel rim."""
    cv = steel_top()
    import math
    for y in range(16):
        for x in range(16):
            dx, dy = x - 7.5, y - 7.5
            r = math.hypot(dx, dy)
            if r < 5.6:
                a = math.atan2(dy, dx)
                blade = (math.sin(a * 4 + r * 0.6) > 0.35)
                cv[x, y] = S[4] if blade else S[2]
                if r < 1.2:
                    cv[x, y] = A[5]
            elif r < 6.6:
                cv[x, y] = S[6] if (dx + dy) < 0 else S[2]
    return cv


def airlock_frame() -> Canvas:
    """Steel frame with hazard stripes along its inner edge."""
    rng = random.Random(54)
    Y = ((214, 170, 40), (40, 38, 36))

    def fn(x, y):
        if x <= 1:
            return Y[0] if ((x + y) // 2) % 2 == 0 else Y[1]   # hazard stripes
        if x == 2 or y == 0:
            return S[6]
        if x == 15 or y == 15:
            return S[2]
        return S[4] if rng.random() < 0.15 else S[5]
    return _canvas_fill(fn)


def airlock_panel(window=False) -> Canvas:
    """Door leaves: light painted panel, reinforcing ribs, meeting edges with a rubber seal.
    The upper texture has a small viewport across both leaves."""
    rng = random.Random(55)

    def fn(x, y):
        if x in (7, 8):
            return R[2]                                  # rubber seal where the leaves meet
        if x in (2, 13) and 0 < y < 15:
            return W[4]                                  # leaf edge
        if y in (3, 12) and not window:
            return W[4]                                  # rib
        if window and 4 <= y <= 8 and 4 <= x <= 11:
            if y in (4, 8) or x in (4, 11):
                return S[4]                              # viewport rim
            if (x + y) in (11, 12) and x < 7:
                return V[5]                              # reflection
            return V[2]
        if window and y == 12:
            return W[4]
        return W[5] if (rng.random() < 0.1 and not window) else W[6]
    return _canvas_fill(fn)


ICON_AIRLOCK = [
    "..ssssssssssss..",
    "..syyHWWkWWHyd..",
    "..sy.HWWkWWW.d..",
    "..syHWvvkvvWyd..",
    "..s.HWvvkvvW.d..",
    "..syHWWWkWWWyd..",
    "..s.HWWWkWWW.d..",
    "..syHWWWkWWWyd..",
    "..s.HWWWkWWW.d..",
    "..syHwwwkwwwyd..",
    "..s.HWWWkWWW.d..",
    "..syHWWWkWWWyd..",
    "..s.HWWWkWWW.d..",
    "..syHWWWkWWWyd..",
    "..s.wwwwkwwww.d.",
    "..sddddddddddd..",
]


def icon_airlock() -> Canvas:
    pal = {'s': S[5], 'd': S[2], 'y': (214, 170, 40), '.': (40, 38, 36), 'H': W[7], 'W': W[6], 'w': W[4], 'k': R[2], 'v': V[2]}
    cv = Canvas()
    for y, row in enumerate(ICON_AIRLOCK):
        for x, ch in enumerate(row):
            if ch in pal and not (ch == '.' and (x < 2 or x > 13)):
                cv[x, y] = pal[ch]
    return cv


def export_blocks(save):
    metal = (150, 230, 0, None)
    paint = (110, 0, 0, None)
    save(casing(), 'block', 'life_support_casing', paint, wrap=False)
    save(plinth(), 'block', 'life_support_plinth', metal, wrap=False)
    save(steel_top(), 'block', 'life_support_top', metal, wrap=False)
    save(ring_tex(), 'block', 'life_support_ring', (180, 232, 0, None), wrap=False)
    save(filler_front(), 'block', 'oxygen_filler_front', paint, wrap=False)
    save(filler_front(True), 'block', 'oxygen_filler_front_on', paint, wrap=False)
    save(filler_top(), 'block', 'oxygen_filler_top', metal, wrap=False)
    save(distributor_front(), 'block', 'oxygen_distributor_front', paint, wrap=False)
    save(distributor_front(True), 'block', 'oxygen_distributor_front_on', paint, wrap=False)
    save(distributor_top(), 'block', 'oxygen_distributor_top', metal, wrap=False)
    save(station_front(), 'block', 'suit_station_front', paint, wrap=False)
    save(station_front(True), 'block', 'suit_station_front_on', paint, wrap=False)
    save(station_top(), 'block', 'suit_station_top', metal, wrap=False)
    save(airlock_frame(), 'block', 'airlock_door_frame', metal, wrap=False)
    save(airlock_panel(), 'block', 'airlock_door_panel', paint, wrap=False)
    save(airlock_panel(True), 'block', 'airlock_door_panel_window', paint, wrap=False)
    save(icon_airlock(), 'item', 'airlock_door')
