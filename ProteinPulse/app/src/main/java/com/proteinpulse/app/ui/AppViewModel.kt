package com.proteinpulse.app.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.proteinpulse.app.ProteinApp
import com.proteinpulse.app.ai.AiException
import com.proteinpulse.app.ai.OfflineCoach
import com.proteinpulse.app.data.AnalysisSource
import com.proteinpulse.app.data.AppSettings
import com.proteinpulse.app.data.DailyReport
import com.proteinpulse.app.data.DayTotal
import com.proteinpulse.app.data.FoodItem
import com.proteinpulse.app.data.LocalFoodEstimator
import com.proteinpulse.app.data.MealAnalysis
import com.proteinpulse.app.data.MealEntry
import com.proteinpulse.app.data.Suggestion
import com.proteinpulse.app.data.SuggestionSet
import com.proteinpulse.app.work.ReportScheduler
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate

enum class Tab { TODAY, COACH, REPORT, SETTINGS }

sealed interface LogState {
    data object Idle : LogState
    data class Analyzing(val text: String) : LogState
    data class Logged(val analysis: MealAnalysis, val note: String?) : LogState
    data class Failed(val message: String) : LogState
}

sealed interface Async<out T> {
    data object Idle : Async<Nothing>
    data object Loading : Async<Nothing>
    data class Ready<T>(val value: T, val note: String? = null) : Async<T>
    data class Failed(val message: String) : Async<Nothing>
}

@OptIn(ExperimentalCoroutinesApi::class)
class AppViewModel(application: Application) : AndroidViewModel(application) {
    private val app = application as ProteinApp
    private val repo = app.meals
    private val store = app.settings

    private val _tab = MutableStateFlow(Tab.TODAY)
    val tab: StateFlow<Tab> = _tab.asStateFlow()

    private val today = MutableStateFlow(LocalDate.now().toEpochDay())

    val settings: StateFlow<AppSettings?> =
        store.settings.stateIn(viewModelScope, SharingStarted.Eagerly, null)

    val todayMeals: StateFlow<List<MealEntry>> =
        today.flatMapLatest { repo.mealsForDay(it) }
            .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    /** Last 7 days, oldest first, today last. */
    val week: StateFlow<List<DayTotal>> =
        today.flatMapLatest { repo.dailyTotals(it - 6, it) }
            .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    /** Last 60 days, used for the streak. */
    val history: StateFlow<List<DayTotal>> =
        today.flatMapLatest { repo.dailyTotals(it - 59, it) }
            .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val recent: StateFlow<List<MealEntry>> =
        repo.recentDescriptions().stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    private val _log = MutableStateFlow<LogState>(LogState.Idle)
    val log: StateFlow<LogState> = _log.asStateFlow()

    private val _suggestions = MutableStateFlow<Async<SuggestionSet>>(Async.Idle)
    val suggestions: StateFlow<Async<SuggestionSet>> = _suggestions.asStateFlow()

    private val _report = MutableStateFlow<Async<DailyReport>>(Async.Idle)
    val report: StateFlow<Async<DailyReport>> = _report.asStateFlow()

    private val _connection = MutableStateFlow<Async<String>>(Async.Idle)
    val connection: StateFlow<Async<String>> = _connection.asStateFlow()

    /** Bumped each time a meal pushes the day over the goal, to fire the celebration. */
    private val _celebrate = MutableStateFlow(0)
    val celebrate: StateFlow<Int> = _celebrate.asStateFlow()

    private var lastDeleted: MealEntry? = null

    fun selectTab(tab: Tab) { _tab.value = tab }

    fun refreshDay() {
        val now = LocalDate.now().toEpochDay()
        if (today.value != now) {
            today.value = now
            _report.value = Async.Idle
            _suggestions.value = Async.Idle
        }
    }

