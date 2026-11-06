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
| 🎨 | Oksijen Dolum İstasyonu | tüpü beşiğe koy; FE ile, gezegenin doğal solunabilir havasından (Dünya) ya da borudan gelen oksijen gazı / sıvı oksijenden doldurur (Manevra Ünitesi gazı da). Dağıtıcının oda havası sayılmaz |
| 🎨 | Oksijen Dağıtıcı | kapalı odayı tick'lere yayılmış taşkın dolumla bulur (en çok 4096 blok), tüpten ya da borudan gelen oksijenle önce odayı basınçlandırır (blok başına oksijen), sonra hacme göre tüketir; sızıntıda hava boşalır; blok değişince yeniden kontrol |
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
| 🎨 | Roket parçaları: burun konisi, kapsül, ısı kalkanı, paraşüt, küçük/orta/büyük tank, deniz seviyesi + vakum kerosen motoru, deniz seviyesi + vakum metaloks motoru (Metan Motorları teknolojisi; Mars'ta üretilen metan + LOX ile dönüş), kademe ayırıcı, ara kademe, kanatçık, ızgara kanatçık, iniş bacakları, manevra iticileri | veriyle tanımlı (`rocket_part/*.json`), gerçekçi kütle/itki/Isp; tanklar kademenin motorunun yakıtını (RP-1 ya da sıvı metan) alır; envanterde 2B, elde ve rokette 3B |
| 🎨 | Roket, yörünge boyutları (Dünya / Ay yörüngesi + diğer cisimler için ortak yörünge), yıldız haritası (gök cismi verisinden: Merkür, Venüs, Mars, Phobos, Deimos ve dış sistem kendiliğinden; Güneş solda, gezegene tıklayınca sistemine yakınlaşır, sağ panelde resim, kısa açıklama, tehlikeler, "Yörüngeye git / Yüzeye in", düz dille yakıt kontrolü, dönüş yakıtı, yolculuk süresi, fırlatma penceresi, adım adım rota) | binince roket kamerası (fareyle çevresinde dön, tekerlekle yakınlaş, F5 ile kokpit penceresi), boyut değişiminde yükleme ekranı yerine geçiş animasyonu (gök kararır → yıldızlar, yıldızlar akar → hedef büyür, gezegen yaklaşır → plazma), geri sayım, ateşleme, kademe ayrılması, gökyüzünün kararması, yörüngede dönen gezegen, Ay'a motorlu iniş + toz, atmosfere giriş (plazma), paraşüt, suya iniş |
| 🎨 | Acil Dönüş İşareti, Yaratıcı Yakıt Kartuşu | mahsur kalmama; test için tam yakıt |
| 🎨 | EVA Giysisi Mk1: Kask, Gövde, Pantolon, Bot | özel 3B modeller: yuvarlak kask, sarmal füme vizör, altın güneş vizörü (eğil + kullan), parlayan yan lambalar; göğüs kontrol kutusu, yaşam destek sırt çantası + oksijen şişeleri; mavi yatak halkaları; tabanlı botlar. Yalnızca dört parça birlikte sızdırmaz |
| 🎨 | Termal Giysi (Mk2) | gümüş alüminize kumaş, altın vizör siperliği, sırtta altın radyatör kanatları; 500 °C'ye kadar (Venüs, Merkür gündüzü) |
| 🎨 | Isıtmalı Giysi (Mk2) | turuncu dolgu yalıtım, kalın yaka, kabarık omuz/bacak, ısıtıcı batarya; −250 °C'ye kadar (Titan) |
| 🎨 | Radyasyon Zırhlı Giysi (Mk3) | kurşun grisi + sarı zırh plakaları, dar vizör yarığı, sırt kalkanı; dozun yalnız %4'ü geçer (Io, Europa) |
| 🎨 | Hologram Projektörü | çoklu blok yapıların nasıl kurulacağını gösterir: yapı seç, yere yansıt, eğil + tekerlek ile döndür, katman katman mod; eksikler camgöbeği hayalet, yanlış bloklar kırmızı çerçeve, ekranda malzeme listesi; denetleyiciye boş elle eğil + sağ tık eksikleri yansıtır. Yapılar `data/<ns>/spacebreaker/multiblock/*.json` |
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
| 🎨 | Terk Edilmiş Keşif Karakolu | merkez modül, laboratuvar, depo **veya** çökmüş sera, drone hangarı, mürettebat yatakhanesi, hava kilidi (iki demir kapı), güneş paneli tarlası, iniş pisti, anten; **gücü kesik kasa** (bulmaca: komuta sandığındaki güç hücresini kapının üstündeki yuvaya tak) ve basınç plakalı ok tuzağı; muhafız: Keşif Dronları (+ çağırıcı) |
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
| 🎨 | Cevherler | Hematit (→ demir), Alçıtaşı Damarı (→ kristal, Taş Otlayan yemi); Mars'ta da meteor demiri |
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

## Faz 3 — Dış Güneş Sistemi (asteroit kuşağı, Jüpiter ve Satürn uyduları)

### Ortak dünya motoru eklentileri
| Durum | İçerik | Not |
|---|---|---|
| 🎨 | Yüzey özellikleri (`content/belt/world`) | gezegen motorunun zeminini yığın başına biçimlendirir: krater tabanlarında tuz lekeleri (faculae), dev kutup çarpma havzası, çatlak buz çizgileri (lineae) ve kaos arazisi, sıvı gölleri, bacalar/gayzerler |
| 🎨 | Küçük gök cismi arazi üreticisi (`SmallBodyTerrain`) | yarıçap ve yüzey yaşından arazi profili; Ceres ve Vesta bununla yapılır, prosedürel asteroitler için hazır |

### Ceres `spacebreaker:ceres`
| Durum | İçerik | Not |
|---|---|---|
| 🎨 | Arazi | koyu, kraterli ovalar; Occator benzeri kraterlerde parlak sodyum karbonat lekeleri (merkez + loblu tuz lekeleri), kırık iç duvar ve duvardan düşmüş kayalar, Ahuna benzeri kriyovolkanik kubbeler, kutuplarda ve birkaç blok altta su buzu; 0,03 g |
| 🎨 | Bloklar | Ceres Regoliti, Amonyaklı Kil, Karbonat Tuz Kabuğu (kalıcı ayak izi), Ceres Kayası (+ merdiven, basamak, duvar), Ceres Buzu, Manyetit Cevheri (→ Ham Manyetit → demir) |
| 🎨 | Yaratık | Kaya Akarı: yassı, üst üste binen karbonat kabuklu plakalardan bir gövde, 6 kısa bacak; çakılın içinde gizlenir; birine vurulunca bütün küme uyanır ve düşük yerçekiminde uzağa sıçrar |
| 🎨 | Zindan | Terk Edilmiş Buz Madenciliği Karakolu: mürettebat modülü, buz işleme salonu, iniş pisti, anten, kuyu kulesi; aşağıda buz merceğine açılmış galeri ve akar yuvası |
| 🎨 | Teknolojiler | Mikro Yerçekiminde Madencilik (4), Tuzlu Su Kimyası (3) |

### Vesta `spacebreaker:vesta`
| Durum | İçerik | Not |
|---|---|---|
| 🎨 | Arazi | gri-bej, yoğun kraterli yaylalar; okra diyojenitli fırlatma örtüsü ve koyu karbonlu madde lekeleri; güney kutbunda dev Rheasilvia havzası (ökrit/diyojenit katmanlı taraçalı duvar, halka sarplıklar, çizgili dev merkez dağı, okra taban) |
| 🎨 | Bloklar | Vesta Regoliti, Diyojenitli Regolit, Ökrit (+ merdiven, basamak, duvar), Ökrit Tuğla (+ merdiven, basamak, duvar), Diyojenit, Kromit Cevheri (→ Kromit) |

### Vesta zindanı ve diğer küçük dünyalar
| Durum | İçerik | Not |
|---|---|---|
| 🎨 | Düşmüş İkmal İniş Aracı (Vesta, Ganymede, Kallisto, Enceladus) | kayma hendeği ve toprak sırtı, yan yatmış iniş aracı (kırık bacak, ısı kalkanı, kabin sandığı), kopmuş güneş paneli, enkaz, merdivenli gömülü kargo bölmesi; her dünyanın kendi zemini, kargosu ve teknolojisi |

### Jüpiter uyduları
| Durum | İçerik | Not |
|---|---|---|
| 🎨 | Gökyüzü | Jüpiter gerçek boyutunda ve evresiyle sabit durur (Io'dan 19°, Europa'dan 12°, Ganymede'den 7,5°, Kallisto'dan 4°); bantlı piksel disk, Büyük Kırmızı Leke, beyaz ovaller |
| 🎨 | Europa `spacebreaker:europa` | genç buz kabuğu: çift sırtlı lekeli çatlaklar (lineae), kaos arazisi (eğik buz salları, donmuş tuzlu su), buz gayzerleri, az krater; kabuğun altında gömülü okyanus katmanı (ileride okyanus erişimi için kanca); günde ~5,4 Sv radyasyon - tepedeki her blok yarıya indirir |
| 🎨 | Europa blokları | Europa Kırağısı (kalıcı ayak izi), Europa Buzu, Europa Buzu Tuğla (+ merdiven, basamak, duvar), Lekeli Buz, Yeniden Donmuş Tuzlu Su, Tuz Cevheri (→ Okyanus Tuzu), Buz Gayzeri (döngüyle püskürür, üstündekini fırlatır) |
| 🎨 | Io `spacebreaker:io` | krater yok; kükürt ovaları (sarı, beyaz SO₂ kırağısı, kırmızı kükürt), dağlar, lav gölleri (paterae), kırmızı halkalı kükürt bulutu bacaları (yakar), lav yakınında ısı |
| 🎨 | Io blokları | Kükürt Kumu, Kükürt Dioksit Kırağısı, Kırmızı Kükürt (kalıcı ayak izi), Io Bazaltı (+ merdiven, basamak, duvar), Io Kükürt Cevheri (→ kükürt), Kükürt Bulutu Bacası |
| 🎨 | Ganymede `spacebreaker:ganymede` | parlak oluklu buz ovaları ile koyu kadim kraterli arazi, parlak kutup örtüleri; Ganymede Regoliti, Oluklu Buz, Koyu Buz Kabuğu |
| 🎨 | Kallisto `spacebreaker:callisto` | Güneş Sistemi'nin en kraterli yüzeyi, parlak kırağı tümsekleri, Valhalla çok halkalı havzası; düşük radyasyon - güvenli üs yeri; Kallisto Regoliti, Parlak Kırağı, Kallisto Kayası (+ merdiven, basamak, duvar) |
| 🎨 | Zindanlar | Gömülü Araştırma Sondası (Europa: iniş aracı, eritme kuyusu, tuzlu su havuzlu buz mağarası), Terk Edilmiş Volkan Gözlemevi (Io: bazalt sığınak, teleskop, sismometre kasası) |
| 🎨 | Teknolojiler | Kriyobot Sondajı (4), Radyasyona Dayanıklı Elektronik (4), Volkanik Isı Kalkanı (4), Manyetosfer Fiziği (4), Derin Uzay Yaşam Alanları (4) |

### Satürn uyduları
| Durum | İçerik | Not |
|---|---|---|
| 🎨 | Titan `spacebreaker:titan` | 1,5 bar azot, oksijen yok, -179 °C; turuncu pus (yoğun sis, yıldızsız gök, soluk güneş), hidrokarbon kumul denizi, organik ovalar, kutuplarda metan gölleri (kaynamayan sıvı metan), metan sağanakları (iri, yavaş düşen soluk amber damlalar); yoğun havada yumuşak düşüş ve uzun süzülme (elytra kaldırma kuvveti kazanır) |
| 🎨 | Titan blokları | Tolin Kumu, Tolin Çamuru (kalıcı ayak izi), Titan Ana Kaya Buzu (+ merdiven, basamak, duvar), Buz Çakılları, Tolin Yatağı (→ Tolin) |
| 🎨 | Enceladus `spacebreaker:enceladus` | en parlak buz; güney kutbunda kaplan çizgisi yarıkları ve gayzer alanı; gökte 28° genişliğinde, halkaları neredeyse yandan görünen Satürn (halka gölgesiyle) |
| 🎨 | Enceladus blokları | Enceladus Karı (kalıcı ayak izi), Enceladus Buzu, Kaplan Çizgisi Buzu (hafif parlar) |
| 🎨 | Zindan | Terk Edilmiş Döner Kanat Hangarı (Titan: tonozlu hangar, sekiz rotorlu uçan araç, atölye, laboratuvar, metan deposu) |
| 🎨 | Teknolojiler | Hava Hareketliliği (4), Hidrokarbon İşleme (4), Gayzer Örneklemesi (5) |

### Neptün sistemi
| Durum | İçerik | Not |
|---|---|---|
| 🎨 | Triton `spacebreaker:triton` | ters yönde dönen, yakalanmış cüce gezegen; -235 °C (ölçülen en soğuk yüzey); yuvarlak çukurlar ve keskin sırtlardan "kavun kabuğu" arazisi, pembe azot kırağısından kutup başlıkları, az krater; güney başlığında koyu azot-toz sütunları fışkırtıp rüzgârla yatık çizgiler bırakan azot gayzerleri (yakmaz, fırlatır); gökte 8° genişliğinde mavi Neptün |
| 🎨 | Triton blokları | Triton Kırağısı, Pembe Azot Kırağısı, Gayzer Tozu (kalıcı ayak izi), Triton Ana Kaya Buzu (+ merdiven, basamak, duvar), Azot Gayzeri, Metan Klatrat Yatağı (→ Metan Klatrat, kömür kadar yanan yakıt) |
| 🎨 | Zindan | Düşmüş İkmal İniş Aracı (Triton) |
| 🎨 | Teknoloji | Kriyovolkanizma (5) |

### Plüton ve Charon
| Durum | İçerik | Not |
|---|---|---|
| 🎨 | Plüton `spacebreaker:pluto` | -230 °C, çok ince azot havası; kalp biçimli dev azot buzu ovası (sığ, düz tabanlı havza) ve üstünde yavaşça dönen konveksiyon hücreleri (çukurlarında kızıl tolin birikintisi); ovanın batı kıyısında su buzu dağları; ekvatorda koyu kızıl tolin ovaları, kutuplarda metan kırağısı; gökte kıpırdamadan duran Charon (3,5°), ufkun üstünde mavi pus katmanları |
| 🎨 | Plüton blokları | Plüton Kırağısı, Kızıl Tolin Regoliti (kalıcı ayak izi), Azot Buzu (çok kaygan), Metan Kırağısı, Plüton Ana Kaya Buzu (+ merdiven, basamak, duvar), Plüton Tolin Yatağı (→ Tolin) |
| 🎨 | Charon `spacebreaker:charon` | gri su buzu ovaları, ekvator boyunca kanyon kuşağı, kuzey kutbunda koyu kızıl kutup başlığı; gökte 7° genişliğinde, hiç kıpırdamayan Plüton |
| 🎨 | Charon blokları | Charon Regoliti, Charon Buzu, Kızıl Kutup Kırağısı, Amonyak Hidrat Yatağı (→ Amonyak Kristali → kemik tozu) |
| 🎨 | Zindanlar | Düşmüş İkmal İniş Aracı (Plüton, Charon) |
| 🎨 | Teknolojiler | Azot Buzulbilimi (5), Kuiper Kuşağı Seyrüseferi (5) |
| 🎨 | Ortak yüzey özelliği | çokgen hücreler (`polygon_cells`): yastık hücreler + kenar çukurları (Plüton) veya çukurcuklu "kavun kabuğu" (Triton için) |

## Ses
| Durum | İçerik | Not |
|---|---|---|
| 🎨 | Görev kontrol anonsları (Türkçe + İngilizce, oyun diline göre) | geri sayım 10→1, ateşleme, kalkış, kademe ayrılması, yörünge, atmosfere giriş, paraşüt, iniş, yakıt/oksijen uyarısı, geri sayım durdu; telsiz filtresi + sinyal sesleri; istemci ayarı `missionControlVoice` |
| 🎨 | Roket sesleri | motor döngüsü, ateşleme, kademe ayrılması, paraşüt, atmosfere giriş gürültüsü |
| 🎨 | Giysi | oksijen alarmı |
| 🎨 | Müzik | altı özgün ortam parçası (sözsüz): Yörünge (yumuşak piyano + sentezleyici), Ay (alçak uğultular, cam çan sesleri), Mars (sıcak pad'ler, uzak tek melodi), Venüs (havadar cam tınılar, derin org uğultusu), Titan (amber pus, yumuşak düşen notalar), Jüpiter uyduları (kabaran pad'ler, buz çanları); biyomların `music` alanından çalar |

## Yaratıklar (gezegenlere özgü, kan yok)
| Gök cismi | Yaratıklar |
|---|---|
| Ay 🎨 | Regolit Kenesi (tozun altına saklanır, düşük yerçekiminde sıçrar), Yaşlı Regolit Kenesi (lav tüpü muhafızı), Toz Yüzücüsü (regolitin hemen altında süzülen pasif canlı, arkasında toz dalgaları), Keşif Dronu (karakol muhafızı, sersemletme atışı), Parıltı Böceği (pasif, ışıyan; fırçayla Parıltı Kabuğu verir → elektriksiz Parıltı Lambası, Ay gecesi için) |
| Mars 🎨 | Pas Böceği (demir oksit yer, tarafsız), Toz Şeytanı (fırtınalarda canlı hortum), Kanyon Pusucusu, Taş Otlayan (iri, pasif otlayıcı), Kutup Kurtçuğu |
| Venüs (bulutlar) 🎨 | Asit Vatozu (süzülen, tarafsız), Bulut Medüzü (pasif gaz kesesi) |
| Merkür 🎨 | Alacakaranlık Sürüngeni (tehlikeli; gece-gündüz sınırında yaşar; güneş yükselince ya da gece çökünce toprağa gömülür), Oyuk Salyangozu (pasif, faydalı: oyuk tortusunu kazıyıp kükürt bırakır, kükürtle çoğalır) |
| Io 🎨 | Kükürt Yengeci (ısıya dayanıklı, yakınına gelene saldırır) |
| Europa 🎨 | Buzaltı Işık Yılanı (biyolüminesan, pasif; tuzlu su havuzlarında ve gömülü okyanusta) |
| Ganymede 🎨 | Oluk Otlayıcısı (zırhlı, sürü halinde, okyanus tuzuyla çoğalır) |
| Kallisto 🎨 | Krater Sıçrayıcısı (ürkek, düşük yerçekiminde uzun sıçrayışlarla kaçar) |
| Titan 🎨 | Metan Kayıcısı (metan göllerinin üstünde yürür) · Tolin Solucanı (kumul denizlerinde halkalı, ürkek kazıcı: yaklaşınca kuma gömülür, sessizlikte çıkar, kumu eleyip ara sıra tolin bırakır; tolin kumuyla çoğalır) |
| Enceladus 🎨 | Gayzer Süzülücüsü (gayzer bulutlarında süzülen parlak çan) |
| Triton 🎨 | Gayzer Böceği (gayzer tozuyla beslenir, gayzer fırlatınca kanat kınlarını açıp süzülerek iner, düşme hasarı almaz; gayzer tozuyla çoğalır) |
| Plüton 🎨 | Azot Kaydırıcısı (azot buzunda karnı üstünde iki kat hızlı kayar, tolinle çoğalır) |
| Charon 🎨 | Kırağı Kaplumbağası (vurulunca kabuğuna çekilir, içerideyken hasarın dörtte birini alır; amonyakla çoğalır) |
| Asteroitler 🎨 | Kaya Akarı (Ceres, Vesta) |
| Ötegezegenler | prosedürel bitki ve hayvanlar (Faz 5) |

## Sonraki fazlar (özet)
- **Faz 2 – İç Sistem:** Mars blokları (regolit, kum, bazalt, olivin kumu, kil, hematit kürecikli toprak, perkloratlı toprak, kuru buz, su buzu), Phobos/Deimos, Venüs bazaltı ve bulut platformları, Merkür grafitli regolit; Sabatier reaktörü, atmosfer işlemcisi, regolit ısıtıcı, araştırma laboratuvarı, teleskop, radyo anteni; metan ve hidrojen motorları, 3 kişilik kapsül, uydular, sondalar, iyon motoru, istasyon modülleri, gezici, Giysi Mk2.
- **Faz 3 – Dış Sistem:** asteroit kayaları ve platin grubu, Io kükürtleri, Europa buzu ve okyanusu, Titan toliniyle metan gölleri, Enceladus/Triton/Plüton buzları; uraninit, galenit → kurşun, monazit → neodimyum; nükleer termal motor, RTG, gaz toplayıcı, füzyon, Giysi Mk3–Mk4.
- **Faz 4 – Yıldızlararası:** warp çekirdeği, uzay teleskobu, spektrometre, prosedürel blok aileleri.
- **Faz 5 – Yaşam**, **Faz 6 – Galaksiler**, **Faz 7 – Cila ve v1.0**, **Faz 8 – 26.2 portu**.
