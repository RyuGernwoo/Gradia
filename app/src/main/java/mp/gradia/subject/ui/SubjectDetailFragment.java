package mp.gradia.subject.ui;

import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.github.dhaval2404.colorpicker.ColorPickerDialog;
import com.github.dhaval2404.colorpicker.listener.ColorListener;
import com.github.dhaval2404.colorpicker.model.ColorShape;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.transition.platform.MaterialElevationScale;
import com.google.android.material.transition.platform.MaterialFade;
import com.google.android.material.transition.platform.MaterialSharedAxis;

import org.jetbrains.annotations.NotNull;

import mp.gradia.R;
import mp.gradia.database.entity.TodoEntity;
import mp.gradia.subject.adapter.TodoAdapter;
import mp.gradia.database.repository.SubjectRepository;
import mp.gradia.subject.viewmodel.TodoViewModel;

import mp.gradia.database.entity.SubjectEntity;
import mp.gradia.subject.viewmodel.SubjectViewModel;
import mp.gradia.time.inner.viewmodel.StudySessionViewModelFactory;

public class SubjectDetailFragment extends Fragment {

    private Toolbar toolbar;
    private SubjectViewModel viewModel;

    private TodoViewModel todoViewModel;
    private SubjectEntity subject;
    private TodoAdapter todoAdapter;
    private RecyclerView recyclerTodo;
    private Button buttonAddTodo;
    private ImageView colorCircle;

    private TextView textSubjectName, textType, textCredit, textDifficulty,
            textMidSchedule, textFinalSchedule, textEvalRatio, textTargetTime;

    private int subjectId;
    private int subjectColor;

    public SubjectDetailFragment() {
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        subjectId = getArguments().getInt("subjectId", -1);
        if (subjectId == -1) return;
        viewModel = new ViewModelProvider(this).get(SubjectViewModel.class);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {

        // 과목 데이터 가져오기
        viewModel.getSubjectById(subjectId).observe(getViewLifecycleOwner(), subject -> {
            if (subject != null) {
                bindSubjectData(subject);
            }
        });

        return inflater.inflate(R.layout.fragment_subject_detail, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        initViews(view);
        setupToolbar(view);
        setupDetailView();
        setupTodo();
    }

    private void initViews(View view) {
        toolbar = view.findViewById(R.id.toolbar);
        colorCircle = view.findViewById(R.id.color_circle);
        // 과목 정보 TextView 연결
        textSubjectName = view.findViewById(R.id.textSubjectName);
        textType = view.findViewById(R.id.textType);
        textCredit = view.findViewById(R.id.textCredit);
        textDifficulty = view.findViewById(R.id.textDifficulty);
        textMidSchedule = view.findViewById(R.id.textMidSchedule);
        textFinalSchedule = view.findViewById(R.id.textFinalSchedule);
        textEvalRatio = view.findViewById(R.id.textEvalRatio);
        textTargetTime = view.findViewById(R.id.textTargetTime);
        recyclerTodo = view.findViewById(R.id.recyclerTodo);
        buttonAddTodo = view.findViewById(R.id.buttonAddTodo);
    }

    private void setupToolbar(View view) {
        toolbar.setNavigationOnClickListener(v -> Navigation.findNavController(view).popBackStack());
        toolbar.inflateMenu(R.menu.subject_detail_menu);
        toolbar.setOnMenuItemClickListener(item -> {
            if (item.getItemId() == R.id.action_edit_subject) {
                Bundle bundle = new Bundle();
                bundle.putInt("subjectId", subjectId);
                Navigation.findNavController(view).navigate(R.id.action_subjectDetail_to_subjectAdd, bundle);

                return true;
            } else if (item.getItemId() == R.id.action_delete_subject) {
                MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(requireContext(), R.style.DeleteSessionDialogTheme);
                builder.setTitle("과목 삭제")
                        .setMessage("이 과목을 정말 삭제하시겠습니까?")
                        .setNegativeButton("취소", null)
                        .setPositiveButton("삭제", (dialog, which) -> deleteSubject());

                AlertDialog alertDialog = builder.create();

                // 3. 다이얼로그의 Window 가져오기
                Window window = alertDialog.getWindow();
                if (window != null) {
                    // 예시: MaterialSharedAxis (Z축)
                    MaterialSharedAxis enterTransitionZ = new MaterialSharedAxis(MaterialSharedAxis.Z, true);
                    enterTransitionZ.setDuration(1000L); // 길게 테스트
                    window.setEnterTransition(enterTransitionZ);

                    MaterialSharedAxis exitTransitionZ = new MaterialSharedAxis(MaterialSharedAxis.Z, false);
                    exitTransitionZ.setDuration(1000L);
                    window.setExitTransition(exitTransitionZ);
                }
                alertDialog.show();
                return true;
            }
            return false;
        });
    }

    private void setSubjectColor(int color) {
        Drawable baseDrawable = ContextCompat.getDrawable(requireContext(), R.drawable.color_circle);
        if (baseDrawable instanceof GradientDrawable) {
            ((GradientDrawable) baseDrawable).setColor(color);
            colorCircle.setImageDrawable(baseDrawable);
        }

        buttonAddTodo.setBackgroundColor(color);
    }

    private void setupDetailView() {
        colorCircle.setOnClickListener(v -> {
            new ColorPickerDialog
                    .Builder(requireContext())
                    .setTitle("과목 색상 선택")
                    .setColorShape(ColorShape.SQAURE)
                    .setDefaultColor(subjectColor)
                    .setColorListener(new ColorListener() {
                        @Override
                        public void onColorSelected(int color, @NotNull String colorHex) {
                            subject.setColor(String.format("#%06X", (0xFFFFFF & color)));
                            viewModel.update(subject);
                            setSubjectColor(color);
                        }
                    })
                    .show();
        });
    }

    private void setupTodo() {
        // 할 일 목록
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

    private void deleteSubject() {
        viewModel.getSubjectById(subjectId).observe(this, subject -> {
            if (subject != null) {
                // 클라우드 동기화 콜백 생성
                SubjectRepository.CloudSyncCallback callback = new SubjectRepository.CloudSyncCallback() {
                    @Override
                    public void onSuccess() {
                        if (getActivity() != null) {
                            Toast.makeText(getActivity(), "과목이 성공적으로 삭제되었습니다.", Toast.LENGTH_SHORT).show();

                            // 삭제 후 과목 리스트로 명확하게 이동
                            Navigation.findNavController(requireActivity(), R.id.nav_host_fragment).navigate(R.id.action_subjectDetail_to_subjectDelete);
                        }
                    }

                    @Override
                    public void onError(String message) {
                        if (getActivity() != null) {
                            Toast.makeText(getActivity(), "삭제 중 오류가 발생했습니다: " + message, Toast.LENGTH_LONG).show();
                        }
                    }
                };

                viewModel.delete(subject, callback);
            }
        });
    }

    private void bindSubjectData(SubjectEntity subject) {
        this.subject = subject;
        textSubjectName.setText(subject.name);
        subjectColor = Color.parseColor(subject.getColor());
        setSubjectColor(subjectColor);

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