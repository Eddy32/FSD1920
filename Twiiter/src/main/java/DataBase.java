

import java.util.HashMap;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class DataBase {
    private HashMap<String, ListPosts> posts;
    private final Lock l = new ReentrantLock();


    public DataBase(){
        this.posts = new HashMap<>();
    }


    public Pair<Integer,Integer> addPost(String topic, String post){
        Pair<Integer, Integer> par;

        ListPosts new_post;
        l.lock();
        if (this.posts.containsKey(topic) ) //Se o topico ja existir vai buscar a lista deles
            new_post = this.posts.get(topic);
        else {
            new_post = new ListPosts();
            this.posts.put(topic,new_post);
        }
        l.unlock();
        par = new_post.getIndexVoltas();
        new_post.addPost(post,par.getLeft());                       //Esquerda indice que foi inserido
                                                                    //Direita numero de voltas que ja deu ao arraylist
        return par;

    }

    public synchronized ListPosts getPostsTopic(String topic){
        return this.posts.get(topic);
    }





    public static void main(String args[]){
        Pair<Integer,Integer> a,b,c,d;
        DataBase teste = new DataBase();
        teste.addPost("teste","1");
        teste.addPost("teste","2");
        teste.addPost("beta","1");
        teste.addPost("teste","3");
        teste.addPost("beta","2");
        teste.addPost("teste","4");
        teste.addPost("teste","5");
        teste.addPost("teste","6");
        teste.addPost("teste","7");
        teste.addPost("teste","8");
        d = teste.addPost("teste","9");
        teste.addPost("beta","3");
        teste.addPost("teste","10");
        a = teste.addPost("teste","11");
        b = teste.addPost("teste","12");
        c = teste.addPost("kapa","1");




    }

}

