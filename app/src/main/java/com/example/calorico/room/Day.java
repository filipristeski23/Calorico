package com.example.calorico.room;

import androidx.room.Entity;
import androidx.room.PrimaryKey;
import androidx.annotation.NonNull;

@Entity(tableName = "days")
public class Day {
    @PrimaryKey(autoGenerate = true)
    private int id;

    @NonNull
    private String date;

    private int totalCalories;
    private int totalProtein;
    private int totalFat;

    public Day(@NonNull String date, int totalCalories, int totalProtein, int totalFat) {
        this.date = date;
        this.totalCalories = totalCalories;
        this.totalProtein = totalProtein;
        this.totalFat = totalFat;
    }

    public int getId() {
        return id;
    }
    public void setId(int id) {
        this.id = id;
    }

    @NonNull
    public String getDate() {
        return date;
    }
    public void setDate(@NonNull String date) {
        this.date = date;
    }

    public int getTotalCalories() {
        return totalCalories;
    }
    public void setTotalCalories(int totalCalories) {
        this.totalCalories = totalCalories;
    }

    public int getTotalProtein() {
        return totalProtein;
    }
    public void setTotalProtein(int totalProtein) {
        this.totalProtein = totalProtein;
    }

    public int getTotalFat() {
        return totalFat;
    }
    public void setTotalFat(int totalFat) {
        this.totalFat = totalFat;
    }
}
