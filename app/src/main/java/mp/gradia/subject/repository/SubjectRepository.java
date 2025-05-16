package mp.gradia.subject.repository;

import android.app.Application;
import android.util.Log;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.LiveDataReactiveStreams;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import io.reactivex.rxjava3.schedulers.Schedulers;
import mp.gradia.database.AppDatabase;
import mp.gradia.database.dao.SubjectDao;
import mp.gradia.database.entity.SubjectEntity;

public class SubjectRepository {

    private final SubjectDao subjectDao;
    // 모든 과목 데이터를 LiveData 형태로 보관
    private final LiveData<List<SubjectEntity>> allSubjects;
    private final ExecutorService executorService = Executors.newSingleThreadExecutor();

    // DB 인스턴스를 가져오고 DAO 초기화
    public SubjectRepository(Application application) {
        AppDatabase db = AppDatabase.getInstance(application);
        subjectDao = db.subjectDao();
        allSubjects = LiveDataReactiveStreams.fromPublisher(subjectDao.getAll()); // 초기화 시 전체 과목 조회
    }

    // 전체 과목 목록 반환
    public LiveData<List<SubjectEntity>> getAllSubjects() {
        return allSubjects;
    }

    // 특정 ID로 과목 하나 조회
    public LiveData<SubjectEntity> getSubjectById(int id) {
        // LiveDataReactiveStreams.fromPublisher()를 사용하여 Flowable을 LiveData로 변환
        return LiveDataReactiveStreams.fromPublisher(subjectDao.getById(id));
    }

    public void insert(SubjectEntity subject) {
        // LiveStream 타입과는 달리 Completable 타입은 Cold Stream이기 때문에 susbcribe를 통해
        // 구독을 해야만 실행됨.
        subjectDao.insert(subject).subscribeOn(Schedulers.from(executorService)).subscribe();
    }

    public void update(SubjectEntity subject) {
        subjectDao.update(subject)
                .subscribeOn(Schedulers.from(executorService))
                .subscribe();
    }

    public void delete(SubjectEntity subject) {
        subjectDao.delete(subject)
                .subscribeOn(Schedulers.from(executorService))
                .subscribe();
    }
}
