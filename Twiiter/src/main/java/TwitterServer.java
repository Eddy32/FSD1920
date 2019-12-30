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
import java.util.TreeMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.IntStream;

public class TwitterServer {

    private int id;
    private int leader;
    private List<Address> addresses;
    private ManagedMessagingService messagingService;
    private VectorClock vectorClock;
    private ArrayList<TreeMap<Integer, Protos.TryUpdate>> serverQueue;

    public TwitterServer(int id, List<Address> addresses) throws Exception {

        this.id = id;
        this.leader = 0;
        this.addresses = addresses;
        this.vectorClock = new VectorClock(id, addresses.size());

        this.serverQueue = new ArrayList<>();
        for (int i=0; i<addresses.size(); i++) serverQueue.add(new TreeMap<>());

        ExecutorService e = Executors.newFixedThreadPool(6);

        // Starting messaging service
        messagingService = new NettyMessagingService.Builder()
                .withName("Twitter_Server_" + id)
                .withReturnAddress(addresses.get(id)).build();

        // Serializers
        Serializer post_serializer = new SerializerBuilder().addType(Protos.Post.class).build();
        Serializer try_update_serializer = new SerializerBuilder().addType(Protos.TryUpdate.class).build();
        Serializer update_serializer = new SerializerBuilder().addType(Protos.Update.class).build();
        Serializer get_serializer = new SerializerBuilder().addType(Protos.Get.class).build();
        Serializer list_serializer = new SerializerBuilder().addType(Protos.List.class).build();

        // When a POST message is received
        messagingService.registerHandler("POST", (addr,bytes)-> {

            // Decoding post info
            Protos.Post post = post_serializer.decode(bytes);

            String post_text = post.getText();
            ArrayList<String> post_topics = post.getCategories();

            // Sending try updates by topic
            for (String topic: post_topics) {

                Protos.TryUpdate try_update = new Protos.TryUpdate(topic, post_text, vectorClock.getId(), vectorClock.getSelf());

                byte[] data = try_update_serializer.encode(try_update);

                messagingService.sendAsync(addresses.get(leader), "TRY_UPDATE", data);

            }

        }, e);

        // When a TRY UPDATE message is received
        messagingService.registerHandler("TRY_UPDATE", (addr,bytes)-> {

            // Decoding data received
            Protos.TryUpdate try_update = try_update_serializer.decode(bytes);

            String post_text = try_update.getText();
            String post_topic = try_update.getCategory();
            int serverId = try_update.getServerId();
            int serverClock = try_update.getServerClock();

            if (serverClock == vectorClock.getClock(serverId) + 1) {

                for (int i = serverClock; !serverQueue.get(serverId).containsKey(i); i++) {

                    // IR BUSCAR INDICE A BASE DE DADOS

                    Protos.Update broadcast = new Protos.Update(post_text, post_topic, , );

                    byte[] data = update_serializer.encode(broadcast);

                    messagingService.sendAsync(addresses.get(serverId), "BROADCAST", data);

                }

            } else {

                // Adding to queue
                serverQueue.get(serverId).put(serverId, try_update);

            }

            Protos.Update broadcast = new Protos.Update(post_text, post_topic, )

        }, e);

        // When a BROADCAST message is received
        messagingService.registerHandler("BROADCAST", (addr,bytes)-> {

            for (Address a : addresses) messagingService.sendAsync(a, "TRY_UPDATE", bytes);

        }, e);

        // When an UPDATE message is received
        messagingService.registerHandler("UPDATE", (addr,bytes)-> {

            // Decoding data received
            Protos.Update update = update_serializer.decode(bytes);

            String post_text = update.getText();
            String post_topic = update.getCategory();
            int topicIndex = update.getIndex();
            int topicCounter = update.getIterator();

            // ATUALIZAR BASE DE DADOS SE TOPIC COUNTER FOR INFERIOR

        }, e);

        // When a GET message is received
        messagingService.registerHandler("GET", (addr,bytes)-> {

            // Decoding data received
            Protos.Get get = get_serializer.decode(bytes);

            ArrayList<String> post_topics = get.getCategories();

            // IR BUSCAR A BASE DE DADOS

            ArrayList<String> recentPosts;

            Protos.List list = new Protos.List(recentPosts);

            byte[] data = list_serializer.encode(list);

            messagingService.sendAsync(addr, "LIST", data);

        }, e);

    }

    public void start () {

        messagingService.start();

    }

    public void stop () {

        messagingService.stop();

    }


    public static void main(String[] args) throws Exception {

        // Getting id from command line
        int id = Integer.parseInt(args[0]);

        // Getting initial port from command line
        int base_port = Integer.parseInt(args[1]);

        // Getting the number of servers from the command line arguments
        int no_addresses = Integer.parseInt((args[2]));

        // Filling a list with the addresses of those servers
        List<Address> addresses = new ArrayList<>();
        for (int i=1; i<=no_addresses; i++) addresses.add(Address.from(base_port + i));

        new TwitterServer(id, addresses).start();

    }
}
