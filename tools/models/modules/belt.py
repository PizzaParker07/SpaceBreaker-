"""Asteroid belt blocks: Ceres and Vesta soils with boot prints, rock families, ice, ores and items."""
from sbmodels.common import *  # noqa: F401,F403
from modules.moon import soil, stairs, slab, wall

# (base block, texture, stem for stairs/slab/wall)
FAMILIES = [
    ('cerean_rock', 'cerean_rock', 'cerean_rock'),
    ('eucrite', 'eucrite', 'eucrite'),
    ('eucrite_bricks', 'eucrite_bricks', 'eucrite_brick'),
]


def generate():
    for s in ('ceres_regolith', 'ammoniated_clay', 'carbonate_crust', 'vestan_regolith', 'diogenitic_regolith'):
        soil(s)
    for base, tex, stem in FAMILIES:
        simple_block(base, b(tex))
        stairs(f'{stem}_stairs', tex)
        slab(f'{stem}_slab', tex, base)
        wall(f'{stem}_wall', tex)
    simple_block('cerean_ice')
    simple_block('diogenite')
    ore('magnetite_ore', b('cerean_rock'), 'magnetite_ore_overlay')
    ore('chromite_ore', b('diogenite'), 'chromite_ore_overlay')
    generated_item('raw_magnetite')
    generated_item('chromite')
    write('models/item/rock_mite_spawn_egg.json', {'parent': 'minecraft:item/template_spawn_egg'})
    soil('bennu_regolith')
    simple_block('bennu_boulder')
    soil('ryugu_regolith')
    simple_block('ryugu_rock')
    soil('eros_regolith')
    simple_block('eros_chondrite')
    soil('itokawa_gravel')
    simple_block('itokawa_rock')
    soil('comet_dust')
    simple_block('comet_ice')
    simple_block('comet_jet')
    simple_block('gold_foil_cladding')
    # the spent engine reuses the rocket engine's 3D model; the model points its nozzle down (facing=down)
    write('models/block/spent_engine.json', {'parent': 'spacebreaker:item/kerosene_engine_3d', 'render_type': 'minecraft:cutout'})
    rot = {'down': {}, 'up': {'x': 180}, 'north': {'x': 270}, 'south': {'x': 90}, 'west': {'x': 90, 'y': 90}, 'east': {'x': 90, 'y': 270}}
    write('blockstates/spent_engine.json', {'variants': {f'facing={f}': dict({'model': b('spent_engine')}, **v) for f, v in rot.items()}})
    write('models/item/spent_engine.json', {'parent': b('spent_engine')})
    for w in ('encke', 'halley', 'tempel'):
        soil(w + '_dust')
        simple_block(w + '_ice')
