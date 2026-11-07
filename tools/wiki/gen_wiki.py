"""Builds a GitHub-style wiki from the field guide's Markdown chapters.

    python3 tools/wiki/gen_wiki.py

Reads src/main/resources/assets/spacebreaker/guide (book.json and the tr_tr / en_us chapters) and writes docs/wiki:
one page per chapter (Turkish first, the English text in a collapsible section), Home.md and _Sidebar.md. Item, tech and
entity names come from the generated language files; vanilla names from the local Minecraft assets when present.
Recipes are read from the data folders and written as tables; screenshots from docs/screenshots are reused.
"""
import glob
import json
import os
import re
import shutil

ROOT = os.path.abspath(os.path.join(os.path.dirname(os.path.abspath(__file__)), '..', '..'))
GUIDE = os.path.join(ROOT, 'src', 'main', 'resources', 'assets', 'spacebreaker', 'guide')
OUT = os.path.join(ROOT, 'docs', 'wiki')
LANGS = ('tr_tr', 'en_us')
DATA_DIRS = [os.path.join(ROOT, 'src', 'generated', 'resources', 'data'), os.path.join(ROOT, 'src', 'main', 'resources', 'data')]

# default keys (the book shows the player's own binding; the wiki shows the default)
KEYS = {
    'tr_tr': {'key.use': 'Sağ Tık', 'key.attack': 'Sol Tık', 'key.sneak': 'Shift', 'key.jump': 'Boşluk',
              'key.togglePerspective': 'F5', 'key.inventory': 'E', 'key.spacebreaker.research': 'J',
              'key.spacebreaker.guide': '(atanmamış)', 'key.spacebreaker.launch': 'R', 'key.spacebreaker.star_map': 'M'},
    'en_us': {'key.use': 'Right Click', 'key.attack': 'Left Click', 'key.sneak': 'Shift', 'key.jump': 'Space',
              'key.togglePerspective': 'F5', 'key.inventory': 'E', 'key.spacebreaker.research': 'J',
              'key.spacebreaker.guide': '(unbound)', 'key.spacebreaker.launch': 'R', 'key.spacebreaker.star_map': 'M'},
}
TEXT = {
    'tr_tr': {'english': 'English', 'research': 'İlgili araştırma', 'recipe': 'Tarif', 'in': 'Girdi', 'machine': 'Makine',
              'out': 'Çıktı', 'craft': 'Çalışma Masası', 'shapeless': 'şekilsiz', 'creature': 'Canlı', 'structure': 'Yapı',
              'hologram': 'Hologram Projektörü ile yerleştirilebilir', 'home': 'Spacebreaker Wiki',
              'home_intro': 'Bu wiki, oyun içindeki **Saha Rehberi**nin metinlerinden otomatik üretilir.',
              'energy': '{e} FE/t · {t} sn', 'chance': '%{p} şansla'},
    'en_us': {'english': 'English', 'research': 'Related research', 'recipe': 'Recipe', 'in': 'Input', 'machine': 'Machine',
              'out': 'Output', 'craft': 'Crafting Table', 'shapeless': 'shapeless', 'creature': 'Creature', 'structure': 'Structure',
              'hologram': 'can be placed with the Hologram Projector', 'home': 'Spacebreaker Wiki',
              'home_intro': 'This wiki is generated from the in-game **Field Guide**.',
              'energy': '{e} FE/t · {t} s', 'chance': '{p}% chance'},
}
# chapter -> screenshot shown at the top of its page (paths under docs/screenshots)
SHOTS = {
    'launch_pad': 'guide/tr_05_launch_pad_3d.jpg', 'first_flight': 'guide/tr_03_first_flight.jpg',
    'processing': 'guide/tr_02_machine_recipe.jpg', 'moon_life': 'guide/tr_04_moon_creature.jpg',
    'isru': 'guide/tr_06_isru.jpg', 'progression': 'guide/tr_07_progression.jpg', 'rocket_parts': 'guide/tr_08_rocket_parts.jpg',
}


