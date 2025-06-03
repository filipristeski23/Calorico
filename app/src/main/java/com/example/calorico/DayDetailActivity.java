package com.example.calorico;

import android.content.Context;
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
import com.example.calorico.room.DayDao;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import java.util.ArrayList;
import java.util.List;

public class DayDetailActivity extends AppCompatActivity {

    private RecyclerView rvFoods;
    private FoodAdapter foodAdapter;
    private TextView tvFoodEmpty;
    private FloatingActionButton fabAddFood;
    private AppDatabase db;
    private boolean isAnonymousUser;
    private int dayIdRoom;
    private Day currentDayRoom;
    private String dayDate;
    private FirebaseAuth mAuth;
    private FirebaseFirestore firestore;
    private String uid;
    private TextView tvLangMK, tvLangEN;

    @Override
    protected void attachBaseContext(Context newBase) {
        Context context = LocaleHelper.onAttach(newBase);
        super.attachBaseContext(context);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_day_detail);

        rvFoods = findViewById(R.id.rvFoods);
        tvFoodEmpty = findViewById(R.id.tvFoodEmpty);
        fabAddFood = findViewById(R.id.fabAddFood);
        Button btnDetailLogout = findViewById(R.id.btnDetailLogout);
        tvLangMK = findViewById(R.id.tvLangMK);
        tvLangEN = findViewById(R.id.tvLangEN);

        rvFoods.setLayoutManager(new LinearLayoutManager(this));
        foodAdapter = new FoodAdapter(this, new ArrayList<>());
        rvFoods.setAdapter(foodAdapter);

        db = AppDatabase.getInstance(getApplicationContext());
        mAuth = FirebaseAuth.getInstance();
        FirebaseUser user = mAuth.getCurrentUser();
        if (user == null) {
            finish();
            return;
        }

        isAnonymousUser = user.isAnonymous();
        if (isAnonymousUser) {
            dayIdRoom = getIntent().getIntExtra("dayIdRoom", -1);
        } else {
            uid = user.getUid();
            firestore = FirebaseFirestore.getInstance();
            dayDate = getIntent().getStringExtra("dayDate");
        }

        tvLangMK.setText(R.string.language_mk);
        tvLangEN.setText(R.string.language_en);

        tvLangMK.setOnClickListener(v -> {
            LocaleHelper.setLocale(DayDetailActivity.this, "mk");
            recreate();
        });

        tvLangEN.setOnClickListener(v -> {
            LocaleHelper.setLocale(DayDetailActivity.this, "en");
            recreate();
        });

        btnDetailLogout.setText(R.string.logout);
        btnDetailLogout.setOnClickListener(v -> {
            mAuth.signOut();
            finish();
        });

        loadDayAndFoods();

