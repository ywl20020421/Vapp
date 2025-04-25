package com.crack.vapp.service;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.util.Log;

import java.util.ArrayList;

public class ProxyService extends Service {


    private static final String TAG = "ProxyService";
    public ProxyService() {
    }

    @Override
    public void onCreate() {
        Log.d(TAG, "onCreate: 代理 ProxyService 运行 ....  理论上 不会运行");
        super.onCreate();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d(TAG, "onStartCommand: 代理 ProxyService 运行 ....  理论上 不会运行");
        return super.onStartCommand(intent, flags, startId);
    }

    @Override
    public IBinder onBind(Intent intent) {
        // TODO: Return the communication channel to the service.
        throw new UnsupportedOperationException("Not yet implemented");
    }
}