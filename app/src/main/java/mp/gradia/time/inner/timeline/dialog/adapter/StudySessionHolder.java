package mp.gradia.time.inner.timeline.dialog.adapter;

import android.graphics.drawable.Drawable;
import android.util.Log;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import mp.gradia.R;
import mp.gradia.database.entity.StudySessionEntity;

/**
 * RecyclerView의 각 아이템 뷰를 캐시하여 스크롤 성능을 향상시키고 findViewById 호출 중복을 방지합니다.
 */
public class StudySessionHolder extends RecyclerView.ViewHolder {
    TextView subjectName;
    TextView percent;
    TextView minute;
    ProgressBar progressBar;

    /**
     * StudySessionHolder의 생성자입니다.
     * @param v 뷰를 포함하는 아이템 뷰입니다.
     */
    public StudySessionHolder(@NonNull View v) {
        super(v);
        subjectName = v.findViewById(R.id.subject_name_textview);
        percent = v.findViewById(R.id.subject_percent_textview);
        minute = v.findViewById(R.id.subject_minute_textview);
        progressBar = v.findViewById(R.id.progress_bar);
    }

    /**
     * ViewHolder에 StudySessionEntity 데이터를 바인딩합니다.
     * @param session 바인딩할 StudySessionEntity 객체입니다.
     * @param color 진행률 표시줄에 적용할 색상입니다.
     * @param totalTime 총 학습 시간입니다.
     */
    public void bind(StudySessionEntity session, int color, long totalTime) {
        long p = (session.getStudyTime() * 100 ) / totalTime;
        String percentStr = p + "%";
        String minuteStr = session.getStudyTime() + "분";

        subjectName.setText(session.getSubjectName());
        percent.setText(percentStr);
        minute.setText(minuteStr);
        Drawable progressDrawable = progressBar.getProgressDrawable().mutate();
        progressDrawable.setColorFilter(color, android.graphics.PorterDuff.Mode.SRC_IN);
        progressBar.setProgressDrawable(progressDrawable);
        progressBar.setProgress((int) p);
    }
}