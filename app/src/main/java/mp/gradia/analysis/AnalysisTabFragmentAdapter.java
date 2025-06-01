package mp.gradia.analysis;

import androidx.fragment.app.Fragment;
import androidx.viewpager2.adapter.FragmentStateAdapter;

public class AnalysisTabFragmentAdapter extends FragmentStateAdapter {
    private final int tabCount;

    public AnalysisTabFragmentAdapter(Fragment fragment, int tabCount) {
        super(fragment);
        this.tabCount = tabCount;
    }

    @Override
    public Fragment createFragment(int position) {
        switch (position) {
            case 0:
                return new GradePredictionFragment();
            case 1:
                return new BarChartFragment();
            case 2:
                return new BarChartByDayFragment();
            default:
                return null;
        }
    }

    @Override
    public int getItemCount() {
        return tabCount;
    }
}
