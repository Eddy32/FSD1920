import Protos.AdressElection;
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

    public TwitterServer(int id, List<Address> addresses) throws Exception {

        this.startedElection = false;
        this.id = id;
        this.leader = 0;
        this.addresses = addresses;
        this.vectorClock = new VectorClock(id, addresses.size());

        this.serverQueue = new ArrayList<>();
        for (int i=0; i<addresses.size(); i++) serverQueue.add(new TreeMap<>());

        ExecutorService e = Executors.newFixedThreadPool(6);

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
        Serializer address_id_serializer = new SerializerBuilder().addType(Protos.AdressElection.class).addType(Address.class).build();

        //When election is started - send message to other servers with port, find the highest #420
        messagingService.registerHandler("ELECTION", (addr,bytes)-> {
            System.out.println("ELEIÇÂOOOOOOO! recebi do "  + addr.toString());
            AdressElection addressReceived =  address_id_serializer.decode(bytes);
            if(this.id != addressReceived.getId_starter()){
                // Decoding post info
                int portReceived = addressReceived.getAddress().port();

                byte[] data;

                if(this.addresses.get(this.id).port() > portReceived ){
                    data = address_id_serializer.encode(new AdressElection(this.addresses.get(this.id), addressReceived.getId_starter()) );
                }
                else{
                    data = bytes; // election_serializer.encode(portReceived);
                }
                messagingService.sendAsync(this.addresses.get((this.id+1)%this.addresses.size()), "ELECTION", data);
            }
            else{
                this.startedElection = false;
                messagingService.sendAsync(addressReceived.getAddress(), "SHARE2LEADER", bytes);
                System.out.println("O CAMPEAO É"  + addressReceived.getAddress().toString());
            }
        }, e);

        //Tell the Server he is the new leader
        messagingService.registerHandler("SHARE2LEADER", (addr,bytes)-> {
            System.out.println("SOU LIDER: " + this.addresses.get(this.id));
            this.leader = this.id;
            AdressElection addressReceived =  address_id_serializer.decode(bytes);
            for(Address a: this.addresses)
                if(!a.equals(addressReceived.getAddress()))
                    messagingService.sendAsync(a, "SHARE", election_serializer.encode(addressReceived.getAddress()));
        }, e);

        //Share the new leader with every server
        messagingService.registerHandler("SHARE", (addr,bytes)-> {
            Address addressReceived =  election_serializer.decode(bytes);
            this.leader = this.addresses.indexOf(addressReceived);
            System.out.println("O meu novo lider é: " + this.leader + "::" + addressReceived.toString());

        }, e);





        // When a POST message is received
        messagingService.registerHandler("POST", (addr,bytes)-> {
            System.out.println("RECEBI!!");
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
/*
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
*/      messagingService.start();
        Thread.sleep(20000);
        System.out.println("ACORDEI :D");
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
        Serializer address_id_serializer = new SerializerBuilder().addType(Protos.AdressElection.class).addType(Address.class).build();
        Protos.AdressElection adressElection = new AdressElection(this.addresses.get(this.id), this.id);
         byte[] data = address_id_serializer.encode(adressElection);
        messagingService.sendAsync(this.addresses.get((this.id+1)%this.addresses.size()), "ELECTION", data);
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
