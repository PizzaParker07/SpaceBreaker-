"""Writes the field guide (assets/spacebreaker/guide/<lang>/*.json and chapters.json) from one bilingual source,
so English and Turkish always have the same entries and pages.

    python3 tools/guide/gen_guide.py

Each page element: ('text', en, tr) · ('heading', en, tr) · ('items', [ids], en, tr) · ('image', path, w, h, en, tr)
· ('recipe', id) · ('break',). Other areas can add their own JSON files under their namespace (see GuideData.java)."""
import json
import os

ROOT = os.path.abspath(os.path.join(os.path.dirname(__file__), '..', '..'))
OUT = os.path.join(ROOT, 'src', 'main', 'resources', 'assets', 'spacebreaker', 'guide')
SB = 'spacebreaker:'

CHAPTERS = [
    ('getting_started', SB + 'guide_book', 0),
    ('materials', SB + 'raw_bauxite', 10),
    ('machines', SB + 'solar_panel', 20),
    ('research', SB + 'research_lab', 30),
    ('dungeons', SB + 'tech_disk', 40),
    ('survival', SB + 'eva_helmet', 50),
    ('rocketry', SB + 'rocket', 55),
    ('moon', SB + 'lunar_regolith', 60),
    ('physics', 'minecraft:firework_rocket', 70),
]

ENTRIES = []


def entry(name, chapter, order, icon, en_title, tr_title, *pages, requires=None):
    ENTRIES.append(dict(name=name, chapter=chapter, order=order, icon=icon, en=en_title, tr=tr_title, pages=pages, requires=requires))


# ------------------------------------------------------------------------------------------- getting started
entry('welcome', 'getting_started', 0, SB + 'guide_book', 'Welcome, Engineer', 'Hoş Geldin, Mühendis',
      ('text', 'Spacebreaker is about building your way to space with real engineering. Rockets obey the rocket equation, '
               'worlds keep their real gravity, air and temperature, and every new machine starts as a technology you have to learn.',
       'Spacebreaker, gerçek mühendislikle uzaya giden yolu inşa etmekle ilgilidir. Roketler roket denklemine uyar, '
       'dünyalar gerçek yerçekimini, havasını ve sıcaklığını korur; her yeni makine önce öğrenmen gereken bir teknoloji olarak başlar.'),
      ('image', SB + 'textures/gui/guide/orbit.png', 112, 64, 'Orbit: falling around the world', 'Yörünge: dünyanın etrafına düşmek'),
      ('text', 'This guide grows with you. Pages about things you have not discovered yet stay locked until you learn the technology behind them.',
       'Bu rehber seninle birlikte büyür. Henüz keşfetmediğin şeylerle ilgili sayfalar, arkalarındaki teknolojiyi öğrenene kadar kilitli kalır.'))
entry('progression', 'getting_started', 1, 'minecraft:compass', 'The Road to the Moon', 'Ay\'a Giden Yol',
      ('heading', '1. Materials', '1. Malzemeler'),
      ('text', 'Find bauxite in the hills and smelt it into aluminium: the light metal every tank, panel and rocket is made of.',
       'Tepelerde boksit bul ve alüminyuma ergit: her tankın, panelin ve roketin yapıldığı hafif metal.'),
      ('heading', '2. Research', '2. Araştırma'),
      ('text', 'Take field samples with a Sample Kit and analyse them in a Research Lab. Each world reveals different technologies.',
       'Numune Çantası ile saha numuneleri al ve Araştırma Laboratuvarı\'nda incele. Her dünya farklı teknolojiler ortaya çıkarır.'),
      ('heading', '3. Ruins', '3. Harabeler'),
      ('text', 'Old research sites hide tech disks. The abandoned observatories on Earth still hold the first rocketry data.',
       'Eski araştırma alanları teknoloji diskleri saklar. Dünya\'daki terk edilmiş gözlemevlerinde hâlâ ilk roketçilik verileri var.'),
      ('heading', '4. Lift-off', '4. Kalkış'),
      ('text', 'Design a rocket, reach orbit, land on the Moon and come home safely. Then keep going.',
       'Bir roket tasarla, yörüngeye çık, Ay\'a in ve sağ salim eve dön. Sonra devam et.'))

# ------------------------------------------------------------------------------------------- materials
entry('bauxite', 'materials', 0, SB + 'raw_bauxite', 'Bauxite', 'Boksit',
      ('items', [SB + 'bauxite_ore', SB + 'deepslate_bauxite_ore', SB + 'raw_bauxite', SB + 'raw_bauxite_block'], '', ''),
      ('text', 'Bauxite is a red-brown rock made of small round pisoids. It forms where rain has washed everything but aluminium '
               'and iron out of the soil, so look for it in the stone of warm hills. Mine it with a stone pickaxe or better.',
       'Boksit küçük yuvarlak pizoidlerden oluşan kızıl-kahve bir kayaçtır. Yağmurun topraktan alüminyum ve demir dışındaki '
       'her şeyi yıkadığı yerlerde oluşur; sıcak tepelerin taşında ara. Taş ya da daha iyi bir kazmayla kaz.'))
