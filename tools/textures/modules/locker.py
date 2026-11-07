"""Supply locker: a shared dungeon storage cabinet set into station walls. Brushed aluminium, a recessed door with a
latch and a stencilled orange band; open, it shows two shelves in a dark interior."""
from sbtex.core import Canvas, hexc
from sbtex import palettes as P

A = P.ALUMINIUM
BAND = hexc('#c8642a')
DARK = hexc('#1d2026')


def _body(fill=A[4]):
    cv = Canvas(16, 16, fill)
    for i in range(16):
        cv[i, 0] = A[5]
        cv[0, i] = A[5]
        cv[i, 15] = A[2]
        cv[15, i] = A[2]
    return cv


def side():
    cv = _body()
    for y in (5, 10):
        for x in range(1, 15):
            cv[x, y] = A[3]  # panel seams
    return cv


def top():
    cv = _body(A[3])
    for x in (4, 11):
        for y in (4, 11):
            cv[x, y] = A[5]  # bolts
    return cv


def front():
    cv = _body()
    # recessed door: a darker frame, the door leaf, a seam down the middle of the double door
    for i in range(2, 14):
        cv[i, 2] = A[2]
        cv[2, i] = A[2]
        cv[i, 13] = A[5]
        cv[13, i] = A[5]
    for x in range(3, 13):
        for y in range(3, 13):
            cv[x, y] = A[4]
    for y in range(3, 13):
        cv[8, y] = A[2]
    for x in range(3, 13):
        cv[x, 5] = BAND  # stencilled band
    for y in (8, 9):
        cv[7, y] = A[5]  # latch handles
        cv[9, y] = A[5]
    return cv


def front_open():
    cv = _body()
    for x in range(2, 14):
        for y in range(2, 14):
            cv[x, y] = DARK
    for x in range(2, 14):
        cv[x, 7] = A[3]  # shelves
        cv[x, 12] = A[3]
    for i in range(2, 14):
        cv[i, 2] = A[2]
        cv[2, i] = A[2]
        cv[13, i] = A[2]
    cv[5, 6] = BAND  # a crate on the shelf
    cv[6, 6] = BAND
    cv[10, 11] = A[5]
    return cv


def export(save):
    metal = (150, 230, 0, None)
    save(front(), 'block', 'supply_locker_front', metal)
    save(front_open(), 'block', 'supply_locker_front_open', metal)
    save(side(), 'block', 'supply_locker_side', metal)
    save(top(), 'block', 'supply_locker_top', metal)
