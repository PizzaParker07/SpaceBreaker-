"""Generates blockstates and block/item models (assets) for Spacebreaker.

    python3 tools/models/gen_assets.py

Complex 3D models are described here as element lists so they stay readable and reviewable.
(Data files — loot tables, tags, recipes, language — come from Java datagen.)"""
import json
import os

ROOT = os.path.abspath(os.path.join(os.path.dirname(__file__), '..', '..'))
ASSETS = os.path.join(ROOT, 'src', 'main', 'resources', 'assets', 'spacebreaker')
NS = 'spacebreaker'
written = []


def write(rel, obj):
    path = os.path.join(ASSETS, rel)
    os.makedirs(os.path.dirname(path), exist_ok=True)
    with open(path, 'w', encoding='utf-8') as f:
        json.dump(obj, f, indent=2)
        f.write('\n')
    written.append(rel)


def b(name):
    return f'{NS}:block/{name}'


def i(name):
    return f'{NS}:item/{name}'


def cube(fr, to, tex, uv=None, faces=('north', 'south', 'east', 'west', 'up', 'down'), rot=None, cull=False, per_face=None):
    """One model element. tex: texture key or dict face->key; per_face: dict face->uv."""
    el = {'from': fr, 'to': to, 'faces': {}}
    for f in faces:
        key = tex[f] if isinstance(tex, dict) else tex
        face = {'texture': '#' + key}
        if per_face and f in per_face:
            face['uv'] = per_face[f]
        elif uv:
            face['uv'] = uv
        if cull:
            face['cullface'] = f
        el['faces'][f] = face
    if rot:
        el['rotation'] = rot
    return el


# ----------------------------------------------------------------------------- generic parents

def ore_overlay_parent():
    full = ['north', 'south', 'east', 'west', 'up', 'down']
    write('models/block/ore_overlay.json', {
        'parent': 'minecraft:block/block',
        'render_type': 'minecraft:cutout_mipped',
        'textures': {'particle': '#stone'},
        'elements': [
            cube([0, 0, 0], [16, 16, 16], 'stone', faces=full, cull=True),
            cube([0, 0, 0], [16, 16, 16], 'overlay', faces=full, cull=True),
        ],
    })


def simple_block(name, texture=None):
    write(f'blockstates/{name}.json', {'variants': {'': {'model': b(name)}}})
    write(f'models/block/{name}.json', {'parent': 'minecraft:block/cube_all', 'textures': {'all': texture or b(name)}})
    write(f'models/item/{name}.json', {'parent': b(name)})


def ore(name, stone, overlay):
    write(f'blockstates/{name}.json', {'variants': {'': {'model': b(name)}}})
    write(f'models/block/{name}.json', {'parent': b('ore_overlay'), 'textures': {'stone': stone, 'overlay': b(overlay)}})
    write(f'models/item/{name}.json', {'parent': b(name)})


def generated_item(name, texture=None):
    write(f'models/item/{name}.json', {'parent': 'minecraft:item/generated', 'textures': {'layer0': texture or i(name)}})


# ----------------------------------------------------------------------------- lunar regolith

def lunar_regolith():
    write('models/block/lunar_regolith.json', {'parent': 'minecraft:block/cube_all', 'textures': {'all': b('lunar_regolith')}})
    write('models/block/lunar_regolith_1.json', {'parent': 'minecraft:block/cube_all', 'textures': {'all': b('lunar_regolith_1')}})
    for side in ('left', 'right'):
        write(f'models/block/lunar_regolith_footprint_{side}.json', {
            'parent': 'minecraft:block/cube',
            'textures': {
                'particle': b('lunar_regolith'),
                'up': b(f'lunar_regolith_footprint_{side}'),
                'down': b('lunar_regolith'),
                'north': b('lunar_regolith'), 'south': b('lunar_regolith'),
                'east': b('lunar_regolith'), 'west': b('lunar_regolith'),
            },
        })
    plain = []
    for model in ('lunar_regolith', 'lunar_regolith_1'):
        for y in (0, 90, 180, 270):
            v = {'model': b(model)}
            if y:
                v['y'] = y
            plain.append(v)
    rot = {'north': 0, 'east': 90, 'south': 180, 'west': 270}
    variants = {}
    for facing, y in rot.items():
        variants[f'facing={facing},print=none'] = plain
        for side in ('left', 'right'):
            v = {'model': b(f'lunar_regolith_footprint_{side}')}
            if y:
                v['y'] = y
            variants[f'facing={facing},print={side}'] = v
    write('blockstates/lunar_regolith.json', {'variants': variants})
    write('models/item/lunar_regolith.json', {'parent': b('lunar_regolith')})


# ----------------------------------------------------------------------------- solar panel

