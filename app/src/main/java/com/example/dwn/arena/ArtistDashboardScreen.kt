package com.example.dwn.arena

import androidx.activity.compose.BackHandler
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import kotlin.math.sin

// ============================================
// DASHBOARD COLORS
// ============================================

private object DashboardColors {
    val gold = Color(0xFFFFD700)
    val amber = Color(0xFFFF8C00)
    val rose = Color(0xFFFF6B9D)
    val violet = Color(0xFF8B5CF6)
    val cyan = Color(0xFF00D9FF)
    val emerald = Color(0xFF10B981)
    val crimson = Color(0xFFEF4444)

    val bgDark = Color(0xFF0A0A0C)
    val bgCard = Color(0xFF12121A)
    val bgElevated = Color(0xFF1A1A24)
    val bgGlass = Color(0x15FFFFFF)

    val textPrimary = Color(0xFFFFFFFF)
    val textSecondary = Color(0xFFB0B0B0)
    val textTertiary = Color(0xFF707070)

    val gradientGold = Brush.horizontalGradient(listOf(gold, amber))
}

// ============================================
// 📊 ARTIST DASHBOARD SCREEN
// ============================================

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ArtistDashboardScreen(
    onBack: () -> Unit,
    onUploadTrack: () -> Unit = {},
    onViewTrack: (String) -> Unit = {},
    onViewAnalytics: () -> Unit = {},
    onManageProfile: () -> Unit = {}
) {
    BackHandler { onBack() }

    val repository = remember { ArenaRepository.getInstance() }
    val currentArtist by repository.currentArtistProfile.collectAsState()
    var selectedTab by remember { mutableIntStateOf(0) }
    val tabs = listOf("Overview", "Tracks", "Analytics", "Audience")

    val scope = rememberCoroutineScope()
    val haptic = LocalHapticFeedback.current

    // Mock data for dashboard
    val myTracks by repository.feedTracks.collectAsState()
    val artistTracks = myTracks.take(5) // Simulating artist's own tracks

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(DashboardColors.bgDark)
    ) {
        // Background
        DashboardBackground()

        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
        ) {
            // Top bar
            DashboardTopBar(
                onBack = onBack,
                onUpload = {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    onUploadTrack()
                },
                onSettings = onManageProfile
            )

            // Tab row
            ScrollableTabRow(
                selectedTabIndex = selectedTab,
                containerColor = Color.Transparent,
                contentColor = Color.White,
                edgePadding = 16.dp,
                indicator = { tabPositions ->
                    if (tabPositions.isNotEmpty() && selectedTab < tabPositions.size) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .wrapContentSize(align = Alignment.BottomStart)
                                .offset(x = tabPositions[selectedTab].left)
                                .width(tabPositions[selectedTab].width)
                                .height(3.dp)
                                .background(
                                    DashboardColors.gradientGold,
                                    RoundedCornerShape(topStart = 3.dp, topEnd = 3.dp)
                                )
                        )
                    }
                },
                divider = {}
            ) {
                tabs.forEachIndexed { index, title ->
                    Tab(
                        selected = selectedTab == index,
                        onClick = { selectedTab = index },
                        text = {
                            Text(
                                title,
                                fontSize = 14.sp,
                                fontWeight = if (selectedTab == index) FontWeight.Bold else FontWeight.Medium
                            )
                        },
                        selectedContentColor = DashboardColors.gold,
                        unselectedContentColor = DashboardColors.textSecondary
                    )
                }
            }

            // Content
            when (selectedTab) {
                0 -> OverviewTab(
                    artistTracks = artistTracks,
                    onUploadTrack = onUploadTrack,
                    onViewTrack = onViewTrack
                )
                1 -> TracksTab(
                    tracks = artistTracks,
                    onTrackClick = onViewTrack,
                    onUploadClick = onUploadTrack
                )
                2 -> AnalyticsTab()
                3 -> AudienceTab()
            }
        }
    }
}

