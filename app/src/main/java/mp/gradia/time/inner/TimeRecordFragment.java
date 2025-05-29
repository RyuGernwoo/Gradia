package mp.gradia.time.inner;

import android.Manifest;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.os.Build;
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

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.cardview.widget.CardView;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.viewpager2.widget.ViewPager2;

import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton;
import com.google.android.material.shape.CornerFamily;
import com.google.android.material.shape.ShapeAppearanceModel;

import java.util.Random;

import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.disposables.CompositeDisposable;
import io.reactivex.rxjava3.schedulers.Schedulers;
import mp.gradia.R;
import mp.gradia.database.AppDatabase;
import mp.gradia.database.dao.SubjectDao;
import mp.gradia.database.entity.SubjectEntity;
import mp.gradia.main.MainActivity;
import mp.gradia.time.inner.record.bottomsheet.adapter.OnSubjectSelectListener;
import mp.gradia.time.inner.record.bottomsheet.adapter.SubjectSelectBottomSheetFragment;
import mp.gradia.time.inner.record.dialog.SessionAddDialog;
import mp.gradia.time.inner.record.stopwatch.StopwatchService;
import mp.gradia.time.inner.record.stopwatch.TimerRecordStopwatchFragment;
import mp.gradia.time.inner.record.timer.TimeRecordTimerFragment;
import mp.gradia.time.inner.record.timer.TimerService;
import mp.gradia.time.inner.viewmodel.SubjectViewModel;
import mp.gradia.time.inner.viewmodel.SubjectViewModelFactory;

public class TimeRecordFragment extends Fragment implements OnSubjectSelectListener {
    // UI Components
    private TextView greetingTextView;
    private TextView selectedSubjectTextView;
    private ImageView cShape;
    private CardView subjectSelectBtn;
    private FrameLayout fragmentContainer;
    private ExtendedFloatingActionButton addSessionFab;
    private LinearLayout expandedMenu;
    private LinearLayout addTimerBtn, addStopwatchBtn, addEventBtn;

    // State Variables
    private int nowFragment;
    private boolean isMenuOpen;
    private SubjectSelectBottomSheetFragment modalBottomSheet;
    private boolean hasData = true;
    private SubjectEntity selectedSubject;

    // Database and ViewModel
    private AppDatabase db;
    private SubjectDao dao;
    private SubjectViewModel subjectViewModel;
    private final CompositeDisposable compositeDisposable = new CompositeDisposable();

    private void restoreState() {
        SharedPreferences timerPrefs = requireContext().getSharedPreferences(TimerService.PREFS_NAME, Context.MODE_PRIVATE);
        SharedPreferences stopwatchPrefs = requireContext().getSharedPreferences(StopwatchService.PREFS_NAME, Context.MODE_PRIVATE);
        boolean isTimerRunning = timerPrefs.getBoolean(TimerService.KEY_IS_RUNNING, false);
        boolean isStopwatchRunning = stopwatchPrefs.getBoolean(StopwatchService.KEY_IS_RUNNING, false);

        if (isTimerRunning) {
            int selectedSubjectId = timerPrefs.getInt(TimerService.KEY_SELECTED_SUBJECT_ID, -1);
            subjectViewModel.loadSubjectById(selectedSubjectId);

            boolean isPause = timerPrefs.getBoolean(TimerService.KEY_IS_PAUSED, false);
            if (isPause) {
                Fragment timerFragment = new TimeRecordTimerFragment();
                Bundle pausedState = new Bundle();
                pausedState.putBoolean(TimerService.KEY_IS_PAUSED, true);

                timerFragment.setArguments(pausedState);
                loadChildFragment(timerFragment);
            }
        } else if (isStopwatchRunning) {
            int selectedSubjectId = stopwatchPrefs.getInt(StopwatchService.KEY_SELECTED_SUBJECT_ID, -1);
            subjectViewModel.loadSubjectById(selectedSubjectId);

            boolean isPause = stopwatchPrefs.getBoolean(StopwatchService.KEY_IS_PAUSED, false);
            if (isPause) {
                Fragment stopwatchFragment = new TimerRecordStopwatchFragment();
                Bundle pausedState = new Bundle();
                pausedState.putBoolean(StopwatchService.KEY_IS_PAUSED, true);

                stopwatchFragment.setArguments(pausedState);
                loadChildFragment(stopwatchFragment);
            } else loadChildFragment(new TimerRecordStopwatchFragment());
        }
    }

