package mp.gradia.time;

import android.content.Context;
import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.viewpager2.widget.ViewPager2;

import mp.gradia.R;

public class TimeFragment extends Fragment {
    private static final int PAGE_RECORD = 0;
    private static final int PAGE_TIMELINE = 1;

    private ViewPager2 viewPager;
    private ToggleViewHolder recordToggle;
    private ToggleViewHolder timelineToggle;

    private static class ToggleViewHolder {
        LinearLayout container;
        ImageView icon;
        TextView text;

        ToggleViewHolder(View parent, int containerId, int iconId, int textId) {
            container = parent.findViewById(containerId);
            icon = parent.findViewById(iconId);
            text = parent.findViewById(textId);
        }

        void setSelected(Context context, boolean isSelected) {
            if (isSelected) {
                container.setBackgroundResource(R.drawable.toggle_selected_background);
                text.setTextColor(ContextCompat.getColor(context, R.color.black));
                icon.setColorFilter(ContextCompat.getColor(context, R.color.black));
            } else {
                container.setBackgroundColor(Color.TRANSPARENT);
                text.setTextColor(ContextCompat.getColor(context, R.color.gray));
                icon.setColorFilter(ContextCompat.getColor(context, R.color.gray));
            }
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_time_main, container, false);

        viewPager = v.findViewById(R.id.view_pager);
        viewPager.setAdapter(new TimeFragmentAdapter(this));

        recordToggle = new ToggleViewHolder(v, R.id.record_toggle, R.id.record_icon, R.id.record_text);
        timelineToggle = new ToggleViewHolder(v, R.id.timeline_toggle, R.id.timeline_icon, R.id.timeline_text);

        setupToggleListener();
        setupViewPagerCallBack();

        return v;
    }

    private void setupToggleListener() {
        recordToggle.container.setOnClickListener(v -> {
            setToggleSelected(0);
            viewPager.setCurrentItem(PAGE_RECORD, true);
        });

        timelineToggle.container.setOnClickListener(v -> {
            setToggleSelected(1);
            viewPager.setCurrentItem(PAGE_TIMELINE, true);
        });
    }

    private void setupViewPagerCallBack() {
        viewPager.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageSelected(int position) {
                setToggleSelected(position);
            }
        });
    }

    private void setToggleSelected(int position) {
        recordToggle.setSelected(requireContext(), position == 0);
        timelineToggle.setSelected(requireContext(), position == 1);
    }
}
