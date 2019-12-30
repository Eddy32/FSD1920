package Protos;

import java.util.ArrayList;

public class List {

    private ArrayList<String> posts;

    public List(ArrayList<String> posts) {
        this.posts = posts;
    }

    public ArrayList<String> getPosts() {
        return posts;
    }

    public void setPosts(ArrayList<String> posts) {
        this.posts = posts;
    }
}
