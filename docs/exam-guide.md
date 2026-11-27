# 考试删减指南：中英文切换 与 AI 健康建议

这份文档回答一个问题：**考试题目不需要某个功能时，应该删掉哪些文件、哪些代码。**

当前分支的功能分三层：

| 层 | 功能 | 能不能删 |
| --- | --- | --- |
| 基础层 | 记录今天（状况 / 步数 / 备注）→ 存 SQLite；历史页查看 | 必须保留 |
| 可选层 A | App 内中英文切换（菜单里点一下，全 App 换语言） | 可整块删除 |
| 可选层 B | AI 健康建议（把今天的记录发给 DeepSeek，显示返回的建议） | 可整块删除 |

两层可选功能在代码里都用标记注释标出来了。在 Android Studio 里按
`Ctrl + Shift + F` 全局搜索下面三个关键词，就能找到所有需要处理的位置：

```
【语言切换 · 考试可删】
【AI 功能 · 考试可删】
考试可删
```

> 先做一件保命的事：**新建分支再删**。`git checkout -b exam-minimal`，
> 删坏了随时 `git checkout -- .` 回到现在这一版。

---

## 0. 先确认你属于哪种情况

| 老师的题目要求 | 按哪一节删 | 删完大概剩几个源文件 |
| --- | --- | --- |
| 只要记录 + 历史（最基础） | 第 1 节 + 第 2 节 | 6 个 Kotlin 文件 + 3 个布局 |
| 记录 + 历史，**不需要** App 内切换语言 | 第 1 节 | 9 个 Kotlin 文件 + 4 个布局 |
| 记录 + 历史 + AI，**不需要**切换语言 | 第 1 节里“保留 AI”的写法 | 10 个 Kotlin 文件 + 4 个布局 |
| 都要（当前版本） | 不删 | 12 个 Kotlin 文件 + 4 个布局 |

“不需要切换语言”的意思是：老师只要求界面有中文和英文两套资源、
跟着手机系统语言自动变，而不要求 App 里自己点按钮切换。
这两种在 Android 里是两件不同的事，第 3 节专门解释。

---

## 1. 版本 A：不要“App 内中英文切换”

### 1.1 语言切换到底靠哪几个文件

| 文件 | 作用 | 删掉后会怎样 |
| --- | --- | --- |
| `app/src/main/java/com/example/bmi/BaseActivity.kt` | 在 `attachBaseContext` 里把 Context 的语言改成用户选的语言 | 三个 Activity 都要改回继承 `AppCompatActivity` |
| `app/src/main/java/com/example/bmi/data/LanguageStore.kt` | 用 SharedPreferences 记住用户选了哪种语言 | 没人再读写这个偏好 |
| `app/src/main/java/com/example/bmi/data/AppLanguage.kt` | `ENGLISH` / `CHINESE` 枚举 | **AI 也在用它**，见 1.3 |
| `app/src/main/res/menu/menu_main.xml` 里的 `menu_language` 项 | 菜单入口 | 菜单里少一项 |
| `strings.xml` 里的 `menu_language` | 菜单文字 | 没人引用，可留可删 |
| `MainActivity` 里的 `menu_language` 分支 | 点击后 `toggleLanguage()` | 菜单项点了没反应 |
| 三个 Activity 里的 `title = getString(...)` | 让标题栏也跟着 App 内语言变 | 标题会跟随手机系统语言（默认就是这样，不算错） |

`values-zh-rCN/strings.xml` **不要删**：它提供中文文案，属于 Android 标准多语言机制，
不是“切换功能”的代码。删了中文手机也会显示英文。

### 1.2 删除步骤（顺序照做，每步之后都能编译）

**第 1 步：三个 Activity 改回普通 AppCompatActivity**

`MainActivity.kt`

```kotlin
// 原来：
// class MainActivity : BaseActivity() {
// 改成：
class MainActivity : AppCompatActivity() {
```

