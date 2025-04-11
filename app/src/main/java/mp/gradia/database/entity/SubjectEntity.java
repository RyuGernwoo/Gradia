package mp.gradia.database.entity;

import androidx.annotation.NonNull;
import androidx.room.ColumnInfo;
import androidx.room.Embedded;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(
        tableName = "subjects"
)
public class SubjectEntity {
    // 과목 id
    @PrimaryKey(autoGenerate = true)
    @NonNull
    @ColumnInfo(name = "subject_id")
    public int subjectId;

    // 과목명
    @ColumnInfo(name = "name")
    public String name;

    // 과목 구분 (0: 전필, 1: 전선, 2: 교양)
    @ColumnInfo(name = "type")
    public int type;

    // 학점
    @ColumnInfo(name = "credit")
    public int credit;

    // 난이도
    // Nullable (Predict Based Value)
    @ColumnInfo(name = "difficulty")
    public Integer difficulty;

    // 중간고사 일정
    @ColumnInfo(name = "mid_term_schedule")
    public String midTermSchedule;

    // 기말고사 일정
    @ColumnInfo(name = "final_term_schedule")
    public String finalTermSchedule;

    // 과목 평가 비율
    @Embedded
    public EvaluationRatio ratio;

    // 월간/주간/일간 목표 시간
    @Embedded
    public TargetStudyTime time;

    // UI Color Tag
    @ColumnInfo(name = "color")
    public String color;

}

