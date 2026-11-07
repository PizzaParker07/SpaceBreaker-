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

# Rutile: needle crystals, red-brown with a golden lustre.
RUTILE = ramp('#2e0f08', '#4f1b0e', '#742a14', '#98401c', '#b55a26', '#cd7a38', '#e09e55', '#efc180')
# Pentlandite: brassy bronze-yellow sulphide.
PENTLANDITE = ramp('#3a2c10', '#5b4618', '#7d6322', '#9e812f', '#b99d40', '#cfb758', '#e2cf7a', '#f1e4a6')
# Spodumene: pale lilac-pink prismatic laths (pegmatite).
SPODUMENE = ramp('#4a3a46', '#6a5566', '#8b7387', '#a891a5', '#c1abbf', '#d6c3d4', '#e7d8e5', '#f4ecf2')
# Wolframite: black-brown, sub-metallic.
WOLFRAMITE = ramp('#110d0b', '#1d1714', '#2b2320', '#3a302b', '#4b3f38', '#605148', '#7a685d', '#9a877a')
# Uraninite (pitchblende): pitch-black botryoidal grains weathering to yellow-green crusts.
URANINITE = ramp('#0c0b09', '#16150f', '#211f16', '#2d2b1d', '#3b3a24', '#4e4f2a', '#7c8634', '#c2cf5a')
# Crude oil / bitumen: brown-black with a greasy sheen.
CRUDE = ramp('#0b0907', '#15110d', '#201a14', '#2c241b', '#3a3025', '#4a3e30', '#5e4f3d', '#7a6852')

# --- Metals ----------------------------------------------------------------------
# Titanium: darker grey with a faint violet cast.
TITANIUM = ramp('#25242b', '#38363f', '#4d4a55', '#64606c', '#7c7885', '#97939f', '#b4b0bb', '#d2cfd7')
# Nickel: warm silver.
NICKEL = ramp('#3a372c', '#55513f', '#706b55', '#8d8770', '#aaa48c', '#c6c0a8', '#dfd9c3', '#f2eedf')
# Lithium: soft silver with a pale lilac tarnish.
LITHIUM = ramp('#3f3540', '#5c4f5e', '#7b6c7d', '#9a8a9b', '#b6a7b6', '#cfc2ce', '#e3d9e2', '#f4eef3')
# Tungsten: heavy dark steel grey.
TUNGSTEN = ramp('#16171b', '#24262c', '#33363e', '#444851', '#575c66', '#6c727d', '#858b96', '#a3a9b3')
# Uranium metal: dense grey that tarnishes towards a dull olive.
URANIUM = ramp('#1b1d18', '#2a2d25', '#3b3f34', '#4d5245', '#616757', '#777e6b', '#8f9782', '#acb49e')
# Carbon steel (ingots, plates, rods, dies).
CARBON_STEEL = ramp('#1f2124', '#303338', '#42464c', '#565a61', '#6b7077', '#82878e', '#9da1a7', '#bcc0c5')
# Aluminium-lithium alloy: champagne-tinted aluminium.
AL_LI = ramp('#36322a', '#544e43', '#757060', '#979180', '#b5b0a0', '#d0ccbe', '#e5e2d7', '#f7f5ef')
# Nickel superalloy: dull grey-green oxide.
SUPERALLOY = ramp('#22231f', '#33352f', '#474940', '#5d6054', '#75786b', '#8f9284', '#abae9f', '#c9cbbd')
# Vanilla metals (for plates/dusts/wires of iron, copper and gold).
IRON = ramp('#353535', '#4e4e4e', '#6b6b6b', '#8a8a8a', '#a8a8a8', '#c4c4c4', '#dcdcdc', '#f0f0f0')
COPPER = ramp('#4a1f12', '#6e3220', '#924630', '#b45b3e', '#cf734f', '#e38f68', '#f0ab88', '#f8cbb0')
# Silicon: blue-grey semimetal with a mirror finish.
SILICON = ramp('#14161f', '#1f2330', '#2c3244', '#3c4459', '#4f5a72', '#67748e', '#8693ab', '#b0bbcf')
# Plastic (HDPE-like off-white) and orange foam insulation.
PLASTIC = ramp('#5a5953', '#77766f', '#94938b', '#b0afa6', '#c8c7be', '#dbdad2', '#eae9e2', '#f6f5ef')
FOAM = ramp('#4a1a05', '#6e2a09', '#94400f', '#b85716', '#d26f22', '#e48a36', '#f0a656', '#f8c584')
# Silicone rubber (red-orange seals).
SILICONE = ramp('#3c1210', '#5a1b17', '#7a2620', '#9a332a', '#b54336', '#cb5a4a', '#de7a68', '#eda294')
# Circuit board solder mask (green) and ablative phenolic (dark brown-black).
PCB = ramp('#06190f', '#0b2818', '#113a22', '#184e2d', '#20633a', '#2b7a49', '#3d945d', '#5cb07a')
ABLATIVE = ramp('#120c09', '#1c1410', '#281d17', '#35281f', '#443428', '#564333', '#6c5641', '#866d54')
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

