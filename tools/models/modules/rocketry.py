"""Rocketry: rocket part models (3D in hand / on the rocket, 2D icon in the GUI).

Every axial part is a stack of "pixel-circle" prisms: a round body built from a few axis-aligned
boxes whose union is a chunky pixel-art circle (no rotated boxes, whole pixels only, like vanilla).
The body is 32 px (two blocks) across, centred on x = z = 8; y = 0 is the bottom of the part.

UVs are projected the way vanilla projects auto-UVs onto blocks, so a part's texture sheet is simply
a set of orthographic views of the part:

  axial parts (64x64 sheet)            radial parts (64x64 sheet, mounted on the north side)
    side   (0,0)   32 x 32  any side     front   (0,0)   32 x 48  north/south view, y in [-16, 32]
    top    (32,0)  32 x 32  from above   profile (32,0)  16 x 48  east/west view,  z in [-16, 0]
    bottom (32,32) 32 x 32  from below   top     (0,48)  32 x 16  from above
    aux    (0,32)  32 x 32  extra view   bottom  (32,48) 32 x 16  from below

The texture painter (tools/textures/modules/rocketry.py) imports PARTS from here, so geometry and
texture always agree.
"""
import math

from sbmodels.common import *  # noqa: F401,F403

SHEET = 64
TEX = 'spacebreaker:block/rocket/'


def circle_boxes(r, step=1):
    """Half-extents (hx, hz) of boxes whose union is a pixel circle of radius r (in px).
    step = 2 draws the circle on a 2-px grid (chunkier, fewer boxes) for big radii."""
    rr = r / step
    rows = int(math.ceil(rr))
    widths = []
    for k in range(rows):
        w = math.sqrt(max(0.0, rr * rr - (k + 0.5) ** 2))
        widths.append(max(1, int(round(w))))
    boxes = []
    for w in sorted(set(widths), reverse=True):
        hz = sum(1 for x in widths if x >= w)
        boxes.append((w * step, hz * step))
    return boxes


def _uv(u0, v0, u1, v1):
    k = 16.0 / SHEET
    return [round(u0 * k, 4), round(v0 * k, 4), round(u1 * k, 4), round(v1 * k, 4)]


AXIAL_REGIONS = {'side': (0, 0), 'top': (32, 0), 'bottom': (32, 32), 'aux': (0, 32)}


def axial_uv(face, fr, to, region='side', top_region='top', bottom_region='bottom'):
    """Projected UV of a face of an axial part (see module docstring). Side regions use v = 32 - y."""
    x0, y0, z0 = fr
    x1, y1, z1 = to
    if face in ('north', 'south', 'east', 'west'):
        ox, oy = AXIAL_REGIONS[region]
        v0, v1 = 32 - y1, 32 - y0
        if face == 'north':
            u0, u1 = 24 - x1, 24 - x0
        elif face == 'south':
            u0, u1 = x0 + 8, x1 + 8
        elif face == 'east':
            u0, u1 = 24 - z1, 24 - z0
        else:
            u0, u1 = z0 + 8, z1 + 8
    elif face == 'up':
        ox, oy = AXIAL_REGIONS[top_region]
        u0, u1, v0, v1 = x0 + 8, x1 + 8, z0 + 8, z1 + 8
    else:
        ox, oy = AXIAL_REGIONS[bottom_region]
        u0, u1, v0, v1 = x0 + 8, x1 + 8, 24 - z1, 24 - z0
    return _uv(ox + u0, oy + v0, ox + u1, oy + v1)


RADIAL_REGIONS = {'front': (0, 0), 'profile': (32, 0), 'top': (0, 48), 'bottom': (32, 48)}


