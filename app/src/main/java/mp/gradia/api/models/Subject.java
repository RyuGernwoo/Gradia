package mp.gradia.api.models;

import java.util.List;

public class Subject {
    private String id;
    private String name;
    private int type;
    private int credit;
    private Integer difficulty;
    private String mid_term_schedule;
    private String final_term_schedule;
    private EvaluationRatio evaluation_ratio;
    private TargetStudyTime target_study_time;
    private List<Todo> todos;
    private String color;
    private String created_at;
    private String updated_at;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int getType() {
        return type;
    }

    public void setType(int type) {
        this.type = type;
    }

    public int getCredit() {
        return credit;
    }

    public void setCredit(int credit) {
        this.credit = credit;
    }

    public Integer getDifficulty() {
        return difficulty;
    }

    public void setDifficulty(Integer difficulty) {
        this.difficulty = difficulty;
    }

    public String getMid_term_schedule() {
        return mid_term_schedule;
    }

    public void setMid_term_schedule(String mid_term_schedule) {
        this.mid_term_schedule = mid_term_schedule;
    }

    public String getFinal_term_schedule() {
        return final_term_schedule;
    }

    public void setFinal_term_schedule(String final_term_schedule) {
        this.final_term_schedule = final_term_schedule;
    }

    public EvaluationRatio getEvaluation_ratio() {
        return evaluation_ratio;
    }

    public void setEvaluation_ratio(EvaluationRatio evaluation_ratio) {
        this.evaluation_ratio = evaluation_ratio;
    }

    public TargetStudyTime getTarget_study_time() {
        return target_study_time;
    }

    public void setTarget_study_time(TargetStudyTime target_study_time) {
        this.target_study_time = target_study_time;
    }

    public List<Todo> getTodos() {
        return todos;
    }

    public void setTodos(List<Todo> todos) {
        this.todos = todos;
    }

    public String getColor() {
        return color;
    }

    public void setColor(String color) {
        this.color = color;
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