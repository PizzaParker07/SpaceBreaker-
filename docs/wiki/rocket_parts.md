# Roket Parçaları

![](../screenshots/guide/tr_08_rocket_parts.jpg)

*İlgili araştırma: Temel Roketçilik*

Bir roket, üst üste dizilmiş **kademelerden** ve en üstteki **yükten** oluşur.

### Yük

**Mürettebat Kapsülü** · **Ablatif Isı Kalkanı** · **Paraşüt Modülü** · **Burun Konisi**

- **Mürettebat Kapsülü**: mürettebatı taşır.
- **Ablatif Isı Kalkanı**: kapsülün **altına** takılır; atmosfere girişte seni korur.
- **Paraşüt Modülü**: kapsülün **üstüne** takılır; yumuşak iniş için.
- **Burun Konisi**: insansız roketlerde yükün tepesi.

### Kademe

**Kerosen Motoru (Deniz Seviyesi)** · **Kerosen Motoru (Vakum)** · **Küçük Yakıt Tankı** · **Orta Yakıt Tankı** · **Büyük Yakıt Tankı**

Her kademede altta **bir motor**, üstünde **yakıt tankları** olur. Tanklar, o kademenin motoru ne yakıyorsa onu taşır.
- **Deniz seviyesi motoru** havada iyi çalışır: ilk kademe için.
- **Vakum motoru** uzayda çok daha verimlidir: üst kademeler için.

### Motorlar ve yakıtlar

**Metaloks Motoru (Deniz Seviyesi)** · **Metaloks Motoru (Vakum)** · **Hidroloks Motoru (Deniz Seviyesi)** · **Hidroloks Motoru (Vakum)**

**Metaloks motorları** *Metan Motorları* ile açılır. **Sıvı metan ve sıvı oksijen** yakarlar; yakıtlarını Mars'ta üretebilirsin. Isp'deki iki sayı: deniz seviyesi / vakum.

| Motor | İtki | Isp | Yakıt |
|---|---|---|---|
| Kerosen (deniz) | 560 kN | 282 / 311 s | RP-1 + LOX (1 : 2,56) |
| Kerosen (vakum) | 190 kN | 345 s | RP-1 + LOX (1 : 2,56) |
| Metaloks (deniz) | 600 kN | 330 / 350 s | metan + LOX (1 : 3,6) |
| Metaloks (vakum) | 220 kN | 380 s | metan + LOX (1 : 3,6) |
| Hidroloks (deniz) | 650 kN | 366 / 452 s | hidrojen + LOX (1 : 6) |
| Hidroloks (vakum) | 110 kN | 465 s | hidrojen + LOX (1 : 6) |
| Nükleer termal (vakum) | 150 kN | 880 s | yalnız sıvı hidrojen |

**Hidroloks motorları** *Hidrojen Motorları* ile açılır. Sıvı hidrojen en verimli yakıttır.

> [!TIP]
> **Gerçek ve oyun farkı:** gerçekte sıvı hidrojen çok hacimlidir (yaklaşık 71 kg/m³, kerosen yaklaşık 800). Oyunda ise tank kapasitesi, içinde ne olursa olsun kütle olarak sayılır; bu yüzden hidroloks ve nükleer kademeler gerçektekinden çok daha küçük tanklarla uçar.

<a id="ntr"></a>
### Nükleer termal motor

**Nükleer Termal Motor (Vakum)**

**Nükleer Termal Motor (Vakum)** (*Nükleer Termal İtki*) oksitleyici yakmaz: bir fisyon reaktörü sıvı hidrojeni ısıtır. Vakumda **880 s** özgül itki, her kimyasal motorun yaklaşık iki katı. Ama 150 kN itki ve 3,4 t kütleyle yerden kalkamayacak kadar zayıftır: üst kademe motorudur. Kademesi hidroloks kademeleri gibi aynı hidrojen hattından yalnız sıvı hidrojen alır.

> [!WARNING]
> Yanarken bir **radyasyon kaynağıdır**: doz uzaklığın karesiyle azalır, motorun üstündeki gölge kalkanı mürettebatı korur. Soğukken (taze yakıtla) güvenlidir. Egzozu neredeyse görünmez: soluk, mavimsi-mor bir ışıma.

Tarifinde bir nükleer yakıt elemanı, gölge kalkanı için tungsten levha, nikel süperalaşım, titanyum levha ve bir hidroloks vakum motoru var. → [Nükleer Yakıt](nuclear_fuel)

|   |   |   |
|---|---|---|
| Tükenmiş Uranyum Levhası | Nükleer Yakıt Elemanı | Tükenmiş Uranyum Levhası |
| Nikel Süper Alaşımı Külçe | Hidroloks Motoru (Vakum) | Nikel Süper Alaşımı Külçe |
| Titanyum Levha | Titanyum Levha | Titanyum Levha |

→ **Nükleer Termal Motor (Vakum)** (Çalışma Masası)

Montaj tezgâhı her kademenin yakıt türünü ve Δv'sini gösterir.

### Ayırıcılar

**Kademe Ayırıcı** · **Ara Kademe**

Kademeler arasına bir **kademe ayırıcı** ya da **ara kademe** koy. Yakıtı biten kademe düşer, roket hafifler.

### Yan parçalar

**Kanatçıklar** · **Izgara Kanatçıklar** · **İniş Bacakları** · **Manevra İticileri**

