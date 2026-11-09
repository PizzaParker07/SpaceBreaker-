"""EVA suit and life support items."""
from sbmodels.common import *  # noqa: F401,F403


# ----------------------------------------------------------------------------- oxygen tank (3D in hand, 2D in GUI)

def tank_model(name, top):
    """Chunky whole-pixel bottle: two crossing boxes make a rounded body, then collar, valve,
    hand wheel and a pressure gauge. ``top`` is the height of the body (12 small, 13 large)."""
    sides = ('north', 'south', 'east', 'west')
    body = {s: [0, 0, 6, 11] for s in sides} | {'up': [6, 0, 12, 6], 'down': [6, 0, 12, 6]}
    body_narrow = {s: [1, 0, 5, 11] for s in sides} | {'up': [6, 0, 12, 6], 'down': [6, 0, 12, 6]}
    steel = {s: [12, 0, 16, 1] for s in sides} | {'up': [12, 0, 16, 4], 'down': [12, 0, 16, 4]}
    valve = {s: [12, 4, 14, 5] for s in sides} | {'up': [12, 4, 14, 6], 'down': [12, 4, 14, 6]}
    wheel = {s: [12, 6, 16, 7] for s in sides} | {'up': [12, 6, 16, 8], 'down': [12, 6, 16, 8]}
    t = top
    els = [
        cube([5, 0, 6], [11, t, 10], 'tex', per_face={'north': body['north'], 'south': body['south'], 'east': body_narrow['east'],
                                                      'west': body_narrow['west'], 'up': body['up'], 'down': body['down']}),
        cube([6, 0, 5], [10, t, 11], 'tex', per_face={'north': body_narrow['north'], 'south': body_narrow['south'], 'east': body['east'],
                                                      'west': body['west'], 'up': body['up'], 'down': body['down']}),
        cube([6, t, 6], [10, t + 1, 10], 'tex', per_face=steel),
        cube([7, t + 1, 7], [9, t + 2, 9], 'tex', per_face=valve),
        cube([6, t + 2, 7], [10, t + 3, 9], 'tex', per_face=wheel),
        cube([7, t + 2, 6], [9, t + 3, 10], 'tex', per_face=wheel),
        # pressure gauge on the east side of the valve
        cube([9, t + 1, 7], [11, t + 3, 9], 'tex', per_face={'east': [6, 6, 8, 8], 'north': [8, 6, 10, 8], 'south': [8, 6, 10, 8],
                                                             'up': [8, 6, 10, 8], 'down': [8, 6, 10, 8], 'west': [8, 6, 10, 8]}),
    ]
    write(f'models/item/{name}_3d.json', {
        'parent': 'minecraft:block/block',
        'textures': {'tex': i('oxygen_tank_model'), 'particle': i('oxygen_tank_model')},
        'elements': els,
        'display': {
            'thirdperson_righthand': {'rotation': [75, 0, 0], 'translation': [0, 2.5, 1.5], 'scale': [0.4, 0.4, 0.4]},
            'thirdperson_lefthand': {'rotation': [75, 0, 0], 'translation': [0, 2.5, 1.5], 'scale': [0.4, 0.4, 0.4]},
            # about the size of a held lantern: small and low in the corner like vanilla items
            'firstperson_righthand': {'rotation': [0, 45, 0], 'translation': [0, 0.5, 0], 'scale': [0.26, 0.26, 0.26]},
            'firstperson_lefthand': {'rotation': [0, 225, 0], 'translation': [0, 0.5, 0], 'scale': [0.26, 0.26, 0.26]},
            'ground': {'rotation': [0, 0, 0], 'translation': [0, 2, 0], 'scale': [0.5, 0.5, 0.5]},
            'fixed': {'rotation': [0, 0, 0], 'translation': [0, -1, 0], 'scale': [0.8, 0.8, 0.8]},
            'head': {'rotation': [0, 0, 0], 'translation': [0, 13, 7], 'scale': [1, 1, 1]},
        },
    })
    write(f'models/item/{name}_icon.json', {'parent': 'minecraft:item/generated', 'textures': {'layer0': i(name)}})
    write(f'models/item/{name}.json', {
        'loader': 'neoforge:separate_transforms',
        'gui_light': 'front',
        'base': {'parent': i(f'{name}_3d')},
        'perspectives': {'gui': {'parent': i(f'{name}_icon')}},
    })


