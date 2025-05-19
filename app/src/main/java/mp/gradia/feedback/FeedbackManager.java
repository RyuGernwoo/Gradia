package mp.gradia.feedback;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;

import mp.gradia.database.entity.StudySessionEntity;

/**
 * 피드백 관리 클래스입니다.
 */
public class FeedbackManager {
    private final static int RECENT_DAYS = 3;

    /**
     * 주어진 학습 세션 목록을 분석하여 피드백을 생성합니다.
     *
     * @param logList 학습 세션 목록
     * @return 분석 결과
     */
    public static Optional<FeedbackAnalysis> analayzeLogPeriod(List<StudySessionEntity> logList) {
        FeedbackAnalysis analysis = new FeedbackAnalysis();

        analysis.setTotalSessionCount(logList.size());
        analysis.setTotalDurationMinutes(logList.stream()
                .reduce(0, (sum, session) -> sum + (int) session.studyTime, Integer::sum));
        analysis.setSubjectStudyTime(new HashMap<>());

        // 과목별 학습 시간 계산
        for (var session : logList) {
            if (!analysis.getSubjectStudyTime().containsKey(session.subjectId)) {
                analysis.getSubjectStudyTime().put(session.subjectId, session.studyTime);
            } else {
                analysis.getSubjectStudyTime().put(session.subjectId,
                        analysis.getSubjectStudyTime().get(session.subjectId) + session.studyTime);
            }
        }

        // 평균 세션 시간 계산
        if (analysis.getTotalSessionCount() > 0) {
            analysis.setAverageSessionDurationMinutes(analysis.getTotalDurationMinutes()
                    / analysis.getTotalSessionCount());
        } else {
            analysis.setAverageSessionDurationMinutes(0);
        }

        // 활동 유형 비율 계산
        // ...

        // 분석 결과에 기간 시작/종료 추가
        LocalTime startDate = null;
        LocalTime endDate = null;

        for (var session : logList) {
            if (startDate == null || session.startTime.isBefore(startDate)) {
                startDate = session.startTime;
            }
            if (endDate == null || session.endTime.isAfter(endDate)) {
                endDate = session.endTime;
            }
        }

        analysis.setStartDate(startDate);
        analysis.setEndDate(endDate);

        return Optional.of(analysis);
    }

    /**
     * 최근 분석 결과와 전체 분석 결과를 바탕으로 피드백을 생성합니다.
     *
     * @param recentAnalysis  최근 분석 결과
     * @param overallAnalysis 전체 분석 결과
     */
    public static List<FeedbackAdvice> generateTemperalAdvice(Optional<FeedbackAnalysis> recentAnalysis,
            Optional<FeedbackAnalysis> overallAnalysis) {
        if (overallAnalysis.isEmpty()) {
            return List.of(FeedbackAdvice.createNoAnalysisAdvice());
        }

        if (recentAnalysis.isEmpty()) {
            return List.of(FeedbackAdvice.createNoRecentSessionAdvice(RECENT_DAYS));
        }

        List<FeedbackAdvice> adviceList = new ArrayList<>();

        var recentAnalysisValue = recentAnalysis.get();
        var overallAnalysisValue = overallAnalysis.get();

        int overallAverageSessionDuration = overallAnalysisValue.getAverageSessionDurationMinutes();
        int recentAverageSessionDuration = recentAnalysisValue.getAverageSessionDurationMinutes();
        int recentTotalSessionCount = recentAnalysisValue.getTotalSessionCount();
        var overallSubjectTime = overallAnalysisValue.getSubjectStudyTime();
        var recentSubjectTime = recentAnalysisValue.getSubjectStudyTime();

        // --- 어드바이스 생성 로직 ---

        // 평균 세션 시간 변화
        if (recentAverageSessionDuration > overallAverageSessionDuration * 1.3) {
            adviceList.add(FeedbackAdvice.createRecentSessionLengthenAdvice(RECENT_DAYS));
        } else if (recentAverageSessionDuration < overallAverageSessionDuration * 0.7) {
            adviceList.add(FeedbackAdvice.createRecentSessionShortenAdvice(RECENT_DAYS));
        }

        // 특정 과목 시간 변화
        // 전체 기간 중 학습 시간 상위 3개 과목 선정

        // toList API가 지원하지 않으므로 for-loop 사용
        var toSort = new ArrayList<>(overallSubjectTime.entrySet());
        toSort.sort((e1, e2) -> e2.getValue().compareTo(e1.getValue()));
        List<Integer> topSubjects = new ArrayList<>();
        long limit = 3;
        for (var stringIntegerEntry : toSort) {
            if (limit-- == 0)
                break;
            Integer key = stringIntegerEntry.getKey();
            topSubjects.add(key);
        }

        // 최근 과목 학습 시간이 없는 경우
        for (var subject : topSubjects) {
            if (!recentSubjectTime.containsKey(subject) && recentTotalSessionCount > 0) {
                adviceList.add(FeedbackAdvice.createNoRecentSpecificSubjectAdvice(RECENT_DAYS, subject));
            }
        }

        // 새로운 과목 학습 시작 여부
        if (recentTotalSessionCount > 0) {
            for (var subject : recentSubjectTime.keySet()) {
                if (!overallSubjectTime.containsKey(subject)) {
                    // 새로운 과목 학습 시작
                    adviceList.add(FeedbackAdvice.createStartNewSubjectAdvice(subject));
                }
            }
        }

        // 특별한 패턴 변화 없음
        if (adviceList.isEmpty()) {
            adviceList.add(FeedbackAdvice.createNoSpecialPatternChangedAdvice());
        }

        // 전체적인 조언 추가
        adviceList.addAll(generateBasicAdvice(overallAnalysis));

        return adviceList;
    }

    /**
     * 기본 피드백 조언을 생성합니다.
     *
     * @param overallAnalysis 전체 분석 결과
     * @return 기본 피드백 조언
     */
    public static List<FeedbackAdvice> generateBasicAdvice(Optional<FeedbackAnalysis> overallAnalysis) {
        List<FeedbackAdvice> adviceList = new ArrayList<>();

        if (overallAnalysis.isEmpty()) {
            return adviceList;
        }

        var overallAnalysisValue = overallAnalysis.get();

        int totalDurationMinutes = overallAnalysisValue.getTotalDurationMinutes();
        int averageSessionDurationMinutes = overallAnalysisValue.getAverageSessionDurationMinutes();
        var subjectStudyTime = overallAnalysisValue.getSubjectStudyTime();

        // 평균 세션 시간 조언
        if (averageSessionDurationMinutes > 0) {
            if (averageSessionDurationMinutes < 25) {
                adviceList.add(FeedbackAdvice.createAverageSessionShortAdvice());
            } else if (averageSessionDurationMinutes > 90) {
                adviceList.add(FeedbackAdvice.createAverageSessionLongAdvice());
            }
        }

        // 특정 과목 편중 조언
        if (subjectStudyTime.size() > 0 && totalDurationMinutes > 0) {
            for (var entry : subjectStudyTime.entrySet()) {
                // 편중된 과목 학습 시간 비율이 50% 이상인 경우
                if (entry.getValue() > totalDurationMinutes * 0.5) {
                    adviceList.add(FeedbackAdvice.createSubjectConcentrationAdvice(entry.getKey()));
                }
            }
        }

        return adviceList;
    }
}
