package com.saokt.taskmanager.presentation.calendar

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.saokt.taskmanager.domain.model.Task
import com.saokt.taskmanager.domain.usecase.task.GetTasksUseCase
import com.saokt.taskmanager.domain.usecase.task.ToggleTaskComplitionUseCase
import com.saokt.taskmanager.domain.usecase.user.GetCurrentUserUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
import java.util.Date
import javax.inject.Inject

@HiltViewModel
class CalendarViewModel @Inject constructor(
    private val getTasksUseCase: GetTasksUseCase,
    private val toggleTaskComplitionUseCase: ToggleTaskComplitionUseCase,
    private val getCurrentUserUseCase: GetCurrentUserUseCase
) : ViewModel() {

    private val _state = MutableStateFlow(CalendarState())
    val state: StateFlow<CalendarState> = _state.asStateFlow()

    init {
        // Observe current user
        viewModelScope.launch {
            getCurrentUserUseCase()
                .catch { }
                .collect { user ->
                    _state.update { st ->
                        val newSt = st.copy(currentUserId = user?.id)
                        recompute(newSt)
                    }
                }
        }
        // Observe tasks
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true) }
            getTasksUseCase()
                .catch { e ->
                    _state.update { it.copy(isLoading = false, error = e.message) }
                }
                .collect { tasks ->
                    _state.update { st ->
                        val newSt = st.copy(isLoading = false, tasks = tasks)
                        recompute(newSt)
                    }
                }
        }
    }

    fun selectDate(date: LocalDate) {
        _state.update { st ->
            val updated = st.copy(selectedDate = date)
            recompute(updated)
        }
    }

    fun nextMonth() {
        _state.update { st -> st.copy(currentMonth = st.currentMonth.plusMonths(1)) }
    }

    fun previousMonth() {
        _state.update { st -> st.copy(currentMonth = st.currentMonth.minusMonths(1)) }
    }

    fun clearError() {
        _state.update { it.copy(error = null) }
    }

    fun toggleTaskCompletion(task: Task) {
        viewModelScope.launch {
            toggleTaskComplitionUseCase(task)
        }
    }

    private fun recompute(base: CalendarState) : CalendarState {
        val selected = base.selectedDate ?: LocalDate.now()
    val byDate = base.tasks
            .filter { it.dueDate != null }
            .groupBy { it.dueDate!!.toLocalDate() }
        val updated = base.copy(
            selectedDate = selected,
            tasksByDate = byDate,
            tasksForSelectedDate = byDate[selected].orEmpty()
        )
        _state.value = updated
        return updated
    }
}

data class CalendarState(
    val isLoading: Boolean = false,
    val error: String? = null,
    val currentMonth: YearMonth = YearMonth.now(),
    val selectedDate: LocalDate? = null,
    val tasks: List<Task> = emptyList(),
    val tasksByDate: Map<LocalDate, List<Task>> = emptyMap(),
    val tasksForSelectedDate: List<Task> = emptyList(),
    val currentUserId: String? = null
)

private fun Date.toLocalDate(): LocalDate =
    this.toInstant().atZone(ZoneId.systemDefault()).toLocalDate()
