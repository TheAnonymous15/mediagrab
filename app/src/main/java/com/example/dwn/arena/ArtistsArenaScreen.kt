package com.example.dwn.arena

import androidx.activity.compose.BackHandler
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.*
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
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
// 🎨 ARENA THEME COLORS
// ============================================

private object ArenaColors {
    // Primary - Golden/Amber artist theme
    val gold = Color(0xFFFFD700)
    val amber = Color(0xFFFF8C00)
    val bronze = Color(0xFFCD7F32)

    // Accents
    val rose = Color(0xFFFF6B9D)
    val violet = Color(0xFF8B5CF6)
    val cyan = Color(0xFF00D9FF)
    val emerald = Color(0xFF10B981)

    // Backgrounds
    val bgDark = Color(0xFF0A0A0C)
    val bgCard = Color(0xFF12121A)
    val bgElevated = Color(0xFF1A1A24)
    val bgGlass = Color(0x15FFFFFF)

    // Text
    val textPrimary = Color(0xFFFFFFFF)
    val textSecondary = Color(0xFFB0B0B0)
    val textTertiary = Color(0xFF707070)

    // Gradients
    val gradientGold = Brush.horizontalGradient(listOf(gold, amber))
    val gradientArtist = Brush.horizontalGradient(listOf(gold, amber, rose))
}

// ============================================
// 🎤 ARTISTS ARENA MAIN SCREEN
// ============================================

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun ArtistsArenaScreen(
    onBack: () -> Unit,
    onNavigateToUpload: () -> Unit = {},
    onNavigateToArtistProfile: (String) -> Unit = {},
    onNavigateToTrackDetail: (String) -> Unit = {},
    onNavigateToRemixStudio: (String) -> Unit = {},
    onPlayTrack: (Track) -> Unit = {}
) {
    BackHandler { onBack() }

    val repository = remember { ArenaRepository.getInstance() }
    val feedTracks by repository.feedTracks.collectAsState()
    val trendingTracks by repository.trendingTracks.collectAsState()
    val freshReleases by repository.freshReleases.collectAsState()
    val featuredArtists by repository.featuredArtists.collectAsState()
    val activeChallenges by repository.activeChallenges.collectAsState()

    val pagerState = rememberPagerState(pageCount = { 4 })
    val scope = rememberCoroutineScope()
    val haptic = LocalHapticFeedback.current

    var selectedTab by remember { mutableIntStateOf(0) }
    var searchQuery by remember { mutableStateOf("") }
    var showSearch by remember { mutableStateOf(false) }

    val tabs = listOf("Discover", "Trending", "Fresh", "Following")

    // Sync tab with pager
    LaunchedEffect(pagerState.currentPage) {
        selectedTab = pagerState.currentPage
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(ArenaColors.bgDark)
    ) {
        // Animated background
        ArenaAnimatedBackground()

        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
        ) {
            // Top Bar
            ArenaTopBar(
                onBack = onBack,
                onSearch = { showSearch = !showSearch },
                onUpload = {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    onNavigateToUpload()
                }
            )

            // Search bar (animated)
            AnimatedVisibility(
                visible = showSearch,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically()
            ) {
                ArenaSearchBar(
                    query = searchQuery,
                    onQueryChange = { searchQuery = it },
                    onClose = { showSearch = false }
                )
            }

            // Tab Row
            ScrollableTabRow(
                selectedTabIndex = selectedTab,
                containerColor = Color.Transparent,
                contentColor = Color.White,
                edgePadding = 16.dp,
                indicator = { tabPositions ->
                    if (tabPositions.isNotEmpty() && selectedTab < tabPositions.size) {
                        val currentTabPosition = tabPositions[selectedTab]
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .wrapContentSize(align = Alignment.BottomStart)
                                .offset(x = currentTabPosition.left)
                                .width(currentTabPosition.width)
                                .height(3.dp)
                                .background(
                                    ArenaColors.gradientGold,
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
                        onClick = {
                            selectedTab = index
                            scope.launch { pagerState.animateScrollToPage(index) }
                        },
                        text = {
                            Text(
                                text = title,
                                fontSize = 14.sp,
                                fontWeight = if (selectedTab == index) FontWeight.Bold else FontWeight.Medium
                            )
                        },
                        selectedContentColor = ArenaColors.gold,
                        unselectedContentColor = ArenaColors.textSecondary
                    )
                }
            }

            // Content Pager
            HorizontalPager(
                state = pagerState,
                modifier = Modifier.fillMaxSize()
            ) { page ->
                when (page) {
                    0 -> DiscoverTab(
                        featuredArtists = featuredArtists,
                        trendingTracks = trendingTracks.take(5),
                        freshReleases = freshReleases.take(5),
                        challenges = activeChallenges,
                        onArtistClick = onNavigateToArtistProfile,
                        onTrackClick = onNavigateToTrackDetail,
                        onPlayTrack = onPlayTrack,
                        onChallengeClick = { /* Navigate to challenge */ }
                    )
                    1 -> TrendingTab(
                        tracks = trendingTracks,
                        onTrackClick = onNavigateToTrackDetail,
                        onPlayTrack = onPlayTrack,
                        onArtistClick = onNavigateToArtistProfile
                    )
                    2 -> FreshReleasesTab(
                        tracks = freshReleases,
                        onTrackClick = onNavigateToTrackDetail,
                        onPlayTrack = onPlayTrack,
                        onArtistClick = onNavigateToArtistProfile
                    )
                    3 -> FollowingTab(
                        tracks = feedTracks,
                        onTrackClick = onNavigateToTrackDetail,
                        onPlayTrack = onPlayTrack,
                        onArtistClick = onNavigateToArtistProfile
                    )
                }
            }
        }

        // FAB for quick upload
        FloatingActionButton(
            onClick = {
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                onNavigateToUpload()
            },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(24.dp)
                .navigationBarsPadding(),
            containerColor = ArenaColors.gold,
            contentColor = Color.Black
        ) {
            Icon(Icons.Default.Add, contentDescription = "Upload Track")
        }
    }
}

