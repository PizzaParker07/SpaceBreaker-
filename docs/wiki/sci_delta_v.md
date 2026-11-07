# Delta-v ve Roket Denklemi

*İlgili araştırma: Temel Roketçilik*

Uzayda yol kilometreyle değil, **Δv** ile ölçülür: hızını toplam ne kadar değiştirebileceğin (m/s). Her manevranın bir Δv bedeli vardır; roketinin Δv'si o bedelleri karşılamalı.

<a id="equation"></a>
### Roket denklemi
**Δv = Isp · g₀ · ln(dolu kütle / boş kütle)**

- **Isp** (özgül itki): motorun verimi. Kerosen ≈ 300 s, hidrojen ≈ 450 s.
- **ln**: logaritma. İşte asıl sorun bu: yakıtı iki katına çıkarmak Δv'yi iki katına **çıkarmaz**.

> [!WARNING]
> Dünya'dan yörüngeye 9,4 km/s gerekir. Kerosenli tek kademe için roketin %96'sı yakıt olmalı. Bu neredeyse imkânsız.

<a id="stages"></a>
### Neden kademe?
Boşalan tankı ve motorları atınca, kalan roket **hafifler**; aynı yakıt daha çok Δv verir. Bu yüzden gerçek roketler 2–3 kademelidir.

Örnek: 504 tonluk bir roket, 10 ton yük, kerosen motorlar.

| | Δv |
|---|---|
| Tek kademe | ≈ 7,9 km/s: yörüngeye yetmez |
| Aynı kütle, iki kademe | ≈ 10,8 km/s: yörüngeye yeter |

<a id="twr"></a>
### İtki/ağırlık oranı
Rampada itki ağırlıktan büyük olmalı (oran > 1), yoksa kalkamazsın. Oran ne kadar yüksekse yerçekimine o kadar az Δv kaybedersin: Dünya'da 1,3 oranla ≈ 1,5 km/s kayıp, Ay'da 2 oranla yalnızca ≈ 0,2 km/s.

<a id="realism"></a>
### Oyundaki sayılar
Roket montaj tezgâhı her kademenin Δv'sini ve itki/ağırlık oranını gösterir. Sunucunun gerçekçilik ayarı yolculuk bedellerini ölçekler:

| Ayar | Bedel | Dünya → yörünge |
|---|---|---|
| Gerçekçi | gerçek değer | ≈ 9,0–9,4 km/s |
| Dengeli | %35 | ≈ 3,2 km/s |
| Kolay | %25 | ≈ 2,3 km/s |

> [!WARNING]
> **Gerçekçi** ayarda yörüngeye çıkılır; Ay ve Mars ise bugünkü montaj sınırını aşan ağır bir roket ister. Uzak dünyalara gitmek için **Dengeli** ya da **Kolay** ayarı seç.

→ [Roket Denklemi](rocket_equation) · [Transfer Yörüngeleri](sci_transfers)

<details>
<summary>English: Delta-v and the Rocket Equation</summary>

*Related research: Basic Rocketry*

In space, distance is not measured in kilometres but in **Δv**: how much you can change your speed in total (m/s). Every manoeuvre has a Δv price; your rocket's Δv has to cover them.

<a id="equation"></a>
### The rocket equation
**Δv = Isp · g₀ · ln(full mass / empty mass)**

- **Isp** (specific impulse): how efficient the engine is. Kerosene ≈ 300 s, hydrogen ≈ 450 s.
- **ln**: the logarithm. That is the catch: doubling the fuel does **not** double the Δv.

> [!WARNING]
> Reaching orbit from Earth takes 9.4 km/s. A single kerosene stage would have to be 96 % fuel. That is close to impossible.

<a id="stages"></a>
### Why stages?
Dropping empty tanks and engines makes the rest of the rocket **lighter**, so the same fuel gives more Δv. That is why real rockets have 2–3 stages.

Example: a 504-tonne rocket, 10 tonnes of payload, kerosene engines.

| | Δv |
|---|---|
| One stage | ≈ 7.9 km/s: not enough for orbit |
| Same mass, two stages | ≈ 10.8 km/s: reaches orbit |

<a id="twr"></a>
### Thrust-to-weight ratio
On the pad, thrust must beat weight (ratio > 1) or you will not lift off. The higher the ratio, the less Δv gravity takes: about 1.5 km/s lost on Earth at 1.3, only ≈ 0.2 km/s on the Moon at 2.

<a id="realism"></a>
### Numbers in the game
The rocket assembler shows each stage's Δv and thrust-to-weight ratio. The server's realism setting scales trip costs:

| Setting | Cost | Earth → orbit |
|---|---|---|
| Realistic | real value | ≈ 9.0–9.4 km/s |
| Balanced | 35 % | ≈ 3.2 km/s |
| Easy | 25 % | ≈ 2.3 km/s |

> [!WARNING]
> On **Realistic** you can reach orbit; the Moon and Mars need a heavier rocket than today's assembly limit. For farther worlds pick **Balanced** or **Easy**.

→ [Rocket Equation](rocket_equation) · [Transfer Orbits](sci_transfers)

</details>
