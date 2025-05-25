package mp.gradia.analysis;

import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.github.mikephil.charting.charts.BarChart;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.data.BarData;
import com.github.mikephil.charting.data.BarDataSet;
import com.github.mikephil.charting.data.BarEntry;
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter;
import com.google.android.material.tabs.TabLayout;

import java.time.DayOfWeek;
import java.time.format.TextStyle;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import mp.gradia.R;
import mp.gradia.database.SubjectIdName;
import mp.gradia.database.entity.DayStudyTime;
import mp.gradia.database.entity.StudySessionEntity;
import mp.gradia.database.entity.SubjectStudyTime;

public class AnalysisFragment extends Fragment {

    private BarChart barChart;
    private TabLayout tabLayout;
    private AnalysisViewModel viewModel;

    private List<SubjectStudyTime> studyTimeList = new ArrayList<>();
    private List<SubjectIdName> subjectNameList = new ArrayList<>();
    private List<StudySessionEntity> sessionList = new ArrayList<>();

    private static final int DAILY_GOAL_MINUTES = 300;


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_analysis, container, false);

        barChart = view.findViewById(R.id.chartStudyTime);
        tabLayout = view.findViewById(R.id.tabMode);
        TextView tvDailyProgress = view.findViewById(R.id.tvDailyProgress);
        ProgressBar progressBarDaily = view.findViewById(R.id.progressGoal_daily);

        TextView tvMonthlyProgress = view.findViewById(R.id.tvMonthlyProgress);
        ProgressBar progressBarMonthly = view.findViewById(R.id.progressGoal_monthly);

        setupChart();

        // 탭 생성
        tabLayout.addTab(tabLayout.newTab().setText("과목별"));
        tabLayout.addTab(tabLayout.newTab().setText("요일별"));

        tabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                if (tab.getPosition() == 0) {
                    tryShowBarChart();
                } else {
                    tryShowBarChartByDay();
                }
            }

            @Override public void onTabUnselected(TabLayout.Tab tab) {}
            @Override public void onTabReselected(TabLayout.Tab tab) {}
        });

        viewModel = new ViewModelProvider(this).get(AnalysisViewModel.class);

        viewModel.getTotalStudyTimes().observe(getViewLifecycleOwner(), times -> {
            studyTimeList = times;
            if (tabLayout.getSelectedTabPosition() == 0) tryShowBarChart();
        });

        viewModel.getSubjectNames().observe(getViewLifecycleOwner(), names -> {
            subjectNameList = names;
            if (tabLayout.getSelectedTabPosition() == 0) tryShowBarChart();
        });

        viewModel.getAllSessions().observe(getViewLifecycleOwner(), sessions -> {
            sessionList = sessions;
            if (tabLayout.getSelectedTabPosition() == 1) tryShowBarChartByDay();
        });

        viewModel.getTodayStudyTime().observe(getViewLifecycleOwner(), minutes -> {
            int percent = (int) Math.min((minutes * 100.0 / DAILY_GOAL_MINUTES), 100);
            tvDailyProgress.setText("오늘의 학습 달성률: " + percent + "%");
            progressBarDaily.setProgress(percent);
        });

        viewModel.getMonthlyStudyTime().observe(getViewLifecycleOwner(), list -> {
            if (list == null || list.isEmpty()) return;
            int totalPercent = 0;
            for (DayStudyTime day : list) {
                totalPercent += Math.min((day.total * 100.0 / DAILY_GOAL_MINUTES), 100);
            }
            int average = totalPercent / list.size();
            tvMonthlyProgress.setText("이번 달 평균 달성률: " + average + "%");
            progressBarMonthly.setProgress(average);
        });


        return view;
    }

    private void setupChart() {
        barChart.getDescription().setEnabled(false);
        barChart.setFitBars(true);
        barChart.getAxisRight().setEnabled(false);
        barChart.getAxisLeft().setAxisMinimum(0f);
        barChart.getXAxis().setPosition(XAxis.XAxisPosition.BOTTOM);
        barChart.getXAxis().setGranularity(1f);
    }

    private void tryShowBarChart() {
        if (studyTimeList == null || subjectNameList == null || studyTimeList.isEmpty() || subjectNameList.isEmpty()) {
            return;
        }

        Map<Integer, String> idNameMap = new HashMap<>();
        for (SubjectIdName pair : subjectNameList) {
            idNameMap.put(pair.getSubjectId(), pair.getSubjectName());
        }

        List<BarEntry> entries = new ArrayList<>();
        List<String> labels = new ArrayList<>();

        for (int i = 0; i < studyTimeList.size(); i++) {
            SubjectStudyTime item = studyTimeList.get(i);
            entries.add(new BarEntry(i, item.getTotalTime()));

            String subjectName = idNameMap.getOrDefault(item.getSubjectId(), "알 수 없음");
            labels.add(subjectName);
        }

        BarDataSet dataSet = new BarDataSet(entries, "과목별 학습시간 (분)");
        dataSet.setColor(Color.parseColor("#9370DB"));

        BarData barData = new BarData(dataSet);
        barData.setBarWidth(0.9f);

        barChart.setData(barData);
        barChart.getXAxis().setValueFormatter(new IndexAxisValueFormatter(labels));
        barChart.invalidate();
    }

    private void tryShowBarChartByDay() {
        if (sessionList == null || sessionList.isEmpty()) return;

        Map<DayOfWeek, Long> timeMap = new EnumMap<>(DayOfWeek.class);
        for (DayOfWeek day : DayOfWeek.values()) {
            timeMap.put(day, 0L);
        }

        for (StudySessionEntity session : sessionList) {
            DayOfWeek day = session.getDate().getDayOfWeek();
            long updated = timeMap.get(day) + session.getStudyTime();
            timeMap.put(day, updated);
        }

        List<BarEntry> entries = new ArrayList<>();
        List<String> labels = new ArrayList<>();
        int index = 0;

        for (DayOfWeek day : DayOfWeek.values()) {
            entries.add(new BarEntry(index++, timeMap.get(day)));
            labels.add(day.getDisplayName(TextStyle.SHORT, Locale.KOREAN));  // 월, 화, ...
        }

        BarDataSet dataSet = new BarDataSet(entries, "요일별 학습시간 (분)");
        dataSet.setColor(Color.parseColor("#9370DB"));

        BarData barData = new BarData(dataSet);
        barData.setBarWidth(0.9f);

        barChart.setData(barData);
        barChart.getXAxis().setValueFormatter(new IndexAxisValueFormatter(labels));
        barChart.invalidate();
    }
}

