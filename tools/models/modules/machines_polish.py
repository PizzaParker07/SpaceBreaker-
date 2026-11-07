"""Second pass on the plainest machine models: crusher, battery bank, regolith heater, sintering furnace, regolith
excavator and ice melter. Each gets a readable silhouette (hopper, flywheel, bucket wheel, chimney, terminals) and,
while running (lit), animated textures or a glow. Runs after machines_industry / machines_isru and replaces their
models for these six."""
from sbmodels.common import *  # noqa: F401,F403
from modules.machines_industry import box, casing_tex, GLOW_DATA, ROT, SIDES

STEEL6 = 'steel'


def lit_glow(fr, to, face, key, uv):
    el = box(fr, to, key, faces=(face,), uv={face: uv})
    el['neoforge_data'] = GLOW_DATA
    el['shade'] = False
    return el


def sides(key, **over):
    d = {f: key for f in SIDES}
    d.update(over)
    return d


def model(name, textures, elements):
    write(f'models/block/{name}.json', {'parent': 'minecraft:block/block', 'render_type': 'minecraft:cutout',
                                        'textures': textures, 'elements': elements})


def polished(name, t, elements, on_textures=None, on_glow=None):
    """<name> and <name>_on (textures swapped for their animated versions, plus glow quads), facing/lit blockstate."""
    model(name, t, elements)
    model(name + '_on', t | (on_textures or {}), elements + (on_glow or []))
    variants = {}
    for facing, y in ROT.items():
        for lit in ('false', 'true'):
            v = {'model': b(name + ('_on' if lit == 'true' else ''))}
            if y:
                v['y'] = y
            variants[f'facing={facing},lit={lit}'] = v
    write(f'blockstates/{name}.json', {'variants': variants})
    write(f'models/item/{name}.json', {'parent': b(name)})


def legs(x0, z0, x1, z1, h=2, w=2):
    """Four corner feet under a body spanning x0..x1, z0..z1."""
    return [box([x, 0, z], [x + w, h, z + w], STEEL6) for x in (x0, x1 - w) for z in (z0, z1 - w)]


def front_frame(x0, x1, y0, y1, z=1, depth=1):
    """Proud frame around a recessed front: two posts, a lintel and a sill, standing `depth` in front of the face."""
    return [box([x0, y0, z], [x0 + 1, y1, z + depth], STEEL6), box([x1 - 1, y0, z], [x1, y1, z + depth], STEEL6),
            box([x0 + 1, y1 - 1, z], [x1 - 1, y1, z + depth], STEEL6), box([x0 + 1, y0, z], [x1 - 1, y0 + 1, z + depth], STEEL6)]


PARTS = {'pipe': b('machine_copper_pipe'), 'panel': b('machine_side_panel'), 'cable': b('machine_cable')}


