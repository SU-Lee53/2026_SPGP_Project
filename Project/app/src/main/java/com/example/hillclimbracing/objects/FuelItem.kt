package com.example.hillclimbracing.objects

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF

class FuelItem(
    private val bitmap: Bitmap,
    var worldX: Float,
    var y: Float,
) {
    var collected = false

    private val paint = Paint().apply {
        color = Color.rgb(255, 190, 40)
        isAntiAlias = true
    }

    private val strokePaint = Paint().apply {
        color = Color.rgb(160, 90, 0)
        style = Paint.Style.STROKE
        strokeWidth = 4f
        isAntiAlias = true
    }

    private val dstRect = RectF()

    fun getCollisionRect(out: RectF): RectF {
        out.set(
            worldX - SIZE / 2f,
            y - SIZE / 2f,
            worldX + SIZE / 2f,
            y + SIZE / 2f,
        )
        return out
    }

    fun draw(canvas: Canvas, cameraX: Float) {
        if (collected) return

        val screenX = worldX - cameraX

        dstRect.set(
            screenX - SIZE / 2f,
            y - SIZE / 2f,
            screenX + SIZE / 2f,
            y + SIZE / 2f,
        )

        canvas.drawBitmap(bitmap, null, dstRect, null)
    }

    companion object {
        const val SIZE = 100f
        const val FUEL_AMOUNT = 13f
    }
}
