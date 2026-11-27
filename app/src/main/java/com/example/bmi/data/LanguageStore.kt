package com.example.bmi.data

import android.content.Context

/*
 * ============================================================================
 * LanguageStore.kt —— 用 SharedPreferences 记住用户选的语言
 * ============================================================================
 * 【这个文件是干什么的】
 *   用户在菜单里点“切换语言”之后，App 要记住这个选择，下次打开还是同一个语言。
 *   这种“少量键值对”的数据，Android 推荐用 SharedPreferences 保存。
 *
 * 【SharedPreferences 怎么用（考试高频考点）】
 *   1. 拿对象：context.getSharedPreferences("文件名", Context.MODE_PRIVATE)
 *      —— MODE_PRIVATE 表示只有本 App 能读写；
 *   2. 读：prefs.getString("key", 默认值)，找不到 key 时返回默认值（这里是 null）；
 *   3. 写：prefs.edit().putString("key", 值).apply()
 *      —— apply() 异步写磁盘（不卡界面），commit() 同步写（返回是否成功）。
 *
 * 【为什么不把语言存进 SQLite】
 *   语言只是一个开关值，不需要查询、排序、关联，用 SharedPreferences 最轻量。
 *
 * ────────────────────────────────────────────────────────────────────────────
 * 【依赖关系 / 考试删除指引】
 *   本文件依赖：AppLanguage。
 *   谁依赖本文件：BaseActivity（读取）、MainActivity（写入）。
 *   ★ 考试不要求中英文切换时，删掉本文件（见 docs/exam-guide.md A 部分）。
 * ────────────────────────────────────────────────────────────────────────────
 */
class LanguageStore(context: Context) {

    // applicationContext：避免持有 Activity 引用造成内存泄漏。
    private val prefs =
        context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    /**
     * 读取用户保存过的语言。
     *
     * @return 用户选过的语言；从没选过时返回 null（表示“跟随手机系统语言”）。
     *
     * 返回 null 而不是直接返回英文，是为了保留 Android 默认的多语言行为：
     * 第一次装机、用户没手动切换过时，中文手机自动显示中文。
     */
    fun load(): AppLanguage? = prefs.getString(KEY_LANGUAGE, null)?.let(AppLanguage::fromTag)

    /** 保存用户选择的语言（apply() 异步写入，不阻塞界面）。 */
    fun save(language: AppLanguage) {
        prefs.edit().putString(KEY_LANGUAGE, language.tag).apply()
    }

    private companion object {
        const val PREFS_NAME = "wellness_settings"
        const val KEY_LANGUAGE = "language_tag"
    }
}
