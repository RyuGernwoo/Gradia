package mp.gradia.database.entity;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.PrimaryKey;

import java.time.LocalDate;
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

    // 과목명
    @NonNull
    @ColumnInfo(name = "subject_id")
    public int subjectId;

    // 공부한 날짜
    @NonNull
    @ColumnInfo(name = "date")
    public LocalDate date;

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
    public long restTime;

    public StudySessionEntity(int subjectId, @NonNull LocalDate date, long studyTime, LocalTime startTime, LocalTime endTime, long restTime) {
        this.subjectId = subjectId;
        this.date = date;
        this.studyTime = studyTime;
        this.startTime = startTime;
        this.endTime = endTime;
        this.restTime = restTime;
    }
    public int getSessionId() {
        return sessionId;
    }

    public void setSessionId(int sessionId) {
        this.sessionId = sessionId;
    }

    public int getSubjectId() {
        return subjectId;
    }

    public void setSubjectId(int subjectId) {
        this.subjectId = subjectId;
    }

    @NonNull
    public LocalDate getDate() {
        return date;
    }

    public void setDate(@NonNull LocalDate date) {
        this.date = date;
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
}