def generate():
    # crusher: 14 px jaw body on feet behind a proud frame, funnel hopper on a neck, flywheel on the right-hand side
    # (model west) and the drive motor with its cable on the left
    t = casing_tex('crusher_front') | PARTS
    t |= {'steel': b('press_steel'), 'hopper': b('crusher_hopper'), 'wheel': b('crusher_flywheel')}
    polished('crusher', t, legs(1, 1, 15, 15) + front_frame(1, 15, 2, 12) + [
        box([1, 2, 2], [15, 12, 15], sides('side', north='front', up='top', down='bottom'), uv={'north': [1, 3, 15, 13]}),
        box([3, 12, 3], [13, 14, 13], STEEL6),
        box([1, 14, 1], [15, 16, 15], sides('side', up='hopper', down='steel'), uv={'up': [1, 1, 15, 15]}),
        box([-1, 2, 3], [0, 12, 13], {'east': 'wheel', 'west': 'wheel'}, uv={'east': [3, 3, 13, 13], 'west': [3, 3, 13, 13]}),
        box([0, 6, 7], [1, 8, 9], STEEL6),
        box([15, 3, 4], [17, 8, 11], sides('steel', east='panel', up='steel', down='steel'), uv={'east': [3, 3, 10, 8]}),
        box([15.5, 0, 12], [16.5, 6, 13], 'cable'),
    ], on_textures={'front': b('crusher_front_on'), 'wheel': b('crusher_flywheel_on')})

    # regolith heater: 14 px furnace on feet with a recessed firebox front, regolith hopper on a neck, flue at the
    # back, the gas feed pipe on the left and a control box on the right
    t = casing_tex('regolith_heater_front') | PARTS
    t |= {'steel': b('press_steel'), 'hopper': b('regolith_heater_hopper'), 'flue': b('machine_flue_top'),
          'glow': b('regolith_heater_glow_on'), 'ember': b('machine_flue_ember')}
    polished('regolith_heater', t, legs(1, 1, 15, 15) + front_frame(1, 15, 2, 13) + [
        box([1, 2, 2], [15, 13, 15], sides('side', north='front', up='top', down='bottom'), uv={'north': [1, 2, 15, 13]}),
        box([4, 13, 3], [10, 15, 9], STEEL6),
        box([2, 15, 1], [12, 18, 11], sides('side', up='hopper', down='steel'), uv={'up': [3, 3, 13, 13]}),
        box([11, 13, 11], [15, 23, 15], {f: 'steel' for f in SIDES} | {'up': 'flue'}, uv={'up': [6, 6, 10, 10]}),
        box([15, 4, 4], [16.5, 5.5, 14], 'pipe'), box([15, 5.5, 12.5], [16.5, 13, 14], 'pipe'),
        box([-1, 4, 5], [1, 10, 11], sides('steel', west='panel'), uv={'west': [5, 4, 11, 10]}),
    ], on_glow=[lit_glow([1, 2, 1.99], [15, 13, 1.99], 'north', 'glow', [1, 2, 15, 13]),
                lit_glow([11, 23.01, 11], [15, 23.01, 15], 'up', 'ember', [6, 6, 10, 10])])

    # sintering furnace: 14 px kiln on feet, recessed door in a proud frame, brick crown, chimney, a burner gas line on
    # the left and the temperature controller on the right
    t = casing_tex('sintering_furnace_front', top='sintering_furnace_top') | PARTS
    t |= {'steel': b('press_steel'), 'flue': b('machine_flue_top'), 'glow': b('sintering_furnace_glow_on'),
          'ember': b('machine_flue_ember')}
    polished('sintering_furnace', t, legs(1, 1, 15, 15) + front_frame(1, 15, 2, 13) + [
        box([1, 2, 2], [15, 13, 15], sides('side', north='front', down='bottom'), uv={'north': [1, 2, 15, 13]}),
        box([1, 13, 1], [15, 15, 15], sides('steel', up='top', down='steel')),
        box([10, 15, 10], [14, 22, 14], {f: 'steel' for f in SIDES} | {'up': 'flue'}, uv={'up': [6, 6, 10, 10]}),
        box([15, 3, 3], [16.5, 4.5, 13], 'pipe'), box([15, 4.5, 3], [16.5, 9, 4.5], 'pipe'),
        box([-1, 5, 5], [1, 11, 11], sides('steel', west='panel'), uv={'west': [5, 4, 11, 10]}),
    ], on_glow=[lit_glow([1, 2, 1.99], [15, 13, 1.99], 'north', 'glow', [1, 2, 15, 13]),
                lit_glow([10, 22.01, 10], [14, 22.01, 14], 'up', 'ember', [6, 6, 10, 10])])

    # regolith excavator: 12 px drive housing on feet, auger column on its right-hand side, hydraulic line and
    # cable on the left, conveyor chute on top, two arms holding the bucket wheel (turned by the renderer)
    t = casing_tex('regolith_excavator_body') | PARTS
    t |= {'steel': b('press_steel'), 'chute': b('regolith_excavator_chute'), 'drill': b('regolith_excavator_drill')}
    polished('regolith_excavator', t, legs(3, 4, 15, 15) + [
        box([3, 2, 4], [15, 12, 15], sides('side', north='front', up='top', down='bottom'), uv={'north': [1, 3, 13, 13]}),
        box([0, 0, 9], [3, 12, 12], {f: 'drill' for f in SIDES}, uv={f: [4, 0, 7, 12] for f in SIDES}),
        box([0, 12, 8], [4, 14, 13], STEEL6),
        box([0, 5, 0], [1, 9, 4], STEEL6), box([15, 5, 0], [16, 9, 4], STEEL6), box([1, 6, 3], [15, 8, 4], STEEL6),
        box([15, 9, 5], [16, 10, 14], 'pipe'), box([15, 3, 12], [16, 9, 13], 'cable'),
        box([6, 12, 5], [12, 14, 15], {f: 'steel' for f in SIDES} | {'up': 'chute'}, uv={'up': [5, 0, 11, 11]}),
    ], on_textures={'chute': b('regolith_excavator_chute_on'), 'drill': b('regolith_excavator_drill_on')})
    wheel_t, wheel_els = excavator_wheel()
    # inventory icon: housing and wheel in one model (in the world the renderer draws the wheel)
    import json, os
    with open(os.path.join(ASSETS, 'models/block/regolith_excavator.json'), encoding='utf-8') as fh:
        housing = json.load(fh)
    write('models/item/regolith_excavator.json', {'parent': 'minecraft:block/block', 'render_type': 'minecraft:cutout',
                                                  'textures': housing['textures'] | {'bucket': wheel_t['bucket']},
                                                  'elements': housing['elements'] + wheel_els})

    # ice melter: 14 px coil plinth, glass tank set back between two frame posts, rear casing, lid with an ice hopper,
    # the meltwater outlet pipe down the right-hand side and a control box on the left
    t = casing_tex('machine_casing') | PARTS
    t |= {'tank': b('ice_melter_tank'), 'steel': b('press_steel'), 'glow': b('ice_melter_glow_on'), 'ice': b('ice_melter_top')}
    polished('ice_melter', t, legs(1, 1, 15, 15, h=1) + [
        box([1, 1, 1], [15, 3, 15], sides('side', up='bottom', down='bottom'), uv={f: [1, 13, 15, 15] for f in SIDES}),
        box([2, 3, 3], [14, 13, 10], 'tank'),
        box([1, 3, 2], [2, 13, 3], STEEL6), box([14, 3, 2], [15, 13, 3], STEEL6),
        box([1, 3, 10], [15, 13, 15], sides('side')),
        box([1, 13, 2], [15, 15, 15], sides('side', north='steel', up='top', down='steel')),
        box([3, 15, 4], [13, 18, 14], sides('steel', up='ice'), uv={'up': [3, 3, 13, 13]}),
        box([-0.5, 2, 6], [1, 4, 12], 'pipe'), box([-0.5, 4, 10.5], [1, 12, 12], 'pipe'),
        box([15, 5, 9], [17, 11, 14], sides('steel', east='panel'), uv={'east': [5, 4, 10, 10]}),
    ], on_textures={'tank': b('ice_melter_tank_on')},
        on_glow=[lit_glow([1, 1, 0.99], [15, 3, 0.99], 'north', 'glow', [1, 13, 15, 15])])

    battery_bank()
    group_one()


