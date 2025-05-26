package mp.gradia.time.inner.record.dialog;

import android.app.Dialog;
import android.os.Bundle;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.NumberPicker;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.widget.Toolbar;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.fragment.app.DialogFragment;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;

import com.google.android.material.appbar.AppBarLayout;
import com.google.android.material.datepicker.MaterialDatePicker;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.snackbar.SnackbarContentLayout;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.google.android.material.timepicker.MaterialTimePicker;
import com.google.android.material.timepicker.TimeFormat;

import java.text.SimpleDateFormat;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.disposables.CompositeDisposable;
import io.reactivex.rxjava3.schedulers.Schedulers;
import mp.gradia.R;
import mp.gradia.database.AppDatabase;
import mp.gradia.database.dao.StudySessionDao;
import mp.gradia.database.dao.SubjectDao;
import mp.gradia.database.entity.StudySessionEntity;
import mp.gradia.database.entity.SubjectEntity;
import mp.gradia.time.inner.viewmodel.StudySessionViewModel;
import mp.gradia.time.inner.viewmodel.StudySessionViewModelFactory;
import mp.gradia.time.inner.viewmodel.SubjectViewModel;
import mp.gradia.time.inner.viewmodel.SubjectViewModelFactory;

// 세션이 종료될 시 Full Screen Dialog를 화면에 띄움
public class SessionAddDialog extends DialogFragment {
    // CONSTANT
    // TimePicker를 업데이트할 때 사용하기 위한 상태 변수
    private static final int SESSION_START_TIME = 0;
    private static final int SESSION_END_TIME = 1;

    public static final int MODE_ADD = 0;
    public static final int MODE_EDIT = 1;
    public static final String REQUEST_KEY = "requestKey";
    // Bundle로 부터 값을 가져오기 위한 KEY 값
    public static final String KEY_SESSION_ID = "sessionID";

    public static final String KEY_SERVER_SESSION_ID = "serverSessionId";
    public static final String KEY_SESSION_MODE = "sessionMode";
    public static final String KEY_SUBJECT_ID = "subjectId";

    public static final String KEY_SERVER_SUBJECT_ID = "serverSubjectId";
    public static final String KEY_SUBJECT_NAME = "subjectName";
    public static final String KEY_SESSION_FOCUS_LEVEL = "sessionFocusLevel";
    public static final String KEY_START_HOUR = "startHour";
    public static final String KEY_START_MINUTE = "startMinute";
    public static final String KEY_END_HOUR = "endHour";
    public static final String KEY_END_MINUTE = "endMinute";
    public static final String KEY_REST_TIME = "restTime";
    public static final String KEY_START_DATE = "startDate";
    public static final String KEY_END_DATE = "endDate";
    public static final String KEY_SESSION_MEMO = "sessionMemo";

    // View Component
    private View v;
    private Toolbar toolbar;
    private TextInputLayout startTimeInputLayout;
    private TextInputLayout endTimeInputLayout;
    private TextInputLayout sessionMemoInputLayout;
    private TextInputEditText dateEditText;
    private TextInputEditText startTimeEditText;
    private TextInputEditText endTimeEditText;
    private TextInputEditText durationEditText;
    private TextInputEditText sessionMemoEditText;
    private AutoCompleteTextView dropdown;
    private Button saveSessionBtn;
    private LinearLayout sessionReviewFocusLevel1;
    private LinearLayout sessionReviewFocusLevel2;
    private LinearLayout sessionReviewFocusLevel3;
    private LinearLayout sessionReviewFocusLevel4;

    // 세션의 정보들을 저장 할 변수
    private boolean isYesterday = false;
    private final long[] selectedDateMillis = { MaterialDatePicker.todayInUtcMilliseconds(), -1 };
    private int sessionId = -1;

