# BMI

一个 Android BMI 计算小应用（作业项目）。本分支 `cityu_lab` 已调整为可直接在
**学校的 Android Studio 2025.2.1 Patch 1（Otter）** 上打开并运行。

## 工具链版本

| 组件 | 版本 | 说明 |
| --- | --- | --- |
| Android Studio | 2025.2.1（Otter）Patch 1 | 支持 AGP 4.0 ~ 8.13 |
| Android Gradle Plugin | 8.13.0 | 该 Studio 版本支持的最高 AGP |
| Gradle | 8.13 | AGP 8.13 要求的 Gradle |
| Kotlin | 2.2.20 | |
| Gradle JDK | 17 及以上 | 用 Studio 自带的 JBR 21 即可，无需手动配置 |
| compileSdk / targetSdk / minSdk | 36 / 34 / 21 | |
| Java / Kotlin 字节码目标 | 17 | 需要 Studio 自带的 JDK 17+ |

## 第一次打开

1. 从 GitHub 下载 ZIP（或 `git clone`），解压到本地任意目录。
   路径里尽量不要有中文和空格，例如 `D:\AndroidProjects\BMI`。
2. Android Studio → **Open** → 选择**包含 `settings.gradle` 的那一层目录**
   （不要选里面的 `app` 目录）。
3. 首次打开会自动下载 Gradle 8.13 和所有依赖，需要联网，请耐心等待。
4. 如果右下角 / 通知栏提示缺少 SDK 组件（例如 *Android SDK Platform 36*、
   *Build-Tools 35.0.0*），直接点 **Install** 让它自动安装。
5. Gradle Sync 结束后，选中 `app` 运行配置，点 **Run** 即可。

`local.properties`（本机 SDK 路径）和 `.idea/`（IDE 配置）都没有提交到仓库，
Android Studio 打开项目时会自动生成，不需要手动创建。

## 校园网访问不了 Google / Maven 时

依赖（androidx、Material、AGP、Kotlin 插件）走的是 `google()` 和
`mavenCentral()`。如果一直卡在下载，可以打开 `settings.gradle`，
把里面被注释掉的阿里云镜像取消注释（两处：`pluginManagement` 和
`dependencyResolutionManagement`），然后重新 Sync。

Gradle 发行版本身也要下载。如果 `services.gradle.org` 很慢或连不上，把
`gradle/wrapper/gradle-wrapper.properties` 里的

```
distributionUrl=https\://services.gradle.org/distributions/gradle-8.13-bin.zip
```

换成腾讯镜像：

```
distributionUrl=https\://mirrors.cloud.tencent.com/gradle/gradle-8.13-bin.zip
```

再重新 Sync。下载过一次之后 Gradle 会缓存到 `~/.gradle`，以后不再重复下载。

## 命令行构建（可选）

```bat
gradlew.bat assembleDebug
```

生成的文件在 `app/build/outputs/apk/debug/app-debug.apk`。

## 已知限制

- 本项目使用的是 AGP 8.13，只能由 Android Studio 2025.2.1 或更新的版本打开。
  更老的 Studio（例如 2021.3 / Dolphin、2022.x）会提示 AGP 版本不支持，
  这时只能用上面的命令行方式构建。
- `targetSdk` 特意保留为 34：如果设成 35 及以上，在 Android 15+ 上系统会强制
  边到边（edge-to-edge）绘制，这个作业原本的布局会变形。
