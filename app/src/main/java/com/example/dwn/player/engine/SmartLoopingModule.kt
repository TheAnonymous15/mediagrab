package com.example.dwn.player.engine

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * ============================================
 * SMART LOOPING MODULE
 * ============================================
 *
 * Smart navigation and looping:
 * - Loop by time
 * - Loop by beats
 * - Loop by bars
 * - Bookmark points
 * - Auto-detected jump markers (intro, verse, chorus, drop, outro)
 */
class SmartLoopingModule {

    private val _loopState = MutableStateFlow(LoopState())
    val loopState: StateFlow<LoopState> = _loopState.asStateFlow()

    private val bookmarks = mutableListOf<Bookmark>()
    private val _bookmarksState = MutableStateFlow<List<Bookmark>>(emptyList())
    val bookmarksState: StateFlow<List<Bookmark>> = _bookmarksState.asStateFlow()

    private val jumpMarkers = mutableListOf<JumpMarker>()
    private val _jumpMarkersState = MutableStateFlow<List<JumpMarker>>(emptyList())
    val jumpMarkersState: StateFlow<List<JumpMarker>> = _jumpMarkersState.asStateFlow()

    // Callback for looping
    var onLoopBoundary: ((shouldLoop: Boolean, loopStartMs: Long) -> Unit)? = null

    // Audio analysis reference for beat/bar looping
    private var analysisModule: AudioAnalysisModule? = null

    fun setAnalysisModule(module: AudioAnalysisModule) {
        this.analysisModule = module
    }

    // ============================================
    // TIME-BASED LOOPING
    // ============================================

    /**
     * Set loop region by time
     */
    fun setLoopByTime(startMs: Long, endMs: Long) {
        _loopState.value = LoopState(
            isEnabled = true,
            startMs = startMs,
            endMs = endMs,
            loopType = LoopType.TIME,
            beats = 0,
            bars = 0
        )
    }

    /**
     * Enable/disable loop
     */
    fun setLoopEnabled(enabled: Boolean) {
        _loopState.value = _loopState.value.copy(isEnabled = enabled)
    }

    /**
     * Toggle loop on/off
     */
    fun toggleLoop() {
        setLoopEnabled(!_loopState.value.isEnabled)
    }

    /**
     * Clear loop
     */
    fun clearLoop() {
        _loopState.value = LoopState()
    }

    // ============================================
    // BEAT-BASED LOOPING
    // ============================================

    /**
     * Set loop by number of beats from current position
     */
    fun setLoopByBeats(beats: Int, currentPositionMs: Long) {
        val beatDuration = analysisModule?.getBeatDurationMs() ?: return
        if (beatDuration <= 0) return

        val startMs = currentPositionMs
        val endMs = startMs + (beats * beatDuration)

        _loopState.value = LoopState(
            isEnabled = true,
            startMs = startMs,
            endMs = endMs,
            loopType = LoopType.BEATS,
            beats = beats,
            bars = 0
        )
    }

    /**
     * Set loop by number of bars from current position
     */
    fun setLoopByBars(bars: Int, currentPositionMs: Long) {
        val barDuration = analysisModule?.getBarDurationMs() ?: return
        if (barDuration <= 0) return

        val startMs = currentPositionMs
        val endMs = startMs + (bars * barDuration)

        _loopState.value = LoopState(
            isEnabled = true,
            startMs = startMs,
            endMs = endMs,
            loopType = LoopType.BARS,
            beats = bars * 4,
            bars = bars
        )
    }

    /**
     * Snap loop to nearest beat boundaries
     */
    fun snapLoopToBeats() {
        analysisModule?.let { analysis ->
            val state = _loopState.value
            if (!state.isEnabled) return

            val snappedStart = analysis.snapToBeat(state.startMs)
            val snappedEnd = analysis.snapToBeat(state.endMs)

            _loopState.value = state.copy(
                startMs = snappedStart,
                endMs = snappedEnd
            )
        }
    }

