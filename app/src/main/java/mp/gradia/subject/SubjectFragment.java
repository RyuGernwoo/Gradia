package mp.gradia.subject;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import mp.gradia.R;
import mp.gradia.subject.repository.SubjectRepository;
import mp.gradia.subject.viewmodel.SubjectViewModel;

public class SubjectFragment extends Fragment {
        private static final String TAG = "SubjectFragment";
        private SubjectViewModel viewModel;

        @Override
        public View onCreateView(@NonNull LayoutInflater inflater,
                        @Nullable ViewGroup container,
                        @Nullable Bundle savedInstanceState) {
                return inflater.inflate(R.layout.fragment_subject, container, false);
        }

        @Override
        public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
                super.onViewCreated(view, savedInstanceState);

                viewModel = new ViewModelProvider(this).get(SubjectViewModel.class);

                // Fragment 생성 시 로컬 DB 기준으로 클라우드 동기화 수행
                performInitialSync();
        }

        @Override
        public void onDestroyView() {
                super.onDestroyView();
                performInitialSync();
        }

        /**
         * 초기 동기화 수행 - 로컬 DB 기준으로 클라우드를 업데이트
         */
        private void performInitialSync() {
                Log.d(TAG, "초기 과목 동기화 시작");

                viewModel.syncLocalToCloud(new SubjectRepository.CloudSyncCallback() {
                        @Override
                        public void onSuccess() {
                                if (getActivity() != null) {
                                        Log.d(TAG, "과목 동기화 완료");
                                }
                        }

                        @Override
                        public void onError(String message) {
                                if (getActivity() != null) {
                                        Log.w(TAG, "과목 동기화 실패: " + message);
                                }
                        }
                });
        }
}
