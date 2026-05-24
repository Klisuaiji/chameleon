package com.kulisaiji.chameleon;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Chameleon 命令系统
 * 仅客户端/单人游戏可用
 */
public class CommandHandler {
    
    private static boolean undoPending = false;
    private static final Path MOD_LIST_OUTPUT = Paths.get("config", "chameleon_mod_list.md");
    
    /**
     * 处理命令
     * @return 命令执行结果消息
     */
    public static List<String> handleCommand(String command, boolean isServer) {
        List<String> output = new ArrayList<>();
        
        if (!command.startsWith("/chameleon")) {
            return output;
        }
        
        ConfigLoader config = new ConfigLoader();
        config.loadOrCreate();
        
        if (!config.isCommandsEnabled()) {
            output.add("命令系统已被禁用");
            return output;
        }
        
        String[] parts = command.split(" ");
        if (parts.length < 2) {
            output.add("用法：/chameleon <list|mod|undo|undo confirm>");
            return output;
        }
        
        String subCommand = parts[1];
        
        switch (subCommand) {
            case "list":
                handleList(output);
                break;
                
            case "mod":
                handleMod(output);
                break;
                
            case "undo":
                if (isServer) {
                    output.add("错误：undo 命令仅客户端可用");
                } else if (parts.length > 2 && "confirm".equals(parts[2])) {
                    if (undoPending) {
                        handleUndo(output);
                        undoPending = false;
                    } else {
                        output.add("错误：请先执行 /chameleon undo 进行确认");
                    }
                } else {
                    undoPending = true;
                    output.add("警告：此操作将恢复所有被禁用的模组到 mods/ 目录");
                    output.add("请在 30 秒内输入 /chameleon undo confirm 确认操作");
                }
                break;
                
            default:
                output.add("未知命令：" + subCommand);
                output.add("用法：/chameleon <list|mod|undo|undo confirm>");
        }
        
        return output;
    }
    
