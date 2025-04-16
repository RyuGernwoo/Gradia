package mp.gradia.database.entity;

import androidx.annotation.NonNull;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(
        tableName = "User"
)
public class UserEntity {

    // 사용자 이메일
    @PrimaryKey
    @NonNull
    @ColumnInfo(name = "user_id")
    public String uid;

    // 사용자 비밀 번호
    @ColumnInfo(name = "password")
    public String password;

    // 사용자 이름
    @ColumnInfo(name = "name")
    public String name;

    // 사용자 성별
    @ColumnInfo(name = "gender")
    public boolean gender;

    // 사용자 나이
    @ColumnInfo(name = "age")
    public int age;

    // 전공
    @ColumnInfo(name = "major")
    public String major;
}