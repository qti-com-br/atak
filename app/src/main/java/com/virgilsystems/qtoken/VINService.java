package com.virgilsystems.qtoken;

import android.content.Context;
import android.content.Intent;
import android.util.Log;

import androidx.annotation.Nullable;
import androidx.core.app.JobIntentService;


public class VINService extends JobIntentService {

    static final int JOB_ID = 1348;
    static final String TAG = VINService.class.getSimpleName();

    public static void enqueueWork(Context context, Intent work) {
        enqueueWork(context, VINService.class, JOB_ID, work);
    }

    @Override
    public int onStartCommand(@Nullable Intent intent, int flags, int startId) {
        return super.onStartCommand(intent, flags, startId);
    }

    @Override
    public void onCreate() {
        super.onCreate();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }

    @Override
    protected void onHandleWork(Intent intent) {
        Log.i(TAG, "Started onHandleWork");
//        String value = intent.getExtras().getString("Key");
//        Log.i(TAG, value);
    }
}
