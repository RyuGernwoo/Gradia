package mp.gradia.api.models;

public class TargetStudyTime {
    private int daily_target_study_time;
    private int weekly_target_study_time;
    private int monthly_target_study_time;

    public int getDaily_target_study_time() {
        return daily_target_study_time;
    }

    public void setDaily_target_study_time(int daily_target_study_time) {
        this.daily_target_study_time = daily_target_study_time;
    }

    public int getWeekly_target_study_time() {
        return weekly_target_study_time;
    }

    public void setWeekly_target_study_time(int weekly_target_study_time) {
        this.weekly_target_study_time = weekly_target_study_time;
    }

    public int getMonthly_target_study_time() {
        return monthly_target_study_time;
    }

    public void setMonthly_target_study_time(int monthly_target_study_time) {
        this.monthly_target_study_time = monthly_target_study_time;
    }
}