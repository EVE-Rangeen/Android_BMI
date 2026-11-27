# 从零创建一个 Wellness Tracker（考试模拟手册）

这份文档教你在一个**全新的空工程**里，按顺序把功能一块块加出来，最终到达
`wellness_tracker` 分支现在的状态。用法是：左边开着 GitHub 上这个分支的代码，
右边新建工程，按本文的顺序复制、修改、验证。

配套文档：

- `docs/adapt-to-other-topic.md` — 题目换了名字/字段时，改代码的哪一处、连带改哪里。
- `docs/exam-guide.md` — 考场不需要某个功能时，删哪些文件。
- `docs/wellness-tracker.md` — 每个文件的作用和 SQLite 设计（含两种画趋势图的写法，7.4 节）。

> 本文只讲“基础功能 + 语言切换 + AI”的搭建顺序。第三块可选功能
> “最近 7 天趋势折线图”依赖 MPAndroidChart，不是每题都需要，所以没有放进
> 从零步骤；需要时按 `docs/wellness-tracker.md` 7.4 节加，或直接复制本分支的
> `TrendActivity.kt` / `activity_trend.xml`。

**核心原则：一次只加一层；编译点按层给，不是每节都给。**
数据层（第 3 节）可以单独编译；从第 4 节起，`MainActivity`、`HistoryActivity`、`BaseActivity`、
`AdviceActivity` 这四个文件互相引用，缺任何一个都会报“找不到”，所以**第 4–7 节一路复制、
中途不编译也不运行，第一次编译放在第 7 节末尾（7.6）**。这也正好对上考试现场：题目要不要
语言切换和 AI，决定第 6、7 节是留下还是按 `docs/exam-guide.md` 删掉，删完再编译，一次成型。

---

## 0. 开始前的准备



### 0.1 包名（`applicationId` / `namespace` / `package`）

本仓库用的是 `com.example.bmi`（沿用作业，题目不检查包名）。

- **建议：新工程直接起名** `BMI`**、包名** `com.example.bmi`**。**
这样所有文件的 `package com.example.bmi` 和 `R` 引用都能原样粘贴，零改动。
- 如果起了别的包名（例如 `com.example.wellness`），粘贴代码前必须先改每个
Kotlin 文件第一行的 `package`，并且 Android Studio 可能提示 `R` 找不到。
改包名的正确姿势：右键包名 → **Refactor → Rename**，勾选
`Search in comments and strings`，而不是手动一个个改。



### 0.2 构建脚本语言：先看你的向导生成了什么

不同版本的 Android Studio 新建工程时，生成的构建文件不一样：


| 向导生成                                                                     | 说明                       | 处理方式                                                                                  |
| ------------------------------------------------------------------------ | ------------------------ | ------------------------------------------------------------------------------------- |
| `build.gradle` + `settings.gradle`                                       | Groovy DSL，本仓库就是这种       | 直接粘贴仓库的构建文件（见 1.2）                                                                    |
| `build.gradle.kts` + `settings.gradle.kts` + `gradle/libs.versions.toml` | Kotlin DSL + 版本目录，较新版本默认 | 二选一：**推荐**删掉 `.kts` 和 `libs.versions.toml`，改用仓库的 Groovy 版本；或者保留 `.kts`，按下面 0.4 的对照表改写 |


先打开工程根目录看一眼，确定属于哪种，再往下做。

> 判断方法：Android Studio 左侧 Project 视图切到 **Project** 模式，
> 根目录有没有 `build.gradle.kts`。



### 0.3 从 GitHub 网页复制代码的正确姿势

考试时你只能看仓库（多半是网页版），所以复制这一步也有讲究：

1. **点文件名 → 点右上角** `Raw`，在纯文本页面 `Ctrl+A`、`Ctrl+C`。
  直接在代码高亮页面上拖选，容易把行号、折叠箭头一起复制进去。
2. 复制完先看**第一行**：`.kt` 文件必须从 `package com.example.bmi` 开始；
  `.xml` 文件必须从 `<?xml version="1.0" encoding="utf-8"?>` 开始。
   如果第一行是别的（比如网页标题），说明你复制多了。
3. **一次只复制一个文件**，粘完先核对第一行。
  第 4 节之前（数据层）每粘一个就编译一次，出错好定位；第 4 节起界面层的文件互相引用，
  中途不编译，统一到 7.6（见本文开头的“核心原则”）。
4. 不要下载 ZIP 后整个覆盖到工程里：ZIP 里没有 `.idea` 和 `local.properties`，
  但会用仓库的 `settings.gradle`、主题名等覆盖你向导生成的配置，
   容易出现“明明代码一样却跑不起来”。
