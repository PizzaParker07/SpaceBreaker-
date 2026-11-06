"""Jupiter's moons: soils with boot prints, ices, rock families, ores, vents and items."""
from sbmodels.common import *  # noqa: F401,F403
from modules.moon import soil, stairs, slab, wall

FAMILIES = [
    ('europan_ice_bricks', 'europan_ice_bricks', 'europan_ice_brick'),
    ('ionian_basalt', 'ionian_basalt', 'ionian_basalt'),
    ('callistan_rock', 'callistan_rock', 'callistan_rock'),
]


def vent(name):
    t = {'top': b(name + '_top'), 'bottom': b(name + '_side'), 'side': b(name + '_side')}
    write(f'models/block/{name}.json', {'parent': 'minecraft:block/cube_bottom_top', 'textures': t})
    write(f'blockstates/{name}.json', {'variants': {'': {'model': b(name)}}})
    write(f'models/item/{name}.json', {'parent': b(name)})


def generate():
    for s in ('europan_frost', 'sulfur_sand', 'so2_frost', 'red_sulfur', 'ganymede_regolith', 'callisto_regolith', 'bright_frost'):
        soil(s)
    for base, tex, stem in FAMILIES:
        simple_block(base, b(tex))
        stairs(f'{stem}_stairs', tex)
        slab(f'{stem}_slab', tex, base)
        wall(f'{stem}_wall', tex)
    for s in ('europan_ice', 'stained_ice', 'refrozen_brine', 'grooved_ice', 'dark_ice_crust'):
        simple_block(s)
    ore('salt_ore', b('europan_ice'), 'salt_ore_overlay')
    ore('ionian_sulfur_ore', b('ionian_basalt'), 'ionian_sulfur_ore_overlay')
    vent('ice_geyser')
    vent('sulfur_vent')
    generated_item('ocean_salt')
    for e in ('glow_serpent', 'sulfur_crab'):
        write(f'models/item/{e}_spawn_egg.json', {'parent': 'minecraft:item/template_spawn_egg'})
