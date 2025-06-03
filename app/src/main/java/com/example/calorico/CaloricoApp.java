package com.example.calorico;

import android.app.Application;
import android.content.Context;

public class CaloricoApp extends Application {
    @Override
    protected void attachBaseContext(Context base) {
        super.attachBaseContext(LocaleHelper.onAttach(base));
    }
}