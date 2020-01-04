import Database.Pair;
import Database.Post;
import Protos.TryUpdate;
import Protos.Update;
import io.atomix.cluster.messaging.ManagedMessagingService;
import io.atomix.cluster.messaging.impl.NettyMessagingService;
import io.atomix.utils.net.Address;
import io.atomix.utils.serializer.Serializer;
import io.atomix.utils.serializer.SerializerBuilder;

import java.sql.DatabaseMetaData;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

import Database.DataBase;

public class TwitterServer {

    private int id; // Server ID
    private int leader; // Leader ID
    private HashMap<Integer,Integer> confirms;
    private List<Address> addresses; // Addresses of all network servers
    private ManagedMessagingService messagingService;
    private Clock clock; // Local Clock
    private VectorClock vectorClock; // Global clock (used only by leader)
    private ArrayList<ConcurrentHashMap<Integer, Protos.TryUpdate>> serverQueue; // Array of the post queue for each server (used only by leader)
    private DataBase postsDB; // Post database
    private ConcurrentHashMap<String,ConcurrentHashMap<Integer, Protos.Post>> postQueue;
    private ConcurrentHashMap<Integer, Update> updateQueue;
    private VectorClock clientClocks;
    private Clock updateClock;
    private Clock clock2FC;
    private ConcurrentHashMap<Integer, Integer> faseCommit;
    private Log logBD;
    private Log log2FC;
    private Log logTU;
    private Log logBR;

