"""Saturn's moons: Titan (tholin sand and mud, water-ice bedrock, ice cobbles, tholin deposits and tholin), Enceladus
(snow, ice, tiger-stripe ice), the methane skater and geyser drifter, and Saturn's disc and rings for Enceladus' sky.

Vanilla execution: 4-6 tones per block from one ramp, calm noise, light from the top-left.
"""
import math
import random

from sbtex.core import Canvas, ramp, clustered, fbm, parse_mask, mask_from, dome_shade, edge_pixels, pick, hexc
from sbtex import palettes as P
from modules.moon import quant, white, blend, blur_dir, voronoi, footprint, overlay, RAW_CHUNK
from modules.mars import dust, prints, sand
from modules.moon_creatures import paint, noise_pick
from modules.jupiter import frost, ice_block


# --------------------------------------------------------------------------- Titan

def tholin_sand(v=0):
    """Dune sand of organic grains, dark brown, fine and even like vanilla sand with a faint ripple."""
    return sand(P.THOLIN_SAND, 11101 + v * 13)


def tholin_mud(v=0):
    """Damp organic sediment of the plains: soft orange-brown clumps, a few darker wet spots."""
    r = P.THOLIN_MUD
    f = blend((clustered(16, 16, 11201 + v, passes=2), 0.6), (white(11202 + v), 0.4))
    cv, _ = quant(f, r, [4, 16, 34, 30, 12, 4])
    return cv


def titan_ice():
    """Water ice hard as rock at -179 degC, stained beige by tholins: fine grain, a few hairline fractures."""
    r = P.TITAN_ICE
    f = blend((clustered(16, 16, 11301, passes=1), 0.5), (white(11302), 0.3), (fbm(16, 16, 11303, [(8, 1.0)]), 0.2))
    cv, _ = quant(f, [r[1], r[2], r[3], r[4], r[5]], [8, 24, 38, 22, 8])
    for x, y in [(2, 4), (3, 4), (4, 5)]:
        cv[x, y] = r[1]
        cv[x, (y + 1) % 16] = r[5]
    return cv


def ice_cobbles():
    """Rounded ice pebbles washed down old methane rivers, packed in brown sediment: each cobble lit top-left."""
    r = P.ICE_COBBLES
    cell, edge, pts = voronoi(11401, 9)
    rng = random.Random(11402)
    tone = {i: rng.choice([3, 4, 4, 5]) for i in range(len(pts))}
    cv = Canvas()
    for y in range(16):
        for x in range(16):
            e = edge[y][x]
            if e < 0.9:
                cv[x, y] = r[0]
                continue
            c = cell[y][x]
            px, py = pts[c]
            dx = ((x + 0.5 - px + 8) % 16) - 8
            dy = ((y + 0.5 - py + 8) % 16) - 8
            t = tone[c] + (1 if dx + dy < -1.5 else (-1 if dx + dy > 1.5 else 0))
            cv[x, y] = r[max(1, min(5, t))]
    return cv


THO = [
    (2, 2, [(0, 0, 2), (1, 0, 3), (0, 1, 1), (1, 1, 1), (2, 1, 0)]),
    (10, 1, [(0, 0, 3), (1, 0, 1), (0, 1, 1), (1, 1, 0)]),
    (5, 7, [(0, 0, 2), (1, 0, 3), (2, 0, 2), (0, 1, 1), (1, 1, 1), (2, 1, 0)]),
    (13, 6, [(0, 0, 3), (0, 1, 1), (1, 1, 0)]),
    (1, 12, [(0, 0, 3), (1, 0, 2), (1, 1, 0)]),
    (9, 12, [(0, 0, 2), (1, 0, 3), (0, 1, 1), (1, 1, 0)]),
]


def tholin_overlay():
    r = P.THOLIN
    return overlay(THO, [r[2], r[3], r[4], r[5]])


def tholin_item():
    """A sticky lump of tholin: amber-brown organic tar with glossy highlights."""
    m = mask_from(parse_mask(RAW_CHUNK))
    sh = dome_shade(m, roundness=3.0)
    rng = random.Random(11501)
    r = P.THOLIN
    cv = Canvas()
    for y in range(16):
        for x in range(16):
            if m[y][x]:
                v = 0.05 + 0.8 * sh[y][x] + (rng.random() - 0.5) * 0.14
                cv[x, y] = pick(r[1:6], v)
    for x, y in [(5, 4), (6, 4), (10, 7)]:
        cv[x, y] = r[6]
    for x, y, side in edge_pixels(m):
        cv[x, y] = r[0] if side == 'br' else r[1]
    return cv