def radial_uv(face, fr, to):
    """Projected UV of a face of a radial part (north side, z in [-16, 0], y in [-16, 32])."""
    x0, y0, z0 = fr
    x1, y1, z1 = to
    if face in ('north', 'south'):
        ox, oy = RADIAL_REGIONS['front']
        v0, v1 = 32 - y1, 32 - y0
        u0, u1 = (24 - x1, 24 - x0) if face == 'north' else (x0 + 8, x1 + 8)
    elif face in ('east', 'west'):
        ox, oy = RADIAL_REGIONS['profile']
        v0, v1 = 32 - y1, 32 - y0
        u0, u1 = (-z1, -z0) if face == 'east' else (z0 + 16, z1 + 16)
    elif face == 'up':
        ox, oy = RADIAL_REGIONS['top']
        u0, u1, v0, v1 = x0 + 8, x1 + 8, z0 + 16, z1 + 16
    else:
        ox, oy = RADIAL_REGIONS['bottom']
        u0, u1, v0, v1 = x0 + 8, x1 + 8, -z1, -z0
    return _uv(ox + u0, oy + v0, ox + u1, oy + v1)


ALL_FACES = ('north', 'south', 'east', 'west', 'up', 'down')
SIDES = ('north', 'south', 'east', 'west')


def box(fr, to, faces=ALL_FACES, radial=False, **regions):
    el = {'from': [round(v, 4) for v in fr], 'to': [round(v, 4) for v in to], 'faces': {}}
    for f in faces:
        uv = radial_uv(f, fr, to) if radial else axial_uv(f, fr, to, **regions)
        el['faces'][f] = {'texture': '#tex', 'uv': uv}
    return el


def prism(y0, y1, r, step=None, top=True, bottom=True, **regions):
    """Round body of radius r between y0 and y1."""
    if step is None:
        step = 2 if r >= 12 else 1
    faces = SIDES + (('up',) if top else ()) + (('down',) if bottom else ())
    els = []
    for hx, hz in circle_boxes(r, step):
        els.append(box([8 - hx, y0, 8 - hz], [8 + hx, y1, 8 + hz], faces, **regions))
    return els


# ----------------------------------------------------------------------------- axial parts

def tank(h):
    return prism(0, h, 16)


def kerosene_engine():
    els = []
    els += prism(16, 20, 16)                              # thrust structure / aft skirt
    els += prism(13, 16, 7, top=False)                    # thrust cone + gimbal
    els += prism(12, 13, 4, top=False, bottom=False)      # throat
    els += prism(10, 12, 5, top=False, bottom=False)      # short sea-level bell
    els += prism(7, 10, 7, top=False, bottom=False)
    els += prism(3, 7, 8, top=False, bottom=False)
    els += prism(0, 3, 9, top=False)
    # turbopump and gas generator exhaust on the side (aux view)
    els.append(box([13, 9, 5], [17, 15, 9], region='aux', top_region='top', bottom_region='bottom'))
    els.append(box([14, 4, 6], [16, 9, 8], region='aux'))
    return els


def vacuum_engine():
    els = []
    els += prism(24, 28, 16)                              # mount ring
    els += prism(21, 24, 7, top=False)                    # thrust cone
    els += prism(19, 21, 4, top=False, bottom=False)      # throat
    els += prism(16, 19, 6, top=False, bottom=False)      # long vacuum bell
    els += prism(12, 16, 8, top=False, bottom=False)
    els += prism(8, 12, 10, top=False, bottom=False)
    els += prism(4, 8, 12, top=False, bottom=False)
    els += prism(0, 4, 13, top=False)
    els.append(box([13, 19, 5], [17, 24, 9], region='aux'))
    return els


def methalox_engine():
    """Methane sea-level engine: compact steel bell, two turbopumps (one each for methane and oxygen)."""
    els = []
    els += prism(16, 20, 16)                              # thrust structure / aft skirt
    els += prism(14, 16, 6, top=False)                    # powerhead
    els += prism(12, 14, 4, top=False, bottom=False)      # throat
    els += prism(9, 12, 6, top=False, bottom=False)       # regeneratively cooled bell
    els += prism(5, 9, 7, top=False, bottom=False)
    els += prism(2, 5, 8, top=False, bottom=False)
    els += prism(0, 2, 9, top=False)
    els.append(box([13, 10, 5], [17, 16, 9], region='aux', top_region='top', bottom_region='bottom'))
    els.append(box([-1, 10, 7], [3, 16, 11], region='aux', top_region='top', bottom_region='bottom'))
    return els