5. 中文注释在网页上复制一般是正常的；如果粘进去变乱码，先把
  **File → Settings → Editor → File Encodings** 全部设为 UTF-8，再重新粘贴。



### 0.4 如果坚持用 Kotlin DSL：对照表

本仓库所有片段都是 Groovy。若你的工程是 `.kts`，按下表改：


| Groovy（本仓库）                                                 | Kotlin DSL（`.kts`）                                           |
| ----------------------------------------------------------- | ------------------------------------------------------------ |
| `id 'com.android.application' version '8.13.0' apply false` | `id("com.android.application") version "8.13.0" apply false` |
| `compileSdk = 36`                                           | 同左（本来就是赋值语法）                                                 |
| `applicationId = "com.example.bmi"`                         | 同左                                                           |
| `buildConfigField "String", "X", "\"v\""`                   | `buildConfigField("String", "X", "\"v\"")`                   |
| `buildFeatures { buildConfig = true }`                      | 同左                                                           |
| `implementation 'androidx.core:core-ktx:1.13.1'`            | `implementation("androidx.core:core-ktx:1.13.1")`            |
| `sourceCompatibility = JavaVersion.VERSION_17`              | 同左                                                           |


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
  旧版 AS（2023.2.1 Iguana 之前）没有这个选项，那时的 `Empty Activity` 就是 XML 版，选它即可；
   判断标准：建完后有 `res/layout/activity_main.xml`、`MainActivity` 继承 `AppCompatActivity` 就对了
3. 参数：


| 项                            | 建议值                 | 说明                             |
| ---------------------------- | ------------------- | ------------------------------ |
| Name                         | `BMI`               | 工程名，随意，不影响代码                   |
| Package name                 | `com.example.bmi`   | **建议照抄**，省去改包名                 |
| Language                     | Kotlin              | 本仓库是 Kotlin                    |
| Minimum SDK                  | API 21              | 仓库 `minSdk = 21`，选 21 或 24 都能跑 |
| Build configuration language | 能选 Groovy 就选 Groovy | 不能选就按 0.2/0.4 处理               |


1. 等 Sync 完成，点 **Run**，确认能看到模板自带的 “Hello World!”。
  **这一步必须先通过**，否则后面出问题分不清是谁的锅。



### 1.2 对齐四个构建文件

要让仓库的代码能直接粘贴，下面四个文件必须一起对齐（缺一个就会版本错配）：


| 文件                                         | 作用                    | 关键内容                                               |
| ------------------------------------------ | --------------------- | -------------------------------------------------- |
| `settings.gradle`                          | 仓库地址、模块列表、项目名         | `include ':app'`                                   |
| `build.gradle`（根目录）                        | 插件版本                  | AGP `8.13.0`、Kotlin `2.2.20`                       |
| `gradle/wrapper/gradle-wrapper.properties` | Gradle 版本             | `gradle-8.13-bin.zip`                              |
| `app/build.gradle`（模块级）                    | SDK 版本、依赖、BuildConfig | `compileSdk 36`、`targetSdk 34`、`minSdk 21`、Java 17 |


并且删掉向导生成的 `*.kts` 和 `gradle/libs.versions.toml`

从github这个repo的Welfare分支来复制上传到assignment的app/build.gradle 不一样 这个文件必须从github进行复制

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


| 仓库文件                                                                        | 你工程里的位置                  | 内容                  |
| --------------------------------------------------------------------------- | ------------------------ | ------------------- |
| `app/src/main/res/values/strings.xml`                                       | 覆盖同名文件                   | 英文文案 + 状况数组         |
| `app/src/main/res/values-zh-rCN/strings.xml`                                | 新建目录 `values-zh-rCN`，放进去 | 中文文案，key 必须与英文完全一致  |
| `app/src/main/res/values/colors.xml`、`themes.xml`、`values-night/themes.xml` | 覆盖同名文件                   | 主题 `Theme.Wellness` |


`values-zh-rCN` 目录要手动建。注意要建的是**文件**而不是空目录：右键 `res` →
**New → Android Resource File**，File name 填 `strings`，Resource type 选 `values`，
Source set 选 `main`，然后在 Available qualifiers 里点 **Locale** → 语言 `zh`、Region `CN`，
确定，AS 会自动生成 `values-zh-rCN/strings.xml`。

> 如果之前点过 “New → Android Resource Directory”，那只会建出一个**空的** `values-zh-rCN`
> 目录（空目录在 Android 视图里不显示，文件资源管理器里才看得到，属正常现象）。
> 补救：右键这个空目录 → **New → File**，命名 `strings.xml` 再粘贴内容，效果一样。



