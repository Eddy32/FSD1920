import io.atomix.cluster.messaging.ManagedMessagingService;
import io.atomix.cluster.messaging.impl.NettyMessagingService;
import io.atomix.storage.journal.SegmentedJournal;
import io.atomix.storage.journal.SegmentedJournalWriter;
import io.atomix.utils.net.Address;
import io.atomix.utils.serializer.Serializer;
import io.atomix.utils.serializer.SerializerBuilder;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;


public class TwoPhaseCommit {

    private static class Coordinator {

        private Address address;
        private List<Address> addresses;

        public static class Message {

            public enum Type {REQUEST, PREPARED, ABORT, OK, COMMIT};

            public Type type;
            public Integer participant_id;
            public List<Integer> participants;

            public Message(Type type, Integer participant_id, List<Integer> participants) {

                this.type = type;
                this.participant_id = participant_id;
                this.participants = participants;

            }
        }

        public Coordinator (Address address, List<Address> addresses) {

            this.address = address;
            this.addresses = addresses;

            ExecutorService e = Executors.newFixedThreadPool(1);

            // Starting messaging service
            ManagedMessagingService messagingService = new NettyMessagingService.Builder()
                    .withName("Coordinator at " + address.toString())
                    .withReturnAddress(address).build();

            messagingService.start();

            // Creating serializer for encoding and decoding to/from bytes
            Serializer s = new SerializerBuilder().addType(Message.class).build();

            SegmentedJournal<String> segmentedJournal = SegmentedJournal.<String>builder()
                    .withName("coordinator_log.txt")
                    .withSerializer(s)
                    .build();

            // Restoring log
            /*SegmentedJournalReader<String> reader = segmentedJournal.openReader(0);
            while(reader.hasNext()) {

                Indexed<String> element = reader.next();
                System.out.println(element.index() + ": " + element.entry());

            }
            reader.close();*/

            // Opening log for writing
            SegmentedJournalWriter<String> writer = segmentedJournal.writer();

            final List<Integer> curr_part = new ArrayList<>();
            final List<Integer> all_part = new ArrayList<>();

            // Handling partipants' messages
            messagingService.registerHandler("MSG", (addr,bytes)-> {

                // Decoding data received
                Message msg_received = s.decode(bytes);

                switch (msg_received.type) {

                    case REQUEST:

                        if (all_part.containsAll(msg_received.participants)) curr_part.add(msg_received.participant_id);
                        else {

                            all_part.clear();
                            all_part.addAll(msg_received.participants);

                            curr_part.clear();
                            curr_part.add(msg_received.participant_id);

                        }

                        if (curr_part.containsAll(all_part))
                            all_part.forEach(part_id ->
                                    messagingService.sendAsync(addr, "MSG",
                                            s.encode(new Message(Message.Type.PREPARED, part_id, all_part))));

                        break;

                    case OK:

                        break;

                    case ABORT:

                        break;

                }

            }, e);

        }

    }

    private static class Participant {

        public Participant (Address ownAddress, Address coordAddress, List<Address> addresses) {



        }

    }

    public static void main(String[] args) {

        // Getting an address for the Coordinator from the command line
        Address coordAddress = Address.from(Integer.parseInt(args[1]));

        // Getting the addresses for the Participants
        List<Address> partAddresses = new ArrayList<>();
        for (int i=0; i < args.length-2; i++) partAddresses.add(Address.from(Integer.parseInt(args[i+2])));

        // Getting the role to play
        int role = Integer.parseInt(args[0]);
        switch (role) {

            case 0: // Coordinator

                new Coordinator(coordAddress, partAddresses);
                break;

            default: // Participant 0

                Participant participant = new Participant(partAddresses.get(role-1), coordAddress, partAddresses);

                if (role == 1) {



                } else if (role == 2) {


                }

                break;
        }

    }
}
