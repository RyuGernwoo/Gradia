package mp.gradia.utils;

import android.util.Log;

import java.time.LocalDateTime;
import java.time.LocalTime;

import mp.gradia.api.models.StudySession;
import mp.gradia.database.entity.StudySessionEntity;

public class StudySessionUtil {
    private static final String TAG = "StudySessionUtil";

    /**
     * ISO 8601 형식의 날짜시간 문자열을 LocalDateTime으로 파싱
     * 마이크로초와 타임존 정보를 포함한 형식을 처리
     * 예: "2025-05-26T11:59:33.681000Z" -> LocalDateTime
     * "2023-10-01T10:30:00Z" -> LocalDateTime
     * "2023-10-01T10:30:00" -> LocalDateTime
     */
    public static LocalDateTime parseIsoDateTimeString(String isoDateTimeString) {
        try {
            if (isoDateTimeString == null || isoDateTimeString.isEmpty()) {
                return LocalDateTime.now();
            }

            // Z나 타임존 정보 제거
            String cleanDateTimeString = isoDateTimeString;
            if (cleanDateTimeString.endsWith("Z")) {
                cleanDateTimeString = cleanDateTimeString.substring(0, cleanDateTimeString.length() - 1);
            }

            // 마이크로초 정보가 있는 경우 밀리초까지만 유지 (Java LocalDateTime은 나노초까지 지원하지만 안전하게 처리)
            if (cleanDateTimeString.contains(".")) {
                int dotIndex = cleanDateTimeString.lastIndexOf('.');
                if (dotIndex > 10) { // 날짜 부분 이후의 점만 처리
                    String fractionalPart = cleanDateTimeString.substring(dotIndex + 1);

                    // 마이크로초(6자리)를 밀리초(3자리)로 변환
                    if (fractionalPart.length() > 3) {
                        fractionalPart = fractionalPart.substring(0, 3);
                    }

                    cleanDateTimeString = cleanDateTimeString.substring(0, dotIndex + 1) + fractionalPart;
                }
            }

            // ISO 8601 형식을 LocalDateTime으로 파싱
            return LocalDateTime.parse(cleanDateTimeString);
        } catch (Exception e) {
            Log.e(TAG, "날짜시간 파싱 실패: " + isoDateTimeString, e);
            return LocalDateTime.now();
        }
    }

    /**
     * ISO 8601 형식의 시간 문자열에서 LocalTime 추출
     * 예: "2023-10-01T10:30:00" -> LocalTime.of(10, 30)
     * "2023-10-01T10:30:00Z" -> LocalTime.of(10, 30)
     * "2023-10-01T10:30:00.123Z" -> LocalTime.of(10, 30)
     */
    public static LocalTime parseTimeFromIsoString(String isoTimeString) {
        try {
            if (isoTimeString == null || isoTimeString.isEmpty()) {
                return LocalTime.now();
            }

            // Z나 타임존 정보 제거
            String cleanTimeString = isoTimeString;
            if (cleanTimeString.endsWith("Z")) {
                cleanTimeString = cleanTimeString.substring(0, cleanTimeString.length() - 1);
            }

            // 밀리초 정보가 있는 경우 제거 (예: .123 부분)
            if (cleanTimeString.contains(".")) {
                int dotIndex = cleanTimeString.lastIndexOf('.');
                if (dotIndex > 10) { // 날짜 부분 이후의 점만 처리
                    cleanTimeString = cleanTimeString.substring(0, dotIndex);
                }
            }

            // ISO 8601 형식을 LocalDateTime으로 파싱한 후 시간 부분만 추출
            LocalDateTime dateTime = LocalDateTime.parse(cleanTimeString);
            return dateTime.toLocalTime();
        } catch (Exception e) {
            Log.e(TAG, "시간 파싱 실패: " + isoTimeString, e);
            return LocalTime.now();
        }
    }

    /**
     * StudySessionEntity를 API용 StudySession으로 변환
     * 주의: 현재는 로컬 subject_id를 사용하며, 향후 Subject 동기화 개선 시 수정 필요
     */
    public static StudySession convertToApiSession(StudySessionEntity entity) {
        StudySession apiSession = new StudySession();

        if (entity.getServerId() != null) {
            apiSession.setId(entity.getServerId());
        }

        // subject_id는 서버의 Subject ID를 사용해야 하지만,
        // 현재는 간단히 로컬 ID를 문자열로 변환하여 사용
        // TODO: Subject Repository와 연동하여 serverId를 조회하는 방식으로 개선 필요
        apiSession.setSubject_id(String.valueOf(entity.getServerSubjectId()));
        apiSession.setDate(entity.getDate().toString());
        apiSession.setStudy_time((int) entity.getStudyTime());

        // start time과 end time이 HH:mm 형식으로 저장되어 있기 때문에
        // LocalDateTime으로 변환 후 ISO 8601 형식으로 저장
        // (예: "2023-10-01T10:00:00")
        LocalDateTime startDateTime = LocalDateTime.of(entity.getDate(), entity.getStartTime());
        LocalDateTime endDateTime = LocalDateTime.of(entity.getDate(), entity.getEndTime());

        apiSession.setStart_time(startDateTime.toString());
        apiSession.setEnd_time(endDateTime.toString());

        apiSession.setRest_time((int) entity.getRestTime());

        return apiSession;
    }

    /**
     * 로컬 학습 세션이 서버 세션보다 최신인지 확인
     */
    public static boolean isLocalSessionNewer(StudySessionEntity localSession, StudySession serverSession) {
        if (localSession.getUpdatedAt() == null) {
            return false;
        }

        if (serverSession.getUpdated_at() == null) {
            return true;
        }

        try {
            LocalDateTime serverUpdatedAt = StudySessionUtil.parseIsoDateTimeString(serverSession.getUpdated_at());
            return localSession.getUpdatedAt().isAfter(serverUpdatedAt);
        } catch (Exception e) {
            Log.e(TAG, "서버 업데이트 시간 파싱 오류", e);
            return true; // 파싱 오류 시 로컬을 우선
        }
    }
}
