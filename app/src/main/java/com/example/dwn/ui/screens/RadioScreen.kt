package com.example.dwn.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.*
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.dwn.radio.*
import kotlin.math.sin

// ============================================
// 🎨 RADIO COLORS
// ============================================

private object RadioColors {
    val pink = Color(0xFFE91E63)
    val pinkLight = Color(0xFFFF4081)
    val purple = Color(0xFF9C27B0)
    val blue = Color(0xFF2196F3)
    val cyan = Color(0xFF00BCD4)
    val teal = Color(0xFF009688)
    val green = Color(0xFF4CAF50)
    val orange = Color(0xFFFF5722)
    val amber = Color(0xFFFFC107)
    val red = Color(0xFFE53935)

    val bgDark = Color(0xFF0A0A0F)
    val bgMid = Color(0xFF101018)
    val surface = Color(0xFF161620)
    val surfaceVariant = Color(0xFF1E1E2A)
    val card = Color(0xFF222230)

    val textPrimary = Color(0xFFFFFFFF)
    val textSecondary = Color(0xFFB0B0B8)
    val textTertiary = Color(0xFF707080)

    val glassWhite = Color(0x14FFFFFF)
    val glassBorder = Color(0x20FFFFFF)

    val live = Color(0xFFE53935)
}

// ============================================
// 📻 RADIO SCREEN
// ============================================

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RadioScreen(
    onBack: () -> Unit
) {
    var isPlaying by remember { mutableStateOf(false) }
    var currentStation by remember { mutableStateOf<RadioStation?>(null) }
    var showSearch by remember { mutableStateOf(false) }
    var searchText by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf<String?>(null) }
    var showStationDetails by remember { mutableStateOf<RadioStation?>(null) }
    var showSleepTimer by remember { mutableStateOf(false) }

    // Mock now playing
    var nowPlaying by remember {
        mutableStateOf(
            NowPlaying(
                stationId = "",
                trackTitle = "Blinding Lights",
                artistName = "The Weeknd",
                albumName = "After Hours",
                isLive = true
            )
        )
    }

    // Data
    val smartStations = remember { defaultSmartStations }
    val traditionalStations = remember { sampleTraditionalStations }
    val categories = remember { defaultGenreCategories + defaultMoodCategories }

    // Recently played (mock)
    var recentlyPlayed by remember { mutableStateOf<List<RadioStation>>(emptyList()) }

    // Favorites
    var favoriteStations by remember { mutableStateOf<Set<String>>(emptySet()) }

    val filteredStations = remember(selectedCategory) {
        if (selectedCategory == null) {
            traditionalStations
        } else {
            when (selectedCategory) {
                "pop" -> traditionalStations.filter { RadioGenre.POP in it.genres }
                "rock" -> traditionalStations.filter { RadioGenre.ROCK in it.genres }
                "electronic" -> traditionalStations.filter { RadioGenre.ELECTRONIC in it.genres }
                "jazz" -> traditionalStations.filter { RadioGenre.JAZZ in it.genres }
                "classical" -> traditionalStations.filter { RadioGenre.CLASSICAL in it.genres }
                "chill" -> traditionalStations.filter { RadioGenre.CHILL in it.genres || RadioGenre.AMBIENT in it.genres }
                else -> traditionalStations
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(RadioColors.bgDark)
    ) {
        // Animated background
        RadioBackground(isPlaying = isPlaying, stationColor = currentStation?.accentColor)

        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
        ) {
            // Top Bar
            RadioTopBar(
                onBack = onBack,
                onSearchClick = { showSearch = !showSearch },
                onTimerClick = { showSleepTimer = true }
            )

            // Search bar
            AnimatedVisibility(
                visible = showSearch,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                RadioSearchBar(
                    searchText = searchText,
                    onSearchTextChange = { searchText = it },
                    onClear = { searchText = "" }
                )
            }

            // Main content
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = if (currentStation != null) 180.dp else 100.dp)
            ) {
                // Now Playing Card (if playing)
                if (currentStation != null) {
                    item {
                        NowPlayingCard(
                            station = currentStation!!,
                            nowPlaying = nowPlaying,
                            isPlaying = isPlaying,
                            onPlayPause = { isPlaying = !isPlaying },
                            onStationClick = { showStationDetails = currentStation }
                        )
                        Spacer(modifier = Modifier.height(24.dp))
                    }
                }

                // Smart Radio Section
                item {
                    SectionHeader(
                        title = "Smart Radio",
                        subtitle = "Personalized for you",
                        icon = "🤖"
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                }

                item {
                    SmartRadioRow(
                        stations = smartStations,
                        currentStationId = currentStation?.id,
                        onStationClick = { station ->
                            currentStation = station
                            isPlaying = true
                            nowPlaying = nowPlaying.copy(
                                stationId = station.id,
                                trackTitle = "AI Generated Mix",
                                artistName = station.name,
                                isLive = false
                            )
                            // Add to recently played
                            recentlyPlayed = (listOf(station) + recentlyPlayed.filter { it.id != station.id }).take(10)
                        }
                    )
                    Spacer(modifier = Modifier.height(28.dp))
                }

                // Recently Played Section (if any)
                if (recentlyPlayed.isNotEmpty()) {
                    item {
                        SectionHeader(
                            title = "Recently Played",
                            subtitle = "Continue listening",
                            icon = "🕐"
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                    }

                    item {
                        RecentlyPlayedRow(
                            stations = recentlyPlayed.take(5),
                            currentStationId = currentStation?.id,
                            onStationClick = { station ->
                                currentStation = station
                                isPlaying = true
                            }
                        )
                        Spacer(modifier = Modifier.height(28.dp))
                    }
                }

                // Trending / Featured Section
                item {
                    SectionHeader(
                        title = "Trending Now",
                        subtitle = "Popular with listeners",
                        icon = "🔥"
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                }

                item {
                    TrendingStationsRow(
                        stations = traditionalStations.sortedByDescending { it.listenerCount }.take(5),
                        currentStationId = currentStation?.id,
                        onStationClick = { station ->
                            currentStation = station
                            isPlaying = true
                            recentlyPlayed = (listOf(station) + recentlyPlayed.filter { it.id != station.id }).take(10)
                        }
                    )
                    Spacer(modifier = Modifier.height(28.dp))
                }

                // Categories
                item {
                    SectionHeader(
                        title = "Browse",
                        subtitle = "Explore by genre & mood",
                        icon = "🎵"
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                }

                item {
                    CategoriesRow(
                        categories = categories,
                        selectedCategory = selectedCategory,
                        onCategoryClick = { id ->
                            selectedCategory = if (selectedCategory == id) null else id
                        }
                    )
                    Spacer(modifier = Modifier.height(24.dp))
                }

                // Live Stations
                item {
                    SectionHeader(
                        title = if (selectedCategory != null) {
                            categories.find { it.id == selectedCategory }?.name ?: "Stations"
                        } else "Live Radio",
                        subtitle = "${filteredStations.size} stations",
                        icon = "📻"
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                }

                items(filteredStations) { station ->
                    StationListItem(
                        station = station,
                        isPlaying = currentStation?.id == station.id && isPlaying,
                        isCurrent = currentStation?.id == station.id,
                        onStationClick = {
                            currentStation = station
                            isPlaying = true
                            nowPlaying = nowPlaying.copy(
                                stationId = station.id,
                                isLive = true
                            )
                        },
                        onFavoriteClick = { /* Toggle favorite */ }
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                }
            }
        }

        // Mini Player (when playing and scrolled)
        if (currentStation != null) {
            MiniPlayer(
                station = currentStation!!,
                nowPlaying = nowPlaying,
                isPlaying = isPlaying,
                onPlayPause = { isPlaying = !isPlaying },
                onExpand = { showStationDetails = currentStation },
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .navigationBarsPadding()
            )
        }
    }

    // Station Details Sheet
    if (showStationDetails != null) {
        StationDetailsSheet(
            station = showStationDetails!!,
            nowPlaying = if (currentStation?.id == showStationDetails?.id) nowPlaying else null,
            isPlaying = currentStation?.id == showStationDetails?.id && isPlaying,
            onDismiss = { showStationDetails = null },
            onPlay = {
                currentStation = showStationDetails
                isPlaying = true
            },
            onPlayPause = { isPlaying = !isPlaying }
        )
    }

    // Sleep Timer Dialog
    if (showSleepTimer) {
        SleepTimerDialog(
            onDismiss = { showSleepTimer = false },
            onSetTimer = { minutes ->
                showSleepTimer = false
                // Set timer
            }
        )
    }
}

// ============================================
// 🌌 ANIMATED BACKGROUND
// ============================================

@Composable
private fun RadioBackground(
    isPlaying: Boolean,
    stationColor: Long?
) {
    val transition = rememberInfiniteTransition(label = "bg")

    val pulse by transition.animateFloat(
        initialValue = 0.5f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(if (isPlaying) 2000 else 4000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse"
    )

    val wave by transition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(8000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "wave"
    )

    val color = stationColor?.let { Color(it) } ?: RadioColors.pink

    Canvas(modifier = Modifier.fillMaxSize()) {
        drawRect(
            brush = Brush.verticalGradient(
                listOf(
                    RadioColors.bgDark,
                    Color(0xFF0D0815),
                    RadioColors.bgMid,
                    RadioColors.bgDark
                )
            )
        )

        if (isPlaying) {
            // Animated sound wave circles
            for (i in 0..2) {
                val offset = i * 120f
                val waveOffset = sin(Math.toRadians((wave + offset).toDouble())).toFloat()

                drawCircle(
                    brush = Brush.radialGradient(
                        listOf(
                            color.copy(alpha = 0.08f * pulse * (1f - i * 0.2f)),
                            Color.Transparent
                        )
                    ),
                    radius = (300f + i * 100f + waveOffset * 30f) * pulse,
                    center = Offset(size.width * 0.5f, size.height * 0.25f)
                )
            }
        }

        // Static orbs
        drawCircle(
            brush = Brush.radialGradient(
                listOf(
                    color.copy(alpha = 0.06f),
                    Color.Transparent
                )
            ),
            radius = 350f,
            center = Offset(size.width * 0.8f, size.height * 0.7f)
        )
    }
}

// ============================================
// 📱 TOP BAR
// ============================================

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun RadioTopBar(
    onBack: () -> Unit,
    onSearchClick: () -> Unit,
    onTimerClick: () -> Unit
) {
    TopAppBar(
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("📻", fontSize = 24.sp)
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        "Radio",
                        fontWeight = FontWeight.Bold,
                        fontSize = 20.sp
                    )
                    Text(
                        "Live & Smart Stations",
                        color = RadioColors.textTertiary,
                        fontSize = 11.sp
                    )
                }
            }
        },
        navigationIcon = {
            IconButton(onClick = onBack) {
                Icon(Icons.Default.ArrowBack, "Back")
            }
        },
        actions = {
            IconButton(onClick = onSearchClick) {
                Icon(Icons.Default.Search, "Search")
            }
            IconButton(onClick = onTimerClick) {
                Icon(Icons.Outlined.Timer, "Sleep Timer")
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = Color.Transparent,
            titleContentColor = RadioColors.textPrimary,
            navigationIconContentColor = RadioColors.textSecondary,
            actionIconContentColor = RadioColors.textSecondary
        )
    )
}

// ============================================
// 🔍 SEARCH BAR
// ============================================

@Composable
private fun RadioSearchBar(
    searchText: String,
    onSearchTextChange: (String) -> Unit,
    onClear: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        shape = RoundedCornerShape(16.dp),
        color = RadioColors.surfaceVariant
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Icons.Default.Search,
                null,
                tint = RadioColors.textTertiary,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            TextField(
                value = searchText,
                onValueChange = onSearchTextChange,
                placeholder = { Text("Search stations...", color = RadioColors.textTertiary) },
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = Color.Transparent,
                    unfocusedContainerColor = Color.Transparent,
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent,
                    focusedTextColor = RadioColors.textPrimary,
                    unfocusedTextColor = RadioColors.textPrimary
                ),
                modifier = Modifier.weight(1f),
                singleLine = true
            )
            if (searchText.isNotEmpty()) {
                IconButton(onClick = onClear, modifier = Modifier.size(32.dp)) {
                    Icon(Icons.Default.Close, "Clear", tint = RadioColors.textSecondary, modifier = Modifier.size(18.dp))
                }
            }
        }
    }
}

