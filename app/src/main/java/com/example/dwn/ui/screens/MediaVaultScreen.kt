package com.example.dwn.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.lazy.grid.*
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
import com.example.dwn.vault.*

// ============================================
// 🎨 VAULT COLORS
// ============================================

private object VaultColors {
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
}

// ============================================
// 🗄️ MEDIA VAULT SCREEN
// ============================================

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MediaVaultScreen(
    onBack: () -> Unit
) {
    var selectedTab by remember { mutableIntStateOf(0) }
    var viewMode by remember { mutableStateOf(VaultViewMode.GRID) }
    var showSearch by remember { mutableStateOf(false) }
    var searchText by remember { mutableStateOf("") }
    var showFilterSheet by remember { mutableStateOf(false) }
    var selectedCollection by remember { mutableStateOf<String?>(null) }
    var showItemDetails by remember { mutableStateOf<VaultMediaItem?>(null) }
    var showNewCollectionDialog by remember { mutableStateOf(false) }

    // Mock data
    val collections = remember { defaultSmartCollections }
    val mockItems = remember { createMockVaultItems() }

    val filteredItems = remember(selectedTab, selectedCollection, searchText) {
        var items = mockItems

        // Filter by tab
        items = when (selectedTab) {
            1 -> items.filter { it.mediaType == VaultMediaType.AUDIO || it.mediaType == VaultMediaType.PODCAST }
            2 -> items.filter { it.mediaType == VaultMediaType.VIDEO }
            3 -> items.filter { it.mediaType == VaultMediaType.CLIP || it.mediaType == VaultMediaType.REMIX }
            else -> items
        }

        // Filter by collection
        if (selectedCollection != null) {
            val collection = collections.find { it.id == selectedCollection }
            if (collection?.isSmartCollection == true) {
                items = when (selectedCollection) {
                    "favorites" -> items.filter { it.isFavorite }
                    "recent" -> items.sortedByDescending { it.addedAt }.take(10)
                    "podcasts" -> items.filter { it.mediaType == VaultMediaType.PODCAST }
                    "downloads" -> items.filter { it.source == MediaSource.DOWNLOAD }
                    "videos" -> items.filter { it.mediaType == VaultMediaType.VIDEO }
                    "clips" -> items.filter { it.mediaType == VaultMediaType.CLIP }
                    else -> items
                }
            }
        }

        // Filter by search
        if (searchText.isNotEmpty()) {
            items = items.filter {
                it.displayName.contains(searchText, ignoreCase = true) ||
                it.tags.any { tag -> tag.contains(searchText, ignoreCase = true) }
            }
        }

        items
    }

    // Statistics
    val stats = remember(mockItems) {
        VaultStatistics(
            totalItems = mockItems.size,
            totalSize = mockItems.sumOf { it.metadata.fileSize },
            totalDuration = mockItems.sumOf { it.metadata.duration },
            itemsByType = mockItems.groupBy { it.mediaType }.mapValues { it.value.size },
            favoritesCount = mockItems.count { it.isFavorite }
        )
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(VaultColors.bgDark)
    ) {
        // Animated background
        VaultBackground()

        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
        ) {
            // Top Bar
            VaultTopBar(
                onBack = onBack,
                viewMode = viewMode,
                onViewModeChange = { viewMode = it },
                onSearchClick = { showSearch = !showSearch },
                onFilterClick = { showFilterSheet = true }
            )

            // Search bar (animated)
            AnimatedVisibility(
                visible = showSearch,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                SearchBar(
                    searchText = searchText,
                    onSearchTextChange = { searchText = it },
                    onClear = { searchText = "" }
                )
            }

            // Main content
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
            ) {
                // Stats header
                if (!showSearch && selectedCollection == null) {
                    StatsHeader(stats = stats)
                    Spacer(modifier = Modifier.height(16.dp))
                }

                // Tab selector
                TabSelector(
                    selectedTab = selectedTab,
                    onTabSelect = { selectedTab = it }
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Collections (horizontal scroll)
                if (selectedCollection == null) {
                    CollectionsRow(
                        collections = collections,
                        onCollectionClick = { selectedCollection = it },
                        onNewCollection = { showNewCollectionDialog = true }
                    )
                    Spacer(modifier = Modifier.height(20.dp))
                } else {
                    // Selected collection header
                    SelectedCollectionHeader(
                        collection = collections.find { it.id == selectedCollection },
                        onClose = { selectedCollection = null }
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                }

                // Media items
                when (viewMode) {
                    VaultViewMode.GRID -> MediaGrid(
                        items = filteredItems,
                        onItemClick = { showItemDetails = it },
                        onFavoriteToggle = { /* Toggle favorite */ }
                    )
                    VaultViewMode.LIST -> MediaList(
                        items = filteredItems,
                        onItemClick = { showItemDetails = it },
                        onFavoriteToggle = { /* Toggle favorite */ }
                    )
                    VaultViewMode.COMPACT -> MediaCompactList(
                        items = filteredItems,
                        onItemClick = { showItemDetails = it }
                    )
                }

                Spacer(modifier = Modifier.height(100.dp))
            }
        }

        // FAB
        FloatingActionButton(
            onClick = { /* Import media */ },
            containerColor = VaultColors.pink,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(20.dp)
                .navigationBarsPadding()
        ) {
            Icon(Icons.Default.Add, "Add Media", tint = Color.White)
        }
    }

    // Item details sheet
    if (showItemDetails != null) {
        ItemDetailsSheet(
            item = showItemDetails!!,
            onDismiss = { showItemDetails = null }
        )
    }

    // Filter sheet
    if (showFilterSheet) {
        FilterSheet(
            onDismiss = { showFilterSheet = false }
        )
    }

    // New collection dialog
    if (showNewCollectionDialog) {
        NewCollectionDialog(
            onDismiss = { showNewCollectionDialog = false },
            onCreate = { name ->
                showNewCollectionDialog = false
                // Create collection
            }
        )
    }
}

