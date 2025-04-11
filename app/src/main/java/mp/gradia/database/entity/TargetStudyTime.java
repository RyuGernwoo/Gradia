package mp.gradia.database.entity;

import androidx.room.ColumnInfo;

public class TargetStudyTime {
    // 일간 학습 목표 시간
    @ColumnInfo(name = "daily_target_study_time")
    public int dailyTargetStudyTime;

    // 주간 학습 목표 시간
    @ColumnInfo(name = "weekly_target_study_time")
    public int weeklyTargetStudyTime;

    // 월간 학습 목표 시간
    @ColumnInfo(name = "monthly_target_study_time")
    public int monthlyTargetStudyTime;
}
