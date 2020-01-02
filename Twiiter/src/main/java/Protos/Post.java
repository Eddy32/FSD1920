package Protos;

import java.util.ArrayList;

public class Post {

    private String            text;
    private ArrayList<String> categories;
    private int id;

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

    public Post(String text, ArrayList<String> categories, int id) {
        this.text = text;
        this.categories = categories;
        this.id = id;
    }
}
