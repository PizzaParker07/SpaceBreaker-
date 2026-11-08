# Sanayi denge tablosu (2026-11-08)

Bu belge yalnızca rapordur; değişiklikler onaydan sonra yapılacak. Veriler tarif JSON'larından ve kaynak koddaki sabitlerden
çıkarıldı, oyun koşusu yapılmadı. Analiz betiği `build` dışında tutuldu; tabloyu yeniden üretmek için tarif klasörleri
(`src/main/resources/data`, `src/generated/resources/data`) okunur.

Okuma notları:
- Süreler tick/20 = saniye. FE/t, makinenin işlem boyunca her tick çektiği enerji; FE/işlem = süre × FE/t.
- JSON'da yazılmayan alanlar kodun varsayılanını alır: süre 200 tick (10 s), enerji 20 FE/t. Aşağıdaki tabloda uygulandı.
- "Dengeli" ayar sanayiyi etkilemiyor: realism yalnız roketlerin Δv ve yerçekimi hesabını ölçekliyor. Sanayi rakamları
  her ayarda aynı.

## 1. Enerji üreticileri

| Üretici | FE/t | Not |
|---|---|---|
| Yanmalı jeneratör | 40 | vanilla yakıt süresi kadar (kömür 1600 tick → 64 000 FE) |
| Güneş paneli | 20 (tepe) | gezegene göre ışık yüzdesi, toz %40'a kadar düşürür |
| Güneş paneli dizisi | 60 (tepe) | 3 takipçi |
| RTG | 40 (taze pelet) | etkinlik 21 oyun gününde yarıya iner |
| Fisyon ünitesi | 240 | eleman başına 240 000 tick (10 oyun günü) tam güç = 57,6 M FE (önce 320 FE/t, 76,8 M) |
| Pumpjack (tüketir) | −30 | 40 tickte 250 mB ham petrol |

Kablolar: bakır 512 FE/t, yüksek gerilim 8192 FE/t.

## 2. Makine tarifleri

### Ark ocağı (`arc_smelting`)

| Tarif | Girdi | Çıktı | Süre | FE/t | FE/işlem |
|---|---|---|---|---|---|
| `aluminum_from_dust` | 1× #c:dusts/aluminum | 1× aluminum_ingot | 4 s | 40 | 3,200 |
| `aluminum_lithium` | 3× #c:ingots/aluminum + 1× #c:ingots/lithium | 4× aluminum_lithium_ingot | 10 s | 80 | 16,000 |
| `copper_from_dust` | 1× #c:dusts/copper | 1× minecraft:copper_ingot | 4 s | 40 | 3,200 |
| `gold_from_dust` | 1× #c:dusts/gold | 1× minecraft:gold_ingot | 4 s | 40 | 3,200 |
| `iron_from_dust` | 1× #c:dusts/iron | 1× minecraft:iron_ingot | 4 s | 40 | 3,200 |
| `lithium_from_dust` | 1× #c:dusts/lithium | 1× lithium_ingot | 4 s | 40 | 3,200 |
| `nickel_from_dust` | 1× #c:dusts/nickel | 1× nickel_ingot | 4 s | 40 | 3,200 |
| `nickel_superalloy` | 3× #c:ingots/nickel + 1× #c:ingots/titanium + 1× #c:ingots/steel | 4× nickel_superalloy_ingot | 12 s | 100 | 24,000 |
| `nuclear_fuel_element` | 2× enriched_uranium + 2× #c:dusts/coal + 2× #c:rods/tungsten | 1× nuclear_fuel_element | 20 s | 120 | 48,000 |
| `silicon_from_quartz` | 1× #c:gems/quartz + 1× #c:dusts/coal | 2× silicon | 8 s | 80 | 12,800 |
| `silicon_from_sand` | 2× #c:sands + 1× #c:dusts/coal | 1× silicon | 10 s | 80 | 16,000 |
| `steel` | 1× #c:ingots/iron + 1× #c:dusts/coal | 1× steel_ingot | 8 s | 60 | 9,600 |
| `titanium_from_dust` | 1× #c:dusts/titanium | 1× titanium_ingot | 4 s | 40 | 3,200 |
| `titanium_from_ore` | 1× #c:ores/rutile | 1× titanium_ingot | 8 s | 80 | 12,800 |
| `titanium_from_raw` | 1× #c:raw_materials/rutile | 1× titanium_ingot | 8 s | 80 | 12,800 |
| `tungsten_from_dust` | 1× #c:dusts/tungsten | 1× tungsten_ingot | 4 s | 40 | 3,200 |
| `tungsten_from_ore` | 1× #c:ores/wolframite | 1× tungsten_ingot | 8 s | 80 | 12,800 |
| `tungsten_from_raw` | 1× #c:raw_materials/wolframite | 1× tungsten_ingot | 8 s | 80 | 12,800 |
| `uranium_from_dust` | 1× #c:dusts/uranium | 1× uranium_ingot | 4 s | 40 | 3,200 |
| `uranium_from_ore` | 1× #c:ores/uraninite | 1× uranium_ingot | 8 s | 80 | 12,800 |
| `uranium_from_raw` | 1× #c:raw_materials/uraninite | 1× uranium_ingot | 8 s | 80 | 12,800 |

