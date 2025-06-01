package mp.gradia.subject.ui;

import android.app.ActionBar;
import android.app.AlertDialog;
import android.app.Dialog;
import android.os.Bundle;
import android.text.InputType;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.LinearLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.textfield.TextInputEditText;

public class TodoInputDialog extends DialogFragment {

    public interface OnTodoEnteredListener {
        void onTodoEntered(String content);
    }

    private OnTodoEnteredListener listener;
    private String defaultText = "";

    public void setListener(OnTodoEnteredListener listener) {
        this.listener = listener;
    }

    public void setDefaultText(String text) {
        this.defaultText = text;
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        int margin = 15;
        float scale = getResources().getDisplayMetrics().density;
        int marginPx = (int) (margin * scale + 0.5f);
        LinearLayout container = new LinearLayout(requireContext());
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        params.setMargins(marginPx, marginPx / 2, marginPx, marginPx / 2);

        TextInputEditText input = new TextInputEditText(requireContext());
        input.setHint("할 일");
        input.setInputType(InputType.TYPE_CLASS_TEXT);
        input.setText(defaultText);
        input.setSelection(defaultText.length());
        input.setLayoutParams(params);
        container.addView(input);

        return new MaterialAlertDialogBuilder(requireContext())
                .setTitle(defaultText.isEmpty() ? "할 일 추가" : "할 일 수정")
                .setView(container)
                .setPositiveButton("확인", (dialog, which) -> {
                    String content = input.getText().toString().trim();
                    if (!content.isEmpty() && listener != null) {
                        listener.onTodoEntered(content);
                    }
                })
                .setNegativeButton("취소", null)
                .create();
    }
}
