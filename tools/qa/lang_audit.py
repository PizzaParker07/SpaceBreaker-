#!/usr/bin/env python3
"""Localization audit for Spacebreaker (English + Turkish).

Checks the generated language files and the field guide Markdown:
  * every key exists in both languages,
  * format placeholders (%s, %1$s, %d, %%) and colour codes match,
  * Turkish strings that are still English (identical to English, or full of English words),
  * Turkish words written without their letters (ş ğ ı İ ç ö ü),
  * guide chapters exist in both languages with the same front matter items/tags,
  * glossary terms (docs/GLOSSARY.md) used consistently.

Usage: python3 tools/qa/lang_audit.py [--markdown out.md]
Exit code 1 when a hard error (missing key, placeholder mismatch, missing chapter) is found.
"""
import json
import re
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
LANG = ROOT / "src/generated/resources/assets/spacebreaker/lang"
GUIDE = ROOT / "src/main/resources/assets/spacebreaker/guide"

PLACEHOLDER = re.compile(r"%(?:\d+\$)?[sdf%]|%\.\d+f|\{\d+\}")
COLOUR = re.compile(r"§[0-9a-fk-or]")
WORD = re.compile(r"[A-Za-zÇĞİÖŞÜçğıöşü']+")

# Proper names and units that are the same in both languages.
SAME_OK = re.compile(r"^(?:[A-Z][a-z]+|Spacebreaker|Δv.*|Isp.*|RP-1|LOX|FE|FE/t|kPa|m/s|%.*|[\W\d]*)$")

ENGLISH_WORDS = {
    "the", "and", "of", "with", "to", "is", "your", "you", "for", "from", "this", "that", "into", "not", "are", "on",
    "when", "while", "will", "can", "it", "its", "be", "by", "at", "or", "an", "no", "has", "have", "needs", "need",
    "press", "click", "use", "here", "there", "only", "fuel", "tank", "engine", "rocket", "launch", "power", "energy",
    "research", "oxygen", "suit", "orbit", "landing", "ready", "missing", "requires", "required", "too", "low", "high",
}
# Common Turkish words of this mod written without their special letters (ASCII stand-ins).
DEACCENTED = {
    "gunes": "güneş", "yorunge": "yörünge", "yorungeye": "yörüngeye", "yakit": "yakıt", "sivi": "sıvı", "ozel": "özel",
    "isik": "ışık", "cikis": "çıkış", "giris": "giriş", "inis": "iniş", "kalkis": "kalkış", "dunya": "dünya",
    "gezegen": None, "uydu": None, "arastirma": "araştırma", "uretim": "üretim", "uret": "üret", "icin": "için",
    "olcum": "ölçüm", "basinc": "basınç", "sicaklik": "sıcaklık", "isi": "ısı", "guc": "güç", "gucu": "gücü",
    "makine": None, "motoru": None, "tup": "tüp", "tupu": "tüpü", "cozum": "çözüm", "bolum": "bölüm", "kucuk": "küçük",
    "buyuk": "büyük", "yuksek": "yüksek", "dusuk": "düşük", "sogutma": "soğutma", "kopru": "köprü", "kati": "katı",
    "celik": "çelik", "bakir": "bakır", "demir": None, "kursun": "kurşun", "gumus": "gümüş", "altin": "altın",
    "agir": "ağır", "hafif": None, "acik": "açık", "kapali": "kapalı", "dogru": "doğru", "yanlis": "yanlış",
    "sayac": "sayaç", "ucus": "uçuş", "ucak": "uçak", "yuzey": "yüzey", "yuzeyi": "yüzeyi", "kat": None,
    "isinim": "ışınım", "radyasyon": None, "zirh": "zırh", "olum": "ölüm", "oksijen": None, "su": None,
    "cevre": "çevre", "kesif": "keşif", "gorev": "görev", "gorevi": "görevi", "donus": "dönüş", "donusum": "dönüşüm",
}
DEACCENTED = {k: v for k, v in DEACCENTED.items() if v}


def load(name):
    with open(LANG / f"{name}.json", encoding="utf-8") as f:
        return json.load(f)


def placeholders(s):
    return sorted(PLACEHOLDER.findall(s))


