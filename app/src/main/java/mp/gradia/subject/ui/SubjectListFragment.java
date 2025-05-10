package mp.gradia.subject.ui;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.PopupMenu;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import mp.gradia.R;
import mp.gradia.database.entity.SubjectEntity;
import mp.gradia.subject.adapter.SubjectAdapter;
import mp.gradia.subject.viewmodel.SubjectViewModel;

public class SubjectListFragment extends Fragment {

    private SubjectViewModel subjectViewModel;
    private SubjectAdapter subjectAdapter;
    private TextView textEmpty;
    private EditText editSearch;

    private List<SubjectEntity> fullSubjectList = new ArrayList<>();
    private int selectedFilterType = -1; // -1: 전체, 0~2: 필터
    private int selectedSort = 0; // 0: 이름, 1: 학점, 2: 주간 목표시간

    public SubjectListFragment() {}

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_subject_list, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // ViewModel 먼저 초기화
        subjectViewModel = new ViewModelProvider(this).get(SubjectViewModel.class);

        // View 연결
        textEmpty = view.findViewById(R.id.textEmpty);
        editSearch = view.findViewById(R.id.editTextSearch);
        RecyclerView recyclerView = view.findViewById(R.id.recyclerViewSubjects);
        FloatingActionButton fabAdd = view.findViewById(R.id.fabAddSubject);
        Button sortButton = view.findViewById(R.id.buttonSort);

        // Adapter 설정
        subjectAdapter = new SubjectAdapter();
        recyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        recyclerView.setAdapter(subjectAdapter);

        // ViewModel 초기값 적용
        selectedSort = subjectViewModel.getSortType();
        selectedFilterType = subjectViewModel.getFilterType();
        editSearch.setText(subjectViewModel.getSearchQuery());

        // RecyclerView 데이터 관찰
        subjectViewModel.getAllSubjects().observe(getViewLifecycleOwner(), subjects -> {
            fullSubjectList = subjects;
            updateFilteredAndSortedList();
        });

        // 과목 클릭 시 상세로 이동
        subjectAdapter.setOnItemClickListener(subject -> {
            Bundle bundle = new Bundle();
            bundle.putInt("subjectId", subject.subjectId);
            Navigation.findNavController(requireView()).navigate(R.id.action_subjectList_to_subjectDetail, bundle);

        });

        // 추가 버튼
        fabAdd.setOnClickListener(v ->
                Navigation.findNavController(view).navigate(R.id.action_subjectList_to_subjectAdd)
        );

        // 필터 버튼 클릭 처리
        sortButton.setOnClickListener(v -> {
            PopupMenu popup = new PopupMenu(requireContext(), v);
            popup.getMenuInflater().inflate(R.menu.menu_sort, popup.getMenu());

            popup.setOnMenuItemClickListener(item -> {
                int itemId = item.getItemId();

                if (itemId == R.id.sort_name) {
                    selectedSort = 0;
                } else if (itemId == R.id.sort_credit) {
                    selectedSort = 1;
                } else if (itemId == R.id.sort_weekly) {
                    selectedSort = 2;
                } else if (itemId == R.id.sort_all) {
                    selectedFilterType = -1;
                    subjectViewModel.setFilterType(selectedFilterType);
                }

                subjectViewModel.setSortType(selectedSort);
                updateFilteredAndSortedList();
                return true;
            });

            popup.show();
        });

        // 검색창 입력 시 실시간 업데이트
        editSearch.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void afterTextChanged(Editable s) {}
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                subjectViewModel.setSearchQuery(s.toString());
                updateFilteredAndSortedList();
            }
        });
    }

    private void updateFilteredAndSortedList() {
        String query = editSearch.getText().toString().toLowerCase();
        List<SubjectEntity> filtered = new ArrayList<>();

        for (SubjectEntity subject : fullSubjectList) {
            if (selectedFilterType != -1 && subject.type != selectedFilterType) continue;
            if (!subject.name.toLowerCase().contains(query)) continue;
            filtered.add(subject);
        }

        switch (selectedSort) {
            case 0: // 이름 오름차순
                Collections.sort(filtered, Comparator.comparing(s -> s.name));
                break;
            case 1: // 학점 내림차순
                Collections.sort(filtered, (s1, s2) -> Integer.compare(s2.credit, s1.credit));
                break;
            case 2: // 주간 목표시간 내림차순
                Collections.sort(filtered, (s1, s2) -> Integer.compare(
                        s2.time.weeklyTargetStudyTime,
                        s1.time.weeklyTargetStudyTime));
                break;
        }

        subjectAdapter.setSubjects(filtered);
        textEmpty.setVisibility(filtered.isEmpty() ? View.VISIBLE : View.GONE);
        TextView sortTextView = requireView().findViewById(R.id.sortTextView);
        String[] sortNames = {"이름", "학점", "주간 목표시간"};
        sortTextView.setText("정렬: " + sortNames[selectedSort]);

    }
}

