package mp.gradia.time.inner;

import android.util.Log;

import androidx.annotation.NonNull;
import androidx.lifecycle.ViewModel;
import androidx.lifecycle.ViewModelProvider;
import mp.gradia.database.dao.SubjectDao;

public class SubjectViewModelFactory implements ViewModelProvider.Factory {
    private final SubjectDao subjectDao;

    public SubjectViewModelFactory(SubjectDao subjectDao) {
        this.subjectDao = subjectDao;
    }

    @NonNull
    @Override
    public <T extends ViewModel> T create(@NonNull Class<T> modelClass) {
        if (modelClass.isAssignableFrom(SubjectViewModel.class)) {
            try {
                Log.d("VIEWMODEL", "Initialize Success");
                return (T) new SubjectViewModel(subjectDao);
            }
            catch (Exception e) {
                Log.e("ERROR", "CANNOT Initialize Viemodel");
                throw new RuntimeException("Cannot create an instance of " + modelClass, e);
            }
        }
        throw new IllegalArgumentException("Unknown ViewModel class: " + modelClass.getName());
    }
}