    /**
     * /chameleon list - 扫描并列出所有模组
     */
    private static void handleList(List<String> output) {
        Path modsDir = Paths.get("mods");
        if (!Files.isDirectory(modsDir)) {
            output.add("错误：mods 目录不存在");
            return;
        }
        
        List<ModInfo> modInfos = new ArrayList<>();
        try (Stream<Path> paths = Files.list(modsDir)) {
            paths.filter(p -> p.toString().toLowerCase().endsWith(".jar"))
                 .forEach(p -> {
                     ModInfo info = ModIDHelper.getModInfo(p);
                     modInfos.add(info);
                 });
        } catch (IOException e) {
            output.add("错误：读取 mods 目录失败 - " + e.getMessage());
            return;
        }
        
        modInfos.sort(Comparator.comparing(ModInfo::getModId));
        
        output.add("═══════════════════════════════════════════════════");
        output.add("  模组列表 - 共 " + modInfos.size() + " 个模组");
        output.add("═══════════════════════════════════════════════════");
        
        StringBuilder markdown = new StringBuilder();
        markdown.append("# Chameleon 模组列表\n\n");
        markdown.append("生成时间：" + java.time.LocalDateTime.now()
            .format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))
            .replace(" ", " \\| ")).append("\n\n");
        markdown.append("| 文件名 | ModID | 名称 | 版本 |\n");
        markdown.append("|--------|-------|------|------|\n");
        
        int count = 0;
        for (ModInfo info : modInfos) {
            count++;
            String line = String.format("[%d] %s", count, info.getFileName());
            
            if (info.getModId() != null) {
                line += " | ID: " + info.getModId();
            }
            if (info.getModName() != null && !info.getModName().equals(info.getModId())) {
                line += " | 名称：" + info.getModName();
            }
            if (info.getVersion() != null) {
                line += " | 版本：" + info.getVersion();
            }
            
            output.add(line);
            
            // Markdown 表格
            String mdLine = String.format("| %s | %s | %s | %s |\n",
                info.getFileName(),
                info.getModId() != null ? info.getModId() : "N/A",
                info.getModName() != null ? info.getModName() : "N/A",
                info.getVersion() != null ? info.getVersion() : "N/A"
            );
            markdown.append(mdLine);
        }
        
        output.add("═══════════════════════════════════════════════════");
        
        // 生成 Markdown 文件
        try {
            Files.createDirectories(MOD_LIST_OUTPUT.getParent());
            try (BufferedWriter writer = Files.newBufferedWriter(MOD_LIST_OUTPUT)) {
                writer.write(markdown.toString());
            }
            output.add("模组列表已导出至：" + MOD_LIST_OUTPUT);
        } catch (IOException e) {
            output.add("警告：导出 Markdown 失败 - " + e.getMessage());
        }
    }
    
    /**
     * /chameleon mod - 查看已禁用的模组
     */
    private static void handleMod(List<String> output) {
        Path disabledDir = Paths.get("mods", "disabled");
        if (!Files.isDirectory(disabledDir)) {
            output.add("没有已禁用的模组（disabled 目录不存在）");
            return;
        }
        
        List<Path> disabledMods = new ArrayList<>();
        try (Stream<Path> paths = Files.list(disabledDir)) {
            paths.filter(p -> p.toString().toLowerCase().endsWith(".jar") ||
                             p.toString().toLowerCase().endsWith(".zip"))
                 .forEach(disabledMods::add);
        } catch (IOException e) {
            output.add("错误：读取 disabled 目录失败 - " + e.getMessage());
            return;
        }
        
        if (disabledMods.isEmpty()) {
            output.add("没有已禁用的模组");
            return;
        }
        
        disabledMods.sort(Comparator.comparing(Path::getFileName));
        
        output.add("═══════════════════════════════════════════════════");
        output.add("  已禁用的模组 - 共 " + disabledMods.size() + " 个");
        output.add("═══════════════════════════════════════════════════");
        
        int count = 0;
        for (Path mod : disabledMods) {
            count++;
            ModInfo info = ModIDHelper.getModInfo(mod);
            
            String line = String.format("[%d] %s", count, mod.getFileName());
            if (info.getModId() != null) {
                line += " | ID: " + info.getModId();
            }
            if (info.getVersion() != null) {
                line += " | 版本：" + info.getVersion();
            }
            
            output.add(line);
        }
        
        output.add("═══════════════════════════════════════════════════");
        output.add("使用 /chameleon undo 可恢复所有模组");
    }
    
    /**
     * /chameleon undo - 恢复所有禁用的模组
     */
    private static void handleUndo(List<String> output) {
        Path disabledDir = Paths.get("mods", "disabled");
        Path modsDir = Paths.get("mods");
        
        if (!Files.isDirectory(disabledDir)) {
            output.add("没有需要恢复的模组");
            return;
        }
        
        List<Path> disabledMods = new ArrayList<>();
        try (Stream<Path> paths = Files.list(disabledDir)) {
            paths.filter(p -> p.toString().toLowerCase().endsWith(".jar") ||
                             p.toString().toLowerCase().endsWith(".zip"))
                 .forEach(disabledMods::add);
        } catch (IOException e) {
            output.add("错误：读取 disabled 目录失败 - " + e.getMessage());
            return;
        }
        
        if (disabledMods.isEmpty()) {
            output.add("没有需要恢复的模组");
            return;
        }
        
        int restored = 0;
        int skipped = 0;
        
        for (Path mod : disabledMods) {
            Path target = modsDir.resolve(mod.getFileName());
            
            // 检查目标位置是否已存在
            if (Files.exists(target)) {
                output.add("跳过：" + mod.getFileName() + " (mods/ 中已存在)");
                skipped++;
                continue;
            }
            
            try {
                Files.move(mod, target, StandardCopyOption.REPLACE_EXISTING);
                output.add("恢复：" + mod.getFileName());
                restored++;
            } catch (IOException e) {
                output.add("失败：" + mod.getFileName() + " - " + e.getMessage());
            }
        }
        
        output.add("═══════════════════════════════════════════════════");
        output.add("恢复完成：成功 " + restored + " 个，跳过 " + skipped + " 个");
        
        // 如果 disabled 目录为空，删除它
        try {
            if (Files.list(disabledDir).count() == 0) {
                Files.delete(disabledDir);
            }
        } catch (IOException e) {
            // 忽略
        }
    }
    
    /**
     * 检查是否是服务器环境
     */
    public static boolean isServerEnvironment() {
        String runtime = System.getProperty("chameleon.runtime", "client");
        return "server".equals(runtime);
    }
}
