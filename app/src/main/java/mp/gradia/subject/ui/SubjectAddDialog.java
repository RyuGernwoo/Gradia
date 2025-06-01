package mp.gradia.subject.ui;

import android.app.Dialog;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.Navigation;

import com.github.dhaval2404.colorpicker.ColorPickerDialog;
import com.github.dhaval2404.colorpicker.listener.ColorListener;
import com.github.dhaval2404.colorpicker.model.ColorShape;
import com.google.android.material.appbar.AppBarLayout;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.datepicker.MaterialDatePicker;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.google.android.material.transition.MaterialFadeThrough;
import com.google.android.material.transition.platform.MaterialFade;

import org.jetbrains.annotations.NotNull;

import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.util.Date;
import java.util.Locale;
import java.util.Random;

import mp.gradia.R;
import mp.gradia.database.entity.EvaluationRatio;
import mp.gradia.database.entity.SubjectEntity;
import mp.gradia.database.entity.TargetStudyTime;
import mp.gradia.database.repository.SubjectRepository;
import mp.gradia.subject.viewmodel.SubjectViewModel;
import mp.gradia.time.inner.record.dialog.SessionAddDialog;

// 과목 추가 및 수정 화면 fragment

public class SubjectAddDialog extends DialogFragment {

    private static final String TAG = "SubjectAddFragment";
    private static final int MID_TERM = 0;
    private static final int FINAL_TERM = 1;
    private static final int SUBJECT_TYPE_REQUIRED = 0;
    private static final int SUBJECT_TYPE_ELECTIVE = 1;
    private static final int SUBJECT_TYPE_LIBERAL_ARTS = 2;
    private static final long DIALOG_TRANSITION_DURATION = 300L;
    // View Component
    private View v;
    private Toolbar toolbar;
    private SubjectViewModel viewModel;
    private LinearLayout subjectColorPicker;
    private TextView subjectColorCode;
    private ChipGroup subjectTypeChipGroup;
    private ChipGroup subjectCreditChipGroup;
    private ChipGroup subjectDifficultyChipGroup;

    private TextInputLayout subjectNameInputLayout;
    private TextInputEditText subjectNameEditText;
    private TextInputEditText dailyTargetStudyTimeEditText;
    private TextInputEditText weeklyTargetStudyTimeEditText;
    private TextInputEditText monthlyTargetStudyTimeEditText;
    private TextInputEditText midTermScheduleEditText;
    private TextInputEditText finalTermScheduleEditText;
    private TextInputEditText midTermRatioEditText;
    private TextInputEditText finalTermRatioEditText;
    private TextInputEditText quizRatioEditText;
    private TextInputEditText assignmentRatioEditText;
    private TextInputEditText attendanceRatioEditText;
    private int editingId = -1;
    private int credit = -1;
    private int difficulty = -1;
    private int type = -1;
    private int subjectColor = -1;
    private LocalDate midTerm = null;
    private LocalDate finalTerm = null;
    private SubjectEntity currentEditingSubject = null;
    private Random random = new Random();

    public SubjectAddDialog() {
    }

    public static SubjectAddDialog newInstance(Bundle bundle) {
        SubjectAddDialog dialog = new SubjectAddDialog();
        dialog.setArguments(bundle);
        return dialog;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setStyle(DialogFragment.STYLE_NORMAL, R.style.fullscreen_dialog);
        viewModel = new ViewModelProvider(this).get(SubjectViewModel.class);

        // 수정 모드인지 확인(수정모드일 때 id가져옴)
        if (getArguments() != null) {
            editingId = getArguments().getInt("subjectId", -1);
        }
    }

    @Override
    public void onStart() {
        super.onStart();
        Dialog dialog = getDialog();
        if (dialog != null) {
            Window window = dialog.getWindow();
            if (window != null) {
                window.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
                // Material 3 Motion 추가(작동 안함)
                MaterialFade enterFade = new MaterialFade();
                enterFade.setDuration(DIALOG_TRANSITION_DURATION);
                MaterialFadeThrough transition = new MaterialFadeThrough();
                window.setEnterTransition(enterFade);

                MaterialFade exitFade = new MaterialFade();
                exitFade.setDuration(DIALOG_TRANSITION_DURATION);
                window.setExitTransition(exitFade);
            }
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        v = inflater.inflate(R.layout.fragment_subject_add_dialog, container, false);
        if (v instanceof ViewGroup) {
            ((ViewGroup) v).setTransitionGroup(true); // API 21+
        }

        // 전체 화면과 앱 뷰의 padding 설정
        AppBarLayout appBarLayout = v.findViewById(R.id.appbar_layout);
        ViewCompat.setOnApplyWindowInsetsListener(appBarLayout, (view, windowInsets) -> {
            Insets insets = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars());
            view.setPadding(view.getPaddingLeft(), insets.top, v.getPaddingRight(), v.getPaddingBottom());
            return windowInsets;
        });

        loadData();
        initViews();
        return v;
    }