@Composable
private fun DashboardBackground() {
    val transition = rememberInfiniteTransition(label = "bg")
    val wave by transition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(15000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "wave"
    )

    Canvas(modifier = Modifier.fillMaxSize()) {
        val w = size.width
        val h = size.height

        // Gold accent orb
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(
                    DashboardColors.gold.copy(alpha = 0.08f),
                    Color.Transparent
                ),
                radius = w * 0.4f
            ),
            radius = w * 0.35f,
            center = Offset(w * 0.9f, h * 0.1f)
        )

        // Violet accent
        val offsetY = sin(Math.toRadians(wave.toDouble())).toFloat() * h * 0.03f
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(
                    DashboardColors.violet.copy(alpha = 0.05f),
                    Color.Transparent
                ),
                radius = w * 0.25f
            ),
            radius = w * 0.2f,
            center = Offset(w * 0.1f, h * 0.5f + offsetY)
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DashboardTopBar(
    onBack: () -> Unit,
    onUpload: () -> Unit,
    onSettings: () -> Unit
) {
    TopAppBar(
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Default.Dashboard,
                    contentDescription = null,
                    tint = DashboardColors.gold,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text(
                        "Artist Dashboard",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Text(
                        "Manage your music",
                        fontSize = 11.sp,
                        color = DashboardColors.textSecondary
                    )
                }
            }
        },
        navigationIcon = {
            IconButton(onClick = onBack) {
                Icon(
                    Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = Color.White
                )
            }
        },
        actions = {
            IconButton(onClick = onUpload) {
                Icon(
                    Icons.Outlined.CloudUpload,
                    contentDescription = "Upload",
                    tint = DashboardColors.gold
                )
            }
            IconButton(onClick = onSettings) {
                Icon(
                    Icons.Default.Settings,
                    contentDescription = "Settings",
                    tint = DashboardColors.textSecondary
                )
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = Color.Transparent
        )
    )
}

// ============================================
// OVERVIEW TAB
// ============================================

@Composable
private fun OverviewTab(
    artistTracks: List<Track>,
    onUploadTrack: () -> Unit,
    onViewTrack: (String) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 80.dp)
    ) {
        // Quick Stats Cards
        item {
            QuickStatsSection()
        }

        // Performance Chart
        item {
            PerformanceChartCard()
        }

        // Recent Activity
        item {
            RecentActivitySection()
        }

        // Top Tracks
        item {
            TopTracksSection(
                tracks = artistTracks,
                onTrackClick = onViewTrack
            )
        }

        // Quick Actions
        item {
            QuickActionsSection(
                onUploadTrack = onUploadTrack
            )
        }
    }
}

@Composable
private fun QuickStatsSection() {
    Column(
        modifier = Modifier.padding(16.dp)
    ) {
        Text(
            "This Week",
            fontSize = 16.sp,
            fontWeight = FontWeight.SemiBold,
            color = Color.White
        )

        Spacer(modifier = Modifier.height(12.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            StatCard(
                modifier = Modifier.weight(1f),
                value = "24.5K",
                label = "Plays",
                change = "+12%",
                isPositive = true,
                icon = Icons.Default.PlayArrow,
                color = DashboardColors.gold
            )
            StatCard(
                modifier = Modifier.weight(1f),
                value = "1,234",
                label = "New Followers",
                change = "+8%",
                isPositive = true,
                icon = Icons.Default.PersonAdd,
                color = DashboardColors.cyan
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            StatCard(
                modifier = Modifier.weight(1f),
                value = "856",
                label = "Likes",
                change = "+24%",
                isPositive = true,
                icon = Icons.Default.Favorite,
                color = DashboardColors.rose
            )
            StatCard(
                modifier = Modifier.weight(1f),
                value = "23",
                label = "Radio Spins",
                change = "+5",
                isPositive = true,
                icon = Icons.Default.Radio,
                color = DashboardColors.violet
            )
        }
    }
}

@Composable
private fun StatCard(
    modifier: Modifier = Modifier,
    value: String,
    label: String,
    change: String,
    isPositive: Boolean,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    color: Color
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        color = DashboardColors.bgCard,
        border = BorderStroke(1.dp, color.copy(alpha = 0.2f))
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .background(color.copy(alpha = 0.15f), RoundedCornerShape(10.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        icon,
                        contentDescription = null,
                        tint = color,
                        modifier = Modifier.size(20.dp)
                    )
                }

                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = if (isPositive) DashboardColors.emerald.copy(alpha = 0.15f)
                    else DashboardColors.crimson.copy(alpha = 0.15f)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            if (isPositive) Icons.Default.TrendingUp else Icons.Default.TrendingDown,
                            contentDescription = null,
                            tint = if (isPositive) DashboardColors.emerald else DashboardColors.crimson,
                            modifier = Modifier.size(12.dp)
                        )
                        Spacer(modifier = Modifier.width(2.dp))
                        Text(
                            change,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = if (isPositive) DashboardColors.emerald else DashboardColors.crimson
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                value,
                fontSize = 24.sp,
                fontWeight = FontWeight.Black,
                color = Color.White
            )

            Text(
                label,
                fontSize = 12.sp,
                color = DashboardColors.textTertiary
            )
        }
    }
}

