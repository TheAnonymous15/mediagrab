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
import androidx.compose.ui.graphics.graphicsLayer
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
// TRACK DETAIL COLORS
// ============================================

private object TrackColors {
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
// 🎵 TRACK DETAIL SCREEN
// ============================================

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TrackDetailScreen(
    trackId: String,
    onBack: () -> Unit,
    onArtistClick: (String) -> Unit = {},
    onPlayTrack: (Track) -> Unit = {},
    onRemixTrack: (String) -> Unit = {},
    onShareTrack: (Track) -> Unit = {}
) {
    BackHandler { onBack() }

    val repository = remember { ArenaRepository.getInstance() }
    var track by remember { mutableStateOf<Track?>(null) }
    var comments by remember { mutableStateOf<List<TrackComment>>(emptyList()) }
    var isPlaying by remember { mutableStateOf(false) }
    var showCommentSheet by remember { mutableStateOf(false) }
    var newComment by remember { mutableStateOf("") }

    val scope = rememberCoroutineScope()
    val haptic = LocalHapticFeedback.current

    LaunchedEffect(trackId) {
        track = repository.getTrack(trackId)
        comments = repository.getComments(trackId)
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(TrackColors.bgDark)
    ) {
        if (track == null) {
            // Loading state
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = TrackColors.gold)
            }
        } else {
            // Animated background based on track mood
            TrackDetailBackground(moods = track!!.moods)

            LazyColumn(
                modifier = Modifier.fillMaxSize()
            ) {
                // Header with cover art
                item {
                    TrackHeader(
                        track = track!!,
                        isPlaying = isPlaying,
                        onBack = onBack,
                        onPlay = {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            isPlaying = !isPlaying
                            if (isPlaying) onPlayTrack(track!!)
                        }
                    )
                }

                // Waveform visualizer
                item {
                    WaveformSection(
                        waveformData = track!!.waveformData.ifEmpty {
                            List(100) { (Math.random() * 0.8 + 0.2).toFloat() }
                        },
                        isPlaying = isPlaying
                    )
                }

                // Action buttons
                item {
                    ActionButtonsRow(
                        track = track!!,
                        onLike = {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            scope.launch { repository.likeTrack(trackId) }
                        },
                        onSave = {
                            scope.launch { repository.saveTrack(trackId) }
                        },
                        onComment = { showCommentSheet = true },
                        onRemix = { onRemixTrack(trackId) },
                        onShare = { onShareTrack(track!!) }
                    )
                }

                // Track info section
                item {
                    TrackInfoSection(
                        track = track!!,
                        onArtistClick = { onArtistClick(track!!.artistId) }
                    )
                }

                // Stats section
                item {
                    StatsSection(track = track!!)
                }

                // Technical info
                item {
                    TechnicalInfoSection(track = track!!)
                }

                // Comments section header
                item {
                    CommentsHeader(
                        commentCount = track!!.commentCount,
                        onAddComment = { showCommentSheet = true }
                    )
                }

                // Comments list
                if (comments.isEmpty()) {
                    item {
                        EmptyCommentsState()
                    }
                } else {
                    items(comments.take(5)) { comment ->
                        CommentItem(
                            comment = comment,
                            onLike = { /* Like comment */ }
                        )
                    }

                    if (comments.size > 5) {
                        item {
                            TextButton(
                                onClick = { showCommentSheet = true },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp)
                            ) {
                                Text(
                                    "View all ${comments.size} comments",
                                    color = TrackColors.gold
                                )
                            }
                        }
                    }
                }

                // Related tracks section
                item {
                    RelatedTracksSection()
                }

                item {
                    Spacer(modifier = Modifier.height(100.dp))
                }
            }
        }

        // Comment sheet
        if (showCommentSheet) {
            ModalBottomSheet(
                onDismissRequest = { showCommentSheet = false },
                containerColor = TrackColors.bgCard
            ) {
                CommentSheet(
                    comments = comments,
                    newComment = newComment,
                    onCommentChange = { newComment = it },
                    onSubmit = {
                        if (newComment.isNotBlank()) {
                            scope.launch {
                                repository.addComment(trackId, newComment)
                                comments = repository.getComments(trackId)
                                newComment = ""
                            }
                        }
                    },
                    onDismiss = { showCommentSheet = false }
                )
            }
        }
    }
}

