#!/usr/bin/env python3
"""Channel audit: every server -> client send of a mod payload is guarded by hasChannel.

A client without Spacebreaker (or with another version) has not agreed the mod's channels; NeoForge refuses to send
it a payload of an unknown channel and throws. So each PacketDistributor.sendToPlayer / sendToPlayers* / sendToAll*
and each connection.send(<custom payload>) in src/main must have a hasChannel check in the lines just before it (or be
inside a helper whose name says it checks). sendToServer (client -> server) is not checked.

Usage: python3 tools/qa/channel_audit.py   (exit 1 when a send is unguarded)
"""
import re
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
SRC = ROOT / "src/main/java"
SEND = re.compile(r"PacketDistributor\.(sendToPlayer|sendToPlayers\w*|sendToAllPlayers)\s*\(|\.connection\.send\(\s*new\s+(?!net\.minecraft\.network\.protocol\.game\.)")
GUARD = re.compile(r"hasChannel")
LOOKBACK = 8


def main():
    sends, bad = 0, []
    for f in sorted(SRC.rglob("*.java")):
        lines = f.read_text(encoding="utf-8").splitlines()
        for i, line in enumerate(lines):
            if line.strip().startswith("//") or "sendToServer" in line or not SEND.search(line):
                continue
            sends += 1
            window = "\n".join(lines[max(0, i - LOOKBACK):i + 1])
            if not GUARD.search(window):
                bad.append(f"{f.relative_to(ROOT)}:{i + 1}: {line.strip()}")
    print(f"{sends} server -> client sends, {len(bad)} without a hasChannel check")
    for b in bad:
        print("  UNGUARDED " + b)
    return 1 if bad else 0


if __name__ == "__main__":
    sys.exit(main())
