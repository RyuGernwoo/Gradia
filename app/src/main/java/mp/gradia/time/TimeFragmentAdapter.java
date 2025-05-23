package mp.gradia.time;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.viewpager2.adapter.FragmentStateAdapter;

import mp.gradia.time.inner.TimeRecordFragment;
import mp.gradia.time.inner.TimeTimelineFragment;

public class TimeFragmentAdapter extends FragmentStateAdapter {
    public TimeFragmentAdapter(@NonNull Fragment fragment) {
        super(fragment);
    }

    /**
     * ViewPager2의 각 위치에 해당하는 Fragment를 생성합니다.
     */
    @NonNull
    @Override
    public Fragment createFragment(int position) {
        return (position == 0) ? new TimeRecordFragment() : new TimeTimelineFragment();
    }

    /**
     * ViewPager2에서 표시할 전체 페이지 수를 반환합니다.
     */
    @Override
    public int getItemCount() {
        return 2;
    }
}