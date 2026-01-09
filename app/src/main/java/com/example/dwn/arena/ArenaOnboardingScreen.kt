package com.example.dwn.arena

import androidx.activity.compose.BackHandler
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch

// ============================================
// SETTINGS COLORS
// ============================================

private object SettingsColors {
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
}

// ============================================
// 🎤 ARTIST ONBOARDING SCREEN
// ============================================

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ArtistOnboardingScreen(
    onBack: () -> Unit,
    onComplete: () -> Unit = {}
) {
    BackHandler { onBack() }

    val repository = remember { ArenaRepository.getInstance() }
    val scope = rememberCoroutineScope()
    val haptic = LocalHapticFeedback.current

    var currentStep by remember { mutableIntStateOf(0) }
    val steps = listOf("Role", "Profile", "Genres", "Finish")

    // Form state
    var selectedRole by remember { mutableStateOf<ArtistRole?>(null) }
    var displayName by remember { mutableStateOf("") }
    var handle by remember { mutableStateOf("") }
    var bio by remember { mutableStateOf("") }
    var selectedGenres by remember { mutableStateOf<List<String>>(emptyList()) }
    var isSubmitting by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(SettingsColors.bgDark)
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
                    Text(
                        "Become an Artist",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { if (currentStep > 0) currentStep-- else onBack() }) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = Color.White
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent
                )
            )

            // Progress indicator
            OnboardingProgress(
                currentStep = currentStep,
                totalSteps = steps.size
            )

            // Content
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .weight(1f)
            ) {
                when (currentStep) {
                    0 -> RoleSelectionStep(
                        selectedRole = selectedRole,
                        onRoleSelect = { selectedRole = it }
                    )
                    1 -> ProfileSetupStep(
                        displayName = displayName,
                        onDisplayNameChange = { displayName = it },
                        handle = handle,
                        onHandleChange = { handle = it.lowercase().replace(" ", "") },
                        bio = bio,
                        onBioChange = { bio = it }
                    )
                    2 -> GenreSelectionStep(
                        selectedGenres = selectedGenres,
                        onGenresChange = { selectedGenres = it }
                    )
                    3 -> FinishStep(
                        displayName = displayName,
                        role = selectedRole,
                        genres = selectedGenres,
                        isSubmitting = isSubmitting
                    )
                }
            }

            // Navigation buttons
            NavigationButtons(
                currentStep = currentStep,
                totalSteps = steps.size,
                canProceed = when (currentStep) {
                    0 -> selectedRole != null
                    1 -> displayName.isNotBlank() && handle.length >= 3
                    2 -> selectedGenres.isNotEmpty()
                    else -> true
                },
                isSubmitting = isSubmitting,
                onBack = { if (currentStep > 0) currentStep-- },
                onNext = {
                    if (currentStep < steps.size - 1) {
                        currentStep++
                    } else {
                        // Submit
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        isSubmitting = true
                        scope.launch {
                            repository.createArtistProfile(
                                displayName = displayName,
                                handle = handle,
                                bio = bio,
                                role = selectedRole ?: ArtistRole.INDEPENDENT_ARTIST,
                                genres = selectedGenres
                            )
                            isSubmitting = false
                            onComplete()
                        }
                    }
                }
            )
        }
    }
}

@Composable
private fun OnboardingProgress(
    currentStep: Int,
    totalSteps: Int
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        repeat(totalSteps) { step ->
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(4.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(
                        if (step <= currentStep) SettingsColors.gold
                        else SettingsColors.bgCard
                    )
            )
        }
    }
}

