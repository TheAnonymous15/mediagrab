package com.example.dwn.arena

import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

// ============================================
// UPLOAD COLORS
// ============================================

private object UploadColors {
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
// 📤 TRACK UPLOAD SCREEN
// ============================================

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TrackUploadScreen(
    onBack: () -> Unit,
    onUploadComplete: (Track) -> Unit = {}
) {
    BackHandler { onBack() }

    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val haptic = LocalHapticFeedback.current
    val repository = remember { ArenaRepository.getInstance() }

    // Upload state
    var currentStep by remember { mutableIntStateOf(0) }
    val steps = listOf("Select File", "Track Info", "Details", "Publish")

    // File selection
    var selectedFileUri by remember { mutableStateOf<Uri?>(null) }
    var selectedFileName by remember { mutableStateOf("") }
    var fileSize by remember { mutableLongStateOf(0L) }

    // Track info
    var title by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var selectedGenres by remember { mutableStateOf<List<String>>(emptyList()) }
    var selectedMoods by remember { mutableStateOf<List<String>>(emptyList()) }
    var bpm by remember { mutableStateOf("") }
    var musicalKey by remember { mutableStateOf("") }
    var isExplicit by remember { mutableStateOf(false) }
    var remixPermission by remember { mutableStateOf(RemixPermission.NO_REMIX) }
    var publishStatus by remember { mutableStateOf(TrackStatus.PUBLIC) }

    // Upload progress
    var isUploading by remember { mutableStateOf(false) }
    var uploadProgress by remember { mutableFloatStateOf(0f) }
    var uploadComplete by remember { mutableStateOf(false) }

    // File picker
    val filePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let {
            selectedFileUri = it
            // Get file name and size
            context.contentResolver.query(it, null, null, null, null)?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val nameIndex = cursor.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
                    val sizeIndex = cursor.getColumnIndex(android.provider.OpenableColumns.SIZE)
                    if (nameIndex >= 0) selectedFileName = cursor.getString(nameIndex)
                    if (sizeIndex >= 0) fileSize = cursor.getLong(sizeIndex)
                }
            }
            currentStep = 1
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(UploadColors.bgDark)
    ) {
        // Background
        UploadBackground()

        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
        ) {
            // Top Bar
            UploadTopBar(
                onBack = onBack,
                currentStep = currentStep,
                totalSteps = steps.size
            )

            // Progress indicator
            StepIndicator(
                steps = steps,
                currentStep = currentStep
            )

            // Content
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .weight(1f)
            ) {
                when {
                    isUploading -> {
                        UploadProgressContent(
                            progress = uploadProgress,
                            fileName = selectedFileName,
                            isComplete = uploadComplete
                        )
                    }
                    currentStep == 0 -> {
                        FileSelectionStep(
                            onSelectFile = { filePicker.launch("audio/*") }
                        )
                    }
                    currentStep == 1 -> {
                        TrackInfoStep(
                            title = title,
                            onTitleChange = { title = it },
                            description = description,
                            onDescriptionChange = { description = it },
                            selectedGenres = selectedGenres,
                            onGenresChange = { selectedGenres = it },
                            fileName = selectedFileName,
                            fileSize = fileSize
                        )
                    }
                    currentStep == 2 -> {
                        DetailsStep(
                            selectedMoods = selectedMoods,
                            onMoodsChange = { selectedMoods = it },
                            bpm = bpm,
                            onBpmChange = { bpm = it },
                            musicalKey = musicalKey,
                            onKeyChange = { musicalKey = it },
                            isExplicit = isExplicit,
                            onExplicitChange = { isExplicit = it },
                            remixPermission = remixPermission,
                            onRemixPermissionChange = { remixPermission = it }
                        )
                    }
                    currentStep == 3 -> {
                        PublishStep(
                            publishStatus = publishStatus,
                            onStatusChange = { publishStatus = it },
                            title = title,
                            genres = selectedGenres,
                            onPublish = {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                isUploading = true
                                scope.launch {
                                    // Simulate upload
                                    for (i in 1..100) {
                                        delay(30)
                                        uploadProgress = i / 100f
                                    }
                                    uploadComplete = true
                                    delay(1000)
                                    onBack()
                                }
                            }
                        )
                    }
                }
            }

            // Navigation buttons
            if (!isUploading) {
                NavigationButtons(
                    currentStep = currentStep,
                    totalSteps = steps.size,
                    canProceed = when (currentStep) {
                        0 -> selectedFileUri != null
                        1 -> title.isNotBlank() && selectedGenres.isNotEmpty()
                        else -> true
                    },
                    onBack = { if (currentStep > 0) currentStep-- },
                    onNext = { if (currentStep < steps.size - 1) currentStep++ }
                )
            }
        }
    }
}

