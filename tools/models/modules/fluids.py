"""Fluid blocks, buckets (NeoForge dynamic bucket model, tinted by the fluid) and the cryogenic canister."""
from sbmodels.common import *  # noqa: F401,F403

LIQUIDS = ['crude_oil', 'rp1', 'liquid_oxygen', 'liquid_hydrogen', 'liquid_methane']


def generate():
    for f in LIQUIDS:
        write(f'models/block/{f}.json', {'textures': {'particle': 'minecraft:block/water_still'}})
        write(f'blockstates/{f}.json', {'variants': {'': {'model': b(f)}}})
        write(f'models/item/{f}_bucket.json', {
            'parent': 'neoforge:item/bucket', 'loader': 'neoforge:fluid_container',
            'fluid': f'{NS}:{f}'})
    generated_item('cryogenic_canister')
