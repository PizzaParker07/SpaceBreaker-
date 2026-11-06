"""EVA suit and life support items."""
from sbmodels.common import *  # noqa: F401,F403


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


def generate():
    for name in ('eva_helmet', 'eva_chestplate', 'eva_leggings', 'eva_boots', 'magnetic_boots', 'large_oxygen_tank', 'maneuvering_unit'):
        generated_item(name)
    oxygen_tank()
