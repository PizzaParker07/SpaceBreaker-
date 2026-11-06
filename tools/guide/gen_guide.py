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
