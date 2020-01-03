import Protos.Get;
import Protos.List;
import Protos.Post;
import io.atomix.cluster.messaging.ManagedMessagingService;
import io.atomix.cluster.messaging.impl.NettyMessagingService;
import io.atomix.utils.net.Address;
import io.atomix.utils.serializer.Serializer;
import io.atomix.utils.serializer.SerializerBuilder;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Random;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class TwitterBot implements Runnable {

    private Address address;
    private Address servidor;
    private ManagedMessagingService messagingService;
    private ExecutorService e;
    private ArrayList<String> categories;
    private Serializer post_serializer = new SerializerBuilder().addType(Post.class).build();
    private Serializer get_serializer = new SerializerBuilder().addType(Get.class).build();
    private Serializer list_serializer = new SerializerBuilder().addType(List.class).build();

    private int id;
    private int no_posts;
    private int no_topics;
    private Clock clock;

    public TwitterBot(Address address, Address servidor, int id, int no_posts, int no_topics) {

        this.address = address;
        this.e = Executors.newFixedThreadPool(1);
        this.categories = new ArrayList<>();
        this.id = id;
        this.no_posts = no_posts;
        this.no_topics = no_topics;
        this.clock = new Clock();

        this.messagingService = new NettyMessagingService.Builder()
                .withName("Twitter_Bot_" + address.toString())
                .withReturnAddress(address).build();

        messagingService.start();

        // Creating serializer for encoding and decoding to/from bytes
        Serializer s = new SerializerBuilder().addType(Address.class).build();

        // Register Handler for when a message is received
        messagingService.registerHandler("ADDRESS", (addr, bytes) -> {

            // Decoding data received
            Address msg = s.decode(bytes);
            this.servidor = msg;

            // Printing it
            System.out.println("Server = " + msg.toString());

            this.run();

        }, e);

        // Handlers
        messagingService.registerHandler("LIST", (addr, bytes) -> {
            System.out.println("Recebi LIST");

            // Decoding list info
            List list = list_serializer.decode(bytes);
            int i = 0;
            for (String post : list.getPosts()) {
                System.out.println(i++ + "-> " + post);
            }
        }, e);

        // Creating the message and serializing it
        byte[] data = s.encode("");
        // Sending the message to every client but itself
        messagingService.sendAsync(servidor, "GET_ADDR", data);

    }

    @Override
    public void run() {

        for (int i=0; i<no_posts; i++) {

            StringBuffer text = new StringBuffer("B" + id + "P" + i);
            ArrayList<String> topics = new ArrayList<>();
            Random r = new Random();
            for (int j=0; j<no_topics; j++) topics.add("#1" );

            //for (int j=0; j<no_topics; j++) topics.add(" #" + j);

            Post post = new Post(text.toString(), topics, "Bot_" + id, clock.increment());
            byte[] data = post_serializer.encode(post);

            System.out.println(servidor.toString());

            messagingService.sendAsync(servidor, "POST", data);

        }

    }

    public static void main(String[] args) throws Exception { // args[0] Porta Cliente args[1] porta loadbalancer args[2] Bot ID args[3] No posts / bot args[4] No topics / post
        try{
            // Getting own port from comm   and line
            int port = Integer.parseInt(args[0]);

            // Getting load balancer port from command line
            int load_balancer = Integer.parseInt(args[1]);

            // Getting spammer settings
            int no_bots = Integer.parseInt(args[2]);
            int no_posts_per_bot = Integer.parseInt(args[3]);
            int no_topics_per_post = Integer.parseInt(args[4]);

            for (int i=0; i<no_bots; i++) new TwitterBot(Address.from(port + i), Address.from(load_balancer), i, no_posts_per_bot, no_topics_per_post);

        }catch (Exception e){
            e.printStackTrace();
        }

    }
}
