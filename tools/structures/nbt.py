"""Minimal NBT writer for structure templates (gzip, big-endian), no dependencies."""
import gzip
import struct

END, BYTE, SHORT, INT, LONG, FLOAT, DOUBLE, BYTE_ARRAY, STRING, LIST, COMPOUND, INT_ARRAY, LONG_ARRAY = range(13)


class Int(int):
    pass


class Byte(int):
    pass


class List(list):
    """Typed NBT list: List(INT, [1, 2, 3])."""

    def __init__(self, tag_type, items=()):
        super().__init__(items)
        self.tag_type = tag_type


def _type_of(v):
    if isinstance(v, bool) or isinstance(v, Byte):
        return BYTE
    if isinstance(v, Int) or isinstance(v, int):
        return INT
    if isinstance(v, float):
        return DOUBLE
    if isinstance(v, str):
        return STRING
    if isinstance(v, List):
        return LIST
    if isinstance(v, dict):
        return COMPOUND
    raise TypeError(type(v))


def _str(s):
    b = s.encode('utf-8')
    return struct.pack('>H', len(b)) + b


def _payload(t, v):
    if t == BYTE:
        return struct.pack('>b', int(v))
    if t == INT:
        return struct.pack('>i', v)
    if t == DOUBLE:
        return struct.pack('>d', v)
    if t == STRING:
        return _str(v)
    if t == LIST:
        et = v.tag_type if v else END
        out = struct.pack('>bi', et, len(v))
        for item in v:
            out += _payload(et, item)
        return out
    if t == COMPOUND:
        out = b''
        for k, item in v.items():
            it = _type_of(item)
            out += struct.pack('>b', it) + _str(k) + _payload(it, item)
        return out + struct.pack('>b', END)
    raise TypeError(t)


def write(path, root: dict):
    data = struct.pack('>b', COMPOUND) + _str('') + _payload(COMPOUND, root)
    with gzip.open(path, 'wb') as f:
        f.write(data)
