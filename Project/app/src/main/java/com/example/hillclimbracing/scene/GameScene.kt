package com.example.hillclimbracing.scene

import android.graphics.Canvas
import android.graphics.Color
import com.example.hillclimbracing.objects.Player
import kr.ac.tukorea.ge.spgp2026.a2dg.objects.IGameObject
import kr.ac.tukorea.ge.spgp2026.a2dg.scene.Scene
import kr.ac.tukorea.ge.spgp2026.a2dg.scene.World
import kr.ac.tukorea.ge.spgp2026.a2dg.view.GameContext
import com.example.hillclimbracing.objects.GameHud
import com.example.hillclimbracing.objects.TouchDriveInput

class MainScene(
    gctx: GameContext,
) : Scene(gctx) {
    enum class Layer {
        BACKGROUND,
        TERRAIN,
        ITEM,
        PLAYER,
        UI,
        TOUCH,
    }

    override val world = World(Layer.entries.toTypedArray())

    private val car = Player(gctx)
    private val hud = GameHud(car)
    private val input = TouchDriveInput(gctx, car)

    init {
        world.add(car, Layer.PLAYER)
        world.add(hud, Layer.UI)
        world.add(input, Layer.TOUCH)
    }

    override fun draw(canvas: Canvas) {
        canvas.drawColor(Color.rgb(180, 220, 255))
        super.draw(canvas)
    }

    override fun update(gctx: GameContext) {
        super.update(gctx)

        if (car.isDead) {
            GameOverScene(gctx, car.distance.toInt()).change()
        }
    }

    override fun touchObjects(): List<IGameObject>? {
        return world.objectsAt(Layer.TOUCH)
    }
}