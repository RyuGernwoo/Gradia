package mp.gradia.api.models;

public class GradePredictionResponse {
    private String raw_prediction;
    private StructuredPrediction structured_prediction;

    public String getRaw_prediction() {
        return raw_prediction;
    }

    public void setRaw_prediction(String raw_prediction) {
        this.raw_prediction = raw_prediction;
    }

    public StructuredPrediction getStructured_prediction() {
        return structured_prediction;
    }

    public void setStructured_prediction(StructuredPrediction structured_prediction) {
        this.structured_prediction = structured_prediction;
    }
}