entry('aluminium', 'materials', 1, SB + 'aluminum_ingot', 'Aluminium', 'Alüminyum',
      ('items', [SB + 'aluminum_ingot', SB + 'aluminum_plate'], '', ''),
      ('text', 'Aluminium is a third as dense as steel and does not rust. Smelt raw bauxite into ingots; plates are the stiff, '
               'thin sheets used for tanks, hulls and machine casings.',
       'Alüminyum çeliğin üçte biri yoğunluğundadır ve paslanmaz. Ham boksiti külçeye ergit; levhalar tanklar, gövdeler ve '
       'makine kasaları için kullanılan ince, sağlam saclardır.'))

# ------------------------------------------------------------------------------------------- machines
entry('solar_tracker', 'machines', 0, SB + 'solar_panel', 'Solar Tracker', 'Güneş Takipli Panel',
      ('items', [SB + 'solar_panel'], '', ''),
      ('text', 'A single-axis tracker turns its panel east to west to face the sun and lies flat at night. It gives up to 20 FE/t '
               'at noon under a clear sky, less in rain, at dawn and at dusk. Energy flows out of every side except the top.',
       'Tek eksenli takip sistemi paneli doğudan batıya çevirerek güneşe bakar, gece düz yatar. Açık havada öğle vakti 20 FE/t\'ye '
       'kadar üretir; yağmurda, şafakta ve alacakaranlıkta daha az. Enerji üst yüz hariç her yönden çıkar.'))

# ------------------------------------------------------------------------------------------- research
entry('sample_kit', 'research', 0, SB + 'sample_kit', 'Sample Kit', 'Numune Çantası',
      ('recipe', SB + 'sample_kit'),
      ('text', 'Use the kit on natural ground (stone, soil, sand, gravel, ore) to bag a field sample of the world you stand on. '
               'Samples from Earth reveal the basics; samples from the Moon and other worlds reveal what only they can teach.',
       'Çantayı doğal zemine (taş, toprak, kum, çakıl, cevher) kullanarak bulunduğun dünyanın saha numunesini al. '
       'Dünya numuneleri temel bilgileri ortaya çıkarır; Ay ve diğer dünyaların numuneleri yalnızca onların öğretebileceklerini.'),
      ('items', [SB + 'field_sample', SB + 'experiment_data'], 'Field samples and experiment records', 'Saha numuneleri ve deney kayıtları'))
entry('research_lab', 'research', 1, SB + 'research_lab', 'Research Lab', 'Araştırma Laboratuvarı',
      ('recipe', SB + 'research_lab'),
      ('image', SB + 'textures/gui/guide/lab_flow.png', 112, 48, 'Disk or sample in, technology out', 'Disk ya da numune girer, teknoloji çıkar'),
      ('text', 'Put a tech disk, field sample or experiment record in the left slot and give the lab power (FE): a solar tracker '
               'next to it is enough for the first technologies. The analysis takes from 30 seconds to a few minutes.',
       'Sol yuvaya bir teknoloji diski, saha numunesi ya da deney kaydı koy ve laboratuvara enerji (FE) ver: ilk teknolojiler '
       'için yanına konan bir güneş paneli yeter. İnceleme 30 saniyeden birkaç dakikaya kadar sürer.'),
      ('text', 'The researcher is whoever opened the lab last. A disk is returned after reading, so you can lend it to friends; '
               'samples are used up. If a sample has nothing new for you, the lab says so and keeps it.',
       'Araştırmacı, laboratuvarı en son açan kişidir. Disk okunduktan sonra geri verilir, böylece arkadaşlarına ödünç verebilirsin; '
       'numuneler tükenir. Bir numunede senin için yeni bir şey yoksa laboratuvar bunu söyler ve numuneyi saklar.'))
entry('tech_disks', 'research', 2, SB + 'tech_disk', 'Tech Disks', 'Teknoloji Diskleri',
      ('items', [SB + 'tech_disk'], '', ''),
      ('text', 'Data cartridges from the early space age, found in ruins on every world. The colour of the label shows the branch: '
               'orange propulsion, cyan life support, brass industry, violet astronomy, grey construction, green exploration.',
       'Erken uzay çağından kalma veri kartuşları; her dünyadaki harabelerde bulunur. Etiket rengi dalı gösterir: '
       'turuncu itki, camgöbeği yaşam desteği, pirinç sanayi, mor astronomi, gri inşaat, yeşil keşif.'),
      ('text', 'A lab can only read a disk once you know its prerequisites, so keep the advanced ones for later.',
       'Laboratuvar bir diski ancak ön koşullarını biliyorsan okuyabilir; ileri seviye diskleri sonraya sakla.'))