@Composable
private fun TrackDetailBackground(moods: List<String>) {
    val transition = rememberInfiniteTransition(label = "bg")

    val wave by transition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(10000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "wave"
    )

    // Determine colors based on moods
    val primaryColor = when {
        moods.any { it.lowercase() in listOf("energetic", "aggressive", "intense") } -> TrackColors.rose
        moods.any { it.lowercase() in listOf("peaceful", "relaxing", "dreamy") } -> TrackColors.cyan
        moods.any { it.lowercase() in listOf("dark", "melancholic") } -> TrackColors.violet
        else -> TrackColors.gold
    }

    Canvas(modifier = Modifier.fillMaxSize()) {
        val w = size.width
        val h = size.height

        // Primary orb
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(
                    primaryColor.copy(alpha = 0.12f),
                    Color.Transparent
                ),
                radius = w * 0.5f
            ),
            radius = w * 0.4f,
            center = androidx.compose.ui.geometry.Offset(w * 0.2f, h * 0.15f)
        )

        // Secondary animated orb
        val offsetY = sin(Math.toRadians(wave.toDouble())).toFloat() * h * 0.05f
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(
                    TrackColors.amber.copy(alpha = 0.08f),
                    Color.Transparent
                ),
                radius = w * 0.3f
            ),
            radius = w * 0.25f,
            center = androidx.compose.ui.geometry.Offset(w * 0.8f, h * 0.4f + offsetY)
        )
    }
}

