package edu.sjsu.cs175.roommate.chores;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import org.junit.Test;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;

public class WeeklyRotationTest {
    @Test public void rotatesAndWraps() {
        Calendar day = Calendar.getInstance();
        day.set(2026, Calendar.MARCH, 7, 12, 0, 0);
        long start = day.getTimeInMillis();
        assertEquals("A", WeeklyRotation.assignee(Arrays.asList("A", "B"), start, start, 0));
        day.add(Calendar.DAY_OF_MONTH, 7);
        assertEquals("B", WeeklyRotation.assignee(Arrays.asList("A", "B"), start, day.getTimeInMillis(), 0));
        day.add(Calendar.DAY_OF_MONTH, 7);
        assertEquals("A", WeeklyRotation.assignee(Arrays.asList("A", "B"), start, day.getTimeInMillis(), 0));
        assertEquals("B", WeeklyRotation.assignee(Arrays.asList("A", "B"), start, start, 1));
        assertNull(WeeklyRotation.assignee(Collections.emptyList(), start, start, 0));
        day.add(Calendar.DAY_OF_MONTH, -21);
        assertNull(WeeklyRotation.assignee(Arrays.asList("A"), start, day.getTimeInMillis(), 0));
    }
}