entry('research_tree', 'research', 3, 'minecraft:writable_book', 'The Research Tree', 'Araştırma Ağacı',
      ('text', 'Press J (or the tree button in a lab) to open the research tree. Each row is a branch; lines show prerequisites. '
               'Gold frames are known, white ones are ready to research and dark ones are still locked. Hover a technology to see '
               'what it needs, how to learn it and what a lab needs to analyse it.',
       'Araştırma ağacını açmak için J\'ye (ya da laboratuvardaki ağaç düğmesine) bas. Her satır bir daldır; çizgiler ön koşulları '
       'gösterir. Altın çerçeveler bilinenler, beyazlar araştırmaya hazır olanlar, koyular hâlâ kilitli olanlardır. Bir teknolojinin '
       'üzerine gelince neye ihtiyaç duyduğunu, nasıl öğrenileceğini ve laboratuvarın neye ihtiyacı olduğunu görürsün.'))
entry('team_research', 'research', 4, 'minecraft:white_banner', 'Researching as a Team', 'Takımca Araştırma',
      ('text', 'Turn on team sharing in a lab and everyone in your scoreboard team learns what it finds, even players who are offline '
               '(they learn it when they join). Create a team with /team add and join it with /team join.',
       'Bir laboratuvarda takımla paylaşımı açarsan, skor tablosu takımındaki herkes onun bulduklarını öğrenir; çevrimdışı oyuncular '
       'bile (oyuna girdiklerinde öğrenirler). /team add ile bir takım kur, /team join ile katıl.'))

# ------------------------------------------------------------------------------------------- dungeons
entry('observatory', 'dungeons', 0, 'minecraft:spyglass', 'Abandoned Observatories', 'Terk Edilmiş Gözlemevleri',
      ('image', SB + 'textures/gui/guide/observatory.png', 112, 64, '', ''),
      ('text', 'Copper-domed observatories stand on open plains and hills, each with an old rocket test stand next to it. '
               'Look for the broken service gantry and the scorched flame trench.',
       'Bakır kubbeli gözlemevleri açık ovalarda ve tepelerde durur; her birinin yanında eski bir roket test rampası vardır. '
       'Yıkık servis kulesini ve kararmış alev kanalını ara.'),
      ('text', 'The archive under the floor keeps the first rocketry technologies. The site is not empty: guardians wait there the '
               'first time anyone walks in.',
       'Zeminin altındaki arşiv ilk roketçilik teknolojilerini saklar. Alan boş değildir: biri ilk kez girdiğinde muhafızlar orada bekler.'))

# ------------------------------------------------------------------------------------------- survival
entry('eva_helmet', 'survival', 0, SB + 'eva_helmet', 'EVA Helmet', 'EVA Kaskı',
      ('items', [SB + 'eva_helmet'], '', ''),
      ('text', 'A pressure helmet with a clear visor and two lamps. Sneak and use it to lower the gold sun visor: '
               'outside the atmosphere the Sun is blinding.',
       'Şeffaf vizörlü ve iki lambalı basınçlı kask. Altın güneş vizörünü indirmek için eğilip kullan: '
       'atmosferin dışında Güneş göz kamaştırır.'))
entry('oxygen', 'survival', 1, SB + 'oxygen_tank', 'Oxygen', 'Oksijen',
      ('items', [SB + 'oxygen_tank'], '', ''),
      ('text', 'One tank holds five minutes of breathing. There is no air on the Moon or in orbit: always carry spares.',
       'Bir tüp beş dakikalık nefes alır. Ay\'da ve yörüngede hava yoktur: her zaman yedek taşı.'))

# ------------------------------------------------------------------------------------------- moon
entry('moon_rocks', 'moon', 0, SB + 'lunar_regolith', 'Lunar Rocks', 'Ay Kayaçları',
      ('items', [SB + 'lunar_regolith', SB + 'mare_basalt', SB + 'ilmenite_ore'], '', ''),
      ('text', 'Regolith is fine grey dust that keeps every boot print. The dark maria are basalt; their black ilmenite crystals '
               'hold titanium and oxygen that can be extracted on the spot.',
       'Regolit, her ayak izini saklayan ince gri tozdur. Karanlık denizler bazalttır; siyah ilmenit kristalleri yerinde '
       'çıkarılabilen titanyum ve oksijen içerir.'),
      requires=SB + 'basic_rocketry')

