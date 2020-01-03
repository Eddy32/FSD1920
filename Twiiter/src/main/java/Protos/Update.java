package Protos;

public class Update {

    private String text;
    private String category;
    private int index;
    private int key;
    private String username;
    private int userClock;

    public Update(String text, String category, int index, String username, int userClock) {
        this.text = text;
        this.category = category;
        this.index = index;
        this.key = 0;
        this.username = username;
        this.userClock = userClock;
    }

    public int getUserClock() {return userClock;}

    public String getUsername() { return username; }

    public String getText() { return text; }

    public String getCategory() {
        return category;
    }

    public int getIndex() {
        return index;
    }

    public void setText(String text) {
        this.text = text;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public void setIndex(int index) {
        this.index = index;
    }

    public int getKey() {
        return key;
    }

    public void setKey(int key) {
        this.key = key;
    }

    public String toString(){
        return (this.text + '.' + this.category + '.' + this.index + '.' + this.key + '.' + this.username + '.' + this.userClock);
    }

    public static Update buildUpdate(String update){
        String[] args;
        args = update.split("\\.+");
        Update done = new Update(args[0],args[1],Integer.parseInt(args[2]),args[4],Integer.parseInt(args[5]));
        done.setKey(Integer.parseInt(args[3]));
        return done;
    }
}
