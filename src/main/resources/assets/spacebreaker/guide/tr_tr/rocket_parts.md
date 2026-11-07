---
title: Roket Parçaları
icon: spacebreaker:kerosene_engine
items: [spacebreaker:nose_cone, spacebreaker:crew_capsule, spacebreaker:heat_shield, spacebreaker:parachute, spacebreaker:fuel_tank_small, spacebreaker:fuel_tank_medium, spacebreaker:fuel_tank_large, spacebreaker:kerosene_engine, spacebreaker:vacuum_engine, spacebreaker:methalox_engine, spacebreaker:methalox_vacuum_engine, spacebreaker:hydrolox_engine, spacebreaker:hydrolox_vacuum_engine, spacebreaker:nuclear_thermal_engine, spacebreaker:decoupler, spacebreaker:interstage, spacebreaker:fins, spacebreaker:grid_fins, spacebreaker:landing_legs, spacebreaker:rcs_thrusters]
research: spacebreaker:basic_rocketry
---
Bir roket, üst üste dizilmiş **kademelerden** ve en üstteki **yükten** oluşur.

## Yük
{items spacebreaker:crew_capsule spacebreaker:heat_shield spacebreaker:parachute spacebreaker:nose_cone}
- {item spacebreaker:crew_capsule}: mürettebatı taşır.
- {item spacebreaker:heat_shield}: kapsülün **altına** takılır; atmosfere girişte seni korur.
- {item spacebreaker:parachute}: kapsülün **üstüne** takılır; yumuşak iniş için.
- {item spacebreaker:nose_cone}: insansız roketlerde yükün tepesi.

## Kademe
{items spacebreaker:kerosene_engine spacebreaker:vacuum_engine spacebreaker:fuel_tank_small spacebreaker:fuel_tank_medium spacebreaker:fuel_tank_large}
Her kademede altta **bir motor**, üstünde **yakıt tankları** olur. Tanklar, o kademenin motoru ne yakıyorsa onu taşır.
- **Deniz seviyesi motoru** havada iyi çalışır: ilk kademe için.
- **Vakum motoru** uzayda çok daha verimlidir: üst kademeler için.

## Motorlar ve yakıtlar
{items spacebreaker:methalox_engine spacebreaker:methalox_vacuum_engine spacebreaker:hydrolox_engine spacebreaker:hydrolox_vacuum_engine}
**Metaloks motorları** {tech spacebreaker:methane_engines} ile açılır. **Sıvı metan ve sıvı oksijen** yakarlar; yakıtlarını Mars'ta üretebilirsin. Isp'deki iki sayı: deniz seviyesi / vakum.

| Motor | İtki | Isp | Yakıt |
|---|---|---|---|
| Kerosen (deniz) | 560 kN | 282 / 311 s | RP-1 + LOX (1 : 2,56) |
| Kerosen (vakum) | 190 kN | 345 s | RP-1 + LOX (1 : 2,56) |
| Metaloks (deniz) | 600 kN | 330 / 350 s | metan + LOX (1 : 3,6) |
| Metaloks (vakum) | 220 kN | 380 s | metan + LOX (1 : 3,6) |
| Hidroloks (deniz) | 650 kN | 366 / 452 s | hidrojen + LOX (1 : 6) |
| Hidroloks (vakum) | 110 kN | 465 s | hidrojen + LOX (1 : 6) |
| Nükleer termal (vakum) | 150 kN | 880 s | yalnız sıvı hidrojen |

**Hidroloks motorları** {tech spacebreaker:hydrogen_engines} ile açılır. Sıvı hidrojen en verimli yakıttır ama çok **hacimlidir** (yoğunluğu suyun on dörtte biri): aynı kütle için çok daha büyük tank gerekir.

## Nükleer termal motor {#ntr}
{items spacebreaker:nuclear_thermal_engine}
{item spacebreaker:nuclear_thermal_engine} ({tech spacebreaker:nuclear_thermal_propulsion}) oksitleyici yakmaz: bir fisyon reaktörü sıvı hidrojeni ısıtır. Vakumda **880 s** özgül itki, her kimyasal motorun yaklaşık iki katı. Ama 150 kN itki ve 3,4 t kütleyle yerden kalkamayacak kadar zayıftır: üst kademe motorudur. Kademesi hidroloks kademeleri gibi aynı hidrojen hattından yalnız sıvı hidrojen alır.

>! Yanarken bir **radyasyon kaynağıdır**: doz uzaklığın karesiyle azalır, motorun üstündeki gölge kalkanı mürettebatı korur. Soğukken güvenlidir. Yakıtı: → [Nükleer Yakıt](nuclear_fuel)

Montaj tezgâhı her kademenin yakıt türünü ve Δv'sini gösterir.

## Ayırıcılar
{items spacebreaker:decoupler spacebreaker:interstage}
Kademeler arasına bir **kademe ayırıcı** ya da **ara kademe** koy. Yakıtı biten kademe düşer, roket hafifler.

## Yan parçalar
{items spacebreaker:fins spacebreaker:grid_fins spacebreaker:landing_legs spacebreaker:rcs_thrusters}
- **Kanatçıklar**: çıkışta yön kaybını azaltır.
- **Izgara kanatçıklar**: ilk kademeyi yönlendirir.
- **İniş bacakları**: Ay'a inmek için **şart**.
- **Manevra iticileri**: inişte ince kontrol.

{recipe spacebreaker:rocketry/kerosene_engine}
