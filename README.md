# Wellness Tracker（健康日志）

一个 Android 考试练习项目：每天记录身体状况、步数和备注，保存到 SQLite，
并在历史页按日期查看全部记录。

本项目从原始的 BMI 作业（`cityu_lab` 分支）改造而来，保留了学校电脑要求的
工具链配置，但源码已经清理成只包含健康日志功能。改造过程、逐文件讲解和
可迁移到同类题目的考场教程见文末文档。

## 功能

- 自动显示当天日期，不需要手输
- 选择身体状况：Good / Fair / Poor（中文：良好 / 一般 / 较差）
- 输入当天步数，做非空、非负校验
- 填写备注（可留空）
- 保存到 SQLite；同一天重复保存会覆盖旧记录
- 历史页按日期倒序显示全部记录，并统计记录天数和平均步数
- 支持中英文资源（`values/` 与 `values-zh-rCN/`）

## 代码结构

```
app/src/main/java/com/example/bmi/
├── MainActivity.kt              # 主界面：输入、校验、保存、跳转
├── HistoryActivity.kt           # 历史页：读库、空状态、ListView
└── data/
    ├── WellnessRecord.kt        # 数据类：日期/状况/步数/备注
    ├── WellnessDbHelper.kt      # SQLite：建表、保存、读取
    └── Dates.kt                 # 日期工具：今天的 yyyy-MM-dd
```

没有网络请求、没有 AI、没有图表，也不需要任何权限。

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
   路径里尽量不要有中文和空格，例如 `D:\AndroidProjects\Wellness`。
2. Android Studio → **Open** → 选择**包含 `settings.gradle` 的那一层目录**
   （不要选里面的 `app` 目录）。
3. 首次打开会自动下载 Gradle 8.13 和所有依赖，需要联网，请耐心等待。
4. 如果提示缺少 SDK 组件（例如 *Android SDK Platform 36*、
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

## 考试补丁

`patch/wellness-tracker.patch` 记录的是从 `cityu_lab` 的提交 `d43a3c9`
到本分支的全部改动，可以直接应用到你已提交的作业代码上：

```bat
git checkout -b exam d43a3c9
git apply --check patch/wellness-tracker.patch
git apply patch/wellness-tracker.patch
```

## 文档

- [`docs/wellness-tracker.md`](docs/wellness-tracker.md)
  —— 本次改造的逐文件说明、数据库设计、patch 用法、自测清单。
- [`docs/exam-playbook.md`](docs/exam-playbook.md)
  —— 把 BMI 作业改成任意“每日记录类”题目的七步教程、变体对照表、常见错误。
- [`docs/开发指南.md`](docs/开发指南.md)
  —— 原始 BMI 作业版本的概念讲解，保留作 Android 知识点参考
  （文中提到的 BMI 源码已在本分支删除）。

## 已知限制

- 本项目使用的是 AGP 8.13，只能由 Android Studio 2025.2.1 或更新的版本打开。
  更老的 Studio 会提示 AGP 版本不支持，这时可以用命令行构建。
- `targetSdk` 特意保留为 34：如果设成 35 及以上，在 Android 15+ 上系统会强制
  边到边（edge-to-edge）绘制，界面可能变形。
- 同一天只保留一条记录。如果考试要求一天多条，把 `date` 主键换成自增 `id`，
  详见 `docs/exam-playbook.md` 的“按天唯一 vs 一天多条”。
