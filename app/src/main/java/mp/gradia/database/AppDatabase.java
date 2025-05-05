package mp.gradia.database;

import android.content.Context;

import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import androidx.room.TypeConverters;

import mp.gradia.database.converter.DateConverter;
import mp.gradia.database.converter.LocalDateConverter;
import mp.gradia.database.converter.LocalTimeConverter;
import mp.gradia.database.dao.StudySessionDao;
import mp.gradia.database.dao.SubjectDao;
import mp.gradia.database.dao.UserDao;
import mp.gradia.database.entity.StudySessionEntity;
import mp.gradia.database.entity.SubjectEntity;
import mp.gradia.database.entity.UserEntity;

@Database(
        entities = {UserEntity.class, SubjectEntity.class, StudySessionEntity.class},
        version = 4
)
@TypeConverters({LocalDateConverter.class, LocalTimeConverter.class})
public abstract class AppDatabase extends RoomDatabase {
    public abstract UserDao userDao();

    public abstract SubjectDao subjectDao();

    public abstract StudySessionDao studySessionDao();

    // If app runs in a single process, should follow the singleton design pattern when instantiating an AppDatabase object
    private static volatile AppDatabase INSTANCE;

    public static AppDatabase getInstance(Context context) {
        if (INSTANCE == null) {
            synchronized (AppDatabase.class) {
                if (INSTANCE == null) {
                    INSTANCE = Room.databaseBuilder(
                            context.getApplicationContext(),
                            AppDatabase.class,
                            "gradia.db"
                    ).fallbackToDestructiveMigration().build();
                }
            }
        }
        return INSTANCE;
    }
}
