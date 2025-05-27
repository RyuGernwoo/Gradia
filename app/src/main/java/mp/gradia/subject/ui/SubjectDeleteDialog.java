package mp.gradia.subject.ui;

import android.app.Dialog;
import android.os.Bundle;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.DialogFragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.Navigation;

import mp.gradia.R;
import mp.gradia.database.repository.SubjectRepository;
import mp.gradia.subject.viewmodel.SubjectViewModel;

public class SubjectDeleteDialog extends DialogFragment {

    private SubjectViewModel viewModel;
    private int subjectId;

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        subjectId = getArguments().getInt("subjectId", -1);
        viewModel = new ViewModelProvider(requireActivity()).get(SubjectViewModel.class);

        return new AlertDialog.Builder(requireContext())
                .setTitle("정말 삭제하시겠습니까?")
                .setNegativeButton("취소", (dialog, which) -> dismiss())
                .setPositiveButton("삭제", (dialog, which) -> {
                    viewModel.getSubjectById(subjectId).observe(this, subject -> {
                        if (subject != null) {
                            // 클라우드 동기화 콜백 생성
                            SubjectRepository.CloudSyncCallback callback = new SubjectRepository.CloudSyncCallback() {
                                @Override
                                public void onSuccess() {
                                    if (getActivity() != null) {
                                        Toast.makeText(getActivity(),
                                                "과목이 성공적으로 삭제되었습니다.",
                                                Toast.LENGTH_SHORT).show();

                                        // 삭제 후 과목 리스트로 명확하게 이동
                                        Navigation.findNavController(requireActivity(), R.id.nav_host_fragment)
                                                .navigate(R.id.action_subjectDeleteDialog_to_subjectList);
                                    }
                                }

                                @Override
                                public void onError(String message) {
                                    if (getActivity() != null) {
                                        Toast.makeText(getActivity(),
                                                "삭제 중 오류가 발생했습니다: " + message,
                                                Toast.LENGTH_LONG).show();
                                    }
                                }
                            };
                            viewModel.delete(subject, callback);
                        }
                    });
                })
                .create();
    }
}
