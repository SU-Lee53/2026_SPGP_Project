package com.example.hillclimbracing.scene

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.view.MotionEvent
import com.example.hillclimbracing.R
import com.example.hillclimbracing.objects.CameraScrollBackground
import com.example.hillclimbracing.objects.HillTerrain
import kr.ac.tukorea.ge.spgp2026.a2dg.objects.Button
import kr.ac.tukorea.ge.spgp2026.a2dg.objects.IGameObject
import kr.ac.tukorea.ge.spgp2026.a2dg.objects.Sprite
import kr.ac.tukorea.ge.spgp2026.a2dg.scene.Scene
import kr.ac.tukorea.ge.spgp2026.a2dg.scene.World
import kr.ac.tukorea.ge.spgp2026.a2dg.view.GameContext

class GameOverScene(
    gctx: GameContext,
    private val distance: Int,
    private val bestDistance: Int,
    private val terrain: HillTerrain,
    private val cameraX: Float,
) : Scene(gctx) {
    enum class Layer {
        BACKGROUND,
        TERRAIN,
        LOGO,
        BUTTON,
    }

    override val world = World(Layer.entries.toTypedArray())

    private val background = CameraScrollBackground(gctx, R.drawable.bg_mountain_loop, 0.16f)
    private val logo = Sprite(gctx, R.drawable.game_over_logo)
    private val retryButton = Button(gctx, R.drawable.btn_retry, 450f, 640f, 320f, 96f) { pressed ->
        if (pressed) {
            GameScene(gctx).change()
        }
        true
    }

    private val textPaint = Paint().apply {
        color = Color.DKGRAY
        textSize = 44f
        textAlign = Paint.Align.CENTER
        isAntiAlias = true
    }

    init {
        terrain.cameraX = cameraX
        background.cameraX = cameraX
        logo.setCenter(450f, 320f)
        logo.setSize(620f, 164f)

        world.add(background, Layer.BACKGROUND)
        world.add(terrain, Layer.TERRAIN)
        world.add(logo, Layer.LOGO)
        world.add(retryButton, Layer.BUTTON)
    }

    override fun draw(canvas: Canvas) {
        val centerX = gctx.metrics.width / 2f

        canvas.drawColor(Color.rgb(220, 240, 255))
        super.draw(canvas)
        canvas.drawText("Distance: ${distance}m", centerX, 445f, textPaint)
        canvas.drawText("Best Distance: ${bestDistance}m", centerX, 510f, textPaint)
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        super.onTouchEvent(event)
        return true
    }

    protected override fun touchObjects(): List<IGameObject> {
        return world.objectsAt(Layer.BUTTON)
    }
}
