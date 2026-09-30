package daa.structures;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class MyLinkedListTest extends IntListContractTest {
    @Override
    IntList create() {
        return new MyLinkedList();
    }

    @Test
    void getCostsOneStepPerVisitedNode() {
        MyLinkedList list = new MyLinkedList();
        for (int i = 0; i < 1000; i++) {
            list.add(i);
        }
        list.counters().reset();
        list.get(0);
        assertEquals(1, list.counters().steps());
        list.counters().reset();
        list.get(999);
        assertEquals(1000, list.counters().steps());
    }

    @Test
    void headInsertAndRemoveAreConstantTime() {
        MyLinkedList list = new MyLinkedList();
        for (int i = 0; i < 10_000; i++) {
            list.add(i);
        }
        list.counters().reset();
        list.add(0, -1);
        assertEquals(0, list.counters().steps());
        assertEquals(2, list.counters().moves());
        list.counters().reset();
        assertEquals(-1, list.remove(0));
        assertEquals(1, list.counters().moves());
    }

    @Test
    void tailStaysCorrectAfterRemovingLastElement() {
        MyLinkedList list = new MyLinkedList();
        list.add(1);
        list.add(2);
        list.add(3);
        assertEquals(3, list.remove(2));
        list.add(4);
        assertEquals(3, list.size());
        assertEquals(4, list.get(2));
        assertEquals(2, list.get(1));
    }

    @Test
    void removingEverythingResetsHeadAndTail() {
        MyLinkedList list = new MyLinkedList();
        list.add(1);
        list.remove(0);
        list.add(2);
        assertEquals(1, list.size());
        assertEquals(2, list.get(0));
        assertTrue(list.contains(2));
    }
}
