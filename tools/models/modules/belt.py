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
