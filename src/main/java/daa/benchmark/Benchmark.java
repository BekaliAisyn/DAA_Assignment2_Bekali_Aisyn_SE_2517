package daa.benchmark;

import daa.structures.Counters;
import daa.structures.DynamicArray;
import daa.structures.IntList;
import daa.structures.MinHeap;
import daa.structures.MyLinkedList;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Locale;
import java.util.Random;

/**
 * Runs workloads W1-W4 on the hand-written structures and writes results/results.csv.
 *
 * <p>Methodology (see REPORT.md, section 3):
 * <ul>
 *   <li>Every workload is generated from {@code new Random(42)}, so all structures receive
 *       exactly the same data and the same operation sequence.</li>
 *   <li>Setup (filling the structure, generating indices/queries) is NOT timed and NOT counted.
 *       Only the operations that define the workload are timed and counted.</li>
 *   <li>A whole-suite pre-warm is discarded; then every configuration is warmed up for at
 *       least {@value #WARMUP} executions and 300 ms (JIT compilation), all discarded; then
 *       {@value #RUNS} measured executions are taken and the median time is reported.</li>
 *   <li>Counters are deterministic; the benchmark checks that all measured runs agree.</li>
 * </ul>
 *
 * <p>Usage: {@code Benchmark [output.csv]} (default {@code results/results.csv}).
 */
public final class Benchmark {
    static final long SEED = 42;
    static final int WARMUP = 3;              // minimum number of discarded warm-up executions
    static final long WARMUP_MIN_NS = 300_000_000L; // ... and warm up for at least 300 ms
    static final int RUNS = 5;
    static final int[] SIZES = {100, 1_000, 10_000, 100_000};

    static final int W1_GETS = 10_000;
    static final int W2_QUERIES = 1_000;
    static final int W3_OPS = 1_000;

    /** One measured execution of a workload. */
    static final class Result {
        long timeNs;
        long steps;
        long moves;
        long comparisons;
        long checksum; // guards against dead-code elimination and detects divergent behaviour
    }

    interface Task {
        Result run(int n);
    }

    interface ListFactory {
        IntList create();
    }

    /** Values are even and non-negative, so odd numbers are guaranteed to be absent (W2). */
    static int nextValue(Random rnd) {
        return 2 * rnd.nextInt(1 << 20);
    }

    // ---------------------------------------------------------------- workloads

    /** W1: fill with n values, then 10 000 random get(index). */
    static Task w1(ListFactory f) {
        return n -> {
            Random rnd = new Random(SEED);
            IntList list = f.create();
            for (int i = 0; i < n; i++) {
                list.add(nextValue(rnd));
            }
            int[] idx = new int[W1_GETS];
            for (int i = 0; i < W1_GETS; i++) {
                idx[i] = rnd.nextInt(n);
            }
            list.counters().reset();
            long sum = 0;
            long t0 = System.nanoTime();
            for (int i = 0; i < W1_GETS; i++) {
                sum += list.get(idx[i]);
            }
            long t1 = System.nanoTime();
            return result(t1 - t0, list.counters(), sum);
        };
    }

    /** W2: fill with n values, then 1 000 contains(x); half of the queries hit, half miss. */
    static Task w2(ListFactory f) {
        return n -> {
            Random rnd = new Random(SEED);
            IntList list = f.create();
            int[] data = new int[n];
            for (int i = 0; i < n; i++) {
                data[i] = nextValue(rnd);
                list.add(data[i]);
            }
            int[] queries = new int[W2_QUERIES];
            for (int i = 0; i < W2_QUERIES; i++) {
                queries[i] = (i % 2 == 0) ? data[rnd.nextInt(n)] : 2 * rnd.nextInt(1 << 20) + 1;
            }
            list.counters().reset();
            long hits = 0;
            long t0 = System.nanoTime();
            for (int i = 0; i < W2_QUERIES; i++) {
                if (list.contains(queries[i])) {
                    hits++;
                }
            }
            long t1 = System.nanoTime();
            if (hits != W2_QUERIES / 2) {
                throw new IllegalStateException("W2: expected exactly half hits, got " + hits);
            }
            return result(t1 - t0, list.counters(), hits);
        };
    }

    /** W3: fill with n values, then 1 000 insertions and 1 000 removals at index 0 or n/2. */
    static Task w3(ListFactory f, boolean head) {
        return n -> {
            Random rnd = new Random(SEED);
            IntList list = f.create();
            for (int i = 0; i < n; i++) {
                list.add(nextValue(rnd));
            }
            int[] inserted = new int[W3_OPS];
            for (int i = 0; i < W3_OPS; i++) {
                inserted[i] = nextValue(rnd);
            }
            int index = head ? 0 : n / 2;
            list.counters().reset();
            long sum = 0;
            long t0 = System.nanoTime();
            for (int i = 0; i < W3_OPS; i++) {
                list.add(index, inserted[i]);
            }
            for (int i = 0; i < W3_OPS; i++) {
                sum += list.remove(index);
            }
            long t1 = System.nanoTime();
            if (list.size() != n) {
                throw new IllegalStateException("W3: size should be back to n");
            }
            return result(t1 - t0, list.counters(), sum);
        };
    }

