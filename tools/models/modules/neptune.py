"""Triton: soils with boot prints, the Tritonian ice family, the nitrogen geyser, ore, item and spawn egg."""
from sbmodels.common import *  # noqa: F401,F403
from modules.moon import soil, stairs, slab, wall
from modules.jupiter import vent


def generate():
    for s in ('triton_frost', 'pink_nitrogen_frost', 'plume_dust'):
        soil(s)
    simple_block('triton_ice')
    stairs('triton_ice_stairs', 'triton_ice')
    slab('triton_ice_slab', 'triton_ice', 'triton_ice')
    wall('triton_ice_wall', 'triton_ice')
    vent('nitrogen_geyser')
    ore('methane_clathrate_ore', b('triton_ice'), 'methane_clathrate_ore_overlay')
    generated_item('methane_clathrate')
    write('models/item/plume_beetle_spawn_egg.json', {'parent': 'minecraft:item/template_spawn_egg'})
