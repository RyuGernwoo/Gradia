package mp.gradia.subject.ui;

import android.graphics.Color;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.Navigation;

import java.util.Random;

import mp.gradia.R;
import mp.gradia.database.entity.EvaluationRatio;
import mp.gradia.database.entity.SubjectEntity;
import mp.gradia.database.entity.TargetStudyTime;
import mp.gradia.subject.repository.SubjectRepository;
import mp.gradia.subject.viewmodel.SubjectViewModel;

// 과목 추가 및 수정 화면 fragment
public class SubjectAddFragment extends Fragment {

    private static final String TAG = "SubjectAddFragment";
    private SubjectViewModel viewModel;
    private EditText inputName, inputCredit, inputDifficulty, inputMid, inputFinal;
    private EditText inputMidRatio, inputFinalRatio, inputQuizRatio, inputAssignmentRatio, inputAttendanceRatio;
    private EditText inputDaily, inputWeekly, inputMonthly;
    private Spinner inputType;
    private int editingId = -1;
    private SubjectEntity currentEditingSubject = null;
    private Random random = new Random();

    public SubjectAddFragment() {
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_subject_add, container, false);
    }

    // UI 초기화 및 버튼 동작 설정
    @Override
    public void onViewCreated(@NonNull View v, @Nullable Bundle s) {
        super.onViewCreated(v, s);
        viewModel = new ViewModelProvider(this).get(SubjectViewModel.class);

        // XML에 정의된 UI 요소 연결
        inputName = v.findViewById(R.id.inputName);
        inputType = v.findViewById(R.id.inputType);
        inputCredit = v.findViewById(R.id.inputCredit);
        inputDifficulty = v.findViewById(R.id.inputDifficulty);
        inputMid = v.findViewById(R.id.inputMidSchedule);
        inputFinal = v.findViewById(R.id.inputFinalSchedule);
        inputMidRatio = v.findViewById(R.id.inputMidRatio);
        inputFinalRatio = v.findViewById(R.id.inputFinalRatio);
        inputQuizRatio = v.findViewById(R.id.inputQuizRatio);
        inputAssignmentRatio = v.findViewById(R.id.inputAssignmentRatio);
        inputAttendanceRatio = v.findViewById(R.id.inputAttendanceRatio);
        inputDaily = v.findViewById(R.id.inputDailyTarget);
        inputWeekly = v.findViewById(R.id.inputWeeklyTarget);
        inputMonthly = v.findViewById(R.id.inputMonthlyTarget);
        Button buttonSave = v.findViewById(R.id.buttonSave);

        // 과목 유형 Spinner 설정
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(
                requireContext(), R.array.subject_type_array, android.R.layout.simple_spinner_item);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        inputType.setAdapter(adapter);

        // 뒤로가기 버튼
        ImageButton buttonBack = v.findViewById(R.id.buttonBack);
        buttonBack.setOnClickListener(view -> Navigation.findNavController(v).popBackStack());

        // 수정 모드인지 확인(수정모드일 때 id가져옴)
        if (getArguments() != null) {
            editingId = getArguments().getInt("subjectId", -1);
        }

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

        // 저장 버튼 클릭 시
        v.findViewById(R.id.buttonSave).setOnClickListener(btn -> {
            if (TextUtils.isEmpty(inputName.getText()))
                return;

            // 버튼 비활성화로 중복 클릭 방지
            btn.setEnabled(false);
        });


        buttonSave.setOnClickListener(btn -> { // buttonSave로 변경
            if (TextUtils.isEmpty(inputName.getText())) {
                Toast.makeText(getContext(), "과목명을 입력해주세요.", Toast.LENGTH_SHORT).show();
                return;
            }

            EvaluationRatio ratio = new EvaluationRatio();
            ratio.midTermRatio = parse(inputMidRatio);
            ratio.finalTermRatio = parse(inputFinalRatio);
            ratio.quizRatio = parse(inputQuizRatio);
            ratio.assignmentRatio = parse(inputAssignmentRatio);
            ratio.attendanceRatio = parse(inputAttendanceRatio);

            TargetStudyTime time = new TargetStudyTime(
                    parse(inputDaily),
                    parse(inputWeekly),
                    parse(inputMonthly)
            );

            String subjectNameStr = inputName.getText().toString();
            int creditInt = parse(inputCredit);
            int typeInt = inputType.getSelectedItemPosition();
            String midScheduleStr = inputMid.getText().toString();
            String finalScheduleStr = inputFinal.getText().toString();
            Integer difficultyInt = TextUtils.isEmpty(inputDifficulty.getText()) ? null : parse(inputDifficulty);

            // 클라우드 동기화 콜백 생성
            SubjectRepository.CloudSyncCallback callback = new SubjectRepository.CloudSyncCallback() {
                @Override
                public void onSuccess() {
                    if (getActivity() != null) {
                        Toast.makeText(getActivity(),
                                editingId == -1 ? "과목이 성공적으로 추가되었습니다." : "과목이 성공적으로 업데이트되었습니다.",
                                Toast.LENGTH_SHORT).show();
                        Navigation.findNavController(v).popBackStack();
                    }
                }

                @Override
                public void onError(String message) {
                    if (getActivity() != null) {
                        Toast.makeText(getActivity(),
                                "저장 중 오류가 발생했습니다: " + message,
                                Toast.LENGTH_LONG).show();
                        btn.setEnabled(true); // 버튼 다시 활성화
                    }
                }
            };

            if (editingId == -1) { // 새 과목 추가
                String randomColor = generateRandomHexColor(); // 무작위 색상 생성
                Log.d(TAG, "새 과목 추가. 생성된 색상: " + randomColor);
                SubjectEntity newSubject = new SubjectEntity(
                        subjectNameStr,
                        creditInt,
                        randomColor, // 무작위 색상 사용
                        typeInt,
                        midScheduleStr,
                        finalScheduleStr,
                        ratio, time
                );
                if (difficultyInt != null) {
                    newSubject.setDifficulty(difficultyInt);
                }
                viewModel.insert(newSubject, callback);
                Log.d(TAG, "새 과목 '" + newSubject.getName() + "' 저장 완료.");
            } else { // 기존 과목 수정
                if (currentEditingSubject != null) {
                    Log.d(TAG, "기존 과목 '" + currentEditingSubject.getName() + "' 수정 시작. 저장된 색상: " + currentEditingSubject.getColor());
                    // currentEditingSubject의 필드를 UI 값으로 업데이트
                    currentEditingSubject.setName(subjectNameStr);
                    currentEditingSubject.setCredit(creditInt);
                    // 색상은 기존 값 유지 (currentEditingSubject.color는 변경하지 않음)
                    currentEditingSubject.setType(typeInt);
                    currentEditingSubject.setMidTermSchedule(midScheduleStr);
                    currentEditingSubject.setFinalTermSchedule(finalScheduleStr);
                    currentEditingSubject.setRatio(ratio);
                    currentEditingSubject.setTime(time);
                    currentEditingSubject.setDifficulty(difficultyInt);

                    viewModel.update(currentEditingSubject, callback);
                    Log.d(TAG, "기존 과목 '" + currentEditingSubject.getName() + "' 업데이트 완료.");
                } else {
                    Log.e(TAG, "수정할 currentEditingSubject가 null입니다. 업데이트 실패.");
                    Toast.makeText(getContext(), "과목 수정 중 오류가 발생했습니다.", Toast.LENGTH_SHORT).show();
                    btn.setEnabled(true);
                    // 오류 처리 또는 사용자에게 알림
                }
            }
        });
    }

    // 기존 데이터로 입력 필드 채우기
    private void fillForm(SubjectEntity subject) {
        if (subject == null) return;
        Log.d(TAG, "fillForm 호출. 과목명: " + subject.getName() + ", 색상: " + subject.getColor());

        inputName.setText(subject.name);
        inputType.setSelection(subject.type);
        inputCredit.setText(String.valueOf(subject.credit));
        inputDifficulty.setText(subject.difficulty == null ? "" : String.valueOf(subject.difficulty));
        inputMid.setText(subject.midTermSchedule != null ? subject.midTermSchedule : "");
        inputFinal.setText(subject.finalTermSchedule != null ? subject.finalTermSchedule : "");

        // subject.ratio가 null일 수 있음
        if (subject.ratio != null) {
            inputMidRatio.setText(String.valueOf(subject.ratio.midTermRatio));
            inputFinalRatio.setText(String.valueOf(subject.ratio.finalTermRatio));
            inputQuizRatio.setText(String.valueOf(subject.ratio.quizRatio));
            inputAssignmentRatio.setText(String.valueOf(subject.ratio.assignmentRatio));
            inputAttendanceRatio.setText(String.valueOf(subject.ratio.attendanceRatio));
        } else {
            inputMidRatio.setText("");
            inputFinalRatio.setText("");
            inputQuizRatio.setText("");
            inputAssignmentRatio.setText("");
            inputAttendanceRatio.setText("");
        }

        // subject.time도 null일 수 있음
        if (subject.time != null) {
            inputDaily.setText(String.valueOf(subject.time.dailyTargetStudyTime));
            inputWeekly.setText(String.valueOf(subject.time.weeklyTargetStudyTime));
            inputMonthly.setText(String.valueOf(subject.time.monthlyTargetStudyTime));
        } else {
            inputDaily.setText("");
            inputWeekly.setText("");
            inputMonthly.setText("");
        }
    }

    private int parse(EditText e) {
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
}