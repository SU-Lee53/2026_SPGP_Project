package com.example.hillclimbracing.objects

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.Shader
import kr.ac.tukorea.ge.spgp2026.a2dg.objects.IGameObject
import kr.ac.tukorea.ge.spgp2026.a2dg.view.GameContext
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.max
import kotlin.math.min
import kotlin.random.Random

class HillTerrain(
    private val gctx: GameContext,
) : IGameObject {
    var cameraX = 0f

    private data class TerrainPoint(
        val x: Float,
        val y: Float,
    )

    private val points = ArrayList<TerrainPoint>()
    private val path = Path()

    private val random = Random(Random.nextInt())
    private var previousDeltaY = 0f

    private val soilPaint = Paint().apply {
        style = Paint.Style.FILL
        isAntiAlias = true
    }

    private val grassPaint = Paint().apply {
        color = Color.rgb(84, 166, 63)
        style = Paint.Style.FILL
        isAntiAlias = true
    }

    private val linePaint = Paint().apply {
        color = Color.rgb(39, 111, 45)
        style = Paint.Style.STROKE
        strokeWidth = 5f
        isAntiAlias = true
    }

    init {
        initializeTerrain()
    }

    override fun update(gctx: GameContext) {
        ensurePointsUntil(cameraX + gctx.metrics.width + LOOKAHEAD_DISTANCE)
        removeOldPoints(cameraX - REMOVE_BACK_DISTANCE)
    }

    override fun draw(canvas: Canvas) {
        if (points.size < 2) return

        val visibleLeft = min(0f, gctx.metrics.screenRect.left)
        val visibleRight = max(gctx.metrics.width, gctx.metrics.screenRect.right)
        val visibleBottom = max(gctx.metrics.height, gctx.metrics.screenRect.bottom)
        val startX = cameraX + visibleLeft - SEGMENT_WIDTH
        val endX = cameraX + visibleRight + SEGMENT_WIDTH

        ensurePointsUntil(endX + SEGMENT_WIDTH)

        drawTerrainSoil(canvas, startX, endX, visibleBottom)
        drawGrassCap(canvas, startX, endX)
        drawTerrainLine(canvas, startX, endX)
    }

    private fun drawTerrainSoil(canvas: Canvas, startX: Float, endX: Float, bottomY: Float) {
        path.reset()

        path.moveTo(startX - cameraX, getGroundY(startX))

        var x = startX + DRAW_SAMPLE_STEP
        while (x <= endX) {
            path.lineTo(x - cameraX, getGroundY(x))
            x += DRAW_SAMPLE_STEP
        }

        path.lineTo(endX - cameraX, getGroundY(endX))
        path.lineTo(endX - cameraX, bottomY)
        path.lineTo(startX - cameraX, bottomY)
        path.close()

        soilPaint.shader = LinearGradient(
            0f,
            MIN_Y,
            0f,
            bottomY,
            intArrayOf(
                Color.rgb(206, 113, 45),
                Color.rgb(172, 82, 34),
                Color.rgb(118, 57, 31),
            ),
            floatArrayOf(0f, 0.46f, 1f),
            Shader.TileMode.CLAMP,
        )
        canvas.drawPath(path, soilPaint)
    }

    private fun drawGrassCap(canvas: Canvas, startX: Float, endX: Float) {
        path.reset()

        path.moveTo(startX - cameraX, getGroundY(startX))

        var x = startX + DRAW_SAMPLE_STEP
        while (x <= endX) {
            path.lineTo(x - cameraX, getGroundY(x))
            x += DRAW_SAMPLE_STEP
        }

        path.lineTo(endX - cameraX, getGroundY(endX))

        x = endX
        while (x >= startX) {
            path.lineTo(x - cameraX, getGroundY(x) + GRASS_DEPTH)
            x -= DRAW_SAMPLE_STEP
        }

        path.lineTo(startX - cameraX, getGroundY(startX) + GRASS_DEPTH)
        path.close()

        canvas.drawPath(path, grassPaint)
    }

    private fun drawTerrainLine(canvas: Canvas, startX: Float, endX: Float) {
        path.reset()

        path.moveTo(startX - cameraX, getGroundY(startX))

        var x = startX + DRAW_SAMPLE_STEP
        while (x <= endX) {
            path.lineTo(x - cameraX, getGroundY(x))
            x += DRAW_SAMPLE_STEP
        }

        path.lineTo(endX - cameraX, getGroundY(endX))

        canvas.drawPath(path, linePaint)
    }

    fun getGroundY(worldX: Float): Float {
        if (worldX <= points.first().x) {
            return points.first().y
        }

        ensurePointsUntil(worldX + SEGMENT_WIDTH * 2f)

        val index = findSegmentIndex(worldX)
        val p0 = points[index]
        val p1 = points[index + 1]

        val t = ((worldX - p0.x) / (p1.x - p0.x)).coerceIn(0f, 1f)
        val s = smoothStep(t)

        return p0.y + (p1.y - p0.y) * s
    }

    fun getSlopeAngle(worldX: Float): Float {
        if (worldX <= points.first().x) {
            return 0f
        }

        ensurePointsUntil(worldX + SEGMENT_WIDTH * 2f)

        val index = findSegmentIndex(worldX)
        val p0 = points[index]
        val p1 = points[index + 1]

        val dx = p1.x - p0.x
        val dy = p1.y - p0.y

        val t = ((worldX - p0.x) / dx).coerceIn(0f, 1f)
        val slope = dy * smoothStepDerivative(t) / dx

        return atan2(slope, 1f)
    }

    private fun initializeTerrain() {
        points.clear()
        previousDeltaY = 0f

        points.add(TerrainPoint(0f, START_Y))
        points.add(TerrainPoint(SEGMENT_WIDTH, START_Y))

        ensurePointsUntil(gctx.metrics.width + LOOKAHEAD_DISTANCE)
    }

    private fun ensurePointsUntil(requiredX: Float) {
        while (points.last().x < requiredX) {
            addNextPoint()
        }
    }

    private fun addNextPoint() {
        val last = points.last()
        val difficulty = difficultyAt(last.x)
        val maxDeltaY = MAX_DELTA_Y * difficulty
        val momentum = (DELTA_MOMENTUM - (difficulty - 1f) * 0.10f).coerceAtLeast(0.30f)

        val rawDelta = random.nextFloat() * 2f - 1f
        var targetDeltaY = rawDelta * maxDeltaY

        val launchDropChance = LAUNCH_DROP_CHANCE * difficulty.coerceAtMost(1f)
        if (last.x > START_LAUNCH_DISTANCE &&
            previousDeltaY < -maxDeltaY * CREST_SETUP_SLOPE &&
            random.nextFloat() < launchDropChance
        ) {
            targetDeltaY = max(targetDeltaY, maxDeltaY * randomRange(0.62f, 1.0f))
        } else if (
            previousDeltaY > maxDeltaY * VALLEY_SETUP_SLOPE &&
            random.nextFloat() < VALLEY_RECOVERY_CHANCE
        ) {
            targetDeltaY = min(targetDeltaY, -maxDeltaY * randomRange(0.42f, 0.75f))
        }

        var deltaY = previousDeltaY * momentum + targetDeltaY * (1f - momentum)

        if (abs(previousDeltaY) > maxDeltaY * 0.65f && abs(deltaY) > abs(previousDeltaY)) {
            deltaY *= 0.75f
        }

        deltaY = deltaY.coerceIn(-maxDeltaY, maxDeltaY)

        var nextY = last.y + deltaY

        if (nextY < MIN_Y) {
            nextY = MIN_Y
            deltaY = abs(deltaY) * EDGE_BOUNCE
        } else if (nextY > MAX_Y) {
            nextY = MAX_Y
            deltaY = -abs(deltaY) * EDGE_BOUNCE
        }

        previousDeltaY = deltaY

        val nextX = last.x + SEGMENT_WIDTH
        points.add(TerrainPoint(nextX, nextY))
    }

    private fun difficultyAt(worldX: Float): Float {
        return (START_TERRAIN_DIFFICULTY + worldX / DIFFICULTY_DISTANCE_SCALE)
            .coerceAtMost(MAX_TERRAIN_DIFFICULTY)
    }

    private fun findSegmentIndex(worldX: Float): Int {
        if (points.size < 2) return 0
        if (worldX <= points.first().x) return 0

        var i = 0
        while (i < points.lastIndex) {
            if (worldX >= points[i].x && worldX <= points[i + 1].x) {
                return i
            }
            i++
        }

        return points.lastIndex - 1
    }

    private fun removeOldPoints(removeBeforeX: Float) {
        while (points.size > 3 && points[1].x < removeBeforeX) {
            points.removeAt(0)
        }
    }

    private fun smoothStep(t: Float): Float {
        return t * t * (3f - 2f * t)
    }

    private fun smoothStepDerivative(t: Float): Float {
        return 6f * t * (1f - t)
    }

    companion object {
        private const val START_Y = 1050f

        private const val SEGMENT_WIDTH = 220f
        private const val MAX_DELTA_Y = 126f
        private const val DELTA_MOMENTUM = 0.42f
        private const val START_TERRAIN_DIFFICULTY = 0.64f
        private const val DIFFICULTY_DISTANCE_SCALE = 7600f
        private const val MAX_TERRAIN_DIFFICULTY = 1.45f

        private const val START_LAUNCH_DISTANCE = 360f
        private const val LAUNCH_DROP_CHANCE = 0.46f
        private const val CREST_SETUP_SLOPE = 0.32f
        private const val VALLEY_SETUP_SLOPE = 0.56f
        private const val VALLEY_RECOVERY_CHANCE = 0.20f

        private const val LOOKAHEAD_DISTANCE = 1400f
        private const val REMOVE_BACK_DISTANCE = 800f

        private const val MIN_Y = 760f
        private const val MAX_Y = 1240f

        private const val EDGE_BOUNCE = 0.62f

        private const val DRAW_SAMPLE_STEP = 10f
        private const val GRASS_DEPTH = 58f
    }

    private fun randomRange(from: Float, to: Float): Float {
        return from + (to - from) * random.nextFloat()
    }
}
