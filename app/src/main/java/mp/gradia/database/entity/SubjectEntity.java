package mp.gradia.database.entity;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.room.ColumnInfo;
import androidx.room.Embedded;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

import java.time.LocalDateTime;

@Entity(tableName = "subjects")
public class SubjectEntity {
    public static final int REQUIRED_SUBJECT = 0;
    public static final int ELECTIVE_SUBJECT = 1;
    public static final int LIB_SUBJECT = 2;

    // 과목 id
    @PrimaryKey(autoGenerate = true)
    @NonNull
    @ColumnInfo(name = "subject_id")
    public int subjectId = 0;

    // 서버 ID
    @Nullable
    @ColumnInfo(name = "server_id")
    public String serverId;

    // 생성 시간
    @Nullable
    @ColumnInfo(name = "created_at")
    public LocalDateTime createdAt;

    // 업데이트 시간
    @Nullable
    @ColumnInfo(name = "updated_at")
    public LocalDateTime updatedAt;

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
    @Nullable
    @ColumnInfo(name = "difficulty")
    public Integer difficulty;

    // 중간고사 일정
    @Nullable
    @ColumnInfo(name = "mid_term_schedule")
    public String midTermSchedule;

    // 기말고사 일정
    @Nullable
    @ColumnInfo(name = "final_term_schedule")
    public String finalTermSchedule;

    // 과목 평가 비율
    @Nullable
    @Embedded
    public EvaluationRatio ratio;

    // 월간/주간/일간 목표 시간
    @Nullable
    @Embedded
    public TargetStudyTime time;

    // UI Color Tag
    @ColumnInfo(name = "color")
    public String color;

    private boolean isExpanded = false;

    public SubjectEntity(String name, int credit, String color, int type, String midTermSchedule,
            String finalTermSchedule, EvaluationRatio ratio, TargetStudyTime time) {
        this.name = name;
        this.credit = credit;
        this.color = color;
        this.type = type;
        this.midTermSchedule = midTermSchedule;
        this.finalTermSchedule = finalTermSchedule;
        this.ratio = ratio;
        this.time = time;
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    public boolean isExpanded() {
        return isExpanded;
    }

    public void setExpanded(boolean expanded) {
        isExpanded = expanded;
    }

    public int getSubjectId() {
        return subjectId;
    }

    public void setSubjectId(int subjectId) {
        this.subjectId = subjectId;
    }

    @Nullable
    public String getServerId() {
        return serverId;
    }

    public void setServerId(@Nullable String serverId) {
        this.serverId = serverId;
    }

    @Nullable
    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(@Nullable LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    @Nullable
    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(@Nullable LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
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

    public String getMidTermSchedule() {
        return midTermSchedule;
    }

    public void setMidTermSchedule(String midTermSchedule) {
        this.midTermSchedule = midTermSchedule;
    }

    public String getFinalTermSchedule() {
        return finalTermSchedule;
    }

    public void setFinalTermSchedule(String finalTermSchedule) {
        this.finalTermSchedule = finalTermSchedule;
    }

    public EvaluationRatio getRatio() {
        return ratio;
    }

    public void setRatio(EvaluationRatio ratio) {
        this.ratio = ratio;
    }

    public String getColor() {
        return color;
    }

    public void setColor(String color) {
        this.color = color;
    }

    public TargetStudyTime getTime() {
        return time;
    }

    public void setTime(TargetStudyTime time) {
        this.time = time;
    }
}
