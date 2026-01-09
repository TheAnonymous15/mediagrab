package com.example.dwn.player.audio

import android.content.Context
import android.media.audiofx.BassBoost
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * PRESETS & DATA - Feature Set 10
 * SPECIAL MODES - Feature Set 12
 *
 * - Factory presets (genre, device, use-case)
 * - User presets
 * - Import / export (JSON)
 * - Special modes: Podcast, Gaming, Call, Karaoke
 */

// ============================================
// PRESET SYSTEM
// ============================================

data class AudioPreset(
    val id: String,
    val name: String,
    val category: PresetCategory,
    val isFactory: Boolean = true,
    val isCustom: Boolean = false,
    val isFavorite: Boolean = false,

    // EQ Settings
    val eqBands: Map<Int, Float> = emptyMap(),  // frequency -> gain
    val preampGain: Float = 0f,

    // Effects Settings
    val bassBoost: Int = 0,
    val virtualizer: Int = 0,
    val reverbPreset: String = "None",

    // Dynamics
    val compressorEnabled: Boolean = false,
    val limiterEnabled: Boolean = false,

    // Spatial
    val stereoWidth: Float = 1f,

    // Special mode
    val specialMode: SpecialMode? = null
)

enum class PresetCategory(val label: String, val icon: String) {
    GENRE("Genre", "🎵"),
    DEVICE("Device", "🎧"),
    USE_CASE("Use Case", "🎯"),
    CUSTOM("Custom", "✨"),
    FAVORITE("Favorites", "⭐")
}

enum class SpecialMode(val label: String, val description: String) {
    PODCAST("Podcast Mode", "Optimized for speech clarity"),
    GAMING("Gaming Mode", "Enhanced footsteps and spatial audio"),
    CALL("Call Clarity", "Clear voice for calls"),
    KARAOKE("Karaoke", "Vocal removal/isolation"),
    MUSIC_MASTERING("Music Mastering", "Balanced for music production"),
    NIGHT_MODE("Night Mode", "Compressed dynamics for quiet listening"),
    WORKOUT("Workout", "Bass boost for exercise"),
    MEDITATION("Meditation", "Calm, balanced sound"),
    SLEEP("Sleep", "Gentle, relaxing frequencies")
}

class PresetManager(private val context: Context) {

    private val prefs = context.getSharedPreferences("audio_presets", Context.MODE_PRIVATE)

    private val _presets = MutableStateFlow<List<AudioPreset>>(emptyList())
    val presets: StateFlow<List<AudioPreset>> = _presets.asStateFlow()

    private val _currentPreset = MutableStateFlow<AudioPreset?>(null)
    val currentPreset: StateFlow<AudioPreset?> = _currentPreset.asStateFlow()

