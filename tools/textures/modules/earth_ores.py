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


def export(save):
    from sbtex import shapes, palettes as P
    save(raw_bauxite_block(), 'block', 'raw_bauxite_block', (40, 10, 50, None))
    save(bauxite_overlay(), 'block', 'bauxite_ore_overlay', (55, 18, 30, None), wrap=False)
    save(shapes.raw_bauxite(), 'item', 'raw_bauxite')
    save(shapes.ingot(P.ALUMINIUM), 'item', 'aluminum_ingot')
    save(shapes.plate(P.ALUMINIUM), 'item', 'aluminum_plate')