def _sw(col, row, w=4, h=4, du=0):
    """UV of a patch on the drill swatch sheet (4x4 patches, see textures electric_drill_model)."""
    return [col * 4 + du, row * 4, col * 4 + du + w, row * 4 + h]


def _faces(side, top=None, bottom=None, front=None, back=None, east=None, west=None):
    top = top or side
    return {'north': front or side, 'south': back or side, 'east': east or side, 'west': west or side,
            'up': top, 'down': bottom or top}


DRILL_DISPLAY = {
    'thirdperson_righthand': {'rotation': [90, -90, 0], 'translation': [0, 1, 2], 'scale': [0.55, 0.55, 0.55]},
    'thirdperson_lefthand': {'rotation': [90, -90, 0], 'translation': [0, 1, 2], 'scale': [0.55, 0.55, 0.55]},
    'firstperson_righthand': {'rotation': [0, 0, 0], 'translation': [1.5, 3, -2], 'scale': [0.42, 0.42, 0.42]},
    'firstperson_lefthand': {'rotation': [0, 0, 0], 'translation': [1.5, 3, -2], 'scale': [0.42, 0.42, 0.42]},
    'gui': {'rotation': [25, -135, 0], 'translation': [0, -1, 0], 'scale': [0.75, 0.75, 0.75]},
    'ground': {'rotation': [0, 0, 0], 'translation': [0, 2, 0], 'scale': [0.45, 0.45, 0.45]},
    'fixed': {'rotation': [0, 90, 0], 'translation': [0, 0, 0], 'scale': [0.8, 0.8, 0.8]},
}
DRILL_SPIN = (0, 22.5, 45, -22.5)


def electric_drill():
    """Pistol-grip electric drill, nose towards -z: white motor housing with a cyan band, ribbed rubber grip and
    trigger, battery pack under the grip, steel chuck and a fluted bit (or the wide-bore titanium crown). Four spin
    frames per bit turn the bit or crown in 22.5 degree steps and shift its flutes, picked by the spin property."""
    def els(frame, wide):
        rot = {'origin': [8, 10, 0], 'axis': 'z', 'angle': DRILL_SPIN[frame]} if DRILL_SPIN[frame] else None
        e = [
            cube([6, 8, 4], [10, 12, 13], 'tex', per_face=_faces(_sw(1, 0), top=_sw(0, 0), bottom=_sw(2, 0), back=_sw(3, 0))),  # motor housing
            cube([6.5, 2, 9], [9.5, 8, 12], 'tex', per_face=_faces(_sw(0, 1)),
                 rot={'origin': [8, 8, 10.5], 'axis': 'x', 'angle': -22.5}),                                                       # grip
            cube([7.5, 6, 7.5], [8.5, 7.5, 9], 'tex', per_face=_faces(_sw(3, 1, 1, 2))),                                          # trigger
            cube([5.5, 0, 8], [10.5, 2.5, 14], 'tex', per_face=_faces(_sw(1, 1), top=_sw(2, 1))),                                 # battery
            cube([6.5, 8.5, 2], [9.5, 11.5, 4], 'tex', per_face=_faces(_sw(0, 2), front=_sw(1, 2))),                             # chuck
        ]
        flutes = _sw(2, 2, 2, 4, du=frame % 2)
        if wide:
            e.append(cube([7.5, 9.5, -3], [8.5, 10.5, 2], 'tex', per_face=_faces(flutes, front=_sw(3, 2, 1, 1)), rot=rot))      # short pilot bit
            e.append(cube([5, 7, 0], [11, 13, 2], 'tex', per_face=_faces(_sw(0, 3), front=_sw(1, 3)), rot=rot))                # titanium crown
        else:
            e.append(cube([7.5, 9.5, -4], [8.5, 10.5, 2], 'tex', per_face=_faces(flutes, front=_sw(3, 2, 1, 1)), rot=rot))      # bit
        return e

    tex = {'tex': i('electric_drill_model'), 'particle': i('electric_drill_model')}
    for wide in (False, True):
        for frame in range(4):
            name = 'electric_drill' + ('_wide' if wide else '') + (f'_spin{frame}' if frame else '')
            if name == 'electric_drill':
                continue
            write(f'models/item/{name}.json', {'parent': 'minecraft:block/block', 'textures': tex, 'elements': els(frame, wide),
                                                'display': DRILL_DISPLAY})
    overrides = [{'predicate': {'spacebreaker:spin': f / 4}, 'model': i(f'electric_drill_spin{f}')} for f in (1, 2, 3)]
    overrides.append({'predicate': {'spacebreaker:wide_bore': 1}, 'model': i('electric_drill_wide')})
    overrides += [{'predicate': {'spacebreaker:wide_bore': 1, 'spacebreaker:spin': f / 4}, 'model': i(f'electric_drill_wide_spin{f}')}
                  for f in (1, 2, 3)]
    write('models/item/electric_drill.json', {'parent': 'minecraft:block/block', 'textures': tex,
                                              'elements': els(0, False), 'display': DRILL_DISPLAY, 'overrides': overrides})


