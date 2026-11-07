---
title: Uzay İstasyonu
icon: spacebreaker:docking_port
items: [spacebreaker:station_kit, spacebreaker:habitat_kit, spacebreaker:station_hull, spacebreaker:station_window, spacebreaker:docking_port, spacebreaker:station_bunk, spacebreaker:fuel_depot, spacebreaker:solar_array, spacebreaker:crew_capsule_large, spacebreaker:cargo_module, spacebreaker:station_core_module, spacebreaker:docking_module, spacebreaker:station_hatch]
research: spacebreaker:space_stations
---
Yörüngede bir ev, atölye ve yakıt deposu. Roketini her seferinde yerden doldurmak zorunda kalmazsın.

{items spacebreaker:station_kit spacebreaker:station_hull spacebreaker:station_window spacebreaker:docking_port}

## Kurulum
1. Yörüngeye bir {item spacebreaker:station_kit} götür ve boşlukta kullan: **5×5** bir gövde zemini açılır.
2. {item spacebreaker:station_hull} ve {item spacebreaker:station_window} ile duvarları ve tavanı ör.
3. İçine bir {item spacebreaker:oxygen_distributor} koy: kapalı oda basınçlanır, kask çıkarabilirsin. → [Üssün Havası](base_air)
4. Dışarıya bir {item spacebreaker:docking_port} tak.

## İstasyon modülleri {#modules}
{items spacebreaker:station_core_module spacebreaker:docking_module}
Hazır modüller standart **ağızlardan** birleşir: yürüme yüksekliğinde 3 × 3'lük bir açıklık; bir şey bağlı değilken dokuz {item spacebreaker:station_hatch} ile kapalıdır (ortadaki işaretlidir).
- **{item spacebreaker:station_core_module}** (7 × 7 × 7): istasyonun kalbi; {item spacebreaker:station_core}, oksijen dağıtıcı, tavan ışıkları, çatıda bir kenetlenme portu ve üstünde birer pencere olan dört yan ağız.
- **{item spacebreaker:docking_module}** (5 × 5 × 7): ucunda kenetlenme portu olan kısa bir tünel.

**Elle:** modülü bir ağzın kapağına kullan. Duvara dayanarak kurulur ve iki duvar kısa bir geçide açılır; iki modül tek bir hava odası olur. Yörüngede boşlukta kullanılan bir çekirdek yeni bir istasyon kurar (başka yerde "İstasyon çekirdeği yalnızca yörüngede açılabilir" der). Elinde modül varken bir ağza bakarsan nereye geleceğinin taslağı görünür: yer varsa yeşil, yolda bir şey varsa kırmızı.

**Roketle:** modüller kapsül yuvasına takılır. Modül taşıyan roket yörüngesindeki istasyona kenetlenir ve modülü çekirdek konsolunda bekletir; almak için konsolu aç. 512 blok içinde istasyon yoksa çekirdek modülü roketin önünde yeni bir istasyon açar, roket de onun çatısına kenetlenir.
{recipe spacebreaker:rocketry/station_core_module}
{recipe spacebreaker:rocketry/docking_module}

## Yaşam modülü kiti
{items spacebreaker:habitat_kit}
Elle duvar örmekle uğraşmak istemiyorsan yörüngede bir {item spacebreaker:habitat_kit} kullan. Önünde **7×5×7** sızdırmaz bir oda açılır:
- pencere şeridi olan gövde duvarları ve sana bakan bir hava kilidi kapısı,
- karşı duvarda bir {item spacebreaker:docking_port},
- ortada bir {item spacebreaker:oxygen_distributor} ve bir köşede bir {item spacebreaker:station_bunk},
- çatıda bir güneş paneli dizisi, altında tavana gömülü bir {item spacebreaker:battery_bank} ve tavandan odaya sarkan bir kablo ucu: odadaki makineleri buraya bağla.

> Dağıtıcıya bir oksijen tüpü tak ya da oksijen borula: oda havayla dolar. Önündeki alan boş değilse kit açılmaz.