@Composable
private fun TrackHeader(
    track: Track,
    isPlaying: Boolean,
    onBack: () -> Unit,
    onPlay: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(380.dp)
    ) {
        // Back button
        IconButton(
            onClick = onBack,
            modifier = Modifier
                .statusBarsPadding()
                .padding(8.dp)
                .align(Alignment.TopStart)
        ) {
            Icon(
                Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = "Back",
                tint = Color.White
            )
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = 60.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Cover art with play button
            Box(
                modifier = Modifier.size(200.dp),
                contentAlignment = Alignment.Center
            ) {
                // Animated ring when playing
                if (isPlaying) {
                    val rotation by rememberInfiniteTransition(label = "rotate").animateFloat(
                        initialValue = 0f,
                        targetValue = 360f,
                        animationSpec = infiniteRepeatable(
                            animation = tween(4000, easing = LinearEasing),
                            repeatMode = RepeatMode.Restart
                        ),
                        label = "rotation"
                    )

                    Box(
                        modifier = Modifier
                            .size(200.dp)
                            .graphicsLayer { rotationZ = rotation }
                            .border(
                                2.dp,
                                Brush.sweepGradient(
                                    listOf(
                                        TrackColors.gold,
                                        TrackColors.amber,
                                        TrackColors.rose,
                                        TrackColors.gold
                                    )
                                ),
                                CircleShape
                            )
                    )
                }

                // Cover art
                Surface(
                    modifier = Modifier.size(180.dp),
                    shape = RoundedCornerShape(24.dp),
                    color = TrackColors.bgElevated,
                    shadowElevation = 16.dp
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            Icons.Default.MusicNote,
                            contentDescription = null,
                            tint = TrackColors.gold.copy(alpha = 0.5f),
                            modifier = Modifier.size(72.dp)
                        )
                    }
                }

                // Play button overlay
                Surface(
                    onClick = onPlay,
                    modifier = Modifier.size(64.dp),
                    shape = CircleShape,
                    color = TrackColors.gold,
                    shadowElevation = 8.dp
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                            contentDescription = if (isPlaying) "Pause" else "Play",
                            tint = Color.Black,
                            modifier = Modifier.size(32.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Track title
            Text(
                track.title,
                fontSize = 24.sp,
                fontWeight = FontWeight.Black,
                color = Color.White,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 24.dp)
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Artist name
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Text(
                    track.artistName,
                    fontSize = 16.sp,
                    color = TrackColors.textSecondary
                )
                if (track.isVerified) {
                    Spacer(modifier = Modifier.width(6.dp))
                    Icon(
                        Icons.Default.Verified,
                        contentDescription = "Verified",
                        tint = TrackColors.gold,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Genres & tags
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                track.genres.take(3).forEach { genre ->
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = TrackColors.gold.copy(alpha = 0.15f)
                    ) {
                        Text(
                            genre,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                            fontSize = 12.sp,
                            color = TrackColors.gold
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun WaveformSection(
    waveformData: List<Float>,
    isPlaying: Boolean
) {
    val transition = rememberInfiniteTransition(label = "waveform")
    val phase by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "phase"
    )

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .height(80.dp),
        shape = RoundedCornerShape(16.dp),
        color = TrackColors.bgCard
    ) {
        Canvas(modifier = Modifier.fillMaxSize().padding(12.dp)) {
            val barWidth = size.width / waveformData.size
            val maxHeight = size.height

            waveformData.forEachIndexed { index, amplitude ->
                val animatedAmplitude = if (isPlaying) {
                    val phaseOffset = (index.toFloat() / waveformData.size + phase) % 1f
                    amplitude * (0.7f + 0.3f * sin(phaseOffset * 2 * Math.PI).toFloat())
                } else {
                    amplitude
                }

                val barHeight = maxHeight * animatedAmplitude
                val x = index * barWidth + barWidth / 2
                val y = (maxHeight - barHeight) / 2

                drawRoundRect(
                    color = if (isPlaying) {
                        val hue = (index.toFloat() / waveformData.size * 60f) // Gold to amber gradient
                        TrackColors.gold.copy(alpha = 0.4f + animatedAmplitude * 0.6f)
                    } else {
                        TrackColors.gold.copy(alpha = 0.3f)
                    },
                    topLeft = androidx.compose.ui.geometry.Offset(x, y),
                    size = androidx.compose.ui.geometry.Size(barWidth * 0.6f, barHeight),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(2f, 2f)
                )
            }
        }
    }
}

@Composable
private fun ActionButtonsRow(
    track: Track,
    onLike: () -> Unit,
    onSave: () -> Unit,
    onComment: () -> Unit,
    onRemix: () -> Unit,
    onShare: () -> Unit
) {
    var isLiked by remember { mutableStateOf(track.isLiked) }
    var isSaved by remember { mutableStateOf(track.isSaved) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        // Like
        ActionButton(
            icon = if (isLiked) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
            label = formatCount(track.likeCount.toLong()),
            tint = if (isLiked) TrackColors.rose else TrackColors.textSecondary,
            onClick = {
                isLiked = !isLiked
                onLike()
            }
        )

        // Save
        ActionButton(
            icon = if (isSaved) Icons.Filled.Bookmark else Icons.Outlined.BookmarkBorder,
            label = formatCount(track.saveCount.toLong()),
            tint = if (isSaved) TrackColors.gold else TrackColors.textSecondary,
            onClick = {
                isSaved = !isSaved
                onSave()
            }
        )

        // Comment
        ActionButton(
            icon = Icons.Outlined.Comment,
            label = formatCount(track.commentCount.toLong()),
            tint = TrackColors.textSecondary,
            onClick = onComment
        )

        // Remix
        if (track.remixPermission != RemixPermission.NO_REMIX) {
            ActionButton(
                icon = Icons.Default.Refresh,
                label = "${track.remixCount}",
                tint = TrackColors.violet,
                onClick = onRemix
            )
        }

        // Share
        ActionButton(
            icon = Icons.Outlined.Share,
            label = "Share",
            tint = TrackColors.textSecondary,
            onClick = onShare
        )
    }
}

@Composable
private fun ActionButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    tint: Color,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        IconButton(onClick = onClick) {
            Icon(
                icon,
                contentDescription = label,
                tint = tint,
                modifier = Modifier.size(28.dp)
            )
        }
        Text(
            label,
            fontSize = 12.sp,
            color = TrackColors.textTertiary
        )
    }
}

@Composable
private fun TrackInfoSection(
    track: Track,
    onArtistClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        shape = RoundedCornerShape(16.dp),
        color = TrackColors.bgCard
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Artist row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onArtistClick),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    modifier = Modifier.size(48.dp),
                    shape = CircleShape,
                    color = TrackColors.bgElevated
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            track.artistName.take(2).uppercase(),
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = TrackColors.gold
                        )
                    }
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            track.artistName,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color.White
                        )
                        if (track.isVerified) {
                            Spacer(modifier = Modifier.width(6.dp))
                            Icon(
                                Icons.Default.Verified,
                                contentDescription = "Verified",
                                tint = TrackColors.gold,
                                modifier = Modifier.size(14.dp)
                            )
                        }
                    }
                    Text(
                        "@${track.artistHandle}",
                        fontSize = 13.sp,
                        color = TrackColors.textTertiary
                    )
                }

                OutlinedButton(
                    onClick = onArtistClick,
                    border = BorderStroke(1.dp, TrackColors.gold),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp)
                ) {
                    Text(
                        "View Profile",
                        fontSize = 12.sp,
                        color = TrackColors.gold
                    )
                }
            }

            if (track.description.isNotEmpty()) {
                Spacer(modifier = Modifier.height(16.dp))
                Divider(color = TrackColors.bgGlass)
                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    track.description,
                    fontSize = 14.sp,
                    color = TrackColors.textSecondary,
                    lineHeight = 22.sp
                )
            }

            // Moods
            if (track.moods.isNotEmpty()) {
                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    track.moods.forEach { mood ->
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = TrackColors.rose.copy(alpha = 0.15f)
                        ) {
                            Text(
                                mood,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                fontSize = 11.sp,
                                color = TrackColors.rose
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun StatsSection(track: Track) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        shape = RoundedCornerShape(16.dp),
        color = TrackColors.bgCard
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            StatItem(
                value = formatCount(track.playCount),
                label = "Plays",
                icon = Icons.Default.PlayArrow
            )
            StatItem(
                value = "${(track.avgListenDuration * 100).toInt()}%",
                label = "Avg Listen",
                icon = Icons.Default.Timer
            )
            StatItem(
                value = "${track.radioSpins}",
                label = "Radio Spins",
                icon = Icons.Default.Radio
            )
            StatItem(
                value = "${track.remixCount}",
                label = "Remixes",
                icon = Icons.Default.Refresh
            )
        }
    }
}

