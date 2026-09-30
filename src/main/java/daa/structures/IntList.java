package daa.structures;

/**
 * Common interface of {@link DynamicArray} and {@link MyLinkedList}, so that the same
 * benchmark code runs on both. Elements are primitive {@code int}s (no boxing).
 */
public interface IntList {

    /** Appends {@code x} at the end. */
    void add(int x);

    /**
     * Inserts {@code x} so that it ends up at position {@code index}.
     *
     * @throws IndexOutOfBoundsException if {@code index < 0 || index > size()}
     */
    void add(int index, int x);

    /**
     * Removes and returns the element at {@code index}.
     *
     * @throws IndexOutOfBoundsException if {@code index < 0 || index >= size()}
     */
    int remove(int index);

    /**
     * Returns the element at {@code index}.
     *
     * @throws IndexOutOfBoundsException if {@code index < 0 || index >= size()}
     */
    int get(int index);

    /** Linear search for {@code x}. */
    boolean contains(int x);

    int size();

    /** Counters of steps / moves / comparisons performed since the last reset. */
    Counters counters();
}
