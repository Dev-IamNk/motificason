package com.nk.motificason.ui.share

import android.graphics.Bitmap
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.scaleIn
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.Replay
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.layer.drawLayer
import androidx.compose.ui.graphics.rememberGraphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nk.motificason.data.model.DailyActivity
import com.nk.motificason.ui.routine.ClockBlockArc
import com.nk.motificason.ui.routine.RoutineClockHelper
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DailyRoutineShareCardScreen(
    activities: List<DailyActivity>,
    displayDate: String,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val graphicsLayer = rememberGraphicsLayer()

    val clockArcs = remember(activities) {
        RoutineClockHelper.calculateClockArcs(activities)
    }

    var animationTrigger by remember { mutableIntStateOf(0) }
    var activeBlockIndex by remember { mutableIntStateOf(0) }
    var isSummaryVisible by remember { mutableStateOf(false) }
    var isSharing by remember { mutableStateOf(false) }

    // Chronological step animation
    LaunchedEffect(animationTrigger, clockArcs) {
        if (clockArcs.isEmpty()) {
            activeBlockIndex = -1
            isSummaryVisible = true
            return@LaunchedEffect
        }

        isSummaryVisible = false
        activeBlockIndex = 0

        for (i in clockArcs.indices) {
            activeBlockIndex = i
            delay(1200L) // Highlight each block for 1.2s
        }

        delay(400L)
        activeBlockIndex = -1 // All illuminated
        isSummaryVisible = true
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = { Text("Daily Routine Card", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                actions = {
                    IconButton(onClick = { animationTrigger++ }) {
                        Icon(
                            imageVector = Icons.Default.Replay,
                            contentDescription = "Replay Animation"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainer
                )
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // 9:16 Vertical Card Viewport
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxHeight(0.95f)
                        .aspectRatio(9f / 16f)
                        .drawWithContent {
                            graphicsLayer.record {
                                this@drawWithContent.drawContent()
                            }
                            drawLayer(graphicsLayer)
                        }
                ) {
                    DailyRoutineStoryCard(
                        clockArcs = clockArcs,
                        displayDate = displayDate,
                        activeBlockIndex = activeBlockIndex,
                        isSummaryVisible = isSummaryVisible
                    )
                }
            }

            // Bottom Action Controls
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 12.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Button(
                    onClick = {
                        if (isSharing) return@Button
                        isSharing = true
                        coroutineScope.launch {
                            try {
                                val imageBitmap = graphicsLayer.toImageBitmap()
                                val androidBitmap = imageBitmap.asAndroidBitmap()
                                val uri = ShareHelper.saveBitmapToCache(context, androidBitmap)
                                if (uri != null) {
                                    val text = "Locked in with today's routine on Motificason! ⚡ #DailyRoutine #Discipline"
                                    ShareHelper.shareImageUri(context, uri, text)
                                }
                            } finally {
                                isSharing = false
                            }
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(54.dp),
                    shape = RoundedCornerShape(16.dp),
                    enabled = !isSharing
                ) {
                    if (isSharing) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(22.dp),
                            color = MaterialTheme.colorScheme.onPrimary,
                            strokeWidth = 2.5.dp
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Default.Share,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Share Routine Story",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = "Share directly to Instagram Stories, WhatsApp & more",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
fun DailyRoutineStoryCard(
    clockArcs: List<ClockBlockArc>,
    displayDate: String,
    activeBlockIndex: Int,
    isSummaryVisible: Boolean,
    modifier: Modifier = Modifier
) {
    val totalMinutes = remember(clockArcs) {
        clockArcs.sumOf { it.durationMinutes }
    }
    val totalFormatted = remember(totalMinutes) {
        RoutineClockHelper.formatDuration(totalMinutes)
    }

    Card(
        modifier = modifier.fillMaxSize(),
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFF0F0E17)
        ),
        border = BorderStroke(1.5.dp, Color(0xFF2E2D3E))
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color(0xFF16152B),
                            Color(0xFF0D0C18),
                            Color(0xFF07070F)
                        )
                    )
                )
                .padding(20.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                // Header Area
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Text(text = "⚡", fontSize = 16.sp)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "MOTIFICASON",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Black,
                            letterSpacing = 2.sp,
                            color = Color(0xFFFFD54F)
                        )
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    Text(
                        text = "DAILY ROUTINE",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 1.sp,
                        color = Color.White
                    )

                    Text(
                        text = if (displayDate.isNotBlank()) displayDate.uppercase() else "TODAY'S SCHEDULE",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White.copy(alpha = 0.6f),
                        letterSpacing = 1.sp
                    )
                }

                // Clock Face with Highlight Animation
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 10.dp),
                    contentAlignment = Alignment.Center
                ) {
                    ClockFaceCanvas(
                        clockArcs = clockArcs,
                        activeBlockIndex = activeBlockIndex,
                        modifier = Modifier
                            .size(220.dp)
                            .padding(8.dp)
                    )

                    // Center Dynamic Callout
                    ClockCenterCallout(
                        clockArcs = clockArcs,
                        activeBlockIndex = activeBlockIndex,
                        isSummaryVisible = isSummaryVisible,
                        totalFormatted = totalFormatted
                    )
                }

                // Summary List of All Blocks Area
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    AnimatedVisibility(
                        visible = isSummaryVisible,
                        enter = fadeIn() + scaleIn(spring(dampingRatio = Spring.DampingRatioMediumBouncy))
                    ) {
                        Surface(
                            shape = RoundedCornerShape(18.dp),
                            color = Color(0xFF1E1C30),
                            border = BorderStroke(1.dp, Color(0xFF3B3958)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "ROUTINE BREAKDOWN",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Black,
                                        letterSpacing = 1.sp,
                                        color = Color(0xFFFFD54F)
                                    )
                                    Text(
                                        text = "$totalFormatted LOCKED IN",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF00E676)
                                    )
                                }

                                Spacer(modifier = Modifier.height(8.dp))

                                // Show up to 4 blocks cleanly
                                val previewArcs = clockArcs.take(4)
                                previewArcs.forEach { arc ->
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 3.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            modifier = Modifier.weight(1f)
                                        ) {
                                            Surface(
                                                shape = CircleShape,
                                                color = arc.color,
                                                modifier = Modifier.size(8.dp)
                                            ) {}
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text(text = arc.activity.emoji, fontSize = 13.sp)
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text(
                                                text = arc.activity.title,
                                                style = MaterialTheme.typography.labelMedium,
                                                color = Color.White,
                                                fontWeight = FontWeight.SemiBold,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }
                                        Text(
                                            text = arc.formattedTimeSpan,
                                            style = MaterialTheme.typography.labelSmall,
                                            color = Color.White.copy(alpha = 0.65f),
                                            fontSize = 11.sp
                                        )
                                    }
                                }

                                if (clockArcs.size > 4) {
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = "+ ${clockArcs.size - 4} more activities scheduled",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = Color.White.copy(alpha = 0.45f),
                                        fontSize = 10.sp
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = "motificason.app",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White.copy(alpha = 0.35f),
                        letterSpacing = 1.sp
                    )
                }
            }
        }
    }
}

