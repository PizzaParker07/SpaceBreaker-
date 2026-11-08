# Nükleer Yakıt

*İlgili araştırma: Nükleer Fisyon*

Uraninit cevherinden nükleer termal roketin yakıtına giden yol.

### Zincir
1. **Ham Uraninit**'i kırıcıda **uranyum tozuna** öğüt.
2. Kimyasal reaktörde toz + **125 mB sülfürik asit** → **Sarı Pasta**.
3. **Gaz Santrifüjü**'nde 4 sarı pasta → **1** **Zenginleştirilmiş Uranyum** + **3** **Tükenmiş Uranyum**.
4. Elektrik ark fırınında 2 zenginleştirilmiş uranyum + 2 kömür tozu + 2 tungsten çubuk → **Nükleer Yakıt Elemanı**.

| Girdi | Makine | Çıktı |
|---|---|---|
| Uranyum Tozu<br>Sülfürik Asit (125 mB) | Kimyasal Reaktör<br>60 FE/t · 4 sn | Sarı Pasta |

| Girdi | Makine | Çıktı |
|---|---|---|
| Sarı Pasta ×4 | Gaz Santrifüjü<br>160 FE/t · 40 sn | Zenginleştirilmiş Uranyum<br>Tükenmiş Uranyum ×3 |

### Santrifüj dizisi
Tek bir santrifüj yavaştır. Gerçek tesislerde binlercesi art arda çalışır. Burada aynı seviyede yanına konan ve **çalışan her santrifüj yarım kademe hız** ekler; en çok dört komşu, yani en fazla **3 kat**. Santrifüjleri bir sıra ya da artı biçiminde diz.

> [!TIP]
> Her zenginleştirmede büyük bir yığın **Tükenmiş Uranyum** çıkar: zenginleştirmenin doğal yan ürünüdür. Çok yoğundur: preste levha kalıbıyla **Tükenmiş Uranyum Levhası** olur ve tungsten levhayla birlikte **radyasyon kalkanı** malzemesi sayılır (*Radyasyon Kalkanı*). Fisyon güç ünitesinin ve nükleer termal motorun tarifinde kullanılır.

| Girdi | Makine | Çıktı |
|---|---|---|
| Tükenmiş Uranyum | Pres<br>30 FE/t · 3 sn | Tükenmiş Uranyum Levhası |

| Girdi | Makine | Çıktı |
|---|---|---|
| Zenginleştirilmiş Uranyum ×2<br>Kömür Tozu ×2<br>Tungsten Çubuk ×2 | Elektrik Ark Fırını<br>120 FE/t · 20 sn | Nükleer Yakıt Elemanı |

<a id="fission"></a>
### Fisyon güç ünitesi

**Fisyon Yüzey Güç Ünitesi**

**Fisyon Yüzey Güç Ünitesi**, kilovat sınıfı bir fisyon yüzey güç ünitesidir. İçine bir **Nükleer Yakıt Elemanı** koy: gece gündüz, nerede olursan ol **240 FE/t** verir. Bir eleman **10 oyun günü** dayanır ve yakıt yalnız güç çekilirken yanar.

|   |   |   |
|---|---|---|
| Alüminyum Levha | Alüminyum Levha | Alüminyum Levha |
| Tükenmiş Uranyum Levhası | Gelişmiş Devre Kartı | Tükenmiş Uranyum Levhası |
| Çelik Levha | Tungsten Levha | Çelik Levha |

→ **Fisyon Yüzey Güç Ünitesi** (Çalışma Masası)

Kalkan yuvalarına tükenmiş uranyum levha ya da tungsten levha konabilir.

> [!WARNING]
> Çalışan çekirdek radyasyon yayar: bir blok uzakta günde yaklaşık **50 Sv**, uzaklığın karesiyle azalır. Aradaki her katı blok yalnız **%30**'unu geçirir; üç blok regolit bunu yaklaşık **%3**'e indirir. Üniteyi göm ya da çevresine regolitten bir set yap, üssünü de onun doğrudan görüş hattından uzak tut.

### RTG peleti
[RTG Yakıt Peleti](power) artık **sinterleme fırınında 4 sarı pastadan** yapılır. → [Enerji](power)