@Composable
private fun PerformanceChartCard() {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .height(200.dp),
        shape = RoundedCornerShape(16.dp),
        color = DashboardColors.bgCard
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "Plays Over Time",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.White
                )

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("7D", "30D", "90D").forEach { period ->
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = if (period == "7D") DashboardColors.gold.copy(alpha = 0.2f)
                            else DashboardColors.bgGlass
                        ) {
                            Text(
                                period,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                fontSize = 11.sp,
                                color = if (period == "7D") DashboardColors.gold else DashboardColors.textTertiary
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Simple line chart visualization
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val points = listOf(0.3f, 0.5f, 0.4f, 0.7f, 0.6f, 0.8f, 0.9f)
                    val w = size.width
                    val h = size.height
                    val stepX = w / (points.size - 1)

                    // Draw grid lines
                    for (i in 0..4) {
                        val y = h * i / 4
                        drawLine(
                            color = DashboardColors.bgGlass,
                            start = Offset(0f, y),
                            end = Offset(w, y),
                            strokeWidth = 1f
                        )
                    }

                    // Draw line path
                    val path = Path()
                    points.forEachIndexed { index, value ->
                        val x = index * stepX
                        val y = h * (1 - value)
                        if (index == 0) {
                            path.moveTo(x, y)
                        } else {
                            path.lineTo(x, y)
                        }
                    }

                    drawPath(
                        path = path,
                        brush = Brush.horizontalGradient(
                            listOf(DashboardColors.gold, DashboardColors.amber)
                        ),
                        style = Stroke(width = 3f)
                    )

                    // Draw points
                    points.forEachIndexed { index, value ->
                        val x = index * stepX
                        val y = h * (1 - value)
                        drawCircle(
                            color = DashboardColors.gold,
                            radius = 6f,
                            center = Offset(x, y)
                        )
                        drawCircle(
                            color = DashboardColors.bgCard,
                            radius = 3f,
                            center = Offset(x, y)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // X-axis labels
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun").forEach { day ->
                    Text(
                        day,
                        fontSize = 10.sp,
                        color = DashboardColors.textTertiary
                    )
                }
            }
        }
    }
}

@Composable
private fun RecentActivitySection() {
    Column(
        modifier = Modifier.padding(16.dp)
    ) {
        Text(
            "Recent Activity",
            fontSize = 16.sp,
            fontWeight = FontWeight.SemiBold,
            color = Color.White
        )

        Spacer(modifier = Modifier.height(12.dp))

        val activities = listOf(
            Triple("🎵", "New Radio Spin", "Your track 'Neon Dreams' was played on House Mix FM"),
            Triple("💜", "Milestone", "You reached 10K plays on 'Lagos Nights'"),
            Triple("🔄", "New Remix", "DJ Nova created a remix of your track"),
            Triple("⭐", "New Feature", "You've been featured in 'Fresh Picks' playlist")
        )

        activities.forEach { (emoji, title, description) ->
            ActivityItem(emoji = emoji, title = title, description = description)
        }
    }
}