// ============================================
// ANIMATED BACKGROUND
// ============================================

@Composable
private fun ArenaAnimatedBackground() {
    val transition = rememberInfiniteTransition(label = "arena_bg")

    val wave1 by transition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(8000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "wave1"
    )

    Canvas(modifier = Modifier.fillMaxSize()) {
        val w = size.width
        val h = size.height

        // Subtle golden orbs
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(
                    ArenaColors.gold.copy(alpha = 0.08f),
                    Color.Transparent
                ),
                radius = w * 0.4f
            ),
            radius = w * 0.35f,
            center = Offset(w * 0.1f, h * 0.15f)
        )

        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(
                    ArenaColors.amber.copy(alpha = 0.06f),
                    Color.Transparent
                ),
                radius = w * 0.3f
            ),
            radius = w * 0.25f,
            center = Offset(w * 0.85f, h * 0.6f)
        )

        // Animated wave accent
        val waveY = h * 0.3f + sin(Math.toRadians(wave1.toDouble())).toFloat() * h * 0.05f
        drawCircle(
            color = ArenaColors.rose.copy(alpha = 0.04f),
            radius = w * 0.2f,
            center = Offset(w * 0.5f, waveY)
        )
    }
}

// ============================================
// TOP BAR
// ============================================

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ArenaTopBar(
    onBack: () -> Unit,
    onSearch: () -> Unit,
    onUpload: () -> Unit
) {
    val transition = rememberInfiniteTransition(label = "logo")
    val glowAlpha by transition.animateFloat(
        initialValue = 0.5f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500),
            repeatMode = RepeatMode.Reverse
        ),
        label = "glow"
    )

    TopAppBar(
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Animated logo icon
                Box(
                    modifier = Modifier.size(36.dp),
                    contentAlignment = Alignment.Center
                ) {
                    // Glow ring
                    Canvas(modifier = Modifier.size(36.dp)) {
                        drawCircle(
                            brush = Brush.sweepGradient(
                                listOf(
                                    ArenaColors.gold.copy(alpha = glowAlpha * 0.6f),
                                    ArenaColors.amber.copy(alpha = glowAlpha * 0.4f),
                                    ArenaColors.rose.copy(alpha = glowAlpha * 0.3f),
                                    ArenaColors.gold.copy(alpha = glowAlpha * 0.6f)
                                )
                            ),
                            style = Stroke(width = 2f)
                        )
                    }

                    Box(
                        modifier = Modifier
                            .size(28.dp)
                            .background(
                                Brush.radialGradient(
                                    listOf(
                                        ArenaColors.gold.copy(alpha = 0.2f),
                                        Color.Transparent
                                    )
                                ),
                                CircleShape
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.MusicNote,
                            contentDescription = null,
                            tint = ArenaColors.gold,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                Column {
                    Text(
                        "Artists Arena",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Black,
                        color = ArenaColors.gold
                    )
                    Text(
                        "Discover • Create • Connect",
                        fontSize = 10.sp,
                        color = ArenaColors.textSecondary,
                        letterSpacing = 0.5.sp
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
            IconButton(onClick = onSearch) {
                Icon(
                    Icons.Default.Search,
                    contentDescription = "Search",
                    tint = ArenaColors.textSecondary
                )
            }
            IconButton(onClick = onUpload) {
                Icon(
                    Icons.Outlined.CloudUpload,
                    contentDescription = "Upload",
                    tint = ArenaColors.gold
                )
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = Color.Transparent
        )
    )
}

// ============================================
// SEARCH BAR
// ============================================

@Composable
private fun ArenaSearchBar(
    query: String,
    onQueryChange: (String) -> Unit,
    onClose: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        shape = RoundedCornerShape(16.dp),
        color = ArenaColors.bgGlass,
        border = BorderStroke(1.dp, ArenaColors.gold.copy(alpha = 0.3f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Icons.Default.Search,
                contentDescription = null,
                tint = ArenaColors.gold,
                modifier = Modifier.size(20.dp)
            )

            Spacer(modifier = Modifier.width(12.dp))

            BasicTextField(
                value = query,
                onValueChange = onQueryChange,
                modifier = Modifier.weight(1f),
                textStyle = androidx.compose.ui.text.TextStyle(
                    color = Color.White,
                    fontSize = 15.sp
                ),
                singleLine = true,
                decorationBox = { innerTextField ->
                    Box {
                        if (query.isEmpty()) {
                            Text(
                                "Search artists, tracks, genres...",
                                color = ArenaColors.textTertiary,
                                fontSize = 15.sp
                            )
                        }
                        innerTextField()
                    }
                }
            )

            if (query.isNotEmpty()) {
                IconButton(
                    onClick = { onQueryChange("") },
                    modifier = Modifier.size(24.dp)
                ) {
                    Icon(
                        Icons.Default.Clear,
                        contentDescription = "Clear",
                        tint = ArenaColors.textSecondary,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            IconButton(
                onClick = onClose,
                modifier = Modifier.size(24.dp)
            ) {
                Icon(
                    Icons.Default.Close,
                    contentDescription = "Close",
                    tint = ArenaColors.textSecondary,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

// ============================================
// DISCOVER TAB
// ============================================

@Composable
private fun DiscoverTab(
    featuredArtists: List<ArtistProfile>,
    trendingTracks: List<Track>,
    freshReleases: List<Track>,
    challenges: List<RemixChallenge>,
    onArtistClick: (String) -> Unit,
    onTrackClick: (String) -> Unit,
    onPlayTrack: (Track) -> Unit,
    onChallengeClick: (String) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 100.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        // Featured Artists Section
        item {
            SectionHeader(
                title = "Featured Artists",
                subtitle = "Rising stars & verified creators",
                icon = Icons.Default.Star
            )
        }

        item {
            LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(featuredArtists) { artist ->
                    FeaturedArtistCard(
                        artist = artist,
                        onClick = { onArtistClick(artist.id) }
                    )
                }
            }
        }

        // Remix Challenges
        if (challenges.isNotEmpty()) {
            item {
                Spacer(modifier = Modifier.height(8.dp))
                SectionHeader(
                    title = "Remix Challenges",
                    subtitle = "Put your spin on these tracks",
                    icon = Icons.Default.Refresh
                )
            }

            item {
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(challenges) { challenge ->
                        RemixChallengeCard(
                            challenge = challenge,
                            onClick = { onChallengeClick(challenge.id) }
                        )
                    }
                }
            }
        }

        // Trending Now
        item {
            Spacer(modifier = Modifier.height(8.dp))
            SectionHeader(
                title = "Trending Now",
                subtitle = "What everyone's listening to",
                icon = Icons.AutoMirrored.Filled.TrendingUp
            )
        }

        items(trendingTracks) { track ->
            TrackListItem(
                track = track,
                rank = trendingTracks.indexOf(track) + 1,
                showRank = true,
                onClick = { onTrackClick(track.id) },
                onPlay = { onPlayTrack(track) },
                onArtistClick = { onArtistClick(track.artistId) }
            )
        }

        // Fresh Releases
        item {
            Spacer(modifier = Modifier.height(8.dp))
            SectionHeader(
                title = "Fresh Releases",
                subtitle = "Just dropped today",
                icon = Icons.Default.NewReleases
            )
        }

        items(freshReleases) { track ->
            TrackListItem(
                track = track,
                onClick = { onTrackClick(track.id) },
                onPlay = { onPlayTrack(track) },
                onArtistClick = { onArtistClick(track.artistId) }
            )
        }
    }
}

// ============================================
// TRENDING TAB
// ============================================

@Composable
private fun TrendingTab(
    tracks: List<Track>,
    onTrackClick: (String) -> Unit,
    onPlayTrack: (Track) -> Unit,
    onArtistClick: (String) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 100.dp)
    ) {
        item {
            // Trending header banner
            TrendingBanner()
        }

        itemsIndexed(tracks) { index, track ->
            TrackListItem(
                track = track,
                rank = index + 1,
                showRank = true,
                onClick = { onTrackClick(track.id) },
                onPlay = { onPlayTrack(track) },
                onArtistClick = { onArtistClick(track.artistId) }
            )
        }
    }
}

@Composable
private fun TrendingBanner() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
            .height(100.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(
                Brush.horizontalGradient(
                    listOf(
                        ArenaColors.gold.copy(alpha = 0.15f),
                        ArenaColors.amber.copy(alpha = 0.1f),
                        ArenaColors.rose.copy(alpha = 0.08f)
                    )
                )
            )
            .border(
                1.dp,
                Brush.horizontalGradient(
                    listOf(
                        ArenaColors.gold.copy(alpha = 0.4f),
                        ArenaColors.amber.copy(alpha = 0.2f)
                    )
                ),
                RoundedCornerShape(20.dp)
            ),
        contentAlignment = Alignment.Center
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Icons.AutoMirrored.Filled.TrendingUp,
                contentDescription = null,
                tint = ArenaColors.gold,
                modifier = Modifier.size(40.dp)
            )

            Spacer(modifier = Modifier.width(16.dp))

            Column {
                Text(
                    "Top Charts",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Black,
                    color = ArenaColors.gold
                )
                Text(
                    "Most played tracks this week",
                    fontSize = 13.sp,
                    color = ArenaColors.textSecondary
                )
            }
        }
    }
}

// ============================================
// FRESH RELEASES TAB
// ============================================

@Composable
private fun FreshReleasesTab(
    tracks: List<Track>,
    onTrackClick: (String) -> Unit,
    onPlayTrack: (Track) -> Unit,
    onArtistClick: (String) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 100.dp)
    ) {
        item {
            FreshReleasesBanner()
        }

        items(tracks) { track ->
            TrackListItem(
                track = track,
                showNew = true,
                onClick = { onTrackClick(track.id) },
                onPlay = { onPlayTrack(track) },
                onArtistClick = { onArtistClick(track.artistId) }
            )
        }
    }
}

@Composable
private fun FreshReleasesBanner() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
            .height(100.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(
                Brush.horizontalGradient(
                    listOf(
                        ArenaColors.emerald.copy(alpha = 0.15f),
                        ArenaColors.cyan.copy(alpha = 0.1f)
                    )
                )
            )
            .border(
                1.dp,
                Brush.horizontalGradient(
                    listOf(
                        ArenaColors.emerald.copy(alpha = 0.4f),
                        ArenaColors.cyan.copy(alpha = 0.2f)
                    )
                ),
                RoundedCornerShape(20.dp)
            ),
        contentAlignment = Alignment.Center
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Icons.Default.NewReleases,
                contentDescription = null,
                tint = ArenaColors.emerald,
                modifier = Modifier.size(40.dp)
            )

            Spacer(modifier = Modifier.width(16.dp))

            Column {
                Text(
                    "Fresh Drops",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Black,
                    color = ArenaColors.emerald
                )
                Text(
                    "New music released today",
                    fontSize = 13.sp,
                    color = ArenaColors.textSecondary
                )
            }
        }
    }
}

