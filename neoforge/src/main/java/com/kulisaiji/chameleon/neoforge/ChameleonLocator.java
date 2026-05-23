package com.kulisaiji.chameleon.neoforge;

import com.kulisaiji.chameleon.*;
import net.neoforged.neoforgespi.locating.*;

import java.nio.file.*;
import java.util.*;
import java.util.function.Consumer;
import java.util.stream.Collectors;

public class ChameleonLocator implements IModFileCandidateLocator {
    /**
     * [修正] NeoForge 21.1.x 的 IModFileCandidateLocator 签名。
     * 使用 ModDiscoveryContext 获取游戏目录，直接扫描 mods 文件夹。
     */
    @Override
    public void findCandidates(ModDiscoveryContext context, Consumer<IModFileCandidate> candidateConsumer) {
        // 如果 Preloading Tricks 的 SetupModService 已提前执行，则跳过
        if (System.getProperty("chameleon.preloading.done") != null) {
            return;
        }

        ConfigLoader config = new ConfigLoader();
        config.loadOrCreate();

        String device = EnvironmentDetector.getDevice();
        String runtime = EnvironmentDetector.getRuntime();
        List<String> patterns = config.getDisablePatterns(device, runtime);
        if (patterns.isEmpty()) {
            return;
        }

        try {
            Path modsDir = context.getGameDirectory().resolve("mods");
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
            System.err.println("[Chameleon] IModFileCandidateLocator 阶段禁用失败: " + e.getMessage());
        }
    }
}