@Composable
private fun ActivityItem(
    emoji: String,
    title: String,
    description: String
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        shape = RoundedCornerShape(12.dp),
        color = DashboardColors.bgCard.copy(alpha = 0.6f)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                emoji,
                fontSize = 24.sp
            )

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    title,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.White
                )
                Text(
                    description,
                    fontSize = 12.sp,
                    color = DashboardColors.textSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Text(
                "2h ago",
                fontSize = 11.sp,
                color = DashboardColors.textTertiary
            )
        }
    }
}

@Composable
private fun TopTracksSection(
    tracks: List<Track>,
    onTrackClick: (String) -> Unit
) {
    Column(
        modifier = Modifier.padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                "Top Performing Tracks",
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color.White
            )

            TextButton(onClick = { }) {
                Text("See All", color = DashboardColors.gold, fontSize = 13.sp)
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        tracks.take(3).forEachIndexed { index, track ->
            DashboardTrackItem(
                rank = index + 1,
                track = track,
                onClick = { onTrackClick(track.id) }
            )
        }
    }
}

@Composable
private fun DashboardTrackItem(
    rank: Int,
    track: Track,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        shape = RoundedCornerShape(12.dp),
        color = DashboardColors.bgCard.copy(alpha = 0.6f)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Rank
            val rankColor = when (rank) {
                1 -> DashboardColors.gold
                2 -> Color(0xFFC0C0C0)
                3 -> Color(0xFFCD7F32)
                else -> DashboardColors.textTertiary
            }

            Text(
                "#$rank",
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = rankColor,
                modifier = Modifier.width(32.dp)
            )

            // Cover
            Surface(
                modifier = Modifier.size(44.dp),
                shape = RoundedCornerShape(8.dp),
                color = DashboardColors.bgElevated
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        Icons.Default.MusicNote,
                        contentDescription = null,
                        tint = DashboardColors.gold.copy(alpha = 0.5f),
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            // Info
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    track.title,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.White,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    "${formatCount(track.playCount)} plays",
                    fontSize = 12.sp,
                    color = DashboardColors.textTertiary
                )
            }

            // Trend indicator
            Column(horizontalAlignment = Alignment.End) {
                Icon(
                    Icons.AutoMirrored.Filled.TrendingUp,
                    contentDescription = null,
                    tint = DashboardColors.emerald,
                    modifier = Modifier.size(16.dp)
                )
                Text(
                    "+${(5..20).random()}%",
                    fontSize = 11.sp,
                    color = DashboardColors.emerald
                )
            }
        }
    }
}

@Composable
private fun QuickActionsSection(
    onUploadTrack: () -> Unit
) {
    Column(
        modifier = Modifier.padding(16.dp)
    ) {
        Text(
            "Quick Actions",
            fontSize = 16.sp,
            fontWeight = FontWeight.SemiBold,
            color = Color.White
        )

        Spacer(modifier = Modifier.height(12.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            QuickActionButton(
                modifier = Modifier.weight(1f),
                icon = Icons.Outlined.CloudUpload,
                label = "Upload Track",
                color = DashboardColors.gold,
                onClick = onUploadTrack
            )
            QuickActionButton(
                modifier = Modifier.weight(1f),
                icon = Icons.Outlined.Album,
                label = "Create Album",
                color = DashboardColors.violet,
                onClick = { }
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            QuickActionButton(
                modifier = Modifier.weight(1f),
                icon = Icons.Outlined.Campaign,
                label = "Start Challenge",
                color = DashboardColors.rose,
                onClick = { }
            )
            QuickActionButton(
                modifier = Modifier.weight(1f),
                icon = Icons.Outlined.Radio,
                label = "Go Live",
                color = DashboardColors.cyan,
                onClick = { }
            )
        }
    }
}

@Composable
private fun QuickActionButton(
    modifier: Modifier = Modifier,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    color: Color,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        color = color.copy(alpha = 0.1f),
        border = BorderStroke(1.dp, color.copy(alpha = 0.3f))
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                icon,
                contentDescription = null,
                tint = color,
                modifier = Modifier.size(28.dp)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                label,
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                color = color,
                textAlign = TextAlign.Center
            )
        }
    }
}