- **Kanatçıklar**: çıkışta yön kaybını azaltır.
- **Izgara kanatçıklar**: ilk kademeyi yönlendirir.
- **İniş bacakları**: Ay'a inmek için **şart**.
- **Manevra iticileri**: inişte ince kontrol.

|   |   |   |
|---|---|---|
| Demir Bloğu | Alüminyum Levha | Demir Bloğu |
| Redstone Tozu | Maden Eritme Ocağı | Redstone Tozu |
| Bakır Bloğu |   | Bakır Bloğu |

→ **Kerosen Motoru (Deniz Seviyesi)** (Çalışma Masası)

<details>
<summary>English: Rocket Parts</summary>

*Related research: Basic Rocketry*

A rocket is a stack of **stages** with the **payload** on top.

### Payload

**Crew Capsule** · **Ablative Heat Shield** · **Parachute Module** · **Nose Cone**

- **Crew Capsule**: carries the crew.
- **Ablative Heat Shield**: goes **under** the capsule; protects you during reentry.
- **Parachute Module**: goes **on top** of the capsule, for a soft landing.
- **Nose Cone**: the top of an uncrewed payload.

### Stages

**Kerosene Engine (Sea Level)** · **Kerosene Engine (Vacuum)** · **Small Fuel Tank** · **Medium Fuel Tank** · **Large Fuel Tank**

Each stage has **one engine** at the bottom and **fuel tanks** above it. The tanks carry whatever that stage's engine burns.
- The **sea-level engine** works well in air: for the first stage.
- The **vacuum engine** is far more efficient in space: for the upper stages.

### Engines and fuels

**Methalox Engine (Sea Level)** · **Methalox Engine (Vacuum)** · **Hydrolox Engine (Sea Level)** · **Hydrolox Engine (Vacuum)**

**Methalox engines** are unlocked by *Methane Engines*. They burn **liquid methane and liquid oxygen**; you can make that fuel on Mars. Two Isp numbers: sea level / vacuum.

| Engine | Thrust | Isp | Fuel |
|---|---|---|---|
| Kerosene (sea level) | 560 kN | 282 / 311 s | RP-1 + LOX (1 : 2.56) |
| Kerosene (vacuum) | 190 kN | 345 s | RP-1 + LOX (1 : 2.56) |
| Methalox (sea level) | 600 kN | 330 / 350 s | methane + LOX (1 : 3.6) |
| Methalox (vacuum) | 220 kN | 380 s | methane + LOX (1 : 3.6) |
| Hydrolox (sea level) | 650 kN | 366 / 452 s | hydrogen + LOX (1 : 6) |
| Hydrolox (vacuum) | 110 kN | 465 s | hydrogen + LOX (1 : 6) |
| Nuclear thermal (vacuum) | 150 kN | 880 s | liquid hydrogen only |

**Hydrolox engines** are unlocked by *Hydrogen Engines*. Liquid hydrogen is the most efficient fuel.

> [!TIP]
> **Real life and the game:** in reality liquid hydrogen is very bulky (about 71 kg/m³, kerosene about 800). In the game a tank's capacity counts as mass whatever is inside, so hydrolox and nuclear stages fly with much smaller tanks than real ones would.

<a id="ntr"></a>
### Nuclear thermal engine

**Nuclear Thermal Engine (Vacuum)**

The **Nuclear Thermal Engine (Vacuum)** (*Nuclear Thermal Propulsion*) burns no oxidiser: a fission reactor heats liquid hydrogen. **880 s** of specific impulse in vacuum, about twice any chemical engine. But at 150 kN and 3.4 t it is too weak to leave the ground: it is an upper-stage engine. Its stage takes liquid hydrogen alone through the same hydrogen line as hydrolox stages.

> [!WARNING]
> While it burns it is a **radiation source**: the dose falls with the square of the distance, and the shadow shield above the engine protects the crew. Cold (with fresh fuel), it is safe. Its exhaust is almost invisible: a faint bluish-violet glow.

Its recipe takes a nuclear fuel element, tungsten plate for the shadow shield, nickel superalloy, titanium plate and a hydrolox vacuum engine. → [Nuclear Fuel](nuclear_fuel)

|   |   |   |
|---|---|---|
| Depleted Uranium Plate | Nuclear Fuel Element | Depleted Uranium Plate |
| Nickel Superalloy Ingot | Hydrolox Engine (Vacuum) | Nickel Superalloy Ingot |
| Titanium Plate | Titanium Plate | Titanium Plate |

→ **Nuclear Thermal Engine (Vacuum)** (Crafting Table)

The assembler shows each stage's fuel type and Δv.

### Separators

**Stage Decoupler** · **Interstage**

Put a **decoupler** or an **interstage** between stages. A stage that runs dry drops away and the rocket gets lighter.

### Side parts

**Fins** · **Grid Fins** · **Landing Legs** · **RCS Thrusters**

- **Fins**: less steering loss on ascent.
- **Grid fins**: steer the first stage.
- **Landing legs**: **required** to land on the Moon.
- **RCS thrusters**: fine control for landings.

|   |   |   |
|---|---|---|
| Block of Iron | Aluminum Plate | Block of Iron |
| Redstone Dust | Blast Furnace | Redstone Dust |
| Block of Copper |   | Block of Copper |

→ **Kerosene Engine (Sea Level)** (Crafting Table)

</details>
