package edu.sjsu.cs175.roommate.chores;

import java.util.List;
import java.util.Calendar;
import java.util.TimeZone;

/** Assignments use calendar dates, avoiding overtime errors at daylight saving changes. */
public final class WeeklyRotation {
    private WeeklyRotation() { }

    public static String assignee(List<String> members, long startMillis, long dateMillis, int offset) {
        if (members == null || members.isEmpty()) return null;
        Calendar start = utcDay(startMillis);
        Calendar date = utcDay(dateMillis);
        long days = (date.getTimeInMillis() - start.getTimeInMillis()) / 86400000L;
        if (days < 0) return null;
        int index = (int) ((days / 7 + Math.floorMod(offset, members.size())) % members.size());
        return members.get(index);
    }

    private static Calendar utcDay(long millis) {
        Calendar local = Calendar.getInstance();
        local.setTimeInMillis(millis);
        Calendar day = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
        day.clear();
        day.set(local.get(Calendar.YEAR), local.get(Calendar.MONTH), local.get(Calendar.DAY_OF_MONTH));
        return day;
    }
}