def methalox_vacuum_engine():
    """Methane vacuum engine: very wide bell with a dark radiatively cooled extension."""
    els = []
    els += prism(26, 30, 16)                              # mount ring
    els += prism(23, 26, 6, top=False)                    # powerhead
    els += prism(21, 23, 4, top=False, bottom=False)      # throat
    els += prism(18, 21, 7, top=False, bottom=False)
    els += prism(14, 18, 9, top=False, bottom=False)
    els += prism(10, 14, 11, top=False, bottom=False)
    els += prism(5, 10, 13, top=False, bottom=False)
    els += prism(0, 5, 14, top=False)
    els.append(box([13, 21, 5], [17, 26, 9], region='aux'))
    els.append(box([-1, 21, 7], [3, 26, 11], region='aux'))
    return els


def decoupler():
    return prism(0, 3, 16)


def interstage():
    return prism(0, 28, 16)


def heat_shield():
    return prism(1, 3, 16) + prism(0, 1, 14, top=False)


CAPSULE_PROFILE = [(0, 4, 16), (4, 8, 15), (8, 12, 14), (12, 16, 12), (16, 20, 11), (20, 23, 9), (23, 26, 7)]


def crew_capsule():
    els = []
    for y0, y1, r in CAPSULE_PROFILE:
        # the front (north) side uses the aux view: hatch and window
        for hx, hz in circle_boxes(r, 2 if r >= 12 else 1):
            fr, to = [8 - hx, y0, 8 - hz], [8 + hx, y1, 8 + hz]
            el = box(fr, to, ('south', 'east', 'west', 'up', 'down'))
            el['faces']['north'] = {'texture': '#tex', 'uv': axial_uv('north', fr, to, region='aux')}
            els.append(el)
    return els


def parachute():
    return prism(0, 5, 6) + prism(5, 6, 4, bottom=False)


NOSE_PROFILE = [(0, 4, 16), (4, 8, 15), (8, 11, 13), (11, 14, 11), (14, 17, 9), (17, 20, 6), (20, 22, 4), (22, 24, 2)]


def nose_cone():
    els = []
    for y0, y1, r in NOSE_PROFILE:
        els += prism(y0, y1, r, top=True, bottom=(y0 == 0))
    return els


# ----------------------------------------------------------------------------- radial parts (north side)

def fins():
    """Swept delta fin; root along the body (z = -8), tip 8 px out. y = 0 is the top of the engine."""
    els = []
    for z0, z1, y0, y1 in [(-10, -8, -4, 16), (-12, -10, -7, 12), (-14, -12, -10, 8), (-16, -14, -12, 4)]:
        els.append(box([7, y0, z0], [9, y1, z1], radial=True))
    return els


def grid_fins():
    """Hinge bracket and a lattice paddle; y = 0 is the top of the stage."""
    return [
        box([6, -6, -10], [10, -2, -8], radial=True),
        box([3, -4, -16], [13, -2, -10], radial=True),
    ]


def landing_legs():
    """Folded leg: strut, knee brace and foot pad; y = 0 is the bottom of the stage (offset in the renderer)."""
    return [
        box([6.5, -14, -10], [9.5, 18, -8], radial=True),     # main strut
        box([7, 16, -9], [9, 20, -8], radial=True),           # hinge block
        box([5, -16, -11], [11, -14, -8], radial=True),       # foot pad
    ]


def rcs_thrusters():
    """Quad thruster block; y = 0 is the top of the stage."""
    return [
        box([6, -7, -11], [10, -3, -8], radial=True),          # pod
        box([5, -6, -10], [6, -4, -9], radial=True),           # side nozzles
        box([10, -6, -10], [11, -4, -9], radial=True),
        box([7, -3, -10], [9, -2, -9], radial=True),           # up/down nozzles
        box([7, -8, -10], [9, -7, -9], radial=True),
    ]


