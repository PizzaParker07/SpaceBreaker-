"""Earth ores, metals, alloys, intermediates (and the Moon's ilmenite overlay, until it moves)."""
from sbmodels.common import *  # noqa: F401,F403

ORES = ['bauxite', 'rutile', 'pentlandite', 'spodumene', 'wolframite']
METAL_BLOCKS = ['aluminum', 'titanium', 'nickel', 'lithium', 'tungsten', 'steel', 'aluminum_lithium', 'nickel_superalloy']
DUSTS = ['aluminum', 'titanium', 'nickel', 'lithium', 'tungsten', 'iron', 'copper', 'gold']
PLATES = ['aluminum', 'titanium', 'nickel', 'lithium', 'tungsten', 'steel', 'aluminum_lithium', 'nickel_superalloy', 'iron', 'copper', 'gold']
WIRES = ['aluminum', 'tungsten', 'copper', 'gold']
RODS = ['titanium', 'tungsten', 'steel', 'iron']
PARTS = ['coal_dust', 'silicon', 'silicon_wafer', 'solar_cell', 'plastic_sheet', 'basic_circuit', 'advanced_circuit',
         'electric_motor', 'ablative_shield_plate', 'gold_foil_insulation', 'orange_foam_insulation', 'silicone_seal',
         'plate_press_die', 'wire_press_die', 'rod_press_die']


def handheld(name, tex=None):
    write(f'models/item/{name}.json', {'parent': 'minecraft:item/handheld', 'textures': {'layer0': tex or i(name)}})


def generate():
    ore_overlay_parent()
    for name in ORES:
        ore(f'{name}_ore', 'minecraft:block/stone', f'{name}_ore_overlay')
        ore(f'deepslate_{name}_ore', 'minecraft:block/deepslate', f'{name}_ore_overlay')
        simple_block(f'raw_{name}_block')
        generated_item(f'raw_{name}')
    ore('ilmenite_ore', b('mare_basalt'), 'ilmenite_ore_overlay')
    for m in METAL_BLOCKS:
        simple_block(f'{m}_block')
        generated_item(f'{m}_ingot')
        generated_item(f'{m}_nugget')
    for m in PLATES:
        generated_item(f'{m}_plate')
    for m in DUSTS:
        generated_item(f'{m}_dust')
    for m in WIRES:
        generated_item(f'{m}_wire')
    for m in RODS:
        handheld(f'{m}_rod')
    for p in PARTS:
        generated_item(p)
    simple_block('oil_seep')
