package mp.gradia.time.inner.viewmodel;

import android.app.Application;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.lifecycle.ViewModel;
import androidx.lifecycle.ViewModelProvider;

import mp.gradia.database.dao.StudySessionDao;
import mp.gradia.database.dao.SubjectDao;

public class StudySessionViewModelFactory implements ViewModelProvider.Factory {
    // Application과 DAO Instance
    private final Application application;
    private final StudySessionDao sessionDao;
    private final SubjectDao subjectDao;

    /**
     * StudySessionViewModelFactory의 생성자입니다. Application과 StudySessionDao를 인자로 받습니다.
     */
    public StudySessionViewModelFactory(Application application, StudySessionDao sessionDao) {
        this.application = application;
        this.sessionDao = sessionDao;
        this.subjectDao = null;
    }

    /**
     * StudySessionViewModelFactory의 생성자입니다. Application, StudySessionDao과
     * SubjectDao를 인자로 받습니다.
     */
    public StudySessionViewModelFactory(Application application, StudySessionDao sessionDao, SubjectDao subjectDao) {
        this.application = application;
        this.sessionDao = sessionDao;
        this.subjectDao = subjectDao;
    }

    /**
     * 지정된 Class의 ViewModel 인스턴스를 생성합니다. StudySessionViewModel만 생성 가능합니다.
     */
    @NonNull
    @Override
    public <T extends ViewModel> T create(@NonNull Class<T> modelClass) {
        if (modelClass.isAssignableFrom(StudySessionViewModel.class)) {
            try {
                Log.d("VIEWMODEL", "Initialize Success");

                if (subjectDao == null)
                    return (T) new StudySessionViewModel(application, sessionDao);
                else
                    return (T) new StudySessionViewModel(application, sessionDao, subjectDao);

            } catch (Exception e) {
                Log.e("ERROR", "CANNOT Initialize Viemodel");
                throw new RuntimeException("Cannot create an instance of " + modelClass, e);
            }
        }
        throw new IllegalArgumentException("Unknown ViewModel class: " + modelClass.getName());
    }
}
