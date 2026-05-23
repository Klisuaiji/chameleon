package com.kulisaiji.chameleon.fabric;

import com.kulisaiji.chameleon.ConfigLoader;
import com.kulisaiji.chameleon.EnvironmentDetector;
import com.kulisaiji.chameleon.ModDisabler;
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.entrypoint.PreLaunchEntrypoint;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Collectors;

public class ChameleonPreLaunch implements PreLaunchEntrypoint {
    @Override
    public void onPreLaunch() {
        // [修正] 通过 FabricLoader 注入 runtime 标记并获取真实游戏目录
        System.setProperty("chameleon.runtime",
            FabricLoader.getInstance().getEnvironmentType().name().toLowerCase());

        ConfigLoader config = new ConfigLoader();
        config.loadOrCreate();

        String device = EnvironmentDetector.getDevice();
        String runtime = EnvironmentDetector.getRuntime();
        List<String> patterns = config.getDisablePatterns(device, runtime);
        if (patterns.isEmpty()) {
            return;
        }

        Path modsDir = FabricLoader.getInstance().getGameDir().resolve("mods");
        if (!Files.isDirectory(modsDir)) {
            return;
        }

        try {
            List<Path> jarPaths = Files.list(modsDir)
                .filter(f -> {
                    String n = f.toString().toLowerCase();
                    return n.endsWith(".jar") || n.endsWith(".zip");
                })
                .collect(Collectors.toList());
            ModDisabler.disableMods(jarPaths, patterns, device, runtime);
        } catch (Exception e) {
            System.err.println("[Chameleon] Fabric PreLaunch 禁用失败: " + e.getMessage());
        }
    }
}
