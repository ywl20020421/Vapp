package com.crack.vapp.core;

import static com.crack.vapp.BaseData.context;

import android.util.Log;

import java.io.File;
import java.io.FilenameFilter;
import java.lang.reflect.Method;

import de.robv.android.xposed.CA_MethodHook;
import de.robv.android.xposed.CalvinBridge;

public class HookSoLoad {

    private static final String TAG = "HookSoLoad";

    public static void hook(File dir) {
        //加载so文件   思路 预加载so文件
        File soDir = new File(dir, "lib/arm64-v8a");
        if(soDir.exists()){
            File[] files = soDir.listFiles(new FilenameFilter() {
                @Override
                public boolean accept(File dir, String name) {
                    return name.contains(".so");
                }
            });
            if(files != null){
                for (File file : files) {
                    System.load(file.getAbsolutePath());
                    Log.d(TAG, "so 加载："+file.getAbsolutePath());
                }
            }else {
                Log.d(TAG, "so 文件不存在");
            }
            //拦截 so加载函数
            try {
                Class<?> System = context.getClassLoader().loadClass("java.lang.System");
                Method loadLibrary = System.getDeclaredMethod("loadLibrary", String.class);
                CalvinBridge.hookMethod(loadLibrary, new CA_MethodHook() {
                    @Override
                    protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                        Log.d(TAG, "loadLibrary: "+param.args[0]);
                        param.args[0] = "c";

                    }
                });
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }



    }
}
