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
import kr.ac.tukorea.ge.spgp2026.a2dg.util.LabelUtil
import kr.ac.tukorea.ge.spgp2026.a2dg.view.GameContext

class PauseScene(
    gctx: GameContext,
) : Scene(gctx) {
    enum class Layer {
        BUTTON,
    }

    override val isTransparent = true
    override val world = World(Layer.entries.toTypedArray())

    private val titleLabel = LabelUtil(
        textSize = 76f,
        color = Color.WHITE,
        align = Paint.Align.CENTER,
    )

    private val resumeButton = Button(gctx, R.drawable.btn_resume, 450f, 720f, 320f, 96f) { pressed ->
        if (pressed) {
            pop()
        }
        true
    }

    private val backToMenuButton = Button(gctx, R.drawable.btn_menu, 450f, 860f, 320f, 96f) { pressed ->
        if (pressed) {
            pop()
            TitleScene(gctx).change()
        }
        true
    }

    init {
        world.add(resumeButton, Layer.BUTTON)
        world.add(backToMenuButton, Layer.BUTTON)
    }

    override fun draw(canvas: Canvas) {
        canvas.drawColor(Color.argb(150, 0, 0, 0))
        titleLabel.draw(canvas, "PAUSED", gctx.metrics.width / 2f, 580f)
        super.draw(canvas)
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        super.onTouchEvent(event)
        return true
    }

    override fun onBackPressed(): Boolean {
        pop()
        return true
    }

    protected override fun touchObjects(): List<IGameObject> {
        return world.objectsAt(Layer.BUTTON)
    }
}
