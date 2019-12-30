package proto;

import java.util.ArrayList;

public class Broadcast {

    private String text;
    private ArrayList<String> categories;
    private int index;
    private int iterator;

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

    public int getIterator() {
        return iterator;
    }

    public void setIterator(int iterator) {
        this.iterator = iterator;
    }

    public Broadcast(String text, ArrayList<String> categories, int index, int iterator) {
        this.text = text;
        this.categories = categories;
        this.index = index;
        this.iterator = iterator;
    }
}
