package com.example.calorico;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.example.calorico.room.AppDatabase;
import com.example.calorico.room.Day;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class MainActivity extends AppCompatActivity {

    private RecyclerView rvDays;
    private DayAdapter dayAdapter;
    private TextView tvEmpty;
    private FloatingActionButton fabAddDay;
    private AppDatabase db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // 1) Find views by ID
        rvDays = findViewById(R.id.rvDays);
        tvEmpty = findViewById(R.id.tvEmpty);
        fabAddDay = findViewById(R.id.fabAddDay);
        Button btnLogout = findViewById(R.id.btnLogout);

        // 2) RecyclerView setup
        rvDays.setLayoutManager(new LinearLayoutManager(this));
        dayAdapter = new DayAdapter(new ArrayList<>());
        rvDays.setAdapter(dayAdapter);

        // 3) When a Day is tapped, open DayDetailActivity
        dayAdapter.setOnItemClickListener(day -> {
            Intent intent = new Intent(MainActivity.this, DayDetailActivity.class);
            intent.putExtra("dayId", day.getId());
            startActivity(intent);
        });

        // 4) Get Room database instance
        db = AppDatabase.getInstance(getApplicationContext());

        // 5) Load existing days
        loadDaysFromDb();

        // 6) FAB inserts a new Day (today’s date, zero totals)
        fabAddDay.setOnClickListener(v -> insertNewDay());

        // 7) Logout simply finishes this activity
        btnLogout.setOnClickListener(v -> finish());
    }

    private void loadDaysFromDb() {
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

    private void insertNewDay() {
        new Thread(() -> {
            String today = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                    .format(new Date());
            Day day = new Day(today, 0, 0, 0);
            db.dayDao().insertDay(day);
            loadDaysFromDb();
        }).start();
    }
}
