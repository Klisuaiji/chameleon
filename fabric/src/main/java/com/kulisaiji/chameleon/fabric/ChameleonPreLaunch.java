package com.kulisaiji.chameleon.fabric;

import com.kulisaiji.chameleon.ConfigLoader;
import com.kulisaiji.chameleon.EnvironmentDetector;
import com.kulisaiji.chameleon.Logger;
import com.kulisaiji.chameleon.ModDisabler;
import com.kulisaiji.chameleon.VersionMatcher;
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.entrypoint.PreLaunchEntrypoint;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Collectors;

public class ChameleonPreLaunch implements PreLaunchEntrypoint {
    @Override
    public void onPreLaunch() {
        // 注入 runtime 标记
        System.setProperty("chameleon.runtime",
            FabricLoader.getInstance().getEnvironmentType().name().toLowerCase());

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
            Path modsDir = FabricLoader.getInstance().getGameDir().resolve("mods");
            if (Files.isDirectory(modsDir)) {
                try {
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
                    Logger.error("Fabric PreLaunch 禁用失败：" + e.getMessage(), e);
                }
            }
        } else {
            Logger.info("无禁用规则，跳过处理");
        }
    }
}