    // UI 초기화 및 버튼 동작 설정
    @Override
    public void onViewCreated(@NonNull View v, @Nullable Bundle s) {
        super.onViewCreated(v, s);
        toolbar.setNavigationOnClickListener(view -> dismiss());

        setupToolbar();
        setupColorPicker();
        setupTypeChips();
        setupCreditChips();
        setupDifficultyChips();
        setupDatePicker();
    }

    private void loadData() {
        if (editingId != -1) {
            Log.d(TAG, "수정 모드 진입. Subject ID: " + editingId);
            viewModel.getSubjectById(editingId).observe(getViewLifecycleOwner(), subject -> {
                if (subject != null) {
                    currentEditingSubject = subject; // 수정할 과목 정보 저장
                    fillForm(subject);
                } else {
                    Log.w(TAG, "Subject with ID " + editingId + " not found.");
                    Toast.makeText(getContext(), "수정할 과목 정보를 찾을 수 없습니다.", Toast.LENGTH_SHORT).show();
                    Navigation.findNavController(v).popBackStack();

                }
            });
        } else {
            Log.d(TAG, "새 과목 추가 모드 진입.");
            currentEditingSubject = null; // 새 과목 추가 모드에서는 null로 초기화
        }
    }

    private void initViews() {
        toolbar = v.findViewById(R.id.toolbar);
        if (editingId != -1)
            toolbar.setTitle("과목 수정");
        subjectColorPicker = v.findViewById(R.id.color_picker_rect);
        subjectColorCode = v.findViewById(R.id.color_code);
        subjectTypeChipGroup = v.findViewById(R.id.subject_type_chip);
        subjectCreditChipGroup = v.findViewById(R.id.subject_credit_chip);
        subjectDifficultyChipGroup = v.findViewById(R.id.subject_difficulty_chip);
        subjectNameInputLayout = v.findViewById(R.id.subject_textinput);
        subjectNameEditText = v.findViewById(R.id.subject_edittext);
        dailyTargetStudyTimeEditText = v.findViewById(R.id.daily_target_study_time_edittext);
        weeklyTargetStudyTimeEditText = v.findViewById(R.id.weekly_target_study_time_edittext);
        monthlyTargetStudyTimeEditText = v.findViewById(R.id.monthly_target_study_time_edittext);
        midTermScheduleEditText = v.findViewById(R.id.mid_term_edittext);
        finalTermScheduleEditText = v.findViewById(R.id.final_term_edittext);
        midTermRatioEditText = v.findViewById(R.id.mid_term_ratio_edittext);
        finalTermRatioEditText = v.findViewById(R.id.final_term_ratio_edittext);
        quizRatioEditText = v.findViewById(R.id.quiz_ratio_edittext);
        assignmentRatioEditText = v.findViewById(R.id.assignment_ratio_edittext);
        attendanceRatioEditText = v.findViewById(R.id.attendance_ratio_edittext);
    }

    private void setupToolbar() {
        toolbar.inflateMenu(R.menu.subject_add_dialog_menu);
        toolbar.setOnMenuItemClickListener(item -> {
                    if (item.getItemId() == R.id.action_save_subject) {
                        saveSubject();
                        return true;
                    }
                    return false;
                }
        );
    }

    private void setupColorPicker() {
        if (subjectColor == -1) {
            subjectColor = Color.parseColor(generateRandomHexColor());
            subjectColorCode.setText(String.format("#%06X", (0xFFFFFF & subjectColor)));
            setSubjectColor(subjectColor);
        }

        subjectColorPicker.setOnClickListener(view -> {
            new ColorPickerDialog
                    .Builder(requireContext())
                    .setTitle("과목 색상 선택")
                    .setColorShape(ColorShape.SQAURE)
                    .setDefaultColor(subjectColor)
                    .setColorListener(new ColorListener() {
                        @Override
                        public void onColorSelected(int color, @NotNull String colorHex) {
                            subjectColor = color;
                            setSubjectColor(color);
                        }
                    })
                    .show();
        });
    }

