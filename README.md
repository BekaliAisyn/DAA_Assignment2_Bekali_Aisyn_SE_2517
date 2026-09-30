# DAA Assignment 2 - Data Structures

In this project I implemented DynamicArray, MyLinkedList and MinHeap from scratch (only `int` and `int[]`, no java.util collections).
I also made a benchmark with 4 workloads, JUnit tests, plots and a report.

- GitHub link: _put your link here_ (branch `main`, tag `v1.0`)
- Report: `REPORT.md` (there is also `REPORT.pdf`)

## What you need
- JDK 17 or newer
- Maven
- Python 3 with pandas and matplotlib (only for the plots)

## How to build, test and run
Everything with one command:

    ./scripts/run_all.sh

Or step by step:

    mvn test
    mvn compile exec:java
    mvn exec:java -Dexec.mainClass=daa.benchmark.HeapBuildExperiment
    mvn exec:java -Dexec.mainClass=daa.benchmark.MemoryFootprint
    python3 scripts/plot.py

- `mvn test` runs the 42 JUnit 5 tests.
- `mvn compile exec:java` runs the main benchmark and writes `results/results.csv`. It takes about 1-2 minutes.
- HeapBuildExperiment is bonus B (`results/buildheap.csv`).
- MemoryFootprint is bonus A (`results/memory.csv`, needs JOL, Maven downloads it).
- `plot.py` makes the PNG charts in `results/plots/`.

If you don't have Maven you can use `scripts/run_without_maven.sh`.

## Folders
- `src/main/java/daa/structures` - IntList, DynamicArray, MyLinkedList, MinHeap
- `src/main/java/daa/metrics` - Counters
- `src/main/java/daa/benchmark` - Benchmark (W1-W4), HeapBuildExperiment, MemoryFootprint
- `src/test/java/daa/structures` - JUnit tests
- `results/` - results.csv, other csv files and the plots
- `scripts/` - run scripts and plot.py

## What the counters mean
- steps: one read of an array cell, or one visit of a list node (one heap comparison = 2 reads)
- moves: one element written into another cell (shift, copy when the array grows, append, swap = 2) or one pointer change in the list
- comparisons: one comparison between two elements

The counters are counted inside the code while it runs. I did not estimate them afterwards.

## How the benchmark works
- Every workload uses `new Random(42)`, so both structures get the same data.
- Filling the structure before the test is not timed and not counted.
- There is a warm-up first (results thrown away), then 5 runs, and I save the median time.
- The CSV columns are: workload, variant, structure, n, time_ms, steps, moves, comparisons. The variant is head or middle for W3 and `-` for the others.

## Git
`main` has only working code and the tag `v1.0`. I worked on the branches `feature/array`, `feature/list`, `feature/heap` and `feature/metrics` and merged them into main.
