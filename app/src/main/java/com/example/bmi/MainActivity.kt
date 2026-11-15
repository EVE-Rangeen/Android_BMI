package com.example.bmi

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.ContextMenu
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.Spinner
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.bmi.data.BmiPreferences
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar

/*
 * ============================================================================
 * MainActivity.kt —— 主界面（输入身高/体重/年龄/性别）
 * ============================================================================
 * 【这个文件是干什么的】
 *   App 启动后的第一个界面。用户在这里输入身高、体重、年龄，选择性别，
 *   点“See BMI Report Now”后把数据打包，跳转到 ReportActivity 去计算结果。
 *
 * 【一句话看懂整个流程】
 *   用户输入 → 点按钮 → 校验输入合法 → 用 SharedPreferences 记住输入
 *           → 用 Intent + Bundle 把数据带到 ReportActivity → 跳转。
 *
 * 【Activity 是什么】
 *   Activity 是 Android 的“一个屏幕”。onCreate() 是这个屏幕刚创建时调用的方法，
 *   相当于“构造函数 + 初始化入口”。所有控件的查找、点击事件都在这里设置。
 *
 * 【本文件用到哪些考试常考知识点】
 *   1. findViewById：根据布局里的 id 拿到控件对象；
 *   2. 从 EditText 读取用户输入（text 属性）；
 *   3. 字符串转数字并校验（toDoubleOrNull / toIntOrNull）；
 *   4. SharedPreferences 保存/读取（自动填充）；
 *   5. Intent + Bundle 在页面之间传数据；
 *   6. 菜单（选项菜单 OptionsMenu + 上下文菜单 ContextMenu）；
 *   7. 对话框（MaterialAlertDialogBuilder）。
 */
class MainActivity : AppCompatActivity() {

    /*
     * 下面是“控件引用”。lateinit 表示“稍后再初始化”（在 onCreate 里用 findViewById 赋值）。
     * 有了这些变量，之后点按钮时就能直接读写界面上的输入框。
     */
    lateinit var heightET: EditText    // 身高输入框
    lateinit var weightET: EditText    // 体重输入框
    lateinit var ageET: EditText       // 年龄输入框
    lateinit var genderSpinner: Spinner// 性别下拉框
    lateinit var bmiImage: ImageView   // BMI 图标
    private lateinit var preferences: BmiPreferences  // SharedPreferences 封装

    /**
     * 屏幕创建时调用。整个界面的初始化都发生在这里。
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // 把 res/layout/activity_main.xml 这个布局“贴”到屏幕上。
        setContentView(R.layout.activity_main)

        // 创建 SharedPreferences 封装（用来记住用户上次输入的内容）。
        preferences = BmiPreferences(this)

        /*
         * findViewById<T>(R.id.xxx)：根据布局里某个控件的 android:id，拿到它的对象。
         * R.id.heightET 就是 activity_main.xml 里 <EditText android:id="@+id/heightET"> 的那个控件。
         */
        heightET = findViewById(R.id.heightET)
        weightET = findViewById(R.id.weightET)
        ageET = findViewById(R.id.ageET)
        genderSpinner = findViewById(R.id.genderSpinner)
        bmiImage = findViewById(R.id.imageView)
        val reportBtn: Button = findViewById<Button>(R.id.reportBtn)

        // 给 BMI 图标注册“长按弹出菜单”（ContextMenu）。
        registerForContextMenu(bmiImage)

