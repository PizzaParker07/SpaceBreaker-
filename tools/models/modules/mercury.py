"""Mercury blocks: regolith and hollow deposits with boot prints, crust, low-reflectance rock and graphite ore."""
from sbmodels.common import *  # noqa: F401,F403
from modules.moon import soil


def generate():
    soil('mercury_regolith')
    soil('hollow_deposit')
    simple_block('mercurian_crust')
    simple_block('low_reflectance_rock')
    ore('graphite_ore', b('low_reflectance_rock'), 'graphite_ore_overlay')
    generated_item('graphite')
    write('models/item/terminator_crawler_spawn_egg.json', {'parent': 'minecraft:item/template_spawn_egg'})
