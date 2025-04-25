package com.crack.vapp.Utils;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public class FileUtil {

    /**
     * 解压 APK 文件到指定目录
     * @param apkFile APK 文件
     * @param targetDir 目标目录
     */
    public static void unzipApk(File apkFile, File targetDir) throws Exception {
        try (ZipInputStream zipInputStream = new ZipInputStream(new FileInputStream(apkFile))) {
            ZipEntry zipEntry;
            while ((zipEntry = zipInputStream.getNextEntry()) != null) {
                File file = new File(targetDir, zipEntry.getName());
                lg.d("解压文件：" + file.getAbsolutePath());
                if (zipEntry.isDirectory() ||zipEntry.getName().equals("") ) {
                    // 如果是目录，确保目录存在
                    file.mkdirs();
                } else {
                    // 确保父文件夹存在
                    if (!file.getParentFile().exists()) {
                        file.getParentFile().mkdirs();
                    }
                    try (FileOutputStream fileOutputStream = new FileOutputStream(file)) {
                        byte[] buffer = new byte[1024];
                        int length;
                        while ((length = zipInputStream.read(buffer)) > 0) {
                            fileOutputStream.write(buffer, 0, length);
                        }
                    }
                }
                zipInputStream.closeEntry();
            }
        }
    }

    // 新增方法：递归删除目录及其内容
    public static void deleteDirectory(File directory) {
        if (directory.exists()) {
            File[] files = directory.listFiles();
            if (files != null) {
                for (File file : files) {
                    if (file.isDirectory()) {
                        deleteDirectory(file);
                    } else {
                        file.delete();
                    }
                }
            }
            directory.delete();
        }
    }
}