@Composable
private fun UploadBackground() {
    val transition = rememberInfiniteTransition(label = "upload_bg")

    val pulse by transition.animateFloat(
        initialValue = 0.5f,
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

        // Golden orb
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(
                    UploadColors.gold.copy(alpha = pulse * 0.1f),
                    Color.Transparent
                ),
                radius = w * 0.4f
            ),
            radius = w * 0.35f,
            center = androidx.compose.ui.geometry.Offset(w * 0.8f, h * 0.2f)
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun UploadTopBar(
    onBack: () -> Unit,
    currentStep: Int,
    totalSteps: Int
) {
    TopAppBar(
        title = {
            Column {
                Text(
                    "Upload Track",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Text(
                    "Step ${currentStep + 1} of $totalSteps",
                    fontSize = 12.sp,
                    color = UploadColors.textSecondary
                )
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
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = Color.Transparent
        )
    )
}

@Composable
private fun StepIndicator(
    steps: List<String>,
    currentStep: Int
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 16.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        steps.forEachIndexed { index, step ->
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Step circle
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .background(
                            when {
                                index < currentStep -> UploadColors.emerald
                                index == currentStep -> UploadColors.gold
                                else -> UploadColors.bgCard
                            },
                            CircleShape
                        )
                        .border(
                            1.dp,
                            when {
                                index <= currentStep -> Color.Transparent
                                else -> UploadColors.textTertiary.copy(alpha = 0.3f)
                            },
                            CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    if (index < currentStep) {
                        Icon(
                            Icons.Default.Check,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(16.dp)
                        )
                    } else {
                        Text(
                            "${index + 1}",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (index == currentStep) Color.Black else UploadColors.textTertiary
                        )
                    }
                }

                // Connector line (except for last step)
                if (index < steps.size - 1) {
                    Box(
                        modifier = Modifier
                            .width(20.dp)
                            .height(2.dp)
                            .background(
                                if (index < currentStep) UploadColors.emerald
                                else UploadColors.bgCard
                            )
                    )
                }
            }
        }
    }
}

@Composable
private fun FileSelectionStep(
    onSelectFile: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // Upload icon with animation
        val transition = rememberInfiniteTransition(label = "upload_icon")
        val bounce by transition.animateFloat(
            initialValue = 0f,
            targetValue = 10f,
            animationSpec = infiniteRepeatable(
                animation = tween(1000),
                repeatMode = RepeatMode.Reverse
            ),
            label = "bounce"
        )

        Surface(
            onClick = onSelectFile,
            modifier = Modifier
                .size(180.dp)
                .offset(y = (-bounce).dp),
            shape = RoundedCornerShape(24.dp),
            color = UploadColors.bgCard,
            border = BorderStroke(
                2.dp,
                Brush.sweepGradient(
                    listOf(
                        UploadColors.gold,
                        UploadColors.amber,
                        UploadColors.gold
                    )
                )
            )
        ) {
            Column(
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Icon(
                    Icons.Outlined.CloudUpload,
                    contentDescription = null,
                    tint = UploadColors.gold,
                    modifier = Modifier.size(64.dp)
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    "Select Audio File",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.White
                )
            }
        }

        Spacer(modifier = Modifier.height(32.dp))

        Text(
            "Supported formats",
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold,
            color = Color.White
        )

        Spacer(modifier = Modifier.height(12.dp))

        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            listOf("WAV", "FLAC", "MP3", "AAC").forEach { format ->
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = UploadColors.bgCard
                ) {
                    Text(
                        format,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        fontSize = 13.sp,
                        color = UploadColors.gold
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            "Maximum file size: 100MB",
            fontSize = 12.sp,
            color = UploadColors.textTertiary
        )
    }
}

@Composable
private fun TrackInfoStep(
    title: String,
    onTitleChange: (String) -> Unit,
    description: String,
    onDescriptionChange: (String) -> Unit,
    selectedGenres: List<String>,
    onGenresChange: (List<String>) -> Unit,
    fileName: String,
    fileSize: Long
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(20.dp)
    ) {
        // Selected file info
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            color = UploadColors.emerald.copy(alpha = 0.1f),
            border = BorderStroke(1.dp, UploadColors.emerald.copy(alpha = 0.3f))
        ) {
            Row(
                modifier = Modifier.padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Default.AudioFile,
                    contentDescription = null,
                    tint = UploadColors.emerald,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        fileName,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color.White
                    )
                    Text(
                        formatFileSize(fileSize),
                        fontSize = 12.sp,
                        color = UploadColors.textSecondary
                    )
                }
                Icon(
                    Icons.Default.CheckCircle,
                    contentDescription = null,
                    tint = UploadColors.emerald,
                    modifier = Modifier.size(20.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Title
        Text(
            "Track Title *",
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold,
            color = Color.White
        )
        Spacer(modifier = Modifier.height(8.dp))
        OutlinedTextField(
            value = title,
            onValueChange = onTitleChange,
            modifier = Modifier.fillMaxWidth(),
            placeholder = { Text("Enter track title", color = UploadColors.textTertiary) },
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = UploadColors.gold,
                unfocusedBorderColor = UploadColors.bgGlass,
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White,
                cursorColor = UploadColors.gold
            ),
            shape = RoundedCornerShape(12.dp),
            singleLine = true
        )

        Spacer(modifier = Modifier.height(20.dp))

        // Description
        Text(
            "Description",
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold,
            color = Color.White
        )
        Spacer(modifier = Modifier.height(8.dp))
        OutlinedTextField(
            value = description,
            onValueChange = onDescriptionChange,
            modifier = Modifier
                .fillMaxWidth()
                .height(120.dp),
            placeholder = { Text("Tell listeners about this track...", color = UploadColors.textTertiary) },
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = UploadColors.gold,
                unfocusedBorderColor = UploadColors.bgGlass,
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White,
                cursorColor = UploadColors.gold
            ),
            shape = RoundedCornerShape(12.dp)
        )

        Spacer(modifier = Modifier.height(20.dp))

        // Genres
        Text(
            "Genres * (select up to 3)",
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold,
            color = Color.White
        )
        Spacer(modifier = Modifier.height(12.dp))

        GenreSelector(
            genres = ArenaGenres.all,
            selectedGenres = selectedGenres,
            onGenreToggle = { genre ->
                onGenresChange(
                    if (genre in selectedGenres) {
                        selectedGenres - genre
                    } else if (selectedGenres.size < 3) {
                        selectedGenres + genre
                    } else {
                        selectedGenres
                    }
                )
            }
        )
    }
}

