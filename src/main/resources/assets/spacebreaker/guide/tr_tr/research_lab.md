---
title: Araştırma Laboratuvarı
icon: spacebreaker:research_lab
items: [spacebreaker:research_lab]
---
Numuneleri, diskleri ve deney kayıtlarını inceleyip sana teknoloji öğreten tezgâh.

{recipe spacebreaker:research_lab}

## Kullanımı
1. Laboratuvarı yerleştir ve yanına enerji ver: başlangıçta bir {item spacebreaker:combustion_generator} yeter.
2. {key key.use} ile aç. **Araştırmacı**, laboratuvarı en son açan oyuncudur.
3. Sol yuvaya bir disk, numune ya da deney kaydı koy.
4. Ok dolunca teknolojiyi öğrenirsin. Ekranda bir bildirim çıkar; aynı anda öğrenilen teknolojiler tek bildirimde toplanır.

Enerji çubuğu soldadır. Laboratuvarın neyi okuduğunu ve ne kadar kaldığını görmek için fareyle okun üstüne gel. Okun üstündeki küçük kırmızı **!** bir şeyin seni beklediği anlamına gelir (enerji yok, yeni bir şey yok, sonuç yuvası dolu...): nedenini görmek için üstüne gel.

| Faz | Süre | Enerji |
|---|---|---|
| Faz 1 | 30 sn | 10 FE/t |
| Faz 2 | 60 sn | 24 FE/t |
| Faz 3 | 90 sn | 60 FE/t |

Laboratuvar 20 000 FE depolar. Enerji biterse inceleme durur ama ilerleme kaybolmaz.

> [!NOTE]
> Disk okunduktan sonra **geri verilir**; arkadaşlarına ödünç verebilirsin. Numune ise tükenir. Numunede senin için yeni bir şey yoksa laboratuvar bunu söyler ve numuneyi saklar.

> [!WARNING]
> Bir diski okuyabilmek için o teknolojinin **ön koşullarını** bilmen gerekir. İleri seviye diskleri sakla; sonra işine yarar.

## Takımla araştırma
Sağ üstteki iki kişi düğmesi **takımla paylaşımı** açar. Açıkken, skor tablosu takımındaki herkes laboratuvarın bulduğunu öğrenir; o an oyunda olmayanlar da girdiklerinde öğrenir. Takım kurmak için `/team add <ad>`, katılmak için `/team join <ad>`.

Yanındaki ağaç düğmesi [Araştırma Ağacı](research_tree)'nı açar.