# name: (geometry, radial)
PARTS = {
    'fuel_tank_small': (lambda: tank(16), False),
    'fuel_tank_medium': (lambda: tank(24), False),
    'fuel_tank_large': (lambda: tank(32), False),
    'kerosene_engine': (kerosene_engine, False),
    'vacuum_engine': (vacuum_engine, False),
    'methalox_engine': (methalox_engine, False),
    'methalox_vacuum_engine': (methalox_vacuum_engine, False),
    'decoupler': (decoupler, False),
    'interstage': (interstage, False),
    'heat_shield': (heat_shield, False),
    'crew_capsule': (crew_capsule, False),
    'parachute': (parachute, False),
    'nose_cone': (nose_cone, False),
    'fins': (fins, True),
    'grid_fins': (grid_fins, True),
    'landing_legs': (landing_legs, True),
    'rcs_thrusters': (rcs_thrusters, True),
}

# Display transforms: parts are two blocks wide, so they are shrunk in hand, on the ground and in frames.
PART_DISPLAY = {
    'thirdperson_righthand': {'rotation': [75, 45, 0], 'translation': [0, 2.5, 0], 'scale': [0.22, 0.22, 0.22]},
    'thirdperson_lefthand': {'rotation': [75, 45, 0], 'translation': [0, 2.5, 0], 'scale': [0.22, 0.22, 0.22]},
    'firstperson_righthand': {'rotation': [0, 45, 0], 'translation': [0, 2, 0], 'scale': [0.24, 0.24, 0.24]},
    'firstperson_lefthand': {'rotation': [0, 225, 0], 'translation': [0, 2, 0], 'scale': [0.24, 0.24, 0.24]},
    'ground': {'rotation': [0, 0, 0], 'translation': [0, 2, 0], 'scale': [0.2, 0.2, 0.2]},
    'fixed': {'rotation': [0, 0, 0], 'translation': [0, 0, 0], 'scale': [0.4, 0.4, 0.4]},
    'head': {'rotation': [0, 0, 0], 'translation': [0, 13, 0], 'scale': [0.4, 0.4, 0.4]},
}


def part_models():
    for name, (geo, radial) in PARTS.items():
        tex = TEX + name
        write(f'models/item/{name}_3d.json', {
            'parent': 'minecraft:block/block',
            'textures': {'tex': tex, 'particle': tex},
            'elements': geo(),
            'display': PART_DISPLAY,
        })
        write(f'models/item/{name}_icon.json', {'parent': 'minecraft:item/generated', 'textures': {'layer0': i(name)}})
        write(f'models/item/{name}.json', {
            'loader': 'neoforge:separate_transforms',
            'gui_light': 'front',
            'base': {'parent': i(name + '_3d')},
            'perspectives': {'gui': {'parent': i(name + '_icon')}},
        })


def generate():
    part_models()
    generated_item('rocket')


# ----------------------------------------------------------------------------- launch facility and machines

def facing_states(name, model, extra=None):
    """Horizontal facing blockstate (model authored facing north)."""
    variants = {}
    for facing, rot in (('north', 0), ('east', 90), ('south', 180), ('west', 270)):
        if extra:
            for k, v in extra.items():
                variants[f'facing={facing},{k}'] = {'model': v, 'y': rot} if rot else {'model': v}
        else:
            variants[f'facing={facing}'] = {'model': model, 'y': rot} if rot else {'model': model}
    write(f'blockstates/{name}.json', {'variants': variants})


def el(fr, to, tex, faces=ALL_FACES, uv=None, rot=None, light=None, shade=True):
    e = {'from': fr, 'to': to, 'faces': {}}
    for f in faces:
        t = tex[f] if isinstance(tex, dict) else tex
        face = {'texture': '#' + t}
        if uv and f in uv:
            face['uv'] = uv[f]
        e['faces'][f] = face
    if rot:
        e['rotation'] = rot
    if light:
        e['neoforge_data'] = {'block_light': light, 'sky_light': light, 'ambient_occlusion': False}
    if not shade:
        e['shade'] = False
    return e


