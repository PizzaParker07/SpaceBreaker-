"""Generates every Spacebreaker texture into src/main/resources/assets/spacebreaker/textures.

    python3 tools/textures/generate.py

Textures are drawn by code (see sbtex/). Re-running is deterministic."""
import os
import sys

HERE = os.path.dirname(os.path.abspath(__file__))
sys.path.insert(0, HERE)

from sbtex import moon, ores, items, machines, suit, pbr  # noqa: E402
from sbtex import palettes as P  # noqa: E402

ROOT = os.path.abspath(os.path.join(HERE, '..', '..'))
TEX = os.path.join(ROOT, 'src', 'main', 'resources', 'assets', 'spacebreaker', 'textures')


def save(cv, kind, name, spec=None, normal=True, wrap=True, invert=False):
    path = os.path.join(TEX, kind, name + '.png')
    cv.save(path)
    if spec is not None:
        smooth, f0, por, rules = spec
        pbr.specular_map(cv.img, smooth, f0, por, rules).save(os.path.join(TEX, kind, name + '_s.png'))
    if normal and spec is not None:
        pbr.normal_map(cv.img, wrap=wrap, invert=invert).save(os.path.join(TEX, kind, name + '_n.png'))
    return path


def main():
    out = []
    # ---- Moon blocks
    out.append(save(moon.mare_basalt(), 'block', 'mare_basalt', (40, 12, 20, None)))
    out.append(save(moon.lunar_regolith(0), 'block', 'lunar_regolith', (18, 10, 64, None)))
    out.append(save(moon.lunar_regolith(1), 'block', 'lunar_regolith_1', (18, 10, 64, None)))
    for side in ('left', 'right'):
        out.append(save(moon.lunar_regolith_footprint(side), 'block', f'lunar_regolith_footprint_{side}', (22, 10, 64, None)))

    # ---- Ore overlays (host stone texture is the vanilla/own rock underneath)
    out.append(save(ores.raw_bauxite_block(), 'block', 'raw_bauxite_block', (40, 10, 50, None)))
    out.append(save(ores.bauxite_overlay(), 'block', 'bauxite_ore_overlay', (55, 18, 30, None), wrap=False))
    ilm_rules = lambda c: (235, 230, 0, 255) if c[3] and sum(c[:3]) > 300 else ((200, 230, 0, 255) if c[3] else None)
    out.append(save(ores.ilmenite_overlay(), 'block', 'ilmenite_ore_overlay', (180, 230, 0, ilm_rules), wrap=False))

    # ---- Solar panel
    cell_rules = lambda c: (235, 10, 0, 255) if (c[2] > c[0] + 20) else ((150, 232, 0, 255) if c[0] > 150 and c[2] > 150 and abs(c[0] - c[2]) < 30 and c[0] < 240 else None)
    out.append(save(machines.solar_cells(), 'block', 'solar_panel_cells', (90, 20, 0, cell_rules), wrap=False))
    out.append(save(machines.solar_back(), 'block', 'solar_panel_back', (60, 20, 0, None), wrap=False))
    out.append(save(machines.solar_frame(), 'block', 'solar_panel_frame', (170, 232, 0, None)))
    out.append(save(machines.steel_mount(), 'block', 'solar_panel_steel', (120, 230, 0, None)))
    out.append(save(machines.solar_base_top(), 'block', 'solar_panel_base_top', (120, 230, 0, None), wrap=False))
    out.append(save(machines.solar_housing(), 'block', 'solar_panel_housing', (120, 230, 0, None), wrap=False))

    # ---- Items
    out.append(save(items.ingot(P.ALUMINIUM), 'item', 'aluminum_ingot'))
    out.append(save(items.plate(P.ALUMINIUM), 'item', 'aluminum_plate'))
    out.append(save(items.raw_bauxite(), 'item', 'raw_bauxite'))
    out.append(save(items.oxygen_tank_icon(), 'item', 'oxygen_tank'))
    out.append(save(machines.oxygen_tank_model(), 'item', 'oxygen_tank_model'))
    out.append(save(items.eva_helmet_icon(), 'item', 'eva_helmet'))

    # ---- Suit model sheets
    out.append(save(suit.eva_helmet(), 'models/armor', 'eva_helmet'))
    out.append(save(suit.eva_helmet_visor(), 'models/armor', 'eva_helmet_visor'))

    print(f'{len(out)} textures written to {TEX}')


if __name__ == '__main__':
    main()
