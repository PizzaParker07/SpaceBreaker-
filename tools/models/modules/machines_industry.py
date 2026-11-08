"""Industry machine models: generator, battery bank, crusher, arc furnace, press, fabricator, electrolyzer,
cryogenic liquefier, tanks, cables and pipes. Models face north; blockstates rotate them."""
from sbmodels.common import *  # noqa: F401,F403

ALL6 = ('north', 'south', 'east', 'west', 'up', 'down')
SIDES = ('north', 'south', 'east', 'west')
ROT = {'north': 0, 'east': 90, 'south': 180, 'west': 270}
GLOW_DATA = {'block_light': 15, 'sky_light': 15}


def box(fr, to, tex, faces=ALL6, uv=None):
    """Element with auto UVs taken from the element's own extent (like vanilla), tex = key or dict face->key."""
    x0, y0, z0 = fr
    x1, y1, z1 = to
    auto = {
        'north': [16 - x1, 16 - y1, 16 - x0, 16 - y0], 'south': [x0, 16 - y1, x1, 16 - y0],
        'east': [16 - z1, 16 - y1, 16 - z0, 16 - y0], 'west': [z0, 16 - y1, z1, 16 - y0],
        'up': [x0, z0, x1, z1], 'down': [x0, 16 - z1, x1, 16 - z0],
    }
    el = {'from': list(fr), 'to': list(to), 'faces': {}}
    for f in faces:
        key = tex.get(f) if isinstance(tex, dict) else tex
        if key is None:
            continue
        u = (uv or {}).get(f) or _wrap(auto[f])
        el['faces'][f] = {'texture': '#' + key, 'uv': u}
    return el


def _span(a, b):
    """Maps a texture span that leaves 0..16 back inside the texture, keeping its width (max 16)."""
    lo, hi = min(a, b), max(a, b)
    w = min(16, hi - lo)
    lo = lo % 16
    if lo + w > 16:
        lo = 16 - w
    return (lo, lo + w) if a <= b else (lo + w, lo)


def _wrap(u):
    u0, u1 = _span(u[0], u[2])
    v0, v1 = _span(u[1], u[3])
    return [u0, v0, u1, v1]


def glow(fr, to, face, key):
    el = box(fr, to, key, faces=(face,))
    el['neoforge_data'] = GLOW_DATA
    el['shade'] = False
    return el


def machine(name, textures, elements, glow_elements=None, item_elements=None, extra_state=None):
    """Writes <name>, <name>_on models and a facing/lit blockstate."""
    base = {'parent': 'minecraft:block/block', 'render_type': 'minecraft:cutout', 'textures': textures, 'elements': elements}
    write(f'models/block/{name}.json', base)
    on = dict(base)
    on['elements'] = elements + (glow_elements or [])
    write(f'models/block/{name}_on.json', on)
    variants = {}
    for facing, y in ROT.items():
        for lit in ('false', 'true'):
            v = {'model': b(name + ('_on' if lit == 'true' else ''))}
            if y:
                v['y'] = y
            variants[f'facing={facing},lit={lit}'] = v
    if extra_state is None:
        write(f'blockstates/{name}.json', {'variants': variants})
    write(f'models/item/{name}.json', {'parent': b(name)})


def casing_tex(front, top='machine_top', side='machine_casing', bottom='machine_bottom'):
    return {'front': b(front), 'top': b(top), 'side': b(side), 'bottom': b(bottom), 'particle': b(side)}


def body(y1=16, front='front', top='top'):
    return box([0, 0, 0], [16, y1, 16], {'north': front, 'south': 'side', 'east': 'side', 'west': 'side', 'up': top, 'down': 'bottom'})


