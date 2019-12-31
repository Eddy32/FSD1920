import Protos.TryUpdate;
import io.atomix.cluster.messaging.ManagedMessagingService;
import io.atomix.cluster.messaging.MessagingService;
import io.atomix.cluster.messaging.impl.NettyMessagingService;
import io.atomix.utils.net.Address;
import io.atomix.utils.serializer.Serializer;
import io.atomix.utils.serializer.SerializerBuilder;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.TreeMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.IntStream;

public class TwitterServer {

    private boolean startedElection;
    private int id;
    private int leader;
    private List<Address> addresses;
    private ManagedMessagingService messagingService;
    private VectorClock vectorClock;
    private ArrayList<TreeMap<Integer, Protos.TryUpdate>> serverQueue;
    private DataBase postsDB;

    public TwitterServer(int id, List<Address> addresses) throws Exception {

        this.startedElection = false;
        this.id = id;
        this.leader = 0;
        this.addresses = addresses;
        this.vectorClock = new VectorClock(id, addresses.size());
        this.postsDB = new DataBase();

        this.serverQueue = new ArrayList<>();
        for (int i=0; i<addresses.size(); i++) serverQueue.add(new TreeMap<>());

        ExecutorService e = Executors.newFixedThreadPool(1);

        // Starting messaging service
        messagingService = new NettyMessagingService.Builder()
                .withName("Twitter_Server_" + this.addresses.get(this.id).toString())
                .withReturnAddress(this.addresses.get(id)).build();

        // Serializers
        Serializer post_serializer = new SerializerBuilder().addType(Protos.Post.class).build();
        Serializer try_update_serializer = new SerializerBuilder().addType(Protos.TryUpdate.class).build();
        Serializer update_serializer = new SerializerBuilder().addType(Protos.Update.class).build();
        Serializer get_serializer = new SerializerBuilder().addType(Protos.Get.class).build();
        Serializer list_serializer = new SerializerBuilder().addType(Protos.List.class).build();
        Serializer election_serializer = new SerializerBuilder().addType(Address.class).build();

        //When election is started - send message to other servers with port, find the highest #420
        messagingService.registerHandler("ELECTION", (addr,bytes)-> {

            Address addressReceived =  election_serializer.decode(bytes);

            if (!this.startedElection) {
                // Decoding post info
                int portReceived = addressReceived.port();

                byte[] data;

                if (this.addresses.get(this.id).port() > portReceived)

                    data = election_serializer.encode(this.addresses.get(this.id));

                else

                    data = bytes; // election_serializer.encode(portReceived);

                messagingService.sendAsync(this.addresses.get((this.id+1) % this.addresses.size()), "ELECTION", data);

            } else {

                this.startedElection = false;
                messagingService.sendAsync(addressReceived, "SHARE2LEADER", bytes);

            }

        }, e);

        //Tell the Server he is the new leader
        messagingService.registerHandler("SHARE2LEADER", (addr,bytes)-> {

            this.leader = this.id;

            Address addressReceived =  election_serializer.decode(bytes);

            for(Address a: this.addresses)
                if(!a.equals(addressReceived))
                    messagingService.sendAsync(a, "SHARE", bytes);

        }, e);

        //Share the new leader with every server
        messagingService.registerHandler("SHARE", (addr,bytes)-> {

            Address addressReceived =  election_serializer.decode(bytes);

            this.leader = this.addresses.indexOf(addressReceived);

        }, e);

        // When a POST message is received
        messagingService.registerHandler("POST", (addr,bytes)-> {

            // Decoding post info
            Protos.Post post = post_serializer.decode(bytes);

            String post_text = post.getText();
            ArrayList<String> post_topics = post.getCategories();

            // PRINTING

            StringBuffer post_print = new StringBuffer();
            post_print.append("POST (" + addr.port() + "): ( ");
            for (String t: post_topics) post_print.append(t + " ");
            post_print.append(") -> " + post_text);

            System.out.println(post_print.toString());

            // Sending try updates by topic
            for (String topic: post_topics) {

                Protos.TryUpdate try_update = new Protos.TryUpdate(topic, post_text, vectorClock.getId(), vectorClock.increment());

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

                // Getting index for post topic
                Integer index = this.postsDB.getIndex(post_topic);

                // Sending broadcast of current post
                Protos.Update broadcast = new Protos.Update(post_text, post_topic, index);
                byte[] data = update_serializer.encode(broadcast);
                messagingService.sendAsync(addresses.get(serverId), "BROADCAST", data);

                System.out.println("SENDING TO " + addr.port() + " FOR BROADCAST: (" + post_topic + ") -> " + post_text);

                // Sending the following posts in the queue for broadcast
                for (int i = serverClock + 1; !serverQueue.get(serverId).containsKey(i); i++) {

                    try_update = serverQueue.get(serverId).remove(i);

                    post_text = try_update.getText();
                    post_topic = try_update.getCategory();
                    index = this.postsDB.getIndex(post_topic);

                    broadcast = new Protos.Update(post_text, post_topic, index);
                    data = update_serializer.encode(broadcast);
                    messagingService.sendAsync(addresses.get(serverId), "BROADCAST", data);

                    System.out.println("SENDING TO " + addr.port() + " FOR BROADCAST: (" + post_topic + ") -> " + post_text);

                }

            } else {

                // PRINTING
                System.out.println("ADDING POST TO QUEUE OF " + addr.port() + ": ( " + post_topic + " ) -> " + post_text);

                // Adding to queue
                serverQueue.get(serverId).put(serverClock, try_update);

            }

        }, e);

        // When a BROADCAST message is received
        messagingService.registerHandler("BROADCAST", (addr,bytes)-> {

            for (Address a : addresses) {

                System.out.println("BROADCASTING TO " + a.port());

                messagingService.sendAsync(a, "UPDATE", bytes);

            }

        }, e);

        // When an UPDATE message is received
        messagingService.registerHandler("UPDATE", (addr,bytes)-> {

            // Decoding data received
            Protos.Update update = update_serializer.decode(bytes);

            String post_text = update.getText();
            String post_topic = update.getCategory();
            int topicIndex = update.getIndex();

            System.out.println("UPDATING LOCAL DB: (" + post_topic + ") -> " + post_text);

            this.postsDB.addPost(post_topic, post_text, topicIndex);

        }, e);
/*
        // When a GET message is received
        messagingService.registerHandler("GET", (addr,bytes)-> {

            // Decoding data received
            Protos.Get get = get_serializer.decode(bytes);

            ArrayList<String> post_topics = get.getCategories();

            //this.postsDB.getPostsTopic()

            ArrayList<String> recentPosts;

            Protos.List list = new Protos.List(recentPosts);

            byte[] data = list_serializer.encode(list);

            messagingService.sendAsync(addr, "LIST", data);

        }, e);
*/
        messagingService.start();
        Thread.sleep(10000);

        startElection();

    }

    public void start () {

        messagingService.start();

    }

    public void stop () {

        messagingService.stop();

    }

    public void startElection(){

        this.startedElection = true;

        Serializer election_serializer = new SerializerBuilder().addType(Address.class).build();

        byte[] data = election_serializer.encode(this.addresses.get(this.id));

        messagingService.sendAsync(this.addresses.get((this.id+1) % this.addresses.size()), "ELECTION", data);

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
      //  for (int i=1; i<=no_addresses; i++) addresses.add(new Address("MBP-de-Pedro.lan",base_port+i, ));
        for (int i=1; i<=no_addresses; i++) addresses.add(Address.from(base_port + i));

        new TwitterServer(id, addresses);

    }
}
