package mp.gradia.login;

import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.widget.ImageButton; // 카카오 로그인 버튼용
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;

// Google Sign-In
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.auth.api.signin.GoogleSignInStatusCodes;
import com.google.android.gms.common.SignInButton;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.tasks.Task;

// Kakao Sign-In
import com.kakao.sdk.auth.model.OAuthToken;
import com.kakao.sdk.common.model.ClientError;
import com.kakao.sdk.common.model.ClientErrorCause;
import com.kakao.sdk.common.model.KakaoSdkError;
import com.kakao.sdk.common.util.Utility;
import com.kakao.sdk.user.UserApiClient;

// RxJava & Room
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.disposables.CompositeDisposable;
import io.reactivex.rxjava3.schedulers.Schedulers;
import kotlin.Unit;
import kotlin.jvm.functions.Function2; // Kakao SDK 콜백용
import mp.gradia.R;
import mp.gradia.api.ApiHelper;
import mp.gradia.api.models.AuthResponse;
import mp.gradia.database.AppDatabase;
import mp.gradia.database.dao.UserDao;
import mp.gradia.database.entity.UserEntity;
import mp.gradia.main.MainActivity;
import mp.gradia.subject.repository.SubjectRepository;

public class LoginActivity extends AppCompatActivity {

    private static final String TAG = "LoginActivity";
    private AppDatabase db;
    private UserDao userDao;
    private final CompositeDisposable disposables = new CompositeDisposable();

    // Social Clients
    private GoogleSignInClient mGoogleSignInClient;
    // Kakao SDK는 UserApiClient 싱글톤 사용

    private SharedPreferences prefs;

    // Google Sign-In Launcher
    private ActivityResultLauncher<Intent> googleSignInLauncher;

    // api helper
    private ApiHelper apiHelper;
    // Subject Repository
    private SubjectRepository subjectRepository;

    // SharedPreferences 키 정의 (복합키 대응)
    private static final String PREFS_NAME = "user_session";
    private static final String KEY_LOGIN_PROVIDER = "login_provider"; // "google" 또는 "kakao"
    private static final String KEY_PROVIDER_ID = "provider_id"; // Google ID 또는 Kakao ID
    private static final String KEY_USER_DISPLAY_NAME = "user_display_name";

