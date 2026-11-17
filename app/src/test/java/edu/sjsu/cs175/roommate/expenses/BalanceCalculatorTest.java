package edu.sjsu.cs175.roommate.expenses;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class BalanceCalculatorTest {

    @Test public void equalSplitAddsUpToTotal() {
        Map<String, Long> shares = BalanceCalculator.splitEqually(1000, Arrays.asList("a", "b", "c"));
        assertEquals(334L, (long) shares.get("a"));
        assertEquals(333L, (long) shares.get("b"));
        assertEquals(333L, (long) shares.get("c"));
    }

    @Test public void oneBillSplitThreeWays() {
        Map<String, Long> net = new HashMap<>();
        BalanceCalculator.addExpense(net, "a", 9000,
                BalanceCalculator.splitEqually(9000, Arrays.asList("a", "b", "c")));

        List<BalanceCalculator.Transfer> transfers = BalanceCalculator.settle(net);
        assertEquals(2, transfers.size());
        for (BalanceCalculator.Transfer t : transfers) {
            assertEquals("a", t.to);
            assertEquals(3000L, t.cents);
        }
    }

    @Test public void billsCancelOut() {
        Map<String, Long> net = new HashMap<>();
        Map<String, Long> half = BalanceCalculator.splitEqually(2000, Arrays.asList("a", "b"));
        BalanceCalculator.addExpense(net, "a", 2000, half);
        BalanceCalculator.addExpense(net, "b", 2000, half);
        assertEquals(0, BalanceCalculator.settle(net).size());
    }

    @Test public void customSplitIsRespected() {
        Map<String, Long> shares = new HashMap<>();
        shares.put("a", 1000L);
        shares.put("b", 4000L);
        Map<String, Long> net = new HashMap<>();
        BalanceCalculator.addExpense(net, "a", 5000, shares);

        List<BalanceCalculator.Transfer> transfers = BalanceCalculator.settle(net);
        assertEquals(1, transfers.size());
        assertEquals("b", transfers.get(0).from);
        assertEquals("a", transfers.get(0).to);
        assertEquals(4000L, transfers.get(0).cents);
    }

    @Test public void convertsDollarsAndCents() {
        assertEquals(1999L, BalanceCalculator.toCents(19.99));
        assertEquals(19.99, BalanceCalculator.toDollars(1999), 0.0001);
    }
}
