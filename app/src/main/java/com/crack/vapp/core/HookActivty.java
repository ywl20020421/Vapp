package com.crack.vapp.core;

import static com.crack.vapp.BaseData.context;
import static com.crack.vapp.BaseData.packageInfo;

import android.app.Activity;
import android.app.Instrumentation;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;

import com.crack.vapp.BaseData;
import com.crack.vapp.Utils.ReflectUtils;
import com.crack.vapp.ui.ProxyActivity;

import java.lang.reflect.Method;

import de.robv.android.xposed.CA_MethodHook;
import de.robv.android.xposed.CalvinBridge;

public class HookActivty {
    private static final String TAG = "HookActivty";


    public static void hook()
    {
        //开启 欺骗 ams
        Method execStartActivity = null;
        try {
            execStartActivity = Instrumentation.class.getDeclaredMethod("execStartActivity",  Context.class,//0
                    IBinder.class,//1
                    IBinder.class,//2
                    Activity.class,//3
                    Intent.class,//4
                    int.class,//5
                    Bundle.class);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        CalvinBridge.hookMethod(execStartActivity, new CA_MethodHook() {
            @Override
            public void beforeHookedMethod(MethodHookParam param) throws Throwable {
                Log.e("hook", "hook 测试成功！"+param.args[3]);
                Intent oriIntent =(Intent) param.args[4];
                Intent intent = new Intent((Activity) param.args[3], ProxyActivity.class);
                intent.setComponent(new ComponentName(BaseData.oriAppName,BaseData.proxyActivity));
                intent.putExtra("oriIntent",oriIntent);
                param.args[4] = intent;
            }
        });


        //欺骗 app
        try {
            Class<?> aClass = ReflectUtils.loadClass("android.app.ActivityThread");
            Class<?> ActivityClientRecord = ReflectUtils.loadClass("android.app.ActivityThread");
            CalvinBridge.hookAllMethods(aClass, "performLaunchActivity", new CA_MethodHook() {
                @Override
                protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                    Log.e(TAG, "createBaseContextForActivity " + param.args[0].getClass());
                    Object arg = param.args[0];
//                    fs.printAllFieldValues(arg);
////                        fs.printAllFieldValues(param.args[1]);
                    Object activityInfo = ReflectUtils.getField(arg, "activityInfo");
//                    fs.printAllFieldValues(activityInfo);
//                    ReflectUtils.printFieldValues(activityInfo);

                    Object LoadedApk = ReflectUtils.getField(arg, "packageInfo");
                    ReflectUtils.printFieldValues(LoadedApk);
                    ReflectUtils.setField(LoadedApk, "mApplication", null);
                    ReflectUtils.setField(LoadedApk, "mApplicationInfo", packageInfo.applicationInfo);

//                    fs.printAllFieldValues(LoadedApk);
//                    Object mAppComponentFactory = fs.getFieldValue(LoadedApk, "mAppComponentFactory");
//                    fs.printAllFieldValues(mAppComponentFactory);

                    //设置资源
//                        fs.setFieldValue(LoadedApk, "mResources", apkResources);

                    //设置 Intent Intent intent = Intent { cmp=com.ywl.fva/.ui.ProxyActivity (has extras) }
                    Intent FakeIntent =(Intent) ReflectUtils.getField(arg, "intent");
                    if(FakeIntent != null){
                        Intent oriIntent = (Intent)FakeIntent.getParcelableExtra("oriIntent");
                        if(oriIntent != null){
                            Log.e(TAG, "替换 Intent intent = " + oriIntent);
                            ReflectUtils.setField(arg, "intent", oriIntent);
                        }
                    }



//                    Intent intent =(Intent) fs.getFieldValue(arg, "intent");
//                    if(intent != null){
//                        Intent oriIntent = (Intent)intent.getParcelableExtra("oriIntent");
//                        if(oriIntent != null){
//                            Log.e(TAG, "替换 Intent intent = " + oriIntent);
//                            fs.setFieldValue(arg, "intent", oriIntent);
//                        }
//                    }

                    //设置 主题 theme
//                        ReflectUtils.setFieldValue(activityInfo, "theme", packageInfo.applicationInfo.theme);
                    ReflectUtils.setField(activityInfo, "theme", packageInfo.applicationInfo.theme);

//
                    super.beforeHookedMethod(param);
                }
            });

        } catch (Exception e) {

        }



        // 处理包名
        Method getPackageName = ReflectUtils.getMethod(context.getClass(), "getPackageName");
        CalvinBridge.hookMethod(getPackageName, new CA_MethodHook() {
            @Override
            protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                super.beforeHookedMethod(param);
            }

            @Override
            protected void afterHookedMethod(MethodHookParam param) throws Throwable {
//                    Log.e(TAG, "getPackageName " + param.getResult());
//                    param.setResult("com.crack.testdemo");
                param.setResult(packageInfo.packageName);
                super.afterHookedMethod(param);
            }
        });
    }
}
