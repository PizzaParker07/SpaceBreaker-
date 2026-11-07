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
