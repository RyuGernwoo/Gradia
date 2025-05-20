package mp.gradia.database.dao;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;

import io.reactivex.rxjava3.core.Completable;
import io.reactivex.rxjava3.core.Maybe;
import mp.gradia.database.entity.UserEntity;

@Dao
public interface UserDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    Completable insertUser(UserEntity user);

    @Query("SELECT * FROM User WHERE login_provider = :loginProvider AND provider_id = :providerId LIMIT 1")
    Maybe<UserEntity> getUserByProviderInfo(String loginProvider, String providerId);

    @Query("SELECT * FROM User WHERE email = :email LIMIT 1")
    Maybe<UserEntity> getUserByEmail(String email);

    @Update
    Completable updateUser(UserEntity user);
}
