//package mp.gradia.subject.adapter;
//
//import android.view.LayoutInflater;
//import android.view.View;
//import android.view.ViewGroup;
//import android.widget.CheckBox;
//import android.widget.ImageButton;
//
//import androidx.annotation.NonNull;
//import androidx.recyclerview.widget.RecyclerView;
//
//import java.util.ArrayList;
//import java.util.List;
//
//
//public class TodoAdapter extends RecyclerView.Adapter<TodoAdapter.TodoViewHolder> {
//
//    // 할 일 항목의 동작 이벤트를 처리
//    public interface TodoActionListener {
//        void onCheckChanged(TodoEntity todo, boolean isChecked); // 체크 여부 변경
//        void onDeleteClicked(TodoEntity todo); // 삭제 버튼 클릭
//        void onItemClicked(TodoEntity todo);   // 항목 클릭
//    }
//
//    // 할 일 목록 데이터
//    private List<TodoEntity> todoList = new ArrayList<>();
//
//    // 액션 리스너 객체
//    private TodoActionListener listener;
//
//
//    public void setListener(TodoActionListener listener) {
//        this.listener = listener;
//    }
//
//    // 외부에서 할 일 목록을 전달받고 갱신
//    public void setTodos(List<TodoEntity> todos) {
//        this.todoList = todos;
//        notifyDataSetChanged(); // 데이터 변경 알림
//    }
//
//    // 아이템 뷰 생성
//    @NonNull
//    @Override
//    public TodoViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
//        View view = LayoutInflater.from(parent.getContext())
//                .inflate(R.layout.item_todo, parent, false); // item_todo.xml 레이아웃 사용
//        return new TodoViewHolder(view);
//    }
//
//    // 뷰홀더에 데이터 바인딩
//    @Override
//    public void onBindViewHolder(@NonNull TodoViewHolder holder, int position) {
//        TodoEntity todo = todoList.get(position);
//
//        // 체크박스 상태 설정
//        holder.checkBox.setText(todo.content);
//        holder.checkBox.setChecked(todo.isDone);
//
//        // 체크 상태 변경 시 호출
//        holder.checkBox.setOnCheckedChangeListener((buttonView, isChecked) -> {
//            todo.isDone = isChecked;
//            if (listener != null) listener.onCheckChanged(todo, isChecked);
//        });
//
//        // 삭제 버튼 클릭 이벤트
//        holder.deleteButton.setOnClickListener(v -> {
//            if (listener != null) listener.onDeleteClicked(todo);
//        });
//
//        // 체크박스 전체 클릭 처리
//        holder.checkBox.setOnClickListener(v -> {
//            if (listener != null) {
//                listener.onItemClicked(todo);
//            }
//        });
//    }
//
//    // 아이템 개수 반환
//    @Override
//    public int getItemCount() {
//        return todoList.size();
//    }
//
//    // 각각의 할 일 항목을 표시
//    static class TodoViewHolder extends RecyclerView.ViewHolder {
//        CheckBox checkBox;        // 할 일 체크박스
//        ImageButton deleteButton; // 삭제 버튼
//
//        public TodoViewHolder(@NonNull View itemView) {
//            super(itemView);
//            checkBox = itemView.findViewById(R.id.checkTodo);           // 체크박스 연결
//            deleteButton = itemView.findViewById(R.id.buttonDeleteTodo); // 삭제버튼 연결
//        }
//    }
//}
