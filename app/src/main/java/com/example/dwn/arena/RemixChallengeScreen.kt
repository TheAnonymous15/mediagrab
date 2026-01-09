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
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
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
import kotlin.math.cos
import kotlin.math.sin

// ============================================
// CHALLENGE COLORS
// ============================================

private object ChallengeColors {
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

    val gradientChallenge = Brush.horizontalGradient(listOf(violet, rose, amber))
}

// ============================================
// 🏆 REMIX CHALLENGE SCREEN
// ============================================

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RemixChallengeScreen(
    challengeId: String,
    onBack: () -> Unit,
    onPlayOriginal: (Track) -> Unit = {},
    onSubmitRemix: () -> Unit = {},
    onViewSubmission: (String) -> Unit = {},
    onArtistClick: (String) -> Unit = {}
) {
    BackHandler { onBack() }

    val repository = remember { ArenaRepository.getInstance() }
    var challenge by remember { mutableStateOf<RemixChallenge?>(null) }
    var originalTrack by remember { mutableStateOf<Track?>(null) }
    var submissions by remember { mutableStateOf<List<Track>>(emptyList()) }

    val scope = rememberCoroutineScope()
    val haptic = LocalHapticFeedback.current

    LaunchedEffect(challengeId) {
        // Mock data - in production would fetch from repository
        challenge = RemixChallenge(
            id = challengeId,
            title = "Remix 'Earthquake'",
            description = "Put your spin on Bass Kingdom's latest banger! Create your own unique remix and submit it for a chance to win amazing prizes. Top 3 remixes get featured on their radio show.",
            originalTrackId = "track_3",
            hostArtistId = "artist_3",
            hostArtistName = "Bass Kingdom",
            startDate = System.currentTimeMillis() - 7 * 24 * 60 * 60 * 1000,
            endDate = System.currentTimeMillis() + 14 * 24 * 60 * 60 * 1000,
            prizes = listOf("🥇 Radio Feature + Official Collab", "🥈 1000 Followers Boost", "🥉 Merch Package"),
            submissionCount = 23,
            isActive = true
        )
        originalTrack = repository.getTrack("track_3")
        submissions = repository.feedTracks.value.take(5) // Mock submissions
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(ChallengeColors.bgDark)
    ) {
        // Animated challenge background
        ChallengeBackground()

        if (challenge == null) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = ChallengeColors.violet)
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize()
            ) {
                // Header
                item {
                    ChallengeHeader(
                        challenge = challenge!!,
                        onBack = onBack
                    )
                }

                // Original track
                item {
                    OriginalTrackSection(
                        track = originalTrack,
                        onPlay = { originalTrack?.let { onPlayOriginal(it) } },
                        onArtistClick = { originalTrack?.let { onArtistClick(it.artistId) } }
                    )
                }

                // Challenge details
                item {
                    ChallengeDetailsSection(challenge = challenge!!)
                }

                // Prizes
                item {
                    PrizesSection(prizes = challenge!!.prizes)
                }

                // Rules
                item {
                    RulesSection()
                }

                // Submit button
                item {
                    SubmitSection(
                        onSubmit = {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            onSubmitRemix()
                        }
                    )
                }

                // Submissions
                item {
                    SubmissionsHeader(count = challenge!!.submissionCount)
                }

                items(submissions) { submission ->
                    SubmissionItem(
                        track = submission,
                        onClick = { onViewSubmission(submission.id) },
                        onPlay = { /* Play submission */ }
                    )
                }

                item {
                    Spacer(modifier = Modifier.height(100.dp))
                }
            }
        }
    }
}

