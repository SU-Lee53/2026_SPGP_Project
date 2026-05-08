package com.example.hillclimbracing.objects

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import kr.ac.tukorea.ge.spgp2026.a2dg.objects.IGameObject
import kr.ac.tukorea.ge.spgp2026.a2dg.view.GameContext
import kotlin.math.abs
import kotlin.math.atan2
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

    private val fillPaint = Paint().apply {
        color = Color.rgb(95, 170, 80)
        style = Paint.Style.FILL
        isAntiAlias = true
    }

    private val linePaint = Paint().apply {
        color = Color.rgb(45, 110, 45)
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

        val startX = cameraX - SEGMENT_WIDTH
        val endX = cameraX + gctx.metrics.width + SEGMENT_WIDTH

        ensurePointsUntil(endX + SEGMENT_WIDTH)

        drawTerrainFill(canvas, startX, endX)
        drawTerrainLine(canvas, startX, endX)
    }

    private fun drawTerrainFill(canvas: Canvas, startX: Float, endX: Float) {
        path.reset()

        path.moveTo(startX - cameraX, getGroundY(startX))

        var x = startX + DRAW_SAMPLE_STEP
        while (x <= endX) {
            path.lineTo(x - cameraX, getGroundY(x))
            x += DRAW_SAMPLE_STEP
        }

        path.lineTo(endX - cameraX, getGroundY(endX))
        path.lineTo(endX - cameraX, gctx.metrics.height)
        path.lineTo(startX - cameraX, gctx.metrics.height)
        path.close()

        canvas.drawPath(path, fillPaint)
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
        ensurePointsUntil(worldX + SEGMENT_WIDTH * 2f)

        val index = findSegmentIndex(worldX)
        val p0 = points[index]
        val p1 = points[index + 1]

        val t = ((worldX - p0.x) / (p1.x - p0.x)).coerceIn(0f, 1f)
        val s = smoothStep(t)

        return p0.y + (p1.y - p0.y) * s
    }

    fun getSlopeAngle(worldX: Float): Float {
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

        val rawDelta = random.nextFloat() * 2f - 1f
        val targetDeltaY = rawDelta * MAX_DELTA_Y

        var deltaY = previousDeltaY * DELTA_MOMENTUM + targetDeltaY * (1f - DELTA_MOMENTUM)

        if (abs(previousDeltaY) > MAX_DELTA_Y * 0.65f && abs(deltaY) > abs(previousDeltaY)) {
            deltaY *= 0.75f
        }

        deltaY = deltaY.coerceIn(-MAX_DELTA_Y, MAX_DELTA_Y)

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

    private fun findSegmentIndex(worldX: Float): Int {
        if (points.size < 2) return 0

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

        private const val SEGMENT_WIDTH = 200f
        private const val MAX_DELTA_Y = 124f
        private const val DELTA_MOMENTUM = 0.43f

        private const val LOOKAHEAD_DISTANCE = 1400f
        private const val REMOVE_BACK_DISTANCE = 800f

        private const val MIN_Y = 760f
        private const val MAX_Y = 1240f

        private const val EDGE_BOUNCE = 0.62f

        private const val DRAW_SAMPLE_STEP = 10f
    }
}