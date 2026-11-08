---
title: Enerji
icon: spacebreaker:combustion_generator
items: [spacebreaker:combustion_generator, spacebreaker:solar_panel, spacebreaker:battery_bank, spacebreaker:copper_cable, spacebreaker:hv_cable, spacebreaker:rtg, spacebreaker:rtg_fuel_pellet, spacebreaker:power_monitor]
---
Bütün makineler **FE** (enerji) ile çalışır. Pelet sinterleme fırınında sarı pastadan yapılır. → [Nükleer Yakıt](nuclear_fuel) Üretim ve tüketim "FE/t" ile, yani her oyun tikinde (saniyede 20 tik) ölçülür.

{items spacebreaker:combustion_generator spacebreaker:solar_panel spacebreaker:battery_bank spacebreaker:copper_cable spacebreaker:hv_cable}

| Blok | Ne yapar? |
|---|---|
| {item spacebreaker:combustion_generator} | Yakıt yakar, **40 FE/t** verir. Yalnızca solunabilir havada çalışır. |
| {item spacebreaker:solar_panel} | Güneşi takip eder; açık öğlede **20 FE/t**. Yağmurda ve gece azalır. |
| {item spacebreaker:battery_bank} | **1 000 000 FE** saklar, önünden 2048 FE/t verir. Bir bloğun altına (tavana) takılırsa aşağı doğru verir. |
| {item spacebreaker:copper_cable} | Enerjiyi taşır; az güç için. |
| {item spacebreaker:hv_cable} | Çok güç taşır. |

{recipe spacebreaker:combustion_generator}

> [!WARNING]
> Ay'da ve diğer havasız dünyalarda yanmalı jeneratör **çalışmaz**: yanacak oksijen yok. Orada güneş panelleri ve bataryalar kullan.

> [!TIP]
> Güneş paneli enerjiyi üst yüzü hariç her yöne verir. Laboratuvarın hemen yanına koymak yeter.

{page}
> Mars'ta paneller tozlanır ve gücünün %60'ına kadar düşer; eğilip kullanarak sil. → [Mars](mars)

## Güç göstergesi
{items spacebreaker:power_monitor}
Bir {item spacebreaker:power_monitor}'ü batarya bankasına (ya da enerji depolayan herhangi bir bloğa) daya: ekranında doluluk, net akış ve dolma ya da boşalma süresi görünür. Karşılaştırıcı çıkışı doluluğu izler; düşük şarjda bir jeneratörü kızıltaşla açabilirsin.

## Gece ve enerji {#night}
10 FE/t'lik sürekli bir yük için (panel öğlede en çok 20 FE/t, akü bankası 1 000 000 FE):

| Dünya | Gece | Güneş | Panel | Akü bankası |
|---|---|---|---|---|
| Merkür | 115 dk | %667 (en çok 3×) | 0,4 | 1,4 |
| Ay | 47 dk | %100 | 1,1 | 0,6 |
| Mars | 9 dk | %43 | 2,5 | 0,1 |
| Ceres | 5 dk | %13 | 8 | 0,06 |
| Europa | 16 dk | %3,7 | 29 | 0,2 |
| Ganymede | 23 dk | %3,7 | 29 | 0,3 |
| Kallisto | 35 dk | %3,7 | 29 | 0,4 |
| Titan | 35 dk | %1,1 | 100 | 0,4 |
| Plüton | 22 dk | %0,06 | ~1000 | 0,3 |

>! Europa ve ötesinde güneş paneli pratik değildir: Güneş çok uzaktır. Oralarda bir {item spacebreaker:rtg} kullan ({tech spacebreaker:radioisotope_power}); tek bir RTG dört tane 10 FE/t'lik yükü taşır ve {item spacebreaker:rtg_fuel_pellet} ile çalışır. Pelet, sinterleme fırınında sarı pastadan yapılır. → [Nükleer Yakıt](nuclear_fuel)

> Büyük bir üs için {item spacebreaker:fission_reactor} tek yakıt elemanıyla 10 gün boyunca 240 FE/t verir, ama gömülmeli ya da kalkanlanmalıdır. → [Fisyon güç ünitesi](nuclear_fuel#fission)

{recipe? spacebreaker:rtg}
