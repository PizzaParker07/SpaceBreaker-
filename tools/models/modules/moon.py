"""Moon blocks."""
from sbmodels.common import *  # noqa: F401,F403


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


def generate():
    simple_block('mare_basalt')
    lunar_regolith()