    // ============================================
    // LOOP CHECK
    // ============================================

    /**
     * Check if playback should loop at current position
     * Call this from playback progress callback
     */
    fun checkLoop(currentPositionMs: Long): Boolean {
        val state = _loopState.value
        if (!state.isEnabled) return false

        if (currentPositionMs >= state.endMs) {
            onLoopBoundary?.invoke(true, state.startMs)
            return true
        }
        return false
    }

    /**
     * Get loop start position
     */
    fun getLoopStart(): Long = _loopState.value.startMs

    /**
     * Get loop end position
     */
    fun getLoopEnd(): Long = _loopState.value.endMs

    /**
     * Get loop duration
     */
    fun getLoopDuration(): Long {
        val state = _loopState.value
        return state.endMs - state.startMs
    }

    // ============================================
    // BOOKMARKS
    // ============================================

    /**
     * Add a bookmark at position
     */
    fun addBookmark(positionMs: Long, name: String = "", color: Int = 0xFFFFFFFF.toInt()): Bookmark {
        val bookmark = Bookmark(
            id = System.currentTimeMillis().toString(),
            position = positionMs,
            name = name.ifEmpty { "Bookmark ${bookmarks.size + 1}" },
            color = color
        )
        bookmarks.add(bookmark)
        bookmarks.sortBy { it.position }
        updateBookmarksState()
        return bookmark
    }

    /**
     * Remove a bookmark
     */
    fun removeBookmark(bookmarkId: String) {
        bookmarks.removeAll { it.id == bookmarkId }
        updateBookmarksState()
    }

    /**
     * Update a bookmark
     */
    fun updateBookmark(bookmarkId: String, name: String? = null, color: Int? = null) {
        val index = bookmarks.indexOfFirst { it.id == bookmarkId }
        if (index >= 0) {
            val old = bookmarks[index]
            bookmarks[index] = old.copy(
                name = name ?: old.name,
                color = color ?: old.color
            )
            updateBookmarksState()
        }
    }

    /**
     * Clear all bookmarks
     */
    fun clearBookmarks() {
        bookmarks.clear()
        updateBookmarksState()
    }

    /**
     * Get all bookmarks
     */
    fun getBookmarks(): List<Bookmark> = bookmarks.toList()

    /**
     * Get bookmark by ID
     */
    fun getBookmark(bookmarkId: String): Bookmark? = bookmarks.find { it.id == bookmarkId }

    /**
     * Get nearest bookmark to position
     */
    fun getNearestBookmark(positionMs: Long): Bookmark? {
        return bookmarks.minByOrNull { kotlin.math.abs(it.position - positionMs) }
    }

    /**
     * Get next bookmark after position
     */
    fun getNextBookmark(positionMs: Long): Bookmark? {
        return bookmarks.firstOrNull { it.position > positionMs }
    }

    /**
     * Get previous bookmark before position
     */
    fun getPreviousBookmark(positionMs: Long): Bookmark? {
        return bookmarks.lastOrNull { it.position < positionMs }
    }

    private fun updateBookmarksState() {
        _bookmarksState.value = bookmarks.toList()
    }

    // ============================================
    // JUMP MARKERS (Auto-detected)
    // ============================================

