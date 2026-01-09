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
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

// ============================================
// SEARCH COLORS
// ============================================

private object SearchColors {
    val gold = Color(0xFFFFD700)
    val amber = Color(0xFFFF8C00)
    val rose = Color(0xFFFF6B9D)
    val violet = Color(0xFF8B5CF6)
    val cyan = Color(0xFF00D9FF)
    val emerald = Color(0xFF10B981)

    val bgDark = Color(0xFF0A0A0C)
    val bgCard = Color(0xFF12121A)
    val bgElevated = Color(0xFF1A1A24)
    val bgGlass = Color(0x15FFFFFF)

    val textPrimary = Color(0xFFFFFFFF)
    val textSecondary = Color(0xFFB0B0B0)
    val textTertiary = Color(0xFF707070)
}

// ============================================
// 🔍 ARENA SEARCH SCREEN
// ============================================

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ArenaSearchScreen(
    onBack: () -> Unit,
    onTrackClick: (String) -> Unit = {},
    onArtistClick: (String) -> Unit = {},
    onPlayTrack: (Track) -> Unit = {}
) {
    BackHandler { onBack() }

    val repository = remember { ArenaRepository.getInstance() }
    val scope = rememberCoroutineScope()
    val haptic = LocalHapticFeedback.current
    val keyboardController = LocalSoftwareKeyboardController.current
    val focusRequester = remember { FocusRequester() }

    var query by remember { mutableStateOf("") }
    var isSearching by remember { mutableStateOf(false) }
    var selectedFilter by remember { mutableStateOf("All") }

    var trackResults by remember { mutableStateOf<List<Track>>(emptyList()) }
    var artistResults by remember { mutableStateOf<List<ArtistProfile>>(emptyList()) }
    var recentSearches by remember { mutableStateOf(listOf("House music", "Nova Beats", "Chill vibes", "Afrobeats")) }

    val filters = listOf("All", "Tracks", "Artists", "Genres", "Moods")

    // Auto-focus search field
    LaunchedEffect(Unit) {
        delay(300)
        focusRequester.requestFocus()
    }

    // Search when query changes
    LaunchedEffect(query) {
        if (query.length >= 2) {
            isSearching = true
            delay(300) // Debounce
            trackResults = repository.searchTracks(query)
            artistResults = repository.searchArtists(query)
            isSearching = false
        } else {
            trackResults = emptyList()
            artistResults = emptyList()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(SearchColors.bgDark)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
        ) {
            // Search header
            SearchHeader(
                query = query,
                onQueryChange = { query = it },
                onBack = onBack,
                onClear = { query = "" },
                focusRequester = focusRequester,
                onSearch = {
                    keyboardController?.hide()
                    if (query.isNotBlank() && query !in recentSearches) {
                        recentSearches = listOf(query) + recentSearches.take(4)
                    }
                }
            )

            // Filter chips
            LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(filters) { filter ->
                    FilterChip(
                        selected = selectedFilter == filter,
                        onClick = { selectedFilter = filter },
                        label = { Text(filter, fontSize = 13.sp) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = SearchColors.gold.copy(alpha = 0.2f),
                            selectedLabelColor = SearchColors.gold,
                            containerColor = SearchColors.bgCard,
                            labelColor = SearchColors.textSecondary
                        ),
                        border = FilterChipDefaults.filterChipBorder(
                            enabled = true,
                            selected = selectedFilter == filter,
                            borderColor = SearchColors.bgGlass,
                            selectedBorderColor = SearchColors.gold.copy(alpha = 0.5f)
                        )
                    )
                }
            }

            // Content
            if (query.isEmpty()) {
                // Show recent searches and trending
                RecentAndTrendingContent(
                    recentSearches = recentSearches,
                    onRecentClick = { query = it },
                    onClearRecent = { recentSearches = emptyList() },
                    onTrendingClick = { query = it }
                )
            } else if (isSearching) {
                // Loading state
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(
                        color = SearchColors.gold,
                        modifier = Modifier.size(32.dp)
                    )
                }
            } else {
                // Search results
                SearchResultsContent(
                    tracks = when (selectedFilter) {
                        "Artists" -> emptyList()
                        else -> trackResults
                    },
                    artists = when (selectedFilter) {
                        "Tracks" -> emptyList()
                        else -> artistResults
                    },
                    selectedFilter = selectedFilter,
                    onTrackClick = onTrackClick,
                    onArtistClick = onArtistClick,
                    onPlayTrack = onPlayTrack
                )
            }
        }
    }
}

