package com.nk.motificason.ui.streak

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AcUnit
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.HourglassEmpty
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nk.motificason.data.engine.DayHistory
import com.nk.motificason.data.engine.DayStatus
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@Composable
fun CalendarHeatmap(
    days: List<DayHistory>,
    modifier: Modifier = Modifier
) {
    // 5 full weeks = 35 days chunked into 5 lists of 7 days (Mon-Sun)
    val weeks = remember(days) {
        if (days.size >= 35) {
            days.takeLast(35).chunked(7)
        } else {
            days.chunked(7)
        }
    }

    var selectedDay by remember { mutableStateOf<DayHistory?>(null) }
    val dayLabelFormatter = DateTimeFormatter.ofPattern("MMM d")
    val fullDateFormatter = DateTimeFormatter.ofPattern("EEEE, MMMM d, yyyy")

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Consistency Heatmap",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Past 5 weeks activity",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Surface(
                    color = MaterialTheme.colorScheme.surfaceContainerHigh,
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = "5 Weeks",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Heatmap Grid
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center
            ) {
                // Day of Week Labels (Mon, Wed, Fri, Sun)
                Column(
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.padding(end = 8.dp, top = 22.dp)
                ) {
                    val dayNames = listOf("M", "T", "W", "T", "F", "S", "S")
                    dayNames.forEachIndexed { index, name ->
                        Box(
                            modifier = Modifier.size(28.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            if (index % 2 == 0) {
                                Text(
                                    text = name,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }

                // 5 Week Columns
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    weeks.forEachIndexed { weekIndex, weekDays ->
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            // Column header (date or "Now")
                            val weekHeader = if (weekIndex == weeks.size - 1) {
                                "Now"
                            } else {
                                weekDays.firstOrNull()?.date?.format(dayLabelFormatter) ?: "W$weekIndex"
                            }
                            Text(
                                text = weekHeader,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier
                                    .height(20.dp)
                                    .padding(bottom = 4.dp),
                                textAlign = TextAlign.Center
                            )

                            // 7 days in this week (Mon to Sun)
                            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                weekDays.forEach { dayHistory ->
                                    HeatmapCell(
                                        dayHistory = dayHistory,
                                        isSelected = selectedDay?.date == dayHistory.date,
                                        onCellClick = {
                                            selectedDay = if (selectedDay?.date == dayHistory.date) null else dayHistory
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Tooltip / detail label when a cell is tapped
            AnimatedVisibility(
                visible = selectedDay != null,
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                selectedDay?.let { day ->
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceContainerHigh,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 6.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = day.date.format(fullDateFormatter),
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface
                            )

                            StatusChip(status = day.status, date = day.date)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
            Spacer(modifier = Modifier.height(10.dp))

            // Heatmap Legend
            HeatmapLegend()
        }
    }
}

@Composable
fun HeatmapCell(
    dayHistory: DayHistory,
    isSelected: Boolean,
    onCellClick: () -> Unit
) {
    val brandColor = MaterialTheme.colorScheme.primary
    val frozenColor = Color(0xFF00BCD4) // Distinct Ice-Blue / Cyan
    val missedBg = MaterialTheme.colorScheme.error.copy(alpha = 0.12f)
    val missedBorder = MaterialTheme.colorScheme.error.copy(alpha = 0.45f)
    val upcomingBg = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
    val upcomingBorder = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)

    val (cellBg, borderStroke) = when (dayHistory.status) {
        DayStatus.COMPLETED -> Pair(
            brandColor,
            if (isSelected) BorderStroke(2.dp, MaterialTheme.colorScheme.onSurface) else null
        )
        DayStatus.FROZEN -> Pair(
            frozenColor,
            if (isSelected) BorderStroke(2.dp, MaterialTheme.colorScheme.onSurface) else null
        )
        DayStatus.MISSED -> Pair(
            missedBg,
            if (isSelected) {
                BorderStroke(2.dp, MaterialTheme.colorScheme.onSurface)
            } else {
                BorderStroke(1.dp, missedBorder)
            }
        )
        DayStatus.UPCOMING -> Pair(
            upcomingBg,
            if (isSelected) {
                BorderStroke(2.dp, MaterialTheme.colorScheme.onSurface)
            } else {
                BorderStroke(1.dp, upcomingBorder)
            }
        )
    }

    Box(
        modifier = Modifier
            .size(28.dp)
            .clip(RoundedCornerShape(6.dp))
            .background(cellBg)
            .then(
                if (borderStroke != null) {
                    Modifier.border(borderStroke, RoundedCornerShape(6.dp))
                } else {
                    Modifier
                }
            )
            .clickable { onCellClick() },
        contentAlignment = Alignment.Center
    ) {
        when (dayHistory.status) {
            DayStatus.COMPLETED -> {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.size(14.dp)
                )
            }
            DayStatus.FROZEN -> {
                Icon(
                    imageVector = Icons.Default.AcUnit,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(13.dp)
                )
            }
            DayStatus.MISSED -> {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error.copy(alpha = 0.7f),
                    modifier = Modifier.size(12.dp)
                )
            }
            DayStatus.UPCOMING -> {
                // Empty / neutral cell
            }
        }
    }
}

@Composable
fun StatusChip(status: DayStatus, date: LocalDate) {
    val isToday = date == LocalDate.now()

    val (label, bg, contentColor, icon) = when (status) {
        DayStatus.COMPLETED -> Quadruple(
            "Completed",
            MaterialTheme.colorScheme.primaryContainer,
            MaterialTheme.colorScheme.onPrimaryContainer,
            Icons.Default.Check
        )
        DayStatus.FROZEN -> Quadruple(
            "Streak Frozen",
            Color(0xFFE0F7FA),
            Color(0xFF006064),
            Icons.Default.AcUnit
        )
        DayStatus.MISSED -> Quadruple(
            "Missed",
            MaterialTheme.colorScheme.errorContainer,
            MaterialTheme.colorScheme.onErrorContainer,
            Icons.Default.Close
        )
        DayStatus.UPCOMING -> Quadruple(
            if (isToday) "Today (Pending)" else "Upcoming",
            MaterialTheme.colorScheme.surfaceVariant,
            MaterialTheme.colorScheme.onSurfaceVariant,
            Icons.Default.HourglassEmpty
        )
    }

    Surface(
        color = bg,
        shape = RoundedCornerShape(8.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = contentColor,
                modifier = Modifier.size(12.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = contentColor,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

private data class Quadruple<A, B, C, D>(val first: A, val second: B, val third: C, val fourth: D)

@Composable
fun HeatmapLegend() {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        LegendItem(
            label = "Completed",
            color = MaterialTheme.colorScheme.primary
        )
        LegendItem(
            label = "Frozen",
            color = Color(0xFF00BCD4)
        )
        LegendItem(
            label = "Missed",
            color = MaterialTheme.colorScheme.error.copy(alpha = 0.35f),
            isOutlined = true
        )
        LegendItem(
            label = "Upcoming",
            color = MaterialTheme.colorScheme.surfaceVariant,
            isOutlined = true
        )
    }
}

@Composable
fun LegendItem(
    label: String,
    color: Color,
    isOutlined: Boolean = false
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(12.dp)
                .clip(RoundedCornerShape(3.dp))
                .background(color)
                .then(
                    if (isOutlined) {
                        Modifier.border(
                            1.dp,
                            MaterialTheme.colorScheme.outlineVariant,
                            RoundedCornerShape(3.dp)
                        )
                    } else {
                        Modifier
                    }
                )
        )
        Spacer(modifier = Modifier.width(4.dp))
        Text(
            text = label,
            fontSize = 11.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
