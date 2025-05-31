package mp.gradia.database.repository;

import android.content.Context;
import android.util.Log;

import androidx.core.util.Pair;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.LiveDataReactiveStreams;

import java.time.LocalDateTime;
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
import io.reactivex.rxjava3.core.Flowable;
import io.reactivex.rxjava3.core.Single;
import io.reactivex.rxjava3.disposables.CompositeDisposable;
import io.reactivex.rxjava3.schedulers.Schedulers;
import mp.gradia.api.ApiService;
import mp.gradia.api.AuthManager;
import mp.gradia.api.RetrofitClient;
import mp.gradia.api.models.Subject;
import mp.gradia.api.models.SubjectsApiResponse;
import mp.gradia.api.models.TimetableItem;
import mp.gradia.api.models.TimetableResponse;
import mp.gradia.database.AppDatabase;
import mp.gradia.database.dao.SubjectDao;
import mp.gradia.database.dao.TodoDao;
import mp.gradia.database.entity.SubjectEntity;
import mp.gradia.database.entity.TodoEntity;
import mp.gradia.utils.SubjectUtil;
import mp.gradia.utils.TodoUtil;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class SubjectRepository {
    private static final String TAG = "SubjectRepository";

    private final SubjectDao subjectDao;

    private final TodoDao todoDao;

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
        todoDao = db.todoDao();
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
            disposables.add(localInsert
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
                                if (callback != null) {
                                    callback.onSuccess();
                                }

                                // 서버 업데이트는 별도로 비동기 수행
                                disposables.add(updateSubjectOnServer(subject)
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
                                if (callback != null) {
                                    callback.onError("과목 업데이트 실패: " + throwable.getMessage());
                                }
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
            Subject apiSubject = SubjectUtil.convertToApiSubject(subject, new ArrayList<>());
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
                            SubjectUtil.updateTimestampsFromResponse(subject, response.body());

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
        Log.d(TAG, "[updateSubjectOnServer] 시작 - 과목: " + subject.getName() + ", 서버 ID: " + subject.getServerId());

        return Single.fromCallable(() -> {
            // 최신 데이터를 동기적으로 가져오기 (IO 스레드에서 실행)
            SubjectEntity latestSubject = subjectDao.getByIdSync(subject.getSubjectId());
            List<TodoEntity> todos = todoDao.getTodosForSubjectSync(subject.getSubjectId());

            Log.d(TAG, "[updateSubjectOnServer] 데이터 조회 완료 - todos: " + todos.size() + "개");

            // API 모델로 변환
            Subject apiSubject = SubjectUtil.convertToApiSubject(latestSubject, todos);

            Log.d(TAG, "[updateSubjectOnServer] API 모델 변환 완료");

            return new Object[] { latestSubject, apiSubject };
        })
                .subscribeOn(Schedulers.from(executorService))
                .flatMapCompletable(data -> {
                    SubjectEntity latestSubject = (SubjectEntity) ((Object[]) data)[0];
                    Subject apiSubject = (Subject) ((Object[]) data)[1];

                    return Completable.create(emitter -> {
                        String authHeader = authManager.getAuthHeader();
                        String serverId = subject.getServerId();

                        Log.d(TAG, "[updateSubjectOnServer] 서버에 업데이트 요청 시작");

                        // 비동기 서버 업데이트 요청
                        apiService.updateSubject(authHeader, serverId, apiSubject).enqueue(new Callback<Subject>() {
                            @Override
                            public void onResponse(Call<Subject> call, Response<Subject> response) {
                                if (response.isSuccessful() && response.body() != null) {
                                    Log.d(TAG, "[updateSubjectOnServer] 서버 업데이트 성공");

                                    // 서버에서 받은 업데이트 시간을 로컬에 반영
                                    SubjectUtil.updateTimestampsFromResponse(latestSubject, response.body());

                                    // 로컬 DB 업데이트 (비동기)
                                    disposables.add(subjectDao.update(latestSubject)
                                            .subscribeOn(Schedulers.from(executorService))
                                            .subscribe(
                                                    () -> {
                                                        Log.d(TAG, "[updateSubjectOnServer] 타임스탬프 업데이트 완료");
                                                        emitter.onComplete();
                                                    },
                                                    error -> {
                                                        Log.e(TAG, "[updateSubjectOnServer] 타임스탬프 업데이트 실패", error);
                                                        emitter.onError(error);
                                                    }));
                                } else {
                                    String errorMsg = "서버에 과목 업데이트 실패: " + response.code();
                                    Log.e(TAG, "[updateSubjectOnServer] " + errorMsg);
                                    emitter.onError(new Exception(errorMsg));
                                }
                            }

                            @Override
                            public void onFailure(Call<Subject> call, Throwable t) {
                                Log.e(TAG, "[updateSubjectOnServer] 네트워크 오류", t);
                                emitter.onError(t);
                            }
                        });
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
                if (SubjectUtil.isLocalNewer(localSubject, serverSubject)) {
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

        List<TodoEntity> localTodos = new ArrayList<>();

        // 서버 데이터를 로컬 엔티티로 변환
        for (Subject serverSubject : serverSubjects) {
            try {
                SubjectEntity localSubject = SubjectUtil.convertServerToLocalSubject(serverSubject);
                var single = subjectDao.insertAndGetId(localSubject)
                        .subscribeOn(Schedulers.from(executorService))
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe(
                                generatedId -> {
                                    if (serverSubject.getTodos() != null) {
                                        Log.d(TAG, "서버 과목의 할 일 목록이 존재합니다: " + serverSubject.getTodos().size() + "개");
                                        for (var serverTodos : serverSubject.getTodos()) {
                                            TodoEntity localTodo = TodoUtil.convertServerToLocalTodo(serverTodos,
                                                    generatedId.intValue());

                                            disposables.add(todoDao.insertCompletable(localTodo)
                                                    .subscribeOn(Schedulers.from(executorService))
                                                    .observeOn(AndroidSchedulers.mainThread())
                                                    .subscribe(
                                                            () -> Log.d(TAG, "로컬 할 일 저장 완료: " + localTodo.content),
                                                            throwable -> Log.e(TAG, "로컬 할 일 저장 실패", throwable)));
                                        }
                                    }
                                    localSubject.setSubjectId(generatedId.intValue());
                                    Log.d(TAG, "서버 과목 데이터 로컬 저장 완료: " + localSubject.getName() + " (ID: "
                                            + localSubject.getSubjectId() + ")");
                                },
                                throwable -> Log.e(TAG, "서버 과목 데이터 로컬 저장 실패", throwable));

                disposables.add(single);
                Log.d(TAG, "변환된 과목: " + localSubject.getName() + " (서버 ID: " + localSubject.getServerId() + ")");
            } catch (Exception e) {
                Log.e(TAG, "과목 변환 중 오류: " + serverSubject.getName(), e);
            }
        }
    }

    public void fetchEveryTimeTable(String url, CloudSyncCallback callback) {
        apiService.getTimetable(url).enqueue(new Callback<TimetableResponse>() {
            @Override
            public void onResponse(Call<TimetableResponse> call, Response<TimetableResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    TimetableResponse timetable = response.body();
                    List<TimetableItem> timetableItems = timetable.getTimetable();
                    List<SubjectEntity> convertedSubjects = SubjectUtil
                            .convertTimetableItemsToSubjectEntities(timetableItems);
                    for (SubjectEntity subject : convertedSubjects) {
                        insert(subject, new CloudSyncCallback() {
                            @Override
                            public void onSuccess() {
                                Log.d(TAG, "과목 추가 성공: " + subject.getName());
                            }

                            @Override
                            public void onError(String error) {
                                Log.e(TAG, "과목 추가 실패: " + subject.getName() + ", 오류: " + error);
                            }
                        });
                    }

                    if (callback != null) {
                        callback.onSuccess();
                    }
                }
            }

            @Override
            public void onFailure(Call<TimetableResponse> call, Throwable t) {
                Log.e(TAG, "과목 조회 실패", t);

                if (callback != null) {
                    callback.onError("과목 조회 실패: " + t.getMessage());
                }
            }
        });
    }

    /**
     * 외부에서 특정 Subject를 서버에 동기화할 수 있는 public 메서드
     */
    public Completable syncSubjectToServer(SubjectEntity subject) {
        if (!authManager.isLoggedIn() || subject.getServerId() == null) {
            return Completable.complete(); // 조건 불만족 시 즉시 완료
        }
        return updateSubjectOnServer(subject);
    }
}
