import io.atomix.cluster.messaging.ManagedMessagingService;
import io.atomix.cluster.messaging.MessagingService;
import io.atomix.cluster.messaging.impl.NettyMessagingService;
import io.atomix.utils.net.Address;
import io.atomix.utils.serializer.Serializer;
import io.atomix.utils.serializer.SerializerBuilder;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.IntStream;

public class TwitterServer {

    private Address address;
    private List<Address> addresses;
    private MessagingService messagingService;
    private VectorClock vectorClock;

    public TwitterServer(Address address, List<Address> addresses) throws Exception {

        this.address = address;
        this.addresses = addresses;
        this.vectorClock = new VectorClock(addresses.indexOf(address), addresses.size());

        ExecutorService e = Executors.newFixedThreadPool(1);

        // Starting messaging service

        ManagedMessagingService messagingService = new NettyMessagingService.Builder()
                .withName("Chat_" + address.toString())
                .withReturnAddress(address).build();

        messagingService.start();

        // Creating serializer for encoding and decoding to/from bytes
        Serializer s = new SerializerBuilder().addType(Message.class).addType(VectorClock.class).build();

        // Register Handler for when a message is received
        messagingService.registerHandler("MSG", (addr,bytes)-> {

            // Decoding data received
            Message msg = s.decode(bytes);

            // Printing it
            System.out.println(msg.vectorClock.toString() + " -> " + msg.message);

            // Updating inner vector clock
            this.vectorClock.update(msg.vectorClock);

        }, e);


        // Sending a message when input is received
        BufferedReader in = new BufferedReader(new InputStreamReader(System.in));
        String msg;

        while (!(msg = in.readLine()).toUpperCase().equals("EXIT")) {

            // Incrementing Vector Clock on send
            vectorClock.increment();

            // Creating the message and serializing it
            byte[] data = s.encode(new Message(msg, vectorClock));

            // Sending the message to every client but itself
            for (Address addr : addresses)
                if (!addr.equals(address))
                    messagingService.sendAsync(addr, "MSG", data);

        }


    }

    private static class Message {

        public String message;
        public VectorClock vectorClock;

        public Message (String message, VectorClock vectorClock) {

            this.message = message;
            this.vectorClock = vectorClock;

        }

    }

    public static class VectorClock {

        private int id;
        private int[] clock;

        public VectorClock (int id, int vcSize) {

            this.id = id;
            this.clock = new int[vcSize];

        }

        public void increment () { clock[id]++; }

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

        public void print () { System.out.println(this.toString()); }

        @Override
        public String toString () { return id + " : " + Arrays.toString(clock); }

    }

    public static void vectorClockTest () {

        // Start vector clocks
        VectorClock v0 = new VectorClock(0, 3);
        VectorClock v1 = new VectorClock(1, 3);
        VectorClock v2 = new VectorClock(2, 3);

        // Send v0
        v0.increment(); v1.update(v0); v2.update(v0);

        // Send v1
        v1.increment(); v0.update(v1); v2.update(v1);

        // Send v2
        v2.increment(); v0.update(v2); v1.update(v2);

        // Interwine
        v1.increment();
        v0.increment();
        v0.update(v1);
        v1.update(v0);
        v2.update(v0);
        v2.update(v1);


        // Print final states
        v0.print(); v1.print(); v2.print();

    }

    public static void main(String[] args) throws Exception {

        // Getting port from command line
        int port = Integer.parseInt(args[0]);

        // Making an Address based on the command line arguments
        Address address = Address.from(port);
        List<Address> addresses = new ArrayList<>();

        for (int i=10001; i<=10005; i++) addresses.add(Address.from(i));

        new TwitterServer(address, addresses);

        //vectorClockTest();
    }
}