@Composable
private fun RoleSelectionStep(
    selectedRole: ArtistRole?,
    onRoleSelect: (ArtistRole) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp)
    ) {
        Text(
            "What describes you best?",
            fontSize = 24.sp,
            fontWeight = FontWeight.Black,
            color = Color.White
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            "Select your primary role as a creator",
            fontSize = 14.sp,
            color = SettingsColors.textSecondary
        )

        Spacer(modifier = Modifier.height(32.dp))

        val roles = listOf(
            Triple(ArtistRole.INDEPENDENT_ARTIST, "Independent Artist", "Singer, songwriter, solo creator"),
            Triple(ArtistRole.PRODUCER, "Producer", "Beat maker, music producer"),
            Triple(ArtistRole.DJ, "DJ", "Live mixing, remixing, sets"),
            Triple(ArtistRole.PODCASTER, "Podcaster", "Audio content creator"),
            Triple(ArtistRole.BAND, "Band / Group", "Part of a musical group"),
            Triple(ArtistRole.LABEL, "Label / Collective", "Managing multiple artists")
        )

        roles.forEach { (role, title, description) ->
            RoleCard(
                title = title,
                description = description,
                isSelected = selectedRole == role,
                onClick = { onRoleSelect(role) }
            )
            Spacer(modifier = Modifier.height(12.dp))
        }
    }
}

