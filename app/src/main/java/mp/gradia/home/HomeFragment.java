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
import android.widget.PopupWindow;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import java.util.List;
import java.util.Locale;

import mp.gradia.R;
import mp.gradia.main.MainActivity;
import mp.gradia.database.entity.SubjectEntity;

public class HomeFragment extends Fragment {
    private static final String TAG = "HomeFragment";
    private LinearLayout subjectListContainer;
    private TextView textNoData;
    private Button btnGoToSubjects;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_home, container, false);

        textNoData = view.findViewById(R.id.tv_empty_message);
        subjectListContainer = view.findViewById(R.id.subject_list_container);

        btnGoToSubjects = view.findViewById(R.id.btn_go_to_subjects);
        btnGoToSubjects.setOnClickListener(v -> {
            // MainActivity의 ViewPager2를 통해 페이지 이동
            if (getActivity() instanceof MainActivity) {
                ((MainActivity) getActivity()).moveToSubjectPage();
            }
        });

        ImageView imgProfile = view.findViewById(R.id.img_profile);
        ImageView imgNotifications = view.findViewById(R.id.img_notifications);

        // 알림 팝업
        imgNotifications.setOnClickListener(v -> {
            View popupView = LayoutInflater.from(getContext()).inflate(R.layout.popup_notifications, null);
            PopupWindow popupWindow = new PopupWindow(popupView,
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    true);

            popupWindow.setElevation(10f);
            popupWindow.showAsDropDown(v, -150, 20); // 위치 조정 가능
        });

        // 프로필 창 진입
        imgProfile.setOnClickListener(v -> {
            Intent intent = new Intent(requireContext(), ProfileActivity.class);
            startActivity(intent);
        });

        // updateSubjectList(); // onResume에서 호출됨
        return view;
    }
    
    /* 추가된 과목 여부에 따라 텍스트와 버튼 또는 추가된 과목 표시 */
    private void updateSubjectList() {
        if (getActivity() == null || !(getActivity() instanceof MainActivity)) {
            Log.w(TAG, "Activity is not MainActivity or is null, cannot update subject list.");
            return;
        }

        List<SubjectEntity> subjects = ((MainActivity) requireActivity()).getSelectedSubjects();
        subjectListContainer.removeAllViews(); // 기존 뷰들을 모두 제거

        if (subjects == null || subjects.isEmpty()) {
            textNoData.setVisibility(View.VISIBLE);
            btnGoToSubjects.setVisibility(View.VISIBLE);
            subjectListContainer.setVisibility(View.GONE);
        } else {
            textNoData.setVisibility(View.GONE);
            btnGoToSubjects.setVisibility(View.GONE);
            subjectListContainer.setVisibility(View.VISIBLE);

            LayoutInflater inflater = LayoutInflater.from(getContext());
            for (SubjectEntity subject : subjects) {
                // item_subject_list.xml을 인플레이트
                View subjectItemView = inflater.inflate(R.layout.item_subject_list, subjectListContainer, false);

                // UI 요소 바인딩
                ImageView colorCircle = subjectItemView.findViewById(R.id.color_circle);
                TextView subjectName = subjectItemView.findViewById(R.id.subject_name);
                ImageButton expandButton = subjectItemView.findViewById(R.id.expand_img_btn);
                GridLayout expandLayout = subjectItemView.findViewById(R.id.layout_expand);

                TextView studyTime = subjectItemView.findViewById(R.id.study_time);
                TextView targetStudyTime = subjectItemView.findViewById(R.id.target_study_time); // XML ID 오타 수정: taget_study_time -> target_study_time
                TextView subjectType = subjectItemView.findViewById(R.id.subject_type);
                TextView subjectCredit = subjectItemView.findViewById(R.id.subject_credit);

                // 데이터 설정
                // 1. 색상 원 설정
                try {
                    if (subject.getColor() != null && !subject.getColor().isEmpty()) { // [cite: 3]
                        GradientDrawable background = (GradientDrawable) ContextCompat.getDrawable(requireContext(), R.drawable.color_circle).mutate();
                        background.setColor(Color.parseColor(subject.getColor())); // [cite: 3]
                        colorCircle.setBackground(background);
                    }
                } catch (IllegalArgumentException e) {
                    Log.e(TAG, "Invalid color format for subject " + subject.getName() + ": " + subject.getColor(), e); // [cite: 3]
                    // 기본 색상 설정 또는 오류 처리
                    GradientDrawable background = (GradientDrawable) ContextCompat.getDrawable(requireContext(), R.drawable.color_circle).mutate();
                    background.setColor(Color.LTGRAY); // 예시: 잘못된 색상일 경우 회색
                    colorCircle.setBackground(background);
                }


                // 2. 과목명 설정
                subjectName.setText(subject.getName()); // [cite: 3]

                // 3. 확장 레이아웃 데이터 설정
                // TODO: 'study_time' (실제 공부한 시간) 데이터는 SubjectEntity에 없으므로, 별도 로직으로 가져와야 합니다. 임시로 "0 시간" 표시.
                studyTime.setText("0 시간");

                if (subject.getTime() != null) { // [cite: 3]
                    // TODO: 일간/주간/월간 목표 시간 중 어떤 것을 표시할지 결정 필요. 여기서는 일간 목표 시간을 사용.
                    targetStudyTime.setText(String.format(Locale.getDefault(), "%d 시간", subject.getTime().getDailyTargetStudyTime())); // [cite: 3]
                } else {
                    targetStudyTime.setText("미설정");
                }

                subjectType.setText(getSubjectTypeString(subject.getType())); // [cite: 3]
                subjectCredit.setText(String.format(Locale.getDefault(), "%d 학점", subject.getCredit())); // [cite: 3]

                // 4. 확장/축소 기능 설정
                expandLayout.setVisibility(View.GONE); // 기본적으로 숨김
                expandButton.setImageResource(R.drawable.outline_expand_more_black_24); // 기본 아이콘
                subject.setExpanded(false); // SubjectEntity에 확장 상태 저장 필드 추가 가정 [cite: 3]

                View.OnClickListener expandClickListener = v -> {
                    boolean isExpanded = !subject.isExpanded(); // [cite: 3]
                    subject.setExpanded(isExpanded); // [cite: 3]
                    expandLayout.setVisibility(isExpanded ? View.VISIBLE : View.GONE);
                    expandButton.setImageResource(isExpanded ? R.drawable.outline_expand_more_black_24 : R.drawable.outline_expand_more_black_24); // ic_expand_less 아이콘 필요
                };
                // 버튼과 과목명 영역 모두 클릭 시 확장/축소되도록 설정 (선택적)
                expandButton.setOnClickListener(expandClickListener);
                subjectItemView.setOnClickListener(expandClickListener); // 카드 전체 클릭 시에도 반응


                // 컨테이너에 추가된 과목 뷰 추가
                subjectListContainer.addView(subjectItemView);
            }
        }
    }

    // 과목 유형 int 값을 문자열로 변환
    private String getSubjectTypeString(int type) {
        switch (type) {
            case SubjectEntity.REQUIRED_SUBJECT: // [cite: 3]
                return getString(R.string.subject_type_required); // strings.xml에 정의된 값 사용 권장
            case SubjectEntity.ELECTIVE_SUBJECT: // [cite: 3]
                return getString(R.string.subject_type_elective);
            case SubjectEntity.LIB_SUBJECT: // [cite: 3]
                return getString(R.string.subject_type_liberal_arts);
            default:
                return getString(R.string.subject_type_unknown);
        }
    }

    /* 화면 돌아올 때마다 과목 있는지 갱신 */
    @Override
    public void onResume() {
        super.onResume();
        Log.d(TAG, "onResume called, updating subject list.");
        updateSubjectList();
    }
}