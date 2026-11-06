"""Field guide pages for the inner planets (Mars, Phobos and Deimos, Venus, Mercury), registered into gen_guide.py.
Same element tuples as there: ('text', en, tr), ('heading', en, tr), ('items', [ids], en, tr)."""

CHAPTER = ('inner_planets', 'spacebreaker:martian_regolith', 65)


def register(entry, sb):
    req = sb + 'interplanetary_transfers'
    ch = CHAPTER[0]

    # --------------------------------------------------------------------------------------- Mars
    entry('mars', ch, 0, sb + 'martian_regolith', 'Mars', 'Mars',
          ('items', [sb + 'martian_regolith', sb + 'martian_sand', sb + 'martian_basalt', sb + 'martian_sandstone'], '', ''),
          ('text', 'A cold desert under a butterscotch sky. The air is thin carbon dioxide at under one hundredth of Earth\'s '
                   'pressure, gravity is 0.38 g and nights fall far below freezing. You need a sealed suit here, not just oxygen.',
           'Karamela renkli bir gökyüzü altında soğuk bir çöl. Hava, Dünya basıncının yüzde birinden azında ince karbondioksittir; '
           'yerçekimi 0,38 g\'dir ve geceler donma noktasının çok altına iner. Burada sadece oksijen değil, sızdırmaz bir giysi gerekir.'),
          ('text', 'The north is a low, smooth plain; the south is old cratered highland. Look for the great volcanoes, the long rift '
                   'canyons, dune seas and the polar ice caps.',
           'Kuzey alçak ve düz bir ovadır; güney eski, kraterli yaylalardır. Dev volkanları, uzun yarık kanyonlarını, kum denizlerini '
           've kutup buz başlıklarını ara.'),
          ('heading', 'Dust storms', 'Toz fırtınaları'),
          ('text', 'Storms can wrap the whole planet for days. They dim the sun, so solar panels give much less power, and they hide '
                   'everything beyond a few blocks. Dust-proof seals keep your machines and suit running.',
           'Fırtınalar günlerce bütün gezegeni sarabilir. Güneşi karartır, bu yüzden güneş panelleri çok daha az güç verir ve birkaç '
           'bloktan ötesini gizler. Toz geçirmez contalar makinelerini ve giysini çalışır tutar.'),
          ('heading', 'Blue sunsets', 'Mavi gün batımları'),
          ('text', 'The fine dust scatters red light to the sides but lets blue light pass straight on, so around a low sun the sky '
                   'turns blue: the opposite of Earth.',
           'İnce toz kırmızı ışığı yanlara saçar ama mavi ışığı dümdüz geçirir; bu yüzden alçak güneşin çevresinde gökyüzü maviye '
           'döner: Dünya\'nın tam tersi.'),
          requires=req)
    entry('mars_resources', ch, 1, sb + 'raw_hematite', 'Martian Resources', 'Mars Kaynakları',
          ('items', [sb + 'hematite_ore', sb + 'hematite_soil', sb + 'perchlorate_soil', sb + 'martian_ice', sb + 'dry_ice'], '', ''),
          ('text', 'The red colour is rust: iron oxide. Hematite ore and soil rich in little hematite spheres are your iron. '
                   'Perchlorate soil is full of toxic salts: useful to chemists, bad for growing food.',
           'Kırmızı renk pastır: demir oksit. Hematit cevheri ve küçük hematit küreleriyle dolu toprak senin demirindir. '
           'Perklorat toprağı zehirli tuzlarla doludur: kimyacılar için yararlı, yiyecek yetiştirmek için kötü.'),
          ('text', 'Water hides as ice in the polar caps, under dust in dusty ice and in the ground of the high latitudes. The caps are '
                   'topped with dry ice, frozen carbon dioxide, that turns straight into gas when it warms up.',
           'Su; kutup başlıklarında buz olarak, tozlu buzda tozun altında ve yüksek enlemlerin toprağında saklanır. Başlıkların üstü, '
           'ısınınca doğrudan gaza dönüşen kuru buzla, yani donmuş karbondioksitle kaplıdır.'),
          ('items', [sb + 'martian_mudstone', sb + 'martian_clay', sb + 'gypsum_crystal', sb + 'olivine_sand'], '', ''),
          ('text', 'Mudstone, clay and gypsum only form in water. Find them in old river valleys and deltas: Mars was once wet.',
           'Çamurtaşı, kil ve alçıtaşı sadece suda oluşur. Onları eski nehir vadilerinde ve deltalarda bul: Mars bir zamanlar ıslaktı.'),
          requires=req)
    entry('mars_life', ch, 2, sb + 'rust_beetle_spawn_egg', 'Life on Mars', 'Mars\'ta Yaşam',
          ('items', [sb + 'rust_beetle_spawn_egg', sb + 'stonegrazer_spawn_egg', sb + 'canyon_lurker_spawn_egg',
                     sb + 'polar_worm_spawn_egg', sb + 'dust_devil_spawn_egg'], '', ''),
          ('text', 'Rust beetles chew hematite spheres out of the soil and shed iron flakes. Stonegrazers move in small herds and lick '
                   'salts from the rocks; feed them gypsum crystals to breed them.',
           'Pas Böcekleri topraktan hematit kürelerini kemirir ve demir pulları döker. Taşyiyenler küçük sürüler halinde dolaşır '
           've kayalardan tuz yalar; onları çoğaltmak için Alçıtaşı Kristali ver.'),
          ('text', 'Careful in the canyons: a canyon lurker folded up looks like one more rock until you come close. Polar worms bite '
                   'with a numbing chill if provoked, and in strong storms living dust devils chase anything that moves.',
           'Kanyonlarda dikkatli ol: katlanmış bir Kanyon Pusucusu, yaklaşana kadar sıradan bir kaya gibi görünür. Kutup Kurtçukları '
           'kızdırılırsa uyuşturan bir soğukla ısırır; güçlü fırtınalarda da canlı Toz Şeytanları hareket eden her şeyi kovalar.'),
          requires=req)
    entry('mars_moons', ch, 3, sb + 'phobos_regolith', 'Phobos and Deimos', 'Phobos ve Deimos',
          ('items', [sb + 'phobos_regolith', sb + 'deimos_regolith', sb + 'carbonaceous_chondrite'], '', ''),
          ('text', 'Two tiny, dark moons, probably captured asteroids. Their gravity is so weak that a jump carries you a long way. '
                   'Mars fills the sky: about 40 degrees across from Phobos, 16 from Deimos, always in the same place.',
           'İki minik, karanlık uydu; büyük olasılıkla yakalanmış asteroitler. Yerçekimleri o kadar zayıftır ki bir zıplama seni '
           'çok uzağa taşır. Mars gökyüzünü doldurur: Phobos\'tan yaklaşık 40, Deimos\'tan 16 derece genişliğinde, hep aynı yerde.'),
          ('text', 'Their soil is rich in carbon, like the oldest meteorites. Old probes crashed on Phobos, and Deimos hides an ice cache.',
           'Toprakları en eski göktaşları gibi karbonca zengindir. Phobos\'a eski sondalar düşmüştür; Deimos ise bir buz deposu saklar.'),
          requires=req)

    # --------------------------------------------------------------------------------------- Venus
    entry('venus', ch, 10, sb + 'venus_basalt', 'Venus', 'Venüs',
          ('items', [sb + 'venus_basalt', sb + 'venus_soil', sb + 'tessera_rock', sb + 'sulfur_crust', sb + 'metal_frost'], '', ''),
          ('text', 'The surface is a furnace: 464 degrees and 92 times Earth\'s air pressure, under a dim orange sky. Nothing you can '
                   'build yet survives there for long.',
           'Yüzey bir fırındır: 464 derece ve Dünya\'nın 92 katı hava basıncı, loş turuncu bir gökyüzünün altında. Henüz '
           'yapabildiğin hiçbir şey orada uzun süre dayanamaz.'),
          ('text', 'Look at the land from above: folded tesserae, broad shield volcanoes and highlands frosted with metal that '
                   'condenses out of the hot air like snow.',
           'Araziye yukarıdan bak: kıvrımlı tesseralar, geniş kalkan volkanları ve sıcak havadan kar gibi yoğunlaşan metalle '
           'kırağılanmış yaylalar.'),
          ('heading', 'Into the clouds', 'Bulutlara doğru'),
          ('text', 'Climb into the cloud deck and the air cools and thins until, around the cloud tops, it is close to one bar and room '
                   'temperature: the friendliest place in the solar system after Earth. But the clouds are sulfuric acid.',
           'Bulut katmanına tırman; hava, bulut tepelerinin yakınında neredeyse bir bar ve oda sıcaklığına inene kadar soğur ve '
           'incelir: Dünya\'dan sonra güneş sisteminin en dost yeri. Ama bulutlar sülfürik asittir.'),
          ('text', 'Venus turns backwards and very slowly: the sun rises in the west, and a day lasts months.',
           'Venüs ters yönde ve çok yavaş döner: güneş batıdan doğar ve bir gün aylarca sürer.'),
          requires=req)
    entry('venus_clouds', ch, 11, sb + 'cloud_jelly_spawn_egg', 'Life and Ruins in the Clouds', 'Bulutlarda Yaşam ve Harabeler',
          ('items', [sb + 'cloud_jelly_spawn_egg', sb + 'acid_ray_spawn_egg', sb + 'aerogel', sb + 'envelope_fabric'], '', ''),
          ('text', 'Cloud jellies drift on sacs of light gas and sieve sulfur from the air. Acid rays glide in wide curves; leave them '
                   'alone, or they spray acid that eats into armour.',
           'Bulut Medüzleri hafif gaz keseleriyle süzülür ve havadan kükürt süzer. Asit Vatozları geniş kavislerle süzülür; onlara '
           'dokunma, yoksa zırhı aşındıran asit püskürtürler.'),
          ('text', 'A derelict aerostat floats above the cloud tops. Its lab and stores still hold tech disks, guarded by acid rays.',
           'Bulut tepelerinin üstünde terk edilmiş bir aerostat süzülür. Laboratuvarı ve deposu hâlâ teknoloji diskleri saklar; '
           'Asit Vatozları onları korur.'),
          ('items', [sb + 'acid_coating'], '', ''),
          ('text', 'Put any armour piece and a fluoropolymer coating together in a crafting grid to make it acid-proof.',
           'Herhangi bir zırh parçasını ve bir Floropolimer Kaplamayı üretim ızgarasında birleştirerek onu aside dayanıklı yap.'),
          requires=req)

    # --------------------------------------------------------------------------------------- Mercury
    entry('mercury', ch, 20, sb + 'mercurian_crust', 'Mercury', 'Merkür',
          ('items', [sb + 'mercury_regolith', sb + 'mercurian_crust', sb + 'low_reflectance_rock', sb + 'hollow_deposit'], '', ''),
          ('text', 'No air and a sun almost three times as wide as at home. In daylight the ground reaches over 400 degrees; at night '
                   'it falls to minus 180. Travel near the terminator, the line between day and night.',
           'Hava yok ve güneş evdekinden neredeyse üç kat geniş. Gündüz zemin 400 dereceyi aşar; gece eksi 180\'e düşer. Gündüz ile '
           'geceyi ayıran çizgi olan terminatörün yakınında yolculuk et.'),
          ('text', 'Long cliffs called lobate scarps formed as the planet cooled and shrank. Bright hollows are young pits where '
                   'volatile minerals boil away. Dark rock is rich in graphite.',
           'Lobat sarplık denen uzun yarlar, gezegen soğuyup büzülürken oluştu. Parlak oyuklar, uçucu minerallerin buharlaşıp '
           'gittiği genç çukurlardır. Koyu kayalar grafitçe zengindir.'),
          ('items', [sb + 'graphite_ore', sb + 'graphite', sb + 'terminator_crawler_spawn_egg'], '', ''),
          ('text', 'Craters near the poles never see the sun and keep water ice. An old ice mine lies in one of them. Terminator '
                   'crawlers roam while the sun is low and dig in when it climbs.',
           'Kutuplara yakın kraterler güneşi hiç görmez ve su buzu saklar. Bunlardan birinde eski bir buz madeni yatar. Alacakaranlık '
           'Sürüngenleri güneş alçakken dolaşır, yükselince toprağa gömülür.'),
          requires=req)

    # --------------------------------------------------------------------------------------- orbit
    entry('inner_orbits', ch, 30, 'minecraft:ender_eye', 'Inner Planets from Orbit', 'Yörüngeden İç Gezegenler',
          ('text', 'From orbit Venus is a smooth, pale ball wrapped in haze, with faint sideways Y-shaped bands in its clouds. Mercury '
                   'looks like a grey Moon. On Mars you can pick out the volcanoes, the rift canyon and the polar caps.',
           'Yörüngeden Venüs, pusa sarılmış düzgün, soluk bir küredir; bulutlarında silik, yan yatmış Y biçimli bantlar vardır. Merkür '
           'gri bir Ay gibi görünür. Mars\'ta volkanları, yarık kanyonunu ve kutup başlıklarını seçebilirsin.'),
          requires=req)
