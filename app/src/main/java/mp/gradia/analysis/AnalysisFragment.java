package mp.gradia.analysis;

import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.viewpager2.widget.ViewPager2;

import com.github.mikephil.charting.charts.BarChart;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.data.BarData;
import com.github.mikephil.charting.data.BarDataSet;
import com.github.mikephil.charting.data.BarEntry;
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter;
import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.TextStyle;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

import mp.gradia.R;
import mp.gradia.database.SubjectIdName;
import mp.gradia.database.entity.DayStudyTime;
import mp.gradia.database.entity.StudySessionEntity;
import mp.gradia.database.entity.SubjectStudyTime;
import mp.gradia.feedback.FeedbackAnalysis;
import mp.gradia.feedback.FeedbackManager;

public class AnalysisFragment extends Fragment {

    private TabLayout tabLayout;
    private ViewPager2 viewPager;
    private AnalysisTabFragmentAdapter adapter;
    private String[] tabTitles = new String[]{"성적 예측 및\n추천", "과목별", "요일별"};
    private AnalysisViewModel viewModel;
    private List<StudySessionEntity> sessionList = new ArrayList<>();

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_analysis, container, false);

        initViews(view);
        setupTab();
        setViewModel();

        new TabLayoutMediator(tabLayout, viewPager, (tab, position) -> {
            tab.setText(tabTitles[position]);
        }).attach();

        viewPager.setCurrentItem(0, false);
        viewPager.setUserInputEnabled(false);
        return view;
    }
    private void initViews(View view) {
        tabLayout = view.findViewById(R.id.tab_layout);
        viewPager = view.findViewById(R.id.view_pager);
    }

    private void setupTab() {
        adapter = new AnalysisTabFragmentAdapter(this, tabTitles.length);
        viewPager.setAdapter(adapter);
    }

    private void setViewModel() {
        viewModel = new ViewModelProvider(this).get(AnalysisViewModel.class);
        viewModel.getAllSessions().observe(getViewLifecycleOwner(), sessions -> {
            sessionList = sessions;
        });
    }

    @Override
    public void onResume() {
        super.onResume();
        Log.d("AnalysisFragment", "onResume called");

        // advice를 생성하는 예시 코드
        Optional<FeedbackAnalysis> analysis = FeedbackManager.analayzeLogPeriod(sessionList);
        var recentSessions = sessionList.stream()
                .filter(s -> s.getDate().isAfter(java.time.LocalDate.now().minusDays(30))).toList();
        Optional<FeedbackAnalysis> recentAnalysis = FeedbackManager.analayzeLogPeriod(recentSessions);

        var advice = FeedbackManager.generateTemperalAdvice(recentAnalysis, analysis);
        for (var line : advice) {
            Log.d("AnalysisFragment", "Advice: " + line.getType());
        }
    }
}