    // Factory presets
    val factoryPresets = listOf(
        // Genre presets
        AudioPreset(
            id = "flat",
            name = "Flat",
            category = PresetCategory.GENRE,
            eqBands = mapOf(60 to 0f, 230 to 0f, 910 to 0f, 3600 to 0f, 14000 to 0f)
        ),
        AudioPreset(
            id = "bass_boost",
            name = "Bass Boost",
            category = PresetCategory.GENRE,
            eqBands = mapOf(60 to 6f, 230 to 4f, 910 to 0f, 3600 to 0f, 14000 to 0f),
            bassBoost = 500
        ),
        AudioPreset(
            id = "treble_boost",
            name = "Treble Boost",
            category = PresetCategory.GENRE,
            eqBands = mapOf(60 to 0f, 230 to 0f, 910 to 0f, 3600 to 4f, 14000 to 6f)
        ),
        AudioPreset(
            id = "rock",
            name = "Rock",
            category = PresetCategory.GENRE,
            eqBands = mapOf(60 to 5f, 230 to 3f, 910 to 0f, 3600 to 3f, 14000 to 5f)
        ),
        AudioPreset(
            id = "pop",
            name = "Pop",
            category = PresetCategory.GENRE,
            eqBands = mapOf(60 to -1f, 230 to 2f, 910 to 4f, 3600 to 2f, 14000 to -1f)
        ),
        AudioPreset(
            id = "jazz",
            name = "Jazz",
            category = PresetCategory.GENRE,
            eqBands = mapOf(60 to 3f, 230 to 0f, 910 to 1f, 3600 to 2f, 14000 to 4f)
        ),
        AudioPreset(
            id = "classical",
            name = "Classical",
            category = PresetCategory.GENRE,
            eqBands = mapOf(60 to 4f, 230 to 2f, 910 to -1f, 3600 to 2f, 14000 to 4f),
            reverbPreset = "Medium Hall"
        ),
        AudioPreset(
            id = "hip_hop",
            name = "Hip Hop",
            category = PresetCategory.GENRE,
            eqBands = mapOf(60 to 5f, 230 to 4f, 910 to 0f, 3600 to 1f, 14000 to 3f),
            bassBoost = 600
        ),
        AudioPreset(
            id = "electronic",
            name = "Electronic",
            category = PresetCategory.GENRE,
            eqBands = mapOf(60 to 4f, 230 to 2f, 910 to 0f, 3600 to 2f, 14000 to 4f),
            bassBoost = 400
        ),
        AudioPreset(
            id = "rnb",
            name = "R&B",
            category = PresetCategory.GENRE,
            eqBands = mapOf(60 to 3f, 230 to 5f, 910 to 2f, 3600 to 1f, 14000 to 3f)
        ),
        AudioPreset(
            id = "acoustic",
            name = "Acoustic",
            category = PresetCategory.GENRE,
            eqBands = mapOf(60 to 4f, 230 to 2f, 910 to 1f, 3600 to 3f, 14000 to 2f)
        ),
        AudioPreset(
            id = "dance",
            name = "Dance",
            category = PresetCategory.GENRE,
            eqBands = mapOf(60 to 5f, 230 to 4f, 910 to 2f, 3600 to 0f, 14000 to -1f),
            bassBoost = 700
        ),
        AudioPreset(
            id = "metal",
            name = "Metal",
            category = PresetCategory.GENRE,
            eqBands = mapOf(60 to 3f, 230 to 0f, 910 to 0f, 3600 to 4f, 14000 to 3f)
        ),
        AudioPreset(
            id = "vocal",
            name = "Vocal Boost",
            category = PresetCategory.GENRE,
            eqBands = mapOf(60 to -2f, 230 to 0f, 910 to 4f, 3600 to 4f, 14000 to 0f)
        ),
        AudioPreset(
            id = "lounge",
            name = "Lounge",
            category = PresetCategory.GENRE,
            eqBands = mapOf(60 to 2f, 230 to 1f, 910 to 0f, 3600 to 1f, 14000 to 2f),
            reverbPreset = "Small Room"
        ),

        // Device presets
        AudioPreset(
            id = "headphones",
            name = "Headphones",
            category = PresetCategory.DEVICE,
            eqBands = mapOf(60 to 1f, 230 to 0f, 910 to 0f, 3600 to 1f, 14000 to 2f),
            virtualizer = 400
        ),
        AudioPreset(
            id = "earbuds",
            name = "Earbuds",
            category = PresetCategory.DEVICE,
            eqBands = mapOf(60 to 3f, 230 to 2f, 910 to 0f, 3600 to 1f, 14000 to 1f),
            bassBoost = 300
        ),
        AudioPreset(
            id = "speaker",
            name = "Speaker",
            category = PresetCategory.DEVICE,
            eqBands = mapOf(60 to 4f, 230 to 2f, 910 to 0f, 3600 to 1f, 14000 to 2f)
        ),
        AudioPreset(
            id = "car",
            name = "Car Audio",
            category = PresetCategory.DEVICE,
            eqBands = mapOf(60 to 3f, 230 to 1f, 910 to 0f, 3600 to 2f, 14000 to 3f),
            bassBoost = 400
        ),
        AudioPreset(
            id = "bluetooth",
            name = "Bluetooth",
            category = PresetCategory.DEVICE,
            eqBands = mapOf(60 to 2f, 230 to 1f, 910 to 0f, 3600 to 1f, 14000 to 1f)
        ),

        // Use case presets
        AudioPreset(
            id = "podcast",
            name = "Podcast",
            category = PresetCategory.USE_CASE,
            eqBands = mapOf(60 to -2f, 230 to 0f, 910 to 3f, 3600 to 3f, 14000 to 0f),
            specialMode = SpecialMode.PODCAST,
            compressorEnabled = true
        ),
        AudioPreset(
            id = "gaming",
            name = "Gaming",
            category = PresetCategory.USE_CASE,
            eqBands = mapOf(60 to 2f, 230 to 0f, 910 to 2f, 3600 to 4f, 14000 to 3f),
            virtualizer = 700,
            specialMode = SpecialMode.GAMING
        ),
        AudioPreset(
            id = "movie",
            name = "Movie",
            category = PresetCategory.USE_CASE,
            eqBands = mapOf(60 to 4f, 230 to 2f, 910 to 1f, 3600 to 2f, 14000 to 3f),
            virtualizer = 500,
            reverbPreset = "Large Room"
        ),
        AudioPreset(
            id = "workout",
            name = "Workout",
            category = PresetCategory.USE_CASE,
            eqBands = mapOf(60 to 6f, 230 to 4f, 910 to 2f, 3600 to 2f, 14000 to 3f),
            bassBoost = 800,
            specialMode = SpecialMode.WORKOUT
        ),
        AudioPreset(
            id = "sleep",
            name = "Sleep",
            category = PresetCategory.USE_CASE,
            eqBands = mapOf(60 to 1f, 230 to 0f, 910 to -1f, 3600 to -2f, 14000 to -3f),
            specialMode = SpecialMode.SLEEP
        ),
        AudioPreset(
            id = "deep_bass",
            name = "Deep Bass",
            category = PresetCategory.USE_CASE,
            eqBands = mapOf(60 to 8f, 230 to 6f, 910 to 2f, 3600 to 0f, 14000 to -1f),
            bassBoost = 900
        ),
        AudioPreset(
            id = "clarity",
            name = "Clarity",
            category = PresetCategory.USE_CASE,
            eqBands = mapOf(60 to -1f, 230 to 0f, 910 to 2f, 3600 to 4f, 14000 to 3f)
        ),
        AudioPreset(
            id = "powerful",
            name = "Powerful",
            category = PresetCategory.USE_CASE,
            eqBands = mapOf(60 to 4f, 230 to 3f, 910 to 2f, 3600 to 4f, 14000 to 5f),
            bassBoost = 500,
            limiterEnabled = true
        )
    )

