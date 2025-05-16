package mp.gradia.database.entity;

import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.PrimaryKey;

@Entity(
        tableName = "todos",
        foreignKeys = @ForeignKey(
                entity = SubjectEntity.class,
                parentColumns = "subject_id",
                childColumns = "subject_id",
                onDelete = ForeignKey.CASCADE
        )
)
public class TodoEntity {
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "todo_id")
    public int todoId;

    @ColumnInfo(name = "subject_id", index = true)
    public int subjectId;

    @ColumnInfo(name = "content")
    public String content;

    @ColumnInfo(name = "is_done")
    public boolean isDone;
}

