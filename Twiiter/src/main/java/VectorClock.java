import java.util.Arrays;
import java.util.stream.IntStream;

public class VectorClock {

    private int id;
    private int[] clock;

    public VectorClock (int id, int vcSize) {

        this.id = id;
        this.clock = new int[vcSize];

    }

    public void increment () { ++clock[id]; }

    public void update (VectorClock vectorClock) {

        if (IntStream.range(0, clock.length)
                .filter(i -> i != id)
                .allMatch(i -> this.clock[i] <= vectorClock.clock[i])) {

            // Updating the local vector clock
            int id_value = this.clock[id];
            this.clock = vectorClock.clock;
            this.clock[id] = id_value;

        }

    }

    public int getId() { return id; }

    public int getSelf() { return clock[id]; }

    public int getClock (int index) { return clock[index];}

    public int[] getVectorClock() { return clock; }

    public void print () { System.out.println(this.toString()); }

    @Override
    public String toString () { return id + " : " + Arrays.toString(clock); }

}