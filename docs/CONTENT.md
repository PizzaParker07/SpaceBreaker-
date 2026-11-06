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
| 🎨 | Boksit → Alüminyum (cevher ×2, ham, ham blok, külçe, parça, blok, levha, toz, tel) | sıcak biyomlarda, yüzeye yakın |
| 🎨 | Rutil → Titanyum, Pentlandit → Nikel, Spodümen → Lityum, Wolframit → Tungsten | her biri: cevher ×2, ham, ham blok, külçe, parça, blok, levha, toz (+ tel/çubuk); rutil derin ve nadir, pentlandit orta derinlik, spodümen dağlarda, wolframit derin |
| 🎨 | Demir / bakır / altın için levha, toz, tel, çubuk | |
| 🎨 | Ham petrol cepleri + yüzey sızıntıları (Petrol Sızıntısı bloğu) | ceplerin tam üstünde katran lekeli zemin |
| 🎨 | Çelik, Al-Li alaşımı, nikel süper alaşımı, plastik levha, silisyum, silisyum plaka, güneş hücresi, temel/gelişmiş devre kartı, elektrik motoru, ablatif kalkan levhası, altın folyo, turuncu köpük, silikon conta, levha/tel/çubuk pres kalıpları, kömür tozu | |
| 🎨 | Sıvılar: ham petrol, RP-1, sıvı oksijen, sıvı hidrojen, sıvı metan, oksijen, hidrojen, metan | `c:` etiketleri, kovalar, Kriyojenik Kap; kriyojenik sıvılar açıkta kaynayıp uçar |

### Paket ② Enerji ve makineler
| Durum | İçerik | Not |
|---|---|---|
| 🎨 | Güneş Takipli Panel | doğu-batı takip, gece düz yatar; 20 FE/t tepe, hava ve güneş açısına bağlı |
| 🎨 | Oksijen Dolum İstasyonu | tüpü beşiğe koy; solunabilir havadan FE ile doldurur (Manevra Ünitesi gazı da) |
| 🎨 | Oksijen Dağıtıcı | kapalı odayı tick'lere yayılmış taşkın dolumla bulur (en çok 4096 blok), tüpten oksijen verir; blok değişince yeniden kontrol |
| 🎨 | Hava Kilidi Kapısı | 2 blok, kayar kanatlar (animasyonlu); kapalıyken hava geçirmez; el ya da kızıltaşla açılır |
| 🎨 | Yanmalı Jeneratör, Batarya Bankası, Bakır / Yüksek Gerilim Kablosu | jeneratör yalnız oksijenli havada yanar; batarya önden verir, şarj göstergesi |
| 🎨 | Kırıcı, Elektrik Ark Fırını, Pres (hareketli koç), Petrol Pompası (çok bloklu, animasyonlu), Rafineri (çok bloklu damıtma kulesi) | |
| 🎨 | Elektroliz Ünitesi, Kriyojenik Sıvılaştırıcı, Sıvı / Basınçlı Gaz / Kriyojenik Tank (dolum görünür), sıvı/gaz borusu (uçtan çekme modu) | |
| 🎨 | Parça Üretim Tezgâhı | `spacebreaker:fabricating` tarifi; Roket ve Giysi alanları kendi tariflerini ekler |
| 🎨 | Yerinde üretim (ISRU): Atmosfer İşlemcisi, Sabatier Reaktörü, Regolit Isıtıcı, Buz Eritici | atmosferden gaz (Mars/Venüs CO₂, Dünya O₂, Titan CH₄); CO₂ + H₂ → metan + su; regolit ve perklorattan O₂; buzdan su, kuru buzdan CO₂; `c:regoliths`, `c:ices`, `c:dry_ices`, `c:perchlorate_soils` etiketleriyle beslenir |
| 🎨 | Karbondioksit gazı (`c:carbon_dioxide`); güneş paneli verimi gezegenin güneşe uzaklığına göre (en fazla 3×) | |
| 🎨 | Kimyasal Reaktör (`spacebreaker:chemical_reaction`), Sülfürik Asit (`c:sulfuric_acid`), Helyum-3 (`c:helium_3`) | kükürt + su + oksijen → sülfürik asit; Merkür güneş rüzgârı regolitinden (`c:regoliths/solar_wind`) hidrojen + He-3 |
| 🎨 | Roket Montaj Tezgâhı | 3 kademe + faydalı yük yuvası, canlı 3B önizleme, kademe başına Δv ve itki/ağırlık, ulaşılabilir hedefler (yörünge, Ay, iniş, dönüş) |

