package mp.gradia.subject;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import mp.gradia.main.MainActivity;

import mp.gradia.R;

/* (임시) */
public class SubjectFragment extends Fragment {
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_subject, container, false);

        Button addSubjectBtn = view.findViewById(R.id.btn_add_subject);
        addSubjectBtn.setOnClickListener(v -> {
            Subject newSubject = new Subject("수학"); // 임의로 선택
            ((MainActivity) requireActivity()).addSubject(newSubject);
        });

        return view;
    }
}
