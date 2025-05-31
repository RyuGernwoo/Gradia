package mp.gradia.database.repository;

import android.content.Context;
import android.util.Log;

import androidx.lifecycle.LiveData;

import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.disposables.CompositeDisposable;
import io.reactivex.rxjava3.schedulers.Schedulers;
import mp.gradia.api.AuthManager;
import mp.gradia.database.AppDatabase;
import mp.gradia.database.dao.SubjectDao;
import mp.gradia.database.dao.TodoDao;
import mp.gradia.database.entity.SubjectEntity;
import mp.gradia.database.entity.TodoEntity;

public class TodoRepository {
    private static final String TAG = "TodoRepository";

    private final TodoDao todoDao;
    private final SubjectDao subjectDao;
    private final ExecutorService executorService = Executors.newSingleThreadExecutor();
    private final AuthManager authManager;
    private final CompositeDisposable disposables = new CompositeDisposable();

    // Subject 서버 동기화를 위한 참조
    private SubjectRepository subjectRepository;

    public TodoRepository(Context context) {
        AppDatabase db = AppDatabase.getInstance(context);
        todoDao = db.todoDao();
        subjectDao = db.subjectDao();
        authManager = AuthManager.getInstance(context);
        subjectRepository = new SubjectRepository(context);
    }

    public LiveData<List<TodoEntity>> getTodosForSubject(int subjectId) {
        return todoDao.getTodosForSubject(subjectId);
    }

    public void insert(TodoEntity todo) {
        insert(todo, null);
    }

    public void insert(TodoEntity todo, SubjectRepository.CloudSyncCallback callback) {
        executorService.execute(() -> {
            try {
                // 1. Todo 추가
                todoDao.insert(todo);
                Log.d(TAG, "Todo 추가 완료: " + todo.content);

                // 2. Subject의 업데이트 시간 갱신 및 서버 동기화
                updateSubjectAndSync(todo.subjectId, callback);
            } catch (Exception e) {
                Log.e(TAG, "Todo 추가 실패", e);
                if (callback != null) {
                    callback.onError("Todo 추가 실패: " + e.getMessage());
                }
            }
        });
    }

    public void update(TodoEntity todo) {
        update(todo, null);
    }

    public void update(TodoEntity todo, SubjectRepository.CloudSyncCallback callback) {
        executorService.execute(() -> {
            try {
                // 1. Todo 업데이트
                todoDao.update(todo);
                Log.d(TAG, "Todo 업데이트 완료: " + todo.content);

                // 2. Subject의 업데이트 시간 갱신 및 서버 동기화
                updateSubjectAndSync(todo.subjectId, callback);
            } catch (Exception e) {
                Log.e(TAG, "Todo 업데이트 실패", e);
                if (callback != null) {
                    callback.onError("Todo 업데이트 실패: " + e.getMessage());
                }
            }
        });
    }

    public void delete(TodoEntity todo) {
        delete(todo, null);
    }

    public void delete(TodoEntity todo, SubjectRepository.CloudSyncCallback callback) {
        executorService.execute(() -> {
            try {
                // 1. Todo 삭제
                todoDao.delete(todo);
                Log.d(TAG, "Todo 삭제 완료: " + todo.content);

                // 2. Subject의 업데이트 시간 갱신 및 서버 동기화
                updateSubjectAndSync(todo.subjectId, callback);
            } catch (Exception e) {
                Log.e(TAG, "Todo 삭제 실패", e);
                if (callback != null) {
                    callback.onError("Todo 삭제 실패: " + e.getMessage());
                }
            }
        });
    }

    /**
     * Subject의 업데이트 시간을 갱신하고 서버에 동기화
     */
    private void updateSubjectAndSync(int subjectId, SubjectRepository.CloudSyncCallback callback) {
        try {
            // Subject 조회
            SubjectEntity subject = subjectDao.getByIdSync(subjectId);
            if (subject == null) {
                Log.w(TAG, "Subject를 찾을 수 없습니다: " + subjectId);
                if (callback != null) {
                    callback.onError("Subject를 찾을 수 없습니다.");
                }
                return;
            }

            // Subject의 업데이트 시간 갱신
            subject.setUpdatedAt(LocalDateTime.now());
            subjectDao.updateSync(subject);

            Log.d(TAG, "Subject 업데이트 시간 갱신 완료: " + subject.getName());

            // 서버 동기화 (로그인 상태이고 서버 ID가 있는 경우에만)
            if (authManager.isLoggedIn() && subject.getServerId() != null) {
                Log.d(TAG, "서버 동기화 시작: " + subject.getName());

                // SubjectRepository의 syncSubjectToServer 메서드를 사용하여 동기화
                disposables.add(subjectRepository.syncSubjectToServer(subject)
                        .subscribeOn(Schedulers.io())
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe(
                                () -> {
                                    Log.d(TAG, "Subject 서버 동기화 완료: " + subject.getName());
                                    if (callback != null) {
                                        callback.onSuccess();
                                    }
                                },
                                throwable -> {
                                    Log.w(TAG, "Subject 서버 동기화 실패 (나중에 재시도): " + subject.getName(), throwable);
                                    // Todo 변경은 성공했으므로 success로 처리
                                    if (callback != null) {
                                        callback.onSuccess();
                                    }
                                }));
            } else {
                Log.d(TAG, "서버 동기화 조건 불만족 - 로컬만 업데이트");
                if (callback != null) {
                    callback.onSuccess();
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Subject 업데이트 실패", e);
            if (callback != null) {
                callback.onError("Subject 업데이트 실패: " + e.getMessage());
            }
        }
    }

    /**
     * 리소스 해제
     */
    public void dispose() {
        disposables.clear();
        if (subjectRepository != null) {
            subjectRepository.dispose();
        }
    }
}