// ============================================
// FOLLOWING TAB
// ============================================

@Composable
private fun FollowingTab(
    tracks: List<Track>,
    onTrackClick: (String) -> Unit,
    onPlayTrack: (Track) -> Unit,
    onArtistClick: (String) -> Unit
) {
    if (tracks.isEmpty()) {
        EmptyFollowingState()
    } else {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 100.dp)
        ) {
            items(tracks) { track ->
                TrackListItem(
                    track = track,
                    onClick = { onTrackClick(track.id) },
                    onPlay = { onPlayTrack(track) },
                    onArtistClick = { onArtistClick(track.artistId) }
                )
            }
        }
    }
}

@Composable
private fun EmptyFollowingState() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(32.dp)
        ) {
            Surface(
                modifier = Modifier.size(80.dp),
                shape = CircleShape,
                color = ArenaColors.bgGlass
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        Icons.Outlined.PersonAdd,
                        contentDescription = null,
                        tint = ArenaColors.gold,
                        modifier = Modifier.size(40.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            Text(
                "Follow Artists",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                "Discover and follow artists to see their latest releases here",
                fontSize = 14.sp,
                color = ArenaColors.textSecondary,
                textAlign = TextAlign.Center
            )
        }
    }
}

// ============================================
// SECTION HEADER
// ============================================