### Gaz santrifüjü (`centrifuging`)

| Tarif | Girdi | Çıktı | Süre | FE/t | FE/işlem |
|---|---|---|---|---|---|
| `enriched_uranium` | 4× yellowcake | 1× enriched_uranium + 3× depleted_uranium | 40 s | 160 | 128,000 |

### Kimyasal reaktör (`chemical_reaction`)

| Tarif | Girdi | Çıktı | Süre | FE/t | FE/işlem |
|---|---|---|---|---|---|
| `graphite_combustion` | 1× graphite + 500 mB c:oxygen | 500 mB carbon_dioxide | 3 s | 40 | 2,400 |
| `sulfuric_acid` | 1× #c:dusts/sulfur + 250 mB c:water | 250 mB sulfuric_acid | 5 s | 60 | 6,000 |
| `yellowcake` | 1× #c:dusts/uranium + 125 mB c:sulfuric_acid | 1× yellowcake | 4 s | 60 | 4,800 |

### Kırıcı (`crushing`)

| Tarif | Girdi | Çıktı | Süre | FE/t | FE/işlem |
|---|---|---|---|---|---|
| `aluminum_ingot` | 1× #c:ingots/aluminum | 1× aluminum_dust | 4 s | 16 | 1,280 |
| `bauxite_ore` | 1× #c:ores/bauxite | 2× aluminum_dust | 6 s | 20 | 2,400 |
| `coal` | 1× minecraft:coal | 1× coal_dust | 3 s | 12 | 720 |
| `cobblestone` | 1× #c:cobblestones/normal | 1× minecraft:gravel | 3 s | 12 | 720 |
| `copper_ingot` | 1× #c:ingots/copper | 1× copper_dust | 4 s | 16 | 1,280 |
| `copper_ore` | 1× #c:ores/copper | 2× copper_dust | 6 s | 20 | 2,400 |
| `dusty_ice` | 1× dusty_ice | 1× minecraft:ice | 4 s | 12 | 960 |
| `gold_ingot` | 1× #c:ingots/gold | 1× gold_dust | 4 s | 16 | 1,280 |
| `gold_ore` | 1× #c:ores/gold | 2× gold_dust | 6 s | 20 | 2,400 |
| `gravel` | 1× #c:gravels | 1× minecraft:sand | 3 s | 12 | 720 |
| `hollow_deposit` | 1× hollow_deposit | 1× sulfur | 4 s | 14 | 1,120 |
| `iron_ingot` | 1× #c:ingots/iron | 1× iron_dust | 4 s | 16 | 1,280 |
| `iron_ore` | 1× #c:ores/iron | 2× iron_dust | 6 s | 20 | 2,400 |
| `lithium_ingot` | 1× #c:ingots/lithium | 1× lithium_dust | 4 s | 16 | 1,280 |
| `martian_ice` | 1× martian_ice | 1× minecraft:ice | 3 s | 10 | 600 |
| `mercury_ice` | 1× mercury_ice | 1× minecraft:ice | 3 s | 10 | 600 |
| `nickel_ingot` | 1× #c:ingots/nickel | 1× nickel_dust | 4 s | 16 | 1,280 |
| `pentlandite_ore` | 1× #c:ores/pentlandite | 2× nickel_dust | 6 s | 20 | 2,400 |
| `perchlorate_soil` | 1× perchlorate_soil | 1× perchlorate_salt | 5 s | 16 | 1,600 |
| `polar_crater_ice` | 1× lunar_ice | 1× minecraft:ice | 3 s | 10 | 600 |
| `raw_bauxite` | 1× #c:raw_materials/bauxite | 1× aluminum_dust | 5 s | 20 | 2,000 |
| `raw_copper` | 1× #c:raw_materials/copper | 1× copper_dust | 5 s | 20 | 2,000 |
| `raw_gold` | 1× #c:raw_materials/gold | 1× gold_dust | 5 s | 20 | 2,000 |
| `raw_iron` | 1× #c:raw_materials/iron | 1× iron_dust | 5 s | 20 | 2,000 |
| `raw_pentlandite` | 1× #c:raw_materials/pentlandite | 1× nickel_dust | 5 s | 20 | 2,000 |
| `raw_rutile` | 1× #c:raw_materials/rutile | 1× titanium_dust | 5 s | 20 | 2,000 |
| `raw_spodumene` | 1× #c:raw_materials/spodumene | 1× lithium_dust | 5 s | 20 | 2,000 |
| `raw_uraninite` | 1× #c:raw_materials/uraninite | 1× uranium_dust | 5 s | 20 | 2,000 |
| `raw_wolframite` | 1× #c:raw_materials/wolframite | 1× tungsten_dust | 5 s | 20 | 2,000 |
| `rutile_ore` | 1× #c:ores/rutile | 2× titanium_dust | 6 s | 20 | 2,400 |
| `spodumene_ore` | 1× #c:ores/spodumene | 2× lithium_dust | 6 s | 20 | 2,400 |
| `stony_asteroid_metal` | 1× #c:regoliths/stony | 1× iron_dust | 4 s | 16 | 1,280 |
| `sulfur_crust` | 1× sulfur_crust | 2× sulfur | 4 s | 14 | 1,120 |
| `titanium_ingot` | 1× #c:ingots/titanium | 1× titanium_dust | 4 s | 16 | 1,280 |
| `tungsten_ingot` | 1× #c:ingots/tungsten | 1× tungsten_dust | 4 s | 16 | 1,280 |
| `uraninite_ore` | 1× #c:ores/uraninite | 2× uranium_dust | 6 s | 20 | 2,400 |
| `uranium_ingot` | 1× #c:ingots/uranium | 1× uranium_dust | 4 s | 16 | 1,280 |
| `wolframite_ore` | 1× #c:ores/wolframite | 2× tungsten_dust | 6 s | 20 | 2,400 |

