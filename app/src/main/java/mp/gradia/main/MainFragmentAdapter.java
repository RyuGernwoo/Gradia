package mp.gradia.main;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.viewpager2.adapter.FragmentStateAdapter;

import mp.gradia.analysis.AnalysisFragment;
import mp.gradia.home.HomeFragment;
import mp.gradia.subject.SubjectFragment;
import mp.gradia.time.TimeFragment;

public class MainFragmentAdapter extends FragmentStateAdapter {
    public MainFragmentAdapter(@NonNull FragmentActivity activity) {
        super(activity);
    }

    @NonNull
    @Override
    public Fragment createFragment(int position) {
        switch (position) {
            case 0: return new HomeFragment();
            case 1: return new SubjectFragment();
            case 2: return new TimeFragment();
            case 3: return new AnalysisFragment();
            default: return new HomeFragment();
        }
    }

    @Override
    public int getItemCount() {
        return 4;
    }
}
