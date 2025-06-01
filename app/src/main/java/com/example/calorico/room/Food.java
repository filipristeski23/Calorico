package com.example.calorico.room;

import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.PrimaryKey;
import androidx.annotation.NonNull;

import static androidx.room.ForeignKey.CASCADE;

@Entity(
        tableName = "foods",
        foreignKeys = @ForeignKey(
                entity = Day.class,
                parentColumns = "id",
                childColumns = "dayId",
                onDelete = CASCADE
        )
)
public class Food {
    @PrimaryKey(autoGenerate = true)
    private int id;

    private int dayId;

    @NonNull
    private String title;

    private int calories;
    private int protein;
    private int fat;


    public Food(int dayId, @NonNull String title, int calories, int protein, int fat) {
        this.dayId = dayId;
        this.title = title;
        this.calories = calories;
        this.protein = protein;
        this.fat = fat;
    }


    public int getId() {
        return id;
    }
    public void setId(int id) {
        this.id = id;
    }

    public int getDayId() {
        return dayId;
    }
    public void setDayId(int dayId) {
        this.dayId = dayId;
    }

    @NonNull
    public String getTitle() {
        return title;
    }
    public void setTitle(@NonNull String title) {
        this.title = title;
    }

    public int getCalories() {
        return calories;
    }
    public void setCalories(int calories) {
        this.calories = calories;
    }

    public int getProtein() {
        return protein;
    }
    public void setProtein(int protein) {
        this.protein = protein;
    }

    public int getFat() {
        return fat;
    }
    public void setFat(int fat) {
        this.fat = fat;
    }
}