def load_json(path, default=None):
    try:
        with open(path, encoding='utf-8') as f:
            return json.load(f)
    except (OSError, ValueError):
        return default


def vanilla_lang():
    """Vanilla names from the local game assets (Gradle cache), if they are there."""
    out = {'tr_tr': {}, 'en_us': {}}
    base = os.path.expanduser('~/.gradle/caches/neoformruntime/assets')
    index = load_json(os.path.join(base, 'indexes', '17.json'), {}).get('objects', {})
    obj = index.get('minecraft/lang/tr_tr.json')
    if obj:
        out['tr_tr'] = load_json(os.path.join(base, 'objects', obj['hash'][:2], obj['hash']), {})
    jar = os.path.expanduser('~/.gradle/caches/neoformruntime/artifacts/minecraft_1.21.1_client.jar')
    if os.path.exists(jar):
        import zipfile
        with zipfile.ZipFile(jar) as z:
            try:
                out['en_us'] = json.loads(z.read('assets/minecraft/lang/en_us.json').decode('utf-8'))
            except KeyError:
                pass
    return out


VANILLA = vanilla_lang()
LANG = {l: load_json(os.path.join(ROOT, 'src', 'generated', 'resources', 'assets', 'spacebreaker', 'lang', l + '.json'), {}) for l in LANGS}


def tr(lang, key):
    return LANG[lang].get(key) or VANILLA[lang].get(key) or LANG['en_us'].get(key) or VANILLA['en_us'].get(key)


def item_name(lang, rid):
    ns, path = rid.split(':', 1) if ':' in rid else ('minecraft', rid)
    for kind in ('item', 'block'):
        n = tr(lang, f'{kind}.{ns}.{path}')
        if n:
            return n
    return path.replace('_', ' ').title()


def tech_name(lang, rid):
    ns, path = rid.split(':', 1)
    return tr(lang, f'tech.{ns}.{path}') or path.replace('_', ' ').title()


def entity_name(lang, rid):
    ns, path = rid.split(':', 1)
    return tr(lang, f'entity.{ns}.{path}') or path.replace('_', ' ').title()


SINGULAR = {'ingots': '{m}_ingot', 'plates': '{m}_plate', 'dusts': '{m}_dust', 'wires': '{m}_wire', 'rods': '{m}_rod',
            'nuggets': '{m}_nugget', 'raw_materials': 'raw_{m}', 'gems': '{m}', 'storage_blocks': '{m}_block', 'ores': '{m}_ore'}
SIMPLE_TAGS = {'c:strings': 'minecraft:string', 'c:leathers': 'minecraft:leather', 'c:glass_blocks': 'minecraft:glass',
               'c:dusts/redstone': 'minecraft:redstone', 'c:gems/lapis': 'minecraft:lapis_lazuli', 'c:tools/shear': 'minecraft:shears',
               'c:tools/shears': 'minecraft:shears', 'minecraft:wool': 'minecraft:white_wool', 'minecraft:planks': 'minecraft:oak_planks',
               'minecraft:logs': 'minecraft:oak_log', 'minecraft:coals': 'minecraft:coal'}


def tag_item(tag):
    """A representative item for a tag (the first entry of our own tag files, or a guess from the c: naming)."""
    if tag in SIMPLE_TAGS:
        return SIMPLE_TAGS[tag]
    ns, path = tag.split(':', 1)
    for base in DATA_DIRS:
        data = load_json(os.path.join(base, ns, 'tags', 'item', path + '.json'))
        if data:
            for v in data.get('values', []):
                v = v if isinstance(v, str) else v.get('id', '')
                if v.startswith('#'):
                    return tag_item(v[1:])
                return v
    if ns == 'c' and '/' in path:
        kind, mat = path.split('/', 1)
        if kind in SINGULAR:
            name = SINGULAR[kind].format(m=mat)
            for cand in ('spacebreaker:' + name, 'minecraft:' + name):
                if cand.split(':')[1] and (tr('en_us', 'item.' + cand.replace(':', '.')) or tr('en_us', 'block.' + cand.replace(':', '.'))):
                    return cand
            return 'minecraft:' + name
    return None


