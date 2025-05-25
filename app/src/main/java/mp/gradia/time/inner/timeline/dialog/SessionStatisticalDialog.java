package mp.gradia.time.inner.timeline.dialog;

import android.app.Dialog;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.TextView;

import androidx.appcompat.widget.Toolbar;
import androidx.cardview.widget.CardView;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.fragment.app.DialogFragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.appbar.AppBarLayout;

import java.lang.annotation.Target;
import java.time.DayOfWeek;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import mp.gradia.R;
import mp.gradia.database.AppDatabase;
import mp.gradia.database.dao.StudySessionDao;
import mp.gradia.database.dao.SubjectDao;
import mp.gradia.database.entity.StudySessionEntity;
import mp.gradia.database.entity.TargetStudyTime;
import mp.gradia.time.inner.timeline.dialog.adapter.StudySessionAdapter;
import mp.gradia.time.inner.viewmodel.StudySessionViewModel;
import mp.gradia.time.inner.viewmodel.StudySessionViewModelFactory;
import mp.gradia.time.inner.viewmodel.SubjectViewModel;
import mp.gradia.time.inner.viewmodel.SubjectViewModelFactory;
import orion.gz.scheduleview.ScheduleEventItem;

public class SessionStatisticalDialog extends DialogFragment {
    // CONSTANT
    private final static int FOCUS_TIME_MORNING = 0;
    private final static int FOCUS_TIME_AFTERNOON = 1;
    private final static int FOCUS_TIME_EVENING = 2;
    private final static int FOCUS_TIME_NIGHT = 3;
    private final static int FOCUS_TIME_DAWN = 4;
    private final static int MAX_TIME_CLASS = 5;
    // View Component
    private Toolbar toolbar;
    private RecyclerView timeDistributionRecyclerView;
    private RecyclerView dailyTargetStudyTimeRecyclerView;
    private RecyclerView weeklyTargetStudyTimeRecyclerView;
    private RecyclerView monthlyTargetStudyTimeRecyclerView;
    private CardView dailyTargetStudyTimeContainer;
    private CardView weeklyTargetStudyTimeContainer;
    private CardView monthlyTargetStudyTimeContainer;
    private TextView totalFocusTimeTextView;
    private TextView avgFocusTimeTextView;
    private TextView mostFocusTimeTextView;
    private TextView focusLevel1CountTextView;
    private TextView focusLevel2CountTextView;
    private TextView focusLevel3CountTextView;
    private TextView focusLevel4CountTextView;

    // States Variables
    private long totalFocusTime = 0;
    private long avgFocusTime = 0;
    private int focusLevel1Count = 0;
    private int focusLevel2Count = 0;
    private int focusLevel3Count = 0;
    private int focusLevel4Count = 0;
    private long[] focusTime;
    private int mostFocusTime;
    private int[] eventColors;
    private int[] integratedColorArray;
    private int[] subjectIds;
    private long[] dailyTargetTime;
    private long[] weeklyTargetTime;
    private long[] monthlyTargetTime;
    private IntegratedDataBundle bundle;
    private IntegratedDataBundle weeklyBundle;
    private IntegratedDataBundle monthlyBundle;
    private List<StudySessionEntity> sessions;
    private List<StudySessionEntity> weeklySession;
    private List<StudySessionEntity> monthlySession;
    private List<ScheduleEventItem> eventList;
    private Map<Integer, TargetStudyTime> targetStudyTimes;
    // Database
    private AppDatabase db;
    private StudySessionDao sessionDao;
    private SubjectDao subjectDao;
    // ViewModel
    private StudySessionViewModel sessionViewModel;
    private SubjectViewModel subjectViewModel;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setStyle(DialogFragment.STYLE_NORMAL, R.style.fullscreen_dialog);

        db = AppDatabase.getInstance(requireContext());
        sessionDao = db.studySessionDao();
        subjectDao = db.subjectDao();

