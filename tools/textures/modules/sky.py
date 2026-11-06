"""Sky textures for airless worlds: the sun disc and the Earth seen from the Moon.

earth_map.png and earth_clouds.png are equirectangular maps (128x64, 2.8 degrees per pixel). The sky renderer
projects them onto a rotating pixel-art globe with the real phase. Coastlines are rough hand-made polygons
in longitude/latitude, so the planet is recognisable at 64 px without copying any map data.
"""
import math
import random

from sbtex.core import Canvas, fbm

W, H = 128, 64

# rough coastlines: (longitude, latitude) degrees
NORTH_AMERICA = [(-168, 65), (-160, 71), (-140, 70), (-120, 72), (-95, 72), (-80, 73), (-62, 60), (-55, 52), (-66, 45),
                 (-70, 42), (-76, 35), (-81, 31), (-80, 25), (-82, 30), (-90, 29), (-97, 26), (-97, 21), (-92, 18), (-87, 21),
                 (-83, 15), (-78, 9), (-80, 7), (-86, 12), (-92, 14), (-105, 20), (-110, 23), (-115, 30), (-118, 34),
                 (-124, 40), (-124, 48), (-130, 55), (-140, 59), (-152, 58), (-158, 56), (-165, 60)]
HUDSON_BAY = [(-95, 60), (-88, 64), (-80, 63), (-77, 55), (-82, 52), (-90, 57)]
SOUTH_AMERICA = [(-80, 9), (-75, 11), (-62, 10), (-50, 0), (-35, -5), (-39, -15), (-48, -26), (-58, -35), (-65, -42),
                 (-68, -52), (-74, -52), (-72, -40), (-71, -30), (-70, -18), (-76, -14), (-81, -5), (-80, 0)]
AFRICA = [(-17, 21), (-13, 28), (-6, 36), (10, 37), (11, 33), (20, 31), (32, 31), (34, 27), (38, 18), (43, 12), (51, 12),
          (48, 4), (40, -5), (40, -15), (35, -24), (28, -33), (20, -35), (17, -29), (12, -17), (13, -6), (9, 2), (8, 4),
          (-5, 5), (-8, 4), (-13, 8), (-17, 14)]
MADAGASCAR = [(44, -12), (50, -15), (47, -25), (43, -22)]
EURASIA = [(-10, 36), (-9, 43), (-1, 44), (-4, 48), (2, 51), (8, 54), (10, 57), (5, 62), (15, 69), (25, 71), (40, 67),
           (60, 69), (70, 73), (80, 72), (100, 77), (115, 74), (140, 72), (160, 70), (180, 68), (180, 65), (170, 60),
           (160, 55), (156, 51), (142, 53), (140, 45), (130, 42), (127, 35), (122, 30), (121, 25), (110, 20), (108, 12),
           (104, 9), (100, 13), (98, 16), (95, 15), (92, 22), (88, 22), (80, 15), (77, 8), (73, 18), (67, 25), (59, 22),
           (52, 15), (43, 13), (39, 21), (34, 28), (35, 33), (36, 36), (26, 39), (20, 40), (13, 44), (9, 44), (3, 43),
           (-1, 37), (-6, 36)]
BRITAIN = [(-5, 50), (1, 51), (0, 54), (-2, 58), (-6, 58), (-5, 54)]
ICELAND = [(-24, 64), (-14, 64), (-15, 66), (-22, 66)]
JAPAN = [(130, 31), (135, 34), (141, 38), (142, 43), (140, 41), (135, 36)]
AUSTRALIA = [(114, -22), (122, -18), (130, -12), (137, -12), (142, -11), (146, -19), (153, -25), (150, -37), (143, -39),
             (135, -35), (129, -32), (115, -34)]
BORNEO = [(109, 1), (117, 7), (119, 0), (116, -4), (110, -3)]
SUMATRA = [(95, 5), (106, -6), (104, -5), (98, 2)]
NEW_GUINEA = [(131, -1), (141, -3), (150, -10), (141, -9), (136, -4)]
NEW_ZEALAND = [(172, -34), (178, -38), (174, -41), (167, -46), (170, -44)]
GREENLAND = [(-73, 78), (-60, 82), (-30, 83), (-20, 75), (-22, 70), (-42, 60), (-50, 64), (-55, 70)]

LAND = [NORTH_AMERICA, SOUTH_AMERICA, AFRICA, MADAGASCAR, EURASIA, BRITAIN, ICELAND, JAPAN, AUSTRALIA, BORNEO, SUMATRA,
        NEW_GUINEA, NEW_ZEALAND]
ICE_LAND = [GREENLAND]
WATER = [HUDSON_BAY]
# hot deserts: Sahara, Arabia, central Australia, Kalahari, southwest North America, Atacama, Gobi
DESERTS = [[(-16, 16), (-12, 30), (10, 32), (32, 30), (35, 16), (15, 15)],
           [(36, 30), (48, 30), (58, 24), (52, 16), (43, 15), (38, 22)],
           [(118, -20), (135, -18), (140, -24), (135, -32), (120, -30)],
           [(15, -18), (25, -18), (26, -28), (17, -28)],
           [(-117, 37), (-104, 36), (-103, 28), (-112, 28)],
           [(-71, -15), (-68, -15), (-69, -28), (-71, -28)],
           [(90, 45), (112, 45), (112, 38), (90, 38)]]

