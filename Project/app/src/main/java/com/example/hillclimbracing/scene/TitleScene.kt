package com.example.hillclimbracing.scene

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.view.MotionEvent
import com.example.hillclimbracing.objects.GameProgress
import kr.ac.tukorea.ge.spgp2026.a2dg.scene.Scene
import kr.ac.tukorea.ge.spgp2026.a2dg.view.GameContext

class TitleScene(
    gctx: GameContext,
) : Scene(gctx) {
    private val titlePaint = Paint().apply {
        color = Color.BLACK
        textSize = 72f
        textAlign = Paint.Align.CENTER
        isAntiAlias = true
    }

    private val guidePaint = Paint().apply {
        color = Color.DKGRAY
        textSize = 40f
        textAlign = Paint.Align.CENTER
        isAntiAlias = true
    }

    override fun draw(canvas: Canvas) {
        val centerX = gctx.metrics.width / 2f
        val bestDistance = GameProgress.getBestDistance(gctx.view.context)

        canvas.drawColor(Color.rgb(220, 240, 255))
        canvas.drawText("Hill Climb Challenge", centerX, 650f, titlePaint)
        canvas.drawText("Best Distance: ${bestDistance}m", centerX, 740f, guidePaint)
        canvas.drawText("Touch to Start", centerX, 830f, guidePaint)
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (event.actionMasked == MotionEvent.ACTION_DOWN) {
            GameScene(gctx).change()
            return true
        }
        return true
    }
}
