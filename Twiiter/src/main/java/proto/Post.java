package proto;

import java.util.ArrayList;

public class Post {

    private String            text;
    private ArrayList<String> categories;

    public Post(String text, ArrayList<String> categories) {
        this.text = text;
        this.categories = categories;
    }

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
}
