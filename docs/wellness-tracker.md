# Wellness Tracker 改造说明（考试练习版）

本分支 `wellness_tracker` 从 `cityu_lab` 的最新提交 `d43a3c9` 创建，把原来的
BMI 作业改成了学长回忆中的考试题：**Wellness Tracker（健康日志）**。

目标只有一个：用最小的改动，做出一个能完成题目全部基本要求、且能直接运行和演示的
MVP（Minimum Viable Product，最小可用版本）。

---

## 1. 题目要求与实现对照

| 题目要求 | 本项目的实现 |
| --- | --- |
| 记录每天的身体状况（良好 / 一般 / 较差） | 主界面的 `Spinner`，选中序号 0/1/2 存库 |
| 记录今天走了多少步 | `EditText` 输入整数，校验后存库 |
| 记录一些信息（备注） | 多行 `EditText`，可留空 |
| 保存 | “Save today record” 按钮写入 SQLite |
| 每天记录一下 | 日期自动取系统当天，同一天再保存会覆盖旧记录 |
| 查看历史记录 | “View history” 打开 `HistoryActivity`，按日期倒序列出全部记录 |
| （可选 A）中英文切换 | 菜单 “Switch language” → 保存选择 + `recreate()`，见 `docs/llm-setup.md` |
| （可选 B）AI 健康建议 | 菜单 “AI advice” → 取今天记录发 DeepSeek，显示 3 条建议 |
| （可选 C）最近 7 天趋势折线图 | 菜单 “Trend chart” → `TrendActivity` 用 MPAndroidChart 的 `LineChart` 画最近 7 条记录的步数折线 |

没有做的内容（刻意不做，属于冗余）：BMI 计算、登录、云同步、编辑/删除单条记录。
旧 BMI 代码已经全部删除（见 4.3 节）；图表用的是新写的 `TrendActivity`，
和旧代码无关。

三块可选功能（语言切换、AI、趋势图）都做成了“可以整块删除”的形式，代码里用
`【语言切换 · 考试可删】` / `【AI 功能 · 考试可删】` / `【图表 · 考试可删】` 标记，
删除清单见 `docs/exam-guide.md`。基础功能不依赖它们，删掉照样能编译运行。

---

## 2. 运行与演示

1. 用 Android Studio 打开项目根目录（包含 `settings.gradle` 的那一层）。
2. 等 Gradle Sync 完成，选中 `app` 运行配置，点 Run。
3. 演示路径：
   - 主界面显示 `Today: 2026-11-26`；
   - 在 “How do you feel today?” 选择 `Good / Fair / Poor`；
   - 输入步数，例如 `8000`；
   - 备注可以留空，也可以写 `Slept well`；
   - 点 **Save today record**，底部出现 `Saved for today.`；
   - 点 **View history**，能看到 `日期 · 状况` 和 `步数 · 备注`；
   - 点右上角菜单 **Trend chart**，能看到最近 7 天的步数折线图；
   - 再保存一次同一天的数据，历史里仍然只有一条（内容被更新）。

数据库文件在设备上的位置：

```
/data/data/com.example.bmi/databases/wellness.db
```

用 Android Studio 的 **Device Explorer**（旧版叫 Device File Explorer）可以导出来，
用任意 SQLite 工具打开，就能看到 `wellness_records` 表。

> 说明：包名仍然是 `com.example.bmi`，没有改成 `com.example.wellness`。
> 这是为了把改动压到最小：改包名要动 `namespace`、`applicationId`、所有
> `package` 声明和目录结构，考试时既费时又容易出错，而且题目并不检查包名。

---

## 3. 如何应用 patch 文件

patch 文件：`patch/wellness-tracker.patch`

它记录的是 **从 `d43a3c9`（你已提交的 BMI 作业）到本分支** 的全部改动。
考试时把 patch 文件和仓库一起带过去，执行：

```bat
cd 你的仓库目录
git status
git checkout -b exam d43a3c9
git apply --check patch/wellness-tracker.patch
git apply patch/wellness-tracker.patch
```

- `--check` 只检查能不能打上，不会改文件。看到没有输出就说明可以。
- 如果提示行尾/空白问题，用：

