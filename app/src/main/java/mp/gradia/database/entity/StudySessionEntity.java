package mp.gradia.database.entity;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.PrimaryKey;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Date;

@Entity(
        tableName = "study_session",
        foreignKeys = @ForeignKey(
            entity = SubjectEntity.class,
                parentColumns = "subject_id",
                childColumns = "subject_id",
                onDelete = ForeignKey.CASCADE
        )
)
public class StudySessionEntity {
    // 세션 id
    @PrimaryKey(autoGenerate = true) // 자동으로 id 증가 (세션별로 오름차순)
    @NonNull
    @ColumnInfo(name = "session_id")
    public int sessionId = 0;

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
    @NonNull
    @ColumnInfo(name = "subject_id")
    public int subjectId;

    @NonNull
    @ColumnInfo(name = "subject_name")
    public String subjectName;

    // 공부한 날짜
    @NonNull
    @ColumnInfo(name = "date")
    public LocalDate date;

    @Nullable
    @ColumnInfo(name = "end_date")
    public LocalDate endDate;

    // 공부 시간
    @ColumnInfo(name = "study_time")
    public long studyTime;

    // 시작 시각 (timestamp)
    @ColumnInfo(name = "start_time")
    public LocalTime startTime;

    // 종료 시각 (timestamp)
    @ColumnInfo(name = "end_time")
    public LocalTime endTime;

    // 휴식/중지 시간
    @Nullable
    @ColumnInfo(name = "rest_time")
    public long restTime = 0;

    @Nullable
    @ColumnInfo(name = "focus_level")
    public int focusLevel = -1;

    @Nullable
    @ColumnInfo(name = "memo")
    public String memo = "";


    // 생성자 정의
    public StudySessionEntity(int subjectId, @NonNull String subjectName, @NonNull LocalDate date, @Nullable LocalDate endDate, long studyTime, LocalTime startTime, LocalTime endTime,@Nullable long restTime,@Nullable int focusLevel, @Nullable String memo) {
        this.subjectId = subjectId;
        this.subjectName = subjectName;
        this.date = date;
        this.endDate = endDate;
        this.studyTime = studyTime;
        this.startTime = startTime;
        this.endTime = endTime;
        this.restTime = restTime;
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
        this.focusLevel = focusLevel;
        this.memo = memo;
    }


    // Getter, Setter 정의
    public int getSessionId() {
        return sessionId;
    }

    public void setSessionId(int sessionId) {
        this.sessionId = sessionId;
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

    public int getSubjectId() {
        return subjectId;
    }

    public void setSubjectId(int subjectId) {
        this.subjectId = subjectId;
    }

    @NonNull
    public String getSubjectName() {
        return subjectName;
    }

    public void setSubjectName(@NonNull String subjectName) {
        this.subjectName = subjectName;
    }

    @NonNull
    public LocalDate getDate() {
        return date;
    }

    public void setDate(@NonNull LocalDate date) {
        this.date = date;
    }

    @Nullable
    public LocalDate getEndDate() {
        return endDate;
    }

    @Nullable
    public void setEndDate(LocalDate endDate) {
        this.endDate = endDate;
    }

    public long getStudyTime() {
        return studyTime;
    }

    public void setStudyTime(long studyTime) {
        this.studyTime = studyTime;
    }

    public LocalTime getStartTime() {
        return startTime;
    }

    public void setStartTime(LocalTime startTime) {
        this.startTime = startTime;
    }

    public LocalTime getEndTime() {
        return endTime;
    }

    public void setEndTime(LocalTime endTime) {
        this.endTime = endTime;
    }

    public long getRestTime() {
        return restTime;
    }

    public void setRestTime(int restTime) {
        this.restTime = restTime;
    }

    public int getFocusLevel() {
        return focusLevel;
    }

    public void setFocusLevel(int focusLevel) {
        this.focusLevel = focusLevel;
    }

    public String getMemo() {
        return memo;
    }

    public void setMemo(String memo) {
        this.memo = memo;
    }
}