SOLAR_TEX = {
    'cells': b('solar_panel_cells'), 'back': b('solar_panel_back'), 'frame': b('solar_panel_frame'),
    'steel': b('solar_panel_steel'), 'base': b('solar_panel_base_top'), 'housing': b('solar_panel_housing'),
    'particle': b('solar_panel_cells'),
}


def solar_base_elements():
    sides = ('north', 'south', 'east', 'west')
    return [
        # anchored base plate
        cube([2, 0, 2], [14, 2, 14], {'up': 'base', 'down': 'steel', **{s: 'steel' for s in sides}},
             per_face={'up': [2, 2, 14, 14], 'down': [2, 2, 14, 14], **{s: [2, 14, 14, 16] for s in sides}}),
        # post
        cube([7, 2, 7], [9, 10.5, 9], 'steel', faces=sides + ('down',), per_face={s: [7, 5.5, 9, 14] for s in sides}),
        # slewing drive motor on the east side of the post
        cube([9, 6, 6.5], [12, 9, 9.5], {'east': 'housing', 'north': 'housing', 'south': 'housing', 'up': 'steel', 'down': 'steel', 'west': 'steel'},
             per_face={'east': [3, 4, 9, 10], 'north': [3, 4, 6, 7], 'south': [3, 4, 6, 7], 'up': [9, 6, 12, 9], 'down': [9, 6, 12, 9], 'west': [0, 0, 3, 3]}),
        # bearing cap the torque tube rests on
        cube([6.5, 10.5, 6.5], [9.5, 11.5, 9.5], 'steel', per_face={s: [6, 4, 9, 5] for s in sides + ('up', 'down')}),
    ]


def solar_head_elements():
    sides = ('north', 'south', 'east', 'west')
    return [
        # torque tube (rotation axis, north-south through y = 12.5)
        cube([7, 11.5, 0.5], [9, 13.5, 15.5], 'steel', per_face={
            'north': [7, 2, 9, 4], 'south': [7, 2, 9, 4], 'east': [0, 6, 15, 8], 'west': [0, 6, 15, 8], 'up': [7, 0, 9, 15], 'down': [7, 0, 9, 15]}),
        # purlins
        cube([0.5, 13.5, 2.5], [15.5, 14, 4], 'frame', per_face={s: [0, 0, 15, 0.5] for s in sides} | {'up': [0, 0, 15, 1.5], 'down': [0, 0, 15, 1.5]}),
        cube([0.5, 13.5, 12], [15.5, 14, 13.5], 'frame', per_face={s: [0, 0, 15, 0.5] for s in sides} | {'up': [0, 0, 15, 1.5], 'down': [0, 0, 15, 1.5]}),
        # the module itself
        cube([0, 14, 0], [16, 15, 16], {'up': 'cells', 'down': 'back', **{s: 'frame' for s in sides}},
             per_face={'up': [0, 0, 16, 16], 'down': [0, 0, 16, 16], **{s: [0, 0, 16, 1] for s in sides}}),
    ]


def solar_panel():
    write('models/block/solar_panel_base.json', {
        'parent': 'minecraft:block/block', 'render_type': 'minecraft:cutout',
        'textures': SOLAR_TEX, 'elements': solar_base_elements()})
    write('models/block/solar_panel_head.json', {
        'parent': 'minecraft:block/block', 'render_type': 'minecraft:cutout', 'ambientocclusion': False,
        'textures': SOLAR_TEX, 'elements': solar_head_elements()})
    write('blockstates/solar_panel.json', {'variants': {'': {'model': b('solar_panel_base')}}})
    write('models/item/solar_panel.json', {
        'parent': 'minecraft:block/block', 'textures': SOLAR_TEX,
        'elements': solar_base_elements() + solar_head_elements(),
        'display': {
            'gui': {'rotation': [30, 225, 0], 'translation': [0, -1, 0], 'scale': [0.6, 0.6, 0.6]},
            'ground': {'rotation': [0, 0, 0], 'translation': [0, 3, 0], 'scale': [0.25, 0.25, 0.25]},
            'fixed': {'rotation': [0, 0, 0], 'translation': [0, 0, 0], 'scale': [0.5, 0.5, 0.5]},
            'thirdperson_righthand': {'rotation': [75, 45, 0], 'translation': [0, 2.5, 0], 'scale': [0.375, 0.375, 0.375]},
            'firstperson_righthand': {'rotation': [0, 45, 0], 'translation': [0, 0, 0], 'scale': [0.4, 0.4, 0.4]},
            'firstperson_lefthand': {'rotation': [0, 225, 0], 'translation': [0, 0, 0], 'scale': [0.4, 0.4, 0.4]},
        }})


# ----------------------------------------------------------------------------- oxygen tank (3D in hand, 2D in GUI)

