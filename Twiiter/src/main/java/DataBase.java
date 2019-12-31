

import java.util.ArrayList;
import java.util.HashMap;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class DataBase {
    private HashMap<String, ListPosts> posts;
    private final Lock l = new ReentrantLock();


    public DataBase(){
        this.posts = new HashMap<>();
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
        new_post.addPost(post,index);                       //Esquerda indice que foi inserido
                                                            //Direita numero de voltas que ja deu ao arraylist

    }

    public Pair<Integer,Integer> getIndexVoltas(String topic){
        if(this.posts.containsKey(topic)){
            Pair<Integer,Integer> par  = this.posts.get(topic).getIndexVoltas();
            return par;
        }
        else
            return new Pair<>(0,0);
    }

    public synchronized ArrayList<String> getPostsTopic(String topic){
        return this.posts.get(topic).getPosts();
    }





    public static void main(String args[]){
        Pair<Integer,Integer> a,b,c,d;
        DataBase teste = new DataBase();
        teste.addPost("teste","1",teste.getIndexVoltas("teste").getLeft());
        teste.addPost("teste","2",teste.getIndexVoltas("teste").getLeft());
        teste.addPost("beta","1",teste.getIndexVoltas("beta").getLeft());
        teste.addPost("teste","3",teste.getIndexVoltas("teste").getLeft());
        teste.addPost("beta","2",teste.getIndexVoltas("beta").getLeft());
        teste.addPost("teste","4",teste.getIndexVoltas("teste").getLeft());
        teste.addPost("teste","5",teste.getIndexVoltas("teste").getLeft());
        teste.addPost("teste","6",teste.getIndexVoltas("teste").getLeft());
        teste.addPost("teste","7",teste.getIndexVoltas("teste").getLeft());
        teste.addPost("teste","8",teste.getIndexVoltas("teste").getLeft());
        teste.addPost("teste","9",teste.getIndexVoltas("teste").getLeft());
        teste.addPost("beta","3",teste.getIndexVoltas("beta").getLeft());
        teste.addPost("teste","10",teste.getIndexVoltas("teste").getLeft());
        teste.addPost("teste","11",teste.getIndexVoltas("teste").getLeft());
        teste.addPost("teste","12",teste.getIndexVoltas("teste").getLeft());
        teste.addPost("kapa","1",teste.getIndexVoltas("kapa").getLeft());




    }

}

