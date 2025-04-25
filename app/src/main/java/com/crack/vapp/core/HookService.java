package com.crack.vapp.core;

import android.content.ContextWrapper;
import android.content.Intent;
import android.util.Log;

import com.crack.vapp.BaseData;
import com.crack.vapp.Utils.ReflectUtils;
import com.crack.vapp.service.ProxyService;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import de.robv.android.xposed.CA_MethodHook;
import de.robv.android.xposed.CalvinBridge;

public class HookService {
    private static final String TAG = "HookService";

    static HashMap<String ,Intent> map = new HashMap<>();
    public static  void hook(){
        //开启 欺骗 ams
        //startService(Intent service)  类 android.app.
        Method startService = ReflectUtils.getMethod(ContextWrapper.class, "startService", Intent.class);
        CalvinBridge.hookMethod(startService, new CA_MethodHook() {
            @Override
            protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                Log.e(TAG, "startService: " );
                ReflectUtils.printFieldValues(param.args[0]);
                Intent oriintent =(Intent) param.args[0];
//                intentList.contains(oriintent)
                map.put("ori", oriintent);
                Intent Fakeintent = new Intent(BaseData.baseActivity, ProxyService.class);
                Fakeintent.setComponent(new android.content.ComponentName(BaseData.oriAppName,"com.crack.vapp.service.ProxyService"));
                Fakeintent.putExtra("oriIntent",oriintent);
                param.args[0] = Fakeintent;
                ReflectUtils.printFieldValues(param.args[0]);
                super.beforeHookedMethod(param);
            }
        });


        // hookService  handleCreateService
        Class<?> ActivityThread = ReflectUtils.loadClass("android.app.ActivityThread");
        CalvinBridge.hookAllMethods(ActivityThread, "handleCreateService", new CA_MethodHook() {
            @Override
            protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                Log.e(TAG, "handleCreateService: " );
                ReflectUtils.printFieldValues(param.args[0]);
                Object info = ReflectUtils.getField(param.args[0], "info");
                ReflectUtils.printFieldValues(info);
                if(info != null){
                   ReflectUtils.setField(info, "name", map.get("ori").getComponent().getClassName());
                   ReflectUtils.setField(info, "packageName", map.get("ori").getComponent().getPackageName());
                }
                super.beforeHookedMethod(param);
            }
        });


    }
}
