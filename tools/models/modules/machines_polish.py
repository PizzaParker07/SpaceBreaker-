"""Second pass on the plainest machine models: crusher, battery bank, regolith heater, sintering furnace, regolith
excavator and ice melter. Each gets a readable silhouette (hopper, flywheel, bucket wheel, chimney, terminals) and,
while running (lit), animated textures or a glow. Runs after machines_industry / machines_isru and replaces their
models for these six."""
from sbmodels.common import *  # noqa: F401,F403
import json
import os

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


# which kind of control box each machine carries, so they do not all look alike (default: two lamps)
PANEL_KIND = {
    'crusher': 'buttons', 'press': 'buttons', 'fabricator': 'buttons', 'atmosphere_processor': 'buttons',
    'chemical_reactor': 'screen', 'fission_reactor': 'screen', 'electrolyzer': 'screen', 'rtg': 'screen',
    'cryogenic_liquefier': 'dial', 'sabatier_reactor': 'dial', 'regolith_heater': 'dial', 'sintering_furnace': 'dial',
    'ice_melter': 'valve', 'combustion_generator': 'valve',
}


def model(name, textures, elements):
    kind = PANEL_KIND.get(name.removesuffix('_on'))
    if kind and 'panel' in textures:
        textures = textures | {'panel': b(f'machine_panel_{kind}'), 'panel_glow': b(f'machine_panel_{kind}_glow')}
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


PARTS = {'pipe': b('machine_copper_pipe'), 'panel': b('machine_side_panel'), 'cable': b('machine_cable'),
         'panel_glow': b('machine_side_panel_glow')}


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
    ], on_textures={'front': b('crusher_front_on'), 'wheel': b('crusher_flywheel_on')},
        on_glow=[lit_glow([17.01, 3, 4], [17.01, 8, 11], 'east', 'panel_glow', [3, 3, 10, 8])])

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
                lit_glow([11, 23.01, 11], [15, 23.01, 15], 'up', 'ember', [6, 6, 10, 10]),
                lit_glow([-1.01, 4, 5], [-1.01, 10, 11], 'west', 'panel_glow', [5, 4, 11, 10])])

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
                lit_glow([10, 22.01, 10], [14, 22.01, 14], 'up', 'ember', [6, 6, 10, 10]),
                lit_glow([-1.01, 5, 5], [-1.01, 11, 11], 'west', 'panel_glow', [5, 4, 11, 10])])

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
        on_glow=[lit_glow([1, 1, 0.99], [15, 3, 0.99], 'north', 'glow', [1, 13, 15, 15]),
                 lit_glow([17.01, 5, 9], [17.01, 11, 14], 'east', 'panel_glow', [5, 4, 10, 10])])

    battery_bank()
    group_one()
    group_two()
    group_three()
    multiblocks()


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


