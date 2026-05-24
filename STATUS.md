# Chameleon 开发状态文档

## 项目概述

Chameleon 是一个多平台 Minecraft 模组禁用工具，支持 NeoForge 和 Fabric 平台，适用于 Minecraft 1.21.1。

## 最新进展

### 2026-05-24 - v2.0 核心功能实现

#### 新增核心功能

1. **日志系统 (Logger.java)**
   - 独立日志文件：`logs/chameleon.log`
   - 三级日志：INFO / WARN / DEBUG
   - 系统语言自动检测并输出对应语言日志
   - 启动摘要统计：扫描/禁用/跳过/错误数量

2. **版本约束系统 (VersionMatcher.java)**
   - 支持语义化版本比较 (SemVer 2.0.0)
   - 约束操作符：`<` `<=` `>` `>=` `=`
   - 示例：`"sodium (<0.6.0)"` 禁用低于 0.6.0 的版本
   - 从 JAR 元数据自动提取版本号并比较

3. **规则校验增强**
   - 正则语法预检：启动时验证 `r:` 规则，无效则警告并跳过
   - 规则命中明细：日志中输出每条规则匹配的模组列表
   - 支持三种规则类型：ModID 匹配、文件名匹配、正则匹配

4. **命令系统 (CommandHandler.java)**
   - `/chameleon list` - 扫描并列出所有模组，生成 Markdown 列表
   - `/chameleon mod` - 查看已禁用的模组
   - `/chameleon undo` - 恢复所有被禁用的模组（需二次确认）
   - 仅客户端可用，服务器禁用 undo 命令

#### 配置文件升级

新增字段：
```json
{
  "log_level": "INFO",           // 日志级别
  "enable_commands": true,       // 命令系统开关
  "version_constraints": [],     // 版本约束数组
  "rules": []                    // 支持 r: 前缀的正则规则
}
```

#### 代码重构

- **ModDisabler.java**: 重构为 `processMods()` 方法，支持多种规则类型
- **ConfigLoader.java**: 新增配置字段加载和解析
- **入口文件**: Fabric 和 NeoForge 入口集成新功能

### 2025-05-23 - 完成的重构

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
├── common/                              # 核心逻辑模块
│   ├── build.gradle                      # Shadow 插件 8.3.5
│   └── src/main/java/com/kulisaiji/chameleon/
│       ├── Logger.java                   # 日志系统（新增）
│       ├── VersionMatcher.java           # 版本比较器（新增）
│       ├── CommandHandler.java           # 命令处理器（新增）
│       ├── EnvironmentDetector.java
│       ├── ConfigLoader.java             # 已升级
│       ├── ModIDHelper.java
│       └── ModDisabler.java              # 已重构
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

## 待办事项

- [ ] 在实际游戏中测试 NeoForge 模块 `ChameleonLocator` 功能
- [ ] 验证 Fabric 模块在真实环境中的运行
- [ ] 添加命令系统权限检查（可选）
- [ ] 支持更多的版本约束语法（如 `^` `~` 等）
- [ ] 添加配置文件的注释支持
