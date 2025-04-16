package mp.gradia.login;

import android.content.Intent;
import android.os.Bundle;
import android.widget.*;
import androidx.appcompat.app.AppCompatActivity;

import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.disposables.CompositeDisposable;
import io.reactivex.rxjava3.schedulers.Schedulers;
import mp.gradia.R;
import mp.gradia.database.AppDatabase;
import mp.gradia.database.entity.UserEntity;

public class RegisterActivity extends AppCompatActivity {

    private EditText etEmail, etName, etPassword, etPasswordConfirm, etAge, etMajor;
    private RadioGroup rgGender;
    private Button btnRegister, btnCancel;
    private final CompositeDisposable disposables = new CompositeDisposable();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        // 뷰 연결
        etEmail = findViewById(R.id.et_email);
        etName = findViewById(R.id.et_name);
        etPassword = findViewById(R.id.et_password);
        etPasswordConfirm = findViewById(R.id.et_password_confirm);
        etAge = findViewById(R.id.et_age);
        etMajor = findViewById(R.id.et_major);
        rgGender = findViewById(R.id.rg_gender);
        btnRegister = findViewById(R.id.btn_register);
        btnCancel = findViewById(R.id.btn_cancel);

        // 회원가입 버튼 클릭
        btnRegister.setOnClickListener(v -> {
            String email = etEmail.getText().toString().trim();
            String name = etName.getText().toString().trim();
            String pw = etPassword.getText().toString().trim();
            String pwCheck = etPasswordConfirm.getText().toString().trim();
            String major = etMajor.getText().toString().trim();

            int age;
            try {
                age = Integer.parseInt(etAge.getText().toString().trim());
            } catch (NumberFormatException e) {
                Toast.makeText(this, "나이를 숫자로 입력하세요", Toast.LENGTH_SHORT).show();
                return;
            }

            boolean gender = rgGender.getCheckedRadioButtonId() == R.id.rb_male;

            if (email.isEmpty() || name.isEmpty() || pw.isEmpty() || pwCheck.isEmpty() || major.isEmpty()) {
                Toast.makeText(this, "모든 항목을 입력하세요", Toast.LENGTH_SHORT).show();
                return;
            }

            if (!pw.equals(pwCheck)) {
                Toast.makeText(this, "비밀번호가 일치하지 않습니다", Toast.LENGTH_SHORT).show();
                return;
            }

            // 이메일 중복 체크
            disposables.add(
                    AppDatabase.getInstance(getApplicationContext()).userDao()
                            .getUserById(email)
                            .subscribeOn(Schedulers.io())
                            .observeOn(AndroidSchedulers.mainThread())
                            .subscribe(user -> {
                                // 이메일 중복
                                Toast.makeText(this, "이미 등록된 이메일입니다", Toast.LENGTH_SHORT).show();
                            }, error -> {
                                Toast.makeText(this, "중복 확인 실패", Toast.LENGTH_SHORT).show();
                            }, () -> {
                                // ✅ 아무 값도 없을 경우 → insert 진행
                                insertUser(email, name, pw, gender, age, major);
                            })
            );
        });

        // 취소 버튼 → 뒤로 가기
        btnCancel.setOnClickListener(v -> finish());
    }

    private void insertUser(String email, String name, String password, boolean gender, int age, String major) {
        UserEntity user = new UserEntity();
        user.uid = email;
        user.name = name;
        user.password = password;
        user.gender = gender;
        user.age = age;
        user.major = major;

        disposables.add(
        AppDatabase.getInstance(getApplicationContext()).userDao()
                .insert(user)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(() -> {
                    Toast.makeText(this, "회원가입 성공", Toast.LENGTH_SHORT).show();
                    finish(); // 로그인 화면으로 돌아감
                }, err -> {
                    Toast.makeText(this, "회원가입 실패: " + err.getMessage(), Toast.LENGTH_SHORT).show();
                })
        );
    }
    @Override
    protected void onDestroy() {
        super.onDestroy();
        disposables.clear(); // 모든 구독 해제
    }
}
