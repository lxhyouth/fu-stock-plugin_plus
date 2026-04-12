# Gradle 常用命令指南

本文档整理了 FuStock Plugin 项目中常用的 Gradle 命令，帮助开发者高效构建和管理项目。

## 📋 目录

- [基础命令](#基础命令)
- [构建命令](#构建命令)
- [清理命令](#清理命令)
- [测试命令](#测试命令)
- [插件开发专用](#插件开发专用)
- [常见问题](#常见问题)

---

#### linux指令, windows '/' 替换成 '\'

## 基础命令

### 查看 Gradle 版本
```bash
./gradlew --version
```

### 查看所有可用任务
```bash
./gradlew tasks
```

### 查看特定类型的任务
```bash
./gradlew tasks --all
```

### 刷新依赖
```bash
./gradlew --refresh-dependencies
```

---

## 构建命令

### 编译项目（不打包）
```bash
./gradlew compileJava
```

### 完整构建（编译 + 测试 + 打包）
```bash
./gradlew build
```

### 完整构建（清理 + 编译 + 测试 + 打包）
```bash
./gradlew clean build
```


### 仅编译，跳过测试
```bash
./gradlew build -x test
```

### 构建插件安装包 ⭐
```bash
./gradlew buildPlugin
```
**说明**：这是 IntelliJ Platform Plugin 开发的核心命令，会生成可安装的插件 ZIP 包。

**输出位置**：`build/distributions/fu-stock-plugin-{version}.zip`

### 运行插件（开发模式）
```bash
./gradlew runIde
```
**说明**：启动一个带有插件的 IDEA 实例，用于实时调试。

---

## 清理命令

### 清理构建产物
```bash
./gradlew clean
```

### ⚠️ 注意事项

如果 `clean` 失败，提示文件被占用：

**Windows 系统**：
1. 关闭所有 IDEA 进程
2. 确保没有运行 `runIde` 任务
3. 重新执行 `clean`

**PowerShell 示例**：
```powershell
# 查找占用文件的进程
Get-Process | Where-Object {$_.ProcessName -like "*idea*"}

# 手动关闭 IDEA 后执行
./gradlew clean
```

---

## 测试命令

### 运行所有测试
```bash
./gradlew test
```

### 运行单个测试类
```bash
./gradlew test --tests "cn.fudoc.trade.YourTestClass"
```

### 运行单个测试方法
```bash
./gradlew test --tests "cn.fudoc.trade.YourTestClass.testMethodName"
```

### 生成测试报告
```bash
./gradlew test
# 报告位置：build/reports/tests/test/index.html
```

### 跳过测试
```bash
./gradlew build -x test
```

---

## 插件开发专用

### 🎯 核心工作流

#### 1. 开发阶段 - 实时调试
```bash
./gradlew runIde
```
- 启动带插件的 IDEA
- 支持热重载（部分修改）
- 便于快速验证功能

#### 2. 打包阶段 - 生成安装包
```bash
./gradlew buildPlugin
```
- 生成完整的插件 ZIP 包
- 包含所有依赖和资源
- 可用于分发和安装

#### 3. 清理阶段 - 重置环境
```bash
./gradlew clean
```
- 删除 `build/` 目录
- 清除编译缓存
- 解决某些构建问题

### 验证插件
```bash
./gradlew verifyPlugin
```
**说明**：检查插件配置是否符合 JetBrains 规范。

### 发布插件
```bash
./gradlew publishPlugin
```
**说明**：将插件发布到 JetBrains Marketplace（需要配置 Token）。

---

## 高级用法

### 并行构建（加速）
```bash
./gradlew build --parallel
```

### 显示详细日志
```bash
./gradlew build --info
```

### 显示调试日志
```bash
./gradlew build --debug
```

### 离线模式（无网络）
```bash
./gradlew build --offline
```

### 指定最大堆内存
```bash
./gradlew build -Dorg.gradle.jvmargs="-Xmx4g"
```

---

## Windows PowerShell 注意事项

### ❌ 错误写法
```powershell
gradlew.bat buildPlugin  # 可能找不到命令
gradlew buildPlugin      # 缺少 .\ 前缀
```

### ✅ 正确写法
```powershell
.\gradlew.bat buildPlugin
.\gradlew buildPlugin    # PowerShell 7+ 支持
```

**原因**：PowerShell 出于安全考虑，执行本地脚本需要显式指定路径（`.\` 前缀）。

---

## 常见问题

### Q1: 构建失败，提示 "Could not copy file"

**原因**：IDEA 进程占用了构建目录的文件。

**解决方案**：
1. 关闭所有 IDEA 窗口
2. 确认 `runIde` 任务已停止
3. 执行 `./gradlew clean`
4. 重新构建

### Q2: 编译错误，中文乱码

**解决方案**：在 `build.gradle.kts` 中确保配置了 UTF-8 编码：
```kotlin
tasks.withType<JavaCompile> {
    options.encoding = "UTF-8"
}
```

### Q3: Gradle 版本过旧警告

**更新 Gradle Wrapper**：
```bash
./gradlew wrapper --gradle-version=9.2.1
```

### Q4: 依赖下载缓慢

**使用国内镜像**：已在 `gradle.properties` 中配置阿里云镜像。

如需手动配置，编辑 `settings.gradle.kts`：
```kotlin
pluginManagement {
    repositories {
        maven { url = uri("https://maven.aliyun.com/repository/gradle-plugin") }
        gradlePluginPortal()
    }
}
```

### Q5: 插件安装后未生效

**解决方案**：
1. 完全关闭 IDEA
2. 删除旧插件：Settings → Plugins → FuStock → Uninstall
3. 安装新插件：Install Plugin from Disk → 选择 `build/distributions/*.zip`
4. 重启 IDEA
5. 如仍有问题：File → Invalidate Caches → Invalidate and Restart

---

## 快捷命令别名（可选）

在 PowerShell Profile 中添加别名（`$PROFILE`）：

```powershell
# Gradle 快捷命令
Set-Alias gw '.\gradlew.bat'
Set-Alias gwb 'gw build'
Set-Alias gwp 'gw buildPlugin'
Set-Alias gwc 'gw clean'
Set-Alias gwt 'gw test'
Set-Alias gwr 'gw runIde'
```

**使用方法**：
```powershell
gwp    # 等同于 .\gradlew.bat buildPlugin
gwr    # 等同于 .\gradlew.bat runIde
```

---

## 项目特定配置

### 当前项目 Gradle 版本
```
Gradle: 9.2.1
JVM: 21
IntelliJ Platform: 2024.3.6
```

### 构建输出位置
```
build/
├── classes/          # 编译后的类文件
├── distributions/    # 插件安装包 ⭐
│   └── fu-stock-plugin-2.0.zip
├── libs/            # JAR 包
├── reports/         # 测试报告
└── tmp/             # 临时文件
```

### 配置文件位置
```
gradle/
├── wrapper/
│   └── gradle-wrapper.properties  # Gradle 版本配置
└── libs.versions.toml             # 依赖版本管理

gradle.properties                  # Gradle 全局配置
build.gradle.kts                   # 主构建脚本
settings.gradle.kts                # 项目设置
```

---

## 参考资源

- [Gradle 官方文档](https://docs.gradle.org/)
- [IntelliJ Platform Plugin SDK](https://plugins.jetbrains.com/docs/intellij/welcome.html)
- [Gradle Wrapper 使用指南](https://docs.gradle.org/current/userguide/gradle_wrapper.html)

---

**最后更新**：2026-04-11  
**维护者**：FuStock Development Team
