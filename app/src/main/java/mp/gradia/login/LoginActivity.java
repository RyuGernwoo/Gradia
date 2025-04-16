package mp.gradia.login;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.disposables.CompositeDisposable;
import io.reactivex.rxjava3.schedulers.Schedulers;
import mp.gradia.R;
import mp.gradia.main.MainActivity;
import mp.gradia.database.AppDatabase;
import mp.gradia.database.dao.UserDao;

public class LoginActivity extends AppCompatActivity {

    private EditText idEditText, pwEditText;
    private AppDatabase db;
    private final CompositeDisposable disposables = new CompositeDisposable();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // 자동 로그인
        SharedPreferences prefs = getSharedPreferences("login_prefs", MODE_PRIVATE);
        boolean autoLogin = prefs.getBoolean("autoLogin", false);
        String savedId = prefs.getString("userId", null);

        if (savedId != null) {
            // 이미 로그인 상태 → 바로 MainActivity로
            Intent intent = new Intent(this, MainActivity.class);
            startActivity(intent);
            finish();
        }

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        idEditText = findViewById(R.id.id_edit);
        pwEditText = findViewById(R.id.password_edit);
        Button loginButton = findViewById(R.id.btn_login);
        Button registerButton = findViewById(R.id.btn_register);
        CheckBox autoLoginCheckBox = findViewById(R.id.checkbox_auto_login);

        db = AppDatabase.getInstance(getApplicationContext());

        // 로그인
        loginButton.setOnClickListener(v -> {
            String uid = idEditText.getText().toString();
            String pw = pwEditText.getText().toString();

            disposables.add(
                    db.userDao().login(uid, pw)
                            .subscribeOn(Schedulers.io())
                            .observeOn(AndroidSchedulers.mainThread())
                            .subscribe(user -> {
                                // 로그인 성공
                                SharedPreferences.Editor editor = prefs.edit();
                                editor.putString("userId", user.uid);
                                editor.putString("userName", user.name);
                                editor.putBoolean("autoLogin", autoLoginCheckBox.isChecked());
                                editor.apply();

                                Intent intent = new Intent(LoginActivity.this, MainActivity.class);
                                startActivity(intent);
                                finish();
                            }, error -> {
                                Toast.makeText(this, "로그인 오류: " + error.getMessage(), Toast.LENGTH_SHORT).show();
                            }, () -> {
                                // 로그인 실패 (아이디/비밀번호 불일치)
                                Toast.makeText(this, "아이디 또는 비밀번호가 틀렸습니다", Toast.LENGTH_SHORT).show();
                            })
            );
        });

        // 회원 가입
        registerButton.setOnClickListener(v -> {
            startActivity(new Intent(this, RegisterActivity.class));
        });
    }
    @Override
    protected void onDestroy() {
        super.onDestroy();
        disposables.clear(); // 모든 구독 해제
    }
}
