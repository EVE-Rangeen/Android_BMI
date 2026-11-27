# 从零创建一个 Wellness Tracker（考试模拟手册）

这份文档教你在一个**全新的空工程**里，按顺序把功能一块块加出来，最终到达
`wellness_tracker` 分支现在的状态。用法是：左边开着 GitHub 上这个分支的代码，
右边新建工程，按本文的顺序复制、修改、验证。

配套文档：

- `docs/adapt-to-other-topic.md` — 题目换了名字/字段时，改代码的哪一处、连带改哪里。
- `docs/exam-guide.md` — 考场不需要某个功能时，删哪些文件。
- `docs/wellness-tracker.md` — 每个文件的作用和 SQLite 设计。

**核心原则：一次只加一层，每层都编译并运行一次。** 考试现场最怕的不是写不出来，
而是写到一半编译不过、又不知道是哪一步引入的。

---

## 0. 开始前的准备

### 0.1 包名（`applicationId` / `namespace` / `package`）

本仓库用的是 `com.example.bmi`（沿用作业，题目不检查包名）。

- **建议：新工程直接起名 `BMI`、包名 `com.example.bmi`。**
  这样所有文件的 `package com.example.bmi` 和 `R` 引用都能原样粘贴，零改动。
- 如果起了别的包名（例如 `com.example.wellness`），粘贴代码前必须先改每个
  Kotlin 文件第一行的 `package`，并且 Android Studio 可能提示 `R` 找不到。
  改包名的正确姿势：右键包名 → **Refactor → Rename**，勾选
  `Search in comments and strings`，而不是手动一个个改。

### 0.2 构建脚本语言：先看你的向导生成了什么

不同版本的 Android Studio 新建工程时，生成的构建文件不一样：

| 向导生成 | 说明 | 处理方式 |
| --- | --- | --- |
| `build.gradle` + `settings.gradle` | Groovy DSL，本仓库就是这种 | 直接粘贴仓库的构建文件（见 1.2） |
| `build.gradle.kts` + `settings.gradle.kts` + `gradle/libs.versions.toml` | Kotlin DSL + 版本目录，较新版本默认 | 二选一：**推荐**删掉 `.kts` 和 `libs.versions.toml`，改用仓库的 Groovy 版本；或者保留 `.kts`，按下面 0.4 的对照表改写 |

先打开工程根目录看一眼，确定属于哪种，再往下做。

> 判断方法：Android Studio 左侧 Project 视图切到 **Project** 模式，
> 根目录有没有 `build.gradle.kts`。

### 0.3 从 GitHub 网页复制代码的正确姿势

考试时你只能看仓库（多半是网页版），所以复制这一步也有讲究：

1. **点文件名 → 点右上角 `Raw`**，在纯文本页面 `Ctrl+A`、`Ctrl+C`。
   直接在代码高亮页面上拖选，容易把行号、折叠箭头一起复制进去。
2. 复制完先看**第一行**：`.kt` 文件必须从 `package com.example.bmi` 开始；
   `.xml` 文件必须从 `<?xml version="1.0" encoding="utf-8"?>` 开始。
   如果第一行是别的（比如网页标题），说明你复制多了。
3. **一次只复制一个文件**，粘进 Android Studio 后立刻编译一次。
   一次复制五个文件再编译，报错时很难判断是哪一个引入的。
4. 不要下载 ZIP 后整个覆盖到工程里：ZIP 里没有 `.idea` 和 `local.properties`，
   但会用仓库的 `settings.gradle`、主题名等覆盖你向导生成的配置，
   容易出现“明明代码一样却跑不起来”。
5. 中文注释在网页上复制一般是正常的；如果粘进去变乱码，先把
   **File → File Encodings** 全部设为 UTF-8，再重新粘贴。

### 0.4 如果坚持用 Kotlin DSL：对照表

本仓库所有片段都是 Groovy。若你的工程是 `.kts`，按下表改：

| Groovy（本仓库） | Kotlin DSL（`.kts`） |
| --- | --- |
| `id 'com.android.application' version '8.13.0' apply false` | `id("com.android.application") version "8.13.0" apply false` |
| `compileSdk = 36` | 同左（本来就是赋值语法） |
| `applicationId = "com.example.bmi"` | 同左 |
| `buildConfigField "String", "X", "\"v\""` | `buildConfigField("String", "X", "\"v\"")` |
| `buildFeatures { buildConfig = true }` | 同左 |
| `implementation 'androidx.core:core-ktx:1.13.1'` | `implementation("androidx.core:core-ktx:1.13.1")` |
| `sourceCompatibility = JavaVersion.VERSION_17` | 同左 |

