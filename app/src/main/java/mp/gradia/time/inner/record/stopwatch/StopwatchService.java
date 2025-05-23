package mp.gradia.time.inner.record.stopwatch;

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

public class StopwatchService extends Service {
    // Tag for Logging
    private static final String TAG = "StopwatchService";

    // ACTION STATE
    public static final String ACTION_START = "ACTION_START";
    public static final String ACTION_PAUSE = "ACTION_PAUSE";
    public static final String ACTION_RESUME = "ACTION_RESUME";
    public static final String ACTION_STOP = "ACTION_STOP";

    // Broadcast Actions
    public static final String BROADCAST_ACTION_STOPWATCH_STATE_CHANGED = "BROADCAST_ACTION_STOPWATCH_STATE_CHANGED";
    public static final String BROADCAST_ACTION_STOPWATCH_UPDATE = "STOPWATCH_UPDATE";
    public static final String BROADCAST_ACTION_STOPWATCH_STOP = "STOPWATCH_STOP";

    // Shared Preference Keys
    public static final String PREFS_NAME = "StopwatchServiceState";
    public static final String KEY_SELECTED_SUBJECT_ID = "selectedSubjectId";
    public static final String KEY_START_TIME = "startTime";
    public static final String KEY_ELAPSED_TIME = "elapsedTime";
    public static final String KEY_IS_RUNNING = "isRunning";
    public static final String KEY_IS_PAUSED = "isPaused";

    // Intent Extra Keys
    public static final String BROADCAST_EXTRA_ELAPSED_TIME = "ELAPSED_TIME";
    public static final String BROADCAST_EXTRA_ACCUMULATED_TIME = "ACCUMULATED_TIME";
    public static final String EXTRA_SHOW_STOPWATCH_FRAGMENT = "EXTRA_SHOW_STOPWATCH_FRAGMENT";

    // Bundle Keys
    public static final String BUNDLE_SUBJECT_NAME = "BUNDLE_SUBJECT_NAME";
    public static final String BUNDLE_SUBJECT_ID = "BUNDLE_SUBJECT_ID";
    public static final String BUNDLE_START_TIME = "BUNDLE_START_TIME";

    // Notification Channels and IDs
    private static final String CHANNEL_ID = "stopwatch_channel";
    private static final String TEMP_CHANNEL_ID = "stopwatch_temp_channel";
    private static final int NOTIFICATION_ID = 3;
    private static final int TEMP_NOTIFICATION_ID = 4;

    // Update Interval
    private static final long UPDATE_INTERVAL_MS = 100;

    // Handler and Runnable
    private Handler handler;
    private Runnable stopwatchRunnable;

    // Time Variables
    private long elapsedMillis = 0L;
    private long startTimeMillis = 0L;
    private long accumulatedMillis = 0L;

    // State Variables
    private boolean isStopwatchRunning = false;
    private boolean isPaused = false;
    private int subjectId;
    private String subjectName;
    private String startTime;

    /**
     * 스톱워치 상태를 SharedPreferences에 저장합니다.
     */
    private void saveStateToPrefs() {
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();

        editor.putInt(KEY_SELECTED_SUBJECT_ID, subjectId);
        editor.putString(KEY_START_TIME, startTime);
        editor.putBoolean(KEY_IS_RUNNING, isStopwatchRunning);
        editor.putBoolean(KEY_IS_PAUSED, isPaused);

        editor.apply();
    }

    /**
     * 서비스가 처음 생성될 때 호출됩니다. 핸들러, 알림 채널을 생성하고 스톱워치 상태를 초기화합니다.
     */
    @Override
    public void onCreate() {
        super.onCreate();
        Log.d(TAG, "onCreate");
        handler = new Handler(Looper.getMainLooper());
        createNotificationChannel();
        initStopwatchState();
    }

