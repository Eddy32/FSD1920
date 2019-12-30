package Protos;

public class Update {

    private String text;
    private String category;
    private int index;
    private int iterator;

    public Update(String text, String category, int index, int iterator) {
        this.text = text;
        this.category = category;
        this.index = index;
        this.iterator = iterator;
    }

    public String getText() {
        return text;
    }

    public String getCategory() {
        return category;
    }

    public int getIndex() {
        return index;
    }

    public int getIterator() {
        return iterator;
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

    public void setIterator(int iterator) {
        this.iterator = iterator;
    }

}