@Composable
fun ClockFaceCanvas(
    clockArcs: List<ClockBlockArc>,
    activeBlockIndex: Int,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier) {
        val strokeWidth = 14.dp.toPx()
        val diameter = size.minDimension - strokeWidth * 2
        val radius = diameter / 2f
        val center = Offset(size.width / 2f, size.height / 2f)
        val topLeft = Offset(center.x - radius, center.y - radius)
        val arcSize = Size(radius * 2, radius * 2)

        // 1. Base 24-hour track ring
        drawCircle(
            color = Color(0xFF232238),
            radius = radius,
            center = center,
            style = Stroke(width = strokeWidth)
        )

        // 2. Hour pole markers (00, 06, 12, 18)
        val markerRadius = radius + strokeWidth * 0.9f
        val poleAngles = listOf(-90f, 0f, 90f, 180f)
        for (angle in poleAngles) {
            val rad = Math.toRadians(angle.toDouble())
            val px1 = (center.x + (radius - strokeWidth / 2) * Math.cos(rad)).toFloat()
            val py1 = (center.y + (radius - strokeWidth / 2) * Math.sin(rad)).toFloat()
            val px2 = (center.x + (radius + strokeWidth / 2) * Math.cos(rad)).toFloat()
            val py2 = (center.y + (radius + strokeWidth / 2) * Math.sin(rad)).toFloat()
            drawLine(
                color = Color(0xFF3F3D5E),
                start = Offset(px1, py1),
                end = Offset(px2, py2),
                strokeWidth = 2.dp.toPx(),
                cap = StrokeCap.Round
            )
        }

        // 3. Draw time block arcs
        clockArcs.forEachIndexed { index, arc ->
            val isActive = activeBlockIndex == index
            val isPassed = activeBlockIndex == -1 || activeBlockIndex > index

            val currentStrokeWidth = if (isActive) strokeWidth * 1.5f else strokeWidth
            val alpha = when {
                isActive -> 1f
                isPassed -> 0.9f
                else -> 0.25f
            }

            drawArc(
                color = arc.color.copy(alpha = alpha),
                startAngle = arc.startAngle,
                sweepAngle = arc.sweepAngle,
                useCenter = false,
                topLeft = topLeft,
                size = arcSize,
                style = Stroke(width = currentStrokeWidth, cap = StrokeCap.Round)
            )

            // Outer pulse glow for active block
            if (isActive) {
                drawArc(
                    color = arc.color.copy(alpha = 0.35f),
                    startAngle = arc.startAngle,
                    sweepAngle = arc.sweepAngle,
                    useCenter = false,
                    topLeft = topLeft,
                    size = arcSize,
                    style = Stroke(width = currentStrokeWidth * 1.7f, cap = StrokeCap.Round)
                )
            }
        }
    }
}