读 `local.properties` 那段，`.kts` 写法：

```kotlin
val localProperties = java.util.Properties().apply {
    val file = rootProject.file("local.properties")
    if (file.exists()) file.inputStream().use { load(it) }
}
val deepSeekApiKey: String = localProperties.getProperty("deepseek.apiKey") ?: ""
```

**注意**：`.kts` 工程如果还留着 `gradle/libs.versions.toml`，而你把 `.kts` 删了改
Groovy，记得把 `libs.versions.toml` 一起删掉，否则 Sync 时会因为“版本目录没有
被任何构建文件使用”而不报错但让人困惑。反过来，保留 `.kts` 就别混用 Groovy 片段。

---

## 1. 新建工程并对齐构建配置

### 1.1 新建

1. **File → New → New Project**；
2. 模板选 **Empty Views Activity**（不是 “Empty Activity”，那个是 Compose）；
3. 参数：

| 项 | 建议值 | 说明 |
| --- | --- | --- |
| Name | `BMI` | 工程名，随意，不影响代码 |
| Package name | `com.example.bmi` | **建议照抄**，省去改包名 |
| Language | Kotlin | 本仓库是 Kotlin |
| Minimum SDK | API 21 | 仓库 `minSdk = 21`，选 21 或 24 都能跑 |
| Build configuration language | 能选 Groovy 就选 Groovy | 不能选就按 0.2/0.4 处理 |

4. 等 Sync 完成，点 **Run**，确认能看到模板自带的 “Hello World!”。
   **这一步必须先通过**，否则后面出问题分不清是谁的锅。

### 1.2 对齐四个构建文件

要让仓库的代码能直接粘贴，下面四个文件必须一起对齐（缺一个就会版本错配）：

| 文件 | 作用 | 关键内容 |
| --- | --- | --- |
| `settings.gradle` | 仓库地址、模块列表、项目名 | `include ':app'` |
| `build.gradle`（项目级） | 插件版本 | AGP `8.13.0`、Kotlin `2.2.20` |
| `gradle/wrapper/gradle-wrapper.properties` | Gradle 版本 | `gradle-8.13-bin.zip` |
| `app/build.gradle`（模块级） | SDK 版本、依赖、BuildConfig | `compileSdk 36`、`targetSdk 34`、`minSdk 21`、Java 17 |

为什么必须四个一起：**AGP 8.13 要求 Gradle ≥ 8.13、JDK ≥ 17、compileSdk 支持 36**。
只换 `app/build.gradle` 而不换 wrapper，会出现
“需要 Gradle 8.13，但当前是 8.x 其他版本”之类的报错。

操作：

1. 从 GitHub 打开这四个文件，**全选复制**，粘贴覆盖你工程里的同名文件；
2. `settings.gradle` 里的 `rootProject.name = "BMI"` 不用改成你的工程名（改名无害）；
3. Sync（大象按钮）→ 只允许出现 “warning”，不能有 “error”。

> `gradlew` / `gradlew.bat` 这两个脚本一般不用换，它们只负责“启动 wrapper”。
> 但如果你的向导工程用的是很新的 Gradle（例如 9.x），建议连这两个脚本一起从仓库复制，
> 保持与 `gradle-wrapper.properties` 配套。

### 1.3 验收

- [ ] Sync 成功；
- [ ] `Run` 能起模板界面；
- [ ] Build 面板里 Gradle 版本显示 8.13、AGP 8.13.0。

---

## 2. 资源层：先把“文字”准备好

先做资源的好处：布局和代码引用 `@string/xxx` 时，不会一路红波浪线。

### 2.1 三套资源

| 仓库文件 | 你工程里的位置 | 内容 |
| --- | --- | --- |
| `app/src/main/res/values/strings.xml` | 覆盖同名文件 | 英文文案 + 状况数组 |
| `app/src/main/res/values-zh-rCN/strings.xml` | 新建目录 `values-zh-rCN`，放进去 | 中文文案，key 必须与英文完全一致 |
| `app/src/main/res/values/colors.xml`、`themes.xml`、`values-night/themes.xml` | 覆盖同名文件 | 主题 `Theme.Wellness` |