        /*
         * ────────────────────────────────────────────────────────────────────
         * 【重点】如何从表单读取用户输入
         * ────────────────────────────────────────────────────────────────────
         * 点击“查看报告”按钮时，做下面几件事：
         *   1. 从三个 EditText 读字符串；
         *   2. 从 Spinner 读选中的位置序号；
         *   3. 把字符串转成数字并校验；
         *   4. 校验通过 → 保存 + 打包 → 跳转。
         * ────────────────────────────────────────────────────────────────────
         */
        reportBtn.setOnClickListener(object : View.OnClickListener {
            override fun onClick(v: View?) {
                /*
                 * 【读取 EditText 的内容】
                 * heightET 是 EditText（输入框），它的 text 属性是 Editable 类型，
                 * 调用 .toString() 才能变成普通 String。
                 * 注意：用户没输入时，text 是空字符串 ""，不是 null。
                 *
                 * 另一种等价写法：heightET.editableText.toString()
                 * （Editable 本质上也是 CharSequence，两种写法结果一样。）
                 */
                val height = heightET.text.toString()
                val weight = weightET.text.toString()
                val age = ageET.text.toString()

                /*
                 * 【读取 Spinner 的选中项】
                 * genderSpinner.selectedItemPosition 返回选中的“位置序号”（从 0 开始）。
                 * 这里故意传“位置”而不是传“男/女文字”，
                 * 因为文字会跟着语言变（中文“男”、英文“Male”），
                 * 而位置 0/1 永远不变，后面的逻辑不会受语言影响。
                 */
                val gender = genderSpinner.selectedItemPosition

                /*
                 * 【字符串转数字 + 校验】
                 * toDoubleOrNull()：能转成小数就返回数值，转不了（比如输入了字母）就返回 null。
                 * 这样就不用手写 try-catch，也不怕用户输入非法内容导致崩溃。
                 * toIntOrNull() 同理（年龄是整数）。
                 */
                val heightValue = height.toDoubleOrNull()
                val weightValue = weight.toDoubleOrNull()
                val ageValue = age.toIntOrNull()

                /*
                 * 校验：任何一个没输入（null）、小于等于 0（年龄允许 0 也算非法）都算无效。
                 * 无效时用 Snackbar（屏幕底部弹出的小提示条）提示，不跳转。
                 */
                if (heightValue == null || weightValue == null || ageValue == null ||
                    heightValue <= 0 || weightValue <= 0 || ageValue < 0
                ) {
                    Snackbar.make(
                        findViewById<View>(android.R.id.content),  // 挂在根布局上
                        R.string.bmi_warning,                       // 提示文案（多语言）
                        Snackbar.LENGTH_SHORT                       // 显示时长：短
                    ).show()
                } else {
                    // ── 校验通过：先记住这次输入（下次自动填充）──
                    preferences.save(height, weight, age, gender)

                    /*
                     * ─────────────────────────────────────────────────────────
                     * 【重点】用 Intent 把数据从 MainActivity 传到 ReportActivity
                     * ─────────────────────────────────────────────────────────
                     * Intent = 意图，用来“启动另一个界面 / 在界面间传数据”。
                     * Bundle = 一个“键值对袋子”，能装多组数据一起带过去。
                     *
                     * 传什么：身高、体重、年龄、性别位置（都是字符串/整数）。
                     * 怎么取：在 ReportActivity 里用 intent.getStringExtra("height") 等取回。
                     * ─────────────────────────────────────────────────────────
                     */
                    val intent = Intent(applicationContext, ReportActivity::class.java)
                    val bundle = Bundle()
                    bundle.putString("height", height)   // key="height" 存身高
                    bundle.putString("weight", weight)   // key="weight" 存体重
                    bundle.putInt("gender", gender)      // key="gender" 存性别位置
                    bundle.putString("age", age)         // key="age" 存年龄
                    intent.putExtras(bundle)             // 把整个袋子塞进 Intent
                    startActivity(intent)                // 跳转
                }

                /*
                 * ── 【考点 / 备用方案】传值的另外两种常见写法 ─────────────────
                 * （把上面 else 块替换成下面任一种都可以，效果几乎一样）
                 *
                 * 方案 B：不包 Bundle，直接 putExtra：
                 *   val intent = Intent(this, ReportActivity::class.java)
                 *   intent.putExtra("height", height)
                 *   intent.putExtra("weight", weight)
                 *   intent.putExtra("age", age)
                 *   intent.putExtra("gender", gender)
                 *   startActivity(intent)
                 *
                 * 方案 C：用 companion object 的静态变量传值（不推荐，但考试偶尔考到）：
                 *   在 ReportActivity 里写：
                 *       companion object { var height: String = "" ; var weight = "" ... }
                 *   这里写：
                 *       ReportActivity.height = height
                 *       ReportActivity.weight = weight
                 *       startActivity(Intent(this, ReportActivity::class.java))
                 *   （缺点：全局变量容易造成状态混乱，仅用于理解“静态变量”这个概念。）
                 * ────────────────────────────────────────────────────────────────────
                 */
            }
        })

        // “关于 BMI”按钮：弹一个对话框。
        val aboutBtn: Button = findViewById(R.id.aboutBtn)
        aboutBtn.setOnClickListener {
            about_BMI_Dialog()
        }

