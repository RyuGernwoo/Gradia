package mp.gradia.api;

import android.content.Context;
import android.util.Log;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
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
     */
    private Completable uploadSubjects(String authHeader, SyncCallback callback) {
        return database.subjectDao().getAll()
                .firstElement()
                .flatMapCompletable(subjects -> {
                    AtomicInteger progress = new AtomicInteger(0);
                    int total = subjects.size();

                    return Completable.create(emitter -> {
                        if (subjects.isEmpty()) {
                            emitter.onComplete();
                            return;
                        }

                        // 각 과목에 대해 서버에 업로드
                        for (SubjectEntity localSubject : subjects) {
                            Subject apiSubject = convertToApiSubject(localSubject);

                            // 이미 서버에 존재하는 과목인지 확인하고 적절한 API 메서드 호출
                            if (localSubject.getSubjectId() > 0) {
                                // 기존 과목 업데이트
                                apiService
                                        .updateSubject(authHeader, String.valueOf(localSubject.getSubjectId()),
                                                apiSubject)
                                        .enqueue(createSubjectCallback(emitter, progress, total, callback));
                            } else {
                                // 새 과목 생성
                                apiService.createSubject(authHeader, apiSubject)
                                        .enqueue(createSubjectCallback(emitter, progress, total, callback));
                            }
                        }
                    });
                });
    }

    /**
     * Room DB의 학습 세션 데이터를 원격 서버에 업로드합니다.
     */
    private Completable uploadStudySessions(String authHeader, SyncCallback callback) {
        return database.studySessionDao().getAll()
                .firstElement()
                .flatMapCompletable(sessions -> {
                    AtomicInteger progress = new AtomicInteger(0);
                    int total = sessions.size();

                    return Completable.create(emitter -> {
                        if (sessions.isEmpty()) {
                            emitter.onComplete();
                            return;
                        }

                        // 각 학습 세션에 대해 서버에 업로드
                        for (StudySessionEntity localSession : sessions) {
                            StudySession apiSession = convertToApiStudySession(localSession);

                            // 이미 서버에 존재하는 세션인지 확인하고 적절한 API 메서드 호출
                            if (localSession.getSessionId() > 0) {
                                // 기존 세션 업데이트
                                apiService
                                        .updateStudySession(authHeader, String.valueOf(localSession.getSessionId()),
                                                apiSession)
                                        .enqueue(createSessionCallback(emitter, progress, total, callback));
                            } else {
                                // 새 세션 생성
                                apiService.createStudySession(authHeader, apiSession)
                                        .enqueue(createSessionCallback(emitter, progress, total, callback));
                            }
                        }
                    });
                });
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
        apiSubject.setName(localSubject.getName());
        apiSubject.setType(localSubject.getType());
        apiSubject.setCredit(localSubject.getCredit());
        apiSubject.setDifficulty(localSubject.getDifficulty());
        apiSubject.setMid_term_schedule(localSubject.getMidTermSchedule());
        apiSubject.setFinal_term_schedule(localSubject.getFinalTermSchedule());
        apiSubject.setColor(localSubject.getColor());

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

        return apiSession;
    }

    /**
     * API Subject 모델을 로컬 SubjectEntity로 변환합니다.
     */
    private SubjectEntity convertToLocalSubject(Subject apiSubject) {
        try {
            int subjectId = 0;
            if (apiSubject.getId() != null && !apiSubject.getId().isEmpty()) {
                subjectId = Integer.parseInt(apiSubject.getId());
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
            localSubject.setDifficulty(apiSubject.getDifficulty());

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
                sessionId = Integer.parseInt(apiSession.getId());
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

            return localSession;
        } catch (Exception e) {
            Log.e(TAG, "StudySession 변환 오류", e);
            throw new RuntimeException("StudySession 변환 오류", e);
        }
    }

    /**
     * 과목 동기화 콜백 생성
     */
    private Callback<Subject> createSubjectCallback(
            CompletableEmitter emitter,
            AtomicInteger progress,
            int total,
            SyncCallback callback) {
        return new Callback<Subject>() {
            @Override
            public void onResponse(Call<Subject> call, Response<Subject> response) {
                int currentProgress = progress.incrementAndGet();
                AndroidSchedulers.mainThread().scheduleDirect(() -> callback.onProgress(currentProgress, total));

                if (currentProgress >= total) {
                    emitter.onComplete();
                }
            }

            @Override
            public void onFailure(Call<Subject> call, Throwable t) {
                if (!emitter.isDisposed()) {
                    emitter.onError(t);
                }
            }
        };
    }

    /**
     * 학습 세션 동기화 콜백 생성
     */
    private Callback<StudySession> createSessionCallback(
            CompletableEmitter emitter,
            AtomicInteger progress,
            int total,
            SyncCallback callback) {
        return new Callback<StudySession>() {
            @Override
            public void onResponse(Call<StudySession> call, Response<StudySession> response) {
                int currentProgress = progress.incrementAndGet();
                AndroidSchedulers.mainThread().scheduleDirect(() -> callback.onProgress(currentProgress, total));

                if (currentProgress >= total) {
                    emitter.onComplete();
                }
            }

            @Override
            public void onFailure(Call<StudySession> call, Throwable t) {
                if (!emitter.isDisposed()) {
                    emitter.onError(t);
                }
            }
        };
    }

    /**
     * 리소스 해제
     */
    public void dispose() {
        disposables.clear();
    }
}