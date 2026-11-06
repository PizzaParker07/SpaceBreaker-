"""Shared helpers for writing blockstates and models."""
import json
import os

ROOT = os.path.abspath(os.path.join(os.path.dirname(__file__), '..', '..', '..'))
ASSETS = os.path.join(ROOT, 'src', 'main', 'resources', 'assets', 'spacebreaker')
NS = 'spacebreaker'
written = []


def write(rel, obj):
    path = os.path.join(ASSETS, rel)
    os.makedirs(os.path.dirname(path), exist_ok=True)
    with open(path, 'w', encoding='utf-8') as f:
        json.dump(obj, f, indent=2)
        f.write('\n')
    written.append(rel)


def b(name):
    return f'{NS}:block/{name}'


def i(name):
    return f'{NS}:item/{name}'


def cube(fr, to, tex, uv=None, faces=('north', 'south', 'east', 'west', 'up', 'down'), rot=None, cull=False, per_face=None):
    """One model element. tex: texture key or dict face->key; per_face: dict face->uv."""
    el = {'from': fr, 'to': to, 'faces': {}}
    for f in faces:
        key = tex[f] if isinstance(tex, dict) else tex
        face = {'texture': '#' + key}
        if per_face and f in per_face:
            face['uv'] = per_face[f]
        elif uv:
            face['uv'] = uv
        if cull:
            face['cullface'] = f
        el['faces'][f] = face
    if rot:
        el['rotation'] = rot
    return el


# ----------------------------------------------------------------------------- generic parents

def ore_overlay_parent():
    full = ['north', 'south', 'east', 'west', 'up', 'down']
    write('models/block/ore_overlay.json', {
        'parent': 'minecraft:block/block',
        'render_type': 'minecraft:cutout_mipped',
        'textures': {'particle': '#stone'},
        'elements': [
            cube([0, 0, 0], [16, 16, 16], 'stone', faces=full, cull=True),
            cube([0, 0, 0], [16, 16, 16], 'overlay', faces=full, cull=True),
        ],
    })


def simple_block(name, texture=None):
    write(f'blockstates/{name}.json', {'variants': {'': {'model': b(name)}}})
    write(f'models/block/{name}.json', {'parent': 'minecraft:block/cube_all', 'textures': {'all': texture or b(name)}})
    write(f'models/item/{name}.json', {'parent': b(name)})


def ore(name, stone, overlay):
    write(f'blockstates/{name}.json', {'variants': {'': {'model': b(name)}}})
    write(f'models/block/{name}.json', {'parent': b('ore_overlay'), 'textures': {'stone': stone, 'overlay': b(overlay)}})
    write(f'models/item/{name}.json', {'parent': b(name)})


def generated_item(name, texture=None):
    write(f'models/item/{name}.json', {'parent': 'minecraft:item/generated', 'textures': {'layer0': texture or i(name)}})