    /**
     * 서비스가 시작될 때마다 호출됩니다. 인텐트 액션에 따라 스톱워치 작업을 수행합니다.
     * @param intent 서비스 시작을 위한 Intent입니다.
     * @param flags 서비스 시작 요청에 대한 추가 데이터입니다.
     * @param startId 이 시작 요청을 나타내는 고유한 정수입니다.
     * @return 시스템이 서비스를 어떻게 처리해야 하는지 나타내는 상수입니다.
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
                if (!isStopwatchRunning) {
                    subjectName = intent.getStringExtra(BUNDLE_SUBJECT_NAME);
                    subjectId = intent.getIntExtra(BUNDLE_SUBJECT_ID, 0);
                    startTime = intent.getStringExtra(BUNDLE_START_TIME);

                    showTempNotification(this, "세션이 시작되었습니다. 오늘도 열심히 달려볼까요?");
                    startStopwatch();
                } else if (isStopwatchRunning) Log.w(TAG, "Stopwatch is already running");
                else stopSelf();
                break;
            case ACTION_PAUSE:
                pauseStopwatch();
                break;
            case ACTION_RESUME:
                resumeStopwatch();
                break;
            case ACTION_STOP:
                stopStopwatch();
                break;
        }

        saveStateToPrefs();
        return START_NOT_STICKY;
    }

    /**
     * 스톱워치 상태 관련 변수들을 초기 상태로 설정합니다.
     */
    private void initStopwatchState() {
        stopwatchRunnable = null;

        isPaused = false;
        isStopwatchRunning = false;
        elapsedMillis = 0;
    }

    /**
     * 스톱워치를 시작하고 포그라운드 알림을 표시합니다.
     */
    private void startStopwatch() {
        if (handler != null && stopwatchRunnable != null)
            handler.removeCallbacks(stopwatchRunnable);

        isStopwatchRunning = true;
        isPaused = false;

        accumulatedMillis = 0L;
        startTimeMillis = SystemClock.elapsedRealtime();
        startForeground(NOTIFICATION_ID, createNotification(formatMillis(0L)));
        startPeriodicUpdates();
        sendStopwatchUpdateBroadCast(0L);
    }

    /**
     * 스톱워치를 일시 중지합니다.
     */
    private void pauseStopwatch() {
        if (isStopwatchRunning && !isPaused) {
            isPaused = true;

            if (handler != null && stopwatchRunnable != null)
                handler.removeCallbacks(stopwatchRunnable);

            long now = SystemClock.elapsedRealtime();
            accumulatedMillis += now - startTimeMillis;
            startTimeMillis = 0;
            updateNotification(formatMillis(accumulatedMillis));
        }
    }

    /**
     * 일시 중지된 스톱워치를 재개합니다.
     */
    private void resumeStopwatch() {
        if (isStopwatchRunning && isPaused) {
            isPaused = false;
            startTimeMillis = SystemClock.elapsedRealtime();
            handler.post(stopwatchRunnable);
            updateNotification(formatMillis(accumulatedMillis));
        }
    }

    /**
     * 스톱워치를 중지하고 서비스와 포그라운드 알림을 종료합니다.
     */
    private void stopStopwatch() {
        if (isStopwatchRunning) {
            showTempNotification(this, "세션이 종료되었습니다.");
            Intent intent = new Intent(BROADCAST_ACTION_STOPWATCH_STOP);
            intent.putExtra(BROADCAST_EXTRA_ACCUMULATED_TIME, accumulatedMillis);
            LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
        }

        initStopwatchState();
        stopForeground(true);
        stopSelf();
    }

    /**
     * 주기적으로 스톱워치 시간을 업데이트하는 Runnable을 시작합니다.
     */
    private void startPeriodicUpdates() {
        if (handler != null && stopwatchRunnable != null)
            handler.removeCallbacks(stopwatchRunnable);

        stopwatchRunnable = new Runnable() {
            @Override
            public void run() {
                if (!isStopwatchRunning || isPaused) return;

                long elapsedTime = calculateCurrentElapsedTime();
                updateNotification(formatMillis(elapsedTime));
                sendStopwatchUpdateBroadCast(elapsedTime);
                handler.postDelayed(this, UPDATE_INTERVAL_MS);
            }
        };
        handler.post(stopwatchRunnable);
    }

