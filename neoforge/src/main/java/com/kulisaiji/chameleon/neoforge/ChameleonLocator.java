package com.kulisaiji.chameleon.neoforge;

import com.kulisaiji.chameleon.ConfigLoader;
import com.kulisaiji.chameleon.EnvironmentDetector;
import com.kulisaiji.chameleon.Logger;
import com.kulisaiji.chameleon.ModDisabler;
import net.neoforged.neoforgespi.ILaunchContext;
import net.neoforged.neoforgespi.locating.IDiscoveryPipeline;
import net.neoforged.neoforgespi.locating.IModFileCandidateLocator;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.stream.Collectors;

public class ChameleonLocator implements IModFileCandidateLocator {
    /**
     * NeoForge 标准 API 中最早的介入点。
     * 在 ModDiscoverer 扫描阶段执行，此时模组元数据尚未解析。
     * 直接扫描 mods/ 目录并移动不兼容 JAR，后续 locator 扫描到的是已清理目录。
     */
    @Override
    public void findCandidates(ILaunchContext context, IDiscoveryPipeline pipeline) {
        // 1. 加载配置
        ConfigLoader config = new ConfigLoader();
        config.loadOrCreate();
        
        // 2. 初始化日志系统
        Logger.initialize(config.getLogLevelEnum());
        Logger.info("系统语言：" + Logger.getSystemLanguage() + " (" + (Logger.isChinese() ? "中文日志" : "English logs") + ")");

        // 3. 获取设备和运行环境
        String device = EnvironmentDetector.getDevice();
        String runtime = EnvironmentDetector.getRuntime();
        
        // 4. 获取禁用规则
        List<String> patterns = config.getDisablePatterns(device, runtime);
        List<ConfigLoader.PatternRule> regexPatterns = config.getRegexPatterns();
        List<VersionMatcher.VersionConstraint> versionConstraints = config.getVersionConstraints();
        
        // 5. 执行模组禁用
        if (!patterns.isEmpty() || !regexPatterns.isEmpty() || !versionConstraints.isEmpty()) {
            try {
                Path modsDir = Paths.get("mods");
                if (!Files.isDirectory(modsDir)) {
                    Logger.debug("mods 目录不存在，跳过处理");
                    return;
                }
                
                List<Path> jarPaths = Files.list(modsDir)
                    .filter(f -> {
                        String n = f.toString().toLowerCase();
                        return n.endsWith(".jar") || n.endsWith(".zip");
                    })
                    .collect(Collectors.toList());
                
                ModDisabler.processMods(
                    jarPaths,
                    patterns,
                    config.getRegexPatterns(),
                    config.getVersionConstraints(),
                    device,
                    runtime
                );
            } catch (Exception e) {
                Logger.error("禁用模组失败：" + e.getMessage(), e);
            }
        } else {
            Logger.info("无禁用规则，跳过处理");
        }
    }
}
