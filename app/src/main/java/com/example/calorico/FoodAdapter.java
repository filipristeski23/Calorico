package com.example.calorico;

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

    public FoodAdapter(List<Food> foodList) {
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
        String valuesText = "Calories: " + food.getCalories() +
                "  Protein: " + food.getProtein() + "g" +
                "  Fat: " + food.getFat() + "g";
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
