package com.kulisaiji.chameleon;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Locale;

public class EnvironmentDetector {
    
    /**
     * 获取当前设备类型
     */
    public static String getDevice() {
        String device = detectLauncher();
        System.out.println("[Chameleon] 设备检测: " + device);
        return device;
    }

    /**
     * 识别当前运行的启动器
     * 采用"类名 + 系统属性 + 文件特征"多重检测
     */
    private static String detectLauncher() {
        // 优先检查系统属性（最可靠）
        String launcherProp = System.getProperty("chameleon.launcher");
        if (launcherProp != null && !launcherProp.isEmpty()) {
            return launcherProp;
        }
        
        // 检查是否在容器中运行
        if (isRunningInContainer()) {
            return "container";
        }

        // --- Android 启动器检测 ---
        String androidLauncher = detectAndroidLauncher();
        if (androidLauncher != null) {
            System.setProperty("chameleon.launcher", androidLauncher);
            return androidLauncher;
        }

        // --- 桌面操作系统检测 ---
        String os = System.getProperty("os.name");
        if (os == null) {
            System.out.println("[Chameleon] 警告: 无法获取操作系统信息");
            return "unknown";
        }
        
        os = os.toLowerCase(Locale.ROOT);
        String device;
        if (os.contains("win")) {
            device = "windows";
        } else if (os.contains("mac")) {
            device = "mac";
        } else if (os.contains("linux")) {
            device = "linux";
        } else if (os.contains("unix")) {
            device = "unix";
        } else {
            device = "unknown";
        }
        
        System.setProperty("chameleon.launcher", device);
        return device;
    }
    
    /**
     * 检测 Android 启动器
     */
    private static String detectAndroidLauncher() {
        // PojavLauncher
        if (isClassExists("net.kdt.pojavlaunch.Tools") ||
            isClassExists("org.pojavlauncher.PojavLauncher") ||
            System.getProperty("pojav.launcher") != null) {
            System.out.println("[Chameleon] 检测到启动器: PojavLauncher");
            return "android";
        }

        // Fold Craft Launcher (FCL)
        if (isClassExists("com.hyplant.fcl.Launcher") ||
            isClassExists("com.hyplant.fcl.BuildConfig") ||
            System.getProperty("fcl.launcher") != null) {
            System.out.println("[Chameleon] 检测到启动器: Fold Craft Launcher");
            return "android";
        }

        // Zalith Launcher
        if (isClassExists("com.zalith.launcher.ZalithLauncher") ||
            isClassExists("com.zalith.launcher.BuildConfig")) {
            System.out.println("[Chameleon] 检测到启动器: Zalith Launcher");
            return "android";
        }

        // Boat & MCinaBox
        if (isClassExists("cosine.math.BoatLauncher") ||
            isClassExists("com.aof.mcinabox.MainActivity") ||
            isClassExists("com.aof.mcinabox.BuildConfig")) {
            System.out.println("[Chameleon] 检测到启动器: Boat/MCinaBox");
            return "android";
        }

        // HMCL-PE
        if (isClassExists("com.tungsten.hmclpe.HMCLPE") ||
            isClassExists("com.tungsten.hmclpe.BuildConfig")) {
            System.out.println("[Chameleon] 检测到启动器: HMCL-PE");
            return "android";
        }

        // 通用 Android 运行时检测
        if (isAndroidRuntime()) {
            System.out.println("[Chameleon] 检测到 Android 运行时环境");
            return "android";
        }
        
        return null;
    }
    
    /**
     * 检测是否在容器中运行
     */
    private static boolean isRunningInContainer() {
        // 检查 Docker 环境
        if (Files.exists(Paths.get("/.dockerenv"))) {
            return true;
        }
        
        // 检查容器内的 cgroup
        try {
            String cgroup = Files.readString(Paths.get("/proc/1/cgroup"));
            if (cgroup.contains("docker") || cgroup.contains("kubepods")) {
                return true;
            }
        } catch (Exception ignored) {}
        
        return false;
    }

    /**
     * 判断是否运行在 Android 运行时环境（ART/Dalvik）
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
     * 获取运行环境类型（客户端/服务端）
     */
    public static String getRuntime() {
        // 优先检查平台已注入的属性
        String prop = System.getProperty("chameleon.runtime");
        if ("client".equals(prop) || "server".equals(prop)) {
            System.out.println("[Chameleon] 运行环境: " + prop + " (来自系统属性)");
            return prop;
        }

        // 通过类存在性判断
        try {
            Class.forName("net.minecraft.client.main.Main");
            System.out.println("[Chameleon] 运行环境: client (检测到客户端主类)");
            System.setProperty("chameleon.runtime", "client");
            return "client";
        } catch (ClassNotFoundException e) {
            System.out.println("[Chameleon] 运行环境: server (未检测到客户端主类)");
            System.setProperty("chameleon.runtime", "server");
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
