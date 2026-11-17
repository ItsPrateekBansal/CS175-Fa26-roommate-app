package edu.sjsu.cs175.roommate.matching;

import java.util.Comparator;
import java.util.List;
import java.util.Map;

/** Compares fields available on both profiles. */
public final class MatchEngine {
    private MatchEngine() { }
    public static final class Result {
        public final String id, name;
        public final int matched, compared;
        Result(String id, String name, int matched, int compared) {
            this.id = id; this.name = name; this.matched = matched; this.compared = compared;
        }
        public int percentage() { return compared == 0 ? 0 : matched * 100 / compared; }
    }
    public static Result compare(String id, Map<String, Object> me, Map<String, Object> other) {
        int matched = 0, compared = 0;
        Object a = me.get("budget"), b = other.get("budget");
        if (a instanceof Number && b instanceof Number) {
            double x = ((Number) a).doubleValue(), y = ((Number) b).doubleValue();
            if (x >= 0 && y >= 0) {
                compared++;
                if (Math.abs(x - y) <= Math.max(x, y) * 0.2) matched++;
            }
        }
        for (String field : new String[]{"sleepTime", "cleanliness", "smoking", "pets"}) {
            a = me.get(field); b = other.get(field);
            if (a == null || b == null || a.toString().trim().isEmpty() || b.toString().trim().isEmpty()) continue;
            compared++;
            if (a.toString().trim().equalsIgnoreCase(b.toString().trim())) matched++;
        }
        Object name = other.get("displayName");
        return new Result(id, name instanceof String && !((String) name).trim().isEmpty() ? ((String) name).trim() : "Unnamed user", matched, compared);
    }
    public static void sort(List<Result> results) {
        results.sort(Comparator.comparingInt(Result::percentage).reversed()
                .thenComparing(Comparator.comparingInt((Result r) -> r.compared).reversed())
                .thenComparing(r -> r.name, String.CASE_INSENSITIVE_ORDER).thenComparing(r -> r.id));
    }
}
