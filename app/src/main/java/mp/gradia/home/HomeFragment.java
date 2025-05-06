package mp.gradia.home;

import android.os.Bundle;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.PopupWindow;
import android.widget.TextView;
import android.widget.Toast;
import android.content.Intent;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import java.util.List;

import mp.gradia.R;
import mp.gradia.database.entity.SubjectEntity;
import mp.gradia.main.MainActivity;

public class HomeFragment extends Fragment {
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

        updateSubjectList();
        return view;
    }
    
    /* 추가된 과목 여부에 따라 텍스트와 버튼 또는 추가된 과목 표시 */
    private void updateSubjectList() {
        List<SubjectEntity> subjects = ((MainActivity) requireActivity()).getSelectedSubjects();

        subjectListContainer.removeAllViews();

        if (subjects.isEmpty()) {
            textNoData.setVisibility(View.VISIBLE);
            btnGoToSubjects.setVisibility(View.VISIBLE);
            subjectListContainer.setVisibility(View.GONE);
        } else {
            textNoData.setVisibility(View.GONE);
            btnGoToSubjects.setVisibility(View.GONE);
            subjectListContainer.setVisibility(View.VISIBLE);

            for (SubjectEntity subject : subjects) {
                TextView tv = new TextView(requireContext());
                tv.setText(subject.getName());
                tv.setTextSize(18f);
                tv.setPadding(20, 20, 20, 20);
                subjectListContainer.addView(tv);
            }
        }
    }

    /* 화면 돌아올 때마다 과목 있는지 갱신 */
    @Override
    public void onResume() {
        super.onResume();
        updateSubjectList();
    }
}