# ------------------------------------------------------------------------------------------- physics
entry('rocket_equation', 'physics', 0, 'minecraft:firework_rocket', 'The Rocket Equation', 'Roket Denklemi',
      ('text', 'Δv = Isp · g₀ · ln(m₀ / m₁)', 'Δv = Isp · g₀ · ln(m₀ / m₁)'),
      ('text', 'How much a rocket can change its speed (Δv) depends on how fast its exhaust leaves (specific impulse, Isp) and on '
               'the ratio of full to empty mass. Because of the logarithm, adding fuel helps less and less: that is why rockets drop '
               'empty stages.',
       'Bir roketin hızını ne kadar değiştirebileceği (Δv), egzozun ne kadar hızlı çıktığına (özgül itki, Isp) ve dolu kütlenin '
       'boş kütleye oranına bağlıdır. Logaritma yüzünden yakıt eklemek gittikçe daha az işe yarar: roketlerin boş kademeleri '
       'bırakmasının nedeni budur.'),
      ('text', 'To lift off at all, thrust must beat weight: a thrust-to-weight ratio above 1, ideally about 1.3 to 1.5.',
       'Kalkabilmek için itki ağırlığı yenmelidir: itki/ağırlık oranı 1\'in üstünde, ideal olarak 1,3 ile 1,5 arası.'),
      requires=SB + 'basic_rocketry')
entry('orbits', 'physics', 1, 'minecraft:clock', 'Orbits', 'Yörüngeler',
      ('image', SB + 'textures/gui/guide/orbit.png', 112, 64, 'Velocity sideways, gravity down', 'Hız yana, yerçekimi aşağı'),
      ('text', 'In orbit you are still falling, but moving sideways so fast that the ground curves away beneath you. Low Earth '
               'orbit needs about 7.8 km/s; with drag and gravity losses a launch costs about 9.4 km/s of Δv.',
       'Yörüngede hâlâ düşüyorsundur, ama yana öyle hızlı gidiyorsun ki zemin altından kıvrılıp uzaklaşır. Alçak Dünya yörüngesi '
       'yaklaşık 7,8 km/s ister; sürtünme ve yerçekimi kayıplarıyla bir fırlatma yaklaşık 9,4 km/s Δv tutar.'),
      requires=SB + 'orbital_mechanics')

# ------------------------------------------------------------------------------------------- more materials
entry('earth_ores', 'materials', 2, SB + 'raw_rutile', 'Ores of the Space Age', 'Uzay Çağının Cevherleri',
      ('items', [SB + 'rutile_ore', SB + 'pentlandite_ore', SB + 'spodumene_ore', SB + 'wolframite_ore'], '', ''),
      ('text', 'Rutile gives titanium and lies deep and rare. Pentlandite gives nickel at middle depths. Spodumene gives lithium '
               'high in the mountains. Wolframite gives tungsten, deep down. Each has a deepslate version and a raw block.',
       'Rutil titanyum verir; derinde ve nadirdir. Pentlandit orta derinlikte nikel verir. Spodümen dağların yükseklerinde '
       'lityum verir. Wolframit derinde tungsten verir. Her birinin derin kayrak çeşidi ve ham bloğu vardır.'),
      ('items', [SB + 'titanium_ingot', SB + 'nickel_ingot', SB + 'lithium_ingot', SB + 'tungsten_ingot'], '', ''))
entry('alloys', 'materials', 3, SB + 'steel_ingot', 'Alloys', 'Alaşımlar',
      ('items', [SB + 'steel_ingot', SB + 'aluminum_lithium_ingot', SB + 'nickel_superalloy_ingot'], '', ''),
      ('text', 'The electric arc furnace melts metals together. Steel is iron with a little carbon: strong and cheap for towers '
               'and pads. Aluminium-lithium is the lightest metal for tanks. Nickel superalloy survives the heat inside engines.',
       'Elektrik ark fırını metalleri birlikte eritir. Çelik biraz karbonlu demirdir: kuleler ve rampalar için güçlü ve ucuz. '
       'Alüminyum-lityum tanklar için en hafif metaldir. Nikel süper alaşımı motorların içindeki ısıya dayanır.'))
entry('components', 'materials', 4, SB + 'basic_circuit', 'Components', 'Bileşenler',
      ('items', [SB + 'basic_circuit', SB + 'advanced_circuit', SB + 'electric_motor', SB + 'silicon_wafer', SB + 'solar_cell',
                 SB + 'plastic_sheet', SB + 'gold_foil_insulation', SB + 'orange_foam_insulation', SB + 'ablative_shield_plate',
                 SB + 'silicone_seal'], '', ''),
      ('text', 'Machines and rocket parts are built from components. Most of them come from the fabricator; silicon comes from '
               'quartz or sand in the arc furnace and is pressed into wafers.',
       'Makineler ve roket parçaları bileşenlerden yapılır. Çoğu Parça Üretim Tezgâhı\'ndan çıkar; silisyum ark fırınında '
       'kuvarstan ya da kumdan elde edilir ve preste plakaya dönüştürülür.'),
      ('recipe', SB + 'basic_circuit'))