def group_two():
    """RTG, combustion generator, press, fabricator, atmosphere processor and Sabatier reactor."""
    # RTG: finned body on four feet, bolted cap with lifting lugs, terminal box on the right, power lead to the ground
    t = parts_tex({'front': b('rtg_front'), 'side': b('rtg_side'), 'top': b('rtg_top'), 'bottom': b('machine_bottom'),
                   'particle': b('rtg_side')})
    pnl, pglow = panel('west', [-1, 3, 5], [1, 9, 11])
    polished('rtg', t, legs(1, 1, 15, 15) + [
        box([1, 2, 1], [15, 14, 15], sides('side', north='front', up='top', down='bottom'), uv={f: [1, 2, 15, 14] for f in SIDES}),
        box([3, 14, 3], [13, 16, 13], sides('steel', up='top', down='bottom')),
        box([4, 16, 7], [6, 17, 9], STEEL6), box([10, 16, 7], [12, 17, 9], STEEL6),
        box([-0.5, 0, 7.5], [0.5, 3, 8.5], 'cable'),
        pnl,
    ], on_glow=[pglow])

    # combustion generator: firebox body on feet behind a frame, chimney with a rain cap, fuel line on the left,
    # controls on the right; the chimney mouth glows while it burns
    t = parts_tex(casing_tex('combustion_generator_front')) | {'glow': b('combustion_generator_glow'),
                                                              'flue': b('machine_flue_top'), 'ember': b('machine_flue_ember')}
    pnl, pglow = panel('west', [-1, 4, 5], [1, 10, 11])
    polished('combustion_generator', t, legs(1, 1, 15, 15) + front_frame(1, 15, 2, 13) + [
        box([1, 2, 2], [15, 13, 15], sides('side', north='front', up='top', down='bottom'), uv={'north': [1, 2, 15, 13]}),
        box([10, 13, 10], [14, 21, 14], {f: 'side' for f in SIDES} | {'up': 'flue'}, uv={'up': [6, 6, 10, 10]}),
        box([9, 21, 9], [15, 22, 15], {f: 'side' for f in SIDES} | {'down': 'side'}),
        box([15, 3, 4], [16.5, 4.5, 13], 'pipe'), box([15, 4.5, 4], [16.5, 10, 5.5], 'pipe'),
        pnl,
    ], on_glow=[pglow, lit_glow([1, 2, 1.99], [15, 13, 1.99], 'north', 'glow', [1, 2, 15, 13]),
                lit_glow([10, 20.99, 10], [14, 20.99, 14], 'up', 'ember', [6, 6, 10, 10])])

    # press: bed on feet, four columns, crown with the hydraulic cylinder on top, pressure line down the left side,
    # pump box on the right; the ram is drawn and stroked by PressRenderer
    t = parts_tex(casing_tex('press_front')) | {'die': b('press_die')}
    pnl, pglow = panel('west', [-1, 2, 5], [1, 8, 11])
    els = legs(1, 1, 15, 15) + [
        box([1, 2, 1], [15, 5, 15], sides('side', north='front', up='die', down='bottom'), uv={'north': [1, 11, 15, 14]}),
        box([1, 13, 1], [15, 16, 15], sides('steel', up='top')),
        box([5, 16, 5], [11, 19, 11], STEEL6), box([6, 19, 6], [10, 20, 10], STEEL6),
    ]
    for x, z in ((1, 1), (13, 1), (1, 13), (13, 13)):
        els.append(box([x, 5, z], [x + 2, 13, z + 2], STEEL6))
    els += [box([15, 3, 7], [16.5, 4.5, 9], 'pipe'), box([15, 4.5, 7], [16.5, 18, 8.5], 'pipe'),
            box([11, 17, 7], [16.5, 18.5, 8.5], 'pipe'), pnl]
    polished('press', t, els, on_glow=[pglow])

    # fabricator: cabinet on feet with the screen set back, work table, gantry with the tool head, a wire spool on the
    # left and controls on the right
    t = parts_tex(casing_tex('fabricator_front', top='fabricator_top')) | {'glow': b('fabricator_glow')}
    pnl, pglow = panel('west', [-1, 3, 5], [1, 9, 11])
    polished('fabricator', t, legs(1, 1, 15, 15) + front_frame(1, 15, 2, 12) + [
        box([1, 2, 2], [15, 12, 15], sides('side', north='front', up='top', down='bottom'), uv={'north': [1, 2, 15, 12]}),
        box([1, 12, 1], [15, 13, 15], sides('steel', up='top')),
        box([1, 13, 13], [3, 18, 15], STEEL6), box([13, 13, 13], [15, 18, 15], STEEL6),
        box([1, 18, 12], [15, 20, 15], STEEL6),
        box([6, 15, 11], [10, 19, 13], STEEL6), box([7, 14, 11], [9, 15, 12], STEEL6),
        box([15, 4, 5], [17, 10, 11], sides('cable', up='steel', down='steel', east='steel')),
        box([17, 6, 7], [17.5, 8, 9], STEEL6),
        pnl,
    ], on_glow=[pglow, lit_glow([1, 2, 1.99], [15, 12, 1.99], 'north', 'glow', [1, 2, 15, 12])])

    # atmosphere processor: compressor body on feet behind its louvred front, fan plenum (the fan turns while it runs),
    # intake stack with a rain cap, product line on the left and controls on the right
    t = parts_tex(casing_tex('atmosphere_processor_front', top='atmosphere_processor_top', side='atmosphere_processor_side'))
    pnl, pglow = panel('west', [-1, 3, 5], [1, 9, 11])
    polished('atmosphere_processor', t, legs(1, 1, 15, 15) + front_frame(1, 15, 2, 12) + [
        box([1, 2, 2], [15, 12, 15], sides('side', north='front', up='bottom', down='bottom'), uv={'north': [1, 2, 15, 12]}),
        box([1, 12, 1], [15, 14, 15], sides('bottom', up='top'), uv={'up': [1, 1, 15, 15]}),
        box([11, 14, 11], [14, 20, 14], STEEL6), box([10, 20, 10], [15, 21, 15], STEEL6),
        box([15, 3, 4], [16.5, 4.5, 12], 'pipe'), box([15, 4.5, 10.5], [16.5, 13, 12], 'pipe'),
        pnl,
    ], on_textures={'top': b('machine_fan_on')}, on_glow=[pglow])

    # Sabatier reactor: base on feet with the sight glass behind a frame, catalyst vessel between two flanges, CO2 and
    # H2 feed lines into the flanges, the water drain on the left, controls on the right
    t = parts_tex(casing_tex('sabatier_reactor_front')) | {'tube': b('sabatier_reactor_tube'), 'glow': b('sabatier_reactor_glow')}
    pnl, pglow = panel('west', [-1, 2, 5], [1, 8, 11])
    polished('sabatier_reactor', t, legs(1, 1, 15, 15) + front_frame(1, 15, 2, 10) + [
        box([1, 2, 2], [15, 10, 15], sides('side', north='front', up='top', down='bottom'), uv={'north': [1, 2, 15, 10]}),
        box([2, 10, 4], [14, 15, 12], {'north': 'tube', 'south': 'tube', 'up': 'tube', 'down': 'tube'},
            uv={'north': [0, 3, 12, 8], 'south': [0, 3, 12, 8], 'up': [0, 0, 12, 8], 'down': [0, 0, 12, 8]}),
        box([1, 9, 3], [2, 16, 13], STEEL6), box([14, 9, 3], [15, 16, 13], STEEL6),
        box([7, 15, 7], [9, 17, 9], STEEL6),
        box([15, 12, 5], [17, 13.5, 6.5], 'pipe'), box([15, 12, 9.5], [17, 13.5, 11], 'pipe'),
        box([15.5, 3, 5], [17, 12, 6.5], 'pipe'), box([15.5, 3, 9.5], [17, 12, 11], 'pipe'),
        box([-0.5, 9, 7.5], [1, 10.5, 9], 'pipe'),
        pnl,
    ], on_glow=[pglow, lit_glow([1, 2, 1.99], [15, 10, 1.99], 'north', 'glow', [1, 2, 15, 10])])