def battery_bank():
    """Cell stack on a plinth under a cap with two terminals; charge bars glow, a chase light runs while power flows."""
    t = casing_tex('battery_bank_front', side='battery_bank_side')
    t |= {'steel': b('press_steel'), 'cell': b('battery_bank_cell'), 'term': b('battery_bank_terminal'), 'flow': b('battery_bank_flow')}
    for lvl in range(1, 5):
        t[f'c{lvl}'] = b(f'battery_bank_charge_{lvl}')
    t |= PARTS
    base = front_frame(1, 15, 2, 13) + [
        box([1, 0, 1], [15, 2, 15], sides('side', up='bottom', down='bottom'), uv={f: [1, 14, 15, 16] for f in SIDES}),
        box([2, 2, 2], [14, 13, 14], sides('cell', north='front'), uv={'north': [2, 2, 14, 13]}),
        box([1, 13, 1], [15, 15, 15], sides('steel', up='top', down='steel')),
        box([3, 15, 6], [5, 17, 8], 'term', uv={f: [0, 0, 2, 2] for f in ('north', 'south', 'east', 'west', 'up')}),
        box([11, 15, 6], [13, 17, 8], 'term', uv={f: [8, 0, 10, 2] for f in ('north', 'south', 'east', 'west', 'up')}),
        # cable glands with their leads on both sides, a bus bar along the back
        box([14, 8, 6], [16, 10, 10], STEEL6), box([14.5, 2, 7.5], [15.5, 8, 8.5], 'cable'),
        box([0, 8, 6], [2, 10, 10], STEEL6), box([0.5, 2, 7.5], [1.5, 8, 8.5], 'cable'),
        box([3, 4, 14], [13, 6, 15], 'pipe'),
    ]
    flow = lit_glow([1, 0, 0.99], [15, 2, 0.99], 'north', 'flow', [1, 14, 15, 16])
    for lvl in range(5):
        for lit in (False, True):
            els = list(base)
            if lvl:
                els.append(lit_glow([2, 2, 1.99], [14, 13, 1.99], 'north', f'c{lvl}', [2, 2, 14, 13]))
            if lit:
                els.append(flow)
            model('battery_bank' + (f'_{lvl}' if lvl else '') + ('_on' if lit else ''), t, els)
    variants = {}
    for facing, y in ROT.items():
        for lit in ('false', 'true'):
            for lvl in range(5):
                for down in ('false', 'true'):
                    v = {'model': b('battery_bank' + (f'_{lvl}' if lvl else '') + ('_on' if lit == 'true' else ''))}
                    if down == 'true':
                        v['x'] = 90  # ceiling-mounted: the front (output) faces down
                    if y:
                        v['y'] = y
                    variants[f'charge={lvl},facing={facing},lit={lit},output_down={down}'] = v
    write('blockstates/battery_bank.json', {'variants': variants})
    write('models/item/battery_bank.json', {'parent': b('battery_bank_4')})