### Paket ③ Roket, rampa, giysi ve Ay
| Durum | İçerik | Not |
|---|---|---|
| 🎨 | Fırlatma tesisi: rampa kaplaması, alev kanalı ızgarası, servis kulesi, erişim kolu (geri sayımda katlanır), yakıt hattı (c:rp1 / c:liquid_oxygen), kontrol konsolu | çok bloklu; boş elle tıklayınca kontrol listesi + eksik bloklar alevle işaretlenir |
| ⬜ | Uyarı ışığı | |
| 🎨 | Roket parçaları: burun konisi, kapsül, ısı kalkanı, paraşüt, küçük/orta/büyük tank, deniz seviyesi + vakum kerosen motoru, kademe ayırıcı, ara kademe, kanatçık, ızgara kanatçık, iniş bacakları, manevra iticileri | veriyle tanımlı (`rocket_part/*.json`), gerçekçi kütle/itki/Isp; envanterde 2B, elde ve rokette 3B |
| 🎨 | Roket, yörünge boyutları (Dünya / Ay yörüngesi + diğer cisimler için ortak yörünge), yıldız haritası (gök cismi verisinden: Merkür, Venüs, Mars, Phobos, Deimos ve dış sistem kendiliğinden) | geri sayım, ateşleme, kademe ayrılması, gökyüzünün kararması, yörüngede dönen gezegen, Ay'a motorlu iniş + toz, atmosfere giriş (plazma), paraşüt, suya iniş |
| 🎨 | Acil Dönüş İşareti, Yaratıcı Yakıt Kartuşu | mahsur kalmama; test için tam yakıt |
| 🎨 | EVA Giysisi Mk1: Kask, Gövde, Pantolon, Bot | özel 3B modeller: yuvarlak kask, sarmal füme vizör, altın güneş vizörü (eğil + kullan), parlayan yan lambalar; göğüs kontrol kutusu, yaşam destek sırt çantası + oksijen şişeleri; mavi yatak halkaları; tabanlı botlar. Yalnızca dört parça birlikte sızdırmaz |
| 🎨 | Termal Giysi (Mk2) | gümüş alüminize kumaş, altın vizör siperliği, sırtta altın radyatör kanatları; 500 °C'ye kadar (Venüs, Merkür gündüzü) |
| 🎨 | Isıtmalı Giysi (Mk2) | turuncu dolgu yalıtım, kalın yaka, kabarık omuz/bacak, ısıtıcı batarya; −250 °C'ye kadar (Titan) |
| 🎨 | Radyasyon Zırhlı Giysi (Mk3) | kurşun grisi + sarı zırh plakaları, dar vizör yarığı, sırt kalkanı; dozun yalnız %4'ü geçer (Io, Europa) |
| 🎨 | Giysi yükseltmeleri | demirci masası: Giysi Yükseltme Şablonu + parça + modül (termal regülatör, radyasyon kalkanı, ek oksijen, jetpack güçlendirici) |
| 🎨 | Boyanabilir aksan | tüm giysi parçalarının halka/aksan rengi boyayla değişir |
| 🎨 | Manyetik EVA Botları | düşük/sıfır yerçekiminde zemine yapıştırır (eğilince bırakır) |
| 🎨 | Manevra Ünitesi | gövdeye takılır; soğuk gazla uçuş (sıfır g'de 6 eksen, Ay'da havada kalma) |
| 🎨 | Oksijen Tüpü (5 dk), Büyük Oksijen Tüpü (15 dk) | envanterde 2B, elde 3B; gövdenin sırt çantasına 2 tüp takılır |
| 🎨 | Kask HUD'u | GUI katmanında (shader güvenli): O2, giysi, vücut ısısı, dış sıcaklık, radyasyon, basınç/g, uyarılar; ölçek/opaklık ayarı |
| 🎨 | Hayatta kalma | boşlukta nefes tutma ve boğulma, yavaş değişen vücut ısısı (hipotermi/aşırı ısınma), radyasyon dozu, gerçek yerçekimi (zıplama ve düşme hasarı ölçekli) |
| ⬜ | kask lambası, çevre sensörü, titanyum aletler, elektrikli matkap, Giysi Mk2+ | |
| 🎨 | Mare Bazaltı, İlmenit Cevheri | |
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

