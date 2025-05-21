package mp.gradia.api;

import android.content.Context;
import android.util.Log;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.core.Completable;
import io.reactivex.rxjava3.core.CompletableEmitter;
import io.reactivex.rxjava3.core.Single;
import io.reactivex.rxjava3.disposables.CompositeDisposable;
import io.reactivex.rxjava3.disposables.Disposable;
import io.reactivex.rxjava3.schedulers.Schedulers;
import mp.gradia.api.models.EvaluationRatio;
import mp.gradia.api.models.StudySession;
import mp.gradia.api.models.StudySessionsApiResponse;
import mp.gradia.api.models.Subject;
import mp.gradia.api.models.SubjectsApiResponse;
import mp.gradia.api.models.TargetStudyTime;
import mp.gradia.database.AppDatabase;
import mp.gradia.database.entity.StudySessionEntity;
import mp.gradia.database.entity.SubjectEntity;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * 로컬 Room DB와 원격 서버 간의 데이터 동기화를 관리하는 유틸리티 클래스입니다.
 */
public class CloudSyncManager {
    private static final String TAG = "CloudSyncManager";

    private final Context context;
    private final ApiService apiService;
    private final AuthManager authManager;
    private final AppDatabase database;
    private final CompositeDisposable disposables = new CompositeDisposable();

    public interface SyncCallback {
        void onSuccess();

        void onError(String message);

        void onProgress(int progress, int total);
    }

    public CloudSyncManager(Context context) {
        this.context = context.getApplicationContext();
        this.apiService = RetrofitClient.getApiService();
        this.authManager = AuthManager.getInstance(context);
        this.database = AppDatabase.getInstance(context);
    }

    /**
     * Room DB의 데이터를 원격 서버로 업로드합니다.
     */
    public void uploadToServer(SyncCallback callback) {
        if (!authManager.isLoggedIn()) {
            callback.onError("로그인이 필요합니다.");
            return;
        }

        String authHeader = authManager.getAuthHeader();

        // 과목과 학습 세션 데이터를 순차적으로 업로드
        uploadSubjects(authHeader, callback)
                .andThen(uploadStudySessions(authHeader, callback))
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(
                        () -> callback.onSuccess(),
                        throwable -> callback.onError("동기화 중 오류가 발생했습니다: " + throwable.getMessage()));
    }

    /**
     * 원격 서버의 데이터를 Room DB로 다운로드합니다.
     */
    public void downloadFromServer(SyncCallback callback) {
        if (!authManager.isLoggedIn()) {
            callback.onError("로그인이 필요합니다.");
            return;
        }

        String authHeader = authManager.getAuthHeader();

        // 과목과 학습 세션 데이터를 순차적으로 다운로드
        downloadSubjects(authHeader, callback)
                .andThen(downloadStudySessions(authHeader, callback))
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(
                        () -> callback.onSuccess(),
                        throwable -> callback.onError("동기화 중 오류가 발생했습니다: " + throwable.getMessage()));
    }

