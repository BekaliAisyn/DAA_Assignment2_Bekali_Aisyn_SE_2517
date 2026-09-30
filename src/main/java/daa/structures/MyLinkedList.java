package daa.structures;

import daa.metrics.Counters;

public final class MyLinkedList implements IntList {
    private static final class Node {
        int value;
        Node next;

        Node(int value) {
            this.value = value;
        }
    }

    private Node head;
    private Node tail;
    private int size;
    private final Counters counters = new Counters();

    @Override
    public void add(int x) {
        Node node = new Node(x);
        if (head == null) {
            head = node;
        } else {
            tail.next = node;
        }
        tail = node;
        size++;
        counters.addMoves(2);
    }

    @Override
    public void add(int index, int x) {
        if (index < 0 || index > size) {
            throw new IndexOutOfBoundsException("index " + index + ", size " + size);
        }
        if (index == size) {
            add(x);
            return;
        }
        Node node = new Node(x);
        if (index == 0) {
            node.next = head;
            head = node;
            size++;
            counters.addMoves(2);
            return;
        }
        Node pred = head;
        for (int i = 0; i < index - 1; i++) {
            pred = pred.next;
        }
        node.next = pred.next;
        pred.next = node;
        size++;
        counters.addSteps(index);
        counters.addMoves(2);
    }

    @Override
    public int remove(int index) {
        checkIndex(index);
        int removed;
        if (index == 0) {
            removed = head.value;
            head = head.next;
            long moves = 1;
            if (head == null) {
                tail = null;
                moves++;
            }
            size--;
            counters.addSteps(1);
            counters.addMoves(moves);
            return removed;
        }
        Node pred = head;
        for (int i = 0; i < index - 1; i++) {
            pred = pred.next;
        }
        Node target = pred.next;
        removed = target.value;
        pred.next = target.next;
        long moves = 1;
        if (target == tail) {
            tail = pred;
            moves++;
        }
        size--;
        counters.addSteps(index + 1L);
        counters.addMoves(moves);
        return removed;
    }

    @Override
    public int get(int index) {
        checkIndex(index);
        Node cur = head;
        for (int i = 0; i < index; i++) {
            cur = cur.next;
        }
        counters.addSteps(index + 1L);
        return cur.value;
    }

    @Override
    public boolean contains(int x) {
        long visited = 0;
        boolean found = false;
        for (Node cur = head; cur != null; cur = cur.next) {
            visited++;
            if (cur.value == x) {
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

    @Override
    public Counters counters() {
        return counters;
    }

    private void checkIndex(int index) {
        if (index < 0 || index >= size) {
            throw new IndexOutOfBoundsException("index " + index + ", size " + size);
        }
    }
}
