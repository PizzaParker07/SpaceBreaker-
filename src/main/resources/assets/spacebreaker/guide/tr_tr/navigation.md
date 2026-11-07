---
title: Uçuş ve Yön Bulma
icon: spacebreaker:emergency_beacon
items: [spacebreaker:emergency_beacon, spacebreaker:landing_beacon]
research: spacebreaker:orbital_mechanics
---
## Roketteyken kamera
Rokete binince kamera roketin çevresinde dönen **üçüncü şahıs** görünümüne geçer.
- Fare: roketin çevresinde dön.
- Fare tekerleği: yakınlaş / uzaklaş.
- {key key.togglePerspective}: kokpit penceresi.

Yörüngeye çıkarken, gezegenler arasında ve inişte yükleme ekranı yoktur; bir geçiş animasyonu oynar.

## Yıldız haritası
Roket **yörüngedeyken** {key key.spacebreaker.star_map} haritayı açar. Yüzeyden önce rampadan fırlatman gerekir.
1. Bütün Güneş Sistemi tek bir haritadır: her dünya yörüngesinde gerçekte bulunduğu yerde, kendi renklerinde küçük bir disk olarak durur. Kuyruklu yıldızlar ve Dünya'ya yakın asteroitler yörüngelerinde küçük noktalardır. Küçük bir işaret roketinin nerede olduğunu gösterir.
2. **Tekerlekle** yakınlaş, **sürükleyerek** kaydır, **sağ tıkla** her şeyi yeniden gör. Bir dünyaya tıkla: görünüm ona doğru kayar ve uydularını görecek kadar yaklaşır.
3. Kısa panelde dünyanın adı ve türü, **Yörünge** ya da **Yüzeye in** seçimi ve tehlikeleri renkli kelimelerle yazar: **Hava yok**, **Solunamaz hava**, **Ezici basınç**, **Sıcak**, **Soğuk**, **Radyasyon**.
4. Yolculuk için **Δv** ve **Süre** büyük rakamlarla, tek bir yakıt çubuğu (gereken yakıt ve roketteki yakıt), bir dönüş satırı (**Dönüş: yakıt yeter**, **Dönüş: orada yakıt üret** ya da **Dönüş: yakıt yetmez**), fırlatma penceresi ve yalnız en önemli uyarı görünür.
5. Rota haritada ince, kesik çizgili bir yaydır. **Git**'e bas. Düğme gri ise panel nedenini söyler.

Bir dünyanın yanındaki küçük **istasyon** işareti, yörüngesinde bir uzay istasyonu olduğu anlamına gelir. → [Uzay İstasyonu](space_station)

Uçarken göstergedeki **Kalan yakıt (Δv)** satırını izle.

> **Δv**, roketinin hızını ne kadar değiştirebileceğidir; kısacası yakıtın.

## Ay'a iniş
- Ay yörüngesinden yüzeye yaklaşık **1870 m/s** gerekir. Dönüş için de bir o kadar.
- **İniş bacakları** olmadan inemezsin.
- Sert inişte roket hasar görür; manevra iticileri inişi yumuşatır.
- Uzun yolculuklarda kriyojenik yakıt buharlaşır; yolculuk kartı ne kadarını gösterir. → [Buharlaşma](rocket_parts#boil_off)
- Üssüne bir {item spacebreaker:landing_beacon} koy: yörüngeden inen roketler en yakın işarete iner ("İniş işareti alındı: üsse iniliyor"). Havası olan dünyalarda rüzgâr inişi altı bloğa kadar kaydırabilir. Yıldız haritası üsleri ve iniş işareti sayılarını istasyonların yanında gösterir.
- **Ay mekiği:** tek kademeli, yeniden kullanılır bir roket (vakum motoru, orta tank, iniş bacakları, kapsül, manevra iticileri). Ay yörüngesindeki istasyon ile yüzeydeki üs arasında tek tankla gidip gelir, porta yanaşıp yakıt deposundan dolar. → [Uzay İstasyonu](space_station)
- **Mars iniş aracı:** metaloks vakum motoru, büyük tank, iniş bacakları, ısı kalkanı, kapsül, paraşüt ve manevra iticileri. Mars yörüngesinden yüzeye iner; Mars'ta üretilen metan ve oksijenle doldurunca yörüngeye geri döner. → [Yerinde Üretim](isru)

## Atmosfere giriş
Isı kalkanının kademesi önemlidir: biri **alçak yörüngeden** dönüşe, daha iyisi **Ay'dan** doğrudan dönüşe dayanır. İpucu satırında yazar.

{items spacebreaker:emergency_beacon}

**Acil dönüş işareti:** Dünya dışında kullanınca seni eve getirecek bir kapsül çağırır. Dünya'da işe yaramaz.
