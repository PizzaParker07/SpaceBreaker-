"""Industry machines: shared casing, per-machine fronts (off/on + emissive glow overlays), parts, tanks,
cables and pipes. Palettes stay small (casing steel + one accent) so machines sit next to vanilla blocks."""
import random

from sbtex.core import Canvas, clustered, pick
from sbtex import palettes as P

S = P.CARBON_STEEL
GLOW = [(255, 120, 30), (255, 170, 60), (255, 220, 120), (255, 245, 200)]
HAZ_Y = (214, 170, 44)
HAZ_K = tuple(S[0])


def casing(seed=1, rivets=True, seam=True) -> Canvas:
    """Riveted steel panel: dark frame, slightly mottled plate, light top edge."""
    cv = Canvas()
    n = clustered(16, 16, seed, passes=2)
    for y in range(16):
        for x in range(16):
            cv[x, y] = S[5] if n[y][x] > 0.82 else S[4]
    for i in range(16):
        cv[i, 0] = S[3]
        cv[i, 15] = S[2]
        cv[0, i] = S[3]
        cv[15, i] = S[2]
        cv[i, 1] = S[5] if 0 < i < 15 else cv[i, 1]
        cv[1, i] = S[5] if 0 < i < 15 else cv[1, i]
        cv[14, i] = S[3] if 0 < i < 15 else cv[14, i]
        cv[i, 14] = S[3] if 0 < i < 15 else cv[i, 14]
    if seam:
        for x in range(2, 14):
            cv[x, 8] = S[3]
    if rivets:
        for x, y in ((2, 3), (13, 3), (2, 12), (13, 12)):
            cv[x, y] = S[5]
            cv[x + 0, y + 1] = S[2]
    return cv


def top(seed=2, vent=True) -> Canvas:
    cv = casing(seed, rivets=True, seam=False)
    if vent:
        for y in (5, 7, 9, 11):
            for x in range(4, 12):
                cv[x, y] = S[0]
                cv[x, y - 1] = S[3] if y > 5 else cv[x, y - 1]
    return cv


def frame_only(cv):
    for i in range(16):
        cv[i, 0] = S[2]
        cv[i, 15] = S[1]
        cv[0, i] = S[2]
        cv[15, i] = S[1]
    return cv


def rect(cv, x0, y0, x1, y1, c):
    for y in range(y0, y1 + 1):
        for x in range(x0, x1 + 1):
            cv[x, y] = c


def recess(cv, x0, y0, x1, y1, fill):
    """Recessed opening: shadow on top/left, light lip bottom/right."""
    rect(cv, x0, y0, x1, y1, fill)
    for x in range(x0 - 1, x1 + 2):
        cv[x, y0 - 1] = S[2]
        cv[x, y1 + 1] = S[5]
    for y in range(y0 - 1, y1 + 2):
        cv[x0 - 1, y] = S[2]
        cv[x1 + 1, y] = S[5]


