package daa.structures;

import daa.metrics.Counters;

public final class DynamicArray implements IntList {
    private static final int DEFAULT_CAPACITY = 8;
    private static final int MAX_CAPACITY = Integer.MAX_VALUE - 8;

    private int[] data;
    private int size;
    private final Counters counters = new Counters();

    public DynamicArray() {
        this(DEFAULT_CAPACITY);
    }

    public DynamicArray(int initialCapacity) {
        if (initialCapacity < 1) {
            throw new IllegalArgumentException("capacity must be >= 1: " + initialCapacity);
        }
        data = new int[initialCapacity];
    }

    @Override
    public void add(int x) {
        growIfFull();
        data[size++] = x;
        counters.addMoves(1);
    }

    @Override
    public void add(int index, int x) {
        if (index < 0 || index > size) {
            throw new IndexOutOfBoundsException("index " + index + ", size " + size);
        }
        growIfFull();
        int shifted = 0;
        for (int i = size; i > index; i--) {
            data[i] = data[i - 1];
            shifted++;
        }
        data[index] = x;
        size++;
        counters.addSteps(shifted);
        counters.addMoves(shifted + 1);
    }

    @Override
    public int remove(int index) {
        checkIndex(index);
        int removed = data[index];
        int shifted = 0;
        for (int i = index; i < size - 1; i++) {
            data[i] = data[i + 1];
            shifted++;
        }
        size--;
        counters.addSteps(1L + shifted);
        counters.addMoves(shifted);
        return removed;
    }

    @Override
    public int get(int index) {
        checkIndex(index);
        counters.addSteps(1);
        return data[index];
    }

    @Override
    public boolean contains(int x) {
        int visited = 0;
        boolean found = false;
        for (int i = 0; i < size; i++) {
            visited++;
            if (data[i] == x) {
                found = true;
                break;
            }
        }
        counters.addSteps(visited);
        counters.addComparisons(visited);
        return found;
    }

    @Override
    public int size() {
        return size;
    }

    public int capacity() {
        return data.length;
    }

    @Override
    public Counters counters() {
        return counters;
    }

    private void checkIndex(int index) {
        if (index < 0 || index >= size) {
            throw new IndexOutOfBoundsException("index " + index + ", size " + size);
        }
    }

    private void growIfFull() {
        if (size < data.length) {
            return;
        }
        if (data.length >= MAX_CAPACITY) {
            throw new IllegalStateException("array too large");
        }
        int newCapacity = data.length > MAX_CAPACITY / 2 ? MAX_CAPACITY : data.length * 2;
        int[] bigger = new int[newCapacity];
        for (int i = 0; i < size; i++) {
            bigger[i] = data[i];
        }
        counters.addSteps(size);
        counters.addMoves(size);
        data = bigger;
    }
}