### 2.2 主题必须一起换（容易被忽略）

新建向导可能给你 `Theme.Material3.DayNight.NoActionBar` 这类 Material3 主题。
本仓库的布局里用到了 `Widget.MaterialComponents.Button.OutlinedButton` 这个样式，
主题文件里还引用了 `?attr/colorPrimaryVariant`（Material3 没有这个属性）。
所以：

- **直接用仓库的** `values/themes.xml` **+** `values-night/themes.xml` **+** `colors.xml` **覆盖**；
- 打开 `AndroidManifest.xml`，把 `android:theme="@style/..."` 改成
`@style/Theme.Wellness`（仓库里已经是这个名字）。
- 通用规则：`@style/` 后面到底填什么
`@style/名字` 引用的就是 `res/values/themes.xml` 里 `<style name="名字">` 定义的那个名字。所以考试时判断方法只有一步：
  1. 打开 `res/values/themes.xml`，找到 `<style name="...">`；
  2. 把引号里的名字原样填到 `android:theme="@style/...">` 里。



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
  1. 右键刚建好的 `data` → New → Kotlin Class/File；
  2. 在弹窗里：Name 填 `WellnessRecord`，Kind 选 File（不是 Class！选 Class 会自动生成 `class WellnessRecord {}`，还要删）；
  3. 可以在左上角把Android视图切换为project视图 从而避免创建到generated的java目录下



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

> 这里是**界面层之前最后一个编译点**：工程还是向导生成的 Hello World，数据层只是新增了
> 三个文件、还没人调用。第 4 节把 `MainActivity` 换成正式版以后，红标会一路持续到第 7 节末，
> 那是预期状态，不是操作失误。

---



## 4. 主界面：输入 + 保存



### 4.1 布局

覆盖 `app/src/main/res/layout/activity_main.xml`（向导生成的 ConstraintLayout 版本整份替换）。
控件和 id 必须与代码一致：


| id                 | 控件                             | 作用                                            |
| ------------------ | ------------------------------ | --------------------------------------------- |
| `dateTV`           | TextView                       | 显示今天日期                                        |
| `conditionSpinner` | Spinner                        | 身体状况，`entries` 指向 `@array/wellness_condition` |
| `stepsET`          | EditText（`inputType="number"`） | 步数                                            |
| `noteET`           | EditText（多行）                   | 备注                                            |
| `saveBtn`          | Button                         | 保存                                            |
| `historyBtn`       | Button（描边样式）                   | 查看历史                                          |




### 4.2 代码

复制 `MainActivity.kt`。注意它是仓库的**最终版**（含可选 A/B 的代码），结构是：

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

> **⚠️ 本步不要编译。** 仓库里的 `MainActivity.kt` 是最终版：原样复制后点 Build 会报一屏错，
> 这是**预期状态**——它提前引用了第 5、6、7 节才创建的文件。**第一次编译放在 7.6。**
>
> 报错很长，但全部能对上号（这张表也用来确认“是缺文件，不是复制坏了”）：
>
>
> | 报错里的符号                                                         | 缺的东西                                    | 哪一节才补     |
> | -------------------------------------------------------------- | --------------------------------------- | --------- |
> | `BaseActivity`、`toggleLanguage`、`'onCreate' overrides nothing` | `BaseActivity.kt`                       | 6.1 / 6.2 |
> | `HistoryActivity`                                              | `HistoryActivity.kt`                    | 5.1       |
> | `R.menu.menu_main`、`menu_language`、`menu_ai_advice`            | `res/menu/menu_main.xml`                | 6.3       |
> | `AdviceActivity`                                               | `AdviceActivity.kt`                     | 7.5       |
> | `title`、`findViewById`、`getString` 等一片红                        | 上面 `BaseActivity` 没解析出来导致的连锁错误，不是真的少了东西 | —         |
>
>
> **做法：原样复制，不做减法，第 7 节末尾一次编译成型。**
>
> 1. 第 4 节照抄 `MainActivity.kt`，**报错先不管、不要点 Run、不要删任何一行**；
> 2. 继续第 5 节（`HistoryActivity`）、第 6 节（`BaseActivity` + 菜单）、第 7 节（AI 页）；
> 3. 到 7.6 再点 Build / Run，这一屏红会自己消失，一次编译通过。
>
> 代价是第 4–6 节中途不能运行、看不到界面，这是正常状态。好处是每份文件都和仓库一致，
> 最后对 `git diff` 时不用回忆自己删过什么。
>
> **例外：如果你决定不要第 6、7 节的功能**（题目不要求语言切换 / AI），这条路走不通：
> 照 `docs/exam-guide.md` 的删除清单把相关代码删掉，删完同样在 7.6 做首次编译。
> 那是“按题目裁功能”，和这里的临时减法不是一回事。