def oxygen_tank():
    tank_model('oxygen_tank', 11)
    tank_model('large_oxygen_tank', 13)


def generate():
    # suit pieces: base icon + grey accent layer tinted with the dye colour (tint index 1)
    pieces = ['magnetic_boots'] + [f'{t}_{p}' for t in ('eva', 'thermal', 'heated', 'radiation')
                                   for p in ('helmet', 'chestplate', 'leggings', 'boots')]
    for name in pieces:
        write(f'models/item/{name}.json', {'parent': 'minecraft:item/generated',
                                            'textures': {'layer0': i(name), 'layer1': i(name + '_accent')}})
    for name in ('maneuvering_unit', 'safety_tether', 'environment_sensor', 'wide_bore_drill_head', 'suit_upgrade_template', 'thermal_regulator_module', 'radiation_shield_module',
                 'extra_oxygen_module', 'jetpack_booster_module'):
        generated_item(name)
    electric_drill()
    for tool in ('pickaxe', 'axe', 'shovel', 'hoe', 'sword'):
        write(f'models/item/titanium_{tool}.json', {'parent': 'minecraft:item/handheld', 'textures': {'layer0': i(f'titanium_{tool}')}})
    oxygen_tank()
    oxygen_filler()
    suit_station()
    oxygen_distributor()
    airlock_door()


# ----------------------------------------------------------------------------- life support blocks

SIDES = ('north', 'south', 'east', 'west')
FACING_Y = {'north': 0, 'east': 90, 'south': 180, 'west': 270}
MACHINE_DISPLAY = {
    'gui': {'rotation': [30, 225, 0], 'translation': [0, 0, 0], 'scale': [0.625, 0.625, 0.625]},
    'ground': {'rotation': [0, 0, 0], 'translation': [0, 3, 0], 'scale': [0.25, 0.25, 0.25]},
    'fixed': {'rotation': [0, 0, 0], 'translation': [0, 0, 0], 'scale': [0.5, 0.5, 0.5]},
    'thirdperson_righthand': {'rotation': [75, 45, 0], 'translation': [0, 2.5, 0], 'scale': [0.375, 0.375, 0.375]},
    'firstperson_righthand': {'rotation': [0, 45, 0], 'translation': [0, 0, 0], 'scale': [0.4, 0.4, 0.4]},
    'firstperson_lefthand': {'rotation': [0, 225, 0], 'translation': [0, 0, 0], 'scale': [0.4, 0.4, 0.4]},
}


