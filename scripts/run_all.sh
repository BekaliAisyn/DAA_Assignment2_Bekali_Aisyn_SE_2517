#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."

mvn -q test
mvn -q compile exec:java
mvn -q exec:java -Dexec.mainClass=daa.benchmark.HeapBuildExperiment
mvn -q exec:java -Dexec.mainClass=daa.benchmark.MemoryFootprint \
    -Djdk.attach.allowAttachSelf=true
python3 scripts/plot.py
