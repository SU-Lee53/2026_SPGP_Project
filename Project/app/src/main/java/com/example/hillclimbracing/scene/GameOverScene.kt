package com.example.hillclimbracing.scene

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.view.MotionEvent
import com.example.hillclimbracing.R
import kr.ac.tukorea.ge.spgp2026.a2dg.objects.Button
import kr.ac.tukorea.ge.spgp2026.a2dg.objects.IGameObject
import kr.ac.tukorea.ge.spgp2026.a2dg.scene.Scene
import kr.ac.tukorea.ge.spgp2026.a2dg.scene.World
import kr.ac.tukorea.ge.spgp2026.a2dg.view.GameContext

class GameOverScene(
    gctx: GameContext,
    private val distance: Int,
    private val bestDistance: Int,
) : Scene(gctx) {
    enum class Layer {
        BUTTON,
    }

    override val world = World(Layer.entries.toTypedArray())

    private val retryButton = Button(gctx, R.drawable.btn_retry, 450f, 910f, 320f, 96f) { pressed ->
        if (pressed) {
            GameScene(gctx).change()
        }
        true
    }

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

    init {
        world.add(retryButton, Layer.BUTTON)
    }

    override fun draw(canvas: Canvas) {
        val centerX = gctx.metrics.width / 2f

        canvas.drawColor(Color.rgb(245, 245, 245))
        canvas.drawText("Game Over", centerX, 600f, titlePaint)
        canvas.drawText("Distance: ${distance}m", centerX, 700f, textPaint)
        canvas.drawText("Best Distance: ${bestDistance}m", centerX, 780f, textPaint)
        super.draw(canvas)
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        super.onTouchEvent(event)
        return true
    }

    protected override fun touchObjects(): List<IGameObject> {
        return world.objectsAt(Layer.BUTTON)
    }
}
