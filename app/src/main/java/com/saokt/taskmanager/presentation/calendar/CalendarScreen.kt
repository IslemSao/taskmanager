package com.saokt.taskmanager.presentation.calendar

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
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
import com.saokt.taskmanager.presentation.components.TaskItem
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.TextStyle
import java.util.Locale
import kotlin.math.abs

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CalendarScreen(
    navController: NavController,
    viewModel: CalendarViewModel = hiltViewModel(),
    onTaskClick: (String) -> Unit = { /* default no-op */ }
) {
    val state by viewModel.state.collectAsState()

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Calendar") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        CalendarContent(
            padding = padding,
            state = state,
            onPrev = { viewModel.previousMonth() },
            onNext = { viewModel.nextMonth() },
            onSelectDate = { viewModel.selectDate(it) },
            onTaskClick = onTaskClick,
            onToggleTask = { viewModel.toggleTaskCompletion(it) }
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
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(padding)
    ) {
        MonthHeader(
            month = state.currentMonth,
            onPrev = onPrev,
            onNext = onNext
        )

        Spacer(modifier = Modifier.height(8.dp))

        DayOfWeekHeader()

        Spacer(modifier = Modifier.height(8.dp))

        MonthGrid(
            month = state.currentMonth,
            selectedDate = state.selectedDate ?: LocalDate.now(),
            hasTasks = { date -> (state.tasksByDate[date]?.isNotEmpty() == true) },
            onSelectDate = onSelectDate
        )

        Spacer(modifier = Modifier.height(12.dp))

        Text(
            text = "Tasks on " + (state.selectedDate ?: LocalDate.now()).toString(),
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(horizontal = 16.dp)
        )

        Spacer(modifier = Modifier.height(8.dp))

        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            if (state.tasksForSelectedDate.isEmpty()) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                    ) {
                        Text(
                            text = "No tasks",
                            modifier = Modifier.padding(16.dp),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                items(state.tasksForSelectedDate.size) { index ->
                    val task = state.tasksForSelectedDate[index]
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

@Composable
private fun MonthHeader(month: YearMonth, onPrev: () -> Unit, onNext: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
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
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        days.forEach { d ->
            Text(
                text = d.getDisplayName(TextStyle.SHORT, Locale.getDefault()),
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
        for (d in 1..daysInMonth) add(month.atDay(d))
        while (size % 7 != 0) add(null)
    }

    LazyVerticalGrid(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        columns = GridCells.Fixed(7),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        userScrollEnabled = false,
        contentPadding = PaddingValues(bottom = 8.dp)
    ) {
        items(cells) { date ->
            DayCell(
                date = date,
                selected = date != null && date == selectedDate,
                hasTasks = date?.let { hasTasks(it) } == true,
                onClick = { date?.let(onSelectDate) }
            )
        }
    }
}

@Composable
private fun DayCell(
    date: LocalDate?,
    selected: Boolean,
    hasTasks: Boolean,
    onClick: () -> Unit
) {
    val shape = MaterialTheme.shapes.small
    Box(
        modifier = Modifier
            .clip(shape)
            .background(
                when {
                    selected -> MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                    date == LocalDate.now() -> MaterialTheme.colorScheme.secondary.copy(alpha = 0.10f)
                    else -> Color.Transparent
                }
            )
            .clickable(enabled = date != null, onClick = onClick)
            .height(42.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = date?.dayOfMonth?.toString() ?: "",
                textAlign = TextAlign.Center
            )
            if (hasTasks) {
                Box(
                    modifier = Modifier
                        .padding(top = 2.dp)
                        .height(4.dp)
                        .fillMaxWidth(0.2f)
                        .clip(shape)
                        .background(MaterialTheme.colorScheme.primary)
                )
            }
        }
    }
}
