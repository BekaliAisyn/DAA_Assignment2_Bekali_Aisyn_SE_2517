package daa.structures;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class DynamicArrayTest extends IntListContractTest {

    @Override
    IntList create() {
        return new DynamicArray();
    }

    @Test
    void capacityDoublesWhenFull() {
        DynamicArray array = new DynamicArray(4);
        for (int i = 0; i < 4; i++) {
            array.add(i);
        }
        assertEquals(4, array.capacity());
        array.add(4);
        assertEquals(8, array.capacity());
        for (int i = 5; i < 9; i++) {
            array.add(i);
        }
        assertEquals(16, array.capacity());
    }

    @Test
    void rejectsInvalidCapacity() {
        assertThrows(IllegalArgumentException.class, () -> new DynamicArray(0));
    }

    @Test
    void getCostsExactlyOneStep() {
        DynamicArray array = new DynamicArray();
        for (int i = 0; i < 1000; i++) {
            array.add(i);
        }
        array.counters().reset();
        array.get(0);
        array.get(999);
        array.get(500);
        assertEquals(3, array.counters().steps());
        assertEquals(0, array.counters().moves());
    }

    @Test
    void insertAtHeadShiftsEveryElement() {
        DynamicArray array = new DynamicArray(2000);
        for (int i = 0; i < 1000; i++) {
            array.add(i);
        }
        array.counters().reset();
        array.add(0, -1);
        assertEquals(1000, array.counters().steps());  // 1000 elements read
        assertEquals(1001, array.counters().moves());  // 1000 shifts + the new element
    }

    @Test
    void removeAtEndShiftsNothing() {
        DynamicArray array = new DynamicArray();
        for (int i = 0; i < 100; i++) {
            array.add(i);
        }
        array.counters().reset();
        array.remove(99);
        assertEquals(0, array.counters().moves());
    }

    @Test
    void resizeCopiesAllElements() {
        DynamicArray array = new DynamicArray(4);
        for (int i = 0; i < 4; i++) {
            array.add(i);
        }
        array.counters().reset();
        array.add(4); // triggers a resize: 4 copies + 1 write
        assertEquals(5, array.counters().moves());
    }
}
