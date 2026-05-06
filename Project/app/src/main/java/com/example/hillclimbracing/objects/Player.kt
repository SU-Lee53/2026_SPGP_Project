package com.example.hillclimbracing.objects

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import androidx.core.graphics.withRotation
import kr.ac.tukorea.ge.spgp2026.a2dg.objects.IGameObject
import kr.ac.tukorea.ge.spgp2026.a2dg.view.GameContext
import kotlin.compareTo
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.sin

class Player(gctx: GameContext, private val terrain: HillTerrain) : IGameObject {
    var worldX = 220f
    var y = 900f
    var cameraX = 0f

    var velocityX = 0f
    var velocityY = 0f

    var fuel = 100f
    var distance = 0f

    var isAccelerating = false
    var isBraking = false
    var isGrounded = false
        private set

    var isDead = false
        private set

    private var angularVelocity = 0f

    private data class Wheel(
        val localX: Float,
        val localY: Float,
        val radius: Float,
    )

    private data class WheelSample(
        val centerX: Float,
        val centerY: Float,
        val groundY: Float,
        val penetration: Float,
        val targetBodyY: Float,
        val isCloseEnough: Boolean,
    )

    private val rearWheel = Wheel(
        localX = -WHEEL_HALF_DISTANCE,
        localY = WHEEL_CENTER_OFFSET_Y,
        radius = WHEEL_RADIUS,
    )

    private val frontWheel = Wheel(
        localX = WHEEL_HALF_DISTANCE,
        localY = WHEEL_CENTER_OFFSET_Y,
        radius = WHEEL_RADIUS,
    )

    private var angleRadians = 0f

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

        val dt = gctx.frameTime.coerceAtMost(MAX_DT)
        if (dt <= 0f) return

        updateHorizontalMovement(dt)
        updateVerticalMovement(dt)
        resolveWheelGroundContact(dt)

        distance = worldX / 10f

        if (fuel <= 0f) {
            fuel = 0f
            isDead = true
        }

