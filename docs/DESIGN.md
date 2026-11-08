# Spacebreaker — Tasarım Belgesi

Sürüm hedefi: NeoForge **1.21.1** (asıl), ardından 26.2 portu · Mod id: `spacebreaker` ·
Paket: `com.pizzaparker07.spacebreaker` · Diller: Türkçe + İngilizce (diğerleri sonra).

## 1. Vizyon
Gerçekçi ama eğlenceli, teknik bir uzay modu. Oyuncu Dünya'da roket mühendisliğiyle başlar,
yörüngeye, Ay'a, Güneş Sistemi'ne, yakın yıldızlara ve en sonunda başka galaksilere ulaşır.
Hikâye hafiftir: Görev Kontrol sesi, ajans görevleri ve uzak sistemlerde nadir gizemli sinyaller.

**Kurallar:** özgün içerik (marka, karakter, isim, görsel yok), kan yok, tüm dokular kodla çizilir,
kredi yalnızca PizzaParker07 / Pizzacı Çocuk.

## 2. Evren
| Katman | İçerik |
|---|---|
| Güneş Sistemi (gerçek veriler) | Merkür, Venüs, Dünya + Ay, Mars + Phobos/Deimos, Ceres, Vesta, Io, Europa, Ganymede, Callisto, Titan, Enceladus, Triton, Plüton + Charon; gaz devlerine ve Güneş'e iniş yok (gaz toplama var) |
| Yakın yıldızlar (gerçek) | Alfa Centauri, Proxima b, Barnard, Sirius, Tau Ceti, TRAPPIST-1 — bilinmeyen ayrıntıyı motor tamamlar |
| Samanyolu | çubuklu sarmal, merkezde kara delik; yıldızlar prosedürel |
| Diğer galaksiler | Andromeda + prosedürel galaksiler |

## 3. Gezegen motoru
```
Evren tohumu → Galaksi → Sektör → Yıldız → Gezegen/Uydu
  FİZİK:  kütle+yarıçap → yerçekimi · ışınım+uzaklık → denge sıcaklığı · kaçış hızı → atmosfer
          gelgit kilidi · manyetik alan → radyasyon · yörünge (Kepler)
  YÜZEY:  krater sıklığı, dağ/volkan/kanyon, sıvı türü (su/metan/lav), kutuplar, renk, maden dağılımı
  OYUN:   ChunkGenerator, biyom arketipleri, renklendirilen blok aileleri, gökyüzü, yörünge görüntüsü
```
- Deterministik: aynı dünya tohumu = aynı evren; her şey ihtiyaç anında üretilir.
- Gerçek ve prosedürel gök cisimleri **aynı JSON şemasını** kullanır (datapack ile genişletilebilir).
- z ekseni = enlem: kuzeye/güneye gittikçe kutuplara ulaşılır.
- Güneş Sistemi gök cisimleri sabit boyutlardır; prosedürel gezegenler çalışma anında açılan boyutlardır
  (kendi boyut yöneticimiz; boşalınca kapanır, ziyaret edilmeyen yer diske yazılmaz).
- Fizik katmanı saf Java'dır ve JUnit ile gerçek değerlere karşı test edilir.

## 4. Sistemler
- **Roketler:** Montaj Tezgâhı'nda parçalardan tasarım; gerçek Isp, Tsiolkovsky Δv, itki/ağırlık;
  en fazla 3 kademe; kalkış/iniş sinematikleri; mahsur kalmama (Acil Dönüş Kapsülü).
- **Yörünge mekaniği:** Kepler yörüngeleri (zaman ölçekli), Hohmann transfer maliyeti,
  fırlatma pencereleri, atmosferle frenleme.
- **Hayatta kalma:** oksijen (tüp + oda dağıtıcısı), vücut ısısı (yavaş değişen), radyasyon dozu, basınç
  (yalnız gerektiği yerde). EVA giysi kademeleri Mk1–Mk5, kask HUD.
