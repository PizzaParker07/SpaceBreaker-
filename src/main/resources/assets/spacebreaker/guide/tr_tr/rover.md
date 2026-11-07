---
title: Gezici
icon: spacebreaker:rover
items: [spacebreaker:rover, spacebreaker:sprint_rover, spacebreaker:pressurized_rover, spacebreaker:amphibious_rover]
research: spacebreaker:rovers
---
Üssünden uzak kraterlere, buz yataklarına ve harabelere giden elektrikli araç.

{recipe spacebreaker:rover}

| | |
|---|---|
| Kişi | 2 |
| Akü | 400 000 FE |
| Tüketim | blok başına 50 FE (≈ 8000 blok) |
| Tırmanma | 1 blok |
| Kargo | 27 yuva |

## Kullanım
- Bin: {key key.use}. İn: {key key.sneak}.
- **Kargo:** {key key.sneak} basılıyken geziciye tıkla.
- Tutuş yerçekimine bağlıdır: düşük yerçekiminde kolay kayarsın, yavaş dön.
- Tutuş zemine de bağlıdır. **Buzda**, en çok da Plüton'un azot buzunda, gezici zor hızlanır, zor durur, zor döner ve gittiği yönde kaymaya devam eder. Erken fren yap.

## Şarj
- Bir {item spacebreaker:battery_bank} ya da kablonun yanında **200 FE/t** ile dolar.
- Elinde {item spacebreaker:solar_panel} ya da {item spacebreaker:rtg} varken geziciye {key key.use}: koltukların arkasındaki arka güverteye takılır, yolda kendini şarj eder.

## Diğer geziciler {#models}
İkisi de hazır bir geziciden yapılır; yükseltmeler ve şarj aynı çalışır.

| | Gezici | Hızlı | Basınçlı |
|---|---|---|---|
| En yüksek hız | 9 m/s | 13 m/s | 7 m/s |
| Akü | 400 000 FE | 300 000 FE | 1 200 000 FE |
| Menzil | ≈ 8000 blok | ≈ 8500 blok | ≈ 15 000 blok |
| Kargo | 27 yuva | 9 yuva | 27 yuva |

{items spacebreaker:sprint_rover}
{item spacebreaker:sprint_rover} alüminyum-lityum gövdesiyle hafif ve sessizdir, ama lastikleri daha az tutar: gevşek zeminde daha çabuk kayar.
{recipe spacebreaker:sprint_rover}

{items spacebreaker:pressurized_rover}
{item spacebreaker:pressurized_rover} kapalı bir kabine sahiptir: içerideyken kask gerekmez, duvarları da **radyasyonun yarısını** keser. Oksijen tankını doldurmak için elinde oksijen gazı ya da LOX dolu bir kapla (örneğin {item spacebreaker:cryogenic_canister}) ona {key key.use} yap, ya da oksijen tutan bir tankın veya gaz borusunun yanına park et: kendini doldurur. Dolu tank tek kişiye yaklaşık **1,8 saat** yeter; içeride biri varken yaşam desteği aküden **4 FE/t** çeker. Tank boşalır ya da akü biterse kabinin basıncı düşer ("Kabin basınçsız"); giysini çıkarma.
{recipe spacebreaker:pressurized_rover}

{items spacebreaker:amphibious_rover}
{item spacebreaker:amphibious_rover} basınçlı kabini yan şamandıraları ve kıçta pervanesi olan sızdırmaz bir gövdeye oturtur. Karada biraz daha yavaştır (6 m/s), ama doğrudan suya ya da Titan'ın göllerindeki gibi sıvı metana girer ve **5 m/s** ile yüzerek devam eder; suda blok başına iki kat enerji harcar. Yüzerken fren yoktur: yavaşlamak için pervaneyi geri çalıştır. Kıyıya burnunu dayayınca karaya çıkar.
{recipe spacebreaker:amphibious_rover}