    // Login Provider 상수
    private static final String PROVIDER_GOOGLE = "google";
    private static final String PROVIDER_KAKAO = "kakao";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login); // Google, Kakao 버튼 포함된 레이아웃

        String keyHash = Utility.INSTANCE.getKeyHash(this);
        Log.d("KeyHash", keyHash);

        prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        db = AppDatabase.getInstance(getApplicationContext());
        userDao = db.userDao();
        apiHelper = new ApiHelper(this);
        subjectRepository = new SubjectRepository(this);

        // Google 로그인 옵션 설정
        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestEmail()
                .requestIdToken(getString(R.string.google_web_client_id)) // strings.xml 사용 권장
                .build();
        mGoogleSignInClient = GoogleSignIn.getClient(this, gso);

        // --- 자동 로그인 확인 ---
        if (isSignedIn()) {
            Log.d(TAG, "저장된 세션 발견 (" + prefs.getString(KEY_LOGIN_PROVIDER, "") + "). MainActivity로 이동합니다.");
            goToMainActivity(
                    prefs.getString(KEY_USER_DISPLAY_NAME, "사용자"),
                    prefs.getString(KEY_LOGIN_PROVIDER, ""),
                    prefs.getString(KEY_PROVIDER_ID, ""));
            return; // 현재 액티비티 종료
        }

        // --- UI 요소 및 리스너 설정 ---
        // Google 로그인 버튼
        SignInButton googleSignInButton = findViewById(R.id.btn_google_sign_in);
        googleSignInButton.setSize(SignInButton.SIZE_WIDE);
        googleSignInButton.setOnClickListener(v -> startGoogleSignIn());

        // Kakao 로그인 버튼
        ImageButton kakaoLoginButton = findViewById(R.id.btn_kakao_sign_in); // ID 확인
        kakaoLoginButton.setOnClickListener(v -> startKakaoSignIn());

        // Google 로그인 결과 처리 콜백 등록
        googleSignInLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == RESULT_OK) {
                        Intent data = result.getData();
                        if (data != null) {
                            Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(data);
                            handleGoogleSignInResult(task);
                        } else {
                            Log.w(TAG, "Google Sign-In 결과 데이터가 null입니다.");
                            Toast.makeText(this, "Google 로그인 결과를 받지 못했습니다.", Toast.LENGTH_SHORT).show();
                        }
                    } else {
                        Log.w(TAG, "Google Sign-In 취소 또는 실패. ResultCode: " + result.getResultCode());
                        // 필요 시 사용자에게 메시지 표시
                    }
                });
    }

    // --- 자동 로그인 확인 메소드 ---
    private boolean isSignedIn() {
        // SharedPreferences에 로그인 제공자 정보와 ID가 모두 있는지 확인
        return prefs.getString(KEY_LOGIN_PROVIDER, null) != null &&
                prefs.getString(KEY_PROVIDER_ID, null) != null;
    }

    // --- Google 로그인 시작 ---
    private void startGoogleSignIn() {
        Log.d(TAG, "Google Sign-In 시작...");
        // 기존 세션 로그아웃 후 진행 (선택적)
        mGoogleSignInClient.signOut().addOnCompleteListener(this, task -> {
            Intent signInIntent = mGoogleSignInClient.getSignInIntent();
            googleSignInLauncher.launch(signInIntent);
        });
    }

    // --- Google 로그인 결과 처리 ---
    private void handleGoogleSignInResult(Task<GoogleSignInAccount> completedTask) {
        try {
            GoogleSignInAccount account = completedTask.getResult(ApiException.class);
            if (account != null && account.getId() != null) {
                Log.i(TAG, "Google Sign-In 성공. Email: " + account.getEmail() + ", Google ID: " + account.getId());

                String idToken = account.getIdToken();
                String googleId = account.getId();
                String displayName = account.getDisplayName();
                String email = account.getEmail();
                Uri photoUri = account.getPhotoUrl();
                String photoUrlString = (photoUri != null) ? photoUri.toString() : null;

                // 이름이 없는 경우 기본값 설정
                if (displayName == null || displayName.isEmpty()) {
                    displayName = (email != null && !email.isEmpty()) ? email.split("@")[0]
                            : ("GoogleUser_" + googleId.substring(0, 5));
                }

                UserEntity googleUser = new UserEntity(
                        PROVIDER_GOOGLE, // loginProvider
                        googleId, // providerId
                        displayName,
                        email,
                        photoUrlString);
                saveOrUpdateUser(googleUser); // 공통 저장/업데이트 메소드 호출

                // google 로그인 API 호출
                apiHelper.loginWithGoogle(idToken, new ApiHelper.ApiCallback<AuthResponse>() {
                    @Override
                    public void onSuccess(AuthResponse authResponse) {
                        Log.d(TAG, "Google 로그인 API 호출 성공: " + authResponse.getAccess_token());
                        // authManager.saveAuthInfo()가 호출된 직후이므로 바로 subject 데이터 동기화 수행
                        subjectRepository.downloadAndReplaceFromServer(new SubjectRepository.CloudSyncCallback() {
                            @Override
                            public void onSuccess() {
                                Log.d(TAG, "Google 로그인 - Subject 데이터 동기화 성공");
                            }

                            @Override
                            public void onError(String message) {
                                Log.e(TAG, "Google 로그인 - Subject 데이터 동기화 오류: " + message);
                                // 동기화 실패해도 계속 진행
                                Toast.makeText(LoginActivity.this, "데이터 동기화에 실패했지만 계속 진행합니다.", Toast.LENGTH_SHORT)
                                        .show();
                            }
                        });
                    }

                    @Override
                    public void onError(String message) {
                        Log.e(TAG, "Google 로그인 API 호출 실패: " + message);
                        Toast.makeText(LoginActivity.this, "Google 로그인 처리 중 오류 발생", Toast.LENGTH_SHORT).show();
                    }
                });

            } else {
                Log.e(TAG, "GoogleSignInAccount 또는 Google ID가 null입니다.");
                Toast.makeText(this, "Google 계정 정보를 가져오지 못했습니다.", Toast.LENGTH_SHORT).show();
            }
        } catch (ApiException e) {
            handleGoogleSignInError(e);
        } catch (Exception e) {
            Log.e(TAG, "Google Sign-In 처리 중 예상치 못한 오류", e);
            Toast.makeText(this, "로그인 처리 중 오류가 발생했습니다.", Toast.LENGTH_SHORT).show();
        }
    }

    // --- Google 로그인 오류 처리 ---
    private void handleGoogleSignInError(ApiException e) {
        Log.w(TAG, "Google Sign-In 실패, code=" + e.getStatusCode() + ", message=" + e.getMessage());
        String errorMessage = "Google 로그인 오류 (" + e.getStatusCode() + ")";
        switch (e.getStatusCode()) {
            case GoogleSignInStatusCodes.SIGN_IN_CANCELLED:
                errorMessage = "로그인이 취소되었습니다.";
                break;
            case GoogleSignInStatusCodes.NETWORK_ERROR:
                errorMessage = "네트워크 연결을 확인해주세요.";
                break;
            // 기타 필요한 오류 코드 처리
        }
        Toast.makeText(this, errorMessage, Toast.LENGTH_LONG).show();
    }

    // --- Kakao 로그인 시작 ---
    private void startKakaoSignIn() {
        Log.d(TAG, "Kakao Sign-In 시작...");

        // 카카오 로그인 콜백 정의
        Function2<OAuthToken, Throwable, Unit> kakaoLoginCallback = (token, error) -> {
            if (error != null) {
                handleKakaoLoginError(error);
            } else if (token != null) {
                Log.i(TAG, "Kakao 로그인 성공. AccessToken: " + token.getAccessToken());
                fetchKakaoUserInfo();
            }
            return null; // Unit 반환
        };

        // 카카오톡 설치 여부 확인
        if (UserApiClient.getInstance().isKakaoTalkLoginAvailable(this)) {
            Log.d(TAG, "카카오톡으로 로그인 시도");
            UserApiClient.getInstance().loginWithKakaoTalk(this, kakaoLoginCallback);
        } else {
            Log.d(TAG, "카카오계정으로 로그인 시도");
            UserApiClient.getInstance().loginWithKakaoAccount(this, kakaoLoginCallback);
        }
    }

    // --- Kakao 로그인 오류 처리 ---
    private void handleKakaoLoginError(Throwable error) {
        Log.e(TAG, "Kakao 로그인 실패", error);
        String errorMessage = "카카오 로그인 실패";
        if (error instanceof ClientError && ((ClientError) error).getReason() == ClientErrorCause.Cancelled) {
            errorMessage = "카카오 로그인이 취소되었습니다.";
            Log.d(TAG, errorMessage);
            // 취소 시에는 Toast 메시지를 보여주지 않을 수도 있음
            return;
        } else if (error instanceof KakaoSdkError) {
            errorMessage += ": " + error.getLocalizedMessage();
        }
        Toast.makeText(this, errorMessage, Toast.LENGTH_SHORT).show();
    }

    // --- Kakao 사용자 정보 가져오기 ---
    private void fetchKakaoUserInfo() {
        Log.d(TAG, "Kakao 사용자 정보 요청...");
        UserApiClient.getInstance().me((user, error) -> {
            if (error != null) {
                Log.e(TAG, "Kakao 사용자 정보 요청 실패", error);
                Toast.makeText(this, "카카오 사용자 정보를 가져오는데 실패했습니다.", Toast.LENGTH_SHORT).show();
            } else if (user != null && user.getId() != null) {
                Log.i(TAG, "Kakao 사용자 정보 요청 성공. Kakao ID: " + user.getId());

                String kakaoId = String.valueOf(user.getId()); // Long -> String
                String nickname = user.getKakaoAccount().getProfile().getNickname();
                String email = user.getKakaoAccount().getEmail(); // 동의 항목에서 선택/필수 동의 필요
                String profileImageUrl = user.getKakaoAccount().getProfile().getThumbnailImageUrl();

                // 이름(닉네임)이 없는 경우 기본값 설정
                if (nickname == null || nickname.isEmpty()) {
                    nickname = (email != null && !email.isEmpty()) ? email.split("@")[0]
                            : ("KakaoUser_" + kakaoId.substring(0, 5));
                }

                UserEntity kakaoUser = new UserEntity(
                        PROVIDER_KAKAO, // loginProvider
                        kakaoId, // providerId
                        nickname,
                        email,
                        profileImageUrl);
                saveOrUpdateUser(kakaoUser); // 공통 저장/업데이트 메소드 호출
            } else {
                Log.e(TAG, "Kakao User 객체 또는 ID가 null입니다.");
                Toast.makeText(this, "카카오 사용자 정보를 가져오지 못했습니다.", Toast.LENGTH_SHORT).show();
            }
            return null; // Unit 반환
        });
    }

    // --- 공통 DB 저장/업데이트 및 후처리 ---
    private void saveOrUpdateUser(UserEntity user) {
        String loginProvider = user.loginProvider;
        String providerId = user.providerId;
        String displayName = user.name; // 저장될 이름

        if (providerId == null || providerId.isEmpty() || loginProvider == null || loginProvider.isEmpty()) {
            Log.e(TAG, "Provider 정보가 유효하지 않아 DB 작업을 중단합니다.");
            Toast.makeText(this, "로그인 정보 처리 중 오류가 발생했습니다.", Toast.LENGTH_SHORT).show();
            return;
        }

        // 복합 키로 DB 조회
        disposables.add(userDao.getUserByProviderInfo(loginProvider, providerId)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(
                        existingUser -> {
                            // 사용자가 이미 존재함 - 정보 업데이트 확인
                            Log.d(TAG, "기존 사용자 확인 (" + loginProvider + "): " + existingUser.name + " (ID: " + providerId
                                    + ")");
                            boolean needsUpdate = false;
                            if (user.name != null && !user.name.equals(existingUser.name)) {
                                existingUser.name = user.name;
                                needsUpdate = true;
                            }
                            if (user.email != null && !user.email.equals(existingUser.email)) {
                                existingUser.email = user.email;
                                needsUpdate = true;
                            }
                            if (user.photoUrl != null && !user.photoUrl.equals(existingUser.photoUrl)) {
                                existingUser.photoUrl = user.photoUrl;
                                needsUpdate = true;
                            }

                            if (needsUpdate) {
                                updateUserInDb(existingUser);
                            } else {
                                // 변경사항 없으면 바로 세션 저장 및 동기화 후 메인 이동
                                saveUserSession(loginProvider, providerId, existingUser.name);
                                goToMainActivity(existingUser.name, loginProvider, providerId);
                            }
                        },
                        error -> {
                            Log.e(TAG, "DB 사용자 조회 중 오류", error);
                            Toast.makeText(LoginActivity.this, "사용자 정보 확인 중 오류 발생", Toast.LENGTH_SHORT).show();
                        },
                        () -> {
                            // 사용자가 존재하지 않음 - 새로 등록
                            Log.d(TAG,
                                    "신규 사용자 등록 (" + loginProvider + "): " + displayName + " (ID: " + providerId + ")");
                            insertNewUserToDb(user);
                        }));
    }

    private void updateUserInDb(UserEntity userToUpdate) {
        disposables.add(userDao.updateUser(userToUpdate)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(
                        () -> {
                            Log.d(TAG, "사용자 정보 DB 업데이트 성공: " + userToUpdate.name);
                            saveUserSession(userToUpdate.loginProvider, userToUpdate.providerId, userToUpdate.name);

                            // 로그인 성공 후 subject 데이터 동기화
                            goToMainActivity(userToUpdate.name, userToUpdate.loginProvider,
                                    userToUpdate.providerId);
                        },
                        error -> {
                            Log.e(TAG, "사용자 정보 DB 업데이트 실패", error);
                            Toast.makeText(LoginActivity.this, "사용자 정보 업데이트에 실패했습니다.", Toast.LENGTH_SHORT).show();
                        }));
    }

    private void insertNewUserToDb(UserEntity newUser) {
        disposables.add(userDao.insertUser(newUser)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(
                        () -> {
                            Log.d(TAG, "신규 사용자 DB 등록 성공: " + newUser.name);
                            saveUserSession(newUser.loginProvider, newUser.providerId, newUser.name);

                            // 로그인 성공 후 subject 데이터 동기화
                            goToMainActivity(newUser.name, newUser.loginProvider, newUser.providerId);
                        },
                        error -> {
                            Log.e(TAG, "신규 사용자 DB 등록 실패", error);
                            Toast.makeText(LoginActivity.this, "사용자 정보 저장에 실패했습니다.", Toast.LENGTH_SHORT).show();
                            // 등록 실패 시 소셜 로그아웃 처리 (선택적)
                            signOutSocialAccount(newUser.loginProvider);
                        }));
    }

    // --- 세션 저장 및 화면 이동 ---
    private void saveUserSession(String loginProvider, String providerId, String displayName) {
        SharedPreferences.Editor editor = prefs.edit();
        editor.putString(KEY_LOGIN_PROVIDER, loginProvider);
        editor.putString(KEY_PROVIDER_ID, providerId);
        editor.putString(KEY_USER_DISPLAY_NAME, displayName);
        editor.apply();
        Log.i(TAG, "사용자 세션 저장: Provider=" + loginProvider + ", ID=" + providerId + ", Name=" + displayName);
    }

    private void goToMainActivity(String displayName, String loginProvider, String providerId) {
        Log.i(TAG, "MainActivity로 이동. User: " + displayName + " (" + loginProvider + ": " + providerId + ")");
        Intent intent = new Intent(this, MainActivity.class);
        // MainActivity에서 사용자 정보를 필요로 한다면 Intent에 추가
        intent.putExtra("USER_DISPLAY_NAME", displayName);
        intent.putExtra("LOGIN_PROVIDER", loginProvider);
        intent.putExtra("PROVIDER_ID", providerId);

        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
        finish();
    }

    // --- 공통 소셜 로그아웃 (선택적) ---
    private void signOutSocialAccount(String provider) {
        if (PROVIDER_GOOGLE.equals(provider) && mGoogleSignInClient != null) {
            mGoogleSignInClient.signOut().addOnCompleteListener(this,
                    task -> Log.d(TAG, "Google 계정 로그아웃 시도 완료 (등록 실패 후)"));
        } else if (PROVIDER_KAKAO.equals(provider)) {
            UserApiClient.getInstance().logout(error -> {
                if (error == null)
                    Log.d(TAG, "Kakao 계정 로그아웃 시도 완료 (등록 실패 후)");
                else
                    Log.e(TAG, "Kakao 로그아웃 실패 (등록 실패 후)", error);
                return null;
            });
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        disposables.clear();
        Log.d(TAG, "LoginActivity onDestroy: Disposables cleared.");
    }
}