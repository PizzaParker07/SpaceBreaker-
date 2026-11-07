---
title: Roket Montaj Tezgâhı
icon: spacebreaker:rocket_assembler
items: [spacebreaker:rocket_assembler, spacebreaker:rocket]
research: spacebreaker:basic_rocketry
---
{recipe spacebreaker:rocketry/rocket_assembler}

Parçaları tezgâhın yuvalarına yerleştir, tezgâh roketi senin için hesaplar.

1. **Kademeleri alttan yukarı doldur.** 1. kademe en altta: motor ve tanklar.
2. Kademeler arasına **ayırıcı** koy.
3. En üste **yükü** koy: kapsül (altında ısı kalkanı, üstünde paraşüt) ya da burun konisi.
4. Yan parçaları yan yuvalara ekle.
5. Ekranda her kademenin **Δv**'sine ve **itki/ağırlık** oranına bak, sonra **Birleştir**'e bas.

Elinde bir {item spacebreaker:rocket} olur. İpucu satırı yüksekliğini, kütlesini, kademe sayısını ve toplam Δv'sini gösterir.

> Nükleer termal motorlu kademe **☢ Nükleer** diye işaretlenir. İlk kademedeyse işaret kırmızı olur: kalkmaya gücü yetmez ve rampayı ışınlar, o yüzden onu üst kademeye koy. → [Nükleer termal motor](rocket_parts#ntr)

## Ne kadar Δv lazım?
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
