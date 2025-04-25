package com.crack.vapp;

import android.app.Activity;
import android.app.Application;
import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;

public class BaseData extends Application {
    public static Application app;
    public static Context context;
    public static Activity baseActivity;

    public static String proxyActivity = "com.crack.vapp.ui.ProxyActivity";

    public static String oriAppName = "com.crack.vapp";


    public static PackageInfo packageInfo = null;

    public static Object Package = null;

    static {
        System.loadLibrary("vapp");
    }

    @Override
    public Context getBaseContext() {
        return super.getBaseContext();
    }

    @Override
    protected void attachBaseContext(Context base) {
        super.attachBaseContext(base);
        app = this;
        context = base;
    }

    private  native void nativeInit();
}