// ============================================
// 🎵 NOW PLAYING CARD
// ============================================

@Composable
private fun NowPlayingCard(
    station: RadioStation,
    nowPlaying: NowPlaying,
    isPlaying: Boolean,
    onPlayPause: () -> Unit,
    onStationClick: () -> Unit
) {
    val stationColor = Color(station.accentColor)

    val transition = rememberInfiniteTransition(label = "np")
    val glow by transition.animateFloat(
        initialValue = 0.6f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500),
            repeatMode = RepeatMode.Reverse
        ),
        label = "glow"
    )

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        shape = RoundedCornerShape(24.dp),
        color = Color.Transparent,
        onClick = onStationClick
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.linearGradient(
                        listOf(
                            stationColor.copy(alpha = 0.9f),
                            stationColor.copy(alpha = 0.6f)
                        )
                    ),
                    RoundedCornerShape(24.dp)
                )
                .then(
                    if (isPlaying) {
                        Modifier.drawBehind {
                            drawRoundRect(
                                color = stationColor.copy(alpha = 0.3f * glow),
                                cornerRadius = androidx.compose.ui.geometry.CornerRadius(24.dp.toPx()),
                                style = androidx.compose.ui.graphics.drawscope.Stroke(width = 3.dp.toPx())
                            )
                        }
                    } else Modifier
                )
                .padding(20.dp)
        ) {
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        // Live indicator
                        if (nowPlaying.isLive) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(8.dp)
                                        .background(RadioColors.live, CircleShape)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    "LIVE",
                                    color = Color.White,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 1.sp
                                )
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                        }

                        Text(
                            station.name,
                            color = Color.White,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium
                        )

                        Spacer(modifier = Modifier.height(4.dp))

                        // Track info
                        if (nowPlaying.trackTitle != null) {
                            Text(
                                nowPlaying.trackTitle,
                                color = Color.White,
                                fontSize = 22.sp,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            if (nowPlaying.artistName != null) {
                                Text(
                                    nowPlaying.artistName,
                                    color = Color.White.copy(alpha = 0.85f),
                                    fontSize = 15.sp
                                )
                            }
                        } else if (nowPlaying.showName != null) {
                            Text(
                                nowPlaying.showName,
                                color = Color.White,
                                fontSize = 22.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    // Play/Pause button
                    FloatingActionButton(
                        onClick = onPlayPause,
                        containerColor = Color.White,
                        modifier = Modifier.size(56.dp)
                    ) {
                        Icon(
                            if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                            "Play/Pause",
                            tint = stationColor,
                            modifier = Modifier.size(32.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Visualizer placeholder
                if (isPlaying) {
                    AudioVisualizer(color = Color.White)
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Actions
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    ActionChip(icon = Icons.Default.FavoriteBorder, label = "Like")
                    ActionChip(icon = Icons.Default.Bookmark, label = "Save")
                    ActionChip(icon = Icons.Default.ContentCut, label = "Clip")
                    ActionChip(icon = Icons.Default.Share, label = "Share")
                }
            }
        }
    }
}

@Composable
private fun AudioVisualizer(color: Color) {
    val transition = rememberInfiniteTransition(label = "vis")

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(24.dp),
        horizontalArrangement = Arrangement.spacedBy(3.dp),
        verticalAlignment = Alignment.Bottom
    ) {
        repeat(20) { index ->
            val height by transition.animateFloat(
                initialValue = 4f,
                targetValue = 24f,
                animationSpec = infiniteRepeatable(
                    animation = tween(
                        durationMillis = (300..600).random(),
                        delayMillis = index * 30,
                        easing = FastOutSlowInEasing
                    ),
                    repeatMode = RepeatMode.Reverse
                ),
                label = "bar$index"
            )

            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(height.dp)
                    .background(color.copy(alpha = 0.7f), RoundedCornerShape(2.dp))
            )
        }
    }
}