    /**
     * Room DB의 과목 데이터를 원격 서버에 업로드합니다.
     * 1. 서버 데이터 목록을 가져옵니다.
     * 2. 로컬 데이터와 서버 데이터를 비교합니다.
     * 3. serverId가 없는 로컬 데이터는 새로 생성합니다.
     * 4. 서버에만 있고 로컬에 없는 데이터는 서버에서 삭제합니다.
     * 5. 로컬과 서버 모두에 있는 데이터는 updatedAt을 비교하여 업데이트 여부를 결정합니다.
     */
    private Completable uploadSubjects(String authHeader, SyncCallback callback) {
        return Completable.create(emitter -> {
            // 1. 먼저 서버에서 과목 목록을 가져옵니다.
            apiService.getSubjects(authHeader).enqueue(new Callback<SubjectsApiResponse>() {
                @Override
                public void onResponse(Call<SubjectsApiResponse> call, Response<SubjectsApiResponse> response) {
                    if (response.isSuccessful() && response.body() != null) {
                        List<Subject> serverSubjects = response.body().getSubjects();

                        // 2. 로컬 데이터를 가져와서 서버 데이터와 비교합니다.
                        Disposable d = database.subjectDao().getAll()
                                .firstElement()
                                .flatMapCompletable(localSubjects -> {
                                    AtomicInteger progress = new AtomicInteger(0);
                                    AtomicInteger total = new AtomicInteger(
                                            localSubjects.size() + serverSubjects.size());
                                    AtomicInteger processedCount = new AtomicInteger(0);
                                    AtomicBoolean hasError = new AtomicBoolean(false);

                                    return Completable.create(innerEmitter -> {
                                        // 서버 과목 ID 맵 생성
                                        Map<String, Subject> serverSubjectMap = new HashMap<>();
                                        for (Subject subject : serverSubjects) {
                                            if (subject.getId() != null) {
                                                serverSubjectMap.put(subject.getId(), subject);
                                            }
                                        }

                                        // 로컬 과목 ID 맵 생성
                                        Map<String, SubjectEntity> localSubjectMap = new HashMap<>();
                                        for (SubjectEntity subject : localSubjects) {
                                            if (subject.getServerId() != null) {
                                                localSubjectMap.put(subject.getServerId(), subject);
                                            }
                                        }

                                        // 3. 로컬에 있지만 서버에 없는 과목을 생성합니다.
                                        for (SubjectEntity localSubject : localSubjects) {
                                            if (localSubject.getServerId() == null
                                                    || !serverSubjectMap.containsKey(localSubject.getServerId())) {
                                                // 새 과목 생성
                                                Subject apiSubject = convertToApiSubject(localSubject);
                                                // ID를 null로 설정하여 서버에서 새 ID를 생성하도록 합니다.
                                                apiSubject.setId(null);

                                                apiService.createSubject(authHeader, apiSubject)
                                                        .enqueue(new Callback<Subject>() {
                                                            @Override
                                                            public void onResponse(Call<Subject> call,
                                                                    Response<Subject> response) {
                                                                if (response.isSuccessful()
                                                                        && response.body() != null) {
                                                                    // 생성된 과목의 서버 ID를 로컬 데이터에 저장
                                                                    String serverId = response.body().getId();
                                                                    if (serverId != null) {
                                                                        localSubject.setServerId(serverId);

                                                                        // created_at, updated_at 업데이트
                                                                        if (response.body().getCreated_at() != null) {
                                                                            try {
                                                                                LocalDateTime createdAt = LocalDateTime
                                                                                        .parse(response.body()
                                                                                                .getCreated_at(),
                                                                                                DateTimeFormatter.ISO_DATE_TIME);
                                                                                localSubject.setCreatedAt(createdAt);
                                                                            } catch (Exception e) {
                                                                                Log.e(TAG, "Created_at 변환 오류", e);
                                                                            }
                                                                        }

                                                                        if (response.body().getUpdated_at() != null) {
                                                                            try {
                                                                                LocalDateTime updatedAt = LocalDateTime
                                                                                        .parse(response.body()
                                                                                                .getUpdated_at(),
                                                                                                DateTimeFormatter.ISO_DATE_TIME);
                                                                                localSubject.setUpdatedAt(updatedAt);
                                                                            } catch (Exception e) {
                                                                                Log.e(TAG, "Updated_at 변환 오류", e);
                                                                            }
                                                                        }

                                                                        // 데이터베이스 업데이트
                                                                        database.subjectDao().update(localSubject);
                                                                    }
                                                                } else {
                                                                    hasError.set(true);
                                                                    Log.e(TAG, "과목 생성 실패: " + response.code());
                                                                }

                                                                // 진행률 업데이트
                                                                updateProgressAndCheckComplete(progress, total,
                                                                        processedCount, innerEmitter, hasError,
                                                                        callback);
                                                            }

                                                            @Override
                                                            public void onFailure(Call<Subject> call, Throwable t) {
                                                                hasError.set(true);
                                                                Log.e(TAG, "과목 생성 네트워크 오류", t);
                                                                updateProgressAndCheckComplete(progress, total,
                                                                        processedCount, innerEmitter, hasError,
                                                                        callback);
                                                            }
                                                        });
                                            } else {
                                                // 4. 로컬과 서버 모두에 있는 과목은 업데이트 시간을 비교합니다.
                                                String serverId = localSubject.getServerId();
                                                Subject serverSubject = serverSubjectMap.get(serverId);

                                                if (serverSubject != null && needsUpdate(localSubject, serverSubject)) {
                                                    // 업데이트가 필요한 경우
                                                    Subject apiSubject = convertToApiSubject(localSubject);

                                                    apiService.updateSubject(authHeader, serverId, apiSubject)
                                                            .enqueue(new Callback<Subject>() {
                                                                @Override
                                                                public void onResponse(Call<Subject> call,
                                                                        Response<Subject> response) {
                                                                    if (response.isSuccessful()
                                                                            && response.body() != null) {
                                                                        // updated_at 업데이트
                                                                        if (response.body().getUpdated_at() != null) {
                                                                            try {
                                                                                LocalDateTime updatedAt = LocalDateTime
                                                                                        .parse(response.body()
                                                                                                .getUpdated_at(),
                                                                                                DateTimeFormatter.ISO_DATE_TIME);
                                                                                localSubject.setUpdatedAt(updatedAt);
                                                                                // 데이터베이스 업데이트
                                                                                database.subjectDao()
                                                                                        .update(localSubject);
                                                                            } catch (Exception e) {
                                                                                Log.e(TAG, "Updated_at 변환 오류", e);
                                                                            }
                                                                        }
                                                                    } else {
                                                                        hasError.set(true);
                                                                        Log.e(TAG, "과목 업데이트 실패: " + response.code());
                                                                    }

                                                                    // 진행률 업데이트
                                                                    updateProgressAndCheckComplete(progress, total,
                                                                            processedCount, innerEmitter, hasError,
                                                                            callback);
                                                                }

                                                                @Override
                                                                public void onFailure(Call<Subject> call, Throwable t) {
                                                                    hasError.set(true);
                                                                    Log.e(TAG, "과목 업데이트 네트워크 오류", t);
                                                                    updateProgressAndCheckComplete(progress, total,
                                                                            processedCount, innerEmitter, hasError,
                                                                            callback);
                                                                }
                                                            });
                                                } else {
                                                    // 업데이트가 필요 없는 경우 바로 진행률 업데이트
                                                    updateProgressAndCheckComplete(progress, total, processedCount,
                                                            innerEmitter, hasError, callback);
                                                }
                                            }
                                        }

                                        // 5. 서버에만 있고 로컬에 없는 과목은 삭제합니다.
                                        for (Subject serverSubject : serverSubjects) {
                                            if (serverSubject.getId() != null
                                                    && !localSubjectMap.containsKey(serverSubject.getId())) {
                                                apiService.deleteSubject(authHeader, serverSubject.getId())
                                                        .enqueue(new Callback<Void>() {
                                                            @Override
                                                            public void onResponse(Call<Void> call,
                                                                    Response<Void> response) {
                                                                if (!response.isSuccessful()) {
                                                                    hasError.set(true);
                                                                    Log.e(TAG, "과목 삭제 실패: " + response.code());
                                                                }

                                                                // 진행률 업데이트
                                                                updateProgressAndCheckComplete(progress, total,
                                                                        processedCount, innerEmitter, hasError,
                                                                        callback);
                                                            }

                                                            @Override
                                                            public void onFailure(Call<Void> call, Throwable t) {
                                                                hasError.set(true);
                                                                Log.e(TAG, "과목 삭제 네트워크 오류", t);
                                                                updateProgressAndCheckComplete(progress, total,
                                                                        processedCount, innerEmitter, hasError,
                                                                        callback);
                                                            }
                                                        });
                                            } else {
                                                // 이미 처리된 서버 과목은 바로 진행률 업데이트
                                                updateProgressAndCheckComplete(progress, total, processedCount,
                                                        innerEmitter, hasError, callback);
                                            }
                                        }

                                        // 처리할 과목이 없는 경우
                                        if (total.get() == 0) {
                                            innerEmitter.onComplete();
                                        }
                                    });
                                })
                                .subscribeOn(Schedulers.io())
                                .subscribe(
                                        () -> emitter.onComplete(),
                                        throwable -> emitter.onError(throwable));

                        disposables.add(d);
                    } else {
                        emitter.onError(new Exception("서버에서 과목 데이터를 가져오는데 실패했습니다."));
                    }
                }

                @Override
                public void onFailure(Call<SubjectsApiResponse> call, Throwable t) {
                    emitter.onError(t);
                }
            });
        });
    }

