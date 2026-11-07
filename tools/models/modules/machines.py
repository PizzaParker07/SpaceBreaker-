"""Machines: solar tracker."""
from sbmodels.common import *  # noqa: F401,F403


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


def solar_array_head_elements():
    """Three tracker modules on one long torque tube (z -16..32), same pivot as the single tracker."""
    sides = ('north', 'south', 'east', 'west')
    els = [cube([7, 11.5, -16], [9, 13.5, 32], 'steel', per_face={
        'north': [7, 2, 9, 4], 'south': [7, 2, 9, 4], 'east': [0, 6, 16, 8], 'west': [0, 6, 16, 8], 'up': [7, 0, 9, 16], 'down': [7, 0, 9, 16]})]
    for z0 in (-16, 0, 16):
        els += [
            cube([0.5, 13.5, z0 + 2.5], [15.5, 14, z0 + 4], 'frame', per_face={s: [0, 0, 15, 0.5] for s in sides} | {'up': [0, 0, 15, 1.5], 'down': [0, 0, 15, 1.5]}),
            cube([0.5, 13.5, z0 + 12], [15.5, 14, z0 + 13.5], 'frame', per_face={s: [0, 0, 15, 0.5] for s in sides} | {'up': [0, 0, 15, 1.5], 'down': [0, 0, 15, 1.5]}),
            cube([0, 14, z0], [16, 15, z0 + 16], {'up': 'cells', 'down': 'back', **{s: 'frame' for s in sides}},
                 per_face={'up': [0, 0, 16, 16], 'down': [0, 0, 16, 16], **{s: [0, 0, 16, 1] for s in sides}}),
        ]
    return els


def solar_array():
    write('models/block/solar_array_base.json', {
        'parent': 'minecraft:block/block', 'render_type': 'minecraft:cutout',
        'textures': SOLAR_TEX, 'elements': solar_base_elements()})
    write('models/block/solar_array_head.json', {
        'parent': 'minecraft:block/block', 'render_type': 'minecraft:cutout', 'ambientocclusion': False,
        'textures': SOLAR_TEX, 'elements': solar_array_head_elements()})
    write('blockstates/solar_array.json', {'variants': {'': {'model': b('solar_array_base')}}})
    write('models/item/solar_array.json', {
        'parent': 'minecraft:block/block', 'textures': SOLAR_TEX,
        'elements': solar_base_elements() + solar_array_head_elements(),
        'display': {
            'gui': {'rotation': [30, 225, 0], 'translation': [0, -1, 0], 'scale': [0.3, 0.3, 0.3]},
            'ground': {'rotation': [0, 0, 0], 'translation': [0, 3, 0], 'scale': [0.15, 0.15, 0.15]},
            'fixed': {'rotation': [0, 0, 0], 'translation': [0, 0, 0], 'scale': [0.3, 0.3, 0.3]},
            'thirdperson_righthand': {'rotation': [75, 45, 0], 'translation': [0, 2.5, 0], 'scale': [0.2, 0.2, 0.2]},
            'firstperson_righthand': {'rotation': [0, 45, 0], 'translation': [0, 0, 0], 'scale': [0.25, 0.25, 0.25]},
            'firstperson_lefthand': {'rotation': [0, 225, 0], 'translation': [0, 0, 0], 'scale': [0.25, 0.25, 0.25]},
        }})


def generate():
    solar_panel()
    solar_array()
