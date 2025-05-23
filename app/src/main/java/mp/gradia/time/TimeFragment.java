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
    // Page Constants
    private static final int PAGE_RECORD = 0;
    private static final int PAGE_TIMELINE = 1;

    // UI Components
    private ViewPager2 viewPager;
    private ToggleViewHolder recordToggle;
    private ToggleViewHolder timelineToggle;

    /**
     * 프래그먼트의 UI를 생성하고 초기화합니다.
     */
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_time_main, container, false);

        initViewPager(v);
        initToggles(v);
        setupToggleListeners();
        setupViewPagerCallback();

        return v;
    }

    /**
     * ViewPager2를 초기화하고 어댑터를 설정합니다.
     */
    private void initViewPager(View v) {
        viewPager = v.findViewById(R.id.view_pager);
        viewPager.setAdapter(new TimeFragmentAdapter(this));
    }

    /**
     * 하단 탭의 토글 버튼(Record, Timeline)을 초기화합니다.
     */
    private void initToggles(View v) {
        recordToggle = new ToggleViewHolder(v, R.id.record_toggle, R.id.record_icon, R.id.record_text);
        timelineToggle = new ToggleViewHolder(v, R.id.timeline_toggle, R.id.timeline_icon, R.id.timeline_text);
    }

    /**
     * 토글 버튼 클릭 리스너를 설정하여 페이지 전환 기능을 구현합니다.
     */
    private void setupToggleListeners() {
        recordToggle.container.setOnClickListener(v -> selectPage(PAGE_RECORD));
        timelineToggle.container.setOnClickListener(v -> selectPage(PAGE_TIMELINE));
    }

    /**
     * ViewPager2의 페이지 변경 콜백을 설정하여 토글 버튼의 상태를 업데이트합니다.
     */
    private void setupViewPagerCallback() {
        viewPager.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageSelected(int position) {
                updateToggleSelection(position);
            }
        });
    }

    /**
     * 지정된 페이지로 ViewPager2를 전환하고 토글 버튼의 선택 상태를 업데이트합니다.
     */
    private void selectPage(int page) {
        updateToggleSelection(page);
        viewPager.setCurrentItem(page, true);
    }

    /**
     * 현재 선택된 페이지에 따라 토글 버튼의 UI 상태를 업데이트합니다.
     */
    private void updateToggleSelection(int position) {
        Context context = requireContext();
        recordToggle.setSelected(context, position == PAGE_RECORD);
        timelineToggle.setSelected(context, position == PAGE_TIMELINE);
    }

    /**
     * 하단 탭 토글 버튼의 뷰 요소들을 관리하는 ViewHolder 클래스입니다.
     */
    private static class ToggleViewHolder {
        final LinearLayout container;
        final ImageView icon;
        final TextView text;

        ToggleViewHolder(View parent, int containerId, int iconId, int textId) {
            container = parent.findViewById(containerId);
            icon = parent.findViewById(iconId);
            text = parent.findViewById(textId);
        }

        /**
         * 토글 버튼의 선택 상태에 따라 배경, 텍스트 색상, 아이콘 색상을 변경합니다.
         */
        void setSelected(Context context, boolean isSelected) {
            int textColor = ContextCompat.getColor(context, isSelected ? R.color.black : R.color.gray);
            int iconColor = textColor;

            container.setBackgroundResource(isSelected ? R.drawable.toggle_selected_background : android.R.color.transparent);
            text.setTextColor(textColor);
            icon.setColorFilter(iconColor);
        }
    }
}