package mp.gradia.feedback;

import org.junit.Test;

import static org.junit.Assert.*;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import mp.gradia.database.entity.StudySessionEntity;

public class FeedbackManagerTest {

    // 과목 문자열을 정수 ID로 매핑하기 위한 맵 (테스트용)
    private static final Map<String, Integer> subjectNameToIdMap = new HashMap<>();
    static {
        subjectNameToIdMap.put("math", 1);
        subjectNameToIdMap.put("science", 2);
        subjectNameToIdMap.put("english", 3);
        subjectNameToIdMap.put("history", 4);
        subjectNameToIdMap.put("art", 5);
    }

    private int getSubjectId(String name) {
        return subjectNameToIdMap.getOrDefault(name, 0); // 기본값 0 또는 예외 처리
    }

    /**
     * 정상적인 데이터가 주어졌을 때, 분석 결과가 올바르게 처리되는지 테스트합니다.
     */
    @Test
    public void testAnalyzeLogPeriodWithNormalData() {
        // 테스트용 데이터 생성
        List<StudySessionEntity> sessions = createSampleSessions();

        // FeedbackManager 분석 실행
        Optional<FeedbackAnalysis> analysis = FeedbackManager.analayzeLogPeriod(sessions);
        var analysisValue = analysis.orElse(null);

        assertNotNull(analysisValue);

        // 결과 검증
        assertEquals(3, analysisValue.getTotalSessionCount());
        assertEquals(190, analysisValue.getTotalDurationMinutes());
        assertEquals(63, analysisValue.getAverageSessionDurationMinutes()); // 190 / 3 = 63.33...

        // 과목별 시간 검증
        assertEquals(Long.valueOf(100), analysisValue.getSubjectStudyTime().get(getSubjectId("math")));
        assertEquals(Long.valueOf(50), analysisValue.getSubjectStudyTime().get(getSubjectId("science")));

        // 날짜 범위 검증 (LocalTime으로 변경됨에 따라 해당 필드가 LocalTime을 반환한다고 가정)
        assertNotNull(analysisValue.getStartDate());
        assertNotNull(analysisValue.getEndDate());
    }

    /**
     * 빈 리스트를 입력으로 주었을 때, 분석 결과가 올바르게 처리되는지 테스트합니다.
     */
    @Test
    public void testAnalyzeLogPeriodWithEmptyList() {
        List<StudySessionEntity> emptySessions = new ArrayList<>();

        Optional<FeedbackAnalysis> analysis = FeedbackManager.analayzeLogPeriod(emptySessions);
        var analysisValue = analysis.orElse(null);

        assertNotNull(analysisValue);

        assertEquals(0, analysisValue.getTotalSessionCount());
        assertEquals(0, analysisValue.getTotalDurationMinutes());
        assertEquals(0, analysisValue.getAverageSessionDurationMinutes());
        assertTrue(analysisValue.getSubjectStudyTime().isEmpty());
        assertNull(analysisValue.getStartDate());
        assertNull(analysisValue.getEndDate());
    }

    /**
     * 단일 세션을 입력으로 주었을 때, 분석 결과가 올바르게 처리되는지 테스트합니다.
     */
    @Test()
    public void testAnalyzeLogPeriodWithSingleSession() {
        List<StudySessionEntity> singleSession = new ArrayList<>();
        StudySessionEntity session = new StudySessionEntity(
                getSubjectId("english"), LocalDate.now(), 45, LocalTime.now(), LocalTime.now().plusMinutes(45), 0);

        singleSession.add(session);

        Optional<FeedbackAnalysis> analysis = FeedbackManager.analayzeLogPeriod(singleSession);

        var analysisValue = analysis.orElse(null);

        assertNotNull(analysisValue);

        assertEquals(1, analysisValue.getTotalSessionCount());
        assertEquals(45, analysisValue.getTotalDurationMinutes());
        assertEquals(45, analysisValue.getAverageSessionDurationMinutes());
        assertEquals(Long.valueOf(45), analysisValue.getSubjectStudyTime().get(getSubjectId("english")));
        assertEquals(session.startTime, analysisValue.getStartDate());
        assertEquals(session.endTime, analysisValue.getEndDate());
    }

