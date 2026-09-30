#!/usr/bin/env bash
# One command: build, run the unit tests, run the benchmark (-> results/results.csv), draw the plots.
# Usage: ./scripts/run_all.sh
set -euo pipefail
cd "$(dirname "$0")/.."

mvn -q test
mvn -q compile exec:java                                                     # results/results.csv
mvn -q exec:java -Dexec.mainClass=daa.benchmark.HeapBuildExperiment          # results/buildheap.csv (bonus B)
mvn -q exec:java -Dexec.mainClass=daa.benchmark.MemoryFootprint \
    -Djdk.attach.allowAttachSelf=true                                        # results/memory.csv (bonus A)
python3 scripts/plot.py                                                      # results/plots/*.png
