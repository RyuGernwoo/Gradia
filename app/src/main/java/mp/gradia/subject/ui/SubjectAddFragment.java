package mp.gradia.subject.ui;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.Spinner;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.Navigation;

import mp.gradia.R;
import mp.gradia.database.entity.EvaluationRatio;
import mp.gradia.database.entity.SubjectEntity;
import mp.gradia.database.entity.TargetStudyTime;
import mp.gradia.subject.viewmodel.SubjectViewModel;

// 과목 추가 및 수정 화면 fragment
public class SubjectAddFragment extends Fragment {

    private SubjectViewModel viewModel;
    private EditText inputName, inputCredit, inputDifficulty, inputMid, inputFinal;
    private EditText inputMidRatio, inputFinalRatio, inputQuizRatio, inputAssignmentRatio, inputAttendanceRatio;
    private EditText inputDaily, inputWeekly, inputMonthly;
    private Spinner inputType;
    private int editingId = -1;

    public SubjectAddFragment() {}


    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_subject_add, container, false);
    }

    // UI 초기화 및 버튼 동작 설정
    @Override
    public void onViewCreated(@NonNull View v, @Nullable Bundle s) {
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

        // 과목 유형 Spinner 설정
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(
                requireContext(), R.array.subject_type_array, android.R.layout.simple_spinner_item);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        inputType.setAdapter(adapter);

        // 뒤로가기 버튼
        ImageButton buttonBack = v.findViewById(R.id.buttonBack);
        buttonBack.setOnClickListener(view -> Navigation.findNavController(v).popBackStack());

        // 수정 모드인지 확인(수정모드일 때 id가져옴)
        editingId = getArguments() != null ? getArguments().getInt("subjectId", -1) : -1;

        if (editingId != -1) {
            viewModel.getSubjectById(editingId).observe(getViewLifecycleOwner(), subject -> {
                if (subject != null) fillForm(subject);
            });
        }

        // 저장 버튼 클릭 시
        v.findViewById(R.id.buttonSave).setOnClickListener(btn -> {
            if (TextUtils.isEmpty(inputName.getText())) return;
            //사용자 입력값
            var ratio = new EvaluationRatio();
            ratio.midTermRatio = parse(inputMidRatio);
            ratio.finalTermRatio = parse(inputFinalRatio);
            ratio.quizRatio = parse(inputQuizRatio);
            ratio.assignmentRatio = parse(inputAssignmentRatio);
            ratio.attendanceRatio = parse(inputAttendanceRatio);

            var time = new TargetStudyTime(
                    parse(inputDaily),
                    parse(inputWeekly),
                    parse(inputMonthly)
            );

            SubjectEntity subject = new SubjectEntity(
                    inputName.getText().toString(),
                    parse(inputCredit),
                    "#dd3333", // 색상은 임시로 설정
                    inputType.getSelectedItemPosition(),
                    inputMid.getText().toString(),
                    inputFinal.getText().toString(),
                    ratio, time
            );

            //db에 삽입이나 업데이트
            if (editingId == -1) viewModel.insert(subject);
            else {
                subject.subjectId = editingId;
                viewModel.update(subject);
            }
            // 저장 후 이전 화면으로
            Navigation.findNavController(v).popBackStack();
        });
    }
    // 기존 데이터로 입력 필드 채우기
    private void fillForm(SubjectEntity subject) {
        inputName.setText(subject.name);
        inputType.setSelection(subject.type);
        inputCredit.setText(String.valueOf(subject.credit));
        inputDifficulty.setText(String.valueOf(subject.difficulty));
        inputMid.setText(subject.midTermSchedule);
        inputFinal.setText(subject.finalTermSchedule);

        inputMidRatio.setText(String.valueOf(subject.ratio.midTermRatio));
        inputFinalRatio.setText(String.valueOf(subject.ratio.finalTermRatio));
        inputQuizRatio.setText(String.valueOf(subject.ratio.quizRatio));
        inputAssignmentRatio.setText(String.valueOf(subject.ratio.assignmentRatio));
        inputAttendanceRatio.setText(String.valueOf(subject.ratio.attendanceRatio));

        inputDaily.setText(String.valueOf(subject.time.dailyTargetStudyTime));
        inputWeekly.setText(String.valueOf(subject.time.weeklyTargetStudyTime));
        inputMonthly.setText(String.valueOf(subject.time.monthlyTargetStudyTime));
    }

    private int parse(EditText e) {
        return TextUtils.isEmpty(e.getText()) ? 0 : Integer.parseInt(e.getText().toString());
    }
}

