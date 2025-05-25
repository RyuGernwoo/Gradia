package mp.gradia.database.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;

import java.util.List;

import io.reactivex.rxjava3.core.Completable;
import io.reactivex.rxjava3.core.Flowable;
import io.reactivex.rxjava3.core.Single;
import mp.gradia.database.SubjectIdName;
import mp.gradia.database.entity.SubjectEntity;

@Dao
public interface SubjectDao {
    @Query("SELECT * FROM subjects")
    Flowable<List<SubjectEntity>> getAll();

    @Query("SELECT * FROM subjects WHERE subject_id = :id")
    Flowable<SubjectEntity> getByIdFlowable(int id);

    @Query("SELECT * FROM subjects WHERE subject_id = :id")
    Single<SubjectEntity> getByIdSingle(int id);

    @Query("SELECT * FROM subjects WHERE subject_id = :id")
    SubjectEntity getById(int id);

    @Query("SELECT * FROM subjects WHERE name LIKE '%' || :keyword || '%'")
    Flowable<List<SubjectEntity>> searchByName(String keyword);

    @Query("DELETE FROM subjects")
    Completable clearAll();

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    Completable insert(SubjectEntity... subject);

    @Update
    Completable update(SubjectEntity... subject);

    @Delete
    Completable delete(SubjectEntity... subject);

    @Query("SELECT subject_id, name FROM subjects")
    LiveData<List<SubjectIdName>> getAllSubjectIdNamePairs();

}