### 4.3 本步不验收

这时工程里的 `MainActivity` 引用着还不存在的 `HistoryActivity`、`BaseActivity`、
`AdviceActivity`，Android Studio 会整片标红。这是**预期状态**，不是复制坏了：

- 不要点 Run，也不要按 `Alt+Enter` 让 IDE “自动修复”，更不要为了消红去删行；
- 主界面的运行验收（日期显示、保存提示、Toast 文案）统一放在 **7.6**，那里是第一次编译点。

本节只核对一件事：`activity_main.xml` 和 `MainActivity.kt` 都已经完整粘进来，第一行分别是
`<?xml version="1.0" encoding="utf-8"?>` 和 `package com.example.bmi`。

---



## 5. 历史页：读库 + 列表



### 5.1 三个文件


| 仓库文件                              | 作用                                     |
| --------------------------------- | -------------------------------------- |
| `res/layout/activity_history.xml` | 页面骨架：统计文字、空状态文字、ListView               |
| `res/layout/item_wellness.xml`    | 列表“一行”的布局：`itemTitle` + `itemSubtitle` |
| `HistoryActivity.kt`              | 查库、算平均、组装 Map、`SimpleAdapter`          |


`activity_history.xml` 的 id：`historySummary`、`historyEmpty`、`historyList`。

刚开始创建activity history.xml有报错是因为还没有创建HistoryActivity.kt

HistoryActivity.kt报错是因为没有创建BaseActivity，因为这个文件继承自这个BaseActivity

> 创建方式：右键 `res/layout` → New → Layout Resource File

> 本节仍然不编译（`BaseActivity`、`AdviceActivity` 还没创建，红标照旧）。要收的尾只有一个：
> **在 Manifest 里注册** `HistoryActivity`（见 5.2）。漏了这一步，7.6 之后点“查看历史”会直接崩
> （`ActivityNotFoundException`），而编译阶段不会给你任何提示。



### 5.2 在 Manifest 里注册

```xml
<activity
    android:name=".HistoryActivity"
    android:exported="false"
    android:label="@string/wellness_history_title" />
```

`exported="false"` 的含义：只允许本 App 内部跳转，不允许别的 App 直接启动它。

### 5.3 本步不验收

- [ ] 三个文件到位：`activity_history.xml`、`item_wellness.xml`、`HistoryActivity.kt`；
- [ ] `HistoryActivity` 已在 Manifest 里注册（见 5.2）；
- [ ] 历史页要用的文案 key 已在 `values/strings.xml` 里（`wellness_history_title`、
  `wellness_history_empty`、`wellness_history_summary` 等），第 2 节整份覆盖时就已经带上了。

> 到这里，**题目要求的基础功能代码已经写完**，运行验收统一在 7.6。
> 第 6、7 节是可选的加分项；不要就按 `docs/exam-guide.md` 删掉，删完同样在 7.6 编译。

---



## 6. 可选 A：App 内中英文切换

顺序很重要：**先有资源（第 2 步已做）→ 再有语言偏好 → 最后才是菜单入口。**

### 6.1 三个文件


| 文件                      | 作用                                                                |
| ----------------------- | ----------------------------------------------------------------- |
| `data/AppLanguage.kt`   | `ENGLISH("en")` / `CHINESE("zh")` + `fromTag()`                   |
| `data/LanguageStore.kt` | SharedPreferences 读写语言选择                                          |
| `BaseActivity.kt`       | `attachBaseContext` 换 Locale；`switchLanguage()` 保存 + `recreate()` |




### 6.2 核对父类：三个 Activity 都继承 BaseActivity

> **走“整份复制最终版”这条路时，这一步没有东西要改。** `MainActivity.kt` 和
> `HistoryActivity.kt` 里本来写的就是 `class ... : BaseActivity()`，第 7 节的
> `AdviceActivity.kt` 创建时也是这个父类。
>
> 只有两种情况才需要真的去改这一行：
>
> - 你按 `docs/exam-guide.md` 删过语言切换（那时父类是 `AppCompatActivity`），现在又想加回来；
> - 你之前自己写过基础版的 `MainActivity`（父类是 `AppCompatActivity`）。
>
> 本节仍不编译：`AdviceActivity` 到第 7 节才有，`MainActivity` 的红标会一直留到 7.6。

```kotlin
class MainActivity : BaseActivity() {
class HistoryActivity : BaseActivity() {
class AdviceActivity : BaseActivity() {
```

