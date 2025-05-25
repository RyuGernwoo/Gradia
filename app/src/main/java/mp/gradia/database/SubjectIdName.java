package mp.gradia.database;

import androidx.room.ColumnInfo;

public class SubjectIdName {

    @ColumnInfo(name = "subject_id")
    public int subjectId;

    @ColumnInfo(name = "name")
    public String subjectName;

    public SubjectIdName(int subjectId, String subjectName) {
        this.subjectId = subjectId;
        this.subjectName = subjectName;
    }

    public int getSubjectId() {
        return subjectId;
    }

    public String getSubjectName() {
        return subjectName;
    }
}

