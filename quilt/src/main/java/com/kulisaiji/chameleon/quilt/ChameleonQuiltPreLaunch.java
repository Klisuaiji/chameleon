package com.kulisaiji.chameleon.quilt;

import com.kulisaiji.chameleon.*;
import org.quiltmc.loader.api.QuiltLoader;
import org.quiltmc.loader.api.entrypoint.PreLaunchEntrypoint;

import java.nio.file.*;
import java.util.List;
import java.util.stream.Collectors;

public class ChameleonQuiltPreLaunch implements PreLaunchEntrypoint {
    @Override
    public void onPreLaunch() {
        System.setProperty("chameleon.runtime",
            QuiltLoader.getEnvironmentType().name().toLowerCase());

        ConfigLoader config = new ConfigLoader();
        config.loadOrCreate();

        String device = EnvironmentDetector.getDevice();
        String runtime = EnvironmentDetector.getRuntime();
        List<String> patterns = config.getDisablePatterns(device, runtime);
        if (patterns.isEmpty()) {
            return;
        }

        Path modsDir = QuiltLoader.getGameDir().resolve("mods");
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
            ModDisabler.disableMods(jarPaths, patterns);
        } catch (Exception e) {
            System.err.println("[Chameleon] Quilt PreLaunch 禁用失败: " + e.getMessage());
        }
    }
}
