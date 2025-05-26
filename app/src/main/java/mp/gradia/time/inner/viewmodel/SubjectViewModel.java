package mp.gradia.time.inner.viewmodel;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import java.lang.annotation.Target;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.disposables.CompositeDisposable;
import io.reactivex.rxjava3.schedulers.Schedulers;
import mp.gradia.database.dao.SubjectDao;
import mp.gradia.database.entity.SubjectEntity;
import mp.gradia.database.entity.TargetStudyTime;
import mp.gradia.subject.Subject;

public class SubjectViewModel extends ViewModel {
    // 로깅 태그
    private static final String TAG = "SubjectViewModel";

    // DAO 및 Disposables
    private final SubjectDao subjectDao;
    private final CompositeDisposable compositeDisposable = new CompositeDisposable();

    // 과목 데이터 LiveData
    private final MutableLiveData<SubjectEntity> selectedSubjectMutableLiveData = new MutableLiveData<>();
    private final MutableLiveData<List<SubjectEntity>> subjectListMutableLiveData = new MutableLiveData<>();
    private final MutableLiveData<Map<Integer, TargetStudyTime>> subjectTargetStudyTimeMutableLiveData = new MutableLiveData<>();
    public LiveData<List<SubjectEntity>> subjectListLiveData = subjectListMutableLiveData;
    public LiveData<SubjectEntity> selectedSubjectLiveData = selectedSubjectMutableLiveData;
    public LiveData<Map<Integer, TargetStudyTime>> subjectTargetStudyTimeLiveData = subjectTargetStudyTimeMutableLiveData;

    /**
     * SubjectViewModel의 생성자입니다. SubjectDao를 인자로 받아 과목 목록을 로드합니다.
     */
    public SubjectViewModel(SubjectDao subjectDao) {
        this.subjectDao = subjectDao;
        loadAllSubjects();
    }

    /**
     * 데이터베이스에서 모든 과목 목록을 로드합니다.
     */
    private void loadAllSubjects() {
        compositeDisposable.add(subjectDao.getAll()
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(
                        subjectList -> {
                            subjectListMutableLiveData.setValue(subjectList);
                        },
                        throwable -> {
                            // 예외 처리
                        }));
    }

    /**
     * 선택된 과목을 설정하고 LiveData를 업데이트합니다.
     *
     * @param subject 선택된 SubjectEntity
     */
    public void selectSubject(SubjectEntity subject) {
        selectedSubjectMutableLiveData.setValue(subject);
    }

    /**
     * ID를 통해 과목을 로드하고 LiveData를 업데이트합니다.
     *
     * @param id 로드할 과목의 ID
     */
    public void loadSubjectById(int id) {
        compositeDisposable.add(subjectDao.getByIdSingle(id)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(
                        subject -> {
                            selectedSubjectMutableLiveData.setValue(subject);
                        },
                        throwable -> {
                            // 예외 처리
                        }));
    }

    public void loadSubjectTargetStudyTime() {
        compositeDisposable.add(subjectDao.getAll()
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(
                        subjectList -> {
                            Map<Integer, TargetStudyTime> targetStudyTimeMap = new HashMap<>();
                            for (SubjectEntity subject : subjectList) {
                                targetStudyTimeMap.put(subject.getSubjectId(), subject.getTime());
                            }
                            subjectTargetStudyTimeMutableLiveData.setValue(targetStudyTimeMap);
                        },
                        throwable -> {
                            // 예외 처리
                        }
                )
        );
    }

    /**
     * ViewModel이 더 이상 사용되지 않을 때 호출됩니다. RxJava Disposables를 해제합니다.
     */
    @Override
    protected void onCleared() {
        super.onCleared();
        compositeDisposable.clear();
    }
}