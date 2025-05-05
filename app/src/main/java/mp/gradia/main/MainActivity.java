package mp.gradia.main;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import androidx.viewpager2.widget.ViewPager2;

import com.google.android.material.bottomnavigation.BottomNavigationView;

import java.util.ArrayList;
import java.util.List;

import mp.gradia.R;
import mp.gradia.database.entity.SubjectEntity;
import mp.gradia.time.inner.timer.TimerService;

public class MainActivity extends AppCompatActivity {
    private ViewPager2 viewPager;
    public static final int HOME_FRAGMENT = 0;
    public static final int SUBJECT_FRAGMENT = 1;
    public static final int TIME_FRAGMENT = 2;
    public static final int ANALYSIS_FRAGMENT = 3;

    private static boolean processInitialized = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);

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
            if (item.getItemId() == R.id.nav_home) viewPager.setCurrentItem(HOME_FRAGMENT, true);
            else if (item.getItemId() == R.id.nav_subject) viewPager.setCurrentItem(SUBJECT_FRAGMENT, true);
            else if (item.getItemId() == R.id.nav_time) viewPager.setCurrentItem(TIME_FRAGMENT, true);
            else if (item.getItemId() == R.id.nav_analysis) viewPager.setCurrentItem(ANALYSIS_FRAGMENT, true);
            else return false;

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