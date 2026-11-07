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


def generate():
    # crusher: plinth, jaw body, a funnel hopper on top and a flywheel on the right-hand side (model west)
    t = casing_tex('crusher_front')
    t |= {'steel': b('press_steel'), 'hopper': b('crusher_hopper'), 'wheel': b('crusher_flywheel')}
    polished('crusher', t, [
        box([0, 0, 0], [16, 1, 16], STEEL6),
        box([0, 1, 0], [16, 12, 16], sides('side', north='front', up='top', down='bottom'), uv={'north': [0, 3, 16, 14]}),
        box([3, 12, 3], [13, 14, 13], STEEL6),
        box([1, 14, 1], [15, 16, 15], sides('side', up='hopper', down='steel'), uv={'up': [1, 1, 15, 15]}),
        box([-1, 2, 3], [0, 12, 13], {'east': 'wheel', 'west': 'wheel'}, uv={'east': [3, 3, 13, 13], 'west': [3, 3, 13, 13]}),
    ], on_textures={'front': b('crusher_front_on'), 'wheel': b('crusher_flywheel_on')})

    # regolith heater: brick-lined crucible furnace, regolith feed hopper on a neck, a flue at the back
    t = casing_tex('regolith_heater_front')
    t |= {'steel': b('press_steel'), 'hopper': b('regolith_heater_hopper'), 'flue': b('machine_flue_top'),
          'glow': b('regolith_heater_glow_on'), 'ember': b('machine_flue_ember')}
    polished('regolith_heater', t, [
        box([0, 0, 0], [16, 12, 16], sides('side', north='front', up='top', down='bottom'), uv={'north': [0, 2, 16, 14]}),
        box([4, 12, 3], [10, 14, 9], STEEL6),
        box([2, 14, 1], [12, 17, 11], sides('side', up='hopper', down='steel'), uv={'up': [3, 3, 13, 13]}),
        box([11, 12, 11], [15, 22, 15], {f: 'steel' for f in SIDES} | {'up': 'flue'}, uv={'up': [6, 6, 10, 10]}),
    ], on_glow=[lit_glow([0, 0, -0.01], [16, 12, -0.01], 'north', 'glow', [0, 2, 16, 14]),
                lit_glow([11, 22.01, 11], [15, 22.01, 15], 'up', 'ember', [6, 6, 10, 10])])

    # sintering furnace: kiln on a plinth with a proud door frame, brick crown and a chimney
    t = casing_tex('sintering_furnace_front', top='sintering_furnace_top')
    t |= {'steel': b('press_steel'), 'flue': b('machine_flue_top'), 'glow': b('sintering_furnace_glow_on'),
          'ember': b('machine_flue_ember')}
    polished('sintering_furnace', t, [
        box([0, 0, 0], [16, 2, 16], STEEL6),
        box([1, 2, 1], [15, 14, 15], sides('side', north='front', down='bottom')),
        box([2, 2, 0], [3, 14, 1], STEEL6), box([13, 2, 0], [14, 14, 1], STEEL6), box([3, 13, 0], [13, 14, 1], STEEL6),
        box([0, 14, 0], [16, 16, 16], sides('steel', up='top', down='steel')),
        box([10, 16, 10], [14, 23, 14], {f: 'steel' for f in SIDES} | {'up': 'flue'}, uv={'up': [6, 6, 10, 10]}),
    ], on_glow=[lit_glow([1, 2, 0.99], [15, 14, 0.99], 'north', 'glow', [1, 2, 15, 14]),
                lit_glow([10, 23.01, 10], [14, 23.01, 14], 'up', 'ember', [6, 6, 10, 10])])

    # regolith excavator: bucket wheel on two arms in front of the drive housing, conveyor chute on top
    t = casing_tex('regolith_excavator_body')
    t |= {'steel': b('press_steel'), 'wheel': b('regolith_excavator_wheel'), 'chute': b('regolith_excavator_chute')}
    polished('regolith_excavator', t, [
        box([0, 0, 4], [16, 13, 16], sides('side', north='front', up='top', down='bottom')),
        box([2, 1, 1], [14, 13, 3], {'north': 'wheel', 'south': 'wheel'}, uv={'north': [2, 2, 14, 14], 'south': [2, 2, 14, 14]}),
        box([2, 6, 2], [14, 8, 4], STEEL6),
        box([0, 5, 0], [2, 9, 4], STEEL6), box([14, 5, 0], [16, 9, 4], STEEL6),
        box([5, 13, 5], [11, 15, 16], {f: 'steel' for f in SIDES} | {'up': 'chute'}, uv={'up': [5, 0, 11, 11]}),
    ], on_textures={'wheel': b('regolith_excavator_wheel_on'), 'chute': b('regolith_excavator_chute_on')})

    # ice melter: plinth with heating coil, glass tank, lid with an ice hopper
    t = casing_tex('machine_casing')
    t |= {'tank': b('ice_melter_tank'), 'steel': b('press_steel'), 'glow': b('ice_melter_glow_on'), 'ice': b('ice_melter_top')}
    polished('ice_melter', t, [
        box([0, 0, 0], [16, 3, 16], sides('side', up='bottom', down='bottom'), uv={f: [0, 13, 16, 16] for f in SIDES}),
        box([1, 3, 2], [15, 13, 10], 'tank'),
        box([0, 3, 10], [16, 13, 16], sides('side')),
        box([0, 13, 1], [16, 15, 16], sides('side', north='steel', up='top', down='steel')),
        box([2, 15, 3], [14, 18, 15], sides('steel', up='ice'), uv={'up': [2, 2, 14, 14]}),
        box([13, 3, 0], [15, 13, 2], STEEL6),
    ], on_textures={'tank': b('ice_melter_tank_on')},
        on_glow=[lit_glow([0, 0, -0.01], [16, 3, -0.01], 'north', 'glow', [0, 13, 16, 16])])

    battery_bank()


def battery_bank():
    """Cell stack on a plinth under a cap with two terminals; charge bars glow, a chase light runs while power flows."""
    t = casing_tex('battery_bank_front', side='battery_bank_side')
    t |= {'steel': b('press_steel'), 'cell': b('battery_bank_cell'), 'term': b('battery_bank_terminal'), 'flow': b('battery_bank_flow')}
    for lvl in range(1, 5):
        t[f'c{lvl}'] = b(f'battery_bank_charge_{lvl}')
    base = [
        box([0, 0, 0], [16, 2, 16], sides('side', up='bottom', down='bottom'), uv={f: [0, 14, 16, 16] for f in SIDES}),
        box([1, 2, 1], [15, 14, 15], sides('cell', north='front')),
        box([0, 14, 0], [16, 16, 16], sides('steel', up='top', down='steel')),
        box([3, 16, 6], [5, 18, 8], 'term', uv={f: [0, 0, 2, 2] for f in ('north', 'south', 'east', 'west', 'up')}),
        box([11, 16, 6], [13, 18, 8], 'term', uv={f: [8, 0, 10, 2] for f in ('north', 'south', 'east', 'west', 'up')}),
    ]
    flow = lit_glow([0, 0, -0.01], [16, 2, -0.01], 'north', 'flow', [0, 14, 16, 16])
    for lvl in range(5):
        for lit in (False, True):
            els = list(base)
            if lvl:
                els.append(lit_glow([1, 2, 0.99], [15, 14, 0.99], 'north', f'c{lvl}', [1, 2, 15, 14]))
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
