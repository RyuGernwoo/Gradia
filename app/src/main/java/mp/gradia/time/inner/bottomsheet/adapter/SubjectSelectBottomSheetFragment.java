package mp.gradia.time.inner.bottomsheet.adapter;

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

import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.core.Scheduler;
import io.reactivex.rxjava3.disposables.CompositeDisposable;
import io.reactivex.rxjava3.schedulers.Schedulers;
import mp.gradia.R;
import mp.gradia.database.AppDatabase;
import mp.gradia.database.dao.SubjectDao;
import mp.gradia.database.entity.SubjectEntity;
import mp.gradia.time.inner.SubjectViewModel;
import mp.gradia.time.inner.SubjectViewModelFactory;
import mp.gradia.time.inner.bottomsheet.Subject;

//
public class SubjectSelectBottomSheetFragment extends BottomSheetDialogFragment {
    public static final String TAG = "ModalBottomSheet";
    private OnSubjectSelectListener listener;
    // RecyclerView Example
    private List<SubjectEntity> item;

    private AppDatabase db;
    private SubjectViewModel subjectViewModel;

    //
    public void setOnBottomSheetItemClickListener(OnSubjectSelectListener listener) {
        this.listener = listener;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        db = AppDatabase.getInstance(requireContext());

        SubjectDao dao = db.subjectDao();
        SubjectViewModelFactory factory = new SubjectViewModelFactory(dao);
        subjectViewModel = new ViewModelProvider(requireParentFragment(), factory).get(SubjectViewModel.class);
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

        // BottomSheetBehavior에 존재하는 문제
        // coordinator layout에서 동작하는 BottomSheet의 하위 레이아웃(즉 내부 레이아웃인 framelayout) 뷰 자체를 아래로 잡아 당길 시에 bottomsheet가 dismiss되지 않고 그대로 framelayout만 사라짐
        // 해당 콜백으로 임시 방편을 구현 해 놓았지만, 여전히 문제 해결이 필요
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

        // RecyclerView for subject list in BottomSheetDiaglog
        RecyclerView rv = getView().findViewById(R.id.subject_list);
        rv.setLayoutManager(new LinearLayoutManager(getContext()));

        item = new ArrayList<>();

        subjectViewModel.subjectListLiveData.observe(getViewLifecycleOwner(),
                subjectList -> {
                    Log.d("hasData", "ADD DATA");
                    item.addAll(subjectList);
                }
        );

        // Setup RecyclerView
        // RecyclerView Adapter
        SubjectSelectAdapter adapter = new SubjectSelectAdapter(item, getContext(), item -> {
            if (listener != null) {
                listener.onBottomSheetItemClick(item);
            }
        });
        rv.setAdapter(adapter);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
    }
}
