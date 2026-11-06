#!/usr/bin/env python3
"""Refreshes the generated part of docs/QA.md.

Run after `./gradlew runGameTestServer` (which writes run-gametest/qa/progression.md):
    python3 tools/qa/update_qa_doc.py
Everything below the marker line in docs/QA.md is replaced by the progression report and the localization audit.
"""
import subprocess
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
DOC = ROOT / "docs/QA.md"
REPORT = ROOT / "run-gametest/qa/progression.md"
MARKER = "<!-- BEGIN GENERATED: do not edit below, run tools/qa/update_qa_doc.py -->"


def main():
    if not REPORT.exists():
        print("run ./gradlew runGameTestServer first", file=sys.stderr)
        return 1
    head = DOC.read_text(encoding="utf-8").split(MARKER)[0] if DOC.exists() else "# Quality assurance\n\n"
    lang = subprocess.run([sys.executable, str(ROOT / "tools/qa/lang_audit.py")], capture_output=True, text=True).stdout
    body = REPORT.read_text(encoding="utf-8").replace("\n## ", "\n### ").replace("\n# ", "\n## ")
    text = head.rstrip() + "\n\n" + MARKER + "\n\n# Generated progression report\n\n" + body.strip() + "\n\n" \
        + lang.replace("## Localization", "# Localization").replace("\n### ", "\n## ").strip() + "\n"
    DOC.write_text(text, encoding="utf-8")
    print(f"wrote {DOC}")
    return 0


if __name__ == "__main__":
    sys.exit(main())
