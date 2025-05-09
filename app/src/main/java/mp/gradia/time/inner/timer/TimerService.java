package mp.gradia.time.inner.timer;

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
import androidx.lifecycle.ViewModelProvider;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import java.util.Locale;
import java.util.Timer;

import mp.gradia.R;
import mp.gradia.database.AppDatabase;
import mp.gradia.database.dao.SubjectDao;
import mp.gradia.time.inner.SubjectViewModel;
import mp.gradia.time.inner.SubjectViewModelFactory;

public class TimerService extends Service {
    private static final String TAG = "TimerService";

    // ACTION STATE
    public static final String ACTION_START = "ACTION_START";
    public static final String ACTION_PAUSE = "ACTION_PAUSE";
    public static final String ACTION_RESUME = "ACTION_RESUME";
    public static final String ACTION_ADJUST_TIME = "ACTION_ADJUST_TIME";
    public static final String ACTION_STOP = "ACTION_STOP";
    public static final String ACTION_MUTE = "ACTION_MUTE";
    public static final String ACTION_UNMUTE = "ACTION_UNMUTE";

    // Broadcast
    public static final String BROADCAST_ACTION_TIMER_STATE_CHANGED = "TIMER_STATE_CHANGED";
    public static final String BROADCAST_ACTION_TIMER_UPDATE = "TIMER_UPDATE";
    public static final String BROADCAST_ACTION_TIMER_STOP = "TIMER_STOP";

    // Shared Pref
    public static final String PREFS_NAME = "TimerServiceState";
    public static final String KEY_SELECTED_SUBJECT_ID = "selectedSubjectId";
    public static final String KEY_DURATION_TIME = "durationTime";
    public static final String KEY_START_TIME = "startTime";
    public static final String KEY_IS_RUNNING = "isRunning";
    public static final String KEY_IS_PAUSED = "isPaused";
    public static final String KEY_IS_MUTED = "isMuted";

    // Intent EXTRA
    public static final String BROADCAST_EXTRA_REMAINING = "REMAINING";
    public static final String EXTRA_ADJUSTMENT_TIME = "EXTRA_ADJUSTMENT_TIME";

    // Bundle
    public static final String BUNDLE_TIMER_TIME = "BUNLDE_TIMER_TIME";
    public static final String BUNDLE_SUBJECT_NAME = "SUBJECT_NAME";
    public static final String BUNDLE_SUBJECT_ID = "SUBJECT_ID";
    public static final String BUNDLE_START_TIME = "START_TIME";

    // Notification
    private static final String CHANNEL_ID = "timer_channel";
    private static final String TEMP_CHANNEL_ID = "timer_temp_channel";
    private static final String TEMP_MUTE_CHANNEL_ID = "timer_temp_muted_channel";
    private static final int NOTIFICATION_ID = 1;
    private static final int TEMP_NOTIFICATION_ID = 2;

    // Time
    private Handler handler;
    private Runnable timerRunnable;

    private long startTimeMillis = 0L;
    private long durationMillis = 0L;
    private long remainingMillis = 0L;

    private boolean isTimerRunning = false;
    private boolean isPaused = false;
    private boolean isMuted = false;
    private int subjectId;
    private String subjectName;
    private String startTime;

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

    private void sendStateChangedBroadcast() {
        Intent intent = new Intent(BROADCAST_ACTION_TIMER_STATE_CHANGED);
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
    }

    @Override
    public void onCreate() {
        super.onCreate();
        Log.d(TAG, "onCreate");
        handler = new Handler(Looper.getMainLooper());
        createNotificationChannel();
        initTimerState();
    }

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
                }
                else if (isTimerRunning) {
                    Log.w(TAG, "Timer is already running");
                    // showTempNotification(this, "Timer is already running");
                }
                else {
                    Log.w(TAG, "Invalid duration: " + duration);
                    stopSelf();
                }
                break;
            case ACTION_ADJUST_TIME:
                long adjustDuration = intent.getLongExtra(EXTRA_ADJUSTMENT_TIME, 0L);
                if (isTimerRunning && adjustDuration != 0) {
                    adjustTimerTime(adjustDuration);
                    saveStateToPrefs();
                }
                else
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

    private void initTimerState() {
        timerRunnable = null;

        isPaused = false;
        isTimerRunning = false;

        remainingMillis = 0;
        startTimeMillis = 0;
        durationMillis = 0;
    }

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

    private void adjustTimerTime(long adjustDuration) {
        Log.d(TAG, "Adjusting timer time by " + adjustDuration + " seconds");
        long adjustMillis = adjustDuration * 1000;
        durationMillis += adjustMillis;
        remainingMillis += adjustMillis;
        sendTimerUpdateBroadcast(remainingMillis);
    }
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

    private void sendTimerUpdateBroadcast(long remainingMillis) {
        Intent intent = new Intent(BROADCAST_ACTION_TIMER_UPDATE);
        if (remainingMillis < 0) remainingMillis = 0;
        intent.putExtra(BROADCAST_EXTRA_REMAINING, remainingMillis);

        LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
        Log.d(TAG, "Send Broadcast");
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            String name = "Timer Channel";
            String description = "Timer Alarm Channel";
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

    private void createTempNotificationChannel(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationManager notificationManager = ContextCompat.getSystemService(context, NotificationManager.class);

            // Normal Channel
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

            // Muted Channel
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

    private Notification createNotification(String contentText) {
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

        Intent intent = new Intent(this, TimeRecordTimerFragment.class);
        int intentFlags = PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE;
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, intent, intentFlags);
        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle(subjectName + " 세션")
                .setContentText(contentText)
                .setSmallIcon(R.drawable.outline_timer_black_24)
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

    private void showTempNotification(Context context, String message) {
        createTempNotificationChannel(context);

        Intent intent = new Intent(context, TimeRecordTimerFragment.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        PendingIntent pendingIntent = PendingIntent.getActivity(context, 3, intent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        String channelId = TEMP_CHANNEL_ID;
        if (isMuted)
            channelId = TEMP_MUTE_CHANNEL_ID;
        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, channelId)
                .setSmallIcon(R.drawable.outline_timer_black_24)
                .setContentTitle(subjectName)
                .setContentText(message)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true)
                .setContentIntent(pendingIntent);

        /*
        if (isMuted) {
            Log.d("TimerService", "Notification Sound & Vibrate Off");
            builder.setSound(null);
            builder.setVibrate(null);
            builder.setDefaults(0);
        } else {
            Log.d("TimerService", "Notification Sound & Vibrate On");
            builder.setSound(Settings.System.DEFAULT_NOTIFICATION_URI);
            builder.setVibrate(new long[]{0, 250, 100, 250});
        }
        */
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            builder.setTimeoutAfter(2500);
        }

        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(context);
        notificationManager.notify(TEMP_NOTIFICATION_ID, builder.build());
    }

    private void updateNotification(String newText) {
        Notification notification = createNotification(newText);
        NotificationManagerCompat.from(this).notify(1, notification);
    }

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

    @Override
    public void onDestroy() {
        saveStateToPrefs();
        super.onDestroy();
        if (handler != null && timerRunnable != null)
            handler.removeCallbacks(timerRunnable);
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}