entry('crude_oil', 'materials', 5, SB + 'crude_oil_bucket', 'Crude Oil', 'Ham Petrol',
      ('items', [SB + 'oil_seep', SB + 'crude_oil_bucket', SB + 'rp1_bucket', SB + 'plastic_sheet'], '', ''),
      ('text', 'Tar-stained ground (an oil seep) means a pocket of crude oil lies right below. Pump it up with a pumpjack and '
               'distil it in a refinery into RP-1 rocket kerosene and plastics.',
       'Katran lekeli zemin (petrol sızıntısı), hemen altında bir ham petrol cebi olduğunu gösterir. Onu petrol pompasıyla çıkar '
       've rafineride RP-1 roket kerosenine ve plastiğe damıt.'))

# ------------------------------------------------------------------------------------------- machines
entry('power', 'machines', 1, SB + 'combustion_generator', 'Power Grid', 'Elektrik Şebekesi',
      ('items', [SB + 'combustion_generator', SB + 'battery_bank', SB + 'copper_cable', SB + 'hv_cable'], '', ''),
      ('text', 'The combustion generator burns fuel, but only where there is breathable air. The battery bank stores energy and '
               'gives it out of its front. Copper cable carries a little power; high-voltage cable carries a lot.',
       'Yanmalı jeneratör yakıt yakar, ama yalnızca solunabilir havanın olduğu yerde. Batarya bankası enerjiyi depolar ve önünden '
       'verir. Bakır kablo biraz güç taşır; yüksek gerilim kablosu çok taşır.'),
      ('recipe', SB + 'combustion_generator'))
entry('processing', 'machines', 2, SB + 'press', 'Processing', 'İşleme',
      ('items', [SB + 'crusher', SB + 'arc_furnace', SB + 'press', SB + 'plate_press_die', SB + 'wire_press_die', SB + 'rod_press_die'], '', ''),
      ('text', 'The crusher grinds ores into dust, two for one. The electric arc furnace smelts dust and makes alloys. The press '
               'shapes ingots into plates, wires and rods; the die you put in decides which, and is never used up.',
       'Kırıcı cevherleri toza öğütür, bire iki. Elektrik ark fırını tozu ergitir ve alaşım yapar. Pres külçeleri levha, tel ve '
       'çubuğa dönüştürür; hangisi olacağına taktığın kalıp karar verir ve kalıp hiç tükenmez.'),
      ('recipe', SB + 'press'))
entry('oil_industry', 'machines', 3, SB + 'pumpjack', 'Pumpjack and Refinery', 'Petrol Pompası ve Rafineri',
      ('items', [SB + 'pumpjack', SB + 'refinery', SB + 'fluid_pipe', SB + 'fluid_tank'], '', ''),
      ('text', 'Both are big machines that need free space around them. Put the pumpjack over an oil seep, pipe the crude oil to '
               'the refinery and collect RP-1 in a tank. Sneak-use the end of a pipe to make it pull from a block.',
       'İkisi de çevresinde boş alan isteyen büyük makinelerdir. Petrol pompasını bir sızıntının üstüne kur, ham petrolü boruyla '
       'rafineriye götür ve RP-1\'i bir tankta topla. Bir borunun ucuna eğilip kullanırsan o bloktan çekmeye başlar.'),
      ('recipe', SB + 'pumpjack'))
entry('cryogenics', 'machines', 4, SB + 'cryogenic_liquefier', 'Cryogenics', 'Kriyojenik',
      ('items', [SB + 'electrolyzer', SB + 'cryogenic_liquefier', SB + 'cryogenic_tank', SB + 'pressurized_tank', SB + 'gas_pipe',
                 SB + 'cryogenic_canister'], '', ''),
      ('text', 'The electrolyzer splits water into oxygen and hydrogen gas. The cryogenic liquefier turns the gases into liquid '
               'oxygen (−183 °C) and liquid hydrogen (−253 °C). Keep them in cryogenic tanks or canisters: spilled in the open '
               'they boil away.',
       'Elektroliz ünitesi suyu oksijen ve hidrojen gazına ayırır. Kriyojenik sıvılaştırıcı gazları sıvı oksijene (−183 °C) ve '
       'sıvı hidrojene (−253 °C) dönüştürür. Onları kriyojenik tank ya da kaplarda tut: açığa dökülürse kaynayıp uçarlar.'),
      ('items', [SB + 'liquid_oxygen_bucket', SB + 'liquid_hydrogen_bucket', SB + 'liquid_methane_bucket'], '', ''))
