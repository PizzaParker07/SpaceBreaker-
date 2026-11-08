# EVA Giysisi

*İlgili araştırma: Basınçlı Giysiler*

Uzayda giysin, kişisel uzay aracındır. İlk giysiyi Dünya'dan çıkmadan, sıradan malzemelerle yaparsın.

**EVA Kaskı** · **EVA Gövdesi** · **EVA Pantolonu** · **EVA Botları**

### Tarifler
- **Kask:** 2 alüminyum levha, 1 altın külçe, 2 beyaz yün, 1 cam.
- **Gövde:** 4 beyaz yün, 2 lapis, 1 kızıltaş tozu, 1 alüminyum levha.
- **Pantolon:** 2 alüminyum levha, 1 lapis, 4 beyaz yün.
- **Botlar:** 2 beyaz yün, 2 siyah yün.

|   |   |   |
|---|---|---|
| Alüminyum Levha | Altın Külçesi | Alüminyum Levha |
| Beyaz Yün | Cam | Beyaz Yün |

→ **EVA Kaskı** (Çalışma Masası)

|   |   |   |
|---|---|---|
| Beyaz Yün |   | Beyaz Yün |
| Lapis Lazuli | Redstone Tozu | Lapis Lazuli |
| Beyaz Yün | Alüminyum Levha | Beyaz Yün |

→ **EVA Gövdesi** (Çalışma Masası)

|   |   |   |
|---|---|---|
| Alüminyum Levha | Lapis Lazuli | Alüminyum Levha |
| Beyaz Yün |   | Beyaz Yün |
| Beyaz Yün |   | Beyaz Yün |

→ **EVA Pantolonu** (Çalışma Masası)

|   |   |   |
|---|---|---|
| Beyaz Yün |   | Beyaz Yün |
| Siyah Yün |   | Siyah Yün |

→ **EVA Botları** (Çalışma Masası)

### Sızdırmazlık
Dört parçayı birlikte giyince bir tıslama duyarsın ve **"Giysi sızdırmaz · yaşam desteği çalışıyor"** mesajı gelir. Bu mesaj en fazla 30 saniyede bir çıkar. Bir parçayı çıkarmak yalnız havasız yerde duyurulur: **"Giysi açıldı: burada hava yok!"**

> [!WARNING]
> Bir parça bile eksikse boşlukta nefes alamazsın.

### Sırt çantası ve tüpler
Tüpler **EVA Gövdesi** içindeki sırt çantasında durur: **iki tüp**, Ek Oksijen modülüyle üç. → [Oksijen](oxygen)
- Gövdeyi giymişken tüp elindeyken `Sağ Tık`: tüpü takar.
- Envanterde tüpü gövdenin üzerine sağ tıkla: takar. Boş imleçle sağ tık: çıkarır.

### Uyarılar
- Oksijen **%25**'in altında: çan sesi.
- **%10**'un altında: 3 saniyede bir bip.
- Oksijen bitince ya da havasız yerde giysi açıkken: hızlı alarm. Göstergedeki uyarı yazısı yanıp söner.

<a id="hud"></a>
### Kask
Kask göstergesi küçük kalır. Hotbar'ın yanında, ana elinin karşı tarafında tek bir oksijen satırı vardır: tüp simgesi ve bir çubuk. Kalan süre (dd:ss) %30'un altında ya da envanter gibi bir ekran açıkken yanında görünür. Çubuk mavidir; **%30**'un altında turuncu, **%15**'in altında kırmızı olur ve yanıp söner.
- **Tehlike simgeleri** oksijen süresinin yanında yalnız bir sorun varken çıkar: giysi (havasız yerde açık ya da %20'nin altında), sıcaklık (giysinin dayanımının ötesi ya da vücudun 35–38,5 °C dışında; 32–41 °C dışında kırmızı), radyasyon (günde 50 mSv'nin üstü ya da toplam 250 mSv; 1000'den sonra kırmızı), basınç (giysi zorlanıyor) ve manevra gazı (%10'un altı).
- **Eve dönüş** (ok, hedef ve uzaklık) yalnız oksijenin, çevre sensörüyle seçtiğin hedefe yürüyerek dönmeye yetmeyecekse görünür. → [Çevre sensörü](tools)
- Hotbar'ın üstünde **tek bir uyarı** yanıp söner: sarı **DÖN: DÖNÜŞ İÇİN O2 AZALIYOR** ya da kırmızı **GERİ DÖNMEYE O2 YETMİYOR**, **GİYSİ SIZDIRMAZ DEĞİL**, **GİYSİ DAYANIMI AŞILDI** gibi.
- Telsiz altyazıları sol üstte çıkar; o sırada sensör kartı onların altına iner. Ekranın üst ortası boş bırakılır.

Güneş göz kamaştırıyorsa **altın güneş vizörünü** indir: kask elindeyken `Shift` + `Sağ Tık`.

