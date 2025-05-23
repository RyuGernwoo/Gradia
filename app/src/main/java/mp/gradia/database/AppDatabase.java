package mp.gradia.database;

import android.content.Context;

import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import androidx.room.TypeConverters;

import mp.gradia.database.converter.DateConverter;
import mp.gradia.database.converter.LocalDateConverter;
import mp.gradia.database.converter.LocalDateTimeConverter;
import mp.gradia.database.converter.LocalTimeConverter;
import mp.gradia.database.dao.StudySessionDao;
import mp.gradia.database.dao.SubjectDao;
import mp.gradia.database.dao.TodoDao;
import mp.gradia.database.dao.UserDao;
import mp.gradia.database.entity.StudySessionEntity;
import mp.gradia.database.entity.SubjectEntity;
import mp.gradia.database.entity.TodoEntity;
import mp.gradia.database.entity.UserEntity;


@Database(entities = { UserEntity.class, SubjectEntity.class, StudySessionEntity.class, TodoEntity.class }, version = 5)
@TypeConverters({ LocalDateConverter.class, LocalTimeConverter.class, LocalDateTimeConverter.class,
        DateConverter.class })
public abstract class AppDatabase extends RoomDatabase {
    public abstract UserDao userDao();

    public abstract SubjectDao subjectDao();

    public abstract TodoDao todoDao();

    public abstract StudySessionDao studySessionDao();

    // If app runs in a single process, should follow the singleton design pattern
    // when instantiating an AppDatabase object
    private static volatile AppDatabase INSTANCE;

    public static AppDatabase getInstance(Context context) {
        if (INSTANCE == null) {
            synchronized (AppDatabase.class) {
                if (INSTANCE == null) {
                    INSTANCE = Room.databaseBuilder(
                            context.getApplicationContext(),
                            AppDatabase.class,
                            "gradia.db").fallbackToDestructiveMigration().build();
                }
            }
        }
        return INSTANCE;
    }

    /**
     * 테스트를 위한 임시 인스턴스 설정 메서드
     * 
     * @param mockDatabase 테스트용 Mock AppDatabase 인스턴스
     */
    public static void setInstanceForTesting(AppDatabase mockDatabase) {
        INSTANCE = mockDatabase;
    }

    /**
     * 테스트 후 인스턴스 초기화 메서드
     */
    public static void resetInstanceForTesting() {
        INSTANCE = null;
    }
}
