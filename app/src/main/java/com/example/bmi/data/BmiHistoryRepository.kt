package com.example.bmi.data

import android.content.Context

/*
 * ============================================================================
 * BmiHistoryRepository.kt —— 历史记录的“统一入口”
 * ============================================================================
 * 【这个文件是干什么的】
 *   它是一个“中间人”（Repository，仓库）。界面层（Activity/ViewModel）想读、写
 *   BMI 历史记录时，不直接碰 SQLite，而是通过这个类。
 *
 * 【为什么要加这一层（Repository 模式）】
 *   1. 把“数据从哪来、怎么存”的细节藏起来，界面只关心“存一条记录 / 取最近 30 天”。
 *   2. 以后如果想换存储方式（比如从 SQLite 换成网络、换成文件），
 *      只需要改这个类，界面层代码一行都不用动。
 *   3. 便于测试：可以传一个“假的 Repository”给 ViewModel，不碰真数据库。
 *
 * 【谁在用这个文件】
 *   - ReportActivity：saveRecord(...) 保存当天记录；
 *   - TrendViewModel：recentRecords(...) 读取最近 30 天。
 *
 * ────────────────────────────────────────────────────────────────────────────
 * 【依赖关系】（考试删文件前先看这里）
 *   本文件依赖：BmiDbHelper、BmiRecord。
 *   谁依赖本文件：ReportActivity（存记录）、TrendViewModel（读记录）。
 *   删除指引：本文件是“SQLite 历史”的对外入口，属于【趋势/历史】功能；
 *             砍掉该功能时和 BmiDbHelper 等一起删。
 * ────────────────────────────────────────────────────────────────────────────
 */
class BmiHistoryRepository(context: Context) {

    /** 真正干活的 SQLite 助手类。 */
    private val dbHelper = BmiDbHelper(context)

    /**
     * 保存一条 BMI 记录（同一天会覆盖为最新一条）。
     */
    fun saveRecord(record: BmiRecord) {
        dbHelper.save(record)
    }

    /**
     * 读取最近 [days] 天的记录，默认 30 天。
     */
    fun recentRecords(days: Int = DEFAULT_DAYS): List<BmiRecord> =
        dbHelper.recentRecords(days)

    companion object {
        /** 趋势页默认展示最近 30 天。 */
        const val DEFAULT_DAYS = 30
    }
}
