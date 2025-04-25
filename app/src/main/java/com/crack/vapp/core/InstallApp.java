package com.crack.vapp.core;

import static com.crack.vapp.Utils.FileUtil.deleteDirectory;
import static com.crack.vapp.Utils.FileUtil.unzipApk;

import android.content.Context;
import android.content.pm.ApplicationInfo;

import com.crack.vapp.Utils.lg;

import java.io.File;


public class InstallApp {

    /**
     * 安装逻辑：将 APK 包解压到自身目录下的 app包名 目录中
     * @param context 上下文
     * @param appInfo 应用信息
     */
    public static void installApp(Context context, ApplicationInfo appInfo) {
        try {
            // 获取 APK 文件路径
            String apkPath = appInfo.sourceDir;
            File apkFile = new File(apkPath);

            // 创建目标目录：自身目录 + app包名
            String packageName = appInfo.packageName;
            File targetDir = new File(context.getFilesDir(), packageName);

            // 如果目标目录已存在，先删除
            if (targetDir.exists()) {
                deleteDirectory(targetDir);
            }

            // 确保父文件夹存在
            if (!targetDir.getParentFile().exists()) {
                targetDir.getParentFile().mkdirs();
            }
            targetDir.mkdirs();

            // 解压 APK 文件到目标目录
            unzipApk(apkFile, targetDir);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


}