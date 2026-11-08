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
Dört parçayı birlikte giyince bir tıslama duyarsın ve **"Giysi sızdırmaz · yaşam desteği çalışıyor"** mesajı gelir. Bu mesaj en fazla 30 saniyede bir çıkar. Bir parçayı çıkarmak yalnız havasız yerde duyurulur: **"Giysi açıldı: burada hava yok!"**

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
## Kask {#hud}
Kask göstergesi küçük kalır. Hotbar'ın yanında, ana elinin karşı tarafında tek bir oksijen satırı vardır: tüp simgesi ve bir çubuk. Kalan süre (dd:ss) %30'un altında ya da envanter gibi bir ekran açıkken yanında görünür. Çubuk mavidir; **%30**'un altında turuncu, **%15**'in altında kırmızı olur ve yanıp söner.
- **Tehlike simgeleri** oksijen süresinin yanında yalnız bir sorun varken çıkar: giysi (havasız yerde açık ya da %20'nin altında), sıcaklık (giysinin dayanımının ötesi ya da vücudun 35–38,5 °C dışında; 32–41 °C dışında kırmızı), radyasyon (günde 50 mSv'nin üstü ya da toplam 250 mSv; 1000'den sonra kırmızı), basınç (giysi zorlanıyor) ve manevra gazı (%10'un altı).
- **Eve dönüş** (ok, hedef ve uzaklık) yalnız oksijenin, çevre sensörüyle seçtiğin hedefe yürüyerek dönmeye yetmeyecekse görünür. → [Çevre sensörü](tools)
- Hotbar'ın üstünde **tek bir uyarı** yanıp söner: sarı **DÖN: DÖNÜŞ İÇİN O2 AZALIYOR** ya da kırmızı **GERİ DÖNMEYE O2 YETMİYOR**, **GİYSİ SIZDIRMAZ DEĞİL**, **GİYSİ DAYANIMI AŞILDI** gibi.
- Telsiz altyazıları sol üstte çıkar; o sırada sensör kartı onların altına iner. Ekranın üst ortası boş bırakılır.

Güneş göz kamaştırıyorsa **altın güneş vizörünü** indir: kask elindeyken {key key.sneak} + {key key.use}.

> Bütün göstergeleri mi istiyorsun? İstemci ayarlarında **detailedHud**'u aç: vücut sıcaklığı, radyasyon, basınç, yerçekimi ve dış sıcaklık, MAG / HALAT / SOĞUK TUZAK / µG etiketleri, manevra gazı ve her zaman görünen eve dönüş. **visorRim** ekranın çevresine soğukta buzlanan ince bir vizör kenarı ekler (varsayılan kapalı; **visorRimOpacity**). **opacity** bütün göstergeyi saydamlaştırır, **enabled** kapatır.

{page}
## Kask lambaları {#lamps}
Kask lambaları karanlıkta kendiliğinden yanar ve baktığın yeri aydınlatır. {key key.spacebreaker.helmet_lamp} ile yakındaki işler için **geniş** ışık (yaklaşık 20 blok) ve lav tünelleriyle gölgeli kraterler için **uzun** ışık (yaklaşık 40 blok) arasında geçiş yaparsın. Yalnız ışık değişir: fazladan enerji ya da oksijen harcamaz. Herkes aynı ışığı görür.

## Ses {#sound}
İstemcideki **gerçekçi ses** ayarı (atmosphereSound, varsayılan olarak **kapalı**) açıksa, havanın ince olduğu yerlerde dünya biraz daha kısık ve yumuşak duyulur: Mars'ta ses en az %80, vakumda (Ay, yörünge, asteroitler, kuyruklu yıldızlar) yaklaşık yarı yarıya, önce de tizler kaybolur. Kendi adımların ve aletin, kask içi sesler (telsiz, görev kontrol, uyarı bipleri), solunabilir hava ve basınçlı odalar normaldir. Ayar kapalıyken her şey her zamanki gibi duyulur.

## Ölünce
Tüpler gövdenin içinde kalır ve gövde sıradan bir zırh gibi düşer: geri aldığında tüpler de geri gelir. Nefes, vücut ısısı ve radyasyon dozu sıfırlanır.

## Giysi Bakım İstasyonu
{items spacebreaker:suit_station}
Giysiyle bir {item spacebreaker:suit_station}'nun üstünde ya da yanında dur: tüplerini ve Manevra Ünitesini doldurur, giysi parçalarındaki aşınmayı yavaşça onarır. Yuvasına konan bir parçayı da onarır; {item spacebreaker:electric_drill}'ı da şarj eder. → [Aletler](tools) Havası solunabilir bir dünyada havayı, değilse borulanan oksijeni kullanır; enerjiyle (FE) çalışır.
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
