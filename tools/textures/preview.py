"""Dev preview: composes textures into an upscaled contact sheet (+ 3x3 tiling check).
Vanilla textures are read from the local Minecraft resources jar for compositing only;
they are never copied into the repository."""
import os
import sys
import zipfile
from io import BytesIO

from PIL import Image, ImageDraw

HERE = os.path.dirname(os.path.abspath(__file__))
ROOT = os.path.abspath(os.path.join(HERE, '..', '..'))
VANILLA_JAR = os.path.join(ROOT, 'build', 'moddev', 'artifacts', 'neoforge-21.1.265-client-extra-aka-minecraft-resources.jar')


def vanilla(path: str) -> Image.Image:
    with zipfile.ZipFile(VANILLA_JAR) as z:
        return Image.open(BytesIO(z.read(f'assets/minecraft/textures/{path}.png'))).convert('RGBA')


def tile3(img: Image.Image) -> Image.Image:
    out = Image.new('RGBA', (img.width * 3, img.height * 3))
    for i in range(3):
        for j in range(3):
            out.paste(img, (i * img.width, j * img.height))
    return out


def sheet(items, scale=10, cols=6, bg=(40, 44, 52), label_h=14):
    cell = 16 * scale * 3 // 2
    w = cols * (cell + 10) + 10
    rows = (len(items) + cols - 1) // cols
    h = rows * (cell + label_h + 14) + 10
    out = Image.new('RGB', (w, h), bg)
    d = ImageDraw.Draw(out)
    try:
        from PIL import ImageFont
        font = ImageFont.truetype('/usr/share/fonts/truetype/dejavu/DejaVuSans.ttf', 13)
    except OSError:
        font = None
    for i, (name, img) in enumerate(items):
        cx = 10 + (i % cols) * (cell + 10)
        cy = 10 + (i // cols) * (cell + label_h + 14)
        k = max(1, cell // max(img.width, img.height))
        im = img.resize((img.width * k, img.height * k), Image.NEAREST)
        out.paste(im, (cx, cy), im)
        d.text((cx, cy + cell + 2), name, fill=(235, 235, 235), font=font)
    return out
