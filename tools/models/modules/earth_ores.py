"""Earth ores and metals (and other ore overlays)."""
from sbmodels.common import *  # noqa: F401,F403


def generate():
    ore_overlay_parent()
    ore('bauxite_ore', 'minecraft:block/stone', 'bauxite_ore_overlay')
    ore('deepslate_bauxite_ore', 'minecraft:block/deepslate', 'bauxite_ore_overlay')
    simple_block('raw_bauxite_block')
    for name in ('raw_bauxite', 'aluminum_ingot', 'aluminum_plate'):
        generated_item(name)
