package daa.structures;

import daa.metrics.Counters;

public final class MinHeap {
    private static final int DEFAULT_CAPACITY = 8;

    private int[] a;
    private int size;
    private final Counters counters = new Counters();

    public MinHeap() {
        this(DEFAULT_CAPACITY);
    }

    public MinHeap(int initialCapacity) {
        if (initialCapacity < 1) {
            throw new IllegalArgumentException("capacity must be >= 1: " + initialCapacity);
        }
        a = new int[initialCapacity];
    }

    public static MinHeap buildHeap(int[] values) {
        MinHeap heap = new MinHeap(Math.max(values.length, 1));
        for (int i = 0; i < values.length; i++) {
            heap.a[i] = values[i];
        }
        heap.size = values.length;
        heap.counters.addSteps(values.length);
        heap.counters.addMoves(values.length);
        for (int i = values.length / 2 - 1; i >= 0; i--) {
            heap.siftDown(i);
        }
        return heap;
    }

    public void insert(int x) {
        if (size == a.length) {
            grow();
        }
        a[size] = x;
        counters.addMoves(1);
        siftUp(size);
        size++;
    }

    public int peekMin() {
        if (size == 0) {
            throw new IllegalStateException("heap is empty");
        }
        counters.addSteps(1);
        return a[0];
    }

    public int extractMin() {
        if (size == 0) {
            throw new IllegalStateException("heap is empty");
        }
        int min = a[0];
        counters.addSteps(1);
        size--;
        if (size > 0) {
            a[0] = a[size];
            counters.addSteps(1);
            counters.addMoves(1);
            siftDown(0);
        }
        return min;
    }

    public int size() {
        return size;
    }

    public boolean isEmpty() {
        return size == 0;
    }

    public Counters counters() {
        return counters;
    }

    public boolean isHeap() {
        for (int child = 1; child < size; child++) {
            if (a[(child - 1) / 2] > a[child]) {
                return false;
            }
        }
        return true;
    }

    private void siftUp(int i) {
        long comparisons = 0;
        long swaps = 0;
        while (i > 0) {
            int parent = (i - 1) / 2;
            comparisons++;
            if (a[i] >= a[parent]) {
                break;
            }
            swap(i, parent);
            swaps++;
            i = parent;
        }
        counters.addComparisons(comparisons);
        counters.addSteps(2 * comparisons);
        counters.addMoves(2 * swaps);
    }

    private void siftDown(int i) {
        long comparisons = 0;
        long swaps = 0;
        while (true) {
            int left = 2 * i + 1;
            if (left >= size) {
                break;
            }
            int smallest = left;
            int right = left + 1;
            if (right < size) {
                comparisons++;
                if (a[right] < a[left]) {
                    smallest = right;
                }
            }
            comparisons++;
            if (a[i] <= a[smallest]) {
                break;
            }
            swap(i, smallest);
            swaps++;
            i = smallest;
        }
        counters.addComparisons(comparisons);
        counters.addSteps(2 * comparisons);
        counters.addMoves(2 * swaps);
    }

    private void swap(int i, int j) {
        int t = a[i];
        a[i] = a[j];
        a[j] = t;
    }

    private void grow() {
        int newCapacity = a.length > Integer.MAX_VALUE / 4 ? Integer.MAX_VALUE - 8 : a.length * 2;
        int[] bigger = new int[newCapacity];
        for (int i = 0; i < size; i++) {
            bigger[i] = a[i];
        }
        counters.addSteps(size);
        counters.addMoves(size);
        a = bigger;
    }
}
