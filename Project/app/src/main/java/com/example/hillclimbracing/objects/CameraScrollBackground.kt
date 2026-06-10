package com.example.hillclimbracing.objects

import android.graphics.Canvas
import kr.ac.tukorea.ge.spgp2026.a2dg.objects.HorzScrollBackground
import kr.ac.tukorea.ge.spgp2026.a2dg.view.GameContext
import kotlin.math.max
import kotlin.math.min

class CameraScrollBackground(
    private val gctx: GameContext,
    resId: Int,
    private val scrollRatio: Float,
) : HorzScrollBackground(gctx, resId, 0f) {
    var cameraX = 0f
        set(value) {
            field = value
            x = -value * scrollRatio
        }

    override fun draw(canvas: Canvas) {
        val visibleLeft = min(0f, gctx.metrics.screenRect.left)
        val visibleRight = max(gctx.metrics.width, gctx.metrics.screenRect.right)
        val visibleTop = min(0f, gctx.metrics.screenRect.top)
        val visibleBottom = max(gctx.metrics.height, gctx.metrics.screenRect.bottom)
        val drawHeight = visibleBottom - visibleTop
        val tileWidth = bitmapWidth * drawHeight / bitmapHeight.toFloat()

        var curr = x % tileWidth
        if (curr > 0f) curr -= tileWidth
        while (curr > visibleLeft) {
            curr -= tileWidth
        }
        while (curr < visibleRight) {
            dstRect.set(curr, visibleTop, curr + tileWidth, visibleBottom)
            canvas.drawBitmap(bitmap, null, dstRect, null)
            curr += tileWidth
        }
    }
}