# --- Outer Solar System: asteroid belt ---------------------------------------------
# Ceres regolith: very dark (albedo 0.09) neutral grey with a faint warm cast (bead, then five powder tones).
CERES_SOIL = ramp('#363431', '#4b4845', '#524f4b', '#595652', '#605c58', '#68645f')
# Ammoniated phyllosilicate clay: dark grey-brown, a touch greener than the regolith.
AMMONIA_CLAY = ramp('#3c3d35', '#4d4d44', '#55544a', '#5c5b50', '#646256', '#6d6a5e')
# Sodium-carbonate faculae: bright, slightly warm white salt crust with soft grey shading.
CARBONATE = ramp('#9b988f', '#b8b5ac', '#c9c6bd', '#d7d4cc', '#e2dfd8', '#eeece6')
# Cerean rock: dark hydrated silicate rock, brown-grey.
CEREAN_ROCK = ramp('#2a2826', '#353230', '#3f3b38', '#4a4541', '#55504b', '#615b55', '#6e6761')
# Cerean ice: dirty water ice from below the regolith, cold grey-blue.
CEREAN_ICE = ramp('#56626b', '#67747e', '#788690', '#8a98a2', '#9eabb4', '#b6c1c9')
# Magnetite: black, sub-metallic with steel-grey glints.
MAGNETITE = ramp('#111113', '#20232b', '#3a404d', '#5d6575', '#9aa3b4')
# Vestan regolith (howardite dust): grey-buff, much brighter than Ceres.
VESTA_SOIL = ramp('#5c5650', '#736b63', '#7c736a', '#857b71', '#8e8378', '#998d81')
# Diogenitic regolith: ochre-tan dust of the Rheasilvia ejecta, orthopyroxene-rich.
DIOGENITIC_SOIL = ramp('#5f5240', '#75654d', '#7f6e53', '#89775a', '#938061', '#9f8b6a')
# Eucrite: fine basalt, grey with pale plagioclase laths.
EUCRITE = ramp('#3d3b39', '#4b4946', '#575451', '#63605c', '#706c67', '#8f8a83', '#b3aea6')
# Diogenite: coarse orthopyroxene, greenish yellow-brown.
DIOGENITE = ramp('#423f31', '#544f3b', '#655f45', '#766f50', '#867f5c', '#978f6a')
# Chromite: black-brown sub-metallic grains.
CHROMITE = ramp('#141110', '#251f1c', '#3a322d', '#584d46', '#7d726b')
# Rock mite: dark carbonaceous carapace, pale salt crystals.
ROCK_MITE = ramp('#1f1d1c', '#2b2826', '#363230', '#423d3a', '#504a46', '#5f5853')
MITE_SALT = ramp('#9d998f', '#c3bfb5', '#dedad1', '#f1eee8')