> [!TIP]
> Bütün göstergeleri mi istiyorsun? İstemci ayarlarında **detailedHud**'u aç: vücut sıcaklığı, radyasyon, basınç, yerçekimi ve dış sıcaklık, MAG / HALAT / SOĞUK TUZAK / µG etiketleri, manevra gazı ve her zaman görünen eve dönüş. **visorRim** ekranın çevresine soğukta buzlanan ince bir vizör kenarı ekler (varsayılan kapalı; **visorRimOpacity**). **opacity** bütün göstergeyi saydamlaştırır, **enabled** kapatır.

<a id="lamps"></a>
### Kask lambaları
Kask lambaları karanlıkta kendiliğinden yanar ve baktığın yeri aydınlatır. `key.spacebreaker.helmet_lamp` ile yakındaki işler için **geniş** ışık (yaklaşık 20 blok) ve lav tünelleriyle gölgeli kraterler için **uzun** ışık (yaklaşık 40 blok) arasında geçiş yaparsın. Yalnız ışık değişir: fazladan enerji ya da oksijen harcamaz. Herkes aynı ışığı görür.

<a id="sound"></a>
### Ses
İstemcideki **gerçekçi ses** ayarı (atmosphereSound, varsayılan olarak **kapalı**) açıksa, havanın ince olduğu yerlerde dünya biraz daha kısık ve yumuşak duyulur: Mars'ta ses en az %80, vakumda (Ay, yörünge, asteroitler, kuyruklu yıldızlar) yaklaşık yarı yarıya, önce de tizler kaybolur. Kendi adımların ve aletin, kask içi sesler (telsiz, görev kontrol, uyarı bipleri), solunabilir hava ve basınçlı odalar normaldir. Ayar kapalıyken her şey her zamanki gibi duyulur.

### Ölünce
Tüpler gövdenin içinde kalır ve gövde sıradan bir zırh gibi düşer: geri aldığında tüpler de geri gelir. Nefes, vücut ısısı ve radyasyon dozu sıfırlanır.

### Giysi Bakım İstasyonu

**Giysi Bakım İstasyonu**

Giysiyle bir **Giysi Bakım İstasyonu**'nun üstünde ya da yanında dur: tüplerini ve Manevra Ünitesini doldurur, giysi parçalarındaki aşınmayı yavaşça onarır. Yuvasına konan bir parçayı da onarır; [Elektrikli Matkap](tools)'ı da şarj eder. → [Aletler](tools) Havası solunabilir bir dünyada havayı, değilse borulanan oksijeni kullanır; enerjiyle (FE) çalışır.

|   |   |   |
|---|---|---|
| Alüminyum Levha | Demir Parmaklık | Alüminyum Levha |
| Örs | Oksijen Dolum İstasyonu | Örs |
| Alüminyum Levha | Redstone Tozu | Alüminyum Levha |

→ **Giysi Bakım İstasyonu** (Çalışma Masası)

### Giysi kademeleri
| Giysi | Sıcaklık | Radyasyon kalkanı |
|---|---|---|
| EVA (Mk1) | −190 / 130 °C | %30 |
| Isıtmalı | −250 / 80 °C | %40 |
| Termal | −190 / 500 °C | %40 |
| Radyasyon | −190 / 130 °C | %96 |

- **Isıtmalı giysi** (*Isıtmalı Giysiler*): soğuk dünyalar, Mars geceleri ve dış uydular.
- **Termal giysi** (*Termal Giysiler*): Merkür'ün gündüzü ve Venüs.
- **Radyasyon giysisi** (*Radyasyon Zırhlı Giysiler*): Jüpiter'in radyasyon kuşakları.

> [!TIP]
> Teknolojisini bilmediğin bir giysiyi giyersen tam korumayı alamazsın; ipucu satırı neyin gerektiğini söyler.

<details>
<summary>English: EVA Suit</summary>

*Related research: Pressure Suits*

In space, your suit is your personal spacecraft. You make the first one before leaving Earth, from ordinary materials.

**EVA Helmet** · **EVA Torso** · **EVA Leggings** · **EVA Boots**

### Recipes
- **Helmet:** 2 aluminium plates, 1 gold ingot, 2 white wool, 1 glass.
- **Body:** 4 white wool, 2 lapis, 1 redstone dust, 1 aluminium plate.
- **Leggings:** 2 aluminium plates, 1 lapis, 4 white wool.
- **Boots:** 2 white wool, 2 black wool.

|   |   |   |
|---|---|---|
| Aluminum Plate | Gold Ingot | Aluminum Plate |
| White Wool | Glass | White Wool |

→ **EVA Helmet** (Crafting Table)

|   |   |   |
|---|---|---|
| White Wool |   | White Wool |
| Lapis Lazuli | Redstone Dust | Lapis Lazuli |
| White Wool | Aluminum Plate | White Wool |

→ **EVA Torso** (Crafting Table)

|   |   |   |
|---|---|---|
| Aluminum Plate | Lapis Lazuli | Aluminum Plate |
| White Wool |   | White Wool |
| White Wool |   | White Wool |

→ **EVA Leggings** (Crafting Table)

