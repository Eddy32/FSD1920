package Protos;

public class Update {

    private String text;
    private String category;
    private int index;
    private int global_clock;
    private int new_clock;

    public String getText() {
        return text;
    }
    public void setNew_clock(int a){ this.new_clock = a ;}
    public int getNew_clock(){ return this.new_clock ;}
    public void setText(String text) {
        this.text = text;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public int getIndex() {
        return index;
    }

    public void setIndex(int index) {
        this.index = index;
    }

    public int getGlobal_clock() {
        return global_clock;
    }

    public void setGlobal_clock(int global_clock) {
        this.global_clock = global_clock;
    }

    public Update(String text, String category, int index, int global_clock) {
        this.text = text;
        this.category = category;
        this.index = index;
        this.global_clock = global_clock;
    }

    public String toString(){
        return (this.text + ';' + this.category + ';' + this.index + ';' + this.global_clock);
    }

    public static Update buildUpdate(String update){
        String[] args;
        args = update.split(";+");
        return new Update(args[0],args[1],Integer.parseInt(args[2]),Integer.parseInt(args[3]));
    }
}