    fun logMeal(text: String) {
        val input = text.trim()
        if (input.isEmpty() || _log.value is LogState.Analyzing) return
        _log.value = LogState.Analyzing(input)
        viewModelScope.launch {
            val s = store.current()
            var note: String? = null
            val analysis: MealAnalysis? = if (s.hasAiKey) {
                try {
                    app.ai.analyzeMeal(s, input)
                } catch (e: AiException) {
                    val offline = LocalFoodEstimator.estimate(input)
                    if (offline == null) {
                        _log.value = LogState.Failed(e.message ?: "Something went wrong")
                        return@launch
                    }
                    note = "AI unavailable (${e.message}) — used an offline estimate."
                    offline
                }
            } else {
                LocalFoodEstimator.estimate(input).also {
                    if (it != null) note = "Offline estimate. Add an API key in Settings for AI accuracy."
                }
            }
            if (analysis == null) {
                _log.value = LogState.Failed(
                    "I couldn't recognise that food offline. Add an API key in Settings for AI analysis, " +
                        "or try something like \"3 eggs and 2 rotis\"."
                )
                return@launch
            }
            saveAndCelebrate(input, analysis)
            _log.value = LogState.Logged(analysis, note)
        }
    }

    fun relog(entry: MealEntry) {
        viewModelScope.launch {
            val analysis = MealAnalysis(entry.items, entry.tip, entry.source)
            saveAndCelebrate(entry.description, analysis)
            _log.value = LogState.Logged(analysis, "Logged again from your recent meals.")
        }
    }

    fun logSuggestion(s: Suggestion) {
        viewModelScope.launch {
            val analysis = MealAnalysis(
                listOf(FoodItem(s.food, "1 serving", s.proteinG, s.calories)),
                s.why,
                AnalysisSource.AI,
            )
            saveAndCelebrate(s.food, analysis)
            _log.value = LogState.Logged(analysis, "Added from your coach's suggestions.")
        }
    }

    private suspend fun saveAndCelebrate(description: String, analysis: MealAnalysis) {
        val goal = store.current().goalG
        val before = todayMeals.value.sumOf { it.proteinG }
        repo.insert(description, analysis)
        if (before < goal && before + analysis.proteinG >= goal) _celebrate.value++
        _report.value = Async.Idle
    }

    fun dismissLog() { _log.value = LogState.Idle }

    fun delete(entry: MealEntry) {
        lastDeleted = entry
        viewModelScope.launch { repo.delete(entry.id) }
    }

    fun undoDelete() {
        val entry = lastDeleted ?: return
        lastDeleted = null
        viewModelScope.launch { repo.restore(entry) }
    }

    fun loadSuggestions() {
        if (_suggestions.value is Async.Loading) return
        _suggestions.value = Async.Loading
        viewModelScope.launch {
            val s = store.current()
            val consumed = todayMeals.value.sumOf { it.proteinG }
            _suggestions.value = if (s.hasAiKey) {
                try {
                    Async.Ready(app.ai.suggestions(s, consumed, todayMeals.value))
                } catch (e: AiException) {
                    Async.Ready(OfflineCoach.suggestions(s.diet, s.goalG, consumed), "AI unavailable: ${e.message}")
                }
            } else {
                Async.Ready(OfflineCoach.suggestions(s.diet, s.goalG, consumed))
            }
        }
    }

    fun generateReport() {
        if (_report.value is Async.Loading) return
        _report.value = Async.Loading
        viewModelScope.launch {
            val s = store.current()
            val meals = todayMeals.value
            _report.value = if (s.hasAiKey && meals.isNotEmpty()) {
                try {
                    Async.Ready(app.ai.dailyReport(s, meals, week.value))
                } catch (e: AiException) {
                    Async.Ready(OfflineCoach.report(s.goalG, meals, week.value), "AI unavailable: ${e.message}")
                }
            } else {
                Async.Ready(OfflineCoach.report(s.goalG, meals, week.value))
            }
        }
    }

    fun updateSettings(rescheduleReport: Boolean = false, transform: (AppSettings) -> AppSettings) {
        viewModelScope.launch {
            store.update(transform)
            if (rescheduleReport) ReportScheduler.schedule(app, store.current(), replace = true)
        }
        _connection.value = Async.Idle
    }

    fun testConnection() {
        _connection.value = Async.Loading
        viewModelScope.launch {
            _connection.value = try {
                Async.Ready(app.ai.testConnection(store.current()))
            } catch (e: AiException) {
                Async.Failed(e.message ?: "Connection failed")
            }
        }
    }
}
