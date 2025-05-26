package mp.gradia.time.inner.viewmodel;

import android.app.Application;
import android.graphics.Color;
import android.util.Log;

import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.core.Single;
import io.reactivex.rxjava3.disposables.CompositeDisposable;
import io.reactivex.rxjava3.schedulers.Schedulers;
import mp.gradia.Event;
import mp.gradia.database.dao.StudySessionDao;
import mp.gradia.database.dao.SubjectDao;
import mp.gradia.database.entity.StudySessionEntity;
import mp.gradia.database.entity.SubjectEntity;
import mp.gradia.subject.repository.StudySessionRepository;
import orion.gz.scheduleview.ScheduleEventItem;

public class StudySessionViewModel extends AndroidViewModel {
    // 로깅 태그
    private static final String TAG = "StudySessionViewModel";

    // Repository 및 Disposables
    private final StudySessionRepository repository;
    private final StudySessionDao sessionDao;
    private final SubjectDao subjectDao;
    private final CompositeDisposable compositeDisposable = new CompositeDisposable();

    // LiveData for Study Session Data
    private final MutableLiveData<Event<StudySessionEntity>> selectedSessionMutableLiveData = new MutableLiveData<>();
    private final MutableLiveData<List<StudySessionEntity>> sessionListMutableLiveData = new MutableLiveData<>();
    private final MutableLiveData<List<StudySessionEntity>> sessionListByDateMutableLiveData = new MutableLiveData<>();
    private final MutableLiveData<List<StudySessionEntity>> scheduleSessionMutableLiveData = new MutableLiveData<>();
    private final MutableLiveData<List<ScheduleEventItem>> scheduleItemMutableLiveData = new MutableLiveData<>();
    private final MutableLiveData<ScheduleDataBundle> scheduleDataBundleMutableLiveData = new MutableLiveData<>();
    private final MutableLiveData<String> errorMessageMutableLiveData = new MutableLiveData<>();

    public LiveData<Event<StudySessionEntity>> selectedSessionLiveData = selectedSessionMutableLiveData;
    public LiveData<List<StudySessionEntity>> sessionListLiveData = sessionListMutableLiveData;
    public LiveData<List<StudySessionEntity>> sessionListByDateLiveData = sessionListByDateMutableLiveData;
    public LiveData<List<StudySessionEntity>> scheduleSessionLiveData = scheduleSessionMutableLiveData;
    public LiveData<List<ScheduleEventItem>> scheduleItemLiveData = scheduleItemMutableLiveData;
    public LiveData<ScheduleDataBundle> scheduleDataBundleLiveData = scheduleDataBundleMutableLiveData;
    public LiveData<String> errorMessageLiveData = errorMessageMutableLiveData;

    /**
     * StudySessionViewModel의 생성자입니다. Application을 인자로 받아 Repository를 초기화합니다.
     */
    public StudySessionViewModel(Application application, StudySessionDao sessionDao) {
        super(application);
        this.repository = new StudySessionRepository(application);
        this.sessionDao = sessionDao;
        this.subjectDao = null;
    }

    /**
     * StudySessionViewModel의 생성자입니다. Application과 DAO들을 인자로 받습니다.
     */
    public StudySessionViewModel(Application application, StudySessionDao sessionDao, SubjectDao subjectDao) {
        super(application);
        this.repository = new StudySessionRepository(application);
        this.sessionDao = sessionDao;
        this.subjectDao = subjectDao;
    }

    /**
     * 데이터베이스에서 모든 세션 목록을 로드합니다.
     */
    public void loadAllSessions() {
        compositeDisposable.add(sessionDao.getAll()
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(
                        sessionList -> {
                            sessionListMutableLiveData.setValue(sessionList);
                        },
                        throwable -> {
                            // 예외 처리
                        }));
    }