def generate():
    # combustion generator: firebox body and a chimney
    t = casing_tex('combustion_generator_front')
    t['glow'] = b('combustion_generator_glow')
    machine('combustion_generator', t, [
        body(14),
        box([10, 14, 10], [14, 21, 14], 'side'),
        box([9, 21, 9], [15, 22, 15], 'side'),
    ], [glow([0, 0, -0.01], [16, 14, 0], 'north', 'glow')])

    # crusher: body with a hopper mouth on top
    t = casing_tex('crusher_front', top='crusher_top')
    machine('crusher', t, [
        body(13, top='side'),
        box([1, 13, 1], [15, 16, 15], {'north': 'side', 'south': 'side', 'east': 'side', 'west': 'side', 'up': 'top', 'down': 'bottom'}),
    ])

    # arc furnace: body, roof, three graphite electrodes with copper clamps
    t = casing_tex('arc_furnace_front', top='arc_furnace_top')
    t['electrode'] = b('arc_furnace_electrode')
    t['glow'] = b('arc_furnace_glow')
    els = [body(14, top='top')]
    for cx in (4, 8, 12):
        els.append(box([cx - 1, 14, 7], [cx + 1, 21, 9], 'electrode', uv={f: [0, 0, 2, 7] for f in SIDES} | {'up': [0, 2, 2, 4]}))
    els.append(box([1, 18, 6], [15, 19, 10], 'side'))
    machine('arc_furnace', t, els, [glow([0, 0, -0.01], [16, 14, 0], 'north', 'glow')])

    # press: base, four columns, crown; the ram is animated by a renderer while working
    t = casing_tex('press_front')
    t['steel'] = b('press_steel')
    t['die'] = b('press_die')
    els = [
        box([0, 0, 0], [16, 5, 16], {'north': 'front', 'south': 'side', 'east': 'side', 'west': 'side', 'up': 'die', 'down': 'bottom'},
            uv={'north': [0, 11, 16, 16], 'south': [0, 11, 16, 16], 'east': [0, 11, 16, 16], 'west': [0, 11, 16, 16]}),
        box([0, 13, 0], [16, 16, 16], {'north': 'steel', 'south': 'steel', 'east': 'steel', 'west': 'steel', 'up': 'top', 'down': 'steel'}),
    ]
    for x, z in ((1, 1), (13, 1), (1, 13), (13, 13)):
        els.append(box([x, 5, z], [x + 2, 13, z + 2], 'steel'))
    machine('press', t, els)
    write('models/block/press_ram.json', {'parent': 'minecraft:block/block', 'render_type': 'minecraft:cutout',
                                          'textures': {'steel': b('press_steel'), 'particle': b('press_steel')},
                                          'elements': [box([4, 9, 4], [12, 13, 12], 'steel'), box([6, 8, 6], [10, 9, 10], 'steel')]})

    # fabricator: cabinet, work table and a small gantry with a tool head
    t = casing_tex('fabricator_front', top='fabricator_top')
    t['glow'] = b('fabricator_glow')
    t['steel'] = b('press_steel')
    machine('fabricator', t, [
        body(12, top='top'),
        box([0, 12, 0], [16, 13, 16], {'north': 'steel', 'south': 'steel', 'east': 'steel', 'west': 'steel', 'up': 'top', 'down': 'bottom'}),
        box([0, 13, 14], [2, 18, 16], 'steel'), box([14, 13, 14], [16, 18, 16], 'steel'),
        box([0, 18, 13], [16, 20, 16], 'steel'),
        box([6, 15, 12], [10, 19, 14], 'steel'), box([7, 14, 12], [9, 15, 13], 'steel'),
    ], [glow([0, 0, -0.01], [16, 12, 0], 'north', 'glow')])

    # electrolyzer: plinth, two glass cells, manifold, rear panel
    t = casing_tex('machine_casing')
    t['cell'] = b('electrolyzer_cell')
    t['cell_on'] = b('electrolyzer_cell_on')
    t['steel'] = b('press_steel')

    def electrolyzer(cell):
        return [
            box([0, 0, 0], [16, 3, 16], {'north': 'side', 'south': 'side', 'east': 'side', 'west': 'side', 'up': 'bottom', 'down': 'bottom'}),
            box([1, 3, 3], [7, 13, 9], cell), box([9, 3, 3], [15, 13, 9], cell),
            box([0, 3, 10], [16, 13, 16], {'north': 'side', 'south': 'side', 'east': 'side', 'west': 'side'}),
            box([0, 13, 1], [16, 16, 16], {'north': 'steel', 'south': 'side', 'east': 'side', 'west': 'side', 'up': 'top', 'down': 'steel'}),
            box([3, 13, 5], [5, 16, 7], 'steel'),
        ]
    write('models/block/electrolyzer.json', {'parent': 'minecraft:block/block', 'render_type': 'minecraft:cutout', 'textures': t, 'elements': electrolyzer('cell')})
    write('models/block/electrolyzer_on.json', {'parent': 'minecraft:block/block', 'render_type': 'minecraft:cutout', 'textures': t, 'elements': electrolyzer('cell_on')})
    variants = {}
    for facing, y in ROT.items():
        for lit in ('false', 'true'):
            v = {'model': b('electrolyzer' + ('_on' if lit == 'true' else ''))}
            if y:
                v['y'] = y
            variants[f'facing={facing},lit={lit}'] = v
    write('blockstates/electrolyzer.json', {'variants': variants})
    write('models/item/electrolyzer.json', {'parent': b('electrolyzer')})

    # cryogenic liquefier: frosted body, compressor fan on top, fins on the sides
    t = casing_tex('cryogenic_liquefier_front', top='cryogenic_liquefier_top', side='machine_fins')
    machine('cryogenic_liquefier', t, [
        body(13, top='side'),
        box([1, 13, 1], [15, 16, 15], {'north': 'bottom', 'south': 'bottom', 'east': 'bottom', 'west': 'bottom', 'up': 'top'}),
    ])

    # battery bank: blockstate with charge level overlays
    t = casing_tex('battery_bank_front', side='battery_bank_side')
    for lvl in range(1, 5):
        t[f'c{lvl}'] = b(f'battery_bank_charge_{lvl}')
    base = [body(16)]
    write('models/block/battery_bank.json', {'parent': 'minecraft:block/block', 'render_type': 'minecraft:cutout', 'textures': t, 'elements': base})
    for lvl in range(1, 5):
        write(f'models/block/battery_bank_{lvl}.json', {'parent': 'minecraft:block/block', 'render_type': 'minecraft:cutout', 'textures': t,
                                                        'elements': base + [glow([0, 0, -0.01], [16, 16, 0], 'north', f'c{lvl}')]})
    variants = {}
    for facing, y in ROT.items():
        for lit in ('false', 'true'):
            for lvl in range(5):
                for down in ('false', 'true'):
                    v = {'model': b('battery_bank' + (f'_{lvl}' if lvl else ''))}
                    if down == 'true':
                        v['x'] = 90  # ceiling-mounted: the front (output) faces down
                    if y:
                        v['y'] = y
                    variants[f'charge={lvl},facing={facing},lit={lit},output_down={down}'] = v
    write('blockstates/battery_bank.json', {'variants': variants})
    write('models/item/battery_bank.json', {'parent': b('battery_bank_4')})

    tanks()
    conduits()


