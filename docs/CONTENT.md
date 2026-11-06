# Spacebreaker — İçerik Listesi

İşaretler: ✅ yapıldı · 🎨 stil örneğinde (onay bekliyor) · ⬜ planlandı

Her içerik paketi gerçek istemcide vitrin dünyasında fotoğraflanır; onaydan sonra bir sonraki pakete geçilir.

## Kalite standardı
- 16×16 vanilla uyumlu piksel sanatı, kodla piksel piksel çizilir; her malzemenin kendi renk rampası, ışık sol üstten.
- Makineler gerçek 3B; hareketli parçalar animasyonlu (ör. güneşi takip eden panel).
- Eşyalar envanterde 2B, elde 3B. Giysi özel 3B model + saydam vizör + altın güneş vizörü + parlayan lambalar.
- Tüm bloklara shader paketleri için LabPBR normal ve parlaklık haritaları.

---

## Faz 1 — Ay

### Paket ① Madenler ve malzemeler
| Durum | İçerik | Not |
|---|---|---|
| 🎨 | Boksit Cevheri (taş + derin kayrak), Ham Boksit, Ham Boksit Bloğu | pizolitik kızıl-kahve yumrular |
| 🎨 | Alüminyum Külçe, Alüminyum Levha | fırçalanmış metal levha |
| ⬜ | Rutil → Titanyum, Pentlandit → Nikel, Spodümen → Lityum, Wolframit → Tungsten | her biri: cevher ×2, ham, ham blok, külçe, parça, blok, levha, toz |
| ⬜ | Ham petrol cepleri + yüzey sızıntıları | RP-1 ve plastik |
| ⬜ | Çelik, Al-Li alaşımı, nikel süper alaşımı, plastik, silisyum plaka, güneş hücresi, devre kartları, elektrik motoru, ablatif kalkan, altın folyo, turuncu köpük, silikon conta, pres kalıpları | |

### Paket ② Enerji ve makineler
| Durum | İçerik | Not |
|---|---|---|
| 🎨 | Güneş Takipli Panel | doğu-batı takip, gece düz yatar; 20 FE/t tepe, hava ve güneş açısına bağlı |
| ⬜ | Yanmalı Jeneratör, Batarya Bankası, Bakır / Yüksek Gerilim Kablosu | |
| ⬜ | Kırıcı, Elektrik Ark Fırını, Pres, Petrol Pompası (çok bloklu), Rafineri (çok bloklu) | |
| ⬜ | Elektroliz Ünitesi, Kriyojenik Sıvılaştırıcı, tanklar, sıvı/gaz borusu | |
| ⬜ | Parça Üretim Tezgâhı, Roket Montaj Tezgâhı (3B önizleme, Δv, itki/ağırlık) | |
| 🎨 | Oksijen Dolum İstasyonu | tüpü beşiğe koy; solunabilir havadan FE ile doldurur (Manevra Ünitesi gazı da) |
| 🎨 | Oksijen Dağıtıcı | kapalı odayı tick'lere yayılmış taşkın dolumla bulur (en çok 4096 blok), tüpten oksijen verir; blok değişince yeniden kontrol |
| 🎨 | Hava Kilidi Kapısı | 2 blok, kayar kanatlar (animasyonlu); kapalıyken hava geçirmez; el ya da kızıltaşla açılır |

