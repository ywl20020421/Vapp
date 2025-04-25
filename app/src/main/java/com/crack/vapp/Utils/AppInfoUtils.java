package com.crack.vapp.Utils;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.List;

public class AppInfoUtils {

    private static final String FILE_NAME = "string_list.dat";

    // 获取设备上安装的所有非系统应用程序的 ApplicationInfo 集合
    public static List<ApplicationInfo> getAllNonSystemApps(Context context) {
        PackageManager packageManager = context.getPackageManager();
        List<ApplicationInfo> allApps = packageManager.getInstalledApplications(PackageManager.GET_META_DATA);

        List<ApplicationInfo> nonSystemApps = new ArrayList<>();
        for (ApplicationInfo appInfo : allApps) {
            // 检查是否为非系统应用
            if ((appInfo.flags & ApplicationInfo.FLAG_SYSTEM) == 0) {
                nonSystemApps.add(appInfo);
            }
        }
        return nonSystemApps;
    }

    // 获取已保存包名对应的 ApplicationInfo 集合
    public static List<ApplicationInfo> getSavedAppInfos(Context context) {
        // 加载已保存的包名集合
        List<String> savedPackageNames = getAllStrings(context);
        if (savedPackageNames == null || savedPackageNames.isEmpty()) {
            return new ArrayList<>(); // 如果没有保存的包名，返回空列表
        }

        // 获取所有已安装应用的 ApplicationInfo 列表
        PackageManager packageManager = context.getPackageManager();
        List<ApplicationInfo> allApps = packageManager.getInstalledApplications(PackageManager.GET_META_DATA);

        // 过滤出已保存包名对应的 ApplicationInfo
        List<ApplicationInfo> savedAppInfos = new ArrayList<>();
        for (ApplicationInfo appInfo : allApps) {
            if (savedPackageNames.contains(appInfo.packageName)) {
                savedAppInfos.add(appInfo);
            }
        }
        return savedAppInfos;
    }

    // 方法1：保存单个字符串到文件，确保不重复
    public static void saveString(Context context, String str) {
        List<String> stringList = loadStringList(context);
        if (stringList == null) {
            stringList = new ArrayList<>();
        }
        // 如果集合中不存在该字符串，则添加并保存
        if (!stringList.contains(str)) {
            stringList.add(str);
            saveStringList(context, stringList);
        }
    }

    // 方法2：从文件中读取所有保存的字符串
    public static List<String> getAllStrings(Context context) {
        List<String> stringList = loadStringList(context);
        return stringList != null ? stringList : new ArrayList<>();
    }

    // 辅助方法：保存字符串列表到文件
    private static void saveStringList(Context context, List<String> stringList) {
        try (FileOutputStream fos = context.openFileOutput(FILE_NAME, Context.MODE_PRIVATE);
             ObjectOutputStream oos = new ObjectOutputStream(fos)) {
            oos.writeObject(stringList);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // 辅助方法：从文件中加载字符串列表
    private static List<String> loadStringList(Context context) {
        File file = new File(context.getFilesDir(), FILE_NAME);
        if (!file.exists()) {
            // 初始化默认数据
            List<String> defaultList = new ArrayList<>();
//            defaultList.add("com.crack.vapp");
            return defaultList;
        }

        try (FileInputStream fis = context.openFileInput(FILE_NAME);
             ObjectInputStream ois = new ObjectInputStream(fis)) {
            return (List<String>) ois.readObject();
        } catch (Exception e) {
            e.printStackTrace();
            return new ArrayList<>();
        }
    }
}
