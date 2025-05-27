package mp.gradia.utils;

import android.util.Log;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.stream.Collectors;

import mp.gradia.api.models.Subject;
import mp.gradia.api.models.TimetableItem;
import mp.gradia.database.entity.EvaluationRatio;
import mp.gradia.database.entity.SubjectEntity;
import mp.gradia.database.entity.TargetStudyTime;

public class SubjectUtil {
    private static final String TAG = "SubjectUtil";
    public static Subject convertToApiSubject(SubjectEntity localSubject) {
        Subject apiSubject = new Subject();
        apiSubject.setId(String.valueOf(localSubject.getSubjectId()));
        if (localSubject.getServerId() != null) {
            apiSubject.setId(localSubject.getServerId());
        }
        apiSubject.setName(localSubject.getName());
        apiSubject.setType(localSubject.getType());
        apiSubject.setCredit(localSubject.getCredit());
        apiSubject.setDifficulty(localSubject.getDifficulty());
        apiSubject.setMid_term_schedule(localSubject.getMidTermSchedule());
        apiSubject.setFinal_term_schedule(localSubject.getFinalTermSchedule());
        apiSubject.setColor(localSubject.getColor());

        // 생성 시간과 업데이트 시간을 ISO 형식 문자열로 변환
        if (localSubject.getCreatedAt() != null) {
            DateTimeFormatter formatter = DateTimeFormatter.ISO_DATE_TIME;
            apiSubject.setCreated_at(localSubject.getCreatedAt().format(formatter));
        }

        if (localSubject.getUpdatedAt() != null) {
            DateTimeFormatter formatter = DateTimeFormatter.ISO_DATE_TIME;
            apiSubject.setUpdated_at(localSubject.getUpdatedAt().format(formatter));
        }

        // 평가 비율 변환
        if (localSubject.getRatio() != null) {
            mp.gradia.api.models.EvaluationRatio apiRatio = new mp.gradia.api.models.EvaluationRatio();
            mp.gradia.database.entity.EvaluationRatio localRatio = localSubject.getRatio();

            apiRatio.setMid_term_ratio(localRatio.midTermRatio);
            apiRatio.setFinal_term_ratio(localRatio.finalTermRatio);
            apiRatio.setQuiz_ratio(localRatio.quizRatio);
            apiRatio.setAssignment_ratio(localRatio.assignmentRatio);
            apiRatio.setAttendance_ratio(localRatio.attendanceRatio);

            apiSubject.setEvaluation_ratio(apiRatio);
        }

        // 목표 공부 시간 변환
        if (localSubject.getTime() != null) {
            mp.gradia.api.models.TargetStudyTime apiTime = new mp.gradia.api.models.TargetStudyTime();
            mp.gradia.database.entity.TargetStudyTime localTime = localSubject.getTime();

            apiTime.setDaily_target_study_time(localTime.dailyTargetStudyTime);
            apiTime.setWeekly_target_study_time(localTime.weeklyTargetStudyTime);
            apiTime.setMonthly_target_study_time(localTime.monthlyTargetStudyTime);

            apiSubject.setTarget_study_time(apiTime);
        }

        return apiSubject;
    }
    public static List<SubjectEntity> convertTimetableItemsToSubjectEntities(List<TimetableItem> timetableItems) {
        Map<String, Integer> credits = new HashMap<>();
        for (TimetableItem item : timetableItems) {
            String subjectName = item.getName();
            // ex) 16:00
            String startTime = item.getStart_time();
            // ex) 17:00
            String endTime = item.getEnd_time();

            int startHour = Integer.parseInt(startTime.split(":")[0]);
            int startMinute = Integer.parseInt(startTime.split(":")[1]);
            int endHour = Integer.parseInt(endTime.split(":")[0]);
            int endMinute = Integer.parseInt(endTime.split(":")[1]);

            // 60분 마다 1학점으로 계산
            int credit = (endHour * 60 + endMinute - startHour * 60 - startMinute) / 60;

            if (credits.containsKey(subjectName)) {
                credits.put(subjectName, credits.get(subjectName) + credit);
            } else {
                credits.put(subjectName, credit);
            }
        }

        return credits.entrySet().stream()
                .map(entry -> new SubjectEntity(
                        entry.getKey(), // 과목명
                        entry.getValue(), // 학점
                        generateRandomHexColor(),
                        0,
                        "",
                        "",
                        new EvaluationRatio(),
                        new TargetStudyTime(0, 0, 0)
                )).collect(Collectors.toList());
    }
    public static SubjectEntity convertServerToLocalSubject(Subject serverSubject) {
        // 평가 비율 변환
        mp.gradia.database.entity.EvaluationRatio localRatio = null;
        if (serverSubject.getEvaluation_ratio() != null) {
            localRatio = new mp.gradia.database.entity.EvaluationRatio();
            mp.gradia.api.models.EvaluationRatio serverRatio = serverSubject.getEvaluation_ratio();

            localRatio.midTermRatio = serverRatio.getMid_term_ratio();
            localRatio.finalTermRatio = serverRatio.getFinal_term_ratio();
            localRatio.quizRatio = serverRatio.getQuiz_ratio();
            localRatio.assignmentRatio = serverRatio.getAssignment_ratio();
            localRatio.attendanceRatio = serverRatio.getAttendance_ratio();
        }

        // 목표 공부 시간 변환
        mp.gradia.database.entity.TargetStudyTime localTime = null;
        if (serverSubject.getTarget_study_time() != null) {
            mp.gradia.api.models.TargetStudyTime serverTime = serverSubject.getTarget_study_time();

            localTime = new mp.gradia.database.entity.TargetStudyTime(
                    serverTime.getDaily_target_study_time(),
                    serverTime.getWeekly_target_study_time(),
                    serverTime.getMonthly_target_study_time());
        }

        // SubjectEntity 생성
        SubjectEntity localSubject = new SubjectEntity(
                serverSubject.getName(),
                serverSubject.getCredit(),
                serverSubject.getColor(),
                serverSubject.getType(),
                serverSubject.getMid_term_schedule(),
                serverSubject.getFinal_term_schedule(),
                localRatio,
                localTime);

        // 서버 ID 및 기타 필드 설정
        localSubject.setServerId(serverSubject.getId());
        localSubject.setDifficulty(serverSubject.getDifficulty());

        // created_at과 updated_at 설정
        if (serverSubject.getCreated_at() != null) {
            try {
                LocalDateTime createdAt = LocalDateTime.parse(serverSubject.getCreated_at(),
                        DateTimeFormatter.ISO_DATE_TIME);
                localSubject.setCreatedAt(createdAt);
            } catch (Exception e) {
                localSubject.setCreatedAt(LocalDateTime.now());
            }
        } else {
            localSubject.setCreatedAt(LocalDateTime.now());
        }

        if (serverSubject.getUpdated_at() != null) {
            try {
                LocalDateTime updatedAt = LocalDateTime.parse(serverSubject.getUpdated_at(),
                        DateTimeFormatter.ISO_DATE_TIME);
                localSubject.setUpdatedAt(updatedAt);
            } catch (Exception e) {
                localSubject.setUpdatedAt(LocalDateTime.now());
            }
        } else {
            localSubject.setUpdatedAt(LocalDateTime.now());
        }

        return localSubject;
    }
    public static boolean isLocalNewer(SubjectEntity localSubject, mp.gradia.api.models.Subject serverSubject) {
        if (localSubject.getUpdatedAt() == null) {
            return false;
        }

        if (serverSubject.getUpdated_at() == null) {
            return true;
        }

        try {
            LocalDateTime serverUpdatedAt = LocalDateTime.parse(serverSubject.getUpdated_at(),
                    DateTimeFormatter.ISO_DATE_TIME);
            return localSubject.getUpdatedAt().isAfter(serverUpdatedAt);
        } catch (Exception e) {
            return true; // 파싱 오류 시 로컬을 우선
        }
    }
    public static void updateTimestampsFromResponse(SubjectEntity subject, Subject serverSubject) {
        if (serverSubject.getCreated_at() != null) {
            try {
                LocalDateTime createdAt = LocalDateTime.parse(serverSubject.getCreated_at(),
                        DateTimeFormatter.ISO_DATE_TIME);
                subject.setCreatedAt(createdAt);
            } catch (Exception e) {
                Log.e(TAG, "Created_at 변환 오류", e);
            }
        }

        if (serverSubject.getUpdated_at() != null) {
            try {
                LocalDateTime updatedAt = LocalDateTime.parse(serverSubject.getUpdated_at(),
                        DateTimeFormatter.ISO_DATE_TIME);
                subject.setUpdatedAt(updatedAt);
            } catch (Exception e) {
                Log.e(TAG, "Updated_at 변환 오류", e);
            }
        }
    }
    public static String generateRandomHexColor() {
        Random random = new Random();
        int red = random.nextInt(151) + 50;   // 50-200
        int green = random.nextInt(151) + 50; // 50-200
        int blue = random.nextInt(151) + 50;  // 50-200
        return String.format("#%02x%02x%02x", red, green, blue);
    }
}
