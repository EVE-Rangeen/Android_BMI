package com.example.bmi.ui.trend

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.example.bmi.data.BmiHistoryRepository
import com.example.bmi.data.BmiRecord

/**
 * Loads the last month of BMI records for [TrendActivity]. The query is done
 * off the ViewModel's state, so rotating the trend screen does not lose data.
 */
class TrendViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = BmiHistoryRepository(application)

    private val _records = MutableLiveData<List<BmiRecord>>()
    val records: LiveData<List<BmiRecord>> = _records

    fun load() {
        _records.value = repository.recentRecords(BmiHistoryRepository.DEFAULT_DAYS)
    }
}
