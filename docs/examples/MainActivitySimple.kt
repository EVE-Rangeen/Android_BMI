package com.example.bmi

import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

/*
 * ============================================================================
 * MainActivity.kt（最简版 · 单文件 BMI 计算器）
 * ============================================================================
 * 【这是什么】
 *   这是“考试用”的最简版参考代码：只用一个 Activity + 一个简单布局，
 *   完成“输入身高体重 → 点按钮 → 直接在本页显示 BMI”，不跳转、不存库、不联网。
 *
 * 【它和完整版 MainActivity 的区别】
 *   完整版：输入 → 跳转到 ReportActivity 计算显示（还要存库、调 AI）。
 *   本版本：输入 → 在本页直接算、直接显示。所有逻辑都在这一个文件里。
 *
 * 【考试怎么用它】
 *   1. 把完整项目里其它文件删掉（见开发指南.md 的“分功能删除表”）；
 *   2. 用本文件的内容覆盖掉 app/src/main/java/com/example/bmi/MainActivity.kt；
 *   3. 用 docs/examples/activity_main_simple.xml 覆盖掉 res/layout/activity_main.xml；
 *   4. 按需改控件和计算逻辑即可，全程只碰这两个文件，不会报错。
 *
 * 【本文件依赖】只有 Android 自带的 AppCompatActivity + Toast，无任何项目内依赖。
 */

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // 把布局贴到屏幕上（见 activity_main_simple.xml）。
        setContentView(R.layout.activity_main)

        /*
         * findViewById<T>(R.id.xxx)：按布局里控件的 android:id 拿到控件对象。
         * R.id.heightET 对应布局里 <EditText android:id="@+id/heightET">。
         */
        val heightET = findViewById<EditText>(R.id.heightET)   // 身高输入框
        val weightET = findViewById<EditText>(R.id.weightET)   // 体重输入框
        val resultTV = findViewById<TextView>(R.id.resultTV)   // 结果文字
        val calcBtn = findViewById<Button>(R.id.calcBtn)       // 计算按钮

        // 给按钮绑定“点击事件”：点一下，执行 { } 里的代码。
        calcBtn.setOnClickListener {
            /*
             * 【从表单读取输入 + 转数字】
             *   heightET.text 是输入框内容（Editable），.toString() 变 String；
             *   .toDoubleOrNull() 把字符串转小数，转不了（空串/字母）就返回 null。
             *   用 OrNull 版本，就不会因为输入非法而崩溃。
             */
            val h = heightET.text.toString().toDoubleOrNull()
            val w = weightET.text.toString().toDoubleOrNull()

            /*
             * 校验：任一为空、或 <= 0 都算无效。
             * 无效时用 Toast（屏幕中间冒出的短提示）提醒，不计算。
             */
            if (h == null || w == null || h <= 0 || w <= 0) {
                Toast.makeText(this, "请输入有效的身高和体重", Toast.LENGTH_SHORT).show()
            } else {
                /*
                 * 【核心计算】BMI = 体重(kg) / 身高(m)^2
                 *   身高输入的是“厘米”，所以先 / 100 变成“米”。
                 */
                val bmi = w / ((h / 100) * (h / 100))

                /*
                 * 把结果显示到 TextView。
                 *   "%.2f".format(bmi) 表示“保留 2 位小数”。
                 *   例：bmi = 24.809... → 显示 "24.81"。
                 */
                resultTV.text = "你的 BMI 是 %.2f".format(bmi)
            }
        }
    }
}

/*
 * ============================================================================
 * 【改编模板】把上面改成“温度转换器”只改三处
 * ============================================================================
 * 考试如果是“输入一个温度 + 选单位(C/F/K)，点计算显示另外两个单位”，
 * 不用新建工程，直接照下面改：
 *
 * ① 布局（activity_main_simple.xml）改成：
 *    一个 EditText(id=temperatureET, inputType=numberDecimal)   ← 输入温度值
 *    一个 Spinner(id=unitSpinner, entries=@array/units)         ← 选择单位 C/F/K
 *    一个 Button(id=calcBtn)
 *    两个 TextView(id=result1TV, result2TV)                      ← 显示另两个单位
 *
 *    strings.xml 里加：<string-array name="units">
 *        <item>Celsius (℃)</item><item>Fahrenheit (℉)</item><item>Kelvin (K)</item>
 *    </string-array>
 *
 * ② 点击事件改成（核心就这几行）：
 *
 *    val t = temperatureET.text.toString().toDoubleOrNull()
 *    if (t == null) { Toast.makeText(this, "请输入温度", Toast.LENGTH_SHORT).show(); return@setOnClickListener }
 *    when (unitSpinner.selectedItemPosition) {
 *        0 -> {                       // 输入是摄氏度 C
 *            result1TV.text = "华氏度 = %.2f ℉".format(t * 9 / 5 + 32)   // C→F
 *            result2TV.text = "开尔文 = %.2f K".format(t + 273.15)         // C→K
 *        }
 *        1 -> {                       // 输入是华氏度 F
 *            result1TV.text = "摄氏度 = %.2f ℃".format((t - 32) * 5 / 9)  // F→C
 *            result2TV.text = "开尔文 = %.2f K".format((t - 32) * 5 / 9 + 273.15) // F→K
 *        }
 *        else -> {                    // 输入是开尔文 K
 *            result1TV.text = "摄氏度 = %.2f ℃".format(t - 273.15)         // K→C
 *            result2TV.text = "华氏度 = %.2f ℉".format((t - 273.15) * 9 / 5 + 32) // K→F
 *        }
 *    }
 *
 * ③ 换算公式（背下来）：
 *    C → F：F = C × 9/5 + 32
 *    F → C：C = (F − 32) × 5/9
 *    C → K：K = C + 273.15
 *    K → C：C = K − 273.15
 * ============================================================================
 */
