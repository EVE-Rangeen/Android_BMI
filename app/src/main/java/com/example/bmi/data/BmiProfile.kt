package com.example.bmi.data

/*
 * ============================================================================
 * BmiProfile.kt —— 一次 BMI 计算涉及的“用户个人信息”
 * ============================================================================
 * 【这个文件是干什么的】
 *   把“性别 + 年龄 + 算好的 BMI 值”打包成一个对象。这个对象会作为参数
 *   在 ReportActivity → ReportViewModel → HealthAdviceRepository → PromptBuilder
 *   之间传递，最终变成发给 AI 的提示词。
 *
 * 【为什么不直接用三个零散变量】
 *   把相关的数据放在一个 data class 里，一次传递一个对象，比传三个参数更清晰，
 *   也不容易漏传、传错顺序。这就是“Model（数据模型）”的含义。
 *
 * 【data class 是什么】
 *   data class 会自动生成 equals / hashCode / toString / copy 等方法，
 *   非常适合“只用来装数据”的类。
 */

/**
 * 用户的性别。
 *
 * 注意：性别是从 MainActivity 的下拉框（Spinner）里选出来的，
 * 下拉框只给了我们一个“位置序号”（0 或 1），
 * 这里把它翻译成有明确含义的 MALE / FEMALE。
 */
enum class Gender {
    MALE,    // 男
    FEMALE;  // 女

    companion object {
        /**
         * 把下拉框的位置序号转换成 Gender。
         *
         * 下拉框的数据来自 res/values/strings.xml 里的 <string-array name="gender">，
         * 第 0 项是“男”，第 1 项是“女”。
         * 因此：位置 1 是女性，其余情况一律按男性处理（防御性写法，避免越界出错）。
         */
        fun fromSpinnerPosition(position: Int): Gender =
            if (position == 1) FEMALE else MALE
    }
}

/**
 * 一次 BMI 计算所需的全部个人信息。
 *
 * @property bmi    已经算好的 BMI 数值（体重 / 身高²），由 ReportActivity 计算。
 * @property age    年龄（整数）。
 * @property gender 性别。
 */
data class BmiProfile(
    val bmi: Double,
    val age: Int,
    val gender: Gender
)