def ingredient(lang, ing):
    if ing is None:
        return ''
    if isinstance(ing, list):
        ing = ing[0] if ing else None
        return ingredient(lang, ing)
    if 'item' in ing:
        return item_name(lang, ing['item'])
    if 'id' in ing:
        return item_name(lang, ing['id'])
    if 'tag' in ing:
        it = tag_item(ing['tag'])
        return item_name(lang, it) if it else '#' + ing['tag']
    if 'ingredient' in ing:
        return ingredient(lang, ing['ingredient'])
    return '?'


def fluid(lang, f):
    if not f:
        return ''
    amount = f.get('amount', 0)
    name = f.get('id') or f.get('fluid') or ('#' + f['tag'] if 'tag' in f else '?')
    if name.startswith('#'):
        label = name[1:].split(':')[-1].replace('_', ' ')
    else:
        ns, path = name.split(':', 1)
        label = tr(lang, f'fluid_type.{ns}.{path}') or tr(lang, f'block.{ns}.{path}') or path.replace('_', ' ')
    return f'{label} ({amount} mB)'


def stack(lang, s):
    n = item_name(lang, s.get('id') or s.get('item', '?'))
    c = s.get('count', 1)
    return f'{n} ×{c}' if c > 1 else n


def find_recipe(rid):
    ns, path = rid.split(':', 1)
    for base in DATA_DIRS:
        r = load_json(os.path.join(base, ns, 'recipe', path + '.json'))
        if r:
            return r
    return None


def recipe_md(lang, rid, machines):
    r = find_recipe(rid)
    if not r:
        return ''
    t = TEXT[lang]
    rtype = r.get('type', '')
    machine = machines.get(rtype)
    machine = item_name(lang, machine) if machine else t['craft']
    out = []
    if rtype in ('minecraft:crafting_shaped',):
        key = r.get('key', {})
        rows = r.get('pattern', [])
        width = max(len(x) for x in rows)
        out.append('| ' + ' | '.join([' '] * width) + ' |')
        out.append('|' + '---|' * width)
        for row in rows:
            cells = [ingredient(lang, key.get(ch)) if ch != ' ' else ' ' for ch in row.ljust(width)]
            out.append('| ' + ' | '.join(cells) + ' |')
        out.append('')
        out.append(f'→ **{stack(lang, r["result"])}** ({t["craft"]})')
        return '\n'.join(out)
    if rtype.endswith('shapeless') or rtype == 'spacebreaker:tool_shapeless':
        ins = ', '.join(ingredient(lang, i) for i in r.get('ingredients', []))
        return f'{ins} → **{stack(lang, r["result"])}** ({t["craft"]}, {t["shapeless"]})'
    if rtype in ('minecraft:smelting', 'minecraft:blasting', 'minecraft:smoking', 'minecraft:campfire_cooking', 'minecraft:stonecutting'):
        return f'{ingredient(lang, r.get("ingredient"))} → **{stack(lang, r["result"])}** ({machine})'
    if rtype == 'minecraft:smithing_transform':
        ins = ' + '.join(ingredient(lang, r.get(k)) for k in ('template', 'base', 'addition'))
        return f'{ins} → **{stack(lang, r["result"])}** ({machine})'
    # Spacebreaker processing machines
    ins = []
    for i in r.get('ingredients', []):
        c = i.get('count', 1)
        n = ingredient(lang, i)
        ins.append(f'{n} ×{c}' if c > 1 else n)
    for k in ('fluid_input', 'fluid_input_2'):
        if k in r:
            ins.append(fluid(lang, r[k]))
    outs = [stack(lang, s) for s in r.get('results', [])]
    if 'bonus' in r:
        outs.append(f'{stack(lang, r["bonus"])} ({t["chance"].format(p=int(round(r.get("bonus_chance", 0) * 100)))})')
    outs += [fluid(lang, f) for f in r.get('fluid_results', [])]
    info = t['energy'].format(e=r.get('energy', 20), t=round(r.get('time', 100) / 20, 1)).replace('.0 ', ' ')
    return (f'| {t["in"]} | {t["machine"]} | {t["out"]} |\n|---|---|---|\n'
            f'| {"<br>".join(ins)} | {machine}<br>{info} | {"<br>".join(outs)} |')


