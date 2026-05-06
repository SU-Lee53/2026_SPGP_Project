package com.example.hillclimbracing

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import kr.ac.tukorea.ge.spgp2026.a2dg.scene.Scene
import kr.ac.tukorea.ge.spgp2026.a2dg.view.GameContext

class TitleScene(gctx: GameContext) : Scene(gctx) {
    private val paint = Paint().apply{
        color = Color.BLACK
        textSize = 64f
        textAlign = Paint.Align.CENTER
    }

    override fun draw(canvas: Canvas){
        canvas.drawColor(Color.WHITE)
        canvas.drawText(
            "Hill Climb Challenge",
            gctx.metrics.width / 2f,
            gctx.metrics.height / 2f,
            paint
        )
    }
}