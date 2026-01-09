package com.example.dwn.vault

import android.content.Context
import android.media.MediaMetadataRetriever
import android.net.Uri
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import java.io.File
import java.security.MessageDigest
import java.util.concurrent.ConcurrentHashMap

/**
 * VaultManager - Central manager for the Universal Media Vault
 *
 * Handles indexing, search, organization, and retrieval of all media assets
 */
class VaultManager(
    private val context: Context
) {
    private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())

    // ============================================
    // STATE
    // ============================================

    private val _state = MutableStateFlow(VaultState())
    val state: StateFlow<VaultState> = _state.asStateFlow()

    private val _items = MutableStateFlow<List<VaultMediaItem>>(emptyList())
    val items: StateFlow<List<VaultMediaItem>> = _items.asStateFlow()

    private val _collections = MutableStateFlow<List<MediaCollection>>(defaultSmartCollections)
    val collections: StateFlow<List<MediaCollection>> = _collections.asStateFlow()

    private val _statistics = MutableStateFlow(VaultStatistics())
    val statistics: StateFlow<VaultStatistics> = _statistics.asStateFlow()

    private val _settings = MutableStateFlow(VaultSettings())
    val settings: StateFlow<VaultSettings> = _settings.asStateFlow()

    // In-memory cache for fast lookups
    private val itemsById = ConcurrentHashMap<String, VaultMediaItem>()
    private val itemsByHash = ConcurrentHashMap<String, String>()  // hash -> id for duplicate detection

    // ============================================
    // INITIALIZATION
    // ============================================

    init {
        // Load saved data
        loadVaultData()

        // Update statistics
        updateStatistics()
    }

    private fun loadVaultData() {
        // In a real app, load from database
        // For now, use mock data
        val mockItems = createMockData()
        _items.value = mockItems
        mockItems.forEach { itemsById[it.id] = it }

        updateState()
    }

    // ============================================
    // MEDIA INGESTION
    // ============================================

    /**
     * Add a new media item to the vault
     */
    suspend fun addMedia(
        uri: String,
        displayName: String,
        mediaType: VaultMediaType,
        source: MediaSource = MediaSource.UNKNOWN,
        tags: List<String> = emptyList(),
        collectionId: String? = null
    ): VaultMediaItem {
        return withContext(Dispatchers.IO) {
            // Extract metadata
            val metadata = extractMetadata(uri)

            // Check for duplicates
            val hash = calculateFileHash(uri)
            if (_settings.value.duplicateDetection && hash != null) {
                val existingId = itemsByHash[hash]
                if (existingId != null) {
                    // Return existing item instead
                    return@withContext itemsById[existingId]!!
                }
            }

            // Create new item
            val item = VaultMediaItem(
                uri = uri,
                fileName = uri.substringAfterLast("/"),
                displayName = displayName,
                mediaType = mediaType,
                source = source,
                metadata = metadata,
                tags = tags,
                collections = listOfNotNull(collectionId)
            )

            // Add to vault
            itemsById[item.id] = item
            if (hash != null) {
                itemsByHash[hash] = item.id
            }

            val currentItems = _items.value.toMutableList()
            currentItems.add(0, item)
            _items.value = currentItems

            updateStatistics()
            updateState()

            item
        }
    }

    /**
     * Import media from downloader
     */
    suspend fun importFromDownloader(
        uri: String,
        fileName: String,
        isVideo: Boolean
    ): VaultMediaItem {
        return addMedia(
            uri = uri,
            displayName = fileName.substringBeforeLast("."),
            mediaType = if (isVideo) VaultMediaType.VIDEO else VaultMediaType.AUDIO,
            source = MediaSource.DOWNLOAD
        )
    }

    /**
     * Import remix from Remix Studio
     */
    suspend fun importFromRemixStudio(
        uri: String,
        projectName: String,
        isVideo: Boolean
    ): VaultMediaItem {
        return addMedia(
            uri = uri,
            displayName = projectName,
            mediaType = if (isVideo) VaultMediaType.VIDEO else VaultMediaType.REMIX,
            source = MediaSource.REMIX_STUDIO
        )
    }

    /**
     * Import room recording
     */
    suspend fun importRoomRecording(
        uri: String,
        roomTitle: String
    ): VaultMediaItem {
        return addMedia(
            uri = uri,
            displayName = roomTitle,
            mediaType = VaultMediaType.ROOM_RECORDING,
            source = MediaSource.AUDIO_ROOM
        )
    }

    /**
     * Extract metadata from media file
     */
    private fun extractMetadata(uri: String): MediaMetadata {
        return try {
            val retriever = MediaMetadataRetriever()
            retriever.setDataSource(context, Uri.parse(uri))

            val duration = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLongOrNull() ?: 0L
            val bitrate = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_BITRATE)?.toIntOrNull() ?: 0
            val hasVideo = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_HAS_VIDEO) == "yes"
            val width = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH)?.toIntOrNull() ?: 0
            val height = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT)?.toIntOrNull() ?: 0
            val artist = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ARTIST)
            val album = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ALBUM)
            val genre = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_GENRE)
            val mimeType = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_MIMETYPE) ?: ""

            retriever.release()

            MediaMetadata(
                duration = duration,
                bitrate = bitrate / 1000,
                hasVideo = hasVideo,
                width = width,
                height = height,
                artist = artist,
                album = album,
                genre = genre,
                mimeType = mimeType
            )
        } catch (e: Exception) {
            MediaMetadata()
        }
    }

    /**
     * Calculate file hash for duplicate detection
     */
    private fun calculateFileHash(uri: String): String? {
        return try {
            val inputStream = context.contentResolver.openInputStream(Uri.parse(uri))
            val digest = MessageDigest.getInstance("MD5")
            val buffer = ByteArray(8192)
            var read: Int

            inputStream?.use { stream ->
                while (stream.read(buffer).also { read = it } != -1) {
                    digest.update(buffer, 0, read)
                }
            }

            digest.digest().joinToString("") { "%02x".format(it) }
        } catch (e: Exception) {
            null
        }
    }

    // ============================================
    // ITEM MANAGEMENT
    // ============================================

    /**
     * Get item by ID
     */
    fun getItem(id: String): VaultMediaItem? = itemsById[id]

    /**
     * Update an item
     */
    fun updateItem(item: VaultMediaItem) {
        itemsById[item.id] = item

        val currentItems = _items.value.toMutableList()
        val index = currentItems.indexOfFirst { it.id == item.id }
        if (index >= 0) {
            currentItems[index] = item
            _items.value = currentItems
        }

        updateState()
    }

    /**
     * Delete an item
     */
    fun deleteItem(id: String) {
        itemsById.remove(id)
        _items.value = _items.value.filter { it.id != id }
        updateStatistics()
        updateState()
    }

    /**
     * Delete multiple items
     */
    fun deleteItems(ids: Set<String>) {
        ids.forEach { itemsById.remove(it) }
        _items.value = _items.value.filter { it.id !in ids }
        updateStatistics()
        updateState()
    }

    /**
     * Toggle favorite status
     */
    fun toggleFavorite(id: String) {
        val item = itemsById[id] ?: return
        updateItem(item.copy(isFavorite = !item.isFavorite))
    }

    /**
     * Archive/unarchive item
     */
    fun toggleArchive(id: String) {
        val item = itemsById[id] ?: return
        updateItem(item.copy(isArchived = !item.isArchived))
    }

    /**
     * Update playback position
     */
    fun updatePlaybackPosition(id: String, position: Long) {
        val item = itemsById[id] ?: return
        val newPlaybackData = item.playbackData.copy(lastPosition = position)
        updateItem(item.copy(
            playbackData = newPlaybackData,
            lastPlayedAt = System.currentTimeMillis()
        ))
    }

    /**
     * Mark as played
     */
    fun markAsPlayed(id: String) {
        val item = itemsById[id] ?: return
        val newPlaybackData = item.playbackData.copy(
            playCount = item.playbackData.playCount + 1,
            isCompleted = true
        )
        updateItem(item.copy(
            playbackData = newPlaybackData,
            lastPlayedAt = System.currentTimeMillis()
        ))
    }

    /**
     * Add bookmark
     */
    fun addBookmark(id: String, position: Long, label: String, note: String = "") {
        val item = itemsById[id] ?: return
        val bookmark = Bookmark(
            position = position,
            label = label,
            note = note
        )
        val newBookmarks = item.playbackData.bookmarks + bookmark
        val newPlaybackData = item.playbackData.copy(bookmarks = newBookmarks)
        updateItem(item.copy(playbackData = newPlaybackData))
    }

    /**
     * Remove bookmark
     */
    fun removeBookmark(itemId: String, bookmarkId: String) {
        val item = itemsById[itemId] ?: return
        val newBookmarks = item.playbackData.bookmarks.filter { it.id != bookmarkId }
        val newPlaybackData = item.playbackData.copy(bookmarks = newBookmarks)
        updateItem(item.copy(playbackData = newPlaybackData))
    }

    /**
     * Add tags to item
     */
    fun addTags(id: String, tags: List<String>) {
        val item = itemsById[id] ?: return
        val newTags = (item.tags + tags).distinct()
        updateItem(item.copy(tags = newTags))
    }

    /**
     * Remove tag from item
     */
    fun removeTag(id: String, tag: String) {
        val item = itemsById[id] ?: return
        updateItem(item.copy(tags = item.tags.filter { it != tag }))
    }

    // ============================================
    // COLLECTIONS
    // ============================================

    /**
     * Create a new collection
     */
    fun createCollection(
        name: String,
        description: String = "",
        icon: String = "📁",
        color: Long = 0xFFE91E63
    ): MediaCollection {
        val collection = MediaCollection(
            name = name,
            description = description,
            icon = icon,
            color = color
        )
        _collections.value = _collections.value + collection
        return collection
    }

    /**
     * Update collection
     */
    fun updateCollection(collection: MediaCollection) {
        val current = _collections.value.toMutableList()
        val index = current.indexOfFirst { it.id == collection.id }
        if (index >= 0) {
            current[index] = collection
            _collections.value = current
        }
    }

    /**
     * Delete collection
     */
    fun deleteCollection(id: String) {
        // Don't delete smart collections
        val collection = _collections.value.find { it.id == id }
        if (collection?.isSmartCollection == true) return

        _collections.value = _collections.value.filter { it.id != id }

        // Remove collection reference from items
        _items.value.filter { id in it.collections }.forEach { item ->
            updateItem(item.copy(collections = item.collections.filter { it != id }))
        }
    }

    /**
     * Add item to collection
     */
    fun addToCollection(itemId: String, collectionId: String) {
        val item = itemsById[itemId] ?: return
        if (collectionId in item.collections) return
        updateItem(item.copy(collections = item.collections + collectionId))
    }

    /**
     * Remove item from collection
     */
    fun removeFromCollection(itemId: String, collectionId: String) {
        val item = itemsById[itemId] ?: return
        updateItem(item.copy(collections = item.collections.filter { it != collectionId }))
    }

    /**
     * Get items in collection
     */
    fun getCollectionItems(collectionId: String): List<VaultMediaItem> {
        val collection = _collections.value.find { it.id == collectionId }

        return if (collection?.isSmartCollection == true && collection.smartRule != null) {
            // Apply smart rule
            applySmartRule(collection.smartRule)
        } else {
            _items.value.filter { collectionId in it.collections }
        }
    }

    private fun applySmartRule(rule: SmartCollectionRule): List<VaultMediaItem> {
        var result = _items.value

        for (condition in rule.conditions) {
            result = result.filter { item ->
                evaluateCondition(item, condition)
            }
        }

        // Sort
        result = when (rule.sortBy) {
            SortOption.DATE_ADDED -> result.sortedBy { it.addedAt }
            SortOption.DATE_MODIFIED -> result.sortedBy { it.modifiedAt }
            SortOption.DATE_PLAYED -> result.sortedBy { it.lastPlayedAt ?: 0 }
            SortOption.NAME -> result.sortedBy { it.displayName }
            SortOption.DURATION -> result.sortedBy { it.metadata.duration }
            SortOption.FILE_SIZE -> result.sortedBy { it.metadata.fileSize }
            SortOption.PLAY_COUNT -> result.sortedBy { it.playbackData.playCount }
        }

        if (rule.sortDescending) {
            result = result.reversed()
        }

        // Apply limit
        if (rule.limit != null) {
            result = result.take(rule.limit)
        }

        return result
    }

    private fun evaluateCondition(item: VaultMediaItem, condition: RuleCondition): Boolean {
        return when (condition.field) {
            RuleField.MEDIA_TYPE -> item.mediaType.name == condition.value
            RuleField.SOURCE -> item.source.name == condition.value
            RuleField.TAG -> condition.value in item.tags
            RuleField.SPEAKER -> item.speakers.any { it.name == condition.value }
            RuleField.DURATION -> {
                val duration = item.metadata.duration
                val value = condition.value.toLongOrNull() ?: 0
                when (condition.operator) {
                    RuleOperator.GREATER_THAN -> duration > value
                    RuleOperator.LESS_THAN -> duration < value
                    else -> true
                }
            }
            RuleField.DATE_ADDED -> {
                val days = condition.value.toIntOrNull() ?: 0
                val cutoff = System.currentTimeMillis() - (days * 24 * 60 * 60 * 1000L)
                item.addedAt >= cutoff
            }
            RuleField.IS_FAVORITE -> item.isFavorite.toString() == condition.value
            RuleField.HAS_TRANSCRIPT -> (item.transcript != null).toString() == condition.value
            else -> true
        }
    }

    // ============================================
    // SEARCH
    // ============================================

    /**
     * Search the vault
     */
    fun search(query: VaultSearchQuery): List<SearchResult> {
        if (query.text.isEmpty() && query.mediaTypes.isEmpty() &&
            query.sources.isEmpty() && !query.onlyFavorites && !query.onlyUnplayed) {
            return emptyList()
        }

        val results = mutableListOf<SearchResult>()

        for (item in _items.value) {
            // Apply filters
            if (query.mediaTypes.isNotEmpty() && item.mediaType !in query.mediaTypes) continue
            if (query.sources.isNotEmpty() && item.source !in query.sources) continue
            if (query.onlyFavorites && !item.isFavorite) continue
            if (query.onlyUnplayed && item.playbackData.playCount > 0) continue
            if (query.onlyWithTranscript && item.transcript == null) continue

            // Check date range
            if (query.dateRange != null) {
                if (item.addedAt < query.dateRange.start || item.addedAt > query.dateRange.end) continue
            }

            // Check duration range
            if (query.durationRange != null) {
                if (item.metadata.duration < query.durationRange.minMs ||
                    item.metadata.duration > query.durationRange.maxMs) continue
            }

            // Text search
            if (query.text.isNotEmpty()) {
                val matchedFields = mutableListOf<SearchField>()
                val highlights = mutableListOf<SearchHighlight>()
                val searchText = query.text.lowercase()

                // Search in title
                if (SearchField.TITLE in query.searchIn) {
                    val titleLower = item.displayName.lowercase()
                    val index = titleLower.indexOf(searchText)
                    if (index >= 0) {
                        matchedFields.add(SearchField.TITLE)
                        highlights.add(SearchHighlight(
                            field = SearchField.TITLE,
                            text = item.displayName,
                            matchStart = index,
                            matchEnd = index + searchText.length
                        ))
                    }
                }

                // Search in transcript
                if (SearchField.TRANSCRIPT in query.searchIn && item.transcript != null) {
                    val fullText = item.transcript.fullText.lowercase()
                    val index = fullText.indexOf(searchText)
                    if (index >= 0) {
                        matchedFields.add(SearchField.TRANSCRIPT)
                        // Find the segment containing this match
                        val segment = item.transcript.segments.find {
                            it.text.lowercase().contains(searchText)
                        }
                        highlights.add(SearchHighlight(
                            field = SearchField.TRANSCRIPT,
                            text = segment?.text ?: "",
                            matchStart = 0,
                            matchEnd = searchText.length,
                            timestamp = segment?.startTime
                        ))
                    }
                }

                // Search in tags
                if (SearchField.TAGS in query.searchIn) {
                    val matchingTag = item.tags.find { it.lowercase().contains(searchText) }
                    if (matchingTag != null) {
                        matchedFields.add(SearchField.TAGS)
                    }
                }

                if (matchedFields.isEmpty()) continue

                // Calculate relevance
                val relevance = calculateRelevance(item, matchedFields, searchText)

                results.add(SearchResult(
                    item = item,
                    matchedFields = matchedFields,
                    highlights = highlights,
                    relevanceScore = relevance
                ))
            } else {
                // No text search, just filters
                results.add(SearchResult(
                    item = item,
                    matchedFields = emptyList(),
                    highlights = emptyList(),
                    relevanceScore = 1f
                ))
            }
        }

        // Sort results
        val sortedResults = when (query.sortBy) {
            SortOption.DATE_ADDED -> results.sortedBy { it.item.addedAt }
            SortOption.NAME -> results.sortedBy { it.item.displayName }
            else -> results.sortedByDescending { it.relevanceScore }
        }

        return if (query.sortDescending) sortedResults.reversed() else sortedResults
    }

    private fun calculateRelevance(
        item: VaultMediaItem,
        matchedFields: List<SearchField>,
        searchText: String
    ): Float {
        var score = 0f

        // Title match is most valuable
        if (SearchField.TITLE in matchedFields) {
            score += 10f
            // Exact match bonus
            if (item.displayName.lowercase() == searchText) score += 5f
            // Starts with bonus
            if (item.displayName.lowercase().startsWith(searchText)) score += 3f
        }

        // Transcript match
        if (SearchField.TRANSCRIPT in matchedFields) {
            score += 5f
        }

        // Tag match
        if (SearchField.TAGS in matchedFields) {
            score += 3f
        }

        // Recency bonus
        val daysSinceAdded = (System.currentTimeMillis() - item.addedAt) / (24 * 60 * 60 * 1000)
        score += maxOf(0f, (30 - daysSinceAdded) / 30f)

        // Favorite bonus
        if (item.isFavorite) score += 1f

        return score
    }

    // ============================================
    // STATISTICS
    // ============================================

    private fun updateStatistics() {
        val items = _items.value

        _statistics.value = VaultStatistics(
            totalItems = items.size,
            totalSize = items.sumOf { it.metadata.fileSize },
            totalDuration = items.sumOf { it.metadata.duration },
            itemsByType = items.groupBy { it.mediaType }.mapValues { it.value.size },
            itemsBySource = items.groupBy { it.source }.mapValues { it.value.size },
            totalPlayTime = items.sumOf { it.playbackData.totalPlayTime },
            favoritesCount = items.count { it.isFavorite },
            collectionsCount = _collections.value.count { !it.isSmartCollection },
            transcribedCount = items.count { it.transcript != null },
            encryptedCount = items.count { it.isEncrypted }
        )
    }

    // ============================================
    // STATE MANAGEMENT
    // ============================================

    private fun updateState() {
        _state.value = _state.value.copy(
            items = _items.value,
            collections = _collections.value,
            statistics = _statistics.value,
            settings = _settings.value
        )
    }

    fun setViewMode(mode: VaultViewMode) {
        _state.value = _state.value.copy(viewMode = mode)
    }

    fun setSelectedItems(ids: Set<String>) {
        _state.value = _state.value.copy(selectedItems = ids)
    }

    fun clearSelection() {
        _state.value = _state.value.copy(selectedItems = emptySet())
    }

    // ============================================
    // MOCK DATA
    // ============================================

    private fun createMockData(): List<VaultMediaItem> {
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
                metadata = MediaMetadata(duration = 1823000, fileSize = 450000000, hasVideo = true, width = 1920, height = 1080),
                chapters = listOf(
                    MediaChapter(startTime = 0, endTime = 300000, title = "Introduction"),
                    MediaChapter(startTime = 300000, endTime = 900000, title = "Setup"),
                    MediaChapter(startTime = 900000, endTime = 1823000, title = "Coding")
                )
            ),
            VaultMediaItem(
                uri = "content://media/podcast/3",
                fileName = "tech_talk_ep45.mp3",
                displayName = "Tech Talk Episode 45: AI Revolution",
                mediaType = VaultMediaType.PODCAST,
                source = MediaSource.PODCAST,
                metadata = MediaMetadata(duration = 3600000, fileSize = 65000000, bitrate = 192),
                transcript = MediaTranscript(
                    fullText = "Welcome to Tech Talk...",
                    segments = listOf(
                        TranscriptSegment(0, 5000, "Welcome to Tech Talk", speakerId = "host"),
                        TranscriptSegment(5000, 15000, "Today we're discussing AI", speakerId = "host")
                    )
                ),
                speakers = listOf(
                    Speaker(name = "John Host", segments = listOf(SpeakerSegment(0, 1800000))),
                    Speaker(name = "Jane Guest", segments = listOf(SpeakerSegment(1800000, 3600000)))
                )
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
                metadata = MediaMetadata(duration = 7200000, fileSize = 120000000),
                speakers = listOf(
                    Speaker(name = "DJ Mike"),
                    Speaker(name = "Sarah"),
                    Speaker(name = "Alex")
                )
            )
        )
    }

    // ============================================
    // CLEANUP
    // ============================================

    fun release() {
        scope.cancel()
    }
}

