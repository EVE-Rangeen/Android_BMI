# Assignment 04 — 接入 LLM（DeepSeek）的健康建议

报告页除了本地计算的 BMI 结果之外，还会把 **BMI、性别、年龄** 发给 DeepSeek，
把模型返回的健康建议显示在报告页下方。建议的语言跟随应用当前语言：
中文界面拿到中文建议，英文界面拿到英文建议。

## 架构：MVVM

```
app/src/main/java/com/example/bmi/
├── ReportActivity.kt              ← View：只负责渲染 + 观察状态
├── data/
│   ├── AppLanguage.kt             ← 语言枚举（中/英）
│   ├── BmiProfile.kt              ← Model：BMI / 性别 / 年龄
│   ├── PromptBuilder.kt           ← 构造 system / user prompt（纯 Kotlin，可单测）
│   ├── DeepSeekClient.kt          ← HTTP 客户端（HttpURLConnection + org.json）
│   └── HealthAdviceRepository.kt  ← Repository：切换线程 + 统一异常
└── ui/report/
    ├── AdviceUiState.kt           ← Idle / Loading / Success / Error，以及 AdviceRequest
    └── ReportViewModel.kt         ← ViewModel：viewModelScope 发请求、重试、状态
```

要点：

- Activity 不碰网络，只 `observe` `adviceState`；点击 Retry 调 `viewModel.retry()`。
- ViewModel 在旋转屏幕时不会重建，所以转屏不会重复请求（也就不会重复扣费）。
- 所有失败都变成 `AdviceUiState.Error(ErrorReason)`，由 Activity 映射成本地化文案。

## API Key 的配置方式

代码里没有硬编码 key。key 在编译时从 `local.properties` 读到 `BuildConfig`：

```properties
# local.properties（已被 .gitignore 忽略，不会提交）
deepseek.apiKey=sk-xxxxxxxxxxxxxxxxxxxx
```

改完 key 需要重新编译（Sync 之后再 Run），因为它是 `buildConfigField`。
如果没有配置 key，App 依然能打开和计算 BMI，只是 AI 建议区域会提示
“未配置 API Key”。

## 请求细节

- 端点：`https://api.deepseek.com/chat/completions`
- 模型：`deepseek-chat`
- 请求体：`messages` 里一条 system（角色 + 语言 + 输出格式要求），一条 user
  （BMI / 性别 / 年龄）
- `temperature = 0.7`，`max_tokens = 320`
- 超时：连接 15s，读取 90s
- 失败重试：UI 上的 Retry 按钮重新发起同一次请求

## 截图

`screenshots/` 下有两张报告页截图（同一组输入：身高 175cm、体重 70kg、
年龄 20、男性，BMI 22.86）：

- `report_zh.png`：中文界面 → 中文建议
- `report_en.png`：英文界面 → 英文建议

在模拟器上切换语言可以用：

```bat
adb shell cmd locale set-app-locales com.example.bmi --locales zh-CN
adb shell cmd locale set-app-locales com.example.bmi --locales en-US
```

## 单元测试

`app/src/test/java/com/example/bmi/PromptBuilderTest.kt` 验证：

- 中文请求里包含“只用简体中文回答”，并且带上了 BMI / 性别 / 年龄；
- 英文请求里包含 “Answer in English only”，并且带上了 BMI / gender / age。

运行：`gradlew.bat testDebugUnitTest`
