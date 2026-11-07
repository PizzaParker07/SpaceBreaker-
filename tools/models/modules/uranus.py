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
    soil('ariel_regolith')
    simple_block('ariel_ice')
    stairs('ariel_ice_stairs', 'ariel_ice')
    slab('ariel_ice_slab', 'ariel_ice', 'ariel_ice')
    wall('ariel_ice_wall', 'ariel_ice')
    simple_block('graben_floor_ice')
    ore('ariel_ammonia_ore', b('ariel_ice'), 'ammonia_hydrate_ore_overlay')
    write('models/item/frost_moth_spawn_egg.json', {'parent': 'minecraft:item/template_spawn_egg'})
    soil('titania_regolith')
    simple_block('titania_ice')
    stairs('titania_ice_stairs', 'titania_ice')
    slab('titania_ice_slab', 'titania_ice', 'titania_ice')
    wall('titania_ice_wall', 'titania_ice')
    simple_block('canyon_wall_ice')
    ore('titania_carbon_ore', b('titania_ice'), 'mirandan_carbon_ore_overlay')
    write('models/item/scree_roller_spawn_egg.json', {'parent': 'minecraft:item/template_spawn_egg'})
    soil('umbriel_regolith')
    simple_block('umbriel_ice')
    stairs('umbriel_ice_stairs', 'umbriel_ice')
    slab('umbriel_ice_slab', 'umbriel_ice', 'umbriel_ice')
    wall('umbriel_ice_wall', 'umbriel_ice')
    simple_block('sooty_ice')
    write('models/item/gloam_hopper_spawn_egg.json', {'parent': 'minecraft:item/template_spawn_egg'})
