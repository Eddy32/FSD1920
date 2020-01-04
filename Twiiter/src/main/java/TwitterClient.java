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
import java.util.ArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class TwitterClient {

    private Address address;
    private Address servidor;
    private ManagedMessagingService messagingService;
    private ExecutorService e;
    private ArrayList<String> categories;
    private Serializer post_serializer = new SerializerBuilder().addType(Post.class).build();
    private Serializer get_serializer = new SerializerBuilder().addType(Get.class).build();
    private Serializer list_serializer = new SerializerBuilder().addType(List.class).build();
    private Clock counter;
    private String username;
    private Log log;

    public TwitterClient(Address address, Address servidor) throws Exception {

        this.address = address;
        this.e = Executors.newFixedThreadPool(5);
        this.categories = new ArrayList<>();
        this.counter = new Clock();
        this.username = "Anonymous";
        this.log = new Log("log" + this.username);


        this.messagingService = new NettyMessagingService.Builder()
                .withName("Twitter_Client_" + address.toString())
                .withReturnAddress(address).build();

        messagingService.start();

        // Creating serializer for encoding and decoding to/from bytes
        Serializer s = new SerializerBuilder().addType(Address.class).build();
        Serializer ackSerializer = new SerializerBuilder().build();


        messagingService.registerHandler("ACK", (addr,bytes)-> {

            int post = ackSerializer.decode(bytes);
            this.log.confirmAction(post);

        }, e);

        // Register Handler for when a message is received
        messagingService.registerHandler("ADDRESS", (addr, bytes) -> {

            // Decoding data received
            Address msg = s.decode(bytes);
            this.servidor = msg;

            // Printing it
            System.out.println("Server = " + msg.toString());
            try {
                startCliente(); // msg = address
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        }, e);

        // Handlers
        messagingService.registerHandler("LIST", (addr, bytes) -> {
            System.out.println("Recebi LIST");

            // Decoding list info
            Protos.List list = list_serializer.decode(bytes);
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

    public void startCliente() throws IOException {



        BufferedReader in = new BufferedReader(new InputStreamReader(System.in));
        String msg;
        int resultado = 0;


        while (true) {
            print_menu();
            if ((msg = in.readLine()).toUpperCase().equals("EXIT")) break;
            try {
                resultado = Integer.parseInt(msg);
                switch (resultado) {
                    case 1:
                        send_post(in);
                        break;
                    case 2:
                        deal_subscriptions(in);
                        break;
                    case 3:
                        list_posts();
                        break;
                    case 4:
                        get_username(in);
                        break;
                    default:
                        System.out.println("Opcao invalida");
                }
                //tipo_menu = resultado;

            } catch (Exception e) {
                System.out.println("Formato invalido");
                ;
            }

        }
    }


    private void get_username(BufferedReader in) {

        System.out.println("Escreva o nome de utilizador: ");
        try {
            String utlizador = in.readLine();
            this.log = new Log("log" + this.username);
            this.username = utlizador;
            this.counter = new Clock();

        } catch (IOException e) {
            System.out.println("Formato invalido");
        }
    }

    private void deal_subscriptions(BufferedReader in) {
        String msg;
        int lido;


        while (true) {
            try {
                print_deal_subscriptions();
                msg = in.readLine();
                lido = Integer.parseInt(msg);
                if (lido == 4) break;
                switch (lido) {
                    case 1:
                        System.out.println("Inserir Categoria");
                        msg = in.readLine();
                        categories.add("#" + msg);
                        break;
                    case 2:
                        System.out.println("Categoria a remover");
                        msg = in.readLine();
                        if (categories.contains("#" + msg)) {
                            categories.remove(categories.indexOf("#" + msg));
                        } else {
                            System.out.println("Categoria não subscrita");
                            System.out.println("Categorias subscritas são");
                            print_categories();
                        }
                        break;
                    case 3:
                        print_categories();
                }

            } catch (IOException ex) {
                System.out.println("Por favor utilizador formato valido");
            }

        }
    }

    private void print_deal_subscriptions() {
        System.out.println("#####################################################");
        System.out.println("# 1 - Adicionar subscricao                          #");
        System.out.println("# 2 - Remover subscricoes                           #");
        System.out.println("# 3 - Listar subscricoes                            #");
        System.out.println("# 4 - Voltar menu inicial                           #");
        System.out.println("#####################################################");
    }

    private void print_categories() {
        System.out.println(categories.toString());
    }

    public void send_post(BufferedReader in ){
            System.out.println("Escreva o post:");
            try {
                String mensagem = in.readLine();
                System.out.println("Categorias");
                ArrayList<String> arrayList = new ArrayList<String>();
                Matcher m = Pattern.compile("#[-_'a-zA-ZÀ-ÖØ-öø-ÿ0-9]*")
                        .matcher(mensagem);
                while (m.find()) {
                    arrayList.add(m.group());
                }
                for (String cena : arrayList)
                    System.out.println(1);
                if( arrayList.size() < 1){
                    System.out.println("Necessário pelo menos 1 categoria");

                }else{
                    int id = counter.increment();
                    Post post = new Post(mensagem, arrayList, username, id );
                    byte[] data = post_serializer.encode(post);
                    System.out.println(servidor.toString());
                    System.out.println("->" + post.toString());
                    this.log.writeLog("POST " + post.toString() + " " + servidor.port(),id);
                    messagingService.sendAsync(servidor, "POST", data);
            }

        } catch (IOException e) {
            System.out.println("Formato invalido");
        }

    }

    public void list_posts() {
        if (categories.size() == 0) {
            System.out.println("Sem categorias selecionadas");
        } else {
            Get get = new Get(categories, counter.increment());

            byte[] data = get_serializer.encode(get);
            System.out.println("Esperar pela resposta do servidor");
            messagingService.sendAsync(servidor, "GET", data);

        }
    }

    public void print_menu() {
        System.out.println("#####################################################");
        System.out.println("# 1 - Fazer um Post                                 #");
        System.out.println("# 2 - Adicionar/Remover subscricoes                 #");
        System.out.println("# 3 - Pedir 10 posts mais recentes das subscricoes  #");
        System.out.println("# 4 - Mudar de utilizador                           #");
        System.out.println("#####################################################");
    }

    public static void main(String[] args) throws Exception { // args[0] Porta Cliente args[1] porta loadbalancer

        // Getting own port from command line
        int port = Integer.parseInt(args[0]);

        // Getting load balancer port from command line
        int load_balancer = Integer.parseInt(args[1]);

        new TwitterClient(Address.from(port), Address.from(load_balancer));

    }
}
