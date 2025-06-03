package com.example.calorico;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import com.example.calorico.room.AppDatabase;
import com.example.calorico.room.Day;
import com.example.calorico.room.FoodDao;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.work.Data;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkManager;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

public class MainActivity extends AppCompatActivity {
    private static final int REQUEST_NOTIFICATION_PERMISSION = 100;
    private RecyclerView rvDays;
    private DayAdapter dayAdapter;
    private TextView tvEmpty;
    private FloatingActionButton fabAddDay;
    private AppDatabase db;
    private FirebaseAuth mAuth;
    private FirebaseFirestore firestore;
    private String uid;
    private boolean isAnonymousUser;

    private final ActivityResultLauncher<String> requestPermissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), isGranted -> { });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS);
            }
        }
        NotificationHelper.createNotificationChannel(this);
        setContentView(R.layout.activity_main);

        rvDays = findViewById(R.id.rvDays);
        tvEmpty = findViewById(R.id.tvEmpty);
        fabAddDay = findViewById(R.id.fabAddDay);
        Button btnLogout = findViewById(R.id.btnLogout);

        rvDays.setLayoutManager(new LinearLayoutManager(this));
        dayAdapter = new DayAdapter(new ArrayList<>());
        rvDays.setAdapter(dayAdapter);

        dayAdapter.setOnItemClickListener(day -> {
            Intent intent = new Intent(MainActivity.this, DayDetailActivity.class);
            intent.putExtra("dayDate", day.getDate());
            intent.putExtra("dayIdRoom", day.getId());
            startActivity(intent);
        });

        db = AppDatabase.getInstance(getApplicationContext());
        mAuth = FirebaseAuth.getInstance();
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser == null) {
            startActivity(new Intent(MainActivity.this, WelcomeActivity.class));
            finish();
            return;
        }

        isAnonymousUser = currentUser.isAnonymous();
        if (!isAnonymousUser) {
            uid = currentUser.getUid();
            firestore = FirebaseFirestore.getInstance();
        }

        loadDays();

        fabAddDay.setOnClickListener(v -> {
            if (isAnonymousUser) {
                insertNewDayRoom();
            } else {
                insertNewDayFirestore();
            }
        });

        btnLogout.setOnClickListener(v -> {
            mAuth.signOut();
            startActivity(new Intent(MainActivity.this, WelcomeActivity.class));
            finish();
        });
    }

    private void loadDays() {
        if (isAnonymousUser) {
            loadDaysFromRoom();
        } else {
            loadDaysFromFirestore();
        }
    }

    private void loadDaysFromRoom() {
        new Thread(() -> {
            List<Day> allDays = db.dayDao().getAllDays();
            runOnUiThread(() -> {
                if (allDays.isEmpty()) {
                    tvEmpty.setVisibility(View.VISIBLE);
                    rvDays.setVisibility(View.GONE);
                } else {
                    tvEmpty.setVisibility(View.GONE);
                    rvDays.setVisibility(View.VISIBLE);
                    dayAdapter.updateDays(allDays);
                }
            });
        }).start();
    }

    private void loadDaysFromFirestore() {
        firestore.collection("users")
                .document(uid)
                .collection("days")
                .orderBy("date")
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        List<Day> dayList = new ArrayList<>();
                        for (QueryDocumentSnapshot doc : task.getResult()) {
                            String date = doc.getString("date");
                            long totalCalories = doc.getLong("totalCalories");
                            long totalProtein = doc.getLong("totalProtein");
                            long totalFat = doc.getLong("totalFat");
                            Day day = new Day(date, (int) totalCalories, (int) totalProtein, (int) totalFat);
                            dayList.add(day);
                        }
                        if (dayList.isEmpty()) {
                            tvEmpty.setVisibility(View.VISIBLE);
                            rvDays.setVisibility(View.GONE);
                        } else {
                            tvEmpty.setVisibility(View.GONE);
                            rvDays.setVisibility(View.VISIBLE);
                            dayAdapter.updateDays(dayList);
                        }
                    } else {
                        tvEmpty.setText("Failed to load days.");
                        tvEmpty.setVisibility(View.VISIBLE);
                        rvDays.setVisibility(View.GONE);
                    }
                });
    }

    private void insertNewDayRoom() {
        new Thread(() -> {
            String today = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date());
            Day day = new Day(today, 0, 0, 0);
            long newId = db.dayDao().insertDay(day);
            Data inputData = CheckFoodLoggingWorker.makeInputDataForRoom((int) newId, today);
            OneTimeWorkRequest checkWork = new OneTimeWorkRequest.Builder(CheckFoodLoggingWorker.class)
                    .setInputData(inputData)
                    .setInitialDelay(12, TimeUnit.HOURS)
                    .build();
            WorkManager.getInstance(getApplicationContext()).enqueue(checkWork);
            runOnUiThread(this::loadDaysFromRoom);
        }).start();
    }

    private void insertNewDayFirestore() {
        String today = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date());
        java.util.Map<String, Object> dayMap = new java.util.HashMap<>();
        dayMap.put("date", today);
        dayMap.put("totalCalories", 0);
        dayMap.put("totalProtein", 0);
        dayMap.put("totalFat", 0);

        firestore.collection("users")
                .document(uid)
                .collection("days")
                .add(dayMap)
                .addOnSuccessListener(documentReference -> {
                    String newDayDocId = documentReference.getId();
                    Data inputData = CheckFoodLoggingWorker.makeInputDataForFirestore(uid, newDayDocId, today);
                    OneTimeWorkRequest checkWork = new OneTimeWorkRequest.Builder(CheckFoodLoggingWorker.class)
                            .setInputData(inputData)
                            .setInitialDelay(12, TimeUnit.HOURS)
                            .build();
                    WorkManager.getInstance(getApplicationContext()).enqueue(checkWork);
                    loadDaysFromFirestore();
                })
                .addOnFailureListener(e -> loadDaysFromFirestore());
    }
}
