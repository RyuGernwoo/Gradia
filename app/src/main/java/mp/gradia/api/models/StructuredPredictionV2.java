package mp.gradia.api.models;

import java.util.List;

public class StructuredPredictionV2 {
    public String score;

    public String score_range;

    public String grade;

    public String confidence;

    public Analysis analysis;

    public List<String> key_factors;

    public PersonalizedAdvice personalized_advice;

    public WeeklyPlan weekly_plan;


    public static class Analysis {
        public String learning_volume;

        public String learning_quality;

        public String learning_consistency;
    }

    public static class PersonalizedAdvice {
        public String priority_high;

        public String optimization;

        public String maintenance;
    }

    public static class WeeklyPlan {
        public String target_hours;

        public String target_sessions;

        public String focus_areas;
    }
}
