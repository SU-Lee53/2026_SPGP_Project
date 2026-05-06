package com.example.hillclimbracing.scene

import android.graphics.Canvas
import android.graphics.Color
import android.view.MotionEvent
import com.example.hillclimbracing.objects.Player
import kr.ac.tukorea.ge.spgp2026.a2dg.objects.IGameObject
import kr.ac.tukorea.ge.spgp2026.a2dg.scene.Scene
import kr.ac.tukorea.ge.spgp2026.a2dg.scene.World
import kr.ac.tukorea.ge.spgp2026.a2dg.view.GameContext
import com.example.hillclimbracing.objects.GameHud
import com.example.hillclimbracing.objects.HillTerrain
import com.example.hillclimbracing.objects.TouchDriveInput

class GameScene(
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

    private val terrain = HillTerrain(gctx)
    private val player = Player(gctx, terrain)
    private val hud = GameHud(player)

    var cameraX = 0f
        private set

    init {
        world.add(terrain, Layer.TERRAIN)
        world.add(player, Layer.PLAYER)
        world.add(hud, Layer.UI)
    }

    override fun update(gctx: GameContext) {
        super.update(gctx)

        cameraX = (player.worldX - 300f).coerceAtLeast(0f)

        terrain.cameraX = cameraX
        player.cameraX = cameraX

        if (player.isDead) {
            GameOverScene(gctx, player.distance.toInt()).change()
        }
    }

    override fun draw(canvas: Canvas) {
        canvas.drawColor(Color.rgb(180, 220, 255))
        super.draw(canvas)
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        handleDriveTouch(event)
        return true
    }

    private fun handleDriveTouch(event: MotionEvent) {
        var accelerating = false
        var braking = false

        val action = event.actionMasked
        val actionIndex = event.actionIndex

        for (i in 0 until event.pointerCount) {
            val released =
                (action == MotionEvent.ACTION_POINTER_UP || action == MotionEvent.ACTION_UP) &&
                        i == actionIndex

            if (released) continue

            val point = gctx.metrics.fromScreen(event.getX(i), event.getY(i))

            if (point.x < gctx.metrics.width / 2f) {
                braking = true
            } else {
                accelerating = true
            }
        }

        if (action == MotionEvent.ACTION_UP || action == MotionEvent.ACTION_CANCEL) {
            accelerating = false
            braking = false
        }

        player.isAccelerating = accelerating
        player.isBraking = braking
    }
}