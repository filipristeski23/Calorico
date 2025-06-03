package com.example.calorico;

import android.content.Context;
import androidx.annotation.NonNull;
import androidx.work.Data;
import androidx.work.Worker;
import androidx.work.WorkerParameters;
import com.example.calorico.room.AppDatabase;
import com.example.calorico.room.FoodDao;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.firestore.FirebaseFirestore;
import java.util.List;

public class CheckFoodLoggingWorker extends Worker {
    private static final String KEY_IS_ANON = "isAnonymous";
    private static final String KEY_DAY_ID_ROOM = "dayIdRoom";
    private static final String KEY_UID = "uid";
    private static final String KEY_DAY_DOC_ID = "dayDocId";
    private static final String KEY_DAY_DATE = "dayDate";

    public CheckFoodLoggingWorker(@NonNull Context context, @NonNull WorkerParameters params) {
        super(context, params);
    }

    @NonNull
    @Override
    public Result doWork() {
        boolean isAnonymous = getInputData().getBoolean(KEY_IS_ANON, true);
        String dayDate = getInputData().getString(KEY_DAY_DATE);
        if (isAnonymous) {
            int dayIdRoom = getInputData().getInt(KEY_DAY_ID_ROOM, -1);
            if (dayIdRoom == -1) {
                return Result.failure();
            }
            AppDatabase db = AppDatabase.getInstance(getApplicationContext());
            FoodDao foodDao = db.foodDao();
            List<?> foods = foodDao.getFoodsForDay(dayIdRoom);
            if (foods == null || foods.isEmpty()) {
                NotificationHelper.sendForgotFoodNotification(getApplicationContext(), dayDate, dayIdRoom);
            }
        } else {
            String uid = getInputData().getString(KEY_UID);
            String dayDocId = getInputData().getString(KEY_DAY_DOC_ID);
            if (uid == null || dayDocId == null) {
                return Result.failure();
            }
            try {
                FirebaseFirestore firestore = FirebaseFirestore.getInstance();
                List<?> docs = Tasks.await(
                        firestore
                                .collection("users")
                                .document(uid)
                                .collection("days")
                                .document(dayDocId)
                                .collection("foods")
                                .get()
                ).getDocuments();
                if (docs == null || docs.isEmpty()) {
                    int notifId = dayDocId.hashCode();
                    NotificationHelper.sendForgotFoodNotification(getApplicationContext(), dayDate, notifId);
                }
            } catch (Exception e) {
                return Result.retry();
            }
        }
        return Result.success();
    }

    public static Data makeInputDataForRoom(int dayIdRoom, String dayDate) {
        return new Data.Builder()
                .putBoolean(KEY_IS_ANON, true)
                .putInt(KEY_DAY_ID_ROOM, dayIdRoom)
                .putString(KEY_DAY_DATE, dayDate)
                .build();
    }

    public static Data makeInputDataForFirestore(String uid, String dayDocId, String dayDate) {
        return new Data.Builder()
                .putBoolean(KEY_IS_ANON, false)
                .putString(KEY_UID, uid)
                .putString(KEY_DAY_DOC_ID, dayDocId)
                .putString(KEY_DAY_DATE, dayDate)
                .build();
    }
}
