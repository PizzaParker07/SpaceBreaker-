"""Generates structure templates (.nbt) into src/main/resources/data/spacebreaker/structure.

    python3 tools/structures/gen_structures.py

Each area owns one file in modules/ with a ``generate(out)`` function, where ``out(name, template)`` saves
data/spacebreaker/structure/<name>.nbt. New module files are discovered automatically.
"""
import importlib
import os
import pkgutil
import sys

HERE = os.path.dirname(os.path.abspath(__file__))
sys.path.insert(0, HERE)
ROOT = os.path.abspath(os.path.join(HERE, '..', '..'))
OUT = os.path.join(ROOT, 'src', 'main', 'resources', 'data', 'spacebreaker', 'structure')


def main():
    import modules
    total = 0

    def out(name, template):
        nonlocal total
        n, p = template.save(os.path.join(OUT, name + '.nbt'))
        total += 1
        print(f'  {name}: {n} blocks, {p} states, size {template.size}')

    for info in pkgutil.iter_modules(modules.__path__):
        mod = importlib.import_module(f'modules.{info.name}')
        if hasattr(mod, 'generate'):
            mod.generate(out)
    print(f'{total} structure(s) written')


if __name__ == '__main__':
    main()
