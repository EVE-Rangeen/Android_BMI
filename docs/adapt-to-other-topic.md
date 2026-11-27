# 换题目时改哪里（改题点地图）

这套代码是按“每天记一条健康日志”写的，但**骨架是通用的**：
`输入 → 校验 → 写 SQLite → 另一个页面读出来展示`。
考试换了题目（喝水记录、心情日记、学习打卡、记账……），改的都是同一批位置。

代码里的关键位置都标了 `【改题点 C#】`，本文就是这张编号表的使用说明。
在 Android Studio 里全局搜索 `改题点`，能直接跳到所有该动手的地方。

---

## 1. 先认清三层结构

```
① 数据层（存什么）
   WellnessRecord      —— 一条记录有哪些字段、什么类型
   WellnessDbHelper    —— 表名/列名/建表 SQL/写入/查询
   Dates               —— 日期格式

② 界面层（怎么填、怎么看）
   activity_main.xml + MainActivity      —— 输入与保存
   activity_history.xml + item_wellness.xml + HistoryActivity  —— 历史列表

③ 文案与可选功能（怎么说、要不要联网）
   strings.xml（中英）、menu_main.xml
   PromptBuilder / AdviceActivity        —— 只有做 AI 时才相关
```

**改题顺序永远是 ①→②→③**。反过来做会一路编译错误，很难定位。

---

## 2. 改题点总表

| 编号 | 改什么 | 在哪个文件 | 关键位置 | 连带必须改 |
| --- | --- | --- | --- | --- |
| **C0** | 工程身份（包名、主题名、App 名） | `app/build.gradle`、`AndroidManifest.xml`、`themes.xml` | `namespace`/`applicationId`/`Theme.Wellness`/`app_name` | 所有 `.kt` 的 `package`；`R.style` 引用 |
| **C1** | 一条记录有哪些字段 | `data/WellnessRecord.kt` | 4 个构造参数 | C2、C3、C4、C5、C6、C7、C9、C10 |
| **C2** | 表名、列名、类型、建表 SQL | `data/WellnessDbHelper.kt` | `onCreate()` 的 CREATE TABLE、`save()`、`allRecords()` | C1（字段要对得上）、`DATABASE_VERSION` |
| **C3** | 界面文字、下拉选项、列表文案 | `res/values/strings.xml`（英文）+ `values-zh-rCN/strings.xml`（中文） | `wellness_*`、`wellness_condition` 数组 | C4（`@string/` 引用）、C5（`R.string.` 引用） |
| **C4** | 主界面有哪些控件、id 叫什么 | `res/layout/activity_main.xml` | `stepsET`、`conditionSpinner`、标签 | C5（`findViewById` 必须同名） |
| **C5** | 输入校验、读取输入 | `MainActivity.saveTodayRecord()` | `toIntOrNull()`、`selectedItemPosition` | C4、C6 |
| **C6** | 组装记录、决定日期 | `MainActivity.saveTodayRecord()` | `WellnessRecord(...)` | C1、C2 |
| **C7** | 历史列表显示什么、怎么统计 | `HistoryActivity.kt` | `summary`、`rows`、`conditionText()`、`buildSubtitle()` | C3、C8、C1 |
| **C8** | 列表一行的布局 | `res/layout/item_wellness.xml` | `itemTitle`、`itemSubtitle` | C7（`SimpleAdapter` 的 id 数组） |
| **C9** | 发给 AI 的提示词 | `data/PromptBuilder.kt` | `systemPrompt()`、`userPrompt()`、`conditionLabel()` | C1、`PromptBuilderTest` |
| **C10** | AI 页顶部摘要 | `AdviceActivity.kt` | `ai_record_summary` 的三个参数、`conditionText()` | C3、C1 |
| **C11** | 有没有菜单、菜单里有什么 | `res/menu/menu_main.xml` + `MainActivity` | `menu_language`、`menu_ai_advice` | 删功能时见 `docs/exam-guide.md` |

