package com.example.dwn.player.engine

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.media.AudioManager
import android.os.Build
import kotlinx.coroutines.flow.StateFlow
import java.util.Calendar

/**
 * ============================================
 * UNIFIED MEDIA PLAYER ENGINE
 * ============================================
 *
 * Next-Gen Media Player that brings together all modules:
 * - Core Playback Engine
 * - Audio FX Module
 * - Multi-Layer Audio Module
 * - FX Automation Module
 * - Audio Analysis Module
 * - Smart Looping Module
 * - Output Routing Module
 * - Listening Mode Module
 * - Timeline Graph Module
 *
 * This is the main entry point for the player system.
 * All platform features plug into this player:
 * - Radio
 * - Podcast
 * - Remix Studio
 * - Beat Maker
 * - Artist tools
 */
class UnifiedMediaPlayer private constructor(private val context: Context) {

    companion object {
        @Volatile
        private var INSTANCE: UnifiedMediaPlayer? = null

        fun getInstance(context: Context): UnifiedMediaPlayer {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: UnifiedMediaPlayer(context.applicationContext).also { INSTANCE = it }
            }
        }
    }

    // ============================================
    // MODULES
    // ============================================

    val core = CorePlaybackEngine(context)
    val fx = AudioFXModule()
    val multiLayer = MultiLayerAudioModule(context)
    val automation = FXAutomationModule()
    val analysis = AudioAnalysisModule()
    val looping = SmartLoopingModule()
    val output = OutputRoutingModule(context)
    val listeningMode = ListeningModeModule()
    val timeline = TimelineGraphModule()

    // ============================================
    // MODULE ENABLED STATE
    // ============================================

    private val enabledModules = mutableSetOf(PlayerModule.CORE)

    // ============================================
    // STATE ACCESSORS
    // ============================================

    val playbackState: StateFlow<PlaybackState> get() = core.playbackState
    val currentMedia: StateFlow<MediaItem?> get() = core.currentMedia
    val playlist: StateFlow<List<MediaItem>> get() = core.playlist
    val fxState: StateFlow<AudioFXState> get() = fx.fxState
    val audioLayersState: StateFlow<List<AudioLayerState>> get() = multiLayer.layersState
    val automationState: StateFlow<Map<String, List<AutomationPoint>>> get() = automation.automationState
    val analysisState: StateFlow<AudioAnalysis?> get() = analysis.analysisState
    val loopState: StateFlow<LoopState> get() = looping.loopState
    val bookmarksState: StateFlow<List<Bookmark>> get() = looping.bookmarksState
    val jumpMarkersState: StateFlow<List<JumpMarker>> get() = looping.jumpMarkersState
    val currentOutput: StateFlow<OutputType> get() = output.currentOutput
    val currentOutputProfile: StateFlow<OutputProfile?> get() = output.currentProfile
    val currentListeningMode: StateFlow<ListeningMode> get() = listeningMode.currentMode
    val timelineGraphData: StateFlow<TimelineGraphData?> get() = timeline.graphData

    // ============================================
    // INITIALIZATION
    // ============================================

    init {
        setupModuleConnections()
        registerReceivers()
    }

    private fun setupModuleConnections() {
        // Connect analysis to looping for beat-based features
        looping.setAnalysisModule(analysis)

        // Connect analysis and looping to timeline
        timeline.setAnalysisModule(analysis)
        timeline.setLoopingModule(looping)

        // Core engine callbacks
        core.onPrepared = { audioSessionId ->
            // Initialize FX with audio session
            if (isModuleEnabled(PlayerModule.FX) || isModuleEnabled(PlayerModule.EQUALIZER)) {
                fx.initialize(audioSessionId)
            }

            // Trigger audio analysis
            if (isModuleEnabled(PlayerModule.ANALYSIS)) {
                val duration = core.playbackState.value.duration
                analysis.analyze(duration)
            }
        }

        core.onPositionUpdate = { positionMs ->
            // Check loop boundaries
            if (isModuleEnabled(PlayerModule.LOOPING)) {
                if (looping.checkLoop(positionMs)) {
                    core.seekTo(looping.getLoopStart())
                }
            }

            // Apply FX automation
            if (isModuleEnabled(PlayerModule.AUTOMATION)) {
                applyAutomationAtPosition(positionMs)
            }
        }

        // Output change callback
        output.onOutputChanged = { outputType, profile ->
            profile?.let { applyOutputProfile(it) }

            // Auto-apply listening mode based on output
            if (listeningMode.isAutoModeEnabled.value) {
                val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
                listeningMode.autoApplyMode(
                    hour = hour,
                    isHeadphonesConnected = outputType == OutputType.HEADPHONES,
                    isBluetoothConnected = outputType == OutputType.BLUETOOTH
                )
            }
        }

        // Listening mode change callback
        listeningMode.onModeChanged = { mode, settings ->
            applyModeSettings(settings)
        }

        // Analysis complete callback
        analysis.onAnalysisComplete = { analysisResult ->
            // Generate timeline graph
            if (isModuleEnabled(PlayerModule.TIMELINE)) {
                val duration = core.playbackState.value.duration
                timeline.generateGraph(duration)
            }

            // Detect jump markers
            looping.detectJumpMarkers(
                durationMs = core.playbackState.value.duration,
                energyProfile = analysisResult.energyProfile
            )

            // Notify listeners
            core.notifyListeners { it.onAudioAnalysisReady(analysisResult) }
        }

        // Looping callback
        looping.onLoopBoundary = { shouldLoop, loopStartMs ->
            if (shouldLoop) {
                core.seekTo(loopStartMs)
            }
        }
    }

    private fun registerReceivers() {
        // Audio becoming noisy (headphones unplugged)
        val noisyFilter = IntentFilter(AudioManager.ACTION_AUDIO_BECOMING_NOISY)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            context.registerReceiver(audioNoisyReceiver, noisyFilter, Context.RECEIVER_NOT_EXPORTED)
        } else {
            context.registerReceiver(audioNoisyReceiver, noisyFilter)
        }
    }

    private val audioNoisyReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action == AudioManager.ACTION_AUDIO_BECOMING_NOISY) {
                core.pause()
                output.detectOutput()
            }
        }
    }

    // ============================================
    // MODULE MANAGEMENT
    // ============================================

    fun enableModule(module: PlayerModule) {
        enabledModules.add(module)

        when (module) {
            PlayerModule.FX, PlayerModule.EQUALIZER -> {
                val sessionId = core.getAudioSessionId()
                if (sessionId != 0) {
                    fx.initialize(sessionId)
                }
            }
            PlayerModule.ANALYSIS -> {
                val duration = core.playbackState.value.duration
                if (duration > 0) {
                    analysis.analyze(duration)
                }
            }
            PlayerModule.TIMELINE -> {
                val duration = core.playbackState.value.duration
                if (duration > 0 && analysis.analysisState.value != null) {
                    timeline.generateGraph(duration)
                }
            }
            else -> { }
        }
    }

    fun disableModule(module: PlayerModule) {
        if (module == PlayerModule.CORE) return // Core cannot be disabled

        enabledModules.remove(module)

        when (module) {
            PlayerModule.FX, PlayerModule.EQUALIZER -> fx.release()
            PlayerModule.MULTI_LAYER -> multiLayer.release()
            PlayerModule.AUTOMATION -> automation.clearAll()
            PlayerModule.ANALYSIS -> analysis.clearAnalysis()
            PlayerModule.LOOPING -> looping.release()
            PlayerModule.TIMELINE -> timeline.clearGraph()
            else -> { }
        }
    }

    fun isModuleEnabled(module: PlayerModule): Boolean = module in enabledModules

    fun getEnabledModules(): Set<PlayerModule> = enabledModules.toSet()

    // ============================================
    // PLAYBACK SHORTCUTS
    // ============================================

    fun play(media: MediaItem) = core.play(media)
    fun playUrl(url: String, title: String = "Stream", artist: String = "") = core.playUrl(url, title, artist)
    fun playFile(filePath: String) = core.playFile(filePath)
    fun playPlaylist(items: List<MediaItem>, startIndex: Int = 0) = core.playPlaylist(items, startIndex)
    fun resume() = core.resume()
    fun pause() = core.pause()
    fun togglePlayPause() = core.togglePlayPause()
    fun stop() = core.stop()
    fun seekTo(positionMs: Long) = core.seekTo(positionMs)
    fun seekToPercent(percent: Float) = core.seekToPercent(percent)
    fun next() = core.next()
    fun previous() = core.previous()
    fun setSpeed(speed: Float) = core.setSpeed(speed)
    fun setVolume(volume: Float) = core.setVolume(volume)
    fun setRepeatMode(mode: RepeatMode) = core.setRepeatMode(mode)
    fun toggleShuffle() = core.toggleShuffle()
    fun isPlaying(): Boolean = core.isPlaying()

    // ============================================
    // LISTENER MANAGEMENT
    // ============================================

    fun addListener(listener: PlayerListener) = core.addListener(listener)
    fun removeListener(listener: PlayerListener) = core.removeListener(listener)

    // ============================================
    // FX AUTOMATION APPLICATION
    // ============================================

    private fun applyAutomationAtPosition(positionMs: Long) {
        val values = automation.getAllValuesAt(positionMs)

        values.forEach { (param, value) ->
            when (param) {
                FXAutomationModule.PARAM_VOLUME -> core.setVolume(value)
                FXAutomationModule.PARAM_BASS_BOOST -> fx.setBassBoostStrength(value.toInt())
                FXAutomationModule.PARAM_VIRTUALIZER -> fx.setVirtualizerStrength(value.toInt())
                FXAutomationModule.PARAM_LOUDNESS -> fx.setLoudnessGain(value.toInt())
                FXAutomationModule.PARAM_REVERB -> fx.setReverbPreset(value.toInt())
            }
        }
    }

    // ============================================
    // OUTPUT PROFILE APPLICATION
    // ============================================

    private fun applyOutputProfile(profile: OutputProfile) {
        fx.toggleEqualizer(profile.eqEnabled)
        if (profile.eqBandLevels.isNotEmpty()) {
            profile.eqBandLevels.forEachIndexed { index, level ->
                fx.setEqualizerBand(index, level)
            }
        }
        fx.setBassBoostStrength(profile.bassBoost)
        fx.setVirtualizerStrength(profile.virtualizer)
        fx.setLoudnessGain(profile.loudnessGain)
        fx.setReverbPreset(profile.reverbPreset)
    }

    // ============================================
    // MODE SETTINGS APPLICATION
    // ============================================

    private fun applyModeSettings(settings: ModeSettings) {
        if (settings.disableAllFX) {
            fx.disableAllFX()
        } else {
            fx.setBassBoostStrength(settings.bassBoost)
            fx.setVirtualizerStrength(settings.virtualizer)
            fx.setLoudnessGain(settings.loudnessGain)
        }
    }

    // ============================================
    // TIMELINE GRAPH SEEK
    // ============================================

    fun seekToGraphPosition(tapPercent: Float) {
        val timeMs = timeline.tapToTimeMs(tapPercent)
        core.seekTo(timeMs)
    }

    // ============================================
    // SMART FEATURES
    // ============================================

    /**
     * Skip to next silence region (smart skipping)
     */
    fun skipToNextSilence() {
        val nextSilence = analysis.getNextSilence(core.playbackState.value.position)
        nextSilence?.let { core.seekTo(it.first) }
    }

    /**
     * Skip current silence
     */
    fun skipSilence() {
        val position = core.playbackState.value.position
        if (analysis.isInSilence(position)) {
            val silences = analysis.getSilenceRegions()
            val currentSilence = silences.find { position in it.first..it.last }
            currentSilence?.let { core.seekTo(it.last + 100) }
        }
    }

    /**
     * Jump to next scene change
     */
    fun jumpToNextScene() {
        val nextScene = analysis.getNextSceneChange(core.playbackState.value.position)
        nextScene?.let { core.seekTo(it) }
    }

    /**
     * Jump to specific marker
     */
    fun jumpToMarker(marker: JumpMarker) {
        core.seekTo(marker.timeMs)
    }

    /**
     * Jump to bookmark
     */
    fun jumpToBookmark(bookmark: Bookmark) {
        core.seekTo(bookmark.position)
    }

    /**
     * Snap current position to nearest beat
     */
    fun snapToBeat() {
        val position = core.playbackState.value.position
        val snappedPosition = analysis.snapToBeat(position)
        core.seekTo(snappedPosition)
    }

    // ============================================
    // CLEANUP
    // ============================================

    fun release() {
        try {
            context.unregisterReceiver(audioNoisyReceiver)
        } catch (e: Exception) { }

        core.release()
        fx.release()
        multiLayer.release()
        analysis.release()
        looping.release()
        timeline.release()

        INSTANCE = null
    }
}