    @Test
    public void testAnalyzeLogPeriodWithMultipleSubjects() {
        List<StudySessionEntity> multiSubjectSessions = new ArrayList<>();

        // 다양한 과목 추가
        String[] subjects = { "math", "science", "english", "history", "art" };
        int[] times = { 30, 45, 60, 20, 15 };

        LocalTime baseTime = LocalTime.now().minusHours(5);

        for (int i = 0; i < subjects.length; i++) {
            StudySessionEntity session = new StudySessionEntity(
                    getSubjectId(subjects[i]), LocalDate.now(), times[i], baseTime, baseTime.plusMinutes(times[i]), 0);
            multiSubjectSessions.add(session);
        }

        Optional<FeedbackAnalysis> analysis = FeedbackManager.analayzeLogPeriod(multiSubjectSessions);
        var analysisValue = analysis.orElse(null);

        assertNotNull(analysisValue);

        // 검증
        assertEquals(5, analysisValue.getTotalSessionCount());
        assertEquals(170, analysisValue.getTotalDurationMinutes());
        assertEquals(34, analysisValue.getAverageSessionDurationMinutes());

        // 각 과목별 시간 검증
        for (int i = 0; i < subjects.length; i++) {
            assertEquals(Long.valueOf(times[i]), analysisValue.getSubjectStudyTime().get(getSubjectId(subjects[i])));
        }
    }

    @Test
    public void testGenerateTemperalAdviceWithNoAnalysis() {
        // 분석 결과가 없는 경우
        Optional<FeedbackAnalysis> emptyAnalysis = Optional.empty();

        List<FeedbackAdvice> adviceList = FeedbackManager.generateTemperalAdvice(emptyAnalysis, emptyAnalysis);

        // 분석 결과가 없으면 "데이터 없음" 조언이 있어야 함
        assertEquals(1, adviceList.size());
        assertEquals(FeedbackAdviceType.NO_ANALYSIS, adviceList.get(0).getType());
    }

    @Test
    public void testGenerateTemperalAdviceWithNoRecentSessions() {
        // 전체 분석 결과는 있지만 최근 분석 결과가 없는 경우
        Optional<FeedbackAnalysis> overallAnalysis = Optional.of(createSampleAnalysis(10, 60,
                createSubjectMap(getSubjectId("math"), 300, getSubjectId("science"), 200)));
        Optional<FeedbackAnalysis> emptyRecent = Optional.empty();

        List<FeedbackAdvice> adviceList = FeedbackManager.generateTemperalAdvice(emptyRecent, overallAnalysis);

        // 최근 세션이 없다는 조언이 있어야 함
        assertEquals(1, adviceList.size());
        assertEquals(FeedbackAdviceType.NO_RECENT_SESSION, adviceList.get(0).getType());
    }

    @Test
    public void testGenerateTemperalAdviceWithLongerSessions() {
        // 전체 평균보다 최근 세션이 더 길어진 경우
        Optional<FeedbackAnalysis> overallAnalysis = Optional.of(createSampleAnalysis(10, 50,
                createSubjectMap(getSubjectId("math"), 300, getSubjectId("science"), 200)));
        Optional<FeedbackAnalysis> recentAnalysis = Optional.of(createSampleAnalysis(3, 75,
                createSubjectMap(getSubjectId("math"), 150, getSubjectId("science"), 75)));

        List<FeedbackAdvice> adviceList = FeedbackManager.generateTemperalAdvice(recentAnalysis, overallAnalysis);

        // 세션이 길어졌다는 조언이 있어야 함
        boolean hasLengthenAdvice = adviceList.stream()
                .anyMatch(advice -> advice.getType() == FeedbackAdviceType.RECENT_SESSION_LENGTHEN);
        assertTrue("세션이 길어졌다는 조언이 없습니다", hasLengthenAdvice);
    }

    @Test
    public void testGenerateTemperalAdviceWithShorterSessions() {
        // 전체 평균보다 최근 세션이 더 짧아진 경우
        Optional<FeedbackAnalysis> overallAnalysis = Optional
                .of(createSampleAnalysis(10, 60, createSubjectMap(getSubjectId("math"), 300, getSubjectId("science"),
                        200, getSubjectId("english"), 100)));
        Optional<FeedbackAnalysis> recentAnalysis = Optional.of(
                createSampleAnalysis(3, 30, createSubjectMap(getSubjectId("math"), 60, getSubjectId("science"), 30)));

        List<FeedbackAdvice> adviceList = FeedbackManager.generateTemperalAdvice(recentAnalysis, overallAnalysis);

        // 세션이 짧아졌다는 조언이 있어야 함
        boolean hasShortenAdvice = adviceList.stream()
                .anyMatch(advice -> advice.getType() == FeedbackAdviceType.RECENT_SESSION_SHORTEN);
        assertTrue("세션이 짧아졌다는 조언이 없습니다", hasShortenAdvice);
    }

