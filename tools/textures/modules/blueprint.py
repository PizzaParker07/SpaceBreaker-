"""Blueprint area: Hologram Projector icon and in-hand model sheet."""
from sbtex.core import Canvas
from sbtex import palettes as P

S, W, A, C, R = P.STEEL, P.SUIT_WHITE, P.ANODIZED_BLUE, P.OXYGEN_CYAN, P.RUBBER

# A hand-held emitter projecting a small wireframe cube (vanilla item style: dark outline, one highlight).
ICON = [
    "................",
    "................",
    "................",
    "................",
    "................",
    "................",
    "................",
    "................",
    "................",
    "......l..l......",
    "....kkkkkkkk....",
    "...kHWWWWWwgk...",
    "...kWbLLLbwgk...",
    "...kkkkkkkkkk...",
    "......kRRk......",
    "......kkkk......",
]


def _line(cv, x0, y0, x1, y1, c):
    n = max(abs(x1 - x0), abs(y1 - y0))
    for i in range(n + 1):
        cv[round(x0 + (x1 - x0) * i / n), round(y0 + (y1 - y0) * i / n)] = c


def icon() -> Canvas:
    pal = {'l': C[4], 'k': (30, 32, 36), 'H': W[7], 'W': W[6], 'w': W[4], 'g': W[4], 'b': A[5], 'L': C[7], 'R': R[3]}
    cv = Canvas()
    for y, row in enumerate(ICON):
        for x, ch in enumerate(row):
            if ch in pal:
                cv[x, y] = pal[ch]
    # wireframe cube floating above the emitter: back square dim, front square bright, corners joined
    back, front = (6, 1, 12, 6), (3, 3, 9, 8)
    for (x0, y0, x1, y1), c in ((back, C[4]), (front, C[6])):
        _line(cv, x0, y0, x1, y0, c)
        _line(cv, x0, y1, x1, y1, c)
        _line(cv, x0, y0, x0, y1, c)
        _line(cv, x1, y0, x1, y1, c)
    for (ax, ay), (bx, by) in zip(((3, 3), (9, 3), (9, 8)), ((6, 1), (12, 1), (12, 6))):
        _line(cv, ax, ay, bx, by, C[6])
    return cv


def model_sheet() -> Canvas:
    """UV sheet of the 3D model. Regions:
    (0,0)-(4,5) grip, (4,0)-(10,4) body side, (4,4)-(10,10) body top, (10,0)-(14,1) ring side,
    (10,1)-(14,5) ring top, (14,0)-(16,2) lens, (0,6)-(2,8) fins."""
    cv = Canvas()
    for y in range(5):
        for x in range(4):
            cv[x, y] = R[3] if (x == 0 or y % 2 == 0) else R[2]
    for y in range(4):
        for x in range(4, 10):
            if y == 0:
                cv[x, y] = W[7]
            elif y == 3:
                cv[x, y] = S[4]
            elif x == 5 and y == 1:
                cv[x, y] = C[6]                         # status light
            else:
                cv[x, y] = W[6] if x > 4 else W[7]
    for y in range(4, 10):
        for x in range(4, 10):
            edge = x in (4, 9) or y in (4, 9)
            cv[x, y] = W[7] if edge else W[6]
    for x in range(10, 14):
        cv[x, 0] = A[5]
    for y in range(1, 5):
        for x in range(10, 14):
            cv[x, y] = A[5] if (x in (10, 13) or y in (1, 4)) else A[4]
    cv[14, 0] = C[7]
    cv[15, 0] = C[6]
    cv[14, 1] = C[6]
    cv[15, 1] = C[6]
    for y in range(6, 8):
        for x in range(0, 2):
            cv[x, y] = W[7] if y == 6 else S[4]
    return cv


def export(save):
    save(icon(), 'item', 'hologram_projector')
    save(model_sheet(), 'item', 'hologram_projector_model')
