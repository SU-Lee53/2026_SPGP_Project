package com.example.hillclimbracing.objects

import androidx.core.graphics.withRotation
import kr.ac.tukorea.ge.spgp2026.a2dg.objects.IGameObject
import kr.ac.tukorea.ge.spgp2026.a2dg.view.GameContext
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.sin
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.RectF
import com.example.hillclimbracing.R

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

    private enum class BodySampleType {
        BOTTOM,
        FRONT,
        REAR,
        ROOF,
    }

    private data class BodySample(
        val localX: Float,
        val localY: Float,
        val type: BodySampleType,
    )
    private val bodySamples = arrayOf(
        BodySample(-75f, 42f, BodySampleType.BOTTOM),
        BodySample(0f, 48f, BodySampleType.BOTTOM),
        BodySample(75f, 42f, BodySampleType.BOTTOM),

        BodySample(122f, -8f, BodySampleType.FRONT),
        BodySample(126f, 24f, BodySampleType.FRONT),

        BodySample(-122f, -8f, BodySampleType.REAR),
        BodySample(-126f, 24f, BodySampleType.REAR),

        BodySample(-85f, -62f, BodySampleType.ROOF),
        BodySample(0f, -72f, BodySampleType.ROOF),
        BodySample(85f, -62f, BodySampleType.ROOF),
    )

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

    private val bodyBitmap: Bitmap = gctx.res.getBitmap(R.mipmap.player_body)
    private val wheelBitmap: Bitmap = gctx.res.getBitmap(R.mipmap.player_wheel)

    private val bodyDstRect = RectF()
    private val wheelDstRect = RectF()

    private var wheelRotationDegrees = 0f

    fun getCollisionRect(out: RectF): RectF {
        out.set(
            worldX - 90f,
            y - 50f,
            worldX + 90f,
            y + 55f,
        )
        return out
    }

    fun addFuel(amount: Float) {
        fuel = (fuel + amount).coerceAtMost(MAX_FUEL)
    }

    override fun update(gctx: GameContext) {
        if (isDead) return

        val dt = gctx.frameTime.coerceAtMost(MAX_DT)
        if (dt <= 0f) return

        updateHorizontalMovement(dt)
        updateVerticalMovement(dt)
        resolveWheelGroundContact(dt)
        resolveBodyTerrainCollision()
        updateWheelRotation(dt)

        distance = worldX / 10f

        if (fuel <= 0f) {
            fuel = 0f
            isDead = true
        }

        val angleDegrees = Math.toDegrees(angleRadians.toDouble()).toFloat()
        if (isGrounded && abs(angleDegrees) > FLIP_DEAD_ANGLE) {
            isDead = true
        }
    }

    private fun resolveBodyTerrainCollision() {
        var maxBottomPenetration = 0f
        var frontHit = false
        var rearHit = false

        for (sample in bodySamples) {
            val px = localToWorldX(sample.localX, sample.localY)
            val py = localToWorldY(sample.localX, sample.localY)
            val groundY = terrain.getGroundY(px)

            // y축은 아래가 +.
            // py가 groundY보다 크면 지형 아래로 들어간 것.
            val penetration = py - groundY

            if (penetration <= BODY_COLLISION_MARGIN) {
                continue
            }

            when (sample.type) {
                BodySampleType.ROOF -> {
                    // 지붕은 바로 죽이면 됨.
                    isDead = true
                    return
                }

                BodySampleType.FRONT -> {
                    frontHit = true
                    maxBottomPenetration = max(maxBottomPenetration, penetration)
                }

                BodySampleType.REAR -> {
                    rearHit = true
                    maxBottomPenetration = max(maxBottomPenetration, penetration)
                }

                BodySampleType.BOTTOM -> {
                    maxBottomPenetration = max(maxBottomPenetration, penetration)
                }
            }
        }

        if (maxBottomPenetration <= 0f) {
            return
        }

        // 차체가 지형에 파묻힌 정도만큼 살짝 위로 빼낸다.
        // 한 번에 전부 빼내면 튀어오르므로 일부만 보정한다.
        val pushAmount = maxBottomPenetration * BODY_PUSH_OUT_RATIO

        if (frontHit) {
            // 앞부분이 오르막에 박힌 경우: 위로만 빼면 계속 낀다.
            // 약간 뒤로 밀어내서 경사면에서 빠져나오게 한다.
            worldX -= pushAmount * FRONT_HIT_BACK_PUSH
            y -= pushAmount
        } else if (rearHit) {
            // 뒤쪽이 박힌 경우는 앞으로 약간 밀어낸다.
            worldX += pushAmount * REAR_HIT_FORWARD_PUSH
            y -= pushAmount
        } else {
            y -= pushAmount
        }

        // 급경사에 앞부분이 박혔으면 즉사 대신 강한 감속.
        if (frontHit) {
            velocityX *= FRONT_HIT_SPEED_REMAIN

            if (velocityY > 0f) {
                velocityY *= 0.25f
            }

            angularVelocity += FRONT_HIT_ANGULAR_IMPULSE
        }

        if (rearHit) {
            velocityX *= REAR_HIT_SPEED_REMAIN
            velocityY *= 0.55f

            // 뒤가 박히면 반대쪽 회전.
            angularVelocity -= REAR_HIT_ANGULAR_IMPULSE
        }

        // 바닥 긁힘은 속도만 약간 줄임.
        if (!frontHit && !rearHit) {
            velocityX *= BODY_SCRAPE_SPEED_REMAIN
            if (velocityY > 0f) {
                velocityY *= 0.25f
            }
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

    private fun bodyPointToWorldX(localX: Float, localY: Float): Float {
        val cosA = cos(angleRadians)
        val sinA = sin(angleRadians)

        return worldX + localX * cosA - localY * sinA
    }

    private fun bodyPointToWorldY(localX: Float, localY: Float): Float {
        val cosA = cos(angleRadians)
        val sinA = sin(angleRadians)

        return y + localX * sinA + localY * cosA
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

        val isContacting = distanceToGround <= SUSPENSION_MAX_LENGTH &&
                distanceToGround >= -MAX_WHEEL_PENETRATION

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
            applyAirRotationControl(dt)

            angleRadians += angularVelocity * dt
            angularVelocity = angularVelocity.coerceIn(
                -MAX_AIR_ANGULAR_VELOCITY,
                MAX_AIR_ANGULAR_VELOCITY,
            )
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

        applySlopeForce(targetAngle, dt)
    }

    private fun applyAirRotationControl(dt: Float) {
        if (isAccelerating && fuel > 0f) {
            // 가속: 뒷바퀴를 들어 올리는 방향
            angularVelocity += AIR_ROTATION_ACCEL * dt
        }

        if (isBraking) {
            // 브레이크: 뒷바퀴를 내리는 방향
            angularVelocity -= AIR_ROTATION_ACCEL * dt
        }

        // 공중에서 아무 입력도 없으면 천천히 회전이 줄어들게 한다.
        if (!isAccelerating && !isBraking) {
            angularVelocity *= AIR_NEUTRAL_DAMPING
        }
    }

    private fun applySlopeForce(targetAngle: Float, dt: Float) {
        val slope = sin(targetAngle)

        val slopeAccel = if (slope < 0f) {
            // 오르막: 더 강하게 감속
            slope * UPHILL_RESIST_ACCEL
        } else {
            // 내리막: 너무 폭주하지 않게 약하게 가속
            slope * DOWNHILL_ACCEL
        }

        velocityX += slopeAccel * dt
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
            drawBodySprite(screenX, screenY)

            drawWheelSprite(
                screenX + rearWheel.localX,
                screenY + rearWheel.anchorLocalY + rearWheel.suspensionLength + WHEEL_DRAW_OFFSET_Y,
            )

            drawWheelSprite(
                screenX + frontWheel.localX,
                screenY + frontWheel.anchorLocalY + frontWheel.suspensionLength + WHEEL_DRAW_OFFSET_Y,
            )
        }
    }

    private fun Canvas.drawBodySprite(screenX: Float, screenY: Float) {
        bodyDstRect.set(
            screenX - BODY_DRAW_WIDTH / 2f + BODY_DRAW_OFFSET_X,
            screenY - BODY_DRAW_HEIGHT / 2f + BODY_DRAW_OFFSET_Y,
            screenX + BODY_DRAW_WIDTH / 2f + BODY_DRAW_OFFSET_X,
            screenY + BODY_DRAW_HEIGHT / 2f + BODY_DRAW_OFFSET_Y,
        )

        drawBitmap(bodyBitmap, null, bodyDstRect, null)
    }

    private fun Canvas.drawWheelSprite(centerX: Float, centerY: Float) {
        withRotation(wheelRotationDegrees, centerX, centerY) {
            wheelDstRect.set(
                centerX - WHEEL_DRAW_RADIUS,
                centerY - WHEEL_DRAW_RADIUS,
                centerX + WHEEL_DRAW_RADIUS,
                centerY + WHEEL_DRAW_RADIUS,
            )

            drawBitmap(wheelBitmap, null, wheelDstRect, null)
        }
    }

    private fun updateWheelRotation(dt: Float) {
        if (velocityX <= 0f) return

        val radians = velocityX * dt / WHEEL_RADIUS
        wheelRotationDegrees += Math.toDegrees(radians.toDouble()).toFloat()
    }

    private fun localToWorldX(localX: Float, localY: Float): Float {
        val cosA = cos(angleRadians)
        val sinA = sin(angleRadians)
        return worldX + localX * cosA - localY * sinA
    }

    private fun localToWorldY(localX: Float, localY: Float): Float {
        val cosA = cos(angleRadians)
        val sinA = sin(angleRadians)
        return y + localX * sinA + localY * cosA
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
        private const val ENGINE_ACCEL = 900f
        private const val BRAKE_ACCEL = 850f
        private const val MAX_SPEED = 1000f

        private const val DRIVE_FRICTION = 0.995f
        private const val BRAKE_FRICTION = 0.982f
        private const val GROUND_FRICTION = 0.988f
        private const val AIR_FRICTION = 0.997f

        // vertical movement
        private const val GRAVITY = 1800f
        private const val MAX_FALL_SPEED = 2300f

        // fuel
        private const val MAX_FUEL = 100f
        private const val FUEL_CONSUMPTION = 9f

        // wheel
        private const val MAX_WHEEL_PENETRATION = 35f
        private const val WHEEL_HALF_DISTANCE = 70f
        private const val WHEEL_RADIUS = 40f

        private const val WHEEL_ANCHOR_OFFSET_Y = 16f

        private const val SUSPENSION_REST_LENGTH = 24f
        private const val SUSPENSION_MIN_LENGTH = 12f
        private const val SUSPENSION_MAX_LENGTH = 34f

        private const val SUSPENSION_STIFFNESS = 320f
        private const val SUSPENSION_DAMPING = 16f
        private const val SUSPENSION_TORQUE_SCALE = 0.010f
        private const val SUSPENSION_VISUAL_FOLLOW_SPEED = 40f

        // rotation
        private const val GROUND_ANGULAR_DAMPING = 0.90f
        private const val ANGLE_ASSIST = 6.0f
        private const val AIR_ANGULAR_DAMPING = 0.985f

        // terrain influence
        private const val UPHILL_RESIST_ACCEL = 1250f
        private const val DOWNHILL_ACCEL = 420f

        // game over
        private const val FLIP_DEAD_ANGLE = 100f

        // air control
        private const val AIR_ROTATION_ACCEL = 11.0f
        private const val MAX_AIR_ANGULAR_VELOCITY = 7.0f
        private const val AIR_NEUTRAL_DAMPING = 0.985f

        // Body
        private const val BODY_COLLISION_MARGIN = 5f

        private const val FRONT_HIT_SPEED_REMAIN = 0.68f
        private const val REAR_HIT_SPEED_REMAIN = 0.60f
        private const val BODY_SCRAPE_SPEED_REMAIN = 0.82f

        private const val FRONT_HIT_ANGULAR_IMPULSE = 0.9f
        private const val REAR_HIT_ANGULAR_IMPULSE = 0.8f
        private const val BODY_PUSH_OUT_RATIO = 0.85f
        private const val FRONT_HIT_BACK_PUSH = 0.45f
        private const val REAR_HIT_FORWARD_PUSH = 0.25f
        private const val BODY_DRAW_WIDTH = 300f
        private const val BODY_DRAW_HEIGHT = 180f
        private const val BODY_DRAW_OFFSET_X = 3f
        private const val BODY_DRAW_OFFSET_Y = -3f

        private const val WHEEL_DRAW_RADIUS = 44f
        private const val WHEEL_DRAW_OFFSET_Y = 5f
    }
}