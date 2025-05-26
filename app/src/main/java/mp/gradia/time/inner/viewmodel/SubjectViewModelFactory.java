package mp.gradia.time.inner.viewmodel;

import android.util.Log;

import androidx.annotation.NonNull;
import androidx.lifecycle.ViewModel;
import androidx.lifecycle.ViewModelProvider;

import mp.gradia.database.dao.SubjectDao;

public class SubjectViewModelFactory implements ViewModelProvider.Factory {
    // DAO 인스턴스
    private final SubjectDao subjectDao;

    /**
     * SubjectViewModelFactory의 생성자입니다. SubjectDao를 인자로 받습니다.
     */
    public SubjectViewModelFactory(SubjectDao subjectDao) {
        this.subjectDao = subjectDao;
    }

    /**
     * 지정된 Class의 ViewModel 인스턴스를 생성합니다. SubjectViewModel만 생성 가능합니다.
     * 
     * @param modelClass 생성할 ViewModel의 Class 객체
     * @param <T>        ViewModel 타입
     * @return 생성된 ViewModel 인스턴스
     * @throws IllegalArgumentException 알 수 없는 ViewModel 클래스인 경우
     */
    @NonNull
    @Override
    public <T extends ViewModel> T create(@NonNull Class<T> modelClass) {
        if (modelClass.isAssignableFrom(SubjectViewModel.class)) {
            try {
                Log.d("VIEWMODEL", "Initialize Success");
                return (T) new SubjectViewModel(subjectDao);
            } catch (Exception e) {
                Log.e("ERROR", "CANNOT Initialize Viemodel");
                throw new RuntimeException("Cannot create an instance of " + modelClass, e);
            }
        }
        throw new IllegalArgumentException("Unknown ViewModel class: " + modelClass.getName());
    }
}