    /**
     * 특정 날짜에 해당하는 스케줄 아이템을 로드합니다.
     * 해당 날짜의 세션과 전날에 시작하여 해당 날짜에 종료되는 세션을 포함합니다.
     * 
     * @param date 로드할 날짜
     */
    public void loadScheduleItemsByDate(final LocalDate date) {
        if (date == null)
            return;

        Single<List<StudySessionEntity>> sessionDate = sessionDao.getByDateSingle(date)
                .subscribeOn(Schedulers.io());
        Single<List<StudySessionEntity>> sessionEndDate = sessionDao.getByDateSingle(date.minusDays(1))
                .subscribeOn(Schedulers.io());
        List<StudySessionEntity> scheduleSession = new ArrayList<>();

        compositeDisposable.add(
                Single.zip(sessionDate, sessionEndDate,
                        (sessionsToday, sessionsYesterday) -> {
                            List<ScheduleEventItem> items = new ArrayList<>();

                            for (StudySessionEntity session : sessionsToday) {
                                SubjectEntity subject = subjectDao.getById(session.getSubjectId());
                                int eventColor = Color.parseColor(subject.getColor());

                                LocalDate startDate = session.getDate();
                                LocalDate endDate = session.getEndDate();

                                LocalTime startTime = session.getStartTime();
                                LocalTime endTime = session.getEndTime();
                                LocalTime displayTime = startTime;
                                LocalTime displayEndTime = endTime;

                                // 세션이 여러 날에 걸쳐 있는 경우
                                // !endDate.equals(date) 와 동치
                                if (endDate.isAfter(startDate)) {
                                    displayEndTime = LocalTime.MAX;
                                }
                                addEventItem(items, session, displayTime, displayEndTime, eventColor);
                                scheduleSession.add(session);
                                Log.d("SessionStatisticalDialog", session.getSubjectName());
                            }

                            for (StudySessionEntity session : sessionsYesterday) {
                                LocalDate startDate = session.getDate();
                                LocalDate endDate = session.getEndDate();

                                // 여러 날에 걸친 이벤트 중 date 에 종료되는 이벤트만 추가
                                if (startDate.isBefore(date) && endDate.equals(date) && startDate.isBefore(endDate)) {
                                    SubjectEntity subject = subjectDao.getById(session.getSubjectId());
                                    int eventColor = Color.parseColor(subject.getColor());

                                    LocalTime endTime = session.getEndTime();
                                    addEventItem(items, session, LocalTime.MIN, endTime, eventColor);
                                    scheduleSession.add(session);
                                    Log.d("SessionStatisticalDialog", session.getSubjectName());
                                }
                            }
                            return new ScheduleDataBundle(items, scheduleSession);
                        })
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe(
                                scheduleDataBundle -> {
                                    List<ScheduleEventItem> scheduleEventList = scheduleDataBundle.getEventItemList();
                                    List<StudySessionEntity> scheduleSessionList = scheduleDataBundle
                                            .getEventSessionList();

                                    scheduleDataBundleMutableLiveData.setValue(scheduleDataBundle);
                                    scheduleItemMutableLiveData.setValue(scheduleEventList);
                                    scheduleSessionMutableLiveData.setValue(scheduleSessionList);
                                    for (StudySessionEntity session : scheduleSession) {
                                        Log.d("SessionStatisticalDialog",
                                                session.getSubjectName() + " " + session.getDate());
                                    }
                                    Log.d("SessionStatisticalDialog", "save complete");
                                },
                                throwable -> {
                                    scheduleItemMutableLiveData.setValue(new ArrayList<>());
                                    scheduleSessionMutableLiveData.setValue(new ArrayList<>());
                                }));
    }

    /**
     * 스케줄 데이터 번들을 LiveData로 반환합니다.
     * 
     * @return 스케줄 데이터 번들 LiveData
     */
    public LiveData<ScheduleDataBundle> getScheduleData() {
        return scheduleDataBundleLiveData;
    }

    /**
     * 스케줄 이벤트 아이템을 목록에 추가합니다.
     * 
     * @param items      스케줄 이벤트 아이템 목록
     * @param session    추가할 StudySessionEntity
     * @param startTime  이벤트 시작 시간
     * @param endTime    이벤트 종료 시간
     * @param eventColor 이벤트 색상
     */
    private void addEventItem(List<ScheduleEventItem> items, StudySessionEntity session, LocalTime startTime,
            LocalTime endTime, int eventColor) {
        if (startTime.isAfter(endTime) || startTime.equals(endTime)
                || (startTime.equals(LocalTime.MIN) && endTime.equals(LocalTime.MAX)))
            return;

        items.add(new ScheduleEventItem(session.getSessionId(), session.getSubjectName(), startTime, endTime,
                eventColor));
    }

    /**
     * 선택된 세션을 설정하고 LiveData를 업데이트합니다.
     * 
     * @param session 선택된 StudySessionEntity
     */
    public void selectSession(StudySessionEntity session) {
        selectedSessionMutableLiveData.setValue(new Event<>(session));
    }