### Elektrolizör (`electrolysis`)

| Tarif | Girdi | Çıktı | Süre | FE/t | FE/işlem |
|---|---|---|---|---|---|
| `water` | 500 mB c:water | 1000 mB hydrogen + 500 mB oxygen | 2 s | 120 | 4,800 |

### İmalatçı (`fabricating`)

| Tarif | Girdi | Çıktı | Süre | FE/t | FE/işlem |
|---|---|---|---|---|---|
| `ablative_shield_plate` | 2× #c:plastics + 2× #c:dusts/coal + 1× #c:plates/titanium | 2× ablative_shield_plate | 8 s | 40 | 6,400 |
| `advanced_circuit` | 2× basic_circuit + 2× silicon_wafer + 1× #c:wires/gold + 1× #c:plastics | 1× advanced_circuit | 10 s | 60 | 12,000 |
| `electric_motor` | 2× #c:plates/steel + 4× #c:wires/copper + 1× #c:rods/steel | 2× electric_motor | 6 s | 40 | 4,800 |
| `gold_foil_insulation` | 1× #c:plates/gold + 1× #c:plastics | 4× gold_foil_insulation | 4 s | 30 | 2,400 |
| `orange_foam_insulation` | 2× #c:plastics + 1× #c:dyes/orange | 4× orange_foam_insulation | 4 s | 30 | 2,400 |
| `solar_cell` | 1× silicon_wafer + 1× #c:wires/copper + 1× #c:glass_panes/colorless | 1× solar_cell | 6 s | 40 | 4,800 |

