package mp.gradia.time.inner.viewmodel;

import android.app.Application;
import android.graphics.Color;
import android.util.Log;

import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.anychart.charts.Sunburst;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.temporal.TemporalAdjuster;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.annotations.NonNull;
import io.reactivex.rxjava3.core.Single;
import io.reactivex.rxjava3.core.SingleObserver;
import io.reactivex.rxjava3.disposables.CompositeDisposable;
import io.reactivex.rxjava3.schedulers.Schedulers;
import mp.gradia.Event;
import mp.gradia.api.models.StudySession;
import mp.gradia.database.dao.StudySessionDao;
import mp.gradia.database.dao.SubjectDao;
import mp.gradia.database.entity.StudySessionEntity;
import mp.gradia.database.entity.SubjectEntity;
import mp.gradia.subject.repository.StudySessionRepository;
import mp.gradia.database.entity.TargetStudyTime;
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
    private final MutableLiveData<StudySessionEntity> selectedSessionMutableLiveData = new MutableLiveData<>();
    private final MutableLiveData<List<StudySessionEntity>> sessionListMutableLiveData = new MutableLiveData<>();
    private final MutableLiveData<List<StudySessionEntity>> sessionListByDateMutableLiveData = new MutableLiveData<>();
    private final MutableLiveData<List<StudySessionEntity>> scheduleSessionMutableLiveData = new MutableLiveData<>();
    private final MutableLiveData<List<StudySessionEntity>> scheduleWeeklySessionMutableLiveData = new MutableLiveData<>();
    private final MutableLiveData<List<StudySessionEntity>> scheduleMonthlySessionMutableLiveData = new MutableLiveData<>();
    private final MutableLiveData<List<ScheduleEventItem>> scheduleItemMutableLiveData = new MutableLiveData<>();
    private final MutableLiveData<ScheduleDataBundle> scheduleDataBundleMutableLiveData = new MutableLiveData<>();
    private final MutableLiveData<String> errorMessageMutableLiveData = new MutableLiveData<>();

    public LiveData<StudySessionEntity> selectedSessionLiveData = selectedSessionMutableLiveData;
    public LiveData<List<StudySessionEntity>> sessionListLiveData = sessionListMutableLiveData;
    public LiveData<List<StudySessionEntity>> sessionListByDateLiveData = sessionListByDateMutableLiveData;
    public LiveData<List<StudySessionEntity>> scheduleSessionLiveData = scheduleSessionMutableLiveData;
    public LiveData<List<StudySessionEntity>> scheduleWeeklySessionLiveData = scheduleWeeklySessionMutableLiveData;
    public LiveData<List<StudySessionEntity>> scheduleMonthlySessionLiveData = scheduleMonthlySessionMutableLiveData;
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
        compositeDisposable.add(sessionDao.getAllFlowable()
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
        Single<List<StudySessionEntity>> allSessions = sessionDao.getAllSingle()
                .subscribeOn(Schedulers.io());
        List<StudySessionEntity> scheduleSession = new ArrayList<>();

        compositeDisposable.add(
                Single.zip(sessionDate, sessionEndDate, allSessions,
                        (sessionsToday, sessionsYesterday, sessions) -> {
                                    Log.d("SessionStatisticalDialog", "loadScheduleItemsByDate");
                                    for (StudySessionEntity session : sessionsToday) {
                                        Log.d("SessionStatisticalDialog", session.getSubjectName() + " " + session.getDate() + " " + session.getStartTime());
                                    }

                            List<ScheduleEventItem> items = new ArrayList<>();
                                    List<StudySessionEntity> weeklySession = new ArrayList<>();
                                    List<StudySessionEntity> monthlySession = new ArrayList<>();
                                    Map<Integer, TargetStudyTime> targetStudyTimes = new HashMap();
                                    int[] subjectIds = new int[sessionsToday.size()];

                                    // weekly
                                    LocalDate monday = date.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
                                    LocalDate sunday = monday.with(TemporalAdjusters.nextOrSame(DayOfWeek.SUNDAY));

                            // monthly
                                    LocalDate startOfMonth = date.withDayOfMonth(1);
                                    LocalDate endOfMonth = date.with(TemporalAdjusters.lastDayOfMonth());

                                    for (int i = 0; i < sessionsToday.size(); i++) {
                                SubjectEntity subject = subjectDao.getById(sessionsToday.get(i).getSubjectId());
                                int eventColor = Color.parseColor(subject.getColor());

                                LocalDate startDate = sessionsToday.get(i).getDate();
                                LocalDate endDate = sessionsToday.get(i).getEndDate();

                                LocalTime startTime = sessionsToday.get(i).getStartTime();
                                LocalTime endTime = sessionsToday.get(i).getEndTime();
                                LocalTime displayTime = startTime;
                                LocalTime displayEndTime = endTime;

                                // 세션이 여러 날에 걸쳐 있는 경우
                                // !endDate.equals(date) 와 동치
                                if (endDate.isAfter(startDate)) {
                                    displayEndTime = LocalTime.MAX;
                                }
                                subjectIds[i] = sessionsToday.get(i).getSubjectId();
                                        addEventItem(items, sessionsToday.get(i), displayTime, displayEndTime, eventColor);
                                scheduleSession.add(sessionsToday.get(i));
                                targetStudyTimes.put(sessionsToday.get(i).getSubjectId(), subjectDao.getById(sessionsToday.get(i).getSubjectId()).getTime());
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
                                    targetStudyTimes.put(session.getSubjectId(), subjectDao.getById(session.getSubjectId()).getTime());
                                        }
                                    }

                                    // Weekly
                                    for (int i = 0; i < sessions.size(); i++) {
                                        for (int j = 0; j < subjectIds.length; j++) {
                                            if (sessions.get(i).getSubjectId() == subjectIds[j]) {
                                                LocalDate d = sessions.get(i).getDate();
                                                if (d.equals(monday) || d.equals(sunday) || (d.isAfter(monday) && d.isBefore(sunday)))
                                                    weeklySession.add(sessions.get(i));
                                            }
                                        }
                                    }

                                    // Monthly
                                    for (int i = 0; i < sessions.size(); i++) {
                                        for (int j = 0; j < subjectIds.length; j++) {
                                            if (sessions.get(i).getSubjectId() == subjectIds[j]) {
                                                LocalDate d = sessions.get(i).getDate();
                                                if (d.equals(startOfMonth) || d.equals(endOfMonth) || (d.isAfter(startOfMonth) && d.isBefore(endOfMonth)))
                                                    monthlySession.add(sessions.get(i));
                                            }
                                        }
                                }

                            return new ScheduleDataBundle(items, scheduleSession, targetStudyTimes, weeklySession, monthlySession);
                        })
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe(
                                scheduleDataBundle -> {
                                    List<ScheduleEventItem> scheduleEventList = scheduleDataBundle.getEventItemList();
                                    List<StudySessionEntity> scheduleSessionList = scheduleDataBundle
                                            .getEventSessionList();
                                    List<StudySessionEntity> weeklySessionList = scheduleDataBundle.getWeeklySessionList();
                                    List<StudySessionEntity> monthlySessionList = scheduleDataBundle.getMonthlySessionList();

                                    scheduleDataBundleMutableLiveData.setValue(scheduleDataBundle);
                                    scheduleItemMutableLiveData.setValue(scheduleEventList);
                                    scheduleSessionMutableLiveData.setValue(scheduleSessionList);
                                    scheduleWeeklySessionMutableLiveData.setValue(weeklySessionList);
                                    scheduleMonthlySessionMutableLiveData.setValue(monthlySessionList);

                                    for (StudySessionEntity session : scheduleSession) {
                                        Log.d("SessionStatisticalDialog",
                                                session.getSubjectName() + " " + session.getDate() + " " + session.getDate() + " " + session.getStartTime());
                                    }
                                    Log.d("SessionStatisticalDialog", "save complete");
                                },
                                throwable -> {
                                    scheduleItemMutableLiveData.setValue(new ArrayList<>());
                                    scheduleSessionMutableLiveData.setValue(new ArrayList<>());
                                }));
    }

    public void loadScheduleItemsByWeek(final LocalDate monday, LocalDate sunday, int[] subjectId) {
        List<StudySessionEntity> weeklySession = new ArrayList<>();
        compositeDisposable.add(sessionDao.getAllFlowable()
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(
                        sessionList -> {
                            for (int i = 0; i < sessionList.size(); i++) {
                                for (int j = 0; j < subjectId.length; j++) {
                                    if (sessionList.get(i).getSubjectId() == subjectId[j]) {
                                        if ((sessionList.get(i).getDate().isAfter(monday) && sessionList.get(i).getDate().isBefore(sunday)) || sessionList.get(i).getDate().equals(monday) || sessionList.get(i).getDate().equals(sunday))
                                            weeklySession.add(sessionList.get(i));
                                    }
                                }
                            }
                            scheduleWeeklySessionMutableLiveData.setValue(weeklySession);
                        },
                        throwable -> {
                            // 예외 처리
                        }
                )
        );
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
        selectedSessionMutableLiveData.setValue(session);
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
                                    selectedSessionMutableLiveData.setValue(session);
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
    public LiveData<StudySessionEntity> getSelectedSession() {
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

    public void deleteSessionById(int id) {
        compositeDisposable.add(
                sessionDao.deleteById(id)
                        .subscribeOn(Schedulers.io())
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe(
                                () -> {
                                    Log.d("SessionAddDialog", "Session deleted successfully");
                                },
                                throwable -> {
                                    Log.e("SessionAddDialog", "Error deleting session", throwable);
                                }
                        )
        );
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
        final Map<Integer, TargetStudyTime> targetStudyTimes;
        final List<StudySessionEntity> weeklySessionList;
        final List<StudySessionEntity> monthlySessionList;

        /**
         * ScheduleDataBundle의 생성자입니다.
         *
         * @param eventItemList    스케줄 이벤트 아이템 목록
         * @param eventSessionList 스터디 세션 엔티티 목록
         */
        ScheduleDataBundle(List<ScheduleEventItem> eventItemList, List<StudySessionEntity> eventSessionList, Map<Integer, TargetStudyTime> targetStudyTimes, List<StudySessionEntity> weeklySessionList, List<StudySessionEntity> monthlySessionList) {
            this.eventItemList = eventItemList;
            this.eventSessionList = eventSessionList;
            this.targetStudyTimes = targetStudyTimes;
            this.weeklySessionList = weeklySessionList;
            this.monthlySessionList = monthlySessionList;
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

        public Map<Integer, TargetStudyTime> getTargetStudyTimes() {return targetStudyTimes; }

        public List<StudySessionEntity> getWeeklySessionList() { return weeklySessionList; }
        public List<StudySessionEntity> getMonthlySessionList() { return monthlySessionList; }
    }
}