    private void setSubjectColor(int color) {
        Drawable baseDrawable = ContextCompat.getDrawable(requireContext(), R.drawable.color_picker_rectangle);
        if (baseDrawable instanceof GradientDrawable) {
            ((GradientDrawable) baseDrawable).setColor(color);
            subjectColorPicker.setBackground(baseDrawable);
        }
        subjectColorCode.setText(String.format("#%06X", (0xFFFFFF & color)));
    }

    private void setupTypeChips() {
        subjectTypeChipGroup.check(R.id.subject_type_required);
        type = SUBJECT_TYPE_REQUIRED;
        subjectTypeChipGroup.setOnCheckedStateChangeListener((group, checkedIds) -> {
            if (!checkedIds.isEmpty()) {
                int selectedChipId = checkedIds.get(0);
                Chip selectedChip = group.findViewById(selectedChipId);
                String chipText = selectedChip.getText().toString();
                Log.d("Chip", chipText);
                switch (chipText) {
                    case "전공필수":
                        type = SUBJECT_TYPE_REQUIRED;
                        break;
                    case "전공선택":
                        type = SUBJECT_TYPE_ELECTIVE;
                        break;
                    case "교양":
                        type = SUBJECT_TYPE_LIBERAL_ARTS;
                        break;
                }
            }
        });
    }

    private void setupCreditChips() {
        subjectCreditChipGroup.check(R.id.subject_credit_1);
        credit = 1;
        subjectCreditChipGroup.setOnCheckedStateChangeListener((group, checkedId) -> {
            if (!checkedId.isEmpty()) {
                int selectedChipId = checkedId.get(0);
                Chip selectedChip = group.findViewById(selectedChipId);
                String chipText = selectedChip.getText().toString();

                switch (chipText) {
                    case "1 학점":
                        credit = 1;
                        break;
                    case "2 학점":
                        credit = 2;
                        break;
                    case "3 학점":
                        credit = 3;
                        break;
                }
            }
        });
    }

    private void setupDifficultyChips() {
        subjectDifficultyChipGroup.check(R.id.subject_difficulty_1);
        difficulty = 1;
        subjectDifficultyChipGroup.setOnCheckedStateChangeListener((group, checkedId) -> {
            if (!checkedId.isEmpty()) {
                int selectedChipId = checkedId.get(0);
                Chip selectedChip = group.findViewById(selectedChipId);
                String chipText = selectedChip.getText().toString();
                switch (chipText) {
                    case "잘 모르겠음":
                        difficulty = -1;
                        break;
                    case "매우 쉬움":
                        difficulty = 0;
                        break;
                    case "조금 쉬움":
                        difficulty = 1;
                        break;
                    case "보통":
                        difficulty = 2;
                        break;
                    case "조금 어려움":
                        difficulty = 3;
                        break;
                    case "매우 어려움":
                        difficulty = 4;
                        break;
                }
            }
        });
    }


    private void setupDatePicker() {
        midTermScheduleEditText.setOnClickListener(v -> {
            showDatePicker(MID_TERM);
        });

        finalTermScheduleEditText.setOnClickListener(v -> {
            showDatePicker(FINAL_TERM);
        });
    }

    private void showDatePicker(int mode) {
        MaterialDatePicker.Builder<Long> builder = MaterialDatePicker.Builder.datePicker();
        builder.setSelection(MaterialDatePicker.todayInUtcMilliseconds());

        if (mode == MID_TERM) {
            builder.setTitleText("중간고사 일정 선택");
        } else if (mode == FINAL_TERM) {
            builder.setTitleText("기말고사 일정 선택");
        }

        final MaterialDatePicker<Long> datePicker = builder.build();
        datePicker.addOnPositiveButtonClickListener(date -> {
            if (mode == MID_TERM) {
                String formattedDate = dateToString(date);
                midTerm = LocalDate.parse(formattedDate);
                midTermScheduleEditText.setText(formattedDate);
            } else if (mode == FINAL_TERM) {
                String formattedDate = dateToString(date);
                finalTerm = LocalDate.parse(formattedDate);
                finalTermScheduleEditText.setText(formattedDate);
            }
        });
        datePicker.show(getParentFragmentManager(), datePicker.toString());
    }

    private String dateToString(long utcMillis) {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        String formattedDate = sdf.format(new Date(utcMillis));
        return formattedDate;
    }

    private void setTypeChips(int type) {
        this.type = type;
        switch (type) {
            case SUBJECT_TYPE_REQUIRED:
                subjectTypeChipGroup.check(R.id.subject_type_required);
                break;
            case SUBJECT_TYPE_ELECTIVE:
                subjectTypeChipGroup.check(R.id.subject_type_elective);
                break;
            case SUBJECT_TYPE_LIBERAL_ARTS:
                subjectTypeChipGroup.check(R.id.subject_type_liberal_arts);
                break;
        }
    }

