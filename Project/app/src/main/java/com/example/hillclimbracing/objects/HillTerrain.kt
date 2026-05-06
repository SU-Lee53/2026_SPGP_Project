package com.example.hillclimbracing.objects

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import kr.ac.tukorea.ge.spgp2026.a2dg.objects.IGameObject
import kr.ac.tukorea.ge.spgp2026.a2dg.view.GameContext
import kotlin.math.atan2

class HillTerrain(
    private val gctx: GameContext,
) : IGameObject {
    var cameraX = 0f

    private data class Point(
        val x: Float,
        val y: Float,
    )

    private val points = mutableListOf<Point>()

    private val terrainPaint = Paint().apply {
        color = Color.rgb(90, 170, 80)
        style = Paint.Style.FILL
        isAntiAlias = true
    }

    private val linePaint = Paint().apply {
        color = Color.rgb(50, 120, 50)
        style = Paint.Style.STROKE
        strokeWidth = 5f
        isAntiAlias = true
    }

    private val path = Path()

    init {
        points.add(Point(0f, 1050f))
        points.add(Point(250f, 1020f))
        points.add(Point(500f, 1080f))
        points.add(Point(750f, 970f))
        points.add(Point(1000f, 1010f))
        points.add(Point(1250f, 930f))
        points.add(Point(1500f, 1040f))
        points.add(Point(1750f, 1000f))
        points.add(Point(2000f, 1100f))
        points.add(Point(2250f, 980f))
        points.add(Point(2500f, 1020f))
    }

    override fun update(gctx: GameContext) {
        ensurePoints(cameraX + gctx.metrics.width + 500f)
    }

    override fun draw(canvas: Canvas) {
        if (points.size < 2) return

        path.reset()

        val first = points.first()
        path.moveTo(first.x - cameraX, first.y)

        for (i in 1 until points.size) {
            val p = points[i]
            path.lineTo(p.x - cameraX, p.y)
        }

        val last = points.last()
        path.lineTo(last.x - cameraX, gctx.metrics.height)
        path.lineTo(first.x - cameraX, gctx.metrics.height)
        path.close()

        canvas.drawPath(path, terrainPaint)

        path.reset()
        path.moveTo(first.x - cameraX, first.y)
        for (i in 1 until points.size) {
            val p = points[i]
            path.lineTo(p.x - cameraX, p.y)
        }
        canvas.drawPath(path, linePaint)
    }

    fun getGroundY(worldX: Float): Float {
        val index = findSegmentIndex(worldX)
        val p0 = points[index]
        val p1 = points[index + 1]

        val t = ((worldX - p0.x) / (p1.x - p0.x)).coerceIn(0f, 1f)
        return p0.y + (p1.y - p0.y) * t
    }

    fun getSlopeAngle(worldX: Float): Float {
        val index = findSegmentIndex(worldX)
        val p0 = points[index]
        val p1 = points[index + 1]

        return atan2(p1.y - p0.y, p1.x - p0.x)
    }

    private fun findSegmentIndex(worldX: Float): Int {
        ensurePoints(worldX + 500f)

        for (i in 0 until points.lastIndex) {
            if (worldX >= points[i].x && worldX <= points[i + 1].x) {
                return i
            }
        }

        return points.lastIndex - 1
    }

    private fun ensurePoints(requiredX: Float) {
        while (points.last().x < requiredX) {
            val last = points.last()
            val nextX = last.x + 250f

            val wave = when ((points.size % 6)) {
                0 -> -70f
                1 -> 50f
                2 -> -110f
                3 -> 80f
                4 -> -40f
                else -> 90f
            }

            val nextY = (last.y + wave).coerceIn(850f, 1180f)
            points.add(Point(nextX, nextY))
        }
    }
}