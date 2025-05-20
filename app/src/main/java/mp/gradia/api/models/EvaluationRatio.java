package mp.gradia.api.models;

public class EvaluationRatio {
    private int mid_term_ratio;
    private int final_term_ratio;
    private int quiz_ratio;
    private int assignment_ratio;
    private int attendance_ratio;

    public int getMid_term_ratio() {
        return mid_term_ratio;
    }

    public void setMid_term_ratio(int mid_term_ratio) {
        this.mid_term_ratio = mid_term_ratio;
    }

    public int getFinal_term_ratio() {
        return final_term_ratio;
    }

    public void setFinal_term_ratio(int final_term_ratio) {
        this.final_term_ratio = final_term_ratio;
    }

    public int getQuiz_ratio() {
        return quiz_ratio;
    }

    public void setQuiz_ratio(int quiz_ratio) {
        this.quiz_ratio = quiz_ratio;
    }

    public int getAssignment_ratio() {
        return assignment_ratio;
    }

    public void setAssignment_ratio(int assignment_ratio) {
        this.assignment_ratio = assignment_ratio;
    }

    public int getAttendance_ratio() {
        return attendance_ratio;
    }

    public void setAttendance_ratio(int attendance_ratio) {
        this.attendance_ratio = attendance_ratio;
    }
}