

import java.util.ArrayList;

public class ListPosts {
    private ArrayList<String> posts;
    private int index;

    public ListPosts(){
        this.posts = new ArrayList<>();
        this.index = 1;
    }

    public ListPosts(ArrayList<String> posts) {
        this.posts = posts;
    }


    public ArrayList<String> getPosts(){
        return this.posts;
    }

    public synchronized void addPost(String post,int index){

        if(this.posts.size() > index%10){
            this.posts.remove(index%10);
        }
        this.posts.add(index%10,post);


    }



    public synchronized int getIndex(){
        return this.index++;
    }




}