同时在文件顶部加一行 import：

```kotlin
import androidx.appcompat.app.AppCompatActivity
```

`HistoryActivity.kt` 同样处理。

`AdviceActivity.kt`（如果保留 AI）也改成 `AppCompatActivity`，并按 1.3 补一个
`currentLanguage()`。

**第 2 步：删菜单里的语言项**

`MainActivity.onOptionsItemSelected()` 里删掉这一支：

```kotlin
// ── 【语言切换 · 考试可删】中文 ↔ 英文，切换后整页重建 ──
R.id.menu_language -> {
    // toggleLanguage() 在 BaseActivity 里：保存选择 + recreate()
    toggleLanguage()
    true
}
```

`res/menu/menu_main.xml` 里删掉：

```xml
<item
    android:id="@+id/menu_language"
    android:title="@string/menu_language"
    app:showAsAction="never" />
```

（如果 AI 也不需要了，整个 `menu_main.xml` 都可以删，见第 2 节。）

**第 3 步：删文件**

```
app/src/main/java/com/example/bmi/BaseActivity.kt
app/src/main/java/com/example/bmi/data/LanguageStore.kt
```

`AppLanguage.kt` 先别删——AI 的 `PromptBuilder` 需要它。如果 AI 也删，再一起删。

**第 4 步：删掉三个 Activity 里的标题设置（可选，但建议删干净）**

```kotlin
// 【语言切换 · 考试可删】标题跟着 App 内语言走（原因见 MainActivity 的说明）。
title = getString(R.string.app_name)
```

HistoryActivity 里是 `title = getString(R.string.wellness_history_title)`，
AdviceActivity 里是 `title = getString(R.string.ai_title)`。

删掉后标题栏会显示 Manifest 里 `android:label` 指向的字符串，由系统按手机语言解析，
这是 Android 的默认行为，考试完全够用。

**第 5 步：编译**

```bat
gradlew.bat assembleDebug
```

### 1.3 如果保留 AI：`currentLanguage()` 怎么办

`AdviceActivity` 原来调用 `BaseActivity.currentLanguage()` 决定“让 AI 用中文还是英文回答”。
`BaseActivity` 删掉后，把这个小函数直接放进 `AdviceActivity`：

```kotlin
import androidx.core.os.ConfigurationCompat
import com.example.bmi.data.AppLanguage

/**
 * 读手机系统当前语言（App 内不再切换语言，所以直接问系统）。
 */
private fun currentLanguage(): AppLanguage = AppLanguage.fromTag(
    ConfigurationCompat.getLocales(resources.configuration).get(0)?.language
)
```

这样 AI 的回答语言仍然会跟着手机语言走：中文手机拿到中文建议，英文手机拿到英文建议。

如果你连“跟着系统语言”都不需要，直接写死：

```kotlin
private fun currentLanguage(): AppLanguage = AppLanguage.CHINESE
```

### 1.4 删完的检查清单

- [ ] 全局搜索 `BaseActivity`：没有任何引用（除了可能残留的注释）。
- [ ] 全局搜索 `LanguageStore`、`toggleLanguage`、`switchLanguage`：没有引用。
- [ ] 全局搜索 `menu_language`：`menu_main.xml` 和 `onOptionsItemSelected` 里都没有了。
- [ ] `gradlew.bat assembleDebug` 编译通过。
- [ ] 打开 App：菜单里只剩“AI 健康建议”（或没有菜单）。
- [ ] 把手机语言切成中文再切回英文：界面跟着变（这是资源机制，与切换功能无关）。

---

## 2. 版本 B：不要 AI 健康建议

### 2.1 AI 功能涉及哪些文件