    private ActivityResultLauncher<String> requestNotificationPermissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), isGranted -> {
            });

    private void requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            requestNotificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS);
        }
    }

    /**
     * 프래그먼트가 처음 생성될 때 호출됩니다. 데이터베이스와 ViewModel을 초기화합니다.
     */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        db = AppDatabase.getInstance(requireContext());
        dao = db.subjectDao();
        SubjectViewModelFactory factory = new SubjectViewModelFactory(dao);
        subjectViewModel = new ViewModelProvider(this, factory).get(SubjectViewModel.class);
        requestNotificationPermission();
    }

    /**
     * 프래그먼트의 UI를 생성하고 초기화합니다.
     */
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

    /**
     * 프래그먼트가 사용자에게 보일 때 호출됩니다. 인사말 메시지를 설정합니다.
     */
    @Override
    public void onResume() {
        super.onResume();
        setRandomGreetingMessage();
        restoreState();
    }

    /**
     * 프래그먼트가 일시 중지될 때 호출됩니다.
     */
    @Override
    public void onPause() {
        super.onPause();

    }

    /**
     * UI 컴포넌트들을 초기화합니다.
     */
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

    /**
     * 랜덤 인사말 메시지를 설정합니다.
     */
    private void setupGreeting() {
        setRandomGreetingMessage();
    }

    /**
     * 과목 선택 버튼 클릭 리스너를 설정하여 BottomSheet를 표시하거나 과목 추가 화면으로 이동합니다.
     */
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

    /**
     * 세션 추가 FAB 메뉴의 애니메이션 및 클릭 리스너를 설정합니다.
     */
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
        addEventBtn.setOnClickListener(v -> {
            SessionAddDialog dialog = new SessionAddDialog();
            dialog.show(getChildFragmentManager(), "SessionAddDialog");
        });
    }

    /**
     * FragmentContainer에 하위 프래그먼트를 로드합니다.
     */
    private void loadChildFragment(Fragment fragment) {
        getChildFragmentManager()
                .beginTransaction()
                .replace(R.id.fragment_container, fragment)
                .commit();
    }

    /**
     * 리소스에서 랜덤 인사말 메시지를 가져와 설정합니다.
     */
    private void setRandomGreetingMessage() {
        String[] messages = getResources().getStringArray(R.array.text_label_study_greeting_messages);
        int randomIdx = new Random().nextInt(messages.length);
        greetingTextView.setText(messages[randomIdx]);
    }

    /**
     * SubjectViewModel을 관찰하여 과목 목록의 변화에 따라 UI 상태를 업데이트합니다.
     */
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

                        selectedSubject = subjectList.get(0);
                        subjectViewModel.selectSubject(selectedSubject);
                        setSelectedSubject(selectedSubject);

                        if (getChildFragmentManager().findFragmentById(R.id.fragment_container) == null) {
                            loadChildFragment(new TimeRecordTimerFragment());
                        }
                    }
                });
    }

    /**
     * 과목 선택 BottomSheet를 표시합니다.
     */
    private void showBottomSheet() {
        if (modalBottomSheet == null) {
            modalBottomSheet = new SubjectSelectBottomSheetFragment();
            modalBottomSheet.setOnBottomSheetItemClickListener(this);
        }
        modalBottomSheet.show(getChildFragmentManager(), modalBottomSheet.TAG);
    }

    /**
     * BottomSheet에서 과목이 선택되었을 때 호출되는 콜백입니다. 선택된 과목을 설정하고 BottomSheet를 닫습니다.
     */
    @Override
    public void onBottomSheetItemClick(SubjectEntity item) {
        modalBottomSheet.dismiss();
        selectedSubject = item;
        subjectViewModel.selectSubject(selectedSubject);
        setSelectedSubject(selectedSubject);
    }

    /**
     * 프래그먼트 뷰가 소멸될 때 호출됩니다.
     */
    @Override
    public void onDestroyView() {
        super.onDestroyView();
        compositeDisposable.clear(); // Dispose RxJava disposables
    }

    /**
     * 선택된 과목 정보를 UI에 표시합니다 (과목 이름, 색상).
     */
    public void setSelectedSubject(SubjectEntity subject) {
        selectedSubjectTextView.setText(subject.getName());
        GradientDrawable drawable = (GradientDrawable) getResources().getDrawable(R.drawable.color_circle);
        drawable.setColor(Color.parseColor(subject.getColor()));
        cShape.setImageDrawable(drawable);
    }
}