package edu.sjsu.cs175.roommate.expenses;

import android.text.format.DateFormat;
import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import edu.sjsu.cs175.roommate.R;
import edu.sjsu.cs175.roommate.data.model.Expense;
import edu.sjsu.cs175.roommate.databinding.ItemExpenseBinding;

public class ExpenseAdapter extends RecyclerView.Adapter<ExpenseAdapter.Holder> {

    /** An expense plus its Firestore document ID. */
    public static final class Row {
        public final String id;
        public final Expense expense;

        public Row(String id, Expense expense) {
            this.id = id;
            this.expense = expense;
        }
    }

    public interface OnLongPress {
        void onLongPress(Row row);
    }

    private static final NumberFormat MONEY = NumberFormat.getCurrencyInstance(Locale.US);

    private final List<Row> rows = new ArrayList<>();
    private final OnLongPress onLongPress;
    private Map<String, String> names;
    private String currentUid;

    public ExpenseAdapter(OnLongPress onLongPress) {
        this.onLongPress = onLongPress;
    }

    public void submit(List<Row> newRows, Map<String, String> names, String currentUid) {
        rows.clear();
        rows.addAll(newRows);
        this.names = names;
        this.currentUid = currentUid;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public Holder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new Holder(ItemExpenseBinding.inflate(
                LayoutInflater.from(parent.getContext()), parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull Holder holder, int position) {
        Row row = rows.get(position);
        Expense e = row.expense;
        ItemExpenseBinding b = holder.binding;

        b.title.setText(e.getTitle());
        b.amount.setText(MONEY.format(e.getAmount() == null ? 0 : e.getAmount()));

        String payer = e.getPaidBy() != null && e.getPaidBy().equals(currentUid)
                ? b.getRoot().getContext().getString(R.string.you)
                : nameOf(e.getPaidBy());
        String date = e.getCreatedAt() == null ? ""
                : DateFormat.format("MMM d", e.getCreatedAt().toDate()).toString();
        String split = "custom".equals(e.getSplitType())
                ? b.getRoot().getContext().getString(R.string.split_custom)
                : b.getRoot().getContext().getString(R.string.split_equal);
        b.subtitle.setText(b.getRoot().getContext()
                .getString(R.string.expense_subtitle, payer, date, split));

        b.getRoot().setOnLongClickListener(v -> {
            onLongPress.onLongPress(row);
            return true;
        });
    }

    private String nameOf(String uid) {
        String name = names == null ? null : names.get(uid);
        return name == null ? "Former member" : name;
    }

    @Override
    public int getItemCount() {
        return rows.size();
    }

    static final class Holder extends RecyclerView.ViewHolder {
        final ItemExpenseBinding binding;

        Holder(ItemExpenseBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }
    }
}