    private String serverSessionId;
    private int sessionMode = MODE_ADD;
    private int clockFormat;
    private int selectedStartHour = -1;
    private int selectedStartMinute = -1;
    private int selectedEndHour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY);
    private int selectedEndMinute = Calendar.getInstance().get(Calendar.MINUTE);
    private int durationHour = -1;
    private int durationMinute = -1;
    private int restTime = -1;
    private int focusLevel = -1;
    private int subjectId = -1;

    private String serverSubjectId;
    private String subjectName;
    private String sessionMemo;
    private List<String> subjects;
    private List<Integer> subjectIds;

    // 데이터베이스 객체
    private AppDatabase db;
    private SubjectDao subjectDao;
    private StudySessionDao sessionDao;

    // 뷰모델
    private SubjectViewModel subjectViewModel;
    private StudySessionViewModel sessionViewModel;

    private CompositeDisposable compositeDisposable = new CompositeDisposable();

    /**
     * 다이얼로그가 처음 생성될 때 호출됩니다. 스타일을 설정하고 데이터베이스, ViewModel, 전달받은 Bundle 데이터를 초기화합니다.
     */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setStyle(DialogFragment.STYLE_NORMAL, R.style.fullscreen_dialog);
        db = AppDatabase.getInstance(requireContext());
        subjectDao = db.subjectDao();
        sessionDao = db.studySessionDao();

        SubjectViewModelFactory subjectFactory = new SubjectViewModelFactory(subjectDao);
        subjectViewModel = new ViewModelProvider(requireParentFragment(), subjectFactory).get(SubjectViewModel.class);

        StudySessionViewModelFactory sessionFactory = new StudySessionViewModelFactory(
                requireActivity().getApplication(), sessionDao);
        sessionViewModel = new ViewModelProvider(requireParentFragment(), sessionFactory)
                .get(StudySessionViewModel.class);
        sessionViewModel.loadAllSessions();

        // getInstance를 통해 정적으로 호출된 경우, 전달받은 데이터들을 멤버변수와 초기화 해주어야 함
        Bundle args = getArguments();
        if (args != null) {
            sessionId = args.getInt(KEY_SESSION_ID);
            serverSessionId = args.getString(KEY_SERVER_SESSION_ID);
            sessionMode = args.getInt(KEY_SESSION_MODE);
            subjectId = args.getInt(KEY_SUBJECT_ID);
            serverSubjectId = args.getString(KEY_SERVER_SUBJECT_ID);
            subjectName = args.getString(KEY_SUBJECT_NAME);
            focusLevel = args.getInt(KEY_SESSION_FOCUS_LEVEL);
            selectedDateMillis[0] = args.getLong(KEY_START_DATE);
            selectedDateMillis[1] = args.getLong(KEY_END_DATE);
            selectedStartHour = args.getInt(KEY_START_HOUR);
            selectedStartMinute = args.getInt(KEY_START_MINUTE);
            selectedEndHour = args.getInt(KEY_END_HOUR);
            selectedEndMinute = args.getInt(KEY_END_MINUTE);
            restTime = args.getInt(KEY_REST_TIME);
            sessionMemo = args.getString(KEY_SESSION_MEMO);
        }
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
        v = inflater.inflate(R.layout.fragment_session_add_dialog, container, false);

        // 전체 화면과 앱 뷰의 padding 설정
        AppBarLayout appBarLayout = v.findViewById(R.id.appbar_layout);
        ViewCompat.setOnApplyWindowInsetsListener(appBarLayout, (view, windowInsets) -> {
            Insets insets = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars());
            view.setPadding(view.getPaddingLeft(), insets.top, v.getPaddingRight(), v.getPaddingBottom());
            return windowInsets;
        });

        initViews();

        return v;
    }

    /**
     * 뷰가 생성된 후 호출됩니다. 툴바, 드롭다운, DatePicker, TimePicker, DurationPicker, 메모 입력 필드, 저장 버튼을 설정합니다.
     */
    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        toolbar.setNavigationOnClickListener(v -> dismiss());

        // 뷰 설정 및 리스너 생성
        setupSessionReviewFocus();
        setupSubjectDropDown();
        setupDatePicker();
        setupTimePicker();
        setupDurationPicker();

        updateTimeEditText(clockFormat, SESSION_START_TIME);
        updateTimeEditText(clockFormat, SESSION_END_TIME);

        setupSessionMemo();
        setupSaveSession();
    }

    /**
     * 새로운 SessionAddDialog 객체를 생성하고 Bundle 데이터를 설정합니다.
     *
     * @param bundle 다이얼로그에 전달할 데이터가 담긴 Bundle 객체입니다.
     * @return 생성된 SessionAddDialog 인스턴스입니다.
     */
    public static SessionAddDialog newInstance(Bundle bundle) {
        SessionAddDialog dialog = new SessionAddDialog();
        dialog.setArguments(bundle);
        return dialog;
    }

    /**
     * UI 컴포넌트들을 초기화합니다.
     */
    private void initViews() {
        toolbar = v.findViewById(R.id.toolbar);
        if (sessionMode == MODE_EDIT) {
            toolbar.setTitle(R.string.appbar_title_edit_session);
            setupToolbarMenu();
        }
        dropdown = v.findViewById(R.id.dropdown_autocomplete);
        dateEditText = v.findViewById(R.id.date_picker_edittext);
        startTimeInputLayout = v.findViewById(R.id.start_time_picker_textinput);
        startTimeEditText = v.findViewById(R.id.start_time_picker_edittext);
        endTimeInputLayout = v.findViewById(R.id.end_time_picker_textinput);
        endTimeEditText = v.findViewById(R.id.end_time_picker_edittext);
        durationEditText = v.findViewById(R.id.duration_edittext);
        sessionMemoInputLayout = v.findViewById(R.id.session_memo_textinput);
        sessionMemoEditText = v.findViewById(R.id.session_memo_edittext);

        if (sessionMemo != null)
            sessionMemoEditText.setText(sessionMemo);

        saveSessionBtn = v.findViewById(R.id.save_session_btn);

        sessionReviewFocusLevel1 = v.findViewById(R.id.session_review_focus_level1);
        sessionReviewFocusLevel2 = v.findViewById(R.id.session_review_focus_level2);
        sessionReviewFocusLevel3 = v.findViewById(R.id.session_review_focus_level3);
        sessionReviewFocusLevel4 = v.findViewById(R.id.session_review_focus_level4);
    }

    private void setupToolbarMenu() {
        toolbar.inflateMenu(R.menu.session_add_dialog_menu);
        toolbar.setOnMenuItemClickListener(item -> {
            if (item.getItemId() == R.id.action_delete_session) {
                new MaterialAlertDialogBuilder(requireContext())
                        .setTitle("세션 삭제")
                        .setMessage("이 세션을 정말 삭제하시겠습니까?")
                        .setNegativeButton("취소", null)
                        .setPositiveButton("삭제", (dialog, which) -> deleteSession())
                        .show();
                return true;
            }
            return false;
        });
    }

    /**
     * 세션 리뷰 집중 레벨 선택 UI를 설정합니다.
     */
    private void setupSessionReviewFocus() {
        LinearLayout[] focusLevels = {
                sessionReviewFocusLevel1,
                sessionReviewFocusLevel2,
                sessionReviewFocusLevel3,
                sessionReviewFocusLevel4
        };

        if (sessionMode == MODE_EDIT && focusLevel != -1) {
            focusLevels[focusLevel].setSelected(true);
        }

        View.OnClickListener focusClickListener = v -> {
            for (int i = 0; i < focusLevels.length; i++) {
                if (v == focusLevels[i]) {
                    focusLevel = i;
                    focusLevels[i].setSelected(true);
                } else {
                    focusLevels[i].setSelected(false);
                }
            }
        };

        for (LinearLayout levelView : focusLevels) {
            levelView.setOnClickListener(focusClickListener);
        }
    }

    /**
     * 과목 드롭다운 메뉴를 설정하고 ViewModel을 통해 데이터를 로드합니다.
     */
    private void setupSubjectDropDown() {
        subjects = new ArrayList<>();
        subjectIds = new ArrayList<>();

        ArrayAdapter<String> adapter = new ArrayAdapter<>(requireContext(), R.layout.list_item, subjects);
        dropdown.setAdapter(adapter);

        if (subjectId != -1)
            subjectViewModel.loadSubjectById(subjectId);

        subjectViewModel.subjectListLiveData.observe(getViewLifecycleOwner(),
                subjectList -> {
                    subjects.clear();
                    for (SubjectEntity subject : subjectList) {
                        subjects.add(subject.getName());
                        subjectIds.add(subject.getSubjectId());
                    }
                    adapter.notifyDataSetChanged();
                });

        subjectViewModel.selectedSubjectLiveData.observe(getViewLifecycleOwner(),
                selectedSubject -> {
                    if (selectedSubject != null) {
                        subjectId = selectedSubject.getSubjectId();
                        serverSubjectId = selectedSubject.getServerId();
                        subjectName = selectedSubject.getName();
                        dropdown.setText(selectedSubject.getName(), false);
                    }
                });

        // 선택된 과목의 id 를 가져옴
        dropdown.setOnItemClickListener((parent, view, position, id) -> {
            subjectId = subjectIds.get(position);
            subjectName = subjects.get(position);
            serverSubjectId = subjectViewModel.subjectListLiveData.getValue().get(position).getServerId();
        });
    }

    /**
     * DatePicker를 설정하고 선택 리스너를 추가합니다.
     */
    private void setupDatePicker() {
        updateDateEditText(selectedDateMillis);
        dateEditText.setOnClickListener(v -> {
            MaterialDatePicker.Builder<Long> builder = MaterialDatePicker.Builder.datePicker();
            builder.setTitleText("날짜 선택");
            builder.setSelection(selectedDateMillis[0]);

            final MaterialDatePicker<Long> datePicker = builder.build();
            datePicker.addOnPositiveButtonClickListener(selection -> {
                selectedDateMillis[0] = selection;
                if (selectedDateMillis[1] != -1 && isYesterday) {
                    selectedDateMillis[1] = selectedDateMillis[0] + Duration.ofDays(1).toMillis();
                }

                updateDateEditText(selectedDateMillis);
                checkValidation();
            });
            datePicker.show(getParentFragmentManager(), datePicker.toString());
        });
    }

    /**
     * 선택된 날짜 (UTC 밀리초)를 "yyyy-MM-dd" 형식의 문자열로 변환하여 DateEditText에 업데이트합니다.
     * @param utcMillis 선택된 날짜의 UTC 밀리초 배열입니다.
     */
    private void updateDateEditText(long[] utcMillis) {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        String formattedDate = sdf.format(new Date(utcMillis[0]));

        if (utcMillis[1] != -1 && isYesterday == true)
            formattedDate += " → " + sdf.format(new Date(utcMillis[1]));

        if (dateEditText != null) {
            dateEditText.setText(formattedDate);
            updateDurationEditText();
        }
    }

    /**
     * TimePicker를 설정하고 선택 리스너를 추가합니다 (시작 시간, 종료 시간).
     */
    private void setupTimePicker() {
        boolean isSystem24Hour = android.text.format.DateFormat.is24HourFormat(requireContext());
        clockFormat = isSystem24Hour ? TimeFormat.CLOCK_24H : TimeFormat.CLOCK_12H;
        updateTimeEditText(clockFormat, SESSION_END_TIME);

        // 세션의 시작 시간을 선택하는 TimePicker
        startTimeEditText.setOnClickListener(v -> {
            showStartTimePicker();
        });

        // 세션의 종료 시간을 선택하는 TimePicker
        endTimeEditText.setOnClickListener(v -> {
            showEndTimePicker();
        });
    }

    /**
     * 시작 시간을 선택하는 MaterialTimePicker를 표시합니다.
     */
    private void showStartTimePicker() {
        int hour = (selectedStartHour != -1 && selectedStartMinute != -1) ? selectedStartHour : selectedEndHour;
        int minute = (selectedStartHour != -1 && selectedStartMinute != -1) ? selectedStartMinute : selectedEndMinute;

        MaterialTimePicker picker = new MaterialTimePicker.Builder()
                .setTimeFormat(clockFormat)
                .setHour(hour)
                .setMinute(minute)
                .setTitleText("시작 시간 선택")
                .setPositiveButtonText("다음")
                .build();

        picker.addOnPositiveButtonClickListener(dialog -> {
            selectedStartHour = picker.getHour();
            selectedStartMinute = picker.getMinute();
            showEndTimePicker();
        });

        picker.show(getParentFragmentManager(), picker.toString());
    }

    /**
     * 종료 시간을 선택하는 MaterialTimePicker를 표시합니다.
     */
    private void showEndTimePicker() {
        MaterialTimePicker picker = new MaterialTimePicker.Builder()
                .setTimeFormat(clockFormat)
                .setHour(selectedEndHour)
                .setMinute(selectedEndMinute)
                .setTitleText("종료 시간 선택")
                .setNegativeButtonText("이전")
                .build();

        picker.addOnPositiveButtonClickListener(dialog -> {
            selectedEndHour = picker.getHour();
            selectedEndMinute = picker.getMinute();
            updateTimeEditText(clockFormat, SESSION_START_TIME);
            updateTimeEditText(clockFormat, SESSION_END_TIME);
            checkValidation();
        });

        picker.addOnNegativeButtonClickListener(dialog -> {
            selectedEndHour = picker.getHour();
            selectedEndMinute = picker.getMinute();
            showStartTimePicker();
        });

        picker.show(getParentFragmentManager(), picker.toString());
    }

    /**
     * 선택된 시간을 지정된 형식의 문자열로 변환하여 해당 TimeEditText에 업데이트합니다.
     *
     * @param clockFormat 시간 형식 (12시간 또는 24시간).
     * @param type        업데이트할 시간 유형 (시작 시간 또는 종료 시간).
     */
    private void updateTimeEditText(int clockFormat, int type) {
        String formattedTime = "null";
        int hour = selectedEndHour;
        int minute = selectedEndMinute;
        TextInputEditText timeEditText = endTimeEditText;

        if (type == SESSION_START_TIME && selectedStartHour != -1 && selectedStartMinute != -1) {
            hour = selectedStartHour;
            minute = selectedStartMinute;
            timeEditText = startTimeEditText;
        }

        switch (clockFormat) {
            case TimeFormat.CLOCK_12H:
                Calendar tempCal = Calendar.getInstance();
                tempCal.set(Calendar.HOUR_OF_DAY, hour);
                tempCal.set(Calendar.MINUTE, minute);
                SimpleDateFormat sdf = new SimpleDateFormat("a hh:mm", Locale.getDefault());
                formattedTime = sdf.format(tempCal.getTime());
                break;
            case TimeFormat.CLOCK_24H:
                formattedTime = String.format(Locale.getDefault(), "%02d:%02d", hour, minute);
                break;
        }

        if (timeEditText != null) {
            timeEditText.setText(formattedTime);
            updateDurationEditText();
        }
    }

    /**
     * 세션의 기간(종료 시간 - 시작 시간)을 계산하여 DurationEditText에 업데이트합니다.
     */
    private void updateDurationEditText() {
        if (selectedStartHour != -1 && selectedStartMinute != -1) {
            int startTotalMinutes = selectedStartHour * 60 + selectedStartMinute;
            int endTotalMinutes = selectedEndHour * 60 + selectedEndMinute;

            int diffMinutes = endTotalMinutes - startTotalMinutes;

            if (diffMinutes < 0) {
                diffMinutes += 24 * 60;
                if (!isYesterday) {
                    if (LocalDate.now().equals(getDate(selectedDateMillis[0]))) {
                        selectedDateMillis[1] = selectedDateMillis[0];
                        selectedDateMillis[0] -= Duration.ofDays(1).toMillis();
                    } else {
                        selectedDateMillis[1] = selectedDateMillis[0] + Duration.ofDays(1).toMillis();
                    }

                    isYesterday = true;
                    updateDateEditText(selectedDateMillis);
                }
            } else if (isYesterday && diffMinutes >= 0) {
                isYesterday = false;
                selectedDateMillis[0] += Duration.ofDays(1).toMillis();
                updateDateEditText(selectedDateMillis);
            } else {
                selectedDateMillis[1] = -1;
            }

            durationHour = diffMinutes / 60;
            durationMinute = diffMinutes % 60;

            String durationText = String.format(Locale.getDefault(), "%02d시간 %02d분", durationHour, durationMinute);
            if (durationEditText != null) durationEditText.setText(durationText);
        }
    }

    /**
     * Duration EditText 클릭 리스너를 설정하여 커스텀 Duration Picker 다이얼로그를 표시합니다.
     */
    private void setupDurationPicker() {
        durationEditText.setOnClickListener(v -> {
            // 시작 시간이 설정 되어 있을때에만 다이얼로그를 호출할 수 있도록 함
            if (selectedStartHour != -1 && selectedStartMinute != -1)
                showDurationPicker();
        });
    }

    /**
     * 세션 메모 입력 필드의 포커스 변경 리스너를 설정하여 힌트 텍스트를 변경합니다.
     */
    private void setupSessionMemo() {
        sessionMemoEditText.setOnFocusChangeListener(
                (v, hasFocus) -> {
                    if (hasFocus)
                        sessionMemoInputLayout.setHint(R.string.session_memo);
                    else
                        sessionMemoInputLayout.setHint(R.string.session_memo_hint);
                });
    }

    /**
     * 시간 및 분 선택을 위한 커스텀 Duration Picker 다이얼로그를 표시합니다.
     */
    private void showDurationPicker() {
        LayoutInflater inflater = requireActivity().getLayoutInflater();
        View dialogView = inflater.inflate(R.layout.dialog_duration_picker, null);

        NumberPicker hourPicker = dialogView.findViewById(R.id.time_hour_picker);
        NumberPicker minutePicker = dialogView.findViewById(R.id.time_minute_picker);

        hourPicker.setMinValue(0);
        hourPicker.setMaxValue(23);

        minutePicker.setMinValue(0);
        minutePicker.setMaxValue(59);

        if (durationHour != -1 && durationMinute != -1) {
            hourPicker.setValue(durationHour);
            minutePicker.setValue(durationMinute);
        } else {
            hourPicker.setValue(0);
            minutePicker.setValue(25);
        }

        MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(requireContext());
        builder.setView(dialogView)
                .setTitle("기간 선택")
                .setPositiveButton("확인", (dialog, id) -> {
                    durationHour = hourPicker.getValue();
                    durationMinute = minutePicker.getValue();
                    selectedEndHour = selectedStartHour + durationHour;
                    selectedEndMinute = selectedStartMinute + durationMinute;
                    updateTimeEditText(clockFormat, SESSION_END_TIME);
                    updateDurationEditText();
                })
                .setNegativeButton("취소", (dialog, id) -> dialog.cancel());
        builder.create().show();
    }

    /**
     * 중복되는 세션이 있을 경우 Snackbar를 표시합니다.
     *
     * @param overlappingSession 중복되는 StudySessionEntity 객체입니다. null이면 중복이 없음을
     *                           의미합니다.
     *
     * @param overlappingSession 중복되는 StudySessionEntity 객체입니다. null이면 중복이 없음을 의미합니다.
     */
    private void showOverlappingSesison(StudySessionEntity overlappingSession) {
        if (overlappingSession == null) {
            startTimeInputLayout.setError(null);
            endTimeInputLayout.setError(null);
            return;
        }

        int overlappingSubjectId = overlappingSession.getSubjectId() - 1;
        LocalDate overlappingDate = overlappingSession.getDate();
        LocalTime overlappingStartTime = overlappingSession.getStartTime();
        LocalTime overlappingEndTime = overlappingSession.getEndTime();
        String subjectName = subjects.get(overlappingSubjectId);
        String errorMsg = "중복되는 세션입니다.\n" + subjectName + " " + overlappingDate + " " + overlappingStartTime + " - "
                + overlappingEndTime;
        Snackbar errorSnackBar = Snackbar.make(v, errorMsg, Snackbar.LENGTH_INDEFINITE).setAnchorView(saveSessionBtn);
        errorSnackBar.setAction("확인", v -> errorSnackBar.dismiss());
        errorSnackBar.show();

        startTimeInputLayout.setError("잘못된 시간입니다.");
        endTimeInputLayout.setError("잘못된 시간입니다.");
        startTimeInputLayout.setErrorIconOnClickListener(
                v -> {
                    errorSnackBar.setAction("수정", view -> showStartTimePicker());
                    errorSnackBar.show();
                });
        endTimeInputLayout.setErrorIconOnClickListener(
                v -> {
                    errorSnackBar.setAction("수정", view -> showStartTimePicker());
                    errorSnackBar.show();
                });
    }

    /**
     * 시간 유효성 검사 콜백 인터페이스입니다.
     */
    public interface TimeValidationCallback {
        /**
         * 시간 유효성 검사 결과가 반환될 때 호출됩니다.
         * @param isValid 시간이 유효한지 여부입니다.
         * @param overlappingSession 중복되는 세션이 있을 경우 해당 세션 객체입니다.
         */
        void onValidationResult(boolean isValid, @Nullable StudySessionEntity overlappingSession);
    }

    /**
     * 세션 저장 버튼 클릭 리스너를 설정하여 세션 정보를 데이터베이스에 저장하고 다이얼로그를 닫습니다.
     */
    private void setupSaveSession() {
        saveSessionBtn.setOnClickListener(v -> {
            if (selectedStartHour == -1 || selectedStartMinute == -1) {
                Toast.makeText(requireContext(), R.string.toast_message_invalid_time, Toast.LENGTH_SHORT).show();
                return;
            }

            LocalDate date = getDate(selectedDateMillis[0]);
            LocalDate endDate = (selectedDateMillis[1] == -1) ? date : getDate(selectedDateMillis[1]);
            LocalTime startTime = LocalTime.of(selectedStartHour, selectedStartMinute);
            LocalTime endTime = LocalTime.of(selectedEndHour, selectedEndMinute);
            long minutes = (long) durationHour * 60 + durationMinute;
            sessionMemo = sessionMemoEditText.getText().toString();

            Bundle result = new Bundle();

            String log = "SubjectId : " + String.valueOf(subjectId) + "\n" + "serverSubjectId: " + serverSubjectId + "\n" + "SubjectName : " + subjectName + "\n"
                    + "Date : " + date.toString() + "\n" + "endDate : " + "\n" + "minutes : " + minutes + "\n"
                    + "StartTime : " + startTime.toString() + "\n" + "EndTime : " + endTime.toString() + "\n"
                    + "Memo : " + sessionMemo + "\n";
            Log.i("SessionAddDialog", log);

            validateTimeSlot(date, endDate, startTime, endTime, (isValid, overlappingSession) -> {
                if (isValid) {
                    showOverlappingSesison(null);
                    StudySessionEntity session = new StudySessionEntity(subjectId, serverSubjectId, subjectName, date, endDate, minutes,
                            startTime, endTime, 0, focusLevel, sessionMemo);

                    if (sessionMode == MODE_EDIT) {
                        session.setSessionId(sessionId);
                        session.setServerId(serverSessionId);
                        Log.d("SessionAddDialog", "Updating serverSessionId: " + serverSessionId);
                        sessionViewModel.updateSession(session);
                        result.putInt(KEY_SESSION_MODE, MODE_EDIT);
                    } else {
                        sessionViewModel.saveSession(session);
                        result.putInt(KEY_SESSION_MODE, MODE_EDIT);
                        String log2 = "SubjectId : " + String.valueOf(subjectId) + "\n" + "SubjectName : " + subjectName
                                + "\n" + "Date : " + date.toString() + "\n" + "endDate : " + "\n" + "minutes : "
                                + minutes + "\n" + "StartTime : " + startTime.toString() + "\n" + "EndTime : "
                                + endTime.toString() + "\n" + "Memo : " + sessionMemo + "\n";
                        Log.i("SessionAddDialog", log2);
                    }

                    Toast.makeText(requireContext(), R.string.toast_message_session_saved, Toast.LENGTH_SHORT).show();
                    getParentFragmentManager().setFragmentResult(REQUEST_KEY, result);
                    dismiss();
                } else {
                    showOverlappingSesison(overlappingSession);
                }
            });
        });
    }

    /**
     * 시간 유효성 검사를 수행하고 결과를 표시합니다.
     */
    private void checkValidation() {
        if (selectedStartHour != -1 && selectedStartMinute != -1) {
            LocalDate date = getDate(selectedDateMillis[0]);
            LocalDate endDate = (selectedDateMillis[1] != -1) ? getDate(selectedDateMillis[1]) : null;
            LocalTime startTime = LocalTime.of(selectedStartHour, selectedStartMinute);
            LocalTime endTime = LocalTime.of(selectedEndHour, selectedEndMinute);

            validateTimeSlot(date, endDate, startTime, endTime, (isValid, overlappingSession) -> {
                if (isValid)
                    showOverlappingSesison(null);
                else {
                    showOverlappingSesison(overlappingSession);
                }
            });
        }
    }

    /**
     * 주어진 시간 슬롯이 기존 세션과 겹치는지 유효성 검사를 수행합니다.
     *
     * @param date     새 세션의 날짜입니다.
     * @param endDate  새 세션의 종료 날짜입니다 (선택 사항).
     * @param start    새 세션의 시작 시간입니다.
     * @param end      새 세션의 종료 시간입니다.
     * @param callback 유효성 검사 결과를 반환할 콜백입니다.
     */
    private void validateTimeSlot(LocalDate date, @Nullable LocalDate endDate, LocalTime start, LocalTime end,
            TimeValidationCallback callback) {
        if (date == null || start == null || end == null || callback == null) {
            if (callback != null)
                callback.onValidationResult(false, null);
            return;
        }

        LocalDateTime newSessionStartDateTime = LocalDateTime.of(date, start);
        LocalDateTime newSessionEndDateTime;

        // 세션 시작 날짜보다 뒤인 경우
        if (endDate != null && endDate.isAfter(date))
            newSessionEndDateTime = LocalDateTime.of(endDate, end);
        // 세션 종료 시간이 세션 시작 시간보다 앞서 있을 때 (즉, 자정을 넘어갈때)
        else if (end.isBefore(start))
            newSessionEndDateTime = LocalDateTime.of(date.plusDays(1), end);
        // 일반적인 상황(시작과 종료의 날짜가 같음)
        else
            newSessionEndDateTime = LocalDateTime.of(date, end);

        // endDate가 startDate보다 빠른 경우 false
        if (!newSessionEndDateTime.isAfter(newSessionStartDateTime)) {
            callback.onValidationResult(false, null);
            return;
        }

        Observer<List<StudySessionEntity>> observer = new Observer<List<StudySessionEntity>>() {
            @Override
            public void onChanged(List<StudySessionEntity> allSessions) {
                sessionViewModel.sessionListLiveData.removeObserver(this);

                if (allSessions == null) {
                    callback.onValidationResult(true, null);
                    return;
                }

                for (StudySessionEntity existingSession : allSessions) {
                    if (sessionId == existingSession.getSessionId())
                        continue;

                    LocalDateTime existingSessionStartDateTime = LocalDateTime.of(existingSession.getDate(),
                            existingSession.getStartTime());
                    LocalDateTime existingSessionEndDateTime;
                    LocalDate existingSessionEndDate = existingSession.getEndDate();

                    // 가져온 세션의 데이터가 endDate를 가지고 있을 경우
                    if (existingSessionEndDate != null && existingSessionEndDate.isAfter(existingSession.getDate()))
                        existingSessionEndDateTime = LocalDateTime.of(existingSessionEndDate,
                                existingSession.getEndTime());
                    // 세션의 endTime이 startTime보다 앞설 경우
                    else if (existingSession.getEndTime().isBefore(existingSession.getStartTime()))
                        existingSessionEndDateTime = LocalDateTime.of(existingSession.getDate().plusDays(1),
                                existingSession.getEndTime());
                    // 일반적인 상황
                    else
                        existingSessionEndDateTime = LocalDateTime.of(existingSession.getDate(),
                                existingSession.getEndTime());

                    // endDate가 startDate보다 빠른 경우
                    if (!existingSessionEndDateTime.isAfter(existingSessionStartDateTime))
                        continue;

                    boolean isOverlap = newSessionStartDateTime.isBefore(existingSessionEndDateTime) &&
                            existingSessionStartDateTime.isBefore(newSessionEndDateTime);

                    if (isOverlap) {
                        callback.onValidationResult(false, existingSession);
                        return;
                    }
                }
                callback.onValidationResult(true, null);
            }
        };
        sessionViewModel.sessionListLiveData.observe(getViewLifecycleOwner(), observer);
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
     * 다이얼로그가 소멸될 때 호출됩니다. RxJava Disposables를 해제합니다.
     */
    @Override
    public void onDestroy() {
        super.onDestroy();
        compositeDisposable.clear();
    }
}