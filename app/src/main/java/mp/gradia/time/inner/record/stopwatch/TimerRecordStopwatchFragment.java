package mp.gradia.time.inner.record.stopwatch;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.graphics.ColorUtils;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;

import mp.gradia.R;
import mp.gradia.database.AppDatabase;
import mp.gradia.database.dao.SubjectDao;
import mp.gradia.time.inner.record.timer.TimerService;
import mp.gradia.time.inner.viewmodel.SubjectViewModel;
import mp.gradia.time.inner.viewmodel.SubjectViewModelFactory;
import mp.gradia.time.inner.record.dialog.SessionAddDialog;

public class TimerRecordStopwatchFragment extends Fragment {
    // Session State Constants
    private static final int SESSION_START = 0;
    private static final int SESSION_END = 1;
    private static final int SESSION_PAUSE = 2;
    private static final int SESSION_RESUME = 3;

    // UI Components
    private TextView stopwatchTextview;
    private Button sessionStartBtn;
    private LinearLayout container;
    private ConstraintLayout sessionControlLayout;
    private FloatingActionButton sessionControlFab;
    private FloatingActionButton sessionEndFab;

    // State Variables
    private long currentHours = 0;
    private long currentMinutes = 0;
    private long currentSeconds = 0;
    private int sessionState = -1;
    private boolean isStopwatchPause = false;
    private long sessionDuration = 0L;
    private LocalTime startTime;
    private LocalTime endTime;

    // Database and ViewModel
    private AppDatabase db;
    private SubjectDao dao;
    private SubjectViewModel selectedSubjectViewModel;

