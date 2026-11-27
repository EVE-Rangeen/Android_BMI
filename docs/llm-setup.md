# AI 健康建议（DeepSeek）与中英文切换

这一版把两部分“上课内容”加在了健康日志之上：

1. **AI 健康建议**：把今天的记录（状况 / 步数 / 备注）发给 DeepSeek，
   把返回的建议显示在单独的一页；
2. **App 内中英文切换**：右上角菜单点一下，整个 App 在中文和英文之间切换，
   AI 的回答语言也跟着变。

> 这两块都是**可选功能**。考试不需要时怎么删，见
> [`docs/exam-guide.md`](exam-guide.md)。本文件只讲它们是怎么实现的。

---

## 1. 结构

```
app/src/main/java/com/example/bmi/
├── BaseActivity.kt                 ← 语言切换：attachBaseContext 换 Locale
├── MainActivity.kt                 ← 主界面 + 右上角菜单（切换语言 / AI 建议）
├── HistoryActivity.kt              ← 历史列表
├── AdviceActivity.kt               ← AI 页：读今天记录 → 请求 → 显示（V）
├── data/
│   ├── AppLanguage.kt              ← 语言枚举（en / zh）
│   ├── LanguageStore.kt            ← SharedPreferences 记住用户选的语言
│   ├── PromptBuilder.kt            ← 拼 system / user 提示词（纯 Kotlin，可单测）
│   ├── DeepSeekClient.kt           ← HTTP 客户端（HttpURLConnection + org.json）
│   └── WellnessAdviceRepository.kt ← Repository：切 IO 线程 + 统一异常
└── ui/advice/
    ├── AdviceUiState.kt            ← Idle / Loading / Success / Error + 错误原因
    └── AdviceViewModel.kt          ← ViewModel：发请求、重试、暴露 LiveData（VM）
```

MVVM 的分工：

- **V（AdviceActivity）** 只负责渲染和触发，不碰网络；
- **VM（AdviceViewModel）** 拿着 Repository 发请求，把结果/异常转成状态；
- **M（WellnessAdviceRepository + DeepSeekClient）** 真正从网络取数据。

Activity 通过 `observe(adviceState)` 拿状态，转屏时 ViewModel 不销毁，
所以不会重复发请求（也就不会重复扣费）。

---

## 2. 语言是怎么切换的

两套资源本来就是现成的：

```
res/values/strings.xml          → 英文（默认）
res/values-zh-rCN/strings.xml   → 中文
```

默认行为是“跟着手机系统语言走”。要让 App 自己决定，用
`BaseActivity.attachBaseContext()`：

```kotlin
override fun attachBaseContext(newBase: Context) {
    val saved = LanguageStore(newBase).load()
    if (saved == null) {                 // 用户没手动选过 → 跟随系统
        super.attachBaseContext(newBase)
        return
    }
    val locale = Locale.forLanguageTag(saved.tag)   // "en" / "zh"
    Locale.setDefault(locale)
    val config = Configuration(newBase.resources.configuration)
    config.setLocale(locale)
    super.attachBaseContext(newBase.createConfigurationContext(config))
}
```

菜单点击后调用 `switchLanguage()`：先写 SharedPreferences，再 `recreate()`。
重建时 `attachBaseContext` 读到新语言，`@string/xxx` 与 `getString()` 就都换了语言。

两个容易踩的点：

- **Activity 标题**由系统按 Manifest 的 `android:label` 解析，不会跟着
  `attachBaseContext` 变，所以三个 Activity 都在 `onCreate` 里写了
  `title = getString(...)`；
- 已经显示的界面不会自动重画，必须 `recreate()`。

AI 的回答语言与界面语言一致：`AdviceActivity` 用
`currentLanguage()`（读当前 Configuration）把 `AppLanguage` 传给 `PromptBuilder`。

---

## 3. API Key 的配置方式

代码里没有硬编码 key。key 在编译时从 `local.properties` 读到 `BuildConfig`：

```properties
# local.properties（已被 .gitignore 忽略，不会提交）
deepseek.apiKey=sk-xxxxxxxxxxxxxxxxxxxx
```

对应 `app/build.gradle`：

```gradle
def localProperties = new Properties()
def localPropertiesFile = rootProject.file('local.properties')
if (localPropertiesFile.exists()) {
    localPropertiesFile.withInputStream { stream -> localProperties.load(stream) }
}
def deepSeekApiKey = localProperties.getProperty('deepseek.apiKey') ?: ''

android {
    defaultConfig {
        buildConfigField "String", "DEEPSEEK_API_KEY", "\"${deepSeekApiKey}\""
        buildConfigField "String", "DEEPSEEK_BASE_URL", "\"https://api.deepseek.com\""
    }
    buildFeatures { buildConfig = true }
}
```

改完 key 要重新编译（Sync 之后再 Run），因为它是编译期常量。
没有配置 key 时 App 仍能正常记录和历史，只是 AI 页会提示“没有配置 API key”。

---

## 4. 请求细节

- 端点：`https://api.deepseek.com/chat/completions`
- 模型：`deepseek-chat`
- 请求体：`messages` 里一条 system（身份 + 语言 + 输出格式要求），一条 user
  （日期 / 身体状况 / 步数 / 备注）
- `temperature = 0.7`，`max_tokens = 320`，`stream = false`
- 超时：连接 15 秒，读取 90 秒
- 失败后页面显示原因和“重试”按钮，重试会原样再发一次

网络请求全部在 `Dispatchers.IO` 上执行（`WellnessAdviceRepository`），
主线程只负责渲染，所以不会卡界面、也不会触发
`NetworkOnMainThreadException`。

---

## 5. 单元测试

`app/src/test/java/com/example/bmi/PromptBuilderTest.kt` 验证：

- 中文 system prompt 里要求“只用简体中文回答”，且不出现英文规则；
- 英文 system prompt 里包含 “Answer in English only”；
- user prompt 里带上了日期、身体状况、步数、备注；
- 备注为空时写成“无”/“none”；
- 同一个状况值（1）在中文提示词里是“一般”，在英文提示词里是 “Fair”。

`PromptBuilder` 不 import 任何 Android 类，所以能在电脑 JVM 上直接跑：

```bat
gradlew.bat testDebugUnitTest
```

---

## 6. 在模拟器上手动切换语言

App 内切换：主界面右上角三个点 → “Switch language / 切换语言”。

也可以用 adb 强制切换（验证资源是否齐全）：

```bat
adb shell am force-stop com.example.bmi
adb shell cmd locale set-app-locales com.example.bmi --locales zh-CN
adb shell cmd locale set-app-locales com.example.bmi --locales en-US
```

注意：后者改的是系统记录的“按 App 语言”，和 App 自己写在
SharedPreferences 里的选择是两套机制；本项目用的是后者（自己的偏好），
所以 adb 那条命令主要用于验证资源，不会覆盖 App 内的选择。
