package mp.gradia.database.entity;

import androidx.annotation.Nullable;
import androidx.room.ColumnInfo;

public class TargetStudyTime {
    // 일간 학습 목표 시간
    @ColumnInfo(name = "daily_target_study_time")
    public int dailyTargetStudyTime = -1;

    // 주간 학습 목표 시간
    @ColumnInfo(name = "weekly_target_study_time")
    public int weeklyTargetStudyTime = -1;

    // 월간 학습 목표 시간
    @ColumnInfo(name = "monthly_target_study_time")
    public int monthlyTargetStudyTime = -1;

    public TargetStudyTime(int dailyTargetStudyTime, int weeklyTargetStudyTime, int monthlyTargetStudyTime) {
        this.dailyTargetStudyTime = dailyTargetStudyTime;
        this.weeklyTargetStudyTime = weeklyTargetStudyTime;
        this.monthlyTargetStudyTime = monthlyTargetStudyTime;
    }

    public int getDailyTargetStudyTime() {
        return dailyTargetStudyTime;
    }

    public int getWeeklyTargetStudyTime() {
        return weeklyTargetStudyTime;
    }

    public int getMonthlyTargetStudyTime() {
        return monthlyTargetStudyTime;
    }

    public void setDailyTargetStudyTime(int dailyTargetStudyTime) {
        this.dailyTargetStudyTime = dailyTargetStudyTime;
    }

    public void setWeeklyTargetStudyTime(int weeklyTargetStudyTime) {
        this.weeklyTargetStudyTime = weeklyTargetStudyTime;
    }

    public void setMonthlyTargetStudyTime(int monthlyTargetStudyTime) {
        this.monthlyTargetStudyTime = monthlyTargetStudyTime;
    }
}
