package mp.gradia.utils;

import mp.gradia.api.models.Todo;
import mp.gradia.database.entity.TodoEntity;

public class TodoUtil {
    public static TodoEntity convertServerToLocalTodo(Todo apiTodo, int subjectId) {
        TodoEntity todoEntity = new TodoEntity();
        todoEntity.subjectId = subjectId;
        todoEntity.content = apiTodo.getContent();
        todoEntity.isDone = apiTodo.isDone();
        return todoEntity;
    }
}
