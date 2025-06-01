package mp.gradia.analysis;

import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.github.mikephil.charting.charts.BarChart;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.data.BarData;
import com.github.mikephil.charting.data.BarDataSet;
import com.github.mikephil.charting.data.BarEntry;
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import mp.gradia.R;
import mp.gradia.database.SubjectIdName;
import mp.gradia.database.entity.DayStudyTime;
import mp.gradia.database.entity.StudySessionEntity;
import mp.gradia.database.entity.SubjectStudyTime;

public class BarChartFragment extends Fragment {
    private BarChart barChart;
    private TextView tvWeekRange;
    private AnalysisViewModel viewModel;
    private ImageButton btnPrevWeek, btnNextWeek;
    private TextView tvDailyProgress;
    private ProgressBar progressBarDaily;
    private TextView tvMonthlyProgress;
    private ProgressBar progressBarMonthly;

    private List<SubjectStudyTime> studyTimeList = new ArrayList<>();
    private List<SubjectIdName> subjectNameList = new ArrayList<>();
    private List<StudySessionEntity> sessionList = new ArrayList<>();

    private static final int DAILY_GOAL_MINUTES = 300;
    private LocalDate currentWeekStart = LocalDate.now().with(DayOfWeek.MONDAY);

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_analysis_barchart, container, false);

        initViews(view);
        setupChart();
        setViewModel();

        return view;
    }

    private void initViews(View view) {
        barChart = view.findViewById(R.id.chartStudyTime);

        tvWeekRange = view.findViewById(R.id.tvWeekRange);
        btnPrevWeek = view.findViewById(R.id.btnPrevWeek);
        btnNextWeek = view.findViewById(R.id.btnNextWeek);

        tvDailyProgress = view.findViewById(R.id.tvDailyProgress);
        progressBarDaily = view.findViewById(R.id.progressGoal_daily);
        tvMonthlyProgress = view.findViewById(R.id.tvMonthlyProgress);
        progressBarMonthly = view.findViewById(R.id.progressGoal_monthly);
    }

    private void setViewModel() {
        viewModel = new ViewModelProvider(getParentFragment()).get(AnalysisViewModel.class);

        viewModel.getTotalStudyTimes().observe(getViewLifecycleOwner(), times -> {
            studyTimeList = times;
            tryShowBarChart();
        });

        viewModel.getSubjectNames().observe(getViewLifecycleOwner(), names -> {
            subjectNameList = names;
            tryShowBarChart();
        });

        viewModel.getAllSessions().observe(getViewLifecycleOwner(), sessions -> {
            sessionList = sessions;
        });

        viewModel.getTodayStudyTime().observe(getViewLifecycleOwner(), minutes -> {
            long safeMinutes = (minutes != null) ? minutes : 0;
            int percent = (int) Math.min((safeMinutes * 100.0 / DAILY_GOAL_MINUTES), 100);
            tvDailyProgress.setText("오늘의 학습 달성률: " + percent + "%");
            progressBarDaily.setProgress(percent);
        });

        viewModel.getMonthlyStudyTime().observe(getViewLifecycleOwner(), list -> {
            if (list == null || list.isEmpty())
                return;
            int totalPercent = 0;
            for (DayStudyTime day : list) {
                totalPercent += Math.min((day.total * 100.0 / DAILY_GOAL_MINUTES), 100);
            }
            int average = totalPercent / list.size();
            tvMonthlyProgress.setText("이번 달 평균 달성률: " + average + "%");
            progressBarMonthly.setProgress(average);
        });

        btnPrevWeek.setOnClickListener(v -> {
            currentWeekStart = currentWeekStart.minusWeeks(1);
            tryShowBarChart();
        });

        btnNextWeek.setOnClickListener(v -> {
            currentWeekStart = currentWeekStart.plusWeeks(1);
            tryShowBarChart();
        });
    }

    private void tryShowBarChart() {
        tvWeekRange.setVisibility(View.VISIBLE);
        if (studyTimeList == null || subjectNameList == null || studyTimeList.isEmpty() || subjectNameList.isEmpty())
            return;

        Map<Integer, String> idNameMap = new HashMap<>();
        for (SubjectIdName pair : subjectNameList)
            idNameMap.put(pair.getSubjectId(), pair.getSubjectName());

        LocalDate end = currentWeekStart.plusDays(6);
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("M월 d일");
        tvWeekRange.setText(currentWeekStart.format(fmt) + " ~ " + end.format(fmt));

        Map<Integer, Long> weeklyTimeMap = new HashMap<>();
        for (StudySessionEntity session : sessionList) {
            LocalDate date = session.getDate();
            if (!date.isBefore(currentWeekStart) && !date.isAfter(end)) {
                int id = session.getSubjectId();
                long updated = weeklyTimeMap.getOrDefault(id, 0L) + session.getStudyTime();
                weeklyTimeMap.put(id, updated);
            }
        }

        List<BarEntry> entries = new ArrayList<>();
        List<String> labels = new ArrayList<>();
        int index = 0;
        for (SubjectIdName pair : subjectNameList) {
            int time = weeklyTimeMap.getOrDefault(pair.getSubjectId(), 0L).intValue();
            entries.add(new BarEntry(index++, time));
            labels.add(pair.getSubjectName());
        }

        BarDataSet dataSet = new BarDataSet(entries, "과목별 학습시간 (분)");
        dataSet.setColor(Color.parseColor("#9370DB"));
        BarData barData = new BarData(dataSet);
        barData.setBarWidth(0.4f);
        barChart.setData(barData);

        XAxis xAxis = barChart.getXAxis();
        xAxis.setValueFormatter(new IndexAxisValueFormatter(labels));
        xAxis.setGranularity(1f);
        xAxis.setGranularityEnabled(true);
        xAxis.setLabelCount(labels.size());
        xAxis.setTextSize(8f);
        xAxis.setDrawGridLines(false);

        float scale = getResources().getDisplayMetrics().density;
        int chartWidth = (int) (Math.max(labels.size(), 4) * 70 * scale);
        barChart.getLayoutParams().width = chartWidth;
        barChart.requestLayout();
        barChart.invalidate();
    }

    private void setupChart() {
        barChart.getDescription().setEnabled(false);
        barChart.setFitBars(true);
        barChart.getAxisRight().setEnabled(false);
        barChart.getAxisLeft().setAxisMinimum(0f);
        barChart.getXAxis().setPosition(XAxis.XAxisPosition.BOTTOM);
        barChart.getXAxis().setGranularity(1f);
    }
}
