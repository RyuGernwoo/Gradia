package mp.gradia.feedback;

/**
 * 피드백 조언의 유형을 나타내는 열거형입니다.
 */
public enum FeedbackAdviceType {
    NO_ANALYSIS, // 분석 결과 없음
    RECENT_SESSION_LENGTHEN, // 최근 세션이 길어짐
    RECENT_SESSION_SHORTEN, // 최근 세션이 짧아짐
    NO_RECENT_SESSION, // 최근 n일간 세션 없음
    NO_RECENT_SPECIFIC_SUBJECT, // 최근 n일간 특정 과목의 학습 없음
    START_NEW_SUBJECT, // 새로운 과목 시작
    NO_SPECIAL_PATTERN_CHANGED, // 특별한 패턴 변화 없음

    AVERAGE_SESSION_SHORT, // 평균 세션이 짧음
    AVERAGE_SESSION_LONG, // 평균 세션이 길음

    SUBJECT_CONCENTRATION, // 특정 과목 집중
}