entry('fabricator', 'machines', 5, SB + 'fabricator', 'Fabricator', 'Parça Üretim Tezgâhı',
      ('recipe', SB + 'fabricator'),
      ('text', 'A powered workbench for precision parts: circuits, motors, solar cells, insulation and shield plates. Its recipes '
               'show up in recipe viewers under "Fabricating".',
       'Hassas parçalar için enerjiyle çalışan tezgâh: devreler, motorlar, güneş hücreleri, yalıtım ve kalkan levhaları. '
       'Tarifleri, tarif gösterici modlarda "Parça üretimi" başlığı altında görünür.'))

# ------------------------------------------------------------------------------------------- survival (more)
entry('eva_suit', 'survival', 2, SB + 'eva_chestplate', 'EVA Suit', 'EVA Giysisi',
      ('items', [SB + 'eva_helmet', SB + 'eva_chestplate', SB + 'eva_leggings', SB + 'eva_boots'], '', ''),
      ('text', 'The suit is sealed only when all four pieces are worn. The torso carries the life-support backpack: right-click '
               'it with an oxygen tank to fit up to two tanks. The helmet display shows oxygen, suit status, body and outside '
               'temperature, radiation and gravity.',
       'Giysi ancak dört parçası birlikte giyildiğinde sızdırmaz olur. Gövde yaşam destek sırt çantasını taşır: en çok iki tüp '
       'takmak için ona oksijen tüpüyle sağ tıkla. Kask göstergesi oksijeni, giysi durumunu, vücut ve dış sıcaklığı, radyasyonu '
       've yerçekimini gösterir.'),
      ('recipe', SB + 'eva_chestplate'))
entry('life_support', 'survival', 3, SB + 'oxygen_distributor', 'Air for the Base', 'Üs İçin Hava',
      ('items', [SB + 'oxygen_filler', SB + 'oxygen_distributor', SB + 'airlock_door', SB + 'large_oxygen_tank'], '', ''),
      ('text', 'The oxygen filler refills tanks from breathable air using power. The oxygen distributor fills a sealed room (up to '
               '4096 blocks) from a tank and tells you where it leaks. Airlock doors are airtight while closed and open by hand '
               'or redstone.',
       'Oksijen dolum istasyonu tüpleri enerjiyle solunabilir havadan doldurur. Oksijen dağıtıcı kapalı bir odayı (en çok 4096 '
       'blok) bir tüpten doldurur ve nereden sızdığını söyler. Hava kilidi kapıları kapalıyken hava geçirmez, elle ya da '
       'kızıltaşla açılır.'),
      ('recipe', SB + 'oxygen_distributor'))
entry('eva_mobility', 'survival', 4, SB + 'maneuvering_unit', 'Moving in Low Gravity', 'Düşük Yerçekiminde Hareket',
      ('items', [SB + 'maneuvering_unit', SB + 'magnetic_boots'], '', ''),
      ('text', 'Fit the maneuvering unit onto an EVA torso and fly with jump and sneak on cold gas; refill it at an oxygen filler. '
               'Magnetic boots keep you on the floor in low and zero gravity; sneak to let go.',
       'Manevra ünitesini bir EVA gövdesine tak ve zıplama ile eğilme tuşlarıyla soğuk gazla uç; oksijen dolum istasyonunda '
       'yeniden doldur. Manyetik botlar seni düşük ve sıfır yerçekiminde zemine tutar; bırakmak için eğil.'))
entry('hazards', 'survival', 5, 'minecraft:snowball', 'Hazards', 'Tehlikeler',
      ('text', 'In vacuum you can hold your breath only for a moment. Body temperature changes slowly: the Moon at noon or in '
               'shadow will cook or freeze an unprotected body. Radiation adds up over time. Gravity is real: on the Moon you '
               'jump far and fall gently.',
       'Boşlukta nefesini yalnızca bir an tutabilirsin. Vücut ısısı yavaş değişir: öğle vakti ya da gölgede Ay korumasız bir '
       'bedeni pişirir ya da dondurur. Radyasyon zamanla birikir. Yerçekimi gerçektir: Ay\'da uzağa zıplar, yavaşça düşersin.'))

# ------------------------------------------------------------------------------------------- rocketry
entry('rocketry_start', 'rocketry', 0, SB + 'rocket', 'Before You Fly', 'Uçmadan Önce',
      ('text', 'Rockets are learned, not invented: find an abandoned observatory, take the Basic Rocketry disk from its archive '
               'and read it in a Research Lab. The rest of this chapter opens up when you know it.',
       'Roketler icat edilmez, öğrenilir: terk edilmiş bir gözlemevi bul, arşivinden Temel Roketçilik diskini al ve Araştırma '
       'Laboratuvarı\'nda oku. Bu bölümün geri kalanı onu öğrendiğinde açılır.'))
