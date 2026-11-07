---
title: Yerinde Üretim (ISRU)
icon: spacebreaker:sabatier_reactor
items: [spacebreaker:atmosphere_processor, spacebreaker:sabatier_reactor, spacebreaker:regolith_heater, spacebreaker:ice_melter, spacebreaker:chemical_reactor, spacebreaker:regolith_excavator, spacebreaker:sintering_furnace, spacebreaker:sintered_regolith, spacebreaker:sintered_regolith_bricks, spacebreaker:item_duct]
research: spacebreaker:isru
---
Dünya'dan fırlatılan her kilogram pahalıdır. Bu yüzden indiğin yerde yapabildiğini **orada** yap. Buna yerinde kaynak kullanımı (ISRU) denir.

## Zincir
| Girdi → çıktı | Makine |
|---|---|
| Buz → su | {item spacebreaker:ice_melter} |
| Kuru buz → CO₂ | {item spacebreaker:ice_melter} |
| Su → O₂ + H₂ | {item spacebreaker:electrolyzer} |
| Yerel hava → CO₂ (Mars) | {item spacebreaker:atmosphere_processor} |
| CO₂ + H₂ → metan + su | {item spacebreaker:sabatier_reactor} |
| Regolit → O₂ | {item spacebreaker:regolith_heater} |

{page}
## Buz eritici
Buzu temiz suya, kuru buzu karbondioksite çevirir.
{recipe spacebreaker:melting/ice}

## Atmosfer işlemcisi
Bulunduğun yerin havasından en yararlı gazı ayırır: Mars'ta **karbondioksit**. Üstünde **açık hava** olmalı; kapalı bir odada çalışmaz.

## Regolit ısıtıcı
Kayayı eritip elektrikle ayrıştırır: **kayadan oksijen**. Bir regolit **250 mB oksijen** verir. Güneş rüzgârı yemiş Ay ve Merkür regoliti bunun yanında **20 mB helyum-3** de verir. Hidrojeni buzdan alırsın.
{recipe spacebreaker:regolith_processing/regolith}
{recipe spacebreaker:regolith_processing/solar_wind_volatiles}

## Regolit Kazıcı
{items spacebreaker:regolith_excavator}
{item spacebreaker:regolith_excavator} altındaki **5×5** alanı kat kat, **16 blok** derine kadar kazar ve kazdığını yanındaki makinelere ya da sandıklara verir. **30 FE/t** harcar.
{recipe spacebreaker:regolith_excavator}

> **Ay üssünde tam otomatik sıvı oksijen:** kazıcı → regolit ısıtıcı → kriyojenik sıvılaştırıcı → kriyojenik tank. Hepsini borula ve kabla: sen yokken de sıvı oksijen birikir.

{page}
## Sabatier reaktörü
Karbondioksit ile hidrojeni **metan ve suya** çevirir. Su, elektroliz ünitesine geri döner. Metanı [Kriyojenik](cryogenics) sıvılaştırıcıyla **sıvı metana** çevir: sıvı oksijenle birlikte **metaloks motorlarının** yakıtıdır. Roketi yakıt hattıyla, boruyla ya da kova / kriyojenik kapla sağ tıklayarak doldur. → [Roket Parçaları](rocket_parts)
{recipe spacebreaker:sabatier/methane}

> [!TIP]
> **Mars'ta oksijen ve metan:** buz eritici → su → elektroliz ünitesi → oksijen + hidrojen; oksijen → kriyojenik sıvılaştırıcı → sıvı oksijen. Hidrojen ve atmosfer işlemcisinden gelen CO₂ Sabatier reaktörüne gider. Çıkan su yine elektroliz ünitesine gider.

## Kimyasal reaktör
Bir katı ve iki sıvıyı birleştirir; örneğin kükürt, su ve oksijenden **sülfürik asit** yapar.
{recipe spacebreaker:chemical_reaction/sulfuric_acid}

{page}
## Sinterleme Fırını
{items spacebreaker:sintering_furnace spacebreaker:sintered_regolith spacebreaker:sintered_regolith_bricks}
{item spacebreaker:sintering_furnace} her gezegenin regolitini elektrikle ısıtıp {item spacebreaker:sintered_regolith}'e kaynaştırır; hava gerekmez. Taş kesicide tuğlaya, basamağa, merdivene ve duvara çevirebilirsin.
{recipe spacebreaker:sintering/regolith}

> Üssünü yerinde üretilen tuğlayla kur: Dünya'dan blok taşıman gerekmez. **Tam bloklar sızdırmaz**; merdiven, basamak ve duvar gibi tam olmayan bloklardan hava kaçar. → [Üssün Havası](base_air)

> Asteroitlerde de üretim var: karbonlu asteroitler sudan, taşlı asteroitler metalden zengindir. → [Dünya'ya Yakın Asteroitler](near_earth_asteroids#mining)

> Kazıcıyı fırının yanına koy: kazdığı regolit doğrudan fırına girer ve yapı bloğu kendiliğinden birikir.

## Eşya kanalı
{items spacebreaker:item_duct}
{item spacebreaker:item_duct} eşyaları envanterler arasında taşır: makineler, sandıklar, huniler.
- Bir ucuna {key key.sneak} basılıyken **boş elle** {key key.use} yap: o uç bağlı envanterden **çeker**. Çeken her uç saniyede **16 eşya** taşır.
- Elinde bir eşyayla {key key.sneak} + {key key.use}: o uç **yalnızca o eşyayı** geçirir.
- Bir huni ya da makinenin çıktısı kanala doğrudan besleyebilir; eşyalar uçlardaki envanterlere gider. Kanalı besleyen huniye geri bir şey gönderilmez, böylece eşyalar döngüye girmez.
{recipe spacebreaker:item_duct}

> Kazıcıdan fırına, fırından sandığa: kanalla tam otomatik bir üretim hattı kurarsın.