    @Test
    public void testGenerateTemperalAdviceWithMissingSubject() {
        // 상위 과목 중 하나가 최근에 학습되지 않은 경우
        Optional<FeedbackAnalysis> overallAnalysis = Optional
                .of(createSampleAnalysis(15, 60, createSubjectMap(getSubjectId("math"), 300, getSubjectId("science"),
                        200, getSubjectId("english"), 100)));
        Optional<FeedbackAnalysis> recentAnalysis = Optional.of(
                createSampleAnalysis(5, 60, createSubjectMap(getSubjectId("math"), 200, getSubjectId("english"), 100)));

        List<FeedbackAdvice> adviceList = FeedbackManager.generateTemperalAdvice(recentAnalysis, overallAnalysis);

        // 특정 과목(science)이 최근에 학습되지 않았다는 조언이 있어야 함
        boolean hasNoRecentSubjectAdvice = adviceList.stream()
                .anyMatch(advice -> advice.getType() == FeedbackAdviceType.NO_RECENT_SPECIFIC_SUBJECT &&
                        advice.getSubjectId() != 0 &&
                        advice.getSubjectId() == getSubjectId("science"));
        assertTrue("특정 과목이 최근에 학습되지 않았다는 조언이 없습니다", hasNoRecentSubjectAdvice);
    }

    @Test
    public void testGenerateTemperalAdviceWithNewSubject() {
        // 새로운 과목이 추가된 경우
        Optional<FeedbackAnalysis> overallAnalysis = Optional
                .of(createSampleAnalysis(15, 60, createSubjectMap(getSubjectId("math"), 300, getSubjectId("science"),
                        200, getSubjectId("english"), 100)));
        Optional<FeedbackAnalysis> recentAnalysis = Optional.of(
                createSampleAnalysis(5, 60, createSubjectMap(getSubjectId("math"), 200, getSubjectId("history"), 100)));

        List<FeedbackAdvice> adviceList = FeedbackManager.generateTemperalAdvice(recentAnalysis, overallAnalysis);

        // 새로운 과목(history)에 대한 조언이 있어야 함
        boolean hasNewSubjectAdvice = adviceList.stream()
                .anyMatch(advice -> advice.getType() == FeedbackAdviceType.START_NEW_SUBJECT &&
                        advice.getSubjectId() != 0 &&
                        advice.getSubjectId() == getSubjectId("history"));
        assertTrue("새로운 과목 시작에 대한 조언이 없습니다", hasNewSubjectAdvice);
    }

    @Test
    public void testGenerateBasicAdviceWithShortSessions() {
        // 평균 세션 시간이 짧은 경우
        Optional<FeedbackAnalysis> analysis = Optional.of(createSampleAnalysis(10, 20,
                createSubjectMap(getSubjectId("math"), 100, getSubjectId("science"), 100)));

        List<FeedbackAdvice> adviceList = FeedbackManager.generateBasicAdvice(analysis);

        // 세션이 짧다는 조언이 있어야 함
        boolean hasShortSessionAdvice = adviceList.stream()
                .anyMatch(advice -> advice.getType() == FeedbackAdviceType.AVERAGE_SESSION_SHORT);
        assertTrue("세션이 짧다는 기본 조언이 없습니다", hasShortSessionAdvice);
    }

    @Test
    public void testGenerateBasicAdviceWithLongSessions() {
        // 평균 세션 시간이 긴 경우
        Optional<FeedbackAnalysis> analysis = Optional.of(createSampleAnalysis(10, 100,
                createSubjectMap(getSubjectId("math"), 500, getSubjectId("science"), 500)));

        List<FeedbackAdvice> adviceList = FeedbackManager.generateBasicAdvice(analysis);

        // 세션이 길다는 조언이 있어야 함
        boolean hasLongSessionAdvice = adviceList.stream()
                .anyMatch(advice -> advice.getType() == FeedbackAdviceType.AVERAGE_SESSION_LONG);
        assertTrue("세션이 길다는 기본 조언이 없습니다", hasLongSessionAdvice);
    }

