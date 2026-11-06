"""LabPBR 1.3 maps for shader packs (Iris/OptiFine): <name>_n.png and <name>_s.png.

_n: R,G = normal XY (128 = flat), B = ambient occlusion (255 = none), A = height (255 = top)
_s: R = perceptual smoothness, G = F0 (0-229 linear) or metal id (230-255),
    B = porosity (0-64) / subsurface (65-255), A = emission (0-254, 255 = none)
"""
import math

from PIL import Image

METAL = {'iron': 230, 'gold': 231, 'aluminium': 232, 'chrome': 233, 'copper': 234}


def _lum(c):
    return (0.299 * c[0] + 0.587 * c[1] + 0.114 * c[2]) / 255.0


def normal_map(img: Image.Image, strength: float = 2.5, wrap: bool = True, invert: bool = False) -> Image.Image:
    w, h = img.size
    px = img.load()
    H = [[(1 - _lum(px[x, y])) if invert else _lum(px[x, y]) for x in range(w)] for y in range(h)]

    def g(x, y):
        if wrap:
            return H[y % h][x % w]
        x = min(max(x, 0), w - 1)
        y = min(max(y, 0), h - 1)
        return H[y][x]

    out = Image.new('RGBA', (w, h))
    o = out.load()
    for y in range(h):
        for x in range(w):
            dx = (g(x + 1, y) - g(x - 1, y)) * strength
            dy = (g(x, y + 1) - g(x, y - 1)) * strength
            nx, ny, nz = -dx, -dy, 1.0
            ln = math.sqrt(nx * nx + ny * ny + nz * nz)
            nx, ny = nx / ln, ny / ln
            ao = int(200 + 55 * H[y][x])
            o[x, y] = (int((nx * 0.5 + 0.5) * 255), int((ny * 0.5 + 0.5) * 255), ao, int(128 + 127 * H[y][x]))
    return out


def specular_map(img: Image.Image, smooth: int, f0: int, porosity: int = 0, rules=None) -> Image.Image:
    """rules: optional function (rgba) -> (smooth, f0, porosity, emission) or None to keep defaults."""
    w, h = img.size
    px = img.load()
    out = Image.new('RGBA', (w, h))
    o = out.load()
    for y in range(h):
        for x in range(w):
            c = px[x, y]
            s, f, p, e = smooth, f0, porosity, 255
            if rules:
                r = rules(c)
                if r:
                    s, f, p, e = r
            o[x, y] = (s, f, p, e)
    return out