// ============================================
// 🌌 ANIMATED BACKGROUND
// ============================================

@Composable
private fun VaultBackground() {
    val transition = rememberInfiniteTransition(label = "bg")

    val pulse by transition.animateFloat(
        initialValue = 0.6f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(5000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse"
    )

    Canvas(modifier = Modifier.fillMaxSize()) {
        drawRect(
            brush = Brush.verticalGradient(
                listOf(
                    VaultColors.bgDark,
                    Color(0xFF0F0818),
                    VaultColors.bgMid,
                    VaultColors.bgDark
                )
            )
        )

        drawCircle(
            brush = Brush.radialGradient(
                listOf(
                    VaultColors.purple.copy(alpha = 0.08f * pulse),
                    Color.Transparent
                )
            ),
            radius = 400f,
            center = Offset(size.width * 0.8f, size.height * 0.15f)
        )

        drawCircle(
            brush = Brush.radialGradient(
                listOf(
                    VaultColors.pink.copy(alpha = 0.06f * pulse),
                    Color.Transparent
                )
            ),
            radius = 350f,
            center = Offset(size.width * 0.1f, size.height * 0.6f)
        )
    }
}

// ============================================
// 📱 TOP BAR
// ============================================

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun VaultTopBar(
    onBack: () -> Unit,
    viewMode: VaultViewMode,
    onViewModeChange: (VaultViewMode) -> Unit,
    onSearchClick: () -> Unit,
    onFilterClick: () -> Unit
) {
    TopAppBar(
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Default.Inventory2,
                    null,
                    tint = VaultColors.purple,
                    modifier = Modifier.size(26.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        "Media Vault",
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp
                    )
                    Text(
                        "Your media universe",
                        color = VaultColors.textTertiary,
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
            IconButton(onClick = onFilterClick) {
                Icon(Icons.Default.FilterList, "Filter")
            }
            IconButton(onClick = {
                onViewModeChange(
                    when (viewMode) {
                        VaultViewMode.GRID -> VaultViewMode.LIST
                        VaultViewMode.LIST -> VaultViewMode.COMPACT
                        VaultViewMode.COMPACT -> VaultViewMode.GRID
                    }
                )
            }) {
                Icon(
                    when (viewMode) {
                        VaultViewMode.GRID -> Icons.Default.GridView
                        VaultViewMode.LIST -> Icons.Default.ViewList
                        VaultViewMode.COMPACT -> Icons.Default.ViewHeadline
                    },
                    "View Mode"
                )
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = Color.Transparent,
            titleContentColor = VaultColors.textPrimary,
            navigationIconContentColor = VaultColors.textSecondary,
            actionIconContentColor = VaultColors.textSecondary
        )
    )
}

// ============================================
// 🔍 SEARCH BAR
// ============================================

@Composable
private fun SearchBar(
    searchText: String,
    onSearchTextChange: (String) -> Unit,
    onClear: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        shape = RoundedCornerShape(16.dp),
        color = VaultColors.surfaceVariant
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Icons.Default.Search,
                null,
                tint = VaultColors.textTertiary,
                modifier = Modifier.size(20.dp)
            )

            Spacer(modifier = Modifier.width(12.dp))

            TextField(
                value = searchText,
                onValueChange = onSearchTextChange,
                placeholder = { Text("Search vault...", color = VaultColors.textTertiary) },
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = Color.Transparent,
                    unfocusedContainerColor = Color.Transparent,
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent,
                    focusedTextColor = VaultColors.textPrimary,
                    unfocusedTextColor = VaultColors.textPrimary
                ),
                modifier = Modifier.weight(1f),
                singleLine = true
            )

            if (searchText.isNotEmpty()) {
                IconButton(onClick = onClear, modifier = Modifier.size(32.dp)) {
                    Icon(
                        Icons.Default.Close,
                        "Clear",
                        tint = VaultColors.textSecondary,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}

// ============================================
// 📊 STATS HEADER
// ============================================

@Composable
private fun StatsHeader(stats: VaultStatistics) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        shape = RoundedCornerShape(20.dp),
        color = Color.Transparent
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.linearGradient(
                        listOf(
                            VaultColors.purple.copy(alpha = 0.8f),
                            VaultColors.pink.copy(alpha = 0.8f)
                        )
                    ),
                    RoundedCornerShape(20.dp)
                )
                .padding(20.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                StatItem(
                    value = "${stats.totalItems}",
                    label = "Items",
                    icon = Icons.Default.Folder
                )
                StatItem(
                    value = formatSize(stats.totalSize),
                    label = "Storage",
                    icon = Icons.Default.Storage
                )
                StatItem(
                    value = formatDuration(stats.totalDuration),
                    label = "Duration",
                    icon = Icons.Default.Schedule
                )
                StatItem(
                    value = "${stats.favoritesCount}",
                    label = "Favorites",
                    icon = Icons.Default.Favorite
                )
            }
        }
    }
}