### Paket ④ Araştırma, zindanlar ve rehber
| Durum | İçerik | Not |
|---|---|---|
| 🎨 | Teknoloji ağacı (Faz 1–3, 61 + diğer alanların 4 + Faz 2/3 dünyaları için örnek türleri) | itki, yaşam desteği, sanayi, astronomi, inşaat, keşif dalları; ön koşullar dallar arası |
| 🎨 | Teknoloji Diski | dal rengine göre etiket; zindanlardan çıkar, laboratuvarda okunur, okunduktan sonra geri verilir |
| 🎨 | Saha Numunesi, Deney Kaydı, Numune Çantası | her dünyanın numunesi farklı teknolojileri ortaya çıkarır |
| 🎨 | Araştırma Laboratuvarı | tezgâh + analiz cihazı + mikroskop; FE ile çalışır, çalışırken ekran parlar; takımla paylaşım |
| 🎨 | Araştırma ağacı ekranı (J) | dallar, ön koşul çizgileri, ipuçları, sürükle/yakınlaştır |
| 🎨 | Saha Rehberi | iki sayfalık kitap: 12 bölüm, 47 sayfa (malzemeler, makineler, giysi, roketçilik, Ay, Mars, Venüs, Merkür, zindanlar, fizik), arama, tarifler, çizimler, teknolojiyle açılan sayfalar (TR + EN) |
| 🎨 | Terk Edilmiş Gözlemevi (Dünya zindanı) | bakır kubbe, kırık teleskop, gizli arşiv (ilk roketçilik diskleri), roket test rampası, muhafızlar |
| 🎨 | Ana başarım ağacı (28) | cevher → külçe → çelik → RP-1 → sıvı oksijen; numune → laboratuvar → teknolojiler; zindan → roket → yörünge → Ay yörüngesi ve Ay → Ay zindanı, kene, dönüş; tam giysi, manevra ünitesi |

---

