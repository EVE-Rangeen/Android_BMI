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

## 分支工作流（main → cityu_lab）

`main` 是日常开发分支，保持原来的旧工具链（AGP 7.3.1），可以继续在本地旧版
Android Studio 里打开；`cityu_lab` 只负责“学校电脑上能跑”的适配，**不要直接
在这个分支上写业务代码**，否则以后每次合并都会更麻烦。

每做完一次作业，按这个顺序操作：

```bat
git checkout main
git add -A
git commit -m "assignment04"
git push origin main

git checkout cityu_lab
git pull
git merge main
```

合并时可能冲突的只有构建配置文件，处理规则如下：

| 冲突文件 | 处理方式 |
| --- | --- |
| `build.gradle` | 用 `cityu_lab` 的（必须保持 AGP 8.13.0 / Kotlin 2.2.20） |
| `app/build.gradle` | `plugins`、`android {}` 用 `cityu_lab` 的；`dependencies {}` 里 `main` 新加的库要保留 |
| `settings.gradle` | 用 `cityu_lab` 的（镜像仓库配置） |
| `.gitignore`、`gradle/wrapper/*`、`gradlew*` | 用 `cityu_lab` 的 |

源码、布局、`strings.xml` 这些业务文件两个分支本来就一样，正常都会自动合并，
不需要处理。

新作业如果加了新依赖，需要注意：

- 要求 compileSdk 33 ~ 36 的库：没问题（这里已经是 36）
- 要求更高 minSdk（例如 24）的库：把 `android { defaultConfig { minSdk } }`
  一起改掉
- 要求 compileSdk 37 或 AGP 9.x 的库：学校那台 Studio 2025.2.1 不支持
  （它最高只到 AGP 8.13 / API 36.1），需要换一个低版本的库

合并完成后，先本地验证一次再推送（本地旧版 Studio 打不开这个分支，用命令行）：

```bat
set JAVA_HOME=<本机 JDK 17 或更高的路径>
gradlew.bat assembleDebug
```

成功之后推送：

```bat
git push origin cityu_lab
```

学校电脑上只需要在 `cityu_lab` 分支上 `git pull`，然后 Sync + Run 即可。

## 已知限制

- 本项目使用的是 AGP 8.13，只能由 Android Studio 2025.2.1 或更新的版本打开。
  更老的 Studio（例如 2021.3 / Dolphin、2022.x）会提示 AGP 版本不支持，
  这时只能用上面的命令行方式构建。
- `targetSdk` 特意保留为 34：如果设成 35 及以上，在 Android 15+ 上系统会强制
  边到边（edge-to-edge）绘制，这个作业原本的布局会变形。
