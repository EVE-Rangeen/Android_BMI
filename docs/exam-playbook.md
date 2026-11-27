# 考场通用教程：把 BMI 作业改成任意“每日记录类”App

老师已经说了：考试题会和 BMI 作业“十分相似”。这句话的含金量很高——相似的不是
BMI 公式，而是**项目的骨架**：

```
输入页（表单） → 校验 → 保存到 SQLite → 另一个页面读出历史 → 列表/统计/图表
```

BMI 作业把这套骨架完整演示了一遍。考试无论换成健康日志、心情日记、喝水记录、
记账、学习打卡，只要还是“每天记点什么，然后能看历史”，改的都是同一批位置。

这份教程给你两样东西：

1. 一套**七步改造法**，从现有代码出发，不引入新框架、不改构建配置；
2. 一张**变体对照表**，告诉你不同题目分别改哪几个字段和控件。

配套的完整示例就是本仓库的 `wellness_tracker` 分支（见
`docs/wellness-tracker.md`），可以对照着看。

> 注意：`wellness_tracker` 分支已经删除了旧 BMI 代码。本文提到的
> `BmiDbHelper.kt`、`BmiRecord.kt`、`BmiPreferences.kt` 等文件名，指的是
> `cityu_lab` 分支（你已提交的作业）里的代码，用来对照抄写；
> 当前分支只剩 `Wellness*` 和 `Dates`。

> 额外参考：当前分支保留了**新写的**“App 内中英文切换”“AI 健康建议”和
> “最近 7 天趋势折线图”三块可选功能。如果考题正好要求其中之一，直接照抄；
> 如果考题用不上，按 `docs/exam-guide.md` 的清单整块删除即可
> （本文只在第 4 节“要图表时”讲图表，其余两块不展开）。

---

## 1. 读题三分钟：先填一张表

不要急着写代码。先回答四个问题，把答案写成一张表：

| 问题 | 例子（Wellness Tracker） |
| --- | --- |
| 一条记录要存哪些信息？ | 日期、身体状况、步数、备注 |
| 每天只能有一条，还是可以多条？ | 每天一条，后保存的覆盖先保存的 |
| 需要统计/图表吗？ | 不需要，只需要历史列表 |
| 需要编辑/删除吗？ | 不需要 |

然后为每个字段决定三件事：

| 字段 | 类型 | 界面控件 | 校验 |
| --- | --- | --- | --- |
| 日期 | `String`（`yyyy-MM-dd`） | 不输入，取系统当天 | 无 |
| 身体状况 | `Int`（0/1/2） | `Spinner` | 一定有值 |
| 步数 | `Int` | `EditText` + `inputType="number"` | 非空、非负 |
| 备注 | `String` | 多行 `EditText` | 可为空 |

这张表决定了后面 90% 的代码。**表没想清楚就开始写，是考场最大的时间浪费。**

---

## 2. 可复用的骨架

```
MainActivity                     WellnessDbHelper                HistoryActivity
────────────                     ─────────────────               ───────────────
findViewById 找控件              继承 SQLiteOpenHelper            allRecords()
读 EditText / Spinner            onCreate() 建表                 空状态 / 统计
校验 toIntOrNull()               save() 写一行                    ListView + Adapter
new 一个数据类                    allRecords() 读全部              显示
db.save(record)
startActivity(HistoryActivity)
```

三个 Kotlin 文件（主界面、数据库助手、历史页）加一个数据类，
两个布局文件（主界面、历史页），就是全部。

---

## 3. 七步改造法

### 第 0 步：保留什么，删什么

**保留**（考试时别碰）：

- `app/build.gradle`、`settings.gradle`、`gradle/`、主题、颜色、图标；
- 包名 `com.example.bmi`、`applicationId`、`MainActivity` 的类名；
- `AndroidManifest.xml` 里 `MainActivity` 的 `MAIN/LAUNCHER` 注册。

**可以不管**：

- 旧的 `ReportActivity`、AI 相关类、旧布局、旧字符串。它们编译得过、界面上进不去，
  不会影响新功能。考试时间宝贵，不要做“大扫除”。
