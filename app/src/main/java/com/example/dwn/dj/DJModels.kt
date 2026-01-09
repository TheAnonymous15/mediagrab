package com.example.dwn.dj

import java.util.UUID

// ============================================
// 🎧 DJ STUDIO DATA MODELS
// ============================================

/**
 * DJ Deck - Represents one turntable/deck
 */
data class DJDeck(
    val id: String = UUID.randomUUID().toString(),
    val deckNumber: Int,  // 1 = Left (A), 2 = Right (B)

    // Loaded Track
    val track: DJTrack? = null,
    val isLoaded: Boolean = false,

    // Playback State
    val isPlaying: Boolean = false,
    val currentPosition: Long = 0L,  // ms
    val playbackSpeed: Float = 1.0f,  // 1.0 = normal, 0.5 = half, 2.0 = double

    // Tempo/BPM
    val bpm: Float = 120f,
    val originalBpm: Float = 120f,
    val tempoAdjustment: Float = 0f,  // -8% to +8%
    val keyLock: Boolean = false,  // Maintain pitch when changing tempo

    // Vinyl/Jog
    val jogWheelPosition: Float = 0f,  // 0-360 degrees
    val isScratching: Boolean = false,
    val scratchAmount: Float = 0f,

    // EQ
    val eqHigh: Float = 0f,    // -12dB to +12dB
    val eqMid: Float = 0f,
    val eqLow: Float = 0f,
    val eqKillHigh: Boolean = false,
    val eqKillMid: Boolean = false,
    val eqKillLow: Boolean = false,

    // Volume & Gain
    val volume: Float = 1f,    // 0 to 1
    val gain: Float = 0f,      // -12dB to +12dB
    val isMuted: Boolean = false,

    // Filter
    val filterCutoff: Float = 0.5f,  // 0 = LPF, 0.5 = off, 1 = HPF
    val filterResonance: Float = 0f,

    // Cue Points
    val cuePoints: List<CuePoint> = emptyList(),
    val hotCues: List<HotCue> = listOf(
        HotCue(1), HotCue(2), HotCue(3), HotCue(4),
        HotCue(5), HotCue(6), HotCue(7), HotCue(8)
    ),

    // Loop
    val loopEnabled: Boolean = false,
    val loopStart: Long = 0L,
    val loopEnd: Long = 0L,
    val loopBars: Int = 4,  // 1, 2, 4, 8, 16, 32

    // Sync
    val syncEnabled: Boolean = false,
    val isMaster: Boolean = false,

    // Visual
    val waveformData: List<Float> = emptyList(),
    val peakLevel: Float = 0f
)

/**
 * Track loaded in DJ deck
 */
data class DJTrack(
    val id: String = UUID.randomUUID().toString(),
    val title: String,
    val artist: String,
    val album: String = "",
    val duration: Long,  // ms
    val uri: String,
    val artworkUri: String? = null,

    // Analysis
    val bpm: Float = 120f,
    val key: String = "Am",  // Musical key
    val energy: Float = 0.7f,  // 0-1
    val waveform: List<Float> = emptyList(),
    val beatGrid: List<Long> = emptyList(),  // Beat positions in ms

    // Metadata
    val genre: String = "",
    val year: Int? = null,
    val bitrate: Int = 320
)

/**
 * Cue point marker
 */
data class CuePoint(
    val id: String = UUID.randomUUID().toString(),
    val position: Long,  // ms
    val label: String = "",
    val color: Long = 0xFFE91E63
)

/**
 * Hot cue pad
 */
data class HotCue(
    val number: Int,
    val position: Long? = null,
    val label: String = "",
    val color: Long = 0xFFE91E63,
    val isSet: Boolean = false
)

// ============================================
// 🎛️ MIXER
// ============================================

/**
 * DJ Mixer - Central mixing unit
 */
