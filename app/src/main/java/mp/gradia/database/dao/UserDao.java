package mp.gradia.database.dao;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;

import java.util.List;

import io.reactivex.rxjava3.core.Completable;
import io.reactivex.rxjava3.core.Flowable;
import mp.gradia.database.entity.UserEntity;

@Dao
public interface UserDao {

    // @Query("SELECT * FROM User")
    // Flowable<List<UserEntity>> getAll();

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    Completable insert(UserEntity... user);

    @Update
    Completable update(UserEntity... user);

    @Delete
    Completable delete(UserEntity... user);
}
