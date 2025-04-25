package com.crack.vapp.test;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.util.Log;

public class testService extends Service {

    static String TAG = "testService";
    public testService() {
    }

    @Override
    public void onCreate() {
        Log.d(TAG, "onCreate: 代理 testService 运行 ....  理论上 不会运行");
        super.onCreate();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d(TAG, "onStartCommand: 代理 testService 运行 ....  理论上 不会运行");
        return super.onStartCommand(intent, flags, startId);
    }

    @Override
    public IBinder onBind(Intent intent) {
        // TODO: Return the communication channel to the service.
        throw new UnsupportedOperationException("Not yet implemented");
    }
}