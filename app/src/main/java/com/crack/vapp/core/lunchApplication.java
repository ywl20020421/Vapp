package com.crack.vapp.core;

import android.app.Application;
import android.content.Context;
import android.content.ContextWrapper;
import android.util.Log;

import com.crack.vapp.BaseData;
import com.crack.vapp.Utils.ReflectUtils;

import java.lang.reflect.Method;

public class lunchApplication {
    private static final String TAG = "lunchApplication";

    public static void lunch(String name)
    {
        if(name != null){
            Class<?> application = null;
            try {
                application = BaseData.app.getClassLoader().loadClass(name);
            } catch (ClassNotFoundException e) {
                throw new RuntimeException(e);
            }
            if(application != null){
                Log.d(TAG, "初始化 applicaltion ");
                try {
                    Application o = (Application)application.getConstructor().newInstance();
                    Method attachBaseContext = ReflectUtils.getMethod(application.getClass(), "attachBaseContext", ContextWrapper.class);
                    if(attachBaseContext != null){
                        attachBaseContext.invoke(o, BaseData.context);
                    }
                    o.onCreate();
                    Log.d(TAG, "初始化 applicaltion 成功");

                } catch (Exception e) {
//                    throw new RuntimeException(e);
                }
            }
        }
    }

}