@Composable
private fun ChallengeBackground() {
    val transition = rememberInfiniteTransition(label = "challenge_bg")

    val rotation by transition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(20000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "rotation"
    )

    val pulse by transition.animateFloat(
        initialValue = 0.8f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse"
    )

    Canvas(modifier = Modifier.fillMaxSize()) {
        val w = size.width
        val h = size.height

        // Rotating gradient orbs
        val orb1X = w * 0.2f + cos(Math.toRadians(rotation.toDouble())).toFloat() * w * 0.1f
        val orb1Y = h * 0.15f + sin(Math.toRadians(rotation.toDouble())).toFloat() * h * 0.05f

        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(
                    ChallengeColors.violet.copy(alpha = pulse * 0.12f),
                    Color.Transparent
                ),
                radius = w * 0.4f
            ),
            radius = w * 0.35f,
            center = Offset(orb1X, orb1Y)
        )

        val orb2X = w * 0.8f - cos(Math.toRadians(rotation.toDouble())).toFloat() * w * 0.08f
        val orb2Y = h * 0.5f + sin(Math.toRadians(rotation.toDouble())).toFloat() * h * 0.03f

        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(
                    ChallengeColors.rose.copy(alpha = pulse * 0.08f),
                    Color.Transparent
                ),
                radius = w * 0.3f
            ),
            radius = w * 0.25f,
            center = Offset(orb2X, orb2Y)
        )
    }
}

@Composable
private fun ChallengeHeader(
    challenge: RemixChallenge,
    onBack: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(280.dp)
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
                .padding(top = 60.dp, start = 20.dp, end = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Challenge badge
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = ChallengeColors.violet.copy(alpha = 0.2f),
                border = BorderStroke(1.dp, ChallengeColors.violet)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.EmojiEvents,
                        contentDescription = null,
                        tint = ChallengeColors.gold,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        "REMIX CHALLENGE",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = ChallengeColors.violet,
                        letterSpacing = 1.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Title
            Text(
                challenge.title,
                fontSize = 28.sp,
                fontWeight = FontWeight.Black,
                color = Color.White,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Host
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "Hosted by ",
                    fontSize = 14.sp,
                    color = ChallengeColors.textSecondary
                )
                Text(
                    challenge.hostArtistName,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = ChallengeColors.gold
                )
                Spacer(modifier = Modifier.width(4.dp))
                Icon(
                    Icons.Default.Verified,
                    contentDescription = "Verified",
                    tint = ChallengeColors.gold,
                    modifier = Modifier.size(14.dp)
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Time remaining
            val daysRemaining = ((challenge.endDate - System.currentTimeMillis()) / (24 * 60 * 60 * 1000)).toInt()

            Surface(
                shape = RoundedCornerShape(12.dp),
                color = if (daysRemaining <= 3) ChallengeColors.rose.copy(alpha = 0.2f)
                else ChallengeColors.emerald.copy(alpha = 0.2f)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.Timer,
                        contentDescription = null,
                        tint = if (daysRemaining <= 3) ChallengeColors.rose else ChallengeColors.emerald,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        if (daysRemaining > 0) "$daysRemaining days remaining" else "Ending soon!",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = if (daysRemaining <= 3) ChallengeColors.rose else ChallengeColors.emerald
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Stats
            Row(
                horizontalArrangement = Arrangement.spacedBy(24.dp)
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        "${challenge.submissionCount}",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Text(
                        "Entries",
                        fontSize = 11.sp,
                        color = ChallengeColors.textTertiary
                    )
                }

                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        "${challenge.prizes.size}",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = ChallengeColors.gold
                    )
                    Text(
                        "Prizes",
                        fontSize = 11.sp,
                        color = ChallengeColors.textTertiary
                    )
                }
            }
        }
    }
}

