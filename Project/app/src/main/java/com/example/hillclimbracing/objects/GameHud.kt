package com.example.hillclimbracing.objects

import android.graphics.Canvas
import android.graphics.Color
import kr.ac.tukorea.ge.spgp2026.a2dg.objects.IGameObject
import kr.ac.tukorea.ge.spgp2026.a2dg.util.LabelUtil
import kr.ac.tukorea.ge.spgp2026.a2dg.view.GameContext

class GameHud(private val car: Player) : IGameObject {
    private val label = LabelUtil(
        textSize = 42f,
        color = Color.BLACK,
    )

    override fun update(gctx: GameContext) {
    }

    override fun draw(canvas: Canvas) {
        label.draw(canvas, "Distance: ${car.distance.toInt()}m", 40f, 80f)
        label.draw(canvas, "Fuel: ${car.fuel.toInt()}%", 40f, 135f)
        label.draw(canvas, "Left: Brake / Right: Gas", 40f, 1510f)
    }
}