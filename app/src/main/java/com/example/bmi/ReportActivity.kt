package com.example.bmi

import android.os.Bundle
import android.widget.Button
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.os.ConfigurationCompat
import androidx.core.view.isVisible
import com.example.bmi.data.AppLanguage
import com.example.bmi.data.BmiProfile
import com.example.bmi.data.Gender
import com.example.bmi.ui.report.AdviceRequest
import com.example.bmi.ui.report.AdviceUiState
import com.example.bmi.ui.report.ReportViewModel
import java.text.DecimalFormat
import java.util.Locale

/**
 * View layer of the report screen (MVVM).
 *
 * It only reads the intent extras, renders the local BMI result and observes
 * [ReportViewModel.adviceState]; any decision about the LLM request is made in
 * the ViewModel.
 */
class ReportActivity : AppCompatActivity() {

    private val viewModel: ReportViewModel by viewModels()

    private lateinit var resultTV: TextView
    private lateinit var adviceTV: TextView
    private lateinit var resultImage: ImageView
    private lateinit var llmAdviceTV: TextView
    private lateinit var llmProgress: ProgressBar
    private lateinit var llmRetry: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_report)

        resultTV = findViewById(R.id.report_result)
        adviceTV = findViewById(R.id.report_advice)
        resultImage = findViewById(R.id.report_image)
        llmAdviceTV = findViewById(R.id.report_llm_advice)
        llmProgress = findViewById(R.id.report_llm_progress)
        llmRetry = findViewById(R.id.report_llm_retry)

        val profile = readProfile() ?: return
        showLocalResult(profile)

        llmRetry.setOnClickListener { viewModel.retry() }
        viewModel.adviceState.observe(this) { state -> renderLlmState(state) }

        // The ViewModel keeps its state across rotations, so only the first
        // creation of the screen triggers a request.
        if (!viewModel.hasRequested) {
            viewModel.loadAdvice(AdviceRequest(profile, currentLanguage()))
        }
    }

    /** Reads the extras sent by [MainActivity] and turns them into a [BmiProfile]. */
    private fun readProfile(): BmiProfile? {
        val bundle = intent.extras
        if (bundle == null) {
            finish()
            return null
        }
        val heightMeters = bundle.getString("height")!!.toDouble() / 100
        val weightKg = bundle.getString("weight")!!.toDouble()
        val age = bundle.getString("age")!!.toInt()
        val gender = Gender.fromSpinnerPosition(bundle.getInt("gender", 0))
        return BmiProfile(
            bmi = weightKg / (heightMeters * heightMeters),
            age = age,
            gender = gender
        )
    }

    /** The BMI advice that was already part of the app, computed offline. */
    private fun showLocalResult(profile: BmiProfile) {
        val bmi = profile.bmi
        resultTV.text = getString(R.string.bmi_result) + " " + DecimalFormat("0.00").format(bmi)

        val limits = localLimits(profile)
        when {
            bmi < limits.first -> {
                adviceTV.setText(R.string.advice_light)
                resultImage.setImageResource(R.drawable.bot_thin)
            }
            bmi >= limits.second -> {
                adviceTV.setText(R.string.advice_heavy)
                resultImage.setImageResource(R.drawable.bot_fat)
            }
            else -> {
                adviceTV.setText(R.string.advice_average)
                resultImage.setImageResource(R.drawable.bot_fit)
            }
        }
    }

    private fun renderLlmState(state: AdviceUiState) {
        when (state) {
            AdviceUiState.Idle -> {
                llmProgress.isVisible = false
                llmAdviceTV.text = ""
                llmRetry.isVisible = false
            }
            AdviceUiState.Loading -> {
                llmProgress.isVisible = true
                llmAdviceTV.setText(R.string.llm_loading)
                llmRetry.isVisible = false
            }
            is AdviceUiState.Success -> {
                llmProgress.isVisible = false
                llmAdviceTV.text = state.text
                llmRetry.isVisible = false
            }
            is AdviceUiState.Error -> {
                llmProgress.isVisible = false
                llmAdviceTV.text = getString(state.reason.messageRes)
                llmRetry.isVisible = true
            }
        }
    }

    /** The app language, which decides the language the LLM has to answer in. */
    private fun currentLanguage(): AppLanguage {
        val locale = ConfigurationCompat.getLocales(resources.configuration).get(0)
            ?: Locale.getDefault()
        return AppLanguage.fromLanguageTag(locale.language)
    }

    // ---------- BMI cut points ----------

    private fun localLimits(profile: BmiProfile): Pair<Double, Double> =
        if (profile.age in 6..18) {
            if (profile.gender == Gender.MALE) boyLimits(profile.age) else girlLimits(profile.age)
        } else {
            18.5 to 22.9
        }

    // BMI-for-age cut points for boys, taken from the percentile table.
    // first  = 5th percentile  (below it: underweight)
    // second = 90th percentile (at or above it: overweight)
    private fun boyLimits(age: Int): Pair<Double, Double> = when (age) {
        6 -> 13.2 to 18.9
        7 -> 13.4 to 19.9
        8 -> 13.7 to 21.0
        9 -> 13.9 to 21.9
        10 -> 14.2 to 22.8
        11 -> 14.6 to 23.7
        12 -> 14.9 to 24.4
        13 -> 15.2 to 25.1
        14 -> 15.5 to 25.6
        15 -> 15.9 to 26.2
        16 -> 16.2 to 26.6
        17 -> 16.4 to 27.1
        else -> 16.7 to 27.5
    }

    // Same table for girls.
    private fun girlLimits(age: Int): Pair<Double, Double> = when (age) {
        6 -> 12.9 to 18.4
        7 -> 13.2 to 19.2
        8 -> 13.5 to 20.2
        9 -> 13.8 to 21.1
        10 -> 14.2 to 22.0
        11 -> 14.5 to 22.8
        12 -> 14.9 to 23.5
        13 -> 15.3 to 24.1
        14 -> 15.6 to 24.7
        15 -> 15.9 to 25.1
        16 -> 16.2 to 25.5
        17 -> 16.4 to 25.8
        else -> 16.6 to 26.0
    }
}