    private void setCreditChips(int credit) {
        this.credit = credit;
        switch (credit) {
            case 1:
                subjectCreditChipGroup.check(R.id.subject_credit_1);
                break;
            case 2:
                subjectCreditChipGroup.check(R.id.subject_credit_2);
                break;
            case 3:
                subjectCreditChipGroup.check(R.id.subject_credit_3);
                break;
        }
    }

    private void setDifficultyChips(int difficulty) {
        this.difficulty = difficulty;
        switch (difficulty) {
            case 0:
                subjectDifficultyChipGroup.check(R.id.subject_difficulty_1);
                break;
            case 1:
                subjectDifficultyChipGroup.check(R.id.subject_difficulty_2);
                break;
            case 2:
                subjectDifficultyChipGroup.check(R.id.subject_difficulty_3);
                break;
            case 3:
                subjectDifficultyChipGroup.check(R.id.subject_difficulty_4);
                break;
            case 4:
                subjectDifficultyChipGroup.check(R.id.subject_difficulty_5);
                break;
        }
    }

    // 기존 데이터로 입력 필드 채우기
    private void fillForm(SubjectEntity subject) {
        if (subject == null) return;
        Log.d(TAG, "fillForm 호출. 과목명: " + subject.getName() + ", 색상: " + subject.getColor());

        subjectNameEditText.setText(subject.getName());
        subjectColor = Color.parseColor(subject.getColor());
        setSubjectColor(subjectColor);
        setTypeChips(subject.getType());
        setCreditChips(subject.getCredit());

        if (subject.getDifficulty() != null && subject.getDifficulty() != -1)
            setDifficultyChips(subject.getDifficulty().intValue());
        if (subject.getMidTermSchedule() != null && !subject.getMidTermSchedule().isEmpty()) {
            midTerm = LocalDate.parse(subject.getMidTermSchedule());
            midTermScheduleEditText.setText(subject.getMidTermSchedule());
        }
        if (subject.getFinalTermSchedule() != null && !subject.getFinalTermSchedule().isEmpty()) {
            finalTerm = LocalDate.parse(subject.getFinalTermSchedule());
            finalTermScheduleEditText.setText(subject.getFinalTermSchedule());
        }

        // subject.ratio가 null일 수 있음
        EvaluationRatio ratio = subject.getRatio();
        if (ratio != null) {
            midTermRatioEditText.setText(String.valueOf(ratio.getMidTermRatio()));
            finalTermRatioEditText.setText(String.valueOf(ratio.getFinalTermRatio()));
            quizRatioEditText.setText(String.valueOf(ratio.getQuizRatio()));
            assignmentRatioEditText.setText(String.valueOf(ratio.getAssignmentRatio()));
            attendanceRatioEditText.setText(String.valueOf(ratio.getAttendanceRatio()));
        }

        // subject.time도 null일 수 있음
        TargetStudyTime time = subject.getTime();
        if (time != null) {
            dailyTargetStudyTimeEditText.setText(String.valueOf(time.getDailyTargetStudyTime()));
            weeklyTargetStudyTimeEditText.setText(String.valueOf(time.getWeeklyTargetStudyTime()));
            monthlyTargetStudyTimeEditText.setText(String.valueOf(time.getMonthlyTargetStudyTime()));
        }
    }

    private int parse(TextInputEditText e) {
        try {
            return TextUtils.isEmpty(e.getText()) ? 0 : Integer.parseInt(e.getText().toString());
        } catch (NumberFormatException ex) {
            Log.w(TAG, "EditText 파싱 오류: " + e.getText().toString() + " - 기본값 0으로 처리.", ex);
            return 0;
        }
    }

    // 무작위 HEX 색상 문자열 생성 메소드
    private String generateRandomHexColor() {
        // 너무 어둡거나 너무 밝은 색상을 피하기 위해 각 채널의 범위를 조절할 수 있습니다.
        // 예: 각 채널을 50~200 사이로 제한하여 너무 극단적인 색 방지
        int red = random.nextInt(151) + 50;   // 50-200
        int green = random.nextInt(151) + 50; // 50-200
        int blue = random.nextInt(151) + 50;  // 50-200
        return String.format("#%02x%02x%02x", red, green, blue);
    }