def group_three():
    """Tanks, solar trackers and the power monitor."""
    from modules.machines import SOLAR_TEX, solar_base_elements, solar_head_elements, solar_array_head_elements

    # tanks: shell on corner feet (TankRenderer draws the level window on the shell, 2..14 px high, so the shell keeps
    # its 1..15 footprint), a flanged top ring and one nozzle stub per side; each kind has its own top fittings
    def stubs():
        out = []
        for face, fr, to in (('north', [11, 3, 0], [14, 6, 1]), ('south', [2, 3, 15], [5, 6, 16]),
                             ('east', [15, 3, 11], [16, 6, 14]), ('west', [0, 3, 2], [1, 6, 5])):
            out.append(box(fr, to, 'pipe'))
        return out

    tops = {
        'fluid_tank': [box([5, 16, 5], [11, 17, 11], STEEL6), box([7, 17, 7], [9, 17.5, 9], STEEL6),
                       box([11.5, 16, 2.5], [13, 19, 4], 'pipe')],
        'pressurized_tank': [box([3, 16, 3], [13, 17, 13], STEEL6), box([5, 17, 5], [11, 18, 11], STEEL6),
                             box([7, 18, 7], [9, 20, 9], 'pipe'), box([6.5, 20, 6.5], [9.5, 20.5, 9.5], STEEL6)],
        'cryogenic_tank': [box([5, 16, 5], [11, 17, 11], STEEL6), box([11, 16, 11], [13, 22, 13], STEEL6),
                           box([10.5, 22, 10.5], [13.5, 22.5, 13.5], STEEL6), box([3, 16, 3], [4.5, 18, 4.5], 'pipe')],
    }
    for name, top in tops.items():
        t = {'side': b(f'{name}_side'), 'top': b(f'{name}_top'), 'particle': b(f'{name}_side')} | PARTS | {'steel': b('press_steel')}
        els = legs(0.5, 0.5, 15.5, 15.5) + [
            box([1, 2, 1], [15, 15, 15], {'north': 'side', 'south': 'side', 'east': 'side', 'west': 'side', 'down': 'top'}),
            box([0.5, 15, 0.5], [15.5, 16, 15.5], sides('steel', up='top', down='steel'), uv={'up': [0.5, 0.5, 15.5, 15.5]}),
        ] + stubs() + top
        model(name, t, els)
        write(f'blockstates/{name}.json', {'variants': {'': {'model': b(name)}}})
        write(f'models/item/{name}.json', {'parent': b(name)})

    # solar trackers: anchor bolts on the base plate, a junction box and the power lead leaving the post
    tex = SOLAR_TEX | {'cable': b('machine_cable'), 'box': b('press_steel')}
    extra = [box([x, 2, z], [x + 1, 2.5, z + 1], 'steel') for x in (2.5, 12.5) for z in (2.5, 12.5)] + [
        box([3, 2, 9], [6, 6, 12], 'box'), box([4, 6, 10], [5, 6.5, 11], 'steel'),
        box([6, 2, 10], [7, 3, 11], 'cable'), box([7, 2, 10], [8, 4, 11], 'cable'),
        box([4, 0, 12], [5, 2, 16], 'cable'),
    ]
    for name, head in (('solar_panel', solar_head_elements), ('solar_array', solar_array_head_elements)):
        model(name + '_base', tex, solar_base_elements() + extra)
        item = {'parent': 'minecraft:block/block', 'textures': tex, 'elements': solar_base_elements() + extra + head()}
        with open(os.path.join(ASSETS, f'models/item/{name}.json'), encoding='utf-8') as fh:
            item['display'] = json.load(fh)['display']
        write(f'models/item/{name}.json', item)

    # power monitor: the screen face stays on the block's front plane (PowerMonitorRenderer draws the figures just in
    # front of it); a raised bezel frames it, a sun visor over the top, the cabinet stands on four feet with grab handles
    t = parts_tex(casing_tex('power_monitor_front'))
    els = legs(1, 1, 15, 16, h=1) + [
        box([1, 1, 0], [15, 16, 16], sides('side', north='front', up='top', down='bottom'), uv={'north': [1, 0, 15, 15]}),
        box([0, 14, -2], [16, 15, 0], STEEL6),
        box([0, 1, -1], [1, 14, 0], STEEL6), box([15, 1, -1], [16, 14, 0], STEEL6), box([1, 1, -1], [15, 2, 0], STEEL6),
        box([-1, 4, 3], [0, 5, 12], STEEL6), box([-1, 5, 3], [0, 10, 4], STEEL6), box([-1, 5, 11], [0, 10, 12], STEEL6),
        box([16, 4, 3], [17, 5, 12], STEEL6), box([16, 5, 3], [17, 10, 4], STEEL6), box([16, 5, 11], [17, 10, 12], STEEL6),
    ]
    model('power_monitor', t, els)


