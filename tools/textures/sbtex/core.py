"""
Spacebreaker texture toolkit — every texture in the mod is drawn by code, pixel by pixel.

Conventions
-----------
* Light always comes from the top-left.
* Every material owns a colour ramp (dark -> light, 5-7 tones). Pixels are always
  quantised onto a ramp, never free colours, so the palette stays coherent.
* Block textures tile seamlessly (all noise wraps around the 16x16 torus).
"""
from __future__ import annotations

import math
import random
from typing import Callable, Iterable, Sequence

from PIL import Image

RGB = tuple[int, int, int]
RGBA = tuple[int, int, int, int]
TRANSPARENT: RGBA = (0, 0, 0, 0)


# --------------------------------------------------------------------------- colour

def hexc(s: str) -> RGB:
    s = s.lstrip('#')
    return int(s[0:2], 16), int(s[2:4], 16), int(s[4:6], 16)


def ramp(*hexes: str) -> list[RGB]:
    return [hexc(h) for h in hexes]


def mix(a: Sequence[int], b: Sequence[int], t: float) -> RGB:
    return tuple(int(round(a[i] + (b[i] - a[i]) * t)) for i in range(3))  # type: ignore


def rgba(c: Sequence[int], a: int = 255) -> RGBA:
    return int(c[0]), int(c[1]), int(c[2]), a


def pick(r: Sequence[RGB], v: float) -> RGB:
    """v in [0,1] -> ramp colour."""
    i = int(round(max(0.0, min(1.0, v)) * (len(r) - 1)))
    return r[i]


# --------------------------------------------------------------------------- canvas

class Canvas:
    def __init__(self, w: int = 16, h: int = 16, fill: RGBA = TRANSPARENT):
        self.w, self.h = w, h
        self.img = Image.new('RGBA', (w, h), fill)
        self.p = self.img.load()

    def __getitem__(self, xy):
        x, y = xy
        return self.p[x % self.w, y % self.h]

    def __setitem__(self, xy, c):
        x, y = xy
        if len(c) == 3:
            c = rgba(c)
        self.p[x % self.w, y % self.h] = tuple(int(v) for v in c)

    def inside(self, x, y) -> bool:
        return 0 <= x < self.w and 0 <= y < self.h

    def put(self, x, y, c):
        if self.inside(x, y):
            self[x, y] = c

    def alpha(self, x, y) -> int:
        if not self.inside(x, y):
            return 0
        return self.p[x, y][3]

    def copy(self) -> 'Canvas':
        c = Canvas(self.w, self.h)
        c.img = self.img.copy()
        c.p = c.img.load()
        return c

    def paste(self, other: 'Canvas', ox: int = 0, oy: int = 0):
        """Alpha-over paste (binary alpha, pixel-art friendly)."""
        for y in range(other.h):
            for x in range(other.w):
                c = other.p[x, y]
                if c[3] > 0:
                    self.put(x + ox, y + oy, c)

    def save(self, path: str):
        import os
        os.makedirs(os.path.dirname(path), exist_ok=True)
        self.img.save(path)


# --------------------------------------------------------------------------- noise (seamless)

def _smooth(t: float) -> float:
    return t * t * (3 - 2 * t)


