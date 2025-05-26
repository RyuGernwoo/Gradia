package mp.gradia.time.inner.record.timer;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Button;

import androidx.core.graphics.ColorUtils;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.Locale;

import mp.gradia.R;
import mp.gradia.database.AppDatabase;
import mp.gradia.database.dao.SubjectDao;
import mp.gradia.time.inner.viewmodel.SubjectViewModel;
import mp.gradia.time.inner.viewmodel.SubjectViewModelFactory;
import mp.gradia.time.inner.record.dialog.SessionAddDialog;
import orion.gz.pomodorotimer.OnTimerChangeListener;
import orion.gz.pomodorotimer.TimerView;

public class TimeRecordTimerFragment extends Fragment {
    // 세션 상태 상수
    private static final int SESSION_START = 0;
    private static final int SESSION_END = 1;
    private static final int SESSION_PAUSE = 2;
    private static final int SESSION_RESUME = 3;
    private static final int MUTE = 4;
    private static final int UNMUTE = 5;

    // 기본 타이머 시간 (분)
    private static final long DEFAULT_MINUTES = 25;

    // UI 컴포넌트
    private TimerView timerView;
    private TextView timeTextview;
    private Button sessionStartBtn;
    private LinearLayout timeControlLayout;
    private LinearLayout sessionControlLayout;
    private FloatingActionButton subtractMinuteFab;
    private FloatingActionButton addMinuteFab;
    private FloatingActionButton sessionControlFab;
    private FloatingActionButton sessionEndFab;
    private FloatingActionButton muteFab;

    // 상태 변수
    private boolean isTimerPause = false;
    private boolean isMuted = false;
    private int sessionState = -1;
    private long sessionDuration;
    private long currentMinutes = 25;
    private long currentSeconds = 0;
    private LocalTime startTime;
    private LocalTime endTime;

    // 데이터베이스 및 뷰모델
    private AppDatabase db;
    private SubjectDao dao;
    private SubjectViewModel selectedSubjectViewModel;

    /**
     * 타이머 업데이트를 위한 BroadcastReceiver입니다.
     * 타이머 서비스로부터 남은 시간 업데이트를 수신하여 UI를 갱신합니다.
     */
    private final BroadcastReceiver timerUpdateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            Log.d("TimerService", "Receive Broadcast :" + intent.getAction());
            final String action = TimerService.BROADCAST_ACTION_TIMER_UPDATE;

