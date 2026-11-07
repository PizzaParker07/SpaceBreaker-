#!/usr/bin/env python3
"""Visual regression for the sky (blocky planets, planet points, comets).

Runs the showcase scenarios below one after the other in a virtual display, shrinks every shot to 480x270 and
compares it with the baseline in docs/screenshots/qa/visual_baseline/. A shot whose mean absolute pixel difference
is above THRESHOLD (0..255) is reported; new shots are reported too.

Usage:
  python3 tools/qa/visual_regression.py            # run and compare
  python3 tools/qa/visual_regression.py --update   # run and make these shots the new baseline
  python3 tools/qa/visual_regression.py --no-run   # compare the shots already in run-showcase/screenshots
Exit code 1 when a shot changed or is missing.
"""
import os
import shutil
import subprocess
import sys
from pathlib import Path

from PIL import Image, ImageChops, ImageStat

ROOT = Path(__file__).resolve().parents[2]
SHOTS = ROOT / "run-showcase/screenshots"
BASE = ROOT / "docs/screenshots/qa/visual_baseline"
SCENARIOS = ["sky_planets", "sky_planets_mars", "blocky_lod", "space_scene", "inner_orbit"]
SIZE = (480, 270)
THRESHOLD = 6.0


def run(scenario):
    env = dict(os.environ, LIBGL_ALWAYS_SOFTWARE="1")
    cmd = ["timeout", "900", "xvfb-run", "-a", "-s", "-screen 0 1920x1080x24", "./gradlew", "runShowcase",
           f"-Pscenario={scenario}", f"-PshotPrefix=vr_{scenario}"]
    with open(ROOT / f"run-showcase/vr_{scenario}.log", "w") as log:
        return subprocess.call(cmd, cwd=ROOT, env=env, stdout=log, stderr=subprocess.STDOUT)


def small(path):
    return Image.open(path).convert("RGB").resize(SIZE)


def main():
    update = "--update" in sys.argv
    if "--no-run" not in sys.argv:
        for s in SCENARIOS:
            code = run(s)
            print(f"{s}: exit {code}")
    current = sorted(p for p in SHOTS.glob("vr_*.png"))
    if update:
        BASE.mkdir(parents=True, exist_ok=True)
        for p in current:
            small(p).save(BASE / (p.stem + ".jpg"), quality=90)
        print(f"baseline updated: {len(current)} shots in {BASE.relative_to(ROOT)}")
        return 0
    bad = 0
    seen = set()
    for p in current:
        ref = BASE / (p.stem + ".jpg")
        seen.add(ref.name)
        if not ref.exists():
            print(f"NEW      {p.stem}")
            bad += 1
            continue
        diff = ImageStat.Stat(ImageChops.difference(small(p), Image.open(ref).convert("RGB"))).mean
        d = sum(diff) / 3
        flag = "CHANGED " if d > THRESHOLD else "ok      "
        bad += d > THRESHOLD
        print(f"{flag} {p.stem}: {d:.1f}")
    for ref in sorted(BASE.glob("*.jpg")):
        if ref.name not in seen:
            print(f"MISSING  {ref.stem}")
            bad += 1
    return 1 if bad else 0


if __name__ == "__main__":
    sys.exit(main())
