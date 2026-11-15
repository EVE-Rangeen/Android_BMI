package com.example.bmi.data

import android.content.Context

/*
 * ============================================================================
 * BmiPreferences.kt —— 用 SharedPreferences 记住上一次填写的表单
 * ============================================================================
 * 【这个文件是干什么的】
 *   用户填过的“身高 / 体重 / 年龄 / 性别”，下次打开 App 会自动填回去，
 *   省得每次都重新输入。这就是用 SharedPreferences 实现的“记住用户输入”。
 *
 * 【SharedPreferences 是什么】
 *   它是 Android 提供的一种“键值对”轻量存储：用字符串 key 存一个值，
 *   下次用同一个 key 取回来。适合存少量、简单的小数据（设置项、上次输入等）。
 *   数据存放在 App 私有的 XML 文件里，App 卸载后会被清掉。
 *
 * 【注意：SharedPreferences ≠ SQLite】
 *   - SharedPreferences：存“少量键值对”，比如“上次的身高是多少”。
 *   - SQLite：真正的“数据库”，能存很多行、能按日期查询，适合“每一天的 BMI 记录”。
 *   本项目两者都用：SharedPreferences 存“最近一次输入”，SQLite 存“历史趋势”。
 *
 * 【谁在用这个文件】
 *   MainActivity 在：
 *     1. 点“查看报告”按钮时调用 save(...) 保存；
 *     2. 首次打开时调用 height()/weight()/age()/gender() 自动填充。
 */
class BmiPreferences(context: Context) {

    /*
     * getSharedPreferences(name, MODE_PRIVATE)：
     *   - name          = "BMI"：这个文件会叫 BMI.xml；
     *   - MODE_PRIVATE  = 只有本 App 能读写（最常用）。
     * 使用 context.applicationContext 而不是 Activity 本身，是为了避免内存泄漏：
     * 这个对象可能比 Activity 活得久，持有 Activity 会导致 Activity 无法被回收。
     */
    private val preferences =
        context.applicationContext.getSharedPreferences(NAME, Context.MODE_PRIVATE)

    /**
     * 保存一次表单输入。
     *
     * @param height 身高（原始字符串，保留用户输入的原样，比如 "170"）。
     * @param weight 体重（字符串）。
     * @param age    年龄（字符串）。
     * @param gender 性别下拉框选中的位置（0 = 男，1 = 女）。
     */
    fun save(height: String, weight: String, age: String, gender: Int) {
        preferences.edit()                     // edit() 返回一个“编辑器”，准备写入
            .putString(KEY_HEIGHT, height)     // 以 KEY_HEIGHT 为 key 存入身高
            .putString(KEY_WEIGHT, weight)     // 以 KEY_WEIGHT 为 key 存入体重
            .putString(KEY_AGE, age)           // 以 KEY_AGE 为 key 存入年龄
            .putInt(KEY_GENDER, gender)        // 以 KEY_GENDER 为 key 存入性别位置
            .apply()                           // apply()：异步写入，立刻返回，不卡界面
        /*
         * ── 【考点 / 备用方案】apply() 与 commit() 的区别 ───────────────────
         * apply()  异步写盘：立即返回，不阻塞 UI 线程，推荐使用（本项目用这个）。
         * commit() 同步写盘：会返回 boolean 表示是否成功，但会阻塞当前线程。
         *          如果考试问“如何保证一定写成功”，可以换成 commit()：
         *
         *   val ok = preferences.edit()
         *       .putString(KEY_HEIGHT, height)
         *       ...
         *       .commit()   // 返回 true/false
         * ────────────────────────────────────────────────────────────────────
         */
    }

    /** 取回保存的身高；没有存过就返回空字符串 ""。 */
    fun height(): String = preferences.getString(KEY_HEIGHT, "") ?: ""

    /** 取回保存的体重；没有存过就返回空字符串 ""。 */
    fun weight(): String = preferences.getString(KEY_WEIGHT, "") ?: ""

    /** 取回保存的年龄；没有存过就返回空字符串 ""。 */
    fun age(): String = preferences.getString(KEY_AGE, "") ?: ""

    /** 取回保存的性别位置；没有存过就返回 0（即“男”）。 */
    fun gender(): Int = preferences.getInt(KEY_GENDER, 0)

    /*
     * companion object 里的常量 = “只属于这个类的全局常量”。
     * 把 key 提取成常量，是为了避免在多处手写 "height" 这样的字符串，
     * 拼错了编译器也发现不了；用常量则能在编译期检查。
     */
    private companion object {
        const val NAME = "BMI"            // SharedPreferences 文件名
        const val KEY_HEIGHT = "height"   // 身高对应的 key
        const val KEY_WEIGHT = "weight"   // 体重对应的 key
        const val KEY_AGE = "age"         // 年龄对应的 key
        const val KEY_GENDER = "gender"   // 性别对应的 key
    }
}