    /**
     * Detect jump markers from audio analysis
     */
    fun detectJumpMarkers(durationMs: Long, energyProfile: List<Float>) {
        jumpMarkers.clear()

        if (energyProfile.isEmpty()) return

        val avgEnergy = energyProfile.average()
        val segments = energyProfile.size

        // Detect intro (low energy at start)
        var introEnd = 0
        for (i in 0 until minOf(segments / 4, energyProfile.size)) {
            if (energyProfile[i] > avgEnergy * 0.8) {
                introEnd = i
                break
            }
        }
        if (introEnd > 2) {
            jumpMarkers.add(JumpMarker(0, MarkerType.INTRO, "Intro"))
        }

        // Detect drops and choruses (high energy regions)
        var wasLow = true
        for (i in 1 until segments) {
            val timeMs = (durationMs * i / segments)
            val energy = energyProfile[i]
            val prevEnergy = energyProfile[i - 1]

            // Significant energy increase = drop or chorus
            if (wasLow && energy > avgEnergy * 1.3 && energy - prevEnergy > avgEnergy * 0.3) {
                val type = if (jumpMarkers.count { it.type == MarkerType.DROP } < 2)
                    MarkerType.DROP else MarkerType.CHORUS
                jumpMarkers.add(JumpMarker(timeMs, type, type.name.lowercase().replaceFirstChar { it.uppercase() }))
                wasLow = false
            } else if (!wasLow && energy < avgEnergy * 0.7) {
                wasLow = true
            }
        }

        // Detect outro (low energy at end)
        var outroStart = segments
        for (i in (segments * 3 / 4) until segments) {
            if (energyProfile[i] < avgEnergy * 0.5) {
                outroStart = i
                break
            }
        }
        if (outroStart < segments - 2) {
            val timeMs = (durationMs * outroStart / segments)
            jumpMarkers.add(JumpMarker(timeMs, MarkerType.OUTRO, "Outro"))
        }

        jumpMarkers.sortBy { it.timeMs }
        updateJumpMarkersState()
    }

    /**
     * Add custom jump marker
     */
    fun addJumpMarker(timeMs: Long, type: MarkerType, label: String = "") {
        jumpMarkers.add(JumpMarker(timeMs, type, label))
        jumpMarkers.sortBy { it.timeMs }
        updateJumpMarkersState()
    }

    /**
     * Remove jump marker at time
     */
    fun removeJumpMarker(timeMs: Long) {
        jumpMarkers.removeAll { it.timeMs == timeMs }
        updateJumpMarkersState()
    }

    /**
     * Clear all jump markers
     */
    fun clearJumpMarkers() {
        jumpMarkers.clear()
        updateJumpMarkersState()
    }

    /**
     * Get all jump markers
     */
    fun getJumpMarkers(): List<JumpMarker> = jumpMarkers.toList()

    /**
     * Get jump markers by type
     */
    fun getJumpMarkersByType(type: MarkerType): List<JumpMarker> {
        return jumpMarkers.filter { it.type == type }
    }

    /**
     * Get nearest jump marker to position
     */
    fun getNearestJumpMarker(positionMs: Long): JumpMarker? {
        return jumpMarkers.minByOrNull { kotlin.math.abs(it.timeMs - positionMs) }
    }

    /**
     * Get next jump marker after position
     */
    fun getNextJumpMarker(positionMs: Long): JumpMarker? {
        return jumpMarkers.firstOrNull { it.timeMs > positionMs }
    }

    /**
     * Get previous jump marker before position
     */
    fun getPreviousJumpMarker(positionMs: Long): JumpMarker? {
        return jumpMarkers.lastOrNull { it.timeMs < positionMs }
    }

    private fun updateJumpMarkersState() {
        _jumpMarkersState.value = jumpMarkers.toList()
    }

    // ============================================
    // LOOP BETWEEN MARKERS
    // ============================================

    /**
     * Set loop between two jump markers
     */
    fun setLoopBetweenMarkers(startMarker: JumpMarker, endMarker: JumpMarker) {
        setLoopByTime(startMarker.timeMs, endMarker.timeMs)
    }

    /**
     * Set loop from current position to next jump marker
     */
    fun setLoopToNextMarker(currentPositionMs: Long) {
        val nextMarker = getNextJumpMarker(currentPositionMs) ?: return
        setLoopByTime(currentPositionMs, nextMarker.timeMs)
    }

    // ============================================
    // CLEANUP
    // ============================================

    fun release() {
        clearLoop()
        clearBookmarks()
        clearJumpMarkers()
    }
}