> 如果你同时从最终版复制了 `TrendActivity.kt`（趋势图，可选 C），它的父类也要写成
> `BaseActivity`，否则图表页的标题不会跟着 App 内语言变。

同时删掉 `import androidx.appcompat.app.AppCompatActivity`（`BaseActivity` 在同一个包，
不需要 import）。

**“改成继承 BaseActivity”到底改的是什么：** 只改类声明那一行的冒号后面，方法体一行都不用动。
`BaseActivity` 自己就是 `open class BaseActivity : AppCompatActivity()`，所以换掉父类之后
`setContentView()`、`findViewById()`、`title`、`getString()` 全都照旧可用，只是多了一层
“每次创建页面前先把 Context 换成 App 内选择的语言”。这也是为什么第 5 节那 17 条红字，
会在 `BaseActivity.kt` 出现之后自己消失。

### 6.3 菜单

本节要新建的只有一个文件：`res/menu/menu_main.xml`（你的工程里还没有 `menu` 目录）。

1. 右键 `app/src/main/res` → **New → Android Resource File**，File name 填 `menu_main`，
  Resource type 选 `Menu`。对话框里**没有 Source set 输入框是正常的**：在 `main` 的 `res`
  上右键，源集就默认是 `main`；看 **Directory name** 显示 `menu`、**Root element** 显示
  `menu` 即可。**Available qualifiers 留空**（选了任何限定符都会生成 `menu-xxx` 目录）。
  另一种等价做法：先 New → Android Resource Directory 建 `menu` 目录，再在里面建 `menu_main.xml`；
2. 把仓库的 `res/menu/menu_main.xml` 整份覆盖进去；
3. `MainActivity` 里的两个菜单方法**已经随整份复制带过来了**，不用手敲。

下面这段只是给你对照用的，确认文件末尾有这两个方法、id 名字对得上：

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

注意 `R.id.menu_language` / `R.id.menu_ai_advice` 里的 `R.id`：这两个 id 由
`menu_main.xml` 里 `android:id="@+id/menu_language"` 生成，和 `strings.xml` 里的
`R.string.menu_language` 是两个不同的东西，别混。

### 6.4 核对标题行

`MainActivity`、`HistoryActivity`（以及第 7 节的 `AdviceActivity`）的 `onCreate` 里
都已经有这一行，复制时带过来了，不需要补：

```kotlin
title = getString(R.string.app_name)     // 历史页用 wellness_history_title，AI 页用 ai_title
```

原因：标题栏默认取 Manifest 的 `android:label`，那是“系统”按手机语言解析的，
App 内切换语言后不会自动跟着变。

### 6.5 本步不验收

- [ ] 三个文件到位：`data/AppLanguage.kt`、`data/LanguageStore.kt`、`BaseActivity.kt`；
- [ ] 三个 Activity 的父类都是 `class XxxActivity : BaseActivity()`，且没有
  `import androidx.appcompat.app.AppCompatActivity`（复制过来的文件本来就满足，核对即可）；
- [ ] `res/menu/menu_main.xml` 已新建并粘好，`MainActivity` 里有 `onCreateOptionsMenu()`
  和 `onOptionsItemSelected()` 两个方法。

> 语言切换的运行验证（切英文、重启保持、清数据恢复系统语言）在 7.6 做。
> 菜单里的 `menu_ai_advice` 依赖第 7 节的 `AdviceActivity`：不做 AI，就把
> `menu_main.xml` 里的这一项、`onOptionsItemSelected()` 里的对应分支一起删掉。

---



## 7. 可选 B：AI 健康建议

顺序：**先能拼提示词（可单测）→ 再能发请求 → 最后才是页面。**

### 7.1 构建配置

1. 在 `local.properties` 里加一行（这个文件不进 Git）：

```properties
deepseek.apiKey=sk-你的key
```

1. `app/build.gradle` 里要有三块：

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

1. 依赖（4 个）：`activity-ktx`、`lifecycle-viewmodel-ktx`、`lifecycle-livedata-ktx`、
  `kotlinx-coroutines-android`。

> `build.gradle` 改完要点一次 **Sync Now**（编辑器右上角的提示条，或大象图标）。不 Sync
> 的话，7.6 编译会报找不到 `ViewModel`、`viewModelScope`、`LiveData`——不是代码写错，
> 是依赖还没下载。



### 7.2 Manifest

两段内容，**放在不同层级**，这是 Manifest 的硬性结构要求：