        /*
         * 【自动填充】
         * savedInstanceState 是 Android 在“旋转屏幕 / 系统回收界面”时保存下来的现场。
         * 如果它是 null，说明这是“第一次创建界面”（不是旋转后的重建），
         * 此时才用 SharedPreferences 把上次输入的内容填回去。
         *
         * 为什么只在这一步填，而不在 onStart() 里每次填？
         * 因为旋转屏幕时 Android 会自动帮 EditText 恢复它自己输入的内容；
         * 如果我们在 onStart 里又用旧数据覆盖一次，用户旋转前刚改、还没提交的内容就丢了。
         */
        if (savedInstanceState == null) {
            restoreProfile()
        }
    }

    /**
     * 把 SharedPreferences 里记住的内容填回输入框。
     * 首次打开 App 时，这里就是“自动填充上次输入”的地方。
     */
    private fun restoreProfile() {
        // setText 会把字符串填进输入框；preferences.height() 返回上次保存的身高。
        heightET.setText(preferences.height())
        weightET.setText(preferences.weight())
        ageET.setText(preferences.age())

        /*
         * 恢复下拉框选中项：
         * genderSpinner.adapter.count 是下拉框里一共有几项（这里是 2）。
         * preferences.gender() 是上次保存的位置；coerceIn(0, count-1) 把它限制在合法范围，
         * 防止保存值越界导致崩溃。
         */
        if (genderSpinner.adapter.count > 0) {
            genderSpinner.setSelection(
                preferences.gender().coerceIn(0, genderSpinner.adapter.count - 1)
            )
        }
    }

    // ===================== 选项菜单（右上角三个点）=====================

    /**
     * 创建右上角的选项菜单。inflate 就是把 res/menu/menu_options.xml 里定义的
     * 菜单项“加载”进 menu 对象。
     */
    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_options, menu)
        return super.onCreateOptionsMenu(menu)
    }

    /**
     * 用户点击某个菜单项时回调。根据 itemId 判断点了哪一个。
     */
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.menu_about -> {
                about_BMI_Dialog()
                return true
            }
            R.id.menu_wiki -> {
                openWebpage(getString(R.string.wiki_url))
                return true
            }
            R.id.menu_trend -> {
                // 打开趋势页（不传数据，因为趋势页自己从数据库读历史）。
                startActivity(Intent(this, TrendActivity::class.java))
                return true
            }
            R.id.menu_exit -> {
                finish()   // 结束当前 Activity，回到桌面/上一层
                return true
            }
        }
        return super.onOptionsItemSelected(item)
    }

    // ===================== 上下文菜单（长按 BMI 图标）=====================

    /**
     * 创建“长按”菜单。必须配合 onCreate 里的 registerForContextMenu(图片) 使用。
     */
    override fun onCreateContextMenu(
        menu: ContextMenu,
        v: View,
        menuInfo: ContextMenu.ContextMenuInfo?
    ) {
        super.onCreateContextMenu(menu, v, menuInfo)
        menuInflater.inflate(R.menu.menu_context, menu)
    }

    /** 处理长按菜单的点击。 */
    override fun onContextItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.menu_item_about -> {
                about_BMI_Dialog()
                return true
            }
            R.id.menu_item_wiki -> {
                openWebpage(getString(R.string.wiki_url))
                return true
            }
            R.id.menu_item_trend -> {
                startActivity(Intent(this, TrendActivity::class.java))
                return true
            }
            else -> return super.onContextItemSelected(item)
        }
    }

    /**
     * 用系统浏览器打开一个网址。
     * ACTION_VIEW 表示“查看某样东西”，配合一个 Uri 就是“打开网页”。
     */
    private fun openWebpage(url: String) {
        val webpage = Uri.parse(url)   // 字符串 → Uri 对象
        val intent = Intent(Intent.ACTION_VIEW, webpage)
        try {
            startActivity(intent)
        } catch (e: ActivityNotFoundException) {
            // 万一手机上没有浏览器，捕获异常并提示，避免崩溃。
            Snackbar.make(
                findViewById<View>(android.R.id.content),
                R.string.no_browser,
                Snackbar.LENGTH_SHORT
            ).show()
        }
    }

    /**
     * 弹出“关于 BMI”对话框。
     * 这里用 MaterialAlertDialogBuilder（Material 组件库的对话框）。
     */
    fun about_BMI_Dialog() {
        MaterialAlertDialogBuilder(this)
            .setTitle(R.string.about_button)          // 标题
            .setMessage(R.string.about_msg)           // 正文
            .setPositiveButton(R.string.button_ok) { dialog, which ->
                // 点“确定”后弹一个 Toast（短暂的小提示）。
                Toast.makeText(
                    this@MainActivity,
                    getString(R.string.button_ok),
                    Toast.LENGTH_SHORT
                ).show()
            }
            .show()

        /*
         * ── 【考点 / 备用方案】用系统自带 AlertDialog 代替 Material 对话框 ──
         * 如果考试环境没有 Material 依赖，或老师要求用最基础的 AlertDialog：
         * 把上面 about_BMI_Dialog 换成下面这段即可（并 import android.app.AlertDialog）：
         *
         *   fun about_BMI_Dialog() {
         *       AlertDialog.Builder(this)
         *           .setTitle(R.string.about_button)
         *           .setMessage(R.string.about_msg)
         *           .setPositiveButton(R.string.button_ok, null)   // null = 点击后直接关闭
         *           .setNegativeButton(R.string.button_ok, null)   // 也可以加“取消”按钮
         *           .show()
         *   }
         * ────────────────────────────────────────────────────────────────────
         */
    }
}