@Composable
private fun SearchHeader(
    query: String,
    onQueryChange: (String) -> Unit,
    onBack: () -> Unit,
    onClear: () -> Unit,
    focusRequester: FocusRequester,
    onSearch: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        shape = RoundedCornerShape(16.dp),
        color = SearchColors.bgCard,
        border = BorderStroke(1.dp, SearchColors.gold.copy(alpha = 0.3f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(
                    Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = SearchColors.textSecondary
                )
            }

            BasicTextField(
                value = query,
                onValueChange = onQueryChange,
                modifier = Modifier
                    .weight(1f)
                    .focusRequester(focusRequester),
                textStyle = TextStyle(
                    color = Color.White,
                    fontSize = 16.sp
                ),
                singleLine = true,
                cursorBrush = SolidColor(SearchColors.gold),
                decorationBox = { innerTextField ->
                    Box {
                        if (query.isEmpty()) {
                            Text(
                                "Search artists, tracks, genres...",
                                color = SearchColors.textTertiary,
                                fontSize = 16.sp
                            )
                        }
                        innerTextField()
                    }
                }
            )

            if (query.isNotEmpty()) {
                IconButton(onClick = onClear) {
                    Icon(
                        Icons.Default.Clear,
                        contentDescription = "Clear",
                        tint = SearchColors.textSecondary,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            IconButton(onClick = onSearch) {
                Icon(
                    Icons.Default.Search,
                    contentDescription = "Search",
                    tint = SearchColors.gold
                )
            }
        }
    }
}

@Composable
private fun RecentAndTrendingContent(
    recentSearches: List<String>,
    onRecentClick: (String) -> Unit,
    onClearRecent: () -> Unit,
    onTrendingClick: (String) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp)
    ) {
        // Recent searches
        if (recentSearches.isNotEmpty()) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "Recent Searches",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.White
                    )
                    TextButton(onClick = onClearRecent) {
                        Text(
                            "Clear All",
                            color = SearchColors.textTertiary,
                            fontSize = 13.sp
                        )
                    }
                }
            }

            items(recentSearches) { search ->
                RecentSearchItem(
                    query = search,
                    onClick = { onRecentClick(search) }
                )
            }

            item {
                Spacer(modifier = Modifier.height(24.dp))
            }
        }

        // Trending searches
        item {
            Text(
                "Trending Now",
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color.White
            )
            Spacer(modifier = Modifier.height(12.dp))
        }

        val trendingSearches = listOf(
            "🔥" to "Afrobeats",
            "📈" to "House Music",
            "🎧" to "Bass Kingdom",
            "🌙" to "Lo-Fi Beats",
            "💫" to "Amapiano",
            "🎹" to "Electronic"
        )

        items(trendingSearches) { (emoji, search) ->
            TrendingSearchItem(
                emoji = emoji,
                query = search,
                onClick = { onTrendingClick(search) }
            )
        }

        // Browse by genre
        item {
            Spacer(modifier = Modifier.height(24.dp))
            Text(
                "Browse by Genre",
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color.White
            )
            Spacer(modifier = Modifier.height(12.dp))
        }

        item {
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                val genres = listOf(
                    "Electronic" to SearchColors.cyan,
                    "Hip Hop" to SearchColors.violet,
                    "R&B" to SearchColors.rose,
                    "Rock" to SearchColors.amber,
                    "Jazz" to SearchColors.gold,
                    "Ambient" to SearchColors.emerald
                )

                items(genres) { (genre, color) ->
                    GenreCard(
                        genre = genre,
                        color = color,
                        onClick = { onTrendingClick(genre) }
                    )
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(80.dp))
        }
    }
}

@Composable
private fun RecentSearchItem(
    query: String,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        color = Color.Transparent
    ) {
        Row(
            modifier = Modifier.padding(vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Icons.Default.History,
                contentDescription = null,
                tint = SearchColors.textTertiary,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                query,
                fontSize = 15.sp,
                color = SearchColors.textSecondary
            )
        }
    }
}

@Composable
private fun TrendingSearchItem(
    emoji: String,
    query: String,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        shape = RoundedCornerShape(12.dp),
        color = SearchColors.bgCard.copy(alpha = 0.6f)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                emoji,
                fontSize = 20.sp
            )
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                query,
                fontSize = 15.sp,
                fontWeight = FontWeight.Medium,
                color = Color.White
            )
            Spacer(modifier = Modifier.weight(1f))
            Icon(
                Icons.Default.TrendingUp,
                contentDescription = null,
                tint = SearchColors.emerald,
                modifier = Modifier.size(18.dp)
            )
        }
    }
}

@Composable
private fun GenreCard(
    genre: String,
    color: Color,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        modifier = Modifier
            .width(120.dp)
            .height(70.dp),
        shape = RoundedCornerShape(12.dp),
        color = color.copy(alpha = 0.15f),
        border = BorderStroke(1.dp, color.copy(alpha = 0.3f))
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Text(
                genre,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                color = color
            )
        }
    }
}

