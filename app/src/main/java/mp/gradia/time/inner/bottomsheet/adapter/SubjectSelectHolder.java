package mp.gradia.time.inner.bottomsheet.adapter;

import android.content.Context;
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
import mp.gradia.time.inner.bottomsheet.Subject;

// ViewHolder caches item views for RecyclerView to improve scroll performance and avoid redundant findViewById calls
public class SubjectSelectHolder extends RecyclerView.ViewHolder {
    View detail;
    TextView subjectName;
    TextView studyTime;
    TextView subjectCredit;
    ImageView colorCircle;
    ImageButton expandImgBtn;

    public SubjectSelectHolder(@NonNull View v) {
        super(v);
        subjectName = v.findViewById(R.id.subject_name);
        studyTime = v.findViewById(R.id.study_time);
        subjectCredit = v.findViewById(R.id.subject_credit);
        colorCircle = v.findViewById(R.id.color_circle);
        detail = v.findViewById(R.id.layout_expand);
        expandImgBtn = v.findViewById(R.id.expand_img_btn);
    }

    public void bind(SubjectEntity subject, Context context, SubjectSelectAdapter.OnItemClickListener listener) {
        String timeStr = "0 시간";
        String creditStr = subject.getCredit() + " 학점";

        subjectName.setText(subject.getName());
        studyTime.setText(timeStr);
        subjectCredit.setText(creditStr);

        Drawable baseDrawable = ContextCompat.getDrawable(context, R.drawable.color_circle);
        if (baseDrawable instanceof GradientDrawable) {
            ((GradientDrawable) baseDrawable).setColor(ContextCompat.getColor(context, subject.getColor()));
            colorCircle.setImageDrawable(baseDrawable);
        }

        itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onItemClick(subject);
            }
        });
    }
}