`values-zh-rCN` 目录要手动建：右键 `res` → **New → Android Resource Directory** →
Resource type 选 `values`，Source set 选 `main`，然后在 Available qualifiers 里选
**Locale** → 语言 `zh`，Region `CN`。

### 2.2 主题必须一起换（容易被忽略）

新建向导可能给你 `Theme.Material3.DayNight.NoActionBar` 这类 Material3 主题。
本仓库的布局里用到了 `Widget.MaterialComponents.Button.OutlinedButton` 这个样式，
主题文件里还引用了 `?attr/colorPrimaryVariant`（Material3 没有这个属性）。
所以：

- **直接用仓库的 `values/themes.xml` + `values-night/themes.xml` + `colors.xml` 覆盖**；
- 打开 `AndroidManifest.xml`，把 `android:theme="@style/..."` 改成
  `@style/Theme.Wellness`（仓库里已经是这个名字）。

### 2.3 验收

- [ ] `strings.xml` 没有红色报错（两个文件的 key 完全对应）；
- [ ] Manifest 里的 `@style/Theme.Wellness` 能点进去；
- [ ] 编译通过（此时还没有代码引用这些字符串，属于正常）。

---

## 3. 数据层：先做“能存”的部分

顺序是 **数据类 → 日期工具 → 数据库助手**，每一层只依赖上一层。

### 3.1 新建包与数据类

1. 在 `app/src/main/java/com/example/bmi/` 下新建包 `data`
   （右键 `com.example.bmi` → New → Package）；
2. 复制 `data/WellnessRecord.kt`；
3. 复制 `data/Dates.kt`。

### 3.2 数据库助手

复制 `data/WellnessDbHelper.kt`。这个文件里有三块要理解（也是 `【改题点 C2】`）：

```kotlin
// 建表：date 是主键 → 一天只有一条
CREATE TABLE wellness_records (
    date TEXT PRIMARY KEY NOT NULL,
    condition INTEGER NOT NULL,
    steps INTEGER NOT NULL,
    note TEXT NOT NULL,
    updated_at INTEGER NOT NULL
)
```

```kotlin
// 保存：同一天再存会替换旧行
insertWithOnConflict(TABLE_RECORDS, null, values, SQLiteDatabase.CONFLICT_REPLACE)
```

```kotlin
// 查询：按日期倒序，最新在最上面
query(TABLE_RECORDS, columns, null, null, null, null, "$COL_DATE DESC")
```

### 3.3 验收

- [ ] 编译通过；
- [ ] 没有 UI 变化（这是正常的，数据层还没有人调用）。

---

## 4. 主界面：输入 + 保存

### 4.1 布局

覆盖 `app/src/main/res/layout/activity_main.xml`（向导生成的 ConstraintLayout 版本整份替换）。
控件和 id 必须与代码一致：

| id | 控件 | 作用 |
| --- | --- | --- |
| `dateTV` | TextView | 显示今天日期 |
| `conditionSpinner` | Spinner | 身体状况，`entries` 指向 `@array/wellness_condition` |
| `stepsET` | EditText（`inputType="number"`） | 步数 |
| `noteET` | EditText（多行） | 备注 |
| `saveBtn` | Button | 保存 |
| `historyBtn` | Button（描边样式） | 查看历史 |

### 4.2 代码

复制 `MainActivity.kt`。它的结构是：

```
onCreate()
 ├── setContentView
 ├── title = getString(app_name)        ← 可选 A（语言切换）才需要
 ├── db = WellnessDbHelper(this)
 ├── findViewById 拿到 6 个控件
 ├── dateTV.text = getString(wellness_today, Dates.today())
 ├── saveBtn.setOnClickListener { saveTodayRecord() }
 └── historyBtn.setOnClickListener { startActivity(...HistoryActivity) }

saveTodayRecord()
 ├── toIntOrNull() 校验
 ├── 组装 WellnessRecord
 └── db.save(record) + Toast

onCreateOptionsMenu / onOptionsItemSelected   ← 可选 A/B 才需要，见第 6 节
```

### 4.3 验收

- [ ] 界面上能看到“今天：日期”、下拉框、两个输入框、两个按钮；
- [ ] 不填步数直接保存 → 出现“请输入有效的步数”；
- [ ] 填 8000 保存 → 出现“已保存今天的记录”；
- [ ] 点“查看历史” → 因为还没做历史页，先建一个空 Activity 占位，
      或者先跳过这一步（下一步马上做）。

