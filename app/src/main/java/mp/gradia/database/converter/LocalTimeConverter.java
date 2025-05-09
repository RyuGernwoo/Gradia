package mp.gradia.database.converter;

import androidx.room.TypeConverter;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

public class LocalTimeConverter {
    private static final DateTimeFormatter formatter = DateTimeFormatter.ISO_LOCAL_DATE;

    @TypeConverter
    public static LocalTime fromString(String value) {
        return value == null ? null : LocalTime.parse(value, formatter);
    }

    @TypeConverter
    public static String localDateToString(LocalTime date) {
        return date == null ? null : date.format(formatter);
    }
}
