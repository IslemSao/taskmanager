package com.saokt.taskmanager.presentation.components

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
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
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.saokt.taskmanager.domain.model.Task
import com.saokt.taskmanager.domain.model.TaskStatus
import com.saokt.taskmanager.domain.model.TimelineEdge
import com.saokt.taskmanager.domain.model.TimelineItem
import com.saokt.taskmanager.domain.model.TimelineRange
import com.saokt.taskmanager.domain.model.TimelineZoom
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
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
    val configuration = LocalConfiguration.current
    val monthFormatter = remember { DateTimeFormatter.ofPattern("MMM yyyy") }
    val compactHeaderFormatter = remember { DateTimeFormatter.ofPattern("dd MMM") }
    val expandedHeaderFormatter = remember(zoom) {
        when (zoom) {
            TimelineZoom.DAY -> DateTimeFormatter.ofPattern("EEE\ndd")
            TimelineZoom.WEEK -> DateTimeFormatter.ofPattern("dd MMM")
            TimelineZoom.MONTH -> DateTimeFormatter.ofPattern("dd")
        }
    }

    BoxWithConstraints(modifier = modifier.fillMaxWidth()) {
        val isCompact = maxWidth < 680.dp
        val cellWidth = zoom.cellWidth(compact = isCompact)
        val chartWidth = cellWidth * timelineRange.totalDays

        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            TimelineSummaryCard(
                range = timelineRange,
                scheduledCount = items.size,
                unscheduledCount = unscheduledTasks.size,
                monthFormatter = monthFormatter
            )

            TimelineToolbar(
                zoom = zoom,
                isCompact = isCompact,
                onZoomChange = onZoomChange,
                onShiftBackward = onShiftBackward,
                onShiftForward = onShiftForward,
                onJumpToToday = onJumpToToday
            )

            if (isCompact && configuration.orientation != Configuration.ORIENTATION_LANDSCAPE) {
                CompactTimelineHint()
            }

            TimelineHeader(
                range = timelineRange,
                cellWidth = cellWidth,
                chartWidth = chartWidth,
                horizontalScroll = horizontalScroll,
                formatter = if (isCompact) compactHeaderFormatter else expandedHeaderFormatter,
                compact = isCompact,
                monthFormatter = monthFormatter
            )

            if (items.isEmpty()) {
                EmptyScheduledTimelineState(hasUnscheduledTasks = unscheduledTasks.isNotEmpty())
            } else if (isCompact) {
                CompactTimelineRows(
                    items = items,
                    range = timelineRange,
                    cellWidth = cellWidth,
                    chartWidth = chartWidth,
                    horizontalScroll = horizontalScroll,
                    secondaryLabel = secondaryLabel,
                    onTaskClick = onTaskClick,
                    onRescheduleTask = onRescheduleTask,
                    onResizeTask = onResizeTask
                )
            } else {
                ExpandedTimelineRows(
                    items = items,
                    range = timelineRange,
                    cellWidth = cellWidth,
                    chartWidth = chartWidth,
                    horizontalScroll = horizontalScroll,
                    secondaryLabel = secondaryLabel,
                    onTaskClick = onTaskClick,
                    onRescheduleTask = onRescheduleTask,
                    onResizeTask = onResizeTask
                )
            }

            if (unscheduledTasks.isNotEmpty()) {
                UnscheduledTasksSection(
                    unscheduledTasks = unscheduledTasks,
                    secondaryLabel = secondaryLabel,
                    onTaskClick = onTaskClick,
                    onPlanTask = onPlanTask
                )
            }
        }
    }
}