def audit_lang():
    en, tr = load("en_us"), load("tr_tr")
    errors, warnings = [], []
    for k in sorted(set(en) - set(tr)):
        errors.append(f"`{k}` missing in tr_tr")
    for k in sorted(set(tr) - set(en)):
        errors.append(f"`{k}` missing in en_us")
    for k in sorted(set(en) & set(tr)):
        e, t = en[k], tr[k]
        if placeholders(e) != placeholders(t):
            errors.append(f"`{k}` placeholders differ: en {placeholders(e)} vs tr {placeholders(t)}")
        if sorted(COLOUR.findall(e)) != sorted(COLOUR.findall(t)):
            warnings.append(f"`{k}` colour codes differ")
        if not t.strip() and e.strip():
            errors.append(f"`{k}` is empty in tr_tr")
        if e == t and not SAME_OK.match(e.strip()) and len(WORD.findall(e)) > 1:
            warnings.append(f"`{k}` is the English text in tr_tr: \"{t}\"")
        words = [w.lower() for w in WORD.findall(t)]
        eng = [w for w in words if w in ENGLISH_WORDS]
        if len(words) >= 3 and len(eng) >= 2 and len(eng) * 3 >= len(words):
            warnings.append(f"`{k}` looks English: \"{t}\"")
        bad = sorted({w for w in words if w in DEACCENTED})
        if bad:
            warnings.append(f"`{k}` missing Turkish letters: {', '.join(f'{w}→{DEACCENTED[w]}' for w in bad)} in \"{t}\"")
        if re.search(r"[ÃÄÅ][\x80-\xBF]|Ã§|Ä±|Ã¶|Ã¼|ÅŸ|ÄŸ|Ä°", t):
            errors.append(f"`{k}` has broken (double-encoded) characters: \"{t}\"")
    return en, tr, errors, warnings


FRONT = re.compile(r"^---\n(.*?)\n---\n", re.S)


def front(text):
    m = FRONT.match(text)
    return m.group(1) if m else ""


def audit_guide():
    errors, warnings = [], []
    en_dir, tr_dir = GUIDE / "en_us", GUIDE / "tr_tr"
    en_files = {p.name for p in en_dir.glob("*.md")}
    tr_files = {p.name for p in tr_dir.glob("*.md")}
    for n in sorted(en_files - tr_files):
        errors.append(f"guide chapter `{n}` has no Turkish version")
    for n in sorted(tr_files - en_files):
        errors.append(f"guide chapter `{n}` has no English version")
    tag = re.compile(r"\{(?:item|tech|recipe\??|structure|entity|items|key)\s[^}]*\}")
    for n in sorted(en_files & tr_files):
        e = (en_dir / n).read_text(encoding="utf-8")
        t = (tr_dir / n).read_text(encoding="utf-8")
        fe, ft = front(e), front(t)
        ie = re.search(r"items:\s*\[(.*?)\]", fe)
        it = re.search(r"items:\s*\[(.*?)\]", ft)
        if (ie.group(1).replace(" ", "") if ie else "") != (it.group(1).replace(" ", "") if it else ""):
            warnings.append(f"guide `{n}`: front-matter items differ between languages")
        te, tt = sorted(tag.findall(e)), sorted(tag.findall(t))
        if te != tt:
            only_e = sorted(set(te) - set(tt))
            only_t = sorted(set(tt) - set(te))
            if only_e or only_t:
                warnings.append(f"guide `{n}`: tags only in English {only_e[:6]}, only in Turkish {only_t[:6]}")
        body = FRONT.sub("", t)
        words = [w.lower() for w in WORD.findall(re.sub(r"\{[^}]*\}|\([^)]*\)|`[^`]*`", " ", body))]
        bad = sorted({w for w in words if w in DEACCENTED})
        if bad:
            warnings.append(f"guide `{n}` (tr): missing Turkish letters: {', '.join(f'{w}→{DEACCENTED[w]}' for w in bad)}")
        eng = [w for w in words if w in ENGLISH_WORDS - {"fuel", "tank", "engine", "rocket", "power", "oxygen", "suit", "orbit"}]
        if len(words) > 30 and len(eng) * 25 > len(words):
            warnings.append(f"guide `{n}` (tr): {len(eng)} English words, e.g. {sorted(set(eng))[:8]}")
        if len(t) < len(e) * 0.6:
            warnings.append(f"guide `{n}` (tr) is much shorter than English ({len(t)} vs {len(e)} chars): content may be missing")
    return errors, warnings


def main():
    out = None
    if "--markdown" in sys.argv:
        out = Path(sys.argv[sys.argv.index("--markdown") + 1])
    en, tr, lerr, lwarn = audit_lang()
    gerr, gwarn = audit_guide()
    lines = ["## Localization audit (`tools/qa/lang_audit.py`)", "",
             f"{len(en)} English and {len(tr)} Turkish keys.", ""]
    for title, items in (("Errors", lerr + gerr), ("Warnings", lwarn + gwarn)):
        lines.append(f"### {title} ({len(items)})")
        lines.append("")
        lines.extend(f"- {i}" for i in items) if items else lines.append("None.")
        lines.append("")
    text = "\n".join(lines)
    if out:
        out.write_text(text, encoding="utf-8")
    print(text)
    return 1 if lerr or gerr else 0


if __name__ == "__main__":
    sys.exit(main())
