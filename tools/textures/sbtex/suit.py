"""EVA suit model textures (UV sheets matching client/model/EvaHelmetModel.java).

Box UV layout (Minecraft ModelPart.Cube), box at (u, v) with size (w, h, d):
  top    (u+d,       v,     w, d)   row 0 = back, last row = front
  bottom (u+d+w,     v,     w, d)
  west   (u,         v+d,   d, h)   (-X side)
  north  (u+d,       v+d,   w, h)   front / face side, row 0 = top
  east   (u+d+w,     v+d,   d, h)   (+X side)
  south  (u+2d+w,    v+d,   w, h)   back
"""
import random

from .core import Canvas, TRANSPARENT
from . import palettes as P

# Must match EvaHelmetModel.java ------------------------------------------------------------
HELMET_TEX = (128, 64)
HELMET_BOXES = {
    #  name        u   v    w   h   d
    'shell_z': (0, 0, 8, 9, 10),
    'shell_x': (0, 19, 10, 9, 8),
    'cap': (0, 36, 8, 1, 8),
    'rounder': (0, 45, 9, 9, 9),
    'liner': (60, 20, 7, 7, 7),
    'ring': (40, 0, 11, 2, 11),
    'lamp_l': (40, 13, 1, 2, 3),
    'lamp_r': (48, 13, 1, 2, 3),
}
VISOR_TEX = (64, 32)
VISOR_BOXES = {
    'glass': (0, 0, 6, 6, 0),
    'gold': (0, 8, 7, 7, 0),
    'lens': (0, 16, 1, 1, 0),
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


def _fill(cv, rect, fn):
    x0, y0, w, h = rect
    for y in range(h):
        for x in range(w):
            c = fn(x, y, w, h)
            if c is not None:
                cv[x0 + x, y0 + y] = c


def eva_helmet() -> Canvas:
    W, S, A, L = P.SUIT_WHITE, P.STEEL, P.ANODIZED_BLUE, P.LAMP
    cv = Canvas(*HELMET_TEX)
    rng = random.Random(77)

    def shell_tone(y, h):
        """Composite shell: a soft vertical gradient fakes the curvature of the dome."""
        t = y / max(1, h - 1)
        base = 7 if t < 0.2 else (6 if t < 0.65 else 5)
        if rng.random() < 0.12:
            base -= 1
        return W[base]

    def shell(x, y, w, h):
        return shell_tone(y, h)

    def edges_only(axis):
        def fn(x, y, w, h):
            edge = (x == 0 or x == w - 1) if axis == 'x' else (y == 0 or y == h - 1)
            return shell(x, y, w, h) if edge else TRANSPARENT
        return fn

    # --- shell_z: front/back slab (the face plate) -------------------------------------
    f = faces(*HELMET_BOXES['shell_z'])

    def front(x, y, w, h):
        if 1 <= x <= 6 and 1 <= y <= 6:
            return TRANSPARENT                       # visor opening
        if (1 <= x <= 6 and y in (0, 7)) or (x in (0, 7) and 1 <= y <= 6):
            top_or_left = y == 0 or x == 0
            return S[6] if top_or_left else S[3]     # machined visor rim
        if y == 8:
            return W[3] if x in (2, 5) else W[5]     # chin vents
        return shell(x, y, w, h)

    def back(x, y, w, h):
        if y in (4, 5) and 2 <= x <= 5:
            return S[2] if x % 2 == 0 else S[4]      # purge valve grille
        if y == 2 and 3 <= x <= 4:
            return A[4]                               # small blue ID tab
        return shell(x, y, w, h)

    _fill(cv, f['north'], front)
    _fill(cv, f['south'], back)
    _fill(cv, f['west'], edges_only('x'))
    _fill(cv, f['east'], edges_only('x'))
    _fill(cv, f['top'], edges_only('y'))
    _fill(cv, f['bottom'], lambda *a: TRANSPARENT)

    # --- shell_x: side slab ---------------------------------------------------------------
    f = faces(*HELMET_BOXES['shell_x'])

    def side(x, y, w, h):
        if x in (1, w - 2) and 2 <= y <= 6:
            return W[5]                               # seam where visor assembly meets the shell
        if 3 <= x <= 4 and 3 <= y <= 4:
            return S[7] if (x, y) == (3, 3) else S[5]  # visor hinge boss
        return shell(x, y, w, h)

    _fill(cv, f['west'], side)
    _fill(cv, f['east'], side)
    _fill(cv, f['north'], edges_only('x'))
    _fill(cv, f['south'], edges_only('x'))
    _fill(cv, f['top'], edges_only('x'))
    _fill(cv, f['bottom'], lambda *a: TRANSPARENT)

    # --- cap and corner rounder -------------------------------------------------------------
    f = faces(*HELMET_BOXES['cap'])
    _fill(cv, f['top'], lambda x, y, w, h: W[7] if rng.random() > 0.1 else W[6])
    for k in ('west', 'north', 'east', 'south'):
        _fill(cv, f[k], lambda x, y, w, h: W[7])
    _fill(cv, f['bottom'], lambda *a: TRANSPARENT)

    f = faces(*HELMET_BOXES['rounder'])
    _fill(cv, f['top'], lambda x, y, w, h: W[7])
    for k in ('west', 'north', 'east', 'south'):
        _fill(cv, f[k], edges_only('x'))
    _fill(cv, f['bottom'], lambda *a: TRANSPARENT)

    # --- neck ring: anodised blue with bolts -------------------------------------------------
    f = faces(*HELMET_BOXES['ring'])
    _fill(cv, f['top'], lambda x, y, w, h: A[5] if (x in (0, w - 1) or y in (0, h - 1)) else A[4])
    _fill(cv, f['bottom'], lambda x, y, w, h: A[1])
    for k in ('west', 'north', 'east', 'south'):
        _fill(cv, f[k], lambda x, y, w, h: (S[7] if (y == 0 and x % 3 == 1) else (A[5] if y == 0 else A[3])))

    # --- lamps -------------------------------------------------------------------------------
    for name in ('lamp_l', 'lamp_r'):
        f = faces(*HELMET_BOXES[name])
        for k in ('top', 'bottom', 'west', 'east', 'south'):
            _fill(cv, f[k], lambda x, y, w, h: W[6] if y == 0 else W[4])
        _fill(cv, f['north'], lambda x, y, w, h: L[3])

    # --- padded liner (only visible on armour stands) ------------------------------------
    R = P.RUBBER
    f = faces(*HELMET_BOXES['liner'])
    for k in ('top', 'bottom', 'west', 'north', 'east', 'south'):
        _fill(cv, f[k], lambda x, y, w, h: R[2] if (x + y) % 3 else R[1])
    return cv


def eva_helmet_visor() -> Canvas:
    """Translucent visor glass, gold sun visor and lamp lenses (rendered in a separate layer)."""
    V, G, L = P.VISOR_GLASS, P.GOLD, P.LAMP
    cv = Canvas(*VISOR_TEX)
    gx, gy, gw, gh, _ = VISOR_BOXES['glass']

    def glass(x, y):
        # dark smoked glass; brighter streak across the upper left; faint lower reflection
        if (x + y) in (2, 3) and y <= 3:
            return (*V[7], 200) if (x + y) == 2 else (*V[6], 160)
        if y == gh - 1:
            return (*V[4], 140)
        return (*V[2 if y < 2 else 3], 105)

    for face_x in (gx, gx + gw):  # north + south faces
        for y in range(gh):
            for x in range(gw):
                cv[face_x + x, gy + y] = glass(x, y)

    ox, oy, ow, oh, _ = VISOR_BOXES['gold']
    # mirror-gold: bright sky in the upper half, a crisp horizon line, darker ground reflection below
    rows = [G[6], G[6], G[7], G[5], G[4], G[3], G[2]]
    for face_x in (ox, ox + ow):
        for y in range(oh):
            for x in range(ow):
                c = rows[y]
                if x == 0 and y < 5:
                    c = G[min(7, G.index(c) + 1)]
                if (x, y) in ((1, 0), (2, 1)):
                    c = G[7]
                cv[face_x + x, oy + y] = c

    lx, ly, _, _, _ = VISOR_BOXES['lens']
    cv[lx, ly] = L[4]
    cv[lx + 1, ly] = L[4]
    return cv
