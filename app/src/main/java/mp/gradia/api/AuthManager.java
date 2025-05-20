package mp.gradia.api;

import android.content.Context;
import android.content.SharedPreferences;

/**
 * 인증 토큰을 관리하는 매니저 클래스입니다.
 */
public class AuthManager {
    private static final String PREF_NAME = "AuthPrefs";
    private static final String KEY_ACCESS_TOKEN = "access_token";
    private static final String KEY_USER_ID = "user_id";
    private static final String KEY_EMAIL = "email";
    private static final String KEY_NAME = "name";

    private SharedPreferences preferences;
    private static AuthManager instance;

    private AuthManager(Context context) {
        preferences = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
    }

    public static synchronized AuthManager getInstance(Context context) {
        if (instance == null) {
            instance = new AuthManager(context.getApplicationContext());
        }
        return instance;
    }

    /**
     * 인증 정보를 저장합니다.
     */
    public void saveAuthInfo(String accessToken, String userId, String email, String name) {
        SharedPreferences.Editor editor = preferences.edit();
        editor.putString(KEY_ACCESS_TOKEN, accessToken);
        editor.putString(KEY_USER_ID, userId);
        editor.putString(KEY_EMAIL, email);
        editor.putString(KEY_NAME, name);
        editor.apply();
    }

    /**
     * 인증 토큰을 가져옵니다.
     */
    public String getAuthToken() {
        return preferences.getString(KEY_ACCESS_TOKEN, null);
    }

    /**
     * Authorization 헤더 값을 생성합니다.
     */
    public String getAuthHeader() {
        String token = getAuthToken();
        if (token != null) {
            return "Bearer " + token;
        }
        return null;
    }

    /**
     * 사용자 ID를 가져옵니다.
     */
    public String getUserId() {
        return preferences.getString(KEY_USER_ID, null);
    }

    /**
     * 사용자 이메일을 가져옵니다.
     */
    public String getUserEmail() {
        return preferences.getString(KEY_EMAIL, null);
    }

    /**
     * 사용자 이름을 가져옵니다.
     */
    public String getUserName() {
        return preferences.getString(KEY_NAME, null);
    }

    /**
     * 사용자가 로그인 되어 있는지 확인합니다.
     */
    public boolean isLoggedIn() {
        return getAuthToken() != null;
    }

    /**
     * 로그아웃합니다.
     */
    public void logout() {
        SharedPreferences.Editor editor = preferences.edit();
        editor.clear();
        editor.apply();
    }

    /**
     * 테스트를 위한 임시 인스턴스 설정 메서드
     * 
     * @param mockInstance 테스트용 MockAuthManager 인스턴스
     */
    public static void setInstanceForTesting(AuthManager mockInstance) {
        instance = mockInstance;
    }

    /**
     * 테스트 후 인스턴스 초기화 메서드
     */
    public static void resetInstanceForTesting() {
        instance = null;
    }
}