# --- Outer Solar System: Jupiter's moons ------------------------------------------------
# Europa: bright, faintly cream frost; pale blue-grey shell ice; red-brown stained lineae ice; orange-brown chaos brine.
EUROPA_FROST = ramp('#a9a59c', '#c9c5bc', '#d3cfc6', '#dcd8d0', '#e4e1da', '#ecebe5')
EUROPA_ICE = ramp('#7f8f99', '#94a4ae', '#a7b6bf', '#b8c6ce', '#c9d5dc', '#dbe4ea')
STAINED_ICE = ramp('#5e3f2f', '#734e3a', '#865d45', '#976c51', '#a87d60', '#b98f72')
BRINE_ICE = ramp('#6a4a35', '#7f5a41', '#93694d', '#a6795b', '#b98c6d', '#d9cbb8')
OCEAN_SALT = ramp('#8b8f96', '#a9adb3', '#c4c8cd', '#dadde1', '#eceef0', '#fbfbfc')
# Io: muted sulfur yellow sand, white SO2 frost, rusty red short-chain sulfur, near-black silicate lava.
SULFUR_SAND = ramp('#857429', '#a38f37', '#ac983e', '#b5a045', '#bea94d', '#c7b257')
SO2_FROST = ramp('#b3b19f', '#cdccbd', '#d6d5c7', '#dfded2', '#e7e7dc', '#efefe7')
RED_SULFUR = ramp('#6a2915', '#86391e', '#914023', '#9c4829', '#a75130', '#b35b37')
IONIAN_BASALT = ramp('#161718', '#1e2021', '#27292a', '#313334', '#3c3e40', '#484a4c')
IO_SULFUR = ramp('#5e4d12', '#8a7419', '#b39b25', '#d4bd3c', '#ead864')
# Ganymede: grey-brown dirty regolith, bright clean grooved ice, dark ancient crust.
GANYMEDE_SOIL = ramp('#4f4a44', '#665f57', '#6e665e', '#766e65', '#7e766c', '#877e74')
GROOVED_ICE = ramp('#8e9196', '#a3a6ab', '#b4b7bb', '#c3c6c9', '#d1d3d6', '#dfe1e3')
DARK_ICE_CRUST = ramp('#2f2b27', '#3b3631', '#46403a', '#514a43', '#5c544c', '#685f56')
# Callisto: the darkest Galilean regolith, white frost knobs, dark icy rock.
CALLISTO_SOIL = ramp('#2e2b27', '#3e3a34', '#45403a', '#4b4640', '#524c45', '#5a534c')
BRIGHT_FROST = ramp('#a8adb3', '#c6cbd0', '#d1d5d9', '#dbdee2', '#e4e7ea', '#eef0f2')
CALLISTAN_ROCK = ramp('#262523', '#302e2b', '#3a3734', '#45413d', '#504b47', '#5c5752', '#6a645e')
# Creatures: glow serpent (deep blue-black skin, cyan glow), sulfur crab (glazed yellow-orange shell).
SERPENT = ramp('#0d141b', '#142029', '#1c2c38', '#253948', '#304858', '#3d5869')
SERPENT_GLOW = ramp('#2a9e98', '#5ef0e0', '#c8fff6')
CRAB = ramp('#4a2a10', '#6e3f15', '#8f5a1c', '#b07c27', '#c99d35', '#dcbd4f', '#ead777')

