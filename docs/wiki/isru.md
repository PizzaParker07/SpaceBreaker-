# Yerinde Üretim (ISRU)

![](../screenshots/guide/tr_06_isru.jpg)

*İlgili araştırma: Yerinde Kaynak Kullanımı*

Dünya'dan fırlatılan her kilogram pahalıdır. Bu yüzden indiğin yerde yapabildiğini **orada** yap. Buna yerinde kaynak kullanımı (ISRU) denir.

### Zincir
| Girdi → çıktı | Makine |
|---|---|
| Buz → su | **Buz Eritici** |
| Kuru buz → CO₂ | **Buz Eritici** |
| Su → O₂ + H₂ | [Elektroliz Ünitesi](cryogenics) |
| Yerel hava → CO₂ (Mars) | **Atmosfer İşlemcisi** |
| CO₂ + H₂ → metan + su | **Sabatier Reaktörü** |
| Regolit → O₂ | **Regolit Isıtıcı** |

### Buz eritici
Buzu temiz suya, kuru buzu karbondioksite çevirir.

| Girdi | Makine | Çıktı |
|---|---|---|
| Ay Buzu | Çalışma Masası<br>30 FE/t · 3 sn | Su (1000 mB) |

### Atmosfer işlemcisi
Bulunduğun yerin havasından en yararlı gazı ayırır: Mars'ta **karbondioksit**. Üstünde **açık hava** olmalı; kapalı bir odada çalışmaz.

### Regolit ısıtıcı
Kayayı eritip elektrikle ayrıştırır: **kayadan oksijen**.

| Girdi | Makine | Çıktı |
|---|---|---|
| Ay Regoliti | Çalışma Masası<br>120 FE/t · 8 sn | Demir Tozu (%20 şansla)<br>Oksijen (250 mB) |

### Sabatier reaktörü
Karbondioksit ile hidrojeni **metan ve suya** çevirir. Su, elektroliz ünitesine geri döner. Metanı [Kriyojenik](cryogenics) sıvılaştırıcıyla **sıvı metana** çevir: sıvı oksijenle birlikte **metaloks motorlarının** yakıtıdır. Roketi yakıt hattıyla, boruyla ya da kova / kriyojenik kapla sağ tıklayarak doldur. → [Roket Parçaları](rocket_parts)

| Girdi | Makine | Çıktı |
|---|---|---|
| carbon dioxide (500 mB)<br>hydrogen (2000 mB) | Sabatier Reaktörü<br>60 FE/t · 4 sn | Metan (500 mB)<br>Su (250 mB) |

> [!TIP]
> **Mars'ta oksijen ve metan:** buz eritici → su → elektroliz ünitesi → oksijen + hidrojen; oksijen → kriyojenik sıvılaştırıcı → sıvı oksijen. Hidrojen ve atmosfer işlemcisinden gelen CO₂ Sabatier reaktörüne gider. Çıkan su yine elektroliz ünitesine gider.

### Kimyasal reaktör
Bir katı ve iki sıvıyı birleştirir; örneğin kükürt, su ve oksijenden **sülfürik asit** yapar.

| Girdi | Makine | Çıktı |
|---|---|---|
| Kükürt<br>water (250 mB)<br>oxygen (750 mB) | Çalışma Masası<br>60 FE/t · 5 sn | Sülfürik Asit (250 mB) |

<details>
<summary>English: In-Situ Resources (ISRU)</summary>

*Related research: In-Situ Resources*

Every kilogram launched from Earth is expensive. So make what you can **where you land**. This is called in-situ resource utilisation (ISRU).

### The chain
| Input → output | Machine |
|---|---|
| Ice → water | **Ice Melter** |
| Dry ice → CO₂ | **Ice Melter** |
| Water → O₂ + H₂ | [Electrolyzer](cryogenics) |
| Local air → CO₂ (Mars) | **Atmosphere Processor** |
| CO₂ + H₂ → methane + water | **Sabatier Reactor** |
| Regolith → O₂ | **Regolith Heater** |

### Ice melter
Turns ice into clean water and dry ice into carbon dioxide.

| Input | Machine | Output |
|---|---|---|
| Lunar Ice | Crafting Table<br>30 FE/t · 3 s | Water (1000 mB) |

### Atmosphere processor
Separates the most useful gas from the local air: **carbon dioxide** on Mars. It needs **open air** above it; it does not work in a closed room.

### Regolith heater
Melts rock and splits it with electricity: **oxygen from rock**.

| Input | Machine | Output |
|---|---|---|
| Lunar Regolith | Crafting Table<br>120 FE/t · 8 s | Iron Dust (20% chance)<br>Oxygen (250 mB) |

### Sabatier reactor
Turns carbon dioxide and hydrogen into **methane and water**. The water goes back to the electrolyser. Turn the methane into **liquid methane** with the [Cryogenics](cryogenics) liquefier: with liquid oxygen it fuels the **methalox engines**. Fill the rocket through the fuel umbilical, a pipe, or by right-clicking with a bucket or cryogenic canister. → [Rocket Parts](rocket_parts)

| Input | Machine | Output |
|---|---|---|
| carbon dioxide (500 mB)<br>hydrogen (2000 mB) | Sabatier Reactor<br>60 FE/t · 4 s | Methane (500 mB)<br>Water (250 mB) |

> [!TIP]
> **Oxygen and methane on Mars:** ice melter → water → electrolyser → oxygen + hydrogen; oxygen → cryogenic liquefier → liquid oxygen. Hydrogen and CO₂ from the atmosphere processor go into the Sabatier reactor. The water it gives goes back to the electrolyser.

### Chemical reactor
Combines one solid and two fluids; for example sulfur, water and oxygen make **sulfuric acid**.

| Input | Machine | Output |
|---|---|---|
| Sulfur<br>water (250 mB)<br>oxygen (750 mB) | Crafting Table<br>60 FE/t · 5 s | Sulfuric Acid (250 mB) |

</details>
