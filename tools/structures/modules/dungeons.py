"""Dungeons (research area): the abandoned observatory and rocket test stand on Earth.

A collapsed stone-brick observatory with a weathered copper dome and a broken telescope, a hidden archive in the
cellar (tech disks), and next to it an old rocket test stand: concrete pad, scorched flame trench, a toppled
service gantry and the wreck of a small sounding rocket."""
import math
import random

from template import Template

ARCHIVE = 'spacebreaker:dungeons/earth/observatory_archive'
SUPPLIES = 'spacebreaker:dungeons/earth/observatory_supplies'

SX, SY, SZ = 34, 18, 25
G = 5            # template y of the ground surface (start_height = -5)
CX, CZ = 9, 12   # observatory centre
R = 5            # observatory radius


def observatory(t: Template, rng: random.Random):
    # --- foundation: round stone-brick plinth, one step wider than the drum
    for x in range(CX - R - 2, CX + R + 3):
        for z in range(CZ - R - 2, CZ + R + 3):
            d = math.hypot(x - CX, z - CZ)
            if d <= R + 1.4:
                t.set(x, G - 1, z, 'stone_bricks')
                t.set(x, G - 2, z, 'cobblestone')
            if d <= R - 0.6:
                # floor: polished andesite with a brick ring and a compass-rose cross
                ring = abs(d - 2.5) < 0.5
                cross = (x == CX or z == CZ) and d < R - 1
                t.set(x, G - 1, z, 'stone_bricks' if ring else 'chiseled_stone_bricks' if (x, z) == (CX, CZ)
                      else 'polished_diorite' if cross else 'polished_andesite')
    # --- drum wall, collapsed towards the north-west
    for x in range(CX - R - 1, CX + R + 2):
        for z in range(CZ - R - 1, CZ + R + 2):
            d = math.hypot(x - CX, z - CZ)
            if not (R - 0.5 <= d < R + 0.5):
                continue
            ang = math.atan2(z - CZ, x - CX)          # 0 = east, -pi/2 = north
            ruin = max(0.0, math.cos(ang - math.radians(-135)))  # 1 at north-west
            height = 5 - int(round(ruin * 4.2 + rng.random() * ruin * 1.5))
            for y in range(G, G + max(0, height)):
                window = (y == G + 2 and abs(math.sin(ang * 4)) < 0.2)
                if window:
                    t.set(x, y, z, 'iron_bars', north='true', south='true', east='true', west='true')
                else:
                    t.set(x, y, z, 'stone_bricks')
            if height == 5:
                t.set(x, G + 5, z, 'stone_brick_slab', type='bottom')
            if height < 5 and rng.random() < 0.6:
                # rubble fallen outside
                ox = int(round(CX + math.cos(ang) * (R + 1 + rng.random() * 2)))
                oz = int(round(CZ + math.sin(ang) * (R + 1 + rng.random() * 2)))
                t.set(ox, G, oz, rng.choice(['cobblestone', 'stone_bricks', 'mossy_cobblestone', 'gravel']))
                if rng.random() < 0.3:
                    t.set(ox, G + 1, oz, 'stone_brick_slab', type='bottom')
    # --- door to the east (towards the test stand)
    for y in (G, G + 1):
        t.set(CX + R, y, CZ, 'air')
    t.set(CX + R, G + 2, CZ, 'stone_brick_stairs', facing='west', half='top')
    t.set(CX + R + 1, G - 1, CZ, 'stone_brick_stairs', facing='west', half='bottom')
    # --- weathered copper dome with an observing slit along the east-west axis; its north-west quarter fell in
    dome_y = G + 5
    for x in range(CX - R, CX + R + 1):
        for y in range(dome_y, dome_y + R + 1):
            for z in range(CZ - R, CZ + R + 1):
                d = math.sqrt((x - CX) ** 2 + ((y - dome_y) * 1.15) ** 2 + (z - CZ) ** 2)
                if not (R - 1.0 <= d < R):
                    continue
                if z == CZ and x >= CX - 1:
                    continue  # the observing slit, open up and over the top
                ang = math.degrees(math.atan2(z - CZ, x - CX)) % 360   # 0 east, 90 south, 180 west, 270 north
                jag = ((x * 3 + z * 5 + y * 7) % 4) * 6
                if 150 - jag < ang < 285 + jag and y >= dome_y + 1 + ((x + z) % 2):
                    continue  # collapsed quarter, jagged edge
                if y == dome_y:
                    name = 'weathered_copper'          # ring beam
                else:
                    patch = math.sin(x * 1.3 + y * 0.7) + math.cos(z * 1.1 - y * 0.9)
                    name = 'oxidized_cut_copper' if patch > 0.9 else 'weathered_cut_copper'
                t.set(x, y, z, name)
    # fallen dome panels lying inside the drum, under the hole
    for (x, z, name) in [(CX - 3, CZ - 1, 'weathered_cut_copper'), (CX - 2, CZ - 2, 'weathered_cut_copper_slab'),
                         (CX - 3, CZ + 1, 'oxidized_cut_copper_slab'), (CX - 1, CZ - 3, 'weathered_cut_copper_stairs'),
                         (CX - 6, CZ - 3, 'weathered_cut_copper'), (CX - 7, CZ - 1, 'oxidized_cut_copper_slab')]:
        if name.endswith('slab'):
            t.set(x, G, z, name, type='bottom', waterlogged='false')
        elif name.endswith('stairs'):
            t.set(x, G, z, name, facing='east', half='bottom', shape='straight', waterlogged='false')
        else:
            t.set(x, G, z, name)
    # --- interior debris
    for _ in range(14):
        a = rng.random() * math.tau
        rr = rng.random() * (R - 1.5)
        x, z = int(round(CX + math.cos(a) * rr)), int(round(CZ + math.sin(a) * rr))
        if (x, z) == (CX, CZ) or abs(z - CZ) <= 0 and x > CX:
            continue
        b = rng.choice(['gravel', 'cobblestone', 'cobweb', 'stone_brick_slab'])
        t.set(x, G, z, b, **({'type': 'bottom', 'waterlogged': 'false'} if b.endswith('slab') else {}))
    # --- the telescope: a pier, a fork mount and a tube aimed up through the slit (east)
    t.set(CX, G, CZ, 'polished_deepslate')
    t.set(CX, G + 1, CZ, 'polished_deepslate_wall', up='true', north='none', south='none', east='none', west='none', waterlogged='false')
    t.set(CX, G + 2, CZ, 'anvil', facing='north')
    tube = [(CX - 1, G + 2), (CX, G + 3), (CX + 1, G + 3), (CX + 2, G + 4), (CX + 3, G + 5)]
    for x, y in tube:
        t.set(x, y, CZ, 'waxed_exposed_copper')
    t.set(CX - 2, G + 2, CZ, 'waxed_exposed_cut_copper_slab', type='top', waterlogged='false')  # eyepiece end
    t.set(CX + 1, G + 2, CZ, 'waxed_exposed_cut_copper_stairs', facing='west', half='top', shape='straight', waterlogged='false')
    t.set(CX + 2, G + 3, CZ, 'waxed_exposed_cut_copper_stairs', facing='west', half='top', shape='straight', waterlogged='false')
    t.set(CX + 3, G + 4, CZ, 'waxed_exposed_cut_copper_stairs', facing='west', half='top', shape='straight', waterlogged='false')
    t.set(CX + 4, G + 6, CZ, 'lightning_rod', facing='east', powered='false', waterlogged='false')  # lens hood
    # --- the observer's corner: desk, books, map table
    t.set(CX + 3, G, CZ - 2, 'cartography_table')
    t.set(CX + 3, G, CZ - 3, 'lectern', facing='west', has_book='false', powered='false')
    t.set(CX + 2, G, CZ - 4, 'spruce_stairs', facing='south', half='bottom', shape='straight', waterlogged='false')
    for z in range(CZ - 2, CZ + 3):
        t.set(CX - 4, G, z, 'bookshelf' if rng.random() < 0.6 else 'chiseled_bookshelf')
    # fix chiseled bookshelf properties (all slots empty)
    for (x, y, z), (name, props, nbt) in list(t.blocks.items()):
        if name == 'minecraft:chiseled_bookshelf':
            p = {f'slot_{i}_occupied': 'true' if rng.random() < 0.3 else 'false' for i in range(6)}
            t.set(x, y, z, 'chiseled_bookshelf', facing='east', **p)
    t.set(CX - 4, G + 1, CZ - 1, 'bookshelf')
    t.set(CX - 3, G + 1, CZ + 3, 'cobweb')
    t.set(CX + 3, G + 3, CZ + 3, 'cobweb')
    # lantern hanging from the intact dome rib
    t.set(CX + 2, G + 8, CZ + 2, 'chain', axis='y', waterlogged='false')
    t.set(CX + 2, G + 7, CZ + 2, 'chain', axis='y', waterlogged='false')
    t.set(CX + 2, G + 6, CZ + 2, 'lantern', hanging='true', waterlogged='false')
    # --- hidden archive in the cellar: trapdoor + ladder
    hx, hz = CX - 2, CZ + 2
    t.set(hx, G - 1, hz, 'spruce_trapdoor', facing='north', half='top', open='false', powered='false', waterlogged='false')
    for y in range(1, G - 1):
        t.set(hx, y, hz, 'ladder', facing='south', waterlogged='false')
    cellar(t, rng, hx, hz)


