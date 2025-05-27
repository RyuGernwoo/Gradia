package mp.gradia.utils;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.stream.Collectors;

import mp.gradia.api.models.TimetableItem;
import mp.gradia.database.entity.EvaluationRatio;
import mp.gradia.database.entity.SubjectEntity;
import mp.gradia.database.entity.TargetStudyTime;

public class SubjectUtil {
    public static List<SubjectEntity> convertToEntityList(List<TimetableItem> timetableItems) {
        Map<String, Integer> credits = new HashMap<>();
        for (TimetableItem item : timetableItems) {
            String subjectName = item.getName();
            // ex) 16:00
            String startTime = item.getStart_time();
            // ex) 17:00
            String endTime = item.getEnd_time();

            int startHour = Integer.parseInt(startTime.split(":")[0]);
            int startMinute = Integer.parseInt(startTime.split(":")[1]);
            int endHour = Integer.parseInt(endTime.split(":")[0]);
            int endMinute = Integer.parseInt(endTime.split(":")[1]);

            // 60분 마다 1학점으로 계산
            int credit = (endHour * 60 + endMinute - startHour * 60 - startMinute) / 60;

            if (credits.containsKey(subjectName)) {
                credits.put(subjectName, credits.get(subjectName) + credit);
            } else {
                credits.put(subjectName, credit);
            }
        }

        return credits.entrySet().stream()
                .map(entry -> new SubjectEntity(
                        entry.getKey(), // 과목명
                        entry.getValue(), // 학점
                        generateRandomHexColor(),
                        0,
                        "",
                        "",
                        new EvaluationRatio(),
                        new TargetStudyTime(0, 0, 0)
                )).collect(Collectors.toList());
    }

    public static String generateRandomHexColor() {
        Random random = new Random();
        int red = random.nextInt(151) + 50;   // 50-200
        int green = random.nextInt(151) + 50; // 50-200
        int blue = random.nextInt(151) + 50;  // 50-200
        return String.format("#%02x%02x%02x", red, green, blue);
    }
}