---

## 3. 依赖链：改了 A 就要改 B

```
C1 字段  ──► C2 数据库列 ──► C3 文案 ──► C4 布局控件 ──► C5 读取/校验
   │                                                        │
   │                                                        ▼
   └──────────────► C6 组装记录 ──► C7 历史显示 ──► C8 行布局
                                        │
                                        └──► C9 提示词 ──► C10 AI 摘要
```

三条实用规则：

1. **改字段名/类型，从 C1 出发顺着箭头走到底**，一个都别跳。
2. **只改文案（C3）最安全**，不会影响逻辑，改完直接 Run。
3. **改完用“全局搜索”兜底**：搜旧的字段名、旧的中文词、旧的 id，
   确认每一处都改完。Android Studio 的 `Ctrl+Shift+F` 比肉眼看靠谱。

---

## 4. 先判断：这道题要不要加字段？

拿到题目，用三个问题决定改动量：

| 问题 | 是 | 否 |
| --- | --- | --- |
| 还是“每天一条记录”吗？ | 保留 `date` 主键（同一天覆盖） | 改自增主键，见 `docs/wellness-tracker.md` 7.1 |
| 记录的**字段数量**和现在一样吗？ | 做“同构替换”（示例一），10 分钟 | 做“加/删字段”（示例二），25 分钟 |
| 数值还是整数吗？ | 不用动类型 | 换 `Double`（示例三），注意连带 6 处 |

---

## 5. 示例一：步数 → 每日喝水量（同构替换，最快）

题目变成：“每天记录饮水量（毫升）和是否达标”。

### 5.1 改动清单（共 9 处）

| 编号 | 文件 | 改法 |
| --- | --- | --- |
| C1 | `WellnessRecord.kt` | `val steps: Int` → `val waterMl: Int` |
| C2 | `WellnessDbHelper.kt` | 列常量 `COL_STEPS = "steps"` → `COL_WATER = "water_ml"`；建表 SQL 里 `steps INTEGER` → `water_ml INTEGER`；`save()` 里 `put(COL_WATER, record.waterMl)`；`allRecords()`/`findByDate()` 里 `c.getInt(2)` 不变（仍是第 3 列） |
| C2 | `WellnessDbHelper.kt` | `DATABASE_VERSION` 从 1 改成 2（表结构变了） |
| C3 | 两个 `strings.xml` | `wellness_steps_label` = “今日饮水量 / Water today”；`wellness_steps_hint` = “例如 1200”；`wellness_history_steps` = `%1$d ml` |
| C4 | `activity_main.xml` | `stepsET` 可以不改 id（省事），只改 `android:hint`；`maxLength` 从 7 调到 5 |
| C5 | `MainActivity.saveTodayRecord()` | 校验改成 `if (waterMl == null \|\| waterMl !in 0..20000)` |
| C6 | `MainActivity.saveTodayRecord()` | `steps = steps` → `waterMl = waterMl`（字段名替换） |
| C7 | `HistoryActivity.buildSubtitle()` | `getString(R.string.wellness_history_steps, record.waterMl)` |
| C9 | `PromptBuilder.userPrompt()` | `append("饮水量：")` / `append("Water: ")`，后面拼 `" ml"` |
| C10 | `AdviceActivity` | `ai_record_summary` 的第 3 个参数改成 `record.waterMl`，字符串里的 “steps” 改 “ml” |

### 5.2 关键代码长什么样

```kotlin
// C1
data class WellnessRecord(
    val date: String,
    val condition: Int,
    val waterMl: Int,      // ← 原来叫 steps
    val note: String
)

// C5 + C6
val waterMl = stepsET.text.toString().trim().toIntOrNull()
if (waterMl == null || waterMl !in 0..20_000) {
    Toast.makeText(this, R.string.wellness_steps_warning, Toast.LENGTH_SHORT).show()
    return
}
val record = WellnessRecord(
    date = Dates.today(),
    condition = conditionSpinner.selectedItemPosition,
    waterMl = waterMl,
    note = noteET.text.toString().trim()
)
```

