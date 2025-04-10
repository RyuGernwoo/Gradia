package mp.gradia.home;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import java.util.List;

import mp.gradia.R;
import mp.gradia.main.MainActivity;
import mp.gradia.subject.Subject;

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

        updateSubjectList();
        return view;
    }
    
    /* 추가된 과목 여부에 따라 텍스트와 버튼 또는 추가된 과목 표시 */
    private void updateSubjectList() {
        List<Subject> subjects = ((MainActivity) requireActivity()).getSelectedSubjects();

        subjectListContainer.removeAllViews();

        if (subjects.isEmpty()) {
            textNoData.setVisibility(View.VISIBLE);
            btnGoToSubjects.setVisibility(View.VISIBLE);
            subjectListContainer.setVisibility(View.GONE);
        } else {
            textNoData.setVisibility(View.GONE);
            btnGoToSubjects.setVisibility(View.GONE);
            subjectListContainer.setVisibility(View.VISIBLE);

            for (Subject subject : subjects) {
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