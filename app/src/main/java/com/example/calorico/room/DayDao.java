package com.example.calorico.room;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;
import androidx.room.Delete;

import java.util.List;
@Dao
public interface DayDao {
    @Insert
    long insertDay(Day day);

    @Query("SELECT * FROM days ORDER BY date DESC")
    List<Day> getAllDays();

    @Query("SELECT * FROM days WHERE id = :dayId LIMIT 1")
    Day getDayById(int dayId);

    @Update
    void updateDay(Day day);

    @Delete
    void deleteDay(Day day);
}