            if (intent != null && action.equals(intent.getAction())) {
                // 타이머 상태 복원 (프래그먼트가 처음 로드될 때)
                if (sessionState == -1)
                    restoreTimerState();
                else {
                    long remainingTime = intent.getLongExtra(TimerService.BROADCAST_EXTRA_REMAINING_TIME, 0L) / 1000;
                    setTimerTime(remainingTime);
                }
            }
        }
    };

    /**
     * 타이머 중지를 위한 BroadcastReceiver입니다.
     * 타이머 서비스로부터 타이머 중지 신호를 수신하여 타이머를 초기화하고 세션 추가 다이얼로그를 표시합니다.
     */
    private final BroadcastReceiver timerStopReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            timerView.setTime(DEFAULT_MINUTES, 0);
            Log.d("TimerService", "Receive Broadcast :" + intent.getAction());
            final String action = TimerService.BROADCAST_ACTION_TIMER_STOP;

            if (intent != null && action.equals(intent.getAction())) {
                endTime = LocalTime.now();
                resetTimer();
                addSession();
            }
        }
    };

    /**
     * SharedPreferences에 저장된 타이머 상태를 복원합니다.
     * 타이머의 남은 시간, 음소거 여부, 일시 정지 여부, 세션 지속 시간, 시작 시간을 로드합니다.
     */
    private void restoreTimerState() {
        SharedPreferences prefs = requireContext().getSharedPreferences(TimerService.PREFS_NAME, Context.MODE_PRIVATE);
        long remainingTime = prefs.getLong(TimerService.KEY_REMAINING_TIME, 0);
        isMuted = prefs.getBoolean(TimerService.KEY_IS_MUTED, false);
        isTimerPause = prefs.getBoolean(TimerService.KEY_IS_PAUSED, false);
        sessionDuration = prefs.getLong(TimerService.KEY_DURATION_TIME, 0);
        startTime = prefs.getString(TimerService.KEY_START_TIME, null) == null ? null : LocalTime.parse(prefs.getString(TimerService.KEY_START_TIME, null));

        setTimerTime(remainingTime);
        sessionState = SESSION_START;
        viewControl(sessionState);

        if (isMuted) viewControl(MUTE);
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
        View v = inflater.inflate(R.layout.fragment_time_record_timer, container, false);

        initViews(v);
        setupTimer();
        setupTimeControl();
        setupTimerControl();
        updateTimerView();

        if (getArguments() != null) {
            isTimerPause = getArguments().getBoolean(TimerService.KEY_IS_PAUSED);
            if (isTimerPause) {
                restoreTimerState();

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
        String update = TimerService.BROADCAST_ACTION_TIMER_UPDATE;
        String stop = TimerService.BROADCAST_ACTION_TIMER_STOP;
        String state = TimerService.BROADCAST_ACTION_TIMER_STATE_CHANGED;

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            LocalBroadcastManager.getInstance(context).registerReceiver(timerUpdateReceiver, new IntentFilter(update));
            LocalBroadcastManager.getInstance(context).registerReceiver(timerStopReceiver, new IntentFilter(stop));
            Log.d("TimerService", "registerReceiver");
        } else {
            Log.w("TimerService", "registerReceiver Falied");
        }
    }

    /**
     * 프래그먼트가 일시 중지될 때 호출됩니다. BroadcastReceiver를 등록 해제합니다.
     */
    @Override
    public void onPause() {
        super.onPause();
        Context context = requireContext();
        LocalBroadcastManager.getInstance(context).unregisterReceiver(timerUpdateReceiver);
        LocalBroadcastManager.getInstance(context).unregisterReceiver(timerStopReceiver);
    }

    /**
     * 프래그먼트 뷰가 소멸될 때 호출됩니다. 현재 타이머 상태(남은 시간)를 SharedPreferences에 저장합니다.
     */
    @Override
    public void onDestroyView() {
        super.onDestroyView();
        Context context = requireContext();
        SharedPreferences prefs = context.getSharedPreferences(TimerService.PREFS_NAME, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putLong(TimerService.KEY_REMAINING_TIME, currentMinutes * 60 + currentSeconds);
        editor.apply();
    }

    /**
     * UI 컴포넌트들을 초기화합니다.
     */
    private void initViews(View v) {
        String formattedTimeText = String.format(Locale.getDefault(), "%02d:%02d", currentMinutes, currentSeconds);
        timerView = v.findViewById(R.id.timer_view);
        timeTextview = v.findViewById(R.id.time_textview);
        timeTextview.setText(formattedTimeText);

        timeControlLayout = v.findViewById(R.id.timer_time_control_layout);
        addMinuteFab = v.findViewById(R.id.add_minute_fab);
        subtractMinuteFab = v.findViewById(R.id.subtract_minute_fab);

        sessionControlLayout = v.findViewById(R.id.timer_session_control_layout);
        sessionStartBtn = v.findViewById(R.id.session_start_btn);
        sessionControlFab = v.findViewById(R.id.session_control_fab);
        sessionEndFab = v.findViewById(R.id.session_end_fab);
        muteFab = v.findViewById(R.id.mute_fab);
    }

    /**
     * 타이머 세션 상태에 따라 UI를 업데이트합니다.
     */
    private void viewControl(int state) {
        switch (state) {
            case SESSION_START:
                sessionState = SESSION_START;
                sessionStartBtn.setVisibility(View.GONE);
                sessionControlLayout.setVisibility(View.VISIBLE);
                addMinuteFab.setVisibility(View.VISIBLE);
                subtractMinuteFab.setVisibility(View.VISIBLE);
                timerView.setTouchable(false);

                try {
                    Fragment parent = requireParentFragment();
                    View parentView = parent.getView();

                    if (parentView != null) {
                        parentView.findViewById(R.id.add_session_fab).setVisibility(View.GONE);
                        parentView.findViewById(R.id.subject_select_btn).setClickable(false);
                    }
                } catch (IllegalStateException e) {
                    Log.e("TimeRecordTimerFragment", "TimeRecordFragment not found");
                }
                break;
            case SESSION_END:
                sessionState = SESSION_END;
                sessionControlFab.setImageResource(R.drawable.outline_pause_black_24);
                muteFab.setImageResource(R.drawable.outline_volume_up_black_24);
                sessionControlLayout.setVisibility(View.GONE);
                addMinuteFab.setVisibility(View.GONE);
                subtractMinuteFab.setVisibility(View.GONE);
                sessionStartBtn.setVisibility(View.VISIBLE);

                try {
                    Fragment parent = requireParentFragment();
                    View parentView = parent.getView();

                    if (parentView != null) {
                        parentView.findViewById(R.id.add_session_fab).setVisibility(View.VISIBLE);
                        parentView.findViewById(R.id.subject_select_btn).setClickable(true);
                    }
                } catch (IllegalStateException e) {
                    Log.e("TimeRecordTimerFragment", "TimeRecordFragment not found");
                }
                break;
            case SESSION_PAUSE:
                sessionState = SESSION_PAUSE;
                timerView.setAlpha(0.5F);
                sessionControlFab.setImageResource(R.drawable.outline_play_arrow_black_24);
                break;
            case SESSION_RESUME:
                sessionState = SESSION_RESUME;
                timerView.setAlpha(1F);
                sessionControlFab.setImageResource(R.drawable.outline_pause_black_24);
                break;
            case MUTE:
                muteFab.setImageResource(R.drawable.outline_volume_off_black_24);
                break;
            case UNMUTE:
                muteFab.setImageResource(R.drawable.outline_volume_up_black_24);
                break;
        }
    }

    /**
     * 타이머를 초기 상태로 되돌립니다.
     */
    private void resetTimer() {
        if (isTimerPause) isTimerPause = false;
        timerView.resetRotation();
        timerView.setTime(DEFAULT_MINUTES, 0);
        timerView.setTouchable(true);
        timerView.setAlpha(1F);

        viewControl(SESSION_END);
    }

    /**
     * 선택된 과목 정보에 따라 타이머 UI의 색상을 설정합니다.
     */
    private void setupTimer() {
        selectedSubjectViewModel.selectedSubjectLiveData.observe(getViewLifecycleOwner(),
                subject -> {
                    Log.d("COLOR", String.valueOf(subject.getColor()));
                    int color = Color.parseColor(subject.getColor());
                    int brightColor = ColorUtils.blendARGB(color, Color.WHITE, 0.2F);
                    int darkColor = ColorUtils.blendARGB(color, Color.BLACK, 0.2F);

                    sessionStartBtn.setBackgroundColor(color);
                    timerView.setCirlceColor(color);
                    timerView.setHandColor(darkColor);
                    timerView.setKnobColor(brightColor);
                });
    }

    /**
     * 타이머 시간 조절 (+/- 1분) FAB의 클릭 리스너를 설정합니다.
     */
    private void setupTimeControl() {
        Context context = requireContext();
        addMinuteFab.setOnClickListener(v -> {
            Intent addTimeIntent = new Intent(context, TimerService.class);
            addTimeIntent.setAction(TimerService.ACTION_ADJUST_TIME);
            addTimeIntent.putExtra(TimerService.EXTRA_ADJUSTMENT_TIME, 60L);
            context.startService(addTimeIntent);
            sessionDuration += 1;
        });

        subtractMinuteFab.setOnClickListener(v -> {
            Intent subtractTimeIntent = new Intent(context, TimerService.class);
            subtractTimeIntent.setAction(TimerService.ACTION_ADJUST_TIME);
            subtractTimeIntent.putExtra(TimerService.EXTRA_ADJUSTMENT_TIME, -60L);
            context.startService(subtractTimeIntent);
            sessionDuration -= 1;
        });
    }

    /**
     * 타이머 시작, 중지, 재개, 음소거 FAB의 클릭 리스너를 설정합니다.
     */
    private void setupTimerControl() {
        Context context = requireContext();
        sessionStartBtn.setOnClickListener(v -> {
            sessionDuration = currentMinutes;
            startTime = LocalTime.now();

            Bundle bundle = new Bundle();
            bundle.putLong(TimerService.BUNDLE_TIMER_TIME, sessionDuration * 60);
            bundle.putString(TimerService.BUNDLE_SUBJECT_NAME, selectedSubjectViewModel.selectedSubjectLiveData.getValue().getName());
            bundle.putInt(TimerService.BUNDLE_SUBJECT_ID, selectedSubjectViewModel.selectedSubjectLiveData.getValue().getSubjectId());
            bundle.putString(TimerService.BUNDLE_START_TIME, startTime.toString());

            Intent startTimerIntent = new Intent(context, TimerService.class);
            startTimerIntent.setAction(TimerService.ACTION_START);
            startTimerIntent.putExtras(bundle);
            context.startService(startTimerIntent);

            viewControl(SESSION_START);
        });
        sessionControlFab.setOnClickListener(v -> {
            if (isTimerPause) {
                Intent resumeTimerIntent = new Intent(context, TimerService.class);
                resumeTimerIntent.setAction(TimerService.ACTION_RESUME);
                context.startService(resumeTimerIntent);

                viewControl(SESSION_RESUME);
                isTimerPause = false;
            } else {
                Intent pauseTimerIntent = new Intent(context, TimerService.class);
                pauseTimerIntent.setAction(TimerService.ACTION_PAUSE);
                context.startService(pauseTimerIntent);

                viewControl(SESSION_PAUSE);
                isTimerPause = true;

            }
        });

        sessionEndFab.setOnClickListener(v -> {
            Intent stopTimerIntent = new Intent(context, TimerService.class);
            stopTimerIntent.setAction(TimerService.ACTION_STOP);
            context.startService(stopTimerIntent);
            endTime = LocalTime.now();
            resetTimer();
        });

        muteFab.setOnClickListener(v -> {
            if (!isMuted) {
                viewControl(MUTE);
                isMuted = true;
                Intent muteAlarmIntent = new Intent(context, TimerService.class);
                muteAlarmIntent.setAction(TimerService.ACTION_MUTE);
                context.startService(muteAlarmIntent);
            } else {
                viewControl(UNMUTE);
                isMuted = false;
                Intent unmuteAlarmIntent = new Intent(context, TimerService.class);
                unmuteAlarmIntent.setAction(TimerService.ACTION_UNMUTE);
                context.startService(unmuteAlarmIntent);
            }
        });
    }

    /**
     * 남은 시간을 기반으로 타이머 뷰와 텍스트를 업데이트합니다.
     * @param remainingTime 남은 시간 (초)
     */
    private void setTimerTime(long remainingTime) {
        long minutes = remainingTime / 60;
        long seconds = remainingTime % 60;
        currentMinutes = minutes;
        currentSeconds = seconds;

        timerView.setTime(currentMinutes, currentSeconds);
        updateTimeText();
    }
    /**
     * TimerView의 타이머 변경 리스너를 설정하여 현재 시간 표시를 업데이트합니다.
     */
    private void updateTimerView() {
        timerView.setOnTimerChangeListener(new OnTimerChangeListener() {
            @Override
            public void onTimerChanged(long minutes, long seconds) {
                currentMinutes = minutes;
                currentSeconds = seconds;
                updateTimeText();
            }
        });
    }

    /**
     * 현재 시간 (분:초)을 TimeTextView에 업데이트합니다.
     */
    private void updateTimeText() {
        String formattedTimeText = String.format(Locale.getDefault(), "%02d:%02d", currentMinutes, currentSeconds);
        timeTextview.setText(formattedTimeText);
    }

    /**
     * 세션 추가 다이얼로그를 표시하고 데이터를 전달합니다.
     */
    private void addSession() {
        int subjectId = selectedSubjectViewModel.selectedSubjectLiveData.getValue().getSubjectId();
        String serverSubjectId = selectedSubjectViewModel.selectedSubjectLiveData.getValue().getServerId();
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
        bundle.putString(SessionAddDialog.KEY_SERVER_SUBJECT_ID, serverSubjectId);
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
     * @param date 변환할 LocalDate 객체
     * @return UTC 기준으로 변환된 밀리초
     */
    private long getUtcMillis(LocalDate date) {
        return date.atStartOfDay(ZoneId.of("UTC")).toInstant().toEpochMilli();
    }
}