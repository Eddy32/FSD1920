package Protos;

public class TryUpdate {

    private String text;
    private String category;
    private int serverId;
    private int serverClock;
    private String user;
    private int userClock;

    public TryUpdate(String text, String category, int serverId, int serverClock, String user, int userClock) {
        this.text = text;
        this.category = category;
        this.serverId = serverId;
        this.serverClock = serverClock;
        this.user = user;
        this.userClock = userClock;
    }

    public int getUserClock() { return userClock; }

    public String getUser() { return user; }

    public String getText() {
        return text;
    }

    public String getCategory() {
        return category;
    }

    public int getServerId() {
        return serverId;
    }

    public int getServerClock() {
        return serverClock;
    }

    public void setText(String text) {
        this.text = text;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public void setServerId(int serverId) {
        this.serverId = serverId;
    }

    public void setServerClock(int serverClock) {
        this.serverClock = serverClock;
    }
}