# --------------------------------------------------------------------------- Enceladus

def enceladus_snow(v=0):
    return frost(P.ENCELADUS_SNOW, 11601 + v * 17)


def enceladus_ice():
    return ice_block(P.ENCELADUS_ICE, 11701, cracks=[[(10, 3), (11, 4), (12, 4)]])


def sulcus_ice():
    """Tiger-stripe ice: deep blue, banded along the fracture, with darker streaks of organics from the plumes."""
    r = P.SULCUS_ICE
    f = blend((blur_dir(clustered(16, 16, 11801, passes=1), 3, 1, 0), 0.65), (white(11802), 0.35))
    cv, _ = quant(f, r, [5, 16, 32, 28, 14, 5])
    return cv


# --------------------------------------------------------------------------- creatures

def methane_skater():
    """64x32 sheet for MethaneSkaterModel: dark amber body with pale lengthwise stripes, banded legs."""
    cv = Canvas(64, 32)
    r = P.SKATER
    n = clustered(64, 64, 11901, passes=1)

    stripe = [hexc('#4f9aa3'), hexc('#7fd0d8'), hexc('#c4f0f2')]

    def body(face, x, y, fw, fh):
        if face == 'top':
            if x == fw // 2:
                return stripe[1] if y % 3 else stripe[2]  # the pale blue stripe down the back
            if x in (1, fw - 2):
                return r[5]
            return noise_pick(r, 3, n, x, y)
        if face == 'bottom':
            return r[1]
        return noise_pick(r, 2, n, x + 20, y + 30)

    paint(cv, 0, 0, 5, 2, 12, body)
    paint(cv, 0, 14, 3, 2, 3, lambda f, x, y, fw, fh: hexc('#120a05') if (f == 'front' and y == 0 and x in (0, 2)) else r[4] if f == 'top' else r[3])
    for y in range(14, 21):
        for x in range(12, 18):
            cv[x, y] = r[5]
    # the raised thorax: the stripe runs over it too
    paint(cv, 34, 0, 3, 1, 4, lambda f, x, y, fw, fh: (stripe[1] if f == 'top' and x == 1 else (r[4] if f == 'top' else r[3])))
    # foot pads: glossy, pale where they dimple the liquid
    paint(cv, 34, 8, 3, 1, 3, lambda f, x, y, fw, fh: stripe[2] if f == 'bottom' else (stripe[1] if f == 'top' and (x + y) % 2 == 0 else stripe[0]))
    for row, length in ((22, 14), (25, 12)):
        for yy in range(row, row + 3):
            for x in range(0, 2 * (length + 1)):
                cv[x, yy] = r[2] if (x // 3) % 2 else r[4]
    return cv


# Tholin worm: honey-amber rings, darker grooves at each ring's edges, tholin dust specks on the back; the mouth inside is
# a dark orange (no red, no flesh tones).
WORM_HONEY = ramp('#2e1a07', '#4a2c0c', '#6a4212', '#8c5a1a', '#ad7424', '#c98f34', '#e0ac4c', '#f0c972')
WORM_DUST = ramp('#3a2414', '#4f3320')
WORM_MOUTH = ramp('#3c1c04', '#5a2c08', '#7a3e0c')


def tholin_worm():
    """64x64 sheet for TholinWormModel: six ringed segments thick in the middle, four mouth lips round a dark orange
    gullet, and the back bristles. Box layout in moon_creatures.faces."""
    cv = Canvas(64, 64)
    r = WORM_HONEY
    n = clustered(64, 64, 12101, passes=1)
    rng = random.Random(12103)
    specks = {(rng.randrange(64), rng.randrange(64)) for _ in range(140)}

    def ring(face, x, y, fw, fh):
        if face == 'bottom':
            return noise_pick(r, 6, n, x, y + 40)
        if face == 'top':
            # the back: amber with dust specks, the ring edges (front and back rows) darker
            if y == 0 or y == fh - 1:
                return r[2]
            if (x * 7 + y * 3 + fw) % 11 == 0 or (x + 30, y + fw) in specks:
                return WORM_DUST[(x + y) % 2]
            return noise_pick(r, 4, n, x, y + 5)
        if face in ('front', 'back'):
            # the ring's end faces show between segments as a dark groove
            return noise_pick(r, 1, n, x + 30, y) if y < fh - 1 else r[3]
        # sides: dark groove at both ring edges, a lighter band along the middle, pale toward the belly
        if x == 0 or x == fw - 1:
            return r[2]
        if y == fh - 1:
            return r[6]
        if y == fh - 2:
            return noise_pick(r, 5, n, x + 9, y + 20)
        return noise_pick(r, 4 if y % 3 else 5, n, x + 9, y + 20)

    def head(face, x, y, fw, fh):
        if face == 'front':
            # the gullet behind the lips: dark orange rings closing in to the darkest middle
            cx, cy = (fw - 1) / 2, (fh - 1) / 2 - 0.5
            d = max(abs(x - cx), abs(y - cy))
            return WORM_MOUTH[0] if d < 0.8 else (WORM_MOUTH[1] if d < 1.6 else r[2])
        return ring(face, x, y, fw, fh)

    def lip(face, x, y, fw, fh):
        # lips: amber outside, a dark orange inner face, lighter tip (the front face)
        if face == 'front':
            return r[6]
        if face in ('bottom',) and fh == 1:
            return WORM_MOUTH[2]
        return noise_pick(r, 5, n, x + 50, y + 3)

    def bristle(face, x, y, fw, fh):
        return r[7] if y == 0 else (r[6] if y == 1 else r[4])

    for (u, v, w, h, d) in [(0, 0, 5, 4, 3), (0, 8, 6, 5, 3), (0, 17, 7, 6, 3), (0, 27, 6, 5, 3), (20, 0, 5, 4, 2), (36, 0, 3, 3, 2)]:
        paint(cv, u, v, w, h, d, head if (u, v) == (0, 0) else ring)
    paint(cv, 20, 12, 5, 1, 2, lip)
    paint(cv, 20, 16, 5, 1, 2, lip)
    paint(cv, 36, 8, 1, 2, 2, lip)
    paint(cv, 48, 0, 1, 3, 1, bristle)
    return cv


def geyser_drifter():
    """64x32 sheet for GeyserDrifterModel: a translucent blue-white bell, brighter rim, pale threads."""
    cv = Canvas(64, 32)
    r = P.DRIFTER
    n = clustered(64, 64, 12001, passes=1)

    def bell(face, x, y, fw, fh):
        a = 200 if face != 'bottom' else 150
        c = noise_pick(r, 2 if face != 'top' else 3, n, x, y)
        return (c[0], c[1], c[2], a)

    paint(cv, 0, 0, 8, 5, 8, bell)
    paint(cv, 0, 13, 6, 2, 6, lambda f, x, y, fw, fh: (*r[3], 210))
    paint(cv, 24, 13, 9, 1, 9, lambda f, x, y, fw, fh: (*r[4], 220))
    for i in range(4):
        paint(cv, 32 + i * 4, 0, 1, 8, 1, lambda f, x, y, fw, fh: (*r[1], 190) if y % 3 else (*r[3], 200))
    return cv


def geyser_drifter_glow():
    cv = Canvas(64, 32)
    g = P.DRIFTER_GLOW
    # spots around the rim (side faces of the rim box start at y = 22)
    for x in range(24, 60, 3):
        cv[x, 22] = g[1]
    for x, y in [(11, 9), (14, 9), (17, 9), (20, 9)]:
        cv[x, y] = g[2]
    return cv


# --------------------------------------------------------------------------- Saturn

def saturn_disc(size=256):
    """Saturn as a full disc in flat tones: pale butterscotch zones and soft tan belts, wider and calmer than Jupiter's,
    a blue-grey polar region in the north with a hint of the hexagon. Rings and phase are drawn in game."""
    from PIL import Image
    zone = hexc('#e7d6ac')
    zone2 = hexc('#dcc795')
    tan = hexc('#cbb07e')
    belt = hexc('#b89a6b')
    belt2 = hexc('#a88c62')
    polar = hexc('#9a9a88')
    polar2 = hexc('#8a919a')
    bands = [(-1.0, -0.8, polar2), (-0.8, -0.66, polar), (-0.66, -0.5, tan), (-0.5, -0.36, zone2), (-0.36, -0.22, belt),
             (-0.22, -0.12, zone), (-0.12, 0.12, zone), (0.12, 0.2, tan), (0.2, 0.34, belt2), (0.34, 0.5, zone2), (0.5, 0.66, tan),
             (0.66, 0.84, belt), (0.84, 1.0, belt2)]
    img = Image.new('RGBA', (size, size), (0, 0, 0, 0))
    px = img.load()
    c = (size - 1) / 2
    for y in range(size):
        for x in range(size):
            u = (x - c) / c
            v = (y - c) / c
            if math.hypot(u, v) > 1:
                continue
            vv = v + 0.006 * math.sin(u * 13 + v * 7)
            col = zone
            for top, bot, colour in bands:
                if top <= vv < bot:
                    col = colour
                    break
            # the north polar hexagon: a six-sided outline around the pole
            ang = math.atan2(u, v + 1.0)
            if v < -0.78:
                hexr = 0.2 / math.cos(((ang * 3 / math.pi) % 1 - 0.5) * math.pi / 3)
                if abs(math.hypot(u, v + 0.98) - hexr) < 0.02:
                    col = polar
            px[x, y] = (col[0], col[1], col[2], 255)
    return img


def saturn_rings(width=256):
    """Radial ring profile, inner edge (1.24 Saturn radii) on the left, outer edge (2.27) on the right: the faint C ring,
    the bright B ring, the dark Cassini Division, the A ring with the Encke Gap. Opacity in alpha, a few flat tones."""
    from PIL import Image
    img = Image.new('RGBA', (width, 4), (0, 0, 0, 0))
    px = img.load()
    for x in range(width):
        r = 1.24 + (2.27 - 1.24) * (x + 0.5) / width
        if r < 1.53:
            col, a = hexc('#8f8370'), 70 if int(r * 60) % 3 else 95
        elif r < 1.95:
            band = int((r - 1.53) * 40) % 4
            col = hexc('#ded0b2') if band in (0, 2) else hexc('#cfbf9e')
            a = 230
        elif r < 2.03:
            col, a = hexc('#4a4438'), 25
        elif 2.205 < r < 2.215:
            col, a = hexc('#4a4438'), 15
        else:
            col = hexc('#bfb59f') if int(r * 50) % 3 else hexc('#b0a68f')
            a = 175
        for y in range(4):
            px[x, y] = (col[0], col[1], col[2], a)
    return img


# --------------------------------------------------------------------------- export

def methane_drop():
    """A large, slow raindrop (tinted pale amber in game): a short rounded streak, brighter at the leading end."""
    cv = Canvas(8, 8)
    for y, a in ((1, 90), (2, 150), (3, 200), (4, 240), (5, 255)):
        cv[3, y] = (235, 235, 235, a)
        cv[4, y] = (210, 210, 210, a)
    cv[3, 6] = (200, 200, 200, 230)
    cv[4, 6] = (180, 180, 180, 200)
    return cv


def methane_splash():
    """The flat splash of a landing drop: a broken ring."""
    cv = Canvas(8, 8)
    for x, y in ((1, 4), (2, 3), (5, 3), (6, 4), (2, 5), (5, 5), (3, 6), (4, 6)):
        cv[x, y] = (225, 225, 225, 210)
    cv[3, 4] = (200, 200, 200, 120)
    cv[4, 4] = (200, 200, 200, 120)
    return cv


def export(save):
    dust_spec = (18, 8, 64, None)
    frost_spec = (90, 14, 30, None)
    for name, fn, r, spec in (('tholin_sand', tholin_sand, P.THOLIN_SAND, dust_spec), ('tholin_mud', tholin_mud, P.THOLIN_MUD, (60, 10, 50, None)),
                              ('enceladus_snow', enceladus_snow, P.ENCELADUS_SNOW, frost_spec)):
        base = fn(0)
        save(base, 'block', name, spec)
        save(fn(1), 'block', name + '_1', spec)
        for side in ('left', 'right'):
            save(footprint(base, prints(r), side), 'block', f'{name}_footprint_{side}', (22, 8, 64, None))
    save(titan_ice(), 'block', 'titan_ice', (60, 14, 10, None))
    save(ice_cobbles(), 'block', 'ice_cobbles', (60, 14, 20, None))
    save(tholin_overlay(), 'block', 'tholin_ore_overlay', (90, 20, 0, None), wrap=False)
    save(enceladus_ice(), 'block', 'enceladus_ice', (200, 20, 0, None))
    glow = lambda c: (200, 20, 0, 230) if c[3] else None
    save(sulcus_ice(), 'block', 'sulcus_ice', (200, 20, 0, glow))
    save(tholin_item(), 'item', 'tholin')
    save(methane_drop(), 'particle', 'methane_drop')
    save(methane_splash(), 'particle', 'methane_splash')
    save(methane_skater(), 'entity', 'saturn/methane_skater')
    save(geyser_drifter(), 'entity', 'saturn/geyser_drifter')
    save(tholin_worm(), 'entity', 'saturn/tholin_worm')
    save(geyser_drifter_glow(), 'entity', 'saturn/geyser_drifter_glow')
    import os
    out = os.path.join(os.path.dirname(__file__), '..', '..', '..', 'src', 'main', 'resources', 'assets', 'spacebreaker', 'textures', 'environment')
    saturn_disc().save(os.path.join(out, 'saturn_disc.png'))
    saturn_rings().save(os.path.join(out, 'saturn_rings.png'))