|   |   |   |
|---|---|---|
| White Wool |   | White Wool |
| Black Wool |   | Black Wool |

→ **EVA Boots** (Crafting Table)

### Sealing
Put on all four pieces and you hear a hiss and see **"Suit sealed · life support online"**. The note shows at most once every 30 seconds. Taking a piece off is only announced where there is no air: **"Suit unsealed: no air out here!"**

> [!WARNING]
> With even one piece missing you cannot breathe in a vacuum.

### Backpack and tanks
The tanks sit in the backpack inside the **EVA Torso**: **two tanks**, three with the Extra Oxygen module. → [Oxygen](oxygen)
- `Right Click` with a tank in hand while wearing the body: fits it.
- In the inventory, right-click the tank onto the body: fits it. Right-click with an empty cursor: takes it out.

### Warnings
- Oxygen below **25%**: a bell.
- Below **10%**: a beep every 3 seconds.
- Out of oxygen, or the suit open in an airless place: a fast alarm. The warning on the display blinks.

<a id="hud"></a>
### Helmet
The helmet display stays small. Beside the hotbar, on the side away from your main hand, is one oxygen line: a tank icon and a bar. The time left (mm:ss) appears beside it below 30 %, or while a screen such as your inventory is open. The bar is blue, orange below **30 %**, and red and blinking below **15 %**.
- **Hazard icons** appear after the oxygen time only while something is wrong: suit (open or below 20 % in an airless place), temperature (beyond your suit's rating, or your body outside 35–38.5 °C; red outside 32–41 °C), radiation (over 50 mSv/day, or 250 mSv in total; red from 1,000), pressure (suit under strain) and maneuvering gas (below 10 %).
- **The way home** (arrow, target and distance) only shows when your oxygen will not last the walk back to the target you picked with the environment sensor. → [Environment sensor](tools)
- **One warning** blinks above the hotbar, such as **TURN BACK: O2 RUNNING LOW FOR THE WAY HOME** in yellow, or **NOT ENOUGH O2 TO GET BACK**, **SUIT NOT SEALED** and **BEYOND SUIT RATING** in red.
- Radio captions appear at the top left; the sensor card moves down below them while they are shown. The top centre of the screen is kept clear.

If the Sun is blinding, lower the **gold sun visor**: `Shift` + `Right Click` with the helmet in hand.

> [!TIP]
> Want every gauge? Turn on **detailedHud** in the client config: body temperature, radiation, pressure, gravity and outside temperature, the MAG / TETHER / COLD TRAP / µG labels, maneuvering gas, and the way home all the time. **visorRim** adds a thin visor rim round the screen that frosts over in the cold (off by default; **visorRimOpacity**). **opacity** fades the whole display and **enabled** turns it off.

<a id="lamps"></a>
### Helmet lamps
The helmet lamps switch on by themselves in the dark and light where you look. `key.spacebreaker.helmet_lamp` switches between a **wide** beam (about 20 blocks) for working nearby and a **long** beam (about 40 blocks) for lava tubes and shadowed craters. Only the light changes: no extra power or oxygen. Everyone sees the same beam.

<a id="sound"></a>
### Sound
With the **realistic sound** client setting (atmosphereSound, **off** by default) the world sounds a little quieter and softer where the air is thin: on Mars at least 80 % of the volume, in vacuum (the Moon, orbit, asteroids, comets) about half, and the high notes fade first. Your own steps and tools, helmet sounds (radio, mission control, warning beeps), breathable air and pressurised rooms stay normal. With the setting off everything sounds as usual.

### When you die
The tanks stay inside the body piece, which drops like any armour: pick it up and the tanks come back with it. Breath, body temperature and radiation dose reset.

### Suit maintenance station

**Suit Maintenance Station**

Stand on or next to a **Suit Maintenance Station** in your suit: it tops up your tanks and Maneuvering Unit and slowly mends wear on your suit pieces. A piece placed in its slot is mended too, and it charges the [Electric Drill](tools). → [Tools](tools) It uses the planet's breathable air, or piped oxygen where there is none, and runs on FE.

|   |   |   |
|---|---|---|
| Aluminum Plate | Iron Bars | Aluminum Plate |
| Anvil | Oxygen Filler | Anvil |
| Aluminum Plate | Redstone Dust | Aluminum Plate |

→ **Suit Maintenance Station** (Crafting Table)

### Suit tiers
| Suit | Temperature | Radiation shielding |
|---|---|---|
| EVA (Mk1) | −190 / 130 °C | 30% |
| Heated | −250 / 80 °C | 40% |
| Thermal | −190 / 500 °C | 40% |
| Radiation | −190 / 130 °C | 96% |

- **Heated suit** (*Heated Suits*): cold worlds, Martian nights and the outer moons.
- **Thermal suit** (*Thermal Suits*): Mercury's day and Venus.
- **Radiation suit** (*Radiation-Hardened Suits*): Jupiter's radiation belts.

> [!TIP]
> Wear a suit whose technology you do not know and you do not get its full protection; the tooltip tells you what is missing.

</details>