@Composable
private fun StatItem(
    value: String,
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            icon,
            contentDescription = null,
            tint = TrackColors.gold,
            modifier = Modifier.size(20.dp)
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            value,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White
        )
        Text(
            label,
            fontSize = 11.sp,
            color = TrackColors.textTertiary
        )
    }
}

@Composable
private fun TechnicalInfoSection(track: Track) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        shape = RoundedCornerShape(16.dp),
        color = TrackColors.bgCard
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                "Technical Details",
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color.White
            )

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                TechInfoItem("Duration", formatDuration(track.duration))
                TechInfoItem("BPM", track.bpm?.toString() ?: "—")
                TechInfoItem("Key", track.key ?: "—")
                TechInfoItem("Loudness", "${track.loudnessLUFS.toInt()} LUFS")
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                TechInfoItem("Format", track.originalFormat)
                TechInfoItem("Bitrate", "${track.bitrate} kbps")
                TechInfoItem("Sample Rate", "${track.sampleRate / 1000}kHz")
                TechInfoItem(
                    "Remix",
                    when (track.remixPermission) {
                        RemixPermission.NO_REMIX -> "No"
                        RemixPermission.PARTIAL_REMIX -> "Partial"
                        RemixPermission.FULL_REMIX -> "Yes"
                        RemixPermission.CREATIVE_COMMONS -> "CC"
                    }
                )
            }
        }
    }
}

@Composable
private fun TechInfoItem(label: String, value: String) {
    Column {
        Text(
            value,
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium,
            color = TrackColors.gold
        )
        Text(
            label,
            fontSize = 10.sp,
            color = TrackColors.textTertiary
        )
    }
}

@Composable
private fun CommentsHeader(
    commentCount: Int,
    onAddComment: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            "Comments",
            fontSize = 16.sp,
            fontWeight = FontWeight.SemiBold,
            color = Color.White
        )

        Spacer(modifier = Modifier.width(8.dp))

        Surface(
            shape = RoundedCornerShape(8.dp),
            color = TrackColors.bgGlass
        ) {
            Text(
                "$commentCount",
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                fontSize = 12.sp,
                color = TrackColors.textSecondary
            )
        }

        Spacer(modifier = Modifier.weight(1f))

        TextButton(onClick = onAddComment) {
            Icon(
                Icons.Default.Add,
                contentDescription = null,
                tint = TrackColors.gold,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                "Add Comment",
                color = TrackColors.gold,
                fontSize = 13.sp
            )
        }
    }
}

@Composable
private fun EmptyCommentsState() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            Icons.Outlined.ChatBubbleOutline,
            contentDescription = null,
            tint = TrackColors.textTertiary,
            modifier = Modifier.size(48.dp)
        )
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            "No comments yet",
            fontSize = 16.sp,
            fontWeight = FontWeight.Medium,
            color = TrackColors.textSecondary
        )
        Text(
            "Be the first to share your thoughts",
            fontSize = 13.sp,
            color = TrackColors.textTertiary
        )
    }
}