    /**
     * 로컬 과목과 서버 과목을 비교하여 업데이트가 필요한지 확인합니다.
     */
    private boolean needsUpdate(SubjectEntity localSubject, Subject serverSubject) {
        // 서버에 updated_at이 없으면 업데이트가 필요하다고 간주
        if (serverSubject.getUpdated_at() == null) {
            return true;
        }

        // 로컬에 updatedAt이 없으면 업데이트가 필요하다고 간주
        if (localSubject.getUpdatedAt() == null) {
            return true;
        }

        try {
            // 서버의 업데이트 시간 파싱
            LocalDateTime serverUpdatedAt = LocalDateTime.parse(serverSubject.getUpdated_at(),
                    DateTimeFormatter.ISO_DATE_TIME);

            // 로컬의 업데이트 시간이 서버보다 최신이면 업데이트 필요
            return localSubject.getUpdatedAt().isAfter(serverUpdatedAt);
        } catch (Exception e) {
            Log.e(TAG, "업데이트 시간 비교 오류", e);
            return true; // 오류 발생 시 업데이트가 필요하다고 간주
        }
    }

    /**
     * 진행률을 업데이트하고 완료 여부를 확인합니다.
     */
    private void updateProgressAndCheckComplete(
            AtomicInteger progress,
            AtomicInteger total,
            AtomicInteger processedCount,
            CompletableEmitter emitter,
            AtomicBoolean hasError,
            SyncCallback callback) {
        int currentProgress = progress.incrementAndGet();
        int currentProcessed = processedCount.incrementAndGet();

        AndroidSchedulers.mainThread().scheduleDirect(() -> callback.onProgress(currentProgress, total.get()));

        // 모든 작업이 완료되었는지 확인
        if (currentProcessed >= total.get()) {
            if (hasError.get()) {
                emitter.onError(new Exception("일부 과목 동기화 작업이 실패했습니다."));
            } else {
                emitter.onComplete();
            }
        }
    }

