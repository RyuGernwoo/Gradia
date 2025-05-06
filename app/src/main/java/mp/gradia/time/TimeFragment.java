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

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_time_main, container, false);

        initViewPager(v);
        initToggles(v);
        setupToggleListeners();
        setupViewPagerCallback();

        return v;
    }

    private void initViewPager(View v) {
        viewPager = v.findViewById(R.id.view_pager);
        viewPager.setAdapter(new TimeFragmentAdapter(this));
    }

    private void initToggles(View v) {
        recordToggle = new ToggleViewHolder(v, R.id.record_toggle, R.id.record_icon, R.id.record_text);
        timelineToggle = new ToggleViewHolder(v, R.id.timeline_toggle, R.id.timeline_icon, R.id.timeline_text);
    }

    private void setupToggleListeners() {
        recordToggle.container.setOnClickListener(v -> selectPage(PAGE_RECORD));
        timelineToggle.container.setOnClickListener(v -> selectPage(PAGE_TIMELINE));
    }

    private void setupViewPagerCallback() {
        viewPager.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageSelected(int position) {
                updateToggleSelection(position);
            }
        });
    }

    private void selectPage(int page) {
        updateToggleSelection(page);
        viewPager.setCurrentItem(page, true);
    }

    private void updateToggleSelection(int position) {
        Context context = requireContext();
        recordToggle.setSelected(context, position == PAGE_RECORD);
        timelineToggle.setSelected(context, position == PAGE_TIMELINE);
    }

    private static class ToggleViewHolder {
        final LinearLayout container;
        final ImageView icon;
        final TextView text;

        ToggleViewHolder(View parent, int containerId, int iconId, int textId) {
            container = parent.findViewById(containerId);
            icon = parent.findViewById(iconId);
            text = parent.findViewById(textId);
        }

        void setSelected(Context context, boolean isSelected) {
            int textColor = ContextCompat.getColor(context, isSelected ? R.color.black : R.color.gray);
            int iconColor = textColor;

            container.setBackgroundResource(isSelected ? R.drawable.toggle_selected_background : android.R.color.transparent);
            text.setTextColor(textColor);
            icon.setColorFilter(iconColor);
        }
    }
}
