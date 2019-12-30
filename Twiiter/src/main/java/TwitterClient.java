import io.atomix.cluster.messaging.ManagedMessagingService;
import io.atomix.cluster.messaging.MessagingService;
import io.atomix.cluster.messaging.impl.NettyMessagingService;
import io.atomix.utils.net.Address;
import io.atomix.utils.serializer.Serializer;
import io.atomix.utils.serializer.SerializerBuilder;
import org.apache.commons.math3.analysis.function.Add;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.IntStream;

public class TwitterClient {

    private Address address;
    private MessagingService messagingService;

    public TwitterClient(Address address, Address servidor ) throws Exception {

        this.address = address;
        ExecutorService e = Executors.newFixedThreadPool(1);

        // Starting messaging service

        ManagedMessagingService messagingService = new NettyMessagingService.Builder()
                .withName("Chat_" + address.toString())
                .withReturnAddress(address).build();

        messagingService.start();

        // Creating serializer for encoding and decoding to/from bytes
        Serializer s = new SerializerBuilder().addType(Address.class).build();

        // Register Handler for when a message is received
        messagingService.registerHandler("ADDRESS", (addr,bytes)-> {

            // Decoding data received
            Address msg = s.decode(bytes);

            // Printing it
            System.out.println("Ip servidor = " + msg.toString());
            try {
                startCliente(msg); // msg = address
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        }, e);


        // Sending a message when input is received
        BufferedReader in = new BufferedReader(new InputStreamReader(System.in));
        String msg = "";

         // Creating the message and serializing it
         byte[] data = s.encode(msg);
         // Sending the message to every client but itself
         messagingService.sendAsync(servidor, "GET_ADDR", data);

        }

        public void startCliente(Address address) throws IOException {
            BufferedReader in = new BufferedReader(new InputStreamReader(System.in));
            String msg;
            int tipo_menu = 0;
            int resultado = 0;

            print_menu();

            while (!(msg = in.readLine()).toUpperCase().equals("EXIT")) {

                try{
                    resultado = Integer.parseInt(msg);
                    switch (resultado){
                        case 0:
                            print_menu();
                            break;
                        case 1:
                            fazer_post(in, address);
                            break;/*
                        case 2:
                            deal_subscriptions(in);
                            break;
                        case 3:
                            pedir_posts(in, address);
                            break;*/
                        default:
                            System.out.printf("Opcao invalida");
                    }
                    //tipo_menu = resultado;

                }catch (Exception e){
                    System.out.printf("Valor não é um inteiro\n");
                }


            }

        }

        public void fazer_post(BufferedReader in, Address address){
            System.out.println("Escreva o post:");
            try {
                String mensagem = in.readLine();
                String[] split = mensagem.split("#[a-zA-Z_]*");
                System.out.printf( split[1]) ;
                for ( String cena : split)
                {
                    System.out.printf(cena);

                }

            } catch (IOException e) {
                e.printStackTrace();
            }


        }

        public void print_menu(){
            System.out.println("#####################################################");
            System.out.println("# 1 - Fazer um Post                                 #");
            System.out.println("# 2 - Adicionar/Remover subscricoes                 #");
            System.out.println("# 3 - Pedir 10 posts mais recentes das subscricoes  #");
            System.out.println("#####################################################");
        }

    public static void main(String[] args) throws Exception {

        // Getting port from command line
        int port =  10001; // Integer.parseInt(args[0]);

        // Making an Address based on the command line arguments
        Address address = Address.from(port);

        new TwitterClient(address, Address.from(10000) );

        //vectorClockTest();
    }
}
