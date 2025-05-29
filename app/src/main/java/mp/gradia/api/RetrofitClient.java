package mp.gradia.api;

import java.util.concurrent.TimeUnit;

import okhttp3.OkHttpClient;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class RetrofitClient {
    private static Retrofit retrofit = null;
    private static final String BASE_URL = "https://gradia-backend-908213614014.us-central1.run.app/";

    // 테스트 목적으로 사용할 mock API 서비스
    private static ApiService mockApiService = null;
    private static boolean isTestMode = false;

    public static Retrofit getClient() {
        if (retrofit == null) {
            // OkHttpClient 설정 - timeout을 길게 설정
            OkHttpClient okHttpClient = new OkHttpClient.Builder()
                    .connectTimeout(60, TimeUnit.SECONDS) // 연결 timeout: 60초
                    .readTimeout(120, TimeUnit.SECONDS) // 읽기 timeout: 120초 (prediction API용)
                    .writeTimeout(60, TimeUnit.SECONDS) // 쓰기 timeout: 60초
                    .build();

            retrofit = new Retrofit.Builder()
                    .baseUrl(BASE_URL)
                    .client(okHttpClient) // OkHttpClient 추가
                    .addConverterFactory(GsonConverterFactory.create())
                    .build();
        }
        return retrofit;
    }

    public static ApiService getApiService() {
        // 테스트 모드일 경우 mock API 서비스 반환
        if (isTestMode && mockApiService != null) {
            return mockApiService;
        }
        // 일반 모드일 경우 실제 API 서비스 반환
        return getClient().create(ApiService.class);
    }

    /**
     * 테스트를 위한 mock API 서비스 설정 메서드
     * 
     * @param apiService mock API 서비스 인스턴스
     */
    public static void setMockApiServiceForTesting(ApiService apiService) {
        mockApiService = apiService;
        isTestMode = true;
    }

    /**
     * 테스트 후 원래 상태로 리셋하는 메서드
     */
    public static void resetMockForTesting() {
        mockApiService = null;
        isTestMode = false;
    }
}