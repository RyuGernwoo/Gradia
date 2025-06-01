package mp.gradia.time.inner.record.bottomsheet.adapter;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;

import java.util.ArrayList;
import java.util.List;

import mp.gradia.R;
import mp.gradia.database.AppDatabase;
import mp.gradia.database.dao.SubjectDao;
import mp.gradia.database.entity.SubjectEntity;
import mp.gradia.subject.ui.SubjectAddDialog;
import mp.gradia.time.inner.viewmodel.SubjectViewModel;
import mp.gradia.time.inner.viewmodel.SubjectViewModelFactory;

public class SubjectSelectBottomSheetFragment extends BottomSheetDialogFragment {
    // Tag for Logging
    public static final String TAG = "ModalBottomSheet";

    // Listener and Data List
    private OnSubjectSelectListener listener;
    private List<SubjectEntity> item;

    // Database and ViewModel
    private AppDatabase db;
    private SubjectViewModel subjectViewModel;

    /**
     * BottomSheet 아이템 클릭 리스너를 설정합니다.
     */
    public void setOnBottomSheetItemClickListener(OnSubjectSelectListener listener) {
        this.listener = listener;
    }

    /**
     * 프래그먼트가 처음 생성될 때 호출됩니다. 데이터베이스와 ViewModel을 초기화합니다.
     */
    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        db = AppDatabase.getInstance(requireContext());
        SubjectDao dao = db.subjectDao();
        SubjectViewModelFactory factory = new SubjectViewModelFactory(dao);
        subjectViewModel = new ViewModelProvider(requireParentFragment(), factory).get(SubjectViewModel.class);
    }

    /**
     * 프래그먼트의 UI를 생성하고 초기화합니다. BottomSheet 레이아웃을 인플레이트합니다.
     */
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.layout_bottom_sheet, container, false);
    }

    /**
     * 뷰가 생성된 후 호출됩니다. BottomSheet의 동작, RecyclerView 및 데이터 바인딩을 설정합니다.
     */
    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // BottomSheet 동작 설정
        FrameLayout standardBottomSheet = view.findViewById(R.id.standard_bottom_sheet);
        BottomSheetBehavior<FrameLayout> standardBottomSheetBehavior = BottomSheetBehavior.from(standardBottomSheet);
        standardBottomSheetBehavior.setState(BottomSheetBehavior.STATE_EXPANDED);

        // BottomSheetBehavior 콜백 추가
        standardBottomSheetBehavior.addBottomSheetCallback(new BottomSheetBehavior.BottomSheetCallback() {
            @Override
            public void onStateChanged(@NonNull View bottomSheet, int newState) {
                if (standardBottomSheetBehavior.getState() == BottomSheetBehavior.STATE_HIDDEN)
                    standardBottomSheetBehavior.setState(BottomSheetBehavior.STATE_EXPANDED);
            }

            @Override
            public void onSlide(@NonNull View bottomSheet, float slideOffset) {
            }
        });

        // RecyclerView 설정
        RecyclerView rv = getView().findViewById(R.id.subject_list);
        rv.setLayoutManager(new LinearLayoutManager(getContext()));

        item = new ArrayList<>();

        // ViewModel을 관찰하여 과목 목록 업데이트
        subjectViewModel.subjectListLiveData.observe(getViewLifecycleOwner(),
                subjectList -> {
                    Log.d("hasData", "ADD DATA");
                    item.clear();
                    item.addAll(subjectList);
                }
        );

        // RecyclerView 어댑터 설정
        SubjectSelectAdapter adapter = new SubjectSelectAdapter(item, getContext(), item -> {
            if (listener != null) {
                listener.onBottomSheetItemClick(item);
            }
        }, subjectId -> {
            Bundle bundle = new Bundle();
            bundle.putInt("subjectId", subjectId);
            SubjectAddDialog dialog = SubjectAddDialog.newInstance(bundle);
            dialog.show(getParentFragmentManager(), "SubjectAddDialog");
            dismiss();
        }
        );
        rv.setAdapter(adapter);
    }

    /**
     * 프래그먼트 뷰가 소멸될 때 호출됩니다.
     */
    @Override
    public void onDestroyView() {
        super.onDestroyView();
    }
}