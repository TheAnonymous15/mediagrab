package com.example.dwn.arena

import androidx.activity.compose.BackHandler
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch

// ============================================
// ARENA COLORS (shared)
// ============================================

private object ProfileColors {
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
// 👤 ARTIST PROFILE SCREEN
// ============================================

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ArtistProfileScreen(
    artistId: String,
    onBack: () -> Unit,
    onTrackClick: (String) -> Unit = {},
    onPlayTrack: (Track) -> Unit = {},
    onRemixTrack: (String) -> Unit = {}
) {
    BackHandler { onBack() }

    val repository = remember { ArenaRepository.getInstance() }
    var artist by remember { mutableStateOf<ArtistProfile?>(null) }
    var tracks by remember { mutableStateOf<List<Track>>(emptyList()) }
    var isFollowing by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val haptic = LocalHapticFeedback.current

    var selectedTab by remember { mutableIntStateOf(0) }
    val tabs = listOf("Tracks", "About", "Stats")

    LaunchedEffect(artistId) {
        artist = repository.getArtistProfile(artistId)
        tracks = repository.getTracksByArtist(artistId)
        isFollowing = artist?.isFollowing ?: false
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(ProfileColors.bgDark)
    ) {
        if (artist == null) {
            // Loading state
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = ProfileColors.gold)
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize()
            ) {
                // Header with banner and profile
                item {
                    ProfileHeader(
                        artist = artist!!,
                        isFollowing = isFollowing,
                        onBack = onBack,
                        onFollow = {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            scope.launch {
                                repository.followArtist(artistId)
                                isFollowing = !isFollowing
                            }
                        }
                    )
                }

                // Stats row
                item {
                    StatsRow(artist = artist!!)
                }

                // Tab row
                item {
                    TabRow(
                        selectedTabIndex = selectedTab,
                        containerColor = Color.Transparent,
                        contentColor = Color.White,
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
                                            ProfileColors.gold,
                                            RoundedCornerShape(topStart = 3.dp, topEnd = 3.dp)
                                        )
                                )
                            }
                        },
                        divider = { Divider(color = ProfileColors.bgGlass) }
                    ) {
                        tabs.forEachIndexed { index, title ->
                            Tab(
                                selected = selectedTab == index,
                                onClick = { selectedTab = index },
                                text = {
                                    Text(
                                        text = title,
                                        fontWeight = if (selectedTab == index) FontWeight.Bold else FontWeight.Medium
                                    )
                                },
                                selectedContentColor = ProfileColors.gold,
                                unselectedContentColor = ProfileColors.textSecondary
                            )
                        }
                    }
                }

                // Tab content
                when (selectedTab) {
                    0 -> {
                        // Tracks tab
                        if (tracks.isEmpty()) {
                            item {
                                EmptyTracksState()
                            }
                        } else {
                            items(tracks) { track ->
                                ArtistTrackItem(
                                    track = track,
                                    onClick = { onTrackClick(track.id) },
                                    onPlay = { onPlayTrack(track) },
                                    onRemix = { onRemixTrack(track.id) }
                                )
                            }
                        }
                    }
                    1 -> {
                        // About tab
                        item {
                            AboutSection(artist = artist!!)
                        }
                    }
                    2 -> {
                        // Stats tab
                        item {
                            StatsSection(artist = artist!!)
                        }
                    }
                }

                item {
                    Spacer(modifier = Modifier.height(100.dp))
                }
            }
        }
    }
}