def uv_box(fr, to):
    """Per-face UVs that take the matching window of a 16x16 texture (like vanilla auto-UV)."""
    x0, y0, z0 = fr
    x1, y1, z1 = to
    return {
        'north': [16 - x1, 16 - y1, 16 - x0, 16 - y0], 'south': [x0, 16 - y1, x1, 16 - y0],
        'west': [z0, 16 - y1, z1, 16 - y0], 'east': [16 - z1, 16 - y1, 16 - z0, 16 - y0],
        'up': [x0, z0, x1, z1], 'down': [x0, 16 - z1, x1, 16 - z0],
    }


def machine_states(name, model_off, model_on):
    variants = {}
    for facing, y in FACING_Y.items():
        for lit in ('false', 'true'):
            v = {'model': b(model_on if lit == 'true' else model_off)}
            if y:
                v['y'] = y
            variants[f'facing={facing},lit={lit}'] = v
    write(f'blockstates/{name}.json', {'variants': variants})


def oxygen_filler():
    """Plinth, a low casing set back with a small gauge on its front, and a tank cradle out in front of it."""
    for suffix, front in (('', 'oxygen_filler_front'), ('_on', 'oxygen_filler_front_on')):
        tex = {'casing': b('life_support_casing'), 'plinth': b('life_support_plinth'),
               'steel': b('life_support_top'), 'ring': b('life_support_ring'), 'front': b(front), 'particle': b('life_support_casing')}
        els = [
            cube([1, 0, 1], [15, 2, 15], {'up': 'steel', 'down': 'steel', **{s: 'plinth' for s in SIDES}}, per_face=uv_box([1, 0, 1], [15, 2, 15]) | {s: [1, 14, 15, 16] for s in SIDES}),
            cube([2, 2, 6], [14, 11, 14], {'north': 'front', 'south': 'casing', 'east': 'casing', 'west': 'casing', 'up': 'steel', 'down': 'steel'},
                 per_face=uv_box([2, 2, 6], [14, 11, 14]) | {'up': [2, 2, 14, 10]}),
            cube([1, 11, 5], [15, 12, 15], {'up': 'steel', 'down': 'steel', **{s: 'plinth' for s in SIDES}},
                 per_face={'up': [1, 1, 15, 11], 'down': [1, 1, 15, 11], **{s: [1, 0, 15, 1] for s in SIDES}}),
            # tank cradle in front: a steel shelf, two blue arms and a low front lip
            cube([3, 2, 1], [13, 3, 6], {'up': 'steel', 'down': 'steel', **{s: 'plinth' for s in SIDES}},
                 per_face=uv_box([3, 2, 1], [13, 3, 6]) | {s: [3, 0, 13, 1] for s in SIDES}),
            cube([3, 3, 2], [4, 7, 6], 'ring', per_face=uv_box([3, 3, 2], [4, 7, 6])),
            cube([12, 3, 2], [13, 7, 6], 'ring', per_face=uv_box([12, 3, 2], [13, 7, 6])),
            cube([4, 3, 1], [12, 4, 2], 'ring', per_face=uv_box([4, 3, 1], [12, 4, 2])),
        ]
        write(f'models/block/oxygen_filler{suffix}.json', {'parent': 'minecraft:block/block', 'render_type': 'minecraft:cutout',
                                                            'textures': tex, 'elements': els})
    machine_states('oxygen_filler', 'oxygen_filler', 'oxygen_filler_on')
    write('models/item/oxygen_filler.json', {'parent': b('oxygen_filler'), 'display': MACHINE_DISPLAY})


