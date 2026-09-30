package daa.benchmark;

import daa.structures.MinHeap;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Locale;
import java.util.Random;

/**
 * Bonus B: Floyd's bottom-up {@code buildHeap} (O(n)) versus n separate {@code insert} calls
 * (O(n log n) worst case). Two inputs: random (average case) and strictly descending
 * (worst case for insert: every new key bubbles up to the root).
 *
 * <p>Output: results/buildheap.csv.
 */
public final class HeapBuildExperiment {
    private static final int[] SIZES = {100, 1_000, 10_000, 100_000, 1_000_000};

    public static void main(String[] args) throws IOException {
        Path out = Paths.get(args.length > 0 ? args[0] : "results/buildheap.csv");
        if (out.getParent() != null) {
            Files.createDirectories(out.getParent());
        }
        try (PrintWriter csv = new PrintWriter(Files.newBufferedWriter(out, StandardCharsets.UTF_8))) {
            csv.println("input,method,n,time_ms,steps,moves,comparisons,comparisons_per_n");
            for (int n : SIZES) {
                for (String input : new String[] {"random", "descending"}) {
                    int[] data = generate(input, n);
                    row(csv, input, "n_inserts", n, data);
                    row(csv, input, "buildHeap", n, data);
                }
            }
        }
        System.out.println("Wrote " + out.toAbsolutePath());
    }

    private static int[] generate(String input, int n) {
        Random rnd = new Random(Benchmark.SEED);
        int[] data = new int[n];
        for (int i = 0; i < n; i++) {
            data[i] = input.equals("random") ? rnd.nextInt() : n - i;
        }
        return data;
    }

    private static MinHeap run(String method, int[] data) {
        if (method.equals("buildHeap")) {
            return MinHeap.buildHeap(data);
        }
        MinHeap heap = new MinHeap();
        for (int v : data) {
            heap.insert(v);
        }
        return heap;
    }

    private static void row(PrintWriter csv, String input, String method, int n, int[] data) {
        long start = System.nanoTime();
        for (int i = 0; i < Benchmark.WARMUP || System.nanoTime() - start < Benchmark.WARMUP_MIN_NS; i++) {
            run(method, data);
        }
        long[] times = new long[Benchmark.RUNS];
        MinHeap last = null;
        for (int i = 0; i < Benchmark.RUNS; i++) {
            long t0 = System.nanoTime();
            last = run(method, data);
            times[i] = System.nanoTime() - t0;
        }
        if (!last.isHeap() || last.size() != n) {
            throw new IllegalStateException("invalid heap");
        }
        long c = last.counters().comparisons();
        csv.printf(Locale.ROOT, "%s,%s,%d,%.6f,%d,%d,%d,%.4f%n", input, method, n,
                Benchmark.median(times) / 1e6, last.counters().steps(), last.counters().moves(),
                c, (double) c / n);
        csv.flush();
    }
}
