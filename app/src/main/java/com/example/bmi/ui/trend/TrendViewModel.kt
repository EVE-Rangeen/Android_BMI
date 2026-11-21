package com.example.bmi.ui.trend

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.example.bmi.data.BmiHistoryRepository
import com.example.bmi.data.BmiRecord

/*
 * ============================================================================
 * TrendViewModel.kt —— 趋势页的 ViewModel
 * ============================================================================
 * 【这个文件是干什么的】
 *   从数据库读最近 30 天记录，放到 LiveData 里供 TrendActivity 观察。
 *
 * 【为什么用 AndroidViewModel 而不是普通 ViewModel】
 *   因为要拿到 Application（上下文）去创建 BmiHistoryRepository（数据库），
 *   AndroidViewModel 构造时会把 Application 传给我们。
 *
 * 【数据流】
 *   TrendActivity.load() → 本类 load() → Repository.recentRecords(30)
 *                        → 结果放进 records（LiveData）→ Activity 收到并画图。
 *
 * ────────────────────────────────────────────────────────────────────────────
 * 【依赖关系】（考试删文件前先看这里）
 *   本文件依赖：BmiHistoryRepository、BmiRecord。
 *   谁依赖本文件：TrendActivity。
 *   删除指引：TrendViewModel 只服务于“趋势页读数据库”，属于【趋势/历史】功能；
 *             砍掉趋势时连同本文件一起删。
 * ────────────────────────────────────────────────────────────────────────────
 */
class TrendViewModel(application: Application) : AndroidViewModel(application) {

    // 用 Application 创建仓库（仓库内部会 new 一个 SQLite 助手）。
    private val repository = BmiHistoryRepository(application)

    // 内部可写、外部只读的 LiveData。
    private val _records = MutableLiveData<List<BmiRecord>>()
    val records: LiveData<List<BmiRecord>> = _records

    /**
     * 加载最近 30 天的记录。
     */
    fun load() {
        _records.value = repository.recentRecords(BmiHistoryRepository.DEFAULT_DAYS)
    }
}
