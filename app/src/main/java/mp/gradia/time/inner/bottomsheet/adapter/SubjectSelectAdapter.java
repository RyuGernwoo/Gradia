package mp.gradia.time.inner.bottomsheet.adapter;

import android.content.Context;
import android.graphics.drawable.GradientDrawable;
import android.transition.TransitionManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;


import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

import mp.gradia.R;
import mp.gradia.time.inner.bottomsheet.Subject;

// Adapter connects data source to RecyclerView
public class SubjectSelectAdapter extends RecyclerView.Adapter<SubjectSelectHolder> {
    private List<Subject> itemList;
    private Context context;
    private OnItemClickListener listener;

    public interface OnItemClickListener {
        void onItemClick(Subject item);
    }

    public SubjectSelectAdapter(List<Subject> itemList, Context context, OnItemClickListener listener) {
        this.itemList = itemList;
        this.context = context;
        this.listener = listener;
    }

    @NonNull
    @Override
    public SubjectSelectHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new SubjectSelectHolder(LayoutInflater.from(parent.getContext()).inflate(R.layout.item_subject_list, parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull SubjectSelectHolder holder, int position) {
        Subject item = itemList.get(position);
        // Bind subject data to view components
        holder.bind(item, context, listener);

        boolean isExpanded = item.isExpanded();
        TransitionManager.beginDelayedTransition((ViewGroup) holder.itemView);

        holder.detail.setVisibility(isExpanded ? View.VISIBLE : View.GONE);
        holder.expandImgBtn.setImageResource(
                isExpanded ?  R.drawable.outline_expand_less_black_24 : R.drawable.outline_expand_more_black_24
        );

        holder.expandImgBtn.setOnClickListener(v -> {
            item.setExpanded(!isExpanded);
            notifyItemChanged(position);
        });
    }

    @Override
    public int getItemCount() {
        return itemList.size();
    }

}