# --- Outer Solar System: Saturn's moons ---------------------------------------------------
# Titan: brown hydrocarbon dune sand, orange-brown organic mud, beige water-ice bedrock, grey ice cobbles, tholin.
THOLIN_SAND = ramp('#3d2b1b', '#513a25', '#59402a', '#61472f', '#6a4e34', '#73563a')
THOLIN_MUD = ramp('#4a2d16', '#5f3a1d', '#674122', '#704826', '#794f2b', '#835730')
TITAN_ICE = ramp('#6c6458', '#82796c', '#948b7e', '#a59d90', '#b5ada1', '#c5beb3', '#d4cec5')
ICE_COBBLES = ramp('#4a3a2a', '#7d766d', '#958e84', '#aba49a', '#c1bbb2', '#d6d1c9')
THOLIN = ramp('#3a1e0a', '#5c3010', '#7f4517', '#a35e21', '#c27a30', '#d99a4a', '#e8b96e')
# Enceladus: the whitest snow in the Solar System, bright blue-white ice, deeper blue tiger-stripe ice.
ENCELADUS_SNOW = ramp('#b4c0c8', '#d2dbe1', '#dde4e9', '#e6ecf0', '#eef2f5', '#f7f9fb')
ENCELADUS_ICE = ramp('#8fa4b3', '#a6b9c6', '#b9cad5', '#cad8e1', '#dae5ec', '#e9f0f5')
SULCUS_ICE = ramp('#3e6e86', '#4c8099', '#5a92ab', '#6aa3bc', '#7db4cb', '#97c7da')
# Creatures: methane skater (dark amber body, pale stripes), geyser drifter (translucent blue-white bell).
SKATER = ramp('#22150b', '#35210f', '#4a2f15', '#62401d', '#7c5428', '#9a6c36', '#b98a4c')
# Tholin worm: rust-orange rings, darker grooves, a pale sifting plate on the head.
WORM = ramp('#2a160a', '#3e2111', '#56301a', '#6e4024', '#895430', '#a56c3f', '#c0884f')
DRIFTER = ramp('#7fa7bf', '#9cbfd3', '#b9d6e5', '#d5ebf4', '#eef8fc')
DRIFTER_GLOW = ramp('#3f9fd9', '#86d3ff', '#d8f4ff')
# Groove grazer (grey-blue banded plates, pale frosted edges) and crater hopper (dark brown-grey fur-like hide, white plates).
GRAZER = ramp('#2c3036', '#3d434b', '#4f5761', '#636c77', '#7a838e', '#959ea8', '#b6bec6')
HOPPER = ramp('#1f1c19', '#2d2925', '#3d3832', '#4f4841', '#625a51', '#776e64')
HOPPER_PLATE = ramp('#a8adb3', '#d1d5d9', '#eef0f2')

# --------------------------------------------------------------------------- Pluto and Charon
# Plutonian frost: pale beige nitrogen-methane frost with a faint warm tint.
PLUTO_FROST = ramp('#8c8070', '#a59888', '#b3a796', '#c0b4a3', '#ccc1b1', '#d9cfc0')
# Red tholin regolith: the dark red-brown equatorial lowlands.
PLUTO_THOLIN = ramp('#2e140d', '#3f1c12', '#4f2417', '#5f2d1c', '#713722', '#85432a')
# Nitrogen ice: soft white with a faint blue cast, glassy.
NITROGEN_ICE = ramp('#a7b1b8', '#bcc6cc', '#cdd5da', '#dbe2e6', '#e7ecef', '#f3f6f7')
# Methane frost: the brightest snow, slightly warm.
METHANE_FROST = ramp('#b9b4ab', '#cdc8bf', '#dbd7cf', '#e7e4dd', '#f1efea', '#faf9f6')
# Plutonian bedrock ice: water ice hard as granite, grey-beige.
PLUTO_ICE = ramp('#6a645c', '#7c766d', '#8d867d', '#9d968c', '#ada69c', '#bdb6ac')
# Charon: neutral grey water ice and regolith.
CHARON_SOIL = ramp('#4a4846', '#5a5856', '#666462', '#71706d', '#7d7b78', '#8a8885')
CHARON_ICE = ramp('#545352', '#636260', '#716f6d', '#7f7d7a', '#8d8b88', '#a09e9a')
# Red polar frost: tholin-stained frost of Charon's pole, darker and browner than Pluto's.
RED_FROST = ramp('#2a120c', '#3a1911', '#4a2116', '#5a2a1b', '#6c3523', '#7f412b')
# Ammonia hydrate: pale cyan-white crystals.
AMMONIA = ramp('#5d7a80', '#86a7ad', '#acc9cd', '#cfe5e7', '#eef9fa')
# Creatures.
SLIDER = ramp('#4a3a32', '#62503f', '#7f6a55', '#9c876f', '#b9a58b', '#d2c2aa')
TORTOISE = ramp('#26221f', '#36312d', '#47413b', '#59524a', '#6c645b', '#80786e')

