package mp.gradia.api.models;

public class GradePredictionRequest {
    private String subject_name;
    private int understanding_level;
    private int study_time_hours;
    private Integer assignment_quiz_avg_score;

    public GradePredictionRequest(String subject_name, int understanding_level, int study_time_hours,
            Integer assignment_quiz_avg_score) {
        this.subject_name = subject_name;
        this.understanding_level = understanding_level;
        this.study_time_hours = study_time_hours;
        this.assignment_quiz_avg_score = assignment_quiz_avg_score;
    }

    public String getSubject_name() {
        return subject_name;
    }

    public void setSubject_name(String subject_name) {
        this.subject_name = subject_name;
    }

    public int getUnderstanding_level() {
        return understanding_level;
    }

    public void setUnderstanding_level(int understanding_level) {
        this.understanding_level = understanding_level;
    }

    public int getStudy_time_hours() {
        return study_time_hours;
    }

    public void setStudy_time_hours(int study_time_hours) {
        this.study_time_hours = study_time_hours;
    }

    public Integer getAssignment_quiz_avg_score() {
        return assignment_quiz_avg_score;
    }

    public void setAssignment_quiz_avg_score(Integer assignment_quiz_avg_score) {
        this.assignment_quiz_avg_score = assignment_quiz_avg_score;
    }
}