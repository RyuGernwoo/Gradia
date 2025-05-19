package mp.gradia.feedback;

/**
 * 피드백 조언을 나타내는 클래스입니다.
 */
public class FeedbackAdvice {
    private FeedbackAdviceType type;
    private int days;
    private int subjectId;

    private FeedbackAdvice(FeedbackAdviceType type) {
        this.type = type;
    }

    public FeedbackAdviceType getType() {
        return type;
    }

    public int getDays() {
        return days;
    }

    public int getSubjectId() {
        return subjectId;
    }

    /**
     * 분석 결과 없음
     * 
     * @return 피드백 조언 객체
     */
    public static FeedbackAdvice createNoAnalysisAdvice() {
        return new FeedbackAdvice(FeedbackAdviceType.NO_ANALYSIS);
    }

    /**
     * 최근 n일간 세션 없음
     * 
     * @param days 최근 세션이 없었던 일수
     * @return 피드백 조언 객체
     */
    public static FeedbackAdvice createNoRecentSessionAdvice(int days) {
        FeedbackAdvice advice = new FeedbackAdvice(FeedbackAdviceType.NO_RECENT_SESSION);
        advice.days = days;
        return advice;
    }

    /**
     * 최근 n일 평균 보다 세션이 길어짐
     * 
     * @param days 최근 세션이 길어졌던 일수
     * @return 피드백 조언 객체
     */
    public static FeedbackAdvice createRecentSessionLengthenAdvice(int days) {
        FeedbackAdvice advice = new FeedbackAdvice(FeedbackAdviceType.RECENT_SESSION_LENGTHEN);
        advice.days = days;
        return advice;
    }

    /**
     * 최근 n일 평균 보다 세션이 짧아짐
     * 
     * @param days 최근 세션이 짧아졌던 일수
     * @return 피드백 조언 객체
     */
    public static FeedbackAdvice createRecentSessionShortenAdvice(int days) {
        FeedbackAdvice advice = new FeedbackAdvice(FeedbackAdviceType.RECENT_SESSION_SHORTEN);
        advice.days = days;
        return advice;
    }

    /**
     * 최근 n일간 특정 과목의 학습 없음
     * 
     * @param days      최근 특정 과목의 학습이 없었던 일수
     * @param subjectId 특정 과목 이름
     * @return 피드백 조언 객체
     */
    public static FeedbackAdvice createNoRecentSpecificSubjectAdvice(int days, int subjectId) {
        FeedbackAdvice advice = new FeedbackAdvice(FeedbackAdviceType.NO_RECENT_SPECIFIC_SUBJECT);
        advice.days = days;
        advice.subjectId = subjectId;
        return advice;
    }

    /**
     * 새로운 과목 시작
     * 
     * @param subjectId 새로운 과목 이름
     * @return 피드백 조언 객체
     */
    public static FeedbackAdvice createStartNewSubjectAdvice(int subjectId) {
        FeedbackAdvice advice = new FeedbackAdvice(FeedbackAdviceType.START_NEW_SUBJECT);
        advice.subjectId = subjectId;
        return advice;
    }

    /**
     * 특별한 패턴 변화 없음
     * 
     * @return 피드백 조언 객체
     */
    public static FeedbackAdvice createNoSpecialPatternChangedAdvice() {
        return new FeedbackAdvice(FeedbackAdviceType.NO_SPECIAL_PATTERN_CHANGED);
    }

    /**
     * 평균 세션이 짧음
     * 
     * @return 피드백 조언 객체
     */
    public static FeedbackAdvice createAverageSessionShortAdvice() {
        return new FeedbackAdvice(FeedbackAdviceType.AVERAGE_SESSION_SHORT);
    }

    /**
     * 평균 세션이 길음
     * 
     * @return 피드백 조언 객체
     */
    public static FeedbackAdvice createAverageSessionLongAdvice() {
        return new FeedbackAdvice(FeedbackAdviceType.AVERAGE_SESSION_LONG);
    }

    /**
     * 특정 과목 집중
     * 
     * @param subjectId 특정 과목 이름
     * @return 피드백 조언 객체
     */
    public static FeedbackAdvice createSubjectConcentrationAdvice(int subjectId) {
        FeedbackAdvice advice = new FeedbackAdvice(FeedbackAdviceType.SUBJECT_CONCENTRATION);
        advice.subjectId = subjectId;
        return advice;
    }
}