def suit_station():
    """Full block: plinth, casing with the service front, standing grate on top, a hose reel on the east side."""
    for suffix, front in (('', 'suit_station_front'), ('_on', 'suit_station_front_on')):
        tex = {'casing': b('life_support_casing'), 'plinth': b('life_support_plinth'), 'top': b('suit_station_top'),
               'steel': b('life_support_top'), 'ring': b('life_support_ring'), 'front': b(front), 'particle': b('life_support_casing')}
        els = [
            cube([0, 0, 0], [16, 2, 16], {'up': 'steel', 'down': 'steel', **{s: 'plinth' for s in SIDES}}, per_face=uv_box([0, 0, 0], [16, 2, 16]) | {s: [0, 14, 16, 16] for s in SIDES}),
            cube([1, 2, 1], [15, 15, 15], {'north': 'front', 'south': 'casing', 'east': 'casing', 'west': 'casing', 'up': 'steel', 'down': 'steel'},
                 per_face={'north': [1, 1, 15, 14], 'south': [1, 1, 15, 14], 'east': [1, 1, 15, 14], 'west': [1, 1, 15, 14], 'up': [1, 1, 15, 15], 'down': [1, 1, 15, 15]}),
            cube([0, 15, 0], [16, 16, 16], {'up': 'top', 'down': 'steel', **{s: 'plinth' for s in SIDES}}, per_face={'up': [0, 0, 16, 16], 'down': [0, 0, 16, 16], **{s: [0, 0, 16, 1] for s in SIDES}}),
            # hose reel on the east side
            cube([15, 5, 5], [16, 11, 11], {'east': 'ring', **{s: 'steel' for s in ('north', 'south', 'west')}, 'up': 'steel', 'down': 'steel'},
                 per_face={'east': [5, 5, 11, 11], 'north': [0, 0, 1, 6], 'south': [0, 0, 1, 6], 'west': [0, 0, 6, 6], 'up': [0, 0, 1, 6], 'down': [0, 0, 1, 6]}),
        ]
        write(f'models/block/suit_station{suffix}.json', {'parent': 'minecraft:block/block', 'render_type': 'minecraft:cutout',
                                                           'textures': tex, 'elements': els})
    machine_states('suit_station', 'suit_station', 'suit_station_on')
    write('models/item/suit_station.json', {'parent': b('suit_station'), 'display': MACHINE_DISPLAY})


def oxygen_distributor():
    for suffix, front in (('', 'oxygen_distributor_front'), ('_on', 'oxygen_distributor_front_on')):
        tex = {'casing': b('life_support_casing'), 'plinth': b('life_support_plinth'),
               'steel': b('life_support_top'), 'ring': b('life_support_ring'), 'front': b(front), 'particle': b('life_support_casing')}
        els = [
            cube([1, 0, 1], [15, 2, 15], {'up': 'steel', 'down': 'steel', **{s: 'plinth' for s in SIDES}}, per_face=uv_box([1, 0, 1], [15, 2, 15]) | {s: [1, 14, 15, 16] for s in SIDES}),
            cube([2, 2, 2], [14, 11, 14], {'north': 'front', 'south': 'casing', 'east': 'casing', 'west': 'casing', 'up': 'steel', 'down': 'steel'},
                 per_face={s: [2, 3, 14, 12] for s in SIDES} | {'up': [2, 2, 14, 14], 'down': [2, 2, 14, 14]}),
            cube([3, 11, 3], [13, 13, 13], {'up': 'steel', 'down': 'steel', **{s: 'plinth' for s in SIDES}},
                 per_face={'up': [3, 3, 13, 13], 'down': [3, 3, 13, 13], **{s: [3, 0, 13, 2] for s in SIDES}}),
            # small air outlet on top
            cube([6, 13, 6], [10, 15, 10], {'up': 'plinth', 'down': 'steel', **{s: 'plinth' for s in SIDES}},
                 per_face={'up': [6, 14, 10, 16], 'down': [6, 6, 10, 10], **{s: [6, 0, 10, 2] for s in SIDES}}),
            # tank port on the south side
            cube([6, 5, 14], [10, 9, 15], {'south': 'ring', **{s: 'steel' for s in ('north', 'east', 'west')}, 'up': 'steel', 'down': 'steel'},
                 per_face={'south': [6, 6, 10, 10], 'north': [6, 6, 10, 10], 'east': [0, 0, 1, 4], 'west': [0, 0, 1, 4], 'up': [6, 0, 10, 1], 'down': [6, 0, 10, 1]}),
        ]
        write(f'models/block/oxygen_distributor{suffix}.json', {'parent': 'minecraft:block/block', 'render_type': 'minecraft:cutout',
                                                                 'textures': tex, 'elements': els})
    machine_states('oxygen_distributor', 'oxygen_distributor', 'oxygen_distributor_on')
    write('models/item/oxygen_distributor.json', {'parent': b('oxygen_distributor'), 'display': MACHINE_DISPLAY})


