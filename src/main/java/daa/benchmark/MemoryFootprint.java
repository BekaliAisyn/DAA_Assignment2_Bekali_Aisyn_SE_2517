package daa.benchmark;

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
import org.openjdk.jol.info.ClassLayout;
import org.openjdk.jol.info.GraphLayout;
import org.openjdk.jol.vm.VM;

/**
 * Bonus A: measures the real heap footprint of each structure with JOL
 * (Java Object Layout, org.openjdk.jol:jol-core).
 *
 * <p>Output: results/memory.csv and results/jol_layouts.txt (object headers, padding, alignment).
 */
public final class MemoryFootprint {
    private static final int[] SIZES = {100, 1_000, 10_000, 100_000};

    public static void main(String[] args) throws Exception {
        Path csvPath = Paths.get("results/memory.csv");
        Path layoutPath = Paths.get("results/jol_layouts.txt");
        Files.createDirectories(csvPath.getParent());

        try (PrintWriter csv = new PrintWriter(Files.newBufferedWriter(csvPath, StandardCharsets.UTF_8));
             PrintWriter txt = new PrintWriter(Files.newBufferedWriter(layoutPath, StandardCharsets.UTF_8))) {
            csv.println("structure,n,total_bytes,bytes_per_element,payload_bytes,overhead_factor");

            txt.println(VM.current().details());
            txt.println(ClassLayout.parseClass(DynamicArray.class).toPrintable());
            txt.println(ClassLayout.parseClass(MyLinkedList.class).toPrintable());
            txt.println(ClassLayout.parseClass(Class.forName("daa.structures.MyLinkedList$Node")).toPrintable());
            txt.println(ClassLayout.parseClass(MinHeap.class).toPrintable());
            txt.println(ClassLayout.parseInstance(new int[10]).toPrintable());

            for (int n : SIZES) {
                Random rnd = new Random(Benchmark.SEED);
                DynamicArray array = new DynamicArray();
                MyLinkedList linked = new MyLinkedList();
                MinHeap heap = new MinHeap();
                for (int i = 0; i < n; i++) {
                    int v = rnd.nextInt();
                    array.add(v);
                    linked.add(v);
                    heap.insert(v);
                }
                row(csv, "DynamicArray", n, GraphLayout.parseInstance(array).totalSize());
                row(csv, "MyLinkedList", n, GraphLayout.parseInstance(linked).totalSize());
                row(csv, "MinHeap", n, GraphLayout.parseInstance(heap).totalSize());
                if (n == 1_000) {
                    txt.println("GraphLayout summary, MyLinkedList n=1000:");
                    txt.println(GraphLayout.parseInstance(linked).toFootprint());
                }
            }
        }
        System.out.println("Wrote " + csvPath.toAbsolutePath() + " and " + layoutPath.toAbsolutePath());
    }

    private static void row(PrintWriter csv, String name, int n, long total) {
        long payload = 4L * n; // n ints, nothing else
        csv.printf(Locale.ROOT, "%s,%d,%d,%.2f,%d,%.2f%n", name, n, total, (double) total / n,
                payload, (double) total / payload);
    }
}
