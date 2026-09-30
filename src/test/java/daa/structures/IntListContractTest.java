package daa.structures;

import static org.junit.jupiter.api.Assertions.*;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;
import org.junit.jupiter.api.Test;

abstract class IntListContractTest {
    abstract IntList create();

    @Test
    void emptyStructure() {
        IntList list = create();
        assertEquals(0, list.size());
        assertFalse(list.contains(0));
        assertThrows(IndexOutOfBoundsException.class, () -> list.get(0));
        assertThrows(IndexOutOfBoundsException.class, () -> list.remove(0));
        assertThrows(IndexOutOfBoundsException.class, () -> list.add(1, 5));
        assertThrows(IndexOutOfBoundsException.class, () -> list.add(-1, 5));
    }

    @Test
    void singleElement() {
        IntList list = create();
        list.add(7);
        assertEquals(1, list.size());
        assertEquals(7, list.get(0));
        assertTrue(list.contains(7));
        assertEquals(7, list.remove(0));
        assertEquals(0, list.size());
        list.add(0, 9);
        list.add(10);
        assertEquals(9, list.get(0));
        assertEquals(10, list.get(1));
    }

    @Test
    void firstAndLastIndex() {
        IntList list = create();
        for (int i = 0; i < 20; i++) {
            list.add(i);
        }
        assertEquals(0, list.get(0));
        assertEquals(19, list.get(19));
        assertEquals(19, list.remove(19));
        assertEquals(0, list.remove(0));
        assertEquals(18, list.size());
        assertEquals(1, list.get(0));
        assertEquals(18, list.get(17));
        list.add(list.size(), 99);
        assertEquals(99, list.get(list.size() - 1));
        list.add(0, -1);
        assertEquals(-1, list.get(0));
    }

    @Test
    void invalidIndicesThrow() {
        IntList list = create();
        for (int i = 0; i < 5; i++) {
            list.add(i);
        }
        assertThrows(IndexOutOfBoundsException.class, () -> list.get(-1));
        assertThrows(IndexOutOfBoundsException.class, () -> list.get(5));
        assertThrows(IndexOutOfBoundsException.class, () -> list.remove(-1));
        assertThrows(IndexOutOfBoundsException.class, () -> list.remove(5));
        assertThrows(IndexOutOfBoundsException.class, () -> list.add(6, 1));
        assertEquals(5, list.size());
    }

    @Test
    void duplicates() {
        IntList list = create();
        for (int i = 0; i < 10; i++) {
            list.add(4);
        }
        list.add(5, 4);
        assertEquals(11, list.size());
        assertTrue(list.contains(4));
        assertEquals(4, list.remove(3));
        assertEquals(10, list.size());
        assertFalse(list.contains(5));
    }

    @Test
    void containsFindsFirstMiddleLastAndMissing() {
        IntList list = create();
        for (int i = 0; i < 100; i++) {
            list.add(i * 2);
        }
        assertTrue(list.contains(0));
        assertTrue(list.contains(100));
        assertTrue(list.contains(198));
        assertFalse(list.contains(1));
        assertFalse(list.contains(200));
    }

    @Test
    void growsPastInitialCapacity() {
        IntList list = create();
        for (int i = 0; i < 10_000; i++) {
            list.add(i);
        }
        assertEquals(10_000, list.size());
        for (int i = 0; i < 10_000; i += 997) {
            assertEquals(i, list.get(i));
        }
    }

    @Test
    void randomOperationsMatchReferenceModel() {
        Random rnd = new Random(42);
        IntList list = create();
        List<Integer> model = new ArrayList<>();
        for (int step = 0; step < 20_000; step++) {
            int op = rnd.nextInt(5);
            int value = rnd.nextInt(50);
            if (op == 0 || model.isEmpty()) {
                list.add(value);
                model.add(value);
            } else if (op == 1) {
                int idx = rnd.nextInt(model.size() + 1);
                list.add(idx, value);
                model.add(idx, value);
            } else if (op == 2) {
                int idx = rnd.nextInt(model.size());
                assertEquals(model.remove(idx).intValue(), list.remove(idx));
            } else if (op == 3) {
                int idx = rnd.nextInt(model.size());
                assertEquals(model.get(idx).intValue(), list.get(idx));
            } else {
                assertEquals(model.contains(value), list.contains(value));
            }
            assertEquals(model.size(), list.size());
        }
        for (int i = 0; i < model.size(); i++) {
            assertEquals(model.get(i).intValue(), list.get(i));
        }
    }

    @Test
    void countersStartAtZeroAndReset() {
        IntList list = create();
        assertEquals(0, list.counters().steps());
        list.add(1);
        assertTrue(list.counters().moves() > 0);
        list.counters().reset();
        assertEquals(0, list.counters().steps());
        assertEquals(0, list.counters().moves());
        assertEquals(0, list.counters().comparisons());
    }

    @Test
    void containsCountsOneComparisonPerVisitedElement() {
        IntList list = create();
        for (int i = 0; i < 50; i++) {
            list.add(i);
        }
        list.counters().reset();
        list.contains(9);
        assertEquals(10, list.counters().comparisons());
        assertEquals(10, list.counters().steps());
        list.counters().reset();
        list.contains(-5);
        assertEquals(50, list.counters().comparisons());
    }
}
