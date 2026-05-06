package com.example.hillclimbracing.objects

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import kr.ac.tukorea.ge.spgp2026.a2dg.objects.IGameObject
import kr.ac.tukorea.ge.spgp2026.a2dg.view.GameContext

class Player(gctx: GameContext) : IGameObject {
    var x = 220f
    var y = 220f
    var velocityX = 0f
    var fuel = 100f
    var distance = 0f
    var isAccelerating = false
    var isBraking = false
    var isDead = false

    private val bodyRect = RectF()
    private val bodyPaint = Paint().apply {
        color = Color.rgb(220, 60, 60)
        isAntiAlias = true
    }

    private val wheelPaint = Paint().apply {
        color = Color.BLACK
        isAntiAlias = true
    }

    override fun update(gctx: GameContext) {
        if (isDead) return

        if (isAccelerating && fuel > 0f) {
            velocityX += 500f * gctx.frameTime
            fuel -= 12f * gctx.frameTime
        }

        if (isBraking) {
            velocityX -= 350f * gctx.frameTime
        }

        velocityX *= 0.985f
        velocityX = velocityX.coerceIn(0f, 900f)

        x += velocityX * gctx.frameTime
        distance = x / 10f

        if (fuel <= 0f) {
            fuel = 0f
            isDead = true
        }
    }

    override fun draw(canvas: Canvas) {
        val screenX = 300f
        val screenY = y

        bodyRect.set(
            screenX - 90f,
            screenY - 50f,
            screenX + 90f,
            screenY + 25f,
        )
        canvas.drawRoundRect(bodyRect, 20f, 20f, bodyPaint)

        canvas.drawCircle(screenX - 55f, screenY + 35f, 26f, wheelPaint)
        canvas.drawCircle(screenX + 55f, screenY + 35f, 26f, wheelPaint)
    }



}