---

## 5. 历史页：读库 + 列表

### 5.1 三个文件

| 仓库文件 | 作用 |
| --- | --- |
| `res/layout/activity_history.xml` | 页面骨架：统计文字、空状态文字、ListView |
| `res/layout/item_wellness.xml` | 列表“一行”的布局：`itemTitle` + `itemSubtitle` |
| `HistoryActivity.kt` | 查库、算平均、组装 Map、`SimpleAdapter` |

`activity_history.xml` 的 id：`historySummary`、`historyEmpty`、`historyList`。

### 5.2 在 Manifest 里注册

```xml
<activity
    android:name=".HistoryActivity"
    android:exported="false"
    android:label="@string/wellness_history_title" />
```

`exported="false"` 的含义：只允许本 App 内部跳转，不允许别的 App 直接启动它。

### 5.3 验收

- [ ] 保存一条记录后打开历史 → 看到“日期 · 状况”“步数 · 备注”；
- [ ] 同一天再保存一次 → 仍然只有一条；
- [ ] 没有任何记录时 → 显示“暂无记录”。

> 到这里，**题目要求的基础功能已经全部完成**。后面的第 6、7 节是可选的加分项，
> 考试不需要就跳过，或者做完再按 `docs/exam-guide.md` 删掉。

---

## 6. 可选 A：App 内中英文切换

顺序很重要：**先有资源（第 2 步已做）→ 再有语言偏好 → 最后才是菜单入口。**

### 6.1 三个文件

| 文件 | 作用 |
| --- | --- |
| `data/AppLanguage.kt` | `ENGLISH("en")` / `CHINESE("zh")` + `fromTag()` |
| `data/LanguageStore.kt` | SharedPreferences 读写语言选择 |
| `BaseActivity.kt` | `attachBaseContext` 换 Locale；`switchLanguage()` 保存 + `recreate()` |

### 6.2 三个 Activity 改成继承 BaseActivity

```kotlin
class MainActivity : BaseActivity() {
class HistoryActivity : BaseActivity() {
class AdviceActivity : BaseActivity() {
```

同时删掉 `import androidx.appcompat.app.AppCompatActivity`（`BaseActivity` 在同一个包，
不需要 import）。

### 6.3 菜单

复制 `res/menu/menu_main.xml`，在 `MainActivity` 里加：

```kotlin
override fun onCreateOptionsMenu(menu: Menu): Boolean {
    menuInflater.inflate(R.menu.menu_main, menu)
    return true
}

override fun onOptionsItemSelected(item: MenuItem): Boolean = when (item.itemId) {
    R.id.menu_language -> { toggleLanguage(); true }
    R.id.menu_ai_advice -> {
        startActivity(Intent(this, AdviceActivity::class.java)); true
    }
    else -> super.onOptionsItemSelected(item)
}
```

### 6.4 标题栏

在每个 Activity 的 `onCreate` 里加一行：

```kotlin
title = getString(R.string.app_name)     // 历史页用 wellness_history_title，AI 页用 ai_title
```

原因：标题栏默认取 Manifest 的 `android:label`，那是“系统”按手机语言解析的，
App 内切换语言后不会自动跟着变。

### 6.5 验收

- [ ] 菜单点“切换语言” → 整页变英文，标题也变；
- [ ] 再点一次 → 变回中文；
- [ ] 杀掉进程重开 → 保持上次的语言；
- [ ] 在系统设置里“清除数据” → 恢复跟随系统语言。

---

## 7. 可选 B：AI 健康建议

顺序：**先能拼提示词（可单测）→ 再能发请求 → 最后才是页面。**

### 7.1 构建配置

1. 在 `local.properties` 里加一行（这个文件不进 Git）：

```properties
deepseek.apiKey=sk-你的key
```

2. `app/build.gradle` 里要有三块：

