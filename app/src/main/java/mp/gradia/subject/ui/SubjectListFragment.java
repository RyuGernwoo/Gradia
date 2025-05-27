package mp.gradia.subject.ui;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.PopupMenu;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import androidx.navigation.Navigation;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.textfield.TextInputEditText;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import mp.gradia.R;
import mp.gradia.database.entity.SubjectEntity;
import mp.gradia.subject.adapter.SubjectAdapter;
import mp.gradia.database.repository.SubjectRepository;
import mp.gradia.subject.viewmodel.SubjectViewModel;

public class SubjectListFragment extends Fragment {
    private static final String EVERYTIME_TIMETABLE_PREF = "everytimeTimetablePref";

    private SubjectViewModel subjectViewModel;
    private SubjectAdapter subjectAdapter;
    private TextView textEmpty;
    private TextInputEditText searchEditText;
    private RecyclerView recyclerView;
    private ImageButton sortImgBtn;
    private FloatingActionButton fabAdd;
    private FloatingActionButton fabAddEverytime;

    private List<SubjectEntity> fullSubjectList = new ArrayList<>();
    private int selectedFilterType = -1; // -1: 전체, 0~2: 필터
    private int selectedSort = 0; // 0: 이름, 1: 학점, 2: 주간 목표시간
    private boolean isShowingAll = false;
    private boolean isExpand = false;
    private boolean isTimetableLoaded = false;

    public SubjectListFragment() {
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // ViewModel 먼저 초기화
        subjectViewModel = new ViewModelProvider(this).get(SubjectViewModel.class);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_subject_list, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        initViews(view);
        setupRecylcerView();
        setupSubjectList();
        loadTimetableStatePref();
        setupSubjectAdd();
        setupSort();
        setupSearch();
    }

    private void initViews(View view) {
        // View 연결
        textEmpty = view.findViewById(R.id.textEmpty);
        searchEditText = view.findViewById(R.id.search_text_input_edit_text);
        recyclerView = view.findViewById(R.id.recyclerViewSubjects);
        sortImgBtn = view.findViewById(R.id.sort_by_img_btn);
        fabAdd = view.findViewById(R.id.fabAddSubject);
        fabAddEverytime = view.findViewById(R.id.fabAddSubjectEverytime);
    }

    private void setupRecylcerView() {
        // Adapter 설정
        subjectAdapter = new SubjectAdapter(requireContext(), fullSubjectList);
        recyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        recyclerView.setAdapter(subjectAdapter);
    }

