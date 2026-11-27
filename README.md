# Wellness Tracker（健康日志）

一个 Android 考试练习项目：每天记录身体状况、步数和备注，保存到 SQLite，
并在历史页按日期查看全部记录。在基础功能之上，还加了两块上课学过的内容：
App 内中英文切换 和 AI 健康建议（DeepSeek）。

本项目从原始的 BMI 作业（`cityu_lab` 分支）改造而来，保留了学校电脑要求的
工具链配置，源码已经清理成只包含健康日志功能；BMI 相关代码仍然是删除状态。
改造过程、逐文件讲解、可迁移到同类题目的考场教程见文末文档。

两块可选功能都可以整块删除，删除清单见 [`docs/exam-guide.md`](docs/exam-guide.md)。

## 功能

**基础功能（题目要求）**

- 自动显示当天日期，不需要手输
- 选择身体状况：Good / Fair / Poor（中文：良好 / 一般 / 较差）
- 输入当天步数，做非空、非负校验
- 填写备注（可留空）
- 保存到 SQLite；同一天重复保存会覆盖旧记录
- 历史页按日期倒序显示全部记录，并统计记录天数和平均步数
- 中英文两套资源（`values/` 与 `values-zh-rCN/`）

**可选功能 A：App 内中英文切换**

- 右上角菜单 → “Switch language / 切换语言”，整个 App 立刻换语言
- 选择用 SharedPreferences 记住，下次打开保持
- 没手动选过时，跟随手机系统语言

**可选功能 B：AI 健康建议**

- 右上角菜单 → “AI advice / AI 健康建议”
- 取今天的记录，发给 DeepSeek，显示 3 条可执行的建议
- 回答语言跟随界面语言：中文界面拿到中文建议，英文界面拿到英文建议
- 失败时显示原因（没配 key / 网络错误 / 接口错误）并支持重试

## 代码结构

```
app/src/main/java/com/example/bmi/
├── MainActivity.kt              # 主界面：输入、校验、保存、菜单
├── HistoryActivity.kt           # 历史页：读库、空状态、ListView
├── BaseActivity.kt              # 【可选 A】语言切换：attachBaseContext
├── AdviceActivity.kt            # 【可选 B】AI 页：取记录、请求、渲染状态
├── data/
│   ├── WellnessRecord.kt        # 数据类：日期/状况/步数/备注
│   ├── WellnessDbHelper.kt      # SQLite：建表、保存、读取
│   ├── Dates.kt                 # 日期工具：今天的 yyyy-MM-dd
│   ├── AppLanguage.kt           # 【可选 A/B】en / zh 枚举
│   ├── LanguageStore.kt         # 【可选 A】SharedPreferences 记语言
│   ├── PromptBuilder.kt         # 【可选 B】拼提示词（纯 Kotlin，可单测）
│   ├── DeepSeekClient.kt        # 【可选 B】HTTP + JSON 客户端
│   └── WellnessAdviceRepository.kt  # 【可选 B】切 IO 线程、统一异常
└── ui/advice/
    ├── AdviceUiState.kt         # 【可选 B】Idle/Loading/Success/Error
    └── AdviceViewModel.kt       # 【可选 B】ViewModel + LiveData + 重试
```

只有 AI 功能需要 `INTERNET` 权限和 API key；基础功能不联网、不需要权限。

## 工具链版本

| 组件 | 版本 | 说明 |
| --- | --- | --- |
| Android Studio | 2025.2.1（Otter）Patch 1 | 最高支持到 AGP 8.13 |
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
   *Build-Tools 36.x*），直接点 **Install** 让它自动安装。
5. Gradle Sync 结束后，选中 `app` 运行配置，点 **Run** 即可。

`local.properties`（本机 SDK 路径）和 `.idea/`（IDE 配置）都没有提交到仓库，
Android Studio 打开项目时会自动生成，不需要手动创建。

### 想用 AI 功能：配置 DeepSeek key（可选）

AI 页面需要一个 API key。在项目根目录的 `local.properties` 里加一行：

```properties
deepseek.apiKey=sk-你的key
```

然后重新 Sync + Run（key 是编译期注入 `BuildConfig` 的，改完必须重新编译）。
不配也能正常使用记录和历史功能，只是 AI 页会提示“没有配置 API key”。

## 考试时用哪一版

同一份代码可以按题目要求删掉可选功能，删除清单写得很具体：

| 题目要求 | 操作 |
| --- | --- |
| 只要记录 + 历史 | 按 `docs/exam-guide.md` 第 1、2 节删除 |
| 不要 App 内切换语言 | 按 `docs/exam-guide.md` 第 1 节删除 |
| 不要 AI | 按 `docs/exam-guide.md` 第 2 节删除 |
| 都要 | 直接用当前版本 |

建议先 `git checkout -b exam-minimal` 再动手，删坏了 `git checkout -- .` 就能恢复。

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

- [`docs/from-scratch.md`](docs/from-scratch.md)
  —— **从零建工程手册**：在空工程里按顺序把功能一块块加出来，每步都有验收点；
  还写了学校版本 / 自己电脑版本的差异与踩坑清单。
- [`docs/adapt-to-other-topic.md`](docs/adapt-to-other-topic.md)
  —— **改题点地图**：题目换成喝水/心情/记账时，改哪一处、连带改哪里，含三个完整示例。
- [`docs/exam-guide.md`](docs/exam-guide.md)
  —— **考试删减指南**：不要中英文切换 / 不要 AI 时，具体删哪些文件、哪些代码。
- [`docs/wellness-tracker.md`](docs/wellness-tracker.md)
  —— 本次改造的逐文件说明、数据库设计、patch 用法、自测清单。
- [`docs/llm-setup.md`](docs/llm-setup.md)
  —— AI 健康建议与语言切换的实现、API key 配置、单元测试说明。
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