@Composable
private fun OriginalTrackSection(
    track: Track?,
    onPlay: () -> Unit,
    onArtistClick: () -> Unit
) {
    Column(
        modifier = Modifier.padding(16.dp)
    ) {
        Text(
            "Original Track",
            fontSize = 16.sp,
            fontWeight = FontWeight.SemiBold,
            color = Color.White
        )

        Spacer(modifier = Modifier.height(12.dp))

        if (track != null) {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                color = ChallengeColors.bgCard,
                border = BorderStroke(1.dp, ChallengeColors.violet.copy(alpha = 0.3f))
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Play button
                    Surface(
                        onClick = onPlay,
                        modifier = Modifier.size(56.dp),
                        shape = RoundedCornerShape(12.dp),
                        color = ChallengeColors.violet.copy(alpha = 0.2f)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                Icons.Default.PlayArrow,
                                contentDescription = "Play",
                                tint = ChallengeColors.violet,
                                modifier = Modifier.size(32.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            track.title,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color.White
                        )
                        Row(
                            modifier = Modifier.clickable(onClick = onArtistClick),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                track.artistName,
                                fontSize = 13.sp,
                                color = ChallengeColors.textSecondary
                            )
                            if (track.isVerified) {
                                Spacer(modifier = Modifier.width(4.dp))
                                Icon(
                                    Icons.Default.Verified,
                                    contentDescription = "Verified",
                                    tint = ChallengeColors.gold,
                                    modifier = Modifier.size(12.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            Text(
                                "${track.bpm ?: "—"} BPM",
                                fontSize = 11.sp,
                                color = ChallengeColors.violet
                            )
                            Text(
                                track.key ?: "—",
                                fontSize = 11.sp,
                                color = ChallengeColors.violet
                            )
                            Text(
                                formatDuration(track.duration),
                                fontSize = 11.sp,
                                color = ChallengeColors.textTertiary
                            )
                        }
                    }

                    IconButton(onClick = { /* Download stems */ }) {
                        Icon(
                            Icons.Outlined.Download,
                            contentDescription = "Download stems",
                            tint = ChallengeColors.gold
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Download stems button
            OutlinedButton(
                onClick = { /* Download stems */ },
                modifier = Modifier.fillMaxWidth(),
                border = BorderStroke(1.dp, ChallengeColors.violet.copy(alpha = 0.5f)),
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = ChallengeColors.violet
                )
            ) {
                Icon(
                    Icons.Outlined.FolderOpen,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Download Stems & Project Files")
            }
        }
    }
}

@Composable
private fun ChallengeDetailsSection(challenge: RemixChallenge) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        shape = RoundedCornerShape(16.dp),
        color = ChallengeColors.bgCard
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                "About This Challenge",
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color.White
            )

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                challenge.description,
                fontSize = 14.sp,
                color = ChallengeColors.textSecondary,
                lineHeight = 22.sp
            )
        }
    }
}