def airlock_door():
    tex = {'frame': b('airlock_door_frame'), 'particle': b('airlock_door_frame')}

    def striped_post(x0, x1, y0, y1):
        """Frame post: hazard stripes (texture columns 0-1) on its front and back, plain steel elsewhere."""
        return cube([x0, y0, 5], [x1, y1, 11], 'frame', per_face={
            'north': [0, 16 - y1, 2, 16 - y0], 'south': [0, 16 - y1, 2, 16 - y0],
            'east': [5, 16 - y1, 11, 16 - y0], 'west': [5, 16 - y1, 11, 16 - y0],
            'up': [x0 + 3, 5, x1 + 3, 11], 'down': [x0 + 3, 5, x1 + 3, 11]})

    lower = [striped_post(0, 2, 0, 16), striped_post(14, 16, 0, 16),
             cube([2, 0, 5], [14, 1, 11], 'frame', per_face={'up': [2, 5, 14, 11], 'down': [2, 5, 14, 11], **{s: [3, 15, 15, 16] for s in SIDES}})]
    upper = [striped_post(0, 2, 0, 16), striped_post(14, 16, 0, 16),
             cube([2, 15, 5], [14, 16, 11], 'frame', per_face={'up': [2, 5, 14, 11], 'down': [2, 5, 14, 11], **{s: [3, 0, 15, 1] for s in SIDES}})]
    write('models/block/airlock_door_frame_lower.json', {'parent': 'minecraft:block/block', 'render_type': 'minecraft:cutout', 'textures': tex, 'elements': lower})
    write('models/block/airlock_door_frame_upper.json', {'parent': 'minecraft:block/block', 'render_type': 'minecraft:cutout', 'textures': tex, 'elements': upper})

    for half, panel_tex, y0, y1 in (('lower', 'airlock_door_panel', 1, 16), ('upper', 'airlock_door_panel_window', 0, 15)):
        ptex = {'panel': b(panel_tex), 'edge': b('life_support_plinth'), 'particle': b(panel_tex)}
        for side, x0, x1 in (('left', 2, 8), ('right', 8, 14)):
            el = cube([x0, y0, 6], [x1, y1, 10], {'north': 'panel', 'south': 'panel', 'east': 'edge', 'west': 'edge', 'up': 'edge', 'down': 'edge'},
                      per_face={'north': [16 - x1, 16 - y1, 16 - x0, 16 - y0], 'south': [x0, 16 - y1, x1, 16 - y0],
                                'east': [6, 16 - y1, 10, 16 - y0], 'west': [6, 16 - y1, 10, 16 - y0],
                                'up': [x0, 6, x1, 10], 'down': [x0, 6, x1, 10]})
            write(f'models/block/airlock_door_panel_{side}_{half}.json',
                  {'parent': 'minecraft:block/block', 'render_type': 'minecraft:cutout', 'textures': ptex, 'elements': [el]})

    parts = []
    for facing, y in FACING_Y.items():
        for half in ('lower', 'upper'):
            apply = {'model': b(f'airlock_door_frame_{half}')}
            if y % 180:
                apply['y'] = 90
            parts.append({'when': {'facing': facing, 'half': half}, 'apply': apply})
    write('blockstates/airlock_door.json', {'multipart': parts})
    generated_item('airlock_door')