    private void saveSubject() {
        if (TextUtils.isEmpty(subjectNameEditText.getText())) {
            String errorMsg = "과목명을 입력해주세요.";
            Snackbar errorSnackBar = Snackbar.make(v, errorMsg, Snackbar.LENGTH_INDEFINITE);
            errorSnackBar.setAction("확인", v -> errorSnackBar.dismiss());
            errorSnackBar.show();

            subjectNameInputLayout.setErrorEnabled(true);
            subjectNameInputLayout.setError("과목명을 입력해주세요.");
            return;
        } else {
            subjectNameInputLayout.setError(null);
            subjectNameInputLayout.setErrorEnabled(false);
        }

        if (type == -1) {
            String errorMsg = "과목 유형을 선택해주세요.";
            Snackbar errorSnackBar = Snackbar.make(v, errorMsg, Snackbar.LENGTH_INDEFINITE);
            errorSnackBar.setAction("확인", v -> errorSnackBar.dismiss());
            errorSnackBar.show();
            return;
        }

        if (credit == -1) {
            String errorMsg = "학점을 선택해주세요.";
            Snackbar errorSnackBar = Snackbar.make(v, errorMsg, Snackbar.LENGTH_INDEFINITE);
            errorSnackBar.setAction("확인", v -> errorSnackBar.dismiss());
            errorSnackBar.show();
            return;
        }

        EvaluationRatio ratio = new EvaluationRatio();
        ratio.setMidTermRatio(parse(midTermRatioEditText));
        ratio.setFinalTermRatio(parse(finalTermRatioEditText));
        ratio.setQuizRatio(parse(quizRatioEditText));
        ratio.setAssignmentRatio(parse(assignmentRatioEditText));
        ratio.setAttendanceRatio(parse(attendanceRatioEditText));

        TargetStudyTime time = new TargetStudyTime(
                parse(dailyTargetStudyTimeEditText),
                parse(weeklyTargetStudyTimeEditText),
                parse(monthlyTargetStudyTimeEditText)
        );

        String subjectNameStr = subjectNameEditText.getText().toString();
        String midScheduleStr = midTermScheduleEditText.getText().toString();
        String finalScheduleStr = finalTermScheduleEditText.getText().toString();

        // 클라우드 동기화 콜백 생성
        SubjectRepository.CloudSyncCallback callback = new SubjectRepository.CloudSyncCallback() {
            @Override
            public void onSuccess() {
                if (getActivity() != null) {
                    Toast.makeText(getActivity(),
                            editingId == -1 ? "과목이 성공적으로 추가되었습니다." : "과목이 성공적으로 업데이트되었습니다.",
                            Toast.LENGTH_SHORT).show();
                    dismiss();
                }
            }

            @Override
            public void onError(String message) {
                if (getActivity() != null) {
                    Toast.makeText(getActivity(),
                            "저장 중 오류가 발생했습니다: " + message,
                            Toast.LENGTH_LONG).show();
                    return;
                }
            }
        };

        if (editingId == -1) {
            SubjectEntity newSubject = new SubjectEntity(
                    subjectNameStr,
                    credit,
                    String.format("#%06X", (0xFFFFFF & subjectColor)),
                    type,
                    midScheduleStr,
                    finalScheduleStr,
                    ratio, time
            );
            if (difficulty != -1) {
                newSubject.setDifficulty(difficulty);
            }
            viewModel.insert(newSubject, callback);
            Log.d(TAG, "새 과목 '" + newSubject.getName() + "' 저장 완료.");
        } else { // 기존 과목 수정
            if (currentEditingSubject != null) {
                Log.d(TAG, "기존 과목 '" + currentEditingSubject.getName() + "' 수정 시작. 저장된 색상: " + currentEditingSubject.getColor());
                // currentEditingSubject의 필드를 UI 값으로 업데이트
                currentEditingSubject.setName(subjectNameStr);
                currentEditingSubject.setCredit(credit);
                currentEditingSubject.setColor(String.format("#%06X", (0xFFFFFF & subjectColor)));
                currentEditingSubject.setType(type);
                currentEditingSubject.setMidTermSchedule(midScheduleStr);
                currentEditingSubject.setFinalTermSchedule(finalScheduleStr);
                currentEditingSubject.setRatio(ratio);
                currentEditingSubject.setTime(time);
                currentEditingSubject.setDifficulty(difficulty);

                viewModel.update(currentEditingSubject, callback);
                Log.d(TAG, "기존 과목 '" + currentEditingSubject.getName() + "' 업데이트 완료.");
            } else {
                Log.e(TAG, "수정할 currentEditingSubject가 null입니다. 업데이트 실패.");
                Toast.makeText(getContext(), "과목 수정 중 오류가 발생했습니다.", Toast.LENGTH_SHORT).show();
                return;
                // 오류 처리 또는 사용자에게 알림
            }
        }
    }
}
