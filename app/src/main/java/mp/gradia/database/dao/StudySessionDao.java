    package mp.gradia.database.dao;

import android.database.Observable;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;

import java.time.LocalDate;
import java.util.List;
import java.util.concurrent.Flow;

import io.reactivex.rxjava3.core.Completable;
import io.reactivex.rxjava3.core.Flowable;
import io.reactivex.rxjava3.core.Single;
import mp.gradia.database.entity.DayStudyTime;
import mp.gradia.database.entity.StudySessionEntity;
import mp.gradia.database.entity.SubjectStudyTime;

@Dao
public interface StudySessionDao {
    @Query("SELECT * FROM study_session")
    Flowable<List<StudySessionEntity>> getAllFlowable();

    @Query("SELECT * FROM study_session")
    Single<List<StudySessionEntity>> getAllSingle();

    @Query("SELECT * FROM study_session WHERE session_id = :id")
    Flowable<StudySessionEntity> getByIdFlowable(int id);

    @Query("SELECT * FROM study_session WHERE session_id = :id")
    Single<StudySessionEntity> getByIdSingle(int id);

    @Query("SELECT * FROM study_session WHERE date = :date")
    Flowable<List<StudySessionEntity>> getByDate(LocalDate date);

    @Query("SELECT * FROM study_session WHERE date = :date")
    Single<List<StudySessionEntity>> getByDateSingle(LocalDate date);

    @Query("DELETE FROM study_session WHERE session_id = :id")
    Completable deleteById(int id);

    @Query("DELETE FROM study_session")
    Completable clearAll();

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    Completable insert(StudySessionEntity... session);

    @Update
    Completable update(StudySessionEntity... session);

    @Delete
    Completable delete(StudySessionEntity... session);

    @Query("SELECT * FROM study_session")
    LiveData<List<StudySessionEntity>> getAllSessions();


    @Query("SELECT subject_id AS subjectId, SUM(study_time) AS totalTime " +
            "FROM study_session " +
            "GROUP BY subject_id")
    LiveData<List<SubjectStudyTime>> getTotalStudyTimePerSubject();

    @Query("SELECT SUM(study_time) FROM study_session WHERE date = :today")
    LiveData<Long> getTodayStudyTime(LocalDate today);

    @Query("SELECT date, SUM(study_time) as total FROM study_session WHERE strftime('%Y-%m', date) = :month GROUP BY date")
    LiveData<List<DayStudyTime>> getMonthlyStudyTime(String month);



}
