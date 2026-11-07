---
title: Delta-v ve Roket Denklemi
icon: minecraft:firework_rocket
research: spacebreaker:basic_rocketry
---
Uzayda yol kilometreyle değil, **Δv** ile ölçülür: hızını toplam ne kadar değiştirebileceğin (m/s). Her manevranın bir Δv bedeli vardır; roketinin Δv'si o bedelleri karşılamalı.

## Roket denklemi {#equation}
{color blue}**Δv = Isp · g₀ · ln(dolu kütle / boş kütle)**{/color}

- **Isp** (özgül itki): motorun verimi. Kerosen ≈ 300 s, hidrojen ≈ 450 s.
- **ln**: logaritma. İşte asıl sorun bu: yakıtı iki katına çıkarmak Δv'yi iki katına **çıkarmaz**.

> [!WARNING]
> Dünya'dan yörüngeye 9,4 km/s gerekir. Kerosenli tek kademe için roketin %96'sı yakıt olmalı. Bu neredeyse imkânsız.

## Neden kademe? {#stages}
Boşalan tankı ve motorları atınca, kalan roket **hafifler**; aynı yakıt daha çok Δv verir. Bu yüzden gerçek roketler 2–3 kademelidir.

Örnek: 504 tonluk bir roket, 10 ton yük, kerosen motorlar.

| | Δv |
|---|---|
| Tek kademe | ≈ 7,9 km/s: yörüngeye yetmez |
| Aynı kütle, iki kademe | ≈ 10,8 km/s: yörüngeye yeter |

## İtki/ağırlık oranı {#twr}
Rampada itki ağırlıktan büyük olmalı (oran > 1), yoksa kalkamazsın. Oran ne kadar yüksekse yerçekimine o kadar az Δv kaybedersin: Dünya'da 1,3 oranla ≈ 1,5 km/s kayıp, Ay'da 2 oranla yalnızca ≈ 0,2 km/s.

## Oyundaki sayılar {#realism}
Roket montaj tezgâhı her kademenin Δv'sini ve itki/ağırlık oranını gösterir. Sunucunun gerçekçilik ayarı yolculuk bedellerini ölçekler:

| Ayar | Bedel | Dünya → yörünge |
|---|---|---|
| Gerçekçi | gerçek değer | ≈ 9,0–9,4 km/s |
| Dengeli | %35 | ≈ 3,2 km/s |
| Kolay | %25 | ≈ 2,3 km/s |

>! **Gerçekçi** ayarda yörüngeye çıkılır; Ay ve Mars ise bugünkü montaj sınırını aşan ağır bir roket ister. Uzak dünyalara gitmek için **Dengeli** ya da **Kolay** ayarı seç.

→ [Roket Denklemi](rocket_equation) · [Transfer Yörüngeleri](sci_transfers)
