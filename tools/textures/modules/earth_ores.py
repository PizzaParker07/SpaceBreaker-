"""Earth ore overlays (the Moon's ilmenite and KREEP live in moon.py). Ores are rendered as an overlay on top of the host stone texture
(vanilla stone/deepslate for Earth ores, our own rocks elsewhere), so they always
match the host block — including with resource packs.

Overlays are hand-placed pixel art: one character per pixel, mapped onto the
material ramp (see legend of each function)."""
from sbtex.core import Canvas


def _draw(rows, r, legend) -> Canvas:
    assert len(rows) == 16, len(rows)
    cv = Canvas()
    for y, row in enumerate(rows):
        assert len(row) == 16, (y, row, len(row))
        for x, ch in enumerate(row):
            if ch in legend:
                cv[x, y] = r[legend[ch]]
    return cv


# Pisolitic bauxite: clumps of small round red-brown pisoids.
# 1 = contact shadow ... 6 = sun-lit top of a pisoid
BAUXITE_ROWS = [
    "................",
    ".54.........54..",
    ".43253.....4432.",
    "..2132......3321",
    "....11.......21.",
    "........54......",
    ".......4321.....",
    "..54....221.....",
    ".4432.....53....",
    ".3321.....32....",
    ".221..........54",
    "......53.....432",
    ".....4432....321",
    ".....3321.....1.",
    "......21........",
    "................",
]


def bauxite_overlay() -> Canvas:
    from sbtex.palettes import BAUXITE
    return _draw(BAUXITE_ROWS, BAUXITE, {str(i): i for i in range(7)})


def raw_bauxite_block() -> Canvas:
    """Massive pisolitic bauxite: earthy red-brown matrix packed with small round pisoids."""
    import random
    from sbtex.core import clustered, pick
    from sbtex.palettes import BAUXITE as r
    rng = random.Random(41)
    n = clustered(16, 16, 41, passes=2)
    g = clustered(16, 16, 42, passes=0)
    cv = Canvas()
    for y in range(16):
        for x in range(16):
            cv[x, y] = pick(r[1:4], 0.1 + n[y][x] * 0.6 + g[y][x] * 0.3)
    for x, y in [(1, 1), (6, 2), (12, 1), (3, 6), (9, 5), (14, 7), (1, 11), (6, 10), (11, 11), (8, 14), (14, 13), (4, 14)]:
        cv[x, y] = r[5]
        cv[x + 1, y] = r[4]
        cv[x, y + 1] = r[4]
        cv[x + 1, y + 1] = r[3]
        cv[x + 2, y + 1] = r[0]
        cv[x + 1, y + 2] = r[0]
    return cv


COAL = [(14, 14, 16), (22, 22, 25), (31, 31, 35), (41, 41, 46), (52, 52, 58), (66, 66, 72), (82, 82, 90), (104, 104, 112)]
WHITE_PAINT = [(84, 87, 94), (114, 118, 126), (146, 150, 158), (176, 180, 187), (202, 205, 210), (222, 224, 228), (238, 239, 242), (250, 250, 251)]