@Composable
private fun TimelineSummaryCard(
    range: TimelineRange,
    scheduledCount: Int,
    unscheduledCount: Int,
    monthFormatter: DateTimeFormatter
) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.28f)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Text(
                    text = formatRangeHeadline(range, monthFormatter),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = "${range.start.format(DateTimeFormatter.ofPattern("dd MMM"))} to ${range.end.format(DateTimeFormatter.ofPattern("dd MMM"))}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = "$scheduledCount scheduled",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = if (unscheduledCount == 0) {
                        "All tasks placed"
                    } else {
                        "$unscheduledCount still unscheduled"
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TimelineToolbar(
    zoom: TimelineZoom,
    isCompact: Boolean,
    onZoomChange: (TimelineZoom) -> Unit,
    onShiftBackward: () -> Unit,
    onShiftForward: () -> Unit,
    onJumpToToday: () -> Unit
) {
    if (isCompact) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(onClick = onShiftBackward, modifier = Modifier.weight(1f)) { Text("Earlier") }
                OutlinedButton(onClick = onJumpToToday, modifier = Modifier.weight(1f)) { Text("Today") }
                OutlinedButton(onClick = onShiftForward, modifier = Modifier.weight(1f)) { Text("Later") }
            }

            SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                TimelineZoom.entries.forEachIndexed { index, option ->
                    SegmentedButton(
                        selected = zoom == option,
                        onClick = { onZoomChange(option) },
                        modifier = Modifier.weight(1f),
                        shape = SegmentedButtonDefaults.itemShape(
                            index = index,
                            count = TimelineZoom.entries.size
                        )
                    ) {
                        Text(option.name.lowercase().replaceFirstChar(Char::uppercase))
                    }
                }
            }
        }
    } else {
        Row(
            modifier = Modifier.fillMaxWidth(),
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
                        shape = SegmentedButtonDefaults.itemShape(
                            index = index,
                            count = TimelineZoom.entries.size
                        )
                    ) {
                        Text(option.name.lowercase().replaceFirstChar(Char::uppercase))
                    }
                }
            }
        }
    }
}

@Composable
private fun CompactTimelineHint() {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.7f)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = "Portrait works, landscape is better",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
            Text(
                text = "You can plan tasks here, but rotating the phone gives you the full Gantt board and much more context at once.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
        }
    }
}

@Composable
private fun TimelineHeader(
    range: TimelineRange,
    cellWidth: Dp,
    chartWidth: Dp,
    horizontalScroll: androidx.compose.foundation.ScrollState,
    formatter: DateTimeFormatter,
    compact: Boolean,
    monthFormatter: DateTimeFormatter
) {
    val today = LocalDate.now()
    val monthSegments = remember(range.start, range.end) { buildMonthSegments(range) }

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(
            modifier = Modifier
                .horizontalScroll(horizontalScroll)
                .width(chartWidth)
        ) {
            monthSegments.forEach { segment ->
                Box(
                    modifier = Modifier
                        .width(cellWidth * segment.dayCount)
                        .clip(RoundedCornerShape(14.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f))
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    contentAlignment = Alignment.CenterStart
                ) {
                    Text(
                        text = segment.month.format(monthFormatter),
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = FontWeight.SemiBold
                    )
                }
                Spacer(modifier = Modifier.width(4.dp))
            }
        }

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
                        .height(if (compact) 46.dp else 52.dp)
                        .padding(horizontal = 2.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(
                            when {
                                isToday -> MaterialTheme.colorScheme.primary.copy(alpha = 0.18f)
                                day.isWeekend() -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.18f)
                                else -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.1f)
                            }
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = day.format(formatter),
                        style = if (compact) {
                            MaterialTheme.typography.labelSmall
                        } else {
                            MaterialTheme.typography.labelMedium
                        },
                        color = if (isToday) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        },
                        fontWeight = if (isToday) FontWeight.Bold else FontWeight.Medium
                    )
                }
            }
        }
    }
}

@Composable
private fun ExpandedTimelineRows(
    items: List<TimelineItem>,
    range: TimelineRange,
    cellWidth: Dp,
    chartWidth: Dp,
    horizontalScroll: androidx.compose.foundation.ScrollState,
    secondaryLabel: (Task) -> String?,
    onTaskClick: (String) -> Unit,
    onRescheduleTask: (Task, Long) -> Unit,
    onResizeTask: (Task, TimelineEdge, Long) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        items.forEach { item ->
            ExpandedTimelineRow(
                item = item,
                range = range,
                cellWidth = cellWidth,
                chartWidth = chartWidth,
                horizontalScroll = horizontalScroll,
                secondaryLabel = secondaryLabel(item.task),
                onTaskClick = { onTaskClick(item.task.id) },
                onRescheduleTask = { deltaDays -> onRescheduleTask(item.task, deltaDays) },
                onResizeTask = { edge, deltaDays -> onResizeTask(item.task, edge, deltaDays) }
            )
        }
    }
}