```bat
git apply --ignore-whitespace patch/wellness-tracker.patch
```

- 如果提示 “patch does not apply”，最稳的是用三方合并：

```bat
git apply --3way patch/wellness-tracker.patch
```

- 打完补丁后，在 Android Studio 里 **Sync Project with Gradle Files**，再 Run。

> 不要把 patch 文件用记事本打开后“另存为”，那会改变行尾，导致打不上。
> 如果考试电脑没有 git，直接按第 4 节的文件清单，从本分支复制文件覆盖过去也可以。

---

## 4. 改了哪些文件

### 4.1 新增文件

| 文件 | 作用 |
| --- | --- |
| `app/src/main/java/com/example/bmi/data/Dates.kt` | 通用日期工具：取今天的 `yyyy-MM-dd` |
| `app/src/main/java/com/example/bmi/data/WellnessRecord.kt` | 一条健康记录的数据类：日期、状况、步数、备注 |
| `app/src/main/java/com/example/bmi/data/WellnessDbHelper.kt` | SQLite 建表、保存、读取全部记录 |
| `app/src/main/java/com/example/bmi/HistoryActivity.kt` | 历史记录页：读库、统计、显示列表 |
| `app/src/main/res/layout/activity_history.xml` | 历史页布局（统计 + 空状态 + ListView） |
| `app/src/main/res/layout/item_wellness.xml` | 历史列表每一行的布局（两行文字） |
| `BaseActivity.kt`（可选 A） | 语言切换：`attachBaseContext` 里替换 Locale |
| `data/AppLanguage.kt`（可选 A/B） | `ENGLISH` / `CHINESE` 枚举 + `fromTag()` |
| `data/LanguageStore.kt`（可选 A） | SharedPreferences 记住用户选的语言 |
| `res/menu/menu_main.xml`（可选 A/B/C） | 右上角菜单：切换语言、AI 建议、趋势图 |
| `AdviceActivity.kt`（可选 B） | AI 页：取今天记录 → 请求 → 渲染状态 |
| `res/layout/activity_advice.xml`（可选 B） | AI 页布局（摘要 + 转圈 + 建议 + 重试） |
| `data/PromptBuilder.kt`（可选 B） | 拼提示词，纯 Kotlin，可单元测试 |
| `data/DeepSeekClient.kt`（可选 B） | HTTP 客户端（`HttpURLConnection` + `org.json`） |
| `data/WellnessAdviceRepository.kt`（可选 B） | 切 IO 线程发请求、统一异常 |
| `ui/advice/AdviceUiState.kt`（可选 B） | 四种状态 + 错误原因枚举 |
| `ui/advice/AdviceViewModel.kt`（可选 B） | ViewModel + LiveData + 重试 |
| `app/src/test/.../PromptBuilderTest.kt`（可选 B） | 提示词单元测试（7 个用例） |
| `TrendActivity.kt`（可选 C） | 趋势页：读最近 7 条记录 → 交给 `LineChart` 画折线 |
| `res/layout/activity_trend.xml`（可选 C） | 趋势页布局（统计文字 + `LineChart` + 空状态） |

### 4.2 修改文件

| 文件 | 改了什么 |
| --- | --- |
| `app/src/main/java/com/example/bmi/MainActivity.kt` | 整体换成“选状况 + 填步数/备注 + 保存 + 查看历史”；再叠加三项：继承 `BaseActivity`（语言）+ 选项菜单（语言 / AI / 趋势图） |
| `app/src/main/res/layout/activity_main.xml` | 整体换成纵向 `LinearLayout` + `ScrollView`，只保留本题需要的控件 |
| `app/src/main/AndroidManifest.xml` | 注册 `MainActivity`、`HistoryActivity`、`AdviceActivity`、`TrendActivity`；只保留 AI 需要的 `INTERNET` 权限；主题改为 `Theme.Wellness` |
| `app/src/main/res/values/strings.xml` | wellness 文案 + 状况数组 + 菜单/AI/趋势图文案，`app_name` 改为 Wellness Tracker |
| `app/src/main/res/values-zh-rCN/strings.xml` | 同上，中文文案 |
| `app/build.gradle` | 只保留健康日志依赖；再为 AI 加回密钥注入、`buildConfig` 开关、ViewModel/协程依赖；为趋势图加 MPAndroidChart |
| `settings.gradle` | 为 MPAndroidChart 增加 JitPack 仓库（`https://jitpack.io`） |
| `app/src/main/res/values/themes.xml` | `Theme.BMI` 重命名为 `Theme.Wellness` |
| `app/src/main/res/values-night/themes.xml` | 同上，深色主题同步改名 |
| `README.md` | 改成健康日志的项目说明，保留工具链/网络/构建部分 |

