//package mp.gradia.subject.viewmodel;
//
//import android.app.Application;
//
//import androidx.annotation.NonNull;
//import androidx.lifecycle.AndroidViewModel;
//import androidx.lifecycle.LiveData;
//
//import java.util.List;
//
//public class TodoViewModel extends AndroidViewModel {
//
//    private final TodoRepository repository;
//
//    public TodoViewModel(@NonNull Application application) {
//        super(application);
//        repository = new TodoRepository(application);
//    }
//
//    public LiveData<List<TodoEntity>> getTodosForSubject(int subjectId) {
//        return repository.getTodosForSubject(subjectId);
//    }
//
//    public void insert(TodoEntity todo) {
//        repository.insert(todo);
//    }
//
//    public void update(TodoEntity todo) {
//        repository.update(todo);
//    }
//
//    public void delete(TodoEntity todo) {
//        repository.delete(todo);
//    }
//}