    init {
        loadPresets()
    }

    private fun loadPresets() {
        val customPresets = loadCustomPresets()
        _presets.value = factoryPresets + customPresets

        // Load last used preset
        val lastPresetId = prefs.getString("last_preset_id", "flat")
        _currentPreset.value = _presets.value.find { it.id == lastPresetId }
    }

    private fun loadCustomPresets(): List<AudioPreset> {
        // Load from SharedPreferences or database
        val customPresetIds = prefs.getStringSet("custom_preset_ids", emptySet()) ?: emptySet()
        return customPresetIds.mapNotNull { id ->
            try {
                val json = prefs.getString("preset_$id", null) ?: return@mapNotNull null
                parsePresetFromJson(json)
            } catch (e: Exception) {
                null
            }
        }
    }

    fun selectPreset(preset: AudioPreset) {
        _currentPreset.value = preset
        prefs.edit().putString("last_preset_id", preset.id).apply()
    }

    fun saveCustomPreset(name: String, basedOn: AudioPreset? = null): AudioPreset {
        val id = "custom_${System.currentTimeMillis()}"
        val current = basedOn ?: _currentPreset.value

        val newPreset = AudioPreset(
            id = id,
            name = name,
            category = PresetCategory.CUSTOM,
            isFactory = false,
            isCustom = true,
            eqBands = current?.eqBands ?: emptyMap(),
            preampGain = current?.preampGain ?: 0f,
            bassBoost = current?.bassBoost ?: 0,
            virtualizer = current?.virtualizer ?: 0,
            reverbPreset = current?.reverbPreset ?: "None",
            compressorEnabled = current?.compressorEnabled ?: false,
            limiterEnabled = current?.limiterEnabled ?: false,
            stereoWidth = current?.stereoWidth ?: 1f
        )

        // Save to storage
        val customIds = prefs.getStringSet("custom_preset_ids", mutableSetOf())?.toMutableSet() ?: mutableSetOf()
        customIds.add(id)

        prefs.edit()
            .putStringSet("custom_preset_ids", customIds)
            .putString("preset_$id", presetToJson(newPreset))
            .apply()

        _presets.value = _presets.value + newPreset
        return newPreset
    }