@Composable
private fun SectionHeader(
    title: String,
    subtitle: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            icon,
            contentDescription = null,
            tint = ArenaColors.gold,
            modifier = Modifier.size(24.dp)
        )

        Spacer(modifier = Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                title,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
            Text(
                subtitle,
                fontSize = 12.sp,
                color = ArenaColors.textSecondary
            )
        }

        TextButton(onClick = { /* See all */ }) {
            Text(
                "See All",
                color = ArenaColors.gold,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

// ============================================
// FEATURED ARTIST CARD
// ============================================

@Composable
private fun FeaturedArtistCard(
    artist: ArtistProfile,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        modifier = Modifier
            .width(140.dp)
            .height(180.dp),
        shape = RoundedCornerShape(16.dp),
        color = ArenaColors.bgCard,
        border = BorderStroke(1.dp, ArenaColors.bgGlass)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Avatar
            Box(
                modifier = Modifier.size(70.dp),
                contentAlignment = Alignment.Center
            ) {
                // Verification ring
                if (artist.verificationStatus == VerificationStatus.VERIFIED ||
                    artist.verificationStatus == VerificationStatus.OFFICIAL) {
                    Canvas(modifier = Modifier.size(70.dp)) {
                        drawCircle(
                            brush = Brush.sweepGradient(
                                listOf(
                                    ArenaColors.gold,
                                    ArenaColors.amber,
                                    ArenaColors.gold
                                )
                            ),
                            style = Stroke(width = 2f)
                        )
                    }
                }

                Surface(
                    modifier = Modifier.size(60.dp),
                    shape = CircleShape,
                    color = ArenaColors.bgElevated
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            artist.displayName.take(2).uppercase(),
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = ArenaColors.gold
                        )
                    }
                }

                // Verification badge
                if (artist.verificationStatus == VerificationStatus.VERIFIED ||
                    artist.verificationStatus == VerificationStatus.OFFICIAL) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .size(20.dp)
                            .background(ArenaColors.gold, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.Verified,
                            contentDescription = "Verified",
                            tint = Color.Black,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                artist.displayName,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color.White,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Text(
                "@${artist.handle}",
                fontSize = 11.sp,
                color = ArenaColors.textTertiary
            )

            Spacer(modifier = Modifier.height(6.dp))

            // Follower count
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Icon(
                    Icons.Outlined.People,
                    contentDescription = null,
                    tint = ArenaColors.textTertiary,
                    modifier = Modifier.size(12.dp)
                )
                Text(
                    formatCount(artist.followerCount),
                    fontSize = 11.sp,
                    color = ArenaColors.textTertiary
                )
            }
        }
    }
}

