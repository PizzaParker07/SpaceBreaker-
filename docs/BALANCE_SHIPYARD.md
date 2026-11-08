# Tersane parçaları için malzeme maliyeti önerisi (2026-11-08)

Bu belge sanayi ekibinin önerisidir; kod değişikliği içermez, uygulama roketçilik ekibindedir. Parçalar ve kütleleri
`content/rocketry/ship/ShipPart.java`'dan alındı. Enerji rakamları `docs/BALANCE_INDUSTRY.md`'deki makine tariflerinden
hesaplandı; balans kararlarından sonraki değerler kullanıldı (fisyon 240 FE/t, regolit ısıtıcı 80 FE/t, pumpjack çelik levha).

## 1. Ara ürünlerin yaklaşık enerji maliyeti

Fırında eritme ve tezgâh 0 FE sayıldı. Pres işlemi 1200 FE, ark ocağı ise işlemine göre (FE/işlem).

| Ara ürün | Yol | ≈ FE / adet |
|---|---|---|
| Alüminyum levha | boksit → fırın → pres | 1,2 k |
| Al-Li levha | 3 Al + 1 Li → ark (4 külçe, 16 k) → pres | 5,2 k |
| Çelik levha / çubuk | demir + kömür tozu → ark 9,6 k → pres | 10,8 k |
| Titanyum levha | ham rutil → ark 12,8 k → pres | 14 k |
| Tungsten levha / çubuk | ham volframit → ark 12,8 k → pres | 14 k |
| Süperalaşım levha | 3 Ni + Ti + çelik → ark (4 külçe) → pres | 12,8 k |
| Temel devre | bakır levha, kızıltaş (tezgâh) | ~3 k |
| İleri devre | 2 temel devre + 2 gofret + altın tel + plastik → imalatçı | ~25 k |
| Elektrik motoru | 2 çelik levha + 4 bakır tel + çelik çubuk → imalatçı (2 adet) | ~19 k |
| Güneş hücresi | gofret + bakır tel + cam → imalatçı | ~10 k |
| Turuncu köpük yalıtım | 2 plastik + boya → imalatçı (4 adet) | ~3,6 k |
| Altın folyo | altın levha + plastik → imalatçı (4 adet) | ~2,4 k |
| Ablatif kalkan levhası | 2 plastik + 2 kömür tozu + Ti levha → imalatçı (2 adet) | ~16 k |
| Tükenmiş uranyum levha | santrifüj yan ürünü → pres | ~2 k |
| Nükleer yakıt elemanı | 2 zenginleştirilmiş U (2 × 128 k) + ark 48 k + tungsten çubuk | ~330 k |

## 2. Parça başına önerilen malzeme

Ölçü: kütle ve görevle orantılı. Hafif yapı Al-Li, sıcak kısımlar süperalaşım ve tungsten, elektronik ileri devre.

| Parça | Kütle | Önerilen malzeme | ≈ FE |
|---|---|---|---|
| Kokpit + burun portu | 2500 kg | 12 Al-Li levha, 4 titanyum levha, 2 ileri devre, 2 temel devre, 4 cam panel, 1 yanaşma portu, 2 silikon conta | ~175 k |
| Kabin | 1800 kg | 10 Al-Li levha, 4 turuncu köpük, 2 silikon conta, 1 temel devre, 2 cam panel | ~70 k |
| Kargo bölmesi | 1200 kg | 8 alüminyum levha, 4 çelik çubuk, 1 temel devre, 1 sandık | ~55 k |
| Küçük tank (4 t yakıt) | 400 kg | 8 Al-Li levha, 4 turuncu köpük, 1 silikon conta | ~56 k |
| Büyük tank (12 t yakıt) | 900 kg | 20 Al-Li levha, 8 turuncu köpük, 2 titanyum levha, 2 silikon conta | ~160 k |
| Metan motoru (900 kN, 380 s) | 1500 kg | 6 süperalaşım levha, 4 titanyum levha, 2 tungsten levha (boğaz), 2 elektrik motoru (turbopompa), 1 ileri devre | ~225 k |
| Nükleer motor (300 kN, 900 s) | 3200 kg | 6 süperalaşım levha, 8 tungsten levha, 4 tükenmiş uranyum levha (kalkan), 2 elektrik motoru, 2 ileri devre; ilk yakıt elemanı ayrı | ~285 k + ~330 k/eleman |
| RCS kümesi | 120 kg | 2 titanyum levha, 1 çelik çubuk, 1 temel devre, 1 silikon conta | ~42 k |
| Güneş kanadı çifti | 300 kg | 8 güneş hücresi, 4 alüminyum levha, 2 demir çubuk, 1 temel devre | ~90 k |

