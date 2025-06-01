package com.example.calorico.room;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Delete;

import java.util.List;

@Dao
public interface FoodDao {
    @Insert
    long insertFood(Food food);

    @Query("SELECT * FROM foods WHERE dayId = :dayId")
    List<Food> getFoodsForDay(int dayId);

    @Delete
    void deleteFood(Food food);

    @Query("DELETE FROM foods WHERE dayId = :dayId")
    void deleteFoodsForDay(int dayId);
}