@Composable
fun ClockCenterCallout(
    clockArcs: List<ClockBlockArc>,
    activeBlockIndex: Int,
    isSummaryVisible: Boolean,
    totalFormatted: String,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = CircleShape,
        color = Color(0xFF141322),
        border = BorderStroke(1.5.dp, Color(0xFF33314E)),
        modifier = modifier.size(130.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(10.dp),
            contentAlignment = Alignment.Center
        ) {
            if (activeBlockIndex in clockArcs.indices) {
                val currentArc = clockArcs[activeBlockIndex]
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(text = currentArc.activity.emoji, fontSize = 28.sp)
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = currentArc.activity.title,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Black,
                        color = Color.White,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        textAlign = TextAlign.Center
                    )
                    Text(
                        text = currentArc.formattedTimeSpan,
                        style = MaterialTheme.typography.labelSmall,
                        color = currentArc.color,
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp
                    )
                    Text(
                        text = currentArc.formattedDuration,
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White.copy(alpha = 0.55f),
                        fontSize = 10.sp
                    )
                }
            } else if (clockArcs.isEmpty()) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text("⏰", fontSize = 28.sp)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "NO BLOCKS",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = Color.White.copy(alpha = 0.6f)
                    )
                }
            } else {
                // Summary completed state
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.LocalFireDepartment,
                        contentDescription = null,
                        tint = Color(0xFFFF7043),
                        modifier = Modifier.size(28.dp)
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "LOCKED IN",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 1.sp,
                        color = Color.White
                    )
                    Text(
                        text = totalFormatted,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Black,
                        color = Color(0xFF00E676)
                    )
                }
            }
        }
    }
}
