package mp.gradia.analysis;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;

import java.util.List;

import mp.gradia.database.SubjectIdName;
import mp.gradia.database.entity.DayStudyTime;
import mp.gradia.database.entity.StudySessionEntity;
import mp.gradia.database.entity.SubjectEntity;
import mp.gradia.database.entity.SubjectStudyTime;
import mp.gradia.database.repository.StudySessionRepository;
import mp.gradia.database.repository.SubjectRepository;

public class AnalysisViewModel extends AndroidViewModel {
    private final StudySessionRepository studySessionRepository;
    private final SubjectRepository subjectRepository;
    private final LiveData<List<SubjectStudyTime>> totalStudyTimes;
    private final LiveData<List<SubjectIdName>> subjectNames;
    private final LiveData<List<StudySessionEntity>> allSessions;
    private final LiveData<List<SubjectEntity>> allSubjects;

    public AnalysisViewModel(@NonNull Application application) {
        super(application);
        studySessionRepository = new StudySessionRepository(application);
        subjectRepository = new SubjectRepository(application);
        totalStudyTimes = studySessionRepository.getTotalStudyTimeBySubject();
        subjectNames = studySessionRepository.getAllSubjectIdNamePairs();
        allSessions = studySessionRepository.getAllSessions();
        allSubjects = subjectRepository.getAllSubjects();
    }

    public LiveData<List<SubjectEntity>> getAllSubjects() {
        return allSubjects;
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
        return studySessionRepository.getTodayStudyTime();
    }

    public LiveData<List<DayStudyTime>> getMonthlyStudyTime() {
        return studySessionRepository.getMonthlyStudyTime();
    }
}