// ============================================
// TRACKS TAB
// ============================================

@Composable
private fun TracksTab(
    tracks: List<Track>,
    onTrackClick: (String) -> Unit,
    onUploadClick: () -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp)
    ) {
        // Upload button
        item {
            Surface(
                onClick = onUploadClick,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                color = DashboardColors.gold.copy(alpha = 0.1f),
                border = BorderStroke(
                    2.dp,
                    Brush.horizontalGradient(
                        listOf(DashboardColors.gold, DashboardColors.amber)
                    )
                )
            ) {
                Row(
                    modifier = Modifier.padding(20.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(
                        Icons.Outlined.CloudUpload,
                        contentDescription = null,
                        tint = DashboardColors.gold,
                        modifier = Modifier.size(28.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        "Upload New Track",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = DashboardColors.gold
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))
        }

        // Track filters
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf("All", "Public", "Private", "Drafts").forEach { filter ->
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = if (filter == "All") DashboardColors.gold.copy(alpha = 0.2f)
                        else DashboardColors.bgCard
                    ) {
                        Text(
                            filter,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                            fontSize = 13.sp,
                            color = if (filter == "All") DashboardColors.gold else DashboardColors.textSecondary
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }

        // Tracks list
        items(tracks) { track ->
            ManageTrackItem(
                track = track,
                onClick = { onTrackClick(track.id) }
            )
        }

        item {
            Spacer(modifier = Modifier.height(80.dp))
        }
    }
}

@Composable
private fun ManageTrackItem(
    track: Track,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        shape = RoundedCornerShape(12.dp),
        color = DashboardColors.bgCard
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Cover
            Surface(
                modifier = Modifier.size(56.dp),
                shape = RoundedCornerShape(10.dp),
                color = DashboardColors.bgElevated
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        Icons.Default.MusicNote,
                        contentDescription = null,
                        tint = DashboardColors.gold.copy(alpha = 0.5f),
                        modifier = Modifier.size(24.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        track.title,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.White,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )

                    Spacer(modifier = Modifier.width(8.dp))

                    // Status badge
                    val (statusColor, statusText) = when (track.status) {
                        TrackStatus.PUBLIC -> DashboardColors.emerald to "Public"
                        TrackStatus.PRIVATE -> DashboardColors.violet to "Private"
                        TrackStatus.DRAFT -> DashboardColors.amber to "Draft"
                        TrackStatus.SCHEDULED -> DashboardColors.cyan to "Scheduled"
                        else -> DashboardColors.textTertiary to "Unknown"
                    }

                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = statusColor.copy(alpha = 0.15f)
                    ) {
                        Text(
                            statusText,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            color = statusColor
                        )
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                Row(
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.PlayArrow,
                            contentDescription = null,
                            tint = DashboardColors.textTertiary,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            formatCount(track.playCount),
                            fontSize = 12.sp,
                            color = DashboardColors.textTertiary
                        )
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.Favorite,
                            contentDescription = null,
                            tint = DashboardColors.textTertiary,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            formatCount(track.likeCount.toLong()),
                            fontSize = 12.sp,
                            color = DashboardColors.textTertiary
                        )
                    }

                    Text(
                        formatDuration(track.duration),
                        fontSize = 12.sp,
                        color = DashboardColors.textTertiary
                    )
                }
            }

            IconButton(onClick = { /* More options */ }) {
                Icon(
                    Icons.Default.MoreVert,
                    contentDescription = "More",
                    tint = DashboardColors.textTertiary
                )
            }
        }
    }
}

