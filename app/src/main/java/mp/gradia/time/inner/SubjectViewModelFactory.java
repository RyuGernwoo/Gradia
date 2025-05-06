package mp.gradia.time.inner;

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
                return (T) new SubjectViewModel(subjectDao);
            }
            catch (Exception e) {
                throw new RuntimeException("Cannot create an instance of " + modelClass, e);
            }
        }
        throw new IllegalArgumentException("Unknown ViewModel class: " + modelClass.getName());
    }
}
