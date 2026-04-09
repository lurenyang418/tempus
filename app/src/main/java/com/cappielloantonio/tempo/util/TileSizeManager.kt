package com.cappielloantonio.tempo.util

import android.content.Context
import com.cappielloantonio.tempo.util.Preferences.getTileSize
import kotlin.math.max
import kotlin.math.min

class TileSizeManager private constructor() {
    private var tileSizePx = 0
    private var tileSpanCount = 0
    private var tileSpacing = 0
    private var genreSizePx = 0
    private var genreSpanCount = 0
    private var genreSpacing = 0
    private val GenreSpacing = 0
    private var discoverWidthPx = 0
    private var discoverHeightPx = 0
    private var tileIsInitialized = false
    private var genreIsInitialized = false
    private var discoverIsInitialized = false

    fun getTileSizePx(context: Context): Int {
        if (!tileIsInitialized) calculateTileSize(context)
        return tileSizePx
    }

    fun getTileSpanCount(context: Context): Int {
        if (!tileIsInitialized) calculateTileSize(context)
        return tileSpanCount
    }

    fun getTileSpacing(context: Context): Int {
        if (!tileIsInitialized) calculateTileSize(context)
        return tileSpacing
    }

    fun getGenreSizePx(context: Context): Int {
        if (!genreIsInitialized) calculateGenreSize(context)
        return genreSizePx
    }

    fun getGenreSpanCount(context: Context): Int {
        if (!genreIsInitialized) calculateGenreSize(context)
        return genreSpanCount
    }

    fun getGenreSpacing(context: Context): Int {
        if (!genreIsInitialized) calculateGenreSize(context)
        return genreSpacing
    }

    fun getDiscoverWidthPx(context: Context): Int {
        if (!discoverIsInitialized) calculateTileSize(context)
        return discoverWidthPx
    }

    fun getDiscoverHeightPx(context: Context): Int {
        if (!discoverIsInitialized) calculateTileSize(context)
        return discoverHeightPx
    }

    fun calculateTileSize(context: Context) {
        val metrics = context.getResources().getDisplayMetrics()
        val screenWidth = metrics.widthPixels.toFloat()
        val screenHeight = metrics.heightPixels.toFloat()

        // retrieve the divisor in the preferences
        val userTileSize = max(2, min(6, getTileSize()))
        val divisor = userTileSize.toFloat()

        // little pading = 10
        tileSizePx = Math.round(min(screenWidth, screenHeight) / divisor) - 10
        tileSpanCount = max(2, Math.round(screenWidth / tileSizePx.toFloat()))

        when (userTileSize) {
            2 -> tileSpacing = 20
            3 -> tileSpacing = 15
            4 -> tileSpacing = 10
            5 -> tileSpacing = 6
            6 -> tileSpacing = 2
            else -> tileSpacing = 20
        }
        tileIsInitialized = true
    }

    fun calculateGenreSize(context: Context) {
        val metrics = context.getResources().getDisplayMetrics()
        val screenWidth = metrics.widthPixels.toFloat()
        val screenHeight = metrics.heightPixels.toFloat()

        // retrieve the divisor in the preferences
        val userTileSize = max(2, min(3, getTileSize()))
        val divisor = userTileSize.toFloat()

        // little pading = 10
        genreSizePx = Math.round(min(screenWidth, screenHeight) / divisor) - 10
        genreSpanCount = max(2, Math.round(screenWidth / genreSizePx.toFloat()))

        when (userTileSize) {
            2 -> genreSpacing = 20
            3 -> genreSpacing = 15
            4 -> genreSpacing = 10
            5 -> genreSpacing = 6
            6 -> genreSpacing = 2
            else -> genreSpacing = 20
        }
        genreIsInitialized = true
    }

    fun calculateDiscoverSize(context: Context) {
        val metrics = context.getResources().getDisplayMetrics()
        val screenWidth = metrics.widthPixels.toFloat()
        val screenHeight = metrics.heightPixels.toFloat()
        val discoverDivisor: Float

        // retrieve the divisor in the preferences
        val userTileSize = max(2, min(6, getTileSize()))

        when (userTileSize) {
            2 -> discoverDivisor = 1.0f
            3 -> discoverDivisor = 1.25f
            4 -> discoverDivisor = 1.5f
            5 -> discoverDivisor = 1.75f
            6 -> discoverDivisor = 2.0f
            else -> discoverDivisor = 1.0f
        }

        discoverWidthPx = Math.round(min(screenWidth, screenHeight) / discoverDivisor) - 50
        discoverHeightPx = Math.round(discoverWidthPx.toFloat() * 0.6f)
        discoverIsInitialized = true
    }

    companion object {
        @JvmStatic
        var instance: TileSizeManager? = null
            get() {
                if (field == null) {
                    field = TileSizeManager()
                }
                return field
            }
            private set
    }
}