@Composable
private fun ExpandedTimelineRow(
    item: TimelineItem,
    range: TimelineRange,
    cellWidth: Dp,
    chartWidth: Dp,
    horizontalScroll: androidx.compose.foundation.ScrollState,
    secondaryLabel: String?,
    onTaskClick: () -> Unit,
    onRescheduleTask: (Long) -> Unit,
    onResizeTask: (TimelineEdge, Long) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.Top
    ) {
        Card(
            modifier = Modifier.width(232.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.18f)
            )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    text = item.task.title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
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
                Text(
                    text = formatTaskWindow(item),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                TimelineBadgeRow(item = item)
            }
        }

        TimelineLane(
            item = item,
            range = range,
            cellWidth = cellWidth,
            chartWidth = chartWidth,
            horizontalScroll = horizontalScroll,
            laneHeight = 68.dp,
            onTaskClick = onTaskClick,
            onRescheduleTask = onRescheduleTask,
            onResizeTask = onResizeTask
        )
    }
}

@Composable
private fun CompactTimelineRows(
    items: List<TimelineItem>,
    range: TimelineRange,
    cellWidth: Dp,
    chartWidth: Dp,
    horizontalScroll: androidx.compose.foundation.ScrollState,
    secondaryLabel: (Task) -> String?,
    onTaskClick: (String) -> Unit,
    onRescheduleTask: (Task, Long) -> Unit,
    onResizeTask: (Task, TimelineEdge, Long) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        items.forEach { item ->
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.14f)
                )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Top
                    ) {
                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                text = item.task.title,
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.SemiBold,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                            secondaryLabel(item.task)?.let {
                                Text(
                                    text = it,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Text(
                                text = formatTaskWindow(item),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        TimelineBadgeRow(item = item)
                    }

                    TimelineLane(
                        item = item,
                        range = range,
                        cellWidth = cellWidth,
                        chartWidth = chartWidth,
                        horizontalScroll = horizontalScroll,
                        laneHeight = 56.dp,
                        onTaskClick = { onTaskClick(item.task.id) },
                        onRescheduleTask = { deltaDays -> onRescheduleTask(item.task, deltaDays) },
                        onResizeTask = { edge, deltaDays -> onResizeTask(item.task, edge, deltaDays) }
                    )
                }
            }
        }
    }
}

@Composable
private fun TimelineBadgeRow(item: TimelineItem) {
    Column(
        horizontalAlignment = Alignment.End,
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        StatusBadge(status = item.task.status)
        if (!item.canEditSchedule) {
            SmallMetaPill(label = "Read only")
        }
    }
}

@Composable
private fun TimelineLane(
    item: TimelineItem,
    range: TimelineRange,
    cellWidth: Dp,
    chartWidth: Dp,
    horizontalScroll: androidx.compose.foundation.ScrollState,
    laneHeight: Dp,
    onTaskClick: () -> Unit,
    onRescheduleTask: (Long) -> Unit,
    onResizeTask: (TimelineEdge, Long) -> Unit
) {
    val today = LocalDate.now()
    val dayOffset = ChronoUnit.DAYS.between(range.start, item.start).toInt()
    val barWidth = if (item.isMilestone) 18.dp else cellWidth * item.spanDays
    val barOffset = cellWidth * dayOffset

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = laneHeight)
    ) {
        Row(
            modifier = Modifier
                .horizontalScroll(horizontalScroll)
                .width(chartWidth)
        ) {
            repeat(range.totalDays) { offset ->
                val day = range.start.plusDays(offset.toLong())
                Box(
                    modifier = Modifier
                        .width(cellWidth)
                        .height(laneHeight)
                        .padding(horizontal = 2.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(
                            when {
                                day == today -> MaterialTheme.colorScheme.primary.copy(alpha = 0.08f)
                                day.isWeekend() -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.12f)
                                else -> Color.Transparent
                            }
                        )
                        .border(
                            width = 1.dp,
                            color = if (day == today) {
                                MaterialTheme.colorScheme.primary.copy(alpha = 0.45f)
                            } else {
                                MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
                            },
                            shape = RoundedCornerShape(12.dp)
                        )
                )
            }
        }

        Row(
            modifier = Modifier
                .horizontalScroll(horizontalScroll)
                .width(chartWidth)
                .padding(top = (laneHeight - 38.dp) / 2)
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

@Composable
private fun EmptyScheduledTimelineState(hasUnscheduledTasks: Boolean) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.18f)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(
                text = "No scheduled tasks in this view",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = if (hasUnscheduledTasks) {
                    "Use the unscheduled section below to place tasks onto the timeline."
                } else {
                    "Create a task with dates to start building the timeline."
                },
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun UnscheduledTasksSection(
    unscheduledTasks: List<Task>,
    secondaryLabel: (Task) -> String?,
    onTaskClick: (String) -> Unit,
    onPlanTask: (Task) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(
            text = "Unscheduled",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold
        )

        unscheduledTasks.forEach { task ->
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.18f)
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = task.title,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold
                        )
                        secondaryLabel(task)?.let {
                            Text(
                                text = it,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Text(
                            text = "No dates yet",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(onClick = { onTaskClick(task.id) }) {
                            Text("Open")
                        }
                        Button(onClick = { onPlanTask(task) }) {
                            Text("Plan today")
                        }
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
    val barColor = statusColor(item.task.status)
    Box(
        modifier = Modifier
            .padding(start = barOffset)
            .width(barWidth)
            .height(38.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(barColor)
            .border(
                width = 1.dp,
                color = Color.White.copy(alpha = 0.12f),
                shape = RoundedCornerShape(14.dp)
            )
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
                        if (deltaDays != 0L) {
                            onRescheduleTask(deltaDays)
                        }
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
                color = Color.White,
                fontWeight = FontWeight.SemiBold,
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
                if (enabled) Color.Black.copy(alpha = 0.16f) else Color.Transparent
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
                        if (deltaDays != 0L) {
                            onResizeTask(edge, deltaDays)
                        }
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
                        if (deltaDays != 0L) {
                            onRescheduleTask(deltaDays)
                        } else {
                            onTaskClick()
                        }
                    }
                )
            }
    )
}

@Composable
private fun StatusBadge(status: TaskStatus) {
    val background = when (status) {
        TaskStatus.TODO -> Color(0x332F6BFF)
        TaskStatus.IN_PROGRESS -> Color(0x33E68A00)
        TaskStatus.DONE -> Color(0x331B8A5A)
    }
    val content = when (status) {
        TaskStatus.TODO -> Color(0xFF7FA6FF)
        TaskStatus.IN_PROGRESS -> Color(0xFFFFB24D)
        TaskStatus.DONE -> Color(0xFF5DD39E)
    }
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(999.dp))
            .background(background)
            .padding(horizontal = 10.dp, vertical = 5.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = status.name.replace('_', ' ').lowercase().replaceFirstChar(Char::uppercase),
            style = MaterialTheme.typography.labelSmall,
            color = content,
            fontWeight = FontWeight.SemiBold
        )
    }
}

