"""Off-world industry (ISRU) machines: atmosphere processor, Sabatier reactor, regolith heater, ice melter.
Same casing language as the Earth machines (modules/machines_industry.py)."""
from sbtex.core import Canvas
from sbtex import palettes as P
from modules.machines_industry import S, GLOW, casing, recess, rect, glow_overlay

NI = P.NICKEL


def atmo_front():
    cv = casing(41, seam=False)
    for y in range(3, 12, 2):                    # intake louvres
        for x in range(3, 13):
            cv[x, y] = S[0]
            cv[x, y + 1] = S[5] if y + 1 < 12 else cv[x, y + 1]
    rect(cv, 6, 13, 9, 13, S[5])
    return cv


def atmo_side():
    """Two filter cartridges behind a frame."""
    cv = casing(42, seam=False)
    for x0 in (3, 9):
        recess(cv, x0, 3, x0 + 3, 12, S[3])
        for y in range(3, 13, 2):
            for x in range(x0, x0 + 4):
                cv[x, y] = S[2]
    return cv


def atmo_top():
    cv = casing(43, rivets=False, seam=False)
    for y in range(2, 14):
        for x in range(2, 14):
            d = ((x - 7.5) ** 2 + (y - 7.5) ** 2) ** 0.5
            if d < 5.8:
                cv[x, y] = S[0] if int(d * 1.3) % 2 == 0 else S[3]
    rect(cv, 7, 7, 8, 8, S[5])
    return cv


def nickel_tube():
    """Catalyst bed: nickel-clad vessel with weld bands."""
    cv = Canvas()
    for y in range(16):
        for x in range(16):
            cv[x, y] = NI[[5, 4, 4, 3, 3, 2, 3, 4][y // 2]]
    for y in range(16):
        cv[3, y] = NI[2]
        cv[12, y] = NI[2]
    return cv


def sabatier_front():
    cv = casing(44, seam=False)
    recess(cv, 4, 4, 11, 9, S[0])                # sight glass
    rect(cv, 4, 12, 5, 12, (160, 40, 30))
    rect(cv, 10, 12, 11, 12, (160, 40, 30))
    return cv


def sabatier_glow():
    px = []
    for y in range(4, 10):
        for x in range(4, 12):
            px.append((x, y, GLOW[0] if y > 7 else (GLOW[1] if y > 5 else GLOW[2])))
    return glow_overlay(px)


def heater_front():
    cv = casing(45, seam=False)
    brick = (120, 52, 38)
    for y in range(3, 13):
        for x in range(3, 13):
            cv[x, y] = S[2] if (y % 3 == 2 or (x + (4 if (y // 3) % 2 else 0)) % 8 == 0) else brick
    rect(cv, 5, 8, 10, 9, S[0])                  # crucible slit
    return cv


def heater_glow():
    px = []
    for x in range(5, 11):
        px.append((x, 8, GLOW[3] if 6 < x < 9 else GLOW[2]))
        px.append((x, 9, GLOW[1]))
    return glow_overlay(px)


def melter_tank():
    """Glass tank with meltwater and floating ice."""
    water = [(38, 78, 140), (52, 98, 165)]
    ice = (200, 222, 240)
    cv = Canvas()
    for y in range(16):
        for x in range(16):
            c = water[1] if 2 < x < 13 else water[0]
            if x in (0, 15):
                c = tuple(S[3])
            elif y < 3:
                c = (150, 190, 215)
            cv[x, y] = c
    for x, y in ((4, 3), (5, 3), (5, 4), (9, 3), (10, 3), (11, 3), (10, 4), (7, 6), (8, 6)):
        cv[x, y] = ice
    return cv


def coil_glow():
    return glow_overlay([(x, y, GLOW[0]) for y in (13, 14) for x in range(2, 14) if (x + y) % 2 == 0])


def export(save):
    metal = (150, 230, 0, None)
    glow_rule = lambda c: (60, 10, 0, 200) if c[3] else None
    save(atmo_front(), 'block', 'atmosphere_processor_front', metal)
    save(atmo_side(), 'block', 'atmosphere_processor_side', metal)
    save(atmo_top(), 'block', 'atmosphere_processor_top', metal)
    save(nickel_tube(), 'block', 'sabatier_reactor_tube', (190, 230, 0, None))
    save(sabatier_front(), 'block', 'sabatier_reactor_front', metal)
    save(sabatier_glow(), 'block', 'sabatier_reactor_glow', (60, 10, 0, glow_rule), normal=False)
    save(heater_front(), 'block', 'regolith_heater_front', metal)
    save(heater_glow(), 'block', 'regolith_heater_glow', (60, 10, 0, glow_rule), normal=False)
    save(melter_tank(), 'block', 'ice_melter_tank', (220, 40, 0, None), normal=False)
    save(coil_glow(), 'block', 'ice_melter_glow', (60, 10, 0, glow_rule), normal=False)


ENAMEL = [(16, 36, 74), (24, 52, 102), (34, 70, 132), (48, 92, 160), (72, 118, 186)]


def reactor_vessel():
    """Glass-lined (enamel blue) reaction vessel with a steel band and a nozzle."""
    cv = Canvas()
    for y in range(16):
        for x in range(16):
            cv[x, y] = ENAMEL[[3, 4, 4, 3, 3, 2, 2, 1][x // 2]]
    for x in range(16):
        cv[x, 4] = S[3]
        cv[x, 5] = S[5]
    rect(cv, 6, 8, 9, 11, S[3])
    rect(cv, 7, 9, 8, 10, S[5])
    return cv


def reactor_front():
    cv = casing(46, seam=False)
    # the front shows only rows 7..15 (the body is 9 px tall)
    recess(cv, 3, 9, 6, 12, S[5])                # pressure gauge
    cv[5, 10] = S[0]
    for i in range(3):                           # hazard diamond
        for j in range(-i, i + 1):
            cv[11 + j, 9 + i] = (214, 170, 44)
            cv[11 + j, 13 - i] = (214, 170, 44)
    cv[11, 11] = S[0]
    return cv


_export_base = export


def export(save):  # noqa: F811
    _export_base(save)
    metal = (150, 230, 0, None)
    save(reactor_vessel(), 'block', 'chemical_reactor_vessel', (210, 40, 0, None))
    save(reactor_front(), 'block', 'chemical_reactor_front', metal)