    private void setupSubjectList() {
        // ViewModel 초기값 적용
        selectedSort = subjectViewModel.getSortType();
        selectedFilterType = subjectViewModel.getFilterType();
        //searchView.setText(subjectViewModel.getSearchQuery());

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

            Log.d("subjectId", String.valueOf(subject.subjectId));
        });
    }

    private void setupSubjectAdd() {
        // 추가 FAB 버튼
        fabAdd.setOnClickListener(v ->
                Navigation.findNavController(v).navigate(R.id.action_subjectList_to_subjectAdd)
        );

        if (isTimetableLoaded) {
            fabAddEverytime.setVisibility(View.GONE);
            return;
        }
        else {
            fabAddEverytime.setVisibility(View.VISIBLE);
        }

        // Everytime 시간표 추가 FAB 버튼
        fabAddEverytime.setOnClickListener(v -> {
            MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(requireContext());
            LayoutInflater inflater = requireActivity().getLayoutInflater();
            View dialogView = inflater.inflate(R.layout.dialog_everytime, null);


            LinearLayout expandContentLayout = dialogView.findViewById(R.id.expand_content_container);
            LinearLayout expandLayoutContainer = dialogView.findViewById(R.id.expand_layout_container);;
            ImageView expandImg = dialogView.findViewById(R.id.expand_img);
            TextView expandTextView = dialogView.findViewById(R.id.expand_textview);
            TextInputEditText everytimeUrlEdittext = dialogView.findViewById(R.id.everytime_url_edittext);

            expandLayoutContainer.setOnClickListener( expandView -> {
                if (isExpand) {
                    isExpand = false;
                    expandTextView.setText(R.string.dialog_everytime_expand);
                    expandImg.setImageResource(R.drawable.ic_expand_more);
                    expandContentLayout.setVisibility(View.GONE);
                }
                else {
                    isExpand = true;
                    expandTextView.setText(R.string.dialog_everytime_contract);
                    expandImg.setImageResource(R.drawable.ic_expand_less);
                    expandContentLayout.setVisibility(View.VISIBLE);
                }
            });

            builder.setTitle("에브리타임 시간표 가져오기")
                    .setNegativeButton("취소", null)
                    .setNeutralButton("에브리타임으로 이동", (dialog, which) -> {
                        launchEveryTime();
                    })
                    .setPositiveButton("확인", (dialog, which) -> {
                        // TO-DO : Implement Timetable from everytime.
                        String url = everytimeUrlEdittext.getText().toString();
                        loadTimeTable(url);
                        // TimeTable을 로드하게되면 로드 된 상태가 저장되어 더이상 사용자에게 fab가 보이지 않게 됨
                        saveTimetableStatePref();
                    })
                    .setView(dialogView);

            builder.create().show();
        });
    }

    private void setupSort() {
        sortImgBtn.setOnClickListener(v -> {
            PopupMenu popup = new PopupMenu(requireContext(), v);
            popup.getMenuInflater().inflate(R.menu.searchbar_menu, popup.getMenu());
            popup.setOnMenuItemClickListener(item -> {
                int itemId = item.getItemId();

                if (itemId == R.id.sort_name) {
                    selectedSort = 0;
                    isShowingAll = false;
                } else if (itemId == R.id.sort_credit) {
                    selectedSort = 1;
                    isShowingAll = false;
                } else if (itemId == R.id.sort_weekly) {
                    selectedSort = 2;
                    isShowingAll = false;
                } else if (itemId == R.id.sort_all) {
                    selectedFilterType = -1;
                    subjectViewModel.setFilterType(selectedFilterType);
                    isShowingAll = true;
                    updateFilteredAndSortedList();
                    return true;
                }

                subjectViewModel.setSortType(selectedSort);
                updateFilteredAndSortedList();
                return true;
            });

            popup.show();
        });
    }

    private void launchEveryTime() {
        String targetAppPackageName = "com.everytime.v2";
        String targetAppActivityName = "com.everytime.v2.SplashActivity";

        Context context = requireContext();
        Intent launchIntent = new Intent();
        launchIntent.setComponent(new android.content.ComponentName(targetAppPackageName, targetAppActivityName));

        if (launchIntent.resolveActivity(context.getPackageManager()) != null) {
            try {
                // 앱 실행
                context.startActivity(launchIntent);
            } catch (android.content.ActivityNotFoundException e) {
                Toast.makeText(context, "앱을 실행할 수 없습니다.\n앱이 설치되어있는지 확인해주세요.", Toast.LENGTH_SHORT).show();

                try {
                    // 플레이스토어 실행
                    Intent storeIntent = new Intent(Intent.ACTION_VIEW, android.net.Uri.parse("market://details?id=" + targetAppPackageName));
                    context.startActivity(storeIntent);
                } catch (android.content.ActivityNotFoundException eMarket) {
                    Intent webIntent = new Intent(Intent.ACTION_VIEW, android.net.Uri.parse("https://play.google.com/store/apps/details?id=" + targetAppPackageName));
                    if (webIntent.resolveActivity(context.getPackageManager()) != null) {
                        context.startActivity(webIntent);
                    } else {
                        Toast.makeText(context, "플레이 스토어에 연결할 수 없습니다.", Toast.LENGTH_SHORT).show();
                    }
                }
            }
        } else {
            Toast.makeText(context, targetAppPackageName + " 앱이 설치되어 있지 않습니다.\n스토어에서 설치해주세요.", Toast.LENGTH_LONG).show();

            try {
                // 플레이스토어 실행
                Intent storeIntent = new Intent(Intent.ACTION_VIEW, android.net.Uri.parse("market://details?id=" + targetAppPackageName));
                context.startActivity(storeIntent);
            } catch (android.content.ActivityNotFoundException eMarket) {
                Intent webIntent = new Intent(Intent.ACTION_VIEW, android.net.Uri.parse("https://play.google.com/store/apps/details?id=" + targetAppPackageName));
                if (webIntent.resolveActivity(context.getPackageManager()) != null) {
                    context.startActivity(webIntent);
                } else {
                    Toast.makeText(context, "플레이 스토어에 연결할 수 없습니다.", Toast.LENGTH_SHORT).show();
                }
            }
        }
    }
    private void loadTimeTable(String url) {
        // TO-DO : Implement Timetable from everytime.
        Log.d("SubjectListFragment", url);
        subjectViewModel.fetchEveryTimeTable(url, new SubjectRepository.CloudSyncCallback() {
            @Override
            public void onSuccess() {
                Toast.makeText(requireContext(), "시간표가 성공적으로 가져와졌습니다.", Toast.LENGTH_SHORT).show();
                // 시간표 로드 후 UI 업데이트
                updateFilteredAndSortedList();

                // set timetable load state
                isTimetableLoaded = true;
            }

            @Override
            public void onError(String error) {
                Toast.makeText(requireContext(), "시간표 가져오기 실패: " + error, Toast.LENGTH_SHORT).show();
            }
        });
    }

    // 현재 저장 상태를 SharedPref에 저장
    private void saveTimetableStatePref() {
        SharedPreferences sharedPreferences = requireContext().getSharedPreferences(EVERYTIME_TIMETABLE_PREF, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putBoolean("timetableLoaded", isTimetableLoaded);
        editor.apply();
    }

    private void loadTimetableStatePref() {
        SharedPreferences sharedPreferences = requireContext().getSharedPreferences(EVERYTIME_TIMETABLE_PREF, Context.MODE_PRIVATE);
        isTimetableLoaded = sharedPreferences.getBoolean("timetableLoaded", false);
    }

    private void setupSearch() {
        // 검색창 입력 시 실시간 업데이트
        searchEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void afterTextChanged(Editable s) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                subjectViewModel.setSearchQuery(s.toString());
                updateFilteredAndSortedList();
            }
        });
    }
    private void updateFilteredAndSortedList() {
        String query = searchEditText.getText().toString().toLowerCase();
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
                Collections.sort(filtered, (s1, s2) -> {
                    int t1 = (s1.time != null) ? s1.time.weeklyTargetStudyTime : 0;
                    int t2 = (s2.time != null) ? s2.time.weeklyTargetStudyTime : 0;
                    return Integer.compare(t2, t1); // 내림차순
                });
                break;
        }

        subjectAdapter.setSubjects(filtered);
        textEmpty.setVisibility(filtered.isEmpty() ? View.VISIBLE : View.GONE);
        TextView sortTextView = requireView().findViewById(R.id.sortTextView);
        if (isShowingAll) {
            sortTextView.setText("정렬: 전체 보기");
        } else {
            String[] sortNames = {"이름", "학점", "주간 목표시간"};
            if (selectedSort >= 0 && selectedSort < sortNames.length) {
                sortTextView.setText("정렬: " + sortNames[selectedSort]);
            }
        }

    }
}

