package com.kulisaiji.chameleon.neoforge;

import com.kulisaiji.chameleon.*;
import net.neoforged.neoforgespi.ILaunchContext;
import net.neoforged.neoforgespi.locating.*;

import java.nio.file.*;
import java.util.*;
import java.util.stream.Collectors;

public class ChameleonLocator implements IModFileCandidateLocator {
    /**
     * NeoForge 标准 API 中最早的介入点。
     * 在 ModDiscoverer 扫描阶段执行，此时模组元数据尚未解析。
     * 直接扫描 mods/ 目录并移动不兼容 JAR，后续 locator 扫描到的是已清理目录。
     */
    @Override
    public void findCandidates(ILaunchContext context, IDiscoveryPipeline pipeline) {
        ConfigLoader config = new ConfigLoader();
        config.loadOrCreate();

        String device = EnvironmentDetector.getDevice();
        String runtime = EnvironmentDetector.getRuntime();
        List<String> patterns = config.getDisablePatterns(device, runtime);
        if (patterns.isEmpty()) {
            return;
        }

        try {
            // 使用相对路径获取 mods 目录
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
            System.err.println("[Chameleon] 禁用模组失败: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
