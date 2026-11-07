#!/usr/bin/env python3
"""Asset and translation audit for Spacebreaker, driven by the registry dump of the GameTest run.

Reads run-gametest/qa/registry.tsv (written by qa/RegistryDumpTests) and checks, for every Spacebreaker
  * block: a blockstate file, every model it names, their parents and textures;
  * item: an item model (models/item/<id>.json), its parents and textures;
  * block, item, entity, fluid: a name in en_us and tr_tr;
  * sound event: an entry in sounds.json, its .ogg files, and its subtitle key in both languages.
Textures drawn in code at run time cannot be checked here; list them in CODE_TEXTURES.

Usage: python3 tools/qa/asset_audit.py [--markdown out.md]
Exit code 1 when anything is missing.
"""
import json
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
ASSETS = ROOT / "src/main/resources/assets"
GEN_ASSETS = ROOT / "src/generated/resources/assets"
LANG = GEN_ASSETS / "spacebreaker/lang"
DUMP = ROOT / "run-gametest/qa/registry.tsv"

# Textures produced in code (dynamic textures registered by the client), "namespace:path".
CODE_TEXTURES = set()
# Models built in code or by a custom loader: the parent chain is not followed past these.
BUILTIN_PARENTS = {"builtin/generated", "builtin/entity", "minecraft:builtin/generated", "minecraft:builtin/entity"}


def split(rl, default="minecraft"):
    if ":" in rl:
        ns, path = rl.split(":", 1)
    else:
        ns, path = default, rl
    return ns, path


def find(ns, sub, path, ext):
    for base in (ASSETS, GEN_ASSETS):
        p = base / ns / sub / (path + ext)
        if p.exists():
            return p
    return None


def is_vanilla(ns):
    return ns == "minecraft" or ns == "neoforge"


class Audit:
    def __init__(self):
        self.errors = []
        self.checked_models = {}

    def err(self, what):
        self.errors.append(what)

    def model(self, rl, owner, default_ns="spacebreaker"):
        """Checks a model, its parents and textures. Returns True when found."""
        ns, path = split(rl, default_ns)
        key = ns + ":" + path
        if key in self.checked_models:
            return self.checked_models[key]
        self.checked_models[key] = True
        if is_vanilla(ns) or key in BUILTIN_PARENTS or path.startswith("builtin/"):
            return True
        f = find(ns, "models", path, ".json")
        if f is None:
            self.checked_models[key] = False
            self.err(f"{owner}: model `{key}` missing")
            return False
        try:
            data = json.loads(f.read_text(encoding="utf-8"))
        except json.JSONDecodeError as e:
            self.err(f"{owner}: model `{key}` is not valid JSON ({e})")
            return False
        if "parent" in data:
            self.model(data["parent"], owner + " → " + key, "minecraft")
        for name, tex in (data.get("textures") or {}).items():
            if not isinstance(tex, str) or tex.startswith("#"):
                continue
            tns, tpath = split(tex)
            if is_vanilla(tns) or (tns + ":" + tpath) in CODE_TEXTURES:
                continue
            if find(tns, "textures", tpath, ".png") is None:
                self.err(f"{owner}: texture `{tns}:{tpath}` (model {key}, #{name}) missing")
        # NeoForge composite / loader models name other models
        for child in (data.get("children") or {}).values():
            if isinstance(child, dict) and "parent" in child:
                self.model(child["parent"], owner + " → " + key, "minecraft")
        return True

    def blockstate(self, block):
        f = find("spacebreaker", "blockstates", block, ".json")
        if f is None:
            self.err(f"block `{block}`: blockstate missing")
            return
        data = json.loads(f.read_text(encoding="utf-8"))
        models = set()

        def collect(v):
            if isinstance(v, dict):
                if "model" in v and isinstance(v["model"], str):
                    models.add(v["model"])
                for x in v.values():
                    collect(x)
            elif isinstance(v, list):
                for x in v:
                    collect(x)
        collect(data)
        if not models:
            self.err(f"block `{block}`: blockstate names no model")
        for m in models:
            self.model(m, f"block `{block}`")