- 注意当前分支的 `TrendActivity` 是**新写的**趋势图页（可选 C），不是旧代码；
  用不上就整块删，见 `docs/exam-guide.md` 第 3 节。

**必须处理**：

- 如果主界面布局被整体换掉，检查 `res/layout-land/` 里有没有同名布局。
  有就删掉或同步修改，否则横屏时会加载旧布局，`findViewById` 返回 null 直接崩溃。

### 第 1 步：写数据类（5 分钟）

新建 `data/Record.kt`（名字随题目改）：

```kotlin
package com.example.bmi.data

data class Record(
    val date: String,      // yyyy-MM-dd；如果一天可以多条，这里换成 id: Int
    val type: Int,         // Spinner 的序号，0/1/2…
    val amount: Int,       // 数字字段：步数/毫升/分钟/页数/金额（金额用 Double）
    val note: String       // 备注，可为空字符串
)
```

`data class` 会自动生成 `equals`、`toString` 等，纯数据用它最合适。

### 第 2 步：写数据库助手（10 分钟）

直接复制原作业的 `BmiDbHelper.kt`，改四处：**类名、库名、表名/列名、CRUD 方法**。

```kotlin
package com.example.bmi.data

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

class RecordDbHelper(context: Context) : SQLiteOpenHelper(
    context.applicationContext, "records.db", null, 1
) {
    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE records (
                date TEXT PRIMARY KEY NOT NULL,
                type INTEGER NOT NULL,
                amount INTEGER NOT NULL,
                note TEXT NOT NULL,
                updated_at INTEGER NOT NULL
            )
            """.trimIndent()
        )
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        db.execSQL("DROP TABLE IF EXISTS records")
        onCreate(db)
    }

    fun save(record: Record) {
        val values = ContentValues().apply {
            put("date", record.date)
            put("type", record.type)
            put("amount", record.amount)
            put("note", record.note)
            put("updated_at", System.currentTimeMillis())
        }
        // date 是主键：同一天再存会替换旧行
        writableDatabase.insertWithOnConflict(
            "records", null, values, SQLiteDatabase.CONFLICT_REPLACE
        )
    }

    fun allRecords(): List<Record> {
        val list = mutableListOf<Record>()
        val cursor = readableDatabase.query(
            "records",
            arrayOf("date", "type", "amount", "note"),
            null, null, null, null,
            "date DESC"
        )
        cursor.use { c ->
            while (c.moveToNext()) {
                list.add(
                    Record(
                        date = c.getString(0),
                        type = c.getInt(1),
                        amount = c.getInt(2),
                        note = c.getString(3)
                    )
                )
            }
        }
        return list
    }
}
```

关键点：

- `onUpgrade` 在**版本号变大**时才调用。改了表结构一定把版本号 +1；
  教学项目里更快的办法是卸载 App 或清除 App 数据。
- 列名必须和 `ContentValues` 的 key、`Cursor` 的读取顺序一致，错一个就崩。
- 日期做主键 + `CONFLICT_REPLACE` = 一天只留最后一条。这是一套组合拳，缺一不可。
- 如果题目要求“同一天多条”，见第 4 节。

### 第 3 步：改主界面布局（10 分钟）

把 `activity_main.xml` 换成纵向 `LinearLayout`，而不是继续和
`ConstraintLayout` 的约束属性较劲：