def multiblocks():
    """Pumpjack and refinery in the same language: a control cabinet with its own panel, copper lines, feet,
    railings. The pumpjack's beam, crank and rod keep moving through PumpjackRenderer while it pumps."""
    from modules.machines_industry import PJ, RF, static_model, pumpjack as _unused  # noqa: F401
    pj = PJ | PARTS | {'panel': b('machine_panel_buttons'), 'panel_glow': b('machine_panel_buttons_glow'),
                       'wheel': b('machine_panel_valve')}

    # main block: skid, A-frame, Samson post cap, plus the control cabinet beside the skid and its lead to the motor
    skid = [
        box([3, 0, -16], [5, 2, 32], 'steel'), box([11, 0, -16], [13, 2, 32], 'steel'),
        box([3, 0, -15], [13, 1, -13], 'steel'), box([3, 0, 29], [13, 1, 31], 'steel'), box([3, 0, 7], [13, 1, 9], 'steel'),
        box([2, 2, 3], [14, 4, 13], 'paint'),
    ]
    for x0 in (3, 11):
        for ang in (22.5, -22.5):
            leg = box([x0, 2, 7], [x0 + 2, 31, 9], 'lattice')
            leg['rotation'] = {'origin': [8, 31, 8], 'axis': 'x', 'angle': ang}
            skid.append(leg)
    skid.append(box([3, 14, 3], [13, 15, 13], 'steel'))
    cab = [box([14, 0, 9], [17, 8, 15], sides('steel', east='panel', up='steel', down='steel'), uv={'east': [5, 4, 11, 10]}),
           box([14, 8, 8.5], [17.5, 8.5, 15.5], 'steel'),
           box([15, 0, 15], [16, 1, 30], 'cable')]
    glow_el = lit_glow([17.01, 2, 9], [17.01, 8, 15], 'east', 'panel_glow', [5, 4, 11, 10])
    static_model('pumpjack', pj, skid + cab)
    static_model('pumpjack_on', pj, skid + cab + [glow_el])
    variants = {}
    for facing, y in ROT.items():
        for lit in ('false', 'true'):
            v = {'model': b('pumpjack' + ('_on' if lit == 'true' else ''))}
            if y:
                v['y'] = y
            variants[f'facing={facing},lit={lit}'] = v
    write('blockstates/pumpjack.json', {'variants': variants})
    static_model('pumpjack_item', pj, skid + cab + [box([6, 16, 6], [10, 20, 10], 'paint')])
    # wellhead: base flange on feet, Christmas tree with a red handwheel on top and a copper flowline
    static_model('pumpjack_well', pj, legs(3, 3, 13, 13, h=1) + [
        box([3, 1, 3], [13, 2, 13], 'steel'),
        box([6, 2, 6], [10, 9, 10], 'well'),
        box([4, 6, 7], [12, 8, 9], 'well'),
        box([7, 9, 7], [9, 11, 9], 'steel'),
        box([5, 11, 5], [11, 11.5, 11], {'up': 'wheel', 'down': 'wheel', 'north': 'steel', 'south': 'steel', 'east': 'steel', 'west': 'steel'},
            uv={'up': [2, 2, 14, 14], 'down': [2, 2, 14, 14]}),
        box([12, 6, 7], [16, 8, 9], 'pipe'), box([13, 5.5, 6.5], [14, 8.5, 9.5], 'steel'),
    ])
    # rear: motor on its own feet with a cable gland where the lead from the cabinet comes in
    static_model('pumpjack_rear', pj, [
        box([3, 0, 2], [13, 2, 14], 'steel'),
        box([5, 2, 4], [11, 10, 12], 'paint'),
        box([6, 2, 12], [10, 7, 16], 'motor'),
        box([11, 2, 5], [13, 5, 8], 'steel'), box([13, 2, 6], [15, 3, 7], 'cable'), box([14, 0, 6], [15, 2, 7], 'cable'),
    ])

    # refinery: fired base on feet behind a frame, dial control box, the column with copper draw-off lines and a
    # railing round its platform
    rf = RF | PARTS | {'panel': b('machine_panel_dial'), 'panel_glow': b('machine_panel_dial_glow')}
    base = legs(1, 1, 15, 15) + front_frame(1, 15, 2, 10) + [
        box([1, 2, 2], [15, 10, 15], sides('side', north='front', up='top', down='bottom'), uv={'north': [1, 5, 15, 13]}),
        box([1, 9, 1], [15, 10, 15], sides('steel', up='top')),
        box([2, 10, 2], [14, 16, 14], {f: 'column' for f in SIDES}),
        box([13, 10, 3], [15, 16, 5], 'steel'),
        box([14.5, 4, 9], [16, 16, 10.5], 'pipe'), box([14, 4, 9], [14.5, 5.5, 10.5], 'pipe'),
        box([-1, 3, 5], [1, 9, 11], sides('steel', west='panel'), uv={'west': [5, 4, 11, 10]}),
    ]
    on = [lit_glow([1, 2, 1.99], [15, 10, 1.99], 'north', 'glow', [1, 5, 15, 13]),
          lit_glow([-1.01, 3, 5], [-1.01, 9, 11], 'west', 'panel_glow', [5, 4, 11, 10])]
    static_model('refinery', rf, base)
    static_model('refinery_on', rf, base + on)
    ladder = box([5, 0, 14.5], [11, 16, 14.5], 'ladder', faces=('south', 'north'), uv={'south': [5, 0, 11, 16], 'north': [5, 0, 11, 16]})
    rail = [box([0, 1, 0], [1, 6, 1], STEEL6), box([15, 1, 0], [16, 6, 1], STEEL6),
            box([0, 1, 15], [1, 6, 16], STEEL6), box([15, 1, 15], [16, 6, 16], STEEL6),
            box([0, 5, 0], [16, 6, 1], STEEL6), box([0, 5, 1], [1, 6, 15], STEEL6), box([15, 5, 1], [16, 6, 15], STEEL6)]
    static_model('refinery_column', rf, [
        box([2, 0, 2], [14, 16, 14], {f: 'column' for f in SIDES}),
        box([0, 0, 0], [16, 1, 16], {'up': 'grating', 'down': 'grating', 'north': 'steel', 'south': 'steel', 'east': 'steel', 'west': 'steel'}),
        box([13, 1, 3], [15, 16, 5], 'steel'),
        box([14.5, 1, 9], [16, 16, 10.5], 'pipe'), box([14, 10, 9], [14.5, 11.5, 10.5], 'pipe'),
        ladder,
    ] + rail)
    static_model('refinery_top', rf, [
        box([2, 0, 2], [14, 8, 14], {f: 'column' for f in SIDES}),
        box([3, 8, 3], [13, 10, 13], {'north': 'column', 'south': 'column', 'east': 'column', 'west': 'column', 'up': 'ctop'}),
        box([7, 10, 7], [9, 16, 9], 'steel'),
        box([6, 15, 6], [10, 16, 10], 'steel'),
        box([13, 0, 3], [15, 6, 5], 'steel'),
        box([14.5, 0, 9], [16, 6, 10.5], 'pipe'), box([14, 4.5, 9], [14.5, 6, 10.5], 'pipe'),
        box([9, 13, 7.5], [12, 14, 8.5], 'pipe'),
    ])
