package mp.gradia.home;

import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.GridLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.PopupMenu;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import mp.gradia.R;
import mp.gradia.database.AppDatabase; // AppDatabase 추가
import mp.gradia.database.dao.StudySessionDao; // StudySessionDao 추가
import mp.gradia.database.dao.SubjectDao; // SubjectDao 추가 (Factory용)
import mp.gradia.main.MainActivity;
import mp.gradia.database.entity.StudySessionEntity; // StudySessionEntity 추가
import mp.gradia.database.entity.SubjectEntity;
import mp.gradia.database.entity.TargetStudyTime;
import mp.gradia.time.inner.viewmodel.StudySessionViewModel; // StudySessionViewModel import
import mp.gradia.time.inner.viewmodel.StudySessionViewModelFactory; // ViewModel Factory import

public class HomeFragment extends Fragment {
    private static final String TAG = "HomeFragment";
    private LinearLayout subjectListContainer; // 과목 리스트
    private TextView textNoData; // 과목 없을 때 표시
    private Button btnGoToSubjects; // 과목 없을 때 과목으로 이동 버튼
    private TextView tvMarqueeNotifications; // 전광판
    private StudySessionViewModel studySessionViewModel; // 과목 세부 정보 가져오기
    private Map<Integer, Long> todayStudiedTimeMap = new HashMap<>(); // 과목 ID별 총 공부 시간(분) 저장
    private ImageButton sortByButton;
    private TextView sortTextView;
    private int selectedSortType = 0;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_home, container, false);

        textNoData = view.findViewById(R.id.tv_empty_message);
        subjectListContainer = view.findViewById(R.id.subject_list_container);
        tvMarqueeNotifications = view.findViewById(R.id.tv_marquee_notifications);
        btnGoToSubjects = view.findViewById(R.id.btn_go_to_subjects);

        btnGoToSubjects.setOnClickListener(v -> {
            // MainActivity의 ViewPager2를 통해 페이지 이동
            if (getActivity() instanceof MainActivity) {
                ((MainActivity) getActivity()).moveToSubjectPage();
            }
        });

        // 정렬 관련 UI 요소 바인딩
        sortByButton = view.findViewById(R.id.sort_by_button);
        sortTextView = view.findViewById(R.id.sort_text_view);

        ImageView imgProfile = view.findViewById(R.id.img_profile);
        // 프로필 창 진입
        imgProfile.setOnClickListener(v -> {
            Intent intent = new Intent(requireContext(), ProfileActivity.class);
            startActivity(intent);
        });

        // ViewModel 초기화
        AppDatabase db = AppDatabase.getInstance(requireActivity().getApplication());
        StudySessionDao sessionDao = db.studySessionDao();
        SubjectDao subjectDaoForFactory = db.subjectDao(); // ViewModel Factory가 SubjectDao도 받는 경우 대비
        StudySessionViewModelFactory factory = new StudySessionViewModelFactory(requireActivity().getApplication(), sessionDao, subjectDaoForFactory);
        studySessionViewModel = new ViewModelProvider(this, factory).get(StudySessionViewModel.class);

        observeTodaysStudySessions(); // 공부 시간 데이터 관찰 시작
        setupSortButton(); // 정렬 버튼 설정 메소드 호출
        updateSortTextView(); // 초기 정렬 텍스트 설정

        return view;
    }

    private void observeTodaysStudySessions() {
        studySessionViewModel.getScheduleSessionLiveData().observe(getViewLifecycleOwner(), todaysSessions -> {
            if (todaysSessions != null) {
                Log.d(TAG, "오늘의 학습 세션 데이터 업데이트됨. 세션 수: " + todaysSessions.size());
                todayStudiedTimeMap.clear();
                for (StudySessionEntity session : todaysSessions) {
                    // 오늘 날짜의 세션만 집계 (loadScheduleItemsByDate가 이미 날짜 필터링하지만, 한번 더 확인 가능)
                    if (session.getDate().equals(LocalDate.now())) {
                        int subjectId = session.getSubjectId();
                        long currentTotal = todayStudiedTimeMap.getOrDefault(subjectId, 0L);
                        todayStudiedTimeMap.put(subjectId, currentTotal + session.getStudyTime()); // studyTime은 분 단위로 가정
                    }
                }
                Log.d(TAG, "오늘의 과목별 공부 시간 집계 완료: " + todayStudiedTimeMap.toString());
                // 공부 시간 데이터가 변경되었으므로, 과목 목록 UI를 다시 그림
                updateSubjectList();
            } else {
                Log.d(TAG, "오늘의 학습 세션 데이터가 null입니다.");
                todayStudiedTimeMap.clear();
                updateSubjectList(); // 빈 데이터로라도 UI 갱신
            }
        });
    }
    private void setupSortButton() {
        sortByButton.setOnClickListener(v -> {
            PopupMenu popup = new PopupMenu(getContext(), sortByButton);
            popup.getMenuInflater().inflate(R.menu.searchbar_menu_home, popup.getMenu()); // 메뉴 리소스 파일명 확인
            popup.setOnMenuItemClickListener(item -> {
                int itemId = item.getItemId();
                if (itemId == R.id.sort_by_name_home) {
                    selectedSortType = 0;
                } else if (itemId == R.id.sort_by_study_time_home) {
                    selectedSortType = 1;
                } else if (itemId == R.id.sort_by_target_time_home) {
                    selectedSortType = 2;
                } else {
                    return false;
                }
                updateSortTextView();
                updateSubjectList(); // 정렬 기준 변경 후 목록 새로고침
                return true;
            });
            popup.show();
        });
    }

    private void updateSortTextView() {
        String sortText = "정렬: ";
        switch (selectedSortType) {
            case 0: default: sortText += "과목명"; break;
            case 1: sortText += "일일 공부 시간순"; break;
            case 2: sortText += "일일 목표 시간순"; break;
        }
        sortTextView.setText(sortText);
    }

    /* 추가된 과목 여부에 따라 텍스트와 버튼 또는 추가된 과목 표시 */
    private void updateSubjectList() {
        if (getActivity() == null || !(getActivity() instanceof MainActivity)) {
            Log.w(TAG, "Activity is not MainActivity or is null, cannot update subject list.");
            return;
        }

        List<SubjectEntity> subjectsFromActivity = ((MainActivity) requireActivity()).getSelectedSubjects();
        if (subjectsFromActivity == null) { // 방어 코드
            subjectsFromActivity = new ArrayList<>();
        }
        // 정렬을 위해 새로운 리스트에 복사
        List<SubjectEntity> subjects = new ArrayList<>(subjectsFromActivity);

        Log.d(TAG, "updateSubjectList called. SOriginal subjects count: " + subjects.size() + ", SortType: " + selectedSortType);
        subjectListContainer.removeAllViews();

        if (subjects == null || subjects.isEmpty()) {
            textNoData.setVisibility(View.VISIBLE);
            btnGoToSubjects.setVisibility(View.VISIBLE);
            subjectListContainer.setVisibility(View.GONE);
            updateMarquee(new ArrayList<>()); // 과목 없을 시 전광판도 비움
        } else {
            textNoData.setVisibility(View.GONE);
            btnGoToSubjects.setVisibility(View.GONE);
            subjectListContainer.setVisibility(View.VISIBLE);

            // 정렬 로직 적용
            switch (selectedSortType) {
                case 0: // 이름 오름차순
                    Collections.sort(subjects, Comparator.comparing(SubjectEntity::getName, String.CASE_INSENSITIVE_ORDER));
                    break;
                case 1: // 오늘 공부 시간 내림차순 (많은 순)
                    Collections.sort(subjects, (s1, s2) -> {
                        long time1 = todayStudiedTimeMap.getOrDefault(s1.getSubjectId(), 0L);
                        long time2 = todayStudiedTimeMap.getOrDefault(s2.getSubjectId(), 0L);
                        return Long.compare(time2, time1); // 내림차순
                    });
                    break;
                case 2: // 일일 목표 시간 내림차순 (많은 순)
                    Collections.sort(subjects, (s1, s2) -> {
                        int target1 = (s1.getTime() != null) ? s1.getTime().getDailyTargetStudyTime() : 0;
                        int target2 = (s2.getTime() != null) ? s2.getTime().getDailyTargetStudyTime() : 0;
                        return Integer.compare(target2, target1); // 내림차순
                    });
                    break;
            }

            LayoutInflater inflater = LayoutInflater.from(getContext());
            List<String> unmetTargetSubjects = new ArrayList<>(); // 목표 미달성 과목명 리스트

            for (SubjectEntity subject : subjects) {
                // item_subject_list_home.xml을 인플레이트
                View subjectItemView = inflater.inflate(R.layout.item_subject_list_home, subjectListContainer, false);

                // UI 요소 바인딩
                ImageView colorCircle = subjectItemView.findViewById(R.id.color_circle);
                TextView subjectName = subjectItemView.findViewById(R.id.subject_name);
                ImageButton expandButton = subjectItemView.findViewById(R.id.expand_img_btn);
                GridLayout expandLayoutTime = subjectItemView.findViewById(R.id.layout_expand); // 시간 정보 GridLayout
                GridLayout expandLayoutButtons = subjectItemView.findViewById(R.id.layout_expand_button); // 버튼 GridLayout

                TextView studyTimeTV = subjectItemView.findViewById(R.id.study_time); // XML ID는 study_time
                TextView targetStudyTimeTV = subjectItemView.findViewById(R.id.target_study_time);

                Button btnRecord = subjectItemView.findViewById(R.id.btn_record_time);
                Button btnAnalysis = subjectItemView.findViewById(R.id.btn_view_analysis);

                // 데이터 설정
                // 1. 색상 원 설정: SubjectEntity에 저장된 color 값 사용
                try {
                    if (subject.getColor() != null && !subject.getColor().isEmpty()) {
                        GradientDrawable background = (GradientDrawable) ContextCompat.getDrawable(requireContext(), R.drawable.color_circle).mutate();
                        background.setColor(Color.parseColor(subject.getColor())); // 저장된 색상 사용
                        colorCircle.setBackground(background);
                    } else {
                        // SubjectEntity에 color 값이 없는 경우 (이론상 SubjectAddFragment에서 항상 설정해줌)
                        GradientDrawable background = (GradientDrawable) ContextCompat.getDrawable(requireContext(), R.drawable.color_circle).mutate();
                        background.setColor(Color.LTGRAY); // 기본 색상
                        colorCircle.setBackground(background);
                        Log.w(TAG, "Subject " + subject.getName() + " has no color, using default LTGRAY.");
                    }
                } catch (IllegalArgumentException e) {
                    Log.e(TAG, "Invalid color format: " + subject.getColor(), e);
                    GradientDrawable background = (GradientDrawable) ContextCompat.getDrawable(requireContext(), R.drawable.color_circle).mutate();
                    background.setColor(Color.GRAY); // 오류 시 회색
                    colorCircle.setBackground(background);
                }

                // 2. 과목명 설정
                subjectName.setText(subject.getName());

                // 3. 확장 레이아웃 시간 데이터 설정 (시간/분 형식으로 변경)
                long actualStudiedTimeMinutesLong = todayStudiedTimeMap.getOrDefault(subject.getSubjectId(), 0L);
                int actualStudiedTimeMinutes = (int) actualStudiedTimeMinutesLong;
                studyTimeTV.setText(formatMinutesToHoursAndMinutes(actualStudiedTimeMinutes));
                Log.d(TAG, "Subject: " + subject.getName() + ", Today's Studied Time: " + actualStudiedTimeMinutes + " mins");

                TargetStudyTime targetTime = subject.getTime();
                int dailyTargetMinutes = 0;
                if (targetTime != null) {
                    dailyTargetMinutes = targetTime.getDailyTargetStudyTime(); // 분 단위 가정
                    targetStudyTimeTV.setText(formatMinutesToHoursAndMinutes(dailyTargetMinutes));
                } else {
                    targetStudyTimeTV.setText("미설정");
                }

                // 4. 확장/축소 기능 설정
                final boolean[] isExpandedState = {subject.isExpanded()};
                expandLayoutTime.setVisibility(isExpandedState[0] ? View.VISIBLE : View.GONE);
                expandLayoutButtons.setVisibility(isExpandedState[0] ? View.VISIBLE : View.GONE); // 버튼 레이아웃도 함께 토글
                expandButton.setImageResource(isExpandedState[0] ? R.drawable.ic_expand_less : R.drawable.ic_expand_more); // ic_expand_more 사용 (이전 파일명과 동일 가정)

                View.OnClickListener expandClickListener = v -> {
                    isExpandedState[0] = !isExpandedState[0];
                    subject.setExpanded(isExpandedState[0]);
                    expandLayoutTime.setVisibility(isExpandedState[0] ? View.VISIBLE : View.GONE);
                    expandLayoutButtons.setVisibility(isExpandedState[0] ? View.VISIBLE : View.GONE);
                    expandButton.setImageResource(isExpandedState[0] ? R.drawable.ic_expand_less : R.drawable.ic_expand_more);
                };
                expandButton.setOnClickListener(expandClickListener);
                subjectItemView.setOnClickListener(expandClickListener);

                // 5. 기록/분석 버튼 리스너 설정
                if (btnRecord != null) {
                    btnRecord.setOnClickListener(v -> {
                        if (getActivity() instanceof MainActivity) {
                            Toast.makeText(getContext(), subject.getName() + " 기록 화면으로 이동합니다.", Toast.LENGTH_SHORT).show();
                            ((MainActivity) getActivity()).navigateToFragment(MainActivity.TIME_FRAGMENT, subject.getSubjectId());
                        }
                    });
                } else {
                    Log.e(TAG, "btn_record_time not found in item_subject_list_home.xml. Did you add the ID?");
                }

                if (btnAnalysis != null) {
                    btnAnalysis.setOnClickListener(v -> {
                        if (getActivity() instanceof MainActivity) {
                            Toast.makeText(getContext(), subject.getName() + " 분석 화면으로 이동합니다.", Toast.LENGTH_SHORT).show();
                            ((MainActivity) getActivity()).navigateToFragment(MainActivity.ANALYSIS_FRAGMENT, subject.getSubjectId());
                        }
                    });
                } else {
                    Log.e(TAG, "btn_view_analysis not found in item_subject_list_home.xml. Did you add the ID?");
                }

                subjectListContainer.addView(subjectItemView);

                // 전광판 메시지용 데이터 수집
                if (targetTime != null && dailyTargetMinutes > 0 && actualStudiedTimeMinutes < dailyTargetMinutes) {
                    if (subject.getName() != null && !subject.getName().isEmpty()) {
                        unmetTargetSubjects.add(subject.getName());
                    }
                }
            }
            // 전광판 내용 설정
            updateMarquee(unmetTargetSubjects);
        }
    }

    // 분을 "X 시간 Y 분" 또는 "Y 분" 형식으로 변환하는 헬퍼 메소드
    private String formatMinutesToHoursAndMinutes(int totalMinutes) {
        if (totalMinutes < 0) totalMinutes = 0; // 음수 방지
        int hours = totalMinutes / 60;
        int minutes = totalMinutes % 60;

        if (hours > 0) {
            return String.format(Locale.getDefault(), "%d 시간 %d 분", hours, minutes);
        } else {
            return String.format(Locale.getDefault(), "%d 분", minutes);
        }
    }

    private void updateMarquee(List<String> unmetSubjects) {
        if (unmetSubjects == null || unmetSubjects.isEmpty()) {
            tvMarqueeNotifications.setVisibility(View.GONE);
            tvMarqueeNotifications.setText(""); // 텍스트 클리어
            Log.d(TAG, "No unmet target subjects, marquee hidden.");
            return;
        }

        StringBuilder marqueeTextBuilder = new StringBuilder();
        for (int i = 0; i < unmetSubjects.size(); i++) {
            marqueeTextBuilder.append(unmetSubjects.get(i));
            if (i < unmetSubjects.size() - 1) {
                marqueeTextBuilder.append(", ");
            }
        }
        marqueeTextBuilder.append("의 일일 목표 시간이 아직 달성되지 않았어요!");
        String originalText = marqueeTextBuilder.toString();
        String finalTextToDisplay = originalText;

        // 텍스트가 짧을 경우 반복하여 길게 만들기 위한 설정
        // 이 값들은 화면 크기, 폰트 크기에 따라 조절이 필요할 수 있습니다.
        final int SHORT_TEXT_CHARACTER_THRESHOLD = 70; // 이 길이 미만이면 짧다고 간주
        final int REPEAT_COUNT_FOR_SHORT_TEXT = 3;     // 짧은 텍스트 반복 횟수
        final String TEXT_REPEAT_SEPARATOR = "          "; // 반복되는 텍스트 사이의 구분자 (공백 여러 개)

        // 실제 렌더링될 너비를 예측하기는 어려우므로, 문자열 길이를 기준으로 판단
        if (originalText.length() < SHORT_TEXT_CHARACTER_THRESHOLD && originalText.length() > 0) {
            StringBuilder repeatedTextBuilder = new StringBuilder();
            for (int i = 0; i < REPEAT_COUNT_FOR_SHORT_TEXT; i++) {
                repeatedTextBuilder.append(originalText);
                if (i < REPEAT_COUNT_FOR_SHORT_TEXT - 1) {
                    repeatedTextBuilder.append(TEXT_REPEAT_SEPARATOR);
                }
            }
            finalTextToDisplay = repeatedTextBuilder.toString();
            Log.d(TAG, "Original marquee text was short. Repeated text: " + finalTextToDisplay);
        }

        tvMarqueeNotifications.setText(finalTextToDisplay);
        tvMarqueeNotifications.setVisibility(View.VISIBLE);
        tvMarqueeNotifications.setSelected(true);
        Log.d(TAG, "Marquee text set: " + finalTextToDisplay);
    }

    /* 화면 돌아올 때마다 과목 갱신 */
    @Override
    public void onResume() {
        super.onResume();
        Log.d(TAG, "onResume called. Requesting today's study sessions.");
        // 오늘 날짜의 세션 데이터를 ViewModel에 요청
        if (studySessionViewModel != null) {
            studySessionViewModel.loadScheduleItemsByDate(LocalDate.now());
        }
    }
}