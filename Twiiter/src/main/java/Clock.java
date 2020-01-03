import java.util.Arrays;

public class Clock {

    private int clock;

    public Clock() { this.clock = 0; }

    public synchronized int increment () { return ++clock; }

    public int get () {return this.clock; }

}