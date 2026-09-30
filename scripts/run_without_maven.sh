#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."
OUT=target/classes
rm -rf target && mkdir -p "$OUT"
JOL="${2:-}"
SRC=$(find src/main -name '*.java')
if [ -z "$JOL" ]; then SRC=$(echo "$SRC" | grep -v MemoryFootprint); fi
javac --release 17 ${JOL:+-cp "$JOL"} -d "$OUT" $SRC

if [ -n "${1:-}" ]; then
  mkdir -p target/test-classes
  javac --release 17 -cp "$OUT:$1" -d target/test-classes $(find src/test -name '*.java')
  java -jar "$1" execute --class-path "$OUT:target/test-classes" --scan-class-path target/test-classes
fi

java -cp "$OUT" daa.benchmark.Benchmark
java -cp "$OUT" daa.benchmark.HeapBuildExperiment
if [ -n "$JOL" ]; then
  java -Djdk.attach.allowAttachSelf=true -cp "$OUT:$JOL" daa.benchmark.MemoryFootprint
fi
python3 scripts/plot.py
