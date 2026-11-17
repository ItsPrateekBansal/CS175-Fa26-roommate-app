package edu.sjsu.cs175.roommate.matching;

import static org.junit.Assert.assertEquals;

import org.junit.Test;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

public class MatchEngineTest {
    private Map<String, Object> profile(Object... values) {
        Map<String, Object> profile = new HashMap<>();
        for (int i = 0; i < values.length; i += 2) profile.put((String) values[i], values[i + 1]);
        return profile;
    }

    @Test public void comparesPreferencesAndIgnoresMissingFields() {
        MatchEngine.Result result = MatchEngine.compare("other",
                profile("budget", 1000, "sleepTime", "Early", "smoking", false, "pets", true),
                profile("budget", 1200, "sleepTime", " early ", "smoking", true));
        assertEquals(3, result.compared);
        assertEquals(2, result.matched);
        assertEquals(66, result.percentage());
    }

    @Test public void budgetBoundaryAndInvalidBudget() {
        assertEquals(1, MatchEngine.compare("a", profile("budget", 80), profile("budget", 100)).matched);
        assertEquals(0, MatchEngine.compare("b", profile("budget", 79), profile("budget", 100)).matched);
        assertEquals(0, MatchEngine.compare("c", profile("budget", -1), profile("budget", 100)).compared);
    }

    @Test public void sortsByScoreThenCoverageThenName() {
        Map<String, Object> me = profile("smoking", false, "pets", true);
        MatchEngine.Result low = MatchEngine.compare("1", me, profile("displayName", "A", "smoking", true));
        MatchEngine.Result one = MatchEngine.compare("2", me, profile("displayName", "B", "smoking", false));
        MatchEngine.Result two = MatchEngine.compare("3", me, profile("displayName", "C", "smoking", false, "pets", true));
        ArrayList<MatchEngine.Result> results = new ArrayList<>(Arrays.asList(low, one, two));
        MatchEngine.sort(results);
        assertEquals("3", results.get(0).id);
        assertEquals("2", results.get(1).id);
        assertEquals("1", results.get(2).id);
    }
}
