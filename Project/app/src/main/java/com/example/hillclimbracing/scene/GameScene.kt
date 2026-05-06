package com.example.hillclimbracing.scene

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.view.MotionEvent
import kr.ac.tukorea.ge.spgp2026.a2dg.scene.Scene
import kr.ac.tukorea.ge.spgp2026.a2dg.view.GameContext

class GameScene(gctx: GameContext) : Scene(gctx) {
    private val paint = Paint().apply{
        color = Color.BLACK
        textSize = 64f
        textAlign = Paint.Align.CENTER
    }

    override fun draw(canvas: Canvas){
        canvas.drawColor(Color.WHITE)
        canvas.drawText(
            "Game Scene",
            gctx.metrics.width / 2f,
            gctx.metrics.height / 2f,
            paint
        )
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        GameOverScene(gctx).push()
        return true
    }

    override fun onBackPressed(): Boolean {
        pop()
        return true
    }

}