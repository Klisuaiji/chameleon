package com.kulisaiji.chameleon.neoforge;

import com.kulisaiji.chameleon.*;
import settingdust.preloading_tricks.api.PreloadingEntrypoint;

import java.nio.file.*;
import java.util.List;
import java.util.stream.Collectors;

public class ChameleonSetupModService implements PreloadingEntrypoint {
    @Override
    public void onPreloading() {
        System.setProperty("chameleon.preloading.done", "true");
        // 在极早期注入 runtime 标记，供 EnvironmentDetector 回退使用
        System.setProperty("chameleon.runtime", detectSide());

        ConfigLoader config = new ConfigLoader();
        config.loadOrCreate();

        String device = EnvironmentDetector.getDevice();
        String runtime = EnvironmentDetector.getRuntime();
        List<String> patterns = config.getDisablePatterns(device, runtime);
        if (patterns.isEmpty()) {
            return;
        }

        try {
            // 早期阶段无加载器 API 可用，使用相对路径
            Path modsDir = Paths.get("mods");
            if (!Files.isDirectory(modsDir)) {
                return;
            }
            List<Path> jarPaths = Files.list(modsDir)
                .filter(f -> {
                    String n = f.toString().toLowerCase();
                    return n.endsWith(".jar") || n.endsWith(".zip");
                })
                .collect(Collectors.toList());
            ModDisabler.disableMods(jarPaths, patterns);
        } catch (Exception e) {
            System.err.println("[Chameleon] SetupModService 阶段禁用失败: " + e.getMessage());
        }
    }

    private String detectSide() {
        String target = System.getProperty("fml.target", "");
        if (target.contains("server") || target.contains("dedicated")) return "server";
        return "client";
    }
}
