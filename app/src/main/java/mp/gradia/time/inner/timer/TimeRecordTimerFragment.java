package mp.gradia.time.inner.timer;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
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

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Date;
import java.util.Timer;

import mp.gradia.R;
import mp.gradia.database.AppDatabase;
import mp.gradia.database.dao.StudySessionDao;
import mp.gradia.database.dao.SubjectDao;
import mp.gradia.database.entity.StudySessionEntity;
import mp.gradia.time.inner.SubjectViewModel;
import mp.gradia.time.inner.SubjectViewModelFactory;
import orion.gz.pomodorotimer.OnTimerChangeListener;
import orion.gz.pomodorotimer.TimerView;

public class TimeRecordTimerFragment extends Fragment {
    private static final long DEFAULT_MINUTES = 25;
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

    private boolean isTimerPause = false;
    private boolean isMuted = false;
    private long sessionDuration;
    private long currentMinutes = 25;
    private long currentSeconds = 0;
    private LocalTime startTime;
    private LocalTime endTime;

    private AppDatabase db;
    private SubjectDao dao;
    private SubjectViewModel selectedSubjectViewModel;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        db = AppDatabase.getInstance(requireContext());
        SubjectDao dao = db.subjectDao();
        SubjectViewModelFactory factory = new SubjectViewModelFactory(dao);
        selectedSubjectViewModel = new ViewModelProvider(requireParentFragment(), factory).get(SubjectViewModel.class);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_time_record_timer, container, false);

        initViews(v);
        setupTimer();
        setupTimeControl();
        setupTimerControl();
        updateTimerView();

        return v;
    }


    private final BroadcastReceiver timerUpdateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            Log.d("TimerService", "Receive Broadcast :" + intent.getAction());
            final String action = TimerService.BROADCAST_ACTION_TIMER_UPDATE;

            if (intent != null && action.equals(intent.getAction())) {
                long remaining = intent.getLongExtra(TimerService.BROADCAST_EXTRA_REMAINING, 0L) / 1000;
                long minutes = remaining / 60;
                long seconds = remaining % 60;
                currentMinutes = (int) minutes;
                currentSeconds = (int) seconds;
                timeTextview.setText(String.format("%2d:%2d", currentMinutes, currentSeconds));
                timerView.setTime(currentMinutes, currentSeconds);
            }
        }
    };

    private final BroadcastReceiver timerStopReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            timerView.setTime(DEFAULT_MINUTES, 0);
            Log.d("TimerService", "Receive Broadcast :" + intent.getAction());
            final String action = TimerService.BROADCAST_ACTION_TIMER_STOP;

            if (intent != null && action.equals(intent.getAction())) {
                resetTimer();
                addSession();
            }
        }
    };

    @Override
    public void onResume() {
        super.onResume();
        Context context = requireContext();
        String update = TimerService.BROADCAST_ACTION_TIMER_UPDATE;
        String stop = TimerService.BROADCAST_ACTION_TIMER_STOP;

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            LocalBroadcastManager.getInstance(context).registerReceiver(timerUpdateReceiver, new IntentFilter(update));
            LocalBroadcastManager.getInstance(context).registerReceiver(timerStopReceiver, new IntentFilter(stop));
            Log.d("TimerService", "registerReceiver");
        } else {
            Log.w("TimerService", "registerReceiver Falied");
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        Context context = requireContext();
        context.unregisterReceiver(timerUpdateReceiver);
        context.unregisterReceiver(timerStopReceiver);
    }

    private void initViews(View v) {
        timerView = v.findViewById(R.id.timer_view);
        timeTextview = v.findViewById(R.id.time_textview);

        timeControlLayout = v.findViewById(R.id.timer_time_control_layout);
        addMinuteFab = v.findViewById(R.id.add_minute_fab);
        subtractMinuteFab = v.findViewById(R.id.subtract_minute_fab);

        sessionControlLayout = v.findViewById(R.id.timer_session_control_layout);
        sessionStartBtn = v.findViewById(R.id.session_start_btn);
        sessionControlFab = v.findViewById(R.id.session_control_fab);
        sessionEndFab = v.findViewById(R.id.session_end_fab);
        muteFab = v.findViewById(R.id.mute_fab);
    }

    private void resetTimer() {
        if (isTimerPause) isTimerPause = false;
        timerView.resetRotation();
        timerView.setTime(DEFAULT_MINUTES, 0);
        timerView.setTouchable(true);
        timerView.setAlpha(1F);

        sessionControlFab.setImageResource(R.drawable.outline_pause_black_24);
        sessionControlLayout.setVisibility(View.GONE);
        addMinuteFab.setVisibility(View.GONE);
        subtractMinuteFab.setVisibility(View.GONE);
        sessionStartBtn.setVisibility(View.VISIBLE);
    }

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

    private void setupTimeControl() {
        Context context = requireContext();
        addMinuteFab.setOnClickListener(v-> {
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
    private void setupTimerControl() {
        Context context = requireContext();
        sessionStartBtn.setOnClickListener(v -> {
            Intent startTimerIntent = new Intent(context, TimerService.class);
            startTimerIntent.setAction(TimerService.ACTION_START);
            sessionDuration = currentMinutes;
            startTimerIntent.putExtra(TimerService.TIMER_TIME, sessionDuration * 60);
            context.startService(startTimerIntent);
            startTime = LocalTime.now();

            sessionStartBtn.setVisibility(View.GONE);
            sessionControlLayout.setVisibility(View.VISIBLE);
            addMinuteFab.setVisibility(View.VISIBLE);
            subtractMinuteFab.setVisibility(View.VISIBLE);
            timerView.setTouchable(false);
        });
        sessionControlFab.setOnClickListener(v -> {
            if (isTimerPause) {
                Intent resumeTimerIntent = new Intent(context, TimerService.class);
                resumeTimerIntent.setAction(TimerService.ACTION_RESUME);
                context.startService(resumeTimerIntent);
                timerView.setAlpha(1F);

                sessionControlFab.setImageResource(R.drawable.outline_pause_black_24);
                isTimerPause = false;
            } else {
                Intent pauseTimerIntent = new Intent(context, TimerService.class);
                pauseTimerIntent.setAction(TimerService.ACTION_PAUSE);
                context.startService(pauseTimerIntent);
                timerView.setAlpha(0.5F);

                isTimerPause = true;
                sessionControlFab.setImageResource(R.drawable.outline_play_arrow_black_24);
            }
        });

        sessionEndFab.setOnClickListener(v -> {
            Intent stopTimerIntent = new Intent(context, TimerService.class);
            stopTimerIntent.setAction(TimerService.ACTION_STOP);
            context.startService(stopTimerIntent);
            resetTimer();
            addSession();
        });

        muteFab.setOnClickListener(v -> {
            if (!isMuted) {
                muteFab.setImageResource(R.drawable.outline_volume_off_black_24);
                isMuted = true;
                Intent muteAlarmIntent = new Intent(context, TimerService.class);
                muteAlarmIntent.setAction(TimerService.ACTION_MUTE);
                context.startService(muteAlarmIntent);
            } else {
                muteFab.setImageResource(R.drawable.outline_volume_up_black_24);
                isMuted = false;
                Intent unmuteAlarmIntent = new Intent(context, TimerService.class);
                unmuteAlarmIntent.setAction(TimerService.ACTION_UNMUTE);
                context.startService(unmuteAlarmIntent);
            }
        });
    }

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

    private void updateTimeText() {
        String time = String.format("%02d:%02d", currentMinutes, currentSeconds);
        timeTextview.setText(time);
    }

    private void addSession() {
        int subjectId = selectedSubjectViewModel.selectedSubjectLiveData.getValue().getSubjectId();
        LocalDate date = LocalDate.now();
        StudySessionEntity studySessionEntity = new StudySessionEntity(subjectId, date, sessionDuration, startTime, endTime,);
    }
}