```gradle
// ① android { } 外面：读 local.properties
def localProperties = new Properties()
def localPropertiesFile = rootProject.file('local.properties')
if (localPropertiesFile.exists()) {
    localPropertiesFile.withInputStream { stream -> localProperties.load(stream) }
}
def deepSeekApiKey = localProperties.getProperty('deepseek.apiKey') ?: ''

// ② defaultConfig 里：注入 BuildConfig 常量
buildConfigField "String", "DEEPSEEK_API_KEY", "\"${deepSeekApiKey}\""
buildConfigField "String", "DEEPSEEK_BASE_URL", "\"https://api.deepseek.com\""

// ③ android { } 里：打开 BuildConfig 生成
buildFeatures { buildConfig = true }
```

3. 依赖（4 个）：`activity-ktx`、`lifecycle-viewmodel-ktx`、`lifecycle-livedata-ktx`、
   `kotlinx-coroutines-android`。

### 7.2 Manifest

```xml
<uses-permission android:name="android.permission.INTERNET" />

<activity
    android:name=".AdviceActivity"
    android:exported="false"
    android:label="@string/ai_title" />
```

漏掉 `INTERNET` 的典型现象：点开 AI 页立刻闪退（`SecurityException`）。

### 7.3 从纯 Kotlin 开始（可以先不碰界面）

1. `data/PromptBuilder.kt` —— 拼 system / user 提示词，不 import 任何 Android 类；
2. 复制 `app/src/test/java/com/example/bmi/PromptBuilderTest.kt`；
3. 跑 `gradlew.bat testDebugUnitTest`，7 个用例应当全绿。

这一步就能验证“语言 → 提示词”的逻辑对不对，不用等界面。

### 7.4 再接通网络

| 顺序 | 文件 | 职责 |
| --- | --- | --- |
| 1 | `data/DeepSeekClient.kt` | `HttpURLConnection` 发 POST、解析 JSON |
| 2 | `data/WellnessAdviceRepository.kt` | `withContext(Dispatchers.IO)` + 统一异常 |
| 3 | `ui/advice/AdviceUiState.kt` | `Idle/Loading/Success/Error` + 错误原因 |
| 4 | `ui/advice/AdviceViewModel.kt` | `viewModelScope` 发请求、`LiveData`、`retry()` |

### 7.5 最后是页面

| 文件 | 作用 |
| --- | --- |
| `res/layout/activity_advice.xml` | 摘要 + 转圈 + 建议文字 + 重试按钮 |
| `AdviceActivity.kt` | 查今天记录 → 发请求 → 按状态渲染 |

### 7.6 验收

- [ ] 没有今天的记录时进 AI 页 → 提示“今天还没有记录”，不发请求；
- [ ] 有记录 → 转圈 → 出现 3 条建议；
- [ ] 中文界面拿到中文建议，英文界面拿到英文建议；
- [ ] 断网 → 提示网络错误 + 出现“重试”按钮；
- [ ] 转屏 → 不重复请求。

---

## 8. 和仓库逐文件对齐

做完后，对着下表检查（左边是仓库，右边是你工程）：

| 仓库路径 | 你工程里应当有 |
| --- | --- |
| `app/src/main/java/com/example/bmi/MainActivity.kt` | ✅ |
| `.../HistoryActivity.kt` | ✅ |
| `.../BaseActivity.kt`（可选） | ✅ 或按 `exam-guide.md` 删掉 |
| `.../AdviceActivity.kt`（可选） | ✅ 或删掉 |
| `.../data/Dates.kt`、`WellnessRecord.kt`、`WellnessDbHelper.kt` | ✅ |
| `.../data/AppLanguage.kt`、`LanguageStore.kt`（可选） | ✅ 或删掉 |
| `.../data/PromptBuilder.kt`、`DeepSeekClient.kt`、`WellnessAdviceRepository.kt`（可选） | ✅ 或删掉 |
| `.../ui/advice/`（可选） | 两文件，或整个目录删掉 |
| `res/layout/activity_main.xml`、`activity_history.xml`、`item_wellness.xml` | ✅ |
| `res/layout/activity_advice.xml`（可选） | ✅ 或删掉 |
| `res/menu/menu_main.xml`（可选） | ✅ 或删掉 |
| `res/values/strings.xml`、`values-zh-rCN/strings.xml` | ✅ 两套 |
| `res/values/themes.xml`、`values-night/themes.xml`、`colors.xml` | ✅ |
| `app/src/test/.../PromptBuilderTest.kt`（可选） | ✅ 或删掉 |
| `AndroidManifest.xml` | 注册了对应页面 +（AI 才有）INTERNET |
| `app/build.gradle`、`build.gradle`、`settings.gradle` | 与本仓库同版本 |

