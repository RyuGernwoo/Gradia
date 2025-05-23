package mp.gradia.api;

import android.content.Context;
import android.util.Log;

import java.util.List;

import mp.gradia.api.models.AuthResponse;
import mp.gradia.api.models.GoogleLoginRequest;
import mp.gradia.api.models.GradePredictionRequest;
import mp.gradia.api.models.GradePredictionResponse;
import mp.gradia.api.models.StudySession;
import mp.gradia.api.models.StudySessionsApiResponse;
import mp.gradia.api.models.Subject;
import mp.gradia.api.models.TimetableResponse;
import mp.gradia.api.models.UserInfo;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * API 호출을 간편하게 하기 위한 헬퍼 클래스입니다.
 */
public class ApiHelper {
    private static final String TAG = "ApiHelper";
    private final ApiService apiService;
    private final AuthManager authManager;

    public ApiHelper(Context context) {
        apiService = RetrofitClient.getApiService();
        authManager = AuthManager.getInstance(context);
    }

    /**
     * 시간표를 가져옵니다.
     */
    public void getTimetable(String everytimeUrl, final ApiCallback<TimetableResponse> callback) {
        apiService.getTimetable(everytimeUrl).enqueue(new Callback<TimetableResponse>() {
            @Override
            public void onResponse(Call<TimetableResponse> call, Response<TimetableResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    callback.onSuccess(response.body());
                } else {
                    callback.onError("시간표를 가져오는데 실패했습니다. 코드: " + response.code());
                }
            }

            @Override
            public void onFailure(Call<TimetableResponse> call, Throwable t) {
                callback.onError("네트워크 오류: " + t.getMessage());
                Log.e(TAG, "네트워크 오류", t);
            }
        });
    }

    /**
     * Google 계정으로 로그인합니다.
     */
    public void loginWithGoogle(String idToken, final ApiCallback<AuthResponse> callback) {
        GoogleLoginRequest request = new GoogleLoginRequest(idToken);
        apiService.loginWithGoogle(request).enqueue(new Callback<AuthResponse>() {
            @Override
            public void onResponse(Call<AuthResponse> call, Response<AuthResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    AuthResponse authResponse = response.body();
                    authManager.saveAuthInfo(
                            authResponse.getAccess_token(),
                            authResponse.getUser_id(),
                            authResponse.getEmail(),
                            authResponse.getName(),
                            "google",
                            "");
                    callback.onSuccess(authResponse);
                } else {
                    callback.onError("로그인에 실패했습니다. 코드: " + response.code());
                }
            }

            @Override
            public void onFailure(Call<AuthResponse> call, Throwable t) {
                callback.onError("네트워크 오류: " + t.getMessage());
                Log.e(TAG, "네트워크 오류", t);
            }
        });
    }

    /**
     * 현재 사용자 정보를 조회합니다.
     */
    public void getCurrentUser(final ApiCallback<UserInfo> callback) {
        if (!authManager.isLoggedIn()) {
            callback.onError("로그인이 필요합니다.");
            return;
        }

        apiService.getCurrentUser(authManager.getAuthHeader()).enqueue(new Callback<UserInfo>() {
            @Override
            public void onResponse(Call<UserInfo> call, Response<UserInfo> response) {
                if (response.isSuccessful() && response.body() != null) {
                    callback.onSuccess(response.body());
                } else {
                    callback.onError("사용자 정보를 가져오는데 실패했습니다. 코드: " + response.code());
                }
            }

            @Override
            public void onFailure(Call<UserInfo> call, Throwable t) {
                callback.onError("네트워크 오류: " + t.getMessage());
                Log.e(TAG, "네트워크 오류", t);
            }
        });
    }

    /**
     * 토큰을 갱신합니다.
     */
    public void refreshToken(final ApiCallback<AuthResponse> callback) {
        apiService.refreshToken(authManager.getAuthHeader()).enqueue(new Callback<AuthResponse>() {
            @Override
            public void onResponse(Call<AuthResponse> call, Response<AuthResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    AuthResponse authResponse = response.body();
                    authManager.saveAuthInfo(
                            authResponse.getAccess_token(),
                            authResponse.getUser_id(),
                            authResponse.getEmail(),
                            authResponse.getName(),
                            "google",
                            "");
                    callback.onSuccess(authResponse);
                } else {
                    callback.onError("토큰 갱신에 실패했습니다. 코드: " + response.code());
                }
            }

            @Override
            public void onFailure(Call<AuthResponse> call, Throwable t) {
                callback.onError("네트워크 오류: " + t.getMessage());
                Log.e(TAG, "네트워크 오류", t);
            }
        });
    }

    /**
     * 학습 세션 목록을 조회합니다.
     */
    public void getStudySessions(String subjectId, final ApiCallback<List<StudySession>> callback) {
        if (!authManager.isLoggedIn()) {
            callback.onError("로그인이 필요합니다.");
            return;
        }

        apiService.getStudySessions(authManager.getAuthHeader(), subjectId)
                .enqueue(new Callback<StudySessionsApiResponse>() {
                    @Override
                    public void onResponse(Call<StudySessionsApiResponse> call,
                            Response<StudySessionsApiResponse> response) {
                        if (response.isSuccessful() && response.body() != null
                                && response.body().getSessions() != null) {
                            callback.onSuccess(response.body().getSessions());
                        } else {
                            String errorMsg = "학습 세션 목록을 가져오는데 실패했습니다.";
                            if (response.body() != null && response.body().getMessage() != null) {
                                errorMsg += " 메시지: " + response.body().getMessage();
                            } else if (response.errorBody() != null) {
                                try {
                                    errorMsg += " 오류: " + response.errorBody().string();
                                } catch (Exception e) {
                                    Log.e(TAG, "Error body parsing failed", e);
                                }
                            }
                            errorMsg += " 코드: " + response.code();
                            callback.onError(errorMsg);
                        }
                    }

                    @Override
                    public void onFailure(Call<StudySessionsApiResponse> call, Throwable t) {
                        callback.onError("네트워크 오류: " + t.getMessage());
                        Log.e(TAG, "네트워크 오류", t);
                    }
                });
    }

    /**
     * 성적을 예측합니다.
     */
    public void predictGrade(String subjectName, int understandingLevel, int studyTimeHours,
            Integer assignmentQuizAvgScore, final ApiCallback<GradePredictionResponse> callback) {
        if (!authManager.isLoggedIn()) {
            callback.onError("로그인이 필요합니다.");
            return;
        }

        GradePredictionRequest request = new GradePredictionRequest(
                subjectName, understandingLevel, studyTimeHours, assignmentQuizAvgScore);

        apiService.predictGrade(authManager.getAuthHeader(), request).enqueue(new Callback<GradePredictionResponse>() {
            @Override
            public void onResponse(Call<GradePredictionResponse> call, Response<GradePredictionResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    callback.onSuccess(response.body());
                } else {
                    callback.onError("성적 예측에 실패했습니다. 코드: " + response.code());
                }
            }

            @Override
            public void onFailure(Call<GradePredictionResponse> call, Throwable t) {
                callback.onError("네트워크 오류: " + t.getMessage());
                Log.e(TAG, "네트워크 오류", t);
            }
        });
    }

    /**
     * API 콜백 인터페이스입니다.
     */
    public interface ApiCallback<T> {
        void onSuccess(T result);

        void onError(String errorMessage);
    }
}