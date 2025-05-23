package mp.gradia.time.inner.record.bottomsheet.adapter;

import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import mp.gradia.R;
import mp.gradia.database.entity.SubjectEntity;

// ViewHolder caches item views for RecyclerView to improve scroll performance and avoid redundant findViewById calls
public class SubjectSelectHolder extends RecyclerView.ViewHolder {
    // UI Components
    View detail;
    TextView subjectName;
    TextView studyTime;
    TextView subjectCredit;
    ImageView colorCircle;
    ImageButton expandImgBtn;

    /**
     * SubjectSelectHolder의 생성자입니다.
     * @param v 뷰를 포함하는 아이템 뷰입니다.
     */
    public SubjectSelectHolder(@NonNull View v) {
        super(v);
        subjectName = v.findViewById(R.id.subject_name);
        studyTime = v.findViewById(R.id.study_time);
        subjectCredit = v.findViewById(R.id.subject_credit);
        colorCircle = v.findViewById(R.id.color_circle);
        detail = v.findViewById(R.id.layout_expand);
        expandImgBtn = v.findViewById(R.id.expand_img_btn);
    }

    /**
     * ViewHolder에 SubjectEntity 데이터를 바인딩합니다.
     * @param subject 바인딩할 SubjectEntity 객체입니다.
     * @param context 애플리케이션 Context입니다.
     * @param listener 아이템 클릭 리스너입니다.
     */
    public void bind(SubjectEntity subject, Context context, SubjectSelectAdapter.OnItemClickListener listener) {
        String timeStr = "0 시간";
        String creditStr = subject.getCredit() + " 학점";

        subjectName.setText(subject.getName());
        studyTime.setText(timeStr);
        subjectCredit.setText(creditStr);

        // 각 과목의 색상을 나타내는 원형 이미지 설정
        Drawable baseDrawable = ContextCompat.getDrawable(context, R.drawable.color_circle);
        if (baseDrawable instanceof GradientDrawable) {
            ((GradientDrawable) baseDrawable).setColor(Color.parseColor(subject.getColor()));
            colorCircle.setImageDrawable(baseDrawable);
        }

        // 아이템 클릭시 현재 클릭된 subject 객체를 넘겨줌
        itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onItemClick(subject);
            }
        });
    }
}