        val angleDegrees = Math.toDegrees(angleRadians.toDouble()).toFloat()
        if (kotlin.math.abs(angleDegrees) > FLIP_DEAD_ANGLE) {
            isDead = true
        }
    }

    private fun updateHorizontalMovement(dt: Float) {
        if (isAccelerating && fuel > 0f) {
            velocityX += ENGINE_ACCEL * dt
            fuel -= FUEL_CONSUMPTION * dt
        }

        if (isBraking) {
            velocityX -= BRAKE_ACCEL * dt
        }

        velocityX *= if (isGrounded) GROUND_FRICTION else AIR_FRICTION
        velocityX = velocityX.coerceIn(0f, MAX_SPEED)

        worldX += velocityX * dt
    }

    private fun updateVerticalMovement(dt: Float) {
        velocityY += GRAVITY * dt
        velocityY = velocityY.coerceAtMost(MAX_FALL_SPEED)

        y += velocityY * dt
    }
    private fun sampleWheel(wheel: Wheel): WheelSample {
        val cosA = kotlin.math.cos(angleRadians)
        val sinA = kotlin.math.sin(angleRadians)

        val centerX = worldX + wheel.localX * cosA - wheel.localY * sinA
        val centerY = y + wheel.localX * sinA + wheel.localY * cosA

        val groundY = terrain.getGroundY(centerX)

        // y가 아래로 증가하는 좌표계.
        // wheelBottomY > groundY 이면 바퀴가 땅을 파고든 상태.
        val wheelBottomY = centerY + wheel.radius
        val penetration = wheelBottomY - groundY

        // 이 바퀴가 지면에 정확히 닿으려면 body center y가 어디여야 하는지
        val wheelOffsetY = wheel.localX * sinA + wheel.localY * cosA
        val targetBodyY = groundY - wheelOffsetY - wheel.radius

        val isCloseEnough = penetration >= -GROUND_SNAP_DISTANCE

        return WheelSample(
            centerX = centerX,
            centerY = centerY,
            groundY = groundY,
            penetration = penetration,
            targetBodyY = targetBodyY,
            isCloseEnough = isCloseEnough,
        )
    }
    private fun resolveWheelGroundContact(dt: Float) {
        val rear = sampleWheel(rearWheel)
        val front = sampleWheel(frontWheel)

        val rearTouches =
            rear.penetration >= 0f || (rear.isCloseEnough && velocityY >= 0f)

        val frontTouches =
            front.penetration >= 0f || (front.isCloseEnough && velocityY >= 0f)

        val groundedCandidate = rearTouches || frontTouches

        if (!groundedCandidate) {
            isGrounded = false

            // 공중에서는 지형 각도를 따라가지 않는다.
            angleRadians += angularVelocity * dt
            angularVelocity *= 0.98f
            return
        }

        isGrounded = true

        // 핵심:
        // 목표 각도는 현재 회전된 바퀴 centerX 기준이 아니라,
        // 차체 기준 앞/뒤 바퀴가 밟아야 할 지형 위치 기준으로 잡는다.
        val rearProbeX = worldX - WHEEL_HALF_DISTANCE
        val frontProbeX = worldX + WHEEL_HALF_DISTANCE

        val rearGroundY = terrain.getGroundY(rearProbeX)
        val frontGroundY = terrain.getGroundY(frontProbeX)

        val targetAngle = atan2(
            frontGroundY - rearGroundY,
            frontProbeX - rearProbeX,
        )

        angleRadians = lerpAngle(
            angleRadians,
            targetAngle,
            (ANGLE_FOLLOW_SPEED * dt).coerceAtMost(1f),
        )

        // 각도를 갱신했으니, 그 각도 기준으로 차체 y를 다시 계산한다.
        val cosA = kotlin.math.cos(angleRadians)
        val sinA = kotlin.math.sin(angleRadians)

        val rearOffsetY = rearWheel.localX * sinA + rearWheel.localY * cosA
        val frontOffsetY = frontWheel.localX * sinA + frontWheel.localY * cosA

        val rearTargetBodyY = rearGroundY - rearOffsetY - rearWheel.radius
        val frontTargetBodyY = frontGroundY - frontOffsetY - frontWheel.radius

        // 같은 선분 위라면 둘은 거의 같은 값이 된다.
        // 지형 꺾임 근처에서는 차체가 지형을 파고들지 않게 더 위쪽 값을 선택한다.
        y = minOf(rearTargetBodyY, frontTargetBodyY)

        if (velocityY > 0f) {
            velocityY = 0f
        }

        velocityX += sin(targetAngle) * SLOPE_ACCEL * dt
        velocityX = velocityX.coerceIn(0f, MAX_SPEED)

        angularVelocity *= 0.85f
    }

    override fun draw(canvas: Canvas) {
        val screenX = worldX - cameraX
        val screenY = y
        val angleDegrees = Math.toDegrees(angleRadians.toDouble()).toFloat()

        canvas.withRotation(angleDegrees, screenX, screenY) {
            bodyRect.set(
                screenX - 90f,
                screenY - 45f,
                screenX + 90f,
                screenY + 30f,
            )

            drawRoundRect(bodyRect, 20f, 20f, bodyPaint)

            drawCircle(
                screenX + rearWheel.localX,
                screenY + rearWheel.localY,
                rearWheel.radius,
                wheelPaint,
            )

            drawCircle(
                screenX + frontWheel.localX,
                screenY + frontWheel.localY,
                frontWheel.radius,
                wheelPaint,
            )
        }
    }

    private fun lerp(from: Float, to: Float, t: Float): Float {
        return from + (to - from) * t
    }

    private fun lerpAngle(from: Float, to: Float, t: Float): Float {
        var diff = to - from

        while (diff > Math.PI.toFloat()) diff -= (Math.PI * 2.0).toFloat()
        while (diff < -Math.PI.toFloat()) diff += (Math.PI * 2.0).toFloat()

        return from + diff * t
    }
    companion object {
        private const val MAX_DT = 1f / 20f

        private const val ENGINE_ACCEL = 620f
        private const val BRAKE_ACCEL = 520f
        private const val MAX_SPEED = 1100f

        private const val GRAVITY = 1800f
        private const val MAX_FALL_SPEED = 2300f

        private const val GROUND_FRICTION = 0.985f
        private const val AIR_FRICTION = 0.995f

        private const val FUEL_CONSUMPTION = 9f

        private const val WHEEL_HALF_DISTANCE = 58f
        private const val WHEEL_RADIUS = 26f
        private const val WHEEL_CENTER_OFFSET_Y = 42f

        private const val GROUND_SNAP_DISTANCE = 8f

        private const val SLOPE_ACCEL = 620f
        private const val ANGLE_FOLLOW_SPEED = 10f

        private const val FLIP_DEAD_ANGLE = 115f
    }
}