    @Test
    public void testGenerateBasicAdviceWithSubjectConcentration() {
        // 특정 과목에 편중된 경우
        Map<Integer, Long> subjectTimes = new HashMap<>();
        subjectTimes.put(getSubjectId("math"), 600L); // 60%
        subjectTimes.put(getSubjectId("science"), 200L); // 20%
        subjectTimes.put(getSubjectId("english"), 200L); // 20%

        FeedbackAnalysis analysis = createSampleAnalysis(10, 100, subjectTimes);

        List<FeedbackAdvice> adviceList = FeedbackManager.generateBasicAdvice(Optional.of(analysis));

        // 과목 편중 조언이 있어야 함
        boolean hasConcentrationAdvice = adviceList.stream()
                .anyMatch(advice -> advice.getType() == FeedbackAdviceType.SUBJECT_CONCENTRATION &&
                        advice.getSubjectId() != 0 &&
                        advice.getSubjectId() == getSubjectId("math"));
        assertTrue("과목 편중에 대한 기본 조언이 없습니다", hasConcentrationAdvice);
    }

    @Test
    public void testGenerateTemperalAdviceWithNoPatternChanges() {
        // 특별한 패턴 변화가 없는 경우
        Optional<FeedbackAnalysis> overallAnalysis = Optional.of(createSampleAnalysis(15, 60,
                createSubjectMap(getSubjectId("math"), 500, getSubjectId("science"), 400)));
        Optional<FeedbackAnalysis> recentAnalysis = Optional.of(
                createSampleAnalysis(5, 58, createSubjectMap(getSubjectId("math"), 145, getSubjectId("science"), 145)));

        List<FeedbackAdvice> adviceList = FeedbackManager.generateTemperalAdvice(recentAnalysis, overallAnalysis);

        // 특별한 패턴 변화 없음 조언이 있어야 함
        boolean hasNoPatternChangeAdvice = adviceList.stream()
                .anyMatch(advice -> advice.getType() == FeedbackAdviceType.NO_SPECIAL_PATTERN_CHANGED);
        assertTrue("패턴 변화 없음 조언이 없습니다", hasNoPatternChangeAdvice);
    }

    // 테스트 헬퍼 메소드

    /**
     * 테스트를 위한 분석 결과 객체를 생성합니다.
     */
    private FeedbackAnalysis createSampleAnalysis(int sessionCount, int avgDuration,
            Map<Integer, Long> subjectTimes) {
        FeedbackAnalysis analysis = new FeedbackAnalysis();
        analysis.setTotalSessionCount(sessionCount);
        analysis.setAverageSessionDurationMinutes(avgDuration);
        analysis.setTotalDurationMinutes(avgDuration * sessionCount);
        analysis.setSubjectStudyTime(subjectTimes);

        // 날짜 설정 (LocalTime으로 변경)
        analysis.setStartDate(LocalTime.now().minusHours(10));
        analysis.setEndDate(LocalTime.now().minusHours(1));

        return analysis;
    }

    /**
     * 테스트를 위한 과목 시간 맵을 생성합니다.
     */
    private Map<Integer, Long> createSubjectMap(int subject1Id, long time1, int subject2Id, long time2) {
        Map<Integer, Long> map = new HashMap<>();
        map.put(subject1Id, time1);
        map.put(subject2Id, time2);
        return map;
    }

    /**
     * 테스트를 위한 과목 시간 맵을 생성합니다.
     */
    private Map<Integer, Long> createSubjectMap(int subject1Id, long time1, int subject2Id, long time2, int subject3Id,
            long time3) {
        Map<Integer, Long> map = new HashMap<>();
        map.put(subject1Id, time1);
        map.put(subject2Id, time2);
        map.put(subject3Id, time3);
        return map;
    }

    // 테스트용 샘플 데이터 생성 메서드
    private List<StudySessionEntity> createSampleSessions() {
        List<StudySessionEntity> sessions = new ArrayList<>();
        LocalTime baseTime = LocalTime.now();

        // 첫 번째 세션
        StudySessionEntity session1 = new StudySessionEntity(
                getSubjectId("math"), LocalDate.now(), 100, baseTime.minusHours(3), baseTime.minusHours(2), 0);

        // 두 번째 세션
        StudySessionEntity session2 = new StudySessionEntity(
                getSubjectId("science"), LocalDate.now(), 50, baseTime.minusHours(1).minusMinutes(30),
                baseTime.minusHours(1).minusMinutes(30).plusMinutes(50), 0);

        // 세 번째 세션
        StudySessionEntity session3 = new StudySessionEntity(
                getSubjectId("english"), LocalDate.now(), 40, baseTime.minusMinutes(20),
                baseTime.minusMinutes(20).plusMinutes(40), 0);

        sessions.add(session1);
        sessions.add(session2);
        sessions.add(session3);

        return sessions;
    }
}
