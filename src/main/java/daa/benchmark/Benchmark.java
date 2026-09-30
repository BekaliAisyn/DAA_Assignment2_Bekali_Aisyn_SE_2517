package daa.benchmark;

import daa.metrics.Counters;
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

public final class Benchmark {
    static final long SEED = 42;
    static final int WARMUP = 3;
    static final long WARMUP_MIN_NS = 300_000_000L;
    static final int RUNS = 5;
    static final int[] SIZES = {100, 1_000, 10_000, 100_000};

    static final int W1_GETS = 10_000;
    static final int W2_QUERIES = 1_000;
    static final int W3_OPS = 1_000;

    static final class Result {
        long timeNs;
        long steps;
        long moves;
        long comparisons;
        long checksum;
    }

    interface Task {
        Result run(int n);
    }

    interface ListFactory {
        IntList create();
    }

    static int nextValue(Random rnd) {
        return 2 * rnd.nextInt(1 << 20);
    }

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

        for (Task t : new Task[] {w1(array), w1(linked), w2(array), w2(linked),
                w3(array, true), w3(linked, true), w3(array, false), w3(linked, false), w4()}) {
            measure(t, 1_000);
        }

        try (PrintWriter csv = new PrintWriter(Files.newBufferedWriter(out, StandardCharsets.UTF_8))) {
            csv.println("workload,variant,structure,n,time_ms,steps,moves,comparisons");
            for (int n : SIZES) {
                long checksumW1 = check(measureRow(csv, "W1", "-", "DynamicArray", n, w1(array)),
                        measureRow(csv, "W1", "-", "MyLinkedList", n, w1(linked)));
                long checksumW2 = check(measureRow(csv, "W2", "-", "DynamicArray", n, w2(array)),
                        measureRow(csv, "W2", "-", "MyLinkedList", n, w2(linked)));
                check(measureRow(csv, "W3", "head", "DynamicArray", n, w3(array, true)),
                        measureRow(csv, "W3", "head", "MyLinkedList", n, w3(linked, true)));
                check(measureRow(csv, "W3", "middle", "DynamicArray", n, w3(array, false)),
                        measureRow(csv, "W3", "middle", "MyLinkedList", n, w3(linked, false)));
                measureRow(csv, "W4", "-", "MinHeap", n, w4());
                System.out.printf(Locale.ROOT, "n=%d done (W1 checksum %d, W2 hits %d)%n", n, checksumW1, checksumW2);
            }
        }
        System.out.println("Wrote " + out.toAbsolutePath());
    }

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
