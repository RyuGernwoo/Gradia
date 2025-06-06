package mp.gradia.feedback;

import java.time.LocalTime;
import java.util.Date;
import java.util.Map;

/**
 * 피드백 분석 결과를 나타내는 클래스입니다.
 */
public class FeedbackAnalysis {
    int totalSessionCount;
    int totalDurationMinutes;
    int averageSessionDurationMinutes;
//    int activityTimeRatio;

    LocalTime startDate;
    LocalTime endDate;

    Map<Integer, Long> subjectStudyTime;
//    Map<String, Integer> activityDistribution;
//    Map<String, Integer> activityTimeDistribution;

    // Getters

    /**
     * 총 세션 수를 가져옵니다.
     * @return 총 세션 수
     */
    public int getTotalSessionCount() {
        return totalSessionCount;
    }

    /**
     * 총 학습 시간을 가져옵니다.
     * @return 총 학습 시간 (분 단위)
     */
    public int getTotalDurationMinutes() {
        return totalDurationMinutes;
    }

    /**
     * 평균 세션 시간을 가져옵니다.
     * @return 평균 세션 시간 (분 단위)
     */
    public int getAverageSessionDurationMinutes() {
        return averageSessionDurationMinutes;
    }

//    public int getActivityTimeRatio() {
//        return activityTimeRatio;
//    }

    /**
     * 시작 날짜를 가져옵니다.
     * @return 시작 날짜
     */
    public LocalTime getStartDate() {
        return startDate;
    }

    /**
     * 종료 날짜를 가져옵니다.
     * @return 종료 날짜
     */
    public LocalTime getEndDate() {
        return endDate;
    }

    /**
     * 과목별 학습 시간을 가져옵니다.
     * @return 과목별 학습 시간 (Map<과목명, 학습시간>)
     */
    public Map<Integer, Long> getSubjectStudyTime() {
        return subjectStudyTime;
    }

//    public Map<String, Integer> getActivityDistribution() {
//        return activityDistribution;
//    }
//
//    public Map<String, Integer> getActivityTimeDistribution() {
//        return activityTimeDistribution;
//    }

    // Setters

    /**
     * 세션 수를 설정합니다.
     * @param totalSessionCount 세션 수
     */
    public void setTotalSessionCount(int totalSessionCount) {
        this.totalSessionCount = totalSessionCount;
    }

    /**
     * 총 학습 시간을 설정합니다.
     * @param totalDurationMinutes 총 학습 시간 (분 단위)
     */
    public void setTotalDurationMinutes(int totalDurationMinutes) {
        this.totalDurationMinutes = totalDurationMinutes;
    }

    /**
     * 평균 세션 시간을 설정합니다.
     * @param averageSessionDurationMinutes 평균 세션 시간 (분 단위)
     */
    public void setAverageSessionDurationMinutes(int averageSessionDurationMinutes) {
        this.averageSessionDurationMinutes = averageSessionDurationMinutes;
    }

//    public void setActivityTimeRatio(int activityTimeRatio) {
//        this.activityTimeRatio = activityTimeRatio;
//    }

    /**
     * 시작 날짜를 설정합니다.
     * @param startDate 시작 날짜
     */
    public void setStartDate(LocalTime startDate) {
        this.startDate = startDate;
    }

    /**
     * 종료 날짜를 설정합니다.
     * @param endDate 종료 날짜
     */
    public void setEndDate(LocalTime endDate) {
        this.endDate = endDate;
    }

    /**
     * 과목별 학습 시간을 설정합니다.
     * @param subjectStudyTime 과목별 학습 시간 (Map<과목명, 학습시간>)
     */
    public void setSubjectStudyTime(Map<Integer, Long> subjectStudyTime) {
        this.subjectStudyTime = subjectStudyTime;
    }

//    public void setActivityDistribution(Map<String, Integer> activityDistribution) {
//        this.activityDistribution = activityDistribution;
//    }
//
//    public void setActivityTimeDistribution(Map<String, Integer> activityTimeDistribution) {
//        this.activityTimeDistribution = activityTimeDistribution;
//    }
}
