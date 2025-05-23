package mp.gradia.subject.repository;

import android.app.Application;
import android.content.Context;

import androidx.lifecycle.LiveData;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import mp.gradia.database.AppDatabase;
import mp.gradia.database.dao.TodoDao;
import mp.gradia.database.entity.TodoEntity;

public class TodoRepository {

    private final TodoDao todoDao;
    private final ExecutorService executorService = Executors.newSingleThreadExecutor();

    public TodoRepository(Context context) {
        AppDatabase db = AppDatabase.getInstance(context);
        todoDao = db.todoDao();
    }

    public LiveData<List<TodoEntity>> getTodosForSubject(int subjectId) {
        return todoDao.getTodosForSubject(subjectId);
    }

    public void insert(TodoEntity todo) {
        executorService.execute(() -> todoDao.insert(todo));
    }

    public void update(TodoEntity todo) {
        executorService.execute(() -> todoDao.update(todo));
    }

    public void delete(TodoEntity todo) {
        executorService.execute(() -> todoDao.delete(todo));
    }
}

