package mp.gradia.database.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

import java.util.List;

import io.reactivex.rxjava3.core.Completable;
import io.reactivex.rxjava3.core.Flowable;
import io.reactivex.rxjava3.core.Single;
import mp.gradia.database.entity.TodoEntity;

// Room 라이브러리가 DAO를 기반으로 자동으로 SQL 코드를 생성
@Dao
public interface TodoDao {
    @Insert
    void insert(TodoEntity todo);

    @Insert
    Completable insertCompletable(TodoEntity todo);

    @Update
    void update(TodoEntity todo);

    @Delete
    void delete(TodoEntity todo);

    // 최신 항목이 위에 오게
    @Query("SELECT * FROM todos WHERE subject_id = :subjectId ORDER BY todo_id DESC")
    LiveData<List<TodoEntity>> getTodosForSubject(int subjectId);

    @Query("SELECT * FROM todos WHERE subject_id = :subjectId ORDER BY todo_id DESC")
    Flowable<List<TodoEntity>> getTodosForSubjectFlowable(int subjectId);

    @Query("SELECT * FROM todos WHERE subject_id = :subjectId ORDER BY todo_id DESC")
    Single<List<TodoEntity>> getTodosForSubjectSingleRx(int subjectId);

    // 동기적으로 Todo 목록을 조회하는 메서드 추가
    @Query("SELECT * FROM todos WHERE subject_id = :subjectId ORDER BY todo_id DESC")
    List<TodoEntity> getTodosForSubjectSync(int subjectId);
}