@Composable
private fun RoleCard(
    title: String,
    description: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = if (isSelected) SettingsColors.gold.copy(alpha = 0.15f) else SettingsColors.bgCard,
        border = BorderStroke(
            2.dp,
            if (isSelected) SettingsColors.gold else SettingsColors.bgGlass
        )
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    title,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = if (isSelected) SettingsColors.gold else Color.White
                )
                Text(
                    description,
                    fontSize = 13.sp,
                    color = SettingsColors.textTertiary
                )
            }

            if (isSelected) {
                Box(
                    modifier = Modifier
                        .size(24.dp)
                        .background(SettingsColors.gold, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.Check,
                        contentDescription = null,
                        tint = Color.Black,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun ProfileSetupStep(
    displayName: String,
    onDisplayNameChange: (String) -> Unit,
    handle: String,
    onHandleChange: (String) -> Unit,
    bio: String,
    onBioChange: (String) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp)
    ) {
        Text(
            "Set up your profile",
            fontSize = 24.sp,
            fontWeight = FontWeight.Black,
            color = Color.White
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            "This is how you'll appear to your fans",
            fontSize = 14.sp,
            color = SettingsColors.textSecondary
        )

        Spacer(modifier = Modifier.height(32.dp))

        // Avatar placeholder
        Box(
            modifier = Modifier.fillMaxWidth(),
            contentAlignment = Alignment.Center
        ) {
            Surface(
                modifier = Modifier.size(100.dp),
                shape = CircleShape,
                color = SettingsColors.bgCard,
                border = BorderStroke(2.dp, SettingsColors.gold.copy(alpha = 0.5f))
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        Icons.Default.CameraAlt,
                        contentDescription = "Add photo",
                        tint = SettingsColors.gold,
                        modifier = Modifier.size(32.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            "Add Profile Photo",
            fontSize = 13.sp,
            color = SettingsColors.gold,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(32.dp))

        // Display name
        Text(
            "Artist Name *",
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold,
            color = Color.White
        )
        Spacer(modifier = Modifier.height(8.dp))
        OutlinedTextField(
            value = displayName,
            onValueChange = onDisplayNameChange,
            modifier = Modifier.fillMaxWidth(),
            placeholder = { Text("Your artist name", color = SettingsColors.textTertiary) },
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = SettingsColors.gold,
                unfocusedBorderColor = SettingsColors.bgGlass,
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White,
                cursorColor = SettingsColors.gold
            ),
            shape = RoundedCornerShape(12.dp),
            singleLine = true
        )

        Spacer(modifier = Modifier.height(20.dp))

        // Handle
        Text(
            "Handle *",
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold,
            color = Color.White
        )
        Spacer(modifier = Modifier.height(8.dp))
        OutlinedTextField(
            value = handle,
            onValueChange = onHandleChange,
            modifier = Modifier.fillMaxWidth(),
            placeholder = { Text("yourhandle", color = SettingsColors.textTertiary) },
            prefix = {
                Text(
                    "@",
                    color = SettingsColors.gold,
                    fontWeight = FontWeight.Bold
                )
            },
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = SettingsColors.gold,
                unfocusedBorderColor = SettingsColors.bgGlass,
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White,
                cursorColor = SettingsColors.gold
            ),
            shape = RoundedCornerShape(12.dp),
            singleLine = true
        )
        if (handle.length >= 3) {
            Spacer(modifier = Modifier.height(4.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Default.CheckCircle,
                    contentDescription = null,
                    tint = SettingsColors.emerald,
                    modifier = Modifier.size(14.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    "Available",
                    fontSize = 12.sp,
                    color = SettingsColors.emerald
                )
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Bio
        Text(
            "Bio",
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold,
            color = Color.White
        )
        Spacer(modifier = Modifier.height(8.dp))
        OutlinedTextField(
            value = bio,
            onValueChange = { if (it.length <= 200) onBioChange(it) },
            modifier = Modifier
                .fillMaxWidth()
                .height(120.dp),
            placeholder = { Text("Tell us about yourself...", color = SettingsColors.textTertiary) },
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = SettingsColors.gold,
                unfocusedBorderColor = SettingsColors.bgGlass,
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White,
                cursorColor = SettingsColors.gold
            ),
            shape = RoundedCornerShape(12.dp)
        )
        Text(
            "${bio.length}/200",
            fontSize = 11.sp,
            color = SettingsColors.textTertiary,
            modifier = Modifier.fillMaxWidth(),
            textAlign = TextAlign.End
        )
    }
}

@Composable
private fun GenreSelectionStep(
    selectedGenres: List<String>,
    onGenresChange: (List<String>) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp)
    ) {
        Text(
            "What's your sound?",
            fontSize = 24.sp,
            fontWeight = FontWeight.Black,
            color = Color.White
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            "Select up to 5 genres that best describe your music",
            fontSize = 14.sp,
            color = SettingsColors.textSecondary
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            "${selectedGenres.size}/5 selected",
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
            color = SettingsColors.gold
        )

        Spacer(modifier = Modifier.height(24.dp))

        // Genre grid
        val genres = ArenaGenres.all

        val rows = genres.chunked(3)
        rows.forEach { rowGenres ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                rowGenres.forEach { genre ->
                    val isSelected = genre in selectedGenres
                    Surface(
                        onClick = {
                            onGenresChange(
                                if (isSelected) {
                                    selectedGenres - genre
                                } else if (selectedGenres.size < 5) {
                                    selectedGenres + genre
                                } else {
                                    selectedGenres
                                }
                            )
                        },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        color = if (isSelected) SettingsColors.gold.copy(alpha = 0.2f)
                        else SettingsColors.bgCard,
                        border = BorderStroke(
                            1.dp,
                            if (isSelected) SettingsColors.gold else SettingsColors.bgGlass
                        )
                    ) {
                        Text(
                            genre,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 12.dp),
                            fontSize = 12.sp,
                            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                            color = if (isSelected) SettingsColors.gold else SettingsColors.textSecondary,
                            textAlign = TextAlign.Center,
                            maxLines = 1
                        )
                    }
                }

                // Fill remaining space if row is incomplete
                repeat(3 - rowGenres.size) {
                    Spacer(modifier = Modifier.weight(1f))
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
        }
    }
}

@Composable
private fun FinishStep(
    displayName: String,
    role: ArtistRole?,
    genres: List<String>,
    isSubmitting: Boolean
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        if (isSubmitting) {
            CircularProgressIndicator(
                color = SettingsColors.gold,
                modifier = Modifier.size(48.dp)
            )
            Spacer(modifier = Modifier.height(24.dp))
            Text(
                "Creating your artist profile...",
                fontSize = 16.sp,
                color = Color.White
            )
        } else {
            // Success preview
            Surface(
                modifier = Modifier.size(100.dp),
                shape = CircleShape,
                color = SettingsColors.gold.copy(alpha = 0.2f),
                border = BorderStroke(3.dp, SettingsColors.gold)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        displayName.take(2).uppercase(),
                        fontSize = 32.sp,
                        fontWeight = FontWeight.Black,
                        color = SettingsColors.gold
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                "Welcome, $displayName!",
                fontSize = 24.sp,
                fontWeight = FontWeight.Black,
                color = Color.White
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                when (role) {
                    ArtistRole.INDEPENDENT_ARTIST -> "Independent Artist"
                    ArtistRole.PRODUCER -> "Producer"
                    ArtistRole.DJ -> "DJ"
                    ArtistRole.PODCASTER -> "Podcaster"
                    ArtistRole.BAND -> "Band / Group"
                    ArtistRole.LABEL -> "Label / Collective"
                    else -> "Artist"
                },
                fontSize = 14.sp,
                color = SettingsColors.gold
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Genre tags
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(genres) { genre ->
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = SettingsColors.bgCard
                    ) {
                        Text(
                            genre,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                            fontSize = 12.sp,
                            color = SettingsColors.textSecondary
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(40.dp))

            Text(
                "You're all set to start sharing your music!",
                fontSize = 14.sp,
                color = SettingsColors.textSecondary,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Features available
            val features = listOf(
                "Upload unlimited tracks",
                "Track your analytics",
                "Connect with fans",
                "Join remix challenges"
            )

            features.forEach { feature ->
                Row(
                    modifier = Modifier.padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.CheckCircle,
                        contentDescription = null,
                        tint = SettingsColors.emerald,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        feature,
                        fontSize = 14.sp,
                        color = Color.White
                    )
                }
            }
        }
    }
}

@Composable
private fun NavigationButtons(
    currentStep: Int,
    totalSteps: Int,
    canProceed: Boolean,
    isSubmitting: Boolean,
    onBack: () -> Unit,
    onNext: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(24.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        if (currentStep > 0) {
            OutlinedButton(
                onClick = onBack,
                modifier = Modifier.weight(1f),
                enabled = !isSubmitting,
                border = BorderStroke(1.dp, SettingsColors.textTertiary),
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = Color.White
                )
            ) {
                Text("Back")
            }
        }

        Button(
            onClick = onNext,
            modifier = Modifier.weight(1f),
            enabled = canProceed && !isSubmitting,
            colors = ButtonDefaults.buttonColors(
                containerColor = SettingsColors.gold,
                contentColor = Color.Black,
                disabledContainerColor = SettingsColors.bgCard,
                disabledContentColor = SettingsColors.textTertiary
            )
        ) {
            Text(
                if (currentStep == totalSteps - 1) "Create Profile" else "Continue",
                fontWeight = FontWeight.Bold
            )
        }
    }
}

// ============================================
// ⚙️ ARTIST SETTINGS SCREEN
// ============================================

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ArtistSettingsScreen(
    onBack: () -> Unit,
    onEditProfile: () -> Unit = {},
    onManageRights: () -> Unit = {},
    onPrivacy: () -> Unit = {},
    onNotifications: () -> Unit = {},
    onLogout: () -> Unit = {}
) {
    BackHandler { onBack() }

    val repository = remember { ArenaRepository.getInstance() }
    val currentArtist by repository.currentArtistProfile.collectAsState()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(SettingsColors.bgDark)
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
                    Text(
                        "Settings",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
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
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent
                )
            )

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp)
            ) {
                // Profile section
                currentArtist?.let { artist ->
                    ProfileSection(
                        artist = artist,
                        onEditProfile = onEditProfile
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Settings sections
                SettingsSection(
                    title = "Account",
                    items = listOf(
                        SettingsItem(Icons.Outlined.Person, "Edit Profile", onEditProfile),
                        SettingsItem(Icons.Outlined.Shield, "Rights & Permissions", onManageRights),
                        SettingsItem(Icons.Outlined.Verified, "Verification", {}),
                        SettingsItem(Icons.Outlined.Link, "Connected Accounts", {})
                    )
                )

                Spacer(modifier = Modifier.height(16.dp))

                SettingsSection(
                    title = "Preferences",
                    items = listOf(
                        SettingsItem(Icons.Outlined.Notifications, "Notifications", onNotifications),
                        SettingsItem(Icons.Outlined.Lock, "Privacy", onPrivacy),
                        SettingsItem(Icons.Outlined.Language, "Language", {}),
                        SettingsItem(Icons.Outlined.Palette, "Appearance", {})
                    )
                )

                Spacer(modifier = Modifier.height(16.dp))

                SettingsSection(
                    title = "Creator Tools",
                    items = listOf(
                        SettingsItem(Icons.Outlined.Analytics, "Analytics Settings", {}),
                        SettingsItem(Icons.Outlined.AttachMoney, "Monetization", {}),
                        SettingsItem(Icons.Outlined.Campaign, "Promotions", {}),
                        SettingsItem(Icons.Outlined.MusicNote, "Audio Quality", {})
                    )
                )

                Spacer(modifier = Modifier.height(16.dp))

                SettingsSection(
                    title = "Support",
                    items = listOf(
                        SettingsItem(Icons.Outlined.HelpOutline, "Help Center", {}),
                        SettingsItem(Icons.Outlined.Feedback, "Send Feedback", {}),
                        SettingsItem(Icons.Outlined.Info, "About Arena", {}),
                        SettingsItem(Icons.Outlined.Policy, "Terms & Policies", {})
                    )
                )

                Spacer(modifier = Modifier.height(24.dp))

                // Logout button
                OutlinedButton(
                    onClick = onLogout,
                    modifier = Modifier.fillMaxWidth(),
                    border = BorderStroke(1.dp, SettingsColors.crimson.copy(alpha = 0.5f)),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = SettingsColors.crimson
                    )
                ) {
                    Icon(
                        Icons.Default.Logout,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Sign Out")
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    "Artists Arena v1.0.0",
                    fontSize = 12.sp,
                    color = SettingsColors.textTertiary,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(80.dp))
            }
        }
    }
}

