package com.nk.motificason.ui.share

import android.graphics.Bitmap
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.scaleIn
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AcUnit
import androidx.compose.material.icons.filled.Check
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
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.layer.drawLayer
import androidx.compose.ui.graphics.rememberGraphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nk.motificason.data.engine.DayHistory
import com.nk.motificason.data.engine.DayStatus
import com.nk.motificason.data.engine.StreakCalculationResult
import com.nk.motificason.data.engine.StreakEngine
import com.nk.motificason.data.model.Habit
import com.nk.motificason.data.model.LockIn
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ShareCardScreen(
    habit: Habit,
    lockIn: LockIn?,
    currentStreak: Int,
    streakDays: List<DayHistory>,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val graphicsLayer = rememberGraphicsLayer()

    val displayDays = remember(streakDays) {
        if (streakDays.size > 30) streakDays.takeLast(30) else streakDays
    }

    var animationTrigger by remember { mutableIntStateOf(0) }
    var revealedCount by remember { mutableIntStateOf(0) }
    var isSummaryVisible by remember { mutableStateOf(false) }
    var isSharing by remember { mutableStateOf(false) }

    // Sequential cell fill animation
    LaunchedEffect(animationTrigger, displayDays) {
        revealedCount = 0
        isSummaryVisible = false

        if (displayDays.isEmpty()) {
            isSummaryVisible = true
            return@LaunchedEffect
        }

        val stepDelayMs = (1500L / displayDays.size).coerceIn(40L, 100L)
        for (i in 1..displayDays.size) {
            revealedCount = i
            delay(stepDelayMs)
        }
        delay(200L)
        isSummaryVisible = true
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = { Text("Lock-In Journey", fontWeight = FontWeight.Bold) },
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
                    LockInJourneyStoryCard(
                        habit = habit,
                        lockIn = lockIn,
                        streakDays = displayDays,
                        currentStreak = currentStreak,
                        revealedCount = revealedCount,
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
                                    val text = "Locked in for $currentStreak days on ${habit.title} with Motificason! 🔥 #LockIn"
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
                            text = "Share to Stories",
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
fun LockInJourneyStoryCard(
    habit: Habit,
    lockIn: LockIn?,
    streakDays: List<DayHistory>,
    currentStreak: Int,
    revealedCount: Int,
    isSummaryVisible: Boolean,
    modifier: Modifier = Modifier
) {
    val totalCells = 30 // Standard 30-day journey grid (5 rows x 6 columns)

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
                        Text(
                            text = "⚡",
                            fontSize = 16.sp
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "MOTIFICASON",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Black,
                            letterSpacing = 2.sp,
                            color = Color(0xFFFFD54F)
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Text(
                        text = lockIn?.emoji ?: "🎯",
                        fontSize = 38.sp
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = lockIn?.name?.uppercase() ?: "LOCK-IN",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 1.sp,
                        color = Color.White
                    )

                    Text(
                        text = habit.title,
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.White.copy(alpha = 0.7f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                // Center Grid (30 Day Streak Grid)
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "JOURNEY GRID",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.2.sp,
                            color = Color.White.copy(alpha = 0.5f)
                        )
                        Text(
                            text = "DAY $revealedCount / ${maxOf(streakDays.size, 1)}",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF00E676)
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    LazyVerticalGrid(
                        columns = GridCells.Fixed(6),
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        contentPadding = PaddingValues(0.dp),
                        userScrollEnabled = false
                    ) {
                        items(totalCells) { index ->
                            val isRevealed = index < revealedCount
                            val hasDay = index < streakDays.size
                            val dayHistory = if (hasDay) streakDays[index] else null

                            StoryDayCell(
                                dayNumber = index + 1,
                                dayHistory = dayHistory,
                                isRevealed = isRevealed
                            )
                        }
                    }
                }

                // Summary Footer Area
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
                            Row(
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                Surface(
                                    shape = CircleShape,
                                    color = Color(0xFFFF5722),
                                    modifier = Modifier.size(36.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            imageVector = Icons.Default.LocalFireDepartment,
                                            contentDescription = null,
                                            tint = Color.White,
                                            modifier = Modifier.size(22.dp)
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.width(12.dp))

                                Column {
                                    Text(
                                        text = "$currentStreak DAYS LOCKED IN",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Black,
                                        color = Color.White
                                    )
                                    Text(
                                        text = if (currentStreak >= 7) "Discipline in motion • Zero excuses" else "Momentum building • Stay locked in",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = Color.White.copy(alpha = 0.7f)
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

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
fun StoryDayCell(
    dayNumber: Int,
    dayHistory: DayHistory?,
    isRevealed: Boolean,
    modifier: Modifier = Modifier
) {
    val isCompleted = dayHistory?.status == DayStatus.COMPLETED
    val isFrozen = dayHistory?.status == DayStatus.FROZEN

    val animatedScale by animateFloatAsState(
        targetValue = if (isRevealed && dayHistory != null) 1f else 0.85f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "cellScale"
    )

    val backgroundColor = when {
        isRevealed && isCompleted -> Color(0xFF00E676) // Vibrant Emerald
        isRevealed && isFrozen -> Color(0xFF00E5FF) // Ice Cyan
        dayHistory != null -> Color(0xFF1E1D2D) // Unrevealed streak day
        else -> Color(0xFF141320) // Empty future slot
    }

    val contentColor = when {
        isRevealed && isCompleted -> Color(0xFF003816)
        isRevealed && isFrozen -> Color(0xFF00363D)
        else -> Color.White.copy(alpha = 0.25f)
    }

    Box(
        modifier = modifier
            .aspectRatio(1f)
            .scale(animatedScale)
            .clip(RoundedCornerShape(8.dp))
            .background(backgroundColor)
            .then(
                if (dayHistory == null) {
                    Modifier.background(
                        Color(0xFF12111E)
                    )
                } else Modifier
            ),
        contentAlignment = Alignment.Center
    ) {
        if (isRevealed && isCompleted) {
            Icon(
                imageVector = Icons.Default.Check,
                contentDescription = null,
                tint = contentColor,
                modifier = Modifier.size(16.dp)
            )
        } else if (isRevealed && isFrozen) {
            Icon(
                imageVector = Icons.Default.AcUnit,
                contentDescription = null,
                tint = contentColor,
                modifier = Modifier.size(15.dp)
            )
        } else {
            Text(
                text = "$dayNumber",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = contentColor
            )
        }
    }
}
