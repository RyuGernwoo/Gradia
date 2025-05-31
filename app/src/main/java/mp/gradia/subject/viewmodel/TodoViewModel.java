package mp.gradia.subject.viewmodel;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;

import java.util.List;

import mp.gradia.database.entity.TodoEntity;
import mp.gradia.database.repository.SubjectRepository;
import mp.gradia.database.repository.TodoRepository;

public class TodoViewModel extends AndroidViewModel {

    private final TodoRepository repository;

    public TodoViewModel(@NonNull Application application) {
        super(application);
        repository = new TodoRepository(application);
    }

    public LiveData<List<TodoEntity>> getTodosForSubject(int subjectId) {
        return repository.getTodosForSubject(subjectId);
    }

    public void insert(TodoEntity todo) {
        repository.insert(todo);
    }

    public void insert(TodoEntity todo, SubjectRepository.CloudSyncCallback callback) {
        repository.insert(todo, callback);
    }

    public void update(TodoEntity todo) {
        repository.update(todo);
    }

    public void update(TodoEntity todo, SubjectRepository.CloudSyncCallback callback) {
        repository.update(todo, callback);
    }

    public void delete(TodoEntity todo) {
        repository.delete(todo);
    }

    public void delete(TodoEntity todo, SubjectRepository.CloudSyncCallback callback) {
        repository.delete(todo, callback);
    }

    @Override
    protected void onCleared() {
        super.onCleared();
        repository.dispose();
    }
}