def tanks():
    for name in ('fluid_tank', 'pressurized_tank', 'cryogenic_tank'):
        t = {'side': b(f'{name}_side'), 'top': b(f'{name}_top'), 'particle': b(f'{name}_side'), 'steel': b('press_steel')}
        els = [box([1, 0, 1], [15, 16, 15], {'north': 'side', 'south': 'side', 'east': 'side', 'west': 'side', 'up': 'top', 'down': 'top'})]
        for x, z in ((0, 0), (14, 0), (0, 14), (14, 14)):
            els.append(box([x, 0, z], [x + 2, 2, z + 2], 'steel'))
        write(f'models/block/{name}.json', {'parent': 'minecraft:block/block', 'render_type': 'minecraft:cutout', 'textures': t, 'elements': els})
        write(f'blockstates/{name}.json', {'variants': {'': {'model': b(name)}}})
        write(f'models/item/{name}.json', {'parent': b(name)})


CONDUITS = {'copper_cable': (2, 'copper_cable_end', False), 'hv_cable': (3, 'hv_cable_end', False),
            'fluid_pipe': (2, 'fluid_pipe_flange', True), 'gas_pipe': (2, 'gas_pipe_flange', True),
            'item_duct': (3, 'item_duct_end', True)}
DIRS = {'north': (0, 0, -1), 'south': (0, 0, 1), 'east': (1, 0, 0), 'west': (-1, 0, 0), 'up': (0, 1, 0), 'down': (0, -1, 0)}


