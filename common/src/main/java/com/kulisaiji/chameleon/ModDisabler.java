package com.kulisaiji.chameleon;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class ModDisabler {
    public static void disableMods(List<Path> jarPaths, List<String> patterns, String device, String runtime) {
        // 输出环境信息
        printBanner();
        System.out.println("[Chameleon] 设备类型: " + device);
        System.out.println("[Chameleon] 运行环境: " + runtime);
        System.out.println("[Chameleon] 禁用规则数: " + patterns.size());
        System.out.println("[Chameleon] 扫描模组数: " + jarPaths.size());
        System.out.println("[Chameleon] ==========================================");
        
        if (patterns.isEmpty()) {
            System.out.println("[Chameleon] 无禁用规则，跳过处理");
            return;
        }
        
        Path disabledDir = Paths.get("mods", "disabled");
        try {
            if (!Files.exists(disabledDir)) {
                Files.createDirectories(disabledDir);
                System.out.println("[Chameleon] 创建禁用目录: " + disabledDir);
            }
        } catch (IOException e) {
            System.err.println("[Chameleon] 无法创建禁用目录: " + e.getMessage());
            return;
        }

        // 防重复处理
        Set<Path> alreadyProcessed = new HashSet<>();
        int disabledCount = 0;
        int scannedCount = 0;

        for (Path jar : jarPaths) {
            if (alreadyProcessed.contains(jar)) {
                continue;
            }
            
            scannedCount++;
            ModInfo modInfo = ModIDHelper.getModInfo(jar);
            
            // 输出模组信息
            printModInfo(modInfo, scannedCount, jarPaths.size());

            for (String pattern : patterns) {
                boolean match = false;
                String matchType = "";

                if (pattern.toLowerCase().endsWith(".jar") || pattern.toLowerCase().endsWith(".zip")) {
                    // 按文件名精确匹配
                    match = modInfo.getFileName().equalsIgnoreCase(pattern);
                    matchType = "文件名";
                } else {
                    // 按 ModID 匹配
                    match = pattern.equalsIgnoreCase(modInfo.getModId());
                    matchType = "ModID";
                }

                if (match) {
                    System.out.println("[Chameleon]   -> 匹配规则 [" + matchType + "]: " + pattern);
                    if (moveToDisabled(jar, modInfo.getFileName(), disabledDir)) {
                        disabledCount++;
                        System.out.println("[Chameleon]   -> 已禁用 ✓");
                    }
                    alreadyProcessed.add(jar);
                    break;
                }
            }
        }
        
        printFooter(scannedCount, disabledCount);
    }
    
    private static void printBanner() {
        System.out.println("[Chameleon] ==========================================");
        System.out.println("[Chameleon]          Chameleon 模组管理器");
        System.out.println("[Chameleon] ==========================================");
    }
    
    private static void printModInfo(ModInfo modInfo, int current, int total) {
        StringBuilder sb = new StringBuilder();
        sb.append("[Chameleon] [").append(current).append("/").append(total).append("] ");
        sb.append("模组: ").append(modInfo.getFileName());
        
        if (modInfo.getModId() != null) {
            sb.append(" | ID: ").append(modInfo.getModId());
        }
        
        if (modInfo.getModName() != null && !modInfo.getModName().equals(modInfo.getModId())) {
            sb.append(" | 名称: ").append(modInfo.getModName());
        }
        
        if (modInfo.getVersion() != null) {
            sb.append(" | 版本: ").append(modInfo.getVersion());
        }
        
        System.out.println(sb.toString());
        
        // 图标信息（如果有）
        if (modInfo.getIconPath() != null) {
            System.out.println("[Chameleon]      图标: " + modInfo.getIconPath());
        }
        
        // 描述（截断显示）
        if (modInfo.getDescription() != null) {
            String desc = modInfo.getDescription();
            if (desc.length() > 50) {
                desc = desc.substring(0, 50) + "...";
            }
            System.out.println("[Chameleon]      描述: " + desc);
        }
    }
    
    private static void printFooter(int scanned, int disabled) {
        System.out.println("[Chameleon] ==========================================");
        System.out.println("[Chameleon] 扫描完成: " + scanned + " 个模组");
        System.out.println("[Chameleon] 已禁用: " + disabled + " 个模组");
        System.out.println("[Chameleon] ==========================================");
    }

    private static boolean moveToDisabled(Path jar, String fileName, Path disabledDir) {
        Path target = disabledDir.resolve(fileName);

        // 处理同名文件备份
        if (Files.exists(target)) {
            Path bak = disabledDir.resolve(fileName + ".bak");
            int count = 1;
            while (Files.exists(bak)) {
                bak = disabledDir.resolve(fileName + ".bak." + count++);
            }
            try {
                Files.move(target, bak, StandardCopyOption.REPLACE_EXISTING);
                System.out.println("[Chameleon]   -> 备份已存在文件: " + bak.getFileName());
            } catch (IOException ignored) {}
        }

        try {
            Files.move(jar, target, StandardCopyOption.REPLACE_EXISTING);
            return true;
        } catch (IOException e) {
            System.err.println("[Chameleon] 无法禁用模组: " + fileName + " - " + e.getMessage());
            return false;
        }
    }
}