验证命令：

```bat
gradlew.bat clean assembleDebug
gradlew.bat lintDebug
gradlew.bat testDebugUnitTest
```

---

## 9. 版本与环境：会不会冲突，哪里要改

### 9.1 结论：学校电脑（Android Studio 2025.2.1 Patch 1）上不需要改代码

本项目就是按学校那台机器的工具链写的，**源码层面没有问题**，逐项对照：

| 项目 | 本仓库 | 2025.2.1 Patch 1 是否支持 | 结论 |
| --- | --- | --- | --- |
| AGP | 8.13.0 | 支持（该版本支持 AGP 4.0 ~ 8.13） | 不用改 |
| Gradle | 8.13（wrapper 指定） | 会按 wrapper 自动下载 | 不用改 |
| Kotlin | 2.2.20 | 从 Maven 下载插件，与 IDE 版本无关 | 不用改 |
| JDK | 17 字节码，需要 JDK ≥ 17 | Studio 自带 JBR 21 | 不用改 |
| compileSdk | 36 | 该 Studio 默认装 36 | 不用改（前提是机器上装了 Platform 36） |
| targetSdk | 34 | 支持 | 不用改 |
| minSdk | 21 | 支持 | 不用改 |
| Material | 1.12.0 | 支持 | 不用改 |
| 语言 API | `Locale.forLanguageTag`（21+）、`createConfigurationContext`（17+） | 都低于 minSdk | 不用改 |

**学校机器上真正需要确认的只有三件事**（都不是代码问题）：

1. **SDK Platform 36 已安装**。没装时 Sync 会报 `Failed to find target with hash string 'android-36'`，
   按提示点 Install，或用 SDK Manager 装。
2. **首次 Sync 能联网**，或者 Gradle 缓存里已经有这些依赖（你交作业时用过同一套版本，
   缓存大概率是热的）。机房断网时可以在 Gradle 面板点 **Toggle Offline Mode**
   再 Sync，前提是之前成功下载过。
3. **AI 功能**（如果你保留了）：学校机器上没有 `local.properties` 的 key，
   AI 页会显示“没有配置 API key”，不会崩。想现场演示 AI，需要临时加 key 且
   机房能访问 `api.deepseek.com`；否则就把 AI 那块删掉（`docs/exam-guide.md` 第 2 节），
   免得老师在断网环境里看到报错。

> 一句话：**代码不用为学校版本做任何修改**；要改的只有“这台机器装没装 SDK 36”
> 和“有没有网/有没有 key”。

### 9.2 你自己电脑上的差异：这些是操作层面的坑

你本机 Studio 版本不是 2025.2.1，所以下面是“本机练习”和“学校考试”不一致时
最容易出问题的地方。**原则：本机为了让工程能跑，做什么临时改动都可以，但绝不提交、
绝不带到学校。**

| 现象 | 原因 | 处理（不要改仓库里的版本号） |
| --- | --- | --- |
| Sync 报 `Dependency requires at least JVM runtime version 11. This build uses a Java 8 JVM` | IDE 的 Gradle JDK 指向了 JRE 8/11 | Settings → Build, Execution, Deployment → Build Tools → **Gradle → Gradle JDK** 选 17 或 21（本机实测：JBR 21 可用；只有 `jre` 没有 `jlink` 的 JDK 会报 `jlink.exe does not exist`，换完整 JDK） |
| Sync 报找不到 `android-36` | 本机 SDK 只装到 34/35 | SDK Manager 装 Platform 36 + Build-Tools |
| 打开工程时提示 “Android Gradle Plugin requires newer Studio” | 本机 Studio 比 2025.2.1 旧 | 只能用命令行 `gradlew.bat assembleDebug` 构建；**不要**为了能在 IDE 里打开而降 AGP 版本，否则改完忘了还原，考试就炸 |
| 打开工程时提示 “Upgrade AGP / Migrate to …” | 本机 Studio 比 2025.2.1 新 | 点 **Don't remind me / Cancel**；如果已经点了，用 `git checkout -- build.gradle app/build.gradle gradle/` 还原 |
| 向导生成 `build.gradle.kts` + `libs.versions.toml` | 新版模板默认 Kotlin DSL | 见 0.2/0.4；**不要把 `.kts` 片段和本仓库的 Groovy 混着粘** |
| 粘贴后一片红：`Unresolved reference: R` | 包名 / `namespace` 与新工程不一致 | 改 `namespace` 和每个文件的 `package`，或一开始就用 `com.example.bmi` |
| 中文注释变乱码 | 文件编码不是 UTF-8 | Settings → Editor → **File Encodings** 全部设 UTF-8；`gradle.properties` 已有 `-Dfile.encoding=UTF-8`，别删 |
| 用记事本改过文件后 `git apply` 失败/差异巨大 | 记事本把 LF 改成 CRLF 或加了 BOM | 用 Android Studio / VS Code 编辑；不要用记事本“另存为” |
| `local.properties` 出现在 `git status` 里 | 被手动 `git add -f` 过 | `git rm --cached local.properties`；`.gitignore` 里已有这一行 |
| 依赖下载卡住 | 本机访问 google()/mavenCentral() 慢 | `settings.gradle` 里已经准备好阿里云镜像，取消注释即可（本机改动，不要提交） |
| 模拟器跑不起来 | 镜像 API 太高或架构不匹配 | 用 API 33/34 的 x86_64 镜像；项目 minSdk 21，任何近期镜像都行 |