# Ore styles: ramp, fleck style, tones used (dark, mid, light), raw chunk variant, raw-item inclusion ramp.
ORES = {
    'bauxite': ('BAUXITE', 'round', (2, 4, 6), 'a'),
    'rutile': ('RUTILE', 'needle', (2, 5, 7), 'b'),
    'pentlandite': ('PENTLANDITE', 'blocky', (2, 4, 6), 'c'),
    'spodumene': ('SPODUMENE', 'lath', (3, 5, 7), 'b'),
    'wolframite': ('WOLFRAMITE', 'tabular', (1, 3, 7), 'a'),
    'uraninite': ('URANINITE', 'round', (1, 4, 7), 'c'),
}
METALS = {  # name -> (ramp, block style, has nugget/block)
    'aluminum': ('ALUMINIUM', 'lines'), 'titanium': ('TITANIUM', 'tiles'), 'nickel': ('NICKEL', 'brushed'),
    'lithium': ('LITHIUM', 'plain'), 'tungsten': ('TUNGSTEN', 'rivets'), 'uranium': ('URANIUM', 'plain'), 'steel': ('CARBON_STEEL', 'rivets'),
    'aluminum_lithium': ('AL_LI', 'lines'), 'nickel_superalloy': ('SUPERALLOY', 'tiles'),
}
DUSTS = ['aluminum', 'titanium', 'nickel', 'lithium', 'tungsten', 'uranium', 'iron', 'copper', 'gold']
PLATES = ['aluminum', 'titanium', 'nickel', 'lithium', 'tungsten', 'steel', 'aluminum_lithium', 'nickel_superalloy', 'iron', 'copper', 'gold']
WIRES = ['aluminum', 'tungsten', 'copper', 'gold']
RODS = ['titanium', 'tungsten', 'steel', 'iron']


# Item and block tones per metal. The shared palettes keep their neutral greys for machines and rockets; the metals
# themselves get a hue each so seven grey ingots, plates and dusts can be told apart in the inventory.
def _hex(*h):
    from sbtex.core import ramp
    return ramp(*h)


ITEM_RAMPS = {
    # aluminium: bright, cool white (the shared ramp already is)
    'aluminum_lithium': _hex('#36324a', '#4f4a68', '#6c6788', '#8c87a8', '#aba7c4', '#c7c3dc', '#dfdcee', '#f3f1fa'),   # blue-lilac
    'lithium': _hex('#45373b', '#635158', '#846e75', '#a48d93', '#c0abb0', '#d7c5c9', '#eadcdf', '#f8f0f2'),            # pink silver
    'nickel_superalloy': _hex('#2b241c', '#41372a', '#5b4d3b', '#76654e', '#917e63', '#ac987c', '#c6b398', '#dfcfb8'),  # bronze grey
    'steel': _hex('#1b2130', '#293144', '#3a4459', '#4e5970', '#636e87', '#7b869e', '#96a0b6', '#b5bdce'),              # blue grey
    'titanium': _hex('#211e33', '#322d4c', '#45406a', '#5a5485', '#716b9f', '#8c87b6', '#aaa6cc', '#cac7e2'),           # anodised violet
    'tungsten': _hex('#0f1013', '#191b1f', '#24272d', '#31353c', '#40454d', '#525860', '#696f78', '#898f98'),           # near black, heavy
}


def ramp_of(name):
    from sbtex import palettes as P
    if name in ITEM_RAMPS:
        return ITEM_RAMPS[name]
    key = {'iron': 'IRON', 'copper': 'COPPER', 'gold': 'GOLD'}.get(name) or METALS[name][0]
    return getattr(P, key)


def glints(cv, r, seed=1):
    """Metal dusts sparkle: three bright specks on the lit side of the heap (yellowcake and coal dust have none)."""
    import random
    rng = random.Random(seed * 7 + 1)
    spots = [(x, y) for y in range(7, 12) for x in range(3, 11) if cv.alpha(x, y)]
    for x, y in rng.sample(spots, min(3, len(spots))):
        cv[x, y] = r[7]
    return cv


def surface_cue(cv, name, r):
    """A small per-metal mark on an ingot or plate: brushed lines on steel, a stamped band on the superalloy."""
    tones = [tuple(t) for t in r]
    for y in range(16):
        for x in range(16):
            c = cv[x, y]
            if c[3] == 0 or tuple(c[:3]) not in tones:
                continue
            i = tones.index(tuple(c[:3]))
            if name == 'steel' and i == 5 and y % 2 == 1:
                cv[x, y] = r[4]
            elif name == 'nickel_superalloy' and 3 <= i <= 6 and x in (7, 8):
                cv[x, y] = r[i - 2]
    return cv


