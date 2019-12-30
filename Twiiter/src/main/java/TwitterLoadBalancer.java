import io.atomix.cluster.messaging.ManagedMessagingService;
import io.atomix.cluster.messaging.impl.NettyMessagingService;
import io.atomix.utils.net.Address;
import io.atomix.utils.serializer.Serializer;
import io.atomix.utils.serializer.SerializerBuilder;

import java.util.ArrayList;
import java.util.Random;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class TwitterLoadBalancer {

    private Address address;
    private List<Address> addresses;
    private ManagedMessagingService messagingService;

    public TwitterLoadBalancer(Address address, List<Address> addresses) {

        this.address = address;
        this.addresses = addresses;

        ExecutorService e = Executors.newFixedThreadPool(1);

        // Starting messaging service

        this.messagingService = new NettyMessagingService.Builder()
                .withName("Twitter_Load_Balancer")
                .withReturnAddress(address).build();

        // Creating serializer for encoding and decoding to/from bytes
        Serializer s = new SerializerBuilder().addType(Address.class).build();

        // Starting randomizer to get a random address from the bunch
        Random r = new Random();

        // Register Handler for when a message is received
        messagingService.registerHandler("GET_ADDR", (addr, bytes) -> {

            int index = r.nextInt(this.addresses.size());

            // Serializing the Address
            byte[] data = s.encode(this.addresses.get(index));

            messagingService.sendAsync(addr, "ADDRESS", data);

        }, e);

    }

    public void start() {

        this.messagingService.start();
    }

    public void stop() {

        this.messagingService.stop();
    }

    public static void main(String[] args) throws Exception {

        // Getting port from command line
        int port = Integer.parseInt(args[0]);

        // Making an Address based on the command line arguments
        Address address = Address.from(port);

        // Getting the number of servers from the command line arguments
        int no_addresses = Integer.parseInt((args[1]));

        // Filling a list with the addresses of those servers
        List<Address> addresses = new ArrayList<>();
        for (int i=1; i<=no_addresses; i++) addresses.add(Address.from(1000 + i));

        new TwitterLoadBalancer(address, addresses).start();

    }
}