def arm(r, d, extend=0):
    lo, hi = 8 - r, 8 + r
    dx, dy, dz = DIRS[d]
    fr, to = [lo, lo, lo], [hi, hi, hi]
    for axis, step in enumerate((dx, dy, dz)):
        if step < 0:
            fr[axis], to[axis] = 0 - extend, lo
        elif step > 0:
            fr[axis], to[axis] = hi, 16 + extend
    return fr, to


def conduits():
    for name, (r, end, pipe) in CONDUITS.items():
        t = {'body': b(name), 'end': b(end), 'particle': b(name), 'valve': b('pipe_valve')}
        lo, hi = 8 - r, 8 + r
        core = [box([lo, lo, lo], [hi, hi, hi], 'body')]
        write(f'models/block/{name}_core.json', {'parent': 'minecraft:block/block', 'textures': t, 'elements': core})
        parts = [{'apply': {'model': b(f'{name}_core')}}]
        for d in DIRS:
            fr, to = arm(r, d)
            faces = tuple(f for f in ALL6 if f != {'north': 'south', 'south': 'north', 'east': 'west', 'west': 'east', 'up': 'down', 'down': 'up'}[d])
            el = box(fr, to, {f: ('end' if f == d else 'body') for f in ALL6}, faces=faces)
            els = [el]
            if pipe:   # flange ring at the block edge
                ffr, fto = arm(r + 1, d)
                for axis, step in enumerate(DIRS[d]):
                    if step < 0:
                        ffr[axis], fto[axis] = 0, 2
                    elif step > 0:
                        ffr[axis], fto[axis] = 14, 16
                els.append(box(ffr, fto, 'end'))
            write(f'models/block/{name}_{d}.json', {'parent': 'minecraft:block/block', 'textures': t, 'elements': els})
            parts.append({'when': {'OR': [{d: 'connected'}, {d: 'extract'}]}, 'apply': {'model': b(f'{name}_{d}')}})
            if pipe:
                vfr, vto = arm(r + 2, d)
                for axis, step in enumerate(DIRS[d]):
                    if step < 0:
                        vfr[axis], vto[axis] = 2, 4
                    elif step > 0:
                        vfr[axis], vto[axis] = 12, 14
                write(f'models/block/{name}_{d}_extract.json', {'parent': 'minecraft:block/block', 'textures': t,
                                                               'elements': [box(vfr, vto, 'valve')]})
                parts.append({'when': {d: 'extract'}, 'apply': {'model': b(f'{name}_{d}_extract')}})
        write(f'blockstates/{name}.json', {'multipart': parts})
        item_els = [box([lo, lo, 0], [hi, hi, 16], {'north': 'end', 'south': 'end', 'east': 'body', 'west': 'body', 'up': 'body', 'down': 'body'})]
        write(f'models/item/{name}.json', {'parent': 'minecraft:block/block', 'textures': t, 'elements': item_els,
                                           'display': {'gui': {'rotation': [30, 45, 0], 'scale': [0.8, 0.8, 0.8]},
                                                       'fixed': {'scale': [0.6, 0.6, 0.6]}}})