// ============================================
// REMIX CHALLENGE CARD
// ============================================

@Composable
private fun RemixChallengeCard(
    challenge: RemixChallenge,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        modifier = Modifier
            .width(260.dp)
            .height(140.dp),
        shape = RoundedCornerShape(16.dp),
        color = ArenaColors.bgCard,
        border = BorderStroke(
            1.dp,
            Brush.horizontalGradient(
                listOf(ArenaColors.violet.copy(alpha = 0.5f), ArenaColors.rose.copy(alpha = 0.3f))
            )
        )
    ) {
        Box {
            // Background gradient
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.horizontalGradient(
                            listOf(
                                ArenaColors.violet.copy(alpha = 0.1f),
                                ArenaColors.rose.copy(alpha = 0.05f)
                            )
                        )
                    )
            )

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(14.dp)
            ) {
                // Challenge badge
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .background(ArenaColors.violet.copy(alpha = 0.2f), RoundedCornerShape(4.dp))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            "CHALLENGE",
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            color = ArenaColors.violet,
                            letterSpacing = 0.5.sp
                        )
                    }

                    Text(
                        "${challenge.submissionCount} entries",
                        fontSize = 11.sp,
                        color = ArenaColors.textTertiary
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    challenge.title,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Text(
                    challenge.description,
                    fontSize = 12.sp,
                    color = ArenaColors.textSecondary,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.weight(1f))

                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.EmojiEvents,
                        contentDescription = null,
                        tint = ArenaColors.gold,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        challenge.prizes.firstOrNull() ?: "Prizes available",
                        fontSize = 11.sp,
                        color = ArenaColors.gold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

// ============================================
// TRACK LIST ITEM
// ============================================

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun TrackListItem(
    track: Track,
    rank: Int? = null,
    showRank: Boolean = false,
    showNew: Boolean = false,
    onClick: () -> Unit,
    onPlay: () -> Unit,
    onArtistClick: () -> Unit
) {
    val scope = rememberCoroutineScope()
    val repository = remember { ArenaRepository.getInstance() }
    var isLiked by remember { mutableStateOf(track.isLiked) }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp)
            .combinedClickable(
                onClick = onClick,
                onLongClick = { /* Show options menu */ }
            ),
        shape = RoundedCornerShape(12.dp),
        color = ArenaColors.bgCard.copy(alpha = 0.6f)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Rank or play button
            Box(
                modifier = Modifier.size(36.dp),
                contentAlignment = Alignment.Center
            ) {
                if (showRank && rank != null) {
                    val rankColor = when (rank) {
                        1 -> ArenaColors.gold
                        2 -> Color(0xFFC0C0C0)
                        3 -> ArenaColors.bronze
                        else -> ArenaColors.textTertiary
                    }
                    Text(
                        "#$rank",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = rankColor
                    )
                } else {
                    IconButton(onClick = onPlay, modifier = Modifier.size(36.dp)) {
                        Icon(
                            Icons.Default.PlayArrow,
                            contentDescription = "Play",
                            tint = ArenaColors.gold
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            // Cover art placeholder
            Surface(
                modifier = Modifier.size(50.dp),
                shape = RoundedCornerShape(8.dp),
                color = ArenaColors.bgElevated
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        Icons.Default.MusicNote,
                        contentDescription = null,
                        tint = ArenaColors.gold.copy(alpha = 0.5f),
                        modifier = Modifier.size(24.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            // Track info
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        track.title,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.White,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false)
                    )

                    if (showNew) {
                        Spacer(modifier = Modifier.width(6.dp))
                        Box(
                            modifier = Modifier
                                .background(ArenaColors.emerald.copy(alpha = 0.2f), RoundedCornerShape(4.dp))
                                .padding(horizontal = 4.dp, vertical = 1.dp)
                        ) {
                            Text(
                                "NEW",
                                fontSize = 8.sp,
                                fontWeight = FontWeight.Bold,
                                color = ArenaColors.emerald
                            )
                        }
                    }

                    if (track.isExplicit) {
                        Spacer(modifier = Modifier.width(4.dp))
                        Box(
                            modifier = Modifier
                                .background(ArenaColors.textTertiary.copy(alpha = 0.3f), RoundedCornerShape(2.dp))
                                .padding(horizontal = 4.dp, vertical = 1.dp)
                        ) {
                            Text(
                                "E",
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                color = ArenaColors.textSecondary
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(2.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.clickable(onClick = onArtistClick)
                ) {
                    Text(
                        track.artistName,
                        fontSize = 13.sp,
                        color = ArenaColors.textSecondary
                    )
                    if (track.isVerified) {
                        Spacer(modifier = Modifier.width(4.dp))
                        Icon(
                            Icons.Default.Verified,
                            contentDescription = "Verified",
                            tint = ArenaColors.gold,
                            modifier = Modifier.size(12.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                // Stats row
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(3.dp)
                    ) {
                        Icon(
                            Icons.Outlined.PlayArrow,
                            contentDescription = null,
                            tint = ArenaColors.textTertiary,
                            modifier = Modifier.size(12.dp)
                        )
                        Text(
                            formatCount(track.playCount),
                            fontSize = 11.sp,
                            color = ArenaColors.textTertiary
                        )
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(3.dp)
                    ) {
                        Icon(
                            Icons.Outlined.FavoriteBorder,
                            contentDescription = null,
                            tint = ArenaColors.textTertiary,
                            modifier = Modifier.size(12.dp)
                        )
                        Text(
                            formatCount(track.likeCount.toLong()),
                            fontSize = 11.sp,
                            color = ArenaColors.textTertiary
                        )
                    }

                    Text(
                        formatDuration(track.duration),
                        fontSize = 11.sp,
                        color = ArenaColors.textTertiary
                    )
                }
            }

            // Actions
            Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                IconButton(
                    onClick = {
                        isLiked = !isLiked
                        scope.launch { repository.likeTrack(track.id) }
                    },
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        if (isLiked) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
                        contentDescription = "Like",
                        tint = if (isLiked) ArenaColors.rose else ArenaColors.textTertiary,
                        modifier = Modifier.size(20.dp)
                    )
                }

                IconButton(
                    onClick = onPlay,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        Icons.Default.PlayCircle,
                        contentDescription = "Play",
                        tint = ArenaColors.gold,
                        modifier = Modifier.size(24.dp)
                    )
                }
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

private fun formatCount(count: Int): String = formatCount(count.toLong())

private fun formatDuration(durationMs: Long): String {
    val totalSeconds = durationMs / 1000
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return "%d:%02d".format(minutes, seconds)
}

