package com.example.calorico;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.example.calorico.room.Food;
import java.util.List;

public class FoodAdapter extends RecyclerView.Adapter<FoodAdapter.FoodViewHolder> {
    private List<Food> foodList;
    private Context context;

    public FoodAdapter(Context context, List<Food> foodList) {
        this.context = context;
        this.foodList = foodList;
    }

    @NonNull
    @Override
    public FoodViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.food_item, parent, false);
        return new FoodViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull FoodViewHolder holder, int position) {
        Food food = foodList.get(position);
        holder.tvTitle.setText(food.getTitle());

        String valuesText = context.getString(R.string.calories) + ": " + food.getCalories() + "  " +
                context.getString(R.string.protein) + ": " + food.getProtein() + "g" +
                "  " + context.getString(R.string.fat) + ": " + food.getFat() + "g";
        holder.tvValues.setText(valuesText);
    }

    @Override
    public int getItemCount() {
        return foodList == null ? 0 : foodList.size();
    }

    public static class FoodViewHolder extends RecyclerView.ViewHolder {
        TextView tvTitle, tvValues;
        public FoodViewHolder(@NonNull View itemView) {
            super(itemView);
            tvTitle = itemView.findViewById(R.id.tvFoodTitle);
            tvValues = itemView.findViewById(R.id.tvFoodValues);
        }
    }

    public void updateFoods(List<Food> newFoods) {
        this.foodList = newFoods;
        notifyDataSetChanged();
    }
}