### 4.3 删除文件

| 类别 | 删除的文件 | 为什么删 |
| --- | --- | --- |
| 旧页面 | `ReportActivity.kt`、`TrendActivity.kt` | 本题没有报告页和趋势页 |
| 旧数据模型 | `BmiDbHelper.kt`、`BmiHistoryRepository.kt`、`BmiPreferences.kt`、`BmiProfile.kt`、`BmiRecord.kt`、`BmiDates.kt`、`Gender.kt` | 已由 `Wellness*` 和 `Dates` 取代 |
| MVVM/图表 | `ui/report/`、`ui/trend/` 整个目录 | 只服务于已删除的报告页和趋势页 |
| 旧布局 | `activity_report.xml`、`activity_trend.xml` 及 `layout-land/` 下对应文件 | 页面已删除 |
| 横屏旧主布局 | `layout-land/activity_main.xml` | Android 横屏时会优先加载它；不删会因找不到 `stepsET` 等控件而崩溃 |
| 菜单/图片 | `menu/menu_options.xml`、`menu/menu_context.xml`、`drawable/bot_*.png` | 旧主界面和报告页专用 |

> 说明：`activity_trend.xml` 后来以**新写的**趋势页布局重新加回（配合可选 C），
> 内容和旧 BMI 版本完全不同；`layout-land/activity_trend.xml` 没有恢复，
> 横屏时继续使用竖屏布局。旧的 `ui/trend/BmiTrendChartView.kt` 没有恢复，
> 新的图表直接用 MPAndroidChart 的 `LineChart`，见 7.4 节。

> 首轮清理时还删掉了 `DeepSeekClient.kt`、`HealthAdviceRepository.kt`、
> `PromptBuilder.kt`、`AppLanguage.kt`、`ui/report/` 和 `docs/llm-setup.md`。
> 后来为了留作考试参考，又按 4.1 节的清单重新加回了**新写的**语言切换和 AI 代码
> （文件名和职责与旧版不同：`WellnessAdviceRepository`、`ui/advice/`）。
> 旧的 BMI 相关代码没有恢复，仍然只存在于 `cityu_lab` 分支。

### 4.4 清理后的最终结构

```
app/src/main/java/com/example/bmi/
├── MainActivity.kt          # 输入、校验、保存、跳转、菜单
├── HistoryActivity.kt       # 历史列表
├── BaseActivity.kt          # 【可选 A】语言切换
├── AdviceActivity.kt        # 【可选 B】AI 建议页
├── TrendActivity.kt         # 【可选 C】趋势折线图页
├── data/
│   ├── Dates.kt             # 今天的日期
│   ├── WellnessRecord.kt    # 数据类
│   ├── WellnessDbHelper.kt  # SQLite
│   ├── AppLanguage.kt       # 【可选 A/B】语言枚举
│   ├── LanguageStore.kt     # 【可选 A】语言偏好
│   ├── PromptBuilder.kt     # 【可选 B】提示词
│   ├── DeepSeekClient.kt    # 【可选 B】HTTP
│   └── WellnessAdviceRepository.kt  # 【可选 B】仓库
└── ui/advice/               # 【可选 B】状态 + ViewModel
```

删掉三块可选功能后，就回到只有 5 个 Kotlin 文件的最小版本，
清单见 `docs/exam-guide.md` 第 4 节。

包名仍然是 `com.example.bmi`。改包名要动 `namespace`、`applicationId`、所有
`package` 声明和目录结构，既费时又容易出错，题目也不检查包名，所以保留不动。
界面主题名已经改成 `Theme.Wellness`，不再出现 BMI 字样。

---

## 5. 和原 BMI 作业的对应关系

