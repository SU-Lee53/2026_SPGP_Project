package com.example.hillclimbracing.objects

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import androidx.core.graphics.withRotation
import kr.ac.tukorea.ge.spgp2026.a2dg.objects.IGameObject
import kr.ac.tukorea.ge.spgp2026.a2dg.view.GameContext
import kotlin.math.abs

class Player(
    gctx: GameContext,
    private val terrain: HillTerrain,
) : IGameObject {
    var worldX = 220f
    var y = 900f
    var cameraX = 0f

    var velocityX = 0f
    var fuel = 100f
    var distance = 0f
    var isAccelerating = false
    var isBraking = false
    var isDead = false

    private var angleDegrees = 0f

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
            velocityX += 520f * gctx.frameTime
            fuel -= 10f * gctx.frameTime
        }

        if (isBraking) {
            velocityX -= 420f * gctx.frameTime
        }

        velocityX *= 0.985f
        velocityX = velocityX.coerceIn(0f, 900f)

        worldX += velocityX * gctx.frameTime

        val groundY = terrain.getGroundY(worldX)
        val slopeAngle = terrain.getSlopeAngle(worldX)

        y = groundY - 55f
        angleDegrees = Math.toDegrees(slopeAngle.toDouble()).toFloat()

        distance = worldX / 10f

        if (fuel <= 0f) {
            fuel = 0f
            isDead = true
        }

        // 발표용 임시 전복 판정.
        // 너무 가파른 지형에서 일정 각도 이상이면 죽게 할 수 있음.
        if (abs(angleDegrees) > 65f) {
            isDead = true
        }
    }

    override fun draw(canvas: Canvas) {
        val screenX = worldX - cameraX
        val screenY = y

        canvas.withRotation(angleDegrees, screenX, screenY) {
            bodyRect.set(
                screenX - 90f,
                screenY - 45f,
                screenX + 90f,
                screenY + 30f,
            )
            drawRoundRect(bodyRect, 20f, 20f, bodyPaint)

            drawCircle(screenX - 55f, screenY + 42f, 26f, wheelPaint)
            drawCircle(screenX + 55f, screenY + 42f, 26f, wheelPaint)
        }
    }
}