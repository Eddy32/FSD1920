

import java.util.ArrayList;

public class ListPosts {
    private ArrayList<String> posts;
    private int volta;
    private int index;

    public ListPosts(){
        this.posts = new ArrayList<>();
        this.volta = 0;
        this.index = 0;
    }

    public ListPosts(ArrayList<String> posts) {
        this.posts = posts;
    }

    public int getVolta(){
        return this.volta;
    }

    public ArrayList<String> getPosts(){
        return this.posts;
    }

    public synchronized void addPost(String post,int index){

        if(this.posts.size() > index){
            this.posts.remove(index);
        }
        this.posts.add(index,post);


    }



    public synchronized Pair<Integer,Integer> getIndexVoltas(){
        Pair<Integer,Integer> pair;
        if(this.index == 10){
            this.volta++;
            this.index=0;
            pair = new Pair(this.index,this.volta);
            return pair;
        }
        else{
            pair = new Pair(this.index++,this.volta);
            return pair;
        }

    }



    public static void main(String args[]){
        ListPosts teste = new ListPosts();
        teste.addPost("1",teste.getIndexVoltas().getLeft());
        teste.addPost("2",teste.getIndexVoltas().getLeft());
        teste.addPost("3",teste.getIndexVoltas().getLeft());
        teste.addPost("4",teste.getIndexVoltas().getLeft());
        teste.addPost("5",teste.getIndexVoltas().getLeft());
        teste.addPost("6",teste.getIndexVoltas().getLeft());
        teste.addPost("7",teste.getIndexVoltas().getLeft());
        teste.addPost("8",teste.getIndexVoltas().getLeft());
        teste.addPost("9",teste.getIndexVoltas().getLeft());
        teste.addPost("10",teste.getIndexVoltas().getLeft());
        teste.addPost("11",teste.getIndexVoltas().getLeft());
        teste.addPost("12",teste.getIndexVoltas().getLeft());
        teste.addPost("13",teste.getIndexVoltas().getLeft());
        teste.addPost("14",teste.getIndexVoltas().getLeft());
        teste.addPost("15",teste.getIndexVoltas().getLeft());
        teste.addPost("16",teste.getIndexVoltas().getLeft());
        teste.addPost("17",teste.getIndexVoltas().getLeft());
        teste.addPost("18",teste.getIndexVoltas().getLeft());
        teste.addPost("19",teste.getIndexVoltas().getLeft());
        teste.addPost("20",teste.getIndexVoltas().getLeft());
        teste.addPost("21",teste.getIndexVoltas().getLeft());
        teste.addPost("22",teste.getIndexVoltas().getLeft());


    }

}
