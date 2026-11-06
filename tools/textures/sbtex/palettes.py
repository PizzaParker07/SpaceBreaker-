"""Material colour ramps (dark -> light). One ramp per material, shared by every texture."""
from .core import ramp

# --- Moon -------------------------------------------------------------------
# Mare basalt: dark, very slightly cool grey, fine grained.
MARE_BASALT = ramp('#202024', '#29292e', '#323237', '#3c3c41', '#47474c', '#545459', '#62626a')
# Lunar regolith (highlands): powdery neutral grey, a touch warm.
REGOLITH = ramp('#55534f', '#62605c', '#6e6c68', '#7a7874', '#86847f', '#93918b', '#a19e98', '#b0ada6')
# Highland soil tones: narrow spread like vanilla sand (bead, then five powder tones).
REGOLITH_SOIL = ramp('#5c5a56', '#74726d', '#7c7a75', '#85837e', '#8e8c86', '#99968f')
# Mare regolith: the dark soil of the lunar seas (basalt dust + glass).
MARE_REGOLITH = ramp('#353432', '#3f3e3c', '#494846', '#53524f', '#5e5c59', '#6a6865', '#777571')
# Mare soil tones (bead, then five powder tones).
MARE_SOIL = ramp('#2f2e2c', '#454441', '#4c4b48', '#53524f', '#5b5a56', '#64625e')
# Orange volcanic glass soil (pyroclastic beads, Apollo 17 style) + black beads.
VOLCANIC_GLASS = ramp('#2e2723', '#6b4a35', '#7b563c', '#8a6344', '#98704d', '#a67e59')
# Frost on polar regolith.
FROST = ramp('#a9bcc4', '#cfdde2', '#eef5f7')
# Anorthosite: pale plagioclase rock of the highlands, cool light grey.
ANORTHOSITE = ramp('#6f706f', '#8a8b89', '#9c9d9a', '#adaeab', '#bebfbb', '#cfd0cc', '#dfe0dc')
# Impact breccia: mid-grey melt matrix with light and dark clasts.
BRECCIA = ramp('#3e3d3b', '#52504d', '#605e5a', '#6e6c68', '#8b8a85', '#a9a8a2', '#c3c2bc')
# Lunar polar ice: dusty, desaturated blue-grey.
LUNAR_ICE = ramp('#4e5d68', '#61737f', '#768893', '#8b9ca6', '#a3b2ba', '#bfcbd1', '#dbe4e8')
# Sintered regolith: warm dark-grey ceramic.
SINTERED = ramp('#2f2c29', '#3d3a36', '#4a4642', '#57524d', '#635e58', '#706a63', '#7e7770')
# Meteoric iron: dark fusion crust over iron-nickel metal.
METEORIC_IRON = ramp('#221f1d', '#332e2a', '#463f39', '#5a524b', '#7a746e', '#a19d98', '#cfccc7')
# Ilmenite: glossy black with a cold violet sheen.
ILMENITE = ramp('#08070b', '#121017', '#1c1a22', '#2a2631', '#454050', '#7b7489', '#bdb7cc', '#ebe8f4')
# KREEP: phosphate/zircon-rich crystals, pale greenish yellow.
KREEP = ramp('#4c5126', '#6b7234', '#8f9846', '#b2ba62', '#d3d98e', '#eef0c4')

# --- Earth ores ----------------------------------------------------------------
# Bauxite pisoids: red-brown to ochre.
BAUXITE = ramp('#3f1d13', '#5c2a1a', '#7a3a22', '#94492a', '#ab5b34', '#c07345', '#d18f5e')

# --- Metals ----------------------------------------------------------------------
# Aluminium: cool, bright, slightly blue grey.
ALUMINIUM = ramp('#2c323c', '#4a525e', '#6c7581', '#8f98a4', '#b0b8c2', '#cdd3da', '#e4e8ed', '#f7f9fb')
# Galvanised steel (mounts, bolts).
STEEL = ramp('#1d2024', '#2b2f35', '#3a3f46', '#4b5159', '#5e646d', '#737a83', '#8b929a', '#a7adb4')
# Brass (valves).
BRASS = ramp('#3b2608', '#5f3e0e', '#875a14', '#ad7a1f', '#cc9a33', '#e2b852', '#f1d27c', '#fbe9b0')
# Gold foil / sun visor.
GOLD = ramp('#3d2503', '#6b4206', '#99630c', '#c08616', '#dca62a', '#efc44a', '#f9de7c', '#fff3c4')

# --- Solar cells -----------------------------------------------------------------
CELL = ramp('#070b1c', '#0c1430', '#122046', '#1a2c5c', '#253c74', '#33508d', '#4b6ba8', '#7393c8')
BACKSHEET = ramp('#7d8088', '#9a9da4', '#b5b8be', '#cfd1d5', '#e2e4e7', '#f1f2f4')

# --- Suit / paint ---------------------------------------------------------------
SUIT_WHITE = ramp('#5b5e66', '#7c8089', '#9da1aa', '#bbbfc6', '#d3d6db', '#e5e7ea', '#f2f3f5', '#fbfbfc')
ANODIZED_BLUE = ramp('#08152e', '#0f2448', '#173766', '#214c86', '#2e64a6', '#4580c2', '#68a0d8', '#98c3ea')
OXYGEN_CYAN = ramp('#06303a', '#0b4654', '#115f6f', '#17798b', '#2194a6', '#36b1c0', '#5ecbd6', '#98e3ea')
VISOR_GLASS = ramp('#0a0f17', '#121a26', '#1c2838', '#2a3a4f', '#3f536b', '#5d7390', '#8aa0bb', '#c7d4e4')
RUBBER = ramp('#101113', '#18191c', '#212327', '#2b2e33', '#373a40', '#45494f')
LAMP = ramp('#5b4a1a', '#9c8433', '#d7c160', '#f3e39a', '#fff7d6', '#ffffff')
