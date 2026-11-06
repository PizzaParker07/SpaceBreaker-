"""Mars blocks: soils with wind-erased boot prints, sands, basalt and sandstone families, ices, ores and items."""
from sbmodels.common import *  # noqa: F401,F403
from modules.moon import soil, slab, wall

SOILS = ['martian_regolith', 'hematite_soil', 'perchlorate_soil', 'martian_clay', 'co2_frost', 'phobos_regolith', 'deimos_regolith']
SIMPLE = ['carbonaceous_chondrite', 'martian_sand', 'olivine_sand', 'martian_mudstone', 'gypsum_block', 'dusty_ice', 'dry_ice']
# (base block, texture, stem for stairs/slab/wall, has wall)
FAMILIES = [
    ('martian_basalt', 'martian_basalt', 'martian_basalt', True),
    ('polished_martian_basalt', 'polished_martian_basalt', 'polished_martian_basalt', False),
    ('martian_basalt_bricks', 'martian_basalt_bricks', 'martian_basalt_brick', True),
]


def stairs3(name, t):
    """Stairs with separate bottom/side/top textures (vanilla sandstone stairs)."""
    write(f'models/block/{name}.json', {'parent': 'minecraft:block/stairs', 'textures': t})
    write(f'models/block/{name}_inner.json', {'parent': 'minecraft:block/inner_stairs', 'textures': t})
    write(f'models/block/{name}_outer.json', {'parent': 'minecraft:block/outer_stairs', 'textures': t})
    yrot = {'east': 0, 'south': 90, 'west': 180, 'north': 270}
    variants = {}
    for facing, fy in yrot.items():
        for half in ('bottom', 'top'):
            for shape in ('straight', 'inner_left', 'inner_right', 'outer_left', 'outer_right'):
                model = b(name) if shape == 'straight' else b(f"{name}_{shape.split('_')[0]}")
                y = fy
                if shape in ('inner_left', 'outer_left'):
                    y = (fy + 270) % 360
                x = 0
                if half == 'top':
                    x = 180
                    if shape in ('inner_left', 'outer_left'):
                        y = fy
                    elif shape in ('inner_right', 'outer_right'):
                        y = (fy + 90) % 360
                v = {'model': model}
                if x:
                    v['x'] = x
                if y:
                    v['y'] = y
                if x or y:
                    v['uvlock'] = True
                variants[f'facing={facing},half={half},shape={shape}'] = v
    write(f'blockstates/{name}.json', {'variants': variants})
    write(f'models/item/{name}.json', {'parent': b(name)})


def slab3(name, t, full):
    write(f'models/block/{name}.json', {'parent': 'minecraft:block/slab', 'textures': t})
    write(f'models/block/{name}_top.json', {'parent': 'minecraft:block/slab_top', 'textures': t})
    write(f'blockstates/{name}.json', {'variants': {
        'type=bottom': {'model': b(name)},
        'type=top': {'model': b(f'{name}_top')},
        'type=double': {'model': b(full)},
    }})
    write(f'models/item/{name}.json', {'parent': b(name)})


def sandstone():
    t = {'bottom': b('martian_sandstone_bottom'), 'side': b('martian_sandstone'), 'top': b('martian_sandstone_top')}
    write('blockstates/martian_sandstone.json', {'variants': {'': {'model': b('martian_sandstone')}}})
    write('models/block/martian_sandstone.json', {'parent': 'minecraft:block/cube_bottom_top', 'textures': t})
    write('models/item/martian_sandstone.json', {'parent': b('martian_sandstone')})
    stairs3('martian_sandstone_stairs', t)
    slab3('martian_sandstone_slab', t, 'martian_sandstone')
    wall('martian_sandstone_wall', 'martian_sandstone')


def random_rotated(name):
    """Plain cube with four random rotations, like vanilla sand (hides tiling on dune fields)."""
    write(f'models/block/{name}.json', {'parent': 'minecraft:block/cube_all', 'textures': {'all': b(name)}})
    write(f'blockstates/{name}.json', {'variants': {'': [{'model': b(name)}, {'model': b(name), 'y': 90},
                                                          {'model': b(name), 'y': 180}, {'model': b(name), 'y': 270}]}})
    write(f'models/item/{name}.json', {'parent': b(name)})


def generate():
    for s in SOILS:
        soil(s)
    for s in SIMPLE:
        if s in ('martian_sand', 'olivine_sand'):
            random_rotated(s)
        else:
            simple_block(s)
    write('blockstates/martian_ice.json', {'variants': {'': {'model': b('martian_ice')}}})
    write('models/block/martian_ice.json', {'parent': 'minecraft:block/cube_all',
                                             'textures': {'all': b('martian_ice')}})
    write('models/item/martian_ice.json', {'parent': b('martian_ice')})
    from modules.moon import stairs
    for base, tex, stem, has_wall in FAMILIES:
        simple_block(base, b(tex))
        stairs(f'{stem}_stairs', tex)
        slab(f'{stem}_slab', tex, base)
        if has_wall:
            wall(f'{stem}_wall', tex)
    sandstone()
    ore('hematite_ore', b('martian_sandstone'), 'hematite_ore_overlay')
    ore('gypsum_ore', b('martian_mudstone'), 'gypsum_ore_overlay')
    generated_item('raw_hematite')
    generated_item('gypsum_crystal')
    generated_item('perchlorate_salt')
    eggs()


EGGS = ['rust_beetle', 'dust_devil', 'canyon_lurker', 'stonegrazer', 'polar_worm']


def eggs():
    for e in EGGS:
        write(f'models/item/{e}_spawn_egg.json', {'parent': 'minecraft:item/template_spawn_egg'})
