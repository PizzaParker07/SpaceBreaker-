"""Moon blocks: soils with boot prints, rocks, building stone families, ice, ores and items."""
from sbmodels.common import *  # noqa: F401,F403

SOILS = ['lunar_regolith', 'mare_regolith', 'volcanic_glass_soil', 'icy_regolith']
ROCKS = ['mare_basalt', 'impact_breccia', 'lunar_ice', 'sintered_regolith', 'meteoric_iron']
# (base block, texture, stem for stairs/slab/wall, has wall)
FAMILIES = [
    ('anorthosite', 'anorthosite', 'anorthosite', True),
    ('polished_anorthosite', 'polished_anorthosite', 'polished_anorthosite', False),
    ('anorthosite_bricks', 'anorthosite_bricks', 'anorthosite_brick', True),
    ('sintered_regolith_bricks', 'sintered_regolith_bricks', 'sintered_regolith_brick', True),
]


def soil(name):
    """Two random-rotated plain variants; boot prints and rover wheel tracks keep their facing so trails line up."""
    write(f'models/block/{name}.json', {'parent': 'minecraft:block/cube_all', 'textures': {'all': b(name)}})
    write(f'models/block/{name}_1.json', {'parent': 'minecraft:block/cube_all', 'textures': {'all': b(name + '_1')}})
    for side in ('left', 'right', 'track'):
        write(f'models/block/{name}_footprint_{side}.json', {
            'parent': 'minecraft:block/cube',
            'textures': {
                'particle': b(name),
                'up': b(f'{name}_footprint_{side}'),
                'down': b(name),
                'north': b(name), 'south': b(name), 'east': b(name), 'west': b(name),
            },
        })
    plain = []
    for model in (name, name + '_1'):
        for y in (0, 90, 180, 270):
            v = {'model': b(model)}
            if y:
                v['y'] = y
            plain.append(v)
    rot = {'north': 0, 'east': 90, 'south': 180, 'west': 270}
    variants = {}
    for facing, y in rot.items():
        variants[f'facing={facing},print=none'] = plain
        for side in ('left', 'right', 'track'):
            v = {'model': b(f'{name}_footprint_{side}')}
            if y:
                v['y'] = y
            variants[f'facing={facing},print={side}'] = v
    write(f'blockstates/{name}.json', {'variants': variants})
    write(f'models/item/{name}.json', {'parent': b(name)})


def stairs(name, tex):
    t = {'bottom': b(tex), 'side': b(tex), 'top': b(tex)}
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


def slab(name, tex, full):
    t = {'bottom': b(tex), 'side': b(tex), 'top': b(tex)}
    write(f'models/block/{name}.json', {'parent': 'minecraft:block/slab', 'textures': t})
    write(f'models/block/{name}_top.json', {'parent': 'minecraft:block/slab_top', 'textures': t})
    write(f'blockstates/{name}.json', {'variants': {
        'type=bottom': {'model': b(name)},
        'type=top': {'model': b(f'{name}_top')},
        'type=double': {'model': b(full)},
    }})
    write(f'models/item/{name}.json', {'parent': b(name)})


def wall(name, tex):
    write(f'models/block/{name}_post.json', {'parent': 'minecraft:block/template_wall_post', 'textures': {'wall': b(tex)}})
    write(f'models/block/{name}_side.json', {'parent': 'minecraft:block/template_wall_side', 'textures': {'wall': b(tex)}})
    write(f'models/block/{name}_side_tall.json', {'parent': 'minecraft:block/template_wall_side_tall', 'textures': {'wall': b(tex)}})
    write(f'models/item/{name}.json', {'parent': 'minecraft:block/wall_inventory', 'textures': {'wall': b(tex)}})
    parts = [{'when': {'up': 'true'}, 'apply': {'model': b(f'{name}_post')}}]
    for d, y in (('north', 0), ('east', 90), ('south', 180), ('west', 270)):
        for height, suffix in (('low', '_side'), ('tall', '_side_tall')):
            apply = {'model': b(name + suffix), 'uvlock': True}
            if y:
                apply['y'] = y
            parts.append({'when': {d: height}, 'apply': apply})
    write(f'blockstates/{name}.json', {'multipart': parts})


def generate():
    for s in SOILS:
        soil(s)
    for r in ROCKS:
        simple_block(r)
    for base, tex, stem, has_wall in FAMILIES:
        simple_block(base, b(tex))
        stairs(f'{stem}_stairs', tex)
        slab(f'{stem}_slab', tex, base)
        if has_wall:
            wall(f'{stem}_wall', tex)
    ore('ilmenite_ore', b('mare_basalt'), 'ilmenite_ore_overlay')
    ore('kreep_ore', b('impact_breccia'), 'kreep_ore_overlay')
    simple_block('glimmer_lamp')
    for item in ('raw_ilmenite', 'kreep_fragment', 'glimmer_shell'):
        generated_item(item)
    for mob in ('regolith_tick', 'elder_regolith_tick', 'dust_swimmer', 'survey_drone', 'glimmer_beetle'):
        write(f'models/item/{mob}_spawn_egg.json', {'parent': 'minecraft:item/template_spawn_egg'})
