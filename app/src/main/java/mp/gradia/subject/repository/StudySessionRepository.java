package mp.gradia.subject.repository;

import android.annotation.SuppressLint;
import android.app.Application;
import android.content.Context;
import android.util.Log;

import androidx.lifecycle.LiveData;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.core.Completable;
import io.reactivex.rxjava3.disposables.CompositeDisposable;
import io.reactivex.rxjava3.schedulers.Schedulers;
import mp.gradia.api.ApiService;
import mp.gradia.api.AuthManager;
import mp.gradia.api.RetrofitClient;
import mp.gradia.api.models.StudySession;
import mp.gradia.api.models.StudySessionsApiResponse;
import mp.gradia.api.models.Subject;
import mp.gradia.api.models.SubjectsApiResponse;
import mp.gradia.database.AppDatabase;
import mp.gradia.database.dao.StudySessionDao;
import mp.gradia.database.dao.SubjectDao;
import mp.gradia.database.SubjectIdName;
import mp.gradia.database.entity.DayStudyTime;
import mp.gradia.database.entity.StudySessionEntity;
import mp.gradia.database.entity.SubjectEntity;
import mp.gradia.database.entity.SubjectStudyTime;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class StudySessionRepository {
    private static final String TAG = "StudySessionRepository";

    private final StudySessionDao studySessionDao;
    private final SubjectDao subjectDao;
    private final ExecutorService executorService = Executors.newSingleThreadExecutor();

    // 클라우드 동기화 관련 필드
    private final ApiService apiService;
    private final AuthManager authManager;
    private final CompositeDisposable disposables = new CompositeDisposable();

    public interface CloudSyncCallback {
        void onSuccess();

        void onError(String message);
    }

    public StudySessionRepository(Context context) {
        AppDatabase db = AppDatabase.getInstance(context);
        studySessionDao = db.studySessionDao();
        subjectDao = db.subjectDao();

        // 클라우드 동기화 초기화
        this.apiService = RetrofitClient.getApiService();
        this.authManager = AuthManager.getInstance(context);
    }

    // 기존 로컬 DB 메서드들
    public LiveData<List<SubjectStudyTime>> getTotalStudyTimeBySubject() {
        return studySessionDao.getTotalStudyTimePerSubject();
    }

    public LiveData<List<SubjectIdName>> getAllSubjectIdNamePairs() {
        return subjectDao.getAllSubjectIdNamePairs();
    }

    public LiveData<List<StudySessionEntity>> getAllSessions() {
        return studySessionDao.getAllSessions();
    }

    public LiveData<Long> getTodayStudyTime() {
        return studySessionDao.getTodayStudyTime(LocalDate.now());
    }

    public LiveData<List<DayStudyTime>> getMonthlyStudyTime() {
        String month = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM"));
        return studySessionDao.getMonthlyStudyTime(month);
    }

    /**
     * 학습 세션 추가 - 로컬 DB 및 서버에 동시 저장
     */
    public void insert(StudySessionEntity session) {
        insert(session, null);
    }

    public void insert(StudySessionEntity session, CloudSyncCallback callback) {
        // 1. 로컬 DB에 먼저 저장
        disposables.add(
                studySessionDao.insert(session)
                        .subscribeOn(Schedulers.from(executorService))
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe(
                                () -> {
                                    Log.d(TAG, "로컬 학습 세션 저장 완료");

                                    // 2. 로그인 상태일 때만 서버에 저장
                                    if (authManager.isLoggedIn()) {
                                        createSessionOnServer(session, callback);
                                    } else {
                                        if (callback != null) {
                                            callback.onSuccess();
                                        }
                                    }
                                },
                                throwable -> {
                                    Log.e(TAG, "로컬 학습 세션 저장 실패", throwable);
                                    if (callback != null) {
                                        callback.onError("로컬 저장 실패: " + throwable.getMessage());
                                    }
                                }));
    }

    /**
     * 학습 세션 업데이트 - 로컬 DB 및 서버에 동시 업데이트
     */
    public void update(StudySessionEntity session) {
        update(session, null);
    }

    public void update(StudySessionEntity session, CloudSyncCallback callback) {
        // 1. 로컬 DB 업데이트
        disposables.add(
                studySessionDao.update(session)
                        .subscribeOn(Schedulers.from(executorService))
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe(
                                () -> {
                                    Log.d(TAG, "로컬 학습 세션 업데이트 완료");
                                    Log.d(TAG, "serverId: " + session.getServerId());

                                    // 2. 서버 ID가 있고 로그인 상태일 때만 서버 업데이트
                                    if (authManager.isLoggedIn() && session.getServerId() != null) {
                                        updateSessionOnServer(session, callback);
                                    } else {
                                        if (callback != null) {
                                            callback.onSuccess();
                                        }
                                    }
                                },
                                throwable -> {
                                    Log.e(TAG, "로컬 학습 세션 업데이트 실패", throwable);
                                    if (callback != null) {
                                        callback.onError("로컬 업데이트 실패: " + throwable.getMessage());
                                    }
                                }));
    }

    /**
     * 학습 세션 삭제 - 로컬 DB 및 서버에서 동시 삭제
     */
    public void delete(StudySessionEntity session) {
        delete(session, null);
    }

    public void delete(StudySessionEntity session, CloudSyncCallback callback) {
        // 1. 서버에서 먼저 삭제 (서버 ID가 있는 경우)
        if (authManager.isLoggedIn() && session.getServerId() != null) {
            deleteSessionFromServer(session.getServerId(), new CloudSyncCallback() {
                @Override
                public void onSuccess() {
                    deleteFromLocal(session, callback);
                }

                @Override
                public void onError(String message) {
                    Log.w(TAG, "서버 삭제 실패, 로컬만 삭제: " + message);
                    deleteFromLocal(session, callback);
                }
            });
        } else {
            deleteFromLocal(session, callback);
        }
    }

    private void deleteFromLocal(StudySessionEntity session, CloudSyncCallback callback) {
        disposables.add(
                studySessionDao.delete(session)
                        .subscribeOn(Schedulers.from(executorService))
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe(
                                () -> {
                                    Log.d(TAG, "로컬 학습 세션 삭제 완료");
                                    if (callback != null) {
                                        callback.onSuccess();
                                    }
                                },
                                throwable -> {
                                    Log.e(TAG, "로컬 학습 세션 삭제 실패", throwable);
                                    if (callback != null) {
                                        callback.onError("로컬 삭제 실패: " + throwable.getMessage());
                                    }
                                }));
    }

    /**
     * 서버에 학습 세션 생성
     */
    private void createSessionOnServer(StudySessionEntity session, CloudSyncCallback callback) {
        String authHeader = authManager.getAuthHeader();
        StudySession apiSession = convertToApiSession(session);

        Log.d(TAG, "서버 학습 세션 생성 요청: " + "subject_id=" + apiSession.getSubject_id() +
                ", date=" + apiSession.getDate() +
                ", start_time=" + apiSession.getStart_time() +
                ", end_time=" + apiSession.getEnd_time() +
                ", study_time=" + apiSession.getStudy_time() +
                ", rest_time=" + apiSession.getRest_time());

        apiService.createStudySession(authHeader, apiSession).enqueue(new Callback<StudySession>() {
            @Override
            public void onResponse(Call<StudySession> call, Response<StudySession> response) {
                if (response.isSuccessful() && response.body() != null) {
                    StudySession createdSession = response.body();

                    // 서버 ID를 로컬 엔티티에 저장
                    session.setServerId(createdSession.getId());
                    studySessionDao.update(session)
                            .subscribeOn(Schedulers.from(executorService))
                            .subscribe(
                                    () -> Log.d(TAG, "서버 ID 저장 완료"),
                                    throwable -> Log.e(TAG, "서버 ID 저장 실패", throwable));

                    Log.d(TAG, "서버 학습 세션 생성 완료");
                    if (callback != null) {
                        callback.onSuccess();
                    }
                } else {
                    Log.e(TAG, "서버 학습 세션 생성 실패: " + response.code());
                    if (callback != null) {
                        callback.onError("서버 생성 실패: " + response.code());
                    }
                }
            }

            @Override
            public void onFailure(Call<StudySession> call, Throwable t) {
                Log.e(TAG, "서버 학습 세션 생성 네트워크 오류", t);
                if (callback != null) {
                    callback.onError("네트워크 오류: " + t.getMessage());
                }
            }
        });
    }

    /**
     * 서버의 학습 세션 업데이트
     */
    private void updateSessionOnServer(StudySessionEntity session, CloudSyncCallback callback) {
        String authHeader = authManager.getAuthHeader();
        String serverId = session.getServerId();
        StudySession apiSession = convertToApiSession(session);

        apiService.updateStudySession(authHeader, serverId, apiSession).enqueue(new Callback<StudySession>() {
            @Override
            public void onResponse(Call<StudySession> call, Response<StudySession> response) {
                if (response.isSuccessful()) {
                    Log.d(TAG, "서버 학습 세션 업데이트 완료");
                    if (callback != null) {
                        callback.onSuccess();
                    }
                } else {
                    Log.e(TAG, "서버 학습 세션 업데이트 실패: " + response.code());
                    if (callback != null) {
                        callback.onError("서버 업데이트 실패: " + response.code());
                    }
                }
            }

            @Override
            public void onFailure(Call<StudySession> call, Throwable t) {
                Log.e(TAG, "서버 학습 세션 업데이트 네트워크 오류", t);
                if (callback != null) {
                    callback.onError("네트워크 오류: " + t.getMessage());
                }
            }
        });
    }

    /**
     * 서버에서 학습 세션 삭제
     */
    private void deleteSessionFromServer(String serverId, CloudSyncCallback callback) {
        String authHeader = authManager.getAuthHeader();

        apiService.deleteStudySession(authHeader, serverId).enqueue(new Callback<Void>() {
            @Override
            public void onResponse(Call<Void> call, Response<Void> response) {
                if (response.isSuccessful()) {
                    Log.d(TAG, "서버 학습 세션 삭제 완료");
                    if (callback != null) {
                        callback.onSuccess();
                    }
                } else {
                    Log.e(TAG, "서버 학습 세션 삭제 실패: " + response.code());
                    if (callback != null) {
                        callback.onError("서버 삭제 실패: " + response.code());
                    }
                }
            }

            @Override
            public void onFailure(Call<Void> call, Throwable t) {
                Log.e(TAG, "서버 학습 세션 삭제 네트워크 오류", t);
                if (callback != null) {
                    callback.onError("네트워크 오류: " + t.getMessage());
                }
            }
        });
    }

    /**
     * StudySessionEntity를 API용 StudySession으로 변환
     * 주의: 현재는 로컬 subject_id를 사용하며, 향후 Subject 동기화 개선 시 수정 필요
     */
    private StudySession convertToApiSession(StudySessionEntity entity) {
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
     * API StudySession을 로컬 StudySessionEntity로 변환 (비동기)
     */
    @SuppressLint("CheckResult")
    private void convertToLocalEntityAsync(StudySession apiSession, ConvertCallback callback) {
        // 날짜 파싱 (YYYY-MM-DD 형식)
        LocalDate date = LocalDate.parse(apiSession.getDate());

        // ISO 8601 시간을 LocalTime으로 변환
        LocalTime startTime = parseTimeFromIsoString(apiSession.getStart_time());
        LocalTime endTime = parseTimeFromIsoString(apiSession.getEnd_time());

        // 서버 subject_id로 로컬 subject 정보 비동기 조회
        subjectDao.getSubjectByServerIdAsync(apiSession.getSubject_id())
                .subscribeOn(Schedulers.from(executorService))
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(
                        subject -> {
                            int localSubjectId;
                            String subjectName;

                            if (subject != null) {
                                localSubjectId = subject.getSubjectId();
                                subjectName = subject.getName();
                            } else {
                                // Subject가 없는 경우 기본값 사용 (나중에 Subject 동기화 시 업데이트됨)
                                localSubjectId = 1; // 임시값
                                subjectName = "Unknown Subject"; // 임시값
                                Log.w(TAG, "Subject not found for server_id: " + apiSession.getSubject_id());
                            }

                            // endDate 계산: startTime + studyTime + restTime
                            long totalMinutes = apiSession.getStudy_time()
                                    + (apiSession.getRest_time() != null ? apiSession.getRest_time() : 0);
                            LocalDate endDate = date;

                            // 시작 시간에 총 시간(분)을 더해서 종료 날짜 계산
                            LocalDateTime startDateTime = LocalDateTime.of(date, startTime);
                            LocalDateTime endDateTime = startDateTime.plusMinutes(totalMinutes);

                            // 날짜가 바뀌었다면 endDate 업데이트
                            if (!endDateTime.toLocalDate().equals(date)) {
                                endDate = endDateTime.toLocalDate();
                            }

                            StudySessionEntity entity = new StudySessionEntity(
                                    localSubjectId,
                                    apiSession.getSubject_id(),
                                    subjectName,
                                    date,
                                    endDate,
                                    apiSession.getStudy_time(),
                                    startTime,
                                    endTime,
                                    apiSession.getRest_time() != null ? apiSession.getRest_time() : 0,
                                    -1, // focusLevel
                                    "" // memo
                            );

                            // 서버 ID 설정
                            entity.setServerId(apiSession.getId());

                            // 생성/수정 시간 설정
                            if (apiSession.getCreated_at() != null) {
                                try {
                                    entity.setCreatedAt(parseIsoDateTimeString(apiSession.getCreated_at()));
                                } catch (Exception e) {
                                    Log.w(TAG, "Created_at 파싱 실패: " + apiSession.getCreated_at(), e);
                                    entity.setCreatedAt(LocalDateTime.now());
                                }
                            }
                            if (apiSession.getUpdated_at() != null) {
                                try {
                                    entity.setUpdatedAt(parseIsoDateTimeString(apiSession.getUpdated_at()));
                                } catch (Exception e) {
                                    Log.w(TAG, "Updated_at 파싱 실패: " + apiSession.getUpdated_at(), e);
                                    entity.setUpdatedAt(LocalDateTime.now());
                                }
                            }

                            callback.onSuccess(entity);
                        },
                        throwable -> {
                            Log.e(TAG, "Subject 조회 실패", throwable);
                            callback.onError("Subject 조회 실패: " + throwable.getMessage());
                        });
    }

    /**
     * 변환 콜백 인터페이스
     */
    public interface ConvertCallback {
        void onSuccess(StudySessionEntity entity);

        void onError(String message);
    }

    /**
     * ISO 8601 형식의 시간 문자열에서 LocalTime 추출
     * 예: "2023-10-01T10:30:00" -> LocalTime.of(10, 30)
     * "2023-10-01T10:30:00Z" -> LocalTime.of(10, 30)
     * "2023-10-01T10:30:00.123Z" -> LocalTime.of(10, 30)
     */
    private LocalTime parseTimeFromIsoString(String isoTimeString) {
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
     * ISO 8601 형식의 날짜시간 문자열을 LocalDateTime으로 파싱
     * 마이크로초와 타임존 정보를 포함한 형식을 처리
     * 예: "2025-05-26T11:59:33.681000Z" -> LocalDateTime
     * "2023-10-01T10:30:00Z" -> LocalDateTime
     * "2023-10-01T10:30:00" -> LocalDateTime
     */
    private LocalDateTime parseIsoDateTimeString(String isoDateTimeString) {
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
     * 로컬 DB 기준으로 클라우드 동기화 수행
     * Fragment 시작 시 호출하여 누락된 동기화를 보완
     */
    public void syncLocalToCloud(CloudSyncCallback callback) {
        if (!authManager.isLoggedIn()) {
            if (callback != null) {
                callback.onError("로그인이 필요합니다.");
            }
            return;
        }

        // 로컬 학습 세션들을 가져와서 동기화 수행
        studySessionDao.getAllFlowable()
                .firstElement()
                .subscribeOn(Schedulers.from(executorService))
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(
                        localSessions -> {
                            if (localSessions.isEmpty()) {
                                if (callback != null)
                                    callback.onSuccess();
                                return;
                            }

                            // 서버에서 학습 세션 목록을 가져와서 비교
                            syncWithServerSessions(localSessions, callback);
                        },
                        throwable -> {
                            Log.e(TAG, "로컬 학습 세션 조회 실패", throwable);
                            if (callback != null) {
                                callback.onError("로컬 데이터 조회 실패: " + throwable.getMessage());
                            }
                        }
                );
    }

    /**
     * 서버 학습 세션 목록과 비교하여 동기화 수행
     */
    private void syncWithServerSessions(List<StudySessionEntity> localSessions, CloudSyncCallback callback) {
        String authHeader = authManager.getAuthHeader();

        apiService.getStudySessions(authHeader, null).enqueue(new Callback<StudySessionsApiResponse>() {
            @Override
            public void onResponse(Call<StudySessionsApiResponse> call,
                    Response<StudySessionsApiResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    List<StudySession> serverSessions = response.body().getSessions();
                    performLocalToCloudSync(serverSessions, localSessions, callback);
                } else {
                    Log.e(TAG, "서버 학습 세션 조회 실패: " + response.code());
                    if (callback != null) {
                        callback.onError("서버 학습 세션 조회 실패: " + response.code());
                    }
                }
            }

            @Override
            public void onFailure(Call<StudySessionsApiResponse> call, Throwable t) {
                Log.e(TAG, "서버 학습 세션 조회 네트워크 오류", t);
                if (callback != null) {
                    callback.onError("서버 연결 실패: " + t.getMessage());
                }
            }
        });
    }

    /**
     * 로컬 학습 세션들을 서버와 비교하여 필요한 동기화 수행
     */
    public void performLocalToCloudSync(List<StudySession> serverSessions, List<StudySessionEntity> localSessions,
            CloudSyncCallback callback) {
        // 서버 세션들을 맵으로 변환 (serverId -> StudySession)
        Map<String, StudySession> serverSessionMap = new HashMap<>();
        for (StudySession serverSession : serverSessions) {
            if (serverSession.getId() != null) {
                serverSessionMap.put(serverSession.getId(), serverSession);
            }
        }

        // 로컬 세션들의 서버 ID 목록 생성
        Set<String> localServerIds = new HashSet<>();
        for (StudySessionEntity localSession : localSessions) {
            if (localSession.getServerId() != null) {
                localServerIds.add(localSession.getServerId());
            }
        }

        // 동기화가 필요한 세션들 분류
        List<StudySessionEntity> needsCreate = new ArrayList<>(); // 서버에 생성 필요
        List<StudySessionEntity> needsUpdate = new ArrayList<>(); // 서버에 업데이트 필요
        List<StudySession> needsDelete = new ArrayList<>(); // 서버에서 삭제 필요

        for (StudySessionEntity localSession : localSessions) {
            if (localSession.getServerId() == null) {
                // 서버 ID가 없으면 서버에 생성 필요
                needsCreate.add(localSession);
            } else if (serverSessionMap.containsKey(localSession.getServerId())) {
                // 서버에 있는 경우 업데이트 시간 비교
                StudySession serverSession = serverSessionMap.get(localSession.getServerId());
                if (isLocalSessionNewer(localSession, serverSession)) {
                    needsUpdate.add(localSession);
                }
            } else {
                // 서버 ID는 있지만 서버에 없는 경우 - 서버에 다시 생성
                localSession.setServerId(null); // 서버 ID 제거
                needsCreate.add(localSession);
            }
        }

        // 서버에는 있지만 로컬에 없는 세션들 찾기 (삭제 필요)
        for (StudySession serverSession : serverSessions) {
            if (serverSession.getId() != null && !localServerIds.contains(serverSession.getId())) {
                needsDelete.add(serverSession);
            }
        }

        Log.d(TAG, "동기화 필요: 생성 " + needsCreate.size() + "개, 업데이트 " + needsUpdate.size() + "개, 삭제 " + needsDelete.size()
                + "개");

        // 동기화 수행
        if (needsCreate.isEmpty() && needsUpdate.isEmpty() && needsDelete.isEmpty()) {
            Log.d(TAG, "동기화할 항목이 없습니다.");
            if (callback != null)
                callback.onSuccess();
            return;
        }

        performBatchSessionSync(needsCreate, needsUpdate, needsDelete, callback);
    }

    /**
     * 로컬 학습 세션이 서버 세션보다 최신인지 확인
     */
    private boolean isLocalSessionNewer(StudySessionEntity localSession, StudySession serverSession) {
        if (localSession.getUpdatedAt() == null) {
            return false;
        }

        if (serverSession.getUpdated_at() == null) {
            return true;
        }

        try {
            LocalDateTime serverUpdatedAt = parseIsoDateTimeString(serverSession.getUpdated_at());
            return localSession.getUpdatedAt().isAfter(serverUpdatedAt);
        } catch (Exception e) {
            Log.e(TAG, "서버 업데이트 시간 파싱 오류", e);
            return true; // 파싱 오류 시 로컬을 우선
        }
    }

    /**
     * 배치로 학습 세션 동기화 수행
     */
    private void performBatchSessionSync(List<StudySessionEntity> needsCreate, List<StudySessionEntity> needsUpdate,
            List<StudySession> needsDelete, CloudSyncCallback callback) {

        int totalItems = needsCreate.size() + needsUpdate.size() + needsDelete.size();
        AtomicInteger completedItems = new AtomicInteger(0);
        AtomicBoolean hasError = new AtomicBoolean(false);

        Runnable checkCompletion = () -> {
            if (completedItems.get() >= totalItems) {
                if (callback != null) {
                    if (hasError.get()) {
                        callback.onError("일부 항목 동기화에 실패했습니다.");
                    } else {
                        callback.onSuccess();
                    }
                }
            }
        };

        // 생성이 필요한 세션들 처리
        for (StudySessionEntity session : needsCreate) {
            createSessionOnServer(session, new CloudSyncCallback() {
                @Override
                public void onSuccess() {
                    Log.d(TAG, "학습 세션 생성 동기화 완료");
                    completedItems.incrementAndGet();
                    checkCompletion.run();
                }

                @Override
                public void onError(String message) {
                    Log.e(TAG, "학습 세션 생성 동기화 실패: " + message);
                    hasError.set(true);
                    completedItems.incrementAndGet();
                    checkCompletion.run();
                }
            });
        }

        // 업데이트가 필요한 세션들 처리
        for (StudySessionEntity session : needsUpdate) {
            updateSessionOnServer(session, new CloudSyncCallback() {
                @Override
                public void onSuccess() {
                    Log.d(TAG, "학습 세션 업데이트 동기화 완료");
                    completedItems.incrementAndGet();
                    checkCompletion.run();
                }

                @Override
                public void onError(String message) {
                    Log.e(TAG, "학습 세션 업데이트 동기화 실패: " + message);
                    hasError.set(true);
                    completedItems.incrementAndGet();
                    checkCompletion.run();
                }
            });
        }

        // 삭제가 필요한 세션들 처리
        for (StudySession session : needsDelete) {
            deleteSessionFromServer(session.getId(), new CloudSyncCallback() {
                @Override
                public void onSuccess() {
                    Log.d(TAG, "서버 학습 세션 삭제 동기화 완료");
                    completedItems.incrementAndGet();
                    checkCompletion.run();
                }

                @Override
                public void onError(String message) {
                    Log.e(TAG, "서버 학습 세션 삭제 동기화 실패: " + message);
                    hasError.set(true);
                    completedItems.incrementAndGet();
                    checkCompletion.run();
                }
            });
        }
    }

    /**
     * 서버에서 학습 세션 데이터를 다운로드하여 로컬 DB를 완전히 교체합니다.
     * 로그인 시 사용되는 메서드로, 기존 로컬 데이터를 모두 삭제하고 서버 데이터로 대체합니다.
     */
    public void downloadAndReplaceFromServer(CloudSyncCallback callback) {
        if (!authManager.isLoggedIn()) {
            if (callback != null) {
                callback.onError("로그인이 필요합니다.");
            }
            return;
        }

        String authHeader = authManager.getAuthHeader();

        apiService.getStudySessions(authHeader, null).enqueue(new Callback<StudySessionsApiResponse>() {
            @Override
            public void onResponse(Call<StudySessionsApiResponse> call,
                    Response<StudySessionsApiResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    List<StudySession> serverSessions = response.body().getSessions();

                    Log.d(TAG, "서버에서 학습 세션 데이터 가져오기 성공: " + serverSessions.size() + "개");

                    // 1. 기존 로컬 데이터 삭제
                    studySessionDao.clearAll()
                            .subscribeOn(Schedulers.from(executorService))
                            .observeOn(AndroidSchedulers.mainThread())
                            .subscribe(
                                    () -> {
                                        Log.d(TAG, "기존 학습 세션 데이터 삭제 완료");

                                        // 2. 서버 데이터를 로컬 데이터로 변환하여 저장
                                        insertServerSessionsToLocal(serverSessions, callback);
                                    },
                                    throwable -> {
                                        Log.e(TAG, "기존 학습 세션 데이터 삭제 실패", throwable);
                                        if (callback != null) {
                                            callback.onError("기존 데이터 삭제 실패: " + throwable.getMessage());
                                        }
                                    });
                } else {
                    String errorMsg = "서버에서 학습 세션 데이터를 가져오는데 실패했습니다. 응답 코드: " + response.code();
                    Log.e(TAG, errorMsg);
                    if (callback != null) {
                        callback.onError(errorMsg);
                    }
                }
            }

            @Override
            public void onFailure(Call<StudySessionsApiResponse> call, Throwable t) {
                Log.e(TAG, "서버 통신 실패", t);
                if (callback != null) {
                    callback.onError("서버 연결 실패: " + t.getMessage());
                }
            }
        });
    }

    /**
     * 서버에서 받은 학습 세션 데이터를 로컬 DB에 저장합니다.
     */
    private void insertServerSessionsToLocal(List<StudySession> serverSessions, CloudSyncCallback callback) {
        if (serverSessions.isEmpty()) {
            Log.d(TAG, "서버에 학습 세션 데이터가 없습니다.");
            if (callback != null) {
                callback.onSuccess();
            }
            return;
        }

        List<StudySessionEntity> localSessions = new ArrayList<>();
        AtomicInteger convertedCount = new AtomicInteger(0);
        AtomicBoolean hasError = new AtomicBoolean(false);

        // 서버 데이터를 로컬 엔티티로 비동기 변환
        for (StudySession serverSession : serverSessions) {
            convertToLocalEntityAsync(serverSession, new ConvertCallback() {
                @Override
                public void onSuccess(StudySessionEntity localSession) {
                    synchronized (localSessions) {
                        localSessions.add(localSession);
                        Log.d(TAG, "변환된 학습 세션: " + localSession.getSubjectName() + " (서버 ID: "
                                + localSession.getServerId() + ")");
                    }

                    int completed = convertedCount.incrementAndGet();
                    if (completed >= serverSessions.size()) {
                        // 모든 변환이 완료되면 DB에 저장
                        saveConvertedSessionsToDb(localSessions, callback, hasError.get());
                    }
                }

                @Override
                public void onError(String message) {
                    Log.e(TAG, "학습 세션 변환 중 오류: " + serverSession.getId() + ", " + message);
                    hasError.set(true);

                    int completed = convertedCount.incrementAndGet();
                    if (completed >= serverSessions.size()) {
                        // 모든 변환이 완료되면 DB에 저장 (오류가 있어도 변환된 것들은 저장)
                        saveConvertedSessionsToDb(localSessions, callback, hasError.get());
                    }
                }
            });
        }
    }

    /**
     * 변환된 세션들을 DB에 저장
     */
    private void saveConvertedSessionsToDb(List<StudySessionEntity> localSessions, CloudSyncCallback callback,
            boolean hasError) {
        if (localSessions.isEmpty()) {
            Log.w(TAG, "변환된 학습 세션이 없습니다.");
            if (callback != null) {
                if (hasError) {
                    callback.onError("모든 학습 세션 변환에 실패했습니다.");
                } else {
                    callback.onSuccess();
                }
            }
            return;
        }

        // 로컬 DB에 일괄 저장
        StudySessionEntity[] sessionsArray = localSessions.toArray(new StudySessionEntity[0]);
        studySessionDao.insert(sessionsArray)
                .subscribeOn(Schedulers.from(executorService))
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(
                        () -> {
                            Log.d(TAG, "서버 학습 세션 데이터 로컬 저장 완료: " + localSessions.size() + "개");
                            if (callback != null) {
                                if (hasError) {
                                    callback.onError("일부 학습 세션 변환에 실패했지만 저장은 완료되었습니다.");
                                } else {
                                    callback.onSuccess();
                                }
                            }
                        },
                        throwable -> {
                            Log.e(TAG, "서버 학습 세션 데이터 로컬 저장 실패", throwable);
                            if (callback != null) {
                                callback.onError("데이터 저장 실패: " + throwable.getMessage());
                            }
                        });
    }

    /**
     * 리소스 해제
     */
    public void dispose() {
        disposables.clear();
    }
}