@Composable
private fun StatItem(
    value: String,
    label: String,
    icon: ImageVector
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Icon(
            icon,
            null,
            tint = Color.White.copy(alpha = 0.8f),
            modifier = Modifier.size(20.dp)
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            value,
            color = Color.White,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold
        )
        Text(
            label,
            color = Color.White.copy(alpha = 0.8f),
            fontSize = 11.sp
        )
    }
}

// ============================================
// 📑 TAB SELECTOR
// ============================================

@Composable
private fun TabSelector(
    selectedTab: Int,
    onTabSelect: (Int) -> Unit
) {
    val tabs = listOf(
        "All" to Icons.Default.Apps,
        "Audio" to Icons.Default.MusicNote,
        "Video" to Icons.Default.Videocam,
        "Clips" to Icons.Default.ContentCut
    )

    LazyRow(
        contentPadding = PaddingValues(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        itemsIndexed(tabs) { index, (label, icon) ->
            val isSelected = selectedTab == index

            Surface(
                onClick = { onTabSelect(index) },
                shape = RoundedCornerShape(12.dp),
                color = if (isSelected) VaultColors.purple else VaultColors.surfaceVariant,
                border = if (isSelected) null else BorderStroke(1.dp, VaultColors.glassBorder)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        icon,
                        null,
                        tint = if (isSelected) Color.White else VaultColors.textSecondary,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        label,
                        color = if (isSelected) Color.White else VaultColors.textSecondary,
                        fontSize = 13.sp,
                        fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal
                    )
                }
            }
        }
    }
}

// ============================================
// 📁 COLLECTIONS ROW
// ============================================

