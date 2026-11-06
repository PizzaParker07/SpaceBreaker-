"""Checks textures against docs/STYLE.md limits (colour counts). Warnings only.

    python3 tools/textures/lint.py
"""
import glob
import os

from PIL import Image

ROOT = os.path.abspath(os.path.join(os.path.dirname(__file__), '..', '..'))
TEX = os.path.join(ROOT, 'src', 'main', 'resources', 'assets', 'spacebreaker', 'textures')
LIMITS = {'block': 6, 'item': 9}


def main():
    warnings = 0
    for kind, limit in LIMITS.items():
        for path in sorted(glob.glob(os.path.join(TEX, kind, '**', '*.png'), recursive=True)):
            name = os.path.basename(path)
            if name.endswith(('_n.png', '_s.png')):
                continue
            img = Image.open(path).convert('RGBA')
            if img.width > 16 or img.height > 16:
                continue  # model sheets / animations are reviewed visually
            colours = {p for p in img.getdata() if p[3] > 0}
            is_overlay = 'overlay' in name
            lim = 4 if is_overlay else limit
            if len(colours) > lim:
                warnings += 1
                print(f'WARN {kind}/{name}: {len(colours)} colours (style limit {lim})')
    print(f'{warnings} warning(s)')


if __name__ == '__main__':
    main()