### 9.3 最容易“带错回学校”的 6 个改动

交卷/考试前，用 `git status` 和 `git diff` 自查这 6 项：

1. `build.gradle` / `app/build.gradle` 里的 AGP、Kotlin、compileSdk 被本机 Studio 自动改过；
2. `gradle/wrapper/gradle-wrapper.properties` 的 `distributionUrl` 被改过；
3. `.idea/`、`*.iml` 被提交（会在学校机器上覆盖 IDE 配置）；
4. `local.properties` 被提交（会覆盖学校的 SDK 路径，直接导致 Sync 失败）；
5. `settings.gradle` 里取消了阿里云镜像注释（学校机器能直连时可以留着，但要知道自己改过）；
6. 为了本机调试临时改了 `minSdk` / `targetSdk`，忘了改回来。

对应的一键还原（只还原构建配置，不动源码）：

```bat
git checkout -- build.gradle app/build.gradle gradle/wrapper/gradle-wrapper.properties settings.gradle
```

---

## 10. 考试时间分配建议（80 分钟版）

| 时间 | 做什么 | 验收 |
| --- | --- | --- |
| 0–10 min | 新建工程、对齐四个构建文件、Sync | 能 Run 模板页 |
| 10–20 min | 资源：strings（中英）、themes、颜色 | 无红色报错 |
| 20–30 min | 数据层：Record、Dates、DbHelper | 编译通过 |
| 30–45 min | 主界面：布局 + 保存逻辑 | 能保存 + Toast |
| 45–55 min | 历史页：布局 + Adapter | 能看到记录 |
| 55–65 min | 按题目加/删可选功能（语言、AI） | 对应功能可用 |
| 65–75 min | 自测清单走一遍 | 全绿 |
| 75–80 min | 清理临时文件、确认没有提交 `.idea`/`local.properties` | `git status` 干净 |

如果时间不够，**优先保证“能保存 + 能看历史”**，语言和 AI 都是加分项。

---

## 11. 一页速查

**建工程的顺序**：构建文件 → 资源 → 数据类 → 数据库 → 主界面 → 历史页 → 可选功能。

**每个文件属于哪一层**：

```
构建配置  build.gradle / app/build.gradle / settings.gradle / wrapper
资源      strings.xml（×2）、themes.xml（×2）、colors.xml
数据      WellnessRecord（字段）、Dates（日期）、WellnessDbHelper（SQLite）
界面      MainActivity + activity_main、HistoryActivity + activity_history + item_wellness
可选 A    BaseActivity、AppLanguage、LanguageStore、menu_main
可选 B    PromptBuilder、DeepSeekClient、WellnessAdviceRepository、
          ui/advice/*、AdviceActivity + activity_advice、PromptBuilderTest
```

**改题目的顺序**：字段（C1）→ 数据库（C2）→ 文案（C3）→ 布局（C4）→ 输入与保存（C5/C6）
→ 历史页（C7/C8）→ AI 提示词（C9/C10）。详见 `docs/adapt-to-other-topic.md`。

**出问题的第一反应**：先看 Build 面板的第一条 error，再全局搜索它提到的类名/资源名，
确认“定义还在不在、引用有没有漏改”，最后才怀疑版本。
