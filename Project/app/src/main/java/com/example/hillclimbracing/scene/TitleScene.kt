package com.example.hillclimbracing.scene

import android.graphics.Canvas
import android.graphics.Color
import android.view.MotionEvent
import com.example.hillclimbracing.R
import com.example.hillclimbracing.objects.CameraScrollBackground
import com.example.hillclimbracing.objects.GameProgress
import com.example.hillclimbracing.objects.HillTerrain
import kr.ac.tukorea.ge.spgp2026.a2dg.objects.Button
import kr.ac.tukorea.ge.spgp2026.a2dg.objects.IGameObject
import kr.ac.tukorea.ge.spgp2026.a2dg.objects.Sprite
import kr.ac.tukorea.ge.spgp2026.a2dg.scene.Scene
import kr.ac.tukorea.ge.spgp2026.a2dg.scene.World
import kr.ac.tukorea.ge.spgp2026.a2dg.util.LabelUtil
import kr.ac.tukorea.ge.spgp2026.a2dg.view.GameContext

class TitleScene(
    gctx: GameContext,
) : Scene(gctx) {
    enum class Layer {
        BACKGROUND,
        TERRAIN,
        LOGO,
        BUTTON,
    }

    override val world = World(Layer.entries.toTypedArray())

    private val background = CameraScrollBackground(gctx, R.drawable.bg_mountain_loop, 0.16f)
    private val terrain = HillTerrain(gctx)
    private val logo = Sprite(gctx, R.drawable.menu_logo)
    private val startButton = Button(gctx, R.drawable.btn_start, 450f, 720f, 320f, 96f) { pressed ->
        if (pressed) {
            GameScene(gctx, terrain).change()
        }
        true
    }
    private val bestLabel = LabelUtil(
        textSize = 40f,
        color = Color.rgb(45, 55, 62),
        align = android.graphics.Paint.Align.CENTER,
    )

    init {
        logo.setCenter(450f, 330f)
        logo.setSize(640f, 240f)

        world.add(background, Layer.BACKGROUND)
        world.add(terrain, Layer.TERRAIN)
        world.add(logo, Layer.LOGO)
        world.add(startButton, Layer.BUTTON)
    }

    override fun draw(canvas: Canvas) {
        val centerX = gctx.metrics.width / 2f
        val bestDistance = GameProgress.getBestDistance(gctx.view.context)

        canvas.drawColor(Color.rgb(220, 240, 255))
        super.draw(canvas)
        bestLabel.draw(canvas, "Best Distance: ${bestDistance}m", centerX, 600f)
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        super.onTouchEvent(event)
        return true
    }

    protected override fun touchObjects(): List<IGameObject> {
        return world.objectsAt(Layer.BUTTON)
    }
}
