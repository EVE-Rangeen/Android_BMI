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

没有做的内容（刻意不做，属于冗余）：BMI 计算、AI 建议、折线图、登录、云同步、
编辑/删除单条记录。旧 BMI 代码仍然留在仓库里，但已经无法从界面进入（见第 8 节）。

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
| `app/src/main/java/com/example/bmi/data/WellnessRecord.kt` | 一条健康记录的数据类：日期、状况、步数、备注 |
| `app/src/main/java/com/example/bmi/data/WellnessDbHelper.kt` | SQLite 建表、保存、读取全部记录 |
| `app/src/main/java/com/example/bmi/HistoryActivity.kt` | 历史记录页：读库、统计、显示列表 |
| `app/src/main/res/layout/activity_history.xml` | 历史页布局（统计 + 空状态 + ListView） |
| `app/src/main/res/layout/item_wellness.xml` | 历史列表每一行的布局（两行文字） |

### 4.2 修改文件

| 文件 | 改了什么 |
| --- | --- |
| `app/src/main/java/com/example/bmi/MainActivity.kt` | 整体换成“选状况 + 填步数/备注 + 保存 + 查看历史”，删掉 BMI 计算、跳转报告页、菜单、对话框、自动填充 |
| `app/src/main/res/layout/activity_main.xml` | 整体换成纵向 `LinearLayout` + `ScrollView`，只保留本题需要的控件 |
| `app/src/main/AndroidManifest.xml` | 注册 `HistoryActivity` |
| `app/src/main/res/values/strings.xml` | 新增 wellness 相关文案和状况数组，`app_name` 改为 Wellness Tracker |
| `app/src/main/res/values-zh-rCN/strings.xml` | 同上，中文文案 |

### 4.3 删除文件

| 文件 | 为什么删 |
| --- | --- |
| `app/src/main/res/layout-land/activity_main.xml` | 这是原 BMI 主界面的**横屏**布局。Android 在横屏时会优先加载 `layout-land/`，如果保留，横屏下 `findViewById(R.id.stepsET)` 会找不到控件并崩溃。删掉后横屏会自动回落到 `layout/activity_main.xml`。 |

### 4.4 没有动的文件

`app/build.gradle`、`settings.gradle`、主题、颜色、图标、旧 BMI 的 Activity 和
data 类全部没动。这带来两个好处：

1. 补丁很小，打上去不会牵连构建配置；
2. 旧代码仍然能编译，不会因为删文件漏改引用而报错。

代价是仓库里留着用不到的 BMI/AI 代码。考试时完全可以保留；如果老师明确要求
“不能有无关代码”，按第 8 节的清单清理，并在清理后完整编译一次。

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
| `TrendActivity` 折线图 | `HistoryActivity` 列表 | 历史记录用 ListView 就够，不必画图 |
| `BmiPreferences` 自动填充 | 无 | 题目没要求“记住上次输入”，删掉 |
| 选项菜单 / 上下文菜单 / 关于对话框 | 无 | 题目没要求，删掉 |

一句话总结：**输入 → 校验 → 写库；另一个页面 → 读库 → 列表。**
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
    date = BmiDates.today(),
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

`BmiDates` 虽然名字里有 BMI，但内容只是 `yyyy-MM-dd` 的日期格式化，属于通用工具。
为了少改文件，这里直接复用了它。如果考试要求代码里不能出现 BMI 字样，把
`data/BmiDates.kt` 重命名为 `data/Dates.kt`，再把引用处的 `BmiDates` 全局替换即可。

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
保存时用它代替 `BmiDates.today()`。这样演示历史记录时不用真的等几天。

### 7.3 “要能删除一条记录”

给 `ListView` 设置 `setOnItemLongClickListener`，拿到位置对应的记录，
执行 `DELETE FROM wellness_records WHERE date = ?`，然后重新查询并
`adapter.notifyDataSetChanged()`（或用新的 adapter 重新赋值）。

### 7.4 “要画趋势图”

原项目的 `BmiTrendChartView` 是专门画 `BmiRecord` 的，不能直接复用。
考试最省事的做法是引入 MPAndroidChart（`app/build.gradle` 里已有注释示例），
用 `LineChart` + `LineDataSet`；或者只显示“最近 7 天步数”的柱状图。
如果题目只说“查看历史”，列表就够，不要主动加图表。

---

## 8. 可选：清理旧 BMI 代码

考试时不建议动这些文件——它们不影响运行，删了反而可能漏改引用。交卷前如果
确实需要一份“干净”的代码，按下面的顺序删，并且每删一组就编译一次。

1. 删页面：`ReportActivity.kt`、`TrendActivity.kt`；
2. 删 UI：`ui/report/`、`ui/trend/` 整个目录；
3. 删数据：`data/BmiDbHelper.kt`、`BmiHistoryRepository.kt`、`BmiPreferences.kt`、
   `BmiProfile.kt`、`BmiRecord.kt`、`Gender.kt`、`DeepSeekClient.kt`、
   `HealthAdviceRepository.kt`、`PromptBuilder.kt`；
4. `data/BmiDates.kt` **先别删**：`MainActivity` 还在用它。可以重命名为
   `Dates.kt` 并全局替换，或者把 `today()` 直接写进 `MainActivity`；
5. 删布局：`activity_report.xml`、`activity_trend.xml` 以及 `layout-land/` 下对应文件；
6. 删菜单：`res/menu/menu_options.xml`、`menu_context.xml`；
7. 删图片：`drawable/bot_thin.png`、`bot_fit.png`、`bot_fat.png`；
8. 删测试：`app/src/test/java/com/example/bmi/PromptBuilderTest.kt`；
9. 清 `AndroidManifest.xml` 里 `ReportActivity`、`TrendActivity` 的注册；
10. 清 `strings.xml` 里不再使用的 BMI / AI 文案（可选，留着不会报错）。

最后 **Build → Clean Project → Rebuild Project**，确认没有 “Unresolved reference”。

---

## 9. 自测清单

- [ ] 什么都不填，直接点保存：出现“请输入有效的步数”，不崩溃、不写库。
- [ ] 输入 `abc`：数字键盘通常打不出来；如果能输入，仍然被校验拦住。
- [ ] 输入 `0`：允许保存（0 步是合法值）。
- [ ] 保存后打开历史：能看到刚保存的日期、状况、步数、备注。
- [ ] 同一天保存两次：历史里只有一条，显示第二次的内容。
- [ ] 历史为空时：显示“暂无记录”，不显示空白列表。
- [ ] 旋转屏幕：不崩溃（因为已删除旧的横屏主布局）。
- [ ] 切换系统语言为中文：界面和历史记录都变成中文。
- [ ] 从历史页按返回键：回到主界面，输入内容还在。
- [ ] 杀掉 App 再打开：历史记录仍然存在（数据真的写进了 SQLite）。

---

## 10. 相关文档

- `docs/exam-playbook.md`：把这道题的做法迁移到其他“每日记录类”题目的通用教程。
- `patch/wellness-tracker.patch`：可直接应用到 `d43a3c9` 的补丁。
