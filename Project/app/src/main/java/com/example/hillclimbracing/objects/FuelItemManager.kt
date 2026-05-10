package com.example.hillclimbracing.objects

import android.graphics.Canvas
import android.graphics.RectF
import kr.ac.tukorea.ge.spgp2026.a2dg.objects.IGameObject
import kr.ac.tukorea.ge.spgp2026.a2dg.view.GameContext
import kotlin.random.Random
import android.graphics.Bitmap
import com.example.hillclimbracing.R

class FuelItemManager(
    private val gctx: GameContext,
    private val terrain: HillTerrain,
    private val player: Player,
) : IGameObject {
    var cameraX = 0f

    private val items = ArrayList<FuelItem>()

    private val playerRect = RectF()
    private val itemRect = RectF()

    private val random = Random(Random.nextInt())
    private var nextSpawnX = 700f

    private val fuelBitmap: Bitmap = gctx.res.getBitmap(R.mipmap.fuel_item)

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
            if (shouldSpawnFuel()) {
                val groundY = terrain.getGroundY(nextSpawnX)

                val heightOffset = random.nextFloat() *
                        (ITEM_MAX_HEIGHT_FROM_GROUND - ITEM_MIN_HEIGHT_FROM_GROUND) +
                        ITEM_MIN_HEIGHT_FROM_GROUND

                items.add(
                    FuelItem(
                        bitmap = fuelBitmap,
                        worldX = nextSpawnX,
                        y = groundY - heightOffset,
                    )
                )
            }

            nextSpawnX += getNextSpawnInterval()
        }
    }

    private fun shouldSpawnFuel(): Boolean {
        // 연료가 많을수록 덜 나오고, 연료가 적으면 조금 더 잘 나오게 한다.
        val spawnChance = when {
            player.fuel > 70f -> 0.55f
            player.fuel > 40f -> 0.70f
            else -> 0.90f
        }

        return random.nextFloat() < spawnChance
    }

    private fun getNextSpawnInterval(): Float {
        val minInterval: Float
        val maxInterval: Float

        when {
            player.fuel > 70f -> {
                minInterval = 2200f
                maxInterval = 3200f
            }

            player.fuel > 40f -> {
                minInterval = 1600f
                maxInterval = 2600f
            }

            else -> {
                // 너무 죽기 직전이면 완전 랜덤으로 방치하지 않고 약간 구제.
                minInterval = 1000f
                maxInterval = 1700f
            }
        }

        return random.nextFloat() * (maxInterval - minInterval) + minInterval
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
        private const val SEED = 20260509

        private const val SPAWN_LOOKAHEAD_DISTANCE = 2200f
        private const val REMOVE_BACK_DISTANCE = 800f

        private const val ITEM_MIN_HEIGHT_FROM_GROUND = 75f
        private const val ITEM_MAX_HEIGHT_FROM_GROUND = 125f
    }
}