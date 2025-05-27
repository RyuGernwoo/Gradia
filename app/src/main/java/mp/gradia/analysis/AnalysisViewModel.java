package mp.gradia.analysis;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;

import java.util.List;

import mp.gradia.database.SubjectIdName;
import mp.gradia.database.entity.DayStudyTime;
import mp.gradia.database.entity.StudySessionEntity;
import mp.gradia.database.entity.SubjectStudyTime;
import mp.gradia.database.repository.StudySessionRepository;

public class AnalysisViewModel extends AndroidViewModel {

    private final StudySessionRepository repository;
    private final LiveData<List<SubjectStudyTime>> totalStudyTimes;
    private final LiveData<List<SubjectIdName>> subjectNames;
    private final LiveData<List<StudySessionEntity>> allSessions;

    public AnalysisViewModel(@NonNull Application application) {
        super(application);
        repository = new StudySessionRepository(application);
        totalStudyTimes = repository.getTotalStudyTimeBySubject();
        subjectNames = repository.getAllSubjectIdNamePairs();
        allSessions = repository.getAllSessions();
    }
    public LiveData<List<StudySessionEntity>> getAllSessions() {
        return allSessions;
    }

    public LiveData<List<SubjectStudyTime>> getTotalStudyTimes() {
        return totalStudyTimes;
    }

    public LiveData<List<SubjectIdName>> getSubjectNames() {
        return subjectNames;
    }
    public LiveData<Long> getTodayStudyTime() {
        return repository.getTodayStudyTime();
    }

    public LiveData<List<DayStudyTime>> getMonthlyStudyTime() {
        return repository.getMonthlyStudyTime();
    }

}