def static_model(name, textures, elements, render='minecraft:cutout', ao=True):
    m = {'parent': 'minecraft:block/block', 'render_type': render, 'textures': textures, 'elements': elements}
    if not ao:
        m['ambientocclusion'] = False
    write(f'models/block/{name}.json', m)


PJ = {'paint': b('pumpjack_paint'), 'lattice': b('pumpjack_lattice'), 'weight': b('pumpjack_weight'),
      'motor': b('pumpjack_motor'), 'well': b('pumpjack_wellhead'), 'steel': b('press_steel'), 'particle': b('pumpjack_paint')}


def pumpjack():
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
    static_model('pumpjack', PJ, skid)
    static_model('pumpjack_on', PJ, skid)
    variants = {}
    for facing, y in ROT.items():
        for lit in ('false', 'true'):
            v = {'model': b('pumpjack')}
            if y:
                v['y'] = y
            variants[f'facing={facing},lit={lit}'] = v
    write('blockstates/pumpjack.json', {'variants': variants})
    write('models/item/pumpjack.json', {'parent': b('pumpjack_item')})
    static_model('pumpjack_item', PJ, skid + [box([6, 16, 6], [10, 20, 10], 'paint')])
    # parts
    static_model('pumpjack_post', PJ, [
        box([3, 13, 6], [13, 16, 10], 'paint'),
        box([5, 12, 7], [11, 13, 9], 'steel'),
    ])
    static_model('pumpjack_well', PJ, [
        box([3, 0, 3], [13, 2, 13], 'steel'),
        box([6, 2, 6], [10, 9, 10], 'well'),
        box([4, 6, 7], [12, 8, 9], 'well'),
        box([7, 9, 7], [9, 11, 9], 'steel'),
        box([12, 6, 7], [16, 8, 9], 'steel'),
    ])
    static_model('pumpjack_rear', PJ, [
        box([3, 0, 2], [13, 2, 14], 'steel'),
        box([5, 2, 4], [11, 10, 12], {'north': 'paint', 'south': 'paint', 'east': 'paint', 'west': 'paint', 'up': 'paint', 'down': 'paint'}),
        box([6, 2, 12], [10, 7, 16], 'motor'),
    ])
    write('models/block/empty_part.json', {'textures': {'particle': b('pumpjack_paint')}})
    # moving parts, pivots at model (8, 8, 8)
    static_model('pumpjack_beam', PJ, [
        box([6, 6, -2], [10, 10, 30], 'paint'),
        box([5, -6, -8], [11, 12, -2], 'paint'),
        box([4, 4, 28], [12, 8, 32], 'paint'),
        box([5, 5, 6], [11, 11, 10], 'steel'),
    ], ao=False)
    static_model('pumpjack_crank', PJ, [
        box([2, 7, 7], [14, 9, 9], 'steel'),
        box([2, 6, 6], [4, 15, 10], 'weight'), box([12, 6, 6], [14, 15, 10], 'weight'),
        box([1, -2, 4], [4, 6, 12], 'weight'), box([12, -2, 4], [15, 6, 12], 'weight'),
    ], ao=False)
    static_model('pumpjack_rod', PJ, [box([7, 0, 7], [9, 16, 9], 'steel')], ao=False)


RF = {'front': b('refinery_front'), 'side': b('machine_casing'), 'top': b('machine_top'), 'bottom': b('machine_bottom'),
      'column': b('refinery_column'), 'ctop': b('refinery_column_top'), 'grating': b('refinery_grating'),
      'ladder': 'minecraft:block/ladder', 'steel': b('press_steel'), 'glow': b('refinery_glow'), 'particle': b('refinery_column')}


