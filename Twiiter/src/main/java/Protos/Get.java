package Protos;

import java.util.ArrayList;

public class Get {

    private ArrayList<String> categories;

    public Get(ArrayList<String> categories) {
        this.categories = categories;
    }

    public ArrayList<String> getCategories() {
        return categories;
    }

    public void setCategories(ArrayList<String> categories) {
        this.categories = categories;
    }

}