理解这张表，就理解了这个题目 80% 的改法。

| BMI 作业 | Wellness Tracker | 说明 |
| --- | --- | --- |
| `heightET` / `weightET` / `ageET` | `stepsET` | 输入框从“多个数字”变成一个数字 |
| `genderSpinner` | `conditionSpinner` | Spinner 的用法完全一样：读 `selectedItemPosition` |
| “See BMI Report Now” 跳 `ReportActivity` | “Save today record” 直接存库 | 本题不需要“结果页”，保存就是最终动作 |
| `ReportActivity` 计算 BMI 并显示 | 无 | 少一个页面、少一次 Intent 传值 |
| `BmiDbHelper`（表 `bmi_records`） | `WellnessDbHelper`（表 `wellness_records`） | 结构照抄，只换表名和列 |
| `TrendActivity` + `BmiTrendChartView` 折线图 | `TrendActivity` + `LineChart`（可选 C） | 页面职责相同，画图从 Canvas 手绘换成 MPAndroidChart |
| `BmiPreferences` 自动填充 | 无 | 题目没要求“记住上次输入”，删掉 |
| 选项菜单 / 上下文菜单 / 关于对话框 | 无 | 题目没要求，删掉 |

一句话总结：**输入 → 校验 → 写库；另一个页面 → 读库 → 列表（需要时再加图表）。**
BMI 作业里“算 BMI”这一步被删掉了，其余骨架几乎原样复用。

---

## 6. 核心代码讲解

### 6.1 保存流程（`MainActivity.saveTodayRecord()`）

```kotlin
val steps = stepsET.text.toString().trim().toIntOrNull()
if (steps == null || steps < 0) {
    Toast.makeText(this, R.string.wellness_steps_warning, Toast.LENGTH_SHORT).show()
    return
}

val record = WellnessRecord(
    date = Dates.today(),
    condition = conditionSpinner.selectedItemPosition,
    steps = steps,
    note = noteET.text.toString().trim()
)
db.save(record)
```

要点：

- `trim()` 先去掉空格，否则 `" 8000 "` 会被 `toIntOrNull()` 判为非法；
- `toIntOrNull()` 一步完成“转数字 + 校验”，不需要 `try/catch`；
- 日期不让用户输入，直接取系统当天，避免格式错误，也符合“每天记录”；
- `selectedItemPosition` 是 `0/1/2`，和 `strings.xml` 里数组的顺序一一对应。

`Dates` 是一个通用的日期工具，只负责把系统时间格式化成 `yyyy-MM-dd`。
如果考试要求“不能出现任何与题目无关的名字”，它本身已经不含 BMI 字样，
不需要再改。

### 6.2 数据库（`WellnessDbHelper`）

表结构：

| 列 | 类型 | 含义 |
| --- | --- | --- |
| `date` | TEXT PRIMARY KEY | 日期，`yyyy-MM-dd`，一天只能有一行 |
| `condition` | INTEGER | 0=良好，1=一般，2=较差 |
| `steps` | INTEGER | 步数 |
| `note` | TEXT | 备注，可以为空字符串，但不会是 NULL |
| `updated_at` | INTEGER | 最后保存的时间戳（毫秒） |

保存时用了：

```kotlin
writableDatabase.insertWithOnConflict(
    TABLE_RECORDS, null, values, SQLiteDatabase.CONFLICT_REPLACE
)
```

因为 `date` 是主键，同一天再插入会冲突；`CONFLICT_REPLACE` 表示“删掉旧行、写入新行”。
于是“一天记多次只留最后一次”自动成立，不需要先查询再判断。

读取时：

```kotlin
readableDatabase.query(
    TABLE_RECORDS, columns, null, null, null, null, "$COL_DATE DESC"
)
```

`DESC` 让最新的日期排在最前面。`cursor.use { }` 保证读完自动关闭游标。

### 6.3 历史页（`HistoryActivity`）

