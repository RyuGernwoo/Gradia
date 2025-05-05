package mp.gradia.time.inner;

import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AnimationUtils;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.cardview.widget.CardView;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.viewpager2.widget.ViewPager2;

import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton;
import com.google.android.material.shape.CornerFamily;
import com.google.android.material.shape.ShapeAppearanceModel;

import java.util.Random;

import mp.gradia.R;
import mp.gradia.database.AppDatabase;
import mp.gradia.database.dao.SubjectDao;
import mp.gradia.database.entity.SubjectEntity;
import mp.gradia.main.MainActivity;
import mp.gradia.time.inner.bottomsheet.adapter.OnSubjectSelectListener;
import mp.gradia.time.inner.bottomsheet.adapter.SubjectSelectBottomSheetFragment;
import mp.gradia.time.inner.stopwatch.TimerRecordStopwatchFragment;
import mp.gradia.time.inner.timer.TimeRecordTimerFragment;

public class TimeRecordFragment extends Fragment implements OnSubjectSelectListener {

    private TextView greetingTextView;
    private TextView selectedSubjectTextView;
    private ImageView cShape;

    private CardView subjectSelectBtn;
    private FrameLayout fragmentContainer;
    private ExtendedFloatingActionButton addSessionFab;
    private LinearLayout expandedMenu;
    private LinearLayout addTimerBtn, addStopwatchBtn, addEventBtn;

    private boolean isMenuOpen;
    private SubjectSelectBottomSheetFragment modalBottomSheet;

    private AppDatabase db;
    private SubjectViewModel subjectViewModel;
    private boolean hasData = true;
    private SubjectEntity selectedSubject;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        db = AppDatabase.getInstance(requireContext());
        SubjectDao dao = db.subjectDao();
        SubjectViewModelFactory factory = new SubjectViewModelFactory(dao);
        subjectViewModel = new ViewModelProvider(this, factory).get(SubjectViewModel.class);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_time_record, container, false);

        initViews(v);
        setupGreeting();
        setupSubjectViewModel();
        setupBottomSheet();
        setupFabMenu();

        return v;
    }

    @Override
    public void onResume() {
        super.onResume();
        setRandomGreetingMessage();
    }

    private void initViews(View v) {
        greetingTextView = v.findViewById(R.id.greeting_textview);
        selectedSubjectTextView = v.findViewById(R.id.selected_subject_textview);
        cShape = v.findViewById(R.id.color_circle);

        fragmentContainer = v.findViewById(R.id.fragment_container);

        subjectSelectBtn = v.findViewById(R.id.subject_select_btn);
        addSessionFab = v.findViewById(R.id.add_session_fab);
        expandedMenu = v.findViewById(R.id.expanded_menu);
        addTimerBtn = v.findViewById(R.id.add_timer_btn);
        addStopwatchBtn = v.findViewById(R.id.add_stopwatch_btn);
        addEventBtn = v.findViewById(R.id.add_event_btn);
    }

    private void setupGreeting() {
        setRandomGreetingMessage();
    }

    private void setupBottomSheet() {
        subjectSelectBtn.setOnClickListener(v -> {
            if (!hasData) {
                ViewPager2 viewPager = getActivity().findViewById(R.id.view_pager);
                viewPager.setCurrentItem(MainActivity.SUBJECT_FRAGMENT, true);
            } else {
                showBottomSheet();
                Log.d("hasData", "setup -" + String.valueOf(hasData));
            }
        });
    }

    private void setupFabMenu() {
        ShapeAppearanceModel defaultShape = new ShapeAppearanceModel.Builder()
                .setAllCornerSizes(56F)
                .build();

        ShapeAppearanceModel expandShape = new ShapeAppearanceModel.Builder()
                .setTopLeftCorner(CornerFamily.ROUNDED, 0F)
                .setTopRightCorner(CornerFamily.ROUNDED, 0F)
                .setBottomLeftCorner(CornerFamily.ROUNDED, 56F)
                .setBottomRightCorner(CornerFamily.ROUNDED, 56F)
                .build();

        addSessionFab.setOnClickListener(v -> {
            if (isMenuOpen) {
                addSessionFab.setShapeAppearanceModel(defaultShape);
                expandedMenu.startAnimation(AnimationUtils.loadAnimation(getContext(), R.anim.slide_out));
                expandedMenu.setVisibility(View.GONE);
            } else {
                addSessionFab.setShapeAppearanceModel(expandShape);
                expandedMenu.startAnimation(AnimationUtils.loadAnimation(getContext(), R.anim.slide_in));
                expandedMenu.setVisibility(View.VISIBLE);
            }
            isMenuOpen = !isMenuOpen;
        });

        addTimerBtn.setOnClickListener(v -> loadChildFragment(new TimeRecordTimerFragment()));
        addStopwatchBtn.setOnClickListener(v -> loadChildFragment(new TimerRecordStopwatchFragment()));
    }

    private void loadChildFragment(Fragment fragment) {
        getChildFragmentManager()
                .beginTransaction()
                .replace(R.id.fragment_container, fragment)
                .commit();
    }

    private void setRandomGreetingMessage() {
        String[] messages = getResources().getStringArray(R.array.text_label_study_greeting_messages);
        int randomIdx = new Random().nextInt(messages.length);
        greetingTextView.setText(messages[randomIdx]);
    }

    private void setupSubjectViewModel() {
        subjectViewModel.subjectListLiveData.observe(getViewLifecycleOwner(),
                subjectList -> {
                    if (subjectList.isEmpty()) {
                        hasData = false;
                        fragmentContainer.setClickable(false);
                        fragmentContainer.setAlpha(0.5F);
                        addSessionFab.setClickable(false);
                        addSessionFab.setAlpha(0.5F);
                        Log.d("hasData", "DATA " + String.valueOf(hasData));
                        Toast.makeText(requireContext(), R.string.toast_message_no_subjects, Toast.LENGTH_LONG).show();
                    } else {
                        hasData = true;
                        Log.d("hasData", "DATA " + String.valueOf(hasData));
                        fragmentContainer.setClickable(true);
                        fragmentContainer.setAlpha(1F);
                        addSessionFab.setClickable(true);
                        addSessionFab.setAlpha(1F);

                        if (getChildFragmentManager().findFragmentById(R.id.fragment_container) == null) {
                            selectedSubject = subjectList.get(0);
                            subjectViewModel.selectSubject(selectedSubject);

                            selectedSubjectTextView.setText(selectedSubject.getName());
                            GradientDrawable drawable = (GradientDrawable) getResources().getDrawable(R.drawable.color_circle);
                            drawable.setColor(Color.parseColor(selectedSubject.getColor()));
                            cShape.setImageDrawable(drawable);
                            loadChildFragment(new TimeRecordTimerFragment());
                        }
                    }
                });
    }

    private void showBottomSheet() {
        if (modalBottomSheet == null) {
            modalBottomSheet = new SubjectSelectBottomSheetFragment();
            modalBottomSheet.setOnBottomSheetItemClickListener(this);
        }
        modalBottomSheet.show(getChildFragmentManager(), modalBottomSheet.TAG);
    }

    @Override
    public void onBottomSheetItemClick(SubjectEntity item) {
        modalBottomSheet.dismiss();
        selectedSubject = item;
        subjectViewModel.selectSubject(selectedSubject);

        selectedSubjectTextView.setText(selectedSubject.getName());
        GradientDrawable drawable = (GradientDrawable) getResources().getDrawable(R.drawable.color_circle);
        drawable.setColor(Color.parseColor(selectedSubject.getColor()));
        cShape.setImageDrawable(drawable);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
    }
}
