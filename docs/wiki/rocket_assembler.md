# Roket Montaj Tezgâhı

*İlgili araştırma: Temel Roketçilik*

|   |   |   |
|---|---|---|
| Mavi Beton | Mavi Beton | Mavi Beton |
| Alüminyum Levha | Çalışma Masası | Alüminyum Levha |
| Demir Bloğu | Redstone Bloğu | Demir Bloğu |

→ **Roket Montaj Tezgâhı** (Çalışma Masası)

Parçaları tezgâhın yuvalarına yerleştir, tezgâh roketi senin için hesaplar.

1. **Kademeleri alttan yukarı doldur.** 1. kademe en altta: motor ve tanklar.
2. Kademeler arasına **ayırıcı** koy.
3. En üste **yükü** koy: kapsül (altında ısı kalkanı, üstünde paraşüt) ya da burun konisi.
4. Yan parçaları yan yuvalara ekle.
5. Ekranda her kademenin **Δv**'sine ve **itki/ağırlık** oranına bak, sonra **Birleştir**'e bas.

Elinde bir **Roket** olur. İpucu satırı yüksekliğini, kütlesini, kademe sayısını ve toplam Δv'sini gösterir.

### Ne kadar Δv lazım?
| Yol | Δv (m/s) |
|---|---|
| Dünya yüzeyi → alçak yörünge | 9400 |
| Alçak yörünge → Ay yörüngesi | 4000 |
| Ay yörüngesi → Ay yüzeyi | 1870 |
| Ay yüzeyi → Ay yörüngesi | 1870 |
| Ay yörüngesi → Dünya (doğrudan giriş) | 850 |

> [!WARNING]
> İtki/ağırlık oranı **1'in altındaysa** roket yerden kalkamaz. İlk kademede 1,3–1,5 arası iyidir.

> [!TIP]
> Δv yetmiyorsa daha çok yakıt eklemek her zaman çözüm değildir: tanklar da ağırdır. Bir kademe daha eklemek ya da üst kademeye vakum motoru takmak genellikle daha iyi sonuç verir. → [Roket Denklemi](rocket_equation)

<details>
<summary>English: Rocket Assembler</summary>

*Related research: Basic Rocketry*

|   |   |   |
|---|---|---|
| Blue Concrete | Blue Concrete | Blue Concrete |
| Aluminum Plate | Crafting Table | Aluminum Plate |
| Block of Iron | Block of Redstone | Block of Iron |

→ **Rocket Assembler** (Crafting Table)

Put the parts into the assembler's slots and it works out the rocket for you.

1. **Fill the stages from the bottom up.** Stage 1 is at the bottom: engine and tanks.
2. Put a **separator** between stages.
3. Put the **payload** on top: a capsule (heat shield under it, parachute on top) or a nose cone.
4. Add side parts in the side slots.
5. Check each stage's **Δv** and **thrust-to-weight** on screen, then press **Assemble**.

You get a **Rocket**. Its tooltip shows the height, mass, number of stages and total Δv.

### How much Δv?
| Trip | Δv (m/s) |
|---|---|
| Earth surface → low orbit | 9,400 |
| Low orbit → lunar orbit | 4,000 |
| Lunar orbit → lunar surface | 1,870 |
| Lunar surface → lunar orbit | 1,870 |
| Lunar orbit → Earth (direct reentry) | 850 |

> [!WARNING]
> If thrust-to-weight is **below 1** the rocket cannot lift off. 1.3–1.5 is good for the first stage.

> [!TIP]
> When Δv falls short, more fuel is not always the answer: tanks are heavy too. Another stage, or a vacuum engine on the upper stage, usually works better. → [The Rocket Equation](rocket_equation)

</details>
