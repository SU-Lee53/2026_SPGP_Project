package com.example.hillclimbracing.objects

import android.graphics.Canvas
import android.view.MotionEvent
import kr.ac.tukorea.ge.spgp2026.a2dg.objects.IGameObject
import kr.ac.tukorea.ge.spgp2026.a2dg.objects.ITouchable
import kr.ac.tukorea.ge.spgp2026.a2dg.view.GameContext

class TouchDriveInput(
    private val gctx: GameContext,
    private val car: Player,
) : IGameObject, ITouchable {

    override fun update(gctx: GameContext) {
    }

    override fun draw(canvas: Canvas) {
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        updateInputState(event)
        return true
    }

    private fun updateInputState(event: MotionEvent) {
        var accelerating = false
        var braking = false

        val action = event.actionMasked
        val actionIndex = event.actionIndex

        for (i in 0 until event.pointerCount) {
            val isPointerReleased =
                (action == MotionEvent.ACTION_POINTER_UP || action == MotionEvent.ACTION_UP) &&
                        i == actionIndex

            if (isPointerReleased) continue

            val point = gctx.metrics.fromScreen(event.getX(i), event.getY(i))
            if (point.x < gctx.metrics.width / 2f) {
                braking = true
            } else {
                accelerating = true
            }
        }

        if (action == MotionEvent.ACTION_UP || action == MotionEvent.ACTION_CANCEL) {
            accelerating = false
            braking = false
        }

        car.isAccelerating = accelerating
        car.isBraking = braking
    }
}