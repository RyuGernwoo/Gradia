package mp.gradia.subject.repository;

import android.app.Application;
import android.content.Context;
import android.util.Log;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.LiveDataReactiveStreams;

import java.time.LocalDateTime;
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
import io.reactivex.rxjava3.core.Single;
import io.reactivex.rxjava3.disposables.CompositeDisposable;
import io.reactivex.rxjava3.schedulers.Schedulers;
import mp.gradia.api.ApiService;
import mp.gradia.api.AuthManager;
import mp.gradia.api.RetrofitClient;
import mp.gradia.api.models.EvaluationRatio;
import mp.gradia.api.models.Subject;
import mp.gradia.api.models.SubjectsApiResponse;
import mp.gradia.api.models.TargetStudyTime;
import mp.gradia.database.AppDatabase;
import mp.gradia.database.dao.SubjectDao;
import mp.gradia.database.entity.SubjectEntity;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class SubjectRepository {
    private static final String TAG = "SubjectRepository";

    private final SubjectDao subjectDao;
    // 모든 과목 데이터를 LiveData 형태로 보관
    private final LiveData<List<SubjectEntity>> allSubjects;
    private final ExecutorService executorService = Executors.newSingleThreadExecutor();

    // 클라우드 동기화 관련 필드
    private final ApiService apiService;
    private final AuthManager authManager;
    private final CompositeDisposable disposables = new CompositeDisposable();

    public interface CloudSyncCallback {
        void onSuccess();

        void onError(String message);
    }

    // DB 인스턴스를 가져오고 DAO 초기화
    public SubjectRepository(Context context) {
        AppDatabase db = AppDatabase.getInstance(context);
        subjectDao = db.subjectDao();
        allSubjects = LiveDataReactiveStreams.fromPublisher(subjectDao.getAll()); // 초기화 시 전체 과목 조회

        // 클라우드 동기화 초기화
        this.apiService = RetrofitClient.getApiService();
        this.authManager = AuthManager.getInstance(context);
    }

    // 전체 과목 목록 반환
    public LiveData<List<SubjectEntity>> getAllSubjects() {
        return allSubjects;
    }

    // 특정 ID로 과목 하나 조회
    public LiveData<SubjectEntity> getSubjectById(int id) {
        // LiveDataReactiveStreams.fromPublisher()를 사용하여 Flowable을 LiveData로 변환
        return LiveDataReactiveStreams.fromPublisher(subjectDao.getByIdFlowable(id));
    }

    /**
     * 과목 추가 - 로컬 DB 및 서버에 동시 저장
     */
    public void insert(SubjectEntity subject) {
        insert(subject, null);
    }

    public void insert(SubjectEntity subject, CloudSyncCallback callback) {
        // 1. 먼저 로컬 DB에 저장
        Single<Long> localInsert = subjectDao.insertAndGetId(subject)
                .subscribeOn(Schedulers.from(executorService))
                .doOnSuccess(generatedId -> {
                    subject.setSubjectId(generatedId.intValue());
                    Log.d(TAG, "로컬 DB에 과목 저장 완료: " + subject.getSubjectId());
                });

        // 2. 서버에도 저장
        if (authManager.isLoggedIn()) {
            disposables.add(
                    localInsert
                            .observeOn(AndroidSchedulers.mainThread())
                            .subscribe(
                                    (generatedId) -> {
                                        Log.d(TAG, "로컬 과목 생성 완료: " + subject.getSubjectId());
                                        if (callback != null)
                                            callback.onSuccess();

                                        // 서버 업로드는 별도로 비동기 수행
                                        disposables.add(uploadSubjectToServer(subject)
                                                .subscribeOn(Schedulers.io())
                                                .observeOn(AndroidSchedulers.mainThread())
                                                .subscribe(
                                                        () -> Log.d(TAG, "서버 동기화 완료: " + subject.getName()),
                                                        throwable -> Log.w(TAG,
                                                                "서버 동기화 실패 (나중에 재시도): " + subject.getName(),
                                                                throwable)));
                                    },
                                    throwable -> {
                                        Log.e(TAG, "과목 생성 실패", throwable);
                                        if (callback != null)
                                            callback.onError("과목 생성 실패: " + throwable.getMessage());
                                    }));
        } else {
            // 로그인되지 않은 경우 로컬 DB만 업데이트
            disposables.add(
                    localInsert
                            .observeOn(AndroidSchedulers.mainThread())
                            .subscribe(
                                    (generatedId) -> {
                                        Log.d(TAG, "과목 생성 완료 (오프라인): " + subject.getName());
                                        if (callback != null)
                                            callback.onSuccess();
                                    },
                                    throwable -> {
                                        Log.e(TAG, "과목 생성 실패", throwable);
                                        if (callback != null)
                                            callback.onError("과목 생성 실패: " + throwable.getMessage());
                                    }));
        }
    }

    /**
     * 과목 업데이트 - 로컬 DB 및 서버에 동시 업데이트
     */
    public void update(SubjectEntity subject) {
        update(subject, null);
    }

    public void update(SubjectEntity subject, CloudSyncCallback callback) {
        // 업데이트 시간 설정
        subject.setUpdatedAt(LocalDateTime.now());

        // 1. 먼저 로컬 DB에 업데이트
        Completable localUpdate = subjectDao.update(subject)
                .subscribeOn(Schedulers.from(executorService))
                .doOnComplete(() -> Log.d(TAG, "로컬 DB에 과목 업데이트 완료: " + subject.getName()));

        Log.d(TAG, "subject server Id: " + subject.getServerId());
        Log.d(TAG, "authManager.isLoggedIn(): " + authManager.isLoggedIn());
        Log.d(TAG, "업데이트할 과목명: " + subject.getName());

        // 2. 서버에도 업데이트
        if (authManager.isLoggedIn() && subject.getServerId() != null) {
            Log.d(TAG, "서버 동기화 조건 만족 - 서버 업데이트 진행");
            disposables.add(localUpdate
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(
                            () -> {
                                Log.d(TAG, "로컬 과목 업데이트 완료: " + subject.getName());
                                if (callback != null)
                                    callback.onSuccess();

                                // 서버 업데이트는 별도로 비동기 수행
                                disposables.add(
                                        updateSubjectOnServer(subject)
                                                .subscribeOn(Schedulers.io())
                                                .observeOn(AndroidSchedulers.mainThread())
                                                .subscribe(
                                                        () -> Log.d(TAG, "서버 동기화 완료: " + subject.getName()),
                                                        throwable -> Log.w(TAG,
                                                                "서버 동기화 실패 (나중에 재시도): " + subject.getName(),
                                                                throwable)));
                            },
                            throwable -> {
                                Log.e(TAG, "과목 업데이트 실패", throwable);
                                if (callback != null)
                                    callback.onError("과목 업데이트 실패: " + throwable.getMessage());
                            }));
        } else {
            Log.d(TAG, "서버 동기화 조건 불만족 - 로컬 DB만 업데이트");
            if (!authManager.isLoggedIn()) {
                Log.d(TAG, "로그인되지 않음");
            }
            if (subject.getServerId() == null) {
                Log.d(TAG, "serverId가 null임");
            }

            // 로그인되지 않았거나 서버 ID가 없는 경우 로컬 DB만 업데이트
            disposables.add(localUpdate
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(
                            () -> {
                                Log.d(TAG, "과목 업데이트 완료 (오프라인): " + subject.getName());
                                if (callback != null)
                                    callback.onSuccess();
                            },
                            throwable -> {
                                Log.e(TAG, "과목 업데이트 실패", throwable);
                                if (callback != null)
                                    callback.onError("과목 업데이트 실패: " + throwable.getMessage());
                            }));
        }
    }

    /**
     * 과목 삭제 - 로컬 DB 및 서버에서 동시 삭제
     */
    public void delete(SubjectEntity subject) {
        delete(subject, null);
    }

    public void delete(SubjectEntity subject, CloudSyncCallback callback) {
        Log.d(TAG, "[delete] 메소드 시작. 과목: " + subject.getName() + ", 서버 ID: " + subject.getServerId() + ", 로그인 상태: "
                + authManager.isLoggedIn());

        Completable localDelete = subjectDao.delete(subject)
                .subscribeOn(Schedulers.from(executorService))
                .doOnComplete(() -> Log.d(TAG, "[delete] localDelete.doOnComplete (로컬 DB 삭제 완료): " + subject.getName()))
                .doOnError(
                        throwable -> Log.e(TAG, "[delete] localDelete.doOnError (로컬 DB 삭제 실패): " + subject.getName()))
                .doOnDispose(() -> Log.w(TAG, "[delete] localDelete.doOnDispose: " + subject.getName() + " 스트림 해제됨!"));

        if (authManager.isLoggedIn() && subject.getServerId() != null) {
            Log.d(TAG, "[delete] 서버 삭제 조건 충족. 로컬 삭제 후 서버 삭제 예정: " + subject.getName());
            disposables.add(
                    localDelete
                            .observeOn(AndroidSchedulers.mainThread())
                            .subscribe(
                                    () -> { // 로컬 삭제 성공 시
                                        Log.i(TAG, "[delete] localDelete.subscribe onSuccess (로컬 과목 삭제 성공 콜백 시작): "
                                                + subject.getName());

                                        if (callback != null) {
                                            callback.onSuccess();
                                        }

                                        disposables.add(
                                                deleteSubjectFromServer(subject)
                                                        .subscribeOn(Schedulers.io())
                                                        .observeOn(AndroidSchedulers.mainThread())
                                                        .subscribe(
                                                                () -> Log.i(TAG,
                                                                        "[delete] deleteSubjectFromServer.subscribe onSuccess (서버 과목 삭제 성공): "
                                                                                + subject.getName()),
                                                                serverError -> Log.e(TAG,
                                                                        "[delete] deleteSubjectFromServer.subscribe onError (서버 과목 삭제 실패): "
                                                                                + subject.getName(),
                                                                        serverError)));
                                        Log.d(TAG, "[delete] deleteSubjectFromServer 호출 시도 후 (disposables.add 완료): "
                                                + subject.getName());
                                    },
                                    localError -> { // 로컬 삭제 실패 시
                                        Log.e(TAG, "[delete] localDelete.subscribe onError (로컬 과목 삭제 실패): "
                                                + subject.getName(), localError);
                                        if (callback != null) {
                                            callback.onError("로컬 과목 삭제 실패: " + localError.getMessage());
                                        }
                                    }));
        } else {
            Log.d(TAG, "[delete] 서버 삭제 조건 불충족 (로그아웃 또는 서버 ID 없음). 로컬 삭제만 진행: " + subject.getName());
            disposables.add(
                    localDelete
                            .observeOn(AndroidSchedulers.mainThread())
                            .subscribe(
                                    () -> {
                                        Log.d(TAG, "[delete] 과목 삭제 완료 (오프라인/서버ID 없음): " + subject.getName());
                                        if (callback != null)
                                            callback.onSuccess();
                                    },
                                    throwable -> {
                                        Log.e(TAG, "[delete] 과목 삭제 실패 (오프라인/서버ID 없음): " + subject.getName(), throwable);
                                        if (callback != null)
                                            callback.onError("과목 삭제 실패: " + throwable.getMessage());
                                    }));
        }
    }

    /**
     * 서버에 과목 업로드
     */
    private Completable uploadSubjectToServer(SubjectEntity subject) {
        return Completable.create(emitter -> {
            String authHeader = authManager.getAuthHeader();
            Subject apiSubject = convertToApiSubject(subject);
            apiSubject.setId(null); // 새 과목이므로 ID는 null로 설정

            apiService.createSubject(authHeader, apiSubject).enqueue(new Callback<Subject>() {
                @Override
                public void onResponse(Call<Subject> call, Response<Subject> response) {
                    if (response.isSuccessful() && response.body() != null) {
                        // 서버에서 생성된 ID를 로컬에 저장
                        String serverId = response.body().getId();
                        if (serverId != null) {
                            subject.setServerId(serverId);

                            // 서버에서 받은 생성/업데이트 시간 설정
                            updateTimestampsFromResponse(subject, response.body());

                            // 로컬 DB 업데이트 완료 후 emitter.onComplete() 호출
                            disposables.add(subjectDao.update(subject)
                                    .subscribeOn(Schedulers.from(executorService))
                                    .subscribe(
                                            () -> {
                                                Log.d(TAG, "서버 ID 저장 완료: " + serverId);
                                                emitter.onComplete();
                                            },
                                            error -> {
                                                Log.e(TAG, "서버 ID 저장 실패", error);
                                                emitter.onError(error);
                                            }));
                        } else {
                            emitter.onComplete();
                        }
                    } else {
                        emitter.onError(new Exception("서버에 과목 생성 실패: " + response.code()));
                    }
                }

                @Override
                public void onFailure(Call<Subject> call, Throwable t) {
                    emitter.onError(t);
                }
            });
        });
    }

    /**
     * 서버의 과목 업데이트
     */
    private Completable updateSubjectOnServer(SubjectEntity subject) {
        return Completable.create(emitter -> {
            String authHeader = authManager.getAuthHeader();
            String serverId = subject.getServerId();
            Subject apiSubject = convertToApiSubject(subject);

            apiService.updateSubject(authHeader, serverId, apiSubject).enqueue(new Callback<Subject>() {
                @Override
                public void onResponse(Call<Subject> call, Response<Subject> response) {
                    if (response.isSuccessful() && response.body() != null) {
                        // 서버에서 받은 업데이트 시간 설정
                        updateTimestampsFromResponse(subject, response.body());

                        // 로컬 DB 업데이트
                        disposables.add(subjectDao.update(subject)
                                .subscribeOn(Schedulers.from(executorService))
                                .subscribe(
                                        () -> Log.d(TAG, "서버 타임스탬프 업데이트 완료"),
                                        error -> Log.e(TAG, "서버 타임스탬프 업데이트 실패", error)));

                        emitter.onComplete();
                    } else {
                        emitter.onError(new Exception("서버에 과목 업데이트 실패: " + response.code()));
                    }
                }

                @Override
                public void onFailure(Call<Subject> call, Throwable t) {
                    emitter.onError(t);
                }
            });
        });
    }

    /**
     * 서버에서 과목 삭제
     */
    private Completable deleteSubjectFromServer(SubjectEntity subject) {
        Log.d(TAG, "[deleteSubjectFromServer] 메소드 호출됨. Subject: " + subject.getName() + ", Server ID: "
                + subject.getServerId()); // 이 로그가 보여야 합니다.
        return Completable.create(emitter -> {
            String authHeader = authManager.getAuthHeader();
            String serverId = subject.getServerId();

            if (serverId == null) {
                emitter.onError(new Exception("서버 ID가 없습니다. 로컬 DB에서만 삭제합니다."));
                return;
            }

            Log.d(TAG, "서버 API 삭제 요청: " + serverId);

            apiService.deleteSubject(authHeader, serverId).enqueue(new Callback<Void>() {
                @Override
                public void onResponse(Call<Void> call, Response<Void> response) {
                    if (response.isSuccessful()) {
                        Log.d(TAG, "서버 과목 삭제 성공: " + subject.getName());
                        emitter.onComplete();
                    } else {
                        Log.e(TAG, "서버 과목 삭제 실패: " + response.code());
                        emitter.onError(new Exception("서버에서 과목 삭제 실패: " + response.code()));
                    }
                }

                @Override
                public void onFailure(Call<Void> call, Throwable t) {
                    Log.d(TAG, "서버 과목 삭제 네트워크 오류", t);
                    emitter.onError(t);
                }
            });
        });
    }

    /**
     * 서버 응답에서 타임스탬프 업데이트
     */
    private void updateTimestampsFromResponse(SubjectEntity subject, Subject serverSubject) {
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

    /**
     * 로컬 SubjectEntity를 API Subject 모델로 변환합니다.
     */
    private Subject convertToApiSubject(SubjectEntity localSubject) {
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
            EvaluationRatio apiRatio = new EvaluationRatio();
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
            TargetStudyTime apiTime = new TargetStudyTime();
            mp.gradia.database.entity.TargetStudyTime localTime = localSubject.getTime();

            apiTime.setDaily_target_study_time(localTime.dailyTargetStudyTime);
            apiTime.setWeekly_target_study_time(localTime.weeklyTargetStudyTime);
            apiTime.setMonthly_target_study_time(localTime.monthlyTargetStudyTime);

            apiSubject.setTarget_study_time(apiTime);
        }

        return apiSubject;
    }

    /**
     * 리소스 해제
     */
    public void dispose() {
        disposables.clear();
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

        // 로컬 과목들을 가져와서 동기화 수행
        disposables.add(subjectDao.getAll()
                .firstElement()
                .subscribeOn(Schedulers.from(executorService))
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(
                        localSubjects -> {
                            if (localSubjects.isEmpty()) {
                                if (callback != null)
                                    callback.onSuccess();
                                return;
                            }

                            // 서버에서 과목 목록을 가져와서 비교
                            syncWithServerSubjects(localSubjects, callback);
                        },
                        throwable -> {
                            Log.e(TAG, "로컬 과목 조회 실패", throwable);
                            if (callback != null) {
                                callback.onError("로컬 데이터 조회 실패: " + throwable.getMessage());
                            }
                        }));
    }

    /**
     * 서버 과목 목록과 비교하여 동기화 수행
     */
    private void syncWithServerSubjects(List<SubjectEntity> localSubjects, CloudSyncCallback callback) {
        String authHeader = authManager.getAuthHeader();

        apiService.getSubjects(authHeader).enqueue(new Callback<SubjectsApiResponse>() {
            @Override
            public void onResponse(Call<SubjectsApiResponse> call,
                    Response<SubjectsApiResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    List<Subject> serverSubjects = response.body().getSubjects();
                    performLocalToCloudSync(localSubjects, serverSubjects, callback);
                } else {
                    Log.e(TAG, "서버 과목 조회 실패: " + response.code());
                    if (callback != null) {
                        callback.onError("서버 과목 조회 실패: " + response.code());
                    }
                }
            }

            @Override
            public void onFailure(Call<SubjectsApiResponse> call, Throwable t) {
                Log.e(TAG, "서버 과목 조회 네트워크 오류", t);
                if (callback != null) {
                    callback.onError("서버 연결 실패: " + t.getMessage());
                }
            }
        });
    }

    /**
     * 로컬 과목들을 서버와 비교하여 필요한 동기화 수행
     */
    private void performLocalToCloudSync(List<SubjectEntity> localSubjects,
            List<Subject> serverSubjects, CloudSyncCallback callback) {

        // 서버 과목들을 맵으로 변환 (serverId -> Subject)
        Map<String, Subject> serverSubjectMap = new HashMap<>();
        for (Subject serverSubject : serverSubjects) {
            if (serverSubject.getId() != null) {
                serverSubjectMap.put(serverSubject.getId(), serverSubject);
            }
        }

        // 로컬 과목들의 서버 ID 목록 생성
        Set<String> localServerIds = new HashSet<>();
        for (SubjectEntity localSubject : localSubjects) {
            if (localSubject.getServerId() != null) {
                localServerIds.add(localSubject.getServerId());
            }
        }

        // 동기화가 필요한 과목들 분류
        List<SubjectEntity> needsCreate = new ArrayList<>(); // 서버에 생성 필요
        List<SubjectEntity> needsUpdate = new ArrayList<>(); // 서버에 업데이트 필요
        List<Subject> needsDelete = new ArrayList<>(); // 서버에서 삭제 필요

        for (SubjectEntity localSubject : localSubjects) {
            if (localSubject.getServerId() == null) {
                // 서버 ID가 없으면 서버에 생성 필요
                needsCreate.add(localSubject);
            } else if (serverSubjectMap.containsKey(localSubject.getServerId())) {
                // 서버에 있는 경우 업데이트 시간 비교
                Subject serverSubject = serverSubjectMap.get(localSubject.getServerId());
                if (isLocalNewer(localSubject, serverSubject)) {
                    needsUpdate.add(localSubject);
                }
            } else {
                // 서버 ID는 있지만 서버에 없는 경우 - 서버에 다시 생성
                localSubject.setServerId(null); // 서버 ID 제거
                needsCreate.add(localSubject);
            }
        }

        // 서버에는 있지만 로컬에 없는 과목들 찾기 (삭제 필요)
        for (Subject serverSubject : serverSubjects) {
            if (serverSubject.getId() != null && !localServerIds.contains(serverSubject.getId())) {
                needsDelete.add(serverSubject);
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

        performBatchSync(needsCreate, needsUpdate, needsDelete, callback);
    }

    /**
     * 로컬 과목이 서버 과목보다 최신인지 확인
     */
    private boolean isLocalNewer(SubjectEntity localSubject, mp.gradia.api.models.Subject serverSubject) {
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
            Log.e(TAG, "서버 업데이트 시간 파싱 오류", e);
            return true; // 파싱 오류 시 로컬을 우선
        }
    }

    /**
     * 배치로 동기화 수행
     */
    private void performBatchSync(List<SubjectEntity> needsCreate, List<SubjectEntity> needsUpdate,
            List<mp.gradia.api.models.Subject> needsDelete, CloudSyncCallback callback) {

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

        // 생성이 필요한 과목들 처리
        for (SubjectEntity subject : needsCreate) {
            disposables.add(uploadSubjectToServer(subject)
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(
                            () -> {
                                Log.d(TAG, "과목 생성 동기화 완료: " + subject.getName());
                                completedItems.incrementAndGet();
                                checkCompletion.run();
                            },
                            throwable -> {
                                Log.e(TAG, "과목 생성 동기화 실패: " + subject.getName(), throwable);
                                hasError.set(true);
                                completedItems.incrementAndGet();
                                checkCompletion.run();
                            }));
        }

        // 업데이트가 필요한 과목들 처리
        for (SubjectEntity subject : needsUpdate) {
            disposables.add(updateSubjectOnServer(subject)
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(
                            () -> {
                                Log.d(TAG, "과목 업데이트 동기화 완료: " + subject.getName());
                                completedItems.incrementAndGet();
                                checkCompletion.run();
                            },
                            throwable -> {
                                Log.e(TAG, "과목 업데이트 동기화 실패: " + subject.getName(), throwable);
                                hasError.set(true);
                                completedItems.incrementAndGet();
                                checkCompletion.run();
                            }));
        }

        // 삭제가 필요한 과목들 처리
        for (Subject subject : needsDelete) {
            disposables.add(deleteSubjectFromServerById(subject.getId())
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(
                            () -> {
                                Log.d(TAG, "서버 과목 삭제 동기화 완료: " + subject.getName());
                                completedItems.incrementAndGet();
                                checkCompletion.run();
                            },
                            throwable -> {
                                Log.e(TAG, "서버 과목 삭제 동기화 실패: " + subject.getName(), throwable);
                                hasError.set(true);
                                completedItems.incrementAndGet();
                                checkCompletion.run();
                            }));
        }
    }

    /**
     * 서버에서 과목을 ID로 삭제
     */
    private Completable deleteSubjectFromServerById(String serverId) {
        return Completable.create(emitter -> {
            String authHeader = authManager.getAuthHeader();

            apiService.deleteSubject(authHeader, serverId).enqueue(new Callback<Void>() {
                @Override
                public void onResponse(Call<Void> call, Response<Void> response) {
                    if (response.isSuccessful()) {
                        emitter.onComplete();
                    } else {
                        emitter.onError(new Exception("서버에서 과목 삭제 실패: " + response.code()));
                    }
                }

                @Override
                public void onFailure(Call<Void> call, Throwable t) {
                    emitter.onError(t);
                }
            });
        });
    }

    /**
     * 서버에서 과목 데이터를 다운로드하여 로컬 DB를 완전히 교체합니다.
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

        apiService.getSubjects(authHeader).enqueue(new retrofit2.Callback<SubjectsApiResponse>() {
            @Override
            public void onResponse(retrofit2.Call<SubjectsApiResponse> call,
                    retrofit2.Response<SubjectsApiResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    List<Subject> serverSubjects = response.body().getSubjects();

                    Log.d(TAG, "서버에서 과목 데이터 가져오기 성공: " + serverSubjects.size() + "개");

                    // 1. 기존 로컬 데이터 삭제
                    disposables.add(subjectDao.clearAll()
                            .subscribeOn(Schedulers.from(executorService))
                            .observeOn(AndroidSchedulers.mainThread())
                            .subscribe(
                                    () -> {
                                        Log.d(TAG, "기존 과목 데이터 삭제 완료");

                                        // 2. 서버 데이터를 로컬 데이터로 변환하여 저장
                                        insertServerSubjectsToLocal(serverSubjects, callback);
                                    },
                                    throwable -> {
                                        Log.e(TAG, "기존 과목 데이터 삭제 실패", throwable);
                                        if (callback != null) {
                                            callback.onError("기존 데이터 삭제 실패: " + throwable.getMessage());
                                        }
                                    }));
                } else {
                    String errorMsg = "서버에서 과목 데이터를 가져오는데 실패했습니다. 응답 코드: " + response.code();
                    Log.e(TAG, errorMsg);
                    if (callback != null) {
                        callback.onError(errorMsg);
                    }
                }
            }

            @Override
            public void onFailure(retrofit2.Call<SubjectsApiResponse> call, Throwable t) {
                Log.e(TAG, "서버 통신 실패", t);
                if (callback != null) {
                    callback.onError("서버 연결 실패: " + t.getMessage());
                }
            }
        });
    }

    /**
     * 서버에서 받은 과목 데이터를 로컬 DB에 저장합니다.
     */
    private void insertServerSubjectsToLocal(List<Subject> serverSubjects, CloudSyncCallback callback) {
        if (serverSubjects.isEmpty()) {
            Log.d(TAG, "서버에 과목 데이터가 없습니다.");
            if (callback != null) {
                callback.onSuccess();
            }
            return;
        }

        List<SubjectEntity> localSubjects = new ArrayList<>();

        // 서버 데이터를 로컬 엔티티로 변환
        for (Subject serverSubject : serverSubjects) {
            try {
                SubjectEntity localSubject = convertServerToLocalSubject(serverSubject);
                localSubjects.add(localSubject);
                Log.d(TAG, "변환된 과목: " + localSubject.getName() + " (서버 ID: " + localSubject.getServerId() + ")");
            } catch (Exception e) {
                Log.e(TAG, "과목 변환 중 오류: " + serverSubject.getName(), e);
            }
        }

        if (localSubjects.isEmpty()) {
            Log.w(TAG, "변환된 과목이 없습니다.");
            if (callback != null) {
                callback.onSuccess();
            }
            return;
        }

        // 로컬 DB에 일괄 저장
        SubjectEntity[] subjectsArray = localSubjects.toArray(new SubjectEntity[0]);
        disposables.add(subjectDao.insert(subjectsArray)
                .subscribeOn(Schedulers.from(executorService))
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(
                        () -> {
                            Log.d(TAG, "서버 과목 데이터 로컬 저장 완료: " + localSubjects.size() + "개");
                            if (callback != null) {
                                callback.onSuccess();
                            }
                        },
                        throwable -> {
                            Log.e(TAG, "서버 과목 데이터 로컬 저장 실패", throwable);
                            if (callback != null) {
                                callback.onError("데이터 저장 실패: " + throwable.getMessage());
                            }
                        }));
    }

    /**
     * 서버 Subject 모델을 로컬 SubjectEntity로 변환합니다.
     */
    private SubjectEntity convertServerToLocalSubject(Subject serverSubject) {
        // 평가 비율 변환
        mp.gradia.database.entity.EvaluationRatio localRatio = null;
        if (serverSubject.getEvaluation_ratio() != null) {
            localRatio = new mp.gradia.database.entity.EvaluationRatio();
            EvaluationRatio serverRatio = serverSubject.getEvaluation_ratio();

            localRatio.midTermRatio = serverRatio.getMid_term_ratio();
            localRatio.finalTermRatio = serverRatio.getFinal_term_ratio();
            localRatio.quizRatio = serverRatio.getQuiz_ratio();
            localRatio.assignmentRatio = serverRatio.getAssignment_ratio();
            localRatio.attendanceRatio = serverRatio.getAttendance_ratio();
        }

        // 목표 공부 시간 변환
        mp.gradia.database.entity.TargetStudyTime localTime = null;
        if (serverSubject.getTarget_study_time() != null) {
            TargetStudyTime serverTime = serverSubject.getTarget_study_time();

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
                Log.e(TAG, "Created_at 변환 오류", e);
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
                Log.e(TAG, "Updated_at 변환 오류", e);
                localSubject.setUpdatedAt(LocalDateTime.now());
            }
        } else {
            localSubject.setUpdatedAt(LocalDateTime.now());
        }

        return localSubject;
    }
}