    /**
     * Room DB의 학습 세션 데이터를 원격 서버에 업로드합니다.
     * 1. 서버 데이터 목록을 가져옵니다.
     * 2. 로컬 데이터와 서버 데이터를 비교합니다.
     * 3. serverId가 없는 로컬 데이터는 새로 생성합니다.
     * 4. 서버에만 있고 로컬에 없는 데이터는 서버에서 삭제합니다.
     * 5. 로컬과 서버 모두에 있는 데이터는 updatedAt을 비교하여 업데이트 여부를 결정합니다.
     */
    private Completable uploadStudySessions(String authHeader, SyncCallback callback) {
        return Completable.create(emitter -> {
            // 1. 먼저 서버에서 학습 세션 목록을 가져옵니다.
            apiService.getStudySessions(authHeader, null).enqueue(new Callback<StudySessionsApiResponse>() {
                @Override
                public void onResponse(Call<StudySessionsApiResponse> call,
                        Response<StudySessionsApiResponse> response) {
                    if (response.isSuccessful() && response.body() != null) {
                        List<StudySession> serverSessions = response.body().getSessions();

                        // 2. 로컬 데이터를 가져와서 서버 데이터와 비교합니다.
                        Disposable d = database.studySessionDao().getAll()
                                .firstElement()
                                .flatMapCompletable(localSessions -> {
                                    AtomicInteger progress = new AtomicInteger(0);
                                    AtomicInteger total = new AtomicInteger(
                                            localSessions.size() + serverSessions.size());
                                    AtomicInteger processedCount = new AtomicInteger(0);
                                    AtomicBoolean hasError = new AtomicBoolean(false);

                                    return Completable.create(innerEmitter -> {
                                        // 서버 세션 ID 맵 생성
                                        Map<String, StudySession> serverSessionMap = new HashMap<>();
                                        for (StudySession session : serverSessions) {
                                            if (session.getId() != null) {
                                                serverSessionMap.put(session.getId(), session);
                                            }
                                        }

                                        // 로컬 세션 ID 맵 생성
                                        Map<String, StudySessionEntity> localSessionMap = new HashMap<>();
                                        for (StudySessionEntity session : localSessions) {
                                            if (session.getServerId() != null) {
                                                localSessionMap.put(session.getServerId(), session);
                                            }
                                        }

                                        // 3. 로컬에 있지만 서버에 없는 세션을 생성합니다.
                                        for (StudySessionEntity localSession : localSessions) {
                                            if (localSession.getServerId() == null
                                                    || !serverSessionMap.containsKey(localSession.getServerId())) {
                                                // 새 세션 생성
                                                StudySession apiSession = convertToApiStudySession(localSession);
                                                // ID를 null로 설정하여 서버에서 새 ID를 생성하도록 합니다.
                                                apiSession.setId(null);

                                                apiService.createStudySession(authHeader, apiSession)
                                                        .enqueue(new Callback<StudySession>() {
                                                            @Override
                                                            public void onResponse(Call<StudySession> call,
                                                                    Response<StudySession> response) {
                                                                if (response.isSuccessful()
                                                                        && response.body() != null) {
                                                                    // 생성된 세션의 서버 ID를 로컬 데이터에 저장
                                                                    String serverId = response.body().getId();
                                                                    if (serverId != null) {
                                                                        localSession.setServerId(serverId);

                                                                        // created_at, updated_at 업데이트
                                                                        if (response.body().getCreated_at() != null) {
                                                                            try {
                                                                                LocalDateTime createdAt = LocalDateTime
                                                                                        .parse(response.body()
                                                                                                .getCreated_at(),
                                                                                                DateTimeFormatter.ISO_DATE_TIME);
                                                                                localSession.setCreatedAt(createdAt);
                                                                            } catch (Exception e) {
                                                                                Log.e(TAG, "Created_at 변환 오류", e);
                                                                            }
                                                                        }

                                                                        if (response.body().getUpdated_at() != null) {
                                                                            try {
                                                                                LocalDateTime updatedAt = LocalDateTime
                                                                                        .parse(response.body()
                                                                                                .getUpdated_at(),
                                                                                                DateTimeFormatter.ISO_DATE_TIME);
                                                                                localSession.setUpdatedAt(updatedAt);
                                                                            } catch (Exception e) {
                                                                                Log.e(TAG, "Updated_at 변환 오류", e);
                                                                            }
                                                                        }

                                                                        // 데이터베이스 업데이트
                                                                        database.studySessionDao().update(localSession);
                                                                    }
                                                                } else {
                                                                    hasError.set(true);
                                                                    Log.e(TAG, "학습 세션 생성 실패: " + response.code());
                                                                }

                                                                // 진행률 업데이트
                                                                updateProgressAndCheckComplete(progress, total,
                                                                        processedCount, innerEmitter, hasError,
                                                                        callback);
                                                            }

                                                            @Override
                                                            public void onFailure(Call<StudySession> call,
                                                                    Throwable t) {
                                                                hasError.set(true);
                                                                Log.e(TAG, "학습 세션 생성 네트워크 오류", t);
                                                                updateProgressAndCheckComplete(progress, total,
                                                                        processedCount, innerEmitter, hasError,
                                                                        callback);
                                                            }
                                                        });
                                            } else {
                                                // 4. 로컬과 서버 모두에 있는 세션은 업데이트 시간을 비교합니다.
                                                String serverId = localSession.getServerId();
                                                StudySession serverSession = serverSessionMap.get(serverId);

                                                if (serverSession != null
                                                        && needsUpdateSession(localSession, serverSession)) {
                                                    // 업데이트가 필요한 경우
                                                    StudySession apiSession = convertToApiStudySession(localSession);

                                                    apiService.updateStudySession(authHeader, serverId, apiSession)
                                                            .enqueue(new Callback<StudySession>() {
                                                                @Override
                                                                public void onResponse(Call<StudySession> call,
                                                                        Response<StudySession> response) {
                                                                    if (response.isSuccessful()
                                                                            && response.body() != null) {
                                                                        // updated_at 업데이트
                                                                        if (response.body().getUpdated_at() != null) {
                                                                            try {
                                                                                LocalDateTime updatedAt = LocalDateTime
                                                                                        .parse(response.body()
                                                                                                .getUpdated_at(),
                                                                                                DateTimeFormatter.ISO_DATE_TIME);
                                                                                localSession.setUpdatedAt(updatedAt);
                                                                                // 데이터베이스 업데이트
                                                                                database.studySessionDao()
                                                                                        .update(localSession);
                                                                            } catch (Exception e) {
                                                                                Log.e(TAG, "Updated_at 변환 오류", e);
                                                                            }
                                                                        }
                                                                    } else {
                                                                        hasError.set(true);
                                                                        Log.e(TAG, "학습 세션 업데이트 실패: " + response.code());
                                                                    }

                                                                    // 진행률 업데이트
                                                                    updateProgressAndCheckComplete(progress, total,
                                                                            processedCount, innerEmitter, hasError,
                                                                            callback);
                                                                }

                                                                @Override
                                                                public void onFailure(Call<StudySession> call,
                                                                        Throwable t) {
                                                                    hasError.set(true);
                                                                    Log.e(TAG, "학습 세션 업데이트 네트워크 오류", t);
                                                                    updateProgressAndCheckComplete(progress, total,
                                                                            processedCount, innerEmitter, hasError,
                                                                            callback);
                                                                }
                                                            });
                                                } else {
                                                    // 업데이트가 필요 없는 경우 바로 진행률 업데이트
                                                    updateProgressAndCheckComplete(progress, total, processedCount,
                                                            innerEmitter, hasError, callback);
                                                }
                                            }
                                        }

                                        // 5. 서버에만 있고 로컬에 없는 세션은 삭제합니다.
                                        for (StudySession serverSession : serverSessions) {
                                            if (serverSession.getId() != null
                                                    && !localSessionMap.containsKey(serverSession.getId())) {
                                                apiService.deleteStudySession(authHeader, serverSession.getId())
                                                        .enqueue(new Callback<Void>() {
                                                            @Override
                                                            public void onResponse(Call<Void> call,
                                                                    Response<Void> response) {
                                                                if (!response.isSuccessful()) {
                                                                    hasError.set(true);
                                                                    Log.e(TAG, "학습 세션 삭제 실패: " + response.code());
                                                                }

                                                                // 진행률 업데이트
                                                                updateProgressAndCheckComplete(progress, total,
                                                                        processedCount, innerEmitter, hasError,
                                                                        callback);
                                                            }

                                                            @Override
                                                            public void onFailure(Call<Void> call, Throwable t) {
                                                                hasError.set(true);
                                                                Log.e(TAG, "학습 세션 삭제 네트워크 오류", t);
                                                                updateProgressAndCheckComplete(progress, total,
                                                                        processedCount, innerEmitter, hasError,
                                                                        callback);
                                                            }
                                                        });
                                            } else {
                                                // 이미 처리된 서버 세션은 바로 진행률 업데이트
                                                updateProgressAndCheckComplete(progress, total, processedCount,
                                                        innerEmitter, hasError, callback);
                                            }
                                        }

                                        // 처리할 세션이 없는 경우
                                        if (total.get() == 0) {
                                            innerEmitter.onComplete();
                                        }
                                    });
                                })
                                .subscribeOn(Schedulers.io())
                                .subscribe(
                                        () -> emitter.onComplete(),
                                        throwable -> emitter.onError(throwable));

                        disposables.add(d);
                    } else {
                        emitter.onError(new Exception("서버에서 학습 세션 데이터를 가져오는데 실패했습니다."));
                    }
                }

                @Override
                public void onFailure(Call<StudySessionsApiResponse> call, Throwable t) {
                    emitter.onError(t);
                }
            });
        });
    }

