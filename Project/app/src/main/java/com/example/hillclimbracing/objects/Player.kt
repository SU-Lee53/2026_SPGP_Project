package com.example.hillclimbracing.objects

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import androidx.core.graphics.withRotation
import kr.ac.tukorea.ge.spgp2026.a2dg.objects.IGameObject
import kr.ac.tukorea.ge.spgp2026.a2dg.view.GameContext
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.max
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

    private class Wheel(
        val localX: Float,
        val anchorLocalY: Float,
        val radius: Float,
        var suspensionLength: Float,
    ) {
        var previousCompression = 0f
    }

    private data class WheelSample(
        val wheel: Wheel,
        val anchorX: Float,
        val anchorY: Float,
        val groundY: Float,
        val distanceToGround: Float,
        val compression: Float,
        val compressionVelocity: Float,
        val hardPenetration: Float,
        val isContacting: Boolean,
    )

    private val rearWheel = Wheel(
        localX = -WHEEL_HALF_DISTANCE,
        anchorLocalY = WHEEL_ANCHOR_OFFSET_Y,
        radius = WHEEL_RADIUS,
        suspensionLength = SUSPENSION_REST_LENGTH,
    )

    private val frontWheel = Wheel(
        localX = WHEEL_HALF_DISTANCE,
        anchorLocalY = WHEEL_ANCHOR_OFFSET_Y,
        radius = WHEEL_RADIUS,
        suspensionLength = SUSPENSION_REST_LENGTH,
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

        val friction = when {
            isAccelerating -> DRIVE_FRICTION
            isBraking -> BRAKE_FRICTION
            isGrounded -> GROUND_FRICTION
            else -> AIR_FRICTION
        }

        velocityX *= friction
        velocityX = velocityX.coerceIn(0f, MAX_SPEED)

        worldX += velocityX * dt
    }

    private fun updateVerticalMovement(dt: Float) {
        velocityY += GRAVITY * dt
        velocityY = velocityY.coerceAtMost(MAX_FALL_SPEED)

        y += velocityY * dt
    }
    private fun sampleWheel(wheel: Wheel, dt: Float): WheelSample {
        val cosA = cos(angleRadians)
        val sinA = sin(angleRadians)

        // 서스펜션 anchor 위치.
        // 바퀴 중심이 아니라 차체에 서스펜션이 매달린 지점이다.
        val anchorX = worldX + wheel.localX * cosA - wheel.anchorLocalY * sinA
        val anchorY = y + wheel.localX * sinA + wheel.anchorLocalY * cosA

        val groundY = terrain.getGroundY(anchorX)

        // anchor에서 지면까지 바퀴 반지름을 뺀 거리.
        // 이 값이 suspensionLength가 되면 바퀴 바닥이 지면에 닿는다.
        val distanceToGround = groundY - anchorY - wheel.radius

        val isContacting = distanceToGround <= SUSPENSION_MAX_LENGTH

        val targetSuspensionLength = if (isContacting) {
            distanceToGround.coerceIn(SUSPENSION_MIN_LENGTH, SUSPENSION_MAX_LENGTH)
        } else {
            SUSPENSION_MAX_LENGTH
        }

        wheel.suspensionLength = lerp(
            wheel.suspensionLength,
            targetSuspensionLength,
            (SUSPENSION_VISUAL_FOLLOW_SPEED * dt).coerceAtMost(1f),
        )

        val compression = if (isContacting) {
            (SUSPENSION_REST_LENGTH - targetSuspensionLength)
                .coerceIn(0f, SUSPENSION_REST_LENGTH - SUSPENSION_MIN_LENGTH)
        } else {
            0f
        }

        val compressionVelocity = if (dt > 0f) {
            (compression - wheel.previousCompression) / dt
        } else {
            0f
        }

        wheel.previousCompression = compression

        // 서스펜션이 최대로 눌렸는데도 땅을 파고든 경우에는
        // 차체를 강제로 조금 밀어올려야 한다.
        val hardPenetration = if (distanceToGround < SUSPENSION_MIN_LENGTH) {
            SUSPENSION_MIN_LENGTH - distanceToGround
        } else {
            0f
        }

        return WheelSample(
            wheel = wheel,
            anchorX = anchorX,
            anchorY = anchorY,
            groundY = groundY,
            distanceToGround = distanceToGround,
            compression = compression,
            compressionVelocity = compressionVelocity,
            hardPenetration = hardPenetration,
            isContacting = isContacting,
        )
    }
    private fun resolveWheelGroundContact(dt: Float) {
        val rear = sampleWheel(rearWheel, dt)
        val front = sampleWheel(frontWheel, dt)

        isGrounded = rear.isContacting || front.isContacting

        if (!isGrounded) {
            angleRadians += angularVelocity * dt
            angularVelocity *= AIR_ANGULAR_DAMPING
            return
        }

        applySuspensionForce(rear, dt)
        applySuspensionForce(front, dt)

        // 너무 깊게 박히는 경우만 강제 보정.
        // 일반적인 접지는 spring force로 처리한다.
        val maxHardPenetration = max(rear.hardPenetration, front.hardPenetration)
        if (maxHardPenetration > 0f) {
            y -= maxHardPenetration

            if (velocityY > 0f) {
                velocityY = 0f
            }
        }

        val rearProbeX = worldX - WHEEL_HALF_DISTANCE
        val frontProbeX = worldX + WHEEL_HALF_DISTANCE

        val rearGroundY = terrain.getGroundY(rearProbeX)
        val frontGroundY = terrain.getGroundY(frontProbeX)

        val targetAngle = atan2(
            frontGroundY - rearGroundY,
            frontProbeX - rearProbeX,
        )

        // 순수 서스펜션만 두면 초반 튜닝이 까다로워서,
        // 약한 보조 토크만 넣는다.
        // 나중에 완전 물리식으로 가고 싶으면 이 줄을 줄이거나 제거하면 된다.
        angularVelocity += angleDiff(angleRadians, targetAngle) * ANGLE_ASSIST * dt

        angleRadians += angularVelocity * dt
        angularVelocity *= GROUND_ANGULAR_DAMPING

        velocityX += sin(targetAngle) * SLOPE_ACCEL * dt
        velocityX = velocityX.coerceIn(0f, MAX_SPEED)
    }

    private fun applySuspensionForce(sample: WheelSample, dt: Float) {
        if (!sample.isContacting) return

        val springAccel =
            sample.compression * SUSPENSION_STIFFNESS +
                    sample.compressionVelocity * SUSPENSION_DAMPING

        if (springAccel <= 0f) return

        // y축은 아래가 + 이므로, 위로 미는 힘은 velocityY를 감소시킨다.
        velocityY -= springAccel * dt

        // 뒤쪽 바퀴가 강하게 눌리면 차 앞이 내려가야 하고,
        // 앞쪽 바퀴가 강하게 눌리면 차 앞이 올라가야 한다.
        val normalizedX = sample.wheel.localX / WHEEL_HALF_DISTANCE
        angularVelocity += -springAccel * normalizedX * SUSPENSION_TORQUE_SCALE * dt
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
                screenY + rearWheel.anchorLocalY + rearWheel.suspensionLength,
                rearWheel.radius,
                wheelPaint,
            )

            drawCircle(
                screenX + frontWheel.localX,
                screenY + frontWheel.anchorLocalY + frontWheel.suspensionLength,
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

    private fun angleDiff(from: Float, to: Float): Float {
        var diff = to - from

        while (diff > Math.PI.toFloat()) {
            diff -= (Math.PI * 2.0).toFloat()
        }

        while (diff < -Math.PI.toFloat()) {
            diff += (Math.PI * 2.0).toFloat()
        }

        return diff
    }

    companion object {
        // frame
        private const val MAX_DT = 1f / 20f

        // horizontal movement
        private const val ENGINE_ACCEL = 1500f
        private const val BRAKE_ACCEL = 1100f
        private const val MAX_SPEED = 1800f

        private const val DRIVE_FRICTION = 0.997f
        private const val BRAKE_FRICTION = 0.985f
        private const val GROUND_FRICTION = 0.992f
        private const val AIR_FRICTION = 0.999f

        // vertical movement
        private const val GRAVITY = 1800f
        private const val MAX_FALL_SPEED = 2300f

        // fuel
        private const val FUEL_CONSUMPTION = 9f

        // wheel
        private const val WHEEL_HALF_DISTANCE = 58f
        private const val WHEEL_RADIUS = 26f

        // 기존 바퀴 중심 y가 42였으므로
        // anchor 18 + rest 24 = 42 정도로 맞춘다.
        private const val WHEEL_ANCHOR_OFFSET_Y = 18f

        // suspension
        private const val SUSPENSION_REST_LENGTH = 24f
        private const val SUSPENSION_MIN_LENGTH = 14f
        private const val SUSPENSION_MAX_LENGTH = 30f

        private const val SUSPENSION_STIFFNESS = 320f
        private const val SUSPENSION_DAMPING = 16f
        private const val SUSPENSION_TORQUE_SCALE = 0.010f
        private const val SUSPENSION_VISUAL_FOLLOW_SPEED = 40f

        // rotation
        private const val GROUND_ANGULAR_DAMPING = 0.90f
        private const val ANGLE_ASSIST = 6.0f
        private const val AIR_ANGULAR_DAMPING = 0.985f

        // terrain influence
        private const val SLOPE_ACCEL = 620f

        // game over
        private const val FLIP_DEAD_ANGLE = 115f
    }
}