import java.util.Arrays;
import java.util.stream.IntStream;

public class VectorClock {

    private int[] clock;

    public VectorClock (int vcSize) { this.clock = new int[vcSize]; }

    public synchronized int increment (int index) { return ++clock[index]; }

    public int getClock (int index) { return clock[index];}

    public int[] getVectorClock() { return clock; }

    public void print () { System.out.println(this.toString()); }

    @Override
    public String toString () { return Arrays.toString(clock); }

}