```xml
<ScrollView xmlns:android="http://schemas.android.com/apk/res/android"
    android:layout_width="match_parent"
    android:layout_height="match_parent"
    android:fillViewport="true">

    <LinearLayout
        android:layout_width="match_parent"
        android:layout_height="wrap_content"
        android:orientation="vertical"
        android:padding="24dp">

        <TextView android:id="@+id/dateTV"
            android:layout_width="match_parent"
            android:layout_height="wrap_content" />

        <Spinner android:id="@+id/typeSpinner"
            android:layout_width="match_parent"
            android:layout_height="wrap_content"
            android:layout_marginTop="16dp"
            android:entries="@array/record_types" />

        <EditText android:id="@+id/amountET"
            android:layout_width="match_parent"
            android:layout_height="wrap_content"
            android:layout_marginTop="16dp"
            android:hint="@string/amount_hint"
            android:inputType="number" />

        <EditText android:id="@+id/noteET"
            android:layout_width="match_parent"
            android:layout_height="wrap_content"
            android:layout_marginTop="16dp"
            android:gravity="top|start"
            android:hint="@string/note_hint"
            android:inputType="textMultiLine"
            android:minLines="3" />

        <Button android:id="@+id/saveBtn"
            android:layout_width="match_parent"
            android:layout_height="wrap_content"
            android:layout_marginTop="24dp"
            android:text="@string/save" />

        <Button android:id="@+id/historyBtn"
            style="@style/Widget.MaterialComponents.Button.OutlinedButton"
            android:layout_width="match_parent"
            android:layout_height="wrap_content"
            android:layout_marginTop="8dp"
            android:text="@string/view_history" />

    </LinearLayout>
</ScrollView>
```

要点：

- 外面套 `ScrollView`，小屏幕 + 弹出键盘时不会把按钮顶出屏幕；
- `inputType="number"` 只限制键盘，代码里仍要校验；
- 每个控件都必须有 `android:id`，否则 `findViewById` 找不到。

### 第 4 步：写保存逻辑（10 分钟）

`MainActivity.onCreate()` 里找控件、绑事件，保存方法照下面写：

```kotlin
private lateinit var db: RecordDbHelper

override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    setContentView(R.layout.activity_main)
    db = RecordDbHelper(this)

    val amountET: EditText = findViewById(R.id.amountET)
    val noteET: EditText = findViewById(R.id.noteET)
    val typeSpinner: Spinner = findViewById(R.id.typeSpinner)

    findViewById<TextView>(R.id.dateTV).text =
        getString(R.string.today, SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date()))

    findViewById<Button>(R.id.saveBtn).setOnClickListener {
        val amount = amountET.text.toString().trim().toIntOrNull()
        if (amount == null || amount < 0) {
            Toast.makeText(this, R.string.invalid_amount, Toast.LENGTH_SHORT).show()
            return@setOnClickListener
        }
        db.save(
            Record(
                date = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date()),
                type = typeSpinner.selectedItemPosition,
                amount = amount,
                note = noteET.text.toString().trim()
            )
        )
        Toast.makeText(this, R.string.saved, Toast.LENGTH_SHORT).show()
    }

    findViewById<Button>(R.id.historyBtn).setOnClickListener {
        startActivity(Intent(this, HistoryActivity::class.java))
    }
}
```

如果你直接复用本项目的 `BmiDates.today()`，就不用写 `SimpleDateFormat`。

**校验的三条铁律**：

1. 空字符串转数字会得到 null，必须判断；
2. 负数、超大数要按题目要求限制；
3. 校验失败就 `return`，绝不能带着非法值写库。

### 第 5 步：写历史页（15 分钟）

新建 `HistoryActivity.kt` 和 `activity_history.xml`。最简单的版本：

```kotlin
class HistoryActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_history)

        val list: ListView = findViewById(R.id.historyList)
        val empty: TextView = findViewById(R.id.historyEmpty)
        val records = RecordDbHelper(this).allRecords()

        if (records.isEmpty()) {
            list.visibility = View.GONE
            empty.visibility = View.VISIBLE
            return
        }

        val rows = records.map { r ->
            "${r.date}  ·  ${typeText(r.type)}  ·  ${r.amount}" +
                if (r.note.isBlank()) "" else "  ·  ${r.note}"
        }
        list.adapter = ArrayAdapter(
            this,
            android.R.layout.simple_list_item_1,
            rows
        )
    }

    private fun typeText(type: Int): String = getString(
        when (type) {
            0 -> R.string.type_0
            1 -> R.string.type_1
            else -> R.string.type_2
        }
    )
}
```

布局只要三样：

