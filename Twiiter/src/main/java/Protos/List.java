package Protos;

import java.util.ArrayList;

public class List {

    private ArrayList<String> posts;

    public List(java.util.List<String> posts) { this.posts = new ArrayList<>(posts); }

    public java.util.List<String> getPosts() {
        return (java.util.List<String>) posts;
    }

    public void setPosts(ArrayList<String> posts) {
        this.posts = posts;
    }
}
