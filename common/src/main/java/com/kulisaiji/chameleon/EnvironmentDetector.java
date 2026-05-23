package com.kulisaiji.chameleon;

import java.util.Locale;

public class EnvironmentDetector {
    public static String getDevice() {
        return detectLauncher();
    }

    /**
     * 识别当前运行的 Android 启动器，支持向下迁移。
     * 采用"类名 + 系统属性"双重检测，涵盖主流的 Android 启动器。
     */
    private static String detectLauncher() {
        // --- PojavLauncher & 基于 Pojav 的衍生启动器 ---
        if (isClassExists("net.kdt.pojavlaunch.Tools") ||
            isClassExists("org.pojavlauncher.PojavLauncher") ||
            System.getProperty("pojav.launcher") != null) {
            return "android";
        }

        // Fold Craft Launcher (FCL)
        if (isClassExists("com.hyplant.fcl.Launcher") ||
            isClassExists("com.hyplant.fcl.BuildConfig") ||
            System.getProperty("fcl.launcher") != null) {
            return "android";
        }

        // Zalith Launcher
        if (isClassExists("com.zalith.launcher.ZalithLauncher") ||
            isClassExists("com.zalith.launcher.BuildConfig")) {
            return "android";
        }

        // Boat & MCinaBox
        if (isClassExists("cosine.math.BoatLauncher") ||
            isClassExists("com.aof.mcinabox.MainActivity") ||
            isClassExists("com.aof.mcinabox.BuildConfig")) {
            return "android";
        }

        // HMCL-PE
        if (isClassExists("com.tungsten.hmclpe.HMCLPE") ||
            isClassExists("com.tungsten.hmclpe.BuildConfig")) {
            return "android";
        }

        // 通用 Android 环境回退检查
        if (isAndroidRuntime()) {
            return "android";
        }

        // --- 桌面操作系统检测 ---
        String os = System.getProperty("os.name");
        if (os == null) {
            return "unknown";
        }
        os = os.toLowerCase(Locale.ROOT);
        if (os.contains("win")) {
            return "windows";
        }
        if (os.contains("mac")) {
            return "mac";
        }
        if (os.contains("linux") || os.contains("unix")) {
            return "linux";
        }
        return "unknown";
    }

    /**
     * 判断是否运行在 Android 运行时环境（ART/Dalvik）。
     * 作为所有 Android 启动器的通用回退检测。
     */
    private static boolean isAndroidRuntime() {
        try {
            Class.forName("android.os.Build");
            return true;
        } catch (ClassNotFoundException e) {
            return false;
        }
    }

    /**
     * [修正] 在极早期阶段（如 SetupModService），Minecraft 主类可能尚未进入 Classpath。
     * 因此增加系统属性回退判断，供平台适配层在更早时机注入。
     */
    public static String getRuntime() {
        // 优先检查平台已注入的属性（由 NeoForge/Fabric/Quilt 在更早期设置）
        String prop = System.getProperty("chameleon.runtime");
        if ("client".equals(prop) || "server".equals(prop)) {
            return prop;
        }

        try {
            Class.forName("net.minecraft.client.main.Main");
            return "client";
        } catch (ClassNotFoundException e) {
            return "server";
        }
    }

    private static boolean isClassExists(String className) {
        try {
            Class.forName(className);
            return true;
        } catch (ClassNotFoundException e) {
            return false;
        }
    }
}
