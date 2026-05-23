# Chameleon 开发状态文档

## 项目概述

Chameleon 是一个多平台 Minecraft 模组禁用工具，支持 NeoForge、Fabric 和 Quilt 平台，适用于 Minecraft 1.21.1。

## 已完成的工作

### 1. 项目结构搭建
- 创建了多模块 Gradle 项目结构
- 模块包括：`common`、`neoforge`、`fabric`、`quilt`
- 配置了根 `build.gradle` 和 `settings.gradle`

### 2. Common 模块核心类
- **EnvironmentDetector.java**: 环境检测，支持 Windows/Linux/macOS/Android 启动器识别
- **ConfigLoader.java**: 配置文件加载与解析（JSON 格式）
- **ModIDHelper.java**: 从 JAR 文件中提取 ModID，支持 NeoForge/Fabric/Quilt 格式
- **ModDisabler.java**: 模组禁用逻辑，包含防重复处理

### 3. 平台适配层
- **NeoForge**: ChameleonLocator (IModFileCandidateLocator), ChameleonSetupModService (SetupModService)
- **Fabric**: ChameleonPreLaunch (PreLaunchEntrypoint)
- **Quilt**: ChameleonQuiltPreLaunch (PreLaunchEntrypoint)

### 4. 配置文件
- NeoForge 的 `neoforge.mods.toml`
- Fabric 的 `fabric.mod.json`
- Quilt 的 `quilt.mod.json`
- SPI 服务注册文件

### 5. Gradle Wrapper
- 配置了 Gradle 8.10 wrapper
- 创建了 `gradlew` 脚本

## 已知问题

### Shadow 插件 ASM 版本兼容性
- **问题描述**: Shadow 插件 8.1.1 内部使用的 ASM 版本无法处理 Java 21 (major version 65) 的 class 文件
- **错误信息**: `Unsupported class file major version 65`
- **影响范围**: `common` 模块的 `shadowJar` 任务无法完成，导致 toml4j 无法重打包到 common JAR 中
- **根因分析**: Shadow 插件 8.1.1 依赖的 ASM 库版本较旧，不支持 Java 21 的 class 文件格式

### Fabric/Quilt 模块
- 由于 Shadow 插件问题未解决，Fabric 和 Quilt 模块尚未验证编译
- NeoForge 模块也因 common 模块构建受阻

## 环境配置
- **Java**: Temurin JDK 21 (已安装)
- **Gradle**: 8.10 (wrapper)
- **Shadow 插件**: 8.1.1

## 下一步建议
1. 升级 Shadow 插件到支持 Java 21 的版本（如 8.3.x 或更高）
2. 或者降级项目 Java 版本到 Java 17（但 Minecraft 1.21.1 要求 Java 21）
3. 寻找替代方案，如手动重打包依赖
4. 考虑使用 Maven 替代 Gradle 构建

## 文件清单

```
Chameleon/
├── build.gradle
├── settings.gradle
├── gradle/
│   └── wrapper/
│       ├── gradle-wrapper.jar
│       └── gradle-wrapper.properties
├── gradlew
├── common/
│   ├── build.gradle
│   └── src/main/java/com/kulisaiji/chameleon/
│       ├── EnvironmentDetector.java
│       ├── ConfigLoader.java
│       ├── ModIDHelper.java
│       └── ModDisabler.java
├── neoforge/
│   ├── build.gradle
│   └── src/main/
│       ├── java/com/kulisaiji/chameleon/neoforge/
│       │   ├── ChameleonLocator.java
│       │   └── ChameleonSetupModService.java
│       └── resources/META-INF/
│           ├── neoforge.mods.toml
│           └── services/
│               ├── net.neoforged.neoforgespi.locating.IModFileCandidateLocator
│               └── settingdust.preloadingtricks.api.SetupModService
├── fabric/
│   ├── build.gradle
│   └── src/main/
│       ├── java/com/kulisaiji/chameleon/fabric/
│       │   └── ChameleonPreLaunch.java
│       └── resources/
│           └── fabric.mod.json
└── quilt/
    ├── build.gradle
    └── src/main/
        ├── java/com/kulisaiji/chameleon/quilt/
        │   └── ChameleonQuiltPreLaunch.java
        └── resources/
            └── quilt.mod.json
```
