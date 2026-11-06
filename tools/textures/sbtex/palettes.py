"""Material colour ramps (dark -> light). One ramp per material, shared by every texture."""
from .core import ramp

# --- Moon -------------------------------------------------------------------
# Mare basalt: dark, very slightly warm grey, fine grained.
MARE_BASALT = ramp('#1f1e22', '#2a292d', '#343337', '#3e3d41', '#49474b', '#555256', '#625f62')
# Lunar regolith: powdery neutral grey (highland/mare mix).
REGOLITH = ramp('#55534f', '#62605c', '#6e6c68', '#7a7874', '#86847f', '#93908b', '#a19e98', '#b0ada6')
# Ilmenite: glossy black with a cold violet sheen.
ILMENITE = ramp('#08070b', '#121017', '#1c1a22', '#2a2631', '#454050', '#7b7489', '#bdb7cc', '#ebe8f4')

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