@Composable
private fun SearchResultsContent(
    tracks: List<Track>,
    artists: List<ArtistProfile>,
    selectedFilter: String,
    onTrackClick: (String) -> Unit,
    onArtistClick: (String) -> Unit,
    onPlayTrack: (Track) -> Unit
) {
    if (tracks.isEmpty() && artists.isEmpty()) {
        // No results
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    Icons.Outlined.SearchOff,
                    contentDescription = null,
                    tint = SearchColors.textTertiary,
                    modifier = Modifier.size(64.dp)
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    "No results found",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.White
                )
                Text(
                    "Try a different search term",
                    fontSize = 14.sp,
                    color = SearchColors.textTertiary
                )
            }
        }
    } else {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp)
        ) {
            // Artists section
            if (artists.isNotEmpty() && selectedFilter in listOf("All", "Artists")) {
                item {
                    Text(
                        "Artists",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.White
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                }

                items(artists.take(if (selectedFilter == "Artists") artists.size else 3)) { artist ->
                    SearchArtistItem(
                        artist = artist,
                        onClick = { onArtistClick(artist.id) }
                    )
                }

                item {
                    Spacer(modifier = Modifier.height(20.dp))
                }
            }

            // Tracks section
            if (tracks.isNotEmpty() && selectedFilter in listOf("All", "Tracks")) {
                item {
                    Text(
                        "Tracks",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.White
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                }

                items(tracks) { track ->
                    SearchTrackItem(
                        track = track,
                        onClick = { onTrackClick(track.id) },
                        onPlay = { onPlayTrack(track) }
                    )
                }
            }

            item {
                Spacer(modifier = Modifier.height(80.dp))
            }
        }
    }
}

@Composable
private fun SearchArtistItem(
    artist: ArtistProfile,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        shape = RoundedCornerShape(12.dp),
        color = SearchColors.bgCard.copy(alpha = 0.6f)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Avatar
            Surface(
                modifier = Modifier.size(50.dp),
                shape = CircleShape,
                color = SearchColors.bgElevated
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        artist.displayName.take(2).uppercase(),
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = SearchColors.gold
                    )
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        artist.displayName,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.White
                    )
                    if (artist.verificationStatus in listOf(VerificationStatus.VERIFIED, VerificationStatus.OFFICIAL)) {
                        Spacer(modifier = Modifier.width(6.dp))
                        Icon(
                            Icons.Default.Verified,
                            contentDescription = "Verified",
                            tint = SearchColors.gold,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }
                Text(
                    "@${artist.handle} • ${formatCount(artist.followerCount)} followers",
                    fontSize = 13.sp,
                    color = SearchColors.textTertiary
                )
            }

            OutlinedButton(
                onClick = { /* Follow */ },
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                border = BorderStroke(1.dp, SearchColors.gold.copy(alpha = 0.5f))
            ) {
                Text(
                    if (artist.isFollowing) "Following" else "Follow",
                    fontSize = 12.sp,
                    color = SearchColors.gold
                )
            }
        }
    }
}

@Composable
private fun SearchTrackItem(
    track: Track,
    onClick: () -> Unit,
    onPlay: () -> Unit
) {
    Surface(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        shape = RoundedCornerShape(12.dp),
        color = SearchColors.bgCard.copy(alpha = 0.6f)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Play button
            IconButton(
                onClick = onPlay,
                modifier = Modifier.size(44.dp)
            ) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    shape = RoundedCornerShape(10.dp),
                    color = SearchColors.bgElevated
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            Icons.Default.PlayArrow,
                            contentDescription = "Play",
                            tint = SearchColors.gold,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    track.title,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.White,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        track.artistName,
                        fontSize = 13.sp,
                        color = SearchColors.textSecondary
                    )
                    if (track.isVerified) {
                        Spacer(modifier = Modifier.width(4.dp))
                        Icon(
                            Icons.Default.Verified,
                            contentDescription = "Verified",
                            tint = SearchColors.gold,
                            modifier = Modifier.size(12.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.height(2.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    track.genres.take(2).forEach { genre ->
                        Text(
                            genre,
                            fontSize = 11.sp,
                            color = SearchColors.gold.copy(alpha = 0.8f)
                        )
                    }
                    Text(
                        formatDuration(track.duration),
                        fontSize = 11.sp,
                        color = SearchColors.textTertiary
                    )
                }
            }

            // Stats
            Column(
                horizontalAlignment = Alignment.End
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.PlayArrow,
                        contentDescription = null,
                        tint = SearchColors.textTertiary,
                        modifier = Modifier.size(12.dp)
                    )
                    Text(
                        formatCount(track.playCount),
                        fontSize = 11.sp,
                        color = SearchColors.textTertiary
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

