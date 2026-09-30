package daa.structures;

import static org.junit.jupiter.api.Assertions.*;

import java.util.PriorityQueue;
import java.util.Random;
import org.junit.jupiter.api.Test;

class MinHeapTest {
    @Test
    void emptyHeapThrowsIllegalState() {
        MinHeap heap = new MinHeap();
        assertTrue(heap.isEmpty());
        assertThrows(IllegalStateException.class, heap::peekMin);
        assertThrows(IllegalStateException.class, heap::extractMin);
    }

    @Test
    void singleElement() {
        MinHeap heap = new MinHeap();
        heap.insert(5);
        assertEquals(5, heap.peekMin());
        assertEquals(5, heap.extractMin());
        assertTrue(heap.isEmpty());
        assertThrows(IllegalStateException.class, heap::extractMin);
    }

    @Test
    void peekDoesNotRemove() {
        MinHeap heap = new MinHeap();
        heap.insert(3);
        heap.insert(1);
        heap.insert(2);
        assertEquals(1, heap.peekMin());
        assertEquals(1, heap.peekMin());
        assertEquals(3, heap.size());
    }

    @Test
    void duplicatesAreKept() {
        MinHeap heap = new MinHeap();
        for (int i = 0; i < 20; i++) {
            heap.insert(7);
        }
        for (int i = 0; i < 20; i++) {
            assertEquals(7, heap.extractMin());
        }
        assertTrue(heap.isEmpty());
    }

    @Test
    void heapPropertyHoldsAfterEveryInsertAndExtract() {
        Random rnd = new Random(42);
        MinHeap heap = new MinHeap();
        for (int i = 0; i < 2_000; i++) {
            heap.insert(rnd.nextInt(500));
            assertTrue(heap.isHeap(), "after insert #" + i);
        }
        for (int i = 0; i < 2_000; i++) {
            heap.extractMin();
            assertTrue(heap.isHeap(), "after extract #" + i);
        }
    }

    @Test
    void extractMinReturnsNonDecreasingOrder() {
        Random rnd = new Random(42);
        MinHeap heap = new MinHeap();
        for (int i = 0; i < 10_000; i++) {
            heap.insert(rnd.nextInt());
        }
        int previous = Integer.MIN_VALUE;
        while (!heap.isEmpty()) {
            int current = heap.extractMin();
            assertTrue(previous <= current);
            previous = current;
        }
    }

    @Test
    void sortedAndReverseSortedInput() {
        MinHeap ascending = new MinHeap();
        MinHeap descending = new MinHeap();
        for (int i = 0; i < 1000; i++) {
            ascending.insert(i);
            descending.insert(1000 - i);
        }
        assertEquals(0, ascending.peekMin());
        assertEquals(1, descending.peekMin());
        assertTrue(ascending.isHeap());
        assertTrue(descending.isHeap());
    }

    @Test
    void interleavedOperationsMatchPriorityQueue() {
        Random rnd = new Random(42);
        MinHeap heap = new MinHeap();
        PriorityQueue<Integer> model = new PriorityQueue<>();
        for (int i = 0; i < 30_000; i++) {
            if (model.isEmpty() || rnd.nextInt(3) != 0) {
                int v = rnd.nextInt(1000) - 500;
                heap.insert(v);
                model.add(v);
            } else {
                assertEquals(model.poll().intValue(), heap.extractMin());
            }
            assertEquals(model.size(), heap.size());
            if (!model.isEmpty()) {
                assertEquals(model.peek().intValue(), heap.peekMin());
            }
        }
    }

    @Test
    void buildHeapProducesValidHeapAndSortedExtraction() {
        Random rnd = new Random(42);
        for (int n : new int[] {0, 1, 2, 3, 10, 1000, 4097}) {
            int[] values = new int[n];
            for (int i = 0; i < n; i++) {
                values[i] = rnd.nextInt(100);
            }
            MinHeap heap = MinHeap.buildHeap(values);
            assertEquals(n, heap.size());
            assertTrue(heap.isHeap());
            int previous = Integer.MIN_VALUE;
            for (int i = 0; i < n; i++) {
                int current = heap.extractMin();
                assertTrue(previous <= current);
                previous = current;
            }
        }
    }

    @Test
    void buildHeapUsesAtMostTwoComparisonsPerElement() {
        Random rnd = new Random(42);
        int n = 100_000;
        int[] values = new int[n];
        for (int i = 0; i < n; i++) {
            values[i] = rnd.nextInt();
        }
        MinHeap heap = MinHeap.buildHeap(values);
        assertTrue(heap.counters().comparisons() <= 2L * n);
    }

    @Test
    void peekMinCostsOneStep() {
        MinHeap heap = new MinHeap();
        for (int i = 0; i < 100; i++) {
            heap.insert(i);
        }
        heap.counters().reset();
        heap.peekMin();
        assertEquals(1, heap.counters().steps());
        assertEquals(0, heap.counters().comparisons());
    }

    @Test
    void insertOfNewMaximumNeedsOneComparison() {
        MinHeap heap = new MinHeap();
        for (int i = 0; i < 100; i++) {
            heap.insert(i);
        }
        heap.counters().reset();
        heap.insert(1000);
        assertEquals(1, heap.counters().comparisons());
        assertEquals(1, heap.counters().moves());
    }
}
