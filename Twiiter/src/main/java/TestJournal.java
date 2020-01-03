
import io.atomix.storage.journal.Indexed;
import io.atomix.storage.journal.SegmentedJournal;
import io.atomix.storage.journal.SegmentedJournalReader;
import io.atomix.storage.journal.SegmentedJournalWriter;
import io.atomix.utils.serializer.Serializer;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.concurrent.CompletableFuture;

public class TestJournal {
    private String logName;
    private Serializer s;
    private SegmentedJournal<String> sj;
    private SegmentedJournalReader<String> r;
    private SegmentedJournalWriter<String> w;
    private HashMap<Integer,ArrayList<Long>> lines;


    public TestJournal(String logName){
        this.logName = logName;
        this.s = Serializer.builder()
                .build();
        this.sj = SegmentedJournal.<String>builder()
                .withName(logName)
                .withSerializer(s)
                .build();
        this.r = sj.openReader(0);
        this.w = sj.writer();
        this.lines = new HashMap<Integer,ArrayList<Long>>();
    }


    public ArrayList<String> readLog(int key){


        ArrayList<String> log = new ArrayList<>();


        //while(this.r.hasNext())

        for(Long i: this.lines.get(key) ){
            this.r = sj.openReader(i);
            Indexed<String> e = r.next();
            log.add(e.entry());
            System.out.println(e.index()+": "+e.entry() + ".");
        }

        this.r.close();

        return log;

    }

    public  void resetLog(){
        Serializer s = Serializer.builder()
                .build();

        SegmentedJournal<String> sj = SegmentedJournal.<String>builder()
                .withName(logName)
                .withSerializer(s)
                .build();

        SegmentedJournalWriter<String> w = sj.writer();
        w.reset(0);

    }

    public void writeLog(ArrayList<String> info, int key){

        if(!this.lines.containsKey(key)){
             ArrayList<Long> line = new ArrayList<Long>();
             this.lines.put(key,line);
        }

        for(String data: info){
            this.lines.get(key).add(w.getNextIndex());
            this.w.append(key + " " + data);

        }

    }

    public void addLines(int key, long index){
        if(!this.lines.containsKey(key)){
            ArrayList<Long> line = new ArrayList<Long>();
            this.lines.put(key,line);
        }
        this.lines.get(key).add(index);
    }

    public void resetJournal(){
        String[] splited; //= str.split("\\s+");
        while(this.r.hasNext()) {
            Indexed<String> e = r.next();
            System.out.print("LI: " + e.entry() +" em " + e.index());
            splited = e.entry().split("\\s+");
            System.out.println("");
            if(splited[1].equals("CONFIRM")){
                System.out.println("verifiquei " + splited[0] + "-> " +splited[1]);
                resetconfirmAction(Integer.parseInt(splited[0]));
            }
            else {
                System.out.println();
                addLines(Integer.parseInt(splited[0]),e.index());
            }

        }
    }

    public void resetconfirmAction(int key){

        this.lines.get(key).remove(0);

    }

    /*   public ArrayList<String> getInstructions(){
        ArrayList<String> inst = new ArrayList<String>();

        for(Long i: this.lines.get(key) ){
            this.r = sj.openReader(i);
            Indexed<String> e = r.next();
            log.add(e.entry());
            System.out.println(e.index()+": "+e.entry() + ".");
        }


    }*/

    public void confirmAction(int key){

        this.w.append(key + " " + "CONFIRM");
        this.lines.get(key).remove(0);

    }

    public static void main(String[] args) throws Exception {

        TestJournal tj = new TestJournal("teste");

        ArrayList<String> data = new ArrayList<String>();
        data.add("Hola");
        data.add("soy");
        data.add("Eddy");

        ArrayList<String> data2 = new ArrayList<String>();
        data2.add("test");
        data2.add("kapa");
        data2.add("son");

        //tj.writeLog(data,1);
        //tj.writeLog(data2,2);
        tj.resetJournal();
        System.out.println("VOu ler:");
        tj.readLog(1);
        tj.readLog(2);
        System.out.println("------------");
        //tj.confirmAction(1);
        //tj.confirmAction(2);
        //tj.confirmAction(1);
        System.out.println("------------");

       // tj.readLog(1);

        Serializer s = Serializer.builder()
                .build();

        SegmentedJournal<String> sj = SegmentedJournal.<String>builder()
                .withName("teste")
                .withSerializer(s)
                .build();


        SegmentedJournalReader<String> r = sj.openReader(0);
        while(r.hasNext()) {
            Indexed<String> e = r.next();

            System.out.println(e.index()+": "+e.entry() + ".");

        }
        //tj.reset(1);
/*
        System.out.println("apaguei o log");


        Serializer s = Serializer.builder()
                .build();

        SegmentedJournal<String> sj = SegmentedJournal.<String>builder()
                .withName("aa")
                .withSerializer(s)
                .build();


        // Funcionamento normal

        SegmentedJournalWriter<String> w = sj.writer();
        w.append("ola");
        w.append("Enviei do X para o Y");
        w.append("SAME");
        w.append("teste");



        r.close();

        w.truncate(2);

        CompletableFuture.supplyAsync(()->{w.flush();return null;})
                .thenRun(()->{
                    w.close();
                });
        // Arranque




        SegmentedJournalReader<String> q = sj.openReader(0);
        while(q.hasNext()) {
            Indexed<String> e = q.next();

            System.out.println(e.index()+": "+e.entry());

        }
        q.close();

 */


    }
}
