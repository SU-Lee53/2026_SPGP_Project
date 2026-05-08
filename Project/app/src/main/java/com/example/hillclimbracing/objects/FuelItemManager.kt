package com.example.hillclimbracing.objects

import android.graphics.Canvas
import android.graphics.RectF
import kr.ac.tukorea.ge.spgp2026.a2dg.objects.IGameObject
import kr.ac.tukorea.ge.spgp2026.a2dg.view.GameContext

class FuelItemManager(
    private val terrain: HillTerrain,
    private val player: Player,
) : IGameObject {
    var cameraX = 0f

    private val items = ArrayList<FuelItem>()

    private val playerRect = RectF()
    private val itemRect = RectF()

    private var nextSpawnX = 700f

    override fun update(gctx: GameContext) {
        spawnItemsAhead()
        checkCollision()
        removeOldItems()
    }

    override fun draw(canvas: Canvas) {
        for (item in items) {
            item.draw(canvas, cameraX)
        }
    }

    private fun spawnItemsAhead() {
        val requiredX = player.worldX + SPAWN_LOOKAHEAD_DISTANCE

        while (nextSpawnX < requiredX) {
            val groundY = terrain.getGroundY(nextSpawnX)

            items.add(
                FuelItem(
                    worldX = nextSpawnX,
                    y = groundY - ITEM_HEIGHT_FROM_GROUND,
                )
            )

            nextSpawnX += SPAWN_INTERVAL
        }
    }

    private fun checkCollision() {
        player.getCollisionRect(playerRect)

        for (item in items) {
            if (item.collected) continue

            item.getCollisionRect(itemRect)

            if (RectF.intersects(playerRect, itemRect)) {
                item.collected = true
                player.addFuel(FuelItem.FUEL_AMOUNT)
            }
        }
    }

    private fun removeOldItems() {
        val removeBeforeX = player.worldX - REMOVE_BACK_DISTANCE

        var i = items.lastIndex
        while (i >= 0) {
            val item = items[i]
            if (item.collected || item.worldX < removeBeforeX) {
                items.removeAt(i)
            }
            i--
        }
    }

    companion object {
        private const val SPAWN_INTERVAL = 900f
        private const val SPAWN_LOOKAHEAD_DISTANCE = 1800f
        private const val REMOVE_BACK_DISTANCE = 800f
        private const val ITEM_HEIGHT_FROM_GROUND = 90f
    }
}