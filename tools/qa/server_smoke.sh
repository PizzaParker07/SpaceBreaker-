#!/usr/bin/env bash
# Dedicated-server smoke test: starts ./gradlew runServer on a fresh world (run/qa_smoke), then over RCON (localhost
# only, a throwaway password) loads and generates a chunk in every Spacebreaker dimension, stops the server and scans
# the log for errors.
# Usage: tools/qa/server_smoke.sh   (exit 1 on a failed dimension or an error in the log)
set -u
ROOT="$(cd "$(dirname "$0")/../.." && pwd)"
cd "$ROOT"
RUN=run
PORT=25599
PASS="qa-smoke-$RANDOM$RANDOM"
mkdir -p "$RUN"
echo "eula=true" > "$RUN/eula.txt"
cat > "$RUN/server.properties" <<EOF
level-name=qa_smoke
online-mode=false
spawn-protection=0
max-tick-time=-1
server-ip=127.0.0.1
enable-rcon=true
rcon.port=$PORT
rcon.password=$PASS
broadcast-rcon-to-ops=false
EOF
rm -rf "$RUN/qa_smoke"
DIMS=$(find src/main/resources/data src/generated/resources/data -path "*dimension/*.json" 2>/dev/null \
  | sed 's|.*/data/\([^/]*\)/dimension/\(.*\)\.json|\1:\2|' | sort -u)
LOG="$RUN/qa_server_smoke.log"
./gradlew runServer < /dev/null > "$LOG" 2>&1 &
PID=$!
for _ in $(seq 1 600); do
  grep -q 'Done (' "$LOG" 2>/dev/null && break
  kill -0 $PID 2>/dev/null || break
  sleep 1
done
if ! grep -q 'Done (' "$LOG"; then
  echo "server did not start"; tail -40 "$LOG"; kill $PID 2>/dev/null; exit 1
fi
OUT=$(python3 - "$PORT" "$PASS" $DIMS <<'PY'
import socket, struct, sys, time
port, pw, dims = int(sys.argv[1]), sys.argv[2], sys.argv[3:]
s = socket.create_connection(("127.0.0.1", port), timeout=600)
def send(i, t, body):
    data = struct.pack("<ii", i, t) + body.encode() + b"\0\0"
    s.sendall(struct.pack("<i", len(data)) + data)
def recv():
    n = struct.unpack("<i", s.recv(4))[0]
    buf = b""
    while len(buf) < n:
        buf += s.recv(n - len(buf))
    i, t = struct.unpack("<ii", buf[:8])
    return i, buf[8:-2].decode(errors="replace")
send(1, 3, pw); recv()
ok = 0
for n, d in enumerate(dims, start=2):
    t0 = time.time()
    send(n, 2, f"execute in {d} run forceload add 0 0")
    _, r = recv()
    good = "Marked chunk" in r
    ok += good
    print(f"{'OK  ' if good else 'FAIL'} {d} ({time.time() - t0:.1f}s): {r.strip()[:120]}")
print(f"LOADED {ok}/{len(dims)}")
send(999, 2, "stop")
PY
)
echo "$OUT"
for _ in $(seq 1 120); do kill -0 $PID 2>/dev/null || break; sleep 1; done
kill $PID 2>/dev/null
fail=0
echo "$OUT" | grep -q "^FAIL" && fail=1
echo "$OUT" | grep -q "^LOADED" || fail=1
errors=$(grep -E 'Exception|/ERROR\]|Error executing' "$LOG" | sort -u)
if [ -n "$errors" ]; then
  echo "errors in the log:"; echo "$errors" | head -40; fail=1
fi
grep -q 'Saving worlds\|Stopping server' "$LOG" && echo "server stopped cleanly" || { echo "server did not stop cleanly"; fail=1; }
rm -rf "$RUN/qa_smoke" "$RUN/server.properties"
exit $fail
