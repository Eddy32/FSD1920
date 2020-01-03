
import io.atomix.storage.journal.Indexed;
import io.atomix.storage.journal.SegmentedJournal;
import io.atomix.storage.journal.SegmentedJournalReader;
import io.atomix.storage.journal.SegmentedJournalWriter;
import io.atomix.utils.serializer.Serializer;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.concurrent.ConcurrentHashMap;

public class Log {
    private String logName;
    private Serializer s;
    private SegmentedJournal<String> sj;
    private SegmentedJournalReader<String> r;
    private SegmentedJournalWriter<String> w;
    private ConcurrentHashMap<Integer,ArrayList<Long>> lines;


    public Log(String logName){
        this.logName = logName;
        this.s = Serializer.builder()
                .build();
        this.sj = SegmentedJournal.<String>builder()
                .withName(logName)
                .withSerializer(s)
                .build();
        this.r = sj.openReader(0);
        this.w = sj.writer();
        this.lines = new ConcurrentHashMap<Integer,ArrayList<Long>>();
    }

    public ArrayList<String> getInstructions(){
        ArrayList<String> inst = new ArrayList<String>();
        for(ArrayList<Long> arrayL: this.lines.values()){
            for(Long l: arrayL){
                this.r = sj.openReader(l);
                Indexed<String> e = r.next();
                inst.add(e.entry());
            }
        }

        return inst;
    }


    public synchronized ArrayList<String> readLog(int key){


        ArrayList<String> log = new ArrayList<>();


        //while(this.r.hasNext())

        for(Long i: this.lines.get(key) ){
            this.r = sj.openReader(i);
            Indexed<String> e = r.next();
            log.add(e.entry());
            System.out.println("ENTRY NO LOG:"+e.entry());
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

    public synchronized void writeLog(String info, int key){

        if(!this.lines.containsKey(key)){
             ArrayList<Long> line = new ArrayList<Long>();
             this.lines.put(key,line);
        }


            this.lines.get(key).add(w.getNextIndex());
            this.w.append(key + " " + info);



    }

    public void addLines(int key, long index){
        if(!this.lines.containsKey(key)){
            ArrayList<Long> line = new ArrayList<Long>();
            this.lines.put(key,line);
        }
        this.lines.get(key).add(index);
    }

    public synchronized void resetJournal(){
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



    public synchronized void confirmAction(int key){
        this.w.append(key + " " + "CONFIRM");
        this.lines.get(key).remove(0);

    }



    public static void main(String[] args) throws Exception {

        Log tj = new Log("teste");

        ArrayList<String> data = new ArrayList<String>();
        tj.writeLog("Hola",1);
        tj.writeLog("soy",2);
        tj.writeLog("Eddy",1);

        ArrayList<String> data2 = new ArrayList<String>();
        tj.writeLog("test",2);
        tj.writeLog("kapa",2);
        tj.writeLog("son",1);

        //tj.writeLog(data,1);
        //tj.writeLog(data2,2);
        //tj.resetJournal();
        System.out.println("VOu ler:");
        tj.readLog(1);
        tj.readLog(2);
        System.out.println("------------");
        tj.confirmAction(1);
        tj.confirmAction(2);
        tj.confirmAction(1);

        tj.readLog(1);
        tj.readLog(2);

        System.out.println("------------");
        ArrayList<String> teste = tj.getInstructions();
        for(String s : teste)
            System.out.println("-> " + s);

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
    }
}