{page}
## Ranza
{items spacebreaker:station_bunk}
Dünya dışında yatak çalışmaz. {item spacebreaker:station_bunk} kullanınca **yeniden doğma noktan** ranzanın üstü olur; istasyonda, Ay'da ya da herhangi bir dünyada.

## Yakıt deposu
{items spacebreaker:fuel_depot}
{item spacebreaker:fuel_depot} yakıtı ve sıvı oksijeni saklar ve **8 blok** içindeki yanaşma portlarına bağlı roketlere pompalar. Pompa çalışırken **40 FE/t** harcar; güneş panellerinden kabloyla besle.

> Yakıtı istasyona tankla taşı: tanklar kırılınca içeriğini korur. → [Kriyojenik](cryogenics)

## Güneş paneli dizisi
{items spacebreaker:solar_array}
{item spacebreaker:solar_array} tek blokluk tabanda üç panel taşır ve en fazla **60 FE/t** üretir. Yörüngede de gece olur: her tur yaklaşık **12000 tick** sürer ve bunun üçte birine yakını gezegenin gölgesinde geçer. Gölgede paneller ve dizi **hiç üretmez**: gece için bir {item spacebreaker:battery_bank} kur. → [Gece ve enerji](power#night)

## Yanaşma
- İstasyonu olan bir yörüngeye varan mürettebatlı roket en yakın yanaşma portunun **36 blok** uzağında belirir. Hızla yaklaşır, son metreleri manevra iticileriyle ağır ağır alır ve bir tık sesiyle kilitlenir: **"Yanaşıldı"**. Yaklaşma sırasında kimse inemez.
- Portun **12 blok** yakınındaki yörüngedeki bir roketten mürettebat istasyona iner ve geri biner.
- Porta borulanan yakıt ve sıvı oksijen yanaşan rokete akar: istasyon bir **yakıt deposu** olur.

## Kargo roketi
{items spacebreaker:cargo_module}
Malzemeyi kendin taşımak zorunda değilsin. Montaj tezgâhında kapsül yerine bir {item spacebreaker:cargo_module} tak: **27 yuvalı** bir ambar olur ve roket insansız uçar.
- **Yörünge hedefi:** istasyona yanaşır. **Yüzey hedefi:** en yakın {item spacebreaker:landing_beacon}'ne iner.
- Varınca ambarı portun ya da işaretin **4 blok** yakınındaki **sandıklara ve shulker kutularına** boşaltır, en yakından başlayarak. Makinelere hiçbir şey koymaz.
- **Variller giden kutusudur:** oraya koyduğun eşyalar bir sonraki seferde yüklenir.
{recipe spacebreaker:rocketry/cargo_module}

### Otomatik sefer
Dünya yüzeyi dışındaki bir hedefe fırlatılan kargo roketi, kalktığı yerle hedef arasında kendi kendine gidip gelir (örneğin Ay üssü ↔ Ay yörüngesindeki istasyon). Her uçta: sandıklara boşaltır → varillerden yükler → **200 tick** bekler (yakıt deposu onu doldurur) → öbür uca uçar. Ambar boşsa ya da yakıt yetmezse bekler ve sana bir kez haber verir.

> Fırlatma konsolunun son satırı seferin durumunu gösterir: sonraki durak, ambarın doluluğu ve roketin yüklediğini, yük ya da yakıt beklediğini veya yolda olduğunu.

> Seferi açıp kapatmak için rokete {key key.sneak} basılıyken boş elle {key key.use} yap.

> Sığmayan eşyalar ambarda kalır; sohbete kaç eşyanın beklediği yazılır. Portun ya da işaretin yanına yeterince sandık koy.

## Büyük kapsül
{items spacebreaker:crew_capsule_large}
{item spacebreaker:crew_capsule_large} **üç kişi** taşır; {tech spacebreaker:heavy_launch} ile açılır.