@Composable
private fun PrizesSection(prizes: List<String>) {
    Column(
        modifier = Modifier.padding(16.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Icons.Default.EmojiEvents,
                contentDescription = null,
                tint = ChallengeColors.gold,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                "Prizes",
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color.White
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        prizes.forEachIndexed { index, prize ->
            val (bgColor, borderColor) = when (index) {
                0 -> ChallengeColors.gold.copy(alpha = 0.15f) to ChallengeColors.gold.copy(alpha = 0.5f)
                1 -> Color(0xFFC0C0C0).copy(alpha = 0.15f) to Color(0xFFC0C0C0).copy(alpha = 0.5f)
                2 -> Color(0xFFCD7F32).copy(alpha = 0.15f) to Color(0xFFCD7F32).copy(alpha = 0.5f)
                else -> ChallengeColors.bgCard to ChallengeColors.bgGlass
            }

            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                shape = RoundedCornerShape(12.dp),
                color = bgColor,
                border = BorderStroke(1.dp, borderColor)
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        when (index) {
                            0 -> "🥇"
                            1 -> "🥈"
                            2 -> "🥉"
                            else -> "🎁"
                        },
                        fontSize = 24.sp
                    )

                    Spacer(modifier = Modifier.width(12.dp))

                    Column {
                        Text(
                            when (index) {
                                0 -> "1st Place"
                                1 -> "2nd Place"
                                2 -> "3rd Place"
                                else -> "Runner Up"
                            },
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = when (index) {
                                0 -> ChallengeColors.gold
                                1 -> Color(0xFFC0C0C0)
                                2 -> Color(0xFFCD7F32)
                                else -> ChallengeColors.textSecondary
                            }
                        )
                        Text(
                            prize.replace(Regex("^[🥇🥈🥉]\\s*"), ""),
                            fontSize = 14.sp,
                            color = Color.White
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun RulesSection() {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        shape = RoundedCornerShape(16.dp),
        color = ChallengeColors.bgCard
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Outlined.Rule,
                    contentDescription = null,
                    tint = ChallengeColors.cyan,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    "Rules & Guidelines",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.White
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            val rules = listOf(
                "Your remix must be original work",
                "Use the provided stems or full track",
                "Minimum 1 minute, maximum 5 minutes",
                "No copyrighted samples without clearance",
                "One submission per artist",
                "Winners announced within 7 days of deadline"
            )

            rules.forEach { rule ->
                Row(
                    modifier = Modifier.padding(vertical = 4.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    Icon(
                        Icons.Default.CheckCircle,
                        contentDescription = null,
                        tint = ChallengeColors.emerald,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        rule,
                        fontSize = 13.sp,
                        color = ChallengeColors.textSecondary
                    )
                }
            }
        }
    }
}

@Composable
private fun SubmitSection(onSubmit: () -> Unit) {
    Column(
        modifier = Modifier.padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Button(
            onClick = onSubmit,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = ChallengeColors.violet,
                contentColor = Color.White
            ),
            shape = RoundedCornerShape(16.dp)
        ) {
            Icon(
                Icons.Default.CloudUpload,
                contentDescription = null
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                "Submit Your Remix",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            "Open Remix Studio to create your entry",
            fontSize = 12.sp,
            color = ChallengeColors.textTertiary
        )
    }
}

@Composable
private fun SubmissionsHeader(count: Int) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            "Submissions",
            fontSize = 16.sp,
            fontWeight = FontWeight.SemiBold,
            color = Color.White
        )

        Spacer(modifier = Modifier.width(8.dp))

        Surface(
            shape = RoundedCornerShape(8.dp),
            color = ChallengeColors.violet.copy(alpha = 0.2f)
        ) {
            Text(
                "$count",
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = ChallengeColors.violet
            )
        }

        Spacer(modifier = Modifier.weight(1f))

        // Sort dropdown
        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                "Sort by: ",
                fontSize = 12.sp,
                color = ChallengeColors.textTertiary
            )
            Text(
                "Most Liked",
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                color = ChallengeColors.gold
            )
            Icon(
                Icons.Default.ArrowDropDown,
                contentDescription = null,
                tint = ChallengeColors.gold,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

@Composable
private fun SubmissionItem(
    track: Track,
    onClick: () -> Unit,
    onPlay: () -> Unit
) {
    Surface(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp),
        shape = RoundedCornerShape(12.dp),
        color = ChallengeColors.bgCard.copy(alpha = 0.6f)
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
                    shape = CircleShape,
                    color = ChallengeColors.violet.copy(alpha = 0.2f)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            Icons.Default.PlayArrow,
                            contentDescription = "Play",
                            tint = ChallengeColors.violet,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    "${track.title} (Remix)",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.White,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        "by ${track.artistName}",
                        fontSize = 12.sp,
                        color = ChallengeColors.textSecondary
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))

                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.Favorite,
                            contentDescription = null,
                            tint = ChallengeColors.rose,
                            modifier = Modifier.size(12.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            "${track.likeCount}",
                            fontSize = 11.sp,
                            color = ChallengeColors.textTertiary
                        )
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.PlayArrow,
                            contentDescription = null,
                            tint = ChallengeColors.textTertiary,
                            modifier = Modifier.size(12.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            "${track.playCount}",
                            fontSize = 11.sp,
                            color = ChallengeColors.textTertiary
                        )
                    }
                }
            }

            // Vote button
            OutlinedButton(
                onClick = { /* Vote */ },
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                border = BorderStroke(1.dp, ChallengeColors.violet.copy(alpha = 0.5f))
            ) {
                Icon(
                    Icons.Default.ThumbUp,
                    contentDescription = null,
                    tint = ChallengeColors.violet,
                    modifier = Modifier.size(14.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    "Vote",
                    fontSize = 12.sp,
                    color = ChallengeColors.violet
                )
            }
        }
    }
}

