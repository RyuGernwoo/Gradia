package mp.gradia.time.inner.bottomsheet.adapter;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;

import java.util.ArrayList;
import java.util.List;

import mp.gradia.R;
import mp.gradia.time.inner.bottomsheet.Subject;

//
public class SubjectSelectBottomSheetFragment extends BottomSheetDialogFragment {
    public static final String TAG = "ModalBottomSheet";
    private OnSubjectSelectListener listener;
    // RecyclerView Example
    private List<Subject> item;

    //
    public void setOnBottomSheetItemClickListener(OnSubjectSelectListener listener) {
        this.listener = listener;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.layout_bottom_sheet, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Setup BottomSheet behavior
        FrameLayout standardBottomSheet = view.findViewById(R.id.standard_bottom_sheet);
        BottomSheetBehavior<FrameLayout> standardBottomSheetBehavior = BottomSheetBehavior.from(standardBottomSheet);
        // Set BottomSheetBehavior attributes
        standardBottomSheetBehavior.setState(BottomSheetBehavior.STATE_EXPANDED);

        // NEED TROUBLE SHOOTING
        // BottomSheetBehavior에 존재하는 문제
        // coordinator layout에서 동작하는 BottomSheet의 하위 레이아웃(즉 내부 레이아웃인 framelayout) 뷰 자체를 아래로 잡아 당길 시에 bottomsheet가 dismiss되지 않고 그대로 framelayout만 사라짐
        // 해당 콜백으로 임시 방편을 구현 해 놓았지만, 여전히 문제 해결이 필요
        standardBottomSheetBehavior.addBottomSheetCallback(new BottomSheetBehavior.BottomSheetCallback() {
            @Override
            public void onStateChanged(@NonNull View bottomSheet, int newState) {
                if (standardBottomSheetBehavior.getState() == BottomSheetBehavior.STATE_HIDDEN) standardBottomSheetBehavior.setState(BottomSheetBehavior.STATE_EXPANDED);
            }
            @Override
            public void onSlide(@NonNull View bottomSheet, float slideOffset) {}
        });

        // RecyclerView for subject list in BottomSheetDiaglog
        RecyclerView rv = getView().findViewById(R.id.subject_list);
        rv.setLayoutManager(new LinearLayoutManager(getContext()));

        // RecyclerView example
        item = new ArrayList<>();
        item.add(new Subject("모바일 프로그래밍", 3, R.color.c1));
        item.add(new Subject("인공지능 개론", 3, R.color.c2));
        item.add(new Subject("AI 수학", 3, R.color.c3));
        item.add(new Subject("데이터 과학", 3, R.color.c4));
        item.add(new Subject("디지털 마케팅", 2, R.color.c5));
        item.add(new Subject("운영체제", 3, R.color.c6));

        // Setup RecyclerView
        // RecyclerView Adapter
        SubjectSelectAdapter adapter = new SubjectSelectAdapter(item, getContext(), item -> {
            if (listener != null) {
                listener.onBottomSheetItemClick(item);
            }
        });
        rv.setAdapter(adapter);
    }
}
