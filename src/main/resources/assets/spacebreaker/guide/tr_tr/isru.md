---
title: Yerinde Üretim (ISRU)
icon: spacebreaker:sabatier_reactor
items: [spacebreaker:atmosphere_processor, spacebreaker:sabatier_reactor, spacebreaker:regolith_heater, spacebreaker:ice_melter, spacebreaker:chemical_reactor]
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
Kayayı eritip elektrikle ayrıştırır: **kayadan oksijen**.
{recipe spacebreaker:regolith_processing/regolith}

{page}
## Sabatier reaktörü
Karbondioksit ile hidrojeni **metan ve suya** çevirir. Su, elektroliz ünitesine geri döner; metanı [Kriyojenik](cryogenics) sıvılaştırıcıyla sıvılaştırıp saklayabilirsin.

>! Roket motorların şimdilik **RP-1 ve sıvı oksijen** yakar. Dönüş için RP-1'i Dünya'dan getir; sıvı oksijeni yerinde üretebilirsin.
{recipe spacebreaker:sabatier/methane}

> [!TIP]
> **Mars'ta oksijen ve metan:** buz eritici → su → elektroliz ünitesi → oksijen + hidrojen; oksijen → kriyojenik sıvılaştırıcı → sıvı oksijen. Hidrojen ve atmosfer işlemcisinden gelen CO₂ Sabatier reaktörüne gider. Çıkan su yine elektroliz ünitesine gider.

## Kimyasal reaktör
Bir katı ve iki sıvıyı birleştirir; örneğin kükürt, su ve oksijenden **sülfürik asit** yapar.
{recipe spacebreaker:chemical_reaction/sulfuric_acid}