@Composable
private fun SmallMetaPill(label: String) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(999.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f))
            .padding(horizontal = 10.dp, vertical = 5.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

private fun formatTaskWindow(item: TimelineItem): String {
    val formatter = DateTimeFormatter.ofPattern("dd MMM")
    return if (item.isMilestone) {
        "Milestone on ${item.start.format(formatter)}"
    } else {
        val days = item.spanDays
        "${item.start.format(formatter)} - ${item.end.format(formatter)}  |  $days day${if (days == 1) "" else "s"}"
    }
}

private fun formatRangeHeadline(range: TimelineRange, formatter: DateTimeFormatter): String {
    return if (YearMonth.from(range.start) == YearMonth.from(range.end)) {
        range.start.format(formatter)
    } else {
        "${range.start.format(formatter)} - ${range.end.format(formatter)}"
    }
}

private fun buildMonthSegments(range: TimelineRange): List<MonthSegment> {
    if (range.totalDays <= 0) return emptyList()

    val segments = mutableListOf<MonthSegment>()
    var cursor = range.start
    while (!cursor.isAfter(range.end)) {
        val currentMonth = YearMonth.from(cursor)
        val lastDayInMonth = currentMonth.atEndOfMonth().coerceAtMost(range.end)
        val dayCount = ChronoUnit.DAYS.between(cursor, lastDayInMonth).toInt() + 1
        segments += MonthSegment(month = currentMonth, dayCount = dayCount)
        cursor = lastDayInMonth.plusDays(1)
    }
    return segments
}

private fun statusColor(status: TaskStatus): Color {
    return when (status) {
        TaskStatus.TODO -> Color(0xFF2F6BFF)
        TaskStatus.IN_PROGRESS -> Color(0xFFE68A00)
        TaskStatus.DONE -> Color(0xFF1B8A5A)
    }
}

private fun TimelineZoom.cellWidth(compact: Boolean): Dp {
    return when (this) {
        TimelineZoom.DAY -> if (compact) 56.dp else 72.dp
        TimelineZoom.WEEK -> if (compact) 44.dp else 56.dp
        TimelineZoom.MONTH -> if (compact) 32.dp else 40.dp
    }
}

private fun LocalDate.isWeekend(): Boolean {
    return dayOfWeek.value >= 6
}

private data class MonthSegment(
    val month: YearMonth,
    val dayCount: Int
)
