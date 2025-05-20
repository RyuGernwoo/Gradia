package mp.gradia.database.entity;

import androidx.room.ColumnInfo;

public class EvaluationRatio {
    // 중간고사 비율
    @ColumnInfo(name = "mid_term_ratio")
    public int midTermRatio;

    // 기말고사 비율
    @ColumnInfo(name = "final_term_ratio")
    public int finalTermRatio;

    // 퀴즈 비율
    @ColumnInfo(name = "quiz_ratio")
    public int quizRatio;

    // 과제 비율
    @ColumnInfo(name = "assignment_ratio")
    public int assignmentRatio;

    // 출석 비율
    @ColumnInfo(name = "attendance_ratio")
    public int attendanceRatio;

    public EvaluationRatio(int midTermRatio, int finalTermRatio, int quizRatio, int assignmentRatio, int attendanceRatio) {
        this.midTermRatio = midTermRatio;
        this.finalTermRatio = finalTermRatio;
        this.quizRatio = quizRatio;
        this.assignmentRatio = assignmentRatio;
        this.attendanceRatio = attendanceRatio;
    }

    public EvaluationRatio() {
        this.midTermRatio = 0;
        this.finalTermRatio = 0;
        this.quizRatio = 0;
        this.assignmentRatio = 0;
        this.attendanceRatio = 0;
    }
}