### 5.3 验收

- [ ] 输入 1200 保存 → 历史显示 “1200 ml”；
- [ ] 输入 30000 → 提示非法；
- [ ] 中文/英文界面文案都换成了“饮水量/Water”。

---

## 6. 示例二：加一个字段（心情 + 学习时长）

题目变成：“每天记录学习时长（分钟）、心情（1–5 星）、备注”。

### 6.1 七步，一步都不能少

**第 1 步（C1）数据类加字段**

```kotlin
data class WellnessRecord(
    val date: String,
    val studyMinutes: Int,   // 学习时长
    val mood: Int,           // 1–5
    val note: String
)
```

**第 2 步（C2）建表加列 + 版本号 +1**

```sql
CREATE TABLE wellness_records (
    date TEXT PRIMARY KEY NOT NULL,
    study_minutes INTEGER NOT NULL,
    mood INTEGER NOT NULL,
    note TEXT NOT NULL,
    updated_at INTEGER NOT NULL
)
```

```kotlin
const val DATABASE_VERSION = 2      // 改了表结构必须 +1
```

> 不 +1 的后果：旧手机上的数据库不会重建，查询新列时抛
> `no such column: study_minutes`。开发期最省事的做法是卸载 App 或
> 设置里“清除数据”，但代码里该 +1 还是要 +1。

**第 3 步（C2）`save()` 与查询**

```kotlin
values.put(COL_MINUTES, record.studyMinutes)
values.put(COL_MOOD, record.mood)

// 查询时列的顺序要和读取时的序号一致
arrayOf(COL_DATE, COL_MINUTES, COL_MOOD, COL_NOTE)
// → c.getString(0), c.getInt(1), c.getInt(2), c.getString(3)
```

**第 4 步（C3）文案**

```xml
<string name="study_label">学习时长（分钟）</string>
<string name="mood_label">今天心情</string>
<string-array name="mood_options">
    <item>1 星</item><item>2 星</item><item>3 星</item><item>4 星</item><item>5 星</item>
</string-array>
```

**第 5 步（C4/C5）布局与读取**

```xml
<EditText
    android:id="@+id/studyET"
    android:inputType="number"
    android:maxLength="4" />

<Spinner
    android:id="@+id/moodSpinner"
    android:entries="@array/mood_options" />
```

```kotlin
val studyMinutes = studyET.text.toString().trim().toIntOrNull()
if (studyMinutes == null || studyMinutes !in 0..1440) { /* Toast + return */ }
val mood = moodSpinner.selectedItemPosition + 1   // 0–4 → 1–5
```

**第 6 步（C7/C8）历史行**：`SimpleAdapter` 仍然两个 key，把第二行改成
“`60 分钟 · 3 星 · 备注`”即可，不需要动 `item_wellness.xml`。

**第 7 步（C9/C10）提示词与摘要**：`userPrompt()` 里加两行参数；
`ai_record_summary` 的占位符从 3 个改成 4 个（`%1$s %2$s %3$d %4$d`）。

### 6.2 加字段时最容易漏的地方

| 漏了哪里 | 现象 |
| --- | --- |
| `DATABASE_VERSION` 没 +1 | 运行时报 `no such column` |
| `save()` 忘 `put` 新列 | 新列永远存 0，或 NOT NULL 约束直接崩 |
| `allRecords()` 的列顺序与 `getXxx(序号)` 不一致 | 数据串位（步数显示成心情） |
| `SimpleAdapter` 的 `intArrayOf(...)` 与行布局 id 不符 | 列表空白或崩溃 |
| 提示词没加新字段 | AI 建议与输入无关（最难发现） |

---

## 7. 示例三：整数 → 小数（记账：金额）

