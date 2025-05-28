package mp.gradia.database.repository;

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
import io.reactivex.rxjava3.disposables.CompositeDisposable;
import io.reactivex.rxjava3.schedulers.Schedulers;
import mp.gradia.api.ApiService;
import mp.gradia.api.AuthManager;
import mp.gradia.api.RetrofitClient;
import mp.gradia.api.models.StudySession;
import mp.gradia.api.models.StudySessionsApiResponse;
import mp.gradia.database.AppDatabase;
import mp.gradia.database.dao.StudySessionDao;
import mp.gradia.database.dao.SubjectDao;
import mp.gradia.database.SubjectIdName;
import mp.gradia.database.entity.DayStudyTime;
import mp.gradia.database.entity.StudySessionEntity;
import mp.gradia.database.entity.SubjectEntity;
import mp.gradia.database.entity.SubjectStudyTime;
import mp.gradia.utils.StudySessionUtil;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class StudySessionRepository {
    private static final String TAG = "StudySessionRepository";

    private final StudySessionDao studySessionDao;
    private final SubjectDao subjectDao;

    private final SubjectRepository subjectRepository;
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
        subjectRepository = new SubjectRepository(context);

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
                studySessionDao.insertAndGetId(session)
                        .subscribeOn(Schedulers.from(executorService))
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe(
                                (generatedId) -> {
                                    Log.d(TAG, "로컬 학습 세션 저장 완료, session_id: " + generatedId);
                                    session.setSessionId(generatedId.intValue());

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
        // 1. 로컬 DB에서 먼저 삭제
        Log.d(TAG, "학습 세션 삭제 요청, session_id: " + session.getSessionId() +
                ", serverId: " + session.getServerId());
        deleteFromLocal(session, callback);

        // 1. 서버에서 먼저 삭제 (서버 ID가 있는 경우)
        if (authManager.isLoggedIn() && session.getServerId() != null) {
            deleteSessionFromServer(session.getServerId(), new CloudSyncCallback() {
                @Override
                public void onSuccess() {
                    Log.d(TAG, "서버 학습 세션 삭제 완료");
                }

                @Override
                public void onError(String message) {
                    Log.w(TAG, "서버 삭제 실패, 로컬만 삭제: " + message);
                }
            });
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
        StudySession apiSession = StudySessionUtil.convertToApiSession(session);

        Log.d(TAG, "서버 학습 세션 생성 요청: " + "subject_id=" + apiSession.getSubject_id() +
                ", date=" + apiSession.getDate() +
                ", start_time=" + apiSession.getStart_time() +
                ", end_time=" + apiSession.getEnd_time() +
                ", study_time=" + apiSession.getStudy_time() +
                ", rest_time=" + apiSession.getRest_time());

        if (apiSession.getSubject_id() == null || apiSession.getSubject_id().isEmpty()) {
            Log.e(TAG, "subject_id가 비어 있습니다. 서버 생성 실패");
            if (callback != null) {
                callback.onError("subject_id가 비어 있습니다.");
            }
            return;
        }

        apiService.createStudySession(authHeader, apiSession).enqueue(new Callback<StudySession>() {
            @Override
            public void onResponse(Call<StudySession> call, Response<StudySession> response) {
                if (response.isSuccessful() && response.body() != null) {
                    StudySession createdSession = response.body();

                    // 서버 ID를 로컬 엔티티에 저장
                    session.setServerId(createdSession.getId());
                    disposables.add(
                            studySessionDao.update(session)
                                    .subscribeOn(Schedulers.from(executorService))
                                    .subscribe(
                                            () -> Log.d(TAG, "서버 ID 저장 완료"),
                                            throwable -> Log.e(TAG, "서버 ID 저장 실패", throwable)));

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
        StudySession apiSession = StudySessionUtil.convertToApiSession(session);

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
     * API StudySession을 로컬 StudySessionEntity로 변환 (비동기)
     */
    private void convertToLocalEntityAsync(StudySession apiSession, ConvertCallback callback) {
        // 날짜 파싱 (YYYY-MM-DD 형식)
        LocalDate date = LocalDate.parse(apiSession.getDate());

        // ISO 8601 시간을 LocalTime으로 변환
        LocalTime startTime = StudySessionUtil.parseTimeFromIsoString(apiSession.getStart_time());
        LocalTime endTime = StudySessionUtil.parseTimeFromIsoString(apiSession.getEnd_time());

        // 서버 subject_id로 로컬 subject 정보 비동기 조회
        disposables.add(
                subjectDao.getAll()
                        .firstElement()
                        .subscribeOn(Schedulers.from(executorService))
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe(
                                subjects -> {
                                    var subject = subjects.stream()
                                            .filter(s -> s.getServerId() != null
                                                    && s.getServerId().equals(apiSession.getSubject_id()))
                                            .findFirst()
                                            .orElse(null);

                                    int localSubjectId;
                                    String subjectName;

                                    if (subject != null) {
                                        localSubjectId = subject.getSubjectId();
                                        subjectName = subject.getName();
                                    } else {
                                        // Subject가 없는 경우 기본값 사용 (나중에 Subject 동기화 시 업데이트됨)
                                        Log.w(TAG, "Subject not found for server_id: " + apiSession.getSubject_id());
                                        return;
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
                                            apiSession.getFocus_level(),
                                            apiSession.getMemo()
                                    );

                                    // 서버 ID 설정
                                    entity.setServerId(apiSession.getId());

                                    // 생성/수정 시간 설정
                                    if (apiSession.getCreated_at() != null) {
                                        try {
                                            entity.setCreatedAt(StudySessionUtil.parseIsoDateTimeString(apiSession.getCreated_at()));
                                        } catch (Exception e) {
                                            Log.w(TAG, "Created_at 파싱 실패: " + apiSession.getCreated_at(), e);
                                            entity.setCreatedAt(LocalDateTime.now());
                                        }
                                    }
                                    if (apiSession.getUpdated_at() != null) {
                                        try {
                                            entity.setUpdatedAt(StudySessionUtil.parseIsoDateTimeString(apiSession.getUpdated_at()));
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
                                }));
    }

    /**
     * 변환 콜백 인터페이스
     */
    public interface ConvertCallback {
        void onSuccess(StudySessionEntity entity);

        void onError(String message);
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
        disposables.add(
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
                                }));
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
     *
     * 1. 로컬 세션 중 서버에 없는 세션은 생성 필요
     * 2. 서버에 있는 세션 중 로컬 세션보다 업데이트 시간이 더 최근인 경우 업데이트 필요
     * 3. 서버에 있는 세션 중 로컬 세션에 없는 경우 삭제 필요
     * 4. 로컬 세션의 Subject가 서버에 없는 경우 Subject 생성 필요
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
        List<Integer> needsSubjectCreate = new ArrayList<>(); // 서버에 Subject 생성 필요

        for (StudySessionEntity localSession : localSessions) {
            if (localSession.getServerSubjectId() == null) {
                // 서버 Subject ID가 없는 경우 - 서버에 Subject 생성 필요
                needsSubjectCreate.add(localSession.getSubjectId());
            }

            if (localSession.getServerId() == null) {
                // 서버 ID가 없으면 서버에 생성 필요
                needsCreate.add(localSession);
            } else if (serverSessionMap.containsKey(localSession.getServerId())) {
                // 서버에 있는 경우 업데이트 시간 비교
                StudySession serverSession = serverSessionMap.get(localSession.getServerId());
                if (StudySessionUtil.isLocalSessionNewer(localSession, serverSession)) {
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


        // 로컬 Subject 동기화가 필요한 경우 먼저 수행
        // 로컬 Subject ID를 서버 ID로 업데이트
        // subject 동기화가 된 후, 이를 기반으로 하는 학습 세션 동기화를 뽑아서 새롭게 수행
        // 단, 세션의 subject가 서버에 없는 경우는 업로드 할 수 없으므로 제외.
        if (!needsSubjectCreate.isEmpty()) {
            Log.d(TAG, "로컬 Subject 동기화 필요: " + needsSubjectCreate.size() + "개");

            Runnable runnable = () -> {
                disposables.add(
                        subjectDao.getAll()
                                .firstElement()
                                .observeOn(Schedulers.from(executorService))
                                .subscribeOn(AndroidSchedulers.mainThread())
                                .subscribe(
                                        subjectList -> {
                                            ArrayList<StudySessionEntity> newNeedsCreate = new ArrayList<>();

                                            for (var session : needsCreate) {
                                                // 세션의 로컬 subjectId가 Subject의 로컬 ID와 일치하는지 확인
                                                SubjectEntity subject = subjectList.stream()
                                                        .filter(s -> s.getSubjectId() == session.getSubjectId())
                                                        .findFirst()
                                                        .orElse(null);

                                                // 서버 ID가 있는 경우에만 동기화
                                                if (subject != null && subject.getServerId() != null) {
                                                    Log.d(TAG, "로컬 Subject ID: " + session.getSubjectId() +
                                                            ", 서버 Subject ID: " + subject.getServerId());

                                                    // 세션의 서버 Subject ID를 업데이트
                                                    session.setServerSubjectId(subject.getServerId());

                                                    // 새로 생성이 필요한 세션 목록에 추가
                                                    newNeedsCreate.add(session);
                                                } else {
                                                    Log.w(TAG, "Subject not found for ID: " + session.getSubjectId());
                                                }
                                            }

                                            performBatchSessionSync(newNeedsCreate, needsUpdate, needsDelete, callback);
                                        },
                                        throwable -> {
                                            Log.e(TAG, "로컬 Subject 조회 실패", throwable);
                                            if (callback != null) {
                                                callback.onError("Subject 조회 실패: " + throwable.getMessage());
                                            }
                                        }));
            };

            // Subject 동기화가 필요한 경우 먼저 수행
            subjectRepository.syncLocalToCloud(new SubjectRepository.CloudSyncCallback() {
                @Override
                public void onSuccess() {
                    Log.d(TAG, "로컬 Subject 동기화 완료");
                    runnable.run();
                }

                @Override
                public void onError(String message) {
                    Log.e(TAG, "로컬 Subject 동기화 실패: " + message);
                    // 동기화 실패 시에도 계속 진행
                    runnable.run();
                    if (callback != null) {
                        callback.onError("Subject 동기화 실패: " + message);
                    }
                }
            });
        } else {
            // Subject 동기화가 필요하지 않은 경우 바로 배치 동기화 수행
            performBatchSessionSync(needsCreate, needsUpdate, needsDelete, callback);
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
                    disposables.add(
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
                                            }));
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
        for (StudySessionEntity session : sessionsArray) {
            Log.d(TAG, "로컬 저장할 학습 세션: " + session.getSubjectName() + " (서버 ID: " + session.getServerId() + ")");
            disposables.add(studySessionDao.insert(session)
                    .subscribeOn(Schedulers.from(executorService))
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(
                            () -> Log.d(TAG, "로컬 학습 세션 저장 완료: " + session.getSubjectName()),
                            throwable -> Log.e(TAG, "로컬 학습 세션 저장 실패: " + session.getSubjectName(), throwable)));
        }
    }

    /**
     * 리소스 해제
     */
    public void dispose() {
        disposables.clear();
    }
}
