package mp.gradia.api.models;

public class StudySession {
    private String id;
    private String subject_id;
    private String date;
    private int study_time;
    private String start_time;
    private String end_time;

    private int focus_level;
    private Integer rest_time;

    private String memo;
    private String created_at;
    private String updated_at;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getSubject_id() {
        return subject_id;
    }

    public void setSubject_id(String subject_id) {
        this.subject_id = subject_id;
    }

    public String getDate() {
        return date;
    }

    public void setDate(String date) {
        this.date = date;
    }

    public int getStudy_time() {
        return study_time;
    }

    public void setStudy_time(int study_time) {
        this.study_time = study_time;
    }

    public String getStart_time() {
        return start_time;
    }

    public void setStart_time(String start_time) {
        this.start_time = start_time;
    }

    public String getEnd_time() {
        return end_time;
    }

    public void setEnd_time(String end_time) {
        this.end_time = end_time;
    }

    public int getFocus_level() {
        return focus_level;
    }

    public void setFocus_level(int focus_level) {
        this.focus_level = focus_level;
    }

    public Integer getRest_time() {
        return rest_time;
    }

    public void setRest_time(Integer rest_time) {
        this.rest_time = rest_time;
    }

    public String getMemo() {
        return memo;
    }

    public void setMemo(String memo) {
        this.memo = memo;
    }

    public String getCreated_at() {
        return created_at;
    }

    public void setCreated_at(String created_at) {
        this.created_at = created_at;
    }

    public String getUpdated_at() {
        return updated_at;
    }

    public void setUpdated_at(String updated_at) {
        this.updated_at = updated_at;
    }
}