    /** W4: insert n values into a MinHeap, then extractMin n times; results must be sorted. */
    static Task w4() {
        return n -> {
            Random rnd = new Random(SEED);
            int[] values = new int[n];
            for (int i = 0; i < n; i++) {
                values[i] = nextValue(rnd);
            }
            MinHeap heap = new MinHeap();
            long sum = 0;
            boolean sorted = true;
            long t0 = System.nanoTime();
            for (int i = 0; i < n; i++) {
                heap.insert(values[i]);
            }
            int previous = Integer.MIN_VALUE;
            for (int i = 0; i < n; i++) {
                int m = heap.extractMin();
                sorted &= previous <= m;
                previous = m;
                sum += m;
            }
            long t1 = System.nanoTime();
            if (!sorted || !heap.isEmpty()) {
                throw new IllegalStateException("W4: extractMin order is not non-decreasing");
            }
            return result(t1 - t0, heap.counters(), sum);
        };
    }

    private static Result result(long ns, Counters c, long checksum) {
        Result r = new Result();
        r.timeNs = ns;
        r.steps = c.steps();
        r.moves = c.moves();
        r.comparisons = c.comparisons();
        r.checksum = checksum;
        return r;
    }

    // ---------------------------------------------------------------- driver

    /** Warm-up, {@value #RUNS} measured runs, median time; verifies determinism. */
    static Result measure(Task task, int n) {
        long start = System.nanoTime();
        for (int i = 0; i < WARMUP || System.nanoTime() - start < WARMUP_MIN_NS; i++) {
            task.run(n);
        }
        long[] times = new long[RUNS];
        Result last = null;
        for (int i = 0; i < RUNS; i++) {
            Result r = task.run(n);
            if (last != null && (r.steps != last.steps || r.moves != last.moves
                    || r.comparisons != last.comparisons || r.checksum != last.checksum)) {
                throw new IllegalStateException("non-deterministic run");
            }
            times[i] = r.timeNs;
            last = r;
        }
        last.timeNs = median(times);
        return last;
    }

    /** Median via insertion sort (no java.util.Arrays needed for 5 numbers). */
    static long median(long[] values) {
        long[] v = new long[values.length];
        for (int i = 0; i < v.length; i++) {
            v[i] = values[i];
        }
        for (int i = 1; i < v.length; i++) {
            long key = v[i];
            int j = i - 1;
            while (j >= 0 && v[j] > key) {
                v[j + 1] = v[j];
                j--;
            }
            v[j + 1] = key;
        }
        return v[v.length / 2];
    }

    public static void main(String[] args) throws IOException {
        Path out = Paths.get(args.length > 0 ? args[0] : "results/results.csv");
        if (out.getParent() != null) {
            Files.createDirectories(out.getParent());
        }
        ListFactory array = DynamicArray::new;
        ListFactory linked = MyLinkedList::new;

        // JVM-wide pre-warm: run every workload once at n = 1 000 and throw the results away,
        // so that the first measured size (n = 100) is not timed with cold, interpreted code.
        for (Task t : new Task[] {w1(array), w1(linked), w2(array), w2(linked),
                w3(array, true), w3(linked, true), w3(array, false), w3(linked, false), w4()}) {
            measure(t, 1_000);
        }

        try (PrintWriter csv = new PrintWriter(Files.newBufferedWriter(out, StandardCharsets.UTF_8))) {
            csv.println("workload,variant,structure,n,time_ms,steps,moves,comparisons");
            for (int n : SIZES) {
                long checksumW1 = check(measureRow(csv, "W1", "random", "DynamicArray", n, w1(array)),
                        measureRow(csv, "W1", "random", "MyLinkedList", n, w1(linked)));
                long checksumW2 = check(measureRow(csv, "W2", "half_hit", "DynamicArray", n, w2(array)),
                        measureRow(csv, "W2", "half_hit", "MyLinkedList", n, w2(linked)));
                check(measureRow(csv, "W3", "head", "DynamicArray", n, w3(array, true)),
                        measureRow(csv, "W3", "head", "MyLinkedList", n, w3(linked, true)));
                check(measureRow(csv, "W3", "middle", "DynamicArray", n, w3(array, false)),
                        measureRow(csv, "W3", "middle", "MyLinkedList", n, w3(linked, false)));
                measureRow(csv, "W4", "insert_extract", "MinHeap", n, w4());
                System.out.printf(Locale.ROOT, "n=%d done (W1 checksum %d, W2 hits %d)%n", n, checksumW1, checksumW2);
            }
        }
        System.out.println("Wrote " + out.toAbsolutePath());
    }

    /** Both structures must return identical answers for the same workload. */
    private static long check(Result a, Result b) {
        if (a.checksum != b.checksum) {
            throw new IllegalStateException("structures disagree: " + a.checksum + " vs " + b.checksum);
        }
        return a.checksum;
    }

    private static Result measureRow(PrintWriter csv, String workload, String variant,
                                     String structure, int n, Task task) {
        Result r = measure(task, n);
        csv.printf(Locale.ROOT, "%s,%s,%s,%d,%.6f,%d,%d,%d%n",
                workload, variant, structure, n, r.timeNs / 1e6, r.steps, r.moves, r.comparisons);
        csv.flush();
        return r;
    }
}