## Gök cisimleri ve astrofizik (veri)
| Durum | İçerik | Not |
|---|---|---|
| ✅ | Güneş Sistemi verisi: Güneş, Merkür, Venüs, Dünya, Ay, Mars, Phobos, Deimos, Ceres, Vesta, Jüpiter, Io, Europa, Ganymede, Callisto, Satürn, Titan, Enceladus, Uranüs, Miranda, Neptün, Triton, Plüton, Charon | NASA/JPL değerleri; gaz/buz devlerine iniş yok |
| ✅ | Astrofizik kütüphanesi (`core/astro`) | yörünge mekaniği, transferler, roket denklemi, yıldız/gezegen modelleri; bkz. [ASTRO.md](ASTRO.md) |
| ✅ | Galaksi temeli: tohumlu Samanyolu, sektörler, prosedürel yıldız sistemleri | gerçek yakın yıldızlar: Alfa Centauri A/B, Proxima b, Barnard b, Sirius A/B, Epsilon Eridani b, Tau Ceti, TRAPPIST-1 b–h |
| ✅ | Güneş Sistemi Δv haritası | [ASTRO.md](ASTRO.md#δv-map-of-the-solar-system) |

## Faz 2 — İç gezegenler (Mars, Phobos, Deimos, Venüs, Merkür)

### Gezegen motoru eklentileri
| Durum | İçerik | Not |
|---|---|---|
| 🎨 | Yer şekilleri (`landforms`, isteğe bağlı) | kumul alanları (rüzgâr yönlü asimetrik sırtlar), büyük yarık kanyonu (taraçalı duvarlar, heyelan tümsekleri), kalkan volkanlar (kaldera, ışınsal lav akıntıları, taban yarı), eski nehir vadileri ve deltalar, katmanlı tortul kayalar, kutup buz örtüsü (oluklar, kırağı), toprak lekeleri, loblu sarplıklar, oluk zincirleri, tessera, zirve kırağısı |
| 🎨 | Ortam kancaları | güneş ışığı (toz fırtınası, bulutlar) ve basınç (yükseklik) çarpanları; güneş paneli fırtınada düşer |

### Mars `spacebreaker:mars`
| Durum | İçerik | Not |
|---|---|---|
| 🎨 | 9 biyom | Kraterli Yaylalar, Kuzey Ovaları, Mars Krateri, Kutup Katmanlı Tortulları, Kumul Denizi, Büyük Yarık Kanyonu, Kalkan Volkan, Antik Nehir Vadisi, Kutup Buz Örtüsü |
| 🎨 | Topraklar | Mars Regoliti, Hematit Kürecikli Toprak, Perkloratlı Toprak, Mars Kili, CO₂ Kırağısı — ayak izi tutar, rüzgâr zamanla siler (fırtınada hızlı) |
| 🎨 | Kumlar, kayalar, buzlar | Mars Kumu (koyu bazalt kumu), Olivin Kumu, Mars Bazaltı / Cilalı / Tuğla aileleri, Katmanlı Kumtaşı ailesi, Mars Çamurtaşı, Alçıtaşı Bloğu, Mars Su Buzu, Tozlu Buz, Kuru Buz (sıcakta süblimleşir) |
| 🎨 | Cevherler | Hematit (→ demir), Alçıtaşı Damarı (→ kristal, Taşyiyen yemi); Mars'ta da meteor demiri |
| 🎨 | Gökyüzü | karamela rengi gün, gün batımında mavi hale, küçük Güneş, kendi saat açılarıyla Phobos (batıdan doğar) ve Deimos |
| 🎨 | Toz fırtınaları | seviye başına kayıtlı, sise ve GUI katmanında toz tonu (shader güvenli), uçuşan toz, %85 daha az güneş, kutuplar daha soğuk; `/spacebreaker duststorm` |
| 🎨 | Zindanlar | Terk Edilmiş Mars Araştırma İstasyonu (sera, ISRU tesisi, garaj, sondaj kulesi), Antik Gömülü Nehir Yatağı (katmanlı mağara, kil yatak, düşmüş sonda) |
| 🎨 | Lav tüpleri | volkan ve ova altında doğal tüneller (bazalt kabuk, cilalı lav astarı, olivin kumlu taban), çökmüş ışıklıklardan girilir; 5+ blok tavan radyasyonu 1/32'nin altına düşürür: hazır üs yeri |
| 🎨 | Kutup katmanlı tortuları | temiz buz arasında düzensiz aralıklı ince toz bantları (12 blokta 1–3), kutup oluklarının duvarlarında görünür |
| 🎨 | Endüstri girdileri | kırıcıda: Perkloratlı Toprak → Perklorat Tuzu (`c:dusts/perchlorate`, oksijen kaynağı); Mars/tozlu/krater buzu → buz (`c:ices/water`, eriyip elektroliz suyu olur) |

### Phobos ve Deimos
| Durum | İçerik | Not |
|---|---|---|
| 🎨 | Boyutlar `spacebreaker:phobos`, `spacebreaker:deimos` | çok küçük, havasız; Phobos'ta oluk zincirleri; gökte dev Mars (≈40° / 16°), evresiyle |
| 🎨 | Bloklar | Phobos Regoliti, Deimos Regoliti (kalıcı ayak izi), Karbonlu Kondrit |
| 🎨 | Zindanlar | Düşmüş Numune Dönüş Sondası (Phobos), Deimos Buz Deposu |

### Venüs `spacebreaker:venus`
| Durum | İçerik | Not |
|---|---|---|
| 🎨 | Yüzey | Volkanik Ovalar, Tessera Yaylaları, kalkan volkanlar, kraterler; 464 °C, 92 bar, loş turuncu pus |
| 🎨 | Bulut katmanı (y 195–245) | ~1 bar, oda sıcaklığı; asit bulutları kaplamasız zırhı aşındırır; üstünde bulut denizi, Güneş batıdan doğar |
| 🎨 | Bloklar ve eşyalar | Venüs Bazaltı, Kavrulmuş Çakıl, Tessera Kayası, Kükürt Kabuğu (→ kükürt), Metal Kırağısı; Hafif Kompozit Panel, Aerojel, Balon Kumaşı; Floropolimer Kaplama (zırh + kaplama → aside dayanıklı zırh) |
| 🎨 | Zindan | Terk Edilmiş Araştırma Balonu (bulut tepelerinde, Asit Vatozu muhafızları) |

### Merkür `spacebreaker:mercury`
| Durum | İçerik | Not |
|---|---|---|
| 🎨 | Arazi | yoğun kraterler, loblu sarplıklar, düz ovalar, parlak oyuklar, kalıcı gölgeli kutup kraterlerinde buz; dev Güneş, 430 °C gündüz / −180 °C gece |
| 🎨 | Bloklar | Merkür Regoliti, Oyuk Tortusu (kalıcı ayak izi), Merkür Kabuğu, Düşük Yansıtmalı Kaya, Grafit Cevheri (→ grafit) |
| 🎨 | Zindan | Terk Edilmiş Kutup Buzu Madeni (gölgeli krater tabanının altında) |
| 🎨 | Endüstri girdileri | kırıcıda: Oyuk Tortusu → kükürt; Venüs Kükürt Kabuğu → kükürt |

### İç gezegen yörüngeleri `spacebreaker:venus_orbit`, `mercury_orbit`, `mars_orbit`
| Durum | İçerik | Not |
|---|---|---|
| 🎨 | Yörünge gökyüzü | dönen gezegen küresi (gece-gündüz sınırı), uzaklığa göre boyutlu Güneş; Venüs'te kalın parlak pus halkası, Mars'ta ince soluk halka, Merkür'de yok; uzakta Phobos/Deimos, Dünya, Merkür, Venüs |
| 🎨 | Haritalar | Venüs bulut bantları (yan yatmış Y), Merkür kraterleri + Caloris + ışınlı genç kraterler, Mars coğrafyası (Tharsis, Olympus, Valles + Noctis, kutup başlıkları); hepsi kodla, piksel stilinde |
| 🎨 | Rehber | yeni "İç Gezegenler" bölümü: Mars, Mars kaynakları, Mars'ta yaşam, Phobos ve Deimos, Venüs, bulutlarda yaşam ve harabeler, Merkür, yörüngeden görünüm (EN + TR, Gezegenlerarası Transferler ile açılır) |

### Faz 2 teknolojileri (zindan diskleri)
| Durum | İçerik | Not |
|---|---|---|
| 🎨 | Toz Geçirmez Contalar, Isıya Dayanıklı Elektronik, Aside Dayanıklı Kaplamalar, Kutup Buzu Madenciliği | ayrıca Mars/Venüs/Merkür zindanlarından metan motorları, Sabatier süreci, ISRU, Mars jeolojisi, astrobiyoloji, bulut platformları |

## Yaratıklar (gezegenlere özgü, kan yok)
| Gök cismi | Yaratıklar |
|---|---|
| Ay 🎨 | Regolit Kenesi (tozun altına saklanır, düşük yerçekiminde sıçrar), Yaşlı Regolit Kenesi (lav tüpü muhafızı), Toz Yüzücüsü (regolitin hemen altında süzülen pasif canlı, arkasında toz dalgaları), Keşif Dronu (karakol muhafızı, sersemletme atışı) |
| Mars 🎨 | Pas Böceği (demir oksit yer, tarafsız), Toz Şeytanı (fırtınalarda canlı hortum), Kanyon Pusucusu, Taşyiyen (iri, pasif otlayıcı), Kutup Kurtçuğu |
| Venüs (bulutlar) 🎨 | Asit Vatozu (süzülen, tarafsız), Bulut Medüzü (pasif gaz kesesi) |
| Merkür 🎨 | Alacakaranlık Sürüngeni (tehlikeli; gece-gündüz sınırında yaşar; güneş yükselince ya da gece çökünce toprağa gömülür), Oyuk Salyangozu (pasif, faydalı: oyuk tortusunu kazıyıp kükürt bırakır, kükürtle çoğalır) |
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