| 文件 | 作用 |
| --- | --- |
| `app/src/main/java/com/example/bmi/AdviceActivity.kt` | AI 页面（读取今天记录 → 请求 → 显示） |
| `app/src/main/res/layout/activity_advice.xml` | AI 页面布局 |
| `app/src/main/java/com/example/bmi/data/DeepSeekClient.kt` | HTTP 客户端（`HttpURLConnection` + `org.json`） |
| `app/src/main/java/com/example/bmi/data/PromptBuilder.kt` | 拼提示词（纯 Kotlin，可单测） |
| `app/src/main/java/com/example/bmi/data/WellnessAdviceRepository.kt` | 切到 IO 线程发请求、统一异常 |
| `app/src/main/java/com/example/bmi/ui/advice/AdviceUiState.kt` | 状态机 + 错误原因枚举 |
| `app/src/main/java/com/example/bmi/ui/advice/AdviceViewModel.kt` | ViewModel：发请求、重试、暴露 LiveData |
| `app/src/test/java/com/example/bmi/PromptBuilderTest.kt` | 提示词单元测试 |

另外有 6 处“外围配置”要改：

1. `AndroidManifest.xml`：`INTERNET` 权限 + `.AdviceActivity` 注册；
2. `app/build.gradle`：密钥读取、`buildConfigField`、`buildFeatures`、4 个依赖；
3. `MainActivity`：菜单项 `menu_ai_advice` 的分支；
4. `res/menu/menu_main.xml`：`menu_ai_advice` 项；
5. `WellnessDbHelper.findByDate()`：只有 AI 页在用；
6. `strings.xml`：`ai_*` 和 `menu_ai_advice` 文案（可留可删，留着不影响编译）。

### 2.2 删除步骤

**第 1 步：删文件（8 个）**

```
app/src/main/java/com/example/bmi/AdviceActivity.kt
app/src/main/java/com/example/bmi/data/DeepSeekClient.kt
app/src/main/java/com/example/bmi/data/PromptBuilder.kt
app/src/main/java/com/example/bmi/data/WellnessAdviceRepository.kt
app/src/main/java/com/example/bmi/ui/advice/AdviceUiState.kt
app/src/main/java/com/example/bmi/ui/advice/AdviceViewModel.kt
app/src/main/res/layout/activity_advice.xml
app/src/test/java/com/example/bmi/PromptBuilderTest.kt
```

删完后 `ui/advice/` 目录是空的，可以整个删掉。

**第 2 步：清 Manifest**

删掉权限：

```xml
<!-- 【AI 功能 · 考试可删】访问 DeepSeek 接口需要联网权限，不写会直接崩溃。 -->
<uses-permission android:name="android.permission.INTERNET" />
```

删掉页面注册：

```xml
<!-- 【AI 功能 · 考试可删】AI 健康建议页。 -->
<activity
    android:name=".AdviceActivity"
    android:exported="false"
    android:label="@string/ai_title" />
```

**第 3 步：清 build.gradle**

删掉这一段（文件里在 `plugins` 和 `android` 之间）：

```gradle
def localProperties = new Properties()
def localPropertiesFile = rootProject.file('local.properties')
if (localPropertiesFile.exists()) {
    localPropertiesFile.withInputStream { stream -> localProperties.load(stream) }
}
def deepSeekApiKey = localProperties.getProperty('deepseek.apiKey') ?: ''
```

删掉 `defaultConfig` 里的两行：

```gradle
buildConfigField "String", "DEEPSEEK_API_KEY", "\"${deepSeekApiKey}\""
buildConfigField "String", "DEEPSEEK_BASE_URL", "\"https://api.deepseek.com\""
```

删掉整个 `buildFeatures` 块：

```gradle
buildFeatures {
    buildConfig = true
}
```

删掉 4 个依赖（`dependencies` 里带注释的那一组）：

```gradle
implementation 'androidx.activity:activity-ktx:1.5.1'
implementation 'androidx.lifecycle:lifecycle-viewmodel-ktx:2.5.1'
implementation 'androidx.lifecycle:lifecycle-livedata-ktx:2.5.1'
implementation 'org.jetbrains.kotlinx:kotlinx-coroutines-android:1.6.4'
```

