package mp.gradia.subject.repository;

import android.app.Application;

import androidx.lifecycle.LiveData;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

import mp.gradia.database.AppDatabase;
import mp.gradia.database.dao.StudySessionDao;
import mp.gradia.database.dao.SubjectDao;
import mp.gradia.database.SubjectIdName;
import mp.gradia.database.entity.DayStudyTime;
import mp.gradia.database.entity.StudySessionEntity;
import mp.gradia.database.entity.SubjectStudyTime;

public class StudySessionRepository {

    private final StudySessionDao studySessionDao;
    private final SubjectDao subjectDao;

    public StudySessionRepository(Application application) {
        AppDatabase db = AppDatabase.getInstance(application);
        studySessionDao = db.studySessionDao();
        subjectDao = db.subjectDao();
    }

    public LiveData<List<SubjectStudyTime>> getTotalStudyTimeBySubject() {
        return studySessionDao.getTotalStudyTimePerSubject();
    }

    public LiveData<List<SubjectIdName>> getAllSubjectIdNamePairs() {
        return subjectDao.getAllSubjectIdNamePairs();
    }
    public LiveData<List<StudySessionEntity>> getAllSessions() {
        return studySessionDao.getAllSessions();
    }
    public LiveData<Long> getTodayStudyTime() {
        return studySessionDao.getTodayStudyTime(LocalDate.now());
    }

    public LiveData<List<DayStudyTime>> getMonthlyStudyTime() {
        String month = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM"));
        return studySessionDao.getMonthlyStudyTime(month);
    }


}

