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
1. Güneş soldadır; **Buradasın** iğnesi nerede olduğunu gösterir. Bir gezegene tıkla: sistemine yakınlaşır. Geri dönmek için **◀ Güneş Sistemi** ya da sağ tık.
2. Sağdaki panel seçtiğin dünyanın resmini, kısa açıklamasını ve tehlikelerini gösterir: **Hava yok**, **Solunamaz hava**, **Ezici basınç**, **Sıcak**, **Soğuk**, **Radyasyon**.
3. **Yörüngeye git** ya da **Yüzeye in**'i seç. (Henüz kurulmamış bir dünyada **Yakında**, yüzeyi olmayan bir cisimde **İnilecek yüzey yok** yazar.)
4. Panel şunları yazar:
   - **Gereken yakıt X · roketinde Y** → **Yeter ✔** ya da **X eksik ✘**
   - **Geri dönüş yakıtı:** yeter ✔ / yetmez ✘
   - **Yolculuk süresi:** X gün
   - **Fırlatma penceresi:** şu an uygun, ya da en iyisi biraz sonra (şimdi gitmek daha çok yakıt ister)
5. Rota haritada numaralı adımlarla çizilir. Büyük **Git** düğmesine bas. Düğme pasifse nedenini yazar.

Bir gezegenin yanındaki küçük **istasyon simgesi**, yörüngesinde bir uzay istasyonu olduğunu gösterir; yan panel onu kimin kurduğunu ve kaç yanaşma portu olduğunu yazar. → [Uzay İstasyonu](space_station)

Uçarken göstergede **Kalan yakıt (Δv)** satırını izle.

> **Δv**, roketinin hızını ne kadar değiştirebileceğidir; kısaca: yakıtın.

## Ay'a iniş
- Ay yörüngesinden yüzeye yaklaşık **1870 m/s** gerekir. Dönüş için de bir o kadar.
- **İniş bacakları** olmadan inemezsin.
- Sert inişte roket hasar görür; manevra iticileri inişi yumuşatır.
- Üssüne bir {item spacebreaker:landing_beacon} koy: yörüngeden inen roketler en yakın işarete iner ("İniş işareti alındı"). Havası olan dünyalarda rüzgâr inişi altı bloğa kadar kaydırabilir. Yıldız haritası üsleri ve iniş işareti sayılarını istasyonların yanında gösterir.
- **Ay mekiği:** tek kademeli, yeniden kullanılır bir roket (vakum motoru, orta tank, iniş bacakları, kapsül, manevra iticileri). Ay yörüngesindeki istasyon ile yüzeydeki üs arasında tek tankla gidip gelir, porta yanaşıp yakıt deposundan dolar. → [Uzay İstasyonu](space_station)
- **Mars iniş aracı:** metaloks vakum motoru, büyük tank, iniş bacakları, ısı kalkanı, kapsül, paraşüt ve manevra iticileri. Mars yörüngesinden yüzeye iner; Mars'ta üretilen metan ve oksijenle doldurunca yörüngeye geri döner. → [Yerinde Üretim](isru)

## Atmosfere giriş
Isı kalkanının kademesi önemlidir: biri **alçak yörüngeden** dönüşe, daha iyisi **Ay'dan** doğrudan dönüşe dayanır. İpucu satırında yazar.

{items spacebreaker:emergency_beacon}

**Acil dönüş işareti:** Dünya dışında kullanınca seni eve getirecek bir kapsül çağırır. Dünya'da işe yaramaz.
