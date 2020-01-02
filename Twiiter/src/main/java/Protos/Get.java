package Protos;

import java.util.ArrayList;

public class Get {

    private ArrayList<String> categories;
    private int id;

    public Get(ArrayList<String> categories, int id) {
        this.categories = categories;
        this.id = id;
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
}