# --------------------------------------------------------------------------- Triton
# Tritonian frost: grey with a faint green cast (the "cantaloupe" terrain).
TRITON_FROST = ramp('#6d7369', '#80867b', '#8e9488', '#9ba194', '#a8ada0', '#b6baad')
# Pink nitrogen frost of the polar cap.
PINK_FROST = ramp('#a88a86', '#bc9f9a', '#cab0aa', '#d6beb8', '#e1cbc6', '#ecd9d5')
# Dark plume dust: the streaks laid down by the geysers.
PLUME_DUST = ramp('#1b1a1a', '#262424', '#302e2e', '#3b3838', '#474343', '#534e4e')
# Tritonian bedrock ice: grey-green water ice.
TRITON_ICE = ramp('#5c6359', '#6b7367', '#7a8275', '#889083', '#979f91', '#a7ae9f')
# Methane clathrate: milky ice cages with yellowish methane.
CLATHRATE = ramp('#7c7a63', '#9e9b7c', '#bdb995', '#d6d2ae', '#ecead0')
BEETLE = ramp('#121112', '#1d1b1d', '#2a272a', '#383438', '#494449', '#5b555b')

# --------------------------------------------------------------------------- Miranda
MIRANDA_SOIL = ramp('#434a52', '#4f5760', '#5a636c', '#666f78', '#727b84', '#7f8891')
MIRANDA_ICE = ramp('#48505a', '#555e68', '#626b75', '#6f7882', '#7d8690', '#8d969f')
BRIGHT_RIDGE = ramp('#a6aaad', '#b8bcbf', '#c8cbce', '#d6d9db', '#e3e5e6', '#eff0f1')
DARK_BAND = ramp('#2c2c2c', '#363635', '#40403f', '#4a4a49', '#555554', '#61615f')
CLINGER = ramp('#2a2a2b', '#3a3a3b', '#4c4c4d', '#606061', '#78787a', '#929294')
# Stained nitrogen ice: the dark grey-brown methane-stained ice of the cell troughs, a hint of tholin red.
STAINED_N2 = ramp('#3b3330', '#4a403b', '#574b44', '#64574e', '#72645a', '#837266')

# --------------------------------------------------------------------------- Ariel (ramps from docs/ASTRO.md)
ARIEL_SOIL = ramp('#8e959c', '#a1a8af', '#b4bbc2', '#c6cdd3', '#d6dce1', '#e5eaee')
ARIEL_ICE = ramp('#7f868e', '#9098a0', '#a2a9b1', '#b3bac1', '#c3c9cf', '#d3d8dd')
GRABEN_FLOOR = ramp('#7f7a74', '#8c8781', '#9a958e', '#a7a29b', '#b4afa8', '#c2bdb6')
CO2_FROST = ramp('#b9bcbf', '#c7cacd', '#d1d4d7', '#dadde0', '#e3e5e7', '#eeeff0')
MOTH = ramp('#7a7d80', '#9a9da0', '#b9bcbf', '#d3d5d7', '#e7e8e9', '#f4f5f5')

# --------------------------------------------------------------------------- Titania (ramps from docs/ASTRO.md)
TITANIA_SOIL = ramp('#4f4943', '#5d5650', '#6b635c', '#797068', '#867c73', '#93887e')
TITANIA_ICE = ramp('#57514b', '#655e57', '#736b63', '#81786f', '#8e847a', '#9b9085')
CANYON_WALL = ramp('#8d8880', '#9c968e', '#a9a39b', '#b5b0a8', '#c2bdb5', '#cfcac2')
UMBRIEL_SOIL = ramp('#232221', '#2c2b29', '#353331', '#3f3d3a', '#4a4844', '#55524e')
UMBRIEL_ICE = ramp('#2a2928', '#33322f', '#3d3b38', '#484642', '#54514c', '#605c57')
SOOT = ramp('#121111', '#191817', '#211f1e', '#2a2826', '#34312f', '#3e3b38')
HOPPER = ramp('#1c1b1a', '#252423', '#2f2d2b', '#3a3835', '#4a4744', '#c9ced2')
OBERON_SOIL = ramp('#50453f', '#5c4f49', '#695952', '#75635c', '#816f65', '#8e7a70')
OBERON_ICE = ramp('#554b46', '#625650', '#6e615b', '#7b6c65', '#877770', '#94837b')
DARK_FILL = ramp('#181514', '#1f1b19', '#26211f', '#2e2825', '#37302c', '#403833')
BURROWER = ramp('#3a322d', '#4a403a', '#5a4f48', '#6c5f56', '#7f7066', '#d8cbb8')
ROLLER = ramp('#2f2c29', '#3f3b37', '#514c47', '#655f58', '#7b746c', '#928a81')
