package mp.gradia.tutorial;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import androidx.core.content.ContextCompat;
import androidx.viewpager2.widget.ViewPager2;

import com.takusemba.spotlight.Spotlight;
import com.takusemba.spotlight.Target;
import com.takusemba.spotlight.shape.Circle;
import com.takusemba.spotlight.OnSpotlightListener;

import java.util.ArrayList;
import java.util.List;

import mp.gradia.R;
import mp.gradia.main.MainActivity;

public class TutorialManager {

    private static final String PREFS_NAME = "tutorial_prefs";
    private static final String KEY_SHOWN = "tutorial_shown";

    // 튜토리얼 표시 여부 확인
    public static boolean hasShownTutorial(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        return prefs.getBoolean(KEY_SHOWN, false);
    }

    // 튜토리얼 완료 표시
    public static void markTutorialShown(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        prefs.edit().putBoolean(KEY_SHOWN, true).apply();
    }

    // 튜토리얼 실행
    public static void showTutorial(Activity activity, View... views) {
        if (hasShownTutorial(activity)) return;

        List<Target> targets = new ArrayList<>();
        List<View> overlays = new ArrayList<>(); // 오버레이 뷰 저장용
        LayoutInflater inflater = LayoutInflater.from(activity);

        // 각 뷰에 대한 타겟 생성
        for (int i = 0; i < views.length; i++) {
            View view = views[i];
            View overlay = createOverlayView(activity, inflater, i);
            overlays.add(overlay);

            Target target = new Target.Builder()
                    .setAnchor(view)
                    .setShape(new Circle(130f))
                    .setOverlay(overlay)
                    .build();

            targets.add(target);
        }


        Spotlight spotlight = buildSpotlight(activity, targets);

        setupButtonListeners(activity, spotlight, overlays);

        spotlight.start();
    }


    private static View createOverlayView(Activity activity, LayoutInflater inflater, int index) {
        View overlay = inflater.inflate(R.layout.tutorial_overlay, null, false);
        TextView title = overlay.findViewById(R.id.tutorial_title);
        TextView desc = overlay.findViewById(R.id.tutorial_desc);
        title.setText(activity.getString(getTitleRes(index)));
        desc.setText(activity.getString(getDescRes(index)));

        return overlay;
    }


    private static Spotlight buildSpotlight(Activity activity, List<Target> targets) {
        return new Spotlight.Builder(activity)
                .setTargets(targets.toArray(new Target[0]))
                .setBackgroundColor(ContextCompat.getColor(activity, R.color.spotlightBackground))
                .setDuration(100L)
                .setAnimation(new android.view.animation.DecelerateInterpolator(2f))
                .setOnSpotlightListener(new OnSpotlightListener() {
                    @Override
                    public void onStarted() {}

                    @Override
                    public void onEnded() {
                        markTutorialShown(activity);
                    }
                })
                .build();
    }


    private static void setupButtonListeners(Activity activity, Spotlight spotlight, List<View> overlays) {
        ViewPager2 viewPager = activity.findViewById(R.id.view_pager);

        for (int i = 0; i < overlays.size(); i++) {
            Button btnNext = overlays.get(i).findViewById(R.id.btn_next);
            Button btnSkip = overlays.get(i).findViewById(R.id.btn_skip);

            if (i == (overlays.size() - 1))
                btnNext.setText("마침");

            final int currentIdx = i + 1;
            btnNext.setOnClickListener(v -> {
                spotlight.next();
                viewPager.setCurrentItem(currentIdx);
                if (currentIdx == overlays.size())
                    viewPager.setCurrentItem(0);
            });
            btnSkip.setOnClickListener(v -> spotlight.finish());

        }
    }

    // 다음 버튼
    private static void handleNextButton(Spotlight spotlight) {
        spotlight.next();

    }


    private static int getTitleRes(int index) {
        switch (index) {
            case 0: return R.string.tutorial_title_home;
            case 1: return R.string.tutorial_title_subject;
            case 2: return R.string.tutorial_title_record;
            case 3: return R.string.tutorial_title_analysis;
            default: return R.string.tutorial_title_default;
        }
    }


    private static int getDescRes(int index) {
        switch (index) {
            case 0: return R.string.tutorial_desc_home;
            case 1: return R.string.tutorial_desc_subject;
            case 2: return R.string.tutorial_desc_record;
            case 3: return R.string.tutorial_desc_analysis;
            default: return R.string.tutorial_desc_default;
        }
    }
}