def main():
    out_md = None
    if "--markdown" in sys.argv:
        out_md = Path(sys.argv[sys.argv.index("--markdown") + 1])
    if not DUMP.exists():
        print(f"{DUMP} missing: run ./gradlew runGameTestServer first", file=sys.stderr)
        return 2
    rows = [l.rstrip("\n").split("\t") for l in DUMP.read_text(encoding="utf-8").splitlines() if l.strip()]
    en = json.loads((LANG / "en_us.json").read_text(encoding="utf-8"))
    tr = json.loads((LANG / "tr_tr.json").read_text(encoding="utf-8"))
    a = Audit()
    counts = {}
    sounds_json = {}
    sj = ASSETS / "spacebreaker/sounds.json"
    if sj.exists():
        sounds_json = json.loads(sj.read_text(encoding="utf-8"))
    for kind, rid, flags, key in rows:
        counts[kind] = counts.get(kind, 0) + 1
        if kind == "block":
            if "liquid" in flags or "invisible" in flags:
                pass  # fluids and invisible blocks are drawn without a block model
            else:
                a.blockstate(rid)
        elif kind == "item":
            f = find("spacebreaker", "models", "item/" + rid, ".json")
            # NeoForge 1.21.1 also accepts a client item model registered in code; flag only plain missing files
            if f is None:
                a.err(f"item `{rid}`: item model missing")
            else:
                a.model("spacebreaker:item/" + rid, f"item `{rid}`")
        elif kind == "sound":
            entry = sounds_json.get(rid)
            if entry is None:
                a.err(f"sound `{rid}`: no entry in sounds.json")
            else:
                for s in entry.get("sounds", []):
                    name = s if isinstance(s, str) else s.get("name")
                    stype = "event" if isinstance(s, dict) and s.get("type") == "event" else "file"
                    if stype == "file":
                        sns, spath = split(name, "spacebreaker")
                        if not is_vanilla(sns) and find(sns, "sounds", spath, ".ogg") is None:
                            a.err(f"sound `{rid}`: file `{sns}:{spath}.ogg` missing")
                sub = entry.get("subtitle")
                if sub:
                    for lang, d in (("en_us", en), ("tr_tr", tr)):
                        if sub not in d:
                            a.err(f"sound `{rid}`: subtitle `{sub}` missing in {lang}")
            continue
        if kind in ("block", "item", "entity", "fluid") and key:
            if kind == "block" and ("liquid" in flags):
                continue  # a liquid block is named through its fluid
            for lang, d in (("en_us", en), ("tr_tr", tr)):
                if key not in d:
                    a.err(f"{kind} `{rid}`: name `{key}` missing in {lang}")
    # every literal translation key used in the code (tooltips, messages, HUD, screens) exists in both languages
    import re
    lit = re.compile(r'(?:translatable|translatableWithFallback|Component\.translatable|I18n\.get|keybind)\(\s*"([a-z0-9_.\-]+)"')
    code_keys = {}
    for src in (ROOT / "src/main/java").rglob("*.java"):
        for m in lit.finditer(src.read_text(encoding="utf-8")):
            code_keys.setdefault(m.group(1), src.relative_to(ROOT).as_posix())
    counts["literal key"] = len(code_keys)
    for key, where in sorted(code_keys.items()):
        if key.endswith(".") or key.startswith("key.") and "." not in key[4:]:
            continue  # a prefix completed at run time
        for lang, d in (("en_us", en), ("tr_tr", tr)):
            if key not in d:
                a.err(f"code key `{key}` ({where}) missing in {lang}")
    lines = [f"## Asset and translation audit (`tools/qa/asset_audit.py`)", "",
             "Checked: " + ", ".join(f"{v} {k}s" for k, v in sorted(counts.items()))
             + f", {len(a.checked_models)} models.", ""]
    if a.errors:
        lines.append(f"Missing ({len(a.errors)}):")
        lines.append("")
        lines += [f"- {e}" for e in sorted(set(a.errors))]
    else:
        lines.append("Nothing missing.")
    text = "\n".join(lines) + "\n"
    print(text)
    if out_md:
        out_md.write_text(text, encoding="utf-8")
    return 1 if a.errors else 0


if __name__ == "__main__":
    sys.exit(main())
