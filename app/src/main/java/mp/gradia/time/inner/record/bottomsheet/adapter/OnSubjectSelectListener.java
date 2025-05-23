package mp.gradia.time.inner.record.bottomsheet.adapter;
import mp.gradia.database.entity.SubjectEntity;

// Bottom Sheet에서 과목이 선택되었을 때 콜백 정의
public interface OnSubjectSelectListener {
    /**
     * Bottom Sheet에서 과목 아이템이 클릭되었을 때 호출됩니다.
     */
    void onBottomSheetItemClick(SubjectEntity item);
}