        fabAddFood.setOnClickListener(v -> showAddFoodDialog());
    }

    private void loadDayAndFoods() {
        if (isAnonymousUser) {
            loadRoomData();
        } else {
            loadFirestoreData();
        }
    }

    private void loadRoomData() {
        new Thread(() -> {
            DayDao dayDao = db.dayDao();
            FoodDao foodDao = db.foodDao();
            currentDayRoom = dayDao.getDayById(dayIdRoom);
            List<Food> foods = foodDao.getFoodsForDay(dayIdRoom);

            runOnUiThread(() -> {
                if (foods.isEmpty()) {
                    tvFoodEmpty.setText(R.string.no_foods);
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

    private void insertFoodRoom(String title, int calories, int protein, int fat) {
        new Thread(() -> {
            FoodDao foodDao = db.foodDao();
            Food food = new Food(dayIdRoom, title, calories, protein, fat);
            foodDao.insertFood(food);

            currentDayRoom.setTotalCalories(currentDayRoom.getTotalCalories() + calories);
            currentDayRoom.setTotalProtein(currentDayRoom.getTotalProtein() + protein);
            currentDayRoom.setTotalFat(currentDayRoom.getTotalFat() + fat);
            db.dayDao().updateDay(currentDayRoom);

            loadRoomData();
        }).start();
    }

    private void loadFirestoreData() {
        CollectionReference daysRef = firestore
                .collection("users")
                .document(uid)
                .collection("days");

        daysRef.whereEqualTo("date", dayDate)
                .limit(1)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    if (!querySnapshot.isEmpty()) {
                        DocumentSnapshot dayDoc = querySnapshot.getDocuments().get(0);

                        String dayDocId = dayDoc.getId();

                        CollectionReference foodsRef = daysRef
                                .document(dayDocId)
                                .collection("foods");

                        foodsRef.get().addOnSuccessListener(foodSnap -> {
                            List<Food> list = new ArrayList<>();
                            for (QueryDocumentSnapshot foodDoc : foodSnap) {
                                String title = foodDoc.getString("title");
                                long cals = foodDoc.getLong("calories");
                                long prot = foodDoc.getLong("protein");
                                long fatF = foodDoc.getLong("fat");
                                Food f = new Food(0, title, (int) cals, (int) prot, (int) fatF);
                                list.add(f);
                            }
                            if (list.isEmpty()) {
                                tvFoodEmpty.setText(R.string.no_foods);
                                tvFoodEmpty.setVisibility(View.VISIBLE);
                                rvFoods.setVisibility(View.GONE);
                            } else {
                                tvFoodEmpty.setVisibility(View.GONE);
                                rvFoods.setVisibility(View.VISIBLE);
                                foodAdapter.updateFoods(list);
                            }
                        });

                    } else {
                        tvFoodEmpty.setText(R.string.no_foods);
                        tvFoodEmpty.setVisibility(View.VISIBLE);
                        rvFoods.setVisibility(View.GONE);
                    }
                });
    }

    private void insertFoodFirestore(String title, int calories, int protein, int fat) {
        CollectionReference daysRef = firestore
                .collection("users")
                .document(uid)
                .collection("days");

        daysRef.whereEqualTo("date", dayDate)
                .limit(1)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    if (!querySnapshot.isEmpty()) {
                        DocumentSnapshot dayDoc = querySnapshot.getDocuments().get(0);

                        String dayDocId = dayDoc.getId();
                        DocumentReference dayRef = daysRef.document(dayDocId);

                        java.util.Map<String, Object> foodMap = new java.util.HashMap<>();
                        foodMap.put("title", title);
                        foodMap.put("calories", calories);
                        foodMap.put("protein", protein);
                        foodMap.put("fat", fat);

                        dayRef.collection("foods")
                                .add(foodMap)
                                .addOnSuccessListener(foodAddTask -> {
                                    long prevCals = dayDoc.getLong("totalCalories");
                                    long prevProt = dayDoc.getLong("totalProtein");
                                    long prevFat = dayDoc.getLong("totalFat");

                                    java.util.Map<String, Object> updates = new java.util.HashMap<>();
                                    updates.put("totalCalories", prevCals + calories);
                                    updates.put("totalProtein", prevProt + protein);
                                    updates.put("totalFat", prevFat + fat);

                                    dayRef.update(updates)
                                            .addOnSuccessListener(updateTask -> loadFirestoreData());
                                });
                    }
                });
    }

    private void showAddFoodDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(R.string.add_food);

        View dialogView = getLayoutInflater().inflate(R.layout.dialog_add_food, null);
        builder.setView(dialogView);

        EditText etTitle = dialogView.findViewById(R.id.etFoodTitle);
        EditText etCalories = dialogView.findViewById(R.id.etFoodCalories);
        EditText etProtein = dialogView.findViewById(R.id.etFoodProtein);
        EditText etFat = dialogView.findViewById(R.id.etFoodFat);

        etTitle.setHint(R.string.food_title);
        etCalories.setHint(R.string.calories_hint);
        etProtein.setHint(R.string.protein_hint);
        etFat.setHint(R.string.fat_hint);

        builder.setPositiveButton(R.string.add, (dialog, which) -> {
            String title = etTitle.getText().toString().trim();
            int calories = Integer.parseInt(etCalories.getText().toString().trim());
            int protein = Integer.parseInt(etProtein.getText().toString().trim());
            int fat = Integer.parseInt(etFat.getText().toString().trim());

            if (isAnonymousUser) {
                insertFoodRoom(title, calories, protein, fat);
            } else {
                insertFoodFirestore(title, calories, protein, fat);
            }
        });
        builder.setNegativeButton(R.string.cancel, null);

        builder.show();
    }
}