```kotlin
val records = WellnessDbHelper(this).allRecords()
if (records.isEmpty()) {
    // 显示空状态
    return
}
val averageSteps = records.sumOf { it.steps }.toDouble() / records.size
summary.text = getString(R.string.wellness_history_summary, records.size, Math.round(averageSteps).toInt())

val rows = records.map { record ->
    mapOf(
        "title" to getString(R.string.wellness_history_title_row, record.date, conditionText(record.condition)),
        "subtitle" to buildSubtitle(record)
    )
}
list.adapter = SimpleAdapter(
    this, rows, R.layout.item_wellness,
    arrayOf("title", "subtitle"),
    intArrayOf(R.id.itemTitle, R.id.itemSubtitle)
)
```

- `ListView` 只负责滚动和回收视图，真正“一行长什么样”由 `item_wellness.xml` 决定；
- `SimpleAdapter` 是系统自带的适配器，把 `Map` 里的值按 key 填到指定控件，
  不用自己写 `BaseAdapter`，代码最少；
- `conditionText()` 把数据库里的 `0/1/2` 翻译成当前语言的文字，这样切换中英文时
  历史记录也会跟着变。

### 6.4 为什么不需要 Intent 传值

保存时数据已经写进 SQLite，历史页自己查询即可。这比“用 Intent 把列表传过去”
更简单，也更符合真实 App 的做法。原作业的趋势页也是这个思路。

---

## 7. 如果考试要求变了，怎么改

### 7.1 “同一天可以记多条”

把 `date` 从主键改成普通列，新增自增主键：

```sql
CREATE TABLE wellness_records (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    date TEXT NOT NULL,
    condition INTEGER NOT NULL,
    steps INTEGER NOT NULL,
    note TEXT NOT NULL,
    updated_at INTEGER NOT NULL
)
```

同时：

1. `DATABASE_VERSION` 从 1 改成 2（否则旧库不会重建表；
   教学项目也可以在系统设置里清除 App 数据）；
2. `save()` 改用普通 `insert(...)`（或 `CONFLICT_ABORT`），不要 `REPLACE`；
3. `allRecords()` 排序改成 `"$COL_DATE DESC, $COL_ID DESC"`；
4. 数据类和读取逻辑加上 `id`（删除/编辑单条时需要）。

### 7.2 “要能选择日期”

在主界面加一个按钮显示日期，点击弹 `DatePickerDialog`，把选中的日期存进变量，
保存时用它代替 `Dates.today()`。这样演示历史记录时不用真的等几天。

### 7.3 “要能删除一条记录”

给 `ListView` 设置 `setOnItemLongClickListener`，拿到位置对应的记录，
执行 `DELETE FROM wellness_records WHERE date = ?`，然后重新查询并
`adapter.notifyDataSetChanged()`（或用新的 adapter 重新赋值）。

### 7.4 “要画趋势图”

先判断要不要做：题目只说“查看历史”时，`HistoryActivity` 的列表已经够用，
**不要主动加图表**。只有题目明确写“图表 / 趋势 / 变化曲线”时才做。

本分支已经带了一个可以直接抄的示例：菜单 “Trend chart” → `TrendActivity`，
用 MPAndroidChart 画最近 7 条记录的步数折线。两种做法都在下面。

#### 方案一：MPAndroidChart（本项目采用，最省事）

MPAndroidChart 是一个成熟的开源图表库，`LineChart`（折线）+ `LineDataSet`（一组点）
两三个类就能出图，坐标轴、网格、缩放、点击高亮都由库负责。代价是多一个第三方依赖，
第一次 Sync 需要能访问 JitPack。

**第 1 步：仓库。** `settings.gradle` 的 `dependencyResolutionManagement` 里加：

```gradle
maven { url 'https://jitpack.io' }
```

不写这一行，Sync 会报 `Could not find com.github.PhilJay:MPAndroidChart:v3.1.0`。
如果校园网访问不了 JitPack，可以换成阿里云镜像或直接用方案二。

**第 2 步：依赖。** `app/build.gradle` 的 `dependencies` 里加：

```gradle
implementation 'com.github.PhilJay:MPAndroidChart:v3.1.0'
```

**第 3 步：布局。** 自定义控件在 XML 里要写**完整类名**，并且必须给固定高度
（`LineChart` 不会像 `TextView` 那样自己撑开，`wrap_content` 会变成 0 高）：

```xml
<com.github.mikephil.charting.charts.LineChart
    android:id="@+id/trendChart"
    android:layout_width="match_parent"
    android:layout_height="280dp" />
```

