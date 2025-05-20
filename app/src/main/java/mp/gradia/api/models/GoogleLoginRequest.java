package mp.gradia.api.models;

public class GoogleLoginRequest {
    private String id_token_str;

    public GoogleLoginRequest(String id_token_str) {
        this.id_token_str = id_token_str;
    }

    public String getId_token_str() {
        return id_token_str;
    }

    public void setId_token_str(String id_token_str) {
        this.id_token_str = id_token_str;
    }
}