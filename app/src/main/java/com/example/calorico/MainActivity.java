package com.example.calorico;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.work.Data;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkManager;
import com.example.calorico.room.AppDatabase;
import com.example.calorico.room.Day;
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
    private static final int DAY_DETAIL_REQUEST = 100;
    private RecyclerView rvDays;
    private DayAdapter dayAdapter;
    private TextView tvEmpty;
    private FloatingActionButton fabAddDay;
    private AppDatabase db;
    private FirebaseAuth mAuth;
    private FirebaseFirestore firestore;
    private String uid;
    private boolean isAnonymousUser;
    private TextView tvLangMK, tvLangEN;

    private final ActivityResultLauncher<String> requestPermissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), isGranted -> { });

    @Override
    protected void attachBaseContext(Context newBase) {
        Context context = LocaleHelper.onAttach(newBase);
        super.attachBaseContext(context);
    }

    private boolean isTablet() {
        return getResources().getConfiguration().smallestScreenWidthDp >= 600;
    }

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
        tvLangMK = findViewById(R.id.tvLangMK);
        tvLangEN = findViewById(R.id.tvLangEN);

        int spanCount;
        if (isTablet()) {
            spanCount = 2;
        } else {
            int orientation = getResources().getConfiguration().orientation;
            spanCount = (orientation == Configuration.ORIENTATION_LANDSCAPE) ? 2 : 1;
        }
        rvDays.setLayoutManager(new GridLayoutManager(this, spanCount));
        dayAdapter = new DayAdapter(this, new ArrayList<>());
        rvDays.setAdapter(dayAdapter);

        dayAdapter.setOnItemClickListener(day -> {
            Intent intent = new Intent(MainActivity.this, DayDetailActivity.class);
            intent.putExtra("dayDate", day.getDate());
            intent.putExtra("dayIdRoom", day.getId());
            startActivityForResult(intent, DAY_DETAIL_REQUEST);
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

        tvLangMK.setText(R.string.language_mk);
        tvLangEN.setText(R.string.language_en);

        tvLangMK.setOnClickListener(v -> {
            LocaleHelper.setLocale(MainActivity.this, "mk");
            recreate();
        });

        tvLangEN.setOnClickListener(v -> {
            LocaleHelper.setLocale(MainActivity.this, "en");
            recreate();
        });

        loadDays();

        fabAddDay.setOnClickListener(v -> {
            if (isAnonymousUser) {
                insertNewDayRoom();
            } else {
                insertNewDayFirestore();
            }
        });

        btnLogout.setText(R.string.logout);
        btnLogout.setOnClickListener(v -> {
            mAuth.signOut();
            Intent intent = new Intent(MainActivity.this, RegisterActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadDays();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == DAY_DETAIL_REQUEST && resultCode == RESULT_OK) {
            loadDays();
        }
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);

        int spanCount;
        if (isTablet()) {
            spanCount = 2;
        } else {
            spanCount = (newConfig.orientation == Configuration.ORIENTATION_LANDSCAPE) ? 2 : 1;
        }
        rvDays.setLayoutManager(new GridLayoutManager(this, spanCount));
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
                    tvEmpty.setText(R.string.no_days);
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
                            tvEmpty.setText(R.string.no_days);
                            tvEmpty.setVisibility(View.VISIBLE);
                            rvDays.setVisibility(View.GONE);
                        } else {
                            tvEmpty.setVisibility(View.GONE);
                            rvDays.setVisibility(View.VISIBLE);
                            dayAdapter.updateDays(dayList);
                        }
                    } else {
                        tvEmpty.setText(R.string.no_days);
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
                    .setInitialDelay(1, TimeUnit.MINUTES)
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
                            .setInitialDelay(1, TimeUnit.MINUTES)
                            .build();
                    WorkManager.getInstance(getApplicationContext()).enqueue(checkWork);
                    loadDaysFromFirestore();
                })
                .addOnFailureListener(e -> loadDaysFromFirestore());
    }
}