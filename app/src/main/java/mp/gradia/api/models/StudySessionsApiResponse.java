package mp.gradia.api.models;

import java.util.List;
import com.google.gson.annotations.SerializedName;

public class StudySessionsApiResponse {

    @SerializedName("sessions")
    private List<StudySession> sessions;

    @SerializedName("message")
    private String message;

    public List<StudySession> getSessions() {
        return sessions;
    }

    public void setSessions(List<StudySession> sessions) {
        this.sessions = sessions;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    @Override
    public String toString() {
        return "StudySessionsApiResponse{" +
                "sessions=" + sessions +
                ", message=\'" + message + '\'' +
                '}';
    }
}