@Composable
private fun GenreSelector(
    genres: List<String>,
    selectedGenres: List<String>,
    onGenreToggle: (String) -> Unit
) {
    val rows = genres.chunked(4)

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        rows.forEach { rowGenres ->
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(rowGenres) { genre ->
                    val isSelected = genre in selectedGenres
                    Surface(
                        onClick = { onGenreToggle(genre) },
                        shape = RoundedCornerShape(20.dp),
                        color = if (isSelected) UploadColors.gold.copy(alpha = 0.2f) else UploadColors.bgCard,
                        border = BorderStroke(
                            1.dp,
                            if (isSelected) UploadColors.gold else UploadColors.bgGlass
                        )
                    ) {
                        Text(
                            genre,
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                            fontSize = 13.sp,
                            color = if (isSelected) UploadColors.gold else UploadColors.textSecondary
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun DetailsStep(
    selectedMoods: List<String>,
    onMoodsChange: (List<String>) -> Unit,
    bpm: String,
    onBpmChange: (String) -> Unit,
    musicalKey: String,
    onKeyChange: (String) -> Unit,
    isExplicit: Boolean,
    onExplicitChange: (Boolean) -> Unit,
    remixPermission: RemixPermission,
    onRemixPermissionChange: (RemixPermission) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(20.dp)
    ) {
        // Moods
        Text(
            "Moods & Vibes",
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold,
            color = Color.White
        )
        Spacer(modifier = Modifier.height(12.dp))

        val moodRows = ArenaMoods.all.chunked(4)
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            moodRows.forEach { rowMoods ->
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(rowMoods) { mood ->
                        val isSelected = mood in selectedMoods
                        Surface(
                            onClick = {
                                onMoodsChange(
                                    if (isSelected) selectedMoods - mood
                                    else if (selectedMoods.size < 5) selectedMoods + mood
                                    else selectedMoods
                                )
                            },
                            shape = RoundedCornerShape(20.dp),
                            color = if (isSelected) UploadColors.rose.copy(alpha = 0.2f) else UploadColors.bgCard,
                            border = BorderStroke(1.dp, if (isSelected) UploadColors.rose else UploadColors.bgGlass)
                        ) {
                            Text(
                                mood,
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                                fontSize = 13.sp,
                                color = if (isSelected) UploadColors.rose else UploadColors.textSecondary
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // BPM and Key row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text("BPM", fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = Color.White)
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = bpm,
                    onValueChange = { if (it.all { c -> c.isDigit() } && it.length <= 3) onBpmChange(it) },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("120", color = UploadColors.textTertiary) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = UploadColors.gold,
                        unfocusedBorderColor = UploadColors.bgGlass,
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        cursorColor = UploadColors.gold
                    ),
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true
                )
            }

            Column(modifier = Modifier.weight(1f)) {
                Text("Key", fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = Color.White)
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = musicalKey,
                    onValueChange = { if (it.length <= 4) onKeyChange(it) },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("Am", color = UploadColors.textTertiary) },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = UploadColors.gold,
                        unfocusedBorderColor = UploadColors.bgGlass,
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        cursorColor = UploadColors.gold
                    ),
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Explicit content toggle
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            color = UploadColors.bgCard
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        "Explicit Content",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color.White
                    )
                    Text(
                        "Contains mature language or themes",
                        fontSize = 12.sp,
                        color = UploadColors.textTertiary
                    )
                }
                Switch(
                    checked = isExplicit,
                    onCheckedChange = onExplicitChange,
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Color.White,
                        checkedTrackColor = UploadColors.gold,
                        uncheckedThumbColor = UploadColors.textTertiary,
                        uncheckedTrackColor = UploadColors.bgElevated
                    )
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Remix permissions
        Text(
            "Remix Permissions",
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold,
            color = Color.White
        )
        Spacer(modifier = Modifier.height(12.dp))

        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            RemixPermission.entries.forEach { permission ->
                Surface(
                    onClick = { onRemixPermissionChange(permission) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    color = if (permission == remixPermission) UploadColors.violet.copy(alpha = 0.15f) else UploadColors.bgCard,
                    border = BorderStroke(
                        1.dp,
                        if (permission == remixPermission) UploadColors.violet else UploadColors.bgGlass
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = permission == remixPermission,
                            onClick = { onRemixPermissionChange(permission) },
                            colors = RadioButtonDefaults.colors(
                                selectedColor = UploadColors.violet,
                                unselectedColor = UploadColors.textTertiary
                            )
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(
                                when (permission) {
                                    RemixPermission.NO_REMIX -> "No Remix"
                                    RemixPermission.PARTIAL_REMIX -> "Partial Remix (Stems only)"
                                    RemixPermission.FULL_REMIX -> "Full Remix Allowed"
                                    RemixPermission.CREATIVE_COMMONS -> "Creative Commons"
                                },
                                fontSize = 14.sp,
                                color = Color.White
                            )
                            Text(
                                when (permission) {
                                    RemixPermission.NO_REMIX -> "Others cannot remix this track"
                                    RemixPermission.PARTIAL_REMIX -> "Allow remixes using stems"
                                    RemixPermission.FULL_REMIX -> "Anyone can create remixes"
                                    RemixPermission.CREATIVE_COMMONS -> "Free to use with attribution"
                                },
                                fontSize = 11.sp,
                                color = UploadColors.textTertiary
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PublishStep(
    publishStatus: TrackStatus,
    onStatusChange: (TrackStatus) -> Unit,
    title: String,
    genres: List<String>,
    onPublish: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Preview card
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            color = UploadColors.bgCard,
            border = BorderStroke(1.dp, UploadColors.gold.copy(alpha = 0.3f))
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Cover placeholder
                Surface(
                    modifier = Modifier.size(120.dp),
                    shape = RoundedCornerShape(16.dp),
                    color = UploadColors.bgElevated
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            Icons.Default.MusicNote,
                            contentDescription = null,
                            tint = UploadColors.gold.copy(alpha = 0.5f),
                            modifier = Modifier.size(48.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    title.ifEmpty { "Untitled Track" },
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(8.dp))

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    genres.take(3).forEach { genre ->
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = UploadColors.gold.copy(alpha = 0.15f)
                        ) {
                            Text(
                                genre,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                fontSize = 11.sp,
                                color = UploadColors.gold
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(32.dp))

        // Publish options
        Text(
            "Visibility",
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold,
            color = Color.White
        )

        Spacer(modifier = Modifier.height(12.dp))

        listOf(
            TrackStatus.PUBLIC to "Public" to "Everyone can listen and discover",
            TrackStatus.PRIVATE to "Private" to "Only you can access",
            TrackStatus.SCHEDULED to "Scheduled" to "Release at a specific time"
        ).forEach { (statusPair, description) ->
            val (status, label) = statusPair
            Surface(
                onClick = { onStatusChange(status) },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                color = if (status == publishStatus) UploadColors.gold.copy(alpha = 0.15f) else UploadColors.bgCard,
                border = BorderStroke(1.dp, if (status == publishStatus) UploadColors.gold else UploadColors.bgGlass)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RadioButton(
                        selected = status == publishStatus,
                        onClick = { onStatusChange(status) },
                        colors = RadioButtonDefaults.colors(
                            selectedColor = UploadColors.gold,
                            unselectedColor = UploadColors.textTertiary
                        )
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(label, fontSize = 14.sp, color = Color.White)
                        Text(description, fontSize = 11.sp, color = UploadColors.textTertiary)
                    }
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
        }

        Spacer(modifier = Modifier.height(32.dp))

        // Publish button
        Button(
            onClick = onPublish,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = UploadColors.gold,
                contentColor = Color.Black
            ),
            shape = RoundedCornerShape(16.dp)
        ) {
            Icon(Icons.Default.Publish, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                "Publish Track",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
private fun UploadProgressContent(
    progress: Float,
    fileName: String,
    isComplete: Boolean
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(40.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // Animated progress ring
        Box(
            modifier = Modifier.size(160.dp),
            contentAlignment = Alignment.Center
        ) {
            val animatedProgress by animateFloatAsState(
                targetValue = progress,
                animationSpec = tween(300),
                label = "progress"
            )

            Canvas(modifier = Modifier.size(160.dp)) {
                // Background ring
                drawArc(
                    color = UploadColors.bgCard,
                    startAngle = -90f,
                    sweepAngle = 360f,
                    useCenter = false,
                    style = Stroke(width = 12f)
                )

                // Progress ring
                drawArc(
                    brush = Brush.sweepGradient(
                        listOf(UploadColors.gold, UploadColors.amber, UploadColors.gold)
                    ),
                    startAngle = -90f,
                    sweepAngle = animatedProgress * 360f,
                    useCenter = false,
                    style = Stroke(width = 12f, cap = androidx.compose.ui.graphics.StrokeCap.Round)
                )
            }

            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                if (isComplete) {
                    Icon(
                        Icons.Default.CheckCircle,
                        contentDescription = null,
                        tint = UploadColors.emerald,
                        modifier = Modifier.size(48.dp)
                    )
                } else {
                    Text(
                        "${(progress * 100).toInt()}%",
                        fontSize = 32.sp,
                        fontWeight = FontWeight.Bold,
                        color = UploadColors.gold
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(32.dp))

        Text(
            if (isComplete) "Upload Complete!" else "Uploading...",
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            color = if (isComplete) UploadColors.emerald else Color.White
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            fileName,
            fontSize = 14.sp,
            color = UploadColors.textSecondary,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun NavigationButtons(
    currentStep: Int,
    totalSteps: Int,
    canProceed: Boolean,
    onBack: () -> Unit,
    onNext: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(20.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        if (currentStep > 0) {
            OutlinedButton(
                onClick = onBack,
                modifier = Modifier.weight(1f),
                border = BorderStroke(1.dp, UploadColors.textTertiary),
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = Color.White
                )
            ) {
                Text("Back")
            }
        }

        if (currentStep < totalSteps - 1) {
            Button(
                onClick = onNext,
                modifier = Modifier.weight(1f),
                enabled = canProceed,
                colors = ButtonDefaults.buttonColors(
                    containerColor = UploadColors.gold,
                    contentColor = Color.Black,
                    disabledContainerColor = UploadColors.bgCard,
                    disabledContentColor = UploadColors.textTertiary
                )
            ) {
                Text("Continue")
            }
        }
    }
}

private fun formatFileSize(bytes: Long): String {
    return when {
        bytes >= 1_048_576 -> String.format("%.1f MB", bytes / 1_048_576.0)
        bytes >= 1024 -> String.format("%.1f KB", bytes / 1024.0)
        else -> "$bytes B"
    }
}

