package mp.gradia.api.models;

import java.util.List;

public class StructuredPrediction {
    private String score;
    private String score_range;
    private String grade;
    private List<String> factors;
    private List<String> advice;

    public String getScore() {
        return score;
    }

    public void setScore(String score) {
        this.score = score;
    }

    public String getScore_range() {
        return score_range;
    }

    public void setScore_range(String score_range) {
        this.score_range = score_range;
    }

    public String getGrade() {
        return grade;
    }

    public void setGrade(String grade) {
        this.grade = grade;
    }

    public List<String> getFactors() {
        return factors;
    }

    public void setFactors(List<String> factors) {
        this.factors = factors;
    }

    public List<String> getAdvice() {
        return advice;
    }

    public void setAdvice(List<String> advice) {
        this.advice = advice;
    }
}