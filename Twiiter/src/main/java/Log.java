
import Database.DataBase;
import Database.Pair;
import Protos.Post;
import Protos.TryUpdate;
import Protos.Update;
import io.atomix.storage.journal.Indexed;
import io.atomix.storage.journal.SegmentedJournal;
import io.atomix.storage.journal.SegmentedJournalReader;
import io.atomix.storage.journal.SegmentedJournalWriter;
import io.atomix.utils.serializer.Serializer;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.concurrent.ConcurrentHashMap;

import static Protos.Update.buildUpdate;

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

    public DataBase readDB(){
        DataBase db = new DataBase();
        while(this.r.hasNext()) {
            Indexed<String> e = r.next();
            Update update = buildUpdate(e.entry());
            String post_text = update.getText();
            String post_topic = update.getCategory();
            int topicIndex = update.getIndex();
            db.addPost(post_topic, post_text, topicIndex);
        }
        return  db;
    }

    public ArrayList<Pair<Update,Integer> > readBR(){
        ArrayList<Pair <Update, Integer> > array = new ArrayList<>();
        HashMap <Integer, String > linhas = new HashMap<>();

        while(this.r.hasNext()) {
            Indexed<String> e = r.next();
            String aux = e.entry();
            String[] aux2 = aux.split(" ");
            if(aux2[1].equals("CONFIRM")){
                linhas.remove(Integer.parseInt(aux2[0]));
            }
            else {
                linhas.put(Integer.parseInt(aux2[0]), aux );
            }
        }
        for (String linha : linhas.values() ){
            String[] aux3 = linha.split(" ");
            array.add( new Pair<Update, Integer>(Update.buildUpdate(aux3[2]),Integer.parseInt(aux3[3])));
        }
        return array;
    }

    public ArrayList<Pair<TryUpdate,Integer> > readTU(){
        ArrayList<Pair <TryUpdate, Integer> > array = new ArrayList<>();
        HashMap <Integer, String > linhas = new HashMap<>();

        while(this.r.hasNext()) {
            Indexed<String> e = r.next();
            String aux = e.entry();
            String[] aux2 = aux.split(" ");
            if(aux2[1].equals("CONFIRM")){
                linhas.remove(Integer.parseInt(aux2[0]));
            }
            else {
                linhas.put(Integer.parseInt(aux2[0]), aux );
            }
        }
        for (String linha : linhas.values() ){
            String[] aux3 = linha.split(" ");
            array.add( new Pair<TryUpdate, Integer>(TryUpdate.buildTryUpdate(aux3[2]),Integer.parseInt(aux3[3])));
        }

        return array;
    }

    public ArrayList<Pair<Update,Integer> > readFC(){
        ArrayList<Pair <Update, Integer> > array = new ArrayList<>();
        HashMap <Integer, String > linhas = new HashMap<>();

        while(this.r.hasNext()) {
            Indexed<String> e = r.next();
            String aux = e.entry();
            String[] aux2 = aux.split(" ");
            if(aux2[1].equals("CONFIRM")){
                linhas.remove(Integer.parseInt(aux2[0]));
            }
            else {
                linhas.put(Integer.parseInt(aux2[0]), aux );
            }
            for (String linha : linhas.values() ){
                String[] aux3 = linha.split(" ");
                array.add( new Pair<Update, Integer>(Update.buildUpdate(aux3[2]),Integer.parseInt(aux3[3])));
            }


        }
        return array;
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

    public synchronized void writeDB(String info){

        this.w.append(info);

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



    }
}