```xml
<LinearLayout xmlns:android="http://schemas.android.com/apk/res/android"
    android:layout_width="match_parent"
    android:layout_height="match_parent"
    android:orientation="vertical"
    android:padding="16dp">

    <TextView android:id="@+id/historySummary"
        android:layout_width="match_parent"
        android:layout_height="wrap_content" />

    <TextView android:id="@+id/historyEmpty"
        android:layout_width="match_parent"
        android:layout_height="0dp"
        android:layout_weight="1"
        android:gravity="center"
        android:visibility="gone"
        android:text="@string/history_empty" />

    <ListView android:id="@+id/historyList"
        android:layout_width="match_parent"
        android:layout_height="0dp"
        android:layout_weight="1" />

</LinearLayout>
```

如果想做成两行更漂亮，直接抄本项目的 `item_wellness.xml` + `SimpleAdapter`，
不要自己写 `BaseAdapter`（`getView` 里的 ViewHolder 复用很容易写错）。

### 第 6 步：注册页面 + 加文案（5 分钟）

`AndroidManifest.xml` 的 `<application>` 里加：

```xml
<activity
    android:name=".HistoryActivity"
    android:exported="false"
    android:label="@string/history_title" />
```

`strings.xml` 里加文案和 Spinner 数组：

```xml
<string name="app_name">Wellness Tracker</string>
<string name="today">Today: %1$s</string>
<string name="save">Save</string>
<string name="view_history">View history</string>
<string name="history_title">History</string>
<string name="history_empty">No records yet.</string>
<string name="invalid_amount">Please enter a valid number.</string>
<string name="saved">Saved.</string>
<string name="type_0">Good</string>
<string name="type_1">Fair</string>
<string name="type_2">Poor</string>

<string-array name="record_types">
    <item>@string/type_0</item>
    <item>@string/type_1</item>
    <item>@string/type_2</item>
</string-array>
```

如果项目有 `values-zh-rCN/strings.xml`，**所有 key 都要同步加一份中文**，
否则中文系统下会回落到英文；漏掉也不会崩，但会显得没做完。

### 第 7 步：编译 + 按清单自测（5 分钟）

先 `Build → Make Project`，再 Run。测试顺序：

1. 空输入 → 应提示，不崩；
2. 正常输入 → 保存成功；
3. 打开历史 → 能看到刚才的记录；
4. 同一天再存一次 → 还是一条（或被更新）；
5. 旋转屏幕 → 不崩；
6. 杀进程重开 → 记录还在；
7. 切换语言 → 文字正常。

---

## 4. 变体对照表

| 题目 | 数据类字段 | 控件选择 | 额外注意 |
| --- | --- | --- | --- |
| 健康日志 Wellness | `date, condition, steps, note` | Spinner + 数字输入 + 备注 | 本分支示例 |
| 心情日记 Mood | `date, mood, note` | Spinner（或 RatingBar/SeekBar） | 心情等级用 0..4 |
| 喝水记录 Water | `date, ml` | 数字输入，或几个快捷按钮（+200ml） | 同一天要**累加**：先查再更新，或 SQL `UPDATE ... SET ml = ml + ?` |
| 学习打卡 Study | `date, minutes, subject` | Spinner + 数字输入 | 统计总时长用 `SUM(minutes)` |
| 记账 Expense | `id, date, amount, category, note` | 小数输入 + Spinner | 一天多条 → 自增 id 主键；金额用 `Double`/`REAL` |
| 睡眠记录 Sleep | `date, hours, quality` | 小数输入 + Spinner | `hours` 用 `Double` |
| 读书打卡 Reading | `date, pages, title` | 数字输入 + 文本输入 | 书名可为空要允许 |
| 运动记录 Workout | `date, type, minutes` | Spinner + 数字输入 | 可能一天多种运动 → 自增 id |

### 按天唯一 vs 一天多条

**按天唯一**（默认，最省事）：

```sql
date TEXT PRIMARY KEY NOT NULL
```

保存用 `insertWithOnConflict(..., CONFLICT_REPLACE)`，读取按 `date DESC` 排序。

**一天多条**：

```sql
id INTEGER PRIMARY KEY AUTOINCREMENT,
date TEXT NOT NULL
```

保存用普通 `insert(...)`，读取按 `date DESC, id DESC` 排序。
数据类要加 `id: Int`，删除/编辑时用 `WHERE id = ?`。

### 要统计时

