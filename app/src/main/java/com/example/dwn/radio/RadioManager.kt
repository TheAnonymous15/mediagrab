package com.example.dwn.radio

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.os.Build
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import java.util.Timer
import java.util.TimerTask

/**
 * RadioManager - Central manager for Super Online Radio
 *
 * Handles station playback, streaming, discovery, and integration
 * with Media Vault, Remix Studio, and Context Media Mode.
 */
class RadioManager(
    private val context: Context
) {
    private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())
    private val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager

    // ============================================
    // STATE
    // ============================================

    private val _state = MutableStateFlow(RadioState())
    val state: StateFlow<RadioState> = _state.asStateFlow()

    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying.asStateFlow()

    private val _currentStation = MutableStateFlow<RadioStation?>(null)
    val currentStation: StateFlow<RadioStation?> = _currentStation.asStateFlow()

    private val _nowPlaying = MutableStateFlow<NowPlaying?>(null)
    val nowPlaying: StateFlow<NowPlaying?> = _nowPlaying.asStateFlow()

    private val _volume = MutableStateFlow(1f)
    val volume: StateFlow<Float> = _volume.asStateFlow()

    private val _isBuffering = MutableStateFlow(false)
    val isBuffering: StateFlow<Boolean> = _isBuffering.asStateFlow()

    // Favorites
    private val _favorites = MutableStateFlow<Set<String>>(emptySet())
    val favorites: StateFlow<Set<String>> = _favorites.asStateFlow()

    // History
    private val _history = MutableStateFlow<List<ListeningHistoryEntry>>(emptyList())
    val history: StateFlow<List<ListeningHistoryEntry>> = _history.asStateFlow()

    // Sleep timer
    private var sleepTimer: Timer? = null
    private val _sleepTimerRemaining = MutableStateFlow<Long?>(null)
    val sleepTimerRemaining: StateFlow<Long?> = _sleepTimerRemaining.asStateFlow()

    // Audio focus
    private var audioFocusRequest: AudioFocusRequest? = null
    private var hasAudioFocus = false

    // ============================================
    // INITIALIZATION
    // ============================================

    init {
        loadInitialData()
    }

    private fun loadInitialData() {
        // Load stations
        val allStations = sampleTraditionalStations + defaultSmartStations

        // Create categories
        val categories = defaultGenreCategories + defaultMoodCategories

        // Create featured content
        val featured = listOf(
            FeaturedRadio(
                type = FeaturedType.PERSONALIZED,
                title = "Made For You",
                subtitle = "Your personal radio mix",
                stationId = "smart_daily_mix",
                color = 0xFFE91E63
            ),
            FeaturedRadio(
                type = FeaturedType.MOOD_MIX,
                title = "Focus Flow",
                subtitle = "Concentration boosting beats",
                stationId = "smart_focus",
                color = 0xFF2196F3
            ),
            FeaturedRadio(
                type = FeaturedType.STATION,
                title = "Lofi Girl",
                subtitle = "24/7 beats to relax/study to",
                stationId = "lofi_girl",
                color = 0xFF9C27B0
            )
        )

        _state.value = _state.value.copy(
            stations = allStations,
            categories = categories,
            featured = featured,
            smartStations = defaultSmartStations
        )
    }

    // ============================================
    // PLAYBACK CONTROL
    // ============================================

    /**
     * Play a station
     */
    fun play(station: RadioStation) {
        scope.launch {
            // Request audio focus
            if (!requestAudioFocus()) {
                _state.value = _state.value.copy(error = "Could not get audio focus")
                return@launch
            }

            _isBuffering.value = true
            _currentStation.value = station

            // Simulate buffering
            delay(500)

            // Create now playing
            val nowPlaying = when (station.type) {
                StationType.TRADITIONAL -> createTraditionalNowPlaying(station)
                StationType.SMART_AUTO -> createSmartNowPlaying(station)
                StationType.PODCAST -> createPodcastNowPlaying(station)
                StationType.CREATOR -> createCreatorNowPlaying(station)
                StationType.LIVE_EVENT -> createLiveEventNowPlaying(station)
            }

            _nowPlaying.value = nowPlaying
            _isPlaying.value = true
            _isBuffering.value = false

            // Update state
            _state.value = _state.value.copy(
                isPlaying = true,
                currentStation = station,
                nowPlaying = nowPlaying,
                isBuffering = false
            )

            // Add to history
            addToHistory(station)

            // Start metadata updates
            startMetadataPolling(station)
        }
    }

    /**
     * Stop playback
     */
    fun stop() {
        _isPlaying.value = false
        _currentStation.value = null
        _nowPlaying.value = null

        _state.value = _state.value.copy(
            isPlaying = false,
            currentStation = null,
            nowPlaying = null
        )

        abandonAudioFocus()
        stopMetadataPolling()
    }

    /**
     * Pause playback
     */
    fun pause() {
        _isPlaying.value = false
        _state.value = _state.value.copy(isPlaying = false)
    }

    /**
     * Resume playback
     */
    fun resume() {
        if (_currentStation.value != null) {
            _isPlaying.value = true
            _state.value = _state.value.copy(isPlaying = true)
        }
    }

    /**
     * Toggle play/pause
     */
    fun togglePlayPause() {
        if (_isPlaying.value) {
            pause()
        } else {
            resume()
        }
    }

    /**
     * Set volume
     */
    fun setVolume(volume: Float) {
        _volume.value = volume.coerceIn(0f, 1f)
    }

    /**
     * Skip to next track (for smart radio)
     */
    fun skipNext() {
        val station = _currentStation.value ?: return
        if (station.type == StationType.SMART_AUTO || station.type == StationType.PODCAST) {
            scope.launch {
                _isBuffering.value = true
                delay(300)
                _nowPlaying.value = createSmartNowPlaying(station)
                _isBuffering.value = false
            }
        }
    }

    // ============================================
    // NOW PLAYING CREATION
    // ============================================

    private fun createTraditionalNowPlaying(station: RadioStation): NowPlaying {
        // Simulate track data for demo
        val tracks = listOf(
            Triple("Blinding Lights", "The Weeknd", "After Hours"),
            Triple("Levitating", "Dua Lipa", "Future Nostalgia"),
            Triple("Save Your Tears", "The Weeknd", "After Hours"),
            Triple("Don't Start Now", "Dua Lipa", "Future Nostalgia"),
            Triple("Watermelon Sugar", "Harry Styles", "Fine Line")
        )
        val track = tracks.random()

        return NowPlaying(
            stationId = station.id,
            trackTitle = track.first,
            artistName = track.second,
            albumName = track.third,
            isLive = true,
            allowClip = station.allowClipping
        )
    }

    private fun createSmartNowPlaying(station: RadioStation): NowPlaying {
        val tracks = when {
            station.moods.contains(RadioMood.FOCUSED) -> listOf(
                Triple("Weightless", "Marconi Union", "Ambient"),
                Triple("Clair de Lune", "Debussy", "Classical"),
                Triple("Intro", "The xx", "xx")
            )
            station.moods.contains(RadioMood.ENERGETIC) -> listOf(
                Triple("Can't Hold Us", "Macklemore", "The Heist"),
                Triple("Titanium", "David Guetta ft. Sia", "Nothing But the Beat"),
                Triple("Stronger", "Kanye West", "Graduation")
            )
            station.moods.contains(RadioMood.RELAXED) -> listOf(
                Triple("Sunset Lover", "Petit Biscuit", "Presence"),
                Triple("Midnight City", "M83", "Hurry Up, We're Dreaming"),
                Triple("Eventually", "Tame Impala", "Currents")
            )
            else -> listOf(
                Triple("Electric Feel", "MGMT", "Oracular Spectacular"),
                Triple("Do I Wanna Know?", "Arctic Monkeys", "AM"),
                Triple("Take On Me", "a-ha", "Hunting High and Low")
            )
        }
        val track = tracks.random()

        return NowPlaying(
            stationId = station.id,
            trackTitle = track.first,
            artistName = track.second,
            albumName = track.third,
            isLive = false,
            duration = (180000..300000).random().toLong(),
            allowClip = true
        )
    }

    private fun createPodcastNowPlaying(station: RadioStation): NowPlaying {
        return NowPlaying(
            stationId = station.id,
            showName = station.name,
            episodeTitle = "Episode ${(1..100).random()}: Latest Update",
            hostName = station.ownerName ?: "Host",
            isLive = false,
            duration = (1800000..3600000).random().toLong(),
            allowClip = station.allowClipping
        )
    }

    private fun createCreatorNowPlaying(station: RadioStation): NowPlaying {
        return NowPlaying(
            stationId = station.id,
            showName = station.name,
            hostName = station.ownerName,
            episodeTitle = "Live Session",
            isLive = station.isLive,
            allowClip = station.allowClipping
        )
    }

    private fun createLiveEventNowPlaying(station: RadioStation): NowPlaying {
        return NowPlaying(
            stationId = station.id,
            showName = station.name,
            episodeTitle = "Live Event",
            isLive = true,
            allowClip = station.allowClipping
        )
    }

    // ============================================
    // METADATA POLLING
    // ============================================

    private var metadataJob: Job? = null

    private fun startMetadataPolling(station: RadioStation) {
        stopMetadataPolling()

        if (station.type == StationType.TRADITIONAL) {
            metadataJob = scope.launch {
                while (isActive) {
                    delay(30000) // Update every 30 seconds
                    if (_isPlaying.value && _currentStation.value?.id == station.id) {
                        _nowPlaying.value = createTraditionalNowPlaying(station)
                    }
                }
            }
        }
    }

    private fun stopMetadataPolling() {
        metadataJob?.cancel()
        metadataJob = null
    }

    // ============================================
    // FAVORITES
    // ============================================

    /**
     * Toggle favorite status
     */
    fun toggleFavorite(stationId: String) {
        val current = _favorites.value.toMutableSet()
        if (stationId in current) {
            current.remove(stationId)
        } else {
            current.add(stationId)
        }
        _favorites.value = current

        // Update station in list
        val stations = _state.value.stations.map { station ->
            if (station.id == stationId) {
                station.copy(isFavorite = stationId in current)
            } else station
        }
        _state.value = _state.value.copy(
            stations = stations,
            favorites = stations.filter { it.id in current }
        )
    }

    /**
     * Check if station is favorite
     */
    fun isFavorite(stationId: String): Boolean = stationId in _favorites.value

    // ============================================
    // HISTORY
    // ============================================

    private fun addToHistory(station: RadioStation) {
        val entry = ListeningHistoryEntry(
            stationId = station.id,
            stationName = station.name,
            stationType = station.type,
            trackTitle = _nowPlaying.value?.trackTitle
        )

        val history = _history.value.toMutableList()
        // Remove duplicate if exists
        history.removeAll { it.stationId == station.id }
        // Add to front
        history.add(0, entry)
        // Keep last 50
        if (history.size > 50) {
            history.removeAt(history.lastIndex)
        }

        _history.value = history
        _state.value = _state.value.copy(recentlyPlayed = history)
    }

    /**
     * Clear history
     */
    fun clearHistory() {
        _history.value = emptyList()
        _state.value = _state.value.copy(recentlyPlayed = emptyList())
    }

    // ============================================
    // SEARCH
    // ============================================

    /**
     * Search stations
     */
    fun search(query: String) {
        if (query.isBlank()) {
            _state.value = _state.value.copy(searchQuery = "", searchResults = emptyList())
            return
        }

        val results = _state.value.stations.filter { station ->
            station.name.contains(query, ignoreCase = true) ||
            station.description.contains(query, ignoreCase = true) ||
            station.genres.any { it.displayName.contains(query, ignoreCase = true) } ||
            station.tags.any { it.contains(query, ignoreCase = true) }
        }

        _state.value = _state.value.copy(
            searchQuery = query,
            searchResults = results
        )
    }

    // ============================================
    // STATION RETRIEVAL
    // ============================================

    /**
     * Get station by ID
     */
    fun getStation(id: String): RadioStation? {
        return _state.value.stations.find { it.id == id }
    }

    /**
     * Get stations by genre
     */
    fun getStationsByGenre(genre: RadioGenre): List<RadioStation> {
        return _state.value.stations.filter { genre in it.genres }
    }

    /**
     * Get stations by type
     */
    fun getStationsByType(type: StationType): List<RadioStation> {
        return _state.value.stations.filter { it.type == type }
    }

    /**
     * Get stations by category
     */
    fun getStationsByCategory(categoryId: String): List<RadioStation> {
        return when (categoryId) {
            "pop" -> _state.value.stations.filter { RadioGenre.POP in it.genres }
            "rock" -> _state.value.stations.filter { RadioGenre.ROCK in it.genres }
            "electronic" -> _state.value.stations.filter { RadioGenre.ELECTRONIC in it.genres }
            "hiphop" -> _state.value.stations.filter { RadioGenre.HIP_HOP in it.genres }
            "jazz" -> _state.value.stations.filter { RadioGenre.JAZZ in it.genres }
            "classical" -> _state.value.stations.filter { RadioGenre.CLASSICAL in it.genres }
            "news" -> _state.value.stations.filter { RadioGenre.NEWS in it.genres || RadioGenre.TALK in it.genres }
            "chill" -> _state.value.stations.filter { RadioGenre.CHILL in it.genres || RadioGenre.AMBIENT in it.genres }
            "focus" -> _state.value.stations.filter { it.moods.contains(RadioMood.FOCUSED) || it.id == "smart_focus" }
            "workout" -> _state.value.stations.filter { it.moods.contains(RadioMood.ENERGETIC) || it.id == "smart_workout" }
            "sleep" -> _state.value.stations.filter { it.moods.contains(RadioMood.PEACEFUL) || it.id == "smart_sleep" }
            "party" -> _state.value.stations.filter { it.moods.contains(RadioMood.PARTY) }
            "morning" -> defaultSmartStations.filter { it.smartConfig?.energyRange?.start ?: 0f >= 0.3f }
            "evening" -> defaultSmartStations.filter { it.smartConfig?.energyRange?.endInclusive ?: 1f <= 0.5f }
            else -> emptyList()
        }
    }

    // ============================================
    // SLEEP TIMER
    // ============================================

    /**
     * Set sleep timer
     */
    fun setSleepTimer(minutes: Int) {
        cancelSleepTimer()

        if (minutes <= 0) return

        val endTime = System.currentTimeMillis() + (minutes * 60 * 1000L)
        _sleepTimerRemaining.value = minutes * 60 * 1000L

        sleepTimer = Timer()
        sleepTimer?.scheduleAtFixedRate(object : TimerTask() {
            override fun run() {
                val remaining = endTime - System.currentTimeMillis()
                if (remaining <= 0) {
                    scope.launch(Dispatchers.Main) {
                        stop()
                        cancelSleepTimer()
                    }
                } else {
                    _sleepTimerRemaining.value = remaining
                }
            }
        }, 0, 1000)
    }

    /**
     * Cancel sleep timer
     */
    fun cancelSleepTimer() {
        sleepTimer?.cancel()
        sleepTimer = null
        _sleepTimerRemaining.value = null
    }

    // ============================================
    // CLIPPING
    // ============================================

    /**
     * Create a clip from current playback
     */
    fun createClip(
        title: String,
        startOffset: Long = -30000,  // 30 seconds back
        duration: Long = 30000       // 30 second clip
    ): RadioClip? {
        val station = _currentStation.value ?: return null
        val nowPlaying = _nowPlaying.value ?: return null

        if (!station.allowClipping || !nowPlaying.allowClip) return null

        return RadioClip(
            stationId = station.id,
            stationName = station.name,
            title = title,
            startTime = System.currentTimeMillis() + startOffset,
            endTime = System.currentTimeMillis() + startOffset + duration,
            duration = duration,
            trackTitle = nowPlaying.trackTitle,
            artistName = nowPlaying.artistName,
            showName = nowPlaying.showName
        )
    }

    // ============================================
    // AUDIO FOCUS
    // ============================================

    private fun requestAudioFocus(): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val focusRequest = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN)
                .setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                        .build()
                )
                .setOnAudioFocusChangeListener { focus ->
                    when (focus) {
                        AudioManager.AUDIOFOCUS_LOSS -> stop()
                        AudioManager.AUDIOFOCUS_LOSS_TRANSIENT -> pause()
                        AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK -> setVolume(0.3f)
                        AudioManager.AUDIOFOCUS_GAIN -> {
                            setVolume(1f)
                            resume()
                        }
                    }
                }
                .build()

            audioFocusRequest = focusRequest
            val result = audioManager.requestAudioFocus(focusRequest)
            hasAudioFocus = result == AudioManager.AUDIOFOCUS_REQUEST_GRANTED
            return hasAudioFocus
        } else {
            @Suppress("DEPRECATION")
            val result = audioManager.requestAudioFocus(
                { focus ->
                    when (focus) {
                        AudioManager.AUDIOFOCUS_LOSS -> stop()
                        AudioManager.AUDIOFOCUS_LOSS_TRANSIENT -> pause()
                        AudioManager.AUDIOFOCUS_GAIN -> resume()
                    }
                },
                AudioManager.STREAM_MUSIC,
                AudioManager.AUDIOFOCUS_GAIN
            )
            hasAudioFocus = result == AudioManager.AUDIOFOCUS_REQUEST_GRANTED
            return hasAudioFocus
        }
    }

    private fun abandonAudioFocus() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            audioFocusRequest?.let {
                audioManager.abandonAudioFocusRequest(it)
            }
        } else {
            @Suppress("DEPRECATION")
            audioManager.abandonAudioFocus(null)
        }
        hasAudioFocus = false
    }

    // ============================================
    // CLEANUP
    // ============================================

    fun release() {
        stop()
        cancelSleepTimer()
        scope.cancel()
    }
}

