# Chameleon 开发状态文档

## 项目概述

Chameleon 是一个多平台 Minecraft 模组禁用工具，支持 NeoForge、Fabric 和 Quilt 平台，适用于 Minecraft 1.21.1。

## 最新进展（2025-05-23）

### 已完成的修复

1. **Shadow 插件升级成功**
   - 从 `com.github.johnrengelman.shadow` 8.1.1 升级至 `com.gradleup.shadow` 8.3.5
   - 该版本正式支持 Java 21 class 文件格式
   - `common` 模块 `shadowJar` 任务构建成功，toml4j 正确重打包到 JAR 中

2. **Gradle 版本适配**
   - 保持使用 Gradle 8.10（满足 NeoForge 和 Fabric 插件的兼容性要求）
   - 环境配置：Temurin JDK 21

3. **NeoForge 模块部分适配**
   - 修正 `ChameleonLocator.findCandidates()` 方法签名以匹配 NeoForge 21.1.x API：
     - 从 `(ModDiscoveryContext, Consumer<IModFileCandidate>)` 改为 `(ILaunchContext, IDiscoveryPipeline)`
   - 修正 `ChameleonSetupModService` 实现 `PreloadingEntrypoint` 接口的 `onPreloading()` 方法
   - 由于 `ILaunchContext` 无 `getGameDirectory()` 方法，改用 `Paths.get("mods")` 相对路径

### 待解决的问题

#### NeoForge 模块编译
- **ChameleonSetupModService** 实现 `PreloadingEntrypoint` 接口时，`@Override` 注解报错：
  - `method does not override or implement a method from a supertype`
- 原因：`PreloadingEntrypoint` 是一个空标记接口（无任何方法），`onPreloading()` 方法不存在于接口中
- 解决方案：移除 `@Override` 注解，或确认 Preloading Tricks 的正确使用方式

#### Fabric 模块
- Fabric Loom 1.7 在 Gradle 8.10 下配置阶段报错 `Unsupported class file major version 65`
- 可能需要升级 Fabric Loom 版本或检查 ASM 依赖冲突

#### Quilt 模块
- `org.quiltmc.loom` 插件在 Quilt 仓库中无法解析（404）
- 当前 `settings.gradle` 中已移除 `quilt` 模块

## 文件清单

```
Chameleon/
├── build.gradle                          # 根构建脚本（已修正插件引用）
├── settings.gradle                       # 模块配置（暂不含 quilt）
├── gradle/wrapper/                      # Gradle Wrapper 配置
├── common/                              # 核心逻辑模块（构建成功）
│   ├── build.gradle                      # Shadow 插件 8.3.5
│   └── src/main/java/com/kulisaiji/chameleon/
│       ├── EnvironmentDetector.java
│       ├── ConfigLoader.java
│       ├── ModIDHelper.java
│       └── ModDisabler.java
├── neoforge/                            # NeoForge 适配层（编译中）
│   ├── build.gradle
│   └── src/main/
│       ├── java/com/kulisaiji/chameleon/neoforge/
│       │   ├── ChameleonLocator.java      # API 签名已修正
│       │   └── ChameleonSetupModService.java # PreloadingEntrypoint 接口待确认
│       └── resources/META-INF/
│           ├── neoforge.mods.toml
│           └── services/
├── fabric/                              # Fabric 适配层（待修复）
│   ├── build.gradle
│   └── src/main/
│       ├── java/com/kulisaiji/chameleon/fabric/
│       │   └── ChameleonPreLaunch.java
│       └── resources/
│           └── fabric.mod.json
└── quilt/                               # Quilt 适配层（暂不可用）
    ├── build.gradle
    └── src/main/
        ├── java/com/kulisaiji/chameleon/quilt/
        │   └── ChameleonQuiltPreLaunch.java
        └── resources/
            └── quilt.mod.json
```

## 下一步建议

1. **NeoForge**：确认 `PreloadingEntrypoint` 的正确实现方式，或改用其他早期介入机制
2. **Fabric**：升级 Fabric Loom 到 1.8+ 或检查 ASM 版本冲突
3. **Quilt**：等待 Quilt Loom 插件仓库恢复或寻找替代方案
