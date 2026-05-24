package com.kulisaiji.chameleon;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

public class ConfigLoader {
    private static final Path CONFIG_PATH = Paths.get("config", "chameleon_config.json");
    private JsonObject config;
    private final Gson gson;
    
    // 新增配置字段
    private String logLevel;
    private Boolean enableCommands;
    private List<VersionMatcher.VersionConstraint> constraints;
    private List<PatternRule> regexPatterns;
    
    /**
     * 正则规则，支持 r: 前缀
     */
    public static class PatternRule {
        public final String pattern;
        public final Pattern compiled;
        public final boolean isValid;
        
        public PatternRule(String pattern, Pattern compiled, boolean isValid) {
            this.pattern = pattern;
            this.compiled = compiled;
            this.isValid = isValid;
        }
        
        public static PatternRule fromString(String rule) {
            if (rule == null || rule.trim().isEmpty()) {
                return new PatternRule(rule, null, false);
            }
            
            String trimmed = rule.trim();
            if (trimmed.startsWith("r:")) {
                // 正则表达式
                String regex = trimmed.substring(2).trim();
                try {
                    Pattern compiled = Pattern.compile(regex, Pattern.CASE_INSENSITIVE);
                    return new PatternRule(regex, compiled, true);
                } catch (Exception e) {
                    Logger.warn("正则语法无效，已跳过：r:" + regex + " - " + e.getMessage());
                    return new PatternRule(regex, null, false);
                }
            } else {
                // 普通规则（ModID 或文件名）
                return new PatternRule(trimmed, null, true);
            }
        }
        
        public boolean matches(String text) {
            if (!isValid || compiled == null) {
                return false;
            }
            return compiled.matcher(text).find();
        }
    }
    
    public ConfigLoader() {
        this.gson = new GsonBuilder().setPrettyPrinting().create();
    }
    
    public void loadOrCreate() {
        if (!Files.exists(CONFIG_PATH)) {
            createDefault();
        }
        try (Reader reader = Files.newBufferedReader(CONFIG_PATH)) {
            config = JsonParser.parseReader(reader).getAsJsonObject();
            loadAdvancedConfig();
        } catch (IOException e) {
            Logger.error("读取配置文件失败：" + e.getMessage());
            config = new JsonObject();
        }
    }
    
    private void loadAdvancedConfig() {
        if (config == null) {
            return;
        }
        
        // 加载日志级别
        logLevel = config.has("log_level") ? config.get("log_level").getAsString() : "INFO";
        
        // 加载命令开关
        enableCommands = config.has("enable_commands") && config.get("enable_commands").getAsBoolean();
        
        // 加载版本约束
        constraints = new ArrayList<>();
        if (config.has("version_constraints")) {
            for (JsonElement e : config.getAsJsonArray("version_constraints")) {
                String rule = e.getAsString();
                VersionMatcher.VersionConstraint vc = VersionMatcher.parseConstraint(rule);
                if (vc != null) {
                    constraints.add(vc);
                }
            }
        }
        
        // 加载正则规则
        regexPatterns = new ArrayList<>();
        if (config.has("rules")) {
            for (JsonElement e : config.getAsJsonArray("rules")) {
                String rule = e.getAsString();
                PatternRule pr = PatternRule.fromString(rule);
                if (pr.isValid) {
                    regexPatterns.add(pr);
                }
            }
        }
        
        Logger.debug("配置文件加载完成");
    }
    
    private void createDefault() {
        try {
            Files.createDirectories(CONFIG_PATH.getParent());
            JsonObject def = new JsonObject();
            
            // 基础配置
            JsonObject equipment = new JsonObject();
            equipment.add("android", new JsonArray());
            equipment.add("windows", new JsonArray());
            equipment.add("linux", new JsonArray());
            equipment.add("mac", new JsonArray());
            
            JsonObject environment = new JsonObject();
            environment.add("client", new JsonArray());
            environment.add("server", new JsonArray());
            
            def.add("equipment", equipment);
            def.add("environment", environment);
            
            // 高级配置
            def.addProperty("log_level", "INFO");
            def.addProperty("enable_commands", true);
            def.add("version_constraints", new JsonArray());
            def.add("rules", new JsonArray());
            
            Files.write(CONFIG_PATH, gson.toJson(def).getBytes());
            config = def;
            
            Logger.info("创建默认配置文件：" + CONFIG_PATH);
        } catch (IOException e) {
            Logger.error("创建配置文件失败：" + e.getMessage());
            config = new JsonObject();
        }
    }
    
    public List<String> getDisablePatterns(String device, String runtime) {
        List<String> patterns = new ArrayList<>();
        if (config == null) return patterns;

        JsonObject equipment = config.getAsJsonObject("equipment");
        if (equipment != null && equipment.has(device)) {
            for (JsonElement e : equipment.getAsJsonArray(device)) {
                patterns.add(e.getAsString());
            }
        }

        JsonObject environment = config.getAsJsonObject("environment");
        if (environment != null && environment.has(runtime)) {
            for (JsonElement e : environment.getAsJsonArray(runtime)) {
                patterns.add(e.getAsString());
            }
        }
        return patterns;
    }
    
    /**
     * 获取日志级别
     */
    public String getLogLevel() {
        return logLevel != null ? logLevel : "INFO";
    }
    
    /**
     * 启用命令系统
     */
    public boolean isCommandsEnabled() {
        return enableCommands != null && enableCommands;
    }
    
    /**
     * 获取版本约束列表
     */
    public List<VersionMatcher.VersionConstraint> getVersionConstraints() {
        return constraints != null ? constraints : new ArrayList<>();
    }
    
    /**
     * 获取正则规则列表
     */
    public List<PatternRule> getRegexPatterns() {
        return regexPatterns != null ? regexPatterns : new ArrayList<>();
    }
    
    /**
     * 获取日志级别枚举
     */
    public Logger.Level getLogLevelEnum() {
        try {
            return Logger.Level.valueOf(logLevel != null ? logLevel.toUpperCase() : "INFO");
        } catch (IllegalArgumentException e) {
            return Logger.Level.INFO;
        }
    }
}
