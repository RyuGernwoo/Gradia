package mp.gradia.database.entity;

import androidx.annotation.NonNull;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.PrimaryKey;

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
    public int sessionId;

    // 과목명
    @NonNull
    @ColumnInfo(name = "subject_id")
    public String subjectId;

    // 공부한 날짜
    @NonNull
    @ColumnInfo(name = "date")
    public String date;

    // 공부 시간
    @ColumnInfo(name = "study_time")
    public int studyTime;

    // 시작 시각 (timestamp)
    @ColumnInfo(name = "start_time")
    public Date startTime;

    // 종료 시각 (timestamp)
    @ColumnInfo(name = "end_time")
    public Date endTime;

    // 휴식/중지 시간
    @ColumnInfo(name = "rest_time")
    public int restTime;
}
