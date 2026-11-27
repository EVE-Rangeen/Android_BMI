package com.example.bmi

import android.content.Context
import android.content.res.Configuration
import androidx.appcompat.app.AppCompatActivity
import androidx.core.os.ConfigurationCompat
import com.example.bmi.data.AppLanguage
import com.example.bmi.data.LanguageStore
import java.util.Locale

/*
 * ============================================================================
 * BaseActivity.kt —— 所有页面的“父类”，统一处理中英文切换（【语言切换 · 考试可删】）
 * ============================================================================
 * 【这个文件是干什么的】
 *   让 MainActivity / HistoryActivity / AdviceActivity 都继承它，
 *   这样“界面该用中文还是英文”只需要在这一个地方写一次。
 *
 * 【核心原理：attachBaseContext】
 *   Activity 启动时，系统会先调用 attachBaseContext(newBase) 把“上下文”交给它，
 *   然后才会加载布局和字符串资源。我们在这里把 newBase 换成一个
 *   “语言已经被改成中文（或英文）的 Context”，后面 getString() 取到的就是对应语言。
 *
 *   具体三步（见 applyLanguage）：
 *     1. new Configuration(旧配置)   —— 复制一份系统配置；
 *     2. config.setLocale(locale)    —— 把语言字段改成 en 或 zh；
 *     3. newBase.createConfigurationContext(config) —— 生成一个带新配置的 Context。
 *
 * 【为什么要 recreate()】
 *   已经显示出来的界面不会自动重画。切换语言后调用 recreate()，
 *   Activity 会被销毁再重建一遍：attachBaseContext 重新执行、
 *   布局里的 @string/xxx 重新解析，界面就整体变成新语言了。
 *
 * 【为什么不直接用 AppCompatDelegate.setApplicationLocales()】
 *   那个 API 需要 AndroidX 的额外服务配置，而且把“系统怎么挑资源”藏得太深。
 *   这里用最原始的 Configuration + Locale，考试时能讲清楚原理，也方便按需删除。
 *
 * ────────────────────────────────────────────────────────────────────────────
 * 【依赖关系 / 考试删除指引】
 *   本文件依赖：AppLanguage、LanguageStore。
 *   谁依赖本文件：MainActivity、HistoryActivity、AdviceActivity（都继承了它）。
 *   ★ 考试不要求中英文切换时（见 docs/exam-guide.md A 部分）：
 *     1. 删掉本文件、AppLanguage.kt、LanguageStore.kt；
 *     2. 三个 Activity 的 `: BaseActivity()` 改回 `: AppCompatActivity()`；
 *     3. 删掉 MainActivity 的 onCreateOptionsMenu/onOptionsItemSelected 里
 *        menu_language 那一段（AI 菜单项要保留就只删语言那一支）。
 *     界面就会恢复成“跟着手机系统语言走”的默认行为。
 * ────────────────────────────────────────────────────────────────────────────
 */
open class BaseActivity : AppCompatActivity() {

    /**
     * 在加载界面之前，把 Context 的语言换成用户选过的语言。
     *
     * 如果用户从没在菜单里切换过（load() 返回 null），就原样交给父类，
     * 让 Android 继续按手机系统语言挑资源。
     */
    override fun attachBaseContext(newBase: Context) {
        val saved = LanguageStore(newBase).load()
        if (saved == null) {
            super.attachBaseContext(newBase)
            return
        }
        super.attachBaseContext(applyLanguage(newBase, saved))
    }

    /** 生成一个“语言被改成 language”的 Context（原理见文件头部的三步说明）。 */
    private fun applyLanguage(base: Context, language: AppLanguage): Context {
        // Locale.forLanguageTag("zh") 按 BCP-47 语言标签创建区域对象，
        // 比已废弃的 Locale("zh") 构造函数更规范。
        val locale = Locale.forLanguageTag(language.tag)

        // 同步修改 JVM 的默认语言，保证 SimpleDateFormat 之类也跟界面一致。
        Locale.setDefault(locale)

        // 复制当前配置，只改语言这一项，其它（屏幕方向、字体缩放…）保持不变。
        val config = Configuration(base.resources.configuration)
        config.setLocale(locale)

        return base.createConfigurationContext(config)
    }

    /**
     * 当前界面用的是什么语言。
     * ConfigurationCompat.getLocales(...) 是 AndroidX 的兼容写法，
     * 在 API 21 的老手机和 Android 13 上都能拿到语言列表。
     */
    protected fun currentLanguage(): AppLanguage =
        AppLanguage.fromTag(ConfigurationCompat.getLocales(resources.configuration).get(0)?.language)

    /**
     * 切换语言并让界面立刻生效。
     *
     * recreate() 会销毁并重建当前 Activity，重建时 attachBaseContext
     * 会读到刚保存的新语言，于是整页文字一起变。
     */
    protected fun switchLanguage(next: AppLanguage) {
        LanguageStore(this).save(next)
        recreate()
    }

    /** 在“中文 ↔ 英文”之间来回切（菜单点击时调用）。 */
    protected fun toggleLanguage() {
        val next = if (currentLanguage() == AppLanguage.CHINESE) {
            AppLanguage.ENGLISH
        } else {
            AppLanguage.CHINESE
        }
        switchLanguage(next)
    }
}
