package mp.gradia.subject.adapter;

import static androidx.core.content.ContentProviderCompat.requireContext;

import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

import mp.gradia.R;
import mp.gradia.database.entity.SubjectEntity;

public class SubjectAdapter extends RecyclerView.Adapter<SubjectAdapter.SubjectViewHolder> {

    private Context context;
    // 과목 리스트 변수
    private List<SubjectEntity> subjectList = new ArrayList<>();

    // 아이템 클릭 시 처리, 리스너 인터페이스
    private OnItemClickListener listener;

    // 외부에서 클릭 이벤트 받음
    public interface OnItemClickListener {
        void onItemClick(SubjectEntity subject);
    }

    // 외부에서 클릭 리스너를 설정
    public void setOnItemClickListener(OnItemClickListener listener) {
        this.listener = listener;
    }

    public SubjectAdapter(Context context, List<SubjectEntity> subjectList) {
        this.context = context;
        this.subjectList = subjectList;
    }

    // 과목 리스트를 설정, 리사이클러뷰 갱신
    public void setSubjects(List<SubjectEntity> subjects) {
        this.subjectList = subjects;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public SubjectViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        // 아이템 레이아웃 생성
        View itemView = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_subject, parent, false);
        return new SubjectViewHolder(itemView);
    }

    @Override
    public void onBindViewHolder(@NonNull SubjectViewHolder holder, int position) {
        // 과목 데이터 바인딩
        SubjectEntity subject = subjectList.get(position);
        holder.textSubjectName.setText(subject.name); // 과목명 설정

        int color = Color.parseColor(subject.getColor());
        Drawable baseDrawable = ContextCompat.getDrawable(context, R.drawable.color_circle);
        if (baseDrawable instanceof GradientDrawable) {
            ((GradientDrawable) baseDrawable).setColor(color);
            holder.circle.setImageDrawable(baseDrawable);
        }

        // 과목 유형에 따라 텍스트 변환
        String typeStr;
        switch (subject.type) {
            case 0: typeStr = "전필"; break;
            case 1: typeStr = "전선"; break;
            case 2: typeStr = "교양"; break;
            default: typeStr = "기타"; break;
        }

        // 유형 + 학점 정보 표시
        holder.textSubjectType.setText(typeStr + " / " + subject.credit + "학점");

        // 주간 목표 시간 표시
        if (subject.time != null) {
            holder.textSubjectTime.setText("주간 목표: " + subject.time.weeklyTargetStudyTime + "시간");
        } else {
            holder.textSubjectTime.setText("주간 목표: 정보 없음");
        }
    }

    @Override
    public int getItemCount() {
        return subjectList.size(); // 전체 과목 수 반환
    }

    // 한 아이템의 뷰 관리
    class SubjectViewHolder extends RecyclerView.ViewHolder {
        private final TextView textSubjectName;
        private final TextView textSubjectType;
        private final TextView textSubjectTime;
        private ImageView circle;

        public SubjectViewHolder(@NonNull View itemView) {
            super(itemView);

            circle = itemView.findViewById(R.id.color_circle);

            // 각 TextView 연결
            textSubjectName = itemView.findViewById(R.id.textSubjectName);
            textSubjectType = itemView.findViewById(R.id.textSubjectType);
            textSubjectTime = itemView.findViewById(R.id.textSubjectTime);

            // 아이템 클릭 이벤트 처리
            itemView.setOnClickListener(v -> {
                int position = getAdapterPosition();
                if (listener != null && position != RecyclerView.NO_POSITION) {
                    SubjectEntity clickedSubject = subjectList.get(position);
                    Log.d("SubjectAdapter", "Clicked: " + clickedSubject.name); // 클릭 로그 출력
                    listener.onItemClick(clickedSubject); // 클릭된 과목 전달
                }
            });
        }
    }
}