### Sıvılaştırıcı (`liquefaction`)

| Tarif | Girdi | Çıktı | Süre | FE/t | FE/işlem |
|---|---|---|---|---|---|
| `hydrogen` | 1000 mB c:hydrogen | 250 mB liquid_hydrogen | 4 s | 160 | 12,800 |
| `methane` | 1000 mB c:methane | 500 mB liquid_methane | 3 s | 100 | 6,000 |
| `oxygen` | 1000 mB c:oxygen | 500 mB liquid_oxygen | 3 s | 100 | 6,000 |

### Buz eritici (`melting`)

| Tarif | Girdi | Çıktı | Süre | FE/t | FE/işlem |
|---|---|---|---|---|---|
| `dry_ice` | 1× #c:dry_ices | 1000 mB carbon_dioxide | 2 s | 20 | 800 |
| `ice` | 1× #c:ices | 1000 mB minecraft:water | 3 s | 30 | 1,800 |
| `water_ice` | 1× #c:ices/water | 1000 mB minecraft:water | 3 s | 30 | 1,800 |

### Pres (`pressing`)

| Tarif | Girdi | Çıktı | Süre | FE/t | FE/işlem |
|---|---|---|---|---|---|
| `aluminum_lithium_plate` | 1× #c:ingots/aluminum_lithium | 1× aluminum_lithium_plate | 3 s | 20 | 1,200 |
| `aluminum_plate` | 1× #c:ingots/aluminum | 1× aluminum_plate | 3 s | 20 | 1,200 |
| `aluminum_wire` | 1× #c:ingots/aluminum | 2× aluminum_wire | 3 s | 20 | 1,200 |
| `copper_plate` | 1× #c:ingots/copper | 1× copper_plate | 3 s | 20 | 1,200 |
| `copper_wire` | 1× #c:ingots/copper | 2× copper_wire | 3 s | 20 | 1,200 |
| `depleted_uranium_plate` | 1× depleted_uranium | 1× depleted_uranium_plate | 3 s | 30 | 1,800 |
| `gold_plate` | 1× #c:ingots/gold | 1× gold_plate | 3 s | 20 | 1,200 |
| `gold_wire` | 1× #c:ingots/gold | 2× gold_wire | 3 s | 20 | 1,200 |
| `iron_plate` | 1× #c:ingots/iron | 1× iron_plate | 3 s | 20 | 1,200 |
| `iron_rod` | 1× #c:ingots/iron | 1× iron_rod | 3 s | 20 | 1,200 |
| `lithium_plate` | 1× #c:ingots/lithium | 1× lithium_plate | 3 s | 20 | 1,200 |
| `nickel_plate` | 1× #c:ingots/nickel | 1× nickel_plate | 3 s | 20 | 1,200 |
| `nickel_superalloy_plate` | 1× #c:ingots/nickel_superalloy | 1× nickel_superalloy_plate | 3 s | 20 | 1,200 |
| `silicon_wafer` | 1× #c:silicon | 2× silicon_wafer | 5 s | 30 | 3,000 |
| `steel_plate` | 1× #c:ingots/steel | 1× steel_plate | 3 s | 20 | 1,200 |
| `steel_rod` | 1× #c:ingots/steel | 1× steel_rod | 3 s | 20 | 1,200 |
| `titanium_plate` | 1× #c:ingots/titanium | 1× titanium_plate | 3 s | 20 | 1,200 |
| `titanium_rod` | 1× #c:ingots/titanium | 1× titanium_rod | 3 s | 20 | 1,200 |
| `tungsten_plate` | 1× #c:ingots/tungsten | 1× tungsten_plate | 3 s | 20 | 1,200 |
| `tungsten_rod` | 1× #c:ingots/tungsten | 1× tungsten_rod | 3 s | 20 | 1,200 |
| `tungsten_wire` | 1× #c:ingots/tungsten | 2× tungsten_wire | 3 s | 20 | 1,200 |

