package mp.gradia.analysis;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.ProgressBar;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.cardview.widget.CardView;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.loadingindicator.LoadingIndicator;
import com.google.android.material.textfield.TextInputLayout;

import java.util.ArrayList;
import java.util.List;

import mp.gradia.R;
import mp.gradia.api.ApiService;
import mp.gradia.api.models.GradePredictionRequestV2;
import mp.gradia.api.models.GradePredictionResponseV2;
import mp.gradia.api.AuthManager;
import mp.gradia.api.models.StructuredPredictionV2;
import mp.gradia.database.entity.SubjectEntity;
import mp.gradia.api.RetrofitClient;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class GradePredictionFragment extends Fragment {
    private static final String TAG = "GradePredictionFragment";

    // UI Components
    private AutoCompleteTextView subjectDropdown;
    private TextInputLayout subjectDropdownLayout;
    private SeekBar understandingSeekBar;
    private TextView understandingLevelText;
    private MaterialButton predictButton;
    private LoadingIndicator loadingProgress;
    private TextView loadingText;
    private CardView loadingCard;
    private CardView resultCard;
    private CardView reliabilityWarningCard;
    private TextView reliabilityWarningText;

    // Result Views
    private TextView predictedGrade;
    private TextView predictedScore;
    private TextView confidence;

    // 분석 결과 개별 요소들
    private TextView learningVolume;
    private TextView learningQuality;
    private TextView learningConsistency;
    private TextView keyFactors;
    private CardView keyFactorsCard;

    // 맞춤 조언 개별 요소들
    private TextView priorityHighAdvice;
    private TextView optimizationAdvice;
    private TextView maintenanceAdvice;
    private CardView priorityHighCard;
    private CardView optimizationCard;
    private CardView maintenanceCard;

    // 주간 계획 개별 요소들
    private TextView targetHours;
    private TextView targetSessions;
    private TextView focusAreas;
    private CardView targetHoursCard;
    private CardView targetSessionsCard;
    private CardView focusAreasCard;

    // Data
    private AnalysisViewModel viewModel;
    private ApiService apiService;
    private AuthManager authManager;
    private List<SubjectEntity> subjectList = new ArrayList<>();
    private List<String> subjectNames = new ArrayList<>();
    private SubjectEntity selectedSubject;
    private int understandingLevel = 3; // 기본값을 3으로 변경

    public static GradePredictionFragment newInstance() {
        return new GradePredictionFragment();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_grade_prediction_embedded, container, false);

        initViews(view);
        setupServices();
        setupUI();
        observeData();

        return view;
    }

    private void initViews(View view) {
        subjectDropdownLayout = view.findViewById(R.id.subjectDropdownLayout);
        subjectDropdown = view.findViewById(R.id.subjectDropdown);
        understandingSeekBar = view.findViewById(R.id.understandingSeekBar);
        understandingLevelText = view.findViewById(R.id.understandingLevelText);
        predictButton = view.findViewById(R.id.predictButton);
        loadingProgress = view.findViewById(R.id.loadingProgress);
        loadingText = view.findViewById(R.id.loadingText);
        loadingCard = view.findViewById(R.id.loadingCard);
        resultCard = view.findViewById(R.id.resultCard);
        reliabilityWarningCard = view.findViewById(R.id.reliabilityWarningCard);
        reliabilityWarningText = view.findViewById(R.id.reliabilityWarningText);

        predictedGrade = view.findViewById(R.id.predictedGrade);
        predictedScore = view.findViewById(R.id.predictedScore);
        confidence = view.findViewById(R.id.confidence);
        learningVolume = view.findViewById(R.id.learningVolume);
        learningQuality = view.findViewById(R.id.learningQuality);
        learningConsistency = view.findViewById(R.id.learningConsistency);
        keyFactors = view.findViewById(R.id.keyFactors);
        keyFactorsCard = view.findViewById(R.id.keyFactorsCard);
        priorityHighAdvice = view.findViewById(R.id.priorityHighAdvice);
        optimizationAdvice = view.findViewById(R.id.optimizationAdvice);
        maintenanceAdvice = view.findViewById(R.id.maintenanceAdvice);
        priorityHighCard = view.findViewById(R.id.priorityHighCard);
        optimizationCard = view.findViewById(R.id.optimizationCard);
        maintenanceCard = view.findViewById(R.id.maintenanceCard);
        targetHours = view.findViewById(R.id.targetHours);
        targetSessions = view.findViewById(R.id.targetSessions);
        focusAreas = view.findViewById(R.id.focusAreas);
        targetHoursCard = view.findViewById(R.id.targetHoursCard);
        targetSessionsCard = view.findViewById(R.id.targetSessionsCard);
        focusAreasCard = view.findViewById(R.id.focusAreasCard);
    }

    private void setupServices() {
        viewModel = new ViewModelProvider(requireParentFragment()).get(AnalysisViewModel.class);
        apiService = RetrofitClient.getApiService();
        authManager = AuthManager.getInstance(requireContext());
    }

    private void setupUI() {
        setupSubjectDropdown();
        setupUnderstandingSeekBar();
        setupPredictButton();
    }

    private void setupSubjectDropdown() {
        ArrayAdapter<String> adapter = new ArrayAdapter<>(requireContext(), R.layout.dropdown_item_subject,
                subjectNames);
        subjectDropdown.setAdapter(adapter);

        // AutoCompleteTextView 클릭 시 드롭다운 표시
        subjectDropdown.setOnClickListener(v -> {
            if (adapter.getCount() > 0) {
                subjectDropdown.showDropDown();
            }
        });

        // TextInputLayout의 endIcon 클릭 시에도 드롭다운 표시
        subjectDropdownLayout.setEndIconOnClickListener(v -> {
            if (adapter.getCount() > 0) {
                subjectDropdown.requestFocus();
                subjectDropdown.showDropDown();
            }
        });

        // 포커스를 받았을 때도 드롭다운 표시
        subjectDropdown.setOnFocusChangeListener((v, hasFocus) -> {
            if (hasFocus && adapter.getCount() > 0) {
                subjectDropdown.showDropDown();
            }
        });

        subjectDropdown.setOnItemClickListener((parent, view, position, id) -> {
            if (position >= 0 && position < subjectList.size()) {
                selectedSubject = subjectList.get(position);
                Log.d(TAG, "선택된 과목: " + selectedSubject.getName() + ", 서버 ID: " + selectedSubject.getServerId());
                // 선택 후 포커스 제거
                subjectDropdown.clearFocus();
            }
        });
    }

    private void setupUnderstandingSeekBar() {
        understandingSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (fromUser) {
                    understandingLevel = Math.max(1, progress); // 최소값 1 보장
                    updateUnderstandingText();
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
            }
        });

        // 초기값 설정
        understandingLevel = understandingSeekBar.getProgress();
        updateUnderstandingText();
    }

    private void updateUnderstandingText() {
        String levelText;
        if (understandingLevel <= 1) {
            levelText = "매우 어려움";
        } else if (understandingLevel == 2) {
            levelText = "어려움";
        } else if (understandingLevel == 3) {
            levelText = "보통";
        } else if (understandingLevel == 4) {
            levelText = "쉬움";
        } else {
            levelText = "매우 쉬움";
        }
        understandingLevelText.setText("이해도: " + understandingLevel + " (" + levelText + ")");
    }

    private void setupPredictButton() {
        predictButton.setOnClickListener(v -> {
            if (validateInput()) {
                performGradePrediction();
            }
        });
    }

    private boolean validateInput() {
        if (selectedSubject == null) {
            Toast.makeText(requireContext(), "과목을 선택해주세요.", Toast.LENGTH_SHORT).show();
            return false;
        }

        if (understandingLevel < 1 || understandingLevel > 5) {
            Toast.makeText(requireContext(), "이해도를 1-5 사이에서 선택해주세요.", Toast.LENGTH_SHORT).show();
            return false;
        }

        String token = authManager.getAuthToken();
        if (token == null || token.isEmpty()) {
            Toast.makeText(requireContext(), "로그인이 필요합니다.", Toast.LENGTH_SHORT).show();
            return false;
        }

        return true;
    }

    private void performGradePrediction() {
        showLoading(true);
        hideResult();
        hideReliabilityWarning();

        String token = "Bearer " + authManager.getAuthToken();
        GradePredictionRequestV2 request = new GradePredictionRequestV2(selectedSubject.getServerId(),
                understandingLevel);

        apiService.predictGradeV2(token, request).enqueue(new Callback<GradePredictionResponseV2>() {
            @Override
            public void onResponse(@NonNull Call<GradePredictionResponseV2> call,
                    @NonNull Response<GradePredictionResponseV2> response) {
                showLoading(false);
                if (response.isSuccessful() && response.body() != null) {
                    displayPredictionResult(response.body());
                } else {
                    showError("성적 예측에 실패했습니다.");
                }
            }

            @Override
            public void onFailure(@NonNull Call<GradePredictionResponseV2> call, @NonNull Throwable t) {
                showLoading(false);
                Log.e(TAG, "API 호출 실패", t);
                showError("네트워크 오류가 발생했습니다.");
            }
        });
    }

    private void displayPredictionResult(GradePredictionResponseV2 response) {
        try {
            StructuredPredictionV2 prediction = response.getStructuredPrediction();
            if (prediction == null) {
                showError("예측 결과를 가져올 수 없습니다.");
                return;
            }

            // 예상 성적
            if (prediction.grade != null) {
                predictedGrade.setText(prediction.grade);
            }

            // 예상 점수
            if (prediction.score != null) {
                predictedScore.setText(prediction.score + "점");
            }

            // 신뢰도 및 경고 표시
            if (prediction.confidence != null) {
                confidence.setText(prediction.confidence);
                checkAndShowReliabilityWarning(prediction.confidence);
            }

            // 분석 결과 개별 표시
            displayAnalysisResults(prediction);

            // 개인화된 조언 개별 표시
            displayPersonalizedAdvice(prediction);

            // 주간 계획 개별 표시
            displayWeeklyPlan(prediction);

            showResult();
        } catch (Exception e) {
            Log.e(TAG, "결과 표시 중 오류 발생", e);
            showError("결과를 표시하는 중 오류가 발생했습니다.");
        }
    }

    private void displayAnalysisResults(StructuredPredictionV2 prediction) {
        // 학습량
        if (prediction.analysis != null && prediction.analysis.learning_volume != null) {
            learningVolume.setText(prediction.analysis.learning_volume);
        } else {
            learningVolume.setText("분석 데이터가 부족합니다.");
        }

        // 학습 품질
        if (prediction.analysis != null && prediction.analysis.learning_quality != null) {
            learningQuality.setText(prediction.analysis.learning_quality);
        } else {
            learningQuality.setText("분석 데이터가 부족합니다.");
        }

        // 학습 일관성
        if (prediction.analysis != null && prediction.analysis.learning_consistency != null) {
            learningConsistency.setText(prediction.analysis.learning_consistency);
        } else {
            learningConsistency.setText("분석 데이터가 부족합니다.");
        }

        // 주요 요인
        if (prediction.key_factors != null && !prediction.key_factors.isEmpty()) {
            StringBuilder factorsText = new StringBuilder();
            for (String factor : prediction.key_factors) {
                factorsText.append("• ").append(factor).append("\n");
            }
            keyFactors.setText(factorsText.toString().trim());
            keyFactorsCard.setVisibility(View.VISIBLE);
        } else {
            keyFactorsCard.setVisibility(View.GONE);
        }
    }

    private void displayPersonalizedAdvice(StructuredPredictionV2 prediction) {
        // 최우선 개선 사항
        if (prediction.personalized_advice != null && prediction.personalized_advice.priority_high != null) {
            priorityHighAdvice.setText(prediction.personalized_advice.priority_high);
            priorityHighCard.setVisibility(View.VISIBLE);
        } else {
            priorityHighCard.setVisibility(View.GONE);
        }

        // 학습 최적화 제안
        if (prediction.personalized_advice != null && prediction.personalized_advice.optimization != null) {
            optimizationAdvice.setText(prediction.personalized_advice.optimization);
            optimizationCard.setVisibility(View.VISIBLE);
        } else {
            optimizationCard.setVisibility(View.GONE);
        }

        // 현재 강점 유지
        if (prediction.personalized_advice != null && prediction.personalized_advice.maintenance != null) {
            maintenanceAdvice.setText(prediction.personalized_advice.maintenance);
            maintenanceCard.setVisibility(View.VISIBLE);
        } else {
            maintenanceCard.setVisibility(View.GONE);
        }
    }

    private void displayWeeklyPlan(StructuredPredictionV2 prediction) {
        // 권장 주간 학습 시간
        if (prediction.weekly_plan != null && prediction.weekly_plan.target_hours != null) {
            targetHours.setText(prediction.weekly_plan.target_hours);
            targetHoursCard.setVisibility(View.VISIBLE);
        } else {
            targetHoursCard.setVisibility(View.GONE);
        }

        // 권장 세션 수
        if (prediction.weekly_plan != null && prediction.weekly_plan.target_sessions != null) {
            targetSessions.setText(prediction.weekly_plan.target_sessions);
            targetSessionsCard.setVisibility(View.VISIBLE);
        } else {
            targetSessionsCard.setVisibility(View.GONE);
        }

        // 집중 영역
        if (prediction.weekly_plan != null && prediction.weekly_plan.focus_areas != null) {
            focusAreas.setText(prediction.weekly_plan.focus_areas);
            focusAreasCard.setVisibility(View.VISIBLE);
        } else {
            focusAreasCard.setVisibility(View.GONE);
        }
    }

    private void checkAndShowReliabilityWarning(String confidenceStr) {
        try {
            // 신뢰도 문자열에서 숫자 추출 (예: "75%" -> 75, "0.75" -> 75)
            String numericStr = confidenceStr.replaceAll("[^0-9.]", "");
            double confidenceValue = Double.parseDouble(numericStr);

            // 만약 값이 1 이하라면 (0.75 같은 소수점 형태) 100을 곱함
            if (confidenceValue <= 1.0) {
                confidenceValue *= 100;
            }

            // 신뢰도가 60% 미만이면 경고 표시
            if (confidenceValue < 60) {
                showReliabilityWarning(
                        "신뢰도가 낮습니다 (" + String.format("%.0f", confidenceValue) + "%). 더 많은 학습 데이터가 필요할 수 있습니다.");
            }
        } catch (NumberFormatException e) {
            Log.w(TAG, "신뢰도 값 파싱 실패: " + confidenceStr, e);
            // 파싱 실패시에도 일반적인 경고 표시 (특정 키워드 확인)
            String lowerConf = confidenceStr.toLowerCase();
            if (lowerConf.contains("low") || lowerConf.contains("낮") || lowerConf.contains("부족")) {
                showReliabilityWarning("신뢰도가 낮습니다. 더 많은 학습 데이터가 필요할 수 있습니다.");
            }
        }
    }

    private void showReliabilityWarning(String message) {
        reliabilityWarningText.setText(message);
        reliabilityWarningCard.setVisibility(View.VISIBLE);
    }

    private void hideReliabilityWarning() {
        reliabilityWarningCard.setVisibility(View.GONE);
    }

    private void showLoading(boolean show) {
        loadingCard.setVisibility(show ? View.VISIBLE : View.GONE);
        predictButton.setEnabled(!show);
    }

    private void showResult() {
        resultCard.setVisibility(View.VISIBLE);
    }

    private void hideResult() {
        resultCard.setVisibility(View.GONE);
    }

    private void showError(String message) {
        Toast.makeText(requireContext(), message, Toast.LENGTH_LONG).show();
    }

    private void observeData() {
        viewModel.getAllSubjects().observe(getViewLifecycleOwner(), subjects -> {
            if (subjects != null) {
                subjectList.clear();
                subjectNames.clear();

                for (SubjectEntity subject : subjects) {
                    subjectList.add(subject);
                    subjectNames.add(subject.getName());
                }

                // Adapter 업데이트
                ArrayAdapter<String> adapter = new ArrayAdapter<>(requireContext(),
                        R.layout.dropdown_item_subject, subjectNames);
                subjectDropdown.setAdapter(adapter);

                Log.d(TAG, "과목 목록 업데이트 완료: " + subjectNames.size() + "개");
            }
        });
    }
}