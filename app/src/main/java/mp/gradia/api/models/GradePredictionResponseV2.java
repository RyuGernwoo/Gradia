package mp.gradia.api.models;

public class GradePredictionResponseV2 {
    public String raw_prediction;

    public LearningPatternAnalysis learning_pattern_analysis;

    public StructuredPredictionV2 structured_prediction;

    public static class LearningPatternAnalysis {
        public int total_sessions;

        public double total_actual_hours;

        public double avg_focus_level;

        public double recent_week_hours;

        public double recent_week_focus;

        public String time_distribution;

        public double study_days_per_week;

        public int avg_session_length;

        public int consistency_score;

        public String focus_trend;
    }

    public String getRawPrediction() {
        return raw_prediction;
    }

    public LearningPatternAnalysis getLearningPatternAnalysis() {
        return learning_pattern_analysis;
    }

    public StructuredPredictionV2 getStructuredPrediction() {
        return structured_prediction;
    }
}
