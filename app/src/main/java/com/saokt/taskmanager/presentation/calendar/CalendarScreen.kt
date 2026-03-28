package com.saokt.taskmanager.presentation.calendar

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.saokt.taskmanager.presentation.components.AppTopBar
import com.saokt.taskmanager.presentation.components.EmptyStateCard
import com.saokt.taskmanager.presentation.components.HeroCard
import com.saokt.taskmanager.presentation.components.SectionCard
import com.saokt.taskmanager.presentation.components.TaskItem
import com.saokt.taskmanager.ui.theme.AppTheme
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.TextStyle
import java.util.Locale

@Composable
fun CalendarScreen(
    navController: NavController,
    viewModel: CalendarViewModel = hiltViewModel(),
    onTaskClick: (String) -> Unit = {}
) {
    val state by viewModel.state.collectAsState()

    Scaffold(
        topBar = {
            AppTopBar(
                title = "Calendar",
                subtitle = "See what is scheduled and what still needs a plan",
                onBack = null
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        CalendarContent(
            padding = padding,
            state = state,
            onPrev = viewModel::previousMonth,
            onNext = viewModel::nextMonth,
            onSelectDate = viewModel::selectDate,
            onTaskClick = onTaskClick,
            onToggleTask = viewModel::toggleTaskCompletion
        )
    }
}

@Composable
private fun CalendarContent(
    padding: PaddingValues,
    state: CalendarState,
    onPrev: () -> Unit,
    onNext: () -> Unit,
    onSelectDate: (LocalDate) -> Unit,
    onTaskClick: (String) -> Unit,
    onToggleTask: (com.saokt.taskmanager.domain.model.Task) -> Unit
) {
    val selectedDate = state.selectedDate ?: LocalDate.now()

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(padding),
        contentPadding = PaddingValues(
            start = AppTheme.screenPadding,
            end = AppTheme.screenPadding,
            top = 8.dp,
            bottom = 32.dp
        ),
        verticalArrangement = Arrangement.spacedBy(AppTheme.sectionSpacing)
    ) {
        item {
            HeroCard(
                eyebrow = "Planner",
                title = state.currentMonth.month.getDisplayName(TextStyle.FULL, Locale.getDefault()) + " " + state.currentMonth.year,
                body = "Browse the month, select a day, and review scheduled tasks without switching context.",
                stats = listOf(
                    "Selected" to selectedDate.dayOfMonth.toString(),
                    "Tasks" to state.tasksForSelectedDate.size.toString()
                )
            )
        }

        item {
            SectionCard(title = "Month view") {
                MonthHeader(month = state.currentMonth, onPrev = onPrev, onNext = onNext)
                DayOfWeekHeader()
                MonthGrid(
                    month = state.currentMonth,
                    selectedDate = selectedDate,
                    hasTasks = { date -> state.tasksByDate[date]?.isNotEmpty() == true },
                    onSelectDate = onSelectDate
                )
            }
        }

        item {
            SectionCard(title = "Tasks on $selectedDate") {
                if (state.tasksForSelectedDate.isEmpty()) {
                    EmptyStateCard(
                        title = "No tasks planned",
                        body = "This day is clear right now. Add or schedule tasks to build your plan.",
                        icon = Icons.Default.DateRange
                    )
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        state.tasksForSelectedDate.forEach { task ->
                            TaskItem(
                                task = task,
                                onClick = { onTaskClick(task.id) },
                                onCompletionToggle = { onToggleTask(task) }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MonthHeader(month: YearMonth, onPrev: () -> Unit, onNext: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = onPrev) {
            Icon(Icons.Default.ChevronLeft, contentDescription = "Previous month")
        }
        Text(
            text = month.month.getDisplayName(TextStyle.FULL, Locale.getDefault()) + " " + month.year,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.SemiBold
        )
        IconButton(onClick = onNext) {
            Icon(Icons.Default.ChevronRight, contentDescription = "Next month")
        }
    }
}

@Composable
private fun DayOfWeekHeader() {
    val days = listOf(
        DayOfWeek.SUNDAY,
        DayOfWeek.MONDAY,
        DayOfWeek.TUESDAY,
        DayOfWeek.WEDNESDAY,
        DayOfWeek.THURSDAY,
        DayOfWeek.FRIDAY,
        DayOfWeek.SATURDAY
    )
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        days.forEach { day ->
            Text(
                text = day.getDisplayName(TextStyle.SHORT, Locale.getDefault()),
                modifier = Modifier.weight(1f),
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun MonthGrid(
    month: YearMonth,
    selectedDate: LocalDate,
    hasTasks: (LocalDate) -> Boolean,
    onSelectDate: (LocalDate) -> Unit
) {
    val firstOfMonth = month.atDay(1)
    val firstDayOfWeekIndex = when (firstOfMonth.dayOfWeek) {
        DayOfWeek.SUNDAY -> 0
        else -> firstOfMonth.dayOfWeek.value
    }
    val daysInMonth = month.lengthOfMonth()
    val cells: List<LocalDate?> = buildList {
        repeat(firstDayOfWeekIndex) { add(null) }
        for (day in 1..daysInMonth) add(month.atDay(day))
        while (size % 7 != 0) add(null)
    }
    val rows = cells.chunked(7)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        rows.forEach { week ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                week.forEach { date ->
                    if (date == null) {
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp)
                        )
                    } else {
                        DayCell(
                            modifier = Modifier.weight(1f),
                            date = date,
                            selected = date == selectedDate,
                            hasTasks = hasTasks(date),
                            onClick = { onSelectDate(date) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun DayCell(
    modifier: Modifier = Modifier,
    date: LocalDate?,
    selected: Boolean,
    hasTasks: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.medium)
            .background(
                when {
                    selected -> MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                    date == LocalDate.now() -> MaterialTheme.colorScheme.secondary.copy(alpha = 0.10f)
                    else -> Color.Transparent
                }
            )
            .clickable(enabled = date != null, onClick = onClick)
            .height(48.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(text = date?.dayOfMonth?.toString() ?: "", textAlign = TextAlign.Center)
            if (hasTasks) {
                Box(
                    modifier = Modifier
                        .padding(top = 3.dp)
                        .height(4.dp)
                        .fillMaxWidth(0.22f)
                        .clip(MaterialTheme.shapes.small)
                        .background(MaterialTheme.colorScheme.primary)
                )
            }
        }
    }
}
