package com.kulisaiji.chameleon;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.*;
import java.util.regex.Pattern;

public class ModDisabler {
    
    /**
     * 规则命中统计
     */
    public static class RuleStats {
        public final String rule;
        public int matchCount = 0;
        public final List<String> matchedMods = new ArrayList<>();
        
        public RuleStats(String rule) {
            this.rule = rule;
        }
    }
    
    /**
     * 操作摘要
     */
    public static class Summary {
        public int scanned = 0;
        public int disabled = 0;
        public int skipped = 0;
        public int errors = 0;
        public final Map<String, RuleStats> ruleStats = new LinkedHashMap<>();
    }
    
    /**
     * 处理模组
     */
    public static Summary processMods(List<Path> jarPaths, List<String> patterns, 
                                      List<ConfigLoader.PatternRule> regexRules,
                                      List<VersionMatcher.VersionConstraint> versionConstraints,
                                      String device, String runtime) {
        Summary summary = new Summary();
        
        Logger.info("═══════════════════════════════════════════════════════════");
        Logger.info("          Chameleon 模组管理器");
        Logger.info("═══════════════════════════════════════════════════════════");
        Logger.info("设备类型：" + device);
        Logger.info("运行环境：" + runtime);
        Logger.info("禁用规则数：" + patterns.size());
        Logger.info("正则规则数：" + regexRules.size());
        Logger.info("版本约束数：" + versionConstraints.size());
        Logger.info("扫描模组数：" + jarPaths.size());
        Logger.info("═══════════════════════════════════════════════════════════");
        
        if (patterns.isEmpty() && regexRules.isEmpty() && versionConstraints.isEmpty()) {
            Logger.info("无禁用规则，跳过处理");
            return summary;
        }
        
        Path disabledDir = Paths.get("mods", "disabled");
        try {
            if (!Files.exists(disabledDir)) {
                Files.createDirectories(disabledDir);
                Logger.info("创建禁用目录：" + disabledDir);
            }
        } catch (IOException e) {
            Logger.error("无法创建禁用目录：" + e.getMessage());
            summary.errors++;
            return summary;
        }

        // 初始化规则统计
        for (String pattern : patterns) {
            summary.ruleStats.put(pattern, new RuleStats(pattern));
        }
        for (ConfigLoader.PatternRule regex : regexRules) {
            summary.ruleStats.put("r:" + regex.pattern, new RuleStats("r:" + regex.pattern));
        }
        for (VersionMatcher.VersionConstraint vc : versionConstraints) {
            summary.ruleStats.put(vc.toString(), new RuleStats(vc.toString()));
        }

        Set<Path> alreadyProcessed = new HashSet<>();

        for (Path jar : jarPaths) {
            if (alreadyProcessed.contains(jar)) {
                summary.skipped++;
                continue;
            }
            
            summary.scanned++;
            ModInfo modInfo = ModIDHelper.getModInfo(jar);
            
            // 输出模组信息
            logModInfo(modInfo, summary.scanned, jarPaths.size());
            Logger.debug("ModID 提取结果：" + modInfo.getModId());

            // 检查是否匹配规则
            MatchResult matchResult = checkMatch(modInfo, patterns, regexRules, versionConstraints);
            
            if (matchResult.matched) {
                Logger.debug("  -> 匹配规则 [" + matchResult.matchType + "]: " + matchResult.matchedRule);
                if (moveToDisabled(jar, modInfo.getFileName(), disabledDir)) {
                    summary.disabled++;
                    Logger.info("  -> 已禁用 ✓");
                    
                    // 更新统计
                    RuleStats stats = summary.ruleStats.get(matchResult.matchedRule);
                    if (stats != null) {
                        stats.matchCount++;
                        stats.matchedMods.add(modInfo.getFileName());
                    }
                } else {
                    summary.errors++;
                }
                alreadyProcessed.add(jar);
            } else {
                Logger.debug("  -> 未匹配任何规则");
            }
        }
        
        // 输出规则命中明细
        logRuleStats(summary);
        
        // 输出摘要
        printSummary(summary);
        
        return summary;
    }
    
    /**
     * 匹配结果
     */
    private static class MatchResult {
        public boolean matched = false;
        public String matchedRule = "";
        public String matchType = "";
    }
    