def excavator_wheel():
    """Bucket wheel, centred on (8, 7) in the plane z 0..2: an octagonal rim, four crossed spokes, eight buckets.
    Diagonal parts use 45-degree element rotations; the renderer spins the whole model."""
    t = {'steel': b('press_steel'), 'bucket': b('regolith_excavator_bucket'), 'particle': b('press_steel')}
    straight = [
        box([2, 6.5, 0.5], [14, 7.5, 1.5], 'steel'),                 # spokes
        box([7.5, 1, 0.5], [8.5, 13, 1.5], 'steel'),
        box([5, 12, 0], [11, 13, 2], 'steel'), box([5, 1, 0], [11, 2, 2], 'steel'),     # rim
        box([2, 4, 0], [3, 10, 2], 'steel'), box([13, 4, 0], [14, 10, 2], 'steel'),
        box([6.5, 13, -0.5], [9.5, 14.5, 2.5], 'bucket'), box([6.5, -0.5, -0.5], [9.5, 1, 2.5], 'bucket'),  # buckets
        box([0.5, 5.5, -0.5], [2, 8.5, 2.5], 'bucket'), box([14, 5.5, -0.5], [15.5, 8.5, 2.5], 'bucket'),
    ]
    els = list(straight)
    for el in straight:
        r = dict(el)
        r['rotation'] = {'origin': [8, 7, 1], 'axis': 'z', 'angle': 45}
        els.append(r)
    els.append(box([6.5, 5.5, -0.5], [9.5, 8.5, 2.5], 'steel'))     # hub
    write('models/block/regolith_excavator_wheel.json', {'parent': 'minecraft:block/block', 'render_type': 'minecraft:cutout',
                                                         'textures': t, 'elements': els})
    return t, els


PANEL_UV = [5, 4, 11, 10]
OUT = {'east': (0.01, 0, 0), 'west': (-0.01, 0, 0), 'north': (0, 0, -0.01), 'south': (0, 0, 0.01)}


def panel(face, fr, to):
    """A control box (6x6 face showing the panel) and the glow quad for its lamps while running."""
    el = box(fr, to, sides('steel', **{face: 'panel'}, up='steel', down='steel'), uv={face: PANEL_UV})
    dx, dy, dz = OUT[face]
    a, b_ = list(fr), list(to)
    if face == 'east':
        a[0] = b_[0]
    elif face == 'west':
        b_[0] = a[0]
    elif face == 'north':
        b_[2] = a[2]
    else:
        a[2] = b_[2]
    g = lit_glow([a[0] + dx, a[1], a[2] + dz], [b_[0] + dx, b_[1], b_[2] + dz], face, 'panel_glow', PANEL_UV)
    return el, g


def parts_tex(t):
    return t | PARTS | {'steel': b('press_steel'), 'panel_glow': b('machine_side_panel_glow')}