def oxygen_tank():
    r45 = {'angle': 45, 'axis': 'y', 'origin': [8, 8, 8]}
    sides = ('north', 'south', 'east', 'west')
    body_uv = {s: [0, 0, 4, 11] for s in sides} | {'up': [4, 0, 8, 4], 'down': [4, 0, 8, 4]}
    shoulder_uv = {s: [4, 0, 7, 1] for s in sides} | {'up': [4, 0, 7, 3], 'down': [4, 0, 7, 3]}
    els = [
        cube([6, 1, 6], [10, 12, 10], 'tex', per_face=body_uv),
        cube([6, 1, 6], [10, 12, 10], 'tex', per_face=body_uv, rot=r45),
        cube([6.5, 12, 6.5], [9.5, 13, 9.5], 'tex', per_face=shoulder_uv),
        cube([6.5, 12, 6.5], [9.5, 13, 9.5], 'tex', per_face=shoulder_uv, rot=r45),
        # neck + valve
        cube([7.25, 13, 7.25], [8.75, 14, 8.75], 'tex', per_face={s: [8, 2, 9.5, 3] for s in sides} | {'up': [8, 0, 9.5, 1.5], 'down': [8, 0, 9.5, 1.5]}),
        cube([7, 14, 7], [9, 15, 9], 'tex', per_face={s: [12, 0, 14, 1] for s in sides} | {'up': [12, 0, 14, 2], 'down': [12, 1, 14, 3]}),
        # hand wheel
        cube([6.75, 15, 7.5], [9.25, 15.75, 8.5], 'tex', per_face={s: [12, 2, 14.5, 2.75] for s in sides} | {'up': [12, 2, 14.5, 3], 'down': [12, 2, 14.5, 3]}),
        cube([7.5, 15, 6.75], [8.5, 15.75, 9.25], 'tex', per_face={s: [12, 2, 14.5, 2.75] for s in sides} | {'up': [12, 2, 13, 3.5], 'down': [12, 2, 13, 3.5]}),
        # pressure gauge on the east side of the valve
        cube([9, 13.75, 7.5], [10.5, 15.25, 8.5], 'tex', per_face={'east': [8, 4, 10, 6], 'north': [10, 4, 11.5, 5.5], 'south': [10, 4, 11.5, 5.5],
                                                                   'up': [10, 4, 11.5, 5], 'down': [10, 4, 11.5, 5], 'west': [10, 4, 11, 5]}),
    ]
    write('models/item/oxygen_tank_3d.json', {
        'parent': 'minecraft:block/block',
        'textures': {'tex': i('oxygen_tank_model'), 'particle': i('oxygen_tank_model')},
        'elements': els,
        'display': {
            'thirdperson_righthand': {'rotation': [75, 0, 0], 'translation': [0, 2.5, 0.5], 'scale': [0.5, 0.5, 0.5]},
            'thirdperson_lefthand': {'rotation': [75, 0, 0], 'translation': [0, 2.5, 0.5], 'scale': [0.5, 0.5, 0.5]},
            'firstperson_righthand': {'rotation': [0, 40, 0], 'translation': [1, 3.2, 0], 'scale': [0.32, 0.32, 0.32]},
            'firstperson_lefthand': {'rotation': [0, 220, 0], 'translation': [1, 3.2, 0], 'scale': [0.32, 0.32, 0.32]},
            'ground': {'rotation': [0, 0, 0], 'translation': [0, 2, 0], 'scale': [0.5, 0.5, 0.5]},
            'fixed': {'rotation': [0, 0, 0], 'translation': [0, 0, 0], 'scale': [0.9, 0.9, 0.9]},
            'head': {'rotation': [0, 0, 0], 'translation': [0, 13, 7], 'scale': [1, 1, 1]},
        },
    })
    write('models/item/oxygen_tank_icon.json', {'parent': 'minecraft:item/generated', 'textures': {'layer0': i('oxygen_tank')}})
    write('models/item/oxygen_tank.json', {
        'loader': 'neoforge:separate_transforms',
        'gui_light': 'front',
        'base': {'parent': i('oxygen_tank_3d')},
        'perspectives': {'gui': {'parent': i('oxygen_tank_icon')}},
    })


def main():
    ore_overlay_parent()
    ore('bauxite_ore', 'minecraft:block/stone', 'bauxite_ore_overlay')
    ore('deepslate_bauxite_ore', 'minecraft:block/deepslate', 'bauxite_ore_overlay')
    ore('ilmenite_ore', b('mare_basalt'), 'ilmenite_ore_overlay')
    simple_block('mare_basalt')
    simple_block('raw_bauxite_block')
    lunar_regolith()
    solar_panel()
    for name in ('raw_bauxite', 'aluminum_ingot', 'aluminum_plate', 'eva_helmet'):
        generated_item(name)
    oxygen_tank()
    print(f'{len(written)} asset files written')


if __name__ == '__main__':
    main()