**第 4 步：喂数据。** 核心只有四行——把记录变成 `Entry(x, y)`，装进
`LineDataSet`，再交给 `LineData`：

```kotlin
// 库里按日期倒序返回，折线要从早到晚，所以 take(7) 之后 reversed()
val records = WellnessDbHelper(this).allRecords().take(7).reversed()
val entries = records.mapIndexed { i, r -> Entry(i.toFloat(), r.steps.toFloat()) }

val dataSet = LineDataSet(entries, "Steps").apply {
    color = Color.parseColor("#2E7D32")
    lineWidth = 2.5f
    setCircleColor(color)
    circleRadius = 4f
    setDrawValues(false)      // 点旁边不标数字，7 个点会挤在一起
    setDrawFilled(true)
    fillAlpha = 40
}

chart.xAxis.apply {
    position = XAxis.XAxisPosition.BOTTOM
    granularity = 1f
    setDrawGridLines(false)
    labelCount = records.size
    labelRotationAngle = -30f
    valueFormatter = object : ValueFormatter() {
        override fun getFormattedValue(value: Float): String {
            val i = value.toInt()
            return if (i in records.indices) records[i].date.takeLast(5) else ""
        }
    }
}
chart.axisRight.isEnabled = false
chart.axisLeft.axisMinimum = 0f
chart.description.isEnabled = false
chart.legend.isEnabled = false
chart.setScaleEnabled(false)   // 页面套在 ScrollView 里时关掉缩放，避免抢手势
chart.data = LineData(dataSet)
chart.invalidate()             // 数据变了必须调用，否则不刷新
```

完整带注释的版本见本分支的 `TrendActivity.kt` 和 `activity_trend.xml`。
从零工程的完整搭建顺序（仓库 → 依赖 → 布局 → Activity → Manifest）见
`docs/from-scratch.md` 7.6。
还要补三处（和新增页面一样）：Manifest 注册 `TrendActivity`、菜单里加一项、
`strings.xml` 加 `trend_*` 文案。

#### 方案二：照 `BmiTrendChartView` 自己写自定义 View（零依赖）

原 BMI 作业的趋势图是手写的，类名 `BmiTrendChartView`，位置：

```
app/src/main/java/com/example/bmi/ui/trend/BmiTrendChartView.kt
```

它不在当前分支（当前分支的旧代码已清理），但可以从提交历史里直接看：

```bat
git show main:app/src/main/java/com/example/bmi/ui/trend/BmiTrendChartView.kt
```

或者切到 `main` / `cityu_lab` 分支用 Android Studio 打开。它的本质就是
“继承 `View` + 重写 `onDraw` + 用 `Canvas` 和 `Paint` 画线”，一共四块：

| 部分 | 作用 |
| --- | --- |
| 构造函数（`@JvmOverloads`） | 让 XML 里能直接写类名，并支持 `style` 属性 |
| `setData(values, labels)` | 保存数据 + `invalidate()` 触发重绘 |
| `onDraw(canvas)` | 把数值映射成坐标，画坐标轴、网格、折线、圆点、日期文字 |
| 空数据分支 | 没有记录时画一行提示，而不是画一条空坐标轴 |

最少可用的骨架（可以直接贴进新文件，按题目改颜色和文字即可）：

```kotlin
package com.example.bmi.ui.trend

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.View

/** 最近 N 天折线图：只画一条线 + 日期标签，够考试用。 */
class TrendChartView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private val linePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 4f
        color = Color.parseColor("#2E7D32")
    }
    private val dotPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#2E7D32")
    }
    private val axisPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        strokeWidth = 2f
        color = Color.LTGRAY
    }
    private val labelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textSize = 26f
        color = Color.GRAY
        textAlign = Paint.Align.CENTER
    }

    private var values: List<Int> = emptyList()
    private var labels: List<String> = emptyList()

    fun setData(values: List<Int>, labels: List<String>) {
        this.values = values
        this.labels = labels
        invalidate()              // 关键：不调用就不会重绘
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        if (values.isEmpty()) return

        val left = 32f
        val right = width - 32f
        val top = 24f
        val bottom = height - 48f
        val maxValue = (values.maxOrNull() ?: 0).coerceAtLeast(1)

        // 底线
        canvas.drawLine(left, bottom, right, bottom, axisPaint)

        val path = Path()
        values.forEachIndexed { i, value ->
            val x = if (values.size == 1) (left + right) / 2f
                    else left + (right - left) * i / (values.size - 1)
            val y = bottom - (bottom - top) * value / maxValue

            if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
            canvas.drawCircle(x, y, 5f, dotPaint)
            canvas.drawText(labels.getOrElse(i) { "" }, x, bottom + 32f, labelPaint)
        }
        canvas.drawPath(path, linePaint)
    }
}
```