### Paket ③ Roket, rampa, giysi ve Ay
| Durum | İçerik | Not |
|---|---|---|
| ⬜ | Fırlatma tesisi: rampa, alev kanalı, servis kulesi + erişim kolu, yakıt hattı, kontrol konsolu, uyarı ışığı | |
| ⬜ | Roket parçaları: burun konisi, kapsül, ısı kalkanı, paraşüt, tanklar, deniz seviyesi + vakum motoru, kademe ayırıcı, ara kademe, kanatçıklar, iniş bacakları, manevra iticileri | |
| 🎨 | EVA Giysisi Mk1: Kask, Gövde, Pantolon, Bot | özel 3B modeller: yuvarlak kask, sarmal füme vizör, altın güneş vizörü (eğil + kullan), parlayan yan lambalar; göğüs kontrol kutusu, yaşam destek sırt çantası + oksijen şişeleri; mavi yatak halkaları; tabanlı botlar. Yalnızca dört parça birlikte sızdırmaz |
| 🎨 | Manyetik EVA Botları | düşük/sıfır yerçekiminde zemine yapıştırır (eğilince bırakır) |
| 🎨 | Manevra Ünitesi | gövdeye takılır; soğuk gazla uçuş (sıfır g'de 6 eksen, Ay'da havada kalma) |
| 🎨 | Oksijen Tüpü (5 dk), Büyük Oksijen Tüpü (15 dk) | envanterde 2B, elde 3B; gövdenin sırt çantasına 2 tüp takılır |
| 🎨 | Kask HUD'u | GUI katmanında (shader güvenli): O2, giysi, vücut ısısı, dış sıcaklık, radyasyon, basınç/g, uyarılar; ölçek/opaklık ayarı |
| 🎨 | Hayatta kalma | boşlukta nefes tutma ve boğulma, yavaş değişen vücut ısısı (hipotermi/aşırı ısınma), radyasyon dozu, gerçek yerçekimi (zıplama ve düşme hasarı ölçekli) |
| ⬜ | kask lambası, çevre sensörü, titanyum aletler, elektrikli matkap, Giysi Mk2+ | |
| 🎨 | Ay Regoliti | **kalıcı ayak izi**, sol/sağ sırayla |
| 🎨 | Mare Bazaltı, İlmenit Cevheri | |
| ⬜ | Anortozit (+ türevler), Darbe Breşi, Turuncu Volkanik Cam Toprağı, Buzlu Regolit, Ay Buzu, Sinterlenmiş Regolit Tuğlası, Meteor Demiri | |
| ⬜ | Üs blokları: alüminyum paneller, titanyum levha, ızgara zemin, lombar, basınçlı cam, kayar kapı, uyarı şeridi, LED/panel lamba/projektör, kablo ve boru kanalları, merdiven/korkuluk/platform | |

---

## Yaratıklar (gezegenlere özgü, kan yok)
| Gök cismi | Yaratıklar |
|---|---|
| Ay | Regolit Kenesi (tozun altına saklanır, düşük yerçekiminde sıçrar), Toz Yüzücüsü (regolitin hemen altında süzülen pasif canlı) |
| Mars | Pas Böceği (demir oksit yer, tarafsız), Toz Şeytanı (fırtınalarda canlı hortum), Kanyon Pusucusu, Taşyiyen (iri, pasif otlayıcı), Kutup Kurtçuğu |
| Venüs (bulutlar) | Asit Vatozu (süzülen, tarafsız), Bulut Medüzü (pasif gaz kesesi) |
| Merkür | Alacakaranlık Sürüngeni (gece-gündüz sınırında yaşar) |
| Io | Kükürt Yengeci |
| Europa | Buzaltı Işık Yılanı (biyolüminesan, pasif) |
| Titan | Metan Kayıcısı, Tolin Solucanı |
| Asteroitler | Kaya Akarı |
| Ötegezegenler | prosedürel bitki ve hayvanlar (Faz 5) |

## Sonraki fazlar (özet)
- **Faz 2 – İç Sistem:** Mars blokları (regolit, kum, bazalt, olivin kumu, kil, hematit kürecikli toprak, perkloratlı toprak, kuru buz, su buzu), Phobos/Deimos, Venüs bazaltı ve bulut platformları, Merkür grafitli regolit; Sabatier reaktörü, atmosfer işlemcisi, regolit ısıtıcı, araştırma laboratuvarı, teleskop, radyo anteni; metan ve hidrojen motorları, 3 kişilik kapsül, uydular, sondalar, iyon motoru, istasyon modülleri, gezici, Giysi Mk2.
- **Faz 3 – Dış Sistem:** asteroit kayaları ve platin grubu, Io kükürtleri, Europa buzu ve okyanusu, Titan toliniyle metan gölleri, Enceladus/Triton/Plüton buzları; uraninit, galenit → kurşun, monazit → neodimyum; nükleer termal motor, RTG, gaz toplayıcı, füzyon, Giysi Mk3–Mk4.
- **Faz 4 – Yıldızlararası:** warp çekirdeği, uzay teleskobu, spektrometre, prosedürel blok aileleri.
- **Faz 5 – Yaşam**, **Faz 6 – Galaksiler**, **Faz 7 – Cila ve v1.0**, **Faz 8 – 26.2 portu**.