def group_one():
    """Electrolyzer, cryogenic liquefier, chemical reactor, arc furnace, gas centrifuge and fission unit in the same
    language as the first six: feet, a recessed front behind a proud frame, their own side parts, lamps that run."""
    # electrolyzer: two glass cells between frame posts on a coil plinth, rear casing, gas take-off pipes on the lid
    t = parts_tex(casing_tex('machine_casing')) | {'cell': b('electrolyzer_cell')}
    pnl, pglow = panel('east', [15, 5, 9], [17, 11, 15])
    polished('electrolyzer', t, legs(1, 1, 15, 15, h=1) + [
        box([1, 1, 1], [15, 3, 15], sides('side', up='bottom', down='bottom'), uv={f: [1, 13, 15, 15] for f in SIDES}),
        box([2, 3, 3], [7, 13, 9], 'cell'), box([9, 3, 3], [14, 13, 9], 'cell'),
        box([1, 3, 2], [2, 13, 3], STEEL6), box([14, 3, 2], [15, 13, 3], STEEL6), box([7, 3, 2], [9, 13, 3], STEEL6),
        box([1, 3, 10], [15, 13, 15], sides('side')),
        box([1, 13, 2], [15, 15, 15], sides('side', north='steel', up='top', down='steel')),
        box([3.5, 15, 5], [5, 18, 6.5], 'pipe'), box([3.5, 18, 5], [5, 19.5, 13], 'pipe'),
        box([11, 15, 5], [12.5, 17, 6.5], 'pipe'), box([11, 17, 5], [12.5, 18.5, 13], 'pipe'),
        box([-0.5, 1, 7.5], [0.5, 9, 8.5], 'cable'), box([-1, 8, 6], [1, 10, 10], STEEL6),
        pnl,
    ], on_textures={'cell': b('electrolyzer_cell_on')}, on_glow=[pglow])

    # cryogenic liquefier: finned cold box on feet, recessed front, compressor fan plenum on top, LOX line on the left,
    # controls on the right; the fan turns while it runs
    t = parts_tex(casing_tex('cryogenic_liquefier_front', top='machine_fan', side='machine_fins'))
    pnl, pglow = panel('west', [-1, 4, 5], [1, 10, 11])
    polished('cryogenic_liquefier', t, legs(1, 1, 15, 15) + front_frame(1, 15, 2, 13) + [
        box([1, 2, 2], [15, 13, 15], sides('side', north='front', up='bottom', down='bottom'), uv={'north': [1, 3, 15, 14]}),
        box([2, 13, 2], [14, 16, 14], sides('steel', up='top'), uv={'up': [2, 2, 14, 14]}),
        box([15, 3, 4], [16.5, 4.5, 13], 'pipe'), box([15, 4.5, 11.5], [16.5, 12, 13], 'pipe'),
        box([14, 11, 11.5], [15, 12, 13], 'pipe'),
        pnl,
    ], on_textures={'top': b('machine_fan_on')}, on_glow=[pglow])

    # chemical reactor: low base on feet with a recessed gauge front, enamelled vessel, agitator drive on the lid, acid
    # feed pipe climbing the left side into the vessel, controls on the right
    t = parts_tex(casing_tex('chemical_reactor_front')) | {'vessel': b('chemical_reactor_vessel')}
    pnl, pglow = panel('west', [-1, 2, 5], [1, 8, 11])
    polished('chemical_reactor', t, legs(1, 1, 15, 15) + front_frame(1, 15, 2, 9) + [
        box([1, 2, 2], [15, 9, 15], sides('side', north='front', up='top', down='bottom'), uv={'north': [1, 8, 15, 15]}),
        box([2, 9, 2], [14, 16, 14], sides('vessel', up='steel', down='steel'), uv={f: [2, 0, 14, 7] for f in SIDES}),
        box([3, 16, 3], [13, 17, 13], STEEL6),
        box([6, 17, 6], [10, 21, 10], STEEL6), box([7, 21, 7], [9, 22, 9], 'cable'),
        box([12, 13, 0], [14, 15, 2], STEEL6),
        box([15, 3, 4], [16.5, 4.5, 11], 'pipe'), box([15, 4.5, 9.5], [16.5, 14, 11], 'pipe'),
        box([14, 12.5, 9.5], [15, 14, 11], 'pipe'),
        pnl,
    ], on_glow=[pglow])

    # arc furnace: brick-doored body on feet behind a frame, roof with three electrodes on a clamp bar, the transformer
    # on the left feeding a heavy cable up to the clamps
    t = parts_tex(casing_tex('arc_furnace_front', top='arc_furnace_top')) | {'electrode': b('arc_furnace_electrode'),
                                                                           'glow': b('arc_furnace_glow')}
    els = legs(1, 1, 15, 15) + front_frame(1, 15, 2, 13) + [
        box([1, 2, 2], [15, 13, 15], sides('side', north='front', up='top', down='bottom'), uv={'north': [1, 2, 15, 13]}),
        box([1, 13, 1], [15, 14, 15], sides('steel', up='top')),
    ]
    for cx in (4, 8, 12):
        els.append(box([cx - 1, 14, 7], [cx + 1, 21, 9], 'electrode', uv={f: [0, 0, 2, 7] for f in SIDES} | {'up': [0, 2, 2, 4]}))
    els += [box([1, 18, 6], [15, 19, 10], 'side'),
            box([15, 2, 3], [17, 10, 13], sides('steel', east='panel', up='steel'), uv={'east': [3, 3, 13, 11]}),
            box([15.5, 10, 7.5], [16.5, 19, 8.5], 'cable'), box([15, 18, 7.5], [15.5, 19, 8.5], 'cable')]
    polished('arc_furnace', t, els, on_glow=[lit_glow([1, 2, 1.99], [15, 13, 1.99], 'north', 'glow', [1, 2, 15, 13]),
                                             lit_glow([17.01, 2, 3], [17.01, 10, 13], 'east', 'panel_glow', [3, 3, 13, 11])])

    # gas centrifuge: control casing on feet with a recessed lamp panel, four rotor tubes, a feed/product header
    # across the tube tops and the feed line down the left side; the stage lamps walk while it spins
    t = parts_tex(casing_tex('gas_centrifuge_front', top='machine_top')) | {'tube': b('gas_centrifuge_tube'),
                                                                           'lamps': b('gas_centrifuge_lamps')}
    els = legs(1, 1, 15, 15, h=1) + front_frame(1, 15, 1, 7) + [
        box([1, 1, 2], [15, 7, 15], sides('side', north='front', up='top', down='bottom'), uv={'north': [1, 9, 15, 15]}),
    ]
    for x, z in ((2, 2), (10, 2), (2, 10), (10, 10)):
        els.append(box([x, 7, z], [x + 4, 16, z + 4], {f: 'tube' for f in SIDES} | {'up': 'top'}, uv={f: [x, 0, x + 4, 9] for f in SIDES}))
    els += [box([3.5, 16, 7.5], [12.5, 17, 8.5], 'pipe'), box([7.5, 16, 3.5], [8.5, 17, 12.5], 'pipe'),
            box([15, 2, 6], [16.5, 3.5, 10], 'pipe'), box([15, 3.5, 8.5], [16.5, 17, 10], 'pipe'),
            box([12.5, 16, 8.5], [16.5, 17, 10], 'pipe')]
    polished('gas_centrifuge', t, els, on_glow=[lit_glow([1, 1, 1.99], [15, 7, 1.99], 'north', 'lamps', [1, 9, 15, 15])])

    # fission surface power unit: core drum on four feet, crossed radiator panels, coolant loop pipes from the drum
    # to the panel roots and a shielded instrument box on the left; the status window glows while it runs
    t = parts_tex(casing_tex('fission_reactor_front', top='machine_top', side='fission_reactor_side')) | {
        'fin': b('fission_reactor_fin'), 'window': b('fission_reactor_window')}
    pnl, pglow = panel('west', [0, 3, 5], [2, 9, 11])
    polished('fission_reactor', t, legs(2, 2, 14, 14) + [
        box([2, 2, 2], [14, 10, 14], sides('side', north='front', up='top', down='bottom'), uv={f: [2, 7, 14, 15] for f in SIDES}),
        box([0, 10, 7.5], [16, 16, 8.5], 'fin', uv={f: [0, 0, 16, 6] for f in ('north', 'south', 'up', 'down', 'east', 'west')}),
        box([7.5, 10, 0], [8.5, 16, 16], 'fin', uv={f: [0, 0, 16, 6] for f in ('east', 'west', 'north', 'south', 'up', 'down')}),
        box([7, 10, 7], [9, 17, 9], 'side'),
        box([14, 5, 3.5], [15.5, 6.5, 6], 'pipe'), box([14, 6.5, 3.5], [15.5, 11, 5], 'pipe'),
        box([14, 5, 10], [15.5, 6.5, 12.5], 'pipe'), box([14, 6.5, 11], [15.5, 11, 12.5], 'pipe'),
        pnl,
    ], on_glow=[pglow, lit_glow([2, 2, 1.99], [14, 10, 1.99], 'north', 'window', [2, 7, 14, 15])])