        StudySessionViewModelFactory sessionFactory = new StudySessionViewModelFactory(sessionDao, subjectDao);
        sessionViewModel = new ViewModelProvider(requireParentFragment(), sessionFactory).get(StudySessionViewModel.class);
        SubjectViewModelFactory subjectFactory = new SubjectViewModelFactory(subjectDao);
        subjectViewModel = new ViewModelProvider(requireParentFragment(),subjectFactory).get(SubjectViewModel.class);
        subjectViewModel.loadSubjectTargetStudyTime();
    }

    /**
     * 다이얼로그가 시작될 때 호출됩니다. 다이얼로그 창의 크기를 설정합니다.
     */
    @Override
    public void onStart() {
        super.onStart();
        Dialog dialog = getDialog();
        if (dialog != null) {
            Window window = dialog.getWindow();
            if (window != null)
                window.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
        }
    }

    /**
     * 다이얼로그의 UI를 생성하고 초기화합니다.
     */
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_session_statistical_dialog, container, false);

        // 전체 화면과 앱 뷰의 padding 설정
        AppBarLayout appBarLayout = v.findViewById(R.id.appbar_layout);
        ViewCompat.setOnApplyWindowInsetsListener(appBarLayout, (view, windowInsets) -> {
            Insets insets = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars());
            view.setPadding(view.getPaddingLeft(), insets.top, v.getPaddingRight(), v.getPaddingBottom());
            return windowInsets;
        });

        initViews(v);
        setupScheduledSession();

        return v;
    }

    /**
     * 뷰가 생성된 후 호출됩니다. 툴바, 드롭다운, DatePicker, TimePicker, DurationPicker, 메모 입력 필드, 저장 버튼을 설정합니다.
     */
    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        toolbar.setNavigationOnClickListener(v -> dismiss());
    }


    /**
     * 새로운 SessionStatisticalDialog 객체를 생성하고 Bundle 데이터를 설정합니다.
     * @param bundle 다이얼로그에 전달할 데이터가 담긴 Bundle 객체입니다.
     * @return 생성된 SessionStatisticalDialog 인스턴스입니다.
     */
    public static SessionStatisticalDialog newInstance(Bundle bundle) {
        SessionStatisticalDialog dialog = new SessionStatisticalDialog();
        dialog.setArguments(bundle);
        return dialog;
    }

    /**
     * UI 컴포넌트들을 초기화합니다.
     * @param v 뷰 계층 구조의 루트 뷰입니다.
     */
    private void initViews(View v) {
        toolbar = v.findViewById(R.id.toolbar);
        timeDistributionRecyclerView = v.findViewById(R.id.time_distribution_list);
        dailyTargetStudyTimeRecyclerView = v.findViewById(R.id.daily_target_study_time_list);
        weeklyTargetStudyTimeRecyclerView = v.findViewById(R.id.weekly_target_study_time_list);
        monthlyTargetStudyTimeRecyclerView = v.findViewById(R.id.monthly_target_study_time_list);
        dailyTargetStudyTimeContainer = v.findViewById(R.id.daily_target_study_time_container);
        weeklyTargetStudyTimeContainer = v.findViewById(R.id.weekly_target_study_time_container);
        monthlyTargetStudyTimeContainer = v.findViewById(R.id.monthly_target_study_time_container);
        totalFocusTimeTextView = v.findViewById(R.id.total_focus_time_textview);
        avgFocusTimeTextView = v.findViewById(R.id.avg_focus_time_textview);
        mostFocusTimeTextView = v.findViewById(R.id.most_focus_time_textview);
        focusLevel1CountTextView = v.findViewById(R.id.focus_level1_count_textview);
        focusLevel2CountTextView = v.findViewById(R.id.focus_level2_count_textview);
        focusLevel3CountTextView = v.findViewById(R.id.focus_level3_count_textview);
        focusLevel4CountTextView = v.findViewById(R.id.focus_level4_count_textview);
    }

    /**
     * 스케줄된 세션 데이터를 설정하고 관찰합니다.
     */
    private void setupScheduledSession() {
        sessionViewModel.getScheduleData().observe(getViewLifecycleOwner(),
                scheduleDataBundle -> {
                    if (scheduleDataBundle != null) {
                        sessions = scheduleDataBundle.getEventSessionList();
                        targetStudyTimes = scheduleDataBundle.getTargetStudyTimes();
                        eventList = scheduleDataBundle.getEventItemList();
                        weeklySession = scheduleDataBundle.getWeeklySessionList();
                        monthlySession = scheduleDataBundle.getMonthlySessionList();

                        focusTime = new long[MAX_TIME_CLASS];
                        eventColors = new int[eventList.size()];

                        if (eventList.size() == sessions.size()) {
                            for (int i = 0; i < eventList.size(); i++) {
                                totalFocusTime += sessions.get(i).getStudyTime();
                                countFocusLevel(sessions.get(i));
                                countMostFocusTime(sessions.get(i));
                                eventColors[i] = eventList.get(i).getColor();
                            }

                            setupTimeStatsData();
                            setupFocusStatsData();

                            loadBundle();

                            loadTargetStudyTime();
                            setupProgressData();
                        }
                    }
                }
        );
    }

    /**
     * 주어진 세션의 집중 레벨을 카운트합니다.
     * @param session 카운트할 StudySessionEntity 객체입니다.
     */
    private void countFocusLevel(StudySessionEntity session) {
        switch (session.getFocusLevel()) {
            case 0:
                focusLevel1Count++;
                break;
            case 1:
                focusLevel2Count++;
                break;
            case 2:
                focusLevel3Count++;
                break;
            case 3:
                focusLevel4Count++;
                break;
        }
    }

    private void loadBundle() {
        if (sessions != null)
            bundle = integrateSessions(sessions);

        if (weeklySession != null)
            weeklyBundle = integrateSessions(weeklySession);

        if (monthlySession != null)
            monthlyBundle = integrateSessions(monthlySession);
    }

    /**
     * 가장 집중한 시간대를 계산합니다.
     * @param session 계산에 사용할 StudySessionEntity 객체입니다.
     */
    private void countMostFocusTime(StudySessionEntity session) {
        LocalTime startTime = session.getStartTime();
        LocalTime endTime = session.getEndTime();
        long studyTime = session.getStudyTime();

        if (startTime.isAfter(LocalTime.of(21, 0, 0)) || startTime.equals(LocalTime.of(21, 0, 0))) {
            long durationNight = Duration.between(startTime, LocalTime.of(23, 59, 59)).toMinutes();

            if (studyTime - durationNight > 0) {
                focusTime[FOCUS_TIME_NIGHT] += durationNight;
                focusTime[FOCUS_TIME_DAWN] += studyTime - durationNight;
            } else {
                focusTime[FOCUS_TIME_NIGHT] += studyTime;
            }
        } else if (startTime.isAfter(LocalTime.of(17, 0, 0)) || startTime.equals(LocalTime.of(17, 0, 0))) {
            long durationEvening = Duration.between(startTime, LocalTime.of(21, 0, 0)).toMinutes();

            if (studyTime - durationEvening > 0) {
                focusTime[FOCUS_TIME_EVENING] += durationEvening;
                focusTime[FOCUS_TIME_NIGHT] += studyTime - durationEvening;
            } else {
                focusTime[FOCUS_TIME_EVENING] += studyTime;
            }
        } else if (startTime.isAfter(LocalTime.NOON) || startTime.equals(LocalTime.NOON)) {
            long durationAfternoon = Duration.between(startTime, LocalTime.of(17, 0, 0)).toMinutes();

            if (studyTime - durationAfternoon > 0) {
                focusTime[FOCUS_TIME_AFTERNOON] += durationAfternoon;
                focusTime[FOCUS_TIME_EVENING] += studyTime - durationAfternoon;
            } else {
                focusTime[FOCUS_TIME_AFTERNOON] += studyTime;
            }

        } else if (startTime.isAfter(LocalTime.of(5, 0, 0)) || startTime.equals(LocalTime.of(5, 0, 0))) {
            long durationMorning = Duration.between(startTime, LocalTime.NOON).toMinutes();

            if (studyTime - durationMorning > 0) {
                focusTime[FOCUS_TIME_MORNING] += durationMorning;
                focusTime[FOCUS_TIME_AFTERNOON] += studyTime - durationMorning;
            } else {
                focusTime[FOCUS_TIME_MORNING] += studyTime;
            }
        } else if (startTime.isAfter(LocalTime.MIDNIGHT) || startTime.equals(LocalTime.MIDNIGHT)) {
            long durationDawn = Duration.between(startTime, LocalTime.of(5, 0, 0)).toMinutes();

            if (studyTime - durationDawn > 0) {
                focusTime[FOCUS_TIME_DAWN] += durationDawn;
                focusTime[FOCUS_TIME_MORNING] += studyTime - durationDawn;
            } else {
                focusTime[FOCUS_TIME_DAWN] += studyTime;
            }
        }

        long max = focusTime[0];
        for (int i = 1; i < MAX_TIME_CLASS; i++)
            if (max < focusTime[i]) max = focusTime[i];

        if (max == focusTime[FOCUS_TIME_MORNING])
            mostFocusTime = FOCUS_TIME_MORNING;
        else if (max == focusTime[FOCUS_TIME_AFTERNOON])
            mostFocusTime = FOCUS_TIME_AFTERNOON;
        else if (max == focusTime[FOCUS_TIME_EVENING])
            mostFocusTime = FOCUS_TIME_EVENING;
        else if (max == focusTime[FOCUS_TIME_NIGHT])
            mostFocusTime = FOCUS_TIME_NIGHT;
        else if (max == focusTime[FOCUS_TIME_DAWN])
            mostFocusTime = FOCUS_TIME_DAWN;
    }

    /**
     * 총 집중 시간 및 가장 집중한 시간대를 UI에 표시합니다.
     */
    private void setupTimeStatsData() {
        long hours = totalFocusTime / 60;
        long minutes = totalFocusTime % 60;
        String totalFocusTime = "";

        if (hours > 0) totalFocusTime += hours + "시간";
        if (minutes > 0) totalFocusTime += minutes + "분";

        totalFocusTimeTextView.setText(totalFocusTime);

        switch (mostFocusTime) {
            case FOCUS_TIME_MORNING:
                mostFocusTimeTextView.setText("아침");
                break;
            case FOCUS_TIME_AFTERNOON:
                mostFocusTimeTextView.setText("오후");
                break;
            case FOCUS_TIME_EVENING:
                mostFocusTimeTextView.setText("저녁");
                break;
            case FOCUS_TIME_NIGHT:
                mostFocusTimeTextView.setText("밤");
                break;
            case FOCUS_TIME_DAWN:
                mostFocusTimeTextView.setText("새벽");
                break;
        }
    }

    /**
     * 집중 레벨별 세션 수를 UI에 표시합니다.
     */
    private void setupFocusStatsData() {
        focusLevel1CountTextView.setText(String.valueOf(focusLevel1Count));
        focusLevel2CountTextView.setText(String.valueOf(focusLevel2Count));
        focusLevel3CountTextView.setText(String.valueOf(focusLevel3Count));
        focusLevel4CountTextView.setText(String.valueOf(focusLevel4Count));
    }

    /**
     * 시간 분포 및 목표 학습 시간 RecyclerView를 설정하고 어댑터를 바인딩합니다.
     */
    private void setupProgressData() {
        // time distribution data (세션 시간 분포)
        timeDistributionRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        StudySessionAdapter timeDistribution = new StudySessionAdapter(requireContext(), bundle.getIntegratedList(), bundle.getIntegratedColorArray(), totalFocusTime);
        timeDistributionRecyclerView.setAdapter(timeDistribution);

        for (int i = 0; i < dailyTargetTime.length; i++) {
            if (dailyTargetTime[i] == 0) dailyTargetStudyTimeContainer.setVisibility(View.GONE);
            if (weeklyTargetTime[i] == 0) weeklyTargetStudyTimeContainer.setVisibility(View.GONE);
            if (monthlyTargetTime[i] == 0) monthlyTargetStudyTimeContainer.setVisibility(View.GONE);
        }

        // daily target study time data
        if (dailyTargetStudyTimeContainer.getVisibility() == View.VISIBLE) {
            dailyTargetStudyTimeRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
            StudySessionAdapter dailyTarget = new StudySessionAdapter(requireContext(), bundle.getIntegratedList(), bundle.getIntegratedColorArray(), dailyTargetTime);
            dailyTargetStudyTimeRecyclerView.setAdapter(dailyTarget);
        }

        // weekly target study time data
        if (weeklyTargetStudyTimeContainer.getVisibility() == View.VISIBLE) {
            weeklyTargetStudyTimeRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
            StudySessionAdapter weeklyTarget = new StudySessionAdapter(requireContext(), weeklyBundle.getIntegratedList(), weeklyBundle.getIntegratedColorArray(), weeklyTargetTime);
            weeklyTargetStudyTimeRecyclerView.setAdapter(weeklyTarget);
        }

        // monthly target study time data
        if (monthlyTargetStudyTimeContainer.getVisibility() == View.VISIBLE) {
            monthlyTargetStudyTimeRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
            StudySessionAdapter monthlyTarget = new StudySessionAdapter(requireContext(), monthlyBundle.getIntegratedList(), monthlyBundle.getIntegratedColorArray(), monthlyTargetTime);
            monthlyTargetStudyTimeRecyclerView.setAdapter(monthlyTarget);
        }
    }

    /**
     * 중복되는 세션을 통합하여 StudySessionEntity 목록과 해당 색상 배열을 반환합니다.
     * @return 통합된 세션 데이터와 색상 배열을 포함하는 IntegratedDataBundle 객체입니다.
     */
    private IntegratedDataBundle integrateSessions(List<StudySessionEntity> sessionList) {
        Map<Integer, StudySessionEntity> integratedMap = new LinkedHashMap<>();
        Map<Integer, Integer> integratedColorMap = new LinkedHashMap<>();

        for (int i = 0; i < sessionList.size(); i++) {
            if (integratedMap.containsKey(sessionList.get(i).getSubjectId())) {
                StudySessionEntity existingSession = integratedMap.get(sessionList.get(i).getSubjectId());
                if (existingSession != null)
                    existingSession.setStudyTime(sessionList.get(i).getStudyTime() + existingSession.getStudyTime());
            } else {
                integratedMap.put(sessionList.get(i).getSubjectId(), new StudySessionEntity(
                        sessionList.get(i).getSubjectId(),
                        sessionList.get(i).getSubjectName(),
                        sessionList.get(i).getDate(),
                        sessionList.get(i).getEndDate(),
                        sessionList.get(i).getStudyTime(),
                        sessionList.get(i).getStartTime(),
                        sessionList.get(i).getEndTime(),
                        0,
                        0,
                        ""));
                if (integratedColorArray == null) {
                    integratedColorMap.put(sessionList.get(i).getSubjectId(), eventColors[i]);
                }
            }
        }

        List<StudySessionEntity> integratedList = new ArrayList<>(integratedMap.values());
        if (subjectIds == null)
            subjectIds = integratedMap.keySet().stream().mapToInt(Integer::intValue).toArray();

        if (integratedColorArray == null)
            integratedColorArray = integratedColorMap.values().stream().mapToInt(Integer::intValue).toArray();

        return new IntegratedDataBundle(integratedList, integratedColorArray);
    }

    private void loadTargetStudyTime() {

        dailyTargetTime = Arrays.stream(subjectIds)
                .mapToLong(id -> {
                    TargetStudyTime t = targetStudyTimes.get(id);
                    if (t != null) {
                        return t.getDailyTargetStudyTime() * 60;
                    } else {
                        return 0;
                    }
                })
                .toArray();
        weeklyTargetTime = Arrays.stream(subjectIds)
                .mapToLong(id -> {
                    TargetStudyTime t = targetStudyTimes.get(id);
                    if (t != null) {
                        return t.getWeeklyTargetStudyTime() * 60;
                    } else {
                        return 0;
                    }
                })
                .toArray();
        monthlyTargetTime = Arrays.stream(subjectIds)
                .mapToLong(id -> {
                    TargetStudyTime t = targetStudyTimes.get(id);
                    if (t != null) {
                        return t.getMonthlyTargetStudyTime() * 60;
                    } else {
                        return 0;
                    }
                })
                .toArray();
    }

    /**
     * 통합된 세션 목록과 색상 배열을 저장하는 내부 클래스입니다.
     */
    static class IntegratedDataBundle {
        List<StudySessionEntity> integratedList;
        int[] integratedColorArray;

        /**
         * IntegratedDataBundle의 생성자입니다.
         * @param integratedList 통합된 StudySessionEntity 목록입니다.
         * @param integratedColorArray 통합된 세션의 색상 배열입니다.
         */
        public IntegratedDataBundle(List<StudySessionEntity> integratedList, int[] integratedColorArray) {
            this.integratedList = integratedList;
            this.integratedColorArray = integratedColorArray;
        }

        /**
         * 통합된 세션 목록을 반환합니다.
         * @return 통합된 StudySessionEntity 목록입니다.
         */
        public List<StudySessionEntity> getIntegratedList() {
            return integratedList;
        }

        /**
         * 통합된 세션의 색상 배열을 반환합니다.
         * @return 통합된 세션의 색상 배열입니다.
         */
        public int[] getIntegratedColorArray() {
            return integratedColorArray;
        }
    }
}