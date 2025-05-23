package mp.gradia.time.inner.record.bottomsheet.adapter;

import android.content.Context;
import android.transition.TransitionManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

import mp.gradia.R;
import mp.gradia.database.entity.SubjectEntity;

public class SubjectSelectAdapter extends RecyclerView.Adapter<SubjectSelectHolder> {
    // Data List and Context
    private List<SubjectEntity> itemList;
    private Context context;

    // Item Click Listener
    private OnItemClickListener listener;

    // Item 클릭 콜백 정의
    public interface OnItemClickListener {
        /**
         * 아이템 클릭 시 호출됩니다.
         * @param item 클릭된 SubjectEntity 객체입니다.
         */
        void onItemClick(SubjectEntity item);
    }

    /**
     * SubjectSelectAdapter의 생성자입니다. 데이터 목록, Context, 클릭 리스너를 인자로 받습니다.
     * @param itemList 표시할 SubjectEntity 객체 목록입니다.
     * @param context 애플리케이션 Context입니다.
     * @param listener 아이템 클릭 리스너입니다.
     */
    public SubjectSelectAdapter(List<SubjectEntity> itemList, Context context, OnItemClickListener listener) {
        this.itemList = itemList;
        this.context = context;
        this.listener = listener;
    }

    /**
     * 아이템 View와 ViewHolder 객체를 생성합니다.
     * @param parent 새로운 View가 속하게 될 ViewGroup입니다.
     * @param viewType 새로운 View의 View Type입니다.
     * @return 새로 생성된 SubjectSelectHolder 객체입니다.
     */
    @NonNull
    @Override
    public SubjectSelectHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new SubjectSelectHolder(LayoutInflater.from(parent.getContext()).inflate(R.layout.item_subject_list, parent, false));
    }

    /**
     * 지정된 위치(position)의 데이터를 ViewHolder에 바인딩합니다.
     * @param holder 데이터를 바인딩할 SubjectSelectHolder 객체입니다.
     * @param position 바인딩할 데이터의 위치입니다.
     */
    @Override
    public void onBindViewHolder(@NonNull SubjectSelectHolder holder, int position) {
        SubjectEntity item = itemList.get(position);
        // Bind subject data to view components
        holder.bind(item, context, listener);

        boolean isExpanded = item.isExpanded();
        TransitionManager.beginDelayedTransition((ViewGroup) holder.itemView);

        holder.detail.setVisibility(isExpanded ? View.VISIBLE : View.GONE);
        holder.expandImgBtn.setImageResource(
                isExpanded ? R.drawable.outline_expand_less_black_24 : R.drawable.outline_expand_more_black_24
        );

        holder.expandImgBtn.setOnClickListener(v -> {
            item.setExpanded(!isExpanded);
            notifyItemChanged(position);
        });
    }

    /**
     * 전체 아이템 개수를 반환합니다.
     * @return 어댑터가 관리하는 전체 아이템 수입니다.
     */
    @Override
    public int getItemCount() {
        return itemList.size();
    }

}