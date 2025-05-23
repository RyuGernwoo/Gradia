package mp.gradia.time.inner.record.timer;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.media.AudioAttributes;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.SystemClock;
import android.provider.Settings;
import android.util.Log;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.core.content.ContextCompat;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import java.util.Locale;

import mp.gradia.R;
import mp.gradia.main.MainActivity;

public class TimerService extends Service {
    // 로깅 태그
    private static final String TAG = "TimerService";

    // 액션 상태
    public static final String ACTION_START = "ACTION_START";
    public static final String ACTION_PAUSE = "ACTION_PAUSE";
    public static final String ACTION_RESUME = "ACTION_RESUME";
    public static final String ACTION_ADJUST_TIME = "ACTION_ADJUST_TIME";
    public static final String ACTION_STOP = "ACTION_STOP";
    public static final String ACTION_MUTE = "ACTION_MUTE";
    public static final String ACTION_UNMUTE = "ACTION_UNMUTE";

    // 브로드캐스트 액션
    public static final String BROADCAST_ACTION_TIMER_STATE_CHANGED = "TIMER_STATE_CHANGED";
    public static final String BROADCAST_ACTION_TIMER_UPDATE = "TIMER_UPDATE";
    public static final String BROADCAST_ACTION_TIMER_STOP = "TIMER_STOP";

    // SharedPreferences 키
    public static final String PREFS_NAME = "TimerServiceState";
    public static final String KEY_SELECTED_SUBJECT_ID = "selectedSubjectId";
    public static final String KEY_DURATION_TIME = "durationTime";
    public static final String KEY_REMAINING_TIME = "remainingTime";
    public static final String KEY_START_TIME = "startTime";
    public static final String KEY_IS_RUNNING = "isRunning";
    public static final String KEY_IS_PAUSED = "isPaused";
    public static final String KEY_IS_MUTED = "isMuted";

    // 인텐트 Extra 키
    public static final String BROADCAST_EXTRA_REMAINING_TIME = "REMAINING_TIME";
    public static final String EXTRA_ADJUSTMENT_TIME = "EXTRA_ADJUSTMENT_TIME";
    public static final String EXTRA_SHOW_TIMER_FRAGMENT = "EXTRA_SHOW_TIMER_FRAGMENT";

    // 번들 키
    public static final String BUNDLE_TIMER_TIME = "BUNLDE_TIMER_TIME";
    public static final String BUNDLE_SUBJECT_NAME = "SUBJECT_NAME";
    public static final String BUNDLE_SUBJECT_ID = "SUBJECT_ID";
    public static final String BUNDLE_START_TIME = "START_TIME";

    // 알림 채널 및 ID
    private static final String CHANNEL_ID = "timer_channel";
    private static final String TEMP_CHANNEL_ID = "timer_temp_channel";
    private static final String TEMP_MUTE_CHANNEL_ID = "timer_temp_muted_channel";
    private static final int NOTIFICATION_ID = 1;
    private static final int TEMP_NOTIFICATION_ID = 2;

    // 시간 변수
    private Handler handler;
    private Runnable timerRunnable;

    private long startTimeMillis = 0L;
    private long durationMillis = 0L;
    private long remainingMillis = 0L;

    // 상태 변수
    private boolean isTimerRunning = false;
    private boolean isPaused = false;
    private boolean isMuted = false;
    private int subjectId;
    private String subjectName;
    private String startTime;

    /**
     * 타이머 상태를 SharedPreferences에 저장합니다.
     */
    private void saveStateToPrefs() {
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();

        editor.putInt(KEY_SELECTED_SUBJECT_ID, subjectId);
        editor.putLong(KEY_DURATION_TIME, durationMillis / 1000L);
        editor.putString(KEY_START_TIME, startTime);
        editor.putBoolean(KEY_IS_RUNNING, isTimerRunning);
        editor.putBoolean(KEY_IS_PAUSED, isPaused);
        editor.putBoolean(KEY_IS_MUTED, isMuted);

        editor.apply();
    }

    /**
     * 서비스가 처음 생성될 때 호출됩니다. 핸들러, 알림 채널을 생성하고 타이머 상태를 초기화합니다.
     */
    @Override
    public void onCreate() {
        super.onCreate();

        Log.d(TAG, "onCreate");
        handler = new Handler(Looper.getMainLooper());
        createNotificationChannel();
        initTimerState();
    }

