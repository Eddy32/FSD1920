package Database;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class DataBase {
    private HashMap<String, ListPosts> posts;
    private final Lock l = new ReentrantLock();
    private int logicCounter;


    public DataBase(){
        this.posts = new HashMap<>();
        this.logicCounter = 0;
    }


    public void addPost(String topic, String post, int index){

        ListPosts new_post;
        l.lock();
        if (this.posts.containsKey(topic) ) //Se o topico ja existir vai buscar a lista deles
            new_post = this.posts.get(topic);
        else {
            new_post = new ListPosts();
            this.posts.put(topic,new_post);
        }
        l.unlock();
        new_post.addPost(post,this.logicCounter++,index);

    }

    public int getIndex(String topic){
        if(this.posts.containsKey(topic))
            return this.posts.get(topic).getIndex();
        else
            return 0;
    }

    public ArrayList<Post> getPostsTopic(String topic){
        ArrayList<Post> order = new ArrayList<>();
        if(this.posts.containsKey(topic)){
            l.lock();
            ArrayList<Post> posts =  this.posts.get(topic).getPosts(); //retorna arraylist posts
            int index = this.posts.get(topic).getIndexNoIncrement()%10;
            l.unlock();

            int tam = posts.size();
            for(int i = 0; i < tam; i++){
                order.add(posts.get((index+i)%tam).clone()); //nem consigo descrever o que aqui se passou #Stonks
            }
        }
        return order;

    }






    public static void main(String args[]){
        DataBase teste = new DataBase();
        teste.addPost("teste","1",teste.getIndex("teste"));
        teste.addPost("teste","2",teste.getIndex("teste"));
        teste.addPost("beta","1",teste.getIndex("beta"));
        teste.addPost("teste","3",teste.getIndex("teste"));
        teste.addPost("beta","2",teste.getIndex("beta"));
        teste.addPost("teste","4",teste.getIndex("teste"));
        teste.addPost("teste","5",teste.getIndex("teste"));
        teste.addPost("teste","6",teste.getIndex("teste"));
        teste.addPost("teste","7",teste.getIndex("teste"));
        teste.addPost("teste","8",teste.getIndex("teste"));
        teste.addPost("teste","9",teste.getIndex("teste"));
        teste.addPost("beta","3",teste.getIndex("beta"));
        teste.addPost("teste","10",teste.getIndex("teste"));
        teste.addPost("teste","11",teste.getIndex("teste"));
        teste.addPost("teste","12",teste.getIndex("teste"));
        teste.addPost("kapa","1",teste.getIndex("kapa"));
        ArrayList<Post> p = teste.getPostsTopic("teste");
        ArrayList<Post> pp = teste.getPostsTopic("beta");





    }

}