    /**
     * 로컬 학습 세션과 서버 학습 세션을 비교하여 업데이트가 필요한지 확인합니다.
     */
    private boolean needsUpdateSession(StudySessionEntity localSession, StudySession serverSession) {
        // 서버에 updated_at이 없으면 업데이트가 필요하다고 간주
        if (serverSession.getUpdated_at() == null) {
            return true;
        }

        // 로컬에 updatedAt이 없으면 업데이트가 필요하다고 간주
        if (localSession.getUpdatedAt() == null) {
            return true;
        }

        try {
            // 서버의 업데이트 시간 파싱
            LocalDateTime serverUpdatedAt = LocalDateTime.parse(serverSession.getUpdated_at(),
                    DateTimeFormatter.ISO_DATE_TIME);

            // 로컬의 업데이트 시간이 서버보다 최신이면 업데이트 필요
            return localSession.getUpdatedAt().isAfter(serverUpdatedAt);
        } catch (Exception e) {
            Log.e(TAG, "업데이트 시간 비교 오류", e);
            return true; // 오류 발생 시 업데이트가 필요하다고 간주
        }
    }

    /**
     * 원격 서버의 과목 데이터를 Room DB에 다운로드합니다.
     */
    private Completable downloadSubjects(String authHeader, SyncCallback callback) {
        return Completable.create(emitter -> {
            apiService.getSubjects(authHeader).enqueue(new Callback<SubjectsApiResponse>() {
                @Override
                public void onResponse(Call<SubjectsApiResponse> call, Response<SubjectsApiResponse> response) {
                    if (response.isSuccessful() && response.body() != null) {
                        List<Subject> apiSubjects = response.body().getSubjects();
                        int total = apiSubjects.size();
                        AtomicInteger progress = new AtomicInteger(0);

                        if (total == 0) {
                            emitter.onComplete();
                            return;
                        }

                        // Room DB의 기존 과목 데이터 삭제 후 서버 데이터 삽입
                        Disposable d = database.subjectDao().clearAll()
                                .andThen(Completable.fromAction(() -> {
                                    List<SubjectEntity> localSubjects = new ArrayList<>();

                                    for (Subject apiSubject : apiSubjects) {
                                        SubjectEntity localSubject = convertToLocalSubject(apiSubject);
                                        localSubjects.add(localSubject);

                                        // 진행 상황 업데이트
                                        int currentProgress = progress.incrementAndGet();
                                        AndroidSchedulers.mainThread()
                                                .scheduleDirect(() -> callback.onProgress(currentProgress, total));
                                    }

                                    database.subjectDao().insert(localSubjects.toArray(new SubjectEntity[0]));
                                }))
                                .subscribeOn(Schedulers.io())
                                .subscribe(
                                        () -> emitter.onComplete(),
                                        throwable -> emitter.onError(throwable));

                        disposables.add(d);
                    } else {
                        emitter.onError(new Exception("서버에서 과목 데이터를 가져오는데 실패했습니다."));
                    }
                }

                @Override
                public void onFailure(Call<SubjectsApiResponse> call, Throwable t) {
                    emitter.onError(t);
                }
            });
        });
    }

