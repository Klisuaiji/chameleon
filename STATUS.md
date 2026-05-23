# Chameleon 开发状态文档

## 项目概述

Chameleon 是一个多平台 Minecraft 模组禁用工具，支持 NeoForge 和 Fabric 平台，适用于 Minecraft 1.21.1。

## 最新进展（2025-05-23）

### 已完成的重构

1. **移除 Preloading Tricks**
   - 彻底删除 NeoForge 模块中的 `ChameleonSetupModService.java`
   - 删除对应 SPI 文件 `settingdust.preloadingtricks.api.SetupModService`
   - NeoForge 仅保留 `ChameleonLocator` 作为唯一入口，使用标准 `IModFileCandidateLocator` API

2. **Fabric 升级**
   - `fabric-loom` 从 `1.7-SNAPSHOT` 升级至 `1.9-SNAPSHOT`
   - `fabric-loader` 升级至 `0.16.10`
   - 添加 `minecraft` 和 `yarn` 依赖（Loom 1.9 必需）

3. **Quilt 模块删除**
   - 移除 `:quilt` 独立模块
   - Quilt 用户可直接安装 Fabric 构建产物（Quilt Loader 兼容 Fabric 模组）

4. **Shadow 插件升级**
   - 从 `com.github.johnrengelman.shadow` 8.1.1 升级至 `com.gradleup.shadow` 8.3.5
   - 解决 Java 21 class 文件格式不支持问题

5. **Gradle 升级**
   - Wrapper 升级至 Gradle 8.11
   - 兼容 Fabric Loom 1.9 和 NeoForge userdev 7.0.145

6. **CI/CD**
   - 添加 `.github/workflows/build.yml`
   - 支持 push/pull_request 自动构建
   - 产物：`chameleon-fabric` 和 `chameleon-neoforge`

## 当前项目结构

```
Chameleon/
├── .github/workflows/build.yml          # GitHub Actions CI
├── build.gradle                          # 根构建脚本
├── settings.gradle                       # 模块配置（common, neoforge, fabric）
├── gradle/wrapper/                      # Gradle Wrapper 8.11
├── common/                              # 核心逻辑模块（构建成功）
│   ├── build.gradle                      # Shadow 插件 8.3.5
│   └── src/main/java/com/kulisaiji/chameleon/
│       ├── EnvironmentDetector.java
│       ├── ConfigLoader.java
│       ├── ModIDHelper.java
│       └── ModDisabler.java
├── neoforge/                            # NeoForge 适配层
│   ├── build.gradle
│   └── src/main/
│       ├── java/com/kulisaiji/chameleon/neoforge/
│       │   └── ChameleonLocator.java    # IModFileCandidateLocator 实现
│       └── resources/META-INF/
│           ├── neoforge.mods.toml
│           └── services/
│               └── net.neoforged.neoforgespi.locating.IModFileCandidateLocator
└── fabric/                              # Fabric 适配层
    ├── build.gradle
    └── src/main/
        ├── java/com/kulisaiji/chameleon/fabric/
        │   └── ChameleonPreLaunch.java
        └── resources/
            └── fabric.mod.json
```

## 待验证项

- NeoForge 模块 `ChameleonLocator` 使用 `ILaunchContext + IDiscoveryPipeline` 签名，需在实际游戏中验证
- Fabric 模块 Loom 1.9 配置需验证编译
- CI 工作流首次运行结果