Notlar:
- Ablatif kalkan levhası artık ısı kalkanında kullanılıyor. Atmosfere dönüş yapacak gemiler için kokpite 2 ablatif levha eklenebilir (~32 k).
- Silikon conta ve cam panel ucuz; tank ve kabine sızdırmazlık hissi veriyor.
- Süperalaşım ve tungsten motorlarda tutulursa ileri madenleri (pentlandit, volframit) gemiye bağlar; bu ilerleme sırasıyla uyumlu.

## 3. Örnek iki gemi

Yakıt ayrıca hesaplandı. Yanaşmış gemi yakıtı istasyondan alıyor, yine de üretim enerjisi gösterildi.

"İlk gemi 7×5×11" ölçüsü (ASTRO.md) `ShipPart` boylarıyla birebir tutmuyor. Kokpit 3 + kargo 4 + küçük tank 3 + motor 3 = 13 blok ediyor. Bu yüzden en yakın düzen varsayıldı.

| Gemi | Parçalar | Parça enerjisi | Yakıt |
|---|---|---|---|
| **İlk gemi** (~7×5×11) | kokpit, kargo bölmesi, küçük tank, metan motoru, 4 RCS | ~680 k FE | 4 t metaloks (O/F 3,6): 2060 mB sıvı metan + 2740 mB LOX ≈ 150 k FE (sıvılaştırma + elektroliz; metan rafineri ya da Sabatier'den) |
| **Nükleer transfer aracı** | kokpit, kabin, kargo bölmesi, 2 büyük tank, nükleer motor, 8 RCS, 2 güneş kanadı çifti | ~1,4 M FE + yakıt elemanı ~330 k | 24 t sıvı hidrojen ≈ 338 000 mB LH₂ ← 1,35 M mB H₂: sıvılaştırma ~17 M FE + elektroliz ~6,5 M FE ≈ **24 M FE** |

## 4. İlk roketle karşılaştırma

| | Parçalar | Yakıt | Gereken makineler |
|---|---|---|---|
| İlk yörünge roketi | ~0 FE elle (presle ~40 k) | ~430 k FE | ~420 k FE (temel set) |
| İlk gemi | ~680 k FE | ~150 k FE | + imalatçı, ark ocağı, Sabatier/rafineri (zaten var) |
| Nükleer transfer aracı | ~1,7 M FE | ~24 M FE | + santrifüj zinciri, fisyon ünitesi önerilir |

Parça enerjisinde ilk gemi ilk roketin yaklaşık 1,6 katı; ara aşama için makul bir adım. Nükleer aracın parçaları
3–4 katı tutuyor, bu da uygun.

**Uyarı:** asıl yük, nükleer aracın sıvı hidrojenidir: ~24 M FE. Tek fisyon ünitesi (240 FE/t) bunu ~5 oyun gününde
karşılar, yanmalı jeneratörle pratik değil. Seçenekler (roketçiliğin kararı):
- büyük tankı nükleer motorla 6 t LH₂'ye sınırlamak ya da tankı küçültmek;
- istasyon yakıt deposunun sıvılaştırmayı yavaş yavaş, kendisi yapması;
- sıvılaştırma tarifini LH₂ için 160 → 100 FE/t'ye indirmek (sanayi tarafında yapılabilir, onay gerekir).

## 5. Karar (2026-11-08)

- Sıvı hidrojen sıvılaştırması **160 → 100 FE/t** oldu (sanayi, `liquefaction/hydrogen`, 80 tick). 1000 mB H₂ → 250 mB LH₂
  artık 8 k FE (önce 12,8 k). Nükleer transfer aracının 24 t LH₂'si yaklaşık 17 M FE'ye iniyor: sıvılaştırma ~10,8 M,
  elektroliz ~6,5 M. Bu geç oyun hedefi; birkaç fisyon ünitesiyle birkaç günde dolması kabul edildi.
- Tank sınırı yok.
- İlk geminin boyunu roketçilik kendi ölçüsünde düzeltecek.
- Ablatif levha ısı kalkanında (roketçilik 7ffa605d).
