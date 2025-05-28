package mp.gradia.api;

import java.util.List;

import mp.gradia.api.models.AuthResponse;
import mp.gradia.api.models.GoogleLoginRequest;
import mp.gradia.api.models.GradePredictionRequest;
import mp.gradia.api.models.GradePredictionRequestV2;
import mp.gradia.api.models.GradePredictionResponse;
import mp.gradia.api.models.GradePredictionResponseV2;
import mp.gradia.api.models.KakaoLoginRequest;
import mp.gradia.api.models.StudySession;
import mp.gradia.api.models.StudySessionsApiResponse;
import mp.gradia.api.models.Subject;
import mp.gradia.api.models.SubjectsApiResponse;
import mp.gradia.api.models.TimetableResponse;
import mp.gradia.api.models.UserInfo;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.DELETE;
import retrofit2.http.GET;
import retrofit2.http.Header;
import retrofit2.http.PATCH;
import retrofit2.http.POST;
import retrofit2.http.Path;
import retrofit2.http.Query;

public interface ApiService {
        // 시간표 가져오기
        @GET("timetable")
        Call<TimetableResponse> getTimetable(@Query("url") String everytimeUrl);

        // Google 계정으로 로그인
        @POST("auth/google")
        Call<AuthResponse> loginWithGoogle(@Body GoogleLoginRequest request);

        // Kakao 계정으로 로그인
        @POST("auth/kakao")
        Call<AuthResponse> loginWithKakao(@Body KakaoLoginRequest request);

        // 현재 사용자 정보 조회
        @GET("auth/users/me")
        Call<UserInfo> getCurrentUser(@Header("Authorization") String token);

        // 토큰 갱신
        @GET("auth/refresh_token")
        Call<AuthResponse> refreshToken(@Header("Authorization") String token);

        // 임시 사용자 생성
        @POST("auth/temp_user_for_test")
        Call<AuthResponse> getTempUser();

        // 임시 사용자 삭제
        @DELETE("auth/temp_user_for_test")
        Call<Void> deleteTempUser(@Header("Authorization") String token);

        // 학습 세션 목록 조회
        @GET("study-session/")
        Call<StudySessionsApiResponse> getStudySessions(@Header("Authorization") String token,
                        @Query("subject_id") String subjectId);

        // 특정 학습 세션 조회
        @GET("study-session/{session_id}")
        Call<StudySession> getStudySession(@Header("Authorization") String token,
                        @Path("session_id") String sessionId);

        // 새 학습 세션 생성
        @POST("study-session/")
        Call<StudySession> createStudySession(@Header("Authorization") String token,
                        @Body StudySession studySession);

        // 학습 세션 업데이트
        @PATCH("study-session/{session_id}")
        Call<StudySession> updateStudySession(@Header("Authorization") String token,
                        @Path("session_id") String sessionId,
                        @Body StudySession studySession);

        // 학습 세션 삭제
        @DELETE("study-session/{session_id}")
        Call<Void> deleteStudySession(@Header("Authorization") String token,
                        @Path("session_id") String sessionId);

        // 과목 목록 조회
        @GET("subject/")
        Call<SubjectsApiResponse> getSubjects(@Header("Authorization") String token);

        // 특정 과목 조회
        @GET("subject/{subject_id}")
        Call<Subject> getSubject(@Header("Authorization") String token,
                        @Path("subject_id") String subjectId);

        // 새 과목 생성
        @POST("subject/")
        Call<Subject> createSubject(@Header("Authorization") String token,
                        @Body Subject subject);

        // 과목 정보 업데이트
        @PATCH("subject/{subject_id}")
        Call<Subject> updateSubject(@Header("Authorization") String token,
                        @Path("subject_id") String subjectId,
                        @Body Subject subject);

        // 과목 삭제
        @DELETE("subject/{subject_id}")
        Call<Void> deleteSubject(@Header("Authorization") String token,
                        @Path("subject_id") String subjectId);

        // 성적 예측
        @POST("grade-prediction/")
        Call<GradePredictionResponse> predictGrade(@Header("Authorization") String token,
                        @Body GradePredictionRequest request);

        @POST("grade-prediction/v2")
        Call<GradePredictionResponseV2> predictGradeV2(@Header("Authorization") String token,
                                                       @Body GradePredictionRequestV2 request);
}