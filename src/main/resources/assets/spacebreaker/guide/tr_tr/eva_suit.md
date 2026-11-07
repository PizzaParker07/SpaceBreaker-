---
title: EVA Giysisi
icon: spacebreaker:eva_chestplate
items: [spacebreaker:eva_helmet, spacebreaker:eva_chestplate, spacebreaker:eva_leggings, spacebreaker:eva_boots, spacebreaker:heated_helmet, spacebreaker:heated_chestplate, spacebreaker:heated_leggings, spacebreaker:heated_boots, spacebreaker:thermal_helmet, spacebreaker:thermal_chestplate, spacebreaker:thermal_leggings, spacebreaker:thermal_boots, spacebreaker:radiation_helmet, spacebreaker:radiation_chestplate, spacebreaker:radiation_leggings, spacebreaker:radiation_boots, spacebreaker:suit_station]
research: spacebreaker:pressure_suits
---
Uzayda giysin, kişisel uzay aracındır. İlk giysiyi Dünya'dan çıkmadan, sıradan malzemelerle yaparsın.

{items spacebreaker:eva_helmet spacebreaker:eva_chestplate spacebreaker:eva_leggings spacebreaker:eva_boots}

## Tarifler
- **Kask:** 2 alüminyum levha, 1 altın külçe, 2 beyaz yün, 1 cam.
- **Gövde:** 4 beyaz yün, 2 lapis, 1 kızıltaş tozu, 1 alüminyum levha.
- **Pantolon:** 2 alüminyum levha, 1 lapis, 4 beyaz yün.
- **Botlar:** 2 beyaz yün, 2 siyah yün.
{recipe spacebreaker:eva_helmet}
{recipe spacebreaker:eva_chestplate}
{recipe spacebreaker:eva_leggings}
{recipe spacebreaker:eva_boots}

{page}
## Sızdırmazlık
Dört parçayı birlikte giyince bir tıslama duyarsın ve **"Giysi sızdırmaz · yaşam desteği çalışıyor"** mesajı gelir. Bir parçayı çıkarırsan **"Giysi açıldı"** der; havasız bir yerdeysen uyarı kırmızıdır.

>! Bir parça bile eksikse boşlukta nefes alamazsın.

## Sırt çantası ve tüpler
Tüpler {item spacebreaker:eva_chestplate} içindeki sırt çantasında durur: **iki tüp**, Ek Oksijen modülüyle üç. → [Oksijen](oxygen)
- Gövdeyi giymişken tüp elindeyken {key key.use}: tüpü takar.
- Envanterde tüpü gövdenin üzerine sağ tıkla: takar. Boş imleçle sağ tık: çıkarır.

## Uyarılar
- Oksijen **%25**'in altında: çan sesi.
- **%10**'un altında: 3 saniyede bir bip.
- Oksijen bitince ya da havasız yerde giysi açıkken: hızlı alarm. Göstergedeki uyarı yazısı yanıp söner.

{page}
## Kask
Kask göstergesi oksijeni, giysinin durumunu, vücut ve dış sıcaklığı, radyasyonu ve yerçekimini gösterir. Güneş göz kamaştırıyorsa **altın güneş vizörünü** indir: kask elindeyken {key key.sneak} + {key key.use}. Ekranın kenarındaki yuvarlak vizör çerçevesini istemcinin ayarlarından (**visorFrame**, **visorFrameOpacity**) kapatabilir ya da saydamlaştırabilirsin.

## Ölünce
Tüpler gövdenin içinde kalır ve gövde sıradan bir zırh gibi düşer: geri aldığında tüpler de geri gelir. Nefes, vücut ısısı ve radyasyon dozu sıfırlanır.

## Giysi Bakım İstasyonu
{items spacebreaker:suit_station}
Giysiyle bir {item spacebreaker:suit_station}'nun üstünde ya da yanında dur: tüplerini ve Manevra Ünitesini doldurur, giysi parçalarındaki aşınmayı yavaşça onarır. Yuvasına konan bir parçayı da onarır. Havası solunabilir bir dünyada havayı, değilse borulanan oksijeni kullanır; enerjiyle (FE) çalışır.
{recipe spacebreaker:suit_station}

## Giysi kademeleri
| Giysi | Sıcaklık | Radyasyon kalkanı |
|---|---|---|
| EVA (Mk1) | −190 / 130 °C | %30 |
| Isıtmalı | −250 / 80 °C | %40 |
| Termal | −190 / 500 °C | %40 |
| Radyasyon | −190 / 130 °C | %96 |

- **Isıtmalı giysi** ({tech spacebreaker:heated_suits}): soğuk dünyalar, Mars geceleri ve dış uydular.
- **Termal giysi** ({tech spacebreaker:thermal_suits}): Merkür'ün gündüzü ve Venüs.
- **Radyasyon giysisi** ({tech spacebreaker:radiation_hardened_suits}): Jüpiter'in radyasyon kuşakları.

> Teknolojisini bilmediğin bir giysiyi giyersen tam korumayı alamazsın; ipucu satırı neyin gerektiğini söyler.
