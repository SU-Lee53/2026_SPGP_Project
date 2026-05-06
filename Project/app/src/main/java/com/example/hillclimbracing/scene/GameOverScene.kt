package com.example.hillclimbracing.scene

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.view.MotionEvent
import kr.ac.tukorea.ge.spgp2026.a2dg.scene.Scene
import kr.ac.tukorea.ge.spgp2026.a2dg.view.GameContext

class GameOverScene(
    gctx: GameContext,
    private val distance: Int,
) : Scene(gctx) {
    private val titlePaint = Paint().apply {
        color = Color.BLACK
        textSize = 72f
        textAlign = Paint.Align.CENTER
        isAntiAlias = true
    }

    private val textPaint = Paint().apply {
        color = Color.DKGRAY
        textSize = 44f
        textAlign = Paint.Align.CENTER
        isAntiAlias = true
    }

    override fun draw(canvas: Canvas) {
        canvas.drawColor(Color.rgb(245, 245, 245))
        canvas.drawText("Game Over", gctx.metrics.width / 2f, 600f, titlePaint)
        canvas.drawText("Distance: ${distance}m", gctx.metrics.width / 2f, 700f, textPaint)
        canvas.drawText("Touch to Retry", gctx.metrics.width / 2f, 820f, textPaint)
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (event.actionMasked == MotionEvent.ACTION_DOWN) {
            MainScene(gctx).change()
            return true
        }
        return true
    }
}