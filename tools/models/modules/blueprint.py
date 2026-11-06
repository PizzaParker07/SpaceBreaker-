"""Blueprint area: Hologram Projector (2D icon in the GUI, 3D in hand)."""
from sbmodels.common import *  # noqa: F401,F403

SIDES = ('north', 'south', 'east', 'west')


def projector():
    def uv(side, top):
        return {s: side for s in SIDES} | {'up': top, 'down': top}
    els = [
        cube([7, 0, 7], [9, 5, 9], 'tex', per_face=uv([0, 0, 2, 5], [0, 0, 2, 2])),                  # grip
        cube([5, 5, 5], [11, 9, 11], 'tex', per_face=uv([4, 0, 10, 4], [4, 4, 10, 10])),              # body
        cube([4, 6, 7], [5, 8, 9], 'tex', per_face=uv([0, 6, 2, 8], [0, 6, 1, 8])),                   # fins
        cube([11, 6, 7], [12, 8, 9], 'tex', per_face=uv([0, 6, 2, 8], [0, 6, 1, 8])),
        cube([6, 9, 6], [10, 10, 10], 'tex', per_face=uv([10, 0, 14, 1], [10, 1, 14, 5])),            # emitter ring
    ]
    lens = cube([7, 10, 7], [9, 11, 9], 'tex', per_face=uv([14, 0, 16, 1], [14, 0, 16, 2]))           # glowing lens
    lens['neoforge_data'] = {'block_light': 15, 'sky_light': 15}
    els.append(lens)
    write('models/item/hologram_projector_3d.json', {
        'parent': 'minecraft:block/block',
        'textures': {'tex': i('hologram_projector_model'), 'particle': i('hologram_projector_model')},
        'elements': els,
        'display': {
            'thirdperson_righthand': {'rotation': [75, 0, 0], 'translation': [0, 2, 1], 'scale': [0.45, 0.45, 0.45]},
            'thirdperson_lefthand': {'rotation': [75, 0, 0], 'translation': [0, 2, 1], 'scale': [0.45, 0.45, 0.45]},
            'firstperson_righthand': {'rotation': [-15, 45, 0], 'translation': [0, 1, 0], 'scale': [0.34, 0.34, 0.34]},
            'firstperson_lefthand': {'rotation': [-15, 225, 0], 'translation': [0, 1, 0], 'scale': [0.34, 0.34, 0.34]},
            'ground': {'rotation': [0, 0, 0], 'translation': [0, 2, 0], 'scale': [0.5, 0.5, 0.5]},
            'fixed': {'rotation': [0, 0, 0], 'translation': [0, 0, 0], 'scale': [0.8, 0.8, 0.8]},
        },
    })
    write('models/item/hologram_projector_icon.json', {'parent': 'minecraft:item/generated', 'textures': {'layer0': i('hologram_projector')}})
    write('models/item/hologram_projector.json', {
        'loader': 'neoforge:separate_transforms', 'gui_light': 'front',
        'base': {'parent': i('hologram_projector_3d')},
        'perspectives': {'gui': {'parent': i('hologram_projector_icon')}, 'fixed': {'parent': i('hologram_projector_icon')}},
    })


def generate():
    projector()
