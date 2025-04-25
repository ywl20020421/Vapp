package com.crack.vapp.Utils;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.res.AssetManager;
import android.content.res.Resources;
import android.os.Build;
import android.util.TypedValue;

import java.lang.ref.WeakReference;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Map;

import de.robv.android.xposed.CA_MethodHook;
import de.robv.android.xposed.CalvinBridge;

public class AddPluginResources {
    public  static  AssetManager assetManager;

    public static Resources apkResources;

    public static Resources multiResources;



    public static void hookRec(){
        Class<?> aClass = ReflectUtils.loadClass("android.content.res.ResourcesImpl");
        Method getValue = ReflectUtils.getMethod(aClass, "getValue", int.class, TypedValue.class, boolean.class);
        try {
            CalvinBridge.hookMethod(getValue, new CA_MethodHook() {
                @Override
                protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                    Object ResourcesImpl = param.thisObject;
                    if(assetManager != null){
                        ReflectUtils.setField(ResourcesImpl, "mAssets", assetManager);
                    }

                    super.beforeHookedMethod(param);
                }
            });

        }catch (Exception e){
            lg.e("hookRec: "+e.getMessage());
        }
    }


    @SuppressLint("SoonBlockedPrivateApi")
    public synchronized static void preloadResource(Context context, String apkFilePath) {
        try {
            // 先创建AssetManager
            Class<? extends AssetManager> AssetManagerClass = AssetManager.class;
            assetManager = AssetManagerClass.newInstance();
            // 将插件资源和宿主资源通过 addAssetPath方法添加进去
            Method addAssetPathMethod = AssetManagerClass.getDeclaredMethod("addAssetPath", String.class);
            addAssetPathMethod.setAccessible(true);
            String hostResourcePath = context.getPackageResourcePath();
            int result_2 = (int) addAssetPathMethod.invoke(assetManager, apkFilePath);
            apkResources = new Resources(assetManager, context.getResources().getDisplayMetrics(), context.getResources().getConfiguration());

//            int result_1 = (int) addAssetPathMethod.invoke(assetManager, hostResourcePath);


            // 接下来创建，合并资源后的Resource
            Resources resources = new Resources(assetManager, context.getResources().getDisplayMetrics(), context.getResources().getConfiguration());
            // 替换 ContextImpl 中Resource对象
            Class<?> contextImplClass = context.getClass();
            Field resourcesField1 = contextImplClass.getDeclaredField("mResources");
            resourcesField1.setAccessible(true);
            resourcesField1.set(context, resources);
            // 先获取到LoadApk对象
            Field loadedApkField = contextImplClass.getDeclaredField("mPackageInfo");
            loadedApkField.setAccessible(true);
            Object loadApk = loadedApkField.get(context);
            Class<?> loadApkClass = loadApk.getClass();
            // 替换掉LoadApk中的Resource对象。
            Field resourcesField2 = loadApkClass.getDeclaredField("mResources");
            resourcesField2.setAccessible(true);
            resourcesField2.set(loadApk, resources);

            //获取到ActivityThread
            Class<?> ActivityThreadClass = Class.forName("android.app.ActivityThread");
            Field sCurrentActivityThreadField = ActivityThreadClass.getDeclaredField("sCurrentActivityThread");
            sCurrentActivityThreadField.setAccessible(true);
            Object ActivityThread = sCurrentActivityThreadField.get(null);
            // 获取到ResourceManager对象
            Field ResourcesManagerField = ActivityThreadClass.getDeclaredField("mResourcesManager");
            ResourcesManagerField.setAccessible(true);
            Object resourcesManager = ResourcesManagerField.get(ActivityThread);
            // 替换掉ResourceManager中resource对象
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N) {
                Class<?> resourcesManagerClass = resourcesManager.getClass();
                Field mActiveResourcesField = resourcesManagerClass.getDeclaredField("mActiveResources");
                mActiveResourcesField.setAccessible(true);
                Map<Object, WeakReference<Resources>> map = (Map<Object, WeakReference<Resources>>) mActiveResourcesField.get(resourcesManager);
                Object key = map.keySet().iterator().next();
                map.put(key, new WeakReference<>(resources));
            } else {
                // still hook Android N Resources, even though it's unnecessary, then nobody will be strange.
                Class<?> resourcesManagerClass = resourcesManager.getClass();
                Field mResourceImplsField = resourcesManagerClass.getDeclaredField("mResourceImpls");
                mResourceImplsField.setAccessible(true);
                Map map = (Map) mResourceImplsField.get(resourcesManager);
                Object key = map.keySet().iterator().next();
                Field mResourcesImplField = Resources.class.getDeclaredField("mResourcesImpl");
                mResourcesImplField.setAccessible(true);
                Object resourcesImpl = mResourcesImplField.get(resources);
                map.put(key, new WeakReference<>(resourcesImpl));
            }
            multiResources = resources;
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
