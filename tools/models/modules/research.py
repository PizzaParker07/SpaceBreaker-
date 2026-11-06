"""Research: Research Lab (bench cabinet + analyser + microscope), tech disk, samples, kit, field guide."""
from sbmodels.common import *  # noqa: F401,F403

LAB_TEX = {
    'front': b('research_lab_front'), 'side': b('research_lab_side'), 'top': b('research_lab_top'),
    'unit': b('research_lab_unit'), 'screen': b('research_lab_screen'), 'screen_on': b('research_lab_screen_on'),
    'scope': b('research_lab_microscope'), 'particle': b('research_lab_side'),
}


def lab_elements(on: bool):
    sides = ('east', 'west', 'south')
    els = [
        # bench cabinet, 9 px high
        cube([0, 0, 0], [16, 9, 16], {'north': 'front', 'up': 'top', 'down': 'side', **{s: 'side' for s in sides}},
             per_face={'north': [0, 7, 16, 16], 'east': [0, 7, 16, 16], 'west': [0, 7, 16, 16], 'south': [0, 7, 16, 16],
                       'up': [0, 0, 16, 16], 'down': [0, 0, 16, 16]}, cull=False),
        # analyser unit on the back half of the worktop
        cube([2, 9, 8], [14, 16, 15], {'north': 'screen', 'up': 'unit', 'east': 'unit', 'west': 'unit', 'south': 'unit', 'down': 'unit'},
             per_face={'north': [2, 2, 14, 9], 'up': [2, 1, 14, 8], 'east': [1, 2, 8, 9], 'west': [8, 2, 15, 9],
                       'south': [2, 2, 14, 9], 'down': [2, 1, 14, 8]}),
        # microscope: foot, column and tube (front left of the worktop)
        cube([3, 9, 2], [7, 10, 6], 'scope', per_face={'up': [0, 0, 4, 4], 'down': [0, 0, 4, 4], 'north': [0, 4, 4, 5],
                                                         'south': [0, 4, 4, 5], 'east': [0, 4, 4, 5], 'west': [0, 4, 4, 5]}),
        cube([5, 10, 5], [6, 14, 6], 'scope', per_face={s: [4, 0, 5, 4] for s in ('north', 'south', 'east', 'west')} | {'up': [4, 0, 5, 1], 'down': [4, 0, 5, 1]}),
        cube([4, 11, 3], [6, 15, 5], 'scope', per_face={s: [8, 8, 10, 12] for s in ('north', 'south', 'east', 'west')} | {'up': [0, 0, 2, 2], 'down': [8, 8, 10, 10]}),
    ]
    if on:
        el = cube([6, 10, 7.95], [12, 15, 7.95], 'screen_on', faces=('north',), per_face={'north': [4, 3, 10, 8]})
        el['shade'] = False
        el['faces']['north']['neoforge_data'] = {'block_light': 15, 'sky_light': 15, 'ambient_occlusion': False}
        els.append(el)
    return els


def research_lab():
    for name, on in (('research_lab', False), ('research_lab_on', True)):
        write(f'models/block/{name}.json', {
            'parent': 'minecraft:block/block', 'textures': LAB_TEX, 'elements': lab_elements(on)})
    variants = {}
    for facing, rot in (('north', 0), ('east', 90), ('south', 180), ('west', 270)):
        for working in ('false', 'true'):
            m = {'model': b('research_lab_on' if working == 'true' else 'research_lab')}
            if rot:
                m['y'] = rot
            variants[f'facing={facing},working={working}'] = m
    write('blockstates/research_lab.json', {'variants': variants})
    write('models/item/research_lab.json', {'parent': b('research_lab')})


def items():
    write('models/item/tech_disk.json', {'parent': 'minecraft:item/generated',
                                          'textures': {'layer0': i('tech_disk'), 'layer1': i('tech_disk_label')}})
    write('models/item/field_sample.json', {'parent': 'minecraft:item/generated',
                                             'textures': {'layer0': i('field_sample'), 'layer1': i('field_sample_contents')}})
    write('models/item/experiment_data.json', {'parent': 'minecraft:item/generated',
                                                'textures': {'layer0': i('experiment_data'), 'layer1': i('experiment_data_band')}})
    write('models/item/sample_kit.json', {'parent': 'minecraft:item/handheld', 'textures': {'layer0': i('sample_kit')}})
    generated_item('guide_book')


def generate():
    research_lab()
    items()