几点必须注意：

- `setData()` 里一定要 `invalidate()`，否则数据变了画面不动；
- `x` 的间隔按 `width` 算，不要写死像素，否则换屏幕会错位；
- `y` 要反过来算：屏幕坐标向下为正，所以 `bottom - 比例 * 高度`；
- 只有一个数据点时 `size - 1 == 0`，必须单独处理（上面用 `values.size == 1` 分支）；
- 布局里同样要给固定高度：`android:layout_height="280dp"`；
- 想画柱状图就把 `drawPath` 换成 `canvas.drawRect(...)`，其余结构不变。

用的时候只要：

```kotlin
val chart: TrendChartView = findViewById(R.id.trendChart)
chart.setData(records.map { it.steps }, records.map { it.date.takeLast(5) })
```

什么时候选哪个：**考试只剩十分钟 → 方案一抄 `TrendActivity`；机房装不了依赖 /
题目不允许第三方库 → 方案二抄上面的骨架。** 两种做法都需要“读最近 N 条 →
按时间正序 → 映射坐标”这三步，换库不换思路。

---

## 8. 旧代码清理记录（已完成）

当前分支已经清理完毕：BMI 相关代码全部删除。之后为了留作考试参考，又在健康日志
之上加回了**新写**的语言切换和 AI 代码（见 4.1 节），它们用
`【语言切换 · 考试可删】` / `【AI 功能 · 考试可删】` 标记，删除清单在
`docs/exam-guide.md`。本节记录的是“删旧 BMI 代码”的过程。

清理顺序如下——如果你在考试现场也想做同样的清理，建议按这个顺序，
每完成一组就编译一次：

1. **先换日期工具**：新增 `data/Dates.kt`，把 `MainActivity` 里的 `BmiDates`
   全部替换成 `Dates`，编译通过后再删 `BmiDates.kt`；
2. 删页面：`ReportActivity.kt`、`TrendActivity.kt`；
3. 删 UI：`ui/report/`、`ui/trend/` 整个目录；
4. 删数据/AI：`BmiDbHelper.kt`、`BmiHistoryRepository.kt`、`BmiPreferences.kt`、
   `BmiProfile.kt`、`BmiRecord.kt`、`Gender.kt`、`DeepSeekClient.kt`、
   `HealthAdviceRepository.kt`、`PromptBuilder.kt`、`AppLanguage.kt`；
5. 删布局：`activity_report.xml`、`activity_trend.xml` 及 `layout-land/` 下对应文件；
6. 删菜单和图片：`res/menu/` 下两个文件、`drawable/bot_*.png`；
7. 删旧测试：`PromptBuilderTest.kt`；
8. 清 `AndroidManifest.xml`：删除 `ReportActivity`、`TrendActivity` 注册和
   `INTERNET` 权限；
9. 清 `strings.xml`：只保留 `app_name` 和 `wellness_*`，中英文两个文件同步；
10. 清 `app/build.gradle`：删除 DeepSeek 密钥读取、`buildConfigField`、
    `buildFeatures { buildConfig = true }`，以及不再使用的
    ConstraintLayout / lifecycle / 协程依赖；
11. 主题改名：`Theme.BMI` → `Theme.Wellness`（`values/` 和 `values-night/` 两处，
    以及 Manifest 的引用）；
12. **Build → Clean Project → Rebuild Project**，再跑一遍第 9 节的自测清单。

