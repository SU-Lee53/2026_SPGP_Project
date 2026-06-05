package com.example.hillclimbracing.objects

import android.content.Context

object GameProgress {
    private const val PREFS_NAME = "hill_climb_progress"
    private const val KEY_BEST_DISTANCE = "best_distance"

    fun getBestDistance(context: Context): Int {
        return context
            .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getInt(KEY_BEST_DISTANCE, 0)
    }

    fun updateBestDistance(context: Context, distance: Int): Int {
        val bestDistance = getBestDistance(context)
        if (distance <= bestDistance) return bestDistance

        context
            .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putInt(KEY_BEST_DISTANCE, distance)
            .apply()

        return distance
    }
}