entry('rocket_parts', 'rocketry', 1, SB + 'kerosene_engine', 'Rocket Parts', 'Roket Parçaları',
      ('items', [SB + 'nose_cone', SB + 'crew_capsule', SB + 'heat_shield', SB + 'parachute', SB + 'fuel_tank_small',
                 SB + 'fuel_tank_medium', SB + 'fuel_tank_large', SB + 'kerosene_engine', SB + 'vacuum_engine', SB + 'decoupler',
                 SB + 'interstage', SB + 'fins', SB + 'grid_fins', SB + 'landing_legs', SB + 'rcs_thrusters'], '', ''),
      ('text', 'Each stage is an engine at the bottom with fuel tanks above it. Decouplers or interstages separate stages. The '
               'capsule carries the crew and sits on a heat shield, with a parachute on top. Fins, landing legs and RCS go on '
               'the side.',
       'Her kademe, altta bir motor ve üstünde yakıt tanklarından oluşur. Kademeleri ayırıcılar ya da ara kademeler ayırır. '
       'Kapsül mürettebatı taşır ve ısı kalkanının üstüne oturur, en üstte paraşüt bulunur. Kanatçıklar, iniş bacakları ve '
       'manevra iticileri yana takılır.'),
      requires=SB + 'basic_rocketry')
entry('rocket_assembler', 'rocketry', 2, SB + 'rocket_assembler', 'Rocket Assembler', 'Roket Montaj Tezgâhı',
      ('recipe', SB + 'rocketry/rocket_assembler'),
      ('text', 'Fill the stages from the bottom up and put the payload on top. The assembler shows the Δv and thrust-to-weight of '
               'every stage before you build. Reaching orbit takes about 9.4 km/s; the Moon about 6 km/s more.',
       'Kademeleri alttan yukarı doldur, yükü en üste koy. Montaj tezgâhı, yapmadan önce her kademenin Δv ve itki/ağırlık '
       'değerini gösterir. Yörüngeye çıkmak yaklaşık 9,4 km/s, Ay\'a gitmek yaklaşık 6 km/s daha ister.'),
      requires=SB + 'basic_rocketry')
entry('launch_pad', 'rocketry', 3, SB + 'launch_pad', 'Launch Pad', 'Fırlatma Rampası',
      ('items', [SB + 'launch_pad', SB + 'flame_trench', SB + 'service_tower', SB + 'crew_access_arm', SB + 'fuel_umbilical',
                 SB + 'launch_control_console'], '', ''),
      ('text', 'Lay a 3×3 of flame trench grates inside a ring of 16 pad plates, build a service tower beside it with the crew '
               'access arm and the fuel umbilical, and place a launch control console nearby. The console checks the pad and '
               'counts down.',
       'On altı rampa plakasından bir halkanın içine 3×3 alev kanalı ızgarası döşe, yanına mürettebat erişim kolu ve yakıt hattı '
       'olan bir servis kulesi dik, yakına bir fırlatma kontrol konsolu koy. Konsol rampayı kontrol eder ve geri sayar.'),
      requires=SB + 'basic_rocketry')
entry('flight', 'rocketry', 4, SB + 'emergency_beacon', 'Flight', 'Uçuş',
      ('text', 'Stand the rocket on the trench, fuel it through the umbilical, climb in and launch from the console. In orbit, '
               'open the star map to choose where to go. Landing on the Moon needs landing legs; coming home needs a heat shield '
               'and a parachute.',
       'Roketi kanalın üstüne dik, yakıt hattından yakıtını doldur, bin ve konsoldan fırlat. Yörüngede nereye gideceğini seçmek '
       'için yıldız haritasını aç. Ay\'a inmek iniş bacakları, eve dönmek ısı kalkanı ve paraşüt ister.'),
      ('items', [SB + 'emergency_beacon'], 'Stranded? The emergency beacon calls a capsule home.',
       'Mahsur mu kaldın? Acil dönüş işareti seni eve götürecek bir kapsül çağırır.'),
      requires=SB + 'basic_rocketry')

# ------------------------------------------------------------------------------------------- moon (more)
entry('moon_building', 'moon', 1, SB + 'sintered_regolith_bricks', 'Building on the Moon', 'Ay\'da İnşaat',
      ('items', [SB + 'anorthosite', SB + 'polished_anorthosite', SB + 'anorthosite_bricks', SB + 'sintered_regolith',
                 SB + 'sintered_regolith_bricks', SB + 'impact_breccia'], '', ''),
      ('text', 'The bright highlands are anorthosite; cut it in a stonecutter. Bake regolith in a furnace into sintered regolith, '
               'the first building block of a Moon base.',
       'Parlak yaylalar anortozittir; onu taş kesicide kes. Regoliti fırında pişirip sinterlenmiş regolit yap: bir Ay üssünün '
       'ilk yapı malzemesi.'),
      requires=SB + 'basic_rocketry')
