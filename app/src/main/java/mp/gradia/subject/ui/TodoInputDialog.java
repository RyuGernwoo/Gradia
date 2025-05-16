package mp.gradia.subject.ui;

import android.app.AlertDialog;
import android.app.Dialog;
import android.os.Bundle;
import android.text.InputType;
import android.widget.EditText;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;

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
        EditText input = new EditText(requireContext());
        input.setInputType(InputType.TYPE_CLASS_TEXT);
        input.setText(defaultText);
        input.setSelection(defaultText.length());

        return new AlertDialog.Builder(requireContext())
                .setTitle(defaultText.isEmpty() ? "할 일 추가" : "할 일 수정")
                .setView(input)
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
