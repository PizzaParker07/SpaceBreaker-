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
| ⬜ | Oksijen Dolum İstasyonu, Oksijen Dağıtıcı, Hava Kilidi | |

### Paket ③ Roket, rampa, giysi ve Ay
| Durum | İçerik | Not |
|---|---|---|
| ⬜ | Fırlatma tesisi: rampa, alev kanalı, servis kulesi + erişim kolu, yakıt hattı, kontrol konsolu, uyarı ışığı | |
| ⬜ | Roket parçaları: burun konisi, kapsül, ısı kalkanı, paraşüt, tanklar, deniz seviyesi + vakum motoru, kademe ayırıcı, ara kademe, kanatçıklar, iniş bacakları, manevra iticileri | |
| 🎨 | EVA Kaskı | özel 3B model, saydam vizör, altın güneş vizörü (eğil + kullan), parlayan lambalar |
| ⬜ | EVA giysisi gövde/pantolon/bot, manevra ünitesi, kask lambası, çevre sensörü, titanyum aletler, elektrikli matkap | |
| 🎨 | Oksijen Tüpü | envanterde 2B, elde 3B; 5 dakikalık oksijen |
| 🎨 | Ay Regoliti, Mare Regoliti, Volkanik Cam Toprağı, Buzlu Regolit | hepsi **kalıcı ayak izi** tutar, sol/sağ sırayla |
| 🎨 | Mare Bazaltı, İlmenit Cevheri (→ Ham İlmenit), KREEP Cevheri (→ KREEP Parçası: nadir toprak + toryum) | |
| 🎨 | Anortozit (+ merdiven, basamak, duvar), Cilalı Anortozit (+ merdiven, basamak), Anortozit Tuğla (+ merdiven, basamak, duvar) | taş kesici tarifleri |
| 🎨 | Darbe Breşi, Ay Buzu, Meteor Demiri (→ 4 ham demir) | |
| 🎨 | Sinterlenmiş Regolit (regolit fırında), Sinterlenmiş Regolit Tuğla (+ merdiven, basamak, duvar) | ilk Ay üssü yapı malzemesi |

### Gezegen motoru ve Ay boyutu
| Durum | İçerik | Not |
|---|---|---|
| 🎨 | Gezegen arazi üreticisi (`spacebreaker:planet`) | veri paketi profili (`spacebreaker/terrain/*.json`): yaylalar, mare havzaları ve kırışık sırtlar, gerçek krater morfolojisi (çanak, yükseltilmiş kenar, ejekta örtüsü, büyük kraterlerde düz taban + teras + merkezi tepe), genç kraterlerde parlak ışınlar, ejekta kayaları, kutuplarda kalıcı gölgeli buzlu krater tabanları; z ekseni = enlem; dünya tohumuna göre belirlenimci |
| 🎨 | Ay boyutu `spacebreaker:moon` | 5 biyom: Ay Yaylaları, Ay Denizi (Mare), Büyük Krater, Piroklastik Ovalar, Ay Kutbu; kayalar, cevher ve buz damarları, meteorit parçaları |
| 🎨 | Gök cismi zamanı | gün uzunluğu gerçek değerden ölçeklenir (ayar), her boyutun kendi saati; `/spacebreaker localtime` |
| 🎨 | Vakum gökyüzü | siyah gök, yoğun yıldız alanı + Samanyolu, mesafeye göre boyutlanan keskin Güneş, Ay'da sabit duran ve **doğru evresiyle** dönen Dünya; gece Dünya ışığı; bulut/yağmur/pus yok |

### Ay zindanları
| Durum | İçerik | Not |
|---|---|---|
| 🎨 | Terk Edilmiş Keşif Karakolu | merkez modül, laboratuvar, depo, drone hangarı, hava kilidi (iki demir kapı), güneş paneli tarlası, iniş pisti, anten; muhafız: Keşif Dronları (+ çağırıcı) |
| 🎨 | Antik Lav Tüpü | çökmüş tavan girişi, kıvrımlı tünel, düşmüş eski sonda enkazı (sandık), Yaşlı Regolit Kenesi yuvası |
| 🎨 | Ganimet | `chests/moon/tech_data` araştırma diskleri için kanca tablosu (Araştırma alanı `spacebreaker:tech_disk` ekler) |
| ⬜ | Üs blokları: alüminyum paneller, titanyum levha, ızgara zemin, lombar, basınçlı cam, kayar kapı, uyarı şeridi, LED/panel lamba/projektör, kablo ve boru kanalları, merdiven/korkuluk/platform | |

---

## Yaratıklar (gezegenlere özgü, kan yok)
| Gök cismi | Yaratıklar |
|---|---|
| Ay 🎨 | Regolit Kenesi (tozun altına saklanır, düşük yerçekiminde sıçrar), Yaşlı Regolit Kenesi (lav tüpü muhafızı), Toz Yüzücüsü (regolitin hemen altında süzülen pasif canlı, arkasında toz dalgaları), Keşif Dronu (karakol muhafızı, sersemletme atışı) |
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