    /**
     * 서비스가 시작될 때마다 호출됩니다. 인텐트 액션에 따라 타이머 작업을 수행합니다.
     */
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent == null || intent.getAction() == null) {
            Log.w(TAG, "onStartCommand: Intent or Action is null");
            stopSelf();
            return START_NOT_STICKY;
        }

        String action = intent.getAction();
        Log.d(TAG, "Action Received : " + action);


        switch (action) {
            case ACTION_START:
                Bundle bundle = intent.getExtras();
                long duration = bundle.getLong(BUNDLE_TIMER_TIME);
                if (duration > 0 && !isTimerRunning) {
                    subjectName = intent.getStringExtra(BUNDLE_SUBJECT_NAME);
                    subjectId = intent.getIntExtra(BUNDLE_SUBJECT_ID, 0);
                    startTime = intent.getStringExtra(BUNDLE_START_TIME);

                    showTempNotification(this, "세션이 시작되었습니다. 오늘도 열심히 달려볼까요?");
                    startTimer(duration);
                    saveStateToPrefs();
                } else if (isTimerRunning) {
                    Log.w(TAG, "Timer is already running");
                    // showTempNotification(this, "Timer is already running");
                } else {
                    Log.w(TAG, "Invalid duration: " + duration);
                    stopSelf();
                }
                break;
            case ACTION_ADJUST_TIME:
                long adjustDuration = intent.getLongExtra(EXTRA_ADJUSTMENT_TIME, 0L);
                if (isTimerRunning && adjustDuration != 0) {
                    adjustTimerTime(adjustDuration);
                    saveStateToPrefs();
                } else
                    Log.w(TAG, "Cannot Adjust Time");
                break;
            case ACTION_PAUSE:
                pauseTimer();
                saveStateToPrefs();
                break;
            case ACTION_RESUME:
                resumeTimer();
                saveStateToPrefs();
                break;
            case ACTION_STOP:
                stopTimer();
                saveStateToPrefs();
                break;
            case ACTION_MUTE:
                isMuted = true;
                saveStateToPrefs();
                break;
            case ACTION_UNMUTE:
                isMuted = false;
                saveStateToPrefs();
                break;
        }

        return START_REDELIVER_INTENT;
    }

    /**
     * 타이머 상태 관련 변수들을 초기 상태로 설정합니다.
     */
    private void initTimerState() {
        timerRunnable = null;

        isPaused = false;
        isTimerRunning = false;

        remainingMillis = 0;
        startTimeMillis = 0;
        durationMillis = 0;
    }

    /**
     * 지정된 시간(초)으로 타이머를 시작하고 포그라운드 알림을 표시합니다.
     */
    private void startTimer(long duration) {
        Log.d(TAG, "Starting timer for " + duration + " seconds");
        if (handler != null && timerRunnable != null)
            handler.removeCallbacks(timerRunnable);

        isTimerRunning = true;
        isPaused = false;

        durationMillis = duration * 1000L;
        startTimeMillis = SystemClock.elapsedRealtime();

        startForeground(NOTIFICATION_ID, createNotification(formatMillis(durationMillis)));
        startPeriodicUpdates();
        sendTimerUpdateBroadcast(durationMillis);
    }

    /**
     * 타이머 시간을 조정합니다.
     */
    private void adjustTimerTime(long adjustDuration) {
        Log.d(TAG, "Adjusting timer time by " + adjustDuration + " seconds");
        long adjustMillis = adjustDuration * 1000;
        durationMillis += adjustMillis;
        remainingMillis += adjustMillis;
        sendTimerUpdateBroadcast(remainingMillis);
    }

    /**
     * 타이머를 일시 중지합니다.
     */
    private void pauseTimer() {
        if (isTimerRunning && !isPaused) {
            Log.d(TAG, "Pausing Timer");
            isPaused = true;

            if (handler != null && timerRunnable != null)
                handler.removeCallbacks(timerRunnable);

            remainingMillis = durationMillis - (SystemClock.elapsedRealtime() - startTimeMillis);
            if (remainingMillis < 0) remainingMillis = 0;
            updateNotification(formatMillis(remainingMillis));
            showTempNotification(this, "세션이 중단되었습니다.");
            sendTimerUpdateBroadcast(remainingMillis);
            Log.d(TAG, "Paused. Remaining millis: " + remainingMillis);
        } else {
            Log.d(TAG, "Timer not running or already paused");
        }
    }

    /**
     * 일시 중지된 타이머를 재개합니다.
     */
    private void resumeTimer() {
        if (isTimerRunning && isPaused) {
            if (remainingMillis <= 0) {
                Log.w(TAG, "Cannot resume, remaining time is zero or negative");
                stopTimer();
                return;
            }

            Log.d(TAG, "Resuming timer with " + remainingMillis + " ms remaining");
            isPaused = false;
            startPeriodicUpdates();
            updateNotification(formatMillis(remainingMillis));
            sendTimerUpdateBroadcast(remainingMillis);
        } else
            Log.w(TAG, "Timer not running or not paused");
    }

    /**
     * 타이머를 중지하고 서비스와 포그라운드 알림을 종료합니다.
     */
    private void stopTimer() {
        Log.d(TAG, "Stopping timer and service");
        if (isTimerRunning) {
            showTempNotification(this, "세션이 종료되었습니다.");
            Intent intent = new Intent(BROADCAST_ACTION_TIMER_STOP);
            LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
        }
        // else showTempNotification(this, "Timer is already stopped");

        initTimerState();
        stopForeground(true);
        stopSelf();
    }

    /**
     * 주기적으로 타이머 시간을 업데이트하는 Runnable을 시작합니다.
     */
    private void startPeriodicUpdates() {
        if (handler != null && timerRunnable != null)
            handler.removeCallbacks(timerRunnable);

        timerRunnable = new Runnable() {
            @Override
            public void run() {
                if (!isTimerRunning || isPaused) return;

                long currentTimeMillis = SystemClock.elapsedRealtime();
                long elapsedTimeMillis = currentTimeMillis - startTimeMillis;
                remainingMillis = durationMillis - elapsedTimeMillis;

                if (remainingMillis <= 0) {
                    Log.d(TAG, "Handler check: Time is up or passed");
                    stopTimer();
                } else {
                    updateNotification(formatMillis(remainingMillis));
                    sendTimerUpdateBroadcast(remainingMillis);
                    handler.postDelayed(this, 1000);
                }
            }
        };
        handler.post(timerRunnable);
    }

    /**
     * 타이머 업데이트 정보를 Broadcast로 전송합니다.
     */
    private void sendTimerUpdateBroadcast(long remainingMillis) {
        Intent intent = new Intent(BROADCAST_ACTION_TIMER_UPDATE);
        if (remainingMillis < 0) remainingMillis = 0;
        intent.putExtra(BROADCAST_EXTRA_REMAINING_TIME, remainingMillis);

        LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
        Log.d(TAG, "Send Broadcast");
    }

    /**
     * 타이머 알림을 위한 Notification Channel을 생성합니다.
     */
    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            String name = "Timer Channel";
            String description = "Timer Service Channel";
            int importance = NotificationManager.IMPORTANCE_LOW;

            NotificationChannel channel = new NotificationChannel(CHANNEL_ID, name, importance);
            channel.setDescription(description);

            NotificationManager notificationManager = ContextCompat.getSystemService(getApplicationContext(), NotificationManager.class);
            if (notificationManager != null) {
                notificationManager.createNotificationChannel(channel);
            }
            createTempNotificationChannel(this);
        }
    }

    /**
     * 타이머 상태 변경 알림을 위한 Notification Channel을 생성합니다.
     */
    private void createTempNotificationChannel(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationManager notificationManager = ContextCompat.getSystemService(context, NotificationManager.class);

            // 일반 알림 채널
            String nameNormal = "Timer State Change Alerts";
            String descriptionNormal = "Timer State Change Channel";
            int importance = NotificationManager.IMPORTANCE_HIGH;


            NotificationChannel channelNormal = new NotificationChannel(TEMP_CHANNEL_ID, nameNormal, importance);
            AudioAttributes audioAttributes = new AudioAttributes.Builder()
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .setUsage(AudioAttributes.USAGE_NOTIFICATION_EVENT)
                    .build();
            channelNormal.setSound(Settings.System.DEFAULT_NOTIFICATION_URI, audioAttributes);
            channelNormal.enableVibration(true);
            channelNormal.setVibrationPattern(new long[]{0, 250, 100, 250});
            channelNormal.setDescription(descriptionNormal);

            if (notificationManager != null) {
                notificationManager.createNotificationChannel(channelNormal);
            }

            // 음소거 알림 채널
            String nameMuted = "Timer State Change Alerts (Muted)";
            String descriptionMuted = "Timer State Change Channel (Muted)";

            NotificationChannel channelMuted = new NotificationChannel(TEMP_MUTE_CHANNEL_ID, nameMuted, importance);
            channelMuted.setSound(null, null);
            channelMuted.enableVibration(false);
            channelMuted.setDescription(descriptionMuted);

            if (notificationManager != null) {
                notificationManager.createNotificationChannel(channelMuted);
            }
        }
    }

    /**
     * 포그라운드 서비스 알림을 생성합니다.
     */
    private Notification createNotification(String timeText) {
        /*
        Intent pauseResumeIntent = new Intent(this, TimerService.class);
        String pauseResumeAction = isPaused ? ACTION_RESUME : ACTION_PAUSE;
        String pauseResumeTitle = isPaused ? "Resume" : "Pause";
        int pauseResumeIcon = isPaused ? R.drawable.outline_pause_black_24 : R.drawable.outline_play_arrow_black_24;
        pauseResumeIntent.setAction(pauseResumeAction);
        PendingIntent pauseResumePendingIntent = PendingIntent.getService(this, 1, pauseResumeIntent, intentFlags);

        Intent stopIntent = new Intent(this, TimerService.class);
        stopIntent.setAction(ACTION_STOP);
        PendingIntent stopPendingIntent = PendingIntent.getService(this, 2, stopIntent, intentFlags);
        */

        Intent intent = new Intent(this, MainActivity.class);
        intent.putExtra(EXTRA_SHOW_TIMER_FRAGMENT, MainActivity.TIME_FRAGMENT);
        int intentFlags = PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE;
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, intent, intentFlags) ;
        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle(subjectName + " 세션")
                .setContentText(timeText)
                .setSmallIcon(R.drawable.outline_hourglass_top_black_24)
                .setContentIntent(pendingIntent)
                .setOngoing(true)
                .setOnlyAlertOnce(true)
                .setContentIntent(pendingIntent);

        /*
        builder.addAction(pauseResumeIcon, pauseResumeTitle, pauseResumePendingIntent);
        builder.addAction(R.drawable.outline_stop_black_24, "Stop", stopPendingIntent);
        */
        return builder.build();
    }

    /**
     * 임시 알림을 표시합니다 (세션 시작/종료 등).
     */
    private void showTempNotification(Context context, String message) {
        createTempNotificationChannel(context);

        Intent intent = new Intent(context, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        intent.putExtra(EXTRA_SHOW_TIMER_FRAGMENT, MainActivity.TIME_FRAGMENT);
        PendingIntent pendingIntent = PendingIntent.getActivity(context, 3, intent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        String channelId = TEMP_CHANNEL_ID;
        if (isMuted) channelId = TEMP_MUTE_CHANNEL_ID;
        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, channelId)
                .setSmallIcon(R.drawable.outline_hourglass_top_black_24)
                .setContentTitle(subjectName)
                .setContentText(message)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true)
                .setContentIntent(pendingIntent);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
            builder.setTimeoutAfter(2500);

        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(context);
        notificationManager.notify(TEMP_NOTIFICATION_ID, builder.build());
    }

    /**
     * 포그라운드 알림의 시간을 업데이트합니다.
     */
    private void updateNotification(String timeText) {
        Notification notification = createNotification(timeText);
        NotificationManagerCompat.from(this).notify(1, notification);
    }

    /**
     * 밀리초를 HH:MM:SS 또는 MM:SS 형식의 문자열로 변환합니다.
     * @param millis 변환할 밀리초
     * @return 형식화된 시간 문자열
     */
    private static String formatMillis(long millis) {
        long seconds = millis / 1000;
        long minutes = seconds / 60;
        long hours = minutes / 60;

        seconds %= 60;
        minutes %= 60;

        if (hours > 0)
            return String.format(Locale.getDefault(), "%02d:%02d:%02d", hours, minutes, seconds);
        else return String.format(Locale.getDefault(), "%02d:%02d", minutes, seconds);
    }

    /**
     * 서비스가 소멸될 때 호출됩니다. 상태를 저장하고 핸들러 콜백을 제거합니다.
     */
    @Override
    public void onDestroy() {
        saveStateToPrefs();
        super.onDestroy();
        if (handler != null && timerRunnable != null)
            handler.removeCallbacks(timerRunnable);
    }

    /**
     * 클라이언트가 서비스에 바인딩할 때 호출됩니다 (현재 사용 안 함).
     */
    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}