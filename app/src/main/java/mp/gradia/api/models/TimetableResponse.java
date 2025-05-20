package mp.gradia.api.models;

import java.util.List;

public class TimetableResponse {
    private List<TimetableItem> timetable;
    private String message;

    public List<TimetableItem> getTimetable() {
        return timetable;
    }

    public void setTimetable(List<TimetableItem> timetable) {
        this.timetable = timetable;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}