    // Broadcast Receiver for Stopwatch Update
    private final BroadcastReceiver stopwatchUpdateReceiver = new BroadcastReceiver() {
        /**
         * 스톱워치 업데이트 브로드캐스트를 수신할 때 호출됩니다.
         * @param context Broadcast가 수신되는 Context입니다.
         * @param intent 수신된 Intent 객체입니다.
         */
        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = StopwatchService.BROADCAST_ACTION_STOPWATCH_UPDATE;

            if (intent != null && action.equals(intent.getAction())) {
                if (sessionState == -1)
                    restoreStopwatchState();
                else {
                    long elapsedTime = intent.getLongExtra(StopwatchService.BROADCAST_EXTRA_ELAPSED_TIME, 0L) / 1000L;
                    setStopwatchTime(elapsedTime);
                }
            }
        }
    };

    // Broadcast Receiver for Stopwatch Stop
    private final BroadcastReceiver stopwatchStopReceiver = new BroadcastReceiver() {
        /**
         * 스톱워치 중지 브로드캐스트를 수신할 때 호출됩니다.
         * @param context Broadcast가 수신되는 Context입니다.
         * @param intent 수신된 Intent 객체입니다.
         */
        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = StopwatchService.BROADCAST_ACTION_STOPWATCH_STOP;
            if (intent != null && action.equals(intent.getAction())) {
                sessionDuration = intent.getLongExtra(StopwatchService.BROADCAST_EXTRA_ACCUMULATED_TIME, 0L) / 1000L;
                endTime = LocalTime.now();
                addSession();
            }
        }
    };

    /**
     * SharedPreferences에서 스톱워치 상태를 복원합니다.
     */
    private void restoreStopwatchState() {
        SharedPreferences prefs = requireContext().getSharedPreferences(StopwatchService.PREFS_NAME, Context.MODE_PRIVATE);
        long elapsedTime = prefs.getLong(StopwatchService.KEY_ELAPSED_TIME, 0);
        isStopwatchPause = prefs.getBoolean(StopwatchService.KEY_IS_PAUSED, false);
        startTime = prefs.getString(StopwatchService.KEY_START_TIME, null) == null ? null : LocalTime.parse(prefs.getString(TimerService.KEY_START_TIME, null));

        setStopwatchTime(elapsedTime);
        sessionState = SESSION_START;
        viewControl(sessionState);
    }

    /**
     * 프래그먼트가 처음 생성될 때 호출됩니다. 데이터베이스와 ViewModel을 초기화합니다.
     */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        db = AppDatabase.getInstance(requireContext());
        dao = db.subjectDao();
        SubjectViewModelFactory factory = new SubjectViewModelFactory(dao);
        selectedSubjectViewModel = new ViewModelProvider(requireParentFragment(), factory).get(SubjectViewModel.class);
    }

    /**
     * 프래그먼트의 UI를 생성하고 초기화합니다.
     */
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_time_record_stopwatch, container, false);

        initViews(v);
        setupStopwatch();
        setupStopwatchControl();

        if (getArguments() != null) {
            isStopwatchPause = getArguments().getBoolean(StopwatchService.KEY_IS_PAUSED);
            if (isStopwatchPause) {
                restoreStopwatchState();

                sessionState = SESSION_PAUSE;
                viewControl(sessionState);
            }
        }
        return v;
    }

    /**
     * 프래그먼트가 사용자에게 보일 때 호출됩니다. BroadcastReceiver를 등록합니다.
     */
    @Override
    public void onResume() {
        super.onResume();
        Context context = requireContext();
        String update = StopwatchService.BROADCAST_ACTION_STOPWATCH_UPDATE;
        String stop = StopwatchService.BROADCAST_ACTION_STOPWATCH_STOP;

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            LocalBroadcastManager.getInstance(context).registerReceiver(stopwatchUpdateReceiver, new IntentFilter(update));
            LocalBroadcastManager.getInstance(context).registerReceiver(stopwatchStopReceiver, new IntentFilter(stop));
        }
    }

    /**
     * 프래그먼트가 일시 중지될 때 호출됩니다. BroadcastReceiver를 등록 해제합니다.
     */
    @Override
    public void onPause() {
        super.onPause();
        Context context = requireContext();
        LocalBroadcastManager.getInstance(context).unregisterReceiver(stopwatchUpdateReceiver);
        LocalBroadcastManager.getInstance(context).unregisterReceiver(stopwatchStopReceiver);
    }

    /**
     * 프래그먼트의 뷰가 소멸될 때 호출됩니다. 현재 스톱워치 시간을 SharedPreferences에 저장합니다.
     */
    @Override
    public void onDestroyView() {
        super.onDestroyView();
        Context context = requireContext();
        SharedPreferences prefs = context.getSharedPreferences(StopwatchService.PREFS_NAME, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putLong(StopwatchService.KEY_ELAPSED_TIME, currentHours * 60 * 60 + currentMinutes * 60 + currentSeconds);
        editor.apply();
    }

    /**
     * UI 컴포넌트들을 초기화합니다.
     * @param v 뷰 계층 구조의 루트 뷰입니다.
     */
    private void initViews(View v) {
        stopwatchTextview = v.findViewById(R.id.stopwatch_textview);
        container = v.findViewById(R.id.container);
        sessionStartBtn = v.findViewById(R.id.session_start_btn);
        sessionControlLayout = v.findViewById(R.id.stopwatch_session_control_layout);
        sessionControlFab = v.findViewById(R.id.session_control_fab);
        sessionEndFab = v.findViewById(R.id.session_end_fab);
    }

    /**
     * 스톱워치 세션 상태에 따라 UI를 업데이트합니다.
     * @param state 업데이트할 세션 상태입니다 (SESSION_START, SESSION_END, SESSION_PAUSE, SESSION_RESUME).
     */
    private void viewControl(int state) {
        switch (state) {
            case SESSION_START:
                sessionState = SESSION_START;
                sessionStartBtn.setVisibility(View.GONE);
                sessionControlLayout.setVisibility(View.VISIBLE);

                try {
                    Fragment fragment = requireParentFragment();
                    View parentView = fragment.getView();

                    if (parentView != null) {
                        parentView.findViewById(R.id.add_session_fab).setVisibility(View.GONE);
                        parentView.findViewById(R.id.subject_select_btn).setClickable(false);
                    }
                } catch (IllegalStateException e) {
                    Log.e("TimeRecordStopwatchFragment", "TimeRecordFragment not found");
                }
                break;
            case SESSION_END:
                sessionState = SESSION_END;
                sessionControlFab.setImageResource(R.drawable.outline_pause_black_24);
                sessionControlLayout.setVisibility(View.GONE);
                sessionStartBtn.setVisibility(View.VISIBLE);
                stopwatchTextview.setText("00:00:00");
                container.setAlpha(1F);

                try {
                    Fragment fragment = requireParentFragment();
                    View parentView = fragment.getView();

                    if (parentView != null) {
                        parentView.findViewById(R.id.add_session_fab).setVisibility(View.VISIBLE);
                        parentView.findViewById(R.id.subject_select_btn).setClickable(true);
                    }
                } catch (IllegalStateException e) {
                    Log.e("TimeRecordStopwatchFragment", "TimeRecordFragment not found");
                }
                break;
            case SESSION_PAUSE:
                sessionState = SESSION_PAUSE;
                container.setAlpha(0.5F);
                sessionControlFab.setImageResource(R.drawable.outline_play_arrow_black_24);
                break;
            case SESSION_RESUME:
                sessionState = SESSION_RESUME;
                container.setAlpha(1F);
                sessionControlFab.setImageResource(R.drawable.outline_pause_black_24);
                break;
        }
    }

    /**
     * 선택된 과목 정보에 따라 스톱워치 UI의 색상을 설정합니다.
     */
    private void setupStopwatch() {
        selectedSubjectViewModel.selectedSubjectLiveData.observe(getViewLifecycleOwner(),
                subject -> {
                    int color = Color.parseColor(subject.getColor());
                    int brightColor = ColorUtils.blendARGB(color, Color.WHITE, 0.7F);

                    sessionStartBtn.setBackgroundColor(color);
                    GradientDrawable drawable = (GradientDrawable) getResources().getDrawable(R.drawable.circle);
                    drawable.setColor(brightColor);
                    container.setBackground(drawable);
                });
    }

    /**
     * 스톱워치 시작, 중지, 재개 버튼의 클릭 리스너를 설정합니다.
     */
    private void setupStopwatchControl() {
        Context context = requireContext();
        sessionStartBtn.setOnClickListener(v -> {
            startTime = LocalTime.now();

            Bundle bundle = new Bundle();
            bundle.putString(StopwatchService.BUNDLE_SUBJECT_NAME, selectedSubjectViewModel.selectedSubjectLiveData.getValue().getName());
            bundle.putInt(StopwatchService.BUNDLE_SUBJECT_ID, selectedSubjectViewModel.selectedSubjectLiveData.getValue().getSubjectId());
            bundle.putString(StopwatchService.BUNDLE_START_TIME, startTime.toString());

            Intent startStopwatchIntent = new Intent(context, StopwatchService.class);
            startStopwatchIntent.setAction(StopwatchService.ACTION_START);
            startStopwatchIntent.putExtras(bundle);
            context.startService(startStopwatchIntent);

            viewControl(SESSION_START);
        });

        sessionControlFab.setOnClickListener(v -> {
            if (isStopwatchPause) {
                Intent resumeStopwatchIntent = new Intent(context, StopwatchService.class);
                resumeStopwatchIntent.setAction(StopwatchService.ACTION_RESUME);
                context.startService(resumeStopwatchIntent);

                viewControl(SESSION_RESUME);
                isStopwatchPause = false;
            } else {
                Intent pauseStopwatchIntent = new Intent(context, StopwatchService.class);
                pauseStopwatchIntent.setAction(StopwatchService.ACTION_PAUSE);
                context.startService(pauseStopwatchIntent);

                viewControl(SESSION_PAUSE);
                isStopwatchPause = true;
            }

        });

        sessionEndFab.setOnClickListener(v -> {
            Intent stopStopwatchIntent = new Intent(context, StopwatchService.class);
            stopStopwatchIntent.setAction(StopwatchService.ACTION_STOP);
            context.startService(stopStopwatchIntent);
            endTime = LocalTime.now();

            viewControl(SESSION_END);
        });
    }

    /**
     * 경과된 시간을 HH:MM:SS 형식으로 스톱워치 텍스트 뷰에 표시합니다.
     * @param elapsedTime 경과된 시간(초)입니다.
     */
    private void setStopwatchTime(long elapsedTime) {
        long hour = elapsedTime / (60 * 60);
        long minute = (elapsedTime % (60 * 60)) / 60;
        long second = elapsedTime % 60;
        currentHours = hour;
        currentMinutes = minute;
        currentSeconds = second;

        stopwatchTextview.setText(String.format("%02d:%02d:%02d", currentHours, currentMinutes, currentSeconds));
    }

    /**
     * 세션 추가 다이얼로그를 표시하고 데이터를 전달합니다.
     */
    private void addSession() {
        int subjectId = selectedSubjectViewModel.selectedSubjectLiveData.getValue().getSubjectId();
        String subjectName = selectedSubjectViewModel.selectedSubjectLiveData.getValue().getName();
        LocalDate date = LocalDate.now();
        Duration duration = Duration.between(startTime, endTime);
        long restTime = duration.toMinutes() - sessionDuration;
        if (restTime < 0) {
            sessionDuration = duration.toMinutes();
            restTime = 0;
        }

        Bundle bundle = new Bundle();
        bundle.putInt(SessionAddDialog.KEY_SESSION_MODE, SessionAddDialog.MODE_ADD);
        bundle.putInt(SessionAddDialog.KEY_SUBJECT_ID, subjectId);
        bundle.putString(SessionAddDialog.KEY_SUBJECT_NAME, subjectName);
        bundle.putInt(SessionAddDialog.KEY_START_HOUR, startTime.getHour());
        bundle.putInt(SessionAddDialog.KEY_START_MINUTE, startTime.getMinute());
        bundle.putInt(SessionAddDialog.KEY_END_HOUR, endTime.getHour());
        bundle.putInt(SessionAddDialog.KEY_END_MINUTE, endTime.getMinute());
        bundle.putInt(SessionAddDialog.KEY_REST_TIME, (int) restTime);
        bundle.putLong(SessionAddDialog.KEY_START_DATE, getUtcMillis(date));
        if (duration.toMinutes() < 0) {
            bundle.putLong(SessionAddDialog.KEY_END_DATE, getUtcMillis(date.plusDays(1)));
        }
        else {
            bundle.putLong(SessionAddDialog.KEY_END_DATE, getUtcMillis(date));
        }

        SessionAddDialog dialog = SessionAddDialog.newInstance(bundle);
        dialog.show(getParentFragmentManager(), "SessionAddDialog");
    }

    /**
     * LocalDate 객체를 UTC 밀리초로 변환합니다.
     * @param date 변환할 LocalDate 객체입니다.
     * @return UTC 밀리초 값입니다.
     */
    private long getUtcMillis(LocalDate date) {
        return date.atStartOfDay(ZoneId.of("UTC")).toInstant().toEpochMilli();
    }
}