@Composable
private fun CollectionsRow(
    collections: List<MediaCollection>,
    onCollectionClick: (String) -> Unit,
    onNewCollection: () -> Unit
) {
    Column(modifier = Modifier.padding(horizontal = 16.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                "Collections",
                color = VaultColors.textSecondary,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold
            )
            TextButton(onClick = onNewCollection) {
                Icon(
                    Icons.Default.Add,
                    null,
                    tint = VaultColors.pink,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text("New", color = VaultColors.pink, fontSize = 12.sp)
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(collections.filter { it.isPinned || !it.isSmartCollection }.take(6)) { collection ->
                CollectionChip(
                    collection = collection,
                    onClick = { onCollectionClick(collection.id) }
                )
            }
        }
    }
}

@Composable
private fun CollectionChip(
    collection: MediaCollection,
    onClick: () -> Unit
) {
    val color = Color(collection.color)

    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(14.dp),
        color = color.copy(alpha = 0.15f),
        border = BorderStroke(1.dp, color.copy(alpha = 0.3f))
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(collection.icon, fontSize = 16.sp)
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                collection.name,
                color = color,
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

@Composable
private fun SelectedCollectionHeader(
    collection: MediaCollection?,
    onClose: () -> Unit
) {
    if (collection == null) return

    val color = Color(collection.color)

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        shape = RoundedCornerShape(16.dp),
        color = color.copy(alpha = 0.15f),
        border = BorderStroke(1.dp, color.copy(alpha = 0.3f))
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(collection.icon, fontSize = 28.sp)
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    collection.name,
                    color = VaultColors.textPrimary,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold
                )
                if (collection.description.isNotEmpty()) {
                    Text(
                        collection.description,
                        color = VaultColors.textTertiary,
                        fontSize = 12.sp
                    )
                }
            }
            IconButton(onClick = onClose) {
                Icon(Icons.Default.Close, "Close", tint = VaultColors.textSecondary)
            }
        }
    }
}

// ============================================
// 📐 MEDIA GRID
// ============================================

