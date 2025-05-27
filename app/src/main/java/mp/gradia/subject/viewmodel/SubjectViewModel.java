package mp.gradia.subject.viewmodel;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;

import java.util.List;

import mp.gradia.database.entity.SubjectEntity;
import mp.gradia.database.repository.SubjectRepository;

public class SubjectViewModel extends AndroidViewModel {
    private int sortType = 0; // 0: 이름, 1: 학점, 2: 주간 목표
    private int filterType = -1; // -1: 전체, 0~2
    private String searchQuery = "";
    private final SubjectRepository repository;
    private final LiveData<List<SubjectEntity>> allSubjects;

    public int getSortType() {
        return sortType;
    }

    public void setSortType(int sortType) {
        this.sortType = sortType;
    }

    public int getFilterType() {
        return filterType;
    }

    public void setFilterType(int filterType) {
        this.filterType = filterType;
    }

    public String getSearchQuery() {
        return searchQuery;
    }

    public void setSearchQuery(String searchQuery) {
        this.searchQuery = searchQuery;
    }

    public SubjectViewModel(@NonNull Application application) {
        super(application);
        repository = new SubjectRepository(application);
        allSubjects = repository.getAllSubjects();
    }

    public LiveData<List<SubjectEntity>> getAllSubjects() {
        return allSubjects;
    }

    public LiveData<SubjectEntity> getSubjectById(int id) {
        return repository.getSubjectById(id);
    }

    public void insert(SubjectEntity subject) {
        repository.insert(subject);
    }

    public void update(SubjectEntity subject) {
        repository.update(subject);
    }

    public void delete(SubjectEntity subject) {
        repository.delete(subject);
    }

    public void insert(SubjectEntity subject, SubjectRepository.CloudSyncCallback callback) {
        repository.insert(subject, callback);
    }

    public void update(SubjectEntity subject, SubjectRepository.CloudSyncCallback callback) {
        repository.update(subject, callback);
    }

    public void delete(SubjectEntity subject, SubjectRepository.CloudSyncCallback callback) {
        repository.delete(subject, callback);
    }

    public void fetchEveryTimeTable(String url, SubjectRepository.CloudSyncCallback callback) {
        repository.fetchEveryTimeTable(url, callback);
    }

    /**
     * 로컬 DB 기준으로 클라우드 동기화 수행
     */
    public void syncLocalToCloud(SubjectRepository.CloudSyncCallback callback) {
        repository.syncLocalToCloud(callback);
    }

    @Override
    protected void onCleared() {
        super.onCleared();
        repository.dispose();
    }
}