// ============================================
// UTILITY FUNCTIONS
// ============================================

private fun formatDuration(durationMs: Long): String {
    val totalSeconds = durationMs / 1000
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return "%d:%02d".format(minutes, seconds)
}

// ============================================
// 🏆 CHALLENGES LIST SCREEN
// ============================================

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChallengesListScreen(
    onBack: () -> Unit,
    onChallengeClick: (String) -> Unit = {},
    onCreateChallenge: () -> Unit = {}
) {
    BackHandler { onBack() }

    val repository = remember { ArenaRepository.getInstance() }
    val challenges by repository.activeChallenges.collectAsState()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(ChallengeColors.bgDark)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
        ) {
            // Top bar
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.EmojiEvents,
                            contentDescription = null,
                            tint = ChallengeColors.gold,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                "Remix Challenges",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            Text(
                                "Compete and win prizes",
                                fontSize = 11.sp,
                                color = ChallengeColors.textSecondary
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
                    IconButton(onClick = onCreateChallenge) {
                        Icon(
                            Icons.Default.Add,
                            contentDescription = "Create Challenge",
                            tint = ChallengeColors.gold
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent
                )
            )

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Active challenges
                item {
                    Text(
                        "Active Challenges",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.White
                    )
                }

                items(challenges.filter { it.isActive }) { challenge ->
                    ChallengeListItem(
                        challenge = challenge,
                        onClick = { onChallengeClick(challenge.id) }
                    )
                }

                // Past challenges
                item {
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        "Past Challenges",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = ChallengeColors.textSecondary
                    )
                }

                item {
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        color = ChallengeColors.bgCard.copy(alpha = 0.5f)
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(32.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                "Past challenges archive coming soon",
                                color = ChallengeColors.textTertiary
                            )
                        }
                    }
                }

                item {
                    Spacer(modifier = Modifier.height(80.dp))
                }
            }
        }
    }
}

@Composable
private fun ChallengeListItem(
    challenge: RemixChallenge,
    onClick: () -> Unit
) {
    val daysRemaining = ((challenge.endDate - System.currentTimeMillis()) / (24 * 60 * 60 * 1000)).toInt()

    Surface(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = ChallengeColors.bgCard,
        border = BorderStroke(
            1.dp,
            Brush.horizontalGradient(
                listOf(
                    ChallengeColors.violet.copy(alpha = 0.5f),
                    ChallengeColors.rose.copy(alpha = 0.3f)
                )
            )
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Status badge
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = if (daysRemaining <= 3) ChallengeColors.rose.copy(alpha = 0.2f)
                    else ChallengeColors.emerald.copy(alpha = 0.2f)
                ) {
                    Text(
                        if (daysRemaining > 0) "$daysRemaining days left" else "Ending soon",
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (daysRemaining <= 3) ChallengeColors.rose else ChallengeColors.emerald
                    )
                }

                Text(
                    "${challenge.submissionCount} entries",
                    fontSize = 12.sp,
                    color = ChallengeColors.textTertiary
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                challenge.title,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )

            Spacer(modifier = Modifier.height(4.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "by ${challenge.hostArtistName}",
                    fontSize = 13.sp,
                    color = ChallengeColors.textSecondary
                )
                Spacer(modifier = Modifier.width(4.dp))
                Icon(
                    Icons.Default.Verified,
                    contentDescription = "Verified",
                    tint = ChallengeColors.gold,
                    modifier = Modifier.size(12.dp)
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                challenge.description,
                fontSize = 13.sp,
                color = ChallengeColors.textTertiary,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Prizes preview
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Default.EmojiEvents,
                    contentDescription = null,
                    tint = ChallengeColors.gold,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    challenge.prizes.firstOrNull()?.replace(Regex("^[🥇🥈🥉]\\s*"), "") ?: "Prizes available",
                    fontSize = 12.sp,
                    color = ChallengeColors.gold
                )
            }
        }
    }
}

