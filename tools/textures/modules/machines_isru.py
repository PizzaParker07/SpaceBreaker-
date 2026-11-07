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


# ---------------------------------------------------------------- RTG and its fuel pellet
AL = P.ALUMINIUM
W = P.TUNGSTEN
TREFOIL_Y = (222, 182, 48)


def rtg_fins():
    """Radiator fins all round, a tungsten band where the thermocouples sit."""
    cv = Canvas()
    for y in range(16):
        for x in range(16):
            cv[x, y] = (AL[5], AL[3], AL[3], AL[1])[x % 4]
    for x in range(16):
        cv[x, 0] = AL[1]
        cv[x, 15] = AL[1]
        cv[x, 7] = W[4]
        cv[x, 8] = W[2]
    return cv


def rtg_front():
    """Fins with a radiation warning plate."""
    import math
    cv = rtg_fins()
    rect(cv, 4, 3, 11, 12, TREFOIL_Y)
    cx, cy = 7.5, 7.5
    for y in range(4, 12):
        for x in range(5, 11):
            dx, dy = x - cx, y - cy
            d = (dx * dx + dy * dy) ** 0.5
            ang = math.degrees(math.atan2(-dy, dx)) % 360
            blade = any(abs((ang - a + 180) % 360 - 180) < 30 for a in (90, 210, 330))
            if d < 0.9 or (1.6 < d < 3.3 and blade):
                cv[x, y] = W[2]
    return cv


def rtg_top():
    """Bolted cap over the heat-source cavity."""
    cv = Canvas()
    for y in range(16):
        for x in range(16):
            d = ((x - 7.5) ** 2 + (y - 7.5) ** 2) ** 0.5
            cv[x, y] = W[4] if d < 5.2 else (AL[1] if d < 6.3 else (AL[3] if (x + y) % 4 else AL[5]))
    for x, y in ((7, 3), (11, 5), (11, 10), (7, 12), (3, 10), (3, 5)):
        cv[x, y] = AL[5]
        cv[x + 1, y] = W[2]
    rect(cv, 6, 6, 9, 9, W[2])
    rect(cv, 7, 7, 8, 8, AL[5])
    return cv


def rtg_pellet():
    """Clad fuel capsule; the end face glows with decay heat."""
    cv = Canvas()
    shade = [W[2], W[4], W[5], W[6], W[5], W[4], W[3], W[1]]
    for y in range(5, 14):
        for i, x in enumerate(range(4, 12)):
            cv[x, y] = shade[i]
    for x in range(4, 12):
        cv[x, 14] = W[1]
    for x in range(5, 11):
        cv[x, 3] = W[3]
    cv[4, 4] = W[3]
    cv[11, 4] = W[2]
    for x in range(5, 11):
        cv[x, 4] = GLOW[1] if 6 <= x <= 9 else GLOW[0]
    cv[7, 4] = GLOW[2]
    cv[8, 4] = GLOW[2]
    for x in range(5, 11):
        cv[x, 9] = W[1] if x in (5, 10) else W[3]
    return cv


_export_isru = export


def export(save):  # noqa: F811
    _export_isru(save)
    metal = (150, 230, 0, None)
    save(rtg_fins(), 'block', 'rtg_side', (190, 230, 0, None))
    save(rtg_front(), 'block', 'rtg_front', metal)
    save(rtg_top(), 'block', 'rtg_top', metal)
    save(rtg_pellet(), 'item', 'rtg_fuel_pellet')