def convert(lang, text, chapters, item_pages, machines, current=None):
    """Field guide Markdown -> GitHub Markdown."""
    m = re.match(r'---\n(.*?)\n---\n?', text, re.S)
    meta = {}
    if m:
        for line in m.group(1).splitlines():
            k, _, v = line.partition(':')
            meta[k.strip()] = v.strip()
        text = text[m.end():]
    t = TEXT[lang]
    out = []
    if meta.get('research'):
        out.append(f'*{t["research"]}: {tech_name(lang, meta["research"])}*\n')

    def inline(s):
        s = s.replace('\\*', '&#42;')
        s = re.sub(r'\{(?:color [^}]*|blue|orange|green|purple|red|gold|gray|grey|/color|/)\}', '', s)

        def item(mm):
            parts = mm.group(1).split(None, 1)
            rid = parts[0]
            name = parts[1] if len(parts) > 1 else item_name(lang, rid)
            page = item_pages.get(rid)
            return f'[{name}]({page})' if page and page != current else f'**{name}**'
        s = re.sub(r'\{item\??\s+([^}]+)\}', item, s)
        s = re.sub(r'\{key\s+([^}]+)\}', lambda mm: f'`{KEYS[lang].get(mm.group(1).strip(), mm.group(1).strip())}`', s)
        s = re.sub(r'\{tech\s+([^}]+)\}', lambda mm: f'*{tech_name(lang, mm.group(1).strip())}*', s)

        def link(mm):
            label, dest = mm.group(1), mm.group(2)
            if dest.startswith('http'):
                return mm.group(0)
            ch, _, anchor = dest.partition('#')
            ch = ch.split(':')[-1]
            target = (ch if ch in chapters else '') + ('#' + anchor if anchor else '')
            return f'[{label}]({target or "#"})'
        return re.sub(r'\[([^\]]+)\]\(([^)]+)\)', link, s)

    for raw in text.split('\n'):
        line = raw.strip()
        tag = re.fullmatch(r'\{(\w+)(\??)\s*([^}]*)\}', line)
        if tag:
            name, optional, args = tag.group(1), tag.group(2), tag.group(3).split()
            if name == 'page':
                continue
            if name == 'recipe' and args:
                md = recipe_md(lang, args[0], machines)
                if md:
                    out.append('\n' + md + '\n')
                continue
            if name == 'craft':
                continue
            if name == 'items' and args:
                out.append('\n' + ' · '.join(f'**{item_name(lang, a)}**' for a in args) + '\n')
                continue
            if name == 'entity' and args:
                out.append(f'\n**{t["creature"]}:** {entity_name(lang, args[0])}\n')
                continue
            if name == 'structure' and args:
                rid = args[0]
                n = tr(lang, f'multiblock.{rid.replace(":", ".")}') or rid.split(':')[1].replace('_', ' ')
                out.append(f'\n**{t["structure"]}:** {n} ({t["hologram"]})\n')
                continue
            if name == 'image' and args:
                ns, path = args[0].split(':', 1)
                src = os.path.join(ROOT, 'src', 'main', 'resources', 'assets', ns, path)
                if os.path.exists(src):
                    os.makedirs(os.path.join(OUT, 'images'), exist_ok=True)
                    dst = os.path.join(OUT, 'images', os.path.basename(path))
                    shutil.copyfile(src, dst)
                    out.append(f'![](images/{os.path.basename(path)})\n')
                continue
        hm = re.match(r'(#+)\s*(.*?)\s*(\{#([\w-]+)\})?$', line)
        if line.startswith('#') and hm:
            anchor = hm.group(4)
            if anchor:
                out.append(f'<a id="{anchor}"></a>')
            out.append('#' * (len(hm.group(1)) + 1) + ' ' + inline(hm.group(2)))
            continue
        if line.startswith('>!'):
            out.append('\n> [!WARNING]\n> ' + inline(line[2:].strip()))
            continue
        if line.startswith('>') and not line.startswith('> [!') and not (out and out[-1].startswith('> ')):
            out.append('\n> [!TIP]\n> ' + inline(line[1:].strip()))
            continue
        out.append(inline(raw.rstrip()))
    return meta.get('title', ''), re.sub(r'\n{3,}', '\n\n', '\n'.join(out)).strip() + '\n'


