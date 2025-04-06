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

    @NonNull
    @Override
    public Fragment createFragment(int position) {
        return (position == 0) ? new TimeRecordFragment() : new TimeTimelineFragment();
    }

    @Override
    public int getItemCount() {
        return 2;
    }
}