def cellar(t: Template, rng: random.Random, hx, hz):
    x0, x1, z0, z1 = hx - 3, hx + 2, hz, hz + 4   # room interior (the shaft hugs the north wall)
    for x in range(x0 - 1, x1 + 2):
        for z in range(z0 - 1, z1 + 2):
            for y in range(0, G - 1):
                edge = x in (x0 - 1, x1 + 1) or z in (z0 - 1, z1 + 1) or y == 0
                if edge:
                    t.set(x, y, z, 'deepslate_tiles' if y == 0 else 'stone_bricks')
                elif (x, z) != (hx, hz):
                    t.set(x, y, z, 'air')
    for x in range(x0, x1 + 1):
        for z in range(z0, z1 + 1):
            t.set(x, 0, z, rng.choice(['deepslate_tiles', 'deepslate_tiles', 'cracked_deepslate_tiles']))
    t.set(hx, G - 2, hz, 'ladder', facing='south', waterlogged='false')
    # ceiling (the plinth covers only part of the room; never leave it open to the sky)
    for x in range(x0 - 1, x1 + 2):
        for z in range(z0 - 1, z1 + 2):
            if (x, z) == (hx, hz):
                continue
            if t.get(x, G - 1, z) is None:
                t.set(x, G - 1, z, 'stone_bricks' if x in (x0 - 1, x1 + 1) or z in (z0 - 1, z1 + 1) else 'coarse_dirt')
    # archive: chest in an alcove, barrels, a lectern and a candle
    t.chest(x0, 1, z1, ARCHIVE, facing='east')
    t.set(x0, 2, z1, 'candle', candles='3', lit='false', waterlogged='false')
    t.set(x0, 1, z1 - 1, 'barrel', facing='up', open='false')
    t.set(x0, 1, z0, 'bookshelf')
    t.set(x0, 2, z0, 'bookshelf')
    t.set(x1, 1, z1, 'lectern', facing='west', has_book='false', powered='false')
    t.set(x1, 1, z0, 'smithing_table')
    t.set(x1, 3, z0, 'cobweb')
    t.set(x0 + 1, 3, z1, 'cobweb')
    t.set(x0 + 2, 3, z0, 'lantern', hanging='true', waterlogged='false')
    t.set(x0 + 2, 1, z1, 'spruce_slab', type='bottom', waterlogged='false')


