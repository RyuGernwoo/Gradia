package mp.gradia.main;

import android.os.Bundle;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.viewpager2.widget.ViewPager2;

import com.google.android.material.bottomnavigation.BottomNavigationView;

import mp.gradia.R;

public class MainActivity extends AppCompatActivity {

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

        // ViewPager Object
        ViewPager2 viewPager = findViewById(R.id.view_pager);
        // Set ViewPager Adapter
        viewPager.setAdapter(new MainFragmentAdapter(this));

        // BottomNavigationView Object
        BottomNavigationView bottomNavigation = findViewById(R.id.bottom_navigation);
        // BottonNavigationView Event Listener
        bottomNavigation.setOnItemSelectedListener(item -> {
            // 0: HomeFragment, 1: SubjectFragment, 2: TimeFragment, 3:AnalysisFragment
            if (item.getItemId() == R.id.nav_home) viewPager.setCurrentItem(0, true);
            else if (item.getItemId() == R.id.nav_subject) viewPager.setCurrentItem(1, true);
            else if (item.getItemId() == R.id.nav_time) viewPager.setCurrentItem(2, true);
            else if (item.getItemId() == R.id.nav_analysis) viewPager.setCurrentItem(3, true);
            else return false;

            return true;
        });

        viewPager.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageSelected(int position) {
                switch (position) {
                    case 0: bottomNavigation.setSelectedItemId(R.id.nav_home); break;
                    case 1: bottomNavigation.setSelectedItemId(R.id.nav_subject); break;
                    case 2: bottomNavigation.setSelectedItemId(R.id.nav_time); break;
                    case 3: bottomNavigation.setSelectedItemId(R.id.nav_analysis); break;
                }
            }
        });
    }
}