def launch_facility():
    # pad plating and trench grate: plain cubes
    write('blockstates/launch_pad.json', {'variants': {'': {'model': b('launch_pad')}}})
    write('models/block/launch_pad.json', {'parent': 'minecraft:block/cube_bottom_top', 'textures': {
        'top': b('launch_pad_top'), 'side': b('launch_pad_side'), 'bottom': b('launch_pad_side')}})
    write('models/item/launch_pad.json', {'parent': b('launch_pad')})

    write('blockstates/flame_trench.json', {'variants': {'': {'model': b('flame_trench')}}})
    write('models/block/flame_trench.json', {'parent': 'minecraft:block/cube_bottom_top', 'textures': {
        'top': b('flame_trench_top'), 'side': b('flame_trench_side'), 'bottom': b('launch_pad_side')}})
    write('models/item/flame_trench.json', {'parent': b('flame_trench')})

    write('blockstates/service_tower.json', {'variants': {'': {'model': b('service_tower')}}})
    write('models/block/service_tower.json', {
        'parent': 'minecraft:block/block', 'render_type': 'minecraft:cutout',
        'textures': {'side': b('service_tower'), 'top': b('service_tower_top'), 'particle': b('service_tower')},
        'elements': [
            el([0, 0, 0], [16, 16, 16], {'north': 'side', 'south': 'side', 'east': 'side', 'west': 'side', 'up': 'top', 'down': 'top'}),
            # inner faces so the lattice reads from inside the tower too
            el([0.01, 0, 0.01], [15.99, 16, 15.99], 'side', faces=('north', 'south', 'east', 'west'),
               uv={f: [16, 0, 0, 16] for f in ('north', 'south', 'east', 'west')}),
        ]})
    write('models/item/service_tower.json', {'parent': b('service_tower')})

    # crew access arm: hinge housing (block) + walkway (drawn by the block entity renderer)
    tex = {'steel': b('tower_steel'), 'orange': b('service_tower_top'), 'grate': b('crew_arm_walkway'),
           'rail': b('crew_arm_rail'), 'particle': b('tower_steel')}
    housing = [
        el([2, 0, 10], [14, 2, 16], 'steel'),
        el([2, 2, 12], [6, 14, 16], 'orange'),
        el([3, 14, 11], [5, 16, 13], 'steel'),
        el([10, 2, 13], [14, 6, 16], 'steel'),
    ]
    for state, model in (('extended=true', 'crew_access_arm'), ('extended=false', 'crew_access_arm')):
        pass
    write('models/block/crew_access_arm.json', {'parent': 'minecraft:block/block', 'textures': tex, 'elements': housing})
    walkway = [
        el([3, 1, -12], [13, 2, 12], 'grate'),                                     # floor grating
        el([3, 2, -12], [4, 12, -11], 'rail'), el([12, 2, -12], [13, 12, -11], 'rail'),   # posts
        el([3, 2, 0], [4, 12, 1], 'rail'), el([12, 2, 0], [13, 12, 1], 'rail'),
        el([3, 11, -12], [4, 12, 12], 'rail'), el([12, 11, -12], [13, 12, 12], 'rail'),   # hand rails
        el([3, 6, -12], [4, 7, 12], 'rail'), el([12, 6, -12], [13, 7, 12], 'rail'),
        el([4, 0, -11], [12, 1, 10], 'steel', faces=('down', 'north', 'south', 'east', 'west')),  # truss under the floor
    ]
    write('models/block/crew_access_arm_walkway.json', {'parent': 'minecraft:block/block', 'textures': tex,
                                                        'ambientocclusion': False, 'elements': walkway})
    facing_states('crew_access_arm', b('crew_access_arm'), extra={'extended=true': b('crew_access_arm'), 'extended=false': b('crew_access_arm')})
    write('models/item/crew_access_arm.json', {'parent': 'minecraft:block/block', 'textures': tex, 'elements': housing + walkway,
                                               'display': {'gui': {'rotation': [30, 225, 0], 'translation': [0, 0, 0], 'scale': [0.5, 0.5, 0.5]},
                                                           'fixed': {'rotation': [0, 0, 0], 'translation': [0, 0, 0], 'scale': [0.5, 0.5, 0.5]}}})

    # fuel umbilical
    utex = {'steel': b('tower_steel'), 'front': b('fuel_umbilical_front'), 'hose': b('fuel_umbilical_hose'),
            'orange': b('service_tower_top'), 'particle': b('tower_steel')}
    umb = [
        el([2, 1, 13], [14, 15, 16], 'orange'),                                   # mounting plate on the tower
        el([3, 3, 6], [13, 13, 13], {'north': 'front', 'south': 'steel', 'east': 'steel', 'west': 'steel', 'up': 'steel', 'down': 'steel'}),
        el([5, 6, -6], [8, 9, 6], 'hose'),                                         # RP-1 line
        el([9, 6, -6], [12, 9, 6], 'hose'),                                        # LOX line
        el([4, 5, -8], [13, 10, -6], {'north': 'front', 'south': 'steel', 'east': 'steel', 'west': 'steel', 'up': 'steel', 'down': 'steel'}),  # quick-disconnect plate
    ]
    write('models/block/fuel_umbilical.json', {'parent': 'minecraft:block/block', 'textures': utex, 'elements': umb})
    facing_states('fuel_umbilical', b('fuel_umbilical'))
    write('models/item/fuel_umbilical.json', {'parent': b('fuel_umbilical')})

    # launch control console
    ctex = {'body': b('console_body'), 'top': b('console_top'), 'screen': b('console_screen'), 'panel': b('console_panel'),
            'particle': b('console_body')}
    console = [
        el([1, 0, 1], [15, 9, 15], {'north': 'panel', 'south': 'body', 'east': 'body', 'west': 'body', 'up': 'top', 'down': 'body'}),
        el([1, 9, 9], [15, 16, 15], {'north': 'screen', 'south': 'body', 'east': 'body', 'west': 'body', 'up': 'body', 'down': 'body'},
           uv={'north': [1, 0, 15, 7]}, light=None),
        el([2, 10, 8.9], [14, 15.4, 9], {'north': 'screen'}, faces=('north',), uv={'north': [1, 8, 15, 14]}, light=15, shade=False),
    ]
    write('models/block/launch_control_console.json', {'parent': 'minecraft:block/block', 'textures': ctex, 'elements': console})
    facing_states('launch_control_console', b('launch_control_console'))
    write('models/item/launch_control_console.json', {'parent': b('launch_control_console')})

    # rocket assembler: a heavy workbench with a blueprint top
    atex = {'top': b('rocket_assembler_top'), 'front': b('rocket_assembler_front'), 'side': b('rocket_assembler_side'),
            'bottom': b('launch_pad_side'), 'particle': b('rocket_assembler_side')}
    write('models/block/rocket_assembler.json', {'parent': 'minecraft:block/orientable_with_bottom', 'textures': atex})
    facing_states('rocket_assembler', b('rocket_assembler'))
    write('models/item/rocket_assembler.json', {'parent': b('rocket_assembler')})


