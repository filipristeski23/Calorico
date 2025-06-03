package com.example.calorico;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.example.calorico.room.Day;
import java.util.List;

public class DayAdapter extends RecyclerView.Adapter<DayAdapter.DayViewHolder> {
    private List<Day> dayList;
    private OnItemClickListener listener;
    private Context context;

    public interface OnItemClickListener {
        void onItemClick(Day day);
    }

    public void setOnItemClickListener(OnItemClickListener listener) {
        this.listener = listener;
    }

    public DayAdapter(Context context, List<Day> dayList) {
        this.context = context;
        this.dayList = dayList;
    }

    @NonNull
    @Override
    public DayViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.day_item, parent, false);
        return new DayViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull DayViewHolder holder, int position) {
        Day day = dayList.get(position);
        holder.tvDate.setText(day.getDate());

        String totalsText = context.getString(R.string.calories) + ": " + day.getTotalCalories() + "  " +
                context.getString(R.string.protein) + ": " + day.getTotalProtein() + "g" +
                "  " + context.getString(R.string.fat) + ": " + day.getTotalFat() + "g";
        holder.tvTotals.setText(totalsText);

        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onItemClick(day);
            }
        });
    }

    @Override
    public int getItemCount() {
        return dayList == null ? 0 : dayList.size();
    }

    public static class DayViewHolder extends RecyclerView.ViewHolder {
        TextView tvDate, tvTotals;
        public DayViewHolder(@NonNull View itemView) {
            super(itemView);
            tvDate = itemView.findViewById(R.id.tvDayDate);
            tvTotals = itemView.findViewById(R.id.tvDayTotals);
        }
    }

    public void updateDays(List<Day> newDays) {
        this.dayList = newDays;
        notifyDataSetChanged();
    }
}