// ============================================
// ANALYTICS TAB
// ============================================

@Composable
private fun AnalyticsTab() {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp)
    ) {
        item {
            Text(
                "Analytics Dashboard",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
            Text(
                "Track your performance over time",
                fontSize = 13.sp,
                color = DashboardColors.textSecondary
            )

            Spacer(modifier = Modifier.height(20.dp))
        }

        // Total stats
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                AnalyticsStat(
                    modifier = Modifier.weight(1f),
                    label = "Total Plays",
                    value = "2.4M",
                    subtitle = "All time"
                )
                AnalyticsStat(
                    modifier = Modifier.weight(1f),
                    label = "Total Followers",
                    value = "15.4K",
                    subtitle = "Active listeners"
                )
            }

            Spacer(modifier = Modifier.height(12.dp))
        }

        // More analytics cards
        item {
            AnalyticsCard(
                title = "Listener Demographics",
                subtitle = "Where your listeners are from"
            ) {
                Column {
                    DemographicBar("United States", 0.35f, "35%")
                    DemographicBar("United Kingdom", 0.22f, "22%")
                    DemographicBar("Germany", 0.15f, "15%")
                    DemographicBar("France", 0.12f, "12%")
                    DemographicBar("Other", 0.16f, "16%")
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }

        item {
            AnalyticsCard(
                title = "Listen Duration",
                subtitle = "How long listeners stay"
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    DurationStat("0-30s", "12%", DashboardColors.crimson)
                    DurationStat("30s-1m", "18%", DashboardColors.amber)
                    DurationStat("1m-2m", "35%", DashboardColors.gold)
                    DurationStat("2m+", "35%", DashboardColors.emerald)
                }
            }

            Spacer(modifier = Modifier.height(80.dp))
        }
    }
}

@Composable
private fun AnalyticsStat(
    modifier: Modifier = Modifier,
    label: String,
    value: String,
    subtitle: String
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        color = DashboardColors.bgCard
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                label,
                fontSize = 12.sp,
                color = DashboardColors.textTertiary
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                value,
                fontSize = 28.sp,
                fontWeight = FontWeight.Black,
                color = DashboardColors.gold
            )
            Text(
                subtitle,
                fontSize = 11.sp,
                color = DashboardColors.textTertiary
            )
        }
    }
}

@Composable
private fun AnalyticsCard(
    title: String,
    subtitle: String,
    content: @Composable () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = DashboardColors.bgCard
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                title,
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color.White
            )
            Text(
                subtitle,
                fontSize = 12.sp,
                color = DashboardColors.textTertiary
            )
            Spacer(modifier = Modifier.height(16.dp))
            content()
        }
    }
}

@Composable
private fun DemographicBar(
    country: String,
    percentage: Float,
    percentLabel: String
) {
    Column(
        modifier = Modifier.padding(vertical = 4.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(country, fontSize = 13.sp, color = Color.White)
            Text(percentLabel, fontSize = 13.sp, color = DashboardColors.gold)
        }
        Spacer(modifier = Modifier.height(4.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(6.dp)
                .clip(RoundedCornerShape(3.dp))
                .background(DashboardColors.bgGlass)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(percentage)
                    .fillMaxHeight()
                    .background(
                        Brush.horizontalGradient(
                            listOf(DashboardColors.gold, DashboardColors.amber)
                        ),
                        RoundedCornerShape(3.dp)
                    )
            )
        }
    }
}

@Composable
private fun DurationStat(
    range: String,
    percentage: String,
    color: Color
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            percentage,
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            color = color
        )
        Text(
            range,
            fontSize = 11.sp,
            color = DashboardColors.textTertiary
        )
    }
}

// ============================================
// AUDIENCE TAB
// ============================================

