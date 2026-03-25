package com.saokt.taskmanager.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.LocalDensity
import com.saokt.taskmanager.domain.model.Task
import com.saokt.taskmanager.domain.model.TaskStatus
import com.saokt.taskmanager.domain.model.TimelineEdge
import com.saokt.taskmanager.domain.model.TimelineItem
import com.saokt.taskmanager.domain.model.TimelineRange
import com.saokt.taskmanager.domain.model.TimelineZoom
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import kotlin.math.roundToInt

@Composable
fun TaskTimeline(
    timelineRange: TimelineRange,
    items: List<TimelineItem>,
    unscheduledTasks: List<Task>,
    zoom: TimelineZoom,
    onZoomChange: (TimelineZoom) -> Unit,
    onShiftBackward: () -> Unit,
    onShiftForward: () -> Unit,
    onJumpToToday: () -> Unit,
    onTaskClick: (String) -> Unit,
    onPlanTask: (Task) -> Unit,
    onRescheduleTask: (Task, Long) -> Unit,
    onResizeTask: (Task, TimelineEdge, Long) -> Unit,
    secondaryLabel: (Task) -> String?,
    modifier: Modifier = Modifier
) {
    val horizontalScroll = rememberScrollState()
    val cellWidth = zoom.cellWidth()
    val chartWidth = cellWidth * timelineRange.totalDays
    val headerFormatter = remember(zoom) {
        when (zoom) {
            TimelineZoom.DAY -> DateTimeFormatter.ofPattern("EEE\ndd")
            TimelineZoom.WEEK -> DateTimeFormatter.ofPattern("dd MMM")
            TimelineZoom.MONTH -> DateTimeFormatter.ofPattern("dd")
        }
    }

    Column(
        modifier = modifier.fillMaxSize()
    ) {
        TimelineToolbar(
            zoom = zoom,
            onZoomChange = onZoomChange,
            onShiftBackward = onShiftBackward,
            onShiftForward = onShiftForward,
            onJumpToToday = onJumpToToday
        )

        TimelineHeader(
            range = timelineRange,
            cellWidth = cellWidth,
            chartWidth = chartWidth,
            horizontalScroll = horizontalScroll,
            formatter = headerFormatter
        )

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(items, key = { it.task.id }) { item ->
                TimelineRow(
                    item = item,
                    cellWidth = cellWidth,
                    chartWidth = chartWidth,
                    range = timelineRange,
                    horizontalScroll = horizontalScroll,
                    secondaryLabel = secondaryLabel(item.task),
                    onTaskClick = { onTaskClick(item.task.id) },
                    onRescheduleTask = { onRescheduleTask(item.task, it) },
                    onResizeTask = { edge, delta -> onResizeTask(item.task, edge, delta) }
                )
            }

            if (unscheduledTasks.isNotEmpty()) {
                item("unscheduled_header") {
                    Text(
                        text = "Unscheduled",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }
                items(unscheduledTasks, key = { "unscheduled-${it.id}" }) { task ->
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f)
                        )
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = task.title,
                                    style = MaterialTheme.typography.titleSmall
                                )
                                secondaryLabel(task)?.let { label ->
                                    Text(
                                        text = label,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                OutlinedButton(onClick = { onTaskClick(task.id) }) {
                                    Text("Open")
                                }
                                Button(onClick = { onPlanTask(task) }) {
                                    Text("Plan Today")
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TimelineToolbar(
    zoom: TimelineZoom,
    onZoomChange: (TimelineZoom) -> Unit,
    onShiftBackward: () -> Unit,
    onShiftForward: () -> Unit,
    onJumpToToday: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            OutlinedButton(onClick = onShiftBackward) { Text("Earlier") }
            OutlinedButton(onClick = onJumpToToday) { Text("Today") }
            OutlinedButton(onClick = onShiftForward) { Text("Later") }
        }

        SingleChoiceSegmentedButtonRow {
            TimelineZoom.entries.forEachIndexed { index, option ->
                SegmentedButton(
                    selected = zoom == option,
                    onClick = { onZoomChange(option) },
                    shape = SegmentedButtonDefaults.itemShape(index = index, count = TimelineZoom.entries.size)
                ) {
                    Text(option.name.lowercase().replaceFirstChar(Char::uppercase))
                }
            }
        }
    }
}

@Composable
private fun TimelineHeader(
    range: TimelineRange,
    cellWidth: Dp,
    chartWidth: Dp,
    horizontalScroll: androidx.compose.foundation.ScrollState,
    formatter: DateTimeFormatter
) {
    val today = remember { LocalDate.now() }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
    ) {
        Spacer(modifier = Modifier.width(180.dp))
        Row(
            modifier = Modifier
                .horizontalScroll(horizontalScroll)
                .width(chartWidth)
        ) {
            repeat(range.totalDays) { offset ->
                val day = range.start.plusDays(offset.toLong())
                val isToday = day == today
                Box(
                    modifier = Modifier
                        .width(cellWidth)
                        .height(52.dp)
                        .padding(horizontal = 2.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(
                            if (isToday) {
                                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.8f)
                            } else {
                                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
                            }
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = day.format(formatter),
                        style = MaterialTheme.typography.labelMedium,
                        color = if (isToday) {
                            MaterialTheme.colorScheme.onPrimaryContainer
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun TimelineRow(
    item: TimelineItem,
    cellWidth: Dp,
    chartWidth: Dp,
    range: TimelineRange,
    horizontalScroll: androidx.compose.foundation.ScrollState,
    secondaryLabel: String?,
    onTaskClick: () -> Unit,
    onRescheduleTask: (Long) -> Unit,
    onResizeTask: (TimelineEdge, Long) -> Unit
) {
    val dayOffset = java.time.temporal.ChronoUnit.DAYS.between(range.start, item.start).toInt()
    val barWidth = if (item.isMilestone) 18.dp else cellWidth * item.spanDays
    val barOffset = cellWidth * dayOffset

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Top
    ) {
        Card(
            modifier = Modifier
                .width(180.dp)
                .padding(end = 12.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Text(
                    text = item.task.title,
                    style = MaterialTheme.typography.titleSmall,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                secondaryLabel?.let {
                    Text(
                        text = it,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                if (!item.canEditSchedule) {
                    FilterChip(
                        selected = false,
                        onClick = {},
                        label = { Text("Read only") },
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }
            }
        }

        Box(
            modifier = Modifier
                .heightIn(min = 68.dp)
                .fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .horizontalScroll(horizontalScroll)
                    .width(chartWidth)
            ) {
                repeat(range.totalDays) {
                    Box(
                        modifier = Modifier
                            .width(cellWidth)
                            .height(68.dp)
                            .padding(horizontal = 2.dp)
                            .border(
                                width = 1.dp,
                                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f),
                                shape = RoundedCornerShape(12.dp)
                            )
                    )
                }
            }

            Row(
                modifier = Modifier
                    .horizontalScroll(horizontalScroll)
                    .width(chartWidth)
                    .padding(top = 14.dp)
            ) {
                Box(modifier = Modifier.fillMaxWidth()) {
                    if (item.isMilestone) {
                        MilestoneNode(
                            item = item,
                            cellWidth = cellWidth,
                            barOffset = barOffset,
                            onTaskClick = onTaskClick,
                            onRescheduleTask = onRescheduleTask
                        )
                    } else {
                        TimelineBar(
                            item = item,
                            cellWidth = cellWidth,
                            barOffset = barOffset,
                            barWidth = barWidth,
                            onTaskClick = onTaskClick,
                            onRescheduleTask = onRescheduleTask,
                            onResizeTask = onResizeTask
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun TimelineBar(
    item: TimelineItem,
    cellWidth: Dp,
    barOffset: Dp,
    barWidth: Dp,
    onTaskClick: () -> Unit,
    onRescheduleTask: (Long) -> Unit,
    onResizeTask: (TimelineEdge, Long) -> Unit
) {
    val density = LocalDensity.current
    val cellWidthPx = with(density) { cellWidth.toPx() }
    Box(
        modifier = Modifier
            .padding(start = barOffset)
            .width(barWidth)
            .height(38.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(statusColor(item.task.status))
            .testTag("timeline_bar_${item.task.id}")
            .clickable(onClick = onTaskClick)
            .pointerInput(item.task.id, cellWidthPx, item.canEditSchedule) {
                if (!item.canEditSchedule) return@pointerInput
                var totalDrag = 0f
                detectDragGestures(
                    onDrag = { change, dragAmount ->
                        change.consume()
                        totalDrag += dragAmount.x
                    },
                    onDragEnd = {
                        val deltaDays = (totalDrag / cellWidthPx).roundToInt().toLong()
                        if (deltaDays != 0L) onRescheduleTask(deltaDays)
                    }
                )
            }
    ) {
        Row(
            modifier = Modifier.fillMaxSize(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            ResizeHandle(
                enabled = item.canEditSchedule,
                edge = TimelineEdge.START,
                cellWidthPx = cellWidthPx,
                onResizeTask = onResizeTask
            )
            Text(
                text = item.task.title,
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 8.dp)
            )
            ResizeHandle(
                enabled = item.canEditSchedule,
                edge = TimelineEdge.END,
                cellWidthPx = cellWidthPx,
                onResizeTask = onResizeTask
            )
        }
    }
}

@Composable
private fun ResizeHandle(
    enabled: Boolean,
    edge: TimelineEdge,
    cellWidthPx: Float,
    onResizeTask: (TimelineEdge, Long) -> Unit
) {
    Box(
        modifier = Modifier
            .width(14.dp)
            .fillMaxHeight()
            .background(
                if (enabled) MaterialTheme.colorScheme.scrim.copy(alpha = 0.2f) else Color.Transparent
            )
            .pointerInput(enabled, edge, cellWidthPx) {
                if (!enabled) return@pointerInput
                var totalDrag = 0f
                detectDragGestures(
                    onDrag = { change, dragAmount ->
                        change.consume()
                        totalDrag += dragAmount.x
                    },
                    onDragEnd = {
                        val deltaDays = (totalDrag / cellWidthPx).roundToInt().toLong()
                        if (deltaDays != 0L) onResizeTask(edge, deltaDays)
                    }
                )
            }
    )
}

@Composable
private fun MilestoneNode(
    item: TimelineItem,
    cellWidth: Dp,
    barOffset: Dp,
    onTaskClick: () -> Unit,
    onRescheduleTask: (Long) -> Unit
) {
    val density = LocalDensity.current
    val cellWidthPx = with(density) { cellWidth.toPx() }
    Box(
        modifier = Modifier
            .padding(start = barOffset)
            .size(20.dp)
            .rotate(45f)
            .clip(RoundedCornerShape(6.dp))
            .background(statusColor(item.task.status))
            .testTag("timeline_milestone_${item.task.id}")
            .clickable(onClick = onTaskClick)
            .pointerInput(item.task.id, item.canEditSchedule, cellWidthPx) {
                if (!item.canEditSchedule) return@pointerInput
                var totalDrag = 0f
                detectDragGestures(
                    onDrag = { change, dragAmount ->
                        change.consume()
                        totalDrag += dragAmount.x
                    },
                    onDragEnd = {
                        val deltaDays = (totalDrag / cellWidthPx).roundToInt().toLong()
                        if (deltaDays != 0L) onRescheduleTask(deltaDays) else onTaskClick()
                    }
                )
            }
    )
}

private fun statusColor(status: TaskStatus): Color {
    return when (status) {
        TaskStatus.TODO -> Color(0xFF2F6BFF)
        TaskStatus.IN_PROGRESS -> Color(0xFFE68A00)
        TaskStatus.DONE -> Color(0xFF1B8A5A)
    }
}

private fun TimelineZoom.cellWidth(): Dp {
    return when (this) {
        TimelineZoom.DAY -> 72.dp
        TimelineZoom.WEEK -> 52.dp
        TimelineZoom.MONTH -> 34.dp
    }
}