    /**
     * ID를 통해 세션을 로드하고 LiveData를 업데이트합니다.
     * 
     * @param sessionId 로드할 세션의 ID
     */
    public void selectSessionById(int sessionId) {
        compositeDisposable.add(
                sessionDao.getByIdSingle(sessionId)
                        .subscribeOn(Schedulers.io())
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe(
                                session -> {
                                    selectedSessionMutableLiveData.setValue(new Event<>(session));
                                },
                                throwable -> {
                                    Log.e("StudySessionViewModel", "Error loading session by ID", throwable);
                                }));
    }

    /**
     * 선택된 세션 LiveData를 반환합니다.
     * 
     * @return 선택된 세션 LiveData
     */
    public LiveData<Event<StudySessionEntity>> getSelectedSession() {
        return selectedSessionLiveData;
    }

    /**
     * 스케줄 세션 목록 LiveData를 반환합니다.
     * 
     * @return 스케줄 세션 목록 LiveData
     */
    public LiveData<List<StudySessionEntity>> getScheduleSessionLiveData() {
        return scheduleSessionLiveData;
    }

    /**
     * 세션을 업데이트합니다. (클라우드 동기화 포함)
     * 
     * @param session 업데이트할 StudySessionEntity
     */
    public void updateSession(StudySessionEntity session) {
        repository.update(session, new StudySessionRepository.CloudSyncCallback() {
            @Override
            public void onSuccess() {
                Log.d(TAG, "Session updated successfully with cloud sync");
            }

            @Override
            public void onError(String message) {
                Log.e(TAG, "Error updating session: " + message);
                errorMessageMutableLiveData.setValue("세션 업데이트 실패: " + message);
            }
        });
    }

    /**
     * 세션을 저장합니다. (클라우드 동기화 포함)
     * 
     * @param session 저장할 StudySessionEntity
     */
    public void saveSession(StudySessionEntity session) {
        repository.insert(session, new StudySessionRepository.CloudSyncCallback() {
            @Override
            public void onSuccess() {
                Log.d(TAG, "Session saved successfully with cloud sync");
            }

            @Override
            public void onError(String message) {
                Log.e(TAG, "Error saving session: " + message);
                errorMessageMutableLiveData.setValue("세션 저장 실패: " + message);
            }
        });
    }

    /**
     * 세션을 삭제합니다. (클라우드 동기화 포함)
     * 
     * @param session 삭제할 StudySessionEntity
     */
    public void deleteSession(StudySessionEntity session) {
        repository.delete(session, new StudySessionRepository.CloudSyncCallback() {
            @Override
            public void onSuccess() {
                Log.d(TAG, "Session deleted successfully with cloud sync");
            }

            @Override
            public void onError(String message) {
                Log.e(TAG, "Error deleting session: " + message);
                errorMessageMutableLiveData.setValue("세션 삭제 실패: " + message);
            }
        });
    }

    /**
     * Repository 리소스를 해제합니다.
     */
    @Override
    protected void onCleared() {
        super.onCleared();
        compositeDisposable.clear();
        repository.dispose();
    }

    /**
     * 스케줄 이벤트 아이템과 스터디 세션 엔티티 목록을 묶는 번들 클래스입니다.
     */
    public static class ScheduleDataBundle {
        final List<ScheduleEventItem> eventItemList;
        final List<StudySessionEntity> eventSessionList;

        /**
         * ScheduleDataBundle의 생성자입니다.
         * 
         * @param eventItemList    스케줄 이벤트 아이템 목록
         * @param eventSessionList 스터디 세션 엔티티 목록
         */
        ScheduleDataBundle(List<ScheduleEventItem> eventItemList, List<StudySessionEntity> eventSessionList) {
            this.eventItemList = eventItemList;
            this.eventSessionList = eventSessionList;
        }

        /**
         * 스케줄 이벤트 아이템 목록을 반환합니다.
         * 
         * @return 스케줄 이벤트 아이템 목록
         */
        public List<ScheduleEventItem> getEventItemList() {
            return eventItemList;
        }

        /**
         * 스터디 세션 엔티티 목록을 반환합니다.
         * 
         * @return 스터디 세션 엔티티 목록
         */
        public List<StudySessionEntity> getEventSessionList() {
            return eventSessionList;
        }
    }
}