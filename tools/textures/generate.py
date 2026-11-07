"""Generates every Spacebreaker texture into src/main/resources/assets/spacebreaker/textures.

    python3 tools/textures/generate.py            # everything
    python3 tools/textures/generate.py moon suit  # only some modules

Shared drawing code lives in sbtex/ (canvas, noise, shading, palettes, PBR, generic shapes).
Each feature area owns one file in modules/ with an ``export(save)`` function; new files are
picked up automatically, so parallel work never touches this script.
"""
import importlib
import os
import pkgutil
import sys

HERE = os.path.dirname(os.path.abspath(__file__))
sys.path.insert(0, HERE)

from sbtex import pbr  # noqa: E402

ROOT = os.path.abspath(os.path.join(HERE, '..', '..'))
TEX = os.path.join(ROOT, 'src', 'main', 'resources', 'assets', 'spacebreaker', 'textures')
written = []


def save(cv, kind, name, spec=None, normal=True, wrap=True, invert=False):
    """Write <kind>/<name>.png; with spec=(smoothness, f0, porosity, rules) also LabPBR _s/_n maps."""
    path = os.path.join(TEX, kind, name + '.png')
    cv.save(path)
    if spec is not None:
        smooth, f0, por, rules = spec
        pbr.specular_map(cv.img, smooth, f0, por, rules).save(os.path.join(TEX, kind, name + '_s.png'))
        if normal:
            pbr.normal_map(cv.img, wrap=wrap, invert=invert).save(os.path.join(TEX, kind, name + '_n.png'))
    written.append(path)
    return path


def main(selected):
    import modules
    for info in pkgutil.iter_modules(modules.__path__):
        if selected and info.name not in selected:
            continue
        mod = importlib.import_module(f'modules.{info.name}')
        if hasattr(mod, 'export'):
            before = len(written)
            mod.export(save)
            print(f'  {info.name}: {len(written) - before} textures')
    # finishing passes run after all modules (e.g. textures derived from other modules' output)
    for info in pkgutil.iter_modules(modules.__path__):
        mod = importlib.import_module(f'modules.{info.name}')
        if hasattr(mod, 'finish'):
            before = len(written)
            mod.finish(save)
            print(f'  {info.name} (finish): {len(written) - before} textures')
    print(f'{len(written)} textures written to {TEX}')


if __name__ == '__main__':
    main(set(sys.argv[1:]))
