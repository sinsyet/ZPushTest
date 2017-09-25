package com.example.zpushtest.base;

import android.app.Application;
import android.content.Context;
import android.util.Log;

import com.example.zpushtest.utils.SP;
import com.zzwtec.distributedpush.api.ZZWPush;


public class APP extends Application {
    private static final String TAG = "APP";
    public static Context sCtx;
    @Override
    public void onCreate() {
        super.onCreate();

        sCtx = this;

        ZZWPush.init(getApplicationContext(), true, new ZZWPush.PushClientInitListener() {
            @Override
            public void onInit(boolean success) {
                Log.e(TAG, "onInit: "+success);
            }
        });
        SP.init(getApplicationContext());
    }
}
