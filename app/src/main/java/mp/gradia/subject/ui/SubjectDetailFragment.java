package mp.gradia.subject.ui;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import mp.gradia.R;
import mp.gradia.database.entity.TodoEntity;
import mp.gradia.subject.adapter.TodoAdapter;
import mp.gradia.subject.viewmodel.TodoViewModel;

import mp.gradia.database.entity.SubjectEntity;
import mp.gradia.subject.viewmodel.SubjectViewModel;

public class SubjectDetailFragment extends Fragment {



    private SubjectViewModel viewModel;

    private TodoViewModel todoViewModel;
    private TodoAdapter todoAdapter;

    private TextView textSubjectName, textType, textCredit, textDifficulty,
            textMidSchedule, textFinalSchedule, textEvalRatio, textTargetTime;

    private int subjectId;

    public SubjectDetailFragment() {}

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_subject_detail, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        subjectId = getArguments().getInt("subjectId", -1);
        if (subjectId == -1) return;



        viewModel = new ViewModelProvider(this).get(SubjectViewModel.class);

        // 과목 정보 TextView 연결
        textSubjectName = view.findViewById(R.id.textSubjectName);
        textType = view.findViewById(R.id.textType);
        textCredit = view.findViewById(R.id.textCredit);
        textDifficulty = view.findViewById(R.id.textDifficulty);
        textMidSchedule = view.findViewById(R.id.textMidSchedule);
        textFinalSchedule = view.findViewById(R.id.textFinalSchedule);
        textEvalRatio = view.findViewById(R.id.textEvalRatio);
        textTargetTime = view.findViewById(R.id.textTargetTime);

        // 버튼 연결
        ImageButton btnBack = view.findViewById(R.id.buttonBack);
        ImageButton btnEdit = view.findViewById(R.id.buttonEdit);
        ImageButton btnDelete = view.findViewById(R.id.buttonDelete);

        // 과목 데이터 가져오기
        viewModel.getSubjectById(subjectId).observe(getViewLifecycleOwner(), subject -> {
            if (subject != null) bindSubjectData(subject);
        });

        // 뒤로가기
        btnBack.setOnClickListener(v -> Navigation.findNavController(view).popBackStack());

        // 수정으로 이동
        btnEdit.setOnClickListener(v -> {
            Bundle bundle = new Bundle();
            bundle.putInt("subjectId", subjectId);
            Navigation.findNavController(view).navigate(R.id.action_subjectDetail_to_subjectAdd, bundle);
            Log.d("subjectId", String.valueOf(subjectId));
        });

        // 삭제 다이얼로그로 이동
        btnDelete.setOnClickListener(v -> {
            Bundle bundle = new Bundle();
            bundle.putInt("subjectId", subjectId);
            Navigation.findNavController(view).navigate(R.id.action_subjectDetail_to_subjectDeleteDialog, bundle);
        });

        // 할 일 목록
        // TODO: 메모 기능 미구현으로 주석 처리

        RecyclerView recyclerTodo = view.findViewById(R.id.recyclerTodo);
        Button buttonAddTodo = view.findViewById(R.id.buttonAddTodo);

        todoAdapter = new TodoAdapter();
        recyclerTodo.setLayoutManager(new LinearLayoutManager(requireContext()));
        recyclerTodo.setAdapter(todoAdapter);

        todoViewModel = new ViewModelProvider(this).get(TodoViewModel.class);
        todoViewModel.getTodosForSubject(subjectId).observe(getViewLifecycleOwner(), todos -> {
            try {
                todoAdapter.setTodos(todos);
            } catch (Exception e) {
                Log.e("TodoLiveDataError", "Error setting todos", e);
            }
        });

        todoAdapter.setListener(new TodoAdapter.TodoActionListener() {
            @Override
            public void onCheckChanged(TodoEntity todo, boolean isChecked) {
                todo.isDone = isChecked;
                todoViewModel.update(todo);
            }

            @Override
            public void onItemClicked(TodoEntity todo) {
                TodoInputDialog dialog = new TodoInputDialog();
                dialog.setDefaultText(todo.content);
                dialog.setListener(newContent -> {
                    todo.content = newContent;
                    todoViewModel.update(todo);
                });
                dialog.show(getParentFragmentManager(), "EditTodo");
            }

            @Override
            public void onDeleteClicked(TodoEntity todo) {
                todoViewModel.delete(todo);
            }
        });

        buttonAddTodo.setOnClickListener(v -> {
            TodoInputDialog dialog = new TodoInputDialog();
            dialog.setListener(content -> {
                TodoEntity newTodo = new TodoEntity();
                newTodo.subjectId = subjectId;
                newTodo.content = content;
                newTodo.isDone = false;
                todoViewModel.insert(newTodo);
            });
            dialog.show(getParentFragmentManager(), "AddTodo");
        });
    }

    private void bindSubjectData(SubjectEntity subject) {
        textSubjectName.setText(subject.name);
        String typeText;
        switch (subject.type) {
            case 0:
                typeText = "전필";
                break;
            case 1:
                typeText = "전선";
                break;
            case 2:
                typeText = "교양";
                break;
            default:
                typeText = "기타";
                break;
        }
        textType.setText(typeText);

        textCredit.setText(subject.credit + "학점");
        textDifficulty.setText(subject.difficulty == null ? "정보 없음" : subject.difficulty + " / 5");

        textMidSchedule.setText(subject.midTermSchedule);
        textFinalSchedule.setText(subject.finalTermSchedule);

        // 평가 비율 출력 (null 방어)
        if (subject.ratio != null) {
            textEvalRatio.setText("중간 " + subject.ratio.midTermRatio +
                    "% / 기말 " + subject.ratio.finalTermRatio +
                    "% / 퀴즈 " + subject.ratio.quizRatio +
                    "% / 과제 " + subject.ratio.assignmentRatio +
                    "% / 출석 " + subject.ratio.attendanceRatio + "%");
        } else {
            textEvalRatio.setText("평가 비율 정보 없음");
        }

// 목표 시간 출력 (null 방어)
        if (subject.time != null) {
            textTargetTime.setText("일간 " + subject.time.dailyTargetStudyTime +
                    "시간 / 주간 " + subject.time.weeklyTargetStudyTime +
                    "시간 / 월간 " + subject.time.monthlyTargetStudyTime + "시간");
        } else {
            textTargetTime.setText("목표 학습 시간 정보 없음");
        }

    }
}