def main():
    book = load_json(os.path.join(GUIDE, 'book.json'))
    machines = book.get('machines', {})
    chapters = [c for cat in book['categories'] for c in cat['chapters']]
    names = {l: load_json(os.path.join(GUIDE, l, 'book.json'), {}) for l in LANGS}
    # item -> chapter page (from the chapters' items: front matter, first one wins)
    item_pages = {}
    for ch in chapters:
        src = open(os.path.join(GUIDE, 'en_us', ch + '.md'), encoding='utf-8').read()
        m = re.search(r'^items:\s*(.*)$', src, re.M)
        if m:
            for i in re.split(r'[\s,\[\]]+', m.group(1)):
                if i:
                    item_pages.setdefault(i, ch)
    if os.path.isdir(OUT):
        shutil.rmtree(OUT)
    os.makedirs(OUT)
    titles = {}
    for ch in chapters:
        parts = {}
        for l in LANGS:
            path = os.path.join(GUIDE, l, ch + '.md')
            if not os.path.exists(path):
                path = os.path.join(GUIDE, 'en_us', ch + '.md')
            parts[l] = convert(l, open(path, encoding='utf-8').read(), chapters, item_pages, machines, ch)
        titles[ch] = (parts['tr_tr'][0], parts['en_us'][0])
        page = [f'# {parts["tr_tr"][0]}', '']
        if ch in SHOTS and os.path.exists(os.path.join(ROOT, 'docs', 'screenshots', SHOTS[ch])):
            page += [f'![](../screenshots/{SHOTS[ch]})', '']
        page += [parts['tr_tr'][1], '<details>', f'<summary>{TEXT["en_us"]["english"]}: {parts["en_us"][0]}</summary>', '',
                 parts['en_us'][1], '</details>', '']
        with open(os.path.join(OUT, ch + '.md'), 'w', encoding='utf-8') as f:
            f.write('\n'.join(page))
    home = [f'# {TEXT["tr_tr"]["home"]}', '', TEXT['tr_tr']['home_intro'], '']
    side = [f'**[{names["tr_tr"].get("title", "Saha Rehberi")}](Home)**', '']
    for cat in book['categories']:
        tr_name = names['tr_tr'].get('categories', {}).get(cat['id'], cat['id'])
        en_name = names['en_us'].get('categories', {}).get(cat['id'], cat['id'])
        home += [f'## {tr_name} <sub>({en_name})</sub>', '']
        side += [f'**{tr_name}**', '']
        for ch in cat['chapters']:
            home.append(f'- [{titles[ch][0]}]({ch}) · *{titles[ch][1]}*')
            side.append(f'- [{titles[ch][0]}]({ch})')
        home.append('')
        side.append('')
    home += ['<details>', '<summary>English</summary>', '', TEXT['en_us']['home_intro'], '', '</details>', '']
    with open(os.path.join(OUT, 'Home.md'), 'w', encoding='utf-8') as f:
        f.write('\n'.join(home))
    with open(os.path.join(OUT, '_Sidebar.md'), 'w', encoding='utf-8') as f:
        f.write('\n'.join(side))
    print(f'{len(chapters)} pages written to {os.path.relpath(OUT, ROOT)}')


if __name__ == '__main__':
    main()