@Composable
private fun ProfileHeader(
    artist: ArtistProfile,
    isFollowing: Boolean,
    onBack: () -> Unit,
    onFollow: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(280.dp)
    ) {
        // Banner gradient background
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(180.dp)
                .background(
                    Brush.verticalGradient(
                        listOf(
                            ProfileColors.gold.copy(alpha = 0.3f),
                            ProfileColors.amber.copy(alpha = 0.2f),
                            ProfileColors.bgDark
                        )
                    )
                )
        )

        // Back button
        IconButton(
            onClick = onBack,
            modifier = Modifier
                .statusBarsPadding()
                .padding(8.dp)
        ) {
            Icon(
                Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = "Back",
                tint = Color.White
            )
        }

        // Profile content
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(horizontal = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Avatar
            Box(
                modifier = Modifier.size(100.dp),
                contentAlignment = Alignment.Center
            ) {
                // Glow ring for verified artists
                if (artist.verificationStatus == VerificationStatus.VERIFIED ||
                    artist.verificationStatus == VerificationStatus.OFFICIAL) {
                    Canvas(modifier = Modifier.size(100.dp)) {
                        drawCircle(
                            brush = Brush.sweepGradient(
                                listOf(
                                    ProfileColors.gold,
                                    ProfileColors.amber,
                                    ProfileColors.rose,
                                    ProfileColors.gold
                                )
                            ),
                            style = Stroke(width = 3f)
                        )
                    }
                }

                Surface(
                    modifier = Modifier.size(88.dp),
                    shape = CircleShape,
                    color = ProfileColors.bgElevated,
                    border = BorderStroke(2.dp, ProfileColors.bgCard)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            artist.displayName.take(2).uppercase(),
                            fontSize = 32.sp,
                            fontWeight = FontWeight.Bold,
                            color = ProfileColors.gold
                        )
                    }
                }

                // Verification badge
                if (artist.verificationStatus == VerificationStatus.VERIFIED ||
                    artist.verificationStatus == VerificationStatus.OFFICIAL) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .size(28.dp)
                            .background(ProfileColors.gold, CircleShape)
                            .border(2.dp, ProfileColors.bgDark, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.Verified,
                            contentDescription = "Verified",
                            tint = Color.Black,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Name and handle
            Text(
                artist.displayName,
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )

            Text(
                "@${artist.handle}",
                fontSize = 14.sp,
                color = ProfileColors.textSecondary
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Role badge
            Box(
                modifier = Modifier
                    .background(
                        ProfileColors.gold.copy(alpha = 0.15f),
                        RoundedCornerShape(8.dp)
                    )
                    .padding(horizontal = 12.dp, vertical = 4.dp)
            ) {
                Text(
                    when (artist.role) {
                        ArtistRole.PRODUCER -> "Producer"
                        ArtistRole.DJ -> "DJ"
                        ArtistRole.PODCASTER -> "Podcaster"
                        ArtistRole.BAND -> "Band"
                        ArtistRole.LABEL -> "Label"
                        else -> "Artist"
                    },
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = ProfileColors.gold
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Action buttons
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Button(
                    onClick = onFollow,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isFollowing) ProfileColors.bgCard else ProfileColors.gold,
                        contentColor = if (isFollowing) ProfileColors.gold else Color.Black
                    ),
                    border = if (isFollowing) BorderStroke(1.dp, ProfileColors.gold) else null,
                    modifier = Modifier.width(120.dp)
                ) {
                    Icon(
                        if (isFollowing) Icons.Default.Check else Icons.Default.Add,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(if (isFollowing) "Following" else "Follow")
                }

                OutlinedButton(
                    onClick = { /* Share profile */ },
                    border = BorderStroke(1.dp, ProfileColors.textTertiary),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = Color.White
                    )
                ) {
                    Icon(
                        Icons.Default.Share,
                        contentDescription = "Share",
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun StatsRow(artist: ArtistProfile) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 16.dp),
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        StatItem(
            value = formatCountLarge(artist.followerCount),
            label = "Followers"
        )
        StatItem(
            value = formatCountLarge(artist.trackCount),
            label = "Tracks"
        )
        StatItem(
            value = formatCountLarge(artist.totalPlays),
            label = "Plays"
        )
    }
}

@Composable
private fun StatItem(value: String, label: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            value,
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White
        )
        Text(
            label,
            fontSize = 12.sp,
            color = ProfileColors.textSecondary
        )
    }
}

@Composable
private fun EmptyTracksState() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(40.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                Icons.Outlined.MusicNote,
                contentDescription = null,
                tint = ProfileColors.textTertiary,
                modifier = Modifier.size(48.dp)
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                "No tracks yet",
                fontSize = 16.sp,
                color = ProfileColors.textSecondary
            )
        }
    }
}

