---
title: Tuşlar ve Kontroller
icon: minecraft:lever
---
Aşağıdaki tuşlar **senin** ayarlarındaki tuşlardır; değiştirdiğinde burada da değişir.

| Ne yapar | Tuş |
|---|---|
| Araştırma ağacını aç | {key key.spacebreaker.research} |
| Saha rehberini aç | {key key.spacebreaker.guide} |
| [Fırlat](first_flight#launch) / geri sayımı durdur | {key key.spacebreaker.launch} |
| Yıldız haritası (yörüngede) | {key key.spacebreaker.star_map} |
| Manevra ünitesiyle yüksel | {key key.jump} |
| Manevra ünitesiyle alçal | {key key.sneak} |
| Kask lambaları: geniş / uzun huzme (40 blok, lav tüpleri ve soğuk tuzaklar için) | {key key.spacebreaker.helmet_lamp} |

## Roketteyken
| Ne yapar | Tuş |
|---|---|
| Roketin çevresinde dön | fare |
| Yakınlaş / uzaklaş | fare tekerleği |
| Kokpit penceresi | {key key.togglePerspective} |
| Geri sayımı başlat / durdur | {key key.spacebreaker.launch} |
| Yıldız haritası (yörüngede) | {key key.spacebreaker.star_map} |
| Roketten in | {key key.sneak} |
→ [Uçuş ve Yön Bulma](navigation)

## Fare ile
- **Altın güneş vizörü:** {item spacebreaker:eva_helmet} elindeyken {key key.sneak} basılı tut ve {key key.use} tuşuna bas.
- **Tüp takmak:** {item spacebreaker:oxygen_tank} elindeyken, üstünde {item spacebreaker:eva_chestplate} varken {key key.use}.
- **Roketi rampaya koymak:** {item spacebreaker:rocket} ile alev kanalının ortasına {key key.use}.
- **Rokete binmek:** rokete {key key.use}. İnmek için {key key.sneak}.
- **Roketi indirmek:** {key key.sneak} basılı tutup rokete vur.
- **Boruyu çekme moduna almak:** {key key.sneak} basılı tutup borunun ucuna {key key.use}.

## Kitapta
| Ne yapar | Tuş |
|---|---|
| Sayfa çevir | ← → veya fare tekerleği |
| Geri | Geri tuşu (Backspace) |
| İçindekiler | Home |
| Ara | Ctrl+F |

## Ayarlar {#settings}
İstemci ayarları oyunun config klasöründedir:
- **spacebreaker-worlds-client.toml:** **planetStyle** gezegenlerin ve uyduların dışarıdan nasıl göründüğünü seçer: **VOXEL** (bloklardan bir top, varsayılan), **CUBE** (yavaşça dönen pikselli bir küp) ya da **ROUND** (eski düz disk). **bodyScale** Güneş'in ve gezegenlerin ne kadar büyük göründüğünü ayarlar (1 = gerçek boyut; varsayılan 8). Bu görünüm gökyüzünde, yörüngede altındaki dünyada ve yıldız haritasında kullanılır.
- **spacebreaker-hud.toml:** kask göstergesi, vizör kenarı ve ses. → [Kask](eva_suit#hud)