> 注意顺序：先删代码文件，再删依赖。反过来会在编译时找不到 `ViewModel`、
> `viewModelScope` 等类。

**第 4 步：清菜单**

`MainActivity.onOptionsItemSelected()` 里删掉：

```kotlin
// ── 【AI 功能 · 考试可删】打开 AI 健康建议页 ──
R.id.menu_ai_advice -> {
    // 不传数据：建议页自己按 Dates.today() 去数据库查今天的记录。
    startActivity(Intent(this, AdviceActivity::class.java))
    true
}
```

`res/menu/menu_main.xml` 里删掉 `menu_ai_advice` 那一项。
如果语言项也已经删了，这个文件就空了：删掉 `menu_main.xml`，同时删掉
`MainActivity` 里的 `onCreateOptionsMenu()` 和 `onOptionsItemSelected()`
两个方法，以及 `import android.view.Menu` / `import android.view.MenuItem`。

**第 5 步：清数据库方法**

`WellnessDbHelper` 里的 `findByDate()` 只有 `AdviceActivity` 在用，可以删掉。
（留着不影响编译，只是没人调用。）

**第 6 步：清字符串（可选）**

两个 `strings.xml` 里可以删掉 `menu_ai_advice` 和所有 `ai_*` 开头的条目。
留着不会报错，Lint 也不会因此报错，删不删看你想多干净。

**第 7 步：编译 + 跑测试**

```bat
gradlew.bat assembleDebug
gradlew.bat testDebugUnitTest
```

### 2.3 删完的检查清单

- [ ] 全局搜索 `AdviceActivity`：只在注释里出现。
- [ ] 全局搜索 `DeepSeek`、`PromptBuilder`、`WellnessAdviceRepository`、`AdviceUiState`：没有引用。
- [ ] 全局搜索 `INTERNET`：Manifest 里没有。
- [ ] 全局搜索 `BuildConfig`：没有任何 Kotlin 文件在用。
- [ ] 打开 App：保存、历史都正常；没有“AI 健康建议”菜单项。
- [ ] `gradlew.bat lintDebug` 通过（如果 Manifest 还注册了不存在的 Activity，
      运行时会崩溃，Lint 也可能给出警告）。

---

## 3. 版本 C：两个功能都不要（最基础版）

按第 1 节做完，再按第 2 节做完，最后处理 `AppLanguage.kt`。

因为 `AppLanguage` 只服务于“界面语言选择”和“AI 回答语言”，两个功能都没了，
它就没有使用者了，可以直接删：

```
app/src/main/java/com/example/bmi/data/AppLanguage.kt
```

最终结构（这就是“只做题目要求”的最小版本）：

```
app/src/main/
├── AndroidManifest.xml
├── java/com/example/bmi/
│   ├── MainActivity.kt          # 输入 + 校验 + 保存 + 跳历史
│   ├── HistoryActivity.kt       # 查库 + 列表 + 统计
│   └── data/
│       ├── Dates.kt             # 今天的 yyyy-MM-dd
│       ├── WellnessRecord.kt    # 数据类
│       └── WellnessDbHelper.kt  # SQLite 建表/保存/查询
└── res/
    ├── layout/activity_main.xml
    ├── layout/activity_history.xml
    ├── layout/item_wellness.xml
    ├── values/strings.xml
    ├── values/strings.xml 同级的 values-zh-rCN/strings.xml
    └── 图标、主题等
```

`res/menu/menu_main.xml` 删掉（没有菜单了），`app/build.gradle` 回到只有
core-ktx / appcompat / material / 测试依赖的状态。

**重点：`values-zh-rCN/` 不要删。** 它是“界面有中文文案”的实现方式，
手机语言是中文时系统自动用这套资源，一行代码都不用写。老师如果说“要中英文”，
通常指的就是这个，而不是 App 内切换按钮。

