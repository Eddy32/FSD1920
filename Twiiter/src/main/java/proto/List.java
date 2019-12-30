package proto;

import java.util.ArrayList;

public class List {

    private ArrayList<String> topics;

    public List(ArrayList<String> topics) {
        this.topics = topics;
    }

    public ArrayList<String> getTopics() {
        return topics;
    }

    public void setTopics(ArrayList<String> topics) {
        this.topics = topics;
    }
}
