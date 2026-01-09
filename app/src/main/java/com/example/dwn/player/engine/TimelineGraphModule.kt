package com.example.dwn.player.engine

import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * ============================================
 * TIMELINE GRAPH MODULE
 * ============================================
 *
 * Graph-based timeline visualization:
 * - Loudness data
 * - Frequency energy data
 * - Intensity data
 * - Jump markers
 *
 * User can tap on graph to seek playback
 */
class TimelineGraphModule {

    private val _graphData = MutableStateFlow<TimelineGraphData?>(null)
    val graphData: StateFlow<TimelineGraphData?> = _graphData.asStateFlow()

    private val graphScope = CoroutineScope(Dispatchers.Default + SupervisorJob())

    private var analysisModule: AudioAnalysisModule? = null
    private var loopingModule: SmartLoopingModule? = null

    fun setAnalysisModule(module: AudioAnalysisModule) {
        this.analysisModule = module
    }

    fun setLoopingModule(module: SmartLoopingModule) {
        this.loopingModule = module
    }

    // ============================================
    // GRAPH GENERATION
    // ============================================

    /**
     * Generate timeline graph from audio analysis
     */
    fun generateGraph(durationMs: Long) {
        val analysis = analysisModule?.analysisState?.value ?: return
        val markers = loopingModule?.getJumpMarkers() ?: emptyList()

        graphScope.launch {
            val graphData = TimelineGraphData(
                loudnessData = analysis.energyProfile.ifEmpty { generateDefaultProfile(100) },
                frequencyEnergyData = generateFrequencyEnergyProfile(100),
                intensityData = generateIntensityProfile(analysis.energyProfile),
                markers = markers,
                duration = durationMs
            )

            _graphData.value = graphData
        }
    }

    /**
     * Generate graph with custom data
     */
    fun generateCustomGraph(
        loudnessData: List<Float>,
        frequencyEnergyData: List<Float>,
        intensityData: List<Float>,
        markers: List<JumpMarker>,
        durationMs: Long
    ) {
        _graphData.value = TimelineGraphData(
            loudnessData = loudnessData,
            frequencyEnergyData = frequencyEnergyData,
            intensityData = intensityData,
            markers = markers,
            duration = durationMs
        )
    }

    private fun generateDefaultProfile(segments: Int): List<Float> {
        return List(segments) { (Math.random() * 60 + 40).toFloat() }
    }

    private fun generateFrequencyEnergyProfile(segments: Int): List<Float> {
        return List(segments) { (Math.random() * 80 + 20).toFloat() }
    }

    private fun generateIntensityProfile(energyProfile: List<Float>): List<Float> {
        if (energyProfile.isEmpty()) return generateDefaultProfile(100)

        // Intensity is derived from energy with smoothing
        return energyProfile.mapIndexed { index, energy ->
            val prev = energyProfile.getOrNull(index - 1) ?: energy
            val next = energyProfile.getOrNull(index + 1) ?: energy
            (prev + energy + next) / 3f
        }
    }

    // ============================================
    // GRAPH QUERIES
    // ============================================

    /**
     * Get value at percentage of timeline
     */
    fun getLoudnessAtPercent(percent: Float): Float {
        val data = _graphData.value?.loudnessData ?: return 0f
        if (data.isEmpty()) return 0f
        val index = (percent * (data.size - 1)).toInt().coerceIn(0, data.size - 1)
        return data[index]
    }

    /**
     * Get frequency energy at percentage
     */
    fun getFrequencyEnergyAtPercent(percent: Float): Float {
        val data = _graphData.value?.frequencyEnergyData ?: return 0f
        if (data.isEmpty()) return 0f
        val index = (percent * (data.size - 1)).toInt().coerceIn(0, data.size - 1)
        return data[index]
    }

    /**
     * Get intensity at percentage
     */
    fun getIntensityAtPercent(percent: Float): Float {
        val data = _graphData.value?.intensityData ?: return 0f
        if (data.isEmpty()) return 0f
        val index = (percent * (data.size - 1)).toInt().coerceIn(0, data.size - 1)
        return data[index]
    }

    /**
     * Convert tap position (0-1) to time in milliseconds
     */
    fun tapToTimeMs(tapPercent: Float): Long {
        val duration = _graphData.value?.duration ?: 0L
        return (duration * tapPercent.coerceIn(0f, 1f)).toLong()
    }

    /**
     * Convert time to position (0-1)
     */
    fun timeToPercent(timeMs: Long): Float {
        val duration = _graphData.value?.duration ?: return 0f
        return if (duration > 0) (timeMs.toFloat() / duration).coerceIn(0f, 1f) else 0f
    }

    /**
     * Find peaks in loudness data
     */
    fun findLoudnessPeaks(threshold: Float = 80f): List<Float> {
        val data = _graphData.value?.loudnessData ?: return emptyList()
        return data.mapIndexedNotNull { index, value ->
            if (value > threshold) index.toFloat() / data.size else null
        }
    }

    /**
     * Find valleys (low points) in loudness data
     */
    fun findLoudnessValleys(threshold: Float = 30f): List<Float> {
        val data = _graphData.value?.loudnessData ?: return emptyList()
        return data.mapIndexedNotNull { index, value ->
            if (value < threshold) index.toFloat() / data.size else null
        }
    }

    // ============================================
    // GRAPH STATISTICS
    // ============================================

    /**
     * Get average loudness
     */
    fun getAverageLoudness(): Float {
        return _graphData.value?.loudnessData?.average()?.toFloat() ?: 0f
    }

    /**
     * Get peak loudness
     */
    fun getPeakLoudness(): Float {
        return _graphData.value?.loudnessData?.maxOrNull() ?: 0f
    }

    /**
     * Get minimum loudness
     */
    fun getMinLoudness(): Float {
        return _graphData.value?.loudnessData?.minOrNull() ?: 0f
    }

    /**
     * Get dynamic range (peak - min)
     */
    fun getDynamicRange(): Float {
        return getPeakLoudness() - getMinLoudness()
    }

    // ============================================
    // GRAPH FOR UI
    // ============================================

    /**
     * Get graph data for UI rendering
     * Returns normalized values (0-1) for easy drawing
     */
    fun getNormalizedLoudnessData(): List<Float> {
        val data = _graphData.value?.loudnessData ?: return emptyList()
        val max = data.maxOrNull() ?: 1f
        return data.map { (it / max).coerceIn(0f, 1f) }
    }

    /**
     * Get graph data downsampled for performance
     */
    fun getDownsampledData(targetSize: Int): List<Float> {
        val data = _graphData.value?.loudnessData ?: return emptyList()
        if (data.size <= targetSize) return getNormalizedLoudnessData()

        val step = data.size.toFloat() / targetSize
        return List(targetSize) { i ->
            val startIndex = (i * step).toInt()
            val endIndex = ((i + 1) * step).toInt().coerceAtMost(data.size)
            data.subList(startIndex, endIndex).average().toFloat()
        }.let { downsampled ->
            val max = downsampled.maxOrNull() ?: 1f
            downsampled.map { (it / max).coerceIn(0f, 1f) }
        }
    }

    /**
     * Get marker positions as percentages
     */
    fun getMarkerPercents(): List<Pair<Float, MarkerType>> {
        val graph = _graphData.value ?: return emptyList()
        if (graph.duration <= 0) return emptyList()

        return graph.markers.map { marker ->
            (marker.timeMs.toFloat() / graph.duration) to marker.type
        }
    }

    // ============================================
    // CLEANUP
    // ============================================

    fun clearGraph() {
        _graphData.value = null
    }

    fun release() {
        graphScope.cancel()
        clearGraph()
    }
}