def value_noise(w: int, h: int, cell: int, rng: random.Random) -> list[list[float]]:
    gw, gh = max(1, w // cell), max(1, h // cell)
    grid = [[rng.random() for _ in range(gw)] for _ in range(gh)]
    out = [[0.0] * w for _ in range(h)]
    for y in range(h):
        for x in range(w):
            fx, fy = x / cell, y / cell
            x0, y0 = int(fx) % gw, int(fy) % gh
            x1, y1 = (x0 + 1) % gw, (y0 + 1) % gh
            tx, ty = _smooth(fx - int(fx)), _smooth(fy - int(fy))
            a = grid[y0][x0] + (grid[y0][x1] - grid[y0][x0]) * tx
            b = grid[y1][x0] + (grid[y1][x1] - grid[y1][x0]) * tx
            out[y][x] = a + (b - a) * ty
    return out


def blur_wrap(f: list[list[float]], passes: int = 1, center: float = 2.0) -> list[list[float]]:
    h, w = len(f), len(f[0])
    for _ in range(passes):
        g = [[0.0] * w for _ in range(h)]
        for y in range(h):
            for x in range(w):
                s = f[y][x] * center
                n = center
                for dx, dy in ((1, 0), (-1, 0), (0, 1), (0, -1)):
                    s += f[(y + dy) % h][(x + dx) % w]
                    n += 1
                g[y][x] = s / n
        f = g
    return f


def normalize(f: list[list[float]]) -> list[list[float]]:
    lo = min(min(r) for r in f)
    hi = max(max(r) for r in f)
    d = (hi - lo) or 1.0
    return [[(v - lo) / d for v in r] for r in f]


def fbm(w: int, h: int, seed: int, cells: Iterable[tuple[int, float]], grain: float = 0.0) -> list[list[float]]:
    """Seamless fractal noise. cells = [(cell_size, weight), ...]; grain = white noise weight."""
    rng = random.Random(seed)
    acc = [[0.0] * w for _ in range(h)]
    total = 0.0
    for cell, wgt in cells:
        n = value_noise(w, h, cell, rng)
        for y in range(h):
            for x in range(w):
                acc[y][x] += n[y][x] * wgt
        total += wgt
    if grain > 0:
        for y in range(h):
            for x in range(w):
                acc[y][x] += rng.random() * grain
        total += grain
    return normalize([[v / total for v in r] for r in acc])


def clustered(w: int, h: int, seed: int, passes: int = 1) -> list[list[float]]:
    """Pixel-art 'clumpy' noise: white noise blurred on the torus."""
    rng = random.Random(seed)
    f = [[rng.random() for _ in range(w)] for _ in range(h)]
    return normalize(blur_wrap(f, passes))


# --------------------------------------------------------------------------- masks & shading

def parse_mask(rows: Sequence[str]) -> list[list[str]]:
    """ASCII art -> 2D char grid. '.' or ' ' is empty."""
    h = len(rows)
    w = max(len(r) for r in rows)
    return [[(r[x] if x < len(r) else '.') for x in range(w)] for r in rows]


def mask_from(grid: list[list[str]], chars: str | None = None) -> list[list[bool]]:
    return [[(c not in '. ' and (chars is None or c in chars)) for c in row] for row in grid]


def distance_field(mask: list[list[bool]]) -> list[list[float]]:
    """Chamfer distance to the nearest empty pixel (outside counts as empty)."""
    h, w = len(mask), len(mask[0])
    INF = 1e9
    d = [[(INF if mask[y][x] else 0.0) for x in range(w)] for y in range(h)]

    def get(x, y):
        if 0 <= x < w and 0 <= y < h:
            return d[y][x]
        return 0.0

    for _ in range(2):
        for y in range(h):
            for x in range(w):
                if d[y][x] == 0:
                    continue
                d[y][x] = min(d[y][x], get(x - 1, y) + 1, get(x, y - 1) + 1,
                              get(x - 1, y - 1) + 1.414, get(x + 1, y - 1) + 1.414)
        for y in range(h - 1, -1, -1):
            for x in range(w - 1, -1, -1):
                if d[y][x] == 0:
                    continue
                d[y][x] = min(d[y][x], get(x + 1, y) + 1, get(x, y + 1) + 1,
                              get(x + 1, y + 1) + 1.414, get(x - 1, y + 1) + 1.414)
    return d


def dome_shade(mask: list[list[bool]], roundness: float = 2.5, light=(-0.6, -0.8)) -> list[list[float]]:
    """Treat the mask as a pillow (height = distance to edge) and light it from the top-left.
    Returns brightness 0..1 for masked pixels."""
    h, w = len(mask), len(mask[0])
    dist = distance_field(mask)
    hgt = [[(min(dist[y][x], roundness) / roundness) ** 0.6 if mask[y][x] else 0.0 for x in range(w)] for y in range(h)]

    def H(x, y):
        if 0 <= x < w and 0 <= y < h:
            return hgt[y][x]
        return 0.0

    out = [[0.0] * w for _ in range(h)]
    for y in range(h):
        for x in range(w):
            if not mask[y][x]:
                continue
            out[y][x] = lambert(H, x, y, light)
    # normalise only over masked pixels
    vals = [out[y][x] for y in range(h) for x in range(w) if mask[y][x]]
    lo, hi = min(vals), max(vals)
    d = (hi - lo) or 1
    for y in range(h):
        for x in range(w):
            if mask[y][x]:
                out[y][x] = (out[y][x] - lo) / d
    return out


def lambert(H: Callable[[int, int], float], x: int, y: int, light=(-0.6, -0.8), lz: float = 0.75,
            strength: float = 2.0) -> float:
    """Lambert term of a height field H at (x,y); image y points down, light from top-left."""
    gx = (H(x + 1, y) - H(x - 1, y)) * 0.5 * strength
    gy = (H(x, y + 1) - H(x, y - 1)) * 0.5 * strength
    nx, ny, nz = -gx, -gy, 1.0
    ln = math.sqrt(nx * nx + ny * ny + nz * nz)
    lx, ly = light
    ll = math.sqrt(lx * lx + ly * ly + lz * lz)
    # vector towards the light = (lx, ly, lz)
    return (nx * lx + ny * ly + nz * lz) / (ln * ll)


def edge_pixels(mask: list[list[bool]]):
    """Yields (x, y, side) for pixels on the boundary. side: 'br' if exposed to bottom/right, else 'tl'."""
    h, w = len(mask), len(mask[0])

    def m(x, y):
        return 0 <= x < w and 0 <= y < h and mask[y][x]

    for y in range(h):
        for x in range(w):
            if not mask[y][x]:
                continue
            br = (not m(x + 1, y)) or (not m(x, y + 1))
            tl = (not m(x - 1, y)) or (not m(x, y - 1))
            if br:
                yield x, y, 'br'
            elif tl:
                yield x, y, 'tl'


def draw_mask(cv: Canvas, mask: list[list[bool]], r: Sequence[RGB], ox: int = 0, oy: int = 0,
              shade: list[list[float]] | None = None, outline: bool = True, rng: random.Random | None = None,
              jitter: float = 0.0, lo: float = 0.18, hi: float = 1.0):
    """Fill a mask with ramp colours using dome shading; darkest ramp entry is the outline."""
    h, w = len(mask), len(mask[0])
    shade = shade or dome_shade(mask)
    body = list(r[1:]) if outline else list(r)
    for y in range(h):
        for x in range(w):
            if mask[y][x]:
                v = lo + (hi - lo) * shade[y][x]
                if rng and jitter:
                    v += (rng.random() - 0.5) * jitter
                cv.put(x + ox, y + oy, pick(body, v))
    if outline:
        for x, y, side in edge_pixels(mask):
            cv.put(x + ox, y + oy, r[0] if side == 'br' else r[1])


# --------------------------------------------------------------------------- geometry helpers

def poly_mask(w: int, h: int, pts: Sequence[tuple[float, float]]) -> list[list[bool]]:
    """Rasterise a convex/concave polygon by testing pixel centres."""
    def inside(px, py):
        c = False
        n = len(pts)
        j = n - 1
        for i in range(n):
            xi, yi = pts[i]
            xj, yj = pts[j]
            if ((yi > py) != (yj > py)) and (px < (xj - xi) * (py - yi) / (yj - yi + 1e-12) + xi):
                c = not c
            j = i
        return c

    return [[inside(x + 0.5, y + 0.5) for x in range(w)] for y in range(h)]


def union(*masks):
    h, w = len(masks[0]), len(masks[0][0])
    return [[any(m[y][x] for m in masks) for x in range(w)] for y in range(h)]


def subtract(a, b):
    h, w = len(a), len(a[0])
    return [[a[y][x] and not b[y][x] for x in range(w)] for y in range(h)]


def empty_mask(w=16, h=16):
    return [[False] * w for _ in range(h)]


def rotate_canvas(cv: Canvas, quarter_turns: int) -> Canvas:
    out = Canvas(cv.w, cv.h)
    img = cv.img.rotate(-90 * quarter_turns)
    out.img = img
    out.p = img.load()
    return out


def upscale(img: Image.Image, k: int) -> Image.Image:
    return img.resize((img.width * k, img.height * k), Image.NEAREST)