    public TwitterServer(int id, List<Address> addresses) throws Exception {

        this.id = id;
        this.leader = 0;
        this.addresses = addresses;
        this.confirms = new HashMap<Integer, Integer>();
        this.logBD = new Log("logBD" + id);
        this.log2FC = new Log("log2FC" + id);
        this.logTU = new Log("logTU" + id);
        this.logBR = new Log("logBR" + id);
        this.vectorClock = new VectorClock();
        this.clock = new Clock();
        this.clock2FC = new Clock();
        this.updateClock = new Clock();

        this.serverQueue = new ArrayList<>();
        for (int i=0; i<addresses.size(); i++) serverQueue.add(new ConcurrentHashMap<>());

        this.postQueue = new ConcurrentHashMap<>();
        this.updateQueue = new ConcurrentHashMap<>();
        this.faseCommit = new ConcurrentHashMap<>();
        this.clientClocks = new VectorClock();

        this.postsDB = this.logBD.readDB();

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
        Serializer ack_serializer = new SerializerBuilder().addType(Pair.class).build();

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
        // Handler ACK
        messagingService.registerHandler("ACK", (addr,bytes)-> {

            Pair<Integer,Integer> key = ack_serializer.decode(bytes);
            switch (key.getLeft()){
                case 1:
                    this.logTU.confirmAction(key.getRight());
                    break;
                case 2:
                    this.logBR.confirmAction(key.getRight());
                    break;
                case 3:
                        synchronized (faseCommit){
                            if (this.faseCommit.containsKey(key.getRight())){
                                int aux = this.faseCommit.get(key.getRight());
                                if( ++aux == addresses.size() ){
                                    log2FC.confirmAction(key.getRight());
                                }else{
                                    this.faseCommit.put(key.getRight(), aux);
                                }
                            }
                            else{
                                this.faseCommit.put(key.getRight(),1);
                            }
                           }
                    break;
                case 4:
                    this.logBD.confirmAction(key.getRight());
                    break;
            }

        },e);
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
            int post_owner_clock = post.getId();

            // PRINTING

            StringBuffer post_print = new StringBuffer();
            post_print.append("RECEIVED POST FROM " + addr.port() + ": ( ");
            for (String t: post_topics) post_print.append(t + " ");
            post_print.append(") -> " + post_text);

            System.out.println(post_print.toString());
            //synchronized (postQueue) {
                if (!this.postQueue.containsKey(post_owner)){
                    this.postQueue.put(post_owner, new ConcurrentHashMap<>());
                    System.out.println("CLOCK FODIDO " + post_text);
                }
            //}

            int serverOwnerClock = this.clientClocks.getClock(post_owner);

            // IF USER CLOCK IS MATCHING
            if (post_owner_clock == serverOwnerClock + 1) {

                System.out.println("TRY UPDATE:  -> " + post_text + " || User = " + post_owner + " UserClock = " + post_owner_clock + "|| GLOBAL " + serverOwnerClock);

                // Sending try updates by topic
                for (String topic: post_topics) {

                    int post_clock = clock.increment();
                    System.out.println("SENDING TRY UPDATE TO " + addresses.get(leader).port() + ": (" + topic + ") -> " + post_text + " || Clock = " + post_clock + " || User = " + post_owner + " UserClock = " + post_owner_clock);

                    Protos.TryUpdate try_update = new Protos.TryUpdate(post_text, topic, id, post_clock, post_owner, post_owner_clock);

                    byte[] data = try_update_serializer.encode(try_update);

                    this.logTU.writeLog("TRY_UPDATE " + try_update.toString() + " " +  addresses.get(leader).port(),post_clock);
                    messagingService.sendAsync(addresses.get(leader), "TRY_UPDATE", data);



                }
                messagingService.sendAsync(addr,"ACK",ack_serializer.encode(post_owner_clock));

                this.clientClocks.increment(post_owner);

                // Checking rest of the queue
                for (int i=clientClocks.getClock(post_owner) + 1; this.postQueue.get(post_owner).containsKey(i); i++) {

                    post = this.postQueue.get(post_owner).remove(i);

                    post_text = post.getText();
                    post_topics = post.getCategories();
                    post_owner_clock = post.getId();

                    post_owner = post.getOwner();

                    // Sending try updates by topic
                    for (String topic: post_topics) {

                        int post_clock = clock.increment();
                        System.out.println("SENDING TRY UPDATE TO " + addresses.get(leader).port() + ": (" + topic + ") -> " + post_text + " || Clock = " + post_clock + " || User = " + post_owner + " UserClock = " + post_owner_clock);

                        Protos.TryUpdate try_update = new Protos.TryUpdate(post_text, topic, id, post_clock, post_owner, post_owner_clock);

                        byte[] data = try_update_serializer.encode(try_update);

                        this.logTU.writeLog("TRY_UPDATE " + try_update.toString() + " " +  addresses.get(leader).port(),post_clock);
                        messagingService.sendAsync(addresses.get(leader), "TRY_UPDATE", data);

                    }
                    messagingService.sendAsync(addr,"ACK",ack_serializer.encode(post_owner_clock));

                    this.clientClocks.increment(post_owner);

                }

            } else {

                // Adding post to queue
                this.postQueue.get(post_owner).put(post_owner_clock, post);

                if(!this.postQueue.get(post_owner).containsKey(post_owner_clock))
                    System.out.println("NAO ENTROU NA QUEUE: " + post_text);

                // Re-checking if the value added to the queue is the next to be broadcasted

                System.out.println("ADDING POST TO POST QUEUE OF " + addr.port() + ":  -> " + post_text + " || Post Clock (" + post_owner_clock + ") /= 1 + Global Server Clock (" + clientClocks.getClock(post_owner) + ")" + " || User = " + post_owner + " UserClock = " + post_owner_clock);
                // Removing everything from the queue becasuse it's my turn
                if (post_owner_clock == clientClocks.getClock(post_owner) + 1) {

                    for (int i=post_owner_clock; this.postQueue.get(post_owner).containsKey(i); i++) {

                        post = this.postQueue.get(post_owner).remove(i);

                        post_text = post.getText();
                        post_topics = post.getCategories();
                        post_owner_clock = post.getId();
                        post_owner = post.getOwner();

                        // Sending try updates by topic
                        for (String topic: post_topics) {

                            int post_clock = clock.increment();
                            System.out.println("SENDING TRY UPDATE TO " + addresses.get(leader).port() + ": (" + topic + ") -> " + post_text + " || Clock = " + post_clock + " || User = " + post_owner + " UserClock = " + post_owner_clock);

                            Protos.TryUpdate try_update = new Protos.TryUpdate(post_text, topic, id, post_clock, post_owner, post_owner_clock);

                            byte[] data = try_update_serializer.encode(try_update);

                            this.logTU.writeLog("TRY_UPDATE " + try_update.toString() + " " +  addresses.get(leader).port(),post_clock);
                            messagingService.sendAsync(addresses.get(leader), "TRY_UPDATE", data);

                        }
                        messagingService.sendAsync(addr,"ACK",ack_serializer.encode(post_owner_clock));

                        this.clientClocks.increment(post_owner);

                    }


                }

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

            //Guarda em log o post recebido

            int vectorClockAct = vectorClock.getClock(serverId);

            if (serverClock == vectorClock.getClock(serverId) + 1) {

                // Getting index for post topic
                Database.Pair<Integer, Integer> index = this.postsDB.getIndexGlobalCounter(post_topic);

                System.out.println("SENDING TO " + addr.port() + " FOR BROADCAST: (" + post_topic + ") -> " + post_text + " || Post Clock (" + serverClock + ") == 1 + Global Server Clock (" + vectorClock.getClock(serverId) + ") || Index: " + index.getLeft() + " GLOBAL_DB " + index.getRight());

                // Sending broadcast of current post
                Protos.Update broadcast = new Protos.Update(post_text, post_topic, index.getLeft(), index.getRight());
                byte[] data = update_serializer.encode(broadcast);

                this.logBR.writeLog("BROADCAST " + try_update.toString() + " " + addresses.get(serverId).port(),index.getRight());
                byte [] dataKey = ack_serializer.encode(new Pair<Integer, Integer>(1, try_update.getServerClock()) );
                //envia o ack do pacote que acabou de receber
                messagingService.sendAsync(addr,"ACK", dataKey);

                messagingService.sendAsync(addresses.get(serverId), "BROADCAST", data);

                vectorClock.increment(serverId);
                // Sending the following posts in the queue for broadcast
                for (int i = vectorClock.getClock(serverId) + 1; serverQueue.get(serverId).containsKey(i); i++) {
                    try_update = serverQueue.get(serverId).remove(i);

                    post_text = try_update.getText();
                    post_topic = try_update.getCategory();
                    index = this.postsDB.getIndexGlobalCounter(post_topic);


                    System.out.println("MANDAR DA QUEUE SENDING TO " + addr.port() + " FOR BROADCAST: (" + post_topic + ") -> " + post_text + " || Post Clock (" + try_update.getServerClock() + ") == 1 + Global Server Clock (" + vectorClock.getClock(serverId) + ") || Index: " + index.getLeft() + " Global DB " + index.getRight()  );
                    broadcast = new Protos.Update(post_text, post_topic, index.getLeft(), index.getRight());
                    data = update_serializer.encode(broadcast);
                    messagingService.sendAsync(addresses.get(serverId), "BROADCAST", data);
                    this.logBR.writeLog("BROADCAST " + try_update.toString() + " " + addresses.get(serverId).port(),try_update.getServerClock());
                    dataKey = ack_serializer.encode(new Pair<Integer, Integer>(1,  try_update.getServerClock()));
                    //envia o ack do pacote que acabou de receber
                    messagingService.sendAsync(addr,"ACK", dataKey);
                    vectorClock.increment(serverId);

                }

            } else {

                // PRINTING

                System.out.println("ADDING TRY UPDATE TO SERVER QUEUE OF " + addr.port() + ": (" + post_topic + ") -> " + post_text + " || Post Clock (" + serverClock + ") /= 1 + Global Server Clock (" + vectorClockAct + ")" + " || User = " + post_owner + " UserClock = " + post_owner_clock);


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
                        Database.Pair<Integer, Integer> index = this.postsDB.getIndexGlobalCounter(post_topic);

                        System.out.println("SENDING2 TO " + addr.port() + " FOR BROADCAST: (" + post_topic + ") -> " + post_text + " || Post Clock (" + try_update.getServerClock() + ") == 1 + Global Server Clock (" + vectorClock.getClock(serverId) + ") || Index: " + index.getLeft() + "Global Clock" + index.getRight());

                        Protos.Update broadcast = new Protos.Update(post_text, post_topic, index.getLeft(), index.getRight());
                        byte[] data = update_serializer.encode(broadcast);

                        this.logBR.writeLog("BROADCAST " + try_update.toString() + " " + addresses.get(serverId).port(),try_update.getServerClock());
                        byte [] dataKey = ack_serializer.encode(new Pair <Integer, Integer>(1, try_update.getServerClock()));
                        //envia o ack do pacote que acabou de receber
                        messagingService.sendAsync(addr,"ACK", dataKey);

                        messagingService.sendAsync(addresses.get(serverId), "BROADCAST", data);
                        vectorClock.increment(serverId);

                    }

                }

            }

        }, e);

        // When a BROADCAST message is received
        messagingService.registerHandler("BROADCAST", (addr,bytes)-> {

            Update update = update_serializer.decode(bytes);
            int global_clock = update.getGlobal_clock();
            this.log2FC.writeLog("2FC " + update.toString() + " " + addr.port() ,global_clock);
            messagingService.sendAsync(addr,"ACK", ack_serializer.encode(new Pair<Integer, Integer>(2,update.getGlobal_clock())));

            System.out.println("Recebi broadcast");
            this.confirms.put(update.getGlobal_clock(), 0);
            for(Address a: addresses){
                //System.out.println("Estão todos ok?");
                messagingService.sendAsync(a,"REQUEST",bytes );
            }

        }, e);

        messagingService.registerHandler("REQUEST", (addr,bytes)-> {
            //System.out.println("ESTOU OTIMO AMIGO :) ");

            messagingService.sendAsync(addr,"PREPARED",bytes);

        }, e);

        messagingService.registerHandler("PREPARED", (addr,bytes)-> {

            Protos.Update update = update_serializer.decode(bytes);
            update2PC(update.getGlobal_clock());
            if(this.confirms.get(update.getGlobal_clock())==this.addresses.size()){ //garantir que ja recebeu todas as confirmaçoes
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

            int globalClock = update.getGlobal_clock();
            int updateCounter = updateClock.get();

            /* COMMIT FAIL
            synchronized (faseCommit){
                if (this.faseCommit.containsKey(globalClock)){
                    int aux = this.faseCommit.get(globalClock);
                    if( aux == addresses.size() ){
                        log.confirmAction(globalClock);
                    }else{
                        this.faseCommit.put(globalClock, ++aux);
                    }
                }
                else{
                    this.faseCommit.put(globalClock,1);
                }
            }*/



            // IF USER CLOCK IS MATCHING
            if (globalClock == updateCounter + 1) {

                System.out.println("UPDATING LOCAL DB:  -> " + post_text + " || globalClock " + globalClock + " updateCounter " + updateCounter + " index " + topicIndex);
                System.out.println("GLOBAL TO ACK 3 " + globalClock);
                this.postsDB.addPost(post_topic, post_text, topicIndex);
                this.logBD.writeDB(update.toString());
                messagingService.sendAsync(addr,"ACK", ack_serializer.encode(new Pair<Integer, Integer>(3, globalClock)));

                this.updateClock.increment();

                // Checking rest of the queue
                for (int i=updateClock.get() + 1; this.updateQueue.containsKey(i); i++) {

                    update = this.updateQueue.remove(i);

                    post_text = update.getText();
                    post_topic = update.getCategory();
                    topicIndex = update.getIndex();
                    globalClock = update.getGlobal_clock();

                    System.out.println("UPDATING FROM QUEUE LOCAL DB:  -> " + post_text + " || globalClock " + globalClock + " updateCounter " + i + " index " + topicIndex);

                    System.out.println("GLOBAL TO ACK 3 " + globalClock);
                    this.postsDB.addPost(post_topic, post_text, topicIndex);
                    this.logBD.writeDB(update.toString());

                    messagingService.sendAsync(addr,"ACK", ack_serializer.encode(new Pair<Integer, Integer>(3, globalClock)));

                    this.updateClock.increment();

                }

            } else {

                // Adding post to queue
                this.updateQueue.put(globalClock, update);
                System.out.println("ADDING UPDATE TO QUEUE:  -> " + post_text + " || globalClock " + globalClock + " updateCounter " + updateCounter + " index " + topicIndex);
                // Re-checking if the value added to the queue is the next to be broadcasted

                // Removing everything from the queue becasuse it's my turn

                if (updateClock.get() == globalClock + 1 ) {
                    System.out.println("Welele ");

                    for (int i=globalClock; this.updateQueue.containsKey(i); i++) {

                        update = this.updateQueue.remove(i);

                        post_text = update.getText();
                        post_topic = update.getCategory();
                        topicIndex = update.getIndex();
                        globalClock = update.getGlobal_clock();

                        System.out.println("2-UPDATING FROM QUEUE LOCAL DB:  -> " + post_text + " || globalClock " + globalClock + " updateCounter " + i + " index " + topicIndex);
                        System.out.println("GLOBAL TO ACK 3 " + globalClock);

                        this.postsDB.addPost(post_topic, post_text, topicIndex);
                        this.logBD.writeDB(update.toString());

                        messagingService.sendAsync(addr,"ACK", ack_serializer.encode(new Pair<Integer, Integer>(3, globalClock)));

                        this.updateClock.increment();

                    }

                }
            }




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

        ArrayList<Pair<Update,Integer>> br_read = logBR.readBR();
        for(Pair<Update,Integer> to_send : br_read){
            messagingService.sendAsync(Address.from(to_send.getRight()) ,"BROADCAST", update_serializer.encode(to_send.getLeft()) );
        }
        ArrayList<Pair<TryUpdate,Integer>> tu_read = logTU.readTU();
        for(Pair<TryUpdate,Integer> to_send : tu_read){
            messagingService.sendAsync(Address.from(to_send.getRight()) ,"TRY_UPDATE", try_update_serializer.encode(to_send.getLeft()) );
        }
        ArrayList<Pair<Protos.Update,Integer>> fc_read = log2FC.readFC();
        for(Pair<Protos.Update,Integer> to_send : fc_read){
            for(Address addr : this.addresses){
                this.confirms.put(to_send.getLeft().getGlobal_clock(), 0);
                messagingService.sendAsync(addr ,"REQUEST", update_serializer.encode(to_send.getLeft()) );
            }
        }
        // Thread.sleep(5000);
        // startElection();


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
