package mp.gradia.main;

import android.os.Bundle;
import android.util.Log;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.viewpager2.widget.ViewPager2;

import com.google.android.material.bottomnavigation.BottomNavigationView;

import java.util.ArrayList;
import java.util.List;

import mp.gradia.R;
import mp.gradia.api.ApiHelper;
import mp.gradia.api.AuthManager;
import mp.gradia.api.models.AuthResponse;
import mp.gradia.database.entity.SubjectEntity;

public class MainActivity extends AppCompatActivity {
    private static final String TAG = "MainActivity";
    private ViewPager2 viewPager;
    public static final int HOME_FRAGMENT = 0;
    public static final int SUBJECT_FRAGMENT = 1;
    public static final int TIME_FRAGMENT = 2;
    public static final int ANALYSIS_FRAGMENT = 3;

    private static boolean processInitialized = false;
    private ApiHelper apiHelper;
    private AuthManager authManager;

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

    @Override
    protected void onStop() {
        super.onStop();
    }

    /* Home에 추가된 과목 없을 때 사용 */
    public void moveToSubjectPage() {
        viewPager.setCurrentItem(SUBJECT_FRAGMENT, true); // 1 = SubjectFragment 위치
    }

    /* 과목 추가 (임시) */
    private final List<SubjectEntity> selectedSubjects = new ArrayList<>();

    public void addSubject(SubjectEntity subject) {
        selectedSubjects.add(subject);
        Log.d("MainActivity", "Subject 추가됨: " + subject.getName());
    }

    public List<SubjectEntity> getSelectedSubjects() {
        return selectedSubjects;
    }
}