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
VESTA_SOIL = ramp('#5a564f', '#716c65', '#79746c', '#827c74', '#8a847c', '#948e85')
# Eucrite: fine basalt, grey with pale plagioclase laths.
EUCRITE = ramp('#3d3b39', '#4b4946', '#575451', '#63605c', '#706c67', '#8f8a83', '#b3aea6')
# Diogenite: coarse orthopyroxene, greenish yellow-brown.
DIOGENITE = ramp('#423f31', '#544f3b', '#655f45', '#766f50', '#867f5c', '#978f6a')
# Chromite: black-brown sub-metallic grains.
CHROMITE = ramp('#141110', '#251f1c', '#3a322d', '#584d46', '#7d726b')
# Rock mite: dark carbonaceous carapace, pale salt crystals.
ROCK_MITE = ramp('#1f1d1c', '#2b2826', '#363230', '#423d3a', '#504a46', '#5f5853')
MITE_SALT = ramp('#9d998f', '#c3bfb5', '#dedad1', '#f1eee8')
