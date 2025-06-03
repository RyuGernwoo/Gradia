package mp.gradia.time.inner;

import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.cardview.widget.CardView;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;

import com.google.android.material.datepicker.MaterialDatePicker;

import java.time.DayOfWeek;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.YearMonth;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.disposables.CompositeDisposable;
import io.reactivex.rxjava3.schedulers.Schedulers;
import mp.gradia.R;
import mp.gradia.database.AppDatabase;
import mp.gradia.database.dao.StudySessionDao;
import mp.gradia.database.dao.SubjectDao;
import mp.gradia.database.entity.StudySessionEntity;
import mp.gradia.time.inner.record.dialog.SessionAddDialog;
import mp.gradia.time.inner.timeline.dialog.SessionStatisticalDialog;
import mp.gradia.time.inner.viewmodel.StudySessionViewModel;
import mp.gradia.time.inner.viewmodel.StudySessionViewModelFactory;
import mp.gradia.time.inner.viewmodel.SubjectViewModel;
import mp.gradia.time.inner.viewmodel.SubjectViewModelFactory;
import orion.gz.scheduleview.OnScheduleTouchListener;
import orion.gz.scheduleview.ScheduleEventItem;
import orion.gz.scheduleview.ScheduleView;

public class TimeTimelineFragment extends Fragment implements SessionAddDialog.SessionDeleteListener {
    private static final String TAG = "TimeTimelineFragment";
    private static final int DAYS_OF_WEEK = 7;
    private ScheduleView scheduleView;
    private ScrollView scheduleScrollView;
    private ImageButton navigateBeforeBtn;
    private ImageButton navigateNextBtn;
    private View calendarItemContainer;
    private CardView sessionStatisticalDataContainer;
    private TextView monthTextView;
    private TextView[] weeklyCalendarDOMTextView;
    private TextView[] weeklyCalendarDayTextView;
    private LinearLayout[] weeklyCalendarItemContainer;
    private LocalDate monday;
    private LocalDate today;
    private LocalDate currentSelectedDate;
    private boolean isScheduleEmpty = false;
    private int currentSelectedIdx = -1;
    private List<ScheduleEventItem> scheduleEventItems;
    private AppDatabase db;
    private StudySessionDao sessionDao;
    private SubjectDao subjectDao;
    private StudySessionViewModel studySessionViewModel;
    private SubjectViewModel subjectViewModel;
    private CompositeDisposable compositeDisposable = new CompositeDisposable();

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        db = AppDatabase.getInstance(requireContext());
        sessionDao = db.studySessionDao();
        subjectDao = db.subjectDao();

        StudySessionViewModelFactory sessionFactory = new StudySessionViewModelFactory(
                requireActivity().getApplication(), sessionDao, subjectDao);
        studySessionViewModel = new ViewModelProvider(this, sessionFactory).get(StudySessionViewModel.class);
        SubjectViewModelFactory subjectFactory = new SubjectViewModelFactory(subjectDao);
        subjectViewModel = new ViewModelProvider(this, subjectFactory).get(SubjectViewModel.class);
        scheduleEventItems = new ArrayList<>();

