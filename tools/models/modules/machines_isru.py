"""Off-world industry (ISRU) machine models; helpers come from the Earth machines module."""
from sbmodels.common import *  # noqa: F401,F403
from modules.machines_industry import box, glow, machine, casing_tex, body


def generate():
    # atmosphere processor: compressor body, fan plenum on top, intake stack with a rain cap
    t = casing_tex('atmosphere_processor_front', top='atmosphere_processor_top', side='atmosphere_processor_side')
    t['steel'] = b('press_steel')
    machine('atmosphere_processor', t, [
        body(12, top='bottom'),
        box([1, 12, 1], [15, 14, 15], {'north': 'bottom', 'south': 'bottom', 'east': 'bottom', 'west': 'bottom', 'up': 'top', 'down': 'bottom'}),
        box([11, 14, 11], [14, 20, 14], 'steel'),
        box([10, 20, 10], [15, 21, 15], 'steel'),
    ])

    # Sabatier reactor: casing with a sight glass, catalyst vessel lying on top between two flanges
    t = casing_tex('sabatier_reactor_front')
    t['tube'] = b('sabatier_reactor_tube')
    t['steel'] = b('press_steel')
    t['glow'] = b('sabatier_reactor_glow')
    machine('sabatier_reactor', t, [
        body(10, top='top'),
        box([2, 10, 4], [14, 15, 12], {'north': 'tube', 'south': 'tube', 'up': 'tube', 'down': 'tube', 'east': 'steel', 'west': 'steel'},
            uv={'north': [0, 3, 12, 8], 'south': [0, 3, 12, 8], 'up': [0, 0, 12, 8], 'down': [0, 0, 12, 8]}),
        box([0, 9, 3], [2, 16, 13], 'steel'), box([14, 9, 3], [16, 16, 13], 'steel'),
        box([7, 15, 7], [9, 17, 9], 'steel'),
    ], [glow([0, 0, -0.01], [16, 10, 0], 'north', 'glow')])

    # regolith heater: refractory crucible furnace with a feed hopper
    t = casing_tex('regolith_heater_front', top='crusher_top')
    t['glow'] = b('regolith_heater_glow')
    machine('regolith_heater', t, [
        body(13, top='side'),
        box([1, 13, 1], [15, 16, 15], {'north': 'side', 'south': 'side', 'east': 'side', 'west': 'side', 'up': 'top', 'down': 'bottom'}),
    ], [glow([0, 0, -0.01], [16, 13, 0], 'north', 'glow')])

    # ice melter: plinth with heating coil, one wide glass tank, manifold on top
    t = casing_tex('machine_casing')
    t['tank'] = b('ice_melter_tank')
    t['steel'] = b('press_steel')
    t['glow'] = b('ice_melter_glow')
    machine('ice_melter', t, [
        box([0, 0, 0], [16, 3, 16], {'north': 'side', 'south': 'side', 'east': 'side', 'west': 'side', 'up': 'bottom', 'down': 'bottom'},
            uv={f: [0, 13, 16, 16] for f in ('north', 'south', 'east', 'west')}),
        box([1, 3, 2], [15, 13, 10], 'tank'),
        box([0, 3, 10], [16, 13, 16], {'north': 'side', 'south': 'side', 'east': 'side', 'west': 'side'}),
        box([0, 13, 1], [16, 16, 16], {'north': 'steel', 'south': 'side', 'east': 'side', 'west': 'side', 'up': 'top', 'down': 'steel'}),
    ], [glow([0, 0, -0.01], [16, 3, 0], 'north', 'glow')])


_generate_base = generate


def generate():  # noqa: F811
    _generate_base()
    t = casing_tex('chemical_reactor_front')
    t['vessel'] = b('chemical_reactor_vessel')
    t['steel'] = b('press_steel')
    machine('chemical_reactor', t, [
        body(9, top='top'),
        box([2, 9, 2], [14, 16, 14], {'north': 'vessel', 'south': 'vessel', 'east': 'vessel', 'west': 'vessel', 'up': 'steel', 'down': 'steel'},
            uv={f: [2, 0, 14, 7] for f in ('north', 'south', 'east', 'west')}),
        box([3, 16, 3], [13, 17, 13], 'steel'),
        box([6, 17, 6], [10, 21, 10], 'steel'),
        box([12, 13, 0], [14, 15, 2], 'steel'),
    ])


_generate_isru = generate


def generate():  # noqa: F811
    _generate_isru()
    # RTG: plinth, finned radiator body with a warning plate, bolted cap
    t = {'front': b('rtg_front'), 'side': b('rtg_side'), 'top': b('rtg_top'), 'bottom': b('machine_bottom'),
         'steel': b('press_steel'), 'particle': b('rtg_side')}
    machine('rtg', t, [
        box([0, 0, 0], [16, 2, 16], {'north': 'steel', 'south': 'steel', 'east': 'steel', 'west': 'steel', 'up': 'bottom', 'down': 'bottom'},
            uv={f: [0, 14, 16, 16] for f in ('north', 'south', 'east', 'west')}),
        box([1, 2, 1], [15, 14, 15], {'north': 'front', 'south': 'side', 'east': 'side', 'west': 'side', 'up': 'top', 'down': 'bottom'},
            uv={f: [1, 2, 15, 14] for f in ('north', 'south', 'east', 'west')}),
        box([3, 14, 3], [13, 16, 13], {'north': 'steel', 'south': 'steel', 'east': 'steel', 'west': 'steel', 'up': 'top', 'down': 'bottom'}),
    ])
    write('models/item/rtg_fuel_pellet.json', {'parent': 'minecraft:item/generated', 'textures': {'layer0': i('rtg_fuel_pellet')}})


_generate_excavator = generate


def generate():  # noqa: F811
    _generate_excavator()
    # regolith excavator: casing with the bucket wheel in front and a drill string below
    t = casing_tex('regolith_excavator_front', top='crusher_top')
    t['steel'] = b('press_steel')
    machine('regolith_excavator', t, [
        body(14, top='top'),
        box([1, 14, 1], [15, 16, 15], {'north': 'side', 'south': 'side', 'east': 'side', 'west': 'side', 'up': 'top', 'down': 'bottom'},
            uv={f: [1, 0, 15, 2] for f in ('north', 'south', 'east', 'west')}),
    ])


_generate_sinter = generate


def generate():  # noqa: F811
    _generate_sinter()
    t = casing_tex('sintering_furnace_front', top='crusher_top')
    t['glow'] = b('sintering_furnace_glow')
    machine('sintering_furnace', t, [body(16, top='top')], [glow([0, 0, -0.01], [16, 16, 0], 'north', 'glow')])
