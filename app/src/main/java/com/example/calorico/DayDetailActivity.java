package com.example.calorico;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.example.calorico.room.AppDatabase;
import com.example.calorico.room.Day;
import com.example.calorico.room.Food;
import com.example.calorico.room.FoodDao;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import java.util.ArrayList;
import java.util.List;


public class DayDetailActivity extends AppCompatActivity {

    private RecyclerView rvFoods;
    private FoodAdapter foodAdapter;
    private TextView tvFoodEmpty;
    private FloatingActionButton fabAddFood;
    private AppDatabase db;
    private int dayId;
    private Day currentDay;  // to update totals

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_day_detail);

        // 1) Find views
        rvFoods = findViewById(R.id.rvFoods);
        tvFoodEmpty = findViewById(R.id.tvFoodEmpty);
        fabAddFood = findViewById(R.id.fabAddFood);
        Button btnDetailLogout = findViewById(R.id.btnDetailLogout);

        // 2) RecyclerView setup
        rvFoods.setLayoutManager(new LinearLayoutManager(this));
        foodAdapter = new FoodAdapter(new ArrayList<>());
        rvFoods.setAdapter(foodAdapter);

        // 3) Database & dayId from Intent
        db = AppDatabase.getInstance(getApplicationContext());
        dayId = getIntent().getIntExtra("dayId", -1);

        // 4) Load this day and its foods
        loadDayAndFoods();

        // 5) Show dialog to add a new Food
        fabAddFood.setOnClickListener(v -> showAddFoodDialog());

        // 6) Logout: finish() back to Login
        btnDetailLogout.setOnClickListener(v -> finish());
    }

    private void loadDayAndFoods() {
        new Thread(() -> {
            currentDay = db.dayDao().getDayById(dayId);
            List<Food> foods = db.foodDao().getFoodsForDay(dayId);

            runOnUiThread(() -> {
                if (foods.isEmpty()) {
                    tvFoodEmpty.setVisibility(View.VISIBLE);
                    rvFoods.setVisibility(View.GONE);
                } else {
                    tvFoodEmpty.setVisibility(View.GONE);
                    rvFoods.setVisibility(View.VISIBLE);
                    foodAdapter.updateFoods(foods);
                }
            });
        }).start();
    }

    private void showAddFoodDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Add Food");

        View dialogView = getLayoutInflater().inflate(R.layout.dialog_add_food, null);
        builder.setView(dialogView);

        EditText etTitle = dialogView.findViewById(R.id.etFoodTitle);
        EditText etCalories = dialogView.findViewById(R.id.etFoodCalories);
        EditText etProtein = dialogView.findViewById(R.id.etFoodProtein);
        EditText etFat = dialogView.findViewById(R.id.etFoodFat);

        builder.setPositiveButton("Add", (dialog, which) -> {
            String title = etTitle.getText().toString().trim();
            int calories = Integer.parseInt(etCalories.getText().toString().trim());
            int protein = Integer.parseInt(etProtein.getText().toString().trim());
            int fat = Integer.parseInt(etFat.getText().toString().trim());
            insertFood(title, calories, protein, fat);
        });
        builder.setNegativeButton("Cancel", null);

        builder.show();
    }

    private void insertFood(String title, int calories, int protein, int fat) {
        new Thread(() -> {
            FoodDao foodDao = db.foodDao();
            Food food = new Food(dayId, title, calories, protein, fat);
            foodDao.insertFood(food);

            currentDay.setTotalCalories(currentDay.getTotalCalories() + calories);
            currentDay.setTotalProtein(currentDay.getTotalProtein() + protein);
            currentDay.setTotalFat(currentDay.getTotalFat() + fat);
            db.dayDao().updateDay(currentDay);

            loadDayAndFoods();
        }).start();
    }
}