@Composable
private fun ArtistTrackItem(
    track: Track,
    onClick: () -> Unit,
    onPlay: () -> Unit,
    onRemix: () -> Unit
) {
    Surface(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp),
        shape = RoundedCornerShape(12.dp),
        color = ProfileColors.bgCard.copy(alpha = 0.6f)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Play button
            IconButton(
                onClick = onPlay,
                modifier = Modifier.size(44.dp)
            ) {
                Icon(
                    Icons.Default.PlayCircle,
                    contentDescription = "Play",
                    tint = ProfileColors.gold,
                    modifier = Modifier.size(36.dp)
                )
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
                        overflow = TextOverflow.Ellipsis
                    )

                    if (track.isExplicit) {
                        Spacer(modifier = Modifier.width(6.dp))
                        Box(
                            modifier = Modifier
                                .background(ProfileColors.textTertiary.copy(alpha = 0.3f), RoundedCornerShape(2.dp))
                                .padding(horizontal = 4.dp, vertical = 1.dp)
                        ) {
                            Text("E", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = ProfileColors.textSecondary)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        formatDurationProfile(track.duration),
                        fontSize = 12.sp,
                        color = ProfileColors.textTertiary
                    )

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Outlined.PlayArrow,
                            contentDescription = null,
                            tint = ProfileColors.textTertiary,
                            modifier = Modifier.size(12.dp)
                        )
                        Text(
                            formatCountSmall(track.playCount),
                            fontSize = 12.sp,
                            color = ProfileColors.textTertiary
                        )
                    }

                    if (track.remixPermission != RemixPermission.NO_REMIX) {
                        Box(
                            modifier = Modifier
                                .background(ProfileColors.violet.copy(alpha = 0.2f), RoundedCornerShape(4.dp))
                                .padding(horizontal = 4.dp, vertical = 1.dp)
                        ) {
                            Text(
                                "Remix",
                                fontSize = 9.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = ProfileColors.violet
                            )
                        }
                    }
                }
            }

            // More options
            IconButton(
                onClick = { /* Show options */ },
                modifier = Modifier.size(32.dp)
            ) {
                Icon(
                    Icons.Default.MoreVert,
                    contentDescription = "More",
                    tint = ProfileColors.textTertiary,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

@Composable
private fun AboutSection(artist: ArtistProfile) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(20.dp)
    ) {
        // Bio
        if (artist.bio.isNotEmpty()) {
            Text(
                "Bio",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                artist.bio,
                fontSize = 14.sp,
                color = ProfileColors.textSecondary,
                lineHeight = 22.sp
            )
            Spacer(modifier = Modifier.height(20.dp))
        }

        // Genres
        if (artist.genres.isNotEmpty()) {
            Text(
                "Genres",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.horizontalScroll(rememberScrollState())
            ) {
                artist.genres.forEach { genre ->
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = ProfileColors.gold.copy(alpha = 0.15f),
                        border = BorderStroke(1.dp, ProfileColors.gold.copy(alpha = 0.3f))
                    ) {
                        Text(
                            genre,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                            fontSize = 13.sp,
                            color = ProfileColors.gold
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(20.dp))
        }

        // Moods
        if (artist.moods.isNotEmpty()) {
            Text(
                "Moods & Vibes",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.horizontalScroll(rememberScrollState())
            ) {
                artist.moods.forEach { mood ->
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = ProfileColors.rose.copy(alpha = 0.15f)
                    ) {
                        Text(
                            mood,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                            fontSize = 13.sp,
                            color = ProfileColors.rose
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(20.dp))
        }

        // Location
        if (artist.location != null) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Outlined.LocationOn,
                    contentDescription = null,
                    tint = ProfileColors.textTertiary,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    artist.location,
                    fontSize = 14.sp,
                    color = ProfileColors.textSecondary
                )
            }
            Spacer(modifier = Modifier.height(12.dp))
        }

        // Joined date
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                Icons.Outlined.CalendarMonth,
                contentDescription = null,
                tint = ProfileColors.textTertiary,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                "Joined ${formatDate(artist.joinedAt)}",
                fontSize = 14.sp,
                color = ProfileColors.textSecondary
            )
        }
    }
}

@Composable
private fun StatsSection(artist: ArtistProfile) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(20.dp)
    ) {
        Text(
            "Performance Overview",
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Stats cards grid
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            StatsCard(
                title = "Total Plays",
                value = formatCountLarge(artist.totalPlays),
                icon = Icons.Outlined.PlayArrow,
                color = ProfileColors.gold,
                modifier = Modifier.weight(1f)
            )
            StatsCard(
                title = "Followers",
                value = formatCountLarge(artist.followerCount),
                icon = Icons.Outlined.People,
                color = ProfileColors.cyan,
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            StatsCard(
                title = "Tracks",
                value = artist.trackCount.toString(),
                icon = Icons.Outlined.MusicNote,
                color = ProfileColors.rose,
                modifier = Modifier.weight(1f)
            )
            StatsCard(
                title = "Following",
                value = artist.followingCount.toString(),
                icon = Icons.Outlined.PersonAdd,
                color = ProfileColors.violet,
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Coming soon analytics
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            color = ProfileColors.bgCard,
            border = BorderStroke(1.dp, ProfileColors.bgGlass)
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    Icons.Outlined.Analytics,
                    contentDescription = null,
                    tint = ProfileColors.gold,
                    modifier = Modifier.size(40.dp)
                )
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    "Advanced Analytics",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.White
                )
                Text(
                    "Detailed insights coming soon",
                    fontSize = 13.sp,
                    color = ProfileColors.textSecondary
                )
            }
        }
    }
}

@Composable
private fun StatsCard(
    title: String,
    value: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    color: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        color = color.copy(alpha = 0.1f),
        border = BorderStroke(1.dp, color.copy(alpha = 0.2f))
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    icon,
                    contentDescription = null,
                    tint = color,
                    modifier = Modifier.size(20.dp)
                )
                Text(
                    title,
                    fontSize = 12.sp,
                    color = ProfileColors.textSecondary
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                value,
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = color
            )
        }
    }
}

// Utility functions
private fun formatCountLarge(count: Long): String {
    return when {
        count >= 1_000_000 -> String.format("%.1fM", count / 1_000_000.0)
        count >= 1_000 -> String.format("%.1fK", count / 1_000.0)
        else -> count.toString()
    }
}

private fun formatCountLarge(count: Int): String = formatCountLarge(count.toLong())

private fun formatCountSmall(count: Long): String {
    return when {
        count >= 1_000_000 -> String.format("%.1fM", count / 1_000_000.0)
        count >= 1_000 -> String.format("%.1fK", count / 1_000.0)
        else -> count.toString()
    }
}

private fun formatDurationProfile(durationMs: Long): String {
    val totalSeconds = durationMs / 1000
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return "%d:%02d".format(minutes, seconds)
}

private fun formatDate(timestamp: Long): String {
    val sdf = java.text.SimpleDateFormat("MMM yyyy", java.util.Locale.getDefault())
    return sdf.format(java.util.Date(timestamp))
}

