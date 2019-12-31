

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

    public synchronized Pair<Integer,Integer> addPost(String post){
        if(this.index == 10){
            this.volta++;
            this.index=0;
        }


        if(this.posts.size() > this.index){
            this.posts.remove(this.index);
        }
        this.posts.add(this.index,post);

        Pair<Integer,Integer> pair = new Pair(this.index++,this.volta);
        return pair;

    }



    public static void main(String args[]){
        ListPosts teste = new ListPosts();
        teste.addPost("1");
        teste.addPost("2");
        teste.addPost("3");
        teste.addPost("4");
        teste.addPost("5");
        teste.addPost("6");
        teste.addPost("7");
        teste.addPost("8");
        teste.addPost("9");
        teste.addPost("10");
        teste.addPost("11");
        teste.addPost("12");
        teste.addPost("13");
        teste.addPost("14");
        teste.addPost("15");
        teste.addPost("16");
        teste.addPost("17");
        teste.addPost("18");
        teste.addPost("19");
        teste.addPost("20");
        teste.addPost("21");


    }

}
