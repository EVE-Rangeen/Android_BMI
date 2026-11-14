package com.example.bmi.data

/*
 * ============================================================================
 * AppLanguage.kt —— 应用当前显示的语言
 * ============================================================================
 * 【这个文件是干什么的】
 *   App 同时支持中文和英文。界面上到底显示中文还是英文，由系统的语言设置决定；
 *   而“用中文还是英文去问 AI”也由这个枚举决定。
 *
 * 【枚举（enum）是什么】
 *   enum 就是“只能取几个固定值”的类型。这里 AppLanguage 只能取 ENGLISH 或 CHINESE，
 *   不可能出现第三种情况，这样编译器就能帮我们防止拼错字符串。
 *
 * 【谁在用这个文件】
 *   1. ReportActivity.currentLanguage() 会根据手机语言，创建出 AppLanguage.ENGLISH
 *      或 AppLanguage.CHINESE；
 *   2. PromptBuilder 用它决定“给 AI 的提示词用中文还是英文写”；
 *   3. 界面上的字符串（“高度”“Height”）不是这个文件负责的，那是 Android 的资源系统
 *      根据 values/ 和 values-zh-rCN/ 两个目录自动切换的。
 */
enum class AppLanguage {

    /** 英文 */
    ENGLISH,

    /** 中文 */
    CHINESE;

    /*
     * companion object 可以理解为“挂在 enum 上的静态工具函数”。
     * 也就是说：我们不需要 new 一个对象，直接写 AppLanguage.fromLanguageTag(...) 就能调用。
     */
    companion object {

        /**
         * 把一个“语言标签”转换成 AppLanguage。
         *
         * @param languageTag 例如手机系统给我们的 "zh"（中文）、"en"（英文）。
         *                    Android 里语言标签可能带地区，比如 "zh-CN"、"en-US"，
         *                    所以我们只用 startsWith 判断前缀。
         * @return 以 "zh" 开头的都当成中文，其余一律当英文（简单可靠）。
         */
        fun fromLanguageTag(languageTag: String): AppLanguage =
            // startsWith(..., ignoreCase = true) 表示“忽略大小写，看字符串是不是以 zh 开头”。
            // 这样 "ZH"、"zh-CN"、"zh-TW" 都会被识别成中文。
            if (languageTag.startsWith("zh", ignoreCase = true)) CHINESE else ENGLISH
    }
}
