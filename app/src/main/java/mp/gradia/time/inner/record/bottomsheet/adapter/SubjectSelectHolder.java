package mp.gradia.time.inner.record.bottomsheet.adapter;

import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import mp.gradia.R;
import mp.gradia.database.entity.SubjectEntity;
import mp.gradia.subject.Subject;
import mp.gradia.subject.ui.SubjectAddDialog;
import mp.gradia.time.inner.record.dialog.SessionAddDialog;

// ViewHolder caches item views for RecyclerView to improve scroll performance and avoid redundant findViewById calls
public class SubjectSelectHolder extends RecyclerView.ViewHolder {
    // UI Components
    View detail;
    TextView subjectName;
    TextView studyTime;
    TextView subjectCredit;
    TextView subjectType;
    ImageView colorCircle;
    ImageButton expandImgBtn;
    Button subjectEditBtn;
    private int dailyTargetStudyTime = -1;
    private int totalStudyTime = -1;

    /**
     * SubjectSelectHolder의 생성자입니다.
     * @param v 뷰를 포함하는 아이템 뷰입니다.
     */
    public SubjectSelectHolder(@NonNull View v) {
        super(v);
        subjectName = v.findViewById(R.id.subject_name);
        studyTime = v.findViewById(R.id.target_study_time);
        subjectType = v.findViewById(R.id.subject_type);
        subjectCredit = v.findViewById(R.id.subject_credit);
        colorCircle = v.findViewById(R.id.color_circle);
        detail = v.findViewById(R.id.layout_expand);
        expandImgBtn = v.findViewById(R.id.expand_img_btn);
        subjectEditBtn = v.findViewById(R.id.subject_edit);
    }

    /**
     * ViewHolder에 SubjectEntity 데이터를 바인딩합니다.
     * @param subject 바인딩할 SubjectEntity 객체입니다.
     * @param context 애플리케이션 Context입니다.
     * @param listener 아이템 클릭 리스너입니다.
     */
    public void bind(SubjectEntity subject, Context context, SubjectSelectAdapter.OnItemClickListener itemClickListener, OnSubjectEditButtonClickListener buttonClickListener) {
        if (subject.getTime() != null && subject.getTime().getDailyTargetStudyTime() != -1)
            dailyTargetStudyTime = subject.getTime().getDailyTargetStudyTime();

        String timeStr = (dailyTargetStudyTime != -1) ? dailyTargetStudyTime + " 시간" : "학습 목표 없음";
        String creditStr = subject.getCredit() + " 학점";

        subjectName.setText(subject.getName());
        setSubjectType(subject.getType());
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
            if (itemClickListener != null) {
                itemClickListener.onItemClick(subject);
            }
        });

        subjectEditBtn.setOnClickListener(v -> {
            if (buttonClickListener != null) {
                buttonClickListener.onEditButtonClick(subject.getSubjectId());
            }
        });
    }

    private void setSubjectType(int type) {
        switch (type) {
            case SubjectEntity.REQUIRED_SUBJECT:
                subjectType.setText("전필");
                break;
            case SubjectEntity.ELECTIVE_SUBJECT:
                subjectType.setText("전선");
                break;
            case SubjectEntity.LIB_SUBJECT:
                subjectType.setText("교양");
                break;
        }
    }
}