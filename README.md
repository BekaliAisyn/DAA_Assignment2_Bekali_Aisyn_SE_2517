# DAA Assignment 2 - Data Structures (DynamicArray, MyLinkedList, MinHeap)

Three data structures written from scratch (`int`/`int[]` only, no `java.util` collections),
loop-invariant proofs, four benchmark workloads with honest counters, JUnit 5 tests and a report.

- GitHub repository: _<add your link here>_ (branch `main`, tag `v1.0`)
- Report: [REPORT.md](REPORT.md) - complexity table, proofs, plots, discussion, bonus A and B

## Requirements
JDK 17+ (tested with 21), Maven 3.8+, Python 3 with `pandas` and `matplotlib` (plots only).

## Build, test, benchmark - one command
```bash
./scripts/run_all.sh
```
which is the same as:
```bash
mvn test                                                         # 42 JUnit 5 tests
mvn compile exec:java                                            # -> results/results.csv
mvn exec:java -Dexec.mainClass=daa.benchmark.HeapBuildExperiment # -> results/buildheap.csv (bonus B)
mvn exec:java -Dexec.mainClass=daa.benchmark.MemoryFootprint     # -> results/memory.csv, jol_layouts.txt (bonus A)
python3 scripts/plot.py                                          # -> results/plots/*.png
```
The benchmark takes about 1.5 minutes (time-based JIT warm-up). Without Maven:
`./scripts/run_without_maven.sh [junit-console-standalone.jar] [jol-core.jar]`.

## Layout
```
src/main/java/daa/structures/   IntList, Counters, DynamicArray, MyLinkedList, MinHeap
src/main/java/daa/benchmark/    Benchmark (W1-W4), HeapBuildExperiment (bonus B), MemoryFootprint (bonus A)
src/test/java/daa/structures/   JUnit 5 tests (shared contract test for both lists + heap tests)
results/results.csv             workload,variant,structure,n,time_ms,steps,moves,comparisons
results/plots/                  PNG charts (time vs n, steps/moves/comparisons vs n, bonus)
scripts/                        run_all.sh, run_without_maven.sh, plot.py
```

## Counters (same rules for every structure)
- **steps**: one array-cell read or one list-node dereference (a heap comparison = 2 steps)
- **moves**: one element written to another cell (shift, resize copy, append, swap = 2) or one pointer assignment
- **comparisons**: one comparison between two elements

## Methodology
Every workload starts from `new Random(42)`. Setup (filling, generating queries) is not timed or counted.
Whole-suite pre-warm, then >= 3 executions and >= 300 ms of warm-up per configuration, then the **median of 5 runs**.
Counters are deterministic and checked for equality across the 5 runs.

## Git workflow
`main` holds working code only (tag `v1.0`); development happened on `feature/array`, `feature/list`,
`feature/heap`, `feature/metrics`, merged with `--no-ff`.