| 片段                                 | 必须放的位置                                             | 放错的后果                                                                                |
| ---------------------------------- | -------------------------------------------------- | ------------------------------------------------------------------------------------ |
| `<uses-permission ... />`          | `<manifest>` 的直接子元素，**在** `<application>` **之前**   | 写进 `<application>` 里会被当成非法子元素（`Element uses-permission is not allowed here`），权限也不会生效 |
| `<activity ... />`（AdviceActivity） | `<application>` 的直接子元素，**和** `MainActivity` **平级** | 嵌在别的 `<activity>` 里会报 `Element activity is not allowed here`，那个页面也不会被注册              |


```xml
<uses-permission android:name="android.permission.INTERNET" />

<activity
    android:name=".AdviceActivity"
    android:exported="false"
    android:label="@string/ai_title" />
```

放到你文件里的样子（骨架，`…` 是你已有的内容）：

```xml
<manifest xmlns:android="http://schemas.android.com/apk/res/android">

    <uses-permission android:name="android.permission.INTERNET" />   <!-- 新增：在 application 之前 -->

    <application ...>                                                <!-- 只能有一个 -->
        <activity ... />                                             <!-- HistoryActivity，已有 -->
        <activity ... />                                             <!-- AdviceActivity，新增，与上面平级 -->
        <activity android:name=".MainActivity" ...>                  <!-- 入口页，已有 -->
            <intent-filter> ... </intent-filter>
        </activity>
    </application>

</manifest>
```

三个 `<activity>` 谁写在前面都可以，但每个都必须直接写在 `<application>` 里，并且各自用
`/>` 或 `</activity>` 正确结束。

漏掉 `INTERNET` 的典型现象：点开 AI 页立刻闪退（`SecurityException`）。

### 7.3 从纯 Kotlin 开始（可以先不碰界面）

1. `data/PromptBuilder.kt` —— 拼 system / user 提示词，不 import 任何 Android 类；
2. 复制 `app/src/test/java/com/example/bmi/PromptBuilderTest.kt`；！！！ 记得复制这个文件
3. **先不要跑测试**：`testDebugUnitTest` 会连带编译主源码，而 `AdviceActivity` 还没建，
  现在跑一定失败。测试放到 7.6 一起跑。

这一步的意义是把“语言 → 提示词”的逻辑单独写出来，等 7.6 编译通过后，用那 7 个用例
验证它，不用等界面。

### 7.4 再接通网络


| 顺序  | 文件                                 | 职责                                        |
| --- | ---------------------------------- | ----------------------------------------- |
| 1   | `data/DeepSeekClient.kt`           | `HttpURLConnection` 发 POST、解析 JSON        |
| 2   | `data/WellnessAdviceRepository.kt` | `withContext(Dispatchers.IO)` + 统一异常      |
| 3   | `ui/advice/AdviceUiState.kt`       | `Idle/Loading/Success/Error` + 错误原因       |
| 4   | `ui/advice/AdviceViewModel.kt`     | `viewModelScope` 发请求、`LiveData`、`retry()` |


**目录要自己建**：第 1、2 个放进已有的 `data` 包；第 3、4 个在 `com.example.bmi.ui.advice`
包里，你的工程里应该还没有 `ui` 目录。

- 建包：右键 `com.example.bmi` → **New → Package**，输入 `ui.advice`（一次建出两层）；
- 建文件：右键刚建好的 `ui.advice` → **New → Kotlin Class/File** → 选 **File**，
文件名 `AdviceUiState`、`AdviceViewModel`，然后整份覆盖。
- 每个文件的第一行必须是 `package com.example.bmi.ui.advice`，与目录一致；不一致时
Studio 会报 "Package name does not match directory"，`import com.example.bmi.ui.advice.*`
也会全部失效。



### 7.5 最后是页面


| 文件                               | 作用                    |
| -------------------------------- | --------------------- |
| `res/layout/activity_advice.xml` | 摘要 + 转圈 + 建议文字 + 重试按钮 |
| `AdviceActivity.kt`              | 查今天记录 → 发请求 → 按状态渲染   |




### 7.6 首次编译 + 统一验收

**走到这里，四个互相引用的文件才第一次凑齐。现在才点编译：**

- 命令行：`gradlew.bat assembleDebug`
- IDE：`Build → Make Project`（或直接 Run）

那一屏红应当全部消失。如果还有错，回到 4.2 的表对照一下缺的是哪一节的文件，
不要靠猜着删代码“消红”。

> 跳过第 6、7 节的情况：按 `docs/exam-guide.md` 删完相关代码后，首次编译同样在这里做，
> 验收只取下面“编译与安装”“主界面”“历史页”三组。

**编译与安装**

