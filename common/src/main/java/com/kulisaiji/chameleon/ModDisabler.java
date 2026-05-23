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
    public static void disableMods(List<Path> jarPaths, List<String> patterns) {
        Path disabledDir = Paths.get("mods", "disabled");
        try {
            if (!Files.exists(disabledDir)) {
                Files.createDirectories(disabledDir);
            }
        } catch (IOException e) {
            return;
        }

        // [拓展] 防重复处理：同一模组被多个规则命中时仅移动一次
        Set<Path> alreadyProcessed = new HashSet<>();

        for (Path jar : jarPaths) {
            if (alreadyProcessed.contains(jar)) {
                continue;
            }

            String fileName = jar.getFileName().toString();
            String modId = ModIDHelper.getModId(jar);

            for (String pattern : patterns) {
                boolean match = false;

                if (pattern.toLowerCase().endsWith(".jar") || pattern.toLowerCase().endsWith(".zip")) {
                    // 按文件名精确匹配
                    match = fileName.equalsIgnoreCase(pattern);
                } else {
                    // 按 ModID 匹配
                    match = pattern.equalsIgnoreCase(modId);
                }

                if (match) {
                    moveToDisabled(jar, fileName, disabledDir);
                    alreadyProcessed.add(jar);
                    break; // [拓展] 命中即停，不再检查后续 patterns
                }
            }
        }
    }

    private static void moveToDisabled(Path jar, String fileName, Path disabledDir) {
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
            } catch (IOException ignored) {}
        }

        try {
            Files.move(jar, target, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            System.err.println("[Chameleon] 无法禁用模组: " + fileName + " - " + e.getMessage());
        }
    }
}
