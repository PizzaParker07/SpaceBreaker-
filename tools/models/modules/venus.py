"""Venus blocks: scorched basalt, gravel, tessera rock, sulfur crust, metal frost and the cloud-deck composites."""
from sbmodels.common import *  # noqa: F401,F403
from modules.mars import random_rotated

SIMPLE = ['venus_basalt', 'tessera_rock', 'sulfur_crust', 'metal_frost', 'composite_panel', 'envelope_fabric']


def generate():
    for s in SIMPLE:
        simple_block(s)
    random_rotated('venus_soil')
    write('blockstates/aerogel.json', {'variants': {'': {'model': b('aerogel')}}})
    write('models/block/aerogel.json', {'parent': 'minecraft:block/cube_all', 'render_type': 'minecraft:translucent',
                                         'textures': {'all': b('aerogel')}})
    write('models/item/aerogel.json', {'parent': b('aerogel')})
    generated_item('sulfur')
    generated_item('acid_coating')
    generated_item('lift_gas_sac')
    generated_item('aerostat_kit')
    for e in ('acid_ray', 'cloud_jelly'):
        write(f'models/item/{e}_spawn_egg.json', {'parent': 'minecraft:item/template_spawn_egg'})
