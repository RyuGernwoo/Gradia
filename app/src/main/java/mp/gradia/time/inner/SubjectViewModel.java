package mp.gradia.time.inner;

import android.view.View;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;
import androidx.lifecycle.viewmodel.ViewModelInitializer;

import java.util.List;

import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.disposables.CompositeDisposable;
import io.reactivex.rxjava3.schedulers.Schedulers;
import mp.gradia.database.AppDatabase;
import mp.gradia.database.dao.SubjectDao;
import mp.gradia.database.entity.SubjectEntity;

public class SubjectViewModel extends ViewModel {
    private static final String TAG = "SubjectViewModel";
    private final SubjectDao subjectDao;
    private final CompositeDisposable compositeDisposable = new CompositeDisposable();

    private final MutableLiveData<SubjectEntity> selectedSubjectMutableLiveData = new MutableLiveData<>();
    private final MutableLiveData<List<SubjectEntity>> subjectListMutableLiveData = new MutableLiveData<>();

    public LiveData<List<SubjectEntity>> subjectListLiveData = subjectListMutableLiveData;
    public LiveData<SubjectEntity> selectedSubjectLiveData = selectedSubjectMutableLiveData;

    public SubjectViewModel(SubjectDao subjectDao) {
        this.subjectDao = subjectDao;
        loadAllSubjects();
    }

    private void loadAllSubjects() {
        compositeDisposable.add(subjectDao.getAll()
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(
                        subjectList -> {
                            subjectListMutableLiveData.setValue(subjectList);
                        },
                        throwable -> {
                            // Exception Handling
                        }
                )
        );
    }

    public void selectSubject(SubjectEntity subject) {
        selectedSubjectMutableLiveData.setValue(subject);
    }

    @Override
    protected void onCleared() {
        super.onCleared();
        compositeDisposable.clear();
    }
}