def canopy():
    """Parachute canopy (drawn above a descending capsule): a shallow stepped dome with alternating gores."""
    els = []
    for y0, y1, r in [(0, 2, 16), (2, 4, 14), (4, 6, 11), (6, 7, 7)]:
        for hx, hz in circle_boxes(r, 2 if r >= 12 else 1):
            els.append(el([8 - hx, y0, 8 - hz], [8 + hx, y1, 8 + hz], 'tex',
                          uv={'up': [(8 - hx + 8) / 2, (8 - hz + 8) / 2, (8 + hx + 8) / 2, (8 + hz + 8) / 2],
                              'down': [(8 - hx + 8) / 2, (8 - hz + 8) / 2, (8 + hx + 8) / 2, (8 + hz + 8) / 2],
                              **{f: [0, 0, 16, 2] for f in SIDES}}))
    # suspension lines down to the capsule
    for x, z in ((-6, -6), (22, -6), (-6, 22), (22, 22)):
        pass
    write('models/item/parachute_canopy.json', {'parent': 'minecraft:block/block', 'textures': {'tex': b('rocket/parachute_canopy'),
                                                                                                'particle': b('rocket/parachute_canopy')},
                                                'elements': els})


def extra_items():
    generated_item('propellant_cartridge')
    generated_item('emergency_beacon')


_old_generate = generate


def generate():  # noqa: F811
    _old_generate()
    launch_facility()
    canopy()
    extra_items()
