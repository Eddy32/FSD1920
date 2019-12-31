package Protos;

public class Update {

    private String text;
    private String category;
    private int index;

    public Update(String text, String category, int index) {
        this.text = text;
        this.category = category;
        this.index = index;
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

    public void setText(String text) {
        this.text = text;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public void setIndex(int index) {
        this.index = index;
    }

}