def excavator_front():
    """A bucket wheel behind a steel frame: dark buckets on a tungsten rim, hazard stripe at the bottom."""
    import math
    cv = casing(71, seam=False)
    for y in range(2, 13):
        for x in range(2, 14):
            d = math.hypot(x - 7.5, y - 7.0)
            if d < 5.6:
                ang = math.atan2(y - 7.0, x - 7.5)
                bucket = int((ang + math.pi) / (2 * math.pi) * 8) % 2 == 0
                cv[x, y] = S[2] if d > 3.8 and bucket else (S[3] if d > 3.8 else (S[2] if d > 1.2 else S[5]))
    for x in range(1, 15):
        cv[x, 14] = TREFOIL_Y if (x // 2) % 2 == 0 else S[2]
    return cv


_export_excavator = export


def export(save):  # noqa: F811
    _export_excavator(save)
    save(excavator_front(), 'block', 'regolith_excavator_front', (150, 230, 0, None))


def sinter_front():
    """Kiln door: a brick-lined arch with a dark firing chamber (the glow layer fills it when hot)."""
    cv = casing(81, seam=False)
    brick = [(132, 72, 51), (132, 72, 51)]
    for y in range(3, 13):
        for x in range(3, 13):
            if y == 3 and x in (3, 12):
                continue
            cv[x, y] = brick[(x // 2 + y) % 2] if (x in (3, 12) or y in (3, 12)) else S[0]
    return cv


def sinter_glow():
    return glow_overlay([(x, y, GLOW[(x + y) % 3]) for y in range(4, 12) for x in range(4, 12) if not (y < 6 and x in (4, 11))])


_export_sinter = export


def export(save):  # noqa: F811
    _export_sinter(save)
    save(sinter_front(), 'block', 'sintering_furnace_front', (150, 230, 0, None))
    save(sinter_glow(), 'block', 'sintering_furnace_glow', (60, 10, 0, lambda c: (60, 10, 0, 200) if c[3] else None), normal=False)


def nuclear_fuel_element():
    """A NERVA-style hexagonal fuel element lying diagonally: tungsten-clad rod with spacer rings, its top end face
    pierced by the hydrogen channels that run through the uranium carbide."""
    from sbtex import palettes as PP
    T = PP.TUNGSTEN
    cv = Canvas()
    # rod axis x + y = 15, from lower left to upper right; across = x + y - 15, along = x - y
    shade = {-2: T[1], -1: T[3], 0: T[5], 1: T[6], 2: T[2]}
    for y in range(16):
        for x in range(16):
            across = x + y - 15
            along = x - y
            if -2 <= across <= 2 and -11 <= along <= 7:
                c = shade[across]
                if along in (-6, 1) and -1 <= across <= 1:
                    c = T[2]        # spacer ring
                cv[x, y] = c
    # end face (upper right): clad rim round dark coolant channels
    for y in range(16):
        for x in range(16):
            across = x + y - 15
            along = x - y
            if along in (8, 9) and -2 <= across <= 2:
                cv[x, y] = T[0] if (across + along) % 2 == 0 and abs(across) <= 1 else T[7]
    return cv


_export_isru_fuel = export


def export(save):  # noqa: F811
    _export_isru_fuel(save)
    save(nuclear_fuel_element(), 'item', 'nuclear_fuel_element')


def centrifuge_front():
    """Control panel of a centrifuge cascade: a small rpm readout and a row of stage lamps."""
    cv = casing(91, seam=False)
    # the casing is only 6 px tall in the model: everything sits in the bottom rows (10..15)
    for y in range(10, 13):
        for x in range(3, 9):
            cv[x, y] = S[0] if (x in (3, 8) or y in (10, 12)) else (60, 120, 80)
    for x in (10, 12):
        cv[x, 11] = (60, 120, 80)
        cv[x + 1, 11] = S[0]
    return cv


def centrifuge_tube():
    """Tall aluminium rotor casing: rounded shading across, a flange band near each end."""
    from sbtex import palettes as PP
    A = PP.ALUMINIUM
    cv = Canvas()
    across = [A[2], A[4], A[5], A[6], A[5], A[4], A[3], A[2]]
    for y in range(16):
        for x in range(16):
            cv[x, y] = across[x % 8]
            if y in (1, 14):
                cv[x, y] = A[1] if x % 8 in (0, 7) else A[3]
    return cv


def yellowcake():
    """Uranium oxide concentrate: a heap of yellow-orange powder."""
    from modules.industry_shapes import dust
    cake = [(98, 70, 18), (128, 94, 24), (160, 120, 30), (190, 148, 40), (214, 172, 52), (232, 196, 80), (244, 216, 120), (250, 232, 160)]
    return dust(cake, seed=17)


def enriched_uranium():
    """Enriched uranium: a dark metal ingot with a faint green sheen."""
    from sbtex.shapes import ingot
    green = [(24, 32, 24), (36, 50, 34), (50, 70, 46), (66, 92, 58), (84, 116, 70), (104, 140, 84), (130, 166, 104), (168, 196, 140)]
    return ingot(green)


def depleted_uranium():
    """Depleted uranium: a dull, very dense grey ingot."""
    from sbtex.shapes import ingot
    from sbtex import palettes as PP
    return ingot(PP.URANIUM, glint=False)


_export_isru_centrifuge = export


def export(save):  # noqa: F811
    _export_isru_centrifuge(save)
    save(centrifuge_front(), 'block', 'gas_centrifuge_front', (150, 230, 0, None))
    save(centrifuge_tube(), 'block', 'gas_centrifuge_tube', (170, 232, 0, None))
    save(yellowcake(), 'item', 'yellowcake')
    save(enriched_uranium(), 'item', 'enriched_uranium')
    save(depleted_uranium(), 'item', 'depleted_uranium')
    from sbtex.shapes import plate
    from sbtex import palettes as PP
    save(plate(PP.URANIUM, seed=23), 'item', 'depleted_uranium_plate')
