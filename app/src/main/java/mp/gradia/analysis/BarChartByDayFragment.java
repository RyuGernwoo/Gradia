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

public class BarChartByDayFragment extends Fragment {
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
        });

        viewModel.getSubjectNames().observe(getViewLifecycleOwner(), names -> {
            subjectNameList = names;
        });

        viewModel.getAllSessions().observe(getViewLifecycleOwner(), sessions -> {
            sessionList = sessions;
            tryShowBarChartByDay();
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
            tryShowBarChartByDay();
        });

        btnNextWeek.setOnClickListener(v -> {
            currentWeekStart = currentWeekStart.plusWeeks(1);
            tryShowBarChartByDay();
        });
    }

    private void setupChart() {
        barChart.getDescription().setEnabled(false);
        barChart.setFitBars(true);
        barChart.getAxisRight().setEnabled(false);
        barChart.getAxisLeft().setAxisMinimum(0f);
        barChart.getXAxis().setPosition(XAxis.XAxisPosition.BOTTOM);
        barChart.getXAxis().setGranularity(1f);
    }

    private void tryShowBarChartByDay() {
        tvWeekRange.setVisibility(View.VISIBLE);
        if (sessionList == null)
            return;

        Map<DayOfWeek, Long> timeMap = new EnumMap<>(DayOfWeek.class);
        for (DayOfWeek day : DayOfWeek.values())
            timeMap.put(day, 0L);

        LocalDate end = currentWeekStart.plusDays(6);
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("M월 d일");
        tvWeekRange.setText(currentWeekStart.format(fmt) + " ~ " + end.format(fmt));

        for (StudySessionEntity session : sessionList) {
            LocalDate date = session.getDate();
            if (!date.isBefore(currentWeekStart) && !date.isAfter(end)) {
                DayOfWeek day = date.getDayOfWeek();
                timeMap.put(day, timeMap.get(day) + session.getStudyTime());
            }
        }

        List<BarEntry> entries = new ArrayList<>();
        List<String> labels = new ArrayList<>();
        int index = 0;
        for (DayOfWeek day : DayOfWeek.values()) {
            entries.add(new BarEntry(index++, timeMap.get(day)));
            labels.add(day.getDisplayName(TextStyle.SHORT, Locale.KOREAN));
        }


        float scale = getResources().getDisplayMetrics().density;
        int chartWidth = (int) (Math.max(labels.size(), 4) * 70 * scale);
        barChart.getLayoutParams().width = chartWidth;
        barChart.requestLayout();

        BarDataSet dataSet = new BarDataSet(entries, "요일별 학습시간 (분)");
        dataSet.setColor(Color.parseColor("#9370DB"));
        BarData barData = new BarData(dataSet);
        barChart.setData(barData);

        XAxis xAxis = barChart.getXAxis();
        xAxis.setValueFormatter(new IndexAxisValueFormatter(labels));
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setGranularity(1f);
        xAxis.setDrawGridLines(false);

        barChart.invalidate();
    }
}