data class DJMixer(
    // Crossfader
    val crossfaderPosition: Float = 0.5f,  // 0 = Deck A, 0.5 = Center, 1 = Deck B
    val crossfaderCurve: CrossfaderCurve = CrossfaderCurve.SMOOTH,

    // Master
    val masterVolume: Float = 0.8f,
    val masterLimiter: Boolean = true,
    val masterPeakL: Float = 0f,
    val masterPeakR: Float = 0f,

    // Booth
    val boothVolume: Float = 0.5f,

    // Headphones
    val headphoneVolume: Float = 0.7f,
    val headphoneMix: Float = 0.5f,  // 0 = Cue, 1 = Master
    val headphoneCueDeckA: Boolean = false,
    val headphoneCueDeckB: Boolean = false,

    // Effects Send
    val fxSendDeckA: Float = 0f,
    val fxSendDeckB: Float = 0f
)

enum class CrossfaderCurve {
    SMOOTH,      // Gradual transition
    SHARP,       // Quick cut
    SCRATCH      // For scratching
}

// ============================================
// 🎚️ EFFECTS
// ============================================

/**
 * DJ Effect unit
 */
data class DJEffect(
    val id: String = UUID.randomUUID().toString(),
    val type: EffectType,
    val isEnabled: Boolean = false,
    val wetDry: Float = 0.5f,  // 0 = dry, 1 = wet
    val parameter1: Float = 0.5f,
    val parameter2: Float = 0.5f,
    val parameter3: Float = 0.5f,
    val beatSync: Boolean = true,
    val beatDivision: Int = 4  // 1/4, 1/2, 1, 2, 4 beats
)

enum class EffectType(val displayName: String, val icon: String) {
    ECHO("Echo", "🔊"),
    DELAY("Delay", "⏱️"),
    REVERB("Reverb", "🌊"),
    FLANGER("Flanger", "🌀"),
    PHASER("Phaser", "〰️"),
    FILTER("Filter", "🎚️"),
    BITCRUSHER("Bitcrush", "👾"),
    GATE("Gate", "🚪"),
    STUTTER("Stutter", "⚡"),
    BRAKE("Brake", "🛑"),
    BACKSPIN("Backspin", "🔄"),
    ROLL("Roll", "🥁"),
    PITCH_SHIFT("Pitch", "🎵"),
    NOISE("Noise", "📻"),
    SLICER("Slicer", "✂️"),
    TRANS("Trans", "🔀")
}

/**
 * Effect bank (group of 3 effects)
 */
data class EffectBank(
    val id: Int,  // 1 or 2
    val effects: List<DJEffect> = listOf(
        DJEffect(type = EffectType.ECHO),
        DJEffect(type = EffectType.FLANGER),
        DJEffect(type = EffectType.REVERB)
    ),
    val activeEffectIndex: Int = 0,
    val isEnabled: Boolean = false,
    val assignedToDeckA: Boolean = true,
    val assignedToDeckB: Boolean = false
)

// ============================================
// 🎵 SAMPLER
// ============================================

/**
 * Sample pad
 */
data class SamplePad(
    val id: Int,
    val name: String = "",
    val uri: String? = null,
    val isLoaded: Boolean = false,
    val isPlaying: Boolean = false,
    val volume: Float = 1f,
    val color: Long = 0xFFE91E63,
    val playMode: SamplePlayMode = SamplePlayMode.ONE_SHOT,
    val quantize: Boolean = true
)

enum class SamplePlayMode {
    ONE_SHOT,    // Play once
    LOOP,        // Loop continuously
    GATE         // Play while held
}

/**
 * Sampler unit
 */
data class DJSampler(
    val pads: List<SamplePad> = (1..16).map { SamplePad(id = it) },
    val volume: Float = 0.8f,
    val isMuted: Boolean = false
)

// ============================================
// 📚 LIBRARY / BROWSER
// ============================================

/**
 * Track in library
 */
data class LibraryTrack(
    val id: String = UUID.randomUUID().toString(),
    val title: String,
    val artist: String,
    val album: String = "",
    val duration: Long,
    val uri: String,
    val bpm: Float? = null,
    val key: String? = null,
    val genre: String = "",
    val artworkUri: String? = null,
    val isAnalyzed: Boolean = false,
    val playCount: Int = 0,
    val lastPlayed: Long? = null,
    val rating: Int = 0,  // 0-5 stars
    val color: Long? = null
)

/**
 * Playlist/Crate
 */
