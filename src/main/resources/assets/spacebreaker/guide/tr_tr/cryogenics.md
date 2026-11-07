---
title: Kriyojenik
icon: spacebreaker:cryogenic_tank
items: [spacebreaker:electrolyzer, spacebreaker:cryogenic_liquefier, spacebreaker:cryogenic_tank, spacebreaker:pressurized_tank, spacebreaker:gas_pipe, spacebreaker:cryogenic_canister, spacebreaker:liquid_oxygen_bucket, spacebreaker:liquid_hydrogen_bucket]
research: spacebreaker:electrolysis
---
Roket motorları yakıtı **sıvı oksijenle** yakar. Onu sudan elde edersin:

1. {item spacebreaker:electrolyzer} suyu **oksijen** ve **hidrojen** gazına ayırır. Onu **sonsuz bir su kaynağından** çekme kipindeki bir {item spacebreaker:fluid_pipe} ile besle (ucuna {key key.sneak} + {key key.use}): **100 mB/t**, kovayla taşımaya gerek yok.
2. Gazları {item spacebreaker:gas_pipe} ile {item spacebreaker:pressurized_tank}'a ya da doğrudan sıvılaştırıcıya götür.
3. {item spacebreaker:cryogenic_liquefier} oksijeni **−183 °C**'de, hidrojeni **−253 °C**'de sıvılaştırır.
4. Sıvıları {item spacebreaker:cryogenic_tank} ya da {item spacebreaker:cryogenic_canister} içinde sakla.

> Tanklar kırılınca içeriğini korur: yakıtı ve sıvı oksijeni tankla taşıyabilirsin, örneğin istasyonun yakıt deposuna. → [Uzay İstasyonu](space_station)

{recipe spacebreaker:electrolysis/water}
{recipe spacebreaker:liquefaction/oxygen}

> [!WARNING]
> Kriyojenik sıvıları açığa dökme: kaynayıp uçarlar. Kova yalnızca taşımak için.