@Composable
private fun ActionChip(
    icon: ImageVector,
    label: String
) {
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = Color.White.copy(alpha = 0.15f),
        onClick = { }
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(icon, null, tint = Color.White, modifier = Modifier.size(14.dp))
            Spacer(modifier = Modifier.width(4.dp))
            Text(label, color = Color.White, fontSize = 11.sp)
        }
    }
}

// ============================================
// 🤖 SMART RADIO ROW
// ============================================

@Composable
private fun SmartRadioRow(
    stations: List<RadioStation>,
    currentStationId: String?,
    onStationClick: (RadioStation) -> Unit
) {
    LazyRow(
        contentPadding = PaddingValues(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        items(stations) { station ->
            SmartStationCard(
                station = station,
                isPlaying = station.id == currentStationId,
                onClick = { onStationClick(station) }
            )
        }
    }
}

@Composable
private fun SmartStationCard(
    station: RadioStation,
    isPlaying: Boolean,
    onClick: () -> Unit
) {
    val stationColor = Color(station.accentColor)

    Surface(
        modifier = Modifier.width(150.dp),
        shape = RoundedCornerShape(18.dp),
        color = Color.Transparent,
        onClick = onClick
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.linearGradient(
                        listOf(stationColor, stationColor.copy(alpha = 0.7f)),
                        start = Offset.Zero,
                        end = Offset(150f, 150f)
                    ),
                    RoundedCornerShape(18.dp)
                )
                .border(
                    width = if (isPlaying) 2.dp else 0.dp,
                    color = if (isPlaying) Color.White else Color.Transparent,
                    shape = RoundedCornerShape(18.dp)
                )
                .padding(16.dp)
        ) {
            Column {
                // Icon
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .background(Color.White.copy(alpha = 0.2f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.AutoAwesome,
                        null,
                        tint = Color.White,
                        modifier = Modifier.size(24.dp)
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                Text(
                    station.name,
                    color = Color.White,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    station.tagline,
                    color = Color.White.copy(alpha = 0.8f),
                    fontSize = 11.sp,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )

                if (isPlaying) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.PlayArrow,
                            null,
                            tint = Color.White,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            "Playing",
                            color = Color.White,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
        }
    }
}

// ============================================
// 📂 CATEGORIES ROW
// ============================================

@Composable
private fun CategoriesRow(
    categories: List<StationCategory>,
    selectedCategory: String?,
    onCategoryClick: (String) -> Unit
) {
    LazyRow(
        contentPadding = PaddingValues(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        items(categories.take(10)) { category ->
            CategoryChip(
                category = category,
                isSelected = category.id == selectedCategory,
                onClick = { onCategoryClick(category.id) }
            )
        }
    }
}

// ============================================
// 🕐 RECENTLY PLAYED ROW
// ============================================

@Composable
private fun RecentlyPlayedRow(
    stations: List<RadioStation>,
    currentStationId: String?,
    onStationClick: (RadioStation) -> Unit
) {
    LazyRow(
        contentPadding = PaddingValues(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(stations) { station ->
            RecentStationCard(
                station = station,
                isPlaying = station.id == currentStationId,
                onClick = { onStationClick(station) }
            )
        }
    }
}

@Composable
private fun RecentStationCard(
    station: RadioStation,
    isPlaying: Boolean,
    onClick: () -> Unit
) {
    val stationColor = Color(station.accentColor)

    Surface(
        modifier = Modifier.width(120.dp),
        shape = RoundedCornerShape(14.dp),
        color = RadioColors.card,
        border = if (isPlaying) BorderStroke(2.dp, stationColor) else BorderStroke(1.dp, RadioColors.glassBorder),
        onClick = onClick
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .background(stationColor.copy(alpha = 0.2f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text(station.type.icon, fontSize = 22.sp)
            }

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                station.name,
                color = RadioColors.textPrimary,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center
            )

            if (isPlaying) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    "▶ Playing",
                    color = stationColor,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}

// ============================================
// 🔥 TRENDING STATIONS ROW
// ============================================

@Composable
private fun TrendingStationsRow(
    stations: List<RadioStation>,
    currentStationId: String?,
    onStationClick: (RadioStation) -> Unit
) {
    LazyRow(
        contentPadding = PaddingValues(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        itemsIndexed(stations) { index, station ->
            TrendingStationCard(
                station = station,
                rank = index + 1,
                isPlaying = station.id == currentStationId,
                onClick = { onStationClick(station) }
            )
        }
    }
}

@Composable
private fun TrendingStationCard(
    station: RadioStation,
    rank: Int,
    isPlaying: Boolean,
    onClick: () -> Unit
) {
    val stationColor = Color(station.accentColor)

    Surface(
        modifier = Modifier.width(160.dp),
        shape = RoundedCornerShape(16.dp),
        color = RadioColors.card,
        border = if (isPlaying) BorderStroke(2.dp, stationColor) else BorderStroke(1.dp, RadioColors.glassBorder),
        onClick = onClick
    ) {
        Box {
            Column(modifier = Modifier.padding(14.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .background(stationColor.copy(alpha = 0.2f), RoundedCornerShape(10.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(station.type.icon, fontSize = 22.sp)
                    }

                    // Rank badge
                    Surface(
                        shape = CircleShape,
                        color = when (rank) {
                            1 -> Color(0xFFFFD700)
                            2 -> Color(0xFFC0C0C0)
                            3 -> Color(0xFFCD7F32)
                            else -> RadioColors.surfaceVariant
                        }
                    ) {
                        Text(
                            "#$rank",
                            color = if (rank <= 3) Color.Black else RadioColors.textSecondary,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    station.name,
                    color = RadioColors.textPrimary,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(4.dp))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        "👥 ${formatListenerCount(station.listenerCount)}",
                        color = RadioColors.textTertiary,
                        fontSize = 11.sp
                    )

                    if (station.isLive) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .background(RadioColors.live, CircleShape)
                        )
                    }
                }

                if (isPlaying) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.PlayArrow,
                            null,
                            tint = stationColor,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            "Now Playing",
                            color = stationColor,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun CategoryChip(
    category: StationCategory,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val color = Color(category.color)

    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(12.dp),
        color = if (isSelected) color else RadioColors.surfaceVariant,
        border = if (!isSelected) BorderStroke(1.dp, RadioColors.glassBorder) else null
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(category.icon, fontSize = 16.sp)
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                category.name,
                color = if (isSelected) Color.White else RadioColors.textSecondary,
                fontSize = 13.sp,
                fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal
            )
        }
    }
}

// ============================================
// 📻 STATION LIST ITEM
// ============================================

@Composable
private fun StationListItem(
    station: RadioStation,
    isPlaying: Boolean,
    isCurrent: Boolean,
    onStationClick: () -> Unit,
    onFavoriteClick: () -> Unit
) {
    val stationColor = Color(station.accentColor)

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        shape = RoundedCornerShape(16.dp),
        color = if (isCurrent) stationColor.copy(alpha = 0.15f) else RadioColors.card,
        border = if (isCurrent) BorderStroke(1.dp, stationColor.copy(alpha = 0.5f)) else BorderStroke(1.dp, RadioColors.glassBorder),
        onClick = onStationClick
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Station logo
            Box(
                modifier = Modifier
                    .size(52.dp)
                    .background(stationColor.copy(alpha = 0.2f), RoundedCornerShape(12.dp)),
                contentAlignment = Alignment.Center
            ) {
                Text(station.type.icon, fontSize = 24.sp)

                // Playing indicator
                if (isPlaying) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .offset(x = 4.dp, y = 4.dp)
                            .size(18.dp)
                            .background(stationColor, CircleShape)
                            .border(2.dp, RadioColors.card, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.PlayArrow,
                            null,
                            tint = Color.White,
                            modifier = Modifier.size(12.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.width(14.dp))

            // Info
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        station.name,
                        color = RadioColors.textPrimary,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false)
                    )

                    if (station.isLive) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = RadioColors.live.copy(alpha = 0.2f)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(5.dp)
                                        .background(RadioColors.live, CircleShape)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    "LIVE",
                                    color = RadioColors.live,
                                    fontSize = 8.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    station.description,
                    color = RadioColors.textTertiary,
                    fontSize = 12.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(6.dp))

                // Tags
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    station.genres.take(2).forEach { genre ->
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = Color(genre.color).copy(alpha = 0.15f)
                        ) {
                            Text(
                                genre.displayName,
                                color = Color(genre.color),
                                fontSize = 9.sp,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }

                    if (station.listenerCount > 0) {
                        Text(
                            "👥 ${formatListenerCount(station.listenerCount)}",
                            color = RadioColors.textTertiary,
                            fontSize = 10.sp
                        )
                    }
                }
            }

            // Favorite
            IconButton(onClick = onFavoriteClick) {
                Icon(
                    if (station.isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                    "Favorite",
                    tint = if (station.isFavorite) RadioColors.pink else RadioColors.textTertiary,
                    modifier = Modifier.size(22.dp)
                )
            }
        }
    }
}

// ============================================
// 🎵 MINI PLAYER
// ============================================

@Composable
private fun MiniPlayer(
    station: RadioStation,
    nowPlaying: NowPlaying,
    isPlaying: Boolean,
    onPlayPause: () -> Unit,
    onExpand: () -> Unit,
    modifier: Modifier = Modifier
) {
    val stationColor = Color(station.accentColor)

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 8.dp),
        shape = RoundedCornerShape(20.dp),
        color = RadioColors.card,
        shadowElevation = 16.dp,
        onClick = onExpand
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Station icon
            Box(
                modifier = Modifier
                    .size(46.dp)
                    .background(stationColor.copy(alpha = 0.2f), RoundedCornerShape(12.dp)),
                contentAlignment = Alignment.Center
            ) {
                Text(station.type.icon, fontSize = 22.sp)
            }

            Spacer(modifier = Modifier.width(12.dp))

            // Info
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    nowPlaying.trackTitle ?: nowPlaying.showName ?: station.name,
                    color = RadioColors.textPrimary,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    nowPlaying.artistName ?: station.name,
                    color = RadioColors.textTertiary,
                    fontSize = 12.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            // Controls
            Row(
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Live indicator
                if (nowPlaying.isLive) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .background(RadioColors.live, CircleShape)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                }

                IconButton(onClick = onPlayPause) {
                    Icon(
                        if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                        "Play/Pause",
                        tint = stationColor,
                        modifier = Modifier.size(32.dp)
                    )
                }
            }
        }
    }
}

