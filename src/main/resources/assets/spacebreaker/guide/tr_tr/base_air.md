---
title: Üssün Havası
icon: spacebreaker:oxygen_distributor
items: [spacebreaker:oxygen_distributor, spacebreaker:airlock_door, spacebreaker:aluminum_panel, spacebreaker:titanium_deck, spacebreaker:pressure_glass, spacebreaker:porthole, spacebreaker:hazard_stripe, spacebreaker:cable_duct, spacebreaker:pipe_duct, spacebreaker:grate, spacebreaker:grate_platform, spacebreaker:railing, spacebreaker:steel_ladder, spacebreaker:floodlight, spacebreaker:led_panel, spacebreaker:warning_light, spacebreaker:sliding_door]
research: spacebreaker:oxygen_distribution
---
Kask takmadan dolaşabileceğin bir üs için kapalı bir oda, bir dağıtıcı ve bir hava kilidi gerekir.

{structure spacebreaker:pressurised_habitat}

{items spacebreaker:oxygen_distributor spacebreaker:airlock_door}

1. Dört bir yanı kapalı bir oda yap (en çok **4096 blok** hacim).
2. İçine bir {item spacebreaker:oxygen_distributor} koy. Ona dolu bir tüp tak ya da **boruyla oksijen** ver.
3. Dağıtıcı odayı tarar. Odada sızıntı varsa **nerede olduğunu** söyler.
4. Kapı yerine {item spacebreaker:airlock_door} kullan: kapalıyken hava geçirmez, elle ya da kızıltaşla açılır.

{recipe spacebreaker:oxygen_distributor}
{recipe spacebreaker:airlock_door}

## Ne kadar oksijen?
- **Basınçlandırma:** önce odayı doldurur, blok başına **4 nefes tiki**. Durum mesajı yüzdeyi gösterir.
- **Sürekli tüketim:** her tikte 1, artı her **40 blok** için 1 nefes tiki.
- **Sızıntı:** hava boşalır, oda yeniden basınçlandırılmalıdır.

Örnek: 100 bloklu bir oda önce 400 nefes tiki (200 mB oksijen gazı) ister, sonra tik başına 3,5 nefes tiki harcar.

> [!TIP]
> Bir blok kırıldığında ya da konduğunda dağıtıcı odayı yeniden kontrol eder. Pencere için cam kullanabilirsin; kapı ise sıradan bir kapı olursa hava kaçar.

{page}
## Üs blokları
{items spacebreaker:aluminum_panel spacebreaker:titanium_deck spacebreaker:pressure_glass spacebreaker:porthole spacebreaker:hazard_stripe spacebreaker:cable_duct}
Üssünü {item spacebreaker:aluminum_panel}, {item spacebreaker:titanium_deck}, {item spacebreaker:pressure_glass} ve {item spacebreaker:porthole} ile kur. **Tam bloklar, basınçlı cam ve lombarlar havayı tutar**; uyarı şeritleri ve kanal kapakları da tam bloktur.

>! {item spacebreaker:grate}, ızgara platform, korkuluk, çelik merdiven, projektör ve basamaklar havayı geçirir. Bunları basınçlı odanın duvarına koyma.

{items spacebreaker:led_panel spacebreaker:floodlight spacebreaker:warning_light}
Aydınlatma için {item spacebreaker:led_panel} ve {item spacebreaker:floodlight}; dönen ışıklı bir {item spacebreaker:warning_light} tehlikeli yerleri işaretler.

## Kayar kapılar ve hava kilidi
{items spacebreaker:sliding_door}
Karşılıklı, en fazla **4 blok** arayla iki {item spacebreaker:sliding_door} bir **hava kilidi** olur: ikisi asla aynı anda açık kalmaz. Birini açmak istediğinde öbürü kapanır, hava kilidi döngüsü çalar ve kapı 40 tik sonra açılır. Elle ya da kızıltaşla aynı kural geçerlidir.
