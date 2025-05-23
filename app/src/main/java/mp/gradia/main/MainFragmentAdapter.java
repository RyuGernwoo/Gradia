package mp.gradia.main;

import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.viewpager2.adapter.FragmentStateAdapter;

import mp.gradia.analysis.AnalysisFragment;
import mp.gradia.home.HomeFragment;
import mp.gradia.subject.SubjectFragment;
import mp.gradia.time.TimeFragment;

// FragmentAdapter supplies fragments to ViewPager2 for swipeable screens
public class MainFragmentAdapter extends FragmentStateAdapter {
    Bundle bundle;
    public MainFragmentAdapter(@NonNull FragmentActivity activity) {
        super(activity);
    }
    public MainFragmentAdapter(@NonNull FragmentActivity activity, Bundle bundle) {
        super(activity);
        this.bundle = bundle;
    }

    @NonNull
    @Override
    public Fragment createFragment(int position) {
        // Return a new instance of the fragment based on position
        switch (position) {
            // 0: HomeFragment, 1: SubjectFragment, 2: TimeFragment, 3: AnalysisFragment
            case 0:
                return new HomeFragment();
            case 1:
                return new SubjectFragment();
            case 2:
                if (bundle != null) {
                    Fragment fragment = new TimeFragment();
                    fragment.setArguments(bundle);
                    return fragment;
                }
                return new TimeFragment();
            case 3:
                return new AnalysisFragment();
            default:
                throw new IllegalArgumentException("Invalid page index : " + position);
        }
    }

    @Override
    public int getItemCount() {
        return 4;
    }
}
