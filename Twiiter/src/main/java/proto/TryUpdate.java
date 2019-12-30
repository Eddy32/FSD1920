package proto;

import java.util.ArrayList;

public class TryUpdate {


    private String            text;
    private ArrayList<String> categories;
    private int               index;

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

    public int getIndex() {
        return index;
    }

    public void setIndex(int index) {
        this.index = index;
    }

    public TryUpdate(String text, ArrayList<String> categories, int index) {
        this.text = text;
        this.categories = categories;
        this.index = index;
    }
}