    /**
     * 원격 서버의 학습 세션 데이터를 Room DB에 다운로드합니다.
     */
    private Completable downloadStudySessions(String authHeader, SyncCallback callback) {
        return Completable.create(emitter -> {
            apiService.getStudySessions(authHeader, null).enqueue(new Callback<StudySessionsApiResponse>() {
                @Override
                public void onResponse(Call<StudySessionsApiResponse> call,
                        Response<StudySessionsApiResponse> response) {
                    if (response.isSuccessful() && response.body() != null) {
                        List<StudySession> apiSessions = response.body().getSessions();
                        int total = apiSessions.size();
                        AtomicInteger progress = new AtomicInteger(0);

                        if (total == 0) {
                            emitter.onComplete();
                            return;
                        }

                        // Room DB의 기존 학습 세션 데이터 삭제 후 서버 데이터 삽입
                        Disposable d = database.studySessionDao().clearAll()
                                .andThen(Completable.fromAction(() -> {
                                    List<StudySessionEntity> localSessions = new ArrayList<>();

                                    for (StudySession apiSession : apiSessions) {
                                        StudySessionEntity localSession = convertToLocalStudySession(apiSession);
                                        localSessions.add(localSession);

                                        // 진행 상황 업데이트
                                        int currentProgress = progress.incrementAndGet();
                                        AndroidSchedulers.mainThread()
                                                .scheduleDirect(() -> callback.onProgress(currentProgress, total));
                                    }

                                    database.studySessionDao()
                                            .insert(localSessions.toArray(new StudySessionEntity[0]));
                                }))
                                .subscribeOn(Schedulers.io())
                                .subscribe(
                                        () -> emitter.onComplete(),
                                        throwable -> emitter.onError(throwable));

                        disposables.add(d);
                    } else {
                        emitter.onError(new Exception("서버에서 학습 세션 데이터를 가져오는데 실패했습니다."));
                    }
                }

                @Override
                public void onFailure(Call<StudySessionsApiResponse> call, Throwable t) {
                    emitter.onError(t);
                }
            });
        });
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
     * 로컬 StudySessionEntity를 API StudySession 모델로 변환합니다.
     */
    private StudySession convertToApiStudySession(StudySessionEntity localSession) {
        StudySession apiSession = new StudySession();
        apiSession.setId(String.valueOf(localSession.getSessionId()));
        if (localSession.getServerId() != null) {
            apiSession.setId(localSession.getServerId());
        }
        apiSession.setSubject_id(String.valueOf(localSession.getSubjectId()));

        // LocalDate를 문자열로 변환 (예: 2023-05-15)
        DateTimeFormatter dateFormatter = DateTimeFormatter.ISO_LOCAL_DATE;
        apiSession.setDate(localSession.getDate().format(dateFormatter));

        apiSession.setStudy_time((int) localSession.getStudyTime());

        // LocalTime을 문자열로 변환 (예: 14:30:00)
        DateTimeFormatter timeFormatter = DateTimeFormatter.ISO_LOCAL_TIME;
        if (localSession.getStartTime() != null) {
            apiSession.setStart_time(localSession.getStartTime().format(timeFormatter));
        }
        if (localSession.getEndTime() != null) {
            apiSession.setEnd_time(localSession.getEndTime().format(timeFormatter));
        }

        apiSession.setRest_time((int) localSession.getRestTime());

        // 생성 시간과 업데이트 시간을 ISO 형식 문자열로 변환
        if (localSession.getCreatedAt() != null) {
            DateTimeFormatter formatter = DateTimeFormatter.ISO_DATE_TIME;
            apiSession.setCreated_at(localSession.getCreatedAt().format(formatter));
        }

        if (localSession.getUpdatedAt() != null) {
            DateTimeFormatter formatter = DateTimeFormatter.ISO_DATE_TIME;
            apiSession.setUpdated_at(localSession.getUpdatedAt().format(formatter));
        }

        return apiSession;
    }

    /**
     * API Subject 모델을 로컬 SubjectEntity로 변환합니다.
     */
    private SubjectEntity convertToLocalSubject(Subject apiSubject) {
        try {
            int subjectId = 0;
            if (apiSubject.getId() != null && !apiSubject.getId().isEmpty()) {
                try {
                    subjectId = Integer.parseInt(apiSubject.getId());
                } catch (NumberFormatException e) {
                    // ID가 정수가 아닌 경우 (예: UUID)
                    subjectId = 0;
                }
            }

            // 평가 비율 변환
            mp.gradia.database.entity.EvaluationRatio localRatio = null;
            if (apiSubject.getEvaluation_ratio() != null) {
                localRatio = new mp.gradia.database.entity.EvaluationRatio();
                EvaluationRatio apiRatio = apiSubject.getEvaluation_ratio();

                localRatio.midTermRatio = apiRatio.getMid_term_ratio();
                localRatio.finalTermRatio = apiRatio.getFinal_term_ratio();
                localRatio.quizRatio = apiRatio.getQuiz_ratio();
                localRatio.assignmentRatio = apiRatio.getAssignment_ratio();
                localRatio.attendanceRatio = apiRatio.getAttendance_ratio();
            }

            // 목표 공부 시간 변환
            mp.gradia.database.entity.TargetStudyTime localTime = null;
            if (apiSubject.getTarget_study_time() != null) {
                TargetStudyTime apiTime = apiSubject.getTarget_study_time();

                localTime = new mp.gradia.database.entity.TargetStudyTime(
                        apiTime.getDaily_target_study_time(),
                        apiTime.getWeekly_target_study_time(),
                        apiTime.getMonthly_target_study_time());
            }

            // 객체 생성
            SubjectEntity localSubject = new SubjectEntity(
                    apiSubject.getName(),
                    apiSubject.getCredit(),
                    apiSubject.getColor(),
                    apiSubject.getType(),
                    apiSubject.getMid_term_schedule(),
                    apiSubject.getFinal_term_schedule(),
                    localRatio,
                    localTime);

            localSubject.setSubjectId(subjectId);
            localSubject.setServerId(apiSubject.getId());
            localSubject.setDifficulty(apiSubject.getDifficulty());

            // created_at과 updated_at 설정
            if (apiSubject.getCreated_at() != null) {
                try {
                    LocalDateTime createdAt = LocalDateTime.parse(apiSubject.getCreated_at(),
                            DateTimeFormatter.ISO_DATE_TIME);
                    localSubject.setCreatedAt(createdAt);
                } catch (Exception e) {
                    Log.e(TAG, "Created_at 변환 오류", e);
                    localSubject.setCreatedAt(LocalDateTime.now());
                }
            }

            if (apiSubject.getUpdated_at() != null) {
                try {
                    LocalDateTime updatedAt = LocalDateTime.parse(apiSubject.getUpdated_at(),
                            DateTimeFormatter.ISO_DATE_TIME);
                    localSubject.setUpdatedAt(updatedAt);
                } catch (Exception e) {
                    Log.e(TAG, "Updated_at 변환 오류", e);
                    localSubject.setUpdatedAt(LocalDateTime.now());
                }
            }

            return localSubject;
        } catch (NumberFormatException e) {
            Log.e(TAG, "Subject ID 변환 오류", e);
            throw new RuntimeException("Subject ID 변환 오류", e);
        }
    }

    /**
     * API StudySession 모델을 로컬 StudySessionEntity로 변환합니다.
     */
    private StudySessionEntity convertToLocalStudySession(StudySession apiSession) {
        try {
            int sessionId = 0;
            if (apiSession.getId() != null && !apiSession.getId().isEmpty()) {
                try {
                    sessionId = Integer.parseInt(apiSession.getId());
                } catch (NumberFormatException e) {
                    // ID가 정수가 아닌 경우 (예: UUID)
                    sessionId = 0;
                }
            }

            int subjectId = 0;
            if (apiSession.getSubject_id() != null && !apiSession.getSubject_id().isEmpty()) {
                subjectId = Integer.parseInt(apiSession.getSubject_id());
            }

            // 문자열에서 LocalDate로 변환
            LocalDate date = LocalDate.parse(apiSession.getDate(), DateTimeFormatter.ISO_LOCAL_DATE);

            // 문자열에서 LocalTime으로 변환
            LocalTime startTime = null;
            if (apiSession.getStart_time() != null && !apiSession.getStart_time().isEmpty()) {
                startTime = LocalTime.parse(apiSession.getStart_time(), DateTimeFormatter.ISO_LOCAL_TIME);
            }

            LocalTime endTime = null;
            if (apiSession.getEnd_time() != null && !apiSession.getEnd_time().isEmpty()) {
                endTime = LocalTime.parse(apiSession.getEnd_time(), DateTimeFormatter.ISO_LOCAL_TIME);
            }

            long restTime = apiSession.getRest_time() != null ? apiSession.getRest_time() : 0;

            // 객체 생성
            StudySessionEntity localSession = new StudySessionEntity(
                    subjectId,
                    date,
                    apiSession.getStudy_time(),
                    startTime,
                    endTime,
                    restTime);

            localSession.setSessionId(sessionId);
            localSession.setServerId(apiSession.getId());

            // created_at과 updated_at 설정
            if (apiSession.getCreated_at() != null) {
                try {
                    LocalDateTime createdAt = LocalDateTime.parse(apiSession.getCreated_at(),
                            DateTimeFormatter.ISO_DATE_TIME);
                    localSession.setCreatedAt(createdAt);
                } catch (Exception e) {
                    Log.e(TAG, "Created_at 변환 오류", e);
                    localSession.setCreatedAt(LocalDateTime.now());
                }
            }

            if (apiSession.getUpdated_at() != null) {
                try {
                    LocalDateTime updatedAt = LocalDateTime.parse(apiSession.getUpdated_at(),
                            DateTimeFormatter.ISO_DATE_TIME);
                    localSession.setUpdatedAt(updatedAt);
                } catch (Exception e) {
                    Log.e(TAG, "Updated_at 변환 오류", e);
                    localSession.setUpdatedAt(LocalDateTime.now());
                }
            }

            return localSession;
        } catch (Exception e) {
            Log.e(TAG, "StudySession 변환 오류", e);
            throw new RuntimeException("StudySession 변환 오류", e);
        }
    }

    /**
     * 리소스 해제
     */
    public void dispose() {
        disposables.clear();
    }
}