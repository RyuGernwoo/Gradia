package mp.gradia.api.models;

public class GradePredictionRequestV2 {
    private String subject_id;

    private int understanding_level;

    public GradePredictionRequestV2(String subject_id, int understanding_level) {
        this.subject_id = subject_id;
        this.understanding_level = understanding_level;
    }

    public String getSubject_id() {
        return subject_id;
    }

    public void setSubject_id(String subject_id) {
        this.subject_id = subject_id;
    }

    public int getUnderstanding_level() {
        return understanding_level;
    }

    public void setUnderstanding_level(int understanding_level) {
        this.understanding_level = understanding_level;
    }
}
