package mp.gradia.database.entity;

public class SubjectStudyTime {

    private int subjectId;
    private long totalTime;

    public SubjectStudyTime(int subjectId, long totalTime) {
        this.subjectId = subjectId;
        this.totalTime = totalTime;
    }

    public int getSubjectId() {
        return subjectId;
    }

    public long getTotalTime() {
        return totalTime;
    }
}


