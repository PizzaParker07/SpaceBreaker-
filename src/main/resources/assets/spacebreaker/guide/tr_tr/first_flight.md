---
title: İlk Uçuşun
icon: spacebreaker:rocket
research: spacebreaker:basic_rocketry
---
Rampadan yörüngeye ve eve dönüşe, adım adım.

## 1. Hazırlık
- Dört parçalı [EVA giysini](eva_suit) giy, iki dolu tüp tak. Kapsül basınçlıdır: koltukta otururken giysisiz de nefes alırsın, ama dışarı adım attığın an yine vakumdasın.
- Yedek tüp ve bir {item spacebreaker:emergency_beacon} al.
- [Fırlatma rampanı](launch_pad) kur ve konsolda kontrol et.

## 2. Gerekenler
- {tech spacebreaker:basic_rocketry}: kapsül, burun konisi, montaj tezgâhı. Diski gözlemevinde.
- {tech spacebreaker:launch_infrastructure}: rampa ve kule.
- {tech spacebreaker:liquid_fuel_engines} ve {tech spacebreaker:staging}: motor, tanklar, ayırıcı.
- {tech spacebreaker:vacuum_engines}: üst kademe motoru. Gözlemevi arşivindeki **Yörünge Altı Uçuş** verisini laboratuvarda incele.

## 3. Roketi tasarla
[Montaj tezgâhında](rocket_assembler) en az iki kademeli, kapsüllü bir roket yap. Yörünge için yaklaşık **9400 m/s** Δv gerekir.

>! İlk roketinde **ısı kalkanı ve paraşüt olmaz**: onları {tech spacebreaker:landing_systems} açar, o da **Yörünge Uçuşu** verisi ister. Bu veriyi yörüngeye ilk çıktığında alırsın. Eve {item spacebreaker:emergency_beacon} ile dön.

## 4. Rampaya koy ve doldur
1. {item spacebreaker:rocket} elindeyken ortadaki kanal ızgarasına {key key.use}.
2. Bir tank RP-1'i ve bir tank sıvı oksijeni borularla {item spacebreaker:fuel_umbilical}'a bağla. Hat roketi doldurur.
3. Konsolda yakıt yüzdesini izle.

{page}

## 5. Bin ve fırlat {#launch}
1. Rokete {key key.use} ile bin. Kamera roketin çevresine geçer: fareyle döndür, tekerlekle yakınlaş, {key key.togglePerspective} ile kokpite geç.
2. Konsoldaki listeye bak: rampa, yakıt, Δv ve itki/ağırlık yeşil olmalı.
3. **Fırlat**'a bas ya da roketteyken {key key.spacebreaker.launch} tuşuna bas. Geri sayım başlar; aynı tuşla durdurabilirsin.

> [!WARNING]
> Yakıt yetmezse kapsül ayrılır ve paraşütle iner. Ama ısı kalkanın yoksa yüksekten düşmek ölümcül olabilir.

## 6. Yörüngede
Yükleme ekranı yoktur: kısa bir geçişten sonra yörüngeye varınca bir mesaj gelir ve çantana iki **Yörünge Uçuşu** verisi düşer. {key key.spacebreaker.star_map} ile **yıldız haritasını** aç, bir dünyaya tıkla ve **Yörüngeye git** ya da **Yüzeye in**'i seç. Harita yakıtın yetip yetmediğini düz dille söyler. Ay yörüngesi için yaklaşık **4000 m/s** daha gerekir. → [Uçuş ve Yön Bulma](navigation)

## 7. Eve dönüş
- **İlk uçuşta:** {item spacebreaker:emergency_beacon} kullan. Isı kalkanlı ve paraşütlü bir kapsül gelir, seni eve götürür.
- **Sonraki uçuşlarda:** kapsülünün altında **ısı kalkanı**, üstünde **paraşüt** olsun. Yıldız haritasında Dünya'yı seçip **Yüzeye in**; kalkan ısıyı alır, paraşüt açılır.

> Eve dönünce Yörünge Uçuşu verisini laboratuvarda incele: {tech spacebreaker:landing_systems} ile ısı kalkanı, paraşüt ve iniş bacakları açılır. Bunun için {tech spacebreaker:precision_manufacturing} da gerekir: ısı kalkanının yapıldığı ablatif kalkan levhalarını fabrikatör üretir. Ay'a inmek için bacaklar şart.
{recipe spacebreaker:rocketry/heat_shield}