def hazard(cv, y0, y1):
    for y in range(y0, y1 + 1):
        for x in range(1, 15):
            cv[x, y] = HAZ_Y if ((x + y) // 2) % 2 == 0 else HAZ_K


def glow_overlay(pixels) -> Canvas:
    cv = Canvas()
    for x, y, c in pixels:
        cv[x, y] = c
    return cv


# ----------------------------------------------------------------------------- machines

def generator_front(lit=False):
    cv = casing(11, seam=False)
    recess(cv, 3, 4, 12, 11, S[0])
    for x in range(3, 13):                       # grate bars
        if x % 2 == 0:
            for y in range(4, 12):
                cv[x, y] = S[3]
    for x in range(3, 13):
        cv[x, 7] = S[2]
    rect(cv, 6, 13, 9, 13, S[5])                 # door handle
    return cv


def generator_glow():
    px = []
    for x in range(3, 13):
        if x % 2 == 1:
            for y in range(4, 12):
                if y != 7:
                    px.append((x, y, GLOW[0] if y < 7 else (GLOW[1] if y < 10 else GLOW[2])))
    return glow_overlay(px)


def crusher_front():
    cv = casing(12, seam=False)
    recess(cv, 3, 3, 12, 9, S[0])
    for x in range(3, 13):                       # jaw teeth
        cv[x, 3 + (x % 2)] = S[3]
        cv[x, 9 - (x % 2)] = S[3]
    hazard(cv, 12, 13)
    return cv


def crusher_top():
    cv = casing(13, rivets=False, seam=False)
    recess(cv, 3, 3, 12, 12, S[0])
    for i in range(3, 13):
        cv[i, 7] = S[2]
        cv[7, i] = S[2]
    return cv


def arc_front():
    cv = casing(14, seam=False)
    brick = [(120, 52, 38), (150, 70, 50)]
    for y in range(3, 13):                       # refractory brick door
        for x in range(3, 13):
            mortar = y % 3 == 2 or (x + (4 if (y // 3) % 2 else 0)) % 8 == 0
            cv[x, y] = S[2] if mortar else brick[1 if y % 3 == 0 else 0]
    for x in range(2, 14):
        cv[x, 2] = S[5]
        cv[x, 13] = S[2]
    rect(cv, 7, 6, 8, 7, S[2])                   # peephole
    return cv


def arc_glow():
    return glow_overlay([(7, 6, GLOW[3]), (8, 6, GLOW[2]), (7, 7, GLOW[2]), (8, 7, GLOW[1])])


def arc_top():
    cv = casing(15, rivets=False, seam=False)
    for cx in (4, 8, 12):
        recess(cv, cx - 1, 7, cx, 8, S[0])
    return cv


def electrode():
    g = [(30, 30, 33), (44, 44, 48), (60, 60, 65), (78, 78, 84)]
    cv = Canvas()
    for y in range(16):
        for x in range(16):
            cv[x, y] = g[[3, 2, 2, 1][x % 4]]
    for x in range(16):
        cv[x, 0] = P.COPPER[5]
        cv[x, 1] = P.COPPER[3]
    return cv


def press_front():
    cv = casing(16, seam=False)
    hazard(cv, 13, 14)
    return cv


def press_parts():
    """Press base/crown/columns/ram: polished steel."""
    cv = Canvas()
    for y in range(16):
        for x in range(16):
            cv[x, y] = S[[5, 4, 4, 3][y % 4]]
    for x in range(16):
        cv[x, 0] = S[6]
    return cv


def press_die():
    cv = Canvas()
    for y in range(16):
        for x in range(16):
            cv[x, y] = S[2]
    rect(cv, 3, 3, 12, 12, S[1])
    rect(cv, 4, 4, 11, 11, S[3])
    return cv


def fabricator_front():
    cv = casing(17, seam=False)
    recess(cv, 3, 3, 12, 8, (18, 32, 28))         # screen
    for x in range(4, 12):
        cv[x, 11] = S[2]
    rect(cv, 4, 11, 5, 12, (150, 40, 30))        # buttons
    rect(cv, 10, 11, 11, 12, (150, 40, 30))
    return cv


def fabricator_glow():
    px = []
    for y in range(3, 9):
        for x in range(3, 13):
            if (y == 4 and 4 <= x <= 9) or (y == 6 and 4 <= x <= 11 and x % 3 != 0) or (y == 8 and 4 <= x <= 7):
                px.append((x, y, (110, 230, 170)))
            elif y in (3, 5, 7):
                px.append((x, y, (30, 90, 70)))
    return glow_overlay(px)


def fabricator_top():
    cv = casing(18, rivets=True, seam=False)
    for y in range(3, 13):
        for x in range(3, 13):
            cv[x, y] = S[3] if (x + y) % 4 else S[2]
    return cv


def electrolyzer_cell(lit=False):
    """Glass cell with water; when lit the bubbles show (animated strip of 4 frames)."""
    frames = 4 if lit else 1
    cv = Canvas(16, 16 * frames)
    water = [(38, 78, 140), (52, 98, 165), (70, 122, 186)]
    for f in range(frames):
        for y in range(16):
            for x in range(16):
                c = water[1] if 2 < x < 13 else water[0]
                if x in (0, 15):
                    c = S[3]
                elif x in (1, 14):
                    c = (150, 190, 215)
                elif y < 2:
                    c = (120, 160, 200)
                cv[x, f * 16 + y] = c
            cv[4, f * 16 + y] = water[2]
        if lit:
            rng = random.Random(f)
            for k in range(7):
                bx = 3 + (k * 5) % 10
                by = (15 - ((f * 4 + k * 3) % 14))
                cv[bx, f * 16 + by] = (210, 230, 245)
    return cv


def liquefier_front():
    cv = casing(19, seam=False)
    frost = [(214, 226, 236), (214, 226, 236)]
    for x in range(1, 15):                       # rime along the cold bottom edge, a few icicle drips above
        cv[x, 14] = frost[1]
        cv[x, 13] = frost[0] if x % 3 else frost[1]
        if x % 4 == 1:
            cv[x, 12] = frost[0]
    for x in (3, 11):
        cv[x, 11] = frost[0]
    recess(cv, 4, 3, 7, 6, frost[1])            # gauge
    cv[6, 4] = (160, 40, 30)
    rect(cv, 10, 3, 11, 7, (160, 40, 30))       # valve
    return cv


def liquefier_top():
    cv = casing(20, rivets=False, seam=False)
    for y in range(2, 14):
        for x in range(2, 14):
            d = ((x - 7.5) ** 2 + (y - 7.5) ** 2) ** 0.5
            if d < 5.6:
                cv[x, y] = S[0] if (int(d) % 2 == 0) else S[3]
    rect(cv, 7, 7, 8, 8, S[5])
    return cv


def fins():
    cv = Canvas()
    for y in range(16):
        for x in range(16):
            cv[x, y] = S[5] if x % 3 == 0 else (S[3] if x % 3 == 1 else S[2])
    return cv


def battery_front():
    cv = casing(21, seam=False)
    for i in range(4):
        x0 = 2 + i * 3
        recess(cv, x0 + 1, 3, x0 + 2, 12, S[0])
    return cv


def battery_charge(level):
    px = []
    cols = [(90, 220, 110), (150, 240, 160)]
    for i in range(4):
        x0 = 2 + i * 3
        if i < level:
            for y in range(3, 13):
                px.append((x0 + 1, y, cols[0]))
                px.append((x0 + 2, y, cols[1] if y < 6 else cols[0]))
    return glow_overlay(px)


def battery_side():
    cv = casing(22, seam=True)
    rect(cv, 4, 4, 5, 5, (170, 40, 30))
    rect(cv, 10, 4, 11, 5, (40, 40, 44))
    return cv


# ----------------------------------------------------------------------------- tanks

def tank_side(band, window=True):
    """Tank shell: paint band, steel rings, a glass window strip (the fill level is drawn by the renderer)."""
    cv = Canvas()
    for y in range(16):
        for x in range(16):
            cv[x, y] = band[[4, 5, 5, 4, 4, 3, 3, 3][x // 2]] if band is not S else S[[4, 5, 5, 4, 4, 3, 3, 3][x // 2]]
    for x in range(16):
        cv[x, 0] = S[4]
        cv[x, 1] = S[2]
        cv[x, 14] = S[4]
        cv[x, 15] = S[2]
    if window:
        for y in range(2, 14):
            for x in (6, 7, 8, 9):
                cv[x, y] = S[0]
            cv[5, y] = S[2]
            cv[10, y] = S[4]
        for y in range(3, 14, 3):
            cv[9, y] = S[4]
    return cv


def tank_top(band):
    cv = Canvas()
    for y in range(16):
        for x in range(16):
            d = ((x - 7.5) ** 2 + (y - 7.5) ** 2) ** 0.5
            cv[x, y] = S[3] if d > 6.2 else band[4]
            if 6.2 <= d < 7.2:
                cv[x, y] = S[2]
    rect(cv, 6, 6, 9, 9, S[4])
    rect(cv, 7, 7, 8, 8, P.BRASS[4])
    return cv


# ----------------------------------------------------------------------------- conduits

def cable(insulation, stripe, metal):
    cv = Canvas()
    for y in range(16):
        for x in range(16):
            cv[x, y] = insulation[[3, 2, 2, 1][(y // 2) % 4]]
            if (x + y) % 8 == 0:
                cv[x, y] = stripe
    return cv


def cable_end(metal):
    cv = Canvas()
    for y in range(16):
        for x in range(16):
            cv[x, y] = metal[5] if (x + y) % 3 else metal[3]
    return cv


def pipe(r, band=None):
    cv = Canvas()
    for y in range(16):
        for x in range(16):
            cv[x, y] = r[[5, 4, 4, 3, 3, 2, 3, 4][y % 8]]
    if band:
        for x in range(16):
            if x % 8 in (0, 1):
                for y in range(16):
                    cv[x, y] = band
    return cv


def pipe_flange(r):
    cv = Canvas()
    for y in range(16):
        for x in range(16):
            cv[x, y] = r[5] if (x in (0, 15) or y in (0, 15)) else r[3]
    for x, y in ((2, 2), (13, 2), (2, 13), (13, 13)):
        cv[x, y] = r[6]
    return cv


def valve():
    cv = Canvas()
    red = [(110, 24, 20), (150, 36, 28), (186, 56, 40), (214, 90, 70)]
    for y in range(16):
        for x in range(16):
            cv[x, y] = red[[3, 2, 2, 1][y % 4]]
    return cv


# ----------------------------------------------------------------------------- pumpjack and refinery

OCHRE = [(58, 40, 14), (88, 62, 20), (120, 86, 28), (150, 110, 38), (176, 132, 48), (198, 154, 64), (216, 176, 86)]


def paint_panel(r, seed=1):
    """Painted steel: flat paint with a darker edge line and two bolts."""
    cv = Canvas()
    n = clustered(16, 16, seed, passes=2)
    for y in range(16):
        for x in range(16):
            cv[x, y] = r[4] if n[y][x] < 0.85 else r[5]
    for i in range(16):
        cv[i, 0] = r[5]
        cv[i, 15] = r[2]
        cv[0, i] = r[5]
        cv[15, i] = r[2]
    for x, y in ((3, 3), (12, 12)):
        cv[x, y] = r[6]
        cv[x + 1, y + 1] = r[2]
    return cv


def lattice():
    """Open steel lattice (A-frame legs, skid): diagonal bracing on a dark ground."""
    cv = Canvas()
    for y in range(16):
        for x in range(16):
            cv[x, y] = S[4]
    for i in range(16):
        cv[i, i] = S[2]
        cv[15 - i, i] = S[2]
    for i in range(16):
        cv[0, i] = S[5]
        cv[15, i] = S[2]
    return cv


def weight():
    cv = Canvas()
    W = [(30, 30, 33), (42, 42, 46), (56, 56, 60), (72, 72, 77)]
    for y in range(16):
        for x in range(16):
            cv[x, y] = W[2] if (x + y) % 7 else W[1]
    for i in range(16):
        cv[i, 0] = W[3]
        cv[0, i] = W[3]
        cv[i, 15] = W[0]
        cv[15, i] = W[0]
    return cv


def motor_side():
    cv = Canvas()
    for y in range(16):
        for x in range(16):
            cv[x, y] = S[4] if y % 2 else S[2]
    for x in range(16):
        cv[x, 0] = S[5]
        cv[x, 15] = S[1]
    rect(cv, 6, 6, 9, 9, P.COPPER[3])
    return cv


def wellhead():
    cv = Canvas()
    for y in range(16):
        for x in range(16):
            cv[x, y] = S[[5, 4, 4, 3][x % 4]]
    for y in (4, 5, 10, 11):
        for x in range(16):
            cv[x, y] = (150, 36, 28) if y in (4, 10) else (110, 24, 20)
    return cv


def refinery_column():
    """Insulated column: aluminium cladding sheets with horizontal bands."""
    A = P.ALUMINIUM
    cv = Canvas()
    for y in range(16):
        for x in range(16):
            cv[x, y] = A[[4, 5, 5, 4, 4, 3, 3, 4][x // 2]]
    for x in range(16):
        cv[x, 0] = A[2]
        cv[x, 7] = A[2]
        cv[x, 8] = A[6]
    for y in range(16):
        cv[5, y] = A[3] if y not in (0, 7) else cv[5, y]
    return cv


def refinery_top():
    A = P.ALUMINIUM
    cv = Canvas()
    for y in range(16):
        for x in range(16):
            d = ((x - 7.5) ** 2 + (y - 7.5) ** 2) ** 0.5
            cv[x, y] = A[4] if d < 5 else A[3]
    rect(cv, 6, 6, 9, 9, S[2])
    return cv


def grating():
    cv = Canvas()
    for y in range(16):
        for x in range(16):
            cv[x, y] = S[4] if (x % 4 == 0 or y % 4 == 0) else (0, 0, 0, 0)
    return cv


def heater_front():
    cv = casing(31, seam=False)
    recess(cv, 3, 6, 12, 12, S[0])
    for x in range(4, 12):
        cv[x, 9] = S[2]
    rect(cv, 4, 2, 11, 3, S[2])
    cv[5, 2] = (160, 40, 30)
    cv[10, 2] = (160, 40, 30)
    return cv


def heater_glow():
    px = []
    for x in range(3, 13):
        for y in (10, 11, 12):
            px.append((x, y, GLOW[0] if y == 10 else (GLOW[1] if y == 11 else GLOW[2])))
    return glow_overlay(px)


def export(save):
    metal = (150, 230, 0, None)
    glow_rule = lambda c: (60, 10, 0, 200) if c[3] else None
    save(casing(1), 'block', 'machine_casing', metal)
    save(top(2), 'block', 'machine_top', metal)
    save(casing(3, rivets=False, seam=False), 'block', 'machine_bottom', metal)
    save(generator_front(), 'block', 'combustion_generator_front', metal)
    save(generator_glow(), 'block', 'combustion_generator_glow', (60, 10, 0, glow_rule), normal=False)
    save(crusher_front(), 'block', 'crusher_front', metal)
    save(crusher_top(), 'block', 'crusher_top', metal)
    save(arc_front(), 'block', 'arc_furnace_front', metal)
    save(arc_glow(), 'block', 'arc_furnace_glow', (60, 10, 0, glow_rule), normal=False)
    save(arc_top(), 'block', 'arc_furnace_top', metal)
    save(electrode(), 'block', 'arc_furnace_electrode', (60, 20, 0, None))
    save(press_front(), 'block', 'press_front', metal)
    save(press_parts(), 'block', 'press_steel', (200, 230, 0, None))
    save(press_die(), 'block', 'press_die', metal)
    save(fabricator_front(), 'block', 'fabricator_front', metal)
    save(fabricator_glow(), 'block', 'fabricator_glow', (60, 10, 0, glow_rule), normal=False)
    save(fabricator_top(), 'block', 'fabricator_top', metal)
    save(electrolyzer_cell(False), 'block', 'electrolyzer_cell', (220, 40, 0, None), normal=False)
    save(electrolyzer_cell(True), 'block', 'electrolyzer_cell_on')
    save(liquefier_front(), 'block', 'cryogenic_liquefier_front', metal)
    save(liquefier_top(), 'block', 'cryogenic_liquefier_top', metal)
    save(fins(), 'block', 'machine_fins', metal)
    save(battery_front(), 'block', 'battery_bank_front', metal)
    save(battery_side(), 'block', 'battery_bank_side', metal)
    for lvl in range(1, 5):
        save(battery_charge(lvl), 'block', f'battery_bank_charge_{lvl}', (60, 10, 0, glow_rule), normal=False)
    tanks = {'fluid_tank': P.CARBON_STEEL, 'pressurized_tank': P.SUIT_WHITE, 'cryogenic_tank': P.FOAM}
    for name, band in tanks.items():
        save(tank_side(band), 'block', f'{name}_side', metal)
        save(tank_top(band), 'block', f'{name}_top', metal)
    rubber = P.RUBBER
    save(cable(rubber, (180, 90, 40), P.COPPER), 'block', 'copper_cable', (80, 10, 0, None))
    save(cable(P.CARBON_STEEL, HAZ_Y, P.ALUMINIUM), 'block', 'hv_cable', (80, 10, 0, None))
    save(cable_end(P.COPPER), 'block', 'copper_cable_end', metal)
    save(cable_end(P.ALUMINIUM), 'block', 'hv_cable_end', metal)
    save(pipe(P.IRON), 'block', 'fluid_pipe', metal)
    save(pipe(P.COPPER, (214, 170, 44)), 'block', 'gas_pipe', metal)
    save(pipe_flange(P.IRON), 'block', 'fluid_pipe_flange', metal)
    save(pipe_flange(P.COPPER), 'block', 'gas_pipe_flange', metal)
    save(valve(), 'block', 'pipe_valve', metal)
    save(paint_panel(OCHRE), 'block', 'pumpjack_paint', (110, 20, 0, None))
    save(lattice(), 'block', 'pumpjack_lattice', metal)
    save(weight(), 'block', 'pumpjack_weight', metal)
    save(motor_side(), 'block', 'pumpjack_motor', metal)
    save(wellhead(), 'block', 'pumpjack_wellhead', metal)
    save(refinery_column(), 'block', 'refinery_column', (170, 232, 0, None))
    save(refinery_top(), 'block', 'refinery_column_top', (170, 232, 0, None))
    save(grating(), 'block', 'refinery_grating', metal, normal=False)
    save(heater_front(), 'block', 'refinery_front', metal)
    save(heater_glow(), 'block', 'refinery_glow', (60, 10, 0, glow_rule), normal=False)
    import os
    from generate import TEX
    with open(os.path.join(TEX, 'block', 'electrolyzer_cell_on.png.mcmeta'), 'w') as f:
        f.write('{\n  "animation": {"frametime": 3}\n}\n')
