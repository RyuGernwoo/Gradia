package mp.gradia.api.models;

public class Todo {
    private String content;
    private boolean isDone;

    public Todo(String content, boolean isDone) {
        this.content = content;
        this.isDone = isDone;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public boolean isDone() {
        return isDone;
    }

    public void setDone(boolean done) {
        isDone = done;
    }
}