    fun deletePreset(presetId: String) {
        val preset = _presets.value.find { it.id == presetId }
        if (preset?.isCustom == true) {
            val customIds = prefs.getStringSet("custom_preset_ids", mutableSetOf())?.toMutableSet() ?: mutableSetOf()
            customIds.remove(presetId)

            prefs.edit()
                .putStringSet("custom_preset_ids", customIds)
                .remove("preset_$presetId")
                .apply()

            _presets.value = _presets.value.filter { it.id != presetId }
        }
    }

    fun toggleFavorite(presetId: String) {
        _presets.value = _presets.value.map { preset ->
            if (preset.id == presetId) {
                val newPreset = preset.copy(isFavorite = !preset.isFavorite)
                if (preset.isCustom) {
                    prefs.edit().putString("preset_$presetId", presetToJson(newPreset)).apply()
                }
                newPreset
            } else preset
        }
    }

    fun getPresetsByCategory(category: PresetCategory): List<AudioPreset> {
        return if (category == PresetCategory.FAVORITE) {
            _presets.value.filter { it.isFavorite }
        } else {
            _presets.value.filter { it.category == category }
        }
    }

    fun exportPreset(preset: AudioPreset): String {
        return presetToJson(preset)
    }

    fun importPreset(json: String): AudioPreset? {
        return try {
            val preset = parsePresetFromJson(json)
            val newPreset = preset.copy(
                id = "imported_${System.currentTimeMillis()}",
                isFactory = false,
                isCustom = true
            )

            saveCustomPreset(newPreset.name, newPreset)
        } catch (e: Exception) {
            null
        }
    }

    private fun presetToJson(preset: AudioPreset): String {
        // Simple JSON serialization
        val eqBandsJson = preset.eqBands.entries.joinToString(",") { "\"${it.key}\":${it.value}" }
        return """
            {
                "id":"${preset.id}",
                "name":"${preset.name}",
                "category":"${preset.category.name}",
                "eqBands":{$eqBandsJson},
                "preampGain":${preset.preampGain},
                "bassBoost":${preset.bassBoost},
                "virtualizer":${preset.virtualizer},
                "reverbPreset":"${preset.reverbPreset}",
                "stereoWidth":${preset.stereoWidth},
                "isFavorite":${preset.isFavorite}
            }
        """.trimIndent()
    }

