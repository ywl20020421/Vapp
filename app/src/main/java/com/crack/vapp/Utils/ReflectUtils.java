package com.crack.vapp.Utils;

import android.util.Log;

import java.lang.reflect.*;
import java.util.*;

public class ReflectUtils {
    private static final String TAG = "ReflectUtils";

    /**
     * 加载类
     * @param className 全限定类名
     * @return Class对象
     */
    public static Class<?> loadClass(String className) {
        try {
            return Class.forName(className);
        } catch (ClassNotFoundException e) {
            Log.e(TAG, "Class not found: " + className, e);
            return null;}
    }

    /**
     * 设置字段值（包括私有字段）
     * @param obj 目标对象
     * @param fieldName 字段名
     * @param value 要设置的值
     */
    public static void setField(Object obj, String fieldName, Object value) {
        try {
            Field field = getField(obj.getClass(), fieldName);
            field.set(obj, value);
        } catch (IllegalAccessException e) {
            Log.e(TAG, "Set field failed: " + fieldName, e);
        }
    }

    /**
     * 获取字段值（包括私有字段）
     * @param obj 目标对象
     * @param fieldName 字段名
     * @return 字段值
     */
    public static Object getField(Object obj, String fieldName) {
        try {
            Field field = getField(obj.getClass(), fieldName);
            return field.get(obj);
        } catch (IllegalAccessException e) {
            Log.e(TAG, "Get field failed: " + fieldName, e);
            return null;
        }
    }
    /**
     * 递归获取字段（包含父类字段）
     */
    private static Field getField(Class<?> clazz, String fieldName) {
        try {
            Field field = clazz.getDeclaredField(fieldName);
            field.setAccessible(true);
            return field;
        } catch (NoSuchFieldException e) {
            if (clazz.getSuperclass() != null) {
                return getField(clazz.getSuperclass(), fieldName);
            }
            Log.e(TAG, "Field not found: " + fieldName, e);
            return null;
        }
    }

    /**
     * 获取方法（包括私有方法）
     * @param clazz 类对象
     * @param methodName 方法名
     * @param parameterTypes 参数类型列表
     * @return Method对象
     */
    public static Method getMethod(Class<?> clazz, String methodName, Class<?>... parameterTypes) {
        try {
            Method method = clazz.getDeclaredMethod(methodName, parameterTypes);
            method.setAccessible(true);
            return method;
        } catch (NoSuchMethodException e) {
            Log.e(TAG, "Method not found: " + methodName, e);
            return null;
        }
    }

    /**
     * 调用方法
     * @param obj 目标对象
     * @param method 要调用的方法
     * @param args 方法参数
     * @return 方法执行结果
     */
    public static Object invokeMethod(Object obj, Method method, Object... args) {
        try {
            return method.invoke(obj, args);
        } catch (IllegalAccessException | InvocationTargetException e) {
            Log.e(TAG, "Invoke method failed: " + method.getName(), e);
            return null;
        }
    }

    /**
     * 打印对象所有字段的值
     * @param obj 目标对象
     * @return 字段值的字符串表示
     */
 private static String printClassFieldValues(Object obj) {
    Class<?> clazz = obj.getClass();
    StringBuilder sb = new StringBuilder();

    sb.append("Field values of ").append(clazz.getSimpleName()).append(":\n");

    // 遍历类继承链
    while (clazz != null && clazz != Object.class) {
        sb.append("\n[").append(clazz.getSimpleName()).append("]\n");

        // 打印当前类的字段
        for (Field field : clazz.getDeclaredFields()) {
            if (Modifier.isStatic(field.getModifiers())) {
                continue; // 跳过静态字段
            }
            try {
                field.setAccessible(true);
                sb.append("  ")
                  .append(field.getType().getSimpleName())
                  .append(" ")
                  .append(field.getName())
                  .append(" = ")
                  .append(field.get(obj))
                  .append("\n");
            } catch (IllegalAccessException e) {
                sb.append("  ")
                  .append(field.getName())
                  .append(" = [access denied]\n");
            }
        }

        clazz = clazz.getSuperclass();
    }

    return sb.toString();
}


    public static void printFieldValues(Object obj){
        try {
            Log.d(TAG, "printFieldValues: "+printClassFieldValues(obj));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
