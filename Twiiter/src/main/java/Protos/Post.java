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

    public Post(String text, ArrayList<String> categories, String owner, int id) {
        this.text = text;
        this.categories = categories;
        this.id = id;
        this.owner = owner;
    }
}
