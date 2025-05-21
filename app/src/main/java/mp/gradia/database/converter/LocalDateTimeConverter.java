package mp.gradia.database.converter;

import androidx.room.TypeConverter;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class LocalDateTimeConverter {
    private static final DateTimeFormatter formatter = DateTimeFormatter.ISO_DATE_TIME;

    @TypeConverter
    public static LocalDateTime fromString(String value) {
        return value == null ? null : LocalDateTime.parse(value, formatter);
    }

    @TypeConverter
    public static String localDateTimeToString(LocalDateTime dateTime) {
        return dateTime == null ? null : dateTime.format(formatter);
    }
}