entry('moon_resources', 'moon', 2, SB + 'lunar_ice', 'Lunar Resources', 'Ay Kaynakları',
      ('items', [SB + 'icy_regolith', SB + 'lunar_ice', SB + 'kreep_ore', SB + 'kreep_fragment', SB + 'meteoric_iron', SB + 'raw_ilmenite'], '', ''),
      ('text', 'Ice hides in the permanently dark floors of polar craters. KREEP holds rare earths and thorium. Meteoric iron '
               'from old impacts breaks into raw iron.',
       'Buz, kutup kraterlerinin hiç güneş görmeyen tabanlarında saklanır. KREEP nadir toprak elementleri ve toryum içerir. Eski '
       'çarpmalardan kalan meteor demiri ham demire ayrılır.'),
      requires=SB + 'basic_rocketry')
entry('moon_life', 'moon', 3, SB + 'regolith_tick_spawn_egg', 'Life on the Moon', 'Ay\'da Yaşam',
      ('items', [SB + 'regolith_tick_spawn_egg', SB + 'elder_regolith_tick_spawn_egg', SB + 'dust_swimmer_spawn_egg',
                 SB + 'survey_drone_spawn_egg'], '', ''),
      ('text', 'Regolith ticks hide under the dust and leap far in the low gravity; an elder tick guards its nest in the lava '
               'tubes. Dust swimmers glide harmlessly just under the surface. Old survey drones still guard their outposts.',
       'Regolit keneleri tozun altına saklanır ve düşük yerçekiminde uzağa sıçrar; yaşlı bir kene lav tüplerindeki yuvasını '
       'korur. Toz yüzücüleri yüzeyin hemen altında zararsızca süzülür. Eski keşif dronları hâlâ karakollarını korur.'),
      requires=SB + 'basic_rocketry')
entry('moon_dungeons', 'dungeons', 1, SB + 'survey_drone_spawn_egg', 'Ruins on the Moon', 'Ay\'daki Harabeler',
      ('text', 'Abandoned survey outposts stand on the plains: a central module, a lab, storage, a drone hangar and an airlock. '
               'Ancient lava tubes open through collapsed roofs in the maria, with the wreck of an old probe deep inside.',
       'Terk edilmiş keşif karakolları ovalarda durur: merkez modül, laboratuvar, depo, dron hangarı ve hava kilidi. Antik lav '
       'tüpleri, denizlerde çökmüş tavanlardan açılır; derinlerinde eski bir sondanın enkazı vardır.'),
      ('text', 'Their labs and probes keep tech disks for the next steps: life support, construction and the Moon itself.',
       'Laboratuvarları ve sondaları sonraki adımlar için teknoloji diskleri saklar: yaşam desteği, inşaat ve Ay\'ın kendisi.'),
      requires=SB + 'basic_rocketry')


def page_json(p, lang):
    k = 0 if lang == 'en_us' else 1
    kind = p[0]
    if kind == 'text':
        return {'text': p[1 + k]}
    if kind == 'heading':
        return {'heading': p[1 + k]}
    if kind == 'items':
        d = {'items': p[1]}
        if p[2 + k]:
            d['text'] = p[2 + k]
        return d
    if kind == 'image':
        d = {'image': p[1], 'width': p[2], 'height': p[3]}
        if p[4 + k]:
            d['text'] = p[4 + k]
        return d
    if kind == 'recipe':
        return {'recipe': p[1]}
    if kind == 'break':
        return {'page_break': True}
    raise ValueError(kind)


def main():
    os.makedirs(OUT, exist_ok=True)
    with open(os.path.join(OUT, 'chapters.json'), 'w', encoding='utf-8') as f:
        json.dump([{'id': c, 'icon': i, 'order': o} for c, i, o in CHAPTERS], f, indent=2, ensure_ascii=False)
        f.write('\n')
    for lang in ('en_us', 'tr_tr'):
        d = os.path.join(OUT, lang)
        os.makedirs(d, exist_ok=True)
        for e in ENTRIES:
            obj = {'chapter': e['chapter'], 'title': e['en'] if lang == 'en_us' else e['tr'], 'icon': e['icon'], 'order': e['order']}
            if e['requires']:
                obj['requires_tech'] = e['requires']
            obj['pages'] = [page_json(p, lang) for p in e['pages']]
            with open(os.path.join(d, e['name'] + '.json'), 'w', encoding='utf-8') as f:
                json.dump(obj, f, indent=2, ensure_ascii=False)
                f.write('\n')
    print(f'{len(ENTRIES)} guide entries x 2 languages, {len(CHAPTERS)} chapters')


if __name__ == '__main__':
    main()
