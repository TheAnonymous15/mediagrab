package com.example.dwn.player.engine

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * ============================================
 * FX AUTOMATION MODULE
 * ============================================
 *
 * Timeline-based FX parameter automation:
 * - FX parameters can change over time
 * - Automation curves stored per parameter
 * - Supports linear, smooth, and step interpolation
 * - Works offline
 *
 * Examples:
 * - Bass boost during chorus
 * - Reverb fade-out at outro
 * - Volume swells
 */
class FXAutomationModule {

    private val automationCurves = mutableMapOf<String, MutableList<AutomationPoint>>()

    private val _automationState = MutableStateFlow<Map<String, List<AutomationPoint>>>(emptyMap())
    val automationState: StateFlow<Map<String, List<AutomationPoint>>> = _automationState.asStateFlow()

    // Interpolation type per parameter
    private val interpolationTypes = mutableMapOf<String, InterpolationType>()

    // Supported parameter names
    companion object {
        const val PARAM_VOLUME = "volume"
        const val PARAM_BASS_BOOST = "bass_boost"
        const val PARAM_VIRTUALIZER = "virtualizer"
        const val PARAM_LOUDNESS = "loudness"
        const val PARAM_REVERB = "reverb"
        const val PARAM_PAN = "pan"
        const val PARAM_EQ_BAND_PREFIX = "eq_band_"
        const val PARAM_SPEED = "speed"

        val SUPPORTED_PARAMS = listOf(
            PARAM_VOLUME,
            PARAM_BASS_BOOST,
            PARAM_VIRTUALIZER,
            PARAM_LOUDNESS,
            PARAM_REVERB,
            PARAM_PAN,
            PARAM_SPEED
        )
    }

    // ============================================
    // AUTOMATION POINTS
    // ============================================

    /**
     * Add an automation point
     */
    fun addPoint(paramName: String, timeMs: Long, value: Float) {
        val points = automationCurves.getOrPut(paramName) { mutableListOf() }

        // Remove existing point at same time
        points.removeAll { it.timeMs == timeMs }

        // Add new point
        points.add(AutomationPoint(timeMs, value))

        // Keep sorted by time
        points.sortBy { it.timeMs }

        updateState()
    }

    /**
     * Remove an automation point
     */
    fun removePoint(paramName: String, timeMs: Long) {
        automationCurves[paramName]?.removeAll { it.timeMs == timeMs }
        updateState()
    }

    /**
     * Update an existing automation point's value
     */
    fun updatePoint(paramName: String, timeMs: Long, newValue: Float) {
        automationCurves[paramName]?.find { it.timeMs == timeMs }?.let { _ ->
            removePoint(paramName, timeMs)
            addPoint(paramName, timeMs, newValue)
        }
    }

    /**
     * Move an automation point to a new time
     */
    fun movePoint(paramName: String, oldTimeMs: Long, newTimeMs: Long) {
        val point = automationCurves[paramName]?.find { it.timeMs == oldTimeMs }
        point?.let {
            removePoint(paramName, oldTimeMs)
            addPoint(paramName, newTimeMs, it.value)
        }
    }

    /**
     * Get all points for a parameter
     */
    fun getPoints(paramName: String): List<AutomationPoint> {
        return automationCurves[paramName]?.toList() ?: emptyList()
    }

    /**
     * Clear all automation for a parameter
     */
    fun clearParameter(paramName: String) {
        automationCurves.remove(paramName)
        interpolationTypes.remove(paramName)
        updateState()
    }

    /**
     * Clear all automation
     */
    fun clearAll() {
        automationCurves.clear()
        interpolationTypes.clear()
        updateState()
    }

    // ============================================
    // INTERPOLATION
    // ============================================

    /**
     * Set interpolation type for a parameter
     */
    fun setInterpolationType(paramName: String, type: InterpolationType) {
        interpolationTypes[paramName] = type
    }

    /**
     * Get interpolation type for a parameter
     */
    fun getInterpolationType(paramName: String): InterpolationType {
        return interpolationTypes[paramName] ?: InterpolationType.LINEAR
    }

    /**
     * Get interpolated value at a specific time
     */
    fun getValueAt(paramName: String, timeMs: Long): Float? {
        val points = automationCurves[paramName] ?: return null
        if (points.isEmpty()) return null

        // Find surrounding points
        val beforePoint = points.lastOrNull { it.timeMs <= timeMs }
        val afterPoint = points.firstOrNull { it.timeMs > timeMs }

        return when {
            beforePoint == null -> points.first().value
            afterPoint == null -> beforePoint.value
            else -> interpolate(
                beforePoint,
                afterPoint,
                timeMs,
                interpolationTypes[paramName] ?: InterpolationType.LINEAR
            )
        }
    }

