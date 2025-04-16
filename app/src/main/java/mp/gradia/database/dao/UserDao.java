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
import io.reactivex.rxjava3.core.Maybe;
import mp.gradia.database.entity.UserEntity;

@Dao
public interface UserDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    Completable insert(UserEntity... user);

    @Update
    Completable update(UserEntity... user);

    @Delete
    Completable delete(UserEntity... user);

    // 로그인 쿼리
    @Query("SELECT * FROM User WHERE user_id = :uid AND password = :password")
    Maybe<UserEntity> login(String uid, String password);

    // 중복 확인 쿼리
    @Query("SELECT * FROM User WHERE user_id = :uid")
    Maybe<UserEntity> getUserById(String uid);
}
