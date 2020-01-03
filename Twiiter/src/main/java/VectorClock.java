import java.util.Arrays;
import java.util.HashMap;

public class VectorClock<T> {

    private HashMap<T, Clock> clock = new HashMap<>();

    public synchronized int increment (T key) {

        if (!this.clock.containsKey(key)) this.clock.put(key, new Clock());

        return this.clock.get(key).increment();

    }

    public int getClock (T key) { return this.clock.getOrDefault(key, new Clock()).get();}

}