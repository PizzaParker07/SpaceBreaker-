"""Structure template builder: place blocks by id + properties, write a vanilla structure .nbt.
Positions never set are left out of the file (they act like structure voids: terrain stays)."""
import os

from nbt import COMPOUND, INT, List, write

DATA_VERSION = 3955  # Minecraft 1.21.1


class Template:
    def __init__(self, sx, sy, sz):
        self.size = (sx, sy, sz)
        self.blocks = {}  # (x,y,z) -> (name, props tuple, nbt or None)

    def inside(self, x, y, z):
        sx, sy, sz = self.size
        return 0 <= x < sx and 0 <= y < sy and 0 <= z < sz

    def set(self, x, y, z, name, nbt=None, **props):
        if not self.inside(x, y, z):
            return
        if ':' not in name:
            name = 'minecraft:' + name
        self.blocks[(x, y, z)] = (name, tuple(sorted((k, str(v).lower()) for k, v in props.items())), nbt)

    def get(self, x, y, z):
        b = self.blocks.get((x, y, z))
        return b[0] if b else None

    def clear(self, x, y, z):
        self.blocks.pop((x, y, z), None)

    def fill(self, x0, y0, z0, x1, y1, z1, name, **props):
        for x in range(min(x0, x1), max(x0, x1) + 1):
            for y in range(min(y0, y1), max(y0, y1) + 1):
                for z in range(min(z0, z1), max(z0, z1) + 1):
                    self.set(x, y, z, name, **props)

    def chest(self, x, y, z, loot_table, facing='north', seed=0):
        self.set(x, y, z, 'chest', nbt={'id': 'minecraft:chest', 'LootTable': loot_table},
                 facing=facing, type='single', waterlogged='false')

    def save(self, path):
        palette, index = [], {}
        blocks = List(COMPOUND)
        for (x, y, z), (name, props, nbt) in sorted(self.blocks.items(), key=lambda kv: (kv[0][1], kv[0][2], kv[0][0])):
            key = (name, props)
            if key not in index:
                index[key] = len(palette)
                entry = {'Name': name}
                if props:
                    entry['Properties'] = {k: v for k, v in props}
                palette.append(entry)
            b = {'pos': List(INT, [x, y, z]), 'state': index[key]}
            if nbt:
                b['nbt'] = nbt
            blocks.append(b)
        root = {
            'DataVersion': DATA_VERSION,
            'size': List(INT, list(self.size)),
            'palette': List(COMPOUND, palette),
            'blocks': blocks,
            'entities': List(COMPOUND, []),
        }
        os.makedirs(os.path.dirname(path), exist_ok=True)
        write(path, root)
        return len(self.blocks), len(palette)
