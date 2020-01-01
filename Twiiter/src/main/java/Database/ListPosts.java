package Database;

import java.util.ArrayList;

public class ListPosts {
    private ArrayList<Post> posts;
    private int index;

    public ListPosts(){
        this.posts = new ArrayList<>();
        this.index = 1;
    }

    public ListPosts(ArrayList<Post> posts) {
        this.posts = posts;
    }

    public int getIndexNoIncrement(){
        return this.index;
    }

    public ArrayList<Post> getPosts(){
        return this.posts;
    }

    public synchronized void addPost(String post,int logicCounter, int index){

        if(this.posts.size() > index%10){
            this.posts.remove(index%10);
        }
        Post p = new Post(post,logicCounter);
        this.posts.add(index%10,p);


    }



    public synchronized int getIndex(){
        return this.index++;
    }




}