- 平均值、总和在 Kotlin 里算最简单：
  `records.map { it.amount }.average()`、`records.sumOf { it.amount }`；
- 也可以在 SQL 里算：`SELECT SUM(amount), AVG(amount) FROM records`；
- 考试优先选 Kotlin，因为不用多写一个 Cursor 解析。

### 要图表时

先判断：题目只说“查看历史”，列表就够，**不要主动加图表**。只有出现
“趋势 / 变化 / 图表 / 曲线”这类字眼才做，优先做折线或柱状，不要做饼图。

**做法一：MPAndroidChart（推荐，本项目的可选 C 就是这样做的）**

1. `settings.gradle` 加仓库：

```gradle
maven { url 'https://jitpack.io' }
```

2. `app/build.gradle` 加依赖，然后 **Sync**：

```gradle
implementation 'com.github.PhilJay:MPAndroidChart:v3.1.0'
```

3. 布局里写**完整类名**，高度必须固定（`wrap_content` 会变成 0 高、一片空白）：

```xml
<com.github.mikephil.charting.charts.LineChart
    android:id="@+id/trendChart"
    android:layout_width="match_parent"
    android:layout_height="280dp" />
```

4. Activity 里三行核心代码 + 坐标轴配置：

```kotlin
// 库里按日期倒序，画图要正序，所以 take(N) 之后 reversed()
val records = db.allRecords().take(7).reversed()     // db = RecordDbHelper(this)
val entries = records.mapIndexed { i, r -> Entry(i.toFloat(), r.amount.toFloat()) }

val set = LineDataSet(entries, "Steps").apply {
    color = Color.parseColor("#2E7D32")
    setDrawValues(false)        // 点旁边不标数字
    setDrawFilled(true)
}

chart.xAxis.apply {
    position = XAxis.XAxisPosition.BOTTOM
    granularity = 1f            // 一个点一个刻度
    setDrawGridLines(false)
    labelCount = records.size
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
chart.data = LineData(set)
chart.invalidate()              // 不调用不刷新
```

5. 三处外围照新增页面处理：Manifest 注册、菜单项、`strings.xml` 文案。

完整带注释的版本见 `TrendActivity.kt` / `activity_trend.xml`。
如果 JitPack 下载不下来（校园网常见），直接用做法二。

**做法二：照 `BmiTrendChartView` 写自定义 View（零依赖）**

原作业的趋势图是手写的，类在 `main` / `cityu_lab` 分支：

```
app/src/main/java/com/example/bmi/ui/trend/BmiTrendChartView.kt
```

不在那个分支也能看：`git show main:app/src/main/java/com/example/bmi/ui/trend/BmiTrendChartView.kt`。
它只有四块：继承 `View` → `setData()` 存数据并 `invalidate()` → `onDraw()` 里
把数值映射成坐标（x 按宽度均分，y = `bottom - 比例 * 高度`）→ 用 `Path` 连线、
`drawCircle` 画点、`drawText` 写日期。可直接复制的骨架在
`docs/wellness-tracker.md` 的 7.4 节，按题目改颜色和文案即可。

**时间不够时**：把折线换成柱状（`canvas.drawRect`）更短；再退一步，用
`TextView` 列出“日期 · 数值”也算展示了变化，比空着强。

---

## 5. 把 Wellness 示例改成另一道题：只改 8 个地方

如果你打算考试时直接套用本项目的 `wellness_tracker` 代码，按顺序改这 8 处：

1. **`strings.xml`（默认 + 中文）**：`app_name`、标签、提示、数组项；
2. **`WellnessRecord.kt`**：字段名/类型（例如 `steps` → `ml`，`condition` → `mood`）；
3. **`WellnessDbHelper.kt`**：
   - `DATABASE_NAME`；
   - `CREATE TABLE` 的列；
   - `save()` 里的 `put(...)`；
   - `allRecords()` 的列数组、`getXxx()` 顺序、`Record(...)` 构造；
