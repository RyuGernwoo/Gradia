package mp.gradia.database.entity;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.room.ColumnInfo;
import androidx.room.Entity;

// 복합 기본 키 정의: loginProvider와 providerId를 묶어서 PK로 사용
@Entity(tableName = "User", primaryKeys = {"login_provider", "provider_id"})
public class UserEntity {

    // 로그인 제공자 ("google" 또는 "kakao")
    @NonNull
    @ColumnInfo(name = "login_provider")
    public String loginProvider;

    // 제공자별 고유 ID (Google ID 또는 Kakao ID)
    @NonNull
    @ColumnInfo(name = "provider_id")
    public String providerId; // Kakao ID는 Long 타입이지만, 문자열로 통일하여 저장

    // 사용자 이름 (표시 이름)
    @Nullable
    @ColumnInfo(name = "name")
    public String name;

    // 사용자 이메일
    @Nullable
    @ColumnInfo(name = "email")
    public String email;

    // 프로필 사진 URL
    @Nullable
    @ColumnInfo(name = "photo_url")
    public String photoUrl;

    // 모든 필드를 받는 생성자
    public UserEntity(@NonNull String loginProvider, @NonNull String providerId,
                      @Nullable String name, @Nullable String email, @Nullable String photoUrl) {
        this.loginProvider = loginProvider;
        this.providerId = providerId;
        this.name = name;
        this.email = email;
        this.photoUrl = photoUrl;
    }

    // Room이 사용할 기본 생성자
    public UserEntity() {}
}