- **Enerji:** FE; güneş paneli (gezegenin güneşe uzaklığına göre verim), yanmalı jeneratör (yalnız
  oksijenli atmosferde), jeotermal, fırtına türbini, RTG, füzyon.
- **Yerinde üretim:** Ay buzu → su/O₂/H₂; Mars CO₂ + H₂ → metan (Sabatier); Titan metan gölleri.
- **Keşif:** teleskop (transit yöntemi), spektroskopi, sondalar, uydular; ilk keşfeden adlandırır.
- **Yaratıklar:** Ay ve gezegenlerde ortama uyumlu özgün canlılar (bkz. CONTENT.md).
- **Görev Kontrol:** geri sayım, uyarılar; seslendirme TR + EN.

## 5. Görsel kalite
- 16×16 vanilla uyumlu piksel sanatı; her malzemenin kendi renk rampası; ışık sol üstten.
- Makineler gerçek 3B; animasyonlu parçalar BlockEntityRenderer ile.
- Eşyalar: envanterde 2B ikon, elde 3B model (`neoforge:separate_transforms`).
- Giysi: özel 3B model + saydam vizör/altın güneş vizörü/parlayan lamba katmanı.
- Tüm bloklara LabPBR `_n` / `_s` haritaları.
- Her içerik paketi gerçek istemcide vitrin dünyasında fotoğraflanır ve onaydan geçer.

## 6. Shader / Sodium uyumu
1. Tam ekran efektler (geçişler, iniş ateşi tonu, vizör, HUD, geri sayım, sinematik bantlar,
   jenerik) **GUI katmanında** çizilir → shader paketinin son karesinin üstünde kalır.
2. Gökyüzü `DimensionSpecialEffects` ile; Iris algılanırsa (zorunlu bağımlılık değil)
   `auto / vanilla / shader-safe` ayarı.
3. Ağır efektlerin (kara delik bükülmesi gibi) shader açıkken basitleştirilmiş sürümü.
4. Her fazda vanilla, Sodium ve Sodium + Iris + shader ile otomatik ekran görüntüsü.

## 7. Performans ve çok oyunculu
- Oksijen taşkın dolumu, büyük yapılar, biyom haritası tick'lere yayılır.
- İstemci efekt kalitesi ayarı (düşük/orta/yüksek).
- Sunucu otoriter; roket durumu, sinematik zamanlaması, oksijen alanları sunucuda, istemcilere paketle.

## 8. Test ve teslim
- **GameTest** (sunucu), **JUnit** (fizik), **gerçek istemci senaryoları** (`runShowcase`, Xvfb).
- CI: her push'ta derleme + GameTest; `v*` etiketi → Actions jar'ı derleyip GitHub Release açar.
- Beta: 1.21.1'e her yeşil push'tan sonra Actions `v<sürüm>-beta.N` ön sürümünü jar'la otomatik açar (`.github/workflows/beta.yml`); yalnız belge değişen push'lar beta açmaz.
- Her faz sonunda jar + TR/EN sürüm notları.

## 9. Yol haritası
| Faz | Ad | Özet |
|---|---|---|
| 1 | Ay | altyapı, motor v1, roket tasarımı, rampa, kalkış, yörünge, Ay'a iniş/dönüş, oksijen, giysi, HUD |
| 2 | İç Sistem | Mars + uyduları, Venüs bulut platformları, Merkür, Kepler + fırlatma pencereleri, istasyon, uydu, sonda, yerinde üretim, araştırma |
| 3 | Dış Sistem | asteroitler, Jüpiter/Satürn uyduları, Plüton; nükleer/füzyon motor, gaz toplama, radyasyon |
| 4 | Yıldızlararası | galaksi üretici, gerçek yakın yıldızlar, prosedürel sistemler, warp, keşif ve adlandırma |
| 5 | Yaşam | prosedürel bitkiler, hayvanlar, biyomlar |
| 6 | Galaksiler | diğer galaksiler, kara delikler, nötron yıldızları |
| 7 | Cila ve v1.0 | ses/müzik, rehber kitap, wiki, denge |
| 8 | 26.2 portu | |
