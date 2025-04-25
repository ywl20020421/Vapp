package com.crack.vapp.Utils;

import static com.crack.vapp.BaseData.context;

import android.app.Activity;
import android.content.Context;
import android.util.Log;

import com.crack.vapp.BaseData;

import java.io.File;
import java.io.FilenameFilter;
import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.util.ArrayList;

import dalvik.system.DexClassLoader;

public class AddDex {
    static final String TAG = "AddDex";

    public static void addPluginApkDex(File apk) {

        Context context = BaseData.baseActivity;

        // 优化后的输出目录
        File optimizedDir = context.getDir("oapk", Context.MODE_PRIVATE);
        DexClassLoader dexClassLoader = new DexClassLoader(
                apk.getAbsolutePath(), // 替换为 APK 文件路径
                optimizedDir.getAbsolutePath(),
                null,
                context.getClassLoader()
        );
        Field Field_parent;
        try {
            Field_parent = ClassLoader.class.getDeclaredField("parent");
            Field_parent.setAccessible(true);
            Object parent = Field_parent.get(context.getClassLoader());

            Field_parent.set(context.getClassLoader(), dexClassLoader);
            Field_parent.set(dexClassLoader,parent );
        } catch (Exception e) {
            Log.e(TAG, "addPluginApkDex: ", e);
        }
        Class<?> aClass = null;
        try {
            aClass = context.getClassLoader().loadClass("com.crack.testdemo.MainActivity");
        } catch (ClassNotFoundException e) {

        }
        Log.d(TAG, "addPluginApkDex: "+aClass);

        //我认为这样做 可以优先加载 插件的 类 这个样 在插件中 就不会出现与 宿主app 拥有相同的sdk 冲突了
    }

    public static void addDexs(File apk) {
        //合并 dex 文件
//        File dir = new File(context.getFilesDir(), appItem.getPackageName());
//        File[] dex_s = dir.listFiles(new FilenameFilter() {
//            @Override
//            public boolean accept(File dir, String name) {
//                return name.contains(".dex");
//            }
//        });
        // 优化后的输出目录
        File optimizedDir = context.getDir("odex", Context.MODE_PRIVATE);

        ArrayList<Object[]> allElements = new ArrayList<>();
        int ElementsLen = 0;
        Field pathList = null;
//        for (File dex : dex_s) {
            // 创建DexClassLoader
            DexClassLoader dexClassLoader = new DexClassLoader(
                    apk.getAbsolutePath(),
                    optimizedDir.getAbsolutePath(),
                    null,
                    context.getClassLoader()
            );
            // 获取BaseDexClassLoader中的DexPathList字段
            Class<?> aClass = ReflectUtils.loadClass("dalvik.system.BaseDexClassLoader");

            try {
                pathList = aClass.getDeclaredField("pathList");
            } catch (NoSuchFieldException e) {
                throw new RuntimeException(e);
            }
            pathList.setAccessible(true);
            Object pathList_obj = null;
            try {
                pathList_obj = pathList.get(dexClassLoader);
            } catch (IllegalAccessException e) {
                throw new RuntimeException(e);
            }
            Object[] dexElementsa = (Object[])ReflectUtils.getField(pathList_obj, "dexElements");

            allElements.add(dexElementsa);
            ElementsLen += dexElementsa.length;
//        }
        //加上原本的
        ClassLoader oriclassLoader = context.getClassLoader();
//        Object pathList_obj =null;
        try {
            pathList_obj = pathList.get(oriclassLoader);
        } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        }

        Object[] dexElements = (Object[])ReflectUtils.getField(pathList_obj, "dexElements");
        //app 自身的dex
        allElements.add(dexElements);
        ElementsLen += dexElements.length;

        Object[] NewDexElements =(Object[]) Array.newInstance(
                dexElements.getClass().getComponentType(),
                ElementsLen
        );
        int index = 0;

        for (Object[] allElement : allElements) {
            System.arraycopy(allElement, 0, NewDexElements, index, allElement.length);
            index += allElement.length;
        }
        //添加完毕 开始设置 变量
        try {
            Field dexElements1 = pathList_obj.getClass().getDeclaredField("dexElements");
            dexElements1.setAccessible(true);
            dexElements1.set(pathList_obj, NewDexElements);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        Log.d(TAG, "handle: dexElements 变量设置完毕！");
    }

}