        // Session Edit 결과를 가져옴
        getParentFragmentManager().setFragmentResultListener(SessionAddDialog.REQUEST_KEY, this,
                (key, bundle) -> {
                    int sessionMode = bundle.getInt(SessionAddDialog.KEY_SESSION_MODE);
                    if (sessionMode == SessionAddDialog.MODE_EDIT) {
                        updateSchedule();
                    }
                });
    }

    /**
     * 프래그먼트의 UI를 생성하고 초기화합니다. 타임라인 레이아웃을 인플레이트합니다.
     */
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_time_timeline, container, false);

        initViews(v);
        setupWeeklyCalendar();
        setupWeeklyCalendarControl();
        setupDatePicker();
        setupShowStatisticalData();
        setupScheduleView();
        updateSchedule();

        return v;
    }

    /**
     * 프래그먼트가 사용자에게 보일 때 호출됩니다. 스케줄을 업데이트합니다.
     */
    @Override
    public void onResume() {
        super.onResume();
        updateSchedule();
    }

    /**
     * 현재 선택된 날짜를 반환합니다.
     *
     * @return 현재 선택된 LocalDate 객체입니다.
     */
    public LocalDate getCurrentSelectedDate() {
        return currentSelectedDate;
    }

    /**
     * UI 컴포넌트들을 초기화합니다.
     *
     * @param v 뷰 계층 구조의 루트 뷰입니다.
     */
    private void initViews(View v) {
        scheduleView = v.findViewById(R.id.schedule_view);
        scheduleScrollView = v.findViewById(R.id.schedule_scroll_view);

        navigateBeforeBtn = v.findViewById(R.id.navigate_before_btn);
        navigateNextBtn = v.findViewById(R.id.navigate_next_btn);
        calendarItemContainer = v.findViewById(R.id.calendar_item_container);
        sessionStatisticalDataContainer = v.findViewById(R.id.session_statistical_data_container);
        monthTextView = v.findViewById(R.id.month_textview);
        weeklyCalendarDOMTextView = new TextView[DAYS_OF_WEEK];
        weeklyCalendarDayTextView = new TextView[DAYS_OF_WEEK];
        weeklyCalendarItemContainer = new LinearLayout[DAYS_OF_WEEK];

        for (int i = 0; i < DAYS_OF_WEEK; i++) {
            weeklyCalendarDOMTextView[i] = v.findViewById(
                    getResources().getIdentifier("weekly_calendar_DOM" + (i + 1), "id", getContext().getPackageName()));
            weeklyCalendarDayTextView[i] = v.findViewById(
                    getResources().getIdentifier("weekly_calendar_day" + (i + 1), "id", getContext().getPackageName()));
            weeklyCalendarItemContainer[i] = v.findViewById(getResources()
                    .getIdentifier("weekly_calendar_item_container" + (i + 1), "id", getContext().getPackageName()));
        }
    }

    /**
     * 주간 캘린더를 설정하고 현재 날짜에 따라 UI를 업데이트합니다.
     */
    private void setupWeeklyCalendar() {
        if (monday == null && today == null) {
            today = LocalDate.now();
            monday = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
            currentSelectedDate = today;
        }
        LocalDate sunday = monday.with(TemporalAdjusters.nextOrSame(DayOfWeek.SUNDAY));
        DateTimeFormatter dayFormatter = DateTimeFormatter.ofPattern("d");
        YearMonth targetYearMonth;

        if (monday.getMonth().equals(sunday.getMonth()))
            targetYearMonth = YearMonth.from(monday);
        else {
            int daysInMonthOfMonday = 0;
            for (int i = 0; i < DAYS_OF_WEEK; i++) {
                if (YearMonth.from(monday.plusDays(i)).equals(YearMonth.from(monday))) {
                    daysInMonthOfMonday++;
                }
            }

            if (daysInMonthOfMonday >= (DAYS_OF_WEEK / 2) + 1)
                targetYearMonth = YearMonth.from(monday);
            else
                targetYearMonth = YearMonth.from(sunday);
        }
        DateTimeFormatter monthYearFormatter = DateTimeFormatter.ofPattern("yyyy년 M월");
        String formattedYearNMonth = targetYearMonth.format(monthYearFormatter);
        monthTextView.setText(formattedYearNMonth);

        for (int i = 0; i < DAYS_OF_WEEK; i++) {
            LocalDate currentDateInWeek = monday.plusDays(i);
            String formattedDay = currentDateInWeek.format(dayFormatter);

            TextView dayNumberView = weeklyCalendarDayTextView[i];
            TextView dayNameView = weeklyCalendarDOMTextView[i];

            if (dayNumberView != null) {
                dayNumberView.setText(formattedDay);
                if (YearMonth.from(currentDateInWeek).equals(targetYearMonth)) {
                    dayNumberView.setTextColor(Color.BLACK);
                    dayNameView.setTextColor(Color.BLACK);
                } else {
                    dayNumberView.setTextColor(Color.GRAY);
                    dayNameView.setTextColor(Color.GRAY);
                }

                if (currentSelectedDate.equals(today) && currentDateInWeek.equals(today)) {
                    currentSelectedIdx = i;
                    currentSelectedDate = today;
                    updateCalendarCellSelection(currentSelectedIdx);
                }
            }
        }
    }

    /**
     * 주간 캘린더의 이전/다음 주 버튼과 각 날짜 아이템의 클릭 리스너를 설정합니다.
     */
    private void setupWeeklyCalendarControl() {
        navigateBeforeBtn.setOnClickListener(v -> {
            monday = monday.minusWeeks(1);
            setupWeeklyCalendar();
            updateCalendarCellSelection(currentSelectedIdx);
        });

        navigateNextBtn.setOnClickListener(v -> {
            monday = monday.plusWeeks(1);
            setupWeeklyCalendar();
            updateCalendarCellSelection(currentSelectedIdx);
        });

        for (int i = 0; i < DAYS_OF_WEEK; i++) {
            final int currentIdx = i;
            weeklyCalendarItemContainer[i].setOnClickListener(v -> {
                updateCalendarCellSelection(currentIdx);
            });
        }
    }

    /**
     * 캘린더 셀의 선택 상태를 업데이트합니다. 선택된 셀은 강조 표시됩니다.
     *
     * @param idx 선택된 셀의 인덱스입니다.
     */
    private void updateCalendarCellSelection(int idx) {
        if (currentSelectedIdx != -1) {
            weeklyCalendarItemContainer[currentSelectedIdx].setBackgroundResource(0);
            weeklyCalendarDayTextView[currentSelectedIdx].setTextColor(Color.BLACK);
            weeklyCalendarDOMTextView[currentSelectedIdx].setTextColor(Color.BLACK);
        }

        weeklyCalendarItemContainer[idx].setBackgroundResource(R.drawable.calendar_today_circle);
        weeklyCalendarDayTextView[idx].setTextColor(Color.WHITE);
        weeklyCalendarDOMTextView[idx].setTextColor(Color.WHITE);
        currentSelectedIdx = idx;
        currentSelectedDate = monday.plusDays(idx);
        updateSchedule();
    }

    /**
     * 월 텍스트뷰 클릭 시 DatePicker를 표시하도록 설정합니다.
     */
    private void setupDatePicker() {
        monthTextView.setOnClickListener(v -> {
            MaterialDatePicker.Builder<Long> builder = MaterialDatePicker.Builder.datePicker();
            builder.setTitleText("날짜 선택");
            builder.setSelection(getUtcMillis(currentSelectedDate));

            final MaterialDatePicker<Long> datePicker = builder.build();
            datePicker.addOnPositiveButtonClickListener(selection -> {
                currentSelectedDate = getDate(selection);
                monday = currentSelectedDate.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
                setupWeeklyCalendar();
                updateCalendarCellSelection(currentSelectedDate.getDayOfWeek().getValue() - 1);
                updateSchedule();
            });
            datePicker.show(getParentFragmentManager(), datePicker.toString());
        });
    }

    /**
     * 통계 데이터 컨테이너 클릭 시 SessionStatisticalDialog를 표시하도록 설정합니다.
     */
    private void setupShowStatisticalData() {
        sessionStatisticalDataContainer.setOnClickListener(v -> {
            if (isScheduleEmpty) {
                Toast.makeText(requireContext(), "현재 추가된 일정이 없습니다.", Toast.LENGTH_SHORT).show();
                return;
            }

            SessionStatisticalDialog dialog = new SessionStatisticalDialog();
            dialog.show(getChildFragmentManager(), "SessionStatisticalDialogFragment");
        });
    }

    /**
     * ScheduleView를 설정하고 스케줄 아이템 변경을 관찰합니다.
     */
    private void setupScheduleView() {
        // SchdeuleView 초기화
        if (currentSelectedDate != null)
            studySessionViewModel.loadScheduleItemsByDate(currentSelectedDate);

        // ScheduleItem LiveData 관찰
        studySessionViewModel.scheduleItemLiveData.observe(getViewLifecycleOwner(),
                eventItems -> {
                    if (scheduleView != null && eventItems != null) {
                        isScheduleEmpty = eventItems.isEmpty();
                        scheduleView.setScheduleEventItems(eventItems);
                        scrollToTop(eventItems);
                    } else if (scheduleView != null) {
                        isScheduleEmpty = true;
                        scheduleView.clearScheduleEventItems();
                    }
                });

        studySessionViewModel.selectedSessionLiveData.observe(getViewLifecycleOwner(),
                session -> {
                    if (session != null) {
                        Bundle bundle = new Bundle();
//                        Log.d(TAG, "세션 ID: " + sessionId);
                        bundle.putInt(SessionAddDialog.KEY_SESSION_ID, session.getSessionId());
                        bundle.putString(SessionAddDialog.KEY_SERVER_SESSION_ID, session.getServerId());
                        bundle.putInt(SessionAddDialog.KEY_SESSION_MODE, SessionAddDialog.MODE_EDIT);
                        bundle.putInt(SessionAddDialog.KEY_SESSION_FOCUS_LEVEL, session.getFocusLevel());
                        bundle.putInt(SessionAddDialog.KEY_SUBJECT_ID, session.getSubjectId());
                        bundle.putString(SessionAddDialog.KEY_SERVER_SUBJECT_ID,
                                session.getServerSubjectId());
                        bundle.putString(SessionAddDialog.KEY_SUBJECT_NAME, session.getSubjectName());
                        bundle.putInt(SessionAddDialog.KEY_START_HOUR, session.getStartTime().getHour());
                        bundle.putInt(SessionAddDialog.KEY_START_MINUTE,
                                session.getStartTime().getMinute());
                        bundle.putInt(SessionAddDialog.KEY_END_HOUR, session.getEndTime().getHour());
                        bundle.putInt(SessionAddDialog.KEY_END_MINUTE, session.getEndTime().getMinute());
                        bundle.putLong(SessionAddDialog.KEY_START_DATE, getUtcMillis(session.getDate()));
                        bundle.putLong(SessionAddDialog.KEY_END_DATE, getUtcMillis(session.getEndDate()));
                        bundle.putString(SessionAddDialog.KEY_SESSION_MEMO, session.getMemo());

                        if (getParentFragmentManager().findFragmentByTag("SessionAddDialog") == null) {
                            SessionAddDialog dialog = SessionAddDialog.newInstance(bundle);
                            dialog.setSessionDeleteListener(this);
                            dialog.show(getParentFragmentManager(), "SessionAddDialog");
                        }
                    }
                });// Listener
        scheduleView.setOnScheduleTouchListener(new OnScheduleTouchListener() {
            @Override
            public void onEventClick(ScheduleEventItem scheduleEventItem) {
                int sessionId = scheduleEventItem.getId();
                studySessionViewModel.selectSessionById(sessionId);
            }

            // 빈 공간 클릭으로 추가 이벤트 구현할 수 있음
            @Override
            public void onEmptySlotClick(LocalTime localTime) {

            }
        });
    }

    @Override
    public void onSessionDeleted(int sessionId) {
        Log.d("SessionAdd", "콜백 호출됨");
        // weeklyCalendarItemContainer[currentSelectedIdx].performClick();
        updateSchedule();
    }

    /**
     * 현재 선택된 날짜에 따라 스케줄을 업데이트합니다.
     */
    private void updateSchedule() {
        if (currentSelectedDate == null) {
            scheduleView.clearScheduleEventItems();
            return;
        }
        studySessionViewModel.loadScheduleItemsByDate(currentSelectedDate);
        scheduleView.invalidate();
    }

    /**
     * 스케줄 뷰를 맨 위로 스크롤합니다. 만약 이벤트가 있다면 가장 빠른 이벤트 시간으로 스크롤합니다.
     *
     * @param items 스케줄 이벤트 아이템 목록입니다.
     */
    private void scrollToTop(List<ScheduleEventItem> items) {
        if (items.isEmpty()) {
            scheduleScrollView.post(() -> scheduleScrollView.smoothScrollTo(0, 0));
            return;
        }
        ScheduleEventItem firstEvent = items.get(0);
        for (int i = 1; i < items.size(); i++) {
            if (items.get(i).getStartTime().isBefore(firstEvent.getStartTime()))
                firstEvent = items.get(i);
        }

        final LocalTime firstEventTime = firstEvent.getStartTime();
        scheduleView.post(() -> {
            float y = scheduleView.getEventTopY(firstEventTime);
            int scrollToY = Math.max(0, (int) y);
            scheduleScrollView.smoothScrollTo(0, scrollToY);
        });
    }

    /**
     * LocalDate 객체를 UTC 밀리초로 변환합니다.
     *
     * @param date 변환할 LocalDate 객체입니다.
     * @return UTC 밀리초 값입니다.
     */
    private long getUtcMillis(LocalDate date) {
        return date.atStartOfDay(ZoneId.of("UTC")).toInstant().toEpochMilli();
    }

    /**
     * UTC 밀리초를 LocalDate 객체로 변환합니다.
     *
     * @param utcMillis 변환할 UTC 밀리초 값입니다.
     * @return 변환된 LocalDate 객체입니다.
     */
    private LocalDate getDate(long utcMillis) {
        Instant instant = Instant.ofEpochMilli(utcMillis);
        ZoneId utcZone = ZoneId.of("UTC");
        return instant.atZone(utcZone).toLocalDate();
    }


    /**
     * 프래그먼트가 소멸될 때 호출됩니다.
     */
    @Override
    public void onDestroy() {
        super.onDestroy();
        compositeDisposable.clear();
    }
}