4. **`activity_main.xml`**：控件类型（数字/小数/多行）、`inputType`、`hint`；
5. **`MainActivity.kt`**：读取哪些控件、怎么校验、数据类怎么构造；
6. **`HistoryActivity.kt`**：`conditionText()` 换成新字段的翻译、行文字格式；
7. **`item_wellness.xml`**：如果行数/样式要变；
8. **`AndroidManifest.xml`**：只有新增页面时才需要改（改字段不用动）。

**不要改**：包名、`applicationId`、Gradle 版本、主题、`MainActivity` 的入口注册。
这些和题目无关，改了只会引入新问题。

### 具体例子：把 Wellness 改成“睡眠记录（Sleep Log）”

假设考题是：每天记录**睡眠时长（小时，可以有小数）**、**睡眠质量**（Good / Fair / Poor）
和备注。我们保留类名 `WellnessRecord` / `WellnessDbHelper` 和控件 id 不动，
只换字段名、类型和文案——这样除了下面 8 处，其它文件一行都不用改。

先看新旧对照：

| 原来 | 改成 |
| --- | --- |
| `WellnessRecord.condition: Int` | `quality: Int`（Spinner 序号 0/1/2） |
| `WellnessRecord.steps: Int` | `hours: Double`（睡眠小时数） |
| 库 `wellness.db` / 表 `wellness_records` | `sleep.db` / `sleep_records` |
| 列 `condition` / `steps` | `quality` / `hours`（`REAL`） |
| 历史行“日期 · 状况 / 8000 steps” | “日期 · 质量 / 7.5 hours” |

第 1 处：**`strings.xml`（默认 + 中文各一份）**。key 不改，只改 value：

```xml
<string name="app_name">Sleep Log</string>
<string name="wellness_condition_label">How did you sleep?</string>
<string name="wellness_condition_good">Good</string>
<string name="wellness_condition_fair">Fair</string>
<string name="wellness_condition_poor">Poor</string>
<string name="wellness_steps_label">Hours slept</string>
<string name="wellness_steps_hint">e.g. 7.5</string>
<string name="wellness_note_label">Notes (optional)</string>
<string name="wellness_note_hint">Dreams, waking up, caffeine…</string>
<string name="wellness_steps_warning">Please enter hours between 0 and 24.</string>

<!-- 历史页：数字从“整数 + steps”改成“一位小数 + hours” -->
<string name="wellness_history_summary">%1$d days recorded · average %2$.1f hours</string>
<string name="wellness_history_steps">%1$.1f hours</string>
```

`wellness_condition` 数组不用改结构，三项仍然指向上面三个字符串。

第 2 处：**`WellnessRecord.kt`**：

```kotlin
data class WellnessRecord(
    val date: String,
    val quality: Int,     // 原来是 condition
    val hours: Double,    // 原来是 steps: Int
    val note: String
)
```

第 3 处：**`WellnessDbHelper.kt`**，按顺序改四小项：

```kotlin
// ① 常量
const val DATABASE_NAME = "sleep.db"
const val DATABASE_VERSION = 2          // 表结构变了必须 +1，否则旧库不重建
const val TABLE_RECORDS = "sleep_records"
const val COL_QUALITY = "quality"
const val COL_HOURS = "hours"

// ② 建表 SQL（hours 用 REAL 存小数）
CREATE TABLE sleep_records (
    date TEXT PRIMARY KEY NOT NULL,
    quality INTEGER NOT NULL,
    hours REAL NOT NULL,
    note TEXT NOT NULL,
    updated_at INTEGER NOT NULL
)

// ③ save()
put(COL_QUALITY, record.quality)
put(COL_HOURS, record.hours)

// ④ allRecords()（findByDate() 里的列数组和构造方法也要同步改）
arrayOf(COL_DATE, COL_QUALITY, COL_HOURS, COL_NOTE)
...
WellnessRecord(
    date = c.getString(0),
    quality = c.getInt(1),
    hours = c.getDouble(2),     // 注意是 getDouble，不是 getInt
    note = c.getString(3)
)
```

第 4 处：**`activity_main.xml`**。id 和控件类型不变，只把步数输入框改成允许小数：

```xml
<EditText
    android:id="@+id/stepsET"
    ...
    android:hint="@string/wellness_steps_hint"
    android:inputType="numberDecimal" />
```

