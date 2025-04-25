package com.crack.vapp.Utils;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.util.Log;

import com.crack.vapp.BaseData;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import java.io.File;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

import axml.xml.AxmlUtil;

public class PackageParser {

    static String TAG = "PackageParser";
    public static PackageInfo getPackageInfo(String apkPath, Context context){

        Class<?> PackageParser = ReflectUtils.loadClass("android.content.pm.PackageParser");
        Method parsePackage = ReflectUtils.getMethod(PackageParser, "parsePackage", File.class, int.class, boolean.class);
        if(PackageParser == null){
            Log.e(TAG, "请开启 去除反射限制 getPackageInfo: PackageParser == null");
            return null;
        }
        try {
            Object o = PackageParser.newInstance();
            Object Package = parsePackage.invoke(o, new File(apkPath), 0, false);
            BaseData.Package = Package;
            if (Package != null) {
                Log.d("hook", "Package: " + Package);

                Class<?> ApexInfo = ReflectUtils.loadClass("android.apex.ApexInfo");

                Method generatePackageInfo1 = ReflectUtils.getMethod(PackageParser, "generatePackageInfo",
                        Package.getClass(),
                        ApexInfo,
                        int.class
                );
                int flags = 1048575;
                Log.d(TAG, "getPackageInfo: flags : "+ flags);
                PackageInfo packageInfo = (PackageInfo)generatePackageInfo1.invoke(o, Package, null, flags);

                if(packageInfo != null){
                    Log.d(TAG, "packageInfo: " + packageInfo);
                    return packageInfo;
                }
            }
        } catch (Exception e) {
//            throw new RuntimeException(e);
            Log.e(TAG, "getPackageInfo: 返回null " );
            return null;
        }
        Log.e(TAG, "getPackageInfo: 返回null " );
        return null;
    }

    public static String getLuncherActivityName(File dir){
        File axml = new File(dir, "AndroidManifest.xml");
        File xml = new File(dir, "Manifest.xml");
        try {
//                Main.main(new String[]{"d", axml.getAbsolutePath(),xml.getAbsolutePath()});
            AxmlUtil.decode(axml.getAbsolutePath(), xml.getAbsolutePath());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        List<String> activityNames = new ArrayList<>();
        try {
            AndroidManifestParser parser = new AndroidManifestParser();
            Document doc = parser.parseXmlFile(xml);
            // 获取所有启动Activity元素
            List<Element> activities = parser.getLauncherActivities(doc);
            Log.d(TAG,"Found " + activities.size() + " launcher activities");
            // 获取所有启动Activity名称
            activityNames = parser.getLauncherActivityNames(doc);
            Log.d(TAG,"Launcher Activity Names: " + activityNames);
            return  activityNames.get(0);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;

    }


}
