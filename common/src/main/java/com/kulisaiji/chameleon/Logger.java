package com.kulisaiji.chameleon;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.concurrent.ConcurrentLinkedQueue;

public class Logger {
    
    public enum Level {
        INFO,
        WARN,
        DEBUG,
        ERROR
    }
    
    private static Level currentLevel = Level.INFO;
    private static Path logPath;
    private static BufferedWriter writer;
    private static final ConcurrentLinkedQueue<String> buffer = new ConcurrentLinkedQueue<>();
    private static boolean initialized = false;
    private static String systemLanguage;
    
    private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("HH:mm:ss");
    
    /**
     * 检测系统语言
     */
    public static String detectSystemLanguage() {
        Locale locale = Locale.getDefault();
        String lang = locale.getLanguage();
        String country = locale.getCountry();
        
        if ("zh".equals(lang)) {
            if ("CN".equals(country)) {
                systemLanguage = "zh_CN";
            } else if ("TW".equals(country)) {
                systemLanguage = "zh_TW";
            } else {
                systemLanguage = "zh_CN";
            }
        } else {
            systemLanguage = lang + "_" + country;
        }
        
        return systemLanguage;
    }
    
    /**
     * 初始化日志系统
     */
    public static void initialize(Level level) {
        if (initialized) {
            return;
        }
        
        currentLevel = level;
        
        try {
            Path logsDir = Paths.get("logs");
            if (!Files.exists(logsDir)) {
                Files.createDirectories(logsDir);
            }
            
            logPath = logsDir.resolve("chameleon.log");
            
            // 如果文件存在，先读取现有内容
            StringBuilder existingContent = new StringBuilder();
            if (Files.exists(logPath)) {
                existingContent.append(Files.readString(logPath));
            }
            
            writer = Files.newBufferedWriter(
                logPath,
                StandardOpenOption.CREATE,
                StandardOpenOption.TRUNCATE_EXISTING
            );
            
            // 写入系统语言信息
            String lang = detectSystemLanguage();
            boolean isChinese = systemLanguage.startsWith("zh");
            
            String header;
            if (isChinese) {
                header = String.format(
                    "═══════════════════════════════════════════════════════════%n" +
                    "  Chameleon 模组管理器 - 启动日志%n" +
                    "  系统语言：%s (%s)%n" +
                    "  日志级别：%s%n" +
                    "  启动时间：%s%n" +
                    "═══════════════════════════════════════════════════════════%n",
                    systemLanguage,
                    isChinese ? "将输出中文日志" : "Will output English logs",
                    currentLevel,
                    LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)
                );
            } else {
                header = String.format(
                    "═══════════════════════════════════════════════════════════%n" +
                    "  Chameleon Mod Manager - Startup Log%n" +
                    "  System Language: %s (%s)%n" +
                    "  Log Level: %s%n" +
                    "  Startup Time: %s%n" +
                    "═══════════════════════════════════════════════════════════%n",
                    systemLanguage,
                    isChinese ? "将输出中文日志" : "Will output English logs",
                    currentLevel,
                    LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)
                );
            }
            
            writer.write(header);
            writer.flush();
            
            initialized = true;
            
            info("Logger initialized");
            
        } catch (IOException e) {
            System.err.println("[Chameleon] Failed to initialize logger: " + e.getMessage());
            initialized = false;
        }
    }
    
    /**
     * 记录日志
     */
    public static void log(Level level, String message) {
        if (level.ordinal() < currentLevel.ordinal()) {
            return;
        }
        
        String timestamp = LocalDateTime.now().format(TIME_FORMAT);
        String levelStr = String.format("%-5s", level);
        String logLine = String.format("[%s] [%s] %s", timestamp, levelStr, message);
        
        // 添加到缓冲区
        buffer.offer(logLine);
        
        // 输出到控制台
        if (level == Level.ERROR) {
            System.err.println(logLine);
        } else {
            System.out.println(logLine);
        }
        
        // 写入文件
        if (initialized && writer != null) {
            try {
                writer.write(logLine);
                writer.newLine();
                writer.flush();
            } catch (IOException e) {
                System.err.println("[Chameleon] Failed to write to log file: " + e.getMessage());
            }
        }
    }
    
    /**
     * 记录 INFO 级别日志
     */
    public static void info(String message) {
        log(Level.INFO, message);
    }
    
    /**
     * 记录 WARN 级别日志
     */
    public static void warn(String message) {
        log(Level.WARN, message);
    }
    
    /**
     * 记录 DEBUG 级别日志
     */
    public static void debug(String message) {
        log(Level.DEBUG, message);
    }
    
    /**
     * 记录 ERROR 级别日志
     */
    public static void error(String message) {
        log(Level.ERROR, message);
    }
    
    /**
     * 记录 ERROR 级别日志（带异常）
     */
    public static void error(String message, Throwable throwable) {
        log(Level.ERROR, message);
        if (initialized && writer != null) {
            try {
                throwable.printStackTrace(new java.io.PrintWriter(writer));
                writer.flush();
            } catch (IOException e) {
                System.err.println("[Chameleon] Failed to write exception to log: " + e.getMessage());
            }
        }
        throwable.printStackTrace(System.err);
    }
    
    /**
     * 获取当前日志级别
     */
    public static Level getCurrentLevel() {
        return currentLevel;
    }
    
    /**
     * 设置日志级别
     */
    public static void setLevel(Level level) {
        currentLevel = level;
        info("Log level changed to: " + level);
    }
    
    /**
     * 获取系统语言设置
     */
    public static String getSystemLanguage() {
        return systemLanguage;
    }
    
    /**
     * 判断是否为中文环境
     */
    public static boolean isChinese() {
        return systemLanguage != null && systemLanguage.startsWith("zh");
    }
    
    /**
     * 关闭日志系统
     */
    public static void shutdown() {
        if (writer != null) {
            try {
                writer.close();
            } catch (IOException e) {
                System.err.println("[Chameleon] Failed to close log file: " + e.getMessage());
            }
        }
        initialized = false;
    }
}