data class DJPlaylist(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val tracks: List<LibraryTrack> = emptyList(),
    val isSmartPlaylist: Boolean = false,
    val color: Long = 0xFFE91E63
)

// ============================================
// 🎯 PERFORMANCE MODES
// ============================================

enum class PerformanceMode(val displayName: String) {
    HOT_CUE("Hot Cue"),
    LOOP("Loop"),
    SLICER("Slicer"),
    SAMPLER("Sampler"),
    ROLL("Roll"),
    PAD_FX("Pad FX")
}

// ============================================
// 📊 DJ STUDIO STATE
// ============================================

data class DJStudioState(
    // Decks
    val deckA: DJDeck = DJDeck(deckNumber = 1),
    val deckB: DJDeck = DJDeck(deckNumber = 2),

    // Mixer
    val mixer: DJMixer = DJMixer(),

    // Effects
    val effectBank1: EffectBank = EffectBank(id = 1),
    val effectBank2: EffectBank = EffectBank(id = 2),

    // Sampler
    val sampler: DJSampler = DJSampler(),

    // Library
    val library: List<LibraryTrack> = emptyList(),
    val playlists: List<DJPlaylist> = emptyList(),
    val history: List<LibraryTrack> = emptyList(),

    // Performance
    val performanceMode: PerformanceMode = PerformanceMode.HOT_CUE,

    // Recording
    val isRecording: Boolean = false,
    val recordingDuration: Long = 0L,
    val recordingUri: String? = null,

    // UI State
    val selectedDeck: Int = 1,  // 1 or 2
    val showLibrary: Boolean = false,
    val showEffects: Boolean = false,
    val showSampler: Boolean = false,
    val isFullscreen: Boolean = false,

    // Sync
    val autoSync: Boolean = true,
    val masterDeck: Int = 1
)

// ============================================
// 🎨 DJ THEME COLORS
// ============================================

object DJColors {
    // Deck colors
    val deckA = 0xFF00D4FF  // Cyan
    val deckB = 0xFFFF6B00  // Orange

    // UI
    val playGreen = 0xFF00FF00
    val cueOrange = 0xFFFF8C00
    val stopRed = 0xFFFF0000
    val syncPurple = 0xFF9C27B0

    // Hot cue colors
    val hotCueColors = listOf(
        0xFFE91E63L,  // Pink
        0xFFFF5722L,  // Orange
        0xFFFFC107L,  // Yellow
        0xFF4CAF50L,  // Green
        0xFF00BCD4L,  // Cyan
        0xFF2196F3L,  // Blue
        0xFF9C27B0L,  // Purple
        0xFF607D8BL   // Grey
    )
}

// ============================================
// 🎵 SAMPLE LIBRARY DATA
// ============================================

val defaultDJTracks = listOf(
    LibraryTrack(
        title = "Summer Vibes",
        artist = "DJ Producer",
        duration = 240000,
        uri = "content://media/audio/1",
        bpm = 128f,
        key = "Am",
        genre = "House"
    ),
    LibraryTrack(
        title = "Night Drive",
        artist = "Synth Master",
        duration = 195000,
        uri = "content://media/audio/2",
        bpm = 124f,
        key = "Fm",
        genre = "Synthwave"
    ),
    LibraryTrack(
        title = "Bass Drop",
        artist = "Heavy Beats",
        duration = 210000,
        uri = "content://media/audio/3",
        bpm = 140f,
        key = "Em",
        genre = "Dubstep"
    ),
    LibraryTrack(
        title = "Groove Machine",
        artist = "Funk Factory",
        duration = 185000,
        uri = "content://media/audio/4",
        bpm = 118f,
        key = "Gm",
        genre = "Tech House"
    ),
    LibraryTrack(
        title = "Midnight Run",
        artist = "Dark Beats",
        duration = 220000,
        uri = "content://media/audio/5",
        bpm = 132f,
        key = "Cm",
        genre = "Techno"
    ),
    LibraryTrack(
        title = "Paradise",
        artist = "Tropical Sound",
        duration = 205000,
        uri = "content://media/audio/6",
        bpm = 122f,
        key = "Dm",
        genre = "Tropical House"
    )
)