@Composable
private fun MediaGrid(
    items: List<VaultMediaItem>,
    onItemClick: (VaultMediaItem) -> Unit,
    onFavoriteToggle: (VaultMediaItem) -> Unit
) {
    LazyVerticalGrid(
        columns = GridCells.Fixed(2),
        contentPadding = PaddingValues(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier.height(((items.size / 2 + 1) * 180).dp)
    ) {
        items(items) { item ->
            MediaGridItem(
                item = item,
                onClick = { onItemClick(item) },
                onFavoriteToggle = { onFavoriteToggle(item) }
            )
        }
    }
}

@Composable
private fun MediaGridItem(
    item: VaultMediaItem,
    onClick: () -> Unit,
    onFavoriteToggle: () -> Unit
) {
    val typeColor = getMediaTypeColor(item.mediaType)

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = VaultColors.card,
        border = BorderStroke(1.dp, VaultColors.glassBorder),
        onClick = onClick
    ) {
        Column {
            // Thumbnail area
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(90.dp)
                    .background(
                        Brush.linearGradient(
                            listOf(typeColor.copy(alpha = 0.3f), typeColor.copy(alpha = 0.1f))
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(item.mediaType.icon, fontSize = 32.sp)

                // Duration badge
                if (item.metadata.duration > 0) {
                    Surface(
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(8.dp),
                        shape = RoundedCornerShape(6.dp),
                        color = Color.Black.copy(alpha = 0.7f)
                    ) {
                        Text(
                            formatDuration(item.metadata.duration),
                            color = Color.White,
                            fontSize = 10.sp,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                        )
                    }
                }

                // Favorite indicator
                if (item.isFavorite) {
                    Icon(
                        Icons.Default.Favorite,
                        null,
                        tint = VaultColors.pink,
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(8.dp)
                            .size(18.dp)
                    )
                }
            }

            // Info
            Column(modifier = Modifier.padding(12.dp)) {
                Text(
                    item.displayName,
                    color = VaultColors.textPrimary,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = typeColor.copy(alpha = 0.2f)
                    ) {
                        Text(
                            item.mediaType.displayName,
                            color = typeColor,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        formatSize(item.metadata.fileSize),
                        color = VaultColors.textTertiary,
                        fontSize = 10.sp
                    )
                }
            }
        }
    }
}

// ============================================
// 📋 MEDIA LIST
// ============================================

@Composable
private fun MediaList(
    items: List<VaultMediaItem>,
    onItemClick: (VaultMediaItem) -> Unit,
    onFavoriteToggle: (VaultMediaItem) -> Unit
) {
    Column(
        modifier = Modifier.padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        items.forEach { item ->
            MediaListItem(
                item = item,
                onClick = { onItemClick(item) },
                onFavoriteToggle = { onFavoriteToggle(item) }
            )
        }
    }
}

@Composable
private fun MediaListItem(
    item: VaultMediaItem,
    onClick: () -> Unit,
    onFavoriteToggle: () -> Unit
) {
    val typeColor = getMediaTypeColor(item.mediaType)

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        color = VaultColors.card,
        border = BorderStroke(1.dp, VaultColors.glassBorder),
        onClick = onClick
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Thumbnail
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .background(typeColor.copy(alpha = 0.2f), RoundedCornerShape(10.dp)),
                contentAlignment = Alignment.Center
            ) {
                Text(item.mediaType.icon, fontSize = 24.sp)
            }

            Spacer(modifier = Modifier.width(12.dp))

            // Info
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    item.displayName,
                    color = VaultColors.textPrimary,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        item.mediaType.displayName,
                        color = typeColor,
                        fontSize = 11.sp
                    )
                    Text(
                        " • ",
                        color = VaultColors.textTertiary,
                        fontSize = 11.sp
                    )
                    Text(
                        formatDuration(item.metadata.duration),
                        color = VaultColors.textTertiary,
                        fontSize = 11.sp
                    )
                    Text(
                        " • ",
                        color = VaultColors.textTertiary,
                        fontSize = 11.sp
                    )
                    Text(
                        formatSize(item.metadata.fileSize),
                        color = VaultColors.textTertiary,
                        fontSize = 11.sp
                    )
                }
            }

            // Actions
            IconButton(onClick = onFavoriteToggle) {
                Icon(
                    if (item.isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                    "Favorite",
                    tint = if (item.isFavorite) VaultColors.pink else VaultColors.textTertiary,
                    modifier = Modifier.size(22.dp)
                )
            }

            IconButton(onClick = { }) {
                Icon(
                    Icons.Default.MoreVert,
                    "More",
                    tint = VaultColors.textTertiary,
                    modifier = Modifier.size(22.dp)
                )
            }
        }
    }
}

// ============================================
// 📝 COMPACT LIST
// ============================================

@Composable
private fun MediaCompactList(
    items: List<VaultMediaItem>,
    onItemClick: (VaultMediaItem) -> Unit
) {
    Column(
        modifier = Modifier.padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        items.forEach { item ->
            MediaCompactItem(
                item = item,
                onClick = { onItemClick(item) }
            )
        }
    }
}