OCEAN_DEEP = (25, 52, 99)
OCEAN = (32, 64, 118)
OCEAN_SHALLOW = (44, 82, 138)
ICE = (226, 233, 238)
ICE_SHADE = (196, 207, 216)
TUNDRA = (95, 101, 82)
BOREAL = (58, 79, 52)
TEMPERATE = (74, 99, 52)
TROPICAL = (52, 92, 44)
SAVANNA = (124, 118, 70)
DESERT = (176, 152, 104)
DESERT_DARK = (156, 128, 86)


def inside(poly, x, y):
    c = False
    j = len(poly) - 1
    for i in range(len(poly)):
        xi, yi = poly[i]
        xj, yj = poly[j]
        if (yi > y) != (yj > y) and x < (xj - xi) * (y - yi) / (yj - yi + 1e-9) + xi:
            c = not c
        j = i
    return c


def lonlat(px, py):
    return (px + 0.5) / W * 360 - 180, 90 - (py + 0.5) / H * 180


def is_land(lon, lat):
    if any(inside(p, lon, lat) for p in WATER):
        return False
    return any(inside(p, lon, lat) for p in LAND + ICE_LAND)


def earth_map():
    noise = fbm(W, H, 2024, [(16, 0.6), (8, 0.4)])
    cv = Canvas(W, H)
    land = [[is_land(*lonlat(x, y)) for x in range(W)] for y in range(H)]
    for y in range(H):
        for x in range(W):
            lon, lat = lonlat(x, y)
            n = noise[y][x]
            a = abs(lat)
            antarctic = lat < -66 - 3 * math.sin(math.radians(lon * 2))
            if antarctic or any(inside(p, lon, lat) for p in ICE_LAND):
                cv[x, y] = ICE if n > 0.35 else ICE_SHADE
                continue
            if land[y][x]:
                if any(inside(p, lon, lat) for p in DESERTS):
                    c = DESERT if n > 0.4 else DESERT_DARK
                elif a > 62:
                    c = TUNDRA if n > 0.45 else BOREAL
                elif a > 48:
                    c = BOREAL
                elif a > 28:
                    c = TEMPERATE if n > 0.35 else SAVANNA
                elif a > 14:
                    c = SAVANNA if n > 0.55 else TROPICAL
                else:
                    c = TROPICAL
                cv[x, y] = c
                continue
            if a > 78:
                cv[x, y] = ICE_SHADE if n > 0.45 else OCEAN_DEEP  # arctic pack ice
                continue
            coast = any(land[(y + dy) % H][(x + dx) % W] for dx, dy in ((1, 0), (-1, 0), (0, 1), (0, -1)) if 0 <= y + dy < H)
            cv[x, y] = OCEAN_SHALLOW if coast else (OCEAN if n > 0.45 else OCEAN_DEEP)
    return cv


def earth_clouds():
    """Cloud cover: a band of tropical towers near the equator, swirling storm belts at mid latitudes,
    clear subtropics. White with two alpha levels (thin / thick)."""
    swirl = fbm(W, H, 77, [(16, 0.35), (8, 0.4), (4, 0.25)])
    cv = Canvas(W, H)
    for y in range(H):
        for x in range(W):
            lon, lat = lonlat(x, y)
            a = abs(lat)
            belt = math.exp(-((a - 52) / 14) ** 2) * 0.55 + math.exp(-(lat / 7) ** 2) * 0.4 + 0.12
            if 18 < a < 32:
                belt *= 0.45
            # streaks follow the latitude bands (jet stream)
            v = swirl[y][(x + int(6 * math.sin(math.radians(lat * 4)))) % W] * 0.75 + belt * 0.6
            if v > 0.8:
                cv[x, y] = (246, 248, 250, 235)
            elif v > 0.7:
                cv[x, y] = (232, 236, 240, 150)
    return cv


def sun_vacuum():
    """32x32 sun for the black sky: crisp white disc (12 px) and a faint stepped glare.
    Drawn additively, so dark pixels simply add little light."""
    cv = Canvas(32, 32)
    c = 15.5
    for y in range(32):
        for x in range(32):
            r = math.hypot(x - c, y - c)
            if r <= 5.6:
                col = (255, 255, 255)
            elif r <= 6.5:
                col = (255, 247, 222)
            else:
                g = 0.42 * (6.5 / r) ** 2.4
                g = round(g * 10) / 10  # pixel-art steps
                if g <= 0:
                    continue
                col = (int(255 * g), int(240 * g), int(212 * g))
            cv[x, y] = (*col, 255)
    return cv


def export(save):
    save(sun_vacuum(), 'environment', 'sun_vacuum')
    save(earth_map(), 'environment', 'earth_map')
    save(earth_clouds(), 'environment', 'earth_clouds')