def oil_seep() -> Canvas:
    """Tar-stained soil: brown earth with glossy black tar patches."""
    import random
    from sbtex.core import clustered, pick
    from sbtex.palettes import CRUDE
    soil = [(50, 38, 28), (64, 49, 35), (78, 60, 43), (92, 72, 52)]
    n = clustered(16, 16, 77, passes=2)
    g = clustered(16, 16, 78, passes=0)
    cv = Canvas()
    for y in range(16):
        for x in range(16):
            v = n[y][x] * 0.7 + g[y][x] * 0.3
            if n[y][x] < 0.42:
                cv[x, y] = CRUDE[1] if g[y][x] > 0.3 else CRUDE[0]
            else:
                cv[x, y] = pick(soil, v)
    rng = random.Random(5)
    for _ in range(6):                     # glossy highlights on the tar
        x, y = rng.randrange(16), rng.randrange(16)
        if n[y][x] < 0.35:
            cv[x, y] = CRUDE[5]
    return cv


def export(save):
    from modules import industry_shapes as S
    from sbtex import palettes as P
    stone_spec = (55, 18, 30, None)
    for ore, (rk, style, tones, variant) in ORES.items():
        r = getattr(P, rk)
        save(S.ore_overlay(r, style, tones), 'block', f'{ore}_ore_overlay', stone_spec, wrap=False)
        save(S.raw_chunk(r, variant, seed=len(ore)), 'item', f'raw_{ore}')
        save(S.raw_block(r, seed=len(ore) * 3), 'block', f'raw_{ore}_block', (40, 10, 50, None))
    metal_spec = (170, 230, 0, None)
    for name, (rk, style) in METALS.items():
        r = ramp_of(name)
        save(surface_cue(S.ingot(r), name, r), 'item', f'{name}_ingot')
        save(S.nugget(r), 'item', f'{name}_nugget')
        save(S.metal_block(r, style, seed=len(name)), 'block', f'{name}_block', metal_spec)
    for name in PLATES:
        save(surface_cue(S.plate(ramp_of(name)), name, ramp_of(name)), 'item', f'{name}_plate')
    for name in DUSTS:
        save(glints(S.dust(ramp_of(name), seed=len(name)), ramp_of(name), seed=len(name)), 'item', f'{name}_dust')
    for name in WIRES:
        save(S.wire(ramp_of(name)), 'item', f'{name}_wire')
    for name in RODS:
        save(S.rod(ramp_of(name)), 'item', f'{name}_rod')
    save(S.dust(COAL, seed=3), 'item', 'coal_dust')
    save(S.raw_chunk(P.SILICON, 'c', seed=11), 'item', 'silicon')
    save(S.wafer(P.SILICON), 'item', 'silicon_wafer')
    save(S.solar_cell_icon(P.CELL, P.ALUMINIUM), 'item', 'solar_cell')
    save(S.plate(P.PLASTIC), 'item', 'plastic_sheet')
    save(S.board(P.PCB, P.COPPER[5], COAL, [(4, 10), (11, 6), (12, 7)]), 'item', 'basic_circuit')
    save(S.board(P.ANODIZED_BLUE, P.GOLD[5], COAL, [(4, 10), (11, 6), (12, 7), (3, 9)], advanced=True), 'item', 'advanced_circuit')
    save(S.motor(P.CARBON_STEEL, P.COPPER, P.IRON), 'item', 'electric_motor')
    save(S.honeycomb(P.ABLATIVE), 'item', 'ablative_shield_plate')
    save(S.crumpled(P.GOLD), 'item', 'gold_foil_insulation')
    save(S.foam_block(P.FOAM), 'item', 'orange_foam_insulation')
    save(S.o_ring(P.SILICONE), 'item', 'silicone_seal')
    for kind in ('plate', 'wire', 'rod'):
        save(S.die(P.CARBON_STEEL, kind), 'item', f'{kind}_press_die')
    save(S.canister(WHITE_PAINT, P.GOLD, P.STEEL), 'item', 'cryogenic_canister')
    save(oil_seep(), 'block', 'oil_seep', (90, 20, 0, None))
