import Database.Post;
import io.atomix.cluster.messaging.ManagedMessagingService;
import io.atomix.cluster.messaging.impl.NettyMessagingService;
import io.atomix.utils.net.Address;
import io.atomix.utils.serializer.Serializer;
import io.atomix.utils.serializer.SerializerBuilder;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.TreeMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

import Database.DataBase;

public class TwitterServer {

    private int id; // Server ID
    private int leader; // Leader ID
    private int twoPC;
    private HashMap<Integer,Integer> confirms;
    private List<Address> addresses; // Addresses of all network servers
    private ManagedMessagingService messagingService;
    private Clock clock; // Local Clock
    private VectorClock vectorClock; // Global clock (used only by leader)
    private ArrayList<TreeMap<Integer, Protos.TryUpdate>> serverQueue; // Array of the post queue for each server (used only by leader)
    private DataBase postsDB; // Post database

    private TestJournal log;

    private HashMap<String,TreeMap<Integer, Protos.Update>> postQueue;
    private VectorClock clientClocks;


    public TwitterServer(int id, List<Address> addresses) throws Exception {

        this.id = id;
        this.leader = 0;
        this.twoPC = 0;
        this.addresses = addresses;
        this.confirms = new HashMap<Integer, Integer>();

        this.vectorClock = new VectorClock();
        this.clock = new Clock();
        this.postsDB = new DataBase();
        this.log = new TestJournal("log" + id);

        this.serverQueue = new ArrayList<>();
        for (int i=0; i<addresses.size(); i++) serverQueue.add(new TreeMap<>());

        this.postQueue = new HashMap<>();
        this.clientClocks = new VectorClock();

        ExecutorService e = Executors.newFixedThreadPool(12);

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
            Protos.AdressElection addressReceived =  address_id_serializer.decode(bytes);
            if(this.id != addressReceived.getId_starter()){

                // Decoding post info
                int portReceived = addressReceived.getAddress().port();

                byte[] data;

                if(this.addresses.get(this.id).port() > portReceived ){
                    data = address_id_serializer.encode(new Protos.AdressElection(this.addresses.get(this.id), addressReceived.getId_starter()) );
                }
                else{
                    data = bytes; // election_serializer.encode(portReceived);
                }
                messagingService.sendAsync(this.addresses.get((this.id+1)%this.addresses.size()), "ELECTION", data);
            }
            else{

                messagingService.sendAsync(addressReceived.getAddress(), "SHARE2LEADER", bytes);

            }
        }, e);

        //Tell the Server he is the new leader
        messagingService.registerHandler("SHARE2LEADER", (addr,bytes)-> {
            this.leader = this.id;

            Protos.AdressElection addressReceived =  address_id_serializer.decode(bytes);
            for(Address a: this.addresses)
                if(!a.equals(addressReceived.getAddress()))
                    messagingService.sendAsync(a, "SHARE", election_serializer.encode(addressReceived.getAddress()));

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

            String post_owner = post.getOwner();
            int post_ownwer_clock = post.getId();

            // PRINTING

            StringBuffer post_print = new StringBuffer();
            post_print.append("RECEIVED POST FROM " + addr.port() + ": ( ");
            for (String t: post_topics) post_print.append(t + " ");
            post_print.append(") -> " + post_text);

            System.out.println(post_print.toString());

            // Sending try updates by topic
            for (String topic: post_topics) {

                int post_clock = clock.increment();
                System.out.println("SENDING TRY UPDATE TO " + addresses.get(leader).port() + ": (" + topic + ") -> " + post_text + " || Clock = " + post_clock + " || User = " + post_owner + " UserClock = " + post_ownwer_clock);

                Protos.TryUpdate try_update = new Protos.TryUpdate(post_text, topic, id, post_clock, post_owner, post_ownwer_clock);

                byte[] data = try_update_serializer.encode(try_update);
                this.log.writeLog("SENDING",post_clock);
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

            String post_owner = try_update.getUser();
            int post_owner_clock = try_update.getUserClock();


            int vectorClockAct = vectorClock.getClock(serverId);

            if (serverClock == vectorClock.getClock(serverId) + 1) {

                // Getting index for post topic
                Integer index = this.postsDB.getIndex(post_topic);

                System.out.println("SENDING TO " + addr.port() + " FOR BROADCAST: (" + post_topic + ") -> " + post_text + " || Post Clock (" + serverClock + ") == 1 + Global Server Clock (" + vectorClock.getClock(serverId) + ") || Index: " + index + " || User = " + post_owner + " UserClock = " + post_owner_clock);

                // Sending broadcast of current post
                Protos.Update broadcast = new Protos.Update(post_text, post_topic, index, post_owner, post_owner_clock);
                byte[] data = update_serializer.encode(broadcast);
                messagingService.sendAsync(addresses.get(serverId), "BROADCAST", data);

                vectorClock.increment(serverId);
                // Sending the following posts in the queue for broadcast
                for (int i = vectorClock.getClock(serverId) + 1; serverQueue.get(serverId).containsKey(i); i++) {
                    try_update = serverQueue.get(serverId).remove(i);

                    post_text = try_update.getText();
                    post_topic = try_update.getCategory();
                    index = this.postsDB.getIndex(post_topic);
                    post_owner = try_update.getUser();
                    post_owner_clock = try_update.getUserClock();


                    System.out.println("SENDING TO " + addr.port() + " FOR BROADCAST: (" + post_topic + ") -> " + post_text + " || Post Clock (" + try_update.getServerClock() + ") == 1 + Global Server Clock (" + vectorClock.getClock(serverId) + ") || Index: " + index + " || User = " + post_owner + " UserClock = " + post_owner_clock);

                    System.out.println("MANDAR DA QUEUE SENDING TO " + addr.port() + " FOR BROADCAST: (" + post_topic + ") -> " + post_text + " || Post Clock (" + try_update.getServerClock() + ") == 1 + Global Server Clock (" + vectorClock.getClock(serverId) + ") || Index: " + index);

                    broadcast = new Protos.Update(post_text, post_topic, index, post_owner, post_owner_clock);
                    data = update_serializer.encode(broadcast);
                    messagingService.sendAsync(addresses.get(serverId), "BROADCAST", data);
                    vectorClock.increment(serverId);

                }

            } else {

                // PRINTING

                System.out.println("ADDING POST TO QUEUE OF " + addr.port() + ": (" + post_topic + ") -> " + post_text + " || Post Clock (" + serverClock + ") /= 1 + Global Server Clock (" + vectorClockAct + ")" + " || User = " + post_owner + " UserClock = " + post_owner_clock);


                // Adding to queue
                serverQueue.get(serverId).put(serverClock, try_update);

                // Re-checking if the value added to the queue is the next to be broadcasted
                if (serverClock == vectorClock.getClock(serverId) + 1) { // serverClock 5; Vector = 4 (+1=5)
                    System.out.println("Welele ");

                    // Removing everything from the queue becasuse it's my turn
                    for (int i = serverClock ; serverQueue.get(serverId).containsKey(i); i++) { // i a começar a 6
                                                                                                   //G Fucking
                        try_update = serverQueue.get(serverId).remove(i);

                        post_text = try_update.getText();
                        post_topic = try_update.getCategory();
                        int index = this.postsDB.getIndex(post_topic);
                        post_owner = try_update.getUser();
                        post_owner_clock = try_update.getUserClock();

                        System.out.println("SENDING TO " + addr.port() + " FOR BROADCAST: (" + post_topic + ") -> " + post_text + " || Post Clock (" + try_update.getServerClock() + ") == 1 + Global Server Clock (" + vectorClock.getClock(serverId) + ") || Index: " + index + " || User = " + post_owner + " UserClock = " + post_owner_clock);

                        Protos.Update broadcast = new Protos.Update(post_text, post_topic, index, post_owner, post_owner_clock);
                        byte[] data = update_serializer.encode(broadcast);
                        messagingService.sendAsync(addresses.get(serverId), "BROADCAST", data);
                        vectorClock.increment(serverId);

                    }

                }

            }

        }, e);

        // When a BROADCAST message is received
        messagingService.registerHandler("BROADCAST", (addr,bytes)-> {



            int key = getTwoPC(); //id para identificar os ACK para o post
            this.confirms.put(key,0); //inicializar a 0
            Protos.Update post = update_serializer.decode(bytes);
            post.setKey(key); //acrescentar ao post a chave
            byte[] data = update_serializer.encode(post);

            for(Address a: addresses){
                //System.out.println("Estão todos ok?");
                messagingService.sendAsync(a,"REQUEST",data );
            }

            /*
            for (Address a : addresses) {

                System.out.println("BROADCASTING TO " + a.port());

                messagingService.sendAsync(a, "UPDATE", bytes);

            }
            */

        }, e);

        messagingService.registerHandler("REQUEST", (addr,bytes)-> {
            //System.out.println("ESTOU OTIMO AMIGO :) ");

            messagingService.sendAsync(addr,"PREPARED",bytes);

        }, e);

        messagingService.registerHandler("PREPARED", (addr,bytes)-> {

            Protos.Update post = update_serializer.decode(bytes);
            update2PC(post.getKey());
            if(this.confirms.get(post.getKey())==this.addresses.size()){ //garantir que ja recebeu todas as confirmaçoes
                for(Address a: this.addresses){
                    //System.out.println("ENTAO TOMA ESTE PACOTE");
                    messagingService.sendAsync(a,"COMMIT",bytes);
                }
            }
            //else System.out.println("esperando mais confirmações");

        }, e);

        messagingService.registerHandler("COMMIT", (addr,bytes)-> {
            //System.out.println("RECEBIDO COM TODO O GOSTO :D ");

            Protos.Update update = update_serializer.decode(bytes);

            String post_text = update.getText();
            String post_topic = update.getCategory();
            int topicIndex = update.getIndex();

            String post_owner = update.getUsername();
            int post_owner_clock = update.getUserClock();

            System.out.println("UPDATING LOCAL DB: (" + post_topic + ") -> " + post_text + " || Index = " + topicIndex + " || User = " + post_owner + " UserClock = " + post_owner_clock);

            // IF USER CLOCK IS MATCHING

            this.postsDB.addPost(post_topic, post_text, topicIndex);

            //if (this.clientClocks.getClock())

            //if (this.postQueue.)


        }, e);

        // When a GET message is received
        messagingService.registerHandler("GET", (addr,bytes)-> {
            System.out.println("Received Get");
            // Decoding data received
            Protos.Get get = get_serializer.decode(bytes);

            // 10 most recent posts
            ArrayList<Database.Post> mostRecent = new ArrayList<>();

            ArrayList<String> post_topics = get.getCategories();

            // PRINTING

            StringBuffer post_print = new StringBuffer();
            post_print.append("GETTING LIST FOR TOPICS: ");
            for (String t: post_topics) post_print.append(t + " ");

            System.out.println(post_print.toString());

            for (String topic: post_topics) {

                System.out.println("Checking topic : " + topic);

                for (Database.Post post : this.postsDB.getPostsTopic(topic)) {

                    System.out.println("Checking post : " + post.getPost());

                    if (mostRecent.size() < 10) { System.out.println("Adding post to most recents"); mostRecent.add(post); }

                    else {

                        System.out.println("Already collected 10 posts: " + mostRecent.size());

                        boolean added = false;

                        for (int i = 0; i < 10; i++) {

                            Database.Post postInRecents = mostRecent.get(i);

                            if (postInRecents.getGlobalCounter() > post.getGlobalCounter()) break;
                            else {

                                System.out.println("Adding post to most recents and shifting array");

                                // Shift array to the right
                                for (int j = i; j < 9; j++) mostRecent.add(j + 1, mostRecent.get(j));

                                // Add element
                                mostRecent.add(i, post);

                            }


                        }

                        // If we don't add, then no more post of the topic will be added
                        if (!added) break;


                    }
                }
            }

            // PRINTING

            post_print = new StringBuffer();
            post_print.append("SENDING LIST FOR TOPICS ( ");
            for (String t: post_topics) post_print.append(t + " ");
            post_print.append(" ) -> ");
            for (Post p: mostRecent) post_print.append("\"" + p.getPost() + "\" ");

            System.out.println(post_print.toString());

            Protos.List list = new Protos.List(mostRecent.stream().map(Post::getPost).collect(Collectors.toList()));

            byte[] data = list_serializer.encode(list);
            System.out.println("Send list to " + addr.toString());
            messagingService.sendAsync(addr, "LIST", data);

        }, e);


        messagingService.start();
        Thread.sleep(5000);
        startElection();


    }

    public void start () {

        messagingService.start();

    }

    public void stop () {

        messagingService.stop();

    }



    public void startElection(){

        System.out.println("Lez go");

        Serializer address_id_serializer = new SerializerBuilder().addType(Protos.AdressElection.class).addType(Address.class).build();

        Protos.AdressElection adressElection = new Protos.AdressElection(this.addresses.get(this.id), this.id);

        byte[] data = address_id_serializer.encode(adressElection);

        messagingService.sendAsync(this.addresses.get((this.id+1)%this.addresses.size()), "ELECTION", data);

    }

    public synchronized int getTwoPC(){
        return this.twoPC++;
    }

    public synchronized void update2PC(int key){
        this.confirms.replace(key,this.confirms.get(key)+1);
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

        new TwitterServer(id, addresses);

    }
}
