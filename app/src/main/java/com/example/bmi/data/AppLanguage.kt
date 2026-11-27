package com.example.bmi.data

/*
 * ============================================================================
 * AppLanguage.kt —— App 支持的语言（【语言切换 · 考试可删】）
 * ============================================================================
 * 【这个文件是干什么的】
 *   定义 App 只支持两种语言：英文和中文。
 *   界面文字由 res/values/ 和 res/values-zh-rCN/ 两套资源提供，
 *   而“现在用的是哪一套”由这个枚举 + LanguageStore 决定。
 *
 * 【enum（枚举）是什么】
 *   enum 就是“只能取几个固定值”的类型。AppLanguage 只能取 ENGLISH 或 CHINESE，
 *   不可能出现第三种值，写错编译器会直接报错。
 *
 * 【tag 是什么】
 *   tag 是语言的“国际标准代号”，Android 和 Java 都用它：
 *     en = English，zh = 中文。
 *   后面用 Locale("en") / Locale("zh") 生成区域对象，交给系统去挑资源目录。
 *
 * ────────────────────────────────────────────────────────────────────────────
 * 【依赖关系 / 考试删除指引】
 *   本文件依赖：无。
 *   谁依赖本文件：LanguageStore、BaseActivity、PromptBuilder、AdviceActivity。
 *
 *   ★ 考试题目【不要求中英文切换】时，按 docs/exam-guide.md 的 A 部分删除：
 *     - 本文件、LanguageStore.kt、BaseActivity.kt、res/menu/menu_main.xml 里的语言菜单项；
 *     - MainActivity / HistoryActivity 改回继承 AppCompatActivity。
 *     （AI 功能也想保留的话，把 AppLanguage 单独留着给 PromptBuilder 用即可。）
 * ────────────────────────────────────────────────────────────────────────────
 */
enum class AppLanguage(val tag: String) {

    /** 英文（对应 res/values/strings.xml）。 */
    ENGLISH("en"),

    /** 中文（对应 res/values-zh-rCN/strings.xml）。 */
    CHINESE("zh");

    companion object {

        /**
         * 把系统的语言代号转成 AppLanguage。
         *
         * @param languageTag 例如 "zh"、"zh-CN"、"en"；也可能拿不到（null）。
         * @return 以 "zh" 开头的都算中文，其余（含 null）一律算英文。
         *
         * startsWith("zh", ignoreCase = true) 会忽略大小写，
         * 所以 "ZH"、"zh-CN"、"zh-TW" 都能识别成中文。
         */
        fun fromTag(languageTag: String?): AppLanguage =
            if (languageTag?.startsWith("zh", ignoreCase = true) == true) CHINESE else ENGLISH
    }
}