### Rafineri (`refining`)

| Tarif | Girdi | Çıktı | Süre | FE/t | FE/işlem |
|---|---|---|---|---|---|
| `crude_oil` | 1000 mB c:crude_oil | 1× plastic_sheet + 500 mB rp1 + 250 mB methane | 5 s | 60 | 6,000 |

### Regolit ısıtıcı (`regolith_processing`)

| Tarif | Girdi | Çıktı | Süre | FE/t | FE/işlem |
|---|---|---|---|---|---|
| `carbonaceous_water` | 1× #c:regoliths/carbonaceous | 200 mB minecraft:water + 50 mB carbon_dioxide | 8 s | 100 | 16,000 |
| `comet_dust` | 1× comet_dust | 300 mB minecraft:water + 100 mB carbon_dioxide | 8 s | 100 | 16,000 |
| `encke_dust` | 1× encke_dust | 300 mB minecraft:water + 100 mB carbon_dioxide | 8 s | 100 | 16,000 |
| `halley_dust` | 1× halley_dust | 300 mB minecraft:water + 100 mB carbon_dioxide | 8 s | 100 | 16,000 |
| `perchlorate` | 1× #c:perchlorate_soils | 500 mB oxygen | 5 s | 80 | 8,000 |
| `perchlorate_salt` | 1× #c:dusts/perchlorate | 750 mB oxygen | 4 s | 80 | 6,400 |
| `regolith` | 1× #c:regoliths | 250 mB oxygen | 8 s | 120 | 19,200 |
| `solar_wind_volatiles` | 1× #c:regoliths/solar_wind | 250 mB oxygen + 20 mB helium_3 | 8 s | 120 | 19,200 |
| `tempel_dust` | 1× tempel_dust | 300 mB minecraft:water + 100 mB carbon_dioxide | 8 s | 100 | 16,000 |

### Sabatier (`sabatier`)

| Tarif | Girdi | Çıktı | Süre | FE/t | FE/işlem |
|---|---|---|---|---|---|
| `methane` | 500 mB c:carbon_dioxide | 500 mB methane + 250 mB minecraft:water | 4 s | 60 | 4,800 |

### Sinter fırını (`sintering`)

| Tarif | Girdi | Çıktı | Süre | FE/t | FE/işlem |
|---|---|---|---|---|---|
| `regolith` | 1× #c:regoliths | 1× sintered_regolith | 5 s | 60 | 6,000 |
| `rtg_fuel_pellet` | 4× yellowcake | 1× rtg_fuel_pellet | 10 s | 80 | 16,000 |
| `solar_wind_regolith` | 1× #c:regoliths/solar_wind | 1× sintered_regolith | 5 s | 60 | 6,000 |

## 3. İlk rover, ilk roket ve fırlatma alanı

