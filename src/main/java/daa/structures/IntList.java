package daa.structures;

import daa.metrics.Counters;

public interface IntList {
    void add(int x);

    void add(int index, int x);

    int remove(int index);

    int get(int index);

    boolean contains(int x);

    int size();

    Counters counters();
}