@Composable
private fun MediaCompactItem(
    item: VaultMediaItem,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = Color.Transparent,
        onClick = onClick
    ) {
        Row(
            modifier = Modifier.padding(vertical = 10.dp, horizontal = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(item.mediaType.icon, fontSize = 16.sp)
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                item.displayName,
                color = VaultColors.textPrimary,
                fontSize = 13.sp,
                modifier = Modifier.weight(1f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            if (item.isFavorite) {
                Icon(
                    Icons.Default.Favorite,
                    null,
                    tint = VaultColors.pink,
                    modifier = Modifier.size(14.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
            }
            Text(
                formatDuration(item.metadata.duration),
                color = VaultColors.textTertiary,
                fontSize = 11.sp
            )
        }
    }
    HorizontalDivider(color = VaultColors.glassBorder.copy(alpha = 0.5f))
}

// ============================================
// 📋 ITEM DETAILS SHEET
// ============================================

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ItemDetailsSheet(
    item: VaultMediaItem,
    onDismiss: () -> Unit
) {
    val typeColor = getMediaTypeColor(item.mediaType)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = VaultColors.surface,
        dragHandle = {
            Box(
                modifier = Modifier
                    .padding(vertical = 12.dp)
                    .width(40.dp)
                    .height(4.dp)
                    .background(VaultColors.glassBorder, RoundedCornerShape(2.dp))
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
                        .size(64.dp)
                        .background(typeColor.copy(alpha = 0.2f), RoundedCornerShape(14.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(item.mediaType.icon, fontSize = 32.sp)
                }
                Spacer(modifier = Modifier.width(16.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        item.displayName,
                        color = VaultColors.textPrimary,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = typeColor.copy(alpha = 0.2f)
                        ) {
                            Text(
                                item.mediaType.displayName,
                                color = typeColor,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            item.source.displayName,
                            color = VaultColors.textTertiary,
                            fontSize = 11.sp
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Quick actions
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                QuickAction(icon = Icons.Default.PlayArrow, label = "Play", color = VaultColors.green)
                QuickAction(icon = if (item.isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder, label = "Favorite", color = VaultColors.pink)
                QuickAction(icon = Icons.Default.Share, label = "Share", color = VaultColors.blue)
                QuickAction(icon = Icons.Default.Delete, label = "Delete", color = VaultColors.red)
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Metadata section
            Text("Details", color = VaultColors.textSecondary, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
            Spacer(modifier = Modifier.height(12.dp))

            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                color = VaultColors.surfaceVariant
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    DetailRow("Duration", formatDuration(item.metadata.duration))
                    DetailRow("Size", formatSize(item.metadata.fileSize))
                    if (item.metadata.bitrate > 0) {
                        DetailRow("Bitrate", "${item.metadata.bitrate} kbps")
                    }
                    if (item.metadata.artist != null) {
                        DetailRow("Artist", item.metadata.artist)
                    }
                    DetailRow("Added", formatDate(item.addedAt))
                    if (item.lastPlayedAt != null) {
                        DetailRow("Last Played", formatDate(item.lastPlayedAt), false)
                    }
                }
            }

            // Tags
            if (item.tags.isNotEmpty()) {
                Spacer(modifier = Modifier.height(20.dp))
                Text("Tags", color = VaultColors.textSecondary, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                Spacer(modifier = Modifier.height(10.dp))
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(item.tags) { tag ->
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = VaultColors.surfaceVariant
                        ) {
                            Text(
                                "#$tag",
                                color = VaultColors.textSecondary,
                                fontSize = 12.sp,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                            )
                        }
                    }
                }
            }

            // Bookmarks
            if (item.playbackData.bookmarks.isNotEmpty()) {
                Spacer(modifier = Modifier.height(20.dp))
                Text("Bookmarks", color = VaultColors.textSecondary, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                Spacer(modifier = Modifier.height(10.dp))
                item.playbackData.bookmarks.forEach { bookmark ->
                    BookmarkItem(bookmark = bookmark)
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }
        }
    }
}

@Composable
private fun QuickAction(
    icon: ImageVector,
    label: String,
    color: Color
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.clickable { }
    ) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .background(color.copy(alpha = 0.15f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, null, tint = color, modifier = Modifier.size(24.dp))
        }
        Spacer(modifier = Modifier.height(6.dp))
        Text(label, color = VaultColors.textSecondary, fontSize = 11.sp)
    }
}

@Composable
private fun DetailRow(label: String, value: String, showDivider: Boolean = true) {
    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(label, color = VaultColors.textTertiary, fontSize = 13.sp)
            Text(value, color = VaultColors.textPrimary, fontSize = 13.sp)
        }
        if (showDivider) {
            HorizontalDivider(color = VaultColors.glassBorder.copy(alpha = 0.3f))
        }
    }
}

@Composable
private fun BookmarkItem(bookmark: Bookmark) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(10.dp),
        color = VaultColors.surfaceVariant
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(4.dp, 32.dp)
                    .background(Color(bookmark.color), RoundedCornerShape(2.dp))
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    bookmark.label.ifEmpty { "Bookmark" },
                    color = VaultColors.textPrimary,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    formatDuration(bookmark.position),
                    color = VaultColors.textTertiary,
                    fontSize = 11.sp
                )
            }
            Icon(
                Icons.Default.PlayArrow,
                "Jump to",
                tint = VaultColors.textSecondary,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

// ============================================
// 🎚️ FILTER SHEET
// ============================================

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FilterSheet(onDismiss: () -> Unit) {
    var selectedTypes by remember { mutableStateOf(setOf<VaultMediaType>()) }
    var onlyFavorites by remember { mutableStateOf(false) }
    var onlyUnplayed by remember { mutableStateOf(false) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = VaultColors.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 32.dp)
        ) {
            Text(
                "Filter",
                color = VaultColors.textPrimary,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(20.dp))

            // Media types
            Text("Media Type", color = VaultColors.textSecondary, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
            Spacer(modifier = Modifier.height(10.dp))

            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(VaultMediaType.entries.toList()) { type ->
                    val isSelected = type in selectedTypes
                    FilterChip(
                        selected = isSelected,
                        onClick = {
                            selectedTypes = if (isSelected) {
                                selectedTypes - type
                            } else {
                                selectedTypes + type
                            }
                        },
                        label = { Text("${type.icon} ${type.displayName}") },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = VaultColors.purple,
                            selectedLabelColor = Color.White
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Quick filters
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Only Favorites", color = VaultColors.textPrimary, modifier = Modifier.weight(1f))
                Switch(
                    checked = onlyFavorites,
                    onCheckedChange = { onlyFavorites = it },
                    colors = SwitchDefaults.colors(checkedTrackColor = VaultColors.pink)
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Only Unplayed", color = VaultColors.textPrimary, modifier = Modifier.weight(1f))
                Switch(
                    checked = onlyUnplayed,
                    onCheckedChange = { onlyUnplayed = it },
                    colors = SwitchDefaults.colors(checkedTrackColor = VaultColors.purple)
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedButton(
                    onClick = {
                        selectedTypes = emptySet()
                        onlyFavorites = false
                        onlyUnplayed = false
                    },
                    modifier = Modifier.weight(1f),
                    border = BorderStroke(1.dp, VaultColors.glassBorder)
                ) {
                    Text("Reset", color = VaultColors.textSecondary)
                }
                Button(
                    onClick = onDismiss,
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(containerColor = VaultColors.purple)
                ) {
                    Text("Apply")
                }
            }
        }
    }
}

// ============================================
// ➕ NEW COLLECTION DIALOG
// ============================================

@Composable
private fun NewCollectionDialog(
    onDismiss: () -> Unit,
    onCreate: (String) -> Unit
) {
    var name by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = VaultColors.surface,
        title = {
            Text("New Collection", color = VaultColors.textPrimary, fontWeight = FontWeight.Bold)
        },
        text = {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Collection Name") },
                placeholder = { Text("My Collection") },
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = VaultColors.purple,
                    unfocusedBorderColor = VaultColors.glassBorder,
                    focusedTextColor = VaultColors.textPrimary,
                    unfocusedTextColor = VaultColors.textPrimary
                ),
                singleLine = true
            )
        },
        confirmButton = {
            Button(
                onClick = { onCreate(name) },
                enabled = name.isNotBlank(),
                colors = ButtonDefaults.buttonColors(containerColor = VaultColors.purple)
            ) {
                Text("Create")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = VaultColors.textSecondary)
            }
        }
    )
}

// ============================================
// 🔧 HELPER FUNCTIONS
// ============================================

private fun getMediaTypeColor(type: VaultMediaType): Color {
    return when (type) {
        VaultMediaType.AUDIO -> VaultColors.pink
        VaultMediaType.VIDEO -> VaultColors.blue
        VaultMediaType.PODCAST -> VaultColors.purple
        VaultMediaType.CLIP -> VaultColors.cyan
        VaultMediaType.VOICE_NOTE -> VaultColors.amber
        VaultMediaType.REMIX -> VaultColors.orange
        VaultMediaType.ROOM_RECORDING -> VaultColors.teal
        VaultMediaType.DOWNLOADED -> VaultColors.green
    }
}

private fun formatDuration(ms: Long): String {
    if (ms <= 0) return "0:00"
    val seconds = (ms / 1000) % 60
    val minutes = (ms / 60000) % 60
    val hours = ms / 3600000
    return if (hours > 0) {
        String.format("%d:%02d:%02d", hours, minutes, seconds)
    } else {
        String.format("%d:%02d", minutes, seconds)
    }
}

private fun formatSize(bytes: Long): String {
    return when {
        bytes >= 1_000_000_000 -> String.format("%.1f GB", bytes / 1_000_000_000f)
        bytes >= 1_000_000 -> String.format("%.1f MB", bytes / 1_000_000f)
        bytes >= 1_000 -> String.format("%.1f KB", bytes / 1_000f)
        else -> "$bytes B"
    }
}

private fun formatDate(timestamp: Long): String {
    val now = System.currentTimeMillis()
    val diff = now - timestamp

    return when {
        diff < 60_000 -> "Just now"
        diff < 3600_000 -> "${diff / 60_000} min ago"
        diff < 86400_000 -> "${diff / 3600_000} hours ago"
        diff < 604800_000 -> "${diff / 86400_000} days ago"
        else -> {
            val date = java.util.Date(timestamp)
            java.text.SimpleDateFormat("MMM d, yyyy", java.util.Locale.getDefault()).format(date)
        }
    }
}

private fun createMockVaultItems(): List<VaultMediaItem> {
    return listOf(
        VaultMediaItem(
            uri = "content://media/audio/1",
            fileName = "morning_vibes.mp3",
            displayName = "Morning Vibes Mix",
            mediaType = VaultMediaType.AUDIO,
            source = MediaSource.DOWNLOAD,
            metadata = MediaMetadata(duration = 245000, fileSize = 8500000, bitrate = 320, artist = "Various Artists"),
            isFavorite = true,
            playbackData = PlaybackData(playCount = 12)
        ),
        VaultMediaItem(
            uri = "content://media/video/2",
            fileName = "tutorial.mp4",
            displayName = "Kotlin Tutorial #5",
            mediaType = VaultMediaType.VIDEO,
            source = MediaSource.DOWNLOAD,
            metadata = MediaMetadata(duration = 1823000, fileSize = 450000000, hasVideo = true, width = 1920, height = 1080)
        ),
        VaultMediaItem(
            uri = "content://media/podcast/3",
            fileName = "tech_talk_ep45.mp3",
            displayName = "Tech Talk Episode 45",
            mediaType = VaultMediaType.PODCAST,
            source = MediaSource.PODCAST,
            metadata = MediaMetadata(duration = 3600000, fileSize = 65000000, bitrate = 192),
            isFavorite = true
        ),
        VaultMediaItem(
            uri = "content://media/clip/4",
            fileName = "funny_moment.mp3",
            displayName = "Funny Podcast Moment",
            mediaType = VaultMediaType.CLIP,
            source = MediaSource.REMIX_STUDIO,
            metadata = MediaMetadata(duration = 45000, fileSize = 720000),
            tags = listOf("funny", "podcast", "clip")
        ),
        VaultMediaItem(
            uri = "content://media/voice/5",
            fileName = "voice_note_123.m4a",
            displayName = "Meeting Notes - Jan 3",
            mediaType = VaultMediaType.VOICE_NOTE,
            source = MediaSource.VOICE_RECORDER,
            metadata = MediaMetadata(duration = 180000, fileSize = 2800000)
        ),
        VaultMediaItem(
            uri = "content://media/remix/6",
            fileName = "my_remix_v2.mp3",
            displayName = "Summer Beats Remix",
            mediaType = VaultMediaType.REMIX,
            source = MediaSource.REMIX_STUDIO,
            metadata = MediaMetadata(duration = 195000, fileSize = 7200000, bitrate = 320),
            isFavorite = true
        ),
        VaultMediaItem(
            uri = "content://media/room/7",
            fileName = "room_recording_abc.mp3",
            displayName = "Late Night Beats Room",
            mediaType = VaultMediaType.ROOM_RECORDING,
            source = MediaSource.AUDIO_ROOM,
            metadata = MediaMetadata(duration = 7200000, fileSize = 120000000)
        ),
        VaultMediaItem(
            uri = "content://media/audio/8",
            fileName = "playlist_mix.mp3",
            displayName = "Ultimate Workout Playlist",
            mediaType = VaultMediaType.AUDIO,
            source = MediaSource.DOWNLOAD,
            metadata = MediaMetadata(duration = 5400000, fileSize = 98000000)
        )
    )
}

