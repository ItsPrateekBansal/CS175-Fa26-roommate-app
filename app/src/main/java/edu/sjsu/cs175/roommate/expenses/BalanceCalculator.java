package edu.sjsu.cs175.roommate.expenses;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Money math for expense splitting. All amounts are in cents to avoid
 * rounding errors with doubles. No Android code, so it can be unit tested.
 */
public final class BalanceCalculator {

    private BalanceCalculator() { }

    /** One payment one person should make to settle up. */
    public static final class Transfer {
        public final String from;
        public final String to;
        public final long cents;

        Transfer(String from, String to, long cents) {
            this.from = from;
            this.to = to;
            this.cents = cents;
        }
    }

    public static long toCents(double dollars) {
        return Math.round(dollars * 100);
    }

    public static double toDollars(long cents) {
        return cents / 100.0;
    }

    /**
     * Splits a total equally. Leftover cents go to the first members,
     * so the shares always add up to the total.
     */
    public static Map<String, Long> splitEqually(long totalCents, List<String> memberIds) {
        Map<String, Long> shares = new HashMap<>();
        if (memberIds.isEmpty()) return shares;
        long base = totalCents / memberIds.size();
        long remainder = totalCents % memberIds.size();
        for (int i = 0; i < memberIds.size(); i++) {
            shares.put(memberIds.get(i), base + (i < remainder ? 1 : 0));
        }
        return shares;
    }

    /**
     * Adds one expense to the running net balances.
     * Positive balance = others owe this person. Negative = this person owes.
     */
    public static void addExpense(Map<String, Long> net, String paidBy,
                                  long amountCents, Map<String, Long> sharesCents) {
        net.put(paidBy, net.getOrDefault(paidBy, 0L) + amountCents);
        for (Map.Entry<String, Long> share : sharesCents.entrySet()) {
            String member = share.getKey();
            net.put(member, net.getOrDefault(member, 0L) - share.getValue());
        }
    }

    /**
     * Turns net balances into a short list of payments.
     * Always pairs the biggest debtor with the biggest creditor.
     */
    public static List<Transfer> settle(Map<String, Long> net) {
        List<Map.Entry<String, Long>> creditors = new ArrayList<>();
        List<Map.Entry<String, Long>> debtors = new ArrayList<>();
        for (Map.Entry<String, Long> e : net.entrySet()) {
            if (e.getValue() > 0) creditors.add(new HashMap.SimpleEntry<>(e.getKey(), e.getValue()));
            if (e.getValue() < 0) debtors.add(new HashMap.SimpleEntry<>(e.getKey(), -e.getValue()));
        }

        List<Transfer> transfers = new ArrayList<>();
        while (!creditors.isEmpty() && !debtors.isEmpty()) {
            creditors.sort((a, b) -> Long.compare(b.getValue(), a.getValue()));
            debtors.sort((a, b) -> Long.compare(b.getValue(), a.getValue()));
            Map.Entry<String, Long> creditor = creditors.get(0);
            Map.Entry<String, Long> debtor = debtors.get(0);

            long amount = Math.min(creditor.getValue(), debtor.getValue());
            transfers.add(new Transfer(debtor.getKey(), creditor.getKey(), amount));

            creditor.setValue(creditor.getValue() - amount);
            debtor.setValue(debtor.getValue() - amount);
            if (creditor.getValue() == 0) creditors.remove(0);
            if (debtor.getValue() == 0) debtors.remove(0);
        }
        return transfers;
    }
}