- [ ] `BUILD SUCCESSFUL`，没有 `Unresolved reference`；
- [ ] `gradlew.bat testDebugUnitTest` → 7 个用例全绿（7.3 的提示词测试）；
- [ ] 装到模拟器能启动，不闪退。

**主界面（第 4 节）**

- [ ] 能看到“今天：日期”、下拉框、两个输入框、两个按钮；
- [ ] 不填步数直接保存 → 出现“请输入有效的步数”；
- [ ] 填 8000 保存 → 出现“已保存今天的记录”。

**历史页（第 5 节）**

- [ ] 点“查看历史”能打开列表，看到“日期 · 状况”“步数 · 备注”；
- [ ] 同一天再保存一次 → 仍然只有一条；
- [ ] 没有任何记录时 → 显示“暂无记录”。

**语言切换（第 6 节，可选）**

- [ ] 菜单点“切换语言” → 整页变英文，标题也变；
- [ ] 再点一次 → 变回中文；
- [ ] 杀掉进程重开 → 保持上次的语言；
- [ ] 系统设置里“清除数据” → 恢复跟随系统语言。

**AI 建议（第 7 节，可选）**

- [ ] 没有今天的记录时进 AI 页 → 提示“今天还没有记录”，不发请求；
- [ ] 有记录 → 转圈 → 出现 3 条建议；
- [ ] 中文界面拿到中文建议，英文界面拿到英文建议；
- [ ] 断网 → 提示网络错误 + 出现“重试”按钮；
- [ ] 转屏 → 不重复请求。

---



## 8. 和仓库逐文件对齐

做完后，对着下表检查（左边是仓库，右边是你工程）：


| 仓库路径                                                                              | 你工程里应当有                  |
| --------------------------------------------------------------------------------- | ------------------------ |
| `app/src/main/java/com/example/bmi/MainActivity.kt`                               | ✅                        |
| `.../HistoryActivity.kt`                                                          | ✅                        |
| `.../BaseActivity.kt`（可选）                                                         | ✅ 或按 `exam-guide.md` 删掉  |
| `.../AdviceActivity.kt`（可选）                                                       | ✅ 或删掉                    |
| `.../data/Dates.kt`、`WellnessRecord.kt`、`WellnessDbHelper.kt`                     | ✅                        |
| `.../data/AppLanguage.kt`、`LanguageStore.kt`（可选）                                  | ✅ 或删掉                    |
| `.../data/PromptBuilder.kt`、`DeepSeekClient.kt`、`WellnessAdviceRepository.kt`（可选） | ✅ 或删掉                    |
| `.../ui/advice/`（可选）                                                              | 两文件，或整个目录删掉              |
| `res/layout/activity_main.xml`、`activity_history.xml`、`item_wellness.xml`         | ✅                        |
| `res/layout/activity_advice.xml`（可选）                                              | ✅ 或删掉                    |
| `res/menu/menu_main.xml`（可选）                                                      | ✅ 或删掉                    |
| `res/values/strings.xml`、`values-zh-rCN/strings.xml`                              | ✅ 两套                     |
| `res/values/themes.xml`、`values-night/themes.xml`、`colors.xml`                    | ✅                        |
| `app/src/test/.../PromptBuilderTest.kt`（可选）                                       | ✅ 或删掉                    |
| `AndroidManifest.xml`                                                             | 注册了对应页面 +（AI 才有）INTERNET |
| `app/build.gradle`、`build.gradle`、`settings.gradle`                               | 与本仓库同版本                  |


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


| 项目         | 本仓库                                                            | 2025.2.1 Patch 1 是否支持   | 结论                        |
| ---------- | -------------------------------------------------------------- | ----------------------- | ------------------------- |
| AGP        | 8.13.0                                                         | 支持（最高到 AGP 8.13）        | 不用改                       |
| Gradle     | 8.13（wrapper 指定）                                               | 会按 wrapper 自动下载         | 不用改                       |
| Kotlin     | 2.2.20                                                         | 从 Maven 下载插件，与 IDE 版本无关 | 不用改                       |
| JDK        | 17 字节码，需要 JDK ≥ 17                                             | Studio 自带 JBR 21        | 不用改                       |
| compileSdk | 36                                                             | 该 Studio 默认装 36         | 不用改（前提是机器上装了 Platform 36） |
| targetSdk  | 34                                                             | 支持                      | 不用改                       |
| minSdk     | 21                                                             | 支持                      | 不用改                       |
| Material   | 1.12.0                                                         | 支持                      | 不用改                       |
| 语言 API     | `Locale.forLanguageTag`（21+）、`createConfigurationContext`（17+） | 都低于 minSdk              | 不用改                       |


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


