package com.crack.vapp.core;

import static com.crack.vapp.BaseData.baseActivity;
import static com.crack.vapp.BaseData.context;
import static com.crack.vapp.core.lunchApplication.lunch;

import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.util.Log;

import com.crack.vapp.BaseData;
import com.crack.vapp.Utils.AddDex;
import com.crack.vapp.Utils.AddPluginResources;
import com.crack.vapp.Utils.PackageParser;

import java.io.File;

public class handle {
    private static final String TAG = "handle";

    public static void begin(ApplicationInfo appInfo){
        // 获取 APK 文件路径
        String apkPath = appInfo.sourceDir;
        File apkFile = new File(apkPath);

        // 创建目标目录：自身目录 + app包名  工作目录
        String packageName = appInfo.packageName;
        File targetDir = new File(baseActivity.getFilesDir(), packageName);

        //获取 app的info
        BaseData.packageInfo = PackageParser.getPackageInfo(apkPath, context);

        String launcherActivityName = PackageParser.getLuncherActivityName(targetDir);

        Log.d(TAG, "完成 解析apk 信息 " + BaseData.packageInfo.packageName + "  lunch activity :"+ launcherActivityName);

        //hook so load
        HookSoLoad.hook(targetDir);

        //开始 hook activity
        HookActivty.hook();
        // hook service
        HookService.hook();

        // hook broadcast

        // hook contentProvider

        //添加资源
        AddPluginResources.preloadResource(context, apkPath);
        AddPluginResources.hookRec();

        //添加 dex
//        AddDex.addPluginApkDex(apkFile);
        AddDex.addDexs(apkFile);

        //初始化 application
//        lunchApplication.lunch(BaseData.packageInfo.applicationInfo.name);

        //启动activity
        Intent intent = null;
        try {
            intent = new Intent(baseActivity, baseActivity.getClassLoader().loadClass(launcherActivityName));
//            intent.putExtra("isFirst", true);
            baseActivity.startActivity(intent);
            baseActivity.finish();
        } catch (ClassNotFoundException e) {
            throw new RuntimeException(e);
        }



    }
}
