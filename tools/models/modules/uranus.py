"""Miranda: regolith with boot prints, the Mirandan ice family, corona ices, ore and spawn egg."""
from sbmodels.common import *  # noqa: F401,F403
from modules.moon import soil, stairs, slab, wall


def generate():
    soil('mirandan_regolith')
    simple_block('mirandan_ice')
    stairs('mirandan_ice_stairs', 'mirandan_ice')
    slab('mirandan_ice_slab', 'mirandan_ice', 'mirandan_ice')
    wall('mirandan_ice_wall', 'mirandan_ice')
    for s in ('bright_ridge_ice', 'dark_band_ice'):
        simple_block(s)
    ore('mirandan_carbon_ore', b('mirandan_ice'), 'mirandan_carbon_ore_overlay')
    write('models/item/cliff_clinger_spawn_egg.json', {'parent': 'minecraft:item/template_spawn_egg'})