    /**
     * 检查模组是否匹配任何规则
     */
    private static MatchResult checkMatch(ModInfo modInfo, List<String> patterns,
                                          List<ConfigLoader.PatternRule> regexRules,
                                          List<VersionMatcher.VersionConstraint> versionConstraints) {
        MatchResult result = new MatchResult();
        
        String modId = modInfo.getModId();
        String fileName = modInfo.getFileName();
        String version = modInfo.getVersion();
        
        // 1. 检查基础规则（ModID 或文件名精确匹配）
        for (String pattern : patterns) {
            boolean match = false;
            
            if (pattern.toLowerCase().endsWith(".jar") || pattern.toLowerCase().endsWith(".zip")) {
                match = fileName.equalsIgnoreCase(pattern);
                if (match) {
                    result.matched = true;
                    result.matchedRule = pattern;
                    result.matchType = "文件名";
                    return result;
                }
            } else {
                match = modId != null && modId.equalsIgnoreCase(pattern);
                if (match) {
                    // 检查版本约束
                    VersionMatcher.VersionConstraint vc = findVersionConstraint(modId, versionConstraints);
                    if (vc != null) {
                        if (vc.matches(VersionMatcher.parseVersion(version))) {
                            result.matched = true;
                            result.matchedRule = pattern + " " + vc.toString();
                            result.matchType = "ModID+ 版本";
                            return result;
                        } else {
                            Logger.debug("    版本不符合约束：" + version + " " + vc.toString());
                            continue;
                        }
                    } else {
                        result.matched = true;
                        result.matchedRule = pattern;
                        result.matchType = "ModID";
                        return result;
                    }
                }
            }
        }
        
        // 2. 检查正则规则
        for (ConfigLoader.PatternRule regex : regexRules) {
            if (regex.matches(fileName) || (modId != null && regex.matches(modId))) {
                result.matched = true;
                result.matchedRule = "r:" + regex.pattern;
                result.matchType = "正则";
                return result;
            }
        }
        
        // 3. 检查纯版本约束规则
        for (VersionMatcher.VersionConstraint vc : versionConstraints) {
            if (modId != null && modId.equalsIgnoreCase(vc.getModId())) {
                if (vc.matches(VersionMatcher.parseVersion(version))) {
                    result.matched = true;
                    result.matchedRule = vc.toString();
                    result.matchType = "版本约束";
                    return result;
                }
            }
        }
        
        return result;
    }
    
    /**
     * 查找 ModID 对应的版本约束
     */
    private static VersionMatcher.VersionConstraint findVersionConstraint(
            String modId, List<VersionMatcher.VersionConstraint> constraints) {
        for (VersionMatcher.VersionConstraint vc : constraints) {
            if (modId.equalsIgnoreCase(vc.getModId())) {
                return vc;
            }
        }
        return null;
    }
    
    /**
     * 输出模组信息
     */
    private static void logModInfo(ModInfo modInfo, int current, int total) {
        StringBuilder sb = new StringBuilder();
        sb.append("[Chameleon] [").append(current).append("/").append(total).append("] ");
        sb.append("模组：").append(modInfo.getFileName());
        
        if (modInfo.getModId() != null) {
            sb.append(" | ID: ").append(modInfo.getModId());
        }
        
        if (modInfo.getModName() != null && !modInfo.getModName().equals(modInfo.getModId())) {
            sb.append(" | 名称：").append(modInfo.getModName());
        }
        
        if (modInfo.getVersion() != null) {
            sb.append(" | 版本：").append(modInfo.getVersion());
        }
        
        Logger.info(sb.toString());
        
        // DEBUG 模式下输出更多信息
        if (Logger.getCurrentLevel() == Logger.Level.DEBUG) {
            if (modInfo.getIconPath() != null) {
                Logger.debug("      图标：" + modInfo.getIconPath());
            }
            if (modInfo.getDescription() != null) {
                Logger.debug("      描述：" + modInfo.getDescription());
            }
        }
    }
    
    /**
     * 输出规则命中统计
     */
    private static void logRuleStats(Summary summary) {
        Logger.info("═══════════════════════════════════════════════════════════");
        Logger.info("规则命中明细：");
        
        boolean hasMatches = false;
        for (RuleStats stats : summary.ruleStats.values()) {
            if (stats.matchCount > 0) {
                Logger.info("  [" + stats.matchCount + "] " + stats.rule);
                for (String mod : stats.matchedMods) {
                    Logger.info("      - " + mod);
                }
                hasMatches = true;
            }
        }
        
        if (!hasMatches) {
            Logger.info("  无命中规则");
        }
        
        Logger.info("═══════════════════════════════════════════════════════════");
    }
    
    /**
     * 输出操作摘要
     */
    private static void printSummary(Summary summary) {
        Logger.info("═══════════════════════════════════════════════════════════");
        Logger.info("启动摘要：");
        Logger.info("  扫描总量：" + summary.scanned);
        Logger.info("  已禁用：" + summary.disabled);
        Logger.info("  已跳过：" + summary.skipped);
        Logger.info("  错误数：" + summary.errors);
        Logger.info("═══════════════════════════════════════════════════════════");
    }
    
    /**
     * 移动到禁用目录
     */
    private static boolean moveToDisabled(Path jar, String fileName, Path disabledDir) {
        Path target = disabledDir.resolve(fileName);

        if (Files.exists(target)) {
            Path bak = disabledDir.resolve(fileName + ".bak");
            int count = 1;
            while (Files.exists(bak)) {
                bak = disabledDir.resolve(fileName + ".bak." + count++);
            }
            try {
                Files.move(target, bak, StandardCopyOption.REPLACE_EXISTING);
                Logger.info("  -> 备份已存在文件：" + bak.getFileName());
            } catch (IOException e) {
                Logger.error("  -> 备份失败：" + e.getMessage());
                return false;
            }
        }

        try {
            Files.move(jar, target, StandardCopyOption.REPLACE_EXISTING);
            return true;
        } catch (IOException e) {
            Logger.error("  -> 无法禁用模组：" + fileName + " - " + e.getMessage());
            return false;
        }
    }
    
    /**
     * 兼容旧方法调用
     */
    @Deprecated
    public static void disableMods(List<Path> jarPaths, List<String> patterns, 
                                   String device, String runtime) {
        processMods(jarPaths, patterns, new ArrayList<>(), new ArrayList<>(), device, runtime);
    }
}