---

## 4. 反向操作：点名要求加回来

如果考试题目正好要求“支持中英文切换”或“接入 AI”，不要从零写，按现在的
代码顺序抄，每一步都能单独验证：

**加语言切换（约 60 行）**

1. `data/AppLanguage.kt`：两个枚举值 + `fromTag()`；
2. `data/LanguageStore.kt`：SharedPreferences 读写；
3. `BaseActivity.kt`：`attachBaseContext` 里换 Locale，`recreate()` 生效；
4. 三个 Activity 改成继承 `BaseActivity`；
5. 菜单项 + `toggleLanguage()`。

**加 AI（约 300 行）**

1. `PromptBuilder`（纯 Kotlin，先写它，能单测）；
2. `DeepSeekClient`（HTTP + JSON）；
3. `WellnessAdviceRepository`（`withContext(Dispatchers.IO)`）；
4. `AdviceUiState` + `AdviceViewModel`（状态机 + `viewModelScope`）；
5. `AdviceActivity` + 布局；
6. Manifest 加 `INTERNET` 和页面注册；
7. `build.gradle` 注入 key、开 `buildConfig`、加协程依赖。

---

## 5. 删除时最容易犯的错

| 症状 | 原因 | 处理 |
| --- | --- | --- |
| `Unresolved reference: BaseActivity` | 删了类，Activity 还继承它 | 改回 `AppCompatActivity` |
| `Unresolved reference: AppLanguage` | 删了枚举，`PromptBuilder` / `AdviceActivity` 还在用 | 要么保留枚举，要么连 AI 一起删 |
| `Unresolved reference: viewModelScope` / `ViewModel` | 删依赖但没删 AI 代码 | 先删代码再删依赖（或反过来补回依赖） |
| 点“AI 健康建议”直接闪退 | Manifest 删了注册 / 删了 `INTERNET` 权限 | `SecurityException` 或 `ActivityNotFoundException`，检查这两处 |
| 中文手机显示英文 | 删了 `values-zh-rCN/strings.xml` | 恢复这个目录，key 要和 `values/strings.xml` 一致 |
| 标题栏语言跟界面不一致 | 删了 `title = getString(...)` 但保留 App 内切换 | 要么恢复这行，要么整套切换都删掉 |
| 切语言后界面没变 | 忘了 `recreate()` | 见 `BaseActivity.switchLanguage()` |
| 编译报 `R.menu.menu_main` 找不到 | 删了 `menu_main.xml` 但 `onCreateOptionsMenu` 还在 | 两个一起删，或两个一起留 |
| 删完 App 白屏/闪退 | 删了 `activity_main.xml` 里的控件但代码还在 `findViewById` | 布局和 `findViewById` 必须成对 |

---

## 6. 删完的统一自检

```bat
gradlew.bat clean assembleDebug
gradlew.bat lintDebug
gradlew.bat testDebugUnitTest
```

三条都通过，再上模拟器走一遍：

1. 主界面 → 填步数 → 保存 → Toast 提示；
2. 打开历史 → 看到刚才那条记录；
3. 同一天再保存一次 → 历史里仍然一条；
4. 手机语言切中文 → 界面变中文；
5. 返回主界面 → 输入内容还在。

如果删坏了想回退：

```bat
git checkout -- .          # 丢弃所有未提交改动
git clean -fd app/src      # 清理新建的未跟踪文件
```

---

## 7. 相关文档

- `README.md` — 项目简介、运行方式、工具链。
- `docs/wellness-tracker.md` — 每个文件的作用、SQLite 设计、patch 用法。
- `docs/llm-setup.md` — AI 部分的架构与 API key 配置（删 AI 时一起删）。
- `docs/exam-playbook.md` — 把这道题迁移到其它“每日记录类”题目的通用七步。
