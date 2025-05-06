package mp.gradia.subject;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.disposables.CompositeDisposable;
import io.reactivex.rxjava3.schedulers.Schedulers;
import mp.gradia.database.AppDatabase;
import mp.gradia.database.dao.SubjectDao;
import mp.gradia.database.entity.SubjectEntity;
import mp.gradia.main.MainActivity;

import mp.gradia.R;

/* (임시) */
public class SubjectFragment extends Fragment {
    private AppDatabase db;
    private CompositeDisposable disposable = new CompositeDisposable();

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_subject, container, false);
        db = AppDatabase.getInstance(requireContext());
        SubjectDao dao = db.subjectDao();

        Button addSubjectBtn = view.findViewById(R.id.btn_add_subject);
        addSubjectBtn.setOnClickListener(v -> {
            SubjectEntity newSubject1 = new SubjectEntity("모바일 프로그래밍", 3, R.color.c1, SubjectEntity.REQUIRED_SUBJECT, null, null, null, null);
            SubjectEntity newSubject2 = new SubjectEntity("인공지능 개론", 3, R.color.c2, SubjectEntity.REQUIRED_SUBJECT, null, null, null, null);
            SubjectEntity newSubject3 = new SubjectEntity("AI 수학", 3, R.color.c3, SubjectEntity.REQUIRED_SUBJECT, null, null, null, null);
            SubjectEntity newSubject4 = new SubjectEntity("데이터 과학", 3, R.color.c4, SubjectEntity.REQUIRED_SUBJECT, null, null, null, null);
            SubjectEntity newSubject5 = new SubjectEntity("디지털 마케팅", 2, R.color.c5, SubjectEntity.REQUIRED_SUBJECT, null, null, null, null);
            SubjectEntity newSubject6 = new SubjectEntity("운영체제", 3, R.color.c6, SubjectEntity.REQUIRED_SUBJECT, null, null, null, null);

           disposable.add(
                   dao.insert(newSubject1, newSubject2, newSubject3, newSubject4, newSubject5, newSubject6)
                           .subscribeOn(Schedulers.io())
                           .observeOn(AndroidSchedulers.mainThread())
                           .subscribe(
                                   () -> {
                                       Log.i("ViewModel/Repo", "Initial subjects inserted successfully.");
                                   },
                                   // onError: 삽입 중 오류 발생 시 호출됨
                                   throwable -> {
                                       Log.e("ViewModel/Repo", "Error inserting initial subjects", throwable);
                                   }
                           )
           );
            //((MainActivity) requireActivity()).addSubject(newSubject);
        });

        return view;
    }
}
