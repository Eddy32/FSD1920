import java.util.Arrays;
import java.util.stream.IntStream;

public class VectorClock {

    private final int id;
    private int[] clock;

    public VectorClock (int id, int vcSize) {

        this.id = id;
        this.clock = new int[vcSize];

    }

    public synchronized int increment () { return ++clock[id]; }

    public int getId() { return id; }

    public int getClock (int index) { return clock[index];}

    public int[] getVectorClock() { return clock; }

    public void print () { System.out.println(this.toString()); }

    @Override
    public String toString () { return id + " : " + Arrays.toString(clock); }

}