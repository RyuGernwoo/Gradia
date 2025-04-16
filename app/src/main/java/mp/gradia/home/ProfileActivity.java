package mp.gradia.home;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.*;
import androidx.appcompat.app.AppCompatActivity;

import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.schedulers.Schedulers;
import mp.gradia.R;
import mp.gradia.login.LoginActivity;
import mp.gradia.database.AppDatabase;
import mp.gradia.database.entity.UserEntity;

public class ProfileActivity extends AppCompatActivity {

    private ImageView imgProfile;
    private TextView tvName, tvUserId;
    private Button btnChangePassword, btnLogout;

    private String userId; // 로그인한 유저 ID

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);

        imgProfile = findViewById(R.id.img_profile);
        tvName = findViewById(R.id.tv_name);
        tvUserId = findViewById(R.id.tv_user_id);
        btnChangePassword = findViewById(R.id.btn_change_password);
        btnLogout = findViewById(R.id.btn_logout);

        // SharedPreferences에서 로그인 정보 가져오기
        SharedPreferences prefs = getSharedPreferences("login_prefs", MODE_PRIVATE);
        userId = prefs.getString("userId", null);

        if (userId == null) {
            Toast.makeText(this, "로그인 정보가 없습니다", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        // 사용자 정보 불러오기
        AppDatabase.getInstance(getApplicationContext()).userDao()
                .getUserById(userId)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(user -> {
                    if (user != null) {
                        tvName.setText(user.name);
                        tvUserId.setText("ID: " + user.uid);
                        // 추가 정보 표시 가능: age, gender, major 등
                    }
                }, err -> {
                    Toast.makeText(this, "유저 정보 불러오기 실패", Toast.LENGTH_SHORT).show();
                });

        // 비밀번호 변경 버튼 (임시 동작 예시)
        btnChangePassword.setOnClickListener(v -> {
            Toast.makeText(this, "비밀번호 변경 기능 준비 중", Toast.LENGTH_SHORT).show();
            // 또는 ChangePasswordActivity 등으로 이동
        });

        // 로그아웃 버튼
        btnLogout.setOnClickListener(v -> {
            SharedPreferences.Editor editor = prefs.edit();
            editor.clear();
            editor.apply();

            Intent intent = new Intent(ProfileActivity.this, LoginActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK); // 뒤로가기 막기
            startActivity(intent);
            finish();
        });
    }
}
