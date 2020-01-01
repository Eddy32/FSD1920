package Database;

public class Post {
    private String post;
    private int globalCounter;

    public Post(String post, int globalCounter) {
        this.post = post;
        this.globalCounter = globalCounter;
    }

    public String getPost() {
        return post;
    }

    public void setPost(String post) {
        this.post = post;
    }

    public int getGlobalCounter() {
        return globalCounter;
    }

    public void setGlobalCounter(int globalCounter) {
        this.globalCounter = globalCounter;
    }

    public Post clone(){
        return new Post(this.getPost(),this.getGlobalCounter());
    }
}