| Girdi | Makine | Çıktı |
|---|---|---|
| Sarı Pasta ×4 | Sinterleme Fırını<br>80 FE/t · 10 sn | RTG Yakıt Peleti |

> [!WARNING]
> Peletin kendisi ise ışınım yayar: üstünde taşırken doz alırsın. RTG'ye tak ve uzak dur.

> [!TIP]
> **Taze uranyum yakıtı tehlikeli değildir:** uranyumun etkinliği düşüktür, eline alıp taşıyabilirsin. Asıl radyasyon, yakıt elemanı **nükleer termal roket motorunda yanarken** çıkar. → [Roket Parçaları](rocket_parts#ntr)

<details>
<summary>English: Nuclear Fuel</summary>

*Related research: Nuclear Fission*

The road from uraninite ore to the fuel of a nuclear thermal rocket.

### The chain
1. Grind **Raw Uraninite** into **uranium dust** in the crusher.
2. In the chemical reactor, dust + **125 mB of sulfuric acid** → **Yellowcake**.
3. In the **Gas Centrifuge**, 4 yellowcake → **1** **Enriched Uranium** + **3** **Depleted Uranium**.
4. In the electric arc furnace, 2 enriched uranium + 2 coal dust + 2 tungsten rods → **Nuclear Fuel Element**.

| Input | Machine | Output |
|---|---|---|
| Uranium Dust<br>Sulfuric Acid (125 mB) | Chemical Reactor<br>60 FE/t · 4 s | Yellowcake |

| Input | Machine | Output |
|---|---|---|
| Yellowcake ×4 | Gas Centrifuge<br>160 FE/t · 40 s | Enriched Uranium<br>Depleted Uranium ×3 |

### The centrifuge cascade
One centrifuge alone is slow. Real plants run thousands one after another. Here **every working centrifuge** placed beside one on the same level adds **half a stage of speed**; up to four neighbours, so at most **3×**. Line them up in a row or a plus shape.

> [!TIP]
> Every enrichment leaves a big pile of **Depleted Uranium**: the natural by-product of enrichment. It is very dense: the press with a plate die turns it into **Depleted Uranium Plate**, which counts as **radiation shielding** material alongside tungsten plate (*Radiation Shielding*). It goes into the recipes of the fission power unit and the nuclear thermal engine.

| Input | Machine | Output |
|---|---|---|
| Depleted Uranium | Press<br>30 FE/t · 3 s | Depleted Uranium Plate |

| Input | Machine | Output |
|---|---|---|
| Enriched Uranium ×2<br>Coal Dust ×2<br>Tungsten Rod ×2 | Electric Arc Furnace<br>120 FE/t · 20 s | Nuclear Fuel Element |

<a id="fission"></a>
### Fission power unit

**Fission Surface Power Unit**

The **Fission Surface Power Unit** is a kilowatt-class fission surface power unit. Load a **Nuclear Fuel Element** and it gives **240 FE/t**, day and night, wherever you are. One element lasts **10 game days**, and fuel only burns while power is drawn.

|   |   |   |
|---|---|---|
| Aluminum Plate | Aluminum Plate | Aluminum Plate |
| Depleted Uranium Plate | Advanced Circuit | Depleted Uranium Plate |
| Steel Plate | Tungsten Plate | Steel Plate |

→ **Fission Surface Power Unit** (Crafting Table)

The shielding slots take depleted uranium plate or tungsten plate.

> [!WARNING]
> A running core is radioactive: about **50 Sv/day** one block away, falling with the square of the distance. Every solid block in between lets only **30 %** through, so three blocks of regolith cut it to about **3 %**. Bury the unit or build a regolith berm round it, and keep your base out of its line of sight.

### RTG pellet
The [RTG Fuel Pellet](power) is now made from **4 yellowcake in the sintering furnace**. → [Power](power)

| Input | Machine | Output |
|---|---|---|
| Yellowcake ×4 | Sintering Furnace<br>80 FE/t · 10 s | RTG Fuel Pellet |

> [!WARNING]
> The pellet itself does give off radiation: carrying it gives you a dose. Put it in the RTG and keep your distance.

> [!TIP]
> **Fresh uranium fuel is not dangerous:** uranium has low activity, and you can pick it up and carry it. The real radiation comes when the fuel element **burns in a nuclear thermal rocket engine**. → [Rocket Parts](rocket_parts#ntr)

</details>