| 现象                                                                                         | 原因                             | 处理（不要改仓库里的版本号）                                                                                                                                                            |
| ------------------------------------------------------------------------------------------ | ------------------------------ | ------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| Sync 报 `Dependency requires at least JVM runtime version 11. This build uses a Java 8 JVM` | IDE 的 Gradle JDK 指向了 JRE 8/11  | Settings → Build, Execution, Deployment → Build Tools → **Gradle → Gradle JDK** 选 17 或 21（本机实测：JBR 21 可用；只有 `jre` 没有 `jlink` 的 JDK 会报 `jlink.exe does not exist`，换完整 JDK） |
| Sync 报找不到 `android-36`                                                                     | 本机 SDK 只装到 34/35               | SDK Manager 装 Platform 36 + Build-Tools                                                                                                                                   |
| 打开工程时提示 “Android Gradle Plugin requires newer Studio”                                      | 本机 Studio 比 2025.2.1 旧         | 只能用命令行 `gradlew.bat assembleDebug` 构建；**不要**为了能在 IDE 里打开而降 AGP 版本，否则改完忘了还原，考试就炸                                                                                           |
| 打开工程时提示 “Upgrade AGP / Migrate to …”                                                       | 本机 Studio 比 2025.2.1 新         | 点 **Don't remind me / Cancel**；如果已经点了，用 `git checkout -- build.gradle app/build.gradle gradle/` 还原                                                                        |
| 向导生成 `build.gradle.kts` + `libs.versions.toml`                                             | 新版模板默认 Kotlin DSL              | 见 0.2/0.4；**不要把** `.kts` **片段和本仓库的 Groovy 混着粘**                                                                                                                           |
| 粘贴后一片红：`Unresolved reference: R`                                                           | 包名 / `namespace` 与新工程不一致       | 改 `namespace` 和每个文件的 `package`，或一开始就用 `com.example.bmi`                                                                                                                   |
| 中文注释变乱码                                                                                    | 文件编码不是 UTF-8                   | Settings → Editor → **File Encodings** 全部设 UTF-8；`gradle.properties` 已有 `-Dfile.encoding=UTF-8`，别删                                                                        |
| 用记事本改过文件后 `git apply` 失败/差异巨大                                                              | 记事本把 LF 改成 CRLF 或加了 BOM        | 用 Android Studio / VS Code 编辑；不要用记事本“另存为”                                                                                                                                 |
| `local.properties` 出现在 `git status` 里                                                      | 被手动 `git add -f` 过             | `git rm --cached local.properties`；`.gitignore` 里已有这一行                                                                                                                    |
| 依赖下载卡住                                                                                     | 本机访问 google()/mavenCentral() 慢 | `settings.gradle` 里已经准备好阿里云镜像，取消注释即可（本机改动，不要提交）                                                                                                                           |
| 模拟器跑不起来                                                                                    | 镜像 API 太高或架构不匹配                | 用 API 33/34 的 x86_64 镜像；项目 minSdk 21，任何近期镜像都行                                                                                                                             |




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


| 时间        | 做什么                                      | 验收                   |
| --------- | ---------------------------------------- | -------------------- |
| 0–10 min  | 新建工程、对齐四个构建文件、Sync                       | 能 Run 模板页            |
| 10–20 min | 资源：strings（中英）、themes、颜色                 | 无红色报错                |
| 20–30 min | 数据层：Record、Dates、DbHelper                | 编译通过（界面层之前的最后一次单独编译） |
| 30–45 min | 主界面 + 历史页：布局与代码全部复制，**不编译**              | 文件与仓库一致，红标属正常        |
| 45–60 min | 可选功能：语言切换 + AI（按题目取舍）                    | 文件与 Manifest 到位      |
| 60–72 min | 首次编译 + 自测清单（7.6）                         | 编译通过，各功能逐项打勾         |
| 72–80 min | 清理临时文件、确认没有提交 `.idea`/`local.properties` | `git status` 干净      |


如果时间不够，**优先保证“能保存 + 能看历史”**，语言和 AI 都是加分项；但注意：既然第 4–7 节
是“复制完再统一编译”，中途砍功能要用 `docs/exam-guide.md` 的删除清单，不能只删文件不删引用。

---



## 11. 一页速查

**建工程的顺序**：构建文件 → 资源 → 数据类 → 数据库 → 主界面 → 历史页 → 可选功能。

**编译点只有两个**：第 3 节末（数据层，能单独跑）、第 7 节末（四个互相引用的文件到齐，见 7.6）。
中间第 4–6 节只复制、不编译也不运行，那时的满屏红属于预期状态。

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