@Composable
private fun CommentItem(
    comment: TrackComment,
    onLike: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp),
        shape = RoundedCornerShape(12.dp),
        color = TrackColors.bgCard.copy(alpha = 0.6f)
    ) {
        Row(
            modifier = Modifier.padding(12.dp)
        ) {
            // Avatar
            Surface(
                modifier = Modifier.size(36.dp),
                shape = CircleShape,
                color = TrackColors.bgElevated
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        comment.userName.take(2).uppercase(),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (comment.isArtist) TrackColors.gold else TrackColors.textSecondary
                    )
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        comment.userName,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.White
                    )
                    if (comment.isArtist) {
                        Spacer(modifier = Modifier.width(6.dp))
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = TrackColors.gold.copy(alpha = 0.2f)
                        ) {
                            Text(
                                "ARTIST",
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 1.dp),
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                color = TrackColors.gold
                            )
                        }
                    }
                    if (comment.timestamp != null) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = TrackColors.cyan.copy(alpha = 0.15f)
                        ) {
                            Text(
                                formatDuration(comment.timestamp),
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 1.dp),
                                fontSize = 10.sp,
                                color = TrackColors.cyan
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    comment.content,
                    fontSize = 14.sp,
                    color = TrackColors.textSecondary,
                    lineHeight = 20.sp
                )

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        getRelativeTime(comment.createdAt),
                        fontSize = 11.sp,
                        color = TrackColors.textTertiary
                    )

                    Spacer(modifier = Modifier.weight(1f))

                    IconButton(
                        onClick = onLike,
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(
                            Icons.Outlined.ThumbUp,
                            contentDescription = "Like",
                            tint = TrackColors.textTertiary,
                            modifier = Modifier.size(14.dp)
                        )
                    }

                    Text(
                        "${comment.likeCount}",
                        fontSize = 11.sp,
                        color = TrackColors.textTertiary
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CommentSheet(
    comments: List<TrackComment>,
    newComment: String,
    onCommentChange: (String) -> Unit,
    onSubmit: () -> Unit,
    onDismiss: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(16.dp)
    ) {
        // Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                "Comments (${comments.size})",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
            Spacer(modifier = Modifier.weight(1f))
            IconButton(onClick = onDismiss) {
                Icon(
                    Icons.Default.Close,
                    contentDescription = "Close",
                    tint = TrackColors.textSecondary
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Comment input
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = newComment,
                onValueChange = onCommentChange,
                modifier = Modifier.weight(1f),
                placeholder = { Text("Write a comment...", color = TrackColors.textTertiary) },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = TrackColors.gold,
                    unfocusedBorderColor = TrackColors.bgGlass,
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White
                ),
                shape = RoundedCornerShape(24.dp),
                singleLine = true
            )

            Spacer(modifier = Modifier.width(8.dp))

            IconButton(
                onClick = onSubmit,
                enabled = newComment.isNotBlank()
            ) {
                Icon(
                    Icons.Default.Send,
                    contentDescription = "Send",
                    tint = if (newComment.isNotBlank()) TrackColors.gold else TrackColors.textTertiary
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Comments list
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f, fill = false)
                .heightIn(max = 400.dp)
        ) {
            items(comments) { comment ->
                CommentItem(
                    comment = comment,
                    onLike = { /* Like */ }
                )
            }
        }
    }
}

@Composable
private fun RelatedTracksSection() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
    ) {
        Text(
            "You might also like",
            fontSize = 16.sp,
            fontWeight = FontWeight.SemiBold,
            color = Color.White
        )

        Spacer(modifier = Modifier.height(12.dp))

        // Placeholder for related tracks
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .height(100.dp),
            shape = RoundedCornerShape(12.dp),
            color = TrackColors.bgCard
        ) {
            Box(
                contentAlignment = Alignment.Center
            ) {
                Text(
                    "Related tracks coming soon",
                    color = TrackColors.textTertiary
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

private fun getRelativeTime(timestamp: Long): String {
    val now = System.currentTimeMillis()
    val diff = now - timestamp

    return when {
        diff < 60_000 -> "Just now"
        diff < 3600_000 -> "${diff / 60_000}m ago"
        diff < 86400_000 -> "${diff / 3600_000}h ago"
        diff < 604800_000 -> "${diff / 86400_000}d ago"
        else -> "${diff / 604800_000}w ago"
    }
}

