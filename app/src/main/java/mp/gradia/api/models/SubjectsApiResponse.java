package mp.gradia.api.models;

import java.util.List;
import com.google.gson.annotations.SerializedName;

public class SubjectsApiResponse {

    @SerializedName("subjects")
    private List<Subject> subjects;

    @SerializedName("message")
    private String message;

    public List<Subject> getSubjects() {
        return subjects;
    }

    public void setSubjects(List<Subject> subjects) {
        this.subjects = subjects;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    @Override
    public String toString() {
        return "SubjectsApiResponse{" +
                "subjects=" + subjects +
                ", message=\'" + message + '\'' +
                '}';
    }
}