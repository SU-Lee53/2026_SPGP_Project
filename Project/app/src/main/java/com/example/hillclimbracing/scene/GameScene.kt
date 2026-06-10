package com.example.hillclimbracing.scene

import android.graphics.Canvas
import android.graphics.Color
import android.view.MotionEvent
import com.example.hillclimbracing.objects.FuelItemManager
import com.example.hillclimbracing.objects.GameHud
import com.example.hillclimbracing.objects.GameProgress
import com.example.hillclimbracing.objects.HillTerrain
import com.example.hillclimbracing.objects.Player
import com.example.hillclimbracing.objects.TouchDriveInput
import kr.ac.tukorea.ge.spgp2026.a2dg.objects.IGameObject
import kr.ac.tukorea.ge.spgp2026.a2dg.scene.Scene
import kr.ac.tukorea.ge.spgp2026.a2dg.scene.World
import kr.ac.tukorea.ge.spgp2026.a2dg.view.GameContext

class GameScene(
    gctx: GameContext,
) : Scene(gctx) {
    enum class Layer {
        TERRAIN,
        ITEM,
        PLAYER,
        UI,
        TOUCH,
    }

    override val world = World(Layer.entries.toTypedArray())

    private val terrain = HillTerrain(gctx)
    private val player = Player(gctx, terrain)
    private val hud = GameHud(gctx, player)
    private val fuelItemManager = FuelItemManager(gctx, terrain, player)
    private val touchDriveInput = TouchDriveInput(gctx, player)

    var cameraX = 0f
        private set

    init {
        world.add(terrain, Layer.TERRAIN)
        world.add(player, Layer.PLAYER)
        world.add(hud, Layer.UI)
        world.add(fuelItemManager, Layer.ITEM)
        world.add(touchDriveInput, Layer.TOUCH)
    }

    override fun update(gctx: GameContext) {
        super.update(gctx)

        cameraX = (player.worldX - 300f).coerceAtLeast(0f)

        terrain.cameraX = cameraX
        player.cameraX = cameraX
        fuelItemManager.cameraX = cameraX

        if (player.isDead) {
            val distance = player.distance.toInt()
            val bestDistance = GameProgress.updateBestDistance(gctx.view.context, distance)
            GameOverScene(gctx, distance, bestDistance).change()
        }
    }

    override fun draw(canvas: Canvas) {
        canvas.drawColor(Color.rgb(180, 220, 255))
        super.draw(canvas)
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        super.onTouchEvent(event)
        return true
    }

    protected override fun touchObjects(): List<IGameObject> {
        return world.objectsAt(Layer.TOUCH)
    }

    override fun onBackPressed(): Boolean {
        PauseScene(gctx).push()
        return true
    }
}
