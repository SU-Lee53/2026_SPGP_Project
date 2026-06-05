package com.example.hillclimbracing.objects

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import com.example.hillclimbracing.R
import kr.ac.tukorea.ge.spgp2026.a2dg.objects.IGameObject
import kr.ac.tukorea.ge.spgp2026.a2dg.objects.ImageNumber
import kr.ac.tukorea.ge.spgp2026.a2dg.util.LabelUtil
import kr.ac.tukorea.ge.spgp2026.a2dg.view.GameContext

class GameHud(
    gctx: GameContext,
    private val car: Player,
) : IGameObject {
    private val panelBitmap: Bitmap = gctx.res.getBitmap(R.mipmap.classic_hud_panel)
    private val panelRect = RectF(18f, 22f, 882f, 243f)

    private val odometer = ImageNumber(
        gctx = gctx,
        mipmapId = R.mipmap.odometer_digits,
        right = 510f,
        top = 93f,
        dstCharWidth = 64f,
        minDigits = 6,
    )

    private val hintLabel = LabelUtil(
        textSize = 30f,
        color = Color.rgb(28, 36, 42),
    )

    private val needlePaint = Paint().apply {
        color = Color.rgb(235, 52, 40)
        strokeWidth = 7f
        strokeCap = Paint.Cap.ROUND
        isAntiAlias = true
    }

    override fun update(gctx: GameContext) {
        odometer.value = car.distance.toInt()
        odometer.update(gctx)
    }

    override fun draw(canvas: Canvas) {
        canvas.drawBitmap(panelBitmap, null, panelRect, null)
        odometer.draw(canvas)
        drawFuelNeedle(canvas)

        hintLabel.draw(canvas, "Left: Brake / Right: Gas", 40f, 1510f)
    }

    private fun drawFuelNeedle(canvas: Canvas) {
        val fuelRatio = (car.fuel / 100f).coerceIn(0f, 1f)
        val topY = 84f
        val bottomY = 184f
        val needleY = bottomY - (bottomY - topY) * fuelRatio

        canvas.drawLine(704f, needleY, 764f, needleY, needlePaint)
        canvas.drawLine(764f, needleY, 782f, needleY - 10f, needlePaint)
        canvas.drawLine(764f, needleY, 782f, needleY + 10f, needlePaint)
    }
}