Spinner 的 `entries="@array/wellness_condition"` 不用动（文案已经在第 1 处换了）。
`label` 文字来自 strings，所以也不用手改。

第 5 处：**`MainActivity.saveTodayRecord()`**：小数解析 + 范围校验 + 新字段：

```kotlin
val hours = stepsET.text.toString().trim().toDoubleOrNull()
if (hours == null || hours < 0.0 || hours > 24.0) {
    Toast.makeText(this, R.string.wellness_steps_warning, Toast.LENGTH_SHORT).show()
    return
}

val record = WellnessRecord(
    date = Dates.today(),
    quality = conditionSpinner.selectedItemPosition,
    hours = hours,
    note = noteET.text.toString().trim()
)
```

`toDoubleOrNull()` 负责“转小数 + 校验”，空串和字母都会返回 null。
变量名 `steps` 顺手改成 `hours` 即可，IDE 会提示所有引用点。

第 6 处：**`HistoryActivity.kt`**：改翻译函数和行文字：

```kotlin
// conditionText(...) → qualityText(...)，内部还是 0/1/2 映射三个 string
private fun qualityText(quality: Int): String = getString(
    when (quality) {
        0 -> R.string.wellness_condition_good
        1 -> R.string.wellness_condition_fair
        else -> R.string.wellness_condition_poor
    }
)

// 统计：平均小时数仍然是 Double，直接交给 %2$.1f
val averageHours = records.sumOf { it.hours } / records.size
summary.text = getString(R.string.wellness_history_summary, records.size, averageHours)

// 第二行：getString 的格式化参数换成 Double
val hours = getString(R.string.wellness_history_steps, record.hours)
```

第 7 处：**`item_wellness.xml`**：本例不用改。仍然是“第一行日期 + 分类、
第二行数值 + 备注”两行结构，`SimpleAdapter` 的 key 也没变。

第 8 处：**`AndroidManifest.xml`**：本例不用改。没有新增页面，Activity 注册不动。

**连带检查（不属于 8 处，但漏了会崩）**：

- 表结构变了必须把 `DATABASE_VERSION` 从 1 改成 2，或者直接卸载 App / 清除 App 数据；
- 如果保留趋势图（可选 C），`TrendActivity` 里 `it.steps` 要改成 `it.hours`，
  `Entry` 的 y 用 `toFloat()`，统计文案从 `%2$d` 改成 `%2$.1f`；
- 如果保留 AI（可选 B），`PromptBuilder` 和 `ai_record_summary` 里也提到了 steps，
  搜索 `steps` 全部改一遍，否则提示词会把“小时”说成“步数”。

改完按第 3 节的第 7 步自测：空输入、`7`、`7.5`、`25`（应被拦）、同一天存两次、
杀掉重开、转屏。全部通过就说明 8 处都改对了。

---

## 6. 考场时间分配（按 45 分钟算）

| 时间 | 做什么 |
| --- | --- |
| 0–5 分钟 | 读题、填第 1 节那张表；复制/打开已提交的项目 |
| 5–15 分钟 | 数据类 + DbHelper，先只写 `save()` 和 `allRecords()` |
| 15–25 分钟 | 主界面布局 + `MainActivity` 保存逻辑 |
| 25–40 分钟 | 历史页 + 列表 + 空状态 |
| 40–45 分钟 | 编译、按自测清单跑一遍、修小问题 |

原则：**先让“保存 → 历史”跑通，再考虑美化**。一个能跑的朴素界面，
比一个编译不过的漂亮界面分数高得多。

---

## 7. 高频错误与排查

