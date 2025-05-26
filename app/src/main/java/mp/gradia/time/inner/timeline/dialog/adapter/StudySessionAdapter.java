package mp.gradia.time.inner.timeline.dialog.adapter;

import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

import mp.gradia.R;
import mp.gradia.database.entity.StudySessionEntity;

public class StudySessionAdapter extends RecyclerView.Adapter<StudySessionHolder> {
    private List<StudySessionEntity> sessionList;
    private int[] colors;
    private Context context;
    private long totalTime = 0;
    private long[] targetTime;

    /**
     * StudySessionAdapter의 생성자입니다.
     * @param context 애플리케이션 Context입니다.
     * @param sessionList StudySessionEntity 객체 목록입니다.
     * @param colors 각 세션에 해당하는 색상 배열입니다.
     * @param totalTime 전체 학습 시간입니다.
     */
    public StudySessionAdapter(Context context, List<StudySessionEntity> sessionList, int[] colors, long totalTime) {
        this.context = context;
        this.sessionList = sessionList;
        this.colors = colors;
        this.totalTime = totalTime;
    }

    public StudySessionAdapter(Context context, List<StudySessionEntity> sessionList, int[] colors, long[] targetTime) {
        this.context = context;
        this.sessionList = sessionList;
        this.colors = colors;
        this.targetTime = targetTime;
    }

    /**
     * 새로운 ViewHolder 객체를 생성합니다.
     * @param parent 새로운 View가 속하게 될 ViewGroup입니다.
     * @param viewType 새로운 View의 View Type입니다.
     * @return 새로 생성된 StudySessionHolder 객체입니다.
     */
    @NonNull
    @Override
    public StudySessionHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new StudySessionHolder(LayoutInflater.from(parent.getContext()).inflate(R.layout.item_statistical_list, parent, false));
    }

    /**
     * 지정된 위치의 데이터를 ViewHolder에 바인딩합니다.
     * @param holder 데이터를 바인딩할 StudySessionHolder 객체입니다.
     * @param position 바인딩할 데이터의 위치입니다.
     */
    @NonNull
    @Override
    public void onBindViewHolder(@NonNull StudySessionHolder holder, int position) {
        StudySessionEntity session = sessionList.get(position);
        int color = colors[position];
        if (targetTime == null)
            holder.bind(session, color, totalTime);
        else if (targetTime != null)
            holder.bind(session, color, targetTime[position]);
    }

    /**
     * 어댑터가 관리하는 전체 아이템 수를 반환합니다.
     * @return 전체 아이템 수입니다.
     */
    @Override
    public int getItemCount() {
        if (sessionList != null)
            return sessionList.size();
        else
            return 0;
    }

}