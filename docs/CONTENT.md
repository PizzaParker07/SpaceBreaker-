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
| 🎨 | Ham petrol cepleri + yüzey sızıntıları (Petrol Sızıntısı bloğu) | ceplerin tam üstünde katran lekeli zemin; kuru biyomlarda sık (28 yığında 1), ova/orman/tayga/bataklık/orman-yağmur/karlı biyomlarda seyrek (72 yığında 1) |
| 🎨 | Çelik, Al-Li alaşımı, nikel süper alaşımı, plastik levha, silisyum, silisyum plaka, güneş hücresi, temel/gelişmiş devre kartı, elektrik motoru, ablatif kalkan levhası, altın folyo, turuncu köpük, silikon conta, levha/tel/çubuk pres kalıpları, kömür tozu | |
| 🎨 | Sıvılar: ham petrol, RP-1, sıvı oksijen, sıvı hidrojen, sıvı metan, oksijen, hidrojen, metan | `c:` etiketleri, kovalar, Kriyojenik Kap; kriyojenik sıvılar açıkta kaynayıp uçar |

### Paket ② Enerji ve makineler
| Durum | İçerik | Not |
|---|---|---|
| 🎨 | Güneş Takipli Panel | doğu-batı takip, gece düz yatar; 20 FE/t tepe, hava ve güneş açısına bağlı; tozlu dünyalarda hücreler tozlanır (üstte pas rengi katman, tam tozda %60 üretim), eğilip boş elle kullanınca silinir, toz şeytanı geçince temizlenir (`Environment.addDustSource`) |
| 🎨 | Oksijen Dolum İstasyonu | tüpü beşiğe koy; FE ile, gezegenin doğal solunabilir havasından (Dünya) ya da borudan gelen oksijen gazı / sıvı oksijenden doldurur (Manevra Ünitesi gazı da). Dağıtıcının oda havası sayılmaz |
| 🎨 | Oksijen Dağıtıcı | kapalı odayı tick'lere yayılmış taşkın dolumla bulur (en çok 4096 blok), tüpten ya da borudan gelen oksijenle önce odayı basınçlandırır (blok başına oksijen), sonra hacme göre tüketir; sızıntıda hava boşalır; blok değişince yeniden kontrol |
| 🎨 | Hava Kilidi Kapısı | 2 blok, kayar kanatlar (animasyonlu); kapalıyken hava geçirmez; el ya da kızıltaşla açılır |
| 🎨 | Yanmalı Jeneratör, Batarya Bankası, Bakır / Yüksek Gerilim Kablosu | jeneratör yalnız oksijenli havada yanar; batarya önden verir (tavana takılınca ön yüzü aşağı bakar: çatıdaki batarya üstteki panelden dolar, alttaki odaya verir), şarj göstergesi |
| 🎨 | Eşya Kanalı (`spacebreaker:item_duct`) | envanterler arası eşya taşır (saniyede 16, uç başına); bir ucu eğilip boş elle kullan → o envanterden çeker (kırmızı valf); elde eşyayla eğilip kullan → o uç yalnız o eşyayı geçirir (aynı eşyayla tekrar: filtre kalkar); kanala bakan huni yalnız besler; tarif: 6 demir levha + huni → 8 |
| 🎨 | Kırıcı, Elektrik Ark Fırını, Pres (hareketli koç), Petrol Pompası (çok bloklu, animasyonlu), Rafineri (çok bloklu damıtma kulesi) | |
| 🎨 | Elektroliz Ünitesi, Kriyojenik Sıvılaştırıcı, Sıvı / Basınçlı Gaz / Kriyojenik Tank (dolum görünür; kırınca içerik eşyada kalır, yerleştirince geri gelir), sıvı/gaz borusu (uçtan çekme modu; sıvı borusu sonsuz su kaynağından 100 mB/t su çeker) | |
| 🎨 | Parça Üretim Tezgâhı | `spacebreaker:fabricating` tarifi; Roket ve Giysi alanları kendi tariflerini ekler |
| 🎨 | Yerinde üretim (ISRU): Atmosfer İşlemcisi, Sabatier Reaktörü, Regolit Isıtıcı, Buz Eritici | atmosferden gaz (Mars/Venüs CO₂, Dünya O₂, Titan CH₄); CO₂ + H₂ → metan + su; regolit ve perklorattan O₂; buzdan su, kuru buzdan CO₂; `c:regoliths`, `c:ices`, `c:dry_ices`, `c:perchlorate_soils` etiketleriyle beslenir |
| 🎨 | Karbondioksit gazı (`c:carbon_dioxide`); güneş paneli verimi gezegenin güneşe uzaklığına göre (en fazla 3×) | |
| 🎨 | Kimyasal Reaktör (`spacebreaker:chemical_reaction`), Sülfürik Asit (`c:sulfuric_acid`), Helyum-3 (`c:helium_3`) | kükürt + su + oksijen → sülfürik asit; Ay/Merkür güneş rüzgârı regolitinden (`c:regoliths/solar_wind`) oksijen + He-3 (hidrojen buzdan) |
| 🎨 | Otomasyon: Regolit Kazıcı, kızıltaş kontrolü, filtreli boru ucu | kazıcı altındaki 5x5 alanı 16 blok derine kazar ve yanındaki makinelere verir; her makinede kızıltaş modu (yok say / sinyal varken / sinyal yokken; ekrandaki düğme); boru ucuna dolu bir kapla eğilip tıklayınca o uç yalnız o sıvıyı geçirir (boş kapla kaldırılır); sıvılaştırıcı yalnız O₂/H₂/CH₄ kabul eder. Ay üssünde kazıcı → ısıtıcı → sıvılaştırıcı → tank zinciri kendi kendine LOX yapar |
| 🎨 | Sinterleme Fırını (`spacebreaker:sintering`) | her gezegenin regolitini (`c:regoliths`, Ay/Mars/Merkür) elektrikle Sinterlenmiş Regolite çevirir (hava gerekmez); tam blok olduğu için oda sızdırmaz; tuğla/merdiven/basamak Ay ailesinin taş kesici tarifleriyle; kazıcıyla beslenip otomatik çalışır |
| 🎨 | Asteroit madenciliği | karbonlu moloz (`c:regoliths/carbonaceous`: Bennu, Ryugu, karbonlu kondrit) regolit ısıtıcıda su + biraz CO₂ + kömür tozu verir; taşlı moloz (`c:regoliths/stony`: Eros, Itokawa) kırıcıda demir tozu + nikel tozu verir, ısıtıcıda oksijen, fırında sinterlenir; kazıcı küçük dünyalarda da çalışır |
| 🎨 | Roket Montaj Tezgâhı | 3 kademe + faydalı yük yuvası, canlı 3B önizleme, kademe başına Δv ve itki/ağırlık, ulaşılabilir hedefler (yörünge, Ay, iniş, dönüş) |