Ağaç ham kaynaklara kadar açıldı. Mümkünse makinesiz yol seçildi: elle levha 2 külçe → 1 levha, tel = levha + makas.
Pres varsa levha 1:1, tel 1 külçe → 2 tel, yani metal yarıya iner.

| Hedef | Ham kaynak (yaklaşık) | Makine süresi | Enerji |
|---|---|---|---|
| Rover (T1) + batarya bankası | 50 ham demir, 26 ham bakır, 6 boksit, 4 spodümen, 1 ham altın, 1 kuvars, 8 kızıltaş, 1000 mB ham petrol (plastik) | ~51 s (ark, imalatçı, pres, rafineri) | ~67 k FE |
| İlk yörünge roketi parçaları (kerosen motor, 2 büyük tank, ısı kalkanı, kapsül) | 64 boksit, 18 ham demir, 18 ham bakır, 2 kömür bloğu, obsidyen, tuğla, cam | 0 (hepsi tezgâhta) | 0 |
| Fırlatma alanı (rampa, kule, kol, göbek bağı, alev kanalı, konsol) | 33 ham demir, 8 boksit, 16 gri beton, 16 demir parmaklık, boya/yün | 0 | 0 |
| İlk roketin yakıtı: 2 × 7800 kg kerolox (O/F 2,56) | 5410 mB RP-1 ← 10 820 mB ham petrol (2 petrol bloğu); 9832 mB LOX ← 19 664 mB O₂ ← 19 660 mB su | pumpjack 87 s, rafineri 55 s, elektrolizör 79 s, sıvılaştırıcı 60 s | ~430 k FE (pumpjack 52 k, rafineri 66 k, elektroliz 189 k, sıvılaştırma 118 k) |
| Temel makine seti (jeneratör, kırıcı, ark ocağı, pres, elektrolizör, sıvılaştırıcı, pumpjack, rafineri, imalatçı) | 172 ham demir, 70 ham bakır, 12 boksit, 38 kömür, 3 kazan, 2 örs | ~393 s (çoğu ark ocağında çelik) | ~421 k FE (368 k'sı çelik) |

Kabaca oyun süresi (Dengeli, tahmindir, ölçüm değildir):
- **İlk rover:** araştırma adımı 8 civarı. Madencilik ve kurulumla birlikte ~1,5–2,5 saat.
- **İlk yörünge uçuşu:** QA ilerleme modelinde adım 10. Toplam enerji ihtiyacı, makine seti ve yakıt birlikte ~850 k FE. Bu, tek yanmalı jeneratörde ~21 dakika ve ~14 kömür demek. Madencilik dahil ~3–5 saat.

## 4. İşaretler ve öneriler

| # | Bulgu | Tür | Öneri |
|---|---|---|---|
| 1 | **Ablatif kalkan levhası** imalatçıda yapılıyor ve araştırmayla açılıyor, ama hiçbir tarif onu kullanmıyor. | kullanılmayan | Isı kalkanı tarifine girsin (roket ekibi); ya da tarif ve araştırmadaki yerinden çıkarılsın. |
| 2 | **Lityum levha** yapılabiliyor (pres ve elle), ama hiçbir tarifte geçmiyor. | kullanılmayan | Tarifler kaldırılsın (lityum yalnız Al-Li için); ya da bir pil/kabin parçasına girsin. |
| 3 | **Fisyon ünitesi** bir elemandan 76,8 M FE veriyor. Elemanın maliyeti ~300 k FE (2 santrifüj işlemi 256 k + ark 48 k) ve malzeme. Oran ~250×. | aşırı güçlü | Yanma 10 → 5 oyun günü, ya da çıkış 320 → 240 FE/t. Geç oyun ödülü olarak bırakılacaksa en azından bilinçli olsun. |
| 4 | **Regolit ısıtıcı:** 250 mB O₂ için 19,2 k FE. Elektroliz aynı oksijeni ~8 kat ucuza veriyor (1000 mB O₂ = 9,6 k FE, artı H₂). | gereksiz pahalı | Ay'da başka su yoksa tek yol bu; enerji 120 → 80 FE/t ya da süre 160 → 120 tick. |
| 5 | **Pumpjack** 2 çelik blok istiyor (18 çelik). Temel setin çelik yükü 37 ark işlemi = 5 dakika ve 368 k FE, ilk roketten önceki en büyük duvar bu. | erken aşamada uzun | Pumpjack tarifinde çelik blok yerine çelik levha (2–4 adet). |
| 6 | **Elle levha** 2:1, pres 1:1. Elle tel ise levha + makasla 4 kat pahalı. | bilinçli | Pres teşviki doğru. Tel için elle yol 1 levha → 3 tel olabilir (ilk saatlerde kablo çok gerekiyor). |
| 7 | **Yanmalı jeneratör = RTG = 40 FE/t.** RTG'nin üstünlüğü yalnız yakıtsızlık ve her yerde çalışması. | ayrım zayıf | RTG taze pelette 60 FE/t (bozunmayla düşer) ya da jeneratör 32 FE/t. |
| 8 | **Kumdan silikon** 10 s'de 1 silikon, kuvarstan 8 s'de 2 silikon. Kum yolu ~2,5× yavaş. | bilinçli | Kum bol olduğu için kalabilir. |
| 9 | **Santrifüj** işlem başına 128 k FE ile en pahalı işlem; zincirleme bağlanınca hızlanıyor. | bilinçli | Değişiklik gerekmez (madde 3 ile birlikte düşünülmeli). |
| 10 | **Elektroliz** her işlemde 1000 mB H₂ yan ürün veriyor; erken oyunda kullanımı yok (sıvılaştırma adım 9). | yan ürün | Hidrojen bir tanka yönlensin ya da havalandırılsın; ilk saatlerde tankları doldurup makineyi tıkamamalı (QA'da kontrol edilmeli). |

Yanlış alarm olmayanlar: kırıcı ve ark ocağı ile cevher ikiye katlama, pres, imalatçı, Sabatier, buz eritici ve
sıvılaştırıcı süreleri birbirleriyle orantılı. Erken makinelerin işlemi 3–8 s, ileri makinelerinki 8–40 s sürüyor.

## 5. Kararlar ve yapılanlar (2026-11-08)

| # | Karar | Durum |
|---|---|---|
| 1 | Ablatif kalkan levhası ısı kalkanı tarifine girecek | roketçilik ekibinde (entegratör iletti), sanayi tarafında değişiklik yok |
| 2 | Lityum levha batarya bankası tarifinde (2 lityum külçe yerine 2 lityum levha) | **yapıldı** 84a31ad0 |
| 3 | Fisyon 320 → 240 FE/t, yanma süresi aynı | **yapıldı** 3ce94bd2 |
| 4 | Regolit ısıtıcı 120 → 80 FE/t (regolit ve güneş rüzgârı uçucuları) | **yapıldı** 2fef59a5 |
| 5 | Pumpjack: 2 çelik blok yerine 2 çelik levha (16 çelik külçe daha az) | **yapıldı** 8d36a92c |
| 6 | Elle bakır tel: 1 levha → 3 tel | **yapıldı** f8d533fc |
| 7 | RTG 40 FE/t kalıyor (sabit ve bakımsız olması avantajı yeterli) | değişiklik yok |
| 10 | Elektroliz H₂ yan ürünü: hidrojen tankı doluyken makine su harcamadan duruyor, oksijen üretmiyor, "çıkış dolu" kırmızı işaretini gösteriyor, hidrojen çekilince devam ediyor | **doğrulandı** (GameTest `electrolyzerStopsWhenHydrogenHasNowhereToGo`, eefd8b8b); tıkanma yok, kod değişikliği gerekmedi |
