package mp.gradia.home;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

// Glide
import com.bumptech.glide.Glide;
import com.bumptech.glide.request.RequestOptions;

// Social Login SDKs (Logout)
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.kakao.sdk.user.UserApiClient;

// RxJava & Room
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.disposables.CompositeDisposable;
import io.reactivex.rxjava3.schedulers.Schedulers;
import mp.gradia.R;
import mp.gradia.database.AppDatabase;
import mp.gradia.database.dao.UserDao;
import mp.gradia.database.entity.UserEntity;
import mp.gradia.login.LoginActivity;

public class ProfileActivity extends AppCompatActivity {

    private static final String TAG = "ProfileActivity";

    private ImageView imgProfilePicture;
    private ImageView imgBackButton;
    private TextView tvProfileName, tvProfileEmail;
    private Button btnLogout;

    private AppDatabase db;
    private UserDao userDao;
    private final CompositeDisposable disposables = new CompositeDisposable();

    // SharedPreferences 키 (LoginActivity와 일치해야 함)
    private static final String PREFS_NAME = "user_session";
    private static final String KEY_LOGIN_PROVIDER = "login_provider";
    private static final String KEY_PROVIDER_ID = "provider_id";
    private static final String KEY_USER_DISPLAY_NAME = "user_display_name";

    // Login Provider 상수
    private static final String PROVIDER_GOOGLE = "google";
    private static final String PROVIDER_KAKAO = "kakao";

    private String currentUserProvider;
    private String currentUserProviderId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile); // UI 요소 ID 확인 필요

        imgBackButton = findViewById(R.id.img_profile_back_button);
        imgProfilePicture = findViewById(R.id.img_profile_picture);
        tvProfileName = findViewById(R.id.tv_profile_name);
        tvProfileEmail = findViewById(R.id.tv_profile_email);
        btnLogout = findViewById(R.id.btn_profile_logout);

        db = AppDatabase.getInstance(getApplicationContext());
        userDao = db.userDao();

        // SharedPreferences에서 현재 사용자 정보 가져오기
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        currentUserProvider = prefs.getString(KEY_LOGIN_PROVIDER, null);
        currentUserProviderId = prefs.getString(KEY_PROVIDER_ID, null);

        if (currentUserProvider == null || currentUserProviderId == null) {
            Toast.makeText(this, "로그인 정보가 유효하지 않습니다. 다시 로그인해주세요.", Toast.LENGTH_LONG).show();
            goToLoginScreen();
            return;
        }

        // 사용자 프로필 로드
        loadUserProfile(currentUserProvider, currentUserProviderId);

        // 리스너 설정
        btnLogout.setOnClickListener(v -> logoutUser());
        imgBackButton.setOnClickListener(v -> onBackPressed());
    }

    private void loadUserProfile(String loginProvider, String providerId) {
        Log.i(TAG, "사용자 프로필 로드 시작. Provider: " + loginProvider + ", ID: " + providerId);
        disposables.add(userDao.getUserByProviderInfo(loginProvider, providerId)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(
                        userEntity -> {
                            if (userEntity != null) {
                                Log.i(TAG, "DB에서 사용자 정보 로드 성공: " + userEntity.name);
                                tvProfileName.setText(userEntity.name != null ? userEntity.name : "이름 없음");
                                tvProfileEmail.setText(userEntity.email != null ? userEntity.email : "이메일 정보 없음");

                                // Glide로 프로필 사진 로드
                                String photoUrl = userEntity.photoUrl;
                                RequestOptions glideOptions = new RequestOptions()
                                        .placeholder(R.drawable.ic_default_profile) // 로딩 중 기본 이미지
                                        .error(R.drawable.ic_profile_error) // 에러 시 이미지
                                        .circleCrop(); // 원형으로 자르기

                                if (photoUrl != null && !photoUrl.isEmpty()) {
                                    Log.d(TAG, "Glide 로드 시도: " + photoUrl);
                                    Glide.with(this).load(photoUrl).apply(glideOptions).into(imgProfilePicture);
                                    
                                } else {
                                    Log.d(TAG, "프로필 사진 URL 없음. 기본 이미지 로드.");
                                    Glide.with(this).load(R.drawable.ic_default_profile).apply(glideOptions)
                                            .into(imgProfilePicture);
                                }
                            } else {
                                Log.w(TAG, "DB에 사용자 정보 없음. Provider: " + loginProvider + ", ID: " + providerId);
                                Toast.makeText(this, "사용자 정보를 찾을 수 없습니다.", Toast.LENGTH_SHORT).show();
                                // 정보가 없으면 강제 로그아웃 처리
                                forceLogout();
                            }
                        },
                        throwable -> {
                            Log.e(TAG, "사용자 정보 로드 중 DB 오류", throwable);
                            Toast.makeText(this, "오류가 발생하여 사용자 정보를 불러오지 못했습니다.", Toast.LENGTH_SHORT).show();
                        }));
    }

    private void logoutUser() {
        Log.i(TAG, "로그아웃 시작. Provider: " + currentUserProvider);

        Runnable clearSessionAndGoToLogin = () -> {
            // SharedPreferences에서 세션 정보 삭제
            SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
            SharedPreferences.Editor editor = prefs.edit();
            editor.remove(KEY_LOGIN_PROVIDER);
            editor.remove(KEY_PROVIDER_ID);
            editor.remove(KEY_USER_DISPLAY_NAME);
            editor.apply();
            Log.i(TAG, "SharedPreferences 세션 정보 삭제 완료.");
            goToLoginScreen();
        };

        if (PROVIDER_GOOGLE.equals(currentUserProvider)) {
            // Google 로그아웃
            GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                    .requestEmail() // 로그아웃 시에는 기본 옵션만으로도 충분
                    .build();
            GoogleSignInClient googleSignInClient = GoogleSignIn.getClient(this, gso);
            googleSignInClient.signOut().addOnCompleteListener(this, task -> {
                if (task.isSuccessful()) {
                    Log.i(TAG, "Google 계정 로그아웃 성공.");
                } else {
                    Log.w(TAG, "Google 계정 로그아웃 실패.", task.getException());
                }
                clearSessionAndGoToLogin.run(); // 성공/실패 여부와 관계없이 세션 정리 및 화면 이동
            });
        } else if (PROVIDER_KAKAO.equals(currentUserProvider)) {
            // Kakao 로그아웃
            UserApiClient.getInstance().logout(error -> {
                if (error != null) {
                    Log.e(TAG, "Kakao 로그아웃 실패.", error);
                } else {
                    Log.i(TAG, "Kakao 로그아웃 성공.");
                }
                clearSessionAndGoToLogin.run(); // 성공/실패 여부와 관계없이 세션 정리 및 화면 이동
                return null; // Unit 반환
            });
        } else {
            // 알 수 없는 Provider 또는 로그인 안 된 상태 (오류 케이스)
            Log.w(TAG, "알 수 없는 로그인 제공자 또는 로그인 상태 아님: " + currentUserProvider);
            clearSessionAndGoToLogin.run(); // 세션 정리 및 로그인 화면 이동
        }
    }

    // DB 정보 불일치 등 예외 상황에서 강제 로그아웃
    private void forceLogout() {
        Log.w(TAG, "강제 로그아웃 수행.");
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        editor.clear(); // 모든 세션 정보 삭제
        editor.apply();
        goToLoginScreen();
    }

    private void goToLoginScreen() {
        Intent intent = new Intent(ProfileActivity.this, LoginActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
        finish();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        disposables.clear();
        Log.d(TAG, "ProfileActivity onDestroy: Disposables cleared.");
    }
}