// ============================================
// 📄 SECTION HEADER
// ============================================

@Composable
private fun SectionHeader(
    title: String,
    subtitle: String,
    icon: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(icon, fontSize = 20.sp)
            Spacer(modifier = Modifier.width(10.dp))
            Column {
                Text(
                    title,
                    color = RadioColors.textPrimary,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    subtitle,
                    color = RadioColors.textTertiary,
                    fontSize = 12.sp
                )
            }
        }

        TextButton(onClick = { }) {
            Text("See All", color = RadioColors.pink, fontSize = 12.sp)
        }
    }
}

// ============================================
// 📋 STATION DETAILS SHEET
// ============================================

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun StationDetailsSheet(
    station: RadioStation,
    nowPlaying: NowPlaying?,
    isPlaying: Boolean,
    onDismiss: () -> Unit,
    onPlay: () -> Unit,
    onPlayPause: () -> Unit
) {
    val stationColor = Color(station.accentColor)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = RadioColors.surface,
        dragHandle = {
            Box(
                modifier = Modifier
                    .padding(vertical = 12.dp)
                    .width(40.dp)
                    .height(4.dp)
                    .background(RadioColors.glassBorder, RoundedCornerShape(2.dp))
            )
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(bottom = 32.dp)
        ) {
            // Header
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(80.dp)
                        .background(stationColor.copy(alpha = 0.2f), RoundedCornerShape(20.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(station.type.icon, fontSize = 40.sp)
                }

                Spacer(modifier = Modifier.width(16.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        station.name,
                        color = RadioColors.textPrimary,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        station.tagline,
                        color = RadioColors.textTertiary,
                        fontSize = 14.sp
                    )

                    if (station.country != null) {
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("🌍", fontSize = 12.sp)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                station.country,
                                color = RadioColors.textSecondary,
                                fontSize = 12.sp
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Play button
            Button(
                onClick = if (nowPlaying != null) onPlayPause else onPlay,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp),
                colors = ButtonDefaults.buttonColors(containerColor = stationColor),
                shape = RoundedCornerShape(16.dp)
            ) {
                Icon(
                    if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                    null,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    if (isPlaying) "Pause" else "Play Now",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Now playing info
            if (nowPlaying != null && isPlaying) {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    color = stationColor.copy(alpha = 0.1f)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            "Now Playing",
                            color = stationColor,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            nowPlaying.trackTitle ?: nowPlaying.showName ?: "Unknown",
                            color = RadioColors.textPrimary,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                        if (nowPlaying.artistName != null) {
                            Text(
                                nowPlaying.artistName,
                                color = RadioColors.textSecondary,
                                fontSize = 14.sp
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(20.dp))
            }

            // Description
            Text(
                station.description,
                color = RadioColors.textSecondary,
                fontSize = 14.sp,
                lineHeight = 22.sp
            )

            Spacer(modifier = Modifier.height(20.dp))

            // Stats
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                StatColumn(value = "${station.streamQuality.bitrate}", label = "kbps")
                StatColumn(value = formatListenerCount(station.listenerCount), label = "listeners")
                StatColumn(value = station.genres.firstOrNull()?.displayName ?: "-", label = "genre")
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Actions
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedButton(
                    onClick = { },
                    modifier = Modifier.weight(1f),
                    border = BorderStroke(1.dp, RadioColors.glassBorder),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Default.FavoriteBorder, null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Favorite")
                }
                OutlinedButton(
                    onClick = { },
                    modifier = Modifier.weight(1f),
                    border = BorderStroke(1.dp, RadioColors.glassBorder),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Default.Share, null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Share")
                }
            }
        }
    }
}

@Composable
private fun StatColumn(value: String, label: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            value,
            color = RadioColors.textPrimary,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold
        )
        Text(
            label,
            color = RadioColors.textTertiary,
            fontSize = 11.sp
        )
    }
}

// ============================================
// ⏰ SLEEP TIMER DIALOG
// ============================================

@Composable
private fun SleepTimerDialog(
    onDismiss: () -> Unit,
    onSetTimer: (Int) -> Unit
) {
    val options = listOf(15, 30, 45, 60, 90, 120)

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = RadioColors.surface,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Outlined.Timer, null, tint = RadioColors.pink)
                Spacer(modifier = Modifier.width(12.dp))
                Text("Sleep Timer", color = RadioColors.textPrimary, fontWeight = FontWeight.Bold)
            }
        },
        text = {
            Column {
                options.forEach { minutes ->
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        shape = RoundedCornerShape(10.dp),
                        color = RadioColors.surfaceVariant,
                        onClick = { onSetTimer(minutes) }
                    ) {
                        Text(
                            "$minutes minutes",
                            color = RadioColors.textPrimary,
                            modifier = Modifier.padding(16.dp)
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = RadioColors.textSecondary)
            }
        }
    )
}

// ============================================
// 🔧 HELPER FUNCTIONS
// ============================================

private fun formatListenerCount(count: Int): String {
    return when {
        count >= 1000000 -> "${count / 1000000}M"
        count >= 1000 -> "${count / 1000}K"
        else -> count.toString()
    }
}

