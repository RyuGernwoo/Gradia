package mp.gradia.main;

import android.os.Bundle;
import android.util.Log;
import android.view.View;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.viewpager2.widget.ViewPager2;

import com.google.android.material.bottomnavigation.BottomNavigationView;


// RxJava 및 Room 관련 import
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.disposables.CompositeDisposable;
import io.reactivex.rxjava3.schedulers.Schedulers;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import mp.gradia.R;
import mp.gradia.api.ApiHelper;
import mp.gradia.api.AuthManager;
import mp.gradia.api.models.AuthResponse;
import mp.gradia.database.AppDatabase;
import mp.gradia.database.dao.SubjectDao;
import mp.gradia.database.entity.SubjectEntity;

public class MainActivity extends AppCompatActivity {
    private static final String TAG = "MainActivity";
    private ViewPager2 viewPager;
    public static final int HOME_FRAGMENT = 0;
    public static final int SUBJECT_FRAGMENT = 1;
    public static final int TIME_FRAGMENT = 2;
    public static final int ANALYSIS_FRAGMENT = 3;


    private ApiHelper apiHelper;
    private AuthManager authManager;

    private static boolean processInitialized = false; // 사용 안하는 듯?

    // SubjectDao 및 데이터 관찰용
    private SubjectDao subjectDao;
    private final CompositeDisposable disposables = new CompositeDisposable();
    private List<SubjectEntity> currentSubjectsList = new ArrayList<>(); // DB에서 관찰한 최신 과목 목록

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);

        // API Helper와 Auth Manager 초기화
        apiHelper = new ApiHelper(this);
        authManager = AuthManager.getInstance(this);

        // 로그인 상태 확인 및 토큰 갱신
        // 백엔드 로직 미구현으로 주석처리
        // checkLoginStatusAndRefreshToken();

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(0, systemBars.top, 0, 0);
            return insets;
        });

        // SubjectDao 인스턴스 가져오기
        subjectDao = AppDatabase.getInstance(getApplicationContext()).subjectDao();

        // DB에서 모든 과목 목록 관찰 시작
        observeAllSubjects();
        // ViewPager
        viewPager = findViewById(R.id.view_pager);
        // Set ViewPager Adapter
        viewPager.setAdapter(new MainFragmentAdapter(this));

        // BottomNavigationView
        BottomNavigationView bottomNavigation = findViewById(R.id.bottom_navigation);
        // BottonNavigationView Event Listener
        bottomNavigation.setOnItemSelectedListener(item -> {
            // Navigate to the corresponding fragment page when a tab is selected
            // 0: HomeFragment, 1: SubjectFragment, 2: TimeFragment, 3: AnalysisFragment
            if (item.getItemId() == R.id.nav_home)
                viewPager.setCurrentItem(HOME_FRAGMENT, true);
            else if (item.getItemId() == R.id.nav_subject)
                viewPager.setCurrentItem(SUBJECT_FRAGMENT, true);
            else if (item.getItemId() == R.id.nav_time)
                viewPager.setCurrentItem(TIME_FRAGMENT, true);
            else if (item.getItemId() == R.id.nav_analysis)
                viewPager.setCurrentItem(ANALYSIS_FRAGMENT, true);
            else
                return false;

            Log.d("MainActivity", "test");
            return true;
        });

        // Sync BottomNavigationView selection when ViewPager2 page changes
        viewPager.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageSelected(int position) {
                // Reflect current page on BottomNavigationView
                // 0: Home, 1: Subject, 2: Time, 3: Analysis
                switch (position) {
                    case 0:
                        bottomNavigation.setSelectedItemId(R.id.nav_home);
                        break;
                    case 1:
                        bottomNavigation.setSelectedItemId(R.id.nav_subject);
                        break;
                    case 2:
                        bottomNavigation.setSelectedItemId(R.id.nav_time);
                        break;
                    case 3:
                        bottomNavigation.setSelectedItemId(R.id.nav_analysis);
                        break;
                }
            }
        });
    }

    /**
     * 로그인 상태를 확인하고 로그인되어 있다면 토큰을 갱신합니다.
     */
    private void checkLoginStatusAndRefreshToken() {
        if (authManager.isLoggedIn()) {
            Log.d(TAG, "사용자가 로그인된 상태입니다. 토큰을 갱신합니다.");
            apiHelper.refreshToken(new ApiHelper.ApiCallback<AuthResponse>() {
                @Override
                public void onSuccess(AuthResponse result) {
                    Log.d(TAG, "토큰 갱신 성공");
                }

                @Override
                public void onError(String errorMessage) {
                    Log.e(TAG, "토큰 갱신 실패: " + errorMessage);
                    // 토큰 갱신에 실패한 경우 로그아웃 처리할 수도 있습니다
                    // authManager.logout();
                }
            });
        } else {
            Log.d(TAG, "사용자가 로그인되지 않은 상태입니다.");
        }
    }

    // 모든 과목 목록을 DB에서 관찰하는 메소드
    private void observeAllSubjects() {
        disposables.add(subjectDao.getAll() // SubjectDao의 getAll() 호출
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(
                        subjects -> {
                            Log.i("MainActivity", "Subjects list updated from DB. Count: " + subjects.size());
                            currentSubjectsList.clear();
                            currentSubjectsList.addAll(subjects);
                            // HomeFragment가 현재 화면에 표시 중이라면, UI를 갱신하도록 알릴 수 있습니다.
                            // 예를 들어, LocalBroadcastManager를 사용하거나,
                            // ViewPager2의 현재 프래그먼트를 가져와 직접 메소드를 호출할 수 있습니다.
                            // 가장 간단한 방법은 HomeFragment의 onResume에서 목록을 다시 로드하는 것입니다.
                            // 만약 HomeFragment가 이미 활성화된 상태에서 이 콜백이 호출된다면,
                            // HomeFragment에 수동으로 UI 업데이트를 트리거해야 할 수 있습니다.
                            // 예: ((MainFragmentAdapter)viewPager.getAdapter()).getHomeFragmentInstance().refreshList();
                            // (MainFragmentAdapter에 해당 메소드 및 HomeFragment 인스턴스 반환 로직 필요)
                        },
                        throwable -> {
                            Log.e("MainActivity", "Error observing subjects from DB", throwable);
                        }
                ));
    }

    // MainActivity.java
    public void navigateToFragment(int fragmentDestination, int subjectId) {
        Bundle bundle = new Bundle();
        bundle.putInt("subject_id_for_fragment", subjectId); // 전달할 데이터 키
        Log.d(TAG, "navigateToFragment called for destination: " + fragmentDestination + " with subjectId: " + subjectId + ". Bundle created.");

        // ViewPager2의 아이템으로 직접 이동
        if (fragmentDestination == SUBJECT_FRAGMENT) { // SUBJECT_FRAGMENT 경우 추가
            viewPager.setCurrentItem(SUBJECT_FRAGMENT, true);
            // TODO: SubjectFragment가 subjectId를 사용하여 특정 과목 정보를 표시해야 하는 경우,
            //  MainFragmentAdapter 또는 SubjectFragment 자체에서 Bundle을 처리하는 로직 추가 필요.
            Log.d(TAG, "Navigating to SubjectFragment. subjectId: " + subjectId);
        } else if (fragmentDestination == TIME_FRAGMENT) {
            viewPager.setCurrentItem(TIME_FRAGMENT, true);
            // TODO: TimeFragment가 Bundle을 받도록 MainFragmentAdapter 또는 TimeFragment 수정 필요
            Log.d(TAG, "Navigating to TimeFragment with subject ID: " + subjectId);
        } else if (fragmentDestination == ANALYSIS_FRAGMENT) {
            viewPager.setCurrentItem(ANALYSIS_FRAGMENT, true);
            // TODO: AnalysisFragment가 Bundle을 받도록 MainFragmentAdapter 또는 AnalysisFragment 수정 필요
            Log.d(TAG, "Navigating to AnalysisFragment with subject ID: " + subjectId);
        } else if (fragmentDestination == HOME_FRAGMENT) { // 다른 프래그먼트로의 이동도 고려하여 추가 (예시)
            viewPager.setCurrentItem(HOME_FRAGMENT, true);
            Log.d(TAG, "Navigating to HomeFragment.");
        } else {
            Log.w(TAG, "Unknown fragment destination: " + fragmentDestination);
        }
        // 실제 데이터 전달은 ViewPager2 어댑터와 각 대상 Fragment의newInstance 패턴 또는 setArguments를 통해 이루어져야 합니다.
        // 간단하게는 ViewPager 아이템 변경 후, 해당 프래그먼트가 Activity로부터 데이터를 가져가도록 할 수도 있습니다.

    }

    @Override
    protected void onStop() {
        super.onStop();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        disposables.clear(); // RxJava 구독 해제 (메모리 누수 방지)
    }

    /* Home에 추가된 과목 없을 때 사용 */
    public void moveToSubjectPage() {
        viewPager.setCurrentItem(SUBJECT_FRAGMENT, true); // 1 = SubjectFragment 위치
    }

    /* HomeFragment에서 사용할 과목 목록 반환 메소드 */
    // 이전 addSubject 및 selectedSubjects 필드는 제거하고, DB에서 관찰하는 currentSubjectsList를 반환
    public List<SubjectEntity> getSelectedSubjects() {
        Log.d("MainActivity", "getSelectedSubjects called. Returning " + currentSubjectsList.size() + " subjects.");
        return new ArrayList<>(currentSubjectsList); // 방어적 복사본 반환
    }
}