    private fun parsePresetFromJson(json: String): AudioPreset {
        // Simple JSON parsing - in production use Gson/Moshi
        val id = Regex("\"id\":\"([^\"]+)\"").find(json)?.groupValues?.get(1) ?: ""
        val name = Regex("\"name\":\"([^\"]+)\"").find(json)?.groupValues?.get(1) ?: ""
        val categoryStr = Regex("\"category\":\"([^\"]+)\"").find(json)?.groupValues?.get(1) ?: "CUSTOM"
        val preampGain = Regex("\"preampGain\":([-\\d.]+)").find(json)?.groupValues?.get(1)?.toFloatOrNull() ?: 0f
        val bassBoost = Regex("\"bassBoost\":(\\d+)").find(json)?.groupValues?.get(1)?.toIntOrNull() ?: 0
        val virtualizer = Regex("\"virtualizer\":(\\d+)").find(json)?.groupValues?.get(1)?.toIntOrNull() ?: 0
        val reverbPreset = Regex("\"reverbPreset\":\"([^\"]+)\"").find(json)?.groupValues?.get(1) ?: "None"
        val stereoWidth = Regex("\"stereoWidth\":([-\\d.]+)").find(json)?.groupValues?.get(1)?.toFloatOrNull() ?: 1f
        val isFavorite = Regex("\"isFavorite\":(true|false)").find(json)?.groupValues?.get(1)?.toBoolean() ?: false

        // Parse EQ bands
        val eqBandsMatch = Regex("\"eqBands\":\\{([^}]+)\\}").find(json)?.groupValues?.get(1) ?: ""
        val eqBands = Regex("\"(\\d+)\":([-\\d.]+)").findAll(eqBandsMatch).associate {
            it.groupValues[1].toInt() to it.groupValues[2].toFloat()
        }

        return AudioPreset(
            id = id,
            name = name,
            category = try { PresetCategory.valueOf(categoryStr) } catch (e: Exception) { PresetCategory.CUSTOM },
            isFactory = false,
            isCustom = true,
            isFavorite = isFavorite,
            eqBands = eqBands,
            preampGain = preampGain,
            bassBoost = bassBoost,
            virtualizer = virtualizer,
            reverbPreset = reverbPreset,
            stereoWidth = stereoWidth
        )
    }
}

// ============================================
// SPECIAL MODES PROCESSOR
// ============================================

class SpecialModeProcessor {

    private val _currentMode = MutableStateFlow<SpecialMode?>(null)
    val currentMode: StateFlow<SpecialMode?> = _currentMode.asStateFlow()

    fun activateMode(mode: SpecialMode?) {
        _currentMode.value = mode
    }

    fun getModeSettings(mode: SpecialMode): Map<String, Any> {
        return when (mode) {
            SpecialMode.PODCAST -> mapOf(
                "eqBoostMid" to true,
                "compressorEnabled" to true,
                "compressorThreshold" to -20f,
                "compressorRatio" to 3f,
                "bassReduce" to -3f,
                "voiceEnhance" to true
            )
            SpecialMode.GAMING -> mapOf(
                "virtualizerStrength" to 800,
                "spatialAudio" to true,
                "footstepEnhance" to true,
                "eqBands" to mapOf(60 to 2f, 2000 to 4f, 8000 to 3f),
                "dynamicRange" to "expanded"
            )
            SpecialMode.CALL -> mapOf(
                "noiseSupression" to true,
                "voiceEnhance" to true,
                "echoCancellation" to true,
                "eqBands" to mapOf(60 to -6f, 1000 to 4f, 4000 to 3f)
            )
            SpecialMode.KARAOKE -> mapOf(
                "vocalRemoval" to true,
                "centerChannelReduce" to -20f,
                "reverbAdd" to "Medium Hall"
            )
            SpecialMode.MUSIC_MASTERING -> mapOf(
                "limiterEnabled" to true,
                "limiterCeiling" to -0.3f,
                "loudnessTarget" to -14f,
                "multiband" to true
            )
            SpecialMode.NIGHT_MODE -> mapOf(
                "compressorEnabled" to true,
                "compressorThreshold" to -30f,
                "compressorRatio" to 6f,
                "limiterEnabled" to true,
                "maxVolume" to 0.6f
            )
            SpecialMode.WORKOUT -> mapOf(
                "bassBoost" to 800,
                "eqBands" to mapOf(60 to 6f, 230 to 4f),
                "loudnessEnhance" to true
            )
            SpecialMode.MEDITATION -> mapOf(
                "eqBands" to mapOf(60 to 2f, 14000 to -3f),
                "reverb" to "Large Hall",
                "lowPassFilter" to 8000f
            )
            SpecialMode.SLEEP -> mapOf(
                "eqBands" to mapOf(60 to 1f, 4000 to -4f, 14000 to -6f),
                "limiterEnabled" to true,
                "maxVolume" to 0.3f,
                "fadeOut" to true
            )
        }
    }
}

