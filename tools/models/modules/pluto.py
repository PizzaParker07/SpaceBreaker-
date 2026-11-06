"""Pluto and Charon: soils with boot prints, ices, the Plutonian ice family, ores, items and spawn eggs."""
from sbmodels.common import *  # noqa: F401,F403
from modules.moon import soil, stairs, slab, wall


def generate():
    for s in ('plutonian_frost', 'tholin_regolith', 'charon_regolith', 'red_polar_frost'):
        soil(s)
    simple_block('plutonian_ice')
    stairs('plutonian_ice_stairs', 'plutonian_ice')
    slab('plutonian_ice_slab', 'plutonian_ice', 'plutonian_ice')
    wall('plutonian_ice_wall', 'plutonian_ice')
    for s in ('nitrogen_ice', 'stained_nitrogen_ice', 'methane_frost', 'charon_ice'):
        simple_block(s)
    ore('plutonian_tholin_ore', b('plutonian_ice'), 'plutonian_tholin_ore_overlay')
    ore('ammonia_hydrate_ore', b('charon_ice'), 'ammonia_hydrate_ore_overlay')
    generated_item('ammonia_crystal')
    for e in ('nitrogen_slider', 'frost_tortoise'):
        write(f'models/item/{e}_spawn_egg.json', {'parent': 'minecraft:item/template_spawn_egg'})