@Composable
private fun AudienceTab() {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp)
    ) {
        item {
            Text(
                "Your Audience",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
            Text(
                "Understand who's listening to your music",
                fontSize = 13.sp,
                color = DashboardColors.textSecondary
            )

            Spacer(modifier = Modifier.height(20.dp))
        }

        // Follower growth
        item {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                color = DashboardColors.bgCard
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text(
                        "Follower Growth",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.White
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text(
                                "15,420",
                                fontSize = 32.sp,
                                fontWeight = FontWeight.Black,
                                color = DashboardColors.gold
                            )
                            Text(
                                "Total Followers",
                                fontSize = 12.sp,
                                color = DashboardColors.textTertiary
                            )
                        }

                        Column(horizontalAlignment = Alignment.End) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    Icons.AutoMirrored.Filled.TrendingUp,
                                    contentDescription = null,
                                    tint = DashboardColors.emerald,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    "+1,234",
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = DashboardColors.emerald
                                )
                            }
                            Text(
                                "This month",
                                fontSize = 11.sp,
                                color = DashboardColors.textTertiary
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }

        // Top fans
        item {
            Text(
                "Super Fans",
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color.White
            )
            Text(
                "Your most engaged listeners",
                fontSize = 12.sp,
                color = DashboardColors.textTertiary
            )

            Spacer(modifier = Modifier.height(12.dp))
        }

        items(5) { index ->
            FanItem(
                rank = index + 1,
                name = listOf("MusicLover99", "BeatJunkie", "NightOwl", "SoundSeeker", "VibeCheck")[index],
                plays = "${(100 - index * 15)}+ plays",
                isSupporter = index < 2
            )
        }

        item {
            Spacer(modifier = Modifier.height(80.dp))
        }
    }
}

@Composable
private fun FanItem(
    rank: Int,
    name: String,
    plays: String,
    isSupporter: Boolean
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        shape = RoundedCornerShape(12.dp),
        color = DashboardColors.bgCard.copy(alpha = 0.6f)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Rank medal
            val medalColor = when (rank) {
                1 -> DashboardColors.gold
                2 -> Color(0xFFC0C0C0)
                3 -> Color(0xFFCD7F32)
                else -> DashboardColors.textTertiary
            }

            if (rank <= 3) {
                Icon(
                    Icons.Default.EmojiEvents,
                    contentDescription = null,
                    tint = medalColor,
                    modifier = Modifier.size(24.dp)
                )
            } else {
                Text(
                    "#$rank",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = DashboardColors.textTertiary,
                    modifier = Modifier.width(24.dp)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            // Avatar
            Surface(
                modifier = Modifier.size(40.dp),
                shape = CircleShape,
                color = DashboardColors.bgElevated
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        name.take(2).uppercase(),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = DashboardColors.gold
                    )
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        name,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.White
                    )
                    if (isSupporter) {
                        Spacer(modifier = Modifier.width(6.dp))
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = DashboardColors.gold.copy(alpha = 0.2f)
                        ) {
                            Text(
                                "SUPPORTER",
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                fontSize = 8.sp,
                                fontWeight = FontWeight.Bold,
                                color = DashboardColors.gold
                            )
                        }
                    }
                }
                Text(
                    plays,
                    fontSize = 12.sp,
                    color = DashboardColors.textTertiary
                )
            }

            IconButton(onClick = { }) {
                Icon(
                    Icons.Outlined.PersonAdd,
                    contentDescription = "Follow back",
                    tint = DashboardColors.gold,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

// ============================================
// UTILITY FUNCTIONS
// ============================================

private fun formatCount(count: Long): String {
    return when {
        count >= 1_000_000 -> String.format("%.1fM", count / 1_000_000.0)
        count >= 1_000 -> String.format("%.1fK", count / 1_000.0)
        else -> count.toString()
    }
}

private fun formatDuration(durationMs: Long): String {
    val totalSeconds = durationMs / 1000
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return "%d:%02d".format(minutes, seconds)
}

