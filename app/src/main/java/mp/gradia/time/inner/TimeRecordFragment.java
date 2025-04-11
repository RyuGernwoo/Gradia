package mp.gradia.time.inner;

import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.cardview.widget.CardView;
import androidx.fragment.app.Fragment;

import java.util.Random;

import mp.gradia.R;
import mp.gradia.time.inner.bottomsheet.Subject;
import mp.gradia.time.inner.bottomsheet.adapter.OnSubjectSelectListener;
import mp.gradia.time.inner.bottomsheet.adapter.SubjectSelectBottomSheetFragment;

public class TimeRecordFragment extends Fragment implements OnSubjectSelectListener {
    private TextView greetingTextView;
    private CardView subjectSelectBtn;
    private ImageView cShape;
    private TextView selectedSubjectTextView;
    private SubjectSelectBottomSheetFragment modalBottomSheet;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_time_record, container, false);

        // Show Random Greeting Messages
        greetingTextView = v.findViewById(R.id.greeting_textview);
        setRandomGreetingMessage();

        // Show Modal Bottom Sheet
        subjectSelectBtn = v.findViewById(R.id.subject_select_btn);
        subjectSelectBtn.setOnClickListener(view -> {
            showBottomSheet();
        });


        selectedSubjectTextView = v.findViewById(R.id.selected_subject_textview);
        cShape = v.findViewById(R.id.color_circle);

        return v;
    }

    /* CONCERN ABOUT PERFOMANCE DEGRADATION */
    // When user return to Fragment change Greeting Message
    @Override
    public void onResume() {
        super.onResume();
        setRandomGreetingMessage();
    }

    // Set a random greeting message on each fragment resume
    private void setRandomGreetingMessage() {
        String[] messages = getResources().getStringArray(R.array.text_label_study_greeting_messages);
        int randomIdx = new Random().nextInt(messages.length - 1);
        greetingTextView.setText(messages[randomIdx]);
    }

    // Display modal bottom sheet to select subject
    private void showBottomSheet() {
        if (modalBottomSheet == null) {
            modalBottomSheet = new SubjectSelectBottomSheetFragment();
            modalBottomSheet.setOnBottomSheetItemClickListener(this);
        }
        modalBottomSheet.show(getChildFragmentManager(), modalBottomSheet.TAG);
    }

    // Handle selected subject and update UI (text and color circle)
    @Override
    public void onBottomSheetItemClick(Subject item) {
        modalBottomSheet.dismiss();

        selectedSubjectTextView.setText(item.getSubjectName());
        GradientDrawable drawable = (GradientDrawable) getContext().getResources().getDrawable(R.drawable.color_circle);
        drawable.setColor(getContext().getResources().getColor(item.getColor()));
        cShape.setImageDrawable(drawable);
    }
}