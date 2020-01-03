package Protos;

import java.util.ArrayList;

public class Post {

    private String text;
    private ArrayList<String> categories;
    private int id;
    private String owner;
    private int ownerClock;

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    public ArrayList<String> getCategories() {
        return categories;
    }

    public void setCategories(ArrayList<String> categories) {
        this.categories = categories;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getOwner() { return owner; }

    public String toString(){
        String fin = this.text + '.' + this.id + '.' + this.owner + '.' +this.ownerClock +'.';
        for(String s: categories){
            fin = fin + s;
            fin = fin + '-';
        }
        return fin;
    }

    public void setOwnerClock(int ownerClock) {
        this.ownerClock = ownerClock;
    }

    public static Post buildUpdate(String post){
        String[] args,categorias;
        args = post.split("\\.+");
        ArrayList<String> cats = new ArrayList<String>();
        categorias = args[4].split("\\-+");

        for(String s: categorias){
            cats.add(s);
        }
        Post p = new Post(args[0],cats,args[2],Integer.parseInt(args[1]));
        p.setOwnerClock(Integer.parseInt(args[3]));
        return p;
    }

    public Post(String text, ArrayList<String> categories, String owner, int id) {
        this.text = text;
        this.categories = categories;
        this.id = id;
        this.owner = owner;
    }
}