删除时最容易踩的坑是“漏改引用”。规则很简单：**先全局搜索类名，确认只剩定义
文件本身，再删。** 例如 `AppLanguage` 同时被 `PromptBuilder`、`ReportViewModel`
和 `ReportActivity` 使用，必须等 AI 那一组文件一起删；`BmiDates` 被
`MainActivity` 使用，必须先替换成 `Dates`。

> 注意：第 4、7、10 步是“删掉旧版 AI”的动作。现在分支里又有了**新版**的
> `DeepSeekClient`、`PromptBuilder`、`AppLanguage` 和 `PromptBuilderTest`，
> 它们是新写的、服务于可选功能 B。要删它们请照 `docs/exam-guide.md` 第 2 节，
> 不要照本节。

原始 BMI 代码在 `cityu_lab` 分支仍然完整保留，需要对照时随时可以切回去看。

---

## 9. 自测清单

**基础功能**

- [ ] 什么都不填，直接点保存：出现“请输入有效的步数”，不崩溃、不写库。
- [ ] 输入 `abc`：数字键盘通常打不出来；如果能输入，仍然被校验拦住。
- [ ] 输入 `0`：允许保存（0 步是合法值）。
- [ ] 保存后打开历史：能看到刚保存的日期、状况、步数、备注。
- [ ] 同一天保存两次：历史里只有一条，显示第二次的内容。
- [ ] 历史为空时：显示“暂无记录”，不显示空白列表。
- [ ] 旋转屏幕：不崩溃（因为已删除旧的横屏主布局）。
- [ ] 从历史页按返回键：回到主界面，输入内容还在。
- [ ] 杀掉 App 再打开：历史记录仍然存在（数据真的写进了 SQLite）。

**可选 A：中英文切换**

- [ ] 菜单点“切换语言”：界面立刻变英文，标题栏也变。
- [ ] 再点一次：变回中文。
- [ ] 切换后进历史页 / AI 页 / 趋势页：语言保持一致。
- [ ] 杀掉 App 再打开：还是上次选的语言（SharedPreferences 生效）。
- [ ] 清除 App 数据后重开：跟随手机系统语言。

**可选 B：AI 健康建议**

- [ ] 今天没有记录时进 AI 页：提示“今天还没有记录”，不发请求。
- [ ] 有记录时进 AI 页：先转圈，然后显示 3 条建议。
- [ ] 中文界面拿到中文建议；英文界面拿到英文建议。
- [ ] 断开网络再进：提示网络错误，出现“重试”按钮。
- [ ] 把 `local.properties` 的 key 清空后重编译：提示“没有配置 API key”。
- [ ] 转屏：不重复请求（ViewModel 存活，`hasRequested` 生效）。

**可选 C：趋势折线图**

- [ ] 一条记录都没有时进趋势页：显示“还没有记录”，不画空坐标轴。
- [ ] 有 2 条以上记录时：折线按日期从早到晚排列，点数和记录数一致。
- [ ] 横轴日期显示成 `MM-DD`，不互相重叠（倾斜 -30°）。
- [ ] 同一天保存两次：图上仍然只有一个点（覆盖，不是两条）。
- [ ] 只有一条记录：点画在中间，不崩溃（`size - 1 == 0` 已单独处理）。
- [ ] 步数全为 0：折线贴在底部，纵轴仍从 0 开始，不崩溃。
- [ ] 退出再进、转屏：图表重新从数据库读数据，显示正常。
- [ ] 趋势页上下滑动：页面能正常滚，不被图表抢走手势。

---

## 10. 相关文档

- `README.md`：项目简介、工具链、运行和 patch 用法。
- `docs/from-scratch.md`：从零建工程（考试模拟），含版本差异与踩坑清单。
- `docs/adapt-to-other-topic.md`：换题目时改哪里（C1–C11 改题点地图 + 三个示例）。
- `docs/exam-guide.md`：考试删减指南（不要语言切换 / 不要 AI / 不要图表时删什么）。
- `docs/llm-setup.md`：AI 与语言切换的实现细节、key 配置、单元测试。
- `docs/exam-playbook.md`：把这道题的做法迁移到其他“每日记录类”题目的通用教程。
- `patch/wellness-tracker.patch`：可直接应用到 `d43a3c9` 的补丁（包含旧代码删除）。