    /**
     * 현재까지 경과된 시간을 계산합니다.
     * @return 현재 경과된 시간(밀리초)입니다.
     */
    private long calculateCurrentElapsedTime() {
        if (!isStopwatchRunning) return accumulatedMillis;
        if (isPaused) return accumulatedMillis;
        long now = SystemClock.elapsedRealtime();
        return accumulatedMillis + (now - startTimeMillis);
    }

    /**
     * 스톱워치 업데이트 정보를 Broadcast로 전송합니다.
     * @param elapsedTime 경과된 시간(밀리초)입니다.
     */
    private void sendStopwatchUpdateBroadCast(long elapsedTime) {
        Intent intent = new Intent(BROADCAST_ACTION_STOPWATCH_UPDATE);
        intent.putExtra(BROADCAST_EXTRA_ELAPSED_TIME, elapsedTime);

        LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
    }

    /**
     * 스톱워치 알림을 위한 Notification Channel을 생성합니다.
     */
    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            String name = "Stopwatch Channel";
            String description = "Stopwatch Service Channel";
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
     * 스톱워치 상태 변경 알림을 위한 Notification Channel을 생성합니다.
     * @param context Notification Channel을 생성할 Context입니다.
     */
    private void createTempNotificationChannel(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationManager notificationManager = ContextCompat.getSystemService(context, NotificationManager.class);

            // Normal Channel
            String nameNormal = "Stopwatch State Change Alerts";
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
        }
    }

    /**
     * 포그라운드 서비스 알림을 생성합니다.
     * @param timeText 알림에 표시할 시간 텍스트입니다.
     * @return 생성된 Notification 객체입니다.
     */
    private Notification createNotification(String timeText) {
        Intent intent = new Intent(this, MainActivity.class);
        intent.putExtra(EXTRA_SHOW_STOPWATCH_FRAGMENT, MainActivity.TIME_FRAGMENT);
        int intentFlags = PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE;
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, intent, intentFlags);
        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle(subjectName + " 세션")
                .setContentText(timeText)
                .setSmallIcon(R.drawable.outline_timer_black_24)
                .setContentIntent(pendingIntent)
                .setOngoing(true)
                .setOnlyAlertOnce(true)
                .setContentIntent(pendingIntent);

        return builder.build();
    }

    /**
     * 임시 알림을 표시합니다 (세션 시작/종료 등).
     * @param context 알림을 표시할 Context입니다.
     * @param message 알림에 표시할 메시지입니다.
     */
    private void showTempNotification(Context context, String message) {
        createTempNotificationChannel(context);

        Intent intent = new Intent(context, MainActivity.class);
        intent.putExtra(EXTRA_SHOW_STOPWATCH_FRAGMENT, MainActivity.TIME_FRAGMENT);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        PendingIntent pendingIntent = PendingIntent.getActivity(context, 3, intent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        String channelId = TEMP_CHANNEL_ID;
        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, channelId)
                .setSmallIcon(R.drawable.outline_timer_black_24)
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
     * @param timeText 업데이트할 시간 텍스트입니다.
     */
    private void updateNotification(String timeText) {
        Notification notification = createNotification(timeText);
        NotificationManagerCompat.from(this).notify(3, notification);

    }

    /**
     * 밀리초를 HH:MM:SS 또는 MM:SS 형식의 문자열로 변환합니다.
     * @param elapsedMillis 변환할 밀리초 값입니다.
     * @return 형식화된 시간 문자열입니다.
     */
    private static String formatMillis(long elapsedMillis) {
        long seconds = elapsedMillis / 1000;
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
        if (handler != null && stopwatchRunnable != null)
            handler.removeCallbacks(stopwatchRunnable);
    }

    /**
     * 클라이언트가 서비스에 바인딩할 때 호출됩니다 (현재 사용 안 함).
     * @param intent 서비스에 바인딩하기 위한 Intent입니다.
     * @return null (바인딩을 지원하지 않음).
     */
    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}