题目变成：“每天记录一笔支出金额（元）和分类”。

### 7.1 连带 6 处

| 位置 | 改法 |
| --- | --- |
| C1 数据类 | `val amount: Double` |
| C2 建表 | `amount REAL NOT NULL`（SQLite 用 REAL 存小数） |
| C2 读取 | `c.getDouble(1)`（原来是 `getInt`） |
| C2 写入 | `put(COL_AMOUNT, record.amount)`（`ContentValues` 支持 Double） |
| C5 校验 | `toDoubleOrNull()` + `if (amount == null \|\| amount <= 0.0)` |
| C4 布局 | `android:inputType="numberDecimal"`（否则小数点打不出来） |

### 7.2 显示要格式化

```xml
<!-- strings.xml：注意 %1$.2f，保留两位小数 -->
<string name="wellness_history_amount">%1$.2f 元 / ¥%1$.2f</string>
```

```kotlin
val text = getString(R.string.wellness_history_amount, record.amount)
```

不要直接 `"$amount 元"`：`0.1 + 0.2` 之类的浮点误差会显示成 `12.300000000000001`。

### 7.3 日期与金额同时改时

如果题目要求“一天可以记多笔”，`date` 就不能做主键了：
改成自增 `id`，并且把 `save()` 的 `CONFLICT_REPLACE` 换成普通 `insert`。
这一步的完整说明在 `docs/wellness-tracker.md` 7.1。

---

## 8. 要不要改名（C0）

**结论：除非题目明确要求，否则不要改。**

| 要改的 | 成本 | 建议 |
| --- | --- | --- |
| 类名 `WellnessRecord` → `WaterRecord` | 中：Refactor → Rename（勾选搜索注释/字符串） | 可不改，功能不受影响 |
| 包名 `com.example.bmi` | 高：namespace + applicationId + 所有 package + 目录 | 不改，题目不检查 |
| 主题名 `Theme.Wellness` | 低：themes.xml ×2 + Manifest | 可不改 |
| `app_name`（桌面显示名） | 极低：两个 `strings.xml` | **建议改**，这是老师唯一会看到的“题目对不对” |

改类名一定用 IDE 的重命名（`Shift+F6`），它会连 `import` 和引用一起改；
手动改文件名会留下 `Unresolved reference`。

---

## 9. 改完的自检清单

**功能**

- [ ] 空输入 → 提示，不崩溃、不写库；
- [ ] 边界值 → 合法（例如 0、上限值），超界 → 提示；
- [ ] 保存 → Toast；同一天再保存 → 覆盖，不新增；
- [ ] 历史 → 数字/单位/备注都对，顺序是最新在上；
- [ ] 空历史 → 显示空状态文字。

**一致性**

- [ ] 全局搜索旧字段名、旧中文词：没有残留；
- [ ] 两个 `strings.xml` 的 key 完全一致（漏一个 → 中文界面显示英文或直接崩）；
- [ ] 改了 `DATABASE_VERSION`（如果表结构变了）；
- [ ] 布局 id 与 `findViewById` 完全一致；
- [ ] 中英文都切换看一遍（如果保留了语言切换）。

**AI（如果保留）**

- [ ] 提示词里带上了新字段；
- [ ] `PromptBuilderTest` 里的断言跟着改，`testDebugUnitTest` 通过；
- [ ] 摘要文案的占位符数量与 `getString(...)` 的参数数量一致。

**编译**

```bat
gradlew.bat assembleDebug
gradlew.bat lintDebug
gradlew.bat testDebugUnitTest
```

---

## 10. 一句话总结

> **字段决定一切**：先定下“一条记录有几个字段、各是什么类型”，
> 然后按 C1 → C2 → C3 → C4 → C5 → C6 → C7 的顺序把这条链子走完，
> 最后才碰 AI（C9/C10）。每一步都 Run 一次，比最后一起调试快得多。
