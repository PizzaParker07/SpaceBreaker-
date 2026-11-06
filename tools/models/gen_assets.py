"""Generates blockstates and block/item models (assets) for Spacebreaker.

    python3 tools/models/gen_assets.py

Shared helpers are in sbmodels/common.py. Each feature area owns one file in modules/ with a
``generate()`` function; new files are discovered automatically.
(Data files — loot tables, tags, recipes, language — come from Java datagen.)
"""
import importlib
import os
import pkgutil
import sys

HERE = os.path.dirname(os.path.abspath(__file__))
sys.path.insert(0, HERE)

from sbmodels import common  # noqa: E402


def main():
    import modules
    for info in pkgutil.iter_modules(modules.__path__):
        mod = importlib.import_module(f'modules.{info.name}')
        if hasattr(mod, 'generate'):
            before = len(common.written)
            mod.generate()
            print(f'  {info.name}: {len(common.written) - before} files')
    print(f'{len(common.written)} asset files written')


if __name__ == '__main__':
    main()
