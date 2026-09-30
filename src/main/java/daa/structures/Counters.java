package daa.structures;

/**
 * Operation counters shared by all data structures.
 *
 * <p>Counting rules (identical for every structure, see REPORT.md, section 3):
 * <ul>
 *   <li><b>steps</b>: one read of an array cell, or one node dereference (visit) in a list;</li>
 *   <li><b>moves</b>: one element written into a different array cell (shift, copy on resize,
 *       append, swap = 2), or one pointer/link assignment in a list;</li>
 *   <li><b>comparisons</b>: one comparison between two elements (or an element and a key).</li>
 * </ul>
 * Index validation is not counted. Counters are flushed once per operation from local
 * variables, so the counting overhead does not distort the timings.
 */
public final class Counters {
    private long steps;
    private long moves;
    private long comparisons;

    void addSteps(long n) { steps += n; }
    void addMoves(long n) { moves += n; }
    void addComparisons(long n) { comparisons += n; }

    public long steps() { return steps; }
    public long moves() { return moves; }
    public long comparisons() { return comparisons; }

    public void reset() {
        steps = 0;
        moves = 0;
        comparisons = 0;
    }

    @Override
    public String toString() {
        return "steps=" + steps + ", moves=" + moves + ", comparisons=" + comparisons;
    }
}