### Paket ③ Roket, rampa, giysi ve Ay
| Durum | İçerik | Not |
|---|---|---|
| 🎨 | Fırlatma tesisi: rampa kaplaması, alev kanalı ızgarası, servis kulesi, erişim kolu (geri sayımda katlanır), yakıt hattı (c:rp1 / c:liquid_oxygen), kontrol konsolu | çok bloklu; boş elle tıklayınca kontrol listesi + eksik bloklar alevle işaretlenir |
| 🎨 | Uyarı ışığı | dönen amber ikaz lambası (her yüze takılır): redstone sinyaliyle ya da 32 blok içindeki bir roket yakıt alırken, geri sayımda, kalkışta veya yanaşırken kendiliğinden yanar; ışını dönen animasyonlu cam (shader güvenli), gerçek blok ışığı 13 |
| 🎨 | Roket parçaları: burun konisi, kapsül, ısı kalkanı, paraşüt, küçük/orta/büyük tank, deniz seviyesi + vakum kerosen motoru, deniz seviyesi + vakum metaloks motoru (Metan Motorları teknolojisi; Mars'ta üretilen metan + LOX ile dönüş), deniz seviyesi + vakum hidroloks motoru (Hidrojen Motorları; en yüksek Isp, Merkür yörüngesine gidip dönüş), kademe ayırıcı, ara kademe, kanatçık, ızgara kanatçık, iniş bacakları, manevra iticileri | veriyle tanımlı (`rocket_part/*.json`), gerçekçi kütle/itki/Isp; tanklar kademenin motorunun yakıtını (RP-1, sıvı metan ya da sıvı hidrojen) alır; envanterde 2B, elde ve rokette 3B |
| 🎨 | Roket, yörünge boyutları (Dünya / Ay yörüngesi + diğer cisimler için ortak yörünge), yıldız haritası (gök cismi verisinden: Merkür, Venüs, Mars, Phobos, Deimos ve dış sistem kendiliğinden; Güneş solda, gezegene tıklayınca sistemine yakınlaşır, sağ panelde resim, kısa açıklama, tehlikeler, "Yörüngeye git / Yüzeye in", düz dille yakıt kontrolü, dönüş yakıtı, yolculuk süresi, fırlatma penceresi, adım adım rota; sağ tık ile "Yakın yıldızlar": Güneş'e 25 ışık yılı içindeki yıldızlar, Samanyolu'nda konum, yıldız bilgisi — yıldızlar arası yolculuk henüz yok; Dünya'ya yakın asteroitler (Bennu, Ryugu, Itokawa, Eros) tek "Asteroitler" düğümünde toplanır; kuyruklu yıldızlar (1P/Halley, 2P/Encke, 67P) tek "Kuyruklu yıldızlar" düğümünde; bilgi kartı: Güneş'e uzaklık, sonraki yakın geçiş, bir tur süresi, etkinlik; yörüngelerine gidilebilir) | binince roket kamerası (fareyle çevresinde dön, tekerlekle yakınlaş, F5 ile kokpit penceresi), dünya içinde yumuşak geçiş: yükseldikçe gök kararır, Dünya'nın ufku eğrilir (yörüngedeki görüntüye kadar), yolculukta yörünge görünür kalır (üstte tek satır), boyut değişiminde yalnız ~1 sn'lik yumuşak karartma, son kare tutulur, "Yükleniyor" ekranı yok, geri sayım, ateşleme, kademe ayrılması, gökyüzünün kararması, yörüngede dönen gezegen, Ay'a motorlu iniş + toz, atmosfere giriş (plazma), paraşüt, suya iniş |
| 🎨 | Acil Dönüş İşareti, Yaratıcı Yakıt Kartuşu | mahsur kalmama; test için tam yakıt |
| 🎨 | İstasyon Gövdesi, İstasyon Penceresi, Yanaşma Portu, İstasyon Kurulum Kiti | yörüngede boşlukta kit 5×5 gövde zemini açar; gövde/pencere/port tam blok → oksijen dağıtıcısıyla basınçlı oda; porta 12 blok yakın yörüngedeki roket "yanaşmış" sayılır: mürettebat inip binebilir, porta borulanan sıvılar roketin tanklarına gider |
| 🎨 | Yaşam Modülü Kiti, İstasyon Ranzası, Yakıt Deposu | kit boşlukta kapalı 7×5×7 oda açar: pencere bandı, hava kilidi kapısı, uzak duvarda yanaşma portu, içeride oksijen dağıtıcısı + ranza, çatıda güneş paneli dizisi ve altında gece için batarya bankası, tavanda bakır kablo ucu (endüstri); dağıtıcıya oksijen tüpü takınca oda hava dolar. Ranza her dünyada yeniden doğma noktası ayarlar (yatak gerekmez). Yakıt deposu 4 tank (RP-1, LOX, metan, hidrojen; 32.000 mB), borudan/kovayla dolar; FE ile çalışır (40 FE/t, 20.000 FE tampon) ve 8 blok içindeki yanaşma portlarındaki roketlere yakıt pompalar. Yörüngeye varan roket o yörüngedeki en yakın yanaşma portuna kendiliğinden yaklaşır: uzakta hızlı, son metrelerde yavaş, manevra iticisi pufları; kenetlenmede kilit sesi ve kabin sarsıntısı. Yıldız haritasında istasyonu olan gezegenin yanında istasyon simgesi; panelde kuran oyuncu ve yanaşma portu sayısı (yakın portlar tek istasyon sayılır). Yörüngede gündüz/gece: istasyon 10 dakikada bir tur atar, turun yaklaşık üçte birinde gezegenin gölgesinde kalır (astro Eclipses; yarı gölgede yumuşak geçiş); güneş gezegenin arkasına kayarak söner, ışık kararır, gece yüzü karanlık. Sunucu ve istemci aynı oyun saatinden hesaplar (`OrbitDaylight.sunlit(level, pos)` güneş panelleri için) |
| 🎨 | Mars İniş Aracı | yaratıcı sekmesinde hazır tek kademe: metaloks vakum motoru, büyük tank, iniş bacakları, ısı kalkanı, kapsül, paraşüt, manevra iticileri. Mars yörüngesinden ısı kalkanı + paraşüt + motorlu son inişle yüzeye (iniş işareti varsa üsse) iner; Mars'ta üretilen metan ve oksijenle doldurulup yörüngeye döner. Toz fırtınasında kalkışta görev kontrolü uyarır (engellemez), paraşüt inişi fırtınada daha çok sapar |
| 🎨 | Nükleer Termal Motor (vakum) | araştırma: Nükleer Termal İtki; tarif nükleer yakıt elemanı (industry, spacebreaker:reactor_fuel etiketi) + radyasyon kalkanı levhası (gölge kalkanı: tükenmiş uranyum ya da tungsten, spacebreaker:radiation_shielding) + nikel süperalaşım + titanyum + hidroloks vakum motoru. Bir fisyon reaktörü sıvı hidrojeni doğrudan ısıtır: LOX yok, Isp 880 s (kimyasalın ~2 katı), 150 kN, 3,4 t; TWR düşük, yerden kalkış için değil. Kademesi yalnız LH2 alır (hidroloks ile aynı hidrojen hattı); tasarım ekranı ve Δv hesabı ona göre. Yanarken çevresine radyasyon verir (giysinin doz modeline bağlı, mesafenin karesiyle azalır, üstteki mürettebat gölge kalkanıyla korunur); soğukken güvenli. GameTest Isp/Δv/dolum ve radyasyon; vitrin 52_nuclear_thermal; montaj ekranında önizleme köşesinde "☢ Nükleer" etiketi, üzerine gelince uyarı (ilk kademedeyse kırmızı: kalkamaz, rampayı ışınlar); vitrin 53_assembler_nuclear |
| 🎨 | Roket parça değerleri gerçek orana çekildi | tank kuru kütlesi taşıdığı yakıtın ~%4'ü (küçük/orta/büyük 140/224/312 kg), kerosen motoru 845 kN / 470 kg (Isp 282/311 s aynı), vakum motoru Isp 348 s; dengeli Δv ölçeği 0,35 → 0,43, böylece QA ilerleme modelinde bütün dünya ve teknoloji adımları birebir aynı kalır. Gerçekçi ayarda artık yörüngeye çıkılır ve ısı kalkanıyla dönülür (QA: 2/4 hedef); Ay ve Mars bugünkü montaj masasının sınırını aşan ağır bir roket ister (yapılandırma açıklamasında yazar). Hazır Ay roketi: 2. kademe 2 büyük tank, 3. kademe orta tank |
| 🎨 | Kriyojenik buharlaşma | sıvı hidrojen günde kalanın %0,1'i, sıvı oksijen ve metan %0,02'si, RP-1 hiç; kademe en hızlı kaynayan sıvısının oranıyla kaybeder. Yalnız uzun yolculukta (astro'nun gerçek transfer süresi) ve yörüngede beklerken, rampada asla; gerçekçilik ayarıyla ölçeklenir (gerçekçi 1, dengeli 0,2, kolay 0,1; dengelide Plüton sistemi gidiş-dönüşü kaymasın diye). Uçuş planlayıcı hesaba katar: yıldız haritası yakıt kontrolüne dahil, "Yolda buharlaşma kaybı: %X" satırı; motor ipucunda günlük oran. GameTest cryogenicBoilOff; dengelide hiçbir dünyanın ilerleme adımı değişmez (yalnız biraz daha büyük tasarımlar), Hale–Bopp yörüngesine gidiş-dönüş artık yok (1300 yıllık yolculuk) |
| 🎨 | Çok Katmanlı Yalıtım ve yalıtımlı tanklar (küçük/orta/büyük) | MLI: 4 altın folyo yalıtım + 4 plastik + alüminyum levha → 2. Tank + MLI (küçük 1, orta 2, büyük 3) → yalıtımlı tank (araştırma: Kriyojenik); +60/90/120 kg kuru kütle, aynı kapasite. Yalıtımlı tankların payı kadar kademenin buharlaşması %75 azalır; harita "buharlaşma kaybı" satırı ve yakıt kontrolü bunu hesaba katar. Altın folyo battaniye, gümüş bant dokusu; vitrin 54_insulated_tanks; GameTest cryogenicBoilOff |
| 🎨 | Kargo Modülü | insansız kargo roketi: kapsül yuvasına takılır (Uzay İstasyonları teknolojisi), 27 yuvalı ambar (rampada ya da yanaşmışken sağ tık). Konsoldan hedef seçilip fırlatılır: hedef bir yörüngeyse oradaki istasyonun portuna kendiliğinden yanaşır, yüzeyse en yakın iniş işaretine iner; ambarı port ya da işaretin 4 blok çevresindeki en yakın sandık ve shulker kutularına boşaltır (makinelere asla), sığmayan ambarda kalır; sahibine mesaj gider. Otomatik sefer: iki uç da Dünya yüzeyi değilse (ör. Ay üssü ↔ Ay yörüngesindeki istasyon) her uçta boşaltır, port/işaret yanındaki varillerden yeni yükü alır, 10 saniye bekler (depo yakıt basar) ve yakıt yetiyorsa öbür uca uçar; yük yoksa ya da yakıt yetmiyorsa bekler. Eğilip boş elle sağ tık seferi açar/kapatır |
| 🎨 | Ay Mekiği, İniş İşareti | yeniden kullanılabilir tek kademe (vakum motoru, orta tank, iniş bacakları, kapsül, manevra iticileri; yaratıcı sekmesinde hazır): Ay yörüngesindeki istasyona yanaşır, depodan yakıt alır, Ay yüzeyine iner ve tek tankla yörüngeye döner. Yüzeye konan iniş işareti kayıtlıdır: yörüngeden inen her roket o dünyadaki en yakın işaretin üstüne iner (üsse dönüş; atmosferli dünyada paraşüt inişi rüzgârla birkaç blok sapabilir). Yıldız haritasında üssü olan dünyanın yanında üs simgesi; panelde kuran oyuncu ve iniş işareti sayısı |
| 🎨 | Mürettebat Kapsülü (3 kişilik) | Ağır Fırlatma teknolojisi; beyaz ısı karoları, üç pencere, tepede yanaşma halkası; üç oyuncu yan yana oturur (çok oyunculu yolculuk); her mürettebat kapsülü basınçlı kabindir: oturan mürettebat yörüngede ve yolculukta giysisiz nefes alır, kapsülden inince kabin havası kalmaz |
| 🎨 | İletişim Rölesi Uydusu, Gözlem Uydusu, İniş Sondası | insansız roket: kapsül yuvasına takılır, konsoldan "Hedef..." ile yıldız haritasında görev hedefi seçilir ve fırlatılır; yörüngede kendiliğinden hedefe uçar. Uydu hedef yörüngede bırakılır (dünyada kalıcı kayıt, yörüngede dönen model); röle o gezegen sisteminde görev kontrolü sinyalini açar (yoksa "Sinyal yok", kilitlemez), gözlem uydusu dünyayı haritalar ve araştırma verisi gönderir; sonda iner, 2 numune + sonda verisi gönderir (çevrimdışıysa girişte) |
| 🎨 | EVA Giysisi Mk1: Kask, Gövde, Pantolon, Bot | özel 3B modeller: yuvarlak kask, sarmal füme vizör, altın güneş vizörü (eğil + kullan), parlayan yan lambalar; göğüs kontrol kutusu, yaşam destek sırt çantası + oksijen şişeleri; mavi yatak halkaları; tabanlı botlar. Vakumda −190…+130 °C (Ay gecesi, Vesta dahil); yoğun havada ısı taşınımı aralığı daraltır. Yalnızca dört parça birlikte sızdırmaz |
| 🎨 | Termal Giysi (Mk2) | gümüş alüminize kumaş, altın vizör siperliği, sırtta altın radyatör kanatları; vakumda −190…+500 °C: Merkür'ün gündüzü ve gecesi, Venüs |
| 🎨 | Isıtmalı Giysi (Mk2) | turuncu dolgu yalıtım, kalın yaka, kabarık omuz/bacak, ısıtıcı batarya; −250 °C'ye kadar (Titan) |
| 🎨 | Radyasyon Zırhlı Giysi (Mk3) | kurşun grisi + sarı zırh plakaları, dar vizör yarığı, sırt kalkanı; dozun yalnız %4'ü geçer (Io, Europa) |
| 🎨 | Hologram Projektörü | baştan üretilebilir (ametist, cam panel, kızıltaş, demir, bakır; araştırma gerekmez); çoklu blok yapıların nasıl kurulacağını gösterir: yapı seç, yere yansıt, eğil + tekerlek ile döndür, katman katman mod; eksikler camgöbeği hayalet, yanlış bloklar kırmızı çerçeve, ekranda malzeme listesi; denetleyiciye boş elle eğil + sağ tık eksikleri yansıtır. Yapılar `data/<ns>/spacebreaker/multiblock/*.json` |
| 🎨 | Giysi yükseltmeleri | demirci masası: Giysi Yükseltme Şablonu + parça + modül (termal regülatör, radyasyon kalkanı, ek oksijen, jetpack güçlendirici) |
| 🎨 | Boyanabilir aksan | tüm giysi parçalarının halka/aksan rengi boyayla değişir |
| 🎨 | Manyetik EVA Botları | düşük/sıfır yerçekiminde zemine yapıştırır (eğilince bırakır); tutunurken metalik tık sesi, HUD'da MAG |
| 🎨 | Manevra Ünitesi | gövdeye takılır; soğuk gazla uçuş (sıfır g'de 6 eksen, Ay'da havada kalma); dört lüleden itişe ters küçük gaz pufları (herkes görür) |
| 🎨 | Giysi Bakım İstasyonu | giysiyle üstünde ya da yanında dur: tüpler ve Manevra Ünitesi dolar (gezegen havası ya da boruyla oksijen), aşınma yavaşça onarılır; yuvaya konan giysi parçası da onarılır; FE ile çalışır |
| 🎨 | Güvenlik Halatı | bir bloğa takılır (kullan / tekrar kullan çözer); 16 bloktan uzağa sürüklenmeyi ve gergin halatta düşmeyi önler; turuncu halat herkese görünür, alçak yerçekiminde az sarkar; blok kırılınca ya da eşya bırakılınca çözülür |
| 🎨 | Kask lambaları ve canlı giysi | karanlıkta lambalar kendiliğinden yanar (H tuşu: geniş / uzun huzme — lav tüpleri ve soğuk tuzaklar için dar, 40 bloğa uzanan koni): bakılan yere blok ışığı gibi kademeli ışık havuzu (dinamik ışık modu gerekmez, shader güvenli), dışarıdan kısa ışık konisi; vizörde gökyüzü yansıması; giysi dayanımının ötesindeki soğukta (ör. soğuk tuzak, Titan) vizör kenarına kırağı çöker, HUD'da SOĞUK TUZAK etiketi ve uyarısı; yürürken sırt çantası ve anten hafifçe sallanır |
| 🎨 | Oksijen Tüpü (5 dk), Büyük Oksijen Tüpü (15 dk) | envanterde 2B, elde 3B; gövdenin sırt çantasına 2 tüp takılır. Süre yürüyüşe göredir: koşarken ×1,5, kazarken/vururken ×1,25, dururken ×0,8 harcanır; termal, ısıtmalı ve radyasyon giysileri ×1,1. HUD süresi ve eve dönüş uyarısı ~5 sn'lik kayan ortalamayı kullanır |
| 🎨 | Kask HUD'u | GUI katmanında (shader güvenli): O2, giysi, vücut ısısı, dış sıcaklık, radyasyon, basınç/g, uyarılar; ölçek/opaklık ayarı |
| 🎨 | Gerçekçi ses | ince havada (Mars) dünya sesleri kısık ve boğuk, vakumda dış sesler neredeyse yok; giysinin dokunduğu sesler (kendi adımların, elindeki alet, kazdığın blok, üstünde durduğun makine) giysiden boğuk gelir. Kask içi sesler (telsiz, görev kontrol, uyarılar) ve basınçlı oda / solunabilir hava normal. Her ses başlarken bir kez sınıflandırılır, OpenAL EFX alçak geçiren filtre (yoksa yalnız ses seviyesi); istemci ayarı `realisticSound`, varsayılan açık |
| 🎨 | Hayatta kalma | boşlukta nefes tutma ve boğulma, yavaş değişen vücut ısısı (hipotermi/aşırı ısınma), radyasyon dozu, gerçek yerçekimi (zıplama ve düşme hasarı ölçekli) |
| 🎨 | Eve dönüş (kask HUD'u) | en yakın kendi roketine, kendi İniş İşaretine ya da basınçlı üsse yön oku ve mesafe (Çevre Sensörü ile eğil + kullan: hedef seç); kalan O2 yürüyüşe yetmeyecekse sarı DÖN, hiç yetmiyorsa kırmızı uyarı; çok oyunculuda yalnız kendi roketin ve işaretin |
| 🎨 | Çevre Sensörü | kullan: 10 sn'lik okuma kartı — basınç (kPa/bar), hava bileşimi (gövde verisinden), sıcaklık (güneşte/gölgede), radyasyon doz hızı, yerçekimi, ayak altındaki zemin; Mars panel tozu için kanca |
| 🎨 | Titanyum aletler: kazma, balta, kürek, çapa, kılıç | demir ile elmas arası (1100 kullanım, hız 7, demir kazma seviyesi), daha hafif ve hızlı savruluyor, soğukta kırılganlaşmıyor; titanyum külçesiyle onarılır |
| 🎨 | Elektrikli Matkap | FE ile çalışır (100.000 FE, blok başına 100 FE): şarjlıyken taş ve toprağı elmas kazmadan hızlı kazar, aşınmaz; şarjsızken işe yaramaz; Giysi Bakım İstasyonunda (yuvada ya da elde) ve her FE eşya şarjcısında dolar; motor sesi. Geniş Kazı Matkap Başlığı (titanyum + kızıltaş bloğu) ile demirci masasında (Giysi Yükseltme Şablonu) 3 × 3 kazar, eğilince tek blok; her blok ayrı şarj harcar |
| 🎨 | Mare Bazaltı, İlmenit Cevheri | |
| 🎨 | EVA Kaskı | özel 3B model, saydam vizör, altın güneş vizörü (eğil + kullan), parlayan lambalar |
| 🎨 | Oksijen Tüpü | envanterde 2B, elde 3B; 5 dakikalık oksijen |
| 🎨 | Ay Regoliti, Mare Regoliti, Volkanik Cam Toprağı, Buzlu Regolit | hepsi **kalıcı ayak izi** tutar, sol/sağ sırayla; gezici geçince **tekerlek izi** (tüm gezegen toprakları) |
| 🎨 | Mare Bazaltı, İlmenit Cevheri (→ Ham İlmenit), KREEP Cevheri (→ KREEP Parçası: nadir toprak + toryum) | |
| 🎨 | Anortozit (+ merdiven, basamak, duvar), Cilalı Anortozit (+ merdiven, basamak), Anortozit Tuğla (+ merdiven, basamak, duvar) | taş kesici tarifleri |
| 🎨 | Darbe Breşi, Ay Buzu, Meteor Demiri (→ 4 ham demir) | |
| 🎨 | Sinterlenmiş Regolit (regolit fırında), Sinterlenmiş Regolit Tuğla (+ merdiven, basamak, duvar) | ilk Ay üssü yapı malzemesi |

### Gezegen motoru ve Ay boyutu
| Durum | İçerik | Not |
|---|---|---|
| 🎨 | Gezegen arazi üreticisi (`spacebreaker:planet`) | veri paketi profili (`spacebreaker/terrain/*.json`): yaylalar, mare havzaları ve kırışık sırtlar, gerçek krater morfolojisi (çanak, yükseltilmiş kenar, ejekta örtüsü, büyük kraterlerde düz taban + teras + merkezi tepe), genç kraterlerde parlak ışınlar, ejekta kayaları, kutuplarda kalıcı gölgeli buzlu krater tabanları; z ekseni = enlem; dünya tohumuna göre belirlenimci |
| 🎨 | Ay boyutu `spacebreaker:moon` | 5 biyom: Ay Yaylaları, Ay Denizi (Mare), Büyük Krater, Piroklastik Ovalar, Ay Kutbu; kayalar, cevher ve buz damarları, meteorit parçaları; Gruithuisen tipi volkanik domlar (açık renkli anortozit, 45–95 blok yarıçap, ≤28 blok yükseklik, minik tepe çukuru) |
| 🎨 | Soğuk tuzaklar (kalıcı gölgeli kutup kraterleri) | taban ve 8 blok üstü güneş görmez: gece sıcaklığının 60 °C altında, gündüz de (Ay'da ≈ −233 °C); 1. kademe giysi yetmez, ısıtmalı giysi (veya yeterli termal modül) gerekir; girişte eylem çubuğu uyarısı; buz → ISRU su/H₂/O₂; buzlu kutbu olan her gezegende (Merkür dahil) |
| 🎨 | Yeraltı sığınağı (havasız dünyalar) | yüzeyin ≥4 blok altında ve gökyüzünü görmeyen yerde gece-gündüz sabit sıcaklık = gündüz/gece ortalaması (Ay ≈ −26 °C, yüzeyde +120/−173 °C yerine); üstteki kaya radyasyonu da keser → lav tüpleri ve kazılmış üsler doğal sığınak |
| 🎨 | Mikrometeorit yağmurları (havasız dünyalar) | ara sıra (ortalama 60 dk'da bir, 40 sn sürer; sunucu ayarı `micrometeorites`: kapatılabilir, aralık/süre): 1 dk önce eylem çubuğunda uyarı, gökyüzünde parlak çizgiler, yerde toz patlamaları; açıkta kalan oyuncuya yarım kalp hasar ve giysi parçası aşınması; üstte herhangi bir çatı (modül, kulübe, mağara, lav tüpü) korur; `/spacebreaker meteorshower` |
| 🎨 | Kuyruklu yıldızlar gökyüzünde (her dünyadan) | astro etkinlik modeline göre yalnız ETKİN/BÜYÜK iken: parlak koma + güneşin tersine solan kuyruk (gökyüzü geçişi, shader güvenli); büyük kuyruklu yıldız (ör. Halley 2061) görününce görev kontrol mesajı (EN/TR); vitrin `sky_planets` 03_earth_halley_2061 (alacakaranlık) + 04_earth_halley_2061_night (karanlık gece); geniş kavisli sarımsı toz kuyruğu + düz mavimsi iyon kuyruğu, yumuşak koma |
| 🎨 | Kuyruklu yıldızın kendi göğü (67P, Encke, Halley, Tempel 1) | Güneş gerçek uzaklığa göre büyür/küçülür (eksantrik yörünge); etkinken koma göğü sarar: Güneş çevresinde yumuşak mavimsi-beyaz parıltı, karşı yönde (kuyruk) daha sönük; uykudayken kapkara gök; vitrin `coma_sky` |
| 🎨 | Kuyruklu yıldızlara gerçekçi buluşma | Δv ve süre astro'nun CometRendezvous tablosundan (Dünya alçak yörüngesinden, günberi/günöte geçişi, eğiklik ve retrograd dahil; dünya başına bir kez, J2000'den 2 geçiş): gidiş: yörüngenin herhangi bir noktasında 25 km/s ve 20 yıl içindeki en ucuz buluşma; dönüş: doğrudan Dünya atmosferine, giriş hızı 15 km/s üstündeyse frenleme yakmasıyla. Halley gidiş ~19 km/s / ~14 yıl, dönüş ~11 km/s (güvenli girişe frenleme dahil) / ~9 yıl: oyun sonu hedefi, NTR gerekir (QA modeli adım 21, Halley buzundan yakıt üreterek dönüş); 67P ve Tempel 1 kimyasal (adım 10); Encke gerçek değerle NTR gerektiriyor (adım 16); Hale–Bopp yalnız gözlem (haritada engel mesajı). Haritada "bu kuyruklu yıldıza ancak nükleer termal motorla ulaşılır" ipucu; buharlaşma yolculuk süresiyle hesaba katılır. GameTest cometRendezvousRoutes |
| 🎨 | Ay'dan (ve her uydudan) Güneş tutulması | geometri astro'dan: gezegen Güneş'i örttükçe Güneş diski söner, gezegen diski kararır ve çevresinde ince kırmızı-turuncu "bütün gün batımları" halkası belirir; ışık, güneş paneli üretimi düşer, sıcaklık yavaşça geceye iner; ~1 dk önce ve başlarken görev kontrol mesajı; `/spacebreaker eclipse` sıradakini söyler; GameTest 2000-01-21 tam Ay tutulması; vitrin 10b_eclipse_from_moon |
| 🎨 | Ay'da Dünya ışığı | Ay gecesinde yakın yüzde zemini Dünya'nın yansıttığı soluk mavimsi-beyaz ışık aydınlatır: Dünya dolunay evresinde ve yüksekteyken belirgin (vanilla gecesinden açık, gündüzden çok karanlık), çeyrekte üçte bir, hilalde neredeyse karanlık; Dünya'nın görünmediği uzak yüzde hiç yok. Evre gökte çizilen Dünya ile aynı; yalnızca istemci ışık haritası (shader güvenli), güneş paneli üretmez; GameTest earthshineFollowsThePhase; vitrin 10c/10d (aynı yer, dolunay ve hilal) |
| 🎨 | Ufuk parıltısı (havasız dünyalar) | İlk Ay iniş araçlarının fotoğrafladığı elektrostatik toz parıltısı: gün batımından hemen sonra / gün doğumundan hemen önce, gizli Güneş'in üstünde ufka yapışık ince, sıcak beyaz bir ışık şeridi; Güneş 0–12° alttayken, en parlak 5°'de. Nadir: gün batımı/doğumlarının yaklaşık üçte birinde, her istemcide aynı (gün sayısı + boyut tohumu). Yalnızca görsel, ek-ışık dörtgenleri (shader güvenli); GameTest horizonGlowIsRareAndBrief; vitrin 10e_horizon_glow |
| 🎨 | Gök cismi zamanı | gün uzunluğu gerçek değerden ölçeklenir (ayar), her boyutun kendi saati; `/spacebreaker localtime` |
| 🎨 | Vakum gökyüzü | siyah gök, yoğun yıldız alanı + Samanyolu, mesafeye göre boyutlanan keskin Güneş, Ay'da sabit duran ve **doğru evresiyle** dönen Dünya; gece Dünya ışığı; bulut/yağmur/pus yok |

### Ay zindanları
| Durum | İçerik | Not |
|---|---|---|
| 🎨 | Terk Edilmiş Keşif Karakolu | merkez modül, laboratuvar, depo **veya** çökmüş sera, drone hangarı, mürettebat yatakhanesi, hava kilidi (iki demir kapı), güneş paneli tarlası, iniş pisti, anten; **gücü kesik kasa** (bulmaca: komuta sandığındaki güç hücresini kapının üstündeki yuvaya tak) ve basınç plakalı ok tuzağı; muhafız: Keşif Dronları (+ çağırıcı) |
| 🎨 | Antik Lav Tüpü | çökmüş tavan girişi, kıvrımlı tünel, düşmüş eski sonda enkazı (sandık), Yaşlı Regolit Kenesi yuvası |
| 🎨 | Eski Buz Sondaj İstasyonu | yalnız kutup kraterlerinin kalıcı gölgeli tabanında (soğuk tuzak, ısıtmalı giysi gerekir): sondaj kulesi ve buza inen sondaj dizisi, hâlâ çalışan RTG'li kontrol kulübesi, çekirdek rafları; ödül: **Buz Çekirdeği Numunesi** (→ 4 Ay buzu), RTG yakıt peleti şansı, araştırma diski; muhafız: Isı Emiciler (soğuk tuzakta sürekli yaşar, RTG'nin enerjisini emer) |
| 🎨 | Ganimet | `chests/moon/tech_data` araştırma diskleri için kanca tablosu (Araştırma alanı `spacebreaker:tech_disk` ekler) |
| 🎨 | Üs blokları (1. kısım): alüminyum panel (+merdiven, basamak, duvar), titanyum güverte levhası (+merdiven, basamak), basınçlı cam, lombar, ızgara zemin, kayar kapı | tam bloklar, basınçlı cam, lombar ve kapalı kayar kapı hava tutar (oksijen dağıtıcısı); ızgara, basamaklar ve açık kapı havayı geçirir; kayar kapı 2 blok yüksek, kanatları yana kayar, el ya da redstone ile açılır; cam ve lombar: opak çerçeve + yarı saydam cam (shader güvenli); taş kesicide de yapılır |
| 🎨 | Üs blokları (2. kısım): uyarı şeridi, LED/panel lamba/projektör, kablo ve boru kanalları, merdiven/korkuluk/platform | |

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
| ✅ | Güneş Sistemi verisi: Güneş, Merkür, Venüs, Dünya, Ay, Mars, Phobos, Deimos, Ceres, Vesta, Bennu, Ryugu, Eros, Itokawa, Jüpiter, Io, Europa, Ganymede, Callisto, Satürn, Titan, Enceladus, Uranüs, Miranda, Neptün, Triton, Plüton, Charon | NASA/JPL değerleri; gaz/buz devlerine iniş yok |
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
| 🎨 | Toz fırtınaları | seviye başına kayıtlı, sise ve GUI katmanında toz tonu (shader güvenli), uçuşan toz, %85 daha az güneş, kutuplar daha soğuk; doğal fırtınadan 2 dk önce ufuk uyarısı, geliş ve diniş mesajları (EN+TR); `/spacebreaker duststorm start|forecast|stop` |
| 🎨 | Toz hortumları (Dust Whirl) | iyi havada, öğleden sonra ova ve kumullarda oyuncu yakınında ara sıra (oyuncu başına ≤2); yavaş dolaşan, tabanda dar yukarı genişleyen yarı saydam huni (yalnızca istemci parçacıkları, shader güvenli), tabanda toz halkası; zararsız, eşya ve oyuncuyu hafifçe savurur, geçtiği güneş panellerini temizler; sunucuda kaydedilmeyen hafif varlık, 128 blokta oyuncu yoksa silinir |
| 🎨 | Güneş paneli tozlanması (Mars) | Mars toz kaynağı: sakin havada ~3 günde tam örtü, fırtınada ×6 (`DustLoad.rate`); verim %100→%60 ve toz katmanı industry'nin panelinde; hortum veya fırça (sağ tık) temizler; Çevre Sensörü toz hızını gösterir |
| 🎨 | Mevsimler ve CO₂ kırağısı | sıkıştırılmış Mars yılı (24 sol, Ls); kış yarıküresinde kutup başlığı ~52°'ye kadar yayılır, gecesi 12° daha ötede ince Mevsimsel CO₂ Kırağısı katmanı (yarı açık, pembemsi tozlu doku; kenarda %30 yamalı, 10° içeride tam örtü), ilkbaharda süblimleşir (alttaki blok bozulmaz; kırılınca iz bırakmaz); oyuncu başına tick başına 4 rastgele sütun, 64 blok içinde, yalnızca yüklü chunk'lar; mevsim değişince mesaj (EN+TR); `/spacebreaker season <Ls>` |
| 🎨 | Mevsimsel basınç döngüsü | hava kutup başlıklarına donup geri süblimleştikçe Mars'ın basıncı yıl boyunca değişir (Viking ölçümleri gibi): güney kışında Ls≈160 −%13, güney yazında Ls≈265 +%13; `Environment.pressureKpa` üzerinden HUD ve Çevre Sensörü gösterir |
| 🎨 | Su buzu bulutları ve kanyon sisi | Viking gözlemleri gibi: Büyük Yarık Kanyonu tabanında gün doğumundan hemen sonra ince, soluk pembemsi-ten sis (en yoğun anda ~80 blok görüş; tabanda dolu, ~30 blok yukarıda biter, alçak gök de puslanır, kuşluğa doğru dağılır); afelyon mevsiminde (Ls≈95 ±28°) ekvator üzerinde ince, tüy gibi mavimsi-beyaz bulut şeritleri (kodla çizilmiş 256 px doku, yumuşak kenarlar, düşük opaklık, yavaş sürüklenir); yalnızca istemci, shader güvenli; mevsim sunucudan istemciye senkron |
| 🎨 | Mars göğünde Dünya ve Ay | gökyüzü efemerisinden gerçek konumda: Dünya parlak mavi-beyaz sabah/akşam yıldızı, Ay yanında sönük ayrı bir nokta (çakışınca gerçek yönde ayrılır); Phobos/Deimos yalnızca Mars göğünün kendi diskleri olarak (çift çizim giderildi, moon) |
| 🎨 | Zindanlar | Terk Edilmiş Mars Araştırma İstasyonu (sera, ISRU tesisi, garaj, sondaj kulesi), Antik Gömülü Nehir Yatağı (katmanlı mağara, kil yatak, düşmüş sonda) |
| 🎨 | Kayıp İniş Aracı | ovalarda seyrek (≈1/km, gezici yolculuğu): eğik altıgen gövde, kırık bacak, gömülü ısı kalkanı, toza serilmiş turuncu-beyaz paraşüt; veri kaydedicide uzay sondaları / Mars jeolojisi / derin uzay ağı diski, deney verisi |
| 🎨 | Lav tüpleri | volkan ve ova altında doğal tüneller (bazalt kabuk, cilalı lav astarı, olivin kumlu taban), çökmüş ışıklıklardan girilir; 5+ blok tavan radyasyonu 1/32'nin altına düşürür: hazır üs yeri |
| 🎨 | Kutup katmanlı tortuları | temiz buz arasında düzensiz aralıklı ince toz bantları (12 blokta 1–3), kutup oluklarının duvarlarında görünür |
| 🎨 | Endüstri girdileri | kırıcıda: Perkloratlı Toprak → Perklorat Tuzu (`c:dusts/perchlorate`, oksijen kaynağı); Mars/tozlu/krater buzu → buz (`c:ices/water`, eriyip elektroliz suyu olur) |

### Phobos ve Deimos
| Durum | İçerik | Not |
|---|---|---|
| 🎨 | Boyutlar `spacebreaker:phobos`, `spacebreaker:deimos` | çok küçük, havasız; Phobos'ta oluk zincirleri; gökte dev Mars (≈40° / 16°), evresiyle |
| 🎨 | Stickney krateri | Phobos'un dev krateri sabit yerde (doğuş noktasının ~320 blok doğusu, 230 blok yarıçap, 58 blok derin kase); arazi motoruna `named_craters` (ad, x, enlem, yarıçap, derinlik, tepe) eklendi |
| 🎨 | Bloklar | Phobos Regoliti, Deimos Regoliti (kalıcı ayak izi), Karbonlu Kondrit |
| 🎨 | Yaratık: Çapa Örücüsü (Tether Spinner) | Phobos ve Deimos'ta; altı bacaklı, uzun sekmelerle gider; her sekmeden önce zemine ipek ip yapıştırır, ip gerilince geri çekilir (küçük ayda kaçış hızı dersi); havada ip parıltıyla görünür, bacaklar toplanır; ip (string) bırakır; karbonlu kondritle çekilir/üretilir |
| 🎨 | Zindanlar | Düşmüş Numune Dönüş Sondası (Phobos), Deimos Buz Deposu, Stickney'nin Çapalı İniş Aracı (dünyada tek: Stickney tabanında, 3 harpun zinciriyle zemine bağlı, "1/2000 g: geri sekti" levhası; güvenlik ipi, mikro-yerçekimi madenciliği / uzay sondası diskleri), Deimos Röle Kulesi (spiral 12 kaya yığını, her biri 2 blok yüksek ve 4+ blok uzak: Dünya'da aşılamaz, Deimos'ta her gerçekçilik ayarında aşılır — test MC zıplama fiziğiyle doğrular; tepede röle ve veri kasası, "1/4000 g" levhası) |

### Venüs `spacebreaker:venus`
| Durum | İçerik | Not |
|---|---|---|
| 🎨 | Yüzey | Volkanik Ovalar, Tessera Yaylaları, kalkan volkanlar, kraterler; 464 °C, 92 bar, loş turuncu pus |
| 🎨 | Bulut katmanı (y 195–245) | ~1 bar, oda sıcaklığı; asit bulutları kaplamasız zırhı aşındırır; üstünde bulut denizi, Güneş batıdan doğar |
| 🎨 | Bloklar ve eşyalar | Venüs Bazaltı, Kavrulmuş Çakıl, Tessera Kayası, Kükürt Kabuğu (→ kükürt), Metal Kırağısı; Hafif Kompozit Panel, Aerojel, Balon Kumaşı; Floropolimer Kaplama (zırh + kaplama → aside dayanıklı zırh) |
| 🎨 | Zindan | Terk Edilmiş Araştırma Balonu (bulut tepelerinde, Asit Vatozu muhafızları) |
| 🎨 | Venüs İniş Sondası | yüzey (464 °C, 92 bar) insana kapalı; bulut katmanından bırakılır, paraşütle iner, yüzeye değince sahibine 2 Venüs numunesi + iniş verisi gönderir, sonra 1 dakikada ısıdan susar; tarif: zarf kumaşı, aerojel, devre, tungsten |
| 🎨 | Aerostat Platform Kiti | bulut kuşağında (≈1 bar, oda sıcaklığı) kullanılınca yüzen bir üs şişirir: 9×9 kompozit güverte, kapılı korkuluk, halatlar, fener ve kaldırma gazı zarfı; yalnız boş havaya yerleşir; tarif: zarf kumaşı + Kaldırma Gazı Kesesi + kompozit panel + zincir |

### Merkür `spacebreaker:mercury`
| Durum | İçerik | Not |
|---|---|---|
| 🎨 | Arazi | yoğun kraterler, loblu sarplıklar, düz ovalar, parlak oyuklar, kalıcı gölgeli kutup kraterlerinde buz; dev Güneş, 430 °C gündüz / −180 °C gece |
| 🎨 | Bloklar | Merkür Regoliti, Oyuk Tortusu (kalıcı ayak izi), Merkür Kabuğu, Düşük Yansıtmalı Kaya, Grafit Cevheri (→ grafit), Merkür Kutup Buzu (gölgeli kraterlerde, grafit lekeli; `c:ices/water`) |
| 🎨 | Zindan | Terk Edilmiş Kutup Buzu Madeni (gölgeli krater tabanının altında) |
| 🎨 | Gölge koridoru | havasız Merkür'de güneşle arandaki her katı blok gölge yapar: gündüz güneşte ≈430 °C, gölgede −60 °C (temel giysi dayanır); terminatöre yakın alçak güneşte uzun gölgelerle gündüz tarafı geçilebilir, öğlen gölgeyi sen kurarsın; girişte/çıkışta aksiyon çubuğunda sıcaklık |
| 🎨 | 3:2 rezonansı: çift gün doğumu | gerçek 3:2 dönme–yörünge rezonansı ve e=0,206 yörünge (Kepler denklemi): günberide güneş gökte durup geri gider; bu dünya "ılık kutup" boylamında — güneş ufkun üstüne yarıya kadar çıkar, geri batar, yeniden doğar; gün uzunluğu (176 Dünya günü, oyunda sıkıştırılmış) korunur; güneş günberide 1,5 kat büyük ve parlak; gökyüzü, ışık, sıcaklık ve gölge aynı bükülmüş saati okur (`PlanetTime.addWarp`, yalnızca Merkür) |
| 🎨 | Endüstri girdileri | kırıcıda: Oyuk Tortusu → kükürt; Venüs Kükürt Kabuğu → kükürt; Kimyasal Reaktörde grafit + O₂ → CO₂ (Merkür'de yerinde metaloks) |
| 🎨 | Bulmaca ve tuzaklar (2. tur) | Buz Madeni: buzla mühürlü depo (tabelalı, kazıyarak geçilir), tahrikte toz kar püskürten donma tuzağı · Aerostat: üç gaz vanası, yalnız kükürt sarısı işaretli olan gondol kapısını açar; iskelede kapaklı döşeme tuzağı (altında örümcek ağlı kargo ağı, ölümcül değil) · Mars İstasyonu: garaj tünelinde hâlâ çalışan çevre savunması (ok tuzağı) |

### İç gezegen yörüngeleri `spacebreaker:venus_orbit`, `mercury_orbit`, `mars_orbit`
| Durum | İçerik | Not |
|---|---|---|
| 🎨 | Yörünge gökyüzü | dönen gezegen küresi (gece-gündüz sınırı), uzaklığa göre boyutlu Güneş; Venüs'te kalın parlak pus halkası, Mars'ta ince soluk halka, Merkür'de yok; uzakta Phobos/Deimos, Dünya, Merkür, Venüs |
| 🎨 | Haritalar | Venüs bulut bantları (yan yatmış Y), Merkür kraterleri + Caloris + ışınlı genç kraterler, Mars coğrafyası (Tharsis, Olympus, Valles + Noctis, kutup başlıkları); hepsi kodla, piksel stilinde |
| 🎨 | Rehber | yeni "İç Gezegenler" bölümü: Mars, Mars kaynakları, Mars'ta yaşam, Phobos ve Deimos, Venüs, bulutlarda yaşam ve harabeler, Merkür, yörüngeden görünüm (EN + TR, Gezegenlerarası Transferler ile açılır) |

### İç gezegen varış notları ve bilinen sorunlar
| Durum | İçerik | Not |
|---|---|---|
| 🎨 | Varış notları | Mars, Phobos, Deimos, Venüs ve Merkür'e ilk inişte sohbette tek satırlık ipucu (oyuncu ve dünya başına bir kez; EN + TR): Mars'ta solunamaz hava, önceden görülen fırtınalar ve panel tozu; Venüs'te yüzey yerine bulut katmanı (y 195-245); Merkür'de gölgede yolculuk ve kutup buzu |
| ⚠️ | Bilinen sorun | Venüs bulut katmanının içinden gökyüzü düz bir pus olarak görünür (kasıtlı: bulut içindesin); bulutun üstüne çıkınca Güneş görünür |
| ⚠️ | Bilinen sorun | Merkür'de çift gün doğumu var, ama oyunun günü geceden uzun olduğu için akşam çift gün batımı yerine Güneş batıda yalnızca duraklar |
| ⚠️ | Bilinen sorun | Phobos'un Güneş önünden geçişi (transit) yok: gerçek ölçekte oyun zamanında ~0,6 saniye süreceği için eklenmedi |
| ⚠️ | Bilinen sorun | Vitrin (showcase) kameralarında görüş açısı en az 30°: oyun 30'un altındaki değerleri sessizce yok sayar |

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
| ✅ | Kuyruklu yıldız modeli (`Comet`, `CometActivity`): 67P/Çuryumov–Gerasimenko, 1P/Halley, 2P/Encke, Hale–Bopp, 9P/Tempel 1 | gerçek yörüngeler; güneşe yakınken su ve toz çıkışı (Marsden yasası), koma ve kuyruk boyu, yörünge başına kütle kaybı; Rosetta ve Giotto ölçümleriyle uyumlu |

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

### Dünya'ya yakın asteroitler (ortak `TinyWorld` paketi)
| Durum | İçerik | Not |
|---|---|---|
| 🎨 | Bennu `spacebreaker:bennu` | ~0,5 km'lik moloz yığını, kömürden koyu; her yerde iri kayalar, ekvator boyunca alçak bir şişkinlik; neredeyse sıfır yerçekimi; Ay'dan sonra en ucuz hedeflerden |
| 🎨 | Ryugu `spacebreaker:ryugu` | topaç biçimli koyu karbonlu moloz; ekvatorda keskin sırt, koyu kayalar |
| 🎨 | Eros `spacebreaker:eros` | ~17 km'lik uzun, taşlı (S-tipi) asteroit; yoğun kraterli bej-gri yüzey, yanında büyük Himeros çukuru |
| 🎨 | Itokawa `spacebreaker:itokawa` | iki parçalı "fıstık"; engebeli, kayalık yüzey ve boynundaki düz çakıl ovası Muses Denizi (iniş yerinde) |
| 🎨 | Bloklar | Bennu Regoliti, Bennu Kaya Bloğu, Ryugu Regoliti, Ryugu Kayası, Eros Regoliti, Eros Kondriti, Itokawa Çakılı, Itokawa Kayası (regolit/çakıl kalıcı ayak izi tutar); su buzu yok, yakıt ikmali yok |
| 🎨 | Zindanlar | Düşmüş İkmal İniş Aracı (Bennu, Ryugu, Eros, Itokawa) → Mikro Yerçekiminde Madencilik / Spektroskopi |
| 🎨 | Ortak yüzey özelliği | enlem sırtı (`latitude_ridge`): bir enlem çizgisi boyunca gövdeyi saran yükselti (Ryugu, Bennu) |

### Kuyruklu yıldızlar (ortak `TinyWorld` paketi)
| Durum | İçerik | Not |
|---|---|---|
| 🎨 | 67P/Çuryumov-Gerasimenko `spacebreaker:churyumov_gerasimenko` | ~4 km'lik iki loblu çekirdek; kömürden koyu toz kabuğunun altında buz; güneş ufkun üstündeyken açık gökyüzündeki jet ağızlarından ince gaz/toz sütunları yükselir; yoğunluk kuyruklu yıldızın gerçek etkinliğini izler (astro modeli: Güneş'ten uzakta uykuda → jet yok, zayıf → seyrek, etkin → tam, büyük kuyruklu yıldız → en yoğun; yalnız görsel, istemci tarafı: hasar ya da itme yok, FPS'e yük bindirmez); buzu yakıt için suya eritilebilir |
| 🎨 | Encke `spacebreaker:encke` | ~5 km'lik küçük, yaşlı, neredeyse tükenmiş çekirdek; 3,3 yılda bir Güneş'e yaklaşan kısa yörünge; tozlu, tümsekli yüzey, seyrek jetler; buzu yakıt için suya eritilebilir |
| 🎨 | Halley `spacebreaker:halley` | en ünlü kuyruklu yıldız: ~15 km'lik uzun, kömürden kara çekirdek, güçlü jetler; buzu yakıt için suya eritilebilir |
| 🎨 | Tempel 1 `spacebreaker:tempel_1` | ~6 km'lik küçük, koyu çekirdek; engebeli, çukurlu zeminin arasında alçak, dik bir yamaçla biten düz ve pürüzsüz bir ova (iniş yerinin yakınında); jetler; buzu yakıt için suya eritilebilir |
| 🎨 | Bloklar | Kuyruklu Yıldız Tozu (kalıcı ayak izi), Kuyruklu Yıldız Buzu, Kuyruklu Yıldız Jeti (tüm kuyruklu yıldızlarda ortak), Encke Tozu, Encke Buzu, Halley Tozu, Halley Buzu, Tempel Tozu, Tempel Buzu |
| 🎨 | Zindan | Düşmüş İkmal İniş Aracı (67P, Encke, Halley, Tempel 1) → Mikro Yerçekiminde Madencilik / Spektroskopi |

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

### Uranüs sistemi
| Durum | İçerik | Not |
|---|---|---|
| 🎨 | Miranda `spacebreaker:miranda` | parçalanıp yeniden birleştirilmiş gibi yamalı bir dünya: eski, kraterli gri ovalar; parlak ve koyu paralel sırtlardan üç dev "yarış pisti" taç yapısı; iniş alanına yakın, yüzü katmanlı çıplak buzdan ~95 blok yüksekliğinde dev fay uçurumu (dibinde moloz yamacı); gökte 22° genişliğinde, ince koyu halkalı soluk camgöbeği Uranüs |
| 🎨 | Miranda blokları | Miranda Regoliti (kalıcı ayak izi), Miranda Ana Kaya Buzu (+ merdiven, basamak, duvar), Parlak Sırt Buzu, Koyu Bant Buzu, Miranda Karbon Yatağı (→ kömür) |
| 🎨 | Zindan | Düşmüş İkmal İniş Aracı (Miranda) |
| 🎨 | Teknoloji | Dikey Keşif (4) |
| 🎨 | Ariel `spacebreaker:ariel` | büyük uyduların en genci: parlak buz ovaları, CO₂ kırağısı, az krater; ovaları kesen dört uzun, dik duvarlı, düz tabanlı rift vadisi (taban buzlu lavla dolmuş, ortasında alçak sırt); gökte 15° genişliğinde halkalı Uranüs |
| 🎨 | Ariel blokları | Ariel Regoliti (kalıcı ayak izi), Ariel Ana Kaya Buzu (+ merdiven, basamak, duvar), Graben Tabanı Buzu, Ariel Amonyak Yatağı (→ Amonyak Kristali); Mars'ın CO₂ kırağısı |
| 🎨 | Zindan ve teknoloji | Düşmüş İkmal İniş Aracı (Ariel) → Buz Kabuğu Tektoniği (4) |
| 🎨 | Titania `spacebreaker:titania` | Uranüs'ün en büyük uydusu: gri, orta yaşlı kraterli yaylalar; üç dev düz kanyon (~64 blok derin, 90 blok geniş, soluk katmanlı duvarlar), merkez tepeli büyük eski krater havzası, kutuplarda CO₂ kırağısı; gökte halkalı Uranüs |
| 🎨 | Titania blokları | Titania Regoliti (kalıcı ayak izi), Titania Ana Kaya Buzu (+ merdiven, basamak, duvar), Kanyon Duvarı Buzu, Titania Karbon Yatağı (→ kömür) |
| 🎨 | Zindanlar ve teknoloji | Düşmüş İkmal İniş Aracı (Titania) → Tektonik Haritalama (5); Kanyon Kenarı Karakolu (yalnız dev kanyonların kenarında, kanyona bakar: kenarda uzun camlı taş kulübe, kanyonun üstüne çelik dikmelerle uzanan korkuluklu balkon ve ölçüm lambası, kablosu kanyona sarkan vinç; haritalama tezgâhı, laboratuvar ve depo sandıkları, arka kapıdan savrulmuş regolit) → Tektonik Haritalama / Dikey Keşif |
| 🎨 | Umbriel `spacebreaker:umbriel` | Uranüs'ün en karanlık uydusu: kraterlerle doymuş, isli karbon tabakasıyla kaplı çok eski yüzey; iniş yerinin doğusunda Wunda krateri ve tabanındaki parlak kırağı halkası (CO₂ kırağısı), kutuplarda CO₂ kırağısı; gökte halkalı Uranüs |
| 🎨 | Umbriel blokları | Umbriel Regoliti (kalıcı ayak izi), Umbriel Ana Kaya Buzu (+ merdiven, basamak, duvar), İsli Buz (eritilince → kömür) |
| 🎨 | Zindan ve teknoloji | Düşmüş İkmal İniş Aracı (Umbriel) → Tektonik Haritalama (5) |
| 🎨 | Oberon `spacebreaker:oberon` | Uranüs'ün en dış büyük uydusu: kraterlerle dolu eski kırmızımsı-gri buz; büyük kraterlerin çoğunun tabanı koyu dolguyla kaplı, iniş yerinin batısında ~11 km'lik (110 blok) dev dağ, doğusunda Mommur Kanyonu, kutuplarda CO₂ kırağısı; gökte küçük, halkalı Uranüs |
| 🎨 | Oberon blokları | Oberon Regoliti (kalıcı ayak izi), Oberon Ana Kaya Buzu (+ merdiven, basamak, duvar), Koyu Krater Dolgusu |
| 🎨 | Zindan ve teknoloji | Düşmüş İkmal İniş Aracı (Oberon) → Tektonik Haritalama (5) |
| 🎨 | Ortak yüzey özellikleri | fay uçurumu (`fault_cliff`), taç yapıları (`coronae`), rift vadileri (`grabens`) |

### Neptün sistemi
| Durum | İçerik | Not |
|---|---|---|
| 🎨 | Triton `spacebreaker:triton` | ters yönde dönen, yakalanmış cüce gezegen; -235 °C (ölçülen en soğuk yüzey); yuvarlak çukurlar ve keskin sırtlardan "kavun kabuğu" arazisi, pembe azot kırağısından kutup başlıkları, az krater; güney başlığında koyu azot-toz sütunları fışkırtıp rüzgârla yatık çizgiler bırakan azot gayzerleri (yakmaz, fırlatır; püskürürken çevreye toplanabilir Gayzer Tozu yağdırır); gökte 8° genişliğinde mavi Neptün |
| 🎨 | Triton blokları | Triton Kırağısı, Pembe Azot Kırağısı, Gayzer Tozu (kalıcı ayak izi), Triton Ana Kaya Buzu (+ merdiven, basamak, duvar), Azot Gayzeri, Metan Klatrat Yatağı (→ Metan Klatrat, kömür kadar yanan yakıt) |
| 🎨 | Proteus `spacebreaker:proteus` | Neptün'ün ikinci büyük uydusu (~210 km), küresel olamayacak kadar küçük, isli koyu kabuklu engebeli cisim; batısında uyduyu neredeyse parçalayan dev Pharos havzası; gökte göğün çeyreğini dolduran Neptün; temiz su buzu yok (yakıt ikmali yok) |
| 🎨 | Proteus blokları | Proteus Regoliti (kalıcı ayak izi), Proteus Kabuğu |
| 🎨 | Nereid `spacebreaker:nereid` | Neptün'ün en dıştaki büyük uydusu (~170 km), muhtemelen yakalanmış cisim: açık gri, engebeli, kraterli su buzu; uyduların en basık yörüngesi (1,4–9,7 milyon km), gökte yalnız küçük mavi bir Neptün; yakıt için buz |
| 🎨 | Nereid blokları | Nereid Kırağısı (kalıcı ayak izi), Nereid Buzu |
| 🎨 | Zindanlar | Düşmüş İkmal İniş Aracı (Proteus, Nereid) → Kriyovolkanizma; Düşmüş İkmal İniş Aracı (Triton); Gayzer Gözlem İstasyonu (güney kutup başlığında bir azot gayzerinin üstüne kurulmuş cam kubbeli istasyon: ortada çubuk halkalı canlı gayzer, kubbe tepesinden yükselen alet direği, numune laboratuvarı ve depo sandıkları, kapıdan içeri savrulmuş pembe kırağı; 2 Gayzer Böceği bekçi) → Kriyovolkanizma |
| 🎨 | Teknoloji | Kriyovolkanizma (5) |

### Plüton ve Charon
| Durum | İçerik | Not |
|---|---|---|
| 🎨 | Plüton `spacebreaker:pluto` | -230 °C, çok ince azot havası; kalp biçimli dev azot buzu ovası (sığ, düz tabanlı havza) ve üstünde yavaşça dönen düz, hafif kubbeli konveksiyon hücreleri (oluklarında metan lekeli koyu buz); ovanın batı kıyısında su buzu dağları; ekvatorda koyu kızıl tolin ovaları, kutuplarda metan kırağısı; gökte kıpırdamadan duran Charon (3,5°), ufkun hemen üstünde yumuşak mavi pus kuşağı ve içinde çok soluk ince katmanlar |
| 🎨 | Plüton blokları | Plüton Kırağısı, Kızıl Tolin Regoliti (kalıcı ayak izi), Azot Buzu (çok kaygan), Lekeli Azot Buzu, Metan Kırağısı, Plüton Ana Kaya Buzu (+ merdiven, basamak, duvar), Plüton Tolin Yatağı (→ Tolin) |
| 🎨 | Charon `spacebreaker:charon` | gri su buzu ovaları, ekvator boyunca kanyon kuşağı, kuzey kutbunda koyu kızıl kutup başlığı; gökte 7° genişliğinde, hiç kıpırdamayan Plüton |
| 🎨 | Charon blokları | Charon Regoliti, Charon Buzu, Kızıl Kutup Kırağısı, Amonyak Hidrat Yatağı (→ Amonyak Kristali → kemik tozu) |
| 🎨 | Zindanlar | Düşmüş İkmal İniş Aracı (Plüton, Charon) |
| 🎨 | Nix `spacebreaker:nix` | Plüton'un ~19 km'lik küçük, düzensiz uydusu (ortak `TinyWorld` paketi): kraterlerle dolu, çok engebeli gri su buzu, bir yanında büyük kızıl tolin lekesi; neredeyse sıfır yerçekimi; gökte Plüton ve Charon; buzu suya eritilebilir (yakıt) |
| 🎨 | Hydra `spacebreaker:hydra` | Plüton sisteminin en parlak yüzeyi: neredeyse saf su buzundan ~19 km'lik engebeli uydu; neredeyse sıfır yerçekimi; yakıt için buz |
| 🎨 | Nix ve Hydra blokları | Nix Kırağısı, Nix Buzu, Hydra Kırağısı, Hydra Buzu (kırağılar kalıcı ayak izi tutar) |
| 🎨 | Zindanlar | Düşmüş İkmal İniş Aracı (Nix, Hydra) → Kuiper Kuşağı Navigasyonu |
| 🎨 | Teknolojiler | Azot Buzulbilimi (5), Kuiper Kuşağı Seyrüseferi (5) |
| 🎨 | Ortak yüzey özelliği | çokgen hücreler (`polygon_cells`): yastık hücreler + kenar çukurları (Plüton) veya çukurcuklu "kavun kabuğu" (Triton için) |

### Faz 3 enerji (Endüstri)
| Durum | İçerik | Not |
|---|---|---|
| 🎨 | Uraninit → Uranyum (cevher ×2, ham, ham blok, külçe, parça, blok, toz) | Dünya'da en derin (y −64…−32) ve en nadir cevher; külçe yalnız ark fırınında |
| 🎨 | RTG Yakıt Peleti | sinter fırını: 4 sarı pasta; etkinliği 21 oyun gününde yarıya iner (depoda da); taşınırken pelet başına ~8 mSv/gün doz (ortam radyasyon kancasıyla giysi doz sistemine eklenir) |
| 🎨 | Sarı Pasta (`spacebreaker:yellowcake`) | kimyasal reaktör: 1 uranyum tozu + 125 mB sülfürik asit → 1 (uranyum oksit konsantresi); RTG peletinin ve zenginleştirmenin hammaddesi |
| 🎨 | Gaz Santrifüjü (`spacebreaker:gas_centrifuge`), Zenginleştirilmiş / Tükenmiş Uranyum | 4 sarı pasta → 1 zenginleştirilmiş + 3 tükenmiş uranyum, 800 tick, 160 FE/t (yavaş, çok enerji); yan yana çalışan her santrifüj yarım kademe hız ekler (4 komşuyla 3 kat) — kademeli dizi; tükenmiş uranyum preste (levha kalıbı) **Tükenmiş Uranyum Levhası** olur: `spacebreaker:radiation_shielding` etiketinde tungsten levhayla birlikte (radyasyon kalkanı tarifleri ikisinden birini alır) |
| 🎨 | Nükleer Yakıt Elemanı (`spacebreaker:nuclear_fuel_element`, etiket `spacebreaker:reactor_fuel`) | ark ocağı: 2 zenginleştirilmiş uranyum + 2 kömür tozu + 2 tungsten çubuk → 1 (uranyum karbür, tungsten kılıf, altıgen eleman); nükleer termal roket motorunun yakıtı (roketçilik); taze yakıt zayıf radyoaktif, taşınırken doz vermez — radyasyonu motor yanarken yayar |
| 🎨 | Fisyon Yüzey Güç Ünitesi (`spacebreaker:fission_reactor`) | kilovat sınıfı fisyon yüzey güç ünitesi: nükleer yakıt elemanıyla 320 FE/t (RTG'nin 8 katı), eleman başına 10 oyun günü; yakıt yalnız güç çekilirken yanar (yük takibi, tampon 64 kFE); çalışırken radyasyon yayar: 1 blokta 50 Sv/gün, uzaklığın karesiyle düşer, aradaki her katı blok dozun %30'unu geçirir (3 blok regolit → ~%3) — göm ya da regolit set yap; tarif: alüminyum levha, radyasyon kalkanı malzemesi (tükenmiş uranyum/tungsten), ileri devre, çelik + tungsten levha |
| 🎨 | Radyoizotop Jeneratör (RTG) | taze peletle 40 FE/t, gece gündüz, havasız ve güneşsiz her yerde; kalkanlı gövde (içindeki pelet doz vermez); çıkış peletin etkinliğiyle azalır |
| 🎨 | Güneş Paneli Dizisi (`spacebreaker:solar_array`) | istasyon ve karakollar için: tek blok taban, kuzey-güney 3 blok kanat, 60 FE/t tepe (3 takipçi); vakumda ufka yakın güneşte hava kaybı yok; yörüngede gezegenin gölgesine girince (turun ~1/3'ü) üretim düşer, yaşam modülü kitinin tek dizisi + yakıt deposunun tamponu gölgede yakıt basmaya yeter |
| 🎨 | Güç Göstergesi (`spacebreaker:power_monitor`) | batarya bankasının (ya da enerji depolayan herhangi bir bloğun) önüne/arkasına dayanır; ekranında doluluk %, net FE/t ve dolma/boşalma süresi; sağ tıkta özet, karşılaştırıcı çıkışı doluluğa göre |
| 🎨 | Çalışma sesleri | gezici motoru (hızla incelir, akü boşken susar, vakumda yalnız binenler duyar), elektrolizör uğultusu, kırıcı öğütmesi |
| 🎨 | Güneş paneli: "burada güneş ışığı %…" | ipucunda gezegene göre yüzde ve öğle tepe değeri, sağ tıkta anlık üretim; 1 FE/t altındaki üretim yuvarlanıp kaybolmaz, birikir |

### Araçlar (Endüstri)
| Durum | İçerik | Not |
|---|---|---|
| 🎨 | Gezegen Gezicisi (`spacebreaker:rover`) | iki kişilik, akülü (400 kFE, blok başına 50 FE → ~8000 blok); düşük yerçekiminde az tutuş ve uzun sıçrayışlar, buzda (blok sürtünmesi) zor fren, zayıf direksiyon ve kayma — Plüton azot buzunda en kaygan, sert inişte yaylanır; toz parçacıkları, gevşek zeminde (Ay regoliti, Mars toprağı) tekerlek izi; karanlıkta kendiliğinden yanan farlar; 27 yuvalı kargo (eğilip tıkla); güneş paneli ve RTG (+ pelet) yükseltmesi; Mars'ta tavan paneli tozlanır (tam tozda %60, göstergede toz %), fırçayla ya da yanından geçen toz hortumuyla temizlenir; batarya bankası ya da kablonun yanına park edince şarj; sürücüye akü/hız göstergesi |

## Ses
| Durum | İçerik | Not |
|---|---|---|
| 🎨 | Görev kontrol anonsları (Türkçe + İngilizce, oyun diline göre) | geri sayım 10→1, ateşleme, kalkış, kademe ayrılması, yörünge, atmosfere giriş, paraşüt, iniş, yakıt/oksijen uyarısı, geri sayım durdu; telsiz filtresi + sinyal sesleri; istemci ayarı `missionControlVoice` |
| 🎨 | Roket sesleri | motor döngüsü, ateşleme, kademe ayrılması, paraşüt, atmosfere giriş gürültüsü, uydu bırakma, sonda inişi |
| 🎨 | İstasyon, araç ve makine sesleri | hava kilidi döngüsü; gezici motoru, elektroliz ünitesi, matkap, gaz santrifüjü (yüksek rotor vızıltısı) ve fisyon reaktörü (alçak uğultu) için kesintisiz 5 sn döngüler (`SbSounds`) |
| 🎨 | Giysi | oksijen alarmı |
| 🎨 | Müzik | sekiz özgün ortam parçası (sözsüz): Yörünge (yumuşak piyano + sentezleyici), Ay (alçak uğultular, cam çan sesleri), Mars (sıcak pad'ler, uzak tek melodi), Venüs (havadar cam tınılar, derin org uğultusu), Titan (amber pus, yumuşak düşen notalar), Jüpiter uyduları (kabaran pad'ler, buz çanları), Buz devleri — Uranüs, Neptün, Plüton ve uyduları (seyrek derin uğultular, uzak tek çanlar), Asteroit kuşağı (yumuşak arpej, uzak metal tınılar); biyomların `music` alanından çalar |

## Yaratıklar (gezegenlere özgü, kan yok)
| Gök cismi | Yaratıklar |
|---|---|
| Ay 🎨 | Regolit Kenesi (tozun altına saklanır, düşük yerçekiminde sıçrar), Yaşlı Regolit Kenesi (lav tüpü muhafızı), Toz Yüzücüsü (regolitin hemen altında süzülen pasif canlı, arkasında toz dalgaları), Keşif Dronu (karakol muhafızı, sersemletme atışı), Parıltı Böceği (pasif, ışıyan; fırçayla Parıltı Kabuğu verir → elektriksiz Parıltı Lambası, Ay gecesi için), Isı Emici (yalnız Ay gecesinde çıkar, lamba ışığında doğmaz; dokunuşu dondurur, akü ve jeneratörlerden enerji emip parlar, gün doğunca süblimleşir; ölünce emdiği enerji kızıltaşa döner) |
| Mars 🎨 | Pas Böceği (demir oksit yer, tarafsız), Toz Şeytanı (fırtınalarda canlı hortum), Kanyon Pusucusu, Taş Otlayan (iri, pasif otlayıcı), Kutup Kurtçuğu |
| Venüs (bulutlar) 🎨 | Asit Vatozu (süzülen, tarafsız; kükürt ve deri bırakır), Bulut Medüzü (pasif gaz kesesi; Kaldırma Gazı Kesesi bırakır: kullanınca 30 sn yavaş düşüş) |
| Merkür 🎨 | Alacakaranlık Sürüngeni (tehlikeli; grafit bırakır; gece-gündüz sınırında yaşar; güneş yükselince ya da gece çökünce toprağa gömülür), Oyuk Salyangozu (pasif, faydalı: oyuk tortusunu kazıyıp kükürt bırakır, kükürtle çoğalır) |
| Io 🎨 | Kükürt Yengeci (ısıya dayanıklı, yakınına gelene saldırır) |
| Europa 🎨 | Buzaltı Işık Yılanı (biyolüminesan, pasif; tuzlu su havuzlarında ve gömülü okyanusta) |
| Ganymede 🎨 | Oluk Otlayıcısı (zırhlı, sürü halinde, okyanus tuzuyla çoğalır) |
| Kallisto 🎨 | Krater Sıçrayıcısı (ürkek, düşük yerçekiminde uzun sıçrayışlarla kaçar) |
| Titan 🎨 | Metan Kayıcısı (metan göllerinin üstünde yürür) · Tolin Solucanı (kumul denizlerinde halkalı, ürkek kazıcı: yaklaşınca kuma gömülür, sessizlikte çıkar, kumu eleyip ara sıra tolin bırakır; tolin kumuyla çoğalır) |
| Enceladus 🎨 | Gayzer Süzülücüsü (gayzer bulutlarında süzülen parlak çan) |
| Titania 🎨 | Moloz Yuvarlakçası (plakalı otlayıcı; vurulunca top olup hızla yuvarlanarak kaçar, top halindeyken hasarın bir kısmını alır; kömürle çoğalır) |
| Umbriel 🎨 | Alacakaranlık Sıçrayanı (uzun arka bacaklı koyu otlayıcı; ovalarda yavaş, yüksek sıçrayışlarla gezer; vurulunca karbon tozu bulutu kaldırıp yakınındakileri kısa süre kör eder; kömürle çoğalır) |
| Oberon 🎨 | Kenar Kazıcısı (geniş pençeli basık otlayıcı; gevşek zeminde vurulunca kendini toprağa gömer ve bir süre hasar almaz, sert buzda yalnızca kaçar; amonyak kristaliyle çoğalır) |
| Ariel 🎨 | Kırağı Güvesi (soluk, geniş kanatlı; vadi çıkıntılarının altında tüner, karanlıkta uçar) |
| Miranda 🎨 | Uçurum Yapışkanı (beş kollu, yassı otlayıcı; duvarlara tırmanır, düşme hasarı almaz; kömürle çoğalır) |
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