    private fun interpolate(
        before: AutomationPoint,
        after: AutomationPoint,
        timeMs: Long,
        type: InterpolationType
    ): Float {
        val progress = (timeMs - before.timeMs).toFloat() / (after.timeMs - before.timeMs)

        return when (type) {
            InterpolationType.LINEAR -> {
                before.value + (after.value - before.value) * progress
            }
            InterpolationType.SMOOTH -> {
                // Smooth step (ease in-out)
                val smoothProgress = progress * progress * (3f - 2f * progress)
                before.value + (after.value - before.value) * smoothProgress
            }
            InterpolationType.STEP -> {
                // Step (jump at midpoint)
                if (progress < 0.5f) before.value else after.value
            }
        }
    }

    // ============================================
    // BATCH OPERATIONS
    // ============================================

    /**
     * Get all automation values at a specific time
     */
    fun getAllValuesAt(timeMs: Long): Map<String, Float> {
        return automationCurves.keys.mapNotNull { param ->
            getValueAt(param, timeMs)?.let { param to it }
        }.toMap()
    }

    /**
     * Import automation from curves
     */
    fun importCurves(curves: List<AutomationCurve>) {
        curves.forEach { curve ->
            automationCurves[curve.paramName] = curve.points.toMutableList()
            interpolationTypes[curve.paramName] = curve.interpolation
        }
        updateState()
    }

    /**
     * Export automation to curves
     */
    fun exportCurves(): List<AutomationCurve> {
        return automationCurves.map { (param, points) ->
            AutomationCurve(
                paramName = param,
                points = points.toList(),
                interpolation = interpolationTypes[param] ?: InterpolationType.LINEAR
            )
        }
    }

    /**
     * Copy automation from one parameter to another
     */
    fun copyAutomation(fromParam: String, toParam: String) {
        automationCurves[fromParam]?.let { points ->
            automationCurves[toParam] = points.toMutableList()
            interpolationTypes[toParam] = interpolationTypes[fromParam] ?: InterpolationType.LINEAR
        }
        updateState()
    }

    /**
     * Scale all values for a parameter
     */
    fun scaleValues(paramName: String, factor: Float) {
        automationCurves[paramName]?.let { points ->
            val scaled = points.map { AutomationPoint(it.timeMs, it.value * factor) }
            automationCurves[paramName] = scaled.toMutableList()
        }
        updateState()
    }

    /**
     * Shift all times for a parameter
     */
    fun shiftTimes(paramName: String, deltaMs: Long) {
        automationCurves[paramName]?.let { points ->
            val shifted = points.map {
                AutomationPoint((it.timeMs + deltaMs).coerceAtLeast(0), it.value)
            }
            automationCurves[paramName] = shifted.toMutableList()
        }
        updateState()
    }

    // ============================================
    // PRESETS
    // ============================================

    /**
     * Create a fade-in automation
     */
    fun createFadeIn(paramName: String, startMs: Long, endMs: Long, startValue: Float = 0f, endValue: Float = 1f) {
        clearParameter(paramName)
        addPoint(paramName, startMs, startValue)
        addPoint(paramName, endMs, endValue)
        setInterpolationType(paramName, InterpolationType.SMOOTH)
    }

    /**
     * Create a fade-out automation
     */
    fun createFadeOut(paramName: String, startMs: Long, endMs: Long, startValue: Float = 1f, endValue: Float = 0f) {
        clearParameter(paramName)
        addPoint(paramName, startMs, startValue)
        addPoint(paramName, endMs, endValue)
        setInterpolationType(paramName, InterpolationType.SMOOTH)
    }

    /**
     * Create a crossfade between two values
     */
    fun createCrossfade(paramName: String, startMs: Long, crossfadeMs: Long, endMs: Long,
                        lowValue: Float = 0f, highValue: Float = 1f) {
        clearParameter(paramName)
        addPoint(paramName, startMs, highValue)
        addPoint(paramName, crossfadeMs, lowValue)
        addPoint(paramName, endMs, highValue)
        setInterpolationType(paramName, InterpolationType.SMOOTH)
    }

    // ============================================
    // STATE
    // ============================================

    private fun updateState() {
        _automationState.value = automationCurves.mapValues { it.value.toList() }
    }

    /**
     * Check if parameter has automation
     */
    fun hasAutomation(paramName: String): Boolean {
        return automationCurves[paramName]?.isNotEmpty() == true
    }

    /**
     * Get all parameters with automation
     */
    fun getAutomatedParameters(): List<String> {
        return automationCurves.keys.filter { automationCurves[it]?.isNotEmpty() == true }
    }
}

