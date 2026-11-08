---
title: Nükleer Yakıt
icon: spacebreaker:nuclear_fuel_element
items: [spacebreaker:uraninite_ore, spacebreaker:raw_uraninite, spacebreaker:uranium_dust, spacebreaker:yellowcake, spacebreaker:gas_centrifuge, spacebreaker:enriched_uranium, spacebreaker:depleted_uranium, spacebreaker:depleted_uranium_plate, spacebreaker:nuclear_fuel_element, spacebreaker:rtg_fuel_pellet]
research: spacebreaker:nuclear_fission
---
Uraninit cevherinden nükleer termal roketin yakıtına giden yol.

## Zincir
1. {item spacebreaker:raw_uraninite}'i kırıcıda **uranyum tozuna** öğüt.
2. Kimyasal reaktörde toz + **125 mB sülfürik asit** → {item spacebreaker:yellowcake}.
3. {item spacebreaker:gas_centrifuge}'nde 4 sarı pasta → **1** {item spacebreaker:enriched_uranium} + **3** {item spacebreaker:depleted_uranium}.
4. Elektrik ark fırınında 2 zenginleştirilmiş uranyum + 2 kömür tozu + 2 tungsten çubuk → {item spacebreaker:nuclear_fuel_element}.
{recipe spacebreaker:chemical_reaction/yellowcake}
{recipe spacebreaker:centrifuging/enriched_uranium}

{page}
## Santrifüj dizisi
Tek bir santrifüj yavaştır. Gerçek tesislerde binlercesi art arda çalışır. Burada aynı seviyede yanına konan ve **çalışan her santrifüj yarım kademe hız** ekler; en çok dört komşu, yani en fazla **3 kat**. Santrifüjleri bir sıra ya da artı biçiminde diz.

> Her zenginleştirmede büyük bir yığın {item spacebreaker:depleted_uranium} çıkar: zenginleştirmenin doğal yan ürünüdür. Çok yoğundur: preste levha kalıbıyla {item spacebreaker:depleted_uranium_plate} olur ve tungsten levhayla birlikte **radyasyon kalkanı** malzemesi sayılır ({tech spacebreaker:radiation_shielding}). Fisyon güç ünitesinin ve nükleer termal motorun tarifinde kullanılır.
{recipe spacebreaker:pressing/depleted_uranium_plate}

{recipe spacebreaker:arc_smelting/nuclear_fuel_element}

## Fisyon güç ünitesi {#fission}
{items spacebreaker:fission_reactor}
{item spacebreaker:fission_reactor}, kilovat sınıfı bir fisyon yüzey güç ünitesidir. İçine bir {item spacebreaker:nuclear_fuel_element} koy: gece gündüz, nerede olursan ol **240 FE/t** verir. Bir eleman **10 oyun günü** dayanır ve yakıt yalnız güç çekilirken yanar.
{recipe spacebreaker:fission_reactor}
Kalkan yuvalarına tükenmiş uranyum levha ya da tungsten levha konabilir.

>! Çalışan çekirdek radyasyon yayar: bir blok uzakta günde yaklaşık **50 Sv**, uzaklığın karesiyle azalır. Aradaki her katı blok yalnız **%30**'unu geçirir; üç blok regolit bunu yaklaşık **%3**'e indirir. Üniteyi göm ya da çevresine regolitten bir set yap, üssünü de onun doğrudan görüş hattından uzak tut.

## RTG peleti
{item spacebreaker:rtg_fuel_pellet} artık **sinterleme fırınında 4 sarı pastadan** yapılır. → [Enerji](power)
{recipe spacebreaker:sintering/rtg_fuel_pellet}

>! Peletin kendisi ise ışınım yayar: üstünde taşırken doz alırsın. RTG'ye tak ve uzak dur.

> **Taze uranyum yakıtı tehlikeli değildir:** uranyumun etkinliği düşüktür, eline alıp taşıyabilirsin. Asıl radyasyon, yakıt elemanı **nükleer termal roket motorunda yanarken** çıkar. → [Roket Parçaları](rocket_parts#ntr)