| 现象 | 原因 | 解决 |
| --- | --- | --- |
| 点按钮直接闪退，日志 `NullPointerException` | `findViewById` 的 id 写错，或横屏加载了旧布局 | 对照 XML 检查 id；删掉 `layout-land/` 里的旧同名布局 |
| 跳转历史页闪退，`ActivityNotFoundException` | Manifest 没注册 `HistoryActivity` | 加 `<activity android:name=".HistoryActivity" ... />` |
| 保存后历史是空的 | 存和读用了不同的库名/表名/列名 | 统一常量；检查 `query` 的列顺序 |
| 同一天出现多条（或反过来覆盖了） | 主键策略和 `CONFLICT_*` 不匹配 | 按第 4 节选一种，成套修改 |
| 输入 `8000 ` 被判非法 | 没 `trim()` | `text.toString().trim().toIntOrNull()` |
| 步数/金额输入框能输字母 | `inputType` 没设 | 设 `number` / `numberDecimal`，代码仍要校验 |
| 改完表结构后崩溃 `no such column` | 数据库版本没变，旧表还在 | 版本号 +1，或清除 App 数据 |
| 列表一直空白 | 没设 `adapter`，或数据为空但没显示空状态 | 设置 adapter；空列表时显示提示文字 |
| 中文系统显示英文 | 只改了 `values/strings.xml` | 同步改 `values-zh-rCN/strings.xml` |
| 字符串格式化崩溃 `MissingFormatArgumentException` | `getString` 传的参数个数和占位符不一致 | `%1$s` 对应 1 个参数，`%1$d` 对应整数 |
| 旋转屏幕后输入丢失 | 正常现象（除非用 `onSaveInstanceState`） | 本题不要求；别在 `onStart` 里用旧值覆盖 |
| 数据库里有数据但重启后没了 | 存到了内存/局部变量，没真正写库 | 确认调用的是 `db.save(...)` |
| 趋势页一片空白 | `LineChart` 高度写成了 `wrap_content` | 给固定高度，如 `android:layout_height="280dp"` |
| 有数据但折线不刷新 | 设置 `chart.data` 后没调用 `invalidate()` | 加一行 `chart.invalidate()` |
| Sync 报找不到 `MPAndroidChart` | 没加 JitPack 仓库，或校园网访问不了 JitPack | `settings.gradle` 加 `maven { url 'https://jitpack.io' }`；或改用自定义 View |

---

## 8. 如果题目根本不是“每日记录”

也可能出现“待办清单 / 联系人 / 图书管理”这类纯 CRUD。骨架仍然一样，只换语义：

| 记录类 App | 通用 CRUD |
| --- | --- |
| 日期（唯一键） | `id INTEGER PRIMARY KEY AUTOINCREMENT` |
| 每天一条 | 每条一个 id，可多条 |
| 历史页 | 列表页 |
| 保存 | 新增（`insert`） |
| 修改同一天 | 编辑（`update ... where id = ?`） |
| — | 删除（`delete ... where id = ?`） |

把第 3 节的 `date` 换成 `id`，`save()` 换成 `insert()`，再加一个“点击列表项进入编辑”
就变成待办清单。**结构不变，只换字段和 SQL。**

---

## 9. 考场不要做的事

- 不要改包名、`applicationId`、Gradle/AGP 版本——和题目无关，极易把项目改坏；
- 不要引入 Room、Retrofit、Jetpack Compose 等新东西——多一个依赖就多一个失败点；
- 不要为了“架构好看”拆 Repository / UseCase / DI——考试只看功能；
- 不要大规模删除旧文件——旧代码能编译就留着，删漏引用会浪费大量时间；
- 不要把密钥、网络请求放进新功能——断网或没配 key 时会失败；
- 不要忘记 Manifest 注册、`strings` 双语、横屏布局这三处“隐形坑”。

---

## 10. 最后：一张可打印的检查卡

```
[ ] 数据字段表填好了（字段/类型/控件/校验）
[ ] data class 写好
[ ] DbHelper：建表 SQL、save、allRecords
[ ] 布局：id 齐全、inputType 正确、ScrollView
[ ] MainActivity：findViewById、校验、db.save、跳转
[ ] HistoryActivity：空状态、adapter、日期倒序
[ ] Manifest 注册新 Activity
[ ] strings 默认 + 中文都加了
[ ] 删掉 layout-land 里的旧主布局
[ ] （要图表时）JitPack 仓库 + 依赖 + 固定高度 + chart.invalidate()
[ ] 编译通过
[ ] 空输入 / 正常保存 / 看历史 / 重开 App / 旋转屏幕
```

把这张卡和 `patch/wellness-tracker.patch` 一起带到考场，就够用了。