def test_stand(t: Template, rng: random.Random):
    px0, px1, pz0, pz1 = 18, 31, 4, 20
    tx0, tx1, tz0, tz1 = 22, 25, 10, 13          # flame trench
    for x in range(px0, px1 + 1):
        for z in range(pz0, pz1 + 1):
            corner = (x in (px0, px1)) and (z in (pz0, pz1))
            if corner:
                continue
            seam = (x - px0) % 4 == 0 or (z - pz0) % 4 == 0
            block = 'light_gray_concrete' if not seam else 'smooth_stone'
            d = math.hypot(x - (tx0 + tx1) / 2, z - (tz0 + tz1) / 2)
            if d < 5.5 and rng.random() < 1.0 - d / 6.5:
                block = rng.choice(['blackstone', 'polished_blackstone', 'basalt', 'gray_concrete', 'coal_block'])
            if rng.random() < 0.06:
                block = rng.choice(['cobblestone', 'gravel', 'coarse_dirt'])
            t.set(x, G - 1, z, block, **({'axis': 'y'} if block == 'basalt' else {}))
            t.set(x, G - 2, z, 'stone')
    # hazard stripes along the north edge
    for x in range(px0 + 1, px1):
        t.set(x, G - 1, pz0, 'yellow_terracotta' if x % 2 else 'black_terracotta')
    # flame trench: two deep, scorched, open to the south-east
    for x in range(tx0, tx1 + 1):
        for z in range(tz0, tz1 + 1):
            t.set(x, G - 1, z, 'air')
            t.set(x, G - 2, z, 'air')
            b = rng.choice(['blackstone', 'magma_block', 'basalt', 'blackstone'])
            t.set(x, G - 3, z, b, **({'axis': 'y'} if b == 'basalt' else {}))
    for x in range(tx0 - 1, tx1 + 2):
        for z in range(tz0 - 1, tz1 + 2):
            if tx0 <= x <= tx1 and tz0 <= z <= tz1:
                continue
            t.set(x, G - 2, z, 'polished_blackstone_bricks')
            t.set(x, G - 3, z, 'polished_blackstone_bricks')
    for x in range(tx0, tx1 + 1):  # exhaust channel to the south
        for z in range(tz1 + 1, tz1 + 5):
            t.set(x, G - 1, z, 'air' if x in (tx0 + 1, tx1 - 1) or z < tz1 + 3 else t.get(x, G - 1, z) or 'blackstone')
            t.set(x, G - 2, z, 'blackstone')
    # launch clamps: four blackstone walls around the trench mouth
    for x, z in [(tx0 - 1, tz0 - 1), (tx1 + 1, tz0 - 1), (tx0 - 1, tz1 + 1), (tx1 + 1, tz1 + 1)]:
        t.set(x, G - 1, z, 'polished_blackstone')
        t.set(x, G, z, 'polished_blackstone_wall', up='true', north='none', south='none', east='none', west='none', waterlogged='false')
    # service gantry: a steel lattice tower east of the trench; its north-east corner buckled and the top fell
    gx0, gz0 = 27, 10
    top = G + 9

    def post_height(dx, dz):
        return top - 4 if (dx, dz) == (2, 0) else top

    def bars(x, y, z, axis):
        if axis == 'x':
            t.set(x, y, z, 'iron_bars', east='true', west='true', north='false', south='false', waterlogged='false')
        else:
            t.set(x, y, z, 'iron_bars', north='true', south='true', east='false', west='false', waterlogged='false')

    for dx in (0, 2):
        for dz in (0, 2):
            for y in range(G, post_height(dx, dz)):
                t.set(gx0 + dx, y, gz0 + dz, 'polished_deepslate_wall', up='true', waterlogged='false',
                      north='low' if dz == 2 else 'none', south='low' if dz == 0 else 'none',
                      east='low' if dx == 0 else 'none', west='low' if dx == 2 else 'none')
    for y in range(G + 1, top):
        if (y - G) % 4 == 0:
            continue  # leave the deck levels open
        for i in (1,):
            if y < post_height(2, 0):
                bars(gx0 + i, y, gz0, 'x')            # north face
                bars(gx0 + 2, y, gz0 + i, 'z')        # east face
            bars(gx0 + i, y, gz0 + 2, 'x')            # south face
            if y > G + 2:
                bars(gx0, y, gz0 + i, 'z')            # west face (open at the bottom: the way in)
    for y in (G + 4, G + 8):                          # decks
        for i in range(3):
            for j in range(3):
                if (i, j) in ((0, 0), (0, 2), (2, 0), (2, 2)):
                    continue
                if y == G + 8 and (i, j) in ((2, 1), (1, 0)):
                    continue                          # torn decking at the buckled corner
                t.set(gx0 + i, y, gz0 + j, 'polished_deepslate_slab', type='bottom', waterlogged='false')
    for i in range(3):                                # cap on the standing posts
        t.set(gx0, top, gz0 + i, 'polished_deepslate_slab', type='bottom', waterlogged='false')
    t.set(gx0 + 1, top, gz0 + 2, 'polished_deepslate_slab', type='bottom', waterlogged='false')
    t.set(gx0 + 2, top, gz0 + 2, 'polished_deepslate_slab', type='bottom', waterlogged='false')
    # swing arm reaching west over the trench, an umbilical hanging from its tip
    for x in range(tx1, gx0):
        t.set(x, G + 5, gz0 + 1, 'polished_deepslate_slab', type='top', waterlogged='false')
    for y in (G + 4, G + 3):
        t.set(tx1, y, gz0 + 1, 'chain', axis='y', waterlogged='false')
    # the fallen top section lying on the pad
    for i in range(4):
        x = gx0 - 1 + i
        if i in (0, 3):
            t.set(x, G, gz0 + 5, 'polished_deepslate_wall', up='true', east='low' if i == 0 else 'none',
                  west='low' if i == 3 else 'none', north='none', south='none', waterlogged='false')
        else:
            bars(x, G, gz0 + 5, 'x')
    t.set(gx0 + 3, G, gz0 + 4, 'polished_deepslate_slab', type='bottom', waterlogged='false')
    # the wrecked sounding rocket, lying across the south of the pad (nose to the west)
    rz = 17
    body = ['white_concrete', 'white_concrete', 'red_concrete', 'white_concrete', 'white_concrete', 'white_concrete']
    for i, b in enumerate(body):
        t.set(20 + i, G, rz, b)
    t.set(19, G, rz, 'smooth_quartz_stairs', facing='east', half='bottom', shape='straight', waterlogged='false')  # nose
    t.set(26, G, rz, 'iron_block')                                  # engine section
    t.set(27, G, rz, 'hopper', facing='down', enabled='true')        # nozzle bell
    for dz in (-1, 1):
        t.set(25, G, rz + dz, 'iron_trapdoor', facing='north' if dz < 0 else 'south', half='bottom', open='true',
              powered='false', waterlogged='false')                    # fins
    t.set(22, G, rz - 1, 'white_concrete')                            # a torn-off panel
    # control bunker: a low hut with a periscope window and the supply chest
    bx0, bz0 = 28, 19
    for x in range(bx0, bx0 + 4):
        for z in range(bz0, bz0 + 4):
            edge = x in (bx0, bx0 + 3) or z in (bz0, bz0 + 3)
            t.set(x, G - 1, z, 'smooth_stone')
            for y in range(G, G + 3):
                if edge:
                    t.set(x, y, z, 'light_gray_concrete' if y < G + 2 else 'smooth_stone')
                else:
                    t.set(x, y, z, 'air')
            t.set(x, G + 3, z, 'smooth_stone_slab', type='bottom', waterlogged='false')
    t.set(bx0, G, bz0 + 1, 'air')                                     # doorway (west)
    t.set(bx0, G + 1, bz0 + 1, 'air')
    t.set(bx0 + 1, G + 1, bz0, 'glass_pane', east='true', west='true', north='false', south='false', waterlogged='false')
    t.set(bx0 + 2, G + 1, bz0, 'glass_pane', east='true', west='true', north='false', south='false', waterlogged='false')
    t.chest(bx0 + 2, G, bz0 + 2, SUPPLIES, facing='north')
    t.set(bx0 + 1, G, bz0 + 2, 'lever', face='floor', facing='north', powered='false')
    t.set(bx0 + 2, G, bz0 + 1, 'redstone_lamp', lit='false')


def nature(t: Template, rng: random.Random):
    """Grass and moss creeping back over the site."""
    for (x, y, z), (name, props, nbt) in list(t.blocks.items()):
        if y != G - 1 or name not in ('minecraft:stone_bricks', 'minecraft:light_gray_concrete', 'minecraft:smooth_stone',
                                       'minecraft:polished_andesite'):
            continue
        if (x, G, z) in t.blocks:
            continue
        r = rng.random()
        if r < 0.05:
            t.set(x, y, z, 'moss_block')
            t.set(x, G, z, 'moss_carpet')
        elif r < 0.11:
            t.set(x, G, z, 'short_grass')


def observatory_ruin() -> Template:
    t = Template(SX, SY, SZ)
    rng = random.Random(19690720)
    observatory(t, rng)
    test_stand(t, rng)
    nature(t, rng)
    # vines on the remaining east wall
    for y in range(G + 1, G + 4):
        if t.get(CX + R, y, CZ + 2) == 'minecraft:stone_bricks':
            t.set(CX + R + 1, y, CZ + 2, 'vine', west='true', east='false', north='false', south='false', up='false')
    return t


def generate(out):
    out('dungeons/earth/observatory', observatory_ruin())
