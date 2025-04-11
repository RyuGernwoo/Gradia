package mp.gradia.database.dao;

import android.database.Observable;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;

import java.util.List;
import java.util.concurrent.Flow;

import io.reactivex.rxjava3.core.Completable;
import io.reactivex.rxjava3.core.Flowable;
import mp.gradia.database.entity.StudySessionEntity;

@Dao
public interface StudySessionDao {
    @Query("SELECT * FROM study_session")
    Flowable<List<StudySessionEntity>> getAll();

    @Query("SELECT * FROM study_session WHERE session_id = :id")
    Flowable<StudySessionEntity> getById(int id);

    @Query("DELETE FROM study_session")
    Completable clearAll();

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    Completable insert(StudySessionEntity... session);

    @Update
    Completable update(StudySessionEntity... session);

    @Delete
    Completable delete(StudySessionEntity... session);

}
