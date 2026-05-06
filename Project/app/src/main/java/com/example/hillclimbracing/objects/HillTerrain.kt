package com.example.hillclimbracing.objects

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import kr.ac.tukorea.ge.spgp2026.a2dg.objects.IGameObject
import kr.ac.tukorea.ge.spgp2026.a2dg.view.GameContext
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

    private val random = Random(SEED)

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

        val startIndex = findVisibleStartIndex(cameraX - SEGMENT_WIDTH)
        val endX = cameraX + gctx.metrics.width + SEGMENT_WIDTH

        path.reset()

        val first = points[startIndex]
        path.moveTo(first.x - cameraX, first.y)

        var i = startIndex + 1
        while (i < points.size && points[i].x <= endX) {
            val p = points[i]
            path.lineTo(p.x - cameraX, p.y)
            i++
        }

        val last = points[(i - 1).coerceAtLeast(startIndex)]
        path.lineTo(last.x - cameraX, gctx.metrics.height)
        path.lineTo(first.x - cameraX, gctx.metrics.height)
        path.close()

        canvas.drawPath(path, fillPaint)

        path.reset()
        path.moveTo(first.x - cameraX, first.y)

        i = startIndex + 1
        while (i < points.size && points[i].x <= endX) {
            val p = points[i]
            path.lineTo(p.x - cameraX, p.y)
            i++
        }

        canvas.drawPath(path, linePaint)
    }

    fun getGroundY(worldX: Float): Float {
        ensurePointsUntil(worldX + SEGMENT_WIDTH * 2f)

        val index = findSegmentIndex(worldX)
        val p0 = points[index]
        val p1 = points[index + 1]

        val t = ((worldX - p0.x) / (p1.x - p0.x)).coerceIn(0f, 1f)
        return p0.y + (p1.y - p0.y) * t
    }

    fun getSlopeAngle(worldX: Float): Float {
        ensurePointsUntil(worldX + SEGMENT_WIDTH * 2f)

        val index = findSegmentIndex(worldX)
        val p0 = points[index]
        val p1 = points[index + 1]

        return atan2(
            p1.y - p0.y,
            p1.x - p0.x,
        )
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

        // 이전 변화량과 섞어서 급격한 꺾임을 줄인다.
        val deltaY = previousDeltaY * 0.55f + targetDeltaY * 0.45f
        previousDeltaY = deltaY

        val nextX = last.x + SEGMENT_WIDTH
        val nextY = (last.y + deltaY).coerceIn(MIN_Y, MAX_Y)

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

    private fun findVisibleStartIndex(startX: Float): Int {
        var i = 0
        while (i < points.lastIndex && points[i + 1].x < startX) {
            i++
        }
        return i
    }

    private fun removeOldPoints(removeBeforeX: Float) {
        // getGroundY가 현재 화면 조금 뒤쪽도 참조할 수 있으므로 최소 2개는 남긴다.
        while (points.size > 3 && points[1].x < removeBeforeX) {
            points.removeAt(0)
        }
    }

    companion object {
        private const val SEED = 20260507

        private const val START_Y = 1050f

        private const val SEGMENT_WIDTH = 220f
        private const val LOOKAHEAD_DISTANCE = 1200f
        private const val REMOVE_BACK_DISTANCE = 800f

        private const val MIN_Y = 850f
        private const val MAX_Y = 1180f
        private const val MAX_DELTA_Y = 85f
    }
}