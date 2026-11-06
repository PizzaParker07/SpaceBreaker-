"""Saturn's moons: Titan and Enceladus soils with boot prints, ices, the Titan ice family, ore, the methane lake block,
items and spawn eggs."""
from sbmodels.common import *  # noqa: F401,F403
from modules.moon import soil, stairs, slab, wall


def generate():
    for s in ('tholin_sand', 'tholin_mud', 'enceladus_snow'):
        soil(s)
    simple_block('titan_ice')
    stairs('titan_ice_stairs', 'titan_ice')
    slab('titan_ice_slab', 'titan_ice', 'titan_ice')
    wall('titan_ice_wall', 'titan_ice')
    for s in ('ice_cobbles', 'enceladus_ice', 'sulcus_ice'):
        simple_block(s)
    ore('tholin_ore', b('titan_ice'), 'tholin_ore_overlay')
    generated_item('tholin')
    write('models/block/titan_methane.json', {'textures': {'particle': 'minecraft:block/water_still'}})
    write('blockstates/titan_methane.json', {'variants': {'': {'model': b('titan_methane')}}})
    for e in ('methane_skater', 'geyser_drifter', 'tholin_worm'):
        write(f'models/item/{e}_spawn_egg.json', {'parent': 'minecraft:item/template_spawn_egg'})