def refinery():
    base = [
        box([0, 0, 0], [16, 10, 16], {'north': 'front', 'south': 'side', 'east': 'side', 'west': 'side', 'up': 'top', 'down': 'bottom'},
            uv={'north': [0, 6, 16, 16], 'south': [0, 6, 16, 16], 'east': [0, 6, 16, 16], 'west': [0, 6, 16, 16]}),
        box([2, 10, 2], [14, 16, 14], {'north': 'column', 'south': 'column', 'east': 'column', 'west': 'column'}),
        box([13, 10, 3], [15, 16, 5], 'steel'),
    ]
    static_model('refinery', RF, base)
    static_model('refinery_on', RF, base + [glow([0, 0, -0.01], [16, 10, 0], 'north', 'glow')])
    for name in ('refinery', 'refinery_on'):
        pass
    variants = {}
    for facing, y in ROT.items():
        for lit in ('false', 'true'):
            v = {'model': b('refinery' + ('_on' if lit == 'true' else ''))}
            if y:
                v['y'] = y
            variants[f'facing={facing},lit={lit}'] = v
    write('blockstates/refinery.json', {'variants': variants})
    write('models/item/refinery.json', {'parent': b('refinery')})
    ladder = box([5, 0, 14.5], [11, 16, 14.5], 'ladder', faces=('south', 'north'), uv={'south': [5, 0, 11, 16], 'north': [5, 0, 11, 16]})
    static_model('refinery_column', RF, [
        box([2, 0, 2], [14, 16, 14], {'north': 'column', 'south': 'column', 'east': 'column', 'west': 'column'}),
        box([0, 0, 0], [16, 1, 16], {'up': 'grating', 'down': 'grating', 'north': 'steel', 'south': 'steel', 'east': 'steel', 'west': 'steel'}),
        box([13, 1, 3], [15, 16, 5], 'steel'),
        ladder,
    ])
    static_model('refinery_top', RF, [
        box([2, 0, 2], [14, 8, 14], {'north': 'column', 'south': 'column', 'east': 'column', 'west': 'column'}),
        box([3, 8, 3], [13, 10, 13], {'north': 'column', 'south': 'column', 'east': 'column', 'west': 'column', 'up': 'ctop'}),
        box([7, 10, 7], [9, 16, 9], 'steel'),
        box([6, 15, 6], [10, 16, 10], 'steel'),
        box([13, 0, 3], [15, 6, 5], 'steel'),
    ])


def parts():
    shapes = {'pumpjack_well': 'pumpjack_well', 'pumpjack_rear': 'pumpjack_rear', 'pumpjack_post': 'pumpjack_post',
              'pumpjack_beam': 'empty_part', 'pumpjack_head': 'empty_part',
              'refinery_column': 'refinery_column', 'refinery_top': 'refinery_top'}
    variants = {}
    for shape, model in shapes.items():
        for facing, y in ROT.items():
            for lit in ('false', 'true'):
                v = {'model': b(model)}
                if y:
                    v['y'] = y
                variants[f'facing={facing},shape={shape}'] = v
    write('blockstates/machine_part.json', {'variants': variants})


_gen_base = generate


def generate():  # noqa: F811
    _gen_base()
    pumpjack()
    refinery()
    parts()


def power_monitor():
    t = casing_tex('power_monitor_front')
    write('models/block/power_monitor.json', {'parent': 'minecraft:block/orientable', 'textures': {
        'front': t['front'], 'side': t['side'], 'top': t['top'], 'particle': t['side']}})
    write('blockstates/power_monitor.json', {'variants': {
        f'facing={f}': ({'model': b('power_monitor'), 'y': r} if r else {'model': b('power_monitor')})
        for f, r in (('north', 0), ('east', 90), ('south', 180), ('west', 270))}})
    write('models/item/power_monitor.json', {'parent': b('power_monitor')})


_generate_monitor = generate


def generate():  # noqa: F811
    _generate_monitor()
    power_monitor()