@Composable
private fun ProfileSection(
    artist: ArtistProfile,
    onEditProfile: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = SettingsColors.bgCard
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Avatar
            Surface(
                modifier = Modifier.size(60.dp),
                shape = CircleShape,
                color = SettingsColors.bgElevated,
                border = BorderStroke(2.dp, SettingsColors.gold.copy(alpha = 0.5f))
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        artist.displayName.take(2).uppercase(),
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = SettingsColors.gold
                    )
                }
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        artist.displayName,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    if (artist.verificationStatus in listOf(VerificationStatus.VERIFIED, VerificationStatus.OFFICIAL)) {
                        Spacer(modifier = Modifier.width(6.dp))
                        Icon(
                            Icons.Default.Verified,
                            contentDescription = "Verified",
                            tint = SettingsColors.gold,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
                Text(
                    "@${artist.handle}",
                    fontSize = 14.sp,
                    color = SettingsColors.textSecondary
                )
            }

            IconButton(onClick = onEditProfile) {
                Icon(
                    Icons.Default.Edit,
                    contentDescription = "Edit",
                    tint = SettingsColors.gold
                )
            }
        }
    }
}

data class SettingsItem(
    val icon: ImageVector,
    val title: String,
    val onClick: () -> Unit
)

@Composable
private fun SettingsSection(
    title: String,
    items: List<SettingsItem>
) {
    Column {
        Text(
            title,
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
            color = SettingsColors.textTertiary,
            modifier = Modifier.padding(start = 4.dp, bottom = 8.dp)
        )

        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            color = SettingsColors.bgCard
        ) {
            Column {
                items.forEachIndexed { index, item ->
                    Surface(
                        onClick = item.onClick,
                        color = Color.Transparent
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                item.icon,
                                contentDescription = null,
                                tint = SettingsColors.textSecondary,
                                modifier = Modifier.size(22.dp)
                            )

                            Spacer(modifier = Modifier.width(16.dp))

                            Text(
                                item.title,
                                fontSize = 15.sp,
                                color = Color.White,
                                modifier = Modifier.weight(1f)
                            )

                            Icon(
                                Icons.Default.KeyboardArrowRight,
                                contentDescription = null,
                                tint = SettingsColors.textTertiary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }

                    if (index < items.size - 1) {
                        Divider(
                            color = SettingsColors.bgGlass,
                            modifier = Modifier.padding(start = 54.dp)
                        )
                    }
                }
            }
        }
    }
}

