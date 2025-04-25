package com.crack.vapp.Utils;

import android.util.Log;

//日志

public class lg {
    private static final String TAG = "lg";

    public static void d( String msg) {
        Log.d(TAG, "d: " + msg);
    }

    public static void e( String msg) {
        Log.e(TAG, "e: " + msg);
    }

    public static void i( String msg) {
        Log.i(TAG, "i: " + msg);
    }
}
